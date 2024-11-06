package org.mineacademy.fo.filter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.FoundationPlayer;

import lombok.Getter;

/**
 * Represents a goddamn filter
 */
@Getter
public abstract class Filter {

	/**
	 * The matcher for filters in command, i.e. identifier:value
	 */
	public static final Pattern FILTER_PATTERN = Pattern.compile("[a-zA-Z]+:[a-zA-Z0-9,_\\-:\\/*\"+]+");

	/**
	 * The registered filters
	 */
	private static final Map<String, Filter> registeredFilters = new HashMap<>();

	/**
	 * The name of the filter
	 */
	private final String identifier;

	/**
	 * Create a new filter
	 *
	 * @param identifier
	 */
	protected Filter(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Return true if the given table supports this filter.
	 * Return true for universal filters.
	 *
	 * @param table
	 * @return
	 */
	public abstract boolean isApplicable(Table table);

	/**
	 * Return a list of usages shown in /protect log tableType ?"
	 *
	 * @return
	 */
	public abstract String[] getUsages();

	/**
	 * Check the value of the filter and return if it is valid.
	 * The value will be stripped off of identifier, i.e. gamemode:creative -> creative
	 *
	 * @param audience
	 * @param value
	 * @return
	 */
	public abstract boolean validate(FoundationPlayer audience, String value);

	/**
	 * Return true if the given row can be displayed when this filter is on.
	 *
	 * @param row
	 * @return
	 */
	public abstract boolean canDisplay(Row row);

	/**
	 * Return the tab complete for the filter, i.e. creative, survival, adventure, spectator for gamemode filter.
	 *
	 * @param audience
	 * @return
	 */
	@Nullable
	public Collection<String> tabComplete(FoundationPlayer audience) {
		return null;
	}

	/**
	 * Return the filter by its name
	 *
	 * @param name
	 * @return
	 */
	@Nullable
	public static Filter getByName(String name) {
		return registeredFilters.get(name);
	}

	/**
	 * Register a new filter
	 *
	 * @param identifier
	 * @param filter
	 */
	public static void register(String identifier, Filter filter) {
		registeredFilters.put(identifier, filter);
	}

	/**
	 * Return all registered filter names
	 *
	 * @return
	 */
	public static Set<String> getFilterNames() {
		return Collections.unmodifiableSet(registeredFilters.keySet());
	}

	/**
	 * Return all registered filters
	 *
	 * @return
	 */
	public static Collection<Filter> getFilters() {
		return Collections.unmodifiableCollection(registeredFilters.values());
	}

	/**
	 * Helper method to parse a date from the given value.
	 *
	 * @param value
	 * @return
	 */
	protected static Date parseDate(String value) {
		value = value.toLowerCase();

		SimpleDateFormat dateFormat = null;
		Date startDate = null;

		if (value.matches("\\d{2}-\\d{2}-\\d{4}"))
			dateFormat = new SimpleDateFormat("dd-MM-yyyy");

		else if (value.matches("\\d{2}-\\d{2}-\\d{4}-\\d{2}:\\d{2}"))
			dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm");

		else if (value.matches("\\d{2}:\\d{2}"))
			dateFormat = new SimpleDateFormat("HH:mm");

		else
			return null;

		dateFormat.setLenient(false);

		try {
			startDate = dateFormat.parse(value);

		} catch (final ParseException e) {
			return null;
		}

		return startDate;
	}
}