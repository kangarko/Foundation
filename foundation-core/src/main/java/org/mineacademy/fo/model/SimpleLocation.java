package org.mineacademy.fo.model;

import org.mineacademy.fo.ValidCore;

import lombok.Data;
import lombok.NonNull;

/**
 * Represents a location in the world
 */
@Data
public final class SimpleLocation {

	/**
	 * The world name.
	 */
	private final String worldName;

	/**
	 * The x coordinate.
	 */
	private final int x;

	/**
	 * The y coordinate.
	 */
	private final int y;

	/**
	 * The z coordinate.
	 */
	private final int z;

	/**
	 * Create a new simple location.
	 *
	 * @param worldName
	 * @param x
	 * @param y
	 * @param z
	 */
	public SimpleLocation(@NonNull String worldName, int x, int y, int z) {
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Return the distance between this location and another location in the same world.
	 *
	 * @param other
	 * @return
	 */
	public double distance(SimpleLocation other) {
		ValidCore.checkBoolean(other.getWorldName().equalsIgnoreCase(this.worldName), "Cannot calculate distance between two locations in different worlds: " + this + " and other world: " + other);

		return Math.sqrt(Math.pow(other.getX() - this.x, 2) +
				Math.pow(other.getY() - this.y, 2) +
				Math.pow(other.getZ() - this.z, 2));
	}

	/**
	 * Get the location formatted as "world x y z"
	 *
	 * @return
	 */
	public String getFormatted() {
		return this.worldName + " " + this.x + " " + this.y + " " + this.z;
	}
}
