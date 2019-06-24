package org.mineacademy.fo.menu.tool;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
import org.mineacademy.fo.event.RocketExplosionEvent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.Data;

/**
 * The event listener class responsible for firing events in tools
 */
public final class ToolsListener implements Listener {

	/**
	 * We automatically scan classes in your plugin to find classes extending "Tool" and register 'em
	 * This process is only done once since startup
	 *
	 * To make this work make sure your tool class has "public static Tool getInstance" method
	 */
	private static boolean toolsRegistered = false;

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

	// Create a new instance for event listening
	public ToolsListener() {
		autoRegisterTools();
	}

	/**
	 * Scan classes and load tools that have "getInstance", see {@link #toolsRegistered}
	 *
	 * @return
	 */
	private static void autoRegisterTools() {
		if (toolsRegistered)
			return;

		toolsRegistered = true;

		final Set<Tool> tools = new HashSet<>();

		try (final JarFile file = new JarFile(SimplePlugin.getSource())) {

			for (final Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements();) {
				final JarEntry jar = entry.nextElement();
				final String name = jar.getName().replace("/", ".");

				if (name.endsWith(".class") && !name.contains("$"))
					try {
						final Class<?> toolClass = Class.forName(name.substring(0, name.length() - 6));

						if (Tool.class.isAssignableFrom(toolClass) && !Tool.class.equals(toolClass) && !Rocket.class.equals(toolClass))
							try {
								// Calling getInstance will also make the tool being automatically registered
								final Object instance = toolClass.getMethod("getInstance").invoke(null);

								tools.add((Tool) instance);

							} catch (NoSuchMethodError | NoSuchMethodException | NullPointerException ex) {
								// Ignore
							} catch (final Throwable t) {
								Common.log("Failed to register Tool class " + toolClass + " due to " + t);
							}

					} catch (final NoClassDefFoundError ex) {
					}
			}
		} catch (final Throwable t) {
		}
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
				Common.tell(player, "&cOups! There was a problem with this tool! Please contact the administrator to review the console for details.");

				event.setCancelled(true);
				t.printStackTrace();
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
						Objects.requireNonNull(shot, "shot = null");
						Objects.requireNonNull(world, "shot.world = null");
						Objects.requireNonNull(loc, "shot.location = null");

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
		final Projectile projectile = event.getEntity();
		final ShotRocket shot = shotRockets.remove(projectile.getUniqueId());

		if (shot != null) {
			final Rocket rocket = shot.getRocket();
			final Player shooter = shot.getShooter();

			if (rocket.canExplode(projectile, shooter)) {
				final RocketExplosionEvent rocketEvent = new RocketExplosionEvent(rocket, projectile, rocket.getExplosionPower(), rocket.isBreakBlocks());

				if (Common.callEvent(rocketEvent)) {
					final Location location = projectile.getLocation();

					shot.getRocket().onExplode(projectile, shot.getShooter());
					projectile.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), rocketEvent.getPower(), false, rocketEvent.isBreakBlocks());
				}

			} else
				projectile.remove();
		}
	}
}
