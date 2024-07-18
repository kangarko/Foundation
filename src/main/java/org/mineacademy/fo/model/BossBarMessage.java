package org.mineacademy.fo.model;

import java.util.function.Function;

import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * Represents a simple boss bar message
 */
@RequiredArgsConstructor
public final class BossBarMessage implements ConfigSerializable {

	/**
	 * The bar color
	 */
	private final BossBar.Color color;

	/**
	 * The bar style
	 */
	private final BossBar.Overlay overlay;

	/**
	 * Seconds to show this bar
	 */
	private final int seconds;

	/**
	 * The percentage of this bar
	 */
	private final float progress;

	/**
	 * The message to show
	 */
	@Getter
	private final Component message;

	/**
	 * Displays this boss bar to the given player
	 *
	 * @param audience
	 */
	public void displayTo(Audience audience) {
		this.displayTo(audience, Function.identity());
	}

	/**
	 * Displays this boss bar to the given player
	 *
	 * @param audience
	 * @param editBeforeDisplay
	 */
	public void displayTo(Audience audience, Function<Component, Component> editBeforeDisplay) {
		Remain.sendBossbarTimed(audience, editBeforeDisplay.apply(this.message), this.seconds, this.progress, this.color, this.overlay);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.color + " " + this.overlay + " " + this.seconds + " " + this.message;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Color", this.color,
				"Style", this.overlay,
				"Seconds", this.seconds,
				"Message", this.message);
	}

	public static BossBarMessage deserialize(SerializedMap map) {
		final BossBar.Color color = map.get("Color", BossBar.Color.class);
		final BossBar.Overlay overlay = ReflectionUtil.lookupEnum(BossBar.Overlay.class, map.getString("Style"));
		final int seconds = map.getInteger("Seconds");
		final float progress = map.getFloat("Progress", 1F);
		final Component message = map.get("Message", Component.class);

		return new BossBarMessage(color, overlay, seconds, progress, message);
	}
}