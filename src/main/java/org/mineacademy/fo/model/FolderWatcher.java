package org.mineacademy.fo.model;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(value = AccessLevel.PROTECTED)
public abstract class FolderWatcher extends Thread {

	/**
	 * A list to help Foundation stop threads on reload
	 */
	private static volatile Set<FolderWatcher> activeThreads = new HashSet<>();

	/**
	 * Stop all active threads
	 */
	public static void stopThreads() {
		for (final FolderWatcher thread : activeThreads)
			thread.stopWatching();

		activeThreads.clear();
	}

	/**
	 * Workaround for duplicated values in the loop
	 */
	private final Map<String, BukkitTask> scheduledUpdates = new HashMap<>();

	/**
	 * The folder that is being watched
	 */
	private final Path folder;

	/**
	 * A one-way flag used to stop the thread while loop deadlock
	 */
	@Getter
	private boolean watching = true;

	/**
	 * Start a new file watcher and start watching the given folder
	 *
	 * @param folder
	 */
	public FolderWatcher(File folder) {
		Valid.checkBoolean(folder.exists(), folder + " does not exists!");
		Valid.checkBoolean(folder.isDirectory(), folder + " must be a directory!");

		this.folder = folder.toPath();
		this.start();

		for (final FolderWatcher other : activeThreads)
			//Valid.checkBoolean(other.folder.toString().equals(this.folder.toString()), "Tried to add a duplicate file watcher for " + this.folder);
			if (other.folder.toString().equals(this.folder.toString()))
				Common.warning("A duplicate file watcher for '" + folder.getPath() + "' was added. This is untested and may causes fatal issues!");

		activeThreads.add(this);

		Debugger.debug("upload", "Started folder watcher for " + folder + " in " + folder.getAbsolutePath() + " (path: " + this.folder + ")");
	}

	/**
	 * Starts watching the given folder and reporting changes
	 */
	@Override
	public final void run() {
		final FileSystem fileSystem = this.folder.getFileSystem();

		try (WatchService service = fileSystem.newWatchService()) {
			final WatchKey registration = this.folder.register(service, ENTRY_MODIFY);

			while (this.watching)
				//synchronized (activeThreads) {
				try {
					final WatchKey watchKey = service.take();

					for (final WatchEvent<?> watchEvent : watchKey.pollEvents()) {
						final Kind<?> kind = watchEvent.kind();

						if (kind == ENTRY_MODIFY) {
							final Path watchEventPath = (Path) watchEvent.context();
							final File fileModified = new File(SimplePlugin.getData(), watchEventPath.toFile().getName());

							final String path = fileModified.getAbsolutePath();
							final BukkitTask pendingTask = this.scheduledUpdates.remove(path);

							// Cancel the old task and reschedule
							if (pendingTask != null)
								pendingTask.cancel();

							// Force run sync -- reschedule five seconds later to ensure no further edits take place
							this.scheduledUpdates.put(path, Common.runLater(10, () -> {
								if (!this.watching)
									return;

								try {
									this.onModified(fileModified);

									this.scheduledUpdates.remove(path);

								} catch (final Throwable t) {
									Common.error(t, "Error in calling onModified when watching changed file " + fileModified);
								}
							}));

							break;
						}
					}

					if (!watchKey.reset())
						Common.error(new FoException("Failed to reset watch key! Restarting sync engine.."));

				} catch (final Throwable t) {
					Common.error(t, "Error in handling watching thread loop for folder " + this.getFolder());
				}
			//}

			registration.cancel();

		} catch (final Throwable t) {
			Common.error(t, "Error in initializing watching thread loop for folder " + this.getFolder());
		}

	}

	/**
	 * Called automatically when the file gets modified
	 *
	 * @param file
	 */
	protected abstract void onModified(File file);

	/**
	 * Stops listening for folder changes
	 */
	public void stopWatching() {
		Valid.checkBoolean(this.watching, "The folder watcher for folder " + this.folder + " is no longer watching!");

		this.watching = false;

		for (final BukkitTask task : this.scheduledUpdates.values())
			try {
				task.cancel();
			} catch (final Exception ex) {
				// ignore
			}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FolderWatcher && ((FolderWatcher) obj).folder.toString().equals(this.folder.toString());
	}
}