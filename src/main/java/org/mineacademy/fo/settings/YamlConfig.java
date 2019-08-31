package org.mineacademy.fo.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * The core configuration class. Manages all settings files.
 *
 * @version 5.0 (of the previous ConfHelper)
 * @author kangarko
 */
public class YamlConfig {

	/**
	 * When you attempt to use {@link #get(String, Class)} or {@link #get(String, Class, Object)}
	 * for a null value, should we insert an empty {@link SerializedMap} to the deserialize method so that it is called?
	 *
	 * Defaults to false, which means if the value is not set, no deserialize is called, we just return null
	 */
	public static boolean DESERIALIZE_NULL = false;

	// ------------------------------------------------------------------------------------------------------------
	// Only allow one instance of file to be loaded for safety.
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * All files that are currently loaded
	 */
	private static volatile StrictMap<ConfigInstance, List<YamlConfig>> loadedFiles = new StrictMap<>();

	/**
	 * Clear the list of loaded files
	 */
	public static final void clearLoadedFiles() {
		loadedFiles.clear();
	}

	/**
	 * Remove a loaded file from {@link #loadedFiles}
	 *
	 * @param file
	 */
	public static final void unregisterLoadedFile(File file) {
		for (final ConfigInstance instance : loadedFiles.keySet()) {
			if (instance.equals(file)) {
				loadedFiles.remove(instance);

				break;
			}
		}
	}

	/**
	 * Looks up a {@link ConfigInstance} for the given file name.
	 *
	 * @param fileName
	 * @return
	 */
	protected static final ConfigInstance findInstance(String fileName) {
		for (final ConfigInstance instance : loadedFiles.keySet()) {
			if (instance.equals(fileName)) {
				Debugger.debug("config", "> Reusing instance of " + fileName + " = " + instance.getFile());

				return instance;
			}
		}

		Debugger.debug("config", "> Creating new instance for " + fileName);
		return null;
	}

	/**
	 * Add new {@link ConfigInstance}, used internally
	 *
	 * @param instance
	 * @param config
	 */
	private static final void addConfig(ConfigInstance instance, YamlConfig config) {
		List<YamlConfig> existing = loadedFiles.get(instance);

		if (existing == null)
			existing = new ArrayList<>();

		existing.add(config);
		loadedFiles.put(instance, existing);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static end /
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A null flag indicating there is no default 'to' config file
	 */
	public static final String NO_DEFAULT = null;

	/**
	 * The config file instance this config belongs to.
	 */
	private ConfigInstance instance;

	/**
	 * The config header
	 */
	private String[] header;

	/**
	 * The local path prefix to make things easier.
	 */
	private String pathPrefix = null;

	/**
	 * Internal flag whether to save the file after loading,
	 * set to true automatically when we edit it
	 */
	private boolean save = false;

	/**
	 * Internal flag that can be toggled to disable working with default files.
	 */
	@Setter
	private boolean usingDefaults = true;

	/**
	 * Internal flag to indicate whether you are calling this from {@link #loadConfiguration(String, String)}
	 */
	private boolean loading = false;

	protected YamlConfig() {
	}

	// ------------------------------------------------------------------------------------------------------------
	// Main loading methods.
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Loads up the localization file. These are stored in the localization/ folder
	 * and have the syntax "messages_" + prefix + ".yml".
	 *
	 * If the folder does not exists with the given file, we create it.
	 *
	 * This method is intended to be called from the static config!
	 *
	 * @param localePrefix
	 * @throws Exception
	 */
	protected final void loadLocalization(String localePrefix) throws Exception {
		Valid.checkNotNull(localePrefix, "locale cannot be null!");

		try {
			loading = true;

			final String localePath = "localization/messages_" + localePrefix + ".yml";
			final InputStream is = FileUtil.getInternalResource(localePath);

			if (is == null)
				throw new FoException(SimplePlugin.getNamed() + " does not support the localization: messages_" + localePrefix + ".yml (For custom locale, set the Locale to 'en' and edit your English file instead)");

			final File file = new File(SimplePlugin.getData(), localePath);
			ConfigInstance instance = findInstance(file.getName());

			if (instance == null) {

				if (!file.exists())
					FileUtil.extract(localePath, (line) -> replaceVariables(line, localePath));

				final YamlConfiguration config = FileUtil.loadConfigurationStrict(file);
				final YamlConfiguration defaultsConfig = Remain.loadConfiguration(is);

				Valid.checkBoolean(file != null && file.exists(), "Failed to load " + localePath + " from " + file);

				instance = new ConfigInstance(file, config, defaultsConfig);
				addConfig(instance, this);
			}

			this.instance = instance;

			onLoadFinish();
		} finally {
			loading = false;
		}

		saveIfNecessary0();
	}

	/**
	 * Load configuration from the give file name.
	 *
	 * The file must exist within out plugin at the same path
	 * since it will be copied to the plugin folder if it does not exist,
	 * and it will be used to server as the default config to serve updates from.
	 *
	 * @param file
	 */
	protected final void loadConfiguration(String file) {
		loadConfiguration(file, file);
	}

	/**
	 * Load configuration from the given path to another path.
	 *
	 * If the file does not exist, we create a new file.
	 * If you set "from" to null, no defaults will be used.
	 *
	 * Both paths must include file extension
	 *
	 * @param from, the origin path within the plugin jar, if null, no defaults are used
	 * @param to, the destination path in plugins/ThisPlugin/
	 */
	protected final void loadConfiguration(String from, String to) {
		Valid.checkNotNull(to, "File to path cannot be null!");
		Valid.checkBoolean(to.contains("."), "To path must contain file extension: " + to);

		if (from != null)
			Valid.checkBoolean(from.contains("."), "From path must contain file extension: " + from);

		try {
			loading = true;

			ConfigInstance instance = findInstance(to);

			if (instance == null) {
				final File file;
				final YamlConfiguration config;
				YamlConfiguration defaultsConfig = null;

				// We will have the default file to return to
				// This enables auto config update
				if (from != null) {
					final InputStream is = FileUtil.getInternalResource(from);
					Valid.checkNotNull(is, "Inbuilt resource not found: " + from);

					defaultsConfig = Remain.loadConfiguration(is);
					file = FileUtil.extract(false, from, to, (line) -> replaceVariables(line, to));
				}

				else
					file = FileUtil.getOrMakeFile(to);

				Valid.checkNotNull(file, "Failed to " + (from != null ? "copy settings from " + from + " to " : "read settings from ") + to);

				config = FileUtil.loadConfigurationStrict(file);
				instance = new ConfigInstance(file, config, defaultsConfig);

				addConfig(instance, this);
			}

			this.instance = instance;

			try {
				onLoadFinish();

			} catch (final Exception ex) {
				Common.logFramed(true,
						"Error loading configuration in " + getFileName() + "!",
						"Problematic section: " + Common.getOrDefault(getPathPrefix(), "''"),
						"Problem: " + ex + " (see below for more)");

				Remain.sneaky(ex);
			}
		} finally {
			loading = false;
		}

		saveIfNecessary0();
	}

	/**
	 * Saves the file if changes have been made
	 */
	private final void saveIfNecessary0() {
		if (save) {
			save();

			save = false;
		}
	}

	/**
	 * Replace variables in the file and write it again. For details see {@link #replaceVariables(String, String)}
	 *
	 * @param file
	 */
	private final void rewriteVariablesIn(File file) {
		final List<String> lines = FileUtil.readLines(file);

		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);

			lines.set(i, replaceVariables(line, file.getName()));
		}

