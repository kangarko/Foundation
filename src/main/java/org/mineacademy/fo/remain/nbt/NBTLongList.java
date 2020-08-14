package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Long implementation for NBTLists
 *
 * @author tr7zw
 */
public class NBTLongList extends NBTList<Long> {

	NBTLongList(final NBTCompound owner, final String name, final NBTType type, final Object list) {
		super(owner, name, type, list);
	}

	@Override
	protected Object asTag(final Long object) {
		try {
			final Constructor<?> con = WrapperClass.NMS_NBTTAGLONG.getClazz().getDeclaredConstructor(long.class);
			con.setAccessible(true);
			return con.newInstance(object);
		} catch (final InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new NbtApiException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

	@Override
	public Long get(final int index) {
		try {
			final Object obj = WrapperReflection.LIST_GET.run(listObject, index);
			return Long.valueOf(obj.toString().replace("L", ""));
		} catch (final NumberFormatException nf) {
			return 0L;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

}
