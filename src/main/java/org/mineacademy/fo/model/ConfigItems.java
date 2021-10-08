package org.mineacademy.fo.model;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.lang.WordUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.NonNull;

/**
 * A special class that can store loaded {@link YamlConfig} files
 *
 * DOES NOT INVOKE {@link YamlConfig#loadConfiguration(String, String)}
 * for you, you must invoke it by yourself as you otherwise normally would!
 *
 * @param <T>
 */
public final class ConfigItems<T extends YamlConfig> {

	/**
	 * A list of all loaded items
	 */
	private volatile StrictList<T> loadedItems = new StrictList<>();

	/**
	 * The item type this class stores, such as "variable, "format", or "arena class"
	 */
	private final String type;

	/**
	 * The folder name where the items are stored, this must be the same
	 * on both your JAR file and in your plugin folder, for example
	 * "classes/" in your JAR file and "classes/" in your plugin folder
	 */
	private final String folder;

	/**
	 * The class we are loading in the list
	 * <p>
	 * *MUST* have a private constructor without any arguments
	 */
	private final Class<T> prototypeClass;

	/**
	 * Are all items stored in a single file?
	 */
	private boolean singleFile = false;

	/**
	 * Create a new config items instance
	 *
	 * @param type
	 * @param folder
	 * @param prototypeClass
	 * @param hasDefaultPrototype
	 * @param singleFile
	 */
	private ConfigItems(String type, String folder, Class<T> prototypeClass, boolean singleFile) {
		this.type = type;
		this.folder = folder;
		this.prototypeClass = prototypeClass;
		this.singleFile = singleFile;
	}

	/**
	 * Load items from the given folder
	 *
	 * @param <P>
	 * @param folder
	 * @param prototypeClass
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFolder(String folder, Class<P> prototypeClass) {
		return new ConfigItems<>(folder.substring(0, folder.length() - (folder.endsWith("es") && !folder.contains("variable") ? 2 : 1)), folder, prototypeClass, false);
	}

	/**
	 * Load items from the given YAML file path
	 *
	 * @param <P>
	 * @param path
	 * @param file
	 * @param prototypeClass
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFile(String path, String file, Class<P> prototypeClass) {
		return new ConfigItems<>(path, file, prototypeClass, true);
	}

	/**
	 * Load all item classes by creating a new instance of them and copying their folder from JAR to disk
	 */
	public void loadItems() {

		// Clear old items
		loadedItems.clear();

		if (singleFile) {
			final File file = FileUtil.extract(this.folder);
			final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			//Valid.checkBoolean(config.isSet(this.type), "Unable to locate configuration section " + this.type + " in " + file);

			if (config.isSet(this.type))
				for (final String name : config.getConfigurationSection(this.type).getKeys(false))
					loadOrCreateItem(name);
		}

		else {
			// Try copy items from our JAR
			if (!FileUtil.getFile(folder).exists())
				FileUtil.extractFolderFromJar(folder + "/", folder);

			// Load items on our disk
			final File[] files = FileUtil.getFiles(folder, "yml");

			for (final File file : files) {
				final String name = FileUtil.getFileName(file);

				loadOrCreateItem(name);
			}
		}
	}

	/**
	 * Create the class (make new instance of) by the given name,
	 * the class must have a private constructor taking in the String (name) or nothing
	 *
	 * @param name
	 * @return
	 */
	public T loadOrCreateItem(@NonNull final String name) {
		return this.loadOrCreateItem(name, null);
	}

	/**
	 * Create the class (make new instance of) by the given name,
	 * the class must have a private constructor taking in the String (name) or nothing
	 *
	 * @param name
	 * @param instantiator by default we create new instances of your item by calling its constructor,
	 * 		  which either can be a no args one or one taking a single argument, the name. If that is not
	 * 		  sufficient, you can supply your custom instantiator here.
	 *
	 * @return
	 */
	public T loadOrCreateItem(@NonNull final String name, @Nullable Supplier<T> instantiator) {
		Valid.checkBoolean(!isItemLoaded(name), "Item " + (this.type == null ? "" : this.type + " ") + "named " + name + " already exists! Available: " + getItemNames());

		// Create a new instance of our item
		T item = null;

		try {

			if (instantiator != null)
				item = instantiator.get();

			else {
				Constructor<T> constructor;
				boolean nameConstructor = true;

				try {
					constructor = prototypeClass.getDeclaredConstructor(String.class);

				} catch (final Exception e) {
					constructor = prototypeClass.getDeclaredConstructor();
					nameConstructor = false;
				}

				Valid.checkBoolean(Modifier.isPrivate(constructor.getModifiers()), "Your class " + prototypeClass + " must have private constructor taking a String or nothing!");
				constructor.setAccessible(true);

				if (nameConstructor)
					item = constructor.newInstance(name);
				else
					item = constructor.newInstance();
			}

			// Register
			loadedItems.add(item);

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load" + (type == null ? prototypeClass.getSimpleName() : " " + type) + " " + name + " from " + folder);
		}

		Valid.checkNotNull(item, "Failed to initiliaze" + (type == null ? prototypeClass.getSimpleName() : " " + type) + " " + name + " from " + folder);
		return item;
	}

	/**
	 * Remove the given item by instance
	 *
	 * @param item
	 */
	public void removeItem(@NonNull final T item) {
		Valid.checkBoolean(isItemLoaded(item.getName()), WordUtils.capitalize(type) + " " + item.getName() + " not loaded. Available: " + getItemNames());

		item.delete();
		loadedItems.remove(item);
	}

	/**
	 * Check if the given item by name is loaded
	 *
	 * @param name
	 * @return
	 */
	public boolean isItemLoaded(final String name) {
		return findItem(name) != null;
	}

	/**
	 * Return the item instance by name, or null if not loaded
	 *
	 * @param name
	 * @return
	 */
	public T findItem(@NonNull final String name) {
		for (final T item : loadedItems)
			if (item.getName().equalsIgnoreCase(name))
				return item;

		return null;
	}

	/**
	 * Return all loaded items
	 *
	 * @return
	 */
	public List<T> getItems() {
		return Collections.unmodifiableList(loadedItems.getSource());
	}

	/**
	 * Return all loaded item names
	 *
	 * @return
	 */
	public List<String> getItemNames() {
		return Common.convert(loadedItems, T::getName);
	}
}
