package org.mineacademy.fo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * Streamlines the process of sending themed messages to players.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Messenger {

	/**
	 * Should we use messenger globally such as in commands and listeners?
	 */
	public static boolean ENABLED = true;

	/**
	 * The prefix sent before info messages.
	 */
	@Setter
	@Getter
	private static String infoPrefix = "&8&l[&9&li&8&l]&7 ";

	/**
	 * The prefix sent before success messages.
	 */
	@Setter
	@Getter
	private static String successPrefix = "&8&l[&2&l\u2714&8&l]&7 ";

	/**
	 * The prefix sent before warning messages.
	 */
	@Setter
	@Getter
	private static String warnPrefix = "&8&l[&6&l!&8&l]&6 ";

	/**
	 * The prefix sent before error messages.
	 */
	@Setter
	@Getter
	private static String errorPrefix = "&8&l[&4&l\u2715&8&l]&c ";

	/**
	 * The prefix sent before questions.
	 */
	@Setter
	@Getter
	private static String questionPrefix = "&8&l[&a&l?&l&8]&7 ";

	/**
	 * The prefix sent before announcements.
	 */
	@Setter
	@Getter
	private static String announcePrefix = "&8&l[&5&l!&l&8]&d ";

	/**
	 * Broadcasts a message prepended with {@link #getInfoPrefix()}
	 *
	 * @param message the message to broadcast.
	 */
	public static void broadcastInfo(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, infoPrefix, message);
	}

	/**
	 * Broadcasts a message prepended with {@link #getSuccessPrefix()}.
	 *
	 * @param message the message to broadcast.
	 */
	public static void broadcastSuccess(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, successPrefix, message);
	}

	/**
	 * Broadcasts a message prepended with {@link #getWarnPrefix()}.
	 *
	 * @param message the message to broadcast.
	 */
	public static void broadcastWarn(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, warnPrefix, message);
	}

	/**
	 * Broadcasts a message prepended with {@link #getErrorPrefix()}.
	 *
	 * @param message the message to broadcast.
	 */
	public static void broadcastError(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, errorPrefix, message);
	}

	/**
	 * Broadcasts a message prepended with {@link #getQuestionPrefix()}.
	 *
	 * @param message the message to broadcast.
	 */
	public static void broadcastQuestion(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, questionPrefix, message);
	}

	/**
	 * Broadcasts a message prepended with {@link #getAnnouncePrefix()}.
	 *
	 * @param message the message to broadcast.
	 */
	public static void broadcastAnnounce(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, announcePrefix, message);
	}

	/**
	 * Sends a message prepended with {@link #getInfoPrefix()} to the given {@link CommandSender}.
	 *
	 * @param player  the {@link CommandSender} to send the message to.
	 * @param message the message to send.
	 */
	public static void info(final CommandSender player, final String message) {
		tell(player, infoPrefix, message);
	}

	/**
	 * Sends a message prepended with {@link #getSuccessPrefix()} to the given {@link CommandSender}.
	 *
	 * @param player  the {@link CommandSender} to send the message to.
	 * @param message the message to send.
	 */
	public static void success(final CommandSender player, final String message) {
		tell(player, successPrefix, message);
	}

	/**
	 * Sends a message prepended with {@link #getWarnPrefix()} to the given {@link CommandSender}.
	 *
	 * @param player  the {@link CommandSender} to send the message to.
	 * @param message the message to send.
	 */
	public static void warn(final CommandSender player, final String message) {
		tell(player, warnPrefix, message);
	}

	/**
	 * Sends messages prepended with {@link #getErrorPrefix()} to the given {@link CommandSender}.
	 *
	 * @param player   the {@link CommandSender} to send the message to.
	 * @param messages the messages to send.
	 */
	public static void error(final CommandSender player, final String... messages) {
		for (final String message : messages)
			error(player, message);
	}

	/**
	 * Sends a message prepended with {@link #getErrorPrefix()} to the given {@link CommandSender}.
	 *
	 * @param player  the {@link CommandSender} to send the message to.
	 * @param message the message to send.
	 */
	public static void error(final CommandSender player, final String message) {
		tell(player, errorPrefix, message);
	}

	/**
	 * Sends a message prepended with {@link #getQuestionPrefix()} to the given {@link CommandSender}.
	 *
	 * @param player  the {@link CommandSender} to send the message to.
	 * @param message the message to send.
	 */
	public static void question(final CommandSender player, final String message) {
		tell(player, questionPrefix, message);
	}

	/**
	 * Sends a message prepended with {@link #getAnnouncePrefix()} to the given {@link CommandSender}.
	 *
	 * @param player  the {@link CommandSender} to send the message to.
	 * @param message the message to send.
	 */
	public static void announce(final CommandSender player, final String message) {
		tell(player, announcePrefix, message);
	}

	/**
	 * Sends a message to the given {@link CommandSender}.
	 *
	 * @param player  the {@link CommandSender} to send the message to.
	 * @param prefix  the prefix to prepend to the message.
	 * @param message the message to send.
	 */
	private static void tell(final CommandSender player, final String prefix, String message) {

		// Support localization being none or empty
		if (message.isEmpty() || "none".equals(message))
			return;

		final String colorless = Common.stripColors(message);
		boolean noPrefix = ChatUtil.isInteractive(colorless);

		// Special case: Send the prefix for actionbar.
		if (colorless.startsWith("<actionbar>"))
			message = message.replace("<actionbar>", "<actionbar>" + prefix);

		if (colorless.startsWith("@noprefix")) {
			message = message.replace("@noprefix", "");

			noPrefix = true;
		}

		// Only insert the prefix if the message is sent through the normal chat.
		Common.tellNoPrefix(player, (noPrefix ? "" : prefix) + message);
	}

	/**
	 * Replaces {plugin_prefix}, {X_prefix} and {prefix_X} with respective messenger variables such as {warn_prefix}
	 * with {@link #getWarnPrefix()} etc.
	 *
	 * @param message the message to replace prefixes in.
	 * @return the message with prefixes replaced.
	 */
	public static String replacePrefixes(final String message) {
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
