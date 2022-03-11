package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.mineacademy.fo.exception.FoException;

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
			throw new FoException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

	@Override
	public int[] get(int index) {
		try {
			final Object obj = ReflectionMethod.LIST_GET.run(this.listObject, index);
			ReflectionMethod.COMPOUND_SET.run(this.tmpContainer.getCompound(), "tmp", obj);
			final int[] val = this.tmpContainer.getIntArray("tmp");
			this.tmpContainer.removeKey("tmp");
			return val;
		} catch (final NumberFormatException nf) {
			return null;
		} catch (final Exception ex) {
			throw new FoException(ex);
		}
	}

}
