package org.mineacademy.fo;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

/**
 * Streamlines the process of sending themed messages to players
 */
@UtilityClass
public class Messenger {

	/**
	 * Should we use messenger globally such as in commands & listeners?
	 */
	public static boolean ENABLED = true;

	/**
	 * The prefix send while sending info message
	 */
	@Setter
	@Getter
	private String infoPrefix = "&8&l[&9&li&8&l]&7 ";

	/**
	 * The prefix send while sending success message
	 */
	@Setter
	@Getter
	private String successPrefix = "&8&l[&2&l\u2714&8&l]&7 ";

	/**
	 * The prefix send while sending warning message
	 */
	@Setter
	@Getter
	private String warnPrefix = "&8&l[&6&l!&8&l]&6 ";

	/**
	 * The prefix send while sending error message
	 */
	@Setter
	@Getter
	private String errorPrefix = "&8&l[&4&l\u2715&8&l]&c ";

	/**
	 * The prefix send while sending questions
	 */
	@Setter
	@Getter
	private String questionPrefix = "&8&l[&a&l?&l&8]&7 ";

	/**
	 * The prefix send while sending announcements
	 */
	@Setter
	@Getter
	private String announcePrefix = "&8&l[&5&l!&l&8]&d ";

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param message
	 */
	public void broadcastInfo(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param message
	 */
	public void broadcastSuccess(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param message
	 */
	public void broadcastWarn(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param message
	 */
	public void broadcastError(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param message
	 */
	public void broadcastQuestion(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param message
	 */
	public void broadcastAnnounce(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, announcePrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public void info(final CommandSender player, final String message) {
		tell(player, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public void success(final CommandSender player, final String message) {
		tell(player, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public void warn(final CommandSender player, final String message) {
		tell(player, warnPrefix, message);
	}

	/**
	 * Send messages prepended with the {@link #getErrorPrefix()}
	 *
	 * @param player
	 * @param messages
	 */
	public void error(final CommandSender player, final String... messages) {
		for (final String message : messages)
			error(player, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public void error(final CommandSender player, final String message) {
		tell(player, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public void question(final CommandSender player, final String message) {
		tell(player, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public void announce(final CommandSender player, final String message) {
		tell(player, announcePrefix, message);
	}

	/*
	 * Internal method to perform the sending
	 */
	private void tell(final CommandSender player, final String prefix, String message) {

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
		return Replacer.replaceArray(message,
				"plugin_prefix", SimpleSettings.PLUGIN_PREFIX,
				"info_prefix", infoPrefix,
				"prefix_info", infoPrefix,
				"success_prefix", successPrefix,
				"prefix_success", successPrefix,
				"warn_prefix", warnPrefix,
				"prefix_warn", warnPrefix,
				"error_prefix", errorPrefix,
				"prefix_error", errorPrefix,
				"question_prefix", questionPrefix,
				"prefix_question", questionPrefix,
				"announce_prefix", announcePrefix,
				"prefix_announce", announcePrefix);
	}
}
