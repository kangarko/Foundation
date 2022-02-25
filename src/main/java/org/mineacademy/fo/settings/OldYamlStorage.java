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
import org.mineacademy.fo.plugin.SimplePlugin;
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.citizensnpcs.api.util.YamlStorage;

final class OldYamlStorage implements ConfigSerializable {

	public static final String NO_DEFAULT = null;

	/*
	 * All currently loaded configurations, stored by disk file name.
	 */
	private static final StrictMap<String, YamlStorage> loadedConfigs = new StrictMap<>();

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
	final Map<String, Object> map = new LinkedHashMap<>();

	/*
	 * The root of this yaml config.
	 */
	private final YamlStorage root;

	/*
	 * The parent object of this yaml config.
	 */
	private final YamlStorage parent;

	/*
	 * The paths stored in the map, delimited by "." such as "Player.Properties.Color"
	 */
	private final String path;

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
	@Getter(value = AccessLevel.PACKAGE)
	@Nullable
	private YamlStorage defaults;

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

	/*
	 * Are we loading the config right now? Prevents duplicate calls and forces saving
	 */
	private boolean loading = false;

	protected OldYamlStorage() {
		this(null, "");
	}

	/*
	 * Load and prepare configuration, super compatible down to Minecraft 1.2.5.
	 */
	private OldYamlStorage(YamlStorage parent, String path) {
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

		if (!path.isEmpty() && parent != null)
			createPath(parent, path);
	}

	// ------------------------------------------------------------------------------------
	// Options
	// ------------------------------------------------------------------------------------

	protected void onLoadFinish() {
	}

	protected SerializedMap onSerialize() {
		return new SerializedMap();
	}

	protected void onSave() {
	}

	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	public boolean isValid() {
		return this.getObject("") instanceof YamlStorage;
	}

	// ------------------------------------------------------------------------------------
	// Miscellaneous
	// ------------------------------------------------------------------------------------

