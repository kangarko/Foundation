package org.mineacademy.fo.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.MissingEnumException;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * The core configuration class. Manages all settings files.
 *
 * @author kangarko
 * @version 5.0 (of the previous ConfHelper)
 */
public class YamlConfig {

	// ------------------------------------------------------------------------------------------------------------
	// Only allow one instance of file to be loaded for safety.
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A null flag indicating there is no default 'to' config file
	 *
	 * <p>
	 * When you call methods with the "def" parameter, we enforce this flag and will
	 * NOT auto update the config with the specified default value
	 */
	public static final String NO_DEFAULT = null;

	/**
	 * All files that are currently loaded
	 */
	private static volatile StrictSet<ConfigInstance> loadedFiles = new StrictSet<>();

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

	// ------------------------------------------------------------------------------------------------------------
	// Static end /
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Internal flag whether to save the file after loading, set to true
	 * automatically when we edit it
	 */
	private boolean save = false;

	/**
	 * Internal flag that can be toggled to disable working with default files.
	 *
	 */
	private boolean useDefaults = true;

	/**
	 * Internal flag to indicate whether you are calling this from
	 * {@link #loadConfiguration(String, String)}
	 */
	private boolean loading = false;

	/**
	 * Internal flag to indicate whether {@link #loadConfiguration(String)} has been called
	 */
	private boolean loaded = false;

	/**
	 * Should we check for validity of the config key-value pair?
	 */
	private final boolean checkAssignables = true;

	protected YamlConfig() {
	}

	/**
	 * Clear the list of loaded files
	 */
	public static final void clearLoadedFiles() {
		synchronized (loadedFiles) {
			loadedFiles.clear();
		}
	}

