package org.mineacademy.fo.model;

import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;

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
	 *  Transform the given config section to tuple
	 *
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public static <K, V> Tuple<K, V> deserialize(SerializedMap map, Class<K> keyType, Class<V> valueType) {
		final K key = SerializeUtil.deserialize(keyType, map.getObject("Key"));
		final V value = SerializeUtil.deserialize(valueType, map.getObject("Value"));

		return new Tuple<>(key, value);
	}

	/**
	 * Do not use
	 *
	 * @param <K>
	 * @param <V>
	 * @param map
	 *
	 * @deprecated do not use
	 * @return
	 */
	@Deprecated
	public static <K, V> Tuple<K, V> deserialize(SerializedMap map) {
		throw new FoException("Tuple cannot be deserialized automatically, call Tuple#deserialize(map, keyType, valueType)");
	}
}
