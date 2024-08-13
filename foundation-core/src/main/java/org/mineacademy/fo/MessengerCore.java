package org.mineacademy.fo;

import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.RemainCore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

/**
 * Streamlines the process of sending themed messages to players
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessengerCore {

	/**
	 * Should we use messenger globally such as in commands & listeners?
	 */
	public static boolean ENABLED = true;

	/**
	 * The prefix send while sending info message
	 */
	@Getter
	private static Component infoPrefix = RemainCore.convertLegacyToAdventure("&8[&9i&8]&7");

	/**
	 * The prefix send while sending success message
	 */
	@Getter
	private static Component successPrefix = RemainCore.convertLegacyToAdventure("&8[&2\u2714&8]&7");

	/**
	 * The prefix send while sending warning message
	 */
	@Getter
	private static Component warnPrefix = RemainCore.convertLegacyToAdventure("&8[&6!&8]&6");

	/**
	 * The prefix send while sending error message
	 */
	@Getter
	private static Component errorPrefix = RemainCore.convertLegacyToAdventure("&8[&4\u2715&8]&c");

	/**
	 * The prefix send while sending questions
	 */
	@Getter
	private static Component questionPrefix = RemainCore.convertLegacyToAdventure("&8[&a?&8]&7");

	/**
	 * The prefix send while sending announcements
	 */
	@Getter
	private static Component announcePrefix = RemainCore.convertLegacyToAdventure("&8[&5!&8]&d");

	/**
	 * Set the prefix for info messages
	 *
	 * @param infoPrefix
	 */
	public static void setInfoPrefix(Component infoPrefix) {
		MessengerCore.infoPrefix = infoPrefix;
	}

	/**
	 * Set the prefix for success messages
	 *
	 * @param successPrefix
	 */
	public static void setSuccessPrefix(Component successPrefix) {
		MessengerCore.successPrefix = successPrefix;
	}

	/**
	 * Set the prefix for warning messages
	 *
	 * @param warnPrefix
	 */
	public static void setWarnPrefix(Component warnPrefix) {
		MessengerCore.warnPrefix = warnPrefix;
	}

	/**
	 * Set the prefix for error messages
	 *
	 * @param errorPrefix
	 */
	public static void setErrorPrefix(Component errorPrefix) {
		MessengerCore.errorPrefix = errorPrefix;
	}

	/**
	 * Set the prefix for question messages
	 *
	 * @param questionPrefix
	 */
	public static void setQuestionPrefix(Component questionPrefix) {
		MessengerCore.questionPrefix = questionPrefix;
	}

	/**
	 * Set the prefix for announcement messages
	 *
	 * @param announcePrefix
	 */
	public static void setAnnouncePrefix(Component announcePrefix) {
		MessengerCore.announcePrefix = announcePrefix;
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastInfo(final String message) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastInfo(final Component component) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, infoPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastSuccess(final String message) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastSuccess(final Component component) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, successPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastWarn(final String message) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastWarn(final Component component) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, warnPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastError(final String message) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastError(final Component component) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, errorPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastQuestion(final String message) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastQuestion(final Component component) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, questionPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param message
	 */
	public static void broadcastAnnounce(final String message) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, announcePrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param component
	 */
	public static void broadcastAnnounce(final Component component) {
		for (final Audience online : Platform.getOnlinePlayers())
			tell(online, announcePrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void info(final Audience player, final String message) {
		tell(player, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param player
	 * @param component
	 */
	public static void info(final Audience player, final Component component) {
		tell(player, infoPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void success(final Audience player, final String message) {
		tell(player, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param player
	 * @param component
	 */
	public static void success(final Audience player, final Component component) {
		tell(player, successPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void warn(final Audience player, final String message) {
		tell(player, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param player
	 * @param component
	 */
	public static void warn(final Audience player, final Component component) {
		tell(player, warnPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void error(final Audience player, final String message) {
		tell(player, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param player
	 * @param component
	 */
	public static void error(final Audience player, final Component component) {
		tell(player, errorPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void question(final Audience player, final String message) {
		tell(player, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param player
	 * @param component
	 */
	public static void question(final Audience player, final Component component) {
		tell(player, questionPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param player
	 * @param message
	 */
	public static void announce(final Audience player, final String message) {
		tell(player, announcePrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param player
	 * @param component
	 */
	public static void announce(final Audience player, final Component component) {
		tell(player, announcePrefix, component);
	}

	/*
	 * Internal method to perform the sending
	 */
	private static void tell(final Audience sender, final Component prefix, @NonNull String message) {
		CommonCore.tellNoPrefix(sender, prefix.append(CommonCore.colorize(message)));
	}

	/*
	 * Internal method to perform the sending
	 */
	private static void tell(final Audience sender, final Component prefix, @NonNull Component component) {
		CommonCore.tellNoPrefix(sender, prefix.append(component));
	}
}
