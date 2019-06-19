package org.mineacademy.fo.visualize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.region.RegionCuboid;
import org.mineacademy.fo.region.RegionIncomplete;
import org.mineacademy.fo.remain.CompParticle;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class RegionVisualizer {

	/**
	 * The list of all particles spawned
	 */
	private final List<Location> particles = new ArrayList<>();

	/**
	 * The task that keeps particles visible by re-rendering them.
	 */
	private BukkitTask particleTask;

	/**
	 * The region that is being visualized
	 */
	private final Region region;

	/**
	 *  What kind of retardet particel?
	 */
	private final CompParticle particle;

	/**
	 * Create a new visualizer for the given two positions using the given particle
	 *
	 * @param primary
	 * @param secondary
	 * @param particle
	 */
	public RegionVisualizer(Location primary, Location secondary, CompParticle particle) {
		this(craftRegion(primary, secondary), particle);
	}

	// Convert two points into a region
	private static Region craftRegion(Location primary, Location secondary) {
		return primary != null && secondary != null ? new RegionCuboid(primary, secondary) : new RegionIncomplete(primary, secondary);
	}

	/**
	 * Start visualizing this region for the given amount of ticks
	 *
	 * @param ticks
	 */
	public void schedule(int ticks) {
		start();

		Common.runLater(ticks, () -> stop());
	}

	/**
	 * Start visualizing this region indefinitely
	 *
	 * @deprecated unsafe, we recommend you use {@link #schedule(int)} instead
	 */
	@Deprecated
	public void start() {
		if (particleTask != null)
			particleTask.cancel();

		if (region != null && !(region instanceof RegionIncomplete)) {
			final Collection<Location> locs = BlockUtil.getBoundingBox(region.getPrimary(), region.getSecondary());

			particles.addAll(locs);

			particleTask = new BukkitRunnable() {

				@Override
				public void run() {
					if (particles.isEmpty())
						cancel();

					else
						for (final Location loc : particles)
							particle.spawn(loc);
				}
			}.runTaskTimer(SimplePlugin.getInstance(), 0, 23);
		}
	}

	/**
	 * Stops visualization
	 */
	public void stop() {
		particles.clear();
	}

	/**
	 * Stops the visualization and starts it again
	 *
	 * @deprecated unsafe since we start it indefinitely. Use {@link #schedule(int)} instead
	 */
	@Deprecated
	public void restart() {
		stop();

		if (region != null && !(region instanceof RegionIncomplete))
			start();
	}
}
