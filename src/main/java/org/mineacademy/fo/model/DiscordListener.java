package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.HierarchyException;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.WebhookUtil;
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
	 * Used if you edit a message. This maps the old message id to the new one
	 * because when we edit a message, its ID is changed
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private final Map<Long, Long> editedMessages = new HashMap<>();

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

	/*
	 * Called automatically when someone writes a message in a Discord channel
	 */
	private final void handleMessageReceived(DiscordGuildMessagePreProcessEvent event) {
		this.message = event.getMessage();

		onMessageReceived(event);
	}

	/*
	 * Called automatically when someone writes a message in a Discord channel
	 */
	private final void handleMessageReceivedLate(DiscordGuildMessagePostProcessEvent event) {
		this.message = event.getMessage();

		onMessageReceivedLate(event);
	}

	/**
	 * Override this to run code when someone writes a message in a Discord channel
	 *
	 * @param event
	 */
	protected abstract void onMessageReceived(DiscordGuildMessagePreProcessEvent event);

	/**
	 * Override this to run code when someone writes a message in a Discord channel
	 * after it has been processed by DiscordSRV (variables replaced etc.).
	 *
	 * @param event
	 */
	protected void onMessageReceivedLate(DiscordGuildMessagePostProcessEvent event) {

	}

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
	 * Retrieve a {@link TextChannel} from the given ID
	 *
	 * @param channelId
	 * @return
	 */
	protected final TextChannel findChannel(long channelId) {
		final JDA jda = DiscordUtil.getJda();

		// JDA can be null when server is starting or connecting
		if (jda != null)
			return jda.getTextChannelById(channelId);

		return null;
	}

	/**
	 * Retrieve a {@link TextChannel} list matching the given name
	 *
	 * @param channelId
	 * @return
	 */
	protected final List<TextChannel> findChannels(String channelName) {
		final JDA jda = DiscordUtil.getJda();

		// JDA can be null when server is starting or connecting
		if (jda != null)
			return jda.getTextChannelsByName(channelName, true);

		return new ArrayList<>();
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
	 * @param channelName
	 * @param message
	 */
	protected final void sendMessage(Player sender, String channelName, String message) {
		HookManager.sendDiscordMessage(sender, channelName, message);
	}

	/**
	 * Convenience method for sending a message to a Discord channel
	 *
	 * @param channelName
	 * @param message
	 */
	protected final void sendMessage(String channelName, String message) {
		HookManager.sendDiscordMessage(channelName, message);
	}

	/**
	 * Sends a webhook message from the given sender in case he's a valid Player
	 *
	 * @param sender
	 * @param channelName
	 * @param message
	 */
	protected final void sendWebhookMessage(CommandSender sender, String channelName, String message) {
		final List<TextChannel> channels = this.findChannels(channelName);
		final TextChannel channel = channels.isEmpty() ? null : channels.get(0);

		if (channel == null)
			return;

		// Send the message
		Common.runAsync(() -> {
			try {
				Debugger.debug("discord", "[Minecraft > Discord] Send MC message from '" + channelName + "' to Discord's '" + channel.getName() + "' channel: " + message);

				// You can remove this if you don't want to use webhooks
				if (sender instanceof Player)
					WebhookUtil.deliverMessage(channel, (Player) sender, message);

				else
					channel.sendMessage(message).complete();

			} catch (final ErrorResponseException ex) {
				Debugger.debug("discord", "Unable to send message to Discord channel " + channelName + ", message: " + message);
			}
		});
	}

	/**
	 * Send a message to the given channel for four seconds
	 *
	 * @param channel
	 * @param message
	 */
	protected final void flashMessage(TextChannel channel, String message) {
		final String finalMessage = Common.stripColors(message);

		Common.runAsync(() -> {
			final Message sentMessage = channel.sendMessage(finalMessage).complete();

			Common.runLaterAsync(4 * 20, () -> {
				try {
					channel.deleteMessageById(sentMessage.getIdLong()).complete();

				} catch (final github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException ex) {

					// Silence if deleted already
					if (!ex.getMessage().contains("Unknown Message"))
						ex.printStackTrace();
				}
			});
		});
	}

	/**
	 * Remove the given message by ID
	 *
	 * @param channel
	 * @param messageId
	 */
	protected final void deleteMessageById(TextChannel channel, long messageId) {
		Common.runAsync(() -> {

			// Try updating the message ID in case it has been edited
			final long latestMessageId = this.editedMessages.getOrDefault(messageId, messageId);

			try {
				channel.deleteMessageById(latestMessageId).complete();

			} catch (final Throwable t) {

				// ignore already deleted
				if (!(t instanceof github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException))
					t.printStackTrace();

				else
					Debugger.debug("discord", "Could not remove Discord message in channel '" + channel.getName() + "' id " + latestMessageId
							+ ", it was probably deleted otherwise or this is a bug.");
			}
		});
	}

	/**
	 * Edit the given message by ID
	 *
	 * @param channel
	 * @param messageId
	 * @param format
	 */
	protected final void editMessageById(TextChannel channel, long messageId, String format) {
		Common.runAsync(() -> {
			try {
				final Message message = channel.retrieveMessageById(messageId).complete();

				if (message != null) {

					// Remove old message
					channel.deleteMessageById(messageId).complete();

					// Send a new one
					final Message newSentMessage = channel
							.sendMessage(format.replace("{player}", message.getAuthor().getName()).replace("*", "\\*").replace("_", "\\_").replace("@", "\\@"))
							.complete();

					this.editedMessages.put(messageId, newSentMessage.getIdLong());
				}

			} catch (final Throwable t) {
				if (!t.toString().contains("Unknown Message"))
					t.printStackTrace();
			}
		});
	}

	/**
	 * Attempt to parse the sender's name into his Minecraft name in case he linked it
	 *
	 * @param member
	 * @param author
	 * @return
	 */
	protected final String findPlayerName(Member member, User author) {
		final String discordName = Common.getOrDefaultStrict(member.getNickname(), author.getName());
		final UUID linkedId = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(author.getId());

		final Player player;

		if (linkedId != null)

			// You could potentially look this in offline players too
			// using an async callback to prevent lag if there's tons
			// of players saved or in case of a HTTP request
			player = Remain.getPlayerByUUID(linkedId);

		else
			player = Bukkit.getPlayer(discordName);

		return player != null && player.isOnline() ? player.getName() : discordName;
	}

	/**
	 * Attempt to kick the player name from the channel
	 *
	 * @param discordSender
	 * @param reason
	 */
	public final void kickMember(DiscordSender discordSender, String reason) {
		Common.runAsync(() -> {
			try {
				final Member member = DiscordUtil.getMemberById(discordSender.getUser().getId());

				if (member != null)
					member.kick(reason).complete();

			} catch (final HierarchyException ex) {
				Common.log("Unable to kick " + discordSender.getName() + " because he appears to be Discord administrator");
			}
		});
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
		 * Register plugin hook
		 *
		 * https://github.com/kangarko/ChatControl-Red/issues/703
		 */
		public void registerHook() {
			try {
				DiscordSRV.getPlugin().getPluginHooks().add(SimplePlugin::getInstance);

			} catch (final Error err) {
				// Support previous Discord versions
			}
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
							"Failed to handle DiscordSRV->Minecraft message (pre process)!",
							"Sender: " + event.getAuthor().getName(),
							"Channel: " + event.getChannel().getName(),
							"Message: " + event.getMessage().getContentDisplay());
				}
		}

		/**
		 * Distribute this message evenly across all listeners
		 *
		 * @param event
		 */
		@Subscribe(priority = ListenerPriority.HIGHEST)
		public void onMessageReceivedLate(DiscordGuildMessagePostProcessEvent event) {
			for (final DiscordListener listener : registeredListeners)
				try {
					listener.handleMessageReceivedLate(event);

				} catch (final RemovedMessageException ex) {
					// Fail through since we handled that

				} catch (final Throwable t) {
					Common.error(t,
							"Failed to handle DiscordSRV->Minecraft message (post process)!",
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
