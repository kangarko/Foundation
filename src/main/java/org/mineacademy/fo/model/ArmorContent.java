package org.mineacademy.fo.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;

/**
 * Represents a player armor content
 */
public final class ArmorContent implements ConfigSerializable {

	/**
	 * The items:
	 * [0] = helmet
	 * [1] = chestplate
	 * [2] = leggings
	 * [3] = boots
	 */
	private final ItemStack[] items;

	/**
	 * Create a new armor content - the length of the given array must be 4,
	 * and the items stored as follows: [0] - helmet, [1] = chestplate, [2] = leggings and [3] = boots
	 *
	 * You can specify null itemstacks as long as array lenght is 4.
	 *
	 * @param items
	 */
	public ArmorContent(ItemStack... items) {
		Valid.checkBoolean(items.length == 4, "Armor must have the length of 4, not " + items.length);

		this.items = items;
	}

	/**
	 * Return the helmet, or null
	 *
	 * @return
	 */
	public ItemStack getHelmet() {
		return items[0];
	}

	/**
	 * Return the chestplate, or null
	 *
	 * @return
	 */
	public ItemStack getChestplate() {
		return items[1];
	}

	/**
	 * Return the leggings, or null
	 *
	 * @return
	 */
	public ItemStack getLeggings() {
		return items[2];
	}

	/**
	 * Return the boots, or null
	 *
	 * @return
	 */
	public ItemStack getBoots() {
		return items[3];
	}

	/**
	 * Return the itemstack (or null) by order
	 * For orders please see the constructor of this class
	 *
	 * @param order
	 * @return
	 */
	public ItemStack getByOrder(int order) {
		Valid.checkBoolean(order >= 0 && order <= 3, "Order out of range 0-3: " + order);

		return items[order];
	}

	/**
	 * Gives the itemstack to this player
	 *
	 * @param player
	 */
	public void give(Player player) {
		final PlayerInventory inv = player.getInventory();

		inv.setHelmet(getHelmet());
		inv.setChestplate(getChestplate());
		inv.setLeggings(getLeggings());
		inv.setBoots(getBoots());
	}

	/**
	 * Converts this class into a serializable map you can save in your settings
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExist("helmet", getHelmet());
		map.putIfExist("chestplate", getChestplate());
		map.putIfExist("leggings", getLeggings());
		map.putIfExist("boots", getBoots());

		return map;
	}

	/**
	 * Creates a new armor content from a map, typically the one you store
	 * in your settings file
	 *
	 * @param map
	 * @return
	 */
	public static ArmorContent deserialize(SerializedMap map) {
		final ItemStack helmet = map.getItem("helmet");
		final ItemStack chestplate = map.getItem("chestplate");
		final ItemStack leggings = map.getItem("leggings");
		final ItemStack boots = map.getItem("boots");

		return new ArmorContent(helmet, chestplate, leggings, boots);
	}
}