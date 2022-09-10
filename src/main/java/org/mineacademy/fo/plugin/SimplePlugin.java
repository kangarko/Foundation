/**
 * (c) 2013 - 2019 - All rights reserved.
 * <p>
 * Do not share, copy, reproduce or sell any part of this library
 * unless you have written permission from MineAcademy.org.
 * All infringements will be prosecuted.
 * <p>
 * If you are the personal owner of the MineAcademy.org End User License
 * then you may use it for your own use in plugins but not for any other purpose.
 */
package org.mineacademy.fo.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.event.SimpleListener;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuListener;
import org.mineacademy.fo.menu.tool.ToolsListener;
import org.mineacademy.fo.metrics.Metrics;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.FolderWatcher;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.JavaScriptExecutor;
import org.mineacademy.fo.model.SimpleHologram;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.SpigotUpdater;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.FileConfig;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.visual.BlockVisualizer;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a basic Java plugin using enhanced library functionality,
 * implementing a listener for easy use
 */
public abstract class SimplePlugin extends JavaPlugin implements Listener {

	// ----------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------

	/**
	 * The instance of this plugin
	 */
	private static volatile SimplePlugin instance;

	/**
	 * Shortcut for getDescription().getVersion()
	 */
	@Getter
	private static String version;

	/**
	 * Shortcut for getName()
	 */
	@Getter
	private static String named;

	/**
	 * Shortcut for getFile()
	 */
	@Getter
	private static File source;

	/**
	 * Shortcut for getDataFolder()
	 */
	@Getter
	private static File data;

	/**
	 * An internal flag to indicate that the plugin is being reloaded.
	 */
	@Getter
	private static volatile boolean reloading = false;

	/**
	 * Returns the instance of {@link SimplePlugin}.
	 * <p>
	 * It is recommended to override this in your own {@link SimplePlugin}
	 * implementation so you will get the instance of that, directly.
	 *
	 * @return this instance
	 */
	public static SimplePlugin getInstance() {
		if (instance == null) {
			try {
				instance = JavaPlugin.getPlugin(SimplePlugin.class);

			} catch (final IllegalStateException ex) {
				if (Bukkit.getPluginManager().getPlugin("PlugMan") != null)
					Bukkit.getLogger().severe("Failed to get instance of the plugin, if you reloaded using PlugMan you need to do a clean restart instead.");

				throw ex;
			}

			Objects.requireNonNull(instance, "Cannot get a new instance! Have you reloaded?");
		}

		return instance;
	}

	/**
	 * Get if the instance that is used across the library has been set. Normally it
	 * is always set, except for testing.
	 *
	 * @return if the instance has been set.
	 */
	public static final boolean hasInstance() {
		return instance != null;
	}

	// ----------------------------------------------------------------------------------------
	// Instance specific
	// ----------------------------------------------------------------------------------------

	/**
	 * For your convenience, event listeners and timed tasks may be set here to stop/unregister
	 * them automatically on reload
	 */
	private final Reloadables reloadables = new Reloadables();

	/**
	 * An internal flag to indicate whether we are calling the {@link #onReloadablesStart()}
	 * block. We register things using {@link #reloadables} during this block
	 */
	private boolean startingReloadables = false;

	/**
	 * Internal boolean indicating if we can proceed to loading the plugin.
	 */
	private final boolean canLoad = true;

	/**
	 * A temporary main command to be set in {@link #setMainCommand(SimpleCommandGroup)}
	 * automatically by us.
	 */
	private SimpleCommandGroup mainCommand;

