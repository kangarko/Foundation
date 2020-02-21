package org.mineacademy.fo.menu;

/**
 * An advanced menu listing items with automatic page support
 * <p>
 * Same as {@link MenuPagged} but async to
 * improve your servers performance
 *
 * @param <T> the item that each page consists of
 */

public final class AsyncMenuPagged<T> extends Menu {
	@Override protected String[] getInfo() {
		return new String[0];
	}
}
