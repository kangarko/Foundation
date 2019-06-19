package org.mineacademy.fo.settings;

import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.update.SpigotUpdateCheck;

/**
 * A simple implementation of a typical main plugin settings
 * where each key can be accessed in a static way from anywhere.
 *
 * Typically we use this class for settings.yml main plugin config.
 */
// Use for settings.yml
@SuppressWarnings("unused")
public abstract class SimpleSettings extends YamlStaticConfig {

	/**
	 * A flag indicating that this class has been loaded
	 *
	 * You can place this class to {@link SimplePlugin#getSettingsClasses()} to make
	 * it load automatically
	 */
	private static boolean settingsClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	@Override
	protected final void load() throws Exception {
		createFileAndLoad(FoConstants.File.SETTINGS);
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * The configuration version number, found in the "Version" key in the file.,
	 */
	protected static Integer VERSION;

	/**
	 * Set and update the config version automatically, however the {@link #VERSION} will
	 * contain the older version used in the file on the disk so you can use
	 * it for comparing in the init() methods
	 *
	 * Please call this as a super method when overloading this!
	 */
	@Override
	protected void beforeLoad() {
		// Load version first so we can use it later
		pathPrefix(null);

		if ((VERSION = getInteger("Version")) != getConfigVersion())
			set("Version", getConfigVersion());
	}

	/**
	 * Return the very latest config version
	 *
	 * Any changes here must also be made to the "Version" key in your settings file.
	 *
	 * @return
	 */
	protected abstract int getConfigVersion();

	// --------------------------------------------------------------------
	// Shared values
	// --------------------------------------------------------------------

	// Unless specified in the init() method, you must write the values
	// below to your settings.yml for this class to function!

	// Values here are defined below so that plugins not definiting their settings.yml still can function,
	// however if your plugin has a settings.yml file you MUST specify all of them in that file.

	/**
	 * What commands should trigger the your main plugin command (separated by a comma ,)? See {@link SimplePlugin#getMainCommand()}
	 *
	 * Typical values for ChatControl:
	 *
	 * Command_Aliases: [chatcontrol, chc, cc]
	 */
	public static StrictList<String> MAIN_COMMAND_ALIASES = new StrictList<>();

	/**
	 * What debug sections should we enable in {@link Debugger} ? When you call {@link Debugger#debug(String, String...)}
	 * those that are specified in this settings are logged into the console, otherwise no message is shown.
	 *
	 * Typically this is left empty: Debug: []
	 */
	public static StrictList<String> DEBUG_SECTIONS = new StrictList<>();

	/**
	 * The localization prefix, given you are using {@link SimpleLocalization} class to load and manage your
	 * locale file. Typically the file path is: localization/messages_PREFIX.yml with this prefix below.
	 *
	 * Typically: Locale: en
	 */
	public static String LOCALE_PREFIX = "en";

	/**
	 * The plugin prefix in front of chat/console messages, added automatically unless
	 * disabled in {@link Common#ADD_LOG_PREFIX} and {@link Common#ADD_TELL_PREFIX}.
	 *
	 * Typically for ChatControl:
	 *
	 * Prefix: "&8[&3ChatControl&8]&7 "
	 */
	public static String PLUGIN_PREFIX = "[" + SimplePlugin.getNamed() + "]"; // Only defined here so we can log messages before settings are loaded, but you still need to write it

	/**
	 * The server name used in {server_name} variable or Bungeecords, if your plugin supports either of those.
	 *
	 * Typically for ChatControl:
	 *
	 * Server_Name: "My ChatControl Server"
	 */
	public static String SERVER_NAME = "Server";

	/**
	 * Antipiracy stuff for our protected software, leave empty to Serialization: ""
	 */
	public static String SECRET_KEY = "";

	/**
	 * The lag threshold used for {@link LagCatcher} in milliseconds. Set to 0 to disable.
	 *
	 * Typically for ChatControl:
	 *
	 * Log_Lag_Over_Milis: 100
	 */
	public static Integer LAG_THRESHOLD_MILLIS = -1; // Only defined here so we can log lag before settings are loaded, but you still need to write it

	/**
	 * Should we check for updates from SpigotMC and notify the console and users with permission?
	 *
	 * See {@link SimplePlugin#getUpdateCheck()} that you can make to return {@link SpigotUpdateCheck} with your Spigot plugin ID.
	 *
	 * Typically for ChatControl:
	 *
	 * Notify_Updates: true
	 */
	public static Boolean NOTIFY_UPDATES = false;

	/**
	 * Should we enable inbuilt advertisements?
	 * ** We found out that users really hate this feature, you may want not to use this completelly **
	 * ** If you want to broadcast important messages regardless of this feature just implement your **
	 * ** own Runnable that checks for a YAML file on your external server on plugin load. **
	 *
	 * Typically for ChatControl
	 *
	 * Notify_Promotions: true
	 */
	public static Boolean NOTIFY_PROMOTIONS = true;

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		Valid.checkBoolean(!settingsClassCalled, "Settings class already loaded!");

		pathPrefix(null);
		upgradeOldSettings();

		MAIN_COMMAND_ALIASES = getCommandList("Command_Aliases");
		DEBUG_SECTIONS = new StrictList<>(getStringList("Debug"));
		LOCALE_PREFIX = getString("Locale");
		PLUGIN_PREFIX = getString("Prefix");
		SERVER_NAME = isSetAbsolute("Server_Name") ? Common.colorize(getString("Server_Name")) : "Server Name Undefined";
		LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
		NOTIFY_UPDATES = getBoolean("Notify_Updates");
		NOTIFY_PROMOTIONS = getBoolean("Notify_Promotions");
		SECRET_KEY = getString("Serialization");

		settingsClassCalled = true;
	}

	/**
	 * Upgrade some of the old and ancient settings from our premium plugins.
	 */
	private static void upgradeOldSettings() {
		{ // Debug
			if (isSetAbsolute("Debugger"))
				move("Debugger", "Debug");

			if (isSetAbsolute("Serialization_Number"))
				move("Serialization_Number", "Serialization");

			// ChatControl
			if (isSetAbsolute("Debugger.Keys")) {
				move("Debugger.Keys", "Serialization");
				move("Debugger.Sections", "Debug");
			}

			// Archaic
			if (isSetAbsolute("Debug") && !(getGodKnowsWhat("Debug") instanceof List))
				set("Debug", null);
		}

		{ // Prefix
			if (isSetAbsolute("Plugin_Prefix"))
				move("Plugin_Prefix", "Prefix");

			if (isSetAbsolute("Check_Updates"))
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
