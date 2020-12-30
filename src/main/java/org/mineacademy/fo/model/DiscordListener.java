package org.mineacademy.fo.model;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictSet;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class DiscordListener implements Listener {

	/**
	 * Holds registered Discord listeners
	 */
	private static final StrictSet<DiscordListener> registeredListeners = new StrictSet<>();

	/**
	 * Cleans all registered listeners
	 */
	public static final void clearRegisteredListeners() {
		registeredListeners.clear();
	}

	/**
	 * Temporarily stores the latest received message
	 */
	private Message message;

	/**
	 * Create a new Discord listener for the DiscordSRV plugin
	 *
	 * @param channel
	 */
	protected DiscordListener() {
		registeredListeners.add(this);
	}

	/**
	 * Register for listening to events only if not already
	 */
	public void register() {
		if (!registeredListeners.contains(this))
			registeredListeners.add(this);
	}

	/**
	 * Called automatically when someone writes a message in a Discord channel
	 *
	 * @param event
	 */
	private final void handleMessageReceived(DiscordGuildMessagePreProcessEvent event) {
		this.message = event.getMessage();

		onMessageReceived(event);
	}

	/**
	 * Override this to run code when someone writes a message in a Discord channel
	 *
	 * @param event
	 */
	protected abstract void onMessageReceived(DiscordGuildMessagePreProcessEvent event);

	/**
	 * Called automatically when someone writes a message in Minecraft and DiscordSRV
	 * automatically processes it -- if you are handling your message, we recommend
	 * canceling the event here to avoid double sending
	 *
	 * @param event
	 */
	protected void onMessageSent(GameChatMessagePreProcessEvent event) {
	}

	/**
	 * Convenience method for finding players, will stop your code if the player
	 * is not online, see {@link #checkBoolean(boolean, String)}
	 *
	 * @param playerName
	 * @param offlineMessage
	 * @return
	 */
	protected final Player findPlayer(String playerName, String offlineMessage) {
		final Player player = Bukkit.getPlayer(playerName);

		checkBoolean(player != null, offlineMessage);
		return player;
	}

	/**
	 * Checks if the given value is true, if not then sends a warning
	 * message to Discord and removes the received message
	 * <p>
	 * Your code will stop executing below if the boolean is false
	 *
	 * @param value
	 * @param warningMessage
	 * @throws RemovedMessageException
	 */
	protected final void checkBoolean(boolean value, String warningMessage) throws RemovedMessageException {
		if (!value)
			returnHandled(warningMessage);
	}

	/**
	 * Remove the message, send a warning and stop executing your code below this
	 *
	 * @param message
	 */
	protected final void returnHandled(String message) {
		removeAndWarn(message);

		throw new RemovedMessageException();
	}

	/**
	 * Removes the message received and sends a warning message from the bot
	 * shown for 2 seconds and then remove it.
	 *
	 * @param warningMessage
	 */
	protected final void removeAndWarn(String warningMessage) {
		removeAndWarn(this.message, warningMessage);
	}

	/**
	 * Removes the given message and sends a warning message from the bot
	 * shown for 2 seconds and then remove it.
	 *
	 * @param message
	 * @param warningMessage
	 */
	protected final void removeAndWarn(Message message, String warningMessage) {
		removeAndWarn(message, warningMessage, 2);
	}

	/**
	 * Removes the given message and sends a warning message from the bot
	 * shown for the given duration in seconds and then remove it.
	 *
	 * @param message
	 * @param warningMessage
	 * @param warningDurationSeconds how long to show the warning message
	 */
	protected final void removeAndWarn(Message message, String warningMessage, int warningDurationSeconds) {
		message.delete().complete();

		final MessageChannel channel = message.getChannel();
		final Message channelWarningMessage = channel.sendMessage(warningMessage).complete();

		channel.deleteMessageById(channelWarningMessage.getIdLong()).completeAfter(warningDurationSeconds, TimeUnit.SECONDS);
	}

	/**
	 * Return if the given member has the given role by name,
	 * case insensitive
	 *
	 * @param member
	 * @param roleName
	 * @return
	 */
	protected final boolean hasRole(Member member, String roleName) {
		for (final Role role : member.getRoles())
			if (role.getName().equalsIgnoreCase(roleName))
				return true;

		return false;
	}

	/**
	 * Convenience method for sending a message as the sender to a Discord channel
	 * <p>
	 * Enhanced functionality is available for the player
	 *
	 * @param sender
	 * @param channel
	 * @param message
	 */
	protected final void sendMessage(Player sender, String channel, String message) {
		HookManager.sendDiscordMessage(sender, channel, message);
	}

	/**
	 * Convenience method for sending a message to a Discord channel
	 *
	 * @param channel
	 * @param message
	 */
	protected final void sendMessage(String channel, String message) {
		HookManager.sendDiscordMessage(channel, message);
	}

	/**
	 * Convenience method for getting all linked DiscordSRV channels
	 *
	 * @return
	 */
	protected final Set<String> getChannels() {
		return HookManager.getDiscordChannels();
	}

	/**
	 * Internal exception to prevent further code execution
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class RemovedMessageException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Distributes received Discord messages across all {@link DiscordListener} classes
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class DiscordListenerImpl implements Listener {

		@Getter
		private static volatile DiscordListenerImpl instance = new DiscordListenerImpl();

		/**
		 * Reload the listener
		 */
		public void resubscribe() {
			DiscordSRV.api.unsubscribe(this);
			DiscordSRV.api.subscribe(this);
		}

		/**
		 * Distribute this message evenly across all listeners
		 *
		 * @param event
		 */
		@Subscribe(priority = ListenerPriority.HIGHEST)
		public void onMessageReceived(DiscordGuildMessagePreProcessEvent event) {
			for (final DiscordListener listener : registeredListeners)
				try {
					listener.handleMessageReceived(event);

				} catch (final RemovedMessageException ex) {
					// Fail through since we handled that

				} catch (final Throwable t) {
					Common.error(t,
							"Failed to handle DiscordSRV->Minecraft message!",
							"Sender: " + event.getAuthor().getName(),
							"Channel: " + event.getChannel().getName(),
							"Message: " + event.getMessage().getContentDisplay());
				}
		}

		/**
		 * Notify when DiscordSRV processes a Minecraft message and wants to
		 * send it to Discord
		 *
		 * @param event
		 */
		@Subscribe(priority = ListenerPriority.HIGHEST)
		public void onMessageSend(GameChatMessagePreProcessEvent event) {
			for (final DiscordListener listener : registeredListeners)
				try {
					listener.onMessageSent(event);

				} catch (final RemovedMessageException ex) {
					// Fail through since we handled that

				} catch (final Throwable t) {
					Common.error(t,
							"Failed to handle Minecraft->DiscordSRV message!",
							"Sender: " + event.getPlayer().getName(),
							"Channel: " + event.getChannel(),
							"Message: " + event.getMessage());
				}
		}
	}
}
