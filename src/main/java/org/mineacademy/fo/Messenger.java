package org.mineacademy.fo;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.remain.Remain;

import lombok.Setter;
import lombok.experimental.UtilityClass;

/**
 * Streamlines the process of sending themed messages to players
 */
@UtilityClass
public class Messenger {

	/**
	 * The prefix send while sending info message
	 */
	@Setter
	private String infoPrefix = "&8&l[&9&li&8&l] &7";

	/**
	 * The prefix send while sending success message
	 */
	@Setter
	private String successPrefix = "&8&l[&2&l\u2714&8&l] &7";

	/**
	 * The prefix send while sending warning message
	 */
	@Setter
	private String warnPrefix = "&8&l[&6&l!&8&l] &6";

	/**
	 * The prefix send while sending error message
	 */
	@Setter
	private String errorPrefix = "&8&l[&4&l\u2715&8&l] &c";

	/**
	 * The prefix send while sending questions
	 */
	@Setter
	private String questionPrefix = "&8&l[&a&l?&l&8] &7";

	/**
	 * The prefix send while sending announcements
	 */
	@Setter
	private String announcePrefix = "&8&l[&5&l!&l&8] &d";

	/**
	 * Send a message prepended with the {@link #infoPrefix}
	 *
	 * @param message
	 */
	public void broadcastInfo(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #successPrefix}
	 *
	 * @param message
	 */
	public void broadcastSuccess(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #warnPrefix}
	 *
	 * @param message
	 */
	public void broadcastWarn(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #errorPrefix}
	 *
	 * @param message
	 */
	public void broadcastError(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #questionPrefix}
	 *
	 * @param message
	 */
	public void broadcastQuestion(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #announcePrefix}
	 *
	 * @param message
	 */
	public void broadcastAnnounce(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, announcePrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #infoPrefix}
	 *
	 * @param player
	 * @param message
	 */
	public void info(final CommandSender player, final String message) {
		tell(player, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #successPrefix}
	 *
	 * @param player
	 * @param message
	 */
	public void success(final CommandSender player, final String message) {
		tell(player, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #warnPrefix}
	 *
	 * @param player
	 * @param message
	 */
	public void warn(final CommandSender player, final String message) {
		tell(player, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #errorPrefix}
	 *
	 * @param player
	 * @param message
	 */
	public void error(final CommandSender player, final String message) {
		tell(player, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #questionPrefix}
	 *
	 * @param player
	 * @param message
	 */
	public void question(final CommandSender player, final String message) {
		tell(player, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #announcePrefix}
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
	private void tell(final CommandSender player, final String prefix, final String message) {
		Common.tellNoPrefix(player, prefix + message);
	}
}
