package org.mineacademy.fo.visualize_old;

/**
 * How should the block be visualized?
 *
 * @deprecated use classes in the new "visual" package
 */
@Deprecated
public enum VisualizeMode {

	/**
	 * Render a glowing bounding box around the block?
	 */
	GLOW,

	/**
	 * Just change the block to another type?
	 */
	MASK,
}