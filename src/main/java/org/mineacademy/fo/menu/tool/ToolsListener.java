package org.mineacademy.fo.menu.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
import org.mineacademy.fo.Common;
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
						Objects.requireNonNull(shot, "shot = null");
						Objects.requireNonNull(world, "shot.world = null");
						Objects.requireNonNull(loc, "shot.location = null");

						final Location directedLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().setY(0).normalize().multiply(1.05)).add(0, 0.2, 0);

						final Projectile copy = world.spawn(directedLoc, shot.getClass());
						copy.setVelocity(shot.getVelocity());

						shotRockets.put(copy.getUniqueId(), new ShotRocket(player, rocket));
						rocket.onLaunch(copy, player);
					});

				} else {
					shotRockets.put(shot.getUniqueId(), new ShotRocket(player, rocket));
					rocket.onLaunch(shot, player);
				}

			} catch (final Throwable t) {
				Common.tell(player, "&cOups! There was a problem with this projectile! Please contact the administrator to review the console for details.");

				event.setCancelled(true);
				t.printStackTrace();
			}
	}

	/**
	 * Handles rockets on impacts
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRocketHit(ProjectileHitEvent event) {
		final Projectile proj = event.getEntity();
		final ShotRocket rocket = shotRockets.remove(proj.getUniqueId());

		if (rocket != null)
			rocket.getRocket().onImpact(proj, rocket.getShooter(), proj.getLocation());
	}

	/**
	 * Handles clicking tools
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

				tool.onBlockClick(event);

				if (tool.autoCancel())
					event.setCancelled(true);

			} catch (final Throwable t) {
				Common.tell(player, "&cOups! There was a problem with this tool! Please contact the administrator to review the console for details.");

				event.setCancelled(true);
				t.printStackTrace();
			}
	}
}
