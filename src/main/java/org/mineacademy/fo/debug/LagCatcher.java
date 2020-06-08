package org.mineacademy.fo.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * A simple yet effective way to calculate duration
 * between two points in code
 */
public final class LagCatcher {

	/**
	 * Stores the name of a code section and the initial time in ms when
	 * it was put there
	 */
	private static final HashMap<String, Long> timings = new HashMap<>();

	private LagCatcher() {
	}

	/**
	 * Puts the code section with the current ms time to the timings map
	 *
	 * @param section
	 */
	public static void start(String section) {
		if (SimpleSettings.LAG_THRESHOLD_MILLIS == -1)
			return;

		timings.put(section, System.nanoTime());
	}

	/**
	 * Stops measuring time in a code section and print a console message
	 * when it took over the threshold set in {@link SimpleSettings}
	 *
	 * @param section
	 */
	public static void end(String section) {
		end(section, false);
	}

	/**
	 * Stops measuring time in a code section and print a console message
	 * when it took over the threshold. Rapid means we always log how long it took,
	 * false means we log it if took over the limit set in {@link SimpleSettings}
	 *
	 * @param section
	 * @param rapid
	 */
	public static void end(String section, boolean rapid) {
		end(section, rapid ? 0 : SimpleSettings.LAG_THRESHOLD_MILLIS);
	}

	/**
	 * Stops measuring time in a code section and print a console message
	 * when it took over the given threshold
	 *
	 * @param section
	 * @param thresholdMs
	 */
	public static void end(String section, int thresholdMs) {
		end(section, thresholdMs, "{section} took {time} ms");
	}

	/**
	 * Stops measuring time in a code section and print a console message.
	 *
	 * @param section
	 * @param message
	 */
	public static void end(String section, String message) {
		final double lag = finishAndCalculate(section);

		message = message.replace("{section}", section);
		message = message.replace("{time}", MathUtil.formatTwoDigits(lag));

		Common.log(message);
	}

	/**
	 * Stops measuring time in a code section and print a console message.
	 *
	 * @param section section to stop
	 */
	public static void endPrint(String section) {
		final double lag = finishAndCalculate(section);

		Common.log(section + " took " + MathUtil.formatTwoDigits(lag));
	}

	/**
	 * Stops measuring time in a code section and print a custom console message
	 * when it took over the given threshold
	 *
	 * Use {section} and {time} to replace the debugged section and how long it took
	 *
	 * @param section
	 * @param thresholdMs
	 * @param message
	 */
	public static void end(String section, int thresholdMs, String message) {
		final double lag = finishAndCalculate(section);

		if (lag > thresholdMs && SimpleSettings.LAG_THRESHOLD_MILLIS != -1) {
			message = message.replace("{section}", section);
			message = message.replace("{time}", MathUtil.formatTwoDigits(lag));

			if (SimplePlugin.hasInstance())
				Common.logNoPrefix("[{plugin_name} {plugin_version}] " + message);
			else
				System.out.println("[LagCatcher] " + message);
		}
	}

	/**
	 * Returns the time in milliseconds how long a measured section section took.
	 *
	 * This does NOT stop the section from being measured.
	 *
	 * @param section
	 * @return the time in 00.000 format, in milliseconds, or 0 if not measured
	 */
	public static double endTook(String section) {
		final Long nanoTime = timings.get(section);

		return calculate(nanoTime);
	}

	/**
	 * Attempts to run the given code the given amount of cycles rapidly
	 * after each other, compounding the lag time together to see how long the
	 * execution takes when it is multiplied
	 *
	 * @param cycles
	 * @param name the lag section name
	 * @param code
	 */
	public static void testPerformance(int cycles, String name, Runnable code) {
		LagCatcher.start(name + "-whole");

		final List<Double> lagMap = new ArrayList<>();

		for (int i = 0; i < cycles; i++) {
			LagCatcher.start(name);
			code.run();
			lagMap.add(finishAndCalculate(name));
		}

		System.out.println("Test '" + name + "' took " + MathUtil.formatTwoDigits(finishAndCalculate(name + "-whole")) + " ms. Average " + MathUtil.average(lagMap) + " ms");
	}

	/**
	 * Calculates how long a section took (in ms) and removes it from the timings map
	 *
	 * @param section
	 * @return
	 */
	public static double finishAndCalculate(String section) {
		final Long nanoTime = timings.remove(section);

		return calculate(nanoTime);
	}

	private static double calculate(Long nanoTime) {
		return nanoTime == null ? 0D : (System.nanoTime() - nanoTime) / 1_000_000D;
	}
}