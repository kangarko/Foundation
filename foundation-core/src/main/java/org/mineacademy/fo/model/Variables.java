package org.mineacademy.fo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MessengerCore;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.RemainCore;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

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
	 * Player - [Original Message - Translated Message]
	 */
	private static final Map<String, Map<String, String>> cache = ExpiringMap.builder().expiration(500, TimeUnit.MILLISECONDS).build();

	/**
	 * If we should replace JavaScript variables
	 */
	@Getter
	@Setter
	private static boolean replaceScript = true;

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
	private static final StrictList<SimpleExpansion> expansions = new StrictList<>();

	/**
	 * Registers a new expansion if it was not already registered
	 *
	 * @param expansion
	 */
	public static void addExpansion(SimpleExpansion expansion) {
		expansions.addIfNotExist(expansion);
	}

	/**
	 * Set the collector to collect variables for the specified audience
	 */
	@Setter
	private static Collector collector = null;

	/**
	 * Collects variables for the specified audience
	 */
	public interface Collector {
		void addVariables(String variable, Audience audience, Map<String, Object> replacements);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	public static String replace(String message, Audience audience) {
		return replace(message, audience, new HashMap<>());
	}

	public static String replace(String message, Audience audience, Map<String, Object> replacements) {
		final Matcher matcher = BRACKET_VARIABLE_PATTERN.matcher(message);

		while (matcher.find()) {
			final String variable = matcher.group();
			final String value = replaceVariable(variable, audience, replacements);

			if (value != null)
				message = message.replace(variable, CommonCore.colorizeLegacy(value));
		}

		return message;
	}

	public static Component replace(Component message, Audience audience) {
		return replace(message, audience, new HashMap<>());
	}

	public static Component replace(Component message, Audience audience, Map<String, Object> replacements) {
		return message.replaceText(b -> b.match(BRACKET_VARIABLE_PATTERN).replacement((result, input) -> {
			final String variable = result.group();
			final String value = replaceVariable(variable, audience, replacements);

			return value == null ? Component.empty() : CommonCore.colorize(value);
		}));
	}

	// TODO readd cache
	private static String replaceVariable(String variable, Audience audience, @NonNull Map<String, Object> replacements) {
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

		// Replace custom expansions
		if (audience != null && !Platform.isPlaceholderAPIHooked()) // TODO test if it works with PAPI still
			for (final SimpleExpansion expansion : expansions) {
				final String value = expansion.replacePlaceholders(audience, variable);

				if (value != null)
					return value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : "");
			}

		replacements.put("prefix", SimpleSettings.PLUGIN_PREFIX);
		replacements.put("plugin_prefix", SimpleSettings.PLUGIN_PREFIX);
		replacements.put("info", MessengerCore.getInfoPrefix());
		replacements.put("info_prefix", MessengerCore.getInfoPrefix());
		replacements.put("prefix_info", MessengerCore.getInfoPrefix());
		replacements.put("success", MessengerCore.getSuccessPrefix());
		replacements.put("success_prefix", MessengerCore.getSuccessPrefix());
		replacements.put("prefix_success", MessengerCore.getSuccessPrefix());
		replacements.put("warn", MessengerCore.getWarnPrefix());
		replacements.put("warn_prefix", MessengerCore.getWarnPrefix());
		replacements.put("prefix_warn", MessengerCore.getWarnPrefix());
		replacements.put("error", MessengerCore.getErrorPrefix());
		replacements.put("error_prefix", MessengerCore.getErrorPrefix());
		replacements.put("prefix_error", MessengerCore.getErrorPrefix());
		replacements.put("question", MessengerCore.getQuestionPrefix());
		replacements.put("question_prefix", MessengerCore.getQuestionPrefix());
		replacements.put("prefix_question", MessengerCore.getQuestionPrefix());
		replacements.put("announce", MessengerCore.getAnnouncePrefix());
		replacements.put("announce_prefix", MessengerCore.getAnnouncePrefix());
		replacements.put("prefix_announce", MessengerCore.getAnnouncePrefix());
		replacements.put("server", RemainCore.getServerName());
		replacements.put("server_name", RemainCore.getServerName());
		replacements.put("date", TimeUtil.getFormattedDate());
		replacements.put("date_short", TimeUtil.getFormattedDateShort());
		replacements.put("date_month", TimeUtil.getFormattedDateMonth());
		replacements.put("chat_line", CommonCore.chatLine());
		replacements.put("chat_line_smooth", CommonCore.chatLineSmooth());
		replacements.put("label", Platform.getDefaultCommandLabel() != null ? Platform.getDefaultCommandLabel() : SimpleLocalization.NONE);

		replacements.put("sender_is_discord", Platform.isDiscord(audience) ? "true" : "false");
		replacements.put("sender_is_console", Platform.isConsole(audience) ? "true" : "false");

		// Replace JavaScript variables
		if (replaceScript) {
			final Variable javascriptKey = Variable.findVariable(variable, Variable.Type.FORMAT);

			if (javascriptKey != null) {
				final String value = javascriptKey.buildPlain(audience, replacements);

				// And we remove the white prefix that is by default added in every component
				// TODO Test if still needed
				//if (value.startsWith(CompChatColor.COLOR_CHAR + "f" + CompChatColor.COLOR_CHAR + "f"))
				//	value = value.substring(4);

				replacements.put(variable, value);
			}
		}

		if (collector != null)
			collector.addVariables(variable, audience, replacements);

		// Finally, do replace
		for (final Map.Entry<String, Object> entry : replacements.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue() != null ? entry.getValue().toString() : "";

			ValidCore.checkBoolean(!key.startsWith("{"), "Variable key cannot start with {, found: " + key);
			ValidCore.checkBoolean(!key.endsWith("}"), "Variable key cannot end with }, found: " + key);

			if (key.equals(variable))
				return value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : "");
		}

		return null;
	}
}
