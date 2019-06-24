package org.mineacademy.fo.remain;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ColorConverter;

import lombok.Getter;

/**
 * Wrapper for {@link DyeColor}
 */
public enum CompDye {

	WHITE,
	ORANGE,
	MAGENTA,
	LIGHT_BLUE,
	YELLOW,
	LIME,
	PINK,
	GRAY,
	LIGHT_GRAY("SILVER"),
	CYAN,
	PURPLE,
	BLUE,
	BROWN,
	GREEN,
	RED,
	BLACK;

	/**
	 * The legacy name
	 */
	private final String legacyName;

	/**
	 * The current DyeColor class
	 */
	@Getter
	private final DyeColor dye;

	/**
	 * Make a new CompDye without a legacy name
	 */
	private CompDye() {
		this(null);
	}

	/**
	 * Make a new CompDye with a legacy name
	 *
	 * @param name
	 */
	private CompDye(String name) {
		this.legacyName = name;

		DyeColor color;

		try {
			color = DyeColor.valueOf(name());

		} catch (final IllegalArgumentException ex) {
			if (name == null)
				throw new FoException("Missing legacy name for DyeColor." + name());

			color = DyeColor.valueOf(name);
		}

		if (color == null)
			throw new FoException("Failed to resolve DyeColor." + name());
		this.dye = color;
	}

	// ------------------------------------------------------------------------------------
	// Static access
	// ------------------------------------------------------------------------------------

	/**
	 * Make new compatible dye from wool data
	 *
	 * @param data
	 * @return
	 */
	public static final CompDye fromWoolData(byte data) {
		return fromDye(DyeColor.getByWoolData(data));
	}

	/**
	 * Make new compatible dye from name
	 *
	 * @param name
	 * @return
	 */
	public static final CompDye fromName(String name) {
		for (final CompDye comp : values())
			if (comp.toString().equalsIgnoreCase(name) || comp.legacyName.equalsIgnoreCase(name))
				return comp;

		throw new FoException("Could not get CompDye from name: " + name);
	}

	/**
	 * Make new compatible dye from bukkit dye
	 *
	 * @param dye
	 * @return
	 */
	public static final CompDye fromDye(DyeColor dye) {
		for (final CompDye comp : values())
			if (comp.getDye() == dye)
				return comp;

		throw new FoException("Could not get CompDye from DyeColor." + dye.toString());
	}

	/**
	 * Returns a {@link CompDye} from the given chat color
	 *
	 * @param color
	 * @return
	 */
	public static final CompDye fromChatColor(ChatColor color) {
		return fromDye(ColorConverter.toDye(color));
	}
}