	/**
	 * A temporary bungee listener, see {@link #setBungeeCord(BungeeListener)}
	 * set automatically by us.
	 */
	private BungeeListener bungeeListener;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		if (MinecraftVersion.olderThan(V.v1_4) && !ReflectionUtil.isClassAvailable("org.bukkit.Sound")) {
			Bukkit.getLogger().severe("Ancient MC version detected, please follow install steps here: https://mineacademy.org/oldmcsupport");
			Bukkit.getLogger().severe("Please note that many features won't work and due to time constraints we can't provide support for such old Minecraft versions.");

			throw new RuntimeException("Ancient MC detected, see above for installation steps.");
		}

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	@Override
	public final void onLoad() {

		// Set the instance
		try {
			getInstance();

		} catch (final Throwable ex) {
			if (MinecraftVersion.olderThan(V.v1_7))
				instance = this; // Workaround
			else
				throw ex;
		}

		// Cache results for best performance
		version = instance.getDescription().getVersion();
		named = instance.getDataFolder().getName();
		source = instance.getFile();
		data = instance.getDataFolder();

		final String version = Bukkit.getVersion();

		if (this.suggestPaper() && !version.contains("Paper")
				&& !version.contains("Purpur")
				&& !version.contains("NachoSpigot")
				&& !version.contains("-Spigot")
				&& MinecraftVersion.atLeast(V.v1_8)) {
			Bukkit.getLogger().severe(Common.consoleLine());
			Bukkit.getLogger().warning("Warning about " + named + ": You're not using Paper!");
			Bukkit.getLogger().warning("Detected: " + version);
			Bukkit.getLogger().warning("");
			Bukkit.getLogger().warning("Third party forks are known to alter server in unwanted");
			Bukkit.getLogger().warning("ways. If you have issues with " + named + " use Paper");
			Bukkit.getLogger().warning("from PaperMC.io otherwise you may not receive our support.");
			Bukkit.getLogger().severe(Common.consoleLine());
		}

		// Load libraries where Spigot does not do this automatically
		this.loadLibraries();

		// Call parent
		this.onPluginLoad();
	}

	@Override
	public final void onEnable() {

		// Disabled upstream
		if (!this.canLoad) {
			Bukkit.getLogger().severe("Not loading, the plugin is disabled (look for console errors above)");

			return;
		}

		// Solve reloading issues with PlugMan
		for (final StackTraceElement element : new Throwable().getStackTrace())
			if (element.toString().contains("com.rylinaux.plugman.util.PluginUtil.load")) {
				Common.warning("Detected PlugMan reload, which is poorly designed. "
						+ "It causes Bukkit not able to get our plugin from a static initializer."
						+ " It may or may not run. Use our own reload command or do a clean restart!");

				break;
			}

		// Check if Foundation is correctly moved
		this.checkShading();

		if (!this.isEnabled())
			return;

		// Before all, check if necessary libraries and the minimum required MC version
		if (!this.checkLibraries0() || !this.checkServerVersions0()) {
			this.setEnabled(false);

			return;
		}

		// Load debug mode early
		Debugger.detectDebugMode();

		// Print startup logo early before onPluginPreStart
		// Disable logging prefix if logo is set
		if (this.getStartupLogo() != null) {
			final String oldLogPrefix = Common.getLogPrefix();

			Common.setLogPrefix("");
			Common.log(this.getStartupLogo());
			Common.setLogPrefix(oldLogPrefix);
		}

		// Inject server-name to newer MC versions that lack it
		Remain.injectServerName();

		// Load our dependency system
		try {
			HookManager.loadDependencies();

		} catch (final Throwable throwable) {
			Common.throwError(throwable, "Error while loading " + this.getDataFolder().getName() + " dependencies!");
		}

		// Return if plugin pre start indicated a fatal problem
		if (!this.isEnabled())
			return;

		try {

			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------

			final Messenger messenger = this.getServer().getMessenger();
			if (!messenger.isOutgoingChannelRegistered(this, "BungeeCord"))
				messenger.registerOutgoingPluginChannel(this, "BungeeCord");

			// Hide plugin name before console messages
			final String oldLogPrefix = Common.getLogPrefix();
			Common.setLogPrefix("");

			this.startingReloadables = true;

			try {
				AutoRegisterScanner.scanAndRegister();

			} catch (final Throwable t) {
				Remain.sneaky(t);

				return;
			}

			this.onReloadablesStart();

			this.startingReloadables = false;

			this.onPluginStart();
			// --------------------------------------------

			// Return if plugin start indicated a fatal problem
			if (!this.isEnabled())
				return;

			// Start update check
			if (this.getUpdateCheck() != null)
				this.getUpdateCheck().run();

			// Register our listeners
			this.registerEvents(this);
			this.registerEvents(new MenuListener());
			this.registerEvents(new FoundationListener());

			if (this.areToolsEnabled())
				this.registerEvents(new ToolsListener());

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded()) {
				final DiscordListener.DiscordListenerImpl discord = DiscordListener.DiscordListenerImpl.getInstance();

				discord.resubscribe();
				discord.registerHook();

				this.reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			// Prepare Nashorn engine
			JavaScriptExecutor.run("");

			// Finish off by starting metrics (currently bStats)
			if (this.getMetricsPluginId() != -1)
				new Metrics(this.getMetricsPluginId());

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			// Finally, place plugin name before console messages after plugin has (re)loaded
			Common.runLater(() -> Common.setLogPrefix(oldLogPrefix));

		} catch (final Throwable t) {
			this.displayError0(t);
		}
	}

