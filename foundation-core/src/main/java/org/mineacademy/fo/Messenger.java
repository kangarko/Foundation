package org.mineacademy.fo;

import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for sending messages with different prefixes specified in {@link Lang}.
 *
 * This is a platform-neutral class, which is extended by "Messenger" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Messenger {

	// ----------------------------------------------------------------------------------------------------
	// Prefixes
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Get the prefix used for success messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-success'.
	 *
	 * @see Lang#component(String)
	 * @return
	 */
	public static SimpleComponent getSuccessPrefix() {
		return Lang.component("prefix-success");
	}

	/**
	 * Get the prefix used for information messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-info'.
	 *
	 * @see Lang#component(String)
	 * @return
	 */
	public static SimpleComponent getInfoPrefix() {
		return Lang.component("prefix-info");
	}

	/**
	 * Get the prefix used for warning messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-warn'.
	 *
	 * @see Lang#component(String)
	 * @return
	 */
	public static SimpleComponent getWarnPrefix() {
		return Lang.component("prefix-warn");
	}

	/**
	 * Get the prefix used for error messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-error'.
	 *
	 * @see Lang#component(String)
	 * @return
	 */
	public static SimpleComponent getErrorPrefix() {
		return Lang.component("prefix-error");
	}

	/**
	 * Get the prefix used for questions.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-question'.
	 *
	 * @see Lang#component(String)
	 * @return
	 */
	public static SimpleComponent getQuestionPrefix() {
		return Lang.component("prefix-question");
	}

	/**
	 * Get the prefix used for announcements.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-announce'.
	 *
	 * @see Lang#component(String)
	 * @return
	 */
	public static SimpleComponent getAnnouncePrefix() {
		return Lang.component("prefix-announce");
	}

	// ----------------------------------------------------------------------------------------------------
	// Broadcasting
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Broadcast a message to online players prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param message
	 */
	public static void broadcastInfo(final String message) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			info(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 */
	public static void broadcastInfo(final SimpleComponent component) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			info(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param message
	 */
	public static void broadcastSuccess(final String message) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			success(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 */
	public static void broadcastSuccess(final SimpleComponent component) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			success(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param message
	 */
	public static void broadcastWarn(final String message) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			warn(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 */
	public static void broadcastWarn(final SimpleComponent component) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			warn(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param message
	 */
	public static void broadcastError(final String message) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			error(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 */
	public static void broadcastError(final SimpleComponent component) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			error(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param message
	 */
	public static void broadcastQuestion(final String message) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			question(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 */
	public static void broadcastQuestion(final SimpleComponent component) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			question(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param message
	 */
	public static void broadcastAnnounce(final String message) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			announce(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 */
	public static void broadcastAnnounce(final SimpleComponent component) {
		for (final FoundationPlayer online : Platform.getOnlinePlayers())
			announce(online, component);
	}

	// ----------------------------------------------------------------------------------------------------
	// Telling by converting the object into a FoundationPlayer
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void info(final T sender, final String message) {
		info(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void info(final T sender, final SimpleComponent component) {
		info(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void success(final T sender, final String message) {
		success(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void success(final T sender, final SimpleComponent component) {
		success(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void warn(final T sender, final String message) {
		warn(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void warn(final T sender, final SimpleComponent component) {
		warn(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void error(final T sender, final String message) {
		error(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void error(final T sender, final SimpleComponent component) {
		error(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void question(final T sender, final String message) {
		question(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void question(final T sender, final SimpleComponent component) {
		question(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void announce(final T sender, final String message) {
		announce(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void announce(final T sender, final SimpleComponent component) {
		announce(Platform.toPlayer(sender), component);
	}

	// ----------------------------------------------------------------------------------------------------
	// Telliung
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void info(final FoundationPlayer audience, final String message) {
		tell(audience, getInfoPrefix(), message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void info(final FoundationPlayer audience, final SimpleComponent component) {
		tell(audience, getInfoPrefix(), component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void success(final FoundationPlayer audience, final String message) {
		tell(audience, getSuccessPrefix(), message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void success(final FoundationPlayer audience, final SimpleComponent component) {
		tell(audience, getSuccessPrefix(), component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void warn(final FoundationPlayer audience, final String message) {
		tell(audience, getWarnPrefix(), message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void warn(final FoundationPlayer audience, final SimpleComponent component) {
		tell(audience, getWarnPrefix(), component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void error(final FoundationPlayer audience, final String message) {
		tell(audience, getErrorPrefix(), message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void error(final FoundationPlayer audience, final SimpleComponent component) {
		tell(audience, getErrorPrefix(), component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void question(final FoundationPlayer audience, final String message) {
		tell(audience, getQuestionPrefix(), message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void question(final FoundationPlayer audience, final SimpleComponent component) {
		tell(audience, getQuestionPrefix(), component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void announce(final FoundationPlayer audience, final String message) {
		tell(audience, getAnnouncePrefix(), message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void announce(final FoundationPlayer audience, final SimpleComponent component) {
		tell(audience, getAnnouncePrefix(), component);
	}

	/*
	 * Send a message to the player with the given prefix.
	 */
	private static void tell(final FoundationPlayer audience, final SimpleComponent prefix, @NonNull String message) {
		tell(audience, prefix, SimpleComponent.fromMini(message));
	}

	/*
	 * Send a message to the player with the given prefix.
	 */
	private static void tell(final FoundationPlayer audience, final SimpleComponent prefix, @NonNull SimpleComponent component) {
		final String plain = component.toPlain();

		if (!plain.isEmpty() && !plain.equals("none"))
			audience.sendMessage(prefix.appendPlain(" ").append(component));
	}
}
