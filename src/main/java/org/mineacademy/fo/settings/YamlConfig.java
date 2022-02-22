package org.mineacademy.fo.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.AnchorNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import lombok.Getter;
import lombok.NonNull;

/**
 * An enhanced base of the MemorySection and other classes in Bukkit adjusted
 * for maximum compatibility (should even work on 1.1, to the latest one),
 * unicode support, comments support, safety and automatic config updating
 * if default file is provided.
 *
 * You can either call {@link #fromFile(File)} or extend this class. If you extend
 * this class, make sure to use {@link #loadConfiguration(File)} and then
 * load your fields using {@link #onLoadFinish()} and save them in {@link #onSerialize()}.
 *
 * @version 6.0
 *
 * @author kangarko, Spigot/Bukkit team, https://github.com/tchristofferson/Config-Updater for comments support
 */
public class YamlConfig implements ConfigSerializable {

	/**
	 * A null flag indicating there is no default 'to' config file
	 *
	 * <p>
	 * When you call methods with the "def" parameter, we enforce this flag and will
	 * NOT auto update the config with the specified default value
	 */
	public static final String NO_DEFAULT = null;

	/*
	 * All currently loaded configurations, stored by disk file name.
	 */
	private static final StrictMap<String, YamlConfig> loadedConfigs = new StrictMap<>();

	/*
	 * The SnakeYAML constructor.
	 */
	private final YamlConstructor constructor;

	/*
	 * The SnakeYAML instance.
	 */
	private final Yaml yaml;

	/*
	 * Stores all loaded values in our memory
	 */
	private final Map<String, Object> map = new LinkedHashMap<>();

	/*
	 * The root of this yaml config.
	 */
	private final YamlConfig root;

	/*
	 * The parent object of this yaml config.
	 */
	private final YamlConfig parent;

	/*
	 * The paths stored in the map, delimited by "." such as "Player.Properties.Color"
	 */
	private final String path;
	private final String fullPath;

	/*
	 * The file associated with this config, set if loaded through a {@link #loadConfiguration(File)}.
	 */
	@Nullable
	private File file;

	/*
	 * The default file from which we pick comments and order when saving,
	 * please see {@link #getUncommentedSections()} to return a list of sections
	 * where users are able to write maps otherwise those maps will be removed upon reload.
	 */
	@Getter
	@Nullable
	private YamlConfig defaults;

	/*
	 * The path to defaults file
	 */
	@Nullable
	private String defaultsPath;

	/*
	 * A custom header (only works when no defaults are set)
	 */
	private List<String> header = new ArrayList<>();

	/*
	 * A temporary path prefix placed upon path and fullpath to save your time calling getX methods:
	 *
	 * get("Player.Properties.Color")
	 * get("Player.Properties.Chat")
	 * get("Player.Properties.Decoration")
	 * etc.
	 *
	 * Becomes just get("Color") etc. if you use {@link #setPathPrefix(String)} to set "Player.Properties"
	 */
	private String pathPrefix = null;

	/**
	 * Create a new unloaded yaml configuration
	 */
	protected YamlConfig() {
		this(null, "");
	}

	/*
	 * Load and prepare configuration, super compatible down to Minecraft 1.2.5.
	 */
	private YamlConfig(YamlConfig parent, String path) {
		this.constructor = new YamlConstructor();

		final YamlRepresenter representer = new YamlRepresenter();
		representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setIndent(2);
		dumperOptions.setWidth(4096); // Do not wrap long lines

		// Load options only if available
		if (ReflectionUtil.isClassAvailable("org.yaml.snakeyaml.LoaderOptions")) {
			Yaml yaml;

			try {
				final LoaderOptions loaderOptions = new LoaderOptions();
				loaderOptions.setMaxAliasesForCollections(512);

				yaml = new Yaml(this.constructor, representer, dumperOptions, loaderOptions);

			} catch (final NoSuchMethodError ex) {
				yaml = new Yaml(this.constructor, representer, dumperOptions);
			}

			this.yaml = yaml;
		}

		else
			this.yaml = new Yaml(this.constructor, representer, dumperOptions);

		this.root = path == null || parent == null ? this : parent.root;
		this.parent = parent;
		this.path = path;
		this.fullPath = path.isEmpty() || parent == null ? "" : createPath(parent, path);
	}

	// ------------------------------------------------------------------------------------
	// Options
	// ------------------------------------------------------------------------------------

	/**
	 * Called automatically after loading methods.
	 */
	protected void onLoadFinish() {
	}

	/**
	 * Called automatically on save, return your custom fields data here
	 *
	 * @return
	 */
	protected SerializedMap onSerialize() {
		return new SerializedMap();
	}

	/**
	 * Called right before any save call.
	 */
	protected void onSave() {
	}

	/**
	 * Override this with paths to sections where users are able to create their own maps,
	 * example: In ChatControl you are able to write your own key-values in Channels.List so we
	 * place "Channels.List" to the list here.
	 *
	 * @return
	 */
	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	/**
	 * Return true if this section of the config file exists
	 *
	 * @return
	 */
	public boolean isValid() {
		return this.getObject("") instanceof YamlConfig;
	}

	// ------------------------------------------------------------------------------------
	// Miscellaneous
	// ------------------------------------------------------------------------------------

