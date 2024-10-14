package org.mineacademy.fo.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.ConfigItems;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a JavaScript variable that can be used in chat messages.
 */
public final class Variable extends YamlConfig {

	/**
	 * Return the prototype file path for the given variable field name
	 */
	public static Function<String, String> PROTOTYPE_PATH = t -> NO_DEFAULT;

	/**
	 * The pattern for a valid variable key
	 */
	private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^\\w+$");

	/**
	 * A list of all loaded variables
	 */
	private static final ConfigItems<Variable> loadedVariables = ConfigItems.fromFolder("variables", Variable.class);

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
	@Getter
	private String senderCondition;

	/**
	 * The JavaScript condition that must return TRUE for this variable to be shown to a receiver
	 */
	@Getter
	private String receiverCondition;

	/**
	 * The permission the sender must have to show the part
	 */
	@Getter
	private String senderPermission;

	/**
	 * The permission receiver must have to see the part
	 */
	@Getter
	private String receiverPermission;

	/**
	 * The hover text or null if not set
	 */
	@Getter
	private List<String> hoverText;

	/**
	 * The JavaScript pointing to a particular {@link ItemStack}
	 */
	@Getter
	private String hoverItem;

	/**
	 * What URL should be opened on click? Null if none
	 */
	@Getter
	private String openUrl;

	/**
	 * What command should be suggested on click? Null if none
	 */
	@Getter
	private String suggestCommand;

	/**
	 * What command should be run on click? Null if none
	 */
	@Getter
	private String runCommand;

	/*
	 * Create and load a new variable (automatically called)
	 */
	private Variable(String file) {
		final String prototypePath = PROTOTYPE_PATH.apply(file);

		this.setHeader(
				CommonCore.configLine(),
				Platform.getPlugin().getName() + " supports dynamic, high performance JavaScript variables! They will",
				"automatically be used when calling Variables#replace for your messages.",
				"",
				"Because variables return a JavaScript value, you can sneak in code to play sounds or spawn",
				"monsters directly in your variable instead of it just displaying text!",
				"",
				"For example of how variables can be used, see our plugin ChatControl's wikipedia article:",
				"https://github.com/kangarko/ChatControl-Red/wiki/JavaScript-Variables",
				CommonCore.configLine());

		this.loadAndExtract(prototypePath, "variables/" + file + ".yml");
	}

	// ----------------------------------------------------------------------------------
	// Loading
	// ----------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.org.mineacademy.fo.settings.YamlConfig#onLoad()
	 */
	@Override
	protected void onLoad() {
		this.type = this.get("Type", Type.class);
		this.key = this.getString("Key");
		this.value = this.getString("Value");
		this.senderCondition = this.getString("Sender_Condition");
		this.receiverCondition = this.getString("Receiver_Condition");
		this.senderPermission = this.getString("Sender_Permission");
		this.receiverPermission = this.getString("Receiver_Permission");

		// Correct common mistakes
		if (this.type == null) {
			this.type = Type.FORMAT;

			this.save();
		}

		// Check for known mistakes
		if (this.key == null || this.key.isEmpty())
			throw new NullPointerException("(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Key' as variable name in " + this.getFile());

		if (this.value == null)
			throw new NullPointerException("(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Value' key as what the variable shows in " + this.getFile() + " (this can be a JavaScript code)");

		if (this.key.startsWith("{") || this.key.startsWith("[")) {
			this.key = this.key.substring(1);

			this.save();
		}

		if (this.key.endsWith("}") || this.key.endsWith("]")) {
			this.key = this.key.substring(0, this.key.length() - 1);

			this.save();
		}

		if (this.type == Type.MESSAGE) {
			this.hoverText = this.getStringList("Hover");
			this.hoverItem = this.getString("Hover_Item");
			this.openUrl = this.getString("Open_Url");
			this.suggestCommand = this.getString("Suggest_Command");
			this.runCommand = this.getString("Run_Command");
		}

		// Test for key validity
		if (!VALID_KEY_PATTERN.matcher(this.key).matches())
			throw new IllegalArgumentException("(DO NOT REPORT, PLEASE FIX YOURSELF) The 'Key' variable in " + this.getFile() + " must only contains letters, numbers or underscores. Do not write [] or {} there!");
	}

	@Override
	public void onSave() {
		this.set("Type", this.type);
		this.set("Key", this.key);
		this.set("Value", this.value);
		this.set("Sender_Condition", this.senderCondition);
		this.set("Receiver_Condition", this.receiverCondition);
		this.set("Hover", this.hoverText);
		this.set("Hover_Item", this.hoverItem);
		this.set("Open_Url", this.openUrl);
		this.set("Suggest_Command", this.suggestCommand);
		this.set("Run_Command", this.runCommand);
		this.set("Sender_Permission", this.senderPermission);
		this.set("Receiver_Permission", this.receiverPermission);
	}

	// ----------------------------------------------------------------------------------
	// Getters
	// ----------------------------------------------------------------------------------

