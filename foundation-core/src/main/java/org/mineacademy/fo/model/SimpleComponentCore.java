package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.RemainCore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;

/**
 * A very simple way of sending interactive chat messages
 */
public class SimpleComponentCore implements ConfigSerializable {

	/**
	 * The component we are creating
	 */
	private final List<ConditionalComponent> components = new ArrayList<>();

	/**
	 * The optional sender of this component
	 */
	private Audience sender;

	/**
	 * Shall this component ignore empty components? Defaults to false
	 */
	@Getter
	private boolean ignoreEmpty = false; // TODO test on false

	protected SimpleComponentCore(String legacyText, boolean colorize) {

		// Inject the center element here already
		if (legacyText.contains("<center>"))
			legacyText = ChatUtil.center(legacyText.replace("<center>", "").trim());

		if (colorize)
			legacyText = CommonCore.colorizeLegacy(legacyText);

		this.components.add(ConditionalComponent.fromLegacy(legacyText));
	}

	protected SimpleComponentCore(Component component) {
		this.components.add(ConditionalComponent.fromComponent(component));
	}

	protected SimpleComponentCore(SerializedMap map) {
		this.components.addAll(map.getList("Components", ConditionalComponent.class));
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a show text event
	 *
	 * @param lines
	 * @return
	 */
	public <T extends SimpleComponentCore> T onHover(final Collection<String> lines) {
		return this.onHover(String.join("\n", lines));
	}

	/**
	 * Add a show text hover event
	 *
	 * @param lines
	 * @return
	 */
	public <T extends SimpleComponentCore> T onHover(final String... lines) {
		return this.onHover(String.join("\n", lines));
	}

	/**
	 * Add a show text event
	 *
	 * @param hover
	 * @return
	 */
	public <T extends SimpleComponentCore> T onHover(final String hover) {
		this.modifyLastComponent(component -> component.hoverEvent(CommonCore.colorize(hover)));

		return (T) this;
	}

	/**
	 * Add a hover event
	 *
	 * @param components
	 * @return
	 */
	public <T extends SimpleComponentCore> T onHover(final Component... components) {
		Component joined = Component.empty();

		for (int i = 0; i < components.length; i++) {
			joined = joined.append(components[i]);

			if (i < components.length - 1)
				joined = joined.append(Component.newline());
		}

		final Component finalComponent = joined;
		this.modifyLastComponent(component -> component.hoverEvent(finalComponent));

		return (T) this;
	}

	/**
	 * Add a hover event
	 *
	 * @param hover
	 * @return
	 */
	public <T extends SimpleComponentCore> T onHover(final HoverEventSource<?> hover) {
		this.modifyLastComponent(component -> component.hoverEvent(hover));

		return (T) this;
	}

	/**
	 * Add a run command event
	 *
	 * @param text
	 * @return
	 */
	public <T extends SimpleComponentCore> T onClickRunCmd(final String text) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.runCommand(text)));

		return (T) this;
	}

	/**
	 * Add a suggest command event
	 *
	 * @param text
	 * @return
	 */
	public <T extends SimpleComponentCore> T onClickSuggestCmd(final String text) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.suggestCommand(text)));

		return (T) this;
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public <T extends SimpleComponentCore> T onClickOpenUrl(final String url) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.openUrl(url)));

		return (T) this;
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public <T extends SimpleComponentCore> T onClickCopyToClipboard(final String url) {
		this.modifyLastComponent(component -> component.clickEvent(ClickEvent.copyToClipboard(url)));

		return (T) this;
	}

	/**
	 * Invoke Component setInsertion
	 *
	 * @param insertion
	 * @return
	 */
	public <T extends SimpleComponentCore> T onClickInsert(final String insertion) {
		this.modifyLastComponent(component -> component.insertion(insertion));

		return (T) this;
	}

	/**
	 * Set the view condition for this component
	 *
	 * @param viewCondition
	 * @return
	 */
	public <T extends SimpleComponentCore> T viewCondition(final String viewCondition) {
		this.getLastComponent().setViewCondition(viewCondition);

		return (T) this;
	}

	/**
	 * Set the view permission for this component
	 *
	 * @param viewPermission
	 * @return
	 */
	public <T extends SimpleComponentCore> T viewPermission(final String viewPermission) {
		this.getLastComponent().setViewPermission(viewPermission);

		return (T) this;
	}

	/**
	 * Quickly replaces an object in all parts of this component
	 *
	 * @param variable the factual variable - you must supply brackets
	 * @param value
	 * @return
	 */
	public <T extends SimpleComponentCore> T replace(final String variable, final String value) {
		for (final ConditionalComponent part : this.components)
			part.setComponent(part.getComponent().replaceText(b -> b.matchLiteral(variable).replacement(value)));

		return (T) this;
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
	public <T extends SimpleComponentCore> T append(final String text) {
		return this.append(text, true);
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @param colorize
	 * @return
	 */
	public <T extends SimpleComponentCore> T append(final String text, final boolean colorize) {
		return this.append(colorize ? CommonCore.colorize(text) : RemainCore.convertLegacyToAdventure(text));
	}

	/**
	 * Append a new simple component
	 *
	 * @param component
	 * @return
	 */
	public <T extends SimpleComponentCore> T append(final SimpleComponentCore component) {
		for (final ConditionalComponent part : component.components)
			this.components.add(part);

		return (T) this;
	}

	/**
	 * Append a new component on the end of this one
	 *
	 * @param component
	 * @return
	 */
	public <T extends SimpleComponentCore> T append(final Component component) {
		this.components.add(ConditionalComponent.fromComponent(component));

		return (T) this;
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events
	 *
	 * @return
	 */
	public String getLegacyText() {
		return RemainCore.convertAdventureToLegacy(this.build(null));
	}

	/**
	 *
	 * @return
	 */
	public String getJson() {
		return RemainCore.convertAdventureToJson(this.build(null));
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
	public Component getComponent(Audience receiver) {
		return this.build(receiver);
	}

	/**
	 * Return the component
	 *
	 * @param sender
	 * @param receiver
	 * @return
	 */
	public Component getComponent(Audience sender, Audience receiver) {
		this.setSender(sender);

		return this.build(receiver);
	}

	/*
	 * Convert into Adventure component
	 */
	private Component build(Audience receiver) {
		Component main = Component.empty();

		for (final ConditionalComponent part : this.components) {
			final Component component = part.build(receiver);

			if (component != null)
				main = main.append(component);
		}

		return this.onBuild(this.sender, receiver, main);
	}

	protected Component onBuild(Audience sender, Audience receiver, Component component) {
		return component;
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * Attempts to send the complete {@link SimpleComponentCore} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param receivers
	 */
	public final void send(final Audience... receivers) {
		this.send(Arrays.asList(receivers));
	}

	/**
	 * Attempts to send the complete {@link SimpleComponentCore} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param receivers
	 */
	public final void send(final Iterable<Audience> receivers) {
		for (final Audience receiver : receivers)
			Platform.tell(receiver, this.build(receiver), this.ignoreEmpty);
	}

	/**
	 * Set the sender of this component
	 *
	 * @param sender
	 * @return
	 */
	public final SimpleComponentCore setSender(final Audience sender) {
		this.sender = sender;

		return this;
	}

	/**
	 * Set if this component should ignore empty components? Defaults to false
	 *
	 * @param ignoreEmpty
	 * @return
	 */
	public final SimpleComponentCore setIgnoreEmpty(final boolean ignoreEmpty) {
		this.ignoreEmpty = ignoreEmpty;

		return this;
	}

	/*
	 * Get the last component or throws an error if none found
	 */
	private ConditionalComponent getLastComponent() {
		ValidCore.checkBoolean(this.components.size() > 0, "No components found!");

		return this.components.get(this.components.size() - 1);
	}

	/*
	 * Helper method to modify the last component
	 */
	protected final void modifyLastComponent(Function<Component, Component> editor) {
		final ConditionalComponent last = this.getLastComponent();

		last.setComponent(editor.apply(last.getComponent()));
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public final SerializedMap serialize() {
		return SerializedMap.of("Components", this.components);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
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
	public static SimpleComponentCore empty() {
		return of(false, "");
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param text
	 * @return
	 */
	public static SimpleComponentCore of(final String text) {
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
	public static SimpleComponentCore of(boolean colorize, String text) {
		return new SimpleComponentCore(text, colorize);
	}

	/**
	 * Create a new interactive chat component
	 *
	 * @param component
	 * @return
	 */
	public static SimpleComponentCore of(Component component) {
		return new SimpleComponentCore(component);
	}

	/**
	 *
	 * @param json
	 * @return
	 */
	public static SimpleComponentCore fromJson(String json) {
		return deserialize(SerializedMap.fromJson(json));
	}

	/**
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponentCore deserialize(SerializedMap map) {
		return new SimpleComponentCore(map);
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
		private Component build(Audience receiver) {

			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !Platform.hasPermission(receiver, this.viewPermission)))
				return null;

			if (this.viewCondition != null && !this.viewCondition.isEmpty()) {
				if (receiver == null)
					return null;

				try {
					final Object result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);

					if (result != null) {
						ValidCore.checkBoolean(result instanceof Boolean, "View condition must return Boolean not " + (result == null ? "null" : result.getClass()) + " for component: " + this);

						if (!((boolean) result))
							return null;
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
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

			part.component = RemainCore.convertLegacyToAdventure(text);

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
