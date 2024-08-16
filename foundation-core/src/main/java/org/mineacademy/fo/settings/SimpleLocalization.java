package org.mineacademy.fo.settings;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MessengerCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.command.DebugCommand;
import org.mineacademy.fo.command.PermsCommand;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.settings.FileConfig.AccusativeHelper;

import net.kyori.adventure.text.Component;

/**
 * A simple implementation of a basic localization file.
 * We create the localization/messages_LOCALEPREFIX.yml file
 * automatically and fill it with values from your localization/messages_LOCALEPREFIX.yml
 * file placed within in your plugin's jar file.
 */
@SuppressWarnings("unused")
public class SimpleLocalization extends YamlStaticConfig {

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
	protected final void onLoad() throws Exception {
		final String localePath = "localization/messages_" + SimpleSettings.LOCALE_PREFIX + ".yml";
		final Object content = FileUtil.getInternalFileContent(localePath);

		ValidCore.checkNotNull(content, Platform.getPlugin().getName() + " does not support the localization: messages_" + SimpleSettings.LOCALE_PREFIX
				+ ".yml (For custom locale, set the Locale to 'en' and edit your English file instead)");

		this.loadConfiguration(localePath);
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * The configuration version number, found in the "Version" key in the file.,
	 *
	 * Defaults to 1 if not set in the file.
	 */
	public static Integer VERSION = 1;

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
		setPathPrefix(null);

		if (isSetDefault("Version"))
			if ((VERSION = getInteger("Version")) != this.getConfigVersion())
				set("Version", this.getConfigVersion());
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

	/**
	 * Always keep the lang file up to date.
	 */
	@Override
	protected final boolean alwaysSaveOnLoad() {
		return true;
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
		public static Component NO_CONSOLE = CommonCore.colorize("&cYou may only use this command as a player");

		/**
		 * The message shown when console runs a command without specifying target player name
		 */
		public static Component CONSOLE_MISSING_PLAYER_NAME = CommonCore.colorize("When running from console, specify player name.");

		/**
		 * The message shown when there is a fatal error running this command
		 */
		public static Component COOLDOWN_WAIT = CommonCore.colorize("Wait {duration} second(s) before using this command again.");

		/**
		 * Keys below indicate an invalid action or input
		 */
		public static Component INVALID_ARGUMENT = CommonCore.colorize("Invalid argument. Run <gold>/{label} ? <red>for help.");
		public static Component INVALID_SUB_ARGUMENT = CommonCore.colorize("Invalid argument. Run '/{label} {0}' for help.");
		public static Component INVALID_ARGUMENT_MULTILINE = CommonCore.colorize("Invalid argument. Usage:");
		public static Component INVALID_TIME = CommonCore.colorize("Expected time such as '3 hours' or '15 minutes'. Got: '{input}'");
		public static Component INVALID_NUMBER = CommonCore.colorize("The number must be a whole or a decimal number. Got: '{input}'");
		public static Component INVALID_STRING = CommonCore.colorize("Invalid string. Got: '{input}'");
		public static Component INVALID_WORLD = CommonCore.colorize("Invalid world '{world}'. Available: {available}");
		public static Component INVALID_UUID = CommonCore.colorize("Invalid UUID '{uuid}'");
		public static Component INVALID_ENUM = CommonCore.colorize("No such {type} '{value}'. Available: {available}");

		/**
		 * The authors label
		 */
		public static Component LABEL_AUTHORS = CommonCore.colorize("Made by");

		/**
		 * The description label
		 */
		public static Component LABEL_DESCRIPTION = CommonCore.colorize("<red><bold>Description:");

		/**
		 * The optional arguments label
		 */
		public static Component LABEL_OPTIONAL_ARGS = CommonCore.colorize("optional arguments");

		/**
		 * The required arguments label
		 */
		public static Component LABEL_REQUIRED_ARGS = CommonCore.colorize("required arguments");

		/**
		 * The usage label
		 */
		public static Component LABEL_USAGE = CommonCore.colorize("&c&lUsage:");

		/**
		 * The help for label
		 */
		public static Component LABEL_HELP_FOR = CommonCore.colorize("Help for /{label}");

		/**
		 * The label shown when building subcommands
		 */
		public static Component LABEL_SUBCOMMAND_DESCRIPTION = CommonCore.colorize(" &f/{label} {sublabel} {usage+}{dash+}{description}");

		/**
		 * The keys below are shown as hover tooltip on /command help menu.
		 */
		public static Component HELP_TOOLTIP_DESCRIPTION = CommonCore.colorize("&7Description: &f{description}");
		public static Component HELP_TOOLTIP_PERMISSION = CommonCore.colorize("&7Permission: &f{permission}");
		public static Component HELP_TOOLTIP_USAGE = CommonCore.colorize("&7Usage: &f");

		/**
		 * The keys below are used in the {@link ReloadCommand}
		 */
		public static Component RELOAD_DESCRIPTION = CommonCore.colorize("Reload the configuration.");
		public static Component RELOAD_STARTED = CommonCore.colorize("Reloading plugin's data, please wait..");
		public static Component RELOAD_SUCCESS = CommonCore.colorize("&6{plugin_name} {plugin_version} has been reloaded.");
		public static Component RELOAD_FILE_LOAD_ERROR = CommonCore.colorize("&4Oups, &cthere was a problem loading files from your disk! See the console for more information. {plugin_name} has not been reloaded.");
		public static Component RELOAD_FAIL = CommonCore.colorize("&4Oups, &creloading failed! See the console for more information. Error: {error}");

		/**
		 * The message shown when there is a fatal error running this command
		 */
		public static Component ERROR = CommonCore.colorize("<red><bold>Oups! <red>The command failed :( Check the console and report the error.");

		/**
		 * The message shown when player has no permissions to view ANY subcommands in group command.
		 */
		public static Component HEADER_NO_SUBCOMMANDS = CommonCore.colorize("&cThere are no arguments for this command.");

		/**
		 * The message shown when player has no permissions to view ANY subcommands in group command.
		 */
		public static Component HEADER_NO_SUBCOMMANDS_PERMISSION = CommonCore.colorize("&cYou don't have permissions to view any subcommands.");

		/**
		 * The primary color shown in the ----- COMMAND ----- header
		 */
		public static CompChatColor HEADER_COLOR = CompChatColor.GOLD;

		/**
		 * The secondary color shown in the ----- COMMAND ----- header such as in /chc ?
		 */
		public static CompChatColor HEADER_SECONDARY_COLOR = CompChatColor.RED;

		/**
		 * The format of the header
		 */
		public static String HEADER_FORMAT = "&r\n{theme_color}&m<center>&r{theme_color} {title} &m\n&r";

		/**
		 * The center character of the format in case \<center\> is used
		 */
		public static String HEADER_CENTER_LETTER = "-";

		/**
		 * The padding of the header in case \<center\> is used
		 */
		public static Integer HEADER_CENTER_PADDING = 130;

		/**
		 * Key for when plugin is reloading
		 */
		public static Component RELOADING = CommonCore.colorize("reloading");

		/**
		 * Key for when plugin is disabled
		 */
		public static Component DISABLED = CommonCore.colorize("disabled");

		/**
		 * The message shown when plugin is reloading or was disabled and player attempts to run command
		 */
		public static Component CANNOT_USE_WHILE_NULL = CommonCore.colorize("<red>Cannot use this command while the plugin is {state}.");

		/**
		 * The message shown in SimpleCommand.findWorld()
		 */
		public static Component CANNOT_AUTODETECT_WORLD = CommonCore.colorize("Only living players can use ~ for their world!");

		/**
		 * The keys below are used in the {@link DebugCommand}
		 */
		public static Component DEBUG_DESCRIPTION = CommonCore.colorize("ZIP your settings for reporting bugs.");
		public static Component DEBUG_PREPARING = CommonCore.colorize("&6Preparing debug log...");
		public static Component DEBUG_SUCCESS = CommonCore.colorize("&2Successfuly copied {amount} file(s) to debug.zip. Your sensitive MySQL information has been removed from yml files. Please upload it via ufile.io and send it to us for review.");
		public static Component DEBUG_COPY_FAIL = CommonCore.colorize("&cCopying files failed on file {file} and it was stopped. See console for more information.");
		public static Component DEBUG_ZIP_FAIL = CommonCore.colorize("&cCreating a ZIP of your files failed, see console for more information. Please ZIP debug/ folder and send it to us via ufile.io manually.");

		/**
		 * The keys below are used in the {@link PermsCommand}
		 */
		public static Component PERMS_DESCRIPTION = CommonCore.colorize("List all permissions the plugin has.");
		public static Component PERMS_USAGE = CommonCore.colorize("[phrase]");
		public static Component PERMS_HEADER = CommonCore.colorize("Listing All {plugin_name} Permissions");
		public static Component PERMS_MAIN = CommonCore.colorize("Main");
		public static Component PERMS_PERMISSIONS = CommonCore.colorize("Permissions:");
		public static Component PERMS_TRUE_BY_DEFAULT = CommonCore.colorize("&7[true by default]");
		public static Component PERMS_INFO = CommonCore.colorize("&7Info: &f");
		public static Component PERMS_DEFAULT = CommonCore.colorize("&7Default? ");
		public static Component PERMS_APPLIED = CommonCore.colorize("&7Do you have it? ");
		public static Component PERMS_YES = CommonCore.colorize("&2yes");
		public static Component PERMS_NO = CommonCore.colorize("&cno");

		/**
		 * The keys below are used in RegionTool
		 */
		public static Component REGION_SET_PRIMARY = CommonCore.colorize("Set the primary region point.");
		public static Component REGION_SET_SECONDARY = CommonCore.colorize("Set the secondary region point.");

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Commands");

			if (isSetDefault("No_Console"))
				NO_CONSOLE = getComponent("No_Console");

			if (isSetDefault("Console_Missing_Player_Name"))
				CONSOLE_MISSING_PLAYER_NAME = getComponent("Console_Missing_Player_Name");

			if (isSetDefault("Cooldown_Wait"))
				COOLDOWN_WAIT = getComponent("Cooldown_Wait");

			if (isSetDefault("Invalid_Argument"))
				INVALID_ARGUMENT = getComponent("Invalid_Argument");

			if (isSetDefault("Invalid_Sub_Argument"))
				INVALID_SUB_ARGUMENT = getComponent("Invalid_Sub_Argument");

			if (isSetDefault("Invalid_Argument_Multiline"))
				INVALID_ARGUMENT_MULTILINE = getComponent("Invalid_Argument_Multiline");

			if (isSetDefault("Invalid_Time"))
				INVALID_TIME = getComponent("Invalid_Time");

			if (isSetDefault("Invalid_Number"))
				INVALID_NUMBER = getComponent("Invalid_Number");

			if (isSetDefault("Invalid_String"))
				INVALID_STRING = getComponent("Invalid_String");

			if (isSetDefault("Invalid_World"))
				INVALID_WORLD = getComponent("Invalid_World");

			if (isSetDefault("Invalid_UUID"))
				INVALID_UUID = getComponent("Invalid_UUID");

			if (isSetDefault("Invalid_Enum"))
				INVALID_ENUM = getComponent("Invalid_Enum");

			if (isSetDefault("Label_Authors"))
				LABEL_AUTHORS = getComponent("Label_Authors");

			if (isSetDefault("Label_Description"))
				LABEL_DESCRIPTION = getComponent("Label_Description");

			if (isSetDefault("Label_Optional_Args"))
				LABEL_OPTIONAL_ARGS = getComponent("Label_Optional_Args");

			if (isSetDefault("Label_Required_Args"))
				LABEL_REQUIRED_ARGS = getComponent("Label_Required_Args");

			if (isSetDefault("Label_Usage"))
				LABEL_USAGE = getComponent("Label_Usage");

			if (isSetDefault("Label_Help_For"))
				LABEL_HELP_FOR = getComponent("Label_Help_For");

			if (isSetDefault("Label_Subcommand_Description"))
				LABEL_SUBCOMMAND_DESCRIPTION = getComponent("Label_Subcommand_Description");

			if (isSetDefault("Help_Tooltip_Description"))
				HELP_TOOLTIP_DESCRIPTION = getComponent("Help_Tooltip_Description");

			if (isSetDefault("Help_Tooltip_Permission"))
				HELP_TOOLTIP_PERMISSION = getComponent("Help_Tooltip_Permission");

			if (isSetDefault("Help_Tooltip_Usage"))
				HELP_TOOLTIP_USAGE = getComponent("Help_Tooltip_Usage");

			if (isSetDefault("Reload_Description"))
				RELOAD_DESCRIPTION = getComponent("Reload_Description");

			if (isSetDefault("Reload_Started"))
				RELOAD_STARTED = getComponent("Reload_Started");

			if (isSetDefault("Reload_Success"))
				RELOAD_SUCCESS = getComponent("Reload_Success");

			if (isSetDefault("Reload_File_Load_Error"))
				RELOAD_FILE_LOAD_ERROR = getComponent("Reload_File_Load_Error");

			if (isSetDefault("Reload_Fail"))
				RELOAD_FAIL = getComponent("Reload_Fail");

			if (isSetDefault("Error"))
				ERROR = getComponent("Error");

			if (isSetDefault("Header_No_Subcommands"))
				HEADER_NO_SUBCOMMANDS = getComponent("Header_No_Subcommands");

			if (isSetDefault("Header_No_Subcommands_Permission"))
				HEADER_NO_SUBCOMMANDS_PERMISSION = getComponent("Header_No_Subcommands_Permission");

			if (isSetDefault("Header_Color"))
				HEADER_COLOR = get("Header_Color", CompChatColor.class);

			if (isSetDefault("Header_Secondary_Color"))
				HEADER_SECONDARY_COLOR = get("Header_Secondary_Color", CompChatColor.class);

			if (isSetDefault("Header_Format"))
				HEADER_FORMAT = getString("Header_Format");

			if (isSetDefault("Header_Center_Letter")) {
				HEADER_CENTER_LETTER = getString("Header_Center_Letter");

				ValidCore.checkBoolean(HEADER_CENTER_LETTER.length() == 1, "Header_Center_Letter must only have 1 letter, not " + HEADER_CENTER_LETTER.length() + ":" + HEADER_CENTER_LETTER);
			}

			if (isSetDefault("Header_Center_Padding"))
				HEADER_CENTER_PADDING = getInteger("Header_Center_Padding");

			if (isSet("Reloading"))
				RELOADING = getComponent("Reloading");

			if (isSet("Disabled"))
				DISABLED = getComponent("Disabled");

			if (isSet("Use_While_Null"))
				CANNOT_USE_WHILE_NULL = getComponent("Use_While_Null");

			if (isSet("Cannot_Autodetect_World"))
				CANNOT_AUTODETECT_WORLD = getComponent("Cannot_Autodetect_World");

			if (isSetDefault("Debug_Description"))
				DEBUG_DESCRIPTION = getComponent("Debug_Description");

			if (isSetDefault("Debug_Preparing"))
				DEBUG_PREPARING = getComponent("Debug_Preparing");

			if (isSetDefault("Debug_Success"))
				DEBUG_SUCCESS = getComponent("Debug_Success");

			if (isSetDefault("Debug_Copy_Fail"))
				DEBUG_COPY_FAIL = getComponent("Debug_Copy_Fail");

			if (isSetDefault("Debug_Zip_Fail"))
				DEBUG_ZIP_FAIL = getComponent("Debug_Zip_Fail");

			if (isSetDefault("Perms_Description"))
				PERMS_DESCRIPTION = getComponent("Perms_Description");

			if (isSetDefault("Perms_Usage"))
				PERMS_USAGE = getComponent("Perms_Usage");

			if (isSetDefault("Perms_Header"))
				PERMS_HEADER = getComponent("Perms_Header");

			if (isSetDefault("Perms_Main"))
				PERMS_MAIN = getComponent("Perms_Main");

			if (isSetDefault("Perms_Permissions"))
				PERMS_PERMISSIONS = getComponent("Perms_Permissions");

			if (isSetDefault("Perms_True_By_Default"))
				PERMS_TRUE_BY_DEFAULT = getComponent("Perms_True_By_Default");

			if (isSetDefault("Perms_Info"))
				PERMS_INFO = getComponent("Perms_Info");

			if (isSetDefault("Perms_Default"))
				PERMS_DEFAULT = getComponent("Perms_Default");

			if (isSetDefault("Perms_Applied"))
				PERMS_APPLIED = getComponent("Perms_Applied");

			if (isSetDefault("Perms_Yes"))
				PERMS_YES = getComponent("Perms_Yes");

			if (isSetDefault("Perms_No"))
				PERMS_NO = getComponent("Perms_No");

			if (isSetDefault("Region_Set_Primary"))
				REGION_SET_PRIMARY = getComponent("Region_Set_Primary");

			if (isSetDefault("Region_Set_Secondary"))
				REGION_SET_SECONDARY = getComponent("Region_Set_Secondary");
		}
	}

