package org.mineacademy.fo;

import org.bukkit.util.Vector;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MathUtil extends MathUtilCore {

	// ----------------------------------------------------------------------------------------------------
	// Vectors
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Rotates the given vector by the given amount of angles (in degrees)
	 * Implementing a cache or precalculating this is recommended for best performance.
	 *
	 * @param vector
	 * @param angle
	 * @return
	 */
	public static Vector rotateAroundAxisX(Vector vector, double angle) {
		angle = Math.toRadians(angle);

		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double y = vector.getY() * cos - vector.getZ() * sin;
		final double z = vector.getY() * sin + vector.getZ() * cos;

		return vector.setY(y).setZ(z);
	}

	/**
	 * Rotates the given vector by the given amount of angles (in degrees)
	 * Implementing a cache or precalculating this is recommended for best performance.
	 *
	 * @param v
	 * @param angle
	 * @return
	 */
	public static Vector rotateAroundAxisY(Vector v, double angle) {
		angle = -angle;
		angle = Math.toRadians(angle);

		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double x = v.getX() * cos + v.getZ() * sin;
		final double z = v.getX() * -sin + v.getZ() * cos;

		return v.setX(x).setZ(z);
	}

	/**
	 * Rotates the given vector by the given amount of angles (in degrees)
	 * Implementing a cache or precalculating this is recommended for best performance.
	 *
	 * @param v
	 * @param angle
	 * @return
	 */
	public static Vector rotateAroundAxisZ(Vector v, double angle) {
		angle = Math.toRadians(angle);

		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double x = v.getX() * cos - v.getY() * sin;
		final double y = v.getX() * sin + v.getY() * cos;

		return v.setX(x).setY(y);
	}
}
