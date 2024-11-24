package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;

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
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

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
	private SimpleComponent(List<ConditionalComponent> components, Style lastStyle) {
		this.subcomponents = components;
		this.lastStyle = lastStyle;
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

			if (MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(V.v1_13) && component.length() > LEGACY_HOVER_LINE_LENGTH_LIMIT)
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

			if (MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(V.v1_13) && message.length() > LEGACY_HOVER_LINE_LENGTH_LIMIT)
				message = String.join("\n", CommonCore.split(message, LEGACY_HOVER_LINE_LENGTH_LIMIT));

			joined = joined.append(SimpleComponent.fromMini("<gray>" + message));

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
		if (color instanceof CompChatColor && MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(V.v1_16))
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

		return new SimpleComponent(copy, this.lastStyle);
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

		return new SimpleComponent(copy, this.lastStyle);
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

		return new SimpleComponent(copy, this.lastStyle);
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

		return new SimpleComponent(copy, this.lastStyle);
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Append a new line on the end of the component.
	 *
	 * @return
	 */
	public SimpleComponent appendNewLine() {
		return this.appendPlain("\n");
	}

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
	 * Append text with & and § color codes to the component.
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent appendAmpersand(String text) {
		return this.append(fromAmpersand(text));
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
	 * Append a new component.
	 *
	 * @param newComponent
	 * @return
	 */
	public SimpleComponent append(Component newComponent) {
		return this.append(SimpleComponent.fromAdventure(newComponent));
	}

	/**
	 * Append a new component.
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent append(SimpleComponent component) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent old : this.subcomponents)
			copy.add(old);

		Style updatedLastStyle = this.lastStyle;

		if (component.lastStyle.color() != null)
			updatedLastStyle = updatedLastStyle.color(component.lastStyle.color());

		for (final Map.Entry<TextDecoration, State> entry : component.lastStyle.decorations().entrySet())
			if (entry.getValue() == State.TRUE)
				updatedLastStyle = updatedLastStyle.decoration(entry.getKey(), State.TRUE);

		for (int i = 0; i < component.subcomponents.size(); i++) {
			final ConditionalComponent subcomponent = component.subcomponents.get(i);
			Component adventure = subcomponent.getComponent();

			if (this.lastStyle != null) {
				if (this.lastStyle.color() != null && adventure.color() == null)
					adventure = adventure.color(this.lastStyle.color());

				for (final Map.Entry<TextDecoration, State> entry : this.lastStyle.decorations().entrySet())
					if (entry.getValue() == State.TRUE)
						adventure = adventure.decoration(entry.getKey(), State.TRUE);
			}

			copy.add(new ConditionalComponent(adventure, subcomponent.getViewPermission(), subcomponent.getViewCondition()));
		}

		return new SimpleComponent(copy, updatedLastStyle);
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
				suffix = CompChatColor.fromTextColor(this.lastStyle.color()).toString();

			for (final Map.Entry<TextDecoration, State> entry : this.lastStyle.decorations().entrySet())
				if (entry.getValue() == State.TRUE)
					suffix += CompChatColor.fromTextDecoration(entry.getKey());
		}

		return LegacyComponentSerializer.legacySection().serialize(this.toAdventure(receiver)) + suffix;
	}

	/**
	 * Return the MiniMessage representation of the component.
	 *
	 * @deprecated a bug in MiniMessage converts gradients to appending the color before each letter, breaking placeholders
	 *             it is advised to replace placeholders on the raw input first, see {@link Lang#plain(String)}
	 * @return
	 */
	@Deprecated
	public String toMini() {
		return this.toMini(null);
	}

	/**
	 * Return the MiniMessage representation of the component for the given receiver.
	 *
	 * @deprecated a bug in MiniMessage converts gradients to appending the color before each letter, breaking placeholders
	 *             it is advised to replace placeholders on the raw input first, see {@link Lang#plain(String)}
	 * @param receiver
	 * @return
	 */
	@Deprecated
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
	 * @param legacy
	 * @return
	 */
	public String toAdventureJson(boolean legacy) {
		return this.toAdventureJson(null, legacy);
	}

	/**
	 * Returns the JSON representation of the component for the given receiver.
	 *
	 * @param receiver
	 * @param legacy
	 *
	 * @return
	 */
	public String toAdventureJson(FoundationPlayer receiver, boolean legacy) {
		return (legacy ? GsonComponentSerializer.colorDownsamplingGson() : GsonComponentSerializer.gson()).serialize(this.toAdventure(receiver));
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
	 * Convert into BungeeCord component.
	 *
	 * @param legacy
	 * @return
	 */
	public BaseComponent[] toBungee(boolean legacy) {
		return (legacy ? BungeeComponentSerializer.legacy() : BungeeComponentSerializer.get()).serialize(this.toAdventure());
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
		final List<Component> children = new ArrayList<>();

		for (final ConditionalComponent part : this.subcomponents)
			children.add(part.build(receiver));

		return Component.textOfChildren(children.toArray(new Component[children.size()]));
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

		return new SimpleComponent(copy, this.lastStyle);
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.fromArray(
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
		return new SimpleComponent(ConditionalComponent.fromAdventure(Component.empty()), Style.empty());
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
		if (message == null)
			return SimpleComponent.empty();

		if (" ".equals(message))
			return fromPlain(" ");

		if (message.startsWith("<center>"))
			message = ChatUtil.center(message.replace("<center>", "").trim());

		// Replace legacy & color codes
		message = CompChatColor.legacyToMini(message, true);

		Component mini;

		try {
			mini = MiniMessage.miniMessage().deserialize(message.replace("\\n", "\n"));

		} catch (final Throwable t) {
			CommonCore.throwError(t, "Error parsing mini message tags in: " + message);

			return null;
		}

		return new SimpleComponent(ConditionalComponent.fromAdventure(mini), LastMessageStyleParser.parseStyle(message));
	}

	/**
	 * Create a new component from the given message.
	 *
	 * Replaces & and § color codes.
	 *
	 * @param legacyText
	 * @return
	 */
	public static SimpleComponent fromAmpersand(@NonNull String legacyText) {
		legacyText = legacyText.replaceAll("(?i)&([0-9A-FK-OR])", "§$1");

		return fromSection(legacyText);
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
		final Component mini = LegacyComponentSerializer.legacySection().deserialize(legacyText);
		final String withMiniTags = CompChatColor.legacyToMini(legacyText, false);

		return new SimpleComponent(ConditionalComponent.fromAdventure(mini), LastMessageStyleParser.parseStyle(withMiniTags));
	}

	/**
	 * Create a new component from adventure component.
	 *
	 * @param component
	 * @param legacy
	 *
	 * @return
	 */
	public static SimpleComponent fromBungee(@NonNull BaseComponent[] component, boolean legacy) {
		return fromAdventure((legacy ? BungeeComponentSerializer.legacy() : BungeeComponentSerializer.get()).deserialize(component));
	}

	/**
	 * Create a new component from JSON.
	 *
	 * @param json
	 * @param legacy
	 *
	 * @return
	 */
	public static SimpleComponent fromAdventureJson(@NonNull String json, boolean legacy) {
		return fromAdventure((legacy ? GsonComponentSerializer.colorDownsamplingGson() : GsonComponentSerializer.gson()).deserialize(json));
	}

	/**
	 * Create a new component from adventure component.
	 *
	 * @param component
	 * @return
	 */
	public static SimpleComponent fromAdventure(@NonNull Component component) {
		return new SimpleComponent(ConditionalComponent.fromAdventure(component), Style.empty());
	}

	/**
	 * Create a new component from plain text.
	 *
	 * @param plainText
	 * @return
	 */
	public static SimpleComponent fromPlain(@NonNull String plainText) {
		return new SimpleComponent(ConditionalComponent.fromPlain(plainText), Style.empty());
	}

	/**
	 * Turns the given map into a component.
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(SerializedMap map) {
		final List<ConditionalComponent> components = map.getList("Components", ConditionalComponent.class);
		final SimpleComponent component = new SimpleComponent(components, map.get("Last_Style", Style.class));

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
			map.putIfExists("Permission", this.viewPermission);
			map.putIfExists("Condition", this.viewCondition);

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

	/**
	 * Helps to resolve last message style from MiniMessage tags.
	 */
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class LastMessageStyleParser {

		/**
		 * The pattern for RGB color codes.
		 */
		private final static Pattern RGB_PATTERN = Pattern.compile("<#[0-9a-fA-F]{6}>");

		/**
		 * The message.
		 */
		private String message;

		/**
		 * The last color.
		 */
		private TextColor lastColor;

		/**
		 * The last decorations.
		 */
		private final Set<TextDecoration> lastDecorations = EnumSet.noneOf(TextDecoration.class);

		/*
		 * Parse the given message.
		 */
		private void parseMessage(String message) {

			// Reset parsing state
			this.message = message;
			this.lastColor = null;
			this.lastDecorations.clear();

			final StringBuilder cleanedMessage = new StringBuilder();
			final int length = message.length();
			boolean insideTag = false;
			final StringBuilder currentTag = new StringBuilder();

			for (int i = 0; i < length; i++) {
				final char ch = message.charAt(i);

				if (ch == '<') {
					insideTag = true;
					currentTag.setLength(0);

				} else if (ch == '>' && insideTag) {
					final String tag = "<" + currentTag.toString() + ">";

					if (tag.equals("<reset>") || tag.equals("<r>")) {
						this.lastColor = null;
						this.lastDecorations.clear();

					} else if (CompChatColor.MINI_TO_COLOR.containsKey(tag))
						this.lastColor = CompChatColor.MINI_TO_COLOR.get(tag);

					else if (CompChatColor.MINI_TO_DECORATION.containsKey(tag))
						this.lastDecorations.add(CompChatColor.MINI_TO_DECORATION.get(tag));

					else if (RGB_PATTERN.matcher(tag).matches())
						this.lastColor = TextColor.fromHexString(tag.substring(1, 8));

					insideTag = false;

				} else if (insideTag)
					// Building tag content
					currentTag.append(ch);

				else
					// Normal text
					cleanedMessage.append(ch);
			}

			this.message = cleanedMessage.toString();
		}

		/**
		 * Parse the last message style from the given message.
		 *
		 * @param message
		 * @return the last message style
		 */
		public static Style parseStyle(String message) {
			final LastMessageStyleParser parser = new LastMessageStyleParser();

			parser.parseMessage(message);

			return Style.style(parser.getLastColor(), parser.getLastDecorations());
		}
	}
}
