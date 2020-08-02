package org.mineacademy.fo.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 *
 */
public interface Actionable extends ConfigSerializable {

	/**
	 * The hover text or null if not set
	 *
	 * @return
	 */
	List<String> getHoverText();

	void setHoverText(List<String> hoverText);

	/**
	 * The JavaScript script pointing to a particular {@link ItemStack}
	 *
	 * @return
	 */
	String getHoverItem();

	void setHoverItem(String hoverItem);

	/**
	 * What URL should be opened on click? Null if none
	 *
	 * @return
	 */
	String getOpenUrl();

	void setOpenUrl(String openUrl);

	/**
	 * What command should be suggested on click? Null if none
	 *
	 * @return
	 */
	String getSuggestCommand();

	void setSuggestCommand(String suggestCommand);

	/**
	 * What command should be run on click? Null if none
	 *
	 * @return
	 */
	String getRunCommand();

	void setRunCommand(String runCommand);

	String getSenderPermission();

	void setSenderPermission(String senderPermission);

	String getReceiverPermission();

	void setReceiverPermission(String receiverPermission);

}
