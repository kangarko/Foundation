package org.mineacademy.fo.database;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ReflectionUtil;
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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple MySQL/MariaDB/SQLite database backed by a HikariCP
 * connection pool.
 * <p>
 * Before running queries make sure to call connect() methods.
 * <p>
 * You can also override onConnected() to run your code after the
 * connection has been established.
 */
public class SimpleDatabase {

	/**
	 * SQLite is single-writer so we always use one connection for it.
	 */
	private static final int POOL_SIZE_SQLITE = 1;

	/**
	 * Headroom subtracted from the server's max_allowed_packet when deciding whether a single batch
	 * row is too large to send, covering the SQL text and prepared-statement framing.
	 */
	private static final long PACKET_SAFETY_MARGIN_BYTES = 64L * 1024L;

	/**
	 * Default pool size for MySQL/MariaDB. Tunable via setPoolSize() before connect().
	 */
	private int poolSize = 10;

	/**
	 * Time HikariCP will wait for a free connection before throwing. Tunable via setConnectionTimeoutMs().
	 */
	private long connectionTimeoutMs = 5_000L;

	/**
	 * Maximum lifetime of a pooled connection. HikariCP rotates connections
	 * older than this to avoid stale TCP sockets that have been closed
	 * server-side without our knowledge. Tunable via setMaxLifetimeMs().
	 */
	private long maxLifetimeMs = 30L * 60L * 1_000L;

	/**
	 * How often the pool pings idle connections to keep them alive. Must be
	 * less than maxLifetimeMs and greater than 30s per HikariCP docs.
	 * Tunable via setKeepaliveMs().
	 */
	private long keepaliveMs = 60L * 1_000L;

	/**
	 * Configure the maximum pool size for MySQL/MariaDB. Must be called before connect().
	 *
	 * @param poolSize
	 */
	public final void setPoolSize(final int poolSize) {
		ValidCore.checkBoolean(poolSize > 0, "Pool size must be > 0, got " + poolSize);

		this.poolSize = poolSize;
	}

	/**
	 * Configure how long HikariCP waits for a free connection. Must be called before connect().
	 *
	 * @param connectionTimeoutMs
	 */
	public final void setConnectionTimeoutMs(final long connectionTimeoutMs) {
		ValidCore.checkBoolean(connectionTimeoutMs >= 250L, "Connection timeout must be >= 250ms, got " + connectionTimeoutMs);

		this.connectionTimeoutMs = connectionTimeoutMs;
	}

	/**
	 * Configure the maximum lifetime of a pooled connection. Must be called before connect().
	 *
	 * @param maxLifetimeMs
	 */
	public final void setMaxLifetimeMs(final long maxLifetimeMs) {
		ValidCore.checkBoolean(maxLifetimeMs >= 30_000L, "Max lifetime must be >= 30s, got " + maxLifetimeMs);

		this.maxLifetimeMs = maxLifetimeMs;
	}

	/**
	 * Configure how often the pool pings idle connections. Must be called before connect().
	 *
	 * @param keepaliveMs
	 */
	public final void setKeepaliveMs(final long keepaliveMs) {
		ValidCore.checkBoolean(keepaliveMs >= 30_000L, "Keepalive must be >= 30s, got " + keepaliveMs);

		this.keepaliveMs = keepaliveMs;
	}

	/**
	 * Map of variables you can use with the {} syntax in SQL.
	 */
	private final Map<String, String> sqlVariables = new HashMap<>();

	/**
	 * The active HikariCP pool, or null if not connected.
	 */
	private HikariDataSource dataSource;

	/**
	 * Guards the pool lifecycle so disconnect() and reconnect() cannot rip a borrowed
	 * connection out from under an in-flight query (reload races with AsyncPlayerPreLoginEvent).
	 * Read lock is held for the lifetime of one borrowed connection; write lock is held by
	 * connect() and disconnect(). Fair ordering ensures disconnect() does not starve under
	 * sustained query load.
	 */
	private final ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock(true);

