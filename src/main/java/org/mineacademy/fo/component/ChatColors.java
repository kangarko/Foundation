package org.mineacademy.fo.component;

import com.google.common.base.Preconditions;
import lombok.Getter;

import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ChatColors {


	/**
	 * The special character which prefixes all chat colour codes. Use this if
	 * you need to dynamically convert colour codes from your custom format.
	 */
	public static final char COLOR_CHAR = '\u00A7';
	public static final String[] ALL_CODES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "a", "B", "b", "C", "c", "D", "d", "E", "e", "F", "f", "K", "k", "L", "l", "M", "m", "N", "n", "O", "o", "R", "r", "X", "x"};
	/**
	 * Pattern to remove all colour codes.
	 */
	public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + String.valueOf(COLOR_CHAR) + "[0-9A-FK-ORX]");
	/**
	 * Colour instances keyed by their active character.
	 */
	private static final Map<Character, ChatColors> BY_CHAR = new HashMap<>();
	/**
	 * Colour instances keyed by their name.
	 */
	private static final Map<String, ChatColors> BY_NAME = new HashMap<>();
	/**
	 * Represents black.
	 */
	public static final ChatColors BLACK = new ChatColors('0', "black", new Color(0x000000));
	/**
	 * Represents dark blue.
	 */
	public static final ChatColors DARK_BLUE = new ChatColors('1', "dark_blue", new Color(0x0000AA));
	/**
	 * Represents dark green.
	 */
	public static final ChatColors DARK_GREEN = new ChatColors('2', "dark_green", new Color(0x00AA00));
	/**
	 * Represents dark blue (aqua).
	 */
	public static final ChatColors DARK_AQUA = new ChatColors('3', "dark_aqua", new Color(0x00AAAA));
	/**
	 * Represents dark red.
	 */
	public static final ChatColors DARK_RED = new ChatColors('4', "dark_red", new Color(0xAA0000));
	/**
	 * Represents dark purple.
	 */
	public static final ChatColors DARK_PURPLE = new ChatColors('5', "dark_purple", new Color(0xAA00AA));
	/**
	 * Represents gold.
	 */
	public static final ChatColors GOLD = new ChatColors('6', "gold", new Color(0xFFAA00));
	/**
	 * Represents gray.
	 */
	public static final ChatColors GRAY = new ChatColors('7', "gray", new Color(0xAAAAAA));
	/**
	 * Represents dark gray.
	 */
	public static final ChatColors DARK_GRAY = new ChatColors('8', "dark_gray", new Color(0x555555));
	/**
	 * Represents blue.
	 */
	public static final ChatColors BLUE = new ChatColors('9', "blue", new Color(0x5555FF));
	/**
	 * Represents green.
	 */
	public static final ChatColors GREEN = new ChatColors('a', "green", new Color(0x55FF55));
	/**
	 * Represents aqua.
	 */
	public static final ChatColors AQUA = new ChatColors('b', "aqua", new Color(0x55FFFF));
	/**
	 * Represents red.
	 */
	public static final ChatColors RED = new ChatColors('c', "red", new Color(0xFF5555));
	/**
	 * Represents light purple.
	 */
	public static final ChatColors LIGHT_PURPLE = new ChatColors('d', "light_purple", new Color(0xFF55FF));
	/**
	 * Represents yellow.
	 */
	public static final ChatColors YELLOW = new ChatColors('e', "yellow", new Color(0xFFFF55));
	/**
	 * Represents white.
	 */
	public static final ChatColors WHITE = new ChatColors('f', "white", new Color(0xFFFFFF));
	/**
	 * Represents magical characters that change around randomly.
	 */
	public static final ChatColors MAGIC = new ChatColors('k', "obfuscated");
	/**
	 * Makes the text bold.
	 */
	public static final ChatColors BOLD = new ChatColors('l', "bold");
	/**
	 * Makes a line appear through the text.
	 */
	public static final ChatColors STRIKETHROUGH = new ChatColors('m', "strikethrough");
	/**
	 * Makes the text appear underlined.
	 */
	public static final ChatColors UNDERLINE = new ChatColors('n', "underline");
	/**
	 * Makes the text italic.
	 */
	public static final ChatColors ITALIC = new ChatColors('o', "italic");
	/**
	 * Resets all previous chat colors or formats.
	 */
	public static final ChatColors RESET = new ChatColors('r', "reset");
	/**
	 * Count used for populating legacy ordinal.
	 */
	private static int count = 0;
	/**
	 * This colour's colour char prefixed by the {@link #COLOR_CHAR}.
	 */
	private final String toString;
	@Getter
	private final String name;
	/**
	 * The RGB color of the ChatColors. null for non-colors (formatting)
	 */
	@Getter
	private final Color color;
	@Getter
	private final char code;

	public ChatColors(char code, String name) {
		this(code, name, null);
	}

	public ChatColors(char code, String name, Color color) {
		this.name = name;
		this.toString = new String(new char[]{
				COLOR_CHAR, code
		});
		this.color = color;
		this.code = code;

		BY_CHAR.put(code, this);
		BY_NAME.put(name.toUpperCase(Locale.ROOT), this);
	}

	private ChatColors(String name, String toString, int rgb) {
		this.name = name;
		this.toString = toString;
		this.color = new Color(rgb);
		this.code = ' ';
	}

	/**
	 * Get the colour represented by the specified code.
	 *
	 * @param code the code to search for
	 * @return the mapped colour, or null if non exists
	 */
	public static ChatColors getByChar(char code) {
		return BY_CHAR.get(code);
	}

	public static ChatColors of(Color color) {
		return of("#" + String.format("%08x", color.getRGB()).substring(2));
	}

	public static ChatColors of(String string) {
		Preconditions.checkArgument(string != null, "string cannot be null");
		if (string.startsWith("#") && string.length() == 7) {
			int rgb;
			try {
				rgb = Integer.parseInt(string.substring(1), 16);
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Illegal hex string " + string);
			}

			StringBuilder magic = new StringBuilder(COLOR_CHAR + "x");
			for (char c : string.substring(1).toCharArray()) {
				magic.append(COLOR_CHAR).append(c);
			}

			return new ChatColors(string, magic.toString(), rgb);
		}

		ChatColors defined = BY_NAME.get(string.toUpperCase(Locale.ROOT));
		if (defined != null) {
			return defined;
		}

		throw new IllegalArgumentException("Could not parse ChatColors " + string);
	}

	@Override
	public String toString() {
		return toString;
	}

}