	public String getFileName() {
		return this.file == null ? null : this.file.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting values
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * Retrieve an object at the given config path, attempting to cast it using vanilla java
	 * and supply with default if set. If defaults file exists, we attempt to copy the
	 * default value to the file.
	 */
	private <T> T getT(@NonNull String path, Class<T> type, Object def) {
		path = this.buildPathPrefix(path);
		Valid.checkBoolean(!path.endsWith("."), "Path must not end with '.': " + path);

		// Copy defaults if not set and log about this change
		this.addDefaultIfNotExist(path, type);

		Object raw = this.getFast0(path);

		// Ensure that the default config actually did have the value, if used
		if (this.defaults != null)
			Valid.checkNotNull(raw, "Failed to set '" + path + "' to " + type.getSimpleName() + " from default config's value: " + this.defaults.getFast0(path) + ", has values: " + this.map.keySet());

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
	private Object getFast0(String path) {

		if (path.isEmpty())
			return this;

		if (this.root == null)
			throw new IllegalStateException("Cannot access section without a root");

		int leadingIndex = -1;
		int lowerIndex;
		YamlStorage section = this;

		while ((leadingIndex = path.indexOf('.', lowerIndex = leadingIndex + 1)) != -1) {
			final String currentPath = path.substring(lowerIndex, leadingIndex);
			if (!section.isSetFast0(currentPath))
				return null;

			section = section.getSectionNoPrefix(currentPath);

			if (section == null)
				return null;
		}

		final String key = path.substring(lowerIndex);

		if (section == this)
			return this.map.get(key);

		return section.getFast0(key);
	}

	public final <T> T get(final String path, final Class<T> type, Object... deserializeParams) {
		return this.get(path, type, null, deserializeParams);
	}

	public final <T> T get(final String path, final Class<T> type, final T def, Object... deserializeParams) {
		final Object object = this.getT(path, Object.class, def);

		return object != null ? SerializeUtil.deserialize(type, object, deserializeParams) : def;
	}

	public Object getObject(final String path, final Object def) {
		return this.isSet(path) ? this.getObject(path) : def;
	}

	public Object getObject(final String path) {
		return this.getT(path, Object.class, null);
	}

	@Nullable
	public Boolean getBoolean(final String path, final boolean def) {
		return this.isSet(path) ? this.getBoolean(path) : def;
	}

	@Nullable
	public Boolean getBoolean(final String path) {
		return this.getT(path, Boolean.class, null);
	}

	public String getString(final String path, final String def) {
		return this.isSet(path) ? this.getString(path) : def;
	}

	public String getString(final String path) {
		final Object object = this.getObject(path);

		if (object == null)
			return null;

		else if (object instanceof List)
			return Common.join((List<?>) object, "\n");

		else if (object instanceof String[])
			return Common.join(Arrays.asList((String[]) object), "\n");

		else if (object.getClass().isArray())
			return Common.join((Object[]) object);

		else if (object instanceof Boolean
				|| object instanceof Integer
				|| object instanceof Long
				|| object instanceof Double
				|| object instanceof Float)
			return Objects.toString(object);

		else if (object instanceof Number)
			return ((Number) object).toString();

		else if (object instanceof String)
			return (String) object;

		throw new FoException("Excepted string at '" + path + "' in " + this.getFileName() + ", got (" + object.getClass() + "): " + object);
	}

	public Long getLong(final String path, final Long def) {
		return this.isSet(path) ? this.getLong(path) : def;
	}

	@Nullable
	public Long getLong(final String path) {
		return this.getT(path, Long.class, null);
	}

	@Nullable
	public Integer getInteger(final String path, final Integer def) {
		return this.isSet(path) ? this.getInteger(path) : def;
	}

	@Nullable
	public Integer getInteger(final String path) {
		return this.getT(path, Integer.class, null);
	}

	@Nullable
	public Double getDouble(final String path, final Double def) {
		return this.isSet(path) ? this.getDouble(path) : def;
	}

	@Nullable
	public Double getDouble(final String path) {
		final Object raw = this.getObject(path);

		return raw != null ? Double.parseDouble(raw.toString()) : null;
	}

	public Location getLocation(final String path, final Location def) {
		return this.isSet(path) ? this.getLocation(path) : def;
	}

	public Location getLocation(final String path) {
		return this.get(path, Location.class);
	}

	public OfflinePlayer getOfflinePlayer(final String path, final OfflinePlayer def) {
		return this.isSet(path) ? this.getOfflinePlayer(path) : def;
	}

	public OfflinePlayer getOfflinePlayer(final String path) {
		return this.getT(path, OfflinePlayer.class, null);
	}

	public SimpleSound getSound(final String path, final SimpleSound def) {
		return this.isSet(path) ? this.getSound(path) : def;
	}

	@Nullable
	public SimpleSound getSound(final String path) {
		return this.isSet(path) ? new SimpleSound(this.getString(path)) : null;
	}

	public AccusativeHelper getAccusativePeriod(final String path, final AccusativeHelper def) {
		return this.isSet(path) ? this.getAccusativePeriod(path) : def;
	}

	@Nullable
	public AccusativeHelper getAccusativePeriod(final String path) {
		return this.isSet(path) ? new AccusativeHelper(this.getString(path)) : null;
	}

	public TitleHelper getTitle(final String path, final String defTitle, final String defSubtitle) {
		return this.isSet(path) ? this.getTitle(path) : new TitleHelper(defTitle, defSubtitle);
	}

	@Nullable
	public TitleHelper getTitle(final String path) {
		final String title = this.getString(path + ".Title");
		final String subtitle = this.getString(path + ".Subtitle");

		return this.isSet(path) ? new TitleHelper(title, subtitle) : null;
	}

	public SimpleTime getTime(final String path, final String def) {
		return this.isSet(path) ? this.getTime(path) : def != null ? SimpleTime.from(def) : null;
	}

	public SimpleTime getTime(final String path) {
		final Object obj = this.getObject(path);

		return obj != null ? SimpleTime.from(obj.toString()) : null;
	}

	@Nullable
	public Double getPercentage(String path) {
		if (this.isSet(path)) {
			final String raw = this.getObject(path).toString();
			Valid.checkBoolean(raw.endsWith("%"), "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must end with %! Got: " + raw);

			final String rawNumber = raw.substring(0, raw.length() - 1);
			Valid.checkInteger(rawNumber, "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must be a whole number! Got: " + raw);

			return Integer.parseInt(rawNumber) / 100D;
		}

		return null;
	}

	public BoxedMessage getBoxedMessage(final String path, final String def) {
		return this.isSet(path) ? this.getBoxedMessage(path) : new BoxedMessage(def);
	}

	@Nullable
	public BoxedMessage getBoxedMessage(final String path) {
		return this.isSet(path) ? new BoxedMessage(this.getString(path)) : null;
	}

	public CompMaterial getMaterial(final String path, final CompMaterial def) {
		return this.isSet(path) ? this.getMaterial(path) : def;
	}

	public CompMaterial getMaterial(final String path) {
		final String name = this.getString(path);

		return name == null ? null : CompMaterial.fromStringStrict(name);
	}

	public <K, V> Tuple<K, V> getTuple(final String key, Class<K> keyType, Class<V> valueType) {
		return this.getTuple(key, null, keyType, valueType);
	}

	public <K, V> Tuple<K, V> getTuple(final String key, final Tuple<K, V> def, Class<K> keyType, Class<V> valueType) {
		final SerializedMap map = this.getMap(key);

		return !map.isEmpty() ? Tuple.deserialize(map, keyType, valueType) : def;
	}

	public ItemStack getItemStack(@NonNull String path) {
		return this.getItemStack(path, null);
	}

	public ItemStack getItemStack(@NonNull String path, ItemStack def) {
		return this.isSet(path) ? this.get(path, ItemStack.class) : def;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting lists
	// ------------------------------------------------------------------------------------------------------------

	public LocationList getLocationList(final String path) {
		return new LocationList(this, this.getList(path, Location.class));
	}

	public List<Object> getList(final String path) {
		final Object obj = this.getObject(path);

		// Allow one values instead of lists, such as
		// "Apply_On: timed" instead of "Apply_On: [timed]" for convenience
		return obj instanceof List ? (List<Object>) obj : obj != null ? Arrays.asList(obj) : new ArrayList<>();
	}

	public List<SerializedMap> getMapList(final String path) {
		return this.getList(path, SerializedMap.class);
	}

	public final <T> Set<T> getSet(final String key, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = this.getList(key, type);

		return list == null ? new HashSet<>() : new HashSet<>(list);
	}

	public <T> List<T> getList(final String path, final Class<T> type) {
		return this.getList(path, type, (Object[]) null);
	}

	public final <T> List<T> getList(final String path, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = new ArrayList<>();
		final List<Object> objects = this.getList(path);

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

	public <T> IsInList<T> getIsInList(String path, Class<T> type) {
		final List<String> stringList = this.getStringList(path);

		if (stringList.size() == 1 && "*".equals(stringList.get(0)))
			return IsInList.fromStar();

		return IsInList.fromList(this.getList(path, type));
	}

	public List<String> getStringList(final String path) {
		final Object raw = this.getObject(path);

		if (raw == null)
			return new ArrayList<>();

		if (raw instanceof String) {
			final String output = (String) raw;

			return "'[]'".equals(output) || "[]".equals(output) ? new ArrayList<>() : Arrays.asList(output);
		}

		if (raw instanceof List)
			return this.fixYamlBooleansInList((List<Object>) raw);

		throw new FoException("Excepted a list at '" + path + "' in " + this.getFileName() + ", got (" + raw.getClass() + "): " + raw);
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

	@Nullable
	public StrictList<String> getCommandList(final String path) {

		// Nowhere to copy from
		if (!this.isSet(path) && this.defaults == null)
			return null;

		final List<String> list = this.getStringList(path);
		Valid.checkBoolean(!list.isEmpty(), "Please set at least one command alias in '" + path + "' (" + this.getFileName() + ") for this will be used as your main command!");

		for (int i = 0; i < list.size(); i++) {
			String command = list.get(i);

			command = command.startsWith("/") ? command.substring(1) : command;
			list.set(i, command);
		}

		return new StrictList<>(list);
	}

	public StrictList<CompMaterial> getMaterialList(final String path) {
		final StrictList<CompMaterial> list = new StrictList<>();

		for (final String raw : this.getStringList(path)) {
			final CompMaterial mat = CompMaterial.fromString(raw);

			if (mat != null)
				list.add(mat);
		}

		return list;
	}

	public SerializedMap getMap(final String path) {
		final LinkedHashMap<?, ?> map = this.getMap(path, Object.class, Object.class);

		return SerializedMap.of(map);
	}

	public final <Key, Value> LinkedHashMap<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType, Object... valueDeserializeParams) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, Value> map = new LinkedHashMap<>();
		final boolean exists = this.isSet(path);

		// Add path prefix right away
		path = this.buildPathPrefix(path);

		// Add defaults
		if (this.defaults != null && !exists) {
			Valid.checkBoolean(this.defaults.isSet(path), "Default '" + this.getFileName() + "' lacks a map at " + path);

			for (final String key : this.defaults.getSectionNoPrefix(path).getKeys(false))
				this.addDefaultIfNotExist(path + "." + key, valueType);
		}

		// Load key-value pairs from config to our map
		if (exists) {
			final Object object = this.getFast0(path);
			Valid.checkBoolean(object instanceof YamlStorage, "Expected a map at '" + path + "', got " + object.getClass().getSimpleName() + ": " + object);

			for (final Map.Entry<String, Object> entry : ((YamlStorage) object).map.entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final Value value = SerializeUtil.deserialize(valueType, entry.getValue(), valueDeserializeParams);

				// Ensure the pair values are valid for the given paramenters
				this.checkAssignable(false, path, key, keyType);
				this.checkAssignable(false, path, value, valueType);

				map.put(key, value);
			}
		}

		return map;
	}

	public final <Key, Value> LinkedHashMap<Key, List<Value>> getMapList(@NonNull String path, final Class<Key> keyType, final Class<Value> setType, Object... setDeserializeParameters) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, List<Value>> map = new LinkedHashMap<>();
		final boolean exists = this.isSet(path);

		// Add path prefix right away
		path = this.buildPathPrefix(path);

		// Add defaults
		if (this.defaults != null && !exists) {
			Valid.checkBoolean(this.defaults.isSet(path), "Default '" + this.getFileName() + "' lacks a map at " + path);

			for (final String key : this.defaults.getSectionNoPrefix(path).getKeys(false))
				this.addDefaultIfNotExist(path + "." + key, setType);
		}

		// Load key-value pairs from config to our map
		if (exists) {
			final Object object = this.getFast0(path);
			Valid.checkBoolean(object instanceof YamlStorage, "Expected a map at '" + path + "', got " + object.getClass().getSimpleName() + ": " + object);

			for (final Map.Entry<String, Object> entry : ((YamlStorage) object).map.entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final List<Value> value = SerializeUtil.deserialize(List.class, entry.getValue(), setDeserializeParameters);

				// Ensure the pair values are valid for the given paramenters
				this.checkAssignable(false, path, key, keyType);

				if (!value.isEmpty())
					for (final Value item : value)
						this.checkAssignable(false, path, item, setType);

				map.put(key, value);
			}
		}

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting sections
	// ------------------------------------------------------------------------------------------------------------

	@Nullable
	public YamlStorage getConfigurationSection(@NonNull String path) {
		return this.getSectionNoPrefix(this.buildPathPrefix(path));
	}

	@Nullable
	public YamlStorage getSectionNoPrefix(@NonNull String path) {
		Object val = this.getFast0(path);

		if (val != null)
			return val instanceof YamlStorage ? (YamlStorage) val : null;

		val = this.getFast0(path);

		return val instanceof YamlStorage ? this.createSection(path) : null;
	}

	@NonNull
	public Set<String> getKeys(boolean deep) {
		final Set<String> result = new LinkedHashSet<>();
		this.mapChildrenKeys(result, this, deep);

		return result;
	}

	@NonNull
	public Map<String, Object> getValues(boolean deep) {
		final Map<String, Object> result = new LinkedHashMap<>();
		this.mapChildrenValues(result, this, deep);

		return result;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Loading
	// ------------------------------------------------------------------------------------------------------------

	public void reload() {
		synchronized (loadedConfigs) {
			Valid.checkNotNull(this.file, "Cannot call reload() before loading since we lack a file to load from yet!");

			this.loadConfiguration(this.defaultsPath, this.file);
		}
	}

	public void loadConfiguration(String defaultPath) {
		this.loadConfiguration(defaultPath, defaultPath);
	}

	public void loadConfiguration(File file) {
		final String dataPath = SimplePlugin.getData().getAbsolutePath();
		final String internalPath = file.getAbsolutePath().replace(dataPath, "").replace("\\", "/").substring(1);

		this.loadConfiguration(FileUtil.getInternalFileContent(internalPath) != null ? internalPath : null, file);
	}

	public void loadConfiguration(@Nullable String from, @NonNull String to) {
		synchronized (loadedConfigs) {
			final File toFile = from != null ? FileUtil.extract(from, to) : FileUtil.getOrMakeFile(to);

			this.loadConfiguration(from, toFile);
		}
	}

	public void loadConfiguration(@Nullable String from, @NonNull File toFile) {
		synchronized (loadedConfigs) {
			Valid.checkBoolean(!this.loading, "Already loading configuration " + from + " to " + toFile);

			this.loading = true;

			try {
				final InputStreamReader stream = new InputStreamReader(new FileInputStream(toFile), StandardCharsets.UTF_8);

				this.file = toFile;
				this.defaultsPath = from;

				// Handle defaults
				if (from != null) {
					this.defaults = new YamlStorage();

					final String content = FileUtil.getInternalFileContent(from);
					Valid.checkNotNull("Failed to find default configuration at path " + from + " (did you reload?)");

					this.defaults.loadFromString0(content);
				}

				this.loadFromReader0(stream);
				this.onLoadFinish();

				// Wait with saving and save changes all at once to reduce expensive calls
				this.loading = false;
				this.save();

			} catch (final IOException ex) {
				throw new FoException(ex, "Unable to load configuration from file " + this.file);

			} finally {

				// Enforce stop loading when an exception is thrown above
				this.loading = false;
			}
		}
	}

	private void loadFromReader0(@NonNull Reader reader) throws IOException {
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

		this.loadFromString0(builder.toString());
	}

	private void loadFromString0(@NonNull String contents) {
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
	}

	// ------------------------------------------------------------------------------------
	// Checking for validity and defaults
	// ------------------------------------------------------------------------------------

	public boolean isSet(@NonNull String path) {
		return this.isSetAbsolute(this.buildPathPrefix(path));
	}

	public boolean isSetAbsolute(@NonNull String path) {
		return this.root != null && this.isSetFast0(path);
	}

	private boolean isSetFast0(@NonNull String path) {
		return this.getFast0(path) != null;
	}

	/*private YamlConfig getDefaultSectionNoPrefix() {
		final YamlConfig defaults = this.root == null ? null : this.root.getDefaults();

		if (defaults != null)
			if (defaults.getFast0(this.fullPath) instanceof YamlConfig)
				return defaults.getSectionNoPrefix(this.fullPath);

		return null;
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

			this.setNoSerializeNoPrefix(pathAbs, object);
		}
	}

	private <T> void checkAndFlagForSave(final String path, final T def) {
		if (this.defaults != null)
			Valid.checkNotNull(def, "Inbuilt config " + this.getFileName() + " lacks " + (def == null ? "key" : def.getClass().getSimpleName()) + " at \"" + path + "\". Is it outdated?");

		Common.log("&7Update " + this.getFileName() + " at &b\'&f" + path + "&b\' &7-> " + (def == null ? "&ckey removed" : "&b\'&f" + def + "&b\'") + "&r");
	}

	private void checkAssignable(final boolean fromDefault, final String path, final Object value, final Class<?> clazz) {
		if (!clazz.isAssignableFrom(value.getClass()) && !clazz.getSimpleName().equals(value.getClass().getSimpleName())) {

			// Exception
			//if (ConfigSerializable.class.isAssignableFrom(clazz) && value instanceof MemorySection)
			//	return;

			throw new FoException("Malformed configuration! Key '" + path + "' in " + (fromDefault ? "inbuilt " : "") + this.getFileName() + " must be " + clazz.getSimpleName() + " but got " + value.getClass().getSimpleName() + ": '" + value + "'");
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Saving
	// ------------------------------------------------------------------------------------------------------------

	public void move(final String fromRelative, final String toAbsolute) {
		this.move(this.getFast0(fromRelative), fromRelative, toAbsolute);
	}

	public void move(final Object value, String fromPathRel, final String toPathAbs) {
		final String oldPathPrefix = this.pathPrefix;

		fromPathRel = this.buildPathPrefix(fromPathRel);

		this.set(fromPathRel, null);
		this.pathPrefix = oldPathPrefix; // set to previous

		this.set(toPathAbs, value);

		Common.log("&7Update " + this.getFileName() + ". Move &b\'&f" + fromPathRel + "&b\' &7(was \'" + value + "&7\') to " + "&b\'&f" + toPathAbs + "&b\'" + "&r");

		this.pathPrefix = oldPathPrefix; // and reset back to whatever it was
	}

	public void save(@NonNull String path, Object value) {
		this.set(path, value);

		this.save();
	}

	public void set(@NonNull String path, @Nullable Object value) {

		Valid.checkBoolean(!this.onSerialize().keySet().contains(path), "Cannot set a config path '" + path + "' that's specified in onSerialize() method, this causes conflicts!"
				+ " Only call save() and we call onSerialize() automatically");

		// Turn into something we can save
		value = SerializeUtil.serialize(value);

		this.setNoSerializeNoPrefix(path, value);
	}

	private void setNoSerializeNoPrefix(@NonNull String path, @Nullable Object value) {
		Valid.checkNotEmpty(path, "Cannot set to an empty path");

		final YamlStorage root = this.root;

		if (root == null)
			throw new IllegalStateException("Cannot use section without a root");

		int leadingIndex = -1, trailingIndex;
		YamlStorage section = this;

		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String node = path.substring(trailingIndex, leadingIndex);
			final YamlStorage subSection = section.getSectionNoPrefix(node);
			if (subSection == null) {
				if (value == null)
					// no need to create missing sub-sections if we want to remove the value:
					return;
				section = section.createSection(node);
			} else
				section = subSection;
		}

		final String key = path.substring(trailingIndex);

		if (section == this) {
			if (value == null)
				this.map.remove(key);

			else
				this.map.put(key, value);

		} else
			section.setNoSerializeNoPrefix(key, value);
	}

	public boolean deleteFile() {
		if (this.file.exists()) {
			this.file.delete();
			unregisterLoadedFile(this.file);

			return true;
		}

		return false;
	}

	public void clear() {
		this.save("", null);
	}

	public void save() {
		Valid.checkNotNull(this.file, "Cannot call save() method when no file was set (did you call load(File)?)");

		this.save(this.file);
	}

	public void save(@NonNull String filePath) {
		this.save(FileUtil.getFile(filePath));
	}

	public void save(@NonNull File file) {

		try {

			// Ignore when loading since we save it anyways afterwards
			if (this.loading)
				return;

			// Early pre-save
			this.onSave();

			// Copy serialized values to our internal map
			for (final Map.Entry<String, Object> entry : this.serialize())
				this.setNoSerializeNoPrefix(entry.getKey(), SerializeUtil.serialize(entry.getValue()));

			if (this.defaults == null) {

				// Hacky: a professional method to remove all null values
				//removeNuls(this.map);

				final File parent = file.getCanonicalFile().getParentFile();

				// Create parent dirs
				if (parent != null)
					parent.mkdirs();

				// Dump to file
				final Map<String, Object> values = this.getValues(false);
				final String header = this.buildHeader();

				System.out.println("=== SAVE " + this.getFileName() + " ===");

				String dump = this.yaml.dump(SerializeUtil.serialize(values));

				if (dump.equals("{}\n")) // empty config
					dump = "";

				final Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);

				try {
					writer.write(header + dump);

				} finally {
					writer.close();
				}

				return;
			}

			Valid.checkNotNull(this.getUncommentedSections(), "getUncommentedSections() cannot be null, return an empty list instead in " + this);

			final String newContent = FileUtil.getInternalFileContent(this.defaultsPath);
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8));

			// ignoredSections can ONLY contain configurations sections
			for (final String ignoredSection : this.getUncommentedSections())
				if (this.defaults.isSet(ignoredSection))
					Valid.checkBoolean(this.defaults.getFast0(ignoredSection) instanceof YamlStorage, "Can only ignore config sections in " + this.defaultsPath + " (file " + this.file + ")" + " not '" + ignoredSection + "' that is " + this.defaults.getFast0(ignoredSection));

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
				final YamlStorage backupConfig = new YamlStorage();

				backupConfig.loadConfiguration(NO_DEFAULT, "unused/" + this.file.getName());

				for (final Map.Entry<String, Object> entry : removedKeys.entrySet())
					backupConfig.set(entry.getKey(), entry.getValue());

				backupConfig.save();

				Common.warning("The following entries in " + this.file.getName() + " are unused and were moved into " + backupConfig.file.getName() + ": " + removedKeys.keySet());
			}

			final Map<String, String> comments = this.dumpComments(newContent.split("\n"));

			this.write(comments, writer);

		} catch (final IOException ex) {
			throw new FoException(ex, "Unable to save configuration to file " + file);
		}
	}

	/*private static void removeNuls(Map<String, Object> map) {
		for (final Iterator<Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
			final Entry<String, Object> entry = it.next();
			//final String key = entry.getKey();
			final Object value = entry.getValue();

			//System.out.println("Trying to remove " + key + " -> (" + (value == null ? "null" : value.getClass().getSimpleName()) + ") " + value);

			if (value instanceof YamlConfig) {
				final Map<String, Object> childMap = ((YamlConfig) value).map;

				removeNuls(childMap);

				if (childMap.isEmpty())
					it.remove();
			}

			if (value == null
					|| (value instanceof Iterable<?> && !((Iterable<?>) value).iterator().hasNext())
					|| (value.getClass().isArray() && ((Object[]) value).length == 0)
					|| (value instanceof Map<?, ?>) && ((Map<?, ?>) value).isEmpty()) {

				it.remove();
				//System.out.println("\tremoving");

				continue;
			}
		}
	}*/

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

				for (final String ignoredKey : this.getUncommentedSections()) {
					if (key.equals(ignoredKey)) {
						final YamlStorage ignoredSection = this.getSectionNoPrefix(ignoredKey);
						final boolean sectionExists = ignoredSection != null;

						// Write from new to old config
						if (!this.isSet(ignoredKey) || (sectionExists && ignoredSection.getKeys(false).isEmpty())) {
							copyAllowed.add(ignoredKey);

							break;
						}

						// Write from old to new, copying all keys and subkeys manually
						else {
							this.write0(key, true, comments, writer);

							if (sectionExists)
								for (final String oldKey : ignoredSection.getKeys(true))
									this.write0(ignoredKey + "." + oldKey, true, comments, writer);

							reverseCopy.add(ignoredKey);
							continue outerloop;
						}
					}

					if (key.startsWith(ignoredKey))
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
		if (newObj instanceof YamlStorage && !forceNew && oldObj instanceof YamlStorage)
			this.writeSection(writer, actualKey, prefixSpaces, (YamlStorage) oldObj);

		// Write the new section, old value is no more
		else if (newObj instanceof YamlStorage)
			this.writeSection(writer, actualKey, prefixSpaces, (YamlStorage) newObj);

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
	private void writeSection(BufferedWriter writer, String actualKey, String prefixSpaces, YamlStorage section) throws IOException {
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
	private Map<String, String> dumpComments(String[] lines) {
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

	@Override
	public SerializedMap serialize() {
		final SerializedMap map = SerializedMap.of(this.map);

		map.overrideAll(this.onSerialize());

		return map;
	}

	// ------------------------------------------------------------------------------------
	// Path prefix
	// ------------------------------------------------------------------------------------

	String buildPathPrefix(@NonNull final String path) {
		final String prefixed = this.pathPrefix != null ? this.pathPrefix + (!path.isEmpty() ? "." + path : "") : path;

		return prefixed.endsWith(".") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;
	}

	protected void setPathPrefix(final String pathPrefix) {
		if (pathPrefix != null) {
			Valid.checkBoolean(!pathPrefix.endsWith("."), "Path prefix must not end with a dot: " + pathPrefix);
			Valid.checkBoolean(!pathPrefix.endsWith(".yml"), "Path prefix must not end with .yml!");
		}

		this.pathPrefix = pathPrefix != null && !pathPrefix.isEmpty() ? pathPrefix : null;
	}

	protected String getPathPrefix() {
		return this.pathPrefix;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Header
	// ------------------------------------------------------------------------------------------------------------

	public final void setHeader(String... value) {
		this.setHeader(Arrays.asList(value));
	}

	@NonNull
	public void setHeader(List<String> value) {
		Valid.checkBoolean(this.defaults == null, "Cannot use setHeader when defaults are set (we then automatically pull the header from default: " + this.defaultsPath + ")");

		this.header = value == null ? Collections.emptyList() : Collections.unmodifiableList(value);
	}

	private String buildHeader() {
		final List<String> header = this.header;

		if (header.isEmpty())
			return "";

		final StringBuilder builder = new StringBuilder();
		boolean startedHeader = false;

		for (int i = header.size() - 1; i >= 0; i--) {
			builder.insert(0, "\n");

			if (startedHeader || header.get(i).length() != 0) {
				builder.insert(0, header.get(i));
				builder.insert(0, "# ");

				startedHeader = true;
			}
		}

		return builder.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Utils
	// ------------------------------------------------------------------------------------------------------------

	@NonNull
	private YamlStorage createSection(@NonNull String path) {
		Valid.checkNotEmpty(path, "Cannot create section at empty path");
		final YamlStorage root = this.root;

		if (root == null)
			throw new IllegalStateException("Cannot create section without a root");

		int leadingIndex = -1;
		int trailingIndex;
		YamlStorage section = this;

		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String node = path.substring(trailingIndex, leadingIndex);
			final YamlStorage subSection = section.getSectionNoPrefix(node);
			if (subSection == null)
				section = section.createSection(node);
			else
				section = subSection;
		}

		final String key = path.substring(trailingIndex);
		if (section == this) {
			final YamlStorage result = new YamlStorage(this, key);
			this.map.put(key, result);

			return result;
		}
		return section.createSection(key);
	}

	@NonNull
	private YamlStorage createSection(@NonNull String path, @NonNull Map<?, ?> map) {
		final YamlStorage section = this.createSection(path);

		for (final Map.Entry<?, ?> entry : map.entrySet())
			if (entry.getValue() instanceof Map)
				section.createSection(entry.getKey().toString(), (Map<?, ?>) entry.getValue());
			else
				section.set(entry.getKey().toString(), entry.getValue());

		return section;
	}

	private void fromNodeTree(@NonNull MappingNode input, @NonNull YamlStorage section) {
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
				section.setNoSerializeNoPrefix(keyString, this.constructor.construct(value));
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

	private void mapChildrenKeys(@NonNull Set<String> output, @NonNull YamlStorage section, boolean deep) {
		if (section instanceof YamlStorage) {
			final YamlStorage sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				output.add(createPath(section, entry.getKey(), this));

				if (deep && entry.getValue() instanceof YamlStorage) {
					final YamlStorage subsection = (YamlStorage) entry.getValue();
					this.mapChildrenKeys(output, subsection, deep);
				}
			}
		} else {
			final Set<String> keys = section.getKeys(deep);

			for (final String key : keys)
				output.add(createPath(section, key, this));
		}
	}

	private void mapChildrenValues(@NonNull Map<String, Object> output, @NonNull YamlStorage section, boolean deep) {
		if (section instanceof YamlStorage) {
			final YamlStorage sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				// Because of the copyDefaults call potentially copying out of order, we must remove and then add in our saved order
				// This means that default values we haven't set end up getting placed first
				// See SPIGOT-4558 for an example using spigot.yml - watch subsections move around to default order
				final String childPath = createPath(section, entry.getKey(), this);

				output.remove(childPath);
				output.put(childPath, entry.getValue());

				if (entry.getValue() instanceof YamlStorage)
					if (deep)
						this.mapChildrenValues(output, (YamlStorage) entry.getValue(), deep);
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
	private static String createPath(@NonNull YamlStorage section, String key) {
		return createPath(section, key, section == null ? null : section.root);
	}

	@NonNull
	private static String createPath(@NonNull YamlStorage section, String key, YamlStorage relativeTo) {
		final YamlStorage root = section.root;

		if (root == null)
			throw new IllegalStateException("Cannot create path without a root");

		final StringBuilder builder = new StringBuilder();

		for (YamlStorage parent = section; parent != null && parent != relativeTo; parent = parent.parent) {
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

	public static void unregisterLoadedFile(final File file) {
		synchronized (loadedConfigs) {
			loadedConfigs.removeWeak(file.getAbsolutePath());
		}
	}

	public static YamlStorage fromFile(File file) {
		synchronized (loadedConfigs) {
			final String path = file.getAbsolutePath();
			YamlStorage config = loadedConfigs.get(path);

			if (config == null) {
				config = new YamlStorage();
				config.loadConfiguration(file);

				loadedConfigs.put(path, config);
			}

			return config;
		}
	}

	public static YamlStorage fromInternalPath(String internalPath) {
		return fromPath(internalPath, internalPath);
	}

	public static YamlStorage fromPath(String internalPath, String diskPath) {
		synchronized (loadedConfigs) {
			YamlStorage config = loadedConfigs.get(internalPath);

			if (config == null) {
				config = new YamlStorage();
				config.loadConfiguration(internalPath, diskPath);

				loadedConfigs.put(internalPath, config);
			}

			return config;
		}
	}

	// ------------------------------------------------------------------------------------
	// Classes helpers
	// ------------------------------------------------------------------------------------

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

		public String formatWithCount(final long count) {
			return count + " " + this.formatWithoutCount(count);
		}

		public String formatWithoutCount(final long count) {
			if (count == 1)
				return this.accusativeSingural;

			if (count > 1 && count < 5)
				return this.accusativePlural;

			return this.genitivePlural;
		}
	}

	public static final class TitleHelper {

		private final String title, subtitle;

		private TitleHelper(final String title, final String subtitle) {
			this.title = Common.colorize(title);
			this.subtitle = Common.colorize(subtitle);
		}

		public void playLong(final Player player) {
			this.playLong(player, null);
		}

		public void playLong(final Player player, final Function<String, String> replacer) {
			this.play(player, 5, 4 * 20, 15, replacer);
		}

		public void playShort(final Player player) {
			this.playShort(player, null);
		}

		public void playShort(final Player player, final Function<String, String> replacer) {
			this.play(player, 3, 2 * 20, 5, replacer);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut) {
			this.play(player, fadeIn, stay, fadeOut, null);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut, Function<String, String> replacer) {
			Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer != null ? replacer.apply(this.title) : this.title, replacer != null ? replacer.apply(this.subtitle) : this.subtitle);
		}
	}

	public static final class LocationList implements Iterable<Location> {

		private final YamlStorage settings;

		private final List<Location> points;

		private LocationList(final YamlStorage settings, final List<Location> points) {
			this.settings = settings;
			this.points = points;
		}

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

		public void add(final Location location) {
			Valid.checkBoolean(!this.hasLocation(location), "Location at " + location + " already exists!");

			this.points.add(location);
			this.settings.save();
		}

		public void remove(final Location location) {
			final Location point = this.find(location);
			Valid.checkNotNull(point, "Location at " + location + " does not exist!");

			this.points.remove(point);
			this.settings.save();
		}

		public boolean hasLocation(final Location location) {
			return this.find(location) != null;
		}

		public Location find(final Location location) {
			for (final Location entrance : this.points)
				if (Valid.locationEquals(entrance, location))
					return entrance;

			return null;
		}

		public List<Location> getLocations() {
			return Collections.unmodifiableList(this.points);
		}

		@Override
		public Iterator<Location> iterator() {
			return this.points.iterator();
		}

		public int size() {
			return this.points.size();
		}
	}
}

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

		this.multiRepresenters.put(YamlStorage.class, new RepresentMap() {

			@Override
			@NonNull
			public Node representData(@NonNull Object data) {
				final YamlStorage config = (YamlStorage) data;

				return super.representData(config.map);
			}
		});

		this.multiRepresenters.remove(Enum.class);
	}
}