		FileUtil.write(file, lines, StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * Replace variables in the destination file before it is copied.
	 * Variables include {plugin.name} (lowercase), {file} and {file.lowercase}
	 * as well as custom variables from {@link #replaceVariables(String)} method
	 *
	 * @param file
	 */
	private final String replaceVariables(String line, String fileName) {
		line = line.replace("{plugin.name}", SimplePlugin.getNamed().toLowerCase());
		line = line.replace("{file}", fileName);
		line = line.replace("{file.lowercase}", fileName.toLowerCase());

		return line;
	}

	/**
	 * Called after the settings file has been loaded.
	 */
	protected void onLoadFinish() {
	}

	/**
	 * Return the Bukkit YAML instance of the config file
	 *
	 * @return
	 */
	protected final YamlConfiguration getConfig() {
		return instance.getConfig();
	}

	/**
	 * Return the Bukkit YAML instance of defaults file, or null if not set
	 *
	 * @return
	 */
	@Nullable
	protected final YamlConfiguration getDefaults() {
		return instance.getDefaultConfig();
	}

	/**
	 * Return the file name of this config
	 *
	 * @return
	 */
	protected final String getFileName() {
		return instance.getFile().getName();
	}

	/**
	 * Set the header of this configuration, use {@link #save()} to save
	 *
	 * @param header
	 */
	protected final void setHeader(String... header) {
		this.header = header;
	}

	// ------------------------------------------------------------------------------------
	// Main manipulation methods
	// ------------------------------------------------------------------------------------

	/**
	 * Saves the content of this config into the file
	 */
	public final void save() {
		if (loading) {
			// If we are loading only set the flag to save to save it later together
			if (!save)
				save = true;

			return;
		}

		final String file = getFileName();
		onSave();

		instance.save(header != null ? header : file.equals(FoConstants.File.DATA) ? FoConstants.Header.DATA_FILE : FoConstants.Header.UPDATED_FILE);
		rewriteVariablesIn(instance.getFile());

		Debugger.debug("config", "&eSaved updated file: " + file + " (# Comments removed)");
	}

	/**
	 * Called automatically when the file is saved
	 */
	protected void onSave() {
	}

	/**
	 * Removes the file on the disk
	 */
	public final void delete() {
		instance.delete();
	}

	/**
	 * Without saving changes, load the file from this disk and load its configuration again
	 */
	public final void reload() {
		try {
			instance.reload();

			onLoadFinish();
			saveIfNecessary0();

		} catch (final Exception e) {
			Common.error(e, "Failed to reload " + getFileName());
		}
	}

	// ------------------------------------------------------------------------------------
	// Configuration Getters
	// ------------------------------------------------------------------------------------

	/**
	 * Main configuration getter. Retrieve a value at a certain path, using type safety and
	 * default configuration.
	 *
	 * The type safety checks if the value is actually of the requested type, throwing errors
	 * if not.
	 *
	 * If {@link #getDefaults()} is set, and the value does not exist, we update the config
	 * file automatically.
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	private final <T> T getT(String path, Class<T> type) {
		Valid.checkNotNull(path, "Path cannot be null");
		path = formPathPrefix(path);

		Valid.checkBoolean(!path.contains(".."), "Path must not contain '..' or more: " + path);
		Valid.checkBoolean(!path.endsWith("."), "Path must not end with '.': " + path);

		// Copy defaults if not set
		// Also logs out the console message about when we save this change
		addDefaultIfNotExist(path, type);

		Object raw = getConfig().get(path);

		// Ensure that the default config actually did have the value, if used
		if (getDefaults() != null)
			Valid.checkNotNull(raw, "Failed to insert value at '" + path + "' from default config");

		// Ensure the value is of the given type
		if (raw != null) {

			// Workaround for empty lists
			if (raw.equals("[]") && type == List.class)
				raw = new ArrayList<>();

			checkAssignable(false, path, raw, type);
		}

		return (T) raw;
	}

	/**
	 * Attempts to find the "public static T deserialize(SerializedMap) " method in the class type to return the given
	 * path as the given class type,
	 *
	 * if that fails then we try to look for "public static T getByName(String)" method in the given type class,
	 *
	 * if that fails than we attempt to deserialize it using {@link SerializeUtil#deserialize(Class, Object)} method
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	protected final <T> T get(String path, Class<T> type) {
		return get(path, type, null);
	}

	/**
	 * Attempts to find the "public static T deserialize(SerializedMap) " method in the class type to return the given
	 * path as the given class type,
	 *
	 * if that fails then we try to look for "public static T getByName(String)" method in the given type class,
	 *
	 * if that fails than we attempt to deserialize it using {@link SerializeUtil#deserialize(Class, Object)} method
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param def
	 * @return
	 */
	protected final <T> T get(String path, Class<T> type, T def) {
		final Object object = convertIfNull(type, getT(path, Object.class));

		return object != null ? SerializeUtil.deserialize(type, object) : def;
	}

	/**
	 * Basically the same as {@link #get(String, Class)} however you can pass your own deserialize arguments here,
	 * see {@link SerializeUtil#deserialize(Class, Object, Object...)}
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param deserializeArguments
	 * @return
	 */
	protected final <T> T getWithData(String path, Class<T> type, Object... deserializeArguments) {
		final Object object = convertIfNull(type, getT(path, Object.class));

		return object != null ? SerializeUtil.deserialize(type, object, deserializeArguments) : null;
	}

	//
	// If there is no object set at a path, we convert it into an empty map, allowing
	// you to invoke deserialize for empty map to use default values instead
	//
	private final Object convertIfNull(Class<?> type, Object object) {
		if (DESERIALIZE_NULL) {
			if (object == null && ConfigSerializable.class.isAssignableFrom(type))
				object = new SerializedMap();

			if ("".equals(object) && Enum.class.isAssignableFrom(type))
				object = null;
		}

		return object;
	}

	/**
	 * Get an unknown object with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Object getObject(String path, Object def) {
		final Object result = getObject(path);
		forceSingleDefaults(path, def);

		return Common.getOrDefault(result, def);
	}

	/**
	 * Get an unknown object
	 *
	 * @param path
	 * @return
	 */
	protected final Object getObject(String path) {
		return getT(path, Object.class);
	}

	/**
	 * Return an enum at this location
	 *
	 * @param path
	 * @param type
	 * @param <T>
	 * @return
	 * @deprecated use {@link #get(String, Class)}
	 */
	@Deprecated
	protected final <T> T getEnum(String path, Class<T> type) {
		return get(path, type);
	}

	/**
	 * Get a boolean with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Boolean getBoolean(String path, boolean def) {
		forceSingleDefaults(path, def);

		return isSet(path) ? getBoolean(path) : def;
	}

	/**
	 * Get a boolean
	 *
	 * @param path
	 * @return
	 */
	protected final Boolean getBoolean(String path) {
		return getT(path, Boolean.class);
	}

	/**
	 * Get a string with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final String getString(String path, String def) {
		forceSingleDefaults(path, def);

		return isSet(path) ? getString(path) : def;
	}

	/**
	 * Return a replacer for localizable messages
	 *
	 * @param path
	 * @return
	 */
	protected final Replacer getReplacer(String path) {
		return getReplacer(path, "");
	}

	/**
	 * Return a replacer for localizable messages
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Replacer getReplacer(String path, String def) {
		final String message = getString(path);

		return Replacer.of(Common.getOrDefault(message, def));
	}

	/**
	 * Get a string
	 *
	 * @param path
	 * @return
	 */
	protected final String getString(String path) {
		return getT(path, String.class);
	}

	/**
	 * Get a long with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Long getLong(String path, Long def) {
		forceSingleDefaults(path, def);

		return isSet(path) ? getLong(path) : def;
	}

	/**
	 * Get a long
	 *
	 * @param path
	 * @return
	 */
	protected final Long getLong(String path) {
		return getT(path, Long.class);
	}

	/**
	 * Get an integer with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Integer getInteger(String path, Integer def) {
		forceSingleDefaults(path, def);

		return isSet(path) ? getInteger(path) : def;
	}

	/**
	 * Get an integer
	 *
	 * @param path
	 * @return
	 */
	protected final Integer getInteger(String path) {
		return getT(path, Integer.class);
	}

	/**
	 * Get a double from any number, failsafe
	 *
	 * @param path
	 * @return
	 */
	protected final Double getDoubleSafe(String path) {
		final String raw = getObject(path).toString();

		return raw != null ? Double.parseDouble(raw) : null;
	}

	/**
	 * Get a double with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Double getDouble(String path, Double def) {
		forceSingleDefaults(path, def);

		return isSet(path) ? getDouble(path) : def;
	}

	/**
	 * Get a double number
	 *
	 * @param path
	 * @return
	 */
	protected final Double getDouble(String path) {
		return getT(path, Double.class);
	}

	/**
	 * Get a Bukkit location, using our serialization method
	 *
	 * @param path
	 * @return
	 */
	protected final Location getLocation(String path) {
		return SerializeUtil.deserializeLocation(getObject(path));
	}

	/**
	 * Get a sound with volume and a pitch
	 *
	 * @param path
	 * @return
	 */
	protected final SimpleSound getSound(String path) {
		return new SimpleSound(getString(path));
	}

	/**
	 * Get a casus for those human languages who support it, mainly used for numbers
	 *
	 * @param path
	 * @return
	 */
	protected final CasusHelper getCasus(String path) {
		return new CasusHelper(getString(path));
	}

	/**
	 * Get a title message, having title and a subtitle
	 *
	 * @param path
	 * @return
	 */
	protected final TitleHelper getTitle(String path) {
		return new TitleHelper(path);
	}

	/**
	 * Get a time value in human readable format, eg. "20 minutes" or "45 ticks"
	 *
	 * @param path
	 * @return
	 */
	protected final TimeHelper getTime(String path) {
		return new TimeHelper(path);
	}

	/**
	 * Get a boxed message having full-width top and bottom lines in chat
	 *
	 * @param path
	 * @return
	 */
	protected final BoxedMessage getBoxedMessage(String path) {
		return new BoxedMessage(getString(path));
	}

	/**
	 * Get a CompMaterial which is our cross-version compatible material class
	 *
	 * @param path
	 * @return
	 */
	protected final CompMaterial getMaterial(String path) {
		final String name = getString(path);

		if (name == null)
			return null;

		return CompMaterial.fromStringStrict(name);
	}

	/**
	 * Get a list of unknown values
	 *
	 * @param path
	 * @param of
	 * @return
	 */
	protected final List<Object> getList(String path) {
		return getT(path, List.class);
	}

	/**
	 * Return a list of hash maps at the given location
	 *
	 * @param path
	 * @return list of maps, or empty map if not set
	 */
	protected final List<SerializedMap> getMapList(String path) {
		return getListSafe(path, SerializedMap.class);
	}

	/**
	 * @see #getList(String, Class), except that this method
	 * never returns null, instead, if the key is not present,
	 * we return an empty set instead of null
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> Set<T> getSetSafe(String key, Class<T> type) {
		final Set<T> list = getSet(key, type);

		return Common.getOrDefault(list, new HashSet<>());
	}

	/**
	 * @see #getList(String, Class)
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> Set<T> getSet(String key, Class<T> type) {
		final List<T> list = getList(key, type);

		return list == null ? null : new HashSet<>(list);
	}

	/**
	 * @see #getList(String, Class), except that this method
	 * never returns null, instead, if the key is not present,
	 * we return an empty set instead of null
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> List<T> getListSafe(String key, Class<T> type) {
		final List<T> list = getList(key, type);

		return Common.getOrDefault(list, new ArrayList<>());
	}

	/**
	 * Return a list of objects of the given type
	 *
	 * If the type is your own class make sure to put public static deserialize(SerializedMap)
	 * method into it that returns the class object from the map!
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	protected final <T> List<T> getList(String path, Class<T> type) {
		if (!isSet(path))
			return null;

		final List<Object> objects = getList(path);
		final List<T> list = new ArrayList<>();

		for (final Object object : objects)
			list.add(SerializeUtil.deserialize(type, object));

		return list;
	}

	/**
	 * Get a simple string array
	 *
	 * @param path
	 * @return the given array, or an empty array
	 */
	protected final String[] getStringArray(String path) {
		return isSet(path) ? String.join("\n", getObject(path).toString()).split("\n") : new String[0];
	}

	/**
	 * Get a string list
	 *
	 * @param path
	 * @return the found list, or an empty list
	 */
	protected final List<String> getStringList(String path) {
		final List<Object> list = getList(path);

		return list != null ? fixYamlBooleansInList(list) : new ArrayList<>();
	}

	/**
	 * Attempts to convert objects into strings, since SnakeYAML parser interprets
	 * "true" and "yes" as boolean types
	 *
	 * @param list
	 * @return
	 */
	private final List<String> fixYamlBooleansInList(@NonNull Iterable<Object> list) {
		final List<String> newList = new ArrayList<>();

		for (final Object obj : list)
			if (obj != null)
				newList.add(obj.toString());

		return newList;
	}

	/**
	 * Get a list of command aliases (special usage in settings.yml)
	 *
	 * @param path
	 * @return
	 */
	protected final StrictList<String> getCommandList(String path) {
		final List<String> list = getStringList(path);
		Valid.checkBoolean(!list.isEmpty(), "Please set at least one command alias in '" + path + "' (" + getFileName() + ") for this will be used as your main command!");

		return new StrictList<>(list);
	}

	/**
	 * Get a list of Materials
	 *
	 * @param path
	 * @return
	 */
	protected final StrictList<Material> getMaterialList(String path) {
		final StrictList<Material> list = new StrictList<>();

		for (final String raw : getStringList(path)) {
			final CompMaterial mat = CompMaterial.fromStringCompat(raw);

			if (mat != null)
				list.add(mat.getMaterial());
		}

		return list;
	}

	/**
	 * Get a list of enchantments
	 *
	 * @param path
	 * @return
	 */
	protected final StrictList<Enchantment> getEnchants(String path) {
		final StrictList<Enchantment> list = new StrictList<>();

		for (final String name : getStringList(path))
			list.add(ItemUtil.findEnchantment(name));

		return list;
	}

	/**
	 * Get a list of enumerations
	 *
	 * @param <E>
	 * @param path
	 * @param listType
	 * @return
	 */
	protected final <E extends Enum<E>> StrictList<E> getEnumList(String path, Class<E> listType) {
		final StrictList<E> list = new StrictList<>();

		for (final String item : getStringList(path))

			// Infinite list, return empty
			if (item.equals("*"))
				return new StrictList<>();

			else if (listType == Material.class) {
				final Material mat = CompMaterial.fromStringCompat(item).getMaterial();

				if (mat != null)
					list.add((E) mat);

			} else if (listType == CompMaterial.class) {
				final CompMaterial mat = CompMaterial.fromStringCompat(item);

				if (mat != null)
					list.add((E) mat);

			} else {

				// Compatibility workaround because we have DROWNED in our default config but it does not exist in old MC
				if (listType == SpawnReason.class && "DROWNED".equals(item) && MinecraftVersion.olderThan(V.v1_13))
					continue;

				list.add(ReflectionUtil.lookupEnum(listType, item));
			}

		return list;
	}

	/**
	 * Get a serialized map
	 *
	 * @param path
	 * @return map, or empty map
	 */
	protected final SerializedMap getMap(String path) {
		return isSet(path) ? SerializedMap.of(Common.getMapFromSection(getT(path, Object.class))) : new SerializedMap();
	}

	/**
	 * Get a map of values and keys
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	protected final <Key, Value> LinkedHashMap<Key, Value> getMap(String path, Class<Key> keyType, Class<Value> valueType) {
		return getMap(path, keyType, valueType, null);
	}

	/**
	 * Get a map of values and keys
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param valueType
	 * @param def
	 * @return
	 */
	protected final <Key, Value> LinkedHashMap<Key, Value> getMap(String path, Class<Key> keyType, Class<Value> valueType, Map<Key, Value> def) {
		Valid.checkNotNull(path, "Path cannot be null");

		if (pathPrefix != null)
			if (!path.startsWith(pathPrefix))
				path = formPathPrefix(path);

		// add default
		if (getDefaults() != null && !getConfig().isSet(path)) {
			Valid.checkBoolean(getDefaults().isSet(path), "Default '" + getFileName() + "' lacks a map at " + path);

			for (final String key : getDefaults().getConfigurationSection(path).getKeys(false))
				addDefaultIfNotExist(path + "." + key, valueType);
		}

		final LinkedHashMap<Key, Value> keys = new LinkedHashMap<>();

		final Object pathObject = getConfig().get(path);

		if (pathObject == null)
			if (def != null)
				return new LinkedHashMap<>(def);

			else
				throw new FoException("Map not found at " + path + " in " + getFileName());

		Valid.checkBoolean(getConfig().isConfigurationSection(path), "Must be section at '" + path + "', got " + pathObject);

		for (final Map.Entry<String, Object> entry : getConfig().getConfigurationSection(path).getValues(false).entrySet()) {
			final Object key = entry.getKey();
			final Object val = entry.getValue();

			Valid.checkBoolean(!keys.containsKey(key), "Duplicate key " + key + " in " + path);

			checkAssignable(false, path, val, valueType);

			final Key parsed = SerializeUtil.deserialize(keyType, key); //(Key) (keyType == Integer.class && key instanceof String ? Integer.parseInt(key.toString()) : key);
			final Value parsedValue = SerializeUtil.deserialize(valueType, val);

			keys.put(parsed, parsedValue);
		}

		return keys;
	}

	/**
	 * Get a map assuming each key contains a map of string and objects
	 *
	 * @deprecated special case in few plugins only
	 * @param path
	 * @return
	 */
	@Deprecated
	protected final LinkedHashMap<String, LinkedHashMap<String, Object>> getValuesAndKeys(String path) {
		Valid.checkNotNull(path, "Path cannot be null");
		path = formPathPrefix(path);

		// add default
		if (getDefaults() != null && !getConfig().isSet(path)) {
			Valid.checkBoolean(getDefaults().isSet(path), "Default '" + getFileName() + "' lacks a section at " + path);

			for (final String name : getDefaults().getConfigurationSection(path).getKeys(false)) {
				for (final String setting : getDefaults().getConfigurationSection(path + "." + name).getKeys(false)) {
					addDefaultIfNotExist(path + "." + name + "." + setting, Object.class);
				}
			}
		}

		Valid.checkBoolean(getConfig().isSet(path), "Malfunction copying default section to " + path);

		// key, values assigned to the key
		final TreeMap<String, LinkedHashMap<String, Object>> groups = new TreeMap<>();

		for (final String name : getConfig().getConfigurationSection(path).getKeys(false)) {
			// type, value (UNPARSED)
			final LinkedHashMap<String, Object> valuesRaw = getMap(path + "." + name, String.class, Object.class);

			groups.put(name, valuesRaw);
		}

		return new LinkedHashMap<>(groups);
	}

	// ------------------------------------------------------------------------------------
	// Configuration Setters
	// ------------------------------------------------------------------------------------

	/**
	 * Sets a certain key with value and saves the file
	 *
	 * @param path
	 * @param value
	 */
	protected final void save(String path, Object value) {
		setNoSave(path, value);

		save();
	}

	/**
	 * Set a default key-value pair to the used file (not the default one) if it does not exist
	 *
	 * @param path
	 * @param value
	 */
	protected final void setIfNotExist(String path, Object value) {
		if (!isSet(path))
			setNoSave(path, value);
	}

	/**
	 * Sets a certain key with value, serialized
	 *
	 * The file is not saved, however it is marked for save so it is saved
	 * at the end of a loading cycle, if you call {@link #loadConfiguration(String)} or {@link #loadLocalization(String)}
	 *
	 * @param path
	 * @param value
	 */
	protected final void setNoSave(String path, Object value) {
		path = formPathPrefix(path);
		value = SerializeUtil.serialize(value);

		getConfig().set(path, value);

		save = true; // Schedule save for later anyways
	}

	/**
	 * Moves a certain config key or section from one path to another
	 *
	 * @param fromRelative
	 * @param toAbsolute
	 */
	protected final void move(String fromRelative, String toAbsolute) {
		move(getObject(fromRelative), fromRelative, toAbsolute);
	}

	/**
	 * Moves a certain config key from one path to another
	 *
	 * @param value
	 * @param fromPathRel
	 * @param toPathAbs
	 */
	protected final void move(Object value, String fromPathRel, String toPathAbs) {
		final String oldPathPrefix = pathPrefix;

		fromPathRel = formPathPrefix(fromPathRel);
		getConfig().set(fromPathRel, null);

		pathPrefix = oldPathPrefix; // set to previous

		checkAndFlagForSave(toPathAbs, value, false);
		getConfig().set(toPathAbs, value);

		Common.log("&7Update " + getFileName() + ". Move &b\'&f" + fromPathRel + "&b\' &7(was \'" + value + "&7\') to " + "&b\'&f" + toPathAbs + "&b\'" + "&r");

		pathPrefix = oldPathPrefix; // and reset back to whatever it was
	}

	/**
	 * A special method that converts a section within a SerializedMap to a different type.
	 *
	 * Automatically saves the config at the end
	 *
	 * @param <O>
	 * @param <N>
	 * @param path
	 * @param mapSection
	 * @param from
	 * @param to
	 * @param converter
	 */
	protected final <O, N> void convertMapList(String path, String mapSection, Class<O> from, Class<N> to, Function<O, N> converter) {
		final List<SerializedMap> list = new ArrayList<>();

		for (final SerializedMap classMap : getMapList(path)) {
			classMap.convert(mapSection, from, to, converter);

			list.add(classMap);
		}

		save(path, list);
	}

	/**
	 * Convert the given config section to an alternative type
	 *
	 * Automatically saves the config at the end
	 *
	 * @param <O>
	 * @param <N>
	 * @param path
	 * @param from
	 * @param to
	 * @param converter
	 */
	protected final <O, N> void convert(String path, Class<O> from, Class<N> to, Function<O, N> converter) {
		final Object old = getObject(path);

		if (old != null) {
			// If the old is a collection check if the first value is old, assume the rest is old as well
			if (old instanceof Collection) {
				final Collection<?> collection = (Collection) old;

				if (collection.isEmpty() || !from.isAssignableFrom(collection.iterator().next().getClass()))
					return;

				final List<N> newCollection = new ArrayList<>();

				for (final O oldItem : (Collection<O>) collection)
					newCollection.add(converter.apply(oldItem));

				save(path, newCollection);

				Common.log("&7Converted '" + path + "' from " + from.getSimpleName() + "[] to " + to.getSimpleName() + "[]");

			} else if (from.isAssignableFrom(old.getClass())) {
				save(path, converter.apply((O) old));

				Common.log("&7Converted '" + path + "' from '" + from.getSimpleName() + "' to '" + to.getSimpleName() + "'");
			}
		}
	}

	/**
	 * Return whether a key exists or not at the given path
	 *
	 * @param path, the path to the key with path prefix added automatically
	 * @return
	 */
	protected final boolean isSet(String path) {
		return isSetAbsolute(formPathPrefix(path));
	}

	/**
	 * Return whether a key exists or not at the given absolute path
	 *
	 * @param path, the path to the key without adding path prefix automatically
	 * @return
	 */
	protected final boolean isSetAbsolute(String path) {
		return getConfig().isSet(path);
	}

	/**
	 * Return whether the default config exist in your plugins jar and contains the
	 * given relative path using {@link #getPathPrefix()} feature
	 *
	 * @param path
	 * @return
	 */
	protected final boolean isSetDefault(String path) {
		return isSetDefaultAbsolute(formPathPrefix(path));
	}

	/**
	 * Return whether the default config exist in your plugins jar and contains the
	 * given absolute path
	 *
	 * @param path
	 * @return
	 */
	protected final boolean isSetDefaultAbsolute(String path) {
		return getDefaults() != null && getDefaults().isSet(path);
	}

	// ------------------------------------------------------------------------------------
	// Lazy helpers
	// ------------------------------------------------------------------------------------

	/**
	 * Place the key from the default settings if those are set and the key does not exists
	 *
	 * @param pathAbs
	 */
	protected final void addDefaultIfNotExist(String pathAbs) {
		addDefaultIfNotExist(pathAbs, Object.class);
	}

	/**
	 * Places a key from the default config into the current config file
	 *
	 * @param pathAbs
	 * @param type
	 */
	private final void addDefaultIfNotExist(String pathAbs, Class<?> type) {
		if (usingDefaults && getDefaults() != null && !isSetAbsolute(pathAbs)) {
			final Object object = getDefaults().get(pathAbs);

			Valid.checkNotNull(object, "Default '" + getFileName() + "' lacks " + Common.article(type.getSimpleName()) + " at '" + pathAbs + "'");
			checkAssignable(true, pathAbs, object, type);

			checkAndFlagForSave(pathAbs, object);
			getConfig().set(pathAbs, object);
		}
	}

	/**
	 * Throws an error if using default settings AND defining the def parameter at the same time.
	 *
	 * We do not allow that, please call methods without the def parameter when using default config
	 * as the default key will be fetchd directly from the default config.
	 *
	 * @param path
	 * @param def
	 */
	private final void forceSingleDefaults(String path, Object def) {
		if (def != null && getDefaults() != null)
			throw new FoException("Cannot use get method with default when getting " + formPathPrefix(path) + " and using a default config for " + getFileName());
	}

	/**
	 * Checks if the file instance exists and is a valid file, checks if default exists,
	 * sets the save flag to true and logs the update
	 *
	 * @param <T>
	 * @param path
	 * @param def
	 */
	private final <T> void checkAndFlagForSave(String path, T def) {
		checkAndFlagForSave(path, def, true);
	}

	/**
	 * Checks if the file instance exists and is a valid file, checks if default exists,
	 * sets the save flag to true and logs the update
	 *
	 * @param <T>
	 * @param path
	 * @param def
	 * @param logUpdate
	 */
	private final <T> void checkAndFlagForSave(String path, T def, boolean logUpdate) {
		Valid.checkBoolean(instance.getFile() != null && instance.getFile().exists() && instance.getConfig() != null, "Inbuilt file or config is null! File: " + instance.getFile() + ", config: " + instance.getConfig());

		if (getDefaults() != null)
			Valid.checkNotNull(def, "Inbuilt config " + getFileName() + " lacks " + (def == null ? "key" : def.getClass().getSimpleName()) + " at \"" + path + "\". Is it outdated?");

		if (logUpdate)
			Common.log("&7Update " + getFileName() + " at &b\'&f" + path + "&b\' &7-> " + (def == null ? "&ckey removed" : "&b\'&f" + def + "&b\'") + "&r");

		save = true;
	}

	/**
	 * Checks if the clazz parameter can be assigned to the given value
	 *
	 * @param fromDefault
	 * @param path
	 * @param value
	 * @param clazz
	 */
	private final void checkAssignable(boolean fromDefault, String path, Object value, Class<?> clazz) {
		if (!clazz.isAssignableFrom(value.getClass()) && !clazz.getSimpleName().equals(value.getClass().getSimpleName()))
			throw new FoException("Malformed configuration! Key '" + path + "' in " + (fromDefault ? "inbuilt " : "") + getFileName() + " must be " + clazz.getSimpleName() + " but got " + value.getClass().getSimpleName() + ": '" + value + "'");
	}

	// ------------------------------------------------------------------------------------
	// Path prefix
	// ------------------------------------------------------------------------------------

	/**
	 * Adds path prefix to the given path
	 *
	 * @param path
	 * @return
	 */
	protected String formPathPrefix(@NonNull String path) {
		final String prefixed = pathPrefix != null ? pathPrefix + (!path.isEmpty() ? "." + path : "") : path;

		return prefixed.endsWith(".") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;
	}

	/**
	 * Sets path prefix to the given path prefix
	 *
	 * @param pathPrefix
	 */
	protected void pathPrefix(String pathPrefix) {
		if (pathPrefix != null) {
			Valid.checkBoolean(!pathPrefix.endsWith("."), "Path prefix must not end with a dot: " + pathPrefix);
			Valid.checkBoolean(!pathPrefix.endsWith(".yml"), "Path prefix must not end with .yml!");
		}

		this.pathPrefix = pathPrefix != null && !pathPrefix.isEmpty() ? pathPrefix : null;
	}

	/**
	 * Returns {@link #pathPrefix}
	 *
	 * @return
	 */
	protected final String getPathPrefix() {
		return pathPrefix;
	}

	// ------------------------------------------------------------------------------------
	// Classes helpers
	// ------------------------------------------------------------------------------------

	/**
	 * A simple helper class for some language-specific values
	 * when creating localization for numbers.
	 */
	public final class CasusHelper {
		private final String akuzativSg; // 1   second (Slovak case - sekundu, not in English)
		private final String akuzativPl; // 2-4 seconds (Slovak case - sekundy)
		private final String genitivPl; // 5+  seconds (Slovak case - sekund)

		private CasusHelper(String raw) {
			final String[] values = raw.split(", ");

			if (values.length == 2) {
				akuzativSg = values[0];
				akuzativPl = values[1];
				genitivPl = akuzativPl;
				return;
			}

			if (values.length != 3)
				throw new FoException("Malformed type, use format: 'second, seconds' OR 'sekundu, sekundy, sekund' (if your language has it)");

			akuzativSg = values[0];
			akuzativPl = values[1];
			genitivPl = values[2];
		}

		public String getPlural() {
			return genitivPl;
		}

		public String formatWithCount(long count) {
			return count + " " + formatWithoutCount(count);
		}

		public String formatWithoutCount(long count) {
			if (count == 1)
				return akuzativSg;
			if (count > 1 && count < 5)
				return akuzativPl;

			return genitivPl;
		}
	}

	/**
	 * A simple helper class for storing title messages
	 */
	public final class TitleHelper {
		private final String title, subtitle;

		private TitleHelper(String path) {
			title = Common.colorize(getString(path + ".Title"));
			subtitle = Common.colorize(getString(path + ".Subtitle"));
		}

		/**
		 * Duration: 4 seconds + 2 second fade in
		 */
		public void playLong(Player player, Function<String, String> replacer) {
			play(player, 5, 4 * 20, 15, replacer);
		}

		/**
		 * Duration: 2 seconds + 1 second fade in
		 */
		public void playShort(Player player, Function<String, String> replacer) {
			play(player, 3, 2 * 20, 5, replacer);
		}

		public void play(Player player, int fadeIn, int stay, int fadeOut, Function<String, String> replacer) {
			Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer.apply(title), replacer.apply(subtitle));
		}
	}

	/**
	 * A simple helper class for storing time
	 */
	@Getter
	public final class TimeHelper {
		private final String raw;
		private final int timeTicks;

		private TimeHelper(String path) {
			final String str = getObject(path).toString().equals("0") ? "0" : getString(path);

			raw = str;
			timeTicks = (int) TimeUtil.toTicks(raw);
		}

		@Override
		public String toString() {
			return raw;
		}
	}
}

/**
 * For safe read-write access we only store one opened file of the same name.
 *
 * This represents the access to that file.
 */
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
class ConfigInstance {

