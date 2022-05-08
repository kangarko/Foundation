package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a chat message surrounded by chat-wide line on the top and bottom:
 * <p>
 * -----------------------------------
 * Hello this is a test!
 * -----------------------------------
 * <p>
 * You can also specify \<center\> in front of the text to center it.
 */
public final class BoxedMessage {

	/**
	 * The color of the top and bottom line
	 */
	public static ChatColor LINE_COLOR = ChatColor.DARK_GRAY;

	/**
	 * All message recipients
	 */
	private final Iterable<? extends CommandSender> recipients;

	/**
	 * The sender of the message
	 */
	private final Player sender;

	/**
	 * The messages to send
	 */
	private final String[] messages;

	/**
	 * Create a new boxed message from the given messages
	 * without sending it to any player
	 *
	 * @param messages
	 */
	public BoxedMessage(@NonNull String... messages) {
		this(null, null, messages);
	}

	/**
	 * Create a new boxed message
	 *
	 * @param recipients
	 * @param sender
	 * @param messages
	 */
	private BoxedMessage(Iterable<? extends CommandSender> recipients, Player sender, @NonNull String[] messages) {
		this.recipients = recipients == null ? null : Common.toList(recipients); // Make a copy to prevent changes in the list on send
		this.sender = sender;
		this.messages = messages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helper methods
	// ------------------------------------------------------------------------------------------------------------

	private void launch() {
		Common.runLater(2, () -> {
			final String oldTellPrefix = Common.getTellPrefix();
			Common.setTellPrefix("");

			this.sendFrame();

			Common.setTellPrefix(oldTellPrefix);
		});
	}

	private void sendFrame() {
		this.sendLine();
		this.sendFrameInternals0();
		this.sendLine();
	}

	private void sendFrameInternals0() {
		for (int i = 0; i < this.getTopLines(); i++)
			this.send("&r");

		for (final String message : this.messages)
			for (final String part : message.split("\n"))
				this.send(part);

		for (int i = 0; i < this.getBottomLines(); i++)
			this.send("&r");
	}

	private int getTopLines() {
		switch (this.length()) {
			case 1:
				return 2;
			case 2:
			case 3:
			case 4:
				return 1;

			default:
				return 0;
		}
	}

	private int getBottomLines() {
		switch (this.length()) {
			case 1:
			case 2:
				return 2;
			case 3:
				return 1;

			default:
				return 0;
		}
	}

	private void sendLine() {
		this.send(LINE_COLOR + Common.chatLineSmooth());
	}

	private int length() {
		int length = 0;

		for (final String message : this.messages)
			for (@SuppressWarnings("unused")
			final String part : message.split("\n"))
				length++;

		return length;
	}

	private void send(String message) {
		message = this.centerMessage0(message);

		if (this.recipients == null)
			this.broadcast0(message);

		else
			this.tell0(message);
	}

	private String centerMessage0(String message) {
		if (message.startsWith("<center>"))
			return ChatUtil.center(message.replaceFirst("\\<center\\>(\\s|)", ""));

		return message;
	}

	private void broadcast0(String message) {
		if (this.sender != null)
			Common.broadcast(message, this.sender);
		else
			Common.broadcastTo(Remain.getOnlinePlayers(), message);
	}

	private void tell0(String message) {
		if (this.sender != null)
			message = message.replace("{player}", Common.resolveSenderName(this.sender));

		Common.broadcastTo(this.recipients, message);
	}

	/**
	 * Finds the given variables (you do not need to put {} brackets, we put them there)
	 * and replaces them with instances
	 *
	 * @param variables
	 * @return
	 */
	public Replacor find(String... variables) {
		return new Replacor(variables);
	}

	public String getMessage() {
		return this.messages.length == 0 ? "" : String.join("\n", this.messages);
	}

	@Override
	public String toString() {
		return "Boxed{" + String.join(", ", this.messages) + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Broadcast this message to everyone on the message
	 */
	public void broadcast() {
		broadcast(null, this.messages);
	}

	/**
	 * Broadcast this message to all players as the sender,
	 * replacing {player} with the sender name
	 *
	 * @param sender
	 */
	public void broadcastAs(Player sender) {
		new BoxedMessage(null, sender, this.messages).launch();
	}

	/**
	 * Sends this message to the recipient
	 *
	 * @param recipient
	 */
	public void tell(CommandSender recipient) {
		tell(null, Arrays.asList(recipient), this.messages);
	}

	/**
	 * Sends this message to given recipients
	 *
	 * @param recipients
	 */
	public void tell(Iterable<? extends CommandSender> recipients) {
		tell(null, recipients, this.messages);
	}

	/**
	 * Sends this message to the given recipient
	 * replacing {player} with the sender name
	 *
	 * @param receiver
	 * @param sender
	 */
	public void tellAs(CommandSender receiver, Player sender) {
		tell(sender, Arrays.asList(receiver), this.messages);
	}

	/**
	 * Sends this message to the given recipients
	 * replacing {player} with the sender name
	 *
	 * @param receivers
	 * @param sender
	 */
	public void tellAs(Iterable<? extends CommandSender> receivers, Player sender) {
		new BoxedMessage(receivers, sender, this.messages).launch();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Send this message to everyone
	 *
	 * @param messages
	 */
	public static void broadcast(String... messages) {
		broadcast(null, messages);
	}

	/**
	 * Sends this message to the all players as the sender
	 *
	 * @param sender
	 * @param messages
	 */
	public static void broadcast(Player sender, String... messages) {
		new BoxedMessage(null, sender, messages).launch();
	}

	/**
	 * Sends the message to the recipient
	 *
	 * @param recipient
	 * @param messages
	 */
	public static void tell(CommandSender recipient, String... messages) {
		tell(null, Arrays.asList(recipient), messages);
	}

	/**
	 * Sends the message to the given recipients
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void tell(Iterable<? extends CommandSender> recipients, String... messages) {
		tell(null, recipients, messages);
	}

	/**
	 * Sends this message to a recipient as sender
	 *
	 * @param sender
	 * @param receiver
	 * @param messages
	 */
	public static void tell(Player sender, CommandSender receiver, String... messages) {
		tell(sender, Arrays.asList(receiver), messages);
	}

	/**
	 * Sends this message to recipients as sender
	 *
	 * @param sender
	 * @param receivers
	 * @param messages
	 */
	public static void tell(Player sender, Iterable<? extends CommandSender> receivers, String... messages) {
		new BoxedMessage(receivers, sender, messages).launch();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacor
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Utility class for quickly replacing variables
	 */
	@RequiredArgsConstructor
	public class Replacor {

		/**
		 * The placeholder names to replace
		 */
		private final String[] variables;

		/**
		 * Replace the variables we store with the given object replacements
		 *
		 * @param replacements
		 * @return
		 */
		public final BoxedMessage replace(Object... replacements) {
			String message = String.join("%delimiter%", BoxedMessage.this.messages);

			for (int i = 0; i < this.variables.length; i++) {
				String find = this.variables[i];

				{ // Auto insert brackets
					if (!find.startsWith("{"))
						find = "{" + find;

					if (!find.endsWith("}"))
						find = find + "}";
				}

				final Object rep = i < replacements.length ? replacements[i] : null;

				message = message.replace(find, rep != null ? Objects.toString(rep) : "");
			}

			final String[] copy = message.split("%delimiter%");

			return new BoxedMessage(BoxedMessage.this.recipients, BoxedMessage.this.sender, copy);
		}
	}
}