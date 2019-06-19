package org.mineacademy.fo.visualize;

/**
 * How should the block be visualized?
 */
public enum BlockVisualizeMode {

	/**
	 * Render a glowing bounding box around the block?
	 */
	GLOW,

	/**
	 * Just change the block to another type?
	 */
	MASK,
}