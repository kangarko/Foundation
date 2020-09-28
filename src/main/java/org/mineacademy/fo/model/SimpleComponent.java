package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * A very simple way of sending interactive chat messages
 */
@ToString
public final class SimpleComponent {

	/**
	 * The pattern to match URL addresses when parsing text
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

	/**
	 * The past components
	 */
	private final List<Part> pastComponents = new ArrayList<>();

	/**
	 * The current component being created
	 */
	@Nullable
	private Part currentComponent;

	/**
	 * The part that is being created
	 */
	@RequiredArgsConstructor
	public static final class Part {

		/**
		 * The text
		 */
		private final String text;

		/**
		 * The view permission
		 */
		private String viewPermission;

		/**
		 * The view JS condition
		 */
		private String viewCondition;

		/**
		 * The hover event
		 */
		private HoverEvent hoverEvent;

		/**
		 * The click event
		 */
		private ClickEvent clickEvent;

		/**
		 * The insertion
		 */
		private String insertion;

		/**
		 * What component to inherit colors/decoration from?
		 */
		@Nullable
		private BaseComponent inheritFormatting;

		/**
		 * Convert this part into a text component
		 *
		 * @return
		 * @deprecated receiver-specific formatting will be lost and forced
		 */
		@Deprecated
		public TextComponent toTextComponent() {
			return toTextComponent(null);
		}

		/**
		 * Turn this part of the components into a {@link TextComponent}
		 * for the given receiver
		 *
		 * @param receiver
		 * @return
		 */
		private TextComponent toTextComponent(CommandSender receiver) {
			if (!canSendTo(receiver))
				return new TextComponent("");

			final BaseComponent[] base = toComponent(this.text, this.inheritFormatting);

			for (final BaseComponent part : base) {
				if (this.hoverEvent != null)
					part.setHoverEvent(hoverEvent);

				if (this.clickEvent != null)
					part.setClickEvent(clickEvent);

				if (this.insertion != null)
					part.setInsertion(insertion);
			}

			return new TextComponent(base);
		}

		/*
		 * Can this component be shown to the given sender?
		 */
		private boolean canSendTo(@Nullable CommandSender receiver) {
			if (receiver == null)
				return true;

			if (this.viewPermission != null && !PlayerUtil.hasPerm(receiver, this.viewPermission))
				return false;

			if (this.viewCondition != null) {
				final Object result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);
				Valid.checkBoolean(result instanceof Boolean, "Receiver condition must return Boolean not " + result.getClass() + " for component: " + this);

				if ((boolean) result == false)
					return false;
			}

