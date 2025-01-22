package org.mineacademy.fo.platform;

import java.io.File;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ProxyUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
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
import org.mineacademy.fo.model.BStatsBukkit;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.LitebanTask;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.AutoRegisterScanner.AutoRegisterHandler;
import org.mineacademy.fo.platform.AutoRegisterScanner.FindInstance;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;

/**
 * Represents a Bukkit plugin.
 *
 * This class extends {@link JavaPlugin} and plugin
 * authors should extend this class when creating a new plugin.
 */
public abstract class BukkitPlugin extends JavaPlugin implements Listener, FoundationPlugin {

	/**
	 * The instance of this plugin
	 */
	private static BukkitPlugin instance;

	/**
	 * Returns the instance of {@link BukkitPlugin}.
	 * <p>
	 * It is recommended to override this in your own {@link BukkitPlugin}
	 * implementation so you will get the instance of that, directly.
	 *
	 * @return this instance
	 */
	public static BukkitPlugin getInstance() {
		if (instance == null) {
			try {
				instance = JavaPlugin.getPlugin(BukkitPlugin.class);

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

	/**
	 * Shortcut to discover if the plugin is initializing
	 */
	private boolean initializing = true;

	/**
	 * false = error has occurred early in loading pipeline so we skip onDisable since there is no data
	 */
	private boolean loadingFailed = false;

	/**
	 * Bukkit has an inconsistency where if we disable the plugin through plugin manager
	 * it will still report as loaded, so we will continue registering events which will crash.
	 * This flag mitigates that issue.
	 */
	private boolean starting = false;
	private boolean platformEnabled = true;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		// TODO Temporarily disabled, investigating https://github.com/kangarko/ChatControl/issues/3005
		/*BukkitConsoleFilter.inject(filter -> {
			for (final Plugin plugin : Bukkit.getPluginManager().getPlugins())
				plugin.getLogger().setFilter(filter);
		
			Bukkit.getLogger().setFilter(filter);
		});*/
	}

	@Override
	public final void onLoad() {
		try {
			getInstance();

			this.setVersion();

			// Avoid issues with shading by using a different version for legacy
			if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.minimessage.MiniMessage"))

				// Pre-merge: 1.16-1.17
				if (ReflectionUtil.isClassAvailable("net.kyori.adventure.audience.Audience")) {
					String version = "4.2.0";

					try {
						Component.class.getMethod("compact");

					} catch (final ReflectiveOperationException ex) {
						version = "4.1.0";
					}

					this.loadLibrary(Library.builder()
							.groupId("net.kyori")
							.artifactId("adventure-text-minimessage")
							.version(version)
							.url("https://bitbucket.org/kangarko/libraries/raw/master/org/mineacademy/library/adventure-text-minimessage/" + version + "/adventure-text-minimessage-" + version + ".jar")
							.build());

				} else
					this.loadLibrary("net.kyori", "adventure-text-minimessage", "4.17.0");

			FoundationLibraries.load(this);

			BukkitPlatform.inject();

			this.loadLibrary("net.kyori", "adventure-platform-bukkit", "4.3.4");

			this.onPluginLoad();

		} catch (final Throwable t) {
			this.loadingFailed = true;
			this.setEnabled(false);

			throw t;

		} finally {
			this.initializing = false;
		}
	}

	/*
	 * Set the game version based on the Bukkit version.
	 */
	private void setVersion() {
		final String bukkitVersion = Bukkit.getBukkitVersion(); // 1.22.1-R0.1-SNAPSHOT
		final String versionString = bukkitVersion.split("\\-")[0]; // 1.22.1
		final String[] versions = versionString.split("\\.");
		ValidCore.checkBoolean(versions.length == 2 || versions.length == 3, "Foundation cannot read Bukkit version '" + bukkitVersion + "', expected '-' and a version number");

		final int version = Integer.parseInt(versions[1]); // 20

		final MinecraftVersion.V current = version <= 3 ? V.v1_3_AND_BELOW : V.parse(version);
		final int subversion = versions.length == 3 ? Integer.parseInt(versions[2]) : 0;

		MinecraftVersion.setVersion(current, subversion);
	}

	@Override
	public final void onEnable() {
		if (this.loadingFailed)
			return;

		//this.scanModernEnumsForUpdates();
		//this.scanEnumsForErrors();

		// Check if Foundation is correctly moved
		this.checkShading();

		// Check the required Minecraft server version
		if (MinecraftVersion.olderThan(V.v1_8)) {
			CommonCore.logFramed(false,
					this.getDataFolder().getName() + " requires Minecraft 1.8.8 or newer to run.",
					"Please upgrade your server.");

			this.setEnabled(false);
			return;
		}

		try {
			this.starting = true;

			if (this.getStartupLogo() != null)
				CommonCore.log(this.getStartupLogo());

			// Outdated adventure
			if (!MinecraftVersion.equals(MinecraftVersion.V.v1_16))
				BukkitPlatform.createAudiences(this);

			// Expand auto register functionality
			AutoRegisterScanner.setCustomRegisterHandler(new AutoRegisterHandler() {

				// Prevent duplicating registering of our {@link FoundationPacketListener}
				private boolean enchantListenersRegistered = false;

				@Override
				public void onPreScan() {
					this.enchantListenersRegistered = false;
				}

				@Override
				public boolean isIgnored(final Class<?> clazz, final boolean printWarnings) {
					if (DiscordListener.class.isAssignableFrom(clazz) && !HookManager.isDiscordSRVLoaded()) {
						if (printWarnings) {
							CommonCore.warning("**** WARNING ****");
							CommonCore.warning("The following class requires DiscordSRV and won't be registered: " + clazz.getSimpleName()
									+ ". To hide this message, put @AutoRegister(hideIncompatibilityWarnings=true) over the class.");
						}

						return true;
					}

					if (PacketListener.class.isAssignableFrom(clazz) && !HookManager.isProtocolLibLoaded()) {
						if (printWarnings && !clazz.equals(BukkitPacketListener.class)) {
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

					if (clazz == RegionTool.class && (!SimpleSettings.REGISTER_REGIONS || !SimpleSettings.REGISTER_TOOLS))
						return true;

					return false;
				}

				@Override
				public boolean canAutoRegister(final Class<?> clazz) {
					return Tool.class.isAssignableFrom(clazz) || SimpleEnchantment.class.isAssignableFrom(clazz) || PacketListener.class.isAssignableFrom(clazz) || DiscordListener.class.isAssignableFrom(clazz);
				}

				@Override
				public boolean autoRegister(final Class<?> clazz, final Tuple<FindInstance, Object> tuple) {

					final FindInstance mode = tuple.getKey();
					final Object instance = tuple.getValue();

					if (SimpleListener.class.isAssignableFrom(clazz)) {
						this.enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						return true;
					}

					else if (PacketListener.class.isAssignableFrom(clazz)) {
						// Automatically registered by means of adding packet adapters
						this.enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						((PacketListener) instance).onRegister();

						return true;
					}

					else if (DiscordListener.class.isAssignableFrom(clazz)) {
						// Automatically registered in its constructor
						this.enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						((DiscordListener) instance).register();

						return true;

					} else if (SimpleEnchantment.class.isAssignableFrom(clazz)) {
						// Automatically registered in its constructor
						this.enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						if (!this.enchantListenersRegistered) {
							this.enchantListenersRegistered = true;

							BukkitPlugin.this.registerEvents(SimpleEnchantment.Listener.getInstance());

							if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null)
								BukkitPacketListener.getInstance().onRegister();
							else
								CommonCore.warning("Custom enchantments require ProtocolLib for lore to be added properly.");
						}

						return true;
					}

					else if (Tool.class.isAssignableFrom(clazz)) {
						// Automatically registered in its constructor that is called when we find instance
						this.enforceModeFor(clazz, mode, FindInstance.SINGLETON);

						return true;

					}

					if (instance instanceof Listener) {
						BukkitPlugin.this.registerEvents(instance);

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
				messenger.registerIncomingPluginChannel(this, ProxyListener.DEFAULT_CHANNEL, BukkitPluginMessage.getInstance());

			if (!messenger.isOutgoingChannelRegistered(this, ProxyListener.DEFAULT_CHANNEL))
				messenger.registerOutgoingPluginChannel(this, ProxyListener.DEFAULT_CHANNEL);

			this.onPluginPreStart();

			if (!this.isEnabled() || !this.platformEnabled)
				return;

			// Scan for @AutoRegister annotations
			AutoRegisterScanner.scanAndRegister();

			if (SimpleSettings.REGISTER_REGIONS) {
				Valid.checkBoolean(DiskRegion.hasCreatedPlayerRegionGetter(), "Alert author of " + this.getName() + " to call DiskRegion#setCreatedPlayerRegionGetter in onPluginLoad()");
				Valid.checkBoolean(DiskRegion.hasCreatedPlayerRegionResetter(), "Alert author of " + this.getName() + " to call DiskRegion#sasCreatedPlayerRegionResetter in onPluginLoad()");

				// Load delayed to fix issues with multiverse plugins
				Platform.runTask(DiskRegion::loadRegions);
			}

			if (!this.isEnabled() || !this.platformEnabled)
				return;

			this.onPluginStart();

			// Freeze enchant registry - must be called after onPluginStart
			if (Remain.isEnchantRegistryUnfrozen())
				Remain.freezeEnchantRegistry();

			// Register our listeners
			this.registerEvents(this);
			this.registerEvents(new BukkitListener());

			if (SimpleSettings.REGISTER_MENUS)
				this.registerEvents(new MenuListener());

			if (SimpleSettings.REGISTER_TOOLS)
				this.registerEvents(new ToolsListener());

			if (HookManager.isPlaceholderAPILoaded())
				this.registerEvents(new PlaceholderHookListener());

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded()) {
				final DiscordListener.DiscordListenerImpl discord = DiscordListener.DiscordListenerImpl.getInstance();

				discord.resubscribe();
				discord.registerHook();

				this.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			if (HookManager.isPlaceholderAPILoaded() && this.useFullPlaceholderAPIParser())
				Variables.setLegacyPlaceholderAPIparser((audience, message) -> {
					final OfflinePlayer player = audience != null && audience.isPlayer() ? audience.getPlayer() : null;

					try {
						message = PlaceholderAPI.setPlaceholders(player, message);
						message = PlaceholderAPI.setBracketPlaceholders(player, message);

					} catch (final Throwable t) {
						Common.logFramed(
								"PlaceholderAPI failed to replace variables!",
								"Message: '" + message + "'",
								"Player: " + audience,
								"",
								"THIS IS IN 99% CASES NOT OUR FAULT. Alert their",
								"their developers to fix it first, as in most",
								"cases they need to properly account for null",
								"player sender or fix their own errors first");

						t.printStackTrace();
					}

					return message;
				});

			if (HookManager.isLiteBansLoaded())
				Platform.runTaskTimerAsync(20 * 2, LitebanTask.getInstance());

			if (this.getBStatsPluginId() != -1)
				new BStatsBukkit(this, this.getBStatsPluginId());

			this.internalPostEnable();

		} catch (final Throwable t) {
			this.displayError(t);

		} finally {
			this.starting = false;
		}
	}

	/*private void scanEnumsForErrors() {
		for (final CompAttribute comp : CompAttribute.values())
			try {
				CompAttribute.valueOf(comp.name());

			} catch (final IllegalArgumentException ex) {
				if (comp.getNmsName() != null)
					Common.log("Invalid CompAttribute " + comp.name());
			}

		for (final CompColor comp : CompColor.values())
			try {
				if (comp.getDye() == null)
					throw new IllegalArgumentException();

			} catch (final IllegalArgumentException ex) {
				Common.log("Invalid CompColor " + comp.getName());
			}

		for (final CompItemFlag comp : CompItemFlag.values())
			try {
				ItemFlag.valueOf(comp.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Invalid CompItemFlag " + comp);
			}

		for (final CompMaterial comp : CompMaterial.values())
			try {
				if (comp.toItem() == null)
					throw new IllegalArgumentException();

			} catch (final IllegalArgumentException ex) {
				Common.log("Invalid CompMaterial " + comp);
			}

		for (final CompParticle comp : CompParticle.values())
			try {
				Particle.valueOf(comp.name());

			} catch (final NoClassDefFoundError err) {
				// Skip

			} catch (final IllegalArgumentException ex) {
				if (!comp.isRemoved())
					Common.log("Invalid CompParticle " + comp);
			}

		if (MinecraftVersion.atLeast(V.v1_21))
			for (final CompSound comp : CompSound.values())
				try {
					Sound.valueOf(comp.name());

				} catch (final IllegalArgumentException ex) {
					Common.log("Invalid CompSound " + comp.name());
				}

		for (final CompVillagerProfession comp : CompVillagerProfession.values())
			try {
				comp.toBukkit();

			} catch (final NoClassDefFoundError err) {
				// Ignore

			} catch (final MissingEnumException ex) {
				Common.log("Invalid CompVillagerProfession " + comp);
			}

		for (final CompVillagerType comp : CompVillagerType.values())
			try {
				comp.toBukkit();

			} catch (final NoClassDefFoundError err) {
				// Ignore

			} catch (final MissingEnumException ex) {
				Common.log("Invalid CompVillagerType " + comp);
			}
	}*/

	/*private void scanModernEnumsForUpdates() {
		for (final Attribute bukkit : Attribute.values())
			try {
				CompAttribute.valueOf(bukkit.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompAttribute for Bukkit's " + bukkit.name());
			}

		for (final DyeColor bukkit : DyeColor.values())
			try {
				CompColor.fromDye(bukkit);

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompColor for Bukkit's " + bukkit.name());
			}

		for (final Enchantment bukkit : Enchantment.values())
			try {
				if (CompEnchantment.getByName(bukkit.getKey().toString()) == null)
					throw new IllegalArgumentException();

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompEnchantment for Bukkit's " + bukkit);
			}

		for (final ItemFlag bukkit : ItemFlag.values())
			try {
				CompItemFlag.valueOf(bukkit.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompItemFlag for Bukkit's " + bukkit);
			}

		for (final Material bukkit : Material.values())
			try {
				CompMaterial.valueOf(bukkit.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompMaterial for Bukkit's " + bukkit);
			}

		for (final Particle bukkit : Particle.values())
			try {
				CompParticle.fromName(bukkit.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompParticle for Bukkit's " + bukkit);
			}

		for (final PotionEffectType bukkit : PotionEffectType.values())
			try {
				CompPotionEffectType.getByName(bukkit.getKey().toString());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompPotionEffectType for Bukkit's " + bukkit);
			}

		for (final Sound bukkit : Sound.values())
			try {
				CompSound.valueOf(bukkit.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompSound for Bukkit's " + bukkit);
			}

		for (final Villager.Profession bukkit : Villager.Profession.values())
			try {
				CompVillagerProfession.valueOf(bukkit.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompVillagerProfession for Bukkit's " + bukkit);
			}

		for (final Villager.Type bukkit : Villager.Type.values())
			try {
				CompVillagerType.valueOf(bukkit.name());

			} catch (final IllegalArgumentException ex) {
				Common.log("Missing CompVillagerType for Bukkit's " + bukkit);
			}
	}*/

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
	 * The exception enabling us to check if for some reason {@link BukkitPlugin}'s instance
	 * does not match this class' instance, which is most likely caused by wrong repackaging
	 * or no repackaging at all (two plugins using Foundation must both have different packages
	 * for their own Foundation version).
	 * <p>
	 * Or, this is caused by a PlugMan, and we have no mercy for that.
	 */
	private class ShadingException extends Throwable {
		private static final long serialVersionUID = 1L;

		public ShadingException() {
			if (!BukkitPlugin.this.getName().equals(BukkitPlugin.this.getDescription().getName())) {
				Bukkit.getLogger().severe("We have a class path problem in the Foundation library");
				Bukkit.getLogger().severe("preventing " + BukkitPlugin.this.getDescription().getName() + " from loading correctly!");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("This is likely caused by two plugins having the");
				Bukkit.getLogger().severe("same Foundation library paths - make sure you");
				Bukkit.getLogger().severe("relocale the package! If you are testing using");
				Bukkit.getLogger().severe("Ant, only test one plugin at the time.");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("Possible cause: " + BukkitPlugin.this.getName());
				Bukkit.getLogger().severe("Foundation package: " + BukkitPlugin.class.getPackage().getName());

				throw new FoException("Shading exception, see above for details.");
			}
		}
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {
		if (this.loadingFailed)
			return;

		BukkitPlatform.closeAudiences();

		try {
			this.onPluginStop();

		} catch (final Throwable t) {
			CommonCore.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		if (CompMetadata.isLegacy() && CompMetadata.MetadataFile.getInstance().getFile() != null)
			CompMetadata.MetadataFile.getInstance().save();

		for (final Player online : Remain.getOnlinePlayers()) {
			try {
				SimpleScoreboard.clearBoardsFor(online);

			} catch (final Throwable t) {
				CommonCore.error(t, "Error clearing scoreboard for player " + online.getName());
			}

			try {
				final Menu menu = Menu.getMenu(online);

				if (menu != null)
					online.closeInventory();

			} catch (final Throwable t) {
				CommonCore.error(t, "Error closing menu for player " + online.getName());
			}
		}

		if (SimpleSettings.REGISTER_REGIONS)
			for (final DiskRegion region : DiskRegion.getRegions())
				try {
					region.save();

				} catch (final Throwable t) {
					CommonCore.error(t, "Error saving region " + region.getFileName() + "...");
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
	 * Called before classes are scanned for \@AutoRegister and before settings are loaded.
	 * Similar to {@link JavaPlugin#onEnable()}
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

			if (CompMetadata.isLegacy() && CompMetadata.MetadataFile.getInstance().getFile() != null)
				CompMetadata.MetadataFile.getInstance().save();

			AutoRegisterScanner.reloadSettings();

			this.onPluginReload();

			if (SimpleSettings.REGISTER_REGIONS)
				DiskRegion.loadRegions();

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
		if (!this.starting)
			this.getServer().getPluginManager().disablePlugin(this);

		this.platformEnabled = false;
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly registering a single event
	 *
	 * @param listener
	 */
	public final void registerEvents(final SimpleListener<? extends Event> listener) {
		if (this.isEnabled() && this.platformEnabled)
			listener.register();
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
	 * Get the default proxy used in {@link ProxyUtil#sendBungeeMessage(Player, Object...)}
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
			this.libraryManager = new BukkitLibraryManager(this);

		return this.libraryManager;
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	@Override
	public boolean isInitializing() {
		return this.initializing;
	}

	@Override
	public final boolean isPluginEnabled() {
		return this.platformEnabled && this.isEnabled();
	}

	/**
	 * Should we parse PlaceholderAPI variables in the given message using their
	 * native method? Performance decreases.
	 *
	 * @return
	 */
	protected boolean useFullPlaceholderAPIParser() {
		return false;
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
