package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * Integer implementation for NBTLists
 *
 * @author tr7zw
 *
 */
public class NBTUUIDList extends NBTList<UUID> {

	private final NBTContainer tmpContainer;

	protected NBTUUIDList(final NBTCompound owner, final String name, final NBTType type, final Object list) {
		super(owner, name, type, list);
		this.tmpContainer = new NBTContainer();
	}

	@Override
	protected Object asTag(final UUID object) {
		try {
			final Constructor<?> con = ClassWrapper.NMS_NBTTAGINTARRAY.getClazz().getDeclaredConstructor(int[].class);
			con.setAccessible(true);
			return con.newInstance(UUIDUtil.uuidToIntArray(object));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new NbtApiException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

	@Override
	public UUID get(final int index) {
		try {
			final Object obj = ReflectionMethod.LIST_GET.run(this.listObject, index);
			ReflectionMethod.COMPOUND_SET.run(this.tmpContainer.getCompound(), "tmp", obj);
			final int[] val = this.tmpContainer.getIntArray("tmp");
			this.tmpContainer.removeKey("tmp");
			return UUIDUtil.uuidFromIntArray(val);
		} catch (final NumberFormatException nf) {
			return null;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

}
