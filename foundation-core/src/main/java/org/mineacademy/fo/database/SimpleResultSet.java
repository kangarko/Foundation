package org.mineacademy.fo.database;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.exception.InvalidRowException;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple ResultSet wrapper with additional utility methods.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SimpleResultSet {

	/**
	 * The table name this result set is from.
	 */
	private final String tableName;

	/**
	 * The delegate result set.
	 */
	private final ResultSet delegate;

	/**
	 * Closes the ResultSet and frees up any resources.
	 *
	 * @throws SQLException if there is an issue when closing the ResultSet.
	 */
	public void close() throws SQLException {
		delegate.close();
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
	public <T> T get(String columnLabel, Class<T> typeOf) throws SQLException {
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
	 * Retrieve the boolean value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the boolean value
	 * @throws SQLException if a database access error occurs
	 */
	public boolean getBoolean(int columnIndex) throws SQLException {
		return delegate.getBoolean(columnIndex);
	}

	/**
	 * Retrieve the boolean value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the boolean value
	 * @throws SQLException if a database access error occurs
	 */
	public boolean getBoolean(String columnLabel) throws SQLException {
		return delegate.getBoolean(columnLabel);
	}

	/**
	 * Retrieve the boolean value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the boolean value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid boolean
	 */
	public boolean getBooleanStrict(String columnLabel) throws SQLException {
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
	public byte getByte(int columnIndex) throws SQLException {
		return delegate.getByte(columnIndex);
	}

	/**
	 * Retrieve the date value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the date value
	 * @throws SQLException if a database access error occurs
	 */
	public Date getDate(int columnIndex) throws SQLException {
		return delegate.getDate(columnIndex);
	}

	/**
	 * Retrieve the date value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the date value
	 * @throws SQLException if a database access error occurs
	 */
	public Date getDate(String columnLabel) throws SQLException {
		return delegate.getDate(columnLabel);
	}

	/**
	 * Retrieve the double value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the double value
	 * @throws SQLException if a database access error occurs
	 */
	public double getDouble(int columnIndex) throws SQLException {
		return delegate.getDouble(columnIndex);
	}

	/**
	 * Retrieve the double value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the double value
	 * @throws SQLException if a database access error occurs
	 */
	public double getDouble(String columnLabel) throws SQLException {
		return delegate.getDouble(columnLabel);
	}

	/**
	 * Retrieve the double value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the double value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid double
	 */
	public double getDoubleStrict(String columnLabel) throws SQLException {
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
	public <T extends Enum<T>> T getEnum(String columnLabel, Class<T> typeOf) throws SQLException {
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
	public <T extends Enum<T>> T getEnumStrict(String columnLabel, Class<T> typeOf) throws SQLException {
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
	public float getFloat(int columnIndex) throws SQLException {
		return delegate.getFloat(columnIndex);
	}

	/**
	 * Retrieve the int value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the int value
	 * @throws SQLException if a database access error occurs
	 */
	public int getInt(int columnIndex) throws SQLException {
		return delegate.getInt(columnIndex);
	}

	/**
	 * Retrieve the int value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the int value
	 * @throws SQLException if a database access error occurs
	 */
	public int getInt(String columnLabel) throws SQLException {
		return delegate.getInt(columnLabel);
	}

	/**
	 * Retrieve the int value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the int value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid integer
	 */
	public int getIntStrict(String columnLabel) throws SQLException {
		final String value = this.getStringStrict(columnLabel);

		try {
			return Integer.parseInt(value);

		} catch (final Throwable t) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with invalid integer value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Retrieve a location array (x, y, z) from the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return an array containing location coordinates [x, y, z]
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not valid
	 */
	public int[] getLocationArrayStrict(String columnLabel) throws SQLException {
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
				Integer.parseInt(split[0]),
				Integer.parseInt(split[1]),
				Integer.parseInt(split[2])
		};
	}

	/**
	 * Retrieve the long value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the long value
	 * @throws SQLException if a database access error occurs
	 */
	public long getLong(int columnIndex) throws SQLException {
		return delegate.getLong(columnIndex);
	}

	/**
	 * Retrieve the long value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the long value
	 * @throws SQLException if a database access error occurs
	 */
	public long getLong(String columnLabel) throws SQLException {
		return delegate.getLong(columnLabel);
	}

	/**
	 * Retrieve the long value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the long value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is not a valid long
	 */
	public long getLongStrict(String columnLabel) throws SQLException {
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
	public Object getObject(int columnIndex) throws SQLException {
		return delegate.getObject(columnIndex);
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
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		return delegate.getObject(columnIndex, type);
	}

	/**
	 * Retrieve the short value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the short value
	 * @throws SQLException if a database access error occurs
	 */
	public short getShort(int columnIndex) throws SQLException {
		return delegate.getShort(columnIndex);
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
	public <T> T getStrict(String columnLabel, Class<T> typeOf) throws SQLException {
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
	public String getString(int columnIndex) throws SQLException {
		return CommonCore.getOrEmpty(delegate.getString(columnIndex));
	}

	/**
	 * Retrieve the String value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the String value or an empty String if null
	 * @throws SQLException if a database access error occurs
	 */
	public String getString(String columnLabel) throws SQLException {
		return CommonCore.getOrEmpty(delegate.getString(columnLabel));
	}

	/**
	 * Retrieve the String value of the column specified by its label strictly.
	 *
	 * @param columnLabel the label of the column
	 * @return the String value
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the value is null or empty
	 */
	public String getStringStrict(String columnLabel) throws SQLException {
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
	public Time getTime(int columnIndex) throws SQLException {
		return delegate.getTime(columnIndex);
	}

	/**
	 * Retrieve the time value of the column specified by its label.
	 *
	 * @param columnLabel the label of the column
	 * @return the time value
	 * @throws SQLException if a database access error occurs
	 */
	public Time getTime(String columnLabel) throws SQLException {
		return delegate.getTime(columnLabel);
	}

	/**
	 * Retrieve the timestamp value of the column specified by index.
	 *
	 * @param columnIndex the column index
	 * @return the timestamp value
	 * @throws SQLException if a database access error occurs
	 */
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return delegate.getTimestamp(columnIndex);
	}

	/**
	 * Retrieve the timestamp value of the column specified by its label as a long.
	 *
	 * @param columnLabel the label of the column
	 * @return the timestamp as a long
	 * @throws SQLException if a database access error occurs
	 * @throws InvalidRowException if the timestamp format is invalid
	 */
	public long getTimestamp(String columnLabel) throws SQLException {
		final String rawTimestamp = delegate.getString(columnLabel);

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
	public long getTimestampStrict(String columnLabel) throws SQLException {
		final String rawTimestamp = delegate.getString(columnLabel);

		if (rawTimestamp == null) {
			CommonCore.warning(Platform.getPlugin().getName() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

			throw new InvalidRowException();
		}

		try {
			return Timestamp.valueOf(rawTimestamp).getTime();

		} catch (final IllegalArgumentException ex) {
			CommonCore.warning("Failed to parse timestamp '" + rawTimestamp + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

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
	public UUID getUniqueId(String columnLabel) throws SQLException {
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
	public UUID getUniqueIdStrict(String columnLabel) throws SQLException {
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
		return delegate.next();
	}
}