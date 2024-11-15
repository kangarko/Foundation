package org.mineacademy.fo.database;

import java.sql.SQLException;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.SimpleLocation;

/**
 * Represents a row in the database with a location and a date.
 */
public abstract class RowLocationDate extends RowDate {

	/**
	 * The location of this row
	 */
	private final SimpleLocation location;

	/**
	 * Create a new row
	 *
	 * @param location
	 */
	protected RowLocationDate(SimpleLocation location) {
		this.location = location;
	}

	/**
	 * Create a new row
	 *
	 * @param resultSet
	 * @throws SQLException
	 */
	protected RowLocationDate(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);

		this.location = resultSet.getLocation("World", "Location");
	}

	@Override
	public SerializedMap toMap() {
		return super.toMap().putArray(
				"World", this.location.getWorldName(),
				"Location", this.location.getX() + " " + this.location.getY() + " " + this.location.getZ());
	}

	/**
	 * Get the location column in this row.
	 *
	 * @return
	 */
	public final SimpleLocation getLocation() {
		return this.location;
	}

	/**
	 * Get the location formatted
	 *
	 * @return
	 */
	public final String getLocationFormatted() {
		return this.location.getFormatted();
	}
}
