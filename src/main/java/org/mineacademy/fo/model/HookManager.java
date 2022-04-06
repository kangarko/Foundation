package org.mineacademy.fo.model;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.Remain;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.TabList.TabListManager;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.server.TemporaryPlayer;
import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.IUser;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;
import com.gmail.nossr50.datatypes.chat.ChatChannel;
import com.gmail.nossr50.datatypes.party.Party;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.util.player.UserManager;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import fr.xephi.authme.api.v3.AuthMeApi;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

/**
 * Our main class hooking into different plugins, providing you
 * convenience access to their methods
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HookManager {

	// ------------------------------------------------------------------------------------------------------------
	// Store hook classes separately for below, avoiding no such method/field errors
	// ------------------------------------------------------------------------------------------------------------

	private static AuthMeHook authMeHook;
	private static BanManagerHook banManagerHook;
	private static BossHook bossHook;
	private static CitizensHook citizensHook;
	private static CMIHook CMIHook;
	private static DiscordSRVHook discordSRVHook;
	private static EssentialsHook essentialsHook;
	private static FactionsHook factionsHook;
	private static ItemsAdderHook itemsAdderHook;
	private static LandsHook landsHook;
	private static LiteBansHook liteBansHook;
	private static LocketteProHook locketteProHook;
	private static LWCHook lwcHook;
	private static McMMOHook mcmmoHook;
	private static MultiverseHook multiverseHook;
	private static MVdWPlaceholderHook MVdWPlaceholderHook;
	private static MythicMobsHook mythicMobsHook;
	private static NickyHook nickyHook;
	private static PlaceholderAPIHook placeholderAPIHook;
	private static PlotSquaredHook plotSquaredHook;
	private static ProtocolLibHook protocolLibHook;
	private static ResidenceHook residenceHook;
	private static TownyHook townyHook;
	private static VaultHook vaultHook;
	private static WorldEditHook worldeditHook;
	private static WorldGuardHook worldguardHook;

	private static boolean nbtAPIDummyHook = false;
	private static boolean nuVotifierDummyHook = false;
	private static boolean townyChatDummyHook = false;

	// ------------------------------------------------------------------------------------------------------------
	// Main loading method
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Detect various plugins and load their methods into this library so you can use it later
	 */
	public static void loadDependencies() {

		if (Common.doesPluginExist("AuthMe"))
			authMeHook = new AuthMeHook();

		if (Common.doesPluginExist("BanManager"))
			banManagerHook = new BanManagerHook();

		if (Common.doesPluginExist("Boss"))
			bossHook = new BossHook();

		if (Common.doesPluginExist("Citizens"))
			citizensHook = new CitizensHook();

		if (Common.doesPluginExist("CMI"))
			CMIHook = new CMIHook();

		if (Common.doesPluginExist("DiscordSRV"))
			try {
				Class.forName("github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel");

				discordSRVHook = new DiscordSRVHook();

			} catch (final ClassNotFoundException ex) {
				Common.error(ex, "&c" + SimplePlugin.getNamed() + " failed to hook DiscordSRV because the plugin is outdated (1.18.x is supported)!");
			}

		if (Common.doesPluginExist("Essentials"))
			essentialsHook = new EssentialsHook();

		// Various kinds of Faction plugins
		final Plugin factions = Bukkit.getPluginManager().getPlugin("Factions");

		if (Common.doesPluginExist("FactionsX") && factions == null)
			Common.log("Note: If you want FactionX integration, install FactionsUUIDAPIProxy.");

		else if (factions != null) {
			final String ver = factions.getDescription().getVersion();
			final String main = factions.getDescription().getMain();

			if (ver.startsWith("1.6") || main.contains("FactionsUUIDAPIProxy")) {
				factionsHook = new FactionsUUID();

			} else if (ver.startsWith("2.")) {
				Class<?> mplayer = null;

				try {
					mplayer = Class.forName("com.massivecraft.factions.entity.MPlayer"); // only support the free version of the plugin
				} catch (final ClassNotFoundException ex) {
				}

				if (mplayer != null) {
					factionsHook = new FactionsMassive();

				} else
					Common.warning("Recognized MCore Factions, but not hooked! Check if you have the latest version!");

			}
		}

		if (Common.doesPluginExist("ItemsAdder"))
			itemsAdderHook = new ItemsAdderHook();

		if (Common.doesPluginExist("Lands"))
			landsHook = new LandsHook();

		if (Common.doesPluginExist("LiteBans"))
			liteBansHook = new LiteBansHook();

		if (Common.doesPluginExist("Lockette"))
			locketteProHook = new LocketteProHook();

		if (Common.doesPluginExist("LWC"))
			lwcHook = new LWCHook();

		if (Common.doesPluginExist("mcMMO")) {
			final String ver = Bukkit.getPluginManager().getPlugin("mcMMO").getDescription().getVersion();

			if (ver.startsWith("2."))
				mcmmoHook = new McMMOHook();
			else
				Common.warning("Could not hook into mcMMO, version 2.x required, you have " + ver);
		}

		if (Common.doesPluginExist("Multiverse-Core"))
			multiverseHook = new MultiverseHook();

		if (Common.doesPluginExist("MVdWPlaceholderAPI"))
			MVdWPlaceholderHook = new MVdWPlaceholderHook();

		if (Common.doesPluginExist("MythicMobs"))
			mythicMobsHook = new MythicMobsHook();

		if (Common.doesPluginExist("Nicky"))
			nickyHook = new NickyHook();

		if (Common.doesPluginExist("PlaceholderAPI"))
			placeholderAPIHook = new PlaceholderAPIHook();

		if (Common.doesPluginExist("PlotSquared")) {
			final String ver = Bukkit.getPluginManager().getPlugin("PlotSquared").getDescription().getVersion();

			if (ver.startsWith("6.") || ver.startsWith("5.") || ver.startsWith("3."))
				plotSquaredHook = new PlotSquaredHook();
			else
				Common.warning("Could not hook into PlotSquared, version 3.x, 5.x or 6.x required, you have " + ver);
		}

		if (Common.doesPluginExist("ProtocolLib")) {
			protocolLibHook = new ProtocolLibHook();

			// Also check if the library is loaded properly
			try {
				if (MinecraftVersion.newerThan(V.v1_6))
					Class.forName("com.comphenix.protocol.wrappers.WrappedChatComponent");
			} catch (final Throwable t) {
				protocolLibHook = null;

				Common.throwError(t, "You are running an old and unsupported version of ProtocolLib, please update it.");
			}
		}

		if (Common.doesPluginExist("Residence"))
			residenceHook = new ResidenceHook();

		if (Common.doesPluginExist("Towny"))
			townyHook = new TownyHook();

		if (Common.doesPluginExist("Vault"))
			vaultHook = new VaultHook();

		if (Common.doesPluginExist("WorldEdit") || Common.doesPluginExist("FastAsyncWorldEdit"))
			worldeditHook = new WorldEditHook();

		if (Common.doesPluginExist("WorldGuard"))
			worldguardHook = new WorldGuardHook(worldeditHook);

		// Dummy hooks

		if (Common.doesPluginExist("NBTAPI"))
			nbtAPIDummyHook = true;

		if (Common.doesPluginExist("Votifier"))
			nuVotifierDummyHook = true;

		if (Common.doesPluginExist("TownyChat"))
			townyChatDummyHook = true;
	}

	/**
	 * Removes packet listeners from ProtocolLib for a plugin
	 *
	 * @param plugin
	 *
	 * @deprecated internal use only, please do not call
	 */
	@Deprecated
	public static void unloadDependencies(final Plugin plugin) {
		if (isProtocolLibLoaded())
			protocolLibHook.removePacketListeners(plugin);

		if (isPlaceholderAPILoaded())
			placeholderAPIHook.unregister();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods for determining which plugins were loaded after you call the load method
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Is AuthMe Reloaded loaded? We only support the latest version
	 *
	 * @return
	 */
	public static boolean isAuthMeLoaded() {
		return authMeHook != null;
	}

	/**
	 * Return if BanManager plugin is detected
	 *
	 * @return
	 */
	public static boolean isBanManagerLoaded() {
		return banManagerHook != null;
	}

	/**
	 * Return if Boss plugin is detected
	 *
	 * @return
	 */
	public static boolean isBossLoaded() {
		return bossHook != null;
	}

	/**
	 * Is CMI loaded?
	 *
	 * @return
	 */
	public static boolean isCMILoaded() {
		return CMIHook != null;
	}

	/**
	 * Is Citizens loaded?
	 *
	 * @return
	 */
	public static boolean isCitizensLoaded() {
		return citizensHook != null;
	}

	/**
	 * Is DiscordSRV loaded?
	 *
	 * @return
	 */
	public static boolean isDiscordSRVLoaded() {
		return discordSRVHook != null;
	}

	/**
	 * Is EssentialsX loaded?
	 *
	 * @return
	 */
	public static boolean isEssentialsLoaded() {
		return essentialsHook != null;
	}

	/**
	 * Are Faction plugins loaded? We support UUID factions and free factions
	 *
	 * @return
	 */
	public static boolean isFactionsLoaded() {
		return factionsHook != null;
	}

	/**
	 * Is FastAsyncWorldEdit loaded?
	 *
	 * @return
	 */
	public static boolean isFAWELoaded() {

		// Check for FAWE directly
		final Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");

		if (fawe != null && fawe.isEnabled())
			return true;

		// Check for legacy FAWE installations
		final Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");

		if (worldEdit != null && worldEdit.isEnabled() && "Fast Async WorldEdit plugin".equals(worldEdit.getDescription().getDescription()))
			return true;

		return false;
	}

	/**
	 * Is ItemsAdder loaded as a plugin?
	 *
	 * @return
	 */
	public static boolean isItemsAdderLoaded() {
		return itemsAdderHook != null;
	}

	/**
	 * Is Lands loaded as a plugin?
	 *
	 * @return
	 */
	public static boolean isLandsLoaded() {
		return landsHook != null;
	}

	/**
	 * Is LiteBans loaded?
	 *
	 * @return
	 */
	public static boolean isLiteBansLoaded() {
		return liteBansHook != null;
	}

	/**
	 * Is Lockette Pro loaded
	 *
	 * @return
	 */
	public static boolean isLocketteProLoaded() {
		return locketteProHook != null;
	}

	/**
	 * Is LWC loaded?
	 *
	 * @return
	 */
	public static boolean isLWCLoaded() {
		return lwcHook != null;
	}

	/**
	 * Is mcMMO loaded?
	 *
	 * @return
	 */
	public static boolean isMcMMOLoaded() {
		return mcmmoHook != null;
	}

	/**
	 * Is Multiverse-Core loaded?
	 *
	 * @return
	 */
	public static boolean isMultiverseCoreLoaded() {
		return multiverseHook != null;
	}

	/**
	 * Is MVdWPlaceholderAPI loaded?
	 *
	 * @return
	 */
	public static boolean isMVdWPlaceholderAPILoaded() {
		return MVdWPlaceholderHook != null;
	}

	/**
	 * Is MythicMobs loaded?
	 *
	 * @return
	 */
	public static boolean isMythicMobsLoaded() {
		return mythicMobsHook != null;
	}

	/**
	 * Is NBTAPI loaded as a plugin?
	 *
	 * @return
	 */
	public static boolean isNbtAPILoaded() {
		return nbtAPIDummyHook;
	}

	/**
	 * Is Nicky loaded?
	 *
	 * @return
	 */
	public static boolean isNickyLoaded() {
		return nickyHook != null;
	}

	/**
	 * Is nuVotifier loaded as a plugin?
	 *
	 * @return
	 */
	public static boolean isNuVotifierLoaded() {
		return nuVotifierDummyHook;
	}

	/**
	 * Is PlaceholderAPI loaded?
	 *
	 * @return
	 */
	public static boolean isPlaceholderAPILoaded() {
		return placeholderAPIHook != null;
	}

	/**
	 * Is PlotSquared loaded?
	 *
	 * @return
	 */
	public static boolean isPlotSquaredLoaded() {
		return plotSquaredHook != null;
	}

	/**
	 * Is ProtocolLib loaded?¡
	 * <p>
	 * This will not only check if the plugin is in plugins folder, but also if it's
	 * correctly loaded and working. (*Should* detect plugin's malfunction when
	 * out-dated.)
	 *
	 * @return
	 */
	public static boolean isProtocolLibLoaded() {
		return protocolLibHook != null;
	}

	/**
	 * Is Residence loaded?
	 *
	 * @return
	 */
	public static boolean isResidenceLoaded() {
		return residenceHook != null;
	}

	/**
	 * Is Towny loaded?
	 *
	 * @return
	 */
	public static boolean isTownyLoaded() {
		return townyHook != null;
	}

	/**
	 * Is TownyChat loaded?
	 *
	 * @return
	 */
	public static boolean isTownyChatLoaded() {
		return townyHook != null && townyChatDummyHook;
	}

	/**
	 * Is Vault loaded?
	 *
	 * @return
	 */
	public static boolean isVaultLoaded() {
		return vaultHook != null;
	}

	/**
	 * Is WorldEdit loaded?
	 *
	 * @return
	 */
	public static boolean isWorldEditLoaded() {
		return worldeditHook != null || isFAWELoaded();
	}

	/**
	 * Is WorldGuard loaded?
	 *
	 * @return
	 */
	public static boolean isWorldGuardLoaded() {
		return worldguardHook != null;
	}

	// ------------------------------------------------------------------------------------------------------------
	//
	//
	// Delegate methods for use from other plugins
	//
	//
	// ------------------------------------------------------------------------------------------------------------

	// ------------------------------------------------------------------------------------------------------------
	// AuthMe
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if player is logged via AuthMe, or true if AuthMe is not installed
	 *
	 * @param player
	 * @return
	 */
	public static boolean isLogged(final Player player) {
		return !isAuthMeLoaded() || authMeHook.isLogged(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Boss-related plugins
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the Boss name from the given entity, if Boss plugin is installed and
	 * the given entity is a Boss, otherwise returns null.
	 *
	 * @param entity
	 * @return
	 */
	public static String getBossName(Entity entity) {
		return isBossLoaded() ? bossHook.getBossName(entity) : null;
	}

	/**
	 * Returns the name from the given entity, if MythicMobs plugin is installed and
	 * the given entity is a mythic mob, otherwise returns null.
	 *
	 * @param entity
	 * @return
	 */
	public static String getMythicMobName(Entity entity) {
		return isMythicMobsLoaded() ? mythicMobsHook.getBossName(entity) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Lands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return lands players for the player's land, or empty list
	 *
	 * @param player
	 * @return
	 */
	public static Collection<Player> getLandPlayers(Player player) {
		return isLandsLoaded() ? landsHook.getLandPlayers(player) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// EssentialsX or CMI
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the given player is afk in EssentialsX or CMI, or false if neither plugin is present
	 *
	 * @param player
	 * @return
	 */
	public static boolean isAfk(final Player player) {
		final boolean essAFK = isEssentialsLoaded() && essentialsHook.isAfk(player.getName());
		final boolean cmiAFK = isCMILoaded() && CMIHook.isAfk(player);

		return essAFK || cmiAFK;
	}

	/**
	 * Return true if the given player is vanished in EssentialsX
	 *
	 * @deprecated this does not call metadata check for most plugins nor NMS check, see {@link PlayerUtil#isVanished(Player)}
	 * @param player
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedEssentials(final Player player) {
		return isEssentialsLoaded() && essentialsHook.isVanished(player.getName());
	}

	/**
	 * Return true if the given player is vanished in CMI
	 *
	 * @deprecated this does not call metadata check for most plugins nor NMS check, see {@link PlayerUtil#isVanished(Player)}
	 * @param player
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedCMI(final Player player) {
		return isCMILoaded() && CMIHook.isVanished(player);
	}

	/**
	 * Sets the vanish status for player in CMI and Essentials
	 *
	 * @deprecated this does not remove vanish metadata and NMS invisibility, use {@link PlayerUtil#setVanished(Player, boolean)} for that
	 * @param player
	 * @param vanished
	 */
	@Deprecated
	public static void setVanished(Player player, boolean vanished) {
		if (isEssentialsLoaded())
			essentialsHook.setVanished(player.getName(), vanished);

		if (isCMILoaded())
			CMIHook.setVanished(player, vanished);
	}

	/**
	 * Return true if the player is muted in EssentialsX or CMI, or false if neither plugin is present
	 *
	 * @param player
	 * @return
	 */
	public static boolean isMuted(final Player player) {

		if (isEssentialsLoaded() && essentialsHook.isMuted(player.getName()))
			return true;

		if (isCMILoaded() && CMIHook.isMuted(player))
			return true;

		if (isBanManagerLoaded() && banManagerHook.isMuted(player))
			return true;

		if (isLiteBansLoaded() && liteBansHook.isMuted(player))
			return true;

		return false;
	}

	/**
	 * If litebans is loaded, mute player - this expects you having /lmute command installed!
	 *
	 * @param targetPlayerName
	 * @param durationTokenized
	 * @param reason
	 */
	public static void setLiteBansMute(String targetPlayerName, String durationTokenized, String reason) {
		if (isLiteBansLoaded())
			Common.dispatchCommand(Bukkit.getConsoleSender(), "lmute " + targetPlayerName + " " + durationTokenized + (reason == null || reason.isEmpty() ? "" : " " + reason));
	}

	/**
	 * If litebans is loaded, unmute player - this expects you having /lunmute command installed!
	 *
	 * @param targetPlayerName
	 */
	public static void setLiteBansUnmute(String targetPlayerName) {
		if (isLiteBansLoaded())
			Common.dispatchCommand(Bukkit.getConsoleSender(), "lunmute " + targetPlayerName);
	}

	/**
	 * Returns if the player has god mode either from Essentials or CMI
	 *
	 * @param player
	 * @return
	 */
	public static boolean hasGodMode(final Player player) {
		final boolean essGodMode = isEssentialsLoaded() && essentialsHook.hasGodMode(player);
		final boolean cmiGodMode = isCMILoaded() && CMIHook.hasGodMode(player);

		return essGodMode || cmiGodMode;
	}

	/**
	 * Toggles a god mode for player from EssentialsX or CMI
	 *
	 * @param player
	 * @param godMode
	 */
	public static void setGodMode(final Player player, final boolean godMode) {
		if (isEssentialsLoaded())
			essentialsHook.setGodMode(player, godMode);

		if (isCMILoaded())
			CMIHook.setGodMode(player, godMode);
	}

	/**
	 * Sets the last /back location for both EssentialsX and CMI
	 *
	 * @param player
	 * @param location
	 */
	public static void setBackLocation(final Player player, final Location location) {
		if (isEssentialsLoaded())
			essentialsHook.setBackLocation(player.getName(), location);

		if (isCMILoaded())
			CMIHook.setLastTeleportLocation(player, location);
	}

	/**
	 * Set EssentialsX and CMI ignored player
	 *
	 * @param player
	 * @param who
	 * @param ignore
	 */
	public static void setIgnore(final UUID player, final UUID who, final boolean ignore) {
		if (isEssentialsLoaded())
			essentialsHook.setIgnore(player, who, ignore);

		if (isCMILoaded())
			CMIHook.setIgnore(player, who, ignore);
	}

	/**
	 * Return true if the player is ignoring another player in EssentialsX
	 *
	 * @param player
	 * @param who
	 * @return
	 */
	public static boolean isIgnoring(final UUID player, final UUID who) {
		Valid.checkBoolean(player != null, "Player to check ignore from cannot be null/empty");
		Valid.checkBoolean(who != null, "Player to check ignore to cannot be null/empty");

		return isEssentialsLoaded() ? essentialsHook.isIgnoring(player, who) : isCMILoaded() ? CMIHook.isIgnoring(player, who) : false;
	}

	/**
	 * Returns the nick for the given recipient from Essentials or Nicky, or if it's a console, their name
	 *
	 * @param sender
	 * @return
	 */
	public static String getNickColored(final CommandSender sender) {
		return getNick(sender, false);
	}

	/**
	 * Returns the nick for the given recipient from Essentials or Nicky, or if it's a console, their name
	 *
	 * @param sender
	 * @return
	 */
	public static String getNickColorless(final CommandSender sender) {
		return getNick(sender, true);
	}

	/**
	 * Returns the nick for the given recipient from Essentials or Nicky, or if it's a console, their name
	 *
	 * @param sender
	 * @param stripColors
	 *
	 * @return
	 */
	private static String getNick(final CommandSender sender, boolean stripColors) {
		final Player player = sender instanceof Player ? (Player) sender : null;

		if (player != null && isNPC(player)) {
			Common.log("&eWarn: Called getNick for NPC " + player.getName() + "! Notify the developers to add an ignore check at " + Debugger.traceRoute(true));

			return player.getName();
		}

		if (player == null)
			return sender.getName();

		final String nickyNick = isNickyLoaded() ? nickyHook.getNick(player) : null;
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNick(player.getName()) : null;
		final String cmiNick = isCMILoaded() ? CMIHook.getNick(player) : null;

		final String nick = nickyNick != null ? nickyNick : cmiNick != null ? cmiNick : essNick != null ? essNick : sender.getName();

		return stripColors ? Common.stripColors(Common.revertColorizing(nick).replace(ChatColor.COLOR_CHAR + "x", "")) : nick;
	}

	/**
	 * Attempts to find a nick from the given player name, defaulting to the given name if null
	 * We support EssentialsX and CMI only.
	 *
	 * @param playerName
	 * @return
	 */
	public static String getNickFromName(final String playerName) {
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNick(playerName) : null;
		final String cmiNick = isCMILoaded() ? CMIHook.getNick(playerName) : null;

		return cmiNick != null ? cmiNick : essNick != null ? essNick : playerName;
	}

	/**
	 * Sets the nickname for Essentials and CMI if installed for the given target player
	 *
	 * @param playerId
	 * @param nick
	 */
	public static void setNick(@NonNull final UUID playerId, @Nullable String nick) {
		if (isEssentialsLoaded())
			essentialsHook.setNick(playerId, nick);

		if (isCMILoaded())
			CMIHook.setNick(playerId, nick);
	}

	/**
	 * Attempts to reverse lookup player name from his nick
	 *
	 * Only Essentials and CMI are supported
	 *
	 * @param nick
	 * @return
	 */
	public static String getNameFromNick(@NonNull String nick) {
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNameFromNick(nick) : nick;
		final String cmiNick = isCMILoaded() ? CMIHook.getNameFromNick(nick) : nick;

		return !essNick.equals(nick) && !"".equals(essNick) ? essNick : !cmiNick.equals(nick) && !"".equals(cmiNick) ? cmiNick : nick;
	}

	// ------------------------------------------------------------------------------------------------------------
	// EssentialsX
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the reply recipient for the given player, or null if not exist
	 *
	 * @param player
	 * @return
	 */
	public static Player getReplyTo(final Player player) {
		return isEssentialsLoaded() ? essentialsHook.getReplyTo(player.getName()) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// ItemsAdder
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Use ItemsAdder to replace font images in the message
	 *
	 * @param message
	 * @return
	 */
	public static String replaceFontImages(final String message) {
		return replaceFontImages(null, message);
	}

	/**
	 * Use ItemsAdder to replace font images in the message based on the player's permission
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	public static String replaceFontImages(@Nullable Player player, final String message) {
		return isItemsAdderLoaded() ? itemsAdderHook.replaceFontImages(player, message) : message;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Multiverse-Core
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the world name alias from Multiverse-Core
	 *
	 * @param world
	 * @return
	 */
	public static String getWorldAlias(final World world) {
		return isMultiverseCoreLoaded() ? multiverseHook.getWorldAlias(world.getName()) : world.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Towny
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return players nation from Towny, or null if not loaded
	 *
	 * @param player
	 * @return
	 */
	public static String getNation(final Player player) {
		return isTownyLoaded() ? townyHook.getNationName(player) : null;
	}

	/**
	 * Return players town name from Towny, or null if none
	 *
	 * @param player
	 * @return
	 */
	public static String getTownName(final Player player) {
		return isTownyLoaded() ? townyHook.getTownName(player) : null;
	}

	/**
	 * Return the online residents in players town, or an empty list
	 *
	 * @param player
	 * @return
	 */
	public static Collection<? extends Player> getTownResidentsOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getTownResidentsOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the online nation players in players nation (Towny), or an empty list
	 *
	 * @param player
	 * @return
	 */
	public static Collection<? extends Player> getNationPlayersOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getNationPlayersOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the online nation players in players ally (Towny), or an empty list
	 *
	 * @param player
	 * @return
	 */
	public static Collection<? extends Player> getAllyPlayersOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getAllyPlayersOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the town owner name at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getTownOwner(final Location location) {
		return isTownyLoaded() ? townyHook.getTownOwner(location) : null;
	}

	/**
	 * Return the town name at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getTown(final Location location) {
		return isTownyLoaded() ? townyHook.getTownName(location) : null;
	}

	/**
	 * Return a list of all loaded towns, or an empty list if none
	 *
	 * @return
	 */
	public static List<String> getTowns() {
		return isTownyLoaded() ? townyHook.getTowns() : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Vault
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the Vault player prefix or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerPrefix(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerPrefix(player) : "";
	}

	/**
	 * Return the Vault player suffix or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerSuffix(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerSuffix(player) : "";
	}

	/**
	 * Return the Vault player permission group or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerPermissionGroup(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerGroup(player) : "";
	}

	/**
	 * Return the players balance from Vault (hooks into your economy plugin)
	 *
	 * @param player
	 * @return
	 */
	public static double getBalance(final Player player) {
		return isVaultLoaded() ? vaultHook.getBalance(player) : 0;
	}

	/**
	 * Return the singular currency name, or null if not loaded
	 *
	 * @return
	 */
	public static String getCurrencySingular() {
		return isVaultLoaded() ? vaultHook.getCurrencyNameSG() : null;
	}

	/**
	 * Return the plural currency name, or null if not loaded
	 *
	 * @return
	 */
	public static String getCurrencyPlural() {
		return isVaultLoaded() ? vaultHook.getCurrencyNamePL() : null;
	}

	/**
	 * Takes money from the player if Vault is installed
	 *
	 * @param player
	 * @param amount
	 */
	public static void withdraw(final Player player, final double amount) {
		if (isVaultLoaded())
			vaultHook.withdraw(player, amount);
	}

	/**
	 * Gives money to the player if Vault is installed
	 *
	 * @param player
	 * @param amount
	 */
	public static void deposit(final Player player, final double amount) {
		if (isVaultLoaded())
			vaultHook.deposit(player, amount);
	}

	/**
	 * Checks if the given player has the given permission, safe to use
	 * for instances where the player may be a {@link TemporaryPlayer} from
	 * ProtocolLib where then we use Vault to check the players perm
	 *
	 * @param player
	 * @param perm
	 * @return
	 */
	public static boolean hasProtocolLibPermission(Player player, String perm) {
		if (isProtocolLibLoaded() && protocolLibHook.isTemporaryPlayer(player))
			return hasVaultPermission(player, perm);

		return PlayerUtil.hasPerm(player, perm);
	}

	/**
	 * Checks if the given player name has a certain permission using vault
	 * Or throws an error if Vault is not present
	 *
	 * @param offlinePlayer
	 * @param perm
	 *
	 * @return
	 */
	public static boolean hasVaultPermission(final OfflinePlayer offlinePlayer, final String perm) {
		Valid.checkBoolean(isVaultLoaded(), "hasVaultPermission called - Please install Vault to enable this functionality!");

		return vaultHook.hasPerm(offlinePlayer, perm);
	}

	/**
	 * Returns the players primary permission group using Vault, or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static String getPlayerPrimaryGroup(final Player player) {
		return isVaultLoaded() ? vaultHook.getPrimaryGroup(player) : "";
	}

	/**
	 * Return true if Vault could find a suitable chat plugin to hook to
	 *
	 * @return
	 */
	public static boolean isChatIntegrated() {
		return isVaultLoaded() ? vaultHook.isChatIntegrated() : false;
	}

	/**
	 * Return true if Vault could find a suitable economy plugin to hook to
	 *
	 * @return
	 */
	public static boolean isEconomyIntegrated() {
		return isVaultLoaded() ? vaultHook.isEconomyIntegrated() : false;
	}

	/**
	 * Updates Vault service providers
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void updateVaultIntegration() {
		if (isVaultLoaded())
			vaultHook.setIntegration();
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlaceholderAPI / MVdWPlaceholderAPI
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Uses PlaceholderAPI and MVdWPlaceholderAPI to replace placeholders in a message
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	public static String replacePlaceholders(final @Nullable OfflinePlayer player, String message) {
		if (message == null || "".equals(message.trim()))
			return message;

		message = isPlaceholderAPILoaded() ? placeholderAPIHook.replacePlaceholders(player, message) : message;
		message = isMVdWPlaceholderAPILoaded() ? MVdWPlaceholderHook.replacePlaceholders(player, message) : message;

		return message;
	}

	/**
	 * Uses PlaceholderAPI to replace relation placeholders in a message
	 *
	 * @param one
	 * @param two
	 * @param message
	 * @return
	 */
	public static String replaceRelationPlaceholders(final Player one, final Player two, final String message) {
		if (message == null || "".equals(message.trim()))
			return message;

		return isPlaceholderAPILoaded() ? placeholderAPIHook.replaceRelationPlaceholders(one, two, message) : message;
	}

	/**
	 * If PlaceholderAPI is loaded, registers a new placeholder within it
	 * with the given variable and value.
	 * <p>
	 * 		The variable is automatically prepended with your plugin name, lowercased + _,
	 * 		such as chatcontrol_ or boss_ + your variable.
	 * <p>
	 * 		Example if the variable is player health in ChatControl plugin: "chatcontrol_health"
	 * <p>
	 * 		The value will be called against the given player
	 * <p>
	 *
	 * 	 * ATTENTION: We now have a new system where you register variables through {@link Variables#addExpansion(SimpleExpansion)}
	 * 			   instead. It gives you better flexibility and, like PlaceholderAPI, you can replace different variables on the fly.
	 *
	 * @param variable
	 * @param value
	 */
	public static void addPlaceholder(final String variable, final Function<Player, String> value) {
		Variables.addExpansion(new SimpleExpansion() {

			@Override
			protected String onReplace(@NonNull CommandSender sender, String identifier) {
				return variable.equalsIgnoreCase(identifier) && sender instanceof Player ? value.apply((Player) sender) : null;
			}
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Factions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get all loaded Factions or null if none
	 *
	 * @return
	 */
	public static Collection<String> getFactions() {
		return isFactionsLoaded() ? factionsHook.getFactions() : null;
	}

	/**
	 * Return the players faction or null if none
	 *
	 * @param player
	 * @return
	 */
	public static String getFaction(final Player player) {
		return isFactionsLoaded() ? factionsHook.getFaction(player) : null;
	}

	/**
	 * Return players in players faction or empty if none
	 *
	 * @param player
	 * @return
	 */
	public static Collection<? extends Player> getOnlineFactionPlayers(final Player player) {
		return isFactionsLoaded() ? factionsHook.getSameFactionPlayers(player) : new ArrayList<>();
	}

	/**
	 * Return a faction name at the given location, or null
	 *
	 * @param location
	 * @return
	 */
	public static String getFaction(final Location location) {
		return isFactionsLoaded() ? factionsHook.getFaction(location) : null;
	}

	/**
	 * Return the faction owner name at the given location, or null
	 *
	 * @param location
	 * @return
	 */
	public static String getFactionOwner(final Location location) {
		return isFactionsLoaded() ? factionsHook.getFactionOwner(location) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// ProtocolLib
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a {@link PacketAdapter} packet listener to ProtocolLib.
	 * <p>
	 * If the plugin is missing, an error will be thrown
	 *
	 * @param adapter
	 */
	public static void addPacketListener(/*Uses object to prevent errors if plugin is not installed*/final Object adapter) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Cannot add packet listeners if ProtocolLib isn't installed");

		protocolLibHook.addPacketListener(adapter);
	}

	/**
	 * Send a {@link PacketContainer} to the given player
	 *
	 * @param player
	 * @param packetContainer
	 */
	public static void sendPacket(final Player player, final Object packetContainer) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Sending packets requires ProtocolLib installed and loaded");

		protocolLibHook.sendPacket(player, packetContainer);
	}

	// ------------------------------------------------------------------------------------------------------------
	// LWC
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the LWC owner of the block, or null
	 *
	 * @param block
	 * @return
	 */
	public static String getLWCOwner(final Block block) {
		return isLWCLoaded() ? lwcHook.getOwner(block) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Lockette Pro
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return if the given player owns the given block from Lockette Pro
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	public static boolean isLocketteOwner(final Block block, final Player player) {
		return isLocketteProLoaded() ? locketteProHook.isOwner(block, player) : false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Residence
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a list of Residence residences at the given location or an empty list
	 *
	 * @return
	 */
	public static Collection<String> getResidences() {
		return isResidenceLoaded() ? residenceHook.getResidences() : new ArrayList<>();
	}

	/**
	 * Get the Residence name at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getResidence(final Location location) {
		return isResidenceLoaded() ? residenceHook.getResidence(location) : null;
	}

	/**
	 * Get the Residence owner at the given location or null if none
	 *
	 * @param location
	 * @return
	 */
	public static String getResidenceOwner(final Location location) {
		return isResidenceLoaded() ? residenceHook.getResidenceOwner(location) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// WorldGuard
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return WorldGuard list of regions at the given location or an empty list
	 *
	 * @param loc
	 * @return
	 */
	public static List<String> getRegions(final Location loc) {
		return isWorldGuardLoaded() ? worldguardHook.getRegionsAt(loc) : new ArrayList<>();
	}

	/**
	 * Return WorldGuard list of loaded regions or an empty list
	 *
	 * @return
	 */
	public static List<String> getRegions() {
		return isWorldGuardLoaded() ? worldguardHook.getAllRegions() : new ArrayList<>();
	}

	/**
	 * Get our representation of a worldguard region by its name or null
	 *
	 * @param name
	 * @return
	 */
	public static Region getRegion(final String name) {
		return isWorldGuardLoaded() ? worldguardHook.getRegion(name) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlotSquared
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get a list of players inside a PlotSquared plot, or empty if not loaded
	 *
	 * @param players
	 * @return
	 */
	public static Collection<? extends Player> getPlotPlayers(final Player players) {
		return isPlotSquaredLoaded() ? plotSquaredHook.getPlotPlayers(players) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// mcMMO
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the active mcMMO party chat
	 *
	 * @param player
	 * @return
	 */
	public static String getActivePartyChat(final Player player) {
		return isMcMMOLoaded() ? mcmmoHook.getActivePartyChat(player) : null;
	}

	/**
	 * Return the online residents in player's party, or an empty list
	 *
	 * @param player
	 * @return
	 */
	public static List<Player> getMcMMOPartyRecipients(final Player player) {
		return isMcMMOLoaded() ? mcmmoHook.getPartyRecipients(player) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Citizens
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the entity is a Citizens NPC
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isNPC(final Entity entity) {
		return isCitizensLoaded() ? citizensHook.isNPC(entity) : false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// DiscordSRV
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return all linked Discord channels. You can link those in DiscordSRV config.yml file
	 *
	 * @return the linked channels or an empty set when DiscordSRV is not loaded
	 */
	public static Set<String> getDiscordChannels() {
		return isDiscordSRVLoaded() ? discordSRVHook.getChannels() : new HashSet<>();
	}

	/**
	 * Sends a message from the given sender to a certain channel on Discord using DiscordSRV
	 * <p>
	 * Enhanced functionality is available if the sender is a player
	 *
	 * @param sender
	 * @param channel
	 * @param message
	 */
	public static void sendDiscordMessage(final CommandSender sender, final String channel, @NonNull final String message) {
		if (isDiscordSRVLoaded() && !Common.stripColors(message).isEmpty())
			discordSRVHook.sendMessage(sender, channel, message);
	}

	/**
	 * Send a message to a Discord channel if DiscordSRV is installed
	 *
	 * @param channel
	 * @param message
	 */
	public static void sendDiscordMessage(final String channel, @NonNull final String message) {
		if (isDiscordSRVLoaded() && !Common.stripColors(message).isEmpty())
			discordSRVHook.sendMessage(channel, message);
	}
}

// ------------------------------------------------------------------------------------------------------------
//
// Below are the individual classes responsible for hooking into third party plugins
// and getting data from them. Due to often changes we do not keep those documented.
//
// ------------------------------------------------------------------------------------------------------------

class AuthMeHook {

	boolean isLogged(final Player player) {
		try {
			final AuthMeApi instance = AuthMeApi.getInstance();

			return instance.isAuthenticated(player);
		} catch (final Throwable t) {
			return false;
		}
	}
}

class EssentialsHook {

	private final Essentials ess;

	EssentialsHook() {
		ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}

	boolean hasGodMode(final Player player) {
		final User user = getUser(player.getName());

		return user != null ? user.isGodModeEnabled() : false;
	}

	void setGodMode(final Player player, final boolean godMode) {
		final User user = getUser(player.getName());

		if (user != null)
			user.setGodModeEnabled(godMode);
	}

	void setIgnore(final UUID player, final UUID toIgnore, final boolean ignore) {
		try {
			final com.earth2me.essentials.User user = ess.getUser(player);
			final com.earth2me.essentials.User toIgnoreUser = ess.getUser(toIgnore);

			if (toIgnoreUser != null)
				user.setIgnoredPlayer(toIgnoreUser, ignore);

		} catch (final Throwable t) {
		}
	}

	boolean isIgnoring(final UUID player, final UUID ignoringPlayer) {
		try {
			final com.earth2me.essentials.User user = ess.getUser(player);
			final com.earth2me.essentials.User ignored = ess.getUser(ignoringPlayer);

			return user != null && ignored != null && user.isIgnoredPlayer(ignored);

		} catch (final Throwable t) {
			return false;
		}
	}

	boolean isAfk(final String pl) {
		final IUser user = getUser(pl);

		return user != null ? user.isAfk() : false;
	}

	boolean isVanished(final String pl) {
		final IUser user = getUser(pl);

		return user != null ? user.isVanished() : false;
	}

	void setVanished(final String playerName, boolean vanished) {
		final IUser user = getUser(playerName);

		if (user != null && user.isVanished() != vanished)
			user.setVanished(false);
	}

	boolean isMuted(final String pl) {
		final com.earth2me.essentials.User user = getUser(pl);

		return user != null ? user.isMuted() : false;
	}

	Player getReplyTo(final String recipient) {
		final User user = getUser(recipient);

		if (user == null)
			return null;

		String replyPlayer = null;

		try {
			replyPlayer = user.getReplyRecipient().getName();

		} catch (final Throwable ex) {
			try {
				final Method getReplyTo = ReflectionUtil.getMethod(user.getClass(), "getReplyTo");

				if (getReplyTo != null) {
					final CommandSource commandSource = ReflectionUtil.invoke(getReplyTo, user);

					replyPlayer = commandSource == null ? null : commandSource.getPlayer().getName();
				}

			} catch (final Throwable t) {
				replyPlayer = null;
			}
		}

		final Player bukkitPlayer = replyPlayer == null ? null : Bukkit.getPlayer(replyPlayer);

		if (bukkitPlayer != null && bukkitPlayer.isOnline())
			return bukkitPlayer;

		return null;
	}

	String getNick(final String player) {
		final User user = getUser(player);

		if (user == null) {
			//Common.log("&cMalfunction getting Essentials user. Have you reloaded?");

			return player;
		}

		final String essNick = Common.getOrEmpty(user.getNickname());

		return "".equals(essNick) ? null : essNick;
	}

	void setNick(final UUID uniqueId, String nick) {
		final User user = getUser(uniqueId);

		if (user != null) {
			final boolean isEmpty = nick == null || Common.stripColors(nick).replace(" ", "").isEmpty();

			user.setNickname(isEmpty ? null : Common.colorize(nick));
		}
	}

	String getNameFromNick(final String maybeNick) {
		final UserMap users = ess.getUserMap();

		if (users != null)
			for (final UUID userId : users.getAllUniqueUsers()) {
				final User user = users.getUser(userId);

				if (user != null && user.getNickname() != null && Valid.colorlessEquals(user.getNickname(), maybeNick))
					return Common.getOrDefault(user.getName(), maybeNick);
			}

		return maybeNick;
	}

	void setBackLocation(final String player, final Location loc) {
		final User user = getUser(player);

		if (user != null)
			try {
				user.setLastLocation(loc);

			} catch (final Throwable t) {
			}
	}

	private User getUser(final String name) {
		if (ess.getUserMap() == null)
			return null;

		User user = null;

		try {
			user = ess.getUserMap().getUser(name);
		} catch (final Throwable t) {
		}

		if (user == null)
			try {
				user = ess.getUserMap().getUserFromBukkit(name);

			} catch (final Throwable ex) {
				user = ess.getUser(name);
			}
		return user;
	}

	private User getUser(final UUID uniqueId) {
		if (ess.getUserMap() == null)
			return null;

		User user = null;

		try {
			user = ess.getUserMap().getUser(uniqueId);
		} catch (final Throwable t) {
		}

		if (user == null)
			try {
				user = ess.getUser(uniqueId);
			} catch (final Throwable ex) {
			}

		return user;
	}

}

class MultiverseHook {

	private final MultiverseCore multiVerse;

	MultiverseHook() {
		multiVerse = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
	}

	String getWorldAlias(final String world) {
		final MultiverseWorld mvWorld = multiVerse.getMVWorldManager().getMVWorld(world);

		if (mvWorld != null)
			return mvWorld.getColoredWorldString();

		return world;
	}
}

class TownyHook {

	Collection<? extends Player> getTownResidentsOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playersTown = getTownName(pl);

		if (!playersTown.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playersTown.equals(getTownName(online)))
					recipients.add(online);

		return recipients;
	}

	Collection<? extends Player> getNationPlayersOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playerNation = getNationName(pl);

		if (!playerNation.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playerNation.equals(getNationName(online)))
					recipients.add(online);

		return recipients;
	}

	Collection<? extends Player> getAllyPlayersOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final Resident resident = getResident(pl);

		if (resident != null)
			for (final Player online : Remain.getOnlinePlayers()) {
				final Resident otherResident = getResident(online);

				if (otherResident != null && otherResident.isAlliedWith(resident))
					recipients.add(online);
			}

		return recipients;
	}

	String getTownName(final Player pl) {
		final Town t = getTown(pl);

		return t != null ? t.getName() : "";
	}

	String getNationName(final Player pl) {
		final Nation n = getNation(pl);

		return n != null ? n.getName() : "";
	}

	List<String> getTowns() {
		try {
			//import com.palmergames.bukkit.towny.object.TownyUniverse;

			return Common.convert(TownyUniverse.getInstance().getTowns(), Town::getName);

		} catch (final Throwable e) {
			return new ArrayList<>();
		}
	}

	String getTownName(final Location loc) {
		final Town town = getTown(loc);

		return town != null ? town.getName() : null;
	}

	private Town getTown(final Location loc) {
		try {
			final WorldCoord worldCoord = WorldCoord.parseWorldCoord(loc);
			final TownBlock townBlock = TownyUniverse.getInstance().getTownBlock(worldCoord);

			return townBlock != null ? townBlock.getTown() : null;

		} catch (final Throwable e) {
			return null;
		}
	}

	String getTownOwner(final Location loc) {
		try {
			final Town town = getTown(loc);

			return town != null ? town.getMayor().getName() : null;

		} catch (final Throwable e) {
			return null;
		}
	}

	private Nation getNation(final Player pl) {
		final Town town = getTown(pl);

		try {
			return town.getNation();

		} catch (final Throwable ex) {
			return null;
		}
	}

	private Town getTown(final Player pl) {
		final Resident res = getResident(pl);

		try {
			return res.getTown();

		} catch (final Throwable ex) {
			return null;
		}
	}

	private Resident getResident(final Player player) {
		try {
			return TownyUniverse.getInstance().getResident(player.getName());

		} catch (final Throwable e) {
			return null;
		}
	}
}

class ProtocolLibHook {

	private final ProtocolManager manager;
	private final StrictSet<Object> registeredListeners = new StrictSet<>();

	ProtocolLibHook() {
		manager = ProtocolLibrary.getProtocolManager();
	}

	final void addPacketListener(final Object listener) {
		Valid.checkBoolean(listener instanceof PacketListener, "Listener must extend or implements PacketListener or PacketAdapter");

		try {
			manager.addPacketListener((PacketListener) listener);

		} catch (final Throwable t) {
			Common.error(t, "Failed to register ProtocolLib packet listener! Ensure you have the latest ProtocolLib. If you reloaded, try a fresh startup (some ProtocolLib esp. for 1.8.8 fails on reload).");

			return;
		}

		registeredListeners.add(listener);
	}

	final void removePacketListeners(final Plugin plugin) {
		manager.removePacketListeners(plugin);

		registeredListeners.clear();
	}

	final void sendPacket(final PacketContainer packet) {
		for (final Player player : Remain.getOnlinePlayers())
			sendPacket(player, packet);
	}

	final void sendPacket(final Player player, final Object packet) {
		Valid.checkNotNull(player);
		Valid.checkBoolean(packet instanceof PacketContainer, "Packet must be instance of PacketContainer from ProtocolLib");

		try {
			manager.sendServerPacket(player, (PacketContainer) packet);

		} catch (final InvocationTargetException e) {
			Common.error(e, "Failed to send " + ((PacketContainer) packet).getType() + " packet to " + player.getName());
		}
	}

	final boolean isTemporaryPlayer(Player player) {
		try {
			return player instanceof TemporaryPlayer;

		} catch (final NoClassDefFoundError err) {
			return false;
		}
	}
}

class VaultHook {

	private Chat chat;
	private Economy economy;
	private Permission permissions;

	VaultHook() {
		setIntegration();
	}

	void setIntegration() {
		final RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		final RegisteredServiceProvider<Chat> chatProvider = Bukkit.getServicesManager().getRegistration(Chat.class);
		final RegisteredServiceProvider<Permission> permProvider = Bukkit.getServicesManager().getRegistration(Permission.class);

		if (economyProvider != null)
			economy = economyProvider.getProvider();

		if (chatProvider != null)
			chat = chatProvider.getProvider();

		if (permProvider != null)
			permissions = permProvider.getProvider();
	}

	boolean isChatIntegrated() {
		return chat != null;
	}

	boolean isEconomyIntegrated() {
		return economy != null;
	}

	// ------------------------------------------------------------------------------
	// Economy
	// ------------------------------------------------------------------------------

	String getCurrencyNameSG() {
		return economy != null ? Common.getOrEmpty(economy.currencyNameSingular()) : "Money";
	}

	String getCurrencyNamePL() {
		return economy != null ? Common.getOrEmpty(economy.currencyNamePlural()) : "Money";
	}

	double getBalance(final Player player) {
		return economy != null ? economy.getBalance(player) : -1;
	}

	void withdraw(final Player player, final double amount) {
		if (economy != null)
			economy.withdrawPlayer(player.getName(), amount);
	}

	void deposit(final Player player, final double amount) {
		if (economy != null)
			economy.depositPlayer(player.getName(), amount);
	}

	// ------------------------------------------------------------------------------
	// Permissions
	// ------------------------------------------------------------------------------

	Boolean hasPerm(@NonNull final OfflinePlayer player, final String perm) {
		try {
			return permissions != null ? perm != null ? permissions.playerHas((String) null, player, perm) : true : null;

		} catch (final Throwable t) {
			Common.logTimed(900,
					"SEVERE: Unable to ask Vault plugin if " + player.getName() + " has " + perm + " permission, returning false. "
							+ "This error only shows every 15 minutes. "
							+ "Run /vault-info and check if your permissions plugin is running correctly.");

			return false;
		}
	}

	Boolean hasPerm(@NonNull final String player, final String perm) {
		try {
			return permissions != null ? perm != null ? permissions.has((String) null, player, perm) : true : null;
		} catch (final UnsupportedOperationException t) {
			return false; // No supported plugin installed
		}
	}

	Boolean hasPerm(@NonNull final String world, @NonNull final String player, final String perm) {
		try {
			return permissions != null ? perm != null ? permissions.has(world, player, perm) : true : null;
		} catch (final UnsupportedOperationException t) {
			return false; // No supported plugin installed
		}
	}

	String getPrimaryGroup(final Player player) {
		try {
			return permissions != null ? permissions.getPrimaryGroup(player) : "";

		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed
		}
	}

	// ------------------------------------------------------------------------------
	// Prefix / Suffix
	// ------------------------------------------------------------------------------

	String getPlayerPrefix(final Player player) {
		try {
			return lookupVault(player, VaultPart.PREFIX);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed
		}
	}

	String getPlayerSuffix(final Player player) {
		try {
			return lookupVault(player, VaultPart.SUFFIX);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed
		}
	}

	String getPlayerGroup(final Player player) {
		try {
			return lookupVault(player, VaultPart.GROUP);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed
		}
	}

	private String lookupVault(final Player player, final VaultPart vaultPart) {
		if (chat == null)
			return "";

		final String[] groups = chat.getPlayerGroups(player);
		String fallback = vaultPart == VaultPart.PREFIX ? chat.getPlayerPrefix(player) : vaultPart == VaultPart.SUFFIX ? chat.getPlayerSuffix(player) : groups != null && groups.length > 0 ? groups[0] : "";

		if (fallback == null)
			fallback = "";

		if (vaultPart == VaultPart.PREFIX /*&& !SimplePlugin.getInstance().vaultMultiPrefix()*/ || vaultPart == VaultPart.SUFFIX /*&& !SimplePlugin.getInstance().vaultMultiSuffix()*/)
			return fallback;

		final List<String> list = new ArrayList<>();

		if (!fallback.isEmpty())
			list.add(fallback);

		if (groups != null)
			for (final String group : groups) {
				final String part = vaultPart == VaultPart.PREFIX ? chat.getGroupPrefix(player.getWorld(), group) : vaultPart == VaultPart.SUFFIX ? chat.getGroupSuffix(player.getWorld(), group) : group;

				if (part != null && !part.isEmpty() && !list.contains(part))
					list.add(part);
			}

		return Common.join(list, vaultPart == VaultPart.GROUP ? ", " : "");
	}

	enum VaultPart {
		PREFIX,
		SUFFIX,
		GROUP,
	}
}

class PlaceholderAPIHook {

	private static volatile VariablesInjector injector;

	PlaceholderAPIHook() {
		try {
			injector = new VariablesInjector();
			injector.register();

		} catch (final Throwable throwable) {
			Common.error(throwable, "Failed to inject our variables into PlaceholderAPI!");
		}
	}

	final void unregister() {
		if (injector != null)
			try {
				injector.unregister();

			} catch (final Throwable t) {
				// Silence, probably plugin got removed in the meantime
			}
	}

	final String replacePlaceholders(final OfflinePlayer player, final String msg) {
		try {
			return setPlaceholders(player, msg);

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace variables!",
					"Player: " + (player == null ? "none" : player.getName()),
					"Message: " + msg,
					"Error: %error");

			return msg;
		}
	}

	private String setPlaceholders(final OfflinePlayer player, String text) {
		final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();

		if (hooks.isEmpty())
			return text;

		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(text);

		while (matcher.find()) {
			String format = matcher.group(1);
			boolean frontSpace = false;
			boolean backSpace = false;

			if (format.startsWith("+")) {
				frontSpace = true;

				format = format.substring(1);
			}

			if (format.endsWith("+")) {
				backSpace = true;

				format = format.substring(0, format.length() - 1);
			}

			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index);
			final String params = format.substring(index + 1);

			if (hooks.containsKey(identifier)) {

				// Wait 0.5 seconds then kill the thread to prevent server
				// crashing on PlaceholderAPI variables hanging up on the main thread
				final Thread currentThread = Thread.currentThread();
				final BukkitTask watchDog = Common.runLater(20, () -> {
					Common.logFramed(
							"IMPORTANT: PREVENTED SERVER CRASH FROM PLACEHOLDERAPI",
							"Replacing a variable using PlaceholderAPI took",
							"longer than our maximum limit (1 second) and",
							"was forcefully interrupted to prevent your",
							"server from crashing. This is not error on",
							"our end, please contact the expansion author.",
							"",
							"Variable: " + identifier,
							"Player: " + (player == null ? "none" : player.getName()));

					currentThread.stop();
				});

				String value = hooks.get(identifier).onRequest(player, params);

				// Indicate we no longer have to kill the thread
				watchDog.cancel();

				if (value != null) {
					value = Matcher.quoteReplacement(Common.colorize(value));

					text = text.replaceAll(Pattern.quote(matcher.group()), value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : ""));
				}
			}
		}

		return text;
	}

	final String replaceRelationPlaceholders(final Player one, final Player two, final String message) {
		try {
			return setRelationalPlaceholders(one, two, message);

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace relation variables!",
					"Player one: " + one,
					"Player two: " + two,
					"Message: " + message,
					"Error: %error");

			return message;
		}
	}

	private String setRelationalPlaceholders(final Player one, final Player two, String text) {
		final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();

		if (hooks.isEmpty())
			return text;

		final Matcher matcher = Variables.BRACKET_REL_PLACEHOLDER_PATTERN.matcher(text);

		while (matcher.find()) {
			final String format = matcher.group(2);
			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index);
			final String params = format.substring(index + 1);

			if (hooks.containsKey(identifier)) {
				if (!(hooks.get(identifier) instanceof Relational))
					continue;

				final Relational rel = (Relational) hooks.get(identifier);
				final String value = one != null && two != null ? rel.onPlaceholderRequest(one, two, params) : "";

				if (value != null)
					text = text.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement(Common.colorize(value)));
			}
		}

		return text;
	}

	private class VariablesInjector extends PlaceholderExpansion {

		/**
		 * Because this is an internal class,
		 * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
		 * PlaceholderAPI is reloaded
		 *
		 * @return true to persist through reloads
		 */
		@Override
		public boolean persist() {
			return true;
		}

		/**
		 * Because this is a internal class, this check is not needed
		 * and we can simply return {@code true}
		 *
		 * @return Always true since it's an internal class.
		 */
		@Override
		public boolean canRegister() {
			return true;
		}

		/**
		 * The name of the person who created this expansion should go here.
		 * <br>For convienience do we return the author from the plugin.yml
		 *
		 * @return The name of the author as a String.
		 */
		@Override
		public String getAuthor() {
			return SimplePlugin.getInstance().getDescription().getAuthors().toString();
		}

		/**
		 * The placeholder identifier should go here.
		 * <br>This is what tells PlaceholderAPI to call our onRequest
		 * method to obtain a value if a placeholder starts with our
		 * identifier.
		 * <br>This must be unique and can not contain % or _
		 *
		 * @return The identifier in {@code %<identifier>_<value>%} as String.
		 */
		@Override
		public String getIdentifier() {
			return SimplePlugin.getNamed().toLowerCase().replace("%", "").replace(" ", "").replace("_", "");
		}

		/**
		 * This is the version of the expansion.
		 * <br>You don't have to use numbers, since it is set as a String.
		 * <p>
		 * For convenience do we return the version from the plugin.yml
		 *
		 * @return The version as a String.
		 */
		@Override
		public String getVersion() {
			return SimplePlugin.getInstance().getDescription().getVersion();
		}

		/**
		 * Replace Foundation variables but with our plugin name added as prefix
		 *
		 * We return null if an invalid placeholder (f.e. %ourplugin_nonexistingplaceholder%) is provided
		 */
		@Override
		public String onRequest(OfflinePlayer offlinePlayer, @NonNull String identifier) {
			final Player player = offlinePlayer != null ? offlinePlayer.getPlayer() : null;

			if (player == null || !player.isOnline())
				return null;

			final boolean frontSpace = identifier.startsWith("+");
			final boolean backSpace = identifier.endsWith("+");

			identifier = frontSpace ? identifier.substring(1) : identifier;
			identifier = backSpace ? identifier.substring(0, identifier.length() - 1) : identifier;

			final Function<CommandSender, String> variable = Variables.getVariable(identifier);

			try {
				if (variable != null) {
					final String value = variable.apply(player);

					if (value != null)
						return value;
				}

				for (final SimpleExpansion expansion : Variables.getExpansions()) {
					final String value = expansion.replacePlaceholders(player, identifier);

					if (value != null) {
						final boolean emptyColorless = Common.stripColors(value).isEmpty();

						return (!value.isEmpty() && frontSpace && !emptyColorless ? " " : "") + value + (!value.isEmpty() && backSpace && !emptyColorless ? " " : "");
					}
				}

			} catch (final Exception ex) {
				Common.error(ex,
						"Error replacing PlaceholderAPI variables",
						"Identifier: " + identifier,
						"Player: " + player.getName());
			}

			return null;
		}
	}
}

class NickyHook {

	NickyHook() {
	}

	String getNick(final Player player) {
		final Constructor<?> nickConstructor = ReflectionUtil.getConstructor("io.loyloy.nicky.Nick", Player.class);
		final Object nick = ReflectionUtil.instantiate(nickConstructor, player);
		String nickname = ReflectionUtil.invoke("get", nick);

		if (nickname != null) {
			final Method formatMethod = ReflectionUtil.getMethod(nick.getClass(), "format", String.class);

			if (formatMethod != null)
				nickname = ReflectionUtil.invoke(formatMethod, nick, nickname);
		}

		return nickname != null && !nickname.isEmpty() ? nickname : null;
	}
}

class MVdWPlaceholderHook {

	MVdWPlaceholderHook() {
	}

	String replacePlaceholders(@Nullable OfflinePlayer player, final String message) {

		if (player == null)
			return message;

		try {
			final Class<?> placeholderAPI = ReflectionUtil.lookupClass("be.maximvdw.placeholderapi.PlaceholderAPI");
			Valid.checkNotNull(placeholderAPI, "Failed to look up class be.maximvdw.placeholderapi.PlaceholderAPI");

			final Method replacePlaceholders = ReflectionUtil.getMethod(placeholderAPI, "replacePlaceholders", OfflinePlayer.class, String.class);
			Valid.checkNotNull(replacePlaceholders, "Failed to look up method PlaceholderAPI#replacePlaceholders(Player, String)");

			final String replaced = ReflectionUtil.invoke(replacePlaceholders, null, player, message);

			return replaced == null ? "" : replaced;

		} catch (final IllegalArgumentException ex) {
			if (!Common.getOrEmpty(ex.getMessage()).contains("Illegal group reference"))
				ex.printStackTrace();

		} catch (final Throwable t) {
			Common.error(t,
					"MvdWPlaceholders placeholders failed!",
					"Player: " + player.getName(),
					"Message: '" + message + "'",
					"Consider writing to developer of that library",
					"first as this may be a bug we cannot handle!",
					"",
					"Your chat message will appear without replacements.");
		}

		return message;
	}
}

class LWCHook {

	private final Class<?> mainClass;
	private final boolean enabled;

	private final Object instance;
	private final Method findProtection;

	LWCHook() {
		this.mainClass = ReflectionUtil.lookupClass("com.griefcraft.lwc.LWC");
		this.enabled = (boolean) ReflectionUtil.getStaticFieldContent(this.mainClass, "ENABLED");

		this.instance = ReflectionUtil.invokeStatic(this.mainClass, "getInstance");
		this.findProtection = ReflectionUtil.getMethod(this.mainClass, "findProtection", Block.class);
	}

	String getOwner(final Block block) {
		if (!this.enabled)
			return null;

		final Object protection = ReflectionUtil.invoke(this.findProtection, this.instance, block);

		if (protection != null) {
			final Object ownerUid = ReflectionUtil.invoke("getOwner", protection);

			if (ownerUid != null) {
				final OfflinePlayer offlinePlayer = Remain.getOfflinePlayerByUUID(UUID.fromString(ownerUid.toString()));

				if (offlinePlayer != null)
					return offlinePlayer.getName();
			}
		}

		return null;
	}
}

class LocketteProHook {

	boolean isOwner(final Block block, final Player player) {
		final Class<?> locketteProAPI = ReflectionUtil.lookupClass("me.crafter.mc.lockettepro.LocketteProAPI");
		final Method isProtected = ReflectionUtil.getMethod(locketteProAPI, "isProtected", Block.class);
		final Method isOwner = ReflectionUtil.getMethod(locketteProAPI, "isOwner", Block.class, Player.class);

		return (boolean) ReflectionUtil.invoke(isProtected, null, block) ? ReflectionUtil.invoke(isOwner, null, block, player) : false;
	}
}

class ResidenceHook {

	public Collection<String> getResidences() {
		return Residence.getInstance().getResidenceManager().getResidences().keySet();
	}

	public String getResidence(final Location loc) {
		final ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);

		if (res != null)
			return res.getName();

		return null;
	}

	public String getResidenceOwner(final Location loc) {
		final ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);

		if (res != null)
			return res.getOwner();

		return null;
	}
}

class WorldEditHook {

	public final boolean legacy;

	public WorldEditHook() {
		boolean ok = false;
		try {
			Class.forName("com.sk89q.worldedit.world.World");
			ok = true;
		} catch (final ClassNotFoundException e) {
		}

		legacy = !ok;
	}
}

class WorldGuardHook {

	private final boolean legacy;

	public WorldGuardHook(final WorldEditHook we) {
		final Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");

		legacy = !wg.getDescription().getVersion().startsWith("7") || we != null && we.legacy;
	}

	public List<String> getRegionsAt(final Location location) {
		final List<String> list = new ArrayList<>();

		getApplicableRegions(location).forEach(region -> {
			final String name = Common.stripColors(region.getId());

			if (!name.startsWith("__"))
				list.add(name);
		});

		return list;
	}

	public Region getRegion(final String name) {
		for (final World w : Bukkit.getWorlds()) {
			final Object rm = getRegionManager(w);
			if (legacy)
				try {

					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;

						if (Common.stripColors(((ProtectedRegion) regObj).getId()).equals(name)) {

							final Class<?> clazz = regObj.getClass();
							final Method getMax = clazz.getMethod("getMaximumPoint");
							final Method getMin = clazz.getMethod("getMinimumPoint");

							final Object regMax = getMax.invoke(regObj);
							final Object regMin = getMin.invoke(regObj);

							final Class<?> vectorClass = Class.forName("com.sk89q.worldedit.BlockVector");
							final Method getX = vectorClass.getMethod("getX");
							final Method getY = vectorClass.getMethod("getY");
							final Method getZ = vectorClass.getMethod("getZ");

							final Location locMax;
							final Location locMin;
							locMax = new Location(w, (Double) getX.invoke(regMax), (Double) getY.invoke(regMax), (Double) getZ.invoke(regMax));
							locMin = new Location(w, (Double) getX.invoke(regMin), (Double) getY.invoke(regMin), (Double) getZ.invoke(regMin));

							return new Region(name, locMin, locMax);
						}
					}

				} catch (final Throwable t) {
					t.printStackTrace();

					throw new FoException("Failed WorldEdit 6 legacy hook, see above & report");
				}
			else
				for (final ProtectedRegion reg : ((com.sk89q.worldguard.protection.managers.RegionManager) rm).getRegions().values())
					if (reg != null && reg.getId() != null && Common.stripColors(reg.getId()).equals(name)) {
						//if(reg instanceof com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion) {
						// just going to pretend that everything is a cuboid..
						final Location locMax;
						final Location locMin;
						final com.sk89q.worldedit.math.BlockVector3 regMax = reg.getMaximumPoint();
						final com.sk89q.worldedit.math.BlockVector3 regMin = reg.getMinimumPoint();

						locMax = new Location(w, regMax.getX(), regMax.getY(), regMax.getZ());
						locMin = new Location(w, regMin.getX(), regMin.getY(), regMin.getZ());

						return new Region(name, locMin, locMax);
					}
		}
		return null;
	}

	public List<String> getAllRegions() {
		final List<String> list = new ArrayList<>();

		for (final World w : Bukkit.getWorlds()) {
			final Object rm = getRegionManager(w);
			if (legacy)
				try {
					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					Method getId = null;
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;
						if (getId == null)
							getId = regObj.getClass().getMethod("getId");

						final String name = Common.stripColors(getId.invoke(regObj).toString());

						if (!name.startsWith("__"))
							list.add(name);
					}
				} catch (final Throwable t) {
					t.printStackTrace();

					throw new FoException("Failed WorldEdit 6 legacy hook, see above & report");
				}
			else
				((com.sk89q.worldguard.protection.managers.RegionManager) rm)
						.getRegions().values().forEach(reg -> {
							if (reg == null || reg.getId() == null)
								return;

							final String name = Common.stripColors(reg.getId());

							if (!name.startsWith("__"))
								list.add(name);
						});
		}

		return list;
	}

	private Iterable<ProtectedRegion> getApplicableRegions(final Location loc) {
		final Object rm = getRegionManager(loc.getWorld());

		if (legacy)
			try {
				return (Iterable<ProtectedRegion>) rm.getClass().getMethod("getApplicableRegions", Location.class).invoke(rm, loc);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldEdit 6 legacy hook, see above & report");
			}

		return ((com.sk89q.worldguard.protection.managers.RegionManager) rm)
				.getApplicableRegions(com.sk89q.worldedit.math.BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
	}

	private Object getRegionManager(final World w) {
		if (legacy)
			try {
				return Class.forName("com.sk89q.worldguard.bukkit.WGBukkit").getMethod("getRegionManager", World.class).invoke(null, w);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldGuard 6 legacy hook, see above & report");
			}

		// causes class errors..
		//return com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(new com.sk89q.worldedit.bukkit.BukkitWorld(w));
		// dynamically load modern WE
		try {

			final Class<?> bwClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld");
			final Constructor<?> bwClassNew = bwClass.getConstructor(World.class);

			Object t = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null);
			t = t.getClass().getMethod("getPlatform").invoke(t);
			t = t.getClass().getMethod("getRegionContainer").invoke(t);
			return t.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World")).invoke(t, bwClassNew.newInstance(w));

		} catch (final Throwable t) {
			t.printStackTrace();

			throw new FoException("Failed WorldGuard hook, see above & report");
		}
	}
}

abstract class FactionsHook {

	/**
	 * Get all loaded factions
	 */
	abstract Collection<String> getFactions();

	/**
	 * Get faction of the player
	 */
	abstract String getFaction(Player pl);

	/**
	 * Get faction in the location
	 */
	abstract String getFaction(Location loc);

	/**
	 * Get faction owner at the specific location
	 */
	abstract String getFactionOwner(Location loc);

	/**
	 * Get all players being in the same faction, used for party chat.
	 */
	final Collection<? extends Player> getSameFactionPlayers(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playerFaction = getFaction(pl);

		if (playerFaction != null && !playerFaction.isEmpty())
			for (final Player online : Remain.getOnlinePlayers()) {
				final String onlineFaction = getFaction(online);

				if (playerFaction.equals(onlineFaction))
					recipients.add(online);
			}

		return recipients;
	}
}

final class FactionsMassive extends FactionsHook {

	FactionsMassive() {
	}

	@Override
	public Collection<String> getFactions() {
		return Common.convert(com.massivecraft.factions.entity.FactionColl.get().getAll(), object -> Common.stripColors(object.getName()));
	}

	@Override
	public String getFaction(final Player pl) {
		try {
			return MPlayer.get(pl.getUniqueId()).getFactionName();
		} catch (final Exception ex) {
			return null;
		}
	}

	@Override
	public String getFaction(final Location loc) {
		final Faction f = BoardColl.get().getFactionAt(PS.valueOf(loc));

		if (f != null)
			return f.getName();

		return null;
	}

	@Override
	public String getFactionOwner(final Location loc) {
		final Faction f = BoardColl.get().getFactionAt(PS.valueOf(loc));

		if (f != null)
			return f.getLeader() != null ? f.getLeader().getName() : null;

		return null;
	}
}

final class FactionsUUID extends FactionsHook {

	@Override
	public Collection<String> getFactions() {
		try {
			final Object i = instance();
			final Set<String> tags = (Set<String>) i.getClass().getMethod("getFactionTags").invoke(i);

			return tags;
		} catch (final Throwable t) {
			t.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFaction(final Player pl) {
		try {
			final Object fplayers = fplayers();
			final Object fpl = fplayers.getClass().getMethod("getByPlayer", Player.class).invoke(fplayers, pl);
			final Object f = fpl != null ? fpl.getClass().getMethod("getFaction").invoke(fpl) : null;
			final Object name = f != null ? f.getClass().getMethod("getTag").invoke(f) : null;

			return name != null ? name.toString() : null;

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFaction(final Location loc) {
		final Object f = findFaction(loc);

		try {
			return f != null ? f.getClass().getMethod("getTag").invoke(f).toString() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFactionOwner(final Location loc) {
		final Object faction = findFaction(loc);

		try {
			return faction != null ? ((com.massivecraft.factions.FPlayer) faction.getClass().getMethod("getFPlayerAdmin").invoke(faction)).getName() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	private Object findFaction(final Location loc) {
		final Class<com.massivecraft.factions.Board> b = com.massivecraft.factions.Board.class;

		try {
			return b.getMethod("getFactionAt", com.massivecraft.factions.FLocation.class).invoke(b.getMethod("getInstance").invoke(null), new com.massivecraft.factions.FLocation(loc));
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	private Object instance() {
		try {
			return Class.forName("com.massivecraft.factions.Factions").getDeclaredMethod("getInstance").invoke(null);
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			throw new FoException(ex);
		}
	}

	private Object fplayers() {
		try {
			return Class.forName("com.massivecraft.factions.FPlayers").getDeclaredMethod("getInstance").invoke(null);
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			throw new FoException(ex);
		}
	}
}

class McMMOHook {

	// Only display error once
	private boolean errorLogged = false;

	String getActivePartyChat(final Player player) {
		try {
			final McMMOPlayer mcplayer = UserManager.getPlayer(player);

			if (mcplayer != null) {
				final Party party = mcplayer.getParty();
				final ChatChannel channelType = mcplayer.getChatChannel();

				return channelType == ChatChannel.PARTY || channelType == ChatChannel.PARTY_OFFICER && party != null ? party.getName() : null;
			}

		} catch (final Throwable throwable) {
			if (!errorLogged) {
				Common.warning("Failed getting mcMMO party chat for " + player.getName() + " due to error. Returning null."
						+ " Ensure you have the latest mcMMO version, if so, contact plugin authors to update the integration. Error was: " + throwable);

				errorLogged = true;
			}
		}

		return null;
	}

	List<Player> getPartyRecipients(final Player bukkitPlayer) {
		try {
			final McMMOPlayer mcplayer = UserManager.getPlayer(bukkitPlayer);

			if (mcplayer != null) {
				final Party party = mcplayer.getParty();

				if (party != null)
					return party.getOnlineMembers();
			}

		} catch (final Throwable throwable) {
			if (!errorLogged) {
				Common.warning("Failed getting mcMMO party recipients for " + bukkitPlayer.getName() + " due to error. Returning null."
						+ " Ensure you have the latest mcMMO version, if so, contact plugin authors to update the integration. Error was: " + throwable);

				errorLogged = true;
			}
		}

		return new ArrayList<>();
	}
}

class PlotSquaredHook {

	private final boolean legacy;

	/**
	 *
	 */
	PlotSquaredHook() {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin("PlotSquared");
		Valid.checkNotNull(plugin, "PlotSquared not hooked yet!");

		this.legacy = plugin.getDescription().getVersion().startsWith("3");
	}

	List<Player> getPlotPlayers(final Player player) {
		final List<Player> players = new ArrayList<>();

		final Class<?> plotPlayerClass = ReflectionUtil.lookupClass((legacy ? "com.intellectualcrafters.plot.object" : "com.plotsquared.core.player") + ".PlotPlayer");
		Method wrap;

		try {
			wrap = plotPlayerClass.getMethod("wrap", Player.class);

		} catch (final ReflectiveOperationException ex) {

			try {
				wrap = plotPlayerClass.getMethod("wrap", Object.class);

			} catch (final ReflectiveOperationException ex2) {

				try {
					wrap = plotPlayerClass.getMethod("from", Object.class);

				} catch (final ReflectiveOperationException ex3) {
					throw new FoException(ex3, "PlotSquared could not convert " + player.getName() + " into PlotPlayer! Is the integration outdated?");
				}
			}
		}

		final Object plotPlayer = ReflectionUtil.invokeStatic(wrap, player);
		Valid.checkNotNull(plotPlayer, "Failed to convert player " + player.getName() + " to PlotPlayer!");

		final Object currentPlot = ReflectionUtil.invoke("getCurrentPlot", plotPlayer);

		if (currentPlot != null)
			for (final Object playerInPlot : (Iterable<?>) ReflectionUtil.invoke("getPlayersInPlot", currentPlot)) {
				final UUID id = ReflectionUtil.invoke("getUUID", playerInPlot);
				final Player online = Bukkit.getPlayer(id);

				if (online != null && online.isOnline())
					players.add(online);
			}

		return players;
	}
}

class CMIHook {

	boolean isVanished(final Player player) {
		final CMIUser user = getUser(player);

		return user != null && user.isVanished();
	}

	void setVanished(Player player, boolean vanished) {
		final CMIUser user = getUser(player);

		if (user != null && user.isVanished() != vanished)
			user.setVanished(false);
	}

	boolean isAfk(final Player player) {
		final CMIUser user = getUser(player);

		return user != null && user.isAfk();
	}

	boolean isMuted(final Player player) {
		final CMIUser user = getUser(player);

		try {
			return user != null && user.getMutedUntil() != 0 && user.getMutedUntil() != null && user.getMutedUntil() > System.currentTimeMillis();

		} catch (final Exception ex) {
			return false;
		}
	}

	boolean hasGodMode(final Player player) {
		final CMIUser user = getUser(player);

		return user != null ? user.isGod() : false;
	}

	void setGodMode(final Player player, final boolean godMode) {
		final CMIUser user = getUser(player);

		if (user != null)
			user.setGod(godMode);
	}

	void setLastTeleportLocation(final Player player, final Location location) {
		final CMIUser user = getUser(player);

		try {
			user.getClass().getMethod("setLastTeleportLocation", Location.class).invoke(user, location);
		} catch (final Throwable t) {
			// Silently fail
		}
	}

	void setIgnore(final UUID player, final UUID who, final boolean ignore) {
		final CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);

		if (ignore)
			user.addIgnore(who, true /* save now */);
		else
			user.removeIgnore(who);
	}

	boolean isIgnoring(final UUID player, final UUID who) {
		try {
			final CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);

			return user.isIgnoring(who);

		} catch (final NullPointerException ex) {
			return false;
		}
	}

	String getNick(final Player player) {
		final CMIUser user = getUser(player);
		final String nick = user == null ? null : user.getNickName();

		return nick == null || "".equals(nick) ? null : nick;
	}

	String getNick(final String playerName) {
		final CMIUser user = getUser(playerName);
		final String nick = user == null ? null : user.getNickName();

		return nick == null || "".equals(nick) ? null : nick;
	}

	void setNick(final UUID uniqueId, String nick) {
		final CMIUser user = getUser(uniqueId);
		final TabListManager tabManager = CMI.getInstance().getTabListManager();

		if (user != null) {
			final boolean isEmpty = nick == null || Common.stripColors(nick).replace(" ", "").isEmpty();

			user.setNickName(isEmpty ? null : Common.colorize(nick), true);
			user.updateDisplayName();

			if (tabManager.isUpdatesOnNickChange())
				tabManager.updateTabList(3);
		}
	}

	String getNameFromNick(String nick) {
		for (final CMIUser user : CMI.getInstance().getPlayerManager().getAllUsers().values())
			if (user != null && user.getNickName() != null && Valid.colorlessEquals(user.getNickName(), nick))
				return Common.getOrDefault(user.getName(), nick);

		return nick;
	}

	private CMIUser getUser(final Player player) {
		return CMI.getInstance().getPlayerManager().getUser(player);
	}

	private CMIUser getUser(final UUID uniqueId) {
		return CMI.getInstance().getPlayerManager().getUser(uniqueId);
	}

	private CMIUser getUser(final String name) {
		return CMI.getInstance().getPlayerManager().getUser(name);
	}
}

class CitizensHook {

	boolean isNPC(final Entity entity) {
		final NPCRegistry reg = CitizensAPI.getNPCRegistry();

		return reg != null ? reg.isNPC(entity) : false;
	}
}

class DiscordSRVHook {

	Set<String> getChannels() {
		return DiscordSRV.getPlugin().getChannels().keySet();
	}

	boolean sendMessage(final String channel, final String message) {
		return sendMessage((CommandSender) null, channel, message);
	}

	boolean sendMessage(final CommandSender sender, final String channel, final String message) {
		final TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// Channel not configured in DiscordSRV config.yml, ignore
		if (textChannel == null) {
			Debugger.debug("discord", "[MC->Discord] Could not find Discord channel '" + channel + "'. Available: " + String.join(", ", getChannels()) + ". Not sending: " + message);

			return false;
		}

		if (sender instanceof Player) {
			Debugger.debug("discord", "[MC->Discord] " + sender.getName() + " send message to '" + channel + "' channel. Message: '" + message + "'");

			final DiscordSRV instance = JavaPlugin.getPlugin(DiscordSRV.class);

			// Dirty: We have to temporarily unset value in DiscordSRV to enable the processChatMessage method to function
			final File file = new File(SimplePlugin.getData().getParent(), "DiscordSRV/config.yml");

			if (file.exists()) {
				final FileConfiguration discordConfig = YamlConfiguration.loadConfiguration(file);

				if (discordConfig != null) {
					final String outMessageKey = "DiscordChatChannelMinecraftToDiscord";
					final boolean outMessageOldValue = discordConfig.getBoolean(outMessageKey);

					discordConfig.set(outMessageKey, true);

					try {
						instance.processChatMessage((Player) sender, message, channel, false);

					} finally {
						discordConfig.set(outMessageKey, outMessageOldValue);
					}
				}
			}

		} else {
			Debugger.debug("discord", "[MC->Discord] " + (sender == null ? "No sender " : sender.getName() + " (generic)") + "sent message to '" + channel + "' channel. Message: '" + message + "'");

			DiscordUtil.sendMessage(textChannel, message);
		}
		return true;
	}
}

class BanManagerHook {

	/*
	 * Return true if the given player is muted
	 */
	boolean isMuted(final Player player) {
		try {
			final Class<?> api = ReflectionUtil.lookupClass("me.confuser.banmanager.common.api.BmAPI");
			final Method isMuted = ReflectionUtil.getMethod(api, "isMuted", UUID.class);

			return ReflectionUtil.invoke(isMuted, null, player.getUniqueId());

		} catch (final Throwable t) {
			if (!t.toString().contains("Could not find class"))
				Common.log("Unable to check if " + player.getName() + " is muted at BanManager. Is the API hook outdated? Got: " + t);

			return false;
		}
	}
}

class BossHook {

	/*
	 * Return the Boss name if the given player is a Boss or null
	 */
	String getBossName(final Entity entity) {
		try {
			final Class<?> api = ReflectionUtil.lookupClass("org.mineacademy.boss.api.BossAPI");
			final Method getBoss = ReflectionUtil.getMethod(api, "getBoss", Entity.class);

			final Object boss = ReflectionUtil.invoke(getBoss, null, entity);

			if (boss != null) {
				final Method getName = ReflectionUtil.getMethod(boss.getClass(), "getName");

				return ReflectionUtil.invoke(getName, boss);
			}

		} catch (final Throwable t) {
			Common.log("Unable to check if " + entity + " is a BOSS. Is the API hook outdated? Got: " + t);
		}

		return null;
	}
}

class MythicMobsHook {

	private Boolean legacyVersion = null;

	MythicMobsHook() {
		final Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs");
		final String version = mythicMobs.getDescription().getVersion();

		if (version.startsWith("4."))
			legacyVersion = true;

		else if (version.startsWith("5."))
			legacyVersion = false;

		else
			Common.warning("Skipping hooking into unsupported MythicMob version " + version + "! Only 4.X.X and 5.X.X are supported.");

	}

	/*
	 * Attempt to return a MythicMob name from the given entity
	 * or null if the entity is not a MythicMob
	 */
	String getBossName(Entity entity) {
		if (legacyVersion == null)
			return null;

		if (legacyVersion)
			return getBossNameV4(entity);

		return getBossNameV5(entity);
	}

	private String getBossNameV4(Entity entity) {
		try {
			final Class<?> mythicMobs = ReflectionUtil.lookupClass("io.lumine.xikage.mythicmobs.MythicMobs");
			final Object instance = ReflectionUtil.invokeStatic(mythicMobs, "inst");
			final Object mobManager = ReflectionUtil.invoke("getMobManager", instance);
			final Optional<Object> activeMob = ReflectionUtil.invoke(ReflectionUtil.getMethod(mobManager.getClass(), "getActiveMob", UUID.class), mobManager, entity.getUniqueId());
			final Object mob = activeMob != null && activeMob.isPresent() ? activeMob.get() : null;

			if (mob != null) {
				final Object mythicEntity = ReflectionUtil.invoke("getEntity", mob);

				if (mythicEntity != null)
					return (String) ReflectionUtil.invoke("getName", mythicEntity);
			}

		} catch (final NoSuchElementException ex) {
		}

		return null;
	}

	private String getBossNameV5(Entity entity) {

		final Object mythicPlugin = ReflectionUtil.invokeStatic(ReflectionUtil.lookupClass("io.lumine.mythic.api.MythicProvider"), "get");
		final Object mobManager = ReflectionUtil.invoke("getMobManager", mythicPlugin);
		final Collection<?> activeMobs = ReflectionUtil.invoke("getActiveMobs", mobManager);

		for (final Object mob : activeMobs) {
			final UUID uniqueId = ReflectionUtil.invoke("getUniqueId", mob);

			if (uniqueId.equals(entity.getUniqueId()))
				return ReflectionUtil.invoke("getName", mob);
		}

		return null;
	}
}

class LandsHook {

	private final Object lands;
	private final Method getLand;

	LandsHook() {
		final Class<?> landsIntegration = ReflectionUtil.lookupClass("me.angeschossen.lands.api.integration.LandsIntegration");
		final Constructor<?> con = ReflectionUtil.getConstructor(landsIntegration, Plugin.class);

		this.lands = ReflectionUtil.instantiate(con, SimplePlugin.getInstance());
		this.getLand = ReflectionUtil.getMethod(landsIntegration, "getLand", Location.class);
	}

	Collection<Player> getLandPlayers(Player player) {
		final Object land = ReflectionUtil.invoke(this.getLand, this.lands, player.getLocation());

		if (land != null)
			return (Collection<Player>) ReflectionUtil.invoke("getOnlinePlayers", land);

		return new ArrayList<>();
	}
}

class LiteBansHook {

	/*
	 * Return true if the given player is muted
	 */
	boolean isMuted(final Player player) {
		return false; // Problematic, we're investigating this
		/*try {
			final Class<?> api = ReflectionUtil.lookupClass("litebans.api.Database");
			final Object instance = ReflectionUtil.invokeStatic(api, "get");
		
			return ReflectionUtil.invoke("isPlayerMuted", instance, player.getUniqueId());
		
		} catch (final Throwable t) {
			if (!t.toString().contains("Could not find class")) {
				Common.log("Unable to check if " + player.getName() + " is muted at LiteBans. Is the API hook outdated? See console error:");
		
				t.printStackTrace();
			}
		
			return false;
		}*/
	}
}

class ItemsAdderHook {

	private final Class<?> itemsAdder;
	private final Method replaceFontImagesMethod;
	private final Method replaceFontImagesMethodNoPlayer;

	ItemsAdderHook() {
		this.itemsAdder = ReflectionUtil.lookupClass("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
		this.replaceFontImagesMethod = ReflectionUtil.getDeclaredMethod(itemsAdder, "replaceFontImages", Player.class, String.class);
		this.replaceFontImagesMethodNoPlayer = ReflectionUtil.getDeclaredMethod(itemsAdder, "replaceFontImages", String.class);
	}

	/*
	 * Return true if the given player is muted
	 */
	String replaceFontImages(@Nullable final Player player, final String message) {
		if (player == null)
			return ReflectionUtil.invokeStatic(replaceFontImagesMethodNoPlayer, message);

		return ReflectionUtil.invokeStatic(replaceFontImagesMethod, player, message);
	}
}