	/**
	 * Runs the script for the given player and the replacements,
	 * returns the output
	 *
	 * @param audience
	 * @param replacements
	 * @return
	 */
	public String getValue(FoundationPlayer audience, Map<String, Object> replacements) {

		// Replace variables in script
		final String script;
		final boolean replacingScript = Variables.isReplaceScript();

		try {
			Variables.setReplaceScript(false);

			script = Variables.replace(this.value, audience, replacements);

		} catch (final Throwable t) {
			final String errorHeadline = "Error replacing placeholders in variable!";

			CommonCore.logFramed(
					errorHeadline,
					"",
					"Variable: " + this.value,
					"Sender: " + audience,
					"Error: " + t.getMessage(),
					"",
					"Please report this issue!");

			Debugger.saveError(t, errorHeadline);

			return "";

		} finally {
			Variables.setReplaceScript(replacingScript);
		}

		Object result = null;

		try {
			result = JavaScriptExecutor.run(script, audience);

		} catch (final FoScriptException ex) {
			CommonCore.logFramed(
					"Error executing JavaScript in a variable!",
					"Variable: " + this.getFile(),
					"Line: " + ex.getErrorLine(),
					"Sender: " + audience,
					"Error: " + ex.getMessage(),
					"",
					"This is likely NOT our plugin bug, check Value key in " + this.getFile(),
					"that it returns a valid JavaScript code before reporting!");

			throw ex;
		}

		return result != null ? result.toString() : "";
	}

	/**
	 * Create the variable and append it to the existing component as if the player initiated it
	 *
	 * @param audience
	 * @param replacements
	 * @return
	 */
	public SimpleComponent build(FoundationPlayer audience, Map<String, Object> replacements) {
		final boolean replacingScript = Variables.isReplaceScript();

		try {
			Variables.setReplaceScript(false);

			if (this.senderPermission != null && !this.senderPermission.isEmpty() && !audience.hasPermission(this.senderPermission))
				return SimpleComponent.empty();

			if (this.senderCondition != null && !this.senderCondition.isEmpty()) {
				try {
					final Object result = JavaScriptExecutor.run(Variables.replace(this.senderCondition, audience, replacements), audience);

					if (result != null) {
						ValidCore.checkBoolean(result instanceof Boolean, "Variable '" + this.getFile() + "' option Condition must return boolean not " + (result == null ? "null" : result.getClass()));

						if (!((boolean) result))
							return SimpleComponent.empty();
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
							"Error executing Sender_Condition in a variable!",
							"Variable: " + this.getFile(),
							"Sender condition: " + this.senderCondition,
							"Sender: " + audience,
							"Error: " + ex.getMessage(),
							"",
							"This is likely NOT a plugin bug,",
							"check your JavaScript code in",
							this.getFile() + " in the 'Sender_Condition' key",
							"before reporting it to us.");

					throw ex;
				}
			}

			final String value = this.getValue(audience, replacements);

			if (value == null || value.isEmpty() || "null".equals(value))
				return SimpleComponent.empty();

			final SimpleComponent component = SimpleComponent.fromMini(value)
					.viewPermission(this.receiverPermission)
					.viewCondition(this.receiverCondition);

			if (!ValidCore.isNullOrEmpty(this.hoverText))
				component.onHover(Variables.replace(String.join("\n", this.hoverText), audience, replacements));

			if (this.hoverItem != null && !this.hoverItem.isEmpty()) {

				try {
					final Object result = JavaScriptExecutor.run(Variables.replace(this.hoverItem, audience, replacements), audience);

					if (result != null) {
						ValidCore.checkBoolean(result.getClass().getSimpleName().contains("ItemStack"), "Variable '" + this.getFile() + "' option Hover_Item must return ItemStack not " + result.getClass());

						component.onHover(Platform.convertItemStackToHoverEvent(result));
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
							"Error executing Hover_Item in a variable!",
							"Variable: " + this.getFile(),
							"Hover Item: " + this.hoverItem,
							"Sender: " + audience,
							"Error: " + ex.getMessage(),
							"",
							"This is likely NOT a plugin bug,",
							"check your JavaScript code in",
							this.getFile() + " in the 'Hover_Item' key",
							"before reporting it to us.");

					throw ex;
				}
			}

			if (this.openUrl != null && !this.openUrl.isEmpty())
				component.onClickOpenUrl(Variables.replace(this.openUrl, audience, replacements));

			if (this.suggestCommand != null && !this.suggestCommand.isEmpty())
				component.onClickSuggestCmd(Variables.replace(this.suggestCommand, audience, replacements));

			if (this.runCommand != null && !this.runCommand.isEmpty())
				component.onClickRunCmd(Variables.replace(this.runCommand, audience, replacements));

			return component;

		} finally {
			Variables.setReplaceScript(replacingScript);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Variable && this.key.equals(((Variable) obj).getKey());
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Creates a new variable and loads
	 *
	 * @param name
	 */
	public static void createVariable(String name) {
		loadedVariables.loadOrCreateItem(name);
	}

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
		return findVariable(name, null);
	}

	/**
	 * Return a variable, or null if not loaded
	 *
	 * @param name
	 * @param type
	 *
	 * @return
	 */
	public static Variable findVariable(@NonNull final String name, final Type type) {
		final Variable variable = loadedVariables.findItem(name);

		return variable != null && variable.getType() == type ? variable : null;
	}

	/**
	 * Return a list of all variables
	 *
	 * @return
	 */
	public static Collection<Variable> getVariables() {
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
		MESSAGE("message"),;

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

			throw new IllegalArgumentException("No such variable type: " + key + " Available: " + CommonCore.join(values()));
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