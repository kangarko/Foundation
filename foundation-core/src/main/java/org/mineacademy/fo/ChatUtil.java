package org.mineacademy.fo;

import java.awt.Color;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.Whiteblacklist;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for managing Minecraft chat.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatUtil {

	/**
	 * The default center padding.
	 */
	private final static int CENTER_PX = 152;

	/**
	 * The pattern to match a domain.
	 */
	private final static Pattern DOMAIN_PATTERN = Pattern.compile("(https?:\\/\\/(?:www\\.|(?!www))[^\\s\\.]+\\.[^\\s]{2,}|www\\.[^\\s]+\\.[^\\s]{2,})");

	/**
	 * Centers a message in chat.
	 *
	 * @see #center(String, char, int)
	 *
	 * @param message
	 *
	 * @deprecated on modern Minecraft version the method does not properly format bold and new Minecraft unicode letters.
	 * @return
	 */
	@Deprecated
	public static String center(final String message) {
		return center(message, ' ');
	}

	/**
	 * Centers a message in chat.
	 *
	 * @see #center(String, char, int)
	 *
	 * @param message
	 * @param centerPx
	 *
	 * @deprecated on modern Minecraft version the method does not properly format bold and new Minecraft unicode letters.
	 * @return
	 */
	@Deprecated
	public static String center(final String message, final int centerPx) {
		return center(message, ' ', centerPx);
	}

	/**
	 * Centers a message in chat.
	 *
	 * @see #center(String, char, int)
	 *
	 * @param message
	 * @param space
	 *
	 * @deprecated on modern Minecraft version the method does not properly format bold and new Minecraft unicode letters.
	 * @return
	 */
	@Deprecated
	private static String center(final String message, final char space) {
		return center(message, space, CENTER_PX);
	}

	/**
	 * Center a string within a given width by padding it with a specified character on both sides.
	 *
	 * <p>This method calculates how many characters are needed to pad the message on each side to center it within
	 * a specified pixel width. It supports Minecraft text formatting codes (including bold), ensuring that formatting is not counted in the width.
	 *
	 * <ul>
	 * <li> For example, calling {@code center("Hello", ' ', 100)} will center "Hello" within 100 pixels, padded with spaces.
	 * </ul>
	 *
	 * @param message the string to center
	 * @param space the character used for padding
	 * @param centerPx the total pixel width to center the message in
	 *
	 * @deprecated on modern Minecraft version the method does not properly format bold and new Minecraft unicode letters.
	 * @return the centered string with padding, or an empty string if the message is null or empty
	 */
	@Deprecated
	public static String center(final String message, final char space, final int centerPx) {
		if (message == null || message.equals(""))
			return "";

		int messagePxSize = 0;

		boolean previousCode = false;
		boolean isBold = false;

		for (final char c : message.toCharArray())

			if (c == '&' || c == CompChatColor.COLOR_CHAR) {
				previousCode = true;

				continue;

			} else if (previousCode) {
				previousCode = false;

				if (c == 'l' || c == 'L') {
					isBold = true;

					continue;

				}

				isBold = false;

			} else {
				final DefaultFontInfo defaultFont = DefaultFontInfo.getDefaultFontInfo(c);

				messagePxSize += isBold ? defaultFont.getBoldLength() : defaultFont.getLength();
				messagePxSize++;
			}

		final StringBuilder builder = new StringBuilder();

		final int halvedMessageSize = messagePxSize / 2;
		final int toCompensate = centerPx - halvedMessageSize;
		final DefaultFontInfo font = DefaultFontInfo.getDefaultFontInfo(space);
		final double spaceLength = isBold ? font.getBoldLength() : font.getLength();

		double compensated = 0;

		while (compensated < toCompensate) {
			builder.append(space);

			compensated += spaceLength;
		}

		return builder.toString() + " " + message + " " + builder.toString();
	}

	/**
	 * Insert dots '.' into the message. Ignore domain and number.
	 *
	 * @param message the message to be processed
	 * @return message with dots inserted
	 */
	public static String insertDot(String message) {
		if (message.isEmpty())
			return "";

		final String lastChar = message.substring(message.length() - 1);
		final String[] words = message.split("\\s");
		final String lastWord = words[words.length - 1];

		if (!isDomain(lastWord) && lastChar.matches("(?i)[a-z\u0400-\u04FF]"))
			message = message + ".";

		return message;
	}

	/**
	 * Make first letters of sentences big. Ignore domains and detect multiple
	 * sentences.
	 *
	 * @param message the message to check
	 * @return capitalized message
	 */
	public static String capitalizeFirst(final String message) {
		if (message.isEmpty())
			return "";

		final String[] sentences = message.split("(?<=[!?\\.])\\s");
		String tempMessage = "";

		for (String sentence : sentences)
			try {
				final String word = message.split("\\s")[0];

				if (!isDomain(word))
					sentence = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);

				tempMessage = tempMessage + sentence + " ";
			} catch (final ArrayIndexOutOfBoundsException ex) {
				// Probably an exotic language, silence
			}

		return tempMessage.trim();
	}

	/**
	 * @see #capitalizeFully(String)
	 *
	 * @param color
	 * @return
	 */
	public static String capitalizeFully(@NonNull CompChatColor color) {
		return capitalizeFully(color.getName());
	}

	/**
	 * @see #capitalizeFully(String)
	 *
	 * @param enumeration
	 * @return
	 */
	public static String capitalizeFully(@NonNull Enum<?> enumeration) {
		return capitalizeFully(enumeration.toString());
	}

	/**
	 * Capitalize each word in the provided string, converting the entire string to lowercase first.
	 *
	 * <p>This method transforms the input string to lowercase, replaces underscores with spaces, and then capitalizes the first letter of each word.
	 *
	 * <ul>
	 * <li> For example, calling {@code capitalizeFully("HELLO_WORLD")} will return "Hello World".
	 * </ul>
	 *
	 * @param name the string to be fully capitalized
	 * @return the fully capitalized string
	 */
	public static String capitalizeFully(@NonNull String name) {
		return capitalize(name.toLowerCase().replace("_", " "));
	}

	/**
	 * <p>Capitalizes all the whitespace separated words in a String.
	 * Only the first letter of each word is changed.
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A <code>null</code> input String returns <code>null</code>.
	 * Capitalization uses the unicode title case, normally equivalent to
	 * upper case.</p>
	 *
	 * <pre>
	 * capitalize(null)        = null
	 * capitalize("")          = ""
	 * capitalize("i am FINE") = "I Am FINE"
	 * capitalize("&7i am FINE") = "I Am FINE" // Colors are supported!
	 * </pre>
	 *
	 * @author Apache Commons - WordUtils
	 * @param message  the String to capitalize, may be null
	 * @return capitalized String, <code>null</code> if null String input
	 */
	public static String capitalize(String message) {
		if (message != null && message.length() != 0) {
			final int strLen = message.length();
			final StringBuffer buffer = new StringBuffer(strLen);
			boolean capitalizeNext = true;

			for (int i = 0; i < strLen; ++i) {
				final char letter = message.charAt(i);

				if (Character.isWhitespace(letter)) {
					buffer.append(letter);

					capitalizeNext = true;

				} else if (capitalizeNext) {
					buffer.append(Character.toTitleCase(letter));

					capitalizeNext = false;
				} else
					buffer.append(letter);

			}

			return buffer.toString();
		}

		return message;
	}

	/**
	 * An improved version of {@link Matcher#quoteReplacement(String)}
	 * where we quote additional letters such as ()+
	 *
	 * @param message
	 * @return
	 */
	public static String quoteReplacement(String message) {
		final StringBuilder builder = new StringBuilder();

		for (int index = 0; index < message.length(); index++) {
			final char c = message.charAt(index);

			if (c == ' ' || c == '\\' || c == '$' || c == '(' || c == ')' || c == '+' || c == '.' || c == '-' || c == '_' || c == '^')
				builder.append('\\');

			builder.append(c);
		}

		return builder.toString();
	}

	/**
	 * Remove emojis from the given message.
	 *
	 * @author https://stackoverflow.com/a/32101331
	 * @param message
	 * @return
	 */
	public static String removeEmoji(String message) {
		if (message == null)
			return "";

		final String regex = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]";
		final Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		final Matcher matcher = pattern.matcher(message);

		return matcher.replaceAll("");
	}

	/**
	 * Calculate the percentage of capital letters in a string, ignoring domain-like words.
	 *
	 * <p>This method checks how many characters in the given message are uppercase, excluding parts that look like domains (e.g., URLs).
	 * It then returns the ratio of uppercase letters to the total length of the checked message as a percentage.
	 *
	 * <ul>
	 * <li> For example, calling {@code getCapsPercentage("HELLO world www.example.com")} will only count "HELLO world" and return a caps percentage.
	 * </ul>
	 *
	 * @param message the string to calculate the percentage of capital letters
	 * @return the percentage of uppercase characters in the message from 0 to 100, or 0 if the message is empty
	 */
	public static double getCapsPercentage(final String message) {
		if (message.isEmpty())
			return 0;

		final String[] sentences = message.split(" ");
		String messageToCheck = "";
		double upperCount = 0;

		for (final String sentence : sentences)
			if (!isDomain(sentence))
				messageToCheck += sentence + " ";

		for (final char ch : messageToCheck.toCharArray())
			if (Character.isUpperCase(ch))
				upperCount++;

		return upperCount / messageToCheck.length();
	}

	/**
	 * Calculate the maximum number of consecutive uppercase letters in a string, excluding ignored words.
	 *
	 * <p>This method counts how many consecutive uppercase letters appear in a string. It ignores sequences defined
	 * in the provided {@code Whiteblacklist}. The method returns the longest streak of consecutive uppercase characters.
	 *
	 * <ul>
	 * <li> For example, calling {@code getCapsInRow("HELLO world", ignored)} will return 5 for "HELLO".
	 * </ul>
	 *
	 * @param message the string to examine
	 * @param ignored a {@code Whiteblacklist} defining words or patterns to ignore
	 * @return the maximum number of consecutive uppercase letters in the message, or 0 if the message is empty
	 */
	public static int getCapsInRow(final String message, final Whiteblacklist ignored) {
		if (message.isEmpty())
			return 0;

		final int[] caps = new int[message.length()];
		final String[] parts = message.split(" ");

		for (int i = 0; i < parts.length; i++)
			if (ignored.isInList(parts[i]))
				parts[i] = parts[i].toLowerCase();

		for (int i = 0; i < parts.length; i++)
			if (isDomain(parts[i]))
				parts[i] = parts[i].toLowerCase();

		final String editedMessage = String.join(" ", parts);

		for (int i = 0; i < editedMessage.length(); i++)
			if (Character.isUpperCase(editedMessage.charAt(i)) && Character.isLetter(editedMessage.charAt(i)))
				caps[i] = 1;
			else
				caps[i] = 0;

		int sum = 0;
		int sumTemp = 0;

		for (final int i : caps)
			if (i == 1) {
				sumTemp++;
				sum = Math.max(sum, sumTemp);
			} else
				sumTemp = 0;

		return sum;
	}

	/**
	 * Calculate the similarity percentage between two strings using edit distance.
	 *
	 * <p>This method compares two strings and returns a percentage representing how similar they are. It first removes unwanted characters using {@code removeSimilarity()} and then calculates the edit distance (Levenshtein distance) between the two strings.
	 *
	 * <ul>
	 * <li> For example, calling {@code getSimilarityPercentage("hello", "hallo")} will return the percentage of similarity between the two words.
	 * </ul>
	 *
	 * @param first the first string to compare
	 * @param second the second string to compare
	 * @return a similarity between 0.0 and 1.0, where 1 means the strings are identical
	 */
	public static double getSimilarityPercentage(String first, String second) {
		if (first.isEmpty() && second.isEmpty())
			return 1D;

		if (Platform.getPlugin().isSimilarityStrippingAccents()) {
			first = replaceDiacritic(first);
			second = replaceDiacritic(second);
		}

		first = first.toLowerCase();
		second = second.toLowerCase();

		String longer = first, shorter = second;

		if (first.length() < second.length()) { // longer should always have greater length
			longer = second;
			shorter = first;
		}

		final int longerLength = longer.length();

		if (longerLength == 0)
			return 0; /* both strings are zero length */

		return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
	}

	/**
	 * Return true if the given string is a http(s) and/or www domain.
	 *
	 * @param message
	 * @return
	 */
	public static boolean isDomain(final String message) {
		return DOMAIN_PATTERN.matcher(message).find();
	}

	/**
	 * Replace special accented letters with their non-accented alternatives
	 * such as "á" is replaced by "a".
	 *
	 * @param message
	 * @return
	 */
	public static String replaceDiacritic(final String message) {
		return Normalizer.normalize(message, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	/**
	 * Generate gradient for the given string using the two colors as start/ending colors
	 * in the RGB format.
	 *
	 * On Minecraft older than 1.16, colors are downsampled to their closest legacy color.
	 *
	 * @param message
	 * @param fromColor
	 * @param toColor the color, such as #FF1122 or &c or red
	 * @return
	 */
	public static String generateGradient(String message, String fromColor, String toColor) {
		return generateGradient(message, CompChatColor.fromString(fromColor), CompChatColor.fromString(toColor));
	}

	/**
	 * Generate gradient for the given string using the two colors as start/ending colors
	 * in the #RGB format.
	 *
	 * On Minecraft older than 1.16, colors are downsampled to their closest legacy color.
	 *
	 * @param message
	 * @param fromColor
	 * @param toColor
	 * @return
	 */
	public static String generateGradient(String message, CompChatColor fromColor, CompChatColor toColor) {
		return generateGradient(message, fromColor.getColor(), toColor.getColor());
	}

	/**
	 * Generate a gradient effect over the characters of a given message by transitioning from one color to another.
	 *
	 * <p>This method applies a smooth color gradient to the message, starting from {@code fromColor} and ending at {@code toColor}.
	 * It also supports Minecraft formatting codes like bold, italic, etc., ensuring decorations are applied correctly after each character.
	 *
	 * <ul>
	 * <li> For example, calling {@code generateGradient("Hello", Color.RED, Color.BLUE)} will return the string with a red-to-blue gradient applied.
	 * </ul>
	 *
	 * On Minecraft older than 1.16, colors are downsampled to their closest legacy color.
	 *
	 * @param message the text to which the gradient will be applied
	 * @param fromColor the starting color of the gradient
	 * @param toColor the ending color of the gradient
	 * @return a string with color codes representing the gradient effect
	 */
	public static String generateGradient(String message, Color fromColor, Color toColor) {
		final char[] letters = message.toCharArray();
		String gradient = "";

		final List<String> decorations = new ArrayList<>();

		for (int i = 0; i < letters.length; i++) {
			final char letter = letters[i];

			// Support color decoration and insert it manually after each character
			if (letter == CompChatColor.COLOR_CHAR && i + 1 < letters.length) {
				final char decoration = letters[i + 1];

				if (decoration == 'k')
					decorations.add(CompChatColor.MAGIC.toString());

				else if (decoration == 'l')
					decorations.add(CompChatColor.BOLD.toString());

				else if (decoration == 'm')
					decorations.add(CompChatColor.STRIKETHROUGH.toString());

				else if (decoration == 'n')
					decorations.add(CompChatColor.UNDERLINE.toString());

				else if (decoration == 'o')
					decorations.add(CompChatColor.ITALIC.toString());

				else if (decoration == 'r')
					decorations.add(CompChatColor.RESET.toString());

				i++;
				continue;
			}

			final float ratio = (float) i / (float) letters.length;

			final int red = (int) (toColor.getRed() * ratio + fromColor.getRed() * (1 - ratio));
			final int green = (int) (toColor.getGreen() * ratio + fromColor.getGreen() * (1 - ratio));
			final int blue = (int) (toColor.getBlue() * ratio + fromColor.getBlue() * (1 - ratio));

			final Color stepColor = new Color(red, green, blue);

			gradient += "<" + CompChatColor.fromColor(stepColor) + ">" + String.join("", decorations) + letters[i];
		}

		return gradient;
	}

	// --------------------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------------------

	/*
	 * Example implementation of the Levenshtein Edit Distance
	 * See http://rosettacode.org/wiki/Levenshtein_distance#Java
	 */
	private static int editDistance(String first, String second) {
		first = first.toLowerCase();
		second = second.toLowerCase();

		final int[] costs = new int[second.length() + 1];
		for (int i = 0; i <= first.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= second.length(); j++)
				if (i == 0)
					costs[j] = j;
				else if (j > 0) {
					int newValue = costs[j - 1];
					if (first.charAt(i - 1) != second.charAt(j - 1))
						newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
					costs[j - 1] = lastValue;
					lastValue = newValue;
				}
			if (i > 0)
				costs[second.length()] = lastValue;
		}
		return costs[second.length()];
	}
}

