package org.mineacademy.fo.event;

import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.menu.tool.Rocket;

import lombok.Getter;
import lombok.Setter;

/**
 * The event that is triggered when a {@link Rocket} explodes.
 */
@Getter
public final class RocketExplosionEvent extends SimpleEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The rocket
	 */
	private final Rocket rocket;

	/**
	 * The projectile
	 */
	private final Projectile projectile;

	/**
	 * The power of this explosion
	 */
	@Setter
	private float power;

	/**
	 * Should this explosion break blocks?
	 */
	@Setter
	private boolean breakBlocks;

	/**
	 * Is the event cancelled?
	 */
	@Setter
	private boolean cancelled;

	public RocketExplosionEvent(Rocket rocket, Projectile projectile, float power, boolean breakBlocks) {
		this.rocket = rocket;
		this.projectile = projectile;
		this.power = power;
		this.breakBlocks = breakBlocks;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}