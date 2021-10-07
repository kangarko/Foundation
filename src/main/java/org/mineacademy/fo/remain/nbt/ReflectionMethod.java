package org.mineacademy.fo.remain.nbt;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * This class caches method reflections, keeps track of method name changes between versions and allows early checking for problems
 *
 * @author tr7zw
 *
 */
enum ReflectionMethod {

	COMPOUND_SET_FLOAT(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, float.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setFloat")),
	COMPOUND_SET_STRING(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setString")),
	COMPOUND_SET_INT(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setInt")),
	COMPOUND_SET_BYTEARRAY(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, byte[].class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setByteArray")),
	COMPOUND_SET_INTARRAY(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int[].class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setIntArray")),
	COMPOUND_SET_LONG(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, long.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setLong")),
	COMPOUND_SET_SHORT(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, short.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setShort")),
	COMPOUND_SET_BYTE(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, byte.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setByte")),
	COMPOUND_SET_DOUBLE(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, double.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setDouble")),
	COMPOUND_SET_BOOLEAN(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, boolean.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setBoolean")),
	COMPOUND_SET_UUID(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, UUID.class }, MinecraftVersion.MC1_16_R1, new Since(MinecraftVersion.MC1_16_R1, "a")),
	COMPOUND_MERGE(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_8_R3, new Since(MinecraftVersion.MC1_8_R3, "a")), //FIXME: No Spigot mapping!
	COMPOUND_SET(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, ClassWrapper.NMS_NBTBASE.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "set")),
	COMPOUND_GET(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "get")),
	COMPOUND_GET_LIST(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class, int.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getList")),
	COMPOUND_OWN_TYPE(ClassWrapper.NMS_NBTBASE.getClazz(), new Class[] {}, MinecraftVersion.MC1_7_R4, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getTypeId")), // Only needed for 1.7.10 getType

	COMPOUND_GET_FLOAT(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getFloat")),
	COMPOUND_GET_STRING(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getString")),
	COMPOUND_GET_INT(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getInt")),
	COMPOUND_GET_BYTEARRAY(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getByteArray")),
	COMPOUND_GET_INTARRAY(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getIntArray")),
	COMPOUND_GET_LONG(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getLong")),
	COMPOUND_GET_SHORT(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getShort")),
	COMPOUND_GET_BYTE(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getByte")),
	COMPOUND_GET_DOUBLE(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getDouble")),
	COMPOUND_GET_BOOLEAN(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getBoolean")),
	COMPOUND_GET_UUID(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_16_R1, new Since(MinecraftVersion.MC1_16_R1, "a")),
	COMPOUND_GET_COMPOUND(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getCompound")),

	NMSITEM_GETTAG(ClassWrapper.NMS_ITEMSTACK.getClazz(), new Class[] {}, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getTag")),
	NMSITEM_SAVE(ClassWrapper.NMS_ITEMSTACK.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "save")),
	NMSITEM_CREATESTACK(ClassWrapper.NMS_ITEMSTACK.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, MinecraftVersion.MC1_10_R1, new Since(MinecraftVersion.MC1_7_R4, "createStack")),

	COMPOUND_REMOVE_KEY(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "remove")),
	COMPOUND_HAS_KEY(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "hasKey")),
	COMPOUND_GET_TYPE(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_8_R3, new Since(MinecraftVersion.MC1_8_R3, "b"), new Since(MinecraftVersion.MC1_9_R1, "d"), new Since(MinecraftVersion.MC1_15_R1, "e"), new Since(MinecraftVersion.MC1_16_R1, "d")), //FIXME: No Spigot mapping!
	COMPOUND_GET_KEYS(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] {}, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "c"), new Since(MinecraftVersion.MC1_13_R1, "getKeys")),

	LISTCOMPOUND_GET_KEYS(ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), new Class[] {}, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "c"), new Since(MinecraftVersion.MC1_13_R1, "getKeys")),
	LIST_REMOVE_KEY(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, MinecraftVersion.MC1_8_R3, new Since(MinecraftVersion.MC1_8_R3, "a"), new Since(MinecraftVersion.MC1_9_R1, "remove")),
	LIST_SIZE(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] {}, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "size")),
	LIST_SET(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class, ClassWrapper.NMS_NBTBASE.getClazz() }, MinecraftVersion.MC1_8_R3, new Since(MinecraftVersion.MC1_8_R3, "a"), new Since(MinecraftVersion.MC1_13_R1, "set")),
	LEGACY_LIST_ADD(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] { ClassWrapper.NMS_NBTBASE.getClazz() }, MinecraftVersion.MC1_7_R4, MinecraftVersion.MC1_13_R2, new Since(MinecraftVersion.MC1_7_R4, "add")),
	LIST_ADD(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class, ClassWrapper.NMS_NBTBASE.getClazz() }, MinecraftVersion.MC1_14_R1, new Since(MinecraftVersion.MC1_14_R1, "add")),
	LIST_GET_STRING(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getString")),
	LIST_GET_COMPOUND(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "get")),
	LIST_GET(ClassWrapper.NMS_NBTTAGLIST.getClazz(), new Class[] { int.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "get"), new Since(MinecraftVersion.MC1_8_R3, "g"), new Since(MinecraftVersion.MC1_9_R1, "h"), new Since(MinecraftVersion.MC1_12_R1, "i"), new Since(MinecraftVersion.MC1_13_R1, "get")),

	ITEMSTACK_SET_TAG(ClassWrapper.NMS_ITEMSTACK.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "setTag")),
	ITEMSTACK_NMSCOPY(ClassWrapper.CRAFT_ITEMSTACK.getClazz(), new Class[] { ItemStack.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "asNMSCopy")),
	ITEMSTACK_BUKKITMIRROR(ClassWrapper.CRAFT_ITEMSTACK.getClazz(), new Class[] { ClassWrapper.NMS_ITEMSTACK.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "asCraftMirror")),

	CRAFT_WORLD_GET_HANDLE(ClassWrapper.CRAFT_WORLD.getClazz(), new Class[] {}, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getHandle")),
	NMS_WORLD_GET_TILEENTITY(ClassWrapper.NMS_WORLDSERVER.getClazz(), new Class[] { ClassWrapper.NMS_BLOCKPOSITION.getClazz() }, MinecraftVersion.MC1_8_R3, new Since(MinecraftVersion.MC1_8_R3, "getTileEntity")),
	NMS_WORLD_SET_TILEENTITY(ClassWrapper.NMS_WORLDSERVER.getClazz(), new Class[] { ClassWrapper.NMS_BLOCKPOSITION.getClazz(), ClassWrapper.NMS_TILEENTITY.getClazz() }, MinecraftVersion.MC1_8_R3, MinecraftVersion.MC1_16_R3, new Since(MinecraftVersion.MC1_8_R3, "setTileEntity")),
	NMS_WORLD_REMOVE_TILEENTITY(ClassWrapper.NMS_WORLDSERVER.getClazz(), new Class[] { ClassWrapper.NMS_BLOCKPOSITION.getClazz() }, MinecraftVersion.MC1_8_R3, new Since(MinecraftVersion.MC1_8_R3, "t"), new Since(MinecraftVersion.MC1_9_R1, "s"), new Since(MinecraftVersion.MC1_13_R1, "n"), new Since(MinecraftVersion.MC1_14_R1, "removeTileEntity")),

	NMS_WORLD_GET_TILEENTITY_1_7_10(ClassWrapper.NMS_WORLDSERVER.getClazz(), new Class[] { int.class, int.class, int.class }, MinecraftVersion.MC1_7_R4, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getTileEntity")),

	TILEENTITY_LOAD_LEGACY191(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_MINECRAFTSERVER.getClazz(), ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_9_R1, MinecraftVersion.MC1_9_R1, new Since(MinecraftVersion.MC1_9_R1, "a")), //FIXME: No Spigot mapping!
	TILEENTITY_LOAD_LEGACY183(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_8_R3, MinecraftVersion.MC1_9_R2, new Since(MinecraftVersion.MC1_8_R3, "c"), new Since(MinecraftVersion.MC1_9_R1, "a"), new Since(MinecraftVersion.MC1_9_R2, "c")), //FIXME: No Spigot mapping!
	TILEENTITY_LOAD_LEGACY1121(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_WORLD.getClazz(), ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_10_R1, MinecraftVersion.MC1_12_R1, new Since(MinecraftVersion.MC1_10_R1, "a"), new Since(MinecraftVersion.MC1_12_R1, "create")),
	TILEENTITY_LOAD_LEGACY1151(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_13_R1, MinecraftVersion.MC1_15_R1, new Since(MinecraftVersion.MC1_12_R1, "create")),
	TILEENTITY_LOAD(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_IBLOCKDATA.getClazz(), ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_16_R1, MinecraftVersion.MC1_16_R3, new Since(MinecraftVersion.MC1_16_R1, "create")),

	TILEENTITY_GET_NBT(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "b"), new Since(MinecraftVersion.MC1_9_R1, "save")),
	TILEENTITY_SET_NBT_LEGACY1151(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, MinecraftVersion.MC1_15_R1, new Since(MinecraftVersion.MC1_7_R4, "a"), new Since(MinecraftVersion.MC1_12_R1, "load")),
	TILEENTITY_SET_NBT_LEGACY1161(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_IBLOCKDATA.getClazz(), ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_16_R1, MinecraftVersion.MC1_16_R3, new Since(MinecraftVersion.MC1_16_R1, "load")),
	TILEENTITY_SET_NBT(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_17_R1, new Since(MinecraftVersion.MC1_16_R1, "load")),
	TILEENTITY_GET_BLOCKDATA(ClassWrapper.NMS_TILEENTITY.getClazz(), new Class[] {}, MinecraftVersion.MC1_16_R1, new Since(MinecraftVersion.MC1_16_R1, "getBlock")),

	CRAFT_ENTITY_GET_HANDLE(ClassWrapper.CRAFT_ENTITY.getClazz(), new Class[] {}, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "getHandle")),
	NMS_ENTITY_SET_NBT(ClassWrapper.NMS_ENTITY.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "f"), new Since(MinecraftVersion.MC1_16_R1, "load")), //FIXME: No Spigot mapping!
	NMS_ENTITY_GET_NBT(ClassWrapper.NMS_ENTITY.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "e"), new Since(MinecraftVersion.MC1_12_R1, "save")),
	NMS_ENTITY_GETSAVEID(ClassWrapper.NMS_ENTITY.getClazz(), new Class[] {}, MinecraftVersion.MC1_14_R1, new Since(MinecraftVersion.MC1_14_R1, "getSaveID")),

	NBTFILE_READ(ClassWrapper.NMS_NBTCOMPRESSEDSTREAMTOOLS.getClazz(), new Class[] { InputStream.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "a")), //FIXME: No Spigot mapping!
	NBTFILE_WRITE(ClassWrapper.NMS_NBTCOMPRESSEDSTREAMTOOLS.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), OutputStream.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "a")), //FIXME: No Spigot mapping!

	PARSE_NBT(ClassWrapper.NMS_MOJANGSONPARSER.getClazz(), new Class[] { String.class }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "parse")),
	REGISTRY_KEYSET(ClassWrapper.NMS_REGISTRYSIMPLE.getClazz(), new Class[] {}, MinecraftVersion.MC1_11_R1, MinecraftVersion.MC1_13_R1, new Since(MinecraftVersion.MC1_11_R1, "keySet")),
	REGISTRY_GET(ClassWrapper.NMS_REGISTRYSIMPLE.getClazz(), new Class[] { Object.class }, MinecraftVersion.MC1_11_R1, MinecraftVersion.MC1_13_R1, new Since(MinecraftVersion.MC1_11_R1, "get")),
	REGISTRY_SET(ClassWrapper.NMS_REGISTRYSIMPLE.getClazz(), new Class[] { Object.class, Object.class }, MinecraftVersion.MC1_11_R1, MinecraftVersion.MC1_13_R1, new Since(MinecraftVersion.MC1_11_R1, "a")), //FIXME: No Spigot mapping!
	REGISTRY_GET_INVERSE(ClassWrapper.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { Object.class }, MinecraftVersion.MC1_11_R1, MinecraftVersion.MC1_13_R1, new Since(MinecraftVersion.MC1_11_R1, "b")), //FIXME: No Spigot mapping!
	REGISTRYMATERIALS_KEYSET(ClassWrapper.NMS_REGISTRYMATERIALS.getClazz(), new Class[] {}, MinecraftVersion.MC1_13_R1, new Since(MinecraftVersion.MC1_13_R1, "keySet")),
	REGISTRYMATERIALS_GET(ClassWrapper.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { ClassWrapper.NMS_MINECRAFTKEY.getClazz() }, MinecraftVersion.MC1_13_R1, new Since(MinecraftVersion.MC1_13_R1, "get")),
	REGISTRYMATERIALS_GETKEY(ClassWrapper.NMS_REGISTRYMATERIALS.getClazz(), new Class[] { Object.class }, MinecraftVersion.MC1_13_R2, new Since(MinecraftVersion.MC1_13_R2, "getKey")),

	GAMEPROFILE_DESERIALIZE(ClassWrapper.NMS_GAMEPROFILESERIALIZER.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_7_R4, new Since(MinecraftVersion.MC1_7_R4, "deserialize")),
	GAMEPROFILE_SERIALIZE(ClassWrapper.NMS_GAMEPROFILESERIALIZER.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz(), ClassWrapper.GAMEPROFILE.getClazz() }, MinecraftVersion.MC1_8_R3, new Since(MinecraftVersion.MC1_8_R3, "serialize")),

	CRAFT_PERSISTENT_DATA_CONTAINER_TO_TAG(ClassWrapper.CRAFT_PERSISTENTDATACONTAINER.getClazz(), new Class[] {}, MinecraftVersion.MC1_14_R1, new Since(MinecraftVersion.MC1_14_R1, "toTagCompound")),
	CRAFT_PERSISTENT_DATA_CONTAINER_GET_MAP(ClassWrapper.CRAFT_PERSISTENTDATACONTAINER.getClazz(), new Class[] {}, MinecraftVersion.MC1_14_R1, new Since(MinecraftVersion.MC1_14_R1, "getRaw")),
	CRAFT_PERSISTENT_DATA_CONTAINER_PUT_ALL(ClassWrapper.CRAFT_PERSISTENTDATACONTAINER.getClazz(), new Class[] { ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz() }, MinecraftVersion.MC1_14_R1, new Since(MinecraftVersion.MC1_14_R1, "putAll")),
	;

	private MinecraftVersion removedAfter;
	private Since targetVersion;
	private Method method;
	private boolean loaded = false;
	private boolean compatible = false;
	private String methodName = null;

	ReflectionMethod(Class<?> targetClass, Class<?>[] args, MinecraftVersion addedSince, MinecraftVersion removedAfter, Since... methodnames) {
		this.removedAfter = removedAfter;
		if (!MinecraftVersion.isAtLeastVersion(addedSince) || (this.removedAfter != null && MinecraftVersion.isNewerThan(removedAfter)))
			return;
		compatible = true;
		final MinecraftVersion server = MinecraftVersion.getVersion();
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

	ReflectionMethod(Class<?> targetClass, Class<?>[] args, MinecraftVersion addedSince, Since... methodnames) {
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
			throw new NbtApiException("Error while calling the method '" + methodName + "', loaded: " + loaded + ", Enum: " + this, ex);
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
		public final MinecraftVersion version;
		public final String name;

		public Since(MinecraftVersion version, String name) {
			this.version = version;
			this.name = name;
		}
	}

}
