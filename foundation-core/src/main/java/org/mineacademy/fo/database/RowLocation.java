package org.mineacademy.fo.database;

import java.sql.SQLException;

import org.mineacademy.fo.model.SimpleLocation;

public abstract class RowLocation extends Row {

	protected RowLocation(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);
	}

	protected RowLocation() {
	}

	/**
	 * Get the location column in this row.
	 *
	 * @return
	 */
	public abstract SimpleLocation getLocation();
}
