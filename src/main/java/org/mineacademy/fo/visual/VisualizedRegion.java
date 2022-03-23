package org.mineacademy.fo.visual;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompParticle;

import lombok.Getter;
import lombok.Setter;

/**
 * A simply way to visualize two locations in the world
 */
public final class VisualizedRegion extends Region {

	/**
	 * A list of players who can see the particles, along with the particle color (requires {@link #particle} to be REDSTONE)
	 */
	private final StrictMap<Player, Color> viewers = new StrictMap<>();

	/**
	 * The task responsible for sending particles
	 */
	private BukkitTask task;

	/**
	 * The particle that is being sent out
	 */
	@Getter
	@Setter
	private CompParticle particle = CompParticle.VILLAGER_HAPPY;

	/**
	 * The delay between each particle shows when visualizing the region, the lower, the better visibility but more CPU drain
	 */
	@Getter
	@Setter
	private int delayTicks = 23;

	/**
	 * Create a new visualizable empty region
	 */
	public VisualizedRegion() {
		this(null, null);
	}

	/**
	 * Create a new visualizable region
	 *
	 * @param primary
	 * @param secondary
	 */
	public VisualizedRegion(final Location primary, final Location secondary) {
		super(primary, secondary);
	}

	/**
	 * Create a visualizable region
	 *
	 * @param name
	 * @param primary
	 * @param secondary
	 */
	public VisualizedRegion(final String name, final Location primary, final Location secondary) {
		super(name, primary, secondary);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Rendering
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Shows the region to the given player for the given duration,
	 * the hides it
	 *
	 * @param player
	 * @param durationTicks
	 */
	public void showParticles(Player player, int durationTicks) {
		this.showParticles(player, null, durationTicks);
	}

	/**
	 * Shows the region to the given player for the given duration,
	 * the hides it
	 *
	 * @param player
	 * @param color
	 * @param durationTicks
	 */
	public void showParticles(Player player, @Nullable Color color, int durationTicks) {
		showParticles(player, color);

		Common.runLater(durationTicks, () -> {
			if (canSeeParticles(player))
				hideParticles(player);
		});
	}

	/**
	 * Shows the region to the given player
	 *
	 * @param player
	 */
	public void showParticles(final Player player) {
		this.showParticles(player, null);
	}

	/**
	 * Shows the region to the given player
	 *
	 * @param player
	 * @param color
	 */
	public void showParticles(final Player player, @Nullable Color color) {
		Valid.checkBoolean(!canSeeParticles(player), "Player " + player.getName() + " already sees region " + this);
		Valid.checkBoolean(isWhole(), "Cannot show particles of an incomplete region " + this);

		viewers.put(player, color);

		if (task == null)
			startVisualizing();
	}

	/**
	 * Hides the region from the given player
	 *
	 * @param player
	 */
	public void hideParticles(final Player player) {
		Valid.checkBoolean(canSeeParticles(player), "Player " + player.getName() + " is not seeing region " + this);

		viewers.removeWeak(player);

		if (viewers.isEmpty() && task != null)
			stopVisualizing();
	}

	/**
	 * Return true if the given player can see the region particles
	 *
	 * @param player
	 * @return
	 */
	public boolean canSeeParticles(final Player player) {
		return viewers.containsKey(player);
	}

	/*
	 * Starts visualizing this region if it is whole
	 */
	private void startVisualizing() {
		Valid.checkBoolean(task == null, "Already visualizing region " + this + "!");
		Valid.checkBoolean(isWhole(), "Cannot visualize incomplete region " + this + "!");

		task = Common.runTimer(delayTicks, new BukkitRunnable() {
			@Override
			public void run() {
				if (viewers.isEmpty()) {
					stopVisualizing();

					return;
				}

				final Set<Location> blocks = BlockUtil.getBoundingBox(getPrimary(), getSecondary());

				for (final Location location : blocks)
					for (final Map.Entry<Player, Color> entry : viewers.entrySet()) {
						final Player viewer = entry.getKey();
						final Color color = entry.getValue();
						final Location viewerLocation = viewer.getLocation();

						if (viewerLocation.getWorld().equals(location.getWorld()) && viewerLocation.distance(location) < 100) {
							if (color != null)
								CompParticle.REDSTONE.spawn(viewer, location, color, 0.5F);

							else
								particle.spawn(viewer, location);
						}
					}

			}
		});
	}

	/*
	 * Stops the region from being visualized
	 */
	private void stopVisualizing() {
		Valid.checkNotNull(task, "Region " + this + " not visualized");

		task.cancel();
		task = null;

		viewers.clear();
	}

	/**
	 * Converts a saved map from your yaml/json file into a region if it contains Primary and Secondary keys
	 *
	 * @param map
	 * @return
	 */
	public static VisualizedRegion deserialize(final SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = map.getLocation("Primary");
		final Location sec = map.getLocation("Secondary");

		return new VisualizedRegion(name, prim, sec);
	}
}
