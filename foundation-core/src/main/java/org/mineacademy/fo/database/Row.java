package org.mineacademy.fo.database;

import java.sql.SQLException;

import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.platform.Platform;

import lombok.ToString;

/**
 * Represents a row in the database
 */
@ToString
public abstract class Row {

	/**
	 * The unique ID of this row
	 */
	private final int id;

	/**
	 * The timestamp this row was created
	 */
	private final long date;

	/**
	 * The server this row was created in
	 */
	private final String server;

	protected Row(SimpleResultSet resultSet) throws SQLException {
		this.id = resultSet.getIntStrict("Id");
		this.date = resultSet.getTimestampStrict("Date");
		this.server = resultSet.getStringStrict("Server");
	}

	protected Row(int id, long date, String server) {
		this.id = id;
		this.date = date;
		this.server = server;
	}

	protected Row() {
		this.id = 0;
		this.date = System.currentTimeMillis();
		this.server = Platform.getCustomServerName();
	}

	/**
	 * Serialize this row into a map
	 *
	 * @return
	 */
	public final SerializedMap toMap() {
		final SerializedMap map = SerializedMap.ofArray(
				"Date", TimeUtil.toSQLTimestamp(this.date),
				"Server", this.server);

		this.onMapCreate(map);

		return map;
	}

	/**
	 * Get the unique ID of this row
	 *
	 * @return
	 */
	public final int getId() {
		return id;
	}

	/**
	 * Get the date this row was created
	 *
	 * @return
	 */
	public final long getDate() {
		return date;
	}

	/**
	 * Get the server this row was created in
	 *
	 * @return
	 */
	public final String getServer() {
		return server;
	}

	/**
	 * Called when creating the map.
	 *
	 * @param map
	 */
	protected abstract void onMapCreate(SerializedMap map);

	/**
	 * Get the table this row belongs to.
	 *
	 * @return
	 */
	public abstract Table getTable();

	/**
	 * Save this row to the database.
	 */
	public final void save() {
		this.getTable().getDatabase().addToQueue(this);
	}
}