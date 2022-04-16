package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.jsonsimple.JSONObject;
import org.mineacademy.fo.jsonsimple.JSONParser;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.ConfigSection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import lombok.NonNull;

/**
 * Serialized map enables you to save and retain values from your
 * configuration easily, such as locations, other maps or lists and
 * much more.
 */
public final class SerializedMap extends StrictCollection implements Iterable<Map.Entry<String, Object>> {

	/**
	 * The Google Json instance
	 */
	private final static Gson gson;

	static {
		// Fix Google complicating things and breaking long formatting
		final GsonBuilder gsonBuilder = new GsonBuilder();

		gsonBuilder.setLongSerializationPolicy(LongSerializationPolicy.STRING);

		gson = gsonBuilder.create();
	}

	/**
	 * A fallback Json parser
	 */
	private final static JSONParser jsonSimple = new JSONParser();

	/**
	 * The internal map with values
	 */
	private final StrictMap<String, Object> map = new StrictMap<>();

	/**
	 * Should we remove entries on get for this map instance,
	 */
	private boolean removeOnGet = false;

	/**
	 * Creates a new serialized map with the given first key-value pair
	 *
	 * @param key
	 * @param value
	 */
	private SerializedMap(final String key, final Object value) {
		this();

		put(key, value);
	}

	public SerializedMap() {
		super("Cannot remove '%s' as it is not in the map!", "Value '%s' is already in the map!");
	}

	/**
	 * Put key-value pairs from another map into this map
	 * <p>
	 * If the key already exist, it is ignored
	 *
	 * @param anotherMap
	 */
	public SerializedMap mergeFrom(final SerializedMap anotherMap) {
		for (final Map.Entry<String, Object> entry : anotherMap.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			if (key != null && value != null && !this.map.containsKey(key))
				this.map.put(key, value);
		}

		return this;
	}

	/**
	 * @see Map#containsKey(Object)
	 *
	 * @param key
	 * @return
	 */
	public boolean containsKey(final String key) {
		return map.containsKey(key);
	}

	/**
	 * Puts a key:value pair into the map only if the values are not null
	 *
	 * @param associativeArray
	 * @return
	 */
	public SerializedMap putArray(final Object... associativeArray) {
		boolean nextIsString = true;
		String lastKey = null;

		for (final Object obj : associativeArray) {
			if (nextIsString) {
				Valid.checkBoolean(obj instanceof String, "Expected String, got " + obj.getClass().getSimpleName() + ": " + SerializeUtil.serialize(obj));

				lastKey = (String) obj;

			} else
				map.override(lastKey, obj);

			nextIsString = !nextIsString;
		}

		return this;
	}

	/**
	 * Add another map to this map
	 *
	 * @param anotherMap
	 * @return this
	 */
	public SerializedMap put(@NonNull SerializedMap anotherMap) {
		map.putAll(anotherMap.asMap());

		return this;
	}

	/**
	 * Puts the key-value pair into the map if the value is true
	 *
	 * @param key
	 * @param value
	 */
	public void putIfTrue(final String key, final boolean value) {
		if (value)
			put(key, value);
	}

	/**
	 * Puts the key-value pair into the map if the value is not null
	 *
	 * @param key
	 * @param value
	 */
	public void putIfExist(final String key, final Object value) {
		if (value != null)
			put(key, value);
	}

	/**
	 * Puts the map into this map if not null and not empty
	 *
	 * This will put a NULL value into the map if the value is null
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final Map<?, ?> value) {
		if (value != null && !value.isEmpty())
			put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			map.getSource().put(key, null);
	}

	/**
	 * Puts the collection into map if not null and not empty
	 *
	 * This will put a NULL value into the map if the value is null
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final Collection<?> value) {
		if (value != null && !value.isEmpty())
			put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			map.getSource().put(key, null);
	}

	/**
	 * Puts the boolean into map if true
	 *
	 * This will put a NULL value into the map if the value is null
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final boolean value) {
		if (value)
			put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			map.getSource().put(key, null);
	}

	/**
	 * Puts the value into map if not null
	 *
	 * This will put a NULL value into the map if the value is null
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final Object value) {
		if (value != null)
			put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			map.getSource().put(key, null);
	}

	/**
	 * Puts a new key-value pair in the map, failing if the value is null
	 * or if the old key exists
	 *
	 * @param key
	 * @param value
	 */
	public void put(final String key, final Object value) {
		Valid.checkNotNull(value, "Value with key '" + key + "' is null!");

		map.put(key, value);
	}

