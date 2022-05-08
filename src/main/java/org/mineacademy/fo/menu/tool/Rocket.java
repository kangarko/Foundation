package org.mineacademy.fo.menu.tool;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mineacademy.fo.Valid;

import lombok.Getter;

/**
 * A rocket is an extended {@link Tool}
 * that explodes when hit the ground.
 * <p>
 * Please use the onExplode method for calling
 * the explosion or call the {@link RocketExplosionEvent} manually.
 */
@Getter
public abstract class Rocket extends Tool {

	/**
	 * The projectile that is being shot
	 */
	private final Class<? extends Projectile> projectile;

	/**
	 * The flying speed with which we shot the projectile
	 */
	private final float flightSpeed;

	/**
	 * The explosion power, recommended: 2 - 15
	 */
	private final float explosionPower;

	/**
	 * Should the explosion break blocks?
	 */
	private final boolean breakBlocks;

	/**
	 * Create a new rocket with the flying speed to 1.5F (slightly above normal) and
	 * explosion power at 5F (TNT is 4F)
	 *
	 * @param projectile
	 * @param flightSpeed
	 */
	protected Rocket(Class<? extends Projectile> projectile) {
		this(projectile, 1.5F);
	}

	/**
	 * Create a new rocket with the explosion power of 5F (TNT is 4F)
	 *
	 * @param projectile
	 * @param flightSpeed
	 */
	protected Rocket(Class<? extends Projectile> projectile, float flightSpeed) {
		this(projectile, flightSpeed, 5F);
	}

	/**
	 * Create a new rocket that destroys blocks
	 *
	 * @param projectile
	 * @param flightSpeed
	 * @param explosionPower
	 */
	protected Rocket(Class<? extends Projectile> projectile, float flightSpeed, float explosionPower) {
		this(projectile, flightSpeed, explosionPower, true);
	}

	/**
	 * Create a new rocket with the given projectile and its speed (1=normal, 5=insane, 10=max,buggy)
	 * as well as the explosion power (1-30 although it bugs over 15 already) and if it should break blocks
	 * <p>
	 * For explosion powers see https://minecraft.gamepedia.com/Explosion
	 *
	 * @param projectile
	 * @param flightSpeed
	 */
	protected Rocket(Class<? extends Projectile> projectile, float flightSpeed, float explosionPower, boolean breakBlocks) {
		Valid.checkBoolean(flightSpeed <= 10F, "Rocket cannot have speed over 10");
		Valid.checkBoolean(explosionPower <= 30F, "Rocket cannot have explosion power over 30");

		this.projectile = projectile;
		this.flightSpeed = flightSpeed;
		this.explosionPower = explosionPower;
		this.breakBlocks = breakBlocks;
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
	protected void onLaunch(Projectile projectile, Player shooter) {
	}

	/**
	 * From when you launch the projectile till the moment it hits the ground
	 * there is a timer task going on for each server tick calling this method
	 * <p>
	 * TIP: You can spawn special flying particles here
	 *
	 * @param projectile
	 * @param shooter
	 */
	protected void onFlyTick(Projectile projectile, Player shooter) {
	}

	/**
	 * Check if the rocket can explode, if false we simply remove the entity
	 *
	 * @param projectile
	 * @param shooter
	 * @return
	 */
	protected boolean canExplode(Projectile projectile, Player shooter) {
		return true;
	}

	/**
	 * Called automatically when this rocket hits the ground.
	 * <p>
	 * TIP: Call {@link #explode(Projectile, float, boolean)}
	 *
	 * @param projectile
	 * @param shooter
	 * @param location
	 */
	protected void onExplode(Projectile projectile, Player shooter) {
	}

	/**
	 * Also shoot rockets when clicking the air (Bukkit cancels the event so)
	 *
	 * @return true
	 */
	@Override
	protected boolean ignoreCancelled() {
		return false;
	}

	/**
	 * Automatically cancel the click event to launch the rocket
	 */
	@Override
	protected boolean autoCancel() {
		return true;
	}
}
