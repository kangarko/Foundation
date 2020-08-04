package org.mineacademy.fo.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * A simple extension of the Bukkit event class to support additional functions,
 * currently:
 * <ul>
 *   <li>Make events work for both sync and async scenarious without errors</li>
 * </ul>
 */
public abstract class SimpleEvent extends Event {

	protected SimpleEvent() {
		// Since 1.14 Spigot has implemented a safety checks for events
		// and will throw errors when an event is fired async and not declared so
		//
		// This will automatically declare the event sync/async based off what thread it is fired from
		// see https://github.com/PaperMC/Paper/issues/2099
		super(!Bukkit.isPrimaryThread());
	}

	/**
	 * Create a new event indicating whether it is run from
	 * the primary Minecraft server thread or not
	 *
	 * @param async
	 */
	protected SimpleEvent(boolean async) {
		super(async);
	}
}
