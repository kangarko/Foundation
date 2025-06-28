package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Integer implementation for NBTLists
 *
 * @author tr7zw
 *
 */
public class NBTIntArrayList extends NBTList<int[]> {

	private final NBTContainer tmpContainer;

	protected NBTIntArrayList(NBTCompound owner, String name, NBTType type, Object list) {
		super(owner, name, type, list);
		this.tmpContainer = new NBTContainer();
	}

	@Override
	protected Object asTag(int[] object) {
		try {
			final Constructor<?> con = ClassWrapper.NMS_NBTTAGINTARRAY.getClazz().getDeclaredConstructor(int[].class);
			con.setAccessible(true);
			return con.newInstance(object);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new NbtApiException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

	@Override
	public int[] get(int index) {
		try {
			final Object obj = ReflectionMethod.LIST_GET.run(listObject, index);
			ReflectionMethod.COMPOUND_SET.run(tmpContainer.getCompound(), "tmp", obj);
			final int[] val = tmpContainer.getIntArray("tmp");
			tmpContainer.removeKey("tmp");
			return val;
		} catch (final NumberFormatException nf) {
			return null;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

}
