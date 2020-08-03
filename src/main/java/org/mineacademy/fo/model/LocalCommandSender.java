package org.mineacademy.fo.model;

import java.util.Set;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

/**
 * Command sender used if this library lacks a Minecraft server (normally never, we
 * are not suited for standardized use without Bukkit server yet)
 */
@Deprecated
public class LocalCommandSender implements CommandSender {

	public static final LocalCommandSender INSTANCE = new LocalCommandSender();

	private LocalCommandSender() {
	}

	@Override
	public PermissionAttachment addAttachment(Plugin arg0) {
		throw err();
	}

	@Override
	public PermissionAttachment addAttachment(Plugin arg0, int arg1) {
		throw err();
	}

	@Override
	public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2) {
		throw err();
	}

	@Override
	public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2, int arg3) {
		throw err();
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		throw err();
	}

	@Override
	public boolean hasPermission(String arg0) {
		throw err();
	}

	@Override
	public boolean hasPermission(Permission arg0) {
		throw err();
	}

	@Override
	public boolean isPermissionSet(String arg0) {
		throw err();
	}

	@Override
	public boolean isPermissionSet(Permission arg0) {
		throw err();
	}

	@Override
	public void recalculatePermissions() {
		te();
	}

	@Override
	public void removeAttachment(PermissionAttachment arg0) {
		te();
	}

	@Override
	public boolean isOp() {
		throw err();
	}

	@Override
	public void setOp(boolean arg0) {
		te();
	}

	@Override
	public String getName() {
		return "[LocalConsole]";
	}

	@Override
	public Server getServer() {
		throw err();
	}

	@Override
	public void sendMessage(String arg0) {
		System.out.println(arg0);
	}

	@Override
	public void sendMessage(String[] msgs) {
		for (final String msg : msgs)
			sendMessage(msg);
	}

	@Override
	public Spigot spigot() {
		throw err();
	}

	private final void te() {
		throw err();
	}

	private final RuntimeException err() {
		return new RuntimeException("unsupported");
	}
}
