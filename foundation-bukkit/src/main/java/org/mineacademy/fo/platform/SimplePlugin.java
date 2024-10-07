package org.mineacademy.fo.platform;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
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
import org.mineacademy.fo.model.BStatsMetrics;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.platform.AutoRegisterScanner.AutoRegisterHandler;
import org.mineacademy.fo.platform.AutoRegisterScanner.FindInstance;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import net.kyori.adventure.text.Component;

/**
 * Represents a Bukkit plugin.
 *
 * This class extends {@link JavaPlugin} and plugin
 * authors should extend this class when creating a new plugin.
 */
public abstract class SimplePlugin extends JavaPlugin implements Listener, FoundationPlugin {

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
				if (Bukkit.getPluginManager().getPlugin("PlugMan") != null || Bukkit.getPluginManager().getPlugin("PlugManX") != null)
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

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	@Override
	public final void onLoad() {
		getInstance();

		this.loadLibraries();

		BukkitPlatform.inject();

		this.onPluginLoad();
	}

	/*
	 * Load the necessary libraries for the plugin to work.
	 */
	private void loadLibraries() {
		this.loadLibrary("org.snakeyaml", "snakeyaml-engine", "2.8");

		if (!ReflectionUtil.isClassAvailable("com.google.gson.Gson"))
			this.loadLibrary("com.google.code.gson", "gson", "2.11.0");

		if (this.getJavaVersion() >= 15 && !ReflectionUtil.isClassAvailable("org.openjdk.nashorn.api.scripting.NashornScriptEngine"))
			this.loadLibrary("org.openjdk.nashorn", "nashorn-core", "15.4");

		if (!ReflectionUtil.isClassAvailable("net.md_5.bungee.chat.BaseComponentSerializer"))
			this.loadLibrary("net.md-5", "bungeecord-api", "1.16-R0.1");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.minimessage.MiniMessage"))

			// Pre-merge: 1.16-1.17
			if (ReflectionUtil.isClassAvailable("net.kyori.adventure.audience.Audience")) {
				String version = "4.2.0";

				try {
					Component.class.getMethod("compact");

				} catch (final ReflectiveOperationException ex) {
					version = "4.1.0";
				}

				this.getLibraryManager().loadLibrary(Library.builder()
						.groupId("net.kyori")
						.artifactId("adventure-text-minimessage")
						.version(version)
						.url("https://bitbucket.org/kangarko/libraries/raw/master/org/mineacademy/library/adventure-text-minimessage/" + version + "/adventure-text-minimessage-" + version + ".jar")
						.build());

			} else
				this.loadLibrary("net.kyori", "adventure-text-minimessage", "4.17.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.audience.Audience"))
			this.loadLibrary("net.kyori", "adventure-api", "4.17.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer"))
			this.loadLibrary("net.kyori", "adventure-text-serializer-plain", "4.17.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"))
			this.loadLibrary("net.kyori", "adventure-text-serializer-legacy", "4.17.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"))
			this.loadLibrary("net.kyori", "adventure-text-serializer-gson", "4.17.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer"))
			this.loadLibrary("net.kyori", "adventure-text-serializer-bungeecord", "4.3.4");
	}

	/*
	 * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
	 *
	 * Need a duplicate of this method here because the one in Remain class cannot be used at this time.
	 */
	private int getJavaVersion() {
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

	@Override
	public final void onEnable() {

		// Check if Foundation is correctly moved
		this.checkShading();

		// Check the required Minecraft server version
		if (MinecraftVersion.olderThan(V.v1_8)) {
			Common.logFramed(false,
					this.getDataFolder().getName() + " requires Minecraft 1.8.8 or newer to run.",
					"Please upgrade your server.");

			this.setEnabled(false);
			return;
		}

		try {
			if (this.getStartupLogo() != null)
				Common.log(this.getStartupLogo());

			// Expand auto register functionality
			AutoRegisterScanner.setCustomRegisterHandler(new AutoRegisterHandler() {

				// Prevent duplicating registering of our {@link FoundationPacketListener}
				private boolean enchantListenersRegistered = false;

				@Override
				public void onPreScan() {
					this.enchantListenersRegistered = false;
				}

				@Override
				public boolean isIgnored(Class<?> clazz, boolean printWarnings) {
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

					if (clazz == RegionTool.class && (!areRegionsEnabled() || !areToolsEnabled()))
						return true;

					return false;
				}

				@Override
				public boolean canAutoRegister(Class<?> clazz) {
					return Tool.class.isAssignableFrom(clazz) || SimpleEnchantment.class.isAssignableFrom(clazz) || PacketListener.class.isAssignableFrom(clazz) || DiscordListener.class.isAssignableFrom(clazz);
				}

				@Override
				public boolean autoRegister(Class<?> clazz, Tuple<FindInstance, Object> tuple) {

					final FindInstance mode = tuple.getKey();
					final Object instance = tuple.getValue();

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
				messenger.registerIncomingPluginChannel(this, ProxyListener.DEFAULT_CHANNEL, FoundationPluginMessageListener.getInstance());

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

			if (CompMetadata.isLegacy())
				this.registerEvents(CompMetadata.MetadataFile.getInstance());

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded()) {
				final DiscordListener.DiscordListenerImpl discord = DiscordListener.DiscordListenerImpl.getInstance();

				discord.resubscribe();
				discord.registerHook();

				this.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			// Move the legacy localization folder to unused
			{
				final File localizationFolder = new File(this.getDataFolder(), "localization");

				if (localizationFolder.exists()) {
					Common.warning("The localization/ folder is now unused, run '" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " dumplocale' to download the new locale format. Moving to unused/ ...");

					final File unusedFolder = new File(this.getDataFolder(), "unused");

					if (!unusedFolder.exists())
						unusedFolder.mkdirs();

					localizationFolder.renameTo(new File(unusedFolder, "localization"));
				}
			}

			if (this.getBStatsPluginId() != -1)
				new BStatsMetrics(this, this.getBStatsPluginId());

		} catch (final Throwable t) {
			this.displayError(t);
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
	 * Handles various startup problems
	 *
	 * @param throwable
	 */
	protected final void displayError(Throwable throwable) {
		Debugger.printStackTrace(throwable);

		Common.log(
				"&4    ___                  _ ",
				"&4   / _ \\  ___  _ __  ___| |",
				"&4  | | | |/ _ \\| '_ \\/ __| |",
				"&4  | |_| | (_) | |_) \\__ \\_|",
				"&4   \\___/ \\___/| .__/|___(_)",
				"&4             |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + this.getDescription().getName() + " v" + this.getDescription().getVersion() + ", plugin is disabled!",
				" &cRunning on " + Bukkit.getName() + " " + Bukkit.getBukkitVersion() + " & Java " + System.getProperty("java.version"),
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

		if (CompMetadata.isLegacy() && CompMetadata.MetadataFile.getInstance().getFile() != null)
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
					Common.error(t, "Error saving region " + region.getFileName() + "...");
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

	// ----------------------------------------------------------------------------------------
	// Reloading and disabling
	// ----------------------------------------------------------------------------------------

	/**
	 * Reload this plugin's settings files.
	 */
	@Override
	public final void reload() {

		try {
			if (CompMetadata.isLegacy() && CompMetadata.MetadataFile.getInstance().getFile() != null)
				CompMetadata.MetadataFile.getInstance().save();

			this.onPluginPreReload();

			AutoRegisterScanner.reloadSettings();

			this.onPluginReload();

			if (this.areRegionsEnabled())
				DiskRegion.loadRegions();

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + this.getName() + " " + this.getVersion());
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
	 * Convenience method for registering a command.
	 *
	 * @see SimpleCommandCore#register()
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
	 *
	 * @param group
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
	 * Should Pattern.CASE_INSENSITIVE be applied when compiling regular expressions in the Common class?
	 *
	 * May impose a slight performance penalty but increases catches.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	@Override
	public boolean isRegexCaseInsensitive() {
		return true;
	}

	/**
	 * Should Pattern.UNICODE_CASE be applied when compiling regular expressions in the Common class?
	 *
	 * May impose a slight performance penalty but useful for non-English servers.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	@Override
	public boolean isRegexUnicode() {
		return true;
	}

	/**
	 * Should we remove diacritical marks before matching regex?
	 * Defaults to true.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	@Override
	public boolean isRegexStrippingAccents() {
		return true;
	}

	/**
	 * Strip colors from checked message while checking it against a regex?
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	@Override
	public boolean isRegexStrippingColors() {
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
	 * Returns the Sentry DSN to use for error tracking or null if Sentry is disabled.
	 *
	 * @return
	 */
	@Override
	public String getSentryDsn() {
		return null;
	}

	/**
	 * Return the bStats plugin id, if not -1, we automatically start reporting
	 * your plugin to bStats.
	 *
	 * @return
	 */
	public int getBStatsPluginId() {
		return -1;
	}

	// ----------------------------------------------------------------------------------------
	// Overriding parent methods
	// ----------------------------------------------------------------------------------------

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
