package org.mineacademy.fo.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.mineacademy.fo.Common;

/**
 * Represents a simple MySQL database
 */
public final class MySQL {

	/**
	 * The connecting URL, for example:
	 *
	 * jdbc:mysql://host:port/database
	 */
	private final String url;

	/**
	 * The database user
	 */
	private final String user;

	/**
	 * The database password
	 */
	private final String password;

	/**
	 * The established connection
	 */
	private Connection connection;

	/**
	 * Internal flag allowing us to reconnect more times if the first attempt fails
	 */
	private int reconnectAttempts = 0;

	/**
	 * Creates a new MySQL connection and connects
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 */
	public MySQL(String host, int port, String database, String user, String password) {
		this(host, port, database, user, password, true);
	}

	/**
	 * Creates a new MySQL connection and connects
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 * @param autoReconnect
	 */
	public MySQL(String host, int port, String database, String user, String password, boolean autoReconnect) {
		this("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=" + autoReconnect, user, password);
	}

	/**
	 * Creates a new MySQL connection and connects
	 *
	 * @param url
	 * @param user
	 * @param password
	 */
	public MySQL(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;

		connect();
	}

	/**
	 * Connects to the database
	 */
	private void connect() {
		try {
			this.connection = DriverManager.getConnection(url, this.user, this.password);
		} catch (final SQLException e) {
			e.printStackTrace();

			Common.logFramed(true, "Failed to estabilish connection to database", "Error: " + e.getMessage());
		}
	}

	/**
	 * Attempts to close the connection, if not null
	 */
	public void close() {
		try {
			if (this.connection != null)
				this.connection.close();

		} catch (final SQLException e) {
			Common.error(e, "Error closing MySQL connection!");
		}
	}

	/**
	 * Attempts to execute a new update query
	 *
	 * @param sql
	 */
	public void update(String sql) {
		openIfClosed();

		try {
			final Statement st = this.connection.createStatement();

			st.executeUpdate(sql);
			st.close();

		} catch (final SQLException e) {
			connect();

			if (reconnectAttempts++ > 0)
				update(sql);

			else {
				reconnectAttempts = 0;

				Common.error(e, "Error on updating MySQL with: " + sql);
			}
		}
	}

	/**
	 * Attempts to execute a new query
	 *
	 * @param qry
	 * @return
	 */
	public ResultSet query(String qry) {
		openIfClosed();

		ResultSet rs = null;

		try {
			final Statement st = this.connection.createStatement();

			rs = st.executeQuery(qry);

		} catch (final SQLException e) {
			connect();

			if (reconnectAttempts++ > 0)
				query(qry);

			else {
				reconnectAttempts = 0;

				Common.error(e, "Error on querying MySQL with: " + qry);
			}
		}

		return rs;
	}

	/**
	 * Attempts to return a prepared statement
	 *
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
		return connection.prepareStatement(sql);
	}

	/**
	 * Opens the connection if it has been closed
	 */
	private void openIfClosed() {
		try {
			if (connection == null || connection.isClosed() || !connection.isValid(0))
				connect();

		} catch (final SQLException ex) {
			Common.error(ex, "Error reconnecting to MySQL");
		}
	}
}