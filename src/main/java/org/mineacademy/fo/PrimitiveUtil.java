package org.mineacademy.fo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PrimitiveUtil {

	private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE;
	private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_TYPE;

	/**
	 * Returns an immutable set of all nine primitive types (including {@code
	 * void}). Note that a simpler way to test whether a {@code Class} instance is a member of this
	 * set is to call {@link Class#isPrimitive}.
	 *
	 * @since 3.0
	 */
	public static Set<Class<?>> allPrimitiveTypes() {
		return PRIMITIVE_TO_WRAPPER_TYPE.keySet();
	}

	/**
	 * Returns an immutable set of all nine primitive-wrapper types (including {@link Void}).
	 *
	 * @since 3.0
	 */
	public static Set<Class<?>> allWrapperTypes() {
		return WRAPPER_TO_PRIMITIVE_TYPE.keySet();
	}

	/**
	 * Returns {@code true} if {@code type} is one of the nine primitive-wrapper types, such as
	 * {@link Integer}.
	 *
	 * @see Class#isPrimitive
	 */
	public static boolean isWrapperType(@NonNull final Class<?> type) {
		return WRAPPER_TO_PRIMITIVE_TYPE.containsKey(type);
	}

	/**
	 * Returns the corresponding wrapper type of {@code type} if it is a primitive type; otherwise
	 * returns {@code type} itself. Idempotent.
	 *
	 * <pre>
	 *     wrap(int.class) == Integer.class
	 *     wrap(Integer.class) == Integer.class
	 *     wrap(String.class) == String.class
	 * </pre>
	 */
	public static <T> Class<T> wrap(@NonNull final Class<T> type) {
		final Class<T> wrapped = (Class<T>) PRIMITIVE_TO_WRAPPER_TYPE.get(type);

		return wrapped == null ? type : wrapped;
	}

	/**
	 * Returns the corresponding primitive type of {@code type} if it is a wrapper type; otherwise
	 * returns {@code type} itself. Idempotent.
	 *
	 * <pre>
	 *     unwrap(Integer.class) == int.class
	 *     unwrap(int.class) == int.class
	 *     unwrap(String.class) == String.class
	 * </pre>
	 */
	public static <T> Class<T> unwrap(@NonNull final Class<T> type) {
		final Class<T> unwrapped = (Class<T>) WRAPPER_TO_PRIMITIVE_TYPE.get(type);

		return unwrapped == null ? type : unwrapped;
	}

	private static void add(final Map<Class<?>, Class<?>> forward, final Map<Class<?>, Class<?>> backward, final Class<?> key, final Class<?> value) {
		forward.put(key, value);
		backward.put(value, key);
	}

	static {
		final Map<Class<?>, Class<?>> primToWrap = new HashMap<>(16);
		final Map<Class<?>, Class<?>> wrapToPrim = new HashMap<>(16);

		add(primToWrap, wrapToPrim, boolean.class, Boolean.class);
		add(primToWrap, wrapToPrim, byte.class, Byte.class);
		add(primToWrap, wrapToPrim, char.class, Character.class);
		add(primToWrap, wrapToPrim, double.class, Double.class);
		add(primToWrap, wrapToPrim, float.class, Float.class);
		add(primToWrap, wrapToPrim, int.class, Integer.class);
		add(primToWrap, wrapToPrim, long.class, Long.class);
		add(primToWrap, wrapToPrim, short.class, Short.class);
		add(primToWrap, wrapToPrim, void.class, Void.class);

		PRIMITIVE_TO_WRAPPER_TYPE = primToWrap;
		WRAPPER_TO_PRIMITIVE_TYPE = wrapToPrim;
	}
}
