package org.mineacademy.fo.remain;

import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType.PrimitivePersistentDataType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBTCompound;
import org.mineacademy.fo.remain.nbt.NBTItem;

/**
 * Utility class for persistent metadata manipulation
 *
 * We apply scoreboard tags to ensure permanent metadata storage
 * if supported, otherwise it is lost on reload
 */
public class CompMetadata {

	/**
	 * The tag delimiter
	 */
	private final static String DELIMITER = "%-%";

	// Static access
	private CompMetadata() {
	}

	// ----------------------------------------------------------------------------------------
	// Setting metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * A shortcut for setting a tag with key-value pair on an item
	 *
	 * @param item
	 * @param compoundTag
	 * @param key
	 * @param value
	 * @return
	 */
	public static ItemStack setMetadata(ItemStack item, String key, String value) {
		Valid.checkNotNull(item, "Setting NBT tag got null item");

		final NBTItem nbt = new NBTItem(item);
		final NBTCompound tag = nbt.addCompound(FoConstants.NBT.TAG);

		tag.setString(key, value);
		return nbt.getItem();
	}

	/**
	 * Attempts to set a persistent metadata for entity
	 *
	 * @param entity
	 * @param tag
	 */
	public static final void setMetadata(Entity entity, String tag) {
		setMetadata(entity, tag, "null");
	}

	/**
	 * Attempts to set a persistent metadata tag with value for entity
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	public static final void setMetadata(Entity entity, String key, String value) {
		Valid.checkNotNull(entity);

		final String tag = format(key, value);

		try {
			if (!entity.getScoreboardTags().contains(tag))
				entity.addScoreboardTag(tag);

		} catch (NoSuchMethodError | NoSuchFieldError ex) {
			checkNBTAPI();

			try {
				de.tr7zw.nbtinjector.NBTInjector.getNbtData(entity).setString(key, value);
			} catch (final Throwable t) {
				Common.error(t, "Failed to set NBT tag for " + entity.getType() + ". Tag: " + key + ", value: " + value);
			}
		}
	}

	// Format the syntax of stored tags
	private static final String format(String key, String value) {
		return SimplePlugin.getNamed() + DELIMITER + key + DELIMITER + value;
	}

	/**
	 * Sets persistent tile entity metadata
	 *
	 * @param tileEntity
	 * @param key
	 * @param value
	 */
	public static final void setMetadata(BlockState tileEntity, String key, String value) {
		Valid.checkNotNull(tileEntity);
		Valid.checkNotNull(key);
		Valid.checkNotNull(value);

		if (MinecraftVersion.atLeast(V.v1_14)) {
			Valid.checkBoolean(tileEntity instanceof TileState, "BlockState must be instance of a TileState not " + tileEntity);

			final TileState tile = (TileState) tileEntity;
			setNamedspaced(tile, key, value);
		}

		else {
			checkNBTAPI();

			try {
				de.tr7zw.nbtinjector.NBTInjector.getNbtData(tileEntity).setString(key, value);
			} catch (final Throwable t) {
				Common.error(t, "Failed to set NBT tag for " + tileEntity.getType() + ". Tag: " + key + ", value: " + value);
			}
		}

		tileEntity.update();
	}

	private static final void setNamedspaced(TileState tile, String key, String value) {
		tile.getPersistentDataContainer().set(new NamespacedKey(SimplePlugin.getInstance(), key), PrimitivePersistentDataType.STRING, value);
	}

	// ----------------------------------------------------------------------------------------
	// Getting metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * A shortcut from reading a certain key from an item's given compound tag
	 *
	 * @param item
	 * @param compoundTag
	 * @param key
	 * @return
	 */
	public static String getMetadata(ItemStack item, String key) {
		Valid.checkNotNull(item, "Reading NBT tag got null item");

		final String compoundTag = FoConstants.NBT.TAG;
		final NBTItem nbt = new NBTItem(item);
		final String name = nbt.hasKey(compoundTag) ? nbt.getCompound(compoundTag).getString(key) : null;

		return name;
	}

	/**
	 * Attempts to get the entity's metadata, first from scoreboard tag,
	 * second from Bukkit metadata
	 *
	 * @param entity
	 * @param key
	 * @return the tag, or null
	 */
	public static final String getMetadata(Entity entity, String key) {
		Valid.checkNotNull(entity);

		try {
			for (final String line : entity.getScoreboardTags()) {
				final String tag = getTag(line, key);

				if (tag != null)
					return tag;
			}

		} catch (NoSuchMethodError | NoSuchFieldError ex) {
			checkNBTAPI();

			try {
				return de.tr7zw.nbtinjector.NBTInjector.getNbtData(entity).getString(key);
			} catch (final Throwable t) {
				Common.error(t, "Failed to get NBT tag for " + entity.getType() + ". Tag: " + key);
			}
		}

		return null;
	}

