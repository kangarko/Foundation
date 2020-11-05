package org.mineacademy.fo.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public final class Variable extends YamlConfig {

	/**
	 * A list of all loaded variables
	 */
	private static final ConfigItems<Variable> loadedVariables = ConfigItems.fromFolder("variable", "variables", Variable.class);

	/**
	 * The kind of this variable
	 */
	@Getter
	private Type type;

	/**
	 * The variable key what we should find
	 */
	@Getter
	private String key;

	/**
	 * The variable value what we should replace the key with
	 * JavaScript engine
	 */
	private String value;

	/**
	 * The JavaScript condition that must return TRUE for this variable to be shown
	 */
	@Nullable
	@Getter
	private String senderCondition;

	/**
	 * The JavaScript condition that must return TRUE for this variable to be shown to a receiver
	 */
	@Nullable
	@Getter
	private String receiverCondition;

	/**
	 * The permission the sender must have to show the part
	 */
	@Nullable
	@Getter
	private String senderPermission;

	/**
	 * The permission receiver must have to see the part
	 */
	@Nullable
	@Getter
	private String receiverPermission;

	/**
	 * The hover text or null if not set
	 */
	@Getter
	@Nullable
	private List<String> hoverText;

	/**
	 * The JavaScript pointing to a particular {@link ItemStack}
	 */
	@Getter
	@Nullable
	private String hoverItem;

	/**
	 * What URL should be opened on click? Null if none
	 */
	@Getter
	@Nullable
	private String openUrl;

	/**
	 * What command should be suggested on click? Null if none
	 */
	@Getter
	@Nullable
	private String suggestCommand;

	/**
	 * What command should be run on click? Null if none
	 */
	@Getter
	@Nullable
	private String runCommand;

	/*
	 * Create and load a new variable (automatically called)
	 */
	private Variable(String file) {
		this.loadConfiguration(NO_DEFAULT, "variables/" + file + ".yml");
	}

	// ----------------------------------------------------------------------------------
	// Loading
	// ----------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {

		// We do not use Valid.checkNotNull below since it appends Report: prefix.
		// Do not incentivize people to report this, instead, we want them to fix this.

		this.senderCondition = getString("Sender_Condition");
		this.receiverCondition = getString("Receiver_Condition");
		this.hoverText = getStringList("Hover");
		this.hoverItem = getString("Hover_Item");
		this.openUrl = getString("Open_Url");
		this.suggestCommand = getString("Suggest_Command");
		this.runCommand = getString("Run_Command");
		this.senderPermission = getString("Sender_Permission");
		this.receiverPermission = getString("Receiver_Permission");

		this.key = getString("Key");
		Objects.requireNonNull(this.key, "(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Key' as variable name in " + getFile());

		this.type = get("Type", Type.class);

		// Auto-fix some of the problems in old ChatControl
		if (SimplePlugin.getNamed().equals("ChatControl") && SimplePlugin.getVersion().startsWith("8.")) {

			if (this.type == null)
				this.type = Type.FORMAT;

			if (this.key.startsWith("{") || this.key.startsWith("["))
				this.key = this.key.substring(1);

			if (this.key.endsWith("}") || this.key.endsWith("]"))
				this.key = this.key.substring(0, this.key.length() - 1);

			if (this.type == Type.FORMAT) {
				if (this.hoverText != null && !this.hoverText.isEmpty()) {
					this.hoverText = null;

					Common.log("&cWarning: Hover_Text is currently unsupported for variable " + getName() + ", please set it in your formatting.yml instead");
				}

				if (this.hoverItem != null) {
					this.hoverItem = null;

					Common.log("&cWarning: Hover_Item is currently unsupported for variable " + getName());
				}
			}

		} else
			Objects.requireNonNull(this.type, "(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Type' key as variable type (set to FORMAT if variable can't be used by players, or MESSAGE if players can use it in chat with [variable_syntax]) in " + getFile());

		// Test for key validity
		if (!Common.regExMatch("^\\w+$", this.key))
			throw new IllegalArgumentException("(DO NOT REPORT, PLEASE FIX YOURSELF) The 'Key' variable in " + getFile() + " must only contains letters, numbers or underscores. Do not write [] or {} there!");

		this.value = getString("Value");
		Objects.requireNonNull(this.value, "(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Value' key as what the variable shows in " + getFile() + " (this can be a JavaScript code)");

		if (this.type == Type.FORMAT) {
			if (this.hoverText != null && !this.hoverText.isEmpty())
				throw new IllegalStateException("FORMAT variables do not support Hover, you need to add this directly to the format and remove this key from " + getFileName());

			if (this.hoverItem != null)
				throw new IllegalStateException("FORMAT variables do not support Hover_Item, you need to add this directly to the format and remove this key from " + getFileName());

			if (this.openUrl != null && !this.openUrl.isEmpty())
				throw new IllegalStateException("FORMAT variables do not support Open_Url, you need to add this directly to the format and remove this key from " + getFileName());

			if (this.suggestCommand != null && !this.suggestCommand.isEmpty())
				throw new IllegalStateException("FORMAT variables do not support Suggest_Command, you need to add this directly to the format and remove this key from " + getFileName());

			if (this.runCommand != null && !this.runCommand.isEmpty())
				throw new IllegalStateException("FORMAT variables do not support Run_Command, you need to add this directly to the format and remove this key from " + getFileName());
		}
	}

	// ----------------------------------------------------------------------------------
	// Getters
	// ----------------------------------------------------------------------------------

	/**
	 * Runs the script for the given player and the replacements,
	 * returns the output
	 *
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public String getValue(CommandSender sender, @Nullable Map<String, Object> replacements) {
		Variables.REPLACE_JAVASCRIPT = false;

		try {
			// Replace variables in script
			final String script = Variables.replace(this.value, sender, replacements);

			return String.valueOf(JavaScriptExecutor.run(script, sender));

		} finally {
			Variables.REPLACE_JAVASCRIPT = true;
		}
	}

	/**
	 * Create the variable and append it to the existing component as if the player initiated it
	 *
	 * @param sender
	 * @param existingComponent
	 * @return
	 */
	public SimpleComponent build(CommandSender sender, SimpleComponent existingComponent, @Nullable Map<String, Object> replacements) {

		if (this.senderPermission != null && !PlayerUtil.hasPerm(sender, this.senderPermission))
			return SimpleComponent.of("");

		if (this.senderCondition != null) {
			final Object result = JavaScriptExecutor.run(this.senderCondition, sender);
			Valid.checkBoolean(result instanceof Boolean, "Variable '" + getName() + "' option Condition must return boolean not " + result.getClass());

			if ((boolean) result == false)
				return SimpleComponent.of("");
		}

		final String value = this.getValue(sender, replacements);

		if (value == null || "".equals(value) || "null".equals(value))
			return SimpleComponent.of("");

		final SimpleComponent component = existingComponent
				.append(Variables.replace(value, sender, replacements))
				.viewPermission(this.receiverPermission)
				.viewCondition(this.receiverCondition);

		if (!Valid.isNullOrEmpty(this.hoverText))
			component.onHover(Variables.replace(this.hoverText, sender, replacements));

		if (this.hoverItem != null) {
			final Object result = JavaScriptExecutor.run(Variables.replace(this.hoverItem, sender, replacements), sender);
			Valid.checkBoolean(result instanceof ItemStack, "Variable '" + getName() + "' option Hover_Item must return ItemStack not " + result.getClass());

			component.onHover((ItemStack) result);
		}

		if (this.openUrl != null)
			component.onClickOpenUrl(Variables.replace(this.openUrl, sender, replacements));

		if (this.suggestCommand != null)
			component.onClickSuggestCmd(Variables.replace(this.suggestCommand, sender, replacements));

		if (this.runCommand != null)
			component.onClickRunCmd(Variables.replace(this.runCommand, sender, replacements));

		return component;
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#toString()
	 */
	@Override
	public String toString() {
		return SerializedMap.ofArray(
				"Key", this.key,
				"Value", this.value,
				"Sender_Condition", this.senderCondition,
				"Receiver_Condition", this.receiverCondition,
				"Hover", this.hoverText,
				"Hover_Item", this.hoverItem,
				"Open_Url", this.openUrl,
				"Suggest_Command", this.suggestCommand,
				"Run_Command", this.runCommand,
				"Sender_Permission", this.senderPermission,
				"Receiver_Permission", this.receiverPermission).toStringFormatted();
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Load all variables from variables/ folder
	 */
	public static void loadVariables() {
		loadedVariables.loadItems();
	}

	/**
	 * Remove the given variable in case it exists
	 *
	 * @param variable
	 */
	public static void removeVariable(final Variable variable) {
		loadedVariables.removeItem(variable);
	}

	/**
	 * Return true if the given variable by key is loaded
	 *
	 * @param name
	 * @return
	 */
	public static boolean isVariableLoaded(final String name) {
		return loadedVariables.isItemLoaded(name);
	}

	/**
	 * Return a variable, or null if not loaded
	 *
	 * @param name
	 * @return
	 */
	public static Variable findVariable(@NonNull final String name) {
		for (final Variable item : getVariables())
			if (item.getKey().equalsIgnoreCase(name))
				return item;

		return null;
	}

	/**
	 * Return a list of all variables
	 *
	 * @return
	 */
	public static List<Variable> getVariables() {
		return loadedVariables.getItems();
	}

	/**
	 * Return a list of all variable names
	 *
	 * @return
	 */
	public static List<String> getVariableNames() {
		return loadedVariables.getItemNames();
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Represents a variable type
	 */
	@RequiredArgsConstructor
	public enum Type {

		/**
		 * This variable is used in chat format and "server to player" messages
		 * Cannot be used by players. Example: [{channel}] {player}: {message}
		 */
		FORMAT("format"),

		/**
		 * This variable can be used by players in chat such as "I have an [item]"
		 */
		MESSAGE("message"),
		;

		/**
		 * The saveable non-obfuscated key
		 */
		@Getter
		private final String key;

		/**
		 * Attempt to load the type from the given config key
		 *
		 * @param key
		 * @return
		 */
		public static Type fromKey(String key) {
			for (final Type mode : values())
				if (mode.key.equalsIgnoreCase(key))
					return mode;

			throw new IllegalArgumentException("No such item type: " + key + ". Available: " + Common.join(values()));
		}

		/**
		 * Returns {@link #getKey()}
		 */
		@Override
		public String toString() {
			return this.key;
		}
	}
}