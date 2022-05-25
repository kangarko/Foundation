package org.mineacademy.fo.menu;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.SoundUtil;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Menu with pages.<br>
 * Supports previous and next buttons.
 */
public abstract class AdvancedMenuPagged<T> extends AdvancedMenu {
    /**
     * Shows if the constructor of MenuPaginated was initialized at least once.
     */
    private final boolean isInitialized;

    /**
     * Slots and their raw {@link #elements}.
     */
    @Getter
    private final TreeMap<Integer, T> elementsSlots = new TreeMap<>();
    /**
     * Raw elements that should be converted to itemStacks.<br>
     * These can be enumerable objects (Tasks, EntityType, Sound, etc.)
     */
    private List<T> elements;
    /**
     * Items (Material, Name, Amount) ready to be displayed in the menu.
     */
    private List<ItemStack> elementsItems;
    /**
     * Position of cursor in {@link #elementsItems} map that is being displayed now.
     */
    private int cursorAt = 0;
    /**
     * Current page opened in the player's menu.
     */
    @Getter
    private int currentPage = 1;
    /**
     * Defines if we should fill all empty locked slots with {@link #getWrapperItem()}. True by default.
     */
    @Getter @Setter
    protected boolean fillWithWrapper = true;
    /**
     * Defines if the previous and next buttons are displayed even if the menu has only one page.
     * False by default.
     */
    @Getter @Setter
    private boolean isPrevNextButtonsEnabledNoPages = false;
    /**
     * The ItemStack that the previous button should have.
     * Default: Material: Spectral_arrow, Name: "&7Previous page"
     */
    @Getter @Setter
    private ItemStack previousButtonItem = ItemCreator.of(CompMaterial.SPECTRAL_ARROW, "&7Previous page").build().make();
    /**
     * The ItemStack that the next button should have.
     * Default: Material: Tipped_arrow, Name: "&7Next page"
     */
    @Getter @Setter
    private ItemStack nextButtonItem = ItemCreator.of(CompMaterial.TIPPED_ARROW, "&7Next page").build().make();

    public AdvancedMenuPagged(Player player){
        this(player, null);
    }

    public AdvancedMenuPagged(Player player, Class<? extends AdvancedMenu> parent){
        super(player, parent);
        init();
        isInitialized = true;
    }

    /**
     * Actions to be taken when opening or updating the menu.<br>
     * It automatically runs when the menu opens. But if you make some changes after calling super()
     * in your child class you must call init() manually in the constructor after all changes.
     */
    @Override
    protected void init(){
        resetCursorAt();
        setElements();
        setElementsItems();
        setElementsSlots();
        if (isInitialized) addPrevNextButtons();
    }

    /**
     * Add previous and next buttons.<br>
     * If no slots are specified, this method must be called only after {@link #setSize} method.<br>
     * Default slots are left bottom corner and right bottom corner for previous and next buttons correspondingly.<br>
     * By default, buttons are only displayed when there is more than one page
     * or {@link #isPrevNextButtonsEnabledNoPages()} is set to true.
     */
    protected void addPrevNextButtons(){
        addPrevNextButtons(getPreviousButtonSlot(), getNextButtonSlot());
    }

    /**
     * Add previous and next buttons with specified slots.<br>
     * By default, buttons are only displayed when there is more than one page
     * or {@link #isPrevNextButtonsEnabledNoPages()} is set to true.
     */
    protected void addPrevNextButtons(int prevSlot, int nextSlot){
        if (getMaxPage() > 1 || isPrevNextButtonsEnabledNoPages){
            addButton(prevSlot, formPreviousButton(getPreviousButtonItem()));
            addButton(nextSlot, formNextButton(getNextButtonItem()));
        }
    }

    protected int getPreviousButtonSlot(){
        return getSize() - 9;
    }

    protected int getNextButtonSlot(){
        return getSize() - 1;
    }

    /**
     * Set {@link #elements} to the {@link #getElements()}.
     */
    private void setElements(){
        this.elements = getElements();
    }

    /**
     * Set {@link #elementsItems} to the {@link #convertToItemStacks}.
     */
    private void setElementsItems(){
        this.elementsItems = convertToItemStacks(elements);
    }

    /**
     * Set the slots where the main elements only can be placed.
     * Meanwhile, it locks all unspecified slots.
     */
    @SuppressWarnings("BoxingBoxedValue")
    protected void setUnlockedSlots(Integer... slots){
        lockedSlots.clear();
        lockedSlots = IntStream.rangeClosed(0, 53).boxed().collect(Collectors.toList());
        for (Integer slot : slots){
            lockedSlots.remove(Integer.valueOf(slot));
        }
    }

    /**
     * Set the slots where the main elements should not be placed.
     */
    protected void setLockedSlots(Integer... slots){
        lockedSlots.clear();
        lockedSlots.addAll(Arrays.asList(slots));
    }

