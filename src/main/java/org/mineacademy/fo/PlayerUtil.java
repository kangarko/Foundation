package org.mineacademy.fo;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.Statistic.Type;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.jsonsimple.JSONObject;
import org.mineacademy.fo.jsonsimple.JSONParser;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompAttribute;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for managing players.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerUtil {

	/**
	 * Spigot 1.9, for whatever reason, decided to merge the armor and main player inventories without providing a way
	 * to access the main inventory. There's lots of ugly code in here to work around that.
	 */
	public static final int USABLE_PLAYER_INV_SIZE = 36;

	/**
	 * Stores block faces to use for later conversion
	 */
	private static final BlockFace[] FACE_AXIS = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	private static final BlockFace[] FACE_RADIAL = { BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST };

	/**
	 * Stores a list of currently pending title animation tasks to restore the tile to its original one
	 */
	private static final Map<UUID, BukkitTask> titleRestoreTasks = new ConcurrentHashMap<>();

	/**
	 * Stores temporarily saved player inventories, their health, attributes and other states
	 */
	private static final Map<UUID, SerializedMap> storedPlayerStates = new HashMap<>();

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Kicks the player on the main thread with a colorized message
	 *
	 * @param player
	 * @param message
	 */
	public static void kick(final Player player, final String... message) {
		Common.runLater(() -> player.kickPlayer(Common.colorize(message)));
	}

	/**
	 * Return the player's connection delay, ping, in milliseconds
	 *
	 * @param player
	 * @return
	 */
	public static int getPing(final Player player) {
		return Remain.getPing(player);
	}

	/**
	 * Converts where the player is looking into a block face
	 * Source: https://bukkit.org/threads/400099/
	 *
	 * @param player
	 * @return
	 */
	public static BlockFace getFacing(Player player) {
		return getFacing(player.getLocation().getYaw(), false);
	}

	/**
	 * Converts where the player is looking into a block face
	 * Source: https://bukkit.org/threads/400099/
	 *
	 * @param player
	 * @param useSubDirections
	 * @return
	 */
	public static BlockFace getFacing(Player player, boolean useSubDirections) {
		return getFacing(player.getLocation().getYaw(), useSubDirections);
	}

	/**
	 * Converts the given yaw into a block face
	 * Source: https://bukkit.org/threads/400099/
	 *
	 * @param yaw
	 * @param useSubDirections
	 * @return
	 */
	public static BlockFace getFacing(float yaw, boolean useSubDirections) {
		if (useSubDirections)
			return FACE_RADIAL[Math.round(yaw / 45F) & 0x7].getOppositeFace();

		return FACE_AXIS[Math.round(yaw / 90F) & 0x3].getOppositeFace();
	}

	/**
	 * Return a yaw from BlockFace
	 *
	 * @param face
	 * @param useSubDirections
	 * @return
	 */
	public static int getFacing(BlockFace face, boolean useSubDirections) {
		return (useSubDirections ? face.ordinal() * 45 : face.ordinal() * 90) - 180;
	}

	/**
	 * Converts the given yaw into the closest valid blockface and then back to yaw
	 *
	 * Used to align entities to look in one of the 4 or 8 directions without you needing
	 * to stand perfectly straight.
	 *
	 * @param yaw
	 * @param useSubDirections
	 * @return
	 */
	public static float alignYaw(float yaw, boolean useSubDirections) {
		BlockFace face = getFacing(yaw, useSubDirections);

		return getFacing(face, useSubDirections);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Statistics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the total amount of time the player has spent on the server.
	 * This will get reset if you delete the playerdata folder inside your main world folder.
	 *
	 * **For Minecraft 1.12 and older this returns a tick value, otherwise this returns the
	 * amount of minutes!**
	 *
	 * @param player
	 * @return
	 */
	public static long getPlayTimeTicksOrSeconds(OfflinePlayer player) {
		final Statistic playTime = Remain.getPlayTimeStatisticName();

		return getStatistic(player, playTime);
	}

	/**
	 * Return statistics of ALL offline players ever played
	 *
	 * @param statistic
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic) {
		return getStatistics(statistic, null, null);
	}

	/**
	 * Return statistics of ALL offline players ever played
	 *
	 * @param statistic
	 * @param material
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic, final Material material) {
		return getStatistics(statistic, material, null);
	}

	/**
	 * Return statistics of ALL offline players ever played
	 *
	 * @param statistic
	 * @param entityType
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic, final EntityType entityType) {
		return getStatistics(statistic, null, entityType);
	}

	/**
	 * Return statistics of ALL offline players ever played
	 *
	 * @param statistic
	 * @param material
	 * @param entityType
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic, final Material material, final EntityType entityType) {
		final TreeMap<Long, OfflinePlayer> statistics = new TreeMap<>(Collections.reverseOrder());

		for (final OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
			final long time = getStatistic(offline, statistic, material, entityType);

			statistics.put(time, offline);
		}

		return statistics;
	}

	/**
	 * Return a statistic of an online player
	 *
	 * @param player
	 * @param statistic
	 * @return
	 */
	public static long getStatistic(final OfflinePlayer player, final Statistic statistic) {
		return getStatistic(player, statistic, null, null);
	}

	/**
	 * Return a statistic of an online player
	 *
	 * @param player
	 * @param statistic
	 * @param material
	 * @return
	 */
	public static long getStatistic(final OfflinePlayer player, final Statistic statistic, final Material material) {
		return getStatistic(player, statistic, material, null);
	}

	/**
	 * Return a statistic of an online player
	 *
	 * @param player
	 * @param statistic
	 * @param entityType
	 * @return
	 */
	public static long getStatistic(final OfflinePlayer player, final Statistic statistic, final EntityType entityType) {
		return getStatistic(player, statistic, null, entityType);
	}

	/**
	 * Return a statistic of an online player
	 *
	 * @param player
	 * @param statistic
	 * @return
	 */
	private static long getStatistic(final OfflinePlayer player, final Statistic statistic, final Material material, final EntityType entityType) {
		// Return live statistic for up to date data and best performance if possible
		if (player.isOnline()) {
			final Player online = player.getPlayer();

			if (statistic.getType() == Type.UNTYPED)
				return online.getStatistic(statistic);

			else if (statistic.getType() == Type.ENTITY)
				return online.getStatistic(statistic, entityType);

			return online.getStatistic(statistic, material);
		}

		// Otherwise read his stats file
		return getStatisticFile(player, statistic, material, entityType);
	}

	// Read json file for the statistic
	private static long getStatisticFile(final OfflinePlayer player, final Statistic statistic, final Material material, final EntityType entityType) {
		final File worldFolder = new File(Bukkit.getServer().getWorlds().get(0).getWorldFolder(), "stats");
		final File statFile = new File(worldFolder, player.getUniqueId().toString() + ".json");

		if (statFile.exists())
			try {
				final JSONObject json = (JSONObject) JSONParser.deserialize(new FileReader(statFile));
				final String name = Remain.getNMSStatisticName(statistic, material, entityType);

				JSONObject section = json.getObject("stats");
				long result = 0;

				for (String part : name.split("\\:")) {
					part = part.replace(".", ":");

					if (section != null) {
						final JSONObject nextSection = section.getObject(part);

						if (nextSection == null) {
							result = Long.parseLong(section.containsKey(part) ? section.get(part).toString() : "0");
							break;
						}

						section = nextSection;
					}
				}

				return result;

			} catch (final Throwable t) {
				throw new FoException(t);
			}

		return 0;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Permissions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return if the given sender has a certain permission
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public static boolean hasPerm(final Permissible sender, final String permission) {
		Valid.checkNotNull(sender, "Cannot call PlayerUtil#hasPerm() for null sender!");

		if (permission == null) {
			Common.log("THIS IS NOT AN ACTUAL ERROR, YOUR PLUGIN WILL WORK FINE");
			Common.log("Internal check got null permission as input, this is no longer allowed.");
			Common.log("We'll return true to prevent errors. Contact developers of " + SimplePlugin.getNamed());
			Common.log("to get it solved and include the fake error below:");

			new Throwable().printStackTrace();

			return true;
		}

		Valid.checkBoolean(!permission.contains("{plugin_name}") && !permission.contains("{plugin_name_lower}"),
				"Found {plugin_name} variable calling hasPerm(" + sender + ", " + permission + ")." + "This is now disallowed, contact plugin authors to put " + SimplePlugin.getNamed().toLowerCase() + " in their permission.");

		return sender.hasPermission(permission);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Inventory
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Sets pretty much every flag the player can have such as
	 * flying etc, back to normal
	 * <p>
	 * Also sets gamemode to survival
	 * <p>
	 * Typical usage: Minigame plugins - call this before joining the player to an arena
	 * <p>
	 * Even disables Essentials god mode and removes vanish (most vanish plugins are supported).
	 *
	 * @param player
	 * @param cleanInventory
	 */
	public static void normalize(final Player player, final boolean cleanInventory) {
		normalize(player, cleanInventory, true);
	}

	/**
	 * Sets pretty much every flag the player can have such as
	 * flying etc, back to normal
	 * <p>
	 * Also sets gamemode to survival
	 * <p>
	 * Typical usage: Minigame plugins - call this before joining the player to an arena
	 * <p>
	 * Even disables Essentials god mode.
	 *
	 * @param player
	 * @param cleanInventory
	 * @param removeVanish   should we remove vanish from players? most vanish plugins are supported
	 */
	public static void normalize(final Player player, final boolean cleanInventory, final boolean removeVanish) {
		synchronized (titleRestoreTasks) {
			HookManager.setGodMode(player, false);

			player.setGameMode(GameMode.SURVIVAL);

			if (cleanInventory) {
				cleanInventoryAndFood(player);

				try {
					CompAttribute.GENERIC_MAX_HEALTH.set(player, 20);

				} catch (final Throwable t) {
					try {
						player.setMaxHealth(20);

					} catch (final Throwable tt) {

						try {
							player.resetMaxHealth();
						} catch (final Throwable ttt) {
							// Minecraft 1.2.5 lol
						}
					}
				}

				try {
					player.setHealth(20);

				} catch (final Throwable t) {
					// Try attribute way

					try {
						final double maxHealthAttr = CompAttribute.GENERIC_MAX_HEALTH.get(player);

						player.setHealth(maxHealthAttr);

					} catch (final Throwable tt) {
						// silence if a third party plugin is controlling health
					}
				}

				player.setHealthScaled(false);

				for (final PotionEffect potion : player.getActivePotionEffects())
					player.removePotionEffect(potion.getType());
			}

			player.setTotalExperience(0);
			player.setLevel(0);
			player.setExp(0F);

			player.resetPlayerTime();
			player.resetPlayerWeather();

			player.setFallDistance(0);

			CompProperty.INVULNERABLE.apply(player, false);
			CompProperty.GLOWING.apply(player, false);
			CompProperty.SILENT.apply(player, false);

			player.setAllowFlight(false);
			player.setFlying(false);

			player.setFlySpeed(0.1F);
			player.setWalkSpeed(0.2F);

			player.setCanPickupItems(true);

			player.setVelocity(new Vector(0, 0, 0));
			player.eject();

			EntityUtil.removeVehiclesAndPassengers(player);

			if (removeVanish)
				try {
					if (player.hasMetadata("vanished")) {
						final Plugin plugin = player.getMetadata("vanished").get(0).getOwningPlugin();

						player.removeMetadata("vanished", plugin);
					}

					for (final Player other : Remain.getOnlinePlayers())
						if (!other.getName().equals(player.getName()) && !other.canSee(player))
							other.showPlayer(player);

				} catch (final NoSuchMethodError err) {
					/* old MC */

				} catch (final Exception ex) {
					ex.printStackTrace();
				}
		}
	}

	/*
	 * Cleans players inventory and restores food levels
	 */
	private static void cleanInventoryAndFood(final Player player) {
		player.getInventory().setArmorContents(null);
		player.getInventory().setContents(new ItemStack[player.getInventory().getContents().length]);

		try {
			player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);
		} catch (final NoSuchMethodError err) {
			/* old MC */
		}

		player.setFireTicks(0);
		player.setFoodLevel(20);
		player.setExhaustion(0);
		player.setSaturation(10);

		player.setVelocity(new Vector(0, 0, 0));
	}

	/**
	 * Returns true if the player has empty both normal and armor inventory
	 *
	 * @param player
	 * @return
	 */
	public static boolean hasEmptyInventory(final Player player) {
		final ItemStack[] inv = player.getInventory().getContents();
		final ItemStack[] armor = player.getInventory().getArmorContents();

		final Object[] everything = Common.joinArrays(inv, armor);

		for (final Object i : everything)
			if (i instanceof ItemStack)
				if (((ItemStack) i).getType() != Material.AIR)
					return false;

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Player states
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Set the players snapshot to be stored locally in the cache
	 *
	 * @param player
	 */
	public static void storeState(final Player player) {
		Valid.checkBoolean(!hasStoredState(player), "Player " + player.getName() + " already has a stored state!");

		final SerializedMap data = SerializedMap.ofArray(
				"gameMode", player.getGameMode(),
				"content", player.getInventory().getContents(),
				"armorContent", player.getInventory().getArmorContents(),
				"maxHealth", Remain.getMaxHealth(player),
				"health", Remain.getHealth(player),
				"healthScaled", player.isHealthScaled(),
				"remainingAir", player.getRemainingAir(),
				"maximumAir", player.getMaximumAir(),
				"fallDistance", player.getFallDistance(),
				"fireTicks", player.getFireTicks(),
				"totalExp", player.getTotalExperience(),
				"level", player.getLevel(),
				"exp", player.getExp(),
				"foodLevel", player.getFoodLevel(),
				"exhaustion", player.getExhaustion(),
				"saturation", player.getSaturation(),
				"flySpeed", player.getFlySpeed(),
				"walkSpeed", player.getWalkSpeed(),
				"potionEffects", player.getActivePotionEffects());

		// Attributes
		final Map<CompAttribute, Double> attributes = new HashMap<>();

		for (final CompAttribute attribute : CompAttribute.values()) {
			final Double value = attribute.get(player);

			if (value != null)
				attributes.put(attribute, value);
		}

		data.put("attributes", attributes);

		// From now on we have to surround each method with try-catch since
		// those are not available in older MC versions

		try {
			data.put("extraContent", player.getInventory().getExtraContents());
		} catch (final Throwable t) {
		}

		try {
			data.put("invulnerable", player.isInvulnerable());
		} catch (final Throwable t) {
		}

		try {
			data.put("silent", player.isSilent());
		} catch (final Throwable t) {
		}

		try {
			data.put("glowing", player.isGlowing());
		} catch (final Throwable t) {
		}

		storedPlayerStates.put(player.getUniqueId(), data);
	}

	/**
	 * Restores the player inventory and properties
	 *
	 * @param player
	 */
	public static void restoreState(final Player player) {
		final SerializedMap data = storedPlayerStates.remove(player.getUniqueId());
		Valid.checkNotNull(data, "Player " + player.getName() + " does not have a stored game state!");

		player.setGameMode(data.get("gameMode", GameMode.class));
		player.getInventory().setContents((ItemStack[]) data.getObject("content"));
		player.getInventory().setArmorContents((ItemStack[]) data.getObject("armorContent"));
		player.setMaxHealth(data.getInteger("maxHealth"));
		player.setHealth(data.getInteger("health"));
		player.setHealthScaled(data.getBoolean("healthScaled"));
		player.setRemainingAir(data.getInteger("remainingAir"));
		player.setMaximumAir(data.getInteger("maximumAir"));
		player.setFallDistance(data.getFloat("fallDistance"));
		player.setFireTicks(data.getInteger("fireTicks"));
		player.setTotalExperience(data.getInteger("totalExp"));
		player.setLevel(data.getInteger("level"));
		player.setExp(data.getFloat("exp"));
		player.setFoodLevel(data.getInteger("foodLevel"));
		player.setExhaustion(data.getFloat("exhaustion"));
		player.setSaturation(data.getFloat("saturation"));
		player.setFlySpeed(data.getFloat("flySpeed"));
		player.setWalkSpeed(data.getFloat("walkSpeed"));

		// Remove old potion effects
		for (final PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());

		// And add news
		for (final PotionEffect effect : data.getList("potionEffects", PotionEffect.class))
			player.addPotionEffect(effect);

		// Attributes
		final Map<CompAttribute, Double> attributes = (Map<CompAttribute, Double>) data.getObject("attributes");

		for (final Entry<CompAttribute, Double> entry : attributes.entrySet())
			entry.getKey().set(player, entry.getValue());

		// From now on we have to surround each method with try-catch since
		// those are not available in older MC versions

		try {
			player.getInventory().setExtraContents((ItemStack[]) data.getObject("extraContent"));
		} catch (final Throwable t) {
		}

		try {
			player.setInvulnerable(data.getBoolean("invulnerable"));
		} catch (final Throwable t) {
		}

		try {
			player.setSilent(data.getBoolean("silent"));
		} catch (final Throwable t) {
		}

		try {
			player.setGlowing(data.getBoolean("glowing"));
		} catch (final Throwable t) {
		}
	}

	/**
	 * Return true if the player has a stored snapshot of inventory and properties
	 * @param player
	 *
	 * @return
	 */
	public static boolean hasStoredState(Player player) {
		return storedPlayerStates.containsKey(player.getUniqueId());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Vanish
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return if the player is vanished, see {@link #isVanished(Player)} or if the other player can see him
	 *
	 * @param player
	 * @param otherPlayer
	 * @return
	 */
	public static boolean isVanished(final Player player, final Player otherPlayer) {
		if (otherPlayer != null && !otherPlayer.canSee(player))
			return true;

		return isVanished(player);
	}

	/**
	 * Return true if the player is vanished. We check for "vanished"
	 * metadata value which is supported by most plugins (CMI, Essentials, etc.)
	 *
	 * Does NOT return true for vanish potions or spectator mode.
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(final Player player) {
		final List<MetadataValue> list = player.getMetadata("vanished");

		for (final MetadataValue meta : list)
			if (meta.asBoolean())
				return true;

		return false;
	}

	/**
	 * Updates vanish status for player using metadata, Essentials, CMI and NMS invisibility.
	 *
	 * @param player
	 * @param vanished
	 */
	public static void setVanished(Player player, boolean vanished) {

		// Hook into other plugins
		HookManager.setVanished(player, vanished);

		// Clear any previous metadata
		for (Iterator<MetadataValue> it = player.getMetadata("vanished").iterator(); it.hasNext();) {
			MetadataValue meta = it.next();

			if (meta.asBoolean())
				meta.invalidate();
		}

		// Re-add metadata if vanished
		if (vanished)
			player.setMetadata("vanished", new FixedMetadataValue(SimplePlugin.getInstance(), true));

		// NMS
		Remain.setInvisible(player, vanished);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Nicks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the player that matches the given nick name and is not vanished
	 *
	 * @param name
	 * @return
	 */
	public static Player getPlayerByNickNoVanish(final String name) {
		return getPlayerByNick(name, false);
	}

	/**
	 * Return the player for the given name or nickname
	 *
	 * @param name
	 * @param ignoreVanished
	 * @return
	 */
	public static Player getPlayerByNick(final String name, final boolean ignoreVanished) {
		final Player found = lookupNickedPlayer0(name);

		if (ignoreVanished && found != null && PlayerUtil.isVanished(found))
			return null;

		return found;
	}

	private static Player lookupNickedPlayer0(final String name) {
		Player found = null;
		int delta = Integer.MAX_VALUE;

		for (final Player player : Remain.getOnlinePlayers()) {

			if (player.getName().equalsIgnoreCase(name))
				return player;

			final String nick = HookManager.getNickColorless(player);

			if (nick.toLowerCase().startsWith(name.toLowerCase())) {
				final int curDelta = Math.abs(nick.length() - name.length());

				if (curDelta < delta) {
					found = player;
					delta = curDelta;
				}

				if (curDelta == 0)
					break;
			}
		}

		return found;
	}

	/**
	 * Performs an async player lookup then runs the action in a sync runnable
	 *
	 * @param name
	 * @param syncCallback
	 */
	public static void lookupOfflinePlayerAsync(String name, Consumer<OfflinePlayer> syncCallback) {
		Common.runAsync(() -> {
			// If the given name is a nick, try to get the real name
			final String parsedName = HookManager.getNameFromNick(name);
			final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(parsedName);

			Common.runLater(() -> syncCallback.accept(offlinePlayer));
		});
	}

	// ----------------------------------------------------------------------------------------------------
	// Animation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Sends an animated title to player. Colors are replaced.
	 *
	 * @param menu           the menu
	 * @param player         the player
	 * @param temporaryTitle the animated title
	 * @param oldTitle       the old title to revert to
	 * @param duration       the duration in ticks
	 */
	public static void updateInventoryTitle(final Menu menu, final Player player, final String temporaryTitle, final String oldTitle, final int duration) {
		Valid.checkNotNull(menu, "Menu == null");
		Valid.checkNotNull(player, "Player == null");
		Valid.checkNotNull(temporaryTitle, "Title == null");
		Valid.checkNotNull(oldTitle, "Old Title == null");

		// Send the packet
		updateInventoryTitle(player, MinecraftVersion.atLeast(V.v1_13) ? temporaryTitle.replace("%", "%%") : temporaryTitle);

		// Prevent flashing titles
		BukkitTask pending = titleRestoreTasks.get(player.getUniqueId());

		if (pending != null)
			pending.cancel();

		pending = Common.runLater(duration, () -> {
			final Menu futureMenu = Menu.getMenu(player);

			if (futureMenu != null && futureMenu.getClass().getName().equals(menu.getClass().getName()))
				updateInventoryTitle(player, oldTitle);
		});

		final UUID uid = player.getUniqueId();

		titleRestoreTasks.put(uid, pending);

		// Prevent overloading the map so remove the key afterwards
		Common.runLater(duration + 1, () -> {
			if (titleRestoreTasks.containsKey(uid))
				titleRestoreTasks.remove(uid);
		});
	}

	/**
	 * Update the player's inventory title without closing the window
	 *
	 * @param player the player
	 * @param title  the new title
	 */
	public static void updateInventoryTitle(final Player player, final String title) {
		Remain.updateInventoryTitle(player, title);
	}

	// ----------------------------------------------------------------------------------------------------
	// Inventory manipulation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Attempts to retrieve the first item that is similar (See {@link ItemUtil#isSimilar(ItemStack, ItemStack)})
	 * to the given item.
	 *
	 * @param player
	 * @param item   the found item or null if none
	 * @return
	 */
	public static ItemStack getFirstItem(final Player player, final ItemStack item) {
		for (final ItemStack otherItem : player.getInventory().getContents())
			if (otherItem != null && ItemUtil.isSimilar(otherItem, item))
				return otherItem;

		return null;
	}

	/**
	 * Take the given material in the given size, return true if the player
	 * had enough to be taken from him (otherwise no action is done)
	 *
	 * @param player
	 * @param material
	 * @param amount
	 * @return
	 */
	public static boolean take(Player player, CompMaterial material, int amount) {
		if (!containsAtLeast(player, amount, material))
			return false;

		final Inventory inventory = player.getInventory();
		final ItemStack[] content = inventory.getContents();

		for (int slot = 0; slot < content.length; slot++) {
			ItemStack item = content[slot];

			if (item != null && material.is(item)) {
				int itemAmount = item.getAmount();
				int newAmount = itemAmount - amount;

				if (newAmount < 0) {
					amount = amount - itemAmount;

					content[slot] = null;
				}

				else {
					item.setAmount(newAmount);

					content[slot] = item;
					break;
				}
			}
		}

		inventory.setContents(content);

		return true;
	}

	/**
	 * Scans the inventory and removes one piece of the first found item
	 * matching the given material
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static boolean takeFirstOnePiece(final Player player, final CompMaterial material) {

		for (final ItemStack item : player.getInventory().getContents())
			if (item != null && material.is(item)) {
				takeOnePiece(player, item);

				return true;
			}

		return false;
	}

	/**
	 * Removes one piece of the given item stack, setting the slot to air
	 * if the item is only 1 amount
	 * <p>
	 * THIS SETS THE AMOUNT OF THE GIVEN ITEMSTACK TO -1 OF ITS CURRENT AMOUNT
	 * AND DOES NOT AUTOMATICALLY REMOVE ITEMS
	 *
	 * @param player
	 * @param item
	 */
	public static void takeOnePiece(final Player player, final ItemStack item) {
		Remain.takeItemOnePiece(player, item);
	}

	/**
	 * Return if the player has enough of the given material
	 *
	 * @param player
	 * @param atLeastSize
	 * @param material
	 * @return
	 */
	public static boolean containsAtLeast(Player player, int atLeastSize, CompMaterial material) {
		int foundSize = 0;

		for (final ItemStack item : player.getInventory().getContents())
			if (item != null && item.getType() == material.getMaterial())
				foundSize += item.getAmount();

		return foundSize >= atLeastSize;
	}

	/**
	 * Attempts to search and replace the first similar itemstack with the new one
	 *
	 * @param inv
	 * @param search
	 * @param replaceWith
	 * @return true if the replace was successful
	 */
	public static boolean updateInvSlot(final Inventory inv, final ItemStack search, final ItemStack replaceWith) {
		Valid.checkNotNull(inv, "Inv = null");

		for (int i = 0; i < inv.getSize(); i++) {
			final ItemStack slot = inv.getItem(i);

			if (slot != null && ItemUtil.isSimilar(slot, search)) {
				inv.setItem(i, replaceWith);

				return true;
			}
		}

		return false;
	}

	/**
	 * Attempts to add items to player's inventory,
	 * returns true if all items were added. If player's
	 * inventory is full, we drop the items nearby and return false.
	 *
	 * @param player
	 * @param items
	 * @return false if inventory was full and some items were dropped at the floor, such as the mic
	 */
	public static boolean addItemsOrDrop(Player player, ItemStack... items) {
		final Map<Integer, ItemStack> leftovers = addItems(player.getInventory(), items);

		final World world = player.getWorld();
		final Location location = player.getLocation();

		for (final ItemStack leftover : leftovers.values()) {
			final Item item = world.dropItem(location, leftover);

			item.setPickupDelay(2 * 20);
		}

		return leftovers.isEmpty();
	}

	/**
	 * Attempts to add items into the inventory,
	 * returning what it couldn't store
	 *
	 * @param inventory
	 * @param items
	 * @return
	 */
	public static Map<Integer, ItemStack> addItems(final Inventory inventory, final Collection<ItemStack> items) {
		return addItems(inventory, items.toArray(new ItemStack[items.size()]));
	}

	/**
	 * Attempts to add items into the inventory,
	 * returning what it couldn't store
	 *
	 * @param inventory
	 * @param items
	 * @return
	 */
	public static Map<Integer, ItemStack> addItems(final Inventory inventory, final ItemStack... items) {
		return addItems(inventory, 0, items);
	}

	/**
	 * Attempts to add items into the inventory,
	 * returning what it couldn't store
	 * <p>
	 * Set oversizedStack to below normal stack size to disable oversized stacks
	 *
	 * @param inventory
	 * @param oversizedStacks
	 * @param items
	 * @return
	 */
	private static Map<Integer, ItemStack> addItems(final Inventory inventory, final int oversizedStacks, final ItemStack... items) {
		if (isCombinedInv(inventory)) {
			final Inventory fakeInventory = makeTruncatedInv((PlayerInventory) inventory);
			final Map<Integer, ItemStack> overflow = addItems(fakeInventory, oversizedStacks, items);
			for (int i = 0; i < fakeInventory.getContents().length; i++)
				inventory.setItem(i, fakeInventory.getContents()[i]);
			return overflow;
		}

		final Map<Integer, ItemStack> left = new HashMap<>();

		// combine items
		final ItemStack[] combined = new ItemStack[items.length];
		for (final ItemStack item : items) {
			if (item == null || item.getAmount() < 1)
				continue;
			for (int j = 0; j < combined.length; j++) {
				if (combined[j] == null) {
					combined[j] = item.clone();
					break;
				}
				if (combined[j].isSimilar(item)) {
					combined[j].setAmount(combined[j].getAmount() + item.getAmount());
					break;
				}
			}
		}

		for (int i = 0; i < combined.length; i++) {
			final ItemStack item = combined[i];
			if (item == null || item.getType() == Material.AIR)
				continue;

			while (true) {
				// Do we already have a stack of it?
				final int maxAmount = oversizedStacks > item.getType().getMaxStackSize() ? oversizedStacks : item.getType().getMaxStackSize();
				final int firstPartial = firstPartial(inventory, item, maxAmount);

				// Drat! no partial stack
				if (firstPartial == -1) {
					// Find a free spot!
					final int firstFree = inventory.firstEmpty();

					if (firstFree == -1) {
						// No space at all!
						left.put(i, item);
						break;
					}

					// More than a single stack!
					if (item.getAmount() > maxAmount) {
						final ItemStack stack = item.clone();
						stack.setAmount(maxAmount);
						inventory.setItem(firstFree, stack);
						item.setAmount(item.getAmount() - maxAmount);
					} else {
						// Just store it
						inventory.setItem(firstFree, item);
						break;
					}

				} else {
					// So, apparently it might only partially fit, well lets do just that
					final ItemStack partialItem = inventory.getItem(firstPartial);

					final int amount = item.getAmount();
					final int partialAmount = partialItem.getAmount();

					// Check if it fully fits
					if (amount + partialAmount <= maxAmount) {
						partialItem.setAmount(amount + partialAmount);
						break;
					}

					// It fits partially
					partialItem.setAmount(maxAmount);
					item.setAmount(amount + partialAmount - maxAmount);
				}
			}
		}
		return left;
	}

	// ----------------------------------------------------------------------------------------------------
	// Utility
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return the first similar itemstack
	 *
	 * @param inventory
	 * @param item
	 * @param maxAmount
	 * @return
	 */
	private static int firstPartial(final Inventory inventory, final ItemStack item, final int maxAmount) {
		if (item == null)
			return -1;
		final ItemStack[] stacks = inventory.getContents();
		for (int i = 0; i < stacks.length; i++) {
			final ItemStack cItem = stacks[i];
			if (cItem != null && cItem.getAmount() < maxAmount && cItem.isSimilar(item))
				return i;
		}
		return -1;
	}

	/**
	 * Creates a new inventory of {@link #USABLE_PLAYER_INV_SIZE} size
	 *
	 * @param playerInventory
	 * @return
	 */
	private static Inventory makeTruncatedInv(final PlayerInventory playerInventory) {
		final Inventory fake = Bukkit.createInventory(null, USABLE_PLAYER_INV_SIZE);
		fake.setContents(Arrays.copyOf(playerInventory.getContents(), fake.getSize()));

		return fake;
	}

	/**
	 * Return true if the inventory is combined player inventory
	 *
	 * @param inventory
	 * @return
	 */
	private static boolean isCombinedInv(final Inventory inventory) {
		return inventory instanceof PlayerInventory && inventory.getContents().length > USABLE_PLAYER_INV_SIZE;
	}
}
