package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleLocation;
import org.mineacademy.fo.platform.BossBarTask.TimedBar;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

/**
 * An implementation of {@link FoundationPlayer} for Bukkit.
 */
@Getter
final class VelocityPlayer extends FoundationPlayer {

	private final boolean isPlayer;
	private final Player player;
	private final CommandSource sender;

	VelocityPlayer(@NonNull CommandSource sender) {
		this.sender = sender;
		this.isPlayer = sender instanceof Player;
		this.player = this.isPlayer ? (Player) sender : null;
	}

	@Override
	public void chat(String message) {
		this.player.spoofChatInput(message);
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.isPlayer ? this.player.getRemoteAddress() : null;
	}

	@Override
	public SimpleLocation getLocation() {
		throw new UnsupportedOperationException("Cannot get Bukkit location from a Velocity player");
	}

	@Override
	protected String getSenderName0() {
		return this.isPlayer ? this.player.getUsername() : "Console";
	}

	@Override
	public FoundationServer getServer() {
		if (this.isPlayer) {
			final Optional<ServerConnection> server = this.player.getCurrentServer();

			if (server.isPresent())
				return new VelocityServer(server.get().getServer());
		}

		return null;
	}

	@Override
	public UUID getUniqueId() {
		ValidCore.checkBoolean(this.isPlayer, "Cannot get UUID for a non-player" + this.getName());

		return this.player.getUniqueId();
	}

	@Override
	public boolean hasHexColorSupport() {
		return !this.isPlayer || this.player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_16);
	}

	@Override
	protected boolean hasPermission0(String permission) {
		return this.sender.hasPermission(permission);
	}

	@Override
	public void hideBossBar0(TimedBar bar) {
		this.sender.hideBossBar(bar.getBar());
	}

	@Override
	public boolean isCommandSender() {
		return true;
	}

	@Override
	public boolean isConsole() {
		return this.sender instanceof ConsoleCommandSource;
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
		return this.isPlayer && this.player.isActive();
	}

	@Override
	public void kick(SimpleComponent reason) {
		ValidCore.checkBoolean(this.isPlayer, "Cannot kick a non-player: " + this.sender);

		this.player.disconnect(reason.toAdventure(this));
	}

	@Override
	public void openBook(Book book) {
		throw new UnsupportedOperationException("Not supported on " + Platform.getType());
	}

	@Override
	protected void performPlayerCommand0(String replacedCommand) {
		VelocityPlugin.getServer().getCommandManager().executeImmediatelyAsync(this.sender, replacedCommand);
	}

	@Override
	public void resetTitle() {
		this.player.resetTitle();
	}

	@Override
	public void sendActionBar(SimpleComponent message) {
		this.sender.sendActionBar(message.toAdventure(this));
	}

	@Override
	public void sendPlayerListHeaderAndFooter(SimpleComponent header, SimpleComponent footer) {
		this.sender.sendPlayerListHeaderAndFooter(header.toAdventure(this), footer.toAdventure(this));
	}

	@Override
	public void sendMessage(Component component) {
		this.sender.sendMessage(component);
	}

	@Override
	public void sendToast(SimpleComponent message, CompToastStyle style) {
		this.sendMessage(message);
	}

	@Override
	public void setTempMetadata(String key, Object value) {
		throw new UnsupportedOperationException("Not supported on " + Platform.getType());
	}

	@Override
	public void showBossBar0(TimedBar bar) {
		this.sender.showBossBar(bar.getBar());
	}

	@Override
	public void showTitle(Title title) {
		this.sender.showTitle(title);
	}
}
