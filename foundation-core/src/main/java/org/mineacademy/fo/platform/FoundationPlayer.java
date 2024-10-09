package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.settings.Lang;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * Similar to Audience in Adventure, the FoundationPlayer represents a platform-neutral
 * audience that can receive messages and interact with the server.
 *
 * To create one, use {@link Platform#toPlayer(Object)} and pass in the familiar object
 * such as a Player or a CommandSender.
 */
public abstract class FoundationPlayer {

	/**
	 * Returns true if the player has the given permission.
	 *
	 * @param permission
	 * @return
	 */
	public final boolean hasPermission(String permission) {
		if (permission.contains("{") || permission.contains("}"))
			throw new FoException("Permission cannot contain variables: " + permission);

		return this.hasPermission0(permission);
	}

	/*
	 * Implementation of hasPermission().
	 */
	protected abstract boolean hasPermission0(String permission);

	/**
	 * Returns true if the player is a player, false if console or command sender.
	 *
	 * @return
	 */
	public abstract boolean isPlayer();

	/**
	 * Returns true if the player is a command sender. For most platforms, Player
	 * is also a command sender.
	 *
	 * @return
	 */
	public abstract boolean isCommandSender();

	/**
	 * Returns true if the player is the console.
	 *
	 * @return
	 */
	public abstract boolean isConsole();

	/**
	 * Returns true if the player is a Discord sender.
	 *
	 * @return
	 */
	public abstract boolean isDiscord();

	/**
	 * Returns the player's name, or the "part-console" lang key if the player is a console.
	 *
	 * @see Lang
	 * @return
	 */
	public final String getName() {
		return this.isConsole() ? Lang.legacy("part-console") : this.getSenderName0();
	}

	/**
	 * Get the player implementation object, such as Player on Bukkit.
	 * Returns null if not applicable.
	 *
	 * @param <T>
	 * @return
	 */
	public abstract <T> T getPlayer();

	/*
	 * Implementation of getName() for players.
	 */
	protected abstract String getSenderName0();

	/**
	 * Runs the given command (without /) as the player, replacing {player} with his name.
	 *
	 * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted
	 * message to playerReplacement directly.
	 *
	 * @param command
	 */
	public final void dispatchCommand(String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		if (command.startsWith("@announce ")) {
			Messenger.announce(this, command.replace("@announce ", ""));
		}

		else if (command.startsWith("@warn ")) {
			Messenger.warn(this, command.replace("@warn ", ""));
		}

		else if (command.startsWith("@error ")) {
			Messenger.error(this, command.replace("@error ", ""));
		}

		else if (command.startsWith("@info ")) {
			Messenger.info(this, command.replace("@info ", ""));
		}

		else if (command.startsWith("@question ")) {
			Messenger.question(this, command.replace("@question ", ""));
		}

		else if (command.startsWith("@success ")) {
			Messenger.success(this, command.replace("@success ", ""));
		}

		else {
			command = command.startsWith("/") && !command.startsWith("//") ? command.substring(1) : command;
			command = Variables.replace(command, this);

			// Workaround for JSON in tellraw getting HEX colors replaced
			if (!command.startsWith("tellraw"))
				command = CompChatColor.translateColorCodes(command);

			if (this.isPlayer())
				this.performPlayerCommand0(command);
			else
				Platform.getPlatform().dispatchConsoleCommand(this, command);
		}
	}

	/**
	 * Sets a temporary metadata for the player that will be lost after the player quits or server reloads.
	 *
	 * @deprecated internal use only. On Bukkit, use CompMetadata instead
	 * @param key
	 * @param value
	 */
	@Deprecated
	public abstract void setTempMetadata(String key, Object value);

	/*
	 * Implementation of dispatchCommand() for players.
	 */
	protected abstract void performPlayerCommand0(String replacedCommand);

	/**
	 * Sends a message to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 */
	public final void sendActionBar(String message) {
		this.sendActionBar(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends a message to the player.
	 *
	 * @param message
	 */
	public abstract void sendActionBar(SimpleComponent message);

	/**
	 * Sends a bossbar to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 * @param progress
	 * @param color
	 * @param overlay
	 */
	public final void sendBossbarPercent(String message, float progress, BossBar.Color color, BossBar.Overlay overlay) {
		this.sendBossbarPercent(SimpleComponent.fromMini(message), progress, color, overlay);
	}

	/**
	 * Sends a bossbar to the player.
	 *
	 * @param message
	 * @param progress
	 * @param color
	 * @param overlay
	 */
	public abstract void sendBossbarPercent(SimpleComponent message, float progress, BossBar.Color color, BossBar.Overlay overlay);

	/**
	 * Sends a bossbar to the player for a certain amount of seconds.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 * @param seconds
	 * @param progress
	 * @param color
	 * @param overlay
	 */
	public final void sendBossbarTimed(String message, int seconds, float progress, BossBar.Color color, BossBar.Overlay overlay) {
		this.sendBossbarTimed(SimpleComponent.fromMini(message), seconds, progress, color, overlay);
	}

	/**
	 * Sends a bossbar to the player for a certain amount of seconds.
	 *
	 * @param message
	 * @param seconds
	 * @param progress
	 * @param color
	 * @param overlay
	 */
	public abstract void sendBossbarTimed(SimpleComponent message, int seconds, float progress, BossBar.Color color, BossBar.Overlay overlay);

	/**
	 * Removes the bossbar from the player.
	 */
	public abstract void removeBossBar();

	/**
	 * Sends a toast to the player if supported by the platform.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 */
	public final void sendToast(String message) {
		this.sendToast(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends a toast to the player if supported by the platform.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 * @param style
	 */
	public final void sendToast(String message, CompToastStyle style) {
		this.sendToast(SimpleComponent.fromMini(message), style);
	}

	/**
	 * Sends a toast to the player if supported by the platform.
	 *
	 * @param message
	 */
	public final void sendToast(SimpleComponent message) {
		this.sendToast(message, CompToastStyle.TASK);
	}

	/**
	 * Sends a toast to the player if supported by the platform.
	 *
	 * @param message
	 * @param style
	 */
	public abstract void sendToast(SimpleComponent message, CompToastStyle style);

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param header
	 * @param footer
	 */
	public final void sendTablist(String header, String footer) {
		this.sendTablist(SimpleComponent.fromMini(header), SimpleComponent.fromMini(footer));
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param header the header
	 * @param footer the footer
	 */
	public abstract void sendTablist(final SimpleComponent header, final SimpleComponent footer);

	/**
	 * Sends a message to the player.
	 *
	 * If message start with {@literal <actionbar>, <toast>, <title>, <bossbar>} or {@literal <center>},
	 * it are sent interactively or centered.
	 *
	 * This method also sends the message to the player if he is having a modal conversation in Bukkit.
	 *
	 * @param component
	 */
	public final void sendMessage(SimpleComponent component) {
		final String plainMessage = component.toPlain(this);

		//if (plainMessage.isEmpty())
		//	Debugger.printStackTrace("Sending empty message to player. Message: " + component.toAdventureJson(this));

		if (plainMessage.startsWith("<actionbar>")) {
			this.sendActionBar(component.replaceLiteral("<actionbar>", ""));

		} else if (plainMessage.startsWith("<toast>")) {
			this.sendToast(component.replaceLiteral("<toast>", ""));

		} else if (plainMessage.startsWith("<title>")) {
			final String stripped = component.toLegacy().replace("<title>", "").trim();

			if (!stripped.isEmpty()) {
				final String[] split = stripped.split("\\|");
				final String title = split[0];
				final String subtitle = split.length > 1 ? CommonCore.joinRange(1, split) : null;

				this.sendTitle(0, 60, 0, title, subtitle);
			}

		} else if (plainMessage.startsWith("<bossbar>")) {
			this.sendBossbarTimed(component.replaceLiteral("<bossbar>", ""), 10, 1F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

		} else if (plainMessage.startsWith("<center>")) {
			final String centeredLegacyMessage = ChatUtil.center(component.toLegacy(this).replaceAll("\\<center\\>(\\s|)", ""));

			this.sendLegacyMessage(centeredLegacyMessage);

		} else if (!plainMessage.equals("none"))
			this.sendRawMessage(component.toAdventure(this));
	}

	/**
	 * Sends the Adventure component to the player.
	 *
	 * This method also sends the message to the player if he is having a modal conversation in Bukkit.
	 *
	 * @param component
	 */
	public abstract void sendRawMessage(Component component);

	/*
	 * Implementation of sendMessage(String) for players.
	 */
	protected abstract void sendLegacyMessage(String message);

	/**
	 * Sends a JSON component message to the player.
	 *
	 * @param json
	 */
	public final void sendJson(String json) {
		this.sendMessage(SimpleComponent.fromAdventureJson(json));
	}

	/**
	 * Sends a title to the player for three seconds.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param title
	 * @param subtitle
	 */
	public final void sendTitle(final String title, final String subtitle) {
		this.sendTitle(20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player for three seconds
	 *
	 * @param title
	 * @param subtitle
	 */
	public final void sendTitle(final SimpleComponent title, final SimpleComponent subtitle) {
		this.sendTitle(20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public final void sendTitle(final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		this.sendTitle(fadeIn, stay, fadeOut, SimpleComponent.fromMini(title), SimpleComponent.fromMini(subtitle));
	}

	/**
	 * Sends a title to the player.
	 *
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public abstract void sendTitle(final int fadeIn, final int stay, final int fadeOut, final SimpleComponent title, final SimpleComponent subtitle);

	/**
	 * Resets the title that is being displayed to the player.
	 */
	public abstract void resetTitle();

	/**
	 * Returns the player's IP address and port or null if not a player or not supported by platform.
	 *
	 * @return
	 */
	public abstract InetSocketAddress getAddress();

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "FoundationPlayer{player=" + this.isPlayer() + ",name=" + this.getName() + "}";
	}
}
