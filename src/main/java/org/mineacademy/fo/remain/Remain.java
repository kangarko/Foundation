package org.mineacademy.fo.remain;

import static org.mineacademy.fo.ReflectionUtil.getNMSClass;
import static org.mineacademy.fo.ReflectionUtil.getOBCClass;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.Statistic.Type;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.ReflectionException;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.UUIDToNameConverter;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.internal.BossBarInternals;
import org.mineacademy.fo.remain.internal.ChatInternals;
import org.mineacademy.fo.remain.nbt.NBTEntity;
import org.mineacademy.fo.settings.SimpleYaml;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

/**
 * Our main cross-version compatibility class.
 * <p>
 * Look up for many methods enabling you to make your plugin
 * compatible with MC 1.8.8 up to the latest version.
 */
public final class Remain {

	/**
	 * Pattern used to match encoded HEX colors &x&F&F&F&F&F&F
	 */
	private static final Pattern RGB_HEX_ENCODED_REGEX = Pattern.compile("(?i)(§x)((§[0-9A-F]){6})");

	/**
	 * The Google Json instance
	 */
	private final static Gson gson = new Gson();

	// ----------------------------------------------------------------------------------------------------
	// Methods below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * The get players method stored here for performance
	 */
	private static final Method getPlayersMethod;

	/**
	 * The get player health method stored here for performance
	 */
	private static final Method getHealthMethod;

	/**
	 * The CraftPlayer.getHandle method
	 */
	private static Method getHandle;

	/**
	 * The EntityPlayer.playerConnection method
	 */
	private static Field fieldPlayerConnection;

	/**
	 * Get if entity is invulnerable on legacy MC
	 */
	private static Field fieldEntityInvulnerable;

	/**
	 * The PlayerConnection.sendPacket method
	 */
	private static Method sendPacket;

