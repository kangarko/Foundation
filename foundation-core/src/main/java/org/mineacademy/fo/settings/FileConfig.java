package org.mineacademy.fo.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CaseNumberFormat;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Tuple;

import lombok.NonNull;

/**
 * This is a base class for all file based configurations.
 */
public abstract class FileConfig extends ConfigSection {

	/**
	 * The data file "data.db" (uses YAML) for saving various data.
	 *
	 * @deprecated the .db extension is misleading since it is used for databases
	 */
	@Deprecated
	//public static final String DATA_DB = "data.db";

	/**
	 * A null, used for convenience in {@link #load(String, String)} where the "to" is null.
	 */
	public static final String NO_DEFAULT = null;

	/**
	 * The default configuration.
	 */
	private FileConfig defaults;

	/**
	 * The file this configuration is stored in.
	 */
	private File file;

	/**
	 * The path prefix for this configuration
	 */
	private String pathPrefix;

	/**
	 * Creates an empty {@link FileConfig} with no default values.
	 */
	public FileConfig() {
		super();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Loading
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Loads this {@link FileConfig} from the specified location inside this plugin's jar,
	 * extracting the file.
	 *
	 * The internal path can contain directories such as bosses/Zombie.yml
	 *
	 * @param internalPath
	 */
	public final void load(String internalPath) {
		this.load(internalPath, internalPath);
	}

	/**
	 * Loads this {@link FileConfig} from the specified location inside this plugin's jar,
	 * extracting the file.
	 *
	 * The internal path can contain directories such as bosses/Zombie.yml
	 * and you can specify a different extraction file (the "to" path).
	 *
	 * @param from
	 * @param to
	 */
	public final void load(String from, String to) {
		if (from != null) {
			final List<String> defaultContent = FileUtil.readLinesFromInternalPath(from);
			ValidCore.checkNotNull(defaultContent, "Inbuilt " + from + " not found! Did you reload?");

			// Load main
			this.load(FileUtil.extract(defaultContent, to));

			// Load defaults
			this.defaults = new YamlConfig();
			this.defaults.loadFromString(String.join("\n", defaultContent));

		} else
			this.load(FileUtil.createIfNotExists(to));
	}

	/**
	 * Loads this {@link FileConfig} from the specified location on the disk.
	 *
	 * @param file File to load from.
	 */
	public final void load(@NonNull File file) {
		this.file = file;

		try {
			final FileInputStream stream = new FileInputStream(file);

			this.load(new InputStreamReader(stream, StandardCharsets.UTF_8));

		} catch (final Exception ex) {
			CommonCore.error(ex, "Cannot load " + file);
		}
	}

	/**
	 * Loads this configuration from the specified reader.
	 *
	 * @param reader
	 * @throws IOException
	 */
	public final void load(Reader reader) throws IOException {
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

	/**
	 * Loads this {@link FileConfig} from the specified string content.
	 *
	 * @param contents
	 */
	protected abstract void loadFromString(String contents);

	// ------------------------------------------------------------------------------------------------------------
	// Saving
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Saves this {@link FileConfig} to the specified location.
	 *
	 * If the file does not exist, it will be created. If already exists, it
	 * will be overwritten so make sure to load the configuration from it first.
	 *
	 * This method will save using the system default encoding, or possibly
	 * using UTF8.
	 */
	public final void save() {
		ValidCore.checkNotNull(this.file, "Cannot save to a null file, call load() or setFile() first in " + this);

		try {
			if (this.canSave()) {
				final File parent = this.file.getCanonicalFile().getParentFile();

				if (parent != null)
					parent.mkdirs();

				// Call the main save method
				this.onSave();

				final String data = this.saveToString();

				try (Writer writer = new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8)) {
					writer.write(data);
				}
			}

		} catch (final IOException ex) {
			CommonCore.error(ex, "Error saving " + this.file);
		}
	}

	/**
	 * Override this to prevent saving the configuration
	 *
	 * @return
	 */
	protected boolean canSave() {
		return true;
	}

	/**
	 * Called before the configuration is saved after canSave() is checked
	 */
	protected void onSave() {
		for (final Map.Entry<String, Object> entry : this.saveToMap().entrySet())
			this.set(entry.getKey(), entry.getValue());
	}

	/**
	 * If you prefer not using onSave(), you can return a map of keys to be saved here
	 *
	 * @return
	 */
	protected SerializedMap saveToMap() {
		return new SerializedMap();
	}

	/**
	 * Saves this {@link FileConfig} to a string, and returns it.
	 *
	 * @return String containing this configuration.
	 */
	protected abstract String saveToString();

	// ------------------------------------------------------------------------------------------------------------
	// Manipulating and checking if data exists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for setting a value and saving the configuration.
	 *
	 * @see #set(String, Object)
	 * @see #save()
	 *
	 * @param path
	 * @param value
	 */
	public final void save(String path, Object value) {
		this.set(path, value);
		this.save();
	}

	/**
	 * Shortcut for moving a value.
	 *
	 * @param fromRel from relative path, path prefix is added
	 * @param toAbs to absolute path, path prefix is not added
	 */
	public final void move(String fromRel, String toAbs) {

		// Get the old value
		final Object oldValue = this.getObject(fromRel);

		// Set the new key
		this.set(toAbs, oldValue);

		// Remove old key
		this.set(fromRel, null);

		CommonCore.log("&7Update " + this.getFile().getName() + ". Move &b'&f" + this.buildPathPrefix(fromRel) + "&b' &7to " + "&b'&f" + toAbs + "&b'" + "&r");
	}

	/**
	 * Sets the specified key=value pair. The value is serialized, see {@link SerializeUtilCore}.
	 * The path prefix is added.
	 *
	 * @see #setPathPrefix(String)
	 * @see SerializeUtilCore#serialize(Language, Object)
	 *
	 * @param path
	 * @param value
	 */
	public final void set(String path, Object value) {
		path = this.buildPathPrefix(path);

		this.store(path, value);
	}

	/**
	 * Returns true if the given path exists.
	 * The path prefix is added.
	 *
	 * @see #setPathPrefix(String)
	 *
	 * @param path
	 * @return
	 */
	public final boolean isSet(String path) {
		path = this.buildPathPrefix(path);

		return this.isStored(path);
	}

	/**
	 * Returns true if the given path exists in the default config and default config is set.
	 * The path prefix is added.
	 *
	 * @see #setPathPrefix(String)
	 *
	 * @param path
	 * @return
	 */
	public final boolean isSetDefault(String path) {
		path = this.buildPathPrefix(path);

		return this.hasDefaults() && this.defaults.isStored(path);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Retrieving data - main
	// ------------------------------------------------------------------------------------------------------------

	public final Object getObject(String path) {
		return this.get(path, Object.class);
	}

	public final <T> T get(String path, Class<T> clazz) {
		path = this.buildPathPrefix(path);

		final Object object = this.retrieve(path);

		if (object == null) {

			// Copy over from defaults if set
			if (this.hasDefaults()) {
				final T defValue = this.defaults.get(path, clazz);

				CommonCore.log("&7Updating " + this.getFile().getName() + " at &b'&f" + path + "&b' &7-> " + "&b'&f" + defValue.toString().replace("\n", ", ") + "&b'");

				this.store(path, defValue);
				return defValue;
			}

			return null;
		}

		if (clazz.isInstance(object))
			return clazz.cast(object);

		return SerializeUtilCore.deserialize(Language.YAML, clazz, object);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Retrieving data - primitives
	// ------------------------------------------------------------------------------------------------------------

	public final Boolean getBoolean(String path) {
		return this.getBoolean(path, null);
	}

	public final Boolean getBoolean(String path, Boolean def) {
		final Boolean val = this.get(path, Boolean.class);

		return val != null ? val : def;
	}

	public final Double getDouble(String path) {
		return this.getDouble(path, null);
	}

	public final Double getDouble(String path, Double def) {
		final Double val = this.get(path, Double.class);

		return val != null ? val : def;
	}

	public final Integer getInteger(String path) {
		return this.getInteger(path, null);
	}

	public final Integer getInteger(String path, Integer def) {
		final Integer val = this.get(path, Integer.class);

		return val != null ? val : def;
	}

	public final Long getLong(String path) {
		return this.getLong(path, null);
	}

	public final Long getLong(String path, Long def) {
		final Long val = this.get(path, Long.class);

		return val != null ? val : def;
	}

	public final String getString(String path) {
		return this.getString(path, null);
	}

	/**
	 * Return a String value from the key at the given path, or supply with default
	 *
	 * This will work even if the key is a list that only has one value, or a number or boolean.
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final String getString(String path, String def) {
		final Object object = this.getObject(path);

		if (object == null)
			return def;

		else if (object instanceof List)
			return CommonCore.join((List<?>) object, "\n");

		else if (object instanceof String[])
			return CommonCore.join(Arrays.asList((String[]) object), "\n");

		else if (object.getClass().isArray())
			return CommonCore.join((Object[]) object);

		else if (ValidCore.isPrimitiveWrapper(object) || object instanceof Number)
			return String.valueOf(object);

		else if (object instanceof String)
			return (String) object;

		throw new FoException("Excepted String at '" + path + "' in, got (" + object.getClass() + "): " + object);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Retrieving data - specials
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return an tuple from the key at the given path
	 *
	 * This is stored as a map that has two sub-keys, one for the first value, second for the latter
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public final <K, V> Tuple<K, V> getTuple(final String key, Class<K> keyType, Class<V> valueType) {
		return this.getTuple(key, null, keyType, valueType);
	}

	/**
	 * Return an tuple from the key at the given path, or supply with default
	 *
	 * This is stored as a map that has two sub-keys, one for the first value, second for the latter
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param def
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public final <K, V> Tuple<K, V> getTuple(final String key, final Tuple<K, V> def, Class<K> keyType, Class<V> valueType) {
		final Object object = this.getObject(key);

		return object != null ? Tuple.deserialize(SerializedMap.of(object), keyType, valueType) : def;
	}

	/**
	 *
	 * @param path
	 * @return
	 */
	public final CaseNumberFormat getCaseNumberFormat(String path) {
		return this.getCaseNumberFormat(path, null);
	}

	/**
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final CaseNumberFormat getCaseNumberFormat(String path, String def) {
		final String raw = this.getString(path, def);

		return raw == null ? null : CaseNumberFormat.fromString(raw);
	}

	public final ZoneId getTimezone(String path) {
		return this.getTimezone(path, null);
	}

	public final ZoneId getTimezone(String path, ZoneId def) {
		final String raw = this.getString(path);

		try {
			return raw != null && !"".equals(raw) ? java.time.ZoneId.of(raw) : def;

		} catch (final Throwable t) {
			throw new IllegalArgumentException("Path '" + this.buildPathPrefix(path) + "' in " + this.getFile() + " contains invalid timezone '" + raw + "'! Valid syntax: https://garygregory.wordpress.com/2013/06/18/what-are-the-java-timezone-ids");
		}
	}

	/**
	 * Return a {@link SimpleComponent} value from the key at the given path.
	 *
	 * @param path
	 * @return
	 */
	public final SimpleComponent getComponent(final String path) {
		return this.getComponent(path, null);
	}

	/**
	 * Return a {@link SimpleComponent} value from the key at the given path
	 * or supply with default if path is not set.
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final SimpleComponent getComponent(final String path, final SimpleComponent def) {
		final String string = this.getString(path);

		return string != null ? SimpleComponent.fromMini(string) : def;
	}

	/**
	 * Return a time from the key at the given path
	 *
	 * @param path
	 * @return
	 */
	public final SimpleTime getTime(final String path) {
		return this.getTime(path, null);
	}

	/**
	 * Return a time from the key at the given path, or supply with default
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final SimpleTime getTime(final String path, final SimpleTime def) {
		final SimpleTime time = this.get(path, SimpleTime.class);

		return time != null ? time : def;
	}

	/**
	 * Return a double percentage from the key at the given path
	 *
	 * This is stored as a string such as 85%
	 *
	 * @param path
	 * @return
	 */
	public final Double getPercentage(String path) {
		return this.getPercentage(path, null);
	}

	/**
	 * Return a double percentage from the key at the given path, or supply with default
	 *
	 * This is stored as a string such as 85%
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Double getPercentage(String path, Double def) {
		final Object object = this.getObject(path);

		if (object != null) {
			final String raw = object.toString();
			ValidCore.checkBoolean(raw.endsWith("%"), "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must end with %! Got: " + raw);

			final String rawNumber = raw.substring(0, raw.length() - 1);
			ValidCore.checkInteger(rawNumber, "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must be a whole number! Got: " + raw);

			return Integer.parseInt(rawNumber) / 100D;
		}

		return def;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Retrieving data - lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a set of the given type from the key at the given path
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @param deserializeParameters
	 * @return
	 */
	public final <T> Set<T> getSet(final String key, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = this.getList(key, type);

		return list == null ? new HashSet<>() : new HashSet<>(list);
	}

	/**
	 * Return a special {@link IsInList} list from the key at the given path
	 *
	 * It is a list used to check if a value is in it, it can contain ["*"] to match all.
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	public final <T> IsInList<T> getIsInList(String path, Class<T> type) {
		final List<String> stringList = this.getStringList(path);

		if (stringList.size() == 1 && "*".equals(stringList.get(0)))
			return IsInList.fromStar();

		return IsInList.fromList(this.getList(path, type));
	}

	public final List<String> getCommandList(final String path) {
		final List<String> list = this.getStringList(path);
		ValidCore.checkBoolean(!list.isEmpty(), "Please set at least one command alias in '" + path + "' (" + this.getFile() + ") for this will be used as your main command!");

		for (int i = 0; i < list.size(); i++) {
			String command = list.get(i);

			command = command.startsWith("/") ? command.substring(1) : command;
			list.set(i, command);
		}

		return list;
	}

	public final List<String> getStringList(String path) {
		final List<?> list = this.getList(path);

		if (list == null)
			return new ArrayList<>(0);

		final List<String> result = new ArrayList<>();

		for (final Object object : list)
			if ((object instanceof String) || (ValidCore.isPrimitiveWrapper(object)))
				result.add(String.valueOf(object));

		return result;
	}

	/**
	 * Return a list of tuples with the given key-value
	 *
	 * @param <K>
	 * @param <V>
	 * @param path
	 * @param tupleKey
	 * @param tupleValue
	 * @return
	 */
	public final <K, V> List<Tuple<K, V>> getTupleList(final String path, final Class<K> tupleKey, final Class<V> tupleValue) {
		final List<Tuple<K, V>> tuples = new ArrayList<>();
		final List<Object> list = this.getList(path);

		if (list != null)
			for (final Object object : list)
				if (object == null)
					tuples.add(null);
				else {
					final Tuple<K, V> tuple = Tuple.deserialize(SerializedMap.of(object), tupleKey, tupleValue);

					tuples.add(tuple);
				}

		return tuples;
	}

	/**
	 * Return a list of maps\<string, object\> list from the key at the given path
	 *
	 * @param path
	 * @return
	 */
	public final List<SerializedMap> getMapList(final String path) {
		return this.getList(path, SerializedMap.class);
	}

	/**
	 * Return a list of a map of the given types from the key at the given path
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
		final LinkedHashMap<Key, List<Value>> map = new LinkedHashMap<>();
		final Object section = this.getObject(path);

		// Load key-value pairs from config to our map
		if (section != null)
			for (final Map.Entry<String, Object> entry : SerializedMap.of(section).entrySet()) {
				final Key key = SerializeUtilCore.deserialize(Language.YAML, keyType, entry.getKey());
				final List<Value> value = SerializeUtilCore.deserialize(Language.YAML, List.class, entry.getValue(), setDeserializeParameters);

				// Ensure the pair values are valid for the given parameters
				this.checkAssignable(path, key, keyType);

				if (!value.isEmpty())
					for (final Value item : value)
						this.checkAssignable(path, item, setType);

				map.put(key, value);
			}

		return map;
	}

	/**
	 * Return a list of the given type from the key at the given path
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param deserializeParameters
	 * @return
	 */
	public final <T> List<T> getList(final String path, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = new ArrayList<>();
		final List<Object> objects = this.getList(path);

		if (type == Map.class && deserializeParameters != null & deserializeParameters.length > 0 && deserializeParameters[0] != String.class)
			throw new FoException("getList('" + path + "') that returns Map must have String.class as key, not " + deserializeParameters[0]);

		if (objects != null)
			for (Object object : objects) {
				object = object != null ? SerializeUtilCore.deserialize(Language.YAML, type, object, deserializeParameters) : null;

				if (object != null)
					list.add((T) object);

				else if (!type.isPrimitive() && type != String.class)
					list.add(null);
			}

		return list;
	}

	public final List<Object> getList(String path) {
		return this.getList(path, null);
	}

	public final List<Object> getList(final String path, List<Object> def) {
		Object obj = this.getObject(path);

		if (obj == null)
			return def != null ? def : new ArrayList<>();

		if (obj instanceof String)
			obj = Arrays.asList(((String) obj).split("\n"));

		else if (ValidCore.isPrimitiveWrapper(obj))
			obj = Arrays.asList(obj.toString());

		ValidCore.checkBoolean(obj instanceof Collection, "Expected a list at " + path + " in " + this.file + ", got " + obj.getClass().getSimpleName() + " instead!");
		return new ArrayList<>((Collection<?>) obj);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Retrieving data - maps
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a map\<string, object\> from the key at the given path
	 *
	 * @param path
	 * @return
	 */
	public final SerializedMap getMap(final String path) {
		final Object object = this.getObject(path);

		return object != null ? SerializedMap.of(object) : new SerializedMap();
	}

	/**
	 * Return a map of the given key and value types from the key at the given path
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param valueType
	 * @param valueDeserializeParams
	 * @return
	 */
	public final <Key, Value> Map<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType, Object... valueDeserializeParams) {
		final Map<Key, Value> map = new LinkedHashMap<>();
		final Object savedKeys = this.getObject(path);

		if (savedKeys != null)
			for (final Map.Entry<String, Object> entry : SerializedMap.of(savedKeys)) {
				final Key key = SerializeUtilCore.deserialize(Language.YAML, keyType, entry.getKey());
				final Value value = SerializeUtilCore.deserialize(Language.YAML, valueType, entry.getValue(), valueDeserializeParams);

				this.checkAssignable(path, key, keyType);
				this.checkAssignable(path, value, valueType);

				map.put(key, value);
			}

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Path prefix
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Set the path prefix for this configuration
	 *
	 * @return
	 */
	public final String getPathPrefix() {
		return pathPrefix;
	}

	/**
	 * Set the path prefix for this configuration
	 *
	 * @param pathPrefix
	 */
	public final void setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	/*
	 * Helper method to add path prefix
	 */
	private final String buildPathPrefix(@NonNull final String path) {
		final String prefixed = this.pathPrefix != null ? this.pathPrefix + (!path.isEmpty() ? "." + path : "") : path;
		final String newPath = prefixed.endsWith(".") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;

		// Check for a case where there is multiple dots at the end... #somePeople
		ValidCore.checkBoolean(!newPath.endsWith("."), "Path '" + path + "' must not end with '.' after path prefix '" + this.pathPrefix + "': " + newPath);
		return newPath;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Defaults
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Checks if this configuration has a source for default values.
	 *
	 * @return
	 */
	public final boolean hasDefaults() {
		return this.defaults != null;
	}

	/**
	 * Gets the source configuration for this configuration.
	 * <p>
	 * If no configuration source was set, but default values were added, then
	 * a {@link ConfigSection} will be returned. If no source was set
	 * and no defaults were set, then this method will return null.
	 *
	 * @return Configuration source for default values, or null if none exist.
	 */
	public final FileConfig getDefaults() {
		return this.defaults;
	}

	/**
	 * Sets the source of all default values for this configuration.
	 * <p>
	 * If a previous source was set, or previous default values were defined,
	 * then they will not be copied to the new source.
	 *
	 * @param defaults New source of default values for this configuration.
	 * @throws IllegalArgumentException Thrown if defaults is null or this.
	 */
	public final void setDefaults(@NonNull FileConfig defaults) {
		this.defaults = defaults;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get the file this configuration is stored in.
	 *
	 * @return
	 */
	public final File getFile() {
		return file;
	}

	protected final void setFile(File file) {
		this.file = file;
	}

	/**
	 * Return the file name without the extension
	 *
	 * @return
	 */
	public String getName() {
		ValidCore.checkNotNull(this.file, "Cannot call FileConfig#getName() before loading the file!");

		final String fileName = this.file.getName();

		if (fileName != null) {
			final int lastDot = fileName.lastIndexOf(".");

			if (lastDot != -1)
				return fileName.substring(0, lastDot);
		}

		return null;
	}

	@Override
	final ConfigSection getParent() {
		return null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * Attempts to force a certain class type for the given object, used to prevent mistakes
	 * such as putting "Enabled: truee" (which is a String) instead of "Enabled: true" (which is a Boolean)
	 */
	private void checkAssignable(final String path, final Object object, final Class<?> type) {
		if (!type.isAssignableFrom(object.getClass()) && !type.getSimpleName().equals(object.getClass().getSimpleName())) {

			// Exceptions
			if (ConfigSerializable.class.isAssignableFrom(type) && object instanceof ConfigSection)
				return;

			throw new FoException("Malformed configuration! Key '" + path + "' in " + this.getFile() + " must be " + type.getSimpleName() + " but got " + object.getClass().getSimpleName() + ": '" + object + "'");
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FileConfig) {
			final FileConfig other = (FileConfig) obj;

			if (other.file == null && this.file == null)
				return super.equals(obj);

			if (other.file == null && this.file != null)
				return false;

			if (other.file != null && this.file == null)
				return false;

			return other.file != null && other.file.equals(this.file);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return this.file == null ? super.hashCode() : this.file.hashCode();
	}

	@Override
	public String toString() {
		return "FileConfiguration{file=" + file + "}";
	}
}
