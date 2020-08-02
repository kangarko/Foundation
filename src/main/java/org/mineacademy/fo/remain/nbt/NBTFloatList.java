package org.mineacademy.fo.remain.nbt;


import org.mineacademy.fo.remain.nbt.nmsmappings.ClassWrapper;
import org.mineacademy.fo.remain.nbt.nmsmappings.ReflectionMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Float implementation for NBTLists
 *
 * @author tr7zw
 */
public class NBTFloatList extends NBTList<Float> {

	NBTFloatList(final NBTCompound owner, final String name, final NBTType type, final Object list) {
		super(owner, name, type, list);
	}

	@Override
	protected Object asTag(final Float object) {
		try {
			final Constructor<?> con = ClassWrapper.NMS_NBTTAGFLOAT.getClazz().getDeclaredConstructor(float.class);
			con.setAccessible(true);
			return con.newInstance(object);
		} catch (final InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
			| NoSuchMethodException | SecurityException e) {
			throw new NbtApiException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

	@Override
	public Float get(final int index) {
		try {
			final Object obj = ReflectionMethod.LIST_GET.run(listObject, index);
			return Float.valueOf(obj.toString());
		} catch (final NumberFormatException nf) {
			return 0f;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

}
