package org.mineacademy.fo.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.database.SimpleDatabase.TableCreator;
import org.mineacademy.fo.platform.Platform;

/**
 * Represents a table in the database.
 */
public abstract class Table {

	/**
	 * The list of all tables.
	 */
	private static List<Table> values = new ArrayList<>();

	/**
	 * The name of this table without the plugin's prefix.
	 */
	private final String key;

	/**
	 * The full table name prefixed with the name of this plugin.
	 *
	 * @see Platform#getPlugin() and getName()
	 */
	private final String name;

	/**
	 * The row class for this table
	 */
	private final Class<? extends Row> rowClass;

	protected Table(String key, Class<? extends Row> rowClass) {
		this.key = key;
		this.name = Platform.getPlugin().getName() + "_" + ChatUtil.capitalize(key);
		this.rowClass = rowClass;

		ValidCore.checkBoolean(!values.contains(this), "Table " + key + " is already registered");
		values.add(this);
	}

	/**
	 * Return the name of this table without the plugin's prefix
	 *
	 * @return
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Return the full table name prefixed with the name of this plugin
	 *
	 * @return
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Return the row class for this table.
	 *
	 * @return
	 */
	public final Class<? extends Row> getRowClass() {
		return rowClass;
	}

	/**
	 * Return the database this table belongs to.
	 *
	 * @return
	 */
	public abstract SimpleDatabase getDatabase();

	/**
	 * Return the row with the given id.
	 *
	 * @param <T>
	 * @param id
	 * @return
	 */
	public <T extends Row> T getRow(int id) {
		return this.getDatabase().getRow(this, id);
	}

	/**
	 * Return all rows in this table.
	 *
	 * @param <T>
	 * @return
	 */
	public <T extends Row> List<T> getRows() {
		return this.getDatabase().getRows(this);
	}

	/**
	 * Called when creating the table.
	 *
	 * @param creator
	 */
	public abstract void onTableCreate(TableCreator creator);

	/**
	 * Create a new row for this table.
	 *
	 * @param <T>
	 * @param resultSet
	 * @return
	 * @throws SQLException
	 */
	public abstract <T extends Row> T createRow(SimpleResultSet resultSet) throws SQLException;

	/**
	 * @deprecated use {@link #getName()}
	 *
	 * @return
	 */
	@Deprecated
	public String name() {
		return this.key;
	}

	@Override
	public final String toString() {
		return this.key;
	}

	/**
	 * Return the table from the given name or null.
	 *
	 * @param name
	 * @return
	 */
	public static Table valueOf(String name) {
		for (final Table table : values)
			if (table.getKey().equalsIgnoreCase(name))
				return table;

		return null;
	}

	/**
	 * Return all registered tables.
	 *
	 * @return
	 */
	public static Table[] values() {
		return values.toArray(new Table[values.size()]);
	}
}
