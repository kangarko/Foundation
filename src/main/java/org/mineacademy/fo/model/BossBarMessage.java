package org.mineacademy.fo.model;

import org.bukkit.entity.Player;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompBarColor;
import org.mineacademy.fo.remain.CompBarStyle;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple boss bar message
 */
@RequiredArgsConstructor
public final class BossBarMessage implements ConfigSerializable {

	/**
	 * The bar color
	 */
	private final CompBarColor color;

	/**
	 * The bar style
	 */
	private final CompBarStyle style;

	/**
	 * Seconds to show this bar
	 */
	private final int seconds;

	/**
	 * The message to show
	 */
	@Getter
	private final String message;

	/**
	 * Displays this boss bar to the given player
	 *
	 * @param player
	 * @param message replace variables here
	 */
	public void displayTo(Player player, String message) {
		Remain.sendBossbarTimed(player, message, this.seconds, this.color, this.style);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.color + " " + this.style + " " + this.seconds + " " + this.message;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Color", this.color,
				"Style", this.style,
				"Seconds", this.seconds,
				"Message", this.message);
	}

	public static BossBarMessage deserialize(SerializedMap map) {
		CompBarColor color = CompBarColor.valueOf(map.getString("Color"));
		CompBarStyle style = CompBarStyle.valueOf(map.getString("Style"));
		int seconds = map.getInteger("Seconds");
		String message = map.getString("Message");

		return new BossBarMessage(color, style, seconds, message);
	}
}