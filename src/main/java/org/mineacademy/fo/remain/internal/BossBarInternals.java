package org.mineacademy.fo.remain.internal;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompBarColor;
import org.mineacademy.fo.remain.CompBarStyle;
import org.mineacademy.fo.remain.Remain;

/**
 * The classes handling Boss Bar cross-server compatibility are based off of the
 * code by SoThatsIt.
 * <p>
 * http://forums.bukkit.org/threads/tutorial-utilizing-the-boss-health-bar.158018/page-2#post-1760928
 *
 * @deprecated internal use only, please use {@link Remain} to set the Boss
 * bar
 */
@Deprecated
public class BossBarInternals implements Listener {

	/**
	 * The fake dragon class
	 */
	private static Class<?> entityClass;

	/**
	 * Does the current MC version require us to spawn the dragon below ground?
	 */
	private static boolean isBelowGround = true;

	/**
	 * The player currently viewing the boss bar
	 */
	private static HashMap<UUID, EnderDragonEntity> players = new HashMap<>();

	/**
	 * Currently running timers (for temporary boss bars)
	 */
	private static HashMap<UUID, Integer> timers = new HashMap<>();

	/**
	 * The singleton instance
	 */
	private static BossBarInternals singleton = null;

	// Singleton
	private BossBarInternals() {
	}

