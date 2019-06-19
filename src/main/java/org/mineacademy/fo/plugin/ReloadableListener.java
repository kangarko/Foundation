package org.mineacademy.fo.plugin;

import org.bukkit.event.Listener;

/**
 * A simple event listener with auto trigger method on reload
 */
public interface ReloadableListener extends Listener {

	/**
	 * Called automatically when the plugin is being reloaded
	 */
	void reload();
}
