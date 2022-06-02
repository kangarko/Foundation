package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.tool.Tool;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The semi-ready implementation of paged menu with all tools.
 * You only have to set the size, title and locked slots.<br><br>
 * To get started, override {@link #setup} method and customize your menu inside it.
 */
public abstract class AdvancedMenuTools extends AdvancedMenuPagged<Tool> {

    public AdvancedMenuTools(Player player) {
        super(player);
    }

    @Override
    protected List<Tool> getElements() {
        return Arrays.stream(Tool.getTools()).collect(Collectors.toList());
    }

    @Override
    protected ItemStack convertToItemStack(Tool tool) {
        return getToolButton(tool).getItem();
    }

    @Override
    protected void onElementClick(Player player, Tool tool, int slot, ClickType clickType) {
        getToolButton(tool).onClickedInMenu(player, this, clickType);
    }

}
