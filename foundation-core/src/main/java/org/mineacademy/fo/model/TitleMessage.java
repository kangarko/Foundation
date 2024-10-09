package org.mineacademy.fo.model;

import java.util.function.Function;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.platform.FoundationPlayer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple title message.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TitleMessage implements ConfigSerializable {

	/**
	 * The title message.
	 */
	private final SimpleComponent titleMessage;

	/**
	 * The subtitle message.
	 */
	private final SimpleComponent subtitleMessage;

	/**
	 * How long is the fade-in animation, in ticks.
	 */
	private final int fadeIn;

	/**
	 * How long the title will stay, in ticks.
	 */
	private final int stay;

	/**
	 * How long is the fade-out animation, in ticks.
	 */
	private final int fadeOut;

	/**
	 * Displays this title message to the given audience.
	 *
	 * @param audience
	 */
	public void displayTo(FoundationPlayer audience) {
		audience.sendTitle(this.fadeIn, this.stay, this.fadeOut, this.titleMessage, this.subtitleMessage);
	}

	/**
	 * Displays this title message to the given audience.
	 *
	 * @param audience
	 * @param variablesReplacer
	 */
	public void displayTo(FoundationPlayer audience, Function<SimpleComponent, SimpleComponent> variablesReplacer) {
		audience.sendTitle(this.fadeIn, this.stay, this.fadeOut, variablesReplacer.apply(this.titleMessage), variablesReplacer.apply(this.subtitleMessage));
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.titleMessage + " " + this.subtitleMessage + " " + this.fadeIn + " " + this.stay + " " + this.fadeOut;
	}

	/**
	 * Converts this title message to a map you can save to a file.
	 *
	 * @return
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"title", this.titleMessage.toMini(),
				"subtitle", this.subtitleMessage.toMini(),
				"fadeIn", this.fadeIn,
				"stay", this.stay,
				"fadeOut", this.fadeOut);
	}

	/**
	 * Create a new title message from the given map.
	 *
	 * @param map
	 * @return
	 */
	public static TitleMessage deserialize(SerializedMap map) {
		final SimpleComponent title = map.getComponent("title");
		final SimpleComponent subtitle = map.getComponent("subtitle");
		final int fadeIn = map.getInteger("fadeIn");
		final int stay = map.getInteger("stay");
		final int fadeOut = map.getInteger("fadeOut");

		return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
	}

	/**
	 * Create a new title message.
	 *
	 * @param title
	 * @param subtitle
	 * @param fadeIn
	 * @param stay
	 * @param fadeOut
	 * @return
	 */
	public static TitleMessage of(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		return of(SimpleComponent.fromMini(title), SimpleComponent.fromMini(subtitle), fadeIn, stay, fadeOut);
	}

	/**
	 * Create a new title message.
	 *
	 * @param title
	 * @param subtitle
	 * @param fadeIn
	 * @param stay
	 * @param fadeOut
	 * @return
	 */
	public static TitleMessage of(SimpleComponent title, SimpleComponent subtitle, int fadeIn, int stay, int fadeOut) {
		return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
	}
}