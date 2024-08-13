package org.mineacademy.fo;

import org.bukkit.command.CommandSender;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Messenger extends MessengerCore {

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void info(final CommandSender sender, final String message) {
		info(Platform.toAudience(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param sender
	 * @param component
	 */
	public static void info(final CommandSender sender, final Component component) {
		info(Platform.toAudience(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void success(final CommandSender sender, final String message) {
		success(Platform.toAudience(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param sender
	 * @param component
	 */
	public static void success(final CommandSender sender, final Component component) {
		success(Platform.toAudience(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void warn(final CommandSender sender, final String message) {
		warn(Platform.toAudience(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param sender
	 * @param component
	 */
	public static void warn(final CommandSender sender, final Component component) {
		warn(Platform.toAudience(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void error(final CommandSender sender, final String message) {
		error(Platform.toAudience(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param sender
	 * @param component
	 */
	public static void error(final CommandSender sender, final Component component) {
		error(Platform.toAudience(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void question(final CommandSender sender, final String message) {
		question(Platform.toAudience(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param sender
	 * @param component
	 */
	public static void question(final CommandSender sender, final Component component) {
		question(Platform.toAudience(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param sender
	 * @param message
	 */
	public static void announce(final CommandSender sender, final String message) {
		announce(Platform.toAudience(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param sender
	 * @param component
	 */
	public static void announce(final CommandSender sender, final Component component) {
		announce(Platform.toAudience(sender), component);
	}
}
