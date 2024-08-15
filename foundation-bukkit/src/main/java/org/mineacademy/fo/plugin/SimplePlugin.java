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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ProxyUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.RegionCommand;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.event.SimpleListener;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.library.BukkitLibraryManager;
import org.mineacademy.fo.library.Library;
import org.mineacademy.fo.library.LibraryManager;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuListener;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolsListener;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleHologram;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyListenerImpl;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.FileConfig;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.visual.BlockVisualizer;

import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Represents a basic Java plugin using enhanced library functionality,
 * implementing a listener for easy use
 */
public abstract class SimplePlugin extends JavaPlugin implements Listener, FoundationPlugin {

	// ----------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------

	/**
	 * The instance of this plugin
	 */
	private static SimplePlugin instance;

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
	private static boolean reloading = false;

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
	 * The library manager
	 */
	private LibraryManager libraryManager;

	/**
	 * An internal flag to indicate whether we are calling the {@link #onReloadablesStart()}
	 * block. We register things using {@link #reloadables} during this block
	 */
	private boolean startingReloadables = false;

	/**
	 * A temporary main command to be set in {@link #setDefaultCommandGroup(SimpleCommandGroup)}
	 * automatically by us.
	 */
	private SimpleCommandGroup defaultCommandGroup;

	/**
	 * The default proxy listener, used in {@link ProxyUtil} if no listener is provided there
	 */
	private ProxyListener defaultProxyListener;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	@Override
	public final void onLoad() {

		// Set the instance
		getInstance();

		// Cache results for best performance
		named = instance.getDataFolder().getName();
		source = instance.getFile();
		data = instance.getDataFolder();

		// Load libraries where Spigot does not do this automatically
		this.loadLibraries();

		// Call parent
		this.onPluginLoad();

	}

	/*
	 * Load the necessary libraries for the plugin to work.
	 */
	private void loadLibraries() {
		if (!ReflectionUtil.isClassAvailable("com.google.gson.Gson"))
			this.loadLibrary("com.google.code.gson", "gson", "2.11.0");

		if (!ReflectionUtil.isClassAvailable("net.md_5.bungee.chat.BaseComponentSerializer"))
			this.loadLibrary("net.md-5", "bungeecord-api", "1.16-R0.1");

		if (getJavaVersion() >= 11)
			this.loadLibrary("org.openjdk.nashorn", "nashorn-core", "15.4");

		final boolean hasAdventure = ReflectionUtil.isClassAvailable("net.kyori.adventure.audience.Audience");

		if (MinecraftVersion.olderThan(V.v1_16)) {
			this.loadLibrary("net.kyori", "adventure-api", "4.17.0");
			this.loadLibrary("net.kyori", "adventure-platform-bukkit", "4.3.3");
		}

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer"))
			this.loadLibrary("net.kyori", "adventure-text-serializer-plain", "4.17.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.minimessage.MiniMessage"))

