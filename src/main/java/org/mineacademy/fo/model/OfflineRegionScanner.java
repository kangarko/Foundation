package org.mineacademy.fo.model;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.ReflectionException;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.event.RegionScanCompleteEvent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompRunnable;
import org.mineacademy.fo.remain.Remain;

/**
 * A class that has ability to scan saved regions on the disk and execute
 * actions for each saved chunk.
 */
public abstract class OfflineRegionScanner {

	/**
	 * Folders that will be scanned
	 */
	private static final String[] FOLDERS = { "region", "DIM-1/region", "DIM1/region" };

	/**
	 * A valid file pattern
	 */
	private static final Pattern FILE_PATTERN = Pattern.compile("r\\.(.+)\\.(.+)\\.mca");

	/**
	 * Seconds between each file processing operation.
	 */
	private static int OPERATION_WAIT_SECONDS = 1;

	/**
	 * Changing flag: How many files processed out of total?
	 */
	private int done = 0;

	/**
	 * Changing flag: The total amount of region files to scan
	 */
	private int totalFiles = 0;

	/**
	 * Changing flag: The world we are scanning
	 */
	private World world;

	/**
	 * Changing flag: Last time an operation was done successfully
	 */
	private long lastTick = System.currentTimeMillis();

	/**
	 * Starts the scan for the given world (warning: this operation is blocking
	 * and takes long time, see {@link #getEstimatedWaitTimeSec(World)})
	 *
	 * @param world
	 */
	public final void scan(World world) {
		scan0(world);
	}

	private final void scan0(World world) {
		Thread watchdog = null;

		try {
			// Disable to prevent lag warnings since we scan chunks on the main thread
			final Field f = Class.forName("org.spigotmc.WatchdogThread").getDeclaredField("instance");

			try {
				f.setAccessible(true);

				watchdog = (Thread) f.get(null);
				watchdog.suspend();

			} catch (final Throwable t) {
				Bukkit.getLogger().severe("FAILED TO DISABLE WATCHDOG, ABORTING! See below and report to us. NO DATA WERE MANIPULATED.");
				Common.callEvent(new RegionScanCompleteEvent(world));

				t.printStackTrace();
				return;
			}
		} catch (final ReflectiveOperationException err) {
			// pass through
		}

		Bukkit.getLogger().info(Common.consoleLine());
		Bukkit.getLogger().info("Scanning regions in " + world.getName());
		Bukkit.getLogger().info(Common.consoleLine());

		LagCatcher.start("Region scanner for " + world.getName());

		final File[] files = getRegionFiles(world);

		if (files == null) {
			Bukkit.getLogger().warning("Unable to locate the region files for: " + world.getName());

			return;
		}

		final Queue<File> queue = new LimitedQueue<>(files.length + 1);
		queue.addAll(Arrays.asList(files));

		this.totalFiles = files.length;
		this.world = world;

		// Start the schedule
		schedule(world.getName(), queue);

		if (watchdog != null)
			watchdog.resume();

		LagCatcher.end("Region scanner for " + world.getName(), true);
	}

	/**
	 * Self-repeating cycle of loading chunks from the disk until
	 * we reach the end of the queue
	 *
	 * @param queue
	 */
	private final void schedule(String worldName, Queue<File> queue) {
		new CompRunnable() {

			@Override
			public void run() {
				final File file = queue.poll();

				// Queue finished
				if (file == null) {
					Bukkit.getLogger().info(Common.consoleLine());
					Bukkit.getLogger().info("Region scanner finished.");
					Bukkit.getLogger().info(Common.consoleLine());

					Common.callEvent(new RegionScanCompleteEvent(world));
					onScanFinished();

					cancel();
					return;
				}

				scanFile(worldName, file);

				Common.runLater(20 * OPERATION_WAIT_SECONDS, () -> schedule(worldName, queue));
			}
		}.runTask(SimplePlugin.getInstance());
	}

	/**
	 * Scans the given region file
	 *
	 * @param file
	 */
	private final void scanFile(String worldName, File file) {
		final Matcher matcher = FILE_PATTERN.matcher(file.getName());

		if (!matcher.matches())
			return;

		final int regionX = Integer.parseInt(matcher.group(1));
		final int regionZ = Integer.parseInt(matcher.group(2));

		System.out.print("[" + Math.round((double) done++ / (double) totalFiles * 100) + "%] Processing " + file);

		// Calculate time, collect memory and increase pauses in between if running out of memory
		if (System.currentTimeMillis() - lastTick > 4000) {
			final long free = Runtime.getRuntime().freeMemory() / 1_000_000;

			if (free < 200) {
				System.out.print(" [Low memory (" + free + "Mb)! Running GC and increasing delay between operations ..]");

				OPERATION_WAIT_SECONDS = +2;

				System.gc();
				Common.sleep(5_000);
			} else
				System.out.print(" [free memory = " + free + " mb]");

			lastTick = System.currentTimeMillis();
		}

		System.out.println();

		// Load the file
		final Object region = RegionAccessor.getRegionFile(worldName, file);

		// Load each chunk within that file
		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++) {
				final int chunkX = x + (regionX << 5);
				final int chunkZ = z + (regionZ << 5);

				if (RegionAccessor.isChunkSaved(region, x, z)) {
					final Chunk chunk = world.getChunkAt(chunkX, chunkZ);

					onChunkScan(chunk);
				}
			}

