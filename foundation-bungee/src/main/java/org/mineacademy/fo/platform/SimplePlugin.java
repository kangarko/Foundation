package org.mineacademy.fo.platform;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.database.SimpleDatabase;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.HandledException;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.library.BungeeLibraryManager;
import org.mineacademy.fo.library.Library;
import org.mineacademy.fo.library.LibraryManager;
import org.mineacademy.fo.model.BStatsBungee;
import org.mineacademy.fo.model.BuiltByBitUpdateCheck;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.mineacademy.fo.settings.SimpleSettings;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Represents a Velocity plugin.
 */
public abstract class SimplePlugin extends Plugin implements FoundationPlugin {

	/**
	 * Stores the BungeeCord channel identifier
	 */
	public static final String BUNGEE_CHANNEL = "BungeeCord";

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
		return instance;
	}

	/**
	 * Returns the server instance
	 *
	 * @return
	 */
	public static ProxyServer getServer() {
		return ProxyServer.getInstance();
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

	/**
	 * false = error has occurred early in loading pipeline so we skip onDisable since there is no data
	 */
	private boolean loadingFailed = false;

	/**
	 * Shortcut to discover if the plugin was disabled (only used internally)
	 */
	private boolean enabled = true;

	/**
	 * Shortcut to discover if the plugin is initializing
	 */
	private boolean initializing = true;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	@Override
	public final void onLoad() {
		instance = this;

		this.loadLibrary("org.snakeyaml", "snakeyaml-engine", "2.8");

		if (!ReflectionUtil.isClassAvailable("org.openjdk.nashorn.api.scripting.NashornScriptEngine"))
			this.loadLibrary("org.openjdk.nashorn", "nashorn-core", "15.4");

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

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer"))
			this.loadLibrary("net.kyori", "adventure-text-serializer-bungeecord", "4.3.4");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.minimessage.MiniMessage"))
			this.loadLibrary("net.kyori", "adventure-text-minimessage", "4.17.0");

		BungeePlatform.inject();

		// Dynamically load filters
		for (final Class<? extends Filter> filterClass : ReflectionUtil.getClasses(this.getFile(), Filter.class))
			try {
				final Constructor<? extends Filter> constructor = ReflectionUtil.getConstructor(filterClass);
				Valid.checkBoolean(constructor.getParameterCount() == 0, "Filter class " + filterClass + " must have a public no args constructor!");
				Valid.checkBoolean(Modifier.isPublic(constructor.getModifiers()), "Filter class " + filterClass + " must have a public constructor!");

				final Filter filter = ReflectionUtil.instantiate(constructor);

				Filter.register(filter.getIdentifier(), filter);

			} catch (final Exception ex) {
				Common.error(ex,
						"Failed to load filter: " + filterClass,
						"Check that it has a public no args constructor!");

				continue;
			}

		// Call delegate
		try {
			this.onPluginLoad();

		} catch (final Throwable t) {
			this.loadingFailed = true;
			this.enabled = false;

			throw t;

		} finally {
			this.initializing = false;
		}
	}

	@Override
	public final void onEnable() {
		if (this.loadingFailed)
			return;

		try {
			if (this.getStartupLogo() != null)
				Common.log(this.getStartupLogo());

			// Register the proxy listener and channel
			this.registerEvents(new ForwardingListener());

			if (!this.getProxy().getChannels().contains("BungeeCord"))
				this.getProxy().registerChannel("BungeeCord");

			// Scan for @AutoRegister annotations
			AutoRegisterScanner.scanAndRegister();

			this.onPluginStart();

			// Return if plugin start indicated a fatal problem
			if (!this.enabled)
				return;

			// Move the legacy localization folder to unused
			{
				final File localizationFolder = new File(this.getDataFolder(), "localization");

				if (localizationFolder.exists()) {
					Common.warning("The localization/ folder is now unused, run '/" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " dumplocale' to download the new locale format. Moving to unused/ ...");

					final File unusedFolder = new File(this.getDataFolder(), "unused");

					if (!unusedFolder.exists())
						unusedFolder.mkdirs();

					localizationFolder.renameTo(new File(unusedFolder, "localization"));
				}
			}

			Platform.runTaskTimerAsync(20, SimpleDatabase.RowQueueWriter.getInstance());

			if (this.getBStatsPluginId() != -1)
				new BStatsBungee(this, this.getBStatsPluginId());

			if (SimpleSettings.NOTIFY_NEW_VERSIONS)
				Platform.runTaskAsync(new BuiltByBitUpdateCheck());

		} catch (final Throwable t) {
			this.displayError(t);
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
				" &cRunning on " + getServer().getVersion() + " (" + getServer().getGameVersion() + ") & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		if (throwable instanceof HandledException)
			throwable = ((HandledException) throwable).getHandle();

		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		if (!(throwable instanceof HandledException)) {
			String error = "Unable to get the error message, search above.";
			if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
				error = throwable.getMessage();

			Common.log(" &cError: " + error);
		} else
			Common.log(" &cError: See above for stack trace.");

		Common.log("&4!-----------------------------------------------------!");

		this.enabled = false;
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {
		if (this.loadingFailed)
			return;

		try {
			this.onPluginStop();

		} catch (final Throwable t) {
			Common.warning("Plugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
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
			getServer().getScheduler().cancel(this);

			this.onPluginPreReload();

			AutoRegisterScanner.reloadSettings();

			this.onPluginReload();

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
		getServer().getPluginManager().unregisterListeners(this);
		getServer().getPluginManager().unregisterCommands(this);
		getServer().getScheduler().cancel(this);

		this.enabled = false;
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
		ValidCore.checkBoolean(!this.initializing, "Cannot register events during plugin initialization! Use onPluginStart() instead.");

		getServer().getPluginManager().registerListener(this, listener);
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
		ValidCore.checkBoolean(!this.initializing, "Cannot register commands during plugin initialization! Use onPluginStart() instead.");

		command.register();
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register()}
	 *
	 * @param group
	 */
	@Override
	public final void registerCommands(final SimpleCommandGroup group) {
		ValidCore.checkBoolean(!this.initializing, "Cannot register commands during plugin initialization! Use onPluginStart() instead.");

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
	 * Get the default proxy used in {@link OutgoingMessage} when no group is provided.
	 *
	 * @return
	 */
	@Override
	public final ProxyListener getDefaultProxyListener() {
		return this.defaultProxyListener;
	}

	/**
	 * Set the default proxy used in {@link OutgoingMessage} when no group is provided.
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
		this.getLibraryManager().loadLibrary(Library
				.builder()
				.groupId(groupId)
				.artifactId(artifactId)
				.resolveTransitiveDependencies(true)
				.version(version)
				.build());
	}

	/**
	 * Get the Libby library manager
	 *
	 * @return
	 */
	public final LibraryManager getLibraryManager() {
		if (this.libraryManager == null)
			this.libraryManager = new BungeeLibraryManager(this);

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

	/**
	 * @see FoundationPlugin#getBuiltByBitId()
	 */
	@Override
	public int getBuiltByBitId() {
		return -1;
	}

	/**
	 * @see FoundationPlugin#getBuiltByBitSharedToken()
	 */
	@Override
	public String getBuiltByBitSharedToken() {
		return null;
	}

	// ----------------------------------------------------------------------------------------
	// Overriding parent methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Return false if plugin was disabled during startup/reload
	 *
	 * @return
	 */
	@Override
	public final boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Get the plugins jar file.
	 */
	@Override
	public final File getFile() {
		return super.getFile();
	}

	/**
	 * Get the plugin's version.
	 */
	@Override
	public final String getVersion() {
		return this.getDescription().getVersion();
	}

	@Override
	public final String getName() {
		return this.getDescription().getName();
	}

	/**
	 * Get the plugin's class loader.
	 */
	@Override
	public final ClassLoader getPluginClassLoader() {
		return this.getClass().getClassLoader();
	}

	/**
	 * Get the plugin's authors, joined by a comma
	 */
	@Override
	public final String getAuthors() {
		return this.getDescription().getAuthor();
	}
}
