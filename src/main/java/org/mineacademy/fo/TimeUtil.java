package org.mineacademy.fo;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.settings.SimpleLocalization.Cases;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for calculating time from ticks and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtil {

	/**
	 * The pattern recognizing 1d1h1s type of dates
	 */
	private static final Pattern TOKEN_PATTERN = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?"
			// Months
			+ "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?"

			// Weeks
			+ "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?"

			// Days
			+ "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?"

			// Hours
			+ "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?"

			// Minutes
			+ "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?"

			// Seconds (the "s" may be left out)
			+ "(?:([0-9]+)\\s*(?:s[a-z]*)?)?",

			Pattern.CASE_INSENSITIVE);

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
	 * Return the current time formatted as per {@link SimpleSettings#DATE_FORMAT}
	 * Defaults to DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @return
	 */
	public static String getFormattedDate() {
		return getFormattedDate(System.currentTimeMillis());
	}

	/**
	 * Return the given timestamp in millis formatted as per {@link SimpleSettings#DATE_FORMAT}
	 * Defaults to DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDate(final long time) {
		return SimpleSettings.DATE_FORMAT.format(time);
	}

	/**
	 * Return the current time formatted as per {@link SimpleSettings#DATE_FORMAT_SHORT}
	 * Defaults to DAY.MONTH.YEAR HOUR:MINUTES
	 *
	 * @return
	 */
	public static String getFormattedDateShort() {
		return getFormattedDateShort(System.currentTimeMillis());
	}

	/**
	 * Return the given date in millis formatted as per {@link SimpleSettings#DATE_FORMAT_SHORT}
	 * Defaults to DAY.MONTH.YEAR HOUR:MINUTES
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDateShort(final long time) {
		return SimpleSettings.DATE_FORMAT_SHORT.format(time);
	}

	/**
	 * Return the current time formatted as per {@link SimpleSettings#DATE_FORMAT_MONTH}
	 * Defaults to DAY.MONTH HOUR:MINUTES
	 *
	 * @return
	 */
	public static String getFormattedDateMonth() {
		return getFormattedDateMonth(System.currentTimeMillis());
	}

	/**
	 * Return the given date in millis formatted as per {@link SimpleSettings#DATE_FORMAT_MONTH}
	 * Defaults to DAY.MONTH HOUR:MINUTES
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDateMonth(final long time) {
		return SimpleSettings.DATE_FORMAT_MONTH.format(time);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the time from a human readable format like "10 minutes"
	 * to seconds.
	 *
	 * @param humanReadableTime the human readable time format: {time} {period}
	 *                          example: 5 seconds, 10 ticks, 7 minutes, 12 hours etc..
	 * @return the converted human time to seconds
	 */
	public static long toTicks(final String humanReadableTime) {
		Valid.checkNotNull(humanReadableTime, "Time is null");

		long seconds = 0L;

		final String[] split = humanReadableTime.split(" ");

		if (split.length < 2)
			throw new IllegalArgumentException("Expected human readable time like '1 second', got '" + humanReadableTime + "' instead");

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

			else if (sub.startsWith("potato"))
				unit = 1337;

			else
				throw new IllegalArgumentException("Must define date type! Example: '1 second' (Got '" + sub + "')");

			seconds += multiplier * (isTicks ? 1 : unit * 20);
		}

		return seconds;
	}

	/**
	 * Formats the given time from seconds into the following format:
	 * <p>
	 * "1 hour 50 minutes 10 seconds" or similar, or less
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeGeneric(final long seconds) {
		final long second = seconds % 60;
		long minute = seconds / 60;
		String hourMsg = "";

		if (minute >= 60) {
			final long hour = seconds / 60 / 60;
			minute %= 60;

			hourMsg = Cases.HOUR.formatWithCount(hour) + " ";
		}

		return hourMsg + (minute != 0 ? Cases.MINUTE.formatWithCount(minute) + " " : "") + Cases.SECOND.formatWithCount(second);
	}

	/**
	 * Format time in "X days Y hours Z minutes Å seconds"
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeDays(final long seconds) {
		final long minutes = seconds / 60;
		final long hours = minutes / 60;
		final long days = hours / 24;

		return (days != 0 ? Cases.DAY.formatWithCount(days) + " " : "")
				+ (hours % 24 != 0 ? Cases.HOUR.formatWithCount(hours % 24) + " " : "")
				+ (minutes % 60 != 0 ? Cases.MINUTE.formatWithCount(minutes % 60) + " " : "")
				+ Cases.SECOND.formatWithCount(seconds % 60);
	}

	/**
	 * Format the time in seconds, for example: 10d 5h 10m 20s or just 5m 10s
	 * If the seconds are zero, an output 0s is given
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeShort(long seconds) {
		long minutes = seconds / 60;
		long hours = minutes / 60;
		final long days = hours / 24;

		hours = hours % 24;
		minutes = minutes % 60;
		seconds = seconds % 60;

		return (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "m " : "") + seconds + "s";
	}

	/**
	 * Format the time in seconds delimited by colon, for example: 01:20:00 for 1 hour 20 minutes
	 * If the seconds are zero, an output 00:00 is given
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeColon(long seconds) {
		long minutes = seconds / 60;
		long hours = minutes / 60;
		final long days = hours / 24;

		hours = hours % 24;
		minutes = minutes % 60;
		seconds = seconds % 60;

		return (days > 0 ? (days < 10 ? "0" : "") + days + ":" : "") +
				(hours > 0 ? (hours < 10 ? "0" : "") + hours + ":" : "") +
				(minutes > 0 ? (minutes < 10 ? "0" : "") + minutes + ":" : "00:") +
				(seconds < 10 ? "0" : "") + seconds;
	}

	/**
	 * Convert the given string token into milliseconds
	 *
	 * Example: 1y, 1mo, 1w, 1d, 1h, 1m, 1s and these can all be combined together
	 *
	 * @param text
	 * @return
	 */
	public static long toMilliseconds(final String text) {
		final Matcher matcher = TOKEN_PATTERN.matcher(text);

		long years = 0, months = 0, weeks = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
		boolean found = false;

		while (matcher.find()) {

			if (matcher.group() == null || matcher.group().isEmpty())
				continue;

			for (int i = 0; i < matcher.groupCount(); i++)
				if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
					found = true;

					break;
				}

			if (found) {
				for (int i = 1; i < 8; i++)
					if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
						final long output = Long.parseLong(matcher.group(i));

						if (i == 1) {
							checkLimit("years", output, 10);

							years = output;
						}

						else if (i == 2) {
							checkLimit("months", output, 12 * 100);

							months = output;
						}

						else if (i == 3) {
							checkLimit("weeks", output, 4 * 100);

							weeks = output;
						}

						else if (i == 4) {
							checkLimit("days", output, 31 * 100);

							days = output;
						}

						else if (i == 5) {
							checkLimit("hours", output, 24 * 100);

							hours = output;
						}

						else if (i == 6) {
							checkLimit("minutes", output, 60 * 100);

							minutes = output;
						}

						else if (i == 7) {
							checkLimit("seconds", output, 60 * 100);

							seconds = output;
						}
					}

				break;
			}
		}

		if (!found)
			throw new NumberFormatException("Date not found from: " + text);

		return (seconds + (minutes * 60) + (hours * 3600) + (days * 86400) + (weeks * 7 * 86400) + (months * 30 * 86400) + (years * 365 * 86400)) * 1000;
	}

	/*
	 * Check value over limit
	 */
	private static void checkLimit(final String type, final long value, final int maxLimit) {
		if (value > maxLimit)
			throw new IllegalArgumentException("Value type " + type + " is out of bounds! Max limit: " + maxLimit + ", given: " + value);
	}

	/**
	 * Convert the current time into one that is recognized by MySQL
	 *
	 * @return
	 */
	public static String toSQLTimestamp() {
		return toSQLTimestamp(System.currentTimeMillis());
	}

	/**
	 * Convert the given long timestamp into one that is recognized by MySQL
	 *
	 * @param timestamp
	 * @return
	 */
	public static String toSQLTimestamp(final long timestamp) {
		final java.util.Date date = new Date(timestamp);

		return new Timestamp(date.getTime()).toString();
	}

	/**
	 * Convert the given MySQL timestamp into a long
	 *
	 * @param timestamp
	 * @return
	 */
	public static long fromSQLTimestamp(final String timestamp) {
		return Timestamp.valueOf(timestamp).getTime();
	}

	/**
	 * Checks if the current time is within a specified timeframe.
	 *
	 * @param time The time string to be checked. It should be in the format 'dd MMM yyyy, HH:mm' and can contain variables such as {year}, {month}, {day}, {hour}, {minute}, and {second} for the current time periods.
	 * @param future If true, the method checks if the current time is before the specified time. If false, it checks if the current time is after the specified time.
	 * @return Returns true if the current time is within the specified timeframe, otherwise returns false.
	 *
	 * Throws ParseException If the time string is not in the correct format or contains invalid variables, a ParseException is thrown.
	 *
	 * The method works as follows:
	 * 1. It gets the current time using Calendar.getInstance().
	 * 2. It replaces short month names (Jan, Feb, etc.) in the time string with full month names (January, February, etc.).
	 * 3. It replaces variables in the time string ({year}, {month}, etc.) with their current values.
	 * 4. It parses the time string into a timestamp.
	 * 5. If the 'future' parameter is true, it checks if the current time is before the timestamp. If the 'future' parameter is false, it checks if the current time is after the timestamp.
	 * 6. If the current time is not within the specified timeframe, it returns false. Otherwise, it returns true.
	 */
	public static boolean isInTimeframe(@NonNull String time, boolean future) {
		final Calendar calendar = Calendar.getInstance();
		final String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
		final String[] fullNameMonths = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

		for (int i = 0; i < months.length; i++)
			time = time.replaceAll(months[i] + "\\b", fullNameMonths[i]);

		time = Replacer.replaceArray(time,
				"year", calendar.get(Calendar.YEAR),
				"month", fullNameMonths[calendar.get(Calendar.MONTH)],
				"day", calendar.get(Calendar.DAY_OF_MONTH),
				"hour", calendar.get(Calendar.HOUR_OF_DAY),
				"minute", calendar.get(Calendar.MINUTE),
				"second", calendar.get(Calendar.SECOND));

		try {
			final long timestamp = new SimpleDateFormat("dd MMM yyyy, HH:mm").parse(time).getTime();

			if (future) {
				if (System.currentTimeMillis() < timestamp)
					return false;

			} else {
				if (System.currentTimeMillis() > timestamp)
					return false;
			}

		} catch (final ParseException ex) {
			Common.throwError(ex,
					"Syntax error in time operator.",
					"Valid: 'dd MMM yyyy, HH:mm' with {year/month/day/hour/minute/second} variables.",
					"Got: " + time);
		}

		return true;
	}
}
