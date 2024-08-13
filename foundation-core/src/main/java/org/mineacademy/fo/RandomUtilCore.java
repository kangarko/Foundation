package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.mineacademy.fo.remain.CompChatColor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for generating random numbers.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class RandomUtilCore {

	/**
	 * The random instance for this class
	 */
	protected static final Random random = new Random();

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
	public static CompChatColor nextChatColor() {
		final char letter = CHAT_COLORS[nextInt(CHAT_COLORS.length)];

		return CompChatColor.getByChar(letter);
	}

	/**
	 * Returns a random integer in bounds
	 *
	 * @param min
	 * @param max
	 * @return
	 */
	public static int nextBetween(final int min, final int max) {
		ValidCore.checkBoolean(min <= max, "Min !< max");

		return min + nextInt(max - min + 1);
	}

	/**
	 * Returns a random integer, see {@link Random#nextInt(int)}
	 *
	 * @param boundExclusive
	 * @return
	 */
	public static int nextInt(final int boundExclusive) {
		ValidCore.checkBoolean(boundExclusive > 0, "Getting a random number must have the bound above 0, got: " + boundExclusive);

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
		final List<T> list = items instanceof List ? new ArrayList<>((List<T>) items) : CommonCore.toList(items);

		// Remove values failing the condition
		if (condition != null)
			for (final Iterator<T> it = list.iterator(); it.hasNext();) {
				final T item = it.next();

				if (!condition.test(item))
					it.remove();
			}

		return list.get(nextInt(list.size()));
	}
}
