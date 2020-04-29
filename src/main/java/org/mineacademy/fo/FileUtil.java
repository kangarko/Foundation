package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for managing files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil {

	/**
	 * File system manipulating with your JAR plugin file, only created once
	 */
	private static FileSystem fileSystem = null;

	/**
	 * Return the name of the file from the given path, stripping
	 * any extension and folders.
	 *
	 * Example: classes/Archer.yml will only return Archer
	 *
	 * @param path
	 * @return
	 */
	public static String getFileName(File file) {
		return getFileName(file.getName());
	}

	/**
	 * Return the name of the file from the given path, stripping
	 * any extension and folders.
	 *
	 * Example: classes/Archer.yml will only return Archer
	 *
	 * @param path
	 * @return
	 */
	public static String getFileName(String path) {
		Valid.checkBoolean(path != null && !path.isEmpty(), "The given path must not be empty!");

		int pos = path.lastIndexOf("/");

		if (pos > 0)
			path = path.substring(pos + 1, path.length());

		pos = path.lastIndexOf(".");

		if (pos > 0)
			path = path.substring(0, pos);

		return path;
	}

	// ----------------------------------------------------------------------------------------------------
	// Getting files
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return a file in a path in our plugin folder, failing if the file does not exist
	 *
	 * @param path
	 * @return
	 */
	public static File getFileStrict(String path) {
		final File file = getFile(path);
		Valid.checkBoolean(file.exists(), "File '" + path + "' does not exists!");

		return file;
	}

	/**
	 * Returns a file from the given path inside our plugin folder, creating
	 * the file if it does not exist
	 *
	 * @param path
	 * @return
	 */
	public static File getOrMakeFile(String path) {
		final File file = getFile(path);

		return file.exists() ? file : createFile(path);
	}

	/**
	 * Create a new file in our plugin folder, supporting multiple directory paths
	 *
	 * Example: logs/admin/console.log or worlds/nether.yml are all valid paths
	 *
	 * @param path
	 * @return
	 */
	private static File createFile(String path) {
		final File datafolder = SimplePlugin.getInstance().getDataFolder();
		final int lastIndex = path.lastIndexOf('/');
		final File directory = new File(datafolder, path.substring(0, lastIndex >= 0 ? lastIndex : 0));

		directory.mkdirs();

		final File destination = new File(datafolder, path);

		try {
			destination.createNewFile();

		} catch (final IOException ex) {
			System.out.println("Failed to create a new file " + path);

			ex.printStackTrace();
		}

		return destination;
	}

	/**
	 * Return a file in a path in our plugin folder, file may not exist
	 *
	 * @param path
	 * @return
	 */
	public static File getFile(String path) {
		return new File(SimplePlugin.getInstance().getDataFolder(), path);
	}

	/**
	 * Return all files in our plugin directory within a given path, ending with the given extension
	 *
	 * @param directory
	 * @param extension
	 * @return
	 */
	public static File[] getFiles(@NonNull String directory, @NonNull String extension) {

		// Remove initial dot, if any
		if (extension.startsWith("."))
			extension = extension.substring(1);

		final File dataFolder = new File(SimplePlugin.getData(), directory);

		if (!dataFolder.exists())
			dataFolder.mkdirs();

		final String finalExtension = extension;

		return dataFolder.listFiles((FileFilter) file -> !file.isDirectory() && file.getName().endsWith("." + finalExtension));
	}

	/**
	 * Checks if the file in the path exists and creates a new one if it does not
	 *
	 * NB: THIS PATH IS ABSOLUTE, I.E. NOT IN YOUR PLUGINS FOLDER
	 *
	 * @param path
	 * @return
	 */
	public static File getOrMakeAbs(String path) {
		return getOrMakeAbs(new File(path));
	}

	/**
	 * Checks if the file in the parent and child path exists and creates a new one if it does not
	 *
	 * NB: THIS PATH IS ABSOLUTE, I.E. NOT IN YOUR PLUGINS FOLDER
	 *
	 * @param parent
	 * @param child
	 * @return
	 */
	public static File getOrMakeAbs(File parent, String child) {
		return getOrMakeAbs(new File(parent, child));
	}

	/**
	 * Checks if the file exists and creates a new one if it does not
	 *
	 * NB: THIS PATH IS ABSOLUTE, I.E. NOT IN YOUR PLUGINS FOLDER
	 *
	 * @param file
	 * @return
	 */
	public static File getOrMakeAbs(File file) {
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (final Throwable t) {
				Common.throwError(t, "Could not create new " + file + " due to " + t);
			}

		return file;
	}

	// ----------------------------------------------------------------------------------------------------
	// Reading
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return all lines in the file, failing if the file does not exists
	 *
	 * @param file
	 * @return
	 */
	public static List<String> readLines(File file) {
		Valid.checkNotNull(file, "File cannot be null");
		Valid.checkBoolean(file.exists(), "File: " + file + " does not exists!");

		try {
			return Files.readAllLines(Paths.get(file.toURI()), StandardCharsets.UTF_8);

		} catch (final IOException ex) {

			// Older method, missing libraries
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				final List<String> lines = new ArrayList<>();
				String line;

				while ((line = br.readLine()) != null)
					lines.add(line);

				return lines;

			} catch (final IOException ee) {
				throw new FoException(ee, "Could not read lines from " + file.getName());
			}
		}
	}

	/**
	 * Loads YAML configuration from file, failing if anything happens or the file does not exist
	 *
	 * @param file
	 * @return
	 * @throws RuntimeException
	 */
	public static YamlConfiguration loadConfigurationStrict(File file) throws RuntimeException {
		Valid.checkNotNull(file, "File is null!");
		Valid.checkBoolean(file.exists(), "File " + file.getName() + " does not exists");

		final YamlConfiguration conf = new YamlConfiguration();

		try {
			checkFileForKnownErrors(file);

			conf.load(file);

		} catch (final FileNotFoundException ex) {
			throw new FoException(ex, "Configuration file missing: " + file.getName());

		} catch (final IOException ex) {
			throw new FoException(ex, "IO exception opening " + file.getName());

		} catch (final InvalidConfigurationException ex) {
			throw new FoException(ex, "Malformed YAML file " + file.getName());

		} catch (final Throwable t) {
			throw new FoException(t, "Error reading YAML file " + file.getName());
		}

		Valid.checkNotNull(conf, "Could not load: " + file.getName());
		return conf;
	}

	/*
	 * Check file for known errors
	 */
	private static void checkFileForKnownErrors(File file) throws IOException {
		for (final String line : Files.readAllLines(file.toPath()))
			if (line.contains("[*]"))
				throw new FoException("Found [*] in your yaml file " + file + ". Please replace it with ['*'] instead.");
		//else
		//	if (line.replaceAll(/*REGEX na zaciatocne a konecne medzery*/)
		// if (line.contains("-") && line.contains("*") && !line.contains("\"") && !contains '
		// else
		// if (Common.hasColor(message) && !contains " '
	}

	// ----------------------------------------------------------------------------------------------------
	// Writing
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Write a line to file with optional prefix which can be null.
	 *
	 * The line will be as follows: [date] prefix msg
	 *
	 * @param to     	path to the file inside the plugin folder
	 * @param prefix 	optional prefix, can be null
	 * @param message   line, is split by \n
	 */
	public static void writeFormatted(String to, String prefix, String message) {
		message = Common.stripColors(message).trim();

		if (!message.equalsIgnoreCase("none") && !message.isEmpty())
			for (final String line : Common.splitNewline(message))
				if (!line.isEmpty())
					write(to, "[" + TimeUtil.getFormattedDate() + "] " + (prefix != null ? prefix + ": " : "") + line);
	}

	/**
	 * Write lines to a file path in our plugin directory,
	 * creating the file if it does not exist, appending lines at the end
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(String to, String... lines) {
		write(to, Arrays.asList(lines));
	}

	/**
	 * Write lines to a file path in our plugin directory,
	 * creating the file if it does not exist, appending lines at the end
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(String to, Collection<String> lines) {
		write(getOrMakeFile(to), lines, StandardOpenOption.APPEND);
	}

	/**
	 * Write the given lines to file
	 *
	 * @param to
	 * @param lines
	 * @param options
	 */
	public static void write(File to, Collection<String> lines, StandardOpenOption... options) {
		try {
			final Path path = Paths.get(to.toURI());

			try {
				Files.write(path, lines, StandardCharsets.UTF_8, options);

			} catch (final ClosedByInterruptException ex) {
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(to, true))) {
					for (final String line : lines)
						bw.append(System.lineSeparator() + line);

				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

		} catch (final Exception ex) {
			System.out.println("Failed to write to " + to);

			ex.printStackTrace(); // do not throw our exception since it would cause an infinite loop if there is a problem due to error writing
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Extracting from our plugin .jar file
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Copy file from our plugin jar to destination.
	 * No action is done if the file already exists.
	 *
	 * @param path the path to the file inside the plugin
	 *
	 * @return the extracted file
	 */
	public static File extract(String path) {
		return extract(false, path, path, null);
	}

	/**
	 * Copy file our plugin jar to destination, replacing variables in that file before it is saved
	 * No action is done if the file already exists.
	 *
	 * @param path the path to the file inside the plugin
	 * @param replacer the variables replacer, takes in a variable (you must put brackets around it) and outputs
	 * the desired string
	 *
	 * @return the extracted file
	 */
	public static File extract(String path, Function<String, String> replacer) {
		return extract(false, path, path, replacer);
	}

	/**
	 * Copy file our plugin jar to destination
	 * No action is done if the file already exists.
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public static File extract(String from, String to) {
		return extract(false, from, to);
	}

	/**
	 * Copy file our plugin jar to destination
	 * No action is done if the file already exists.
	 *
	 * @param override
	 * @param from
	 * @param to
	 * @return
	 */
	public static File extract(boolean override, String from, String to) {
		return extract(override, from, to, null);
	}

	/**
	 * Copy file from our plugin jar to destination - customizable destination file
	 * name.
	 *
	 * @param override always extract file even if already exists?
	 * @param from     the path to the file inside the plugin
	 * @param to       the path where the file will be copyed inside the plugin
	 *                 folder
	 * @param replacer the variables replacer
	 *
	 * @return the extracted file
	 */
	public static File extract(boolean override, String from, String to, @Nullable Function<String, String> replacer) {
		File file = new File(SimplePlugin.getInstance().getDataFolder(), to);

		final InputStream is = FileUtil.getInternalResource("/" + from);
		Valid.checkNotNull(is, "Inbuilt file not found: " + from);

		if (!override && file.exists())
			return file;

		file = FileUtil.createFile(to);

		try {

			final List<String> lines = new ArrayList<>();

			// Load lines from internal file and replace them
			try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
				String line;

				while ((line = br.readLine()) != null) {
					line = replacer != null ? replacer.apply(line) : line;

					lines.add(line);
				}
			}

			Files.write(file.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (final IOException ex) {
			Common.error(ex,
					"Failed to extract " + from + " to " + to,
					"Error: %error");
		}

		return file;
	}

	/**
	 * Extracts the folder and all of its content from the JAR file to
	 * the given path in your plugin folder
	 *
	 * @param source the source folder in your JAR plugin file
	 * @param destination the destination folder name in your plugin folder
	 */
	public static void extractFolderFromJar(String source, final String destination) {
		try {
			final Path target = getFile(destination).toPath();
			final URI resource = SimplePlugin.class.getResource("").toURI();

			final FileSystem fileSystem;

			if (FileUtil.fileSystem == null) {
				fileSystem = FileSystems.newFileSystem(resource, Collections.emptyMap());

				FileUtil.fileSystem = fileSystem;

			} else
				fileSystem = FileSystems.getFileSystem(resource);

			final Path jarPath = fileSystem.getPath(source);

			Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {

				private Path currentTarget;

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					currentTarget = target.resolve(jarPath.relativize(dir).toString());
					Files.createDirectories(currentTarget);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.copy(file, target.resolve(jarPath.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to copy " + source + " to " + destination);
		}
	}

	/**
	 * Return an internal resource within our plugin's jar file
	 *
	 * @param path
	 * @return the resource input stream, or null if not found
	 */
	public static InputStream getInternalResource(String path) {
		// First attempt
		InputStream is = SimplePlugin.getInstance().getClass().getResourceAsStream(path);

		// Try using Bukkit
		if (is == null)
			is = SimplePlugin.getInstance().getResource(path);

		// The hard way - go in the jar file
		if (is == null)
			try (JarFile f = new JarFile(SimplePlugin.getSource())) {
				final JarEntry e = f.getJarEntry(path);

				if (e != null)
					is = f.getInputStream(e);

			} catch (final IOException ex) {
				ex.printStackTrace();
			}

		return is;
	}

	// ----------------------------------------------------------------------------------------------------
	// Archiving
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Creates a ZIP archive from the given source directory (inside our plugin folder)
	 * to the given full path (in our plugin folder) - please do not specify any extension, just the dir & file name
	 *
	 * @param sourceDirectory
	 * @param to
	 * @throws IOException
	 */
	public static void zip(String sourceDirectory, String to) throws IOException {
		final File parent = SimplePlugin.getInstance().getDataFolder().getParentFile().getParentFile();
		final Path pathTo = Files.createFile(Paths.get(new File(parent, to + ".zip").toURI()));

		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(pathTo))) {
			final Path pathFrom = Paths.get(new File(parent, sourceDirectory).toURI());

			Files.walk(pathFrom).filter(path -> !Files.isDirectory(path) && !path.toFile().getName().endsWith(".log")).forEach(path -> {
				final ZipEntry zipEntry = new ZipEntry(pathFrom.relativize(path).toString());

				try {
					zs.putNextEntry(zipEntry);

					Files.copy(path, zs);
					zs.closeEntry();
				} catch (final IOException ex) {
					ex.printStackTrace();
				}
			});
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Checksums
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Generates a md5 checksum from the given file
	 *
	 * @param filename
	 * @return
	 */
	public static String getMD5Checksum(File filename) {
		try {
			final byte[] b = createChecksum(filename);
			String result = "";

			for (int i = 0; i < b.length; i++)
				result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);

			return result;
		} catch (final Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}

	private static byte[] createChecksum(File filename) throws Exception {
		try (InputStream fis = new FileInputStream(filename)) {
			final byte[] buffer = new byte[1024];
			final MessageDigest complete = MessageDigest.getInstance("MD5");
			int numRead;

			do {
				numRead = fis.read(buffer);

				if (numRead > 0)
					complete.update(buffer, 0, numRead);
			} while (numRead != -1);

			return complete.digest();
		}
	}
}
