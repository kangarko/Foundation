package org.mineacademy.fo.filter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collection;

import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.FoundationPlayer;

public final class FilterDate extends Filter {

	private long millisecondsBack;

	public FilterDate() {
		super("date");
	}

	@Override
	public boolean isApplicable(Table table) {
		return true;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"date:<today/yesterday/week/lweek/month/l(ast)month/year/lyear> - Show results from the given time period.",
				"date:<30m/h/d/w/mo/y> - Show results from 30 minutes/hours/days/weeks/months/years ago.",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return Arrays.asList("today", "yesterday", "week", "lweek", "month", "lmonth", "year", "lyear", "tweek", "thisweek", "tmonth", "thismonth", "tyear", "thisyear", "30m", "1h", "1d", "1w", "1mo", "1y");
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		value = value.toLowerCase();

		final long now = System.currentTimeMillis();
		final long millisecondsBack;
		final ZoneId zone = ZoneId.systemDefault();

		if ("today".equals(value))
			millisecondsBack = now - LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli();

		else if ("yesterday".equals(value))
			millisecondsBack = now - LocalDate.now().minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();

		else if ("lastweek".equals(value) || "lweek".equals(value))
			millisecondsBack = now - LocalDate.now().minusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli();

		else if ("lastmonth".equals(value) || "lmonth".equals(value))
			millisecondsBack = now - LocalDate.now().minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli();

		else if ("lastyear".equals(value) || "lyear".equals(value))
			millisecondsBack = now - LocalDate.now().minusYears(1).atStartOfDay(zone).toInstant().toEpochMilli();

		else if ("tweek".equals(value) || "thisweek".equals(value))
			millisecondsBack = now - LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant().toEpochMilli();

		else if ("tmonth".equals(value) || "thismonth".equals(value))
			millisecondsBack = now - LocalDate.now().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli();

		else if ("tyear".equals(value) || "thisyear".equals(value))
			millisecondsBack = now - LocalDate.now().withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli();

		else
			try {
				millisecondsBack = TimeUtil.toMilliseconds(value);

			} catch (final Exception ex) {
				Messenger.error(audience, ex.getMessage());

				return false;
			}

		this.millisecondsBack = millisecondsBack;

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return row.getDate() >= System.currentTimeMillis() - this.millisecondsBack;
	}
}
