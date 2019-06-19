package org.mineacademy.fo.model;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a Discord command sender for discord integration
 *
 * Special use only in our plugins
 */
@RequiredArgsConstructor
public final class DiscordSender implements CommandSender {

	/**
	 * The name of the sender
	 */
	@Getter
	private final String name;

	/**
	 * ChatControl flag for mute bypass
	 */
	private boolean bypassMute; // can still send message from Discord to Bukkit even if the channel is muted? For admins.

	public void setBypassMuted() {
		bypassMute = true;
	}

	public boolean canBypassMuted() {
		return bypassMute;
	}

	@Override
	public boolean isPermissionSet(String p0) {
		return false;
	}

	@Override
	public boolean isPermissionSet(Permission p0) {
		return false;
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
	public PermissionAttachment addAttachment(Plugin p0, String p1, boolean p2) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin p0) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin p0, String p1, boolean p2, int p3) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin p0, int p1) {
		return null;
	}

	@Override
	public void removeAttachment(PermissionAttachment p0) {

	}

	@Override
	public void recalculatePermissions() {

	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return null;
	}

	@Override
	public boolean isOp() {
		final OfflinePlayer player = Bukkit.getOfflinePlayer(name);

		return player != null ? player.isOp() : false;
	}

	@Override
	public void setOp(boolean p0) {
	}

	@Override
	public void sendMessage(String p0) {
		Common.log(p0);
	}

	@Override
	public void sendMessage(String[] p0) {
		Common.log(p0);
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public Spigot spigot() {
		return null;
	}
}
