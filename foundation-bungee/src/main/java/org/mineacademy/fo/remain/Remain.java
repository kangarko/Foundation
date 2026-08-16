package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.platform.BungeePlugin;
import org.mineacademy.fo.platform.FoundationPlugin;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

/**
 * Our main cross-version compatibility class.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Remain extends RemainCore {

	/**
	 * The server getter, used to change for Redis compatibility.
	 */
	@Setter
	private static Supplier<Collection<ServerInfo>> serverGetter = () -> ProxyServer.getInstance().getServers().values();

	/**
	 * Return the server by the given name
	 *
	 * @param name
	 * @return
	 */
	public static ServerInfo getServer(final String name) {
		for (final ServerInfo server : Remain.getServers())
			if (server.getName().equalsIgnoreCase(name))
				return server;

		return null;
	}

	/**
	 * Returns all servers
	 *
	 * @return
	 */
	public static Collection<ServerInfo> getServers() {
		return serverGetter.get();
	}

	/**
	 * Returns all online players
	 *
	 * @param ignoreVanished
	 * @return the online players
	 */
	public static Collection<ProxiedPlayer> getOnlinePlayers(final boolean ignoreVanished) {
		final Collection<ProxiedPlayer> players = new ArrayList<>();

		for (final ServerInfo serverInfo : getServers())
			for (final ProxiedPlayer player : serverInfo.getPlayers())
				if (player != null && !players.contains(player)) {
					if (ignoreVanished && PlayerUtil.isVanished(player))
						continue;

					players.add(player);
				}

		return players;
	}

	/**
	 * Get the player or null if he is not online
	 *
	 * @param ignoreVanished
	 * @param name
	 * @return
	 */
	public static ProxiedPlayer getPlayer(final String name, final boolean ignoreVanished) {
		for (final ProxiedPlayer player : getOnlinePlayers(ignoreVanished))
			if (player.getName().equalsIgnoreCase(name))
				return player;

		return null;
	}

	/**
	 * Get the player or null if he is not online
	 *
	 * @param ignoreVanished
	 * @param uuid
	 * @return
	 */
	public static ProxiedPlayer getPlayer(final UUID uuid, final boolean ignoreVanished) {
		for (final ProxiedPlayer player : getOnlinePlayers(ignoreVanished))
			if (player.getUniqueId().equals(uuid))
				return player;

		return null;
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param timer
	 * @return the task or null
	 */
	public static Task runTaskAsync(final int delayTicks, final Runnable timer) {
		ValidCore.checkBoolean(!(timer instanceof ScheduledTask), "Cannot use ScheduledTask for scheduling tasks!");
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return null;

		return SimpleBungeeTask.fromBungee(BungeePlugin.getServer().getScheduler()
				.schedule(BungeePlugin.getInstance(), runnable, delayTicks * 50, TimeUnit.MILLISECONDS));
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param timer
	 * @return
	 */
	public static Task runTaskTimerAsync(final int delayTicks, final int repeatTicks, final Runnable timer) {
		ValidCore.checkBoolean(!(timer instanceof ScheduledTask), "Cannot use ScheduledTask for scheduling tasks!");
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return null;

		return SimpleBungeeTask.fromBungee(BungeePlugin.getServer().getScheduler()
				.schedule(BungeePlugin.getInstance(), runnable, delayTicks * 50, repeatTicks * 50, TimeUnit.MILLISECONDS));
	}
}

/**
 * Implements a Task for both Bukkit and Folia.
 */
final class SimpleBungeeTask implements Task {

	private final ScheduledTask task;

	@Getter
	private boolean cancelled = false;

	public SimpleBungeeTask(final ScheduledTask task) {
		this.task = task;
	}

	@Override
	public void cancel() {
		this.task.cancel();

		this.cancelled = true;
	}

	@Override
	public int getTaskId() {
		return this.task.getId();
	}

	@Override
	public boolean isSync() {
		return false;
	}

	@Override
	public FoundationPlugin getOwner() {
		return BungeePlugin.getInstance();
	}

	static SimpleBungeeTask fromBungee(final ScheduledTask task) {
		return new SimpleBungeeTask(task);
	}
}