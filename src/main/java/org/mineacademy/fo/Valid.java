package org.mineacademy.fo;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.settings.SimpleLocalization;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for checking conditions and throwing our safe exception that is
 * logged into file.
 */
@UtilityClass
public final class Valid {

	/**
	 * Matching valid integers
	 */
	private final Pattern PATTERN_INTEGER = Pattern.compile("-?\\d+");

	/**
	 * Matching valid whole numbers
	 */
	private final Pattern PATTERN_DECIMAL = Pattern.compile("([0-9]+\\.?[0-9]*|\\.[0-9]+)");

	// ------------------------------------------------------------------------------------------------------------
	// Checking for validity and throwing errors if false or null
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Throw an error if the given object is null
	 *
	 * @param toCheck
	 */
	public void checkNotNull(final Object toCheck) {
		if (toCheck == null)
			throw new FoException();
	}

	/**
	 * Throw an error with a custom message if the given object is null
	 *
	 * @param toCheck
	 * @param falseMessage
	 */
	public void checkNotNull(final Object toCheck, final String falseMessage) {
		if (toCheck == null)
			throw new FoException(falseMessage);
	}

	/**
	 * Throw an error if the given expression is false
	 *
	 * @param expression
	 */
	public void checkBoolean(final boolean expression) {
		if (!expression)
			throw new FoException();
	}

	/**
	 * Throw an error with a custom message if the given expression is false
	 *
	 * @param expression
	 * @param falseMessage
	 * @param replacements
	 */
	public void checkBoolean(final boolean expression, final String falseMessage, final Object... replacements) {
		if (!expression) {
			String message = falseMessage;

			try {
				message = String.format(falseMessage, replacements);

			} catch (final Throwable t) {
			}

			throw new FoException(message);
		}
	}

	/**
	 * Throw an error with a custom message if the given toCheck string is not a number!
	 *
	 * @param toCheck
	 * @param falseMessage
	 * @param replacements
	 */
	public void checkInteger(final String toCheck, final String falseMessage, final Object... replacements) {
		if (!Valid.isInteger(toCheck))
			throw new FoException(String.format(falseMessage, replacements));
	}

	/**
	 * Throw an error with a custom message if the given collection is null or empty
	 *
	 * @param collection
	 * @param message
	 */
	public void checkNotEmpty(final Collection<?> collection, final String message) {
		if (collection == null || collection.size() == 0)
			throw new IllegalArgumentException(message);
	}

	/**
	 * Throw an error if the given message is empty or null
	 *
	 * @param message
	 * @param emptyMessage
	 */
	public void checkNotEmpty(final String message, final String emptyMessage) {
		if (message == null || message.length() == 0)
			throw new IllegalArgumentException(emptyMessage);
	}

	/**
	 * Check if the player has the given permission, if false we send him {@link SimpleLocalization#NO_PERMISSION}
	 * message and return false, otherwise no message is sent and we return true
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public boolean checkPermission(final CommandSender sender, final String permission) {
		if (!PlayerUtil.hasPerm(sender, permission)) {
			Common.tell(sender, SimpleLocalization.NO_PERMISSION.replace("{permission}", permission));

			return false;
		}

		return true;
	}

	/**
	 * Check if the code calling this method is run from the main thread,
	 * failing with the error message if otherwise
	 *
	 * @param asyncErrorMessage
	 */
	public void checkSync(final String asyncErrorMessage) {
		Valid.checkBoolean(Bukkit.isPrimaryThread(), asyncErrorMessage);
	}