	// Initialize reflection and start listening to events
	static {
		if (Remain.isProtocol18Hack()) {
			entityClass = v1_8Hack.class;
			isBelowGround = false;

		} else if (MinecraftVersion.equals(V.v1_6))
			entityClass = v1_6.class;

		else if (MinecraftVersion.equals(V.v1_7))
			entityClass = v1_7.class;

		else if (MinecraftVersion.equals(V.v1_8)) {
			entityClass = v1_8.class;
			isBelowGround = false;

		} else if (MinecraftVersion.newerThan(V.v1_8))
			entityClass = v1_9Native.class;

		if (!MinecraftVersion.olderThan(V.v1_6)) {
			Valid.checkNotNull(entityClass, "Compatible does not support Boss bar on MC version " + MinecraftVersion.getServerVersion() + "!");

			if (singleton == null && SimplePlugin.getInstance().isEnabled()) {
				singleton = new BossBarInternals();

				Bukkit.getPluginManager().registerEvents(singleton, SimplePlugin.getInstance());

				if (Remain.isProtocol18Hack())
					Common.runTimer(5, () -> {
						for (final UUID uuid : players.keySet()) {
							final Player player = Remain.getPlayerByUUID(uuid);

							Remain.sendPacket(player, players.get(uuid).getTeleportPacket(getDragonLocation(player.getLocation())));
						}
					});
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPluginDisable(final PluginDisableEvent e) {
		if (!MinecraftVersion.olderThan(V.v1_6) && e.getPlugin().equals(SimplePlugin.getInstance()) && singleton != null)
			singleton.stop();
	}

	// Removes bars from all players
	private void stop() {
		if (!MinecraftVersion.olderThan(V.v1_6)) {
			for (final Player player : Remain.getOnlinePlayers())
				removeBar(player);

			players.clear();

			for (final int timerID : timers.values())
				Bukkit.getScheduler().cancelTask(timerID);
			timers.clear();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		removeBar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerKick(final PlayerKickEvent event) {
		removeBar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		handleTeleport(event.getPlayer(), event.getTo().clone());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerRespawnEvent event) {
		handleTeleport(event.getPlayer(), event.getRespawnLocation().clone());
	}

	// Fixes bar disappearing on teleport
	private void handleTeleport(final Player player, final Location loc) {
		if (MinecraftVersion.olderThan(V.v1_6) || !hasBar(player))
			return;

		final EnderDragonEntity oldDragon = getDragon(player, "");

		if (oldDragon instanceof v1_9Native)
			return;

		Common.runLater(2, () -> {
			if (!hasBar(player))
				return;

			final float health = oldDragon.health;
			final String message = oldDragon.name;

			Remain.sendPacket(player, getDragon(player, "").getDestroyPacket());

			players.remove(player.getUniqueId());

			final EnderDragonEntity dragon = addDragon(player, loc, message);
			dragon.health = health;

			sendDragon(dragon, player);
		});
	}

	/**
	 * Set a message for the given player.<br>
	 * It will remain there until the player logs off or another plugin overrides
	 * it.<br>
	 * This method will show a health bar using the given percentage value and will
	 * cancel any running timers.
	 *
	 * @param player  The player who should see the given message.
	 * @param message The message shown to the player.<br>
	 *                Due to limitations in Minecraft this message cannot be longer
	 *                than 64 characters.<br>
	 *                It will be cut to that size automatically.
	 * @param percent The percentage of the health bar filled.<br>
	 *                This value must be between 0F (inclusive) and 100F
	 *                (inclusive).
	 * @throws IllegalArgumentException If the percentage is not within valid
	 *                                  bounds.
	 */
	public static void setMessage(final Player player, final String message, final float percent, final CompBarColor color, final CompBarStyle style) {
		Valid.checkBoolean(0F <= percent && percent <= 100F, "Percent must be between 0F and 100F, but was: " + percent);

		if (MinecraftVersion.olderThan(V.v1_6))
			return;

		if (hasBar(player))
			removeBar(player);

		final EnderDragonEntity dragon = getDragon(player, message);

		dragon.name = cleanMessage(message);
		dragon.health = percent / 100f * dragon.getMaxHealth();

		if (color != null)
			dragon.barColor = color;
		if (style != null)
			dragon.barStyle = style;

		cancelTimer(player);

		sendDragon(dragon, player);
	}

	/**
	 * Set a message for the given player.<br>
	 * It will remain there until the player logs off or another plugin overrides
	 * it.<br>
	 * This method will use the health bar as a decreasing timer, all previously
	 * started timers will be cancelled.<br>
	 * The timer starts with a full bar.<br>
	 * The health bar will be removed automatically if it hits zero.
	 *
	 * @param player  The player who should see the given timer/message.
	 * @param message The message shown to the player.<br>
	 *                Due to limitations in Minecraft this message cannot be longer
	 *                than 64 characters.<br>
	 *                It will be cut to that size automatically.
	 * @param seconds The amount of seconds displayed by the timer.<br>
	 *                Supports values above 1 (inclusive).
	 * @throws IllegalArgumentException If seconds is zero or below.
	 */
	public static void setMessage(final Player player, final String message, final int seconds, final CompBarColor color, final CompBarStyle style) {
		Valid.checkBoolean(seconds > 0, "Seconds must be > 1 ");

		if (MinecraftVersion.olderThan(V.v1_6))
			return;

		if (hasBar(player))
			removeBar(player);

		final EnderDragonEntity dragon = getDragon(player, message);

		dragon.name = cleanMessage(message);
		dragon.health = dragon.getMaxHealth();

		if (color != null)
			dragon.barColor = color;
		if (style != null)
			dragon.barStyle = style;

		final float dragonHealthMinus = dragon.getMaxHealth() / seconds;

		cancelTimer(player);

		timers.put(player.getUniqueId(), Common.runTimer(20, 20, () -> {
			final EnderDragonEntity drag = getDragon(player, "");
			drag.health -= dragonHealthMinus;

			if (drag.health <= 1) {
				removeBar(player);
				cancelTimer(player);
			} else
				sendDragon(drag, player);

		}).getTaskId());

		sendDragon(dragon, player);
	}

	public static void removeBar(final Player player) {
		if (!hasBar(player))
			return;

		if (MinecraftVersion.olderThan(V.v1_6))
			return;

		final EnderDragonEntity dragon = getDragon(player, "");

		if (dragon instanceof v1_9Native)
			((v1_9Native) dragon).removePlayer(player);
		else
			Remain.sendPacket(player, getDragon(player, "").getDestroyPacket());

		players.remove(player.getUniqueId());

		cancelTimer(player);
	}

	private static boolean hasBar(final Player player) {
		return players.containsKey(player.getUniqueId());
	}

	private static String cleanMessage(String message) {
		if (message.length() > 64)
			message = message.substring(0, 63);

		return message;
	}

	private static void cancelTimer(final Player player) {
		final Integer timerID = timers.remove(player.getUniqueId());

		if (timerID != null)
			Bukkit.getScheduler().cancelTask(timerID);
	}

	private static void sendDragon(final EnderDragonEntity dragon, final Player player) {
		if (dragon instanceof v1_9Native) {
			final v1_9Native bar = (v1_9Native) dragon;

			bar.addPlayer(player);
			bar.setProgress(dragon.health / dragon.getMaxHealth());
		} else {
			Remain.sendPacket(player, dragon.getMetaPacket(dragon.getWatcher()));
			Remain.sendPacket(player, dragon.getTeleportPacket(getDragonLocation(player.getLocation())));
		}
	}

	private static EnderDragonEntity getDragon(final Player player, final String message) {
		if (hasBar(player))
			return players.get(player.getUniqueId());

		return addDragon(player, cleanMessage(message));
	}

	private static EnderDragonEntity addDragon(final Player player, final String message) {
		final EnderDragonEntity dragon = newDragon(message, getDragonLocation(player.getLocation()));

		if (dragon instanceof v1_9Native)
			((v1_9Native) dragon).addPlayer(player);

		else
			Remain.sendPacket(player, dragon.getSpawnPacket());

		players.put(player.getUniqueId(), dragon);

		return dragon;
	}

	private static EnderDragonEntity addDragon(final Player player, final Location loc, final String message) {
		final EnderDragonEntity dragon = newDragon(message, getDragonLocation(loc));

		if (dragon instanceof v1_9Native)
			((v1_9Native) dragon).addPlayer(player);

		else
			Remain.sendPacket(player, dragon.getSpawnPacket());

		players.put(player.getUniqueId(), dragon);

		return dragon;
	}

	private static Location getDragonLocation(Location loc) {
		if (isBelowGround) {
			loc.subtract(0, 300, 0);
			return loc;
		}

		final float pitch = loc.getPitch();

		if (pitch >= 55)
			loc.add(0, -300, 0);
		else if (pitch <= -55)
			loc.add(0, 300, 0);
		else
			loc = loc.getBlock().getRelative(getDirection(loc), Bukkit.getViewDistance() * 16).getLocation();

		return loc;
	}

	private static BlockFace getDirection(final Location loc) {
		final float dir = Math.round(loc.getYaw() / 90);
		if (dir == -4 || dir == 0 || dir == 4)
			return BlockFace.SOUTH;
		if (dir == -1 || dir == 3)
			return BlockFace.EAST;
		if (dir == -2 || dir == 2)
			return BlockFace.NORTH;
		if (dir == -3 || dir == 1)
			return BlockFace.WEST;
		return null;
	}

	private static EnderDragonEntity newDragon(final String message, final Location loc) {
		EnderDragonEntity fakeDragon = null;

		try {
			fakeDragon = (EnderDragonEntity) entityClass.getConstructor(String.class, Location.class).newInstance(message, loc);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return fakeDragon;
	}

	public static void callStatic() {
		// Loads static {} block, for checking compatibility
	}
}
