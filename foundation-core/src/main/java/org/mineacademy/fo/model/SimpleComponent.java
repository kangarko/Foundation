package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * An adaption of {@link Component} that allows for easier creation of chat
 * components with click/hover events, colors and even per-receiver condition/permissions.
 *
 * It also fixes the issue where if you place a color at the end of one component and append
 * new text to it, the new text won't have the color.
 */
public final class SimpleComponent implements ConfigSerializable, ComponentLike {

	/**
	 * The limit of characters per line for hover events in legacy versions
	 * of Minecraft where there is no automatic line wrapping.
	 */
	private static final int LEGACY_HOVER_LINE_LENGTH_LIMIT = 55;

	/**
	 * The components we are creating
	 */
	private final List<ConditionalComponent> subcomponents;

	/**
	 * The last style used, null if none
	 */
	@Getter
	private Style lastStyle = null;

	/*
	 * Create a new simple component.
	 */
	private SimpleComponent(List<ConditionalComponent> components) {
		this(components, null);
	}

	/*
	 * Create a new simple component.
	 */
	private SimpleComponent(List<ConditionalComponent> components, Style lastStyle) {
		this.subcomponents = components;
		this.lastStyle = lastStyle;
	}

	/*
	 * Create a new simple component.
	 */
	private SimpleComponent(ConditionalComponent component) {
		this(component, null);
	}

	/*
	 * Create a new simple component.
	 */
	private SimpleComponent(ConditionalComponent component, Style lastStyle) {
		this.subcomponents = Collections.singletonList(component);
		this.lastStyle = lastStyle;
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a hover event.
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(Collection<SimpleComponent> lines) {
		return this.onHover(lines.toArray(new SimpleComponent[lines.size()]));
	}

	/**
	 * Add a hover event.
	 *
	 * @param components
	 * @return
	 */
	public SimpleComponent onHover(SimpleComponent... components) {
		Component joined = Component.empty();

		for (int i = 0; i < components.length; i++) {
			String component = components[i].toLegacy();

			if (MinecraftVersion.olderThan(V.v1_13) && component.length() > LEGACY_HOVER_LINE_LENGTH_LIMIT)
				component = String.join("\n", CommonCore.split(component, LEGACY_HOVER_LINE_LENGTH_LIMIT));

			joined = joined.append(SimpleComponent.fromSection(component));

			if (i < components.length - 1)
				joined = joined.append(Component.newline());
		}

		final Component finalComponent = joined.asComponent();
		return modifyLastComponentAndReturn(component -> component.hoverEvent(finalComponent));
	}

	/**
	 * Add a hover event.
	 *
	 * @param messages
	 * @return
	 */
	public SimpleComponent onHover(String... messages) {
		Component joined = Component.empty();

		for (int i = 0; i < messages.length; i++) {
			String message = messages[i];

			if (MinecraftVersion.olderThan(V.v1_13) && message.length() > LEGACY_HOVER_LINE_LENGTH_LIMIT)
				message = String.join("\n", CommonCore.split(message, LEGACY_HOVER_LINE_LENGTH_LIMIT));

			joined = joined.append(SimpleComponent.fromMini(message));

			if (i < messages.length - 1)
				joined = joined.append(Component.newline());
		}

		final Component finalComponent = joined.asComponent();

		return modifyLastComponentAndReturn(component -> component.hoverEvent(finalComponent));
	}

	/**
	 * Add a hover event. To put an ItemStack here, see {@link Platform#convertItemStackToHoverEvent(Object)}.
	 *
	 * @param hover
	 * @return
	 */
	public SimpleComponent onHover(HoverEventSource<?> hover) {
		return modifyLastComponentAndReturn(component -> component.hoverEvent(hover));
	}

	/**
	 * Add a run command event.
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickRunCmd(String text) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.runCommand(text)));
	}

	/**
	 * Add a suggest command event.
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickSuggestCmd(String text) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.suggestCommand(text)));
	}

	/**
	 * Open the given URL.
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickOpenUrl(String url) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.openUrl(url)));
	}

	/**
	 * Open the given URL.
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickCopyToClipboard(String url) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.copyToClipboard(url)));
	}

	/**
	 * Invoke SimpleComponent setInsertion.
	 *
	 * @param insertion
	 * @return
	 */
	public SimpleComponent onClickInsert(String insertion) {
		return modifyLastComponentAndReturn(component -> component.insertion(insertion));
	}

	/**
	 * Set the view condition for this component.
	 *
	 * @param viewCondition
	 * @return
	 */
	public SimpleComponent viewCondition(String viewCondition) {
		this.subcomponents.get(this.subcomponents.size() - 1).setViewCondition(viewCondition);

		return this;
	}

	/**
	 * Set the view permission for this component.
	 *
	 * @param viewPermission
	 * @return
	 */
	public SimpleComponent viewPermission(String viewPermission) {
		this.subcomponents.get(this.subcomponents.size() - 1).setViewPermission(viewPermission);

		return this;
	}

	/**
	 * Set the text color for this component.
	 *
	 * @param color
	 * @return
	 */
	public SimpleComponent color(TextColor color) {

		// No RGB support in older versions
		if (color instanceof CompChatColor && MinecraftVersion.olderThan(V.v1_16))
			color = NamedTextColor.nearestTo(color);

		final TextColor finalColor = color;
		return modifyLastComponentAndReturn(component -> component.color(finalColor));
	}

	/**
	 * Set the text decoration for this component
	 *
	 * @param color
	 * @return
	 */
	public SimpleComponent decoration(TextDecoration color) {
		return modifyLastComponentAndReturn(component -> component.decoration(color, true));
	}

	/**
	 * Quickly replaces an object in all parts of this component, adding
	 * {} around it.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceBracket(String variable, String value) {
		return this.replaceBracket(variable, fromPlain(value));
	}

	/**
	 * Quickly replaces an object in all parts of this component, adding
	 * {} around it.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceBracket(String variable, SimpleComponent value) {
		return this.replaceLiteral("{" + variable + "}", value);
	}

	/**
	 * Quickly replaces the literal in all parts of this component.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceLiteral(String variable, String value) {
		return this.replaceLiteral(variable, fromPlain(value));
	}

	/**
	 * Quickly replaces the literal in all parts of this component.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceLiteral(String variable, SimpleComponent value) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.matchLiteral(variable).replacement(value));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function.
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public SimpleComponent replaceMatch(Pattern pattern, BiFunction<MatchResult, TextComponent.Builder, ComponentLike> replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function.
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public SimpleComponent replaceMatch(Pattern pattern, String replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function.
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public SimpleComponent replaceMatch(Pattern pattern, SimpleComponent replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Append plain text to the component.
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent appendPlain(String text) {
		return this.append(fromPlain(text));
	}

	/**
	 * Append text with § color codes to the component.
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent appendSection(String text) {
		return this.append(fromSection(text));
	}

	/**
	 * Append text with &, § or MiniMessage tags to the component.
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent appendMini(String text) {
		return this.append(fromMini(text));
	}

	/**
	 * Append a new line on the end of the component.
	 *
	 * @return
	 */
	public SimpleComponent appendNewLine() {
		return this.appendPlain("\n");
	}

	/**
	 * Append a new component.
	 *
	 * @param newComponent
	 * @return
	 */
	public SimpleComponent append(SimpleComponent newComponent) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent oldSubcomponent : this.subcomponents)
			copy.add(oldSubcomponent);

