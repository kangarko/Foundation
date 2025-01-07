package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.DynmapSender;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleLocation;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.BossBarTask.TimedBar;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;

/**
 * An implementation of {@link FoundationPlayer} for Bukkit.
 */
@Getter
final class BukkitPlayer extends FoundationPlayer {

	private final Audience audience;
	private final boolean isPlayer;
	private final Player player;
	private final CommandSender sender;

	public BukkitPlayer(@NonNull final CommandSender sender) {
		this.sender = sender;
		this.isPlayer = sender instanceof Player;
		this.player = this.isPlayer ? (Player) sender : null;
		this.audience = BukkitPlatform.hasAdventure() ? BukkitPlatform.getAdventure().sender(sender) : null;
	}

	@Override
	public void chat(final String message) {
		if (this.isPlayer)
			this.player.chat(message);
		else
			this.sender.sendMessage(message);
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.isPlayer ? this.player.getAddress() : null;
	}

	@Override
	public SimpleLocation getLocation() {
		ValidCore.checkBoolean(this.isPlayer, "Cannot get location for a non-player: " + this.sender);
		final Location location = this.player.getLocation();

		return new SimpleLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	@Override
	protected String getSenderName0() {
		return this.sender.getName();
	}

	@Override
	public FoundationServer getServer() {
		return BukkitServer.getInstance();
	}

	@Override
	public UUID getUniqueId() {
		if (this.isPlayer)
			return this.player.getUniqueId();

		else if (this.isConsole())
			return CommonCore.ZERO_UUID;

		else if (this.isDiscord())
			return ((DiscordSender) this.sender).getUniqueId();

		else if (this.isDynmap())
			return ((DynmapSender) this.sender).getUniqueId();

		throw new UnsupportedOperationException("Getting UUID of " + this.sender.getClass().getSimpleName() + " " + this.sender + " is unsupported");
	}

	@Override
	public boolean hasHexColorSupport() {
		return MinecraftVersion.atLeast(V.v1_16);
	}

	@Override
	protected boolean hasPermission0(final String permission) {
		return this.sender.hasPermission(permission);
	}

	@Override
	protected void hideBossBar0(final TimedBar bar) {
		if (this.isPlayer) {
			if (Remain.isCommandSenderAudience())
				this.sender.hideBossBar(bar.getBar());

			else
				this.audience.hideBossBar(bar.getBar());
		}
	}

	@Override
	public boolean isCommandSender() {
		return true;
	}

	@Override
	public boolean isConsole() {
		return this.sender instanceof ConsoleCommandSender;
	}

	@Override
	public boolean isDiscord() {
		return this.sender instanceof DiscordSender;
	}

	@Override
	public boolean isDynmap() {
		return this.sender instanceof DynmapSender;
	}

	@Override
	public boolean isPlayer() {
		return this.isPlayer;
	}

	@Override
	public boolean isPlayerOnline() {
		return this.isPlayer && this.player.isOnline();
	}

	@Override
	public void kick(final SimpleComponent reason) {
		ValidCore.checkBoolean(this.isPlayer, "Cannot kick a non-player: " + this.sender);

		if (Bukkit.isPrimaryThread())
			this.kick0(reason);

		else
			Platform.runTask(() -> this.kick0(reason));
	}

	private void kick0(final SimpleComponent reason) {
		if (Remain.isCommandSenderAudience())
			this.player.kick(reason.toAdventure(this));
		else
			this.player.kickPlayer(reason.toLegacySection(this));
	}

	@Override
	public void openBook(final Book book) {

		// Render as text, replacing variables
		if (MinecraftVersion.olderThan(V.v1_8) || !this.isPlayer) {
			final List<SimpleComponent> pages = new ArrayList<>();
			int pageNumber = 1;
			final Variables variables = Variables.builder(this);

			for (final Component component : book.pages()) {
				final String legacyPage = LegacyComponentSerializer.legacySection().serialize(component);

				pages.add(Lang.component("command-book-page", "page", pageNumber++));

				for (final String line : legacyPage.split("\n"))
					pages.add(SimpleComponent.fromMiniAmpersand(" &7- &r" + variables.replaceLegacy(line)));

				pages.add(SimpleComponent.empty());
			}

			new ChatPaginator()
					.setFoundationHeader(Lang.legacy("command-book-page-header",
							"title", CommonCore.getOrDefault(book.title(), Lang.component("command-book-unnamed")),
							"author", CommonCore.getOrDefault(book.author(), Lang.component("command-book-unsigned"))))
					.setPages(pages)
					.send(this);

			return;
		}

		Remain.openBook(this, ItemCreator.fromBookAdventure(book, false).make());
	}

	@Override
	protected void performPlayerCommand0(final String replacedCommand) {
		if (this.isPlayer) {
			if (Bukkit.isPrimaryThread())
				this.player.chat("/" + replacedCommand);
			else
				Bukkit.getScheduler().runTask(BukkitPlugin.getInstance(), () -> this.player.chat("/" + replacedCommand));

		} else
			Bukkit.getScheduler().runTask(BukkitPlugin.getInstance(), () -> Bukkit.dispatchCommand(this.sender, replacedCommand));
	}

	@Override
	public void resetTitle() {
		if (this.isPlayer && MinecraftVersion.atLeast(V.v1_8)) {
			if (MinecraftVersion.atLeast(V.v1_8))
				this.showTitle("", "");
			else
				try {
					this.player.resetTitle();

				} catch (final NoSuchMethodError ex) {
					Remain.resetTitleLegacy(this.player);
				}
		}
	}

	@Override
	public void sendActionBar(final SimpleComponent component) {
		if (Remain.isCommandSenderAudience()) {
			this.sender.sendActionBar(component.toAdventure(this));

			return;
		}

		if (!this.isPlayer || MinecraftVersion.olderThan(V.v1_8))
			this.sender.sendMessage(component.toLegacySection(this));

		else
			try {
				this.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, component.toBungee(this, !this.hasHexColorSupport()));

			} catch (final NoSuchMethodError err) {
				Remain.sendActionBarLegacyPacket(this.player, component.toLegacySection(this));
			}
	}

