package org.mineacademy.fo.library;

import java.io.InputStream;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.plugin.PluginManager;

/**
 * A runtime dependency manager for Velocity plugins.
 */
public class VelocityLibraryManager extends LibraryManager {

	/**
	 * Velocity plugin manager used for adding files to the plugin's classpath
	 */
	@NotNull
	private final PluginManager pluginManager;

	/**
	 * The plugin instance required by the plugin manager to add files to the
	 * plugin's classpath
	 */
	@NotNull
	private final Object plugin;

	/**
	 * Creates a new Velocity library manager.
	 *
	 * @param dataDirectory plugin's data directory
	 * @param pluginManager Velocity plugin manager
	 * @param plugin        the plugin to manage
	 */
	public VelocityLibraryManager(Object plugin, Path dataDirectory, PluginManager pluginManager) {
		super(dataDirectory);

		this.pluginManager = pluginManager;
		this.plugin = plugin;
	}

	/**
	 * Adds a file to the Velocity plugin's classpath.
	 *
	 * @param file the file to add
	 */
	@Override
	protected void addToClasspath(@NotNull Path file) {
		this.pluginManager.addToClasspath(this.plugin, file);
	}

	@Override
	protected InputStream getResourceAsStream(@NotNull String path) {
		return this.getClass().getClassLoader().getResourceAsStream(path);
	}
}
