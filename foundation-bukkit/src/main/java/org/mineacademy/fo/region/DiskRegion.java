package org.mineacademy.fo.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.platform.SimplePlugin;
import org.mineacademy.fo.settings.ConfigItems;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Represents a region in a yml file in regions/ folder in your plugin's folder.
 *
 * To use this, enable regions in {@link SimplePlugin#areRegionsEnabled()}.
 */
@Getter
public final class DiskRegion extends YamlConfig {

	/**
	 * All loaded disk regions
	 */
	private static final ConfigItems<DiskRegion> loadedRegions = ConfigItems.fromFolder("regions", DiskRegion.class);

	/**
	 * The way for us to get the created region for a player, which is typically used in PlayerCache
	 * in plugins and this class is not available at the library level.
	 *
	 * Example: DiskRegion.setRegionGetter(player -> PlayerCache.from(player).getCreatedRegion());
	 */
	@Setter
	private static Function<Player, VisualizedRegion> regionGetter;

	/**
	 * The region object
	 */
	private VisualizedRegion border;

	/*
	 * Create a new region from disk
	 */
	private DiskRegion(String name) {
		this(name, null);
	}

	/*
	 * Create a new region from command
	 */
	private DiskRegion(String name, @Nullable VisualizedRegion border) {
		this.border = border;

		final SimpleCommandGroup defaultGroup = Platform.getPlugin().getDefaultCommandGroup();
		Valid.checkNotNull(defaultGroup, "Cannot use DiskRegion without default command group! Set Main_Command_Aliases key in settings.yml!");
		final String label = defaultGroup.getLabel();

		this.setHeader(
				Common.configLine(),
				"This file stores a cuboid region.",
				"",
				"To create one, get the region tool via '/" + label + " tools' and follow",
				"instructions. To remove a region, use the '/" + label + " region' command",
				"or stop your server and remove this file.",
				Common.configLine() + "\n");

		this.load(NO_DEFAULT, "regions/" + name + ".yml");
	}

	/**
	 * @see org.mineacademy.org.mineacademy.fo.settings.YamlConfig#onLoad()
	 */
	@Override
	protected void onLoad() {

		// Only load if not created via command
		if (this.border != null) {
			this.save();

			return;
		}

		final SerializedMap map = SerializedMap.of(this);

		try {
			this.border = VisualizedRegion.deserialize(map);

		} catch (Throwable ex) {

			while (ex.getCause() != null)
				ex = ex.getCause();

			if (ex instanceof InvalidWorldException)
				Common.log("Skipping region with invalid world. Region data: " + map);

			else
				Common.error(ex, "Failed to load region from map: " + map);
		}
	}

	@Override
	public void onSave() {
		if (this.border != null)
			for (final Map.Entry<String, Object> entry : this.border.serialize().entrySet())
				this.set(entry.getKey(), entry.getValue());
	}

	/**
	 * Return the name of this region
	 *
	 * @param name
	 */
	public void setName(@NonNull String name) {
		if (this.border != null)
			this.border.setName(name);
		else
			this.border = new VisualizedRegion(name, null, null);

		this.save();
	}

	/**
	 * Return {@link Region#getPrimary()}
	 *
	 * @return
	 */
	public Location getPrimary() {
		return this.border != null ? this.border.getPrimary() : null;
	}

	/**
	 * Set the primary location
	 *
	 * @param primary
	 */
	public void setPrimary(Location primary) {
		if (this.border != null)
			this.border.setPrimary(primary);
		else
			this.border = new VisualizedRegion("", primary, null);

		this.save();
	}

	/**
	 * Return {@link Region#getSecondary()}
	 *
	 * @return
	 */
	public Location getSecondary() {
		return this.border != null ? this.border.getSecondary() : null;
	}

	/**
	 * Set the secondary location
	 *
	 * @param secondary
	 */
	public void setSecondary(Location secondary) {
		if (this.border != null)
			this.border.setSecondary(secondary);
		else
			this.border = new VisualizedRegion("", null, secondary);

		this.save();
	}

	/**
	 * Return {@link Region#getCenter()}
	 *
	 * @return
	 */
	public Location getCenter() {
		return this.border != null ? this.border.getCenter() : null;
	}

	/**
	 * Return {@link Region#getBlocks()}
	 *
	 * @return
	 */
	public List<Block> getBlocks() {
		return this.border != null ? this.border.getBlocks() : Arrays.asList();
	}

	/**
	 * Return {@link Region#isWithin(Location)}
	 *
	 * @param location
	 * @return
	 */
	public boolean isWithin(Location location) {
		return this.border != null ? this.border.isWithin(location) : false;
	}

	/**
	 * Teleport player to region center
	 *
	 * @param player
	 */
	public void teleportToCenter(Player player) {
		if (this.border != null && this.border.isWhole())
			this.border.teleportToCenter(player);
	}

	/**
	 * Visualize this region for player
	 *
	 * @param player
	 */
	public void visualize(Player player) {
		this.visualize(player, null);
	}

	/**
	 * Visualize this region for player
	 *
	 * @param player
	 * @param color
	 */
	public void visualize(Player player, Color color) {
		Valid.checkNotNull(this.border, "Cannot call visualize using a region with no border");

		if (!this.border.canSeeParticles(player)) {

			if (color != null)
				this.border.setDelayTicks(8);

			this.border.showParticles(player, color, 10 * 20);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DiskRegion && ((DiskRegion) obj).border != null && this.getName().equals(((DiskRegion) obj).getName());
	}

	@Override
	public String toString() {
		return "DiskRegion{name=" + getName() + ", primary=" + SerializeUtil.serializeLoc(getPrimary()) + ", secondary=" + SerializeUtil.serializeLoc(getSecondary()) + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the region the player is currently creating
	 *
	 * @param player
	 * @return
	 */
	public static VisualizedRegion getCreatedRegion(Player player) {
		Valid.checkNotNull(regionGetter, "Please call DiskRegion#setRegionGetter before getting the region for player!");

		final VisualizedRegion region = regionGetter.apply(player);
		Valid.checkNotNull(region, "Wrong implementation! Player " + player.getName() + " has null region! Always return a non-empty region in DiskRegion#setRegionGetter");

		return region;
	}

	/**
	 * @param name
	 * @param region
	 * @return
	 * @see ConfigItems#loadOrCreateItem(String)
	 */
	public static DiskRegion createRegion(@NonNull final String name, @NonNull final VisualizedRegion region) {
		return loadedRegions.loadOrCreateItem(name, () -> new DiskRegion(name, region));
	}

	/**
	 * @see ConfigItems#loadItems()
	 */
	public static void loadRegions() {
		loadedRegions.loadItems();
	}

	/**
	 * Remove the given region
	 *
	 * @param region
	 */
	public static void removeRegion(final DiskRegion region) {
		loadedRegions.removeItem(region);
	}

	/**
	 * @param name
	 * @return
	 * @see ConfigItems#isItemLoaded(String)
	 */
	public static boolean isRegionLoaded(final String name) {
		return loadedRegions.isItemLoaded(name);
	}

	/**
	 * @param name
	 * @return
	 * @see ConfigItems#findItem(String)
	 */
	public static DiskRegion findRegion(@NonNull final String name) {
		return loadedRegions.findItem(name);
	}

	/**
	 * Return regions in which the entity is
	 *
	 * @param entity
	 * @return
	 */
	public static List<DiskRegion> findRegions(final Entity entity) {
		return findRegions(entity.getLocation());
	}

	/**
	 * Return regions in the given location
	 *
	 * @param location
	 * @return
	 */
	public static List<DiskRegion> findRegions(final Location location) {
		final List<DiskRegion> foundRegions = new ArrayList<>();

		for (final DiskRegion region : getRegions())
			if (region.border != null && region.border.isWhole() && region.border.isWithin(location))
				foundRegions.add(region);

		return foundRegions;
	}

	/**
	 * Return regions in the given location
	 *
	 * @param location
	 * @return
	 */
	public static List<String> findRegionNames(final Location location) {
		final List<String> foundRegions = new ArrayList<>();

		for (final DiskRegion region : getRegions())
			if (region.border != null && region.border.isWhole() && region.border.isWithin(location))
				foundRegions.add(region.getName());

		return foundRegions;
	}

	/**
	 * @return
	 * @see ConfigItems#getItems()
	 */
	public static Collection<DiskRegion> getRegions() {
		return loadedRegions.getItems();
	}

	/**
	 * @return
	 * @see ConfigItems#getItemNames()
	 */
	public static Set<String> getRegionNames() {
		return loadedRegions.getItemNames();
	}
}
