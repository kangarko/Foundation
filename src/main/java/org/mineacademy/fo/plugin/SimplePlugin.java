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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.SimpleBungee;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.event.SimpleListener;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuListener;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolsListener;
import org.mineacademy.fo.metrics.Metrics;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.EnchantmentListener;
import org.mineacademy.fo.model.FolderWatcher;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.JavaScriptExecutor;
import org.mineacademy.fo.model.SimpleEnchantment;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.model.SimpleHologram;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.SpigotUpdater;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.settings.YamlStaticConfig;
import org.mineacademy.fo.visual.BlockVisualizer;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a basic Java plugin using enhanced library functionality
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
	 *
	 * @return plugin's version
	 */
	@Getter
	private static String version;

	/**
	 * Shortcut for getName()
	 *
	 * @return plugin's name
	 */
	@Getter
	private static String named;

	/**
	 * Shortcut for getFile()
	 *
	 * @return plugin's jar file
	 */
	@Getter
	private static File source;

	/**
	 * Shortcut for getDataFolder()
	 *
	 * @return plugins' data folder in plugins/
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
	 * Is this plugin enabled? Checked for after {@link #onPluginPreStart()}
	 */
	protected boolean isEnabled = true;

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
		named = instance.getName();
		source = instance.getFile();
		data = instance.getDataFolder();

		// Call parent
		onPluginLoad();
	}

	@Override
	public final void onEnable() {

		// Solve reloading issues with PlugMan
		for (final StackTraceElement element : new Throwable().getStackTrace()) {
			if (element.toString().contains("com.rylinaux.plugman.util.PluginUtil.load")) {
				Common.warning("Detected PlugMan reload, which is poorly designed. "
						+ "It causes Bukkit not able to get our plugin from a static initializer."
						+ " It may or may not run. Use our own reload command or do a clean restart!");

				break;
			}
		}

		// Check if Foundation is correctly moved
		checkShading();

		if (!isEnabled)
			return;

		// Before all, check if necessary libraries and the minimum required MC version
		if (!checkLibraries0() || !checkServerVersions0()) {
			isEnabled = false;
			setEnabled(false);

			return;
		}

		// Load debug mode early
		Debugger.detectDebugMode();

		// Disable logging prefix if logo is set
		if (getStartupLogo() != null)
			Common.ADD_LOG_PREFIX = false;

		// Print startup logo early before onPluginPreStart
		if (getStartupLogo() != null) {
			final boolean hadLogPrefix = Common.ADD_LOG_PREFIX;

			Common.ADD_LOG_PREFIX = false;
			Common.log(getStartupLogo());
			Common.ADD_LOG_PREFIX = hadLogPrefix;
		}

		// Inject server-name to newer MC versions that lack it
		Remain.injectServerName();

		// Load our dependency system
		try {
			HookManager.loadDependencies();

		} catch (final Throwable throwable) {
			Common.throwError(throwable, "Error while loading " + getName() + " dependencies!");
		}

		// --------------------------------------------
		// Call the main pre start method
		// --------------------------------------------
		onPluginPreStart();
		// --------------------------------------------

		// Return if plugin pre start indicated a fatal problem
		if (!isEnabled || !isEnabled())
			return;

		try {

			// Load our main static settings classes
			YamlStaticConfig.load(getSettings());

			if (!isEnabled || !isEnabled())
				return;

			// Register classes
			checkSingletons();

			if (!isEnabled || !isEnabled())
				return;

			// Load legacy permanent metadata store
			CompMetadata.MetadataFile.getInstance();

			SimpleHologram.init();

			// Register main command if it is set
			if (getMainCommand() != null) {
				Valid.checkBoolean(!SimpleSettings.MAIN_COMMAND_ALIASES.isEmpty(), "Please make a settings class extending SimpleSettings and specify Command_Aliases in your settings file.");

				reloadables.registerCommands(SimpleSettings.MAIN_COMMAND_ALIASES, getMainCommand());
			}

			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------
			if (!isEnabled || !isEnabled())
				return;

			// Hide plugin name before console messages
			final boolean hadLogPrefix = Common.ADD_LOG_PREFIX;
			Common.ADD_LOG_PREFIX = false;

			startingReloadables = true;
			onReloadablesStart();
			startingReloadables = false;

			onPluginStart();
			// --------------------------------------------

			// Return if plugin start indicated a fatal problem
			if (!isEnabled || !isEnabled())
				return;

			// Register BungeeCord when used
			if (getBungeeCord() != null)
				registerBungeeCord(getBungeeCord());

			// Start update check
			if (getUpdateCheck() != null)
				getUpdateCheck().run();

			// Register our listeners
			registerEvents(this); // For convenience
			registerEvents(new MenuListener());
			registerEvents(new FoundationListener());
			registerEvents(new EnchantmentListener());

			if (areToolsEnabled())
				registerEvents(new ToolsListener());

			// Register our packet listener
			FoundationPacketListener.addNativeListener();

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded()) {
				final DiscordListener.DiscordListenerImpl discord = DiscordListener.DiscordListenerImpl.getInstance();

				discord.resubscribe();
				discord.registerHook();

				reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			// Prepare Nashorn engine
			JavaScriptExecutor.run("");

			// Finish off by starting metrics (currently bStats)
			final int pluginId = getMetricsPluginId();

			if (pluginId != -1)
				new Metrics(this, pluginId);

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			// Finally, place plugin name before console messages after plugin has (re)loaded
			Common.runLater(() -> Common.ADD_LOG_PREFIX = hadLogPrefix);

		} catch (final Throwable t) {
			displayError0(t);
		}
	}

	/**
	 * Register a simple bungee class as a custom bungeecord listener,
	 * for sample implementation you can see the SimpleBungee field at:
	 * https://github.com/kangarko/PluginTemplate/blob/main/src/main/java/org/mineacademy/template/PluginTemplate.java
	 *
	 * DO NOT use this if you only have that one field there with a getter, we already register it automatically,
	 * this method is intended to be used if you have multiple fields there and want to register multiple channels.
	 * Then you just call this method and parse the field into it from your onReloadablesStart method.
	 */
	protected final void registerBungeeCord(@NonNull SimpleBungee bungee) {
		final Messenger messenger = getServer().getMessenger();

		messenger.registerIncomingPluginChannel(this, bungee.getChannel(), bungee.getListener());
		messenger.registerOutgoingPluginChannel(this, bungee.getChannel());

		reloadables.registerEvents(bungee.getListener());
		Debugger.debug("bungee", "Registered BungeeCord listener on channel " + bungee.getChannel());
	}

	/**
	 * Scans your plugin and if your {@link Tool} or {@link SimpleEnchantment} class implements {@link Listener}
	 * and has "instance" method to be a singleton, your events are registered there automatically
	 * <p>
	 * If not, we only call the instance constructor in case there is any underlying registration going on
	 */
	private static void checkSingletons() {

		final Pattern anonymousClassPattern = Pattern.compile("\\w+\\$[0-9]$");

		try (final JarFile file = new JarFile(SimplePlugin.getSource())) {
			for (final Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements();) {
				final JarEntry jar = entry.nextElement();
				final String name = jar.getName().replace("/", ".");

				try {

					if (name.endsWith(".class")) {
						final String className = name.substring(0, name.length() - 6);
						Class<?> clazz = null;

						try {
							clazz = SimplePlugin.class.getClassLoader().loadClass(className);
						} catch (final NoClassDefFoundError | ClassNotFoundException | IncompatibleClassChangeError error) {
							continue;
						}

						if (Modifier.isAbstract(clazz.getModifiers()))
							continue;

						System.out.println("@@@ iterating over " + className);

						if (anonymousClassPattern.matcher(className).find()) {
							System.out.println("@ignoring anonymous inner class " + className);

							continue;
						}

						final boolean isTool = Tool.class.isAssignableFrom(clazz);
						final boolean isEnchant = SimpleEnchantment.class.isAssignableFrom(clazz);
						final boolean isExpansion = SimpleExpansion.class.isAssignableFrom(clazz);

						if (isTool || isEnchant || isExpansion) {

							if (isEnchant && MinecraftVersion.olderThan(V.v1_13)) {
								Bukkit.getLogger().warning("**** WARNING ****");
								Bukkit.getLogger().warning("SimpleEnchantment requires Minecraft 1.13.2 or greater. The following class will not be registered: " + clazz.getName());

								continue;
							}

							try {
								Field instanceField = null;

								for (final Field field : clazz.getDeclaredFields())
									if ((Tool.class.isAssignableFrom(field.getType()) || Enchantment.class.isAssignableFrom(field.getType()) || SimpleExpansion.class.isAssignableFrom(field.getType()))
											&& Modifier.isPrivate(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
										instanceField = field;

								for (final Method method : clazz.getDeclaredMethods())
									System.out.println("\tmethod " + method);

								Valid.checkNotNull(instanceField, "Your class " + className + " must be a singleton and have 'private static final " + clazz.getSimpleName()
										+ " instance' field and a private constructor!");

								if (instanceField != null) {
									instanceField.setAccessible(true);

									final Object instance = instanceField.get(null);

									// Enforce private constructors
									for (final Constructor<?> con : instance.getClass().getDeclaredConstructors())
										Valid.checkBoolean(Modifier.isPrivate(con.getModifiers()), "Constructor " + con + " not private! Did you put '@NoArgsConstructor(access = AccessLevel.PRIVATE)' in your "
												+ className + " class?");

									// Register if expansion
									if (isExpansion)
										Variables.addExpansion((SimpleExpansion) instance);

									// Finally register events
									if (instance instanceof Listener)
										Common.registerEvents((Listener) instance);

									System.out.println("@auto registered class " + clazz);
								}

							} catch (final NoClassDefFoundError | NoSuchFieldError ex) {
								Bukkit.getLogger().warning("Failed to auto register class " + clazz + " due to it requesting missing fields/classes: " + ex.getMessage());

								// Ignore if no field is present

							} catch (final Throwable t) {
								final String error = Common.getOrEmpty(t.getMessage());

								if (t instanceof NoClassDefFoundError && error.contains("org/bukkit/entity")) {
									Bukkit.getLogger().warning("**** WARNING ****");

									if (error.contains("DragonFireball"))
										Bukkit.getLogger().warning("Your Minecraft version does not have DragonFireball class, we suggest replacing it with a Fireball instead in: " + clazz);
									else
										Bukkit.getLogger().warning("Your Minecraft version does not have " + error + " class you call in: " + clazz);
								} else
									Common.error(t, "Failed to auto register class " + clazz);
							}
						}
					}

				} catch (final Throwable t) {
					if (t instanceof VerifyError) // Exception in other class we loaded
						continue;

					Common.error(t, "Failed to scan class '" + name + "' using Foundation!");
				}
			}
		} catch (final Throwable t) {
			Common.error(t, "Failed to scan classes using Foundation!");
		}
	}

	/**
	 * A dirty way of checking if Foundation has been shaded correctly
	 */
	private final void checkShading() {
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
	private final class ShadingException extends Throwable {
		private static final long serialVersionUID = 1L;

		public ShadingException() {
			if (!SimplePlugin.getNamed().equals(getDescription().getName())) {
				Bukkit.getLogger().severe(Common.consoleLine());
				Bukkit.getLogger().severe("We have a class path problem in the Foundation library");
				Bukkit.getLogger().severe("preventing " + getDescription().getName() + " from loading correctly!");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("This is likely caused by two plugins having the");
				Bukkit.getLogger().severe("same Foundation library paths - make sure you");
				Bukkit.getLogger().severe("relocale the package! If you are testing using");
				Bukkit.getLogger().severe("Ant, only test one plugin at the time.");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("Possible cause: " + SimplePlugin.getNamed());
				Bukkit.getLogger().severe("Foundation package: " + SimplePlugin.class.getPackage().getName());
				Bukkit.getLogger().severe(Common.consoleLine());

				isEnabled = false;
			}
		}
	}

	/**
	 * Check if both md5 chat and gson libraries are present,
	 * or suggest an additional plugin to fix their lack
	 *
	 * @return
	 */
	private final boolean checkLibraries0() {
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
			Bukkit.getLogger().severe("lacks libraries " + getName() + " needs:");
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
	private final boolean checkServerVersions0() {

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
		final V minimumVersion = getMinimumVersion();

		if (minimumVersion != null && MinecraftVersion.olderThan(minimumVersion)) {
			Common.logFramed(false,
					getName() + " requires Minecraft " + minimumVersion + " or newer to run.",
					"Please upgrade your server.");

			return false;
		}

		// Check max version
		final V maximumVersion = getMaximumVersion();

		if (maximumVersion != null && MinecraftVersion.newerThan(maximumVersion)) {
			Common.logFramed(false,
					getName() + " requires Minecraft " + maximumVersion + " or older to run.",
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

		Common.log(
				"&4    ___                  _ ",
				"&4   / _ \\  ___  _ __  ___| |",
				"&4  | | | |/ _ \\| '_ \\/ __| |",
				"&4  | |_| | (_) | |_) \\__ \\_|",
				"&4   \\___/ \\___/| .__/|___(_)",
				"&4             |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + getDescription().getName() + " v" + getDescription().getVersion() + ", plugin is disabled!",
				" &cRunning on " + getServer().getBukkitVersion() + " (" + MinecraftVersion.getServerVersion() + ") & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		if (throwable instanceof InvalidConfigurationException) {
			Common.log(" &cSeems like your config is not a valid YAML.");
			Common.log(" &cUse online services like");
			Common.log(" &chttp://yaml-online-parser.appspot.com/");
			Common.log(" &cto check for syntax errors!");

		} else if (throwable instanceof UnsupportedOperationException || throwable.getCause() != null && throwable.getCause() instanceof UnsupportedOperationException)
			if (getServer().getBukkitVersion().startsWith("1.2.5"))
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

		getPluginLoader().disablePlugin(this);
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {

		// If the early startup was interrupted, do not call shutdown methods
		if (!isEnabled)
			return;

		try {
			onPluginStop();
		} catch (final Throwable t) {
			Common.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		unregisterReloadables();

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

		Objects.requireNonNull(instance, "Instance of " + getName() + " already nulled!");
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
	 * Called before we start loading the plugin, but after {@link #onPluginLoad()}
	 */
	protected void onPluginPreStart() {
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
		final boolean hadLogPrefix = Common.ADD_LOG_PREFIX;
		Common.ADD_LOG_PREFIX = false;

		Common.log(Common.consoleLineSmooth());
		Common.log(" ");
		Common.log("Reloading plugin " + this.getName() + " v" + getVersion());
		Common.log(" ");

		reloading = true;

		try {
			Debugger.detectDebugMode();

			unregisterReloadables();

			// Load our dependency system
			try {
				HookManager.loadDependencies();

			} catch (final Throwable throwable) {
				Common.throwError(throwable, "Error while loading " + getName() + " dependencies!");
			}

			onPluginPreReload();
			reloadables.reload();

			YamlConfig.clearLoadedFiles();
			YamlStaticConfig.load(getSettings());

			CompMetadata.MetadataFile.onReload();

			FoundationPacketListener.addNativeListener();

			SimpleHologram.init();

			Lang.reloadFile();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);
			onPluginReload();

			// Something went wrong in the reload pipeline
			if (!isEnabled || !isEnabled())
				return;

			if (getMainCommand() != null)
				reloadables.registerCommands(SimpleSettings.MAIN_COMMAND_ALIASES, getMainCommand());

			startingReloadables = true;
			onReloadablesStart();
			startingReloadables = false;

			if (HookManager.isDiscordSRVLoaded()) {
				DiscordListener.DiscordListenerImpl.getInstance().resubscribe();

				reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			if (getBungeeCord() != null)
				registerBungeeCord(getBungeeCord());

			Common.log(Common.consoleLineSmooth());

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + getName() + " " + getVersion());

		} finally {
			Common.ADD_LOG_PREFIX = hadLogPrefix;

			reloading = false;
		}
	}

	private final void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		BlockVisualizer.stopAll();
		FolderWatcher.stopThreads();

		if (HookManager.isDiscordSRVLoaded())
			DiscordListener.clearRegisteredListeners();

		try {
			HookManager.unloadDependencies(this);
		} catch (final NoClassDefFoundError ex) {
		}

		getServer().getMessenger().unregisterIncomingPluginChannel(this);
		getServer().getMessenger().unregisterOutgoingPluginChannel(this);

		getServer().getScheduler().cancelTasks(this);
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
			for (final Constructor<?> con : pluginClass.getConstructors()) {
				if (con.getParameterCount() == 0) {
					final T instance = (T) ReflectionUtil.instantiate(con);

					Debugger.debug("auto-register", "Auto-registering events in " + pluginClass);
					registerEvents(instance);

					continue classLookup;
				}
			}

			Debugger.debug("auto-register", "Skipping auto-registering events in " + pluginClass + " because it lacks at least one no arguments constructor");
		}
	}

	/**
	 * Convenience method for quickly registering events if the condition is met
	 *
	 * @param listener
	 * @param condition
	 */
	protected final void registerEventsIf(final Listener listener, final boolean condition) {
		if (condition)
			if (startingReloadables)
				reloadables.registerEvents(listener);
			else
				registerEvents(listener);
	}

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listener
	 */
	protected final void registerEvents(final Listener listener) {
		if (startingReloadables)
			reloadables.registerEvents(listener);
		else
			getServer().getPluginManager().registerEvents(listener, this);

		if (listener instanceof DiscordListener)
			((DiscordListener) listener).register();
	}

	/**
	 * Convenience method for quickly registering an event if the condition is met
	 *
	 * @param listener
	 * @param condition
	 */
	protected final void registerEventsIf(final SimpleListener<? extends Event> listener, final boolean condition) {
		if (condition)
			if (startingReloadables)
				reloadables.registerEvents(listener);
			else
				registerEvents(listener);
	}

	/**
	 * Convenience method for quickly registering a single event
	 *
	 * @param listener
	 */
	protected final void registerEvents(final SimpleListener<? extends Event> listener) {
		if (startingReloadables)
			reloadables.registerEvents(listener);

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

			if (SimpleSubCommand.class.isAssignableFrom(pluginClass)) {
				Debugger.debug("auto-register", "Skipping auto-registering command " + pluginClass + " because sub-commands cannot be registered");

				continue;
			}

			try {
				for (final Constructor<?> con : pluginClass.getConstructors()) {
					if (con.getParameterCount() == 0) {
						final T instance = (T) ReflectionUtil.instantiate(con);

						Debugger.debug("auto-register", "Auto-registering command " + pluginClass);

						if (instance instanceof SimpleCommand)
							registerCommand((SimpleCommand) instance);

						else
							registerCommand(instance);

						continue classLookup;
					}
				}

			} catch (final LinkageError ex) {
				Common.log("Unable to register commands in '" + pluginClass.getSimpleName() + "' due to error: " + ex);
			}

			Debugger.debug("auto-register", "Skipping auto-registering command " + pluginClass + " because it lacks at least one no arguments constructor");
		}
	}

	/**
	 * Convenience method for registering a bukkit command
	 *
	 * @param command
	 */
	protected final void registerCommand(final Command command) {
		Remain.registerCommand(command);
	}

	/**
	 * Convenience shortcut for calling the register method in {@link SimpleCommand}
	 *
	 * @param command
	 */
	protected final void registerCommand(final SimpleCommand command) {
		command.register();
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register(String, List))}
	 *
	 * @param labelAndAliases
	 * @param group
	 */
	protected final void registerCommands(final String labelAndAliases, final SimpleCommandGroup group) {
		this.registerCommands(new StrictList<>(labelAndAliases.split("\\|")), group);
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register(StrictList)}
	 *
	 * @param labelAndAliases
	 * @param group
	 */
	protected final void registerCommands(final StrictList<String> labelAndAliases, final SimpleCommandGroup group) {
		Valid.checkBoolean(!labelAndAliases.isEmpty(), "Must specify at least label for command group: " + group);

		if (getMainCommand() != null && getMainCommand().getLabel().equals(labelAndAliases.get(0)))
			throw new FoException("Your main command group is registered automatically!");

		reloadables.registerCommands(labelAndAliases, group);
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
	 * Return your main setting classes extending {@link YamlStaticConfig}.
	 * <p>
	 * TIP: Extend {@link SimpleSettings} and {@link SimpleLocalization}
	 *
	 * @return
	 */
	public List<Class<? extends YamlStaticConfig>> getSettings() {
		return null;
	}

	/**
	 * Get your main command group, e.g. for ChatControl it's /chatcontrol
	 *
	 * @return
	 */
	public SimpleCommandGroup getMainCommand() {
		return null;
	}

	/**
	 * Get the year of foundation displayed in {@link #getMainCommand()}
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
	 * Return the BungeeCord suite if you want this plugin
	 * to send and receive messages from BungeeCord
	 *
	 * @return
	 */
	public SimpleBungee getBungeeCord() {
		return null;
	}

	/**
	 * Should every message be divided by \n by an own method (tends to work more
	 * then split("\n"))
	 *
	 * @return
	 */
	public boolean enforeNewLine() {
		return false;
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
	 * Use JavaScript variables/javascript.txt file
	 *
	 * @return
	 * @deprecated this feature has been removed
	 */
	@Deprecated
	public boolean areScriptVariablesEnabled() {
		return false;
	}

	// ----------------------------------------------------------------------------------------
	// Prevention
	// ----------------------------------------------------------------------------------------

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
		throw unsupported("onCommand");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
		throw unsupported("onTabComplete");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final FileConfiguration getConfig() {
		throw unsupported("getConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void saveConfig() {
		throw unsupported("saveConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void saveDefaultConfig() {
		throw unsupported("saveDefaultConfig");
	}

	/**
	 * @deprecated do not use
	 */
	@Deprecated
	@Override
	public final void reloadConfig() {
		throw new FoException("Cannot call reloadConfig in " + getName() + ", use reload()!");
	}

	private final FoException unsupported(final String method) {
		return new FoException("Cannot call " + method + " in " + getName() + ", use YamlConfig or SimpleCommand classes in Foundation for that!");
	}
}
