package org.mineacademy.fo.model;

import java.util.function.Function;

import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.platform.FoundationPlayer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;

/**
 * Represents a simple boss bar message.
 */
@Getter
@RequiredArgsConstructor
public final class BossBarMessage implements ConfigSerializable {

	/**
	 * The bar color.
	 */
	private final BossBar.Color color;

	/**
	 * The bar style.
	 */
	private final BossBar.Overlay overlay;

	/**
	 * Seconds to show this bar.
	 */
	private final int seconds;

	/**
	 * The percentage of this bar.
	 */
	private final float progress;

	/**
	 * The message to show.
	 */
	private final SimpleComponent message;

	/**
	 * Displays this boss bar to the given player.
	 *
	 * @param audience
	 */
	public void displayTo(FoundationPlayer audience) {
		audience.sendBossbarTimed(this.message, this.seconds, this.progress, this.color, this.overlay);
	}

	/**
	 * Displays this boss bar to the given player.
	 *
	 * @param audience
	 * @param messageEditor to replace variables in the message
	 */
	public void displayTo(FoundationPlayer audience, Function<SimpleComponent, SimpleComponent> messageEditor) {
		audience.sendBossbarTimed(messageEditor.apply(this.message), this.seconds, this.progress, this.color, this.overlay);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.color + " " + this.overlay + " " + this.seconds + " " + this.message;
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Color", this.color,
				"Style", this.overlay,
				"Seconds", this.seconds,
				"Message", this.message);
	}

	/**
	 * @see SerializeUtilCore#deserialize(org.mineacademy.fo.SerializeUtilCore.Language, Class, Object, Object...)
	 *
	 * @param map
	 * @return
	 */
	public static BossBarMessage deserialize(SerializedMap map) {
		final BossBar.Color color = map.get("Color", BossBar.Color.class);
		final BossBar.Overlay overlay = ReflectionUtil.lookupEnum(BossBar.Overlay.class, map.getString("Style"));
		final int seconds = map.getInteger("Seconds");
		final float progress = map.getFloat("Progress", 1F);
		final SimpleComponent message = map.getComponent("Message");

		return new BossBarMessage(color, overlay, seconds, progress, message);
	}
}