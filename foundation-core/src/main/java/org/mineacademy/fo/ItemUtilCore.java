
package org.mineacademy.fo;

import org.mineacademy.fo.remain.CompChatColor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for managing items.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class ItemUtilCore {

	// ----------------------------------------------------------------------------------------------------
	// Enumeration - fancy names
	// ----------------------------------------------------------------------------------------------------

	/**
	 * See {@link #bountify(String)}
	 *
	 * @param color
	 * @return
	 */
	public static String bountify(@NonNull CompChatColor color) {
		return bountify(color.getName());
	}

	/**
	 * Removes _ from the enum, lowercases everything and finally capitalizes it
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountify(@NonNull Enum<?> enumeration) {
		return bountify(enumeration.toString());
	}

	/**
	 * Lowercases the given name and replaces _ with spaces
	 *
	 * @param name
	 * @return
	 */
	public static String bountify(@NonNull String name) {
		return ChatUtil.capitalizeFully(name.toLowerCase().replace("_", " "));
	}
}