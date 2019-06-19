package org.mineacademy.fo.menu;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.InventoryDrawer;

/**
 *  An incremental menu that list items.
 */
public abstract class MenuList extends MenuStandard {

	private final List<String> list;

	protected MenuList(String listName, Iterable<String> list) {
		this(null, listName, list);
	}

	protected MenuList(Menu parent, String listName, Iterable<String> list) {
		super(parent);

		this.list = Common.toList(list);

		setSize(18 + 9 * (this.list.size() / 9));
		setTitle(listName + " Menu");
	}

	@Override
	protected final List<Button> getButtonsToAutoRegister() {
		final List<Button> items = new ArrayList<>(getSize());

		for (int i = 0; i < list.size(); i++)
			items.add(getListButton(list.get(i), i));

		fillSpace(items, 2);
		return items;
	}

	private final void fillSpace(List<Button> items, int preserve) {
		for (int i = items.size(); i < getSize() - preserve; i++)
			items.add(Button.makeEmpty());
	}

	protected abstract Button getListButton(String listName, int listIndex);

	@Override
	protected final void onDraw(InventoryDrawer inv) {
		for (final Button item : getButtonsToAutoRegister())
			inv.pushItem(item.getItem());
	}

	@Override
	protected final String[] getInfo() {
		return null;
	}
}