	/**
	 * Puts a new key-value pair in the map, failing if key is null
	 * and replacing the old key if exist
	 *
	 * @param key
	 * @param value
	 */
	public void override(final String key, final Object value) {
		//Valid.checkNotNull(value, "Cannot put null values into SerializedMap! Value with key '" + key + "' is null!");

		map.override(key, value);
	}

	/**
	 * Overrides all map values
	 *
	 * @param map
	 */
	public void overrideAll(SerializedMap map) {
		map.forEach(this::override);
	}

	/**
	 * Remove the given key, returning null if not set
	 *
	 * @param key
	 * @return
	 */
	public Object removeWeak(final String key) {
		return map.removeWeak(key);
	}

	/**
	 * Remove the given key, throwing error if not set
	 *
	 * @param key
	 * @return
	 */
	public Object remove(final String key) {
		return map.remove(key);
	}

	/**
	 * Remove a given key by value
	 *
	 * @param value
	 */
	public void removeByValue(final Object value) {
		map.removeByValue(value);
	}

	/**
	 * Returns a string from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public String getString(final String key) {
		return getString(key, null);
	}

	/**
	 * Returns a string from the map, with an optional default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public String getString(final String key, final String def) {
		return get(key, String.class, def);
	}

	/**
	 * Returns a UUID from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public UUID getUUID(final String key) {
		return getUUID(key, null);
	}

	/**
	 * Returns a UUID from the map, with an optional default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public UUID getUUID(final String key, final UUID def) {
		return get(key, UUID.class, def);
	}

	/**
	 * Returns a location from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Location getLocation(final String key) {
		return get(key, org.bukkit.Location.class, null);
	}

	/**
	 * Returns a long from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Long getLong(final String key) {
		return getLong(key, null);
	}

	/**
	 * Return the long value or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Long getLong(final String key, final Long def) {
		final Number n = get(key, Long.class, def);

		return n != null ? n.longValue() : null;
	}

	/**
	 * Returns an integer from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Integer getInteger(final String key) {
		return getInteger(key, null);
	}

	/**
	 * Return the integer key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Integer getInteger(final String key, final Integer def) {
		return get(key, Integer.class, def);
	}

	/**
	 * Returns a double from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Double getDouble(final String key) {
		return getDouble(key, null);
	}

	/**
	 * Return the double key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Double getDouble(final String key, final Double def) {
		return get(key, Double.class, def);
	}

	/**
	 * Returns a float from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Float getFloat(final String key) {
		return getFloat(key, null);
	}

	/**
	 * Return the float key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Float getFloat(final String key, final Float def) {
		return get(key, Float.class, def);
	}

	/**
	 * Returns a boolean from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Boolean getBoolean(final String key) {
		return getBoolean(key, null);
	}

	/**
	 * Return the boolean key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Boolean getBoolean(final String key, final Boolean def) {
		return get(key, Boolean.class, def);
	}

	/**
	 * Returns a material from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public CompMaterial getMaterial(final String key) {
		return getMaterial(key, null);
	}

	/**
	 * Return a material from the map or the default given
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public CompMaterial getMaterial(final String key, final CompMaterial def) {
		final String raw = getString(key);

		return raw != null ? CompMaterial.fromString(raw) : def;
	}

	/**
	 * Returns an itemstack from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public ItemStack getItemStack(final String key) {
		return getItem(key, null);
	}

	/**
	 * Return an itemstack at the key position or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public ItemStack getItem(final String key, final ItemStack def) {
		final Object obj = get(key, Object.class, null);

		if (obj == null)
			return def;

		return SerializeUtil.deserialize(ItemStack.class, obj);
	}

	/**
	 * Return a tuple
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public <K, V> Tuple<K, V> getTuple(final String key, Class<K> keyType, Class<V> valueType) {
		return getTuple(key, null, keyType, valueType);
	}

	/**
	 * Return a tuple or default
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param def
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public <K, V> Tuple<K, V> getTuple(final String key, final Tuple<K, V> def, Class<K> keyType, Class<V> valueType) {
		return get(key, Tuple.class, def, keyType, valueType);
	}

	/**
	 * Returns a string list from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public List<String> getStringList(final String key) {
		return getStringList(key, null);
	}

	/**
	 * Return string list or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public List<String> getStringList(final String key, final List<String> def) {
		final List<String> list = getList(key, String.class);

		return list == null ? def : list;
	}

	/**
	 * Return a list of serialized maps or null if not set
	 *
	 * @param key
	 * @return
	 */
	public List<SerializedMap> getMapList(final String key) {
		return getList(key, SerializedMap.class);
	}

