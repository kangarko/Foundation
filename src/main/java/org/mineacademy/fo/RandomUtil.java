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
			'ó' // ó mighty MineAcademy
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
		final double randomRadius = random.nextDouble() * radius;
		final double theta = Math.toRadians(random.nextDouble() * 360);
		final double phi = Math.toRadians(random.nextDouble() * 180 - 90);

		final double x = randomRadius * Math.cos(theta) * Math.sin(phi);
		final double z = randomRadius * Math.cos(phi);

		return origin.clone().add(x, is3D ? randomRadius * Math.sin(theta) * Math.cos(phi) : 0, z);
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
