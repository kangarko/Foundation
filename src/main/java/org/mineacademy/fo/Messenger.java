package org.mineacademy.fo;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Streamlines the process of sending themed messages to players
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Messenger {

	/**
	 * Should we use messenger globally such as in commands & listeners?
	 */
	public static boolean ENABLED = true;

	/**
	 * Cached list of replacements
	 */
	private static final Map<String, String> replacements = new HashMap<>();

	/**
	 * The prefix send while sending info message
	 */
	@Getter
	private static String infoPrefix = "&8&l[&9&li&8&l]&7 ";

	/**
	 * The prefix send while sending success message
	 */
	@Getter
	private static String successPrefix = "&8&l[&2&l\u2714&8&l]&7 ";

	/**
	 * The prefix send while sending warning message
	 */
	@Getter
	private static String warnPrefix = "&8&l[&6&l!&8&l]&6 ";

	/**
	 * The prefix send while sending error message
	 */
	@Getter
	private static String errorPrefix = "&8&l[&4&l\u2715&8&l]&c ";

	/**
	 * The prefix send while sending questions
	 */
	@Getter
	private static String questionPrefix = "&8&l[&a&l?&l&8]&7 ";

	/**
	 * The prefix send while sending announcements
	 */
	@Getter
	private static String announcePrefix = "&8&l[&5&l!&l&8]&d ";

	/**
	 * Set the prefix for info messages
	 *
	 * @param infoPrefix
	 */
	public static void setInfoPrefix(String infoPrefix) {
		Messenger.infoPrefix = infoPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for success messages
	 *
	 * @param successPrefix
	 */
	public static void setSuccessPrefix(String successPrefix) {
		Messenger.successPrefix = successPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for warning messages
	 *
	 * @param warnPrefix
	 */
	public static void setWarnPrefix(String warnPrefix) {
		Messenger.warnPrefix = warnPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for error messages
	 *
	 * @param errorPrefix
	 */
	public static void setErrorPrefix(String errorPrefix) {
		Messenger.errorPrefix = errorPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for question messages
	 *
	 * @param questionPrefix
	 */
	public static void setQuestionPrefix(String questionPrefix) {
		Messenger.questionPrefix = questionPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for announcement messages
	 *
	 * @param announcePrefix
	 */
	public static void setAnnouncePrefix(String announcePrefix) {
		Messenger.announcePrefix = announcePrefix;

		updatePrefixes();
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastInfo(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastSuccess(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastWarn(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastError(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastQuestion(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param message
	 */
	public static void broadcastAnnounce(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, announcePrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void info(final CommandSender player, final String message) {
		tell(player, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void success(final CommandSender player, final String message) {
		tell(player, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void warn(final CommandSender player, final String message) {
		tell(player, warnPrefix, message);
	}

	/**
	 * Send messages prepended with the {@link #getErrorPrefix()}
	 *
	 * @param player
	 * @param messages
	 */
	public static void error(final CommandSender player, final String... messages) {
		for (final String message : messages)
			error(player, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void error(final CommandSender player, final String message) {
		tell(player, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void question(final CommandSender player, final String message) {
		tell(player, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void announce(final CommandSender player, final String message) {
		tell(player, announcePrefix, message);
	}

	/*
	 * Internal method to perform the sending
	 */
	private static void tell(final CommandSender player, final String prefix, String message) {

		// Support localization being none or empty
		if (message.isEmpty() || "none".equals(message))
			return;

		final String colorless = Common.stripColors(message);
		boolean noPrefix = ChatUtil.isInteractive(colorless);

		// Special case: Send the prefix for actionbar
		if (colorless.startsWith("<actionbar>"))
			message = message.replace("<actionbar>", "<actionbar>" + prefix);

		if (colorless.startsWith("@noprefix")) {
			message = message.replace("@noprefix", "");

			noPrefix = true;
		}

		// Only insert prefix if the message is sent through the normal chat
		Common.tellNoPrefix(player, (noPrefix ? "" : prefix) + message);
	}

	/**
	 * Replace {plugin_prefix} and {X_prefix} and {prefix_X} with respective messenger variables
	 * such as {warn_prefix} with {@link #getWarnPrefix()} etc.
	 *
	 * @param message
	 * @return
	 */
	public static String replacePrefixes(String message) {
		final StringBuilder result = new StringBuilder(message);

		replacements.forEach((key, value) -> {
			int start = result.indexOf(key);

			while (start != -1) {
				result.replace(start, start + key.length(), value);

				start = result.indexOf(key, start + value.length());
			}
		});

		return result.toString();
	}

	/*
	 * Refresh prefixes for maximum performance (replacing takes <0.002ms per call in production (!) testing)
	 */
	private static void updatePrefixes() {
		replacements.put("{plugin_prefix}", SimpleSettings.PLUGIN_PREFIX);
		replacements.put("{info_prefix}", infoPrefix);
		replacements.put("{prefix_info}", infoPrefix);
		replacements.put("{success_prefix}", successPrefix);
		replacements.put("{prefix_success}", successPrefix);
		replacements.put("{warn_prefix}", warnPrefix);
		replacements.put("{prefix_warn}", warnPrefix);
		replacements.put("{error_prefix}", errorPrefix);
		replacements.put("{prefix_error}", errorPrefix);
		replacements.put("{question_prefix}", questionPrefix);
		replacements.put("{prefix_question}", questionPrefix);
		replacements.put("{announce_prefix}", announcePrefix);
		replacements.put("{prefix_announce}", announcePrefix);
	}
}
