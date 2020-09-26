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
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * A very simple way of sending interactive chat messages
 */
public final class SimpleComponent {

	/**
	 * The pattern to match URL addresses when parsing text
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

	/**
	 * The past components having different hover/click events
	 */
	private final List<PermissibleComponent> pastComponents = new ArrayList<>();

	/**
	 * The current component that is being modified
	 */
	private PermissibleComponent[] currentComponents;

	/**
	 * Create a new interactive chat component
	 *
	 * @param text
	 */
	private SimpleComponent(String... text) {
		this.currentComponents = fromLegacyText(String.join("\n", Common.colorize(text)), null);
	}

	/**
	 * Represents a component with viewing permission
	 */
	@RequiredArgsConstructor
	@Setter
	public static class PermissibleComponent {

		@Getter
		private final BaseComponent component;

		private String viewPermission;
		private String viewCondition;

		public boolean canSendTo(@Nullable CommandSender receiver) {
			if (receiver == null)
				return true;

			if (this.viewPermission != null && !PlayerUtil.hasPerm(receiver, this.viewPermission))
				return false;

			if (this.viewCondition != null) {
				final Object result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);
				Valid.checkBoolean(result instanceof Boolean, "Receiver condition must return Boolean not " + result.getClass() + " for component: " + component.toLegacyText());

				if ((boolean) result == false)
					return false;
			}

			return true;
		}
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
		final List<BaseComponent> baseComponents = Common.convert(Arrays.asList(fromLegacyText(Common.colorize(text), null)), PermissibleComponent::getComponent);

		for (final PermissibleComponent component : currentComponents)
			component.getComponent().setHoverEvent(new HoverEvent(action, baseComponents.toArray(new BaseComponent[baseComponents.size()])));

