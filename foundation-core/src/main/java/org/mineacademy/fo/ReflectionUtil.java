package org.mineacademy.fo;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.MissingEnumException;
import org.mineacademy.fo.exception.ReflectionException;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * Utility class for various reflection methods.
 *
 * This is a platform-neutral class, which is extended by "ReflectionUtil" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtil {

	/**
	 * Compatible enum classes that fail gracefully so that the plugin loads even on legacy Minecraft
	 * versions where those types do not existe but are present in plugin's default settings.
	 */
	private static final Map<Class<? extends Enum<?>>, Map<String, V>> legacyEnumTypes = new HashMap<>();

	/**
	 * The legacy enum name translator used to translate legacy enums.
	 */
	@Setter
	private static LegacyEnumNameTranslator legacyEnumNameTranslator;

	/**
	 * The org.bukkit.Keyed class.
	 */
	private static Class<?> orgBukkitKeyed = null;

	/**
	 * Cache reflection lookup for performance purposes.
	 */
	private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, ReflectionData<?>> reflectionDataCache = new ConcurrentHashMap<>();
	private static final Collection<String> classNameGuard = ConcurrentHashMap.newKeySet();
	private static final Map<Class<?>, Method> enumClassCache = new HashMap<>();

	/**
	 * Maps primitive <code>Class</code>es to their corresponding wrapper <code>Class</code>.
	 */
	private static final Map<Class<?>, Class<?>> primitiveToWrapperMap = new HashMap<>();

	/**
	 * Maps wrapper <code>Class</code>es to their corresponding primitive types.
	 */
	private static final Map<Class<?>, Class<?>> wrapperToPrimitiveMap = new HashMap<>();

	static {
		primitiveToWrapperMap.put(Boolean.TYPE, Boolean.class);
		primitiveToWrapperMap.put(Byte.TYPE, Byte.class);
		primitiveToWrapperMap.put(Character.TYPE, Character.class);
		primitiveToWrapperMap.put(Short.TYPE, Short.class);
		primitiveToWrapperMap.put(Integer.TYPE, Integer.class);
		primitiveToWrapperMap.put(Long.TYPE, Long.class);
		primitiveToWrapperMap.put(Double.TYPE, Double.class);
		primitiveToWrapperMap.put(Float.TYPE, Float.class);
		primitiveToWrapperMap.put(Void.TYPE, Void.TYPE);

		for (final Class<?> primitiveClass : primitiveToWrapperMap.keySet()) {
			final Class<?> wrapperClass = primitiveToWrapperMap.get(primitiveClass);

			if (!primitiveClass.equals(wrapperClass))
				wrapperToPrimitiveMap.put(wrapperClass, primitiveClass);
		}

		try {
			orgBukkitKeyed = Class.forName("org.bukkit.Keyed");

		} catch (final ClassNotFoundException e) {
			// Ignore
		}
	}

	/**
	 * Add a legacy enum type that is not present in older Minecraft versions.
	 *
	 * @param enumClass
	 * @param map
	 */
	public static void addLegacyEnumType(Class<? extends Enum<?>> enumClass, Map<String, V> map) {
		legacyEnumTypes.put(enumClass, map);
	}

	/**
	 * Return a constructor for the given fully qualified class path such as
	 * org.mineacademy.boss.BossPlugin.
	 *
	 * @param classPath
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructor(@NonNull final String classPath, final Class<?>... params) {
		final Class<?> clazz = lookupClass(classPath);

		return getConstructor(clazz, params);
	}

	/**
	 * Return a constructor for the given class.
	 *
	 * @param <T>
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static <T> Constructor<T> getConstructor(@NonNull final Class<T> clazz, final Class<?>... params) {
		try {
			if (reflectionDataCache.containsKey(clazz))
				return (Constructor<T>) reflectionDataCache.get(clazz).getConstructor(params);

			Constructor<T> constructor;

			try {
				constructor = clazz.getConstructor(params);

			} catch (final NoSuchMethodException err) {
				constructor = clazz.getDeclaredConstructor(params);
			}

			constructor.setAccessible(true);

			return constructor;

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not get constructor of " + clazz + " with parameters " + CommonCore.join(params));
		}
	}

	/**
	 * Get the field content.
	 *
	 * @param <T>
	 * @param instance
	 * @param field
	 * @return
	 */
	public static <T> T getFieldContent(final Object instance, final String field) {
		return getFieldContent(instance.getClass(), field, instance);
	}

	/*
	 * Get the field content in the given class
	 */
	private static <T> T getFieldContent(Class<?> clazz, final String fieldName, final Object instance) {
		final String originalClassName = new String(clazz.getName());

		do
			// note: getDeclaredFields() fails if any of the fields are classes that cannot be loaded
			for (final Field field : clazz.getDeclaredFields())
				if (field.getName().equals(fieldName))
					return (T) getFieldContent(instance, field);
		while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		throw new ReflectionException("No such field " + fieldName + " in " + originalClassName + " or its superclasses");
	}

	/**
	 * Get the field content.
	 *
	 * @param instance
	 * @param field
	 * @return
	 */
	public static Object getFieldContent(final Object instance, final Field field) {
		try {
			field.setAccessible(true);

			return field.get(instance);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not get field " + field.getName() + " in instance " + (instance != null ? instance : field).getClass().getSimpleName());
		}
	}

	/**
	 * Get all fields from the class and its super classes.
	 *
	 * @param clazz
	 * @return
	 */
	public static Field[] getFields(@NonNull Class<?> clazz) {
		final List<Field> list = new ArrayList<>();

		try {
			do
				list.addAll(Arrays.asList(clazz.getDeclaredFields()));
			while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		} catch (final NullPointerException ex) {
			// Pass through - such as interfaces or object itself throw this
		}

		return list.toArray(new Field[0]);
	}

	/**
	 * Gets the declared field in class by its name.
	 *
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getField(final Class<?> clazz, final String fieldName) {
		try {

			if (reflectionDataCache.containsKey(clazz))
				return reflectionDataCache.get(clazz).getDeclaredField(fieldName);

			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);

			return field;

		} catch (final ReflectiveOperationException ex) {
			CommonCore.sneaky(ex);
		}

		return null;
	}

	/**
	 * Set a declared field to the given value.
	 *
	 * @param instance
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setField(@NonNull final Object instance, final String fieldName, final Object fieldValue) {
		final Field field = getField(instance.getClass(), fieldName);

		try {
			field.set(instance, fieldValue);

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Get the static field content.
	 *
	 * @param <T>
	 * @param clazz
	 * @param field
	 * @return
	 */
	public static <T> T getStaticFieldContent(@NonNull final Class<?> clazz, final String field) {
		return getFieldContent(clazz, field, null);
	}

	/**
	 * Set the static field to the given value.
	 *
	 * @param clazz
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setStaticField(@NonNull final Class<?> clazz, final String fieldName, final Object fieldValue) {
		try {
			final Field field = getField(clazz, fieldName);

			field.set(null, fieldValue);

		} catch (final Throwable t) {
			throw new FoException(t, "Could not set " + fieldName + " in " + clazz + " to " + fieldValue);
		}
	}

	/**
	 * Get a declared class method.
	 *
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 */
	public static Method getMethod(Class<?> clazz, final String methodName, Class<?>... args) {
		final Class<?> originalClass = clazz;

		while (!clazz.equals(Object.class))
			try {
				final Method method = clazz.getDeclaredMethod(methodName, args);
				method.setAccessible(true);

				return method;

			} catch (final NoSuchMethodException ex) {
				clazz = clazz.getSuperclass();

			} catch (final Throwable t) {
				throw new ReflectionException(t, "Error lookup up method " + methodName + " in class " + originalClass + " and her subclasses");
			}

		return null;
	}

	/**
	 * Invoke a static method.
	 *
	 * @param <T>
	 * @param cl
	 * @param methodName
	 * @param params
	 * @return
	 */
	public static <T> T invokeStatic(final Class<?> cl, final String methodName, final Object... params) {
		return invokeStatic(getMethod(cl, methodName), params);
	}

	/**
	 * Invoke a static method.
	 *
	 * @param <T>
	 * @param method
	 * @param params
	 * @return
	 */
	public static <T> T invokeStatic(@NonNull final Method method, final Object... params) {
		try {
			ValidCore.checkBoolean(Modifier.isStatic(method.getModifiers()),
					"Method " + method.getName() + " must be static to be invoked through invokeStatic with params: " + CommonCore.join(params));

			return (T) method.invoke(null, params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not invoke " + method + " with params " + Arrays.toString(params));
		}
	}

	/**
	 * Invoke a non static method.
	 *
	 * @param <T>
	 * @param methodName
	 * @param instance
	 * @param params
	 * @return
	 */
	public static <T> T invoke(@NonNull final String methodName, @NonNull final Object instance, final Object... params) {
		final List<Class<?>> args = CommonCore.convertArrayToList(params, Object::getClass);
		final Method method = getMethod(instance.getClass(), methodName, args.toArray(new Class<?>[args.size()]));
		ValidCore.checkNotNull(method, "Unable to invoke " + methodName + "(" + CommonCore.join(params) + ") because such method was not found in " + instance.getClass());

		return invoke(method, instance, params);
	}

	/**
	 * Invoke a non static method.
	 *
	 * @param <T>
	 * @param method
	 * @param instance
	 * @param params
	 * @return
	 */
	public static <T> T invoke(final Method method, final Object instance, final Object... params) {
		ValidCore.checkNotNull(method, "Cannot invoke a null method for " + (instance == null ? "static" : instance.getClass().getSimpleName() + "") + " instance '" + instance + "' " + " with params " + CommonCore.join(params));

		try {
			return (T) method.invoke(instance, params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not invoke method " + method + " on instance " + instance + " with params " + CommonCore.join(params));
		}
	}

	/**
	 * Makes a new instance of a class by its full path name.
	 *
	 * @param <T>
	 * @param classPath
	 * @return
	 */
	public static <T> T instantiate(final String classPath) {
		final Class<T> clazz = lookupClass(classPath);

		return instantiate(clazz);
	}

	/**
	 * Makes a new instance of a class.
	 *
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz) {
		try {
			final Constructor<T> constructor;

			if (reflectionDataCache.containsKey(clazz))
				constructor = ((ReflectionData<T>) reflectionDataCache.get(clazz)).getDeclaredConstructor();
			else
				constructor = ReflectionUtil.getConstructor(clazz);

			return constructor.newInstance();

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not make instance of: " + clazz);
		}
	}

	/**
	 * Makes a new instance of a class with arguments.
	 *
	 * @param <T>
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz, final Object... params) {
		try {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Object param : params) {
				ValidCore.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);
				final Class<?> paramClass = param.getClass();

				classes.add(paramClass.isPrimitive() ? wrapperToPrimitive(paramClass) : paramClass);
			}

			final Class<?>[] paramArr = classes.toArray(new Class<?>[0]);
			final Constructor<T> constructor;

			if (reflectionDataCache.containsKey(clazz))
				constructor = ((ReflectionData<T>) reflectionDataCache.get(clazz)).getDeclaredConstructor(paramArr);
			else {
				classCache.put(clazz.getCanonicalName(), clazz);

				constructor = (Constructor<T>) reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new).getDeclaredConstructor(paramArr);
			}

			constructor.setAccessible(true);

			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not make instance of: " + clazz);
		}
	}

	/**
	 * Attempts to create a new instance from the given constructor and parameters
	 *
	 * @param <T>
	 * @param constructor
	 * @param params
	 * @return
	 */
	public static <T> T instantiate(final Constructor<T> constructor, final Object... params) {
		try {
			constructor.setAccessible(true);

			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not make new instance of " + constructor + " with params: " + CommonCore.join(params));
		}
	}

	/**
	 * Return true if the given absolute class path is available,
	 * useful for checking for older MC versions for classes such as org.bukkit.entity.Phantom.
	 *
	 * @param path
	 * @return
	 */
	public static boolean isClassAvailable(final String path) {
		try {
			if (classCache.containsKey(path))
				return true;

			Class.forName(path);

			return true;

		} catch (final Throwable t) {
			return false;
		}
	}

	/**
	 * Wrapper for Class.forName.
	 *
	 * @param <T>
	 * @param path
	 * @return
	 */
	public static <T> Class<T> lookupClass(final String path) {
		if (classCache.containsKey(path))
			return (Class<T>) classCache.get(path);

		if (classNameGuard.contains(path)) {
			while (classNameGuard.contains(path)) {
				// Wait for other thread
			}

			return lookupClass(path); // Re run method to see if the cached value now exists.
		}

		try {
			classNameGuard.add(path);

			final Class<?> clazz = Class.forName(path);

			classCache.put(path, clazz);
			reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new);

			return (Class<T>) clazz;

		} catch (final ClassNotFoundException ex) {
			throw new ReflectionException("Could not find class: " + path);

		} finally {
			classNameGuard.remove(path);
		}
	}

	/**
	 * Attempts to find an enum, throwing formatted error showing all available
	 * values if not found.
	 *
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum.
	 *
	 * If the given type is known to be found in new MC versions, we may return null
	 * instead of throwing an error. This is to prevent default configs containing
	 * this enum from crashing the plugin when loaded on legacy MC version.
	 *
	 * @param <E> the class, ideally Enum or OldEnum as we will try invoking valueOf,
	 *           fromName, fromKey and fromString methods on it
	 * @param typeOf
	 * @param name
	 *
	 * @return the enum or error with exceptions, see above
	 */
	public static <E> E lookupEnum(final Class<E> typeOf, final String name) {
		return lookupEnum(typeOf, name, typeOf.getSimpleName() + " value '" + name + "' is not found! Available: {available}");
	}

	/**
	 * Attempts to find an enum, throwing formatted error showing all available
	 * values if not found.
	 *
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum.
	 *
	 * If the given type is known to be found in new MC versions, we may return null
	 * instead of throwing an error. This is to prevent default configs containing
	 * this enum from crashing the plugin when loaded on legacy MC version.
	 *
	 * @param <E> the class, ideally Enum or OldEnum as we will try invoking valueOf,
	 *           fromName, fromKey and fromString methods on it
	 * @param typeOf
	 * @param name
	 * @param errorMessage
	 *
	 * @throws MissingEnumException if the enum is not found and is not inside {@link #addLegacyEnumType(Class, Map)}
	 * @return the enum or error with exceptions, see above
	 */
	public static <E> E lookupEnum(final Class<E> typeOf, String name, final String errorMessage) {
		name = name.toUpperCase().replace(" ", "_");

		final E result = lookupEnumSilent(typeOf, name);

		if (result == null) {

			// Return null for legacy types instead of throwing an exception.
			final Map<String, V> legacyMap = legacyEnumTypes.get(typeOf);

			if (legacyMap != null) {
				final V since = legacyMap.get(name);

				if (since != null && MinecraftVersion.olderThan(since))
					return null;
			}

			final String available = typeOf.isEnum() ? CommonCore.join(typeOf.getEnumConstants()) : Lang.plain("unknown");
			throw new MissingEnumException(name, errorMessage.replace("{available}", available));
		}

		return result;
	}

	/**
	 * Wrapper for Enum.valueOf without throwing an exception.
	 *
	 * @param <E> the class, ideally Enum or OldEnum as we will try invoking valueOf,
	 *           fromName, fromKey and fromString methods on it
	 * @param typeOf
	 * @param name
	 * @return the enum, or null if not exists
	 */
	public static <E> E lookupEnumSilent(@NonNull Class<E> typeOf, @NonNull String name) {
		name = name.toUpperCase().replace(" ", "_");

		try {
			// Some compatibility workaround for plugins having these values in their default config
			// to prevents malfunction on plugin's first load when loaded on older Minecraft version.
			if (legacyEnumNameTranslator != null)
				name = legacyEnumNameTranslator.translateName(typeOf, name);

			Method method = enumClassCache.get(typeOf);

			if (method == null)
				try {
					final Method fromKey = typeOf.getDeclaredMethod("fromKey", String.class);

					if (Modifier.isPublic(fromKey.getModifiers()) && Modifier.isStatic(fromKey.getModifiers()))
						method = fromKey;

				} catch (final NoSuchMethodException t) {
				}

			if (method == null)
				try {
					final Method valueOf = typeOf.getDeclaredMethod("valueOf", String.class);

					if (Modifier.isPublic(valueOf.getModifiers()) && Modifier.isStatic(valueOf.getModifiers()))
						method = valueOf;

				} catch (final NoSuchMethodException t) {
				}

			// Only invoke fromName from non-Bukkit API since this gives unexpected results.
			if (method == null && !typeOf.getName().contains("org.bukkit"))
				try {
					final Method fromName = typeOf.getDeclaredMethod("fromName", String.class);

					if (Modifier.isPublic(fromName.getModifiers()) && Modifier.isStatic(fromName.getModifiers()))
						method = fromName;

				} catch (final NoSuchMethodException t) {
				}

			if (method != null)
				try {
					final E value = (E) method.invoke(null, name);

					// Cache after method invocation to ensure it went right.
					enumClassCache.put(typeOf, method);

					if (value != null)
						return value;

				} catch (final InvocationTargetException ex) {
					if (ex.getCause() instanceof IllegalArgumentException)
						return null;

					throw ex;
				}

			return null;

		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new FoException(ex, "Error invocating enum finding method for " + typeOf.getSimpleName() + " from string " + name);
		}
	}

	/**
	 * Check if the class of the instance is either an enum or implements the `Keyed` interface.
	 *
	 * @param instance the instance's class to check
	 * @return {@code true} if the class is an enum or implements the `Keyed` interface, {@code false} otherwise
	 */
	public static boolean isEnumLike(Object instance) {
		return isEnumLike(instance.getClass());
	}

	/**
	 * Check if a class is either an enum or implements the `Keyed` interface.
	 *
	 * @param clazz the class to check
	 * @return {@code true} if the class is an enum or implements the `Keyed` interface, {@code false} otherwise
	 */
	public static boolean isEnumLike(Class<?> clazz) {
		return clazz.isEnum() || (orgBukkitKeyed != null && orgBukkitKeyed.isAssignableFrom(clazz));
	}

	/**
	 * Get all classes in the plugin file.
	 *
	 * @param <T>
	 * @param pluginFile
	 * @param extendingClass
	 * @return
	 */
	@SneakyThrows
	public static <T> TreeSet<Class<T>> getClasses(@NonNull File pluginFile, Class<T> extendingClass) {

		final TreeSet<Class<T>> classes = new TreeSet<>(Comparator.comparing(Class::toString));

		try (final JarFile jarFile = new JarFile(pluginFile)) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();

				if (name.endsWith(".class")) {
					name = name.replaceFirst("\\.class", "").replace("/", ".");

					Class<?> clazz = null;

					try {
						clazz = Class.forName(name, false, Platform.getPlugin().getPluginClassLoader());

						if (extendingClass == null || (extendingClass.isAssignableFrom(clazz) && clazz != extendingClass))
							classes.add((Class<T>) clazz);

					} catch (final Throwable throwable) {

						if (extendingClass != null && (clazz != null && extendingClass.isAssignableFrom(clazz)) && clazz != extendingClass)
							CommonCore.log("Unable to load class '" + name + "' due to error: " + throwable);

						continue;
					}
				}
			}
		}

		return classes;
	}

	// ------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------

	/**
	 * <p>Converts the specified wrapper class to its corresponding primitive
	 * class.</p>
	 *
	 * <p>This method is the counter part of <code>primitiveToWrapper()</code>.
	 * If the passed in class is a wrapper class for a primitive type, this
	 * primitive type will be returned (e.g. <code>Integer.TYPE</code> for
	 * <code>Integer.class</code>). For other classes, or if the parameter is
	 * <b>null</b>, the return value is <b>null</b>.</p>
	 *
	 * @param cls the class to convert, may be <b>null</b>
	 * @return the corresponding primitive type if <code>cls</code> is a
	 * wrapper class, <b>null</b> otherwise
	 *
	 * @author Apache Commons ClassUtils
	 */
	public static Class<?> wrapperToPrimitive(Class<?> cls) {
		return wrapperToPrimitiveMap.get(cls);
	}

	// ------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------

	/**
	 * Helps to translate legacy names into something the current server can recognize
	 */
	public interface LegacyEnumNameTranslator {

		/**
		 * Return the translated legacy name for the given enum type and name.
		 *
		 * @param <E> the class, ideally Enum or OldEnum as we will try invoking valueOf,
		 *           fromName, fromKey and fromString methods on it
		 * @param enumType the class of the enum
		 * @param name the string enum name
		 * @return
		 */
		<E> String translateName(Class<E> enumType, String name);
	}

	/**
	 * A utility class that caches constructors and fields using reflection for a specific class type.
	 *
	 * This class allows for efficient access to constructors and fields by caching them in memory,
	 * reducing the need for multiple expensive reflection lookups.
	 *
	 * @param <T>
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class ReflectionData<T> {

		private final Class<T> clazz;

		private final Map<Integer, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
		private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
		private final Collection<String> fieldGuard = ConcurrentHashMap.newKeySet();
		private final Collection<Integer> constructorGuard = ConcurrentHashMap.newKeySet();

		public void cacheConstructor(final Constructor<T> constructor) {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Class<?> param : constructor.getParameterTypes()) {
				ValidCore.checkNotNull(param, "Argument cannot be null when instatiating " + this.clazz);

				classes.add(param);
			}

			this.constructorCache.put(Arrays.hashCode(classes.toArray(new Class<?>[0])), constructor);
		}

		public Constructor<T> getDeclaredConstructor(final Class<?>... paramTypes) throws NoSuchMethodException {
			final Integer hashCode = Arrays.hashCode(paramTypes);

			if (this.constructorCache.containsKey(hashCode))
				return (Constructor<T>) this.constructorCache.get(hashCode);

			if (this.constructorGuard.contains(hashCode)) {
				while (this.constructorGuard.contains(hashCode)) {

				} // Wait for other thread;
				return this.getDeclaredConstructor(paramTypes);
			}

			this.constructorGuard.add(hashCode);

			try {
				final Constructor<T> constructor = this.clazz.getDeclaredConstructor(paramTypes);

				this.cacheConstructor(constructor);

				return constructor;

			} finally {
				this.constructorGuard.remove(hashCode);
			}
		}

		public Constructor<T> getConstructor(final Class<?>... paramTypes) throws NoSuchMethodException {
			final Integer hashCode = Arrays.hashCode(paramTypes);

			if (this.constructorCache.containsKey(hashCode))
				return (Constructor<T>) this.constructorCache.get(hashCode);

			if (this.constructorGuard.contains(hashCode)) {
				while (this.constructorGuard.contains(hashCode)) {
					// Wait for other thread;
				}

				return this.getConstructor(paramTypes);
			}

			this.constructorGuard.add(hashCode);

			try {
				final Constructor<T> constructor = this.clazz.getConstructor(paramTypes);

				this.cacheConstructor(constructor);

				return constructor;

			} finally {
				this.constructorGuard.remove(hashCode);
			}
		}

		public void cacheField(final Field field) {
			this.fieldCache.put(field.getName(), field);
		}

		public Field getDeclaredField(final String name) throws NoSuchFieldException {

			if (this.fieldCache.containsKey(name))
				return this.fieldCache.get(name);

			if (this.fieldGuard.contains(name)) {
				while (this.fieldGuard.contains(name)) {
				}

				return this.getDeclaredField(name);
			}

			this.fieldGuard.add(name);

			try {
				final Field field = this.clazz.getDeclaredField(name);

				this.cacheField(field);

				return field;

			} finally {
				this.fieldGuard.remove(name);
			}
		}
	}
}
