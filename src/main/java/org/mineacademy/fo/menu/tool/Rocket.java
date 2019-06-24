package org.mineacademy.fo.menu.tool;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.event.RocketExplosionEvent;

/**
 * A rocket is an extended {@link Tool}
 * that explodes when hit the ground.
 *
 * Please use {@link #explode(Projectile, Location, float, boolean)} for calling
 * the explosion or call the {@link RocketExplosionEvent} manually.
 *
 */
public abstract class Rocket extends Tool {

	/**
	 * Create a new explosion, firing {@link RocketExplosionEvent}
	 * and then creating explosion using the Bukkit API
	 *
	 * @param projectile
	 * @param location
	 * @param power
	 * @param breakBlocks
	 */
	protected final void explode(Projectile projectile, Location location, float power, boolean breakBlocks) {
		final RocketExplosionEvent event = new RocketExplosionEvent(this, projectile, location, power, breakBlocks);

		if (Common.callEvent(event))
			projectile.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), event.getPower(), false, event.isBreakBlocks());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onBlockClick(PlayerInteractEvent e) {
	}

	/**
	 * Return true if this rocket can be shot in the given location.
	 * We use this for example to check if the shooter player is within an arena
	 *
	 * @param shooter
	 * @param location
	 * @return
	 */
	protected boolean canLaunch(Player shooter, Location location) {
		return true;
	}

	/**
	 * Called automatically when this rocket is being shot
	 *
	 * @param projectile
	 * @param shooter
	 */
	protected abstract void onLaunch(Projectile projectile, Player shooter);

	/**
	 * Called automatically when this rocket hits the ground
	 *
	 * @param projectile
	 * @param shooter
	 * @param location
	 */
	protected abstract void onHit(Projectile projectile, Player shooter, Location location);
}
