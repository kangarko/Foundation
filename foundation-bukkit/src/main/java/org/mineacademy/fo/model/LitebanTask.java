package org.mineacademy.fo.model;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;

import lombok.Getter;

/**
 * A task that fetches mutes from LiteBans and caches them in memory.
 *
 * @deprecated internal use only, see {@link HookManager} for public API
 */
@Deprecated
public final class LitebanTask extends BukkitRunnable {

	@Getter
	private final static LitebanTask instance = new LitebanTask();

	private final Map<String, Long> mutedPlayersByUniqueId = new HashMap<>();
	private Object apiInstance;
	private Method methodPrepareStatement;

	LitebanTask() {
		final Class<?> classDatabase;

		try {
			classDatabase = Class.forName("litebans.api.Database");

		} catch (final ClassNotFoundException ex) {
			CommonCore.log("LiteBans API not found, skipping integration.");

			return;
		}

		this.apiInstance = ReflectionUtil.invokeStatic(classDatabase, "get");
		this.methodPrepareStatement = ReflectionUtil.getMethod(classDatabase, "prepareStatement", String.class);
	}

	@Override
	public void run() {
		if (this.methodPrepareStatement == null)
			return;

		this.mutedPlayersByUniqueId.clear();

		try (PreparedStatement statement = ReflectionUtil.invoke(this.methodPrepareStatement, this.apiInstance, "SELECT * FROM {mutes}")) {
			statement.execute();

			final ResultSet resultSet = statement.getResultSet();

			while (resultSet.next()) {
				final String uuid = resultSet.getString("UUID");
				final boolean active = resultSet.getBoolean("ACTIVE");
				final long until = resultSet.getLong("UNTIL");

				if (active) {
					if (until != 0 && until < System.currentTimeMillis())
						continue;

					this.mutedPlayersByUniqueId.put(uuid, until);
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

				try {
					this.cancel();
				} catch (final Throwable tt) {
					// Not scheduled yet, ignore
				}
			}
		}
	}

	public boolean isMuted(UUID uuid) {
		return this.mutedPlayersByUniqueId.containsKey(uuid.toString());
	}

	public long getUnmuteTime(UUID uuid) {
		return this.mutedPlayersByUniqueId.getOrDefault(uuid.toString(), 0L);
	}
}
