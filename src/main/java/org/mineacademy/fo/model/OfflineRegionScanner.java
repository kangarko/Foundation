package org.mineacademy.fo.model;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
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
import org.mineacademy.fo.event.RegionScanCompleteEvent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.Setter;

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
	private static int WAIT_TIME_BETWEEN_SCAN_SECONDS = 1;

	/**
	 * Changing flag: How many files processed out of total?
	 */
	private int processedFilesCount = 0;

	/**
	 * Changing flag: The total amount of region files to scan
	 */
	private int totalFilesCount = 0;

	/**
	 * Changing flag: The world we are scanning
	 */
	@Getter
	private World world;

	/**
	 * Changing flag: Last time an operation was done successfully
	 */
	private long lastTick = System.currentTimeMillis();

	/**
	 * In fast mode we wont load chunks only return their x-z coordinates
	 *
	 * false = we call {@link #onChunkScan(Chunk)}
	 * true = we call {@link #onChunkScanFast(int, int)}
	 */
	@Setter
	private boolean fastMode = false;

	/**
	 * Starts the scan for the given world (warning: this operation is blocking
	 * and takes long time, see {@link #getEstimatedWaitTimeSec(World)})
	 *
	 * @param world
	 */
	public final void scan(World world) {
		final boolean hadAutoSave = world.isAutoSave();

		try {
			world.setAutoSave(false);
			this.scan0(world);

		} finally {
			world.setAutoSave(hadAutoSave);
		}
	}

	/*
	 * Invoke the main scan of all chunks within this world on the disk, both loaded and unloaded
	 */
	private void scan0(World world) {

		Common.log(
				Common.consoleLine(),
				"Scanning regions in " + world.getName(),
				Common.consoleLine());

		// Disable watch dog
		this.disableWatchdog();

		// Collect files
		final File[] files = getRegionFiles(world);

		if (files == null || files.length == 0) {
			Common.warning("Unable to locate the region files for: " + world.getName());

			return;
		}

		final Queue<File> queue = new LimitedQueue<>(files.length + 1);
		queue.addAll(Arrays.asList(files));

		this.totalFilesCount = files.length;
		this.world = world;

		// Start the schedule
		this.schedule0(queue);
	}

	/*
	 * Disable to prevent lag warnings since we scan chunks on the main thread
	 */
	private void disableWatchdog() {
		try {
			final Class<?> watchDog = Class.forName("org.spigotmc.WatchdogThread");
			final Method doStop = ReflectionUtil.getMethod(watchDog, "doStop");

			ReflectionUtil.invokeStatic(doStop);

		} catch (final ReflectiveOperationException err) {
			// pass through, probably not using Spigot
		}
	}

	/*
	 * Self-repeating cycle of loading chunks from the disk until
	 * we reach the end of the queue
	 */
	private void schedule0(Queue<File> queue) {
		new SimpleRunnable() {

			@Override
			public void run() {
				final File file = queue.poll();

				// Queue finished
				if (file == null) {
					Common.log(
							Common.consoleLine(),
							"Region scanner finished. World saved.",
							Common.consoleLine());

					Common.callEvent(new RegionScanCompleteEvent(OfflineRegionScanner.this.world));

					OfflineRegionScanner.this.onScanFinished();
					this.cancel();

					return;
				}

				OfflineRegionScanner.this.scanFile(file, queue);
			}
		}.runTask(SimplePlugin.getInstance());
	}

	/*
	 * Scans the given region file
	 */
	private void scanFile(File file, Queue<File> queue) {
		final Matcher matcher = FILE_PATTERN.matcher(file.getName());

		if (!matcher.matches())
			return;

		final int regionX = Integer.parseInt(matcher.group(1));
		final int regionZ = Integer.parseInt(matcher.group(2));

		System.out.print("[" + Math.round((double) this.processedFilesCount++ / (double) this.totalFilesCount * 100) + "%] Processing " + file);

		// Calculate time, collect memory and increase pauses in between if running out of memory
		if (System.currentTimeMillis() - this.lastTick > 4000) {
			final long free = Runtime.getRuntime().freeMemory() / 1_000_000;

			if (free < 200) {
				System.out.print(" [Low memory (" + free + "Mb)! Running GC and increasing delay between operations ..]");

				WAIT_TIME_BETWEEN_SCAN_SECONDS = +2;

				System.gc();
				Common.sleep(5_000);
			} else
				System.out.print(" [free memory = " + free + " mb]");

			this.lastTick = System.currentTimeMillis();
		}

		System.out.println();

		// Load the file
		final Object region = RegionAccessor.getRegionFile(this.world.getName(), file);

		// Load each chunk within that file
		scan:
		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++) {
				final int chunkX = x + (regionX << 5);
				final int chunkZ = z + (regionZ << 5);

				if (RegionAccessor.isChunkSaved(region, x, z))
					if (this.fastMode)
						this.onChunkScanFast(chunkX, chunkZ);

					else {
						final Chunk chunk = this.world.getChunkAt(chunkX, chunkZ);

						try {
							this.onChunkScan(chunk);

						} catch (final Throwable t) {
							Common.error(t, "Failed to scan chunk " + chunk + ", aborting for safety");

							break scan;
						}
					}
			}

		// Save
		try {
			RegionAccessor.save(region);

		} catch (final Throwable t) {
			Common.log("Failed to save region " + file + ", operation stopped.");
			Remain.sneaky(t);
		}

		if (this.fastMode)
			this.schedule0(queue);

		else
			Common.runLater(WAIT_TIME_BETWEEN_SCAN_SECONDS, () -> this.schedule0(queue));

	}

	/**
	 * Called when a chunk is being scanned and loaded
	 * ONLY CALLED WHEN FASTMODE IS NOT ENABLED (by default)
	 * ELSE WE CALL THE {@link #onChunkScanFast(int, int)} method you need to override
	 *
	 * @param chunk
	 */
	protected abstract void onChunkScan(Chunk chunk);

	/**
	 * Called when a chunk is being scanned and loaded
	 * ONLY CALLED WHEN FASTMODE IS ENABLED
	 *
	 * @param chunkX
	 * @param chunkZ
	 */
	protected void onChunkScanFast(int chunkX, int chunkZ) {
	}

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
		for (final String folder : FOLDERS) {
			final File file = new File(world.getWorldFolder(), folder);

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

		return (int) (Math.round(WAIT_TIME_BETWEEN_SCAN_SECONDS * 1.5D) * files.length);
	}
}

