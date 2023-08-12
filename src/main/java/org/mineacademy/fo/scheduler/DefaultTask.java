package org.mineacademy.fo.scheduler;

import org.bukkit.scheduler.BukkitScheduler;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultTask implements Task {

	private final int taskId;
	private final BukkitScheduler scheduler;

	@Override
	public void cancel() {
		this.scheduler.cancelTask(taskId);
	}
}