	// Parses the tag and gets its value
	private static final String getTag(String raw, String key) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(key) ? parts[2] : null;
	}

	/**
	 * Return saved tile entity metadata, or null if none
	 *
	 * @param tileEntity
	 * @param key
	 * @return
	 */
	public static final String getMetadata(BlockState tileEntity, String key) {
		Valid.checkNotNull(tileEntity);
		Valid.checkNotNull(key);

		if (MinecraftVersion.atLeast(V.v1_14)) {
			Valid.checkBoolean(tileEntity instanceof TileState, "BlockState must be instance of a TileState not " + tileEntity);

			final TileState tile = (TileState) tileEntity;
			return getNamedspaced(tile, key);
		}

		checkNBTAPI();

		try {
			return de.tr7zw.nbtinjector.NBTInjector.getNbtData(tileEntity).getString(key);
		} catch (final Throwable t) {
			Common.throwError(t, "Failed to get NBT tag for " + tileEntity.getType() + ". Tag: " + key);

			return null;
		}
	}

	private static final String getNamedspaced(TileState tile, String key) {
		return tile.getPersistentDataContainer().get(new NamespacedKey(SimplePlugin.getInstance(), key), PrimitivePersistentDataType.STRING);
	}

	// ----------------------------------------------------------------------------------------
	// Checking for metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * Return true if the given itemstack has the given key stored at its compound
	 * tag {@link FoConstants.NBT#TAG}
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static final boolean hasMetadata(ItemStack item, String key) {
		Valid.checkNotNull(item);

		final NBTItem nbt = new NBTItem(item);
		final NBTCompound tag = nbt.getCompound(FoConstants.NBT.TAG);

		return tag != null && tag.hasKey(key);
	}

	/**
	 * Returns if the entity has the given tag by key, first checks scoreboard tags,
	 * and then bukkit metadata
	 *
	 * @param entity
	 * @param key
	 * @return
	 */
	public static final boolean hasMetadata(Entity entity, String key) {
		Valid.checkNotNull(entity);

		try {
			for (final String line : entity.getScoreboardTags())
				if (hasTag(line, key))
					return true;

		} catch (NoSuchMethodError | NoSuchFieldError ex) {
			checkNBTAPI();

			try {
				return de.tr7zw.nbtinjector.NBTInjector.getNbtData(entity).hasKey(key);
			} catch (final Throwable t) {
				Common.error(t, "Failed to get NBT tag for " + entity.getType() + ". Tag: " + key);
			}
		}

		return false;
	}

	/**
	 * Return true if the given tile entity block such as {@link CreatureSpawner} has
	 * the given key
	 *
	 * @param tileEntity
	 * @param key
	 * @return
	 */
	public static final boolean hasMetadata(BlockState tileEntity, String key) {
		Valid.checkNotNull(tileEntity);
		Valid.checkNotNull(key);

		if (MinecraftVersion.atLeast(V.v1_14)) {
			Valid.checkBoolean(tileEntity instanceof TileState, "BlockState must be instance of a TileState not " + tileEntity);

			final TileState tile = (TileState) tileEntity;
			return hasNamedspaced(tile, key);
		}

		checkNBTAPI();

		try {
			return de.tr7zw.nbtinjector.NBTInjector.getNbtData(tileEntity).hasKey(key);
		} catch (final Throwable t) {
			Common.throwError(t, "Failed to get NBT tag for " + tileEntity.getType() + ". Tag: " + key);

			return false;
		}
	}

	private static final boolean hasNamedspaced(TileState tile, String key) {
		return tile.getPersistentDataContainer().has(new NamespacedKey(SimplePlugin.getInstance(), key), PrimitivePersistentDataType.STRING);
	}

	// Parses the tag and gets its value
	private static final boolean hasTag(String raw, String tag) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(tag);
	}

	private static final void checkNBTAPI() {
		System.out.println("**** DO NOT REPORT THE FOLLOWING ERROR, YOU NEED TO INSTALL AN ADDITIONAL LIBRARY FOR THIS PLUGIN TO WORK");
		Valid.checkBoolean(HookManager.isNbtAPILoaded(), "Storing persistent NBT tags on Entities/Blocks requries MC 1.14+ or NBTAPI plugin. Please download the 800kb+ plugin file from https://ci.codemc.org/job/Tr7zw/job/Item-NBT-API");

	}
}
