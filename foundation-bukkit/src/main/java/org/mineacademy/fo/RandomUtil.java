package org.mineacademy.fo;

import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtil extends RandomUtilCore {

	/**
	 * Returns a random dye color
	 *
	 * @return
	 */
	public static DyeColor nextDyeColor() {
		return DyeColor.values()[random.nextInt(DyeColor.values().length)];
	}

	/**
	 * Return a random bright bukkit color, 7 colors are selected
	 *
	 * @return
	 */
	public static Color nextColor() {
		return nextItem(Color.AQUA, Color.ORANGE, Color.WHITE, Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE);
	}

	/**
	 * Returns a random location
	 *
	 * @param origin
	 * @param radius
	 * @param is3D true for sphere, false for cylinder search
	 * @return
	 */
	public static Location nextLocation(final Location origin, final double radius, final boolean is3D) {
		final double rectX = random.nextDouble() * radius;
		final double rectZ = random.nextDouble() * radius;
		final double offsetX;
		final double offsetZ;
		double offsetY = 0;
		final int transform = random.nextInt(4);
		if (is3D) {
			final double rectY = random.nextDouble() * radius;
			offsetY = getYCords(transform, rectY);
		}
		if (transform == 0) {
			offsetX = rectX;
			offsetZ = rectZ;
		} else if (transform == 1) {
			offsetX = -rectZ;
			offsetZ = rectX;
		} else if (transform == 2) {
			offsetX = -rectX;
			offsetZ = -rectZ;
		} else {
			offsetX = rectZ;
			offsetZ = -rectX;
		}

		return origin.clone().add(offsetX, offsetY, offsetZ);
	}

	/**
	 * Returns a random location, between the min and the max radius:
	 * Example: Min radius is 500 and max is 2000, then we return locations around 500-2000 blocks away from the origin
	 *
	 * @param origin
	 * @param minRadius
	 * @param maxRadius
	 * @param is3D true for sphere, false for cylinder search
	 * @return
	 */
	public static Location nextLocation(final Location origin, final double minRadius, final double maxRadius, final boolean is3D) {
		ValidCore.checkBoolean(maxRadius > 0 && minRadius > 0, "Max and min radius must be over 0");
		ValidCore.checkBoolean(maxRadius > minRadius, "Max radius must be greater than min radius");

		final double rectX = random.nextDouble() * (maxRadius - minRadius) + minRadius;
		final double rectZ = random.nextDouble() * (maxRadius + minRadius) - minRadius;
		final double offsetX;
		final double offsetZ;

		double offsetY = 0;
		final int transform = random.nextInt(4);
		if (is3D) {
			final double rectY = random.nextDouble() * (maxRadius + minRadius) - minRadius;
			offsetY = getYCords(transform, rectY);
		}
		if (transform == 0) {
			offsetX = rectX;
			offsetZ = rectZ;
		} else if (transform == 1) {
			offsetX = -rectZ;
			offsetZ = rectX;
		} else if (transform == 2) {
			offsetX = -rectX;
			offsetZ = -rectZ;
		} else {
			offsetX = rectZ;
			offsetZ = -rectX;
		}

		return origin.clone().add(offsetX, offsetY, offsetZ);
	}

	// Get the Y cords for the location
	private static double getYCords(int transform, double rectY) {
		double offsetY;

		final double nextY = random.nextDouble();

		if (transform < 2)
			offsetY = nextY >= 0.5 ? -rectY : rectY;

		else
			offsetY = nextY >= 0.5 ? rectY : -rectY;

		return offsetY;
	}

	/**
	 * Return a random x location within that chunk
	 *
	 * @param chunk
	 * @return
	 */
	public static int nextChunkX(final Chunk chunk) {
		return RandomUtil.nextInt(16) + (chunk.getX() << 4) - 16;
	}

	/**
	 * Return a random z location within that chunk
	 *
	 * @param chunk
	 * @return
	 */
	public static int nextChunkZ(final Chunk chunk) {
		return RandomUtil.nextInt(16) + (chunk.getZ() << 4) - 16;
	}
}
