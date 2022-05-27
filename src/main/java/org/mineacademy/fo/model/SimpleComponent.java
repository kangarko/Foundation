package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

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
public final class SimpleComponent implements ConfigSerializable {

	/**
	 * Prevent oversized JSON from kicking players by removing interactive elements from it?
	 */
	public static boolean STRIP_OVERSIZED_COMPONENTS = true;

	/**
	 * The pattern to match URL addresses when parsing text
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("^(https?)://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*[-a-zA-Z\\d]?([^&]+[^\\da-fk-orA-FK-OR])?$");

	/**
	 * The past components
	 */
	private final List<Part> pastComponents = new ArrayList<>();

	/**
	 * The current component being created
	 */
	private Part currentComponent;

	/**
	 * Create a new interactive chat component
	 *
	 * @param text
	 */
	private SimpleComponent(String text) {

		// Inject the center element here already
		if (Common.stripColors(text).startsWith("<center>"))
			text = ChatUtil.center(text.replace("<center>", "").trim());

		this.currentComponent = new Part(text);
	}

	/**
	 * Private constructor used when deserializing
	 */
	private SimpleComponent() {
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a show text event
	 *
	 * @param texts
	 * @return
	 */
	public SimpleComponent onHover(Collection<String> texts) {
		return this.onHover(Common.toArray(texts));
	}

	/**
	 * Add a show text hover event
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(String... lines) {
		// I don't know why we have to wrap this inside new text component but we do this
		// to properly reset bold and other decoration colors
		final String joined = Common.colorize(String.join("\n", lines));
		this.currentComponent.hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { new TextComponent(TextComponent.fromLegacyText(joined)) });

		return this;
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
		if (CompMaterial.isAir(item.getType()))
			return this.onHover("Air");

		this.currentComponent.hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[] { new TextComponent(Remain.toJson(item)) });

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
	 * Add a run command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickRunCmd(String text) {
		return this.onClick(Action.RUN_COMMAND, text);
	}

	/**
	 * Add a suggest command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickSuggestCmd(String text) {
		return this.onClick(Action.SUGGEST_COMMAND, text);
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickOpenUrl(String url) {
		return this.onClick(Action.OPEN_URL, url);
	}

	/**
	 * Add a command event
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
	 * Invoke {@link TextComponent#setInsertion(String)}
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
		this.pastComponents.add(0, component.currentComponent);
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
	 * @param inheritFormatting
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(String text, BaseComponent inheritFormatting, boolean colorize) {

		// Get the last extra
		BaseComponent inherit = inheritFormatting != null ? inheritFormatting : this.currentComponent.toTextComponent(false, null);

		if (inherit != null && inherit.getExtra() != null && !inherit.getExtra().isEmpty())
			inherit = inherit.getExtra().get(inherit.getExtra().size() - 1);

		// Center text for each line separately if replacing colors
		if (colorize) {
			final List<String> formatContents = Arrays.asList(text.split("\n"));

			for (int i = 0; i < formatContents.size(); i++) {
				final String line = formatContents.get(i);

				if (Common.stripColors(line).startsWith("<center>"))
					formatContents.set(i, ChatUtil.center(line.replace("<center>", "")));
			}

			text = String.join("\n", formatContents);
		}

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
		this.pastComponents.add(this.currentComponent);
		this.pastComponents.addAll(component.pastComponents);

		// Get the last extra
		BaseComponent inherit = Common.getOrDefault(component.currentComponent.inheritFormatting, this.currentComponent.toTextComponent(false, null));

		if (inherit != null && inherit.getExtra() != null && !inherit.getExtra().isEmpty())
			inherit = inherit.getExtra().get(inherit.getExtra().size() - 1);

		this.currentComponent = component.currentComponent;
		this.currentComponent.inheritFormatting = inherit;

		return this;
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events
	 *
	 * @return
	 */
	public String getPlainMessage() {
		return this.build(null).toLegacyText();
	}

	/**
	 * Builds the component and its past components into a {@link TextComponent}
	 *
	 * @return
	 */
	public TextComponent getTextComponent() {
		return this.build(null);
	}

	/**
	 * Builds the component and its past components into a {@link TextComponent}
	 *
	 * @param receiver
	 * @return
	 */
	public TextComponent build(CommandSender receiver) {
		TextComponent preparedComponent = null;

		for (final Part part : this.pastComponents) {
			final TextComponent component = part.toTextComponent(true, receiver);

			if (component != null)
				if (preparedComponent == null)
					preparedComponent = component;
				else
					preparedComponent.addExtra(component);
		}

		final TextComponent currentComponent = this.currentComponent.toTextComponent(true, receiver);

		if (currentComponent != null)
			if (preparedComponent == null)
				preparedComponent = currentComponent;
			else
				preparedComponent.addExtra(currentComponent);

		return Common.getOrDefault(preparedComponent, new TextComponent(""));
	}

	/**
	 * Quickly replaces an object in all parts of this component
	 *
	 * @param variable the factual variable - you must supply brackets
	 * @param value
	 * @return
	 */
	public SimpleComponent replace(String variable, Object value) {
		final String serialized = SerializeUtil.serialize(value).toString();

		for (final Part part : this.pastComponents) {
			Valid.checkNotNull(part.text);

			part.text = part.text.replace(variable, serialized);
		}

		Valid.checkNotNull(this.currentComponent.text);
		this.currentComponent.text = this.currentComponent.text.replace(variable, serialized);

		return this;
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 * @param receivers
	 * @param <T>
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
	 * @param receivers
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
	 * @param sender
	 * @param receivers
	 */
	public <T extends CommandSender> void sendAs(CommandSender sender, Iterable<T> receivers) {
		for (final CommandSender receiver : receivers) {
			final TextComponent component = this.build(receiver);

			if (receiver instanceof Player && sender instanceof Player)
				this.setRelationPlaceholders(component, (Player) receiver, (Player) sender);

			// Prevent clients being kicked out, so we just send plain message instead
			if (STRIP_OVERSIZED_COMPONENTS && Remain.toJson(component).length() + 1 >= Short.MAX_VALUE) {
				final String legacy = Common.colorize(component.toLegacyText());

				if (legacy.length() + 1 >= Short.MAX_VALUE)
					Common.warning("JSON Message to " + receiver.getName() + " was too large and could not be sent: '" + legacy + "'");

				else {
					Common.warning("JSON Message to " + receiver.getName() + " was too large, removing interactive elements to avoid kick. Sending plain: '" + legacy + "'");

					receiver.sendMessage(legacy);
				}

			} else
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
				this.setRelationPlaceholders(text, receiver, sender);
			}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.serialize().toStringFormatted();
	}

	// --------------------------------------------------------------------
	// Serialize
	// --------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIf("Current_Component", this.currentComponent);
		map.put("Past_Components", this.pastComponents);

		return map;
	}

	/**
	 * Create a {@link SimpleComponent} from the serialized map
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(SerializedMap map) {
		final SimpleComponent component = new SimpleComponent();

		component.currentComponent = map.get("Current_Component", Part.class);
		component.pastComponents.addAll(map.getList("Past_Components", Part.class));

		return component;
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
	private static TextComponent[] toComponent(@NonNull String message, BaseComponent inheritFormatting) {
		final List<TextComponent> components = new ArrayList<>();

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

		StringBuilder builder = new StringBuilder();
		TextComponent component = new TextComponent();

		for (int index = 0; index < message.length(); index++) {
			char letter = message.charAt(index);

			if (letter == ChatColor.COLOR_CHAR) {
				if (++index >= message.length())
					break;

				letter = message.charAt(index);

				if (letter >= 'A' && letter <= 'Z')
					letter += 32;

				ChatColor format;

				if (letter == 'x' && index + 12 < message.length()) {
					final StringBuilder hex = new StringBuilder("#");

					for (int j = 0; j < 6; j++)
						hex.append(message.charAt(index + 2 + (j * 2)));

					try {
						format = ChatColor.of(hex.toString());

					} catch (NoSuchMethodError | IllegalArgumentException ex) {
						format = null;
					}

					index += 12;

				} else
					format = ChatColor.getByChar(letter);

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

					/*component.setBold(false);
					component.setItalic(false);
					component.setStrikethrough(false);
					component.setUnderlined(false);*/

				} else {
					component = new TextComponent();

					component.setColor(format);
				}

