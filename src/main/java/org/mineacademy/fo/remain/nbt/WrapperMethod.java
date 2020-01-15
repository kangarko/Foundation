package org.mineacademy.fo.remain.nbt;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

/**
 * This class caches method reflections, keeps track of method name changes between versions and allows early checking for problems
 *
 * @author tr7zw
 */
@SuppressWarnings("javadoc")
public enum WrapperMethod {

	COMPOUND_SET_FLOAT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, float.class }, V.v1_7, new Since(V.v1_7, "setFloat")),
	COMPOUND_SET_STRING(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, String.class }, V.v1_7, new Since(V.v1_7, "setString")),
	COMPOUND_SET_INT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int.class }, V.v1_7, new Since(V.v1_7, "setInt")),
	COMPOUND_SET_BYTEARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, byte[].class }, V.v1_7, new Since(V.v1_7, "setByteArray")),
	COMPOUND_SET_INTARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int[].class }, V.v1_7, new Since(V.v1_7, "setIntArray")),
	COMPOUND_SET_LONG(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, long.class }, V.v1_7, new Since(V.v1_7, "setLong")),
	COMPOUND_SET_SHORT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, short.class }, V.v1_7, new Since(V.v1_7, "setShort")),
	COMPOUND_SET_BYTE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, byte.class }, V.v1_7, new Since(V.v1_7, "setByte")),
	COMPOUND_SET_DOUBLE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, double.class }, V.v1_7, new Since(V.v1_7, "setDouble")),
	COMPOUND_SET_BOOLEAN(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, boolean.class }, V.v1_7, new Since(V.v1_7, "setBoolean")),
	COMPOUND_MERGE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_7, new Since(V.v1_7, "a")), //FIXME: No Spigot mapping!
	COMPOUND_SET(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, WrapperClass.NMS_NBTBASE.getClazz() }, V.v1_7, new Since(V.v1_7, "set")),
	COMPOUND_GET(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "get")),
	COMPOUND_GET_LIST(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int.class }, V.v1_7, new Since(V.v1_7, "getList")),

	COMPOUND_GET_FLOAT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getFloat")),
	COMPOUND_GET_STRING(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getString")),
	COMPOUND_GET_INT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getInt")),
	COMPOUND_GET_BYTEARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getByteArray")),
	COMPOUND_GET_INTARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getIntArray")),
	COMPOUND_GET_LONG(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getLong")),
	COMPOUND_GET_SHORT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getShort")),
	COMPOUND_GET_BYTE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getByte")),
	COMPOUND_GET_DOUBLE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getDouble")),
	COMPOUND_GET_BOOLEAN(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getBoolean")),
	COMPOUND_GET_COMPOUND(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "getCompound")),

	NMSITEM_GETTAG(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] {}, V.v1_7, new Since(V.v1_7, "getTag")),
	NMSITEM_SAVE(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_7, new Since(V.v1_7, "save")),
	NMSITEM_CREATESTACK(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_7, V.v1_10, new Since(V.v1_7, "createStack")),

	COMPOUND_REMOVE_KEY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "remove")),
	COMPOUND_HAS_KEY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "hasKey")),
	COMPOUND_GET_TYPE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, V.v1_8, new Since(V.v1_8, "b"), new Since(V.v1_9, "d"), new Since(V.v1_15, "e")), //FIXME: No Spigot mapping!
	COMPOUND_GET_KEYS(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] {}, V.v1_7, new Since(V.v1_7, "c"), new Since(V.v1_13, "getKeys")),

	LISTCOMPOUND_GET_KEYS(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] {}, V.v1_7, new Since(V.v1_7, "c"), new Since(V.v1_13, "getKeys")),
	LIST_REMOVE_KEY(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, V.v1_7, new Since(V.v1_7, "a"), new Since(V.v1_9, "remove")),
	LIST_SIZE(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] {}, V.v1_7, new Since(V.v1_7, "size")),
	LIST_SET(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class, WrapperClass.NMS_NBTBASE.getClazz() }, V.v1_7, new Since(V.v1_7, "a"), new Since(V.v1_13, "set")),
	LEGACY_LIST_ADD(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { WrapperClass.NMS_NBTBASE.getClazz() }, V.v1_7, V.v1_13, new Since(V.v1_7, "add")),
	LIST_ADD(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class, WrapperClass.NMS_NBTBASE.getClazz() }, V.v1_14, new Since(V.v1_14, "add")),
	LIST_GET_STRING(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, V.v1_7, new Since(V.v1_7, "getString")),
	LIST_GET_COMPOUND(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, V.v1_7, new Since(V.v1_7, "get")),
	LIST_GET(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, V.v1_7, new Since(V.v1_7, "g"), new Since(V.v1_9, "h"), new Since(V.v1_12, "i"), new Since(V.v1_13, "get")),

	ITEMSTACK_SET_TAG(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_7, new Since(V.v1_7, "setTag")),
	ITEMSTACK_NMSCOPY(WrapperClass.CRAFT_ITEMSTACK.getClazz(), new Class[] { ItemStack.class }, V.v1_7, new Since(V.v1_7, "asNMSCopy")),
	ITEMSTACK_BUKKITMIRROR(WrapperClass.CRAFT_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_ITEMSTACK.getClazz() }, V.v1_7, new Since(V.v1_7, "asCraftMirror")),

	CRAFT_WORLD_GET_HANDLE(WrapperClass.CRAFT_WORLD.getClazz(), new Class[] {}, V.v1_7, new Since(V.v1_7, "getHandle")),
	NMS_WORLD_GET_TILEENTITY(WrapperClass.NMS_WORLDSERVER.getClazz(), new Class[] { WrapperClass.NMS_BLOCKPOSITION.getClazz() }, V.v1_7, new Since(V.v1_7, "getTileEntity")),
	NMS_WORLD_SET_TILEENTITY(WrapperClass.NMS_WORLDSERVER.getClazz(), new Class[] { WrapperClass.NMS_BLOCKPOSITION.getClazz(), WrapperClass.NMS_TILEENTITY.getClazz() }, V.v1_7, new Since(V.v1_7, "setTileEntity")),
	NMS_WORLD_REMOVE_TILEENTITY(WrapperClass.NMS_WORLDSERVER.getClazz(), new Class[] { WrapperClass.NMS_BLOCKPOSITION.getClazz() }, V.v1_7, new Since(V.v1_7, "t"), new Since(V.v1_9, "s"), new Since(V.v1_13, "n"), new Since(V.v1_14, "removeTileEntity")),

	//TILEENTITY_LOAD_LEGACY191(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[]{WrapperClass.NMS_MINECRAFTSERVER.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz()}, V.v1_9, V.v1_9, new Since(V.v1_9, "a")), //FIXME: No Spigot mapping!
	//TILEENTITY_LOAD_LEGACY183(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[]{WrapperClass.NMS_NBTTAGCOMPOUND.getClazz()}, V.v1_8, V.v1_9, new Since(V.v1_8, "c"), new Since(V.v1_9, "a"), new Since(V.v1_9, "c")), //FIXME: No Spigot mapping!
	TILEENTITY_LOAD_LEGACY1121(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_WORLD.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_10, V.v1_12, new Since(V.v1_10, "a"), new Since(V.v1_12, "create")),
	TILEENTITY_LOAD(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_13, new Since(V.v1_12, "create")),

	TILEENTITY_GET_NBT(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_8, new Since(V.v1_8, "b"), new Since(V.v1_9, "save")),
	TILEENTITY_SET_NBT(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_8, new Since(V.v1_8, "a"), new Since(V.v1_12, "load")),

	CRAFT_ENTITY_GET_HANDLE(WrapperClass.CRAFT_ENTITY.getClazz(), new Class[] {}, V.v1_7, new Since(V.v1_7, "getHandle")),
	NMS_ENTITY_SET_NBT(WrapperClass.NMS_ENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_8, new Since(V.v1_8, "f")), //FIXME: No Spigot mapping!
	NMS_ENTITY_GET_NBT(WrapperClass.NMS_ENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, V.v1_8, new Since(V.v1_8, "e"), new Since(V.v1_12, "save")),
	NMS_ENTITY_GETSAVEID(WrapperClass.NMS_ENTITY.getClazz(), new Class[] {}, V.v1_14, new Since(V.v1_14, "getSaveID")),

	NBTFILE_READ(WrapperClass.NMS_NBTCOMPRESSEDSTREAMTOOLS.getClazz(), new Class[] { InputStream.class }, V.v1_7, new Since(V.v1_7, "a")), //FIXME: No Spigot mapping!
	NBTFILE_WRITE(WrapperClass.NMS_NBTCOMPRESSEDSTREAMTOOLS.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), OutputStream.class }, V.v1_7, new Since(V.v1_7, "a")), //FIXME: No Spigot mapping!

	PARSE_NBT(WrapperClass.NMS_MOJANGSONPARSER.getClazz(), new Class[] { String.class }, V.v1_7, new Since(V.v1_7, "parse")),
	REGISTRY_KEYSET(WrapperClass.NMS_REGISTRYSIMPLE.getClazz(), new Class[] {}, V.v1_11, V.v1_13, new Since(V.v1_11, "keySet")),
	REGISTRY_GET(WrapperClass.NMS_REGISTRYSIMPLE.getClazz(), new Class[] { Object.class }, V.v1_11, V.v1_13, new Since(V.v1_11, "get")),
	REGISTRY_SET(WrapperClass.NMS_REGISTRYSIMPLE.getClazz(), new Class[] { Object.class, Object.class }, V.v1_11, V.v1_13, new Since(V.v1_11, "a")), //FIXME: No Spigot mapping!
	REGISTRY_GET_INVERSE(WrapperClass.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { Object.class }, V.v1_11, V.v1_13, new Since(V.v1_11, "b")), //FIXME: No Spigot mapping!
	REGISTRYMATERIALS_KEYSET(WrapperClass.NMS_REGISTRYMATERIALS.getClazz(), new Class[] {}, V.v1_13, new Since(V.v1_13, "keySet")),
	REGISTRYMATERIALS_GET(WrapperClass.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { WrapperClass.NMS_MINECRAFTKEY.getClazz() }, V.v1_13, new Since(V.v1_13, "get")),

	;

	private Method method;
	private boolean loaded = false;
	private boolean compatible = false;
	private String methodName = null;

	private WrapperMethod(Class<?> targetClass, Class<?>[] args, MinecraftVersion.V addedSince, MinecraftVersion.V removedAfter, Since... methodnames) {

		if (MinecraftVersion.olderThan(addedSince) || removedAfter != null && MinecraftVersion.newerThan(removedAfter))
			return;

		compatible = true;
		Since targetVersion = methodnames[0];

		for (final Since since : methodnames)
			if (MinecraftVersion.atLeast(since.version))
				targetVersion = since;

		if (targetClass != null)
			try {
				method = targetClass.getMethod(targetVersion.name, args);
				Valid.checkNotNull(method, "Method null in " + this);

				method.setAccessible(true);
				methodName = targetVersion.name;

				loaded = true;

			} catch (NullPointerException | NoSuchMethodException | SecurityException ex) {
				Common.error(ex, this + " could not find method '" + (targetVersion == null ? "" : targetVersion.name) + "' in '" + targetClass + "'");
			}
	}

	WrapperMethod(Class<?> targetClass, Class<?>[] args, MinecraftVersion.V addedSince, Since... methodnames) {
		this(targetClass, args, addedSince, null, methodnames);
	}

	/**
	 * Runs the method on a given target object using the given args.
	 *
	 * @param target
	 * @param args
	 * @return Value returned by the method
	 */
	public Object run(Object target, Object... args) {
		if (method == null)
			throw new NbtApiException("Method not loaded! '" + this + "'");
		try {
			return method.invoke(target, args);

		} catch (final Exception ex) {
			throw new NbtApiException("Error while calling the method '" + methodName + "' on '" + target + "' loaded: " + loaded + ", Enum: " + this + ", arguments = " + args, ex);
		}
	}

	/**
	 * @return The MethodName, used in this Minecraft Version
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @return Has this method been linked
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * @return Is this method available in this Minecraft Version
	 */
	public boolean isCompatible() {
		return compatible;
	}

	protected static class Since {
		public final org.mineacademy.fo.MinecraftVersion.V version;
		public final String name;

		public Since(MinecraftVersion.V version, String name) {
			this.version = version;
			this.name = name;
		}
	}

}
