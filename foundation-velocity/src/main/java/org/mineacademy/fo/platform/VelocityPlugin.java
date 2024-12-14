package org.mineacademy.fo.platform;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.library.LibraryManager;
import org.mineacademy.fo.library.VelocityLibraryManager;
import org.mineacademy.fo.model.BStatsVelocity;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;

/**
 * Represents a Velocity plugin.
 */
public abstract class VelocityPlugin implements FoundationPlugin {

	/**
	 * Stores the legacy BungeeCord channel identifier
	 */
	public static final LegacyChannelIdentifier LEGACY_BUNGEE_CHANNEL = new LegacyChannelIdentifier("BungeeCord");
	public static final MinecraftChannelIdentifier MODERN_BUNGEE_CHANNEL = MinecraftChannelIdentifier.create("bungeecord", "main");

	/**
	 * The instance of this plugin
	 */
	private static VelocityPlugin instance;

	/**
	 * Returns the instance of {@link VelocityPlugin}.
	 * <p>
	 * It is recommended to override this in your own {@link VelocityPlugin}
	 * implementation so you will get the instance of that, directly.
	 *
	 * @return this instance
	 */
	public static VelocityPlugin getInstance() {
		return instance;
	}

	/**
	 * Returns the server instance
	 *
	 * @return
	 */
	public static ProxyServer getServer() {
		return getInstance().proxy;
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
	 * The proxy server
	 */
	private ProxyServer proxy;

	/**
	 * The proxy logger
	 */
	private Logger logger;

	/**
	 * The path data
	 */
	private File dataFolder;

	/**
	 * Shortcut for getFile()
	 */
	private File file;

	/**
	 * The plugin version
	 */
	private String version;

	/**
	 * The plugin name
	 */
	private String name;

	/**
	 * The plugin authors
	 */
	private List<String> authors;

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

	public VelocityPlugin(final ProxyServer proxy, final Logger logger, @DataDirectory final Path dataDirectory) {
		instance = this;

		try {
			this.file = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

			// Hacky due to Velocity lacking simpler implementation
			final Plugin annotation = this.getClass().getDeclaredAnnotation(Plugin.class);

			if (annotation != null) {
				this.version = annotation.version();
				this.name = annotation.name();
				this.authors = Arrays.asList(annotation.authors());

			} else {

				// If annotation isn't used, try to load from velocity-plugin.json directly. You can place this file to your src/main/resources and use variables in it.
				final List<String> lines = FileUtil.readLinesFromInternalPath(this.getFile(), "velocity-plugin.json");
				ValidCore.checkBoolean(lines != null, "Either place @Plugin annotation over your main class or write velocity-plugin.json to your resources folder!");

				final JsonObject json = CommonCore.GSON.fromJson(String.join("", lines), JsonObject.class);

				this.version = json.get("version").getAsString();
				this.name = json.get("name").getAsString();
				this.authors = new ArrayList<>();

				if (json.has("authors")) {
					final JsonElement authors = json.get("authors");

					if (authors.isJsonArray())
						for (final JsonElement author : authors.getAsJsonArray())
							this.authors.add(author.getAsString());
					else
						this.authors.addAll(Arrays.asList(authors.getAsString()));
				}
			}

			ValidCore.checkBoolean(this.version != null && !this.version.contains("${project.version}"), "Invalid plugin version: " + this.version);
			ValidCore.checkBoolean(this.name != null && !this.name.contains("${project.name}"), "Invalid plugin name: " + this.name);

			this.proxy = proxy;
			this.logger = logger;
			this.dataFolder = new File(dataDirectory.toFile().getParentFile(), this.name); // Another hack to prevent lowercase folders

			// Call delegate
			this.onPluginLoad();

		} catch (final Throwable t) {
			this.loadingFailed = true;
			this.enabled = false;

			t.printStackTrace();

		} finally {
			this.initializing = false;
		}
	}

	@Subscribe
	public final void onProxyInitialization(final ProxyInitializeEvent event) {
		if (this.loadingFailed)
			return;

		FoundationLibraries.load(this);
		VelocityPlatform.inject();

		try {
			if (this.getStartupLogo() != null)
				CommonCore.log(this.getStartupLogo());

			// Register the proxy listener and channel
			this.proxy.getChannelRegistrar().register(LEGACY_BUNGEE_CHANNEL, MODERN_BUNGEE_CHANNEL);

			this.registerEvents(new VelocityListener());

			// Scan for @AutoRegister annotations
			AutoRegisterScanner.scanAndRegister();

			this.onPluginStart();

			// Return if plugin start indicated a fatal problem
			if (!this.enabled)
				return;

			if (this.getBStatsPluginId() != -1)
				new BStatsVelocity(this, this.proxy, this.logger, this.dataFolder.toPath(), this.getBStatsPluginId());

			this.internalPostEnable();

		} catch (final Throwable t) {
			this.displayError(t);
		}
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Subscribe
	public final void onProxyShutdown(final ProxyShutdownEvent event) {
		if (this.loadingFailed)
			return;

		try {
			this.onPluginStop();

		} catch (final Throwable t) {
			CommonCore.warning("Plugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		Objects.requireNonNull(instance, "Instance of " + this.dataFolder.getName() + " already nulled!");
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
			this.proxy.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);

			this.onPluginPreReload();

			AutoRegisterScanner.reloadSettings();

			this.onPluginReload();

			this.internalPostEnable();

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
		this.proxy.getEventManager().unregisterListeners(this);
		this.proxy.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);

		if (this.defaultCommandGroup != null)
			this.proxy.getCommandManager().unregister(this.defaultCommandGroup.getLabel());

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
			this.libraryManager = new VelocityLibraryManager(this, this.dataFolder.toPath(), this.proxy.getPluginManager());

		return this.libraryManager;
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * Return true if the plugin has not yet reached onPluginStart() method.
	 *
	 * @return
	 */
	@Override
	public final boolean isInitializing() {
		return this.initializing;
	}

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
	 * Return the proxy server (aka Bukkit#getServer())
	 *
	 * @return
	 */
	public final ProxyServer getProxy() {
		return this.proxy;
	}

	/**
	 * Get the plugin's logger.
	 * @return
	 */
	public final Logger getLogger() {
		return this.logger;
	}

	// ----------------------------------------------------------------------------------------
	// Overriding parent methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Return the data folder
	 *
	 * @return
	 */
	@Override
	public final File getDataFolder() {
		return this.dataFolder;
	}

	/**
	 * Get the plugins jar file.
	 */
	@Override
	public final File getFile() {
		return this.file;
	}

	/**
	 * Get the plugin's version.
	 */
	@Override
	public final String getVersion() {
		return this.version;
	}

	/**
	 * Get the plugin's name.
	 *
	 * @return
	 */
	@Override
	public final String getName() {
		return this.name;
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
		return String.join(", ", this.authors);
	}
}
