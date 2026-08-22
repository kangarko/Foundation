package org.mineacademy.fo.database;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.exception.InvalidRowException;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.model.SimpleLocation;
import org.mineacademy.fo.platform.Platform;

import com.google.gson.reflect.TypeToken;

import lombok.Getter;

/**
 * Represents a simple ResultSet wrapper with additional utility methods.
 */
@Getter
public final class SimpleResultSet {

	/**
	 * The table name this result set is from.
	 */
	private final String tableName;

	/**
	 * The delegate result set.
	 */
	private final ResultSet delegate;

	private SimpleResultSet(final Table table, final ResultSet resultSet) {
		this.tableName = table.getName();
		this.delegate = resultSet;
	}

	/**
	 * Closes the ResultSet and frees up any resources.
	 *
	 * @throws SQLException if there is an issue when closing the ResultSet.
	 */
	public void close() throws SQLException {
		this.delegate.close();
	}

	/**
	 * Get and convert the value from the specified column into the desired type.
	 *
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * String myString = myObject.get("column_name", String.class);
	 * }</pre>
	 *
	 * @param <T>       The type you want the value to be converted into.
	 * @param columnLabel The name of the column you want to get the value from.
	 * @param typeOf    The class object representing the type you want (e.g., {@code String.class}, {@code Integer.class}).
	 * @return The value of the specified column, deserialized into an instance of the given type.
	 *         Returns null if the value is empty or invalid.
	 * @throws SQLException If there is an issue accessing the database.
	 * @throws InvalidRowException If the row contains invalid data that cannot be deserialized.
	 */
	public <T> T get(final String columnLabel, final Class<T> typeOf) throws SQLException {
		final String value = this.getString(columnLabel);

		if (value == null || "".equals(value))
			return null;

		try {
			return SerializeUtilCore.deserialize(Language.JSON, typeOf, value);

		} catch (final Throwable ex) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid item value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve a list of items from the column specified by its label.
	 *
	 * @param <T>
	 * @param columnLabel
	 * @param typeOf
	 * @return
	 * @throws SQLException
	 */
	public <T> List<T> getList(final String columnLabel, final Class<T> typeOf) throws SQLException {
		final String value = this.getString(columnLabel);

		if (value == null || "".equals(value))
			return null;

		final List<Object> stringList;

		try {
			stringList = CommonCore.GSON.fromJson(value, new TypeToken<List<Object>>() {
			}.getType());

		} catch (final Throwable ex) {
			CommonCore.warning(this.tableName + " table has invalid " + columnLabel + " row with value '" + value + "', ignoring. The error was: " + ex.toString());

			throw new InvalidRowException();
		}

		final List<T> list = new ArrayList<>();

		for (final Object element : stringList)
			try {
				list.add(SerializeUtilCore.deserialize(Language.JSON, typeOf, element));

			} catch (final Throwable ex) {
				// Get to the root cause and then ignore if the world is not loaded anymore.
				if (CommonCore.getRootCause(ex) instanceof InvalidWorldException)
					continue;
				else
					CommonCore.error(ex, "Failed to deserialize list element in table " + this.tableName + "! Raw: " + element);
			}

		return list;
	}

	/**
	 * Retrieve the boolean value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the boolean value
	 * @throws SQLException if a database access error occurs
	 */
	public boolean getBoolean(final int columnIndex) throws SQLException {
		return this.delegate.getBoolean(columnIndex);
	}

	/**
	 * Retrieve the boolean value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the boolean value
	 * @throws SQLException if a database access error occurs
	 */
	public boolean getBoolean(final String columnLabel) throws SQLException {
		return this.delegate.getBoolean(columnLabel);
	}

	/**
	 * Retrieve the boolean value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the boolean value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid boolean
	 */
	public boolean getBooleanStrict(final String columnLabel) throws SQLException {
		final String value = this.getStringStrict(columnLabel);

		try {
			return Boolean.parseBoolean(value);

		} catch (final Throwable t) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid boolean value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve the byte value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the byte value
	 * @throws SQLException if a database access error occurs
	 */
	public byte getByte(final int columnIndex) throws SQLException {
		return this.delegate.getByte(columnIndex);
	}

	/**
	 * Retrieve the date value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the date value
	 * @throws SQLException if a database access error occurs
	 */
	public Date getDate(final int columnIndex) throws SQLException {
		return this.delegate.getDate(columnIndex);
	}

	/**
	 * Retrieve the date value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the date value
	 * @throws SQLException if a database access error occurs
	 */
	public Date getDate(final String columnLabel) throws SQLException {
		return this.delegate.getDate(columnLabel);
	}

	/**
	 * Retrieve the double value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the double value
	 * @throws SQLException if a database access error occurs
	 */
	public double getDouble(final int columnIndex) throws SQLException {
		return this.delegate.getDouble(columnIndex);
	}

	/**
	 * Retrieve the double value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the double value
	 * @throws SQLException if a database access error occurs
	 */
	public double getDouble(final String columnLabel) throws SQLException {
		return this.delegate.getDouble(columnLabel);
	}

	/**
	 * Retrieve the double value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the double value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid double
	 */
	public double getDoubleStrict(final String columnLabel) throws SQLException {
		final String value = this.getStringStrict(columnLabel);

		try {
			return Double.parseDouble(value);

		} catch (final Throwable t) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid double value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve an enum value from the column specified by its label.
	 *
	 * @param <T> the enum type
	 * @param columnLabel the label of the column
	 * @param typeOf the enum class type
	 * @return the enum value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the enum value is invalid
	 */
	public <T extends Enum<T>> T getEnum(final String columnLabel, final Class<T> typeOf) throws SQLException {
		final String value = this.getString(columnLabel);

		if (value != null && !"".equals(value)) {
			final T enumValue = ReflectionUtil.lookupEnumSilent(typeOf, value);

			if (enumValue == null) {
				CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid " + typeOf.getSimpleName() + " enum value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring. Valid values: " + CommonCore.join(typeOf.getEnumConstants()));

				throw new InvalidRowException();
			}

			return enumValue;
		}

		return null;
	}

	/**
	 * Retrieve an enum value from the column specified by its label strictly.
	 *
	 * @param <T> the enum type
	 * @param columnLabel the label of the column
	 * @param typeOf the enum class type
	 * @return the enum value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the enum value is invalid
	 */
	public <T extends Enum<T>> T getEnumStrict(final String columnLabel, final Class<T> typeOf) throws SQLException {
		final String value = this.getStringStrict(columnLabel);
		final T enumValue = ReflectionUtil.lookupEnumSilent(typeOf, value);

		if (enumValue == null) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid " + typeOf.getSimpleName() + " enum value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring. Valid values: " + CommonCore.join(typeOf.getEnumConstants()));

			throw new InvalidRowException();
		}

		return enumValue;
	}

	/**
	 * Retrieve the float value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the float value
	 * @throws SQLException if a database access error occurs
	 */
	public float getFloat(final int columnIndex) throws SQLException {
		return this.delegate.getFloat(columnIndex);
	}

	/**
	 * Retrieve the int value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the int value
	 * @throws SQLException if a database access error occurs
	 */
	public int getInt(final int columnIndex) throws SQLException {
		return this.delegate.getInt(columnIndex);
	}

	/**
	 * Retrieve the int value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the int value
	 * @throws SQLException if a database access error occurs
	 */
	public int getInt(final String columnLabel) throws SQLException {
		return this.delegate.getInt(columnLabel);
	}

	/**
	 * Retrieve the int value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the int value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid integer
	 */
	public int getIntStrict(final String columnLabel) throws SQLException {
		final String value = this.getStringStrict(columnLabel);

		try {
			return Integer.parseInt(value);

		} catch (final Throwable t) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid integer value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve a location from the world and "x y z" columns.
	 *
	 * @param worldColumn
	 * @param positionColumn
	 * @return
	 * @throws SQLException
	 */
	public SimpleLocation getLocation(final String worldColumn, final String positionColumn) throws SQLException {
		final String worldName = this.getStringStrict(worldColumn);
		final int[] position = this.getLocationArrayStrict(positionColumn);

		return new SimpleLocation(worldName, position[0], position[1], position[2]);
	}

	/**
	 * Retrieve a location array (x, y, z) from the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return an array containing location coordinates [x, y, z]
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not valid
	 */
	public int[] getLocationArrayStrict(final String columnLabel) throws SQLException {
		final String value = this.getString(columnLabel);

		if (value == null || "".equals(value)) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}

		final String[] split = value.split(" ");

		if (split.length != 3) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid location value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}

		return new int[] {
				(int) Double.parseDouble(split[0]), // convert decimals
				(int) Double.parseDouble(split[1]),
				(int) Double.parseDouble(split[2])
		};
	}

	/**
	 * Retrieve the long value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the long value
	 * @throws SQLException if a database access error occurs
	 */
	public long getLong(final int columnIndex) throws SQLException {
		return this.delegate.getLong(columnIndex);
	}

	/**
	 * Retrieve the long value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the long value
	 * @throws SQLException if a database access error occurs
	 */
	public long getLong(final String columnLabel) throws SQLException {
		return this.delegate.getLong(columnLabel);
	}

	/**
	 * Retrieve the long value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the long value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid long
	 */
	public long getLongStrict(final String columnLabel) throws SQLException {
		final String value = this.getStringStrict(columnLabel);

		try {
			return Long.parseLong(value);

		} catch (final Throwable t) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid long value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve the object value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the object value
	 * @throws SQLException if a database access error occurs
	 */
	public Object getObject(final int columnIndex) throws SQLException {
		return this.delegate.getObject(columnIndex);
	}

	/**
	 * Retrieve the object value of the column specified by index, and cast to the specified type.
	 *
	 * @param <T> the expected type
	 * @param columnIndex the column index
	 * @param type the class type of the expected value
	 * @return the object value cast to the specified type
	 * @throws SQLException if a database access error occurs
	 */
	public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
		return this.delegate.getObject(columnIndex, type);
	}

	/**
	 * Retrieve the short value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the short value
	 * @throws SQLException if a database access error occurs
	 */
	public short getShort(final int columnIndex) throws SQLException {
		return this.delegate.getShort(columnIndex);
	}

	/**
	 * Retrieve a strict value from the column specified by its label and cast to the specified type.
	 *
	 * @param <T> the expected type
	 * @param columnLabel the label of the column
	 * @param typeOf the class type of the expected value
	 * @return the object value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is invalid
	 */
	public <T> T getStrict(final String columnLabel, final Class<T> typeOf) throws SQLException {
		final String value = this.getStringStrict(columnLabel);

		if (value == null || "".equals(value)) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}

		try {
			return SerializeUtilCore.deserialize(Language.JSON, typeOf, value);

		} catch (final Throwable ex) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid item value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve the String value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the String value or an empty String if null
	 * @throws SQLException if a database access error occurs
	 */
	public String getString(final int columnIndex) throws SQLException {
		return CommonCore.getOrEmpty(this.delegate.getString(columnIndex));
	}

	/**
	 * Retrieve the String value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the String value or an empty String if null
	 * @throws SQLException if a database access error occurs
	 */
	public String getString(final String columnLabel) throws SQLException {
		return CommonCore.getOrEmpty(this.delegate.getString(columnLabel));
	}

	/**
	 * Retrieve the String value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the String value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is null or empty
	 */
	public String getStringStrict(final String columnLabel) throws SQLException {
		final String value = this.getString(columnLabel);

		if (value == null || "".equals(value)) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}

		return value;
	}

