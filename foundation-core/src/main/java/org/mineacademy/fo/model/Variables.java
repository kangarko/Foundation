package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
	public static final Pattern BRACKET_VARIABLE_PATTERN = Pattern.compile("\\{((?:[^{}]+|(?:\\{[^{}]*\\}))*)\\}");

	/**
	 * The pattern to find simple {syntax} placeholders starting with {rel_} (used for PlaceholderAPI)
	 */
	public static final Pattern BRACKET_REL_VARIABLE_PATTERN = Pattern.compile("[({)](rel_)([^}]+)[(})]");

	/**
	 * The patterns used for conversion of hex colors to mini.
	 */
	public static final Pattern HEX_AMPERSAND_PATTERN = Pattern.compile("(?<!<|:)&#([a-fA-F0-9]{6})(?!>)");
	public static final Pattern HEX_LITERAL_PATTERN = Pattern.compile("(?<!<|:|&)#([a-fA-F0-9]{6})(?!>)");
	public static final Pattern HEX_BRACKET_PATTERN = Pattern.compile("\\{#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})\\}");
	public static final Pattern HEX_MD5_PATTERN = Pattern.compile("[" + CompChatColor.COLOR_CHAR + "]x([" + CompChatColor.COLOR_CHAR + "][0-9a-fA-F]){6}");

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
	 * @deprecated internal use only, the full PlaceholderAPI parser
	 */
	@Deprecated
	@Setter
	private static BiFunction<FoundationPlayer, String, String> legacyPlaceholderAPIparser;

	/**
	 * Stores cache for legacy variables by audience's name.
	 */
	private final Map<ToLegacyMode, Map<String, Map<String, String>>> legacyCache = ExpiringMap.builder().expiration(500, TimeUnit.MILLISECONDS).build();

	/**
	 * Whether we should replace JavaScript variables in replace() methods.
	 *
	 * Used to prevent a race condition.
	 */
	@Getter(value = AccessLevel.PACKAGE)
	@Setter(value = AccessLevel.PACKAGE)
	private static boolean replaceScript = true;

	/**
	 * Set if we should support variables in variables?
	 * I.e. {luckperms_prefix} that returns itemsadder variable.
	 *
	 * Effectivelly halfs the performance of d plugin.
	 */
	@Getter
	@Setter
	private static boolean doubleParse = false;

	/**
	 * Convert &#123456 and #123456 to <#123456>? Defaults to false.
	 */
	@Getter
	@Setter
	private static boolean convertHexToMini = false;

	/**
	 * The audience for whom we are replacing variables.
	 */
	private FoundationPlayer audience;

	/**
	 * The custom placeholders map we apply on top of other placeholders.
	 */
	private final Map<String, Object> placeholders = new HashMap<>();

	/**
	 * Whether to convert the component to legacy text, plain text or mini message in replaceLegacy() methods.
	 */
	private ToLegacyMode toLegacyMode = ToLegacyMode.MINI;

	/**
	 * Whether to cache the result of the legacy conversion for 500ms. Defaults to false.
	 */
	private boolean cache = false;

	/**
	 * Set the audience for whom we are replacing variables.
	 *
	 * This must be compatible with {@link Platform#toPlayer(Object)}
	 *
	 * @param audience
	 * @return
	 */
	public Variables audience(@Nullable final Object audience) {
		this.audience = audience == null ? null : Platform.toPlayer(audience);

		return this;
	}

	/**
	 * Set the placeholders map.
	 *
	 * @param placeholders
	 * @return
	 */
	public Variables placeholders(@NonNull final Map<String, Object> placeholders) {
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
	public Variables placeholderArray(@NonNull final Object... placeholders) {
		final Map<String, Object> map = CommonCore.newHashMap(placeholders);

		for (final Map.Entry<String, Object> entry : map.entrySet()) {
			final String key = entry.getKey();

			if (key.charAt(0) == '{' || key.charAt(key.length() - 1) == '}')
				throw new FoException("Placeholders must not start or end with {}. Found: " + key);
		}

		this.placeholders.putAll(map);

		return this;
	}

	/**
	 * Add a custom placeholder to the placeholders map.
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public Variables placeholder(@NonNull final String key, @NonNull final Object value) {
		this.placeholders.put(key, value);

		return this;
	}

	/**
	 * Set the mode for converting component to legacy text.
	 *
	 * @param toLegacyMode
	 * @return
	 */
	public Variables toLegacyMode(@NonNull final ToLegacyMode toLegacyMode) {
		this.toLegacyMode = toLegacyMode;

		return this;
	}

	/**
	 * Set whether to cache the result of the legacy conversion for 500ms.
	 *
	 * @param cache
	 * @return
	 */
	public Variables cache(final boolean cache) {
		this.cache = cache;

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
	public List<String> replaceLegacyList(@NonNull final List<String> list) {
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
	public String[] replaceLegacyArray(@NonNull final String[] array) {
		final String[] replaced = new String[array.length];

		for (int i = 0; i < array.length; i++)
			replaced[i] = this.replaceLegacy(array[i]);

		return replaced;
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
		message = this.replaceLegacy0(message);

		if (legacyPlaceholderAPIparser != null)
			message = legacyPlaceholderAPIparser.apply(this.audience, message);

		if (doubleParse) {
			message = this.replaceLegacy0(message);

			if (legacyPlaceholderAPIparser != null)
				message = legacyPlaceholderAPIparser.apply(this.audience, message);
		}

		// We wrap them to prevent parsing variables in the {message}
		// So now we just heuristically unwrap
		message = message.replace("\\{U", "{").replace("\\O}", "}");

		return message;
	}

	/*
	 * Implementation for replacing variables.
	 */
	private String replaceLegacy0(@NonNull final String message) {
		final Matcher matcher = BRACKET_VARIABLE_PATTERN.matcher(message);
		final StringBuilder result = new StringBuilder();
		int lastMatchEnd = 0;

		final Map<String, String> cache = this.cache && this.audience != null ? legacyCache.getOrDefault(this.toLegacyMode, new HashMap<>()).getOrDefault(this.audience.getName(), new HashMap<>()) : null;

		while (matcher.find()) {
			final String variable = matcher.group(1);
			result.append(message, lastMatchEnd, matcher.start());

			final String cached = cache != null ? cache.get(variable) : null;

			if (cached != null)
				result.append(cached);

			else {
				String value = variable.isEmpty() ? matcher.group() : this.replaceVariableLegacy(variable);

				if (value == null)
					value = matcher.group();

				else {

					// Stupid was of fixing variables being parsed in {message}
					if (variable.equals("message"))
						value = value.replace("{", "\\{U").replace("}", "\\O}");

					// Probably there is a better way to do this...
					if (convertHexToMini && !variable.equals("message")) {
						final Matcher ampMatcher = HEX_AMPERSAND_PATTERN.matcher(value);
						value = ampMatcher.replaceAll("<#$1>");

						// Translate super long §x string to hex
						{
							final Matcher md5Matcher = HEX_MD5_PATTERN.matcher(value);
							final StringBuffer buffer = new StringBuffer();

							while (md5Matcher.find()) {
								final String legacyFormat = md5Matcher.group();
								final String hexColor = legacyFormat.replaceAll("[" + CompChatColor.COLOR_CHAR + "]", "").substring(1);

								md5Matcher.appendReplacement(buffer, "<#" + hexColor + ">");
							}

							md5Matcher.appendTail(buffer);
							value = buffer.toString();
						}

						// Translate {#132456} to hey
						{
							// Match both 3-digit and 6-digit hex codes inside {#} brackets
							final Matcher bracketMatcher = HEX_BRACKET_PATTERN.matcher(value);
							final StringBuffer buffer = new StringBuffer();

							while (bracketMatcher.find()) {
								String hex = bracketMatcher.group(1);

								// Expand 3-digit hex codes to 6 digits (e.g., #F00 → FF0000)
								if (hex.length() == 3)
									hex = hex.replaceAll("(.)", "$1$1");

								// Convert to MiniMessage color format while preserving case sensitivity
								final String replacement = "<#" + hex + ">";
								bracketMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
							}

							bracketMatcher.appendTail(buffer);
							value = buffer.toString();
						}
					}
				}

				result.append(value);

				if (this.cache && cache != null)
					cache.put(variable, value);
			}

			lastMatchEnd = matcher.end();
		}

		if (this.cache && cache != null)
			legacyCache.computeIfAbsent(this.toLegacyMode, key -> new HashMap<>()).put(this.audience.getName(), cache);

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
		component = this.replaceComponent0(component);

		if (doubleParse)
			component = this.replaceComponent0(component);

		return component;
	}

	/*
	 * Implementation for replacing variables.
	 */
	private SimpleComponent replaceComponent0(@NonNull final SimpleComponent component) {
		return component.replaceMatch(BRACKET_VARIABLE_PATTERN, (result, input) -> {
			final String variable = result.group(1);
			SimpleComponent value = variable.isEmpty() ? null : this.replaceVariable(variable);

			if (value != null && convertHexToMini) {
				value = value.replaceMatch(HEX_AMPERSAND_PATTERN, (result2, builder) -> {
					return Component.text("<#" + result2.group(1) + ">");
				});

				value = value.replaceMatch(HEX_MD5_PATTERN, (result2, builder) -> {
					final String legacyFormat = result2.group();
					final String hexColor = legacyFormat.replaceAll("[" + CompChatColor.COLOR_CHAR + "]", "").substring(1);

					return Component.text("<#" + hexColor + ">");
				});
			}

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

				else if (rawValue instanceof Boolean)
					replacedValue = SimpleComponent.fromPlain(rawValue.toString());

				else if (rawValue instanceof Collection)
					replacedValue = SimpleComponent.fromMiniAmpersand(CommonCore.join((Collection<?>) rawValue));

				else if (rawValue instanceof Enum[])
					replacedValue = SimpleComponent.fromMiniAmpersand(CommonCore.join((Enum[]) rawValue));

				else if (rawValue instanceof UUID)
					replacedValue = SimpleComponent.fromPlain(rawValue.toString());

				else if (!(rawValue instanceof String) && !(rawValue instanceof Number))
					throw new IllegalArgumentException("Expected String in Variables#placeholders() in {" + key + "}, got " + rawValue.getClass().getSimpleName() + ": was " + rawValue);

				else
					replacedValue = SimpleComponent.fromMiniSection(rawValue.toString());

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
					replacedValue = SimpleComponent.fromMiniSection(value);

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

				if (rawValue instanceof SimpleComponent) {
					final SimpleComponent component = (SimpleComponent) rawValue;

					if (this.toLegacyMode == ToLegacyMode.MINI)
						replacedValue = component.toMini(this.audience);

					else if (this.toLegacyMode == ToLegacyMode.PLAIN)
						replacedValue = component.toPlain(this.audience);

					else
						replacedValue = component.toLegacySection(this.audience);

				} else if (rawValue instanceof Component) {
					final Component component = (Component) rawValue;

					if (this.toLegacyMode == ToLegacyMode.MINI)
						replacedValue = SimpleComponent.serializeAdventureToMini(component);

					else if (this.toLegacyMode == ToLegacyMode.PLAIN)
						replacedValue = PlainTextComponentSerializer.plainText().serialize(component);

					else
						replacedValue = LegacyComponentSerializer.legacySection().serialize(component);
				}

				else if (rawValue instanceof Collection)
					replacedValue = CommonCore.join((Collection<?>) rawValue);

				else if (rawValue instanceof Enum[])
					replacedValue = CommonCore.join((Enum[]) rawValue);

				else if (rawValue instanceof Boolean)
					replacedValue = rawValue.toString();

				else if (rawValue instanceof UUID)
					replacedValue = rawValue.toString();

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
				String value = javascriptVariable.buildLegacy(this.audience, this.placeholders);

				if (value != null) {
					if (this.toLegacyMode == ToLegacyMode.MINI) {
						// Keep as is, support all

					} else {
						if (this.toLegacyMode == ToLegacyMode.PLAIN)
							value = SimpleComponent.fromMiniAmpersand(value).toPlain();

						else
							value = CompChatColor.convertMiniToLegacy(value); // No gradient support, they will simply be lost
					}

					replacedValue = value;
				}
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

	/**
	 * Replace the [item] style variables in the given component.
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent replaceMessageVariables(SimpleComponent component) {
		component = this.replaceMessageVariables0(component);

		if (doubleParse)
			component = this.replaceMessageVariables0(component);

		return component;
	}

	/*
	 * Implementation for replacing [item] and the like variables.
	 */
	private SimpleComponent replaceMessageVariables0(final SimpleComponent component) {
		return component.replaceMatch(Variables.MESSAGE_VARIABLE_PATTERN, (match, input) -> {
			final String key = match.group(1);
			final Variable variable = Variable.findVariableByKey(key, Variable.Type.MESSAGE);

			return variable != null ? variable.build(this.audience, this.placeholders).toAdventure(null) : input;
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Register a new expansion if it was not already registered.
	 *
	 * @param expansion
	 */
	public static void addExpansion(final SimpleExpansion expansion) {
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
	public static Variables builder(@Nullable final FoundationPlayer audience) {
		return new Variables().audience(audience);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The mode for converting component to legacy text.
	 */
	public enum ToLegacyMode {
		/**
		 * Convert component to legacy text.
		 */
		LEGACY,

		/**
		 * Convert component to plain text.
		 */
		PLAIN,

		/**
		 * Convert component to mini message.
		 */
		MINI;
	}
}
