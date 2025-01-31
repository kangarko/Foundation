package org.mineacademy.fo.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidRowException;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.Platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple MySQL database.
 * <p>
 * Before running queries make sure to call connect() methods.
 * <p>
 * You can also override onConnected() to run your code after the
 * connection has been established.
 * <p>
 * To use this class you must know the MySQL command syntax!
 */
public class SimpleDatabase {

	/**
	 * Map of variables you can use with the {} syntax in SQL.
	 */
	private final Map<String, String> sqlVariables = new HashMap<>();

	/**
	 * The established connection, or null if none.
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private Connection connection;

	/**
	 * The last credentials from the connect function, or null if never called.
	 */
	private LastCredentials lastCredentials;

	/*
	 * Is this a SQLite connection?
	 */
	private boolean isSQLite = false;

	// --------------------------------------------------------------------
	// Connecting
	// --------------------------------------------------------------------

	/**
	 * Return true if the connect function was called so that the driver was loaded.
	 *
	 * @return
	 */
	public final boolean isConnected() {
		return this.connection != null;
	}

	/**
	 * Attempts to establish a new MySQL database connection.
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 */
	public final void connect(final String host, final int port, final String database, final String user, final String password) {
		this.connect("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true", user, password);
	}

	/**
	 * Connects to the database.
	 *
	 * WARNING: Requires a database type NOT needing a username nor a password!
	 *
	 * @param url
	 */
	public final void connect(final String url) {
		this.connect(url, null, null);
	}