	/**
	 * The last credentials from the connect function, or null if never called.
	 */
	private LastCredentials lastCredentials;

	/*
	 * Is this a SQLite connection?
	 */
	private boolean isSQLite = false;

	/*
	 * The server's max_allowed_packet in bytes, read once on connect for MySQL/MariaDB, or 0 when
	 * unknown or SQLite. Used by insertBatch to skip rows that would exceed it.
	 */
	private long maxAllowedPacketBytes = 0;

	// --------------------------------------------------------------------
	// Connecting
	// --------------------------------------------------------------------

	/**
	 * Return true if connect() has been called and the pool is open.
	 *
	 * @return
	 */
	public final boolean isConnected() {
		return this.dataSource != null && !this.dataSource.isClosed();
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
		this.connect("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&socketTimeout=10000&connectTimeout=5000", user, password);
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
		this.connectionLock.writeLock().lock();

		try {
			this.connectInternal(url, user, password);

		} finally {
			this.connectionLock.writeLock().unlock();
		}
	}

	/*
	 * Builds the pool and runs onConnected. Always invoked under the write lock.
	 */
	private void connectInternal(final String url, final String user, final String password) {
		// Guard against double-init leaking the previous pool's threads.
		if (this.dataSource != null)
			this.disconnect();

		this.isSQLite = false;
		this.maxAllowedPacketBytes = 0;

		try {
			final String driverClassName = this.loadDriverFor(url);

			// HikariCP needs slf4j-api on the classpath. Paper and Velocity bundle it, but bare
			// BungeeCord does not. Load it on demand so HikariCP's class initializer doesn't
			// fail with NoClassDefFoundError.
			if (!ReflectionUtil.isClassAvailable("org.slf4j.LoggerFactory"))
				Platform.getPlugin().loadLibrary("org.slf4j", "slf4j-api", "2.0.17");

			Platform.getPlugin().loadLibrary("com.zaxxer", "HikariCP", "7.0.2");

			String resolvedUrl = url;

			if (this.isSQLite) {
				final String headlessUrl = url.replace("jdbc:sqlite://", "");

				if (headlessUrl.split("\\.").length == 2 && !headlessUrl.contains("\\") && !headlessUrl.contains("/"))
					resolvedUrl = "jdbc:sqlite:" + FileUtil.getFile(headlessUrl).getPath();
			}

			final HikariConfig config = new HikariConfig();

			config.setJdbcUrl(resolvedUrl);

			// Force the specific driver. DriverManager iterates all registered drivers and
			// returns the first that accepts the URL, so a third-party plugin shading a broken
			// or relocated JDBC driver can hijack our connection. Setting this explicitly
			// pins HikariCP to the driver we just loaded.
			config.setDriverClassName(driverClassName);

			if (user != null)
				config.setUsername(user);

			if (password != null)
				config.setPassword(password);

			config.setPoolName(Platform.getPlugin().getName() + "-" + this.getClass().getSimpleName());
			config.setConnectionTimeout(this.connectionTimeoutMs);

			// Force a query-based liveness check so PoolBase.isConnectionAlive() never falls back
			// to JDBC4 isValid()/setNetworkTimeout, which throws SQLNonTransientConnectionException
			// ("No operations allowed after connection closed") on TCP connections silently dropped
			// by NAT/firewall. Both SQLite and MySQL drivers handle SELECT 1.
			config.setConnectionTestQuery("SELECT 1");
			config.setValidationTimeout(2_000L);

			if (!this.isSQLite) {
				config.setMaxLifetime(this.maxLifetimeMs);
				config.setKeepaliveTime(this.keepaliveMs);
			}

			final int effectivePoolSize = this.isSQLite ? POOL_SIZE_SQLITE : this.poolSize;

			config.setMaximumPoolSize(effectivePoolSize);
			config.setMinimumIdle(effectivePoolSize);

			this.dataSource = new HikariDataSource(config);

			String databaseName = url.substring(url.lastIndexOf("/") + 1);
			databaseName = databaseName.contains("?") ? databaseName.substring(0, databaseName.indexOf("?")) : databaseName;

			this.lastCredentials = new LastCredentials(url, databaseName, user, password);

			if (!this.isSQLite)
				this.loadMaxAllowedPacket();

			// Create tables automatically
			for (final Table createdTable : this.getTables()) {
				final TableCreator creator = new TableCreator(createdTable.getName());

				try {
					createdTable.onTableCreate(creator);

					this.createTable(creator);

				} catch (final Exception ex) {
					CommonCore.error(ex, "Error creating table " + createdTable.getName() + ", aborting.");
					this.disconnect();

					return;
				}
			}

			try {
				this.onConnected();

			} catch (final Exception ex) {
				CommonCore.error(ex, "Error after connecting to database, shutting down the plugin for safety.");
				this.disconnect();
				Platform.getPlugin().disable();

				return;
			}

		} catch (final Throwable throwable) {
			// Ensure no half-built pool keeps housekeeper threads alive when we re-throw.
			this.disconnect();

			this.handleConnectError(throwable, url, user);
		}
	}

