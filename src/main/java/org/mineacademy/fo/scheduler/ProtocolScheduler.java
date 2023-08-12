package org.mineacademy.fo.scheduler;

import org.mineacademy.fo.remain.Remain;

public abstract class ProtocolScheduler {

	private static ProtocolScheduler instance;

	protected ProtocolScheduler() {
	}

	public abstract Task scheduleSyncRepeatingTask(Runnable task, long delay, long period);

	public abstract Task runTask(Runnable task);

	public abstract Task scheduleSyncDelayedTask(Runnable task, long delay);

	public static ProtocolScheduler getInstance() {

		if (instance == null)
			instance = Remain.isFolia() ? new FoliaScheduler() : new DefaultScheduler();

		return instance;
	}
}