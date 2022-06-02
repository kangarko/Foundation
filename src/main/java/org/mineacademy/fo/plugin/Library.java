package org.mineacademy.fo.plugin;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Represents a standalone library that can be loaded. You can also set
 * a minimum and a maximum MC version where this library works, as well
 * as Java versions to restrict loading.
 */
@Accessors(chain = true)
@Setter
public final class Library {

	/*
	 * Stored for convenience and performance purposes
	 */
	private static final int JAVA_VERSION = SimplePlugin.getJavaVersion();

	/**
	 * The groupID as per maven standards such as "org.mineacademy"
	 */
	@Getter
	private final String groupId;

	/**
	 * The artifactID as per maven standards such as "foundation"
	 */
	@Getter
	private final String artifactId;

	/**
	 * The version of the library such as "1.0.0"
	 */
	@Getter
	private final String version;

	/**
	 * The jar path from where we download the library JAR.
	 */
	private final String jarPath;

	/**
	 * The minimum Minecraft version this library requires.
	 */
	@Nullable
	private MinecraftVersion.V minimumMinecraftVersion;

	/**
	 * The maximum Minecraft version this library loads on.
	 */
	@Nullable
	private MinecraftVersion.V maximumMinecraftVersion;

	/**
	 * The minimum Java version this library loads on.
	 */
	@Nullable
	private Integer minimumJavaVersion;

	/**
	 * The maximum Java version this library loads on.
	 */
	@Nullable
	private Integer maximumJavaVersion;

	/**
	 * An optional user agent you can set when connecting to download the JAR from your own repository.
	 */
	@Nullable
	private String userAgent = null;

	/*
	 * Create a new library
	 */
	private Library(String groupId, String artifactId, String version, String repositoryPath) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;

		this.jarPath = repositoryPath;
	}

	/**
	 * Loads this library, returning true if loading was successful,
	 * false if minimum/maximum Java or Minecraft version not met
	 *
	 * @return
	 */
	public boolean load() {
		Valid.checkBoolean(JAVA_VERSION <= 8, "Library feature requires Java 8 and does not work on Java " + JAVA_VERSION);

		if (this.minimumJavaVersion != null && JAVA_VERSION < this.minimumJavaVersion)
			return false;

		if (this.maximumJavaVersion != null && JAVA_VERSION > this.maximumJavaVersion)
			return false;

		if (this.minimumMinecraftVersion != null && MinecraftVersion.olderThan(this.minimumMinecraftVersion))
			return false;

		if (this.maximumMinecraftVersion != null && MinecraftVersion.newerThan(this.maximumMinecraftVersion))
			return false;

		try {
			final File libraries = new File(Bukkit.getWorldContainer(), "libraries");
			final File file = new File(libraries, this.groupId.replace(".", "/") + "/" + this.artifactId.replace(".", "/") + "/" + this.version + "/" + this.artifactId + "-" + this.version + ".jar");

			// Download file from repository to our disk
			if (!file.exists()) {
				file.getParentFile().mkdirs();

				Bukkit.getLogger().info("Downloading library: " + this.getName());

				final URL url = new URL(this.jarPath);
				final URLConnection connection = url.openConnection();

				if (this.userAgent != null)
					connection.setRequestProperty("User-Agent", this.userAgent);

				try (InputStream in = connection.getInputStream()) {
					Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}

			//Bukkit.getLogger().info("Loading library: " + this.getName());

			// Load the library into the plugin's class loader
			final URL url = file.toURI().toURL();
			final ClassLoader classLoader = SimplePlugin.class.getClassLoader();
			final Method method = ReflectionUtil.getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);

			ReflectionUtil.invoke(method, classLoader, url);

		} catch (final Throwable throwable) {
			throw new RuntimeException("Unable to load library " + this.getName() + ".", throwable);
		}

		return true;
	}

	/**
	 * Return the groupId:artifactId:version as string
	 *
	 * @return
	 */
	public String getName() {
		return this.groupId + ":" + this.artifactId + ":" + this.version;
	}

	/**
	 * See {@link #getName()}
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return this.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new library from the Maven Central repository
	 *
	 * Use this if your favorite library is found at: https://mvnrepository.com/repos/central
	 *
	 * The path syntax is as follows: "groupId:artifactId:version" such as "org.jsoup:jsoup:1.14.3"
	 *
	 * @param path
	 * @return
	 */
	public static Library fromMavenRepo(String path) {
		final String[] split = path.split("\\:");
		Valid.checkBoolean(split.length == 3, "Malformed library path, expected <groupId>:<name>:<version>, got: " + path);

		return fromMavenRepo(split[0], split[1], split[2]);
	}

	/**
	 * Create a new library from the Maven Central repository
	 *
	 * Use this if your favorite library is found at: https://mvnrepository.com/repos/central
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return
	 */
	public static Library fromMavenRepo(String groupId, String artifactId, String version) {
		final String jarPath = "https://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";

		return new Library(groupId, artifactId, version, jarPath);
	}

	/**
	 * Create a new library from your own custom repository path.
	 *
	 * The path must be fully qualified online URL to the JAR. In {@link #fromMavenRepo(String, String, String)}
	 * we use "https://repo1.maven.org/maven2/{groupId}/{artifactId}/{version}/{artifactId}-{version}.jar"
	 * but in reality this could potentially be whatever such as yourdomain.com/yourlibrary.jar
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param jarPath
	 * @return
	 */
	public static Library fromPath(String groupId, String artifactId, String version, String jarPath) {
		return new Library(groupId, artifactId, version, jarPath);
	}
}