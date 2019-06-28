package org.mineacademy.fo.remain;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.UidtoNameConverter;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.internal.BossBarInternals;
import org.mineacademy.fo.remain.internal.ChatInternals;
import org.mineacademy.fo.remain.internal.NBTInternals;
import org.mineacademy.fo.remain.internal.ParticleInternals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.NonNull;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public final class Remain {

	// ----------------------------------------------------------------------------------------------------
	// Flags below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * The get players method stored here for performance
	 */
	private static Method getPlayersMethod;

	/**
	 * The get player health method stored here for performance
	 */
	private static Method getHealthMethod;

	/**
	 * Does the current server version get player list as a collection?
	 */
	private static boolean isGetPlayersCollection = false;

	/**
	 * Does the current server version get player health as a double?
	 */
	private static boolean isGetHealthDouble = false;

	/**
	 * Does the current server version support title API that sends fadeIn, stay and fadeOut arguments?
	 */
	private static boolean hasExtendedPlayerTitleAPI = false;

	/**
	 * Does the current server version support particle API?
	 */
	private static boolean hasParticleAPI = true;

	/**
	 * Does the current server version support native scoreboard API?
	 */
	private static boolean newScoreboardAPI = true;

	/**
	 * Does the current server version support book event?
	 */
	private static boolean hasBookEvent = true;

	/**
	 * Does the current server version support getting inventorsy location?
	 */
	private static boolean hasInventoryLocation = true;

	/**
	 * Does the current server version support permanent scoreboard tags?M
	 */
	private static boolean hasScoreboardTags = true;

	/**
	 * Does the current server version support spawn egg meta?
	 */
	private static boolean hasSpawnEggMeta = true;

	/**
	 * Does the current server version support advancements?
	 */
	private static boolean hasAdvancements = true;

	/**
	 * Does the current server has the "net.md_5.bungee" library present?
	 */
	private static boolean bungeeApiPresent = true;

	/**
	 * Stores player cooldowns for old MC versions
	 */
	private final static StrictMap<UUID /*Player*/, StrictMap<Material, Integer>> cooldowns = new StrictMap<>();

	// Singleton
	private Remain() {
	}

	/**
	 * Initialize all fields and methods automatically when we set the plugin
	 */
	static {

		// Check compatibility
		try {
			ChatInternals.callStatic();
			NBTInternals.checkCompatible();
			ParticleInternals.ANGRY_VILLAGER.getClass();

			for (final Material bukkitMaterial : Material.values())
				CompMaterial.fromString(bukkitMaterial.toString());

			for (final CompMaterial compMaterial : CompMaterial.values())
				compMaterial.getMaterial();

			ReflectionUtil.getNMSClass("Entity");
		} catch (final Throwable t) {
			System.out.println("** COMPATIBILITY TEST FAILED - THIS PLUGIN WILL NOT FUNCTION **");
			System.out.println("** YOUR MINECRAFT VERSION APPEARS UNSUPPORTED: " + MinecraftVersion.getCurrent() + " **");

			t.printStackTrace();

			System.out.println("***************************************************************");
			throw new FoException("(This is a proxy exception, look for errors above)");
		}

		try {

			// Test for very old CraftBukkit (older than 1.3.2)
			try {
				Class.forName("org.bukkit.Sound");
			} catch (final ClassNotFoundException ex) {
				throw new UnsupportedOperationException("Minecraft 1.2.5 is not supported.");
			}

			getPlayersMethod = Bukkit.class.getMethod("getOnlinePlayers");
			isGetPlayersCollection = getPlayersMethod.getReturnType() == Collection.class;

			getHealthMethod = LivingEntity.class.getMethod("getHealth");
			isGetHealthDouble = getHealthMethod.getReturnType() == double.class;

			hasExtendedPlayerTitleAPI = MinecraftVersion.atLeast(V.v1_11);

			try {
				World.class.getMethod("spawnParticle", org.bukkit.Particle.class, Location.class, int.class);
			} catch (final NoClassDefFoundError | ReflectiveOperationException ex) {
				hasParticleAPI = false;
			}

			try {
				Class.forName("net.md_5.bungee.chat.ComponentSerializer");
			} catch (final ClassNotFoundException ex) {
				bungeeApiPresent = false;

				throw new FoException(
						"&cYour server version (&f" + Bukkit.getBukkitVersion().replace("-SNAPSHOT", "") + "&c) doesn't\n" +
								" &cinclude &elibraries required&c for this plugin to\n" +
								" &crun. Install the following plugin for compatibility:\n" +
								" &fhttps://www.spigotmc.org/resources/38379");
			}

			try {
				Objective.class.getMethod("getScore", String.class);
			} catch (NoClassDefFoundError | NoSuchMethodException e) {
				newScoreboardAPI = false;
			}

			try {
				Class.forName("org.bukkit.event.player.PlayerEditBookEvent").getName();
			} catch (final ClassNotFoundException ex) {
				hasBookEvent = false;
			}

			try {
				Inventory.class.getMethod("getLocation");
			} catch (final ReflectiveOperationException ex) {
				hasInventoryLocation = false;
			}

			try {
				Entity.class.getMethod("getScoreboardTags");
			} catch (final ReflectiveOperationException ex) {
				hasScoreboardTags = false;
			}

			try {
				Class.forName("org.bukkit.inventory.meta.SpawnEggMeta");
			} catch (final ClassNotFoundException err) {
				hasSpawnEggMeta = false;
			}

			try {
				Class.forName("org.bukkit.advancement.Advancement");
				Class.forName("org.bukkit.NamespacedKey");

			} catch (final ClassNotFoundException err) {
				hasAdvancements = false;
			}

		} catch (final ReflectiveOperationException ex) {
			throw new UnsupportedOperationException("Failed to set up reflection, " + SimplePlugin.getNamed() + " won't work properly", ex);
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Getters for various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Is 'net.md_5.bungee.api.chat' package present? Spigot 1.7.10 and never.
	 *
	 * @return if the bungee chat API is present
	 */
	public static boolean isBungeeApiPresent() {
		return bungeeApiPresent;
	}

	/**
	 * Is this server supporting native scoreboard api?
	 *
	 * @return if server supports native scoreboard api
	 */
	public static boolean hasNewScoreboardAPI() {
		return newScoreboardAPI;
	}

	/**
	 * Is this server supporting particles?
	 *
	 * @return if server supports native particle api
	 */
	public static boolean hasParticleAPI() {
		return hasParticleAPI;
	}

	/**
	 * Is this server supporting book event?
	 *
	 * @return if server supports book event
	 */
	public static boolean hasBookEvent() {
		return hasBookEvent;
	}

	/**
	 * Is this server supporting permanent scoreboard tags?
	 *
	 * @return if server supports permanent scoreboard tags
	 */
	public static boolean hasScoreboardTags() {
		return hasScoreboardTags;
	}

	/**
	 * Return if the server version supports {@link SpawnEggMeta}
	 *
	 * @return true if egg meta are supported
	 */
	public static boolean hasSpawnEggMeta() {
		return hasSpawnEggMeta;
	}

	// ----------------------------------------------------------------------------------------------------
	// Compatibility methods below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the health of an entity
	 *
	 * @param entity the entity
	 * @return the health
	 */
	public static int getHealth(LivingEntity entity) {
		return isGetHealthDouble ? (int) entity.getHealth() : getHealhLegacy(entity);
	}

	/**
	 * Returns all online players
	 *
	 * @return the online players
	 */
	public static Collection<? extends Player> getOnlinePlayers() {
		return isGetPlayersCollection ? Bukkit.getOnlinePlayers() : Arrays.asList(getPlayersLegacy());
	}

	/**
	 * Spawns a falling block
	 *
	 * @param loc
	 * @param material
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(Location loc, Material material) {
		return spawnFallingBlock(loc, material, (byte) 0);
	}

	/**
	 * Spawns a falling block.
	 *
	 * @param loc
	 * @param material
	 * @param data
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(Location loc, Material material, byte data) {
		if (MinecraftVersion.atLeast(V.v1_13))
			return loc.getWorld().spawnFallingBlock(loc, Bukkit.getUnsafe().fromLegacy(material, data));
		else {
			try {
				return (FallingBlock) loc.getWorld().getClass().getMethod("spawnFallingBlock", Location.class, int.class, byte.class).invoke(loc.getWorld(), loc, material.getId(), data);
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();

				return null;
			}
		}
	}

	/**
	 * Sets a data of a block in the world.
	 *
	 * @param block
	 * @param data
	 */
	public static void setData(Block block, int data) {
		try {
			Block.class.getMethod("setData", byte.class).invoke(block, (byte) data);
		} catch (final NoSuchMethodException ex) {
			block.setBlockData(Bukkit.getUnsafe().fromLegacy(block.getType(), (byte) data), true);

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Sets a block type and its data, applying physics.
	 *
	 * @param block
	 * @param material
	 * @param data
	 */
	public static void setTypeAndData(Block block, CompMaterial material, byte data) {
		setTypeAndData(block, material.getMaterial(), data);
	}

	/**
	 * Sets a block type and its data, applying physics.
	 *
	 * @param block
	 * @param material
	 * @param data
	 */
	public static void setTypeAndData(Block block, Material material, byte data) {
		setTypeAndData(block, material, data, true);
	}

	/**
	 * Sets a block type and its data.
	 *
	 * @param block
	 * @param material
	 * @param data
	 */
	public static void setTypeAndData(Block block, Material material, byte data, boolean physics) {
		if (MinecraftVersion.atLeast(V.v1_13)) {
			block.setType(material);
			block.setBlockData(Bukkit.getUnsafe().fromLegacy(material, data), physics);

		} else {
			try {
				block.getClass().getMethod("setTypeIdAndData", int.class, byte.class, boolean.class).invoke(block, material.getId(), data, physics);
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Sends a fake block update to a certain location, and than reverts it back to
	 * the real block after a while.
	 *
	 * @param delayTicks the pause between reverting back
	 * @param player     the player
	 * @param loc        the location
	 * @param material   the material
	 */
	public static void animateBlockChange(final int delayTicks, final Player player, final Location loc, final CompMaterial material) {
		// Force to run sync
		Common.runLater(() -> {
			try {
				player.sendBlockChange(loc, material.getMaterial().createBlockData());
			} catch (final NoSuchMethodError ex) {
				player.sendBlockChange(loc, material.getMaterial(), (byte) material.getData());
			}
		});

		// Rest
		Common.runLater(delayTicks, () -> {
			try {
				player.sendBlockChange(loc, loc.getBlock().getBlockData());
			} catch (final NoSuchMethodError ex) {
				player.sendBlockChange(loc, material.getMaterial(), (byte) material.getData());
			}
		});
	}

	/**
	 * Converts json string into legacy colored text
	 *
	 * @param json
	 * @return
	 * @throws InteractiveTextFoundException
	 */
	public static String toLegacyText(String json) throws InteractiveTextFoundException {
		return toLegacyText(json, true);
	}

	/**
	 * Converts chat message in JSON (IChatBaseComponent) to one lined old style
	 * message with color codes. e.g. {text:"Hello world",color="red"} converts to
	 * &cHello world
	 *
	 * @throws InteractiveTextFoundException if click/hover event are found. Such
	 *                                       events would be removed, and therefore
	 *                                       message containing them shall not be
	 *                                       unpacked
	 *
	 * @param denyEvents if an exception should be thrown if hover/click event is
	 *                   found.
	 */
	public static String toLegacyText(String json, boolean denyEvents) throws InteractiveTextFoundException {
		Valid.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");
		String text = "";

		try {
			for (final BaseComponent comp : ComponentSerializer.parse(json)) {
				if ((comp.getHoverEvent() != null || comp.getClickEvent() != null) && denyEvents)
					throw new InteractiveTextFoundException();

				text += comp.toLegacyText();
			}
		} catch (final Throwable t) {
			Common.log("Unable to parse JSON message. Got " + t.getMessage());
		}

		return text;
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 */
	public static String toJson(String message) {
		Valid.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");

		return toJson(TextComponent.fromLegacyText(message));
	}

	/**
	 * Converts base components into json
	 *
	 * @param comps
	 * @return
	 */
	public static String toJson(BaseComponent... comps) {
		Valid.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");

		String json;

		try {
			json = ComponentSerializer.toString(comps);

		} catch (final Throwable t) {
			json = new Gson().toJson(new TextComponent(comps).toLegacyText());
		}

		return json;
	}

	/**
	 * Converts json into base component array
	 *
	 * @param json
	 * @return
	 */
	public static BaseComponent[] toComponent(String json) {
		Valid.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");

		return ComponentSerializer.parse(json);
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(CommandSender sender, String json) {
		try {
			sendComponent(sender, ComponentSerializer.parse(json));

		} catch (final RuntimeException ex) {
			Common.error(ex, "Malformed JSON when sending message to " + sender.getName() + " with JSON: " + json);
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param comps
	 */
	public static void sendComponent(CommandSender sender, Object comps) {
		BungeeChatProvider.sendComponent(sender, comps);
	}

	/**
	 * Sends a title to the player (1.8+) Texts will be colorized.
	 *
	 * @param player   the player
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public static void sendTitle(final Player player, int fadeIn, int stay, int fadeOut, String title, String subtitle) {
		if (MinecraftVersion.newerThan(V.v1_7)) {
			if (hasExtendedPlayerTitleAPI)
				player.sendTitle(Common.colorize(title), Common.colorize(subtitle), fadeIn, stay, fadeOut);
			else
				ChatInternals.sendTitleLegacy(player, fadeIn, stay, fadeOut, title, subtitle);

		} else {
			Common.tell(player, title);
			Common.tell(player, subtitle);
		}
	}

	/**
	 * Resets the title that is being displayed to the player (1.8+)
	 *
	 * @param player the player
	 */
	public static void resetTitle(Player player) {
		if (hasExtendedPlayerTitleAPI)
			player.resetTitle();
		else
			ChatInternals.resetTitleLegacy(player);
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null. (1.8+)
	 * Texts will be colorized.
	 *
	 * @param player the player
	 * @param header the header
	 * @param footer the footer
	 */
	public static void sendTablist(Player player, String header, String footer) {
		Valid.checkBoolean(MinecraftVersion.newerThan(V.v1_7), "Sending tab list requires Minecraft 1.8x or newer!");

		if (MinecraftVersion.atLeast(V.v1_13))
			player.setPlayerListHeaderFooter(Common.colorize(header), Common.colorize(footer));
		else
			ChatInternals.sendTablistLegacy(player, header, footer);
	}

	/**
	 * Displays message above player's health and hunger bar. (1.8+) Text will be
	 * colorized.
	 *
	 * @param player the player
	 * @param text   the text
	 */
	public static void sendActionBar(Player player, String text) {
		if (!MinecraftVersion.newerThan(V.v1_7)) {
			Common.tell(player, text);
			return;
		}

		try {
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Common.colorize(text)));

		} catch (final NoSuchMethodError err) {
			ChatInternals.sendActionBarLegacy(player, text);
		}
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param player
	 * @param message
	 * @param percent
	 */
	public static void sendBossbarPercent(Player player, String message, float percent) {
		sendBossbarPercent(player, message, percent, null, null);
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param player
	 * @param message
	 * @param percent
	 * @param color
	 * @param style
	 */
	public static void sendBossbarPercent(Player player, String message, float percent, CompBarColor color, CompBarStyle style) {
		BossBarInternals.setMessage(player, message, percent, color, style);
	}

	/**
	 * Send boss bar only for limited time
	 *
	 * @param player
	 * @param message
	 * @param seconds
	 */
	public static void sendBossbarTimed(Player player, String message, int seconds) {
		sendBossbarTimed(player, message, seconds, null, null);
	}

	/**
	 * Send boss bar only for limited time
	 *
	 * @param player
	 * @param message
	 * @param seconds
	 * @param color
	 * @param style
	 */
	public static void sendBossbarTimed(Player player, String message, int seconds, CompBarColor color, CompBarStyle style) {
		BossBarInternals.setMessage(player, message, seconds, color, style);
	}

	/**
	 * Creates new plugin command from given label
	 *
	 * @param label
	 * @return
	 */
	public static PluginCommand newCommand(String label) {
		try {
			final Constructor<PluginCommand> con = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
			con.setAccessible(true);

			return con.newInstance(label, SimplePlugin.getInstance());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException("Unable to create command: /" + label, ex);
		}
	}

	/**
	 * Sets a custom command name
	 *
	 * @param command
	 * @param name
	 */
	public static void setCommandName(PluginCommand command, String name) {
		try {
			command.setName(name);
		} catch (final NoSuchMethodError ex) {
		}
	}

	/**
	 * Injects an existing command into the command map
	 *
	 * @param command
	 */
	public static void registerCommand(Command command) {
		final CommandMap commandMap = getCommandMap();
		commandMap.register(command.getLabel(), command);

		Valid.checkBoolean(command.isRegistered(), "Command /" + command.getLabel() + " could not have been registered properly!");
	}

	/**
	 * Removes a command by its label from command map, includes all aliases
	 *
	 * @param label the label
	 */
	public static void unregisterCommand(String label) {
		unregisterCommand(label, true);
	}

	/**
	 * Removes a command by its label from command map, optionally can also remove
	 * aliases
	 *
	 * @param label          the label
	 * @param removeAliases, also remove aliases?
	 */
	public static void unregisterCommand(String label, boolean removeAliases) {
		try {
			// Unregister the commandMap from the command itself.
			final PluginCommand command = Bukkit.getPluginCommand(label);

			if (command != null) {
				final Field commandField = Command.class.getDeclaredField("commandMap");
				commandField.setAccessible(true);

				if (command.isRegistered())
					command.unregister((CommandMap) commandField.get(command));
			}

			// Delete command + aliases from server's command map.
			final Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
			f.setAccessible(true);

			final Map<String, Command> cmdMap = (Map<String, Command>) f.get(Remain.getCommandMap());

			cmdMap.remove(label);

			if (command != null && removeAliases)
				for (final String alias : command.getAliases())
					cmdMap.remove(alias);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException("Failed to unregister command /" + label, ex);
		}
	}

	// Return servers command map
	private static SimpleCommandMap getCommandMap() {
		try {
			return (SimpleCommandMap) ReflectionUtil.getOFCClass("CraftServer").getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException("Unable to get the command map", ex);
		}
	}

	/**
	 * Register a new enchantment in Bukkit, unregistering it first to avoid errors
	 *
	 * @param enchantment
	 */
	public static void registerEnchantment(Enchantment enchantment) {
		unregisterEnchantment(enchantment);

		ReflectionUtil.setStaticField(Enchantment.class, "acceptingNew", true);
		Enchantment.registerEnchantment(enchantment);

	}

	/**
	 * Unregister an enchantment from Bukkit. Works even for vanilla MC enchantments (found in Enchantment class)
	 *
	 * @param enchantment
	 */
	public static void unregisterEnchantment(Enchantment enchantment) {
		{ // Unregister by key
			final Map<NamespacedKey, Enchantment> byKey = ReflectionUtil.getStaticFieldContent(Enchantment.class, "byKey");

			byKey.remove(enchantment.getKey());
		}

		{ // Unregister by name
			final Map<String, Enchantment> byName = ReflectionUtil.getStaticFieldContent(Enchantment.class, "byName");

			byName.remove(enchantment.getName());
		}
	}

	/**
	 * Returns the inventory location
	 *
	 * @param inv the inventory
	 * @return the location
	 */
	public static Location getLocation(Inventory inv) {
		if (hasInventoryLocation)
			try {
				return inv.getLocation();

			} catch (final NullPointerException ex) { // EnderChest throws this
				return null;
			}

		return inv.getHolder() instanceof BlockState ? ((BlockState) inv.getHolder()).getLocation() : !inv.getViewers().isEmpty() ? inv.getViewers().iterator().next().getLocation() : null;
	}

	/**
	 * Attempts to respawn the player, either via native method or reflection
	 *
	 * @param player
	 */
	public static void respawnNow(Player player) {
		respawn(player, 0);
	}

	/**
	 * Attempts to respawn the player, either via native method or reflection
	 *
	 * @param player
	 * @param delayTicks how long to way before respawning, minimum 1 tick
	 */
	public static void respawn(Player player, int delayTicks) {
		Common.runLater(delayTicks, () -> {
			try {
				player.spigot().respawn();

			} catch (final NoSuchMethodError err) {
				ReflectionUtil.respawn(player);
			}
		});
	}

	/**
	 * Since Minecraft introduced double yelding, it fires two events for
	 * interaction for each hand. Return if the event was fired for the main hand.
	 *
	 * Backwards compatible.
	 *
	 * @param e, the event
	 * @return if the event was fired for main hand only
	 */
	public static boolean isInteractEventPrimaryHand(PlayerInteractEvent e) {
		try {
			return e.getHand() != null && e.getHand() == EquipmentSlot.HAND;

		} catch (final NoSuchMethodError err) {
			return true; // Older MC, always true since there was no off-hand
		}
	}

	/**
	 * See {@link #isInteractEventPrimaryHand(PlayerInteractEvent)}
	 *
	 * @param e
	 * @return
	 */
	public static boolean isInteractEventPrimaryHand(PlayerInteractEntityEvent e) {
		try {
			return e.getHand() != null && e.getHand() == EquipmentSlot.HAND;
		} catch (final NoSuchMethodError err) {
			return true; // Older MC, always true since there was no off-hand
		}
	}

	/**
	 * Returns a scoreboard score
	 *
	 * @param obj
	 * @param entry
	 * @return
	 */
	public static Score getScore(Objective obj, String entry) {
		entry = Common.colorize(entry);

		try {
			return obj.getScore(entry);

		} catch (final NoSuchMethodError err) {
			return obj.getScore(Bukkit.getOfflinePlayer(entry));
		}
	}

	/**
	 * Tries to find offline player by uuid
	 *
	 * This method may include a blocking call
	 *
	 * @param id
	 * @return
	 */
	public static OfflinePlayer getOfflinePlayerByUUID(UUID id) {
		try {
			return Bukkit.getOfflinePlayer(id);

		} catch (final NoSuchMethodError err) {
			final UidtoNameConverter f = new UidtoNameConverter(id);

			try {
				final String name = f.call();

				return Bukkit.getOfflinePlayer(name);
			} catch (final Throwable t) {
				return null;
			}
		}
	}

	/**
	 * Tries to find online player by uuid
	 *
	 * @param id
	 * @return
	 */
	public static Player getPlayerByUUID(UUID id) {
		try {
			return Bukkit.getPlayer(id);

		} catch (final NoSuchMethodError err) {
			for (final Player online : getOnlinePlayers())
				if (online.getUniqueId().equals(id))
					return online;

			return null;
		}
	}

	/**
	 * Gets the final damage of an event
	 *
	 * @param e
	 * @return
	 */
	public static double getFinalDamage(EntityDamageEvent e) {
		try {
			return e.getFinalDamage();

		} catch (final NoSuchMethodError err) {
			return e.getDamage();
		}
	}

	/**
	 * Return the correct inventory that was clicked (either bottom or top inventory
	 * or null if clicked outside)
	 *
	 * @param e the inventory click event
	 * @return the actual inventory clicked, either bottom or top, or null if
	 *         clicked outside
	 */
	public static Inventory getClickedInventory(InventoryClickEvent e) {
		final int slot = e.getRawSlot();
		final InventoryView view = e.getView();

		return slot < 0 ? null : view.getTopInventory() != null && slot < view.getTopInventory().getSize() ? view.getTopInventory() : view.getBottomInventory();
	}

	/**
	 * Sets a custom name to entity
	 *
	 * @param en
	 * @param name
	 */
	public static void setCustomName(Entity en, String name) {
		try {
			en.setCustomNameVisible(true);
			en.setCustomName(Common.colorize(name));
		} catch (final NoSuchMethodError er) {
		}
	}

	/**
	 * Tries to get the first material, or return the second as fall back
	 *
	 * @param material
	 * @param fallback
	 * @return
	 */
	public static CompMaterial getMaterial(String material, CompMaterial fallback) {
		Material mat = null;

		try {
			mat = Material.getMaterial(material);
		} catch (final Throwable t) {
		}

		return mat != null ? CompMaterial.fromMaterial(mat) : fallback;
	}

	/**
	 * Tries to get the new material by name, or returns the old one as a fall back
	 *
	 * @param newMaterial
	 * @param oldMaterial
	 * @return
	 */
	public static Material getMaterial(String newMaterial, String oldMaterial) {
		try {
			return Material.getMaterial(newMaterial);

		} catch (final Throwable t) {
			return Material.getMaterial(oldMaterial);
		}
	}

	/**
	 * Get the target block for player
	 *
	 * @param en
	 * @param radius
	 * @return
	 */
	public static Block getTargetBlock(LivingEntity en, int radius) {
		try {
			return (Block) en.getClass().getMethod("getTargetBlock", Set.class, int.class).invoke(en, (HashSet<Material>) null, radius);

		} catch (final ReflectiveOperationException ex) {
			try {
				return (Block) en.getClass().getMethod("getTargetBlock", HashSet.class, int.class).invoke(en, (HashSet<Byte>) null, radius);

			} catch (final ReflectiveOperationException ex2) {
				throw new FoException("Unable to get target block for " + en, ex);
			}
		}
	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot
	 * be modified that much. It imposes a slight performance penalty.
	 *
	 * @param receiver
	 * @param message
	 */
	public static void sendToast(Player receiver, String message) {
		if (hasAdvancements && message != null && !message.isEmpty()) {
			final String colorized = Common.colorize(message);

			if (!colorized.isEmpty())
				new AdvancementAccessor(
						"" + System.nanoTime(),
						colorized,
						"book")
								.show(receiver);
		}
	}

	/**
	 * Set the visual cooldown for the given material, see {@link Player#setCooldown(Material, int)}
	 * You still have to implement custom handling of it
	 *
	 * Old MC versions are supported and handled by us
	 * however there is no visual effect
	 *
	 * @param player
	 * @param material
	 * @param cooldownTicks
	 */
	public static void setCooldown(Player player, Material material, int cooldownTicks) {
		try {
			player.setCooldown(material, cooldownTicks);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			cooldown.override(material, cooldownTicks);
			cooldowns.override(player.getUniqueId(), cooldown);
		}
	}

	/**
	 * See {@link Player#hasCooldown(Material)}
	 *
	 * Old MC versions are supported and handled by us
	 * however there is no visual effect
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static boolean hasCooldown(Player player, Material material) {
		try {
			return player.hasCooldown(material);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			return cooldown.contains(material);
		}
	}

	/**
	 * Return the item cooldown as specified in {@link Player#getCooldown(Material)}
	 *
	 * Old MC versions are supported and handled by us
	 * however there is no visual effect
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static int getCooldown(Player player, Material material) {
		try {
			return player.getCooldown(material);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			return cooldown.getOrDefault(material, 0);
		}
	}

	// Internal method to get a players cooldown map
	private static StrictMap<Material, Integer> getCooldown(Player player) {
		return cooldowns.getOrDefault(player.getUniqueId(), new StrictMap<>());
	}

	/**
	 * Return the entity by UUID
	 *
	 * @param uuid
	 * @return
	 */
	public static Entity getEntity(UUID uuid) {
		catchAsync("iterating through entities [CMN]");

		for (final World world : Bukkit.getWorlds())
			for (final Entity entity : world.getEntities())
				if (entity.getUniqueId().equals(uuid))
					return entity;

		return null;
	}

	/**
	 * Return nearby entities in a location
	 *
	 * @param location
	 * @param radius
	 * @return
	 */
	public static Collection<Entity> getNearbyEntities(Location location, double radius) {
		try {
			return location.getWorld().getNearbyEntities(location, radius, radius, radius);

		} catch (final Throwable t) {
			final List<Entity> found = new ArrayList<>();

			for (final Entity e : location.getWorld().getEntities())
				if (e.getLocation().distance(location) <= radius)
					found.add(e);

			return found;
		}
	}

	/**
	 * Takes one piece of the hand item
	 *
	 * @param player
	 */
	public static void takeHandItem(Player player) {
		takeItemAndSetAsHand(player, player.getItemInHand());
	}

	/**
	 * Takes one piece of the given item and sets it as hand
	 *
	 * @param player
	 * @param item
	 */
	public static void takeItemAndSetAsHand(Player player, ItemStack item) {
		if (item.getAmount() > 1) {
			item.setAmount(item.getAmount() - 1);
			player.getInventory().setItemInHand(item);

		} else
			player.getInventory().setItemInHand(null);

		player.updateInventory();
	}

	/**
	 * Takes 1 piece of the item from players inventory
	 *
	 * @param player
	 * @param item
	 */
	public static void takeItemOnePiece(Player player, ItemStack item) {
		Common.runLater(() -> {
			if (item.getAmount() > 1) {
				item.setAmount(item.getAmount() - 1);

			} else {
				if (MinecraftVersion.atLeast(V.v1_9))
					item.setAmount(0);

				// Explanation: For some weird reason there is a bug not removing 1 piece of ItemStack in 1.8.8
				else {
					final ItemStack[] content = player.getInventory().getContents();

					for (int i = 0; i < content.length; i++) {
						final ItemStack c = content[i];

						if (c != null && c.equals(item)) {
							content[i] = null;

							break;
						}
					}

					player.getInventory().setContents(content);
				}
			}

			player.updateInventory();
		});
	}

	/**
	 * Attempts to insert a certain potion to the given item
	 *
	 * @param item
	 * @param type
	 * @param level
	 */
	public static void setPotion(ItemStack item, PotionEffectType type, int level) {
		final PotionType wrapped = PotionType.getByEffect(type);
		final PotionMeta meta = (PotionMeta) item.getItemMeta();

		try {
			final org.bukkit.potion.PotionData data = new org.bukkit.potion.PotionData(level > 0 && wrapped != null ? wrapped : PotionType.WATER);

			if (level > 0 && wrapped == null)
				meta.addEnchant(Enchantment.DURABILITY, 1, true);

			meta.setBasePotionData(data);

		} catch (final NoSuchMethodError | NoClassDefFoundError ex) {
			meta.setMainEffect(type);
			meta.addCustomEffect(new PotionEffect(type, Integer.MAX_VALUE, level - 1), true);
		}

		item.setItemMeta(meta);
	}

	/**
	 * Attempts to return the I18N localized display name, or returns the
	 * capitalized Material name if fails.
	 *
	 * Requires PaperSpigot.
	 *
	 * @param item the {@link ItemStack} to get I18N name from
	 * @return the I18N localized name or Material name
	 */
	public static String getI18NDisplayName(ItemStack item) {
		try {
			return (String) item.getClass().getDeclaredMethod("getI18NDisplayName").invoke(item);

		} catch (final Throwable t) {
			return ItemUtil.bountifyCapitalized(item.getType());
		}
	}

	/**
	 * Load YAML configuration from stream, throwing any errors
	 *
	 * @param is the input stream
	 * @return the configuration
	 */
	public static YamlConfiguration loadConfiguration(InputStream is) {
		YamlConfiguration conf = null;

		try {
			conf = loadConfigurationStrict(is);

		} catch (final Throwable ex) {
			ex.printStackTrace();
		}

		Valid.checkNotNull(conf, "Could not load configuration from " + is);
		return conf;
	}

	/**
	 * Load YAML configuration from stream as unicode, throwing any errors
	 *
	 * @param is the input stream
	 * @return the configuration
	 * @throws Throwable when any error occurs
	 */
	public static YamlConfiguration loadConfigurationStrict(InputStream is) throws Throwable {
		final YamlConfiguration conf = new YamlConfiguration();

		try {
			conf.load(new InputStreamReader(is, StandardCharsets.UTF_8));

		} catch (final NoSuchMethodError ex) {
			loadFromString(is, conf);
		}

		return conf;
	}

	// Load the YAML configuration from stream
	private static void loadFromString(InputStream stream, YamlConfiguration conf) throws IOException, InvalidConfigurationException {
		Valid.checkNotNull(stream, "Stream cannot be null");

		final StringBuilder builder = new StringBuilder();
		final InputStreamReader reader = new InputStreamReader(stream);

		try (final BufferedReader input = new BufferedReader(reader)) {
			String line;

			while ((line = input.readLine()) != null) {
				builder.append(line);
				builder.append('\n');
			}
		}

		conf.loadFromString(builder.toString());
	}

	/**
	 * Return the max health configure from spigot
	 *
	 * @return max health, or 2048 if not found
	 */
	public static double getMaxHealth() {
		try {
			final String health = String.valueOf(Class.forName("org.spigotmc.SpigotConfig").getField("maxHealth").get(null));

			return health.contains(".") ? Double.parseDouble(health) : Integer.parseInt(health);

		} catch (final Throwable t) {
			return 2048.0;
		}
	}

	/**
	 * Returns if statistics do not save
	 *
	 * @return true if stat saving was disabled, false if not or if not running
	 *         Spigot
	 */
	public static boolean isStatSavingDisabled() {
		try {
			return (boolean) Class.forName("org.spigotmc.SpigotConfig").getField("disableStatSaving").get(null);

		} catch (final ReflectiveOperationException ex) {
			try {
				final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File("spigot.yml"));

				return cfg.isSet("stats.disable-saving") ? cfg.getBoolean("stats.disable-saving") : false;
			} catch (final Throwable t) {
				// No Spigot
			}
		}

		return false;
	}

	/**
	 * Calls "catch async" method in Spigot that checks if we are calling from the
	 * main thread
	 *
	 * @param message
	 */
	public static void catchAsync(String message) {
		try {
			Class.forName("org.spigotmc.AsyncCatcher").getMethod("catchOp", String.class).invoke(null, message);
		} catch (final Throwable t) {
			// No spigot
		}
	}

	/**
	 * Converts an unchecked exception into checked
	 *
	 * @param throwable
	 */
	public static void sneaky(Throwable throwable) {
		try {
			SneakyThrow.sneaky(throwable);

		} catch (NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
	}

	// ------------------------ Legacy ------------------------

	// return the legacy online player array
	private static Player[] getPlayersLegacy() {
		try {
			return (Player[]) getPlayersMethod.invoke(null);
		} catch (final ReflectiveOperationException ex) {
			throw new FoException("Reflection malfunction", ex);
		}
	}

	// return the legacy get health int method
	private static int getHealhLegacy(LivingEntity pl) {
		try {
			return (int) getHealthMethod.invoke(pl);
		} catch (final ReflectiveOperationException ex) {
			throw new FoException("Reflection malfunction", ex);
		}
	}

	// ------------------------ Utility ------------------------

	/**
	 * Thrown when message contains hover or click events which would otherwise got
	 * removed.
	 *
	 * Such message is not checked.
	 */
	public static class InteractiveTextFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InteractiveTextFoundException() {
		}
	}
}

/**
 * A wrapper for Spigot
 */
class SneakyThrow {

	public static void sneaky(Throwable t) {
		throw SneakyThrow.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(Throwable t) throws T {
		throw (T) t;
	}
}

/**
 * A wrapper for bungee chat component library
 */
class BungeeChatProvider {

	static void sendComponent(CommandSender sender, Object comps) {

		if (comps instanceof TextComponent)
			sendComponent0(sender, (TextComponent) comps);

		else
			sendComponent0(sender, (BaseComponent[]) comps);
	}

	private static void sendComponent0(CommandSender sender, BaseComponent... comps) {
		String plainMessage = "";

		for (final BaseComponent comp : comps)
			plainMessage += comp.toLegacyText();

		if (!(sender instanceof Player)) {
			tell0(sender, plainMessage);

			return;
		}

		try {
			((Player) sender).spigot().sendMessage(comps);

		} catch (NoClassDefFoundError | NoSuchMethodError ex) {
			if (MinecraftVersion.newerThan(V.v1_7))
				Common.error(ex, "Error printing JSON message, sending as plain.");

			tell0(sender, plainMessage);

		} catch (final Exception ex) {
			tell0(sender, plainMessage);
		}
	}

	private static void tell0(CommandSender sender, String msg) {
		Valid.checkNotNull(sender, "Sender cannot be null");

		if (msg.isEmpty() || "none".equals(msg))
			return;

		if (msg.startsWith("[JSON]")) {
			final String stripped = msg.replaceFirst("\\[JSON\\]", "").trim();

			if (!stripped.isEmpty())
				Remain.sendJson(sender, stripped);

		} else
			for (final String part : msg.split("\n"))
				sender.sendMessage(part);
	}
}

/**
 * A wrapper for advancements
 */
class AdvancementAccessor {

	private final NamespacedKey key;
	private final String icon;
	private final String title;

	AdvancementAccessor(@NonNull final String namespacedKey, final String title, final String icon) {
		this(new NamespacedKey(SimplePlugin.getInstance(), namespacedKey), title, icon);
	}

	AdvancementAccessor(final NamespacedKey namespacedKey, final String title, final String icon) {
		key = namespacedKey;
		this.title = title;
		this.icon = icon;
	}

	public void show(final Player player) {
		add();
		grant(player);

		Common.runLater(10, () -> {
			revoke(player);
			remove();
		});
	}

	private void grant(final Player pl) {
		final Advancement adv = getAdvancement();
		final AdvancementProgress progress = pl.getAdvancementProgress(adv);

		if (!progress.isDone())
			progress.getRemainingCriteria().forEach((crit) -> progress.awardCriteria(crit));
	}

	private void revoke(final Player pl) {
		final Advancement adv = getAdvancement();
		final AdvancementProgress prog = pl.getAdvancementProgress(adv);

		if (prog.isDone())
			prog.getAwardedCriteria().forEach((crit) -> prog.revokeCriteria(crit));
	}

	private void add() {
		Bukkit.getUnsafe().loadAdvancement(key, getJson());
	}

	private void remove() {
		Bukkit.getUnsafe().removeAdvancement(key);
	}

	private Advancement getAdvancement() {
		return Bukkit.getAdvancement(key);
	}

	public String getJson() {
		final JsonObject json = new JsonObject();

		final JsonObject icon = new JsonObject();
		icon.addProperty("item", this.icon);

		final JsonObject display = new JsonObject();
		display.add("icon", icon);
		display.addProperty("title", title);
		display.addProperty("description", "");
		display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/adventure.png");
		display.addProperty("frame", "goal");
		display.addProperty("announce_to_chat", false);
		display.addProperty("show_toast", true);
		display.addProperty("hidden", true);

		final JsonObject criteria = new JsonObject();

		final JsonObject trigger = new JsonObject();
		trigger.addProperty("trigger", "minecraft:impossible");

		criteria.add("impossible", trigger);

		json.add("criteria", criteria);
		json.add("display", display);

		return new Gson().toJson(json);
	}
}