package org.mineacademy.fo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Ref;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.FoException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for various reflection methods
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtil {

	private static final Map<String, Class<?>> classCache = new HashMap<>();
	private static final Map<Class<?>, ReflectionData<?>> reflectionDataCache = new HashMap<>();

	private static final class ReflectionData<T> {
		private final Class<T> clazz;

		ReflectionData(final Class<T> clazz) {
			this.clazz = clazz;
		}

		private final Map<String, Collection<Method>> methodCache = new HashMap<>();
		private final Map<Integer, Constructor<?>> constructorCache = new HashMap<>();
		private final Map<String, Field> fieldCache = new HashMap<>();

		public Constructor<T> getDeclaredConstructor(Class<?>... paramTypes) throws NoSuchMethodException {
			return getDeclaredConstructor(false, paramTypes);
		}

		public void cacheConstructor(final Constructor<T> constructor) {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Class<?> param : constructor.getParameterTypes()) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);

				classes.add(param.isPrimitive() ? ClassUtils.wrapperToPrimitive(param) : param);
			}
		}

		@SuppressWarnings("unchecked")
		public Constructor<T> getDeclaredConstructor(final boolean cache, final Class<?>... paramTypes) throws NoSuchMethodException {
			final int hashCode = Arrays.hashCode(paramTypes);
			if (constructorCache.containsKey(hashCode)) {
				return (Constructor<T>) constructorCache.get(hashCode);
			}
			final Constructor<T> constructor = clazz.getDeclaredConstructor(paramTypes);
			if (cache) {
				constructorCache.put(hashCode, constructor);
			}
			return constructor;
		}

		@SuppressWarnings("unchecked")
		public Constructor<T> getConstructor(final boolean cache, final Class<?>... paramTypes) throws NoSuchMethodException {
			final int hashCode = Arrays.hashCode(paramTypes);
			if (constructorCache.containsKey(hashCode)) {
				return (Constructor<T>) constructorCache.get(hashCode);
			}
			final Constructor<T> constructor = clazz.getConstructor(paramTypes);
			if (cache) {
				constructorCache.put(hashCode, constructor);
			}
			return constructor;
		}

		@SuppressWarnings("unchecked")
		public Optional<Constructor<T>> getCachedConstructor(Class<?>... paramTypes) {
			return Optional.ofNullable((Constructor<T>) constructorCache.get(Arrays.hashCode(paramTypes)));
		}

		public void cacheMethod(final Method method) {
			methodCache.computeIfAbsent(method.getName(), (unused) -> new HashSet<>()).add(method);
		}

		public Optional<Method> getCachedMethod(final String name, final Class<?>... paramTypes) {
			for (final Method m : methodCache.get(name)) {
				if (Arrays.equals(m.getParameterTypes(), paramTypes)) {
					return Optional.of(m);
				}
			}
			return Optional.empty();
		}

		public Method getDeclaredMethod(final String name, final Class<?>... paramTypes) throws NoSuchMethodException {
			return getDeclaredMethod(name, false, paramTypes);
		}

		public Method getMethod(final String name, final Class<?>... paramTypes) throws NoSuchMethodException {
			return getMethod(name, false, paramTypes);
		}


		public Method getDeclaredMethod(final String name, final boolean cache, final Class<?>... paramTypes) throws NoSuchMethodException {
			if (methodCache.containsKey(name)) {
				final Collection<Method> methods = methodCache.get(name);
				for (final Method method : methods) {
					if (Arrays.equals(paramTypes, method.getParameterTypes())) {
						return method;
					}
				}
			}
			final Method method = clazz.getDeclaredMethod(name, paramTypes);
			if (cache) {
				methodCache.computeIfAbsent(name, (unused) -> new HashSet<>()).add(method);
			}
			return method;
		}

		public Method getMethod(final String name, final boolean cache, final Class<?>... paramTypes) throws NoSuchMethodException {
			if (methodCache.containsKey(name)) {
				final Collection<Method> methods = methodCache.get(name);
				for (final Method method : methods) {
					if (Arrays.equals(paramTypes, method.getParameterTypes())) {
						return method;
					}
				}
			}
			final Method method = clazz.getMethod(name, paramTypes);
			if (cache) {
				methodCache.computeIfAbsent(name, (unused) -> new HashSet<>()).add(method);
			}
			return method;
		}

		public void cacheField(final Field field) {
			fieldCache.put(field.getName(), field);
		}

		public Optional<Field> getCachedField(final String name) {
			return Optional.ofNullable(fieldCache.get(name));
		}


		public Field getDeclaredField(final String name) throws NoSuchFieldException {
			return getDeclaredField(name, false);
		}

		public Field getField(final String name) throws NoSuchFieldException {
			return getField(name, false);
		}

		public Field getDeclaredField(final String name, final boolean cache) throws NoSuchFieldException{
			if (fieldCache.containsKey(name)) {
				return fieldCache.get(name);
			}
			final Field field = clazz.getDeclaredField(name);
			if (cache) {
				fieldCache.put(name, field);
			}
			return field;
		}

		public Field getField(final String name, final boolean cache) throws NoSuchFieldException {
			if (fieldCache.containsKey(name)) {
				return fieldCache.get(name);
			}
			final Field field = clazz.getField(name);
			if (cache) {
				fieldCache.put(name, field);
			}
			return field;
		}

	}

	/**
	 * The full package name for NMS
	 */
	public static final String NMS = "net.minecraft.server";

	/**
	 * The package name for Craftbukkit
	 */
	public static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";

	/**
	 * Find a class in net.minecraft.server package, adding the version
	 * automatically
	 *
	 * @param name
	 * @return
	 */
	public static Class<?> getNMSClass(final String name) {
		return ReflectionUtil.lookupClass(NMS + "." + MinecraftVersion.getServerVersion() + "." + name);
	}

	/**
	 * Find a class in net.minecraft.server package, adding the version
	 * automatically
	 *
	 * @param cache Whether the resultant class should be cached into memory.
	 * @param name
	 * @return
	 */
	public static Class<?> getNMSClass(final boolean cache, final String name) {
		return ReflectionUtil.lookupClass(cache, NMS + "." + MinecraftVersion.getServerVersion() + "." + name);
	}

	/**
	 * Find a class in org.bukkit.craftbukkit package, adding the version
	 * automatically
	 *
	 * @param name
	 * @return
	 */
	public static Class<?> getOBCClass(final String name) {
		return ReflectionUtil.lookupClass( CRAFTBUKKIT + "." + MinecraftVersion.getServerVersion() + "." + name);
	}

	/**
	 * Find a class in org.bukkit.craftbukkit package, adding the version
	 * automatically
	 *
	 * @param cache Whether the resultant class should be cached into memory.
	 * @param name
	 * @return
	 */
	public static Class<?> getOBCClass(final boolean cache, final String name) {
		return ReflectionUtil.lookupClass( cache,CRAFTBUKKIT + "." + MinecraftVersion.getServerVersion() + "." + name);
	}

	/**
	 * Return a constructor for the given NMS class. We prepend the class name
	 * with the {@link #NMS} so you only have to give in the name of the class.
	 *
	 * @param nmsClass
	 * @param params
	 * @return
	 */
	public static Constructor<?> getNMSConstructor(@NonNull final String nmsClass, final Class<?>... params) {
		return getConstructor(getNMSClass(nmsClass), params);
	}

	/**
	 * Return a constructor for the given NMS class. We prepend the class name
	 * with the {@link #NMS} so you only have to give in the name of the class.
	 *
	 * @param cache Whether the returned value should be cached.
	 */
	public static Constructor<?> getNMSConstructor(final boolean cache, @NonNull final String nmsClass, final Class<?>... params) {
		return getConstructor(cache, getNMSClass(nmsClass), params);
	}

	/**
	 * Return a constructor for the given OBC class. We prepend the class name
	 * with the {@link #OBC} so you only have to give in the name of the class.
	 *
	 * @param obcClass
	 * @param params
	 * @return
	 */
	public static Constructor<?> getOBCConstructor(@NonNull final String obcClass, final Class<?>... params) {
		return getConstructor(getOBCClass(obcClass), params);
	}

	/**
	 * Return a constructor for the given OBC class. We prepend the class name
	 * with the {@link #OBC} so you only have to give in the name of the class.
	 *
	 * @param cache Whether the returned value should be cached for later use.
	 */
	public static Constructor<?> getOBCConstructor(final boolean cache, @NonNull final String obcClass, final Class<?>... params) {
		return getConstructor(cache, getOBCClass(obcClass), params);
	}

	/**
	 * Return a constructor for the given fully qualified class path such as
	 * org.mineacademy.boss.BossPlugin
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
	 * Return a constructor for the given fully qualified class path such as
	 * org.mineacademy.boss.BossPlugin
	 *
	 * @param cache Whether the returned constructor should be cached into memory.
	 */
	public static Constructor<?> getConstructor(final boolean cache, @NonNull final String classPath, final Class<?>... params) {
		final Class<?> clazz = lookupClass(cache, classPath);

		return getConstructor(cache, clazz, params);
	}

	/**
	 * Return a constructor for the given class
	 *
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructor(@NonNull final Class<?> clazz, final Class<?>... params) {
		return getConstructor(false, clazz, params);
	}

	/**
	 * Return a constructor for the given class
	 *
	 * @param cache Whether the returned constructor should be cached into memory.
	 */
	public static Constructor<?> getConstructor(final boolean cache, @NonNull final Class<?> clazz, final Class<?>... params) {
		try {
			if (reflectionDataCache.containsKey(clazz)) {
				return reflectionDataCache.get(clazz).getConstructor(cache, params);
			}
			final Constructor<?> constructor = clazz.getConstructor(params);
			constructor.setAccessible(true);

			return constructor;

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not get constructor of " + clazz + " with parameters " + Common.joinToString(params));
		}
	}

	/**
	 * Get the field content
	 *
	 * @param instance
	 * @param field
	 * @return
	 */
	public static <T> T getFieldContent(final Object instance, final String field) {
		return getFieldContent(instance.getClass(), field, instance);
	}

	/**
	 * Get the field content
	 *
	 * @param <T>
	 * @param clazz
	 * @param field
	 * @param instance
	 * @param type
	 * @return
	 */
	public static <T> T getFieldContent(Class<?> clazz, final String field, final Object instance) {
		final String originalClassName = clazz.getSimpleName();

		do
			// note: getDeclaredFields() fails if any of the fields are classes that cannot be loaded
			for (final Field f : clazz.getDeclaredFields())
				if (f.getName().equals(field))
					return (T) getFieldContent(f, instance);
		while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		throw new ReflectionException("No such field " + field + " in " + originalClassName + " or its superclasses");
	}

	/**
	 * Get the field content
	 *
	 * @param field
	 * @param instance
	 * @return
	 */
	public static Object getFieldContent(final Field field, final Object instance) {
		try {
			field.setAccessible(true);

			return field.get(instance);

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not get field " + field.getName() + " in instance " + (instance != null ? instance : field).getClass().getSimpleName());
		}
	}

	/**
	 * Get all fields from the class and its super classes
	 *
	 * @param clazz
	 * @return
	 */
	public static Field[] getAllFields(Class<?> clazz) {
		final List<Field> list = new ArrayList<>();

		do
			list.addAll(Arrays.asList(clazz.getDeclaredFields()));
		while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		return list.toArray(new Field[0]);
	}

	/**
	 * Gets the declared field in class by its name
	 *
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getDeclaredField(final Class<?> clazz, final String fieldName) {
		return getDeclaredField(false, clazz, fieldName);
	}


	/**
	 * Gets the declared field in class by its name
	 *
	 * @param cache Whether the returned constructor should be cached into memory.
	 *
	 */
	public static Field getDeclaredField(final boolean cache, final Class<?> clazz, final String fieldName) {
		try {
			if (reflectionDataCache.containsKey(clazz)) {
				return reflectionDataCache.get(clazz).getDeclaredField(fieldName, cache);
			}
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);

			return field;

		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Set a declared field to the given value
	 *
	 * @param instance
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setDeclaredField(@NonNull final Object instance, final String fieldName, final Object fieldValue) {
		final Field field = getDeclaredField(instance.getClass(), fieldName);

		try {
			field.set(instance, fieldValue);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Convenience method for getting a static field content.
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
	 * Set the static field to the given value
	 *
	 * @param clazz
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setStaticField(@NonNull final Class<?> clazz, final String fieldName, final Object fieldValue) {
		try {
			final Field field = getDeclaredField(clazz, fieldName);

			field.set(null, fieldValue);

		} catch (final Throwable t) {
			throw new FoException(t, "Could not set " + fieldName + " in " + clazz + " to " + fieldValue);
		}
	}

	/**
	 * Set the static field to the given value
	 *
	 * @param object
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setStaticField(@NonNull final Object object, final String fieldName, final Object fieldValue) {
		try {
			final Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);

			field.set(object, fieldValue);

		} catch (final Throwable t) {
			throw new FoException(t, "Could not set " + fieldName + " in " + object + " to " + fieldValue);
		}
	}

	/**
	 * Gets a class method
	 *
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 */
	public static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... args) {
		for (final Method method : clazz.getMethods())
			if (method.getName().equals(methodName) && isClassListEqual(args, method.getParameterTypes())) {
				method.setAccessible(true);

				return method;
			}

		return null;
	}

	// Compares class lists
	private static boolean isClassListEqual(final Class<?>[] first, final Class<?>[] second) {
		if (first.length != second.length)
			return false;

		for (int i = 0; i < first.length; i++)
			if (first[i] != second[i])
				return false;

		return true;
	}

	/**
	 * Gets a class method
	 *
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 */
	public static Method getMethod(final Class<?> clazz, final String methodName, final Integer args) {
		for (final Method method : clazz.getMethods())
			if (method.getName().equals(methodName) && args.equals(new Integer(method.getParameterTypes().length))) {
				method.setAccessible(true);

				return method;
			}

		return null;
	}

	/**
	 * Gets a class method
	 *
	 * @param clazz
	 * @param methodName
	 * @return
	 */
	public static Method getMethod(final Class<?> clazz, final String methodName) {
		for (final Method method : clazz.getMethods())
			if (method.getName().equals(methodName)) {
				method.setAccessible(true);
				return method;
			}

		return null;
	}

	/**
	 * Invoke a static method
	 *
	 * @param <T>
	 * @param method
	 * @param params
	 * @return
	 */
	public static <T> T invokeStatic(final Class<?> cl, final String methodName, final Object... params) {
		return invokeStatic(getMethod(cl, methodName), params);
	}

	/**
	 * Invoke a static method
	 *
	 * @param <T>
	 * @param method
	 * @param params
	 * @return
	 */
	public static <T> T invokeStatic(final Method method, final Object... params) {
		try {
			return (T) method.invoke(null, params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Could not invoke static method " + method + " with params " + StringUtils.join(params), ex);
		}
	}

	/**
	 * Invoke a non static method
	 *
	 * @param <T>
	 * @param method
	 * @param instance
	 * @param params
	 * @return
	 */
	public static <T> T invoke(final String methodName, final Object instance, final Object... params) {
		return invoke(getMethod(instance.getClass(), methodName), instance, params);
	}

	/**
	 * Invoke a non static method
	 *
	 * @param <T>
	 * @param method
	 * @param instance
	 * @param params
	 * @return
	 */
	public static <T> T invoke(final Method method, final Object instance, final Object... params) {
		try {
			Valid.checkNotNull(method, "Method cannot be null for " + instance);

			return (T) method.invoke(instance, params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Could not invoke method " + method + " on instance " + instance + " with params " + StringUtils.join(params), ex);
		}
	}

	/**
	 * Makes a new instance of a class
	 *
	 * @param clazz
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz) {
		try {
			final Constructor<T> c;
			if (reflectionDataCache.containsKey(clazz)) {
				c = ((ReflectionData<T>) reflectionDataCache.get(clazz)).getDeclaredConstructor(false);
			} else {
				c = clazz.getDeclaredConstructor();
			}
			c.setAccessible(true);

			return c.newInstance();

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not make instance of: " + clazz, e);
		}
	}

	/**
	 * Makes a new instance of a class with arguments
	 *
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz, final Object... params) {
		return instantiate(false, clazz, params);
	}

	/**
	 * Makes a new instance of a class with arguments.
	 *
	 * @param cache Whether the constructor and class should be cached into memeory.
	 * @param clazz
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(final boolean cache, final Class<T> clazz, final Object... params) {
		try {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Object param : params) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);
				final Class<?> paramClass = param.getClass();

				classes.add(paramClass.isPrimitive() ? ClassUtils.wrapperToPrimitive(paramClass) : paramClass);
			}

			final Constructor<T> c;
			if (cache) {
				classCache.put(clazz.getCanonicalName(), clazz);
				c = (Constructor<T>) reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new).getDeclaredConstructor(true, classes.toArray(new Class<?>[0]));
			} else {
				c = clazz.getDeclaredConstructor(classes.toArray(new Class<?>[0]));
			}
			c.setAccessible(true);

			return c.newInstance(params);

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not make instance of: " + clazz, e);
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
			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not make new instance of " + constructor + " with params: " + Common.joinToString(params));
		}
	}

	/**
	 * Attempts to create a new instance from the given constructor and parameters
	 *
	 * @param cache Whether the returned constructor should be cached into memory.
	 *
	 */
	public static <T> T instantiate(final boolean cache, final Constructor<T> constructor, final Object... params) {
		if (cache) {
			final Class<T> clazz = constructor.getDeclaringClass();
			classCache.put(clazz.getCanonicalName(), clazz);
			((ReflectionData<T> )reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new)).cacheConstructor(constructor);
		}
		try {
			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not make new instance of " + constructor + " with params: " + Common.joinToString(params));
		}
	}

	/**
	 * Return true if the given absolute class path is available,
	 * useful for checking for older MC versions for classes such as org.bukkit.entity.Phantom
	 *
	 * @param path
	 * @return
	 */
	public static boolean isClassAvailable(final String path) {
		try {
			if (classCache.containsKey(path)) {
				return true;
			}
			Class.forName(path);
			return true;

		} catch (final Throwable t) {
			return false;
		}
	}


	public static boolean isClassAvailable(final boolean cacheIfPresent, final String path) {
		try {
			if (classCache.containsKey(path)) {
				return true;
			}
			final Class<?> clazz = Class.forName(path);
			if (cacheIfPresent) {
				classCache.put(path, clazz);
				reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new);
			}
			return true;

		} catch (final Throwable t) {
			return false;
		}
	}

	/**
	 * Wrapper for Class.forName
	 *
	 * @param path
	 * @param type
	 * @return
	 */
	public static <T> Class<T> lookupClass(final String path, final Class<T> type) {
		return (Class<T>) lookupClass(path);
	}

	/**
	 * Wrapper for Class.forName
	 *
	 * @param path
	 * @return
	 */
	public static Class<?> lookupClass(final String path) {
		return lookupClass(false, path);
	}

	/**
	 * Wrapper for Class.forName
	 *
	 * @param cache Whether the looked-up class should be cached into memory.
	 * @return
	 */
	public static Class<?> lookupClass(final boolean cache, final String path) {
		if (classCache.containsKey(path)) {
			return classCache.get(path);
		}
		try {
			final Class<?> clazz = Class.forName(path);
			if (cache) {
				classCache.put(path, clazz);
				reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new);
			}
			return clazz;

		} catch (final ClassNotFoundException ex) {
			throw new ReflectionException("Could not find class: " + path);
		}
	}

	/**
	 * Attempts to find an enum, throwing formatted error showing all available
	 * values if not found
	 * <p>
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum
	 *
	 * @param enumType
	 * @param name
	 * @return the enum or error
	 */
	public static <E extends Enum<E>> E lookupEnum(final Class<E> enumType, final String name) {
		return lookupEnum(enumType, name, "The enum '" + enumType.getSimpleName() + "' does not contain '" + name + "' on MC " + MinecraftVersion.getServerVersion() + "! Available values: {available}");
	}

	/**
	 * Attempts to find an enum, throwing formatted error showing all available
	 * values if not found Use {available} in errMessage to get all enum values.
	 * <p>
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum
	 *
	 * @param enumType
	 * @param name
	 * @param errMessage
	 * @return
	 */
	public static <E extends Enum<E>> E lookupEnum(final Class<E> enumType, String name, final String errMessage) {
		Valid.checkNotNull(enumType, "Type missing for " + name);
		Valid.checkNotNull(name, "Name missing for " + enumType);

		final String rawName = name.toUpperCase().replace(" ", "_");

		// Some compatibility workaround for ChatControl, Boss, CoreArena and other plugins
		// having these values in their default config. This prevents
		// malfunction on plugin's first load, in case it is loaded on an older MC version.
		if (MinecraftVersion.atLeast(V.v1_13)) {
			if (enumType == Material.class)
				if (rawName.equals("RAW_FISH"))
					name = "PUFFERFISH";

				else if (rawName.equals("MONSTER_EGG"))
					name = "SHEEP_SPAWN_EGG";

			if (enumType == org.bukkit.block.Biome.class)
				if (rawName.equalsIgnoreCase("ICE_MOUNTAINS"))
					name = "SNOWY_TAIGA";
		}

		if (MinecraftVersion.atLeast(V.v1_14))
			if (enumType == EntityType.class)
				if (rawName.equals("TIPPED_ARROW"))
					name = "ARROW";

		final String oldName = name;

		E result = lookupEnumSilent(enumType, name);

		if (result == null) {
			name = name.toUpperCase();
			result = lookupEnumSilent(enumType, name);
		}

		if (result == null) {
			name = name.replace(" ", "_");
			result = lookupEnumSilent(enumType, name);
		}

		if (result == null)
			result = lookupEnumSilent(enumType, name.replace("_", ""));

		if (result == null) {
			name = name.endsWith("S") ? name.substring(0, name.length() - 1) : name + "S";
			result = lookupEnumSilent(enumType, name);
		}

		if (result == null)
			throw new MissingEnumException(oldName, errMessage.replace("{available}", StringUtils.join(enumType.getEnumConstants(), ", ")));

		return result;
	}

	/**
	 * Wrapper for Enum.valueOf without throwing an exception
	 *
	 * @param enumType
	 * @param name
	 * @return the enum, or null if not exists
	 */
	public static <E extends Enum<E>> E lookupEnumSilent(final Class<E> enumType, final String name) {
		try {
			return Enum.valueOf(enumType, name);
		} catch (final IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * Attempts to lookup an enum by its primary name, if fails then by secondary
	 * name, if fails than returns null
	 *
	 * @param newName
	 * @param oldName
	 * @param clazz
	 * @return
	 */
	public static <T extends Enum<T>> T getEnum(final String newName, final String oldName, final Class<T> clazz) {
		T en = ReflectionUtil.lookupEnumSilent(clazz, newName);

		if (en == null)
			en = ReflectionUtil.lookupEnumSilent(clazz, oldName);

		return en;
	}

	/**
	 * Advanced: Attempts to find an enum by its full qualified name
	 *
	 * @param enumFullName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Enum<?> getEnum(final String enumFullName) {
		final String[] x = enumFullName.split("\\.(?=[^\\.]+$)");
		if (x.length == 2) {
			final String enumClassName = x[0];
			final String enumName = x[1];
			try {
				final Class<Enum> cl = (Class<Enum>) Class.forName(enumClassName);
				return Enum.valueOf(cl, enumName);
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Gets Enum (Basic)
	 *
	 * @param object
	 * @param value
	 * @param <T>
	 * @return
	 */
	public static <T extends Enum<T>> T getEnumBasic(final Object object, final String value) {
		return Enum.valueOf((Class<T>) object, value);
	}

	/**
	 * Gets the caller stack trace methods if you call this method Useful for
	 * debugging
	 *
	 * @param skipMethods
	 * @param count
	 * @return
	 */
	public static String getCallerMethods(final int skipMethods, final int count) {
		final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

		String methods = "";
		int counted = 0;

		for (int i = 2 + skipMethods; i < elements.length && counted < count; i++) {
			final StackTraceElement el = elements[i];

			if (!el.getMethodName().equals("getCallerMethods") && el.getClassName().indexOf("java.lang.Thread") != 0) {
				final String[] clazz = el.getClassName().split("\\.");

				methods += clazz[clazz.length == 0 ? 0 : clazz.length - 1] + "#" + el.getLineNumber() + "-" + el.getMethodName() + "()" + (i + 1 == elements.length ? "" : ".");
				counted++;
			}
		}

		return methods;
	}

	/**
	 * Gets the caller stack trace methods if you call this method Useful for
	 * debugging
	 *
	 * @param skipMethods
	 * @return
	 */
	public static String getCallerMethod(final int skipMethods) {
		final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

		for (int i = 2 + skipMethods; i < elements.length; i++) {
			final StackTraceElement el = elements[i];

			if (!el.getMethodName().equals("getCallerMethod") && el.getClassName().indexOf("java.lang.Thread") != 0)
				return el.getMethodName();
		}

		return "";
	}

	// ------------------------------------------------------------------------------------------
	// JavaPlugin related methods
	// ------------------------------------------------------------------------------------------

	/**
	 * Return a tree set of classes from the plugin that extend the given class
	 *
	 * @param <T>
	 * @param <T>
	 * @param plugin
	 * @param extendingClass
	 * @return
	 */
	public static <T> List<Class<? extends T>> getClasses(final Plugin plugin, @NonNull final Class<T> extendingClass) {
		final List<Class<? extends T>> found = new ArrayList<>();

		for (final Class<?> clazz : getClasses(plugin))
			if (extendingClass.isAssignableFrom(clazz) && clazz != extendingClass)
				found.add((Class<? extends T>) clazz);

		return found;
	}

	/**
	 * Get all classes in the java plugin
	 *
	 * @param plugin
	 * @return
	 */
	public static TreeSet<Class<?>> getClasses(final Plugin plugin) {
		try {
			return getClasses0(plugin);

		} catch (final ReflectiveOperationException | IOException ex) {
			throw new FoException(ex, "Failed getting classes for " + plugin.getName());
		}
	}

	// Attempts to search for classes inside of the plugin's jar
	private static TreeSet<Class<?>> getClasses0(final Plugin plugin) throws ReflectiveOperationException, IOException {
		Valid.checkNotNull(plugin, "Plugin is null!");
		Valid.checkBoolean(JavaPlugin.class.isAssignableFrom(plugin.getClass()), "Plugin must be a JavaPlugin");

		// Get the plugin .jar
		final Method m = JavaPlugin.class.getDeclaredMethod("getFile");
		m.setAccessible(true);
		final File pluginFile = (File) m.invoke(plugin);

		final TreeSet<Class<?>> classes = new TreeSet<>(Comparator.comparing(Class::toString));

		try (final JarFile jarFile = new JarFile(pluginFile)) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();

				if (name.endsWith(".class")) {
					name = name.replace("/", ".").replaceFirst(".class", "");

					final Class<?> clazz;

					// Workaround
					if (name.startsWith("de.exceptionflug.protocolize.api")) {
						continue;
					}
					try {
						clazz = Class.forName(name);
					} catch (final Throwable throwable) {
						continue;
					}

					classes.add(clazz);
				}
			}
		}

		return classes;
	}

	/**
	 * Represents an exception during reflection operation
	 */
	public static final class ReflectionException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ReflectionException(final String msg) {
			super(msg);
		}

		public ReflectionException(final String msg, final Exception ex) {
			super(msg, ex);
		}
	}

	/**
	 * Represents a failure to get the enum from {@link #lookupEnum(Class, String)}
	 * and {@link #lookupEnum(Class, String, String)} methods
	 */
	public static final class MissingEnumException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private final String enumName;

		public MissingEnumException(final String enumName, final String msg) {
			super(msg);

			this.enumName = enumName;
		}

		public MissingEnumException(final String enumName, final String msg, final Exception ex) {
			super(msg, ex);

			this.enumName = enumName;
		}

		public String getEnumName() {
			return enumName;
		}
	}
}
