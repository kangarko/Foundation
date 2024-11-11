package org.mineacademy.fo.remain;

import java.io.File;
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
import org.bukkit.entity.Player;
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
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ChatPaginator;
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
	 * Should we use file storage for metadata on legacy Minecraft versions?
	 */
	public static boolean ENABLE_LEGACY_FILE_STORAGE = true;

	/**
	 * An internal metadata tag the player gets when he opens a sign on legacy Minecraft version.
	 *
	 * We use this in the sign update packet listener to handle sign updating.
	 */
	public static final String TAG_OPENED_SIGN = SimplePlugin.getNamed() + "_OpenedSign";

	/**
	 * An internal metadata tag the player gets when we use {@link ChatPaginator}
	 */
	public static final String TAG_PAGINATION = SimplePlugin.getNamed() + "_Pages";

	/**
	 * The tag delimiter
	 */
	private static final String DELIMITER = "%-%";

	/**
	 * Minecraft 1.14+ supports persistent metadata meaning entities/tiles can have custom tags easily
	 */
	private static final boolean hasPersistentMetadata = MinecraftVersion.atLeast(V.v1_14);

	/**
	 * Caches {@link NamespacedKey} for performance purposes
	 */
	private static Map<String, Object> namespacedCache = new HashMap<>();

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
		if (hasPersistentMetadata)
			setPersistentMetadata(entity, key, value);

		else
			setFileMetadata(entity.getUniqueId(), key, value);
	}

	/**
	 * Attempts to set a persistent metadata tag with value for entity using file storage (metadata.yml)
	 *
	 * @param entityUid
	 * @param key
	 * @param value
	 */
	public static void setFileMetadata(@NonNull final UUID entityUid, @NonNull final String key, final String value) {
		MetadataFile.getInstance().setMetadata(entityUid, key, value);
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
		return CompMaterial.isAir(item.getType()) ? null : NBT.get(item, nbt -> {
			String value = Common.getOrNull(nbt.getString(key));

			if (value == null) {
				final ReadableNBT compound = nbt.getCompound(SimplePlugin.getInstance().getName() + "_NbtTag");

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
			return getFileMetadata(entity.getUniqueId(), key);
	}

	/**
	 * Attempts to get the entity's metadata from metadata.yml file
	 *
	 * @param entityUid
	 * @param key
	 * @return the tag, or null
	 */
	public static String getFileMetadata(@NonNull final UUID entityUid, @NonNull final String key) {
		return MetadataFile.getInstance().getMetadata(entityUid, key);
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

		return parts.length == 3 && parts[0].equals(SimplePlugin.getInstance().getName()) && parts[1].equals(key) ? parts[2] : null;
	}

	/*
	 * Helper method to format a tag
	 */
	private static String formatTag(final String key, final String value) {
		return SimplePlugin.getInstance().getName() + DELIMITER + key + DELIMITER + value;
	}

	/*
	 * Returns persistent metadata with our plugin assigned as namedspaced key for MC 1.14+
	 */
	private static String getPersistentMetadata(final Object entity, final String key) {
		Valid.checkBoolean(entity instanceof PersistentDataHolder, "Can only use CompMetadata#setMetadata(" + key + ") for persistent data holders, got " + entity.getClass());
		final PersistentDataContainer data = ((PersistentDataHolder) entity).getPersistentDataContainer(); // Prevents no class def error on legacy MC
		final NamespacedKey namespacedKey = (NamespacedKey) getOrCacheKey(key);

		return Common.getOrNull(data.get(namespacedKey, PersistentDataType.STRING));
	}

	/*
	 * Sets persistent metadata with our plugin assigned as namedspaced key for MC 1.14+
	 */
	private static void setPersistentMetadata(final Object entity, final String key, final String value) {
		Valid.checkBoolean(entity instanceof PersistentDataHolder, "Can only use CompMetadata#setMetadata(" + key + ") for persistent data holders, got " + entity.getClass());

		final PersistentDataContainer data = ((PersistentDataHolder) entity).getPersistentDataContainer(); // Prevents no class def error on legacy MC
		final boolean remove = value == null || "".equals(value);
		final NamespacedKey namespacedKey = (NamespacedKey) getOrCacheKey(key);

		if (remove)
			data.remove(namespacedKey);
		else
			data.set(namespacedKey, PersistentDataType.STRING, value);
	}

	private static Object getOrCacheKey(String key) {
		return namespacedCache.computeIfAbsent(key, k -> new NamespacedKey(SimplePlugin.getInstance(), key));
	}

	/**
	 * Due to lack of persistent metadata implementation until Minecraft 1.14.x,
	 * we store them manually.
	 */
	@Getter
	public static final class MetadataFile extends YamlConfig implements Listener {

		@Getter
		private static final MetadataFile instance = new MetadataFile();

		/**
		 * Stores entity metadata by UUID
		 */
		private final Map<UUID, Map<String, String>> entityMetadata = new HashMap<>();

		/**
		 * Stores tile entity metadata by Location
		 */
		private final Map<Location, BlockCache> blockMetadata = new HashMap<>();

		private boolean loaded = false;

		private MetadataFile() {
			this.migrateOldFile();
			this.setPathPrefix("Metadata");
			this.setHeader(
					Common.configLine(),
					" DO NOT EDIT - THIS FILE IS MACHINE-GENERATED",
					" ",
					" Stores plugin-related metadata for entities and blocks.",
					Common.configLine());
		}

		private void migrateOldFile() {
			final File legacyFile = FileUtil.getFile("legacy-metadata.yml");

			if (legacyFile.exists()) {
				final File newFile = FileUtil.getFile("metadata.yml");

				if (newFile.exists())
					legacyFile.delete();
				else
					legacyFile.renameTo(newFile);
			}
		}

		private void loadIfHasnt() {
			if (!this.loaded) {

				// Avoid file creation unless actually used in the set method below
				final File file = FileUtil.getFile("metadata.yml");

				if (file.exists())
					this.load(file);

				else
					this.setFile(file);

				this.loaded = true;
			}
		}

		@Override
		protected void onLoad() {
			this.loadEntities();
			this.loadBlockStates();
		}

		@Override
		protected boolean canSaveFile() {
			return this.getBoolean("Initialized", false);
		}

		@Override
		protected void onSave() {
			this.set("Entity", this.entityMetadata.isEmpty() ? null : this.entityMetadata);
			this.set("Block", this.blockMetadata.isEmpty() ? null : this.blockMetadata);

			if (this.entityMetadata.isEmpty() && this.blockMetadata.isEmpty())
				this.set("Initialized", null);
		}

		@EventHandler
		public void onEntityDeath(final EntityDeathEvent event) {
			final Entity entity = event.getEntity();

			if (!(entity instanceof Player)) {
				final UUID uniqueId = entity.getUniqueId();

				this.entityMetadata.remove(uniqueId);

				//this.save(); -> handled in onPluginStop()
			}
		}

		private void loadEntities() {
			this.entityMetadata.clear();

			for (final String uuidString : this.getMap("Entity").keySet()) {
				final UUID uuid = UUID.fromString(uuidString);
				final Object raw = this.getObject("Entity." + uuidString);

				if (raw instanceof List) {
					final List<String> metadata = (List<String>) raw;

					if (!metadata.isEmpty()) {
						final Map<String, String> converted = new HashMap<>();

						for (final String meta : metadata) {
							final String[] parts = meta.split(DELIMITER);

							if (parts.length == 3 && parts[0].equals(SimplePlugin.getInstance().getName()))
								converted.put(parts[1], parts[2]);
						}

						this.entityMetadata.put(uuid, converted);
					}
				}

				else {
					final SerializedMap data = SerializedMap.of(raw);

					if (!data.isEmpty()) {
						final Map<String, String> converted = new HashMap<>();

						for (final Map.Entry<String, Object> entry : data.entrySet())
							converted.put(entry.getKey(), entry.getValue().toString());

						this.entityMetadata.put(uuid, converted);
					}
				}
			}

			Common.runLater(4, () -> {
				for (final Iterator<UUID> iterator = this.entityMetadata.keySet().iterator(); iterator.hasNext();) {
					final UUID uniqueId = iterator.next();
					final Entity entity = Remain.getEntity(uniqueId);

					if (entity == null) {
						if (Remain.getOfflinePlayerByUUID(uniqueId).hasPlayedBefore())
							continue;

						iterator.remove();
					}
				}

				if (!this.entityMetadata.isEmpty())
					this.set("Initialized", true);
			});
		}

		private void loadBlockStates() {
			this.blockMetadata.clear();

			for (final String locationString : this.getMap("Block").keySet()) {
				final Location location = SerializeUtil.deserialize(SerializeUtil.Mode.YAML, Location.class, locationString);

				final Block block = location.getBlock();
				final BlockCache blockCache = this.get("Block." + locationString, BlockCache.class);

				// Check if the block remained the same
				if (block != null && CompMaterial.fromBlock(block) == blockCache.getType())
					this.blockMetadata.put(location, blockCache);
			}

			if (!this.blockMetadata.isEmpty())
				this.set("Initialized", true);
		}

		protected String getMetadata(final UUID entityUid, @NonNull final String key) {
			this.loadIfHasnt();

			final Map<String, String> metadata = this.entityMetadata.get(entityUid);

			if (metadata != null) {
				final String value = metadata.get(key);

				return value != null && !value.isEmpty() ? value : null;
			}

			return null;
		}

		protected void setMetadata(final UUID entityUid, @NonNull final String key, final String value) {
			this.loadIfHasnt();
			this.set("Initialized", true);

			final Map<String, String> metadata = this.entityMetadata.getOrDefault(entityUid, new HashMap<>());
			final boolean remove = value == null || "".equals(value);

			if (remove)
				metadata.remove(key);
			else
				metadata.put(key, value);

			if (metadata.isEmpty())
				this.entityMetadata.remove(entityUid);
			else
				this.entityMetadata.put(entityUid, metadata);

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
			this.set("Initialized", true);

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
				final CompMaterial type = map.get("Type", CompMaterial.class);
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