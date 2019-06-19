package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.Stack;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.FoException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Utility class for manipulating NBT tags
 */
class NBTReflectionUtil {

	/**
	 * The Google's JSON library for serializing objects
	 */
	private static final Gson gson = new Gson();

	private static Class getCraftItemStack() {
		return ReflectionUtil.getOFCClass("inventory.CraftItemStack");
	}

	private static Class getCraftEntity() {
		return ReflectionUtil.getOFCClass("entity.CraftEntity");
	}

	protected static Class getNBTBase() {
		return ReflectionUtil.getNMSClass("NBTBase");
	}

	protected static Class getNBTTagString() {
		return ReflectionUtil.getNMSClass("NBTTagString");
	}

	protected static Class getNBTTagCompound() {
		return ReflectionUtil.getNMSClass("NBTTagCompound");
	}

	protected static Class getTileEntity() {
		return ReflectionUtil.getNMSClass("TileEntity");
	}

	protected static Class getCraftWorld() {
		return ReflectionUtil.getOFCClass("CraftWorld");
	}

	private static Object getNewNBTTag() {
		return ReflectionUtil.instatiate(ReflectionUtil.getNMSClass("NBTTagCompound"));
	}

	private static Object getNewBlockPosition(int x, int y, int z) {
		try {
			final Class<?> c = ReflectionUtil.getNMSClass("BlockPosition");

			return c.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
		} catch (final Exception ex) {
			throw new FoException("Error in getting new block position", ex);
		}
	}

	public static Object setNBTTag(Object NBTTag, Object NMSItem) {
		try {
			final Method method = NMSItem.getClass().getMethod("setTag", NBTTag.getClass());

			method.invoke(NMSItem, NBTTag);

			return NMSItem;

		} catch (final Exception ex) {
			throw new FoException("Error in setting nbt tag", ex);
		}
	}

