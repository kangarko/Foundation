package org.mineacademy.fo.collection;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;

import com.google.gson.Gson;

/**
 * Serialized map enables you to save and retain values from your
 * configuration easily, such as locations, other maps or lists and
 * much more.
 */
public final class SerializedMap extends StrictCollection {

	/**
	 * The Google Json instance
	 */
	private final static Gson gson = new Gson();

	/**
	 * The internal map with values
	 */
	private final StrictMap<String, Object> map = new StrictMap<>();

	/**
	 * Creates a new serialized map with the given first key-value pair
	 *
	 * @param key
	 * @param value
	 */
	public SerializedMap(String key, Object value) {
		this();

		put(key, value);
	}

	public SerializedMap() {
		super("Cannot remove '%s' as it is not in the map!", "Value '%s' is already in the map!");
	}

	/**
	 * @see Map#containsKey(Object)
	 */
	public boolean containsKey(String key) {
		return map.contains(key);
	}

	/**
	 * Puts the key-value pair into the map if the value is not null
	 *
	 * @param key
	 * @param value
	 */
	public void putIfExist(String key, @Nullable Object value) {
		if (value != null)
			put(key, value);
	}

	/**
	 * Puts a new key-value pair in the map, failing if the value is null
	 * or if the old key exists
	 *
	 * @param key
	 * @param value
	 */
	public void put(String key, Object value) {
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
	public void override(String key, Object value) {
		Valid.checkNotNull(value, "Value with key '" + key + "' is null!");

		map.override(key, value);
	}

	/**
	 * Returns a string from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		return getString(key, null);
	}

	/**
	 * Returns a string from the map, with an optional default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public String getString(String key, String def) {
		return get(key, String.class, def);
	}

	/**
	 * Returns a location from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Location getLocation(String key) {
		return get(key, org.bukkit.Location.class, null);
	}

	/**
	 * Returns a long from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Long getLong(String key) {
		return getLong(key, null);
	}

	/**
	 * Return the long value or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Long getLong(String key, Long def) {
		final Number n = get(key, Long.class, def);

		return n != null ? n.longValue() : null;
	}

	/**
	 * Returns an integer from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Integer getInteger(String key) {
		return getInteger(key, null);
	}

	/**
	 * Return the integer key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Integer getInteger(String key, Integer def) {
		return get(key, Integer.class, def);
	}

	/**
	 * Returns a double from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Double getDouble(String key) {
		return getDouble(key, null);
	}

	/**
	 * Return the double key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Double getDouble(String key, Double def) {
		return get(key, Double.class, def);
	}

	/**
	 * Returns a float from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Float getFloat(String key) {
		return getFloat(key, null);
	}

	/**
	 * Return the float key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Float getFloat(String key, Float def) {
		return get(key, Float.class, def);
	}

	/**
	 *  Returns a boolean from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public Boolean getBoolean(String key) {
		return getBoolean(key, null);
	}

	/**
	 * Return the boolean key or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Boolean getBoolean(String key, Boolean def) {
		return get(key, Boolean.class, def);
	}

	/**
	 * Returns a material from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public CompMaterial getMaterial(String key) {
		return getMaterial(key, null);
	}

	/**
	 * Return a material from the map or the default given
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public CompMaterial getMaterial(String key, CompMaterial def) {
		final String raw = getString(key);

		return raw != null ? CompMaterial.fromString(raw) : def;
	}

	/**
	 * Returns an itemstack from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public ItemStack getItem(String key) {
		return getItem(key, null);
	}

	/**
	 * Return an itemstack at the key position or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public ItemStack getItem(String key, ItemStack def) {
		final Object obj = get(key, Object.class, null);

		if (obj == null)
			return def;

		if (obj instanceof ItemStack)
			return (ItemStack) obj;

		final Map<String, Object> map = (Map<String, Object>) obj;
		final ItemStack item = ItemStack.deserialize(map);

		final Object raw = map.get("meta");

		if (raw != null) {
			if (raw instanceof ItemMeta)
				item.setItemMeta((ItemMeta) raw);

			else if (raw instanceof Map) {
				try {
					final Map<String, Object> metaMap = (Map<String, Object>) raw;

					final Class<?> cl = ReflectionUtil.getOBCClass("inventory." + (metaMap.containsKey("spawnedType") ? "CraftMetaSpawnEgg" : "CraftMetaItem"));
					final Constructor<?> c = cl.getDeclaredConstructor(Map.class);
					c.setAccessible(true);

					final Object craftMeta = c.newInstance((Map<String, ?>) raw);

					if (craftMeta instanceof ItemMeta)
						item.setItemMeta((ItemMeta) craftMeta);

				} catch (final Throwable t) {
					t.printStackTrace();
				}
			}
		}

		return item;
	}

	/**
	 * Returns a string list from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public List<String> getStringList(String key) {
		return getStringList(key, null);
	}

	/**
	 * Return string list or default
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public List<String> getStringList(String key, List<String> def) {
		final List<String> list = getList(key, String.class);

		return list == null ? def : list;
	}

	/**
	 * Return a list of serialized maps or null if not set
	 *
	 * @param key
	 * @return
	 */
	public List<SerializedMap> getMapList(String key) {
		return getList(key, SerializedMap.class);
	}

	/**
	 * @see #getList(String, Class), except that this method
	 * never returns null, instead, if the key is not present,
	 * we return an empty list instead of null
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
	 * Return a list of objects of the given type
	 *
	 * If the type is your own class make sure to put public static deserialize(SerializedMap)
	 * method into it that returns the class object from the map!
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> List<T> getList(String key, Class<T> type) {
		if (!map.contains(key))
			return null;

		final List<Object> objects = (List<Object>) map.get(key);
		final List<T> list = new ArrayList<>();

		for (final Object object : objects)
			list.add(SerializeUtil.deserialize(type, object));

		return list;
	}

	/**
	 * Return a serialized map or an empty one if it does not exist
	 *
	 * @param key
	 * @return
	 */
	public SerializedMap getMapSafe(String key) {
		return Common.getOrDefault(getMap(key), new SerializedMap());
	}

	/**
	 * Returns a serialized map (String-Object pairs) from the map, or null if does not exist
	 *
	 * @param key
	 * @return
	 */
	public SerializedMap getMap(String key) {
		final Object raw = get(key, Object.class);

		return raw != null ? SerializedMap.of(Common.getMapFromSection(raw)) : null;
	}

	/**
	 * Return an object at the given location
	 *
	 * @param key
	 * @return
	 */
	public Object getObject(String key) {
		return get(key, Object.class);
	}

	/**
	 * Return an object at the given location, or default if it does not exist
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Object getObject(String key, Object def) {
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
	public <T> T get(String key, Class<T> type) {
		return get(key, type, null);
	}

	/**
	 *
	 * Returns the key and attempts to deserialize it as the given type, with a default value
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @param def
	 * @return
	 */
	private <T> T get(String key, Class<T> type, T def) {
		Object raw = map.get(key);

		// Try to get the value by key with ignoring case
		if (raw == null)
			raw = getValueIgnoreCase(key);

		return raw == null ? def : SerializeUtil.deserialize(type, raw, key);
	}

	/**
	 * Looks up a value by the string key, case ignored
	 *
	 * @param key
	 * @return
	 */
	public Object getValueIgnoreCase(String key) {
		for (final Entry<String, Object> e : map.entrySet())
			if (e.getKey().equalsIgnoreCase(key))
				return e.getValue();

		return null;
	}

	/**
	 * @see Map#forEach(BiConsumer)
	 */
	public void forEach(BiConsumer<String, Object> consumer) {
		for (final Entry<String, Object> e : map.entrySet())
			consumer.accept(e.getKey(), e.getValue());
	}

	/**
	 * @see Map#keySet()
	 */
	public Set<String> keySet() {
		return map.keySet();
	}

	/**
	 * @see Map#values()
	 */
	public Collection<Object> values() {
		return map.values();
	}

	/**
	 * @see Map#entrySet()
	 */
	public Set<Entry<String, Object>> entrySet() {
		return map.entrySet();
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
	 * @see Map#isEmpty()
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
	public <O, N> void convert(String path, Class<O> from, Class<N> to, Function<O, N> converter) {
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

				override(path, newCollection);

				Common.log("&7Converted '" + path + "' from " + from.getSimpleName() + "[] to " + to.getSimpleName() + "[]");

			} else if (from.isAssignableFrom(old.getClass())) {
				override(path, converter.apply((O) old));

				Common.log("&7Converted '" + path + "' from '" + from.getSimpleName() + "' to '" + to.getSimpleName() + "'");
			}
		}
	}

