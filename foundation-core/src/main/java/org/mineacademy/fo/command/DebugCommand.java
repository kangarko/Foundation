package org.mineacademy.fo.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Setter;

/**
 * A sample sub-command that you can automatically add
 * to your main command group to help collect debugging information
 * users can submit to you when they have problems.
 */
public final class DebugCommand extends SimpleSubCommand {

	/**
	 * Set the custom debug lines you would like to add to the debug file
	 */
	@Setter
	private static List<String> debugLines = new ArrayList<>();

	/**
	 * Create a new sub-command with the given permission.
	 *
	 * @param permission
	 */
	public DebugCommand(String permission) {
		this();

		this.setPermission(permission);
	}

	public DebugCommand() {
		super("debug");

		this.setDescription("ZIP your settings for reporting bugs.");
	}

	@Override
	protected void onCommand() {
		this.tell(SimpleLocalization.Commands.DEBUG_PREPARING);

		final File debugFolder = FileUtil.getFile("debug");
		final List<File> files = this.listFilesRecursively(Platform.getPlugin().getDataFolder(), new ArrayList<>());

		// Clean up the old folder if exists
		FileUtil.deleteRecursivelly(debugFolder);

		// Collect general debug information first
		this.writeDebugInformation();

		// Copy all plugin files
		this.copyFilesToDebug(files);

		// Zip the folder
		this.zipAndRemoveFolder(debugFolder);

		this.tell(SimpleLocalization.Commands.DEBUG_SUCCESS.replaceText(b -> b.matchLiteral("{amount}").replacement(String.valueOf(files.size()))));
	}

	/*
	 * Write our own debug information
	 */
	private void writeDebugInformation() {

		final List<String> lines = CommonCore.toList(CommonCore.consoleLine(),
				" Debug log generated " + TimeUtil.getFormattedDate(),
				CommonCore.consoleLine(),
				"Plugin: " + Platform.getPlugin().getName(),
				"Server Version: " + Platform.getServerVersion(),
				"Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.specification.vendor") + "/" + System.getProperty("java.vm.vendor") + ")",
				"OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"),
				"Players Online: " + Platform.getOnlinePlayers().size(),
				"Plugins: " + String.join(", ", Platform.getServerPlugins()));

		lines.addAll(debugLines);
		FileUtil.write("debug/general.txt", lines);
	}

	/*
	 * Copy the given files into debug/ folder
	 */
	private void copyFilesToDebug(List<File> files) {

		for (final File file : files)
			try {
				// Get the path in our folder
				final String path = file.getPath().replace("\\", "/").replace("plugins/" + Platform.getPlugin().getName(), "");

				// Create a copy file
				final File copy = FileUtil.createIfNotExists("debug/" + path);

				// Strip sensitive keys from .YML files
				if (file.getName().endsWith(".yml")) {
					final YamlConfig config = YamlConfig.fromFile(file);
					final YamlConfig copyConfig = YamlConfig.fromFile(copy);

					for (final String key : config.getKeys(true)) {
						final Object value = config.getObject(key);

						if (!key.contains("MySQL"))
							copyConfig.set(key, value);
					}

					copyConfig.save(copy);
				}

				else
					Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);

			} catch (final Exception ex) {
				ex.printStackTrace();

				this.returnTell(SimpleLocalization.Commands.DEBUG_COPY_FAIL.replaceText(b -> b.matchLiteral("{file}").replacement(file.getName())));
			}
	}

	/*
	 * Zips the given folder and removes it afterwards
	 */
	private void zipAndRemoveFolder(File folder) {
		try {
			final String path = folder.getPath();

			FileUtil.zip(path, path);
			FileUtil.deleteRecursivelly(folder);

		} catch (final IOException ex) {
			ex.printStackTrace();

			this.returnTell(SimpleLocalization.Commands.DEBUG_ZIP_FAIL);
		}
	}

	/*
	 * Load the list of files available to ZIP
	 */
	private List<File> listFilesRecursively(File folder, List<File> files) {
		for (final File file : folder.listFiles())
			if (file.isDirectory()) {
				// Ignore log directory and ignore the debug directory itself
				if (!file.getName().equals("logs") && !file.getName().equals("debug"))
					this.listFilesRecursively(file, files);

			} else // Ignore the debug zip file itself
			if (!file.getName().equals("debug.zip") && !file.getName().equals("mysql.yml"))
				files.add(file);

		return files;
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}

	/**
	 * Add custom debug lines to the general.txt file in the compressed ZIP file.
	 *
	 * @param lines
	 */
	public static void addDebugLines(String... lines) {
		for (final String line : lines)
			debugLines.add(line);
	}
}
