package org.mineacademy.fo.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for solving problems and errors
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Debugger {

	/**
	 * Stores messages to be printed out at once at the end,
	 * the key is the debug section and the list contains messages that will be connected
	 * and printed.
	 */
	private static final Map<String, ArrayList<String>> pendingMessages = new HashMap<>();

	/**
	 * Prints a debug messages to the console if the given section is being debugged
	 * <p>
	 * You can set if the section is debugged by setting it in "Debug" key in your settings.yml,
	 * by default your class extending {@link SimpleSettings}
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
	 * Puts a message for the specific section into the queue. These are stored there until
	 * you call {@link #push(String)} and then put together and printed.
	 *
	 * @param section
	 * @param message
	 */
	public static void put(String section, String message) {
		if (!isDebugged(section))
			return;

		final ArrayList<String> list = pendingMessages.getOrDefault(section, new ArrayList<>());
		list.add(message);

		pendingMessages.put(section, list);
	}

	/**
	 * Puts the message at the end of the pending message queue and pushes the final log
	 * to the console
	 *
	 * @param section
	 * @param message
	 */
	public static void push(String section, String message) {
		put(section, message);
		push(section);
	}

	/**
	 * Clears all pending messages from {@link #put(String, String)}, puts them together
	 * and prints them into your console
	 *
	 * @param section
	 */
	public static void push(String section) {
		if (!isDebugged(section))
			return;

		final List<String> parts = pendingMessages.remove(section);

		if (parts == null)
			return;

		final String whole = String.join("", parts);

		for (final String message : whole.split("\n"))
			debug(section, message);
	}

	/**
	 * Get if the given section is being debugged
	 * <p>
	 * You can set if the section is debugged by setting it in "Debug" key in your settings.yml,
	 * by default your class extending {@link SimpleSettings}
	 * <p>
	 * If you set Debug to ["*"] this will always return true
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
	 * Logs the error in the console and writes all details into the errors.log file
	 *
	 * @param t
	 * @param messages
	 */
	public static void saveError(Throwable t, String... messages) {

		final String systemInfo = "Running " + Platform.getServerName() + " " + Platform.getServerVersion() + " and Java " + System.getProperty("java.version");

		try {
			final List<String> lines = new ArrayList<>();
			final String header = Platform.getPlugin().getName() + " " + Platform.getPlugin().getVersion() + " encountered " + CommonCore.article(t.getClass().getSimpleName());

			// Write out header and server info
			fill(lines,
					"------------------------------------[ " + TimeUtil.getFormattedDate() + " ]-----------------------------------",
					header,
					systemInfo,
					"Plugins: " + CommonCore.join(Platform.getServerPlugins(), ", "),
					"----------------------------------------------------------------------------------------------");

			// Write additional data
			if (messages != null && !String.join("", messages).isEmpty()) {
				fill(lines, "\nMore Information: ");
				fill(lines, messages);
			}

			{ // Write the stack trace

				do {
					// Write the error header
					fill(lines, t == null ? "Unknown error" : t.getClass().getSimpleName() + " " + CommonCore.getOrDefault(t.getMessage(), CommonCore.getOrDefault(t.getLocalizedMessage(), "(Unknown cause)")));

					int count = 0;

					for (final StackTraceElement el : t.getStackTrace()) {
						count++;

						final String trace = el.toString();

						if (trace.contains("sun.reflect"))
							continue;

						if (count > 6 && trace.startsWith("net.minecraft.server"))
							break;

						fill(lines, "\t at " + el.toString());
					}
				} while ((t = t.getCause()) != null);
			}

			fill(lines, "----------------------------------------------------------------------------------------------", System.lineSeparator());

			// Log to the console
			CommonCore.log(header + "! Please check your error.log and report this issue with the information in that file. " + systemInfo);

			// Finally, save the error file
			FileUtil.write("error.log", lines);

		} catch (final Throwable secondError) {

			// Use system in case CommonCore#log threw the error
			System.out.println("Got error when saving another error! Saving error:" + secondError);
			System.out.println("Original error that is not saved:");

			t.printStackTrace();
		}
	}

	private static void fill(List<String> list, String... messages) {
		list.addAll(Arrays.asList(messages));
	}

	// ----------------------------------------------------------------------------------------------------
	// Utility methods
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Print out where your method is being called from
	 * Such as: YourClass > YourMainClass > MinecraftServer > Thread
	 * <p>
	 * Also can print line numbers YourClass#LineNumber
	 *
	 * @param trackLineNumbers
	 * @return
	 */
	public static List<String> traceRoute(boolean trackLineNumbers) {
		final Exception exception = new RuntimeException("I love horses");
		final List<String> paths = new ArrayList<>();

		for (final StackTraceElement el : exception.getStackTrace()) {
			final String[] classNames = el.getClassName().split("\\.");
			final String className = classNames[classNames.length - 1];
			final String line = el.toString();

			if (line.contains("net.minecraft.server") || line.contains("org.bukkit.craftbukkit"))
				break;

			if (line.contains("org.bukkit.plugin.java.JavaPluginLoader") || line.contains("org.bukkit.plugin.SimplePluginManager") || line.contains("org.bukkit.plugin.JavaPlugin"))
				continue;

			if (!paths.contains(className))
				paths.add(className + "#" + el.getMethodName() + (trackLineNumbers ? "(" + el.getLineNumber() + ")" : ""));
		}

		// Remove call to self
		if (!paths.isEmpty())
			paths.remove(0);

		return paths;
	}

	/**
	 * Prints array values with their indexes on each line
	 *
	 * @param values
	 */
	public static void printValues(Object[] values) {
		if (values != null) {
			log(CommonCore.consoleLine());
			log("Enumeration of " + CommonCore.plural(values.length, values.getClass().getSimpleName().toLowerCase().replace("[]", "")));

			for (int i = 0; i < values.length; i++)
				log("&8[" + i + "] &7" + values[i]);
		} else
			log("Value are null");
	}

	/**
	 * Prints stack trace until we reach the native MC/Bukkit with a custom message
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
	 * Prints a Throwable's first line and stack traces.
	 * <p>
	 * Ignores the native Bukkit/Minecraft server.
	 *
	 * @param throwable the throwable to print
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

	private static void printStackTraceElements(Throwable throwable) {
		for (final StackTraceElement element : throwable.getStackTrace()) {
			final String line = element.toString();

			if (canPrint(line))
				log("\tat " + line);
		}
	}

	/**
	 * Returns whether a line is suitable for printing as an error line - we ignore stuff from NMS and other spam as this is not needed
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

	private static void log(String message) {
		System.out.println(message);
	}
}
