package org.mineacademy.fo.database;

import java.sql.SQLException;

import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.collection.SerializedMap;

/**
 * Represents a row in the database with a date.
 */
public abstract class RowDate extends Row {

	/**
	 * The timestamp this row was created.
	 */
	private final long date;

	/**
	 * Create a new row
	 */
	protected RowDate() {
		this.date = System.currentTimeMillis();
	}

	/**
	 * Create a new row
	 *
	 * @param resultSet
	 * @throws SQLException
	 */
	protected RowDate(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);

		this.date = resultSet.getTimestampStrict("Date");
	}

	@Override
	public SerializedMap toMap() {
		return SerializedMap.fromArray(
				"Date", TimeUtil.toSQLTimestamp(this.date));
	}

	/**
	 * Get the date this row was created
	 *
	 * @return
	 */
	public final long getDate() {
		return this.date;
	}
}
