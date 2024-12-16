package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.BossBarTask.TimedBar;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.SystemChat;

/**
 * An implementation of {@link FoundationPlayer} for Bukkit.
 */
@Getter
final class BungeePlayer extends FoundationPlayer {

	private final Audience audience;
	private final boolean isPlayer;
	private final ProxiedPlayer player;
	private final CommandSender sender;

	BungeePlayer(@NonNull final CommandSender sender) {
		this.sender = sender;
		this.isPlayer = sender instanceof ProxiedPlayer;
		this.player = this.isPlayer ? (ProxiedPlayer) sender : null;
		this.audience = BungeePlatform.getAdventure().sender(sender);
	}

	@Override
	public void chat(final String message) {
		final String json = SimpleComponent.fromPlain(message).toAdventureJson(this, !this.hasHexColorSupport());

		if (this.isPlayer) {
			if (this.player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_19)
				this.player.unsafe().sendPacket(new SystemChat(TextComponent.fromArray(ComponentSerializer.deserialize(json)), (byte) 0));
			else
				this.player.chat(message);
		} else
			this.sendMessage(SimpleComponent.fromPlain(message));
	}

	private int createBossBarColor(final net.kyori.adventure.bossbar.BossBar.Color color) {
		if (color == net.kyori.adventure.bossbar.BossBar.Color.PINK)
			return 0;
		else if (color == net.kyori.adventure.bossbar.BossBar.Color.BLUE)
			return 1;
		else if (color == net.kyori.adventure.bossbar.BossBar.Color.RED)
			return 2;
		else if (color == net.kyori.adventure.bossbar.BossBar.Color.GREEN)
			return 3;
		else if (color == net.kyori.adventure.bossbar.BossBar.Color.YELLOW)
			return 4;
		else if (color == net.kyori.adventure.bossbar.BossBar.Color.PURPLE)
			return 5;
		else if (color == net.kyori.adventure.bossbar.BossBar.Color.WHITE)
			return 6;

		return 5;
	}

	private byte createBossBarFlag(final Set<net.kyori.adventure.bossbar.BossBar.Flag> flags) {
		byte bit = 0;

		for (final net.kyori.adventure.bossbar.BossBar.Flag flag : flags) {
			if (flag == net.kyori.adventure.bossbar.BossBar.Flag.DARKEN_SCREEN)
				bit |= 1;
			else if (flag == net.kyori.adventure.bossbar.BossBar.Flag.PLAY_BOSS_MUSIC)
				bit |= 1 << 1;
			else if (flag == net.kyori.adventure.bossbar.BossBar.Flag.CREATE_WORLD_FOG)
				bit |= 1 << 2;
		}

		return bit;
	}

	private int createBossBarOverlay(final net.kyori.adventure.bossbar.BossBar.Overlay overlay) {
		if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS)
			return 0;
		else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_6)
			return 1;
		else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10)
			return 2;
		else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_12)
			return 3;
		else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_20)
			return 4;

		return 0;
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.isPlayer ? this.player.getAddress() : null;
	}

	@Override
	protected String getSenderName0() {
		return this.isPlayer ? this.player.getName() : "Console";
	}

	@Override
	public FoundationServer getServer() {
		return this.isPlayer ? new BungeeServer(this.player.getServer().getInfo()) : null;
	}

	@Override
	public UUID getUniqueId() {
		ValidCore.checkBoolean(this.isPlayer, "Cannot get UUID for a non-player" + this.getName());

		return this.player.getUniqueId();
	}

	@Override
	public boolean hasHexColorSupport() {
		return !this.isPlayer || this.player.getPendingConnection().getVersion() >= MINIMUM_PROTOCOL_FOR_HEX;
	}

	@Override
	protected boolean hasPermission0(final String permission) {
		return this.sender.hasPermission(permission);
	}

	@Override
	public void hideBossBar0(final TimedBar bar) {
		if (this.isPlayer && this.player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_9)
			this.player.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.BossBar(bar.getUniqueId(), 1 /* remove action */));
	}

	@Override
	public boolean isCommandSender() {
		return true;
	}

	@Override
	public boolean isConsole() {
		return this.sender.equals(BungeePlugin.getServer().getConsole());
	}

	@Override
	public boolean isDiscord() {
		return false;
	}

	@Override
	public boolean isPlayer() {
		return this.isPlayer;
	}

	@Override
	public boolean isPlayerOnline() {
		return this.isPlayer && this.player.isConnected();
	}

	@Override
	public void kick(final SimpleComponent reason) {
		ValidCore.checkBoolean(this.isPlayer, "Cannot kick a non-player: " + this.sender);

		this.player.disconnect(reason.toLegacySection(this));
	}

	@Override
	public void openBook(final Book book) {
		throw new UnsupportedOperationException("Not supported on " + Platform.getType());
	}

	@Override
	protected void performPlayerCommand0(final String replacedCommand) {
		ProxyServer.getInstance().getPluginManager().dispatchCommand(this.sender, replacedCommand);
	}

	@Override
	public void resetTitle() {
		if (this.isPlayer) {
			if (this.player.getPendingConnection().getVersion() > ProtocolConstants.MINECRAFT_1_8)
				this.audience.resetTitle();
			else
				this.showTitle("", ""); // fix adventure for some reason only resetting title but not subtitle
		}
	}

	@Override
	public void sendActionBar(final SimpleComponent message) {
		this.audience.sendActionBar(message.toAdventure(this));
	}

	@Override
	public void sendPlayerListHeaderAndFooter(final SimpleComponent header, final SimpleComponent footer) {
		this.audience.sendPlayerListHeaderAndFooter(header.toAdventure(this), footer.toAdventure(this));
	}

	@Override
	public void sendMessage0(final Component component) {
		// Due to adventure bug, players on modern MC are getting kicked out due to invalid
		// packet -- unless we serialize using md_5's method
		final String json = SimpleComponent.fromAdventure(component).toAdventureJson(this, !this.hasHexColorSupport());

		this.sender.sendMessage(ComponentSerializer.parse(json));
	}

	@Override
	public void sendToast(final SimpleComponent message, final CompToastStyle style) {
		this.sendMessage(message);
	}

	@Override
	public void setTempMetadata(final String key, final Object value) {
		throw new UnsupportedOperationException("Not supported on " + Platform.getType());
	}

	@Override
	public void showBossBar0(final TimedBar bar) {
		if (this.isPlayer) {
			if (this.player.getPendingConnection().getVersion() < ProtocolConstants.MINECRAFT_1_9) {
				// Not really supported unless we spawn a false ender dragon, a lot of hassle
				this.sendMessage(bar.getBar().name());

				return;
			}

			final net.md_5.bungee.protocol.packet.BossBar barPacket = new net.md_5.bungee.protocol.packet.BossBar(bar.getUniqueId(), 0 /* remove action */);

			barPacket.setTitle(new TextComponent(SimpleComponent.fromAdventure(bar.getBar().name()).toBungee(this, !this.hasHexColorSupport())));
			barPacket.setHealth(bar.getBar().progress());
			barPacket.setColor(this.createBossBarColor(bar.getBar().color()));
			barPacket.setDivision(this.createBossBarOverlay(bar.getBar().overlay()));
			barPacket.setFlags(this.createBossBarFlag(bar.getBar().flags()));

			this.player.unsafe().sendPacket(barPacket);
		}
	}

	@Override
	public void showTitle(final Title title) {
		this.audience.showTitle(title);
	}
}