			// Pre-merge: 1.16-1.17
			if (hasAdventure) {
				String version = "4.2.0-SNAPSHOT";

				try {
					Component.class.getMethod("compact");

				} catch (final ReflectiveOperationException ex) {
					version = "4.1.0-SNAPSHOT";
				}

				this.getLibraryManager().loadLibrary(Library.builder()
						.groupId("net.kyori")
						.artifactId("adventure-text-minimessage")
						.version(version)
						.url("https://bitbucket.org/kangarko/libraries/raw/master/org/mineacademy/library/adventure-text-minimessage/" + version + "/adventure-text-minimessage-" + version + ".jar")
						.build());

			} else
				this.loadLibrary("net.kyori", "adventure-text-minimessage", "4.10.0");
	}

	@Override
	public final void onEnable() {

		// Solve reloading issues with PlugMan
		for (final StackTraceElement element : new Throwable().getStackTrace())
			if (element.toString().contains("com.rylinaux.plugman.util.PluginUtil.load")) {
				Common.warning("Detected PlugMan reload, which is poorly designed. "
						+ "It causes Bukkit not able to get our plugin from a static initializer."
						+ " You will get no support. Use our own reload command or do a clean restart!");

				break;
			}

		// Check if Foundation is correctly moved
		this.checkShading();

		// Before all, check if necessary libraries and the minimum required MC version
		if (!this.checkServerVersions0()) {
			this.setEnabled(false);

			return;
		}

		// Save logging prefix and disable the one set by the user
		final String oldLogPrefix = Common.getLogPrefix();
		Common.setLogPrefix("");

		try {

			if (this.getStartupLogo() != null)
				Common.log(this.getStartupLogo());

			Variables.setCollector(new BukkitVariableCollector());

			HookManager.loadDependencies();

			this.registerDefaultProxyChannels(ProxyListener.DEFAULT_CHANNEL);

			this.startingReloadables = true;

			AutoRegisterScanner.scanAndRegister();

			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				this.registerEvents(CompMetadata.MetadataFile.getInstance());

			if (this.areRegionsEnabled())
				DiskRegion.loadRegions();

			this.onReloadablesStart();

			this.startingReloadables = false;

			this.onPluginStart();

			if (Remain.isEnchantRegistryUnfrozen())
				Remain.freezeEnchantRegistry();

			// Return if plugin start indicated a fatal problem
			if (!this.isEnabled())
				return;

			// Register our listeners
			this.registerEvents(this);
			this.registerEvents(new FoundationListener());

			if (this.areMenusEnabled())
				this.registerEvents(new MenuListener());

			if (this.areToolsEnabled())
				this.registerEvents(new ToolsListener());

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded()) {
				final DiscordListener.DiscordListenerImpl discord = DiscordListener.DiscordListenerImpl.getInstance();

				discord.resubscribe();
				discord.registerHook();

				this.reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

		} catch (final Throwable t) {
			this.displayError0(t);

		} finally {

			// Finally, place plugin name before console messages after plugin has (re)loaded
			Common.runLater(() -> Common.setLogPrefix(oldLogPrefix));
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

				throw new FoException("Shading exception, see above for details.");
			}
		}
	}

	/**
	 * Check if the minimum required MC version is installed
	 *
	 * @return
	 */
	private boolean checkServerVersions0() {

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
				privateDistro ? null : " &cRunning on " + Bukkit.getBukkitVersion() + " & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		if (throwable instanceof InvalidConfigurationException) {
			Common.log(" &cSeems like your config is not a valid YAML.");
			Common.log(" &cUse online services like");
			Common.log(" &chttp://yaml-online-parser.appspot.com/");
			Common.log(" &cto check for syntax errors!");

		} else if (throwable instanceof UnsupportedOperationException || throwable.getCause() != null && throwable.getCause() instanceof UnsupportedOperationException) {
			Common.log(" &cUnable to setup reflection!");
			Common.log(" &cYour server is either too old or");
			Common.log(" &cthe plugin broke on the new version :(");
		}

		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		String error = "Unable to get the error message, search above.";
		if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
			error = throwable.getMessage();

		Common.log(" &cError: " + error);
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

		if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
			CompMetadata.MetadataFile.getInstance().save();

		this.unregisterReloadables();

		try {
			for (final Player online : Remain.getOnlinePlayers())
				SimpleScoreboard.clearBoardsFor(online);

		} catch (final Throwable t) {
			Common.error(t, "Error clearing scoreboards for players..");
		}

		try {
			for (final Player online : Remain.getOnlinePlayers()) {
				final Menu menu = Menu.getMenu(online);

				if (menu != null)
					online.closeInventory();
			}
		} catch (final Throwable t) {
			Common.error(t, "Error closing menu inventories for players..");
		}

		if (this.areRegionsEnabled())
			for (final DiskRegion region : DiskRegion.getRegions())
				try {
					region.save();

				} catch (final Throwable t) {
					Common.error(t, "Error saving region " + region.getName() + "...");
				}

		Platform.closeAdventurePlatform();

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
	@Override
	public final void reload() {
		final String oldLogPrefix = Common.getLogPrefix();
		Common.setLogPrefix("");

		reloading = true;

		try {
			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				CompMetadata.MetadataFile.getInstance().save();

			this.unregisterReloadables();
			this.registerDefaultProxyChannels(ProxyListener.DEFAULT_CHANNEL);

			// Load our dependency system
			try {
				HookManager.loadDependencies();

			} catch (final Throwable throwable) {
				Common.throwError(throwable, "Error while loading " + this.getDataFolder().getName() + " dependencies!");
			}

			this.onPluginPreReload();
			this.reloadables.reload();

			SimpleHologram.onReload();

			this.startingReloadables = true;

			// Register classes
			AutoRegisterScanner.scanAndRegister();

			this.onPluginReload();

			// Something went wrong in the reload pipeline
			if (!this.isEnabled()) {
				this.startingReloadables = false;

				return;
			}

			// Register prefix after
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			Lang.reloadLang();
			Lang.loadPrefixes();

			if (this.areRegionsEnabled())
				DiskRegion.loadRegions();

			this.onReloadablesStart();

			this.startingReloadables = false;

			if (HookManager.isDiscordSRVLoaded()) {
				DiscordListener.DiscordListenerImpl.getInstance().resubscribe();

				this.reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				this.registerEvents(CompMetadata.MetadataFile.getInstance());

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + this.getDataFolder().getName() + " " + this.getVersion());

		} finally {
			Common.setLogPrefix(oldLogPrefix);

			reloading = false;
		}
	}

	private void registerDefaultProxyChannels(String channelName) {
		final Messenger messenger = this.getServer().getMessenger();

		// Always make the main channel available
		if (!messenger.isIncomingChannelRegistered(this, channelName))
			messenger.registerIncomingPluginChannel(this, channelName, ProxyListenerImpl.getInstance());

		if (!messenger.isOutgoingChannelRegistered(this, channelName))
			messenger.registerOutgoingPluginChannel(this, channelName);
	}

	private void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		BlockVisualizer.stopAll();

		FileConfig.clearLoadedSections();

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

		Common.cancelTasks();

		this.defaultCommandGroup = null;
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

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
	@Override
	@Nullable
	public SimpleCommandGroup getDefaultCommandGroup() {
		return this.defaultCommandGroup;
	}

	/**
	 * @deprecated do not use, internal use only
	 * @param group
	 */
	@Deprecated
	public final void setDefaultCommandGroup(SimpleCommandGroup group) {
		Valid.checkBoolean(this.defaultCommandGroup == null, "Main command has already been set to " + this.defaultCommandGroup);

		this.defaultCommandGroup = group;
	}

	/**
	 * Get the year of foundation displayed in our {@link SimpleCommandGroup} on help
	 *
	 * @return -1 by default, or the founded year
	 */
	@Override
	public int getFoundedYear() {
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
	 * Strip colors from checked message while checking it against a regex?
	 *
	 * @return
	 */
	@Override
	public boolean isRegexStrippingColors() {
		return true;
	}

	/**
	 * Should Pattern.CASE_INSENSITIVE be applied when compiling regular expressions in the Common class?
	 * <p>
	 * May impose a slight performance penalty but increases catches.
	 *
	 * @return
	 */
	@Override
	public boolean isRegexCaseInsensitive() {
		return true;
	}

	/**
	 * Should Pattern.UNICODE_CASE be applied when compiling regular expressions in the Common class?
	 * <p>
	 * May impose a slight performance penalty but useful for non-English servers.
	 *
	 * @return
	 */
	@Override
	public boolean isRegexUnicode() {
		return true;
	}

	/**
	 * Should we remove diacritical marks before matching regex?
	 * Defaults to true
	 *
	 * @return
	 */
	@Override
	public boolean isRegexStrippingAccents() {
		return true;
	}

	/**
	 * Should we replace accents with their non accented friends when
	 * checking two strings for similarity in {@link ChatUtil}?
	 *
	 * @return defaults to true
	 */
	@Override
	public boolean isSimilarityStrippingAccents() {
		return true;
	}

	/**
	 * Returns the "default" proxy listener you use. This is used in {@link ProxyUtil} when no channel is provided.
	 *
	 * @deprecated only returns the first listener, if you have multiple, do not use, order not guaranteed
	 * @return
	 */
	@Override
	@Deprecated
	public final ProxyListener getDefaultProxyListener() {
		return this.defaultProxyListener;
	}

	/**
	 * Sets a proxy listener to use in {@link ProxyUtil} when no listener is provided
	 *
	 * @deprecated INTERNAL USE ONLY
	 * @param listener
	 */
	@Deprecated
	public final void setDefaultProxyListener(ProxyListener listener) {
		this.defaultProxyListener = listener;
	}

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 *
	 * If the provided library has any relocations, they will be applied to
	 * create a relocated jar and the relocated jar will be loaded instead.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	@Override
	public final void loadLibrary(String groupId, String artifactId, String version) {
		this.getLibraryManager().loadLibrary(Library.builder().groupId(groupId).artifactId(artifactId).resolveTransitiveDependencies(true).version(version).build());
	}

	/**
	 * Get the Libby library manager
	 *
	 * @return
	 */
	public final LibraryManager getLibraryManager() {
		if (this.libraryManager == null)
			this.libraryManager = new BukkitLibraryManager(this);

		return this.libraryManager;
	}

	/**
	 * Should we listen for {@link Menu} class clicking?
	 *
	 * True by default. Returning false here will break the entire Foundation menu
	 * system, useful if you want to use your own.
	 *
	 * @return
	 */
	public boolean areMenusEnabled() {
		return true;
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

	/**
	 * Should we enable the region system? Loads {@link DiskRegion#loadRegions()}
	 * You still need to register the subcommand {@link RegionCommand} manually.
	 *
	 * @return
	 */
	public boolean areRegionsEnabled() {
		return false;
	}

	/**
	 * Remove [Not Secure] misinformation message from console chat.
	 *
	 * @return
	 */
	public boolean filterInsecureChat() {
		return true;
	} // TODO delete these and see the errors

	/**
	 * Get the plugins jar file
	 */
	@Override
	public final File getFile() {
		return super.getFile();
	}

	/**
	 * Return the command specified in plugin.yml
	 *
	 * @deprecated Still works, but Foundation provides SimpleCommand instead
	 * 			   for your commands where you can use \@AutoRegister to register
	 * 		  	   commands automatically without the need of using plugin.yml.
	 */
	@Deprecated
	@Override
	public final PluginCommand getCommand(final String name) {
		return super.getCommand(name);
	}

	@Override
	public final void disable() {
		this.getServer().getPluginManager().disablePlugin(this);
	}

	@Override
	public final String getVersion() {
		return this.getDescription().getVersion();
	}

	@Override
	public final ClassLoader getPluginClassLoader() {
		return super.getClassLoader();
	}

	@Override
	public final String getAuthors() {
		return String.join(", ", this.getDescription().getAuthors());
	}
}
