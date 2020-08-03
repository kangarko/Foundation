package org.mineacademy.fo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.exception.FoException;

import java.util.Set;

/**
 * Represents a Discord command sender for Discord integration
 */
@RequiredArgsConstructor
public final class DiscordSender implements CommandSender {

	/**
	 * The name of the sender
	 */
	@Getter
	private final String name;

	@Override
	public boolean isPermissionSet(String permission) {		
		throw unsupported("isPermissionSet");
	}

	@Override
	public boolean isPermissionSet(Permission permission) {
		throw unsupported("isPermissionSet");
	}

	@Override
	public boolean hasPermission(String perm) {
		return perm == null ? true : HookManager.hasPermissionUnsafe(name, perm);
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return perm == null ? true : HookManager.hasPermissionUnsafe(name, perm.getName());
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		throw unsupported("addAttachment");
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		throw unsupported("addAttachment");
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		throw unsupported("addAttachment");
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		throw unsupported("addAttachment");
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		throw unsupported("removeAttachment");
	}

	@Override
	public void recalculatePermissions() {
		throw unsupported("recalculatePermissions");
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		throw unsupported("getEffectivePermissions");
	}

	@Override
	public boolean isOp() {
		throw unsupported("isOp");
	}

	@Override
	public void setOp(boolean op) {
		throw unsupported("setOp");
	}

	@Override
	public void sendMessage(String message) {
		// Silence is golden
	}

	@Override
	public void sendMessage(String[] message) {
		// Silence is golden
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public Spigot spigot() {
		throw unsupported("spigot");
	}

	private FoException unsupported(String method) {
		return new FoException("DiscordSender cannot invoke " + method + "()");
	}
}
