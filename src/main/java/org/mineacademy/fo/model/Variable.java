package org.mineacademy.fo.model;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Variable extends YamlConfig implements Actionable {

	/**
	 * A list of all loaded variables
	 */
	private static final ConfigItems<Variable> loadedVariables = new ConfigItems<>("variable", "variables", Variable.class);

	/**
	 * Where can this variable be used
	 */
	@Getter
	private VariableScope scope;

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

	// ----------------------------------------------------------------------------------
	// Actionable
	// ----------------------------------------------------------------------------------

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
		this.key = isSet("Key") ? getString("Key") : null;
		this.value = isSet("Value") ? getString("Value") : null;
		this.scope = isSet("Scope") ? get("Scope", VariableScope.class) : VariableScope.FORMAT;
		this.hoverText = isSet("Hover") ? getStringList("Hover") : null;
		this.hoverItem = isSet("Hover_Item") ? getString("Hover_Item") : null;
		this.openUrl = isSet("Open_Url") ? getString("Open_Url") : null;
		this.suggestCommand = isSet("Suggest_Command") ? getString("Suggest_Command") : null;
		this.runCommand = isSet("Run_Command") ? getString("Run_Command") : null;
		this.senderPermission = isSet("Sender_Permission") ? getString("Sender_Permission") : null;
		this.receiverPermission = isSet("Receiver_Permission") ? getString("Receiver_Permission") : null;
	}

	// ----------------------------------------------------------------------------------
	// Getters
	// ----------------------------------------------------------------------------------

	/**
	 * Runs the script for the given player and
	 * returns the output
	 *
	 * @param player
	 * @return
	 */
	public String getValue(CommandSender player) {
		// Replace variables in script
		final String script = Variables.replace(scope, this.value, player);

		return String.valueOf(JavaScriptExecutor.run(script, player));
	}

	// ----------------------------------------------------------------------------------
	// Setters
	// ----------------------------------------------------------------------------------

	/**
	 * Set where this variable may be used
	 *
	 * @param score the score to set
	 */
	public void setScope(VariableScope score) {
		this.scope = score;

		save();
	}

	/**
	 * Set what we should find in chat such
	 * as {player} or [item]
	 *
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;

		save();
	}

	/**
	 * Sets the script used to replace the key
	 * such as "player.getName()" for the {@link #getKey()}
	 *
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;

		save();
	}

	/**
	 * Set the hover text for this format
	 *
	 * @param hoverText the hoverText to set
	 */
	@Override
	public void setHoverText(List<String> hoverText) {
		this.hoverText = hoverText;

		save();
	}

	/**
	 * Set the script to get a particular item stack
	 *
	 * @param hoverItem the hoverItem to set
	 */
	@Override
	public void setHoverItem(String hoverItem) {
		this.hoverItem = hoverItem;

		save();
	}

	/**
	 * Set what URL should be opened for this command
	 *
	 * @param openUrl the openUrl to set
	 */
	@Override
	public void setOpenUrl(String openUrl) {
		this.openUrl = openUrl;

		save();
	}

	/**
	 * Set what command to suggest on this command
	 *
	 * @param suggestCommand the suggestCommand to set
	 */
	@Override
	public void setSuggestCommand(String suggestCommand) {
		this.suggestCommand = suggestCommand;

		save();
	}

	@Override
	public void setRunCommand(String runCommand) {
		this.runCommand = runCommand;

		save();
	}

	@Override
	public void setSenderPermission(String senderPermission) {
		this.senderPermission = senderPermission;

		save();
	}

	@Override
	public void setReceiverPermission(String receiverPermission) {
		this.receiverPermission = receiverPermission;

		save();
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
				"Key", key,
				"Value", value,
				"Hover", hoverText,
				"Hover_Item", hoverItem,
				"Open_Url", openUrl,
				"Suggest_Command", suggestCommand,
				"Run_Command", runCommand,
				"Sender_Permission", senderPermission,
				"Receiver_Permission", receiverPermission);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	public static void loadVariables() {
		loadedVariables.loadItems();
	}

	public static Variable loadOrCreateVariable(final String name) {
		return loadedVariables.loadOrCreateItem(name);
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

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Where a variable can be used
	 */
	public enum VariableScope {

		/**
		 * Variable may be used in chat formatting
		 * such as {player}
		 */
		FORMAT,

		/**
		 * Players can type this variable to their chat msg and it will be translated
		 * such as [item]
		 */
		CHAT
	}
}