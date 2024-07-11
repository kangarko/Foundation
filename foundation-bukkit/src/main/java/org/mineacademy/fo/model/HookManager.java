package org.mineacademy.fo.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.platform.SimplePlugin;
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
import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.IUser;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;
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

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.EntityTarget;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Our main class for hooking into different plugins, providing you
 * convenient access to their methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HookManager {

	// ------------------------------------------------------------------------------------------------------------
	// Store hook classes separately below, avoiding no such method/field errors
	// ------------------------------------------------------------------------------------------------------------

	private static AdvancedVanishHook advancedVanishHook;
	private static AuthMeHook authMeHook;
	private static BanManagerHook banManagerHook;
	private static BentoBoxHook bentoBoxHook;
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
	private static MultiverseHook multiverseHook;
	private static MVdWPlaceholderHook MVdWPlaceholderHook;
	private static MythicMobsHook mythicMobsHook;
	private static NickyHook nickyHook;
	private static PlaceholderAPIHook placeholderAPIHook;
	private static PlotSquaredHook plotSquaredHook;
	private static PremiumVanishHook premiumVanishHook;
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
	 * Detect various plugins and load their methods into this library so you can use it later.
	 */
	public static void loadDependencies() {

		if (Platform.isPluginInstalled("AdvancedVanish"))
			advancedVanishHook = new AdvancedVanishHook();

		if (Platform.isPluginInstalled("AuthMe"))
			authMeHook = new AuthMeHook();

		if (Platform.isPluginInstalled("BanManager"))
			banManagerHook = new BanManagerHook();

		if (Platform.isPluginInstalled("BentoBox"))
			bentoBoxHook = new BentoBoxHook();

		if (Platform.isPluginInstalled("Boss"))
			bossHook = new BossHook();

		if (Platform.isPluginInstalled("Citizens"))
			citizensHook = new CitizensHook();

		if (Platform.isPluginInstalled("CMI"))
			CMIHook = new CMIHook();

		if (Platform.isPluginInstalled("DiscordSRV"))
			try {
				Class.forName("github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel");

				discordSRVHook = new DiscordSRVHook();

			} catch (final ClassNotFoundException ex) {
				Common.error(ex, "&c" + SimplePlugin.getInstance().getName() + " failed to hook into DiscordSRV because the plugin is outdated (1.18.x is supported)!");
			}

		if (Platform.isPluginInstalled("Essentials"))
			essentialsHook = new EssentialsHook();

		// Various kinds of Faction plugins.
		final Plugin factions = Bukkit.getPluginManager().getPlugin("Factions");

		if (Platform.isPluginInstalled("FactionsX") && factions == null)
			Common.log("Note: If you want FactionX integration, install FactionsUUIDAPIProxy.");

		else if (factions != null) {
			final String ver = factions.getDescription().getVersion();
			final String main = factions.getDescription().getMain();

			if (ver.startsWith("1.6") || main.contains("FactionsUUIDAPIProxy"))
				factionsHook = new FactionsUUID();
			else if (ver.startsWith("2.")) {
				Class<?> mplayer = null;

				try {
					mplayer = Class.forName("com.massivecraft.factions.entity.MPlayer"); // only support the free version of the plugin
				} catch (final ClassNotFoundException ex) {
				}

				if (mplayer != null)
					factionsHook = new FactionsMassive();
				else
					Common.warning("Recognized MCore Factions, but it isn't hooked! Check if you have the latest version!");

			}
		}

		if (Platform.isPluginInstalled("ItemsAdder"))
			itemsAdderHook = new ItemsAdderHook();

		if (Platform.isPluginInstalled("Lands"))
			landsHook = new LandsHook();

		if (Platform.isPluginInstalled("LiteBans"))
			liteBansHook = new LiteBansHook();

		if (Platform.isPluginInstalled("Lockette"))
			locketteProHook = new LocketteProHook();

		if (Platform.isPluginInstalled("LWC"))
			lwcHook = new LWCHook();

		if (Platform.isPluginInstalled("Multiverse-Core"))
			multiverseHook = new MultiverseHook();

		if (Platform.isPluginInstalled("MVdWPlaceholderAPI"))
			MVdWPlaceholderHook = new MVdWPlaceholderHook();

		if (Platform.isPluginInstalled("MythicMobs"))
			mythicMobsHook = new MythicMobsHook();

		if (Platform.isPluginInstalled("Nicky"))
			nickyHook = new NickyHook();

		if (Platform.isPluginInstalled("PlaceholderAPI"))
			placeholderAPIHook = new PlaceholderAPIHook();

		if (Platform.isPluginInstalled("PlotSquared")) {
			final String ver = Bukkit.getPluginManager().getPlugin("PlotSquared").getDescription().getVersion();

			if (ver.startsWith("7.") || ver.startsWith("6.") || ver.startsWith("5.") || ver.startsWith("3."))
				plotSquaredHook = new PlotSquaredHook();
			else
				Common.warning("Could not hook into PlotSquared. Version 3.x, 5.x or 6.x required, you have " + ver);
		}

		if (Platform.isPluginInstalled("PremiumVanish"))
			premiumVanishHook = new PremiumVanishHook();

		if (Platform.isPluginInstalled("ProtocolLib")) {
			// Also check if the library is loaded properly.
			try {
				protocolLibHook = new ProtocolLibHook();

				Class.forName("com.comphenix.protocol.wrappers.WrappedChatComponent");

			} catch (final Throwable t) {
				protocolLibHook = null;

				Common.error(t, "You are running an old and unsupported version of ProtocolLib, please update it. The plugin will continue to function without hooking into it.");
			}
		}

		if (Platform.isPluginInstalled("Residence"))
			residenceHook = new ResidenceHook();

		if (Platform.isPluginInstalled("Towny"))
			townyHook = new TownyHook();

		if (Platform.isPluginInstalled("Vault"))
			vaultHook = new VaultHook();

		if (Platform.isPluginInstalled("WorldEdit") || Platform.isPluginInstalled("FastAsyncWorldEdit"))
			worldeditHook = new WorldEditHook();

		if (Platform.isPluginInstalled("WorldGuard"))
			worldguardHook = new WorldGuardHook(worldeditHook);

		// Dummy hooks.

		if (Platform.isPluginInstalled("NBTAPI"))
			nbtAPIDummyHook = true;

		if (Platform.isPluginInstalled("Votifier"))
			nuVotifierDummyHook = true;

		if (Platform.isPluginInstalled("TownyChat"))
			townyChatDummyHook = true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods for determining which plugins were loaded after you call the load method
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Is AdvancedVanish loaded?
	 *
	 * @return
	 */
	public static boolean isAdvancedVanishLoaded() {
		return advancedVanishHook != null;
	}

	/**
	 * Is AuthMe Reloaded loaded? We only support the latest version
	 *
	 * @return
	 */
	public static boolean isAuthMeLoaded() {
		return authMeHook != null;
	}

	/**
	 * Is BanManager loaded?
	 *
	 * @return
	 */
	public static boolean isBanManagerLoaded() {
		return banManagerHook != null;
	}

	/**
	 * Is BentoBox loaded?
	 *
	 * @return
	 */
	public static boolean isBentoBoxLoaded() {
		return bentoBoxHook != null;
	}

	/**
	 * Is Boss loaded?
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
	 * Are any Faction plugins loaded?
	 * We support FactionsUUID and the free Factions.
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

		// Check for FastAsyncWorldEdit directly.
		final Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");

		if (fawe != null && fawe.isEnabled())
			return true;

		// Check for legacy FastAsyncWorldEdit installations.
		final Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");

		if (worldEdit != null && worldEdit.isEnabled() && "Fast Async WorldEdit plugin".equals(worldEdit.getDescription().getDescription()))
			return true;

		return false;
	}

	/**
	 * Is ItemsAdder loaded?
	 *
	 * @return
	 */
	public static boolean isItemsAdderLoaded() {
		return itemsAdderHook != null;
	}

	/**
	 * Is Lands loaded?
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
	 * Is Lockette Pro loaded?
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
	 * Is NBTAPI loaded?
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
	 * Is nuVotifier loaded?
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
	 * Is PremiumVanish loaded?
	 *
	 * @return
	 */
	public static boolean isPremiumVanishLoaded() {
		return premiumVanishHook != null;
	}

	/**
	 * Is ProtocolLib loaded?
	 * <p>
	 * This will not only check if the plugin is in the plugins folder, but
	 * also if it's correctly loaded and working. (Should detect the plugin's
	 * malfunction when it's outdated).
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
	 * Return true if the player is logged via AuthMe, or true if AuthMe is not installed.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static boolean isLogged(final Player player) {
		return !isAuthMeLoaded() || authMeHook.isLogged(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Boss and MythicMobs.
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the Boss name from the given entity, if Boss is installed
	 * and the given entity is a Boss, otherwise returns null.
	 *
	 * @param entity the entity to check.
	 * @return
	 */
	public static String getBossName(@NonNull Entity entity) {
		return isBossLoaded() ? bossHook.getBossName(entity) : null;
	}

	/**
	 * Returns the name from the given entity, if MythicMobs is installed
	 * and the given entity is a MythicMob, otherwise returns null.
	 *
	 * @param entity the entity to check.
	 * @return
	 */
	public static String getMythicMobName(@NonNull Entity entity) {
		return isMythicMobsLoaded() ? mythicMobsHook.getBossName(entity) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// BentoBox
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the visitors for the specified player's island, or an empty
	 * set if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxVisitors(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandVisitors(player) : new HashSet<>();
	}

	/**
	 * Return the coops for the specified player's island, or an empty set
	 * if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxCoops(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandCoops(player) : new HashSet<>();
	}

	/**
	 * Return the trustees for the specified player's island, or an empty
	 * set if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxTrustees(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandTrustees(player) : new HashSet<>();
	}

	/**
	 * Return the members for the specified player's island, or an empty
	 * set if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxMembers(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandMembers(player) : new HashSet<>();
	}

	/**
	 * Return the subowners for the specified player's island, or an empty
	 * set if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxSubOwners(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandSubOwners(player) : new HashSet<>();
	}

	/**
	 * Return the owners for the specified player's island, or an empty set
	 * if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxOwners(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandOwners(player) : new HashSet<>();
	}

	/**
	 * Return the moderators for the specified player's island, or an empty
	 * set if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxMods(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandMods(player) : new HashSet<>();
	}

	/**
	 * Return the admins for the specified player's island, or an empty set
	 * if it's null.
	 *
	 * @param player the player's island to check.
	 * @return
	 */
	public static Set<UUID> getBentoBoxAdmins(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandAdmins(player) : new HashSet<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Lands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the players for the player's land, or an empty list if it's
	 * null.
	 *
	 * @param player the player's land to check.
	 * @return
	 */
	public static Collection<Player> getLandPlayers(Player player) {
		return isLandsLoaded() ? landsHook.getLandPlayers(player) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// AdvancedVanish, CMI and EssentialsX
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the given player is AFK in EssentialsX or CMI, or
	 * false if neither plugin is present.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static boolean isAfk(final Player player) {
		final boolean essAFK = isEssentialsLoaded() && essentialsHook.isAfk(player.getName());
		final boolean cmiAFK = isCMILoaded() && CMIHook.isAfk(player);

		return essAFK || cmiAFK;
	}

	/**
	 * Return true if the given player is vanished in EssentialsX.
	 *
	 * @deprecated this does not call a metadata check for most plugins,
	 *             nor an NMS check. See {@link PlayerUtil#isVanished(Player)}.
	 * @param player the player to check.
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedEssentials(final Player player) {
		return isEssentialsLoaded() && essentialsHook.isVanished(player.getName());
	}

	/**
	 * Return true if the given player is vanished in CMI.
	 *
	 * @deprecated this does not call a metadata check for most plugins,
	 *             nor an NMS check. See {@link PlayerUtil#isVanished(Player)}.
	 * @param player the player to check.
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedCMI(final Player player) {
		return isCMILoaded() && CMIHook.isVanished(player);
	}

	/**
	 * Return true if the given player is vanished in AdvancedVanish.
	 *
	 * @deprecated this does not a call metadata check for most plugins,
	 *             nor an NMS check. See {@link PlayerUtil#isVanished(Player)}.
	 * @param player the player to check.
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedAdvancedVanish(final Player player) {
		return isAdvancedVanishLoaded() && advancedVanishHook.isVanished(player);
	}

	/**
	 * Return true if the given player is vanished in PremiumVanish.
	 *
	 * @deprecated this does not a call metadata check for most plugins,
	 *             nor an NMS check. See {@link PlayerUtil#isVanished(Player)}.
	 * @param player the player to check.
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedPremiumVanish(final Player player) {
		return isPremiumVanishLoaded() && premiumVanishHook.isVanished(player);
	}

	/**
	 * Returns if the player is vanished from plugins AdvancedVanish, CMI,
	 * PremiumVanish and EssentialsX.
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(final Player player) {

		if (isVanishedPremiumVanish(player))
			return true;

		if (isVanishedAdvancedVanish(player))
			return true;

		if (isVanishedCMI(player))
			return true;

		if (isVanishedEssentials(player))
			return true;

		return false;
	}

	/**
	 * Sets the vanish status for the player in AdvancedVanish, CMI,
	 * PremiumVanish and EssentialsX.
	 *
	 * @deprecated this does not remove the vanish metadata and NMS
	 * invisibility. Use {@link PlayerUtil#setVanished(Player, boolean)}
	 * for that.
	 * @param player   the player whose vanish status you want to set.
	 * @param vanished the state to set the player's vanish to.
	 */
	@Deprecated
	public static void setVanished(@NonNull Player player, boolean vanished) {
		if (isEssentialsLoaded())
			essentialsHook.setVanished(player.getName(), vanished);

		if (isCMILoaded())
			CMIHook.setVanished(player, vanished);

		if (isAdvancedVanishLoaded())
			advancedVanishHook.setVanished(player, vanished);

		if (isPremiumVanishLoaded())
			premiumVanishHook.setVanished(player, vanished);
	}

	/**
	 * Return true if the player is muted in BanManager, CMI, EssentialsX
	 * or LiteBans, or false if none of these plugins are present.
	 *
	 * @param player the player to check.
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
	 * Mutes the given player if LiteBans is installed. This expects you to
	 * have the /lmute command!
	 *
	 * @param targetPlayerName  the player to mute.
	 * @param durationTokenized the duration to mute the player for.
	 * @param reason            the reason to mute the player for.
	 */
	public static void setLiteBansMute(String targetPlayerName, String durationTokenized, String reason) {
		if (isLiteBansLoaded())
			Platform.runTask(0, () -> {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lmute " + targetPlayerName + " " + durationTokenized + (reason == null || reason.isEmpty() ? "" : " " + reason));
			});
	}

	/**
	 * Unmutes the given player if LiteBans is installed. This expects you
	 * to have the /lunmute command!
	 *
	 * @param targetPlayerName the player to unmute.
	 */
	public static void setLiteBansUnmute(String targetPlayerName) {
		if (isLiteBansLoaded())
			Platform.runTask(0, () -> {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lunmute " + targetPlayerName);
			});
	}

	/**
	 * Return true if the given player has god mode in EssentialsX or CMI,
	 * or false if neither plugin is present.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static boolean hasGodMode(final Player player) {
		final boolean essGodMode = isEssentialsLoaded() && essentialsHook.hasGodMode(player);
		final boolean cmiGodMode = isCMILoaded() && CMIHook.hasGodMode(player);

		return essGodMode || cmiGodMode;
	}

	/**
	 * Sets the player's god mode status in CMI and EssentialsX.
	 *
	 * @param player  the player whose god mode status you want to set.
	 * @param godMode the state to set the player's god mode to.
	 */
	public static void setGodMode(final Player player, final boolean godMode) {
		if (isEssentialsLoaded())
			essentialsHook.setGodMode(player, godMode);

		if (isCMILoaded())
			CMIHook.setGodMode(player, godMode);
	}

	/**
	 * Sets the player's last /back location in CMI and EssentialsX.
	 *
	 * @param player   the player whose /back location you want to set.
	 * @param location the location to set the player's /back location to.
	 */
	public static void setBackLocation(final Player player, final Location location) {
		if (isEssentialsLoaded())
			essentialsHook.setBackLocation(player.getName(), location);

		if (isCMILoaded())
			CMIHook.setLastTeleportLocation(player, location);
	}

	/**
	 * Sets the player's ignore status for the given target in CMI and Esse
	 *
	 * @param player the player whose ignore status you want to set.
	 * @param who    the target player who you want the player to ignore.
	 * @param ignore the state to set the player's ignore status to.
	 */
	public static void setIgnore(final UUID player, final UUID who, final boolean ignore) {
		if (isEssentialsLoaded())
			essentialsHook.setIgnore(player, who, ignore);

		if (isCMILoaded())
			CMIHook.setIgnore(player, who, ignore);
	}

	/**
	 * Return true if the player is ignoring another player in CMI or EssentialsX,
	 * or false if neither plugin is present.
	 *
	 * @param player the player to check.
	 * @param who    the target player to check.
	 * @return
	 */
	public static boolean isIgnoring(final UUID player, final UUID who) {
		Valid.checkBoolean(player != null, "Player to check ignore from cannot be null/empty");
		Valid.checkBoolean(who != null, "Player to check ignore to cannot be null/empty");

		return isEssentialsLoaded() ? essentialsHook.isIgnoring(player, who) : isCMILoaded() ? CMIHook.isIgnoring(player, who) : false;
	}

	/**
	 * Returns the colored nickname of the given recipient from CMI,
	 * EssentialsX or Nicky, or if it's a console, their name.
	 *
	 * @param sender the player who's nickname you want to get.6
	 * @return
	 */
	public static String getNickColored(final CommandSender sender) {
		return getNick(sender, false);
	}

	/**
	 * Returns the nickname, stripped of colors, for the given recipient from
	 * CMI, EssentialsX or Nicky, or if it's a console, their name.
	 *
	 * @param sender the player whose nickname you want to get.
	 * @return
	 */
	public static String getNickColorless(final CommandSender sender) {
		return getNick(sender, true);
	}

	/**
	 * Returns the nickname for the given recipient from CMI, EssentialsX or
	 * Nicky, or if it's a console, their name.
	 *
	 * @param sender      the player whose nickname you want to get.
	 * @param stripColors should we strip colors from the nickname?
	 *
	 * @return
	 */
	private static String getNick(final CommandSender sender, boolean stripColors) {
		final Player player = sender instanceof Player ? (Player) sender : null;

		if (player != null && isNPC(player)) {
			Debugger.printStackTrace("Called getNick for NPC " + player.getName() + "! Notify the developers to ignore NPCs using HookManager#isNPC!");

			return player.getName();
		}

		if (player == null)
			return sender.getName();

		final String nickyNick = isNickyLoaded() ? nickyHook.getNick(player) : null;
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNick(player.getName()) : null;
		final String cmiNick = isCMILoaded() ? CMIHook.getNick(player) : null;

		final String nick = nickyNick != null ? nickyNick : cmiNick != null ? cmiNick : essNick != null ? essNick : sender.getName();

		return stripColors ? CompChatColor.stripColorCodes(nick) : nick;
	}

	/**
	 * Attempts to find a nickname from the given player name, defaulting to
	 * the given name if it's null. We only support CMI and EssentialsX.
	 *
	 * @param playerName the player name to use.
	 * @return
	 */
	public static String getNickFromName(final String playerName) {
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNick(playerName) : null;
		final String cmiNick = isCMILoaded() ? CMIHook.getNick(playerName) : null;

		return cmiNick != null ? cmiNick : essNick != null ? essNick : playerName;
	}

	/**
	 * Sets the given player's nickname for CMI and EssentialsX.
	 *
	 * @param playerId the player whose nickname you want to set.
	 * @param nick     the nickname to set.
	 */
	public static void setNick(@NonNull final UUID playerId, @Nullable String nick) {
		if (isEssentialsLoaded())
			essentialsHook.setNick(playerId, nick);

		if (isCMILoaded())
			CMIHook.setNick(playerId, nick);
	}

	/**
	 * Attempts to reverse lookup a player's name from their nickname.
	 * Only CMI and EssentialsX are supported.
	 *
	 * @param nick the nickname to use.
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
	 * Return the reply recipient for the given player, or null if it doesn't
	 * exist.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static Player getReplyTo(final Player player) {
		return isEssentialsLoaded() ? essentialsHook.getReplyTo(player.getName()) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// ItemsAdder
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Use ItemsAdder to replace font images in the message.
	 *
	 * @param component the message.
	 * @return
	 */
	public static SimpleComponent replaceFontImages(final SimpleComponent component) {
		return replaceFontImages(null, component);
	}

	/**
	 * Use ItemsAdder to replace font images in the message based on the player's permission
	 *
	 * @param player  the player to use.
	 * @param component the message.
	 * @return
	 */
	public static SimpleComponent replaceFontImages(@Nullable Player player, SimpleComponent component) {
		return isItemsAdderLoaded() ? itemsAdderHook.replaceFontImages(player, component) : component;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Multiverse-Core
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the world name's alias from Multiverse-Core.
	 *
	 * @param world the world to use.
	 * @return
	 */
	public static String getWorldAlias(final World world) {
		return isMultiverseCoreLoaded() ? multiverseHook.getWorldAlias(world.getName()) : world.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Towny
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the player's nation from Towny, or "" if it isn't loaded.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static String getNation(final Player player) {
		return isTownyLoaded() ? townyHook.getNationName(player) : "";
	}

	/**
	 * Return the player's town name from Towny, or "" if there is none.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static String getTownName(final Player player) {
		return isTownyLoaded() ? townyHook.getTownName(player) : "";
	}

	/**
	 * Return the online residents in the player's town, or an empty list.
	 *
	 * @param player the player's town to check.
	 * @return
	 */
	public static Collection<? extends Player> getTownResidentsOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getTownResidentsOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the online nation players in the player's nation, or an empty
	 * list.
	 *
	 * @param player the player's nation to check.
	 * @return
	 */
	public static Collection<? extends Player> getNationPlayersOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getNationPlayersOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the online nation players in the player's ally, or an empty list.
	 *
	 * @param player the player's ally to check.
	 * @return
	 */
	public static Collection<? extends Player> getAllyPlayersOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getAllyPlayersOnline(player) : new ArrayList<>();
	}

	/**
	 * Return the town owner name at the given location, or null if there is none.
	 *
	 * @param location the location to check.
	 * @return
	 */
	public static String getTownOwner(final Location location) {
		return isTownyLoaded() ? townyHook.getTownOwner(location) : null;
	}

	/**
	 * Return the town name at the given location, or null if there is none.
	 *
	 * @param location the location to check.
	 * @return
	 */
	public static String getTown(final Location location) {
		return isTownyLoaded() ? townyHook.getTownName(location) : null;
	}

	/**
	 * Return a list of all loaded towns, or an empty list if there are none.
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
	 * Return the player's prefix, or an empty string if they don't have one.
	 *
	 * @param player the player whose prefix you want to get.
	 * @return
	 */
	public static String getPlayerPrefix(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerPrefix(player) : "";
	}

	/**
	 * Return the player's suffix, or an empty string if they don't have one.
	 *
	 * @param player the player whose suffix you want to get.
	 * @return
	 */
	public static String getPlayerSuffix(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerSuffix(player) : "";
	}

	/**
	 * Return the player's permission group, or an empty string if they don't
	 * have one.
	 *
	 * @param player the player whose permission group you want to get.
	 * @return
	 */
	public static String getPlayerPermissionGroup(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerGroup(player) : "";
	}

	/**
	 * Return the player's balance from Vault (hooks into your economy plugin).
	 *
	 * @param player the player whose balance you want to get.
	 * @return
	 */
	public static double getBalance(final Player player) {
		return isVaultLoaded() ? vaultHook.getBalance(player) : 0;
	}

	/**
	 * Return the singular currency name, or empty if Vault isn't loaded.
	 *
	 * @return
	 */
	public static String getCurrencySingular() {
		return isVaultLoaded() ? vaultHook.getCurrencyNameSG() : "";
	}

	/**
	 * Return the plural currency name, or empty if Vault isn't loaded.
	 *
	 * @return
	 */
	public static String getCurrencyPlural() {
		return isVaultLoaded() ? vaultHook.getCurrencyNamePL() : "";
	}

	/**
	 * Takes the given amount of money from the player if Vault is installed.
	 *
	 * @param player the player to take the money from.
	 * @param amount the amount of money to take.
	 */
	public static void withdraw(final Player player, final double amount) {
		if (isVaultLoaded())
			vaultHook.withdraw(player, amount);
	}

	/**
	 * Gives the given amount of money to the player if Vault is installed.
	 *
	 * @param player the player to give the money to.
	 * @param amount the amount of money to give.
	 */
	public static void deposit(final Player player, final double amount) {
		if (isVaultLoaded())
			vaultHook.deposit(player, amount);
	}

	/**
	 * Checks if the given player has the given permission. This is
	 * safe to use for instances where the player may be a temporary
	 * player from ProtocolLib, where then we use Vault to check the
	 * player's permission.
	 *
	 * @param player the player to check.
	 * @param perm   the permission to check.
	 * @return
	 */
	public static boolean hasProtocolLibPermission(Player player, String perm) {
		if (isProtocolLibLoaded() && protocolLibHook.isTemporaryPlayer(player))
			return hasVaultPermission(player, perm);

		return player.hasPermission(perm);
	}

	/**
	 * Checks if the given player name has a certain permission using Vault,
	 * throwing an error if Vault is not present.
	 *
	 * @param offlinePlayer the player to check.
	 * @param perm          the permission to check.
	 *
	 * @return
	 */
	public static boolean hasVaultPermission(final OfflinePlayer offlinePlayer, final String perm) {
		Valid.checkBoolean(isVaultLoaded(), "hasVaultPermission called - Please install Vault to enable this functionality!");

		return vaultHook.hasPerm(offlinePlayer, perm);
	}

	/**
	 * Checks if the given command sender has the given permission.
	 *
	 * You are advised to do the following checks beforehand,
	 * this method does not have them for maximum performance:
	 *
	 * **SENDER AND PERMISSION MUST NOT BE NULL**
	 * **VAULT MUST BE INSTALLED**
	 *
	 * Returns NULL if Vault could not connect to a compatible Permission plugin.
	 * Returns TRUE or FALSE depending on the result of the Vault check.
	 * Returns FALSE on exception and fails silently by printing the error to the console.
	 *
	 * @param player
	 * @param permission
	 * @return
	 */
	public static Boolean hasVaultPermissionFast(final Player player, final String permission) {
		return vaultHook.hasPerm(player, permission);
	}

	/**
	 * Returns the player's primary permission group using Vault, or an empty
	 * string if they don't have one.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static String getPlayerPrimaryGroup(final Player player) {
		return isVaultLoaded() ? vaultHook.getPrimaryGroup(player) : "";
	}

	/**
	 * Returns true if Vault was able to find a suitable chat plugin to hook
	 * into.
	 *
	 * @return
	 */
	public static boolean isChatIntegrated() {
		return isVaultLoaded() ? vaultHook.isChatIntegrated() : false;
	}

	/**
	 * Returns true if Vault was able to find a suitable economy plugin to
	 * hook into.
	 *
	 * @return
	 */
	public static boolean isEconomyIntegrated() {
		return isVaultLoaded() ? vaultHook.isEconomyIntegrated() : false;
	}

	/**
	 * Updates the Vault service providers.
	 *
	 * @deprecated internal use only.
	 */
	@Deprecated
	public static void updateVaultIntegration() {
		if (isVaultLoaded())
			vaultHook.setIntegration();
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlaceholderAPI and MVdWPlaceholderAPI
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Uses PlaceholderAPI and MVdWPlaceholderAPI to replace placeholders in a
	 * message.
	 *
	 * @param player  the player to parse the placeholders against.
	 * @param message the message to parse the placeholders in.
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
	 *
	 * @return
	 */
	public static Map<String, Object> getPlaceholderAPIHooks() {
		return isPlaceholderAPILoaded() ? placeholderAPIHook.getHooks() : new HashMap<>();
	}

	/**
	 *
	 * @param placeholderExpansion
	 * @param identifier
	 * @param param
	 * @param params
	 * @return
	 */
	public static String getPlaceholderAPIValue(Object placeholderExpansion, String identifier, Player param, String params) {
		return isPlaceholderAPILoaded() ? placeholderAPIHook.getValue(placeholderExpansion, identifier, param, params) : "";
	}

	/**
	 * Uses PlaceholderAPI to replace relational placeholders in a message.
	 *
	 * @param one     the first player to compare.
	 * @param two     the second player to compare.
	 * @param message the message to parse the placeholders in.
	 * @return
	 */
	public static String replaceRelationPlaceholders(final Player one, final Player two, final String message) {
		if (message == null || "".equals(message.trim()))
			return message;

		return isPlaceholderAPILoaded() ? placeholderAPIHook.replaceRelationPlaceholders(one, two, message) : message;
	}

	/**
	 * Uses PlaceholderAPI to replace relational placeholders in a message.
	 *
	 * @param firstAudience
	 * @param secondAudience
	 * @param component
	 * @return
	 */
	public static SimpleComponent replaceRelationPlaceholders(@Nullable final FoundationPlayer firstAudience, @Nullable final FoundationPlayer secondAudience, final SimpleComponent component) {
		return isPlaceholderAPILoaded() ? placeholderAPIHook.replaceRelationPlaceholders(firstAudience, secondAudience, component) : component;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Factions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get all loaded Factions, or null if there are none.
	 *
	 * @return
	 */
	public static Collection<String> getFactions() {
		return isFactionsLoaded() ? factionsHook.getFactions() : null;
	}

	/**
	 * Return the player's faction, or null if they don't have one.
	 *
	 * @param player the player to check.
	 * @return
	 */
	public static String getFaction(final Player player) {
		return isFactionsLoaded() ? factionsHook.getFaction(player) : null;
	}

	/**
	 * Return the players in the player's faction, or empty if there are none.
	 *
	 * @param player the player's faction to check.
	 * @return
	 */
	public static Collection<? extends Player> getOnlineFactionPlayers(final Player player) {
		return isFactionsLoaded() ? factionsHook.getSameFactionPlayers(player) : new ArrayList<>();
	}

	/**
	 * Return a faction name at the given location, or null if there is none.
	 *
	 * @param location the location to check.
	 * @return
	 */
	public static String getFaction(final Location location) {
		return isFactionsLoaded() ? factionsHook.getFaction(location) : null;
	}

	/**
	 * Return the faction owner's name at the given location, or null if there
	 * is none.
	 *
	 * @param location the location to check.
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
	 * If the plugin is missing, an error will be thrown.
	 *
	 * @param adapter the adapter to add.
	 */
	public static void addPacketListener(/* Uses an Object to prevent errors if the plugin is not installed. */final Object adapter) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Cannot add packet listeners if ProtocolLib isn't installed");

		protocolLibHook.addPacketListener(adapter);
	}

	/**
	 * Removes a {@link PacketAdapter} packet listener from ProtocolLib.
	 * <p>
	 * If the plugin is missing, or the listener hasn't been registered, an error will be thrown
	 *
	 * @param adapter the adapter to remove.
	 */
	public static void removePacketListener(final Object adapter) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Cannot remove packet listeners if ProtocolLib isn't installed");

		protocolLibHook.removePacketListener(adapter);
	}

	/**
	 * Send a {@link PacketContainer} to the given player.
	 *
	 * @param player          the player to send the packet container to.
	 * @param packetContainer the packet container to send.
	 */
	public static void sendPacket(final Player player, final Object packetContainer) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Sending packets requires ProtocolLib to be installed and loaded");

		protocolLibHook.sendPacket(player, packetContainer);
	}

	// ------------------------------------------------------------------------------------------------------------
	// LWC
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the owner of the given block in LWC, or null if there is none.
	 *
	 * @param block the block to check.
	 * @return
	 */
	public static String getLWCOwner(final Block block) {
		return isLWCLoaded() ? lwcHook.getOwner(block) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Lockette Pro
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return if the given player owns the given block from Lockette Pro.
	 *
	 * @param block  the block to check.
	 * @param player the player to check.
	 * @return
	 */
	public static boolean isLocketteOwner(final Block block, final Player player) {
		return isLocketteProLoaded() ? locketteProHook.isOwner(block, player) : false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Residence
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a list of Residences, or an empty list if there are none.
	 *
	 * @return
	 */
	public static Collection<String> getResidences() {
		return isResidenceLoaded() ? residenceHook.getResidences() : new ArrayList<>();
	}

	/**
	 * Get the Residence name at the given location, or null if there is none.
	 *
	 * @param location the location to check.
	 * @return
	 */
	public static String getResidence(final Location location) {
		return isResidenceLoaded() ? residenceHook.getResidence(location) : null;
	}

	/**
	 * Get the Residence owner at the given location, or null if there is none.
	 *
	 * @param location the location to check.
	 * @return
	 */
	public static String getResidenceOwner(final Location location) {
		return isResidenceLoaded() ? residenceHook.getResidenceOwner(location) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// WorldGuard
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a list of regions at the given location, or an empty list if
	 * there are none.
	 *
	 * @param loc the location to check.
	 * @return
	 */
	public static List<String> getRegions(final Location loc) {
		return isWorldGuardLoaded() ? worldguardHook.getRegionsAt(loc) : new ArrayList<>();
	}

	/**
	 * Return a list of loaded regions, or an empty list if there are none.
	 *
	 * @return
	 */
	public static List<String> getRegions() {
		return isWorldGuardLoaded() ? worldguardHook.getAllRegions() : new ArrayList<>();
	}

	/**
	 * Get our representation of a WorldGuard region by its name, or null if
	 * there are none.
	 *
	 * @param name the name to use.
	 * @return
	 */
	public static Region getRegion(final String name) {
		return isWorldGuardLoaded() ? worldguardHook.getRegion(name) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlotSquared
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Get a list of players inside a plot, or empty if the plugin isn't loaded.
	 *
	 * @param players the player's location to check for players.
	 * @return
	 */
	public static Collection<? extends Player> getPlotPlayers(final Player players) {
		return isPlotSquaredLoaded() ? plotSquaredHook.getPlotPlayers(players) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Citizens
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the entity is a Citizens NPC.
	 *
	 * @param entity the entity to check.
	 * @return
	 */
	public static boolean isNPC(final Entity entity) {
		return isCitizensLoaded() ? citizensHook.isNPC(entity) : false;
	}

	/**
	 * Return the target of the entity.
	 *
	 * @param entity the entity to check.
	 * @return
	 */
	public static Entity getNPCTarget(final Entity entity) {
		return isCitizensLoaded() ? citizensHook.getNPCTarget(entity) : null;
	}

	/**
	 * Attemps to destroy entity, if it is a Citizens NPC.
	 *
	 * @param entity
	 */
	public static void destroyNPC(final Entity entity) {
		if (isCitizensLoaded())
			citizensHook.destroyNPC(entity);
	}

	// ------------------------------------------------------------------------------------------------------------
	// DiscordSRV
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return all linked Discord channels. You can link those in the config.yml
	 * of DiscordSRV.
	 *
	 * @return the linked channels, or an empty set if DiscordSRV is not loaded.
	 */
	public static Set<String> getDiscordChannels() {
		return isDiscordSRVLoaded() ? discordSRVHook.getChannels() : new HashSet<>();
	}

	/**
	 * Sends a message from the given sender to a certain channel on Discord
	 * using DiscordSRV.
	 * <p>
	 * Enhanced functionality is available if the sender is a player.
	 *
	 * @param sender  the sender to send the message from.
	 * @param channel the channel to send the message in.
	 * @param message the message to send.
	 */
	public static void sendDiscordMessage(final CommandSender sender, final String channel, @NonNull final String message) {
		if (isDiscordSRVLoaded())
			discordSRVHook.sendMessage(sender, channel, message);
	}

	/**
	 * Send a message to a Discord channel if DiscordSRV is installed.
	 *
	 * @param channel the channel to send the message in.
	 * @param message the message to send.
	 */
	public static void sendDiscordMessage(final String channel, @NonNull final String message) {
		if (isDiscordSRVLoaded())
			discordSRVHook.sendMessage(channel, message);
	}
}

// ------------------------------------------------------------------------------------------------------------
//
// Below are the individual classes responsible for hooking into third party plugins
// and getting data from them. Due to often changes we do not keep these documented.
//
// ------------------------------------------------------------------------------------------------------------

class AdvancedVanishHook {

	boolean isVanished(Player player) {
		final Class<?> clazz = ReflectionUtil.lookupClass("me.quantiom.advancedvanish.util.AdvancedVanishAPI");
		final Object instance = ReflectionUtil.getStaticFieldContent(clazz, "INSTANCE");

		final Method isPlayerVanished = ReflectionUtil.getMethod(clazz, "isPlayerVanished", Player.class);

		return ReflectionUtil.invoke(isPlayerVanished, instance, player);
	}

	void setVanished(Player player, boolean vanished) {
		final Class<?> clazz = ReflectionUtil.lookupClass("me.quantiom.advancedvanish.util.AdvancedVanishAPI");
		final Object instance = ReflectionUtil.getStaticFieldContent(clazz, "INSTANCE");

		if (vanished) {
			if (!this.isVanished(player)) {
				final Method vanishPlayer = ReflectionUtil.getMethod(clazz, "vanishPlayer", Player.class, boolean.class);

				ReflectionUtil.invoke(vanishPlayer, instance, player, false);
			}

		} else if (this.isVanished(player)) {
			final Method unVanishPlayer = ReflectionUtil.getMethod(clazz, "unVanishPlayer", Player.class, boolean.class);

			ReflectionUtil.invoke(unVanishPlayer, instance, player, false);
		}
	}
}

class AuthMeHook {

	private final Object instance;
	private final Method isAuthenticated;

	AuthMeHook() {
		final Class<?> apiClass = ReflectionUtil.lookupClass("fr.xephi.authme.api.v3.AuthMeApi");
		final Method getInstance = ReflectionUtil.getMethod(apiClass, "getInstance");

		this.instance = ReflectionUtil.invokeStatic(getInstance);
		this.isAuthenticated = ReflectionUtil.getMethod(apiClass, "isAuthenticated", Player.class);
	}

	boolean isLogged(final Player player) {
		return ReflectionUtil.invoke(this.isAuthenticated, this.instance, player);
	}
}

class EssentialsHook {

	private final Essentials ess;

	EssentialsHook() {
		this.ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}

	boolean hasGodMode(final Player player) {
		final User user = this.getUser(player.getName());

		return user != null ? user.isGodModeEnabled() : false;
	}

	void setGodMode(final Player player, final boolean godMode) {
		final User user = this.getUser(player.getName());

		if (user != null)
			user.setGodModeEnabled(godMode);
	}

	void setIgnore(final UUID player, final UUID toIgnore, final boolean ignore) {
		try {
			final com.earth2me.essentials.User user = this.ess.getUser(player);
			final com.earth2me.essentials.User toIgnoreUser = this.ess.getUser(toIgnore);

			if (toIgnoreUser != null)
				user.setIgnoredPlayer(toIgnoreUser, ignore);

		} catch (final Throwable t) {
		}
	}

	boolean isIgnoring(final UUID player, final UUID ignoringPlayer) {
		try {
			final com.earth2me.essentials.User user = this.ess.getUser(player);
			final com.earth2me.essentials.User ignored = this.ess.getUser(ignoringPlayer);

			return user != null && ignored != null && user.isIgnoredPlayer(ignored);

		} catch (final Throwable t) {
			return false;
		}
	}

	boolean isAfk(final String pl) {
		final IUser user = this.getUser(pl);

		return user != null ? user.isAfk() : false;
	}

	boolean isVanished(final String pl) {
		final IUser user = this.getUser(pl);

		return user != null ? user.isVanished() : false;
	}

	void setVanished(final String playerName, boolean vanished) {
		final IUser user = this.getUser(playerName);

		if (user != null && user.isVanished() != vanished)
			user.setVanished(false);
	}

	boolean isMuted(final String pl) {
		final com.earth2me.essentials.User user = this.getUser(pl);

		return user != null ? user.isMuted() : false;
	}

	Player getReplyTo(final String recipient) {
		final User user = this.getUser(recipient);

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
		final User user = this.getUser(player);

		if (user == null)
			return player;

		final String essNick = Common.getOrEmpty(user.getNickname());

		return "".equals(essNick) ? null : essNick;
	}

	void setNick(final UUID uniqueId, String nick) {
		final User user = this.getUser(uniqueId);

		if (user != null) {
			final SimpleComponent nickComponent = SimpleComponent.fromMini(nick);
			final boolean isEmpty = nick == null || nickComponent.toPlain().replace(" ", "").isEmpty();

			user.setNickname(isEmpty ? null : nickComponent.toLegacy());
		}
	}

	String getNameFromNick(String maybeNick) {
		maybeNick = SimpleComponent.fromMini(maybeNick).toPlain().toLowerCase();

		final UserMap users = this.ess.getUserMap();

		if (users != null)
			for (final UUID userId : users.getAllUniqueUsers()) {
				final User user = users.getUser(userId);

				if (user != null && user.getNickname() != null && SimpleComponent.fromMini(user.getNickname()).toPlain().toLowerCase().equals(maybeNick))
					return Common.getOrDefault(user.getName(), maybeNick);
			}

		return maybeNick;
	}

	void setBackLocation(final String player, final Location loc) {
		final User user = this.getUser(player);

		if (user != null)
			try {
				user.setLastLocation(loc);

			} catch (final Throwable t) {
			}
	}

	private User getUser(final String name) {
		User user = null;

		try {
			user = this.ess.getUser(name);
		} catch (final Throwable t) {
		}

		if (user != null)
			return user;

		if (this.ess.getUserMap() == null)
			return null;

		try {
			user = this.ess.getUserMap().getUser(name);
		} catch (final Throwable t) {
		}

		if (user != null)
			return user;

		try {
			final Method getUserFromBukkit = ReflectionUtil.getMethod(this.ess.getUserMap().getClass(), "getUserFromBukkit", String.class);

			if (getUserFromBukkit == null)
				user = this.ess.getUser(name);
			else
				user = ReflectionUtil.invoke(getUserFromBukkit, this.ess.getUserMap(), name);

		} catch (final Throwable ex) {
		}

		return user;
	}

	private User getUser(final UUID uniqueId) {
		if (this.ess.getUserMap() == null)
			return null;

		User user = null;

		try {
			user = this.ess.getUserMap().getUser(uniqueId);
		} catch (final Throwable t) {
		}

		if (user == null)
			try {
				user = this.ess.getUser(uniqueId);
			} catch (final Throwable ex) {
			}

		return user;
	}

}

class MultiverseHook {

	private final MultiverseCore multiVerse;

	MultiverseHook() {
		this.multiVerse = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
	}

	String getWorldAlias(final String world) {
		final MultiverseWorld mvWorld = this.multiVerse.getMVWorldManager().getMVWorld(world);

		if (mvWorld != null)
			return mvWorld.getColoredWorldString();

		return world;
	}
}

class TownyHook {

	Collection<? extends Player> getTownResidentsOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playersTown = this.getTownName(pl);

		if (!playersTown.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playersTown.equals(this.getTownName(online)))
					recipients.add(online);

		return recipients;
	}

	Collection<? extends Player> getNationPlayersOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playerNation = this.getNationName(pl);

		if (!playerNation.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playerNation.equals(this.getNationName(online)))
					recipients.add(online);

		return recipients;
	}

	Collection<? extends Player> getAllyPlayersOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final Resident resident = this.getResident(pl);

		if (resident != null)
			for (final Player online : Remain.getOnlinePlayers()) {
				final Resident otherResident = this.getResident(online);

				if (otherResident != null && otherResident.isAlliedWith(resident))
					recipients.add(online);
			}

		return recipients;
	}

	String getTownName(final Player pl) {
		final Town t = this.getTown(pl);

		return t != null ? t.getName() : "";
	}

	String getNationName(final Player pl) {
		final Nation n = this.getNation(pl);

		return n != null ? n.getName() : "";
	}

	List<String> getTowns() {
		try {
			//import com.palmergames.bukkit.towny.object.TownyUniverse;

			return Common.convertList(TownyUniverse.getInstance().getTowns(), Town::getName);

		} catch (final Throwable e) {
			return new ArrayList<>();
		}
	}

	String getTownName(final Location loc) {
		final Town town = this.getTown(loc);

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
			final Town town = this.getTown(loc);

			return town != null ? town.getMayor().getName() : null;

		} catch (final Throwable e) {
			return null;
		}
	}

	private Nation getNation(final Player pl) {
		final Town town = this.getTown(pl);

		try {
			return town.getNation();

		} catch (final Throwable ex) {
			return null;
		}
	}

	private Town getTown(final Player pl) {
		final Resident res = this.getResident(pl);

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
	private final Set<Object> registeredListeners = new HashSet<>();

	ProtocolLibHook() {
		this.manager = ProtocolLibrary.getProtocolManager();

		if (this.manager == null)
			Common.warning("Unable to get protocol manager. Ensure ProtocolLib threw no errors in your startup log and is compatible with your server version. "
					+ "If you're a developer, place ProtocolLib to softDepend in plugin.yml. Packet features won't function.");
	}

	final void addPacketListener(final Object listener) {
		Valid.checkBoolean(listener instanceof com.comphenix.protocol.events.PacketListener, "Listener must extend or implements com.comphenix.protocol.events.PacketListener or PacketAdapter");

		if (this.manager != null) {
			try {
				this.manager.addPacketListener((com.comphenix.protocol.events.PacketListener) listener);

			} catch (final Throwable t) {
				Common.error(t, "Failed to register ProtocolLib packet listener! Ensure you have the latest ProtocolLib. If you reloaded, try a fresh startup (some ProtocolLib esp. for 1.8.8 fails on reload).");

				return;
			}

			this.registeredListeners.add(listener);
		}
	}

	final void removePacketListener(final Object listener) {
		Valid.checkBoolean(listener instanceof com.comphenix.protocol.events.PacketListener, "Listener must extend or implements com.comphenix.protocol.events.PacketListener or PacketAdapter");

		if (this.manager != null) {
			Valid.checkBoolean(this.registeredListeners.contains(listener), "Listener must already be registered with ProtocolLib.");

			try {
				this.manager.removePacketListener((com.comphenix.protocol.events.PacketListener) listener);

			} catch (final Throwable t) {
				Common.error(t, "Failed to unregister ProtocolLib packet listener!");

				return;
			}

			this.registeredListeners.remove(listener);
		}
	}

	final void sendPacket(final PacketContainer packet) {
		for (final Player player : Remain.getOnlinePlayers())
			this.sendPacket(player, packet);
	}

	final void sendPacket(final Player player, final Object packet) {
		Valid.checkNotNull(player);
		Valid.checkBoolean(packet instanceof PacketContainer, "Packet must be instance of PacketContainer from ProtocolLib");

		if (this.manager != null)
			try {
				this.manager.sendServerPacket(player, (PacketContainer) packet);

			} catch (final Exception e) {
				Common.error(e, "Failed to send " + ((PacketContainer) packet).getType() + " packet to " + player.getName());
			}
	}

	final boolean isTemporaryPlayer(Player player) {
		try {
			return player != null && player.getClass().getSimpleName().contains("TemporaryPlayer"); // Solves compatibiltiy issues

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
		this.setIntegration();
	}

	void setIntegration() {
		final RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		final RegisteredServiceProvider<Chat> chatProvider = Bukkit.getServicesManager().getRegistration(Chat.class);
		final RegisteredServiceProvider<Permission> permProvider = Bukkit.getServicesManager().getRegistration(Permission.class);

		if (economyProvider != null)
			this.economy = economyProvider.getProvider();

		if (chatProvider != null)
			this.chat = chatProvider.getProvider();

		if (permProvider != null)
			this.permissions = permProvider.getProvider();
	}

	boolean isChatIntegrated() {
		return this.chat != null;
	}

	boolean isEconomyIntegrated() {
		return this.economy != null;
	}

	// ------------------------------------------------------------------------------
	// Economy
	// ------------------------------------------------------------------------------

	String getCurrencyNameSG() {
		return this.economy != null ? Common.getOrEmpty(this.economy.currencyNameSingular()) : "Money";
	}

	String getCurrencyNamePL() {
		return this.economy != null ? Common.getOrEmpty(this.economy.currencyNamePlural()) : "Money";
	}

	double getBalance(final Player player) {
		return this.economy != null ? this.economy.getBalance(player) : -1;
	}

	void withdraw(final Player player, final double amount) {
		if (this.economy != null)
			this.economy.withdrawPlayer(player.getName(), amount);
	}

	void deposit(final Player player, final double amount) {
		if (this.economy != null)
			this.economy.depositPlayer(player.getName(), amount);
	}

	// ------------------------------------------------------------------------------
	// Permissions
	// ------------------------------------------------------------------------------

	@Nullable
	Boolean hasPerm(final Player player, final String permission) {
		if (this.permissions == null)
			return null;

		try {
			return this.permissions.playerHas((World) null, player.getName(), permission);

		} catch (final Throwable t) {
			Common.logTimed(900,
					"SEVERE: Unable to ask Vault plugin if " + player.getName() + " has '" + permission + "' permission, returning false. "
							+ "This error only shows every 15 minutes. "
							+ "Run /vault-info and check if your permissions plugin is running correctly.");

			return false;
		}
	}

	Boolean hasPerm(@NonNull final OfflinePlayer player, final String perm) {
		try {
			return this.permissions != null ? perm != null ? this.permissions.playerHas((String) null, player, perm) : true : null;

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
			return this.permissions != null ? perm != null ? this.permissions.has((String) null, player, perm) : true : null;
		} catch (final UnsupportedOperationException t) {
			return false; // No supported plugin installed.
		}
	}

	Boolean hasPerm(@NonNull final String world, @NonNull final String player, final String perm) {
		try {
			return this.permissions != null ? perm != null ? this.permissions.has(world, player, perm) : true : null;
		} catch (final UnsupportedOperationException t) {
			return false; // No supported plugin installed.
		}
	}

	String getPrimaryGroup(final Player player) {
		try {
			return this.permissions != null ? this.permissions.getPrimaryGroup(player) : "";

		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	// ------------------------------------------------------------------------------
	// Prefix / Suffix
	// ------------------------------------------------------------------------------

	String getPlayerPrefix(final Player player) {
		try {
			return this.lookupVault(player, VaultPart.PREFIX);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	String getPlayerSuffix(final Player player) {
		try {
			return this.lookupVault(player, VaultPart.SUFFIX);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	String getPlayerGroup(final Player player) {
		try {
			return this.lookupVault(player, VaultPart.GROUP);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	private String lookupVault(final Player player, final VaultPart vaultPart) {
		if (this.chat == null)
			return "";

		final String[] groups = this.chat.getPlayerGroups(player);
		String fallback = vaultPart == VaultPart.PREFIX ? this.chat.getPlayerPrefix(player) : vaultPart == VaultPart.SUFFIX ? this.chat.getPlayerSuffix(player) : groups != null && groups.length > 0 ? groups[0] : "";

		if (fallback == null)
			fallback = "";

		if (vaultPart == VaultPart.PREFIX || vaultPart == VaultPart.SUFFIX)
			return fallback;

		final List<String> list = new ArrayList<>();

		if (!fallback.isEmpty())
			list.add(fallback);

		if (groups != null)
			for (final String group : groups) {
				final String part = vaultPart == VaultPart.PREFIX ? this.chat.getGroupPrefix(player.getWorld(), group) : vaultPart == VaultPart.SUFFIX ? this.chat.getGroupSuffix(player.getWorld(), group) : group;

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

final class PlaceholderAPIHook {

	private final VariablesInjector injector;
	private Map<String, Object> hooks;

	PlaceholderAPIHook() {
		injector = new VariablesInjector();

		try {
			injector.register();

		} catch (final Throwable throwable) {
			Common.error(throwable, "Failed to inject our variables into PlaceholderAPI!");
		}
	}

	void unregister() {
		if (injector != null)
			try {
				injector.unregister();

			} catch (final Throwable t) {
				// Silence, the plugin probably got removed in the meantime.
			}
	}

	String replacePlaceholders(final OfflinePlayer player, final String msg) {
		try {
			return this.setPlaceholders(player, msg);

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace variables!",
					"Player: " + (player == null ? "none" : player.getName()),
					"Message: " + msg,
					"Error: {error}");

			return msg;
		}
	}

	Map<String, Object> getHooks() {
		if (this.hooks == null) {
			this.hooks = new HashMap<>();

			// MineAcademy edit: Case insensitive
			for (final PlaceholderExpansion expansion : PlaceholderAPIPlugin.getInstance().getLocalExpansionManager().getExpansions())
				this.hooks.put(expansion.getIdentifier().toLowerCase(), expansion);
		}

		return this.hooks;
	}

	private String setPlaceholders(final OfflinePlayer player, String text) {
		return this.setPlaceholders(player, text, this.getHooks(), Variables.BRACKET_VARIABLE_PATTERN.matcher(text));
	}

	private String setPlaceholders(@Nullable OfflinePlayer player, String message, Map<String, Object> hooks, Matcher matcher) {
		while (matcher.find()) {
			String variable = matcher.group(1);
			boolean frontSpace = false;
			boolean backSpace = false;

			if (variable.startsWith("+")) {
				frontSpace = true;

				variable = variable.substring(1);
			}

			if (variable.endsWith("+")) {
				backSpace = true;

				variable = variable.substring(0, variable.length() - 1);
			}

			final int index = variable.indexOf("_");

			if (index <= 0 || index >= variable.length())
				continue;

			final String identifier = variable.substring(0, index).toLowerCase();
			final String params = variable.substring(index + 1);

			if (hooks.containsKey(identifier)) {
				String value = this.getValue(hooks.get(identifier), identifier, player, params);

				if (value != null) {
					value = Matcher.quoteReplacement(CompChatColor.translateColorCodes(value));

					message = message.replaceAll(Pattern.quote(matcher.group()), value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : ""));
				}
			}
		}

		return message;
	}

	String getValue(Object placeholderExpansion, String identifier, OfflinePlayer player, String params) {
		return ((PlaceholderExpansion) placeholderExpansion).onRequest(player, params);
	}

	String replaceRelationPlaceholders(final Player one, final Player two, String message) {
		try {
			final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();

			if (!hooks.isEmpty())
				message = this.setRelationalPlaceholders0(one, two, message, hooks, Variables.BRACKET_REL_VARIABLE_PATTERN.matcher(message));

			return message;

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace relation variables!",
					"Player one: " + one,
					"Player two: " + two,
					"Message: " + message,
					"Error: {error}");

			return message;
		}
	}

	SimpleComponent replaceRelationPlaceholders(@Nullable FoundationPlayer firstAudience, @Nullable FoundationPlayer secondAudience, SimpleComponent component) {
		final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();
		final boolean canReplace = firstAudience.isPlayer() && secondAudience.isPlayer();

		return component.replaceMatch(Variables.BRACKET_REL_VARIABLE_PATTERN, (matcher, input) -> {
			String text = input.content();

			try {
				final String format = matcher.group(2);
				final int index = format.indexOf("_");

				if (index <= 0 || index >= format.length())
					return input;

				final String identifier = format.substring(0, index);
				final String params = format.substring(index + 1);

				if (hooks.containsKey(identifier)) {
					final PlaceholderHook hook = hooks.get(identifier);

					if (hook instanceof Relational) {
						final Relational relational = (Relational) hook;
						final String value = canReplace ? relational.onPlaceholderRequest((Player) firstAudience, (Player) secondAudience, params) : null;

						text = text.replaceAll(Pattern.quote(matcher.group()), value != null ? Matcher.quoteReplacement(CompChatColor.translateColorCodes(value)) : "");
					}
				}

			} catch (final Throwable t) {
				Common.error(t,
						"PlaceholderAPI failed to replace relation variables!",
						"Player one: " + firstAudience,
						"Player two: " + secondAudience,
						"Variable: " + matcher.group(),
						"Component: " + component.toLegacy(),
						"Error: {error}");

			}
			return input.content(text);
		});
	}

	private String setRelationalPlaceholders0(final Player one, final Player two, String text, Map<String, PlaceholderHook> hooks, Matcher matcher) {
		while (matcher.find()) {
			final String format = matcher.group(2);
			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index);
			final String params = format.substring(index + 1);

			if (hooks.containsKey(identifier)) {
				final PlaceholderHook hook = hooks.get(identifier);

				if (hook instanceof Relational) {
					final Relational relational = (Relational) hook;
					final String value = one != null && two != null ? relational.onPlaceholderRequest(one, two, params) : "";

					if (value != null)
						text = text.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement(CompChatColor.translateColorCodes(value)));
				}
			}
		}

		return text;
	}

	private class VariablesInjector extends PlaceholderExpansion {

		/**
		 * Because this is an internal class,
		 * you must override this method to let PlaceholderAPI know to not
		 * unregister your expansion class when PlaceholderAPI is reloaded.
		 *
		 * @return true to persist through reloads.
		 */
		@Override
		public boolean persist() {
			return true;
		}

		/**
		 * Because this is an internal class, this check is not needed
		 * and we can simply return true.
		 *
		 * @return always true since it's an internal class.
		 */
		@Override
		public boolean canRegister() {
			return true;
		}

		/**
		 * The name of the person who created this expansion should go here.
		 * <br>For convienience we return the author from the plugin.yml.
		 *
		 * @return the name of the author as a String.
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
		 * <br>This must be unique and cannot contain % or _.
		 *
		 * @return The identifier in {@code %<identifier>_<value>%} as a String.
		 */
		@Override
		public String getIdentifier() {
			return SimplePlugin.getInstance().getName().toLowerCase().replace("%", "").replace(" ", "").replace("_", "");
		}

		/**
		 * This is the version of the expansion.
		 * <br>You don't have to use numbers, since it is set as a String.
		 * <p>
		 * For convenience we return the version from the plugin.yml.
		 *
		 * @return the version as a String.
		 */
		@Override
		public String getVersion() {
			return SimplePlugin.getInstance().getDescription().getVersion();
		}

		/**
		 * Replace Foundation variables but with our plugin name added as a
		 * prefix.
		 *
		 * We return null if an invalid placeholder (i.e. %ourplugin_nonexistingplaceholder%)
		 * is provided.
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

			try {
				for (final SimpleExpansion expansion : Variables.getExpansions()) {
					final SimpleComponent valueComponent = expansion.replacePlaceholders(Platform.toPlayer(player), identifier);

					if (valueComponent != null) {
						final boolean emptyColorless = valueComponent.toPlain().isEmpty();
						final String value = valueComponent.toLegacy();

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
					"MvdWPlaceholderAPI placeholders failed!",
					"Player: " + player.getName(),
					"Message: '" + message + "'",
					"Consider writing to the developer of that library",
					"first as this may be a bug we cannot handle!",
					"",
					"Your chat message will appear without replacements.");
		}

		return message;
	}
}

class PremiumVanishHook {

	private final Method isInvisible;
	private final Method hidePlayer;
	private final Method showPlayer;

	public PremiumVanishHook() {
		final Class<?> clazz = ReflectionUtil.lookupClass("de.myzelyam.api.vanish.VanishAPI");

		this.isInvisible = ReflectionUtil.getMethod(clazz, "isInvisible", Player.class);
		this.hidePlayer = ReflectionUtil.getMethod(clazz, "hidePlayer", Player.class, boolean.class, boolean.class);
		this.showPlayer = ReflectionUtil.getMethod(clazz, "showPlayer", Player.class, boolean.class);
	}

	boolean isVanished(Player player) {
		return ReflectionUtil.invokeStatic(this.isInvisible, player);
	}

	void setVanished(Player player, boolean vanished) {
		if (vanished) {
			if (!this.isVanished(player))
				ReflectionUtil.invokeStatic(this.hidePlayer, player, true, false);

		} else {
			if (this.isVanished(player))
				ReflectionUtil.invokeStatic(this.showPlayer, player, true);
		}
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

		this.legacy = !ok;
	}
}

class WorldGuardHook {

	private final boolean legacy;

	public WorldGuardHook(final WorldEditHook we) {
		final Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");

		this.legacy = !wg.getDescription().getVersion().startsWith("7") || we != null && we.legacy;
	}

	public List<String> getRegionsAt(final Location location) {
		final List<String> list = new ArrayList<>();

		this.getApplicableRegions(location).forEach(region -> {
			final String name = CompChatColor.stripColorCodes(region.getId());

			if (!name.startsWith("__"))
				list.add(name);
		});

		return list;
	}

	public Region getRegion(final String name) {
		for (final World w : Bukkit.getWorlds()) {
			final Object rm = this.getRegionManager(w);
			if (this.legacy)
				try {

					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;

						if (CompChatColor.stripColorCodes(((ProtectedRegion) regObj).getId()).equals(name)) {

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

					throw new FoException("Failed WorldEdit 6 legacy hook, see above and report");
				}
			else
				for (final ProtectedRegion reg : ((com.sk89q.worldguard.protection.managers.RegionManager) rm).getRegions().values())
					if (reg != null && reg.getId() != null && CompChatColor.stripColorCodes(reg.getId()).equals(name)) {
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
			final Object rm = this.getRegionManager(w);
			if (this.legacy)
				try {
					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					Method getId = null;
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;
						if (getId == null)
							getId = regObj.getClass().getMethod("getId");

						final String name = CompChatColor.stripColorCodes(getId.invoke(regObj).toString());

						if (!name.startsWith("__"))
							list.add(name);
					}
				} catch (final Throwable t) {
					t.printStackTrace();

					throw new FoException("Failed WorldEdit 6 legacy hook, see above and report");
				}
			else
				((com.sk89q.worldguard.protection.managers.RegionManager) rm)
						.getRegions().values().forEach(reg -> {
							if (reg == null || reg.getId() == null)
								return;

							final String name = CompChatColor.stripColorCodes(reg.getId());

							if (!name.startsWith("__"))
								list.add(name);
						});
		}

		return list;
	}

	private Iterable<ProtectedRegion> getApplicableRegions(final Location loc) {
		final Object rm = this.getRegionManager(loc.getWorld());

		if (this.legacy)
			try {
				return (Iterable<ProtectedRegion>) rm.getClass().getMethod("getApplicableRegions", Location.class).invoke(rm, loc);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldEdit 6 legacy hook, see above and report");
			}

		return ((com.sk89q.worldguard.protection.managers.RegionManager) rm)
				.getApplicableRegions(com.sk89q.worldedit.math.BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
	}

	private Object getRegionManager(final World w) {
		if (this.legacy)
			try {
				return Class.forName("com.sk89q.worldguard.bukkit.WGBukkit").getMethod("getRegionManager", World.class).invoke(null, w);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldGuard 6 legacy hook, see above and report");
			}

		// Causes class errors.
		//return com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(new com.sk89q.worldedit.bukkit.BukkitWorld(w));
		// Dynamically load modern WorldEdit.
		try {

			final Class<?> bwClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld");
			final Constructor<?> bwClassNew = bwClass.getConstructor(World.class);

			Object t = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null);
			t = t.getClass().getMethod("getPlatform").invoke(t);
			t = t.getClass().getMethod("getRegionContainer").invoke(t);
			return t.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World")).invoke(t, bwClassNew.newInstance(w));

		} catch (final Throwable t) {
			t.printStackTrace();

			throw new FoException("Failed WorldGuard hook, see above and report");
		}
	}
}

abstract class FactionsHook {

	/**
	 * Get all loaded factions.
	 */
	abstract Collection<String> getFactions();

	/**
	 * Get the faction of the player.
	 */
	abstract String getFaction(Player pl);

	/**
	 * Get the faction at the given location
	 */
	abstract String getFaction(Location loc);

	/**
	 * Get the faction owner at the given location.
	 */
	abstract String getFactionOwner(Location loc);

	/**
	 * Get all players in the same faction, used for party chat.
	 */
	final Collection<? extends Player> getSameFactionPlayers(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playerFaction = this.getFaction(pl);

		if (playerFaction != null && !playerFaction.isEmpty())
			for (final Player online : Remain.getOnlinePlayers()) {
				final String onlineFaction = this.getFaction(online);

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
		return Common.convertList(com.massivecraft.factions.entity.FactionColl.get().getAll(), object -> CompChatColor.stripColorCodes(object.getName()));
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
			final Object i = this.instance();
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
			final Object fplayers = this.fplayers();
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
		final Object f = this.findFaction(loc);

		try {
			return f != null ? f.getClass().getMethod("getTag").invoke(f).toString() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFactionOwner(final Location loc) {
		final Object faction = this.findFaction(loc);

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

		final Class<?> plotPlayerClass = ReflectionUtil.lookupClass((this.legacy ? "com.intellectualcrafters.plot.object" : "com.plotsquared.core.player") + ".PlotPlayer");
		Method wrap;

		try {
			wrap = plotPlayerClass.getMethod("from", Object.class);

		} catch (final ReflectiveOperationException ex3) {
			try {
				wrap = plotPlayerClass.getMethod("wrap", Object.class);

			} catch (final ReflectiveOperationException ex2) {
				try {
					wrap = plotPlayerClass.getMethod("wrap", Player.class);

				} catch (final ReflectiveOperationException ex) {
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
		final CMIUser user = this.getUser(player);

		return user != null && user.isVanished();
	}

	void setVanished(Player player, boolean vanished) {
		final CMIUser user = this.getUser(player);

		if (user != null && user.isVanished() != vanished)
			user.setVanished(false);
	}

	boolean isAfk(final Player player) {
		final CMIUser user = this.getUser(player);

		return user != null && user.isAfk();
	}

	boolean isMuted(final Player player) {
		final CMIUser user = this.getUser(player);

		try {
			return user != null && user.getMutedUntil() != 0 && user.getMutedUntil() != null && user.getMutedUntil() > System.currentTimeMillis();

		} catch (final Exception ex) {
			return false;
		}
	}

	boolean hasGodMode(final Player player) {
		final CMIUser user = this.getUser(player);

		return user != null ? user.isGod() : false;
	}

	void setGodMode(final Player player, final boolean godMode) {
		final CMIUser user = this.getUser(player);

		if (user != null)
			try {
				CMI.getInstance().getNMS().changeGodMode(player, godMode);

			} catch (final Throwable tt) {
				try {
					final Method setGod = CMIUser.class.getMethod("setGod", Boolean.class);

					setGod.invoke(user, godMode);

				} catch (final Throwable t) {
					// unavailable
				}
			}
	}

	void setLastTeleportLocation(final Player player, final Location location) {
		final CMIUser user = this.getUser(player);

		try {
			user.getClass().getMethod("setLastTeleportLocation", Location.class).invoke(user, location);
		} catch (final Throwable t) {
			// Silently fail.
		}
	}

	void setIgnore(final UUID player, final UUID who, final boolean ignore) {
		final CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);

		if (ignore)
			user.addIgnore(who, true /* Save now. */);
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
		final CMIUser user = this.getUser(player);
		final String nick = user == null ? null : user.getNickName();

		return nick == null || "".equals(nick) ? null : nick;
	}

	String getNick(final String playerName) {
		final CMIUser user = this.getUser(playerName);
		final String nick = user == null ? null : user.getNickName();

		return nick == null || "".equals(nick) ? null : nick;
	}

	void setNick(final UUID uniqueId, String nick) {
		final CMIUser user = this.getUser(uniqueId);
		final TabListManager tabManager = CMI.getInstance().getTabListManager();

		if (user != null) {
			final boolean isEmpty = nick == null || CompChatColor.stripColorCodes(nick).replace(" ", "").isEmpty();

			user.setNickName(isEmpty ? null : CompChatColor.translateColorCodes(nick), true);
			user.updateDisplayName();

			if (tabManager.isUpdatesOnNickChange())
				tabManager.updateTabList(3);
		}
	}

	String getNameFromNick(String nick) {
		nick = CompChatColor.stripColorCodes(nick).toLowerCase();

		for (final CMIUser user : CMI.getInstance().getPlayerManager().getAllUsers().values())
			if (user != null && user.getNickName() != null && CompChatColor.stripColorCodes(user.getNickName()).toLowerCase().equals(nick))
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
		try {
			final NPCRegistry reg = CitizensAPI.getNPCRegistry();

			return reg != null ? reg.isNPC(entity) : false;
		} catch (final NoClassDefFoundError err) {
			Common.logTimed(60 * 30, "Unable to check if " + entity + " is Citizens NPC, got " + err + ". This error only shows once per 30min.");

			return false;
		}
	}

	Entity getNPCTarget(Entity entity) {
		final NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);

		if (npc != null) {
			final EntityTarget target = npc.getNavigator().getEntityTarget();

			if (target != null)
				return target.getTarget();
		}

		return null;
	}

	void destroyNPC(Entity entity) {
		final NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);

		if (npc != null)
			npc.destroy();
	}
}

class DiscordSRVHook {

	Set<String> getChannels() {
		return DiscordSRV.getPlugin().getChannels().keySet();
	}

	boolean sendMessage(final String channel, final String message) {
		return this.sendMessage(null, channel, message);
	}

	boolean sendMessage(@Nullable CommandSender sender, final String channel, String message) {
		message = CompChatColor.stripColorCodes(message);

		if (message.replace(" ", "").isEmpty())
			return false;

		final TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// The channel is not configured in the config.yml of Discord,
		// so we can ignore it.
		if (textChannel == null) {
			Debugger.debug("discord", "[MC->Discord] Could not find Discord channel '" + channel + "'. Available: " + String.join(", ", this.getChannels()) + ". Not sending: " + message);

			return false;
		}

		if (sender instanceof Player) {
			Debugger.debug("discord", "[MC->Discord] " + sender.getName() + " send message to '" + channel + "' channel. Message: '" + message + "'");

			final DiscordSRV instance = JavaPlugin.getPlugin(DiscordSRV.class);

			// Dirty: We have to temporarily set a configuration value in
			// DiscordSRV to enable the processChatMessage method to function.
			final String key = "DiscordChatChannelMinecraftToDiscord";
			final Map<String, Object> runtimeValues = ReflectionUtil.getFieldContent(DiscordSRV.config(), "runtimeValues");
			final Object oldValue = runtimeValues.get(key);

			runtimeValues.put(key, true);

			try {
				instance.processChatMessage((Player) sender, message, channel, false);

			} finally {
				if (oldValue == null)
					runtimeValues.remove(key);
				else
					runtimeValues.put(key, oldValue);
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
	 * Return true if the given player is muted.
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

class BentoBoxHook {

	Set<UUID> getIslandVisitors(Player player) {
		return this.getIslandUsers(player, RanksManager.VISITOR_RANK);
	}

	Set<UUID> getIslandCoops(Player player) {
		return this.getIslandUsers(player, RanksManager.COOP_RANK);
	}

	Set<UUID> getIslandTrustees(Player player) {
		return this.getIslandUsers(player, RanksManager.TRUSTED_RANK);
	}

	Set<UUID> getIslandMembers(Player player) {
		return this.getIslandUsers(player, RanksManager.MEMBER_RANK);
	}

	Set<UUID> getIslandSubOwners(Player player) {
		return this.getIslandUsers(player, RanksManager.SUB_OWNER_RANK);
	}

	Set<UUID> getIslandOwners(Player player) {
		return this.getIslandUsers(player, RanksManager.OWNER_RANK);
	}

	Set<UUID> getIslandMods(Player player) {
		return this.getIslandUsers(player, RanksManager.MOD_RANK);
	}

	Set<UUID> getIslandAdmins(Player player) {
		return this.getIslandUsers(player, RanksManager.ADMIN_RANK);
	}

	private Set<UUID> getIslandUsers(Player player, int rank) {
		final IslandsManager manager = BentoBox.getInstance().getIslands();
		final Optional<Island> maybeIsland = manager.getIslandAt(player.getLocation());

		if (maybeIsland.isPresent()) {
			final Island island = maybeIsland.get();

			return island.getMemberSet(rank);

		} else {
			final UUID uniqueId = player.getUniqueId();

			for (final World world : Bukkit.getWorlds()) {
				try {
					final Island island = manager.getIsland(world, uniqueId);

					if (island != null)
						return island.getMemberSet(rank);

				} catch (final Throwable t) {
				}
			}
		}

		return new HashSet<>();
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
			Common.log("Unable to check if " + entity + " is a Boss. Is the API hook outdated? Got: " + t);
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
			this.legacyVersion = true;

		else if (version.startsWith("5."))
			this.legacyVersion = false;

		else
			Common.warning("Skipping hooking into unsupported MythicMob version " + version + "! Only 4.X.X and 5.X.X are supported.");

	}

	/*
	 * Attempt to return a MythicMob name from the given entity,
	 * or null if the entity is not a MythicMob.
	 */
	String getBossName(Entity entity) {
		if (this.legacyVersion == null)
			return null;

		if (this.legacyVersion)
			return this.getBossNameV4(entity);

		return this.getBossNameV5Direct(entity);
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

		return Remain.getEntityName(entity);
	}

	private String getBossNameV5Direct(Entity entity) {
		final UUID ourUniqueId = entity.getUniqueId();
		final MobManager mobManager = MythicProvider.get().getMobManager();

		for (final ActiveMob mob : mobManager.getActiveMobs())
			if (ourUniqueId.equals(mob.getUniqueId()))
				return mob.getName();

		return Remain.getEntityName(entity);
	}
}

class LandsHook {

	private final Object landsClass;
	private final Method getArea;
	private final Method getLand;
	private final Method getName;

	LandsHook() {
		final Class<?> lands = ReflectionUtil.lookupClass("me.angeschossen.lands.api.LandsIntegration");
		final Class<?> area = ReflectionUtil.lookupClass("me.angeschossen.lands.api.land.Area");
		final Class<?> land = ReflectionUtil.lookupClass("me.angeschossen.lands.api.land.Land");

		final Method of = ReflectionUtil.getMethod(lands, "of", Plugin.class);

		this.landsClass = ReflectionUtil.invokeStatic(of, SimplePlugin.getInstance());
		this.getArea = ReflectionUtil.getMethod(lands, "getArea", Location.class);
		this.getLand = ReflectionUtil.getMethod(area, "getLand");
		this.getName = ReflectionUtil.getMethod(land, "getName");
	}

	Collection<Player> getLandPlayers(Player sender) {
		final List<Player> playersAtLocation = new ArrayList<>();

		final Object senderArea = ReflectionUtil.invoke(this.getArea, this.landsClass, sender.getLocation());
		final Object senderLand = senderArea != null ? ReflectionUtil.invoke(this.getLand, senderArea) : null;

		final boolean senderInWilderness = senderLand == null;
		final String senderLandName = senderInWilderness ? "" : ReflectionUtil.invoke(this.getName, senderLand);

		for (final Player recipient : Remain.getOnlinePlayers()) {

			final Object recipientArea = ReflectionUtil.invoke(this.getArea, this.landsClass, recipient.getLocation());
			final Object recipientLand = recipientArea != null ? ReflectionUtil.invoke(this.getLand, recipientArea) : null;
			final boolean recipientInWilderness = recipientLand == null;

			// Both in wilderness
			if (recipientInWilderness && senderInWilderness)
				playersAtLocation.add(recipient);

			// Other player in land
			if (senderLand == null)
				continue;

			if (recipientLand != null && ReflectionUtil.invoke(this.getName, recipientLand).equals(senderLandName))
				playersAtLocation.add(recipient);
		}

		return playersAtLocation;
	}
}

class LiteBansHook {

	private final Set<String> mutedPlayerUids = new HashSet<>();
	private final Object instance;
	private final Method methodPrepareStatement;

	LiteBansHook() {
		final Class<?> classDatabase = ReflectionUtil.lookupClass("litebans.api.Database");

		this.instance = ReflectionUtil.invokeStatic(classDatabase, "get");
		this.methodPrepareStatement = ReflectionUtil.getMethod(classDatabase, "prepareStatement", String.class);

		// LiteBans throws an artificial exception when run on main thread on Bukkit, so we store all data in memory
		// for maximum performance, cached every 5 seconds, see
		// https://gitlab.com/ruany/LiteBansAPI/-/blob/master/src/main/java/litebans/api/Database.java
		Platform.runTaskTimerAsync(20 * 5, new BukkitRunnable() {

			@Override
			public void run() {
				mutedPlayerUids.clear();

				try (PreparedStatement statement = ReflectionUtil.invoke(methodPrepareStatement, instance, "SELECT * FROM {mutes}")) {
					statement.execute();

					final ResultSet resultSet = statement.getResultSet();

					while (resultSet.next()) {
						final String uuid = resultSet.getString("UUID");
						final boolean active = resultSet.getBoolean("ACTIVE");
						final long until = resultSet.getLong("UNTIL");

						if (active) {
							if (until != 0 && until < System.currentTimeMillis())
								continue;

							mutedPlayerUids.add(uuid);
						}
					}

				} catch (final Throwable t) {
					Common.error(t, "Error while fetching mutes from LiteBans, aborting. Is the integration outdated?");

					this.cancel();
				}
			}
		});
	}

	/*
	 * Return true if the given player is muted.
	 */
	boolean isMuted(final Player player) {
		return this.mutedPlayerUids.contains(player.getUniqueId().toString());
	}
}

class ItemsAdderHook {

	private Class<?> itemsAdder;
	private Method replaceFontImages;
	private Method replaceFontImagesNoPlayer;
	private Method replaceFontImagesAdventure;
	private Method replaceFontImagesAdventureNoPlayer;
	private boolean failed = false;

	ItemsAdderHook() {
	}

	SimpleComponent replaceFontImages(@Nullable final Player player, final SimpleComponent component) {

		if (this.replaceFontImages == null) {

			// integration failed, do not spam
			if (this.failed)
				return component;

			try {
				this.itemsAdder = ReflectionUtil.lookupClass("dev.lone.itemsadder.api.FontImages.FontImageWrapper");

				this.replaceFontImages = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", Permissible.class, String.class);
				this.replaceFontImagesNoPlayer = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", String.class);

				this.replaceFontImagesAdventure = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", Permissible.class, Component.class);
				this.replaceFontImagesAdventureNoPlayer = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", Component.class);

			} catch (final Throwable original) {
				try {
					this.replaceFontImages = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", Player.class, String.class);
					this.replaceFontImagesNoPlayer = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", String.class);

				} catch (final Throwable tt) {
					Common.warning("Unable to resolve ItemsAdder API. The plugin will continue to function, but no font images will be replaced. Is the integration outdated?");

					original.printStackTrace();
					this.failed = true;
				}
			}
		}

		if (player == null) {
			if (replaceFontImagesAdventureNoPlayer != null)
				return ReflectionUtil.invokeStatic(this.replaceFontImagesAdventureNoPlayer, component.toAdventure());
			else
				return ReflectionUtil.invokeStatic(this.replaceFontImagesNoPlayer, component.toLegacy());
		}

		if (replaceFontImagesAdventure != null)
			return SimpleComponent.fromAdventure(ReflectionUtil.invokeStatic(this.replaceFontImagesAdventure, player, component.toAdventure()));
		else
			return SimpleComponent.fromSection(ReflectionUtil.invokeStatic(this.replaceFontImages, player, component.toLegacy()));
	}
}