	/*
	 * Loads libraries from plugin.yml or from getLibraries()
	 */
	private void loadLibraries() {
		final int javaVersion = getJavaVersion();
		final List<Library> libraries = new ArrayList<>();

		// Force add md_5 bungee chat since it's needed
		if (!ReflectionUtil.isClassAvailable("net.md_5.bungee.api.ChatColor"))
			libraries.add(Library.fromMavenRepo("net.md-5", "bungeecord-chat", "1.16-R0.4"));

		if (MinecraftVersion.olderThan(V.v1_16)) {
			final YamlConfiguration pluginFile = new YamlConfiguration();

			// We have to load it using the legacy way for ancient MC versions
			try {
				pluginFile.loadFromString(String.join("\n", FileUtil.getInternalFileContent("plugin.yml")));

			} catch (final Throwable t) {
				throw new RuntimeException(t);
			}

			for (final String libraryPath : pluginFile.getStringList("legacy-libraries")) {
				if (javaVersion < 15 && libraryPath.contains("org.openjdk.nashorn:nashorn-core"))
					continue;

				final Library library = Library.fromMavenRepo(libraryPath);

				libraries.add(library);
			}

			// Load normally
			if (!libraries.isEmpty() && javaVersion >= 9)
				Common.logFramed(
						"Warning: Unsupported Java version: " + javaVersion + " for your server",
						"version! Minecraft " + MinecraftVersion.getServerVersion() + " was designed for Java 8",
						"and we're unable unable to load 'legacy-libraries'",
						"that this plugin uses:",
						Common.join(libraries, ", ", Library::getGroupId),
						"",
						"To fix this, start your server using Java 8 or",
						"upgrade to Minecraft 1.16 or greater.");

			else
				for (final Library library : libraries)
					library.load();
		}

		// Always load user-defined libraries
		final List<Library> manualLibraries = this.getLibraries();

		// But only on Java 8 (for now)
		if (!manualLibraries.isEmpty() && javaVersion > 8)
			Common.warning("The getLibraries() feature only supports Java 8 for now and does not work on Java " + javaVersion + ". To load the following libraries, "
					+ "install Java 8 or upgrade to Minecraft 16 where you use the 'libraries' feature of plugin.yml to load. Skipping loading: " + manualLibraries);

		else
			methodLibraryLoader:
			for (final Library library : manualLibraries) {

				// Detect conflicts
				for (final Library otherLibrary : libraries)
					if (library.getArtifactId().equals(otherLibrary.getArtifactId()) && library.getGroupId().equals(otherLibrary.getGroupId())) {
						Common.warning("Detected library conflict: '" + library.getGroupId() + "." + library.getArtifactId() + "' is defined both in getLibraries() and plugin.yml! "
								+ "We'll prefer the version from plugin.yml, if you want to use the one from getLibraries() then remove it from your plugin.yml file.");

						continue methodLibraryLoader;
					}

				library.load();
			}
	}

	/**
	 * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		String version = System.getProperty("java.version");

		if (version.startsWith("1."))
			version = version.substring(2, 3);

		else {
			final int dot = version.indexOf(".");

			if (dot != -1)
				version = version.substring(0, dot);
		}

		if (version.contains("-"))
			version = version.split("\\-")[0];

		return Integer.parseInt(version);
	}

	/**
	 * A list of libraries to automatically download and load.
	 *
	 * **REQUIRES JAVA 8 FOR THE TIME BEING**
	 *
	 * @deprecated requires Java 8 thus only works on Minecraft 1.16 or lower with such Java version installed
	 * @return
	 */
	@Deprecated
	protected List<Library> getLibraries() {
		return new ArrayList<>();
	}