	/**
	 * Check if the code calling this method is run from a different than main thread,
	 * failing with the error message if otherwise
	 *
	 * @param syncErrorMessage
	 */
	public void checkAsync(final String syncErrorMessage) {
		Valid.checkBoolean(!Bukkit.isPrimaryThread(), syncErrorMessage);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Checking for true without throwing errors
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the given string is a valid integer
	 *
	 * @param raw
	 * @return
	 */
	public boolean isInteger(final String raw) {
		Valid.checkNotNull(raw, "Cannot check if null is an integer!");

		return Valid.PATTERN_INTEGER.matcher(raw).matches();
	}

	/**
	 * Return true if the given string is a valid whole number
	 *
	 * @param raw
	 * @return
	 */
	public boolean isDecimal(final String raw) {
		Valid.checkNotNull(raw, "Cannot check if null is a decimal!");

		return Valid.PATTERN_DECIMAL.matcher(raw).matches();
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
	public static boolean isNumber(@NonNull String raw) {
		Valid.checkNotNull(raw, "Cannot check if null is a Number!");

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
	 * Return true if the array consists of null or empty string values only
	 *
	 * @param array
	 * @return
	 */
	public boolean isNullOrEmpty(final Collection<?> array) {
		return array == null || Valid.isNullOrEmpty(array.toArray());
	}

	/**
	 * Return true if the map is null or only contains null values
	 *
	 * @param map
	 * @return
	 */
	public boolean isNullOrEmptyValues(SerializedMap map) {
		return isNullOrEmptyValues(map == null ? null : map.asMap());
	}

	/**
	 * Return true if the map is null or only contains null values
	 *
	 * @param map
	 * @return
	 */
	public boolean isNullOrEmptyValues(final Map<?, ?> map) {

		if (map == null)
			return true;

		for (final Object value : map.values())
			if (value != null)
				return false;

		return true;
	}

	/**
	 * Return true if the array consists of null or empty string values only
	 *
	 * @param array
	 * @return
	 */
	public boolean isNullOrEmpty(final Object[] array) {
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
	 * Return true if the given message is null or empty
	 *
	 * @param message
	 * @return
	 */
	public boolean isNullOrEmpty(final String message) {
		return message == null || message.isEmpty();
	}

	/**
	 * Return true if all x-y-z coordinates of the given vector are finite valid numbers
	 * (see {@link Double#isFinite(double)})
	 *
	 * @param vector
	 * @return
	 */
	public boolean isFinite(final Vector vector) {
		return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
	}

	/**
	 * Return true if the given value is between bounds
	 *
	 * @param value
	 * @param ranged
	 * @return
	 */
	public boolean isInRange(final long value, final RangedValue ranged) {
		return value >= ranged.getMinLong() && value <= ranged.getMaxLong();
	}

	/**
	 * Return true if the given value is between bounds
	 *
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public boolean isInRange(final double value, final double min, final double max) {
		return value >= min && value <= max;
	}

	/**
	 * Return true if the given value is between bounds
	 *
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public boolean isInRange(final long value, final long min, final long max) {
		return value >= min && value <= max;
	}

	/**
	 * Return true if the given object is a {@link UUID}
	 *
	 * @param object
	 * @return
	 */
	public boolean isUUID(Object object) {
		if (object instanceof String) {
			final String[] components = object.toString().split("-");

			return components.length == 5;
		}

		return object instanceof UUID;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Equality checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the two locations has same world and block positions
	 *
	 * @param first
	 * @param sec
	 * @return
	 */
	public boolean locationEquals(final Location first, final Location sec) {

		if (first == null && sec == null)
			return true;

		if ((first == null && sec == null) || (first != null && sec == null))
			return false;

		try {
			if (!first.getWorld().getName().equals(sec.getWorld().getName()))
				return false;
		} catch (final NullPointerException ex) {
			// Ignore
		}

		return first.getBlockX() == sec.getBlockX() && first.getBlockY() == sec.getBlockY() && first.getBlockZ() == sec.getBlockZ();
	}

	/**
	 * Compare two lists. Two lists are considered equal if they are same length and all values are the same.
	 * Exception: Strings are stripped of colors before comparation.
	 *
	 * @param first first list to compare
	 * @param second second list to compare with
	 * @return true if lists are equal
	 */
	public <T> boolean listEquals(final List<T> first, final List<T> second) {
		if (first == null && second == null)
			return true;

		if (first == null)
			return false;

		if (second == null)
			return false;

		if (first.size() != second.size())
			return false;

		for (int i = 0; i < first.size(); i++) {
			final T f = first.get(i);
			final T s = second.get(i);

			if (f == null && s != null)
				return false;

			if (f != null && s == null)
				return false;

			if (f != null && !f.equals(s))
				if (!Common.stripColors(f.toString()).equalsIgnoreCase(Common.stripColors(s.toString())))
					return false;
		}

		return true;
	}

	/**
	 * Return true if two strings are equal regardless of their colors
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public boolean colorlessEquals(final String first, final String second) {
		return Common.stripColors(first).equalsIgnoreCase(Common.stripColors(second));
	}

	/**
	 * Return true if two string lists are equal regardless of their colors
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public boolean colorlessEquals(final List<String> first, final List<String> second) {
		return colorlessEquals(Common.toArray(first), Common.toArray(second));
	}

	/**
	 * Return true if two string arrays are equal regardless of their colors
	 *
	 * @param firstArray
	 * @param secondArray
	 * @return
	 */
	public boolean colorlessEquals(final String[] firstArray, final String[] secondArray) {
		for (int i = 0; i < firstArray.length; i++) {
			final String first = Common.stripColors(firstArray[i]);
			final String second = i < secondArray.length ? Common.stripColors(secondArray[i]) : "";

			if (!first.equalsIgnoreCase(second))
				return false;
		}

		return true;
	}

	/**
	 * Return true if the given list contains all strings equal
	 *
	 * @param values
	 * @return
	 */
	public boolean valuesEqual(Collection<String> values) {
		final List<String> copy = new ArrayList<>(values);
		String lastValue = null;

		for (final String value : copy) {
			if (lastValue == null)
				lastValue = value;

			if (!lastValue.equals(value))
				return false;

			lastValue = value;
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
	public boolean isInList(final String element, final boolean isBlacklist, final Iterable<String> list) {
		return isBlacklist == Valid.isInList(element, list);
	}

	/**
	 * Return true if any element in the given list equals (case ignored) to your given element
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public boolean isInList(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).equalsIgnoreCase(removeSlash(matched)))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * Return true if any element in the given list starts with (case ignored) your given element
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public boolean isInListStartsWith(final String element, final Iterable<String> list) {
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
	 * @param element
	 * @param list
	 * @return
	 */
	public boolean isInListRegex(final String element, final Iterable<String> list) {
		try {
			for (final String regex : list)
				if (Common.regExMatch(regex, element))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * Return true if the given enum contains the given element, by {@link Enum#name()} (case insensitive)
	 *
	 * @param element
	 * @param enumeration
	 * @return
	 */
	public boolean isInListEnum(final String element, final Enum<?>[] enumeration) {
		for (final Enum<?> constant : enumeration)
			if (constant.name().equalsIgnoreCase(element))
				return true;

		return false;
	}

	/**
	 * Return true if any element in the given list contains (case ignored) your given element
	 *
	 * @param element
	 * @param list
	 * @return
	 *
	 * @deprecated can lead to unwanted matches such as when /time is in list, /t will also get caught
	 */
	@Deprecated
	public boolean isInListContains(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).toLowerCase().contains(removeSlash(matched).toLowerCase()))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * Prepare the message for isInList comparation - lowercases it and removes the initial slash /
	 *
	 * @param message
	 * @return
	 */
	private String removeSlash(String message) {
		return message.startsWith("/") ? message.substring(1) : message;
	}
}
