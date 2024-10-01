package org.mineacademy.fo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for checking conditions and throwing our exception that is logged into error.log.
 *
 * This is a platform-neutral class, which is extended by "Valid" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ValidCore {

	/**
	 * A pattern for valid integers.
	 */
	private static final Pattern PATTERN_INTEGER = Pattern.compile("-?\\d+");

	/**
	 * A pattern for valid whole numbers.
	 */
	private static final Pattern PATTERN_DECIMAL = Pattern.compile("([0-9]+\\.?[0-9]*|\\.[0-9]+)");

	// ------------------------------------------------------------------------------------------------------------
	// Checking for validity and throwing errors if false or null
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Throw a {@link FoException} if the given object is null.
	 *
	 * @param toCheck
	 */
	public static void checkNotNull(final Object toCheck) {
		if (toCheck == null)
			throw new FoException();
	}

	/**
	 * Throw a {@link FoException} with a custom message if the given object is null.
	 *
	 * @param toCheck
	 * @param falseMessage
	 */
	public static void checkNotNull(final Object toCheck, final String falseMessage) {
		if (toCheck == null)
			throw new FoException(falseMessage);
	}

	/**
	 * Throw a {@link FoException} with a custom message if the given expression is false.
	 *
	 * @param expression
	 * @param falseMessage
	 */
	public static void checkBoolean(final boolean expression, final String falseMessage) {
		if (!expression)
			throw new FoException(falseMessage);
	}

	/**
	 * Throw a {@link FoException} with a custom message if the given toCheck string is not a number!
	 *
	 * @param toCheck
	 * @param falseMessage
	 */
	public static void checkInteger(final String toCheck, final String falseMessage) {
		if (!ValidCore.isInteger(toCheck))
			throw new FoException(falseMessage);
	}

	/**
	 * Throw a {@link FoException} with a custom message if the given collection is null or empty.
	 *
	 * @param collection
	 * @param message
	 */
	public static void checkNotEmpty(final Map<?, ?> collection, final String message) {
		if (collection == null || collection.size() == 0)
			throw new IllegalArgumentException(message);
	}

	/**
	 * Throw a {@link FoException} with a custom message if the given collection is null or empty.
	 *
	 * @param collection
	 * @param message
	 */
	public static void checkNotEmpty(final Collection<?> collection, final String message) {
		if (collection == null || collection.size() == 0)
			throw new IllegalArgumentException(message);
	}

	/**
	 * Throw a {@link FoException} if the given message is empty or null.
	 *
	 * @param message
	 * @param emptyMessage
	 */
	public static void checkNotEmpty(final String message, final String emptyMessage) {
		if (message == null || message.length() == 0)
			throw new IllegalArgumentException(emptyMessage);
	}

	/**
	 * Check if the audience has the given permission, if false we send him a message found in the 'no-permission' key
	 * in {@link Lang} and return false, otherwise no message is sent and we return true.
	 *
	 * @param audience
	 * @param permission
	 * @return
	 */
	public static boolean checkPermission(final FoundationPlayer audience, final String permission) {
		if (!audience.hasPermission(permission)) {
			audience.sendMessage(Lang.componentVars("no-permission", "permission", SimpleComponent.fromPlain(permission)));

			return false;
		}

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Checking for true without throwing errors
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the given string is a valid integer.
	 *
	 * @param raw
	 * @return
	 */
	public static boolean isInteger(final String raw) {
		ValidCore.checkNotNull(raw, "Cannot check if null is an integer!");

		return ValidCore.PATTERN_INTEGER.matcher(raw).matches();
	}

	/**
	 * Check if the given string is a valid integer, if not throw a {@link FoException}.
	 *
	 * @param raw
	 * @param falseMessage
	 */
	public static void checkDecimal(final String raw, final String falseMessage) {
		ValidCore.checkBoolean(isDecimal(raw), falseMessage);
	}

	/**
	 * Return true if the given string is a valid whole number.
	 *
	 * @param raw
	 * @return
	 */
	public static boolean isDecimal(final String raw) {
		ValidCore.checkNotNull(raw, "Cannot check if null is a decimal!");

		return ValidCore.PATTERN_DECIMAL.matcher(raw).matches();
	}

	/**
	 * <p>Checks whether the String a valid Java number.</p>
	 *
	 * <p>Valid numbers include hexadecimal marked with the <code>0x</code>
	 * qualifier, scientific notation and numbers marked with a type
	 * qualifier (e.g. 123L).</p>
	 *
	 * <p><code>Null</code> and empty String will return
	 * <code>false</code>.</p>
	 *
	 * @author Apache Commons NumberUtils
	 * @param raw  the <code>String</code> to check
	 * @return <code>true</code> if the string is a correctly formatted number
	 */
	public static boolean isNumber(@NonNull final String raw) {
		ValidCore.checkNotNull(raw, "Cannot check if null is a Number!");

		if (raw.isEmpty())
			return false;

		final char[] letters = raw.toCharArray();
		int length = letters.length;
		boolean hasExp = false;
		boolean hasDecPoint = false;
		boolean allowSigns = false;
		boolean foundDigit = false;

		// deal with any possible sign up front
		final int start = (letters[0] == '-') ? 1 : 0;

		if (length > start + 1)
			if (letters[start] == '0' && letters[start + 1] == 'x') {
				int i = start + 2;
				if (i == length)
					return false; // str == "0x"
				// checking hex (it can't be anything else)
				for (; i < letters.length; i++)
					if ((letters[i] < '0' || letters[i] > '9')
							&& (letters[i] < 'a' || letters[i] > 'f')
							&& (letters[i] < 'A' || letters[i] > 'F'))
						return false;
				return true;
			}
		length--; // don't want to loop to the last char, check it afterwords
		// for type qualifiers
		int i = start;
		// loop to the next to last char or to the last char if we need another digit to
		// make a valid number (e.g. chars[0..5] = "1234E")
		while (i < length || (i < length + 1 && allowSigns && !foundDigit)) {
			if (letters[i] >= '0' && letters[i] <= '9') {
				foundDigit = true;
				allowSigns = false;

			} else if (letters[i] == '.') {
				if (hasDecPoint || hasExp)
					// two decimal points or dec in exponent
					return false;
				hasDecPoint = true;
			} else if (letters[i] == 'e' || letters[i] == 'E') {
				// we've already taken care of hex.
				if (hasExp)
					// two E's
					return false;
				if (!foundDigit)
					return false;
				hasExp = true;
				allowSigns = true;
			} else if (letters[i] == '+' || letters[i] == '-') {
				if (!allowSigns)
					return false;
				allowSigns = false;
				foundDigit = false; // we need a digit after the E
			} else
				return false;
			i++;
		}
		if (i < letters.length) {
			if (letters[i] >= '0' && letters[i] <= '9')
				// no type qualifier, OK
				return true;
			if (letters[i] == 'e' || letters[i] == 'E')
				// can't have an E at the last byte
				return false;
			if (letters[i] == '.') {
				if (hasDecPoint || hasExp)
					// two decimal points or dec in exponent
					return false;
				// single trailing decimal point after non-exponent is ok
				return foundDigit;
			}
			if (!allowSigns
					&& (letters[i] == 'd'
							|| letters[i] == 'D'
							|| letters[i] == 'f'
							|| letters[i] == 'F'))
				return foundDigit;
			if (letters[i] == 'l'
					|| letters[i] == 'L')
				// not allowing L with an exponent
				return foundDigit && !hasExp;
			// last character is illegal
			return false;
		}
		// allowSigns is true iff the val ends in 'E'
		// found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
		return !allowSigns && foundDigit;
	}

	/**
	 * Return true if the array consists of null or empty string values only.
	 *
	 * @param array
	 * @return
	 */
	public static boolean isNullOrEmpty(final Collection<?> array) {
		return array == null || ValidCore.isNullOrEmpty(array.toArray());
	}

	/**
	 * Return true if the map is null or only contains null values.
	 *
	 * @param map
	 * @return
	 */
	public static boolean isNullOrEmptyValues(final SerializedMap map) {
		return isNullOrEmptyValues(map == null ? null : map.asMap());
	}

	/**
	 * Return true if the map is null or only contains null values.
	 *
	 * @param map
	 * @return
	 */
	public static boolean isNullOrEmptyValues(final Map<?, ?> map) {
		if (map == null)
			return true;

		for (final Object value : map.values())
			if (value != null)
				return false;

		return true;
	}

	/**
	 * Return true if the array consists of null or empty string values only.
	 *
	 * @param array
	 * @return
	 */
	public static boolean isNullOrEmpty(final Object[] array) {
		if (array != null)
			for (final Object object : array)
				if (object instanceof String) {
					if (!((String) object).isEmpty())
						return false;

				} else if (object != null)
					return false;

		return true;
	}

	/**
	 * Return true if the given message is null or empty.
	 *
	 * @param message
	 * @return
	 */
	public static boolean isNullOrEmpty(final String message) {
		return message == null || message.isEmpty();
	}

	/**
	 * Return true if the given value is between bounds.
	 *
	 * @param value
	 * @param ranged
	 * @return
	 */
	public static boolean isInRange(final long value, final RangedValue ranged) {
		return value >= ranged.getMinLong() && value <= ranged.getMaxLong();
	}

	/**
	 * Return true if the given value is between bounds.
	 *
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static boolean isInRange(final double value, final double min, final double max) {
		return value >= min && value <= max;
	}

	/**
	 * Return true if the given value is between bounds.
	 *
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static boolean isInRange(final long value, final long min, final long max) {
		return value >= min && value <= max;
	}

	/**
	 * Return true if the given object is a {@link UUID}.
	 *
	 * @param object
	 * @return
	 */
	public static boolean isUUID(final Object object) {
		if (object instanceof String) {
			final String[] components = object.toString().split("-");

			return components.length == 5;
		}

		return object instanceof UUID;
	}

	/**
	 * Return if the input is a primitive wrapper such as Integer, Boolean, etc.
	 *
	 * @param input
	 * @return
	 */
	public static boolean isPrimitiveWrapper(Object input) {
		return input instanceof Integer || input instanceof Boolean || input instanceof Character || input instanceof Byte || input instanceof Short || input instanceof Double || input instanceof Long || input instanceof Float;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Equality checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Compare two lists. Two lists are considered equal if they are same length and all values are the same.
	 * Strings are stripped of colors before comparation.
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public static boolean listEquals(final String[] first, final String[] second) {
		return listEquals(CommonCore.toList(first), CommonCore.toList(second));
	}

	/**
	 * Compare two lists. Two lists are considered equal if they are same length and all values are the same.
	 * Exception: Strings are stripped of colors before comparation.
	 *
	 * @param <T>
	 * @param first first list to compare
	 * @param second second list to compare with
	 * @return true if lists are equal
	 */
	public static <T> boolean listEquals(final List<String> first, final List<String> second) {
		if (first == null && second == null)
			return true;

		if (first == null)
			return false;

		if (second == null)
			return false;

		if (first.size() != second.size())
			return false;

		for (int i = 0; i < first.size(); i++) {
			final String firstElement = first.get(i);
			final String secondElement = second.get(i);

			if (firstElement == null && secondElement != null)
				return false;

			if (firstElement != null && secondElement == null)
				return false;

			if (firstElement != null && !firstElement.equals(secondElement))
				if (!CompChatColor.stripColorCodes(firstElement.toString()).equalsIgnoreCase(CompChatColor.stripColorCodes(secondElement.toString())))
					return false;
		}

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Matching in lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the given is in the given list, depending on the mode
	 *
	 * If isBlacklist mode is enabled, we return true when element is NOT in the list,
	 * if it is false, we return true if element is in the list.
	 *
	 * @param element
	 * @param isBlacklist
	 * @param list
	 * @return
	 */
	public static boolean isInList(final String element, final boolean isBlacklist, final Iterable<String> list) {
		return isBlacklist == ValidCore.isInList(element, list);
	}

	/**
	 * Return true if any element in the given list equals (case ignored) to your given element.
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public static boolean isInList(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).equalsIgnoreCase(removeSlash(matched)))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * Return true if any element in the given list starts with (case ignored) your given element.
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public static boolean isInListStartsWith(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).toLowerCase().startsWith(removeSlash(matched).toLowerCase()))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * Return true if any element in the given list matches your given element.
	 *
	 * A regular expression is compiled from that list element.
	 *
	 * @param message
	 * @param patterns
	 * @return
	 */
	public static boolean isInListRegex(final String message, final Iterable<String> patterns) {
		try {
			for (final String pattern : patterns)
				if (CommonCore.compilePattern(pattern).matcher(message).find())
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * Return true if any element in the given list matches your given element.
	 *
	 * A regular expression is compiled from that list element.
	 *
	 * @param message
	 * @param patterns
	 * @return
	 */
	public static boolean isInListRegexFast(final String message, final Iterable<Pattern> patterns) {
		try {
			for (final Pattern pattern : patterns)
				if (pattern.matcher(message).find())
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/*
	 * Prepare the message for isInList comparation - lowercases it and removes the initial slash /
	 */
	private static String removeSlash(final String message) {
		return message.startsWith("/") ? message.substring(1) : message;
	}
}