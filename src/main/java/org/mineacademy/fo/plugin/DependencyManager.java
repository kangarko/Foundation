package org.mineacademy.fo.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.settings.YamlConfig;

import com.google.common.io.ByteStreams;

import lombok.Getter;

public class DependencyManager {

	public void loadPluginDependencies() {
		YamlConfig pluginFile = YamlConfig.fromInternalPath("plugin.yml");

		if (MinecraftVersion.olderThan(V.v1_16))
			for (String library : pluginFile.getStringList("libraries")) {
				String[] split = library.split("\\:");
				Valid.checkBoolean(split.length == 3, "Malformed library path, expected <groupId>:<name>:<version>, got: " + library);

				Dependency dependency = new Dependency(split[0], split[1], split[2]);
				loadDependency(dependency);
			}
	}

	public void loadDependency(Dependency dependency) {
		System.out.println("Loading " + dependency.getPathName());

		try {
			final File file = this.downloadDependency(dependency);

			this.addToClasspath(file, (URLClassLoader) SimplePlugin.class.getClassLoader());

		} catch (final Throwable throwable) {
			Common.throwError(throwable, "Unable to load dependency " + dependency.getPathName() + ".");
		}
	}

	private void addToClasspath(File file, URLClassLoader classLoader) throws IOException {
		URL url = file.toURI().toURL();
		Method method = ReflectionUtil.getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);

		ReflectionUtil.invoke(method, classLoader, url);
	}

	private File downloadDependency(Dependency dependency) {
		final File libraries = new File(Bukkit.getWorldContainer(), "libraries");
		final File file = new File(libraries, dependency.getPathName());

		if (!file.exists()) {
			file.getParentFile().mkdirs();

			dependency.download(file);
		}

		return file;
	}

	public static final class Dependency {

		private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

		private final String url = "https://repo1.maven.org/maven2/";
		private final String mavenRepoPath;

		@Getter
		private final String pathName;

		public Dependency(String groupId, String artifactId, String version) {
			this.pathName = groupId.replace(".", "/") + "/" + artifactId.replace(".", "/") + "/" + version + "/" + artifactId + "-" + version + ".jar";
			this.mavenRepoPath = String.format(MAVEN_FORMAT, groupId.replace(".", "/"), artifactId, version, artifactId, version);
		}

		public void download(File file) {
			try {

				final URL dependencyUrl = new URL(this.url + this.mavenRepoPath);
				final URLConnection connection = dependencyUrl.openConnection();

				try (InputStream in = connection.getInputStream()) {
					final byte[] bytes = ByteStreams.toByteArray(in);

					if (bytes.length == 0)
						throw new FoException("Empty stream");

					Files.write(file.toPath(), bytes, StandardOpenOption.CREATE_NEW);
				}

			} catch (final IOException ex) {
				throw new FoException(ex);
			}
		}

	}

}