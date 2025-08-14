package org.mineacademy.fo.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
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
	 * A map of all variables by their key
	 */
	private static final Map<String, Variable> variablesByKeys = new HashMap<>();

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
	 * The permission the sender must have to show the part
	 */
	@Getter
	private String senderPermission;

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
	private Variable(final String file) {
		final String prototypePath = PROTOTYPE_PATH.apply(file);

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
		this.senderPermission = this.getString("Sender_Permission");

		// Correct common mistakes
		if (this.type == null)
			this.type = Type.FORMAT;

		// Check for known mistakes
		if (this.key == null || this.key.isEmpty())
			throw new FoException("(DO NOT REPORT, FIX IT YOURSELF) Set 'Key' as variable name in " + this.getFile(), false);

		if (this.value == null)
			throw new FoException("(DO NOT REPORT, FIX IT YOURSELF) Set 'Value' key as what the variable shows in " + this.getFile() + " (this must be a valid JavaScript code, if unsure put a string there and surround with '' quotes)", false);

		final char startChar = this.key.charAt(0);
		final char endChar = this.key.charAt(this.key.length() - 1);

		if (startChar == '{' || startChar == '[')
			this.key = this.key.substring(1);

		if (endChar == '}' || endChar == ']')
			this.key = this.key.substring(0, this.key.length() - 1);

		if (this.type == Type.MESSAGE) {
			this.hoverText = this.getStringList("Hover");
			this.hoverItem = this.getString("Hover_Item");
			this.openUrl = this.getString("Open_Url");
			this.suggestCommand = this.getString("Suggest_Command");
			this.runCommand = this.getString("Run_Command");
		}

		if (this.isSet("Receiver_Condition") || this.isSet("Receiver_Permission"))
			CommonCore.warning("The 'Receiver_Condition' and 'Receiver_Permission' keys are no longer supported in variables and will be removed from " + this.getFile());

		// Test for key validity
		if (!VALID_KEY_PATTERN.matcher(this.key).matches())
			throw new FoException("(DO NOT REPORT, PLEASE FIX YOURSELF) The 'Key' variable in " + this.getFile() + " must only contains letters, numbers or underscores. Do not write [] or {} there! Got: '" + this.key + "'", false);

		// Always save to update keys
		this.save();
	}

	@Override
	public void onSave() {
		this.set("Type", this.type);
		this.set("Key", this.key);
		this.set("Value", this.value);
		this.set("Sender_Condition", this.senderCondition);
		this.set("Sender_Permission", this.senderPermission);
		this.set("Hover", this.hoverText);
		this.set("Hover_Item", this.hoverItem);
		this.set("Open_Url", this.openUrl);
		this.set("Suggest_Command", this.suggestCommand);
		this.set("Run_Command", this.runCommand);
		this.set("Receiver_Condition", null);
		this.set("Receiver_Permission", null);
	}

	// ----------------------------------------------------------------------------------
	// Getters
	// ----------------------------------------------------------------------------------

	/**
	 * Runs the script for the given player and the replacements,
	 * returns the output
	 *
	 * @param variables
	 * @return
	 */
	public String getValue(final Variables variables) {

		// Replace variables in script
		final String script;
		final boolean replacingScript = Variables.isReplaceScript();

		try {
			Variables.setReplaceScript(false);

			script = variables.replaceLegacy(this.value);

		} catch (final Throwable t) {
			final String errorHeadline = "Error replacing placeholders in variable!";

			CommonCore.logFramed(
					errorHeadline,
					"",
					"Variable: " + this.value,
					"Sender: " + variables.audience(),
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
			result = JavaScriptExecutor.run(script, variables);

		} catch (final FoScriptException ex) {
			CommonCore.logFramed(
					"Error executing JavaScript in a variable!",
					"Variable: " + this.getFile(),
					(ex instanceof FoScriptException ? "Line: " + ex.getErrorLine() : ""),
					"Sender: " + variables.audience(),
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
	 * @param variables
	 * @return
	 */
	public SimpleComponent build(final FoundationPlayer audience, Variables variables) {
		final boolean replacingScript = Variables.isReplaceScript();

		try {
			Variables.setReplaceScript(false);

			if (this.senderPermission != null && !this.senderPermission.isEmpty() && !audience.hasPermission(this.senderPermission))
				return SimpleComponent.empty();

			if (this.senderCondition != null && !this.senderCondition.isEmpty())
				try {
					final Object result = JavaScriptExecutor.run(variables.replaceLegacy(this.senderCondition), audience);

					if (result != null) {
						if (!(result instanceof Boolean))
							throw new FoException("Variable '" + this.getFile() + "' option Condition must return boolean not " + (result == null ? "null" : result.getClass() + ": " + result), false);

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

			final String value = this.getValue(variables);

			if (value == null || value.isEmpty() || "null".equals(value))
				return SimpleComponent.empty();

			SimpleComponent component = SimpleComponent.fromMiniAmpersand(value);

			if (!ValidCore.isNullOrEmpty(this.hoverText))
				component = component.onHoverLegacy(variables.replaceLegacyArray(CommonCore.toArray(this.hoverText)));

			if (this.hoverItem != null && !this.hoverItem.isEmpty())
				try {
					final Object result = JavaScriptExecutor.run(variables.replaceLegacy(this.hoverItem), audience);

					if (result != null) {
						if (!result.getClass().getSimpleName().contains("ItemStack"))
							throw new FoException("Variable '" + this.getFile() + "' option Hover_Item must return ItemStack not " + result.getClass(), false);

						component = component.onHover(Platform.convertItemStackToHoverEvent(result));
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

			if (this.openUrl != null && !this.openUrl.isEmpty())
				component = component.onClickOpenUrl(variables.replaceLegacy(this.openUrl));

			if (this.suggestCommand != null && !this.suggestCommand.isEmpty())
				component = component.onClickSuggestCmd(variables.replaceLegacy(this.suggestCommand));

			if (this.runCommand != null && !this.runCommand.isEmpty())
				component = component.onClickRunCmd(variables.replaceLegacy(this.runCommand));

			return component;

		} finally {
			Variables.setReplaceScript(replacingScript);
		}
	}

	/**
	 * Create the variable as legacy, no interactive nor receiver conditional components
	 * are supported.
	 *
	 * @param audience
	 * @param placeholders
	 * @return
	 */
	/*public String buildLegacy(final FoundationPlayer audience, final Map<String, Object> placeholders) {
		final Variables variables = Variables.builder(audience).placeholders(placeholders);
	
		return this.buildLegacy(audience, variables);
	}*/

	/**
	 * Create the variable as legacy, no interactive nor receiver conditional components
	 * are supported.
	 *
	 * @param variables
	 * @return
	 */
	public String buildLegacy(final Variables variables) {
		final boolean replacingScript = Variables.isReplaceScript();

		try {
			Variables.setReplaceScript(false);

			if (this.senderPermission != null && !this.senderPermission.isEmpty() && !variables.audience().hasPermission(this.senderPermission))
				return "";

			if (this.senderCondition != null && !this.senderCondition.isEmpty())
				try {
					final Object result = JavaScriptExecutor.run(variables.replaceLegacy(this.senderCondition), variables.audience());

					if (result != null) {
						if (!(result instanceof Boolean))
							throw new FoException("Variable '" + this.getFile() + "' option Condition must return boolean not " + (result == null ? "null" : result.getClass() + ": " + result), false);

						if (!((boolean) result))
							return "";
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
							"Error executing Sender_Condition in a variable!",
							"Variable: " + this.getFile(),
							"Sender condition: " + this.senderCondition,
							"Sender: " + variables.audience(),
							"Error: " + ex.getMessage(),
							"",
							"This is likely NOT a plugin bug,",
							"check your JavaScript code in",
							this.getFile() + " in the 'Sender_Condition' key",
							"before reporting it to us.");

					throw ex;
				}

			final String value = this.getValue(variables);

			return value == null || value.isEmpty() || "null".equals(value) ? "" : value;

		} finally {
			Variables.setReplaceScript(replacingScript);
		}
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Variable && this.key.equals(((Variable) obj).getKey());
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Load all variables from variables/ folder
	 */
	public static void loadVariables() {
		loadedVariables.loadItems();

		// Cache by the actual key not file name
		variablesByKeys.clear();

		for (final Variable variable : loadedVariables.getItems())
			variablesByKeys.put(variable.getKey(), variable);
	}

	/**
	 * Return a variable, or null if not loaded
	 *
	 * @param name
	 * @param type
	 *
	 * @return
	 */
	public static Variable findVariableByFileName(@NonNull final String name, final Type type) {
		final Variable variable = loadedVariables.findItem(name);

		return variable != null && variable.getType() == type ? variable : null;
	}

	/**
	 * Return a variable, or null if not loaded
	 *
	 * @param key the placeholder name without {}
	 * @param type
	 *
	 * @return
	 */
	public static Variable findVariableByKey(@NonNull final String key, final Type type) {
		final Variable variable = variablesByKeys.get(key);

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
	public static List<String> getVariableFileNames() {
		return loadedVariables.getItemNames();
	}

	/**
	 * Return a list of all variable names
	 *
	 * @return
	 */
	public static Set<String> getVariableKeyNames() {
		return variablesByKeys.keySet();
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
		public static Type fromKey(final String key) {
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