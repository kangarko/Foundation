package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleLocation;
import org.mineacademy.fo.platform.BossBarTask.TimedBar;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;

/**
 * An implementation of {@link FoundationPlayer} for Bukkit.
 */
@Getter
final class BukkitPlayer extends FoundationPlayer {

	private final boolean isPlayer;
	private final Player player;
	private final CommandSender sender;
	private final Audience audience;

	public BukkitPlayer(@NonNull CommandSender sender) {
		this.sender = sender;
		this.isPlayer = sender instanceof Player;
		this.player = this.isPlayer ? (Player) sender : null;
		this.audience = BukkitPlatform.hasAdventure() ? BukkitPlatform.getAdventure().sender(sender) : null;
	}

	@Override
	public void chat(String message) {
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
	public SimpleLocation getBukkitLocation() {
		ValidCore.checkBoolean(this.isPlayer, "Cannot get Bukkit location for a non-player" + this.getName());
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
		ValidCore.checkBoolean(this.isPlayer, "Cannot get UUID for a non-player" + this.getName());

		return this.player.getUniqueId();
	}

	@Override
	public boolean hasHexColorSupport() {
		return MinecraftVersion.atLeast(V.v1_16);
	}

	@Override
	protected boolean hasPermission0(String permission) {
		return this.sender.hasPermission(permission);
	}

	@Override
	protected void hideBossBar0(TimedBar bar) {
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
	public boolean isPlayer() {
		return this.isPlayer;
	}

	@Override
	public boolean isPlayerOnline() {
		return this.isPlayer && this.player.isOnline();
	}

	@Override
	public void kick(SimpleComponent reason) {
		ValidCore.checkBoolean(this.isPlayer, "Cannot kick a non-player: " + this.sender);

		if (Bukkit.isPrimaryThread())
			this.player.kickPlayer(reason.toLegacy());

		else
			Platform.runTask(() -> this.player.kickPlayer(reason.toLegacy()));
	}

	@Override
	public void openBook(Book book) {
		org.mineacademy.fo.model.Book.fromAdventure(book).open(this);
	}

	@Override
	protected void performPlayerCommand0(String replacedCommand) {
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
	public void sendActionBar(SimpleComponent message) {

		// Native is fastest
		if (Remain.isCommandSenderAudience()) {
			this.sender.sendActionBar(message);

			return;
		}

		if (!this.isPlayer || MinecraftVersion.olderThan(V.v1_8))
			this.sender.sendMessage(message.toLegacy());

		else
			try {
				this.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message.toBungee(!this.hasHexColorSupport()));

			} catch (final NoSuchMethodError err) {
				Remain.sendActionBarLegacyPacket(this.player, message);
			}
	}

	@Override
	public void sendPlayerListHeaderAndFooter(SimpleComponent header, SimpleComponent footer) {
		if (this.isPlayer && MinecraftVersion.atLeast(V.v1_8))
			try {
				this.player.setPlayerListHeaderFooter(header.toLegacy(), footer.toLegacy());

			} catch (final NoSuchMethodError ex) {
				Remain.sendTablistLegacyPacket(this.player, header, footer);
			}
	}

	@Override
	public void sendRawMessage(Component component) {

		// Paper is fastest: ~0.1ms vs ~0.3ms below
		if (Remain.isCommandSenderAudience()) {
			this.sender.sendMessage(component);

			return;
		}

		// Console does not send empty messages so we add a space
		if (!this.isPlayer) {
			final String legacy = SimpleComponent.fromAdventure(component).toLegacy();

			this.sender.sendMessage(legacy.isEmpty() ? " " : legacy);
			return;
		}

		this.player.spigot().sendMessage(SimpleComponent.fromAdventure(component).toBungee(!this.hasHexColorSupport()));
	}

	@Override
	public void sendToast(SimpleComponent message, CompToastStyle style) {
		if (this.isPlayer)
			Remain.sendToast(this.player, message.toLegacy(), style);
		else
			this.sendMessage(message);
	}

	@Override
	public void setTempMetadata(String key, Object value) {
		ValidCore.checkBoolean(this.isPlayer, "Cannot set temp metadata for non-players!");

		this.player.setMetadata(key, new FixedMetadataValue(BukkitPlugin.getInstance(), value));
	}

	@Override
	protected void showBossBar0(TimedBar bar) {
		if (this.isPlayer) {
			if (Remain.isCommandSenderAudience())
				this.sender.showBossBar(bar.getBar());
			else
				this.audience.showBossBar(bar.getBar());

		} else
			this.sendRawMessage(bar.getBar().name());
	}

	@Override
	public void showTitle(Title title) {
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
