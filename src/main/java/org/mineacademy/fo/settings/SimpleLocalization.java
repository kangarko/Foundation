package org.mineacademy.fo.settings;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * A simple implementation of a basic localization file.
 * We create the localization/messages_LOCALEPREFIX.yml file
 * automatically and fill it with values from your localization/messages_LOCALEPREFIX.yml
 * file placed within in your plugins jar file.
 */
@SuppressWarnings("unused")
public abstract class SimpleLocalization extends YamlStaticConfig {

	/**
	 * A fallback localization is a file in your plugin's JAR we open and look values
	 * in, when user-selected localization doesn't have them nor it has them in your JAR.
	 * <br><br>
	 * Example:<br>
	 * 	messages_en.yml as fallback in the JAR<br>
	 * 	messages_it.yml in the JAR<br>
	 * 	messages_it.yml on the disk & used<br>
	 * <br>
	 * When there is a string missing from messages_it on the disk, we try to place the
	 * default one from the same file in the JAR. However, when messages_it.yml in the
	 * JAR also lacks this file, we then visit the fallback file.
	 * <br>
	 * This saves enormous amount of time since you don't have to copy-paste new
	 * localization keys over all message files each time you make an update. Instead,
	 * only place them into the fallback file, by default it's messages_en.yml.
	 */
	private static String FALLBACK_LOCALIZATION_FILE = "localization/messages_en.yml";

	/**
	 * See {@link #FALLBACK_LOCALIZATION_FILE}
	 *
	 * @param fallBackFile
	 */
	public static void setFallbackLocalizationFile(final String fallBackFile) {
		FALLBACK_LOCALIZATION_FILE = fallBackFile;
	}

	/**
	 * The fallback localization file config instance, see {@link #FALLBACK_LOCALIZATION_FILE}.
	 */
	private static YamlConfiguration fallbackLocalization;

