package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleLocation;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;

/**
 * An implementation of {@link FoundationPlayer} for Bukkit.
 */
@Getter
final class VelocityPlayer extends FoundationPlayer {

	private final boolean isPlayer;
	private final Player player;
	private final CommandSource sender;
	private final List<BossBar> viewedBossBars = new ArrayList<>();

	public VelocityPlayer(@NonNull CommandSource sender) {
		this.sender = sender;
		this.isPlayer = sender instanceof Player;
		this.player = this.isPlayer ? (Player) sender : null;
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.isPlayer ? this.player.getRemoteAddress() : null;
	}

	@Override
	public SimpleLocation getBukkitLocation() {
		throw new UnsupportedOperationException("Cannot get Bukkit location from a Velocity player");
	}

	@Override
	public String getCurrentServerName() {
		if (this.isPlayer) {
			final Optional<ServerConnection> server = this.player.getCurrentServer();

			if (server.isPresent())
				return server.get().getServerInfo().getName();
		}

		return "";
	}

	@Override
	protected String getSenderName0() {
		return this.isPlayer ? this.player.getUsername() : "Console";
	}

	@Override
	public UUID getUniqueId() {
		Valid.checkBoolean(this.isPlayer, "Cannot get UUID for a non-player" + this.getName());

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

	public boolean isOnline() {
		return this.isPlayer && this.player.isActive();
	}

	@Override
	public boolean isPlayer() {
		return this.isPlayer;
	}

	@Override
	public void kick(SimpleComponent reason) {
		Valid.checkBoolean(this.isPlayer, "Cannot kick a non-player: " + this.sender);

		this.player.disconnect(reason.toAdventure());
	}

	@Override
	protected void performPlayerCommand0(String replacedCommand) {
		this.player.spoofChatInput("/" + replacedCommand);
	}

	@Override
	public void removeBossBar() {
		for (final BossBar bar : this.viewedBossBars)
			this.sender.hideBossBar(bar);
	}

	@Override
	public void resetTitle() {
		this.player.resetTitle();
	}

	@Override
	public void sendActionBar(SimpleComponent message) {
		this.sender.sendActionBar(message);
	}

	@Override
	public void sendBossbarPercent(SimpleComponent message, float progress, Color color, Overlay overlay) {
		final BossBar bar = BossBar.bossBar(message, progress, color, overlay);

		this.viewedBossBars.add(bar);
		this.sender.showBossBar(bar);
	}

	@Override
	public void sendBossbarTimed(SimpleComponent message, int secondsToShow, float progress, Color color, Overlay overlay) {
		final BossBar bar = BossBar.bossBar(message, progress, color, overlay);

		this.sender.showBossBar(bar);
		Platform.runTask(secondsToShow * 20, () -> this.sender.hideBossBar(bar));
	}

	@Override
	protected void sendLegacyMessage(String message) {
		this.sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
	}

	@Override
	public void sendRawMessage(Component component) {
		this.sender.sendMessage(component);
	}

	@Override
	public void sendTablist(SimpleComponent header, SimpleComponent footer) {
		this.sender.sendPlayerListHeaderAndFooter(header, footer);
	}

	@Override
	public void sendTitle(int fadeIn, int stay, int fadeOut, SimpleComponent title, SimpleComponent subtitle) {
		if (title == null)
			title = SimpleComponent.empty();

		if (subtitle == null)
			subtitle = SimpleComponent.empty();

		final Duration fadeInDuration = Duration.ofMillis(fadeIn * 50);
		final Duration stayDuration = Duration.ofMillis(stay * 50);
		final Duration fadeOutDuration = Duration.ofMillis(fadeOut * 50);

		this.sender.showTitle(Title.title(title.toAdventure(), subtitle.toAdventure(), Times.times(fadeInDuration, stayDuration, fadeOutDuration)));
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
