package org.mineacademy.fo.model;

import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;

import lombok.Data;

/**
 * Simple tuple for key-value pairs
 * @param <K>
 * @param <V>
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
	 * Return this tuple in X - Y syntax
	 *
	 * @return
	 */
	public String toLine() {
		return key + " - " + value;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.toLine();
	}

	/**
	 * Transform the given config section to tuple
	 *
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public static <K, V> Tuple<K, V> deserialize(SerializedMap map, Class<K> keyType, Class<V> valueType) {
		final K key = map.containsKey("Key") ? SerializeUtil.deserialize(keyType, map.getObject("Key")) : null;
		final V value = map.containsKey("Value") ? SerializeUtil.deserialize(valueType, map.getObject("Value")) : null;

		return new Tuple<>(key, value);
	}

	/**
	 * Deserialize the given line (it must have the KEY - VALUE syntax) into the given tuple
	 *
	 * @param <K>
	 * @param <V>
	 * @param line
	 * @param keyType
	 * @param valueType
	 * @return tuple or null if line is null
	 */
	public static <K, V> Tuple<K, V> deserialize(String line, Class<K> keyType, Class<V> valueType) {
		if (line == null)
			return null;

		final String split[] = line.split(" - ");
		Valid.checkBoolean(split.length == 2, "Line must have the syntax <" + keyType.getSimpleName() + "> - <" + valueType.getSimpleName() + "> but got: " + line);

		final K key = SerializeUtil.deserialize(keyType, split[0]);
		final V value = SerializeUtil.deserialize(valueType, split[1]);

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