	/**
	 * Strings related to player-server conversation waiting for his chat input
	 */
	public static final class Conversation {

		/**
		 * The key used when the player wants to converse but he is not conversing.
		 */
		public static Component CONVERSATION_NOT_CONVERSING = CommonCore.colorize("You must be conversing with the server!");

		/**
		 * Called when console attempts to start conversing
		 */
		public static String CONVERSATION_REQUIRES_PLAYER = "Only players may enter this conversation.";

		/**
		 * Called in the try-catch handling when an error occurs
		 */
		public static String CONVERSATION_ERROR = "&cOups! There was a problem in this conversation! Please contact the administrator to review the console for details.";

		/**
		 * Called in SimplePrompt
		 */
		public static String CONVERSATION_CANCELLED = "Your pending chat answer has been canceled.";

		/**
		 * Called in SimplePrompt
		 */
		public static String CONVERSATION_CANCELLED_INACTIVE = "Your pending chat answer has been canceled because you were inactive.";

		private static void init() {
			setPathPrefix("Conversation");

			if (isSetDefault("Not_Conversing"))
				CONVERSATION_NOT_CONVERSING = getComponent("Not_Conversing");

			if (isSetDefault("Requires_Player"))
				CONVERSATION_REQUIRES_PLAYER = getString("Requires_Player");

			if (isSetDefault("Conversation_Error"))
				CONVERSATION_ERROR = getString("Error");

			if (isSetDefault("Conversation_Cancelled"))
				CONVERSATION_CANCELLED = getString("Conversation_Cancelled");

			if (isSetDefault("Conversation_Cancelled_Inactive"))
				CONVERSATION_CANCELLED_INACTIVE = getString("Conversation_Cancelled_Inactive");
		}
	}