				continue;
			}

			int pos = message.indexOf(' ', index);

			if (pos == -1)
				pos = message.length();

			// Web link handling
			if (URL_PATTERN.matcher(message).region(index, pos).find()) {

				if (builder.length() > 0) {
					final TextComponent old = component;
					component = new TextComponent(old);

					old.setText(builder.toString());

					builder = new StringBuilder();
					components.add(old);
				}

				final TextComponent old = component;
				component = new TextComponent(old);

				final String urlString = message.substring(index, pos);
				component.setText(urlString);
				component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlString.startsWith("http") ? urlString : "http://" + urlString));
				components.add(component);

				index += pos - index - 1;
				component = old;

				continue;
			}

			builder.append(letter);
		}

		component.setText(builder.toString());
		components.add(component);

		//return components.toArray(new TextComponent[components.size()]);
		return new TextComponent[] { new TextComponent(components.toArray(new TextComponent[components.size()])) };
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
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
		return new SimpleComponent(colorize ? Common.colorize(text) : text);
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	/**
	 * The part that is being created
	 */
	static final class Part implements ConfigSerializable {

		/**
		 * The text
		 */
		private String text;

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

		private BaseComponent inheritFormatting;

		/*
		 * Create a new part
		 */
		private Part(String text) {
			Valid.checkNotNull(text, "Part text cannot be null");

			this.text = text;
		}

		/**
		 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
		 */
		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Text", this.text);
			map.putIf("View_Permission", this.viewPermission);
			map.putIf("View_Condition", this.viewCondition);
			map.putIf("Hover_Event", this.hoverEvent);
			map.putIf("Click_Event", this.clickEvent);
			map.putIf("Insertion", this.insertion);
			map.putIf("Inherit_Formatting", this.inheritFormatting);

			return map;
		}

		/**
		 * Create a Part from the given serializedMap
		 *
		 * @param map
		 * @return
		 */
		public static Part deserialize(SerializedMap map) {
			final Part part = new Part(map.getString("Text"));

			part.viewPermission = map.getString("View_Permission");
			part.viewCondition = map.getString("View_Condition");
			part.hoverEvent = map.get("Hover_Event", HoverEvent.class);
			part.clickEvent = map.get("Click_Event", ClickEvent.class);
			part.insertion = map.getString("Insertion");
			part.inheritFormatting = map.get("Inherit_Formatting", BaseComponent.class);

			return part;
		}

		/**
		 * Turn this part of the components into a {@link TextComponent}
		 * for the given receiver
		 *
		 * @param checkForReceiver
		 * @param receiver
		 * @return
		 */
		private TextComponent toTextComponent(boolean checkForReceiver, CommandSender receiver) {
			if ((checkForReceiver && !this.canSendTo(receiver)) || this.isEmpty())
				return null;

			final List<BaseComponent> base = toComponent(this.text, this.inheritFormatting)[0].getExtra();

			for (final BaseComponent part : base) {
				if (this.hoverEvent != null)
					part.setHoverEvent(this.hoverEvent);

				if (this.clickEvent != null)
					part.setClickEvent(this.clickEvent);

				if (this.insertion != null)
					part.setInsertion(this.insertion);
			}

			return new TextComponent(base.toArray(new BaseComponent[base.size()]));
		}

		/*
		 * Return if we're dealing with an empty format
		 */
		private boolean isEmpty() {
			return this.text.isEmpty() && this.hoverEvent == null && this.clickEvent == null && this.insertion == null;
		}

		/*
		 * Can this component be shown to the given sender?
		 */
		private boolean canSendTo(CommandSender receiver) {

			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !PlayerUtil.hasPerm(receiver, this.viewPermission)))
				return false;

			if (this.viewCondition != null && !this.viewCondition.isEmpty()) {
				if (receiver == null)
					return false;

				final Object result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "View condition must return Boolean not " + (result == null ? "null" : result.getClass()) + " for component: " + this);

					if (!((boolean) result))
						return false;
				}
			}

			return true;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return this.serialize().toStringFormatted();
		}
	}
}
