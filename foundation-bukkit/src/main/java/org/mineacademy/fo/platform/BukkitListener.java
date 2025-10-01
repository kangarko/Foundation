package org.mineacademy.fo.platform;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.model.BuiltByBitUpdateCheck;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.visual.Visualizer;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;

/**
 * Listens for some events we handle for you automatically
 */
final class BukkitListener implements Listener {

	BukkitListener() {

		// Custom click events
		try {
			Class.forName("io.papermc.paper.event.player.PlayerCustomClickEvent");
			Platform.registerEvents(new CustomClickListener());

		} catch (final ClassNotFoundException ex) {
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();

		SimpleScoreboard.clearBoardsFor(player);
		Visualizer.stopVisualizing(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServiceRegister(final ServiceRegisterEvent event) {
		HookManager.updateVaultIntegration();
	}

	/**
	 * Delegates removing legacy metadata for dead entities.
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityDeath(final EntityDeathEvent event) {
		final Entity entity = event.getEntity();

		if (!(entity instanceof Player) && CompMetadata.isLegacy())
			CompMetadata.MetadataFile.getInstance().onEntityRemove(entity.getUniqueId());
	}

	/**
	 * Handler for {@link ChatPaginator}
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(final PlayerCommandPreprocessEvent event) {

		final Player player = event.getPlayer();
		final FoundationPlayer audience = Platform.toPlayer(player);
		final String message = event.getMessage();

		if (message.startsWith("/#flp")) {
			final String[] args = message.split(" ");

			if (args.length != 2) {
				audience.sendMessage(Lang.component("page-no-page-number"));

				event.setCancelled(true);
				return;
			}

			if (!player.hasMetadata(CompMetadata.TAG_PAGINATION)) {
				event.setCancelled(true);

				return;
			}

			final String numberRaw = args[1];
			int page = -1;

			try {
				page = Integer.parseInt(numberRaw) - 1;

			} catch (final NumberFormatException ex) {
				audience.sendMessage(Lang.component("page-invalid-page", "input", numberRaw));

				event.setCancelled(true);
				return;
			}

			final ChatPaginator chatPages = (ChatPaginator) player.getMetadata(CompMetadata.TAG_PAGINATION).get(0).value();
			final Map<Integer, List<SimpleComponent>> pages = chatPages.getPages();

			// Remove empty lines
			pages.entrySet().removeIf(entry -> entry.getValue().isEmpty());

			if (pages.isEmpty() || !pages.containsKey(page)) {
				Messenger.error(player, pages.isEmpty()
						? Lang.component("page-no-pages")
						: Lang.component("page-no-page"));

				event.setCancelled(true);

				return;
			}

			{ // Send the message body
				for (final SimpleComponent component : chatPages.getHeader())
					audience.sendMessage(component);

				final List<SimpleComponent> messagesOnPage = pages.get(page);
				int count = 1;

				for (final SimpleComponent component : messagesOnPage)
					audience.sendMessage(component.replaceBracket("count", String.valueOf(page + count++)));

				int whiteLines = chatPages.getLinesPerPage();

				if (pages.size() > 1) {
					if (whiteLines == 15 && pages.size() == 1)
						if (messagesOnPage.size() < 17)
							whiteLines = 7;
						else
							whiteLines += 2;

					for (int i = messagesOnPage.size(); i < whiteLines; i++)
						audience.sendMessage(SimpleComponent.fromPlain(" "));
				}

				for (final SimpleComponent component : chatPages.getFooter())
					audience.sendMessage(component);
			}

			// Fill in the pagination line
			if (pages.size() > 1) {
				player.sendMessage(" ");

				final int pagesDigits = (int) (Math.log10(pages.size()) + 1);
				final int multiply = 23 - (int) MathUtil.ceiling(pagesDigits);

				SimpleComponent component = SimpleComponent
						.fromMiniAmpersand("&8&m" + CommonCore.duplicate("-", multiply) + "&r");

				if (page == 0)
					component = component.appendMiniAmpersand(" &7« ");
				else
					component = component
							.appendMiniAmpersand(" &6« ")
							.onHover(Lang.component("page-go-to-page", "page", String.valueOf(page)))
							.onClickRunCmd("/#flp " + page);

				component = component
						.appendMiniAmpersand("&f" + (page + 1)).onHover(Lang.component("page-go-to-first-page")).onClickRunCmd("/#flp 1")
						.appendMiniAmpersand("&7/").onHover(Lang.component("page-tooltip"))
						.appendMiniAmpersand("&f" + pages.size() + "").onHover(Lang.component("page-go-to-last-page")).onClickRunCmd("/#flp " + pages.size());

				if (page + 1 >= pages.size())
					component = component.appendMiniAmpersand(" &7» ");
				else
					component = component
							.appendMiniAmpersand(" &6» ")
							.onHover(Lang.component("page-go-to-page", "page", String.valueOf(page + 2)))
							.onClickRunCmd("/#flp " + (page + 2));

				audience.sendMessage(component
						.appendMiniAmpersand("&8&m" + CommonCore.duplicate("-", multiply)));
			}

			// Prevent "Unknown command message"
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		final FoundationPlayer audience = Platform.toPlayer(player);

		// Delay to make visible
		Platform.runTask(2, () -> {
			if (player.hasPermission(Platform.getPlugin().getName().toLowerCase() + ".update.notify") && SimpleSettings.NOTIFY_NEW_VERSIONS && BuiltByBitUpdateCheck.isNewVersionAvailable())
				for (final SimpleComponent component : BuiltByBitUpdateCheck.getUpdateMessage())
					audience.sendMessage(component);
		});

		// Workaround for Essentials and CMI bug where they report "vanished" metadata when
		// the /vanish command is run, but forgot to do so after reload, despite player still
		// being vanished. So we just set the metadata on join back manually.
		//
		// Saves tons of performance when we check if a player is vanished.
		if (!player.hasMetadata("vanished")) {
			final boolean essVanished = HookManager.isVanishedEssentials(player);
			final boolean cmiVanished = HookManager.isVanishedCMI(player);
			final boolean advancedVanishVanished = HookManager.isVanishedAdvancedVanish(player);
			final boolean premiumVanishVanished = HookManager.isVanishedPremiumVanish(player);

			if (essVanished || cmiVanished || advancedVanishVanished || premiumVanishVanished) {
				final Plugin plugin = Bukkit.getPluginManager().getPlugin(essVanished ? "Essentials" : cmiVanished ? "CMI" : advancedVanishVanished ? "AdvancedVanish" : "PremiumVanish");

				player.setMetadata("vanished", new FixedMetadataValue(plugin, true));
			}
		}
	}
}

final class CustomClickListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCustomCommand(PlayerCustomClickEvent event) {
		if (!(event.getCommonConnection() instanceof PlayerGameConnection))
			return;

		final String key = event.getIdentifier().toString();
		final Player player = ((PlayerGameConnection) event.getCommonConnection()).getPlayer();

		if (key.equals("minecraft:fo_custom_command")) {
			final Map<String, String> map = CommonCore.GSON.fromJson(event.getTag().toString(), Map.class);
			final String command = map.get("command");

			player.chat(command);
		}
	}
}