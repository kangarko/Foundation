package org.mineacademy.fo.remain;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType.PrimitivePersistentDataType;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBTTileEntity;

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

			// Lost upon reload
			entity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), tag));
		}
	}

	// Format the syntax of stored tags
	private static final String format(String key, String value) {
		return SimplePlugin.getNamed() + DELIMITER + key + DELIMITER + value;
	}

	/**
	 * Sets persistent tile entity metadata
	 *
	 * @param tileBlock
	 * @param key
	 * @param value
	 */
	public static final void setMetadata(Block tileBlock, String key, String value) {
		setMetadata(tileBlock.getState(), key, value);
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
			tileEntity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), value));

			final NBTTileEntity nbt = new NBTTileEntity(tileEntity);
			nbt.setString(key, value);
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
			if (entity.hasMetadata(key))
				return getTag(entity.getMetadata(key).get(0).asString(), key);
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
	 * @param tileBlock
	 * @param key
	 * @return
	 */
	public static final String getMetadata(Block tileBlock, String key) {
		return getMetadata(tileBlock.getState(), key);
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

		final String nbt = new NBTTileEntity(tileEntity).getString(key);
		final String result = (nbt == null || "".equals(nbt)) && tileEntity.hasMetadata(key) ? tileEntity.getMetadata(key).get(0).asString() : nbt;

		return result != null && result.isEmpty() ? null : result;
	}

	private static final String getNamedspaced(TileState tile, String key) {
		return tile.getPersistentDataContainer().get(new NamespacedKey(SimplePlugin.getInstance(), key), PrimitivePersistentDataType.STRING);
	}

	// ----------------------------------------------------------------------------------------
	// Checking for metadata
	// ----------------------------------------------------------------------------------------

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
			return entity.hasMetadata(key);
		}

		return false;
	}

	/**
	 * Return true if the given tile entity block such as {@link CreatureSpawner} has
	 * the given key
	 *
	 * @param tileBlock
	 * @param key
	 * @return
	 */
	public static final boolean hasMetadata(Block tileBlock, String key) {
		return hasMetadata(tileBlock.getState(), key);
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

		final boolean nbtHas = new NBTTileEntity(tileEntity).hasKey(key);

		return !nbtHas && tileEntity.hasMetadata(key) ? true : nbtHas;
	}

	private static final boolean hasNamedspaced(TileState tile, String key) {
		return tile.getPersistentDataContainer().has(new NamespacedKey(SimplePlugin.getInstance(), key), PrimitivePersistentDataType.STRING);
	}

	// Parses the tag and gets its value
	private static final boolean hasTag(String raw, String tag) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(tag);
	}
}
