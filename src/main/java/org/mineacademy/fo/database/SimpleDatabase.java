package org.mineacademy.fo.database;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple MySQL database
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
	 * The established connection, or null if none
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private volatile Connection connection;

	/**
	 * Map of variables you can use with the {} syntax in SQL
	 */
	private final StrictMap<String, String> sqlVariables = new StrictMap<>();

	/**
	 * The last credentials from the connect function, or null if never called
	 */
	private LastCredentials lastCredentials;

	/**
	 * Indicates that {@link #batchUpdate(List)} is ongoing
	 */
	private boolean batchUpdateGoingOn = false;

	/**
	 * Private indicator that we are connecting to database right now
	 */
	private boolean connecting = false;

	/**
	 * Optional Hikari data source (you plugin needs to include com.zaxxer.HikariCP library in its plugin.yml (MC 1.16+ required)
	 */
	private Object hikariDataSource;

	// --------------------------------------------------------------------
	// Connecting
	// --------------------------------------------------------------------

	/**
	 * Attempts to establish a new database connection
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
	 * Attempts to establish a new database connection,
	 * you can then use {table} in SQL to replace with your table name
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
	 * Attempts to establish a new database connection
	 * you can then use {table} in SQL to replace with your table name
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
	 * Connects to the database
	 *
	 * @param url
	 * @param user
	 * @param password
	 */
	public final void connect(final String url, final String user, final String password) {
		this.connect(url, user, password, null);
	}

	/**
	 * Connects to the database
	 * you can then use {table} in SQL to replace with your table name*
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @param table
	 */
	public final void connect(final String url, final String user, final String password, final String table) {

		this.connecting = true;

		try {

			// Close any open connection
			this.close();

			// Support local storage of databases on your disk, typically in your plugin's folder
			// Make sure to load the library using "libraries" and "legacy-libraries" feature in plugin.yml:
			//
			// libraries:
			// - org.xerial:sqlite-jdbc:3.36.0.3
			//
			// legacy-libraries:
			// - org.xerial:sqlite-jdbc:3.36.0.3
			//
			if (url.startsWith("jdbc:sqlite")) {
				Class.forName("org.sqlite.JDBC");

				this.connection = DriverManager.getConnection(url);
			}

			// Avoid using imports so that Foundation users don't have to include Hikari, you can
			// optionally load the library using "libraries" and "legacy-libraries" feature in plugin.yml:
			//
			// libraries:
			// - com.zaxxer:HikariCP:5.0.1
			// legacy-libraries:
			//  - org.slf4j:slf4j-simple:1.7.36
			//  - org.slf4j:slf4j-api:1.7.36
			//  - com.zaxxer:HikariCP:4.0.3
			//
			else if (ReflectionUtil.isClassAvailable("com.zaxxer.hikari.HikariConfig")) {

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
					Common.warning("Could not get HikariCP connection, please report this with the information below to github.com/kangarko/foundation");
					Common.warning("Method: " + getConnection);
					Common.warning("Arguments: " + Common.join(getConnection.getParameters()));

					t.printStackTrace();
				}
			}

			/*
			 * Check for JDBC Drivers (MariaDB, MySQL or Legacy MySQL)
			 */
			else {
				if (url.startsWith("jdbc:mariadb://") && ReflectionUtil.isClassAvailable("org.mariadb.jdbc.Driver"))
					Class.forName("org.mariadb.jdbc.Driver");

				else if (url.startsWith("jdbc:mysql://") && ReflectionUtil.isClassAvailable("com.mysql.cj.jdbc.Driver"))
					Class.forName("com.mysql.cj.jdbc.Driver");

				else {
					Common.warning("Your database driver is outdated, switching to MySQL legacy JDBC Driver. If you encounter issues, consider updating your database or switching to MariaDB. You can safely ignore this warning");

					Class.forName("com.mysql.jdbc.Driver");
				}

				this.connection = user != null && password != null ? DriverManager.getConnection(url, user, password) : DriverManager.getConnection(url);
			}

			this.lastCredentials = new LastCredentials(url, user, password, table);
			this.onConnected();

		} catch (final Exception ex) {

			if (Common.getOrEmpty(ex.getMessage()).contains("No suitable driver found"))
				Common.logFramed(true,
						"Failed to look up database driver! If you had database disabled,",
						"then enable it and reload - this is expected.",
						"",
						"You have have access to your server machine, try installing",
						"https://mariadb.com/downloads/connectors/connectors-data-access/",
						"",
						"If this problem persists after a restart, please contact",
						"your hosting provider with the error message below.");
			else
				Common.logFramed(true,
						"Failed to connect to database",
						"URL: " + url,
						"Error: " + ex.getMessage());

			Remain.sneaky(ex);

		} finally {
			this.connecting = false;
		}
	}

	/**
	 * Attempts to connect using last known credentials. Fails gracefully if those are not provided
	 * i.e. connect function was never called
	 */
	protected final void connectUsingLastCredentials() {
		if (this.lastCredentials != null)
			this.connect(this.lastCredentials.url, this.lastCredentials.user, this.lastCredentials.password, this.lastCredentials.table);
	}

	/**
	 * Called automatically after the first connection has been established
	 */
	protected void onConnected() {
	}

	// --------------------------------------------------------------------
	// Disconnecting
	// --------------------------------------------------------------------

	/**
	 * Attempts to close the result set if not
	 *
	 * @param resultSet
	 */
	public final void close(ResultSet resultSet) {
		try {
			if (!resultSet.isClosed())
				resultSet.close();

		} catch (final SQLException e) {
			Common.error(e, "Error closing database result set!");
		}
	}

	/**
	 * Attempts to close the connection, if not null
	 */
	public final void close() {
		try {
			if (this.connection != null)
				this.connection.close();

			if (this.hikariDataSource != null)
				ReflectionUtil.invoke("close", this.hikariDataSource);

		} catch (final SQLException e) {
			Common.error(e, "Error closing database connection!");
		}
	}

	// --------------------------------------------------------------------
	// Querying
	// --------------------------------------------------------------------

	/**
	 * Creates a database table, to be used in onConnected
	 *
	 * @param creator
	 */
	protected final void createTable(TableCreator creator) {
		String columns = "";

		for (final TableRow column : creator.getColumns()) {
			columns += (columns.isEmpty() ? "" : ", ") + "`" + column.getName() + "` " + column.getDataType();

			if (column.getAutoIncrement() != null && column.getAutoIncrement())
				columns += " NOT NULL AUTO_INCREMENT";

			else if (column.getNotNull() != null && column.getNotNull())
				columns += " NOT NULL";

			if (column.getDefaultValue() != null)
				columns += " DEFAULT " + column.getDefaultValue();
		}

		if (creator.getPrimaryColumn() != null)
			columns += ", PRIMARY KEY (`" + creator.getPrimaryColumn() + "`)";

		try {
			this.update("CREATE TABLE IF NOT EXISTS `" + creator.getName() + "` (" + columns + ") DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;");

		} catch (final Throwable t) {
			if (t.toString().contains("Unknown collation")) {
				Common.log("You need to update your database driver to support utf8mb4_unicode_520_ci collation. We switched to support unicode using 4 bits length because the previous system only supported 3 bits.");
				Common.log("Some characters such as smiley or Chinese are stored in 4 bits so they would crash the 3-bit database leading to more problems. Most hosting providers have now widely adopted the utf8mb4_unicode_520_ci encoding you seem lacking. Disable database connection or update your driver to fix this.");
			}

			else
				throw t;
		}
	}

	/**
	 * Insert the given column-values pairs into the {@link #getTable()}
	 *
	 * @param columsAndValues
	 */
	protected final void insert(@NonNull SerializedMap columsAndValues) {
		this.insert("{table}", columsAndValues);
	}

	/**
	 * Insert the given serializable object as its column-value pairs into the given table
	 *
	 * @param <T>
	 * @param table
	 * @param serializableObject
	 */
	protected final <T extends ConfigSerializable> void insert(String table, @NonNull T serializableObject) {
		this.insert(table, serializableObject.serialize());
	}

	/**
	 * Insert the given column-values pairs into the given table
	 *
	 * @param table
	 * @param columsAndValues
	 */
	protected final void insert(String table, @NonNull SerializedMap columsAndValues) {
		final String columns = Common.join(columsAndValues.keySet());
		final String values = Common.join(columsAndValues.values(), ", ", value -> value == null || value.equals("NULL") ? "NULL" : "'" + value + "'");
		final String duplicateUpdate = Common.join(columsAndValues.entrySet(), ", ", entry -> entry.getKey() + "=VALUES(" + entry.getKey() + ")");

		this.update("INSERT INTO " + this.replaceVariables(table) + " (" + columns + ") VALUES (" + values + ") ON DUPLICATE KEY UPDATE " + duplicateUpdate + ";");
	}

	/**
	 * Insert the batch map into {@link #getTable()}
	 *
	 * @param maps
	 */
	protected final void insertBatch(@NonNull List<SerializedMap> maps) {
		this.insertBatch("{table}", maps);
	}

	/**
	 * Insert the batch map into the database
	 *
	 * @param table
	 * @param maps
	 */
	protected final void insertBatch(String table, @NonNull List<SerializedMap> maps) {
		final List<String> sqls = new ArrayList<>();

		for (final SerializedMap map : maps) {
			final String columns = Common.join(map.keySet());
			final String values = Common.join(map.values(), ", ", this::parseValue);
			final String duplicateUpdate = Common.join(map.entrySet(), ", ", entry -> entry.getKey() + "=VALUES(" + entry.getKey() + ")");

			sqls.add("INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ") ON DUPLICATE KEY UPDATE " + duplicateUpdate + ";");
		}

		this.batchUpdate(sqls);
	}

	/*
	 * A helper method to insert compatible value to db
	 */
	private final String parseValue(Object value) {
		return value == null || value.equals("NULL") ? "NULL" : "'" + SerializeUtil.serialize(value).toString() + "'";
	}

	/**
	 * Attempts to execute a new update query
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown
	 *
	 * @param sql
	 */
	protected final void update(String sql) {

		if (!this.connecting)
			Valid.checkAsync("Updating database must be done async! Call: " + sql);

		this.checkEstablished();

		if (!this.isConnected())
			this.connectUsingLastCredentials();

		sql = this.replaceVariables(sql);
		Valid.checkBoolean(!sql.contains("{table}"), "Table not set! Either use connect() method that specifies it or call addVariable(table, 'yourtablename') in your constructor!");

		Debugger.debug("mysql", "Updating database with: " + sql);

		try {
			final Statement statement = this.connection.createStatement();

			statement.executeUpdate(sql);
			statement.close();

		} catch (final SQLException e) {
			this.handleError(e, "Error on updating database with: " + sql);
		}
	}

	/**
	 * Lists all rows in the given table with the "*" parameter, listing all rows
	 *
	 * @param table
	 * @param consumer
	 */
	protected final void selectAll(String table, ResultReader consumer) {
		this.select(table, "*", consumer);
	}

	/**
	 * Lists all rows in the given table with the given parameter
	 *
	 * @param table
	 * @param param
	 * @param consumer
	 */
	protected final void select(String table, String param, ResultReader consumer) {
		if (this.isLoaded()) {
			final ResultSet resultSet = this.query("SELECT " + param + " FROM " + table);

			try {
				while (resultSet.next())
					try {
						consumer.accept(resultSet);

					} catch (final Throwable t) {
						Common.log("Error reading a row from table " + table + " with param '" + param + "', aborting...");

						t.printStackTrace();
						break;
					}

			} catch (final Throwable t) {
				Common.error(t, "Error selecting rows from table " + table + " with param '" + param + "'");

			} finally {
				this.close(resultSet);
			}
		}
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
	protected final int count(String table, Object... array) {
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
	protected final int count(String table, SerializedMap conditions) {

		// Convert conditions into SQL syntax
		final Set<String> conditionsList = Common.convertSet(conditions.entrySet(), entry -> entry.getKey() + " = '" + SerializeUtil.serialize(entry.getValue()) + "'");

		// Run the query
		final String sql = "SELECT * FROM " + table + (conditionsList.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditionsList)) + ";";

		try {
			final ResultSet resultSet = this.query(sql);
			int count = 0;

			while (resultSet.next())
				count++;

			return count;

		} catch (final SQLException ex) {
			Common.throwError(ex,
					"Unable to count rows!",
					"Table: " + this.replaceVariables(table),
					"Conditions: " + conditions,
					"Query: " + sql);
		}

		return 0;
	}

	/**
	 * Attempts to execute a new query
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown
	 *
	 * @param sql
	 * @return
	 */
	protected final ResultSet query(String sql) {
		Valid.checkAsync("Sending database query must be called async, command: " + sql);

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
			if (ex instanceof SQLSyntaxErrorException && ex.getMessage().startsWith("Table") && ex.getMessage().endsWith("doesn't exist"))
				return new DummyResultSet();

			this.handleError(ex, "Error on querying database with: " + sql);
		}

		return null;
	}

	/**
	 * Executes a massive batch update
	 *
	 * @param sqls
	 */
	protected final void batchUpdate(@NonNull List<String> sqls) {
		if (sqls.size() == 0)
			return;

		try {
			final Statement batchStatement = this.getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			final int processedCount = sqls.size();

			// Prevent automatically sending db instructions
			this.getConnection().setAutoCommit(false);

			for (final String sql : sqls)
				batchStatement.addBatch(this.replaceVariables(sql));

			if (processedCount > 10_000)
				Common.log("Updating your database (" + processedCount + " entries)... PLEASE BE PATIENT THIS WILL TAKE "
						+ (processedCount > 50_000 ? "10-20 MINUTES" : "5-10 MINUTES") + " - If server will print a crash report, ignore it, update will proceed.");

			// Set the flag to start time notifications timer
			this.batchUpdateGoingOn = true;

			// Notify console that progress still is being made
			new Timer().scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					if (SimpleDatabase.this.batchUpdateGoingOn)
						Common.log("Database batch update is still processing, " + RandomUtil.nextItem("keep calm", "stand by", "watch the show", "check your db", "drink water", "call your friend") + " and DO NOT SHUTDOWN YOUR SERVER.");
					else
						this.cancel();
				}
			}, 1000 * 30, 1000 * 30);

			// Execute
			batchStatement.executeBatch();

			// This will block the thread
			this.getConnection().commit();

			if (!batchStatement.isClosed())
				batchStatement.close();

			//Common.log("Updated " + processedCount + " database entries.");

		} catch (final Throwable t) {
			final List<String> errorLog = new ArrayList<>();

			errorLog.add(Common.consoleLine());
			errorLog.add(" [" + TimeUtil.getFormattedDateShort() + "] Failed to save batch sql, please contact the plugin author with this file content: " + t);
			errorLog.add(Common.consoleLine());

			for (final String statement : sqls)
				errorLog.add(this.replaceVariables(statement));

			FileUtil.write("sql-error.log", sqls);

			t.printStackTrace();

		} finally {
			try {
				this.getConnection().setAutoCommit(true);

			} catch (final SQLException ex) {
				ex.printStackTrace();
			}

			// Even in case of failure, cancel
			this.batchUpdateGoingOn = false;
		}
	}

	/**
	 * Attempts to return a prepared statement
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown
	 *
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	protected final java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
		this.checkEstablished();

		if (!this.isConnected())
			this.connectUsingLastCredentials();

		sql = this.replaceVariables(sql);

		Debugger.debug("mysql", "Preparing statement: " + sql);
		return this.connection.prepareStatement(sql);
	}

	/**
	 * Attempts to return a prepared statement
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown
	 *
	 * @param sql
	 * @param type
	 * @param concurrency
	 *
	 * @return
	 * @throws SQLException
	 */
	protected final java.sql.PreparedStatement prepareStatement(String sql, int type, int concurrency) throws SQLException {
		this.checkEstablished();

		if (!this.isConnected())
			this.connectUsingLastCredentials();

		sql = this.replaceVariables(sql);

		Debugger.debug("mysql", "Preparing statement: " + sql);
		return this.connection.prepareStatement(sql, type, concurrency);
	}

	/**
	 * Is the connection established, open and valid?
	 * Performs a blocking ping request to the database
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
	private void handleError(Throwable t, String fallbackMessage) {
		if (t.toString().contains("Unknown collation")) {
			Common.log("You need to update your database provider driver. We switched to support unicode using 4 bits length because the previous system only supported 3 bits.");
			Common.log("Some characters such as smiley or Chinese are stored in 4 bits so they would crash the 3-bit database leading to more problems. Most hosting providers have now widely adopted the utf8mb4_unicode_520_ci encoding you seem lacking. Disable database connection or update your driver to fix this.");
		}

		else if (t.toString().contains("Incorrect string value")) {
			Common.log("Attempted to save unicode letters (e.g. coors) to your database with invalid encoding, see https://stackoverflow.com/a/10959780 and adjust it. MariaDB may cause issues, use MySQL 8.0 for best results.");

			t.printStackTrace();

		} else
			Common.throwError(t, fallbackMessage);
	}

	// --------------------------------------------------------------------
	// Non-blocking checking
	// --------------------------------------------------------------------

	/**
	 * Return if the developer called {@link #addVariable(String, String)} early enough
	 * to be registered
	 *
	 * @param key
	 * @return
	 */
	final boolean hasVariable(String key) {
		return this.sqlVariables.containsKey(key);
	}

	/**
	 * Return the table from last connection, throwing an error if never connected
	 *
	 * @return
	 */
	protected final String getTable() {
		this.checkEstablished();

		return Common.getOrEmpty(this.lastCredentials.table);
	}

	/**
	 * Checks if the connect() function was called
	 */
	private final void checkEstablished() {
		Valid.checkBoolean(this.isLoaded(), "Connection was never established, did you call connect() on " + this + "? Use isLoaded() to check.");
	}

	/**
	 * Return true if the connect function was called so that the driver was loaded
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

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	/**
	 * Helps to create new database tables preventing SQL syntax errors
	 */
	@Getter
	@RequiredArgsConstructor
	protected final static class TableCreator {

		/**
		 * The table name
		 */
		private final String name;

		/**
		 * The table columns
		 */
		private final List<TableRow> columns = new ArrayList<>();

		/**
		 * The primary column
		 */
		private String primaryColumn;

		/**
		 * Add a new column of the given name and data type
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator add(String name, String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).build());

			return this;
		}

		/**
		 * Add a new column of the given name and data type that is "NOT NULL"
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator addNotNull(String name, String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).notNull(true).build());

			return this;
		}

		/**
		 * Add a new column of the given name and data type that is "NOT NULL AUTO_INCREMENT"
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator addAutoIncrement(String name, String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).autoIncrement(true).build());

			return this;
		}

		/**
		 * Add a new column of the given name and data type that has a default value
		 *
		 * @param name
		 * @param dataType
		 * @param def
		 * @return
		 */
		public TableCreator addDefault(String name, String dataType, String def) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).defaultValue(def).build());

			return this;
		}

		/**
		 * Marks which column is the primary key
		 *
		 * @param primaryColumn
		 * @return
		 */
		public TableCreator setPrimaryColumn(String primaryColumn) {
			this.primaryColumn = primaryColumn;

			return this;
		}

		/**
		 * Create a new table
		 *
		 * @param name
		 * @return
		 */
		public static TableCreator of(String name) {
			return new TableCreator(name);
		}
	}

	/*
	 * Internal helper to create table rows
	 */
	@Data
	@Builder
	private final static class TableRow {

		/**
		 * The table row name
		 */
		private final String name;

		/**
		 * The data type
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
	 * A helper class to read results set. (We cannot use a simple Consumer since it does not
	 * catch exceptions automatically.)
	 */
	protected interface ResultReader {

		/**
		 * Reads and process the given results set, we handle exceptions for you
		 *
		 * @param set
		 * @throws SQLException
		 */
		void accept(ResultSet set) throws SQLException;
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
		 * The user name for the database
		 */
		private final String user;

		/**
		 * The password for the database
		 */
		private final String password;

		/**
		 * The table. Never used in this class, only stored for your convenience
		 */
		private final String table;
	}
}