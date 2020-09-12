package org.mineacademy.fo.model;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(value = AccessLevel.PROTECTED)
public abstract class FolderWatcher extends Thread {

	/**
	 * Workaround for duplicated values in the loop
	 */
	private static volatile ExpiringMap<Path, File> justModified = ExpiringMap.builder().expiration(10, TimeUnit.MILLISECONDS).build();

	/**
	 * The folder that is being watched
	 */
	private final Path folder;

	/**
	 * Start a new file watcher and start watching the given folder
	 *
	 * @param folder
	 */
	public FolderWatcher(File folder) {
		Valid.checkBoolean(folder.isDirectory(), folder + " must be a directory!");

		this.folder = folder.toPath();
		this.start();
	}

	/**
	 * Starts watching the given folder and reporting changes
	 */
	@Override
	public final void run() {
		final FileSystem fileSystem = folder.getFileSystem();

		try (WatchService service = fileSystem.newWatchService()) {
			folder.register(service, ENTRY_MODIFY);

			while (true) {
				try {
					final WatchKey watchKey = service.take();

					for (final WatchEvent<?> watchEvent : watchKey.pollEvents()) {
						final Kind<?> kind = watchEvent.kind();

						if (justModified.containsKey(this.folder))
							break;

						if (kind == ENTRY_MODIFY) {
							final Path watchEventPath = (Path) watchEvent.context();
							final File fileModified = new File(SimplePlugin.getData(), watchEventPath.toFile().getName());

							// Force run sync
							Common.runLater(() -> {
								try {
									onModified(fileModified);

								} catch (final Throwable t) {
									Common.error(t, "Error in calling onModified when watching changed file " + fileModified);
								}
							});

							justModified.put(this.folder, fileModified);
						}
					}

					if (!watchKey.reset())
						break;

				} catch (final Throwable t) {
					Common.error(t, "Error in handling watching thread loop for folder " + this.getFolder());
				}
			}

		} catch (final IOException ex) {
			Common.error(ex, "Error in initializing watching thread loop for folder " + this.getFolder());
		}
	}

	/**
	 * Called automatically when the file gets modified
	 *
	 * @param file
	 */
	protected abstract void onModified(File file);
}