	/*
	 * Resolves the JDBC driver class for the given URL, downloading the matching
	 * artifact at runtime. Sets isSQLite as a side effect.
	 */
	private String loadDriverFor(final String url) {
		if (url.startsWith("jdbc:sqlite")) {
			Platform.getPlugin().loadLibrary("org.xerial", "sqlite-jdbc", "3.51.3.0");

			this.isSQLite = true;

			return "org.sqlite.JDBC";
		}

		if (url.startsWith("jdbc:mysql://")) {
			Platform.getPlugin().loadLibrary("com.mysql", "mysql-connector-j", "9.6.0");

			return "com.mysql.cj.jdbc.Driver";
		}

		if (url.startsWith("jdbc:mariadb://")) {
			Platform.getPlugin().loadLibrary("org.mariadb.jdbc", "mariadb-java-client", "3.5.8");

			return "org.mariadb.jdbc.Driver";
		}

		throw new FoException("Unknown database driver '" + url + "'. Only SQLite, MySQL and MariaDB (which supports MariaDB automatically) are supported at this time.", false);
	}

	/*
	 * Reads the server's max_allowed_packet once after connecting (MySQL/MariaDB only) so insertBatch
	 * can skip rows that would exceed it. Leaves the value at 0 (protection disabled) if it cannot be read.
	 */
	private void loadMaxAllowedPacket() {
		try (Connection connection = this.dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT @@max_allowed_packet")) {

			if (resultSet.next())
				this.maxAllowedPacketBytes = resultSet.getLong(1);

		} catch (final SQLException ex) {
			Debugger.debug("mysql", "Could not read max_allowed_packet, oversized-row protection disabled: " + ex.getMessage());
		}
	}

	/*
	 * HikariCP wraps the underlying SQLException in PoolInitializationException; walk
	 * the cause chain to surface the original message in the user-facing error.
	 */
	private void handleConnectError(final Throwable throwable, final String url, final String user) {
		Throwable cause = throwable;

		while (cause != null && !(cause instanceof SQLException))
			cause = cause.getCause();

		final String message = cause != null ? CommonCore.getOrEmpty(cause.getMessage()) : CommonCore.getOrEmpty(throwable.getMessage());

		if (cause instanceof SQLNonTransientConnectionException && message.equals("Too many connections"))
			CommonCore.throwErrorUnreported(throwable,
					"Too many connections to the database!",
					"URL: " + url,
					"User: " + user,
					"",
					"If increasing `max_connections` in your database config (not in our plugin)",
					"is not possible, run these two SQL queries and report results to us:",
					"SHOW STATUS WHERE `variable_name` = 'Threads_connected';",
					"and:",
					"SHOW PROCESSLIST;");

		else if (throwable instanceof UnsatisfiedLinkError)
			CommonCore.throwErrorUnreported(throwable,
					"Failed to load the database driver",
					"URL: " + url,
					"User: " + user,
					"Error: " + throwable.getMessage(),
					"",
					"Please make sure you have the correct driver for your database installed.",
					"Check the console for more information.");

		else if (message.contains("Communications link failure") || message.contains("Could not connect to") || message.contains("invalid database address") || message.contains("Connection refused")
				|| message.contains("Access denied for user") || message.contains("Could not create connection to database server") || message.contains("Incorrect port value"))
			CommonCore.throwErrorUnreported(throwable,
					"Failed to connect to a database",
					"URL: " + url,
					"User: " + user,
					"Error: " + message);

		else
			CommonCore.throwError(throwable,
					"Failed to connect to a database",
					"URL: " + url,
					"User: " + user,
					"Error: " + message);
	}

