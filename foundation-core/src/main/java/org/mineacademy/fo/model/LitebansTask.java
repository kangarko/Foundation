package org.mineacademy.fo.model;

import java.util.Set;
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

	/**
	 * UUIDs whose join-time async lookup has not yet succeeded. Triggers a
	 * synchronous fallback lookup on the next {@link #isMuted(UUID)} call,
	 * and is cleared the moment any successful lookup or live entryAdded
	 * event populates the cache for the player.
	 */
	private final Set<String> pendingLookupUniqueIds = ConcurrentHashMap.newKeySet();

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
			this.registeredListener = LitebansHook.registerListener(this.mutedPlayersByUniqueId);
			this.enabled = true;

		} catch (final Throwable t) {
			// Typically MissingImplementationException when the LiteBans plugin is detected
			// (Platform#isPluginInstalled returned true at the call site) but its API has
			// not finished initializing yet, e.g. due to plugin load order on Velocity, or
			// when another plugin shaded the litebans.api.* classes without LiteBans itself
			// being installed. Log a warning rather than CommonCore#error, which would
			// auto-report this expected condition as a crash.
			CommonCore.warning("Failed to hook into LiteBans (" + t.getClass().getSimpleName()
					+ (t.getMessage() != null ? ": " + t.getMessage() : "")
					+ "). LiteBans integration disabled for this session.");
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
		this.pendingLookupUniqueIds.clear();
		this.registeredListener = null;
		this.enabled = false;
	}

	/**
	 * Called when a player joins. Schedules a single async lookup to seed the cache
	 * for pre-existing mutes (entries added before our listener was registered, on
	 * offline players, or on other network nodes with broadcast sync disabled).
	 *
	 * Transient LiteBans DB failures (e.g. {@code SQLNonTransientConnectionException}
	 * from an idle pool socket closed by the MariaDB server) leave the UUID marked
	 * pending so {@link #isMuted(UUID)} performs a synchronous fallback lookup on
	 * the next chat event rather than silently letting a muted player through.
	 *
	 * Safe to call from the main thread: schedules an async task internally.
	 */
	public void onPlayerJoin(final UUID uniqueId) {
		if (!this.enabled)
			return;

		this.pendingLookupUniqueIds.add(uniqueId.toString());
		Platform.runTaskAsync(() -> this.tryLookup(uniqueId, false));
	}

	/*
	 * Run the LiteBans mute lookup. Clears the pending flag on success.
	 * Async-only failures are warned (so server owners see the DB issue once);
	 * synchronous-fallback failures are silent because they would otherwise spam
	 * the console with one warning per chat message during a LiteBans outage.
	 */
	private void tryLookup(final UUID uniqueId, final boolean fromSyncFallback) {
		try {
			LitebansHook.lookupAndCache(this.mutedPlayersByUniqueId, uniqueId);
			this.pendingLookupUniqueIds.remove(uniqueId.toString());

		} catch (final Throwable t) {
			if (!fromSyncFallback)
				CommonCore.warning("LiteBans mute lookup failed for " + uniqueId + " (" + t.getClass().getSimpleName()
						+ (t.getMessage() != null ? ": " + t.getMessage() : "")
						+ "). Will retry on the next mute check for this player.");
		}
	}

	/**
	 * Called when a player quits. Evicts the cache entry to keep memory bounded.
	 */
	public void onPlayerQuit(final UUID uniqueId) {
		final String key = uniqueId.toString();

		this.mutedPlayersByUniqueId.remove(key);
		this.pendingLookupUniqueIds.remove(key);
	}

	@Deprecated
	public boolean isMuted(final UUID uniqueId) {
		final String key = uniqueId.toString();

		// Synchronous fallback: if the join-time async lookup failed, query LiteBans
		// now so a transient DB hiccup does not let a muted player chat freely. Chat
		// events are processed off the main thread on modern Paper / Folia, so a
		// brief blocking DB call here is acceptable. Self-healing: the pending flag
		// is cleared on the first success, after which this branch is skipped.
		if (this.enabled && this.pendingLookupUniqueIds.contains(key))
			this.tryLookup(uniqueId, true);

		final Long until = this.mutedPlayersByUniqueId.get(key);

		if (until == null)
			return false;

		if (until > 0 && until < System.currentTimeMillis()) {
			this.mutedPlayersByUniqueId.remove(key);

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
