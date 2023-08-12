package org.mineacademy.fo.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.mineacademy.fo.plugin.SimplePlugin;

public class DefaultScheduler extends ProtocolScheduler {

	private final BukkitScheduler scheduler;

	protected DefaultScheduler() {
		this.scheduler = Bukkit.getScheduler();
	}

	@Override
	public Task scheduleSyncRepeatingTask(Runnable task, long delay, long period) {
		int taskId = scheduler.scheduleSyncRepeatingTask(SimplePlugin.getInstance(), task, delay, period);

		return taskId >= 0 ? new DefaultTask(taskId, scheduler) : null;
	}

	@Override
	public Task runTask(Runnable task) {
		int taskId = scheduler.runTask(SimplePlugin.getInstance(), task).getTaskId();

		return taskId >= 0 ? new DefaultTask(taskId, scheduler) : null;
	}

	@Override
	public Task scheduleSyncDelayedTask(Runnable task, long delay) {
		int taskId = scheduler.scheduleSyncDelayedTask(SimplePlugin.getInstance(), task, delay);

		return taskId >= 0 ? new DefaultTask(taskId, scheduler) : null;
	}
}