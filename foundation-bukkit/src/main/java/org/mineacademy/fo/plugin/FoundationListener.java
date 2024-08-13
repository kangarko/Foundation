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
import org.mineacademy.fo.MathUtilCore;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponentCore;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.SimpleLocalization;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

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
		final Audience audience = Platform.toAudience(player);
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
		final Map<Integer, List<SimpleComponentCore>> pages = chatPages.getPages();

		// Remove empty lines
		pages.entrySet().removeIf(entry -> entry.getValue().isEmpty());

		if (pages.isEmpty() || !pages.containsKey(page)) {
			final String playerMessage = pages.isEmpty() ? SimpleLocalization.Pages.NO_PAGES : SimpleLocalization.Pages.NO_PAGE;

			if (Messenger.ENABLED)
				Messenger.error(player, playerMessage);
			else
				Common.tell(player, playerMessage);

			event.setCancelled(true);
			return;
		}

		{ // Send the message body
			for (final SimpleComponentCore component : chatPages.getHeader())
				component.send(audience);

			final List<SimpleComponentCore> messagesOnPage = pages.get(page);
			int count = 1;

			for (final SimpleComponentCore comp : messagesOnPage)
				comp.replace("{count}", String.valueOf(page + count++)).send(audience);

			int whiteLines = chatPages.getLinesPerPage();

			if (whiteLines == 15 && pages.size() == 1)
				if (messagesOnPage.size() < 17)
					whiteLines = 7;
				else
					whiteLines += 2;

			for (int i = messagesOnPage.size(); i < whiteLines; i++)
				SimpleComponentCore.of("&r").send(audience);

			for (final SimpleComponentCore component : chatPages.getFooter())
				component.send(audience);
		}

		// Fill in the pagination line
		if (MinecraftVersion.atLeast(V.v1_7) && pages.size() > 1) {
			Common.tellNoPrefix(audience, Component.text(" "));

			final int pagesDigits = (int) (Math.log10(pages.size()) + 1);
			final int multiply = 23 - (int) MathUtilCore.ceiling(pagesDigits);

			final SimpleComponentCore pagination = SimpleComponentCore.of(chatPages.getThemeColor() + "&m" + Common.duplicate("-", multiply) + "&r");

			if (page == 0)
				pagination.append(" &7« ");
			else
				pagination.append(" &6« ").onHover(SimpleLocalization.Pages.GO_TO_PAGE.replace("{page}", String.valueOf(page))).onClickRunCmd("/#flp " + page);

			pagination.append("&f" + (page + 1)).onHover(SimpleLocalization.Pages.GO_TO_FIRST_PAGE).onClickRunCmd("/#flp 1");
			pagination.append("/").onHover(SimpleLocalization.Pages.TOOLTIP);
			pagination.append(pages.size() + "").onHover(SimpleLocalization.Pages.GO_TO_LAST_PAGE).onClickRunCmd("/#flp " + pages.size());

			if (page + 1 >= pages.size())
				pagination.append(" &7» ");
			else
				pagination.append(" &6» ").onHover(SimpleLocalization.Pages.GO_TO_PAGE.replace("{page}", String.valueOf(page + 2))).onClickRunCmd("/#flp " + (page + 2));

			pagination.append(chatPages.getThemeColor() + "&m" + Common.duplicate("-", multiply));

			pagination.send(audience);
		}

		// Prevent "Unknown command message"
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		// Workaround for Essentials and CMI bug where they report "vanished" metadata when
		// the /vanish command is run, but forgot to do so after reload, despite player still
		// being vanished. So we just set the metadata on join back manually.
		//
		// Saves tons of performance when we check if a player is vanished.
		if (!player.hasMetadata("vanished")) {
			final boolean essVanished = HookManager.isVanishedEssentials(player);
			final boolean cmiVanished = HookManager.isVanishedCMI(player);
			final boolean advVanished = HookManager.isVanishedAdvancedVanish(player);
			final boolean premiumVanishVanished = HookManager.isVanishedPremiumVanish(player);

			if (essVanished || cmiVanished || advVanished || premiumVanishVanished) {
				final Plugin plugin = Bukkit.getPluginManager().getPlugin(essVanished ? "Essentials" : cmiVanished ? "CMI" : advVanished ? "AdvancedVanish" : "PremiumVanish");

				player.setMetadata("vanished", new FixedMetadataValue(plugin, true));
			}
		}
	}
}
