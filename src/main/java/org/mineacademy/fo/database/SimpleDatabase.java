package org.mineacademy.fo.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple MySQL database
 * <p>
 * Before running queries make sure to call connect() methods.
 * <p>
 * You can also override {@link #onConnected()} to run your code after the
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
	 * The last credentials from the connect function, or null if never called
	 */
	private LastCredentials lastCredentials;

	/**
	 * Map of variables you can use with the {} syntax in SQL
	 */
	private final StrictMap<String, String> sqlVariables = new StrictMap<>();

	/**
	 * Indicates that {@link #batchUpdate(List)} is ongoing
	 */
	private boolean batchUpdateGoingOn = false;

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
		connect(host, port, database, user, password, null);
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
		connect(host, port, database, user, password, table, true);
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
		connect("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&autoReconnect=" + autoReconnect, user, password, table);
	}

	/**
	 * Connects to the database
	 *
	 * @param url
	 * @param user
	 * @param password
	 */
	public final void connect(final String url, final String user, final String password) {
		connect(url, user, password, null);
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

		// Close any open connection
		close();

		try {
			if (ReflectionUtil.isClassAvailable("com.mysql.cj.jdbc.Driver"))
				Class.forName("com.mysql.cj.jdbc.Driver");

			else {
				Common.warning("Your database driver is outdated, MySQL 8.0 is unsupported. If you encounter issues, use MariaDB instead.");

				Class.forName("com.mysql.jdbc.Driver");
			}

			this.lastCredentials = new LastCredentials(url, user, password, table);
			this.connection = DriverManager.getConnection(url, user, password);

			onConnected();

		} catch (final Exception e) {

			if (Common.getOrEmpty(e.getMessage()).contains("No suitable driver found"))
				Common.logFramed(true,
						"Failed to look up MySQL driver",
						"If you had MySQL disabled, then enabled it and reload,",
						"this is normal - just restart.",
						"",
						"You have have access to your server machine, try installing",
						"https://dev.mysql.com/downloads/connector/j/5.1.html#downloads",
						"",
						"If this problem persists after a restart, please contact",
						"your hosting provider.");
			else
				Common.logFramed(true,
						"Failed to connect to MySQL database",
						"URL: " + url,
						"Error: " + e.getMessage());

			Remain.sneaky(e);
		}
	}

	/**
	 * Attempts to connect using last known credentials. Fails gracefully if those are not provided
	 * i.e. connect function was never called
	 */
	private final void connectUsingLastCredentials() {
		if (lastCredentials != null)
			connect(lastCredentials.url, lastCredentials.user, lastCredentials.password, lastCredentials.table);
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
	 * Attempts to close the connection, if not null
	 */
	public final void close() {
		if (connection != null)
			try {
				connection.close();

			} catch (final SQLException e) {
				Common.error(e, "Error closing MySQL connection!");
			}
	}

	// --------------------------------------------------------------------
	// Querying
	// --------------------------------------------------------------------

	/**
	 * Insert the given column-values pairs into the {@link #getTable()}
	 *
	 * @param columsAndValues
	 */
	protected final void insert(@NonNull SerializedMap columsAndValues) {
		this.insert("{table}", columsAndValues);
	}

	/**
	 * Insert the given column-values pairs into the given table
	 *
	 * @param table
	 * @param columsAndValues
	 */
	protected final void insert(String table, @NonNull SerializedMap columsAndValues) {
		final String columns = Common.join(columsAndValues.keySet());
		final String values = Common.join(columsAndValues.values(), ", ", value -> value == null || value.equals("NULL") ? "NULL" : "'" + SerializeUtil.serialize(value).toString() + "'");
		final String duplicateUpdate = Common.join(columsAndValues.entrySet(), ", ", entry -> entry.getKey() + "=VALUES(" + entry.getKey() + ")");

		update("INSERT INTO " + replaceVariables(table) + " (" + columns + ") VALUES (" + values + ") ON DUPLICATE KEY UPDATE " + duplicateUpdate + ";");
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
		checkEstablished();

		if (!isConnected())
			connectUsingLastCredentials();

		sql = replaceVariables(sql);
		Valid.checkBoolean(!sql.contains("{table}"), "Table not set! Either use connect() method that specifies it or call addVariable(table, 'yourtablename') in your constructor!");

		Debugger.debug("mysql", "Updating MySQL with: " + sql);

		try {
			final Statement statement = connection.createStatement();

			statement.executeUpdate(sql);
			statement.close();

		} catch (final SQLException e) {
			handleError(e, "Error on updating MySQL with: " + sql);
		}
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
		checkEstablished();

		if (!isConnected())
			connectUsingLastCredentials();

		sql = replaceVariables(sql);

		Debugger.debug("mysql", "Querying MySQL with: " + sql);

		try {
			final Statement statement = connection.createStatement();
			final ResultSet resultSet = statement.executeQuery(sql);

			return resultSet;

		} catch (final SQLException ex) {
			handleError(ex, "Error on querying MySQL with: " + sql);
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
			final Statement batchStatement = getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			final int processedCount = sqls.size();

			// Prevent automatically sending db instructions
			getConnection().setAutoCommit(false);

			for (final String sql : sqls)
				batchStatement.addBatch(replaceVariables(sql));

			if (processedCount > 10_000)
				Common.log("Updating your database (" + processedCount + " entries)... PLEASE BE PATIENT THIS WILL TAKE "
						+ (processedCount > 50_000 ? "10-20 MINUTES" : "5-10 MINUTES") + " - If server will print a crash report, ignore it, update will proceed.");

			// Set the flag to start time notifications timer
			batchUpdateGoingOn = true;

			// Notify console that progress still is being made
			new Timer().scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					if (batchUpdateGoingOn)
						Common.log("Still executing, " + RandomUtil.nextItem("keep calm", "stand by", "watch the show", "check your db", "drink water", "call your friend") + " and DO NOT SHUTDOWN YOUR SERVER.");
					else
						cancel();
				}
			}, 1000 * 30, 1000 * 30);

			// Execute
			batchStatement.executeBatch();

			// This will block the thread
			getConnection().commit();

			//Common.log("Updated " + processedCount + " database entries.");

		} catch (final Throwable t) {
			final List<String> errorLog = new ArrayList<>();

			errorLog.add(Common.consoleLine());
			errorLog.add(" [" + TimeUtil.getFormattedDateShort() + "] Failed to save batch sql, please contact the plugin author with this file content: " + t);
			errorLog.add(Common.consoleLine());

			for (final String statement : sqls)
				errorLog.add(replaceVariables(statement));

			FileUtil.write("sql-error.log", sqls);

			t.printStackTrace();

		} finally {
			try {
				getConnection().setAutoCommit(true);

			} catch (final SQLException ex) {
				ex.printStackTrace();
			}

			// Even in case of failure, cancel
			batchUpdateGoingOn = false;
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
		checkEstablished();

		if (!isConnected())
			connectUsingLastCredentials();

		sql = replaceVariables(sql);

		Debugger.debug("mysql", "Preparing statement: " + sql);

		return connection.prepareStatement(sql);
	}

	/**
	 * Is the connection established, open and valid?
	 * Performs a blocking ping request to the database
	 *
	 * @return whether the connection driver was set
	 */
	protected final boolean isConnected() {
		if (!isLoaded())
			return false;

		try {
			return !connection.isClosed() && connection.isValid(0);

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
			Common.log("You need to update your MySQL provider driver. We switched to support unicode using 4 bits length because the previous system only supported 3 bits.");
			Common.log("Some characters such as smiley or Chinese are stored in 4 bits so they would crash the 3-bit database leading to more problems. Most hosting providers have now widely adopted the utf8mb4_unicode_520_ci encoding you seem lacking. Disable MySQL connection or update your driver to fix this.");
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
	 * Return the table from last connection, throwing an error if never connected
	 *
	 * @return
	 */
	protected final String getTable() {
		checkEstablished();

		return Common.getOrEmpty(lastCredentials.table);
	}

	/**
	 * Checks if the connect() function was called
	 */
	private final void checkEstablished() {
		Valid.checkBoolean(isLoaded(), "Connection was never established");
	}

	/**
	 * Return true if the connect function was called so that the driver was loaded
	 *
	 * @return
	 */
	public final boolean isLoaded() {
		return connection != null;
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
		sqlVariables.put(name, value);
	}

	/**
	 * Replace the {table} and {@link #sqlVariables} in the sql query
	 *
	 * @param sql
	 * @return
	 */
	protected final String replaceVariables(String sql) {

		for (final Entry<String, String> entry : sqlVariables.entrySet())
			sql = sql.replace("{" + entry.getKey() + "}", entry.getValue());

		return sql.replace("{table}", getTable());
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