/**
 * Reflection helper class for accessing region files
 */
class RegionAccessor {

	private static Constructor<?> regionFileConstructor;
	private static Method isChunkSaved;

	private static final boolean atleast1_13, atleast1_14, atleast1_15, atleast1_16, atleast1_18;
	private static final String saveMethodName;

	static {
		atleast1_13 = MinecraftVersion.atLeast(V.v1_13);
		atleast1_14 = MinecraftVersion.atLeast(V.v1_14);
		atleast1_15 = MinecraftVersion.atLeast(V.v1_15);
		atleast1_16 = MinecraftVersion.atLeast(V.v1_16);
		atleast1_18 = MinecraftVersion.atLeast(V.v1_18);

		saveMethodName = atleast1_13 ? "close" : "c";

		try {
			final Class<?> regionFileClass = ReflectionUtil.getNMSClass("RegionFile", "net.minecraft.world.level.chunk.storage.RegionFile");
			regionFileConstructor = atleast1_18 ? regionFileClass.getConstructor(Path.class, Path.class, boolean.class)
					: atleast1_16 ? regionFileClass.getConstructor(File.class, File.class, boolean.class)
							: atleast1_15 ? regionFileClass.getConstructor(File.class, File.class)
									: regionFileClass.getConstructor(File.class);

			isChunkSaved = atleast1_14 ? regionFileClass.getMethod("b", ReflectionUtil.getNMSClass("ChunkCoordIntPair", "net.minecraft.world.level.ChunkCoordIntPair"))
					: regionFileClass.getMethod(atleast1_13 ? "b" : "c", int.class, int.class);

		} catch (final ReflectiveOperationException ex) {
			Remain.sneaky(ex);
		}
	}

	static Object getRegionFile(String worldName, File file) {
		try {
			final File container = new File(Bukkit.getWorldContainer(), worldName);

			return atleast1_18 ? regionFileConstructor.newInstance(file.toPath(), container.toPath(), false)
					: atleast1_16 ? regionFileConstructor.newInstance(file, container, false)
							: atleast1_15 ? regionFileConstructor.newInstance(file, container)
									: regionFileConstructor.newInstance(file);

		} catch (final Throwable ex) {
			throw new RuntimeException("Could not create region file from " + file, ex);
		}
	}

	static boolean isChunkSaved(Object region, int x, int z) {
		try {
			if (MinecraftVersion.newerThan(V.v1_13)) {
				final Object chunkCoordinates = ReflectionUtil.getNMSClass("ChunkCoordIntPair", "net.minecraft.world.level.ChunkCoordIntPair")
						.getConstructor(int.class, int.class).newInstance(x, z);

				return (boolean) isChunkSaved.invoke(region, chunkCoordinates);
			}

			return (boolean) isChunkSaved.invoke(region, x, z);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not find if region file " + region + " has chunk at " + x + " " + z);
		}
	}

	static void save(Object region) {
		try {
			region.getClass().getDeclaredMethod(saveMethodName).invoke(region);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Error saving region " + region);
		}
	}
}