	@Override
	public void sendPlayerListHeaderAndFooter(final SimpleComponent header, final SimpleComponent footer) {
		if (Remain.isCommandSenderAudience())
			this.audience.sendPlayerListHeaderAndFooter(header.toAdventure(this), footer.toAdventure(this));

		else if (this.isPlayer && MinecraftVersion.atLeast(V.v1_8))
			try {
				this.player.setPlayerListHeaderFooter(header.toLegacySection(this), footer.toLegacySection(this));

			} catch (final NoSuchMethodError ex) {
				Remain.sendTablistLegacyPacket(this.player, header.toLegacySection(this), footer.toLegacySection(this));
			}
	}

	@Override
	public void sendMessage0(final Component component) {

		// Paper is fastest: ~0.1ms vs ~0.3ms below
		if (Remain.isCommandSenderAudience()) {
			this.sender.sendMessage(component);

			return;
		}

		// Console does not send empty messages so we add a space
		if (!this.isPlayer) {
			final String legacy = LegacyComponentSerializer.legacySection().serialize(component);

			this.sender.sendMessage(legacy.isEmpty() ? " " : legacy);
			return;
		}

		//this.audience.sendMessage(component);
		this.player.spigot().sendMessage((!this.hasHexColorSupport() ? BungeeComponentSerializer.legacy() : BungeeComponentSerializer.get()).serialize(component));
	}

	@Override
	public void sendToast(final SimpleComponent component, final CompToastStyle style) {
		if (this.isPlayer)
			Remain.sendToast(this.player, component.toLegacySection(this), style);
		else
			this.sendMessage(component);
	}

	@Override
	public void setTempMetadata(final String key, final Object value) {
		ValidCore.checkBoolean(this.isPlayer, "Cannot set temp metadata for non-players!");

		this.player.setMetadata(key, new FixedMetadataValue(BukkitPlugin.getInstance(), value));
	}

	@Override
	protected void showBossBar0(final TimedBar bar) {
		if (this.isPlayer) {
			if (Remain.isCommandSenderAudience())
				this.sender.showBossBar(bar.getBar());
			else
				this.audience.showBossBar(bar.getBar());

		} else
			this.sendMessage(bar.getBar().name());
	}

	@Override
	public void showTitle(final Title title) {
		if (Remain.isCommandSenderAudience()) {
			this.sender.showTitle(title);

			return;
		}

		if (!this.isPlayer || MinecraftVersion.olderThan(V.v1_8)) {
			this.sendMessage(title.title());
			this.sendMessage(title.subtitle());

		} else
			this.audience.showTitle(title);
	}
}
