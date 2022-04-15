package org.mineacademy.fo.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.NonNull;

/**
 * Represents a simple database where values are flattened and stored
 * by {@link UUID}.
 * <p>
 * The table structure is as follows:
 * <p>
 * UUID varchar(64) | Name text       | Data text      | Updated bigint
 * ------------------------------------------------------------
 * Player's uuid    | Last known name | {json data}    | Date of last save call
 * <p>
 * We use JSON to flatten those values and provide convenience methods
 * onLoad and onSave for you to override so that you can easily save/load data to MySQL.
 * <p>
 * Also see getExpirationDays(), by default we remove values not touched
 * within the last 90 days.
 * <p>
 * For a less-restricting solution see {@link SimpleDatabase} however you will
 * need to run own queries and implement own table structure that requires MySQL
 * command syntax knowledge.
 *
 * @param <T> the model you use to load/save entries, such as your player cache
 */
public abstract class SimpleFlatDatabase<T> extends SimpleDatabase {

	/**
	 * An internal flag to prevent dead lock so that we do not call any
	 * more queries within the {@link #load(UUID, Object)} or {@link #save(UUID, Object)} methods
	 */
	private boolean isQuerying = false;

	/**
	 * Creates the table if it does not exist
	 * <p>
	 * To override this override {@link #onConnectFinish()}
	 */
	@Override
	protected final void onConnected() {

		// First, see if the database exists, create it if not
		update("CREATE TABLE IF NOT EXISTS {table}(UUID varchar(64), Name text, Data text, Updated bigint)");

		// Remove entries that have not been updated in the last X days
		removeOldEntries();

		// Call any hooks
		onConnectFinish();
	}

	/**
	 * You can override this to run code after the connection was made and
	 * the table created as well as purged ({@link #removeOldEntries()})
	 */
	protected void onConnectFinish() {
	}

	/**
	 * Remove entries that have not been updated (called {@link #save(Identifiable)} method) for the
	 * last given X amount of days
	 */
	private void removeOldEntries() {
		final long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(getExpirationDays());

		update("DELETE FROM {table} WHERE Updated < " + threshold + "");
	}

	/**
	 * When you call the save method, we write the last updated time to the entry.
	 * On plugin loading we can remove entries that have not been saved/updated
	 * for the given amount of days.
	 * <p>
	 * Default: 90 days
	 *
	 * @return
	 */
	protected int getExpirationDays() {
		return 90;
	}

	/**
	 * Load the data for the given unique ID and his cache
	 *
	 * @param uuid
	 * @param cache
	 */
	public final void load(final UUID uuid, final T cache) {
		if (!isLoaded() || isQuerying)
			return;

		try {
			LagCatcher.start("mysql");
			isQuerying = true;

			Debugger.debug("mysql", "---------------- MySQL - Loading data for " + uuid);

			final ResultSet resultSet = query("SELECT * FROM {table} WHERE UUID='" + uuid + "'");
			final String dataRaw = resultSet.next() ? resultSet.getString("Data") : "{}";
			Debugger.debug("mysql", "JSON: " + dataRaw);

			final SerializedMap data = SerializedMap.fromJson(dataRaw);
			Debugger.debug("mysql", "Deserialized data: " + data);

			// Call the user specified load method
			onLoad(data, cache);

			// Close connection at the end
			resultSet.close();

		} catch (final Throwable t) {
			Common.error(t,
					"Failed to load data from MySQL!",
					"UUID: " + uuid,
					"Error: %error");

		} finally {
			isQuerying = false;

			logPerformance("loading");
		}
	}

	/**
	 * Your method to load the data for the given unique ID and his cache
	 *
	 * @param map  the map that is automatically converted from the JSON array
	 *             stored in the database
	 * @param data the data you want to fill out to
	 */
	protected abstract void onLoad(SerializedMap map, T data);

	/**
	 * Save the data for the given name, unique ID and his cache
	 * <p>
	 * If the onSave returns empty data we delete the row
	 *
	 * @param name  last known name - players may change those
	 * @param uuid
	 * @param cache
	 */
	public final void save(final String name, final UUID uuid, final T cache) {
		if (!isLoaded() || isQuerying)
			return;

		try {
			LagCatcher.start("mysql");
			isQuerying = true;

			// Save using the user configured save method
			final SerializedMap data = onSave(cache);

			Debugger.debug("mysql", "---------------- MySQL - Saving data for " + uuid);
			Debugger.debug("mysql", "Raw data: " + data);
			Debugger.debug("mysql", "JSON: " + (data == null ? "null" : data.toJson()));

			// Remove data if empty
			if (data == null || data.isEmpty()) {
				update("DELETE FROM {table} WHERE UUID= '" + uuid + "';");

				if (Debugger.isDebugged("mysql"))
					Debugger.debug("mysql", "Data was empty, row has been removed.");

			} else if (isStored(uuid))
				update("UPDATE {table} SET Data='" + data.toJson() + "', Updated='" + System.currentTimeMillis() + "' WHERE UUID='" + uuid + "';");
			else
				update("INSERT INTO {table}(UUID, Name, Data, Updated) VALUES ('" + uuid + "', '" + name + "', '" + data.toJson() + "', '" + System.currentTimeMillis() + "');");

		} catch (final Throwable ex) {
			Common.error(ex,
					"Failed to save data to MySQL!",
					"UUID: " + uuid,
					"Error: %error");

		} finally {
			isQuerying = false;

			logPerformance("saving");
		}
	}

	/**
	 * Utility method to finish LagCatcher mysql measure and log
	 * if there was some lag, or if we detected mysql being run
	 * from the main thread.
	 *
	 * @param operation
	 */
	private void logPerformance(final String operation) {
		final boolean isMainThread = Bukkit.isPrimaryThread();

		LagCatcher.end("mysql", isMainThread ? 10 : MathUtil.atLeast(200, SimpleSettings.LAG_THRESHOLD_MILLIS),
				ChatUtil.capitalize(operation) + " data to MySQL took {time} ms" + (isMainThread ? " - To prevent slowing the server, " + operation + " can be made async (carefully)" : ""));
	}

	/**
	 * Checks if the given unique id is stored in the database
	 *
	 * @param uuid
	 * @return
	 * @throws SQLException
	 */
	private boolean isStored(@NonNull final UUID uuid) throws SQLException {
		final ResultSet resultSet = query("SELECT * FROM {table} WHERE UUID= '" + uuid.toString() + "'");

		if (resultSet == null)
			return false;

		if (resultSet.next())
			return resultSet.getString("UUID") != null;

		return false;
	}

	/**
	 * Your method to save the data for the given unique ID and his cache
	 * <p>
	 * Return an empty data to delete the row
	 *
	 * @param data
	 * @return
	 */
	protected abstract SerializedMap onSave(T data);
}
