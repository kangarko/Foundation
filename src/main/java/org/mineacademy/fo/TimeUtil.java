package org.mineacademy.fo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.mineacademy.fo.exception.FoException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for calculating time from ticks and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtil {

	/**
	 * The date format in dd.MM.yyy HH:mm:ss
	 */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	/**
	 * The date format in dd.MM.yyy HH:mm
	 */
	private static final DateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

	// ------------------------------------------------------------------------------------------------------------
	// Current time
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Seconds elapsed since January the 1st, 1970
	 *
	 * @return System.currentTimeMillis / 1000
	 */
	public static long currentTimeSeconds() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * Ticks elapsed since January the 1st, 1970
	 *
	 * @return System.currentTimeMillis / 50
	 */
	public static long currentTimeTicks() {
		return System.currentTimeMillis() / 50;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Formatting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the current date formatted as DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @return
	 */
	public static String getFormattedDate() {
		return getFormattedDate(System.currentTimeMillis());
	}

	/**
	 * Return the given date in millis formatted as
	 * DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDate(long time) {
		return DATE_FORMAT.format(time);
	}

	/**
	 * Return the current date formatted as DAY.MONTH.YEAR HOUR:MINUTES
	 *
	 * @return
	 */
	public static String getFormattedDateShort() {
		return DATE_FORMAT_SHORT.format(System.currentTimeMillis());
	}

	/**
	 * Return the given date in millis formatted as
	 * DAY.MONTH.YEAR HOUR:MINUTES
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDateShort(long time) {
		return DATE_FORMAT_SHORT.format(time);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the time from a human readable format like "10 minutes"
	 * to seconds.
	 *
	 * @param humanReadableTime the human readable time format: {time} {period}
	 * 		  	   example: 5 seconds, 10 ticks, 7 minutes, 12 hours etc..
	 *
	 * @return the converted human time to seconds
	 */
	public static long toTicks(String humanReadableTime) {
		Valid.checkNotNull(humanReadableTime, "Time is null");

		long seconds = 0L;

		final String[] split = humanReadableTime.split(" ");

		for (int i = 1; i < split.length; i++) {
			final String sub = split[i].toLowerCase();
			int multiplier = 0; // e.g 2 hours = 2
			long unit = 0; // e.g hours = 3600
			boolean isTicks = false;

			try {
				multiplier = Integer.parseInt(split[i - 1]);
			} catch (final NumberFormatException e) {
				continue;
			}

			// attempt to match the unit time
			if (sub.startsWith("tick"))
				isTicks = true;

			else if (sub.startsWith("second"))
				unit = 1;

			else if (sub.startsWith("minute"))
				unit = 60;

			else if (sub.startsWith("hour"))
				unit = 3600;

			else if (sub.startsWith("day"))
				unit = 86400;

			else if (sub.startsWith("week"))
				unit = 604800;

			else if (sub.startsWith("month"))
				unit = 2629743;

			else if (sub.startsWith("year"))
				unit = 31556926;
			else
				throw new FoException("Must define date type! Example: '1 second' (Got '" + sub + "')");

			seconds += multiplier * (isTicks ? 1 : unit * 20);
		}

		return seconds;
	}

	/**
	 * Formats the given time from seconds into the following format:
	 *
	 * "1 hour 50 minutes 10 seconds" or similar, or less
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeGeneric(int seconds) {
		final int second = seconds % 60;
		int minute = seconds / 60;
		String hourMsg = "";

		if (minute >= 60) {
			final int hour = seconds / 60;
			minute %= 60;

			hourMsg = (hour == 1 ? "hour" : "hours") + " ";
		}

		return hourMsg + minute + (minute > 0 ? (minute == 1 ? " minute" : " minutes") + " " : "") + second + (second == 1 ? " second" : " seconds");
	}
}
