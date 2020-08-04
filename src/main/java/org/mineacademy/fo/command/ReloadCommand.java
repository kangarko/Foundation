package org.mineacademy.fo.command;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * A simple predefined command for quickly reloading the plugin
 * using /{label} reload|rl
 */
public final class ReloadCommand extends SimpleSubCommand {

	public ReloadCommand() {
		super("reload|rl");

		setDescription("Reload the configuration.");
	}

	@Override
	protected void onCommand() {
		try {
			SimplePlugin.getInstance().reload();
			Common.tell(sender, SimpleLocalization.Commands.RELOAD_SUCCESS.replace("{plugin_name}", SimplePlugin.getNamed()).replace("{plugin_version}", SimplePlugin.getVersion()));

		} catch (final Throwable t) {
			Common.tell(sender, SimpleLocalization.Commands.RELOAD_FAIL.replace("{error}", t.getMessage() != null ? t.getMessage() : "unknown"));

			t.printStackTrace();
		}
	}
}