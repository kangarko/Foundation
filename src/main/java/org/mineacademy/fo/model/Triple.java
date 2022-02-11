package org.mineacademy.fo.model;

import lombok.Data;

/**
 * A simple placeholder for storing three values
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
@Data
public final class Triple<A, B, C> {

	/**
	 * The first value we hold
	 */
	private final A first;

	/**
	 * The second value we hold
	 */
	private final B second;

	/**
	 * The third value we hold
	 */
	private final C third;
}