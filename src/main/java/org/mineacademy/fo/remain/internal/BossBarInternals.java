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

import lombok.Getter;

/**
 * The classes handling Boss Bar cross-server compatibility are based off of the
 * code by SoThatsIt.
 * <p>
 * http://forums.bukkit.org/threads/tutorial-utilizing-the-boss-health-bar.158018/page-2#post-1760928
 */
public final class BossBarInternals implements Listener {

	/**
	 * The singleton instance
	 */
	@Getter
	private static BossBarInternals instance = new BossBarInternals();

	/**
	 * The fake dragon class
	 */
	private final Class<?> entityClass;

	/**
	 * Does the current MC version require us to spawn the dragon below ground?
	 */
	private final boolean isBelowGround;

	/**
	 * The player currently viewing the boss bar
	 */
	private final HashMap<UUID, NMSDragon> players = new HashMap<>();

	/**
	 * Currently running timers (for temporary boss bars)
	 */
	private final HashMap<UUID, Integer> timers = new HashMap<>();

	// Singleton
	private BossBarInternals() {

		if (MinecraftVersion.olderThan(V.v1_6)) {
			this.entityClass = null;
			this.isBelowGround = false;
		}

		else if (Remain.isProtocol18Hack()) {
			this.entityClass = NMSDragon_v1_8Hack.class;
			this.isBelowGround = false;

		} else if (MinecraftVersion.equals(V.v1_6)) {
			this.entityClass = NMSDragon_v1_6.class;
			this.isBelowGround = true;

		} else if (MinecraftVersion.equals(V.v1_7)) {
			this.entityClass = NMSDragon_v1_7.class;
			this.isBelowGround = true;

		} else if (MinecraftVersion.equals(V.v1_8)) {
			this.entityClass = NMSDragon_v1_8.class;
			this.isBelowGround = false;

		} else {
			this.entityClass = NMSDragon_v1_9.class;
			this.isBelowGround = true;
		}

		if (MinecraftVersion.atLeast(V.v1_6)) {
			Valid.checkNotNull(entityClass, "Compatible does not support Boss bar on MC version " + MinecraftVersion.getServerVersion() + "!");

			Common.registerEvents(this);

			if (Remain.isProtocol18Hack())
				Common.runTimer(5, () -> {
					for (final UUID uuid : players.keySet()) {
						final Player player = Remain.getPlayerByUUID(uuid);

						Remain.sendPacket(player, players.get(uuid).getTeleportPacket(getDragonLocation(player.getLocation())));
					}
				});
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPluginDisable(final PluginDisableEvent event) {
		if (event.getPlugin().equals(SimplePlugin.getInstance()))
			this.stop();
	}

	// Removes bars from all players
	private void stop() {
		for (final Player player : Remain.getOnlinePlayers())
			this.removeBar(player);

		this.players.clear();

		for (final int timerID : this.timers.values())
			Bukkit.getScheduler().cancelTask(timerID);

		this.timers.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		this.removeBar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerKick(final PlayerKickEvent event) {
		this.removeBar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		this.handleTeleport(event.getPlayer(), event.getTo().clone());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerRespawnEvent event) {
		this.handleTeleport(event.getPlayer(), event.getRespawnLocation().clone());
	}

	// Fixes bar disappearing on teleport
	private void handleTeleport(final Player player, final Location loc) {
		if (!this.hasBar(player))
			return;

		final NMSDragon oldDragon = this.getDragon(player, "");

		if (oldDragon instanceof NMSDragon_v1_9)
			return;

		Common.runLater(2, () -> {
			if (!this.hasBar(player))
				return;

			final float health = oldDragon.getHealth();
			final String message = oldDragon.getName();

			Remain.sendPacket(player, this.getDragon(player, "").getDestroyPacket());

			this.players.remove(player.getUniqueId());

			final NMSDragon dragon = this.addDragon(player, loc, message);
			dragon.setHealthF(health);

			this.sendDragon(dragon, player);
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
	 * @param color
	 * @param style
	 * @throws IllegalArgumentException If the percentage is not within valid
	 *                                  bounds.
	 */
	public void setMessage(final Player player, final String message, final float percent, final CompBarColor color, final CompBarStyle style) {
		Valid.checkBoolean(0F <= percent && percent <= 100F, "Percent must be between 0F and 100F, but was: " + percent);

		if (this.entityClass == null)
			return;

		if (hasBar(player))
			removeBar(player);

		final NMSDragon dragon = getDragon(player, message);

		dragon.setName(cleanMessage(message));
		dragon.setHealthF(percent / 100f * dragon.getMaxHealth());

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
	 * @param color
	 * @param style
	 * @throws IllegalArgumentException If seconds is zero or below.
	 */
	public void setMessage(final Player player, final String message, final int seconds, final CompBarColor color, final CompBarStyle style) {
		Valid.checkBoolean(seconds > 0, "Seconds must be > 1 ");

		if (this.entityClass == null)
			return;

		if (hasBar(player))
			removeBar(player);

		final NMSDragon dragon = getDragon(player, message);

		dragon.setName(cleanMessage(message));
		dragon.setHealthF(dragon.getMaxHealth());

		if (color != null)
			dragon.barColor = color;
		if (style != null)
			dragon.barStyle = style;

		final float dragonHealthMinus = dragon.getMaxHealth() / seconds;

		cancelTimer(player);

		this.timers.put(player.getUniqueId(), Common.runTimer(20, 20, () -> {
			final NMSDragon drag = getDragon(player, "");
			drag.setHealthF(drag.getHealth() - dragonHealthMinus);

			if (drag.getHealth() <= 1) {
				removeBar(player);
				cancelTimer(player);
			} else
				sendDragon(drag, player);

		}).getTaskId());

		sendDragon(dragon, player);
	}

	/**
	 * Removes the bar from the given player
	 *
	 * @param player
	 */
	public void removeBar(final Player player) {

		if (this.entityClass == null)
			return;

		if (!hasBar(player))
			return;

		final NMSDragon dragon = getDragon(player, "");

		if (dragon instanceof NMSDragon_v1_9)
			((NMSDragon_v1_9) dragon).removePlayer(player);
		else
			Remain.sendPacket(player, getDragon(player, "").getDestroyPacket());

		this.players.remove(player.getUniqueId());

		cancelTimer(player);
	}

	private boolean hasBar(final Player player) {
		return this.players.containsKey(player.getUniqueId());
	}

	private static String cleanMessage(String message) {
		if (message.length() > 64)
			message = message.substring(0, 63);

		return message;
	}

	private void cancelTimer(final Player player) {
		final Integer timerID = this.timers.remove(player.getUniqueId());

		if (timerID != null)
			Bukkit.getScheduler().cancelTask(timerID);
	}

	private void sendDragon(final NMSDragon dragon, final Player player) {
		if (dragon instanceof NMSDragon_v1_9) {
			final NMSDragon_v1_9 bar = (NMSDragon_v1_9) dragon;

			bar.addPlayer(player);
			bar.setProgress(dragon.getHealth() / dragon.getMaxHealth());

		} else {
			Remain.sendPacket(player, dragon.getMetaPacket(dragon.getWatcher()));
			Remain.sendPacket(player, dragon.getTeleportPacket(getDragonLocation(player.getLocation())));
		}
	}

	private NMSDragon getDragon(final Player player, final String message) {
		if (this.hasBar(player))
			return this.players.get(player.getUniqueId());

		return addDragon(player, cleanMessage(message));
	}

	private NMSDragon addDragon(final Player player, final String message) {
		return this.addDragon(player, player.getLocation(), message);
	}

	private NMSDragon addDragon(final Player player, final Location loc, final String message) {
		final NMSDragon dragon = newDragon(message, getDragonLocation(loc));

		if (dragon instanceof NMSDragon_v1_9)
			((NMSDragon_v1_9) dragon).addPlayer(player);

		else
			Remain.sendPacket(player, dragon.getSpawnPacket());

		this.players.put(player.getUniqueId(), dragon);

		return dragon;
	}

	private Location getDragonLocation(Location loc) {
		if (this.isBelowGround) {
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

		loc.subtract(0, 150, 0);

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

	private NMSDragon newDragon(final String message, final Location loc) {
		NMSDragon fakeDragon = null;

		try {
			fakeDragon = (NMSDragon) this.entityClass.getConstructor(String.class, Location.class).newInstance(message, loc);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return fakeDragon;
	}
}