			return true;
		}
	}

	/**
	 * Create a new interactive chat component
	 *
	 * @param text
	 */
	private SimpleComponent(String text) {
		this.currentComponent = new Part(text);
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
	 * <p>
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
		this.currentComponent.hoverEvent = new HoverEvent(action, TextComponent.fromLegacyText(text));

		return this;
	}

	/**
	 * Set view permission for last component part
	 *
	 * @param viewPermission
	 * @return
	 */
	public SimpleComponent viewPermission(String viewPermission) {
		this.currentComponent.viewPermission = viewPermission;

		return this;
	}

	/**
	 * Set view permission for last component part
	 *
	 * @param viewCondition
	 * @return
	 */
	public SimpleComponent viewCondition(String viewCondition) {
		this.currentComponent.viewCondition = viewCondition;

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
		this.currentComponent.clickEvent = new ClickEvent(action, text);

		return this;
	}

	/**
	 * Invoke {@link TextComponent#setInsertion(String)} for {@link #currentComponents}
	 *
	 * @param insertion
	 * @return
	 */
	public SimpleComponent onClickInsert(String insertion) {
		this.currentComponent.insertion = insertion;

		return this;
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Append new component at the beginning of all components
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent appendFirst(SimpleComponent component) {
		this.pastComponents.addAll(0, component.pastComponents);

		return this;
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent append(String text) {
		return this.append(text, true);
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(String text, boolean colorize) {
		return this.append(text, null, colorize);
	}

	/**
	 * Create another component. The current is put in a list of past components
	 * so next time you use onClick or onHover, you will be added the event to the new one
	 * specified here
	 *
	 * @param text
	 * @param inheritFormatting
	 * @return
	 */
	public SimpleComponent append(String text, BaseComponent inheritFormatting) {
		return this.append(text, inheritFormatting, true);
	}

	/**
	 * Create another component. The current is put in a list of past components
	 * so next time you use onClick or onHover, you will be added the event to the new one
	 * specified here
	 *
	 * @param text
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(String text, BaseComponent inheritFormatting, boolean colorize) {

		// Get the last extra
		BaseComponent inherit = inheritFormatting != null ? inheritFormatting : this.currentComponent.toTextComponent();

		if (inherit.getExtra() != null && !inherit.getExtra().isEmpty())
			inherit = inherit.getExtra().get(inherit.getExtra().size() - 1);

		this.pastComponents.add(this.currentComponent);
		this.currentComponent = new Part(colorize ? Common.colorize(text) : text);
		this.currentComponent.inheritFormatting = inherit;

		return this;
	}

	/**
	 * Append a new component on the end of this one
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent append(SimpleComponent component) {
		this.pastComponents.addAll(component.pastComponents);

		this.pastComponents.add(this.currentComponent);
		this.currentComponent = component.currentComponent;

		return this;
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events
	 *
	 * @return
	 */
	public String getPlainMessage() {
		return build(null).toLegacyText();
	}

	/**
	 * Builds the component and its past components into a {@link TextComponent}
	 *
	 * @return
	 */
	public TextComponent getTextComponent() {
		return build(null);
	}

	/**
	 * Builds the component and its past components into a {@link TextComponent}
	 *
	 * @param receiver
	 * @return
	 */
	public TextComponent build(CommandSender receiver) {
		final TextComponent preparedComponent = new TextComponent("");

		for (final Part part : this.pastComponents)
			preparedComponent.addExtra(part.toTextComponent(receiver));

		preparedComponent.addExtra(this.currentComponent.toTextComponent(receiver));

		return preparedComponent;
	}

	/**
	 * Return all past components, immutable
	 *
	 * @return the components
	 */
	public List<SimpleComponent.Part> getPreviousComponents() {
		return Collections.unmodifiableList(this.pastComponents);
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param receiver
	 */
	public <T extends CommandSender> void send(T... receivers) {
		this.send(Arrays.asList(receivers));
	}

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param <T>
	 * @param receiver
	 */
	public <T extends CommandSender> void send(Iterable<T> receivers) {
		this.sendAs(null, receivers);
	}

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * We will also replace relation placeholders if the sender is set and is player.
	 *
	 * @param <T>
	 * @param receiver
	 */
	public <T extends CommandSender> void sendAs(@Nullable CommandSender sender, Iterable<T> receivers) {
		for (final CommandSender receiver : receivers) {
			final TextComponent component = new TextComponent(build(receiver));

			if (receiver instanceof Player && sender instanceof Player)
				setRelationPlaceholders(component, (Player) receiver, (Player) sender);

			Remain.sendComponent(receiver, component);
		}
	}

	/*
	 * Replace relationship placeholders in the full component and all of its extras
	 */
	private void setRelationPlaceholders(final TextComponent component, final Player receiver, final Player sender) {

		// Set the main text
		component.setText(HookManager.replaceRelationPlaceholders(sender, receiver, component.getText()));

		if (component.getExtra() == null)
			return;

		for (final BaseComponent extra : component.getExtra())
			if (extra instanceof TextComponent) {

				final TextComponent text = (TextComponent) extra;
				final ClickEvent clickEvent = text.getClickEvent();
				final HoverEvent hoverEvent = text.getHoverEvent();

				// Replace for the text itself
				//text.setText(HookManager.replaceRelationPlaceholders(sender, receiver, text.getText()));

				// And for the click event
				if (clickEvent != null)
					text.setClickEvent(new ClickEvent(clickEvent.getAction(), HookManager.replaceRelationPlaceholders(sender, receiver, clickEvent.getValue())));

				// And for the hover event
				if (hoverEvent != null)
					for (final BaseComponent hoverBaseComponent : hoverEvent.getValue())
						if (hoverBaseComponent instanceof TextComponent) {
							final TextComponent hoverTextComponent = (TextComponent) hoverBaseComponent;

							hoverTextComponent.setText(HookManager.replaceRelationPlaceholders(sender, receiver, hoverTextComponent.getText()));
						}

				// Then repeat for the extra parts in the text itself
				setRelationPlaceholders(text, receiver, sender);
			}
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * Compile the message into components, creating a new {@link PermissibleComponent}
	 * each time the message has a new & color/formatting, preserving
	 * the last color
	 *
	 * @param message
	 * @param inheritFormatting
	 * @param viewPermission
	 * @return
	 */
	private static TextComponent[] toComponent(@NonNull String message, @Nullable BaseComponent inheritFormatting) {
		final List<TextComponent> components = new ArrayList<>();

		// TODO being able to be used in {message}
		if (Common.stripColors(message).startsWith("<center>"))
			message = ChatUtil.center(message.replace("<center>", ""));

		// Plot the previous formatting manually before the message to retain it
		if (inheritFormatting != null) {
			if (inheritFormatting.isBold())
				message = ChatColor.BOLD + message;

			if (inheritFormatting.isItalic())
				message = ChatColor.ITALIC + message;

			if (inheritFormatting.isObfuscated())
				message = ChatColor.MAGIC + message;

			if (inheritFormatting.isStrikethrough())
				message = ChatColor.STRIKETHROUGH + message;

			if (inheritFormatting.isUnderlined())
				message = ChatColor.UNDERLINE + message;

			message = inheritFormatting.getColor() + message;
		}

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

				ChatColor format;

				if (c == 'x' && i + 12 < message.length()) {
					final StringBuilder hex = new StringBuilder("#");

					for (int j = 0; j < 6; j++)
						hex.append(message.charAt(i + 2 + (j * 2)));

					try {
						format = ChatColor.of(hex.toString());

					} catch (final IllegalArgumentException ex) {
						format = null;
					}

					i += 12;

				} else
					format = ChatColor.getByChar(c);

				if (format == null)
					continue;

				if (builder.length() > 0) {
					final TextComponent old = component;
					component = new TextComponent(old);
					old.setText(builder.toString());
					builder = new StringBuilder();
					components.add(old);
				}

				if (format == ChatColor.BOLD)
					component.setBold(true);

				else if (format == ChatColor.ITALIC)
					component.setItalic(true);

				else if (format == ChatColor.UNDERLINE)
					component.setUnderlined(true);

				else if (format == ChatColor.STRIKETHROUGH)
					component.setStrikethrough(true);

				else if (format == ChatColor.MAGIC)
					component.setObfuscated(true);

				else if (format == ChatColor.RESET) {
					format = ChatColor.WHITE;

					component = new TextComponent();
					component.setColor(format);

				} else {
					component = new TextComponent();

					component.setColor(format);
				}

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
					components.add(old);
				}

				final TextComponent old = component;
				component = new TextComponent(old);

				final String urlString = message.substring(i, pos);
				component.setText(urlString);
				component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlString.startsWith("http") ? urlString : "http://" + urlString));
				components.add(component);

				i += pos - i - 1;
				component = old;

				continue;
			}

			builder.append(c);
		}

		component.setText(builder.toString());
		components.add(component);

		return components.toArray(new TextComponent[components.size()]);
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param text
	 * @return
	 */
	public static SimpleComponent empty() {
		return of(true, "");
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(String text) {
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
	public static SimpleComponent of(boolean colorize, String text) {
		return new SimpleComponent(text == null ? "" : colorize ? Common.colorize(text) : text);
	}
}
