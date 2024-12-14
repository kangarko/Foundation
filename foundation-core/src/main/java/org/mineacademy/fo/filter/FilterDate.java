package org.mineacademy.fo.filter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.RowDate;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.settings.SimpleSettings;

public final class FilterDate extends Filter {

	private long from;
	private long to;

	public FilterDate() {
		super("date");
	}

	@Override
	public boolean isApplicable(final Table table) {
		return RowDate.class.isAssignableFrom(table.getRowClass());
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"date:<today/yesterday/thisweek/lastweek/thismonth/lastmonth/thisyear/lastyear> - Show results from the given time period.",
				"date:<30m/h/d/w/mo/y> - Show results from 30 minutes/hours/days/weeks/months/years ago.",
		};
	}

	@Override
	public Collection<String> tabComplete(final FoundationPlayer audience) {
		return Arrays.asList("today", "yesterday", "thisweek", "lastweek", "thismonth", "lastmonth", "thisyear", "lastyear", "30m", "1h", "1d", "1w", "1mo", "1y");
	}

	@Override
	public boolean validate(final FoundationPlayer audience, String value) {
		value = value.toLowerCase();

		final long now = System.currentTimeMillis();
		final ZoneId zone = SimpleSettings.TIMEZONE;
		final LocalDate localDate = TimeUtil.getCurrentDate();

		ZonedDateTime start = null, end = null;

		if ("today".equals(value)) {
			start = localDate.atStartOfDay(zone);
			end = start.plusDays(1).minusSeconds(1);

		} else if ("yesterday".equals(value)) {
			start = localDate.minusDays(1).atStartOfDay(zone);
			end = start.plusDays(1).minusSeconds(1);

		} else if ("lastweek".equals(value)) {
			start = localDate.minusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay(zone);
			end = start.plusWeeks(1).minusSeconds(1);

		} else if ("lastmonth".equals(value)) {
			start = localDate.minusMonths(1).withDayOfMonth(1).atStartOfDay(zone);
			end = localDate.withDayOfMonth(1).atStartOfDay(zone).minusSeconds(1);

		} else if ("lastyear".equals(value)) {
			start = localDate.minusYears(1).withDayOfYear(1).atStartOfDay(zone);
			end = localDate.withDayOfYear(1).atStartOfDay(zone).minusSeconds(1);

		} else if ("thisweek".equals(value)) {
			start = localDate.with(DayOfWeek.MONDAY).atStartOfDay(zone);
			end = start.plusWeeks(1).minusSeconds(1);

		} else if ("thismonth".equals(value)) {
			start = localDate.withDayOfMonth(1).atStartOfDay(zone);
			end = localDate.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).minusSeconds(1);

		} else if ("thisyear".equals(value)) {
			start = localDate.withDayOfYear(1).atStartOfDay(zone);
			end = localDate.plusYears(1).withDayOfYear(1).atStartOfDay(zone).minusSeconds(1);

		} else
			try {
				this.from = now - TimeUtil.toMilliseconds(value);
				this.to = now;

				return true;

			} catch (final Exception ex) {
				Messenger.error(audience, ex.getMessage());

				return false;
			}

		ValidCore.checkNotNull(start, "Unable to parse date from " + value);
		ValidCore.checkNotNull(end, "Unable to parse date from " + value);

		this.from = start.toInstant().toEpochMilli();
		this.to = end.toInstant().toEpochMilli();

		return true;
	}

	@Override
	public boolean canDisplay(final Row row) {
		final long date = ((RowDate) row).getDate();

		return date >= this.from && date <= this.to;
	}
}
