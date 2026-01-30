package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Field;

final class ReflectionUtil {

	public static Field getMappedField(Class<?> clazz, String mapping) {
		final String mojmapName = mapping.split("#")[1];
		try {
			return clazz.getField(mojmapName);
		} catch (NoSuchFieldException | SecurityException e) {
			// not Mojamp, try remapped
		}
		try {
			return clazz.getDeclaredField(MojangToMapping.getMapping().get(mapping));
		} catch (final Exception e) {
			throw new NbtApiException("Unable to find field " + mapping + " in class " + clazz.getName(), e);
		}
	}

}