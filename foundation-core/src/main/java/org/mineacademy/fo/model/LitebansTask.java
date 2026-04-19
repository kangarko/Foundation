package org.mineacademy.fo.model;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.platform.Platform;

import litebans.api.Database;
import litebans.api.Entry;
import litebans.api.Events;

/**
 * Event-driven LiteBans integration. Listens for mute add/remove events via
 * the official LiteBans API and performs a single async lookup per player join
 * to cover pre-existing mutes. Replaces the old polling-based approach that
 * exhausted the connection pool when multiple MineAcademy plugins were loaded.
 *
 * Works on all platforms (Bukkit, BungeeCord, Velocity).
 *
 * @deprecated internal use only, on Bukkit see HookManager for public API
 */
@Deprecated
public final class LitebansTask {

	private final static LitebansTask instance = new LitebansTask();

	private final ConcurrentMap<String, Long> mutedPlayersByUniqueId = new ConcurrentHashMap<>();

	private volatile boolean enabled = false;

	private volatile Object registeredListener;

	private LitebansTask() {
	}

	public static LitebansTask getInstance() {
		return instance;
	}

	/**
	 * Register the LiteBans events listener. No-op if the LiteBans API is not on the classpath.
	 * Safe to call from any thread. Idempotent.
	 */
	public void enable() {
		if (this.enabled)
			return;

		try {
			Class.forName("litebans.api.Events");

		} catch (final ClassNotFoundException ex) {
			CommonCore.log("LiteBans API not found, skipping integration.");

			return;
		}

		try {
			this.registeredListener = LitebansHook.registerListener(this.mutedPlayersByUniqueId);
			this.enabled = true;

		} catch (final Throwable t) {
			CommonCore.error(t, "Failed to hook into LiteBans, got: " + t.getMessage() + " (unless you explicitly need this integration, you can ignore this error)");
		}
	}

	/**
	 * Unregister the LiteBans events listener and clear the cache.
	 */
	public void disable() {
		if (!this.enabled)
			return;

		try {
			LitebansHook.unregisterListener(this.registeredListener);

		} catch (final Throwable t) {
			CommonCore.error(t, "Failed to unregister LiteBans listener");
		}

		this.mutedPlayersByUniqueId.clear();
		this.registeredListener = null;
		this.enabled = false;
	}

	/**
	 * Called when a player joins. Performs a single async lookup to seed the cache
	 * for pre-existing mutes (entries added before our listener was registered, or
	 * on other network nodes with broadcast sync disabled).
	 *
	 * Safe to call from the main thread: schedules an async task internally.
	 */
	public void onPlayerJoin(final UUID uniqueId) {
		if (!this.enabled)
			return;

		Platform.runTaskAsync(() -> {
			try {
				LitebansHook.lookupAndCache(this.mutedPlayersByUniqueId, uniqueId);

			} catch (final Throwable t) {
				CommonCore.error(t, "Failed to look up LiteBans mute for " + uniqueId);
			}
		});
	}

	/**
	 * Called when a player quits. Evicts the cache entry to keep memory bounded.
	 */
	public void onPlayerQuit(final UUID uniqueId) {
		this.mutedPlayersByUniqueId.remove(uniqueId.toString());
	}

	@Deprecated
	public boolean isMuted(final UUID uniqueId) {
		final Long until = this.mutedPlayersByUniqueId.get(uniqueId.toString());

		if (until == null)
			return false;

		if (until > 0 && until < System.currentTimeMillis()) {
			this.mutedPlayersByUniqueId.remove(uniqueId.toString());

			return false;
		}

		return true;
	}

	@Deprecated
	public long getUnmuteTime(final UUID uniqueId) {
		final Long until = this.mutedPlayersByUniqueId.get(uniqueId.toString());

		return until == null ? 0L : until;
	}

	/**
	 * All direct references to litebans.api.* are confined to this nested class.
	 * The JVM only loads this class (and thus the LiteBans classes) when a method
	 * here is first invoked, which only happens after we verified via Class.forName
	 * that the LiteBans API is present on the classpath.
	 */
	private static final class LitebansHook {

		static Object registerListener(final ConcurrentMap<String, Long> cache) {
			final Events.Listener listener = new Events.Listener() {

				@Override
				public void entryAdded(final Entry entry) {
					applyEntry(cache, entry);
				}

				@Override
				public void entryRemoved(final Entry entry) {
					if ("mute".equals(entry.getType()) && entry.getUuid() != null)
						cache.remove(entry.getUuid());
				}
			};

			Events.get().register(listener);

			return listener;
		}

		static void unregisterListener(final Object listener) {
			if (listener instanceof Events.Listener)
				Events.get().unregister((Events.Listener) listener);
		}

		static void lookupAndCache(final ConcurrentMap<String, Long> cache, final UUID uniqueId) {
			if (Database.get().isPlayerMuted(uniqueId, null)) {
				final Entry mute = Database.get().getMute(uniqueId, null, null);

				if (mute != null)
					applyEntry(cache, mute);
			} else
				cache.remove(uniqueId.toString());
		}

		static void applyEntry(final ConcurrentMap<String, Long> cache, final Entry entry) {
			if (!"mute".equals(entry.getType()) || !entry.isActive() || entry.getUuid() == null)
				return;

			final long until = entry.isPermanent() ? 0L : entry.getDateEnd();

			cache.put(entry.getUuid(), until);
		}
	}
}
