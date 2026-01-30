package org.mineacademy.fo.remain.nbt;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

class DefaultMethodInvoker {

	private static Method invokeDefaultMethod;

	static {
		try {
			invokeDefaultMethod = InvocationHandler.class.getDeclaredMethod("invokeDefault",
					Object.class, Method.class, Object[].class);
			invokeDefaultMethod.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			// we are in java 8, use the fallback
		}
	}

	/**
	 * Using reflections to access reflections, since some are still on java 8.
	 * @param srcInt
	 *
	 * @param target
	 * @param method
	 * @param args
	 * @return
	 */
	public static Object invokeDefault(Class<?> srcInt, Object target, Method method, Object[] args) {
		if (invokeDefaultMethod != null) { // java 9+
			try {
				return invokeDefaultMethod.invoke(null, target, method, args);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new NbtApiException("Error while trying to invoke a default method for Java 9+. " + target + " "
						+ method + " " + Arrays.toString(args), e);
			}
		} else {
			try {
				final Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
				constructor.setAccessible(true);
				return constructor.newInstance(srcInt).in(srcInt).unreflectSpecial(method, srcInt).bindTo(target)
						.invokeWithArguments(args);
			} catch (final Throwable e) {
				throw new NbtApiException("Error while trying to invoke a default method for Java 8. " + target + " "
						+ method + " " + Arrays.toString(args), e);
			}
		}
	}

}
