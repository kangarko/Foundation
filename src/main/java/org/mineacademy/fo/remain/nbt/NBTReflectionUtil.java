package org.mineacademy.fo.remain.nbt;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

import com.google.gson.Gson;

/**
 * Utility class for translating NBTApi calls to reflections into NMS code All
 * methods are allowed to throw {@link NbtApiException}
 *
 * @author tr7zw
 */
public class NBTReflectionUtil {

	private static final Gson gson = new Gson();

	private static Field field_unhandledTags;

	static {
		try {
			field_unhandledTags = WrapperClass.CRAFT_METAITEM.getClazz().getDeclaredField("unhandledTags");
			field_unhandledTags.setAccessible(true);
		} catch (final NoSuchFieldException e) {
			if (MinecraftVersion.atLeast(V.v1_8))
				e.printStackTrace();
		}
	}

	/**
	 * Hidden constructor
	 */
	private NBTReflectionUtil() {

	}

	/**
	 * Gets the NMS Entity for a given Bukkit Entity
	 *
	 * @param entity Bukkit Entity
	 * @return NMS Entity
	 */
	static Object getNMSEntity(final Entity entity) {
		try {
			return WrapperReflection.CRAFT_ENTITY_GET_HANDLE.run(WrapperClass.CRAFT_ENTITY.getClazz().cast(entity));
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting the NMS Entity from a Bukkit Entity!", e);
		}
	}

	/**
	 * Reads in a InputStream as NMS Compound
	 *
	 * @param stream InputStream of any NBT file
	 * @return NMS Compound
	 */
	static Object readNBT(final InputStream stream) {
		try {
			return WrapperReflection.NBTFILE_READ.run(null, stream);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while reading a NBT File!", e);
		}
	}

	/**
	 * Writes a NMS Compound to an OutputStream
	 *
	 * @param nbt    NMS Compound
	 * @param stream Stream to write to
	 * @return ???
	 */
	static Object writeNBT(final Object nbt, final OutputStream stream) {
		try {
			return WrapperReflection.NBTFILE_WRITE.run(null, nbt, stream);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while writing NBT!", e);
		}
	}

	/**
	 * Writes a Compound to an OutputStream
	 *
	 * @param comp   Compound
	 * @param stream Stream to write to
	 */
	static void writeApiNBT(final NBTCompound comp, final OutputStream stream) {
		try {
			Object nbttag = comp.getCompound();
			if (nbttag == null)
				nbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
			if (!valideCompound(comp))
				return;
			final Object workingtag = gettoCompount(nbttag, comp);
			WrapperReflection.NBTFILE_WRITE.run(null, workingtag, stream);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while writing NBT!", e);
		}
	}

