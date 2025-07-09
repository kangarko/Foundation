package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * String implementation for NBTLists
 *
 * @author tr7zw
 *
 */
public class NBTStringList extends NBTList<String> {

	protected NBTStringList(final NBTCompound owner, final String name, final NBTType type, final Object list) {
		super(owner, name, type, list);
	}

	@Override
	public String get(final int index) {
		try {
			final Object ret = ReflectionMethod.LIST_GET_STRING.run(this.listObject, index);
			if (ret instanceof Optional<?>)
				return ((Optional<String>) ret).orElse("");
			return (String) ret;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

	@Override
	protected Object asTag(final String object) {
		try {
			final Constructor<?> con = ClassWrapper.NMS_NBTTAGSTRING.getClazz().getDeclaredConstructor(String.class);
			con.setAccessible(true);
			return con.newInstance(object);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new NbtApiException("Error while wrapping the Object " + object + " to it's NMS object!", e);
		}
	}

}
