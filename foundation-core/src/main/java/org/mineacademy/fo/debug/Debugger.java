package org.mineacademy.fo.debug;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.HandledException;
import org.mineacademy.fo.model.BuiltByBitUpdateCheck;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for solving problems and errors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Debugger {

	private static final String CRASH_REPORT_URL = "https://matejpacan.com/api/crash-report";

	private static final Set<String> reportedExceptions = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private static final List<Supplier<Map<String, String>>> crashTags = new ArrayList<>();

	/**
	 * Add a tag to the crash error reporting.
	 *
	 * @param tag
	 */
	public static void addCrashTag(Supplier<Map<String, String>> tag) {
		crashTags.add(tag);
	}

	/**
	 * Logs a message to the console if the section name is within {@link SimpleSettings#DEBUG_SECTIONS}
	 * or if it is "*".
	 *
	 * Debug sections are by default stored in settings.yml under the "Debug" key.
	 *
	 * @param section
	 * @param messages
	 */
	public static void debug(final String section, final String... messages) {
		if (isDebugged(section))
			for (final String message : messages)
				log("[" + section + "] " + message);
	}

	/**
	 * Returns true if the section is within {@link SimpleSettings#DEBUG_SECTIONS} or if it is "*".
	 *
	 * Debug sections are by default stored in settings.yml under the "Debug" key.
	 *
	 * @param section
	 * @return
	 */
	public static boolean isDebugged(final String section) {
		return SimpleSettings.DEBUG_SECTIONS.contains(section) || SimpleSettings.DEBUG_SECTIONS.contains("*");
	}

	// ----------------------------------------------------------------------------------------------------
	// Saving errors to file
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Save an error and relevant information to an `error.log` file.
	 *
	 * <p>This method stores the error details, additional messages, system information,
	 * and the stack trace of the error for debugging purposes.</p>
	 *
	 * @param throwable The exception or error that occurred. The stack trace will be logged, and its causes will be chained.
	 * @param messages  Optional additional information that may help identify or explain the issue.
	 *
	 * <p><b>Example Usage:</b></p>
	 * <pre>{@code
	 * try {
	 *   // Some code that might throw an error
	 * } catch (Throwable t) {
	 *   saveError(t, "Something went wrong while executing this operation.");
	 * }
	 * }</pre>
	 *
	 * <p>The method also logs a message to the server console, informing the user to check the `error.log`.</p>
	 *
	 * <p>In case another error occurs while saving the log, it will attempt to log that error to the system.</p>
	 *
	 * <p>File is written to the server's base directory.</p>
	 */
	public static void saveError(Throwable throwable, final String... messages) {

		if (!Platform.hasPlatform()) {
			System.out.println("Fatal error saving error, platform not set yet.");
			System.out.println("This is typically caused by a previous error, check console.");

			throwable.printStackTrace();

			return;
		}

		final FoundationPlugin plugin = Platform.getPlugin();

		// Ignore PlugMan errors
		for (final StackTraceElement element : throwable.getStackTrace())
			if (element.getClassName().contains(".plugman.") || element.getClassName().contains(".plugmanx.")) {
				CommonCore.warning("Please do not use PlugMan to interact with " + Platform.getPlugin().getName() + " because it causes issues. Restart your server or use the inbuilt reload command instead.");

				return;
			}

		final String ignoreReason = getIgnoreReason(throwable);

		if (plugin.isErrorReportingSupported() && SimpleSettings.ERROR_AUTO_REPORTING && !BuiltByBitUpdateCheck.isNewVersionAvailable() && !(throwable instanceof OutOfMemoryError) && ignoreReason == null) {
			final Throwable finalThrowable = throwable;

			final StackTraceElement[] elements = finalThrowable.getStackTrace();
			final String key = Arrays.toString(elements);

			if (!reportedExceptions.contains(key) && elements.length > 0) {
				reportedExceptions.add(key);

				Platform.runTaskAsync(() -> {
					if (!"%%__BUILTBYBIT__%%".equals("true"))
						throw new FoException(plugin.getName(), false);

					try {
						final StringBuilder trace = new StringBuilder();
						Throwable cause = finalThrowable;

						do {
							trace.append(cause.getClass().getSimpleName()).append(": ").append(CommonCore.getOrDefault(cause.getMessage(), "(Unknown cause)")).append("\n");

							for (final StackTraceElement el : cause.getStackTrace())
								trace.append("\tat ").append(el.toString()).append("\n");
						} while ((cause = cause.getCause()) != null);

						final String traceHash = Integer.toHexString(key.hashCode());

						final Map<String, String> payload = new java.util.LinkedHashMap<>();

						payload.put("plugin_name", plugin.getName());
						payload.put("plugin_version", plugin.getVersion());
						payload.put("server_version", Platform.getPlatformVersion());
						payload.put("server_distro", Platform.getPlatformName());
						payload.put("player_count", String.valueOf(Platform.getOnlinePlayers().size()));
						payload.put("trace_hash", traceHash);
						payload.put("stack_trace", trace.toString());

						if (messages != null && messages.length > 0) {
							final StringBuilder messageBuilder = new StringBuilder();

							for (final String message : messages)
								if (message != null && !message.isEmpty() && !message.equals("\n"))
									messageBuilder.append(message).append("\n");

							if (messageBuilder.length() > 0)
								payload.put("messages", messageBuilder.toString().trim());
						}

						payload.put("bbb_nonce", "%%__NONCE__%%");

						final Map<String, String> customTags = new java.util.LinkedHashMap<>();

						for (final Supplier<Map<String, String>> supplier : crashTags) {
							final Map<String, String> map = supplier.get();

							if (map != null)
								customTags.putAll(map);
						}

						if (!customTags.isEmpty())
							payload.put("custom_tags", CommonCore.GSON.toJson(customTags));

						postCrashReport(payload);

					} catch (final Throwable t) {
						t.printStackTrace();
					}
				});
			}
		}

		saveErrorLocally(throwable, ignoreReason, messages);
	}

	private static void postCrashReport(final Map<String, String> payload) {
		try {
			final StringBuilder json = new StringBuilder("{");
			boolean first = true;

			for (final Map.Entry<String, String> entry : payload.entrySet()) {
				if (!first)
					json.append(",");

				json.append("\"").append(escapeJson(entry.getKey())).append("\":\"").append(escapeJson(entry.getValue())).append("\"");
				first = false;
			}

			json.append("}");

			final java.net.URL url = new java.net.URI(CRASH_REPORT_URL).toURL();
			final java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			connection.setRequestProperty("User-Agent", "MineAcademy-CrashReporter/1.0");
			connection.setConnectTimeout(10_000);
			connection.setReadTimeout(10_000);
			connection.setDoOutput(true);

			try (final java.io.OutputStream os = connection.getOutputStream()) {
				os.write(json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}

			final int responseCode = connection.getResponseCode();

			if (responseCode >= 400)
				CommonCore.log("Crash report failed with HTTP " + responseCode);

			connection.disconnect();

		} catch (final Throwable t) {
			CommonCore.log("Failed to send crash report: " + t.getMessage());
		}
	}

	private static String escapeJson(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	/*
	 * Returns a short, server-owner-facing reason if the given error is a known server-environment
	 * or transient infrastructure problem rather than a plugin bug, or null if it looks like a genuine
	 * bug worth reporting. The returned reason is both the gate for skipping auto-reporting and the
	 * message printed to the console so the owner can fix it themselves.
	 */
	private static String getIgnoreReason(Throwable throwable) {
		Throwable cause = throwable;

		do {
			final String msg = cause.getMessage();

			if (msg != null && (msg.contains("zip file closed") || msg.contains("has thrown a zip file error")))
				return "A plugin or server JAR was changed or reloaded while the server was running. Restart your server fully instead of reloading it.";

			if (msg != null && (msg.contains("No space left on device") || msg.contains("Disk quota exceeded") || msg.contains("Read-only file system")))
				return "Your server ran out of disk space or its disk is read-only. Free up space and make sure the plugin folder is writable.";

			if (cause instanceof java.nio.file.NoSuchFileException || cause instanceof java.io.FileNotFoundException) {
				if (msg != null && (msg.contains(".jar") || msg.contains(".paper-remapped")))
					return "A plugin or server JAR file was moved or deleted while the server was running. Restart your server fully.";
			}

			if (cause instanceof java.net.SocketTimeoutException
					|| cause instanceof java.net.SocketException
					|| cause instanceof java.net.UnknownHostException)
				return "A network connection timed out or was reset. This is a temporary connectivity issue, not a plugin bug.";

			// Redis/Jedis connection dropped (connection reset, unexpected end of stream, pool failure):
			// transient infrastructure failure, not a plugin bug. Class-name check because RedisBungee
			// relocates jedis into its internal package.
			if (cause.getClass().getName().endsWith("JedisConnectionException"))
				return "Lost connection to your Redis server. Make sure Redis is running and reachable from this server.";

			if (cause instanceof java.sql.SQLTransientConnectionException || cause instanceof java.sql.SQLTimeoutException)
				return "Lost connection to your database. This is a temporary network issue, not a plugin bug.";

			if (msg != null && msg.contains("Connection is not available, request timed out"))
				return "Your database connection pool timed out waiting for a free connection. Make sure your database server is reachable and not overloaded.";

			// Remote MySQL/MariaDB connection dropped mid-query (network reset, firewall, server restart):
			// a CommunicationsException (SQLState class 08) wrapping a socket reset / EOF. Transient
			// infrastructure failure, not a plugin bug.
			if (cause instanceof java.io.EOFException || cause.getClass().getName().endsWith("CommunicationsException"))
				return "Lost connection to your MySQL/MariaDB database mid-query. Check your network and that the database server stays online.";

			if (cause instanceof java.sql.SQLException) {
				final String sqlState = ((java.sql.SQLException) cause).getSQLState();

				if (sqlState != null && sqlState.startsWith("08"))
					return "Lost connection to your database. This is a temporary network issue, not a plugin bug.";
			}

			// A single row exceeds the server's max_allowed_packet. The user must raise that server limit;
			// the plugin already skips oversized rows on insert where it can.
			if (msg != null && (msg.contains("max_allowed_packet") || msg.contains("Packet for query is too large")))
				return "A database row exceeded your server's max_allowed_packet limit. Increase max_allowed_packet in your MySQL/MariaDB configuration.";

			// SQLite cannot open or write the database file: permissions, full disk, read-only mount,
			// or a network/cloud-synced folder. An environment problem, not a plugin bug.
			if (msg != null && (msg.contains("SQLITE_CANTOPEN") || msg.contains("unable to open database file") || msg.contains("opening db") || msg.contains("SQLITE_READONLY") || msg.contains("attempt to write a readonly database") || msg.contains("missing database")))
				return "The plugin could not open or write its SQLite database file. Make sure the server has read and write permission to the plugin folder, the disk is not full, and the folder is not read-only or on a network/cloud-synced drive.";

			if (msg != null && (msg.contains("SQLITE_BUSY") || msg.contains("SQLITE_LOCKED") || msg.contains("database is locked")))
				return "Your SQLite database is locked by another process. Make sure only one server instance uses this database file at a time.";

			if (msg != null && (msg.contains("SQLITE_CORRUPT") || msg.contains("database disk image is malformed")))
				return "Your SQLite database file is corrupted. Restore it from a backup, or stop the server and delete it so the plugin can recreate it.";

			if (msg != null && msg.contains("SQLITE_IOERR"))
				return "A disk I/O error occurred while accessing your SQLite database. Check your disk health and that there is free space.";

			if (msg != null && (msg.contains("no such table") || msg.contains("no such column")))
				return "Your SQLite database is missing a table or column. Reload the plugin so it can rebuild the schema, or restore from a backup.";

			// MySQL/MariaDB user-side database corruption: orphaned .frm with missing .ibd
			// tablespace, or InnoDB recovery failed. Not a plugin bug, the user must repair
			// their database (DROP TABLE in MySQL, let the plugin recreate it).
			if (msg != null && msg.contains("doesn't exist in engine"))
				return "Your MySQL/MariaDB table is corrupted (missing tablespace). Drop the affected table so the plugin can recreate it, or restore from a backup.";

			// User wrote an invalid human-readable time in a config such as a localized
			// "5 сек." instead of "5 seconds". A configuration error, not a plugin bug.
			if (msg != null && (msg.contains("Must define date type! Example!") || msg.contains("Expected human readable time like")))
				return "A time value in your configuration is not valid. Use English time formats like '5 seconds', '10 minutes' or '1 hour'.";
		} while ((cause = cause.getCause()) != null);

		return null;
	}

	private static void saveErrorLocally(Throwable throwable, final String ignoreReason, final String... messages) {
		final String systemInfo = "Running " + Platform.getPlatformName() + " " + Platform.getPlatformVersion() + " and Java " + System.getProperty("java.version");

		try {
			final List<String> lines = new ArrayList<>();
			final String header = Platform.getPlugin().getName() + " " + Platform.getPlugin().getVersion() + " encountered " + throwable.getClass().getSimpleName();
			final SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			// Write out header and server info
			fill(lines,
					"------------------------------------[ " + date.format(new Date()) + " ]-----------------------------------",
					header,
					systemInfo,
					"Plugins: " + CommonCore.join(Platform.getPlugins()),
					"----------------------------------------------------------------------------------------------");

			// Write additional data
			if (messages != null && !String.join("", messages).isEmpty()) {
				fill(lines, "\nMore Information: ");
				fill(lines, messages);
			}

			// Write the stack trace
			do {
				// Write the error header
				fill(lines, throwable == null ? "Unknown error" : throwable.getClass().getSimpleName() + " " + CommonCore.getOrDefault(throwable.getMessage(), "(Unknown cause)"));

				int count = 0;

				for (final StackTraceElement el : throwable.getStackTrace()) {
					count++;

					final String trace = el.toString();

					if (trace.contains("sun.reflect"))
						continue;

					if (count > 6 && trace.startsWith("net.minecraft.server"))
						break;

					fill(lines, "\t at " + el.toString());
				}
			} while ((throwable = throwable.getCause()) != null);

			fill(lines, "----------------------------------------------------------------------------------------------", System.lineSeparator());

			// Log to the console
			if (ignoreReason != null)
				CommonCore.log(header + ": " + ignoreReason + " This is not a plugin bug, so it was not reported. Full details saved to error.log. " + systemInfo);
			else
				CommonCore.log(header + "! Please check your error.log and report this issue with the information in that file. " + systemInfo);

			// Finally, save the error file
			FileUtil.write("error.log", lines);

		} catch (final Throwable secondError) {

			// Use system in case CommonCore#log threw the error
			if (throwable != null) {
				log(CommonCore.configLine());
				log("Got error when saving another error!");
				log("Original error that is not saved:");
				log(CommonCore.configLine());
				throwable.printStackTrace();
			}

			log(CommonCore.configLine());
			log("New error:");
			log(CommonCore.configLine());
			secondError.printStackTrace();
			log(CommonCore.configLine());
		}
	}

	/*
	 * Fill the list with the messages.
	 */
	private static void fill(final List<String> list, final String... messages) {
		list.addAll(Arrays.asList(messages));
	}

	// ----------------------------------------------------------------------------------------------------
	// Utility methods
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Prints array values with their indexes on each line.
	 *
	 * @param values
	 */
	public static void printValues(final Object[] values) {
		if (values != null) {
			log(CommonCore.chatLine());
			log("Enumeration of " + values.length + "x" + values.getClass().getSimpleName().toLowerCase().replace("[]", ""));

			for (int i = 0; i < values.length; i++)
				log("[" + i + "] " + values[i]);
		} else
			log("Value are null");
	}

	/**
	 * Prints stack trace until we reach the native MC/Bukkit with a custom message.
	 *
	 * @param debugLogMessage purely informative message to wrap the thrown stack trace around
	 */
	public static void printStackTrace(final String debugLogMessage) {
		final StackTraceElement[] trace = new Exception().getStackTrace();

		log("!----------------------------------------------------------------------------------------------------------!");
		log(debugLogMessage);
		log("!----------------------------------------------------------------------------------------------------------!");

		for (int i = 1; i < trace.length; i++) {
			final String line = trace[i].toString();

			if (canPrint(line))
				log("\tat " + line);
		}

		log("--------------------------------------------------------------------------------------------------------end-");
	}

	/**
	 * Print the stack trace of a {@link Throwable} to the console.
	 *
	 * <p>This method logs the exception details and its causes in a structured way.</p>
	 *
	 * <p>If the exception has causes (i.e., it wraps other exceptions), those will also be logged.
	 * For custom {@link FoException}s, it skips printing the main exception if it's simply a wrapper
	 * to avoid console spam, and only logs the underlying cause.</p>
	 *
	 * @param throwable The exception or error to print. Its stack trace and the stack trace of any causes will be logged.
	 *
	 * <p><b>Example Usage:</b></p>
	 * <pre>{@code
	 * try {
	 *   // Some code that might throw an error
	 * } catch (Throwable t) {
	 *   printStackTrace(t);
	 * }
	 * }</pre>
	 *
	 * <p>The method first logs the message of the original throwable, then prints the stack trace elements.
	 * If there are additional causes, it logs and prints them as well.</p>
	 */
	public static void printStackTrace(@NonNull final Throwable throwable) {

		if (throwable instanceof HandledException)
			return;

		// Load all causes
		final List<Throwable> causes = new ArrayList<>();

		if (throwable.getCause() != null) {
			Throwable cause = throwable.getCause();

			do
				causes.add(cause);
			while ((cause = cause.getCause()) != null);
		}

		if (throwable instanceof FoException && !causes.isEmpty())
			// Do not print parent exception if we are only wrapping it, saves console spam
			log(throwable.getMessage());

		else {
			log(throwable.toString());

			printStackTraceElements(throwable);
		}

		if (!causes.isEmpty()) {
			final Throwable lastCause = causes.get(causes.size() - 1);

			log(lastCause.toString());
			printStackTraceElements(lastCause);
		}
	}

	/*
	 * Print the stack trace elements of the throwable.
	 */
	private static void printStackTraceElements(final Throwable throwable) {
		for (final StackTraceElement element : throwable.getStackTrace()) {
			final String line = element.toString();

			if (canPrint(line))
				log("\tat " + line);
		}
	}

	/**
	 * Returns whether a line is suitable for printing as an error line.
	 * We ignore stuff from NMS and other spam as this is not needed.
	 *
	 * @param stackTraceLine
	 * @return
	 */
	private static boolean canPrint(final String stackTraceLine) {
		return !stackTraceLine.startsWith("net.minecraft") &&
				!stackTraceLine.startsWith("org.bukkit.") &&
				!stackTraceLine.startsWith("org.github.paperspigot.") &&
				!stackTraceLine.startsWith("java.") &&
				!stackTraceLine.startsWith("javax.script") &&
				!stackTraceLine.startsWith("nashorn") &&
				!stackTraceLine.startsWith("org.yaml.snakeyaml") &&
				!stackTraceLine.startsWith("sun.reflect") &&
				!stackTraceLine.startsWith("sun.misc");

		//!stackTraceLine.contains("org.bukkit.craftbukkit") &&
		//!stackTraceLine.contains("java.lang.Thread.run") &&
		//!stackTraceLine.contains("java.util.concurrent.ThreadPoolExecutor");
	}

	/*
	 * Helper method to log the message.
	 */
	private static void log(final String message) {
		Platform.log(message);
	}
}
