package org.mineacademy.fo.remain.nbt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.MinecraftVersion.V;

import com.google.gson.Gson;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for translating NBTApi calls to reflections into NMS code All
 * methods are allowed to throw {@link NbtApiException}
 *
 * @author tr7zw
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NBTReflectionUtil {

	private static final Gson gson = new Gson();

	/**
	 * Gets the NMS Entity for a given Bukkit Entity
	 *
	 * @param entity Bukkit Entity
	 * @return NMS Entity
	 */
	public static Object getNMSEntity(Entity entity) {
		try {
			return WrapperMethod.CRAFT_ENTITY_GET_HANDLE.run(WrapperClass.CRAFT_ENTITY.getClazz().cast(entity));
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting the NMS Entity from a Bukkit Entity!", e);
		}
	}

	/**
	 * Reads in a FileInputStream as NMS Compound
	 *
	 * @param stream InputStream of any NBT file
	 * @return NMS Compound
	 */
	public static Object readNBTFile(FileInputStream stream) {
		try {
			return WrapperMethod.NBTFILE_READ.run(null, stream);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while reading a NBT File!", e);
		}
	}

	/**
	 * Writes a NMS Compound to a FileOutputStream
	 *
	 * @param nbt    NMS Compound
	 * @param stream Stream to write to
	 * @return ???
	 */
	public static Object saveNBTFile(Object nbt, FileOutputStream stream) {
		try {
			return WrapperMethod.NBTFILE_WRITE.run(null, nbt, stream);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while saving a NBT File!", e);
		}
	}

	/**
	 * Simulates getOrCreateTag. If an Item doesn't yet have a Tag, it will return a
	 * new empty tag.
	 *
	 * @param nmsitem
	 * @return NMS Compound
	 */
	public static Object getItemRootNBTTagCompound(Object nmsitem) {
		try {
			final Object answer = WrapperMethod.NMSITEM_GETTAG.run(nmsitem);
			return answer != null ? answer : WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting an Itemstack's NBTCompound!", e);
		}
	}

	/**
	 * Converts {@link NBTCompound} to NMS ItemStacks
	 *
	 * @param nbtcompound Any valid {@link NBTCompound}
	 * @return NMS ItemStack
	 */
	public static Object convertNBTCompoundtoNMSItem(NBTCompound nbtcompound) {
		try {
			if (org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_11)) {
				return WrapperObject.NMS_COMPOUNDFROMITEM.getInstance(nbtcompound.getCompound());
			} else {
				return WrapperMethod.NMSITEM_CREATESTACK.run(null, nbtcompound.getCompound());
			}
		} catch (final Exception e) {
			throw new NbtApiException("Exception while converting NBTCompound to NMS ItemStack!", e);
		}
	}

	/**
	 * Converts NMS ItemStacks to {@link NBTContainer}
	 *
	 * @param nmsitem NMS ItemStack
	 * @return {@link NBTContainer} with all the data
	 */
	public static NBTContainer convertNMSItemtoNBTCompound(Object nmsitem) {
		try {
			final Object answer = WrapperMethod.NMSITEM_SAVE.run(nmsitem, WrapperObject.NMS_NBTTAGCOMPOUND.getInstance());
			return new NBTContainer(answer);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while converting NMS ItemStack to NBTCompound!", e);
		}
	}

	/**
	 * Gets the Vanilla NBT Compound from a given NMS Entity
	 *
	 * @param nmsEntity
	 * @return NMS NBT Compound
	 */
	public static Object getEntityNBTTagCompound(Object nmsEntity) {
		try {
			final Object nbt = WrapperClass.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			Object answer = WrapperMethod.NMS_ENTITY_GET_NBT.run(nmsEntity, nbt);
			if (answer == null)
				answer = nbt;
			return answer;
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting NBTCompound from NMS Entity!", e);
		}
	}

	/**
	 * Loads all Vanilla tags from a NMS Compound into a NMS Entity
	 *
	 * @param nbtTag
	 * @param nmsEntity
	 * @return The NMS Entity
	 */
	public static Object setEntityNBTTag(Object nbtTag, Object nmsEntity) {
		try {
			WrapperMethod.NMS_ENTITY_SET_NBT.run(nmsEntity, nbtTag);
			return nmsEntity;
		} catch (final Exception ex) {
			throw new NbtApiException("Exception while setting the NBTCompound of an Entity", ex);
		}
	}

	/**
	 * Gets the NMS Compound from a given TileEntity
	 *
	 * @param tile
	 * @return NMS Compound with the Vanilla data
	 */
	public static Object getTileEntityNBTTagCompound(BlockState tile) {
		try {
			final Object pos = WrapperObject.NMS_BLOCKPOSITION.getInstance(tile.getX(), tile.getY(), tile.getZ());
			final Object cworld = WrapperClass.CRAFT_WORLD.getClazz().cast(tile.getWorld());
			final Object nmsworld = WrapperMethod.CRAFT_WORLD_GET_HANDLE.run(cworld);
			final Object o = WrapperMethod.NMS_WORLD_GET_TILEENTITY.run(nmsworld, pos);
			final Object tag = WrapperClass.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			Object answer = WrapperMethod.TILEENTITY_GET_NBT.run(o, tag);
			if (answer == null)
				answer = tag;
			return answer;
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting NBTCompound from TileEntity!", e);
		}
	}

	/**
	 * Sets Vanilla tags from a NMS Compound to a TileEntity
	 *
	 * @param tile
	 * @param comp
	 */
	public static void setTileEntityNBTTagCompound(BlockState tile, Object comp) {
		try {
			final Object pos = WrapperObject.NMS_BLOCKPOSITION.getInstance(tile.getX(), tile.getY(), tile.getZ());
			final Object cworld = WrapperClass.CRAFT_WORLD.getClazz().cast(tile.getWorld());
			final Object nmsworld = WrapperMethod.CRAFT_WORLD_GET_HANDLE.run(cworld);
			final Object o = WrapperMethod.NMS_WORLD_GET_TILEENTITY.run(nmsworld, pos);

			WrapperMethod.TILEENTITY_SET_NBT.run(o, comp);

		} catch (final Exception e) {
			throw new NbtApiException("Exception while setting NBTData for a TileEntity!", e);
		}
	}

	/**
	 * Gets the subCompound with a given name from a NMS Compound
	 *
	 * @param compound
	 * @param name
	 * @return NMS Compound or null
	 */
	public static Object getSubNBTTagCompound(Object compound, String name) {
		try {
			if ((boolean) WrapperMethod.COMPOUND_HAS_KEY.run(compound, name)) {
				return WrapperMethod.COMPOUND_GET_COMPOUND.run(compound, name);
			} else {
				throw new NbtApiException("Tried getting invalide compound '" + name + "' from '" + compound + "'!");
			}
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting NBT subcompounds!", e);
		}
	}

	/**
	 * Creates a subCompound with a given name in the given NMS Compound
	 *
	 * @param comp
	 * @param name
	 */
	public static void addNBTTagCompound(NBTCompound comp, String name) {
		if (name == null) {
			remove(comp, name);
			return;
		}
		Object nbttag = comp.getCompound();
		if (nbttag == null) {
			nbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp))
			return;

		final Object workingtag = getToCompound(nbttag, comp);
		try {
			WrapperMethod.COMPOUND_SET.run(workingtag, name, WrapperClass.NMS_NBTTAGCOMPOUND.getClazz().newInstance());
			comp.setCompound(nbttag);

		} catch (final Exception e) {
			throw new NbtApiException("Exception while adding a Compound!", e);
		}
	}

	/**
	 * Checks if the Compound is correctly linked to it's roots
	 *
	 * @param comp
	 * @return true if this is a valide Compound, else false
	 */
	public static Boolean validateCompound(NBTCompound comp) {
		Object root = comp.getCompound();
		if (root == null) {
			root = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		return getToCompound(root, comp) != null;
	}

	protected static Object getToCompound(Object nbttag, NBTCompound comp) {
		final Deque<String> structure = new ArrayDeque<>();
		while (comp.getParent() != null) {
			structure.add(comp.getName());
			comp = comp.getParent();
		}
		while (!structure.isEmpty()) {
			final String target = structure.pollLast();
			nbttag = getSubNBTTagCompound(nbttag, target);
			if (nbttag == null) {
				throw new NbtApiException("Unable to find tag '" + target + "' in " + nbttag);
			}
		}
		return nbttag;
	}

	/**
	 * Merges the second {@link NBTCompound} into the first one
	 *
	 * @param comp        Target for the merge
	 * @param nbtcompound Data to merge
	 */
	public static void mergeOtherNBTCompound(NBTCompound comp, NBTCompound nbtcompound) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = getToCompound(rootnbttag, comp);
		try {
			WrapperMethod.COMPOUND_MERGE.run(workingtag, nbtcompound.getCompound());
			comp.setCompound(rootnbttag);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while merging two NBTCompounds!", e);
		}
	}

	/**
	 * Returns the content for a given key inside a Compound
	 *
	 * @param comp
	 * @param key
	 * @return Content saved under this key
	 */
	public static String getContent(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = getToCompound(rootnbttag, comp);
		try {
			return WrapperMethod.COMPOUND_GET.run(workingtag, key).toString();
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting the Content for key '" + key + "'!", e);
		}
	}

	/**
	 * Sets a key in a {@link NBTCompound} to a given value
	 *
	 * @param comp
	 * @param key
	 * @param val
	 */
	public static void set(NBTCompound comp, String key, Object val) {
		if (val == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp)) {
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		}
		final Object workingtag = getToCompound(rootnbttag, comp);
		try {
			WrapperMethod.COMPOUND_SET.run(workingtag, key, val);
			comp.setCompound(rootnbttag);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while setting key '" + key + "' to '" + val + "'!", e);
		}
	}

	/**
	 * Returns the List saved with a given key.
	 *
	 * @param comp
	 * @param key
	 * @param type
	 * @param clazz
	 * @return The list at that key. Null if it's an invalide type
	 */
	public static <T> NBTList<T> getList(NBTCompound comp, String key, NBTType type, Class<T> clazz) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		try {
			final Object nbt = WrapperMethod.COMPOUND_GET_LIST.run(workingtag, key, type.getId());
			if (clazz == String.class) {
				return (NBTList<T>) new NBTStringList(comp, key, type, nbt);
			} else if (clazz == NBTListCompound.class) {
				return (NBTList<T>) new NBTCompoundList(comp, key, type, nbt);
			} else if (clazz == Integer.class) {
				return (NBTList<T>) new NBTIntegerList(comp, key, type, nbt);
			} else {
				return null;
			}
		} catch (final Exception ex) {
			throw new NbtApiException("Exception while getting a list with the type '" + type + "'!", ex);
		}
	}

	/**
	 * Uses Gson to set a {@link Serializable} value in a Compound
	 *
	 * @param comp
	 * @param key
	 * @param value
	 */
	public static void setObject(NBTCompound comp, String key, Object value) {
		try {
			final String json = gson.toJson(value);
			setData(comp, WrapperMethod.COMPOUND_SET_STRING, key, json);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while setting the Object '" + value + "'!", e);
		}
	}

	/**
	 * Uses Gson to load back a {@link Serializable} object from the Compound
	 *
	 * @param comp
	 * @param key
	 * @param type
	 * @return The loaded Object or null, if not found
	 */
	public static <T> T getObject(NBTCompound comp, String key, Class<T> type) {
		final String json = (String) getData(comp, WrapperMethod.COMPOUND_GET_STRING, key);
		if (json == null)
			return null;

		return deserializeJson(json, type);
	}

	private static <T> T deserializeJson(String json, Class<T> type) {
		try {
			if (json == null) {
				return null;
			}

			final T obj = gson.fromJson(json, type);
			return type.cast(obj);
		} catch (final Exception ex) {
			throw new NbtApiException("Error while converting json to " + type.getName(), ex);
		}
	}

	/**
	 * Deletes the given key
	 *
	 * @param comp
	 * @param key
	 */
	public static void remove(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		WrapperMethod.COMPOUND_REMOVE_KEY.run(workingtag, key);
		comp.setCompound(rootnbttag);
	}

	/**
	 * Gets the Keyset inside this Compound
	 *
	 * @param comp
	 * @return Set of all keys
	 */
	public static Set<String> getKeys(NBTCompound comp) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = getToCompound(rootnbttag, comp);
		return (Set<String>) WrapperMethod.COMPOUND_GET_KEYS.run(workingtag);
	}

	/**
	 * Sets data inside the Compound
	 *
	 * @param comp
	 * @param type
	 * @param key
	 * @param data
	 */
	public static void setData(NBTCompound comp, WrapperMethod type, String key, Object data) {
		if (data == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		}
		if (!validateCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = getToCompound(rootnbttag, comp);
		type.run(workingtag, key, data);
		comp.setCompound(rootnbttag);
	}

	/**
	 * Gets data from the Compound
	 *
	 * @param comp
	 * @param type
	 * @param key
	 * @return The value or default fallback from NMS
	 */
	public static Object getData(NBTCompound comp, WrapperMethod type, String key) {
		final Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			return null;
		}
		if (!validateCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = getToCompound(rootnbttag, comp);
		return type.run(workingtag, key);
	}

}
