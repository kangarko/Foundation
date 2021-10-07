package org.mineacademy.fo.slider;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * An example slider iterating through items and highlighting one.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemSlider implements Slider<List<ItemStack>> {

	/**
	 * The filler items
	 */
	private final ItemStack fillerItem;

	/**
	 * The selected item
	 */
	private final ItemStack highlightItem;

	/**
	 * The amount of items to surround the highlighted item around (left and right side).
	 */
	private int width = 1;

	/*
	 * The current head in the slider.
	 */
	private int currentPointer = 0;

	/**
	 * Set the amount of items to surround the highlighted item around (left and right side).
	 *
	 * @param width
	 * @return
	 */
	public ItemSlider width(int width) {
		this.width = width;

		return this;
	}

	/**
	 * @see org.mineacademy.fo.slider.Slider#next()
	 */
	@Override
	public List<ItemStack> next() {

		if (this.currentPointer == this.width)
			this.currentPointer = 0;

		final List<ItemStack> items = new ArrayList<>();

		for (int i = this.width - 1; i > this.width - this.currentPointer - 1; i--)
			items.add(this.fillerItem);

		items.add(this.highlightItem);

		for (int i = 0; i < this.width - this.currentPointer - 1; i++)
			items.add(this.fillerItem);

		this.currentPointer++;

		return items;
	}

	/**
	 * Create a new slider for the given items.
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemSlider from(CompMaterial filler, CompMaterial highlighted) {
		return from(ItemCreator.of(filler), ItemCreator.of(highlighted));
	}

	/**
	 * Create a new slider for the given items.
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemSlider from(ItemCreator.ItemCreatorBuilder filler, ItemCreator.ItemCreatorBuilder highlighted) {
		return from(filler.build().make(), highlighted.build().make());
	}

	/**
	 * Create a new slider for the given items.
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemSlider from(ItemStack filler, ItemStack highlighted) {
		return new ItemSlider(filler, highlighted);
	}
}