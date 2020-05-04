package org.mineacademy.fo.model;

import java.util.List;

import org.bukkit.inventory.ItemStack;

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
}
