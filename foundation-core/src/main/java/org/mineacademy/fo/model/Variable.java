package org.mineacademy.fo.model;

import java.util.Collection;
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
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.ConfigItems;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;

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
				"-------------------------------------------------------------------------------------------------",
				Platform.getPluginName() + " supports dynamic, high performance JavaScript variables! They will",
				"automatically be used when calling Variables#replace for your messages.",
				"",
				"Because variables return a JavaScript value, you can sneak in code to play sounds or spawn",
				"monsters directly in your variable instead of it just displaying text!",
				"",
				"For example of how variables can be used, see our plugin ChatControl's wikipedia article:",
				"https://github.com/kangarko/ChatControl-Red/wiki/JavaScript-Variables",
				" -------------------------------------------------------------------------------------------------");

		this.loadConfiguration(prototypePath, "variables/" + file + ".yml");
	}

	// ----------------------------------------------------------------------------------
	// Loading
	// ----------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoad()
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
			throw new NullPointerException("(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Key' as variable name in " + this.getFileName());

		if (this.value == null)
			throw new NullPointerException("(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Value' key as what the variable shows in " + this.getFileName() + " (this can be a JavaScript code)");

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
			throw new IllegalArgumentException("(DO NOT REPORT, PLEASE FIX YOURSELF) The 'Key' variable in " + this.getFileName() + " must only contains letters, numbers or underscores. Do not write [] or {} there!");
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
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public String getValue(Audience sender, Map<String, Object> replacements) {

		// Replace variables in script
		final String script;
		final boolean replacingScript = Variables.isReplaceScript();

		try {
			Variables.setReplaceScript(false);

			script = Variables.replace(this.value, sender, replacements);

		} catch (final Throwable t) {
			final String errorHeadline = "Error replacing placeholders in variable!";

			CommonCore.logFramed(
					errorHeadline,
					"",
					"Variable: " + this.value,
					"Sender: " + sender,
					"Replacements: " + replacements,
					"Error: " + t.getMessage(),
					"",
					"Please report this issue!");

			if (FoException.isErrorSavedAutomatically())
				Debugger.saveError(t, errorHeadline);

			return "";

		} finally {
			Variables.setReplaceScript(replacingScript);
		}

		Object result = null;

		try {
			result = JavaScriptExecutor.run(script, sender);

		} catch (final FoScriptException ex) {
			CommonCore.logFramed(
					"Error executing JavaScript in a variable!",
					"Variable: " + this.getFileName(),
					"Line: " + ex.getErrorLine(),
					"Sender: " + sender,
					"Replacements: " + replacements,
					"Error: " + ex.getMessage(),
					"",
					"This is likely NOT our plugin bug, check Value key in " + this.getFileName(),
					"that it returns a valid JavaScript code before reporting!");

			throw ex;
		}

		return result != null ? result.toString() : "";
	}

	/**
	 * Builds this variable without additional components
	 *
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public String buildPlain(Audience sender, Map<String, Object> replacements) {
		if (this.senderPermission != null && !this.senderPermission.isEmpty() && !Platform.hasPermission(sender, this.senderPermission))
			return "";

		if (this.senderCondition != null && !this.senderCondition.isEmpty()) {
			final boolean replacingScript = Variables.isReplaceScript();

			try {
				Variables.setReplaceScript(false);

				final Object result = JavaScriptExecutor.run(Variables.replace(this.senderCondition, sender, replacements), sender);

				if (result != null) {
					ValidCore.checkBoolean(result instanceof Boolean, "Variable '" + this.getFileName() + "' option Condition must return boolean not " + (result == null ? "null" : result.getClass()));

					if (!((boolean) result))
						return "";
				}

			} catch (final FoScriptException ex) {
				CommonCore.logFramed(
						"Error executing Sender_Condition in a variable!",
						"Variable: " + this.getFileName(),
						"Sender condition: " + this.senderCondition,
						"Sender: " + sender,
						"Replacements: " + replacements,
						"Error: " + ex.getMessage(),
						"",
						"This is likely NOT a plugin bug,",
						"check your JavaScript code in",
						this.getFileName() + " in the 'Sender_Condition' key",
						"before reporting it to us.");

				throw ex;

			} finally {
				Variables.setReplaceScript(replacingScript);
			}
		}

		final String value = this.getValue(sender, replacements);

		return value == null || value.isEmpty() || "null".equals(value) ? "" : value;
	}

	/**
	 * Create the variable and append it to the existing component as if the player initiated it
	 *
	 * @param sender
	 * @param existingComponent
	 * @param replacements
	 * @return
	 */
	public SimpleComponentCore build(Audience sender, SimpleComponentCore existingComponent, Map<String, Object> replacements) {
		final boolean replacingScript = Variables.isReplaceScript();

		try {
			Variables.setReplaceScript(false);

			if (this.senderPermission != null && !this.senderPermission.isEmpty() && !Platform.hasPermission(sender, this.senderPermission))
				return SimpleComponentCore.of("");

			if (this.senderCondition != null && !this.senderCondition.isEmpty()) {
				try {
					final Object result = JavaScriptExecutor.run(Variables.replace(this.senderCondition, sender, replacements), sender);

					if (result != null) {
						ValidCore.checkBoolean(result instanceof Boolean, "Variable '" + this.getFileName() + "' option Condition must return boolean not " + (result == null ? "null" : result.getClass()));

						if (!((boolean) result))
							return SimpleComponentCore.of("");
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
							"Error executing Sender_Condition in a variable!",
							"Variable: " + this.getFileName(),
							"Sender condition: " + this.senderCondition,
							"Sender: " + sender,
							"Replacements: " + replacements,
							"Error: " + ex.getMessage(),
							"",
							"This is likely NOT a plugin bug,",
							"check your JavaScript code in",
							this.getFileName() + " in the 'Sender_Condition' key",
							"before reporting it to us.");

					throw ex;
				}
			}

			final String value = this.getValue(sender, replacements);

			if (value == null || value.isEmpty() || "null".equals(value))
				return SimpleComponentCore.of("");

			final SimpleComponentCore component = (existingComponent == null ? SimpleComponentCore.of(value) : existingComponent.append(value))
					.viewPermission(this.receiverPermission)
					.viewCondition(this.receiverCondition);

			if (!ValidCore.isNullOrEmpty(this.hoverText))
				component.onHover(Variables.replace(String.join("\n", this.hoverText), sender, replacements));

			if (this.hoverItem != null && !this.hoverItem.isEmpty()) {

				try {
					final Object result = JavaScriptExecutor.run(Variables.replace(this.hoverItem, sender, replacements), sender);

					if (result != null) {
						ValidCore.checkBoolean(result.getClass().getSimpleName().contains("ItemStack"), "Variable '" + this.getFileName() + "' option Hover_Item must return ItemStack not " + result.getClass());

						component.onHover(Platform.convertItemStackToHoverEvent(result));
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
							"Error executing Hover_Item in a variable!",
							"Variable: " + this.getFileName(),
							"Hover Item: " + this.hoverItem,
							"Sender: " + sender,
							"Replacements: " + replacements,
							"Error: " + ex.getMessage(),
							"",
							"This is likely NOT a plugin bug,",
							"check your JavaScript code in",
							this.getFileName() + " in the 'Hover_Item' key",
							"before reporting it to us.");

					throw ex;
				}
			}

			if (this.openUrl != null && !this.openUrl.isEmpty())
				component.onClickOpenUrl(Variables.replace(this.openUrl, sender, replacements));

			if (this.suggestCommand != null && !this.suggestCommand.isEmpty())
				component.onClickSuggestCmd(Variables.replace(this.suggestCommand, sender, replacements));

			if (this.runCommand != null && !this.runCommand.isEmpty())
				component.onClickRunCmd(Variables.replace(this.runCommand, sender, replacements));

			return component;

		} finally {
			Variables.setReplaceScript(replacingScript);
		}
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#equals(java.lang.Object)
	 */
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
		for (final Variable item : getVariables())
			if (item.getKey().equalsIgnoreCase(name) && (type == null || item.getType() == type))
				return item;

		return null;
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
	public static Set<String> getVariableNames() {
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

			throw new IllegalArgumentException("No such item type: " + key + ". Available: " + CommonCore.join(values()));
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