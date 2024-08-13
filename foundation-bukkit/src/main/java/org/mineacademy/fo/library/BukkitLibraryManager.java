package org.mineacademy.fo.library;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import lombok.NonNull;

/**
 * A runtime dependency manager for Bukkit plugins.
 *
 * @author https://github.com/jonesdevelopment/libby
 */
public final class BukkitLibraryManager extends LibraryManager {

	/**
	 * Plugin classpath helper
	 */
	private final URLClassLoaderHelper classLoader;

	private final Plugin plugin;

	/**
	 * Creates a new Bukkit library manager.
	 *
	 * @param plugin the plugin to manage
	 */
	public BukkitLibraryManager(@NonNull Plugin plugin) {
		super(Bukkit.getWorldContainer().toPath().resolve("libraries"));

		this.classLoader = new URLClassLoaderHelper((URLClassLoader) plugin.getClass().getClassLoader(), this);
		this.plugin = plugin;
	}

	/**
	 * Adds a file to the Bukkit plugin's classpath.
	 *
	 * @param file the file to add
	 */
	@Override
	protected void addToClasspath(@NonNull Path file) {
		this.classLoader.addToClasspath(file);
	}

	@Override
	protected InputStream getResourceAsStream(@NonNull String path) {
		return this.plugin.getResource(path);
	}
}
