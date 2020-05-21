package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.Data;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * A very simple way of sending interactive chat messages
 */
public class SimpleComponent {

	/**
	 * The pattern to match URL addresses when parsing text
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

	/**
	 * The past components having different hover/click events
	 */
	private final List<PermissibleComponent> pastComponents = new ArrayList<>();

	/**
	 * Shall we automatically translate & into colors? True by default
	 *
	 * Hover/Click events will always be colorized
	 */
	private final boolean colorize;

	/**
	 * The current component that is being modified
	 */
	private PermissibleComponent[] currentComponents;

	/**
	 * Create a new empty component
	 */
	protected SimpleComponent() {
		this(true, "");
	}

	/**
	 * Create a new interactive chat component
	 *
	 * @param colorize
	 * @param text
	 */
	protected SimpleComponent(boolean colorize, String... text) {
		this.colorize = colorize;
		this.currentComponents = fromLegacyText(colorize ? String.join("\n", Common.colorize(text)) : String.join("\n", text), null, null);
	}

	@Data
	public class PermissibleComponent {

		private final BaseComponent component;
		private final String permission;
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a show text hover event for the {@link #currentComponents}
	 *
	 * @param texts
	 * @return
	 */
	public SimpleComponent onHover(Collection<String> texts) {
		return onHover(texts.toArray(new String[texts.size()]));
	}

	/**
	 * Add a show text hover event for the {@link #currentComponents}
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onHover(String... text) {
		return onHover(HoverEvent.Action.SHOW_TEXT, String.join("\n", text));
	}

	/**
	 * Shows the item on hover if it is not air.
	 *
	 * NB: Some colors from lore may get lost as a result of Minecraft/Spigot bug.
	 *
	 * @param item
	 * @return
	 */
	public SimpleComponent onHover(ItemStack item) {
		return CompMaterial.isAir(item.getType()) ? onHover("Air") : onHover(HoverEvent.Action.SHOW_ITEM, Remain.toJson(item));
	}

	/**
	 * Add a hover event for the {@link #currentComponents}
	 *
	 * @param action
	 * @param text
	 * @return
	 */
	public SimpleComponent onHover(HoverEvent.Action action, String text) {
		for (final PermissibleComponent component : currentComponents)
			component.getComponent().setHoverEvent(new HoverEvent(action, TextComponent.fromLegacyText(Common.colorize(text))));

		return this;
	}

	/**
	 * Add a run command event for the {@link #currentComponents}
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickRunCmd(String text) {
		return onClick(Action.RUN_COMMAND, text);
	}

	/**
	 * Add a suggest command event for the {@link #currentComponents}
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickSuggestCmd(String text) {
		return onClick(Action.SUGGEST_COMMAND, text);
	}

	/**
	 * Open the given URL for the {@link #currentComponents}
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickOpenUrl(String url) {
		return onClick(Action.OPEN_URL, url);
	}

	/**
	 * Add a command event for the {@link #currentComponents}
	 *
	 * @param action
	 * @param text
	 * @return
	 */
	public SimpleComponent onClick(Action action, String text) {
		for (final PermissibleComponent component : currentComponents)
			component.getComponent().setClickEvent(new ClickEvent(action, Common.colorize(text)));

		return this;
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	public SimpleComponent append(String text) {
		return append(text, null);
	}

	/**
	 * Create another component. The current is put in a list of past components
	 * so next time you use onClick or onHover, you will be added the event to the new one
	 * specified here
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent append(String text, String viewPermission) {

		// Copy the last color to reuse in the next component
		ChatColor lastcolor = null;

		for (final PermissibleComponent baseComponent : currentComponents) {
			pastComponents.add(baseComponent);

			lastcolor = baseComponent.getComponent().getColor();
		}

		currentComponents = fromLegacyText(colorize ? Common.colorize(text) : text, lastcolor, viewPermission);

		return this;
	}

	/**
	 * Form a single {@link TextComponent} out of all components created
	 *
	 * @return
	 */
	public TextComponent build() {
		final TextComponent mainComponent = new TextComponent("");

		for (final PermissibleComponent pastComponent : pastComponents)
			mainComponent.addExtra(pastComponent.getComponent());

		for (final PermissibleComponent currentComponent : currentComponents)
			mainComponent.addExtra(currentComponent.getComponent());

		return mainComponent;
	}

	/**
	 * Return all components that we have created
	 *
	 * @return
	 */
	public List<PermissibleComponent> getComponents() {
		return Common.joinArrays(pastComponents, Arrays.asList(currentComponents));
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events
	 *
	 * @return
	 */
	public String getPlainMessage() {
		return build().toLegacyText();
	}

	/*
	 * Compile the message into a component, creating a new BaseComponent
	 * each time the message has a new & color/formatting, preserving
	 * the last color
	 */
	private PermissibleComponent[] fromLegacyText(@NonNull String message, @Nullable ChatColor lastColor, @Nullable String viewPermission) {
		final ArrayList<PermissibleComponent> components = new ArrayList<>();
		final Matcher matcher = URL_PATTERN.matcher(message);

		StringBuilder builder = new StringBuilder();
		TextComponent component = new TextComponent();

		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);

			if (c == ChatColor.COLOR_CHAR) {
				if (++i >= message.length())
					break;

				c = message.charAt(i);

				if (c >= 'A' && c <= 'Z')
					c += 32;

				ChatColor format = ChatColor.getByChar(c);

				if (format == null)
					continue;

				if (builder.length() > 0) {
					final TextComponent old = component;

					component = new TextComponent(old);
					old.setText(builder.toString());

					builder = new StringBuilder();
					components.add(new PermissibleComponent(old, viewPermission));
				}

				switch (format) {
					case BOLD:
						component.setBold(true);
						break;
					case ITALIC:
						component.setItalic(true);
						break;
					case UNDERLINE:
						component.setUnderlined(true);
						break;
					case STRIKETHROUGH:
						component.setStrikethrough(true);
						break;
					case MAGIC:
						component.setObfuscated(true);
						break;
					case RESET:
						format = ChatColor.WHITE;
					default:
						component = new TextComponent();
						component.setColor(format);
						break;
				}

				lastColor = format;
				continue;
			}

			int pos = message.indexOf(' ', i);

			if (pos == -1)
				pos = message.length();

			// Web link handling
			if (matcher.region(i, pos).find()) {
				if (builder.length() > 0) {
					final TextComponent old = component;

					component = new TextComponent(old);
					old.setText(builder.toString());
					builder = new StringBuilder();
					components.add(new PermissibleComponent(old, viewPermission));
				}

				final TextComponent old = component;
				component = new TextComponent(old);

				final String urlString = message.substring(i, pos);
				component.setText(urlString);
				component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlString.startsWith("http") ? urlString : "http://" + urlString));
				components.add(new PermissibleComponent(component, viewPermission));

				i += pos - i - 1;
				component = old;

				continue;
			}

			builder.append(c);
		}

		component.setText(builder.toString());
		component.setColor(lastColor);

		components.add(new PermissibleComponent(component, viewPermission));

		return components.stream().toArray(PermissibleComponent[]::new);
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 *
	 * If they are console, they receive a plain text message.
	 *
	 * @param <T>
	 * @param senders
	 */
	public <T extends CommandSender> void send(Iterable<T> senders) {
		final TextComponent mainComponent = build();

		for (final CommandSender sender : senders)
			Remain.sendComponent(sender, mainComponent);
	}

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 *
	 * If they are console, they receive a plain text message.
	 *
	 * @param senders
	 */
	public <T extends CommandSender> void send(T... senders) {
		final TextComponent mainComponent = build();

		for (final CommandSender sender : senders)
			Remain.sendComponent(sender, mainComponent);
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(String... text) {
		return of(true, text);
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param colorize
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(boolean colorize, String... text) {
		return new SimpleComponent(colorize, text);
	}
}
