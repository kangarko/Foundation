package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.mineacademy.fo.exception.FoException;

/**
 * Float implementation for NBTLists
 *
 * @author tr7zw
 *
 */
public class NBTFloatList extends NBTList<Float> {

	protected NBTFloatList(NBTCompound owner, String name, NBTType type, Object list) {
		super(owner, name, type, list);
	}

	@Override
	protected Object asTag(Float object) {
		try {
			final Constructor<?> con = WrapperClass.NMS_NBTTAGFLOAT.getClazz().getDeclaredConstructor(float.class);
			con.setAccessible(true);
			return con.newInstance(object);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new FoException(e, "Error while wrapping the Object " + object + " to it's NMS object!");
		}
	}

	@Override
	public Float get(int index) {
		try {
			final Object obj = WrapperReflection.LIST_GET.run(listObject, index);
			return Float.parseFloat(obj.toString());
		} catch (final NumberFormatException nf) {
			return 0f;
		} catch (final Exception ex) {
			throw new FoException(ex);
		}
	}

}
