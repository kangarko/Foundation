package org.mineacademy.fo.remain.nbt;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.mineacademy.fo.exception.FoException;

final class ReflectionUtil {

	private static Field field_modifiers;

	static {
		try {
			field_modifiers = Field.class.getDeclaredField("modifiers");
			field_modifiers.setAccessible(true);
		} catch (final NoSuchFieldException ex) {
			try {
				// This hacky workaround is for newer jdk versions 11+?
				final Method fieldGetter = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
				fieldGetter.setAccessible(true);
				final Field[] fields = (Field[]) fieldGetter.invoke(Field.class, false);
				for (final Field f : fields)
					if (f.getName().equals("modifiers")) {
						field_modifiers = f;
						field_modifiers.setAccessible(true);
						break;
					}
			} catch (final Exception e) {
				throw new FoException(e);
			}
		}
		if (field_modifiers == null)
			throw new FoException("Unable to init the modifiers Field.");
	}

	public static Field makeNonFinal(Field field) throws IllegalArgumentException, IllegalAccessException {
		final int mods = field.getModifiers();
		if (Modifier.isFinal(mods))
			field_modifiers.set(field, mods & ~Modifier.FINAL);
		return field;
	}

	public static void setFinal(Object obj, Field field, Object newValue)
			throws IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		field = makeNonFinal(field);
		field.set(obj, newValue);
	}

}