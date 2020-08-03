package org.mineacademy.fo.model;

import org.mineacademy.fo.collection.SerializedMap;

import lombok.Data;

/**
 * Simple tuple for key-value pairs
 */
@Data
public final class Tuple<K, V> implements ConfigSerializable {

	/**
	 * The key
	 */
	private final K key;

	/**
	 * The value
	 */
	private final V value;

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray("Key", key, "Value", value);
	}

	/**
	 * Transform (speculatively) config section to tuple
	 *
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @return
	 */
	public static <K, V> Tuple<K, V> deserialize(SerializedMap map) {
		final K key = (K) map.getObject("Key");
		final V value = (V) map.getObject("Value");

		return new Tuple<>(key, value);
	}
}
