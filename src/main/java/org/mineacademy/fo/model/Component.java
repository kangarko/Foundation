package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.Remain;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * A very simple way of sending interactive chat messages
 */
public final class Component {

	/**
	 * The past components having different hover/click events
	 */
	private final List<TextComponent> pastComponents = new ArrayList<>();

	/**
	 * The current component that is being modified
	 */
	private TextComponent currentComponent;

	/**
	 * Create a new interactive chat component
	 *
	 * @param text
	 */
	public Component(String... text) {
		this.currentComponent = new TextComponent(String.join("\n", Common.colorize(text)));
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a show text hover event for the {@link #currentComponent}
	 *
	 * @param text
	 * @return
	 */
	public Component onHover(String... text) {
		return onHover(String.join("\n", text));
	}

	/**
	 * Add a show text hover event for the {@link #currentComponent}
	 *
	 * @param text
	 * @return
	 */
	public Component onHover(String text) {
		return onHover(HoverEvent.Action.SHOW_TEXT, text);
	}

	/**
	 * Add a hover event for the {@link #currentComponent}
	 *
	 * @param action
	 * @param text
	 * @return
	 */
	public Component onHover(HoverEvent.Action action, String text) {
		currentComponent.setHoverEvent(new HoverEvent(action, TextComponent.fromLegacyText(Common.colorize(text))));

		return this;
	}

	/**
	 * Add a run command event for the {@link #currentComponent}
	 *
	 * @param text
	 * @return
	 */
	public Component onClickRunCmd(String text) {
		return onClick(Action.RUN_COMMAND, text);
	}

	/**
	 * Add a suggest command event for the {@link #currentComponent}
	 *
	 * @param text
	 * @return
	 */
	public Component onClickSuggestCmd(String text) {
		return onClick(Action.SUGGEST_COMMAND, text);
	}

	/**
	 * Add a command event for the {@link #currentComponent}
	 *
	 * @param action
	 * @param text
	 * @return
	 */
	public Component onClick(Action action, String text) {
		currentComponent.setClickEvent(new ClickEvent(action, Common.colorize(text)));

		return this;
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Create another component. The current is put in a list of past components
	 * so next time you use onClick or onHover, you will be added the event to the new one
	 * specified here
	 *
	 * @param text
	 * @return
	 */
	public Component append(String text) {
		pastComponents.add(currentComponent);
		currentComponent = new TextComponent(Common.colorize(text));

		return this;
	}

	/**
	 * Form a single {@link TextComponent} out of all components created
	 *
	 * @return
	 */
	public TextComponent build() {
		final TextComponent mainComponent = new TextComponent();

		for (final TextComponent pastComponent : pastComponents)
			mainComponent.addExtra(pastComponent);

		mainComponent.addExtra(currentComponent);

		return mainComponent;
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * Attempts to send the complete {@link Component} to the given
	 * command senders. If they are players, we send them interactive elements.
	 *
	 * If they are console, they receive a plain text message.
	 *
	 * @param senders
	 */
	public void send(CommandSender... senders) {
		final TextComponent mainComponent = build();

		for (final CommandSender sender : senders)
			Remain.sendComponent(sender, mainComponent);
	}
}
