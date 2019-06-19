package org.mineacademy.fo.region;

import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.BlockClick;

import lombok.RequiredArgsConstructor;

/**
 * Represents a region point
 */
@RequiredArgsConstructor
public enum RegionPoint {

	/**
	 * Primary region point (first, left click)
	 */
	PRIMARY("Primary"),

	/**
	 * Secondary region point (second, right click)
	 */
	SECONDARY("Secondary");

	/**
	 * The localized unobfuscated key
	 */
	private final String key;

	@Override
	public String toString() {
		return key;
	}

	/**
	 * Converts a block click into a region point
	 *
	 * left -> primary and right -> secondary
	 *
	 * @param click
	 * @return
	 */
	public static RegionPoint fromClick(BlockClick click) {
		if (click == BlockClick.LEFT_CLICK)
			return PRIMARY;

		else if (click == BlockClick.RIGHT_CLICK)
			return RegionPoint.SECONDARY;

		throw new FoException("Unhandled region point from click " + click);
	}
}