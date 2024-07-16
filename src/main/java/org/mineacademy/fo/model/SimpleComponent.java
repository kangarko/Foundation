package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;

/**
 * A very simple way of sending interactive chat messages
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class SimpleComponent implements ConfigSerializable {

	/**
	 * The component we are creating
	 */
	private final List<ConditionalComponent> components = new ArrayList<>();

	/**
	 * The optional sender of this component
	 */
	@Nullable
	private CommandSender sender;

	/**
	 * Shall this component ignore empty components? Defaults to false
	 */
	@Getter
	private boolean ignoreEmpty = false; // TODO test on false

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a show text event
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(final Collection<String> lines) {
		return this.onHover(String.join("\n", lines));
	}

	/**
	 * Add a show text hover event
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(final String... lines) {
		return this.onHover(String.join("\n", lines));
	}

	/**
	 * Add a show text event
	 *
	 * @param hover
	 * @return
	 */
	public SimpleComponent onHover(final String hover) {
		this.modifyLastComponent(component -> component.hoverEvent(Remain.convertLegacyToAdventure(Common.colorize(hover))));

		return this;
	}

	/**
	 * Add a hover event
	 *
	 * @param hover
	 * @return
	 */
	public SimpleComponent onHover(final HoverEventSource<?> hover) {
		this.modifyLastComponent(component -> component.hoverEvent(hover));

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
	public SimpleComponent onHover(@NonNull final ItemStack item) {
		if (CompMaterial.isAir(item.getType()))
			return this.onHover("Air");

		try {
			this.modifyLastComponent(component -> component.hoverEvent(Remain.toHoverEvent(item)));

		} catch (final Throwable t) {
			Common.logFramed(
					"Error parsing ItemStack to simple component!",
					"Item: " + item,
					"Error: " + t.getMessage());

			t.printStackTrace();
		}

		return this;
	}

	/**
	 * Add a run command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickRunCmd(final String text) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.runCommand(text)));

		return this;
	}

	/**
	 * Add a suggest command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickSuggestCmd(final String text) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.suggestCommand(text)));

		return this;
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickOpenUrl(final String url) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.openUrl(url)));

		return this;
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickCopyToClipboard(final String url) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.copyToClipboard(url)));

		return this;
	}

	/**
	 * Invoke Component setInsertion
	 *
	 * @param insertion
	 * @return
	 */
	public SimpleComponent onClickInsert(final String insertion) {
		this.modifyLastComponent(component -> component.insertion(insertion));

		return this;
	}

	/**
	 * Set the view condition for this component
	 *
	 * @param viewCondition
	 * @return
	 */
	public SimpleComponent viewCondition(final String viewCondition) {
		this.getLastComponent().setViewCondition(viewCondition);

		return this;
	}

	/**
	 * Set the view permission for this component
	 *
	 * @param viewPermission
	 * @return
	 */
	public SimpleComponent viewPermission(final String viewPermission) {
		this.getLastComponent().setViewPermission(viewPermission);

		return this;
	}

	/**
	 * Quickly replaces an object in all parts of this component
	 *
	 * @param variable the factual variable - you must supply brackets
	 * @param value
	 * @return
	 */
	public SimpleComponent replace(final String variable, final String value) {
		for (final ConditionalComponent part : this.components)
			part.setComponent(part.getComponent().replaceText(b -> b.matchLiteral(variable).replacement(value)));

		return this;
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent append(final String text) {
		return this.append(text, true);
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(final String text, final boolean colorize) {
		return this.append(Remain.convertLegacyToAdventure(colorize ? Common.colorize(text) : text));
	}

	/**
	 * Append a new simple component
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent append(final SimpleComponent component) {
		for (final ConditionalComponent part : component.components)
			this.components.add(part);

		return this;
	}

	/**
	 * Append a new component on the end of this one
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent append(final Component component) {
		this.components.add(ConditionalComponent.fromComponent(component));

		return this;
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events
	 *
	 * @return
	 */
	public String getLegacyText() {
		return Remain.convertAdventureToLegacy(this.build(null));
	}

	/**
	 *
	 * @return
	 */
	public String getJson() {
		return Remain.convertAdventureToJson(this.build(null));
	}

	/**
	 * Return the component
	 *
	 * @return
	 */
	public Component getComponent() {
		return this.build(null);
	}

	/**
	 * Return the component
	 *
	 * @param receiver
	 * @return
	 */
	public Component getComponent(CommandSender receiver) {
		return this.build(receiver);
	}

	/**
	 * Return the component
	 *
	 * @param sender
	 * @param receiver
	 * @return
	 */
	public Component getComponent(CommandSender sender, CommandSender receiver) {
		this.setSender(sender);

		return this.build(receiver);
	}

	/*
	 * Convert into Adventure component
	 */
	private Component build(CommandSender receiver) {
		Component main = Component.empty();

		for (final ConditionalComponent part : this.components) {
			final Component component = part.build(receiver);

			if (component != null)
				main = main.append(component);
		}

		return HookManager.replaceRelationPlaceholders(this.sender, receiver, main);
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
	public <T extends CommandSender> void send(final T... receivers) {
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
	public <T extends CommandSender> void send(final Iterable<T> receivers) {
		for (final CommandSender receiver : receivers)
			Remain.tell(receiver, this.build(receiver), this.ignoreEmpty);
	}

	/**
	 * Set the sender of this component
	 *
	 * @param sender
	 * @return
	 */
	public SimpleComponent setSender(final CommandSender sender) {
		this.sender = sender;

		return this;
	}

	/**
	 * Set if this component should ignore empty components? Defaults to false
	 *
	 * @param ignoreEmpty
	 * @return
	 */
	public SimpleComponent setIgnoreEmpty(final boolean ignoreEmpty) {
		this.ignoreEmpty = ignoreEmpty;

		return this;
	}

	/*
	 * Get the last component or throws an error if none found
	 */
	private ConditionalComponent getLastComponent() {
		Valid.checkBoolean(this.components.size() > 0, "No components found!");

		return this.components.get(this.components.size() - 1);
	}

	/*
	 * Helper method to modify the last component
	 */
	private void modifyLastComponent(Function<Component, Component> editor) {
		final ConditionalComponent last = this.getLastComponent();

		last.setComponent(editor.apply(last.getComponent()));
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.of("Components", this.components);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getJson();
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

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
	public static SimpleComponent of(final String text) {
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
		final SimpleComponent simpleComponent = new SimpleComponent();

		// Inject the center element here already
		if (text.contains("<center>"))
			text = ChatUtil.center(text.replace("<center>", "").trim());

		if (colorize)
			text = Common.colorize(text);

		simpleComponent.components.add(ConditionalComponent.fromLegacy(text));

		return simpleComponent;
	}

	/**
	 * Create a new interactive chat component
	 *
	 * @param component
	 * @return
	 */
	public static SimpleComponent of(Component component) {
		final SimpleComponent simpleComponent = new SimpleComponent();

		simpleComponent.components.add(ConditionalComponent.fromComponent(component));

		return simpleComponent;
	}

	/**
	 *
	 * @param json
	 * @return
	 */
	public static SimpleComponent fromJson(String json) {
		return deserialize(SerializedMap.fromJson(json));
	}

	/**
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(SerializedMap map) {
		final SimpleComponent component = new SimpleComponent();

		component.components.addAll(map.getList("Components", ConditionalComponent.class));

		return component;
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	@Setter
	@Getter
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	static final class ConditionalComponent implements ConfigSerializable {

		private Component component;
		private String viewPermission;
		private String viewCondition;

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Component", this.component);
			map.putIf("Permission", this.viewPermission);
			map.putIf("Condition", this.viewCondition);

			return map;
		}

		public static ConditionalComponent deserialize(final SerializedMap map) {
			final ConditionalComponent part = new ConditionalComponent();

			part.component = map.get("Component", Component.class);
			part.viewPermission = map.getString("Permission");
			part.viewCondition = map.getString("Condition");

			return part;
		}

		/*
		 * Build the component for the given receiver
		 */
		private Component build(@Nullable CommandSender receiver) {

			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !receiver.hasPermission(this.viewPermission)))
				return null;

			if (this.viewCondition != null && !this.viewCondition.isEmpty()) {
				if (receiver == null)
					return null;

				try {
					final Object result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);

					if (result != null) {
						Valid.checkBoolean(result instanceof Boolean, "View condition must return Boolean not " + (result == null ? "null" : result.getClass()) + " for component: " + this);

						if (!((boolean) result))
							return null;
					}

				} catch (final FoScriptException ex) {
					Common.logFramed(
							"Failed parsing view condition for component!",
							"",
							"The view condition must be a JavaScript code that returns a boolean!",
							"Component: " + this,
							"Line: " + ex.getErrorLine(),
							"Error: " + ex.getMessage());

					throw ex;
				}
			}

			return this.component;
		}

		@Override
		public String toString() {
			return this.serialize().toStringFormatted();
		}

		/**
		 * Create a new component from a legacy text
		 *
		 * @param text
		 * @return
		 */
		static ConditionalComponent fromLegacy(String text) {
			final ConditionalComponent part = new ConditionalComponent();

			part.component = Remain.convertLegacyToAdventure(text);

			return part;
		}

		/**
		 * Create a new component from a component
		 *
		 * @param component
		 * @return
		 */
		static ConditionalComponent fromComponent(Component component) {
			final ConditionalComponent part = new ConditionalComponent();

			part.component = component;

			return part;
		}
	}
}
