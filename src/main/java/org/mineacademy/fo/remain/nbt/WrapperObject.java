package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;

/**
 * This Enum wraps Constructors for NMS classes
 *
 * @author tr7zw
 *
 */
public enum WrapperObject {
	NMS_NBTTAGCOMPOUND(null, null, WrapperClass.NMS_NBTTAGCOMPOUND.getClazz()),
	NMS_BLOCKPOSITION(null, null, WrapperClass.NMS_BLOCKPOSITION.getClazz(), int.class, int.class, int.class),
	NMS_COMPOUNDFROMITEM(MinecraftVersion.V.v1_11, null, WrapperClass.NMS_ITEMSTACK.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz()),
	;

	private Constructor<?> construct;
	private Class<?> targetClass;

	WrapperObject(MinecraftVersion.V from, MinecraftVersion.V to, Class<?> clazz, Class<?>... args) {
		if (from != null && MinecraftVersion.olderThan(from))
			return;
		if (to != null && MinecraftVersion.newerThan(to))
			return;
		try {
			this.targetClass = clazz;
			construct = clazz.getDeclaredConstructor(args);
			construct.setAccessible(true);

		} catch (final Exception ex) {
			Common.error(ex, "Unable to find the constructor for the class '" + clazz.getName() + "'");
		}
	}

	/**
	 * Creates an Object instance with given args
	 *
	 * @param args
	 * @return Object created
	 */
	public Object getInstance(Object... args) {
		try {
			return construct.newInstance(args);
		} catch (final Exception ex) {
			throw new NbtApiException("Exception while creating a new instance of '" + targetClass + "'", ex);
		}
	}

}
