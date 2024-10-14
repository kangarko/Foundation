package org.mineacademy.fo.settings;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ValidCore;

import lombok.NonNull;

/**
 * A special class that can store loaded {@link YamlConfig} files
 * <p>
 * DOES NOT INVOKE {@link YamlConfig#loadAndExtract(String, String)}
 * for you, you must invoke it by yourself as you otherwise normally would!
 *
 * @param <T>
 */
public final class ConfigItems<T extends YamlConfig> {

	/**
	 * A map and a list of all loaded items for performance
	 */
	private final Map<String, T> loadedItemsMap = new HashMap<>();
	private List<T> items = new ArrayList<>(); // non final, can be sorted later
	private final List<String> itemNames = new ArrayList<>();

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
	 * The custom function to sort items, null for A-B sorting.
	 */
	private final Function<List<T>, List<T>> listComparator;

	/**
	 * Create a new config items instance
	 *
	 * @param type
	 * @param folder
	 * @param prototypeCreator
	 * @param singleFile
	 * @param listComparator
	 */
	private ConfigItems(String type, String folder, Function<String, Class<T>> prototypeCreator, boolean singleFile, Function<List<T>, List<T>> listComparator) {
		this.type = type;
		this.folder = folder;
		this.prototypeCreator = prototypeCreator;
		this.singleFile = singleFile;
		this.listComparator = listComparator;
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
		return fromFolder(folder, prototypeClass, null);
	}

	/**
	 * Load items from the given folder
	 *
	 * @param <P>
	 * @param folder
	 * @param prototypeClass
	 * @param listComparator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFolder(String folder, Class<P> prototypeClass, Function<List<P>, List<P>> listComparator) {
		return fromFolder(folder, fileName -> prototypeClass, listComparator);
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
		return fromFolder(folder, prototypeCreator, null);
	}

	/**
	 * Load items from the given folder
	 *
	 * @param <P>
	 * @param folder
	 * @param prototypeCreator
	 * @param listComparator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFolder(String folder, Function<String, Class<P>> prototypeCreator, Function<List<P>, List<P>> listComparator) {
		return new ConfigItems<>(folder.substring(0, folder.length() - (folder.endsWith("es") && !folder.contains("variable") ? 2 : folder.endsWith("s") ? 1 : 0)), folder, prototypeCreator, false, listComparator);
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
	 * @param prototypeClass
	 * @param listComparator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFile(String path, String file, Class<P> prototypeClass, Function<List<P>, List<P>> listComparator) {
		return fromFile(path, file, fileName -> prototypeClass, listComparator);
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
		return fromFile(path, file, prototypeCreator, null);
	}

	/**
	 * Load items from the given YAML file path
	 *
	 * @param <P>
	 * @param path
	 * @param file
	 * @param prototypeCreator
	 * @param listComparator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFile(String path, String file, Function<String, Class<P>> prototypeCreator, Function<List<P>, List<P>> listComparator) {
		return new ConfigItems<>(path, file, prototypeCreator, true, listComparator);
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
	public void loadItems(Function<File, T> loader) {

		// Clear old items
		this.loadedItemsMap.clear();

		if (this.singleFile) {
			final File file = FileUtil.extract(this.folder);
			final YamlConfig config = YamlConfig.fromFile(file);

			if (config.isSet(this.type))
				for (final String name : config.getMap(this.type).keySet())
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
	public T loadOrCreateItem(@NonNull final String name, Supplier<T> instantiator) {
		ValidCore.checkBoolean(!this.isItemLoaded(name), "Item " + (this.type == null ? "" : this.type + " ") + "named " + name + " already exists! Available: " + this.getItemNames());

		// Create a new instance of our item
		T item = null;

		try {

			if (instantiator != null)
				item = instantiator.get();

			else {
				Constructor<T> constructor = null;
				boolean nameConstructor = true;

				final Class<T> prototypeClass = this.prototypeCreator.apply(name);
				ValidCore.checkNotNull(prototypeClass);

				try {
					constructor = prototypeClass.getDeclaredConstructor(String.class);

				} catch (final Throwable t) {
					try {
						constructor = prototypeClass.getDeclaredConstructor();

						nameConstructor = false;
					} catch (final Throwable tt) {
						// User forgot his constructor
					}
				}

				ValidCore.checkBoolean(constructor != null && (Modifier.isPrivate(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers())),
						"Your class " + prototypeClass + " must also have a private or a protected constructor taking a String or nothing! Found: " + constructor);

				constructor.setAccessible(true);

				try {
					if (nameConstructor)
						item = constructor.newInstance(name);
					else
						item = constructor.newInstance();

				} catch (final InstantiationException ex) {
					CommonCore.throwError(ex, "Failed to create new" + (this.type == null ? prototypeClass.getSimpleName() : " " + this.type) + " " + name + " from " + constructor);
				}
			}

			// Register
			this.loadedItemsMap.put(name, item);

			this.items.add(item);
			this.itemNames.add(name);

			// Custom reordering
			if (this.listComparator != null)
				this.items = this.listComparator.apply(this.items);

			Collections.sort(this.itemNames, String.CASE_INSENSITIVE_ORDER);

		} catch (final Throwable t) {
			CommonCore.throwError(t, "Failed to load" + name + (this.singleFile ? "" : " from " + this.folder));
		}

		ValidCore.checkNotNull(item, "Failed to initiliaze " + name + " from " + this.folder);
		return item;
	}

	/**
	 * Remove the given item by instance
	 *
	 * @param item
	 */
	public void removeItem(@NonNull final T item) {
		this.removeItemByName(item.getFileName());
	}

	/**
	 * Remove the given item by instance
	 *
	 * @param name
	 */
	public void removeItemByName(@NonNull final String name) {
		final T item = this.findItem(name);
		ValidCore.checkNotNull(item, ChatUtil.capitalize(this.type) + " " + name + " not loaded. Available: " + this.getItemNames());

		if (this.singleFile)
			item.save("", null);
		else
			item.getFile().delete();

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
	 * Return all loaded items, any modifications will be lost on reload
	 * Sorted using the list comparator in the static methods if set
	 *
	 * @return
	 */
	public List<T> getItems() {
		return this.items;
	}

	/**
	 * Return all loaded item names, any modifications will be lost on reload
	 *
	 * @return
	 */
	public List<String> getItemNames() {
		return this.itemNames;
	}
}