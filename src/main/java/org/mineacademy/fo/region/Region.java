package org.mineacademy.fo.region;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Represents a cuboid region
 */
public class Region implements ConfigSerializable {

	/**
	 * The name of the region, or null if not given
	 */
	@Getter
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
		if (this.primary == null || this.secondary == null)
			return null;

		Valid.checkBoolean(this.primary.getWorld().getName().equals(this.secondary.getWorld().getName()), "Points must be in one world! Primary: " + this.primary + " != secondary: " + this.secondary);

		final int x1 = this.primary.getBlockX(), x2 = this.secondary.getBlockX(),
				y1 = this.primary.getBlockY(), y2 = this.secondary.getBlockY(),
				z1 = this.primary.getBlockZ(), z2 = this.secondary.getBlockZ();

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
		Valid.checkBoolean(this.isWhole(), "Cannot perform getCenter on a non-complete region: " + this.toString());

		final Location[] centered = this.getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		return new Location(primary.getWorld(),
				(primary.getX() + secondary.getX()) / 2,
				(primary.getY() + secondary.getY()) / 2,
				(primary.getZ() + secondary.getZ()) / 2);
	}

	/**
	 * Return true if the primary location has been set
	 *
	 * @return
	 */
	public final boolean hasPrimary() {
		return this.primary != null;
	}

	/**
	 * Return true if the secondary location has been set
	 *
	 * @return
	 */
	public final boolean hasSecondary() {
		return this.secondary != null;
	}

	/**
	 * Return if the given location equals to the primary location.
	 * Returns false if this region has no primary location set.
	 *
	 * @param location
	 * @return
	 */
	public final boolean isPrimary(Location location) {
		return this.primary != null && Valid.locationEquals(this.primary, location);
	}

	/**
	 * Return if the given location equals to the secondary location.
	 * Returns false if this region has no secondary location set.
	 *
	 * @param location
	 * @return
	 */
	public final boolean isSecondary(Location location) {
		return this.secondary != null && Valid.locationEquals(this.secondary, location);
	}

	/**
	 * Return a close of the primary location
	 *
	 * @return the primary
	 */
	public final Location getPrimary() {
		return this.primary == null ? null : this.primary.clone();
	}

	/**
	 * Return a close of the secondary location
	 *
	 * @return the secondary
	 */
	public final Location getSecondary() {
		return this.secondary == null ? null : this.secondary.clone();
	}

	/**
	 * Count all blocks within this region
	 *
	 * @return
	 */
	public final List<Block> getBlocks() {
		Valid.checkBoolean(this.isWhole(), "Cannot perform getBlocks on a non-complete region: " + this.toString());
		final Location[] centered = this.getCorrectedPoints();

		return BlockUtil.getBlocks(centered[0], centered[1]);
	}

	/**
	 * Return locations representing the bounding box of a cuboid region,
	 * used when rendering particle effects
	 *
	 * @return
	 */
	public final Set<Location> getBoundingBox() {
		Valid.checkBoolean(this.isWhole(), "Cannot perform getBoundingBox on a non-complete region: " + this.toString());

		return BlockUtil.getBoundingBox(this.primary, this.secondary);
	}

	/**
	 * Count all entities within this region
	 *
	 * @return
	 */
	public final List<Entity> getEntities() {
		Valid.checkBoolean(this.isWhole(), "Cannot perform getEntities on a non-complete region: " + this.toString());

		final List<Entity> found = new LinkedList<>();

		final Location[] centered = this.getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		final int xMin = (int) primary.getX() >> 4;
		final int xMax = (int) secondary.getX() >> 4;
		final int zMin = (int) primary.getZ() >> 4;
		final int zMax = (int) secondary.getZ() >> 4;

		for (int cx = xMin; cx <= xMax; ++cx)
			for (int cz = zMin; cz <= zMax; ++cz)
				for (final Entity entity : this.getWorld().getChunkAt(cx, cz).getEntities())
					if (entity.isValid() && entity.getLocation() != null && this.isWithin(entity.getLocation()))
						found.add(entity);

		return found;
	}

	/**
	 * Get world for this region
	 *
	 * @return
	 */
	public final World getWorld() {
		if (!this.isWhole())
			return null;

		if (this.primary != null && this.secondary == null)
			return Bukkit.getWorld(this.primary.getWorld().getName());

		if (this.secondary != null && this.primary == null)
			return Bukkit.getWorld(this.secondary.getWorld().getName());

		Valid.checkBoolean(this.primary.getWorld().getName().equals(this.secondary.getWorld().getName()), "Worlds of this region not the same: " + this.primary.getWorld() + " != " + this.secondary.getWorld());
		return Bukkit.getWorld(this.primary.getWorld().getName());
	}

	/**
	 * Return true if the given point is within this region
	 *
	 * @param location
	 * @return
	 */
	public final boolean isWithin(@NonNull final Location location) {
		Valid.checkBoolean(this.isWhole(), "Cannot perform isWithin on a non-complete region: " + this.toString());

		if (!location.getWorld().getName().equals(this.primary.getWorld().getName()))
			return false;

		final Location[] centered = this.getCorrectedPoints();
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
		return this.primary != null && this.secondary != null;
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
	 * @param secondary
	 */
	public final void setSecondary(final Location secondary) {
		this.secondary = secondary;
	}

	/**
	 * Sets the location from the click type. LEFT = primary, RIGHT = secondary
	 *
	 * @param location
	 * @param click
	 */
	public final void setLocation(Location location, ClickType click) {
		this.setLocation(location, click, false);
	}

	/**
	 * Sets the location from the click type. LEFT = primary, RIGHT = secondary
	 *
	 * If the given location point exists, it will get removed. If it does
	 * not exist, it will get placed, creating a toggle on - toggle off effect.
	 *
	 * @param location
	 * @param click
	 * @return true if the location was set, null if it was removed (or location param is null)
	 */
	public final boolean toggleLocation(Location location, ClickType click) {
		return this.setLocation(location, click, true);
	}

	/*
	 * Helper method to set location from click type, removing old one if toggle mode
	 */
	private boolean setLocation(Location location, ClickType click, boolean toggle) {
		final boolean isPrimary = click == ClickType.LEFT;

		if (isPrimary) {
			if (location == null || (this.hasPrimary() && this.isPrimary(location) && toggle)) {
				this.setPrimary(null);

				return false;

			} else {
				this.setPrimary(location);

				return true;
			}

		} else if (location == null || (this.hasSecondary() && this.isSecondary(location) && toggle)) {
			this.setSecondary(null);

			return false;

		} else {
			this.setSecondary(location);

			return true;
		}
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Region) {
			final Region otherRegion = (Region) obj;

			if ((otherRegion.name != null && this.name == null) || (otherRegion.name == null && this.name != null))
				return false;

			if ((otherRegion.name != null && !otherRegion.name.equals(this.name)) || (otherRegion.name != null && !this.name.equals(otherRegion.name)))
				return false;

			return Valid.locationEquals(otherRegion.getPrimary(), this.primary) && Valid.locationEquals(otherRegion.getSecondary(), this.secondary);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.primary, this.secondary);
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName() + "{name=" + this.name + ",location=" + Common.shortLocation(this.primary) + " - " + Common.shortLocation(this.secondary) + "}";
	}

	/**
	 * Saves the region data into a map you can save in your yaml or json file
	 */
	@Override
	public final SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExist("Name", this.name);
		map.putIfExist("Primary", this.primary);
		map.putIfExist("Secondary", this.secondary);

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