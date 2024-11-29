package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.collection.ExpiringMap;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.Component;

/**
 * A class that replaces variables in a message. In Foundation, we use
 * placeholders and variables interchangeably.
 *
 * However, for clarity, a Map<String, Object> is typically called "placeholders".
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Variables {

	/**
	 * The pattern to find [syntax_name] variables.
	 */
	public static final Pattern MESSAGE_VARIABLE_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

	/**
	 * The pattern to find {syntax} variables.
	 */
	public static final Pattern BRACKET_VARIABLE_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	/**
	 * The pattern to find simple {syntax} placeholders starting with {rel_} (used for PlaceholderAPI)
	 */
	public static final Pattern BRACKET_REL_VARIABLE_PATTERN = Pattern.compile("[({)](rel_)([^}]+)[(})]");

	/**
	 * We use this string to join and split lists of strings when replacing variables to increase performance.
	 */
	private static final String MAGIC_STRING_CONCATENATION = "%FLPV%";

	/**
	 * The cache for variables that expire after 5 seconds.
	 */
	private static final Map<String, SimpleComponent> cache = ExpiringMap.builder().expiration(5, TimeUnit.SECONDS).build();

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * This is used to dynamically replace the variable based on its content, like
	 * PlaceholderAPI.
	 *
	 * We also hook into PlaceholderAPI, however, you'll have to use your plugin's prefix before
	 * all variables when called from there.
	 */
	private static final List<SimpleExpansion> expansions = new ArrayList<>();

	/**
	 * Whether we should replace JavaScript variables in replace() methods.
	 *
	 * Used to prevent a race condition.
	 */
	@Getter(value = AccessLevel.PACKAGE)
	@Setter(value = AccessLevel.PACKAGE)
	private static boolean replaceScript = true;

	/**
	 * The audience for whom we are replacing variables.
	 */
	private FoundationPlayer audience;

	/**
	 * The custom placeholders map we apply on top of other placeholders.
	 */
	private Map<String, Object> placeholders = new HashMap<>();

	/**
	 * The mode for the legacy replace() methods.
	 */
	private LegacyMode legacyMode = LegacyMode.TO_SECTION;

	/**
	 * Set the audience for whom we are replacing variables.
	 *
	 * This must be compatible with {@link Platform#toPlayer(Object)}
	 *
	 * @param audience
	 * @return
	 */
	public Variables audience(@Nullable Object audience) {
		this.audience = audience == null ? null : Platform.toPlayer(audience);

		return this;
	}

	/**
	 * Set the placeholders map.
	 *
	 * @param placeholders
	 * @return
	 */
	public Variables placeholders(@NonNull Map<String, Object> placeholders) {
		this.placeholders = placeholders;

		return this;
	}

	/**
	 * Return the placeholders map.
	 *
	 * @return
	 */
	public Map<String, Object> placeholders() {
		return this.placeholders;
	}

	/**
	 * Add an array of placeholders to the placeholders map.
	 * They must be in the format: string, value, string, value etc.
	 * Where value must be either a String, a primitive or a SimpleComponent.
	 *
	 * For example: placeholderArray(player, "Notch, "age", 21, "rank", SimpleComponent.fromSection("&cVIP")
	 *
	 * @param placeholders
	 * @return
	 */
	public Variables placeholderArray(@NonNull Object... placeholders) {
		this.placeholders = CommonCore.newHashMap(placeholders);

		return this;
	}

	/**
	 * Add a custom placeholder to the placeholders map.
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public Variables placeholder(@NonNull String key, @NonNull Object value) {
		this.placeholders.put(key, value);

		return this;
	}

	/**
	 * Set the mode for the legacy replace() methods. Because each replace()
	 * method works with components, we need to know how to turn them back
	 * into legacy. By default, it's {@link LegacyMode#TO_SECTION}.
	 *
	 * @param legacyMode
	 * @return
	 */
	public Variables legacyMode(@NonNull LegacyMode legacyMode) {
		this.legacyMode = legacyMode;

		return this;
	}

	/**
	 * Replace variables in the given list.
	 *
	 * @see #replace(String)
	 *
	 * @param list
	 * @return
	 */
	public List<String> replaceList(@NonNull List<String> list) {
		return Arrays.asList(this.replace(String.join(MAGIC_STRING_CONCATENATION, list)).split(MAGIC_STRING_CONCATENATION)); // less overhead than replacing each element
	}

	/**
	 * Replace variables in the given list array.
	 *
	 * @see #replace(String)
	 *
	 * @param list
	 * @return
	 */
	public String[] replaceArray(@NonNull String[] list) {
		return this.replace(String.join(MAGIC_STRING_CONCATENATION, list.clone())).split(MAGIC_STRING_CONCATENATION); // less overhead than replacing each element
	}

	/**
	 * Replace variables in the message.
	 *
	 * PlaceholderAPI is supported.
	 *
	 * This method substitutes placeholders and variables with corresponding values from various sources,
	 * such as predefined strings, player information, and configurations.
	 *
	 * For example, it could replace the variable "prefix_warn" with a warning prefix like "[Warn]"
	 * or replace built-in placeholders like server name or formatted dates.
	 *
	 * To add custom variables, see {@link #addExpansion(SimpleExpansion)}
	 *
	 * @param message
	 * @return
	 */
	public String replace(@NonNull String message) {
		final Matcher matcher = BRACKET_VARIABLE_PATTERN.matcher(message);
		final StringBuilder result = new StringBuilder();
		int lastMatchEnd = 0;

		while (matcher.find()) {
			final String variable = matcher.group(1);

			result.append(message, lastMatchEnd, matcher.start());

			final SimpleComponent value = this.replaceVariable(variable);

			if (value != null)
				switch (this.legacyMode) {
					case TO_PLAIN:
						result.append(value.toPlain());
						break;
					case TO_SECTION:
						result.append(value.toLegacy());
						break;
					case TO_MINI:
						result.append(value.toMini());
						break;
				}

			else
				result.append(matcher.group());

			lastMatchEnd = matcher.end();
		}

		result.append(message.substring(lastMatchEnd));
		return result.toString();
	}

	/**
	 * Replace variables in the component.
	 *
	 * PlaceholderAPI is supported.
	 *
	 * This method substitutes placeholders and variables with corresponding values from various sources,
	 * such as predefined strings, player information, and configurations.
	 *
	 * For example, it could replace the variable "prefix_warn" with a warning prefix like "[Warn]"
	 * or replace built-in placeholders like server name or formatted dates.
	 *
	 * To add custom variables, see {@link #addExpansion(SimpleExpansion)}
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent replace(@NonNull SimpleComponent component) {
		return component.replaceMatch(BRACKET_VARIABLE_PATTERN, (result, input) -> {
			final String variable = result.group(1);
			final SimpleComponent value = this.replaceVariable(variable);

			return value == null ? SimpleComponent.fromPlain(result.group()) : value;
		});
	}

	/*
	 * Replace a given variable with its corresponding value.
	 */
	private SimpleComponent replaceVariable(String variable) {
		SimpleComponent replacedValue = null;

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

		for (final Map.Entry<String, Object> entry : this.placeholders.entrySet()) {
			final String key = entry.getKey();

			if (key.equals(variable)) {
				final Object rawValue = entry.getValue();

				if (rawValue == null)
					return SimpleComponent.empty();

				if (rawValue instanceof SimpleComponent)
					replacedValue = (SimpleComponent) rawValue;

				else if (rawValue instanceof Component)
					replacedValue = SimpleComponent.fromAdventure((Component) rawValue);

				else if (rawValue instanceof Collection)
					replacedValue = SimpleComponent.fromSection(CommonCore.joinAnd((Collection<?>) rawValue));

				else if (rawValue.getClass().isArray())
					replacedValue = SimpleComponent.fromSection(CommonCore.joinAnd(Arrays.asList((Object[]) rawValue)));

				else
					replacedValue = SimpleComponent.fromMini(rawValue.toString());

				break;
			}
		}

		if (replacedValue == null)
			replacedValue = cache.get(variable);

		if (replacedValue == null && this.audience != null && replaceScript) {
			final Variable javascriptKey = Variable.findVariable(variable, Variable.Type.FORMAT);

			if (javascriptKey != null) {
				final SimpleComponent value = javascriptKey.build(this.audience, this.placeholders);

				if (value != null)
					replacedValue = value;
			}
		}

		if (replacedValue == null)
			for (final SimpleExpansion expansion : expansions) {
				final SimpleComponent value = expansion.replacePlaceholders(this.audience, variable);

				if (value != null) {
					replacedValue = value;

					break;
				}
			}

		if (replacedValue != null)
			cache.put(variable, replacedValue);
		else
			cache.put(variable, SimpleComponent.fromPlain(variable));

		if (frontSpace)
			replacedValue = SimpleComponent.fromPlain(" ").append(replacedValue);

		if (backSpace)
			replacedValue = replacedValue.appendPlain(" ");

		return replacedValue;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Register a new expansion if it was not already registered.
	 *
	 * @param expansion
	 */
	public static void addExpansion(SimpleExpansion expansion) {
		expansions.add(expansion);

		expansions.sort((first, second) -> Integer.compare(second.getPriority(), first.getPriority()));
	}

	/**
	 * Return all registered expansions. The list is mutable.
	 *
	 * @return
	 */
	public static List<SimpleExpansion> getExpansions() {
		return expansions;
	}

	/**
	 * Return a new variables instance.
	 *
	 * @return
	 */
	public static Variables builder() {
		return new Variables();
	}

	/**
	 * Return a new variables instance replacing variables for the given audience.
	 *
	 * @param audience
	 * @return
	 */
	public static Variables builder(@Nullable FoundationPlayer audience) {
		return new Variables().audience(audience);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Represents how we should turn the component in replace() methods back to a legacy String.
	 *
	 * @see Variables#replace(String)
	 */
	public enum LegacyMode {

		/**
		 * Convert the components to plain text. No colors or formatting.
		 */
		TO_PLAIN,

		/**
		 * Convert the components to legacy text. § colors and formatting.
		 */
		TO_SECTION,

		/**
		 * Convert the components to mini text such as \<green\>Hello
		 */
		TO_MINI;
	}
}