	// ----------------------------------------------------------------------------------------------------
	// Flags below
	// ----------------------------------------------------------------------------------------------------

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
	 * Can you call {@link YamlConfiguration#load(java.io.Reader)}
	 */
	private static boolean hasYamlReaderLoad = true;

	/**
	 * Does the current server has the "net.md_5.bungee" library present?
	 */
	private static boolean bungeeApiPresent = true;

	/**
	 * Is org/bukkit/inventory/meta/ItemMeta class present? MC 1.4.7+
	 */
	private static boolean hasItemMeta = true;

	/**
	 * Return if the {@link Entity#addPassenger(Entity)} method is available.
	 */
	private static boolean hasAddPassenger = true;

	/**
	 * Stores player cooldowns for old MC versions
	 */
	private final static StrictMap<UUID /*Player*/, StrictMap<Material, Integer>> cooldowns = new StrictMap<>();

	/**
	 * The internal private section path data class
	 */
	private static Class<?> sectionPathDataClass = null;

	/**
	 * The server-name from server.properties (is lacking on new Minecraft version so we have to readd it back)
	 */
	private static String serverName;

	// Singleton
	private Remain() {
	}

	/**
	 * Initialize all fields and methods automatically when we set the plugin
	 */
	static {
		Valid.checkBoolean(MinecraftVersion.getCurrent().isTested(), "Your Minecraft version " + MinecraftVersion.getCurrent() + " is unsupported by " + SimplePlugin.getNamed());

		// Check compatibility
		try {
			ChatInternals.callStatic();

			CompParticle.CRIT.getClass();

			for (final Material bukkitMaterial : Material.values())
				CompMaterial.fromString(bukkitMaterial.toString());

			for (final CompMaterial compMaterial : CompMaterial.values())
				compMaterial.getMaterial();

			getNMSClass("Entity", "net.minecraft.world.entity.Entity");

		} catch (final Throwable t) {
			boolean isThermos = false;

			try {
				Class.forName("thermos.ThermosRemapper");

				isThermos = true;
			} catch (final Throwable tt) {
			}

			if (!isThermos) {
				Bukkit.getLogger().severe("** COMPATIBILITY TEST FAILED - THIS PLUGIN WILL NOT FUNCTION PROPERLY **");
				Bukkit.getLogger().severe("** YOUR MINECRAFT VERSION APPEARS UNSUPPORTED: " + MinecraftVersion.getCurrent() + " **");

				t.printStackTrace();

				Bukkit.getLogger().severe("***************************************************************");
			}
		}

		try {

			final boolean hasNMS = MinecraftVersion.atLeast(V.v1_4);

			// Load optional parts
			try {

				getHandle = getOBCClass("entity.CraftPlayer").getMethod("getHandle");

				fieldPlayerConnection = getNMSClass("EntityPlayer", "net.minecraft.server.level.EntityPlayer")
						.getField(MinecraftVersion.atLeast(V.v1_17) ? "b" : hasNMS ? "playerConnection" : "netServerHandler");

				sendPacket = getNMSClass(hasNMS ? "PlayerConnection" : "NetServerHandler", "net.minecraft.server.network.PlayerConnection")
						.getMethod(MinecraftVersion.atLeast(V.v1_18) ? "a" : "sendPacket", getNMSClass("Packet", "net.minecraft.network.protocol.Packet"));

				if (MinecraftVersion.olderThan(V.v1_12)) {
					fieldEntityInvulnerable = ReflectionUtil.getNMSClass("Entity").getDeclaredField("invulnerable");
					fieldEntityInvulnerable.setAccessible(true);
				} else
					fieldEntityInvulnerable = null;

			} catch (final Throwable t) {

				t.printStackTrace();

				if (MinecraftVersion.atLeast(V.v1_7)) {
					Bukkit.getLogger().warning("Unable to find setup some parts of reflection. Plugin will still function.");
					Bukkit.getLogger().warning("Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
					Bukkit.getLogger().warning("Ignore this if using Cauldron. Otherwise check if your server is compatibble.");
				}

				fieldPlayerConnection = null;
				sendPacket = null;
				getHandle = null;
			}

			// Load mandatory parts
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
								" &fhttps://mineacademy.org/plugins/#misc");
			}

			try {
				Objective.class.getMethod("getScore", String.class);
			} catch (final NoClassDefFoundError | NoSuchMethodException e) {
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

			try {
				YamlConfiguration.class.getMethod("load", java.io.Reader.class);

			} catch (final NoSuchMethodException err) {
				hasYamlReaderLoad = false;
			}

			try {
				org.bukkit.inventory.ItemStack.class.getMethod("getItemMeta");

			} catch (final Exception ex) {
				hasItemMeta = false;
			}

			try {
				Entity.class.getMethod("addPassenger", Entity.class);

			} catch (final Exception ex) {
				hasAddPassenger = false;
			}

			try {
				sectionPathDataClass = ReflectionUtil.lookupClass("org.bukkit.configuration.SectionPathData");

			} catch (final ReflectionException ex) {
				// unsupported
			}

		} catch (final ReflectiveOperationException ex) {
			throw new UnsupportedOperationException("Failed to set up reflection, " + SimplePlugin.getNamed() + " won't work properly", ex);
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Returns Minecraft World class
	 *
	 * @param world
	 * @return
	 */
	public static Object getHandleWorld(final World world) {
		Object nms = null;
		final Method handle = ReflectionUtil.getMethod(world.getClass(), "getHandle");
		try {
			nms = handle.invoke(world);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
		return nms;
	}

	/**
	 * Returns Minecraft Entity class
	 *
	 * @param entity
	 * @return
	 */
	public static Object getHandleEntity(final Entity entity) {
		Object nms_entity = null;
		final Method handle = ReflectionUtil.getMethod(entity.getClass(), "getHandle");

		try {
			nms_entity = handle.invoke(entity);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return nms_entity;
	}

	/**
	 * Returns true if we are running a 1.8 protocol hack
	 *
	 * @return
	 */
	public static boolean isProtocol18Hack() {
		if (MinecraftVersion.newerThan(V.v1_9))
			return false;

		try {
			getNMSClass("PacketPlayOutEntityTeleport", "N/A").getConstructor(int.class, int.class, int.class, int.class, byte.class, byte.class, boolean.class, boolean.class);

		} catch (final Throwable t) {
			return false;
		}

		return true;
	}

	/**
	 * Advanced: Sends a packet to the player
	 *
	 * @param player the player
	 * @param packet the packet
	 */
	public static void sendPacket(final Player player, final Object packet) {
		if (getHandle == null || fieldPlayerConnection == null || sendPacket == null) {
			Common.log("Cannot send packet " + packet.getClass().getSimpleName() + " on your server sofware (known to be broken on Cauldron).");

			return;
		}

		try {
			final Object handle = getHandle.invoke(player);
			final Object playerConnection = fieldPlayerConnection.get(handle);

			sendPacket.invoke(playerConnection, packet);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Error sending packet " + packet.getClass() + " to player " + player.getName());
		}
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
	public static int getHealth(final LivingEntity entity) {
		return isGetHealthDouble ? (int) entity.getHealth() : getHealhLegacy(entity);
	}

	/**
	 * Return the max health of an entity
	 *
	 * @param entity
	 * @return
	 */
	public static int getMaxHealth(final LivingEntity entity) {
		return isGetHealthDouble ? (int) entity.getMaxHealth() : getMaxHealhLegacy(entity);
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
	 * Spawn a falling block at the given block location
	 *
	 * @param block
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Block block) {
		return spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5) /* fix alignment */, block.getType(), block.getData());
	}

	/**
	 * Spawns a falling block at that location
	 *
	 * @param loc
	 * @param block
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Location loc, final Block block) {
		if (MinecraftVersion.atLeast(V.v1_13))
			return loc.getWorld().spawnFallingBlock(loc, block.getBlockData());
		else
			try {
				return (FallingBlock) loc.getWorld().getClass().getMethod("spawnFallingBlock", Location.class, int.class, byte.class).invoke(loc.getWorld(), loc, ReflectionUtil.invoke("getTypeId", block), block.getData());
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();

				return null;
			}
	}

	/**
	 * Spawns a falling block
	 *
	 * @param loc
	 * @param material
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Location loc, final Material material) {
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
	public static FallingBlock spawnFallingBlock(final Location loc, final Material material, final byte data) {
		if (MinecraftVersion.atLeast(V.v1_13))
			return loc.getWorld().spawnFallingBlock(loc, material, data);
		else
			try {
				return (FallingBlock) loc.getWorld().getClass().getMethod("spawnFallingBlock", Location.class, int.class, byte.class).invoke(loc.getWorld(), loc, material.getId(), data);
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();

				return null;
			}
	}

	/**
	 * Attempts to drop the item allowing space for applying properties to the item
	 * before it is spawned
	 *
	 * @param location
	 * @param item
	 * @param modifier
	 * @return the item
	 * @deprecated use {@link EntityUtil#dropItem(Location, ItemStack, Consumer)}
	 */
	@Deprecated
	public static Item spawnItem(final Location location, final ItemStack item, final Consumer<Item> modifier) {
		try {

			final Class<?> nmsWorldClass = getNMSClass("World", "net.minecraft.world.level.World");
			final Class<?> nmsStackClass = getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Class<?> nmsEntityClass = getNMSClass("Entity", "net.minecraft.world.entity.Entity");
			final Class<?> nmsItemClass = getNMSClass("EntityItem", "net.minecraft.world.entity.item.EntityItem");

			final Constructor<?> entityConstructor = nmsItemClass.getConstructor(nmsWorldClass, double.class, double.class, double.class, nmsStackClass);

			final Object nmsWorld = location.getWorld().getClass().getMethod("getHandle").invoke(location.getWorld());
			final Method asNmsCopy = getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			final Object nmsEntity = entityConstructor.newInstance(nmsWorld, location.getX(), location.getY(), location.getZ(), asNmsCopy.invoke(null, item));

			final Class<?> craftItemClass = getOBCClass("entity.CraftItem");
			final Class<?> craftServerClass = getOBCClass("CraftServer");

			final Object bukkitItem = craftItemClass.getConstructor(craftServerClass, nmsItemClass).newInstance(Bukkit.getServer(), nmsEntity);
			Valid.checkBoolean(bukkitItem instanceof Item, "Failed to make an dropped item, got " + bukkitItem.getClass().getSimpleName());

			modifier.accept((Item) bukkitItem);

			{ // add to the world + call event
				final Method addEntity = location.getWorld().getClass().getMethod("addEntity", nmsEntityClass, SpawnReason.class);
				addEntity.invoke(location.getWorld(), nmsEntity, SpawnReason.CUSTOM);
			}

			return (Item) bukkitItem;

		} catch (final ReflectiveOperationException ex) {
			Common.error(ex, "Error spawning item " + item.getType() + " at " + location);

			return null;
		}
	}

	/**
	 * Return NMS copy of the given itemstack
	 *
	 * @param itemStack
	 * @return
	 */
	public static Object asNMSCopy(ItemStack itemStack) {
		try {
			final Method asNmsCopy = getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			return asNmsCopy.invoke(null, itemStack);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to convert item to NMS item: " + itemStack);

			return null;
		}
	}

	/**
	 * Sets a data of a block in the world.
	 *
	 * @param block
	 * @param data
	 */
	public static void setData(final Block block, final int data) {
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
	public static void setTypeAndData(final Block block, final CompMaterial material, final byte data) {
		setTypeAndData(block, material.getMaterial(), data);
	}

	/**
	 * Sets a block type and its data, applying physics.
	 *
	 * @param block
	 * @param material
	 * @param data
	 */
	public static void setTypeAndData(final Block block, final Material material, final byte data) {
		setTypeAndData(block, material, data, true);
	}

	/**
	 * Sets a block type and its data.
	 *
	 * @param block
	 * @param material
	 * @param data
	 * @param physics
	 */
	public static void setTypeAndData(final Block block, final Material material, final byte data, final boolean physics) {
		if (MinecraftVersion.atLeast(V.v1_13)) {
			block.setType(material);
			block.setBlockData(Bukkit.getUnsafe().fromLegacy(material, data), physics);

		} else
			try {
				block.getClass().getMethod("setTypeIdAndData", int.class, byte.class, boolean.class).invoke(block, material.getId(), data, physics);
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();
			}
	}

	/**
	 * Converts json string into legacy colored text
	 *
	 * @param json
	 * @return
	 * @throws InteractiveTextFoundException
	 */
	public static String toLegacyText(final String json) throws InteractiveTextFoundException {
		return toLegacyText(json, true);
	}

	/**
	 * Converts chat message in JSON (IChatBaseComponent) to one lined old style
	 * message with color codes. e.g. {text:"Hello world",color="red"} converts to
	 * &cHello world
	 * @param json
	 *
	 * @param denyEvents if an exception should be thrown if hover/click event is
	 *                   found.
	 * @return
	 * @throws InteractiveTextFoundException if click/hover event are found. Such
	 *                                       events would be removed, and therefore
	 *                                       message containing them shall not be
	 *                                       unpacked
	 */
	public static String toLegacyText(final String json, final boolean denyEvents) throws InteractiveTextFoundException {
		Valid.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");
		final StringBuilder text = new StringBuilder();

		// Translate options does not want to work well with ChatControl
		if (json.contains("\"translate\""))
			return text.append("").toString();

		try {
			for (final BaseComponent comp : ComponentSerializer.parse(json)) {
				if ((comp.getHoverEvent() != null || comp.getClickEvent() != null) && denyEvents)
					throw new InteractiveTextFoundException();

				text.append(comp.toLegacyText());
			}

		} catch (final Throwable throwable) {

			// Do not catch our own exception
			if (throwable instanceof InteractiveTextFoundException)
				throw throwable;
		}

		return text.toString();
	}

	/**
	 * Return the given list as JSON
	 *
	 * @param list
	 * @return
	 */
	public static String toJson(final Collection<String> list) {
		return gson.toJson(list);
	}

	/**
	 * Convert the given json into list
	 *
	 * @param json
	 * @return
	 */
	public static List<String> fromJsonList(String json) {
		return gson.fromJson(json, List.class);
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 * @param message
	 * @return
	 */
	public static String toJson(final String message) {
		Valid.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");

		return toJson(TextComponent.fromLegacyText(message));
	}

	/**
	 * Converts base components into json
	 *
	 * @param comps
	 * @return
	 */
	public static String toJson(final BaseComponent... comps) {
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
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string
	 * for sending with {@link net.md_5.bungee.api.chat.BaseComponent}'s.
	 *
	 * @param item the item to convert
	 * @return the Json string representation of the item
	 */
	public static String toJson(ItemStack item) {
		// ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
		final Class<?> craftItemstack = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
		final Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemstack, "asNMSCopy", ItemStack.class);

		// NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
		final Class<?> nmsItemStack = ReflectionUtil.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
		final Class<?> nbtTagCompound = ReflectionUtil.getNMSClass("NBTTagCompound", "net.minecraft.nbt.NBTTagCompound");
		final Method saveItemstackMethod = ReflectionUtil.getMethod(nmsItemStack, MinecraftVersion.atLeast(V.v1_18) ? "b" : "save", nbtTagCompound);

		final Object nmsNbtTagCompoundObj = ReflectionUtil.instantiate(nbtTagCompound);
		final Object nmsItemStackObj = ReflectionUtil.invoke(asNMSCopyMethod, null, item);
		final Object itemAsJsonObject = ReflectionUtil.invoke(saveItemstackMethod, nmsItemStackObj, nmsNbtTagCompoundObj);

		// Return a string representation of the serialized object
		return itemAsJsonObject.toString();
	}

	/**
	 * Converts json into base component array
	 *
	 * @param json
	 * @return
	 */
	public static BaseComponent[] toComponent(final String json) {
		Valid.checkBoolean(bungeeApiPresent, "(Un)packing chat requires Spigot 1.7.10 or newer");

		try {
			return ComponentSerializer.parse(json);

		} catch (final Throwable t) {
			Common.throwError(t,
					"Failed to call toComponent!",
					"Json: " + json,
					"Error: %error%");

			return null;
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 * @param placeholders
	 */
	public static void sendJson(final CommandSender sender, final String json, final SerializedMap placeholders) {
		try {
			final BaseComponent[] components = ComponentSerializer.parse(json);

			if (MinecraftVersion.atLeast(V.v1_16))
				replaceHexPlaceholders(Arrays.asList(components), placeholders);

			sendComponent(sender, components);

		} catch (final RuntimeException ex) {
			Common.error(ex, "Malformed JSON when sending message to " + sender.getName() + " with JSON: " + json);
		}
	}

	/*
	 * A helper Method for MC 1.16+ to partially solve the issue of HEX colors in JSON
	 *
	 * BaseComponent does not support colors when in text, they must be set at the color level
	 */
	private static void replaceHexPlaceholders(final List<BaseComponent> components, final SerializedMap placeholders) {

		for (final BaseComponent component : components) {
			if (component instanceof TextComponent) {
				final TextComponent textComponent = (TextComponent) component;
				String text = textComponent.getText();

				for (final Map.Entry<String, Object> entry : placeholders.entrySet()) {
					String key = entry.getKey();
					String value = Common.simplify(entry.getValue());

					// Detect HEX in placeholder
					final Matcher match = RGB_HEX_ENCODED_REGEX.matcher(text);

					while (match.find()) {

						// Find the color
						final String color = "#" + match.group(2).replace(ChatColor.COLOR_CHAR + "", "");

						// Remove it from chat and bind it to TextComponent instead
						value = match.replaceAll("");
						textComponent.setColor(net.md_5.bungee.api.ChatColor.of(color));
					}

					key = key.charAt(0) != '{' ? "{" + key : key;
					key = key.charAt(key.length() - 1) != '}' ? key + "}" : key;

					text = text.replace(key, value);
					textComponent.setText(text);
				}
			}

			if (component.getExtra() != null)
				replaceHexPlaceholders(component.getExtra(), placeholders);

			if (component.getHoverEvent() != null)
				replaceHexPlaceholders(Arrays.asList(component.getHoverEvent().getValue()), placeholders);
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final CommandSender sender, final String json) {
		try {
			sendComponent(sender, ComponentSerializer.parse(json));

		} catch (final Throwable t) {

			// Silence a bug in md_5's library
			if (t.toString().contains("missing 'text' property"))
				return;

			throw new RuntimeException("Malformed JSON when sending message to " + sender.getName() + " with JSON: " + json, t);
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param comps
	 */
	public static void sendComponent(final CommandSender sender, final Object comps) {
		BungeeChatProvider.sendComponent(sender, comps);
	}

	/**
	 * Sends a title to the player (1.8+) for three seconds
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 */
	public static void sendTitle(final Player player, final String title, final String subtitle) {
		sendTitle(player, 20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player (1.8+) Texts will be colorized.
	 *
	 * @param player   the player
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public static void sendTitle(final Player player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		if (MinecraftVersion.newerThan(V.v1_7))
			if (hasExtendedPlayerTitleAPI)
				player.sendTitle(Common.colorize(title), Common.colorize(subtitle), fadeIn, stay, fadeOut);
			else
				ChatInternals.sendTitleLegacy(player, fadeIn, stay, fadeOut, title, subtitle);
		else {
			Common.tell(player, title);
			Common.tell(player, subtitle);
		}
	}

	/**
	 * Resets the title that is being displayed to the player (1.8+)
	 *
	 * @param player the player
	 */
	public static void resetTitle(final Player player) {
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
	public static void sendTablist(final Player player, final String header, final String footer) {
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
	public static void sendActionBar(final Player player, final String text) {
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
	public static void sendBossbarPercent(final Player player, final String message, final float percent) {
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
	public static void sendBossbarPercent(final Player player, final String message, final float percent, final CompBarColor color, final CompBarStyle style) {
		BossBarInternals.getInstance().setMessage(player, message, percent, color, style);
	}

	/**
	 * Send boss bar only for limited time
	 *
	 * @param player
	 * @param message
	 * @param seconds
	 */
	public static void sendBossbarTimed(final Player player, final String message, final int seconds) {
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
	public static void sendBossbarTimed(final Player player, final String message, final int seconds, final CompBarColor color, final CompBarStyle style) {
		BossBarInternals.getInstance().setMessage(player, message, seconds, color, style);
	}

	/**
	 * Attempts to remove a boss bar from player.
	 * <p>
	 * Only works if you rendered it through methods in this class!
	 *
	 * @param player
	 */
	public static void removeBossBar(final Player player) {
		BossBarInternals.getInstance().removeBar(player);
	}

	/**
	 * Broadcast a chest open animation at the given block,
	 * the block must be a chest!
	 *
	 * @param block
	 */
	public static void sendChestClose(Block block) {
		sendChestAction(block, 0);
	}

	/**
	 * Broadcast a chest open animation at the given block,
	 * the block must be a chest!
	 *
	 * @param block
	 */
	public static void sendChestOpen(Block block) {
		sendChestAction(block, 1);
	}

	/*
	 * A helper method
	 */
	private static void sendChestAction(Block block, int action) {

		final BlockState state = block.getState();
		Valid.checkBoolean(state instanceof Chest, "You can only send chest action packet for chests not " + block);

		try {
			if (action == 1)
				((Chest) state).open();
			else
				((Chest) state).close();

		} catch (final NoSuchMethodError t) {
			final Location location = block.getLocation();

			final Class<?> blockClass = getNMSClass("Block");
			final Class<?> blocks = getNMSClass("Blocks");

			final Object position = ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("BlockPosition", double.class, double.class, double.class), location.getX(), location.getY(), location.getZ());
			final Object packet = ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("PacketPlayOutBlockAction",
					ReflectionUtil.getNMSClass("BlockPosition"), blockClass, int.class, int.class), position, ReflectionUtil.getStaticFieldContent(blocks, "CHEST"), 1, action);

			for (final Player player : getOnlinePlayers())
				sendPacket(player, packet);
		}
	}

	/**
	 * Creates new plugin command from given label
	 *
	 * @param label
	 * @return
	 */
	public static PluginCommand newCommand(final String label) {
		try {
			final Constructor<PluginCommand> con = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
			con.setAccessible(true);

			return con.newInstance(label, SimplePlugin.getInstance());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Unable to create command: /" + label);
		}
	}

	/**
	 * Sets a custom command name
	 *
	 * @param command
	 * @param name
	 */
	public static void setCommandName(final PluginCommand command, final String name) {
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
	public static void registerCommand(final Command command) {
		final CommandMap commandMap = getCommandMap();
		commandMap.register(command.getLabel(), command);

		Valid.checkBoolean(command.isRegistered(), "Command /" + command.getLabel() + " could not have been registered properly!");
	}

	/**
	 * Removes a command by its label from command map, includes all aliases
	 *
	 * @param label the label
	 */
	public static void unregisterCommand(final String label) {
		unregisterCommand(label, true);
	}

	/**
	 * Removes a command by its label from command map, optionally can also remove
	 * aliases
	 *
	 * @param label          the label
	 * @param removeAliases also remove aliases?
	 */
	public static void unregisterCommand(final String label, final boolean removeAliases) {
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

			final Map<String, Command> cmdMap = (Map<String, Command>) f.get(getCommandMap());

			cmdMap.remove(label);

			if (command != null && removeAliases)
				for (final String alias : command.getAliases())
					cmdMap.remove(alias);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Failed to unregister command /" + label);
		}
	}

	/**
	 * Return the server's command map
	 *
	 * @return
	 */
	public static SimpleCommandMap getCommandMap() {
		try {
			return (SimpleCommandMap) getOBCClass("CraftServer").getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Unable to get the command map");
		}
	}

	/**
	 * Register a new enchantment in Bukkit, unregistering it first to avoid errors
	 *
	 * @param enchantment
	 */
	public static void registerEnchantment(final Enchantment enchantment) {
		unregisterEnchantment(enchantment);

		ReflectionUtil.setStaticField(Enchantment.class, "acceptingNew", true);
		Enchantment.registerEnchantment(enchantment);
	}

	/**
	 * Unregister an enchantment from Bukkit. Works even for vanilla MC enchantments (found in Enchantment class)
	 *
	 * @param enchantment
	 */
	public static void unregisterEnchantment(final Enchantment enchantment) {

		if (MinecraftVersion.atLeast(V.v1_13)) { // Unregister by key
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
	public static Location getLocation(final Inventory inv) {
		if (hasInventoryLocation)
			try {
				return inv.getLocation();

			} catch (final NullPointerException ex) { // EnderChest throws this
				return null;
			}

		return inv.getHolder() instanceof BlockState ? ((BlockState) inv.getHolder()).getLocation() : !inv.getViewers().isEmpty() ? inv.getViewers().iterator().next().getLocation() : null;
	}

	/**
	 * Return the biome at the given location
	 *
	 * @param loc
	 * @return
	 */
	public static Biome getBiome(Location loc) {
		try {
			return loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

		} catch (final NoSuchMethodError err) {
			return loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockZ());
		}
	}

	/**
	 * Return the language of the player's Minecraft client
	 * <p>
	 * See {@link Player#getLocale()}
	 * <p>
	 * Returns null if not available for your MC version
	 *
	 * @param player
	 * @return
	 */
	public static String getLocale(final Player player) {
		try {
			return player.getLocale();

		} catch (final Throwable t) {
			try {
				final Player.Spigot spigot = player.spigot();
				final Method method = ReflectionUtil.getMethod(spigot.getClass(), "getLocale");

				return (String) ReflectionUtil.invoke(method, spigot);

			} catch (final Throwable tt) {
				return null;
			}
		}
	}

	/**
	 * Return the NMS statistic name for the given statistic
	 *
	 * @param stat
	 * @param mat
	 * @param en
	 * @return
	 */
	public static String getNMSStatisticName(final Statistic stat, final Material mat, final EntityType en) {
		final Class<?> craftStatistic = getOBCClass("CraftStatistic");
		Object nmsStatistic = null;

		try {
			if (stat.getType() == Type.UNTYPED)
				nmsStatistic = craftStatistic.getMethod("getNMSStatistic", stat.getClass()).invoke(null, stat);

			else if (stat.getType() == Type.ENTITY)
				nmsStatistic = craftStatistic.getMethod("getEntityStatistic", stat.getClass(), en.getClass()).invoke(null, stat, en);

			else
				nmsStatistic = craftStatistic.getMethod("getMaterialStatistic", stat.getClass(), mat.getClass()).invoke(null, stat, mat);

			Valid.checkNotNull(nmsStatistic, "Could not get NMS statistic from Bukkit's " + stat);

			if (MinecraftVersion.equals(V.v1_8)) {
				final Field f = nmsStatistic.getClass().getField("name");
				f.setAccessible(true);
				return f.get(nmsStatistic).toString();
			}

			return (String) nmsStatistic.getClass().getMethod(MinecraftVersion.atLeast(V.v1_18) ? "d" : "getName").invoke(nmsStatistic);
		} catch (final Throwable t) {
			throw new FoException(t, "Error getting NMS statistic name from " + stat);
		}
	}

	/**
	 * Attempts to respawn the player after 2 ticks, either via native method or reflection
	 *
	 * @param player
	 */
	public static void respawn(final Player player) {
		respawn(player, 2);
	}

	/**
	 * Attempts to respawn the player, either via native method or reflection
	 *
	 * @param player
	 * @param delayTicks how long to way before respawning, minimum 1 tick
	 */
	public static void respawn(final Player player, final int delayTicks) {
		Common.runLater(delayTicks, () -> {
			try {
				player.spigot().respawn();

			} catch (final NoSuchMethodError err) {
				try {
					final Object respawnEnum = getNMSClass("EnumClientCommand", "N/A").getEnumConstants()[0];
					final Constructor<?>[] constructors = getNMSClass("PacketPlayInClientCommand", "N/A").getConstructors();

					for (final Constructor<?> constructor : constructors) {
						final Class<?>[] args = constructor.getParameterTypes();
						if (args.length == 1 && args[0] == respawnEnum.getClass()) {
							final Object packet = getNMSClass("PacketPlayInClientCommand", "N/A").getConstructor(args).newInstance(respawnEnum);

							sendPacket(player, packet);
							break;
						}
					}

				} catch (final Throwable e) {
					throw new FoException(e, "Failed to send respawn packet to " + player.getName());
				}
			}
		});
	}

	/**
	 * Opens the book for the player given the book is a WRITTEN_BOOK
	 *
	 * @param player
	 * @param book
	 */
	public static void openBook(Player player, ItemStack book) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_8), "Opening books is only supported on MC 1.8 and greater");
		Valid.checkBoolean(book.getItemMeta() instanceof BookMeta, "openBook method called for not a book item: " + book);

		// Fix "Invalid book tag" error when author/title is empty
		final BookMeta meta = (BookMeta) book.getItemMeta();

		if (meta.getAuthor() == null)
			meta.setAuthor("");

		if (meta.getTitle() == null)
			meta.setTitle("");

		book.setItemMeta(meta);

		try {

			player.openBook(book);

		} catch (final NoSuchMethodError ex) {
			final ItemStack oldItem = player.getItemInHand();

			// Set the book temporarily to hands
			player.setItemInHand(book);

			final Object craftPlayer = getHandleEntity(player);
			final Object nmsItemstack = asNMSCopy(book);

			Common.runLater(() -> {
				final Method openInventory = ReflectionUtil.getMethod(craftPlayer.getClass(), "openBook", nmsItemstack.getClass());
				ReflectionUtil.invoke(openInventory, craftPlayer, nmsItemstack);

				// Reset hands
				player.setItemInHand(oldItem);
			});
		}
	}

	/**
	 * Update the player's inventory title without closing the window
	 *
	 * @param player the player
	 * @param title  the new title
	 * @deprecated use {@link PlayerUtil#updateInventoryTitle(Player, String)}
	 */
	@Deprecated
	public static void updateInventoryTitle(final Player player, String title) {

		try {

			if (MinecraftVersion.atLeast(V.v1_17) || MinecraftVersion.atLeast(V.v1_18)) {
				final boolean is1_18 = MinecraftVersion.atLeast(V.v1_18);

				final Object nmsPlayer = Remain.getHandleEntity(player);
				final Object chatComponent = toIChatBaseComponentPlain(ChatColor.translateAlternateColorCodes('&', title));

				final int inventorySize = player.getOpenInventory().getTopInventory().getSize() / 9;
				String containerName;

				if (inventorySize == 1)
					containerName = "a";

				else if (inventorySize == 2)
					containerName = "b";

				else if (inventorySize == 3)
					containerName = "c";

				else if (inventorySize == 4)
					containerName = "d";

				else if (inventorySize == 5)
					containerName = "e";

				else if (inventorySize == 6)
					containerName = "f";
				else
					throw new FoException("Cannot generate NMS container class to update inventory of size " + inventorySize);

				final Object container = ReflectionUtil.getStaticFieldContent(ReflectionUtil.lookupClass("net.minecraft.world.inventory.Containers"), containerName);

				final Constructor<?> packetConstructor = ReflectionUtil.getConstructor(
						"net.minecraft.network.protocol.game.PacketPlayOutOpenWindow",
						int.class,
						container.getClass(),
						ReflectionUtil.lookupClass("net.minecraft.network.chat.IChatBaseComponent"));

				final Object activeContainer = ReflectionUtil.getFieldContent(nmsPlayer, is1_18 ? "bW" : "bV");
				final int windowId = ReflectionUtil.getFieldContent(activeContainer, "j");

				final Method method = is1_18 ? ReflectionUtil.getMethod(nmsPlayer.getClass(), "a", ReflectionUtil.lookupClass("net.minecraft.world.inventory.Container")) : null;

				Remain.sendPacket(player, ReflectionUtil.instantiate(packetConstructor, windowId, container, chatComponent));

				if (is1_18)
					ReflectionUtil.invoke(method, nmsPlayer, activeContainer);

				else
					ReflectionUtil.invoke("initMenu", nmsPlayer, activeContainer);

				return;
			}

			if (MinecraftVersion.olderThan(V.v1_9) && title.length() > 32)
				title = title.substring(0, 32);

			final Object entityPlayer = getHandleEntity(player);
			final Object activeContainer = entityPlayer.getClass().getField("activeContainer").get(entityPlayer);
			final Object windowId = activeContainer.getClass().getField("windowId").get(activeContainer);

			final Object packetOpenWindow;

			if (MinecraftVersion.atLeast(V.v1_8)) {
				final Constructor<?> chatMessageConst = getNMSClass("ChatMessage", "net.minecraft.network.chat.ChatMessage").getConstructor(String.class, Object[].class);
				final Object chatMessage = chatMessageConst.newInstance(ChatColor.translateAlternateColorCodes('&', title), new Object[0]);

				if (MinecraftVersion.newerThan(V.v1_13)) {
					final int inventorySize = player.getOpenInventory().getTopInventory().getSize() / 9;

					if (inventorySize < 1 || inventorySize > 6) {
						Common.log("Cannot update title for " + player.getName() + " as their inventory has non typical size: " + inventorySize + " rows");

						return;
					}

					final Class<?> containersClass = getNMSClass("Containers", "net.minecraft.world.inventory.Containers");
					final Constructor<?> packetConst = getNMSClass("PacketPlayOutOpenWindow", "net.minecraft.network.protocol.game.PacketPlayOutOpenWindow")
							.getConstructor(/*windowID*/int.class, /*containers*/containersClass, /*msg*/getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"));

					final String containerName = "GENERIC_9X" + inventorySize;

					final Object container = containersClass.getField(containerName).get(null);

					packetOpenWindow = packetConst.newInstance(windowId, container, chatMessage);

				} else {
					final Constructor<?> packetConst = getNMSClass("PacketPlayOutOpenWindow", "N/A").getConstructor(int.class, String.class, getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"), int.class);

					packetOpenWindow = packetConst.newInstance(windowId, "minecraft:chest", chatMessage, player.getOpenInventory().getTopInventory().getSize());
				}
			} else {
				final Constructor<?> openWindow = ReflectionUtil.getConstructor(
						getNMSClass(MinecraftVersion.atLeast(V.v1_7) ? "PacketPlayOutOpenWindow" : "Packet100OpenWindow", "N/A"), int.class, int.class, String.class, int.class, boolean.class);

				packetOpenWindow = ReflectionUtil.instantiate(openWindow, windowId, 0, ChatColor.translateAlternateColorCodes('&', title), player.getOpenInventory().getTopInventory().getSize(), true);
			}

			sendPacket(player, packetOpenWindow);
			entityPlayer.getClass().getMethod("updateInventory", getNMSClass("Container", "net.minecraft.world.inventory.Container")).invoke(entityPlayer, activeContainer);

		} catch (final ReflectiveOperationException ex) {
			Common.error(ex, "Error updating " + player.getName() + " inventory title to '" + title + "'");
		}
	}

	/**
	 * Sends a fake block update to a certain location, and than reverts it back to
	 * the real block after a while.
	 *
	 * @param delayTicks the pause between reverting back
	 * @param player     the player
	 * @param location   the location
	 * @param material   the material
	 */
	public static void sendBlockChange(final int delayTicks, final Player player, final Location location, final CompMaterial material) {
		if (delayTicks > 0)
			Common.runLater(delayTicks, () -> sendBlockChange0(player, location, material));
		else
			sendBlockChange0(player, location, material);
	}

	private static void sendBlockChange0(final Player player, final Location location, final CompMaterial material) {
		try {
			player.sendBlockChange(location, material.getMaterial().createBlockData());
		} catch (final NoSuchMethodError ex) {
			player.sendBlockChange(location, material.getMaterial(), material.getData());
		}
	}

	/**
	 * Sends to the player the block update packet of the given block, typically
	 * to reset it back to the real state
	 *
	 * @param delayTicks
	 * @param player
	 * @param block
	 */
	public static void sendBlockChange(final int delayTicks, final Player player, final Block block) {
		if (delayTicks > 0)
			Common.runLater(delayTicks, () -> sendBlockChange0(player, block));
		else
			sendBlockChange0(player, block);
	}

	private static void sendBlockChange0(final Player player, final Block block) {
		try {
			player.sendBlockChange(block.getLocation(), block.getBlockData());
		} catch (final NoSuchMethodError ex) {
			player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
		}
	}

	/**
	 * Return how long the player has played on this server (pulled from your world statistics file)
	 * in minutes
	 *
	 * @param player
	 * @return
	 */
	public static int getPlaytimeMinutes(final Player player) {
		final Statistic stat = getPlayTimeStatisticName();

		return player.getStatistic(stat) / (stat.name().contains("TICK") ? 20 * 60 : 60 * 60);
	}

	/**
	 * Return either PLAY_ONE_TICK for MC <1.13 or PLAY_ONE_MINUTE for 1.13+
	 *
	 * @return
	 */
	public static Statistic getPlayTimeStatisticName() {
		return Statistic.valueOf(MinecraftVersion.olderThan(V.v1_13) ? "PLAY_ONE_TICK" : "PLAY_ONE_MINUTE");
	}

	/**
	 * Return if the play time statistic is measured in ticks
	 *
	 * @return
	 */
	public static boolean isPlaytimeStatisticTicks() {
		return MinecraftVersion.olderThan(V.v1_13);
	}

	/**
	 * Since Minecraft introduced double yelding, it fires two events for
	 * interaction for each hand. Return if the event was fired for the main hand.
	 * <p>
	 * Backwards compatible.
	 *
	 * @param event the event
	 * @return if the event was fired for main hand only
	 */
	public static boolean isInteractEventPrimaryHand(final PlayerInteractEvent event) {

		if (MinecraftVersion.olderThan(V.v1_9))
			return true;

		try {
			return event.getHand() != null && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;

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
	public static boolean isInteractEventPrimaryHand(final PlayerInteractEntityEvent e) {

		if (MinecraftVersion.olderThan(V.v1_9))
			return true;

		try {
			return e.getHand() != null && e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;

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
	public static Score getScore(final Objective obj, String entry) {
		Valid.checkNotNull(obj, "Objective cannot be null");

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
	 * @param id
	 * @return
	 */
	public static OfflinePlayer getOfflinePlayerByUUID(final UUID id) {
		try {
			return Bukkit.getOfflinePlayer(id);

		} catch (final NoSuchMethodError err) {
			if (Bukkit.isPrimaryThread())
				Common.log("getOfflinePlayerByUUID required two blocking calls on main thread - please notify " + SimplePlugin.getNamed() + " plugin authors.");

			final UUIDToNameConverter f = new UUIDToNameConverter(id);

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
	 *
	 * @return null if offline or player
	 */
	public static Player getPlayerByUUID(final UUID id) {
		try {
			final Player player = Bukkit.getPlayer(id);

			return player != null && player.isOnline() ? player : null;

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
	 * @param event
	 * @return
	 */
	public static double getFinalDamage(final EntityDamageEvent event) {
		try {
			return event.getFinalDamage();

		} catch (final NoSuchMethodError err) {
			return event.getDamage();
		}
	}

	/**
	 * Return the correct inventory that was clicked (either bottom or top inventory
	 * or null if clicked outside)
	 *
	 * @param event the inventory click event
	 * @return the actual inventory clicked, either bottom or top, or null if
	 * clicked outside
	 */
	public static Inventory getClickedInventory(final InventoryClickEvent event) {
		final int slot = event.getRawSlot();
		final InventoryView view = event.getView();

		return slot < 0 ? null : view.getTopInventory() != null && slot < view.getTopInventory().getSize() ? view.getTopInventory() : view.getBottomInventory();
	}

	/**
	 * Return a list of pages (new MC also will expose interactive elements)
	 * in a book
	 *
	 * @param meta
	 * @return
	 */
	public static List<BaseComponent[]> getPages(BookMeta meta) {
		try {
			return meta.spigot().getPages();

		} catch (final NoSuchMethodError ex) {
			final List<BaseComponent[]> list = new ArrayList<>();

			for (final String page : meta.getPages())
				list.add(TextComponent.fromLegacyText(page));

			return list;
		}
	}

	/**
	 * Attempts to set the book pages from the given list
	 *
	 * @param meta
	 * @param pages
	 */
	public static void setPages(BookMeta meta, List<BaseComponent[]> pages) {
		try {
			meta.spigot().setPages(pages);

		} catch (final NoSuchMethodError ex) {
			try {
				final List<Object> chatComponentPages = (List<Object>) ReflectionUtil.getFieldContent(ReflectionUtil.getOBCClass("inventory.CraftMetaBook"), "pages", meta);

				for (final BaseComponent[] text : pages)
					chatComponentPages.add(toIChatBaseComponent(text));

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Return IChatBaseComponent from the given plain text
	 *
	 * @param text
	 * @return
	 */
	public static Object toIChatBaseComponentPlain(String text) {
		return toIChatBaseComponent(TextComponent.fromLegacyText(text));
	}

	/**
	 * Return IChatBaseComponent from the given component list
	 *
	 * @param baseComponents
	 * @return
	 */
	public static Object toIChatBaseComponent(BaseComponent[] baseComponents) {
		return toIChatBaseComponent(toJson(baseComponents));
	}

	/**
	 * Return IChatBaseComponent from the given JSON
	 *
	 * @param json
	 * @return
	 */
	public static Object toIChatBaseComponent(String json) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "Serializing chat components requires Minecraft 1.7.10 and greater");

		final Class<?> chatSerializer = ReflectionUtil.getNMSClass((MinecraftVersion.equals(V.v1_7) ? "" : "IChatBaseComponent$") + "ChatSerializer", "net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");
		final Method a = ReflectionUtil.getMethod(chatSerializer, "a", String.class);

		return ReflectionUtil.invoke(a, null, json);
	}

	/**
	 * Return the name of the entity
	 *
	 * @param entity
	 * @return
	 */
	public static String getName(final Entity entity) {
		try {
			return entity.getName();

		} catch (final NoSuchMethodError t) {
			return entity instanceof Player ? ((Player) entity).getName() : ItemUtil.bountifyCapitalized(entity.getType());
		}
	}

	/**
	 * Sets a custom name to entity
	 *
	 * @param entity
	 * @param name
	 */
	public static void setCustomName(final Entity entity, final String name) {
		try {
			entity.setCustomNameVisible(true);
			entity.setCustomName(Common.colorize(name));

		} catch (final NoSuchMethodError er) {
			Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "setCustomName requires Minecraft 1.7.10+");

			final NBTEntity nbt = new NBTEntity(entity);

			nbt.setInteger("CustomNameVisible", 1);
			nbt.setString("CustomName", Common.colorize(name));
		}
	}

	/**
	 * Removes a custom name if the entity had it
	 *
	 * @param entity
	 */
	public static void removeCustomName(final Entity entity) {
		try {
			entity.setCustomNameVisible(false);
			entity.setCustomName(null);

		} catch (final NoSuchMethodError er) {
			Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "setCustomName requires Minecraft 1.7.10+");

			final NBTEntity nbt = new NBTEntity(entity);

			nbt.removeKey("CustomNameVisible");
			nbt.removeKey("CustomName");
		}
	}

	/**
	 * Calls NMS to find out if the entity is invisible, works for any entity,
	 * better than Bukkit since it has extreme downwards compatibility and does not require LivingEntity
	 *
	 * @deprecated use {@link PlayerUtil#isVanished(Player)} to check for vanish from other plugins also
	 *
	 * @param entity
	 * @return
	 */
	@Deprecated
	public static boolean isInvisible(Entity entity) {
		if (entity instanceof LivingEntity && MinecraftVersion.atLeast(V.v1_16))
			return ((LivingEntity) entity).isInvisible();

		else if (MinecraftVersion.atLeast(V.v1_4)) {
			final Object nmsEntity = getHandleEntity(entity);

			return (boolean) ReflectionUtil.invoke("isInvisible", nmsEntity);
		}

		return false;
	}

	/**
	 * Calls NMS to set invisibility status of any entity,
	 * better than Bukkit since it has extreme downwards compatibility and does not require LivingEntity
	 *
	 * @param entity
	 * @param invisible
	 *
	 * @deprecated use {@link PlayerUtil#setVanished(Player, boolean)} to disable vanish for plugins also
	 */
	@Deprecated
	public static void setInvisible(Object entity, boolean invisible) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_4), "Entity#setInvisible requires Minecraft 1.4.7 or greater");

		if (entity instanceof LivingEntity && MinecraftVersion.atLeast(V.v1_16))
			((LivingEntity) entity).setInvisible(invisible);

		else {
			final Object nmsEntity = entity.getClass().toString().contains("net.minecraft.server") ? entity : entity instanceof LivingEntity ? getHandleEntity((LivingEntity) entity) : null;
			Valid.checkNotNull(nmsEntity, "setInvisible requires either a LivingEntity or a NMS Entity, got: " + entity.getClass());

			// https://www.spigotmc.org/threads/how-do-i-make-an-entity-go-invisible-without-using-potioneffects.321227/
			Common.runLater(2, () -> ReflectionUtil.invoke("setInvisible", nmsEntity, invisible));
		}
	}

	/**
	 * Return if the given entity is invulnerable
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isInvulnerable(Entity entity) {
		try {
			return entity.isInvulnerable();

		} catch (final NoSuchMethodError ex) {

			if (fieldEntityInvulnerable != null)
				try {
					return (boolean) fieldEntityInvulnerable.get(getHandleEntity(entity));

				} catch (final ReflectiveOperationException exx) {
				}

			return false;
		}
	}

	/**
	 * Set the invulnerable status for an entity,
	 * this may fail on old Minecraft versions such as 1.7.10.
	 *
	 * @param entity
	 * @param invulnerable
	 */
	public static void setInvulnerable(Entity entity, boolean invulnerable) {
		CompProperty.INVULNERABLE.apply(entity, invulnerable);
	}

	/**
	 * Tries to get the first material, or return the second as fall back
	 *
	 * @param material
	 * @param fallback
	 * @return
	 */
	public static CompMaterial getMaterial(final String material, final CompMaterial fallback) {
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
	public static Material getMaterial(final String newMaterial, final String oldMaterial) {
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
	public static Block getTargetBlock(final LivingEntity en, final int radius) {
		try {
			return en.getTargetBlock((Set<Material>) null, radius);

		} catch (final Throwable t) {
			if (t instanceof IllegalStateException)
				return null;

			try {
				return (Block) en.getClass().getMethod("getTargetBlock", HashSet.class, int.class).invoke(en, (HashSet<Byte>) null, radius);

			} catch (final ReflectiveOperationException ex2) {
				throw new FoException(t, "Unable to get target block for " + en);
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
	public static void sendToast(final Player receiver, final String message) {
		sendToast(receiver, message, CompMaterial.BOOK);
	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot
	 * be modified that much. It imposes a slight performance penalty.
	 *
	 * @param receiver
	 * @param message
	 * @param icon
	 */
	public static void sendToast(final Player receiver, final String message, final CompMaterial icon) {
		if (message != null && !message.isEmpty()) {
			final String colorized = Common.colorize(message);

			if (!colorized.isEmpty()) {
				Valid.checkSync("Toasts may only be sent from the main thread");

				if (hasAdvancements)
					new AdvancementAccessor(colorized, icon.toString().toLowerCase()).show(receiver);

				else
					receiver.sendMessage(colorized);
			}
		}
	}

	/**
	 * Send a "toast" notification to the given receivers. This is an advancement notification that cannot
	 * be modified that much. It imposes a slight performance penalty the more players to send to.
	 *
	 * Each player sending is delayed by 0.1s
	 *
	 * @param receivers
	 * @param message you can replace player-specific variables in the message here
	 * @param icon
	 */
	public static void sendToast(final List<Player> receivers, final Function<Player, String> message, final CompMaterial icon) {

		if (hasAdvancements) {
			Common.runLaterAsync(() -> {
				for (final Player receiver : receivers) {

					// Sleep to mitigate sending not working at once
					Common.sleep(100);

					Common.runLater(() -> {
						final String colorized = Common.colorize(message.apply(receiver));

						if (!colorized.isEmpty()) {
							final AdvancementAccessor accessor = new AdvancementAccessor(colorized, icon.toString().toLowerCase());

							if (receiver.isOnline())
								accessor.show(receiver);
						}
					});
				}
			});

		} else
			for (final Player receiver : receivers) {
				final String colorized = Common.colorize(message.apply(receiver));

				if (!colorized.isEmpty())
					receiver.sendMessage(colorized);
			}

	}

	/**
	 * Set the visual cooldown for the given material, see {@link Player#setCooldown(Material, int)}
	 * You still have to implement custom handling of it
	 * <p>
	 * Old MC versions are supported and handled by us
	 * however there is no visual effect
	 *
	 * @param player
	 * @param material
	 * @param cooldownTicks
	 */
	public static void setCooldown(final Player player, final Material material, final int cooldownTicks) {
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
	 * <p>
	 * Old MC versions are supported and handled by us
	 * however there is no visual effect
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static boolean hasCooldown(final Player player, final Material material) {
		try {
			return player.hasCooldown(material);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			return cooldown.containsKey(material);
		}
	}

	/**
	 * Return the item cooldown as specified in {@link Player#getCooldown(Material)}
	 * <p>
	 * Old MC versions are supported and handled by us
	 * however there is no visual effect
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static int getCooldown(final Player player, final Material material) {
		try {
			return player.getCooldown(material);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			return cooldown.getOrDefault(material, 0);
		}
	}

	// Internal method to get a players cooldown map
	private static StrictMap<Material, Integer> getCooldown(final Player player) {
		return cooldowns.getOrDefault(player.getUniqueId(), new StrictMap<>());
	}

	/**
	 * Return the player ping
	 *
	 * @deprecated use {@link PlayerUtil#getPing(Player)}
	 * @param player
	 * @return
	 */
	@Deprecated
	public static int getPing(Player player) {
		try {
			return player.getPing();

		} catch (final NoSuchMethodError err) {
			final Object entityPlayer = Remain.getHandleEntity(player);

			return (int) ReflectionUtil.getFieldContent(entityPlayer, "ping");
		}
	}

	/**
	 * Return the entity by UUID
	 *
	 * @param uuid
	 * @return
	 */
	public static Entity getEntity(final UUID uuid) {
		Valid.checkSync("Remain#getEntity must be called on the main thread");

		for (final World world : Bukkit.getWorlds())
			for (final Entity entity : world.getEntities())
				if (entity.getUniqueId().equals(uuid))
					return entity;

		return null;
	}

	/**
	 * Attempts to find the hit entity from the projectile hit event.
	 *
	 * @param event
	 * @return
	 */
	public static LivingEntity getHitEntity(ProjectileHitEvent event) {
		try {

			// Try getting the hit entity directly
			if (event.getHitEntity() instanceof LivingEntity)
				return (LivingEntity) event.getHitEntity();

		} catch (final Throwable t) {

			// If this fails, try getting the entity to which the projectile was attached,
			// imperfect, but mostly works.
			final double radius = 0.5;

			for (final Entity nearby : event.getEntity().getNearbyEntities(radius, radius, radius))
				if (nearby instanceof LivingEntity)
					return (LivingEntity) nearby;
		}

		return null;
	}

	/**
	 * Return nearby entities in a location
	 *
	 * @param location
	 * @param radius
	 * @return
	 */
	public static Collection<Entity> getNearbyEntities(final Location location, final double radius) {
		try {
			return location.getWorld().getNearbyEntities(location, radius, radius, radius);

		} catch (final Throwable t) {
			final List<Entity> found = new ArrayList<>();

			for (final Entity nearby : location.getWorld().getEntities())
				if (nearby.getLocation().distance(location) <= radius)
					found.add(nearby);

			return found;
		}
	}

	/**
	 * Takes one piece of the hand item
	 *
	 * @param player
	 */
	public static void takeHandItem(final Player player) {
		takeItemAndSetAsHand(player, player.getItemInHand());
	}

	/**
	 * Takes one piece of the given item and sets it as hand
	 *
	 * @param player
	 * @param item
	 */
	public static void takeItemAndSetAsHand(final Player player, final ItemStack item) {
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
	public static void takeItemOnePiece(final Player player, final ItemStack item) {
		if (MinecraftVersion.atLeast(V.v1_15))
			item.setAmount(item.getAmount() - 1);

		else
			Common.runLater(() -> {
				if (item.getAmount() > 1)
					item.setAmount(item.getAmount() - 1);
				else if (MinecraftVersion.atLeast(V.v1_9))
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

				player.updateInventory();
			});
	}

	/**
	 * Attempts to insert a potion to the given item with duration of 10 minutes.
	 *
	 * @param item
	 * @param type
	 * @param level
	 */
	public static void setPotion(final ItemStack item, final PotionEffectType type, final int level) {
		setPotion(item, type, 20 * 60 * 10, level);
	}

	/**
	 * Attempts to insert a potion to the given item.
	 *
	 * @param item
	 * @param type
	 * @param durationTicks
	 * @param level
	 */
	public static void setPotion(final ItemStack item, final PotionEffectType type, final int durationTicks, final int level) {
		if (hasItemMeta)
			PotionSetter.setPotion(item, type, durationTicks, level);
	}

	/**
	 * Attempts to return the I18N localized display name, or returns the
	 * capitalized Material name if fails.
	 * <p>
	 * Requires PaperSpigot.
	 *
	 * @param item the {@link ItemStack} to get I18N name from
	 * @return the I18N localized name or Material name
	 */
	public static String getI18NDisplayName(final ItemStack item) {
		try {
			return (String) item.getClass().getDeclaredMethod("getI18NDisplayName").invoke(item);

		} catch (final Throwable t) {
			return ItemUtil.bountifyCapitalized(item.getType());
		}
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
	 * Spigot
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
	 * Converts an unchecked exception into checked
	 *
	 * @param throwable
	 */
	public static void sneaky(final Throwable throwable) {
		try {
			SneakyThrow.sneaky(throwable);

		} catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
	}

	/**
	 * Sets a game rule
	 *
	 * @param world    world to set game rule in
	 * @param gameRule game rule
	 * @param value    value to set (true/false)
	 */
	@SuppressWarnings("rawtypes")
	public static void setGameRule(final World world, final String gameRule, final boolean value) {
		try {
			if (MinecraftVersion.newerThan(V.v1_13)) {
				final GameRule rule = GameRule.getByName(gameRule);

				world.setGameRule(rule, value);
			} else
				world.setGameRuleValue(gameRule, "" + value);

		} catch (final Throwable t) {
			Common.error(t, "Game rule " + gameRule + " not found.");
		}
	}

	/**
	 * New Minecraft versions lack server-name that we rely on for BungeeCord,
	 * restore it back
	 */
	public static void injectServerName() {
		final Properties properties = new Properties();
		final File props = new File(SimplePlugin.getData().getParentFile().getParentFile(), "server.properties");

		// If user has Bungee_Server_Name in their settings, move it automatically
		final File settingsFile = FileUtil.getFile("settings.yml");
		String previousName = null;

		if (settingsFile.exists()) {
			final SimpleYaml settings = SimpleYaml.loadConfiguration(settingsFile);
			final String previousNameRaw = settings.getString("Bungee_Server_Name");

			if (previousNameRaw != null && !previousNameRaw.isEmpty() && !"none".equals(previousNameRaw) && !"undefined".equals(previousNameRaw)) {
				Common.warning("Detected Bungee_Server_Name being used in your settings.yml that is now located in server.properties." +
						" It has been moved there and you can now delete this key from settings.yml if it was not deleted already.");

				previousName = previousNameRaw;
			}
		}

		try (final FileReader fileReader = new FileReader(props)) {
			properties.load(fileReader);

			if (!properties.containsKey("server-name") || previousName != null) {
				properties.setProperty("server-name", previousName != null ? previousName : "Undefined - see mineacademy.org/server-properties to configure");

				try (FileWriter fileWriter = new FileWriter(props)) {
					properties.store(fileWriter, "Minecraft server properties\nModified by " + SimplePlugin.getNamed() + ", see mineacademy.org/server-properties for more information");
				}
			}

			serverName = properties.getProperty("server-name");

		} catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return the server name identifier (used for BungeeCord)
	 *
	 * @return
	 */
	public static String getServerName() {
		Valid.checkBoolean(isServerNameChanged(), "Detected getServerName call, please configure your 'server-name' in server.properties according to mineacademy.org/server-properties");

		return serverName;
	}

	/**
	 * Return true if the server-name property in server.properties got modified
	 *
	 * @return
	 */
	public static boolean isServerNameChanged() {
		return !"see mineacademy.org/server-properties to configure".contains(serverName) && !"undefined".equals(serverName) && !"Unknown Server".equals(serverName);
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
	 * Return the server's ticks per second (requires Paper otherwise we return 20)
	 *
	 * @return
	 */
	public static int getTPS() {

		try {
			final Method getTPS = Bukkit.class.getDeclaredMethod("getTPS", double[].class);

			return (int) MathUtil.floor(getTPS == null ? 20 : ((double[]) getTPS.invoke(null))[0]);
		} catch (final ReflectiveOperationException ex) {

			// Unsupported
			return 20;
		}
	}

	/**
	 * Converts the given object that may be a SectionPathData for MC 1.18 back into its root data
	 * such as {@link MemorySection}
	 *
	 * @param objectOrSectionPathData
	 * @return
	 */
	public static Object getRootOfSectionPathData(Object objectOrSectionPathData) {
		if (objectOrSectionPathData != null && objectOrSectionPathData.getClass() == sectionPathDataClass)
			objectOrSectionPathData = ReflectionUtil.invoke("getData", objectOrSectionPathData);

		return objectOrSectionPathData;
	}

	/**
	 * Return true if the given object is a memory section
	 *
	 * @param obj
	 * @return
	 */
	public static boolean isMemorySection(Object obj) {
		return obj != null && sectionPathDataClass == obj.getClass();
	}

	// ----------------------------------------------------------------------------------------------------
	// Getters for various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return if the server is running Paper, formerly PaperSpigot software.
	 * <p>
	 * Paper is a fork of Spigot compatible with most Bukkit plugins.
	 * <p>
	 * We use the method getTPS to determine if Paper is installed.
	 *
	 * @return true if the server is running Paper(Spigot)
	 */
	public static boolean isPaper() {
		try {
			Class.forName("co.aikar.timings.Timing");

			return true;

		} catch (final Throwable e) {
			return false;
		}
	}

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

	/**
	 * Return if the server version supports {@link YamlConfiguration#load(java.io.Reader)}
	 * otherwise you need to use just {@link InputStream}
	 *
	 * @return
	 */
	public static boolean hasYamlReaderLoad() {
		return hasYamlReaderLoad;
	}

	/**
	 * Return if this MC is likely 1.3.2 and greater
	 *
	 * @return
	 */
	public static boolean hasItemMeta() {
		return hasItemMeta;
	}

	/**
	 * Return if the MC version is 1.16+ that supports HEX RGB colors
	 *
	 * @return
	 */
	public static boolean hasHexColors() {
		return MinecraftVersion.atLeast(V.v1_16);
	}

	/**
	 * Return if the Entity class has the addPassenger method
	 *
	 * @return
	 */
	public static boolean hasAddPassenger() {
		return hasAddPassenger;
	}

	// ------------------------ Legacy ------------------------

	// return the legacy online player array
	private static Player[] getPlayersLegacy() {
		try {
			return (Player[]) getPlayersMethod.invoke(null);
		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Reflection malfunction");
		}
	}

	// return the legacy get health int method
	private static int getHealhLegacy(final LivingEntity entity) {
		try {
			return (int) getHealthMethod.invoke(entity);
		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Reflection malfunction");
		}
	}

	// return the legacy get health int method
	private static int getMaxHealhLegacy(final LivingEntity entity) {
		try {
			final Object number = LivingEntity.class.getMethod("getMaxHealth").invoke(entity);

			if (number instanceof Double)
				return ((Double) number).intValue();
			if (number instanceof Integer)
				return (Integer) number;

			return (int) Double.parseDouble(number.toString());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Reflection malfunction");
		}
	}

	// ------------------------ Utility ------------------------

	/**
	 * Thrown when message contains hover or click events which would otherwise got
	 * removed.
	 * <p>
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

	public static void sneaky(final Throwable t) {
		throw SneakyThrow.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
		throw (T) t;
	}
}

/**
 * A wrapper for bungee chat component library
 */
class BungeeChatProvider {

	static void sendComponent(final CommandSender sender, final Object comps) {
		if (comps instanceof TextComponent)
			sendComponent0(sender, (TextComponent) comps);

		else
			sendComponent0(sender, (BaseComponent[]) comps);
	}

	private static void sendComponent0(final CommandSender sender, final BaseComponent... comps) {
		final StringBuilder plainMessage = new StringBuilder();

		for (final BaseComponent comp : comps)
			plainMessage.append(comp.toLegacyText().replaceAll(ChatColor.COLOR_CHAR + "x", ""));

		if (!(sender instanceof Player)) {
			tell0(sender, plainMessage.toString());

			return;
		}

		try {
			if (MinecraftVersion.equals(V.v1_7)) {
				final Class<?> chatBaseComponentClass = getNMSClass("IChatBaseComponent", "N/A");
				final Class<?> packetClass = getNMSClass("PacketPlayOutChat", "N/A");

				final Object chatBaseComponent = Remain.toIChatBaseComponent(comps);
				final Object packet = ReflectionUtil.instantiate(ReflectionUtil.getConstructor(packetClass, chatBaseComponentClass), chatBaseComponent);

				Remain.sendPacket((Player) sender, packet);

			} else
				((Player) sender).spigot().sendMessage(comps);

		} catch (final Throwable ex) {

			// This is the minimum MC version that supports interactive chat
			// Ignoring Cauldron
			if (MinecraftVersion.atLeast(V.v1_7) && !Bukkit.getName().contains("Cauldron"))
				Common.throwError(ex, "Failed to send component: " + plainMessage.toString() + " to " + sender.getName());

			tell0(sender, plainMessage.toString());
		}
	}

	private static void tell0(final CommandSender sender, final String msg) {
		Valid.checkNotNull(sender, "Sender cannot be null");

		if (msg.isEmpty() || "none".equals(msg))
			return;

		final String stripped = msg.startsWith("[JSON]") ? msg.replaceFirst("\\[JSON\\]", "").trim() : msg;

		for (final String part : stripped.split("\n"))
			sender.sendMessage(part);
	}
}

/**
 * A wrapper for advancements
 */
class AdvancementAccessor {

	private final NamespacedKey key;
	private final String icon;
	private final String message;

	AdvancementAccessor(final String message, final String icon) {
		this.key = new NamespacedKey(SimplePlugin.getInstance(), UUID.randomUUID().toString());
		this.message = message;
		this.icon = icon;
	}

	public void show(final Player player) {
		loadAdvancement();
		grantAdvancement(player);

		Common.runLater(10, () -> {
			revokeAdvancement(player);
			removeAdvancement();
		});
	}

	private void loadAdvancement() {
		Bukkit.getUnsafe().loadAdvancement(key, compileJson0());
	}

	private String compileJson0() {
		final JsonObject json = new JsonObject();

		final JsonObject icon = new JsonObject();
		icon.addProperty("item", this.icon);

		final JsonObject display = new JsonObject();
		display.add("icon", icon);
		display.addProperty("title", message);
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

	private void grantAdvancement(final Player plazer) {
		final Advancement adv = getAdvancement();
		final AdvancementProgress progress = plazer.getAdvancementProgress(adv);

		if (!progress.isDone())
			progress.getRemainingCriteria().forEach(crit -> progress.awardCriteria(crit));
	}

	private void revokeAdvancement(final Player plazer) {
		final Advancement adv = getAdvancement();
		final AdvancementProgress prog = plazer.getAdvancementProgress(adv);

		if (prog.isDone())
			prog.getAwardedCriteria().forEach(crit -> prog.revokeCriteria(crit));
	}

	private void removeAdvancement() {
		Bukkit.getUnsafe().removeAdvancement(key);
	}

	private Advancement getAdvancement() {
		return Bukkit.getAdvancement(key);
	}
}

class PotionSetter {

	/**
	 * Attempts to insert a certain potion to the given item
	 *
	 * @param item
	 * @param type
	 * @param durationTicks
	 * @param level
	 */
	public static void setPotion(final ItemStack item, final PotionEffectType type, final int durationTicks, final int level) {
		Valid.checkBoolean(item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta, "Can only use setPotion for items with PotionMeta not: " + item.getItemMeta());

		final org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
		final PotionType wrapped = PotionType.getByEffect(type);

		try {
			if (level > 0 && wrapped == null) {
				final org.bukkit.potion.PotionData data = new org.bukkit.potion.PotionData(level > 0 && wrapped != null ? wrapped : PotionType.WATER);

				meta.setBasePotionData(data);
				meta.addEnchant(Enchantment.DURABILITY, 1, true);
			}

		} catch (final NoSuchMethodError | NoClassDefFoundError ex) {
		}

		// For some reason this does not get added so we have to add it manually on top of the lore
		if (MinecraftVersion.olderThan(V.v1_9)) {
			final List<String> lore = new ArrayList<>();

			lore.add(Common.colorize("&7" + ItemUtil.bountifyCapitalized(type) + " (" + TimeUtil.formatTimeColon(durationTicks / 20) + ")"));

			if (meta.getLore() != null)
				lore.addAll(meta.getLore());

			meta.setLore(lore);
		}

		meta.setMainEffect(type);
		meta.addCustomEffect(new PotionEffect(type, durationTicks, level - 1), true);

		item.setItemMeta(meta);
	}
}