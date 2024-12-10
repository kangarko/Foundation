package org.mineacademy.fo.library;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import com.velocitypowered.api.plugin.PluginManager;

/**
 * A runtime dependency manager for Velocity plugins.
 */
public class VelocityLibraryManager extends LibraryManager {

	/**
	 * Velocity plugin manager used for adding files to the plugin's classpath
	 */
	private final PluginManager pluginManager;

	/**
	 * The plugin instance required by the plugin manager to add files to the
	 * plugin's classpath
	 */
	private final Object plugin;

	/**
	 * Creates a new Velocity library manager.
	 *
	 * @param dataDirectory plugin's data directory
	 * @param pluginManager Velocity plugin manager
	 * @param plugin        the plugin to manage
	 */
	public VelocityLibraryManager(Object plugin, Path dataDirectory, PluginManager pluginManager) {
		super(new File(dataDirectory.toFile().getParentFile().getParentFile(), "libraries").toPath());

		this.pluginManager = pluginManager;
		this.plugin = plugin;
	}

	/**
	 * Adds a file to the Velocity plugin's classpath.
	 *
	 * @param file the file to add
	 */
	@Override
	protected void addToClasspath(Path file) {
		this.pluginManager.addToClasspath(this.plugin, file);
	}

	@Override
	protected InputStream getResourceAsStream(String path) {
		return this.getClass().getClassLoader().getResourceAsStream(path);
	}
}
