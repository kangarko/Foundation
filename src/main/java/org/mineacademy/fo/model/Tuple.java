package org.mineacademy.fo.model;

import lombok.Data;

/**
 * Simple tuple for key-value pairs
 */
@Data
public final class Tuple<K, V> {

	/**
	 * The key
	 */
	private final K key;

	/**
	 * The value
	 */
	private final V value;
}
