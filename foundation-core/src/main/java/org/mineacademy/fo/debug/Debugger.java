package org.mineacademy.fo.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleSettings;

import io.sentry.Sentry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for solving problems and errors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Debugger {

	/**
	 * Logs a message to the console if the section name is within {@link SimpleSettings#DEBUG_SECTIONS}
	 * or if it is "*".
	 *
	 * Debug sections are by default stored in settings.yml under the "Debug" key.
	 *
	 * @param section
	 * @param messages
	 */
	public static void debug(String section, String... messages) {
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
	public static boolean isDebugged(String section) {
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
	public static void saveError(Throwable throwable, String... messages) {

		// Log to sentry if enabled.
		final FoundationPlugin plugin = Platform.getPlugin();

		if (plugin.getSentryDsn() != null && SimpleSettings.SENTRY) {
			final Throwable finalThrowable = throwable;

			plugin.loadLibrary("io.sentry", "sentry", "8.0.0-alpha.4");

			// Need to address the bug where a globally included sentry has the DSN of the first plugin
			Sentry.init(options -> {
				options.setDsn(plugin.getSentryDsn());
				options.setTracesSampleRate(0.0);

				// Add plugin name and version to Sentry context
				options.setBeforeSend((event, hint) -> {
					event.setRelease(plugin.getVersion());
					event.setServerName(null);
					event.setDist(Platform.getPlatformVersion());
					event.setTag("plugin_name", plugin.getName());
					event.setTag("plugin_version", plugin.getVersion());
					event.setTag("server_version", Platform.getPlatformVersion());
					event.setTag("server_distro", Platform.getPlatformName());

					if ("%%__BUILTBYBIT__%%".equals("true")) {
						event.setTag("bbb_user_id", "%%__USER__%%");
						event.setTag("bbb_user_name", "%%__USERNAME__%%");
						event.setTag("bbb_user_name", "%%__USERNAME__%%");
						event.setTag("bbb_nonce", "%%__NONCE__%%");
					}

					return event;
				});
			});

			Platform.runTaskAsync(() -> Sentry.captureException(finalThrowable));
		}

		// Else, only log locally.
		else {
			final String systemInfo = "Running " + Platform.getPlatformName() + " " + Platform.getPlatformVersion() + " and Java " + System.getProperty("java.version");

			try {
				final List<String> lines = new ArrayList<>();
				final String header = Platform.getPlugin().getName() + " " + Platform.getPlugin().getVersion() + " encountered " + throwable.getClass().getSimpleName();

				// Write out header and server info
				fill(lines,
						"------------------------------------[ " + TimeUtil.getFormattedDate() + " ]-----------------------------------",
						header,
						systemInfo,
						"Plugins: " + CommonCore.join(Platform.getServerPlugins()),
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
				CommonCore.log(header + "! Please check your error.log and report this issue with the information in that file. " + systemInfo);

				// Finally, save the error file
				FileUtil.write("error.log", lines);

			} catch (final Throwable secondError) {

				// Use system in case CommonCore#log threw the error
				log("Got error when saving another error! Saving error:" + secondError);
				log("Original error that is not saved:");

				throwable.printStackTrace();
			}
		}
	}

	/*
	 * Fill the list with the messages.
	 */
	private static void fill(List<String> list, String... messages) {
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
	public static void printValues(Object[] values) {
		if (values != null) {
			log(CommonCore.chatLine());
			log("Enumeration of " + values.length + "x" + values.getClass().getSimpleName().toLowerCase().replace("[]", ""));

			for (int i = 0; i < values.length; i++)
				log("&8[" + i + "] &7" + values[i]);
		} else
			log("Value are null");
	}

	/**
	 * Prints stack trace until we reach the native MC/Bukkit with a custom message.
	 *
	 * @param debugLogMessage purely informative message to wrap the thrown stack trace around
	 */
	public static void printStackTrace(String debugLogMessage) {
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
	public static void printStackTrace(@NonNull Throwable throwable) {

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
	private static void printStackTraceElements(Throwable throwable) {
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
	 * @param message
	 * @return
	 */
	private static boolean canPrint(String message) {
		return !message.contains("net.minecraft") &&
				!message.contains("org.bukkit.craftbukkit") &&
				!message.contains("org.github.paperspigot.ServerScheduler") &&
				!message.contains("nashorn") &&
				!message.contains("javax.script") &&
				!message.contains("org.yaml.snakeyaml") &&
				!message.contains("sun.reflect") &&
				!message.contains("sun.misc") &&
				!message.contains("java.lang.Thread.run") &&
				!message.contains("java.util.concurrent.ThreadPoolExecutor");
	}

	/*
	 * Helper method to log the message.
	 */
	private static void log(String message) {
		System.out.println(message);
	}
}
