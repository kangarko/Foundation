package org.mineacademy.fo.settings;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;

import lombok.NonNull;

/**
 * A special class that can store loaded {@link YamlConfig} files
 * <p>
 * DOES NOT INVOKE {@link YamlConfig#loadConfiguration(String, String)}
 * for you, you must invoke it by yourself as you otherwise normally would!
 *
 * @param <T>
 */
public final class ConfigItems<T extends YamlConfig> {

	/**
	 * A list of all loaded items
	 */
	private volatile StrictMap<String, T> loadedItemsMap = new StrictMap<>();

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
	 * How we are going to instantiate a single class from file?
	 *
	 * This is for advanced use only, by default, each config item is the same class
	 * for example in Boss plugin, each boss in bosses/ folder will make a Boss class.
	 *
	 * Examples where this can be useful: If you have a minigame plugin and want to store
	 * different minigames in one folder such as MobArena and BedWars both in games/ folder,
	 * then you will read the "Type" key in each arena file by opening the file name provided
	 * in the function as config and returning the specific arena class from a key in that file.
	 */
	private final Function<String, Class<T>> prototypeCreator;

	/**
	 * Are all items stored in a single file?
	 */
	private final boolean singleFile;

	/**
	 * Create a new config items instance
	 *
	 * @param type
	 * @param folder
	 * @param prototypeCreator
	 * @param singleFile
	 */
	private ConfigItems(String type, String folder, Function<String, Class<T>> prototypeCreator, boolean singleFile) {
		this.type = type;
		this.folder = folder;
		this.prototypeCreator = prototypeCreator;
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
		return fromFolder(folder, fileName -> prototypeClass);
	}

	/**
	 * Load items from the given folder
	 *
	 * @param <P>
	 * @param folder
	 * @param prototypeCreator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFolder(String folder, Function<String, Class<P>> prototypeCreator) {
		return new ConfigItems<>(folder.substring(0, folder.length() - (folder.endsWith("es") && !folder.contains("variable") ? 2 : folder.endsWith("s") ? 1 : 0)), folder, prototypeCreator, false);
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
		return fromFile(path, file, fileName -> prototypeClass);
	}

	/**
	 * Load items from the given YAML file path
	 *
	 * @param <P>
	 * @param path
	 * @param file
	 * @param prototypeCreator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFile(String path, String file, Function<String, Class<P>> prototypeCreator) {
		return new ConfigItems<>(path, file, prototypeCreator, true);
	}

	/**
	 * Load all item classes by creating a new instance of them and copying their folder from JAR to disk
	 */
	public void loadItems() {
		this.loadItems(null);
	}

	/**
	 * Load all item classes by creating a new instance of them and copying their folder from JAR to disk
	 *
	 * @param loader for advanced loading mechanisms, most people wont use this
	 */
	public void loadItems(@Nullable Function<File, T> loader) {

		// Clear old items
		this.loadedItemsMap.clear();

		if (this.singleFile) {
			final File file = FileUtil.extract(this.folder);
			final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

			if (config.isSet(this.type))
				for (final String name : config.getConfigurationSection(this.type).getKeys(false))
					this.loadOrCreateItem(name);
		} else {
			// Try copy items from our JAR
			if (!FileUtil.getFile(this.folder).exists())
				FileUtil.extractFolderFromJar(this.folder + "/", this.folder);

			// Load items on our disk
			final File[] files = FileUtil.getFiles(this.folder, "yml");

			for (final File file : files)
				if (loader != null)
					loader.apply(file);

				else {
					final String name = FileUtil.getFileName(file);

					this.loadOrCreateItem(name);
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
	 *                     which either can be a no args one or one taking a single argument, the name. If that is not
	 *                     sufficient, you can supply your custom instantiator here.
	 * @return
	 */
	public T loadOrCreateItem(@NonNull final String name, @Nullable Supplier<T> instantiator) {
		Valid.checkBoolean(!this.isItemLoaded(name), "Item " + (this.type == null ? "" : this.type + " ") + "named " + name + " already exists! Available: " + this.getItemNames());

		// Create a new instance of our item
		T item = null;

		try {

			if (instantiator != null)
				item = instantiator.get();

			else {
				Constructor<T> constructor;
				boolean nameConstructor = true;

				final Class<T> prototypeClass = this.prototypeCreator.apply(name);
				Valid.checkNotNull(prototypeClass);

				try {
					constructor = prototypeClass.getDeclaredConstructor(String.class);

				} catch (final Exception e) {
					constructor = prototypeClass.getDeclaredConstructor();
					nameConstructor = false;
				}

				Valid.checkBoolean(Modifier.isPrivate(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers()),
						"Your class " + prototypeClass + " must have a private or protected constructor taking a String or nothing!");

				constructor.setAccessible(true);

				try {
					if (nameConstructor)
						item = constructor.newInstance(name);
					else
						item = constructor.newInstance();

				} catch (final InstantiationException ex) {
					Common.throwError(ex, "Failed to create new" + (this.type == null ? prototypeClass.getSimpleName() : " " + this.type) + " " + name + " from " + constructor);
				}
			}

			// Register
			this.loadedItemsMap.put(name, item);

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load" + name + (this.singleFile ? "" : " from " + this.folder));
		}

		Valid.checkNotNull(item, "Failed to initiliaze " + name + " from " + this.folder);
		return item;
	}

	/**
	 * Remove the given item by instance
	 *
	 * @param item
	 */
	public void removeItem(@NonNull final T item) {
		this.removeItemByName(item.getName());
	}

	/**
	 * Remove the given item by instance
	 *
	 * @param name
	 */
	public void removeItemByName(@NonNull final String name) {
		final T item = this.findItem(name);
		Valid.checkNotNull(item, ChatUtil.capitalize(this.type) + " " + name + " not loaded. Available: " + this.getItemNames());

		if (this.singleFile)
			item.save("", null);
		else
			item.deleteFile();

		this.loadedItemsMap.remove(name);
	}

	/**
	 * Check if the given item by name is loaded
	 *
	 * @param name
	 * @return
	 */
	public boolean isItemLoaded(final String name) {
		return this.findItem(name) != null;
	}

	/**
	 * Return the item instance by name, or null if not loaded
	 *
	 * @param name
	 * @return
	 */
	public T findItem(@NonNull final String name) {
		final T item = this.loadedItemsMap.get(name);

		// Fallback to case insensitive
		if (item == null)
			for (final Map.Entry<String, T> entry : this.loadedItemsMap.entrySet())
				if (entry.getKey().equalsIgnoreCase(name))
					return entry.getValue();

		return item;
	}

	/**
	 * Return all loaded items
	 *
	 * @return
	 */
	public List<T> getItems() {
		return Collections.unmodifiableList(new ArrayList<>(this.loadedItemsMap.values()));
	}

	/**
	 * Return all loaded item names
	 *
	 * @return
	 */
	public Set<String> getItemNames() {
		return this.loadedItemsMap.keySet();
	}
}