		return this;
	}

	public SimpleComponent viewPermission(String viewPermission) {
		for (final PermissibleComponent component : currentComponents)
			component.setViewPermission(viewPermission);

		return this;
	}

	public SimpleComponent viewCondition(String javaScriptViewCondition) {
		for (final PermissibleComponent component : currentComponents)
			component.setViewCondition(javaScriptViewCondition);

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
			component.getComponent().setClickEvent(new ClickEvent(action, text));

		return this;
	}

	/**
	 * Invoke {@link TextComponent#setInsertion(String)} for {@link #currentComponents}
	 *
	 * @param insertion
	 * @return
	 */
	public SimpleComponent onClickInsert(String insertion) {
		for (final PermissibleComponent component : currentComponents)
			component.getComponent().setInsertion(insertion);

		return this;
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	public SimpleComponent appendFirst(SimpleComponent component) {
		pastComponents.add(0, new PermissibleComponent(component.getTextComponent()));

		return this;
	}

	/**
	 * Append a new component on the end of this one
	 *
	 * @param newComponent
	 * @return
	 */
	public SimpleComponent append(SimpleComponent newComponent) {
		for (final PermissibleComponent baseComponent : currentComponents)
			pastComponents.add(baseComponent);

		currentComponents = new PermissibleComponent[] { new PermissibleComponent(newComponent.build(null)) };

		return this;
	}

	public SimpleComponent append(String text) {
		return append(text, null);
	}

	public SimpleComponent appendWithOptions(String text, AppendOption... options) {
		return append(text, null, options);
	}

	/**
	 * Create another component. The current is put in a list of past components
	 * so next time you use onClick or onHover, you will be added the event to the new one
	 * specified here
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent append(String text, BaseComponent compotentToInheritFormattingFrom, AppendOption... options) {
		final List<AppendOption> optionsList = Arrays.asList(options);

		// Copy the last color to reuse in the next component
		BaseComponent lastComponentFormatting = null;

		for (int i = 0; i < currentComponents.length; i++) {
			final PermissibleComponent baseComponent = currentComponents[i];
			pastComponents.add(baseComponent);

			if (!optionsList.contains(AppendOption.DO_NOT_INHERIT_FORMAT)) {
				if (compotentToInheritFormattingFrom != null) {
					final List<BaseComponent> extra = compotentToInheritFormattingFrom.getExtra();

					lastComponentFormatting = extra != null ? extra.get(extra.size() - 1) : compotentToInheritFormattingFrom;
				} else
					lastComponentFormatting = baseComponent.getComponent();
			}
		}

		currentComponents = fromLegacyText(!optionsList.contains(AppendOption.DO_NOT_COLORIZE) ? Common.colorize(text) : text, lastComponentFormatting);

		return this;
	}

	/**
	 * Form a single {@link TextComponent} out of all components created
	 *
	 * @return
	 */
	private TextComponent build(CommandSender receiver) {
		final TextComponent mainComponent = new TextComponent("");

		for (final PermissibleComponent pastComponent : pastComponents)
			if (pastComponent.canSendTo(receiver))
				mainComponent.addExtra(pastComponent.getComponent());

		for (final PermissibleComponent currentComponent : currentComponents)
			if (currentComponent.canSendTo(receiver))
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
		return build(null).toLegacyText();
	}

	/**
	 * Return md_5 {@link TextComponent} object
	 *
	 * @return
	 */
	public TextComponent getTextComponent() {
		return build(null);
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
	 * @param <T>
	 * @param receiver
	 */
	public <T extends CommandSender> void send(Iterable<T> receivers) {
		for (final CommandSender receiver : receivers) {
			final TextComponent mainComponent = build(receiver);

			Remain.sendComponent(receiver, mainComponent);
		}
	}

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param receiver
	 */
	public <T extends CommandSender> void send(T... receivers) {
		for (final CommandSender receiver : receivers) {
			final TextComponent mainComponent = build(receiver);

			Remain.sendComponent(receiver, mainComponent);
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
	private static PermissibleComponent[] fromLegacyText(@NonNull String message, @Nullable BaseComponent inheritFormatting) {
		final List<PermissibleComponent> components = new ArrayList<>();

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

				ChatColor format = ChatColor.getByChar(c);

				if (format == null)
					continue;

				if (builder.length() > 0) {
					final TextComponent old = component;

					component = new TextComponent(old);
					old.setText(builder.toString());

					builder = new StringBuilder();
					components.add(new PermissibleComponent(old));
				}

				switch (format.getName().toUpperCase()) {
					case "BOLD":
						component.setBold(true);
						break;
					case "ITALIC":
						component.setItalic(true);
						break;
					case "UNDERLINE":
						component.setUnderlined(true);
						break;
					case "STRIKETHROUGH":
						component.setStrikethrough(true);
						break;
					case "OBFUSCATED":
						component.setObfuscated(true);
						break;
					case "MAGIC":
						component.setObfuscated(true);
						break;
					case "RESET":
						format = ChatColor.RESET;

					default:
						component = new TextComponent();
						component.setColor(format);

						if (format == ChatColor.RESET) {
							component.setBold(false);
							component.setItalic(false);
							component.setUnderlined(false);
							component.setStrikethrough(false);
							component.setObfuscated(false);
						}

						break;
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
					components.add(new PermissibleComponent(old));
				}

				final TextComponent old = component;
				component = new TextComponent(old);

				final String urlString = message.substring(i, pos);
				component.setText(urlString);
				component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlString.startsWith("http") ? urlString : "http://" + urlString));
				components.add(new PermissibleComponent(component));

				i += pos - i - 1;
				component = old;

				continue;
			}

			builder.append(c);
		}

		component.setText(builder.toString());
		components.add(new PermissibleComponent(component));

		return components.stream().toArray(PermissibleComponent[]::new);
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param colorize
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(String... text) {
		return new SimpleComponent(text);
	}

	public enum AppendOption {
		DO_NOT_COLORIZE,
		DO_NOT_INHERIT_FORMAT;
	}
}
