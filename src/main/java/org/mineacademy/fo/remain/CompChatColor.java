package org.mineacademy.fo.remain;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

import lombok.Getter;
import lombok.NonNull;

/**
 * Simplistic enumeration of all supported color values for chat.
 *
 * @author md_5, backported for comp. reasons by kangarko
 */
public final class CompChatColor {

	/**
	 * The special character which prefixes all chat colour codes. Use this if
	 * you need to dynamically convert colour codes from your custom format.
	 */
	public static final char COLOR_CHAR = '\u00A7';
	public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

	/**
	 * Pattern to remove all colour codes.
	 */
	public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + String.valueOf(COLOR_CHAR) + "[0-9A-FK-ORX]");

	/**
	 * Colour instances keyed by their active character.
	 */
	private static final Map<Character, CompChatColor> BY_CHAR = new HashMap<>();

	/**
	 * Colour instances keyed by their name.
	 */
	private static final Map<String, CompChatColor> BY_NAME = new HashMap<>();

	/**
	 * Represents black.
	 */
	public static final CompChatColor BLACK = new CompChatColor('0', "black", new Color(0x000000));

	/**
	 * Represents dark blue.
	 */
	public static final CompChatColor DARK_BLUE = new CompChatColor('1', "dark_blue", new Color(0x0000AA));

	/**
	 * Represents dark green.
	 */
	public static final CompChatColor DARK_GREEN = new CompChatColor('2', "dark_green", new Color(0x00AA00));

	/**
	 * Represents dark blue (aqua).
	 */
	public static final CompChatColor DARK_AQUA = new CompChatColor('3', "dark_aqua", new Color(0x00AAAA));

	/**
	 * Represents dark red.
	 */
	public static final CompChatColor DARK_RED = new CompChatColor('4', "dark_red", new Color(0xAA0000));

	/**
	 * Represents dark purple.
	 */
	public static final CompChatColor DARK_PURPLE = new CompChatColor('5', "dark_purple", new Color(0xAA00AA));

	/**
	 * Represents gold.
	 */
	public static final CompChatColor GOLD = new CompChatColor('6', "gold", new Color(0xFFAA00));

	/**
	 * Represents gray.
	 */
	public static final CompChatColor GRAY = new CompChatColor('7', "gray", new Color(0xAAAAAA));

	/**
	 * Represents dark gray.
	 */
	public static final CompChatColor DARK_GRAY = new CompChatColor('8', "dark_gray", new Color(0x555555));

	/**
	 * Represents blue.
	 */
	public static final CompChatColor BLUE = new CompChatColor('9', "blue", new Color(0x05555FF));

	/**
	 * Represents green.
	 */
	public static final CompChatColor GREEN = new CompChatColor('a', "green", new Color(0x55FF55));

	/**
	 * Represents aqua.
	 */
	public static final CompChatColor AQUA = new CompChatColor('b', "aqua", new Color(0x55FFFF));

	/**
	 * Represents red.
	 */
	public static final CompChatColor RED = new CompChatColor('c', "red", new Color(0xFF5555));

	/**
	 * Represents light purple.
	 */
	public static final CompChatColor LIGHT_PURPLE = new CompChatColor('d', "light_purple", new Color(0xFF55FF));

	/**
	 * Represents yellow.
	 */
	public static final CompChatColor YELLOW = new CompChatColor('e', "yellow", new Color(0xFFFF55));

	/**
	 * Represents white.
	 */
	public static final CompChatColor WHITE = new CompChatColor('f', "white", new Color(0xFFFFFF));

	/**
	 * Represents magical characters that change around randomly.
	 */
	public static final CompChatColor MAGIC = new CompChatColor('k', "obfuscated");

	/**
	 * Makes the text bold.
	 */
	public static final CompChatColor BOLD = new CompChatColor('l', "bold");

	/**
	 * Makes a line appear through the text.
	 */
	public static final CompChatColor STRIKETHROUGH = new CompChatColor('m', "strikethrough");

	/**
	 * Makes the text appear underlined.
	 */
	public static final CompChatColor UNDERLINE = new CompChatColor('n', "underline");

	/**
	 * Makes the text italic.
	 */
	public static final CompChatColor ITALIC = new CompChatColor('o', "italic");

	/**
	 * Resets all previous chat colors or formats.
	 */
	public static final CompChatColor RESET = new CompChatColor('r', "reset");

	/**
	 * This colour's colour char prefixed by the {@link #COLOR_CHAR}.
	 */
	private final String toString;

	@Getter
	private final String name;

	/**
	 * The RGB color of the ChatColor. null for non-colors (formatting)
	 */
	@Getter
	private final Color color;

	private CompChatColor(char code, String name) {
		this(code, name, null);
	}

	private CompChatColor(char code, String name, Color color) {
		this.name = name;
		this.toString = new String(new char[] { COLOR_CHAR, code });
		this.color = color;

		BY_CHAR.put(code, this);
		BY_NAME.put(name.toUpperCase(Locale.ROOT), this);
	}

	private CompChatColor(String name, String toString, int rgb) {
		this.name = name;
		this.toString = toString;
		this.color = new Color(rgb);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + Objects.hashCode(this.toString);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || getClass() != obj.getClass())
			return false;

		return Objects.equals(this.toString, ((CompChatColor) obj).toString);
	}

	@Override
	public String toString() {
		return toString;
	}

	/**
	 * Strips the given message of all color codes
	 *
	 * @param input String to strip of color
	 * @return A copy of the input string, without any coloring
	 */
	public static String stripColor(final String input) {
		if (input == null) {
			return null;
		}

		return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
	}

	public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
		final char[] b = textToTranslate.toCharArray();

		for (int i = 0; i < b.length - 1; i++)
			if (b[i] == altColorChar && ALL_CODES.indexOf(b[i + 1]) > -1) {
				b[i] = CompChatColor.COLOR_CHAR;

				b[i + 1] = Character.toLowerCase(b[i + 1]);
			}

		return new String(b);
	}

	/**
	 * Get the colour represented by the specified code.
	 *
	 * @param code the code to search for
	 * @return the mapped colour, or null if non exists
	 */
	public static CompChatColor getByChar(char code) {
		return BY_CHAR.get(code);
	}

	public static CompChatColor of(Color color) {
		return of("#" + Integer.toHexString(color.getRGB()).substring(2));
	}

	public static CompChatColor of(@NonNull String string) {

		if (string.startsWith("#") && string.length() == 7) {

			if (!MinecraftVersion.atLeast(V.v1_16))
				throw new IllegalArgumentException("Only Minecraft 1.16+ supports # HEX color codes!");

			int rgb;

			try {
				rgb = Integer.parseInt(string.substring(1), 16);

			} catch (final NumberFormatException ex) {
				throw new IllegalArgumentException("Illegal hex string " + string);
			}

			final StringBuilder magic = new StringBuilder(COLOR_CHAR + "x");

			for (final char c : string.substring(1).toCharArray())
				magic.append(COLOR_CHAR).append(c);

			return new CompChatColor(string, magic.toString(), rgb);
		}

		if (string.length() == 2) {
			if (string.charAt(0) != '&')
				throw new IllegalArgumentException("Invalid syntax, please use & + color code. Got: " + string);

			final CompChatColor byChar = BY_CHAR.get(string.charAt(1));

			if (byChar != null)
				return byChar;

		} else {
			final CompChatColor byName = BY_NAME.get(string.toUpperCase(Locale.ROOT));

			if (byName != null)
				return byName;

			if (string.equalsIgnoreCase("magic"))
				return MAGIC;
		}

		throw new IllegalArgumentException("Could not parse CompChatColor " + string);
	}

	/**
	 * Get an array of all defined colors and formats.
	 *
	 * @return copied array of all colors and formats
	 */
	public static CompChatColor[] values() {
		return BY_CHAR.values().toArray(new CompChatColor[BY_CHAR.values().size()]);
	}

	/**
	 * Return a list of all colors
	 *
	 * @return
	 */
	public static List<CompChatColor> getColors() {
		return Arrays.asList(BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE);
	}

	/**
	 * Return a list of all decorations
	 *
	 * @return
	 */
	public static List<CompChatColor> getDecorations() {
		return Arrays.asList(MAGIC, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC);
	}
}
