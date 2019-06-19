package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.fo.RandomUtil;

/**
 * A pretty specific class for picking up items for the player randomly,
 * used when giving a random class to the player in arena plugins
 * where each picked class is checked against players permission
 *
 * @param <T>
 */
public abstract class RandomNoRepeatPicker<T> {

	/**
	 * The list holding all items
	 */
	private final List<T> list = new ArrayList<>();

	/**
	 * Randomly picks 1 item from the given list
	 *
	 * @param items
	 * @return
	 */
	public T pickFrom(Iterable<T> items) {
		return pickFromFor(items, null);
	}

	/**
	 * Randomly picks 1 item for the player using {@link #canObtain(Player, Object)} method
	 *
	 * @param items
	 * @param player
	 * @return
	 */
	public T pickFromFor(Iterable<T> items, Player player) {
		for (final T item : items)
			list.add(item);

		return pickRandom(player);
	}

	/**
	 * Picks randomly 1 item and evaluates it against {@link #canObtain(Player, Object)}
	 * until we run out of items or find 1 that the player can acquire
	 *
	 * @param player
	 * @return
	 */
	private T pickRandom(Player player) {
		if (list.isEmpty())
			return null;

		T picked = null;

		while (!list.isEmpty() || picked == null) {
			picked = list.remove(RandomUtil.nextInt(list.size()));

			if (player == null)
				return picked;

			if (picked != null && canObtain(player, picked))
				return picked;
		}

		return picked;
	}

	/**
	 * Should return true if the player can obtain the given item
	 *
	 * @param player
	 * @param picked
	 * @return
	 */
	protected abstract boolean canObtain(Player player, T picked);
}