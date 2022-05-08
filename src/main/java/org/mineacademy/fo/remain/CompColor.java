package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
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
public final class CompColor {

	/**
	 * The list of all native values
	 */
	private static final List<CompColor> values = new ArrayList<>();

	/**
	 * The blue color
	 */
	public static final CompColor BLUE = new CompColor("BLUE", DyeColor.BLUE);

	/**
	 * The black color
	 */
	public static final CompColor BLACK = new CompColor("BLACK", DyeColor.BLACK);

	/**
	 * The dark aqua (cyan) color
	 */
	public static final CompColor DARK_AQUA = new CompColor("DARK_AQUA", DyeColor.CYAN);

	/**
	 * The dark blue color, called BLUE for dyecolor
	 */
	public static final CompColor DARK_BLUE = new CompColor("DARK_BLUE", DyeColor.BLUE);

	/**
	 * The aqua color (called light blue for dyecolor)
	 */
	public static final CompColor AQUA = new CompColor("AQUA", DyeColor.LIGHT_BLUE);

	/**
	 * The light gray color, previously silver (compatible for all MC versions)
	 */
	public static final CompColor GRAY = new CompColor("GRAY", getEnum("LIGHT_GRAY", "SILVER", DyeColor.class), null, "SILVER");

	/**
	 * The dark gray color, called gray for dyecolor
	 */
	public static final CompColor DARK_GRAY = new CompColor("DARK_GRAY", DyeColor.GRAY);

	/**
	 * The dark green color, called green for dyecolor
	 */
	public static final CompColor DARK_GREEN = new CompColor("DARK_GREEN", DyeColor.GREEN);

	/**
	 * The green color, called lime for dyecolor
	 */
	public static final CompColor GREEN = new CompColor("GREEN", DyeColor.LIME);

	/**
	 * The gold color, called orange for dyecolor
	 */
	public static final CompColor GOLD = new CompColor("GOLD", DyeColor.ORANGE);

	/**
	 * The brown color
	 * <p>
	 * NB: This color does not have a {@link ChatColor} alternative,
	 * so we give you GOLD chat color instead
	 */
	public static final CompColor BROWN = new CompColor("BROWN", DyeColor.BROWN, ChatColor.GOLD);

	/**
	 * The dark red color, called red for dyecolor
	 */
	public static final CompColor DARK_RED = new CompColor("DARK_RED", DyeColor.RED);

	/**
	 * The red color
	 */
	public static final CompColor RED = new CompColor("RED", DyeColor.RED);

	/**
	 * The white
	 */
	public static final CompColor WHITE = new CompColor("WHITE", DyeColor.WHITE);

	/**
	 * The yellow color
	 */
	public static final CompColor YELLOW = new CompColor("YELLOW", DyeColor.YELLOW);

	/**
	 * The dark purple color, called purple for dyecolor
	 */
	public static final CompColor DARK_PURPLE = new CompColor("DARK_PURPLE", DyeColor.PURPLE);

	/**
	 * The light purple color, called magenta for dyecolor
	 */
	public static final CompColor LIGHT_PURPLE = new CompColor("LIGHT_PURPLE", DyeColor.MAGENTA);

	/**
	 * The pink color
	 * <p>
	 * NB: This color does not have a {@link ChatColor} alternative,
	 * so we give you LIGHT_PURPLE chat color instead
	 */
	public static final CompColor PINK = new CompColor("PINK", DyeColor.PINK, ChatColor.LIGHT_PURPLE);

	/**
	 * The toString representation
	 */
	@Getter
	private final String name;

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

	/**
	 * The associated HEX color or null if not set
	 */
	private Color color;

	private CompColor(final Color color) {
		this(null, null, null);

		this.color = color;
	}

	private CompColor(final String name, final DyeColor dye) {
		this(name, dye, null);
	}

	private CompColor(final String name, final DyeColor dye, final ChatColor chatColor) {
		this(name, dye, chatColor, null);
	}

	private CompColor(final String name, final DyeColor dye, final ChatColor chatColor, final String legacyName) {
		this.name = name;
		this.dye = dye;
		this.chatColor = chatColor == null ? name != null ? ChatColor.valueOf(name) : ChatColor.WHITE : chatColor;
		this.legacyName = Common.getOrEmpty(legacyName);

		values.add(this);
	}