	/**
	 * Called automatically after the first connection has been established.
	 */
	protected void onConnected() {
	}

	/**
	 * Closes the connection pool, if not null.
	 */
	public final void disconnect() {
		this.connectionLock.writeLock().lock();

		try {
			if (this.dataSource != null) {
				try {
					this.dataSource.close();

				} catch (final Throwable ex) {
					CommonCore.error(ex, "Error closing database connection pool!");

				} finally {
					this.dataSource = null;
				}
			}
		} finally {
			this.connectionLock.writeLock().unlock();
		}
	}

	/*
	 * Checks if we connected to the database
	 */
	private final void ensureConnected() {
		ValidCore.checkBoolean(this.isConnected(), "Connection was never established, did you call connect() on " + this + "?");
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

			if (this.isSQLite)
				if (dataType.equals("datetime") || dataType.equals("longtext"))
					dataType = "text";

				else if (dataType.startsWith("varchar"))
					dataType = "text";

				else if (dataType.startsWith("bigint"))
					dataType = "integer";

				else if (creator.getPrimaryColumn() != null && creator.getPrimaryColumn().equals(column.getName()))
					dataType = "INTEGER PRIMARY KEY";

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

			for (final Object value : columnsAndValues.values())
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

	/**
	 * Appends the given map into the database table as a new row.
	 *
	 * @param table
	 * @param columnsAndValues
	 */
	protected final void insert(final Table table, @NonNull final SerializedMap columnsAndValues) {
		final String tableName = this.replaceVariables(table.getName());

		// Building column names and placeholders for values (?)
		final String columns = String.join(",", columnsAndValues.keySet());
		final String placeholders = columnsAndValues.keySet().stream().map(key -> "?").collect(Collectors.joining(","));

		final StringBuilder sql = new StringBuilder("INSERT " + (this.isSQLite ? "OR REPLACE " : "") + "INTO ").append(tableName).append(" (").append(columns).append(") VALUES (").append(placeholders).append(")");

		// Execute the query using PreparedStatement
		try (PreparedStatement preparedStatement = this.prepareStatement(sql.toString())) {
			int index = 1;

			for (final Object value : columnsAndValues.values())
				if (value == null || value.equals("NULL"))
					preparedStatement.setNull(index++, java.sql.Types.NULL);

				else if (value instanceof String)
					preparedStatement.setString(index++, (String) value);

				else
					preparedStatement.setObject(index++, SerializeUtilCore.serialize(Language.JSON, value));

			Debugger.debug("mysql", "[insert] Running SQL: " + preparedStatement.toString().replace("\n", ""));

			preparedStatement.executeUpdate();

		} catch (final SQLException ex) {
			CommonCore.error(ex,
					"Error inserting into database",
					"Table: " + tableName,
					"Query: " + sql);
		}
	}

	/**
	 * Insert the batch map into the database as new rows.
	 *
	 * @param table
	 * @param maps
	 */
	protected final void insertBatch(final Table table, @NonNull final List<SerializedMap> maps) {
		// Unlike the synchronous query methods, this runs from the async RowQueueWriter once per second
		// regardless of connection state, so a missing pool is an expected, recoverable condition rather
		// than developer error: a failed or aborted initial connect that left the plugin running without
		// a database, or the brief teardown window during a settings reload. Skip the write instead of
		// throwing a fatal, auto-reported exception. borrowConnection() below still parks on the read lock,
		// so a normal in-progress reload completes and writes via the new pool rather than being skipped.
		if (!this.isConnected())
			return;

		if (maps.isEmpty())
			return;

		final String columns = String.join(", ", maps.get(0).keySet());
		final String placeholders = String.join(", ", Collections.nCopies(maps.get(0).size(), "?"));
		final String sql = "INSERT INTO " + table.getName() + " (" + columns + ") VALUES (" + placeholders + ");";

		Debugger.debug("mysql", "Batch insert SQL: " + sql);

		// Serialize each row once (reused below for the prepared statement) and skip any single row whose
		// payload exceeds the server's max_allowed_packet. With rewriteBatchedStatements disabled the driver
		// sends each row as its own packet, so one oversized row would otherwise abort the whole batch (and
		// kill the connection) with "Packet for query is too large".
		final long limit = this.maxAllowedPacketBytes > 0 ? this.maxAllowedPacketBytes - PACKET_SAFETY_MARGIN_BYTES : Long.MAX_VALUE;
		final List<Object[]> rows = new ArrayList<>(maps.size());

		for (final SerializedMap map : maps)
			try {
				final Object[] values = new Object[map.size()];
				long estimatedBytes = 0;
				int index = 0;

				for (Object value : map.values()) {
					value = SerializeUtilCore.serialize(Language.JSON, value);

					if (value instanceof JsonArray)
						value = ((JsonArray) value).toString();

					value = value == null || "NULL".equals(value) ? null : value instanceof Boolean ? (boolean) value ? 1 : 0 : value;

					if (value instanceof String)
						estimatedBytes += ((String) value).getBytes(StandardCharsets.UTF_8).length;

					values[index++] = value;
				}

				if (estimatedBytes > limit) {
					CommonCore.log("Skipped saving a row into '" + table.getName() + "' because its size (" + estimatedBytes
							+ " bytes) exceeds your database server's max_allowed_packet (" + this.maxAllowedPacketBytes
							+ " bytes). Raise max_allowed_packet on your MySQL/MariaDB server to store rows this large.");

					continue;
				}

				rows.add(values);

			} catch (final Throwable t) {
				CommonCore.error(t,
						"Error processing database batch entry!",
						"Batch entry: " + map);
			}

		if (rows.isEmpty())
			return;

		// Borrow ONE connection for the whole transaction.
		try (Connection connection = this.borrowConnection()) {
			final boolean originalAutoCommit = connection.getAutoCommit();

			try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				connection.setAutoCommit(false);

				for (final Object[] values : rows) {
					int index = 1;

					for (final Object value : values)
						preparedStatement.setObject(index++, value);

					preparedStatement.addBatch();
				}

				Debugger.debug("mysql", "Executing batch of " + rows.size() + " row(s)...");
				preparedStatement.executeBatch();

				connection.commit();

			} finally {
				try {
					connection.setAutoCommit(originalAutoCommit);

				} catch (final SQLException ignored) {
					// Connection is being returned to the pool; HikariCP resets state on next borrow.
				}
			}

		} catch (final SQLException ex) {
			final String message = ex.getMessage() != null ? ex.getMessage() : "";

			if (message.contains("Can not read response from server")) {
				CommonCore.log("Error executing batch insert: " + sql);

				ex.printStackTrace();

			} else if (message.contains("Incorrect string value")) {
				CommonCore.log("Your db column's character set or collation not supporting 4-byte UTF-8 characters (e.g., emojis). "
						+ "See https://docs.mineacademy.org/general/compatibility.html#remote-database");

				ex.printStackTrace();

			} else
				CommonCore.error(ex,
						"Error executing a batch insert",
						"SQL Query: " + sql);
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

		try (Connection connection = this.borrowConnection()) {
			final boolean originalAutoCommit = connection.getAutoCommit();

			try (Statement batchStatement = connection.createStatement(this.isSQLite ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_SENSITIVE, this.isSQLite ? ResultSet.CONCUR_READ_ONLY : ResultSet.CONCUR_UPDATABLE)) {
				final int processedCount = sqls.size();

				for (final String sql : sqls)
					batchStatement.addBatch(this.replaceVariables(sql));

				if (processedCount > 10_000)
					CommonCore.log("Updating your database (" + processedCount + " entries)... PLEASE BE PATIENT THIS WILL TAKE "
							+ (processedCount > 50_000 ? "10-20 MINUTES" : "5-10 MINUTES") + " - If server will print a crash report, ignore it, update will proceed.");

				connection.setAutoCommit(false);

				batchStatement.executeBatch();

				connection.commit();

			} finally {
				try {
					connection.setAutoCommit(originalAutoCommit);

				} catch (final SQLException ignored) {
					// Connection is being returned to the pool; HikariCP resets state on next borrow.
				}
			}

		} catch (final SQLException ex) {
			CommonCore.error(ex,
					"Error executing a batch update",
					"SQLs (" + sqls.size() + "): " + sqls);
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

	/**
	 * Delete rows from the given table based on the where conditions.
	 *
	 * @param table The database table to delete from.
	 * @param where The where conditions.
	 */
	protected final void delete(final Table table, final Where where) {
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

	/*
	 * Borrows a connection from the pool, prepares a statement, and returns a proxy
	 * that closes BOTH the underlying statement and its borrowed connection on close().
	 * This keeps the existing try-with-resources call sites unchanged.
	 */
	protected final PreparedStatement prepareStatement(final String sql) {
		Connection borrowed = null;

		try {
			borrowed = this.borrowConnection();
			final PreparedStatement realStatement = borrowed.prepareStatement(sql);

			return wrapPreparedStatement(realStatement, borrowed);

		} catch (final SQLException ex) {
			if (borrowed != null)
				try {
					borrowed.close();
				} catch (final SQLException ignored) {
				}

			CommonCore.throwError(ex,
					"Error preparing a statement",
					"Query: " + sql);

			return null;
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
		this.ensureConnected();

		sql = this.replaceVariables(sql);
		ValidCore.checkBoolean(!sql.contains("{table}"), "Table not set! Either use connect() method that specifies it or call addVariable(table, 'yourtablename') in your constructor!");

		Debugger.debug("mysql", "Updating database with: " + sql);

		try (Connection connection = this.borrowConnection();
				Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);

		} catch (final SQLException ex) {
			CommonCore.error(ex,
					"Error updating database",
					"Query: " + sql);
		}
	}

	/**
	 * Attempts to execute a new query.
	 * <p>
	 * Make sure you called connect() first otherwise an error will be thrown.
	 *
	 * The returned ResultSet OWNS its borrowed Connection: closing the ResultSet
	 * also closes the underlying Statement and returns the Connection to the pool.
	 * Always wrap the call in try-with-resources.
	 *
	 * @param sql
	 * @return
	 *
	 * @deprecated Unchecked sql query, prone to SQL injections. You need to perform the validation yourself.
	 */
	@Deprecated
	protected final ResultSet queryUnsafe(String sql) {
		sql = this.replaceVariables(sql);

		Debugger.debug("mysql", "Querying database with: " + sql);

		Connection borrowed = null;
		Statement statement = null;

		try {
			borrowed = this.borrowConnection();
			statement = borrowed.createStatement();
			final ResultSet resultSet = statement.executeQuery(sql);

			return wrapResultSet(resultSet, statement, borrowed);

		} catch (final SQLException ex) {
			if (statement != null)
				try {
					statement.close();
				} catch (final SQLException ignored) {
				}

			if (borrowed != null)
				try {
					borrowed.close();
				} catch (final SQLException ignored) {
				}

			CommonCore.error(ex,
					"Error querying database",
					"Query: " + sql);
		}

		return null;
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

	// --------------------------------------------------------------------
	// Pool-aware proxies for JDBC objects whose lifetime spans method calls
	// --------------------------------------------------------------------

	/*
	 * Borrows a connection from the pool while holding the read lock for the lifetime
	 * of that connection. The returned Connection is a proxy whose close() releases
	 * the underlying connection AND the read lock. Callers MUST close it (try-with-resources).
	 *
	 * disconnect() takes the write lock, so it blocks until every in-flight borrow
	 * has been released; new borrows park on the read lock until the new pool is up.
	 * This eliminates the "Communications link failure" race during /reload under load.
	 */
	private Connection borrowConnection() throws SQLException {
		this.connectionLock.readLock().lock();

		Connection real = null;

		try {
			this.ensureConnected();

			real = this.dataSource.getConnection();

			final Connection borrowed = real;
			final AtomicBoolean closed = new AtomicBoolean(false);
			final ReentrantReadWriteLock.ReadLock readLock = this.connectionLock.readLock();

			return (Connection) Proxy.newProxyInstance(
					SimpleDatabase.class.getClassLoader(),
					new Class<?>[] { Connection.class },
					new InvocationHandler() {
						@Override
						public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
							if ("close".equals(method.getName()) && (args == null || args.length == 0)) {
								if (closed.compareAndSet(false, true))
									try {
										borrowed.close();

									} finally {
										readLock.unlock();
									}

								return null;
							}

							try {
								return method.invoke(borrowed, args);

							} catch (final InvocationTargetException ex) {
								throw ex.getCause();
							}
						}
					});

		} catch (final SQLException | RuntimeException ex) {
			if (real != null)
				try {
					real.close();
				} catch (final SQLException ignored) {
				}

			this.connectionLock.readLock().unlock();

			throw ex;
		}
	}

	/*
	 * Wraps a PreparedStatement so that close() also closes its borrowed Connection.
	 */
	private static PreparedStatement wrapPreparedStatement(final PreparedStatement statement, final Connection connection) {
		return (PreparedStatement) Proxy.newProxyInstance(
				SimpleDatabase.class.getClassLoader(),
				new Class<?>[] { PreparedStatement.class },
				new InvocationHandler() {
					@Override
					public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
						if ("close".equals(method.getName()) && (args == null || args.length == 0))
							return closeAll(statement, connection);

						try {
							return method.invoke(statement, args);

						} catch (final InvocationTargetException ex) {
							throw ex.getCause();
						}
					}
				});
	}

	/*
	 * Wraps a ResultSet so that close() also closes its parent Statement and Connection.
	 */
	private static ResultSet wrapResultSet(final ResultSet resultSet, final Statement statement, final Connection connection) {
		return (ResultSet) Proxy.newProxyInstance(
				SimpleDatabase.class.getClassLoader(),
				new Class<?>[] { ResultSet.class },
				new InvocationHandler() {
					@Override
					public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
						if ("close".equals(method.getName()) && (args == null || args.length == 0))
							return closeAll(resultSet, statement, connection);

						try {
							return method.invoke(resultSet, args);

						} catch (final InvocationTargetException ex) {
							throw ex.getCause();
						}
					}
				});
	}

	/*
	 * Closes a chain of AutoCloseables, swallowing intermediate failures so we always
	 * attempt to release the underlying Connection back to the pool.
	 */
	private static Object closeAll(final AutoCloseable... resources) throws SQLException {
		SQLException pending = null;

		for (final AutoCloseable resource : resources)
			if (resource != null)
				try {
					resource.close();

				} catch (final SQLException ex) {
					if (pending == null)
						pending = ex;
					else
						pending.setNextException(ex);

				} catch (final Exception ex) {
					if (pending == null)
						pending = new SQLException(ex);
					else
						pending.setNextException(new SQLException(ex));
				}

		if (pending != null)
			throw pending;

		return null;
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
