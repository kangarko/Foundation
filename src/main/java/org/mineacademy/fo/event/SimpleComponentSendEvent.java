package org.mineacademy.fo.event;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.model.SimpleComponent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Triggered when any send() or sendAs() methods are used in {@link SimpleComponent} class.
 *
 * You can use this to change the message for each individual recipient and send
 * different messages for each player.
 *
 * ** YOU MUST USE {@link SimpleComponent#setFiringEvent(boolean)} to true for this event to call **
 */
@Getter
@Setter
@AllArgsConstructor
public final class SimpleComponentSendEvent extends SimpleCancellableEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * The sender, or null if unknown
	 */
	@Nullable
	private final CommandSender sender;

	/**
	 * The receiver of this component
	 */
	private final CommandSender receiver;

	/**
	 * The componentized message you can modify
	 */
	private TextComponent component;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}