	/**
	 * Key related to players
	 */
	public static final class Player {

		/**
		 * Message shown when the player is not online on this server
		 */
		public static Component NOT_ONLINE = CommonCore.colorize("&cPlayer {player} &cis not online on this server.");

		/**
		 * Message shown when Bukkit#getOfflinePlayer(String) returns that the player has not played before
		 */
		public static Component NOT_PLAYED_BEFORE = CommonCore.colorize("&cPlayer {player} &chas not played before or we could not locate his disk data.");

		/**
		 * Message shown the an offline player is returned null from a given UUID.
		 */
		public static Component INVALID_UUID = CommonCore.colorize("&cCould not find a player from UUID {uuid}.");

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Player");

			if (isSetDefault("Not_Online"))
				NOT_ONLINE = getComponent("Not_Online");

			if (isSetDefault("Not_Played_Before"))
				NOT_PLAYED_BEFORE = getComponent("Not_Played_Before");

			if (isSetDefault("Invalid_UUID"))
				INVALID_UUID = getComponent("Invalid_UUID");
		}
	}

	/**
	 * Keys related to ChatPaginator
	 */
	public static final class Pages {

		public static String NO_PAGE_NUMBER = "&cPlease specify the page number for this command.";
		public static String NO_PAGES = "There are no results to list.";
		public static String NO_PAGE = "Pages do not contain the given page number.";
		public static String INVALID_PAGE = "&cYour input '{input}' is not a valid number.";
		public static String GO_TO_PAGE = "&7Go to page {page}";
		public static String GO_TO_FIRST_PAGE = "&7Go to the first page";
		public static String GO_TO_LAST_PAGE = "&7Go to the last page";
		public static String[] TOOLTIP = {
				"&7You can also navigate using the",
				"&7hidden /#flp <page> command."
		};

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Pages");

			if (isSetDefault("No_Page_Number"))
				NO_PAGE_NUMBER = getString("No_Page_Number");

			if (isSetDefault("No_Pages"))
				NO_PAGES = getString("No_Pages");

			if (isSetDefault("No_Page"))
				NO_PAGE = getString("No_Page");

			if (isSetDefault("Invalid_Page"))
				INVALID_PAGE = getString("Invalid_Page");

			if (isSetDefault("Go_To_Page"))
				GO_TO_PAGE = getString("Go_To_Page");

			if (isSetDefault("Go_To_First_Page"))
				GO_TO_FIRST_PAGE = getString("Go_To_First_Page");

			if (isSetDefault("Go_To_Last_Page"))
				GO_TO_LAST_PAGE = getString("Go_To_Last_Page");

			if (isSetDefault("Tooltip"))
				TOOLTIP = CommonCore.toArray(getStringList("Tooltip"));
		}
	}

	/**
	 * Keys related to the GUI system
	 */
	public static final class Menu {

		/**
		 * Message shown when the player is not online on this server
		 */
		public static String ITEM_DELETED = "&2The {item} has been deleted.";

		/**
		 * Message shown when the player tries to open menu, but has an ongoing conversation.
		 */
		public static String CANNOT_OPEN_DURING_CONVERSATION = "&cType 'exit' to quit your conversation before opening menu.";

		/**
		 * Message shown on error
		 */
		public static String ERROR = "&cOups! There was a problem with this menu! Please contact the administrator to review the console for details.";

		/**
		 * Keys related to menu pagination
		 */
		public static String PAGE_PREVIOUS = "&8<< &fPage {page}";
		public static String PAGE_NEXT = "Page {page} &8>>";
		public static String PAGE_FIRST = "&7First Page";
		public static String PAGE_LAST = "&7Last Page";

		/**
		 * Keys related to menu titles and tooltips
		 */
		public static String TITLE_TOOLS = "Tools Menu";
		public static String TOOLTIP_INFO = "&fMenu Information";
		public static String BUTTON_RETURN_TITLE = "&4&lReturn";
		public static String[] BUTTON_RETURN_LORE = { "", "Return back." };

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Menu");

			if (isSetDefault("Item_Deleted"))
				ITEM_DELETED = getString("Item_Deleted");

			if (isSetDefault("Cannot_Open_During_Conversation"))
				CANNOT_OPEN_DURING_CONVERSATION = getString("Cannot_Open_During_Conversation");

			if (isSetDefault("Error"))
				ERROR = getString("Error");

			if (isSetDefault("Page_Previous"))
				PAGE_PREVIOUS = getString("Page_Previous");

			if (isSetDefault("Page_Next"))
				PAGE_NEXT = getString("Page_Next");

			if (isSetDefault("Page_First"))
				PAGE_FIRST = getString("Page_First");

			if (isSetDefault("Page_Last"))
				PAGE_LAST = getString("Page_Last");

			if (isSetDefault("Title_Tools"))
				TITLE_TOOLS = getString("Title_Tools");

			if (isSetDefault("Tooltip_Info"))
				TOOLTIP_INFO = getString("Tooltip_Info");

			if (isSetDefault("Button_Return_Title"))
				BUTTON_RETURN_TITLE = getString("Button_Return_Title");

			if (isSetDefault("Button_Return_Lore"))
				BUTTON_RETURN_LORE = CommonCore.toArray(getStringList("Button_Return_Lore"));
		}
	}

	/**
	 * Keys related to tools
	 */
	public static final class Tool {

		/**
		 * The message shown when a tool errors out.
		 */
		public static String ERROR = "&cOups! There was a problem with this tool! Please contact the administrator to review the console for details.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Tool");

			if (isSetDefault("Error"))
				ERROR = getString("Error");
		}
	}

	/**
	 * Keys related to cases
	 */
	public static class Cases {

		public static AccusativeHelper SECOND = AccusativeHelper.of("second", "seconds");
		public static AccusativeHelper MINUTE = AccusativeHelper.of("minute", "minutes");
		public static AccusativeHelper HOUR = AccusativeHelper.of("hour", "hours");
		public static AccusativeHelper DAY = AccusativeHelper.of("day", "days");
		public static AccusativeHelper WEEK = AccusativeHelper.of("week", "weeks");
		public static AccusativeHelper MONTH = AccusativeHelper.of("month", "months");
		public static AccusativeHelper YEAR = AccusativeHelper.of("year", "years");

		private static void init() {
			setPathPrefix("Cases");

			if (isSetDefault("Second"))
				SECOND = getCasus("Second");

			if (isSetDefault("Minute"))
				MINUTE = getCasus("Minute");

			if (isSetDefault("Hour"))
				HOUR = getCasus("Hour");

			if (isSetDefault("Day"))
				DAY = getCasus("Day");

			if (isSetDefault("Week"))
				WEEK = getCasus("Week");

			if (isSetDefault("Month"))
				MONTH = getCasus("Month");

			if (isSetDefault("Year"))
				YEAR = getCasus("Year");
		}
	}

	/**
	 * Denotes the "none" message
	 */
	public static String NONE = "None";

	/**
	 * The message for player if they lack a permission.
	 */
	public static Component NO_PERMISSION = CommonCore.colorize("<red>Insufficient permission ({permission}).");

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
	 * The message when a section is missing from data file (the one ending in .db) (typically we use
	 * this file to store serialized values such as arenas from minigame plugins).
	 */
	public static String DATA_MISSING = "&c{name} lacks database information! Please only create {type} in-game! Skipping..";

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		setPathPrefix(null);
		ValidCore.checkBoolean(!localizationClassCalled, "Localization class already loaded!");

		if (isSetDefault("No_Permission"))
			NO_PERMISSION = getComponent("No_Permission");

		if (isSetDefault("Server_Prefix"))
			SERVER_PREFIX = getString("Server_Prefix");

		if (isSetDefault("Console_Name"))
			CONSOLE_NAME = getString("Console_Name");

		if (isSetDefault("Data_Missing"))
			DATA_MISSING = getString("Data_Missing");

		if (isSetDefault("None"))
			NONE = getString("None");

		if (isSetDefault("Prefix.Announce"))
			MessengerCore.setAnnouncePrefix(getComponent("Prefix.Announce"));

		if (isSetDefault("Prefix.Error"))
			MessengerCore.setErrorPrefix(getComponent("Prefix.Error"));

		if (isSetDefault("Prefix.Info"))
			MessengerCore.setInfoPrefix(getComponent("Prefix.Info"));

		if (isSetDefault("Prefix.Question"))
			MessengerCore.setQuestionPrefix(getComponent("Prefix.Question"));

		if (isSetDefault("Prefix.Success"))
			MessengerCore.setSuccessPrefix(getComponent("Prefix.Success"));

		if (isSetDefault("Prefix.Warn"))
			MessengerCore.setWarnPrefix(getComponent("Prefix.Warn"));

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
