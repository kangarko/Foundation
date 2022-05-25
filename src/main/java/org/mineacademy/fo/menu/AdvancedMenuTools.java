package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AdvancedMenuTools extends AdvancedMenuPagged<Tool> {

    public AdvancedMenuTools(Player player) {
        this(player, null);
    }

    public AdvancedMenuTools(Player player, Class<? extends AdvancedMenu> parent){
        super(player, parent);
        init();
    }

    @Override
    protected List<Tool> getElements() {
        return Arrays.stream(ToolRegistry.getTools()).collect(Collectors.toList());
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
