package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.exception.FoException;

/**
 * This Enum wraps Constructors for NMS classes
 *
 * @author tr7zw
 *
 */
enum WrapperObject {
	NMS_NBTTAGCOMPOUND(null, null, WrapperClass.NMS_NBTTAGCOMPOUND.getClazz()),
	NMS_BLOCKPOSITION(null, null, WrapperClass.NMS_BLOCKPOSITION.getClazz(), int.class, int.class, int.class),
	NMS_COMPOUNDFROMITEM(WrapperVersion.MC1_11_R1, null, WrapperClass.NMS_ITEMSTACK.getClazz(), WrapperClass.NMS_NBTTAGCOMPOUND.getClazz()),;

	private Constructor<?> construct;
	private Class<?> targetClass;

	WrapperObject(WrapperVersion from, WrapperVersion to, Class<?> clazz, Class<?>... args) {
		if (clazz == null)
			return;
		if (from != null && WrapperVersion.getVersion().getVersionId() < from.getVersionId())
			return;
		if (to != null && WrapperVersion.getVersion().getVersionId() > to.getVersionId())
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
			throw new FoException(ex, "Exception while creating a new instance of '" + targetClass + "'");
		}
	}

}