	/**
	 * Register a simple bungee class as a custom bungeecord listener.
	 *
	 * DO NOT use this if you only have that one field there with a getter, we already register it automatically,
	 * this method is intended to be used if you have multiple fields there and want to register multiple channels.
	 * Then you just call this method and parse the field into it from your onReloadablesStart method.
	 */
	protected final void registerBungeeCord(@NonNull BungeeListener bungee) {
		final Messenger messenger = this.getServer().getMessenger();

		messenger.registerIncomingPluginChannel(this, bungee.getChannel(), bungee);
		messenger.registerOutgoingPluginChannel(this, bungee.getChannel());

		this.reloadables.registerEvents(bungee);
	}

	/**
	 * A dirty way of checking if Foundation has been shaded correctly
	 */
	private void checkShading() {
		try {
			throw new ShadingException();
		} catch (final Throwable t) {
		}
	}

	/**
	 * The exception enabling us to check if for some reason {@link SimplePlugin}'s instance
	 * does not match this class' instance, which is most likely caused by wrong repackaging
	 * or no repackaging at all (two plugins using Foundation must both have different packages
	 * for their own Foundation version).
	 * <p>
	 * Or, this is caused by a PlugMan, and we have no mercy for that.
	 */
	private class ShadingException extends Throwable {
		private static final long serialVersionUID = 1L;

		public ShadingException() {
			if (!SimplePlugin.getNamed().equals(SimplePlugin.this.getDescription().getName())) {
				Bukkit.getLogger().severe(Common.consoleLine());
				Bukkit.getLogger().severe("We have a class path problem in the Foundation library");
				Bukkit.getLogger().severe("preventing " + SimplePlugin.this.getDescription().getName() + " from loading correctly!");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("This is likely caused by two plugins having the");
				Bukkit.getLogger().severe("same Foundation library paths - make sure you");
				Bukkit.getLogger().severe("relocale the package! If you are testing using");
				Bukkit.getLogger().severe("Ant, only test one plugin at the time.");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("Possible cause: " + SimplePlugin.getNamed());
				Bukkit.getLogger().severe("Foundation package: " + SimplePlugin.class.getPackage().getName());
				Bukkit.getLogger().severe(Common.consoleLine());

				throw new FoException("Shading exception, see above for details.");
			}
		}
	}

	/**
	 * Check if both md5 chat and gson libraries are present,
	 * or suggest an additional plugin to fix their lack
	 *
	 * @return
	 */
	private boolean checkLibraries0() {

		boolean md_5 = false;
		boolean gson = false;

		try {
			Class.forName("net.md_5.bungee.api.chat.BaseComponent");
			md_5 = true;
		} catch (final ClassNotFoundException ex) {
		}

		try {
			Class.forName("com.google.gson.JsonSyntaxException");
			gson = true;

		} catch (final ClassNotFoundException ex) {
		}

		if (!md_5 || !gson) {
			Bukkit.getLogger().severe(Common.consoleLine());
			Bukkit.getLogger().severe("Your Minecraft version (" + MinecraftVersion.getCurrent() + ")");
			Bukkit.getLogger().severe("lacks libraries " + this.getDataFolder().getName() + " needs:");
			Bukkit.getLogger().severe("JSON Chat (by md_5) found: " + md_5);
			Bukkit.getLogger().severe("Gson (by Google) found: " + gson);
			Bukkit.getLogger().severe(" ");
			Bukkit.getLogger().severe("To fix that, please install BungeeChatAPI:");
			Bukkit.getLogger().severe("https://mineacademy.org/plugins/#misc");
			Bukkit.getLogger().severe(Common.consoleLine());
		}

		return true;
	}

