package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.persistence.PersistentDataType.PrimitivePersistentDataType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBTCompound;
import org.mineacademy.fo.remain.nbt.NBTItem;
import org.mineacademy.fo.settings.YamlSectionConfig;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
		setMetadata(entity, tag, tag);
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

		if (Remain.hasScoreboardTags()) {
			if (!entity.getScoreboardTags().contains(tag))
				entity.addScoreboardTag(tag);

		} else {
			entity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), value));

			MetadataFile.getInstance().addMetadata(entity, key, value);
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

			setNamedspaced((TileState) tileEntity, key, value);
			tileEntity.update();

		} else {
			tileEntity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), value));
			tileEntity.update();

			MetadataFile.getInstance().addMetadata(tileEntity, key, value);
		}
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

		final String value = nbt.hasKey(compoundTag) ? nbt.getCompound(compoundTag).getString(key) : null;

		return Common.getOrNull(value);
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

		if (Remain.hasScoreboardTags())
			for (final String line : entity.getScoreboardTags()) {
				final String tag = getTag(line, key);

				if (tag != null && !tag.isEmpty())
					return tag;
			}

		final String value = entity.hasMetadata(key) ? entity.getMetadata(key).get(0).asString() : null;

		return Common.getOrNull(value);
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
	 * @param key, or null if none
	 * @return
	 */
	public static final String getMetadata(BlockState tileEntity, String key) {
		Valid.checkNotNull(tileEntity);
		Valid.checkNotNull(key);

		if (MinecraftVersion.atLeast(V.v1_14)) {
			Valid.checkBoolean(tileEntity instanceof TileState, "BlockState must be instance of a TileState not " + tileEntity);

			return getNamedspaced((TileState) tileEntity, key);
		}

		final String value = tileEntity.hasMetadata(key) ? tileEntity.getMetadata(key).get(0).asString() : null;

		return Common.getOrNull(value);
	}

	private static final String getNamedspaced(TileState tile, String key) {
		final String value = tile.getPersistentDataContainer().get(new NamespacedKey(SimplePlugin.getInstance(), key), PrimitivePersistentDataType.STRING);

		return Common.getOrNull(value);
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

		if (Remain.hasScoreboardTags())
			for (final String line : entity.getScoreboardTags())
				if (hasTag(line, key))
					return true;

		return entity.hasMetadata(key);
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

			return hasNamedspaced((TileState) tileEntity, key);
		}

		return tileEntity.hasMetadata(key);
	}

	private static final boolean hasNamedspaced(TileState tile, String key) {
		return tile.getPersistentDataContainer().has(new NamespacedKey(SimplePlugin.getInstance(), key), PrimitivePersistentDataType.STRING);
	}

	// Parses the tag and gets its value
	private static final boolean hasTag(String raw, String tag) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(tag);
	}

	/**
	 * Due to lack of persistent metadata implementation until Minecraft 1.14.x,
	 * we simply store them in a file during server restart and then apply
	 * as a temporary metadata for the Bukkit entities.
	 *
	 * internal use only
	 */
	//@Deprecated
	public static final class MetadataFile extends YamlSectionConfig {

		@Getter
		private static final MetadataFile instance = new MetadataFile();

		private final StrictMap<UUID, List<String>> entityMetadataMap = new StrictMap<>();
		private final StrictMap<Location, BlockCache> blockMetadataMap = new StrictMap<>();

		private MetadataFile() {
			super("Metadata");

			loadConfiguration(NO_DEFAULT, FoConstants.File.DATA);
		}

		@Override
		protected void onLoadFinish() {
			loadEntities();
			loadBlockStates();

			save();
		}

		private void loadEntities() {
			entityMetadataMap.clear();

			for (final String uuidName : getMap("Entity").keySet()) {
				final UUID uuid = UUID.fromString(uuidName);

				// Remove broken key
				if (!(getObject("Entity." + uuidName) instanceof List)) {
					setNoSave("Entity." + uuidName, null);

					continue;
				}

				final List<String> metadata = getStringList("Entity." + uuidName);
				final Entity entity = Remain.getEntity(uuid);

				// Check if the entity is still real
				if (!metadata.isEmpty() && entity != null && entity.isValid() && !entity.isDead()) {
					entityMetadataMap.put(uuid, metadata);

					applySavedMetadata(metadata, entity);
				}
			}

			setNoSave("Entity", entityMetadataMap);
		}

		private void loadBlockStates() {
			blockMetadataMap.clear();

			for (final String locationRaw : getMap("Block").keySet()) {
				final Location location = SerializeUtil.deserializeLocation(locationRaw);
				final BlockCache blockCache = get("Block." + locationRaw, BlockCache.class);

				final Block block = location.getBlock();

				// Check if the block remained the same
				if (!CompMaterial.isAir(block) && CompMaterial.fromBlock(block) == blockCache.getType()) {
					blockMetadataMap.put(location, blockCache);

					applySavedMetadata(blockCache.getMetadata(), block);
				}
			}

			setNoSave("Block", blockMetadataMap);
		}

		private void applySavedMetadata(List<String> metadata, Metadatable entity) {
			for (final String metadataLine : metadata) {
				if (metadataLine.isEmpty())
					continue;

				final String[] lines = metadataLine.split(DELIMITER);
				Valid.checkBoolean(lines.length == 3, "Malformed metadata line for " + entity + ". Length 3 != " + lines.length + ". Data: " + metadataLine);

				final String key = lines[1];
				final String value = lines[2];

				entity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), value));
			}
		}

		protected void addMetadata(Entity entity, @NonNull String key, String value) {
			final List<String> metadata = entityMetadataMap.getOrPut(entity.getUniqueId(), new ArrayList<>());

			for (final Iterator i = metadata.iterator(); i.hasNext();) {
				final String meta = (String) i.next();

				if (getTag(meta, key) != null)
					i.remove();
			}

			if (value != null && !value.isEmpty()) {
				final String formatted = format(key, value);

				metadata.add(formatted);
			}

			save("Entity", entityMetadataMap);
		}

		protected void addMetadata(BlockState blockState, String key, String value) {
			final BlockCache blockCache = blockMetadataMap.getOrPut(blockState.getLocation(), new BlockCache(CompMaterial.fromBlock(blockState.getBlock()), new ArrayList<>()));

			for (final Iterator i = blockCache.getMetadata().iterator(); i.hasNext();) {
				final String meta = (String) i.next();

				if (getTag(meta, key) != null)
					i.remove();
			}

			if (value != null && !value.isEmpty()) {
				final String formatted = format(key, value);

				blockCache.getMetadata().add(formatted);
			}

			{ // Save
				for (final Map.Entry<Location, BlockCache> entry : blockMetadataMap.entrySet())
					setNoSave("Block." + SerializeUtil.serializeLoc(entry.getKey()), entry.getValue().serialize());

				save();
			}
		}

		@Getter
		@RequiredArgsConstructor
		public static final class BlockCache implements ConfigSerializable {
			private final CompMaterial type;
			private final List<String> metadata;

			@Override
			public SerializedMap serialize() {
				final SerializedMap map = new SerializedMap();

				map.put("Type", type.toString());
				map.put("Metadata", metadata);

				return map;
			}

			public static BlockCache deserialize(SerializedMap map) {
				final CompMaterial type = map.getMaterial("Type");
				final List<String> metadata = map.getStringList("Metadata");

				return new BlockCache(type, metadata);
			}
		}
	}
}