	public static Object getNMSItemStack(ItemStack item) {
		if (item.getType() == Material.AIR)
			return null;

		final Class<?> cis = getCraftItemStack();

		try {
			return cis.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);

		} catch (final Exception ex) {
			throw new FoException("Error in getting nms itemstack", ex);
		}
	}

	public static Object getNMSEntity(Entity entity) {
		try {
			return getCraftEntity().getMethod("getHandle").invoke(getCraftEntity().cast(entity));

		} catch (final Exception ex) {
			throw new FoException("Error in getting nms entity", ex);
		}
	}

	public static ItemStack getBukkitItemStack(Object item) {

		try {
			final Class cis = getCraftItemStack();
			final Method method = getCraftItemStack().getMethod("asCraftMirror", item.getClass());

			return (ItemStack) method.invoke(cis, item);
		} catch (final Exception ex) {
			throw new FoException("Error in getting bukkit itemstack", ex);
		}
	}

	public static Object getItemRootNBTTagCompound(Object nmsitem) {
		if (nmsitem == null)
			return null;

		try {
			final Method method = nmsitem.getClass().getMethod("getTag");

			return method.invoke(nmsitem);

		} catch (final Exception ex) {
			throw new FoException("Error in getting item root nbt tag", ex);
		}
	}

	public static Object getEntityNBTTagCompound(Object nmsitem) {

		final Class c = nmsitem.getClass();
		java.lang.reflect.Method method;
		try {
			method = c.getMethod(getEntityGetNbtMethodName(), getNBTTagCompound());
			final Object nbt = getNBTTagCompound().newInstance();
			Object answer = method.invoke(nmsitem, nbt);
			if (answer == null)
				answer = nbt;
			return answer;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object setEntityNBTTag(Object NBTTag, Object NMSItem) {
		try {
			java.lang.reflect.Method method;
			method = NMSItem.getClass().getMethod(getEntitySetNbtMethodName(), getNBTTagCompound());
			method.invoke(NMSItem, NBTTag);
			return NMSItem;
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static Object getTileEntityNBTTagCompound(BlockState tile) {
		java.lang.reflect.Method method;
		try {
			final Object pos = getNewBlockPosition(tile.getX(), tile.getY(), tile.getZ());
			final Object cworld = getCraftWorld().cast(tile.getWorld());
			final Object nmsworld = cworld.getClass().getMethod("getHandle").invoke(cworld);
			final Object o = nmsworld.getClass().getMethod("getTileEntity", pos.getClass()).invoke(nmsworld, pos);
			method = getTileEntity().getMethod(getTileDataMethodName(), getNBTTagCompound());
			final Object tag = getNBTTagCompound().newInstance();
			Object answer = method.invoke(o, tag);
			if (answer == null)
				answer = tag;
			return answer;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void setTileEntityNBTTagCompound(BlockState tile, Object comp) {
		java.lang.reflect.Method method;
		try {
			final Object pos = getNewBlockPosition(tile.getX(), tile.getY(), tile.getZ());
			final Object cworld = getCraftWorld().cast(tile.getWorld());
			final Object nmsworld = cworld.getClass().getMethod("getHandle").invoke(cworld);
			final Object o = nmsworld.getClass().getMethod("getTileEntity", pos.getClass()).invoke(nmsworld, pos);
			method = getTileEntity().getMethod("a", getNBTTagCompound());
			method.invoke(o, comp);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static Object getSubNBTTagCompound(Object compound, String name) {

		final Class c = compound.getClass();
		java.lang.reflect.Method method;
		try {
			method = c.getMethod("getCompound", String.class);
			final Object answer = method.invoke(compound, name);
			return answer;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void addNBTTagCompound(NBTCompound comp, String name) {
		if (name == null) {
			remove(comp, name);
			return;
		}
		Object nbttag = comp.getCompound();
		if (nbttag == null) {
			nbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(nbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("set", String.class, getNBTBase());
			method.invoke(workingtag, name, getNBTTagCompound().newInstance());
			comp.setCompound(nbttag);

			return;
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return;
	}

	public static Boolean validateCompound(NBTCompound comp) {
		Object root = comp.getCompound();
		if (root == null) {
			root = getNewNBTTag();
		}
		return getToCompound(root, comp) != null;
	}

	private static Object getToCompound(Object nbttag, NBTCompound comp) {
		final Stack<String> structure = new Stack<>();
		while (comp.getParent() != null) {
			structure.add(comp.getName());
			comp = comp.getParent();
		}
		while (!structure.isEmpty()) {
			nbttag = getSubNBTTagCompound(nbttag, structure.pop());
			if (nbttag == null) {
				return null;
			}
		}
		return nbttag;
	}

	public static void setString(NBTCompound comp, String key, String text) {
		if (text == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setString", String.class, String.class);
			method.invoke(workingtag, key, text);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static String getString(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getString", String.class);
			return (String) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static Object get(NBTCompound comp, String key) {
		Object root = comp.getCompound();

		if (root == null)
			root = getNewNBTTag();

		if (!validateCompound(comp))
			return null;

		final Object working = getToCompound(root, comp);

		try {
			return working.getClass().getMethod("get", String.class).invoke(working, key);

		} catch (final Exception ex) {
			ex.printStackTrace();

			return null;
		}
	}

	public static void setInt(NBTCompound comp, String key, Integer i) {
		if (i == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setInt", String.class, int.class);
			method.invoke(workingtag, key, i);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Integer getInt(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getInt", String.class);
			return (Integer) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setByteArray(NBTCompound comp, String key, byte[] b) {
		if (b == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setByteArray", String.class, byte[].class);
			method.invoke(workingtag, key, b);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return;
	}

	public static byte[] getByteArray(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getByteArray", String.class);
			return (byte[]) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setIntArray(NBTCompound comp, String key, int[] i) {
		if (i == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setIntArray", String.class, int[].class);
			method.invoke(workingtag, key, i);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static int[] getIntArray(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getIntArray", String.class);
			return (int[]) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setFloat(NBTCompound comp, String key, Float f) {
		if (f == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setFloat", String.class, float.class);
			method.invoke(workingtag, key, (float) f);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Float getFloat(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getFloat", String.class);
			return (Float) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setLong(NBTCompound comp, String key, Long f) {
		if (f == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setLong", String.class, long.class);
			method.invoke(workingtag, key, (long) f);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Long getLong(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getLong", String.class);
			return (Long) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setShort(NBTCompound comp, String key, Short f) {
		if (f == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setShort", String.class, short.class);
			method.invoke(workingtag, key, (short) f);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Short getShort(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getShort", String.class);
			return (Short) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setByte(NBTCompound comp, String key, Byte f) {
		if (f == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setByte", String.class, byte.class);
			method.invoke(workingtag, key, (byte) f);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Byte getByte(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getByte", String.class);
			return (Byte) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setDouble(NBTCompound comp, String key, Double d) {
		if (d == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setDouble", String.class, double.class);
			method.invoke(workingtag, key, d);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Double getDouble(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getDouble", String.class);
			return (Double) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static byte getType(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return 0;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod(getTypeMethodName(), String.class);
			return (byte) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}

	public static void setBoolean(NBTCompound comp, String key, Boolean d) {
		if (d == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("setBoolean", String.class, boolean.class);
			method.invoke(workingtag, key, d);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Boolean getBoolean(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getBoolean", String.class);
			return (Boolean) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void set(NBTCompound comp, String key, Object val) {
		if (val == null) {
			remove(comp, key);
			return;
		}
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp)) {
			new Throwable("Invalid compound " + comp).printStackTrace();
			return;
		}
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("set", String.class, getNBTBase());

			method.invoke(workingtag, key, val);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static NBTList getList(NBTCompound comp, String key, NBTType type) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("getList", String.class, int.class);
			return new NBTList(comp, key, type, method.invoke(workingtag, key, type.getId()));
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void setObject(NBTCompound comp, String key, String string) {
		try {
			setString(comp, key, string);

		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static <T> T getObject(NBTCompound comp, String key, Class<T> type) {
		final String json = getString(comp, key);
		if (json == null)
			return null;

		try {
			return deserializeJson(json, type);

		} catch (final JsonSyntaxException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	private static <T> T deserializeJson(String json, Class<T> type) throws JsonSyntaxException {
		final T obj = gson.fromJson(json, type);

		return type.cast(obj);
	}

	public static void remove(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("remove", String.class);
			method.invoke(workingtag, key);
			comp.setCompound(rootnbttag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Boolean hasKey(NBTCompound comp, String key) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod("hasKey", String.class);
			return (Boolean) method.invoke(workingtag, key);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static Set<String> getKeys(NBTCompound comp) {
		Object rootnbttag = comp.getCompound();
		if (rootnbttag == null) {
			rootnbttag = getNewNBTTag();
		}
		if (!validateCompound(comp))
			return null;
		final Object workingtag = getToCompound(rootnbttag, comp);
		java.lang.reflect.Method method;
		try {
			method = workingtag.getClass().getMethod(MinecraftVersion.atLeast(V.v1_13) ? "getKeys" : "c");
			return (Set<String>) method.invoke(workingtag);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	protected static String getTileDataMethodName() {
		return MinecraftVersion.olderThan(V.v1_9) ? "b" : "save";
	}

	protected static String getTypeMethodName() {
		return MinecraftVersion.olderThan(V.v1_9) ? "b" : "d";
	}

	protected static String getEntityGetNbtMethodName() {
		return "b";
	}

	protected static String getEntitySetNbtMethodName() {
		return "a";
	}

	protected static String getRemoveMethodName() {
		return MinecraftVersion.olderThan(V.v1_9) ? "a" : "remove";
	}
}