	/**
	 * The file this configuration belongs to.
	 */
	private final File file;

	/**
	 * Our config we are manipulating.
	 */
	private final YamlConfiguration config;

	/**
	 * The default config we reach out to fill values from.
	 */
	private final YamlConfiguration defaultConfig;

	/**
	 * Saves the config instance with the given header, can be null
	 *
	 * @param header
	 */
	protected void save(String[] header) {

		if (header != null) {
			config.options().copyHeader(true);
			config.options().header(String.join("\n", header));
		}

		try {
			config.save(file);

		} catch (final IOException e) {
			Common.error(e, "Failed to save " + file.getName());
		}
	}

	/**
	 * Loads the config file again without saving changes
	 *
	 * @throws Exception
	 */
	protected void reload() throws Exception {
		config.load(file);
	}

	/**
	 * Removes the config file from the disk
	 */
	protected void delete() {
		YamlConfig.unregisterLoadedFile(file);

		file.delete();
	}

	/**
	 * Returns true if the given file name equals to the one we store here
	 *
	 * @param file
	 * @return
	 */
	public boolean equals(File file) {
		return equals((Object) file);
	}

	/**
	 * Returns true if the given file name equals to the one we store here
	 *
	 * @param fileName
	 * @return
	 */
	public boolean equals(String fileName) {
		return equals((Object) fileName);
	}

	/**
	 * Returns true if the given object is a ConfigInstance having the same file name,
	 * or a file with the same name as this config instance
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ConfigInstance ? ((ConfigInstance) obj).file.getName().equals(this.file.getName()) : obj instanceof File ? ((File) obj).getName().equals(this.file.getName()) : obj instanceof String ? ((String) obj).equals(this.file.getName()) : false;
	}
}