	/**
	 * Connects to the database. You can then use {table} in SQL to replace with your table name.
	 *
	 * @param url
	 * @param user
	 * @param password
	 */
	public final void connect(final String url, final String user, final String password) {
		this.isSQLite = false;

		try {
			if (url.startsWith("jdbc:sqlite")) {
				Platform.getPlugin().loadLibrary("org.xerial", "sqlite-jdbc", "3.47.1.0");

				Class.forName("org.sqlite.JDBC");

				final String headlessUrl = url.replace("jdbc:sqlite://", "");

				if (headlessUrl.split("\\.").length == 2 && !headlessUrl.contains("\\") && !headlessUrl.contains("/")) {
					final String path = FileUtil.getFile(headlessUrl).getPath();

					this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);

				} else
					this.connection = DriverManager.getConnection(url);

				this.isSQLite = true;
			}

			else if (url.startsWith("jdbc:mysql://")) {
				try {
					Platform.getPlugin().loadLibrary("com.mysql", "mysql-connector-j", "9.1.0");

					Class.forName("com.mysql.cj.jdbc.Driver");

				} catch (final Throwable t) {
					CommonCore.warning("Your database driver is outdated, switching to MySQL legacy JDBC Driver. You can ignore this but if you encounter issues, update Java.");

					Platform.getPlugin().loadLibrary("com.mysql", "mysql-connector-java", "8.0.33");
					Class.forName("com.mysql.jdbc.Driver");
				}
			}

			else if (url.startsWith("jdbc:mariadb://")) {
				Platform.getPlugin().loadLibrary("org.mariadb.jdbc", "mariadb-java-client", CommonCore.getJavaVersion() <= 11 ? "2.7.12" : "3.5.1");

				Class.forName("org.mariadb.jdbc.Driver");

			} else
				throw new FoException("Unknown database driver '" + url + "'. Only SQLite, MySQL and MariaDB (which supports MariaDB automatically) are supported at this time.");

			this.connection = user != null && password != null ? DriverManager.getConnection(url, user, password) : DriverManager.getConnection(url);

			String databaseName = url.substring(url.lastIndexOf("/") + 1);
			databaseName = databaseName.contains("?") ? databaseName.substring(0, databaseName.indexOf("?")) : databaseName;

			this.lastCredentials = new LastCredentials(url, databaseName, user, password);

			// Create tables automatically
			for (final Table createdTable : this.getTables()) {
				final TableCreator creator = new TableCreator(createdTable.getName());

				try {
					createdTable.onTableCreate(creator);

					this.createTable(creator);

				} catch (final Exception ex) {
					CommonCore.error(ex, "Error creating table " + createdTable.getName() + ", aborting.");

					return;
				}
			}

			try {
				this.onConnected();

			} catch (final Exception ex) {
				CommonCore.error(ex, "Error after connecting to database, shutting down the plugin for safety.");
				Platform.getPlugin().disable();

				return;
			}

		} catch (final Exception ex) {
			final String message = CommonCore.getOrEmpty(ex.getMessage());

			if (ex instanceof SQLNonTransientConnectionException && message.equals("Too many connections")) {
				CommonCore.throwErrorUnreported(ex,
						"Too many connections to the database!",
						"URL: " + url,
						"User: " + user,
						"",
						"If increasing `max_connections` in your database config (not in our plugin)",
						"is not possible, run these two SQL queries and report results to us:",
						"SHOW STATUS WHERE `variable_name` = 'Threads_connected';",
						"and:",
						"SHOW PROCESSLIST;");
			}

			// Mostly user-caused errors, do not report to sentry
			else if (message.contains("Access denied for user") || message.contains("Could not create connection to database server"))
				CommonCore.throwErrorUnreported(ex,
						"Failed to connect to a database",
						"URL: " + url,
						"User: " + user,
						"Error: " + ex.getMessage());

			else
				CommonCore.throwError(ex,
						"Failed to connect to a database",
						"URL: " + url,
						"User: " + user,
						"Error: " + ex.getMessage());
		}

	}

	/**
	 * Called automatically after the first connection has been established.
	 */
	protected void onConnected() {
	}

	/**
	 * Attempts to close the connection, if not null.
	 */
	public final void disconnect() {
		try {
			if (this.connection != null)
				this.connection.close();

		} catch (final SQLException ex) {
			CommonCore.error(ex, "Error closing database connection!");
		}
	}

	/*
	 * Checks if we connected to the database
	 */
	private final void ensureConnected() {
		ValidCore.checkBoolean(this.isConnected(), "Connection was never established, did you call connect() on " + this + "? Use isLoaded() to check.");
	}

	/*
	 * If connection is closed, attempt to connect using the last credentials
	 */
	private final void reconnectIfClosed() {
		ValidCore.checkNotNull(this.lastCredentials, "Last credentials are null, did you call connect() on " + this + "?");

		try {
			boolean isValid = true;

			try {
				isValid = this.connection.isValid(0);
			} catch (final AbstractMethodError ex) {
				// Unsupported driver
			}

			if (!isValid || this.connection.isClosed())
				this.connect(this.lastCredentials.getUrl(), this.lastCredentials.getUser(), this.lastCredentials.getPassword());

		} catch (final SQLException | AbstractMethodError ex) {
			CommonCore.error(ex, "Failed to reconnect to the database");
		}
	}

	// --------------------------------------------------------------------
	// Working with the new Table OOP model.
	// --------------------------------------------------------------------

	/**
	 * Get the row by id in the given table
	 *
	 * @param <T>
	 * @param table
	 * @param id
	 * @return
	 */
	public final <T extends Row> T getRow(final Table table, final int id) {
		final List<T> list = new ArrayList<>();

		this.select(table, Where.builder().equals("Id", id), resultSet -> {
			final T row = table.createRowOrNull(resultSet);

			if (row != null)
				list.add(row);
		});

		if (!list.isEmpty()) {
			ValidCore.checkBoolean(list.size() == 1, "Found more than one row with id " + id + " in table " + table.getName() + ": " + list);

			return list.get(0);
		}

		return null;
	}

	/**
	 * Get all rows in the given table
	 *
	 * @param <T>
	 * @param table
	 * @return
	 */
	public final <T extends Row> List<T> getRows(final Table table) {
		final List<T> entries = new ArrayList<>();

		this.selectAll(table, resultSet -> {
			final T row = table.createRowOrNull(resultSet);

			if (row != null)
				entries.add(row);
		});

		Collections.reverse(entries);

		return entries;
	}

	/**
	 * Get selected rows in the given table
	 *
	 * @param <T>
	 * @param table
	 * @param where
	 * @return
	 */
	public <T extends Row> T getRowWhere(final Table table, final Where where) {
		final List<T> rows = this.getRowsWhere(table, where);
		ValidCore.checkBoolean(rows.size() <= 1, "Found more than one (" + rows.size() + ") row in " + table.getName() + " where " + where + ": " + rows);

		return rows.isEmpty() ? null : rows.get(0);
	}

	/**
	 * Get selected rows in the given table
	 *
	 * @param <T>
	 * @param table
	 * @param where
	 * @return
	 */
	public <T extends Row> List<T> getRowsWhere(final Table table, final Where where) {
		final List<T> entries = new ArrayList<>();

		this.select(table, where, resultSet -> {
			final T row = table.createRowOrNull(resultSet);

			if (row != null)
				entries.add(row);
		});

		Collections.reverse(entries);

		return entries;
	}

	/**
	 * Inserts the given row into the database table, replacing any existing rows.
	 *
	 * This is a blocking operation.
	 *
	 * @param row
	 */
	public final void upsert(final Row row) {
		ValidCore.checkNotNull(row, "To use Database#upsert(), override " + row.getClass().getSimpleName() + "#getUniqueColumn()");

		this.upsert(row.getTable(), row.getUniqueColumn(), row.toMap());
	}

	/**
	 * Add a map of data to the queue for the given table, appending as a new row.
	 *
	 * This is a non-blocking operation.
	 *
	 * @param row
	 */
	public final void insertToQueue(final Row row) {
		RowQueueWriter.getInstance().addToQueue(row);
	}

	/**
	 * Remove a row from the given table
	 *
	 * @param table
	 * @param row
	 */
	public final void deleteRow(final Table table, final Row row) {
		this.delete(table, Where.builder().equals("Id", row.getId()));
	}

	/**
	 * Override to return a list of tables.
	 *
	 * Defaults to an empty array.
	 *
	 * @return
	 */
	public Table[] getTables() {
		return new Table[0];
	}

	// --------------------------------------------------------------------
	// Querying
	// --------------------------------------------------------------------

	/**
	 * Creates a database table, to be used in onConnected.
	 *
	 * @param creator
	 */
	protected final void createTable(final TableCreator creator) {
		String columns = "";

		for (final TableRow column : creator.getColumns()) {
			String dataType = column.getDataType().toLowerCase();

			if (this.isSQLite) {
				if (dataType.equals("datetime") || dataType.equals("longtext"))
					dataType = "text";

				else if (dataType.startsWith("varchar"))
					dataType = "text";

				else if (dataType.startsWith("bigint"))
					dataType = "integer";

				else if (creator.getPrimaryColumn() != null && creator.getPrimaryColumn().equals(column.getName()))
					dataType = "INTEGER PRIMARY KEY";
			}

			columns += (columns.isEmpty() ? "" : ", ") + "`" + column.getName() + "` " + dataType;

			if (column.getAutoIncrement() != null && column.getAutoIncrement())
				if (this.isSQLite)
					columns += " AUTOINCREMENT";

				else
					columns += " NOT NULL AUTO_INCREMENT";

			else if (column.getNotNull() != null && column.getNotNull())
				columns += " NOT NULL";

			if (column.getDefaultValue() != null)
				columns += " DEFAULT " + column.getDefaultValue();
		}

		if (creator.getPrimaryColumn() != null && !this.isSQLite)
			columns += ", PRIMARY KEY (`" + creator.getPrimaryColumn() + "`)";

		try {
			this.updateUnsafe("CREATE TABLE IF NOT EXISTS `" + creator.getName() + "` (" + columns + ") " + (this.isSQLite ? "" : "DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci") + ";");

		} catch (final Throwable t) {
			if (t.toString().contains("Unknown collation"))
				CommonCore.log("You need to update your database driver to support utf8mb4_unicode_520_ci collation. This is now required for storing emojis and non-engliish characters.");

			else
				throw t;
		}
	}

	/**
	 * Inserts the given map into the database table, replacing any existing rows.
	 *
	 * @param table
	 * @param uniqueColumnName
	 * @param columnsAndValues
	 */
	protected final void upsert(final Table table, @NonNull final Tuple<String, Object> uniqueColumn, @NonNull final SerializedMap columnsAndValues) {
		synchronized (this.connection) {
			final String tableName = this.replaceVariables(table.getName());

			// Building column names and placeholders for values (?)
			final String columns = String.join(",", columnsAndValues.keySet());
			final String placeholders = columnsAndValues.keySet().stream().map(key -> "?").collect(Collectors.joining(","));

			final StringBuilder sql = new StringBuilder("INSERT " + (this.isSQLite ? "OR REPLACE " : "") + "INTO ").append(tableName).append(" (").append(columns).append(") VALUES (").append(placeholders).append(")");

			if (!this.isSQLite) {

				// Prepare the duplicate update clause for MySQL
				final String duplicateUpdate = columnsAndValues.keySet().stream().map(key -> key + "=VALUES(" + key + ")").collect(Collectors.joining(","));

				sql.append(" ON DUPLICATE KEY UPDATE ").append(duplicateUpdate);

			} else {
				// Reason for this extra ugly connection is that Minecraft 1.8.8 ships with outdated SQLite
				// And we can't use Libby to download a new one due to a conflict.
				final String removeSql = "DELETE FROM " + tableName + " WHERE " + uniqueColumn.getKey() + " = ?;";

				try (PreparedStatement preparedStatement = this.prepareStatement(removeSql)) {
					preparedStatement.setObject(1, uniqueColumn.getValue());

					Debugger.debug("mysql", "[sqlite/remove] Running SQL: " + preparedStatement.toString().replace("\n", ""));
					preparedStatement.executeUpdate();

				} catch (final SQLException ex) {
					CommonCore.error(ex,
							"Error removing old SQLite column",
							"Table: " + tableName,
							"Query: " + removeSql);
				}
			}

			// Execute the query using PreparedStatement
			try (PreparedStatement preparedStatement = this.prepareStatement(sql.toString())) {
				int index = 1;

				for (final Object value : columnsAndValues.values()) {
					if (value == null || value.equals("NULL"))
						preparedStatement.setNull(index++, java.sql.Types.NULL);

					else if (value instanceof String)
						preparedStatement.setString(index++, (String) value);

					else {
						Object converted = SerializeUtilCore.serialize(Language.JSON, value);

						if (converted instanceof JsonElement)
							converted = ((JsonElement) converted).toString();

						if (!(converted instanceof String) && !(converted instanceof Boolean) && !(converted instanceof Number) && !converted.getClass().isPrimitive())
							throw new SQLException("Cannot store " + converted.getClass() + " in database, must be a primitive type, number or a string. Got: " + converted);

						preparedStatement.setObject(index++, converted);
					}
				}

				Debugger.debug("mysql", "[insert] Running SQL: " + preparedStatement.toString().replace("\n", ""));

				preparedStatement.executeUpdate();

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error inserting into database",
						"Table: " + tableName,
						"Unique column: " + uniqueColumn,
						"Columns and values: " + columnsAndValues,
						"Query: " + sql);
			}
		}
	}

	/**
	 * Appends the given map into the database table as a new row.
	 *
	 * @param table
	 * @param columnsAndValues
	 */
	protected final void insert(final Table table, @NonNull final SerializedMap columnsAndValues) {
		synchronized (this.connection) {
			final String tableName = this.replaceVariables(table.getName());

			// Building column names and placeholders for values (?)
			final String columns = String.join(",", columnsAndValues.keySet());
			final String placeholders = columnsAndValues.keySet().stream().map(key -> "?").collect(Collectors.joining(","));

			final StringBuilder sql = new StringBuilder("INSERT " + (this.isSQLite ? "OR REPLACE " : "") + "INTO ").append(tableName).append(" (").append(columns).append(") VALUES (").append(placeholders).append(")");

			// Execute the query using PreparedStatement
			try (PreparedStatement preparedStatement = this.prepareStatement(sql.toString())) {
				int index = 1;

				for (final Object value : columnsAndValues.values()) {
					if (value == null || value.equals("NULL"))
						preparedStatement.setNull(index++, java.sql.Types.NULL);

					else if (value instanceof String)
						preparedStatement.setString(index++, (String) value);

					else
						preparedStatement.setObject(index++, SerializeUtilCore.serialize(Language.JSON, value));
				}

				Debugger.debug("mysql", "[insert] Running SQL: " + preparedStatement.toString().replace("\n", ""));

				preparedStatement.executeUpdate();

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error inserting into database",
						"Table: " + tableName,
						"Query: " + sql);
			}
		}
	}

	/**
	 * Insert the batch map into the database as new rows.
	 *
	 * @param table
	 * @param maps
	 */
	protected final void insertBatch(final Table table, @NonNull final List<SerializedMap> maps) {
		this.ensureConnected();

		if (maps.isEmpty())
			return;

		this.reconnectIfClosed();

		synchronized (this.connection) {
			final String columns = String.join(", ", maps.get(0).keySet());
			final String placeholders = String.join(", ", Collections.nCopies(maps.get(0).size(), "?"));
			final String sql = "INSERT INTO " + table.getName() + " (" + columns + ") VALUES (" + placeholders + ");";

			Debugger.debug("mysql", "Batch Insert SQL Template: " + sql);

			try (PreparedStatement preparedStatement = this.connection.prepareStatement(sql)) {

				// Disable auto-commit for batch operations
				this.connection.setAutoCommit(false);

				for (final SerializedMap map : maps)
					try {
						int index = 1;

						for (Object value : map.values()) {
							value = SerializeUtilCore.serialize(Language.JSON, value);

							if (value instanceof JsonArray)
								value = ((JsonArray) value).toString();

							preparedStatement.setObject(index++, value == null || "NULL".equals(value) ? null : value instanceof Boolean ? ((boolean) value) ? 1 : 0 : value);
						}

						preparedStatement.addBatch();

					} catch (final Throwable t) {
						CommonCore.error(t,
								"Error processing database batch entry!",
								"Batch entry: " + map);
					}

				Debugger.debug("mysql", "Executing batch...");
				preparedStatement.executeBatch();

				// Commit the transaction
				this.connection.commit();

			} catch (final SQLException ex) {
				if (ex.getMessage() != null && ex.getMessage().contains("Can not read response from server")) {
					CommonCore.log("Error executing batch insert: " + sql);

					ex.printStackTrace();
				} else
					CommonCore.error(ex,
							"Error executing a batch insert",
							"SQL Query: " + sql);

			} finally {
				try {
					this.connection.setAutoCommit(true);

				} catch (final SQLException ex) {
					CommonCore.error(ex, "Error resetting auto-commit.");
				}
			}
		}
	}

	/**
	 * Executes a massive batch update.
	 *
	 * @deprecated SQLs are not sanitized
	 * @param sqls
	 */
	@Deprecated
	protected final void batchUpdateUnsafe(@NonNull final List<String> sqls) {
		this.ensureConnected();

		if (sqls.isEmpty())
			return;

		synchronized (this.connection) {
			this.reconnectIfClosed();

			try {
				try (Statement batchStatement = this.connection.createStatement(this.isSQLite ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_SENSITIVE, this.isSQLite ? ResultSet.CONCUR_READ_ONLY : ResultSet.CONCUR_UPDATABLE)) {
					final int processedCount = sqls.size();

					for (final String sql : sqls)
						batchStatement.addBatch(this.replaceVariables(sql));

					if (processedCount > 10_000)
						CommonCore.log("Updating your database (" + processedCount + " entries)... PLEASE BE PATIENT THIS WILL TAKE "
								+ (processedCount > 50_000 ? "10-20 MINUTES" : "5-10 MINUTES") + " - If server will print a crash report, ignore it, update will proceed.");

					// Prevent automatically sending db instructions
					this.connection.setAutoCommit(false);

					// Execute
					batchStatement.executeBatch();

					// This will block the thread
					this.connection.commit();

				} finally {
					this.connection.setAutoCommit(true);
				}

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error executing a batch update",
						"SQLs (" + sqls.size() + "): " + sqls);
			}
		}
	}

	/**
	 * Lists all rows in the given table.
	 *
	 * @param table
	 * @param consumer
	 */
	protected final void selectAll(final Table table, final ResultReader consumer) {
		this.select(table, null, consumer);
	}

	/**
	 * Selects all rows from the given table according to the where map clauses.
	 *
	 * @param table
	 * @param whereMap
	 * @param consumer
	 */
	protected final void select(final Table table, final Where where, final ResultReader consumer) {
		synchronized (this.connection) {
			final StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table.getName());

			if (where != null && !where.getConditions().isEmpty())
				sql.append(" WHERE ").append(where.buildSql());

			try (PreparedStatement preparedStatement = this.prepareStatement(sql.toString())) {
				if (where != null && !where.getValues().isEmpty()) {
					int index = 1;

					for (final Object value : where.getValues())
						preparedStatement.setObject(index++, value);
				}

				Debugger.debug("mysql", "[select] Running SQL: " + preparedStatement.toString().replace("\n", ""));

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					while (resultSet.next())
						try {
							consumer.accept(SimpleResultSet.wrap(table, resultSet));

						} catch (final InvalidRowException ex) {
							// Pardoned

						} catch (final Throwable throwable) {
							CommonCore.error(throwable, "Error selecting a row from table " + table.getName() + " where " + sql);
						}
				}
			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error selecting database rows",
						"Table: " + table.getName(),
						"Query: " + sql);
			}
		}
	}

	/**
	 * Select columns from the given table.
	 *
	 * @param table
	 * @param columns
	 * @param consumer
	 */
	protected final void selectColumns(final Table table, final List<String> columns, final ResultReader consumer) {
		this.selectColumns(table, columns, null, consumer);
	}

	/**
	 * Select columns from the given table.
	 *
	 * @param table
	 * @param columns
	 * @param where
	 * @param consumer
	 */
	protected final void selectColumns(final Table table, final List<String> columns, final Where where, final ResultReader consumer) {
		synchronized (this.connection) {
			final String tableName = table.getName();
			final StringBuilder sql = new StringBuilder("SELECT ");

			sql.append(String.join(", ", columns)).append(" FROM ").append(tableName);

			if (where != null && !where.getConditions().isEmpty())
				sql.append(" WHERE ").append(where.buildSql());

			try (PreparedStatement preparedStatement = this.prepareStatement(sql.toString())) {
				if (where != null && !where.getValues().isEmpty()) {
					int index = 1;

					for (final Object value : where.getValues())
						preparedStatement.setObject(index++, value);
				}

				Debugger.debug("mysql", "[select columns] Running SQL: " + preparedStatement.toString().replace("\n", ""));

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					while (resultSet.next())
						consumer.accept(SimpleResultSet.wrap(table, resultSet));
				}

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error selecting database columns",
						"Table: " + tableName,
						"Query: " + sql);
			}
		}
	}

	/**
	 * Returns the amount of rows from the given table per the conditions,
	 *
	 * Example conditions: SerializedMap.fromArray("Status", "PENDING")
	 * This example will return all rows where column Status equals PENDING.
	 *
	 * @param table
	 * @param conditions
	 * @return
	 */
	protected final int count(final Table table, final SerializedMap conditions) {
		synchronized (this.connection) {
			final String tableName = this.replaceVariables(table.getName());

			final StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);

			if (!conditions.isEmpty()) {
				queryBuilder.append(" WHERE ");
				queryBuilder.append(String.join(" AND ", conditions.entrySet().stream().map(entry -> entry.getKey() + " = ?").collect(Collectors.toList())));
			}

			final String sql = queryBuilder.toString();

			try (PreparedStatement preparedStatement = this.prepareStatement(sql)) {
				int index = 1;

				for (final Map.Entry<String, Object> entry : conditions.entrySet())
					preparedStatement.setObject(index++, entry.getValue());

				Debugger.debug("mysql", "[count] Running SQL: " + preparedStatement.toString().replace("\n", ""));

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next())
						return resultSet.getInt(1);
				}

			} catch (final SQLException ex) {
				CommonCore.throwError(ex,
						"Error counting database rows",
						"Table: " + tableName,
						"Query: " + sql);
			}

			return 0;
		}
	}

	/**
	 * Delete rows from the given table based on the where conditions.
	 *
	 * @param table The database table to delete from.
	 * @param where The where conditions.
	 */
	protected final void delete(final Table table, final Where where) {
		synchronized (this.connection) {
			ValidCore.checkBoolean(where != null && !where.getConditions().isEmpty(), "The where conditions cannot be empty for a delete operation!");
			final String sql = "DELETE FROM " + table.getName() + " WHERE " + where.buildSql();

			try (PreparedStatement preparedStatement = this.prepareStatement(sql)) {
				int index = 1;

				for (final Object value : where.getValues())
					preparedStatement.setObject(index++, value);

				Debugger.debug("mysql", "[delete] Running SQL: " + preparedStatement.toString().replace("\n", ""));

				preparedStatement.executeUpdate();

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error deleting database rows",
						"Table: " + table.getName(),
						"Query: " + sql);
			}
		}
	}

	/**
	 * Deletes rows from the given table where the 'Date' column is less than the provided timestamp.
	 *
	 * @param table The table from which to delete rows.
	 * @param timestamp The timestamp limit. Rows with 'Date' earlier than this will be deleted.
	 */
	protected final void deleteOlderThan(final Table table, @NonNull final Timestamp timestamp) {
		this.deleteOlderThan(table, "Date", timestamp);
	}

	/**
	 * Deletes rows from the given table where the given column is less than the provided timestamp.
	 *
	 * @param table The table from which to delete rows.
	 * @param columnName The column name to compare the timestamp against.
	 * @param timestamp The timestamp limit. Rows with 'Date' earlier than this will be deleted.
	 */
	protected final void deleteOlderThan(final Table table, final String columnName, @NonNull final Timestamp timestamp) {
		synchronized (this.connection) {
			final String sql = "DELETE FROM " + table.getName() + " WHERE " + columnName + " < ?";

			try (PreparedStatement preparedStatement = this.prepareStatement(sql)) {
				if (this.isSQLite)
					preparedStatement.setString(1, timestamp.toString());
				else
					preparedStatement.setTimestamp(1, timestamp);

				Debugger.debug("mysql", "[delete older than] Running SQL: " + sql.replace("?", timestamp.toString()));

				preparedStatement.executeUpdate();

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error deleting database rows",
						"Table: " + table.getName(),
						"Query: " + sql);
			}
		}
	}

	/*
	 * Creates a new prepared statement for the given sql query.
	 */
	protected final PreparedStatement prepareStatement(final String sql) {
		this.ensureConnected();

		synchronized (this.connection) {
			this.reconnectIfClosed();

			try {
				return this.connection.prepareStatement(sql);

			} catch (final SQLException ex) {
				CommonCore.throwError(ex,
						"Error preparing a statement",
						"Query: " + sql);

				return null;
			}
		}
	}

	/**
	 * Check if a specific column exists in a MySQL or SQLite database.
	 *
	 * @param tableName
	 * @param column
	 * @return
	 * @throws SQLException
	 */
	protected final boolean doesColumnExist(final Table table, final String column) throws SQLException {
		this.ensureConnected();

		synchronized (this.connection) {
			final String tableName = table.getName();

			if (this.isSQLite) {
				final String sql = "PRAGMA table_info(" + tableName + ");";

				Debugger.debug("mysql", "[does column exist/sqlite] Running SQL: " + sql);

				try (PreparedStatement statement = this.prepareStatement(sql);
						ResultSet resultSet = statement.executeQuery()) {

					while (resultSet.next()) {
						final String columnName = resultSet.getString("name");

						// Compare with the expected column name
						if (columnName.equalsIgnoreCase(column))
							return true;
					}

				} catch (final SQLException ex) {
					CommonCore.error(ex,
							"Error checking if SQLite database column exists",
							"Table: " + tableName,
							"Column: " + column,
							"Query: " + sql);
				}

			} else {
				final String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

				try (PreparedStatement preparedStatement = this.prepareStatement(sql)) {
					preparedStatement.setString(1, this.lastCredentials.getDatabaseName());
					preparedStatement.setString(2, tableName);
					preparedStatement.setString(3, column);

					Debugger.debug("mysql", "[does column exist/mysql] Running SQL: " + preparedStatement.toString().replace("\n", ""));

					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						if (resultSet.next())
							return resultSet.getInt(1) > 0;
					}

				} catch (final SQLException ex) {
					CommonCore.error(ex,
							"Error checking if MySQL database column exists",
							"Table: " + tableName,
							"Column: " + column,
							"Query: " + sql);
				}
			}

			return false;
		}
	}

	/**
	 * Attempts to execute a new update query.
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown.
	 *
	 * @param sql
	 *
	 * @deprecated Unchecked sql query, prone to SQL injections. You need to perform the validation yourself.
	 */
	@Deprecated
	protected final void updateUnsafe(String sql) {
		synchronized (this.connection) {
			this.reconnectIfClosed();

			sql = this.replaceVariables(sql);
			ValidCore.checkBoolean(!sql.contains("{table}"), "Table not set! Either use connect() method that specifies it or call addVariable(table, 'yourtablename') in your constructor!");

			Debugger.debug("mysql", "Updating database with: " + sql);

			try (Statement statement = this.connection.createStatement()) {
				statement.executeUpdate(sql);

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error updating database",
						"Query: " + sql);
			}
		}
	}

	/**
	 * Attempts to execute a new query.
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown.
	 *
	 * @param sql
	 * @return
	 *
	 * @deprecated Unchecked sql query, prone to SQL injections. You need to perform the validation yourself.
	 */
	@Deprecated
	protected final ResultSet queryUnsafe(String sql) {
		this.ensureConnected();

		synchronized (this.connection) {
			this.reconnectIfClosed();

			sql = this.replaceVariables(sql);

			Debugger.debug("mysql", "Querying database with: " + sql);

			try {
				final Statement statement = this.connection.createStatement();
				final ResultSet resultSet = statement.executeQuery(sql);

				return resultSet;

			} catch (final SQLException ex) {
				CommonCore.error(ex,
						"Error querying database",
						"Query: " + sql);
			}

			return null;
		}
	}

	// --------------------------------------------------------------------
	// Variables
	// --------------------------------------------------------------------

	/**
	 * Returns true if the database is SQLite.
	 *
	 * @return
	 */
	protected final boolean isSQLite() {
		return this.isSQLite;
	}

	/**
	 * Adds a new variable you can then use in your queries.
	 * The variable name will be added {} brackets automatically.
	 *
	 * @param name
	 * @param value
	 */
	protected final void addVariable(final String name, final String value) {
		this.sqlVariables.put(name, value);
	}

	/*
	 * Replace the {table} and {@link #sqlVariables} in the sql query
	 */
	private String replaceVariables(final String sql) {
		final StringBuilder builder = new StringBuilder(sql);

		this.sqlVariables.forEach((key, value) -> {
			final String varPattern = "{" + key + "}";
			int index;

			while ((index = builder.indexOf(varPattern)) != -1)
				builder.replace(index, index + varPattern.length(), value);
		});

		return builder.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A specialized task to make I/O operations off of the main thread
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class RowQueueWriter implements Runnable {

		/**
		 * The singleton instance
		 */
		private static final RowQueueWriter instance = new RowQueueWriter();

		/**
		 * Sync database write operations.
		 */
		private final Map<Table, List<SerializedMap>> queue = new HashMap<>();

		@Override
		public void run() {
			final Map<Table, List<SerializedMap>> copy = new HashMap<>();

			synchronized (instance) {
				copy.putAll(this.queue);

				this.queue.clear();
			}

			for (final Iterator<Map.Entry<Table, List<SerializedMap>>> it = copy.entrySet().iterator(); it.hasNext();) {
				final Map.Entry<Table, List<SerializedMap>> entry = it.next();

				final Table table = entry.getKey();
				final List<SerializedMap> maps = entry.getValue();

				table.getDatabase().insertBatch(table, maps);
			}
		}

		/*
		 * Adds a row to the queue.
		 */
		private void addToQueue(final Row row) {
			synchronized (instance) {
				this.queue.computeIfAbsent(row.getTable(), key -> new ArrayList<>()).add(row.toMap());
			}
		}

		/*
		 * Get the singleton instance
		 */
		public static RowQueueWriter getInstance() {
			synchronized (instance) {
				return instance;
			}
		}
	}

	/**
	 * Represents a where clause builder for SQL queries.
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Where {

		/**
		 * The conditions
		 */
		private final List<String> conditions = new ArrayList<>();

		/**
		 * The values
		 */
		private final List<Object> values = new ArrayList<>();

		/**
		 * EQUALS condition
		 *
		 * @param column
		 * @param value
		 * @return
		 */
		public Where equals(final String column, final String value) {
			this.conditions.add(column + " = ?");
			this.values.add(value);

			return this;
		}

		/**
		 * EQUALS condition
		 *
		 * @param column
		 * @param value
		 * @return
		 */
		public Where equals(final String column, final Number value) {
			this.conditions.add(column + " = ?");
			this.values.add(value);

			return this;
		}

		/**
		 * LIKE condition, e.g. WHERE column LIKE '%pattern%'
		 *
		 * @param column
		 * @param pattern
		 * @return
		 */
		public Where like(final String column, final String pattern) {
			this.conditions.add(column + " LIKE ?");

			ValidCore.checkBoolean(pattern.charAt(0) != '%' && pattern.charAt(pattern.length() - 1) != '%', "Pattern must not start or end with %, got " + pattern);
			this.values.add("%" + pattern + "%");

			return this;
		}

		/**
		 * Greater Than condition
		 *
		 * @param column
		 * @param value
		 * @return
		 */
		public Where greaterThan(final String column, final Number value) {
			this.conditions.add(column + " > ?");
			this.values.add(value);
			return this;
		}

		/**
		 * Less Than condition
		 *
		 * @param column
		 * @param value
		 * @return
		 */
		public Where lessThan(final String column, final Number value) {
			this.conditions.add(column + " < ?");
			this.values.add(value);

			return this;
		}

		/**
		 * IN condition, e.g. WHERE column IN (value1, value2, value3)
		 *
		 * @param column
		 * @param values
		 * @return
		 */
		public Where in(final String column, @NonNull final Collection<?> values) {
			if (values.isEmpty())
				return this;

			if (values.stream().anyMatch(v -> !(v instanceof String) && !(v instanceof Number)))
				throw new FoException("Where in() values must be either a string or a number, got " + values);

			final String inClause = String.join(",", values.stream().map(v -> "?").toArray(String[]::new));
			this.conditions.add(column + " IN (" + inClause + ")");
			this.values.addAll(values);

			return this;
		}

		/**
		 * NOT IN condition, e.g. WHERE column NOT IN (value1, value2, value3)
		 *
		 * @param column
		 * @param values
		 * @return
		 */
		public Where notIn(final String column, @NonNull final Collection<?> values) {
			if (values.isEmpty())
				return this;

			if (values.stream().anyMatch(v -> !(v instanceof String) && !(v instanceof Number)))
				throw new FoException("Where in() values must be either a string or a number, got " + values);

			final String notInClause = String.join(",", values.stream().map(v -> "?").toArray(String[]::new));
			this.conditions.add(column + " NOT IN (" + notInClause + ")");
			this.values.addAll(values);

			return this;
		}

		/**
		 * IS NULL condition
		 *
		 * @param column
		 * @return
		 */
		public Where isNull(final String column) {
			this.conditions.add(column + " IS NULL");

			return this;
		}

		/**
		 * IS NOT NULL condition
		 *
		 * @param column
		 * @return
		 */
		public Where isNotNull(final String column) {
			this.conditions.add(column + " IS NOT NULL");

			return this;
		}

		/**
		 * BETWEEN condition, e.g. WHERE column BETWEEN val1 AND val2
		 *
		 * @param column
		 * @param lowerValue
		 * @param upperValue
		 * @return
		 */
		public Where between(final String column, final Number lowerValue, final Number upperValue) {
			this.conditions.add(column + " BETWEEN ? AND ?");
			this.values.add(lowerValue);
			this.values.add(upperValue);

			return this;
		}

		/**
		 * OR condition, e.g. WHERE (expression1 OR expression2)
		 *
		 * @param anotherClause
		 * @return
		 */
		public Where or(final Where anotherClause) {
			if (!anotherClause.getConditions().isEmpty()) {
				this.conditions.add("(" + String.join(" OR ", anotherClause.getConditions()) + ")");

				this.values.addAll(anotherClause.getValues());
			}

			return this;
		}

		/**
		 * AND condition (used to join two Where clauses)
		 *
		 * @param anotherClause
		 * @return
		 */
		public Where and(final Where anotherClause) {
			if (!anotherClause.getConditions().isEmpty()) {
				this.conditions.add("(" + String.join(" AND ", anotherClause.getConditions()) + ")");

				this.values.addAll(anotherClause.getValues());
			}

			return this;
		}

		/**
		 * @deprecated do not use, not to be mixed with other equals methods
		 */
		@Deprecated
		@Override
		public boolean equals(final Object obj) {
			throw new UnsupportedOperationException("Cannot use Java native equals method on Where");
		}

		/**
		 * Get a string representation of the conditions.
		 */
		@Override
		public String toString() {
			final List<String> merged = new ArrayList<>();

			for (int i = 0; i < this.conditions.size(); i++) {
				final String condition = this.conditions.get(i);
				final Object value = this.values.get(i);

				merged.add(condition.replace("?", "").trim() + " " + value);
			}

			return String.join(", ", merged);
		}

		/**
		 * Build SQL from the conditions
		 *
		 * @return
		 */
		public String buildSql() {
			return String.join(" AND ", this.conditions);
		}

		/**
		 * Create a new Where clause
		 *
		 * @return
		 */
		public static Where builder() {
			return new Where();
		}
	}

	/**
	 * Helps to create new database tables preventing SQL syntax errors
	 */
	@Getter
	@RequiredArgsConstructor
	public final static class TableCreator {

		/**
		 * The table name.
		 */
		private final String name;

		/**
		 * The table columns.
		 */
		private final List<TableRow> columns = new ArrayList<>();

		/**
		 * The primary column.
		 */
		private String primaryColumn;

		/**
		 * Add a new column of the given name and data type.
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator add(final String name, final String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).build());

			return this;
		}

		/**
		 * Add a new column of the given name and data type that is "NOT NULL".
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator addNotNull(final String name, final String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).notNull(true).build());

			return this;
		}

		/**
		 * Add a new column of the given name and data type that is "NOT NULL AUTO_INCREMENT".
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator addAutoIncrement(final String name, final String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).autoIncrement(true).build());

			return this;
		}

		/**
		 * Add a new column of the given name and data type that has a default value.
		 *
		 * @param name
		 * @param dataType
		 * @param def
		 * @return
		 */
		public TableCreator addDefault(final String name, final String dataType, final String def) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).defaultValue(def).build());

			return this;
		}

		/**
		 * Marks which column is the primary key.
		 *
		 * @param primaryColumn
		 * @return
		 */
		public TableCreator setPrimaryColumn(final String primaryColumn) {
			this.primaryColumn = primaryColumn;

			return this;
		}
	}

	/**
	 * A helper class to read results set - we cannot use a simple Consumer since it does not
	 * catch exceptions automatically.
	 */
	public interface ResultReader {

		/**
		 * Reads and process the given results set, we handle exceptions for you.
		 *
		 * @param set
		 * @throws SQLException
		 */
		void accept(SimpleResultSet set) throws SQLException;
	}
}

/*
 * Internal helper to create table rows.
 */
@Data
@Builder
final class TableRow {

	/**
	 * The table row name.
	 */
	private final String name;

	/**
	 * The data type.
	 */
	private final String dataType;

	/**
	 * Is this row NOT NULL?
	 */
	private final Boolean notNull;

	/**
	 * Does this row have a default value?
	 */
	private final String defaultValue;

	/**
	 * Is this row NOT NULL AUTO_INCREMENT?
	 */
	private final Boolean autoIncrement;
}

/**
 * Stores last known credentials from the connect() functions
 */
@Getter
@RequiredArgsConstructor
final class LastCredentials {

	/**
	 * The connecting URL, for example:
	 * <p>
	 * jdbc:mysql://host:port/database
	 */
	private final String url;

	/**
	 * The user name for the database.
	 */
	private final String databaseName;

	/**
	 * The user name for the database.
	 */
	private final String user;

	/**
	 * The password for the database.
	 */
	private final String password;
}