		for (int i = 0; i < newComponent.subcomponents.size(); i++) {
			final ConditionalComponent newSubcomponent = newComponent.subcomponents.get(i);
			Component adventure = newSubcomponent.getComponent();

			// Why I prefer legacy over Adventure > last style is not properly kept, i.e. "&c[Prefix]&7" resets the gray, so we have
			// to manually save it and reapply it later
			if (i == 0 && this.lastStyle != null)
				adventure = adventure.style(this.lastStyle);

			copy.add(new ConditionalComponent(adventure, newSubcomponent.getViewPermission(), newSubcomponent.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Return if this component is empty.
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.isEmpty(null);
	}

	/**
	 * Return if this component is empty for the given receiver.
	 *
	 * @param receiver
	 * @return
	 */
	public boolean isEmpty(FoundationPlayer receiver) {
		return this.subcomponents.isEmpty() || this.toPlain(receiver).isEmpty();
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events.
	 *
	 * @return
	 */
	public String toLegacy() {
		return this.toLegacy(null);
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events for the given receiver.
	 *
	 * @param receiver
	 * @return
	 */
	public String toLegacy(FoundationPlayer receiver) {

		// Append tail from the last style
		String suffix = "";

		if (this.lastStyle != null) {
			if (this.lastStyle.color() != null)
				suffix = CompChatColor.fromString(this.lastStyle.color().asHexString()).toString();

			for (final Map.Entry<TextDecoration, State> entry : this.lastStyle.decorations().entrySet())
				if (entry.getValue() == State.TRUE)
					suffix += CompChatColor.fromString(entry.getKey().name()).toString();
		}

		return LegacyComponentSerializer.legacySection().serialize(this.toAdventure(receiver)) + suffix;
	}

	/**
	 * Return the minimessage representation of the component.
	 *
	 * @return
	 */
	public String toMini() {
		return this.toMini(null);
	}

	/**
	 * Return the minimessage representation of the component for the given receiver.
	 *
	 * @param receiver
	 * @return
	 */
	public String toMini(FoundationPlayer receiver) {
		return MiniMessage.miniMessage().serialize(this.toAdventure(receiver));
	}

	/**
	 * Return the plain colorless message combining all components into one
	 * without click/hover events.
	 *
	 * This effectivelly removes all & and § colors as well as MiniMessage tags.
	 *
	 * @return
	 */
	public String toPlain() {
		return this.toPlain(null);
	}

	/**
	 * Return the plain colorless message combining all components into one
	 * without click/hover events for the given receiver.
	 *
	 * This effectivelly removes all & and § colors as well as MiniMessage tags.
	 *
	 * @param receiver
	 * @return
	 */
	public String toPlain(FoundationPlayer receiver) {
		return PlainTextComponentSerializer.plainText().serialize(this.toAdventure(receiver));
	}

	/**
	 * Returns the JSON representation of the component.
	 *
	 * @return
	 */
	public String toAdventureJson() {
		return this.toAdventureJson(null);
	}

	/**
	 * Returns the JSON representation of the component for the given receiver.
	 *
	 * @param receiver
	 * @return
	 */
	public String toAdventureJson(FoundationPlayer receiver) {
		return GsonComponentSerializer.gson().serialize(this.toAdventure(receiver));
	}

	/**
	 * @see #toAdventure()
	 *
	 * @deprecated use {@link #toAdventure()} instead
	 * @return
	 */
	@Deprecated
	@Override
	public Component asComponent() {
		return this.toAdventure();
	}

	/**
	 * Convert into Adventure component.
	 *
	 * @return
	 */
	public Component toAdventure() {
		return this.toAdventure(null);
	}

	/**
	 * Convert into Adventure component, executing viewCondition and viewPermission for the given receiver.
	 *
	 * @param receiver the given receiver, can be null
	 * @return
	 */
	public Component toAdventure(FoundationPlayer receiver) {
		Component main = null;

		for (final ConditionalComponent part : this.subcomponents) {
			final Component component = part.build(receiver);

			if (component != null) {
				if (main == null)
					main = component;
				else
					main = main.append(component);
			}
		}

		return main;
	}

	/*
	 * Helper method to modify the last component.
	 */
	protected SimpleComponent modifyLastComponentAndReturn(Function<Component, Component> editor) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (int i = 0; i < this.subcomponents.size(); i++) {
			ConditionalComponent component = this.subcomponents.get(i);

			if (i == this.subcomponents.size() - 1)
				component = ConditionalComponent.fromAdventure(editor.apply(component.getComponent()));

			copy.add(component);
		}

		return new SimpleComponent(copy);
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Components", this.subcomponents,
				"Last_Style", this.lastStyle);
	}

	/**
	 * Return if this component is equal to another component or
	 * if it is a string, return if the plain representation is equal.
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String)
			return this.toPlain().equals(obj);

		if (obj instanceof SimpleComponent) {
			final SimpleComponent other = (SimpleComponent) obj;

			return this.toMini().equals(other.toMini());
		}

		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		throw new FoException("SimpleComponent#toString() is unsupported, use toMini(), toLegacy() or toPlain() as needed");
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * Create a new empty component.
	 *
	 * @return
	 */
	public static SimpleComponent empty() {
		return new SimpleComponent(ConditionalComponent.fromAdventure(Component.empty()));
	}

	/**
	 * Create a new component from the given message.
	 *
	 * Replaces & color codes and MiniMessage tags.
	 *
	 * @param message
	 * @return
	 */
	public static SimpleComponent fromMini(String message) {
		if (message == null || message.trim().isEmpty())
			return SimpleComponent.empty();

		if (message.startsWith("<center>"))
			message = ChatUtil.center(message.replace("<center>", "").trim());

		// Replace legacy & color codes
		message = CompChatColor.legacyToMini(message, true);

		Component mini;

		try {
			mini = MiniMessage.miniMessage().deserialize(message);

		} catch (final Throwable t) {
			CommonCore.throwError(t, "Error parsing mini message tags in: " + message);

			return null;
		}

		Style lastStyle = null;

		// if message ends with color code from the above map, add an empty component at the end with the same color
		if (!message.endsWith(" "))
			for (final String value : CompChatColor.LEGACY_TO_MINI.values()) {
				if (message.endsWith(value)) {
					lastStyle = MiniMessage.miniMessage().deserialize(value).style();

					mini = Component
							.text("")
							.style(lastStyle)
							.children(Arrays.asList(mini));

					break;
				}
			}

		return new SimpleComponent(ConditionalComponent.fromAdventure(mini), lastStyle);
	}

	/**
	 * Create a new component from the given message.
	 *
	 * Replaces § color codes.
	 *
	 * @param legacyText
	 * @return
	 */
	public static SimpleComponent fromSection(@NonNull String legacyText) {
		Style lastStyle = null;
		Component mini = LegacyComponentSerializer.legacySection().deserialize(legacyText);

		// if message ends with color code from the above map, add an empty component at the end with the same color
		if (!legacyText.endsWith(" "))
			for (final CompChatColor color : CompChatColor.values()) {
				if (legacyText.endsWith(color.toString())) {
					lastStyle = MiniMessage.miniMessage().deserialize(CompChatColor.legacyToMini(color.toString(), false)).style();

					mini = Component
							.text("")
							.style(lastStyle)
							.children(Arrays.asList(mini));

					break;
				}
			}

		return new SimpleComponent(ConditionalComponent.fromAdventure(mini));
	}

	/**
	 * Create a new component from adventure component.
	 *
	 * @param component
	 * @return
	 */
	public static SimpleComponent fromAdventure(@NonNull Component component) {
		return new SimpleComponent(ConditionalComponent.fromAdventure(component));
	}

	/**
	 * Create a new component from plain text.
	 *
	 * @param plainText
	 * @return
	 */
	public static SimpleComponent fromPlain(@NonNull String plainText) {
		return new SimpleComponent(ConditionalComponent.fromPlain(plainText));
	}

	/**
	 * Create a new component from JSON.
	 *
	 * @param json
	 * @return
	 */
	public static SimpleComponent fromAdventureJson(@NonNull String json) {
		return new SimpleComponent(ConditionalComponent.fromJson(json));
	}

	/**
	 * Create a new component from the given children.
	 *
	 * @param components
	 * @return
	 */
	public static SimpleComponent fromChildren(SimpleComponent... components) {
		final List<ConditionalComponent> children = new ArrayList<>();

		for (final SimpleComponent component : components)
			children.addAll(component.subcomponents);

		return new SimpleComponent(children);
	}

	/**
	 * Turns the given map into a component.
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(SerializedMap map) {
		final List<ConditionalComponent> components = map.getList("Components", ConditionalComponent.class);
		final SimpleComponent component = new SimpleComponent(components);

		component.lastStyle = map.get("Last_Style", Style.class);

		return component;
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	/**
	 * Helper class to store a component with view condition and view permission.
	 */
	@Setter
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	static final class ConditionalComponent implements ConfigSerializable {

		/**
		 * The adventure component.
		 */
		private final Component component;

		/**
		 * The view permission executed for receivers.
		 */
		private String viewPermission;

		/**
		 * The view condition executed for receivers.
		 */
		private String viewCondition;

		/**
		 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
		 */
		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Component", MiniMessage.miniMessage().serialize(this.component));
			map.putIf("Permission", this.viewPermission);
			map.putIf("Condition", this.viewCondition);

			return map;
		}

		/**
		 * Turn the map into a conditional component.
		 *
		 * @param map
		 * @return
		 */
		public static ConditionalComponent deserialize(SerializedMap map) {
			final Component component = MiniMessage.miniMessage().deserialize(map.getString("Component"));
			final ConditionalComponent part = new ConditionalComponent(component);

			part.viewPermission = map.getString("Permission");
			part.viewCondition = map.getString("Condition");

			return part;
		}

		/*
		 * Build the component for the given receiver.
		 */
		private Component build(FoundationPlayer receiver) {
			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !receiver.hasPermission(this.viewPermission)))
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
		 * Create a new conditional component from JSON.
		 *
		 * @param text
		 * @return
		 */
		static ConditionalComponent fromJson(String json) {
			return new ConditionalComponent(GsonComponentSerializer.gson().deserialize(json));
		}

		/**
		 * Create a new conditional component from adventure component.
		 *
		 * @param component
		 * @return
		 */
		static ConditionalComponent fromAdventure(Component component) {
			return new ConditionalComponent(component);
		}

		/**
		 * Create a new conditional component from plain text.
		 *
		 * @param component
		 * @return
		 */
		static ConditionalComponent fromPlain(String plainText) {
			return new ConditionalComponent(Component.text(plainText));
		}
	}
}
