package org.mineacademy.fo.plugin;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;
import lombok.Setter;

@Setter
final class Library {

	@Getter
	private final String groupId;

	@Getter
	private final String artifactId;

	@Getter
	private final String version;

	private final String repositoryPath;

	private MinecraftVersion.V minimumVersion;
	private MinecraftVersion.V maximumVersion;

	private Library(String groupId, String artifactId, String version, String repositoryPath) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;

		this.repositoryPath = repositoryPath;
	}

	public boolean load() {

		if (this.minimumVersion != null && MinecraftVersion.olderThan(this.minimumVersion))
			return false;

		if (this.maximumVersion != null && MinecraftVersion.newerThan(this.maximumVersion))
			return false;

		try {
			final File libraries = new File(Bukkit.getWorldContainer(), "libraries");
			final File file = new File(libraries, this.groupId.replace(".", "/") + "/" + this.artifactId.replace(".", "/") + "/" + this.version + "/" + this.artifactId + "-" + this.version + ".jar");

			// Download file from repository to our disk
			if (!file.exists()) {
				file.getParentFile().mkdirs();

				Bukkit.getLogger().info("Downloading library: " + this.getName());

				final URL url = new URL(this.repositoryPath);
				final URLConnection connection = url.openConnection();

				try (InputStream in = connection.getInputStream()) {
					Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}

			Bukkit.getLogger().info("Loading library: " + this.getName());

			// Load the library into the plugin's class loader
			final URL url = file.toURI().toURL();
			final Method method = ReflectionUtil.getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);
			final ClassLoader classLoader = SimplePlugin.class.getClassLoader();

			ReflectionUtil.invoke(method, classLoader, url);

		} catch (final Throwable throwable) {
			Common.throwError(throwable, "Unable to load library " + this.getName() + ".");
		}

		return true;
	}

	public String getName() {
		return this.groupId + ":" + this.artifactId + ":" + this.version;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public static Library fromMavenRepo(String path) {
		final String[] split = path.split("\\:");
		Valid.checkBoolean(split.length == 3, "Malformed library path, expected <groupId>:<name>:<version>, got: " + path);

		return fromMavenRepo(split[0], split[1], split[2]);
	}

	public static Library fromMavenRepo(String groupId, String artifactId, String version) {
		final String repositoryPath = "https://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";

		return new Library(groupId, artifactId, version, repositoryPath);
	}

	public static Library fromCustomRepo(String groupId, String artifactId, String version, String repositoryPath) {
		return new Library(groupId, artifactId, version, repositoryPath);
	}
}