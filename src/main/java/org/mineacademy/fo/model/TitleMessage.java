package org.mineacademy.fo.model;

import org.bukkit.entity.Player;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple title message
 */
@RequiredArgsConstructor
public final class TitleMessage implements ConfigSerializable {

	/**
	 * title
	 */
	@Getter
	private final String titleMessage;

	/**
	 * title
	 */
	@Getter
	private final String subtitleMessage;

	/**
	 * fadeIn
	 */
	private final int fadeIn;

	/**
	 * stay
	 */
	private final int stay;

	/**
	 * stay
	 */
	private final int fadeOut;

	/**
	 * Displays this title message to the given player
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 */
	public void displayTo(Player player, String title, String subtitle) {
		Remain.sendTitle(player, this.fadeIn, this.stay, this.fadeOut, title, subtitle);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.titleMessage + " " + this.subtitleMessage + " " + this.fadeIn + " " + this.stay + " " + this.fadeOut;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"title", this.titleMessage,
				"subtitle", this.subtitleMessage,
				"fadeIn", this.fadeIn,
				"stay", this.stay,
				"fadeOut", this.fadeOut);
	}

	/**
	 * Create a new title message from the given map
	 *
	 * @param map
	 * @return
	 */
	public static TitleMessage deserialize(SerializedMap map) {
		String title = map.getString("title");
		String subtitle = map.getString("subtitle");
		int fadeIn = map.getInteger("fadeIn");
		int stay = map.getInteger("stay");
		int fadeOut = map.getInteger("fadeOut");

		return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
	}
}