package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mineacademy.fo.Valid;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a simple way of checking for whitelist or blacklist according
 * to the list, see {@link #Whiteblacklist(List)}
 */
@Getter
public final class Whiteblacklist {

	/**
	 * The list of items
	 */
	private final Set<String> items;

	/**
	 * Used for matching items against an item
	 *
	 * true = except
	 * false = only
	 */
	private final boolean whitelist;

	/**
	 * Special flag if the list is set to ["*"], that will always return true for
	 * everything
	 */
	private final boolean entireList;

	/**
	 * Create a new white black list from the given list
	 *
	 * If the first line equals to '@blacklist', matching will be
	 * blacklisting (only rules), otherwise this will be a whitelist (except rules)
	 */
	public Whiteblacklist(@NonNull List<String> items) {
		if (!items.isEmpty()) {
			final String firstLine = items.get(0);

			// Identify if the first line contains our flags
			this.entireList = firstLine.equals("*");
			this.whitelist = !firstLine.equals("@blacklist") && !entireList;

			final List<String> newItems = new ArrayList<>(items);

			// If yes, remove it from the list
			if (this.entireList || firstLine.equals("@blacklist"))
				newItems.remove(0);

			this.items = new HashSet<>(this.whitelist ? items : newItems);
		}

		else {
			this.items = new HashSet<>();
			this.whitelist = true;
			this.entireList = false;
		}
	}

	/**
	 * Return true if {@link Valid#isInList(String, Iterable)} returns true
	 * inverting it according to the {@link #isWhitelist()} flag
	 *
	 * @param item
	 * @return
	 */
	public boolean isInList(String item) {
		if (entireList)
			return true;

		final boolean match = Valid.isInList(item, this.items);

		return whitelist ? match : !match;
	}

	/**
	 * Return true if {@link Valid#isInListRegex(String, Iterable)} returns true
	 * inverting it according to the {@link #isWhitelist()} flag
	 *
	 * @param item
	 * @return
	 */
	public boolean isInListRegex(String item) {
		if (entireList)
			return true;

		final boolean match = Valid.isInListRegex(item, this.items);

		return whitelist ? match : !match;
	}

	/**
	 * Return true if {@link Valid#isInListContains(String, Iterable)} returns true
	 * inverting it according to the {@link #isWhitelist()} flag
	 *
	 * @param item
	 * @return
	 *
	 * @deprecated can lead to unwanted matches such as when /time is in list, /t will also get caught
	 */
	@Deprecated
	public boolean isInListContains(String item) {
		if (entireList)
			return true;

		final boolean match = Valid.isInListContains(item, this.items);

		return whitelist ? match : !match;
	}

	/**
	 * Return true if {@link Valid#isInListStartsWith(String, Iterable)} returns true
	 * inverting it according to the {@link #isWhitelist()} flag
	 *
	 * @param item
	 * @return
	 */
	public boolean isInListStartsWith(String item) {
		if (entireList)
			return true;

		final boolean match = Valid.isInListStartsWith(item, this.items);

		return whitelist ? match : !match;
	}
}
