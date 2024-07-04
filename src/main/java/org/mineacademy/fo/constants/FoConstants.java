package org.mineacademy.fo.constants;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * Stores constants for this plugin
 */
public final class FoConstants {

	/**
	 * Represents a UUID consisting of 0's only
	 */
	public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	public static final class File {

		/**
		 * The name of our settings file
		 */
		public static final String SETTINGS = "settings.yml";

		/**
		 * The error file created automatically to log errors to
		 */
		public static final String ERRORS = "error.log";

		/**
		 * The debug file to log debug messages to
		 */
		public static final String DEBUG = "debug.log";

		/**
		 * The data file (uses YAML) for saving various data
		 */
		public static final String DATA = "data.db";

		/**
		 * Files related to the ChatControl plugin
		 */
		public static final class ChatControl {

			/**
			 * The command-spy.log file in logs/ folder
			 */
			public static final String COMMAND_SPY = "logs/command-spy.log";

			/**
			 * The chat log file in logs/ folder
			 */
			public static final String CHAT_LOG = "logs/chat.log";

			/**
			 * The admin log in log/s folder
			 */
			public static final String ADMIN_CHAT = "logs/admin-chat.log";

			/**
			 * The bungee chat log file in logs/ folder
			 */
			public static final String BUNGEE_CHAT = "logs/bungee-chat.log";

			/**
			 * The rules log file in logs/ folder
			 */
			public static final String RULES_LOG = "logs/rules.log";

			/**
			 * The console log file in logs/ folder
			 */
			public static final String CONSOLE_LOG = "logs/console.log";

			/**
			 * The file logging channels joins and leaves in logs/ folder
			 */
			public static final String CHANNEL_JOINS = "logs/channel-joins.log";
		}
	}

	public static final class Header {

		/**
		 * The header for data file
		 *
		 * Use YamlConfig/setHeader() to override this.
		 */
		public static final String[] DATA_FILE = {
				"",
				"This file stores various data you create via the plugin.",
				"",
				" ** THE FILE IS MACHINE GENERATED. PLEASE DO NOT EDIT **",
				""
		};

		/**
		 * The header for a variable file that does not have a default prototype.
		 */
		public static final String[] VARIABLE_FILE = {
				"-------------------------------------------------------------------------------------------------",
				SimplePlugin.getNamed() + " supports dynamic, high performance JavaScript variables! They will",
				"automatically be used when calling Variables#replace for your messages.",
				"",
				"Because variables return a JavaScript value, you can sneak in code to play sounds or spawn",
				"monsters directly in your variable instead of it just displaying text!",
				"",
				"For example of how variables can be used, see our plugin ChatControl's wikipedia article:",
				"https://github.com/kangarko/ChatControl-Red/wiki/JavaScript-Variables",
				" -------------------------------------------------------------------------------------------------",
		};
	}

	public static final class NBT {

		/**
		 * An internal metadata tag the player gets when he opens the menu.
		 *
		 * <p>
		 * Used in {@link Menu#getMenu(Player)}
		 */
		public static final String TAG_MENU_CURRENT = SimplePlugin.getNamed() + "_Menu";

		/**
		 * An internal metadata tag the player gets when he opens another menu.
		 *
		 * <p>
		 * Used in {@link Menu#getPreviousMenu(Player)}
		 */
		public static final String TAG_MENU_PREVIOUS = SimplePlugin.getNamed() + "_Previous_Menu";

		/**
		 * An internal metadata tag the player gets when he closes our menu so you can
		 * reopen last closed menu manually.
		 *
		 * <p>
		 * Used in {@link Menu#getLastClosedMenu(Player)}
		 */
		public static final String TAG_MENU_LAST_CLOSED = SimplePlugin.getNamed() + "_Last_Closed_Menu";
	}
}