		// Save
		try {
			RegionAccessor.save(region);

		} catch (final Throwable t) {
			Bukkit.getLogger().severe("Failed to save region " + file + ", operation stopped.");
			Remain.sneaky(t);
		}
	}

	/**
	 * Called when a chunk is being scanned and loaded
	 *
	 * @param chunk
	 */
	protected abstract void onChunkScan(Chunk chunk);

	/**
	 * Called when the scan is finished, after {@link RegionScanCompleteEvent}
	 */
	protected void onScanFinished() {
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return all region files stored on the disk for the given world
	 *
	 * @param world
	 * @return
	 */
	public static File[] getRegionFiles(World world) {
		final File regionDir = getRegionDirectory(world);

		return regionDir == null ? null : regionDir.listFiles((FilenameFilter) (dir, name) -> name.toLowerCase().endsWith(".mca"));
	}

	/**
	 * Return the region directory for the given world
	 *
	 * @param world
	 * @return
	 */
	private static final File getRegionDirectory(World world) {
		for (final String f : FOLDERS) {
			final File file = new File(world.getWorldFolder(), f);

			if (file.isDirectory())
				return file;
		}

		return null;
	}

	/**
	 * Get how long scanning should take for the given world
	 * depending on its amount of region files
	 *
	 * @param world
	 * @return
	 */
	public static int getEstimatedWaitTimeSec(World world) {
		final File[] files = getRegionFiles(world);

		return (OPERATION_WAIT_SECONDS + 2) * files.length;
	}
}

/**
 * Reflection helper class for accessing region files
 */
class RegionAccessor {

	private static Constructor<?> regionFileConstructor;
	private static Method isChunkSaved;

	private static boolean atleast1_13, atleast1_14, atleast1_15, atleast1_16;
	private static final String saveMethodName;

	static {
		atleast1_13 = MinecraftVersion.atLeast(V.v1_13);
		atleast1_14 = MinecraftVersion.atLeast(V.v1_14);
		atleast1_15 = MinecraftVersion.atLeast(V.v1_15);
		atleast1_16 = MinecraftVersion.atLeast(V.v1_16);

		saveMethodName = atleast1_13 ? "close" : "c";

		try {
			final Class<?> regionFileClass = ReflectionUtil.getNMSClass("RegionFile");
			regionFileConstructor = atleast1_16 ? regionFileClass.getConstructor(File.class, File.class, boolean.class) : atleast1_15 ? regionFileClass.getConstructor(File.class, File.class) : regionFileClass.getConstructor(File.class);
			isChunkSaved = atleast1_14 ? regionFileClass.getMethod("b", ReflectionUtil.getNMSClass("ChunkCoordIntPair")) : regionFileClass.getMethod(atleast1_13 ? "b" : "c", int.class, int.class);

		} catch (final ReflectiveOperationException ex) {
			Remain.sneaky(ex);
		}
	}

	static Object getRegionFile(String worldName, File file) {
		try {
			final File container = new File(Bukkit.getWorldContainer(), worldName);

			return atleast1_16 ? regionFileConstructor.newInstance(file, container, true) : atleast1_15 ? regionFileConstructor.newInstance(file, container) : regionFileConstructor.newInstance(file);
		} catch (final Throwable ex) {
			throw new RuntimeException("Could not create region file from " + file, ex);
		}
	}

	static boolean isChunkSaved(Object region, int x, int z) {
		try {
			if (MinecraftVersion.newerThan(V.v1_13)) {
				final Object chunkCoordinates = ReflectionUtil.getNMSClass("ChunkCoordIntPair").getConstructor(int.class, int.class).newInstance(x, z);

				return (boolean) isChunkSaved.invoke(region, chunkCoordinates);
			}

			return (boolean) isChunkSaved.invoke(region, x, z);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Could not find if region file " + region + " has chunk at " + x + " " + z, ex);
		}
	}

	static void save(Object region) {
		try {
			region.getClass().getDeclaredMethod(saveMethodName).invoke(region);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Error saving region " + region, ex);
		}
	}
}
