package org.mineacademy.fo.database;

import java.sql.SQLException;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.SimpleLocation;

public abstract class RowLocation extends Row {

	private SimpleLocation location;

	protected RowLocation(SimpleResultSet resultSet) throws SQLException {
		super(resultSet);
	}

	protected RowLocation(int id, long date, String server, SimpleLocation location) {
		super(id, date, server);

		this.location = location;
	}

	protected RowLocation(SimpleLocation location) {
		this.location = location;
	}

	protected RowLocation() {
	}

	@Override
	protected void onMapCreate(SerializedMap map) {
		map.putArray(
				"Location", this.location.getX() + " " + this.location.getY() + " " + this.location.getZ(),
				"World", this.location.getWorldName());
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
