package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a simple way of checking for whitelist or blacklist according
 * to the list, see {@link #Whiteblacklist(List)}
 */
public final class Whiteblacklist {

	/**
	 * The list of items
	 */
	@Getter
	private final Set<String> items;

	/**
	 * The list of items in precompiled pattern formats
	 */
	private final Set<Pattern> patterns;

	/**
	 * Were patterns compiled?
	 */
	private final boolean compileAsPatterns;

	/**
	 * Used for matching items against an item
	 *
	 * true = except
	 * false = only
	 */
	@Getter
	private final boolean whitelist;

	/**
	 * Special flag if the list is set to ["*"], that will always return true for
	 * everything
	 */
	@Getter
	private final boolean entireList;

	/**
	 * Create a new white black list from the given list
	 *
	 * If the first line equals to '@blacklist', matching will be
	 * blacklisting (only rules), otherwise this will be a whitelist (except rules)
	 *
	 * @param items
	 */
	public Whiteblacklist(@NonNull List<String> items) {
		this(items, false);
	}

	/**
	 * Create a new white black list from the given list
	 *
	 * If the first line equals to '@blacklist', matching will be
	 * blacklisting (only rules), otherwise this will be a whitelist (except rules)
	 *
	 * @param items
	 * @param compileAsPatterns shall we precompile the list for maximum performance?
	 */
	public Whiteblacklist(@NonNull List<String> items, boolean compileAsPatterns) {
		this.patterns = new HashSet<>();
		this.compileAsPatterns = compileAsPatterns;

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

			for (final String item : items)
				if (!"*".equals(item) && !"@blacklist".equals(item))
					copyList.add(item);

			this.items = new HashSet<>(copyList);
			this.whitelist = whitelist;
			this.entireList = entireList;
			this.patterns.clear();

			if (compileAsPatterns)
				for (String item : this.items)
					this.patterns.add(Common.compilePattern(item));
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

		final boolean match = this.compileAsPatterns ? Valid.isInListRegexFast(item, this.patterns) : Valid.isInListRegex(item, this.items);

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
