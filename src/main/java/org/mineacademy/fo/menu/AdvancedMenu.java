package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.SoundUtil;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Basic advanced menu.
 * Supports adding items, buttons and filling locked slots with wrapper item.
 * Also contains some ready buttons (Menu, ReturnBack, Refresh, etc.).<br><br>
 * To get started, override {@link #setup} method and customize your menu inside it.
 */
public abstract class AdvancedMenu extends Menu {

    /**
     * The player watching the menu.
     */
    private final Player player;
    /**
     * Contains buttons and their slots.
     */
    private final TreeMap<Integer, Button> buttons = new TreeMap<>();
    /**
     * Custom slots and their itemStacks.<br>
     * You can define them in {@link #addItem} method.
     */
    private final TreeMap<Integer, ItemStack> items = new TreeMap<>();
    /**
     * In AdvancedMenu, locked slots are filled with {@link #getWrapperItem()}.<br>
     * In AdvancedMenuPaginated, <i>elementsItems</i> are not displayed on these slots and slots
     * are filled with {@link #getWrapperItem()}.
     */
    protected List<Integer> lockedSlots = new ArrayList<>();
    /**
     * The menu which is opened from {@link #getReturnBackButton}.
     */
    private Class<? extends AdvancedMenu> parentMenu = getClass();
    /**
     * Convenient item that you can use to close some menu slots.<br>
     * By default, displays on empty locked slots in MenuPaginated.<br>
     * Default item is gray stained-glass.
     * Set this item to whether you want by {@link #setWrapper}.
     * To disable item set it to null.
     */
    private ItemStack wrapperItem = ItemCreator.of(MenuUtil.defaultWrapperMaterial, "").make();

    public AdvancedMenu(Player player){
        this.player = player;
    }

    /**
     * Add button to the menu.<br>
     * <b>Use it only inside {@link #setup} method to avoid errors!</b><br>
     * @param slot the slot the button should be displayed on
     * @param btn the button
     */
    protected final void addButton(Integer slot, Button btn){
        buttons.put(slot, btn);
    }

    /**
     * Add custom item with no behavior to the menu.<br>
     * <b>Use it only inside {@link #setup} method to avoid errors!</b><br>
     * If you want item to have behavior use {@link #addButton}.
     * @param slot the slot the item should be placed on
     * @param item the item
     */
    protected final void addItem(Integer slot, ItemStack item){
        items.put(slot, item);
    }

    /**
     * Redraw the menu without moving the cursor to the center.
     */
    protected void refreshMenu(){
        redraw();
    }

    /**
     * Get a player watching the menu.
     */
    public final Player getPlayer(){
        return this.player;
    }

    /**
     * Get the {@link #items}
     */
    protected final TreeMap<Integer, ItemStack> getItems(){
        return items;
    }

    /**
     * Get the {@link #buttons}
     */
    protected final TreeMap<Integer, Button> getButtons(){
        return buttons;
    }

    /**
     * Get the {@link #lockedSlots}
     */
    protected final List<Integer> getLockedSlots(){
        return lockedSlots;
    }

    /**
     * For {@link AdvancedMenu}, set the slots that should be filled with {@link #wrapperItem}.
     * For {@link AdvancedMenuPagged}, set the slots the main elements should NOT be placed on.
     */
    protected final void setLockedSlots(Integer... slots){
        lockedSlots.clear();
        lockedSlots.addAll(Arrays.asList(slots));
    }

    /**
     * See {@link #setLockedSlots(Integer...)} for the detailed description.<br><br>
     * Figures available: 9x6_bounds, 9x6_circle, 9x6_rows, 9x6_columns, 9x6_six_slots,
     * 9x6_two_slots, 9x3_bounds, 9x3_one_slot, 9x1_one_slot.
     */
    protected final void setLockedSlots(String figure){
        switch (figure) {
            case ("9x6_bounds"): setLockedSlots(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53); break;
            case ("9x6_circle"): setUnlockedSlots(12, 13, 14, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 39, 40, 41); break;
            case ("9x6_rows"): setLockedSlots(0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53); break;
            case ("9x6_columns"): setLockedSlots(0, 8, 9, 18, 27, 36, 45, 17, 26, 35, 44, 53); break;
            case ("9x6_six_slots"): setUnlockedSlots(21, 22, 23, 30, 31, 32); break;
            case ("9x6_two_slots"): setUnlockedSlots(22, 31); break;
            case ("9x3_bounds"): setUnlockedSlots(10, 11, 12, 13, 14, 15, 16); break;
            case ("9x3_one_slot"): setUnlockedSlots(13); break;
            case ("9x1_one_slot"): setUnlockedSlots(4); break;
            default: new ArrayList<>();
        }
    }

    /**
     * Get the {@link #parentMenu}.
     */
    protected final Class<? extends AdvancedMenu> getParentMenu(){
        return this.parentMenu;
    }

    /**
     * Set the {@link #parentMenu}.
     */
    protected final void setParent(Class<? extends AdvancedMenu> parent){
        this.parentMenu = parent;
    }

    /**
     * Get the {@link #wrapperItem}
     */
    protected final ItemStack getWrapperItem(){
        return wrapperItem;
    }

    protected final void setWrapper(CompMaterial material){
        wrapperItem = ItemCreator.of(material, "").make();
    }

    protected final void setWrapper(ItemStack item){
        wrapperItem = item;
    }

    /**
     * Display this menu to the player given in the constructor.
     */
    public void display(){
        setup();
        displayTo(getPlayer());
    }

    /**
     * Does the same as {@link #getReturnBackButton(ItemStack)}.
     * Uses the default button from {@link MenuUtil#defaultReturnBackItem}.
     */
    protected final Button getReturnBackButton(){
        return getReturnBackButton(MenuUtil.defaultReturnBackItem);
    }

    /**
     * Get the button that returns player to the parent menu given in the constructor.
     * If the parent is not given it will return player to the same menu.
     * If item is not given, it will get its item from {@link MenuUtil#defaultReturnBackItem}.<br><br>
     * <b>NOTE</b> that this button returns player to the non-personalized menu.
     * To use personalized menus, use {@link #getMenuButton}.<br><br>
     * @return the return button
     */
    protected final Button getReturnBackButton(@NotNull ItemStack item){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, AdvancedMenu menu, ClickType click) {
                newInstanceOf(player, parentMenu).display();
            }

            @Override
            public ItemStack getItem() {
                return item;
            }
        };
    }

    /**
     * Does the same as {@link #getRefreshButton(ItemStack)}.
     * Uses the default button from {@link MenuUtil#defaultRefreshItem}.
     */
    protected final Button getRefreshButton(){
        return getRefreshButton(MenuUtil.defaultRefreshItem);
    }

    /**
     * Get the button that refreshes the menu.<br>
     * If the given item is null, it will get its item from {@link MenuUtil#defaultRefreshItem}.<br>
     * This button does not imply any additional behavior.
     * @return the refresh button
     */
    protected final Button getRefreshButton(@NotNull ItemStack item){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, AdvancedMenu menu, ClickType click) {
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
     * <b>Attention! Please use {@link #getMenuButton(Class)} if possible because it is much more efficient!
     * Use this method only if your menu constructor is custom or the menu must be created by instance
     * (and not by class).</b><br><br>
     * Does the same as {@link #getMenuButton(ItemStack, AdvancedMenu)}.
     * Uses the default button from {@link MenuUtil#defaultMenuItem}.
     */
    protected final Button getMenuButton(AdvancedMenu to){
        return getMenuButton(MenuUtil.defaultMenuItem, to);
    }

    /**
     * <b>Attention! Please use {@link #getMenuButton(ItemStack, Class)} if possible because it is much more efficient!
     * Use this method only if your menu constructor is custom or the menu must be created by instance
     * (and not by class).</b><br><br>
     * Create a new button which opens a given menu instance.
     * @param item how the button should look like
     * @param to what menu the player should be sent to
     * @return the button
     */
    protected final Button getMenuButton(ItemStack item, AdvancedMenu to){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, AdvancedMenu menu, ClickType click) {
                to.display();
            }

            @Override
            public ItemStack getItem() {
                return item;
            }
        };
    }

    /**
     * Does the same as {@link #getMenuButton(ItemStack, Class)}.
     * Uses default item from {@link MenuUtil#defaultMenuItem}.
     */
    protected final Button getMenuButton(Class<? extends AdvancedMenu> to){
        return getMenuButton(MenuUtil.defaultMenuItem, to);
    }

    /**
     * Create a new button which opens a new menu instance created from a given class.
     * @param item how the button should look like
     * @param to what menu the player should be sent to
     * @return the button
     */
    protected final Button getMenuButton(ItemStack item, Class<? extends AdvancedMenu> to){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, AdvancedMenu menu, ClickType click) {
                newInstanceOf(player, to).display();
            }

            @Override
            public ItemStack getItem() {
                return item;
            }
        };
    }

    /**
     * Create a new instance of the menu from the given class.
     */
    private AdvancedMenu newInstanceOf(Player player, Class<? extends AdvancedMenu> menu){
        try{
            AdvancedMenu am = menu.getDeclaredConstructor(Player.class).newInstance(player);
            am.setParent(menu);
            return am;
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
        }
        throw new NullPointerException("Could not create a new instance of " + menu.getName() + " class. " +
                "Please create a constructor with only Player argument.");
    }

    /**
     * Make the button from the tool. It gives player one piece of this tool.<br>
     * This button gets its additional lore depending on if player has the tool
     * in the inventory from {@link #getAlreadyHaveLore()} and {@link #getClickToGetLore()}
     * so you can override them to set your custom items and messages.<br>
     * @param tool the tool we should give
     * @return the button
     */
    protected final Button getToolButton(Tool tool){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, AdvancedMenu menu, ClickType click) {
                if (tool.isConsumable()){
                    SoundUtil.Play.POP_HIGH(player);
                    player.getInventory().addItem(tool.getItem());
                }
                else{
                    if (!player.getInventory().containsAtLeast(tool.getItem(), 1)){
                        SoundUtil.Play.POP_HIGH(player);
                        player.getInventory().addItem(tool.getItem());
                    }
                    else{
                        SoundUtil.Play.POP_LOW(player);
                        player.getInventory().removeItem(tool.getItem());
                    }
                }
                refreshMenu();
            }

            @Override
            public ItemStack getItem() {
                List<String> lore;
                boolean isConsumable = tool.isConsumable();
                boolean hasTool = hasItem(player, tool);

                if (isConsumable){
                    lore = getClickToGetLore();
                }
                else{
                    lore = hasTool ? getAlreadyHaveLore() : getClickToGetLore();
                }

                return ItemCreator.of(tool.getItem()).lore(lore).glow(!isConsumable && hasTool).make();
            }
        };
    }

    /**
     * Checks if the player has the specified tool in the inventory.
     */
    private boolean hasItem(Player player, Tool tool){
        return player.getInventory().containsAtLeast(tool.getItem(), 1);
    }

    /**
     * Get the additional item lore of the tool if the player already has this tool in the inventory.
     */
    protected List<String> getAlreadyHaveLore(){
        return Arrays.asList("", "&cYou already have this item!");
    }

    /**
     * Get the additional item lore of the tool if player does not have this tool in the inventory yet.
     */
    protected List<String> getClickToGetLore(){
        return Arrays.asList("", "&2Click to get this item");
    }

    /**
     * Does the same as {@link #getInfoButton(ItemStack)}.
     * Uses the default button from {@link MenuUtil#defaultInfoItem}.
     */
    protected final Button getInfoButton(){
        return getInfoButton(MenuUtil.defaultInfoItem);
    }

    /**
     * Get the button that shows info about the menu.
     * By default, does nothing when clicked, but you can override it and add your behavior.
     * This button gets its info from {@link #getInfoName()} and {@link #getInfoLore()}.
     * So you can override them and set your custom name and lore.
     * @return the button
     */
    protected final Button getInfoButton(ItemStack item){
        return Button.makeDummy(ItemCreator.of(item).name(getInfoName())
                .lore(Arrays.asList(getInfoLore())).hideTags(true));
    }

    /**
     * Get the name of the button that shows info about the menu.<br>
     * Override it to set your own name.
     * @see #getInfoButton(ItemStack)
     */
    protected String getInfoName(){
        return "&f" + ItemUtil.bountifyCapitalized(getTitle()) + " Menu Information";
    }

    /**
     * Get the lore of the button that shows info about the menu.<br>
     * Override it to set your own lore (info).
     * @see #getInfoButton(ItemStack)
     */
    protected String[] getInfoLore() {
        return new String[]{
                "",
                "&7Override &fgetInfoName() &7and &fgetInfoLore()",
                "&7in " + getClass().getSimpleName() + " &7to set your own menu description."
        };
    }

    /**
     * For {@link AdvancedMenu}, set the slots that should NOT be filled with {@link #wrapperItem}.<br>
     * For {@link AdvancedMenuPagged}, set the slots the main elements can only be placed on.
     * Note that all unspecified slots are locked.
     */
    @SuppressWarnings("BoxingBoxedValue")
    protected final void setUnlockedSlots(Integer... slots){
        lockedSlots.clear();
        lockedSlots = IntStream.rangeClosed(0, 53).boxed().collect(Collectors.toList());
        for (Integer slot : slots){
            lockedSlots.remove(Integer.valueOf(slot));
        }
    }

    @Override
    protected void onMenuClick(Player player, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {
        if (getButtons().containsKey(slot)){
            getButtons().get(slot).onClickedInMenu(player, this, click);
        }
    }

    /**
     * Override this method and customize your menu here.
     * This method is automatically started just before displaying a menu to a player.
     */
    protected void setup(){}

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

    @Override
    public AdvancedMenu newInstance() {
        return this;
    }

    /**
     * Send a message to the {@link #getPlayer()}
     */
    public void tell(Object... messages) {
        Common.tell(this.player, Arrays.toString(messages));
    }

    /**
     * Send an information message to the {@link #getPlayer()}
     */
    public void tellInfo(Object message) {
        Messenger.info(this.player, message.toString());
    }

    /**
     * Send a success message to the {@link #getPlayer()}
     */
    public void tellSuccess(Object message) {
        Messenger.success(this.player, message.toString());
    }

    /**
     * Send a warning message to the {@link #getPlayer()}
     */
    public void tellWarn(Object message) {
        Messenger.warn(this.player, message.toString());
    }

    /**
     * Send an error message to the {@link #getPlayer()}
     */
    public void tellError(Object message) {
        Messenger.error(this.player, message.toString());
    }

    /**
     * Send a question message to the {@link #getPlayer()}
     */
    public void tellQuestion(Object message) {
        Messenger.question(this.player, message.toString());
    }

    /**
     * Send an announcement message to the {@link #getPlayer()}
     */
    public void tellAnnounce(Object message) {
        Messenger.announce(this.player, message.toString());
    }
}