	/**
	 * Get the bukkit color
	 *
	 * @return
	 */
	public Color getColor() {
		return this.color != null ? this.color : this.dye.getColor();
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
	public static CompColor fromWoolData(final byte data) {
		return fromDye(DyeColor.getByWoolData(data));
	}

	/**
	 * Make new compatible dye from name
	 *
	 * @param color
	 * @return
	 */
	public static CompColor fromColor(Color color) {
		return fromName("#" + Integer.toHexString(color.asRGB()).substring(2));
	}

	/**
	 * Make new compatible dye from name. Name can also be a valid
	 * RGB: #123456
	 *
	 * @param name
	 * @return
	 */
	public static CompColor fromName(String name) {

		// Support HEX colors
		if (name.startsWith("#") && name.length() == 7)
			return new CompColor(Color.fromRGB(
					Integer.parseInt(name.substring(1, 3), 16),
					Integer.parseInt(name.substring(3, 5), 16),
					Integer.parseInt(name.substring(5, 7), 16)));

		name = name.toUpperCase();

		for (final CompColor comp : values())
			if (comp.chatColor.toString().equals(name) || comp.dye.toString().equals(name) || comp.legacyName.equals(name))
				return comp;

		throw new IllegalArgumentException("Could not get CompColor from name: " + name);
	}

	/**
	 * Make new compatible dye from bukkit dye
	 *
	 * @param dye
	 * @return
	 */
	public static CompColor fromDye(final DyeColor dye) {
		for (final CompColor comp : values())
			if (comp.dye == dye || comp.legacyName.equals(dye.toString()))
				return comp;

		throw new IllegalArgumentException("Could not get CompColor from DyeColor." + dye.toString());
	}

	/**
	 * Returns a {@link CompColor} from the given chat color
	 *
	 * @param color
	 * @return
	 */
	public static CompColor fromChatColor(final ChatColor color) {
		for (final CompColor comp : values())
			if (comp.chatColor == color || comp.legacyName.equals(color.toString()))
				return comp;

		throw new IllegalArgumentException("Could not get CompColor from ChatColor." + color.name());
	}

	/**
	 * Returns a {@link CompColor} from the given chat color
	 *
	 * @param color
	 * @return
	 */
	public static CompColor fromChatColor(final CompChatColor color) {
		for (final CompColor comp : values())
			if (comp.chatColor.name().equalsIgnoreCase(color.getName()) || comp.legacyName.equalsIgnoreCase(color.toString()))
				return comp;

		throw new FoException("Could not get CompColor from ChatColor." + color.getName());
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
	public static DyeColor toDye(final ChatColor color) {
		final CompColor c = fromName(color.name());

		return c != null ? c.getDye() : DyeColor.WHITE;
	}

	/**
	 * Convert a dye color into chat color
	 *
	 * @param dye
	 * @return
	 */
	public static ChatColor toColor(final DyeColor dye) {
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
	public static CompMaterial toConcrete(final ChatColor color) {
		final CompMaterial wool = toWool(color);

		return CompMaterial.fromString(wool.toString().replace("_WOOL", MinecraftVersion.olderThan(V.v1_12) ? "_STAINED_GLASS" : "_CONCRETE"));
	}

	/**
	 * Create colored wool from the given chat color
	 *
	 * @param color
	 * @return
	 */
	public static CompMaterial toWool(final ChatColor color) {
		final CompColor comp = fromChatColor(color);

		if (comp == AQUA)
			return CompMaterial.LIGHT_BLUE_WOOL;

		if (comp == BLACK)
			return CompMaterial.BLACK_WOOL;

		if (comp == BLUE)
			return CompMaterial.BLUE_WOOL;

		if (comp == BROWN)
			return CompMaterial.BROWN_WOOL;

		if (comp == DARK_AQUA)
			return CompMaterial.CYAN_WOOL;

		if (comp == DARK_BLUE)
			return CompMaterial.BLUE_WOOL;

		if (comp == DARK_GRAY)
			return CompMaterial.GRAY_WOOL;

		if (comp == DARK_GREEN)
			return CompMaterial.GREEN_WOOL;

		if (comp == DARK_PURPLE)
			return CompMaterial.PURPLE_WOOL;

		if (comp == DARK_RED)
			return CompMaterial.RED_WOOL;

		if (comp == GOLD)
			return CompMaterial.ORANGE_WOOL;

		if (comp == GRAY)
			return CompMaterial.LIGHT_GRAY_WOOL;

		if (comp == GREEN)
			return CompMaterial.LIME_WOOL;

		if (comp == LIGHT_PURPLE)
			return CompMaterial.MAGENTA_WOOL;

		if (comp == PINK)
			return CompMaterial.PINK_WOOL;

		if (comp == RED)
			return CompMaterial.RED_WOOL;

		if (comp == WHITE)
			return CompMaterial.WHITE_WOOL;

		if (comp == YELLOW)
			return CompMaterial.YELLOW_WOOL;

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
	public static List<ChatColor> getChatColors() {
		final List<ChatColor> list = new ArrayList<>();

		for (final ChatColor color : ChatColor.values())
			if (color.isColor() && !color.isFormat())
				list.add(color);

		return list;
	}

	// ----------------------------------------------------------------------------------------------------
	// Leftovers from when this class was an enum
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return all predefined colors
	 *
	 * @return
	 */
	public static CompColor[] values() {
		return values.toArray(new CompColor[values.size()]);
	}

	/**
	 * See {@link #fromName(String)}
	 *
	 * @param name
	 * @return
	 */
	public static CompColor valueOf(String name) {
		return fromName(name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}