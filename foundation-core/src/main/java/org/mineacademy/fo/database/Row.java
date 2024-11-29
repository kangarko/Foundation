package org.mineacademy.fo.database;

import java.sql.SQLException;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.Platform;

/**
 * Represents a row in the database
 */
public abstract class Row {

	/**
	 * The unique ID of this row, can be null.
	 */
	private final Integer id;

	/**
	 * Create a new row
	 */
	protected Row() {
		this.id = null;
	}

	/**
	 * Create a new row from the result set.
	 * This will assume the Id column exists and error out if not.
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
		ValidCore.checkNotNull(this.id, "ID not set for " + this.toMap());

		return this.id;
	}

	/**
	 * Get the table this row belongs to.
	 *
	 * @return
	 */
	public abstract Table getTable();

	/**
	 * Get the unique column name of this row, if any
	 *
	 * @return
	 */
	public Tuple<String, Object> getUniqueColumn() {
		return null;
	}

	/**
	 * Save this row to the database by adding it to the queue. This will add it as a new row.
	 */
	public final void insertToQueue() {
		this.getTable().getDatabase().insertToQueue(this);
	}

	/**
	 * Save this row to the database by adding it to the queue. This will replace the existing row exists.
	 *
	 * If using SQLIte, override {@link #getUniqueColumn()} and make sure the table has a unique column.
	 */
	public final void upsert() {
		if (Platform.isAsync())
			this.getTable().getDatabase().upsert(this);
		else
			Platform.runTaskAsync(() -> this.getTable().getDatabase().upsert(this));
	}

	/**
	 * Delete this row from the database
	 */
	public final void delete() {
		this.getTable().getDatabase().deleteRow(this.getTable(), this);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + this.toMap().toStringFormatted();
	}
}