	/**
	 * A flag indicating that this class has been loaded
	 * <p>
	 * You can place this class to {@link SimplePlugin#getSettings()} to make
	 * it load automatically
	 */
	private static boolean localizationClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	/**
	 * Create and load the localization/messages_LOCALEPREFIX.yml file.
	 * <p>
	 * See {@link SimpleSettings#LOCALE_PREFIX} for the locale prefix.
	 * <p>
	 * The locale file is extracted from your plugins jar to the localization/ folder
	 * if it does not exists, or updated if it is out of date.
	 */
	@Override
	protected final void load() throws Exception {
		createLocalizationFile(SimpleSettings.LOCALE_PREFIX);
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
	 * <p>
	 * Please call this as a super method when overloading this!
	 */
	@Override
	protected final void preLoad() {
		// Load version first so we can use it later
		pathPrefix(null);

		if ((VERSION = getInteger("Version")) != getConfigVersion())
			set("Version", getConfigVersion());

		// Load English localization file
		fallbackLocalization = FileUtil.loadInternalConfiguration(FALLBACK_LOCALIZATION_FILE);
	}

	/**
	 * Return the very latest config version
	 * <p>
	 * Any changes here must also be made to the "Version" key in your settings file.
	 *
	 * @return
	 */
	protected abstract int getConfigVersion();

	// --------------------------------------------------------------------
	// Fallback
	// --------------------------------------------------------------------

	/**
	 * Get a String from the localization file, utilizing
	 * {@link #FALLBACK_LOCALIZATION_FILE} mechanics
	 *
	 * @param path
	 * @return
	 */
	protected static final String getFallbackString(final String path) {
		return getFallback(path, String.class);
	}

	/**
	 * Get a list from the localization file, utilizing
	 * {@link #FALLBACK_LOCALIZATION_FILE} mechanics
	 *
	 * @param <T>
	 * @param path
	 * @param listType
	 * @return
	 */
	protected static final <T> List<T> getFallbackList(final String path, final Class<T> listType) {
		final List<T> list = new ArrayList<>();
		final List<Object> objects = getFallback(path, List.class);

		if (objects != null)
			for (final Object object : objects)
				list.add(object != null ? SerializeUtil.deserialize(listType, object) : null);

		return list;
	}

	/**
	 * Get a key from the localization file, utilizing
	 * {@link #FALLBACK_LOCALIZATION_FILE} mechanics
	 *
	 * @param <T>
	 * @param path
	 * @param typeOf
	 * @return
	 */
	protected static final <T> T getFallback(final String path, final Class<T> typeOf) {

		// If string already exists, has a default path, or locale is set to english, use the native method
		if (isSet(path) || isSetDefault(path) || "en".equals(SimpleSettings.LOCALE_PREFIX))
			return get(path, typeOf);

		// Try to pull the value from English localization
		final String relativePath = formPathPrefix(path);
		final Object key = fallbackLocalization.get(relativePath);

		Valid.checkNotNull(key, "Neither " + getFileName() + ", the default one, nor " + FALLBACK_LOCALIZATION_FILE + " contained " + relativePath + "! Please report this to " + SimplePlugin.getNamed() + " developers!");
		Valid.checkBoolean(key.getClass().isAssignableFrom(typeOf), "Expected " + typeOf + " at " + relativePath + " in " + FALLBACK_LOCALIZATION_FILE + " but got " + key.getClass() + ": " + key);

		// Write it to the file being used
		set(path, key);

		return (T) key;
	}

	// --------------------------------------------------------------------
	// Shared values
	// --------------------------------------------------------------------

	// NB: Those keys are optional - you do not have to write them into your messages_X.yml files
	// but if you do, we will use your values instead of the default ones!

	/**
	 * Locale keys related to your plugin commands
	 */
	public static final class Commands {

		/**
		 * The message at "No_Console" key shown when console is denied executing a command.
		 */
		public static String NO_CONSOLE = "&cYou may only use this command as a player";

		/**
		 * The message shown when there is a fatal error running this command
		 */
		public static String COOLDOWN_WAIT = "&cWait {duration} second(s) before using this command again.";

		/**
		 * The message shown when the player tries a command but inputs an
		 * invalid first argument parameter. We suggest he types /{label} ? for help so make
		 * sure you implement some help there as well.
		 */
		public static String INVALID_ARGUMENT = "&cInvalid argument. Run &6/{label} ? &cfor help.";

		/**
		 * The message shown when the player tries a command but inputs an
		 * invalid second argument parameter. We so suggest he types /{label} {0} for help
		 */
		public static String INVALID_SUB_ARGUMENT = "&cInvalid argument. Run '/{label} {0}' for help.";

		/**
		 * The message shown on the same occasion as {@link #INVALID_ARGUMENT} however
		 * this is shows when the command overrides {@link SimpleCommand#getMultilineUsageMessage()}
		 */
		public static String INVALID_ARGUMENT_MULTILINE = "&cInvalid argument. Usage:";

		/**
		 * The authors label
		 */
		public static String LABEL_AUTHORS = "Made by";

		/**
		 * The description label
		 */
		public static String LABEL_DESCRIPTION = "&cDescription: {description}";

		/**
		 * The optional arguments label
		 */
		public static String LABEL_OPTIONAL_ARGS = "optional arguments";

		/**
		 * The required arguments label
		 */
		public static String LABEL_REQUIRED_ARGS = "required arguments";

		/**
		 * The multiline usages label, see {@link SimpleCommand#getMultilineUsageMessage()}
		 */
		public static String LABEL_USAGES = "&cUsages:";

		/**
		 * The usage label
		 */
		public static String LABEL_USAGE = "&cUsage:";

		/**
		 * The message at "Reload_Success" key shown when the plugin has been reloaded successfully.
		 */
		public static String RELOAD_SUCCESS = "&6{plugin_name} {plugin_version} has been reloaded.";

		/**
		 * The message ar "Reload_File_Load_Error" key shown then pre-loading disk files fails.
		 */
		public static String RELOAD_FILE_LOAD_ERROR = "&4Oups, &cthere was a problem loading files from your disk! See the console for more information. {plugin_name} has not been reloaded.";

		/**
		 * The message at "Reload_Fail" key shown when the plugin has failed to reload.
		 */
		public static String RELOAD_FAIL = "&4Oups, &creloading failed! See the console for more information. Error: {error}";

		/**
		 * The message shown when there is a fatal error running this command
		 */
		public static String ERROR = "&4&lOups! &cThe command failed :( Check the console and report the error.";

		/**
		 * The message shown when player has no permissions to view ANY subcommands in group command.
		 */
		public static String HEADER_NO_SUBCOMMANDS = "&cThere are no arguments for this command.";

		/**
		 * The message shown when player has no permissions to view ANY subcommands in group command.
		 */
		public static String HEADER_NO_SUBCOMMANDS_PERMISSION = "&cYou don't have permissions to view any subcommands.";

		/**
		 * Key for when plugin is reloading {@link org.mineacademy.fo.plugin.SimplePlugin}
		 */
		public static String RELOADING = "reloading";

		/**
		 * Key for when plugin is disabled {@link org.mineacademy.fo.plugin.SimplePlugin}
		 */

		public static String DISABLED = "disabled";
		/**
		 * The message shown when plugin is reloading or was disabled and player attempts to run command
		 */
		public static String USE_WHILE_NULL = "&cCannot use this command while the plugin is {state}.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			pathPrefix("Commands");

			if (isSetDefault("No_Console"))
				NO_CONSOLE = getString("No_Console");

			if (isSetDefault("Cooldown_Wait"))
				COOLDOWN_WAIT = getString("Cooldown_Wait");

			if (isSetDefault("Invalid_Argument"))
				INVALID_ARGUMENT = getString("Invalid_Argument");

			if (isSetDefault("Invalid_Sub_Argument"))
				INVALID_SUB_ARGUMENT = getString("Invalid_Sub_Argument");

			if (isSetDefault("Invalid_Argument_Multiline"))
				INVALID_ARGUMENT_MULTILINE = getString("Invalid_Argument_Multiline");

			if (isSetDefault("Label_Authors"))
				LABEL_AUTHORS = getString("Label_Authors");

			if (isSetDefault("Label_Description"))
				LABEL_DESCRIPTION = getString("Label_Description");

			if (isSetDefault("Label_Optional_Args"))
				LABEL_OPTIONAL_ARGS = getString("Label_Optional_Args");

			if (isSetDefault("Label_Required_Args"))
				LABEL_REQUIRED_ARGS = getString("Label_Required_Args");

			if (isSetDefault("Label_Usage"))
				LABEL_USAGE = getString("Label_Usage");

			if (isSetDefault("Label_Usages"))
				LABEL_USAGES = getString("Label_Usages");

			if (isSetDefault("Reload_Success"))
				RELOAD_SUCCESS = getString("Reload_Success");

			if (isSetDefault("Reload_File_Load_Error"))
				RELOAD_FILE_LOAD_ERROR = getString("Reload_File_Load_Error");

			if (isSetDefault("Reload_Fail"))
				RELOAD_FAIL = getString("Reload_Fail");

			if (isSetDefault("Error"))
				ERROR = getString("Error");

			if (isSetDefault("Header_No_Subcommands"))
				HEADER_NO_SUBCOMMANDS = getString("Header_No_Subcommands");

			if (isSetDefault("Header_No_Subcommands_Permission"))
				HEADER_NO_SUBCOMMANDS_PERMISSION = getString("Header_No_Subcommands_Permission");

			if (isSet("Reloading"))
				RELOADING = getString("Reloading");

			if (isSet("Disabled"))
				DISABLED = getString("Disabled");

			if (isSet("Use_While_Null"))
				USE_WHILE_NULL = getString("Use_While_Null");
		}
	}

