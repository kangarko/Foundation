package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import com.google.common.collect.Sets;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Utility class for block manipulation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlockUtil {

	/**
	 * Matches all DOUBLE or STEP block names
	 */
	private static final Pattern SLAB_PATTERN = Pattern.compile("(?!DOUBLE).*STEP");

	/**
	 * The block faces we use while searching for all parts of the given
	 * tree upwards
	 */
	private static final BlockFace[] TREE_TRUNK_FACES = {
			BlockFace.UP, BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH
	};

	/**
	 * A list of safe blocks upon which a tree naturally grows
	 */
	private final static Set<String> TREE_GROUND_BLOCKS = Sets.newHashSet("GRASS_BLOCK", "COARSE_DIRT", "DIRT", "MYCELIUM", "PODZOL");

	/**
	 * The vertical gaps when creating locations for a bounding box,
	 * see {@link #getBoundingBox(Location, Location)}
	 */
	public static double BOUNDING_VERTICAL_GAP = 1;

	/**
	 * The horizontal gaps when creating locations for a bounding box,
	 * see {@link #getBoundingBox(Location, Location)}
	 */
	public static double BOUNDING_HORIZONTAL_GAP = 1;

	// ------------------------------------------------------------------------------------------------------------
	// Cuboid region manipulation
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the given location is within the two vector cuboid bounds
	 *
	 * @param location
	 * @param primary
	 * @param secondary
	 * @return
	 */
	public static boolean isWithinCuboid(final Location location, final Location primary, final Location secondary) {
		final double locX = location.getX();
		final double locY = location.getY();
		final double locZ = location.getZ();

		final int x = primary.getBlockX();
		final int y = primary.getBlockY();
		final int z = primary.getBlockZ();

		final int x1 = secondary.getBlockX();
		final int y1 = secondary.getBlockY();
		final int z1 = secondary.getBlockZ();

		if (locX >= x && locX <= x1 || locX <= x && locX >= x1)
			if (locZ >= z && locZ <= z1 || locZ <= z && locZ >= z1)
				if (locY >= y && locY <= y1 || locY <= y && locY >= y1)
					return true;

		return false;
	}

	/**
	 * Return locations representing the bounding box of a chunk,
	 * used when rendering particle effects for example.
	 *
	 * @param chunk
	 * @return
	 */
	public static Set<Location> getBoundingBox(@NonNull Chunk chunk) {
		final int minX = chunk.getX() << 4;
		final int minY = 0;
		final int minZ = chunk.getZ() << 4;

		final int maxX = minX | 15;
		final int maxY = chunk.getWorld().getMaxHeight();
		final int maxZ = minZ | 15;

		final Location primary = new Location(chunk.getWorld(), minX, minY, minZ);
		final Location secondary = new Location(chunk.getWorld(), maxX, maxY, maxZ);

		return getBoundingBox(primary, secondary);
	}

	/**
	 * Return locations representing the bounding box of a cuboid region,
	 * used when rendering particle effects
	 *
	 * @param primary
	 * @param secondary
	 * @return
	 */
	public static Set<Location> getBoundingBox(final Location primary, final Location secondary) {
		final List<VectorHelper> shape = new ArrayList<>();

		final VectorHelper min = getMinimumPoint(primary, secondary);
		final VectorHelper max = getMaximumPoint(primary, secondary).add(1, 0, 1);

		final int height = getHeight(primary, secondary);

		final List<VectorHelper> bottomCorners = new ArrayList<>();

		bottomCorners.add(new VectorHelper(min.getX(), min.getY(), min.getZ()));
		bottomCorners.add(new VectorHelper(max.getX(), min.getY(), min.getZ()));
		bottomCorners.add(new VectorHelper(max.getX(), min.getY(), max.getZ()));
		bottomCorners.add(new VectorHelper(min.getX(), min.getY(), max.getZ()));

		for (int i = 0; i < bottomCorners.size(); i++) {
			final VectorHelper p1 = bottomCorners.get(i);
			final VectorHelper p2 = i + 1 < bottomCorners.size() ? (VectorHelper) bottomCorners.get(i + 1) : (VectorHelper) bottomCorners.get(0);

			final VectorHelper p3 = p1.add(0, height, 0);
			final VectorHelper p4 = p2.add(0, height, 0);
			shape.addAll(plotLine(p1, p2));
			shape.addAll(plotLine(p3, p4));
			shape.addAll(plotLine(p1, p3));

			for (double offset = BOUNDING_VERTICAL_GAP; offset < height; offset += BOUNDING_VERTICAL_GAP) {
				final VectorHelper p5 = p1.add(0.0D, offset, 0.0D);
				final VectorHelper p6 = p2.add(0.0D, offset, 0.0D);
				shape.addAll(plotLine(p5, p6));
			}
		}

		final Set<Location> locations = new HashSet<>();

		for (final VectorHelper vector : shape)
			locations.add(new Location(primary.getWorld(), vector.getX(), vector.getY(), vector.getZ()));

		return locations;
	}

	private static List<VectorHelper> plotLine(final VectorHelper p1, final VectorHelper p2) {
		final List<VectorHelper> ShapeVectors = new ArrayList<>();

		final int points = (int) (p1.distance(p2) / BOUNDING_HORIZONTAL_GAP) + 1;
		final double length = p1.distance(p2);
		final double gap = length / (points - 1);

		final VectorHelper gapShapeVector = p2.subtract(p1).normalize().multiply(gap);

		for (int i = 0; i < points; i++) {
			final VectorHelper currentPoint = p1.add(gapShapeVector.multiply(i));

			ShapeVectors.add(currentPoint);
		}

		return ShapeVectors;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Spherical manipulation
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get all locations within the given 3D spherical radius, hollow or not
	 * <p>
	 * NOTE: Calling this operation causes performance penaulty (>100ms for 30 radius!), be careful.
	 *
	 * @param location
	 * @param radius
	 * @param hollow
	 * @return
	 */
	public static Set<Location> getSphere(final Location location, final int radius, final boolean hollow) {
		final Set<Location> blocks = new HashSet<>();
		final World world = location.getWorld();
		final int X = location.getBlockX();
		final int Y = location.getBlockY();
		final int Z = location.getBlockZ();
		final int radiusSquared = radius * radius;

		if (hollow) {
			for (int x = X - radius; x <= X + radius; x++)
				for (int y = Y - radius; y <= Y + radius; y++)
					for (int z = Z - radius; z <= Z + radius; z++)
						if ((X - x) * (X - x) + (Y - y) * (Y - y) + (Z - z) * (Z - z) <= radiusSquared)
							blocks.add(new Location(world, x, y, z));

			return makeHollow(blocks, true);
		}

		for (int x = X - radius; x <= X + radius; x++)
			for (int y = Y - radius; y <= Y + radius; y++)
				for (int z = Z - radius; z <= Z + radius; z++)
					if ((X - x) * (X - x) + (Y - y) * (Y - y) + (Z - z) * (Z - z) <= radiusSquared)
						blocks.add(new Location(world, x, y, z));

		return blocks;
	}

	/**
	 * Get all locations within the given 2D circle radius, hollow or full circle
	 * <p>
	 * NOTE: Calling this operation causes performance penaulty (>100ms for 30 radius!), be careful.
	 *
	 * @param location
	 * @param radius
	 * @param hollow
	 * @return
	 */
	public static Set<Location> getCircle(final Location location, final int radius, final boolean hollow) {
		final Set<Location> blocks = new HashSet<>();
		final World world = location.getWorld();

		final int initialX = location.getBlockX();
		final int initialY = location.getBlockY();
		final int initialZ = location.getBlockZ();
		final int radiusSquared = radius * radius;

		if (hollow) {
			for (int x = initialX - radius; x <= initialX + radius; x++)
				for (int z = initialZ - radius; z <= initialZ + radius; z++)
					if ((initialX - x) * (initialX - x) + (initialZ - z) * (initialZ - z) <= radiusSquared)
						blocks.add(new Location(world, x, initialY, z));

			return makeHollow(blocks, false);
		}

		for (int x = initialX - radius; x <= initialX + radius; x++)
			for (int z = initialZ - radius; z <= initialZ + radius; z++)
				if ((initialX - x) * (initialX - x) + (initialZ - z) * (initialZ - z) <= radiusSquared)
					blocks.add(new Location(world, x, initialY, z));

		return blocks;
	}

	/**
	 * Creates a new list of outer location points from all given points
	 *
	 * @param blocks
	 * @param sphere
	 * @return
	 */
	private static Set<Location> makeHollow(final Set<Location> blocks, final boolean sphere) {
		final Set<Location> edge = new HashSet<>();

		if (!sphere) {
			for (final Location location : blocks) {
				final World world = location.getWorld();
				final int x = location.getBlockX();
				final int y = location.getBlockY();
				final int z = location.getBlockZ();

				final Location front = new Location(world, x + 1, y, z);
				final Location back = new Location(world, x - 1, y, z);
				final Location left = new Location(world, x, y, z + 1);
				final Location right = new Location(world, x, y, z - 1);

				if (!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right)))
					edge.add(location);

			}
			return edge;
		}

		for (final Location location : blocks) {
			final World world = location.getWorld();

			final int x = location.getBlockX();
			final int y = location.getBlockY();
			final int z = location.getBlockZ();

			final Location front = new Location(world, x + 1, y, z);
			final Location back = new Location(world, x - 1, y, z);
			final Location left = new Location(world, x, y, z + 1);
			final Location right = new Location(world, x, y, z - 1);
			final Location top = new Location(world, x, y + 1, z);
			final Location bottom = new Location(world, x, y - 1, z);

			if (!(blocks.contains(front) && blocks.contains(back) && blocks.contains(left) && blocks.contains(right) && blocks.contains(top) && blocks.contains(bottom)))
				edge.add(location);
		}

		return edge;

	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting blocks within a cuboid
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all blocks within the two cuboid bounds (may take a while)
	 *
	 * @param primary
	 * @param secondary
	 * @return
	 */
	public static List<Block> getBlocks(final Location primary, final Location secondary) {
		Valid.checkNotNull(primary, "Primary region point must be set!");
		Valid.checkNotNull(secondary, "Secondary region point must be set!");

		final List<Block> blocks = new ArrayList<>();

		final int topBlockX = primary.getBlockX() < secondary.getBlockX() ? secondary.getBlockX() : primary.getBlockX();
		final int bottomBlockX = primary.getBlockX() > secondary.getBlockX() ? secondary.getBlockX() : primary.getBlockX();

		final int topBlockY = primary.getBlockY() < secondary.getBlockY() ? secondary.getBlockY() : primary.getBlockY();
		final int bottomBlockY = primary.getBlockY() > secondary.getBlockY() ? secondary.getBlockY() : primary.getBlockY();

		final int topBlockZ = primary.getBlockZ() < secondary.getBlockZ() ? secondary.getBlockZ() : primary.getBlockZ();
		final int bottomBlockZ = primary.getBlockZ() > secondary.getBlockZ() ? secondary.getBlockZ() : primary.getBlockZ();

		for (int x = bottomBlockX; x <= topBlockX; x++)
			for (int z = bottomBlockZ; z <= topBlockZ; z++)
				for (int y = bottomBlockY; y <= topBlockY; y++) {
					final Block block = primary.getWorld().getBlockAt(x, y, z);

					if (block != null)
						blocks.add(block);
				}

		return blocks;
	}

	/**
	 * Return all blocks in the given chunk
	 *
	 * @param chunk
	 * @return
	 */
	public static List<Block> getBlocks(@NonNull Chunk chunk) {
		final List<Block> blocks = new ArrayList<>();

		final int minX = chunk.getX() << 4;
		final int minZ = chunk.getZ() << 4;

		final int maxX = minX | 15;
		final int maxY = chunk.getWorld().getMaxHeight();
		final int maxZ = minZ | 15;

		for (int x = minX; x <= maxX; ++x)
			for (int y = 0; y <= maxY; ++y)
				for (int z = minZ; z <= maxZ; ++z)
					blocks.add(chunk.getBlock(x, y, z));

		return blocks;
	}

	/**
	 * Get all the blocks in a specific area centered around the Location passed in
	 *
	 * @param loc
	 * @param height
	 * @param radius
	 * @return
	 */
	public static List<Block> getBlocks(final Location loc, final int height, final int radius) {
		final List<Block> blocks = new ArrayList<>();

		for (int y = 0; y < height; y++)
			for (int x = -radius; x <= radius; x++)
				for (int z = -radius; z <= radius; z++) {
					final Block checkBlock = loc.getBlock().getRelative(x, y, z);

					if (checkBlock != null && checkBlock.getType() != Material.AIR)
						blocks.add(checkBlock);
				}
		return blocks;
	}

	/**
	 * Return chunks around the given location
	 *
	 * @param location
	 * @param radius
	 * @return
	 */
	public static List<Chunk> getChunks(final Location location, final int radius) {
		final HashSet<Chunk> addedChunks = new HashSet<>();
		final World world = location.getWorld();

		final int chunkX = location.getBlockX() >> 4;
		final int chunkZ = location.getBlockZ() >> 4;

		for (int x = chunkX - radius; x <= chunkX + radius; ++x)
			for (int z = chunkZ - radius; z <= chunkZ + radius; ++z)
				if (world.isChunkLoaded(x, z))
					addedChunks.add(world.getChunkAt(x, z));

		return new ArrayList<>(addedChunks);
	}

	/**
	 * Return all x-z locations within a chunk
	 *
	 * @param chunk
	 * @return
	 */
	public static List<Location> getXZLocations(Chunk chunk) {
		final List<Location> found = new ArrayList<>();

		final int chunkX = chunk.getX() << 4;
		final int chunkZ = chunk.getZ() << 4;

		for (int x = chunkX; x < chunkX + 16; x++)
			for (int z = chunkZ; z < chunkZ + 16; z++)
				found.add(new Location(chunk.getWorld(), x, 0, z));

		return found;
	}

	/**
	 * Return all leaves/logs upwards connected to that given tree block.
	 * Parts are sorted according to their Y coordinate from lowest to highest.
	 *
	 * @param treeBase
	 * @return
	 */
	public static List<Block> getTreePartsUp(final Block treeBase) {
		final Material baseMaterial = treeBase.getState().getType();

		final String logType = MinecraftVersion.atLeast(V.v1_13) ? baseMaterial.toString() : "LOG";
		final String leaveType = MinecraftVersion.atLeast(V.v1_13) ? logType.replace("_LOG", "") + "_LEAVES" : "LEAVES";

		final Set<Block> treeParts = new HashSet<>();
		final Set<Block> toSearch = new HashSet<>();
		final Set<Block> searched = new HashSet<>();

		toSearch.add(treeBase.getRelative(BlockFace.UP));
		searched.add(treeBase);

		int cycle;

		for (cycle = 0; cycle < 1000 && !toSearch.isEmpty(); cycle++) {
			final Block block = toSearch.iterator().next();

			toSearch.remove(block);
			searched.add(block);

			if (block.getType().toString().equals(logType) || block.getType().toString().equals(leaveType)) {
				treeParts.add(block);

				for (final BlockFace face : TREE_TRUNK_FACES) {
					final Block relative = block.getRelative(face);

					if (!searched.contains(relative))
						toSearch.add(relative);

				}

			} else if (!block.getType().isTransparent())
				return new ArrayList<>();
		}

		return new ArrayList<>(treeParts);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Block type checkers
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns true whether the given block is a "LOG" type and we perform a search
	 * down to the bottom most connected block to find if that stands on tree ground blocks.
	 *
	 * @param treeBaseBlock
	 * @return
	 */
	public static boolean isLogOnGround(Block treeBaseBlock) {
		// Validates the block passed in is actually a log
		if (!(CompMaterial.isLog(treeBaseBlock.getType())))
			return false;

		// Reach for the bottom most tree-like block
		while (CompMaterial.isLog(treeBaseBlock.getType()))
			treeBaseBlock = treeBaseBlock.getRelative(BlockFace.DOWN);

		return TREE_GROUND_BLOCKS.contains(CompMaterial.fromMaterial(treeBaseBlock.getType()).toString());
	}

	/**
	 * Will a FallingBlock which lands on this Material break and drop to the
	 * ground?
	 *
	 * @param material to check
	 * @return boolean
	 */
	public static boolean isBreakingFallingBlock(final Material material) {
		return material.isTransparent() &&
				material != CompMaterial.NETHER_PORTAL.getMaterial() &&
				material != CompMaterial.END_PORTAL.getMaterial() ||
				material == CompMaterial.COBWEB.getMaterial() ||
				material == Material.DAYLIGHT_DETECTOR ||
				CompMaterial.isTrapDoor(material) ||
				material == CompMaterial.OAK_SIGN.getMaterial() ||
				CompMaterial.isWallSign(material) ||
				// Match all slabs besides double slab
				SLAB_PATTERN.matcher(material.name()).matches();
	}

	/**
	 * Return true when the given material is a tool, e.g. doesn't stack
	 *
	 * @param material
	 * @return
	 */
	public static boolean isTool(final Material material) {
		return material.name().endsWith("AXE") // axe & pickaxe
				|| material.name().endsWith("SPADE")
				|| material.name().endsWith("SWORD")
				|| material.name().endsWith("HOE")
				|| material.name().endsWith("BUCKET") // water, milk, lava,..
				|| material == CompMaterial.BOW.getMaterial()
				|| material == CompMaterial.FISHING_ROD.getMaterial()
				|| material == CompMaterial.CLOCK.getMaterial()
				|| material == CompMaterial.COMPASS.getMaterial()
				|| material == CompMaterial.FLINT_AND_STEEL.getMaterial();
	}

	/**
	 * Return true if the material is an armor
	 *
	 * @param material
	 * @return
	 */
	public static boolean isArmor(final Material material) {
		return material.name().endsWith("HELMET")
				|| material.name().endsWith("CHESTPLATE")
				|| material.name().endsWith("LEGGINGS")
				|| material.name().endsWith("BOOTS");
	}

	/**
	 * Returns true if block is safe to select
	 *
	 * @param material the material
	 * @return
	 */
	public static boolean isForBlockSelection(final Material material) {
		if (!material.isBlock() || material == Material.AIR)
			return false;

		try {
			if (material.isInteractable()) // Ignore chests etc.
				return false;

		} catch (final Throwable t) {
		}

		try {
			if (material.hasGravity()) // Ignore falling blocks
				return false;
		} catch (final Throwable t) {
		}

		return material.isSolid();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Finding blocks and locations
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Scan the location from top to bottom to find the highest Y coordinate that is not air and not snow.
	 * This will return the free coordinate above the snow layer.
	 *
	 * @param location
	 * @return the y coordinate, or -1 if not found
	 */
	public static int findHighestBlockNoSnow(final Location location) {
		return findHighestBlockNoSnow(location.getWorld(), location.getBlockX(), location.getBlockZ());
	}

	/**
	 * Scan the location from top to bottom to find the highest Y coordinate that is not air and not snow.
	 * This will return the free coordinate above the snow layer.
	 *
	 * @param world
	 * @param x
	 * @param z
	 * @return the y coordinate, or -1 if not found
	 */
	public static int findHighestBlockNoSnow(final World world, final int x, final int z) {
		for (int y = world.getMaxHeight() - 1; y > 0; y--) {
			final Block block = world.getBlockAt(x, y, z);

			if (!CompMaterial.isAir(block)) {

				if (block.getType() == CompMaterial.SNOW_BLOCK.getMaterial())
					return -1;

				if (block.getType() == CompMaterial.SNOW.getMaterial())
					continue;

				return y + 1;
			}
		}

		return -1;
	}

	/**
	 * Scans the location from top to bottom to find the highest Y non-air coordinate that matches
	 * the given predicate.
	 *
	 * @param location
	 * @param predicate
	 * @return
	 */
	public static int findHighestBlock(final Location location, final Predicate<Material> predicate) {
		return findHighestBlock(location.getWorld(), location.getBlockX(), location.getBlockZ(), predicate);
	}

	/**
	 * Scans the location from top to bottom to find the highest Y non-air coordinate that matches
	 * the given predicate. For nether worlds, we recommend you see {@link #findHighestNetherAirBlock(World, int, int)}
	 *
	 * @param world
	 * @param x
	 * @param z
	 * @param predicate
	 * @return the y coordinate, or -1 if not found
	 */
	public static int findHighestBlock(final World world, final int x, final int z, final Predicate<Material> predicate) {
		for (int y = world.getMaxHeight() - 1; y > 0; y--) {
			final Block block = world.getBlockAt(x, y, z);

			if (block != null && !CompMaterial.isAir(block) && predicate.test(block.getType()))
				return y + 1;
		}

		return -1;

	}

	/**
	 * @see #findHighestNetherAirBlock(World, int, int)
	 *
	 * @param location
	 * @return
	 */
	public static int findHighestNetherAirBlock(@NonNull Location location) {
		return findHighestNetherAirBlock(location.getWorld(), location.getBlockX(), location.getBlockZ());
	}

	/**
	 * Returns the first air block that has air block above it and a solid block below. Useful for finding
	 * nether location from the bottom up to spawn mobs (not spawning them on the top bedrock as per {@link #findHighestBlock(Location, Predicate)}).
	 *
	 * @param world
	 * @param x
	 * @param z
	 * @return
	 */
	public static int findHighestNetherAirBlock(@NonNull World world, int x, int z) {
		Valid.checkBoolean(world.getEnvironment() == Environment.NETHER, "findHighestNetherAirBlock must be called in nether worlds, " + world.getName() + " is of type " + world.getEnvironment());

		for (int y = 0; y < world.getMaxHeight(); y++) {
			final Block block = world.getBlockAt(x, y, z);
			final Block above = block.getRelative(BlockFace.UP);
			final Block below = block.getRelative(BlockFace.DOWN);

			if (block != null && CompMaterial.isAir(block) && CompMaterial.isAir(above) && !CompMaterial.isAir(below) && below.getType().isSolid())
				return y;
		}

		return -1;
	}

	/**
	 * Returns the closest location to the given one of the given locations
	 *
	 * @param location
	 * @param locations
	 * @return
	 */
	public static Location findClosestLocation(Location location, List<Location> locations) {
		locations = new ArrayList<>(locations);
		final Location playerLocation = location;

		Collections.sort(locations, (f, s) -> Double.compare(f.distance(playerLocation), s.distance(playerLocation)));
		return locations.get(0);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Shooting blocks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Shoot the given block to the sky with the given velocity (maybe your arrow velocity?)
	 * and can even make the block burn on impact. The shot block is set to air
	 *
	 * @param block
	 * @param velocity
	 * @return
	 */
	public static FallingBlock shootBlock(final Block block, final Vector velocity) {
		return shootBlock(block, velocity, 0D);
	}

	/**
	 * Shoot the given block to the sky with the given velocity (maybe your arrow velocity?)
	 * and can even make the block burn on impact. The shot block is set to air
	 *
	 * We adjust velocity a bit using random to add a bit for more realism, if you do not
	 * want this, use {@link #spawnFallingBlock(Block, Vector)}
	 *
	 * @param block
	 * @param velocity
	 * @param burnOnFallChance from 0.0 to 1.0
	 * @return
	 */
	public static FallingBlock shootBlock(final Block block, final Vector velocity, final double burnOnFallChance) {
		if (!canShootBlock(block))
			return null;

		final FallingBlock falling = Remain.spawnFallingBlock(block.getLocation().clone().add(0.5, 0, 0.5), block.getType());

		{ // Set velocity to reflect the given velocity but change a bit for more realism
			final double x = MathUtil.range(velocity.getX(), -2, 2) * 0.5D;
			final double y = Math.random();
			final double z = MathUtil.range(velocity.getZ(), -2, 2) * 0.5D;

			falling.setVelocity(new Vector(x, y, z));
		}

		if (RandomUtil.chanceD(burnOnFallChance) && block.getType().isBurnable())
			scheduleBurnOnFall(falling);

		// Prevent drop
		falling.setDropItem(false);

		// Remove the block
		block.setType(Material.AIR);

		return falling;
	}

	/**
	 * Just spawns the falling block without adjusting its velocity
	 *
	 * @param block
	 * @param velocity
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Block block, final Vector velocity) {
		final FallingBlock falling = Remain.spawnFallingBlock(block.getLocation().clone().add(0.5, 0, 0.5), block.getType());

		// Apply velocity
		falling.setVelocity(velocity);

		// Prevent drop
		falling.setDropItem(false);

		// Remove the block
		block.setType(Material.AIR);

		return falling;
	}

	/**
	 * Return the allowed material types to shoot this block
	 *
	 * @param block
	 * @return
	 */
	private static boolean canShootBlock(final Block block) {
		final Material material = block.getType();

		return !CompMaterial.isAir(material) && (material.toString().contains("STEP") || material.toString().contains("SLAB") || BlockUtil.isForBlockSelection(material));
	}

	/**
	 * Schedule to set the flying block on fire upon impact
	 *
	 * @param block
	 */
	private static void scheduleBurnOnFall(final FallingBlock block) {
		EntityUtil.trackFalling(block, () -> {
			final Block upperBlock = block.getLocation().getBlock().getRelative(BlockFace.UP);

			if (upperBlock.getType() == Material.AIR)
				upperBlock.setType(Material.FIRE);
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helper classes
	// ------------------------------------------------------------------------------------------------------------

	private static VectorHelper getMinimumPoint(final Location pos1, final Location pos2) {
		return new VectorHelper(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
	}

	private static VectorHelper getMaximumPoint(final Location pos1, final Location pos2) {
		return new VectorHelper(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
	}

	private static int getHeight(final Location pos1, final Location pos2) {
		final VectorHelper min = getMinimumPoint(pos1, pos2);
		final VectorHelper max = getMaximumPoint(pos1, pos2);

		return (int) (max.getY() - min.getY() + 1.0D);
	}

	@RequiredArgsConstructor
	private final static class VectorHelper {

		@Getter
		protected final double x, y, z;

		public VectorHelper add(final VectorHelper other) {
			return this.add(other.x, other.y, other.z);
		}

		public VectorHelper add(final double x, final double y, final double z) {
			return new VectorHelper(this.x + x, this.y + y, this.z + z);
		}

		public VectorHelper subtract(final VectorHelper other) {
			return this.subtract(other.x, other.y, other.z);
		}

		public VectorHelper subtract(final double x, final double y, final double z) {
			return new VectorHelper(this.x - x, this.y - y, this.z - z);
		}

		public VectorHelper multiply(final double n) {
			return new VectorHelper(this.x * n, this.y * n, this.z * n);
		}

		public VectorHelper divide(final double n) {
			return new VectorHelper(this.x / n, this.y / n, this.z / n);
		}

		public double length() {
			return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
		}

		public double distance(final VectorHelper other) {
			return Math.sqrt(Math.pow(other.x - this.x, 2) +
					Math.pow(other.y - this.y, 2) +
					Math.pow(other.z - this.z, 2));
		}

		public VectorHelper normalize() {
			return this.divide(this.length());
		}

		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof VectorHelper))
				return false;

			final VectorHelper other = (VectorHelper) obj;
			return other.x == this.x && other.y == this.y && other.z == this.z;
		}

		@Override
		public String toString() {
			return "(" + this.x + ", " + this.y + ", " + this.z + ")";
		}
	}
}
