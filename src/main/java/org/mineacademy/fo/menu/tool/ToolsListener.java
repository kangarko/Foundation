package org.mineacademy.fo.menu.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.event.RocketExplosionEvent;
import org.mineacademy.fo.remain.Remain;

import lombok.Data;

/**
 * The event listener class responsible for firing events in tools
 */
public final class ToolsListener implements Listener {

	/**
	 * Stores rockets that were shot
	 */
	private final Map<UUID, ShotRocket> shotRockets = new HashMap<>();

	/**
	 * Represents a shot rocket with the shooter
	 */
	@Data
	private final class ShotRocket {
		private final Player shooter;
		private final Rocket rocket;
	}

	// -------------------------------------------------------------------------------------------
	// Main tool listener
	// -------------------------------------------------------------------------------------------

	/**
	 * Handles clicking tools and shooting rocket
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onToolClick(PlayerInteractEvent event) {
		if (!Remain.isInteractEventPrimaryHand(event))
			return;

		final Player player = event.getPlayer();
		final Tool tool = ToolRegistry.getTool(player.getItemInHand());

		if (tool != null)
			try {
				if ((event.isCancelled() || !event.hasBlock()) && tool.ignoreCancelled())
					return;

				if (tool instanceof Rocket) {
					final Rocket rocket = (Rocket) tool;

					if (rocket.canLaunch(player, player.getEyeLocation()))
						player.launchProjectile(rocket.getProjectile(), player.getEyeLocation().getDirection().multiply(rocket.getFlightSpeed()));
					else
						event.setCancelled(true);

				} else
					tool.onBlockClick(event);

				if (tool.autoCancel())
					event.setCancelled(true);

			} catch (final Throwable t) {
				event.setCancelled(true);

				Common.tell(player, "&cOups! There was a problem with this tool! Please contact the administrator to review the console for details.");
				Common.error(t, "Failed to handle " + event.getAction() + " using Tool: " + tool.getClass());
			}
	}

	/**
	 * Handles hotbar focus/defocus for tools
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHeltItem(PlayerItemHeldEvent e) {
		final Player pl = e.getPlayer();

		final Tool curr = ToolRegistry.getTool(pl.getInventory().getItem(e.getNewSlot()));
		final Tool prev = ToolRegistry.getTool(pl.getInventory().getItem(e.getPreviousSlot()));

		// Player has attained focus
		if (curr != null) {

			if (prev != null) {

				// Not really
				if (prev.equals(curr))
					return;

				prev.onHotbarDefocused(pl);
			}

			curr.onHotbarFocused(pl);
		}
		// Player lost focus
		else if (prev != null)
			prev.onHotbarDefocused(pl);
	}

	// -------------------------------------------------------------------------------------------
	// Rockets
	// -------------------------------------------------------------------------------------------

	/**
	 * Handles launching a rocket
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRocketShoot(ProjectileLaunchEvent event) {
		final Projectile shot = event.getEntity();
		final Object /* 1.6.4 Comp */ shooter = shot.getShooter();

		if (!(shooter instanceof Player))
			return;

		if (shotRockets.containsKey(shot.getUniqueId()))
			return;

		final Player player = (Player) shooter;
		final Tool tool = ToolRegistry.getTool(player.getItemInHand());

		if (tool != null && tool instanceof Rocket)
			try {
				final Rocket rocket = (Rocket) tool;

				if (event.isCancelled() && tool.ignoreCancelled())
					return;

				if (!rocket.canLaunch(player, shot.getLocation())) {
					event.setCancelled(true);

					return;
				}

				if (tool.autoCancel() || shot instanceof EnderPearl) {
					final World world = shot.getWorld();
					final Location loc = shot.getLocation();

					Common.runLater(() -> shot.remove());

					Common.runLater(1, () -> {
						Valid.checkNotNull(shot, "shot = null");
						Valid.checkNotNull(world, "shot.world = null");
						Valid.checkNotNull(loc, "shot.location = null");

						final Location directedLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().setY(0).normalize().multiply(1.05)).add(0, 0.2, 0);

						final Projectile copy = world.spawn(directedLoc, shot.getClass());
						copy.setVelocity(shot.getVelocity());

						shotRockets.put(copy.getUniqueId(), new ShotRocket(player, rocket));
						rocket.onLaunch(copy, player);

						Common.runTimer(1, new BukkitRunnable() {

							private long elapsedTicks = 0;

							@Override
							public void run() {
								if (!copy.isValid() || copy.isOnGround() || elapsedTicks++ > 20 * 30 /*Remove after 30 seconds to reduce server strain*/)
									cancel();

								else
									rocket.onFlyTick(copy, player);
							}
						});
					});

				} else {
					shotRockets.put(shot.getUniqueId(), new ShotRocket(player, rocket));
					rocket.onLaunch(shot, player);
				}

			} catch (final Throwable t) {
				event.setCancelled(true);

				Common.tell(player, "&cOups! There was a problem with this projectile! Please contact the administrator to review the console for details.");
				Common.error(t, "Failed to shoot Rocket " + tool.getClass());
			}
	}

	/**
	 * Handles rockets on impacts
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRocketHit(ProjectileHitEvent event) {
		final Projectile projectile = event.getEntity();
		final ShotRocket shot = shotRockets.remove(projectile.getUniqueId());

		if (shot != null) {
			final Rocket rocket = shot.getRocket();
			final Player shooter = shot.getShooter();

			try {
				if (rocket.canExplode(projectile, shooter)) {
					final RocketExplosionEvent rocketEvent = new RocketExplosionEvent(rocket, projectile, rocket.getExplosionPower(), rocket.isBreakBlocks());

					if (Common.callEvent(rocketEvent)) {
						final Location location = projectile.getLocation();

						shot.getRocket().onExplode(projectile, shot.getShooter());
						projectile.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), rocketEvent.getPower(), false, rocketEvent.isBreakBlocks());
					}

				} else
					projectile.remove();
			} catch (final Throwable t) {
				Common.tell(shooter, "&cOups! There was a problem with this rocket! Please contact the administrator to review the console for details.");
				Common.error(t, "Failed to handle impact by Rocket " + shot.getRocket().getClass());
			}
		}
	}
}
