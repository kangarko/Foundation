package org.mineacademy.fo.model;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.Platform;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

/**
 * Represents a Discord command sender for Discord integration
 */
@Getter
@RequiredArgsConstructor
public final class DiscordSender implements CommandSender {

	private final String name;
	private final UUID uniqueId;
	/**
	 * @deprecated this will store arbitrary player cache value which is used in upstream plugins i.e.
	 * chatcontrol to contain data, i.e. whether some player ignores this player or not, etc.
	 */
	@Deprecated
	private final Object cache;
	private final OfflinePlayer offlinePlayer;
	private final User user;
	private final MessageChannel channel;
	private final Message message;

	@Override
	public boolean isPermissionSet(final String permission) {
		throw this.unsupported("isPermissionSet");
	}

	@Override
	public boolean isPermissionSet(final Permission permission) {
		throw this.unsupported("isPermissionSet");
	}

	@Override
	public boolean hasPermission(final String perm) {
		if (HookManager.isVaultLoaded()) {
			final Boolean result = HookManager.hasVaultPermission(this.offlinePlayer, perm);

			return result != null && result;
		}

		return false;
	}

	@Override
	public boolean hasPermission(final Permission perm) {
		return this.hasPermission(perm.getName());
	}

	@Override
	public PermissionAttachment addAttachment(final Plugin plugin, final String name, final boolean value) {
		throw this.unsupported("addAttachment");
	}

	@Override
	public PermissionAttachment addAttachment(final Plugin plugin) {
		throw this.unsupported("addAttachment");
	}

	@Override
	public PermissionAttachment addAttachment(final Plugin plugin, final String name, final boolean value, final int ticks) {
		throw this.unsupported("addAttachment");
	}

	@Override
	public PermissionAttachment addAttachment(final Plugin plugin, final int ticks) {
		throw this.unsupported("addAttachment");
	}

	@Override
	public void removeAttachment(final PermissionAttachment attachment) {
		throw this.unsupported("removeAttachment");
	}

	@Override
	public void recalculatePermissions() {
		throw this.unsupported("recalculatePermissions");
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		throw this.unsupported("getEffectivePermissions");
	}

	@Override
	public boolean isOp() {
		throw this.unsupported("isOp");
	}

	@Override
	public void setOp(final boolean op) {
		throw this.unsupported("setOp");
	}

	@Override
	public void sendMessage(final String... messages) {
		for (final String message : messages)
			this.sendMessage(message);
	}

	@Override
	public void sendMessage(final String message) {
		final String finalMessage = CompChatColor.stripColorCodes(message);

		Platform.runTaskAsync(() -> {
			final Message sentMessage = this.channel.sendMessage(finalMessage).complete();

			try {
				// Automatically remove after a short while
				this.channel.deleteMessageById(sentMessage.getIdLong()).completeAfter(4, TimeUnit.SECONDS);

			} catch (final Throwable t) {

				// Ignore already deleted messages
				if (!t.toString().contains("Unknown Message"))
					t.printStackTrace();
			}
		});
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public Spigot spigot() {
		throw this.unsupported("spigot");
	}

	private FoException unsupported(final String method) {
		return new FoException("DiscordSender cannot invoke " + method + "()");
	}

	/**
	 * @see org.bukkit.command.CommandSender#sendMessage(java.util.UUID, java.lang.String)
	 */
	@Override
	public void sendMessage(final UUID uuid, final String message) {
		this.sendMessage(message);
	}

	/**
	 * @see org.bukkit.command.CommandSender#sendMessage(java.util.UUID, java.lang.String[])
	 */
	@Override
	public void sendMessage(final UUID uuid, final String... messages) {
		this.sendMessage(messages);
	}

	@Override
	public Component name() {
		return Component.text(this.name);
	}

	/**
	 * Get the name of the channel wherefrom the dude sent his message.
	 *
	 * @return
	 */
	public String getChannelName() {
		return this.channel.getName();
	}
}
