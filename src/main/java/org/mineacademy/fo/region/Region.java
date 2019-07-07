package org.mineacademy.fo.region;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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
@Getter
public class Region implements ConfigSerializable {

	/**
	 * The name of the region, or null if not given
	 */
	@Nullable
	@Setter
	private String name;

	/**
	 * The primary region position
	 */
	private final Location primary;

	/**
	 * The secondary region position
	 */
	private final Location secondary;

	public Region(Location primary, Location secondary) {
		this(null, primary, secondary);
	}

	public Region(String name, @NonNull Location primary, @NonNull Location secondary) {
		Valid.checkNotNull(primary.getWorld(), "Primary location lacks a world!");
		Valid.checkNotNull(secondary.getWorld(), "Primary location lacks a world!");
		Valid.checkBoolean(primary.getWorld().getName().equals(secondary.getWorld().getName()), "Points must be in one world! Primary: " + primary + " != secondary: " + secondary);

		this.name = name;

		final int x1 = primary.getBlockX(), x2 = secondary.getBlockX(),
				y1 = primary.getBlockY(), y2 = secondary.getBlockY(),
				z1 = primary.getBlockZ(), z2 = secondary.getBlockZ();

		this.primary = primary.clone();
		this.secondary = secondary.clone();

		this.primary.setX(Math.min(x1, x2));
		this.primary.setY(Math.min(y1, y2));
		this.primary.setZ(Math.min(z1, z2));

		this.secondary.setX(Math.max(x1, x2));
		this.secondary.setY(Math.max(y1, y2));
		this.secondary.setZ(Math.max(z1, z2));
	}

	public final Location getCenter() {
		return new Location(primary.getWorld(),
				(primary.getX() + secondary.getX()) / 2,
				(primary.getY() + secondary.getY()) / 2,
				(primary.getZ() + secondary.getZ()) / 2);
	}

	public final List<Block> getBlocks() {
		return BlockUtil.getBlocks(primary, secondary);
	}

	public final List<Entity> getEntities() {
		final List<Entity> found = new LinkedList<>();

		final int xMin = (int) primary.getX() >> 4;
		final int xMax = (int) secondary.getX() >> 4;
		final int zMin = (int) primary.getZ() >> 4;
		final int zMax = (int) secondary.getZ() >> 4;

		for (int cx = xMin; cx <= xMax; ++cx)
			for (int cz = zMin; cz <= zMax; ++cz)
				for (final Entity en : getWorld().getChunkAt(cx, cz).getEntities())
					if (en.isValid() && en.getLocation() != null && isWithin(en.getLocation()))
						found.add(en);

		return found;
	}

	public final World getWorld() {
		Valid.checkBoolean(primary.getWorld().equals(secondary.getWorld()), "Worlds of this region not the same: " + primary.getWorld() + " != " + secondary.getWorld());

		return primary.getWorld();
	}

	public final boolean isWithin(@NonNull Location loc) {
		if (!loc.getWorld().equals(primary.getWorld()))
			return false;

		final int x = (int) loc.getX();
		final int y = (int) loc.getY();
		final int z = (int) loc.getZ();

		return x >= primary.getX() && x <= secondary.getX()
				&& y >= primary.getY() && y <= secondary.getY()
				&& z >= primary.getZ() && z <= secondary.getZ();
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "{" + Common.shortLocation(primary) + " - " + Common.shortLocation(secondary) + "}";
	}

	/**
	 * Saves the region data into a map you can save in your yaml or json file
	 */
	@Override
	public final SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExist("Name", name);
		map.put("Primary", primary);
		map.put("Secondary", secondary);

		return map;
	}

	/**
	 * Converts a saved map from your yaml/json file into a region if it contains Primary and Secondary keys
	 *
	 * @param map
	 * @return
	 */
	public static Region deserialize(SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = map.getLocation("Primary");
		final Location sec = map.getLocation("Secondary");

		return new Region(name, prim, sec);
	}
}