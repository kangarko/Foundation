package org.mineacademy.fo.menu.tool;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.event.RocketExplosionEvent;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The event listener class responsible for firing events in tools
 */
public final class ToolsListener implements Listener {

	/**
	 * Stores rockets that were shot
	 */
	private final Map<UUID, ShotRocket> shotRockets = new HashMap<>();

	/**
	 * Handles clicking tools and shooting rocket
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onToolClick(final PlayerInteractEvent event) {
		if (!Remain.isInteractEventPrimaryHand(event))
			return;

		final Player player = event.getPlayer();
		final Tool tool = ToolRegistry.getTool(player.getInventory().getItemInMainHand());
		int initialAmount = 0;
		int finalAmount = 0;

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

				} else {
					initialAmount = ItemUtil.getAmount(player, tool.getItem());
					tool.onBlockClick(event);
					finalAmount = ItemUtil.getAmount(player, tool.getItem());
				}

				if (tool.autoCancel()) event.setCancelled(true);

			} catch (final Throwable t) {
				if (initialAmount < finalAmount){
					player.getInventory().addItem(tool.getItem());
				}
				event.setCancelled(true);

				Common.tell(player, SimpleLocalization.Tool.ERROR);
				Common.error(t,
						"Failed to handle " + event.getAction() + " using tool: " + tool.getClass());
			}
	}

	// -------------------------------------------------------------------------------------------
	// Main tool listener
	// -------------------------------------------------------------------------------------------

	/**
	 * Handles block placing
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onToolPlaceBlock(final BlockPlaceEvent event) {

		final Player player = event.getPlayer();
		final Tool tool = ToolRegistry.getTool(player.getInventory().getItemInMainHand());
		int initialAmount = 0;
		int finalAmount = 0;

		if (tool != null)
			try {
				if (event.isCancelled() && tool.ignoreCancelled())
					return;

				initialAmount = ItemUtil.getAmount(player, tool.getItem());
				tool.onBlockPlace(event);
				finalAmount = ItemUtil.getAmount(player, tool.getItem());

				if (tool.autoCancel())
					event.setCancelled(true);

			} catch (final Throwable t) {
				if (initialAmount < finalAmount){
					player.getInventory().addItem(tool.getItem());
				}
				event.setCancelled(true);

				Common.tell(player, SimpleLocalization.Tool.ERROR);
				Common.error(t,
						"Failed to handle placing " + event.getBlock() + " using tool: " + tool.getClass());
			}
	}

	/**
	 * Handles hotbar focus/defocus for tools
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHeltItem(final PlayerItemHeldEvent event) {
		final Player player = event.getPlayer();

		final Tool current = ToolRegistry.getTool(player.getInventory().getItem(event.getNewSlot()));
		final Tool previous = ToolRegistry.getTool(player.getInventory().getItem(event.getPreviousSlot()));

		// Player has attained focus
		if (current != null) {

			if (previous != null) {

				// Not really
				if (previous.equals(current))
					return;

				previous.onHotbarDefocused(player);
			}

			current.onHotbarFocused(player);
		}
		// Player lost focus
		else if (previous != null)
			previous.onHotbarDefocused(player);
	}

	/**
	 * Handles launching a rocket
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRocketShoot(final ProjectileLaunchEvent event) {
		final Projectile shot = event.getEntity();
		final Object /* 1.6.4 Comp */ shooter;

		try {
			shooter = shot.getShooter();
		} catch (final NoSuchMethodError ex) {
			if (MinecraftVersion.atLeast(V.v1_4))
				ex.printStackTrace();

			return;
		}

		if (!(shooter instanceof Player))
			return;

		if (shotRockets.containsKey(shot.getUniqueId()))
			return;

		final Player player = (Player) shooter;
		final Tool tool = ToolRegistry.getTool(player.getInventory().getItemInMainHand());

		if (tool instanceof Rocket)
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

					Common.runLater(shot::remove);

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

				Common.tell(player, SimpleLocalization.Tool.ERROR);
				Common.error(t,
						"Failed to shoot rocket " + tool.getClass());
			}
	}

	@EventHandler
	public void onToolDrop(PlayerDropItemEvent event){
		final Tool tool = ToolRegistry.getTool(event.getItemDrop().getItemStack());

		if (tool != null && tool.isDropForbidden()){
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onToolMove(InventoryClickEvent event){
		if (event.getInventory().getType() == InventoryType.PLAYER ||
		event.getInventory().getType() == InventoryType.CREATIVE ||
		event.getInventory().getType() == InventoryType.CRAFTING) return;

		final Tool currentTool = ToolRegistry.getTool(event.getCurrentItem());

		if (currentTool != null){
			if (currentTool.isDropForbidden()){
				event.setCancelled(true);
			}
		}
	}

	// -------------------------------------------------------------------------------------------
	// Rockets
	// -------------------------------------------------------------------------------------------

	/**
	 * Handles rockets on impacts
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRocketHit(final ProjectileHitEvent event) {
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
				Common.tell(shooter, SimpleLocalization.Tool.ERROR);
				Common.error(t,
						"Failed to handle impact by rocket " + shot.getRocket().getClass());
			}
		}
	}
	/**
	 * Represents a shot rocket with the shooter
	 */
	@Data
	private final class ShotRocket {
		private final Player shooter;
		private final Rocket rocket;
	}
}
