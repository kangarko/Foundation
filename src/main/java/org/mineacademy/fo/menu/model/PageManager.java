package org.mineacademy.fo.menu.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mineacademy.fo.Common;

import lombok.Getter;

/**
 * A utility class for calculating menu items pages
 *
 * @param <T> the item that each page containts
 */
@Getter
public final class PageManager<T> {

	/**
	 * Pages by their order, containing list of items
	 */
	private final Map<Integer, List<T>> pages;

	/**
	 * The cell size, must be a division of 9
	 */
	private final int cellSize;

	/**
	 * Create a new page manager
	 *
	 * Use {@link #populate(int, Iterable)}
	 *
	 * @param cellSize the cell size
	 * @param allItems all items to be put into pages
	 */
	private PageManager(int cellSize, Iterable<T> allItems) {
		this.cellSize = cellSize;
		this.pages = fillPages(Common.toList(allItems));
	}

	/**
	 * Dynamically populates the pages
	 *
	 * @param allItems all items that will be split
	 * @return the map containing pages and their items
	 */
	private Map<Integer, List<T>> fillPages(List<T> allItems) {
		final Map<Integer, List<T>> pages = new HashMap<>();
		final int pageCount = allItems.size() == cellSize ? 0 : allItems.size() / cellSize;

		for (int i = 0; i <= pageCount; i++) {
			final List<T> pageItems = new ArrayList<>();

			final int down = cellSize * i;
			final int up = down + cellSize;

			for (int valueIndex = down; valueIndex < up; valueIndex++)
				if (valueIndex < allItems.size()) {
					final T page = allItems.get(valueIndex);

					pageItems.add(page);
				}

				else
					break;

			pages.put(i, pageItems);
		}

		return pages;
	}

	/**
	 * Automatically creates pages for items according to the specific cell size
	 *
	 * @param cellSize the cell size
	 * @param items the items
	 * @return the paged items
	 */
	public static <T> Map<Integer, List<T>> populate(int cellSize, Iterable<T> items) {
		return new PageManager<>(cellSize, items).getPages();
	}
}
