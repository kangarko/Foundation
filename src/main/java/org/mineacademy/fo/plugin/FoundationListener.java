package org.mineacademy.fo.plugin;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.SpigotUpdater;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * Listens for some events we handle for you automatically
 */
final class FoundationListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent event) {
		SimpleScoreboard.clearBoardsFor(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServiceRegister(ServiceRegisterEvent event) {
		HookManager.updateVaultIntegration();
	}

	/**
	 * Handler for {@link ChatPaginator}
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {

		final Player player = event.getPlayer();
		final String message = event.getMessage();

		if (!message.startsWith("/#flp"))
			return;

		final String[] args = message.split(" ");

		if (args.length != 2) {
			Common.tell(player, SimpleLocalization.Pages.NO_PAGE_NUMBER);

			event.setCancelled(true);
			return;
		}

		final String nbtPageTag = ChatPaginator.getPageNbtTag();

		if (!player.hasMetadata(nbtPageTag)) {
			event.setCancelled(true);

			return;
		}

		// Prevent shading issue with multiple plugins having Foundation shaded
		if (player.hasMetadata("FoPages") && !player.getMetadata("FoPages").get(0).asString().equals(SimplePlugin.getNamed()))
			return;

		final String numberRaw = args[1];
		int page = -1;

		try {
			page = Integer.parseInt(numberRaw) - 1;

		} catch (final NumberFormatException ex) {
			Common.tell(player, SimpleLocalization.Pages.INVALID_PAGE.replace("{input}", numberRaw));

			event.setCancelled(true);
			return;
		}

		final ChatPaginator chatPages = (ChatPaginator) player.getMetadata(nbtPageTag).get(0).value();
		final Map<Integer, List<SimpleComponent>> pages = chatPages.getPages();

		// Remove empty lines
		pages.entrySet().removeIf(entry -> entry.getValue().isEmpty());

		if (!pages.containsKey(page)) {
			final String playerMessage = SimpleLocalization.Pages.NO_PAGE;

			if (Messenger.ENABLED)
				Messenger.error(player, playerMessage);
			else
				Common.tell(player, playerMessage);

			event.setCancelled(true);
			return;
		}

		{ // Send the message body
			for (final SimpleComponent component : chatPages.getHeader())
				component.send(player);

			final List<SimpleComponent> messagesOnPage = pages.get(page);
			int count = 1;

			for (final SimpleComponent comp : messagesOnPage)
				comp.replace("{count}", page + count++).send(player);

			int whiteLines = chatPages.getLinesPerPage();

			if (whiteLines == 15 && pages.size() == 1) {
				if (messagesOnPage.size() < 17)
					whiteLines = 7;
				else
					whiteLines += 2;
			}

			for (int i = messagesOnPage.size(); i < whiteLines; i++)
				SimpleComponent.of("&r").send(player);

			for (final SimpleComponent component : chatPages.getFooter())
				component.send(player);
		}

		// Fill in the pagination line
		if (MinecraftVersion.atLeast(V.v1_7) && pages.size() > 1) {
			Common.tellNoPrefix(player, " ");

			final int pagesDigits = (int) (Math.log10(pages.size()) + 1);
			final int multiply = 23 - (int) MathUtil.ceiling(pagesDigits);

			final SimpleComponent pagination = SimpleComponent.of(chatPages.getThemeColor() + "&m" + Common.duplicate("-", multiply) + "&r");

			if (page == 0)
				pagination.append(" &7« ");
			else
				pagination.append(" &6« ").onHover(SimpleLocalization.Pages.GO_TO_PAGE.replace("{page}", String.valueOf(page))).onClickRunCmd("/#flp " + page);

			pagination.append("&f" + (page + 1)).onHover(SimpleLocalization.Pages.GO_TO_FIRST_PAGE).onClickRunCmd("/#flp 1");
			pagination.append("/");
			pagination.append(pages.size() + "").onHover(SimpleLocalization.Pages.TOOLTIP);

			if (page + 1 >= pages.size())
				pagination.append(" &7» ");
			else
				pagination.append(" &6» ").onHover(SimpleLocalization.Pages.GO_TO_PAGE.replace("{page}", String.valueOf(page + 2))).onClickRunCmd("/#flp " + (page + 2));

			pagination.append(chatPages.getThemeColor() + "&m" + Common.duplicate("-", multiply));

			pagination.send(player);
		}

		// Prevent "Unknown command message"
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final SpigotUpdater check = SimplePlugin.getInstance().getUpdateCheck();

		if (check != null && check.isNewVersionAvailable() && PlayerUtil.hasPerm(player, check.getPermission().replace("{plugin_name}", SimplePlugin.getNamed().toLowerCase().replace(" ", "_"))))
			Common.tellLater(4 * 20, player, check.getNotifyMessage());

		// Workaround for Essentials and CMI bug where they report "vanished" metadata when
		// the /vanish command is run, but forgot to do so after reload, despite player still
		// being vanished. So we just set the metadata on join back manually.
		//
		// Saves tons of performance when we check if a player is vanished.
		if (!player.hasMetadata("vanished")) {
			final boolean essVanished = HookManager.isVanishedEssentials(player);
			final boolean cmiVanished = HookManager.isVanishedCMI(player);

			if (essVanished || cmiVanished) {
				final Plugin plugin = Bukkit.getPluginManager().getPlugin(essVanished ? "Essentials" : "CMI");

				player.setMetadata("vanished", new FixedMetadataValue(plugin, true));
			}
		}
	}
}