	/**
	 * Retrieve the time value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the time value
	 * @throws SQLException if a database access error occurs
	 */
	public Time getTime(final int columnIndex) throws SQLException {
		return this.delegate.getTime(columnIndex);
	}

	/**
	 * Retrieve the time value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the time value
	 * @throws SQLException if a database access error occurs
	 */
	public Time getTime(final String columnLabel) throws SQLException {
		return this.delegate.getTime(columnLabel);
	}

	/**
	 * Retrieve the timestamp value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the timestamp value
	 * @throws SQLException if a database access error occurs
	 */
	public Timestamp getTimestamp(final int columnIndex) throws SQLException {
		return this.delegate.getTimestamp(columnIndex);
	}

	/**
	 * Retrieve the timestamp value of the column specified by its label as a long.
	 *
	 * @param columnLabel the label of the column
	 * @return the timestamp as a long
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the timestamp format is invalid
	 */
	public long getTimestamp(final String columnLabel) throws SQLException {
		final String rawTimestamp = this.delegate.getString(columnLabel);

		if (rawTimestamp == null)
			return 0;

		try {
			return Timestamp.valueOf(rawTimestamp).getTime();

		} catch (final IllegalArgumentException ex) {
			CommonCore.warning("Failed to parse timestamp '" + rawTimestamp + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve the timestamp value of the column specified by its label as a long strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the timestamp as a long
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the timestamp format is invalid
	 */
	public long getTimestampStrict(final String columnLabel) throws SQLException {
		final String rawTimestamp = this.delegate.getString(columnLabel);

		if (rawTimestamp == null) {
			CommonCore.warning(this.tableName + " table has invalid timestamp row with null/empty column '" + columnLabel + "'. Ignoring.");

			throw new InvalidRowException();
		}

		try {
			return Timestamp.valueOf(rawTimestamp).getTime();

		} catch (final IllegalArgumentException ex) {
			CommonCore.warning(this.tableName + " table has timestamp column '" + columnLabel + "' with invalid value '" + rawTimestamp + "'. Ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve the UUID value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the UUID value or null if the value is invalid
	 * @throws SQLException if a database access error occurs
	 */
	public UUID getUniqueId(final String columnLabel) throws SQLException {
		final String value = this.getString(columnLabel);

		if (value == null || "".equals(value))
			return null;

		try {
			return UUID.fromString(value);

		} catch (final Throwable ex) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid UUID value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve the UUID value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the UUID value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is invalid
	 */
	public UUID getUniqueIdStrict(final String columnLabel) throws SQLException {
		final String value = this.getStringStrict(columnLabel);

		try {
			return UUID.fromString(value);

		} catch (final Throwable ex) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid UUID value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Move the cursor to the next row.
	 *
	 * @return true if the cursor is moved to a valid row; false if there are no more rows
	 * @throws SQLException if a database access error occurs
	 */
	public boolean next() throws SQLException {
		return this.delegate.next();
	}

	/**
	 * Wrap the given result set with the specified table name.
	 *
	 * @param table
	 * @param resultSet
	 * @return
	 */
	public static SimpleResultSet wrap(final Table table, final ResultSet resultSet) {
		return new SimpleResultSet(table, resultSet);
	}
}