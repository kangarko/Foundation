package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleLocation;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.BossBarTask.TimedBar;
import org.mineacademy.fo.settings.Lang;

import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Emitter;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.Action;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.title.TitlePart;

/**
 * Similar to Audience in Adventure, the FoundationPlayer represents a platform-neutral
 * audience that can receive messages and interact with the server.
 *
 * However, it can only hold ONE player at a time to prevent confusion (Audience
 * can also hold the entire server's audience which is a new concept unused in Bukkit)
 *
 * To create one, use {@link Platform#toPlayer(Object)} and pass in the familiar object
 * such as a Player or a CommandSender.
 */
public abstract class FoundationPlayer implements Audience {

	/**
	 * The minimum protocol version required for HEX colors to work. Added in 20w17a.
	 */
	protected static final int MINIMUM_PROTOCOL_FOR_HEX = 713;

	/*
	 * Darken the given color by one shade.
	 */
	private static TextColor darkenOneShade(@NonNull TextColor color) {
		final int red = Math.max(color.red() - 1, 0);
		final int green = color.green();
		final int blue = color.blue();

		return TextColor.color(red, green, blue);
	}

	/*
	 * On legacy, the client resets hover formatting on \n, so we reapply it from the previous line.
	 *
	 * See https://github.com/KyoriPowered/adventure/issues/1132
	 */
	private static Component fixHoverLosingStyleInLegacyMultiline(Component adventure) {
		if (adventure.hoverEvent() != null) {
			final HoverEvent<?> hover = adventure.hoverEvent();

			if (hover.action() == Action.SHOW_TEXT) {

				// Cleverly flip back to MiniMessage to retain complex formatting structures
				final String oldMini = MiniMessage.miniMessage().serialize((Component) hover.value());

				if (oldMini.contains("\n")) {
					final String[] oldLines = oldMini.split("\n");
					Style lastStyle = null;

					for (int i = 0; i < oldLines.length; i++) {
						final String line = oldLines[i];

						if (lastStyle != null) {

							// Append decorations
							for (final Map.Entry<TextDecoration, State> entry : lastStyle.decorations().entrySet())
								if (entry.getValue() == State.TRUE)
									oldLines[i] = "<" + entry.getKey().name() + ">" + line;

							// MiniMessage ignores the tag if it is equal to the last one, so we need shift its color
							// This is invisible on legacy thanks to downsapling
							if (lastStyle.color() != null)
								oldLines[i] = "<" + darkenOneShade(lastStyle.color()).asHexString() + ">" + line;
						}

						// Find the last style and apply it to the next line using the updated last line
						lastStyle = SimpleComponent.LastMessageStyleParser.parseStyle(oldLines[i]);
					}

					adventure = adventure.hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize(String.join("\n", oldLines))));
				}
			}
		}

		if (!adventure.children().isEmpty()) {
			final List<Component> newChildren = new ArrayList<>();

			for (final Component child : adventure.children())
				newChildren.add(fixHoverLosingStyleInLegacyMultiline(child));

			adventure = adventure.children(newChildren);
		}

		return adventure;
	}

	/**
	 * Spoofs a chat message from the player.
	 *
	 * @param message
	 */
	public abstract void chat(String message);

	/**
	 * Clears the title from the player.
	 */
	@Override
	public final void clearTitle() {
		this.resetTitle();
	}

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void deleteMessage(SignedMessage.Signature signature) {
		throw new UnsupportedOperationException("deleteMessage");
	}

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

		if (command.startsWith("@announce "))
			Messenger.announce(this, command.replace("@announce ", ""));

		else if (command.startsWith("@warn "))
			Messenger.warn(this, command.replace("@warn ", ""));

		else if (command.startsWith("@error "))
			Messenger.error(this, command.replace("@error ", ""));

		else if (command.startsWith("@info "))
			Messenger.info(this, command.replace("@info ", ""));

		else if (command.startsWith("@question "))
			Messenger.question(this, command.replace("@question ", ""));

		else if (command.startsWith("@success "))
			Messenger.success(this, command.replace("@success ", ""));

		else {
			command = Variables.builder(this).replace(command.startsWith("/") && !command.startsWith("//") ? command.substring(1) : command);

			// Workaround for JSON in tellraw getting HEX colors replaced
			if (!command.startsWith("tellraw"))
				command = CompChatColor.translateColorCodes(command);

			if (this.isPlayer())
				this.performPlayerCommand0(command.replace("§", "&"));
			else
				Platform.getPlatform().dispatchConsoleCommand(this, command);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FoundationPlayer) {
			final FoundationPlayer other = (FoundationPlayer) obj;

			if (other.isPlayer() && !this.isPlayer())
				return false;

			if (!other.isPlayer() && this.isPlayer())
				return false;

			return other.isPlayer() ? other.getUniqueId().equals(this.getUniqueId()) : other.getName().equals(this.getName());
		}

		return false;
	}

	/**
	 * Returns the player's IP address and port or null if not a player or not supported by platform.
	 *
	 * @return
	 */
	public abstract InetSocketAddress getAddress();

	/**
	 * Returns the player's location if called on Bukkit and the player is not a console
	 * or returns null if not applicable.
	 *
	 * Throws exception if called on Bukkit and the sender is console:
	 * @see #isPlayer()
	 *
	 * @return
	 */
	public SimpleLocation getBukkitLocation() {
		return null;
	}

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

	/**
	 * Get the command sender implementation object, such as CommandSender on Bukkit.
	 *
	 * @param <T>
	 * @return
	 */
	@NonNull
	public abstract <T> T getSender();

	/**
	 * Implementation of getName() for players.
	 */
	protected abstract String getSenderName0();

	/**
	 * Return the current server His Majesty is on,
	 * null for poor consoles or weird Velocity players.
	 *
	 * @return
	 */
	public abstract FoundationServer getServer();

	/**
	 * Returns the player's unique ID, or error if we are not a player.
	 *
	 * @return
	 */
	public abstract UUID getUniqueId();

	/**
	 * Return true if the player has RGB support
	 *
	 * @return
	 */
	public abstract boolean hasHexColorSupport();

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

	/**
	 * Implementation of hasPermission().
	 */
	protected abstract boolean hasPermission0(String permission);

	/**
	 * Hides the boss bar from the player.
	 */
	@Override
	public final void hideBossBar(BossBar bar) {
		if (this.isPlayer())
			BossBarTask.getInstance().hide(this, bar);
	}

	/**
	 * Implementation of hideBossBar().
	 *
	 * @param bar
	 */
	protected abstract void hideBossBar0(TimedBar bar);

	/**
	 * Removes all boss bars from the player.
	 */
	public final void hideBossBars() {
		if (this.isPlayer())
			BossBarTask.getInstance().hideAll(this);
	}

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
	 * Returns true if the player is a player, false if console or command sender.
	 *
	 * @return
	 */
	public abstract boolean isPlayer();

	/**
	 * Return true if this is a player and the player is online
	 *
	 * @return
	 */
	public abstract boolean isPlayerOnline();

	/**
	 * Kicks this player, or throws error if not a player.
	 *
	 * @param reason
	 */
	public abstract void kick(SimpleComponent reason);

	/**
	 * Kicks this player, or throws error if not a player.
	 *
	 * @param reason
	 */
	public final void kick(String reason) {
		this.kick(SimpleComponent.fromMini(reason));
	}

	/**
	 * Opens the book for the player.
	 */
	@Override
	public abstract void openBook(Book book);

	/**
	 * Opens the book for the player.
	 *
	 * @param title
	 * @param author
	 * @param pages
	 */
	public final void openBook(SimpleComponent title, SimpleComponent author, Collection<SimpleComponent> pages) {
		this.openBook(Book.book(title.toAdventure(), author.toAdventure(), pages.stream().map(SimpleComponent::toAdventure).collect(Collectors.toList())));
	}

	/**
	 * Opens the book for the player.
	 *
	 * @param title
	 * @param author
	 * @param pages
	 */
	public final void openBook(String title, String author, String... pages) {
		this.openBook(SimpleComponent.fromMini(title), SimpleComponent.fromMini(author), Arrays.stream(pages).map(SimpleComponent::fromMini).collect(Collectors.toList()));
	}

	/*
	 * Implementation of dispatchCommand() for players.
	 */
	protected abstract void performPlayerCommand0(String replacedCommand);

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void playSound(Sound sound) {
		throw new UnsupportedOperationException("playSound");
	}

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void playSound(Sound sound, double x, double y, double z) {
		this.playSound(sound);
	}

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void playSound(Sound sound, Emitter emitter) {
		this.playSound(sound);
	}

	/**
	 * Resets the title that is being displayed to the player.
	 */
	@Override
	public abstract void resetTitle();

	/**
	 * Sends an actionbar message to the player.
	 */
	@Override
	public final void sendActionBar(Component message) {
		this.sendActionBar(SimpleComponent.fromAdventure(message));
	}

	/**
	 * Sends a message to the player.
	 *
	 * @param message
	 */
	public abstract void sendActionBar(SimpleComponent message);

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
	 * Sends a JSON component message to the player.
	 *
	 * @param json
	 */
	public final void sendJson(String json) {
		this.sendMessage(SimpleComponent.fromAdventureJson(json, !this.hasHexColorSupport()));
	}

	/**
	 * @deprecated use {@link #sendRawMessage(Component)}
	 */
	@Deprecated
	@Override
	public final void sendMessage(Identity source, Component message, MessageType type) {
		this.sendRawMessage(message);
	}

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

		if (plainMessage.startsWith("<actionbar>"))
			this.sendActionBar(component.replaceLiteral("<actionbar>", ""));

		else if (plainMessage.startsWith("<toast>"))
			this.sendToast(component.replaceLiteral("<toast>", ""));

		else if (plainMessage.startsWith("<title>")) {
			final String stripped = component.toLegacy().replace("<title>", "").trim();

			if (!stripped.isEmpty()) {
				final String[] split = stripped.split("\\|");
				final String title = split[0];
				final String subtitle = split.length > 1 ? CommonCore.joinRange(1, split) : null;

				this.showTitle(0, 60, 0, title, subtitle);
			}

		} else if (plainMessage.startsWith("<bossbar>"))
			this.showBossbarTimed(component.replaceLiteral("<bossbar>", ""), 10, 1F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

		else if (plainMessage.startsWith("<center>")) {
			final String centeredLegacyMessage = ChatUtil.center(component.toLegacy(this).replaceAll("\\<center\\>(\\s|)", ""));

			this.sendRawMessage(SimpleComponent.fromSection(centeredLegacyMessage).toAdventure());

		} else if (!plainMessage.equals("none")) {
			Component adventure = component.toAdventure(this);

			if (!this.hasHexColorSupport())
				adventure = fixHoverLosingStyleInLegacyMultiline(adventure);

			this.sendRawMessage(adventure);
		}
	}

	/**
	 * Sends a MiniMessage message to the player. Legacy and mini tags are both supported.
	 *
	 * @param message
	 */
	public final void sendMiniMessage(String message) {
		this.sendMessage(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends a plain message to the player. No colors are supported.
	 *
	 * @param message
	 */
	public final void sendPlainMessage(String message) {
		this.sendMessage(SimpleComponent.fromPlain(message));
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 */
	@Override
	public final void sendPlayerListHeaderAndFooter(Component header, Component footer) {
		this.sendPlayerListHeaderAndFooter(SimpleComponent.fromAdventure(header), SimpleComponent.fromAdventure(footer));
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param header the header
	 * @param footer the footer
	 */
	public abstract void sendPlayerListHeaderAndFooter(final SimpleComponent header, final SimpleComponent footer);

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param header
	 * @param footer
	 */
	public final void sendPlayerListHeaderAndFooter(String header, String footer) {
		this.sendPlayerListHeaderAndFooter(SimpleComponent.fromMini(header), SimpleComponent.fromMini(footer));
	}

	/**
	 * Sends the Adventure component to the player.
	 *
	 * This method also sends the message to the player if he is having a modal conversation in Bukkit.
	 *
	 * @param component
	 */
	public abstract void sendRawMessage(Component component);

	/**
	 * @deprecated use {@link #showTitle(Title)}i
	 */
	@Deprecated
	@Override
	public final <T> void sendTitlePart(TitlePart<T> part, T value) {
		throw new UnsupportedOperationException("sendTitlePart, use sendTitle instead");
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
	 * Sets a temporary metadata for the player that will be lost after the player quits or server reloads.
	 *
	 * @deprecated internal use only. On Bukkit, use CompMetadata instead
	 * @param key
	 * @param value
	 */
	@Deprecated
	public abstract void setTempMetadata(String key, Object value);

	/**
	 * Shows a bossbar to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 */
	@Override
	public final void showBossBar(BossBar bar) {
		if (this.isPlayer())
			BossBarTask.getInstance().show(this, TimedBar.permanent(bar));
	}

	/**
	 * Sends a bossbar to the player.
	 *
	 * @param message
	 * @param progress
	 * @param color
	 * @param overlay
	 *
	 * @return the bossbar
	 */
	public final BossBar showBossBar(SimpleComponent message, float progress, BossBar.Color color, BossBar.Overlay overlay) {
		final BossBar bar = BossBar.bossBar(message, progress, color, overlay);
		this.showBossBar(bar);

		return bar;
	}

	/**
	 * Sends a bossbar to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 * @param progress
	 * @param color
	 * @param overlay
	 *
	 * @return
	 */
	public final BossBar showBossBar(String message, float progress, BossBar.Color color, BossBar.Overlay overlay) {
		return this.showBossBar(SimpleComponent.fromMini(message), progress, color, overlay);
	}

	/**
	 * Implementation of showBossBar().
	 *
	 * @param bar
	 */
	protected abstract void showBossBar0(TimedBar bar);

	/**
	 * Shows a bossbar to the player for a certain amount of seconds.
	 *
	 * @param secondsToShow
	 * @param bar
	 */
	public final void showBossbarTimed(int secondsToShow, BossBar bar) {
		if (this.isPlayer())
			BossBarTask.getInstance().show(this, TimedBar.timed(bar, secondsToShow));
	}

	/**
	 * Sends a bossbar to the player for a certain amount of seconds.
	 *
	 * @param message
	 * @param seconds
	 * @param progress
	 * @param color
	 * @param overlay
	 *
	 * @return the bossbar
	 */
	public final BossBar showBossbarTimed(SimpleComponent message, int seconds, float progress, BossBar.Color color, BossBar.Overlay overlay) {
		final BossBar bar = BossBar.bossBar(message, progress, color, overlay);
		this.showBossbarTimed(seconds, bar);

		return bar;
	}

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
	 *
	 * @return the bossbar
	 */
	public final BossBar showBossbarTimed(String message, int seconds, float progress, BossBar.Color color, BossBar.Overlay overlay) {
		return this.showBossbarTimed(SimpleComponent.fromMini(message), seconds, progress, color, overlay);
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
	public final void showTitle(final int fadeIn, final int stay, final int fadeOut, final SimpleComponent title, final SimpleComponent subtitle) {
		this.showTitle(Title.title(title.toAdventure(), subtitle.toAdventure(), Times.of(Duration.ofMillis(fadeIn * 50), Duration.ofMillis(stay * 50), Duration.ofMillis(fadeOut * 50))));
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
	public final void showTitle(final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		this.showTitle(fadeIn, stay, fadeOut, SimpleComponent.fromMini(title), SimpleComponent.fromMini(subtitle));
	}

	/**
	 * Sends a title to the player for three seconds
	 *
	 * @param title
	 * @param subtitle
	 */
	public final void showTitle(final SimpleComponent title, final SimpleComponent subtitle) {
		this.showTitle(20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player for three seconds.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param title
	 * @param subtitle
	 */
	public final void showTitle(final String title, final String subtitle) {
		this.showTitle(20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Shows a title to the player.
	 */
	@Override
	public abstract void showTitle(Title title);

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public void stopSound(SoundStop sound) {
		throw new UnsupportedOperationException("stopSound");
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "FoundationPlayer{player=" + this.isPlayer() + ",name=" + this.getName() + "}";
	}

}
