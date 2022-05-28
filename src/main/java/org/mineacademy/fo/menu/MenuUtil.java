package org.mineacademy.fo.menu;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

/**
 * Utility class containing some useful features for menus.
 */
public class MenuUtil {

    public static ItemStack defaultReturnBackItem = ItemCreator.of(CompMaterial.OAK_DOOR, "&7Go back").build().make();
    public static ItemStack defaultRefreshItem = ItemCreator.of(CompMaterial.REDSTONE, "&7Refresh menu").build().make();
    public static ItemStack defaultInfoItem = ItemCreator.of(CompMaterial.BOOK).build().make();
    public static ItemStack defaultMenuItem = ItemCreator.of(CompMaterial.APPLE).build().make();

}