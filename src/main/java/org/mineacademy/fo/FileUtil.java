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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.Tuple;
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
	 * Return the name of the file from the given path, stripping
	 * any extension and folders.
	 * <p>
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
		final File datafolder = SimplePlugin.getInstance().getDataFolder();
		final int lastIndex = path.lastIndexOf('/');
		final File directory = new File(datafolder, path.substring(0, lastIndex >= 0 ? lastIndex : 0));

		directory.mkdirs();

		final File destination = new File(datafolder, path);

		try {
			destination.createNewFile();

		} catch (final IOException ex) {
			Bukkit.getLogger().severe("Failed to create a new file " + path);

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

	// ----------------------------------------------------------------------------------------------------
	// Reading
	// ----------------------------------------------------------------------------------------------------

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
	public static List<String> readLines(File file) {
		Valid.checkNotNull(file, "File cannot be null");
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
	 * @param prefix
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
			Bukkit.getLogger().severe("Failed to write to " + to);

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
	 * @return the extracted file
	 */
	public static File extract(String path) {
		return extract(path, path);
	}

	/**
	 * Copy file from our plugin jar to destination - customizable destination file
	 * name.
	 *
	 * @param override always extract file even if already exists?
	 * @param from     the path to the file inside the plugin
	 * @param to       the path where the file will be copyed inside the plugin
	 *                 folder
	 * @return the extracted file
	 */
	public static File extract(String from, String to) {
		File file = new File(SimplePlugin.getInstance().getDataFolder(), to);

		final List<String> lines = getInternalResource(from);
		Valid.checkNotNull(lines, "Inbuilt file not found: " + from);

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
	 * @param from
	 * @param to
	 * @return
	 */
	public static File extractRaw(String path) {
		File file = new File(SimplePlugin.getInstance().getDataFolder(), path);

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
	 * @return the resource input stream, or null if not found
	 */
	public static List<String> getInternalResource(@NonNull String path) {

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

	// ----------------------------------------------------------------------------------------------------
	// Archiving
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Attempt to compress the given file into a byte array of the following structure:
	 *
	 * A list of all lines from the file + the file path up to your server root folder such as plugins/ChatControl/localization/ if
	 * the file is in the localization/ folder in your plugin folder appended at the end of the list.
	 *
	 * THE FILE MUST BE SOMEWHERE IN YOUR ROOT SERVER FOLDER (does not matter how many directories deep)
	 * AND YOUR WORKING DIRECTORY MUST BE SET TO YOUR ROOT SERVER FOLDER.
	 *
	 * @param file
	 * @return filepath from server root with the byte compressed file content array
	 */
	public static Tuple<String, byte[]> compress(@NonNull File file) {
		Valid.checkBoolean(!file.isDirectory(), "Cannot compress a directory: " + file.getPath());
		final List<String> lines = new ArrayList<>(readLines(file)); // ensure modifiable

		// Add all parent directories until we reach the plugin's folder
		final List<File> parentDirs = new ArrayList<>();

		File parent = file.getParentFile();

		while (parent != null) {
			parentDirs.add(parent);

			parent = parent.getParentFile();
		}

		Collections.reverse(parentDirs);

		final String filePath = Common.join(parentDirs, "/", File::getName) + "/" + file.getName();

		lines.add(filePath);

		// Join using our custom deliminer
		final String joinedLines = String.join("%CMPRSDBF%", lines);

		return new Tuple<>(filePath, CompressUtil.compress(joinedLines));
	}

	/**
	 * Decompresses and writes all content to the file from the data array
	 *
	 * see {@link #compress(File)}
	 *
	 * @param data
	 *
	 * @return
	 */
	public static File decompressAndWrite(@NonNull byte[] data) {
		return decompressAndWrite(null, data);
	}

	/**
	 * Decompresses and writes all content to the file,
	 *
	 * see {@link #compress(File)}
	 *
	 * @param destination
	 * @param data
	 *
	 * @return
	 */
	public static File decompressAndWrite(File destination, @NonNull byte[] data) {
		final Tuple<File, List<String>> tuple = decompress(data);

		if (destination == null)
			destination = tuple.getKey();

		final List<String> lines = tuple.getValue();

		try {
			FileUtil.createIfNotExists(destination);

			Files.write(destination.toPath(), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (final IOException e) {
			Common.throwError(e, "Failed to write " + lines.size() + " lines into " + destination);
		}

		return destination;
	}

	/**
	 * Decompresses the given data array into a file-lines tuple,
	 * see {@link #compress(File)}
	 *
	 * @param data
	 * @return
	 */
	public static Tuple<File, List<String>> decompress(@NonNull byte[] data) {
		final String decompressed = CompressUtil.decompress(data);
		final String[] linesRaw = decompressed.split("%CMPRSDBF%");
		Valid.checkBoolean(linesRaw.length > 0, "Received empty lines to decompress into a file!");

		final List<String> lines = new ArrayList<>();

		// Load lines
		for (int i = 0; i < linesRaw.length - 1; i++)
			lines.add(linesRaw[i]);

		// The final line is the file
		final File file = new File(linesRaw[linesRaw.length - 1]);

		return new Tuple<>(file, lines);
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
		final File parent = SimplePlugin.getInstance().getDataFolder().getParentFile().getParentFile();
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
