package org.mineacademy.fo.platform;

import java.io.File;
import java.util.Objects;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.library.BungeeLibraryManager;
import org.mineacademy.fo.library.LibraryManager;
import org.mineacademy.fo.model.BStatsBungee;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.message.OutgoingMessage;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Represents a Velocity plugin.
 */
public abstract class BungeePlugin extends Plugin implements FoundationPlugin {

	/**
	 * Stores the BungeeCord channel identifier
	 */
	public static final String BUNGEE_CHANNEL = "BungeeCord";

	/**
	 * The instance of this plugin
	 */
	private static BungeePlugin instance;

	/**
	 * Returns the instance of {@link BungeePlugin}.
	 * <p>
	 * It is recommended to override this in your own {@link BungeePlugin}
	 * implementation so you will get the instance of that, directly.
	 *
	 * @return this instance
	 */
	public static BungeePlugin getInstance() {
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

	@Override
	public final void onLoad() {
		instance = this;

		try {
			this.setVersion();

			FoundationLibraries.load(this);

			if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.minimessage.MiniMessage"))
				this.loadLibrary("net.kyori", "adventure-text-minimessage", "4.26.1");

			this.loadLibrary("net.kyori", "adventure-platform-bungeecord", "4.4.1");

			BungeePlatform.inject();

			// Call delegate
			this.onPluginLoad();

		} catch (final Throwable t) {
			this.loadingFailed = true;
			this.enabled = false;

			throw t;

		} finally {
			this.initializing = false;
		}
	}

	/*
	 * Set the version of the server we are running on.
	 */
	private void setVersion() {
		String bungeeVersion = BungeePlugin.getServer().getVersion();

		if (bungeeVersion.startsWith("git:")) {
			final String[] split = bungeeVersion.split("\\:");

			ValidCore.checkBoolean(split.length > 1, "Unsupported platform (BungeeCord or Waterfall is supported): " + bungeeVersion);
			bungeeVersion = split[2];

			MinecraftVersion.parseAndSet(bungeeVersion);

		} else
			this.getLogger().warning("Unsupported platform '" + bungeeVersion + "'. Only BungeeCord or Waterfall are officially supported. If issues arise we will not be able to provide support.");
	}

	@Override
	public final void onEnable() {
		if (this.loadingFailed)
			return;

		BungeePlatform.createAudiences(this);

		try {
			if (this.getStartupLogo() != null)
				CommonCore.log(this.getStartupLogo());

			// Register the proxy listener and channel
			this.registerEvents(new BungeeListener());

			if (!this.getProxy().getChannels().contains("BungeeCord"))
				this.getProxy().registerChannel("BungeeCord");

			// Scan for @AutoRegister annotations
			AutoRegisterScanner.scanAndRegister();

			this.onPluginStart();

			// Return if plugin start indicated a fatal problem
			if (!this.enabled)
				return;

			if (this.getBStatsPluginId() != -1)
				new BStatsBungee(this, this.getBStatsPluginId());

			this.internalPostEnable();

		} catch (final Throwable t) {
			this.displayError(t);
		}
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {
		if (this.loadingFailed)
			return;

		this.enabled = false;

		getServer().getScheduler().cancel(this);

		BungeePlatform.closeAudiences();

		try {
			this.onPluginStop();

		} catch (final Throwable t) {
			CommonCore.warning("Plugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
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
			this.onPluginPreReload();

			AutoRegisterScanner.reloadSettings();

			this.onPluginReload();

		} catch (final Throwable t) {
			CommonCore.throwError(t, "Error reloading " + this.getName() + " " + this.getVersion());
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
	public final void setDefaultCommandGroup(final SimpleCommandGroup group) {
		ValidCore.checkBoolean(this.defaultCommandGroup == null, "Main command has already been set to " + this.defaultCommandGroup);

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
	public final void setDefaultProxyListener(final ProxyListener listener) {
		this.defaultProxyListener = listener;
	}

	// ----------------------------------------------------------------------------------------
	// Library manager
	// ----------------------------------------------------------------------------------------

	/**
	 * Get the Libby library manager
	 *
	 * @return
	 */
	@Override
	public final LibraryManager getLibraryManager() {
		if (this.libraryManager == null)
			this.libraryManager = new BungeeLibraryManager(this);

		return this.libraryManager;
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * Return false if plugin was disabled during startup/reload
	 *
	 * @return
	 */
	@Override
	public final boolean isPluginEnabled() {
		return this.enabled;
	}

	/**
	 * Return true if the plugin has not yet reached onPluginStart() method.
	 */
	@Override
	public final boolean isInitializing() {
		return this.initializing;
	}

	// ----------------------------------------------------------------------------------------
	// Overriding parent methods
	// ----------------------------------------------------------------------------------------

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

	/**
	 * Get the plugin's name.
	 */
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