	/**
	 * Converts this map into a JSON string
	 *
	 * @return
	 */
	public String toJson() {
		return gson.toJson(serialize());
	}

	@Override
	public String toString() {
		return serialize().toString();
	}

	// ----------------------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Parses the given object into Serialized map
	 *
	 * @param object
	 * @return the serialized map, or an empty map if object could not be parsed
	 */
	public static SerializedMap of(Object object) {
		if (object instanceof SerializedMap)
			return (SerializedMap) object;

		if (object instanceof Map || object instanceof MemorySection)
			return of(Common.getMapFromSection(object));

		return new SerializedMap();
	}

	/**
	 * Converts the given Map into a serializable map
	 *
	 * @param map
	 * @return
	 */
	public static SerializedMap of(Map<String, Object> map) {
		final SerializedMap serialized = new SerializedMap();

		serialized.map.setAll(map);
		return serialized;
	}

	/**
	 * Attempts to parse the given JSON into a serialized map
	 *
	 * Values are not deserialized right away, they are converted
	 * when you call get() functions
	 *
	 * @param json
	 * @return
	 */
	public static SerializedMap fromJson(String json) {
		final SerializedMap serializedMap = new SerializedMap();
		final Map<String, Object> map = gson.fromJson(json, Map.class);

		serializedMap.map.putAll(map);

		return serializedMap;
	}
}
