package org.mineacademy.fo.settings;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.model.SpigotUpdater;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * A simple implementation of a typical main plugin settings
 * where each key can be accessed in a static way from anywhere.
 * <p>
 * Typically we use this class for settings.yml main plugin config.
 */
// Use for settings.yml
@SuppressWarnings("unused")
public class SimpleSettings extends YamlStaticConfig {

	/**
	 * A flag indicating that this class has been loaded
	 * <p>
	 * You can place this class to {@link org.mineacademy.fo.plugin.SimplePlugin#getSettings()} ()} to
	 * make it load automatically
	 */
	private static boolean settingsClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	@Override
	protected final void onLoad() throws Exception {
		loadConfiguration(getSettingsFileName());
	}

	/**
	 * Get the file name for these settings, by default settings.yml
	 *
	 * @return
	 */
	protected String getSettingsFileName() {
		return FoConstants.File.SETTINGS;
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * The configuration version number, found in the "Version" key in the file.,
	 */
	public static Integer VERSION;

	/**
	 * Set and update the config version automatically, however the {@link #VERSION} will
	 * contain the older version used in the file on the disk so you can use
	 * it for comparing in the init() methods
	 * <p>
	 * Please call this as a super method when overloading this!
	 */
	@Override
	protected void preLoad() {
		// Load version first so we can use it later
		setPathPrefix(null);

		if ((VERSION = getInteger("Version")) != getConfigVersion())
			set("Version", getConfigVersion());
	}

	/**
	 * Return the very latest config version
	 * <p>
	 * Any changes here must also be made to the "Version" key in your settings file.
	 *
	 * @return
	 */
	protected int getConfigVersion() {
		return 1;
	}

	// --------------------------------------------------------------------
	// Settings we offer by default for your main config file
	// Specify those you need to modify
	// --------------------------------------------------------------------

	/**
	 * The {timestamp} format.
	 */
	public static DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	/**
	 * The {location} format.
	 */
	public static String LOCATION_FORMAT = "{world} [{x}, {y}, {z}]";

	/**
	 * What debug sections should we enable in {@link Debugger} ? When you call {@link Debugger#debug(String, String...)}
	 * those that are specified in this settings are logged into the console, otherwise no message is shown.
	 * <p>
	 * Typically this is left empty: Debug: []
	 */
	public static StrictList<String> DEBUG_SECTIONS = new StrictList<>();

	/**
	 * The plugin prefix in front of chat/console messages.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Prefix: "&8[&3ChatControl&8]&7 "
	 */
	public static String PLUGIN_PREFIX = "&7" + SimplePlugin.getNamed() + " //";

	/**
	 * The lag threshold used for {@link LagCatcher} in milliseconds. Set to -1 to disable.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Log_Lag_Over_Milis: 100
	 */
	public static Integer LAG_THRESHOLD_MILLIS = 100;

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 * <p>
	 * Regex_Timeout_Milis: 100
	 */
	public static Integer REGEX_TIMEOUT = 100;

	/**
	 * What commands should trigger the your main plugin command (separated by a comma ,)? See {@link SimplePlugin#getMainCommand()}
	 * <p>
	 * Typical values for ChatControl:
	 * <p>
	 * Command_Aliases: [chatcontrol, chc, cc]
	 * <p>
	 * // ONLY MANDATORY IF YOU OVERRIDE {@link SimplePlugin#getMainCommand()} //
	 */
	public static StrictList<String> MAIN_COMMAND_ALIASES = new StrictList<>();

	/**
	 * The localization prefix, given you are using {@link SimpleLocalization} class to load and manage your
	 * locale file. Typically the file path is: localization/messages_PREFIX.yml with this prefix below.
	 * <p>
	 * Typically: Locale: en
	 * <p>
	 * // ONLY MANDATORY IF YOU USE SIMPLELOCALIZATION //
	 */
	public static String LOCALE_PREFIX = "en";

	/**
	 * Should we check for updates from SpigotMC and notify the console and users with permission?
	 * <p>
	 * See {@link SimplePlugin#getUpdateCheck()} that you can make to return {@link SpigotUpdater} with your Spigot plugin ID.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Notify_Updates: true
	 * <p>
	 * // ONLY MANDATORY IF YOU OVERRIDE {@link SimplePlugin#getUpdateCheck()} //
	 */
	public static Boolean NOTIFY_UPDATES = true;

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		Valid.checkBoolean(!settingsClassCalled, "Settings class already loaded!");

		setPathPrefix(null);
		upgradeOldSettings();

		if (isSetDefault("Timestamp_Format"))
			try {
				TIMESTAMP_FORMAT = new SimpleDateFormat(getString("Timestamp_Format"));

			} catch (final IllegalArgumentException ex) {
				Common.throwError(ex, "Wrong 'Timestamp_Format '" + getString("Timestamp_Format") + "', see https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for examples'");
			}

		if (isSetDefault("Location_Format"))
			LOCATION_FORMAT = getString("Location_Format");

		if (isSetDefault("Prefix"))
			PLUGIN_PREFIX = getString("Prefix");

		if (isSetDefault("Log_Lag_Over_Milis")) {
			LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
			Valid.checkBoolean(LAG_THRESHOLD_MILLIS == -1 || LAG_THRESHOLD_MILLIS >= 0, "Log_Lag_Over_Milis must be either -1 to disable, 0 to log all or greater!");

			if (LAG_THRESHOLD_MILLIS == 0)
				Common.log("&eLog_Lag_Over_Milis is 0, all performance is logged. Set to -1 to disable.");
		}

		if (isSetDefault("Debug"))
			DEBUG_SECTIONS = new StrictList<>(getStringList("Debug"));

		if (isSetDefault("Regex_Timeout_Milis"))
			REGEX_TIMEOUT = getInteger("Regex_Timeout_Milis");

		// -------------------------------------------------------------------
		// Load maybe-mandatory values
		// -------------------------------------------------------------------

		{ // Load localization
			final boolean keySet = isSetDefault("Locale");

			LOCALE_PREFIX = keySet ? getString("Locale") : LOCALE_PREFIX;
		}

		{ // Load main command alias
			final boolean keySet = isSetDefault("Command_Aliases");

			MAIN_COMMAND_ALIASES = keySet ? getCommandList("Command_Aliases") : MAIN_COMMAND_ALIASES;
		}

		{ // Load updates notifier
			final boolean keySet = isSetDefault("Notify_Updates");

			NOTIFY_UPDATES = keySet ? getBoolean("Notify_Updates") : NOTIFY_UPDATES;
		}

		settingsClassCalled = true;
	}

	/**
	 * Upgrade some of the old and ancient settings from our premium plugins.
	 */
	private static void upgradeOldSettings() {

		{ // Debug
			if (isSet("Debugger"))
				move("Debugger", "Debug");

			if (isSet("Serialization_Number"))
				move("Serialization_Number", "Serialization");

			// ChatControl
			if (isSet("Debugger.Keys")) {
				move("Debugger.Keys", "Serialization");
				move("Debugger.Sections", "Debug");
			}

			// Archaic
			if (isSet("Debug") && !(getObject("Debug") instanceof List))
				set("Debug", null);
		}

		{ // Prefix
			if (isSet("Plugin_Prefix"))
				move("Plugin_Prefix", "Prefix");

			if (isSet("Check_Updates"))
				move("Check_Updates", "Notify_Updates");
		}
	}

	/**
	 * Was this class loaded?
	 *
	 * @return
	 */
	public static final Boolean isSettingsCalled() {
		return settingsClassCalled;
	}

	/**
	 * Reset the flag indicating that the class has been loaded,
	 * used in reloading.
	 */
	public static final void resetSettingsCall() {
		settingsClassCalled = false;
	}
}
