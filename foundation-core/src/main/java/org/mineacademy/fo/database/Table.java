package org.mineacademy.fo.database;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.List;

import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.database.SimpleDatabase.TableCreator;
import org.mineacademy.fo.exception.ReflectionException;

/**
 * Represents a table in the database.
 */
public interface Table {

	/**
	 * Return the name of this table without the plugin's prefix
	 *
	 * @return
	 */
	String getKey();

	/**
	 * Return the full table name prefixed with the name of this plugin
	 *
	 * @return
	 */
	String getName();

	/**
	 * Return the row class for this table.
	 *
	 * @return
	 */
	Class<? extends Row> getRowClass();

	/**
	 * Return the database this table belongs to.
	 *
	 * @return
	 */
	SimpleDatabase getDatabase();

	/**
	 * Return the row with the given id.
	 *
	 * @param <T>
	 * @param id
	 * @return
	 */
	default <T extends Row> T getRow(int id) {
		return this.getDatabase().getRow(this, id);
	}

	/**
	 * Return all rows in this table.
	 *
	 * @param <T>
	 * @return
	 */
	default <T extends Row> List<T> getRows() {
		return this.getDatabase().getRows(this);
	}

	/**
	 * Called when creating the table.
	 *
	 * @param creator
	 */
	void onTableCreate(TableCreator creator);

	/**
	 * Create a new row for this table.
	 *
	 * @param <T>
	 * @param resultSet
	 * @return
	 * @throws SQLException
	 */
	default <T extends Row> T createRow(SimpleResultSet resultSet) throws SQLException {
		Constructor<?> constructor;

		try {
			constructor = ReflectionUtil.getConstructor(this.getRowClass(), SimpleResultSet.class);

		} catch (final ReflectionException ex) {
			constructor = null;
		}

		ValidCore.checkNotNull(constructor, "Row class " + this.getRowClass() + " must have a constructor with SimpleResultSet parameter");

		return (T) ReflectionUtil.instantiate(constructor, resultSet);
	}
}
