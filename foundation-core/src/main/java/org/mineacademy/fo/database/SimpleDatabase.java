package org.mineacademy.fo.database;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidRowException;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
	 * Should we use the more modern HikariCP connector (if available)?
	 */
	@Getter
	@Setter
	private static boolean connectUsingHikari = true;

	/**
	 * The established connection, or null if none.
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private Connection connection;

	/**
	 * Map of variables you can use with the {} syntax in SQL.
	 */
	private final Map<String, String> sqlVariables = new HashMap<>();

	/**
	 * The last credentials from the connect function, or null if never called.
	 */
	private LastCredentials lastCredentials;

	/**
	 * Private indicator that we are connecting to database right now.
	 */
	private boolean connecting = false;

	/*
	 * Optional Hikari data source.
	 */
	private Object hikariDataSource;

	/*
	 * Is this a SQLite connection?
	 */
	private boolean isSQLite = false;

	// --------------------------------------------------------------------
	// Connecting
	// --------------------------------------------------------------------

	/**
	 * Attempts to establish a new database connection.
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 */
	public final void connect(final String host, final int port, final String database, final String user, final String password) {
		this.connect(host, port, database, user, password, null);
	}

	/**
	 * Attempts to establish a new database connection. You can then use {table} in SQL to replace with your table name.
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 * @param table
	 */
	public final void connect(final String host, final int port, final String database, final String user, final String password, final String table) {
		this.connect(host, port, database, user, password, table, true);
	}

	/**
	 * Attempts to establish a new database connection. You can then use {table} in SQL to replace with your table name.
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 * @param table
	 * @param autoReconnect
	 */
	public final void connect(final String host, final int port, final String database, final String user, final String password, final String table, final boolean autoReconnect) {
		this.connect("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&autoReconnect=" + autoReconnect, user, password, table);
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
	 * Connects to the database.
	 *
	 * @param url
	 * @param user
	 * @param password
	 */
	public final void connect(final String url, final String user, final String password) {
		this.connect(url, user, password, null);
	}

	/**
	 * Connects to the database. You can then use {table} in SQL to replace with your table name.
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @param table
	 */
	public final void connect(final String url, final String user, final String password, final String table) {
		try {
			this.connecting = true;

			if (url.startsWith("jdbc:sqlite")) {
				Platform.getPlugin().loadLibrary("org.xerial", "sqlite-jdbc", "3.46.0.0");

				Class.forName("org.sqlite.JDBC");

				final String urlHeadless = url.replace("jdbc:sqlite://", "");

				if (urlHeadless.split("\\.").length == 2 && !urlHeadless.contains("\\") && !urlHeadless.contains("/")) {
					final String path = FileUtil.getFile(urlHeadless).getPath();

					this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
				} else
					this.connection = DriverManager.getConnection(url);

				this.isSQLite = true;
			}

			else if (connectUsingHikari) {
				Platform.getPlugin().loadLibrary("com.zaxxer", "HikariCP", CommonCore.getJavaVersion() >= 11 ? "5.1.0" : "4.0.3");

				final Object hikariConfig = ReflectionUtil.instantiate("com.zaxxer.hikari.HikariConfig");

				if (url.startsWith("jdbc:mysql://"))
					try {
						ReflectionUtil.invoke("setDriverClassName", hikariConfig, "com.mysql.cj.jdbc.Driver");

					} catch (final Throwable t) {

						// Fall back to legacy driver
						ReflectionUtil.invoke("setDriverClassName", hikariConfig, "com.mysql.jdbc.Driver");
					}
				else if (url.startsWith("jdbc:mariadb://"))
					ReflectionUtil.invoke("setDriverClassName", hikariConfig, "org.mariadb.jdbc.Driver");

				else
					throw new FoException("Unknown database driver, expected jdbc:mysql or jdbc:mariadb, got: " + url);

				ReflectionUtil.invoke("setJdbcUrl", hikariConfig, url);

				if (user != null)
					ReflectionUtil.invoke("setUsername", hikariConfig, user);

				if (password != null)
					ReflectionUtil.invoke("setPassword", hikariConfig, password);

				final Constructor<?> dataSourceConst = ReflectionUtil.getConstructor("com.zaxxer.hikari.HikariDataSource", hikariConfig.getClass());
				final Object hikariSource = ReflectionUtil.instantiate(dataSourceConst, hikariConfig);

				this.hikariDataSource = hikariSource;

				final Method getConnection = hikariSource.getClass().getDeclaredMethod("getConnection");

				try {
					this.connection = ReflectionUtil.invoke(getConnection, hikariSource);

				} catch (final Throwable t) {
					CommonCore.warning("Could not get HikariCP connection, please report this with the information below to github.com/kangarko/foundation");
					CommonCore.warning("Method: " + getConnection);
					CommonCore.warning("Arguments: " + CommonCore.join(getConnection.getParameters()));

					t.printStackTrace();
				}
			}

			/*
			 * Check for JDBC Drivers (MariaDB, MySQL or Legacy MySQL).
			 */
			else {
				if (url.startsWith("jdbc:mariadb://")) {
					Platform.getPlugin().loadLibrary("org.mariadb.jdbc", "mariadb-java-client", "3.4.0");

					Class.forName("org.mariadb.jdbc.Driver");

				} else if (url.startsWith("jdbc:mysql://")) {
					Platform.getPlugin().loadLibrary("com.mysql", "mysql-connector-j", "9.0.0");

					Class.forName("com.mysql.cj.jdbc.Driver");

				} else {
					CommonCore.warning("Your database driver is outdated, switching to MySQL legacy JDBC Driver. If you encounter issues, consider updating your Java version. You can safely ignore this warning");

					Class.forName("com.mysql.jdbc.Driver");
				}

				this.connection = user != null && password != null ? DriverManager.getConnection(url, user, password) : DriverManager.getConnection(url);
			}

			this.lastCredentials = new LastCredentials(url, user, password, table);
			this.onConnected();

		} catch (final Exception ex) {
			if (CommonCore.getOrEmpty(ex.getMessage()).contains("No suitable driver found"))
				CommonCore.logFramed(
						"Failed to look up database driver! If you had database disabled,",
						"then enable it and reload - this is expected.",
						"",
						"You have have access to your server machine, try installing",
						"https://mariadb.com/downloads/connectors/connectors-data-access/",
						"",
						"If this problem persists after a restart, please contact",
						"your hosting provider with the error message below.");
			else
				CommonCore.logFramed(
						"Failed to connect to database",
						"URL: " + url,
						"Error: " + ex.getMessage());

			CommonCore.sneaky(ex);

		} finally {
			this.connecting = false;
		}
	}

	/**
	 * Attempts to connect using last known credentials. Fails gracefully if those are not provided,
	 * i.e. connect function was never called.
	 */
	protected final void connectUsingLastCredentials() {
		if (this.lastCredentials != null)
			this.connect(this.lastCredentials.url, this.lastCredentials.user, this.lastCredentials.password, this.lastCredentials.table);
	}

	/**
	 * Called automatically after the first connection has been established.
	 */
	protected void onConnected() {
	}

	// --------------------------------------------------------------------
	// Disconnecting
	// --------------------------------------------------------------------

	/**
	 * Attempts to close the result set if not.
	 *
	 * @param resultSet
	 */
	public final void close(final ResultSet resultSet) {
		try {
			if (!resultSet.isClosed())
				resultSet.close();

		} catch (final SQLException e) {
			CommonCore.error(e, "Error closing database result set!");
		}
	}

	/**
	 * Attempts to close the connection, if not null.
	 */
	public final void close() {
		try {
			if (this.connection != null)
				this.connection.close();

			if (this.hikariDataSource != null)
				ReflectionUtil.invoke("close", this.hikariDataSource);

		} catch (final SQLException e) {
			CommonCore.error(e, "Error closing database connection!");
		}
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
		synchronized (this.connection) {
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
				this.update("CREATE TABLE IF NOT EXISTS `" + creator.getName() + "` (" + columns + ") " + (this.isSQLite ? "" : "DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci") + ";");

			} catch (final Throwable t) {
				if (t.toString().contains("Unknown collation")) {
					CommonCore.log("You need to update your database driver to support utf8mb4_unicode_520_ci collation. We switched to support unicode using 4 bits length because the previous system only supported 3 bits.");
					CommonCore.log("Some characters such as smiley or Chinese are stored in 4 bits so they would crash the 3-bit database leading to more problems. Most hosting providers have now widely adopted the utf8mb4_unicode_520_ci encoding you seem lacking. Disable database connection or update your driver to fix this.");
				}

				else
					throw t;
			}
		}
	}

	/**
	 * Insert the given column-values pairs into the {@link #getTable()}.
	 *
	 * @param columsAndValues
	 */
	protected final void insert(@NonNull final SerializedMap columsAndValues) {
		this.insert("{table}", columsAndValues);
	}

	/**
	 * Insert the given serializable object as its column-value pairs into the given table.
	 *
	 * @param <T>
	 * @param table
	 * @param serializableObject
	 */
	protected final <T extends ConfigSerializable> void insert(final String table, @NonNull final T serializableObject) {
		this.insert(table, serializableObject.serialize());
	}

	/**
	 * Insert the given column-values pairs into the given table.
	 *
	 * @param table
	 * @param columnsAndValues
	 */
	protected final void insert(final String table, @NonNull final SerializedMap columnsAndValues) {
		synchronized (this.connection) {
			final String columns = CommonCore.join(columnsAndValues.keySet());
			final String values = CommonCore.join(columnsAndValues.values(), ", ", value -> value == null || value.equals("NULL") ? "NULL" : (value instanceof Number ? String.valueOf(value) : "'" + value + "'"));
			final String duplicateUpdate = CommonCore.join(columnsAndValues.entrySet(), ", ", entry -> entry.getKey() + "=VALUES(" + entry.getKey() + ")");

			this.update("INSERT INTO " + this.replaceVariables(table) + " (" + columns + ") VALUES (" + values + ")" + (this.isSQLite ? "" : " ON DUPLICATE KEY UPDATE " + duplicateUpdate + ";"));
		}
	}

	/**
	 * Insert the batch map into {@link #getTable()}.
	 *
	 * @param maps
	 */
	protected final void insertBatch(@NonNull final List<SerializedMap> maps) {
		this.insertBatch("{table}", maps);
	}

	/**
	 * Insert the batch map into the database
	 *
	 * @param table
	 * @param maps
	 */
	protected final void insertBatch(final String table, @NonNull final List<SerializedMap> maps) {
		synchronized (this.connection) {
			final List<String> sqls = new ArrayList<>();

			for (final SerializedMap map : maps)
				try {
					final String columns = CommonCore.join(map.keySet());
					final String values = CommonCore.join(map.values(), ", ", this::parseValue);
					final String duplicateUpdate = CommonCore.join(map.entrySet(), ", ", entry -> entry.getKey() + "=VALUES(" + entry.getKey() + ")");

					final String sql = "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ")" + (this.isSQLite ? "" : " ON DUPLICATE KEY UPDATE " + duplicateUpdate + ";");
					Debugger.debug("mysql", "Inserting batch SQL: " + sql);

					sqls.add(sql);

				} catch (final Throwable t) {
					CommonCore.error(t, "Error inserting batch map: " + map);
				}

			this.batchUpdate(sqls);
		}
	}

	/*
	 * A helper method to insert compatible value to db.
	 */
	private final String parseValue(final Object value) {
		final Object serialized = SerializeUtilCore.serialize(Language.JSON, value);

		return value == null || value.equals("NULL") ? "NULL" : "'" + serialized.toString() + "'";
	}

	/**
	 * Attempts to execute a new update query.
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown.
	 *
	 * @param sql
	 */
	protected final void update(String sql) {
		if (!this.connecting)
			ValidCore.checkBoolean(Platform.isAsync(), "Updating database must be done async! Call: " + sql);

		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);
			ValidCore.checkBoolean(!sql.contains("{table}"), "Table not set! Either use connect() method that specifies it or call addVariable(table, 'yourtablename') in your constructor!");

			Debugger.debug("mysql", "Updating database with: " + sql);

			try (Statement statement = this.connection.createStatement()) {
				statement.executeUpdate(sql);

			} catch (final SQLException e) {
				this.handleError(e, "Error on updating database with: " + sql);
			}
		}
	}

	/**
	 * Lists all rows in the given table.
	 *
	 * @param table
	 * @param consumer
	 */
	protected final void selectAll(final String table, final ResultReader consumer) {
		this.select(table, (String) null, consumer);
	}

	/**
	 * Lists all rows in the given table matching the given where clauses. Example use:
	 *
	 * select(table, "PlayerUid = " + player.getUniqueId(), resultSet);
	 *
	 * Do not forget to close the connection when done in your consumer.
	 *
	 * @param table
	 * @param where
	 * @param consumer
	 */
	protected final void select(final String table, final String where, final ResultReader consumer) {
		synchronized (this.connection) {
			if (!this.isLoaded())
				return;

			final String tableName = this.replaceVariables(table);

			try (ResultSet resultSet = this.query("SELECT * FROM " + table + (where == null ? "" : " WHERE " + where))) {
				while (resultSet.next())
					try {
						consumer.accept(new SimpleResultSet(tableName, resultSet));

					} catch (final InvalidRowException ex) {
						// Pardoned

					} catch (final Throwable t) {
						CommonCore.log("Error reading a row from table " + tableName + " where " + (where == null ? "all" : where) + ", aborting...");

						t.printStackTrace();
						break;
					}

			} catch (final Throwable t) {
				CommonCore.error(t, "Error selecting rows from table " + table + " where " + (where == null ? "all" : where));
			}
		}
	}

	/**
	 * Lists all rows in the given table matching the given where clauses. Example use:
	 *
	 * Map<String, Object> conditions = new LinkedHashMap<>();
	 *
	 * conditions.put("name", "John");
	 * conditions.put("age", 30);
	 * conditions.put("city", "%New York%");
	 *
	 * Do not forget to close the connection when done in your consumer.
	 *
	 * @param table
	 * @param where
	 * @param consumer
	 */
	protected final void select(final String table, final Map<String, Object> where, final ResultReader consumer) {
		synchronized (this.connection) {
			if (!this.isLoaded())
				return;

			final String tableName = this.replaceVariables(table);

			try (ResultSet resultSet = this.query("SELECT * FROM " + table + " " + buildWhere(where))) {
				while (resultSet.next())
					try {
						consumer.accept(new SimpleResultSet(tableName, resultSet));

					} catch (final InvalidRowException ex) {
						// Pardoned

					} catch (final Throwable t) {
						CommonCore.log("Error reading a row from table " + tableName + " where " + (where == null ? "all" : where) + ", aborting...");

						t.printStackTrace();
						break;
					}

			} catch (final Throwable t) {
				CommonCore.error(t, "Error selecting rows from table " + table + " where " + (where == null ? "all" : where));
			}
		}
	}

	/*
	 * Builds a WHERE clause from the given conditions.
	 */
	private static String buildWhere(Map<String, Object> conditions) {
		if (conditions == null || conditions.isEmpty())
			return "";

		final List<String> clauses = new ArrayList<>();

		conditions.forEach((key, value) -> {
			String clause;

			if (value instanceof String)
				clause = String.format("%s = '%s'", key, value);

			else
				clause = String.format("%s = %s", key, value);

			clauses.add(clause);
		});

		return "WHERE " + String.join(" AND ", clauses);
	}

	/**
	 * Returns the amount of rows from the given table per the key-value conditions.
	 *
	 * Example conditions: count("MyTable", "Player", "kangarko, "Status", "PENDING")
	 * This example will return all rows where column Player is equal to kangarko and Status column equals PENDING.
	 *
	 * @param table
	 * @param array
	 * @return
	 */
	protected final int count(final String table, final Object... array) {
		return this.count(table, SerializedMap.ofArray(array));
	}

	/**
	 * Returns the amount of rows from the given table per the conditions,
	 *
	 * Example conditions: SerializedMap.ofArray("Player", "kangarko, "Status", "PENDING")
	 * This example will return all rows where column Player is equal to kangarko and Status column equals PENDING.
	 *
	 * @param table
	 * @param conditions
	 * @return
	 */
	protected final int count(final String table, final SerializedMap conditions) {
		synchronized (this.connection) {
			// Convert conditions into SQL syntax
			final Collection<String> conditionsList = CommonCore.convertList(conditions.entrySet(), entry -> entry.getKey() + " = '" + SerializeUtilCore.serialize(Language.JSON, entry.getValue()) + "'");

			// Run the query
			final String sql = "SELECT * FROM " + table + (conditionsList.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditionsList)) + ";";

			try (ResultSet resultSet = this.query(sql)) {
				int count = 0;

				while (resultSet.next())
					count++;

				return count;

			} catch (final SQLException ex) {
				CommonCore.throwError(ex,
						"Unable to count rows!",
						"Table: " + this.replaceVariables(table),
						"Conditions: " + conditions,
						"Query: " + sql);
			}

			return 0;
		}
	}

	/**
	 * Attempts to execute a new query.
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown.
	 *
	 * @param sql
	 * @return
	 */
	protected final ResultSet query(String sql) {
		ValidCore.checkBoolean(Platform.isAsync(), "Sending database query must be called async, command: " + sql);

		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);

			Debugger.debug("mysql", "Querying database with: " + sql);

			try {
				final Statement statement = this.connection.createStatement();
				final ResultSet resultSet = statement.executeQuery(sql);

				return resultSet;

			} catch (final SQLException ex) {
				this.handleError(ex, "Error on querying database with: " + sql);
			}

			return null;
		}
	}

	/**
	 * Executes a massive batch update.
	 *
	 * @param sqls
	 */
	protected final void batchUpdate(@NonNull final List<String> sqls) {
		if (sqls.isEmpty())
			return;

		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			try (Statement batchStatement = this.getConnection().createStatement(this.isSQLite ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_SENSITIVE, this.isSQLite ? ResultSet.CONCUR_READ_ONLY : ResultSet.CONCUR_UPDATABLE)) {
				final int processedCount = sqls.size();

				for (final String sql : sqls)
					batchStatement.addBatch(this.replaceVariables(sql));

				if (processedCount > 10_000)
					CommonCore.log("Updating your database (" + processedCount + " entries)... PLEASE BE PATIENT THIS WILL TAKE "
							+ (processedCount > 50_000 ? "10-20 MINUTES" : "5-10 MINUTES") + " - If server will print a crash report, ignore it, update will proceed.");

				// Prevent automatically sending db instructions
				this.getConnection().setAutoCommit(false);

				try {
					// Execute
					batchStatement.executeBatch();

					// This will block the thread
					this.getConnection().commit();

				} catch (final Throwable t) {
					final List<String> errorMessage = new ArrayList<>();

					errorMessage.add("Error executing a batch update with " + sqls.size() + " SQLs:");

					for (final String sql : sqls)
						errorMessage.add(sql);

					CommonCore.error(t, CommonCore.toArray(errorMessage));

					// Cancel the task but handle the error upstream
					throw t;
				}

			} catch (final Throwable t) {
				t.printStackTrace();

			} finally {
				try {
					this.getConnection().setAutoCommit(true);

				} catch (final SQLException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * Attempts to return a prepared statement.
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown.
	 *
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	protected final java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);

			Debugger.debug("mysql", "Preparing statement: " + sql);
			return this.connection.prepareStatement(sql);
		}
	}

	/**
	 * Attempts to return a prepared statement.
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown.
	 *
	 * @param sql
	 * @param type
	 * @param concurrency
	 *
	 * @return
	 * @throws SQLException
	 */
	protected final java.sql.PreparedStatement prepareStatement(String sql, final int type, final int concurrency) throws SQLException {
		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);

			Debugger.debug("mysql", "Preparing statement: " + sql);
			return this.connection.prepareStatement(sql, type, concurrency);
		}
	}

	/**
	 * Is the connection established, open and valid?
	 *
	 * Performs a blocking ping request to the database.
	 *
	 * @return whether the connection driver was set
	 */
	protected final boolean isConnected() {
		if (!this.isLoaded())
			return false;

		try {
			if (!this.connection.isValid(0))
				return false;
		} catch (SQLException | AbstractMethodError err) {
			// Pass through silently
		}

		try {
			return !this.connection.isClosed();

		} catch (final SQLException ex) {
			return false;
		}
	}

	/*
	 * Checks if there's a collation-related error and prints warning message for the user to
	 * update his database.
	 */
	private void handleError(final Throwable t, final String fallbackMessage) {
		if (t.toString().contains("Unknown collation")) {
			CommonCore.log("You need to update your database provider driver. We switched to support unicode using 4 bits length because the previous system only supported 3 bits.");
			CommonCore.log("Some characters such as smiley or Chinese are stored in 4 bits so they would crash the 3-bit database leading to more problems. Most hosting providers have now widely adopted the utf8mb4_unicode_520_ci encoding you seem lacking. Disable database connection or update your driver to fix this.");
		}

		else if (t.toString().contains("Incorrect string value")) {
			CommonCore.log("Attempted to save unicode letters (e.g. coors) to your database with invalid encoding, see https://stackoverflow.com/a/10959780 and adjust it. MariaDB may cause issues, use MySQL 8.0 for best results.");

			t.printStackTrace();

		} else
			CommonCore.throwError(t, fallbackMessage);
	}

	// --------------------------------------------------------------------
	// Non-blocking checking
	// --------------------------------------------------------------------

	/**
	 * Return if the developer called {@link #addVariable(String, String)} early enough
	 * to be registered.
	 *
	 * @param key
	 * @return
	 */
	final boolean hasVariable(final String key) {
		return this.sqlVariables.containsKey(key);
	}

	/**
	 * Return the table from last connection, throwing an error if never connected.
	 *
	 * @return
	 */
	protected final String getTable() {
		this.checkEstablished();

		return CommonCore.getOrEmpty(this.lastCredentials.table);
	}

	/**
	 * Checks if the connect() function was called.
	 */
	private final void checkEstablished() {
		ValidCore.checkBoolean(this.isLoaded(), "Connection was never established, did you call connect() on " + this + "? Use isLoaded() to check.");
	}

	/**
	 * Return true if the connect function was called so that the driver was loaded.
	 *
	 * @return
	 */
	public final boolean isLoaded() {
		return this.connection != null;
	}

	// --------------------------------------------------------------------
	// Variables
	// --------------------------------------------------------------------

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

	/**
	 * Replace the {table} and {@link #sqlVariables} in the sql query
	 *
	 * @param sql
	 * @return
	 */
	protected final String replaceVariables(String sql) {
		for (final Entry<String, String> entry : this.sqlVariables.entrySet())
			sql = sql.replace("{" + entry.getKey() + "}", entry.getValue());

		return sql.replace("{table}", this.getTable());
	}

	/**
	 * Return if the database is SQLite
	 *
	 * @return
	 */
	protected final boolean isSQLite() {
		return this.isSQLite;
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

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

		/**
		 * Create a new table.
		 *
		 * @param name
		 * @return
		 */
		public static TableCreator of(final String name) {
			return new TableCreator(name);
		}
	}

	/*
	 * Internal helper to create table rows.
	 */
	@Data
	@Builder
	private final static class TableRow {

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

	/**
	 * Stores last known credentials from the connect() functions
	 */
	@RequiredArgsConstructor
	private final class LastCredentials {

		/**
		 * The connecting URL, for example:
		 * <p>
		 * jdbc:mysql://host:port/database
		 */
		private final String url;

		/**
		 * The user name for the database.
		 */
		private final String user;

		/**
		 * The password for the database.
		 */
		private final String password;

		/**
		 * The table. Never used in this class, only stored for your convenience.
		 */
		private final String table;
	}
}