package org.mineacademy.fo.database;

import java.sql.SQLException;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.ToString;

/**
 * Represents a row in the database
 */
@ToString
public abstract class Row {

	/**
	 * The unique ID of this row
	 */
	private final Integer id;

	/**
	 * Create a new row
	 */
	protected Row() {
		this.id = null;
	}

	/**
	 * Create a new row
	 *
	 * @param resultSet
	 * @throws SQLException
	 */
	protected Row(SimpleResultSet resultSet) throws SQLException {
		this.id = resultSet.getIntStrict("Id");
	}

	/**
	 * Serialize this row into a map
	 *
	 * @return
	 */
	public abstract SerializedMap toMap();

	/**
	 * Get the unique ID of this row
	 *
	 * @return
	 */
	public final int getId() {
		ValidCore.checkNotNull(this.id, "ID not set for " + this);

		return this.id;
	}

	/**
	 * Get the table this row belongs to.
	 *
	 * @return
	 */
	public abstract Table getTable();

	/**
	 * Save this row to the database by adding it to the queue.
	 */
	public final void save() {
		this.getTable().getDatabase().addToQueue(this);
	}

	/**
	 * Save this row to the database immediately.
	 */
	public final void saveNow() {
		this.getTable().getDatabase().insert(this.getTable(), this.toMap());
	}

	/**
	 * Delete this row from the database
	 */
	public final void delete() {
		this.getTable().getDatabase().deleteRow(this.getTable(), this);
	}
}