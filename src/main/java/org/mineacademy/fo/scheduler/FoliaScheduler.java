package org.mineacademy.fo.scheduler;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.plugin.SimplePlugin;

public class FoliaScheduler extends ProtocolScheduler {

	private final Object foliaScheduler;
	private final Method runAtFixedRate;
	private final Method runDelayed;
	private final Method execute;
	private final Method cancel;

	protected FoliaScheduler() {
		this.foliaScheduler = ReflectionUtil.invoke("getGlobalRegionScheduler", Bukkit.getServer());
		this.runAtFixedRate = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
		this.execute = ReflectionUtil.getMethod(foliaScheduler.getClass(), "run", Plugin.class, Consumer.class);
		this.runDelayed = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class);
		this.cancel = ReflectionUtil.getMethod(ReflectionUtil.lookupClass("io.papermc.paper.threadedregions.scheduler.ScheduledTask"), "cancel");
	}

	@Override
	public Task scheduleSyncRepeatingTask(Runnable task, long delay, long period) {
		Object taskHandle = ReflectionUtil.invoke(runAtFixedRate, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) (t -> task.run()), delay, period);

		return new FoliaTask(cancel, taskHandle);
	}

	@Override
	public Task runTask(Runnable task) {
		Object taskHandle = ReflectionUtil.invoke(execute, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) (t -> task.run()));

		return new FoliaTask(cancel, taskHandle);
	}

	@Override
	public Task scheduleSyncDelayedTask(Runnable task, long delay) {
		Object taskHandle = ReflectionUtil.invoke(runDelayed, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) (t -> task.run()), delay);

		return new FoliaTask(cancel, taskHandle);
	}
}