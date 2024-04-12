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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBT;
import org.mineacademy.fo.remain.nbt.NBTEntity;
import org.mineacademy.fo.remain.nbt.NBTTileEntity;
import org.mineacademy.fo.remain.nbt.ReadWriteNBT;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Utility class for persistent metadata manipulation
 * <p>
 * We apply scoreboard tags to ensure permanent metadata storage
 * if supported, otherwise it is lost on reload
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompMetadata {

	/**
	 * Legacy <1.14 uses a NBT-API and hard file storage in data.db for metadata
	 */
	@Getter
	private static boolean legacy = MinecraftVersion.olderThan(V.v1_14);

	/**
	 * Minecraft 1.7+ is supported by NBT-API
	 */
	private static boolean hasNBTAPI = MinecraftVersion.atLeast(V.v1_7);

	/**
	 * The tag delimiter
	 */
	private final static String DELIMITER = "%-%";

	// ----------------------------------------------------------------------------------------
	// Setting metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * A shortcut for setting a tag with key-value pair on an item. Set value to null to remove
	 * <p>&nbsp;</p>
	 * <p>
	 * NOTE: The current behavior, where it clones the item, may change in the future. This change is aimed
	 * at improving performance and consistency in behavior, as it is not clear this method clones the
	 * item. You will still have the option to clone the item itself; the return value will not be
	 * removed, only adjusted to return the item you provide.
	 * </p>
	 * @param item
	 * @param key
	 * @param value
	 * @return
	 */
	public static ItemStack setMetadata(@NonNull final ItemStack item, @NonNull final String key, final String value) {
		Valid.checkBoolean(hasNBTAPI, "CompMetadata#setMetadata() requires Minecraft 1.7.10 or newer");

		boolean remove = value == null || "".equals(value);
		final ItemStack clone = new ItemStack(item);

		return NBT.modify(clone, tag -> {
			final ReadWriteNBT compound = tag.getOrCreateCompound(FoConstants.NBT.TAG);

			if (remove) {
				if (compound.hasTag(key))
					compound.removeKey(key);
			} else
				compound.setString(key, value);

			return clone;
		});
	}

	/**
	 * Attempts to set a persistent metadata tag with value for entity
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	public static void setMetadata(@NonNull final Entity entity, @NonNull final String key, final String value) {
		boolean remove = value == null || "".equals(value);

		if (Remain.hasScoreboardTags()) {
			final String tag = formatTag(key, value);

			if (remove) {
				if (entity.getScoreboardTags().contains(tag))
					entity.removeScoreboardTag(tag);

			} else if (!entity.getScoreboardTags().contains(tag))
				entity.addScoreboardTag(tag);

		} else if (hasNBTAPI) {
			final NBTEntity nbt = new NBTEntity(entity);
			final ReadWriteNBT compound = nbt.getOrCreateCompound(FoConstants.NBT.TAG);

			if (remove) {
				if (compound.hasTag(key))
					compound.removeKey(key);
			} else
				compound.setString(key, value);

		} else {
			if (remove) {
				entity.removeMetadata(key, SimplePlugin.getInstance());

				MetadataFile.getInstance().removeMetadata(entity, key);

			} else {
				entity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), value));

				MetadataFile.getInstance().addMetadata(entity, key, value);
			}
		}
	}

	/**
	 * Sets persistent tile entity metadata, set value to null to remove
	 *
	 * @param tileEntity
	 * @param key
	 * @param value
	 */
	public static void setMetadata(@NonNull final BlockState tileEntity, @NonNull final String key, final String value) {
		boolean remove = value == null || "".equals(value);

		if (!legacy) {
			Valid.checkBoolean(tileEntity instanceof TileState, "BlockState must be instance of a TileState not " + tileEntity);

			if (remove)
				removeNamedspaced((TileState) tileEntity, key);
			else
				setNamedspaced((TileState) tileEntity, key, value);

			tileEntity.update();

		} else if (hasNBTAPI) {
			final NBTTileEntity nbt = new NBTTileEntity(tileEntity);
			final ReadWriteNBT compound = nbt.getOrCreateCompound(FoConstants.NBT.TAG);

			if (remove) {
				if (compound.hasTag(key))
					compound.removeKey(key);
			} else
				compound.setString(key, value);

		} else {
			if (remove) {
				tileEntity.removeMetadata(key, SimplePlugin.getInstance());

				MetadataFile.getInstance().removeMetadata(tileEntity, key);

			} else {
				tileEntity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), value));

				MetadataFile.getInstance().addMetadata(tileEntity, key, value);
			}

			tileEntity.update();
		}
	}

	// ----------------------------------------------------------------------------------------
	// Getting metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * Return true if the given itemstack has the given key stored at its compound
	 * tag {@link org.mineacademy.fo.constants.FoConstants.NBT#TAG}
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static boolean hasMetadata(@NonNull final ItemStack item, @NonNull final String key) {
		String metadata = getMetadata(item, key);

		return metadata != null && !metadata.isEmpty();
	}

	/**
	 * Returns if the entity has the given tag by key, first checks scoreboard tags,
	 * and then bukkit metadata
	 *
	 * @param entity
	 * @param key
	 * @return
	 */
	public static boolean hasMetadata(@NonNull final Entity entity, @NonNull final String key) {
		String metadata = getMetadata(entity, key);

		return metadata != null && !metadata.isEmpty();
	}

	/**
	 * Return true if the given tile entity block such as {@link CreatureSpawner} has
	 * the given key
	 *
	 * @param tileEntity
	 * @param key
	 * @return
	 */
	public static boolean hasMetadata(@NonNull final BlockState tileEntity, @NonNull final String key) {
		String metadata = getMetadata(tileEntity, key);

		return metadata != null && !metadata.isEmpty();
	}

	/**
	 * A shortcut from reading a certain key from an item's given compound tag
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static String getMetadata(final ItemStack item, @NonNull final String key) {
		Valid.checkBoolean(hasNBTAPI, "CompMetadata#getMetadata() requires Minecraft 1.7.10 or newer");

		if (item == null || CompMaterial.isAir(item.getType()))
			return null;

		return NBT.get(item, nbt -> {
			final String value = nbt.hasTag(FoConstants.NBT.TAG) ? nbt.getCompound(FoConstants.NBT.TAG).getString(key) : null;

			return Common.getOrNull(value);
		});
	}

	/**
	 * Attempts to get the entity's metadata, first from scoreboard tag,
	 * second from Bukkit metadata
	 *
	 * @param entity
	 * @param key
	 * @return the tag, or null
	 */
	public static String getMetadata(@NonNull final Entity entity, @NonNull final String key) {
		if (Remain.hasScoreboardTags())
			for (final String line : entity.getScoreboardTags()) {
				final String tag = getTag(line, key);

				if (tag != null && !tag.isEmpty())
					return tag;
			}

		else if (hasNBTAPI) {
			final NBTEntity nbt = new NBTEntity(entity);
			final ReadWriteNBT compound = nbt.getCompound(FoConstants.NBT.TAG);

			if (compound != null && compound.hasTag(key))
				return compound.getString(key);
		}

		return Common.getOrNull(entity.hasMetadata(key) ? entity.getMetadata(key).get(0).asString() : null);
	}

	/**
	 * Return saved tile entity metadata, or null if none
	 *
	 * @param tileEntity
	 * @param key       or null if none
	 * @return
	 */
	public static String getMetadata(@NonNull final BlockState tileEntity, @NonNull final String key) {
		if (MinecraftVersion.atLeast(V.v1_14)) {
			Valid.checkBoolean(tileEntity instanceof TileState, "BlockState must be instance of a TileState not " + tileEntity);

			return getNamedspaced((TileState) tileEntity, key);

		} else if (hasNBTAPI) {
			final NBTTileEntity nbt = new NBTTileEntity(tileEntity);
			final ReadWriteNBT compound = nbt.getCompound(FoConstants.NBT.TAG);

			if (compound != null && compound.hasTag(key))
				return compound.getString(key);
		}

		return Common.getOrNull(tileEntity.hasMetadata(key) ? tileEntity.getMetadata(key).get(0).asString() : null);
	}

	// ----------------------------------------------------------------------------------------
	// Temporary metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * Sets a temporary metadata to entity. This metadata is NOT persistent
	 * and is removed on server stop, restart or reload.
	 * <p>
	 * Use {@link #setMetadata(Entity, String)} to set persistent custom tags for entities.
	 *
	 * @param entity
	 * @param tag
	 */
	public static void setTempMetadata(final Entity entity, final String tag) {
		entity.setMetadata(tag, new FixedMetadataValue(SimplePlugin.getInstance(), tag));
	}

	/**
	 * Sets a temporary metadata to entity. This metadata is NOT persistent
	 * and is removed on server stop, restart or reload.
	 * <p>
	 * Use {@link #setMetadata(Entity, String)} to set persistent custom tags for entities.
	 *
	 * @param entity
	 * @param tag
	 * @param key
	 */
	public static void setTempMetadata(final Entity entity, final String tag, final Object key) {
		entity.setMetadata(tag, new FixedMetadataValue(SimplePlugin.getInstance(), key));
	}

	/**
	 * Return entity metadata value or null if has none
	 * <p>
	 * Only usable if you set it using the {@link #setTempMetadata(Entity, String, Object)} with the key parameter
	 * because otherwise the tag is the same as the value we return
	 *
	 * @param entity
	 * @param key
	 * @return
	 */
	public static MetadataValue getTempMetadata(final Entity entity, final String key) {
		return entity.hasMetadata(key) ? entity.getMetadata(key).get(0) : null;
	}

	/**
	 * Return true if player has the given temporary metadata
	 *
	 * @param player
	 * @param tag
	 * @return
	 */
	public static boolean hasTempMetadata(final Entity player, final String tag) {
		return player.hasMetadata(tag);
	}

	/**
	 * Remove temporary metadata from the entity
	 *
	 * @param player
	 * @param key
	 */
	public static void removeTempMetadata(final Entity player, final String key) {
		if (player.hasMetadata(key))
			player.removeMetadata(key, SimplePlugin.getInstance());
	}

	// ----------------------------------------------------------------------------------------
	// Utility methods
	// ----------------------------------------------------------------------------------------

	private static String getTag(final String raw, final String key) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(key) ? parts[2] : null;
	}

	private static String formatTag(final String key, final String value) {
		return SimplePlugin.getNamed() + DELIMITER + key + DELIMITER + value;
	}

	private static String getNamedspaced(final PersistentDataHolder dataHolder, final String key) {
		return Common.getOrNull(dataHolder.getPersistentDataContainer().get(new NamespacedKey(SimplePlugin.getInstance(), key), PersistentDataType.STRING));
	}

	private static void setNamedspaced(final PersistentDataHolder dataHolder, final String key, final String value) {
		dataHolder.getPersistentDataContainer().set(new NamespacedKey(SimplePlugin.getInstance(), key), PersistentDataType.STRING, value);
	}

	private static void removeNamedspaced(final TileState tile, final String key) {
		tile.getPersistentDataContainer().remove(new NamespacedKey(SimplePlugin.getInstance(), key));
	}

	/**
	 * Due to lack of persistent metadata implementation until Minecraft 1.14.x,
	 * we simply store them in a file during server restart and then apply
	 * as a temporary metadata for the Bukkit entities.
	 * <p>
	 * internal use only
	 */
	@AutoRegister
	public static final class MetadataFile extends YamlConfig {

		@Getter
		private static final MetadataFile instance = new MetadataFile();

		private static boolean canSave = false;

		private final StrictMap<UUID, List<String>> entityMetadataMap = new StrictMap<>();
		private final StrictMap<Location, BlockCache> blockMetadataMap = new StrictMap<>();

		private MetadataFile() {
			if (CompMetadata.legacy) {
				this.setPathPrefix("Metadata");
				this.setSaveEmptyValues(false);

				this.loadConfiguration(NO_DEFAULT, FoConstants.File.DATA);
			}
		}

		@Override
		protected void onLoad() {
			if (CompMetadata.legacy) {
				this.loadEntities();

				this.loadBlockStates();
			}
		}

		@Override
		protected boolean canSaveFile() {
			return canSave;
		}

		@Override
		protected boolean skipSaveIfNoFile() {
			return true;
		}

		@Override
		protected void onSave() {
			if (CompMetadata.legacy) {
				this.set("Entity", this.entityMetadataMap);
				this.set("Block", this.blockMetadataMap);
			}
		}

		private void loadEntities() {
			this.entityMetadataMap.clear();

			for (final String uuidName : this.getMap("Entity").keySet()) {
				final UUID uuid = UUID.fromString(uuidName);

				// Remove broken key
				if (!(this.getObject("Entity." + uuidName) instanceof List)) {
					this.set("Entity." + uuidName, null);

					continue;
				}

				final List<String> metadata = this.getStringList("Entity." + uuidName);
				final Entity entity = Remain.getEntity(uuid);

				// Check if the entity is still real
				if (!metadata.isEmpty() && entity != null && entity.isValid() && !entity.isDead()) {
					this.entityMetadataMap.put(uuid, metadata);

					this.applySavedMetadata(metadata, entity);
				}
			}

			this.set("Entity", this.entityMetadataMap);
		}

		private void loadBlockStates() {
			this.blockMetadataMap.clear();

			for (final String locationRaw : this.getMap("Block").keySet()) {
				final Location location = SerializeUtil.deserializeLocation(locationRaw);
				final BlockCache blockCache = this.get("Block." + locationRaw, BlockCache.class);

				final Block block = location.getBlock();

				// Check if the block remained the same
				if (!CompMaterial.isAir(block) && CompMaterial.fromBlock(block) == blockCache.getType()) {
					this.blockMetadataMap.put(location, blockCache);

					this.applySavedMetadata(blockCache.getMetadata(), block);
				}
			}

			this.set("Block", this.blockMetadataMap);

		}

		private void applySavedMetadata(final List<String> metadata, final Metadatable entity) {
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

		protected void addMetadata(final Entity entity, @NonNull final String key, final String value) {
			final List<String> metadata = this.entityMetadataMap.getOrPut(entity.getUniqueId(), new ArrayList<>());

			for (final Iterator<String> i = metadata.iterator(); i.hasNext();) {
				final String meta = i.next();

				if (getTag(meta, key) != null)
					i.remove();
			}

			if (value != null && !value.isEmpty()) {
				final String formatted = formatTag(key, value);

				metadata.add(formatted);
			}

			this.save("Entity", this.entityMetadataMap);
		}

		protected void addMetadata(final BlockState blockState, final String key, final String value) {
			final BlockCache blockCache = this.blockMetadataMap.getOrPut(blockState.getLocation(), new BlockCache(CompMaterial.fromBlock(blockState.getBlock()), new ArrayList<>()));

			for (final Iterator<String> i = blockCache.getMetadata().iterator(); i.hasNext();) {
				final String meta = i.next();

				if (getTag(meta, key) != null)
					i.remove();
			}

			if (value != null && !value.isEmpty()) {
				final String formatted = formatTag(key, value);

				blockCache.getMetadata().add(formatted);
			}

			{ // Save
				for (final Map.Entry<Location, BlockCache> entry : this.blockMetadataMap.entrySet())
					this.set("Block." + SerializeUtil.serializeLoc(entry.getKey()), entry.getValue().serialize());

				this.save();
			}
		}

		protected void removeMetadata(final Entity entity, final String key) {
			final List<String> metadata = this.entityMetadataMap.getOrPut(entity.getUniqueId(), new ArrayList<>());

			for (final Iterator<String> i = metadata.iterator(); i.hasNext();) {
				final String meta = i.next();

				if (getTag(meta, key) != null)
					i.remove();
			}

			this.save("Entity", this.entityMetadataMap);
		}

		protected void removeMetadata(final BlockState blockState, final String key) {
			if (!blockMetadataMap.containsKey(blockState.getLocation()))
				return;

			final BlockCache blockCache = blockMetadataMap.get(blockState.getLocation());

			for (final Iterator<String> i = blockCache.getMetadata().iterator(); i.hasNext();) {
				final String meta = i.next();

				if (getTag(meta, key) != null) {
					i.remove();
					break;
				}
			}

			if (blockCache.getMetadata().isEmpty()) {
				blockMetadataMap.remove(blockState.getLocation());
			}

			{ // Save
				for (final Map.Entry<Location, BlockCache> entry : this.blockMetadataMap.entrySet())
					this.set("Block." + SerializeUtil.serializeLoc(entry.getKey()), entry.getValue().serialize());

				this.save();
			}
		}

		public static void saveOnce() {
			if (CompMetadata.legacy) {
				try {
					canSave = true;
					instance.save();

				} finally {
					canSave = false;
				}
			}
		}

		@Getter
		@RequiredArgsConstructor
		public static final class BlockCache implements ConfigSerializable {
			private final CompMaterial type;
			private final List<String> metadata;

			public static BlockCache deserialize(final SerializedMap map) {
				final CompMaterial type = map.getMaterial("Type");
				final List<String> metadata = map.getStringList("Metadata");

				return new BlockCache(type, metadata);
			}

			@Override
			public SerializedMap serialize() {
				final SerializedMap map = new SerializedMap();

				map.put("Type", this.type.toString());
				map.put("Metadata", this.metadata);

				return map;
			}
		}
	}
}