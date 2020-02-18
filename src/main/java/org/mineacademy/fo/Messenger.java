package org.mineacademy.fo;

import org.bukkit.command.CommandSender;

import lombok.experimental.UtilityClass;

/**
 * Streamlines the process of sending themed messages to players
 */
@UtilityClass
public class Messenger {

	/**
	 * The prefix send while sending info message
	 */
	public String INFO_PREFIX = "&8&l[&9&li&8&l] &7";

	/**
	 * The prefix send while sending success message
	 */
	public String SUCCESS_PREFIX = "&8&l[&2&l\u2714&8&l] &7";

	/**
	 * The prefix send while sending warning message
	 */
	public String WARN_PREFIX = "&8&l[&6&l!&8&l] &6";

	/**
	 * The prefix send while sending error message
	 */
	public String ERROR_PREFIX = "&8&l[&4&l\u2715&8&l] &c";

	/**
	 * The prefix send while sending questions
	 */
	public String QUESTION_PREFIX = "&8&l[&a&l?&l&8] &7";

	/**
	 * The prefix send while sending announcements
	 */
	public String ANNOUNCE_PREFIX = "&8&l[&5&l!&l&8] &d";

	/**
	 * Send a message prepended with the {@link #INFO_PREFIX}
	 *
	 * @param player
	 * @param message
	 */
	public void info(final CommandSender player, final String message) {
		tell(player, INFO_PREFIX, message);
	}

	/**
	 * Send a message prepended with the {@link #SUCCESS_PREFIX}
	 *
	 * @param player
	 * @param message
	 */
	public void success(final CommandSender player, final String message) {
		tell(player, SUCCESS_PREFIX, message);
	}

	/**
	 * Send a message prepended with the {@link #WARN_PREFIX}
	 *
	 * @param player
	 * @param message
	 */
	public void warn(final CommandSender player, final String message) {
		tell(player, WARN_PREFIX, message);
	}

	/**
	 * Send a message prepended with the {@link #ERROR_PREFIX}
	 *
	 * @param player
	 * @param message
	 */
	public void error(final CommandSender player, final String message) {
		tell(player, ERROR_PREFIX, message);
	}

	/**
	 * Send a message prepended with the {@link #QUESTION_PREFIX}
	 *
	 * @param player
	 * @param message
	 */
	public void question(final CommandSender player, final String message) {
		tell(player, QUESTION_PREFIX, message);
	}

	/**
	 * Send a message prepended with the {@link #ANNOUNCE_PREFIX}
	 *
	 * @param player
	 * @param message
	 */
	public void announce(final CommandSender player, final String message) {
		tell(player, ANNOUNCE_PREFIX, message);
	}

	/*
	 * Internal method to perform the sending
	 */
	private void tell(final CommandSender player, final String prefix, final String message) {
		Common.tellNoPrefix(player, prefix + message);
	}
}