/**
 * Stores most Minecraft letters with their length.
 *
 * @deprecated does not properly format bold and new Minecraft unicode letters.
 */
@Deprecated
enum DefaultFontInfo {

	A('A', 5),
	a('a', 5),
	B('B', 5),
	b('b', 5),
	C('C', 5),
	c('c', 5),
	D('D', 5),
	d('d', 5),
	E('E', 5),
	e('e', 5),
	F('F', 5),
	f('f', 4),
	G('G', 5),
	g('g', 5),
	H('H', 5),
	h('h', 5),
	I('I', 3),
	i('i', 1),
	J('J', 5),
	j('j', 5),
	K('K', 5),
	k('k', 4),
	L('L', 5),
	l('l', 1),
	M('M', 5),
	m('m', 5),
	N('N', 5),
	n('n', 5),
	O('O', 5),
	o('o', 5),
	P('P', 5),
	p('p', 5),
	Q('Q', 5),
	q('q', 5),
	R('R', 5),
	r('r', 5),
	S('S', 5),
	s('s', 5),
	T('T', 5),
	t('t', 4),
	U('U', 5),
	u('u', 5),
	V('V', 5),
	v('v', 5),
	W('W', 5),
	w('w', 5),
	X('X', 5),
	x('x', 5),
	Y('Y', 5),
	y('y', 5),
	Z('Z', 5),
	z('z', 5),
	NUM_1('1', 5),
	NUM_2('2', 5),
	NUM_3('3', 5),
	NUM_4('4', 5),
	NUM_5('5', 5),
	NUM_6('6', 5),
	NUM_7('7', 5),
	NUM_8('8', 5),
	NUM_9('9', 5),
	NUM_0('0', 5),
	EXCLAMATION_POINT('!', 1),
	AT_SYMBOL('@', 6),
	NUM_SIGN('#', 5),
	DOLLAR_SIGN('$', 5),
	PERCENT('%', 5),
	UP_ARROW('^', 5),
	AMPERSAND('&', 5),
	ASTERISK('*', 5),
	LEFT_PARENTHESIS('(', 4),
	RIGHT_PERENTHESIS(')', 4),
	MINUS('-', 5),
	UNDERSCORE('_', 5),
	PLUS_SIGN('+', 5),
	EQUALS_SIGN('=', 5),
	LEFT_CURL_BRACE('{', 4),
	RIGHT_CURL_BRACE('}', 4),
	LEFT_BRACKET('[', 3),
	RIGHT_BRACKET(']', 3),
	COLON(':', 1),
	SEMI_COLON(';', 1),
	DOUBLE_QUOTE('"', 3),
	SINGLE_QUOTE('\'', 1),
	LEFT_ARROW('<', 4),
	RIGHT_ARROW('>', 4),
	QUESTION_MARK('?', 5),
	SLASH('/', 5),
	BACK_SLASH('\\', 5),
	LINE('|', 1),
	TILDE('~', 5),
	TICK('`', 2),
	PERIOD('.', 1),
	COMMA(',', 1),
	SPACE(' ', 4),
	DEFAULT('a', 4);

	private final char character;
	private final int length;

	DefaultFontInfo(final char character, final int length) {
		this.character = character;
		this.length = length;
	}

	public char getCharacter() {
		return this.character;
	}

	public int getLength() {
		return this.length;
	}

	public int getBoldLength() {
		if (this == DefaultFontInfo.SPACE)
			return this.getLength();
		return this.length + 1;
	}

	public static DefaultFontInfo getDefaultFontInfo(final char c) {
		for (final DefaultFontInfo dFI : DefaultFontInfo.values())
			if (dFI.getCharacter() == c)
				return dFI;

		return DefaultFontInfo.DEFAULT;
	}
}
