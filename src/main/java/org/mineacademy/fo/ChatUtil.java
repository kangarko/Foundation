package org.mineacademy.fo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for managing in-game chat.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatUtil {

	/**
	 * The default center padding
	 */
	public final static int CENTER_PX = 154;

	/**
	 * The vertical lines a player can see at once in his chat history
	 */
	public final static int VISIBLE_CHAT_LINES = 20;

	/**
	 * Centers a message automatically for padding {@link #CENTER_PX}
	 *
	 * @param message
	 * @return
	 */
	public static String center(final String message) {
		return center(message, ' ', ChatColor.WHITE);
	}

	/**
	 * Centers a message for padding using the given center px
	 *
	 * @param message
	 * @param centerPx
	 * @return
	 */
	public static String center(final String message, final int centerPx) {
		return center(message, ' ', ChatColor.WHITE, centerPx);
	}

	/**
	 * Centers a message for padding {@link #CENTER_PX} with the given space character
	 * colored by the given chat color, example:
	 *
	 * ================= My Centered Message ================= (if the space is '=')
	 *
	 * @param message
	 * @param space
	 * @param spaceColor
	 * @return
	 */
	public static String center(final String message, final char space, final ChatColor spaceColor) {
		return center(message, space, spaceColor, CENTER_PX);
	}

	/**
	 * Centers a message according to the given space character, color and padding
	 *
	 * @param message
	 * @param space
	 * @param spaceColor
	 * @param centerPx
	 * @return
	 */
	public static String center(final String message, final char space, final ChatColor spaceColor, final int centerPx) {
		if (message == null || message.equals(""))
			return "";

		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;

		for (final char c : message.toCharArray())
			if (c == '&') {
				previousCode = true;

				continue;

			} else if (previousCode == true) {
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
		final int spaceLength = DefaultFontInfo.getDefaultFontInfo(space).getLength() + 1;

		int compensated = 0;

		while (compensated < toCompensate) {
			builder.append(spaceColor.toString() + space);

			compensated += spaceLength;
		}

		return builder.toString() + " " + message + " " + builder.toString();
	}

	/**
	 * Moves the given messages to the center of the chat screen
	 *
	 * @param messages
	 * @return
	 */
	public static String[] verticalCenter(final String... messages) {
		return verticalCenter(Arrays.asList(messages));
	}

	/**
	 * Moves the given messages to the center of the chat screen
	 *
	 * @param messages
	 * @return
	 */
	public static String[] verticalCenter(final Collection<String> messages) {
		final List<String> lines = new ArrayList<>();
		final long padding = MathUtil.ceiling((VISIBLE_CHAT_LINES - messages.size()) / 2);

		for (int i = 0; i < padding; i++)
			lines.add(RandomUtil.nextColorOrDecoration());

		for (final String message : messages)
			lines.add(message);

		for (int i = 0; i < padding; i++)
			lines.add(RandomUtil.nextColorOrDecoration());

		return lines.toArray(new String[lines.size()]);
	}

	/**
	 * Inserts dots '.' into the message. Ignores domains and numbers.
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
	 * Makes first letters of sentences big. Ignores domains and detects multiple
	 * sentences.
	 *
	 * @param message the message to check
	 * @return capitalized message
	 */
	public static String capitalize(final String message) {
		if (message.isEmpty())
			return "";

		final String[] sentences = message.split("(?<=[!?\\.])\\s");
		String tempMessage = "";

		for (String sentence : sentences) {
			final String word = message.split("\\s")[0];

			if (!isDomain(word))
				sentence = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);

			tempMessage = tempMessage + sentence + " ";
		}
		return tempMessage.trim();
	}

	/**
	 * Lowercases the second character in a sentence in the message.
	 *
	 * @param message
	 * @return
	 */
	public static String lowercaseSecondChar(final String message) {
		if (message.isEmpty())
			return "";

		final String[] sentences = message.split("(?<=[!?\\.])\\s");
		String tempMessage = "";

		for (String sentence : sentences) {
			try {
				if (sentence.length() > 2)
					if (!isDomain(message.split("\\s")[0]) && sentence.length() > 2 && Character.isUpperCase(sentence.charAt(0)) && Character.isLowerCase(sentence.charAt(2)))
						sentence = sentence.substring(0, 1) + sentence.substring(1, 2).toLowerCase() + sentence.substring(2);

				tempMessage = tempMessage + sentence + " ";
			} catch (final NullPointerException ex) {
			}
		}
		return tempMessage.trim();
	}

	/**
	 * How much big letters the message has, in percentage.
	 *
	 * @param message the message to check
	 * @return how much percent of the message is big letters (from 0 to 100)
	 */
	public static double getCapsPercentage(final String message) {
		if (message.isEmpty())
			return 0;

		final String[] sentences = message.split(" ");
		String msgToCheck = "";
		double upperCount = 0;

		for (final String sentence : sentences)
			if (!isDomain(sentence))
				msgToCheck += sentence + " ";

		for (final char ch : msgToCheck.toCharArray())
			if (Character.isUpperCase(ch))
				upperCount++;

		return upperCount / msgToCheck.length();
	}

	/**
	 * How many big letters the message has.
	 *
	 * @param message the message to check
	 * @param ignored the list of strings to ignore (whitelist)
	 * @return how many big letters are in message
	 */
	public static int getCapsInRow(final String message, final List<String> ignored) {
		if (message.isEmpty())
			return 0;

		final int[] caps = splitCaps(message, ignored);

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
	 * Calculates the similarity (a double within 0.00 and 1.00) between two strings.
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public static double percentageSimilarity(final String first, final String second) {
		if (first.isEmpty() && second.isEmpty())
			return 1D;

		String longer = first, shorter = second;

		if (first.length() < second.length()) { // longer should always have greater length
			longer = second;
			shorter = first;
		}

		final int longerLength = longer.length();

		if (longerLength == 0)
			return 0; /* both strings are zero length */

		final double result = (longerLength - editDistance(longer, shorter)) / (double) longerLength;

		return result;

	}

	/**
	 * Return true if the given string is a http(s) and/or www domain
	 *
	 * @param message
	 * @return
	 */
	public static boolean isDomain(final String message) {
		return Common.regExMatch("(https?:\\/\\/(?:www\\.|(?!www))[^\\s\\.]+\\.[^\\s]{2,}|www\\.[^\\s]+\\.[^\\s]{2,})", message);
	}

	/**
	 * Replace special accented letters with their non-accented alternatives
	 * such as á is replaced by a
	 *
	 * @param message
	 * @return
	 */
	public static String replaceDiacritic(final String message) {
		return Normalizer.normalize(message, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	// --------------------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------------------

	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
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

	private static int[] splitCaps(final String message, final List<String> ignored) {
		final int[] editedMsg = new int[message.length()];
		final String[] parts = message.split(" ");

		for (int i = 0; i < parts.length; i++) {
			for (final String whitelisted : ignored)
				if (whitelisted.equalsIgnoreCase(parts[i]))
					parts[i] = parts[i].toLowerCase();
		}

		for (int i = 0; i < parts.length; i++)
			if (isDomain(parts[i]))
				parts[i] = parts[i].toLowerCase();

		final String msg = StringUtils.join(parts, " ");

		for (int i = 0; i < msg.length(); i++)
			if (Character.isUpperCase(msg.charAt(i)) && Character.isLetter(msg.charAt(i)))
				editedMsg[i] = 1;
			else
				editedMsg[i] = 0;
		return editedMsg;
	}
}

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
	SPACE(' ', 3),
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