	/**
	 * Check if the minimum required MC version is installed
	 *
	 * @return
	 */
	private boolean checkServerVersions0() {

		// Call the static block to test compatibility early
		if (!MinecraftVersion.getCurrent().isTested())
			Common.logFramed(
					"*** WARNING ***",
					"Your Minecraft version " + MinecraftVersion.getCurrent() + " has not yet",
					"been officialy tested with the Foundation,",
					"the library that " + SimplePlugin.getNamed() + " plugin uses.",
					"",
					"Loading the plugin at your own risk...",
					Common.consoleLine());

		// Check min version
		final V minimumVersion = this.getMinimumVersion();

		if (minimumVersion != null && MinecraftVersion.olderThan(minimumVersion)) {
			Common.logFramed(false,
					this.getDataFolder().getName() + " requires Minecraft " + minimumVersion + " or newer to run.",
					"Please upgrade your server.");

			return false;
		}

		// Check max version
		final V maximumVersion = this.getMaximumVersion();

		if (maximumVersion != null && MinecraftVersion.newerThan(maximumVersion)) {
			Common.logFramed(false,
					this.getDataFolder().getName() + " requires Minecraft " + maximumVersion + " or older to run.",
					"Please downgrade your server or",
					"wait for the new version.");

			return false;
		}

		return true;
	}

	/**
	 * Handles various startup problems
	 *
	 * @param throwable
	 */
	protected final void displayError0(Throwable throwable) {
		Debugger.printStackTrace(throwable);

		final boolean privateDistro = this.getServer().getBukkitVersion().contains("1.8.8-R0.2");

		Common.log(
				"&4    ___                  _ ",
				"&4   / _ \\  ___  _ __  ___| |",
				"&4  | | | |/ _ \\| '_ \\/ __| |",
				"&4  | |_| | (_) | |_) \\__ \\_|",
				"&4   \\___/ \\___/| .__/|___(_)",
				"&4             |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + this.getDescription().getName() + " v" + this.getDescription().getVersion() + ", plugin is disabled!",
				privateDistro ? null : " &cRunning on " + this.getServer().getBukkitVersion() + " (" + MinecraftVersion.getServerVersion() + ") & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		if (throwable instanceof InvalidConfigurationException) {
			Common.log(" &cSeems like your config is not a valid YAML.");
			Common.log(" &cUse online services like");
			Common.log(" &chttp://yaml-online-parser.appspot.com/");
			Common.log(" &cto check for syntax errors!");

		} else if (throwable instanceof UnsupportedOperationException || throwable.getCause() != null && throwable.getCause() instanceof UnsupportedOperationException)
			if (this.getServer().getBukkitVersion().startsWith("1.2.5"))
				Common.log(" &cSorry but Minecraft 1.2.5 is no longer supported!");
			else {
				Common.log(" &cUnable to setup reflection!");
				Common.log(" &cYour server is either too old or");
				Common.log(" &cthe plugin broke on the new version :(");
			}

		{
			while (throwable.getCause() != null)
				throwable = throwable.getCause();

			String error = "Unable to get the error message, search above.";
			if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
				error = throwable.getMessage();

			Common.log(" &cError: " + error);
		}
		Common.log("&4!-----------------------------------------------------!");

		this.getPluginLoader().disablePlugin(this);
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {

		try {
			this.onPluginStop();
		} catch (final Throwable t) {
			Common.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		this.unregisterReloadables();

		try {
			for (final Player online : Remain.getOnlinePlayers())
				SimpleScoreboard.clearBoardsFor(online);

		} catch (final Throwable t) {
			Common.log("Error clearing scoreboards for players..");

			t.printStackTrace();
		}

		try {
			for (final Player online : Remain.getOnlinePlayers()) {
				final Menu menu = Menu.getMenu(online);

				if (menu != null)
					online.closeInventory();
			}
		} catch (final Throwable t) {
			Common.log("Error closing menu inventories for players..");

			t.printStackTrace();
		}

		Objects.requireNonNull(instance, "Instance of " + this.getDataFolder().getName() + " already nulled!");
		instance = null;
	}

	// ----------------------------------------------------------------------------------------
	// Delegate methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Called before the plugin is started, see {@link JavaPlugin#onLoad()}
	 */
	protected void onPluginLoad() {
	}

	/**
	 * The main loading method, called when we are ready to load
	 */
	protected abstract void onPluginStart();

	/**
	 * The main method called when we are about to shut down
	 */
	protected void onPluginStop() {
	}

	/**
	 * Invoked before settings were reloaded.
	 */
	protected void onPluginPreReload() {
	}

	/**
	 * Invoked after settings were reloaded.
	 */
	protected void onPluginReload() {
	}

	/**
	 * Register your commands, events, tasks and files here.
	 * <p>
	 * This is invoked when you start the plugin, call /reload, or the {@link #reload()}
	 * method.
	 */
	protected void onReloadablesStart() {
	}

	// ----------------------------------------------------------------------------------------
	// Reload
	// ----------------------------------------------------------------------------------------

	/**
	 * Attempts to reload the plugin
	 */
	public final void reload() {
		final String oldLogPrefix = Common.getLogPrefix();
		Common.setLogPrefix("");

		Common.log(Common.consoleLineSmooth());
		Common.log(" ");
		Common.log("Reloading plugin " + this.getDataFolder().getName() + " v" + getVersion());
		Common.log(" ");

		reloading = true;

		try {
			Debugger.detectDebugMode();

			this.unregisterReloadables();

			FileConfig.clearLoadedSections();

			// Load our dependency system
			try {
				HookManager.loadDependencies();

			} catch (final Throwable throwable) {
				Common.throwError(throwable, "Error while loading " + this.getDataFolder().getName() + " dependencies!");
			}

			this.onPluginPreReload();
			this.reloadables.reload();

			final YamlConfig metadata = CompMetadata.MetadataFile.getInstance();
			metadata.save();
			metadata.reload();

			SimpleHologram.onReload();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);
			this.onPluginReload();

			// Something went wrong in the reload pipeline
			if (!this.isEnabled())
				return;

			this.startingReloadables = true;

			// Register classes
			AutoRegisterScanner.scanAndRegister();

			Lang.reloadLang();
			Lang.loadPrefixes();

			this.onReloadablesStart();

			this.startingReloadables = false;

			if (HookManager.isDiscordSRVLoaded()) {
				DiscordListener.DiscordListenerImpl.getInstance().resubscribe();

				this.reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			Common.log(Common.consoleLineSmooth());

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + this.getDataFolder().getName() + " " + getVersion());

		} finally {
			Common.setLogPrefix(oldLogPrefix);

			reloading = false;
		}
	}

	private void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		BlockVisualizer.stopAll();
		FolderWatcher.stopThreads();

		try {
			if (HookManager.isDiscordSRVLoaded())
				DiscordListener.clearRegisteredListeners();
		} catch (final NoClassDefFoundError ex) {
		}

		try {
			HookManager.unloadDependencies(this);
		} catch (final NoClassDefFoundError ex) {
		}

		this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
		this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);

