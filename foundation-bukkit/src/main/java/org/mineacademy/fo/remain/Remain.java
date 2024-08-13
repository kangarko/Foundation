package org.mineacademy.fo.remain;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.conversations.ConversationContext;
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
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.SimpleComponentCore;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.model.SimpleTask;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.UUIDToNameConverter;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.internal.BossBarInternals;
import org.mineacademy.fo.remain.nbt.NBTEntity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Remain extends RemainCore {

	/**
	 * Stores player cooldowns for old MC versions
	 */
	private final static StrictMap<UUID /*Player*/, StrictMap<Material, Integer>> cooldowns = new StrictMap<>();

	/**
	 * Must manually unfreeze in your plugin, resolves https://github.com/kangarko/ChatControl-Red/issues/2662
	 */
	@Getter
	private static boolean enchantRegistryUnfrozen = false;

	// ----------------------------------------------------------------------------------------------------
	// Fields
	// ----------------------------------------------------------------------------------------------------

	/**
	 * The get players method stored here for performance
	 */
	private static Method getPlayersMethod;

	/**
	 * Does the current server version get player list as a collection?
	 */
	private static boolean isGetPlayersCollection = false;

	/**
	 * The get player health method stored here for performance
	 */
	private static Method getHealthMethod;

	/**
	 * Does the current server version get player health as a double?
	 */
	private static boolean isGetHealthDouble = false;

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

	/**
	 * The getNmsItemStackTag method
	 */
	private static Method getNmsItemStackTag;

	/**
	 * Fields related to sending interactive chat components on legacy MC
	 */
	private static Constructor<?> chatMessageConstructor;

	/**
	 * Fields related to Folia
	 */
	private static Object foliaScheduler;
	private static Method runAtFixedRate;
	private static Method runDelayed;
	private static Method execute;
	private static Method cancel;
	private static Method cancelTasks;

	// ----------------------------------------------------------------------------------------------------
	// Flags
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return true if we are on paper
	 */
	private static boolean isPaper = false;

	/**
	 * Return true if this is a Folia server
	 */
	private static boolean isFolia = false;

	/**
	 * Return true if this server is Thermos
	 */
	private static boolean isThermos = false;

	/**
	 * Return true if we are on mojang remapped server
	 */
	private static boolean isUsingMojangMappings = false;

	/**
	 * Does the current server version support book event?
	 */
	private static boolean hasBookEvent = true;

	/**
	 * Return true if we have the overcomplicated io.papermc.paper.event.player.AsyncChatEvent
	 */
	private static boolean hasAdventureChatEvent = true;

	/**
	 * Does the current server version support spawn egg meta?
	 */
	private static boolean hasSpawnEggMeta = true;

	/**
	 * Does the current server version support advancements?
	 */
	private static boolean hasAdvancements = true;

	/**
	 * Does the current server version support particle API?
	 */
	private static boolean hasWorldSpawnParticle = true;

	/**
	 * Does the current server version support native scoreboard API?
	 */
	private static boolean hasObjectiveGetScore = true;

	/**
	 * Does the current server version support getting inventorsy location?
	 */
	private static boolean hasInventoryGetLocation = true;

	/**
	 * Does the current server version support permanent scoreboard tags?
	 */
	private static boolean hasEntityGetScoreboardTags = true;

	/**
	 * Can you call {@link YamlConfiguration#load(java.io.Reader)}
	 */
	private static boolean hasYamlConfigurationLoadReader = true;

	/**
	 * Is org/bukkit/inventory/meta/ItemMeta class present? MC 1.4.7+
	 */
	private static boolean hasItemMeta = true;

	/**
	 * Return if the {@link Entity#addPassenger(Entity)} method is available.
	 */
	private static boolean hasEntityAddPassenger = true;

	/**
	 * Return true if PlayerInventory class has the getExtraContents method
	 */
	private static boolean hasPlayerExtraInventoryContent = true;

	/**
	 * Return true if Player has openSign method.
	 */
	private static boolean hasPlayerOpenSignMethod = true;

	static {
		CompParticle.CRIT.getClass();

		final boolean atLeast1_4 = MinecraftVersion.atLeast(V.v1_4);

		isPaper = ReflectionUtil.isClassAvailable("co.aikar.timings.Timing");
		isFolia = ReflectionUtil.isClassAvailable("io.papermc.paper.threadedregions.RegionizedServer");
		isThermos = ReflectionUtil.isClassAvailable("thermos.ThermosRemapper");
		isUsingMojangMappings = ReflectionUtil.isClassAvailable("net.minecraft.server.level.ServerPlayer");

		hasBookEvent = ReflectionUtil.isClassAvailable("org.bukkit.event.player.PlayerEditBookEvent");
		hasAdventureChatEvent = ReflectionUtil.isClassAvailable("io.papermc.paper.event.player.AsyncChatEvent");
		hasSpawnEggMeta = ReflectionUtil.isClassAvailable("org.bukkit.inventory.meta.SpawnEggMeta");
		hasAdvancements = ReflectionUtil.isClassAvailable("org.bukkit.advancement.Advancement") && ReflectionUtil.isClassAvailable("org.bukkit.NamespacedKey");

		try {
			getPlayersMethod = Bukkit.class.getMethod("getOnlinePlayers");
			isGetPlayersCollection = getPlayersMethod.getReturnType() == Collection.class;
		} catch (final Throwable t) {
			Common.error(t, "Failed to find Bukkit.getOnlinePlayers()");
		}

		try {
			getHealthMethod = LivingEntity.class.getMethod("getHealth");
			isGetHealthDouble = getHealthMethod.getReturnType() == double.class;
		} catch (final Throwable t) {
			Common.error(t, "Failed to find LivingEntity.getHealth()");
		}

		try {
			fieldPlayerConnection = ReflectionUtil.getNMSClass("EntityPlayer", "net.minecraft.server.level.EntityPlayer")
					.getField(MinecraftVersion.atLeast(V.v1_20) ? "c" : MinecraftVersion.atLeast(V.v1_17) ? "b" : atLeast1_4 ? "playerConnection" : "netServerHandler");
		} catch (final Throwable t) {
			Common.error(t, "Failed to find EntityPlayer.playerConnection");
		}

		if (MinecraftVersion.olderThan(V.v1_12))
			try {
				fieldEntityInvulnerable = ReflectionUtil.getNMSClass("Entity").getDeclaredField("invulnerable");
				fieldEntityInvulnerable.setAccessible(true);
			} catch (final Throwable t) {
				// Unavailable
			}

		try {
			sendPacket = ReflectionUtil.getNMSClass(atLeast1_4 ? "PlayerConnection" : "NetServerHandler", "net.minecraft.server.network.PlayerConnection")
					.getMethod(MinecraftVersion.atLeast(V.v1_18) ? "a" : "sendPacket", ReflectionUtil.getNMSClass("Packet", "net.minecraft.network.protocol.Packet"));
		} catch (final Throwable t) {
			Common.error(t, "Failed to find PlayerConnection.sendPacket()");
		}

		if (MinecraftVersion.olderThan(V.v1_16)) {
			final Class<?> nmsItemStack = ReflectionUtil.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");

			getNmsItemStackTag = ReflectionUtil.getMethod(nmsItemStack, "getTag");
		}

		try {
			World.class.getMethod("spawnParticle", org.bukkit.Particle.class, Location.class, int.class);
		} catch (final Throwable ex) {
			hasWorldSpawnParticle = false;
		}

		try {
			Objective.class.getMethod("getScore", String.class);
		} catch (final Throwable e) {
			hasObjectiveGetScore = false;
		}

		try {
			Inventory.class.getMethod("getLocation");
		} catch (final Throwable ex) {
			hasInventoryGetLocation = false;
		}

		try {
			Entity.class.getMethod("getScoreboardTags");
		} catch (final Throwable ex) {
			hasEntityGetScoreboardTags = false;
		}

		try {
			YamlConfiguration.class.getMethod("load", java.io.Reader.class);
		} catch (final Throwable err) {
			hasYamlConfigurationLoadReader = false;
		}

		try {
			org.bukkit.inventory.ItemStack.class.getMethod("getItemMeta");
		} catch (final Throwable ex) {
			hasItemMeta = false;
		}

		try {
			Entity.class.getMethod("addPassenger", Entity.class);
		} catch (final Throwable ex) {
			hasEntityAddPassenger = false;
		}

		try {
			PlayerInventory.class.getMethod("getExtraContents");
		} catch (final Throwable ex) {
			hasPlayerExtraInventoryContent = false;
		}

		try {
			Player.class.getMethod("openSign", org.bukkit.block.Sign.class);
		} catch (final Throwable ex) {
			hasPlayerOpenSignMethod = false;
		}

		if (MinecraftVersion.newerThan(V.v1_6) && MinecraftVersion.olderThan(V.v1_12))
			try {
				final Class<?> chatBaseComponent = ReflectionUtil.getNMSClass("IChatBaseComponent", "N/A");
				final Class<?> chatPacket = ReflectionUtil.getNMSClass("PacketPlayOutChat", "N/A");

				if (MinecraftVersion.newerThan(V.v1_11))
					chatMessageConstructor = chatPacket.getConstructor(chatBaseComponent, ReflectionUtil.getNMSClass("ChatMessageType", "N/A"));
				else
					chatMessageConstructor = MinecraftVersion.newerThan(V.v1_7) ? chatPacket.getConstructor(chatBaseComponent, byte.class) : chatPacket.getConstructor(chatBaseComponent);

			} catch (final ReflectiveOperationException ex) {
				if (!isThermos)
					Common.error(ex, "Unable to setup chat internals");
			}

		if (isFolia) {
			foliaScheduler = ReflectionUtil.invoke("getGlobalRegionScheduler", org.bukkit.Bukkit.getServer());
			runAtFixedRate = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
			execute = ReflectionUtil.getMethod(foliaScheduler.getClass(), "run", Plugin.class, Consumer.class);
			runDelayed = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class);
			cancelTasks = ReflectionUtil.getMethod(foliaScheduler.getClass(), "cancelTasks", Plugin.class);
			cancel = ReflectionUtil.getMethod(ReflectionUtil.lookupClass("io.papermc.paper.threadedregions.scheduler.ScheduledTask"), "cancel");
		}

		Remain.init();
	}

	// ----------------------------------------------------------------------------------------------------
	// Cross-version compatibiltiy methods
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Returns all online players
	 *
	 * @return the online players
	 */
	public static Collection<? extends Player> getOnlinePlayers() {
		return isGetPlayersCollection ? Bukkit.getOnlinePlayers() : Arrays.asList(ReflectionUtil.invokeStatic(getPlayersMethod));
	}

	/**
	 * Returns the health of an entity
	 *
	 * @param entity the entity
	 * @return the health
	 */
	public static int getHealth(final LivingEntity entity) {
		return isGetHealthDouble ? (int) entity.getHealth() : ReflectionUtil.invoke(getHealthMethod, entity);
	}

	/**
	 * Return the max health of an entity
	 *
	 * @param entity
	 * @return
	 */
	public static int getMaxHealth(final LivingEntity entity) {
		if (isGetHealthDouble)
			return (int) entity.getMaxHealth();

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

	/**
	 * Advanced: Sends a packet to the player
	 *
	 * @param player the player
	 * @param packet the packet
	 */
	public static void sendPacket(final Player player, final Object packet) {
		if (sendPacket == null || fieldPlayerConnection == null) {
			Common.log("Cannot send packet " + packet.getClass() + " (known to be broken on Cauldron).");

			return;
		}

		final Object handle = getHandleEntity(player);
		final Object playerConnection = ReflectionUtil.getFieldContent(fieldPlayerConnection, handle);

		ReflectionUtil.invoke(sendPacket, playerConnection, packet);
	}

	// ----------------------------------------------------------------------------------------------------
	// Invulnerability
	// ----------------------------------------------------------------------------------------------------

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

	// ----------------------------------------------------------------------------------------------------
	// Cooldown
	// ----------------------------------------------------------------------------------------------------

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

	// ----------------------------------------------------------------------------------------------------
	// Enchantment
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Freeze back enchant registry
	 *
	 * @deprecated called internally already in {@link SimplePlugin}
	 */
	@Deprecated
	public static void freezeEnchantRegistry() {
		if (MinecraftVersion.atLeast(V.v1_19)) {
			final Object enchantmentRegistry = getEnchantRegistry();
			final Method freezeMethod = ReflectionUtil.getDeclaredMethod(enchantmentRegistry.getClass(), Remain.isUsingMojangMappings() ? "freeze" : "l");

			ReflectionUtil.invoke(freezeMethod, enchantmentRegistry);
		}

		if (MinecraftVersion.olderThan(V.v1_20)) {
			clearLegacyEnchantMap();

			ReflectionUtil.invokeStatic(Enchantment.class, "stopAcceptingRegistrations");
		}
	}

	private static void clearLegacyEnchantMap() {
		try {
			final Class<?> enchantCommandClass = ReflectionUtil.lookupClass("org.bukkit.command.defaults.EnchantCommand");

			if (enchantCommandClass != null) {
				final List<String> enchants = ReflectionUtil.getStaticFieldContent(enchantCommandClass, "ENCHANTMENT_NAMES");

				enchants.clear();
			}
		} catch (final Throwable t) {
			// prob unsupported at server level anymore
		}
	}

	/*
	 * Helper to get the registry object
	 */
	private static Object getEnchantRegistry() {
		final Class<?> registryClass = ReflectionUtil.lookupClass("net.minecraft.core.registries.BuiltInRegistries");
		final Object enchantmentRegistry = ReflectionUtil.getStaticFieldContent(registryClass, Remain.isUsingMojangMappings() ? "ENCHANTMENT" : MinecraftVersion.equals(V.v1_19) ? "g" : "f");

		return enchantmentRegistry;
	}

	/**
	 * Unfreeze enchant registry
	 *
	 * @deprecated called internally already in {@link SimplePlugin}
	 */
	@Deprecated
	public static void unfreezeEnchantRegistry() {
		if (MinecraftVersion.atLeast(V.v1_19)) {
			final boolean mojMap = Remain.isUsingMojangMappings();
			final Object enchantmentRegistry = getEnchantRegistry();

			try {
				// works fine in versions (1.19.3 and up)
				ReflectionUtil.setDeclaredField(enchantmentRegistry, mojMap ? "frozen" : "l", false); // MappedRegistry#frozen
				ReflectionUtil.setDeclaredField(enchantmentRegistry, mojMap ? "unregisteredIntrusiveHolders" : "m", new IdentityHashMap<>()); // MappedRegistry#unregisteredIntrusiveHolders

			} catch (final Throwable t) {
				try {
					// in (1.19 - 1.19.2) the obfuscation is different.
					ReflectionUtil.setDeclaredField(enchantmentRegistry, mojMap ? "frozen" : "ca", false); // MappedRegistry#frozen
					// unregisteredIntrusiveHolders does not exist in this version

				} catch (final Throwable tt) {
					// Unable to unfreeze (i.e. 1.20.2, we only support the latest subversion)
				}
			}

		}

		if (MinecraftVersion.olderThan(V.v1_20)) {
			ReflectionUtil.setStaticField(Enchantment.class, "acceptingNew", true);

			clearLegacyEnchantMap();
		}

		enchantRegistryUnfrozen = true;
	}

	// ----------------------------------------------------------------------------------------------------
	// Conversation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Get the session data map from the conversation context.
	 *
	 * @param context
	 * @return
	 */
	public static Map<Object, Object> getAllSessionData(ConversationContext context) {
		try {
			return context.getAllSessionData();

		} catch (final NoSuchMethodError err) {
			return ReflectionUtil.getFieldContent(context, "sessionData");
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Misc
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return NMS copy of the given itemstack
	 *
	 * @param itemStack
	 * @return
	 */
	public static Object asNMSCopy(ItemStack itemStack) {
		try {
			final Method asNmsCopy = ReflectionUtil.getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			return asNmsCopy.invoke(null, itemStack);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to convert item to NMS item: " + itemStack);

			return null;
		}
	}

	/**
	 * Serializes the component into plain text
	 *
	 * @param component
	 * @return
	 */
	public static Object convertAdventureToIChatBase(Component component) {
		return convertJsonToIChatBase(convertAdventureToJson(component));
	}

	/**
	 * Return IChatBaseComponent from the given JSON
	 *
	 * @param json
	 * @return
	 */
	public static Object convertJsonToIChatBase(String json) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "Serializing chat components requires Minecraft 1.7.10 and greater");

		final Class<?> chatSerializer = ReflectionUtil.getNMSClass((MinecraftVersion.equals(V.v1_7) ? "" : "IChatBaseComponent$") + "ChatSerializer", "net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");
		final Method a = ReflectionUtil.getMethod(chatSerializer, "a", String.class);

		return ReflectionUtil.invoke(a, null, json);
	}

	/**
	 * Return IChatBaseComponent from legacy text
	 *
	 * @param legacyText
	 * @return
	 */
	public static Object convertLegacyToIChatBase(String legacyText) {
		final String json = convertLegacyToJson(legacyText);

		return convertJsonToIChatBase(json);
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
	 * Return the top inventory of the player's open inventory
	 *
	 * @param player
	 * @return
	 */
	public static Inventory getBottomInventoryFromOpenInventory(Player player) {
		return invokeOpenInventoryMethod(player, "getBottomInventory");
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

		if (slot < 0)
			return null;

		final Inventory topInventory = invokeInventoryViewMethod(event, "getTopInventory");
		final Inventory bottomInventory = invokeInventoryViewMethod(event, "getBottomInventory");

		return topInventory != null && slot < topInventory.getSize() ? topInventory : bottomInventory;
	}

	/**
	 * Return the server's command map
	 *
	 * @return
	 */
	public static SimpleCommandMap getCommandMap() {
		final Class<?> craftServer = ReflectionUtil.getOBCClass("CraftServer");

		try {
			return (SimpleCommandMap) craftServer.getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer());

		} catch (final ReflectiveOperationException ex) {

			try {
				return ReflectionUtil.getFieldContent(Bukkit.getServer(), "commandMap");

			} catch (final Throwable ex2) {
				throw new FoException(ex2, "Unable to get the command map");
			}
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

	/*
	 * Get the handle of the given instance
	 */
	private static Object getHandle(Object instance, String methodName) {
		final Method handle = ReflectionUtil.getMethod(instance.getClass(), methodName);
		Valid.checkNotNull(handle, "Cannot call " + methodName + "() for " + instance.getClass() + " (" + instance + ")");

		return ReflectionUtil.invoke(handle, instance);
	}

	/**
	 * Returns Minecraft Entity class
	 *
	 * @param entity
	 * @return
	 */
	public static Object getHandleEntity(final Object entity) {
		return getHandle(entity, entity instanceof BlockState ? "getTileEntity" : "getHandle");
	}

	/**
	 * Get the server handle
	 *
	 * @return
	 */
	public static Object getHandleServer() {
		return getHandle(Bukkit.getServer(), "getServer");
	}

	/**
	 * Returns Minecraft World class
	 *
	 * @param world
	 * @return
	 */
	public static Object getHandleWorld(final World world) {
		return getHandle(world, "getHandle");
	}

	/**
	 * Attempts to resolve the hit block from projectile hit event
	 *
	 * @param event
	 * @return
	 */
	public static Block getHitBlock(ProjectileHitEvent event) {
		try {
			return event.getHitBlock();

		} catch (final Throwable t) {

			final Block entityBlock = event.getEntity().getLocation().getBlock();

			if (!CompMaterial.isAir(entityBlock))
				return entityBlock;

			for (final BlockFace face : Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH)) {
				final Block adjucentBlock = entityBlock.getRelative(face);

				if (!CompMaterial.isAir(adjucentBlock))
					return adjucentBlock;
			}
		}

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
			return ItemUtil.bountify(item.getType());
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
	 * Returns the inventory location
	 *
	 * @param inv the inventory
	 * @return the location
	 */
	public static Location getLocation(final Inventory inv) {
		if (hasInventoryGetLocation)
			try {
				return inv.getLocation();

			} catch (final NullPointerException ex) { // EnderChest throws this
				return null;
			}

		return inv.getHolder() instanceof BlockState ? ((BlockState) inv.getHolder()).getLocation() : !inv.getViewers().isEmpty() ? inv.getViewers().iterator().next().getLocation() : null;
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
	 * Return the name of the entity
	 *
	 * @param entity
	 * @return
	 */
	public static String getName(final Entity entity) {
		try {
			return entity.getName();

		} catch (final NoSuchMethodError t) {
			return entity instanceof Player ? ((Player) entity).getName() : ItemUtil.bountify(entity.getType());
		}
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
	 * Return the NMS statistic name for the given statistic
	 *
	 * @param stat
	 * @param mat
	 * @param en
	 * @return
	 */
	public static String getNMSStatisticName(final Statistic stat, final Material mat, final EntityType en) {
		final Class<?> craftStatistic = ReflectionUtil.getOBCClass("CraftStatistic");
		Object nmsStatistic = null;

		try {
			if (stat.getType() == Statistic.Type.UNTYPED)
				nmsStatistic = craftStatistic.getMethod("getNMSStatistic", stat.getClass()).invoke(null, stat);

			else if (stat.getType() == Statistic.Type.ENTITY)
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
	 * Return the player ping
	 *
	 * @param player
	 * @return
	 */
	public static int getPing(Player player) {
		try {
			return player.getPing();

		} catch (final NoSuchMethodError err) {
			final Object entityPlayer = Remain.getHandleEntity(player);

			return (int) ReflectionUtil.getFieldContent(entityPlayer, "ping");
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
	 * Return how long the player has played on this server (pulled from your world statistics file)
	 * in minutes
	 *
	 * @param player
	 * @return
	 */
	public static long getPlaytimeMinutes(final OfflinePlayer player) {
		return getPlaytimeSeconds(player) / 60;
	}

	/**
	 * Return how long the player has played on this server (pulled from your world statistics file)
	 * in seconds.
	 *
	 * @param player
	 * @return
	 */
	public static long getPlaytimeSeconds(final OfflinePlayer player) {
		final long value = PlayerUtil.getStatistic(player, getPlayTimeStatisticName());

		return value / 20;
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
	 * Returns a scoreboard score
	 *
	 * @param obj
	 * @param entry
	 * @return
	 */
	public static Score getScore(final Objective obj, String entry) {
		Valid.checkNotNull(obj, "Objective cannot be null");

		entry = Common.colorizeLegacy(entry);

		try {
			return obj.getScore(entry);

		} catch (final NoSuchMethodError err) {
			return obj.getScore(Bukkit.getOfflinePlayer(entry));
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
	 * Return the top inventory of the player's open inventory
	 *
	 * @param player
	 * @return
	 */
	public static Inventory getTopInventoryFromOpenInventory(Player player) {
		return invokeOpenInventoryMethod(player, "getTopInventory");
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
	 * Returns the player's view distance
	 *
	 * @param player
	 * @return
	 */
	public static int getViewDistance(Player player) {
		try {
			return player.getClientViewDistance();

		} catch (final NoSuchMethodError err) {
			final Method getViewDistance = ReflectionUtil.getMethod(player.spigot().getClass(), "getViewDistance");

			return ReflectionUtil.invoke(getViewDistance, player.spigot());
		}
	}

	/**
	 *
	 * @param <T>
	 * @param event
	 * @param methodName
	 * @return
	 */
	public static <T> T invokeInventoryViewMethod(InventoryEvent event, String methodName) {
		final Object view = ReflectionUtil.invoke("getView", event);

		return ReflectionUtil.invoke(methodName, view);
	}

	/**
	 *
	 * @param <T>
	 * @param player
	 * @param methodName
	 * @return
	 */
	public static <T> T invokeOpenInventoryMethod(Player player, String methodName) {
		final Object view = ReflectionUtil.invoke("getOpenInventory", player);

		return ReflectionUtil.invoke(methodName, view);
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
	 * Calls NMS to find out if the entity is invisible, works for any entity,
	 * better than Bukkit since it has extreme downwards compatibility and does not require LivingEntity
	 *
	 * USE WITH CAUTION, returns true for spectator mode and vanish potions
	 *
	 * @param entity
	 * @return
	 */
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
	 * Return if the play time statistic is measured in ticks
	 *
	 * @return
	 */
	public static boolean isPlaytimeStatisticTicks() {
		return MinecraftVersion.olderThan(V.v1_13);
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
	 * A shortcut method to generate a new {@link NamespacedKey}. Requires MC 1.13+
	 *
	 * The name is randomly assigned in the format YOURPLUGIN_RANDOM where YOURPLUGIN
	 * is your plugin's name and RANDOM are 16 random letters.
	 *
	 * @return
	 */
	public static NamespacedKey newNamespaced() {
		return new NamespacedKey(SimplePlugin.getInstance(), SimplePlugin.getNamed() + "_" + RandomUtil.nextString(16));
	}

	/**
	 * A shortcut method to generate a new {@link NamespacedKey}. Requires MC 1.13+
	 *
	 * @param name
	 * @return
	 */
	public static NamespacedKey newNamespaced(String name) {
		return new NamespacedKey(SimplePlugin.getInstance(), name);
	}

	/**
	 * Opens the book for the player given the book is a WRITTEN_BOOK
	 *
	 * @param player
	 * @param book
	 */
	public static void openBook(Player player, ItemStack book) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_8), "Opening books is only supported on MC 1.8 and greater");
		Valid.checkBoolean(book.getItemMeta() instanceof org.bukkit.inventory.meta.BookMeta, "openBook method called for not a book item: " + book);
		Valid.checkBoolean(CompMaterial.fromMaterial(book.getType()) == CompMaterial.WRITTEN_BOOK, "Can only call openBook for WRITTEN_BOOK! Got: " + book);

		// Fix "Invalid book tag" error when author/title is empty
		final org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();

		if (meta.getAuthor() == null)
			meta.setAuthor("");

		if (meta.getTitle() == null)
			meta.setTitle("");

		if (meta.getPageCount() == 0)
			meta.setPages(""); // Empty book

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
	 * Opens the sign for the player. On legacy versions, ProtocolLib is
	 * required to save the edits to the sign after updating it.
	 *
	 * @param player
	 * @param signBlock
	 */
	public static void openSign(Player player, Block signBlock) {
		final BlockState state = signBlock.getState();
		Valid.checkBoolean(state instanceof Sign, "Block is not a sign: " + signBlock);

		final Sign sign = (Sign) state;

		if (hasPlayerOpenSignMethod) {
			player.openSign(sign);

		} else {
			final Class<?> chatComponentClass = ReflectionUtil.getNMSClass("IChatBaseComponent");
			final Class<?> blockPositionClass = ReflectionUtil.getNMSClass("BlockPosition");

			final Object blockPosition = ReflectionUtil.instantiate(ReflectionUtil.getConstructor(blockPositionClass, int.class, int.class, int.class), signBlock.getX(), signBlock.getY(), signBlock.getZ());
			final Object[] chatComponent = (Object[]) java.lang.reflect.Array.newInstance(chatComponentClass, 4);

			for (int i = 0; i < 4; i++)
				chatComponent[i] = convertLegacyToIChatBase(sign.getLine(i));

			final Object nmsSign = Remain.getHandleEntity(sign);
			final Object nmsPlayer = Remain.getHandleEntity(player);

			// Set the sign to be editable and assign the editing player to it
			ReflectionUtil.setDeclaredField(nmsSign, "isEditable", true);
			ReflectionUtil.setDeclaredField(nmsSign, "h", nmsPlayer);

			CompMetadata.setTempMetadata(player, FoConstants.NBT.METADATA_OPENED_SIGN, sign.getLocation());
			Remain.sendPacket(player, ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("PacketPlayOutOpenSignEditor", blockPositionClass), blockPosition));
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
	 * Sends to the player the block update packet of the given block, typically
	 * to reset it back to the real state
	 *
	 * @param delayTicks
	 * @param player
	 * @param block
	 */
	public static void sendBlockChange(final int delayTicks, final Player player, final Block block) {
		Common.runLater(delayTicks, () -> sendBlockChange0(player, block));
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
		Common.runLater(delayTicks, () -> sendBlockChange0(player, location, material));
	}

	private static void sendBlockChange0(final Player player, final Block block) {
		try {
			player.sendBlockChange(block.getLocation(), block.getBlockData());
		} catch (final NoSuchMethodError ex) {
			player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
		}
	}

	private static void sendBlockChange0(final Player player, final Location location, final CompMaterial material) {
		try {
			player.sendBlockChange(location, material.getMaterial().createBlockData());
		} catch (final NoSuchMethodError ex) {
			player.sendBlockChange(location, material.getMaterial(), material.getData());
		}
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

			final Class<?> blockClass = ReflectionUtil.getNMSClass("Block");
			final Class<?> blocks = ReflectionUtil.getNMSClass("Blocks");

			final Object position = ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("BlockPosition", double.class, double.class, double.class), location.getX(), location.getY(), location.getZ());
			final Object packet = ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("PacketPlayOutBlockAction",
					ReflectionUtil.getNMSClass("BlockPosition"), blockClass, int.class, int.class), position, ReflectionUtil.getStaticFieldContent(blocks, "CHEST"), 1, action);

			for (final Player player : getOnlinePlayers())
				sendPacket(player, packet);
		}
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
		sendToast(receivers, message, icon, CompToastStyle.GOAL);
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
	 * @param style
	 */
	public static void sendToast(final List<Player> receivers, final Function<Player, String> message, final CompMaterial icon, final CompToastStyle style) {

		if (hasAdvancements)
			Common.runAsync(() -> {
				for (final Player receiver : receivers) {

					// Sleep to mitigate sending not working at once
					Common.sleep(100);

					Common.runLater(() -> {
						final String colorized = Common.colorizeLegacy(message.apply(receiver));

						if (!colorized.isEmpty()) {
							final AdvancementAccessor accessor = new AdvancementAccessor(colorized, icon.toString().toLowerCase(), style);

							if (receiver.isOnline())
								accessor.show(receiver);
						}
					});
				}
			});
		else
			for (final Player receiver : receivers) {
				final String colorized = Common.colorizeLegacy(message.apply(receiver));

				if (!colorized.isEmpty())
					receiver.sendMessage(colorized);
			}

	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot be
	 * modified on its first screen. It imposes a slight performance penalty.
	 *
	 * @param audience
	 * @param message
	 */
	public static void sendToast(Audience audience, Component message) {
		if (audience instanceof Player) {
			final String legacy = Remain.convertAdventureToLegacy(message);

			Remain.sendToast((Player) audience, legacy);

		} else
			Remain.tell(audience, message);
	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot be
	 * modified on its first screen. It imposes a slight performance penalty.
	 *
	 * @param receiver
	 * @param message
	 */
	public static void sendToast(Player receiver, String message) {
		sendToast(receiver, message, CompMaterial.BOOK, CompToastStyle.TASK);
	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot be
	 * modified on its first screen. It imposes a slight performance penalty.
	 *
	 * You can change the icon appearing on the first screen here.
	 *
	 * @param receiver
	 * @param message
	 * @param icon
	 */
	public static void sendToast(final Player receiver, final String message, final CompMaterial icon) {
		sendToast(receiver, message, icon, CompToastStyle.TASK);
	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot
	 * be modified that much. It imposes a slight performance penalty.
	 *
	 * You can change the icon appearing on the first screen here.
	 * You can also pick the first screen from precreated Minecraft screens here.
	 *
	 * @param receiver
	 * @param message
	 * @param icon
	 * @param toastStyle
	 */
	public static void sendToast(final Player receiver, final String message, final CompMaterial icon, final CompToastStyle toastStyle) {
		if (message != null && !message.isEmpty()) {
			final String colorized = Common.colorizeLegacy(message);

			if (!colorized.isEmpty()) {
				Valid.checkSync("Toasts may only be sent from the main thread");

				if (hasAdvancements)
					new AdvancementAccessor(colorized, icon.toString().toLowerCase(), toastStyle).show(receiver);

				else
					receiver.sendMessage(colorized);
			}
		}
	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot be
	 * modified on its first screen. It imposes a slight performance penalty.
	 *
	 * You can pick the first screen from precreated Minecraft screens here.
	 *
	 * @param receiver
	 * @param message
	 * @param toastStyle
	 */
	public static void sendToast(Player receiver, String message, CompToastStyle toastStyle) {
		sendToast(receiver, message, CompMaterial.BOOK, toastStyle);
	}

	/**
	 * Attempts to remove a boss bar from player.
	 * <p>
	 * Only works if you rendered it through methods in this class!
	 *
	 * @param player
	 */
	public static void removeBossbar(final Player player) {
		BossBarInternals.getInstance().removeBar(player);
	}

	public static void resetTitle(final CommandSender sender) {
		resetTitle(Platform.toAudience(sender));
	}

	public static void sendActionBar(final CommandSender player, final String text) {
		sendActionBar(Platform.toAudience(player), CommonCore.colorize(text));
	}

	/**
	 * Displays message above player's health and hunger bar. (1.8+) Text will be
	 * colorized.
	 *
	 * @param audience the player
	 * @param message   the text
	 */
	public static void sendActionBar(final Audience audience, Component message) {
		if (MinecraftVersion.olderThan(V.v1_7)) {
			audience.sendMessage(message);

			return;
		}

		if (MinecraftVersion.equals(V.v1_7) && chatMessageConstructor != null && audience instanceof Player) {
			try {
				final Object iChatBaseComponent = Remain.convertAdventureToIChatBase(message);
				final Object packet;
				final byte type = 2;

				if (MinecraftVersion.atLeast(V.v1_12)) {
					final Class<?> chatMessageTypeEnum = ReflectionUtil.getNMSClass("ChatMessageType", "net.minecraft.network.chat.ChatMessageType");

					packet = chatMessageConstructor.newInstance(iChatBaseComponent, chatMessageTypeEnum.getMethod("a", byte.class).invoke(null, type));

				} else
					packet = chatMessageConstructor.newInstance(iChatBaseComponent, type);

				Remain.sendPacket((Player) audience, packet);

			} catch (final ReflectiveOperationException ex) {
				CommonCore.error(ex, "Failed to send action bar to " + ((Player) audience).getName() + ", message: " + message);
			}

		} else
			audience.sendActionBar(message);
	}

	/**
	 * Send the sender a component, ignoring it if it is empty
	 *
	 * @param sender
	 * @param component
	 */
	public static void tell(Object sender, Component component) {
		tell(sender, component, true);
	}

	/**
	 * Send the sender a component, ignoring it if it is empty
	 *
	 * @param sender
	 * @param component
	 * @param skipEmpty
	 */
	public static void tell(@NonNull Object sender, Component component, boolean skipEmpty) {
		ValidCore.checkBoolean(sender instanceof Audience || sender instanceof CommandSender, "Can only send components to Audience or CommandSender, not: " + sender);

		final boolean is1_7 = MinecraftVersion.equals(V.v1_7);

		if (RemainCore.convertAdventureToPlain(component).trim().isEmpty() && skipEmpty)
			return;

		if (MinecraftVersion.olderThan(V.v1_7) || (is1_7 && !(sender instanceof Player))) {
			if (sender instanceof CommandSender)
				CommonCore.tell(Platform.toAudience(sender), component);

			return;
		}

		if (is1_7) {
			final Player player = (Player) sender;
			final JsonObject json = GsonComponentSerializer.gson().serializeToTree(component).getAsJsonObject();
			convertHoverEvent(json);

			if (json.toString().contains("{RIADOK}")) {
				int count = 0;
				final Map<Integer, JsonArray> split = new HashMap<>();

				for (final JsonElement extraElement : json.get("extra").getAsJsonArray()) {
					final JsonObject extra = extraElement.getAsJsonObject();
					final String text = extra.get("text").getAsString();

					if (text.equals("{RIADOK}"))
						count++;
					else {
						if (!split.containsKey(count))
							split.put(count, new JsonArray());

						split.get(count).add(extra);
					}
				}

				for (final JsonArray array : split.values())
					player.spigot().sendMessage(net.md_5.bungee.chat.ComponentSerializer.parse(array.toString()));

			} else
				player.spigot().sendMessage(net.md_5.bungee.chat.ComponentSerializer.parse(json.toString()));

		} else {
			if (sender instanceof Audience)
				((Audience) sender).sendMessage(component);

			else
				Platform.toAudience(sender).sendMessage(component);
		}
	}

	/*
	 * Helper method to rename the component's contents to value for legacy MC versions
	 */
	private static void convertHoverEvent(JsonObject map) {
		if (map.has("hoverEvent")) {
			final JsonObject hoverEvent = map.get("hoverEvent").getAsJsonObject();
			JsonObject value = null;

			if (hoverEvent.has("contents")) {
				value = hoverEvent.get("contents").getAsJsonObject();

				hoverEvent.remove("contents");
			}

			if (value != null)
				hoverEvent.add("value", value);
		}

		if (map.has("extra")) {
			final JsonArray extraArray = map.get("extra").getAsJsonArray();

			for (final JsonElement key : extraArray)
				convertHoverEvent(key.getAsJsonObject());
		}
	}

	/**
	 * This will attempt to place a bed block to the initial block and the other head block in the facing direction
	 *
	 * Use {@link PlayerUtil#getFacing(Player)} to get where a player is looking at
	 *
	 * @param initialBlock
	 * @param facing
	 */
	public static void setBed(Block initialBlock, BlockFace facing) {

		if (MinecraftVersion.atLeast(V.v1_13))
			for (final Bed.Part part : Bed.Part.values()) {
				initialBlock.setBlockData(Bukkit.createBlockData(CompMaterial.WHITE_BED.getMaterial(), data -> {
					((Bed) data).setPart(part);
					((Bed) data).setFacing(facing);
				}));

				initialBlock = initialBlock.getRelative(facing.getOppositeFace());
			}

		else {
			initialBlock = initialBlock.getRelative(facing);

			final Material bedMaterial = Material.valueOf("BED_BLOCK");
			final Block bedFootBlock = initialBlock.getRelative(facing.getOppositeFace());

			final BlockState bedFootState = bedFootBlock.getState();
			bedFootState.setType(bedMaterial);

			final org.bukkit.material.Bed bedFootData = new org.bukkit.material.Bed(bedMaterial);
			bedFootData.setHeadOfBed(false);
			bedFootData.setFacingDirection(facing);

			bedFootState.setData(bedFootData);
			bedFootState.update(true);

			final BlockState bedHeadState = initialBlock.getState();
			bedHeadState.setType(bedMaterial);

			final org.bukkit.material.Bed bedHeadData = new org.bukkit.material.Bed(bedMaterial);
			bedHeadData.setHeadOfBed(true);
			bedHeadData.setFacingDirection(facing);

			bedHeadState.setData(bedHeadData);
			bedHeadState.update(true);
		}
	}

	/**
	 * This will attempt to place a bed block to the initial block and the other head block in the facing direction
	 *
	 * Use {@link PlayerUtil#getFacing(Player)} to get where a player is looking at
	 *
	 * @param initialLocation
	 * @param facing
	 */
	public static void setBed(Location initialLocation, BlockFace facing) {
		setBed(initialLocation.getBlock(), facing);
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
	 * Sets a custom name to entity
	 *
	 * @param entity
	 * @param name
	 */
	public static void setCustomName(final Entity entity, final String name) {
		setCustomName(entity, name, true);
	}

	/**
	 * Sets a custom name to entity
	 *
	 * @param entity
	 * @param name
	 * @param visible
	 */
	public static void setCustomName(final Entity entity, final String name, final boolean visible) {
		try {
			entity.setCustomNameVisible(visible);

			if (name != null)
				entity.setCustomName(Common.colorizeLegacy(name));

		} catch (final NoSuchMethodError er) {
			Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "setCustomName requires Minecraft 1.7.10+");

			final NBTEntity nbt = new NBTEntity(entity);

			nbt.setInteger("CustomNameVisible", visible ? 1 : 0);

			if (name != null)
				nbt.setString("CustomName", Common.colorizeLegacy(name));
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
			final Object nmsEntity = entity.getClass().toString().contains("net.minecraft.server") ? entity : entity instanceof LivingEntity ? getHandleEntity(entity) : null;
			Valid.checkNotNull(nmsEntity, "setInvisible requires either a LivingEntity or a NMS Entity, got: " + entity.getClass());
			final Method setInvisible = ReflectionUtil.getMethod(nmsEntity.getClass(), "setInvisible", boolean.class);

			// https://www.spigotmc.org/threads/how-do-i-make-an-entity-go-invisible-without-using-potioneffects.321227/
			Common.runLater(2, () -> {
				try {
					ReflectionUtil.invoke(setInvisible, nmsEntity, invisible);

				} catch (final Throwable t) {

					// unsupported
					t.printStackTrace();
				}
			});
		}
	}

	/**
	 * Attempts to set the book pages from the given list.
	 *
	 * @param metaObject
	 * @param pages
	 */
	public static void setPages(Object metaObject, SimpleComponentCore... pages) {
		Valid.checkBoolean(hasItemMeta, "Cannot set pages without ItemMeta on this server version: " + MinecraftVersion.getFullVersion());
		Valid.checkBoolean(metaObject instanceof org.bukkit.inventory.meta.BookMeta, "Object must be BookMeta, got: " + metaObject.getClass());

		final org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) metaObject;

		if (MinecraftVersion.olderThan(V.v1_8)) {
			for (final SimpleComponentCore component : pages)
				meta.addPage(component.getLegacyText());

			return;
		}

		try {
			final BungeeComponentSerializer serializer = BungeeComponentSerializer.get();
			final List<BaseComponent[]> spigotPages = new ArrayList<>();

			for (final SimpleComponentCore component : pages)
				try {
					spigotPages.add(serializer.serialize(component.getComponent()));

				} catch (final Throwable t) {
					Common.error(t, "Failed to turn simple component into bungee component: " + component);
				}

			meta.spigot().setPages(spigotPages);

		} catch (final NoSuchMethodError ex) {
			try {
				final List<Object> chatComponentPages = (List<Object>) ReflectionUtil.getFieldContent(ReflectionUtil.getOBCClass("inventory.CraftMetaBook"), "pages", meta);

				for (final SimpleComponentCore component : pages)
					chatComponentPages.add(convertJsonToIChatBase(component.getJson()));

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
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
	 * Sets a block type and its data.
	 *
	 * @param block
	 * @param material
	 */
	public static void setTypeAndData(final Block block, final CompMaterial material) {
		if (MinecraftVersion.atLeast(V.v1_13))
			block.setType(material.getMaterial());
		else
			try {
				block.getClass().getMethod("setTypeIdAndData", int.class, byte.class, boolean.class).invoke(block, material.getId(), material.getData(), true);
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
	public static void setTypeAndData(final Block block, final Material material, final byte data) {
		setTypeAndData(block, CompMaterial.fromLegacy(material.name(), data));
	}

	/**
	 * Attempts to set render distance of the player to the given value
	 * returning false if we got a reflective exception (such as when not using PaperSpigot
	 * or on an outdated MC version).
	 * @param player
	 * @param viewDistanceChunks
	 *
	 * @return
	 */
	public static boolean setViewDistance(Player player, int viewDistanceChunks) {

		try {
			final Method setViewDistance = Player.class.getDeclaredMethod("setViewDistance", int.class);

			ReflectionUtil.invoke(setViewDistance, player, viewDistanceChunks);
			return true;

		} catch (final ReflectiveOperationException ex) {

			// Not using Paper or old MC version
			return false;
		}
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

			final Class<?> nmsWorldClass = ReflectionUtil.getNMSClass("World", "net.minecraft.world.level.World");
			final Class<?> nmsStackClass = ReflectionUtil.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Class<?> nmsEntityClass = ReflectionUtil.getNMSClass("Entity", "net.minecraft.world.entity.Entity");
			final Class<?> nmsItemClass = ReflectionUtil.getNMSClass("EntityItem", "net.minecraft.world.entity.item.EntityItem");

			final Constructor<?> entityConstructor = nmsItemClass.getConstructor(nmsWorldClass, double.class, double.class, double.class, nmsStackClass);

			final Object nmsWorld = location.getWorld().getClass().getMethod("getHandle").invoke(location.getWorld());
			final Method asNmsCopy = ReflectionUtil.getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			final Object nmsEntity = entityConstructor.newInstance(nmsWorld, location.getX(), location.getY(), location.getZ(), asNmsCopy.invoke(null, item));

			final Class<?> craftItemClass = ReflectionUtil.getOBCClass("entity.CraftItem");
			final Class<?> craftServerClass = ReflectionUtil.getOBCClass("CraftServer");

			final Object bukkitItem = craftItemClass.getConstructor(craftServerClass, nmsItemClass).newInstance(Bukkit.getServer(), nmsEntity);
			Valid.checkBoolean(bukkitItem instanceof Item, "Failed to make an dropped item, got " + bukkitItem.getClass().getSimpleName());

			// Default delay to 750ms
			try {
				((Item) bukkitItem).setPickupDelay(15);
			} catch (final Throwable t) {
				// unsupported
			}

			if (modifier != null)
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

		else {
			if (item.getAmount() > 1)
				item.setAmount(item.getAmount() - 1);

			// Explanation: For some weird reason there is a bug not removing 1 piece of ItemStack in 1.8.8
			else {
				final ItemStack[] content = player.getInventory().getContents();

				for (int slot = 0; slot < content.length; slot++) {
					final ItemStack slotItem = content[slot];

					if (slotItem != null && slotItem.equals(item)) {
						content[slot] = null;

						break;
					}
				}

				player.getInventory().setContents(content);
			}

			player.updateInventory();
		}
	}

	/**
	 * Converts the item into Adventure hover
	 *
	 * @param item
	 * @return
	 */
	public static HoverEvent<?> convertItemStackToHoverEvent(ItemStack item) {
		Valid.checkBoolean(item != null && !CompMaterial.isAir(item), "Hover item must not be null or air");

		if (item instanceof HoverEventSource)
			return ((HoverEventSource<?>) item).asHoverEvent();

		final Material material = item.getType();
		final Object nsmItemStack = Remain.asNMSCopy(item);
		final Object nmsCompound = ReflectionUtil.invoke(getNmsItemStackTag, nsmItemStack);
		final BinaryTagHolder binaryTagHolder = nmsCompound != null ? BinaryTagHolder.binaryTagHolder(nmsCompound.toString()) : null;

		Key key;

		if (MinecraftVersion.atLeast(V.v1_13))
			key = Key.key(material.getKey().getNamespace(), material.getKey().getKey());

		else
			key = Key.key(material.name().toLowerCase());

		return HoverEvent.showItem(key, item.getAmount(), binaryTagHolder);

	}

	/**
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string
	 * for sending with components.
	 *
	 * @param item the item to convert
	 * @return the Json string representation of the item
	 */
	public static String convertItemStackToJson(ItemStack item) {
		if (MinecraftVersion.atLeast(V.v1_4)) {
			// ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
			final Class<?> craftItemstack = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
			final Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemstack, "asNMSCopy", ItemStack.class);

			Valid.checkNotNull(asNMSCopyMethod, "Unable to find " + craftItemstack + "#asNMSCopy() method for server version " + Bukkit.getBukkitVersion());

			// NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
			final Class<?> nmsItemStack = ReflectionUtil.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Object nmsItemStackObj = ReflectionUtil.invoke(asNMSCopyMethod, null, item);

			if (MinecraftVersion.newerThan(V.v1_20) || (MinecraftVersion.atLeast(V.v1_20) && MinecraftVersion.getSubversion() > 4)) {
				if (Remain.isPaper()) {
					final Class<?> providerClass = ReflectionUtil.lookupClass("net.minecraft.core.HolderLookup$Provider");
					final Method saveMethod = ReflectionUtil.getMethod(nmsItemStack, "saveOptional", providerClass);

					final Object registryAccess = ReflectionUtil.invoke("registryAccess", Remain.getHandleServer());
					final Object compoundTag = ReflectionUtil.invoke(saveMethod, nmsItemStackObj, registryAccess);

					return compoundTag.toString();
				} else
					// Spigot has different mappings so we just give up and render the base item
					return "{Count:" + item.getAmount() + "b,id:\"" + item.getType().getKey().toString() + "\"}";

			} else {
				final Class<?> nbtTagCompound = ReflectionUtil.getNMSClass("NBTTagCompound", "net.minecraft.nbt.NBTTagCompound");
				final Method saveItemstackMethod = ReflectionUtil.getMethod(nmsItemStack, MinecraftVersion.equals(V.v1_18) || MinecraftVersion.equals(V.v1_19) || (MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() < 5) ? "b" : "save", nbtTagCompound);

				Valid.checkNotNull(saveItemstackMethod, "Unable to find " + nmsItemStack + "#save() method for server version " + Bukkit.getBukkitVersion());

				final Object nmsNbtTagCompoundObj = ReflectionUtil.instantiate(nbtTagCompound);
				final Object itemAsJsonObject = ReflectionUtil.invoke(saveItemstackMethod, nmsItemStackObj, nmsNbtTagCompoundObj);

				return itemAsJsonObject.toString();
			}
		}

		return item.getType().toString();
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
	 * Update the player's inventory title without closing the window
	 *
	 * @param player the player
	 * @param title  the new title
	 * @deprecated use {@link PlayerUtil#updateInventoryTitle(Player, String)}
	 */
	@Deprecated
	public static void updateInventoryTitle(final Player player, String title) {
		try {
			final Object view = ReflectionUtil.invoke("getOpenInventory", player);
			final Method setTitle = ReflectionUtil.getMethod(view.getClass(), "setTitle", String.class);

			if (setTitle == null)
				throw new NoSuchMethodError();

			ReflectionUtil.invoke(setTitle, view, Common.colorize(title));

		} catch (final NoSuchMethodError err) {

			final Inventory topInventory = Remain.getTopInventoryFromOpenInventory(player);

			try {
				if (MinecraftVersion.atLeast(V.v1_17)) {
					final String nmsVersion = MinecraftVersion.getServerVersion();

					final boolean is1_17 = MinecraftVersion.equals(V.v1_17);
					final boolean is1_18 = MinecraftVersion.equals(V.v1_18);
					final boolean is1_19 = MinecraftVersion.equals(V.v1_19);

					final Object nmsPlayer = Remain.getHandleEntity(player);
					final Object chatComponent = convertAdventureToIChatBase(Common.colorize(title));

					final int inventorySize = topInventory.getSize() / 9;
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

					String activeContainerName;

					if (is1_17)
						activeContainerName = "bV";

					else if (is1_18)
						activeContainerName = nmsVersion.contains("R2") ? "bV" : "bW";

					else if (is1_19)
						activeContainerName = nmsVersion.contains("R3") ? "bP" : "bU";

					else
						activeContainerName = "bR";

					final Object activeContainer = ReflectionUtil.getFieldContent(nmsPlayer, activeContainerName);
					final int windowId = ReflectionUtil.getFieldContent(activeContainer, "j");
					Remain.sendPacket(player, ReflectionUtil.instantiate(packetConstructor, windowId, container, chatComponent));

					// Re-initialize the menu internally
					Method method = ReflectionUtil.getMethod(nmsPlayer.getClass(), "initMenu", ReflectionUtil.lookupClass("net.minecraft.world.inventory.Container"));

					if (method == null)
						method = ReflectionUtil.getMethod(nmsPlayer.getClass(), "a", ReflectionUtil.lookupClass("net.minecraft.world.inventory.Container"));

					if (method != null)
						ReflectionUtil.invoke(method, nmsPlayer, activeContainer);

					return;
				}

				if (MinecraftVersion.olderThan(V.v1_9) && title.length() > 32)
					title = title.substring(0, 32);

				final Object entityPlayer = getHandleEntity(player);
				final Object activeContainer = entityPlayer.getClass().getField("activeContainer").get(entityPlayer);
				final Object windowId = activeContainer.getClass().getField("windowId").get(activeContainer);

				final Object packetOpenWindow;

				if (MinecraftVersion.atLeast(V.v1_8)) {
					final Constructor<?> chatMessageConst = ReflectionUtil.getNMSClass("ChatMessage", "net.minecraft.network.chat.ChatMessage").getConstructor(String.class, Object[].class);
					final Object chatMessage = chatMessageConst.newInstance(CompChatColor.translateColorCodes(title), new Object[0]);

					if (MinecraftVersion.newerThan(V.v1_13)) {
						final int inventorySize = topInventory.getSize() / 9;

						if (inventorySize < 1 || inventorySize > 6) {
							Common.log("Cannot update title for " + player.getName() + " as his inventory has non typical size: " + inventorySize + " rows");

							return;
						}

						final Class<?> containersClass = ReflectionUtil.getNMSClass("Containers", "net.minecraft.world.inventory.Containers");
						final Constructor<?> packetConst = ReflectionUtil.getNMSClass("PacketPlayOutOpenWindow", "net.minecraft.network.protocol.game.PacketPlayOutOpenWindow")
								.getConstructor(/*windowID*/int.class, /*containers*/containersClass, /*msg*/ReflectionUtil.getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"));

						final String containerName = "GENERIC_9X" + inventorySize;

						final Object container = containersClass.getField(containerName).get(null);

						packetOpenWindow = packetConst.newInstance(windowId, container, chatMessage);

					} else {
						final Constructor<?> packetConst = ReflectionUtil.getNMSClass("PacketPlayOutOpenWindow", "N/A").getConstructor(int.class, String.class, ReflectionUtil.getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"), int.class);

						packetOpenWindow = packetConst.newInstance(windowId, "minecraft:chest", chatMessage, topInventory.getSize());
					}
				} else {
					final Constructor<?> openWindow = ReflectionUtil.getConstructor(
							ReflectionUtil.getNMSClass(MinecraftVersion.atLeast(V.v1_7) ? "PacketPlayOutOpenWindow" : "Packet100OpenWindow", "N/A"), int.class, int.class, String.class, int.class, boolean.class);

					packetOpenWindow = ReflectionUtil.instantiate(openWindow, windowId, 0, CompChatColor.translateColorCodes(title), topInventory.getSize(), true);
				}

				sendPacket(player, packetOpenWindow);
				entityPlayer.getClass().getMethod("updateInventory", ReflectionUtil.getNMSClass("Container", "net.minecraft.world.inventory.Container")).invoke(entityPlayer, activeContainer);

			} catch (final ReflectiveOperationException ex) {
				Common.error(ex, "Error updating " + player.getName() + " inventory title to '" + title + "'");
			}
		}
	}

	/**
	 * Attempts to cancel all tasks
	 */
	public static void cancelTasks() {
		if (Remain.isFolia())
			ReflectionUtil.invoke(cancelTasks, foliaScheduler, SimplePlugin.getInstance());
		else
			Bukkit.getScheduler().cancelTasks(SimplePlugin.getInstance());
	}

	/**
	 * Runs the task even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return the task or null
	 */
	public static Task runLater(final int delayTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle;

			if (delayTicks == 0)
				taskHandle = ReflectionUtil.invoke(execute, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run());
			else
				taskHandle = ReflectionUtil.invoke(runDelayed, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), delayTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskLater(SimplePlugin.getInstance(), delayTicks);

			else
				task = Bukkit.getScheduler().runTaskLater(SimplePlugin.getInstance(), runnable, delayTicks);

			final SimpleTask simpleTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleSyncDelayedTask(SimplePlugin.getInstance(), runnable, delayTicks), false);
		}
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return the task or null
	 */
	public static Task runLaterAsync(final int delayTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle;

			if (delayTicks == 0)
				taskHandle = ReflectionUtil.invoke(execute, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run());
			else
				taskHandle = ReflectionUtil.invoke(runDelayed, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), delayTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskLaterAsynchronously(SimplePlugin.getInstance(), delayTicks);

			else
				task = Bukkit.getScheduler().runTaskLaterAsynchronously(SimplePlugin.getInstance(), runnable, delayTicks);

			final SimpleTask simpleTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleAsyncDelayedTask(SimplePlugin.getInstance(), runnable, delayTicks), true);
		}
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param delayTicks  the delay before first run
	 * @param repeatTicks the delay between each run
	 * @param runnable        the task
	 * @return the bukkit task or null if error
	 */
	public static Task runTimer(final int delayTicks, final int repeatTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle = ReflectionUtil.invoke(runAtFixedRate, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), Math.max(1, delayTicks), repeatTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskTimer(SimplePlugin.getInstance(), delayTicks, repeatTicks);

			else
				task = Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks);

			final SimpleTask simpleTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleSyncRepeatingTask(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks), false);
		}
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static Task runTimerAsync(final int delayTicks, final int repeatTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle = ReflectionUtil.invoke(runAtFixedRate, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), Math.max(1, delayTicks), repeatTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskTimerAsynchronously(SimplePlugin.getInstance(), delayTicks, repeatTicks);

			else
				task = Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks);

			final SimpleTask simplTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simplTask);

			return simplTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleAsyncRepeatingTask(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks), true);
		}
	}

	// Check our plugin instance if it's enabled
	// In case it is disabled, just runs the task and returns true
	// Otherwise we return false and the task will be run correctly in Bukkit scheduler
	// This is fail-safe to critical save-on-exit operations in case our plugin is improperly reloaded (PlugMan) or malfunctions
	private static boolean runIfDisabled(final Runnable run) {
		if (!SimplePlugin.getInstance().isEnabled()) {
			run.run();

			return true;
		}

		return false;
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
			ValidCore.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "setCustomName requires Minecraft 1.7.10+");

			final NBTEntity nbt = new NBTEntity(entity);

			nbt.removeKey("CustomNameVisible");
			nbt.removeKey("CustomName");
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
					final Object respawnEnum = ReflectionUtil.getNMSClass("EnumClientCommand", "N/A").getEnumConstants()[0];
					final Constructor<?>[] constructors = ReflectionUtil.getNMSClass("PacketPlayInClientCommand", "N/A").getConstructors();

					for (final Constructor<?> constructor : constructors) {
						final Class<?>[] args = constructor.getParameterTypes();
						if (args.length == 1 && args[0] == respawnEnum.getClass()) {
							final Object packet = ReflectionUtil.getNMSClass("PacketPlayInClientCommand", "N/A").getConstructor(args).newInstance(respawnEnum);

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

	// ----------------------------------------------------------------------------------------------------
	// Getters for various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return if the server is running Paper, formerly PaperSpigot software.
	 *
	 * @return true if the server is running Paper(Spigot)
	 */
	public static boolean isPaper() {
		return isPaper;
	}

	/**
	 * Return true if this is a Folia server
	 *
	 * @return
	 */
	public static boolean isFolia() {
		return isFolia;
	}

	/**
	 * Return true if this is a Thermos server
	 *
	 * @return
	 */
	public static boolean isThermos() {
		return isThermos;
	}

	/**
	 * Return true if we are using mojang mappings (note that Foundation's NMS
	 * does not support them but will fail gracefully)
	 *
	 * @return
	 */
	public static boolean isUsingMojangMappings() {
		return isUsingMojangMappings;
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
	 * Returns true if we have the complicated io.papermc.paper.event.player.AsyncChatEvent
	 * @return
	 */
	public static boolean hasAdventureChatEvent() {
		return hasAdventureChatEvent;
	}

	/**
	 * Return if the server version supports SpawnEggMeta
	 *
	 * @return true if egg meta are supported
	 */
	public static boolean hasSpawnEggMeta() {
		return hasSpawnEggMeta;
	}

	/**
	 * Is this server supporting particles?
	 *
	 * @return if server supports native particle api
	 */
	public static boolean hasWorldSpawnParticle() {
		return hasWorldSpawnParticle;
	}

	/**
	 * Is this server supporting native scoreboard api?
	 *
	 * @return if server supports native scoreboard api
	 */
	public static boolean hasScoreboardGetScore() {
		return hasObjectiveGetScore;
	}

	/**
	 * Is this server supporting permanent scoreboard tags?
	 *
	 * @return if server supports permanent scoreboard tags
	 */
	public static boolean hasEntityGetScoreboardTags() {
		return hasEntityGetScoreboardTags;
	}

	/**
	 * Return if the server version supports {@link YamlConfiguration#load(java.io.Reader)}
	 * otherwise you need to use just {@link InputStream}
	 *
	 * @return
	 */
	public static boolean hasYamlConfigurationLoadReader() {
		return hasYamlConfigurationLoadReader;
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
	 * Return if the Entity class has the addPassenger method
	 *
	 * @return
	 */
	public static boolean hasEntityAddPassenger() {
		return hasEntityAddPassenger;
	}

	/**
	 *
	 * Return true if player inventory class has extra inventory content
	 *
	 * @return
	 */
	public static boolean hasPlayerExtraInventoryContent() {
		return hasPlayerExtraInventoryContent;
	}

	/**
	 * Return true if the Player class has the open sign method
	 *
	 * @return
	 */
	public static boolean hasPlayerOpenSignMethod() {
		return hasPlayerOpenSignMethod;
	}
}

/**
 * A wrapper for advancements
 */
final class AdvancementAccessor {

	private final NamespacedKey key;
	private final String icon;
	private final String message;
	private final CompToastStyle toastStyle;

	AdvancementAccessor(final String message, final String icon, CompToastStyle toastStyle) {
		this.key = new NamespacedKey(SimplePlugin.getInstance(), UUID.randomUUID().toString());
		this.message = message;
		this.icon = icon;
		this.toastStyle = toastStyle;
	}

	public void show(final Player player) {
		this.loadAdvancement();
		this.grantAdvancement(player);

		Common.runLater(10, () -> {
			this.revokeAdvancement(player);
			this.removeAdvancement();
		});
	}

	private void loadAdvancement() {
		Bukkit.getUnsafe().loadAdvancement(this.key, this.compileJson0());
	}

	private String compileJson0() {
		final JsonObject json = new JsonObject();

		final JsonObject icon = new JsonObject();

		if (MinecraftVersion.atLeast(V.v1_21) || (MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() >= 5)) {
			icon.addProperty("id", this.icon);
		} else
			icon.addProperty("item", this.icon);

		final JsonObject display = new JsonObject();
		display.add("icon", icon);
		display.addProperty("title", this.message);
		display.addProperty("description", "");
		display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/adventure.png");
		display.addProperty("frame", this.toastStyle.getKey());
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
		final Advancement adv = this.getAdvancement();
		final AdvancementProgress progress = plazer.getAdvancementProgress(adv);

		if (!progress.isDone())
			progress.getRemainingCriteria().forEach(crit -> progress.awardCriteria(crit));
	}

	private void revokeAdvancement(final Player plazer) {
		final Advancement adv = this.getAdvancement();
		final AdvancementProgress prog = plazer.getAdvancementProgress(adv);

		if (prog.isDone())
			prog.getAwardedCriteria().forEach(crit -> prog.revokeCriteria(crit));
	}

	private void removeAdvancement() {
		Bukkit.getUnsafe().removeAdvancement(this.key);
	}

	private Advancement getAdvancement() {
		return Bukkit.getAdvancement(this.key);
	}
}

final class PotionSetter {

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

		if (wrapped != null && MinecraftVersion.olderThan(V.v1_20))
			try {
				meta.setBasePotionType(wrapped);

			} catch (final NoSuchMethodError ex) {
			}

		if (level > 0 && wrapped == null) {
			Class<?> potionDataClass = null;

			try {
				potionDataClass = ReflectionUtil.lookupClass("org.bukkit.potion.PotionData");
			} catch (final Exception e) {
			}

			if (potionDataClass != null) {
				final Constructor<?> potionConst = ReflectionUtil.getConstructor(potionDataClass, PotionType.class, boolean.class, boolean.class);
				final Object potionData = ReflectionUtil.instantiate(potionConst, level > 0 && wrapped != null ? wrapped : PotionType.WATER, false, false);
				final Method setBasePotionData = ReflectionUtil.getMethod(meta.getClass(), "setBasePotionData", potionDataClass);

				ReflectionUtil.invoke(setBasePotionData, meta, potionData);
			}
		}

		// For some reason this does not get added so we have to add it manually on top of the lore
		if (MinecraftVersion.olderThan(V.v1_9)) {
			if (item.getData().getData() == 0) {
				final List<String> lore = new ArrayList<>();
				final String potionLine = Common.colorizeLegacy("&7" + ItemUtil.bountify(type) + " (" + TimeUtil.formatTimeColon(durationTicks / 20) + ")");

				lore.add(potionLine);

				if (meta.getLore() != null)
					for (final String otherLore : meta.getLore())
						if (!otherLore.contains(potionLine))
							lore.add(otherLore);

				item.getData().setData((byte) 45);

				meta.setDisplayName(Common.colorizeLegacy("&rPotion Of " + ItemUtil.bountify(type)));
				meta.setLore(lore);
			}
		}

		//meta.setMainEffect(type);
		meta.addCustomEffect(new PotionEffect(type, durationTicks, level - 1), true);

		item.setItemMeta(meta);
	}
}
