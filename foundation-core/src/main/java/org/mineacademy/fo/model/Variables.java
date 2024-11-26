package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleSettings;

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
	 * Variables added to Foundation by you or other plugins
	 *
	 * This is used to dynamically replace the variable based on its content, like
	 * PlaceholderAPI.
	 *
	 * We also hook into PlaceholderAPI, however, you'll have to use your plugin's prefix before
	 * all variables when called from there.
	 */
	private static final Set<SimpleExpansion> expansions = new HashSet<>();

	/**
	 * Set the collector to collect variables for the specified audience
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	private static List<Collector> collectors = new ArrayList<>(); // TODO merge with expansions

	/**
	 * If PlaceholderAPI is installed to replace its placeholders within.
	 *
	 * @deprecated oh for a better way to do this
	 */
	@Deprecated
	private static Boolean hasPlaceholderAPI = null;

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

		// Cache for repeated variable lookups to avoid redundant processing.
		final HashMap<String, SimpleComponent> variableCache = new HashMap<>();

		while (matcher.find()) {
			final String variable = matcher.group(1);

			result.append(message, lastMatchEnd, matcher.start());

			SimpleComponent value;

			if (variableCache.containsKey(variable))
				value = variableCache.get(variable);
			else {
				value = this.replaceVariable(variable);

				if (value != null)
					variableCache.put(variable, value);
			}

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

		// Needed for the PlaceholderAPI split
		final int index = variable.indexOf("_");
		String pluginIdentifier = "";
		String params = "";

		if (!(index <= 0 || index >= variable.length())) {
			pluginIdentifier = variable.substring(0, index).toLowerCase();

			params = variable.substring(index + 1);
		}

		if (hasPlaceholderAPI == null)
			hasPlaceholderAPI = Platform.isPluginInstalled("PlaceholderAPI");

		// If PlaceholderAPI is installed, the replaced below uses it
		if (!hasPlaceholderAPI)
			for (final SimpleExpansion expansion : expansions) {
				final SimpleComponent value = expansion.replacePlaceholders(this.audience, params);

				if (value != null) {
					replacedValue = value;

					break;
				}
			}

		if (this.audience != null && replaceScript) {
			final Variable javascriptKey = Variable.findVariable(variable, Variable.Type.FORMAT);

			if (javascriptKey != null) {
				final SimpleComponent value = javascriptKey.build(this.audience, this.placeholders);

				if (value != null)
					replacedValue = value;
			}
		}

		for (final Collector collector : collectors) {
			final SimpleComponent collectedVariable = collector.replaceVariable(pluginIdentifier, params, variable, this.audience);

			if (collectedVariable != null)
				replacedValue = collectedVariable;
		}

		if ("prefix_plugin".equals(variable))
			replacedValue = SimpleSettings.PREFIX;

		else if ("prefix_info".equals(variable))
			replacedValue = Messenger.getInfoPrefix();

		else if ("prefix_success".equals(variable))
			replacedValue = Messenger.getSuccessPrefix();

		else if ("prefix_warn".equals(variable))
			replacedValue = Messenger.getWarnPrefix();

		else if ("prefix_error".equals(variable))
			replacedValue = Messenger.getErrorPrefix();

		else if ("prefix_question".equals(variable))
			replacedValue = Messenger.getQuestionPrefix();

		else if ("prefix_announce".equals(variable))
			replacedValue = Messenger.getAnnouncePrefix();

		else if ("server_name".equals(variable))
			replacedValue = Platform.hasCustomServerName() ? SimpleComponent.fromPlain(Platform.getCustomServerName()) : SimpleComponent.empty();

		else if ("date".equals(variable))
			replacedValue = SimpleComponent.fromPlain(TimeUtil.getFormattedDate());

		else if ("date_short".equals(variable))
			replacedValue = SimpleComponent.fromPlain(TimeUtil.getFormattedDateShort());

		else if ("date_month".equals(variable))
			replacedValue = SimpleComponent.fromPlain(TimeUtil.getFormattedDateMonth());

		else if ("chat_line".equals(variable))
			replacedValue = SimpleComponent.fromPlain(CommonCore.chatLine());

		else if ("chat_line_smooth".equals(variable))
			replacedValue = SimpleComponent.fromSection(CommonCore.chatLineSmooth());

		else if ("sender_is_discord".equals(variable))
			replacedValue = SimpleComponent.fromPlain(this.audience != null && this.audience.isDiscord() ? "true" : "false");

		else if ("sender_is_console".equals(variable))
			replacedValue = SimpleComponent.fromPlain(this.audience != null && this.audience.isConsole() ? "true" : "false");

		else if ("sender_is_player".equals(variable))
			replacedValue = SimpleComponent.fromPlain(this.audience.isPlayer() ? "true" : "false");

		else if ("label".equals(variable)) {
			final SimpleCommandGroup defaultGroup = Platform.getPlugin().getDefaultCommandGroup();

			if (defaultGroup != null)
				replacedValue = SimpleComponent.fromPlain(defaultGroup.getLabel());
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
	}

	/**
	 * Return all registered expansions. The list is mutable.
	 *
	 * @return
	 */
	public static Set<SimpleExpansion> getExpansions() {
		return expansions;
	}

	/**
	 * Add a collector to collect variables for an audience.
	 *
	 * @param collector
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void addCollector(@NonNull Collector collector) {
		collectors.add(collector);
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

	/**
	 * Collects variables for the specified audience.
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public interface Collector {
		SimpleComponent replaceVariable(String plugin, String params, String variable, FoundationPlayer audience);
	}
}
