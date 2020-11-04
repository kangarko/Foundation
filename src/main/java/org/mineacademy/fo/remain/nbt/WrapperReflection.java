package org.mineacademy.fo.remain.nbt;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.exception.FoException;

/**
 * This class caches method reflections, keeps track of method name changes between versions and allows early checking for problems
 *
 * @author tr7zw
 *
 */
enum WrapperReflection {

	COMPOUND_SET_FLOAT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, float.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setFloat")),
	COMPOUND_SET_STRING(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setString")),
	COMPOUND_SET_INT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setInt")),
	COMPOUND_SET_BYTEARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, byte[].class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setByteArray")),
	COMPOUND_SET_INTARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int[].class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setIntArray")),
	COMPOUND_SET_LONG(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, long.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setLong")),
	COMPOUND_SET_SHORT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, short.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setShort")),
	COMPOUND_SET_BYTE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, byte.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setByte")),
	COMPOUND_SET_DOUBLE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, double.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setDouble")),
	COMPOUND_SET_BOOLEAN(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, boolean.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setBoolean")),
	COMPOUND_SET_UUID(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, UUID.class }, WrapperVersion.MC1_16_R1, new Since(WrapperVersion.MC1_16_R1, "a")),
	COMPOUND_MERGE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "a")), //FIXME: No Spigot mapping!
	COMPOUND_SET(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, WrapperClass.NMS_NBTBASE.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "set")),
	COMPOUND_GET(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "get")),
	COMPOUND_GET_LIST(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getList")),
	COMPOUND_OWN_TYPE(WrapperClass.NMS_NBTBASE.getClazz(), new Class[] {}, WrapperVersion.MC1_7_R4, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getTypeId")), // Only needed for 1.7.10 getType

	COMPOUND_GET_FLOAT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getFloat")),
	COMPOUND_GET_STRING(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getString")),
	COMPOUND_GET_INT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getInt")),
	COMPOUND_GET_BYTEARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getByteArray")),
	COMPOUND_GET_INTARRAY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getIntArray")),
	COMPOUND_GET_LONG(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getLong")),
	COMPOUND_GET_SHORT(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getShort")),
	COMPOUND_GET_BYTE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getByte")),
	COMPOUND_GET_DOUBLE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getDouble")),
	COMPOUND_GET_BOOLEAN(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getBoolean")),
	COMPOUND_GET_UUID(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_16_R1, new Since(WrapperVersion.MC1_16_R1, "a")),
	COMPOUND_GET_COMPOUND(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getCompound")),

	NMSITEM_GETTAG(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] {}, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getTag")),
	NMSITEM_SAVE(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "save")),
	NMSITEM_CREATESTACK(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, WrapperVersion.MC1_10_R1, new Since(WrapperVersion.MC1_7_R4, "createStack")),

	COMPOUND_REMOVE_KEY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "remove")),
	COMPOUND_HAS_KEY(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "hasKey")),
	COMPOUND_GET_TYPE(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "b"), new Since(WrapperVersion.MC1_9_R1, "d"), new Since(WrapperVersion.MC1_15_R1, "e"), new Since(WrapperVersion.MC1_16_R1, "d")), //FIXME: No Spigot mapping!
	COMPOUND_GET_KEYS(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] {}, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "c"), new Since(WrapperVersion.MC1_13_R1, "getKeys")),

	LISTCOMPOUND_GET_KEYS(WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] {}, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "c"), new Since(WrapperVersion.MC1_13_R1, "getKeys")),
	LIST_REMOVE_KEY(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "a"), new Since(WrapperVersion.MC1_9_R1, "remove")),
	LIST_SIZE(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] {}, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "size")),
	LIST_SET(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class, WrapperClass.NMS_NBTBASE.getClazz() }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "a"), new Since(WrapperVersion.MC1_13_R1, "set")),
	LEGACY_LIST_ADD(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { WrapperClass.NMS_NBTBASE.getClazz() }, WrapperVersion.MC1_7_R4, WrapperVersion.MC1_13_R2, new Since(WrapperVersion.MC1_7_R4, "add")),
	LIST_ADD(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class, WrapperClass.NMS_NBTBASE.getClazz() }, WrapperVersion.MC1_14_R1, new Since(WrapperVersion.MC1_14_R1, "add")),
	LIST_GET_STRING(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getString")),
	LIST_GET_COMPOUND(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "get")),
	LIST_GET(WrapperClass.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "get"), new Since(WrapperVersion.MC1_8_R3, "g"), new Since(WrapperVersion.MC1_9_R1, "h"), new Since(WrapperVersion.MC1_12_R1, "i"), new Since(WrapperVersion.MC1_13_R1, "get")),

	ITEMSTACK_SET_TAG(WrapperClass.NMS_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "setTag")),
	ITEMSTACK_NMSCOPY(WrapperClass.CRAFT_ITEMSTACK.getClazz(), new Class[] { ItemStack.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "asNMSCopy")),
	ITEMSTACK_BUKKITMIRROR(WrapperClass.CRAFT_ITEMSTACK.getClazz(), new Class[] { WrapperClass.NMS_ITEMSTACK.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "asCraftMirror")),

	CRAFT_WORLD_GET_HANDLE(WrapperClass.CRAFT_WORLD.getClazz(), new Class[] {}, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getHandle")),
	NMS_WORLD_GET_TILEENTITY(WrapperClass.NMS_WORLDSERVER.getClazz(), new Class[] { WrapperClass.NMS_BLOCKPOSITION.getClazz() }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "getTileEntity")),
	NMS_WORLD_SET_TILEENTITY(WrapperClass.NMS_WORLDSERVER.getClazz(), new Class[] { WrapperClass.NMS_BLOCKPOSITION.getClazz(), WrapperClass.NMS_TILEENTITY.getClazz() }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "setTileEntity")),
	NMS_WORLD_REMOVE_TILEENTITY(WrapperClass.NMS_WORLDSERVER.getClazz(), new Class[] { WrapperClass.NMS_BLOCKPOSITION.getClazz() }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "t"), new Since(WrapperVersion.MC1_9_R1, "s"), new Since(WrapperVersion.MC1_13_R1, "n"), new Since(WrapperVersion.MC1_14_R1, "removeTileEntity")),

	NMS_WORLD_GET_TILEENTITY_1_7_10(WrapperClass.NMS_WORLDSERVER.getClazz(), new Class[] { int.class, int.class, int.class }, WrapperVersion.MC1_7_R4, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getTileEntity")),

	TILEENTITY_LOAD_LEGACY191(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_MINECRAFTSERVER.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_9_R1, WrapperVersion.MC1_9_R1, new Since(WrapperVersion.MC1_9_R1, "a")), //FIXME: No Spigot mapping!
	TILEENTITY_LOAD_LEGACY183(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_8_R3, WrapperVersion.MC1_9_R2, new Since(WrapperVersion.MC1_8_R3, "c"), new Since(WrapperVersion.MC1_9_R1, "a"), new Since(WrapperVersion.MC1_9_R2, "c")), //FIXME: No Spigot mapping!
	TILEENTITY_LOAD_LEGACY1121(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_WORLD.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_10_R1, WrapperVersion.MC1_12_R1, new Since(WrapperVersion.MC1_10_R1, "a"), new Since(WrapperVersion.MC1_12_R1, "create")),
	TILEENTITY_LOAD_LEGACY1151(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_13_R1, WrapperVersion.MC1_15_R1, new Since(WrapperVersion.MC1_12_R1, "create")),
	TILEENTITY_LOAD(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_IBLOCKDATA.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_16_R1, new Since(WrapperVersion.MC1_16_R1, "create")),

	TILEENTITY_GET_NBT(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "b"), new Since(WrapperVersion.MC1_9_R1, "save")),
	TILEENTITY_SET_NBT_LEGACY1151(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, WrapperVersion.MC1_15_R1, new Since(WrapperVersion.MC1_7_R4, "a"), new Since(WrapperVersion.MC1_12_R1, "load")),
	TILEENTITY_SET_NBT(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] { WrapperClass.NMS_IBLOCKDATA.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_16_R1, new Since(WrapperVersion.MC1_16_R1, "load")),
	TILEENTITY_GET_BLOCKDATA(WrapperClass.NMS_TILEENTITY.getClazz(), new Class[] {}, WrapperVersion.MC1_16_R1, new Since(WrapperVersion.MC1_16_R1, "getBlock")),

	CRAFT_ENTITY_GET_HANDLE(WrapperClass.CRAFT_ENTITY.getClazz(), new Class[] {}, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "getHandle")),
	NMS_ENTITY_SET_NBT(WrapperClass.NMS_ENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "f"), new Since(WrapperVersion.MC1_16_R1, "load")), //FIXME: No Spigot mapping!
	NMS_ENTITY_GET_NBT(WrapperClass.NMS_ENTITY.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "e"), new Since(WrapperVersion.MC1_12_R1, "save")),
	NMS_ENTITY_GETSAVEID(WrapperClass.NMS_ENTITY.getClazz(), new Class[] {}, WrapperVersion.MC1_14_R1, new Since(WrapperVersion.MC1_14_R1, "getSaveID")),

	NBTFILE_READ(WrapperClass.NMS_NBTCOMPRESSEDSTREAMTOOLS.getClazz(), new Class[] { InputStream.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "a")), //FIXME: No Spigot mapping!
	NBTFILE_WRITE(WrapperClass.NMS_NBTCOMPRESSEDSTREAMTOOLS.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), OutputStream.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "a")), //FIXME: No Spigot mapping!

	PARSE_NBT(WrapperClass.NMS_MOJANGSONPARSER.getClazz(), new Class[] { String.class }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "parse")),
	REGISTRY_KEYSET(WrapperClass.NMS_REGISTRYSIMPLE.getClazz(), new Class[] {}, WrapperVersion.MC1_11_R1, WrapperVersion.MC1_13_R1, new Since(WrapperVersion.MC1_11_R1, "keySet")),
	REGISTRY_GET(WrapperClass.NMS_REGISTRYSIMPLE.getClazz(), new Class[] { Object.class }, WrapperVersion.MC1_11_R1, WrapperVersion.MC1_13_R1, new Since(WrapperVersion.MC1_11_R1, "get")),
	REGISTRY_SET(WrapperClass.NMS_REGISTRYSIMPLE.getClazz(), new Class[] { Object.class, Object.class }, WrapperVersion.MC1_11_R1, WrapperVersion.MC1_13_R1, new Since(WrapperVersion.MC1_11_R1, "a")), //FIXME: No Spigot mapping!
	REGISTRY_GET_INVERSE(WrapperClass.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { Object.class }, WrapperVersion.MC1_11_R1, WrapperVersion.MC1_13_R1, new Since(WrapperVersion.MC1_11_R1, "b")), //FIXME: No Spigot mapping!
	REGISTRYMATERIALS_KEYSET(WrapperClass.NMS_REGISTRYMATERIALS.getClazz(), new Class[] {}, WrapperVersion.MC1_13_R1, new Since(WrapperVersion.MC1_13_R1, "keySet")),
	REGISTRYMATERIALS_GET(WrapperClass.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { WrapperClass.NMS_MINECRAFTKEY.getClazz() }, WrapperVersion.MC1_13_R1, new Since(WrapperVersion.MC1_13_R1, "get")),
	REGISTRYMATERIALS_GETKEY(WrapperClass.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { Object.class }, WrapperVersion.MC1_13_R2, new Since(WrapperVersion.MC1_13_R2, "getKey")),

	GAMEPROFILE_DESERIALIZE(WrapperClass.NMS_GAMEPROFILESERIALIZER.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz() }, WrapperVersion.MC1_7_R4, new Since(WrapperVersion.MC1_7_R4, "deserialize")),
	GAMEPROFILE_SERIALIZE(WrapperClass.NMS_GAMEPROFILESERIALIZER.getClazz(), new Class[] { WrapperClass.NMS_NBTTAGCOMPOUND.getClazz(), WrapperClass.GAMEPROFILE.getClazz() }, WrapperVersion.MC1_8_R3, new Since(WrapperVersion.MC1_8_R3, "serialize"));

	private WrapperVersion removedAfter;
	private Since targetVersion;
	private Method method;
	private boolean loaded = false;
	private boolean compatible = false;
	private String methodName = null;

	WrapperReflection(Class<?> targetClass, Class<?>[] args, WrapperVersion addedSince, WrapperVersion removedAfter, Since... methodnames) {
		this.removedAfter = removedAfter;
		if (!WrapperVersion.isAtLeastVersion(addedSince) || (this.removedAfter != null && WrapperVersion.isNewerThan(removedAfter)))
			return;
		compatible = true;
		final WrapperVersion server = WrapperVersion.getVersion();
		Since target = methodnames[0];
		for (final Since s : methodnames) {
			if (s.version.getVersionId() <= server.getVersionId() && target.version.getVersionId() < s.version.getVersionId())
				target = s;
		}
		targetVersion = target;
		try {
			method = targetClass.getMethod(targetVersion.name, args);
			method.setAccessible(true);
			loaded = true;
			methodName = targetVersion.name;
		} catch (NullPointerException | NoSuchMethodException | SecurityException ex) {
			System.out.println("[NBTAPI] Unable to find the method '" + targetVersion.name + "' in '" + (targetClass == null ? "null" : targetClass.getSimpleName()) + "' Enum: " + this); //NOSONAR This gets loaded before the logger is loaded
		}
	}

	WrapperReflection(Class<?> targetClass, Class<?>[] args, WrapperVersion addedSince, Since... methodnames) {
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
			throw new FoException("Method not loaded! '" + this + "'");
		try {
			return method.invoke(target, args);
		} catch (final Exception ex) {
			throw new FoException(ex, "Error while calling the method '" + methodName + "', loaded: " + loaded + ", Enum: " + this);
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
		public final WrapperVersion version;
		public final String name;

		public Since(WrapperVersion version, String name) {
			this.version = version;
			this.name = name;
		}
	}

}
