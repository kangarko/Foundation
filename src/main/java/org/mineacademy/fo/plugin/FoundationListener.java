package org.mineacademy.fo.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.update.SpigotUpdater;

/**
 * Listens for some events we handle for you automatically
 */
final class FoundationListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent e) {
		final SpigotUpdater check = SimplePlugin.getInstance().getUpdateCheck();

		if (check != null && check.isNewVersionAvailable() && PlayerUtil.hasPerm(e.getPlayer(), FoConstants.Permissions.NOTIFY_UPDATE))
			Common.tellLater(4 * 20, e.getPlayer(), check.getNotifyMessage());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent e) {
		SimpleScoreboard.clearBoardsFor(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServiceRegister(ServiceRegisterEvent e) {
		HookManager.updateVaultIntegration();
	}
}
