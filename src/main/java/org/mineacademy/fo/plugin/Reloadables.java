package org.mineacademy.fo.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.event.SimpleListener;

/**
 * A simple way of registering events and other things that
 * are cancelled automatically when the plugin is reloaded.
 */
final class Reloadables {

	/**
	 * A list of currently enabled event listeners
	 */
	private final StrictList<Listener> listeners = new StrictList<>();

	/**
	 * A list of registered command groups
	 */
	private final StrictList<SimpleCommandGroup> commandGroups = new StrictList<>();

	// -------------------------------------------------------------------------------------------
	// Main
	// -------------------------------------------------------------------------------------------

	/**
	 * Remove all listeners and cancel all running tasks
	 */
	void reload() {
		for (final Listener listener : this.listeners)
			HandlerList.unregisterAll(listener);

		this.listeners.clear();

		for (final SimpleCommandGroup commandGroup : this.commandGroups)
			commandGroup.unregister();

		this.commandGroups.clear();
	}

	// -------------------------------------------------------------------------------------------
	// Events / Listeners
	// -------------------------------------------------------------------------------------------

	/**
	 * Register events to Bukkit
	 *
	 * @param listener
	 */
	void registerEvents(Listener listener) {
		Common.registerEvents(listener);

		this.listeners.add(listener);
	}

	/**
	 * Register events to Bukkit using our listener
	 *
	 * @param <T>
	 * @param listener
	 */
	<T extends Event> void registerEvents(SimpleListener<T> listener) {
		listener.register();

		this.listeners.add(listener);
	}

	// -------------------------------------------------------------------------------------------
	// Command groups
	// -------------------------------------------------------------------------------------------

	/**
	 * Register the given command group
	 *
	 * @param label
	 * @param aliases
	 * @param group
	 */
	void registerCommands(final SimpleCommandGroup group) {
		group.register();

		this.commandGroups.add(group);
	}
}