	/**
	 * Return the file name, with extension.
	 *
	 * @return
	 */
	public final String getFileName() {
		return this.file == null ? null : this.file.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting values
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * Retrieve an object at the given config path, attempting to cast it using vanilla java
	 * or return null if not set. If defaults file exists, we attempt to copy the
	 * default value to the file.
	 */
	private final <T> T getT(@NonNull String path, Class<T> type) {
		return this.getT(path, type, this.getDefault(path));
	}

	/*
	 * Retrieve an object at the given config path, attempting to cast it using vanilla java
	 * and supply with default if set. If defaults file exists, we attempt to copy the
	 * default value to the file.
	 */
	private final <T> T getT(@NonNull String path, Class<T> type, Object def) {
		path = this.compilePathPrefix(path);
		Valid.checkBoolean(!path.endsWith("."), "Path must not end with '.': " + path);

		// Copy defaults if not set and log about this change
		this.addDefaultIfNotExist(path, type);

		Object raw = this.getFast0(path, def);

		// Ensure that the default config actually did have the value, if used
		if (this.defaults != null)
			Valid.checkNotNull(raw, "Failed to insert value at '" + path + "' from default config");

		// Ensure the value is of the given type
		if (raw != null) {

			// Workaround for empty lists
			if (raw.equals("[]") && type == List.class)
				raw = new ArrayList<>();

			// Retype manually
			if (type == Long.class && raw instanceof Integer)
				raw = (long) Integer.parseInt(raw.toString());

			this.checkAssignable(false, path, raw, type);
		}

		return (T) raw;
	}

	/*
	 * Fast lookup of a section in our map with out default object as fallback
	 * Credits to Spigot/Bukkit team
	 */
	private final Object getFast0(String path) {
		return this.getFast0(path, this.getDefault(path));
	}

	/*
	 * Fast lookup of a section in our map with out default object as fallback
	 * Credits to Spigot/Bukkit team
	 */
	private final Object getFast0(String path, Object def) {

		if (path.isEmpty())
			return this;

		if (this.root == null)
			throw new IllegalStateException("Cannot access section without a root");

		int leadingIndex = -1;
		int lowerIndex;
		YamlConfig section = this;

		while ((leadingIndex = path.indexOf('.', lowerIndex = leadingIndex + 1)) != -1) {
			final String currentPath = path.substring(lowerIndex, leadingIndex);
			if (!section.contains(currentPath, true))
				return def;

			section = section.getConfigurationSection(currentPath);

			if (section == null)
				return def;
		}

		final String key = path.substring(lowerIndex);

		if (section == this) {
			final Object result = this.map.get(key);

			return result == null ? def : result;
		}

		return section.getFast0(key, def);
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
	 * @param deserializeParams
	 * @return
	 */
	public final <T> T get(final String path, final Class<T> type, Object... deserializeParams) {
		return get(path, type, null, deserializeParams);
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
	 * @param deserializeParams
	 * @return
	 */
	public final <T> T get(final String path, final Class<T> type, final T def, Object... deserializeParams) {
		final Object object = getT(path, Object.class);

		return object != null ? SerializeUtil.deserialize(type, object, deserializeParams) : def;
	}

	/**
	 * Get an unknown object with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Object getObject(final String path, final Object def) {
		return isSet(path) ? getObject(path) : def;
	}

	/**
	 * Get an unknown object
	 *
	 * @param path
	 * @return
	 */
	public final Object getObject(final String path) {
		return getT(path, Object.class);
	}

	/**
	 * Get a boolean with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	@Nullable
	public final Boolean getBoolean(final String path, final boolean def) {
		return isSet(path) ? getBoolean(path) : def;
	}

	/**
	 * Get a boolean
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final Boolean getBoolean(final String path) {
		return getT(path, Boolean.class);
	}

	/**
	 * Get a string with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final String getString(final String path, final String def) {
		return isSet(path) ? getString(path) : def;
	}

	/**
	 * Get a string
	 *
	 * @param path
	 * @return
	 */
	public final String getString(final String path) {
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
		return isSet(path) ? getLong(path) : def;
	}

	/**
	 * Get a long
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final Long getLong(final String path) {
		return getT(path, Long.class);
	}

	/**
	 * Get an integer with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	@Nullable
	public final Integer getInteger(final String path, final Integer def) {
		return isSet(path) ? getInteger(path) : def;
	}

	/**
	 * Get an integer
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final Integer getInteger(final String path) {
		return getT(path, Integer.class);
	}

	/**
	 * Get a double with a default value
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	@Nullable
	public final Double getDouble(final String path, final Double def) {
		return isSet(path) ? getDouble(path) : def;
	}

	/**
	 * Get a double number
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final Double getDouble(final String path) {
		final Object raw = getObject(path);

		return raw != null ? Double.parseDouble(raw.toString()) : null;
	}

	/**
	 * Get a Bukkit location, using our serialization method
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Location getLocation(final String path, final Location def) {
		return isSet(path) ? getLocation(path) : def;
	}

	/**
	 * Get a Bukkit location, using our serialization method
	 *
	 * @param path
	 * @return
	 */
	public final Location getLocation(final String path) {
		return get(path, Location.class);
	}

	/**
	 * Get an offline player, using Bukkit's serialization method
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final OfflinePlayer getOfflinePlayer(final String path, final OfflinePlayer def) {
		return isSet(path) ? getOfflinePlayer(path) : def;
	}

	/**
	 * Get an offline player, using Bukkit's serialization method
	 *
	 * @param path
	 * @return
	 */
	public final OfflinePlayer getOfflinePlayer(final String path) {
		return getT(path, OfflinePlayer.class);
	}

	/**
	 * Get a sound with volume and a pitch
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final SimpleSound getSound(final String path, final SimpleSound def) {
		return isSet(path) ? getSound(path) : def;
	}

	/**
	 * Get a sound with volume and a pitch
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final SimpleSound getSound(final String path) {
		return isSet(path) ? new SimpleSound(getString(path)) : null;
	}

	/**
	 * Returns a time period denoted in the Accusative case for languages that support that.
	 * Used in messages such as "Please wait X seconds before chatting" where Slavic and many
	 * other languages support three forms, 1 second, 2-5 seconds and 0 or 5+ seconds have
	 * different formats.
	 *
	 * This allows you to format your message automatically, example in Slovak:
	 *
	 * (čakajte...) 1 sekundu
	 * 2 sekundy
	 * 0/5 sekúnd
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final AccusativeHelper getAccusativePeriod(final String path, final AccusativeHelper def) {
		return isSet(path) ? getAccusativePeriod(path) : def;
	}

	/**
	 * Returns a time period denoted in the Accusative case for languages that support that.
	 * Used in messages such as "Please wait X seconds before chatting" where Slavic and many
	 * other languages support three forms, 1 second, 2-5 seconds and 0 or 5+ seconds have
	 * different formats.
	 *
	 * This allows you to format your message automatically, example in Slovak:
	 *
	 * (čakajte...) 1 sekundu
	 * 2 sekundy
	 * 0/5 sekúnd
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final AccusativeHelper getAccusativePeriod(final String path) {
		return isSet(path) ? new AccusativeHelper(getString(path)) : null;
	}

	/**
	 * Get a title message, having title and a subtitle
	 *
	 * @param path
	 * @param defTitle
	 * @param defSubtitle
	 * @return
	 */
	public final TitleHelper getTitle(final String path, final String defTitle, final String defSubtitle) {
		return isSet(path) ? getTitle(path) : new TitleHelper(defTitle, defSubtitle);
	}

	/**
	 * Get a title message, having title and a subtitle
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final TitleHelper getTitle(final String path) {
		final String title = this.getString(path + ".Title");
		final String subtitle = this.getString(path + ".Subtitle");

		return isSet(path) ? new TitleHelper(title, subtitle) : null;
	}

	/**
	 * Get a time value in human readable format, eg. "20 minutes" or "45 ticks"
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final SimpleTime getTime(final String path, final String def) {
		return isSet(path) ? getTime(path) : def != null ? SimpleTime.from(def) : null;
	}

	/**
	 * Get a time value in human readable format, eg. "20 minutes" or "45 ticks"
	 *
	 * @param path
	 * @return
	 */
	public final SimpleTime getTime(final String path) {
		final Object obj = getObject(path);

		return obj != null ? SimpleTime.from(obj.toString()) : null;
	}

	/**
	 * Get a percentage from 0% to 100%
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final Double getPercentage(String path) {
		if (isSet(path)) {
			final String raw = getObject(path).toString();
			Valid.checkBoolean(raw.endsWith("%"), "Your " + path + " key in " + getPathPrefix() + "." + path + " must end with %! Got: " + raw);

			final String rawNumber = raw.substring(0, raw.length() - 1);
			Valid.checkInteger(rawNumber, "Your " + path + " key in " + getPathPrefix() + "." + path + " must be a whole number! Got: " + raw);

			return Integer.parseInt(rawNumber) / 100D;
		}

		return null;
	}

	/**
	 * Get a boxed message having full-width top and bottom lines in chat
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final BoxedMessage getBoxedMessage(final String path, final String def) {
		return isSet(path) ? getBoxedMessage(path) : new BoxedMessage(def);
	}

	/**
	 * Get a boxed message having full-width top and bottom lines in chat
	 *
	 * @param path
	 * @return
	 */
	public final BoxedMessage getBoxedMessage(final String path) {
		return new BoxedMessage(getString(path));
	}

	/**
	 * Get a CompMaterial which is our cross-version compatible material class
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final CompMaterial getMaterial(final String path, final CompMaterial def) {
		return isSet(path) ? getMaterial(path) : def;
	}

	/**
	 * Get a CompMaterial which is our cross-version compatible material class
	 *
	 * @param path
	 * @return
	 */
	public final CompMaterial getMaterial(final String path) {
		final String name = getString(path);

		return name == null ? null : CompMaterial.fromStringStrict(name);
	}

	/**
	 * Return a tuple
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @return
	 */
	public final <K, V> Tuple<K, V> getTuple(final String key, Class<K> keyType, Class<V> valueType) {
		return getTuple(key, null, keyType, valueType);
	}

	/**
	 * Return a tuple or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public final <K, V> Tuple<K, V> getTuple(final String key, final Tuple<K, V> def, Class<K> keyType, Class<V> valueType) {
		final SerializedMap map = getMap(key);

		return !map.isEmpty() ? Tuple.deserialize(map, keyType, valueType) : def;
	}

	/**
	 * Returns an ItemStack or null if not set
	 *
	 * @param path
	 * @return
	 */
	public final ItemStack getItemStack(@NonNull String path) {
		return this.getItemStack(path, null);
	}

	/**
	 * Returns an ItemStack or default if not set
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final ItemStack getItemStack(@NonNull String path, ItemStack def) {
		return isSet(path) ? get(path, ItemStack.class) : def;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get location list at the given config path
	 *
	 * @param path
	 * @return
	 */
	public final LocationList getLocationList(final String path) {
		return new LocationList(this, this.getList(path, Location.class));
	}

	/**
	 * Get a list of unknown values
	 *
	 * @param path
	 * @return
	 */
	public final List<Object> getList(final String path) {
		final Object obj = getObject(path);

		// Allow one values instead of lists, such as Apply_On: timed instead of Apply_On: [timed] for
		// maximum user convenience
		return obj instanceof List ? (List<Object>) obj : obj != null ? Arrays.asList(obj) : new ArrayList<>();
	}

	/**
	 * Return a list of hash maps at the given location
	 *
	 * @param path
	 * @return list of maps, or empty map if not set
	 */
	public final List<SerializedMap> getMapList(final String path) {
		return getList(path, SerializedMap.class);
	}

	/**
	 * @param <T>
	 * @param key
	 * @param type
	 * @param deserializeParameters
	 * @return
	 * @see #getList(String, Class)
	 */
	public final <T> Set<T> getSet(final String key, final Class<T> type, final Object... deserializeParameters) {
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
	public final <T> List<T> getList(final String path, final Class<T> type) {
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
	public final <T> List<T> getList(final String path, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = new ArrayList<>();
		final List<Object> objects = getList(path);

		if (type == Map.class && deserializeParameters != null & deserializeParameters.length > 0 && deserializeParameters[0] != String.class)
			throw new FoException("getList('" + path + "') that returns Map must have String.class as key, not " + deserializeParameters[0]);

		if (objects != null)
			for (Object object : objects) {
				object = object != null ? SerializeUtil.deserialize(type, object, deserializeParameters) : null;

				if (object != null)
					list.add((T) object);
			}

		return list;
	}

	/**
	 * Get a matching list
	 *
	 * @param path
	 * @param type
	 * @return
	 */
	public final <T> IsInList<T> getIsInList(String path, Class<T> type) {
		final List<String> stringList = getStringList(path);

		if (stringList.size() == 1 && "*".equals(stringList.get(0)))
			return IsInList.fromStar();

		return IsInList.fromList(getList(path, type));
	}

	/**
	 * Get a simple string array
	 *
	 * @param path
	 * @return the given array, or an empty array
	 */
	/*public final String[] getStringArray(final String path) {
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
	}*/

	/**
	 * Get a string list
	 *
	 * @param path
	 * @return the found list, or an empty list
	 */
	public final List<String> getStringList(final String path) {
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

	/*
	 * Attempts to convert objects into strings, since SnakeYAML parser interprets
	 * "true" and "yes" as boolean types
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
	@Nullable
	public final StrictList<String> getCommandList(final String path) {

		// Nowhere to copy from
		if (!isSet(path) && this.defaults == null)
			return null;

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
	public final StrictList<CompMaterial> getMaterialList(final String path) {
		final StrictList<CompMaterial> list = new StrictList<>();

		for (final String raw : getStringList(path)) {
			final CompMaterial mat = CompMaterial.fromString(raw);

			if (mat != null)
				list.add(mat);
		}

		return list;
	}

	/**
	 * Get a serialized map
	 *
	 * @param path
	 * @return map, or empty map
	 */
	public final SerializedMap getMap(final String path) {
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
	 * @param valueDeserializeParams
	 *
	 * @return
	 */
	public final <Key, Value> LinkedHashMap<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType, Object... valueDeserializeParams) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, Value> map = new LinkedHashMap<>();

		// Add path prefix right away
		path = compilePathPrefix(path);

		// Add defaults
		if (defaults != null && !this.isSet(path)) {
			Valid.checkBoolean(defaults.isSet(path), "Default '" + getFileName() + "' lacks a map at " + path);

			for (final String key : defaults.getConfigurationSection(path).getKeys(false))
				addDefaultIfNotExist(path + "." + key, valueType);
		}

		// Load key-value pairs from config to our map
		final SerializedMap configSection = SerializedMap.of(this.getObject(path));

		for (final Map.Entry<String, Object> entry : configSection.entrySet()) {
			final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
			final Value value = SerializeUtil.deserialize(valueType, entry.getValue(), valueDeserializeParams);

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
	 * @param setDeserializeParameters
	 * @return
	 */
	public final <Key, Value> LinkedHashMap<Key, List<Value>> getMapList(@NonNull String path, final Class<Key> keyType, final Class<Value> setType, Object... setDeserializeParameters) {
		// The map we are creating, preserve order
		final LinkedHashMap<Key, List<Value>> map = new LinkedHashMap<>();

		// Add path prefix right away
		path = compilePathPrefix(path);

		// Add defaults
		if (defaults != null && !this.isSet(path)) {
			Valid.checkBoolean(defaults.isSet(path), "Default '" + getFileName() + "' lacks a map at " + path);

			for (final String key : defaults.getConfigurationSection(path).getKeys(false))
				addDefaultIfNotExist(path + "." + key, setType);
		}

		// Load key-value pairs from config to our map
		final SerializedMap configSection = SerializedMap.of(this.getObject(path));

		for (final Map.Entry<String, Object> entry : configSection.entrySet()) {
			final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
			final List<Value> value = SerializeUtil.deserialize(List.class, entry.getValue(), setDeserializeParameters);

			// Ensure the pair values are valid for the given paramenters
			checkAssignable(false, path, key, keyType);

			if (!value.isEmpty())
				for (final Value item : value)
					checkAssignable(false, path, item, setType);

			map.put(key, value);
		}

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting sections
	// ------------------------------------------------------------------------------------------------------------

	public final YamlConfig getConfigurationSection(@NonNull String path) {
		Object val = this.getFast0(path, null);

		if (val != null)
			return val instanceof YamlConfig ? (YamlConfig) val : null;

		val = this.getFast0(path, this.getDefault(path));

		return val instanceof YamlConfig ? this.createSection(path) : null;
	}

	@NonNull
	public final Set<String> getKeys(boolean deep) {
		final Set<String> result = new LinkedHashSet<>();

		final YamlConfig root = this.root;
		if (root != null) {
			final YamlConfig defaults = this.getDefaultSection();

			if (defaults != null)
				result.addAll(defaults.getKeys(deep));
		}

		this.mapChildrenKeys(result, this, deep);

		return result;
	}

	@NonNull
	public final Map<String, Object> getValues(boolean deep) {
		final Map<String, Object> result = new LinkedHashMap<>();

		final YamlConfig root = this.root;

		if (root != null) {
			final YamlConfig defaults = this.getDefaultSection();

			if (defaults != null)
				result.putAll(defaults.getValues(deep));
		}

		this.mapChildrenValues(result, this, deep);

		return result;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Loading
	// ------------------------------------------------------------------------------------------------------------

	public final void reload() {
		Valid.checkNotNull(this.file, "Cannot call reload() before loading since we lack a file to load from yet!");

		this.loadConfiguration(this.defaultsPath, this.file);
	}

	public final void loadConfiguration(String defaultPath) {
		this.loadConfiguration(defaultPath, defaultPath);
	}

	public final void loadConfiguration(File file) {
		this.loadConfiguration(null, file);
	}

	public final void loadConfiguration(@Nullable String from, @NonNull String to) {
		final File toFile = from != null ? FileUtil.extract(from, to) : FileUtil.getOrMakeFile(to);

		this.loadConfiguration(from, toFile);
	}

	public final void loadConfiguration(@Nullable String from, @NonNull File toFile) {

		try {
			final InputStreamReader stream = new InputStreamReader(new FileInputStream(toFile), StandardCharsets.UTF_8);

			this.file = toFile;
			this.defaultsPath = from;

			// Handle defaults
			if (from != null) {
				this.defaults = new YamlConfig();

				final List<String> lines = FileUtil.getInternalResource(from);
				Valid.checkNotNull("Failed to find default configuration at path " + from + " (did you reload?)");

				this.defaults.loadFromString(String.join("\n", lines));
			}

			this.loadConfiguration(stream);

		} catch (final IOException ex) {
			throw new FoException(ex, "Unable to load configuration from file " + this.file);
		}
	}

	public final void loadConfiguration(@NonNull Reader reader) throws IOException {
		final BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
		final StringBuilder builder = new StringBuilder();

		try {
			String line;

			while ((line = input.readLine()) != null) {
				builder.append(line);
				builder.append('\n');
			}

		} finally {
			input.close();
		}

		this.loadFromString(builder.toString());
	}

	public final void loadFromString(@NonNull String contents) {

		MappingNode node;

		try (Reader reader = new UnicodeReader(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)))) {
			node = (MappingNode) this.yaml.compose(reader);

		} catch (YAMLException | IOException e) {
			throw new FoException(e);

		} catch (final ClassCastException e) {
			throw new FoException("Top level is not a Map.");
		}

		this.map.clear();

		if (node != null)
			this.fromNodeTree(node, this);

		this.onLoadFinish();
	}

	// ------------------------------------------------------------------------------------
	// Checking for validity and defaults
	// ------------------------------------------------------------------------------------

	/**
	 * Return true if the given path exists, excluding {@link #getPathPrefix()}
	 *
	 * @param path
	 * @return
	 */
	public final boolean isSetAbsolute(@NonNull String path) {
		return this.root != null && this.contains(path);
	}

	/**
	 * Return true if the given path exists, including {@link #getPathPrefix()}
	 *
	 * @param path
	 * @return
	 */
	public final boolean isSet(@NonNull String path) {
		path = this.compilePathPrefix(path);

		return this.root != null && this.contains(path);
	}

	private boolean contains(@NonNull String path) {
		return this.contains(path, false);
	}

	private boolean contains(@NonNull String path, boolean ignoreDefault) {
		return (ignoreDefault ? this.getFast0(path, null) : this.getFast0(path)) != null;
	}

	private final Object getDefault(@NonNull String path) {
		path = this.compilePathPrefix(path);

		final YamlConfig defaults = this.root == null ? null : this.root.getDefaults();

		return defaults == null ? null : defaults.getFast0(createPath(this, path));
	}

	private YamlConfig getDefaultSection() {
		final YamlConfig defaults = this.root == null ? null : this.root.getDefaults();

		if (defaults != null)
			if (defaults.getFast0(this.fullPath) instanceof YamlConfig)
				return defaults.getConfigurationSection(this.fullPath);

		return null;
	}

	/*
	 * Place the key from the default settings if those are set and the key does not
	 * exists
	 */
	/*private void addDefaultIfNotExist(final String pathAbs) {
		addDefaultIfNotExist(pathAbs, Object.class);
	}*/

	/*
	 * Places a key from the default config into the current config file
	 */
	private void addDefaultIfNotExist(final String pathAbs, final Class<?> type) {
		if (this.defaults != null && !this.isSetAbsolute(pathAbs)) {
			final Object object = this.defaults.get(pathAbs, Object.class);

			Valid.checkNotNull(object, "Default '" + this.getFileName() + "' lacks " + Common.article(type.getSimpleName()) + " at '" + pathAbs + "'");
			this.checkAssignable(true, pathAbs, object, type);

			this.checkAndFlagForSave(pathAbs, object);
			this.setNoSave(pathAbs, object);
		}
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
		this.checkAndFlagForSave(path, def, true);
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
		if (this.defaults != null)
			Valid.checkNotNull(def, "Inbuilt config " + this.getFileName() + " lacks " + (def == null ? "key" : def.getClass().getSimpleName()) + " at \"" + path + "\". Is it outdated?");

		if (logUpdate)
			Common.log("&7Update " + this.getFileName() + " at &b\'&f" + path + "&b\' &7-> " + (def == null ? "&ckey removed" : "&b\'&f" + def + "&b\'") + "&r");
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
		if (!clazz.isAssignableFrom(value.getClass()) && !clazz.getSimpleName().equals(value.getClass().getSimpleName())) {

			// Exception
			if (ConfigSerializable.class.isAssignableFrom(clazz) && value instanceof MemorySection)
				return;

			throw new FoException("Malformed configuration! Key '" + path + "' in " + (fromDefault ? "inbuilt " : "") + this.getFileName() + " must be " + clazz.getSimpleName() + " but got " + value.getClass().getSimpleName() + ": '" + value + "'");
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Saving
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Moves a certain config key or section from one path to another
	 *
	 * @param fromRelative
	 * @param toAbsolute
	 */
	protected final void move(final String fromRelative, final String toAbsolute) {
		this.move(this.getFast0(fromRelative), fromRelative, toAbsolute);
	}

	/**
	 * Moves a certain config key from one path to another
	 *
	 * @param value
	 * @param fromPathRel
	 * @param toPathAbs
	 */
	protected final void move(final Object value, String fromPathRel, final String toPathAbs) {
		final String oldPathPrefix = this.pathPrefix;

		fromPathRel = this.compilePathPrefix(fromPathRel);

		this.setNoSave(fromPathRel, null);
		this.pathPrefix = oldPathPrefix; // set to previous

		this.setNoSave(toPathAbs, value);

		Common.log("&7Update " + this.getFileName() + ". Move &b\'&f" + fromPathRel + "&b\' &7(was \'" + value + "&7\') to " + "&b\'&f" + toPathAbs + "&b\'" + "&r");

		this.pathPrefix = oldPathPrefix; // and reset back to whatever it was
	}

	public final void save(@NonNull String path, Object value) {
		this.setNoSave(path, value);

		this.save();
	}

	public final void setNoSave(@NonNull String path, Object value) {
		Valid.checkNotEmpty(path, "Cannot set to an empty path");

		final YamlConfig root = this.root;

		if (root == null)
			throw new IllegalStateException("Cannot use section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		YamlConfig section = this;

		while ((i1 = path.indexOf('.', i2 = i1 + 1)) != -1) {
			final String node = path.substring(i2, i1);
			final YamlConfig subSection = section.getConfigurationSection(node);
			if (subSection == null) {
				if (value == null)
					// no need to create missing sub-sections if we want to remove the value:
					return;
				section = section.createSection(node);
			} else
				section = subSection;
		}

		final String key = path.substring(i2);

		if (section == this) {
			if (value == null)
				this.map.remove(key);

			else
				this.map.put(key, value);

		} else
			section.setNoSave(key, value);
	}

	/**
	 * Permanently removes the disk file and unloads configuration
	 *
	 * @return
	 */
	public final boolean deleteFile() {
		if (this.file.exists()) {
			this.file.delete();
			unregisterLoadedFile(this.file);

			return true;
		}

		return false;
	}

	/**
	 * Deletes all data from the relevant part of the file depending on {@link #getPathPrefix()}, keeping the file.
	 */
	public final void clear() {
		this.save("", null);
	}

	/**
	 * Saves the content of the file (must be loaded first using load(File)
	 */
	public final void save() {
		Valid.checkNotNull(this.file, "Cannot call save() method when no file was set (did you call load(File)?)");

		this.save(this.file);
	}

	/**
	 * Saves the content of this config to the given file path in your plugin's folder
	 *
	 * @param filePath
	 */
	public final void save(@NonNull String filePath) {
		this.save(FileUtil.getFile(filePath));
	}

	/**
	 * Saves the content of this config to the given file in your plugin's folder
	 *
	 * @param file
	 */
	public final void save(@NonNull File file) {

		try {
			this.onSave();

			if (this.defaults == null) {

				final File parent = file.getCanonicalFile().getParentFile();

				// Create parent dirs
				if (parent != null)
					parent.mkdirs();

				// Dump to file
				final String data = this.saveToString();
				final Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);

				try {
					writer.write(data);

				} finally {
					writer.close();
				}

				return;
			}

			final List<String> newLines = FileUtil.getInternalResource(this.defaultsPath);
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8));

			// ignoredSections can ONLY contain configurations sections
			for (final String ignoredSection : this.getUncommentedSections())
				if (this.defaults.isSet(ignoredSection))
					Valid.checkBoolean(this.defaults.getFast0(ignoredSection) instanceof YamlConfig, "Can only ignore config sections in " + this.defaultsPath + " (file " + this.file + ")" + " not '" + ignoredSection + "' that is " + this.defaults.getFast0(ignoredSection));

			// Save keys added to config that are not in default and would otherwise be lost
			final Set<String> newKeys = this.defaults.getKeys(true);
			final Map<String, Object> removedKeys = new LinkedHashMap<>();

			outerLoop:
			for (final Map.Entry<String, Object> oldEntry : this.getValues(true).entrySet()) {
				final String oldKey = oldEntry.getKey();

				for (final String ignoredKey : this.getUncommentedSections())
					if (oldKey.startsWith(ignoredKey))
						continue outerLoop;

				if (!newKeys.contains(oldKey))
					removedKeys.put(oldKey, oldEntry.getValue());
			}

			// Move to unused/ folder and retain old path
			if (!removedKeys.isEmpty()) {
				final YamlConfig backupConfig = new YamlConfig();

				backupConfig.loadConfiguration(NO_DEFAULT, "unused/" + this.file.getName());

				for (final Map.Entry<String, Object> entry : removedKeys.entrySet())
					backupConfig.setNoSave(entry.getKey(), entry.getValue());

				backupConfig.save();

				Common.warning("The following entries in " + this.file.getName() + " are unused and were moved into " + backupConfig.file.getName() + ": " + removedKeys.keySet());
			}

			final Map<String, String> comments = this.dumpComments(newLines);

			this.write(comments, writer);

		} catch (final IOException ex) {
			throw new FoException(ex, "Unable to save configuration to file " + file);
		}
	}

	/**
	 * Dumps the entire content of this config into a saveable yaml string
	 *
	 * @return
	 */
	@NonNull
	public final String saveToString() {
		final Map<String, Object> values = this.getValues(false);
		final String header = this.buildHeader();

		String dump = this.yaml.dump(SerializeUtil.serialize(values));

		if (dump.equals("{}\n")) // empty config
			dump = "";

		return header + dump;
	}

	// It checks if key has a comment associated with it and writes comment then the key and value
	// Notice: Various authors, original credit lost. Please contact us for crediting you.
	private void write(Map<String, String> comments, BufferedWriter writer) throws IOException {

		final Set<String> copyAllowed = new HashSet<>();
		final Set<String> reverseCopy = new HashSet<>();

		outerloop:
		for (final String key : this.defaults.getKeys(true)) {

			checkIgnore:
			{

				for (final String allowed : copyAllowed)
					if (key.startsWith(allowed))
						break checkIgnore;

				// These keys are already written below
				for (final String allowed : reverseCopy)
					if (key.startsWith(allowed))
						continue outerloop;

				for (final String ignoredSection : this.getUncommentedSections()) {
					if (key.equals(ignoredSection))
						// Write from new to old config
						if (!this.isSet(ignoredSection) || this.getConfigurationSection(ignoredSection).getKeys(false).isEmpty()) {
							copyAllowed.add(ignoredSection);

							break;
						}

						// Write from old to new, copying all keys and subkeys manually
						else {
							this.write0(key, true, comments, writer);

							for (final String oldKey : this.getConfigurationSection(ignoredSection).getKeys(true))
								this.write0(ignoredSection + "." + oldKey, true, comments, writer);

							reverseCopy.add(ignoredSection);
							continue outerloop;
						}

					if (key.startsWith(ignoredSection))
						continue outerloop;
				}
			}

			this.write0(key, false, comments, writer);
		}

		final String danglingComments = comments.get(null);

		if (danglingComments != null)
			writer.write(danglingComments);

		writer.close();
	}

	private void write0(String key, boolean forceNew, Map<String, String> comments, BufferedWriter writer) throws IOException {

		final String[] keys = key.split("\\.");
		final String actualKey = keys[keys.length - 1];
		final String comment = comments.remove(key);

		final StringBuilder prefixBuilder = new StringBuilder();
		final int indents = keys.length - 1;
		this.appendPrefixSpaces(prefixBuilder, indents);
		final String prefixSpaces = prefixBuilder.toString();

		// No \n character necessary, new line is automatically at end of comment
		if (comment != null)
			writer.write(comment);

		final Object newObj = this.defaults.getFast0(key);
		final Object oldObj = this.getFast0(key);

		// Write the old section
		if (newObj instanceof YamlConfig && !forceNew && oldObj instanceof YamlConfig)
			this.writeSection(writer, actualKey, prefixSpaces, (YamlConfig) oldObj);

		// Write the new section, old value is no more
		else if (newObj instanceof YamlConfig)
			this.writeSection(writer, actualKey, prefixSpaces, (YamlConfig) newObj);

		// Write the old object
		else if (oldObj != null && !forceNew)
			this.write(oldObj, actualKey, prefixSpaces, writer);

		// Write new object
		else
			this.write(newObj, actualKey, prefixSpaces, writer);
	}

	// Doesn't work with configuration sections, must be an actual object
	// Auto checks if it is serializable and writes to file
	private void write(Object obj, String actualKey, String prefixSpaces, BufferedWriter writer) throws IOException {

		if (obj instanceof String || obj instanceof Character) {
			if (obj instanceof String) {
				final String string = (String) obj;

				// Split multi line strings using |-
				if (string.contains("\n")) {
					writer.write(prefixSpaces + actualKey + ": |-\n");

					for (final String line : string.split("\n"))
						writer.write(prefixSpaces + "    " + line + "\n");

					return;
				}
			}

			writer.write(prefixSpaces + actualKey + ": " + this.yaml.dump(SerializeUtil.serialize(obj)));

		} else if (obj instanceof List)
			this.writeList((List<?>) obj, actualKey, prefixSpaces, writer);

		else
			writer.write(prefixSpaces + actualKey + ": " + this.yaml.dump(SerializeUtil.serialize(obj)));

	}

	// Writes a configuration section
	private void writeSection(BufferedWriter writer, String actualKey, String prefixSpaces, YamlConfig section) throws IOException {
		if (section.getKeys(false).isEmpty())
			writer.write(prefixSpaces + actualKey + ":");

		else
			writer.write(prefixSpaces + actualKey + ":");

		writer.write("\n");
	}

	// Writes a list of any object
	private void writeList(List<?> list, String actualKey, String prefixSpaces, BufferedWriter writer) throws IOException {
		writer.write(this.dumpListAsString(list, actualKey, prefixSpaces));
	}

	private String dumpListAsString(List<?> list, String actualKey, String prefixSpaces) {
		final StringBuilder builder = new StringBuilder(prefixSpaces).append(actualKey).append(":");

		if (list.isEmpty()) {
			builder.append(" []\n");
			return builder.toString();
		}

		builder.append("\n");

		for (int i = 0; i < list.size(); i++) {
			final Object o = list.get(i);

			if (o instanceof String || o instanceof Character)
				builder.append(prefixSpaces).append("- '").append(o.toString().replace("'", "''")).append("'");
			else if (o instanceof List)
				builder.append(prefixSpaces).append("- '").append(this.yaml.dump(SerializeUtil.serialize(o)));
			else
				builder.append(prefixSpaces).append("- ").append(SerializeUtil.serialize(o));

			if (i != list.size())
				builder.append("\n");
		}

		return builder.toString();
	}

	// Key is the config key, value = comment and/or ignored sections
	// Parses comments, blank lines, and ignored sections
	private Map<String, String> dumpComments(List<String> lines) {
		final Map<String, String> comments = new LinkedHashMap<>();
		final StringBuilder builder = new StringBuilder();
		final StringBuilder keyBuilder = new StringBuilder();
		int lastLineIndentCount = 0;

		//outer:
		for (final String line : lines) {
			if (line != null && line.trim().startsWith("-"))
				continue;

			if (line == null || line.trim().equals("") || line.trim().startsWith("#"))
				builder.append(line).append("\n");
			else {
				lastLineIndentCount = this.setFullKey(keyBuilder, line, lastLineIndentCount);

				if (keyBuilder.length() > 0) {
					comments.put(keyBuilder.toString(), builder.toString());
					builder.setLength(0);
				}
			}
		}

		if (builder.length() > 0)
			comments.put(null, builder.toString());

		return comments;
	}

	// Counts spaces in front of key and divides by 2 since 1 indent = 2 spaces
	private int countIndents(String s) {
		int spaces = 0;

		for (final char c : s.toCharArray())
			if (c == ' ')
				spaces += 1;
			else
				break;

		return spaces / 2;
	}

	// Ex. keyBuilder = key1.key2.key3 --> key1.key2
	private void removeLastKey(StringBuilder keyBuilder) {
		String temp = keyBuilder.toString();
		final String[] keys = temp.split("\\.");

		if (keys.length == 1) {
			keyBuilder.setLength(0);
			return;
		}

		temp = temp.substring(0, temp.length() - keys[keys.length - 1].length() - 1);
		keyBuilder.setLength(temp.length());
	}

	// Updates the keyBuilder and returns configLines number of indents
	private int setFullKey(StringBuilder keyBuilder, String configLine, int lastLineIndentCount) {
		final int currentIndents = this.countIndents(configLine);
		final String key = configLine.trim().split(":")[0];

		if (keyBuilder.length() == 0)
			keyBuilder.append(key);

		else if (currentIndents == lastLineIndentCount) {
			// Replace the last part of the key with current key
			this.removeLastKey(keyBuilder);

			if (keyBuilder.length() > 0)
				keyBuilder.append(".");

			keyBuilder.append(key);
		} else if (currentIndents > lastLineIndentCount)
			// Append current key to the keyBuilder
			keyBuilder.append(".").append(key);

		else {
			final int difference = lastLineIndentCount - currentIndents;

			for (int i = 0; i < difference + 1; i++)
				this.removeLastKey(keyBuilder);

			if (keyBuilder.length() > 0)
				keyBuilder.append(".");

			keyBuilder.append(key);
		}

		return currentIndents;
	}

	private String getPrefixSpaces(int indents) {
		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < indents; i++)
			builder.append("  ");

		return builder.toString();
	}

	private void appendPrefixSpaces(StringBuilder builder, int indents) {
		builder.append(this.getPrefixSpaces(indents));
	}

	/**
	 * See {@link ConfigSerializable#serialize()}
	 */
	@Override
	public final SerializedMap serialize() {
		final SerializedMap map = SerializedMap.of(this.map);

		map.overrideAll(this.onSerialize());

		return map;
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
	protected String compilePathPrefix(@NonNull final String path) {
		final String prefixed = this.pathPrefix != null ? this.pathPrefix + (!path.isEmpty() ? "." + path : "") : path;

		return prefixed.endsWith(".") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;
	}

	/**
	 * Sets path prefix to the given path prefix
	 *
	 * @param pathPrefix
	 */
	protected void setPathPrefix(final String pathPrefix) {
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
		return this.pathPrefix;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Header
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Set the header of this file. DOES NOT SAVE THE FILE. Must be done before calling save().
	 * Only works when default file DOES NOT EXIST, otherwise we use its header and comments instead.
	 *
	 * @param value
	 */
	public final void setHeader(String... value) {
		this.setHeader(Arrays.asList(value));
	}

	/**
	 * Set the header of this file. DOES NOT SAVE THE FILE. Must be done before calling save().
	 * Only works when default file DOES NOT EXIST, otherwise we use its header and comments instead.
	 *
	 * @param value
	 */
	@NonNull
	public final void setHeader(List<String> value) {
		Valid.checkBoolean(this.defaults == null, "Cannot use setHeader when defaults are set (we then automatically pull the header from default: " + this.defaultsPath + ")");

		this.header = value == null ? Collections.emptyList() : Collections.unmodifiableList(value);
	}

	private final String buildHeader() {
		List<String> header = this.header;

		if (header.isEmpty() && this.defaults != null && !this.defaults.header.isEmpty())
			header = this.defaults.header;

		if (header.isEmpty())
			return "";

		final StringBuilder builder = new StringBuilder();
		boolean startedHeader = false;

		for (int i = header.size() - 1; i >= 0; i--) {
			builder.insert(0, "\n");

			if (startedHeader || header.get(i).length() != 0) {
				builder.insert(0, header.get(i));
				builder.insert(0, '#');

				startedHeader = true;
			}
		}

		return builder.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Utils
	// ------------------------------------------------------------------------------------------------------------

	@NonNull
	private YamlConfig createSection(@NonNull String path) {
		Valid.checkNotEmpty(path, "Cannot create section at empty path");
		final YamlConfig root = this.root;

		if (root == null)
			throw new IllegalStateException("Cannot create section without a root");

		int leadingIndex = -1;
		int trailingIndex;
		YamlConfig section = this;

		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String node = path.substring(trailingIndex, leadingIndex);
			final YamlConfig subSection = section.getConfigurationSection(node);
			if (subSection == null)
				section = section.createSection(node);
			else
				section = subSection;
		}

		final String key = path.substring(trailingIndex);
		if (section == this) {
			final YamlConfig result = new YamlConfig(this, key);
			this.map.put(key, result);

			return result;
		}
		return section.createSection(key);
	}

	@NonNull
	private YamlConfig createSection(@NonNull String path, @NonNull Map<?, ?> map) {
		final YamlConfig section = this.createSection(path);

		for (final Map.Entry<?, ?> entry : map.entrySet())
			if (entry.getValue() instanceof Map)
				section.createSection(entry.getKey().toString(), (Map<?, ?>) entry.getValue());
			else
				section.setNoSave(entry.getKey().toString(), entry.getValue());

		return section;
	}

	private void fromNodeTree(@NonNull MappingNode input, @NonNull YamlConfig section) {
		this.constructor.flattenMapping(input);

		for (final NodeTuple nodeTuple : input.getValue()) {
			final Node key = nodeTuple.getKeyNode();
			final String keyString = String.valueOf(this.constructor.construct(key));
			Node value = nodeTuple.getValueNode();

			while (value instanceof AnchorNode)
				value = ((AnchorNode) value).getRealNode();

			if (value instanceof MappingNode && !this.hasSerializedTypeKey((MappingNode) value))
				this.fromNodeTree((MappingNode) value, section.createSection(keyString));
			else
				section.setNoSave(keyString, this.constructor.construct(value));
		}
	}

	private boolean hasSerializedTypeKey(MappingNode node) {
		for (final NodeTuple nodeTuple : node.getValue()) {
			final Node keyNode = nodeTuple.getKeyNode();
			if (!(keyNode instanceof ScalarNode))
				continue;
			final String key = ((ScalarNode) keyNode).getValue();
			if (key.equals(ConfigurationSerialization.SERIALIZED_TYPE_KEY))
				return true;
		}
		return false;
	}

	private void mapChildrenKeys(@NonNull Set<String> output, @NonNull YamlConfig section, boolean deep) {
		if (section instanceof YamlConfig) {
			final YamlConfig sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				output.add(createPath(section, entry.getKey(), this));

				if (deep && entry.getValue() instanceof YamlConfig) {
					final YamlConfig subsection = (YamlConfig) entry.getValue();
					this.mapChildrenKeys(output, subsection, deep);
				}
			}
		} else {
			final Set<String> keys = section.getKeys(deep);

			for (final String key : keys)
				output.add(createPath(section, key, this));
		}
	}

	private void mapChildrenValues(@NonNull Map<String, Object> output, @NonNull YamlConfig section, boolean deep) {
		if (section instanceof YamlConfig) {
			final YamlConfig sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				// Because of the copyDefaults call potentially copying out of order, we must remove and then add in our saved order
				// This means that default values we haven't set end up getting placed first
				// See SPIGOT-4558 for an example using spigot.yml - watch subsections move around to default order
				final String childPath = createPath(section, entry.getKey(), this);

				output.remove(childPath);
				output.put(childPath, entry.getValue());

				if (entry.getValue() instanceof YamlConfig)
					if (deep)
						this.mapChildrenValues(output, (YamlConfig) entry.getValue(), deep);
			}
		} else {
			final Map<String, Object> values = section.getValues(deep);

			for (final Map.Entry<String, Object> entry : values.entrySet())
				output.put(createPath(section, entry.getKey(), this), entry.getValue());
		}
	}

	@Override
	public String toString() {
		return "YamlConfig{file=" + this.getFileName() + ", defaults=" + this.defaultsPath + ", keys=" + this.serialize().keySet() + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	@NonNull
	private static String createPath(@NonNull YamlConfig section, String key) {
		return createPath(section, key, section == null ? null : section.root);
	}

	@NonNull
	private static String createPath(@NonNull YamlConfig section, String key, YamlConfig relativeTo) {
		final YamlConfig root = section.root;

		if (root == null)
			throw new IllegalStateException("Cannot create path without a root");

		final StringBuilder builder = new StringBuilder();

		for (YamlConfig parent = section; parent != null && parent != relativeTo; parent = parent.parent) {
			if (builder.length() > 0)
				builder.insert(0, '.');

			builder.insert(0, parent.path);
		}

		if (key != null && key.length() > 0) {
			if (builder.length() > 0)
				builder.append('.');

			builder.append(key);
		}

		return builder.toString();
	}

	/**
	 * Remove a loaded file from our internal loaded files map
	 *
	 * @param file
	 */
	public static final void unregisterLoadedFile(final File file) {
		synchronized (loadedConfigs) {
			loadedConfigs.removeWeak(file.getAbsolutePath());
		}
	}

	/**
	 * Loads the configuration without defaults from the given file.
	 *
	 * @param file
	 * @return
	 */
	public static YamlConfig fromFile(File file) {
		synchronized (loadedConfigs) {
			final String path = file.getAbsolutePath();
			YamlConfig config = loadedConfigs.get(path);

			if (config == null) {
				config = new YamlConfig();
				config.loadConfiguration(file);

				loadedConfigs.put(path, config);
			}

			return config;
		}
	}

	/**
	 * Loads the configuration trying to copy the internal path from
	 * your JAR file to the same path in plugins, i.e. "settings.yml"
	 *
	 * @param internalPath
	 * @return
	 */
	public static YamlConfig fromInternalPath(String internalPath) {
		return fromPath(internalPath, internalPath);
	}

	/**
	 * Loads the configuration trying to copy the internal path from
	 * your JAR file to a path in plugins, i.e. "prototype/arena.yml"
	 * to arenas/{yourArena}.yml
	 *
	 * @param internalPath
	 * @param diskPath
	 * @return
	 */
	public static YamlConfig fromPath(String internalPath, String diskPath) {
		synchronized (loadedConfigs) {
			YamlConfig config = loadedConfigs.get(internalPath);

			if (config == null) {
				config = new YamlConfig();
				config.loadConfiguration(internalPath, diskPath);

				loadedConfigs.put(internalPath, config);
			}

			return config;
		}
	}

	// ------------------------------------------------------------------------------------
	// Classes helpers
	// ------------------------------------------------------------------------------------

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
			for (final Location point : this.points)
				if (Valid.locationEquals(point, location)) {
					this.points.remove(point);

					this.settings.save();
					return false;
				}

			this.points.add(location);
			this.settings.save();

			return true;
		}

		/**
		 * Add a new location
		 *
		 * @param location
		 */
		public void add(final Location location) {
			Valid.checkBoolean(!this.hasLocation(location), "Location at " + location + " already exists!");

			this.points.add(location);
			this.settings.save();
		}

		/**
		 * Remove an existing location
		 *
		 * @param location
		 */
		public void remove(final Location location) {
			final Location point = this.find(location);
			Valid.checkNotNull(point, "Location at " + location + " does not exist!");

			this.points.remove(point);
			this.settings.save();
		}

		/**
		 * Return true if the given location exists
		 *
		 * @param location
		 * @return
		 */
		public boolean hasLocation(final Location location) {
			return this.find(location) != null;
		}

		/**
		 * Return a validated location from the given location Pretty much the same but
		 * no yaw/pitch
		 *
		 * @param location
		 * @return
		 */
		public Location find(final Location location) {
			for (final Location entrance : this.points)
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
			return Collections.unmodifiableList(this.points);
		}

		/**
		 * Return iterator for this
		 *
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<Location> iterator() {
			return this.points.iterator();
		}

		/**
		 * Get how many points were set
		 *
		 * @return
		 */
		public int size() {
			return this.points.size();
		}
	}

	/**
	 * Helps to accurately display periods for languages with cases, i.e. English vs Slovak/other langs.
	 * in messages set in the accusative case (example: "Please wait 1 second before chatting again")
	 * where as the "1 second" is in accusative (i.e. "Prosím počkajte 1 sekundu")
	 *
	 * 1 second (sekundu)
	 * 2 seconds (sekundy)
	 * 0 or 5+ seconds (sekúnd)
	 */
	public static final class AccusativeHelper {

		private final String accusativeSingural; // 1 second (Slovak case - sekundu)
		private final String accusativePlural; // 2-4 seconds (Slovak case - sekundy, not in English)
		private final String genitivePlural; // 0 or 5+ seconds (Slovak case - sekund)

		private AccusativeHelper(final String raw) {
			final String[] values = raw.split(", ");

			if (values.length == 2) {
				this.accusativeSingural = values[0];
				this.accusativePlural = values[1];
				this.genitivePlural = this.accusativePlural;

				return;
			}

			if (values.length != 3)
				throw new FoException("Malformed type, use format: 'second, seconds' OR 'sekundu, sekundy, sekund' (if your language has it)");

			this.accusativeSingural = values[0];
			this.accusativePlural = values[1];
			this.genitivePlural = values[2];
		}

		public String getPlural() {
			return this.genitivePlural;
		}

		/**
		 * Formats the given number with an appropriate case
		 *
		 * I.e. "5 sekúnd"
		 *
		 * @param count
		 * @return
		 */
		public String formatWithCount(final long count) {
			return count + " " + this.formatWithoutCount(count);
		}

		/**
		 * Just returns the given duration in the specific case for the number.
		 *
		 * I.e. "sekúnd"
		 *
		 * @param count
		 * @return
		 */
		public String formatWithoutCount(final long count) {
			if (count == 1)
				return this.accusativeSingural;

			if (count > 1 && count < 5)
				return this.accusativePlural;

			return this.genitivePlural;
		}
	}

	/**
	 * A simple helper class for storing title messages
	 */
	public static final class TitleHelper {

		/**
		 * Stores the title and the subtitle
		 */
		private final String title, subtitle;

		private TitleHelper(final String title, final String subtitle) {
			this.title = Common.colorize(title);
			this.subtitle = Common.colorize(subtitle);
		}

		/**
		 * Sends the title and subtitle to the player for around 6 seconds.
		 *
		 * @param player
		 */
		public void playLong(final Player player) {
			this.playLong(player, null);
		}

		/**
		 * Sends the title and subtitle to the player for around 6 seconds,
		 * using the replacer function so you can replace variables.
		 *
		 * @param player
		 * @param replacer
		 */
		public void playLong(final Player player, final Function<String, String> replacer) {
			this.play(player, 5, 4 * 20, 15, replacer);
		}

		/**
		 * Sends the title and subtitle to the player for around 3 seconds,
		 * using the replacer function so you can replace variables.
		 *
		 * @param player
		 */
		public void playShort(final Player player) {
			this.playShort(player, null);
		}

		/**
		 * Sends the title and subtitle to the player for around 3 seconds,
		 * using the replacer function so you can replace variables.
		 *
		 * @param player
		 * @param replacer
		 */
		public void playShort(final Player player, final Function<String, String> replacer) {
			this.play(player, 3, 2 * 20, 5, replacer);
		}

		/**
		 * Plays this title for player with the given fadeIn, stay and fadeOut periods (in ticks).
		 *
		 * @param player
		 * @param fadeIn
		 * @param stay
		 * @param fadeOut
		 */
		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut) {
			this.play(player, fadeIn, stay, fadeOut, null);
		}

		/**
		 * Plays this title for player with the given fadeIn, stay and fadeOut periods (in ticks)
		 * using the replacer function so you can replace variables.
		 *
		 * @param player
		 * @param fadeIn
		 * @param stay
		 * @param fadeOut
		 * @param replacer
		 */
		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut, Function<String, String> replacer) {
			Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer != null ? replacer.apply(this.title) : this.title, replacer != null ? replacer.apply(this.subtitle) : this.subtitle);
		}
	}
}

/**
 * Backport support for serialization of Bukkit values (and those used by plugin authors
 * using {@link ConfigurationSerializable} from Bukkit.
 */
final class YamlConstructor extends SafeConstructor {

	public YamlConstructor() {
		this.yamlConstructors.put(Tag.MAP, new ConstructYamlMap() {

			@Override
			public Object construct(@NonNull Node node) {
				if (node.isTwoStepsConstruction())
					throw new YAMLException("Unexpected referential mapping structure. Node: " + node);

				final Map<Object, Object> raw = (Map<Object, Object>) super.construct(node);

				if (raw.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
					final Map<String, Object> typed = new LinkedHashMap<>(raw.size());

					for (final Map.Entry<Object, Object> entry : raw.entrySet())
						typed.put(entry.getKey().toString(), entry.getValue());

					try {
						return ConfigurationSerialization.deserializeObject(typed);

					} catch (final IllegalArgumentException ex) {
						throw new YAMLException("Could not deserialize object", ex);
					}
				}

				return raw;
			}

			@Override
			public final void construct2ndStep(@NonNull Node node, @NonNull Object object) {
				throw new YAMLException("Unexpected mapping structure. Node: " + node);
			}
		});
	}

	public Object construct(@NonNull Node node) {
		return this.constructObject(node);
	}

	@Override
	public void flattenMapping(@NonNull final MappingNode node) {
		super.flattenMapping(node);
	}
}

/**
 * Backport support for serialization of Bukkit values (and those used by plugin authors
 * using {@link ConfigurationSerializable} from Bukkit.
 */
final class YamlRepresenter extends Representer {

	public YamlRepresenter() {
		this.multiRepresenters.put(ConfigurationSerialization.class, new RepresentMap() {

			@Override
			@NonNull
			public Node representData(@NonNull Object data) {
				final ConfigurationSerializable serializable = (ConfigurationSerializable) data;
				final Map<String, Object> values = new LinkedHashMap<>();

				values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, serializable.getClass().getName());
				values.putAll(serializable.serialize());

				return super.representData(values);
			}
		});

		this.multiRepresenters.remove(Enum.class);
	}
}
