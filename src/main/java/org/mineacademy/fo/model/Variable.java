package org.mineacademy.fo.model;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Variable extends YamlConfig {

	/**
	 * The pattern to find singular [syntax_name] variables
	 */
	public static final Pattern SINGLE_VARIABLE_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

	/**
	 * A list of all loaded variables
	 */
	private static final ConfigItems<Variable> loadedVariables = ConfigItems.fromFolder("variable", "variables", Variable.class, false);

	static {
		loadedVariables.setVerbose(false);
	}

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

	// ----------------------------------------------------------------------------------
	// Loading
	// ----------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {
		this.key = getString("Key");
		Valid.checkNotNull(this.key, "Please set the 'Key' variable name in " + getFileName() + " variable file!");

		this.value = getString("Value");
		Valid.checkNotNull(this.value, "Please set the 'Value' variable output in " + getFileName() + " variable file!");

		this.senderCondition = getString("Sender_Condition");
		this.receiverCondition = getString("Receiver_Condition");
		this.hoverText = getStringList("Hover");
		this.hoverItem = getString("Hover_Item");
		this.openUrl = getString("Open_Url");
		this.suggestCommand = getString("Suggest_Command");
		this.runCommand = getString("Run_Command");
		this.senderPermission = getString("Sender_Permission");
		this.receiverPermission = getString("Receiver_Permission");
	}

	// ----------------------------------------------------------------------------------
	// Getters
	// ----------------------------------------------------------------------------------

	/**
	 * Runs the script for the given player and
	 * returns the output
	 *
	 * @param sender
	 * @return
	 */
	public String getValue(CommandSender sender) {
		final boolean replaceJs = Variables.REPLACE_JAVASCRIPT;

		try {
			Variables.REPLACE_JAVASCRIPT = false;

			// Replace variables in script
			final String script = Variables.replace(this.value, sender);

			return String.valueOf(JavaScriptExecutor.run(script, sender));

		} finally {
			Variables.REPLACE_JAVASCRIPT = replaceJs;
		}
	}

	/**
	 * Create the variable and append it to the existing component as if the player initiated it
	 *
	 * @param sender
	 * @param existingComponent
	 * @return
	 */
	public SimpleComponent build(CommandSender sender, SimpleComponent existingComponent) {

		if (this.senderPermission != null && !PlayerUtil.hasPerm(sender, this.senderPermission))
			return SimpleComponent.of("");

		if (this.senderCondition != null) {
			final Object result = JavaScriptExecutor.run(this.senderCondition, sender);
			Valid.checkBoolean(result instanceof Boolean, "Variable '" + getName() + "' option Condition must return boolean not " + result.getClass());

			if ((boolean) result == false)
				return SimpleComponent.of("");
		}

		final String value = this.getValue(sender);

		if (value == null || "".equals(value) || "null".equals(value))
			return SimpleComponent.of("");

		final SimpleComponent component = existingComponent
				.append(Variables.replace(value, sender))
				.viewPermission(this.receiverPermission)
				.viewCondition(this.receiverCondition);

		if (!Valid.isNullOrEmpty(this.hoverText))
			component.onHover(Variables.replace(this.hoverText, sender));

		if (this.hoverItem != null) {
			final Object result = JavaScriptExecutor.run(Variables.replace(this.hoverItem, sender), sender);
			Valid.checkBoolean(result instanceof ItemStack, "Variable '" + getName() + "' option Hover_Item must return ItemStack not " + result.getClass());

			component.onHover((ItemStack) result);
		}

		if (this.openUrl != null)
			component.onClickOpenUrl(Variables.replace(this.openUrl, sender));

		if (this.suggestCommand != null)
			component.onClickSuggestCmd(Variables.replace(this.suggestCommand, sender));

		if (this.runCommand != null)
			component.onClickRunCmd(Variables.replace(this.runCommand, sender));

		return component;
	}

	// ----------------------------------------------------------------------------------
	// Serialize
	// ----------------------------------------------------------------------------------

	/**
	 * Turn this class into a saveable format to the file
	 *
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
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
				"Receiver_Permission", this.receiverPermission);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	public static void loadVariables() {
		loadedVariables.loadItems();
	}

	public static void removeVariable(final Variable variable) {
		loadedVariables.removeItem(variable);
	}

	public static boolean isVariableLoaded(final String name) {
		return loadedVariables.isItemLoaded(name);
	}

	public static Variable findVariable(@NonNull final String name) {
		return loadedVariables.findItem(name);
	}

	public static List<Variable> getVariables() {
		return loadedVariables.getItems();
	}

	public static List<String> getVariableNames() {
		return loadedVariables.getItemNames();
	}
}