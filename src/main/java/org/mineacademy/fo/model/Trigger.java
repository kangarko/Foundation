package org.mineacademy.fo.model;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictSet;

/**
 * A simple class that returns true for certain integer values
 * working as a trigger for minigame plugins when we need to
 * refill the chests at certain phases
 */
public final class Trigger {

	/**
	 * Mode one: Trigger when match numbers here
	 */
	private final StrictSet<Integer> triggers;

	/**
	 * Mode two: Trigger each time on the trigger
	 */
	private final Integer staticTrigger;

	/**
	 * Changing: the increasing count when you call {@link #trigger(int)}
	 */
	private int count = 0;

	/**
	 * Create a new trigger for the given points
	 *
	 * @param triggers
	 */
	public Trigger(StrictSet<Integer> triggers) {
		this(null, triggers);
	}

	/**
	 * Create a new trigger for the given 1 point
	 *
	 * @param trigger
	 */
	public Trigger(Integer trigger) {
		this(trigger, null);
	}

	private Trigger(Integer trigger, StrictSet<Integer> triggers) {
		this.staticTrigger = trigger;
		this.triggers = triggers;

		Valid.checkBoolean(!(staticTrigger == null && triggers == null), "Specify either a static trigger or a trigger list!");
	}

	/**
	 * Return true whether the given number is within the trigger list
	 * or equals the trigger number
	 *
	 * @param number
	 * @return
	 */
	public boolean trigger(int number) {
		if (staticTrigger != null) {
			if (++count >= staticTrigger) {
				count = 0;

				return true;
			}

			return false;
		}

		return triggers.contains(number);
	}
}
