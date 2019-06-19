package org.mineacademy.fo.model;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.mineacademy.fo.ReflectionUtil;

import lombok.Getter;

/**
 * A utility class enabling you to convert between {@link DyeColor} and {@link ChatColor} with ease
 */
@Getter
public enum ColorConverter {

	/**
	 * The blue color
	 */
	BLUE(DyeColor.BLUE),

	/**
	 * The black color
	 */
	BLACK(DyeColor.BLACK),

	/**
	 * The dark aqua (cyan) color
	 */
	DARK_AQUA(DyeColor.CYAN),

	/**
	 * The dark blue color, called BLUE for dyecolor
	 */
	DARK_BLUE(DyeColor.BLUE),

	/**
	 * The aqua color (called light blue for dyecolor)
	 */
	AQUA(DyeColor.LIGHT_BLUE),

	/**
	 * The light gray color, previously silver (compatible for all MC versions)
	 */
	GRAY(ReflectionUtil.getEnum("LIGHT_GRAY", "SILVER", DyeColor.class)),

	/**
	 * The dark gray color, called gray for dyecolor
	 */
	DARK_GRAY(DyeColor.GRAY),

	/**
	 * The dark green color, called green for dyecolor
	 */
	DARK_GREEN(DyeColor.GREEN),

	/**
	 * The green color, called lime for dyecolor
	 */
	GREEN(DyeColor.LIME),

	/**
	 * The gold color, called orange for dyecolor
	 */
	GOLD(DyeColor.ORANGE),

	/**
	 * The brown color
	 *
	 * NB: This color does not have a {@link ChatColor} alternative,
	 * so we give you GOLD chat color instead
	 */
	BROWN(DyeColor.BROWN, ChatColor.GOLD),

	/**
	 * The dark red color, called red for dyecolor
	 */
	DARK_RED(DyeColor.RED),

	/**
	 * The red color
	 */
	RED(DyeColor.RED),

	/**
	 * The white
	 */
	WHITE(DyeColor.WHITE),

	/**
	 * The yellow color
	 */
	YELLOW(DyeColor.YELLOW),

	/**
	 * The dark purple color, called purple for dyecolor
	 */
	DARK_PURPLE(DyeColor.PURPLE),

	/**
	 * The light purple color, called magenta for dyecolor
	 */
	LIGHT_PURPLE(DyeColor.MAGENTA),

	/**
	 * The pink color
	 *
	 * NB: This color does not have a {@link ChatColor} alternative,
	 * so we give you LIGHT_PURPLE chat color instead
	 */
	PINK(DyeColor.PINK, ChatColor.LIGHT_PURPLE);

	/**
	 * The {@link DyeColor} representation
	 */
	private final DyeColor dye;

	/**
	 * The {@link ChatColor} representation
	 */
	private final ChatColor chatColor;

	private ColorConverter(DyeColor dye) {
		this(dye, null);
	}

	private ColorConverter(DyeColor dye, ChatColor chatColor) {
		this.dye = dye;
		this.chatColor = chatColor == null ? ChatColor.valueOf(toString()) : chatColor;
	}

	// ----------------------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Convert a chat color into dye color
	 *
	 * @param color
	 * @return
	 */
	public static final DyeColor toDye(ChatColor color) {
		final ColorConverter c = ReflectionUtil.lookupEnumSilent(ColorConverter.class, color.name());

		return c != null ? c.getDye() : DyeColor.WHITE;
	}

	/**
	 * Convert a dye color into chat color
	 *
	 * @param dye
	 * @return
	 */
	public static final ChatColor toColor(DyeColor dye) {
		for (final ColorConverter c : ColorConverter.values())
			if (c.getDye() == dye)
				return c.getChatColor();

		return ChatColor.WHITE;
	}
}