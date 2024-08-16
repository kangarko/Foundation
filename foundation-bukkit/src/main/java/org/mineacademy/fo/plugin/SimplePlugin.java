package org.mineacademy.fo.plugin;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ProxyUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.RegionCommand;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.enchant.SimpleEnchantment;
import org.mineacademy.fo.event.SimpleListener;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.library.BukkitLibraryManager;
import org.mineacademy.fo.library.Library;
import org.mineacademy.fo.library.LibraryManager;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuListener;
import org.mineacademy.fo.menu.tool.RegionTool;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolsListener;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.platform.PlatformBukkit;
import org.mineacademy.fo.plugin.AutoRegisterScanner.AutoRegisterHandler;
import org.mineacademy.fo.plugin.AutoRegisterScanner.FindInstance;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyListenerImpl;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;

/**
 * Represents a Bukkit plugin. This class extends {@link JavaPlugin} and plugin
 * authors should extend this class when creating a new plugin.
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
	 * The Bukkit Audiences adventure platform
	 */
	private Object adventure;

	/**
	 * The library manager used to load third party libraries.
	 */
	private LibraryManager libraryManager;

	/**
	 * A temporary main command to be set in {@link #setDefaultCommandGroup(SimpleCommandGroup)}
	 * automatically by us.
	 */
	private SimpleCommandGroup defaultCommandGroup;

	/**
	 * The default proxy listener, used in {@link ProxyUtil} and {@link OutgoingMessage} if no listener is provided there
	 */
	private ProxyListener defaultProxyListener;

	/**
	 * An internal flag to indicate that the plugin is being reloaded.
	 */
	private boolean reloading = false;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	@Override
	public final void onLoad() {
		Platform.setInstance(new PlatformBukkit());

		getInstance();

		this.loadLibraries();
		this.onPluginLoad();
	}

	/*
	 * Load the necessary libraries for the plugin to work.
	 */
	private void loadLibraries() {

		this.loadLibrary("org.snakeyaml", "snakeyaml-engine", "2.7");

		if (!ReflectionUtil.isClassAvailable("com.google.gson.Gson"))
			this.loadLibrary("com.google.code.gson", "gson", "2.11.0");

		if (!ReflectionUtil.isClassAvailable("net.md_5.bungee.chat.BaseComponentSerializer"))
			this.loadLibrary("net.md-5", "bungeecord-api", "1.16-R0.1");

		if (!ReflectionUtil.isClassAvailable("org.openjdk.nashorn.api.scripting.NashornScriptEngine"))
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

		// Check if Foundation is correctly moved
		this.checkShading();

		// Check the required Minecraft server version
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

			// Initialize platform-specific variables
			Variables.setCollector(new BukkitVariableCollector());

			// Add a handler for chat paginator
			ChatPaginator.setCustomSender(new BiFunction<Audience, Integer, Boolean>() {

				@Override
				public Boolean apply(Audience audience, Integer page) {

					if (audience instanceof Player) {
						final Player player = (Player) audience;

						// Remove old FoPages to prevent conflicts when two or more plugins use Foundation shaded
						if (player.hasMetadata("FoPages")) {
							final org.bukkit.plugin.Plugin owningPlugin = player.getMetadata("FoPages").get(0).getOwningPlugin();

							player.removeMetadata("FoPages", owningPlugin);
						}

						player.setMetadata("FoPages", new FixedMetadataValue(SimplePlugin.getInstance(), SimplePlugin.this.getName()));
						player.setMetadata(ChatPaginator.getPageNbtTag(), new FixedMetadataValue(SimplePlugin.getInstance(), this));

						player.chat("/#flp " + page);
						return true;
					}

					return false;
				}
			});

			// Expand auto register functionality
			AutoRegisterScanner.setCustomRegisterHandler(new AutoRegisterHandler() {

				// Prevent duplicating registering of our {@link FoundationPacketListener}
				private boolean enchantListenersRegistered = false;

				@Override
				public void onPreScan() {
					this.enchantListenersRegistered = false;
				}

				@Override
				public boolean canAutoRegister(Class<?> clazz) {
					if (clazz == RegionTool.class && (!areRegionsEnabled() || !areToolsEnabled()))
						return false;

					return Tool.class.isAssignableFrom(clazz)
							|| SimpleEnchantment.class.isAssignableFrom(clazz)
							|| PacketListener.class.isAssignableFrom(clazz)
							|| DiscordListener.class.isAssignableFrom(clazz);
				}

				@Override
				public boolean autoRegister(Class<?> clazz, boolean printWarnings, Tuple<FindInstance, Object> tuple) {

					final FindInstance mode = tuple.getKey();
					final Object instance = tuple.getValue();

					if (DiscordListener.class.isAssignableFrom(clazz) && !HookManager.isDiscordSRVLoaded()) {
						if (printWarnings) {
							CommonCore.warning("**** WARNING ****");
							CommonCore.warning("The following class requires DiscordSRV and won't be registered: " + clazz.getSimpleName()
									+ ". To hide this message, put @AutoRegister(hideIncompatibilityWarnings=true) over the class.");
						}

						return true;
					}

					if (PacketListener.class.isAssignableFrom(clazz) && !HookManager.isProtocolLibLoaded()) {
						if (printWarnings && !clazz.equals(FoundationPacketListener.class)) {
							CommonCore.warning("**** WARNING ****");
							CommonCore.warning("The following class requires ProtocolLib and won't be registered: " + clazz.getSimpleName()
									+ ". To hide this message, put @AutoRegister(hideIncompatibilityWarnings=true) over the class.");
						}

						return true;
					}

					if (SimpleEnchantment.class.isAssignableFrom(clazz) && SimpleEnchantment.getHandleClass() == null) {
						if (printWarnings && !clazz.equals(SimpleEnchantment.class)) {
							CommonCore.warning("**** WARNING ****");
							CommonCore.warning("The following class requires SimpleEnchantment#registerEnchantmentHandle to be implemented and won't be registered: " + clazz.getSimpleName()
									+ ". See https://www.youtube.com/watch?v=1_W0ISi5ZbM for a sample tutorial."
									+ " To hide this message, put @AutoRegister(hideIncompatibilityWarnings=true) over the class.");
						}

						return true;
					}

					// TODO check if proxy listener still works

					if (SimpleListener.class.isAssignableFrom(clazz)) {
						enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						return true;
					}

					else if (PacketListener.class.isAssignableFrom(clazz)) {
						// Automatically registered by means of adding packet adapters
						enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						((PacketListener) instance).onRegister();

						return true;
					}

					else if (DiscordListener.class.isAssignableFrom(clazz)) {
						// Automatically registered in its constructor
						enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						((DiscordListener) instance).register();

						return true;

					} else if (SimpleEnchantment.class.isAssignableFrom(clazz)) {
						// Automatically registered in its constructor
						enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						if (!this.enchantListenersRegistered) {
							this.enchantListenersRegistered = true;

							registerEvents(SimpleEnchantment.Listener.getInstance());

							if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null)
								FoundationPacketListener.getInstance().onRegister();
							else
								CommonCore.warning("Custom enchantments require ProtocolLib for lore to be added properly.");
						}

						return true;
					}

					else if (Tool.class.isAssignableFrom(clazz)) {
						// Automatically registered in its constructor that is called when we find instance
						enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						return true;

					}

					if (instance instanceof Listener) {
						registerEvents((Listener) instance);

						return true;
					}

					return false;
				}
			});

			// Register third party hooks
			HookManager.loadDependencies();

			// Register proxy messaging - always make the default channel available
			final Messenger messenger = this.getServer().getMessenger();

			if (!messenger.isIncomingChannelRegistered(this, ProxyListener.DEFAULT_CHANNEL))
				messenger.registerIncomingPluginChannel(this, ProxyListener.DEFAULT_CHANNEL, ProxyListenerImpl.getInstance());

			if (!messenger.isOutgoingChannelRegistered(this, ProxyListener.DEFAULT_CHANNEL))
				messenger.registerOutgoingPluginChannel(this, ProxyListener.DEFAULT_CHANNEL);

			// Scan for @AutoRegister annotations
			AutoRegisterScanner.scanAndRegister();

			if (this.areRegionsEnabled())
				DiskRegion.loadRegions();

			this.onPluginStart();

			// Freeze enchant registry - must be called after onPluginStart
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

			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				this.registerEvents(CompMetadata.MetadataFile.getInstance());

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded()) {
				final DiscordListener.DiscordListenerImpl discord = DiscordListener.DiscordListenerImpl.getInstance();

				discord.resubscribe();
				discord.registerHook();

				this.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

		} catch (final Throwable t) {
			this.displayError(t);

		} finally {

			// Finally, place plugin name before console messages after plugin has (re)loaded
			Common.runLater(() -> Common.setLogPrefix(oldLogPrefix));
		}
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
			if (!getName().equals(SimplePlugin.this.getDescription().getName())) {
				Bukkit.getLogger().severe("We have a class path problem in the Foundation library");
				Bukkit.getLogger().severe("preventing " + SimplePlugin.this.getDescription().getName() + " from loading correctly!");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("This is likely caused by two plugins having the");
				Bukkit.getLogger().severe("same Foundation library paths - make sure you");
				Bukkit.getLogger().severe("relocale the package! If you are testing using");
				Bukkit.getLogger().severe("Ant, only test one plugin at the time.");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("Possible cause: " + getName());
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
	protected final void displayError(Throwable throwable) {
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

		for (final Player online : Remain.getOnlinePlayers()) {
			try {
				SimpleScoreboard.clearBoardsFor(online);

			} catch (final Throwable t) {
				Common.error(t, "Error clearing scoreboard for player " + online.getName());
			}

			try {
				final Menu menu = Menu.getMenu(online);

				if (menu != null)
					online.closeInventory();

			} catch (final Throwable t) {
				Common.error(t, "Error closing menu for player " + online.getName());
			}
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

	// ----------------------------------------------------------------------------------------
	// Reloading and disabling
	// ----------------------------------------------------------------------------------------

	@Override
	public final boolean isReloading() {
		return this.reloading;
	}

	/**
	 * Reload this plugin's settings files.
	 */
	@Override
	public final void reload() {
		this.reloading = true;

		try {
			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				CompMetadata.MetadataFile.getInstance().save();

			this.onPluginPreReload();

			AutoRegisterScanner.reloadSettings();

			this.onPluginReload();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			Lang.reloadLang();

			if (this.areRegionsEnabled())
				DiskRegion.loadRegions();

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + this.getName() + " " + this.getVersion());

		} finally {
			reloading = false;
		}
	}

	/**
	 * Disables this plugin
	 *
	 * Attempting to disable a plugin that is not enabled will have no effect
	 */
	@Override
	public final void disable() {
		this.getServer().getPluginManager().disablePlugin(this);
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listener
	 */
	public final void registerEvents(final Listener listener) {
		this.getServer().getPluginManager().registerEvents(listener, this);
	}

	/**
	 * Convenience method for quickly registering a single event
	 *
	 * @param listener
	 */
	public final void registerEvents(final SimpleListener<? extends Event> listener) {
		listener.register();
	}

	/**
	 * Convenience method for registering a bukkit command
	 *
	 * @param command
	 */
	@Override
	public final void registerCommand(final SimpleCommandCore command) {
		command.register();
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register()}
	 *
	 * @param group
	 */
	@Override
	public final void registerCommands(final SimpleCommandGroup group) {
		group.register();
	}

	// ----------------------------------------------------------------------------------------
	// Defaults
	// ----------------------------------------------------------------------------------------

	/**
	 * Get the default command group used in registering a {@link SimpleSubCommand} using {@link AutoRegister}
	 * annotation when no group is provided in its constructor.
	 *
	 * @return
	 */
	@Override
	public final SimpleCommandGroup getDefaultCommandGroup() {
		return this.defaultCommandGroup;
	}

	/**
	 * Set the default command group used in registering a {@link SimpleSubCommand} using {@link AutoRegister}
	 * annotation when no group is provided in its constructor.
	 */
	@Override
	public final void setDefaultCommandGroup(SimpleCommandGroup group) {
		Valid.checkBoolean(this.defaultCommandGroup == null, "Main command has already been set to " + this.defaultCommandGroup);

		this.defaultCommandGroup = group;
	}

	/**
	 * Set the default proxy used in {@link ProxyUtil#sendBungeeMessage(Player, Object...)}
	 * and {@link OutgoingMessage} when no group is provided.
	 *
	 * @return
	 */
	@Override
	public final ProxyListener getDefaultProxyListener() {
		return this.defaultProxyListener;
	}

	/**
	 * Set the default proxy used in {@link ProxyUtil#sendBungeeMessage(Player, Object...)}
	 * and {@link OutgoingMessage} when no group is provided.
	 *
	 * @param listener
	 */
	@Override
	public final void setDefaultProxyListener(ProxyListener listener) {
		this.defaultProxyListener = listener;
	}

	// ----------------------------------------------------------------------------------------
	// Library manager
	// ----------------------------------------------------------------------------------------

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

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * The start-up fancy logo
	 *
	 * @return null by default
	 */
	public String[] getStartupLogo() {
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
	}

	// ----------------------------------------------------------------------------------------
	// Overriding parent methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Return the adventure platform
	 *
	 * @return
	 */
	@Override
	public final Object getAdventure() {
		if (this.adventure == null)
			this.adventure = BukkitAudiences.create(instance);

		return this.adventure;
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

	/**
	 * Get the plugins jar file
	 */
	@Override
	public final File getFile() {
		return super.getFile();
	}

	/**
	 * Get the plugin's version
	 */
	@Override
	public final String getVersion() {
		return this.getDescription().getVersion();
	}

	/**
	 * Get the plugin's class loader
	 */
	@Override
	public final ClassLoader getPluginClassLoader() {
		return super.getClassLoader();
	}

	/**
	 * Get the plugin's authors, joined by a comma
	 */
	@Override
	public final String getAuthors() {
		return String.join(", ", this.getDescription().getAuthors());
	}
}