	/**
	 * Return a set from the map, or an empty set if the map does not
	 * contain the given key.
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 * @see #getList(String, Class)
	 */
	public <T> Set<T> getSet(final String key, final Class<T> type) {
		final List<T> list = getList(key, type);

		return new HashSet<>(list);
	}

	/**
	 * Return {@link IsInList} implementation, of a list that is always
	 * returning true, if the given key equals to ["*"]
	 *
	 * @param path
	 * @param type
	 * @return
	 */
	public <T> IsInList<T> getIsInList(String path, Class<T> type) {
		final List<String> stringList = getStringList(path);

		if (stringList.size() == 1 && "*".equals(stringList.get(0)))
			return IsInList.fromStar();

		return IsInList.fromList(getList(path, type));
	}

	/**
	 * Return a list of objects of the given type, or empty list if map does not contains key.
	 * <p>
	 * If the type is your own class make sure to put public static deserialize(SerializedMap)
	 * method into it that returns the class object from the map!
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> List<T> getList(final String key, final Class<T> type) {
		final List<T> list = new ArrayList<>();

		if (!map.containsKey(key))
			return list;

		final Object rawList = Remain.getRootOfSectionPathData(this.removeOnGet ? map.removeWeak(key) : map.get(key));

		// Forgive if string used instead of string list
		if (type == String.class && rawList instanceof String) {
			list.add((T) rawList);

		} else {
			Valid.checkBoolean(rawList instanceof Collection<?>, "Key '" + key + "' expected to have a list, got " + rawList.getClass().getSimpleName() + " instead! Try putting '' quotes around the message: " + rawList);

			for (final Object object : (Collection<Object>) rawList)
				list.add(object == null ? null : SerializeUtil.deserialize(type, object));
		}

		return list;
	}

	/**
	 * Returns a serialized map (String-Object pairs) from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public SerializedMap getMap(final String key) {
		final Object raw = get(key, Object.class);

		return raw != null ? SerializedMap.of(raw) : new SerializedMap();
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
	 * @return
	 */
	public <Key, Value> LinkedHashMap<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType) {
		// The map we are creating, preserve order
		final LinkedHashMap<Key, Value> map = new LinkedHashMap<>();
		final Object raw = this.map.get(path);

		if (raw != null)
			for (final Entry<?, ?> entry : SerializedMap.of(raw).entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final Value value = SerializeUtil.deserialize(valueType, entry.getValue());

				// Ensure the pair values are valid for the given paramenters
				checkAssignable(path, key, keyType);
				checkAssignable(path, value, valueType);

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
	public <Key, Value> LinkedHashMap<Key, Set<Value>> getMapSet(@NonNull String path, final Class<Key> keyType, final Class<Value> setType) {
		// The map we are creating, preserve order
		final LinkedHashMap<Key, Set<Value>> map = new LinkedHashMap<>();
		Object raw = this.map.get(path);

		if (raw != null) {
			raw = SerializedMap.of(raw);

			for (final Entry<String, Object> entry : ((SerializedMap) raw).entrySet()) {
				final Key key = SerializeUtil.deserialize(keyType, entry.getKey());
				final List<Value> value = SerializeUtil.deserialize(List.class, entry.getValue());

				// Ensure the pair values are valid for the given paramenters
				checkAssignable(path, key, keyType);

				if (!value.isEmpty())
					for (final Value item : value)
						checkAssignable(path, item, setType);

				map.put(key, new HashSet<>(value));
			}
		}

		return map;
	}

	/*
	 * Checks if the clazz parameter can be assigned to the given value
	 */
	private void checkAssignable(final String path, final Object value, final Class<?> clazz) {
		if (!clazz.isAssignableFrom(value.getClass()) && !clazz.getSimpleName().equals(value.getClass().getSimpleName()))
			throw new FoException("Malformed map! Key '" + path + "' in the map must be " + clazz.getSimpleName() + " but got " + value.getClass().getSimpleName() + ": '" + value + "'");
	}

	/**
	 * Return an object at the given location
	 *
	 * @param key
	 * @return
	 */
	public Object getObject(final String key) {
		return get(key, Object.class);
	}

	/**
	 * Return an object at the given location, or default if it does not exist
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Object getObject(final String key, final Object def) {
		return get(key, Object.class, def);
	}

	/**
	 * Returns a key and attempts to deserialize it as the given type
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> T get(final String key, final Class<T> type) {
		return get(key, type, null);
	}

	/**
	 * Returns the key and attempts to deserialize it as the given type, with a default value
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @param def
	 * @param deserializeParameters
	 * @return
	 */
	public <T> T get(final String key, final Class<T> type, final T def, Object... deserializeParameters) {
		Object raw = removeOnGet ? map.removeWeak(key) : map.get(key);

		// Try to get the value by key with ignoring case
		if (raw == null)
			raw = getValueIgnoreCase(key);

		// Assume empty means default for enumerations
		if ("".equals(raw) && Enum.class.isAssignableFrom(type))
			return def;

		return raw == null ? def : SerializeUtil.deserialize(type, raw, deserializeParameters);
	}

	/**
	 * Looks up a value by the string key, case ignored
	 *
	 * @param key
	 * @return
	 */
	public Object getValueIgnoreCase(final String key) {
		for (final Entry<String, Object> entry : map.entrySet())
			if (entry.getKey().equalsIgnoreCase(key))
				return entry.getValue();

		return null;
	}

	/**
	 * @see Map#forEach(BiConsumer)
	 *
	 * @param consumer
	 */
	public void forEach(final BiConsumer<String, Object> consumer) {
		for (final Entry<String, Object> e : map.entrySet())
			consumer.accept(e.getKey(), e.getValue());
	}

	/**
	 * Return the first entry or null if map is empty
	 *
	 * @return
	 */
	public Map.Entry<String, Object> firstEntry() {
		return isEmpty() ? null : map.getSource().entrySet().iterator().next();
	}

	/**
	 * @see Map#keySet()
	 *
	 * @return
	 */
	public Set<String> keySet() {
		return map.keySet();
	}

	/**
	 * @see Map#values()
	 *
	 * @return
	 */
	public Collection<Object> values() {
		return map.values();
	}

	/**
	 * @see Map#entrySet()
	 *
	 * @return
	 */
	public Set<Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	/**
	 * @see Map#size()
	 *
	 * @return
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Get the Java map representation
	 *
	 * @return
	 */
	public Map<String, Object> asMap() {
		return map.getSource();
	}

	/**
	 * Convert this map into a serialized one (again, but iterating through each pair as well)
	 */
	@Override
	public Object serialize() {
		return map.serialize();
	}

	/**
	 * Converts this map into a JSON string
	 *
	 * @return
	 */
	public String toJson() {
		final Object map = serialize();

		try {
			return gson.toJson(map);

		} catch (final Throwable t) {
			Common.error(t, "Failed to serialize to json, data: " + map);

			return "{}";
		}
	}

	/**
	 * @see Map#isEmpty()
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Automatically convert a section within this map from one type to another
	 *
	 * @param <O>
	 * @param <N>
	 * @param path
	 * @param from
	 * @param to
	 * @param converter
	 */
	public <O, N> void convert(final String path, final Class<O> from, final Class<N> to, final Function<O, N> converter) {
		final Object old = getObject(path);

		if (old != null)
			// If the old is a collection check if the first value is old, assume the rest is old as well
			if (old instanceof Collection) {
				final Collection<?> collection = (Collection<?>) old;

				if (collection.isEmpty() || !from.isAssignableFrom(collection.iterator().next().getClass()))
					return;

				final List<N> newCollection = new ArrayList<>();

				for (final O oldItem : (Collection<O>) collection)
					newCollection.add(converter.apply(oldItem));

				override(path, newCollection);

				Common.logNoPrefix("[" + SimplePlugin.getNamed() + "] Converted '" + path + "' from " + from.getSimpleName() + "[] to " + to.getSimpleName() + "[]");

			} else if (from.isAssignableFrom(old.getClass())) {
				override(path, converter.apply((O) old));

				Common.logNoPrefix("[" + SimplePlugin.getNamed() + "] Converted '" + path + "' from '" + from.getSimpleName() + "' to '" + to.getSimpleName() + "'");
			}
	}

	/**
	 * Convert the key pairs into formatted string such as {
	 * 	"key" = "value"
	 *  "another" = "value2"
	 *  ...
	 * }
	 *
	 * @return
	 */
	public String toStringFormatted() {
		final Map<?, ?> map = (Map<?, ?>) serialize();
		final List<String> lines = new ArrayList<>();

		lines.add("{");

		for (final Map.Entry<?, ?> entry : map.entrySet()) {
			final Object value = entry.getValue();

			if (value != null && !value.toString().equals("[]") && !value.toString().equals("{}") && !value.toString().isEmpty() && !value.toString().equals("0.0") && !value.toString().equals("false"))
				lines.add("\t'" + entry.getKey() + "' = '" + entry.getValue() + "'");
		}

		lines.add("}");

		return String.join("\n", lines);
	}

	/**
	 * @param removeOnGet the removeOnGet to set
	 */
	public void setRemoveOnGet(boolean removeOnGet) {
		this.removeOnGet = removeOnGet;
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return this.map.entrySet().iterator();
	}

	@Override
	public String toString() {
		return serialize().toString();
	}

	// ----------------------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Create a new map with the first key-value pair
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public static SerializedMap of(final String key, final Object value) {
		return new SerializedMap(key, value);
	}

	/**
	 * Create new serialized map from key-value pairs like you would in PHP:
	 * <p>
	 * array(
	 * "name" => value,
	 * "name2" => value2,
	 * )
	 * <p>
	 * Except now you just use commas instead of =>'s
	 *
	 * @param array
	 * @return
	 */
	public static SerializedMap ofArray(final Object... array) {

		// If the first argument is a map already, treat as such
		if (array != null && array.length == 1) {
			final Object firstArgument = array[0];

			if (firstArgument instanceof SerializedMap)
				return (SerializedMap) firstArgument;

			if (firstArgument instanceof Map)
				return SerializedMap.of(firstArgument);

			if (firstArgument instanceof StrictMap)
				return SerializedMap.of(((StrictMap<String, Object>) firstArgument).getSource());
		}

		final SerializedMap map = new SerializedMap();
		map.putArray(array);

		return map;
	}

	/**
	 * Parses the given object into Serialized map
	 *
	 * @param object
	 * @return the serialized map, or an empty map if object could not be parsed
	 */
	public static SerializedMap of(@NonNull Object object) {

		if (object instanceof SerializedMap)
			return (SerializedMap) object;

		if (object instanceof MemorySection)
			return of(Common.getMapFromSection(object));

		if (object instanceof ConfigSection)
			return of(((ConfigSection) object).getValues(false));

		if (object instanceof Map) {
			final Map<String, Object> copyOf = new LinkedHashMap<>();

			for (final Map.Entry<?, ?> entry : ((Map<String, Object>) object).entrySet()) {
				final Object key = entry.getKey();

				if (key == null)
					copyOf.put(null, entry.getValue());

				else {
					final String stringKey = key.toString();
					final Object value = entry.getValue();

					final String[] split = stringKey.split("\\=");

					// Spigot's special way of storing maps 'key=value'
					if (split.length == 2 && value == null) {
						final String actualKey = split[0];
						final String actualValue = split[1];

						copyOf.put(actualKey, actualValue);
					}

					else
						copyOf.put(stringKey, value);
				}
			}

			final SerializedMap serialized = new SerializedMap();
			serialized.map.putAll(copyOf);

			return serialized;
		}

		throw new FoException("SerializedMap does not know how to convert " + object.getClass().getSimpleName() + ": " + object);
	}

	/**
	 * Attempts to parse the given JSON into a serialized map
	 * <p>
	 * Values are not deserialized right away, they are converted
	 * when you call get() functions
	 *
	 * @param json
	 * @return
	 */
	public static SerializedMap fromJson(@NonNull final String json) {

		synchronized (jsonSimple) {
			if (json.isEmpty() || "[]".equals(json) || "{}".equals(json))
				return new SerializedMap();

			// Fallback to simple
			try {
				final Object parsed = jsonSimple.parse(json);

				if (parsed instanceof JSONObject)
					return SerializedMap.of(parsed);

				throw new FoException("Unable to deserialize " + (parsed != null ? parsed.getClass() : "unknown class") + " from: " + json);

			} catch (final Throwable secondThrowable) {
				Common.throwError(secondThrowable, "Failed to parse JSON from " + json);

				return null;
			}
		}
	}
}
