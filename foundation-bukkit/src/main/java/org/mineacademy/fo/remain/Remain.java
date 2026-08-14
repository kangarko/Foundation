package org.mineacademy.fo.remain;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.util.Vector;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.CompToastStyle;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.UUIDToNameConverter;
import org.mineacademy.fo.platform.BukkitPlugin;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.nbt.NBTEntity;
import org.mineacademy.fo.settings.Lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.md_5.bungee.api.chat.BaseComponent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Remain {

	/**
	 * The full package name for NMS.
	 */
	private static final String NMS = "net.minecraft.server";

	/**
	 * The package name for Craftbukkit.
	 */
	private static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";

	/**
	 * Stores player cooldowns for old MC versions
	 */
	private final static Map<UUID /*Player*/, Map<Material, Integer>> cooldowns = new HashMap<>();

	/**
	 * Must manually unfreeze in your plugin, resolves https://github.com/kangarko/ChatControl/issues/2662
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
	private static Method fromJSONToNMSComponentmethod;
	private static Constructor<?> chatPacketConstructor;
	private static Constructor<?> tabConstructor;

	/**
	 * Fields related to Folia
	 */
	private static Object foliaScheduler;
	private static Method runAtFixedRate;
	private static Method runDelayed;
	private static Method execute;
	private static Method cancel;

	/**
	 * Fields related to Folia async scheduler
	 */
	private static Object foliaAsyncScheduler;
	private static Method asyncExecute;
	private static Method asyncRunDelayed;
	private static Method asyncRunAtFixedRate;

	/**
	 * Fields related to Folia entity scheduler
	 */
	private static Method entityGetScheduler;
	private static Method entitySchedulerRun;
	private static Method entitySchedulerRunDelayed;

	/**
	 * Fields related to skull handling.
	 */
	private static Method metaSetProfileMethod;
	private static Field metaProfileField;

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
	 * Cached Moonrise TickThread.isTickThread() method on Paper/Folia, null elsewhere.
	 * Returns true on the main thread (Paper) or any region tick thread (Folia).
	 */
	private static java.lang.reflect.Method isTickThreadMethod = null;

	// Backing Moonrise method for Remain#isTickThreadFor(Entity).
	private static java.lang.reflect.Method isTickThreadForEntityMethod = null;

	// CraftEntity.getHandleRaw() — skips the TickThread check that getHandle() performs,
	// required to pass a foreign-region entity into isTickThreadForEntityMethod.
	private static java.lang.reflect.Method getHandleRawMethod = null;

	/**
	 * Return true if this server is Thermos
	 */
	private static boolean isThermos = false;

	/**
	 * Return true if we are on mojang remapped server
	 */
	private static boolean isUsingMojangMappings = false;

	/**
	 * Return true if the CommandSender class implements Audience
	 */
	private static boolean isCommandSenderAudience = true;

	/**
	 * Does the current server version support book event?
	 */
	private static boolean hasBookEvent = true;

	/**
	 * Return true if we have the overcomplicated io.papermc.paper.event.player.AsyncChatEvent
	 */
	private static boolean hasAdventureChatEvent = true;

	/**
	 * Does the server's Adventure api expose TranslatableComponent#arguments()? Added in Adventure 4.15.0.
	 * When false, the bundled Adventure serializers crash with NoSuchMethodError on translatable components.
	 */
	private static boolean hasAdventureTranslatableArguments = true;

	/**
	 * Does the current server have Paper's PlayerProfile API (Paper 1.12.2+)?
	 */
	private static boolean hasPaperProfile = false;

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

	/**
	 * Return true if Player has openVirtualSign method (Paper 1.21.7+).
	 */
	private static boolean hasPlayerOpenVirtualSignMethod = false;

	/**
	 * Return true if the Entity class has the getHeight method (Bukkit 1.13+).
	 * On older versions we fall back to the NMS Entity.length field.
	 */
	private static boolean hasEntityGetHeight = true;

	/**
	 * Cached NMS Entity height field for 1.8 to 1.12, where Bukkit had no getHeight().
	 * The field is named "length" in NMS for these versions.
	 */
	private static Field nmsEntityHeightField = null;

	/**
	 * Return true if the Entity class has the getWidth method (Bukkit 1.13+).
	 * On older versions we fall back to the NMS Entity.width field.
	 */
	private static boolean hasEntityGetWidth = true;

	/**
	 * Cached NMS Entity width field for 1.8 to 1.12, where Bukkit had no getWidth().
	 * The field is named "width" in NMS for these versions.
	 */
	private static Field nmsEntityWidthField = null;

	/**
	 * The safeguard NMS prefix used in Bukkit 1.4 to 1.20.4.
	 *
	 * @deprecated internal use only and no longer needed on Minecraft 1.20.5 and greater
	 */
	@Deprecated
	@Getter
	private static String nmsVersion = "";

	static {

		// Initialize safeguard prefix first
		{
			final String packageName = Bukkit.getServer() == null ? "" : Bukkit.getServer().getClass().getPackage().getName();
			final String curr = packageName.substring(packageName.lastIndexOf('.') + 1);

			nmsVersion = !"craftbukkit".equals(curr) && !"".equals(packageName) ? curr : "";
		}

		isPaper = ReflectionUtil.isClassAvailable("co.aikar.timings.Timing");
		hasPaperProfile = isPaper && ReflectionUtil.isClassAvailable("com.destroystokyo.paper.profile.PlayerProfile");
		isFolia = ReflectionUtil.isClassAvailable("io.papermc.paper.threadedregions.RegionizedServer");
		isThermos = ReflectionUtil.isClassAvailable("thermos.ThermosRemapper");

		// Moonrise TickThread on modern Paper and Folia; absent on 1.8.x / legacy Spigot.
		if (ReflectionUtil.isClassAvailable("ca.spottedleaf.moonrise.common.util.TickThread")) {
			try {
				final Class<?> tickThreadClass = ReflectionUtil.lookupClass("ca.spottedleaf.moonrise.common.util.TickThread");

				isTickThreadMethod = tickThreadClass.getMethod("isTickThread");

				try {
					final Class<?> nmsEntityClass = ReflectionUtil.lookupClass("net.minecraft.world.entity.Entity");

					isTickThreadForEntityMethod = tickThreadClass.getMethod("isTickThreadFor", nmsEntityClass);

				} catch (final Throwable ignored) {

					// Forge hybrids throw unchecked ReflectionException from lookupClass, never crash clinit
					isTickThreadForEntityMethod = null;
				}

			} catch (final Throwable ex) {
				isTickThreadMethod = null;
			}
		}

		try {
			final Class<?> craftEntityClass = ReflectionUtil.lookupClass(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntity");

			getHandleRawMethod = craftEntityClass.getMethod("getHandleRaw");

		} catch (final Throwable ignored) {
			getHandleRawMethod = null;
		}
		isUsingMojangMappings = ReflectionUtil.isClassAvailable("net.minecraft.server.level.ServerPlayer");

		try {
			isCommandSenderAudience = Audience.class.isAssignableFrom(CommandSender.class);

		} catch (final Throwable t) {
			CommonCore.error(t, "Failed to find Audience class");
		}

		hasBookEvent = ReflectionUtil.isClassAvailable("org.bukkit.event.player.PlayerEditBookEvent");
		hasAdventureChatEvent = ReflectionUtil.isClassAvailable("io.papermc.paper.event.player.AsyncChatEvent");
		hasAdventureTranslatableArguments = ReflectionUtil.isClassAvailable("net.kyori.adventure.text.TranslatableComponent")
				&& ReflectionUtil.getMethod(ReflectionUtil.lookupClass("net.kyori.adventure.text.TranslatableComponent"), "arguments") != null;
		hasSpawnEggMeta = ReflectionUtil.isClassAvailable("org.bukkit.inventory.meta.SpawnEggMeta");
		hasAdvancements = ReflectionUtil.isClassAvailable("org.bukkit.advancement.Advancement") && ReflectionUtil.isClassAvailable("org.bukkit.NamespacedKey");

		try {
			getPlayersMethod = Bukkit.class.getMethod("getOnlinePlayers");
			isGetPlayersCollection = getPlayersMethod.getReturnType() == Collection.class;
		} catch (final Throwable t) {
			CommonCore.error(t, "Failed to find Bukkit.getOnlinePlayers()");
		}


		try {
			if (isUsingMojangMappings) {

				// Member names vary by mapping (Mojang, Forge SRG), but the connection is
				// always the only ServerGamePacketListenerImpl field on ServerPlayer
				final Class<?> serverPlayerClass = ReflectionUtil.lookupClass("net.minecraft.server.level.ServerPlayer");
				final Class<?> gameListenerClass = ReflectionUtil.lookupClass("net.minecraft.server.network.ServerGamePacketListenerImpl");

				fieldPlayerConnection = ReflectionUtil.getFieldByType(serverPlayerClass, gameListenerClass);

				if (fieldPlayerConnection == null)
					throw new NoSuchFieldException("No field of type ServerGamePacketListenerImpl found in " + serverPlayerClass.getName());

			} else {
				fieldPlayerConnection = Remain.getNMSClass("EntityPlayer", "net.minecraft.server.level.EntityPlayer")
						.getField(MinecraftVersion.atLeast(V.v1_20) ? "c" : MinecraftVersion.atLeast(V.v1_17) ? "b" : "playerConnection");
			}

		} catch (final Throwable t) {
			if (isUnsupportedPlatform())
				CommonCore.warning("Failed to find EntityPlayer.playerConnection on " + Platform.getPlatformName() + ", packet-dependent features are disabled.");
			else
				CommonCore.error(t, "Failed to find EntityPlayer.playerConnection");
		}

		if (MinecraftVersion.olderThan(V.v1_12))
			try {
				fieldEntityInvulnerable = Remain.getNMSClass("Entity").getDeclaredField("invulnerable");
				fieldEntityInvulnerable.setAccessible(true);
			} catch (final Throwable t) {
				// Unavailable
			}

		try {
			if (isUsingMojangMappings) {

				// The field failure was already logged above, stay quiet here
				if (fieldPlayerConnection != null) {
					final Class<?> packetClass = ReflectionUtil.lookupClass("net.minecraft.network.protocol.Packet");

					// Paper 1.20.5 through 1.21.6 declares a sendPacket(Packet) alias next to
					// send(Packet), which makes the signature search ambiguous. Try both names,
					// since mappings that rename the Mojang one leave the Paper alias alone, and
					// only search by signature when neither name survived
					sendPacket = ReflectionUtil.getMethod(fieldPlayerConnection.getType(), "send", packetClass);

					if (sendPacket == null)
						sendPacket = ReflectionUtil.getMethod(fieldPlayerConnection.getType(), "sendPacket", packetClass);

					if (sendPacket == null)
						sendPacket = ReflectionUtil.getMethodBySignature(fieldPlayerConnection.getType(), void.class, packetClass);

					if (sendPacket == null)
						throw new NoSuchMethodException("No void method taking a single Packet parameter found in " + fieldPlayerConnection.getType().getName());
				}

			} else {
				sendPacket = Remain.getNMSClass("PlayerConnection", "net.minecraft.server.network.PlayerConnection")
						.getMethod(MinecraftVersion.atLeast(V.v1_18) ? "a" : "sendPacket", Remain.getNMSClass("Packet", "net.minecraft.network.protocol.Packet"));
			}
		} catch (final Throwable t) {
			if (isUnsupportedPlatform())
				CommonCore.warning("Failed to find PlayerConnection.sendPacket() on " + Platform.getPlatformName() + ", packet-dependent features are disabled.");
			else
				CommonCore.error(t, "Failed to find PlayerConnection.sendPacket()");
		}

		if (MinecraftVersion.olderThan(V.v1_16))
			try {
				final Class<?> nmsItemStack = Remain.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");

				getNmsItemStackTag = ReflectionUtil.getMethod(nmsItemStack, "getTag");

			} catch (final Throwable t) {
				if (!isUnsupportedPlatform())
					CommonCore.error(t, "Failed to find NMS ItemStack.getTag()");
			}

		try {
			World.class.getMethod("spawnParticle", org.bukkit.Particle.class, Location.class, int.class);
		} catch (final Throwable ex) {
			hasWorldSpawnParticle = false;
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

		try {
			final Class<?> positionClass = Class.forName("io.papermc.paper.math.Position");
			final Class<?> sideClass = Class.forName("org.bukkit.block.sign.Side");

			Player.class.getMethod("openVirtualSign", positionClass, sideClass);

			hasPlayerOpenVirtualSignMethod = true;
		} catch (final Throwable ex) {
			// Not available
		}

		try {
			Entity.class.getMethod("getHeight");

		} catch (final Throwable ex) {
			hasEntityGetHeight = false;

			try {
				final Class<?> nmsEntityClass = Remain.getNMSClass("Entity", "net.minecraft.world.entity.Entity");

				nmsEntityHeightField = nmsEntityClass.getField("length");
				nmsEntityHeightField.setAccessible(true);

			} catch (final Throwable ex2) {
				if (!isThermos)
					CommonCore.error(ex2, "Failed to find NMS Entity height field on legacy MC");
			}
		}

		try {
			Entity.class.getMethod("getWidth");

		} catch (final Throwable ex) {
			hasEntityGetWidth = false;

			try {
				final Class<?> nmsEntityClass = Remain.getNMSClass("Entity", "net.minecraft.world.entity.Entity");

				nmsEntityWidthField = nmsEntityClass.getField("width");
				nmsEntityWidthField.setAccessible(true);

			} catch (final Throwable ex2) {
				if (!isThermos)
					CommonCore.error(ex2, "Failed to find NMS Entity width field on legacy MC");
			}
		}

		if (MinecraftVersion.olderThan(V.v1_17))
			try {
				final Class<?> chatSerializer = Remain.getNMSClass("IChatBaseComponent$ChatSerializer");

				fromJSONToNMSComponentmethod = ReflectionUtil.getMethod(chatSerializer, "a", String.class);

			} catch (final Throwable t) {
				if (!isUnsupportedPlatform())
					CommonCore.error(t, "Failed to find NMS ChatSerializer on legacy MC");
			}

		else
			try {
				final Class<?> chatSerializer = Class.forName("net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");

				fromJSONToNMSComponentmethod = ReflectionUtil.getMethod(chatSerializer, "a", String.class);

			} catch (final ClassNotFoundException e) {
				try {
					final Class<?> chatSerializer = Class.forName("net.minecraft.network.chat.Component$Serializer");

					fromJSONToNMSComponentmethod = chatSerializer.getMethod("fromJson", String.class);

				} catch (final Exception e1) {
					try {
						final Class<?> chatSerializer = Class.forName("org.bukkit.craftbukkit." + getNmsVersion() + ".util.CraftChatMessage");

						fromJSONToNMSComponentmethod = chatSerializer.getMethod("fromJSON", String.class);

					} catch (final Exception e2) {
						try {
							final Class<?> craftChatMessage = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");

							fromJSONToNMSComponentmethod = craftChatMessage.getMethod("fromJSON", String.class);

						} catch (final ReflectiveOperationException ex) {
							CommonCore.error(ex,
									"Failed to find CraftChatMessage.fromJSON() or ",
									"Component$Serializer or ",
									"IChatBaseComponent$ChatSerializer class. ",
									"Alert Foundation authors to update!");
						}
					}
				}
			}

		if (MinecraftVersion.olderThan(V.v1_13))
			try {
				final Class<?> chatBaseComponent = Remain.getNMSClass("IChatBaseComponent", "N/A");

				if (MinecraftVersion.olderThan(V.v1_12)) {
					final Class<?> chatPacket = Remain.getNMSClass("PacketPlayOutChat", "N/A");

					if (MinecraftVersion.newerThan(V.v1_11))
						chatPacketConstructor = chatPacket.getConstructor(chatBaseComponent, Remain.getNMSClass("ChatMessageType", "N/A"));
					else
						chatPacketConstructor = chatPacket.getConstructor(chatBaseComponent, byte.class);
				}

				final Class<?> tablistClass = Remain.getNMSClass("PacketPlayOutPlayerListHeaderFooter", "N/A");

				tabConstructor = MinecraftVersion.equals(V.v1_12) ? tablistClass.getConstructor() : tablistClass.getConstructor(chatBaseComponent);

			} catch (final Throwable t) {
				if (!isThermos)
					CommonCore.error(t, "Unable to setup chat internals");
			}

		if (isFolia)
			try {
				foliaScheduler = ReflectionUtil.invoke("getGlobalRegionScheduler", org.bukkit.Bukkit.getServer());
				runAtFixedRate = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
				execute = ReflectionUtil.getMethod(foliaScheduler.getClass(), "run", Plugin.class, Consumer.class);
				runDelayed = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class);
				cancel = ReflectionUtil.getMethod(ReflectionUtil.lookupClass("io.papermc.paper.threadedregions.scheduler.ScheduledTask"), "cancel");

				foliaAsyncScheduler = ReflectionUtil.invoke("getAsyncScheduler", org.bukkit.Bukkit.getServer());
				asyncExecute = ReflectionUtil.getMethod(foliaAsyncScheduler.getClass(), "runNow", Plugin.class, Consumer.class);
				asyncRunDelayed = ReflectionUtil.getMethod(foliaAsyncScheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
				asyncRunAtFixedRate = ReflectionUtil.getMethod(foliaAsyncScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);

				entityGetScheduler = Entity.class.getMethod("getScheduler");

				final Class<?> entitySchedulerClass = entityGetScheduler.getReturnType();

				entitySchedulerRun = entitySchedulerClass.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
				entitySchedulerRunDelayed = entitySchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
			} catch (final Throwable t) {
				CommonCore.error(t, "Failed to setup Folia scheduler");
			}
	}

	private static boolean isUnsupportedPlatform() {
		if (ReflectionUtil.isClassAvailable("net.minecraftforge.common.MinecraftForge") || ReflectionUtil.isClassAvailable("net.neoforged.neoforge.common.NeoForge"))
			return true;

		final String platformName = Platform.getPlatformName();

		return platformName.contains("Arclight") || platformName.contains("Cardboard") || (platformName.contains("CraftBukkit") && MinecraftVersion.atLeast(V.v1_12));
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
	 * Advanced: Sends a packet to the player
	 *
	 * @param player the player
	 * @param packet the packet
	 */
	public static void sendPacket(final Player player, final Object packet) {
		if (sendPacket == null || fieldPlayerConnection == null) {
			CommonCore.logTimed(60 * 60, "Cannot send packet " + packet.getClass() + ", this server's connection class could not be resolved on startup. This message only shows once per hour.");

			return;
		}

		final Object handle = getHandleEntity(player);
		final Object playerConnection = ReflectionUtil.getFieldContent(handle, fieldPlayerConnection);

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
	public static boolean isInvulnerable(final Entity entity) {
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
	public static void setInvulnerable(final Entity entity, final boolean invulnerable) {
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
			final Map<Material, Integer> cooldown = getCooldown(player);

			cooldown.put(material, cooldownTicks);
			cooldowns.put(player.getUniqueId(), cooldown);
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
			final Map<Material, Integer> cooldown = getCooldown(player);

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
			final Map<Material, Integer> cooldown = getCooldown(player);

			return cooldown.getOrDefault(material, 0);
		}
	}

	// Internal method to get a players cooldown map
	private static Map<Material, Integer> getCooldown(final Player player) {
		return cooldowns.getOrDefault(player.getUniqueId(), new HashMap<>());
	}

	// ----------------------------------------------------------------------------------------------------
	// Enchantment
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Freeze back enchant registry
	 *
	 * @deprecated called internally already in {@link BukkitPlugin}
	 */
	@Deprecated
	public static void freezeEnchantRegistry() {
		if (MinecraftVersion.newerThan(V.v1_20)) {
			final Object enchantmentRegistry = getEnchantRegistry();

			ReflectionUtil.setField(enchantmentRegistry, "frozen", true);
			return;
		}

		if (MinecraftVersion.atLeast(V.v1_19)) {
			final Object enchantmentRegistry = getEnchantRegistry();
			final Method freezeMethod = ReflectionUtil.getMethod(enchantmentRegistry.getClass(), Remain.isUsingMojangMappings() ? "freeze" : "l");

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
		if (MinecraftVersion.newerThan(V.v1_20)) {
			final Class<?> craftRegistryClass = ReflectionUtil.lookupClass("org.bukkit.craftbukkit.CraftRegistry");
			final Class<?> resourceKeyClass = ReflectionUtil.lookupClass("net.minecraft.resources.ResourceKey");
			final Class<?> registriesClass = ReflectionUtil.lookupClass("net.minecraft.core.registries.Registries");
			final Object enchantmentKey = ReflectionUtil.getStaticFieldContent(registriesClass, "ENCHANTMENT");
			final Method method = ReflectionUtil.getMethod(craftRegistryClass, "getMinecraftRegistry", resourceKeyClass);

			return ReflectionUtil.invoke(method, null, enchantmentKey);
		}

		final Class<?> registryClass = ReflectionUtil.lookupClass("net.minecraft.core.registries.BuiltInRegistries");
		final Object enchantmentRegistry = ReflectionUtil.getStaticFieldContent(registryClass, Remain.isUsingMojangMappings() ? "ENCHANTMENT" : MinecraftVersion.equals(V.v1_19) ? "g" : "f");

		return enchantmentRegistry;
	}

	/**
	 * Unfreeze enchant registry
	 *
	 * @deprecated called internally already in {@link BukkitPlugin}
	 */
	@Deprecated
	public static void unfreezeEnchantRegistry() {
		if (MinecraftVersion.newerThan(V.v1_20)) {
			final Object enchantmentRegistry = getEnchantRegistry();

			ReflectionUtil.setField(enchantmentRegistry, "frozen", false);

			enchantRegistryUnfrozen = true;
			return;
		}

		if (MinecraftVersion.atLeast(V.v1_19)) {
			final boolean mojMap = Remain.isUsingMojangMappings();
			final Object enchantmentRegistry = getEnchantRegistry();

			try {
				ReflectionUtil.setField(enchantmentRegistry, mojMap ? "frozen" : "l", false);
				ReflectionUtil.setField(enchantmentRegistry, mojMap ? "unregisteredIntrusiveHolders" : "m", new IdentityHashMap<>());

			} catch (final Throwable t) {
				try {
					ReflectionUtil.setField(enchantmentRegistry, mojMap ? "frozen" : "ca", false);

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
	public static Map<Object, Object> getAllSessionData(final ConversationContext context) {
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
	 * Returns the World#isEnabled() method which takes an entity type
	 *
	 * @return
	 */
	public static Method getIsEnabledFeatureWorldMethod() {
		final boolean hasFeatureClass = ReflectionUtil.isClassAvailable("io.papermc.paper.world.flag.FeatureDependant") && ReflectionUtil.isClassAvailable("io.papermc.paper.world.flag.FeatureFlagSetHolder");

		if (hasFeatureClass) {
			final Class<?> featureFlagSetHolderClass = ReflectionUtil.lookupClass("io.papermc.paper.world.flag.FeatureFlagSetHolder");
			final Class<?> featureDependent = ReflectionUtil.lookupClass("io.papermc.paper.world.flag.FeatureDependant");

			return ReflectionUtil.getMethod(featureFlagSetHolderClass, "isEnabled", featureDependent);
		}

		return null;
	}

	/**
	 * Return NMS copy of the given itemstack
	 *
	 * @param itemStack
	 * @return
	 */
	public static Object asNMSCopy(final ItemStack itemStack) {
		try {
			final Method asNmsCopy = Remain.getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			return asNmsCopy.invoke(null, itemStack);

		} catch (final ReflectiveOperationException ex) {
			CommonCore.throwError(ex, "Unable to convert item to NMS item: " + itemStack);

			return null;
		}
	}

	/**
	 * Return IChatBaseComponent from the given JSON
	 *
	 * @param legacy
	 * @return
	 */
	public static Object convertLegacyToIChatBase(final String legacy) {
		return convertJsonToIChatBase("{\"text\":\"" + legacy.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
	}

	/**
	 * Return IChatBaseComponent from the given JSON
	 *
	 * @param json
	 * @return
	 */
	public static Object convertJsonToIChatBase(final String json) {
		ValidCore.checkNotNull(fromJSONToNMSComponentmethod, "Cannot convert JSON to NMS Component - missing methods to do so (see earlier log). Json: " + json);

		return ReflectionUtil.invokeStatic(fromJSONToNMSComponentmethod, json);
	}

	/**
	 * Return the biome at the given location
	 *
	 * @param block
	 * @return
	 */
	public static Biome getBiome(final Block block) {
		try {
			final Method getBiome = ReflectionUtil.getMethod(Block.class, "getBiome");

			return ReflectionUtil.invoke(getBiome, block);

		} catch (final NoSuchMethodError err) {
			return getBiome(block.getLocation());
		}
	}

	/**
	 * Return the biome at the given location
	 *
	 * @param location
	 * @return
	 */
	public static Biome getBiome(final Location location) {
		final Method getBiome = ReflectionUtil.getMethod(World.class, "getBiome", int.class, int.class, int.class);

		if (getBiome != null)
			return ReflectionUtil.invoke(getBiome, location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		else
			return location.getWorld().getBiome(location.getBlockX(), location.getBlockZ());
	}

	/**
	 * Return the top inventory of the player's open inventory
	 *
	 * @param player
	 * @return
	 */
	public static Inventory getBottomInventoryFromOpenInventory(final Player player) {
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
		final Class<?> craftServer = Remain.getOBCClass("CraftServer");

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
	 * Return the entity by UUID. Async-safe on Paper/Folia via the native
	 * {@code Bukkit.getEntity(UUID)} entity tracker lookup (O(1)). Falls back
	 * to a full world scan only on pre-1.10 servers that lack the API.
	 *
	 * @param uuid
	 * @return
	 */
	public static Entity getLoadedEntity(final UUID uuid) {
		try {
			final Entity direct = Bukkit.getEntity(uuid);

			if (direct != null)
				return direct;

		} catch (final NoSuchMethodError legacy) {
			for (final World world : Bukkit.getWorlds())
				for (final Entity entity : world.getEntities())
					if (entity.getUniqueId().equals(uuid))
						return entity;
		}

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
	private static Object getHandle(final Object instance, final String methodName) {
		final Method handle = ReflectionUtil.getMethod(instance.getClass(), methodName);
		ValidCore.checkNotNull(handle, "Cannot call " + methodName + "() for " + instance.getClass() + " (" + instance + ")");

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
	public static Block getHitBlock(final ProjectileHitEvent event) {
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
	public static LivingEntity getHitEntity(final ProjectileHitEvent event) {
		try {
			if (event.getHitEntity() instanceof LivingEntity)
				return (LivingEntity) event.getHitEntity();

		} catch (final Throwable t) {
			final Projectile projectile = event.getEntity();
			final Vector velocity = projectile.getVelocity();
			final Location impactPoint = projectile.getLocation().add(velocity);
			final Entity shooter = projectile.getShooter() instanceof Entity ? (Entity) projectile.getShooter() : null;
			final double scanRadius = Math.max(2, velocity.length() + 1);

			LivingEntity closest = null;
			double closestDist = Double.MAX_VALUE;

			for (final Entity nearby : projectile.getNearbyEntities(scanRadius, scanRadius, scanRadius)) {
				if (!(nearby instanceof LivingEntity) || nearby.equals(shooter) || nearby.equals(projectile))
					continue;

				final double dist = nearby.getLocation().distanceSquared(impactPoint);

				if (dist < closestDist) {
					closestDist = dist;
					closest = (LivingEntity) nearby;
				}
			}

			return closest;
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
			return ChatUtil.capitalizeFully(item.getType());
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
	public static String getEntityName(final Entity entity) {
		try {
			return entity.getName();

		} catch (final NoSuchMethodError t) {
			return entity instanceof Player ? ((Player) entity).getName() : ChatUtil.capitalizeFully(entity.getType());
		}
	}

	/**
	 * Return the height of the entity in blocks (its bounding box vertical size).
	 *
	 * Uses Bukkit's Entity.getHeight() on 1.13+ and falls back to the NMS Entity.length
	 * field on 1.8 to 1.12 where the Bukkit API method does not exist.
	 *
	 * @param entity
	 * @return
	 */
	public static double getEntityHeight(final Entity entity) {
		if (hasEntityGetHeight)
			return entity.getHeight();

		if (nmsEntityHeightField == null)
			return 1;

		try {
			return ((Number) nmsEntityHeightField.get(getHandleEntity(entity))).doubleValue();

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Failed to read NMS Entity.length for " + entity);
		}
	}

	/**
	 * Return the width of the entity in blocks (its bounding box horizontal size).
	 *
	 * Uses Bukkit's Entity.getWidth() on 1.13+ and falls back to the NMS Entity.width
	 * field on 1.8 to 1.12 where the Bukkit API method does not exist.
	 *
	 * @param entity
	 * @return
	 */
	public static double getEntityWidth(final Entity entity) {
		if (hasEntityGetWidth)
			return entity.getWidth();

		if (nmsEntityWidthField == null)
			return 1;

		try {
			return ((Number) nmsEntityWidthField.get(getHandleEntity(entity))).doubleValue();

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Failed to read NMS Entity.width for " + entity);
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
			final List<Entity> all = location.getWorld().getEntities();

			for (final Iterator<Entity> it = all.iterator(); it.hasNext();) {
				final Entity nearby = it.next();

				if (nearby.isValid() && nearby.getLocation().distance(location) <= radius)
					found.add(nearby);
			}

			return found;
		}
	}

	/**
	 * Perform a ray trace from a start location along a direction, returning the closest
	 * hit entity within maxDistance, or null.
	 *
	 * On MC 1.13+ uses native World.rayTraceEntities(). Falls back to manual AABB
	 * intersection against nearby entities for older versions.
	 *
	 * @param start the eye/start location
	 * @param direction the normalized direction vector
	 * @param maxDistance the maximum distance to check
	 * @return the closest hit entity, or null
	 */
	public static Entity rayTraceEntities(final Location start, final Vector direction, final double maxDistance) {
		return rayTraceEntities(start, direction, maxDistance, null);
	}

	/**
	 * Perform a ray trace from a start location along a direction, returning the closest
	 * hit entity within maxDistance that matches the filter, or null.
	 *
	 * On MC 1.13+ uses native World.rayTraceEntities(). Falls back to manual AABB
	 * intersection against nearby entities for older versions.
	 *
	 * @param start the eye/start location
	 * @param direction the normalized direction vector
	 * @param maxDistance the maximum distance to check
	 * @param filter predicate to filter entities, or null for all entities
	 * @return the closest hit entity, or null
	 */
	public static Entity rayTraceEntities(final Location start, final Vector direction, final double maxDistance, final java.util.function.Predicate<Entity> filter) {
		try {
			final org.bukkit.util.RayTraceResult result = start.getWorld().rayTraceEntities(start, direction, maxDistance, 0.0, filter);

			return result != null ? result.getHitEntity() : null;

		} catch (final Throwable t) {
			Entity closest = null;
			double closestDist = Double.MAX_VALUE;

			for (final Entity entity : getNearbyEntities(start, maxDistance)) {
				if (filter != null && !filter.test(entity))
					continue;

				final Location feet = entity.getLocation();
				final double halfWidth = getEntityWidth(entity) / 2.0;
				final double height = getEntityHeight(entity);

				final double hitDist = EntityUtil.rayIntersectsAABB(start.toVector(), direction, feet.toVector(), halfWidth, height);

				if (hitDist < 0 || hitDist > maxDistance || hitDist >= closestDist)
					continue;

				closestDist = hitDist;
				closest = entity;
			}

			return closest;
		}
	}

	/**
	 * Return the NMS statistic name for the given statistic
	 *
	 * @param stat
	 * @param mat
	 * @param entityType
	 * @return
	 */
	public static String getNMSStatisticName(final Statistic stat, final Material mat, final EntityType entityType) {
		final Class<?> craftStatistic = Remain.getOBCClass("CraftStatistic");
		Object nmsStatistic = null;

		try {
			if (stat.getType() == Statistic.Type.UNTYPED)
				nmsStatistic = craftStatistic.getMethod("getNMSStatistic", stat.getClass()).invoke(null, stat);

			else if (stat.getType() == Statistic.Type.ENTITY)
				nmsStatistic = craftStatistic.getMethod("getEntityStatistic", stat.getClass(), entityType.getClass()).invoke(null, stat, entityType);

			else
				nmsStatistic = craftStatistic.getMethod("getMaterialStatistic", stat.getClass(), mat.getClass()).invoke(null, stat, mat);

			ValidCore.checkNotNull(nmsStatistic, "Could not get NMS statistic from Bukkit's " + stat);

			if (MinecraftVersion.equals(V.v1_8)) {
				final Field f = nmsStatistic.getClass().getField("name");
				f.setAccessible(true);
				return f.get(nmsStatistic).toString();
			}

			return (String) nmsStatistic.getClass().getMethod(MinecraftVersion.atLeast(V.v1_21) ? "getName" : MinecraftVersion.atLeast(V.v1_18) ? "d" : "getName").invoke(nmsStatistic);
		} catch (final Throwable t) {
			throw new FoException(t, "Error getting NMS statistic name from " + stat);
		}
	}

	/**
	 * Tries to find offline player by uuid
	 *
	 * @param uniqueId
	 * @return
	 */
	public static OfflinePlayer getOfflinePlayerByUniqueId(final UUID uniqueId) {
		try {
			return Bukkit.getOfflinePlayer(uniqueId);

		} catch (final NoSuchMethodError err) {
			if (Bukkit.isPrimaryThread())
				CommonCore.log("getOfflinePlayerByUUID required two blocking calls on main thread - please notify " + BukkitPlugin.getInstance().getName() + " plugin authors.");

			try {
				final String name = new UUIDToNameConverter(uniqueId).call();

				return Bukkit.getOfflinePlayer(name);

			} catch (final Throwable t) {
				CommonCore.error(t, "Failed to get offline player by UUID: " + uniqueId);

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
	public static int getPing(final Player player) {
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
		return ReflectionUtil.lookupEnum(Statistic.class, MinecraftVersion.olderThan(V.v1_13) ? "PLAY_ONE_TICK" : "PLAY_ONE_MINUTE");
	}

	/**
	 * Returns a scoreboard score
	 *
	 * @param obj
	 * @param entry
	 * @return
	 */
	public static Score getScore(final Objective obj, String entry) {
		ValidCore.checkNotNull(obj, "Objective cannot be null");

		entry = CompChatColor.translateColorCodes(entry);

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
	public static Inventory getTopInventoryFromOpenInventory(final Player player) {
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
	public static int getViewDistance(final Player player) {
		try {
			return player.getClientViewDistance();

		} catch (final NoSuchMethodError err) {
			final Method getViewDistance = ReflectionUtil.getMethod(player.spigot().getClass(), "getViewDistance");

			return ReflectionUtil.invoke(getViewDistance, player.spigot());
		}
	}

	public static <T> T invokeInventoryViewMethod(final InventoryEvent event, final String methodName) {
		if (MinecraftVersion.atLeast(V.v1_21))
			return invokeViewMethodDirect(event.getView(), methodName);

		final Object view = ReflectionUtil.invoke("getView", event);

		return ReflectionUtil.invoke(methodName, view);
	}

	public static <T> T invokeOpenInventoryMethod(final Player player, final String methodName) {
		if (MinecraftVersion.atLeast(V.v1_21))
			return invokeViewMethodDirect(player.getOpenInventory(), methodName);

		final Object view = ReflectionUtil.invoke("getOpenInventory", player);

		return ReflectionUtil.invoke(methodName, view);
	}

	// On 1.21+ InventoryView is an interface (SPIGOT-7768); direct INVOKEINTERFACE bypasses Paper's reflection-rewriter (kangarko/Protect#108).
	private static <T> T invokeViewMethodDirect(final InventoryView view, final String methodName) {
		switch (methodName) {
			case "getType":
				return (T) view.getType();
			case "getTitle":
				return (T) view.getTitle();
			case "getTopInventory":
				return (T) view.getTopInventory();
			case "getBottomInventory":
				return (T) view.getBottomInventory();
			default:
				return ReflectionUtil.invoke(methodName, view);
		}
	}

	/**
	 * See {@link #isInteractEventPrimaryHand(PlayerInteractEvent)}
	 *
	 * @param event
	 * @return
	 */
	public static boolean isInteractEventPrimaryHand(final PlayerInteractEntityEvent event) {
		if (MinecraftVersion.olderThan(V.v1_9))
			return true;

		try {
			return event.getHand() != null && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;

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
	public static boolean isInvisible(final Entity entity) {
		if (entity instanceof LivingEntity && MinecraftVersion.atLeast(V.v1_16))
			return entity.isInvisible();

		final Object nmsEntity = getHandleEntity(entity);
		return (boolean) ReflectionUtil.invoke("isInvisible", nmsEntity);
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

			return con.newInstance(label, BukkitPlugin.getInstance());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Unable to create command: /" + label);
		}
	}

	/**
	 * Opens the book for the player given the book is a WRITTEN_BOOK
	 *
	 * @param audience
	 * @param book
	 */
	public static void openBook(final FoundationPlayer audience, final ItemStack book) {
		openBook((Player) audience.getPlayer(), book);
	}

	/**
	 * Opens the book for the player given the book is a WRITTEN_BOOK
	 *
	 * @param player
	 * @param book
	 */
	public static void openBook(final Player player, final ItemStack book) {
		ValidCore.checkBoolean(MinecraftVersion.atLeast(V.v1_8), "Opening books is only supported on MC 1.8 and greater");
		ValidCore.checkBoolean(book.getItemMeta() instanceof org.bukkit.inventory.meta.BookMeta, "openBook method called for not a book item: " + book);
		ValidCore.checkBoolean(book.getType().toString().equals("WRITTEN_BOOK"), "Can only call openBook for WRITTEN_BOOK! Got: " + book);

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

			Platform.runTask(() -> {
				Method openBook = ReflectionUtil.getMethod(craftPlayer.getClass(), "openBook", nmsItemstack.getClass());

				if (openBook == null) {
					final Class<?> enumHand = Remain.getNMSClass("EnumHand");
					final Object mainHand = ReflectionUtil.getStaticFieldContent(enumHand, "MAIN_HAND");

					openBook = ReflectionUtil.getMethod(craftPlayer.getClass(), "a", nmsItemstack.getClass(), enumHand);
					ValidCore.checkNotNull(openBook, "Unable to find openBook method for " + craftPlayer);

					ReflectionUtil.invoke(openBook, craftPlayer, nmsItemstack, mainHand);

				} else
					ReflectionUtil.invoke(openBook, craftPlayer, nmsItemstack);

				// Reset hands
				player.setItemInHand(oldItem);
			});
		}
	}

	/**
	 * Opens the sign for the player. On legacy versions, the PacketEvents plugin
	 * is required to save the edits to the sign after updating it.
	 *
	 * @param player
	 * @param signBlock
	 */
	public static void openSign(final Player player, final Block signBlock) {
		final BlockState state = signBlock.getState();
		ValidCore.checkBoolean(state instanceof Sign, "Block is not a sign: " + signBlock);

		final Sign sign = (Sign) state;

		if (hasPlayerOpenSignMethod)
			player.openSign(sign);
		else {
			final Class<?> chatComponentClass = Remain.getNMSClass("IChatBaseComponent");
			final Class<?> blockPositionClass = Remain.getNMSClass("BlockPosition");

			final Object blockPosition = ReflectionUtil.instantiate(ReflectionUtil.getConstructor(blockPositionClass, int.class, int.class, int.class), signBlock.getX(), signBlock.getY(), signBlock.getZ());
			final Object[] chatComponent = (Object[]) java.lang.reflect.Array.newInstance(chatComponentClass, 4);

			for (int i = 0; i < 4; i++)
				chatComponent[i] = convertLegacyToIChatBase(sign.getLine(i));

			final Object nmsSign = Remain.getHandleEntity(sign);
			final Object nmsPlayer = Remain.getHandleEntity(player);

			// Set the sign to be editable and assign the editing player to it
			ReflectionUtil.setField(nmsSign, "isEditable", true);
			ReflectionUtil.setField(nmsSign, "h", nmsPlayer);

			CompMetadata.setTempMetadata(player, CompMetadata.TAG_OPENED_SIGN, sign.getLocation());
			Remain.sendPacket(player, ReflectionUtil.instantiate(Remain.getConstructorNMS("PacketPlayOutOpenSignEditor", blockPositionClass), blockPosition));
		}
	}

	/**
	 * Injects an existing command into the command map
	 *
	 * @param command
	 */
	public static void registerCommand(final Command command) {
		final CommandMap commandMap = getCommandMap();
		commandMap.register(command.getLabel(), BukkitPlugin.getInstance().getName().toLowerCase(), command);

		ValidCore.checkBoolean(command.isRegistered(), "Command /" + command.getLabel() + " could not have been registered properly!");
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
		Platform.runTask(delayTicks, () -> runLocationTask(block.getLocation(), () -> sendBlockChange0(player, block)));
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
		Platform.runTask(delayTicks, () -> runLocationTask(location, () -> sendBlockChange0(player, location, material)));
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
	private static void sendChestAction(final Block block, final int action) {

		final BlockState state = block.getState();
		ValidCore.checkBoolean(state instanceof Chest, "You can only send chest action packet for chests not " + block);

		try {
			if (action == 1)
				((Chest) state).open();
			else
				((Chest) state).close();

		} catch (final NoSuchMethodError t) {
			final Location location = block.getLocation();

			final Class<?> blockClass = Remain.getNMSClass("Block");
			final Class<?> blocks = Remain.getNMSClass("Blocks");

			final Object position = ReflectionUtil.instantiate(Remain.getConstructorNMS("BlockPosition", double.class, double.class, double.class), location.getX(), location.getY(), location.getZ());
			final Object packet = ReflectionUtil.instantiate(Remain.getConstructorNMS("PacketPlayOutBlockAction",
					Remain.getNMSClass("BlockPosition"), blockClass, int.class, int.class), position, ReflectionUtil.getStaticFieldContent(blocks, "CHEST"), 1, action);

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
	public static void sendChestClose(final Block block) {
		sendChestAction(block, 0);
	}

	/**
	 * Broadcast a chest open animation at the given block,
	 * the block must be a chest!
	 *
	 * @param block
	 */
	public static void sendChestOpen(final Block block) {
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

	/*
	 * Whether a toast can be shown by injecting a temporary advancement.
	 *
	 * On Folia and its threaded-region forks (Canvas, etc.) Bukkit.getUnsafe().loadAdvancement() reloads
	 * every online player's advancements from the calling thread while those players keep ticking on their
	 * own region threads, throwing ConcurrentModificationException. There is no thread-safe way to inject
	 * a temporary advancement on such servers, so callers fall back to a chat message instead.
	 */
	private static boolean canShowAdvancementToast() {
		return hasAdvancements && !isFolia;
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
		if (canShowAdvancementToast())
			Platform.runTaskAsync(() -> {
				for (final Player receiver : receivers) {

					// Sleep to mitigate sending not working at once
					CommonCore.sleep(100);

					Platform.runTask(() -> {
						final String colorized = CompChatColor.translateColorCodes(message.apply(receiver));

						if (!colorized.isEmpty()) {
							final AdvancementAccessor accessor = new AdvancementAccessor(colorized, icon, style);

							if (receiver.isOnline())
								accessor.show(receiver);
						}
					});
				}
			});
		else
			for (final Player receiver : receivers) {
				final String colorized = message.apply(receiver);

				if (!colorized.isEmpty())
					Platform.toPlayer(receiver).sendMessage(SimpleComponent.fromMiniAmpersand(colorized));
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
	 * @param style
	 */
	public static void sendToastToAudience(@NonNull final List<FoundationPlayer> receivers, @NonNull final Function<FoundationPlayer, String> message, @NonNull final CompMaterial icon, @NonNull final CompToastStyle style) {
		ValidCore.checkBoolean(!CompMaterial.isAir(icon), "Toast icon cannot be air!");

		if (canShowAdvancementToast())
			Platform.runTaskAsync(() -> {
				for (final FoundationPlayer receiver : receivers) {

					// Sleep to mitigate sending not working at once
					CommonCore.sleep(100);

					Platform.runTask(() -> {
						final String colorized = CompChatColor.translateColorCodes(message.apply(receiver));

						if (!colorized.isEmpty()) {
							final AdvancementAccessor accessor = new AdvancementAccessor(colorized.replace("|", "\n"), icon, style);
							final Player playerReceiver = receiver.getPlayer();

							if (playerReceiver.isOnline())
								accessor.show(playerReceiver.getPlayer());
						}
					});
				}
			});
		else
			for (final FoundationPlayer receiver : receivers) {
				final String messageSpecific = message.apply(receiver);

				if (!messageSpecific.isEmpty())
					receiver.sendMessage(SimpleComponent.fromMiniAmpersand(messageSpecific));
			}
	}

	/**
	 * Send a "toast" notification. This is an advancement notification that cannot be
	 * modified on its first screen. It imposes a slight performance penalty.
	 *
	 * @param receiver
	 * @param message
	 */
	public static void sendToast(final Player receiver, final String message) {
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
	 * Send a "toast" notification. This is an advancement notification that cannot be
	 * modified on its first screen. It imposes a slight performance penalty.
	 *
	 * You can pick the first screen from precreated Minecraft screens here.
	 *
	 * @param receiver
	 * @param message
	 * @param toastStyle
	 */
	public static void sendToast(final Player receiver, final String message, final CompToastStyle toastStyle) {
		sendToast(receiver, message, CompMaterial.BOOK, toastStyle);
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
	public static void sendToast(@NonNull final Player receiver, final String message, @NonNull final CompMaterial icon, @NonNull final CompToastStyle toastStyle) {
		if (message != null && !message.isEmpty()) {
			final String colorized = SimpleComponent.fromMiniAmpersand(message).toLegacySection(Platform.toPlayer(receiver)).replace("|", "\n");

			if (!colorized.isEmpty())
				if (canShowAdvancementToast())
					Platform.runTask(() -> new AdvancementAccessor(colorized, icon, toastStyle).show(receiver));

				else
					receiver.sendMessage(colorized);
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
	public static void setBed(Block initialBlock, final BlockFace facing) {

		if (MinecraftVersion.atLeast(V.v1_13))
			for (final org.bukkit.block.data.type.Bed.Part part : org.bukkit.block.data.type.Bed.Part.values()) {
				initialBlock.setBlockData(Bukkit.createBlockData(CompMaterial.WHITE_BED.getMaterial(), data -> {
					((org.bukkit.block.data.type.Bed) data).setPart(part);
					((org.bukkit.block.data.type.Bed) data).setFacing(facing);
				}));

				initialBlock = initialBlock.getRelative(facing.getOppositeFace());
			}

		else {
			initialBlock = initialBlock.getRelative(facing);

			final Material bedMaterial = ReflectionUtil.lookupEnum(Material.class, "BED_BLOCK");
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
	public static void setBed(final Location initialLocation, final BlockFace facing) {
		setBed(initialLocation.getBlock(), facing);
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
				try {
					entity.customName(SimpleComponent.fromMiniAmpersand(name).toAdventure(null));

				} catch (final NoSuchMethodError err) {
					entity.setCustomName(CompChatColor.translateColorCodes(name));
				}

		} catch (final NoSuchMethodError er) {
			final NBTEntity nbt = new NBTEntity(entity);

			nbt.setInteger("CustomNameVisible", visible ? 1 : 0);

			if (name != null)
				nbt.setString("CustomName", CompChatColor.translateColorCodes(name));
		}
	}

	/**
	 * Sets a data of a block in the world.
	 *
	 * @param block
	 * @param data
	 */
	public static void setData(final Block block, final int data) {
		if (MinecraftVersion.atLeast(V.v1_13)) {
			final org.bukkit.block.data.BlockData blockData = block.getBlockData();

			if (blockData instanceof org.bukkit.block.data.Ageable) {
				((org.bukkit.block.data.Ageable) blockData).setAge(data);
				block.setBlockData(blockData, true);
			} else
				block.setBlockData(Bukkit.getUnsafe().fromLegacy(block.getType(), (byte) data), true);

		} else {
			try {
				Block.class.getMethod("setData", byte.class).invoke(block, (byte) data);
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();
			}
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
			CommonCore.error(t, "Game rule " + gameRule + " not found.");
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
	public static void setInvisible(final Object entity, final boolean invisible) {
		if (entity instanceof LivingEntity && MinecraftVersion.atLeast(V.v1_16))
			((LivingEntity) entity).setInvisible(invisible);

		else {
			final Object nmsEntity = entity.getClass().toString().contains("net.minecraft.server") ? entity : entity instanceof LivingEntity ? getHandleEntity(entity) : null;
			ValidCore.checkNotNull(nmsEntity, "setInvisible requires either a LivingEntity or a NMS Entity, got: " + entity.getClass());
			final Method setInvisible = ReflectionUtil.getMethod(nmsEntity.getClass(), "setInvisible", boolean.class);

			// https://www.spigotmc.org/threads/how-do-i-make-an-entity-go-invisible-without-using-potioneffects.321227/
			Platform.runTask(2, () -> {
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
	public static void setPages(final Object metaObject, final List<SimpleComponent> pages) {
		setPages(metaObject, pages.toArray(new SimpleComponent[pages.size()]));
	}

	/**
	 * Attempts to set the book pages from the given list.
	 *
	 * @param metaObject
	 * @param pages
	 */
	public static void setPages(final Object metaObject, final SimpleComponent... pages) {
		ValidCore.checkBoolean(metaObject instanceof org.bukkit.inventory.meta.BookMeta, "Object must be BookMeta, got: " + metaObject.getClass());

		final org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) metaObject;

		try {
			final List<net.kyori.adventure.text.Component> adventurePages = new ArrayList<>();

			for (final SimpleComponent page : pages)
				adventurePages.add(page.toAdventure());

			meta.pages(adventurePages);

		} catch (final NoSuchMethodError noAdventureError) {
			try {
				final List<BaseComponent[]> spigotPages = new ArrayList<>();

				for (final SimpleComponent component : pages)
					try {
						spigotPages.add(component.toBungee(null, MinecraftVersion.atLeast(V.v1_16)));

					} catch (final Throwable t) {
						CommonCore.error(t, "Failed to turn simple component into bungee component: " + component);
					}

				meta.spigot().setPages(spigotPages);

			} catch (final NoSuchMethodError iLikeDinosaursError) {
				meta.setPages(CommonCore.convertArrayToList(pages, SimpleComponent::toLegacySection));
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
	public static void setPotion(final ItemStack item, final PotionEffectType type, final long durationTicks, final int level) {
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
		setTypeAndData(block, CompMaterial.fromLegacy(ReflectionUtil.getEnumName(material), data));
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
	public static boolean setViewDistance(final Player player, final int viewDistanceChunks) {

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
	 * Sends an actionbar packet to the player
	 * Used for legacy MC versions.
	 *
	 * @param player
	 * @param legacy
	 */
	public static void sendActionBarLegacyPacket(final Player player, final String legacy) {
		ValidCore.checkBoolean(MinecraftVersion.olderThan(V.v1_13), "This method is unsupported on MC 1.13 and later");

		sendLegacyChat(player, convertLegacyToIChatBase(legacy), (byte) 2);
	}

	/**
	 * Sends a chat packet to the player
	 *
	 * @param player
	 * @param json
	 */
	public static void sendLegacyChat(final Player player, final String json) {
		sendLegacyChat(player, convertJsonToIChatBase(json), (byte) 1);
	}

	private static void sendLegacyChat(final Player player, final Object iChatBaseComponent, final byte type) {
		try {
			final Object packet;

			if (MinecraftVersion.atLeast(V.v1_12)) {
				final Class<?> chatMessageTypeEnum = Remain.getNMSClass("ChatMessageType", "net.minecraft.network.chat.ChatMessageType");

				packet = chatPacketConstructor.newInstance(iChatBaseComponent, chatMessageTypeEnum.getMethod("a", byte.class).invoke(null, type));

			} else
				packet = chatPacketConstructor.newInstance(iChatBaseComponent, type);

			Remain.sendPacket(player, packet);

		} catch (final ReflectiveOperationException ex) {
			CommonCore.error(ex, "Failed to send message packet type " + type + " to " + player.getName() + ", message: " + iChatBaseComponent);
		}
	}

	/**
	 * Send tablist to player
	 *
	 * @param player
	 * @param header
	 * @param footer
	 */
	public static void sendTablistLegacyPacket(final Player player, final String header, final String footer) {
		ValidCore.checkBoolean(MinecraftVersion.olderThan(V.v1_13), "This method is unsupported on MC 1.13 and later");

		try {
			if (tabConstructor == null)
				return;

			final Object headerIChatBase = convertLegacyToIChatBase(header);
			final Object packet;

			if (MinecraftVersion.equals(V.v1_12)) {
				packet = tabConstructor.newInstance();

				final Field fieldHeader = packet.getClass().getDeclaredField("a");
				fieldHeader.setAccessible(true);
				fieldHeader.set(packet, headerIChatBase);

			} else
				packet = tabConstructor.newInstance(headerIChatBase);

			if (footer != null) {
				final Object footerIChatBase = convertLegacyToIChatBase(footer);

				final Field fieldFooter = packet.getClass().getDeclaredField("b"); // footer
				fieldFooter.setAccessible(true);
				fieldFooter.set(packet, footerIChatBase);
			}

			Remain.sendPacket(player, packet);

		} catch (final ReflectiveOperationException ex) {
			CommonCore.error(ex, "Failed to send tablist to " + player.getName() + ", title: " + header + " " + footer);
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
	 * Spawns a falling block.
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
	 */
	public static Item spawnItem(final Location location, final ItemStack item, final Consumer<Item> modifier) {
		try {

			final Class<?> nmsWorldClass = MinecraftVersion.atLeast(V.v1_21)
					? ReflectionUtil.lookupClass("net.minecraft.world.level.Level")
					: Remain.getNMSClass("World", "net.minecraft.world.level.World");
			final Class<?> nmsStackClass = Remain.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Class<?> nmsEntityClass = Remain.getNMSClass("Entity", "net.minecraft.world.entity.Entity");
			final Class<?> nmsItemClass = MinecraftVersion.atLeast(V.v1_21)
					? ReflectionUtil.lookupClass("net.minecraft.world.entity.item.ItemEntity")
					: Remain.getNMSClass("EntityItem", "net.minecraft.world.entity.item.EntityItem");

			final Constructor<?> entityConstructor = nmsItemClass.getConstructor(nmsWorldClass, double.class, double.class, double.class, nmsStackClass);

			final Object nmsWorld = location.getWorld().getClass().getMethod("getHandle").invoke(location.getWorld());
			final Method asNmsCopy = Remain.getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			final Object nmsEntity = entityConstructor.newInstance(nmsWorld, location.getX(), location.getY(), location.getZ(), asNmsCopy.invoke(null, item));

			final Class<?> craftItemClass = Remain.getOBCClass("entity.CraftItem");
			final Class<?> craftServerClass = Remain.getOBCClass("CraftServer");

			final Object bukkitItem = craftItemClass.getConstructor(craftServerClass, nmsItemClass).newInstance(Bukkit.getServer(), nmsEntity);
			ValidCore.checkBoolean(bukkitItem instanceof Item, "Failed to make an dropped item, got " + bukkitItem.getClass().getSimpleName());

			// Default delay to 750ms
			try {
				((Item) bukkitItem).setPickupDelay(15);
			} catch (final Throwable t) {
				// unsupported
			}

			if (modifier != null)
				modifier.accept((Item) bukkitItem);

			{ // add to the world + call event
				final String addEntityMethod = MinecraftVersion.atLeast(V.v1_21) ? "addEntityToWorld" : "addEntity";
				final Method addEntity = location.getWorld().getClass().getMethod(addEntityMethod, nmsEntityClass, SpawnReason.class);
				addEntity.invoke(location.getWorld(), nmsEntity, SpawnReason.CUSTOM);
			}

			return (Item) bukkitItem;

		} catch (final ReflectiveOperationException ex) {
			CommonCore.error(ex, "Error spawning item " + item.getType() + " at " + location);

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
	public static HoverEvent<?> convertItemStackToHoverEvent(final ItemStack item) {
		ValidCore.checkBoolean(item != null, "Hover item must not be null");

		if (CompMaterial.isAir(item))
			return HoverEvent.showText(Component.text(Lang.plain("part-air")));

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
			key = Key.key(ReflectionUtil.getEnumName(material).toLowerCase());

		return HoverEvent.showItem(key, item.getAmount(), binaryTagHolder);

	}

	/**
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string
	 * for sending with components.
	 *
	 * @param item the item to convert
	 * @return the Json string representation of the item
	 */
	public static String convertItemStackToJson(final ItemStack item) {
		// ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
		final Class<?> craftItemstack = Remain.getOBCClass("inventory.CraftItemStack");
		final Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemstack, "asNMSCopy", ItemStack.class);

		ValidCore.checkNotNull(asNMSCopyMethod, "Unable to find " + craftItemstack + "#asNMSCopy() method for server version " + Bukkit.getBukkitVersion());

		// NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
		final Class<?> nmsItemStack = Remain.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
		final Object nmsItemStackObj = ReflectionUtil.invoke(asNMSCopyMethod, null, item);

		if (MinecraftVersion.atLeast(V.v1_20, 5)) {
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
			final Class<?> nbtTagCompound = Remain.getNMSClass("NBTTagCompound", "net.minecraft.nbt.NBTTagCompound");
			final Method saveItemstackMethod = ReflectionUtil.getMethod(nmsItemStack, MinecraftVersion.equals(V.v1_18) || MinecraftVersion.equals(V.v1_19) || MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() < 5 ? "b" : "save", nbtTagCompound);

			ValidCore.checkNotNull(saveItemstackMethod, "Unable to find " + nmsItemStack + "#save() method for server version " + Bukkit.getBukkitVersion());

			final Object nmsNbtTagCompoundObj = ReflectionUtil.instantiate(nbtTagCompound);
			final Object itemAsJsonObject = ReflectionUtil.invoke(saveItemstackMethod, nmsItemStackObj, nmsNbtTagCompoundObj);

			return itemAsJsonObject.toString();
		}
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
			final Field knownCommandsFields = SimpleCommandMap.class.getDeclaredField("knownCommands");
			knownCommandsFields.setAccessible(true);

			final Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsFields.get(getCommandMap());

			knownCommands.remove(label);

			if (command != null && removeAliases)
				for (final String alias : command.getAliases())
					knownCommands.remove(alias);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Failed to unregister command /" + label);
		}
	}

	/**
	 * Update the player's inventory title without closing the window
	 *
	 * @param player the player
	 * @param title  the new title
	 */
	public static void updateInventoryTitle(final Player player, String title) {
		try {
			final Object view = ReflectionUtil.invoke("getOpenInventory", player);
			final Method setTitle = ReflectionUtil.getMethod(view.getClass(), "setTitle", String.class);

			if (setTitle == null)
				throw new NoSuchMethodError();

			ReflectionUtil.invoke(setTitle, view, CompChatColor.translateColorCodes(title));

		} catch (final NoSuchMethodError err) {

			final Inventory topInventory = Remain.getTopInventoryFromOpenInventory(player);

			try {
				if (MinecraftVersion.atLeast(V.v1_17)) {
					final String nmsVersion = Remain.getNmsVersion();

					final boolean is1_17 = MinecraftVersion.equals(V.v1_17);
					final boolean is1_18 = MinecraftVersion.equals(V.v1_18);
					final boolean is1_19 = MinecraftVersion.equals(V.v1_19);

					final Object nmsPlayer = Remain.getHandleEntity(player);
					final Object chatComponent = convertLegacyToIChatBase(CompChatColor.translateColorCodes(title));

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
					final Constructor<?> chatMessageConst = Remain.getNMSClass("ChatMessage", "net.minecraft.network.chat.ChatMessage").getConstructor(String.class, Object[].class);
					final Object chatMessage = chatMessageConst.newInstance(CompChatColor.translateColorCodes(title), new Object[0]);

					if (MinecraftVersion.newerThan(V.v1_13)) {
						final int inventorySize = topInventory.getSize() / 9;

						if (inventorySize < 1 || inventorySize > 6) {
							CommonCore.log("Cannot update title for " + player.getName() + " as his inventory has non typical size: " + inventorySize + " rows");

							return;
						}

						final Class<?> containersClass = Remain.getNMSClass("Containers", "net.minecraft.world.inventory.Containers");
						final Constructor<?> packetConst = Remain.getNMSClass("PacketPlayOutOpenWindow", "net.minecraft.network.protocol.game.PacketPlayOutOpenWindow")
								.getConstructor(/*windowID*/int.class, /*containers*/containersClass, /*msg*/Remain.getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"));

						final String containerName = "GENERIC_9X" + inventorySize;

						final Object container = containersClass.getField(containerName).get(null);

						packetOpenWindow = packetConst.newInstance(windowId, container, chatMessage);

					} else {
						final Constructor<?> packetConst = Remain.getNMSClass("PacketPlayOutOpenWindow", "N/A").getConstructor(int.class, String.class, Remain.getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"), int.class);

						packetOpenWindow = packetConst.newInstance(windowId, "minecraft:chest", chatMessage, topInventory.getSize());
					}
				} else {
					final Constructor<?> openWindow = ReflectionUtil.getConstructor(
							Remain.getNMSClass("PacketPlayOutOpenWindow", "N/A"), int.class, int.class, String.class, int.class, boolean.class);

					packetOpenWindow = ReflectionUtil.instantiate(openWindow, windowId, 0, CompChatColor.translateColorCodes(title), topInventory.getSize(), true);
				}

				sendPacket(player, packetOpenWindow);
				entityPlayer.getClass().getMethod("updateInventory", Remain.getNMSClass("Container", "net.minecraft.world.inventory.Container")).invoke(entityPlayer, activeContainer);

			} catch (final ReflectiveOperationException ex) {
				CommonCore.error(ex, "Error updating " + player.getName() + " inventory title to '" + title + "'");
			}
		}
	}

	/**
	 * Runs the task on the given entity's owning thread on Folia/ShreddedPaper,
	 * or falls back to {@link #runTask(int, Runnable)} on non-Folia servers.
	 *
	 * @param entity     the entity whose scheduler to use
	 * @param delayTicks the delay in ticks (0 = next tick)
	 * @param timer      the runnable to execute
	 */
	public static void runEntityTask(final Entity entity, final int delayTicks, final Runnable timer) {
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return;

		if (!isFolia || entity == null) {
			runTask(delayTicks, runnable);

			return;
		}

		try {
			final Object scheduler = entityGetScheduler.invoke(entity);

			if (delayTicks <= 0)
				entitySchedulerRun.invoke(scheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), (Runnable) null);
			else
				entitySchedulerRunDelayed.invoke(scheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), (Runnable) null, (long) delayTicks);

		} catch (final Throwable t) {
			CommonCore.error(t, "Failed to schedule entity task for " + entity + ", falling back to global scheduler");

			runTask(delayTicks, runnable);
		}
	}

	/**
	 * Runs the task on the region owning the given location on Folia,
	 * or immediately on non-Folia servers.
	 *
	 * @param location the location whose owning region scheduler to use
	 * @param timer    the runnable to execute
	 */
	public static void runLocationTask(final Location location, final Runnable timer) {
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return;

		if (!isFolia) {
			runnable.run();

			return;
		}

		Bukkit.getRegionScheduler().run(BukkitPlugin.getInstance(), location, task -> runnable.run());
	}

	/**
	 * Runs the task even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param timer
	 * @return the task or null
	 */
	public static Task runTask(final int delayTicks, final Runnable timer) {
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle;

			if (delayTicks == 0)
				taskHandle = ReflectionUtil.invoke(execute, foliaScheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run());
			else
				taskHandle = ReflectionUtil.invoke(runDelayed, foliaScheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), delayTicks);

			final SimpleBukkitTask task = SimpleBukkitTask.fromFolia(cancel, taskHandle);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(task);

			return task;
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskLater(BukkitPlugin.getInstance(), delayTicks);

			else
				task = Bukkit.getScheduler().runTaskLater(BukkitPlugin.getInstance(), runnable, delayTicks);

			final SimpleBukkitTask simpleTask = SimpleBukkitTask.fromBukkit(task);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			final SimpleBukkitTask simpleTask = SimpleBukkitTask.fromBukkit(Bukkit.getScheduler().scheduleSyncDelayedTask(BukkitPlugin.getInstance(), runnable, delayTicks), false);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(simpleTask);

			return simpleTask;
		}
	}

	/**
	 * Runs the task async even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param timer
	 * @return the task or null
	 */
	public static Task runTaskAsync(final int delayTicks, final Runnable timer) {
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle;

			if (delayTicks == 0)
				taskHandle = ReflectionUtil.invoke(asyncExecute, foliaAsyncScheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run());
			else
				taskHandle = ReflectionUtil.invoke(asyncRunDelayed, foliaAsyncScheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), delayTicks * 50L, TimeUnit.MILLISECONDS);

			final SimpleBukkitTask task = SimpleBukkitTask.fromFolia(cancel, taskHandle);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(task);

			return task;
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = delayTicks == 0
						? ((BukkitRunnable) runnable).runTaskAsynchronously(BukkitPlugin.getInstance())
						: ((BukkitRunnable) runnable).runTaskLaterAsynchronously(BukkitPlugin.getInstance(), delayTicks);

			else
				task = delayTicks == 0
						? Bukkit.getScheduler().runTaskAsynchronously(BukkitPlugin.getInstance(), runnable)
						: Bukkit.getScheduler().runTaskLaterAsynchronously(BukkitPlugin.getInstance(), runnable, delayTicks);

			final SimpleBukkitTask simpleTask = SimpleBukkitTask.fromBukkit(task);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			final SimpleBukkitTask task = SimpleBukkitTask.fromBukkit(Bukkit.getScheduler().scheduleAsyncDelayedTask(BukkitPlugin.getInstance(), runnable, delayTicks), true);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(task);

			return task;
		}
	}

	/**
	 * Runs the task timer even if the plugin is disabled.
	 *
	 * @param delayTicks  the delay before first run
	 * @param repeatTicks the delay between each run
	 * @param timer        the task
	 * @return the bukkit task or null if error
	 */
	public static Task runTaskTimer(final int delayTicks, final int repeatTicks, final Runnable timer) {
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle = ReflectionUtil.invoke(runAtFixedRate, foliaScheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), Math.max(1, delayTicks), repeatTicks);

			final SimpleBukkitTask task = SimpleBukkitTask.fromFolia(cancel, taskHandle);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(task);

			return task;
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskTimer(BukkitPlugin.getInstance(), delayTicks, repeatTicks);

			else
				task = Bukkit.getScheduler().runTaskTimer(BukkitPlugin.getInstance(), runnable, delayTicks, repeatTicks);

			final SimpleBukkitTask simpleTask = SimpleBukkitTask.fromBukkit(task);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			final SimpleBukkitTask task = SimpleBukkitTask.fromBukkit(Bukkit.getScheduler().scheduleSyncRepeatingTask(BukkitPlugin.getInstance(), runnable, delayTicks, repeatTicks), false);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(task);

			return task;
		}
	}

	/**
	 * Runs the task timer async even if the plugin is disabled.
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param timer
	 * @return
	 */
	public static Task runTaskTimerAsync(final int delayTicks, final int repeatTicks, final Runnable timer) {
		final Runnable runnable = CommonCore.wrapRunnableInExceptionCatcher(timer);

		if (CommonCore.runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle = ReflectionUtil.invoke(asyncRunAtFixedRate, foliaAsyncScheduler, BukkitPlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), Math.max(1, delayTicks) * 50L, repeatTicks * 50L, TimeUnit.MILLISECONDS);

			final SimpleBukkitTask task = SimpleBukkitTask.fromFolia(cancel, taskHandle);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(task);

			return task;
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskTimerAsynchronously(BukkitPlugin.getInstance(), delayTicks, repeatTicks);

			else
				task = Bukkit.getScheduler().runTaskTimerAsynchronously(BukkitPlugin.getInstance(), runnable, delayTicks, repeatTicks);

			final SimpleBukkitTask simplTask = SimpleBukkitTask.fromBukkit(task);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(simplTask);

			return simplTask;

		} catch (final NoSuchMethodError err) {
			final SimpleBukkitTask task = SimpleBukkitTask.fromBukkit(Bukkit.getScheduler().scheduleAsyncRepeatingTask(BukkitPlugin.getInstance(), runnable, delayTicks, repeatTicks), true);

			if (timer instanceof SimpleRunnable)
				((SimpleRunnable) timer).setupTask(task);

			return task;
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
			final NBTEntity nbt = new NBTEntity(entity);

			nbt.removeKey("CustomNameVisible");
			nbt.removeKey("CustomName");
		}
	}

	/**
	 * Teleports the entity to the given location, using teleportAsync on Folia
	 * where a synchronous teleport throws when crossing region threads.
	 *
	 * @param entity
	 * @param location
	 */
	public static void teleport(final Entity entity, final Location location) {
		if (isFolia())
			entity.teleportAsync(location);
		else
			entity.teleport(location);
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
		Platform.runTask(delayTicks, () -> {
			try {
				player.spigot().respawn();

			} catch (final NoSuchMethodError err) {
				try {
					final Object respawnEnum = Remain.getNMSClass("EnumClientCommand", "N/A").getEnumConstants()[0];
					final Constructor<?>[] constructors = Remain.getNMSClass("PacketPlayInClientCommand", "N/A").getConstructors();

					for (final Constructor<?> constructor : constructors) {
						final Class<?>[] args = constructor.getParameterTypes();
						if (args.length == 1 && args[0] == respawnEnum.getClass()) {
							final Object packet = Remain.getNMSClass("PacketPlayInClientCommand", "N/A").getConstructor(args).newInstance(respawnEnum);

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
	 * Removes the entity with its passengers and the vehicle, if any,
	 * as well as removes the NPC if it is a Citizens NPC
	 *
	 * @param entity
	 */
	public static void removeEntityWithPassengersAndNPC(final Entity entity) {
		EntityUtil.removeVehiclesAndPassengers(entity);

		if (HookManager.isCitizensLoaded())
			HookManager.destroyNPC(entity);

		if (!(entity instanceof Player))
			entity.remove();
	}

	// ----------------------------------------------------------------------------------------------------
	// NMS-related
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Find a class automatically for older MC version (such as type EntityPlayer for oldName
	 * and we automatically find the proper NMS import) or if MC 1.17+ is used then type
	 * the full class path such as net.minecraft.server.level.EntityPlayer and we use that instead.
	 *
	 * @param oldName
	 * @param fullName1_17
	 * @return
	 */
	public static Class<?> getNMSClass(final String oldName, final String fullName1_17) {
		return MinecraftVersion.atLeast(V.v1_17) ? ReflectionUtil.lookupClass(fullName1_17) : getNMSClass(oldName);
	}

	/**
	 * Find a class in net.minecraft.server package, adding the version
	 * automatically (or empty on 1.20.5+).
	 *
	 * @deprecated Minecraft 1.17+ has a different path name,
	 *             use {@link #getNMSClass(String, String)} instead
	 *
	 * @param name
	 * @return
	 */
	@Deprecated
	public static Class<?> getNMSClass(final String name) {
		String safeguardPrefix = Remain.getNmsVersion();

		if (!safeguardPrefix.isEmpty())
			safeguardPrefix += ".";

		return ReflectionUtil.lookupClass(NMS + "." + safeguardPrefix + name);
	}

	/**
	 * Find a class in org.bukkit.craftbukkit package, adding the version
	 * automatically (or empty on 1.20.5+).
	 *
	 * @param name
	 * @return
	 */
	public static Class<?> getOBCClass(final String name) {
		String version = Remain.getNmsVersion();

		if (!version.isEmpty())
			version += ".";

		return ReflectionUtil.lookupClass(CRAFTBUKKIT + "." + version + name);
	}

	/**
	 * Return a constructor for the given NMS class name (such as EntityZombie).
	 *
	 * @param nmsClassPath
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructorNMS(@NonNull final String nmsClassPath, final Class<?>... params) {
		return ReflectionUtil.getConstructor(getNMSClass(nmsClassPath), params);
	}

	// ----------------------------------------------------------------------------------------------------
	// Skull-related
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Set the base64-encoded texture data to a SkullMeta object, typically for use in an item.
	 *
	 * @param meta The SkullMeta instance to modify.
	 * @param base64 The base64 string containing the texture data, typically related to a player’s skin.
	 * @return
	 */
	public static SkullMeta setSkullMetaBase64(final SkullMeta meta, final String base64) {

		if (hasPaperProfile) {
			meta.setPlayerProfile(createPaperProfileFromBase64(base64));

			return meta;
		}

		try {
			if (metaSetProfileMethod == null) {
				metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", ReflectionUtil.lookupClass("com.mojang.authlib.GameProfile"));

				metaSetProfileMethod.setAccessible(true);
			}

			metaSetProfileMethod.invoke(meta, getProfileFromBase64(base64));

		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {

			// If in an older API where there is no setProfile method, we set the profile field directly.
			try {
				if (metaProfileField == null) {
					metaProfileField = meta.getClass().getDeclaredField("profile");
					metaProfileField.setAccessible(true);
				}
				metaProfileField.set(meta, getProfileFromBase64(base64));

			} catch (NoSuchFieldException | IllegalAccessException ex2) {
				ex2.printStackTrace();
			}
		}

		return meta;
	}

	private static com.destroystokyo.paper.profile.PlayerProfile createPaperProfileFromBase64(final String base64) {
		final UUID uuid = new UUID(base64.substring(base64.length() - 20).hashCode(), base64.substring(base64.length() - 10).hashCode());
		final com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(uuid, "aaaaa");

		profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", base64));

		return profile;
	}

	/**
	 * Get a Minecraft GameProfile object based on a base64-encoded string,
	 * formatted with random UUID derived from the base64 string.
	 *
	 * @param base64 The base64 string containing the texture data.
	 * @return A GameProfile object containing the profile and skin data.
	 */
	public static Object getProfileFromBase64(final String base64) {
		final UUID uuid = new UUID(base64.substring(base64.length() - 20).hashCode(), base64.substring(base64.length() - 10).hashCode());

		try {
			final Class<?> gameProfileClass = ReflectionUtil.lookupClass("com.mojang.authlib.GameProfile");
			final Class<?> propertyClass = ReflectionUtil.lookupClass("com.mojang.authlib.properties.Property");

			final Object propertyInstance = propertyClass.getConstructor(String.class, String.class).newInstance("textures", base64);
			Object fakeProfileInstance;

			try {
				fakeProfileInstance = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(uuid, "aaaaa");

				final Method getProperties = fakeProfileInstance.getClass().getMethod("getProperties");
				final Object propertyMap = getProperties.invoke(fakeProfileInstance);

				propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", propertyInstance);

			} catch (final ReflectiveOperationException ex2) {
				final Class<?> propertyMapClass = ReflectionUtil.lookupClass("com.mojang.authlib.properties.PropertyMap");
				final com.google.common.collect.Multimap<String, Object> mutableMap = com.google.common.collect.LinkedHashMultimap.create();

				mutableMap.put("textures", propertyInstance);

				final Object newPropertyMap = propertyMapClass.getConstructor(com.google.common.collect.Multimap.class).newInstance(mutableMap);
				fakeProfileInstance = gameProfileClass.getConstructor(UUID.class, String.class, propertyMapClass).newInstance(uuid, "aaaaa", newPropertyMap);
			}

			if (MinecraftVersion.atLeast(V.v1_21, 1)) {
				final Class<?> resolvableProfileClass = ReflectionUtil.lookupClass("net.minecraft.world.item.component.ResolvableProfile");
				final Object fakeResolvableProfileInstance = resolvableProfileClass.getConstructor(gameProfileClass).newInstance(fakeProfileInstance);

				return fakeResolvableProfileInstance;
			} else
				return fakeProfileInstance;

		} catch (final ReflectiveOperationException ex) {
			CommonCore.throwError(ex);

			return null;
		}
	}

	/**
	 * Convert a skin texture URL into a base64-encoded string, suitable for use in setting custom player heads.
	 *
	 * @param url The URL of the skin texture (must start with 'http://' or 'https://').
	 * @return The base64-encoded string of the texture data.
	 * @throws IllegalArgumentException If the URL does not start with 'http://' or 'https://'.
	 */
	public static String convertSkinTextureUrlToBase64(final String url) {
		ValidCore.checkBoolean(url.startsWith("http://") || url.startsWith("https://"), "URL for skull must start with http:// or https://, given: " + url);

		final URI actualUrl;

		try {
			actualUrl = new URI(url);

		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}

		final String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + actualUrl.toString() + "\"}}}";
		return Base64.getEncoder().encodeToString(toEncode.getBytes());
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
	 * Return true if the calling thread is a server tick thread.
	 *
	 * On Folia, this is any region scheduler tick thread. On modern Paper, this is the main thread.
	 * On legacy Spigot/CraftBukkit/1.8.x where Moonrise is absent, falls back to Bukkit.isPrimaryThread().
	 *
	 * Use this in preference to Bukkit.isPrimaryThread() when you need to detect tick threads on Folia,
	 * because Folia has no single "primary" thread.
	 *
	 * @return
	 */
	public static boolean isTickThread() {
		if (isTickThreadMethod != null) {
			try {
				return (boolean) isTickThreadMethod.invoke(null);

			} catch (final ReflectiveOperationException ex) {
				throw new RuntimeException("Failed to invoke TickThread.isTickThread()", ex);
			}
		}

		return Bukkit.isPrimaryThread();
	}

	/**
	 * Return true if the calling thread currently owns the region that ticks the given entity.
	 * On non-Folia this is effectively {@link Bukkit#isPrimaryThread()} because a single tick
	 * thread owns every entity; on Folia it is a precise per-region check.
	 *
	 * @param entity the entity whose region ownership to test, or null
	 * @return true when it is safe to touch the entity's NMS state from the current thread
	 */
	public static boolean isTickThreadFor(final org.bukkit.entity.Entity entity) {
		if (entity == null || !isFolia)
			return true;

		if (isTickThreadForEntityMethod == null || getHandleRawMethod == null)
			return isTickThread();

		try {
			final Object nmsEntity = getHandleRawMethod.invoke(entity);

			return (boolean) isTickThreadForEntityMethod.invoke(null, nmsEntity);

		} catch (final ReflectiveOperationException ex) {
			return isTickThread();
		}
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
	 * Return true if the CommandSender implements Audience.
	 *
	 * @return
	 */
	public static boolean isCommandSenderAudience() {
		return isCommandSenderAudience;
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
	 * Returns true if the server's Adventure api exposes TranslatableComponent#arguments() (Adventure 4.15.0+).
	 *
	 * When false, serializing a translatable component (such as an item name on a shop sign) throws
	 * NoSuchMethodError because the bundled Adventure serializers call arguments() which the older api lacks.
	 *
	 * @return
	 */
	public static boolean hasAdventureTranslatableArguments() {
		return hasAdventureTranslatableArguments;
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

	/**
	 * Return true if the Player class has the openVirtualSign method (Paper 1.21.7+)
	 *
	 * @return
	 */
	public static boolean hasPlayerOpenVirtualSignMethod() {
		return hasPlayerOpenVirtualSignMethod;
	}
}

/**
 * Implements a Task for both Bukkit and Folia.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class SimpleBukkitTask implements Task {

	@Getter
	private final int taskId;

	@Getter
	private final boolean sync;

	private final Method foliaCancelMethod;
	private final Object foliaTaskInstance;

	@Getter
	private boolean cancelled = false;

	@Override
	public void cancel() {
		if (Remain.isFolia())
			ReflectionUtil.invoke(this.foliaCancelMethod, this.foliaTaskInstance);

		else
			Bukkit.getScheduler().cancelTask(this.taskId);

		this.cancelled = true;
	}

	static SimpleBukkitTask fromBukkit(final BukkitTask task) {
		return new SimpleBukkitTask(task.getTaskId(), task.isSync(), null, null);
	}

	static SimpleBukkitTask fromBukkit(final int taskId, final boolean sync) {
		return taskId >= 0 ? null : new SimpleBukkitTask(taskId, sync, null, null);
	}

	static SimpleBukkitTask fromFolia(final Method foliaCancelMethod, final Object foliaTaskInstance) {
		return new SimpleBukkitTask(0, false, foliaCancelMethod, foliaTaskInstance);
	}

	@Override
	public Plugin getOwner() {
		return BukkitPlugin.getInstance();
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

	AdvancementAccessor(final String message, final CompMaterial icon, final CompToastStyle toastStyle) {
		this.key = new NamespacedKey(BukkitPlugin.getInstance(), UUID.randomUUID().toString());
		this.message = message;
		this.icon = resolveItemIcon(icon).toString().toLowerCase();
		this.toastStyle = toastStyle;
	}

	/*
	 * Block-only materials (BLACK_WALL_BANNER, PISTON_HEAD, POTTED_*) exist in the
	 * Material enum but have no item registry entry, so passing their key to
	 * Bukkit.getUnsafe().loadAdvancement() makes the codec throw JsonParseException
	 * and crashes the toast task. Fall back to BOOK with a warning so the user fixes
	 * their config. Material#isItem() only exists on 1.13+, older versions had a
	 * lenient codec and cannot hit this crash.
	 */
	private static CompMaterial resolveItemIcon(final CompMaterial icon) {
		if (!MinecraftVersion.atLeast(V.v1_13) || icon.getMaterial().isItem())
			return icon;

		CommonCore.warning("Toast icon '" + icon + "' is a block-only material with no item form, falling back to BOOK. Pick a material that can exist as an item.");
		return CompMaterial.BOOK;
	}

	public void show(final Player player) {
		this.loadAdvancement();
		this.grantAdvancement(player);

		Platform.runTask(10, () -> {
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

		if (MinecraftVersion.atLeast(V.v1_20, 5))
			icon.addProperty("id", this.icon);
		else
			icon.addProperty("item", this.icon);

		final JsonObject display = new JsonObject();
		display.add("icon", icon);

		final SimpleComponent titleComponent = SimpleComponent.fromMiniAmpersand(this.message);
		final JsonElement titleElement = CommonCore.GSON.fromJson(titleComponent.toAdventureJson(false), JsonElement.class);
		display.add("title", titleElement);

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

		return CommonCore.GSON.toJson(json);
	}

	private void grantAdvancement(final Player player) {
		final Advancement adv = this.getAdvancement();
		final AdvancementProgress progress = player.getAdvancementProgress(adv);

		if (!progress.isDone())
			progress.getRemainingCriteria().forEach(crit -> progress.awardCriteria(crit));
	}

	private void revokeAdvancement(final Player player) {
		final Advancement adv = this.getAdvancement();
		final AdvancementProgress prog = player.getAdvancementProgress(adv);

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
	public static void setPotion(final ItemStack item, final PotionEffectType type, final long durationTicks, final int level) {
		ValidCore.checkBoolean(item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta, "Can only use setPotion for items with PotionMeta not: " + item.getItemMeta());

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
		if (MinecraftVersion.olderThan(V.v1_9))
			if (item.getData().getData() == 0) {
				final List<String> lore = new ArrayList<>();
				final String potionLine = CompChatColor.translateColorCodes("<gray>" + ChatUtil.capitalizeFully(type.getName()) + " (" + TimeUtil.formatTimeColon(durationTicks / 20) + ")");

				lore.add(potionLine);

				if (meta.getLore() != null)
					for (final String otherLore : meta.getLore())
						if (!otherLore.contains(potionLine))
							lore.add(otherLore);

				item.getData().setData((byte) 45);

				meta.setDisplayName(CompChatColor.translateColorCodes("<reset>Potion Of " + ChatUtil.capitalizeFully(type.getName())));
				meta.setLore(lore);
			}

		//meta.setMainEffect(type);
		meta.addCustomEffect(new PotionEffect(type, durationTicks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) durationTicks, level - 1), true);

		item.setItemMeta(meta);
	}
}
