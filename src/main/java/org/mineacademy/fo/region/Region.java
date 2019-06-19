package org.mineacademy.fo.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.model.ConfigSerializable;

/**
 * The protected arena region
 */
public interface Region extends ConfigSerializable {

	/**
	 * Get the primary region point
	 *
	 * @return
	 */
	Location getPrimary();

	/**
	 * Get the secondary region point
	 *
	 * @return
	 */
	Location getSecondary();

	/**
	 * Get the center point of the region, roughly
	 *
	 * @return
	 */
	Location getCenter();

	/**
	 * Get all blocks within the region
	 *
	 * @return
	 */
	Block[] getBlocks();

	/**
	 * Ret all regions within this region
	 *
	 * @return
	 */
	Entity[] getEntities();

	/**
	 * Get the region world
	 *
	 * @return
	 */
	World getWorld();

	boolean isWithin(Location loc);
}
