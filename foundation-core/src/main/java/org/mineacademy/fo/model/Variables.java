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
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
	 * Stores cache for legacy variables by audience's name.
	 */
	private static final Map<String, Map<String, String>> legacyCache = ExpiringMap.builder().expiration(100, TimeUnit.MILLISECONDS).build();

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
		this.placeholders.putAll(placeholders);

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

		for (final Map.Entry<String, Object> entry : this.placeholders.entrySet()) {
			final String key = entry.getKey();

			if (key.charAt(0) == '{' || key.charAt(key.length() - 1) == '}')
				throw new FoException("Placeholders must not start or end with {}. Found: " + key);
		}

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
	 * Replace variables in the given list.
	 *
	 * @see #replaceLegacy(String)
	 *
	 * @param list
	 * @return
	 */
	public List<String> replaceLegacyList(@NonNull List<String> list) {
		final List<String> replaced = new ArrayList<>(list.size());

		for (int i = 0; i < list.size(); i++)
			replaced.add(this.replaceLegacy(list.get(i)));

		return replaced;
	}

	/**
	 * Replace variables in the given list array.
	 *
	 * @see #replaceLegacy(String)
	 *
	 * @param array
	 * @return
	 */
	public String[] replaceLegacyArray(@NonNull String[] array) {
		final String[] replaced = new String[array.length];

		for (int i = 0; i < array.length; i++)
			replaced[i] = this.replaceLegacy(array[i]);

		return array;
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
	public String replaceLegacy(@NonNull String message) {
		final Matcher matcher = BRACKET_VARIABLE_PATTERN.matcher(message);
		final StringBuilder result = new StringBuilder();
		int lastMatchEnd = 0;

		final Map<String, String> cache = this.audience != null ? legacyCache.getOrDefault(this.audience.getName(), new HashMap<>()) : null;

		while (matcher.find()) {
			final String variable = matcher.group(1);
			result.append(message, lastMatchEnd, matcher.start());

			final String cached = cache != null ? cache.get(variable) : null;

			if (cached != null)
				result.append(cached);

			else {
				String value = this.replaceVariableLegacy(variable);

				if (value == null)
					value = matcher.group();

				result.append(value);

				if (cache != null)
					cache.put(variable, value);
			}

			lastMatchEnd = matcher.end();
		}

		if (cache != null)
			legacyCache.put(this.audience.getName(), cache);

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
	public SimpleComponent replaceComponent(@NonNull SimpleComponent component) {
		return component.replaceMatch(BRACKET_VARIABLE_PATTERN, (result, input) -> {
			final String variable = result.group(1);
			final SimpleComponent value = this.replaceVariable(variable);

			return value == null ? PlainTextComponentSerializer.plainText().deserialize(result.group()) : value.toAdventure(this.audience);
		});
	}

	/*
	 * Replace a given variable with its corresponding value.
	 */
	private SimpleComponent replaceVariable(String variable) {
		SimpleComponent replacedValue = null;

		boolean frontSpace = false;
		boolean backSpace = false;

		if (variable.charAt(0) == '+') {
			variable = variable.substring(1);

			frontSpace = true;
		}

		if (variable.charAt(variable.length() - 1) == '+') {
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

		if (replacedValue == null && this.audience != null && replaceScript) {
			final Variable javascriptVariable = Variable.findVariableByKey(variable, Variable.Type.FORMAT);

			if (javascriptVariable != null) {
				final SimpleComponent value = javascriptVariable.build(this.audience, this.placeholders);

				if (value != null)
					replacedValue = value;
			}
		}

		if (replacedValue == null)
			for (final SimpleExpansion expansion : expansions) {
				final String value = expansion.replacePlaceholders(this.audience, variable);

				if (value != null) {
					replacedValue = SimpleComponent.fromMini(value);

					break;
				}
			}

		final String replacedPlainValue = replacedValue == null ? "" : replacedValue.toPlain(this.audience);

		if ((frontSpace || backSpace) && !replacedPlainValue.isEmpty()) {
			if (frontSpace && replacedPlainValue.charAt(0) != ' ')
				replacedValue = SimpleComponent.fromPlain(" ").append(replacedValue);

			if (backSpace && replacedPlainValue.charAt(replacedPlainValue.length() - 1) != ' ')
				replacedValue = replacedValue.appendPlain(" ");
		}

		return replacedValue;
	}

	/*
	 * Replace a given variable with its corresponding value.
	 */
	private String replaceVariableLegacy(String variable) {
		String replacedValue = null;

		boolean frontSpace = false;
		boolean backSpace = false;

		if (variable.charAt(0) == '+') {
			variable = variable.substring(1);

			frontSpace = true;
		}

		final int length = variable.length();

		if (variable.charAt(length - 1) == '+') {
			variable = variable.substring(0, variable.length() - 1);

			backSpace = true;
		}

		for (final Map.Entry<String, Object> entry : this.placeholders.entrySet()) {
			final String key = entry.getKey();

			if (key.equals(variable)) {
				final Object rawValue = entry.getValue();

				if (rawValue == null)
					return "";

				if (rawValue instanceof SimpleComponent)
					replacedValue = ((SimpleComponent) rawValue).toMini(this.audience);

				else if (rawValue instanceof Component)
					replacedValue = SimpleComponent.MINIMESSAGE_PARSER.serialize((Component) rawValue);

				else if (!(rawValue instanceof String) && !(rawValue instanceof Number))
					throw new IllegalArgumentException("Expected String in Variables#placeholders() in {" + key + "}, got " + rawValue.getClass().getSimpleName() + ": was " + rawValue);

				else
					replacedValue = rawValue.toString();

				break;
			}
		}

		if (replacedValue == null && this.audience != null && replaceScript) {
			final Variable javascriptVariable = Variable.findVariableByKey(variable, Variable.Type.FORMAT);

			if (javascriptVariable != null) {
				final String value = javascriptVariable.buildLegacy(this.audience, this.placeholders);

				if (value != null)
					replacedValue = value;
			}
		}

		if (replacedValue == null)
			for (final SimpleExpansion expansion : expansions) {
				final String value = expansion.replacePlaceholders(this.audience, variable);

				if (value != null) {
					replacedValue = value;

					break;
				}
			}

		if (replacedValue != null)
			if ((frontSpace || backSpace) && !replacedValue.isEmpty()) {
				if (frontSpace && !replacedValue.startsWith(" "))
					replacedValue = " " + replacedValue;

				if (backSpace && !replacedValue.endsWith(" "))
					replacedValue = replacedValue + " ";
			}

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
}
