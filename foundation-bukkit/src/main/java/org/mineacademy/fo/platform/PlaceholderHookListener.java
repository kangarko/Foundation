package org.mineacademy.fo.platform;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mineacademy.fo.model.HookManager;

import me.clip.placeholderapi.events.ExpansionRegisterEvent;

/**
 * A helper listener to reload PlaceholderAPI hooks when a new expansion is registered.
 */
final class PlaceholderHookListener implements Listener {

	@EventHandler
	public void onExpansionRegister(ExpansionRegisterEvent event) {
		HookManager.reloadPlaceholderAPIHooks();
	}
}
