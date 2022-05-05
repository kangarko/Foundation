package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for managing files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil {

	/**
	 * Return the name of the file from the given path, stripping
	 * any extension and folders.
	 * <p>
	 * Example: classes/Archer.yml will only return Archer
	 *
	 * @param file
	 * @return
	 */
	public static String getFileName(File file) {
		return getFileName(file.getName());
	}

	/**
	 * Return the name of the file from the given path, stripping
	 * any extension and folders.
	 * <p>
	 * Example: classes/Archer.yml will only return Archer
	 *
	 * @param path
	 * @return
	 */
	public static String getFileName(String path) {
		Valid.checkBoolean(path != null && !path.isEmpty(), "The given path must not be empty!");

		int pos = path.lastIndexOf("/");

		if (pos > 0)
			path = path.substring(pos + 1);

		pos = path.lastIndexOf(".");

		if (pos > 0)
			path = path.substring(0, pos);

		return path;
	}

	// ----------------------------------------------------------------------------------------------------
	// Getting files
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Returns a file from the given path inside our plugin folder, creating
	 * the file if it does not exist
	 *
	 * @param path
	 * @return
	 */
	public static File getOrMakeFile(String path) {
		final File file = getFile(path);

		return file.exists() ? file : createIfNotExists(path);
	}

	/**
	 * Checks if the file exists and creates a new one if it does not
	 *
	 * @param file
	 * @return
	 */
	public static File createIfNotExists(File file) {
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (final Throwable t) {
				Common.throwError(t, "Could not create new " + file + " due to " + t);
			}

		return file;
	}

	/**
	 * Create a new file in our plugin folder, supporting multiple directory paths
	 * <p>
	 * Example: logs/admin/console.log or worlds/nether.yml are all valid paths
	 *
	 * @param path
	 * @return
	 */
	public static File createIfNotExists(String path) {
		final File datafolder = SimplePlugin.getData();
		final int lastIndex = path.lastIndexOf('/');
		final File directory = new File(datafolder, path.substring(0, lastIndex >= 0 ? lastIndex : 0));

		directory.mkdirs();

		final File destination = new File(datafolder, path);

		try {
			destination.createNewFile();

		} catch (final IOException ex) {
			Common.error(ex, "Failed to create a new file " + path);
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
		return new File(SimplePlugin.getData(), path);
	}

	/**
	 * Return all files in our plugin directory within a given path, ending with the given extension
	 *
	 * @param directory inside your plugin's folder
	 * @param extension where dot is placed automatically in case it is lacking
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

	// ----------------------------------------------------------------------------------------------------
	// Reading
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return all lines from the given URL, opening a connection with a fake user agent first
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static List<String> readLines(URL url) throws IOException {

		final URLConnection connection = url.openConnection();

		// Set a random user agent to prevent most webhostings from rejecting with 403
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040803 Firefox/0.9.3");
		connection.setConnectTimeout(6000);
		connection.setReadTimeout(6000);
		connection.setDoOutput(true);

		return readLines(connection);
	}

	/**
	 * Return all lines from the given connection
	 *
	 * @param connection
	 * @return
	 */
	public static List<String> readLines(URLConnection connection) {
		final List<String> lines = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;

			while ((inputLine = reader.readLine()) != null)
				lines.add(inputLine);

		} catch (final IOException ex) {
			Remain.sneaky(ex);
		}

		return lines;
	}

	/**
	 * Return all lines from file in a path in our plugin folder, file must exists.
	 *
	 * @param fileName
	 * @return
	 */
	public static List<String> readLines(String fileName) {
		return readLines(getFile(fileName));
	}

	/**
	 * Return all lines in the file, failing if the file does not exists
	 *
	 * @param file
	 * @return
	 */
	public static List<String> readLines(@NonNull File file) {
		Valid.checkBoolean(file.exists(), "File: " + file + " does not exists!");

		// Older method, missing libraries
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			final List<String> lines = new ArrayList<>();
			String line;

			while ((line = br.readLine()) != null)
				lines.add(line);

			return lines;

		} catch (final IOException ee) {
			throw new FoException(ee, "Could not read lines from " + file.getName());
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Writing
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Write a line to file
	 * <p>
	 * The line will be as follows: [date] msg
	 *
	 * @param to
	 * @param message
	 */
	public static void writeFormatted(String to, String message) {
		writeFormatted(to, null, message);
	}

	/**
	 * Write a line to file with optional prefix which can be null.
	 * <p>
	 * The line will be as follows: [date] prefix msg
	 *
	 * @param to      path to the file inside the plugin folder
	 * @param prefix  optional prefix, can be null
	 * @param message line, is split by \n
	 */
	public static void writeFormatted(String to, String prefix, String message) {
		message = Common.stripColors(message).trim();

		if (!message.equalsIgnoreCase("none") && !message.isEmpty())
			for (final String line : message.split("\n"))
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
	 * Write lines to a file, creating the file if not exist appending lines at the end
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(File to, String... lines) {
		write(createIfNotExists(to), Arrays.asList(lines), StandardOpenOption.APPEND);
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
				if (!to.exists())
					createIfNotExists(to);

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

			// do not throw our exception since it would cause an infinite loop if there is a problem due to error writing
			Common.error(ex, "Failed to write to " + to);
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
	 * @return the extracted file
	 */
	public static File extract(String path) {
		return extract(path, path);
	}

	/**
	 * Copy file from our plugin jar to destination - customizable destination file
	 * name.
	 *
	 * @param from     the path to the file inside the plugin
	 * @param to       the path where the file will be copyed inside the plugin
	 *                 folder
	 * @return the extracted file
	 */
	public static File extract(String from, String to) {
		File file = new File(SimplePlugin.getData(), to);

		final List<String> lines = getInternalFileContent(from);
		Valid.checkNotNull(lines, "Inbuilt " + from + " not found! Did you reload?");

		if (file.exists())
			return file;

		file = createIfNotExists(to);

		try {
			final String fileName = getFileName(file);

			// Replace variables in lines
			for (int i = 0; i < lines.size(); i++)
				lines.set(i, replaceVariables(lines.get(i), fileName));

			Files.write(file.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (final IOException ex) {
			Common.error(ex,
					"Failed to extract " + from + " to " + to,
					"Error: %error");
		}

		return file;
	}

	/**
	 * Similar to {@link #extract(String, String)} but intended
	 * for non-text file types such as images etc.
	 *
	 * @param path
	 * @return
	 */
	public static File extractRaw(String path) {
		File file = new File(SimplePlugin.getData(), path);

		try (JarFile jarFile = new JarFile(SimplePlugin.getSource())) {

			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry entry = it.nextElement();

				if (entry.toString().equals(path)) {
					final InputStream is = jarFile.getInputStream(entry);

					if (file.exists())
						return file;

					file = createIfNotExists(path);

					try {
						Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

					} catch (final IOException ex) {
						Common.error(ex,
								"Failed to extract " + path,
								"Error: %error");
					}

					return file;

				}
			}

		} catch (final Throwable ex) {
			ex.printStackTrace();
		}

		throw new FoException("Inbuilt file not found: " + path);
	}

	/*
	 * A helper method to replace variables in files we are extracting.
	 *
	 * Saves us time so that we can distribute the same file across multiple
	 * plugins each having its own unique plugin name and file name.
	 */
	private static String replaceVariables(String line, String fileName) {
		return line
				.replace("{plugin_name}", SimplePlugin.getNamed())
				.replace("{plugin_name_lower}", SimplePlugin.getNamed().toLowerCase())
				.replace("{file}", fileName)
				.replace("{file_lowercase}", fileName);
	}

	/**
	 * Extracts the folder and all of its content from the JAR file to
	 * the given path in your plugin folder
	 *
	 * @param folder      the source folder in your JAR plugin file
	 * @param destination the destination folder name in your plugin folder
	 */
	public static void extractFolderFromJar(String folder, final String destination) {
		Valid.checkBoolean(folder.endsWith("/"), "Folder must end with '/'! Given: " + folder);
		Valid.checkBoolean(!folder.startsWith("/"), "Folder must not start with '/'! Given: " + folder);

		if (getFile(folder).exists())
			return;

		try (JarFile jarFile = new JarFile(SimplePlugin.getSource())) {
			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry jarEntry = it.nextElement();
				final String entryName = jarEntry.getName();

				// Copy each individual file manually
				if (entryName.startsWith(folder) && !entryName.equals(folder))
					extract(entryName);
			}

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to copy folder " + folder + " to " + destination);
		}
	}

	/**
	 * Return an internal resource within our plugin's jar file
	 *
	 * @param path
	 * @return the content of the internal file
	 */
	public static List<String> getInternalFileContent(@NonNull String path) {

		try (JarFile jarFile = new JarFile(SimplePlugin.getSource())) {

			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry entry = it.nextElement();

				if (entry.toString().equals(path)) {

					final InputStream is = jarFile.getInputStream(entry);
					final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
					final List<String> lines = reader.lines().collect(Collectors.toList());

					reader.close();
					return lines;
				}
			}

		} catch (final Throwable ex) {
			ex.printStackTrace();
		}

		return null;
	}

	/**
	 * Remove the given file with all subfolders
	 *
	 * @param file
	 */
	public static void deleteRecursivelly(File file) {
		if (file.isDirectory())
			for (final File subfolder : file.listFiles())
				deleteRecursivelly(subfolder);

		if (file.exists())
			Valid.checkBoolean(file.delete(), "Failed to delete file: " + file);
	}

	/**
	 * Creates a ZIP archive from the given source directory (inside our plugin folder)
	 * to the given full path (in our plugin folder) - please do not specify any extension, just the dir & file name
	 *
	 * @param sourceDirectory
	 * @param to
	 * @throws IOException
	 */
	public static void zip(String sourceDirectory, String to) throws IOException {
		final File parent = SimplePlugin.getData().getParentFile().getParentFile();
		final File toFile = new File(parent, to + ".zip");

		if (toFile.exists())
			Valid.checkBoolean(toFile.delete(), "Failed to delete old file " + toFile);

		final Path pathTo = Files.createFile(Paths.get(toFile.toURI()));

		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(pathTo))) {
			final Path pathFrom = Paths.get(new File(parent, sourceDirectory).toURI());

			Files.walk(pathFrom).filter(path -> !Files.isDirectory(path)).forEach(path -> {
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
}
