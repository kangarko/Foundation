package org.mineacademy.fo.remain;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.FoException;

import lombok.Getter;

/**
 * A utility class enabling you to convert between {@link DyeColor} and {@link ChatColor} with ease
 */
public enum CompColor {

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
	GRAY(ReflectionUtil.getEnum("LIGHT_GRAY", "SILVER", DyeColor.class), null, "SILVER"),

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
	@Getter
	private final DyeColor dye;

	/**
	 * The {@link ChatColor} representation
	 */
	@Getter
	private final ChatColor chatColor;

	/**
	 * The legacy dye/chat color name, or null if none
	 */
	private final String legacyName;

	private CompColor(DyeColor dye) {
		this(dye, null);
	}

	private CompColor(DyeColor dye, ChatColor chatColor) {
		this(dye, chatColor, null);
	}

	private CompColor(DyeColor dye, ChatColor chatColor, String legacyName) {
		this.dye = dye;
		this.chatColor = chatColor == null ? ChatColor.valueOf(toString()) : chatColor;
		this.legacyName = Common.getOrEmpty(legacyName);
	}

	// ----------------------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Make new compatible dye from wool data
	 *
	 * @param data
	 * @return
	 */
	public static final CompColor fromWoolData(byte data) {
		return fromDye(DyeColor.getByWoolData(data));
	}

	/**
	 * Make new compatible dye from name
	 *
	 * @param name
	 * @return
	 */
	public static final CompColor fromName(String name) {
		name = name.toUpperCase();

		for (final CompColor comp : values())
			if (comp.chatColor.toString().equals(name) || comp.dye.toString().equals(name) || comp.legacyName.equals(name))
				return comp;

		throw new FoException("Could not get CompColor from name: " + name);
	}

	/**
	 * Make new compatible dye from bukkit dye
	 *
	 * @param dye
	 * @return
	 */
	public static final CompColor fromDye(DyeColor dye) {
		for (final CompColor comp : values())
			if (comp.dye == dye || comp.legacyName.equals(dye.toString()))
				return comp;

		throw new FoException("Could not get CompColor from DyeColor." + dye.toString());
	}

	/**
	 * Returns a {@link CompDye} from the given chat color
	 *
	 * @param color
	 * @return
	 */
	public static final CompColor fromChatColor(ChatColor color) {
		for (final CompColor comp : values())
			if (comp.chatColor == color || comp.legacyName.equals(color.toString()))
				return comp;

		throw new FoException("Could not get CompColor from ChatColor." + color.toString());
	}

	// ----------------------------------------------------------------------------------------------------
	// Converters
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Convert a chat color into dye color
	 *
	 * @param color
	 * @return
	 */
	public static final DyeColor toDye(ChatColor color) {
		final CompColor c = ReflectionUtil.lookupEnumSilent(CompColor.class, color.name());

		return c != null ? c.getDye() : DyeColor.WHITE;
	}

	/**
	 * Convert a dye color into chat color
	 *
	 * @param dye
	 * @return
	 */
	public static final ChatColor toColor(DyeColor dye) {
		for (final CompColor c : CompColor.values())
			if (c.getDye() == dye)
				return c.getChatColor();

		return ChatColor.WHITE;
	}
}