	/**
	 * Simulates getOrCreateTag. If an Item doesn't yet have a Tag, it will return a
	 * new empty tag.
	 *
	 * @param nmsitem
	 * @return NMS Compound
	 */
	static Object getItemRootNBTTagCompound(final Object nmsitem) {
		try {
			if (nmsitem == null)
				return WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();

			final Object answer = WrapperReflection.NMSITEM_GETTAG.run(nmsitem);
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
	static Object convertNBTCompoundtoNMSItem(final NBTCompound nbtcompound) {
		try {
			final Object nmsComp = gettoCompount(nbtcompound.getCompound(), nbtcompound);
			if (WrapperVersion.getVersion().getVersionId() >= WrapperVersion.MC1_11_R1.getVersionId())
				return WrapperObject.NMS_COMPOUNDFROMITEM.getInstance(nmsComp);
			else
				return WrapperReflection.NMSITEM_CREATESTACK.run(null, nmsComp);
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
	static NBTContainer convertNMSItemtoNBTCompound(final Object nmsitem) {
		try {
			final Object answer = WrapperReflection.NMSITEM_SAVE.run(nmsitem, WrapperObject.NMS_NBTTAGCOMPOUND.getInstance());
			return new NBTContainer(answer);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while converting NMS ItemStack to NBTCompound!", e);
		}
	}

	/**
	 * Gets a live copy of non-vanilla NBT tags.
	 *
	 * @param meta ItemMeta from which tags should be retrieved
	 * @return Map containing unhandled (custom) NBT tags
	 */
	static Map<String, Object> getUnhandledNBTTags(final ItemMeta meta) {
		try {
			return (Map<String, Object>) field_unhandledTags.get(meta);
		} catch (final Exception e) {
			throw new NbtApiException("Exception while getting unhandled tags from ItemMeta!", e);
		}
	}

	/**
	 * Gets the Vanilla NBT Compound from a given NMS Entity
	 *
	 * @param nmsEntity
	 * @return NMS NBT Compound
	 */
	static Object getEntityNBTTagCompound(final Object nmsEntity) {
		try {
			final Object nbt = WrapperClass.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			Object answer = WrapperReflection.NMS_ENTITY_GET_NBT.run(nmsEntity, nbt);
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
	static Object setEntityNBTTag(final Object nbtTag, final Object nmsEntity) {
		try {
			WrapperReflection.NMS_ENTITY_SET_NBT.run(nmsEntity, nbtTag);
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
	static Object getTileEntityNBTTagCompound(final BlockState tile) {
		try {
			final Object pos = WrapperObject.NMS_BLOCKPOSITION.getInstance(tile.getX(), tile.getY(), tile.getZ());
			final Object cworld = WrapperClass.CRAFT_WORLD.getClazz().cast(tile.getWorld());
			final Object nmsworld = WrapperReflection.CRAFT_WORLD_GET_HANDLE.run(cworld);
			final Object o = WrapperReflection.NMS_WORLD_GET_TILEENTITY.run(nmsworld, pos);
			final Object tag = WrapperClass.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			Object answer = WrapperReflection.TILEENTITY_GET_NBT.run(o, tag);
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
	static void setTileEntityNBTTagCompound(final BlockState tile, final Object comp) {
		try {
			final Object pos = WrapperObject.NMS_BLOCKPOSITION.getInstance(tile.getX(), tile.getY(), tile.getZ());
			final Object cworld = WrapperClass.CRAFT_WORLD.getClazz().cast(tile.getWorld());
			final Object nmsworld = WrapperReflection.CRAFT_WORLD_GET_HANDLE.run(cworld);
			final Object o = WrapperReflection.NMS_WORLD_GET_TILEENTITY.run(nmsworld, pos);
			if (WrapperVersion.getVersion().getVersionId() >= WrapperVersion.MC1_16_R1.getVersionId()) {
				final Object blockData = WrapperReflection.TILEENTITY_GET_BLOCKDATA.run(o);
				WrapperReflection.TILEENTITY_SET_NBT.run(o, blockData, comp);
			} else
				WrapperReflection.TILEENTITY_SET_NBT_LEGACY1151.run(o, comp);
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
	private static Object getSubNBTTagCompound(final Object compound, final String name) {
		try {
			if ((boolean) WrapperReflection.COMPOUND_HAS_KEY.run(compound, name))
				return WrapperReflection.COMPOUND_GET_COMPOUND.run(compound, name);
			else
				throw new NbtApiException("Tried getting invalide compound '" + name + "' from '" + compound + "'!");
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
	static void addNBTTagCompound(final NBTCompound comp, final String name) {
		if (name == null) {
			remove(comp, name);
			return;
		}
		Object nbttag = comp.getCompound();
		if (nbttag == null)
			nbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			return;
		final Object workingtag = gettoCompount(nbttag, comp);
		try {
			WrapperReflection.COMPOUND_SET.run(workingtag, name, WrapperClass.NMS_NBTTAGCOMPOUND.getClazz().newInstance());
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
	static Boolean valideCompound(final NBTCompound comp) {
		Object root = comp.getCompound();
		if (root == null)
			root = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		return (gettoCompount(root, comp)) != null;
	}

	static Object gettoCompount(Object nbttag, NBTCompound comp) {
		final Deque<String> structure = new ArrayDeque<>();
		while (comp.getParent() != null) {
			structure.add(comp.getName());
			comp = comp.getParent();
		}
		while (!structure.isEmpty()) {
			final String target = structure.pollLast();
			nbttag = getSubNBTTagCompound(nbttag, target);
			if (nbttag == null)
				throw new NbtApiException("Unable to find tag '" + target + "' in " + nbttag);
		}
		return nbttag;
	}

	/**
	 * Merges the second {@link NBTCompound} into the first one
	 *
	 * @param comp           Target for the merge
	 * @param nbtcompoundSrc Data to merge
	 */
	static void mergeOtherNBTCompound(final NBTCompound comp, final NBTCompound nbtcompoundSrc) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = gettoCompount(rootnbttag, comp);
		Object rootnbttagSrc = nbtcompoundSrc.getCompound();
		if (rootnbttagSrc == null)
			rootnbttagSrc = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(nbtcompoundSrc))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtagSrc = gettoCompount(rootnbttagSrc, nbtcompoundSrc);
		try {
			WrapperReflection.COMPOUND_MERGE.run(workingtag, workingtagSrc);
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
	static String getContent(final NBTCompound comp, final String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = gettoCompount(rootnbttag, comp);
		try {
			return WrapperReflection.COMPOUND_GET.run(workingtag, key).toString();
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
	public static void set(final NBTCompound comp, final String key, final Object val) {
		if (val == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = gettoCompount(rootnbttag, comp);
		try {
			WrapperReflection.COMPOUND_SET.run(workingtag, key, val);
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
	public static <T> NBTList<T> getList(final NBTCompound comp, final String key, final NBTType type, final Class<T> clazz) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			return null;
		final Object workingtag = gettoCompount(rootnbttag, comp);
		try {
			final Object nbt = WrapperReflection.COMPOUND_GET_LIST.run(workingtag, key, type.getId());
			if (clazz == String.class)
				return (NBTList<T>) new NBTStringList(comp, key, type, nbt);
			else if (clazz == NBTListCompound.class)
				return (NBTList<T>) new NBTCompoundList(comp, key, type, nbt);
			else if (clazz == Integer.class)
				return (NBTList<T>) new NBTIntegerList(comp, key, type, nbt);
			else if (clazz == Float.class)
				return (NBTList<T>) new NBTFloatList(comp, key, type, nbt);
			else if (clazz == Double.class)
				return (NBTList<T>) new NBTDoubleList(comp, key, type, nbt);
			else if (clazz == Long.class)
				return (NBTList<T>) new NBTLongList(comp, key, type, nbt);
			else
				return null;
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
	static void setObject(final NBTCompound comp, final String key, final Object value) {
		try {
			final String json = gson.toJson(value);

			setData(comp, WrapperReflection.COMPOUND_SET_STRING, key, json);

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
	public static <T> T getObject(final NBTCompound comp, final String key, final Class<T> type) {
		final String json = (String) getData(comp, WrapperReflection.COMPOUND_GET_STRING, key);

		if (json == null)
			return null;

		final T obj = gson.fromJson(json, type);
		return type.cast(obj);
	}

	/**
	 * Deletes the given key
	 *
	 * @param comp
	 * @param key
	 */
	public static void remove(final NBTCompound comp, final String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			return;
		final Object workingtag = gettoCompount(rootnbttag, comp);
		WrapperReflection.COMPOUND_REMOVE_KEY.run(workingtag, key);
		comp.setCompound(rootnbttag);
	}

	/**
	 * Gets the Keyset inside this Compound
	 *
	 * @param comp
	 * @return Set of all keys
	 */
	static Set<String> getKeys(final NBTCompound comp) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = gettoCompount(rootnbttag, comp);
		return (Set<String>) WrapperReflection.COMPOUND_GET_KEYS.run(workingtag);
	}

	/**
	 * Sets data inside the Compound
	 *
	 * @param comp
	 * @param type
	 * @param key
	 * @param data
	 */
	static void setData(final NBTCompound comp, final WrapperReflection type, final String key, final Object data) {
		if (data == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			rootnbttag = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
		if (!valideCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = gettoCompount(rootnbttag, comp);
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
	public static Object getData(final NBTCompound comp, final WrapperReflection type, final String key) {
		final Object rootnbttag = comp.getCompound();
		if (rootnbttag == null)
			return null;
		if (!valideCompound(comp))
			throw new NbtApiException("The Compound wasn't able to be linked back to the root!");
		final Object workingtag = gettoCompount(rootnbttag, comp);
		return type.run(workingtag, key);
	}

}