	/**
	 * Remove a loaded file from {@link #loadedFiles}
	 *
	 * @param file
	 */
	public static final void unregisterLoadedFile(final File file) {
		synchronized (loadedFiles) {
			for (final ConfigInstance instance : loadedFiles)
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
	protected static final ConfigInstance findInstance(final String fileName) {
		synchronized (loadedFiles) {
			for (final ConfigInstance instance : loadedFiles)
				if (instance.equals(fileName))
					return instance;

			return null;
		}
	}

	/**
	 * Add new {@link ConfigInstance}, used internally
	 *
	 * @param instance
	 * @param config
	 */
	private static void addConfig(final ConfigInstance instance, final YamlConfig config) {
		synchronized (loadedFiles) {
			Valid.checkBoolean(!config.loaded, "Config " + config.getClass() + " for file " + instance.getFile() + " has already been loaded: " + Debugger.traceRoute(true));

			loadedFiles.add(instance);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Main loading methods.
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Loads up the localization file. These are stored in the localization/ folder
	 * and have the syntax "messages_" + prefix + ".yml".
	 *
	 * <p>
	 * If the folder does not exists with the given file, we create it.
	 *
	 * <p>
	 * This method is intended to be called from the static config!
	 *
	 * @param localePrefix
	 * @throws Exception
	 */
	protected final void loadLocalization(final String localePrefix) throws Exception {

		synchronized (loadedFiles) {
			Valid.checkNotNull(localePrefix, "locale cannot be null!");

			try {
				loading = true;

				final String localePath = "localization/messages_" + localePrefix + ".yml";
				final InputStream is = FileUtil.getInternalResource(localePath);
				Valid.checkNotNull(is, SimplePlugin.getNamed() + " does not support the localization: messages_" + localePrefix + ".yml (For custom locale, set the Locale to 'en' and edit your English file instead)");

				final File file = new File(SimplePlugin.getData(), localePath);
				ConfigInstance instance = findInstance(file.getName());

				if (instance == null) {
					if (!file.exists()) {
						FileUtil.extract(localePath);

						// Reformat afterwards with comments engine
						if (saveComments())
							save = true;
					}

					final SimpleYaml config = FileUtil.loadConfigurationStrict(file);
					final SimpleYaml defaultsConfig = Remain.loadConfiguration(is);

					Valid.checkBoolean(file != null && file.exists(), "Failed to load " + localePath + " from " + file);

					instance = new ConfigInstance(file, config, defaultsConfig, saveComments(), getUncommentedSections(), localePath);
					addConfig(instance, this);
				}

				this.instance = instance;

				onLoadFinish();

				loaded = true;

			} finally {
				loading = false;
			}

			saveIfNecessary0();
		}
	}

	/**
	 * Load configuration from the give file name.
	 *
	 * <p>
	 * The file must exist within out plugin at the same path since it will be
	 * copied to the plugin folder if it does not exist, and it will be used to
	 * server as the default config to serve updates from.
	 *
	 * @param file
	 */
	protected final void loadConfiguration(final String file) {
		loadConfiguration(file, file);
	}

	/**
	 * Load configuration from the given path to another path.
	 *
	 * <p>
	 * If the file does not exist, we create a new file. If you set "from" to null,
	 * no defaults will be used.
	 *
	 * <p>
	 * Both paths must include file extension
	 *
	 * @param from, the origin path within the plugin jar, if null, no defaults are
	 *              used
	 * @param to,   the destination path in plugins/ThisPlugin/
	 */
	public final void loadConfiguration(final String from, final String to) {

		synchronized (loadedFiles) {

			Valid.checkBoolean(!loading, "Duplicate call to loadConfiguration (already loading)");
			Valid.checkNotNull(to, "File to path cannot be null!");
			Valid.checkBoolean(to.contains("."), "To path must contain file extension: " + to);

			if (from != null)
				Valid.checkBoolean(from.contains("."), "From path must contain file extension: " + from);
			else
				useDefaults = false;

			try {
				loading = true;

				ConfigInstance instance = findInstance(to);

				if (instance == null) {
					final File file;
					final SimpleYaml config;
					SimpleYaml defaultsConfig = null;

					// Reformat afterwards with comments engine
					if (!new File(SimplePlugin.getInstance().getDataFolder(), to).exists() && saveComments())
						save = true;

					// We will have the default file to return to
					if (from != null) {
						final InputStream is = FileUtil.getInternalResource(from);
						Valid.checkNotNull(is, "Inbuilt resource not found: " + from);

						defaultsConfig = Remain.loadConfiguration(is);
						file = FileUtil.extract(from, to);

					} else
						file = FileUtil.getOrMakeFile(to);

					Valid.checkNotNull(file, "Failed to " + (from != null ? "copy settings from " + from + " to " : "read settings from ") + to);

					config = FileUtil.loadConfigurationStrict(file);

					instance = new ConfigInstance(file, config, defaultsConfig, saveComments(), getUncommentedSections(), from == null ? to : from);
					addConfig(instance, this);
				}

				this.instance = instance;

				try {
					onLoadFinish();

				} catch (final Exception ex) {
					Common.throwError(ex, "Error loading configuration in " + getFileName() + "!", "Problematic section: " + Common.getOrDefault(getPathPrefix(), "''"), "Problem: " + ex + " (see below for more)");
				}

				loaded = true;

			} finally {
				loading = false;
			}

			saveIfNecessary0();
		}
	}

	/**
	 * Saves the file if changes have been made
	 */
	private void saveIfNecessary0() {

		// We want to save the file if the save is pending or if there are no defaults
		if (save || saveComments()) {
			save();

			save = false;
		}
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
	protected final SimpleYaml getConfig() {
		Valid.checkNotNull(instance, "Cannot call getConfig when no instance is set!");

		return instance.getConfig();
	}

	/**
	 * Return the Bukkit YAML instance of defaults file, or null if not set
	 *
	 * @return
	 */

	protected final SimpleYaml getDefaults() {
		Valid.checkNotNull(instance, "Cannot call getDefaults when no instance is set!");

		return instance.getDefaultConfig();
	}

	/**
	 * Return the file name of this config
	 *
	 * @return
	 */
	protected final String getFileName() {
		Valid.checkNotNull(instance, "Instance for " + getClass() + " is null");
		Valid.checkNotNull(instance.getFile(), "Instance file in " + getClass() + " is null");

		return instance.getFile().getName();
	}

	/**
	 * Set the header of this configuration, use {@link #save()} to save
	 *
	 * @param header
	 */
	protected final void setHeader(final String... header) {
		this.header = header;
	}

	/**
	 * Return the name of this file without the extension
	 *
	 * @return
	 */
	public String getName() {
		return FileUtil.getFileName(instance.getFile());
	}

	/**
	 * Return the file corresponding with these settings
	 *
	 * @return
	 */
	public final File getFile() {
		return instance.getFile();
	}

	// ------------------------------------------------------------------------------------
	// Main manipulation methods
	// ------------------------------------------------------------------------------------

	/**
	 * Saves the content of this config into the file
	 */
	public void save() {
		if (loading) {
			save = true;

			return;
		}

		onSave();

		{ // Save automatically
			final SerializedMap map = serialize();

			if (map != null)
				for (final Map.Entry<String, Object> entry : map.entrySet())
					setNoSave(entry.getKey(), entry.getValue());
		}

		instance.save(header != null ? header : getFileName().equals(FoConstants.File.DATA) ? FoConstants.Header.DATA_FILE : FoConstants.Header.UPDATED_FILE);
	}

	/**
	 * Called automatically when the file is saved
	 */
	protected void onSave() {
	}

	/**
	 * Called automatically on save, use this to put
	 * things you want saved in your file and they will then be automatically
	 * saved when call save() method
	 *
	 * @return
	 */
	protected SerializedMap serialize() {
		return null;
	}

	/**
	 * Removes the file on the disk
	 */
	public final void delete() {
		instance.delete();
	}

	/**
	 * Without saving changes, load the file from this disk and load its
	 * configuration again
	 */
	public final void reload() {
		try {
			instance.reload();

			save = true;

			onLoadFinish();
			saveIfNecessary0();

		} catch (final Exception e) {
			Common.error(e, "Failed to reload " + getFileName());
		}
	}

	/**
	 * Shall we attempt to save comments into this yaml config
	 * and enforce the file to always look like the default one?
	 *
	 * You can exclude sections you do not want to symlink in {@link #getUncommentedSections()}
	 *
	 * Defaults to false.
	 *
	 * @return
	 */
	protected boolean saveComments() {
		return false;
	}

	/**
	 * If {@link #SAVE_COMMENTS} is on, what sections should we ignore from
	 * being updated/enforced commands?
	 *
	 * E.g. In ChatControl people can add their own channels so we make the
	 * "Channels.List" ignored so that peoples' channels (new sections) won't get
	 * remove.
	 *
	 * None by default.
	 *
	 * @return
	 */
	protected List<String> getUncommentedSections() {
		return null;
	}

	// ------------------------------------------------------------------------------------
	// Configuration Getters
	// ------------------------------------------------------------------------------------

	/**
	 * Main configuration getter. Retrieve a value at a certain path, using type
	 * safety and default configuration.
	 *
	 * <p>
	 * The type safety checks if the value is actually of the requested type,
	 * throwing errors if not.
	 *
	 * <p>
	 * If {@link #getDefaults()} is set, and the value does not exist, we update the
	 * config file automatically.
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	private <T> T getT(String path, final Class<T> type) {

		Debugger.debug("config", "Called get() '" + path + "' = '" + this.getConfig().get(path) + "' " + (this.getDefaults() != null ? "vs def = '" + this.getDefaults().get(path) + "'" : "no defaults") + ". Disk config contains: " + this.getConfig().getValues(true));

		Valid.checkNotNull(path, "Path cannot be null");
		path = formPathPrefix(path);

		// Commented out : players with their names starting with a dot will cause plugins malfunction
		//Valid.checkBoolean(!path.contains(".."), "Path must not contain '..' or more: " + path);
		Valid.checkBoolean(!path.endsWith("."), "Path must not end with '.': " + path);

		// Copy defaults if not set
		// Also logs out the console message about when we save this change
		addDefaultIfNotExist(path, type);

		Object raw = getConfig().get(path);

		// Ensure that the default config actually did have the value, if used
		if (useDefaults && getDefaults() != null)
			Valid.checkNotNull(raw, "Failed to insert value at '" + path + "' from default config");

		// Ensure the value is of the given type
		if (raw != null) {

			// Workaround for empty lists
			if (raw.equals("[]") && type == List.class)
				raw = new ArrayList<>();

			// Retype manually
			if (type == Long.class && raw instanceof Integer)
				raw = (long) raw;

			checkAssignable(false, path, raw, type);
		}

		return (T) raw;
	}

	/**
	 * Attempts to find the "public static T deserialize(SerializedMap) " method in
	 * the class type to return the given path as the given class type,
	 *
	 * <p>
	 * if that fails then we try to look for "public static T getByName(String)"
	 * method in the given type class,
	 *
	 * <p>
	 * if that fails than we attempt to deserialize it using
	 * {@link SerializeUtil#deserialize(Class, Object)} method
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	protected final <T> T get(final String path, final Class<T> type) {
		return get(path, type, null);
	}

	/**
	 * Attempts to find the "public static T deserialize(SerializedMap) " method in
	 * the class type to return the given path as the given class type,
	 *
	 * <p>
	 * if that fails then we try to look for "public static T getByName(String)"
	 * method in the given type class,
	 *
	 * <p>
	 * if that fails than we attempt to deserialize it using
	 * {@link SerializeUtil#deserialize(Class, Object)} method
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param def
	 * @return
	 */
	protected final <T> T get(final String path, final Class<T> type, final T def) {
		final Object object = getT(path, Object.class);

		return object != null ? SerializeUtil.deserialize(type, object) : def;
	}

	/**
	 * Basically the same as {@link #get(String, Class)} however you can pass your
	 * own deserialize arguments here, see
	 * {@link SerializeUtil#deserialize(Class, Object, Object...)}
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param deserializeArguments
	 * @return
	 */
	protected final <T> T getWithData(final String path, final Class<T> type, final Object... deserializeArguments) {
		final Object object = getT(path, Object.class);

		return object != null ? SerializeUtil.deserialize(type, object, deserializeArguments) : null;
	}

	/**
	 * Get an unknown object with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Object getObject(final String path, final Object def) {
		forceSingleDefaults(path);

		return isSet(path) ? getObject(path) : def;
	}

	/**
	 * Get an unknown object
	 *
	 * @param path
	 * @return
	 */
	protected final Object getObject(final String path) {
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
	protected final <T> T getEnum(final String path, final Class<T> type) {
		return get(path, type);
	}

	/**
	 * Get a boolean with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Boolean getBoolean(final String path, final boolean def) {
		forceSingleDefaults(path);

		final boolean set = isSet(path);
		Debugger.debug("config", "\tGetting Boolean at '" + path + "', " + (set ? "set to = " + getBoolean(path) : "not set, returning default " + def));

		return isSet(path) ? getBoolean(path) : def;
	}

	/**
	 * Get a boolean
	 *
	 * @param path
	 * @return
	 */
	protected final Boolean getBoolean(final String path) {
		return getT(path, Boolean.class);
	}

	/**
	 * Get a string with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final String getString(final String path, final String def) {
		forceSingleDefaults(path);

		return isSet(path) ? getString(path) : def;
	}

	/**
	 * Get a string
	 *
	 * @param path
	 * @return
	 */
	protected final String getString(final String path) {
		final Object object = getObject(path);

		if (object == null)
			return null;

		else if (object instanceof List)
			return Common.join((List<?>) object, "\n");

		else if (object instanceof String[])
			return Common.join(Arrays.asList((String[]) object), "\n");

		else if (object instanceof Boolean
				|| object instanceof Integer
				|| object instanceof Long
				|| object instanceof Double
				|| object instanceof Float)
			return Objects.toString(object);

		else if (object instanceof String)
			return (String) object;

		throw new FoException("Excepted string at '" + path + "' in " + getFileName() + ", got (" + object.getClass() + "): " + object);
	}

	/**
	 * Get a long with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Long getLong(final String path, final Long def) {
		forceSingleDefaults(path);

		return isSet(path) ? getLong(path) : def;
	}

	/**
	 * Get a long
	 *
	 * @param path
	 * @return
	 */
	protected final Long getLong(final String path) {
		return getT(path, Long.class);
	}

	/**
	 * Get an integer with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Integer getInteger(final String path, final Integer def) {
		forceSingleDefaults(path);

		final boolean set = isSet(path);
		Debugger.debug("config", "\tGetting Integer at '" + path + "', " + (set ? "set to = " + getInteger(path) : "not set, returning default " + def));

		return isSet(path) ? getInteger(path) : def;
	}

	/**
	 * Get an integer
	 *
	 * @param path
	 * @return
	 */
	protected final Integer getInteger(final String path) {
		return getT(path, Integer.class);
	}

	/**
	 * Get a double from any number, failsafe
	 *
	 * @param path
	 * @return
	 *
	 * @deprecated use {@link #getDouble(String)}
	 */
	@Deprecated
	protected final Double getDoubleSafe(final String path) {
		return getDouble(path);
	}

	/**
	 * Get a double with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Double getDouble(final String path, final Double def) {
		forceSingleDefaults(path);

		return isSet(path) ? getDouble(path) : def;
	}

	/**
	 * Get a double number
	 *
	 * @param path
	 * @return
	 */
	protected final Double getDouble(final String path) {
		final Object raw = getObject(path);

		return raw != null ? Double.parseDouble(raw.toString()) : null;
	}

	/**
	 * Return a replacer for localizable messages
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Replacer getReplacer(final String path, final String def) {
		forceSingleDefaults(path);

		return isSet(path) ? getReplacer(path) : Replacer.of(def);
	}

	/**
	 * Return a replacer for localizable messages
	 *
	 * @param path
	 * @return
	 */
	protected final Replacer getReplacer(final String path) {
		return Replacer.of(getString(path));
	}

	/**
	 * Get location list at the given config path
	 *
	 * @param path
	 * @return
	 */
	protected final LocationList getLocations(final String path) {
		return new LocationList(this, getList(path, Location.class));
	}

	/**
	 * Get a Bukkit location, using our serialization method
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final Location getLocation(final String path, final Location def) {
		forceSingleDefaults(path);

		return isSet(path) ? getLocation(path) : def;
	}

	/**
	 * Get a Bukkit location, using our serialization method
	 *
	 * @param path
	 * @return
	 */
	protected final Location getLocation(final String path) {
		return get(path, Location.class);
	}

	/**
	 * Get a sound with volume and a pitch
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final SimpleSound getSound(final String path, final SimpleSound def) {
		forceSingleDefaults(path);

		return isSet(path) ? getSound(path) : def;
	}

	/**
	 * Get a sound with volume and a pitch
	 *
	 * @param path
	 * @return
	 */
	protected final SimpleSound getSound(final String path) {
		return new SimpleSound(getString(path));
	}

	/**
	 * Get a casus for those human languages that support it, mainly used for
	 * numbers
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final CasusHelper getCasus(final String path, final CasusHelper def) {
		forceSingleDefaults(path);

		return isSet(path) ? getCasus(path) : def;
	}

	/**
	 * Get a casus for those human languages that support it, mainly used for
	 * numbers
	 *
	 * @param path
	 * @return
	 */
	protected final CasusHelper getCasus(final String path) {
		return new CasusHelper(getString(path));
	}

	/**
	 * Get a title message, having title and a subtitle
	 *
	 * @param path
	 * @param defTitle
	 * @param defSubtitle
	 * @return
	 */
	protected final TitleHelper getTitle(final String path, final String defTitle, final String defSubtitle) {
		forceSingleDefaults(path);

		return isSet(path) ? getTitle(path) : new TitleHelper(defTitle, defSubtitle);
	}

	/**
	 * Get a title message, having title and a subtitle
	 *
	 * @param path
	 * @return
	 */
	protected final TitleHelper getTitle(final String path) {
		return new TitleHelper(path);
	}

	/**
	 * Get a time value in human readable format, eg. "20 minutes" or "45 ticks"
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final <T extends SimpleTime> T getTime(final String path, final String def) {
		forceSingleDefaults(path);

		return isSet(path) ? getTime(path) : def != null ? (T) SimpleTime.from(def) : null;
	}

	/**
	 * Get a time value in human readable format, eg. "20 minutes" or "45 ticks"
	 *
	 * @param path
	 * @return
	 */
	protected final <T extends SimpleTime> T getTime(final String path) {
		final Object obj = getObject(path);

		return obj != null ? (T) SimpleTime.from(obj.toString()) : null;
	}

	/**
	 * Get a boxed message having full-width top and bottom lines in chat
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final BoxedMessage getBoxedMessage(final String path, final String def) {
		forceSingleDefaults(path);

		return isSet(path) ? getBoxedMessage(path) : new BoxedMessage(def);
	}

	/**
	 * Get a boxed message having full-width top and bottom lines in chat
	 *
	 * @param path
	 * @return
	 */
	protected final BoxedMessage getBoxedMessage(final String path) {
		return new BoxedMessage(getString(path));
	}

	/**
	 * Get a CompMaterial which is our cross-version compatible material class
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected final CompMaterial getMaterial(final String path, final CompMaterial def) {
		forceSingleDefaults(path);

		return isSet(path) ? getMaterial(path) : def;
	}

	/**
	 * Get a CompMaterial which is our cross-version compatible material class
	 *
	 * @param path
	 * @return
	 */
	protected final CompMaterial getMaterial(final String path) {
		final String name = getString(path);

		return name == null ? null : CompMaterial.fromStringStrict(name);
	}

	/**
	 * Get a list of unknown values
	 *
	 * @param path
	 * @param of
	 * @return
	 */
	protected final List<Object> getList(final String path) {
		final List<Object> list = getT(path, List.class);

		return Common.getOrDefault(list, new ArrayList<>());
	}

	/**
	 * Return a list of hash maps at the given location
	 *
	 * @param path
	 * @return list of maps, or empty map if not set
	 */
	protected final List<SerializedMap> getMapList(final String path) {
		return getList(path, SerializedMap.class);
	}

	/**
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 * @see #getList(String, Class), except that this method never returns null,
	 * instead, if the key is not present, we return an empty set instead of
	 * null
	 * @deprecated use {@link #getSet(String, Class)} for the same behavior
	 */
	@Deprecated
	protected final <T> Set<T> getSetSafe(final String key, final Class<T> type) {
		return getSet(key, type);
	}

	/**
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 * @see #getList(String, Class)
	 */
	protected final <T> Set<T> getSet(final String key, final Class<T> type) {
		final List<T> list = getList(key, type);

		return list == null ? new HashSet<>() : new HashSet<>(list);
	}

	/**
	 * Return a list of objects of the given type
	 *
	 * <p>
	 * If the type is your own class make sure to put public static
	 * deserialize(SerializedMap) method into it that returns the class object from
	 * the map!
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	protected final <T> List<T> getList(final String path, final Class<T> type) {
		return getList(path, type, (Object[]) null);
	}

	/**
	 * Return a list of objects of the given type
	 *
	 * <p>
	 * If the type is your own class make sure to put public static
	 * deserialize(SerializedMap, deserializedParameters) method into it that
	 * returns the class object from the map!
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param deserializeParameters
	 * @return
	 */
	protected final <T> List<T> getList(final String path, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = new ArrayList<>();
		final List<Object> objects = getList(path);

		if (objects != null)
			for (final Object object : objects)
				list.add(object != null ? SerializeUtil.deserialize(type, object, deserializeParameters) : null);

		return list;
	}

	/**
	 * Return a list of enumerations that are checked for some special values
	 * we use in our plugins that will break on older Minecraft versions, so we
	 * just ignore them and do not throw any error.
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 *
	 * @return
	 * @deprecated this code is specifically targeted for our plugins only
	 */
	@Deprecated
	protected final <T extends Enum<T>> List<T> getCompatibleEnumList(final String path, final Class<T> type) {
		final StrictList<T> list = new StrictList<>();
		final List<String> enumNames = getStringList(path);

		if (enumNames.size() == 1 && "*".equals(enumNames.get(0)))
			return list.getSource();

		if (enumNames != null)
			for (final String enumName : enumNames) {
				T parsedEnum = null;

				try {
					parsedEnum = ReflectionUtil.lookupEnumSilent(type, enumName);

				} catch (final IllegalArgumentException | MissingEnumException ex) {

					// Only throw an exception if the user has put in malformed value
					if (!LegacyEnum.isIncompatible(type, enumName))
						throw ex;
				}

				if (parsedEnum != null)
					list.add(parsedEnum);
			}

		return list.getSource();
	}

	/**
	 * Get a simple string array
	 *
	 * @param path
	 * @return the given array, or an empty array
	 */
	protected final String[] getStringArray(final String path) {
		final Object array = getObject(path);

		if (array == null)
			return new String[0];

		else if (array instanceof String)
			return ((String) array).split("\n");

		else if (array instanceof List)
			return Common.join((List<?>) array, "\n").split("\n");

		else if (array instanceof String[])
			return (String[]) array;

		throw new FoException("Excepted string or string list at '" + path + "' in " + getFileName() + ", got (" + array.getClass() + "): " + array);
	}

	/**
	 * Get a string list
	 *
	 * @param path
	 * @return the found list, or an empty list
	 */
	protected final List<String> getStringList(final String path) {
		final Object raw = getObject(path);

		if (raw == null)
			return new ArrayList<>();

		if (raw instanceof String) {
			final String output = (String) raw;

			return "'[]'".equals(output) || "[]".equals(output) ? new ArrayList<>() : Arrays.asList(output);
		}

		if (raw instanceof List)
			return fixYamlBooleansInList((List<Object>) raw);

		throw new FoException("Excepted a list at '" + path + "' in " + getFileName() + ", got (" + raw.getClass() + "): " + raw);
	}

	/**
	 * Attempts to convert objects into strings, since SnakeYAML parser interprets
	 * "true" and "yes" as boolean types
	 *
	 * @param list
	 * @return
	 */
	private List<String> fixYamlBooleansInList(@NonNull final Iterable<Object> list) {
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
	protected final StrictList<String> getCommandList(final String path) {
		final List<String> list = getStringList(path);
		Valid.checkBoolean(!list.isEmpty(), "Please set at least one command alias in '" + path + "' (" + getFileName() + ") for this will be used as your main command!");

		for (int i = 0; i < list.size(); i++) {
			String command = list.get(i);

			command = command.startsWith("/") ? command.substring(1) : command;
			list.set(i, command);
		}

		return new StrictList<>(list);
	}

	/**
	 * Get a list of Materials
	 *
	 * @param path
	 * @return
	 */
	protected final StrictList<CompMaterial> getMaterialList(final String path) {
		final StrictList<CompMaterial> list = new StrictList<>();

		for (final String raw : getStringList(path)) {
			final CompMaterial mat = CompMaterial.fromStringCompat(raw);

			if (mat != null)
				list.add(mat);
		}

		return list;
	}

	/**
	 * Get a list of enchantments
	 *
	 * @param path
	 * @return
	 */
	protected final StrictList<Enchantment> getEnchants(final String path) {
		final StrictList<Enchantment> list = new StrictList<>();

		for (final String name : getStringList(path))
			list.add(ItemUtil.findEnchantment(name));

		return list;
	}

	/**
	 * Get a serialized map
	 *
	 * @param path
	 * @return map, or empty map
	 */
	protected final SerializedMap getMap(final String path) {
		final LinkedHashMap<?, ?> map = getMap(path, Object.class, Object.class);

		return SerializedMap.of(map);
	}

	/**
	 * Load a map with preserved order from the given path. Each key in the map
	 * must match the given key/value type and will be deserialized
	 * <p>
	 * We will add defaults if applicable
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param valueType
	 * @param valueParameter
	 * @return
	 */
	protected final <Key, Value> LinkedHashMap<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, Value> map = new LinkedHashMap<>();

		final SimpleYaml config = getConfig();
		final SimpleYaml defaults = getDefaults();

		// Add path prefix right away
		path = formPathPrefix(path);

		// Add defaults
		if (defaults != null && !config.isSet(path)) {
			Valid.checkBoolean(defaults.isSet(path), "Default '" + getFileName() + "' lacks a map at " + path);

			for (final String key : defaults.getConfigurationSection(path).getKeys(false))
				addDefaultIfNotExist(path + "." + key, valueType);
		}

		// Load key-value pairs from config to our map
		final ConfigurationSection configSection = config.getConfigurationSection(path);

		if (configSection != null)
			for (final Map.Entry<String, Object> entry : configSection.getValues(false).entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final Value value = SerializeUtil.deserialize(valueType, entry.getValue());

				// Ensure the pair values are valid for the given paramenters
				checkAssignable(false, path, key, keyType);
				checkAssignable(false, path, value, valueType);

				map.put(key, value);
			}

		return map;
	}

	/**
	 * Load a map having a Set as value with the given parameters
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param setType
	 * @return
	 */
	protected final <Key, Value> LinkedHashMap<Key, Set<Value>> getMapSet(@NonNull String path, final Class<Key> keyType, final Class<Value> setType) {
		// The map we are creating, preserve order
		final LinkedHashMap<Key, Set<Value>> map = new LinkedHashMap<>();

		final SimpleYaml config = getConfig();
		final SimpleYaml defaults = getDefaults();

		// Add path prefix right away
		path = formPathPrefix(path);

		// Add defaults
		if (defaults != null && !config.isSet(path)) {
			Valid.checkBoolean(defaults.isSet(path), "Default '" + getFileName() + "' lacks a map at " + path);

			for (final String key : defaults.getConfigurationSection(path).getKeys(false))
				addDefaultIfNotExist(path + "." + key, setType);
		}

		// Load key-value pairs from config to our map
		final ConfigurationSection configSection = config.getConfigurationSection(path);

		if (configSection != null)
			for (final Map.Entry<String, Object> entry : configSection.getValues(false).entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final List<Value> value = SerializeUtil.deserialize(List.class, entry.getValue());

				// Ensure the pair values are valid for the given paramenters
				checkAssignable(false, path, key, keyType);

				if (!value.isEmpty())
					for (final Value item : value)
						checkAssignable(false, path, item, setType);

				map.put(key, new HashSet<>(value));
			}

		return map;
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
	protected final void save(final String path, final Object value) {
		setNoSave(path, value);

		save();
	}

	/**
	 * Set a default key-value pair to the used file (not the default one) if it
	 * does not exist
	 *
	 * @param path
	 * @param value
	 */
	protected final void setIfNotExist(final String path, final Object value) {
		if (!isSet(path))
			setNoSave(path, value);

	}

	/**
	 * Sets a certain key with value, serialized
	 *
	 * <p>
	 * The file is not saved, however it is marked for save so it is saved at the
	 * end of a loading cycle, if you call {@link #loadConfiguration(String)} or
	 * {@link #loadLocalization(String)}
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
	protected final void move(final String fromRelative, final String toAbsolute) {
		move(getObject(fromRelative), fromRelative, toAbsolute);
	}

	/**
	 * Moves a certain config key from one path to another
	 *
	 * @param value
	 * @param fromPathRel
	 * @param toPathAbs
	 */
	protected final void move(final Object value, String fromPathRel, final String toPathAbs) {
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
	 * A special method that converts a section within a SerializedMap to a
	 * different type.
	 *
	 * <p>
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
	protected final <O, N> void convertMapList(final String path, final String mapSection, final Class<O> from, final Class<N> to, final Function<O, N> converter) {
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
	 * <p>
	 * Automatically saves the config at the end
	 *
	 * @param <O>
	 * @param <N>
	 * @param path
	 * @param from
	 * @param to
	 * @param converter
	 */
	@SuppressWarnings("rawtypes")
	protected final <O, N> void convert(final String path, final Class<O> from, final Class<N> to, final Function<O, N> converter) {
		final Object old = getObject(path);

		if (old != null)
			// If the old is a collection check if the first value is old, assume the rest
			// is old as well
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

	protected final <T> T getOrSetDefault(final String path, final T defaultValue) {
		if (isSet(path)) {
			if (defaultValue instanceof Replacer)
				return (T) Replacer.of(getString(path));

			return (T) get(path, defaultValue.getClass());
		}
		save(path, defaultValue);
		return defaultValue;
	}

	/**
	 * Return whether a key exists or not at the given path
	 *
	 * @param path, the path to the key with path prefix added automatically
	 * @return
	 */
	protected final boolean isSet(final String path) {
		return isSetAbsolute(formPathPrefix(path));
	}

	/**
	 * Return whether a key exists or not at the given absolute path
	 *
	 * @param path, the path to the key without adding path prefix automatically
	 * @return
	 */
	protected final boolean isSetAbsolute(final String path) {
		return getConfig().isSet(path);
	}

	/**
	 * Return whether the default config exist in your plugins jar and contains the
	 * given relative path using {@link #getPathPrefix()} feature
	 *
	 * @param path
	 * @return
	 */
	protected final boolean isSetDefault(final String path) {
		return isSetDefaultAbsolute(formPathPrefix(path));
	}

	/**
	 * Return whether the default config exist in your plugins jar and contains the
	 * given absolute path
	 *
	 * @param path
	 * @return
	 */
	protected final boolean isSetDefaultAbsolute(final String path) {
		return getDefaults() != null && getDefaults().isSet(path);
	}

	// ------------------------------------------------------------------------------------
	// Lazy helpers
	// ------------------------------------------------------------------------------------

	/**
	 * Place the key from the default settings if those are set and the key does not
	 * exists
	 *
	 * @param pathAbs
	 */
	protected final void addDefaultIfNotExist(final String pathAbs) {
		addDefaultIfNotExist(pathAbs, Object.class);
	}

	/**
	 * Places a key from the default config into the current config file
	 *
	 * @param pathAbs
	 * @param type
	 */
	protected void addDefaultIfNotExist(final String pathAbs, final Class<?> type) {
		if (useDefaults && getDefaults() != null && !isSetAbsolute(pathAbs)) {
			final Object object = getDefaults().get(pathAbs);

			Valid.checkNotNull(object, "Default '" + getFileName() + "' lacks " + Common.article(type.getSimpleName()) + " at '" + pathAbs + "'");
			checkAssignable(true, pathAbs, object, type);

			checkAndFlagForSave(pathAbs, object);
			getConfig().set(pathAbs, object);
		}
	}

	/**
	 * Throws an error if using default settings AND defining the def parameter at
	 * the same time.
	 *
	 * <p>
	 * We do not allow that, please call methods without the def parameter when
	 * using default config as the default key will be fetched directly from the
	 * default config.
	 *
	 * @param path
	 */
	private void forceSingleDefaults(final String path) {
		if (useDefaults && getDefaults() != null)
			throw new FoException("Cannot use get method with default when getting " + formPathPrefix(path) + " and using a default config for " + getFileName());
	}

	/**
	 * Checks if the file instance exists and is a valid file, checks if default
	 * exists, sets the save flag to true and logs the update
	 *
	 * @param <T>
	 * @param path
	 * @param def
	 */
	private <T> void checkAndFlagForSave(final String path, final T def) {
		checkAndFlagForSave(path, def, true);
	}

	/**
	 * Checks if the file instance exists and is a valid file, checks if default
	 * exists, sets the save flag to true and logs the update
	 *
	 * @param <T>
	 * @param path
	 * @param def
	 * @param logUpdate
	 */
	private <T> void checkAndFlagForSave(final String path, final T def, final boolean logUpdate) {
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
	private void checkAssignable(final boolean fromDefault, final String path, final Object value, final Class<?> clazz) {
		if (checkAssignables && !clazz.isAssignableFrom(value.getClass()) && !clazz.getSimpleName().equals(value.getClass().getSimpleName()))
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
	protected String formPathPrefix(@NonNull final String path) {
		final String prefixed = pathPrefix != null ? pathPrefix + (!path.isEmpty() ? "." + path : "") : path;

		return prefixed.endsWith(".") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;
	}

	/**
	 * Sets path prefix to the given path prefix
	 *
	 * @param pathPrefix
	 */
	protected void pathPrefix(final String pathPrefix) {
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

	/**
	 * Get a map assuming each key contains a map of string and objects
	 */
	@Deprecated
	protected final LinkedHashMap<String, LinkedHashMap<String, Object>> getValuesAndKeys_OLD(String path) {
		Valid.checkNotNull(path, "Path cannot be null");
		path = formPathPrefix(path);

		// add default
		if (getDefaults() != null && !getConfig().isSet(path)) {
			Valid.checkBoolean(getDefaults().isSet(path), "Default '" + getFileName() + "' lacks a section at " + path);

			for (final String name : getDefaults().getConfigurationSection(path).getKeys(false))
				for (final String setting : getDefaults().getConfigurationSection(path + "." + name).getKeys(false))
					addDefaultIfNotExist(path + "." + name + "." + setting, Object.class);
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

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "YamlConfig{file=" + getFileName() + ", path prefix=" + this.pathPrefix + "}";
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		throw new RuntimeException("Please implement your own equals() method for " + getClass());
	}

	// ------------------------------------------------------------------------------------
	// Classes helpers
	// ------------------------------------------------------------------------------------

	/**
	 * @deprecated This class has been moved into {@link SimpleTime}.
	 * 			   To migrate, simply rename TimeHelper into SimpleTime everywhere.
	 */
	@Deprecated
	public static final class TimeHelper extends SimpleTime {

		protected TimeHelper(String time) {
			super(time);
		}

		/**
		 * Generate new time. Valid examples: 15 ticks 1 second 25 minutes 3 hours etc.
		 *
		 * @deprecated use {@link SimpleTime#from(String)} that has now replaced this constructor
		 * @param time
		 * @return
		 */
		@Deprecated
		public static TimeHelper from(final String time) {
			return new TimeHelper(time);
		}
	}

	/**
	 * Represents a list of locations in the config
	 */
	public static final class LocationList implements Iterable<Location> {

		/**
		 * The settings where we have these points
		 */
		private final YamlConfig settings;

		/**
		 * The list of locations
		 */
		private final List<Location> points;

		/**
		 * Create a new location list
		 *
		 * @param settings
		 * @param points
		 */
		private LocationList(final YamlConfig settings, final List<Location> points) {
			this.settings = settings;
			this.points = points;
		}

		/**
		 * Shortcut for adding/removing locations. Returns true if the given location
		 * not existed and it was removed or returns false if it was found and removed.
		 *
		 * @param location
		 * @return
		 */
		public boolean toggle(final Location location) {
			for (final Location point : points)
				if (Valid.locationEquals(point, location)) {
					points.remove(point);

					settings.save();
					return false;
				}

			points.add(location);
			settings.save();

			return true;
		}

		/**
		 * Add a new location
		 *
		 * @param location
		 */
		public void add(final Location location) {
			Valid.checkBoolean(!hasLocation(location), "Location at " + location + " already exists!");

			points.add(location);
			settings.save();
		}

		/**
		 * Remove an existing location
		 *
		 * @param location
		 */
		public void remove(final Location location) {
			final Location point = find(location);
			Valid.checkNotNull(point, "Location at " + location + " does not exist!");

			points.remove(point);
			settings.save();
		}

		/**
		 * Return true if the given location exists
		 *
		 * @param location
		 * @return
		 */
		public boolean hasLocation(final Location location) {
			return find(location) != null;
		}

		/**
		 * Return a validated location from the given location Pretty much the same but
		 * no yaw/pitch
		 *
		 * @param location
		 * @return
		 */
		public Location find(final Location location) {
			for (final Location entrance : points)
				if (Valid.locationEquals(entrance, location))
					return entrance;

			return null;
		}

		/**
		 * Return locations
		 *
		 * @return all locations
		 */
		public List<Location> getLocations() {
			return Collections.unmodifiableList(points);
		}

		/**
		 * Return iterator for this
		 *
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<Location> iterator() {
			return points.iterator();
		}

		/**
		 * Get how many points were set
		 *
		 * @return
		 */
		public int size() {
			return points.size();
		}
	}

	/**
	 * A simple helper class for some language-specific values when creating
	 * localization for numbers.
	 */
	public final class CasusHelper {
		private final String akuzativSg; // 1 second (Slovak case - sekundu, not in English)
		private final String akuzativPl; // 2-4 seconds (Slovak case - sekundy)
		private final String genitivPl; // 5+ seconds (Slovak case - sekund)

		private CasusHelper(final String raw) {
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

		public String formatWithCount(final long count) {
			return count + " " + formatWithoutCount(count);
		}

		public String formatWithoutCount(final long count) {
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

		private TitleHelper(final String path) {
			this(getString(path + ".Title"), getString(path + ".Subtitle"));
		}

		private TitleHelper(final String title, final String subtitle) {
			this.title = Common.colorize(title);
			this.subtitle = Common.colorize(subtitle);
		}

		/**
		 * Duration: 4 seconds + 2 second fade in
		 */
		public void playLong(final Player player, final Function<String, String> replacer) {
			play(player, 5, 4 * 20, 15, replacer);
		}

		/**
		 * Duration: 2 seconds + 1 second fade in
		 */
		public void playShort(final Player player, final Function<String, String> replacer) {
			play(player, 3, 2 * 20, 5, replacer);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut) {
			this.play(player, fadeIn, stay, fadeOut, null);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut, Function<String, String> replacer) {
			Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer != null ? replacer.apply(title) : title, replacer != null ? replacer.apply(subtitle) : subtitle);
		}
	}
}

/**
 * For safe read-write access we only store one opened file of the same name.
 *
 * <p>
 * This represents the access to that file.
 */
//@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
final class ConfigInstance {

	/**
	 * The file this configuration belongs to.
	 */
	@Getter
	private final File file;

	/**
	 * Our config we are manipulating.
	 */
	private final SimpleYaml config;

	/**
	 * @return the config
	 */
	public SimpleYaml getConfig() {
		return config;
	}

	/**
	 * The default config we reach out to fill values from.
	 */
	@Getter
	private final SimpleYaml defaultConfig;

	/**
	 * Experimental - Should we save comments for this config instance?
	 */
	private final boolean saveComments;

	/**
	 * If {@link YamlConfig#SAVE_COMMENTS} is on, we'll ignore these sections
	 * from comments being set
	 */
	private final List<String> uncommentedSections;

	/**
	 * Wherefrom shall we save o' mighty comments?
	 */
	private final String commentsFilePath;

	/**
	 * Saves the config instance with the given header, can be null
	 *
	 * @param header
	 */
	protected void save(final String[] header) {

		if (header != null) {
			config.options().copyHeader(true);
			config.options().header(String.join("\n", header));
		}

		if (Bukkit.isPrimaryThread())
			this.save0();

		else
			Common.runLater(this::save0);
	}

	private void save0() {

		try {

			// Either use write method for comments or the one from Bukkit
			if (!writeComments()) {

				// Pull the data on the main thread
				final Map<String, Object> values = config.getValues(false);

				try {

					// Yaml#dump should be save async... note that this also builds header
					final String data = config.saveToString(values);
					Files.createParentDirs(file);

					final Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);

					try {
						writer.write(data);
					} finally {
						writer.close();
					}

				} catch (final Throwable t) {
					Common.error(t);
				}
			}

		} catch (final Exception ex) {
			Common.error(ex, "Failed to save " + file.getName());
		}
	}

	/**
	 * Attempts to save configuration comments using default file
	 *
	 * @throws IOException
	 */
	public boolean writeComments() throws IOException {
		if (this.commentsFilePath != null && this.saveComments) {
			YamlComments.writeComments(this.commentsFilePath, this.file, Common.getOrDefault(this.uncommentedSections, new ArrayList<>()));

			return true;
		}

		return false;
	}

	/**
	 * Loads the config file again without saving changes
	 *
	 * @throws Exception
	 */
	protected void reload() throws IOException, InvalidConfigurationException {
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
	public boolean equals(final File file) {
		return equals((Object) file);
	}

	/**
	 * Returns true if the given file name equals to the one we store here
	 *
	 * @param fileName
	 * @return
	 */
	public boolean equals(final String fileName) {
		return equals((Object) fileName);
	}

	/**
	 * Returns true if the given object is a ConfigInstance having the same file
	 * name, or a file with the same name as this config instance
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof ConfigInstance ? ((ConfigInstance) obj).file.getName().equals(file.getName()) : obj instanceof File ? ((File) obj).getName().equals(file.getName()) : obj instanceof String ? ((String) obj).equals(file.getName()) : false;
	}
}

/**
 * A special class holding what enum values we have in our configuration
 * that are incompatible with older Minecraft version.
 * <p>
 * If those are loaded we just forgive them and do not throw an error.
 */
final class LegacyEnum {

	/**
	 * The map list of backward-incompatible types
	 */
	private static final StrictMap<Class<? extends Enum<?>>, List<String>> INCOMPATIBLE_TYPES = new StrictMap<>();

	/**
	 * Load incompatible values to map
	 */
	static {
		INCOMPATIBLE_TYPES.put(SpawnReason.class, Arrays.asList("DROWNED"));
	}

	/**
	 * Return true if the given enum class and name type is known to be
	 * backward incompatible.
	 *
	 * @param <T>
	 * @param type
	 * @param enumName
	 * @return
	 */
	public static <T extends Enum<T>> boolean isIncompatible(Class<T> type, String enumName) {
		final List<String> types = INCOMPATIBLE_TYPES.get(type);

		return types != null && types.contains(enumName.toUpperCase().replace(" ", "_"));
	}
}
