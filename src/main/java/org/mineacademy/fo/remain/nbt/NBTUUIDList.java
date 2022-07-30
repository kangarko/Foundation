package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.mineacademy.fo.exception.FoException;

/**
 * Integer implementation for NBTLists
 *
 * @author tr7zw
 *
 */
public class NBTUUIDList extends NBTList<UUID> {

	private final NBTContainer tmpContainer;

	protected NBTUUIDList(NBTCompound owner, String name, NBTType type, Object list) {
		super(owner, name, type, list);
		this.tmpContainer = new NBTContainer();
	}

	@Override
	protected Object asTag(UUID object) {
		try {
			final Constructor<?> con = ClassWrapper.NMS_NBTTAGINTARRAY.getClazz().getDeclaredConstructor(int[].class);
			con.setAccessible(true);
			return con.newInstance(uuidToIntArray(object));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new FoException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

	@Override
	public UUID get(int index) {
		try {
			final Object obj = ReflectionMethod.LIST_GET.run(this.listObject, index);
			ReflectionMethod.COMPOUND_SET.run(this.tmpContainer.getCompound(), "tmp", obj);
			final int[] val = this.tmpContainer.getIntArray("tmp");
			this.tmpContainer.removeKey("tmp");
			return uuidFromIntArray(val);
		} catch (final NumberFormatException nf) {
			return null;
		} catch (final Exception ex) {
			throw new FoException(ex);
		}
	}

	public static UUID uuidFromIntArray(int[] is) {
		return new UUID((long) is[0] << 32 | is[1] & 4294967295L,
				(long) is[2] << 32 | is[3] & 4294967295L);
	}

	public static int[] uuidToIntArray(UUID uUID) {
		final long l = uUID.getMostSignificantBits();
		final long m = uUID.getLeastSignificantBits();
		return leastMostToIntArray(l, m);
	}

	private static int[] leastMostToIntArray(long l, long m) {
		return new int[] { (int) (l >> 32), (int) l, (int) (m >> 32), (int) m };
	}

}
