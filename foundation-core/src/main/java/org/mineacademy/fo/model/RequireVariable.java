package org.mineacademy.fo.model;

import java.util.function.Function;

import org.mineacademy.fo.exception.FoException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a variable that must match a certain value
 * for example in ChatControl's rules this is used as "require variable {player_gamemode} CREATIVE"
 * and so on.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequireVariable {

	/**
	 * The variable to check
	 */
	private final String variable;

	/**
	 * The required value, if starts with !, it means it should not match
	 */
	private final String requiredValue;

	/**
	 * Check if the variable matches the required value
	 *
	 * @param replacer the function to replace the variable with its actual value, parse variables here
	 * @return
	 */
	public boolean matches(Function<String, String> replacer) {
		String result = replacer.apply(this.variable);

		if ("yes".equals(result) || "1".equals(result))
			result = "true";

		else if ("no".equals(result) || "0".equals(result) || "".equals(result))
			result = "false";

		if (!this.requiredValue.startsWith("!") && !result.equalsIgnoreCase(this.requiredValue))
			return false;

		if (this.requiredValue.startsWith("!") && result.equalsIgnoreCase(this.requiredValue.substring(1)))
			return false;

		return true;
	}

	/**
	 * Parse the given line into a new RequireVariable
	 *
	 * @param line
	 * @return
	 */
	public static RequireVariable parse(String line) {
		final String[] split = line.split(" ");

		if (split.length != 1 && split.length != 2)
			throw new FoException("Invalid require variable syntax - it must be in the form '<variable> <true/false>' or '<variable>' (to match if it is true), got: '" + line + "'", false);

		else {
			final String variable = split[0];
			final String requiredValue = split.length == 2 ? split[1] : "true";

			return from(variable, requiredValue);
		}
	}

	/**
	 * Create a new RequireVariable from the given variable and required value
	 *
	 * @param variable
	 * @param requiredValue
	 * @return
	 */
	public static RequireVariable from(String variable, String requiredValue) {
		if ("yes".equals(requiredValue) || "1".equals(requiredValue))
			requiredValue = "true";

		else if ("no".equals(requiredValue) || "0".equals(requiredValue))
			requiredValue = "false";

		return new RequireVariable(variable, requiredValue);
	}
}
