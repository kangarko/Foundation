package org.mineacademy.fo.collection;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

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

/**
 * Serialized map enables you to save and retain values from your
 * configuration easily, such as locations, other maps or lists and
 * much more.
 */
public final class SerializedMap extends StrictCollection {

	/**
	 * The internal map with values
	 */
	private final StrictMap<String, Object> map = new StrictMap<>();

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
	 *
	 * @param key
	 * @param value
	 */
	public void put(String key, Object value) {
		Valid.checkNotNull(value, "Value with key '" + key + "' is null!");

		map.put(key, value);
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

					final Class<?> cl = ReflectionUtil.getOFCClass("inventory." + (metaMap.containsKey("spawnedType") ? "CraftMetaSpawnEgg" : "CraftMetaItem"));
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
	@SuppressWarnings("serial")
	public List<String> getStringList(String key, List<String> def) {
		final Object list = map.get(key);

		if (list == null)
			return def;

		return list instanceof List ? (List<String>) list : new ArrayList<String>() {
			{
				add(list.toString());
			}
		};
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

	@Override
	public String toString() {
		return serialize().toString();
	}

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
}
