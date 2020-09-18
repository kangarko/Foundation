package org.mineacademy.fo.plugin;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.constants.FoPermissions;
import org.mineacademy.fo.model.ChatPages;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.SpigotUpdater;

/**
 * Listens for some events we handle for you automatically
 */
final class FoundationListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent event) {
		final SpigotUpdater check = SimplePlugin.getInstance().getUpdateCheck();

		if (check != null && check.isNewVersionAvailable() && PlayerUtil.hasPerm(event.getPlayer(), FoPermissions.NOTIFY_UPDATE))
			Common.tellLater(4 * 20, event.getPlayer(), check.getNotifyMessage());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent event) {
		SimpleScoreboard.clearBoardsFor(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServiceRegister(ServiceRegisterEvent event) {
		HookManager.updateVaultIntegration();
	}

	/**
	 * Handler for {@link ChatPages}
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {

		final Player player = event.getPlayer();
		final String message = event.getMessage();
		final String[] args = message.split(" ");

		if (!message.startsWith("/#flp"))
			return;

		if (args.length != 2) {
			Common.tell(player, "&cPlease specify the page number for this command.");

			event.setCancelled(true);
			return;
		}

		if (!player.hasMetadata("FoPages")) {
			Common.tell(player, "&cYou do not have any pages saved to show.");

			event.setCancelled(true);
			return;
		}

		final String numberRaw = args[1];
		int page = -1;

		try {
			page = Integer.parseInt(numberRaw);

		} catch (final NumberFormatException ex) {
			Common.tell(player, "&cYour input '" + numberRaw + "' is not a valid number.");

			event.setCancelled(true);
			return;
		}

		final ChatPages chatPages = (ChatPages) player.getMetadata("FoPages").get(0).value();
		final Map<Integer, List<SimpleComponent>> pages = chatPages.getPages();

		if (!pages.containsKey(page)) {
			Common.tell(player, "Pages do not contain page number ");

			event.setCancelled(true);
			return;
		}

		{ // Send the message body
			for (final SimpleComponent component : chatPages.getHeader())
				component.send(player);

			final List<SimpleComponent> messagesOnPage = pages.get(page);

			for (final SimpleComponent comp : messagesOnPage)
				comp.send(player);

			for (int i = messagesOnPage.size(); i < chatPages.getLinesPerPage(); i++)
				SimpleComponent.of("&r").send(player);

			for (final SimpleComponent component : chatPages.getFooter())
				component.send(player);
		}

		Common.tell(player, " ");

		{ // Fill in the pagination line
			final SimpleComponent pagination = SimpleComponent.of("&7Page " + (page + 1) + "/" + pages.size() + ". Visit the ");

			if (page == 0)
				pagination.append("&7previous page");
			else
				pagination.append("&6&nprevious page&7").onHover("&7Go to page " + page).onClickRunCmd("/#flp " + (page - 1));

			pagination.append(" or the ");

			if (page + 1 >= pages.size())
				pagination.append("next one");
			else
				pagination.append("&6&nnext one&7").onHover("&7Go to page " + (page + 1)).onClickRunCmd("/#flp " + (page + 1));

			pagination.append(".");

			pagination.send(player);
		}

		// Prevent "Unknown command message"
		event.setCancelled(true);
	}
}
