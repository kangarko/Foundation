package org.mineacademy.fo.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.YamlConfig;

/**
 * A simple predefined sub-command for quickly reloading the plugin
 * using /{label} reload|rl
 */
public final class ReloadCommand extends SimpleSubCommandCore {

	/**
	 * Create a new sub-command with the "reload" and "rl" aliases registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 */
	public ReloadCommand() {
		this("reload|rl");
	}

	/**
	 * Create a new sub-command with the given label registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 *
	 * @param label
	 */
	public ReloadCommand(String label) {
		super(label);

		this.setProperties();
	}

	/**
	 * Create a new sub-command with the "reload" and "rl" aliases registered in the given command group.
	 *
	 * @param group
	 */
	public ReloadCommand(SimpleCommandGroup group) {
		this(group, "reload|rl");
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param group
	 * @param label
	 */
	public ReloadCommand(SimpleCommandGroup group, String label) {
		super(group, label);

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setMaxArguments(0);
		this.setDescription(Lang.component("command-reload-description"));
	}

	@Override
	protected void onCommand() {
		try {
			this.tellInfo(Lang.component("command-reload-started"));
			final List<String> erroredFiles = new ArrayList<>();

			final List<File> yamlFiles = new ArrayList<>();

			this.collectYamlFiles(Platform.getPlugin().getDataFolder(), yamlFiles);

			for (final File file : yamlFiles)
				try {
					YamlConfig.fromFile(file);

				} catch (final Throwable t) {
					t.printStackTrace();

					erroredFiles.add(file.getName());
				}

			if (!erroredFiles.isEmpty()) {
				this.tellError(Lang.componentVars("command-reload-file-load-error", "files", String.join(", ", erroredFiles)));

				return;
			}

			Platform.getPlugin().reload();

			this.tellSuccess(Lang.componentVars("command-reload-success"));

		} catch (final Throwable t) {
			this.tellError(Lang.componentVars("command-reload-fail", "error", t.getMessage() != null ? t.getMessage() : "unknown"));

			t.printStackTrace();
		}
	}

	/*
	 * Get a list of all files ending with "yml" in the given directory
	 * and its subdirectories
	 */
	private List<File> collectYamlFiles(File directory, List<File> list) {
		if (directory.exists())
			for (final File file : directory.listFiles()) {
				if (file.getName().endsWith("yml"))
					list.add(file);

				if (file.isDirectory())
					this.collectYamlFiles(file, list);
			}

		return list;
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}