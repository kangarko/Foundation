package org.mineacademy.fo.model;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Valid;
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
		this.key = getString("Key");
		Valid.checkNotNull(this.key, "Please set the 'Key' variable name in " + getFileName() + " variable file!");

		this.value = getString("Value");
		Valid.checkNotNull(this.value, "Please set the 'Value' variable output in " + getFileName() + " variable file!");

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
	 * @param player
	 * @return
	 */
	public String getValue(Player player) {
		// Replace variables in script
		final String script = Variables.replace(this.value, player);

		return String.valueOf(JavaScriptExecutor.run(script, player));
	}

	// ----------------------------------------------------------------------------------
	// Setters
	// ----------------------------------------------------------------------------------

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