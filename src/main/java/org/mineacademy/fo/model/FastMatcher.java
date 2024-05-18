package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An extremely limited and simple matcher modeled off of regex, intentionally culled down for
 * maximum performance and efficiency.
 *
 * It matches items in four different modes:
 * 1. Start your pattern with * and we will evaluate if the message starts with it,
 * 2. End it with * and we'll evaluate endings,
 * 3. Start with " and end with " to evaluate equal
 * 4. Otherwise we evaluate if message contains the pattern
 *
 * You can use | to separate multiple matches i.e. DIAMOND_*|GOLDEN_* etc
 * You can use '*'  to match everything
 *
 * If you still want/need to use regex you can prefix your message with "* " and then match
 * normally, i.e. "* ^DIAMOND_(SWORD|HOE)"
 *
 * Example: DIAMOND_* will match all DIAMOND_HOE, DIAMOND_SPADE etc. but not SUPERDIAMOND_SPADE
 *
 * Rationale: The Protect plugin evaluates each slot in the inventory (27 + armor) against all rules,
 * and using the complex regex class drags performance down too much.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FastMatcher implements ConfigSerializable {

	/**
	 * The full regex class
	 */
	private final Pattern pattern;

	/**
	 * The raw pattern
	 */
	private final String rawPattern;

	/**
	 * The list of matcher, null if pattern is *
	 */
	@Nullable
	private final Matcher[] matchers;

	/**
	 * Return if this matcher matches the given message,
	 * case sensitive
	 *
	 * @param message
	 * @return
	 */
	public boolean find(String message) {

		// Indicates we match everything
		if (this.matchers == null)
			return true;

		if (message.isEmpty())
			return false;

		// Indicate regex is used
		if (this.pattern != null)
			return this.pattern.matcher(message).find();

		// Use our matching
		for (final Matcher matcher : this.matchers) {
			Valid.checkNotEmpty(matcher.getPattern(), "Matcher pattern cannot be empty! Use * instead to match everything in " + this);

			if (matcher.find(message))
				return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return "FastMatcher{pattern=" + rawPattern + "}";
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray("Pattern", this.rawPattern);
	}

	/**
	 * Deserialize the matcher
	 *
	 * @param map
	 * @return
	 */
	public static FastMatcher deserialize(SerializedMap map) {
		return compile(map.getString("Pattern"));
	}

	/**
	 * Compiles a matcher from the given pattern
	 *
	 * @param pattern
	 * @return
	 */
	public static FastMatcher compile(String pattern) {

		if ("*".equals(pattern))
			return new FastMatcher(null, pattern, null);

		else if (pattern.startsWith("* "))
			return new FastMatcher(Pattern.compile(pattern.substring(2)), pattern, null);

		final List<Matcher> matchers = new ArrayList<>();

		for (final String part : pattern.split("\\|"))
			matchers.add(Matcher.compile(part));

		return new FastMatcher(null, pattern, matchers.toArray(new Matcher[matchers.size()]));
	}

	/**
	 * Compile a list of matchers from the given list
	 *
	 * @param list
	 * @return
	 */
	public static List<FastMatcher> compileFromList(List<String> list) {
		final List<FastMatcher> matchers = new ArrayList<>();

		for (final String pattern : list)
			matchers.add(compile(pattern));

		return matchers;
	}
}

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class Matcher {

	/**
	 * The pattern, adjusted for mode, see above
	 */
	private final String pattern;

	/**
	 * The matching mode
	 */
	private final int mode;

	/**
	 * Return true if the pattern matches the given message
	 * @param message
	 * @return
	 */
	public boolean find(String message) {
		if (this.mode == 1)
			return message.startsWith(this.pattern);

		else if (this.mode == 2)
			return message.endsWith(this.pattern);

		else if (this.mode == 3)
			return message.equals(this.pattern);

		else
			return message.contains(this.pattern);

	}

	/**
	 * Compiles a matcher from the given pattern, case sensitive.
	 *
	 * @param pattern
	 * @return
	 */
	public static Matcher compile(String pattern) {
		int mode = 4;

		if (pattern.startsWith("*")) {
			mode = 1;
			pattern = pattern.substring(1);

		} else if (pattern.endsWith("*")) {
			mode = 2;
			pattern = pattern.substring(0, pattern.length() - 1);

		} else if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
			mode = 3;
			pattern = pattern.substring(1, pattern.length() - 1);
		}

		return new Matcher(pattern, mode);
	}
}