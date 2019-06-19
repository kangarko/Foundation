package org.mineacademy.fo.collection;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

/**
 * A standard hash map with toJson and fromJson additional methods
 */
public final class JsonMap {

	/**
	 * The Google Json instance
	 */
	private final static Gson gson = new Gson();

	/**
	 * The standard map
	 */
	private final Map<String, Object> map = new HashMap<>();

	/**
	 * Return the original map
	 *
	 * @return
	 */
	public Map<String, Object> getMap() {
		return map;
	}

	/**
	 * Delegate method for {@link Map#put(Object, Object)}
	 *
	 * @param key
	 * @param value
	 */
	public void put(String key, Object value) {
		map.put(key, value);
	}

	/**
	 * Delegate method for {@link Map#getOrDefault(Object, Object)}
	 *
	 * @param key
	 * @param defValue
	 * @return
	 */
	public Object getOrDefault(String key, Object defValue) {
		return map.getOrDefault(key, defValue);
	}

	/**
	 * Delegate method for {@link Map#isEmpty()}
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Converts this map into a json string
	 *
	 * @return
	 */
	public String toJson() {
		return gson.toJson(map);
	}

	/**
	 * Creates a map from the json string
	 *
	 * @param json
	 * @return
	 */
	public static JsonMap fromJson(String json) {
		final JsonMap jsonMap = new JsonMap();
		final Map<String, Object> map = gson.fromJson(json, Map.class);

		jsonMap.map.putAll(map);

		return jsonMap;
	}
}
