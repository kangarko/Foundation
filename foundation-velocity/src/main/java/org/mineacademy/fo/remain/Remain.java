package org.mineacademy.fo.remain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.VelocityPlugin;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Our main cross-version compatibility class.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Remain extends RemainCore {

	/**
	 * The server getter, used to change for Redis compatibility.
	 */
	@Setter
	private static Supplier<Collection<RegisteredServer>> serverGetter = () -> VelocityPlugin.getServer().getAllServers();

	/**
	 * Return the server by the given name
	 *
	 * @param name
	 * @return
	 */
	public static RegisteredServer getServer(final String name) {
		for (final RegisteredServer server : Remain.getServers())
			if (server.getServerInfo().getName().equalsIgnoreCase(name))
				return server;

		return null;
	}

	/**
	 * Returns all servers
	 *
	 * @return
	 */
	public static Collection<RegisteredServer> getServers() {
		return serverGetter.get();
	}

	/**
	 * Returns all online players
	 *
	 * @param ignoreVanished
	 * @return the online players
	 */
	public static Collection<Player> getOnlinePlayers(final boolean ignoreVanished) {
		final Collection<Player> players = new ArrayList<>();

		for (final RegisteredServer serverInfo : getServers())
			for (final Player player : serverInfo.getPlayersConnected())
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
	public static Player getPlayer(final String name, final boolean ignoreVanished) {
		for (final Player player : getOnlinePlayers(ignoreVanished))
			if (player.getUsername().equalsIgnoreCase(name))
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
	public static Player getPlayer(final UUID uuid, final boolean ignoreVanished) {
		for (final Player player : getOnlinePlayers(ignoreVanished))
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

		return SimpleVelocityTask.fromVelocity(VelocityPlugin.getServer().getScheduler()
				.buildTask(VelocityPlugin.getInstance(), runnable)
				.delay(Duration.ofMillis(delayTicks * 50))
				.schedule());
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

		return SimpleVelocityTask.fromVelocity(VelocityPlugin.getServer().getScheduler()
				.buildTask(VelocityPlugin.getInstance(), runnable)
				.delay(Duration.ofMillis(delayTicks * 50))
				.repeat(Duration.ofMillis(repeatTicks * 50))
				.schedule());
	}
}

/**
 * Implements a Task for both Bukkit and Folia.
 */
final class SimpleVelocityTask implements Task {

	private static volatile int globalTaskId = 0;

	private final ScheduledTask task;

	@Getter
	private final int taskId;

	@Getter
	private boolean cancelled = false;

	public SimpleVelocityTask(final ScheduledTask task) {
		this.task = task;
		this.taskId = globalTaskId++;
	}

	@Override
	public void cancel() {
		if (this.task.status() == TaskStatus.SCHEDULED)
			this.task.cancel();

		this.cancelled = true;
	}

	@Override
	public boolean isSync() {
		return false;
	}

	@Override
	public FoundationPlugin getOwner() {
		return VelocityPlugin.getInstance();
	}

	static SimpleVelocityTask fromVelocity(final ScheduledTask task) {
		return new SimpleVelocityTask(task);
	}
}