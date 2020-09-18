package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
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
	GRAY(getEnum("LIGHT_GRAY", "SILVER", DyeColor.class), null, "SILVER"),

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
	 * <p>
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
	 * <p>
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

	private CompColor(final DyeColor dye) {
		this(dye, null);
	}

	private CompColor(final DyeColor dye, final ChatColor chatColor) {
		this(dye, chatColor, null);
	}

	private CompColor(final DyeColor dye, final ChatColor chatColor, final String legacyName) {
		this.dye = dye;
		this.chatColor = chatColor == null ? ChatColor.valueOf(toString()) : chatColor;
		this.legacyName = Common.getOrEmpty(legacyName);
	}

	// ----------------------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Attempts to lookup an enum by its primary name, if fails then by secondary
	 * name, if fails than returns null
	 *
	 * @param newName
	 * @param oldName
	 * @param clazz
	 * @return
	 */
	private static <T extends Enum<T>> T getEnum(final String newName, final String oldName, final Class<T> clazz) {
		T en = ReflectionUtil.lookupEnumSilent(clazz, newName);

		if (en == null)
			en = ReflectionUtil.lookupEnumSilent(clazz, oldName);

		return en;
	}

	/**
	 * Make new compatible dye from wool data
	 *
	 * @param data
	 * @return
	 */
	public static final CompColor fromWoolData(final byte data) {
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
	public static final CompColor fromDye(final DyeColor dye) {
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
	public static final CompColor fromChatColor(final ChatColor color) {
		for (final CompColor comp : values())
			if (comp.chatColor == color || comp.legacyName.equals(color.toString()))
				return comp;

		throw new FoException("Could not get CompColor from ChatColor." + color.name());
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
	public static final DyeColor toDye(final ChatColor color) {
		final CompColor c = ReflectionUtil.lookupEnumSilent(CompColor.class, color.name());

		return c != null ? c.getDye() : DyeColor.WHITE;
	}

	/**
	 * Convert a dye color into chat color
	 *
	 * @param dye
	 * @return
	 */
	public static final ChatColor toColor(final DyeColor dye) {
		for (final CompColor c : CompColor.values())
			if (c.getDye() == dye)
				return c.getChatColor();

		return ChatColor.WHITE;
	}

	/**
	 * Return a colored concrete (or wool if the current MC does not support it
	 *
	 * @param color
	 * @return
	 */
	public static final CompMaterial toConcrete(final ChatColor color) {
		final CompMaterial wool = toWool(color);

		return CompMaterial.fromString(wool.toString().replace("_WOOL", MinecraftVersion.olderThan(V.v1_12) ? "_STAINED_GLASS" : "_CONCRETE"));
	}

	/**
	 * Create colored wool from the given chat color
	 *
	 * @param color
	 * @return
	 */
	public static final CompMaterial toWool(final ChatColor color) {
		final CompColor comp = fromChatColor(color);

		switch (comp) {
			case AQUA:
				return CompMaterial.LIGHT_BLUE_WOOL;

			case BLACK:
				return CompMaterial.BLACK_WOOL;

			case BLUE:
				return CompMaterial.BLUE_WOOL;

			case BROWN:
				return CompMaterial.BROWN_WOOL;

			case DARK_AQUA:
				return CompMaterial.CYAN_WOOL;

			case DARK_BLUE:
				return CompMaterial.BLUE_WOOL;

			case DARK_GRAY:
				return CompMaterial.GRAY_WOOL;

			case DARK_GREEN:
				return CompMaterial.GREEN_WOOL;

			case DARK_PURPLE:
				return CompMaterial.PURPLE_WOOL;

			case DARK_RED:
				return CompMaterial.RED_WOOL;

			case GOLD:
				return CompMaterial.ORANGE_WOOL;

			case GRAY:
				return CompMaterial.LIGHT_GRAY_WOOL;

			case GREEN:
				return CompMaterial.LIME_WOOL;

			case LIGHT_PURPLE:
				return CompMaterial.MAGENTA_WOOL;

			case PINK:
				return CompMaterial.PINK_WOOL;

			case RED:
				return CompMaterial.RED_WOOL;

			case WHITE:
				return CompMaterial.WHITE_WOOL;

			case YELLOW:
				return CompMaterial.YELLOW_WOOL;
		}

		return CompMaterial.WHITE_WOOL;
	}

	// ----------------------------------------------------------------------------------------------------
	// Utils
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Get a list of all chat colors (only colors, NO formats like bold, reset etc.)
	 *
	 * @return
	 */
	public static final List<ChatColor> getChatColors() {
		final List<ChatColor> list = new ArrayList<>();

		for (final ChatColor color : ChatColor.values())
			if (color.isColor() && !color.isFormat())
				list.add(color);

		return list;
	}
}