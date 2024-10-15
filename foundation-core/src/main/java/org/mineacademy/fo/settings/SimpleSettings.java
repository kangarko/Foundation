package org.mineacademy.fo.settings;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.Platform;

/**
 * A simple implementation of a typical main plugin settings
 * where each key can be accessed in a static way from anywhere.
 * <p>
 * Typically we use this class for settings.yml main plugin config.
 */
@SuppressWarnings("unused")
public class SimpleSettings extends YamlStaticConfig {

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	@Override
	protected final void load() throws Exception {
		this.loadConfiguration(this.getSettingsFileName());
	}

	/**
	 * Get the file name for these settings, by default settings.yml
	 *
	 * @return
	 */
	protected String getSettingsFileName() {
		return "settings.yml";
	}

	// --------------------------------------------------------------------
	// Static keys - they haved a default value but we use the one from
	// your file if they are set.
	// --------------------------------------------------------------------

	/**
	 * What commands should trigger the your main plugin command (separated by a comma ,)?
	 * <p>
	 * Typical values for ChatControl:
	 * <p>
	 * Command_Aliases: [chatcontrol, chc, cc]
	 * <p>
	 */
	public static List<String> MAIN_COMMAND_ALIASES = new ArrayList<>();

	/**
	 * The plugin prefix in front of chat/console messages.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Prefix: "&8[&3ChatControl&8]&7 "
	 */
	public static SimpleComponent PREFIX = SimpleComponent.fromMini("&7" + Platform.getPlugin().getName() + " //");

	/**
	 * What debug sections should we enable in {@link Debugger} ? When you call {@link Debugger#debug(String, String...)}
	 * those that are specified in this settings are logged into the console, otherwise no message is shown.
	 * <p>
	 * Typically this is left empty: Debug: []
	 */
	public static List<String> DEBUG_SECTIONS = new ArrayList<>();

	/**
	 * The lag threshold used for {@link LagCatcher} in milliseconds. Set to -1 to disable.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Log_Lag_Over_Milis: 100
	 */
	public static Integer LAG_THRESHOLD_MILLIS = 100;

	/**
	 * The localization language tag.
	 *
	 * Typically: Locale: en_US
	 */
	public static String LOCALE = "en_US";

	/**
	 * Notify updates to ops on join and the console?
	 *
	 * Defaults to true.
	 */
	public static Boolean NOTIFY_NEW_VERSIONS = true;

	/**
	 * Report errors to Sentry.io?
	 *
	 * Defaults to true.
	 */
	public static Boolean SENTRY = true;

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		setPathPrefix(null);

		if (isSetDefault("Command_Aliases"))
			MAIN_COMMAND_ALIASES = getCommandList("Command_Aliases");

		if (isSetDefault("Prefix"))
			PREFIX = getComponent("Prefix");

		if (isSetDefault("Debug"))
			DEBUG_SECTIONS = getStringList("Debug");

		if (isSetDefault("Log_Lag_Over_Milis")) {
			LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
			ValidCore.checkBoolean(LAG_THRESHOLD_MILLIS == -1 || LAG_THRESHOLD_MILLIS >= 0, "Log_Lag_Over_Milis must be either -1 to disable, 0 to log all or greater!");

			if (LAG_THRESHOLD_MILLIS == 0)
				CommonCore.log("&eLog_Lag_Over_Milis is 0, all performance is logged. Set to -1 to disable.");
		}

		if (isSetDefault("Locale")) {
			LOCALE = getString("Locale");

			if (LOCALE.equals("en")) {
				LOCALE = "en_US";
				set("Locale", "en_US");

			} else if (!LOCALE.contains("_")) {
				CommonCore.warning("Locale '" + LOCALE + "' is invalid, defaulting to 'en_US'.");

				LOCALE = "en_US";
				set("Locale", "en_US");
			}
		}

		if (isSetDefault("Notify_New_Versions"))
			NOTIFY_NEW_VERSIONS = getBoolean("Notify_New_Versions");

		if (isSetDefault("Sentry"))
			SENTRY = getBoolean("Sentry");
	}
}