		this.getServer().getScheduler().cancelTasks(this);

		this.mainCommand = null;
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly registering events in all classes in your plugin that
	 * extend the given class.
	 *
	 * NB: You must have a no arguments constructor otherwise it will not be registered
	 *
	 * TIP: Set your Debug key in your settings.yml to ["auto-register"] to see what is registered.
	 *
	 * @param extendingClass
	 */
	protected final <T extends Listener> void registerAllEvents(final Class<T> extendingClass) {

		Valid.checkBoolean(!extendingClass.equals(Listener.class), "registerAllEvents does not support Listener.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleListener.class), "registerAllEvents does not support SimpleListener.class due to conflicts, create your own middle class instead");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			for (final Constructor<?> con : pluginClass.getConstructors())
				if (con.getParameterCount() == 0) {
					final T instance = (T) ReflectionUtil.instantiate(con);

					this.registerEvents(instance);

					continue classLookup;
				}
		}
	}

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listener
	 */
	protected final void registerEvents(final Listener listener) {
		if (this.startingReloadables)
			this.reloadables.registerEvents(listener);
		else
			this.getServer().getPluginManager().registerEvents(listener, this);

		if (listener instanceof DiscordListener)
			((DiscordListener) listener).register();
	}

	/**
	 * Convenience method for quickly registering a single event
	 *
	 * @param listener
	 */
	protected final void registerEvents(final SimpleListener<? extends Event> listener) {
		if (this.startingReloadables)
			this.reloadables.registerEvents(listener);

		else
			listener.register();
	}

