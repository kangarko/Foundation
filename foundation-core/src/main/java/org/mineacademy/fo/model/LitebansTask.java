package org.mineacademy.fo.model;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;

import lombok.Getter;

/**
 * A task that fetches mutes from LiteBans and caches them in memory.
 *
 * Works on all platforms (Bukkit, BungeeCord, Velocity).
 *
 * @deprecated internal use only, on Bukkit see HookManager for public API
 */
@Deprecated
public final class LitebansTask implements Runnable {

	@Getter
	private final static LitebansTask instance = new LitebansTask();

	private volatile Map<String, Long> mutedPlayersByUniqueId = Collections.emptyMap();
	private Object apiInstance;
	private Method methodPrepareStatement;
	private volatile boolean cancelled = false;

	@Deprecated
	LitebansTask() {
		final Class<?> classDatabase;

		try {
			classDatabase = Class.forName("litebans.api.Database");

		} catch (final ClassNotFoundException ex) {
			CommonCore.log("LiteBans API not found, skipping integration.");

			return;
		}

		try {
			this.apiInstance = ReflectionUtil.invokeStatic(classDatabase, "get");
			this.methodPrepareStatement = ReflectionUtil.getMethod(classDatabase, "prepareStatement", String.class);

		} catch (final Throwable t) {
			CommonCore.log("Failed to hook into LiteBans, got: " + t.getMessage() + " (unless you explicitly need this integration, you can ignore this error)");
		}
	}

	@Deprecated
	@Override
	public void run() {
		if (this.cancelled || this.methodPrepareStatement == null)
			return;

		final Map<String, Long> freshMap = new HashMap<>();

		try (PreparedStatement statement = ReflectionUtil.invoke(this.methodPrepareStatement, this.apiInstance, "SELECT * FROM {mutes}")) {
			statement.execute();

			try (final ResultSet resultSet = statement.getResultSet()) {
				while (resultSet.next()) {
					final String uuid = resultSet.getString("UUID");
					final boolean active = resultSet.getBoolean("ACTIVE");
					final long until = resultSet.getLong("UNTIL");

					if (active) {
						if (until > 0 && until < System.currentTimeMillis())
							continue;

						freshMap.put(uuid, until);
					}
				}
			}

		} catch (final IllegalStateException | SQLException ex) {
			// Ignore

		} catch (Throwable t) {
			while (t.getCause() != null)
				t = t.getCause();

			if (t instanceof IllegalStateException || t instanceof SQLException) {
				// ignore

			} else {
				CommonCore.error(t, "Error while fetching mutes from LiteBans, aborting. Is the integration outdated?");

				this.cancelled = true;
			}
		}

		this.mutedPlayersByUniqueId = freshMap;
	}

	@Deprecated
	public boolean isMuted(UUID uuid) {
		return this.mutedPlayersByUniqueId.containsKey(uuid.toString());
	}

	@Deprecated
	public long getUnmuteTime(UUID uuid) {
		return this.mutedPlayersByUniqueId.getOrDefault(uuid.toString(), 0L);
	}
}
