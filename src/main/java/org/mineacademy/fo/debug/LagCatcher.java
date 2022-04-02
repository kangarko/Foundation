package org.mineacademy.fo.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple yet effective way to calculate duration
 * between two points in code
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LagCatcher {

	/**
	 * Stores sections with the time time they started to be measured
	 */
	private static volatile Map<String, Long> startTimesMap = new HashMap<>();

	/**
	 * Stores sections with a list of lag durations for each section
	 */
	private static volatile Map<String, List<Long>> durationsMap = new HashMap<>();

	/**
	 * Used to completely disable "X took Y ms" messages from being printed in to your console.
	 *
	 * Defaults to true.
	 */
	@Setter
	@Getter
	private static boolean printingMessages = true;

	/**
	 * Puts the code section with the current ms time to the timings map
	 *
	 * @param section
	 */
	public static void start(String section) {
		if (SimpleSettings.LAG_THRESHOLD_MILLIS == -1)
			return;

		startTimesMap.put(section, System.nanoTime());
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
		end(section, rapid ? 0 : SimpleSettings.LAG_THRESHOLD_MILLIS, "{section} took {time} ms");
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

		if (lag > thresholdMs && SimpleSettings.LAG_THRESHOLD_MILLIS != -1) {
			message = (SimplePlugin.hasInstance() ? "[" + SimplePlugin.getNamed() + " " + SimplePlugin.getVersion() + "] " : "") + message
					.replace("{section}", section)
					.replace("{time}", MathUtil.formatTwoDigits(lag));

			if (printingMessages)
				System.out.println(message);
		}
	}

	/**
	 * Attempts to run the given code the given amount of cycles rapidly
	 * after each other, compounding the lag time together to see how long the
	 * execution takes when it is multiplied
	 *
	 * @param cycles
	 * @param name   the lag section name
	 * @param code
	 */
	public static void performanceTest(int cycles, String name, Runnable code) {
		Valid.checkBoolean(cycles > 0, "Cycles must be above 0");

		LagCatcher.start(name + "-whole");

		final List<Double> lagMap = new ArrayList<>();

		for (int i = 0; i < cycles; i++) {
			LagCatcher.start(name);
			code.run();
			lagMap.add(finishAndCalculate(name));
		}

		System.out.println("Test '" + name + "' took " + MathUtil.formatTwoDigits(finishAndCalculate(name + "-whole")) + " ms. Average " + MathUtil.average(lagMap) + " ms");

		// Measure individual sub sections of the performance test
		if (!durationsMap.isEmpty()) {
			for (final Map.Entry<String, List<Long>> entry : durationsMap.entrySet()) {
				final String section = entry.getKey();
				long duration = 0;

				for (final long sectionDuration : entry.getValue())
					duration += sectionDuration;

				System.out.println("\tSection '" + section + "' took " + MathUtil.formatTwoDigits(duration / 1_000_000D));
			}

			System.out.println("Section measurement ended.");

			durationsMap.clear();
		}
	}

	/**
	 * Work like {@link #start(String)} method except that this will accumulate every time
	 * you call it and show in {@link #performanceTest(int, String, Runnable)}!
	 *
	 * @param section
	 */
	public static void performancePartStart(String section) {
		List<Long> sectionDurations = durationsMap.get(section);

		if (sectionDurations == null) {
			sectionDurations = new ArrayList<>();

			durationsMap.put(section, sectionDurations);
		}

		// Do not calculate duration, just append last time at the end
		sectionDurations.add(System.nanoTime());
	}

	/**
	 * Work like {@link #start(String)} method except that this will accumulate every time
	 * you call it and show in {@link #performanceTest(int, String, Runnable)}!
	 *
	 * This will catch the duration of the last {@link #performancePartStart(String)} call
	 * and put the lag to the map shown after performance test has ended.
	 *
	 * @param section
	 */
	public static void performancePartSnap(String section) {
		Valid.checkBoolean(durationsMap.containsKey(section), "Section " + section + " is not measured! Are you calling it from performanceTest?");

		final List<Long> sectionDurations = durationsMap.get(section);

		final int index = sectionDurations.size() - 1;
		final long nanoTime = sectionDurations.get(index);
		final long duration = System.nanoTime() - nanoTime;

		sectionDurations.set(index, duration);
	}

	/**
	 * Calculates how long a section took (in ms) but does not remove it from the map
	 * it will continue being measure
	 *
	 * @param section
	 */
	public static void took(String section) {
		final Long nanoTime = startTimesMap.get(section);
		final String message = section + " took " + MathUtil.formatTwoDigits(nanoTime == null ? 0D : (System.nanoTime() - nanoTime) / 1_000_000D) + " ms";

		if (printingMessages) {
			if (SimplePlugin.hasInstance())
				Common.logNoPrefix("[{plugin_name} {plugin_version}] " + message);
			else
				System.out.println("[LagCatcher] " + message);
		}
	}

	/**
	 * Calculates how long a section took (in ms) and removes it from the timings map
	 *
	 * @param section
	 * @return
	 */
	private static double finishAndCalculate(String section) {
		final Long nanoTime = startTimesMap.remove(section);

		return nanoTime == null ? 0D : (System.nanoTime() - nanoTime) / 1_000_000D;
	}
}