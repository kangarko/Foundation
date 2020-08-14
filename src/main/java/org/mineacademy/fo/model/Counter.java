package org.mineacademy.fo.model;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * Special usage class used in CoreArena/Boss to count loaded arenas
 * vs. the ones that actually load correctly
 *
 * @deprecated subject for removal
 */
@Deprecated
public final class Counter {

	/**
	 * The total files present in a folder
	 */
	private final int total;

	/**
	 * The limit of messages so we dont flood the console
	 * if the user has e.g. 150 arenas
	 */
	private final int limit;

	/**
	 * The type of object we are speaking about, e.g. "arena"
	 */
	protected final String type;

	/**
	 * The internal increasing counter
	 */
	private int count = 0;

	/**
	 * Create a new counter of the given type for the amount of total files in a folder
	 * with a limit and announces the start
	 *
	 * @param total
	 * @param limit
	 * @param type
	 */
	public Counter(int total, int limit, String type) {
		this.total = total;
		this.limit = limit;
		this.type = type;

		announceStart();
	}

	/**
	 * Logs the "loading" console message
	 */
	private void announceStart() {
		Common.log(" ");
		Common.log("&8Loading " + type + (type.endsWith("ss") ? "e" : "") + "s" + " (" + total + ") ..");
	}

	/**
	 * Logs the console "loading X" message if not over limit
	 *
	 * @param what the name of the object, e.g. the name of the arena
	 */
	public void count(String what) {
		if (count++ < limit)
			Common.log(" &7> Loading &f" + what);

		else if (count == limit + 1)
			Common.log(" and " + (total - count + 1) + " more...");
	}

	/**
	 * Logs the "data missing" console message for invalid objects
	 *
	 * @param what the name of the object, e.g. the name of the arena
	 */
	public void invalid(String what) {
		Common.log("  &7- &c" + SimpleLocalization.DATA_MISSING.replace("{name}", what).replace("{type}", type));
	}
}