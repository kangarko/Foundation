package org.mineacademy.fo.region;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents an incomplete region with one or no points set
 */
@Getter
public final class RegionIncomplete implements Region {

	/**
	 * The primary region position
	 */
	private final Location primary;

	/**
	 * The secondary region position
	 */
	private final Location secondary;

	/**
	 * Create a new incomplete region
	 *
	 * @param primary
	 * @param secondary
	 */
	public RegionIncomplete(@Nullable Location primary, @Nullable Location secondary) {
		this.primary = primary != null && primary.getWorld() != null ? primary : null;
		this.secondary = secondary != null && secondary.getWorld() != null ? secondary : null;
	}

	/**
	 * @deprecated unsupported
	 */
	@Deprecated
	@Override
	public Location getCenter() {
		throw new FoException("Region incomplete");
	}

	/**
	 * @deprecated unsupported
	 */
	@Deprecated
	@Override
	public List<Entity> getEntities() {
		throw new FoException("Region incomplete");
	}

	/**
	 * @deprecated unsupported
	 */
	@Deprecated
	@Override
	public List<Block> getBlocks() {
		throw new FoException("Region incomplete");
	}

	/**
	 * @deprecated unsupported
	 */
	@Deprecated
	@Override
	public World getWorld() {
		throw new FoException("Region incomplete");
	}

	/**
	 * @deprecated unsupported
	 */
	@Deprecated
	@Override
	public boolean isWithin(@NonNull Location loc) {
		throw new FoException("Region incomplete");
	}

	/**
	 * @deprecated unsupported
	 */
	@Deprecated
	@Override
	public SerializedMap serialize() {
		return new SerializedMap();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + (primary != null ? Common.shortLocation(primary) : "null") + " - " + (secondary != null ? Common.shortLocation(secondary) : "null") + "}";
	}
}