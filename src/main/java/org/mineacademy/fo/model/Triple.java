package org.mineacademy.fo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A simple placeholder for storing three values
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
@Getter
@RequiredArgsConstructor
public class Triple<A, B, C> {

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