    /**
     * Set the slots where the main elements should not be placed.
     * Figures available: 9x6_bounds, 9x6_circle, 9x6_rows, 9x6_columns, 9x6_two_slots,
     * 9x6_six_slots, 9x3_one_slot, 9x3_bounds, 9x1_one_slot.
     */
    @SuppressWarnings("SameParameterValue")
    protected final void setLockedSlots(String figure){
        switch (figure) {
            case ("9x6_bounds"): setLockedSlots(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53); break;
            case ("9x6_circle"): setUnlockedSlots(12, 13, 14, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 39, 40, 41); break;
            case ("9x6_rows"): setLockedSlots(0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53); break;
            case ("9x6_columns"): setLockedSlots(0, 8, 9, 18, 27, 36, 45, 17, 26, 35, 44, 53); break;
            case ("9x6_two_slots"): setUnlockedSlots(22, 31); break;
            case ("9x6_six_slots"): setUnlockedSlots(21, 22, 23, 30, 31, 32); break;
            case ("9x3_one_slot"): setUnlockedSlots(13); break;
            case ("9x3_bounds"): setUnlockedSlots(10, 11, 12, 13, 14, 15, 16); break;
            case ("9x1_one_slot"): setUnlockedSlots(4); break;
            default: new ArrayList<>();
        }
    }

    /**
     * Create the previous button itemStack.
     */
    private Button formPreviousButton(ItemStack itemStack){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (currentPage <= 1) {
                    CompSound.VILLAGER_NO.play(getViewer());
                    return;
                }
                currentPage -= 1;
                resetCursorAt();
                redraw();
                SoundUtil.Play.CLICK_LOW(player);
            }

            @Override
            public ItemStack getItem() {
                return itemStack;
            }
        };
    }

    /**
     * Create the next button itemStack.
     */
    private Button formNextButton(ItemStack itemStack){
        return new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (currentPage >= getMaxPage()) {
                    CompSound.VILLAGER_NO.play(getViewer());
                    return;
                }
                currentPage += 1;
                resetCursorAt();
                redraw();
                SoundUtil.Play.CLICK_HIGH(player);
            }

            @Override
            public ItemStack getItem() {
                return itemStack;
            }
        };
    }

    /**
     * Get the amount of unlocked slots.
     */
    private int getAvailableSlotsSize(){
        return getSize() - lockedSlots.size();
    }

    /**
     * Get the number of pages that can be in the menu considering amount of elements and available slots.
     */
    protected final int getMaxPage(){
        float a = (float) elementsItems.size() / getAvailableSlotsSize();
        return (a % 2 == 0 ? (int)a : (int)a + 1);
    }

    /**
     * Set the slots for the elements.<br>
     * If the slot is already taken by a custom item or another button,
     * or it is locked then this slot will not be used.
     */
    private void setElementsSlots(){
        elementsSlots.clear();
        for (T element : elements){
            loopSlots(element);
        }
    }

    private void loopSlots(T element){
        for (int page = 1; page <= getMaxPage(); page++){
            for (int slot = 0; slot < getSize(); slot++){
                int finalSlot = (page - 1) * getSize() + slot;

                if (getItems().containsKey(slot)) continue;
                if (lockedSlots.contains(slot)) continue;
                if (elementsSlots.containsKey(finalSlot)) continue;

                elementsSlots.put(finalSlot, element);
                return;
            }
        }
    }

    /**
     * Resets the {@link #cursorAt} to the first slot of this page.
     */
    private void resetCursorAt() {
        this.cursorAt = (currentPage - 1) * getAvailableSlotsSize();
    }

    /**
     * Display items on their slots.
     * This method already has a good working implementation, so try not to override it.
     */
    @Override
    public ItemStack getItemAt(int slot){
        if (getButtons().containsKey(slot)){
            return getButtons().get(slot).getItem();
        }
        if (getItems().containsKey(slot)){
            return getItems().get(slot);
        }
        if (isFillWithWrapper() && lockedSlots.contains(slot)){
            return getWrapperItem();
        }
        else{
            if (cursorAt >= elementsItems.size()) return null;
            ItemStack item = elementsItems.get(cursorAt);
            cursorAt += 1;
            return item;
        }
    }

    @Override
    protected void onMenuClick(Player player, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {
        if (clicked.getType().isAir()) return;
        int pagedSlot = slot + (currentPage - 1) * getSize();
        boolean isElement = elementsSlots.containsKey(pagedSlot);
        if (isElement) onElementClick(player,  elementsSlots.get(pagedSlot), slot, click);

        if (getButtons().containsKey(slot)){
            getButtons().get(slot).onClickedInMenu(player, this, click);
        }
    }

    /**
     * Actions that should be executed when player clicks on list element.
     * It does not work on locked slots and custom slots.
     */
    protected void onElementClick(Player player, T object, int slot, ClickType clickType) {}

    /**
     * Convert the elements to itemStacks that should be displayed in the menu.
     */
    private List<ItemStack> convertToItemStacks(List<T> elements){
        return elements.stream().map(this::convertToItemStack).collect(Collectors.toList());
    }

    /**
     * Get elements that should be converted to itemStacks.
     */
    protected abstract List<T> getElements();

    /**
     * Convert each element to itemStack which should be displayed in the menu.
     */
    protected abstract ItemStack convertToItemStack(T element);
}
