package org.mineacademy.fo.region;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Represents a cuboid region
 */
@Getter
public class Region implements ConfigSerializable {

	/**
	 * Represents an empty region
	 */
	public static final VisualizedRegion EMPTY = new VisualizedRegion(null, null);

	/**
	 * The name of the region, or null if not given
	 */
	@Setter
	private String name;

	/**
	 * The primary region position
	 */
	private Location primary;

	/**
	 * The secondary region position
	 */
	private Location secondary;

	/**
	 * Create a new region
	 *
	 * @param primary
	 * @param secondary
	 */
	public Region(final Location primary, final Location secondary) {
		this(null, primary, secondary);
	}

	/**
	 * Create a new named region
	 *
	 * @param name
	 * @param primary
	 * @param secondary
	 */
	public Region(final String name, final Location primary, final Location secondary) {
		this.name = name;

		if (primary != null) {
			Valid.checkNotNull(primary.getWorld(), "Primary location lacks a world!");

			this.primary = primary;
		}

		if (secondary != null) {
			Valid.checkNotNull(secondary.getWorld(), "Primary location lacks a world!");

			this.secondary = secondary;
		}
	}

	/*
	 * Change primary/secondary around to make secondary always the lowest point
	 */
	private Location[] getCorrectedPoints() {
		if (primary == null || secondary == null)
			return null;

		Valid.checkBoolean(primary.getWorld().getName().equals(secondary.getWorld().getName()), "Points must be in one world! Primary: " + primary + " != secondary: " + secondary);

		final int x1 = primary.getBlockX(), x2 = secondary.getBlockX(),
				y1 = primary.getBlockY(), y2 = secondary.getBlockY(),
				z1 = primary.getBlockZ(), z2 = secondary.getBlockZ();

		final Location primary = this.primary.clone();
		final Location secondary = this.secondary.clone();

		primary.setX(Math.min(x1, x2));
		primary.setY(Math.min(y1, y2));
		primary.setZ(Math.min(z1, z2));

		secondary.setX(Math.max(x1, x2));
		secondary.setY(Math.max(y1, y2));
		secondary.setZ(Math.max(z1, z2));

		return new Location[] { primary, secondary };
	}

	/**
	 * Calculate a rough location of the center of this region
	 *
	 * @return
	 */
	public final Location getCenter() {
		Valid.checkBoolean(isWhole(), "Cannot perform getCenter on a non-complete region: " + toString());

		final Location[] centered = getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		return new Location(primary.getWorld(),
				(primary.getX() + secondary.getX()) / 2,
				(primary.getY() + secondary.getY()) / 2,
				(primary.getZ() + secondary.getZ()) / 2);
	}

	/**
	 * Count all blocks within this region
	 *
	 * @return
	 */
	public final List<Block> getBlocks() {
		Valid.checkBoolean(isWhole(), "Cannot perform getBlocks on a non-complete region: " + toString());
		final Location[] centered = getCorrectedPoints();

		return BlockUtil.getBlocks(centered[0], centered[1]);
	}

	/**
	 * Return locations representing the bounding box of a cuboid region,
	 * used when rendering particle effects
	 *
	 * @return
	 */
	public final Set<Location> getBoundingBox() {
		Valid.checkBoolean(isWhole(), "Cannot perform getBoundingBox on a non-complete region: " + toString());

		return BlockUtil.getBoundingBox(primary, secondary);
	}

	/**
	 * Count all entities within this region
	 *
	 * @return
	 */
	public final List<Entity> getEntities() {
		Valid.checkBoolean(isWhole(), "Cannot perform getEntities on a non-complete region: " + toString());

		final List<Entity> found = new LinkedList<>();

		final Location[] centered = getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		final int xMin = (int) primary.getX() >> 4;
		final int xMax = (int) secondary.getX() >> 4;
		final int zMin = (int) primary.getZ() >> 4;
		final int zMax = (int) secondary.getZ() >> 4;

		for (int cx = xMin; cx <= xMax; ++cx)
			for (int cz = zMin; cz <= zMax; ++cz)
				for (final Entity entity : getWorld().getChunkAt(cx, cz).getEntities())
					if (entity.isValid() && entity.getLocation() != null && isWithin(entity.getLocation()))
						found.add(entity);

		return found;
	}

	/**
	 * Get world for this region
	 *
	 * @return
	 */
	public final World getWorld() {
		if (!isWhole())
			return null;

		if (primary != null && secondary == null)
			return Bukkit.getWorld(primary.getWorld().getName());

		if (secondary != null && primary == null)
			return Bukkit.getWorld(secondary.getWorld().getName());

		Valid.checkBoolean(primary.getWorld().getName().equals(secondary.getWorld().getName()), "Worlds of this region not the same: " + primary.getWorld() + " != " + secondary.getWorld());
		return Bukkit.getWorld(primary.getWorld().getName());
	}

	/**
	 * Return true if the given point is within this region
	 *
	 * @param location
	 * @return
	 */
	public final boolean isWithin(@NonNull final Location location) {
		Valid.checkBoolean(isWhole(), "Cannot perform isWithin on a non-complete region: " + toString());

		if (!location.getWorld().getName().equals(primary.getWorld().getName()))
			return false;

		final Location[] centered = getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		final int x = (int) location.getX();
		final int y = (int) location.getY();
		final int z = (int) location.getZ();

		return x >= primary.getX() && x <= secondary.getX()
				&& y >= primary.getY() && y <= secondary.getY()
				&& z >= primary.getZ() && z <= secondary.getZ();
	}

	/**
	 * Return true if both region points are set
	 *
	 * @return
	 */
	public final boolean isWhole() {
		return primary != null && secondary != null;
	}

	/**
	 * Set the primary region point
	 *
	 * @param primary
	 */
	public final void setPrimary(final Location primary) {
		this.primary = primary;
	}

	/**
	 * Set the secondary region point
	 *
	 * @param primary
	 */
	public final void setSecondary(final Location secondary) {
		this.secondary = secondary;
	}

	/**
	 * Sets a new primary and secondary locations,
	 * preserving old keys if the new are not given
	 *
	 * @param primary
	 * @param secondary
	 */
	public final void updateLocationsWeak(final Location primary, final Location secondary) {
		if (primary != null)
			this.primary = primary;

		if (secondary != null)
			this.secondary = secondary;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "{name=" + name + ",location=" + Common.shortLocation(primary) + " - " + Common.shortLocation(secondary) + "}";
	}

	/**
	 * Saves the region data into a map you can save in your yaml or json file
	 */
	@Override
	public final SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExist("Name", name);
		map.putIfExist("Primary", primary);
		map.putIfExist("Secondary", secondary);

		return map;
	}

	/**
	 * Converts a saved map from your yaml/json file into a region if it contains Primary and Secondary keys
	 *
	 * @param map
	 * @return
	 */
	public static Region deserialize(final SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = map.getLocation("Primary");
		final Location sec = map.getLocation("Secondary");

		return new Region(name, prim, sec);
	}
}