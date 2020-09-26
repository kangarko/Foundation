package org.mineacademy.fo.command;

import java.util.List;

import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.YamlConfig;

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
			// Syntax check YML files before loading
			boolean syntaxParsed = true;

			for (final YamlConfig loaded : YamlConfig.getLoadedFiles()) {
				try {
					if (loaded.getFile().exists())
						FileUtil.loadConfigurationStrict(loaded.getFile());

				} catch (final FoException ex) {
					ex.printStackTrace();

					syntaxParsed = false;
				}
			}

			if (!syntaxParsed) {
				tell(SimpleLocalization.Commands.RELOAD_FILE_LOAD_ERROR);

				return;
			}

			SimplePlugin.getInstance().reload();
			tell(SimpleLocalization.Commands.RELOAD_SUCCESS);

		} catch (final Throwable t) {
			tell(SimpleLocalization.Commands.RELOAD_FAIL.replace("{error}", t.getMessage() != null ? t.getMessage() : "unknown"));

			t.printStackTrace();
		}
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}