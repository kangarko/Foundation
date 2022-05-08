package org.mineacademy.fo.slider;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemFrameClockwiseSlider implements Slider<Map<Integer, ItemStack>> {

	/**
	 * The filler items
	 */
	private final ItemStack fillerItem;

	/**
	 * The selected item
	 */
	private final ItemStack highlightItem;

	/**
	 * The size of the frame for the animation (allowed values - 18, 27, 36, 45, 54), make sure the menu's size is big enough
	 */
	private int frameSize = 27;

	/*
	 * The current head in the slider.
	 */
	private int currentPointer = 0;

	/**
	 * Set the frame's size
	 *
	 * @param frameSize
	 * @return
	 */
	public ItemFrameClockwiseSlider frameSize(final int frameSize) {
		this.frameSize = frameSize;

		return this;
	}

	/**
	 * @see org.mineacademy.fo.slider.Slider#next()
	 */
	@Override
	public Map<Integer, ItemStack> next() {

		final Map<Integer, ItemStack> items = new HashMap<>();
		final int rowCount = this.frameSize / 9;

		for (int index = 0; index < this.frameSize; index++) {
			final int row = index / 9;
			final int column = (index % 9) + 1;

			if (row == 0 || row == rowCount - 1 || column == 1 || column == 9)
				items.put(index, this.fillerItem);
		}

		if (this.currentPointer < 8)
			this.currentPointer++;

		else if (this.currentPointer == 8)
			this.currentPointer = 17;

		else if (this.currentPointer <= this.frameSize - 1 && this.currentPointer > this.frameSize - 9)
			this.currentPointer--;

		else if (this.currentPointer == 9)
			this.currentPointer = 0;

		else if (rowCount >= 3)
			if (this.currentPointer == 17)
				this.currentPointer = 26;

			else if (this.currentPointer == 18)
				this.currentPointer = 9;

			else if (rowCount >= 4)
				if (this.currentPointer == 26)
					this.currentPointer = 35;

				else if (this.currentPointer == 27)
					this.currentPointer = 18;

				else if (rowCount >= 5)
					if (this.currentPointer == 35)
						this.currentPointer = 44;

					else if (this.currentPointer == 36)
						this.currentPointer = 27;

					else if (rowCount == 6)
						if (this.currentPointer == 44)
							this.currentPointer = 53;

						else if (this.currentPointer == 45)
							this.currentPointer = 36;

		items.replace(this.currentPointer, this.highlightItem);

		return items;
	}

	/**
	 * Create a new slider for the given items.
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemFrameClockwiseSlider from(final CompMaterial filler, final CompMaterial highlighted) {
		return from(ItemCreator.of(filler), ItemCreator.of(highlighted));
	}

	/**
	 * Create a new slider for the given items.
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemFrameClockwiseSlider from(final ItemCreator filler, final ItemCreator highlighted) {
		return from(filler.make(), highlighted.make());
	}

	/**
	 * Create a new slider for the given items.
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemFrameClockwiseSlider from(final ItemStack filler, final ItemStack highlighted) {
		return new ItemFrameClockwiseSlider(filler, highlighted);
	}
}
