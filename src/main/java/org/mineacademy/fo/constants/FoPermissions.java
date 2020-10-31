package org.mineacademy.fo.constants;

import org.mineacademy.fo.command.annotation.Permission;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * Used to store basic library permissions
 */
public class FoPermissions {

	@Permission(value = "Receive plugin update notifications on join.")
	public static final String NOTIFY_UPDATE;

	static {
		NOTIFY_UPDATE = SimplePlugin.getNamed().toLowerCase() + ".notify.update";
	}
}