package org.mineacademy.fo.model;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple toast message
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
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
	@Getter
	private final SimpleComponent message;

	/**
	 * Displays this toast message to the given player
	 *
	 * @param player
	 */
	public void displayTo(Player player) {
		Remain.sendToast(player, this.message.toLegacy(), this.icon, this.style);
	}

	/**
	 * Displays this toast message to the given player
	 *
	 * @param player
	 * @param variableReplacer
	 */
	public void displayTo(Player player, Function<SimpleComponent, SimpleComponent> variableReplacer) {
		Remain.sendToast(player, variableReplacer.apply(this.message).toLegacy(), this.icon, this.style);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.icon + " " + this.style + " " + " " + this.message.toMini();
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"icon", this.icon.toString(),
				"style", this.style.toString(),
				"message", this.message.toMini());
	}

	public static ToastMessage deserialize(SerializedMap map) {
		final CompMaterial icon = CompMaterial.fromString(map.getString("icon"));
		final CompToastStyle style = CompToastStyle.valueOf(map.getString("style"));
		final SimpleComponent message = map.getComponent("message");

		return new ToastMessage(icon, style, message);
	}

	/**
	 * Create a new toast message
	 *
	 * @param material
	 * @param style
	 * @param message
	 * @return
	 */
	public static ToastMessage of(CompMaterial material, CompToastStyle style, String message) {
		return of(material, style, SimpleComponent.fromMini(message));
	}

	/**
	 * Create a new toast message
	 *
	 * @param material
	 * @param style
	 * @param component
	 * @return
	 */
	public static ToastMessage of(CompMaterial material, CompToastStyle style, SimpleComponent component) {
		return new ToastMessage(material, style, component);
	}
}