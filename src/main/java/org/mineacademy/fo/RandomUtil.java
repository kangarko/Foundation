package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for generating random numbers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtil {

	/**
	 * The random instance for this class
	 */
	private static final Random random = new Random();

	/**
	 * Symbols for chat colors using the & character including decorations like bold italics etc
	 */
	private static final char[] COLORS_AND_DECORATION = {
			'0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e',
			'f', 'k', 'l', 'n', 'o'
	};

	/**
	 * Only valid chat colors without decorations
	 */
	private static final char[] CHAT_COLORS = {
			'0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e',
			'f'
	};

	/**
	 * English alphabet letters
	 */
	private static final char[] LETTERS = {
			'a', 'b', 'c', 'd', 'e',
			'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o',
			'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'y', 'z',
	};

	/**
	 * Return the random instance
	 *
	 * @return
	 */
	public static Random getRandom() {
		return random;
	}

	/**
	 * Return true if the given percent was matched
	 *
	 * @param percent the percent, from 0 to 100
	 * @return
	 */
	public static boolean chance(final long percent) {
		return chance((int) percent);
	}

	/**
	 * Return true if the given percent was matched
	 *
	 * @param percent the percent, from 0 to 100
	 * @return
	 */
	public static boolean chance(final int percent) {
		return random.nextDouble() * 100D < percent;
	}

	/**
	 * Return true if the given percent was matched
	 *
	 * @param percent the percent, from 0.00 to 1.00
	 * @return
	 */
	public static boolean chanceD(final double percent) {
		return random.nextDouble() < percent;
	}

	/**
	 * Returns a random dye color
	 *
	 * @return
	 */
	public static DyeColor nextDyeColor() {
		return DyeColor.values()[random.nextInt(DyeColor.values().length)];
	}

	/**
	 * Returns a random chat color in this format: & + the color character
	 * Example: &e for yellow
	 * <p>
	 * Will also return decorations
	 *
	 * @return
	 */
	public static String nextColorOrDecoration() {
		return "&" + COLORS_AND_DECORATION[nextInt(COLORS_AND_DECORATION.length)];
	}

	/**
	 * Generates random text, like lorem ipsum but completely
	 * different.
	 *
	 * @param length
	 * @return
	 */
	public static String nextString(int length) {
		String text = "";

		for (int i = 0; i < length; i++)
			text += LETTERS[nextInt(LETTERS.length)];

		return text;
	}

	/**
	 * Return a random chat color
	 *
	 * @return
	 */
	public static ChatColor nextChatColor() {
		final char letter = CHAT_COLORS[nextInt(CHAT_COLORS.length)];

		return ChatColor.getByChar(letter);
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
	 * Returns a random integer in bounds
	 *
	 * @param min
	 * @param max
	 * @return
	 */
	public static int nextBetween(final int min, final int max) {
		Valid.checkBoolean(min <= max, "Min !< max");

		return min + nextInt(max - min + 1);
	}

	/**
	 * Returns a random integer, see {@link Random#nextInt(int)}
	 *
	 * @param boundExclusive
	 * @return
	 */
	public static int nextInt(final int boundExclusive) {
		Valid.checkBoolean(boundExclusive > 0, "Getting a random number must have the bound above 0, got: " + boundExclusive);

		return random.nextInt(boundExclusive);
	}

	/**
	 * Returns a random true/false by 50% chance
	 *
	 * @return
	 */
	public static boolean nextBoolean() {
		return random.nextBoolean();
	}

	/**
	 * Return a random item in array
	 *
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> T nextItem(final T... items) {
		return items[nextInt(items.length)];
	}

	/**
	 * Return a random item in list
	 *
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> T nextItem(final Iterable<T> items) {
		return nextItem(items, null);
	}

	/**
	 * Return a random item in list only among those that match the given condition
	 *
	 * @param <T>
	 * @param items
	 * @param condition the condition applying when selecting
	 * @return
	 */
	public static <T> T nextItem(final Iterable<T> items, final Predicate<T> condition) {
		final List<T> list = items instanceof List ? new ArrayList<>((List<T>) items) : Common.toList(items);

		// Remove values failing the condition
		if (condition != null)
			for (final Iterator<T> it = list.iterator(); it.hasNext();) {
				final T item = it.next();

				if (!condition.test(item))
					it.remove();
			}

		return list.get(nextInt(list.size()));
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
		Valid.checkBoolean(maxRadius > 0 && minRadius > 0, "Max and min radius must be over 0");
		Valid.checkBoolean(maxRadius > minRadius, "Max radius must be greater than min radius");

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

	public static double getYCords(int transform, double rectY) {
		double offsetY;
		double nextY = random.nextDouble();
		if (transform < 2) {
			offsetY = nextY >= 0.5 ? -rectY : rectY;
		} else {
			offsetY = nextY >= 0.5 ? rectY : -rectY;
		}
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
