package org.mineacademy.fo.model;

import lombok.Getter;
import lombok.NonNull;
import org.mineacademy.fo.Valid;

import java.util.*;

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
	 * @param items
	 */
	public Whiteblacklist(@NonNull List<String> items) {
		if (!items.isEmpty()) {
			final String firstLine = items.get(0);
			final String secondLine = items.size() > 1 ? items.get(1) : "";

			boolean entireList = false;
			boolean whitelist = true;

			if ("*".equals(firstLine) || "*".equals(secondLine))
				entireList = true;

			if ("@blacklist".equals(firstLine) || "@blacklist".equals(secondLine))
				whitelist = false;

			final List<String> copyList = new ArrayList<>();

			for (final String oldItem : items)
				if (!"*".equals(oldItem) && !"@blacklist".equals(oldItem))
					copyList.add(oldItem);

			this.items = new HashSet<>(copyList);
			this.whitelist = whitelist;
			this.entireList = entireList;
		}

		else {
			this.items = new HashSet<>();
			this.whitelist = true;
			this.entireList = false;
		}
	}

	/**
	 * Evaluates if the given collection contains at least one match
	 *
	 * @param items
	 * @return
	 */
	public boolean isInList(Collection<String> items) {
		if (this.entireList)
			if (this.whitelist && !items.isEmpty())
				return true;

			else if (!this.whitelist && items.isEmpty())
				return true;

		for (final String item : items)
			if (this.isInList(item))
				return true;

		return false;
	}

	/**
	 * Return true if {@link Valid#isInList(String, Iterable)} returns true
	 * inverting it according to the {@link #isWhitelist()} flag
	 *
	 * @param item
	 * @return
	 */
	public boolean isInList(String item) {
		if (this.entireList)
			return this.whitelist;

		final boolean match = Valid.isInList(item, this.items);

		return this.whitelist ? match : !match;
	}

	/**
	 * Return true if {@link Valid#isInListRegex(String, Iterable)} returns true
	 * inverting it according to the {@link #isWhitelist()} flag
	 *
	 * @param item
	 * @return
	 */
	public boolean isInListRegex(String item) {
		if (this.entireList)
			return this.whitelist;

		final boolean match = Valid.isInListRegex(item, this.items);

		return this.whitelist ? match : !match;
	}

	/**
	 * Return true if {@link Valid#isInListStartsWith(String, Iterable)} returns true
	 * inverting it according to the {@link #isWhitelist()} flag
	 *
	 * @param item
	 * @return
	 */
	public boolean isInListStartsWith(String item) {
		if (this.entireList)
			return this.whitelist;

		final boolean match = Valid.isInListStartsWith(item, this.items);

		return this.whitelist ? match : !match;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "{" + (this.entireList ? "entire list" : this.whitelist ? "whitelist" : "blacklist") + " " + this.items + "}";
	}
}
