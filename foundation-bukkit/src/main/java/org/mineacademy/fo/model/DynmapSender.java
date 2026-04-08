package org.mineacademy.fo.model;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.dynmap.bukkit.DynmapPlugin;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.Platform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

/**
 * Represents a Dynmap command sender for Dynmap integration
 */
@Getter
@RequiredArgsConstructor
public final class DynmapSender implements CommandSender {

	/**
	 * The given name, might be empty, in this case we supply "web"
	 */
	private final String name;

	/**
	 * The unique ID of the sender
	 */
	private final UUID uniqueId;

	/**
	 * The associated online player
	 */
	private final Player onlinePlayer;

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
		return this.onlinePlayer != null && this.onlinePlayer.hasPermission(perm);
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
		this.sendPlainMessage(SimpleComponent.fromMiniAmpersand(message).toPlain());
	}

	@Override
	public void sendPlainMessage(final String message) {
		Platform.runTaskAsync(() -> {
			try {
				DynmapPlugin.plugin.sendBroadcastToWeb(this.name, message);

			} catch (final NoClassDefFoundError ex) {
				CommonCore.warning("DynMap plugin is missing, not sending: " + message);
			}
		});
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
		return new FoException(this.getClass().getSimpleName() + " cannot invoke " + method + "()");
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
		return Component.text(this.getName());
	}
}
