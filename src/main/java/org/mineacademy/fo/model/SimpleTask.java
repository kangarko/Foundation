package org.mineacademy.fo.model;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimpleTask implements BukkitTask {

	@Getter
	private final int taskId;

	@Getter
	private final boolean sync;

	private final Method foliaCancelMethod;
	private final Object foliaTaskInstance;

	@Getter
	private boolean cancelled = false;

	@Override
	public void cancel() {
		if (Remain.isFolia())
			ReflectionUtil.invoke(this.foliaCancelMethod, this.foliaTaskInstance);

		else
			Bukkit.getScheduler().cancelTask(taskId);

		this.cancelled = true;
	}

	public static SimpleTask fromBukkit(BukkitTask task) {
		return new SimpleTask(task.getTaskId(), task.isSync(), null, null);
	}

	public static SimpleTask fromBukkit(int taskId, boolean sync) {
		return taskId >= 0 ? null : new SimpleTask(taskId, sync, null, null);
	}

	public static SimpleTask fromFolia(Method foliaCancelMethod, Object foliaTaskInstance) {
		return new SimpleTask(0, false, foliaCancelMethod, foliaTaskInstance);
	}

	@Override
	public Plugin getOwner() {
		return SimplePlugin.getInstance();
	}
}