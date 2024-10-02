package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find singular [syntax_name] variables.
	 */
	public static final Pattern MESSAGE_VARIABLE_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

	/**
	 * The pattern to find simple {syntax} placeholders.
	 */
	public static final Pattern BRACKET_VARIABLE_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	/**
	 * The pattern to find simple {syntax} placeholders starting with {rel_} (used for PlaceholderAPI)
	 */
	public static final Pattern BRACKET_REL_VARIABLE_PATTERN = Pattern.compile("[({)](rel_)([^}]+)[(})]");

	/**
	 * If we should replace JavaScript variables
	 */
	@Getter(value = AccessLevel.PACKAGE)
	@Setter(value = AccessLevel.PACKAGE)
	private static boolean replaceScript = true;

	/**
	 * If PlaceholderAPI is installed.
	 *
	 * @deprecated oh for a better way to do this
	 */
	@Deprecated
	private static Boolean hasPlaceholderAPI = null;

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * This is used to dynamically replace the variable based on its content, like
	 * PlaceholderAPI.
	 *
	 * We also hook into PlaceholderAPI, however, you'll have to use your plugin's prefix before
	 * all variables when called from there.
	 */
	@Getter
	private static final Set<SimpleExpansion> expansions = new HashSet<>();

	/**
	 * Registers a new expansion if it was not already registered
	 *
	 * @param expansion
	 */
	public static void addExpansion(SimpleExpansion expansion) {
		expansions.add(expansion);
	}

	/**
	 * Set the collector to collect variables for the specified audience
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	@Setter
	private static Collector collector = null;

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replace variables in the messages.
	 *
	 * @see #replace(String, FoundationPlayer)
	 *
	 * @param list
	 * @param audience
	 * @param replacements
	 * @return
	 */
	public static List<String> replaceListArray(List<String> list, FoundationPlayer audience, Map<String, Object> replacements) {
		return Arrays.asList(replaceListArray(list.toArray(new String[list.size()]), audience, replacements));
	}

	/**
	 * Replace variables in the messages.
	 *
	 * @see #replace(String, FoundationPlayer)
	 *
	 * @param list
	 * @param audience
	 * @param replacements
	 * @return
	 */
	public static String[] replaceListArray(String[] list, FoundationPlayer audience, Map<String, Object> replacements) {
		return replace(String.join("%FLPV%", list), audience, replacements).split("%FLPV%");
	}

	/**
	 * Replace variables in the message for the given audience.
	 *
	 * PlaceholderAPI is supported.
	 *
	 * This method substitutes placeholders and variables with corresponding values from various sources,
	 * such as predefined strings, player information, and configurations.
	 *
	 * For example, it could replace the variable "prefix_warn" with a warning prefix like "[WARN]"
	 * or replace built-in placeholders like server name or formatted dates.
	 *
	 * To add custom variables, see {@link #addExpansion(SimpleExpansion)}
	 *
	 * @param message
	 * @param audience
	 * @return
	 */
	public static String replace(String message, FoundationPlayer audience) {
		return replace(message, audience, new HashMap<>());
	}

	/**
	 * Replace variables in the message for the given audience, adding the extra variables in the replacements map.
	 *
	 * PlaceholderAPI is supported.
	 *
	 * This method substitutes placeholders and variables with corresponding values from various sources,
	 * such as predefined strings, player information, and configurations.
	 *
	 * For example, it could replace the variable "prefix_warn" with a warning prefix like "[WARN]"
	 * or replace built-in placeholders like server name or formatted dates.
	 *
	 * To add custom variables, see {@link #addExpansion(SimpleExpansion)}
	 *
	 * @param message
	 * @param audience
	 * @param replacements
	 * @return
	 */
	public static String replace(String message, FoundationPlayer audience, Map<String, Object> replacements) {
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
				value = replaceVariable(variable, audience, replacements);

				if (value != null)
					variableCache.put(variable, value);
			}

			if (value != null)
				result.append(value.toLegacy());
			else
				result.append(matcher.group());

			lastMatchEnd = matcher.end();
		}

		result.append(message.substring(lastMatchEnd));
		return result.toString();
	}

	/**
	 * Replace variables in the message for the given audience.
	 *
	 * PlaceholderAPI is supported.
	 *
	 * This method substitutes placeholders and variables with corresponding values from various sources,
	 * such as predefined strings, player information, and configurations.
	 *
	 * For example, it could replace the variable "prefix_warn" with a warning prefix like "[WARN]"
	 * or replace built-in placeholders like server name or formatted dates.
	 *
	 * To add custom variables, see {@link #addExpansion(SimpleExpansion)}
	 *
	 * @param message
	 * @param audience
	 * @return
	 */
	public static SimpleComponent replace(SimpleComponent message, FoundationPlayer audience) {
		return replace(message, audience, new HashMap<>());
	}

	/**
	 * Replace variables in the message for the given audience, adding the extra variables in the replacements map.
	 *
	 * PlaceholderAPI is supported.
	 *
	 * This method substitutes placeholders and variables with corresponding values from various sources,
	 * such as predefined strings, player information, and configurations.
	 *
	 * For example, it could replace the variable "prefix_warn" with a warning prefix like "[WARN]"
	 * or replace built-in placeholders like server name or formatted dates.
	 *
	 * To add custom variables, see {@link #addExpansion(SimpleExpansion)}
	 *
	 * @param message
	 * @param audience
	 * @param replacements
	 * @return
	 */
	public static SimpleComponent replace(SimpleComponent message, FoundationPlayer audience, Map<String, Object> replacements) {
		return message.replaceMatch(BRACKET_VARIABLE_PATTERN, (result, input) -> {
			final String variable = result.group(1);
			final SimpleComponent value = replaceVariable(variable, audience, replacements);

			return value == null ? SimpleComponent.fromPlain(result.group()) : value;
		});
	}

	/*
	 * Replace a given variable with its corresponding value.
	 */
	private static SimpleComponent replaceVariable(String variable, FoundationPlayer audience, @NonNull Map<String, Object> replacements) {
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
				final SimpleComponent value = expansion.replacePlaceholders(audience, params);

				if (value != null) {
					replacedValue = value;

					break;
				}
			}

		if (audience != null && replaceScript) {
			final Variable javascriptKey = Variable.findVariable(variable, Variable.Type.FORMAT);

			if (javascriptKey != null) {
				final SimpleComponent value = javascriptKey.build(audience, replacements);

				if (value != null)
					replacedValue = value;
			}
		}

		if (collector != null) {
			final SimpleComponent collectedVariable = collector.replaceVariable(pluginIdentifier, params, variable, audience);

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
			replacedValue = SimpleComponent.fromPlain(audience != null && audience.isDiscord() ? "true" : "false");

		else if ("sender_is_console".equals(variable))
			replacedValue = SimpleComponent.fromPlain(audience != null && audience.isConsole() ? "true" : "false");

		else if ("sender_is_player".equals(variable))
			replacedValue = SimpleComponent.fromPlain(audience.isPlayer() ? "true" : "false");

		else if ("label".equals(variable)) {
			final SimpleCommandGroup defaultGroup = Platform.getPlugin().getDefaultCommandGroup();

			if (defaultGroup != null)
				replacedValue = SimpleComponent.fromPlain(defaultGroup.getLabel());
		}

		for (final Map.Entry<String, Object> entry : replacements.entrySet()) {
			final String key = entry.getKey();

			if (key.equals(variable)) {
				final Object rawValue = entry.getValue();

				if (rawValue == null)
					return SimpleComponent.empty();

				if (rawValue instanceof SimpleComponent)
					replacedValue = (SimpleComponent) rawValue;

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
	// Classes
	// ------------------------------------------------------------------------------------------------------------

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
