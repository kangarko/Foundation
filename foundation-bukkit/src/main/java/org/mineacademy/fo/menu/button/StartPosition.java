package org.mineacademy.fo.menu.button;

import org.mineacademy.fo.menu.button.annotation.Position;

/**
 * Represents where we should begin placing a button that has the
 * {@link Position} annotation.
 *
 * Example: If the position annotation's value is 0, and
 * the start position is top left, the slot stays at 0.
 *
 * If the position is 5, and the start position is center,
 * then it is moved five slots to the right. If it is -5, it is
 * moved five slots to the left, etc.
 */
public enum StartPosition {

	TOP_LEFT,

	/**
	 * Automatically put AFTER the return button to the second most bottom left slot
	 * if return button exists
	 */
	BOTTOM_LEFT,

	BOTTOM_CENTER,

	CENTER,

}
