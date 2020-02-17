package org.mineacademy.fo.visualize_old;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompParticle;

/**
 *  @deprecated use classes in the new "visual" package
 */
@Deprecated
public final class RegionVisualized extends Region {

	private final static CompParticle DEFAULT_PARTICLE = CompParticle.VILLAGER_HAPPY;

	/**
	 * The list of all particles spawned
	 */
	private final List<Location> particles = new ArrayList<>();

	/**
	 * The task that keeps particles visible by re-rendering them.
	 */
	private BukkitTask particleTask;

	public RegionVisualized(final String name, final Location primary, final Location secondary) {
		super(name, primary, secondary);
	}

	public void show(final int durationTicks) {
		show(durationTicks, DEFAULT_PARTICLE);
	}

	/**
	 * Start visualizing this region for the given amount of ticks
	 *
	 * @param durationTicks
	 */
	public void show(final int durationTicks, final CompParticle particle) {
		start(particle);

		Common.runLater(durationTicks, () -> stop());
	}

	private void start(final CompParticle particle) {
		if (particleTask != null)
			particleTask.cancel();

		particles.clear();
		particles.addAll(BlockUtil.getBoundingBox(getPrimary(), getSecondary()));

		particleTask = Common.runTimer(23, new BukkitRunnable() {
			@Override
			public void run() {
				if (particles.isEmpty()) {
					cancel();

					return;
				}

				for (final Location loc : particles)
					particle.spawn(loc);
			}
		});

	}

	public void restart(final int durationTicks) {
		restart(durationTicks, DEFAULT_PARTICLE);
	}

	public void restart(final int durationTicks, final CompParticle particle) {
		stop();

		show(durationTicks, particle);
	}

	/**
	 * Stops visualization
	 */
	public void stop() {
		particles.clear();
	}

	/**
	 * Converts a saved map from your yaml/json file into a region if it contains Primary and Secondary keys
	 *
	 * @param map
	 * @return
	 */
	public static RegionVisualized deserialize(final SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = map.getLocation("Primary");
		final Location sec = map.getLocation("Secondary");

		return new RegionVisualized(name, prim, sec);
	}
}
