package org.mineacademy.fo.model;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.AllArgsConstructor;

/**
 * An elegant way to find {variables} and replace them.
 */
@AllArgsConstructor
public final class Replacer {

	/**
	 * Replace all variables in the {@link SerializedMap#ofArray(Object...)} format
	 * adding {} to them if they do not contain it already
	 *
	 * @param list
	 * @param replacements
	 * @return
	 */
	public static List<String> replaceArray(List<String> list, Object... replacements) {
		String joined = String.join("%FLPV%", list);
		joined = replaceArray(joined, replacements);

		return java.util.Arrays.asList(joined.split("%FLPV%"));
	}

	/**
	 * Replace all variables in the {@link SerializedMap#ofArray(Object...)} format
	 * adding {} to them if they do not contain it already
	 *
	 * @param message
	 * @param replacements
	 * @return
	 */
	public static String replaceArray(String message, Object... replacements) {
		final SerializedMap map = SerializedMap.ofArray(replacements);

		return replaceVariables(message, map);
	}

	/**
	 * Replace all variables in the {@link SerializedMap#ofArray(Object...)} format
	 * adding {} to them if they do not contain it already
	 *
	 * @param list
	 * @param replacements
	 * @return
	 */
	public static List<String> replaceVariables(List<String> list, SerializedMap replacements) {
		String joined = String.join("%FLPV%", list);
		joined = replaceVariables(joined, replacements);

		return java.util.Arrays.asList(joined.split("%FLPV%"));
	}

	/**
	 * Replace key pairs in the message
	 *
	 * @param message
	 * @param variables
	 * @return
	 */
	public static String replaceVariables(String message, SerializedMap variables) {
		if (message == null)
			return null;

		if ("".equals(message))
			return "";

		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(message);

		while (matcher.find()) {
			String variable = matcher.group(1);

			boolean frontSpace = false;
			boolean backSpace = false;

			if (variable.startsWith("+")) {
				variable = variable.substring(1);

				frontSpace = true;
			}

			if (variable.endsWith("+")) {
				variable = variable.substring(0, variable.length() - 1);

				backSpace = true;
			}

			String value = null;

			for (final Map.Entry<String, Object> entry : variables.entrySet()) {
				String variableKey = entry.getKey();

				variableKey = variableKey.startsWith("{") ? variableKey.substring(1) : variableKey;
				variableKey = variableKey.endsWith("}") ? variableKey.substring(0, variableKey.length() - 1) : variableKey;

				if (variableKey.equals(variable))
					value = entry.getValue() == null ? "null" : entry.getValue().toString();
			}

			if (value != null) {
				final boolean emptyColorless = Common.stripColors(value).isEmpty();
				value = value.isEmpty() ? "" : (frontSpace && !emptyColorless ? " " : "") + Common.colorize(value) + (backSpace && !emptyColorless ? " " : "");

				message = message.replace(matcher.group(), value);
			}
		}

		return message;
	}
}
