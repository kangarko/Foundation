package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.mineacademy.fo.model.CompChatColor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Utility class for generating random numbers.
 *
 * This is a platform-neutral class, which is extended by "RandomUtil" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtil {

	/**
	 * The random instance for this class.
	 */
	@Getter
	private static final Random random = new Random();

	/**
	 * Symbols for chat colors using the & character including decorations like bold, italics etc.
	 */
	private static final char[] LEGACY_COLOR_AND_DECORATION_SYMBOLS = {
			'0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e',
			'f', 'k', 'l', 'n', 'o'
	};

	/**
	 * Letters for valid chat colors without decorations.
	 */
	private static final char[] LEGACY_CHAT_COLOR_SYMBOLS = {
			'0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e',
			'f'
	};

	/**
	 * English alphabet letters.
	 */
	private static final char[] ENGLISH_ALPHABET_LETTERS = {
			'a', 'b', 'c', 'd', 'e',
			'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o',
			'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'y', 'z',
	};

	/**
	 * Check if a random chance, based on a given percentage, succeeds.
	 * <p>
	 * This method simulates a percentage-based chance, where a random number is compared to the input percentage.
	 * <p>
	 * Example usage:
	 * <pre>
	 *   if (chance(25)) {
	 *       // 25% chance succeeds
	 *   }
	 * </pre>
	 *
	 * @param percent the success percentage (0-100)
	 * @return {@code true} if the random chance is below the given percentage, {@code false} otherwise
	 */
	public static boolean chance(final long percent) {
		return random.nextDouble() * 100D < percent;
	}

	/**
	 * Check if a random chance, based on a given decimal percentage, succeeds.
	 * <p>
	 * This method evaluates the chance using a decimal value where 1.0 represents 100%, 0.5 represents 50%, and so on.
	 * <p>
	 * Example usage:
	 * <pre>
	 *   if (chanceD(0.25)) {
	 *       // 25% chance succeeds
	 *   }
	 * </pre>
	 *
	 * @param percent the success percentage as a decimal (0.0 to 1.0)
	 * @return {@code true} if the random chance is below the given decimal percentage, {@code false} otherwise
	 */
	public static boolean chanceD(final double percent) {
		return random.nextDouble() < percent;
	}

	/**
	 * Returns a random chat color in this format: & + the color character.
	 * Example: &e for yellow.
	 *
	 * Will also return decorations.
	 *
	 * @return
	 */
	public static String nextColorOrDecorationAmpersand() {
		return "&" + LEGACY_COLOR_AND_DECORATION_SYMBOLS[nextInt(LEGACY_COLOR_AND_DECORATION_SYMBOLS.length)];
	}

	/**
	 * Returns a random chat color in this format: § + the color character.
	 * Example: §e for yellow.
	 *
	 * Will also return decorations.
	 *
	 * @return
	 */
	public static String nextColorOrDecorationSection() {
		return String.valueOf(CompChatColor.COLOR_CHAR) + LEGACY_COLOR_AND_DECORATION_SYMBOLS[nextInt(LEGACY_COLOR_AND_DECORATION_SYMBOLS.length)];
	}

	/**
	 * Generates random text, like lorem ipsum but gibberish.
	 *
	 * @param length
	 * @return
	 */
	public static String nextString(int length) {
		String text = "";

		for (int i = 0; i < length; i++)
			text += ENGLISH_ALPHABET_LETTERS[nextInt(ENGLISH_ALPHABET_LETTERS.length)];

		return text;
	}

	/**
	 * Return a random chat color.
	 *
	 * @return
	 */
	public static CompChatColor nextChatColor() {
		final char letter = LEGACY_CHAT_COLOR_SYMBOLS[nextInt(LEGACY_CHAT_COLOR_SYMBOLS.length)];

		return CompChatColor.getByChar(letter);
	}

	/**
	 * Returns a random integer in bounds.
	 *
	 * @param min
	 * @param max
	 * @return
	 */
	public static int nextIntBetween(final int min, final int max) {
		ValidCore.checkBoolean(min <= max, "Min !< max");

		return min + nextInt(max - min + 1);
	}

	/**
	 * Returns a random integer, see {@link Random#nextInt(int)}.
	 *
	 * @param boundExclusive
	 * @return
	 */
	public static int nextInt(final int boundExclusive) {
		ValidCore.checkBoolean(boundExclusive > 0, "Getting a random number must have the bound above 0, got: " + boundExclusive);

		return random.nextInt(boundExclusive);
	}

	/**
	 * Returns a random true/false by 50% chance.
	 *
	 * @return
	 */
	public static boolean nextBoolean() {
		return random.nextBoolean();
	}

	/**
	 * Return a random item in array.
	 *
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> T nextItem(final T... items) {
		return items[nextInt(items.length)];
	}

	/**
	 * Return a random item in list.
	 *
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> T nextItem(final Collection<T> items) {
		return nextItem(items, null);
	}

	/**
	 * Return a random item in list only among those that match the given condition.
	 *
	 * @param <T>
	 * @param items
	 * @param condition the condition applying when selecting or null if no condition
	 * @return
	 */
	public static <T> T nextItem(final Collection<T> items, final Predicate<T> condition) {
		final List<T> list = new ArrayList<>(items);

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
