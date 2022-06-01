package org.mineacademy.fo.remain;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

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
	 * Colour instances keyed by their active character.
	 */
	private static final Map<Character, CompChatColor> BY_CHAR = new HashMap<>();

	/**
	 * Colour instances keyed by their name.
	 */
	private static final Map<String, CompChatColor> BY_NAME = new HashMap<>();

	/**
	 * Represents colors we can use for MC before 1.16
	 */
	private static final Color[] LEGACY_COLORS = {
			new Color(0, 0, 0),
			new Color(0, 0, 170),
			new Color(0, 170, 0),
			new Color(0, 170, 170),
			new Color(170, 0, 0),
			new Color(170, 0, 170),
			new Color(255, 170, 0),
			new Color(170, 170, 170),
			new Color(85, 85, 85),
			new Color(85, 85, 255),
			new Color(85, 255, 85),
			new Color(85, 255, 255),
			new Color(255, 85, 85),
			new Color(255, 85, 255),
			new Color(255, 255, 85),
			new Color(255, 255, 255),
	};

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
	 * The code representing this color such as a, r, etc.
	 */
	private final char code;

	/**
	 * The name of this color
	 */
	@Getter
	private final String name;

	/**
	 * The RGB color of the ChatColor. null for non-colors (formatting)
	 */
	@Getter
	private final Color color;

	/**
	 * This colour's colour char prefixed by the {@link #COLOR_CHAR}.
	 */
	private final String toString;

	private CompChatColor(char code, String name) {
		this(code, name, null);
	}

	private CompChatColor(char code, String name, Color color) {
		this.code = code;
		this.name = name;
		this.color = color;
		this.toString = new String(new char[] { COLOR_CHAR, code });

		BY_CHAR.put(code, this);
		BY_NAME.put(name.toUpperCase(Locale.ROOT), this);
	}

	private CompChatColor(String name, String toString, int rgb) {
		this.code = '#';
		this.name = name;
		this.color = new Color(rgb);
		this.toString = toString;
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

		if (obj == null || this.getClass() != obj.getClass())
			return false;

		return Objects.equals(this.toString, ((CompChatColor) obj).toString);
	}

	/**
	 * Get the color code
	 *
	 * @return the code
	 */
	public char getCode() {
		Valid.checkBoolean(this.code != '#', "Cannot retrieve color code for HEX colors");

		return this.code;
	}

	/**
	 * Return true if the color is HEX?
	 *
	 * @return
	 */
	public boolean isHex() {
		return this.code == '#';
	}

	/**
	 * Return the color's name such as red, or in case of hex color return
	 * the code, colorized
	 * @return
	 */
	public String toEscapedString() {
		return this.isHex() ? this.toString + "\\\\" + this.getName() : ItemUtil.bountify(this.getName());
	}

	/**
	 * Return a string you can save to YAML config
	 *
	 * @return
	 */
	public String toSaveableString() {
		return this.getName();
	}

	/**
	 * This will translate the color into the actual color, use getName to get the saveable color!
	 */
	@Override
	public String toString() {
		return this.toString;
	}

	/**
	 * Get the color represented by the specified code.
	 *
	 * @param code the code to search for
	 * @return the mapped colour, or null if non exists
	 */
	public static CompChatColor getByChar(char code) {
		return BY_CHAR.get(code);
	}

	/**
	 * Parse the given color to chat color
	 *
	 * @param color
	 * @return
	 */
	public static CompChatColor of(Color color) {
		return of("#" + Integer.toHexString(color.getRGB()).substring(2));
	}

	/**
	 * Get a color from #123456 HEX code, & color code or name
	 *
	 * @param string
	 * @return
	 */
	public static CompChatColor of(@NonNull String string) {

		if (string.startsWith("#") && string.length() == 7) {

			// Default to white on ancient MC versions
			if (MinecraftVersion.olderThan(V.v1_7))
				return CompChatColor.WHITE;

			if (!MinecraftVersion.atLeast(V.v1_16)) {
				final Color color = getColorFromHex(string);

				return getClosestLegacyColor(color);
			}

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

	/*
	 * Parse the given HEX into a Java Color object
	 */
	private static Color getColorFromHex(String hex) {
		return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16));
	}

	/**
	 * Returns the closest legacy chat color from the given color.
	 *
	 * Uses all the available colors before HEX was added in MC 1.16.
	 *
	 * @param color
	 * @return
	 */
	public static CompChatColor getClosestLegacyColor(Color color) {
		if (MinecraftVersion.olderThan(V.v1_16)) {
			if (color.getAlpha() < 128)
				return null;

			int index = 0;
			double best = -1;

			for (int i = 0; i < LEGACY_COLORS.length; i++)
				if (areSimilar(LEGACY_COLORS[i], color))
					return CompChatColor.getColors().get(i);

			for (int i = 0; i < LEGACY_COLORS.length; i++) {
				final double distance = getDistance(color, LEGACY_COLORS[i]);

				if (distance < best || best == -1) {
					best = distance;
					index = i;
				}
			}

			return CompChatColor.getColors().get(index);
		}

		return CompChatColor.of(color);
	}

	/*
	 * Return if colors are nearly identical
	 */
	private static boolean areSimilar(Color first, Color second) {
		return Math.abs(first.getRed() - second.getRed()) <= 5 &&
				Math.abs(first.getGreen() - second.getGreen()) <= 5 &&
				Math.abs(first.getBlue() - second.getBlue()) <= 5;

	}

	/*
	 * Returns how different two colors are
	 */
	private static double getDistance(Color first, Color second) {
		final double rmean = (first.getRed() + second.getRed()) / 2.0;
		final double r = first.getRed() - second.getRed();
		final double g = first.getGreen() - second.getGreen();
		final int b = first.getBlue() - second.getBlue();

		final double weightR = 2 + rmean / 256.0;
		final double weightG = 4.0;
		final double weightB = 2 + (255 - rmean) / 256.0;

		return weightR * r * r + weightG * g * g + weightB * b * b;
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
