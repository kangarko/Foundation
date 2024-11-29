package org.mineacademy.fo.model;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.RequiredArgsConstructor;

/**
 * Represents a simple toast message
 */
@RequiredArgsConstructor
public final class ToastMessage implements ConfigSerializable {

	/**
	 * The toast material (icon)
	 */
	private final CompMaterial icon;

	/**
	 * The toast style
	 */
	private final CompToastStyle style;

	/**
	 * The message to show
	 */
	private final String message;

	/**
	 * Displays this toast message to the given player
	 *
	 * @param player
	 */
	public void displayTo(Player player) {
		Remain.sendToast(player, CompChatColor.translateColorCodes(this.message), this.icon, this.style);
	}

	/**
	 * Displays this toast message to the given player
	 *
	 * @param player
	 * @param variableReplacer
	 */
	public void displayTo(Player player, Function<String, String> variableReplacer) {
		Remain.sendToast(player, variableReplacer.apply(this.message), this.icon, this.style);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.icon + " " + this.style + " " + " " + this.message;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.fromArray(
				"icon", this.icon.toString(),
				"style", this.style.toString(),
				"message", this.message);
	}

	/**
	 * Deserialize a new toast message
	 *
	 * @param map
	 * @return
	 */
	public static ToastMessage deserialize(SerializedMap map) {
		final CompMaterial icon = CompMaterial.fromString(map.getString("icon"));
		final CompToastStyle style = CompToastStyle.valueOf(map.getString("style"));
		final String message = map.getString("message");

		return new ToastMessage(icon, style, message);
	}
}