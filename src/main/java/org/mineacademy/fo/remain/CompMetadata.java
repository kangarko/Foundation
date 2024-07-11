package org.mineacademy.fo.remain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBT;
import org.mineacademy.fo.remain.nbt.ReadableNBT;
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
	 * Should we use file storage for metadata on Minecraft below 1.14?
	 */
	public static boolean ENABLE_LEGACY_FILE_STORAGE = true;

	/**
	 * Minecraft 1.14+ supports persistent metadata meaning entities/tiles can have custom tags easily
	 */
	private static boolean hasPersistentMetadata = MinecraftVersion.atLeast(V.v1_14);

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
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "Using CompMetadata for ItemStacks requires Minecraft 1.7.10 or newer");

		final boolean remove = value == null || "".equals(value);
		final ItemStack clone = new ItemStack(item);

		return NBT.modify(clone, tag -> {
			if (remove) {
				if (tag.hasTag(key))
					tag.removeKey(key);
			} else
				tag.setString(key, value);

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
		if (hasPersistentMetadata) {
			setPersistentMetadata(entity, key, value);

		} else
			MetadataFile.getInstance().setMetadata(entity, key, value);
	}

	/**
	 * Sets persistent tile entity metadata, set value to null to remove
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	public static void setMetadata(@NonNull final BlockState entity, @NonNull final String key, final String value) {
		if (hasPersistentMetadata) {
			setPersistentMetadata(entity, key, value);

			entity.update(true);

		} else
			MetadataFile.getInstance().setMetadata(entity, key, value);
	}

	// ----------------------------------------------------------------------------------------
	// Getting metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * Return true if the given itemstack has the given key
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static boolean hasMetadata(@NonNull final ItemStack item, @NonNull final String key) {
		final String metadata = getMetadata(item, key);

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
		final String metadata = getMetadata(entity, key);

		return metadata != null && !metadata.isEmpty();
	}

	/**
	 * Return true if the given tile entity block such as {@link CreatureSpawner} has
	 * the given key
	 *
	 * @param entity
	 * @param key
	 * @return
	 */
	public static boolean hasMetadata(@NonNull final BlockState entity, @NonNull final String key) {
		final String metadata = getMetadata(entity, key);

		return metadata != null && !metadata.isEmpty();
	}

	/**
	 * A shortcut from reading a certain key from an item's given compound tag
	 *
	 * @param item
	 * @param key
	 * @return
	 */
	public static String getMetadata(@NonNull final ItemStack item, @NonNull final String key) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "Using CompMetadata for ItemStacks requires Minecraft 1.7.10 or newer");

		return CompMaterial.isAir(item.getType()) ? null : NBT.get(item, nbt -> {
			String value = Common.getOrNull(nbt.getString(key));

			if (value == null) {
				final ReadableNBT compound = nbt.getCompound(SimplePlugin.getNamed() + "_NbtTag");

				if (compound != null && compound.hasTag(key))
					value = Common.getOrNull(compound.getString(key));
			}

			return value;
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

		// PENDING REMOVAL
		if (Remain.hasScoreboardTags())
			for (final String line : entity.getScoreboardTags()) {
				final String value = getTag(line, key);

				if (value != null && !value.isEmpty()) {
					setMetadata(entity, key, value); // Set the metadata the new way

					return value;
				}
			}

		if (hasPersistentMetadata) {
			return getPersistentMetadata(entity, key);

		} else
			return MetadataFile.getInstance().getMetadata(entity, key);
	}

	/**
	 * Return saved tile entity metadata, or null if none
	 *
	 * @param entity
	 * @param key       or null if none
	 * @return
	 */
	public static String getMetadata(@NonNull final BlockState entity, @NonNull final String key) {
		if (hasPersistentMetadata) {
			return getPersistentMetadata(entity, key);

		} else
			return MetadataFile.getInstance().getMetadata(entity, key);
	}

	// ----------------------------------------------------------------------------------------
	// Temporary metadata
	// ----------------------------------------------------------------------------------------

	/**
	 * Sets a temporary metadata to entity. This metadata is NOT persistent
	 * and is removed on server stop, restart or reload.
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
		player.removeMetadata(key, SimplePlugin.getInstance());
	}

	// ----------------------------------------------------------------------------------------
	// Utility methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Return if we are using file storage for metadata (MC older than 1.14).
	 *
	 * @return
	 */
	public static boolean isLegacy() {
		return !hasPersistentMetadata;
	}

	/*
	 * Helper method to get a tag from a raw string
	 */
	private static String getTag(final String raw, final String key) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(key) ? parts[2] : null;
	}

	/*
	 * Helper method to format a tag
	 */
	private static String formatTag(final String key, final String value) {
		return SimplePlugin.getNamed() + DELIMITER + key + DELIMITER + value;
	}

	/*
	 * Returns persistent metadata with our plugin assigned as namedspaced key for MC 1.14+
	 */
	private static String getPersistentMetadata(final Object entity, final String key) {
		Valid.checkBoolean(entity instanceof PersistentDataHolder, "Can only use CompMetadata#setMetadata(" + key + ") for persistent data holders, got " + entity.getClass());
		final PersistentDataContainer data = ((PersistentDataHolder) entity).getPersistentDataContainer(); // Prevents no class def error on legacy MC

		return Common.getOrNull(data.get(new NamespacedKey(SimplePlugin.getInstance(), key), PersistentDataType.STRING));
	}

	/*
	 * Sets persistent metadata with our plugin assigned as namedspaced key for MC 1.14+
	 */
	private static void setPersistentMetadata(final Object entity, final String key, final String value) {
		Valid.checkBoolean(entity instanceof PersistentDataHolder, "Can only use CompMetadata#setMetadata(" + key + ") for persistent data holders, got " + entity.getClass());

		final PersistentDataContainer data = ((PersistentDataHolder) entity).getPersistentDataContainer(); // Prevents no class def error on legacy MC
		final boolean remove = value == null || "".equals(value);

		if (remove)
			data.remove(new NamespacedKey(SimplePlugin.getInstance(), key));
		else
			data.set(new NamespacedKey(SimplePlugin.getInstance(), key), PersistentDataType.STRING, value);
	}

	/**
	 * Due to lack of persistent metadata implementation until Minecraft 1.14.x,
	 * we store them manually.
	 */
	public static final class MetadataFile extends YamlConfig implements Listener {

		@Getter
		private static final MetadataFile instance = new MetadataFile();

		/**
		 * Stores entity metadata by UUID
		 */
		private final Map<UUID, Set<String>> entityMetadata = new HashMap<>();

		/**
		 * Stores tile entity metadata by Location
		 */
		private final Map<Location, BlockCache> blockMetadata = new HashMap<>();

		private boolean loaded = false;

		private MetadataFile() {
			if (!hasPersistentMetadata && ENABLE_LEGACY_FILE_STORAGE) {
				this.setPathPrefix("Metadata");
				this.setSaveEmptyValues(false);

				this.setHeader(
						"-------------------------------------------------------------------------------------------------",
						"This file is used to store metadata for entities and blocks in Minecraft versions below 1.14.",
						"If you delete this file or upgrade to Minecraft 1.14+, all metadata will be lost.",
						"",
						"THIS FILE IS MACHINE GENERATED. DO NOT EDIT",
						"-------------------------------------------------------------------------------------------------");
			}
		}

		private void loadIfHasnt() {
			if (!this.loaded) {
				this.loadConfiguration(NO_DEFAULT, "legacy-metadata.yml");

				this.loaded = true;
			}
		}

		@Override
		protected void onLoad() {
			this.loadEntities();
			this.loadBlockStates();
		}

		@Override
		protected boolean skipSaveIfNoFile() {
			return true;
		}

		@Override
		protected boolean canSaveFile() {
			return !hasPersistentMetadata && ENABLE_LEGACY_FILE_STORAGE;
		}

		@Override
		public SerializedMap saveToMap() {
			return SerializedMap.ofArray(
					"Entity", this.entityMetadata,
					"Block", this.blockMetadata);
		}

		@EventHandler
		public void onEntityDeath(final EntityDeathEvent event) {
			final Entity entity = event.getEntity();
			final UUID uniqueId = entity.getUniqueId();

			this.entityMetadata.remove(uniqueId);

			//this.save(); -> handled in onPluginStop()
		}

		private void loadEntities() {
			this.entityMetadata.clear();

			for (final String uuidString : this.getMap("Entity").keySet()) {
				final UUID uuid = UUID.fromString(uuidString);

				// Remove broken keys
				if (!(this.getObject("Entity." + uuidString) instanceof List)) {
					this.set("Entity." + uuidString, null);

					continue;
				}

				final Set<String> metadata = this.getSet("Entity." + uuidString, String.class);

				if (!metadata.isEmpty())
					this.entityMetadata.put(uuid, metadata);
			}
		}

		private void loadBlockStates() {
			this.blockMetadata.clear();

			for (final String locationString : this.getMap("Block").keySet()) {
				final Location location = SerializeUtil.deserializeLocation(locationString);

				final Block block = location.getBlock();
				final BlockCache blockCache = this.get("Block." + locationString, BlockCache.class);

				// Check if the block remained the same
				if (block != null && CompMaterial.fromBlock(block) == blockCache.getType())
					this.blockMetadata.put(location, blockCache);
			}
		}

		protected String getMetadata(final Entity entity, @NonNull final String key) {
			this.loadIfHasnt();

			final UUID uniqueId = entity.getUniqueId();
			final Set<String> metadata = this.entityMetadata.getOrDefault(uniqueId, new HashSet<>());

			for (final Iterator<String> iterator = metadata.iterator(); iterator.hasNext();) {
				final String meta = iterator.next();
				final String value = getTag(meta, key);

				if (value != null && !value.isEmpty())
					return value;
			}

			return null;
		}

		protected void setMetadata(final Entity entity, @NonNull final String key, final String value) {
			this.loadIfHasnt();

			final UUID uniqueId = entity.getUniqueId();
			final Set<String> metadata = this.entityMetadata.getOrDefault(uniqueId, new HashSet<>());
			final boolean remove = value == null || "".equals(value);

			// Remove the old value
			for (final Iterator<String> iterator = metadata.iterator(); iterator.hasNext();) {
				final String meta = iterator.next();

				if (getTag(meta, key) != null)
					iterator.remove();
			}

			if (!remove) {
				final String formatted = formatTag(key, value);

				metadata.add(formatted);
				this.entityMetadata.put(uniqueId, metadata);
			}

			if (metadata.isEmpty())
				this.entityMetadata.remove(uniqueId);

			//this.save("Entity", this.entityMetadata); -> handled in onPluginStop()
		}

		protected String getMetadata(final BlockState entity, @NonNull final String key) {
			this.loadIfHasnt();

			final Location location = entity.getLocation();
			final BlockCache blockCache = this.blockMetadata.get(location);

			if (blockCache == null)
				return null;

			final boolean hasSinceChanged = CompMaterial.fromBlock(location.getBlock()) != blockCache.getType();

			for (final Iterator<String> iterator = blockCache.getMetadata().iterator(); iterator.hasNext();) {
				final String meta = iterator.next();
				final String value = getTag(meta, key);

				if (value != null && !value.isEmpty()) {
					if (hasSinceChanged)
						iterator.remove();
					else
						return value;
				}
			}

			return null;
		}

		protected void setMetadata(final BlockState entity, final String key, final String value) {
			this.loadIfHasnt();

			final Location location = entity.getLocation();
			BlockCache blockCache = this.blockMetadata.get(location);
			final boolean remove = value == null || "".equals(value);

			// Remove the old value
			if (blockCache != null)
				for (final Iterator<String> iterator = blockCache.getMetadata().iterator(); iterator.hasNext();) {
					final String meta = iterator.next();

					if (getTag(meta, key) != null)
						iterator.remove();
				}

			if (!remove) {
				final String formatted = formatTag(key, value);

				if (blockCache == null)
					blockCache = BlockCache.create(CompMaterial.fromBlock(entity.getBlock()));

				blockCache.getMetadata().add(formatted);
				this.blockMetadata.put(location, blockCache);
			}

			if (blockCache != null && blockCache.getMetadata().isEmpty())
				this.blockMetadata.remove(location);

			//this.save("Block", this.blockMetadata); -> handled in onPluginStop()
		}

		@Getter
		@RequiredArgsConstructor
		public static final class BlockCache implements ConfigSerializable {

			private final CompMaterial type;
			private final Set<String> metadata;

			public static BlockCache deserialize(final SerializedMap map) {
				final CompMaterial type = map.getMaterial("Type");
				final Set<String> metadata = map.getSet("Metadata", String.class);

				return new BlockCache(type, metadata);
			}

			@Override
			public SerializedMap serialize() {
				final SerializedMap map = new SerializedMap();

				map.put("Type", this.type.toString());
				map.put("Metadata", this.metadata);

				return map;
			}

			public static BlockCache create(final CompMaterial type) {
				return new BlockCache(type, new HashSet<>());
			}
		}
	}
}