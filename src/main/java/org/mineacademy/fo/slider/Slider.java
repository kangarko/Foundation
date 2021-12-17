package org.mineacademy.fo.slider;

/**
 * Represents a slider for animating text or items.
 *
 * A slider takes in a list of items (or a string) and
 * then moves them in the precreated direction using the {@link #next()} method.
 * @param <T>
 */
public interface Slider<T> {

	/**
	 * Move to the next item in the list.
	 *
	 * @return
	 */
	T next();
}
