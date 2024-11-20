package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleLocation;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * An implementation of {@link FoundationPlayer} for Bukkit.
 */
@Getter
final class BungeePlayer extends FoundationPlayer {

	private final boolean isPlayer;
	private final ProxiedPlayer player;
	private final CommandSender sender;

	public BungeePlayer(@NonNull CommandSender sender) {
		this.sender = sender;
		this.isPlayer = sender instanceof ProxiedPlayer;
		this.player = this.isPlayer ? (ProxiedPlayer) sender : null;
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.isPlayer ? this.player.getAddress() : null;
	}

	@Override
	public SimpleLocation getBukkitLocation() {
		throw new UnsupportedOperationException("Cannot get Bukkit location from a Velocity player");
	}

	@Override
	public String getCurrentServerName() {
		return this.isPlayer ? this.player.getServer().getInfo().getName() : "";
	}

	@Override
	protected String getSenderName0() {
		return this.isPlayer ? this.player.getName() : "Console";
	}

	@Override
	public UUID getUniqueId() {
		Valid.checkBoolean(this.isPlayer, "Cannot get UUID for a non-player" + this.getName());

		return this.player.getUniqueId();
	}

	@Override
	public boolean hasHexColorSupport() {
		return !this.isPlayer || this.player.getPendingConnection().getVersion() >= MINIMUM_PROTOCOL_FOR_HEX;
	}

	@Override
	protected boolean hasPermission0(String permission) {
		return this.sender.hasPermission(permission);
	}

	@Override
	public boolean isCommandSender() {
		return true;
	}

	@Override
	public boolean isConsole() {
		return this.sender.equals(SimplePlugin.getServer().getConsole());
	}

	@Override
	public boolean isDiscord() {
		return false;
	}

	public boolean isOnline() {
		return this.isPlayer && this.player.isConnected();
	}

	@Override
	public boolean isPlayer() {
		return this.isPlayer;
	}

	@Override
	public void kick(SimpleComponent reason) {
		Valid.checkBoolean(this.isPlayer, "Cannot kick a non-player: " + this.sender);

		this.player.disconnect(reason.toLegacy());
	}

	@Override
	protected void performPlayerCommand0(String replacedCommand) {
		this.player.chat("/" + replacedCommand);
	}

	@Override
	public void removeBossBar() {
		throw new UnsupportedOperationException("Not supported in BungeeCord");
	}

	@Override
	public void resetTitle() {
		throw new UnsupportedOperationException("Not supported in BungeeCord");
	}

	@Override
	public void sendActionBar(SimpleComponent message) {
		if (this.isPlayer)
			this.player.sendMessage(ChatMessageType.ACTION_BAR, message.toBungee(!this.hasHexColorSupport()));
		else
			this.sender.sendMessage(message.toBungee(!this.hasHexColorSupport()));
	}

	@Override
	public void sendBossbarPercent(SimpleComponent message, float progress, Color color, Overlay overlay) {
		throw new UnsupportedOperationException("Not supported in BungeeCord");
	}

	@Override
	public void sendBossbarTimed(SimpleComponent message, int secondsToShow, float progress, Color color, Overlay overlay) {
		throw new UnsupportedOperationException("Not supported in BungeeCord");
	}

	@Override
	protected void sendLegacyMessage(String message) {
		this.sender.sendMessage(message);
	}

	@Override
	public void sendRawMessage(Component component) {
		this.sender.sendMessage(SimpleComponent.fromAdventure(component).toBungee(!this.hasHexColorSupport()));
	}

	@Override
	public void sendTablist(SimpleComponent header, SimpleComponent footer) {
		if (this.isPlayer)
			this.player.setTabHeader(header.toBungee(!this.hasHexColorSupport()), footer.toBungee(!this.hasHexColorSupport()));
	}

	@Override
	public void sendTitle(int fadeIn, int stay, int fadeOut, SimpleComponent title, SimpleComponent subtitle) {
		if (title == null)
			title = SimpleComponent.empty();

		if (subtitle == null)
			subtitle = SimpleComponent.empty();

		final ProxyServer server = ProxyServer.getInstance();

		if (this.isPlayer)
			server.createTitle().fadeIn(fadeIn).stay(stay).fadeOut(fadeOut).title(title.toBungee(!this.hasHexColorSupport())).subTitle(subtitle.toBungee(!this.hasHexColorSupport())).send(this.player);

		else {
			if (!title.isEmpty())
				this.sender.sendMessage(title.toBungee(!this.hasHexColorSupport()));

			if (!subtitle.isEmpty())
				this.sender.sendMessage(subtitle.toBungee(!this.hasHexColorSupport()));
		}
	}

	@Override
	public void sendToast(SimpleComponent message, CompToastStyle style) {
		this.sendMessage(message);
	}

	@Override
	public void setTempMetadata(String key, Object value) {
		throw new UnsupportedOperationException("Not supported in Velocity");
	}
}
