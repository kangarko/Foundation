package org.mineacademy.fo.debug;

import java.util.HashMap;
import java.util.Map;

import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A simple class to help calculate duration between code points.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LagCatcher {

	/**
	 * Stores sections with the time time they started to be measured
	 */
	private static final Map<String, Long> startTimesMap = new HashMap<>();

	/**
	 * Puts the code section with the current ms time to the timings map
	 *
	 * @param section
	 */
	public static void start(String section) {
		if (SimpleSettings.LAG_THRESHOLD_MILLIS != -1)
			startTimesMap.put(section, System.nanoTime());
	}

	/**
	 * Stops measuring time in a code section and print a console message
	 * when it took over the threshold set in {@link SimpleSettings}
	 *
	 * @param section
	 */
	public static void end(String section) {
		end(section, SimpleSettings.LAG_THRESHOLD_MILLIS);
	}

	/**
	 * Stops measuring time in a code section and print a console message
	 * when it took over the given threshold. Rapid means we always log how long it took,
	 * false means we log it if took over the limit set in {@link SimpleSettings}
	 *
	 * @param section
	 * @param thresholdMs
	 */
	public static void end(String section, int thresholdMs) {
		end(section, thresholdMs, "{section} took {time} ms");
	}

	/**
	 * Stops measuring time in a code section and print a custom console message
	 * when it took over the given threshold
	 * <p>
	 * Use {section} and {time} to replace the debugged section and how long it took
	 *
	 * @param section
	 * @param thresholdMs
	 * @param message
	 */
	public static void end(String section, int thresholdMs, String message) {
		final double lag = finishAndCalculate(section);

		if (lag > thresholdMs && thresholdMs != -1) {
			message = "[" + Platform.getPlugin().getName() + " " + Platform.getPlugin().getVersion() + "] " + message.replace("{section}", section).replace("{time}", MathUtil.formatTwoDigits(lag));

			System.out.println(message);
		}
	}

	/**
	 * A very manual method to log how long a section took in ms
	 *
	 * @param startNano the start time in nanoseconds, put System.nanoTime() on top of your code you want to measure
	 * @param section
	 */
	public static void took(long startNano, String section) {
		System.out.println(section + " took: " + (System.nanoTime() - startNano) / 1_000_000D + "ms");
	}

	/*
	 * Calculates how long a section took (in ms) and removes it from the timings map
	 */
	private static double finishAndCalculate(String section) {
		final Long nanoTime = startTimesMap.remove(section);

		return nanoTime == null ? 0D : (System.nanoTime() - nanoTime) / 1_000_000D;
	}
}