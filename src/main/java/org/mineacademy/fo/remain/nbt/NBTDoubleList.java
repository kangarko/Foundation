package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Double implementation for NBTLists
 *
 * @author tr7zw
 */
public class NBTDoubleList extends NBTList<Double> {

	NBTDoubleList(final NBTCompound owner, final String name, final NBTType type, final Object list) {
		super(owner, name, type, list);
	}

	@Override
	protected Object asTag(final Double object) {
		try {
			final Constructor<?> con = WrapperClass.NMS_NBTTAGDOUBLE.getClazz().getDeclaredConstructor(double.class);
			con.setAccessible(true);
			return con.newInstance(object);
		} catch (final InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new NbtApiException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

	@Override
	public Double get(final int index) {
		try {
			final Object obj = WrapperReflection.LIST_GET.run(listObject, index);
			return Double.valueOf(obj.toString());
		} catch (final NumberFormatException nf) {
			return 0d;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

}
