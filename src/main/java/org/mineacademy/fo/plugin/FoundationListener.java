package org.mineacademy.fo.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.constants.FoPermissions;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.update.SpigotUpdateCheck;

/**
 * Listens for some events we handle for you automatically
 */
public final class FoundationListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent e) {
		final SpigotUpdateCheck check = SimplePlugin.getInstance().getUpdateCheck();

		if (check != null && check.isNewerVersionAvailable() && PlayerUtil.hasPerm(e.getPlayer(), FoPermissions.Notify.UPDATE))
			Common.tellLater(4 * 20, e.getPlayer(), check.getUpdateMessage());
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