	/**
	 * Convenience method for quickly registering all command classes in your plugin that
	 * extend the given class.
	 *
	 * NB: You must have a no arguments constructor otherwise it will not be registered
	 *
	 * TIP: Set your Debug key in your settings.yml to ["auto-register"] to see what is registered.
	 *
	 * @param extendingClass
	 */
	protected final <T extends Command> void registerAllCommands(final Class<T> extendingClass) {
		Valid.checkBoolean(!extendingClass.equals(Command.class), "registerAllCommands does not support Command.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleCommand.class), "registerAllCommands does not support SimpleCommand.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleSubCommand.class), "registerAllCommands does not support SubCommand.class");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			if (SimpleSubCommand.class.isAssignableFrom(pluginClass))
				continue;

			try {
				for (final Constructor<?> con : pluginClass.getConstructors())
					if (con.getParameterCount() == 0) {
						final T instance = (T) ReflectionUtil.instantiate(con);

						if (instance instanceof SimpleCommand)
							this.registerCommand(instance);

						else
							this.registerCommand(instance);

						continue classLookup;
					}

			} catch (final LinkageError ex) {
				Common.log("Unable to register commands in '" + pluginClass.getSimpleName() + "' due to error: " + ex);
			}
		}
	}

	/**
	 * Convenience method for registering a bukkit command
	 *
	 * @param command
	 */
	protected final void registerCommand(final Command command) {
		if (command instanceof SimpleCommand)
			((SimpleCommand) command).register();

		else
			Remain.registerCommand(command);
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register()}
	 *
	 * @param labelAndAliases
	 * @param group
	 */
	protected final void registerCommands(final SimpleCommandGroup group) {
		if (this.startingReloadables)
			this.reloadables.registerCommands(group);

		else
			group.register();
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * The start-up fancy logo
	 *
	 * @return null by default
	 */
	protected String[] getStartupLogo() {
		return null;
	}

	/**
	 * The the minimum MC version to run
	 * <p>
	 * We will prevent loading it automatically if the server's version is
	 * below the given one
	 *
	 * @return
	 */
	public MinecraftVersion.V getMinimumVersion() {
		return null;
	}

	/**
	 * The maximum MC version for this plugin to load
	 * <p>
	 * We will prevent loading it automatically if the server's version is
	 * above the given one
	 *
	 * @return
	 */
	public MinecraftVersion.V getMaximumVersion() {
		return null;
	}

	/**
	 * If you use \@AutoRegister on a command group that has a no args constructor,
	 * we use the label and aliases from {@link SimpleSettings#MAIN_COMMAND_ALIASES}
	 * and associate it here for the record.
	 *
	 * @return
	 */
	@Nullable
	public SimpleCommandGroup getMainCommand() {
		return this.mainCommand;
	}

	/**
	 * @deprecated do not use, internal use only
	 * @param group
	 */
	@Deprecated
	public final void setMainCommand(SimpleCommandGroup group) {
		Valid.checkBoolean(this.mainCommand == null, "Main command has already been set to " + this.mainCommand);

		this.mainCommand = group;
	}

	/**
	 * Get the year of foundation displayed in our {@link SimpleCommandGroup} on help
	 *
	 * @return -1 by default, or the founded year
	 */
	public int getFoundedYear() {
		return -1;
	}

	/**
	 * Get your automatic update check
	 *
	 * @return
	 */
	public SpigotUpdater getUpdateCheck() {
		return null;
	}

	/**
	 * If you want to use bStats.org metrics system,
	 * simply return the plugin ID (https://bstats.org/what-is-my-plugin-id)
	 * here and we will automatically start tracking it.
	 * <p>
	 * Defaults to -1 which means disabled
	 *
	 * @return
	 */
	public int getMetricsPluginId() {
		return -1;
	}

	/**
	 * Foundation automatically can filter console commands for you, including
	 * messages from other plugins or the server itself, preventing unnecessary console spam.
	 *
	 * You can return a list of messages that will be matched using "startsWith OR contains" method
	 * and will be filtered.
	 *
	 * @return
	 */
	public Set<String> getConsoleFilter() {
		return new HashSet<>();
	}

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 *
	 * @return time limit in milliseconds for processing regular expression
	 */
	public int getRegexTimeout() {
		throw new FoException("Must override getRegexTimeout()");
	}

	/**
	 * Strip colors from checked message while checking it against a regex?
	 *
	 * @return
	 */
	public boolean regexStripColors() {
		return true;
	}

	/**
	 * Should Pattern.CASE_INSENSITIVE be applied when compiling regular expressions in {@link Common#compilePattern(String)}?
	 * <p>
	 * May impose a slight performance penalty but increases catches.
	 *
	 * @return
	 */
	public boolean regexCaseInsensitive() {
		return true;
	}

	/**
	 * Should Pattern.UNICODE_CASE be applied when compiling regular expressions in {@link Common#compilePattern(String)}?
	 * <p>
	 * May impose a slight performance penalty but useful for non-English servers.
	 *
	 * @return
	 */
	public boolean regexUnicode() {
		return true;
	}

	/**
	 * Should we remove diacritical marks before matching regex?
	 * Defaults to true
	 *
	 * @return
	 */
	public boolean regexStripAccents() {
		return true;
	}

	/**
	 * Should we replace accents with their non accented friends when
	 * checking two strings for similarity in ChatUtil?
	 *
	 * @return defaults to true
	 */
	public boolean similarityStripAccents() {
		return true;
	}

	/**
	 * Should we send a suggestion to use PaperSpigot if not using it?
	 *
	 * @return defaults to true
	 */
	public boolean suggestPaper() {
		return true;
	}

	/**
	 * Returns the default or "main" bungee listener you use. This is checked from {@link BungeeUtil#sendPluginMessage(org.mineacademy.fo.bungee.BungeeMessageType, Object...)}
	 * so that you won't have to pass in channel name each time and we use channel name from this listener instead.
	 *
	 * @deprecated only returns the first found bungee listener, if you have multiple, do not use, order not guaranteed
	 * @return
	 */
	@Deprecated
	public final BungeeListener getBungeeCord() {
		return this.bungeeListener;
	}

	/**
	 * Sets the first valid bungee listener
	 *
	 * @deprecated INTERNAL USE ONLY, DO NOT USE! can only set one bungee listener, if you have multiple, order not guaranteed
	 * @param bungeeListener
	 */
	@Deprecated
	public final void setBungeeCord(BungeeListener bungeeListener) {
		this.bungeeListener = bungeeListener;
	}

	/**
	 * Should we listen for {@link Tool} in this plugin and
	 * handle clicking events automatically? Disable to increase performance
	 * if you do not want to use our tool system. Enabled by default.
	 *
	 * @return
	 */
	public boolean areToolsEnabled() {
		return true;
	}

	// ----------------------------------------------------------------------------------------
	// Prevention
	// ----------------------------------------------------------------------------------------

	public final ClassLoader getClazzLoader() {
		return this.getClassLoader();
	}

	/**
	 * Get the plugins jar file
	 */
	@Override
	protected final File getFile() {
		return super.getFile();
	}

	/**
	 * @deprecated DO NOT USE
	 * Use {@link SimpleCommand#register()} instead for your commands
	 */
	@Deprecated
	@Override
	public final PluginCommand getCommand(final String name) {
		return super.getCommand(name);
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		throw this.unsupported("onCommand");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
		throw this.unsupported("onTabComplete");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final FileConfiguration getConfig() {
		throw this.unsupported("getConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void saveConfig() {
		throw this.unsupported("saveConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void saveDefaultConfig() {
		throw this.unsupported("saveDefaultConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void reloadConfig() {
		throw new FoException("Cannot call reloadConfig in " + this.getDataFolder().getName() + ", use reload()!");
	}

	private FoException unsupported(final String method) {
		return new FoException("Cannot call " + method + " in " + this.getDataFolder().getName() + ", use YamlConfig or SimpleCommand classes in Foundation for that!");
	}
}
