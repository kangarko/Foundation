package org.mineacademy.fo.menu;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.SoundUtil;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@SuppressWarnings("unused")
public abstract class AdvancedMenu extends Menu {

    /**
     * The player for whom the menu is opened.
     */
    @Getter
    private final Player player;
    private final Class<? extends AdvancedMenu> parent;
    /**
     * Contains buttons and their slots.
     */
    @Getter
    private final TreeMap<Integer, Button> buttons = new TreeMap<>();
    /**
     * Custom slots and their itemStacks.<br>
     * You can define them in {@link #addItem} method.
     */
    @Getter
    private final TreeMap<Integer, ItemStack> items = new TreeMap<>();
    /**
     * In AdvancedMenu, locked slots are filled with {@link #getWrapperItem()}.<br>
     * In AdvancedMenuPaginated, <i>elementsItems</i> are not displayed on these slots and slots
     * are filled with {@link #getWrapperItem()} only if {@link AdvancedMenuPagged#fillWithWrapper} is true.
     */
    protected List<Integer> lockedSlots = new ArrayList<>();
    /**
     * The material of the wrapper item.
     * See {@link #wrapperItem} for more info.
     */
    @Getter @Setter
    private CompMaterial wrapperMaterial = CompMaterial.GRAY_STAINED_GLASS_PANE;
    /**
     * Convenient item that you can use to close some menu slots.<br>
     * By default, displays on empty locked slots in MenuPaginated.<br>
     * Default item is gray stained-glass.
     * Set this item to whether you want by {@link #setWrapperItem}.
     * To disable item set it to null.
     */
    @Getter @Setter @Nullable
    private ItemStack wrapperItem = ItemCreator.of(wrapperMaterial, "").build().make();

    public AdvancedMenu(Player player){
        this(player, null);
    }

    public AdvancedMenu(Player player, Class<? extends AdvancedMenu> parent){
        this.player = player;
        this.parent = (parent == null ? getClass() : parent);
    }

    /**
     * Add button to the menu.
     * @param slot the slot the button should be displayed on
     * @param btn the button
     */
    protected void addButton(Integer slot, Button btn){
        buttons.put(slot, btn);
    }

    /**
     * Add custom item with no behavior to the menu.
     * If you want item to have behavior use {@link #addButton}.
     * @param slot the slot the item should be placed on
     * @param item the item
     */
    protected void addItem(Integer slot, ItemStack item){
        items.put(slot, item);
    }

    /**
     * Actions to be taken when opening or updating the menu.<br>
     * It automatically runs when the menu opens. But if you make some changes after calling super()
     * in your child class you must call init() manually in the constructor after all changes.
     */
    protected void init(){}

    /**
     * Redraw the menu without moving the cursor to the center.
     */
    protected void refreshMenu(){
        init();
        redraw();
    }

    /**
     * Display this menu to the player given in the constructor.
     */
    public final void display(){
        displayTo(getPlayer());
    }

    /**
     * See {@link #getReturnBackButton(ItemStack)}
     */
    protected Button getReturnBackButton(){
        return getReturnBackButton(MenuUtil.defaultReturnBackItem);
    }

    /**
     * Get the button that returns player to the parent menu given in the constructor.
     * If the parent is not given it will return player to the same menu.
     * If item is not given, it will get its item from {@link MenuUtil#defaultReturnBackItem}.<br>
     * You can override this button and add some your logic.
     * @return the return button
     */
    protected Button getReturnBackButton(@NotNull ItemStack item){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                openNewMenu(player, parent);
            }

            @Override
            public ItemStack getItem() {
                return item;
            }
        };
    }

    /**
     * Get the button that displays new menu to the player.
     * @param item how the button should look like
     * @param to what menu the player should be sent to
     * @return the button
     */
    protected Button getMenuButton(ItemStack item, Class<? extends AdvancedMenu> to){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                openNewMenu(player, to);
            }

            @Override
            public ItemStack getItem() {
                return item;
            }
        };
    }

    /**
     * Create a new instance of the menu from the given class and display it to the player.
     */
    private void openNewMenu(Player player, Class<? extends AdvancedMenu> menu){
        try{
            menu.getDeclaredConstructor(Player.class).newInstance(player).display();
        }
        catch (NoSuchMethodException e){
            e.printStackTrace();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * See {@link #getRefreshButton(ItemStack)}.
     */
    protected Button getRefreshButton(){
        return getRefreshButton(MenuUtil.defaultRefreshItem);
    }

    /**
     * Get the button that refreshes the menu.<br>
     * If the given item is null, it will get its item from {@link MenuUtil#defaultRefreshItem}.<br>
     * This button does not imply additional behavior, but you still
     * can override it if you want to add some logic.
     * @return the refresh button
     */
    protected Button getRefreshButton(@NotNull ItemStack item){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                SoundUtil.Play.POP(player);
                refreshMenu();
            }

            @Override
            public ItemStack getItem() {
                return item;
            }
        };
    }

    /**
     * Make the button from the tool. It gives player one piece of this tool.<br>
     * This button gets its display-items from {@link #getAlreadyHaveTool} and {@link #getClickToGetTool}
     * so you can override them to set your custom items and messages.<br>
     * Or you can override this whole method to set your custom items and logic.
     * @param tool the tool we should give
     * @return the button
     */
    protected Button getToolButton(Tool tool){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (!player.getInventory().contains(tool.getItem())){
                    SoundUtil.Play.POP_HIGH(player);
                    player.getInventory().addItem(tool.getItem());
                }
                else{
                    SoundUtil.Play.POP_LOW(player);
                    player.getInventory().removeItem(tool.getItem());
                }
                refreshMenu();
            }

            @Override
            public ItemStack getItem() {
                return (hasItem(getPlayer(), tool) ? getAlreadyHaveTool(tool) : getClickToGetTool(tool));
            }
        };
    }

    /**
     * Get the item of the tool of this tool is already contained in the player's inventory.
     * @param tool the tool
     * @return the result item
     */
    protected ItemStack getAlreadyHaveTool(Tool tool){
        return ItemCreator.of(tool.getItem()).lore("&cYou already have this item").glow(true).build().make();
    }

    /**
     * Get the item of the tool if this tool is not contained in the player's inventory.
     * @param tool the tool
     * @return the result item
     */
    protected ItemStack getClickToGetTool(Tool tool){
        return ItemCreator.of(tool.getItem()).lore("&2Click to get this item").build().make();
    }

    /**
     * Get the button that shows info about the menu.
     * By default, does nothing when clicked, but you can override it and add your behavior.
     * This button gets its info from {@link #getInfo()}. So you can override it and set your custom information.
     * @return the button
     */
    protected Button getInfoButton(){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
            }

            @Override
            public ItemStack getItem() {
                return Button.makeInfo(getInfo()).getItem();
            }
        };
    }

    /**
     * Checks if the player has the specified tool in the inventory.
     */
    private boolean hasItem(Player player, Tool tool){
        return player.getInventory().contains(tool.getItem());
    }

    @Override
    protected void onMenuClick(Player player, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {
        if (getButtons().containsKey(slot)){
            getButtons().get(slot).onClickedInMenu(player, this, click);
        }
    }

    @Override
    public ItemStack getItemAt(int slot) {
        if (getItems().containsKey(slot)){
            return getItems().get(slot);
        }
        if (getButtons().containsKey(slot)){
            return getButtons().get(slot).getItem();
        }
        if (lockedSlots.contains(slot)){
            return getWrapperItem();
        }
        return null;
    }
}