	/**
	 * Key related to players
	 */
	public static final class Player {

		/**
		 * Message shown when the player is not online on this server
		 */
		public static String NOT_ONLINE = "&cPlayer {player} &cis not online on this server.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			pathPrefix("Player");

			if (isSetDefault("Not_Online"))
				NOT_ONLINE = getString("Not_Online");
		}
	}

	/**
	 * Key related to the GUI system
	 */
	public static final class Menu {

		/**
		 * Message shown when the player is not online on this server
		 */
		public static String ITEM_DELETED = "&2The {item} has been deleted.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			pathPrefix("Menu");

			if (isSetDefault("Item_Deleted"))
				ITEM_DELETED = getString("Item_Deleted");
		}
	}

	/**
	 * Keys related to updating the plugin
	 */
	public static final class Update {

		/**
		 * The message if a new version is found but not downloaded
		 */
		public static String AVAILABLE = "&2A new version of &3{plugin_name}&2 is available.\n"
				+ "&2Current version: &f{current}&2; New version: &f{new}\n"
				+ "&2URL: &7https://spigotmc.org/resources/{resource_id}/.";

		/**
		 * The message if a new version is found and downloaded
		 */
		public static String DOWNLOADED = "&3{plugin_name}&2 has been upgraded from {current} to {new}.\n"
				+ "&2Visit &7https://spigotmc.org/resources/{resource_id} &2for more information.\n"
				+ "&2Please restart the server to load the new version.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			pathPrefix(null);

			// Upgrade from old path
			if (isSetAbsolute("Update_Available"))
				move("Update_Available", "Update.Available");

			pathPrefix("Update");

			if (isSetDefault("Available"))
				AVAILABLE = getString("Available");

			if (isSetDefault("Downloaded"))
				DOWNLOADED = getString("Downloaded");
		}
	}

	/**
	 * The message for player if they lack a permission.
	 */
	public static String NO_PERMISSION = "&cInsufficient permission ({permission}).";

	/**
	 * The server prefix. Example: you have to use it manually if you are sending messages
	 * from the console to players
	 */
	public static String SERVER_PREFIX = "[Server]";

	/**
	 * The console localized name. Example: Console
	 */
	public static String CONSOLE_NAME = "Console";

	/**
	 * The message when a section is missing from data.db file (typically we use
	 * this file to store serialized values such as arenas from minigame plugins).
	 */
	public static String DATA_MISSING = "&c{name} lacks database information! Please only create {type} in-game! Skipping..";

	/**
	 * The message when the console attempts to start a server conversation which is prevented.
	 */
	public static String CONVERSATION_REQUIRES_PLAYER = "Only players may enter this conversation.";

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		pathPrefix(null);
		Valid.checkBoolean(!localizationClassCalled, "Localization class already loaded!");

		if (isSetDefault("No_Permission"))
			NO_PERMISSION = getString("No_Permission");

		if (isSetDefault("Server_Prefix"))
			SERVER_PREFIX = getString("Server_Prefix");

		if (isSetDefault("Console_Name"))
			CONSOLE_NAME = getString("Console_Name");

		if (isSetDefault("Data_Missing"))
			DATA_MISSING = getString("Data_Missing");

		if (isSetDefault("Conversation_Requires_Player"))
			CONVERSATION_REQUIRES_PLAYER = getString("Conversation_Requires_Player");

		localizationClassCalled = true;
	}

	/**
	 * Was this class loaded?
	 *
	 * @return
	 */
	public static final Boolean isLocalizationCalled() {
		return localizationClassCalled;
	}

	/**
	 * Reset the flag indicating that the class has been loaded,
	 * used in reloading.
	 */
	public static final void resetLocalizationCall() {
		localizationClassCalled = false;
	}
}
