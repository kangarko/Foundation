package org.mineacademy.fo.filter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.RowDate;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.FoundationPlayer;

public final class FilterTo extends Filter {

	private long endDate;

	public FilterTo() {
		super("to");
	}

	@Override
	public boolean isApplicable(final Table table) {
		return RowDate.class.isAssignableFrom(table.getRowClass());
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"to:<dd-mm-yyyy>_<hh-mm> or to:<dd-mm-yyyy> - Show results until the given date range. The date range can also be a unix timestamp.",
				"from:<hh-mm> to:<hh-mm> - Show results for the given time range today.",
		};
	}

	@Override
	public Collection<String> tabComplete(final FoundationPlayer audience) {
		final Date now = new Date();

		return Arrays.asList(
				new SimpleDateFormat("dd-MM-yyyy_HH-mm").format(now),
				new SimpleDateFormat("dd-MM-yyyy").format(now),
				new SimpleDateFormat("HH-mm").format(now));
	}

	@Override
	public boolean validate(final FoundationPlayer audience, final String value) {
		final Date parsed = parseDate(value);

		if (parsed == null) {
			Messenger.error(audience, "Invalid date format. Please use dd-MM-yyyy, dd-MM-yyyy_HH-mm or HH-mm");

			return false;
		}

		this.endDate = parsed.getTime();
		return true;
	}

	@Override
	public boolean canDisplay(final Row row) {
		return ((RowDate) row).getDate() <= this.endDate;
	}
}
