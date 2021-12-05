package org.mineacademy.fo;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

import javax.annotation.Nullable;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Utility class for various reflection methods
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtil {

	/**
	 * The full package name for NMS
	 */
	public static final String NMS = "net.minecraft.server";

	/**
	 * The package name for Craftbukkit
	 */
	public static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";

	/**
	 * Compatible enum classes that fail gracefully so that
	 * plugin loads even on old MC versions where those types are non existent
	 * but are present in plugin's default configuration files
	 */
	private static final Map<Class<? extends Enum<?>>, Map<String, V>> legacyEnumTypes;

	/**
	 * Reflection utilizes a simple cache for fastest performance
	 */
	private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, ReflectionData<?>> reflectionDataCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Method[]> methodCache = new ConcurrentHashMap<>();
	private static final Collection<String> classNameGuard = ConcurrentHashMap.newKeySet();

	/**
	 * Find a class automatically for older MC version (such as type EntityPlayer for oldName
	 * and we automatically find the proper NMS import) or if MC 1.17+ is used then type
	 * the full class path such as net.minecraft.server.level.EntityPlayer and we use that instead.
	 *
	 * @param oldName
	 * @param fullName1_17
	 * @return
	 */
	public static Class<?> getNMSClass(String oldName, String fullName1_17) {
		return MinecraftVersion.atLeast(V.v1_17) ? lookupClass(fullName1_17) : getNMSClass(oldName);
	}

	/**
	 * Find a class in net.minecraft.server package, adding the version
	 * automatically
	 *
	 * @deprecated Minecraft 1.17 has a different path name,
	 *             use {@link #getNMSClass(String, String)} instead
	 *
	 * @param name
	 * @return
	 */
	@Deprecated
	public static Class<?> getNMSClass(final String name) {
		String version = MinecraftVersion.getServerVersion();

		if (!version.isEmpty())
			version += ".";

		return ReflectionUtil.lookupClass(NMS + "." + version + name);
	}

	/**
	 * Find a class in org.bukkit.craftbukkit package, adding the version
	 * automatically
	 *
	 * @param name
	 * @return
	 */
	public static Class<?> getOBCClass(final String name) {
		String version = MinecraftVersion.getServerVersion();

		if (!version.isEmpty())
			version += ".";

		return ReflectionUtil.lookupClass(CRAFTBUKKIT + "." + version + name);
	}

	/**
	 * Return a constructor for the given NMS class name (such as EntityZombie)
	 *
	 * @param nmsClassPath
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructorNMS(@NonNull final String nmsClassPath, final Class<?>... params) {
		return getConstructor(getNMSClass(nmsClassPath), params);
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
	 * Return a constructor for the given class
	 *
	 */
	public static Constructor<?> getConstructor(@NonNull final Class<?> clazz, final Class<?>... params) {
		try {
			if (reflectionDataCache.containsKey(clazz))
				return reflectionDataCache.get(clazz).getConstructor(params);

			final Constructor<?> constructor = clazz.getConstructor(params);
			constructor.setAccessible(true);

			return constructor;

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not get constructor of " + clazz + " with parameters " + Common.join(params));
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
	public static Field[] getAllFields(@NonNull Class<?> clazz) {
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
	 * Gets the declared field in class by its name
	 *
	 */
	public static Field getDeclaredField(final Class<?> clazz, final String fieldName) {
		try {

			if (reflectionDataCache.containsKey(clazz))
				return reflectionDataCache.get(clazz).getDeclaredField(fieldName);

			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);

			return field;

		} catch (final ReflectiveOperationException ex) {
			Remain.sneaky(ex);
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
	 * Gets a class method
	 *
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 */
	public static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... args) {
		try {
			Method method = clazz.getMethod(methodName, args);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException e) {}

		Method[] methods = methodCache.computeIfAbsent(clazz, k -> clazz.getMethods());
		for (final Method method : methods)
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
	 * Get a declared class method
	 *
	 */
	public static Method getDeclaredMethod(Class<?> clazz, final String methodName, Class<?>... args) {
		final Class<?> originalClass = clazz;

		while (!clazz.equals(Object.class)) {

			try {
				final Method method = clazz.getDeclaredMethod(methodName, args);
				method.setAccessible(true);

				return method;

			} catch (final NoSuchMethodException ex) {
				clazz = clazz.getSuperclass();

			} catch (final Throwable t) {
				throw new ReflectionException(t, "Error lookup up method " + methodName + " in class " + originalClass + " and her subclasses");
			}
		}

		throw new ReflectionException("Unable to find method " + methodName + " with params " + Common.join(args) + " in class " + originalClass + " and her subclasses");
	}

	/**
	 * Invoke a static method
	 *
	 * @param <T>
	 * @param methodName
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
			throw new ReflectionException(ex, "Could not invoke static method " + method + " with params " + Common.join(params, ", ", Common::simplify));
		}
	}

	/**
	 * Invoke a non static method
	 *
	 * @param <T>
	 * @param methodName
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
		Valid.checkNotNull(method, "Method cannot be null for " + instance);

		try {
			return (T) method.invoke(instance, params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not invoke method " + method + " on instance " + instance + " with params " + StringUtils.join(params));
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
			final Constructor<T> constructor;

			if (reflectionDataCache.containsKey(clazz))
				constructor = ((ReflectionData<T>) reflectionDataCache.get(clazz)).getDeclaredConstructor();

			else
				constructor = clazz.getDeclaredConstructor();

			constructor.setAccessible(true);

			return constructor.newInstance();

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not make instance of: " + clazz);
		}
	}

	/**
	 * Makes a new instanceo of the given NMS class with arguments,
	 * NB: Does not work on Minecraft 1.17+
	 *
	 * @param nmsPath
	 * @param params
	 * @return
	 */
	public static <T> T instantiateNMS(final String nmsPath, final Object... params) {
		return (T) instantiate(getNMSClass(nmsPath), params);
	}

	/**
	 * Makes a new instance of a class with arguments.
	 *
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz, final Object... params) {
		try {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Object param : params) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);
				final Class<?> paramClass = param.getClass();

				classes.add(paramClass.isPrimitive() ? ClassUtils.wrapperToPrimitive(paramClass) : paramClass);
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
			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not make new instance of " + constructor + " with params: " + Common.join(params));
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
			if (classCache.containsKey(path))
				return true;

			Class.forName(path);

			return true;

		} catch (final Throwable t) {
			return false;
		}
	}

	/**
	 * Wrapper for Class.forName
	 * @param <T>
	 *
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
	 * values if not found
	 *
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum
	 *
	 * If the given type is known to be found in new MC versions, we may return null
	 * instead of throwing an error. This is to prevent default configs containing
	 * this enum from crashing the plugin when loaded on legacy MC version.
	 *
	 * @param enumType
	 * @param name
	 *
	 * @return the enum or error with exceptions, see above
	 */
	@Nullable
	public static <E extends Enum<E>> E lookupEnum(final Class<E> enumType, final String name) {
		return lookupEnum(enumType, name, "The enum '" + enumType.getSimpleName() + "' does not contain '" + name + "' on MC " + MinecraftVersion.getServerVersion() + "! Available values: {available}");
	}

	/**
	 * Attempts to find an enum, throwing formatted error showing all available
	 * values if not found Use {available} in errMessage to get all enum values.
	 *
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum
	 *
	 * If the given type is known to be found in new MC versions, we may return null
	 * instead of throwing an error. This is to prevent default configs containing
	 * this enum from crashing the plugin when loaded on legacy MC version.
	 *
	 * @param enumType
	 * @param name
	 * @param errMessage
	 *
	 * @return the enum or error with exceptions, see above
	 */
	public static <E extends Enum<E>> E lookupEnum(final Class<E> enumType, String name, final String errMessage) {
		Valid.checkNotNull(enumType, "Type missing for " + name);
		Valid.checkNotNull(name, "Name missing for " + enumType);

		final String rawName = name.toUpperCase().replace(" ", "_");

		// Some compatibility workaround for ChatControl, Boss, CoreArena and other plugins
		// having these values in their default config. This prevents
		// malfunction on plugin's first load, in case it is loaded on an older MC version.
		{
			if (enumType == org.bukkit.block.Biome.class) {
				if (MinecraftVersion.atLeast(V.v1_13))
					if (rawName.equalsIgnoreCase("ICE_MOUNTAINS"))
						name = "SNOWY_TAIGA";
			}

			if (enumType == EntityType.class) {
				if (MinecraftVersion.atLeast(V.v1_16))
					if (rawName.equals("PIG_ZOMBIE"))
						name = "ZOMBIFIED_PIGLIN";

				if (MinecraftVersion.atLeast(V.v1_14))
					if (rawName.equals("TIPPED_ARROW"))
						name = "ARROW";

				if (MinecraftVersion.olderThan(V.v1_16))
					if (rawName.equals("ZOMBIFIED_PIGLIN"))
						name = "PIG_ZOMBIE";

				if (MinecraftVersion.olderThan(V.v1_9))
					if (rawName.equals("TRIDENT"))
						name = "ARROW";

					else if (rawName.equals("DRAGON_FIREBALL"))
						name = "FIREBALL";

				if (MinecraftVersion.olderThan(V.v1_13))
					if (rawName.equals("DROWNED"))
						name = "ZOMBIE";

					else if (rawName.equals("ZOMBIE_VILLAGER"))
						name = "ZOMBIE";
			}

			if (enumType == DamageCause.class) {
				if (MinecraftVersion.olderThan(V.v1_13))
					if (rawName.equals("DRYOUT"))
						name = "CUSTOM";

				if (MinecraftVersion.olderThan(V.v1_11))
					if (rawName.equals("ENTITY_SWEEP_ATTACK"))
						name = "ENTITY_ATTACK";

					else if (rawName.equals("CRAMMING"))
						name = "CUSTOM";

				if (MinecraftVersion.olderThan(V.v1_9))
					if (rawName.equals("FLY_INTO_WALL"))
						name = "SUFFOCATION";

					else if (rawName.equals("HOT_FLOOR"))
						name = "LAVA";

				if (rawName.equals("DRAGON_BREATH"))
					try {
						DamageCause.valueOf("DRAGON_BREATH");
					} catch (final Throwable t) {
						name = "ENTITY_ATTACK";
					}
			}
		}

		final String oldName = name;

		E result = lookupEnumSilent(enumType, name);

		// Try making the enum uppercased
		if (result == null) {
			name = name.toUpperCase();

			result = lookupEnumSilent(enumType, name);
		}

		// Try replacing spaces with underscores
		if (result == null) {
			name = name.replace(" ", "_");

			result = lookupEnumSilent(enumType, name);
		}

		// Try crunching all underscores (were spaces) all together
		if (result == null)
			result = lookupEnumSilent(enumType, name.replace("_", ""));

		if (result == null) {

			// Return null for legacy types
			final Map<String, V> legacyMap = legacyEnumTypes.get(enumType);

			if (legacyMap != null) {
				final V since = legacyMap.get(rawName);

				if (since != null && MinecraftVersion.olderThan(since))
					return null;
			}

			throw new MissingEnumException(oldName, errMessage.replace("{available}", StringUtils.join(enumType.getEnumConstants(), ", ")));
		}

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

			if (enumType == CompMaterial.class || enumType == Material.class) {
				final CompMaterial material = CompMaterial.fromString(name);

				if (material != null)
					return enumType == CompMaterial.class ? (E) material : (E) material.getMaterial();
			}

			// Since we obfuscate our plugins, enum names are changed.
			// Therefore we look up a special fromKey method in some of our enums
			boolean hasKey = false;
			Method method = null;

			try {
				method = enumType.getDeclaredMethod("fromKey", String.class);

				if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
					hasKey = true;

			} catch (final Throwable t) {
			}

			// Only invoke fromName from non-Bukkit API since this gives unexpected results
			if (method == null && !enumType.getName().contains("org.bukkit"))
				try {
					method = enumType.getDeclaredMethod("fromName", String.class);

					if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
						hasKey = true;

				} catch (final Throwable t) {
				}

			if (hasKey)
				return (E) method.invoke(null, name);

			// Resort to enum name
			return Enum.valueOf(enumType, name);

		} catch (final IllegalArgumentException ex) {
			return null;

		} catch (final ReflectiveOperationException ex) {
			return null;
		}
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

		final StringBuilder methods = new StringBuilder();
		int counted = 0;

		for (int i = 2 + skipMethods; i < elements.length && counted < count; i++) {
			final StackTraceElement el = elements[i];

			if (!el.getMethodName().equals("getCallerMethods") && el.getClassName().indexOf("java.lang.Thread") != 0) {
				final String[] clazz = el.getClassName().split("\\.");

				methods.append(clazz[clazz.length == 0 ? 0 : clazz.length - 1]).append("#").append(el.getLineNumber()).append("-").append(el.getMethodName()).append("()").append(i + 1 == elements.length ? "" : ".");
				counted++;
			}
		}

		return methods.toString();
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
	public static List<Class<?>> getClasses(final Plugin plugin) {
		final List<Class<?>> found = new ArrayList<>();

		for (final Class<?> clazz : getClasses(plugin, null))
			found.add(clazz);

		return found;
	}

	/**
	 * Get all classes in the java plugin
	 *
	 * @param plugin
	 * @return
	 */
	@SneakyThrows
	public static <T> TreeSet<Class<T>> getClasses(final Plugin plugin, Class<T> extendingClass) {
		Valid.checkNotNull(plugin, "Plugin is null!");
		Valid.checkBoolean(JavaPlugin.class.isAssignableFrom(plugin.getClass()), "Plugin must be a JavaPlugin");

		// Get the plugin .jar
		final Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
		getFileMethod.setAccessible(true);

		final File pluginFile = (File) getFileMethod.invoke(plugin);

		final TreeSet<Class<T>> classes = new TreeSet<>(Comparator.comparing(Class::toString));

		try (final JarFile jarFile = new JarFile(pluginFile)) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();

				if (name.endsWith(".class")) {
					name = name.replace("/", ".").replaceFirst(".class", "");

					Class<?> clazz = null;

					try {
						clazz = Class.forName(name, false, SimplePlugin.class.getClassLoader());

						if (extendingClass == null || (extendingClass.isAssignableFrom(clazz) && clazz != extendingClass))
							classes.add((Class<T>) clazz);

					} catch (final Throwable throwable) {

						if (extendingClass != null && (clazz != null && extendingClass.isAssignableFrom(clazz)) && clazz != extendingClass)
							Common.log("Unable to load class '" + name + "' due to error: " + throwable);

						continue;
					}
				}
			}
		}

		return classes;
	}

	static {

		final Map<Class<? extends Enum<?>>, Map<String, V>> legacyEnums = new HashMap<>();

		final Map<String, V> entities = new HashMap<>();
		entities.put("TIPPED_ARROW", V.v1_9);
		entities.put("SPECTRAL_ARROW", V.v1_9);
		entities.put("SHULKER_BULLET", V.v1_9);
		entities.put("DRAGON_FIREBALL", V.v1_9);
		entities.put("SHULKER", V.v1_9);
		entities.put("AREA_EFFECT_CLOUD", V.v1_9);
		entities.put("LINGERING_POTION", V.v1_9);
		entities.put("POLAR_BEAR", V.v1_10);
		entities.put("HUSK", V.v1_10);
		entities.put("ELDER_GUARDIAN", V.v1_11);
		entities.put("WITHER_SKELETON", V.v1_11);
		entities.put("STRAY", V.v1_11);
		entities.put("DONKEY", V.v1_11);
		entities.put("MULE", V.v1_11);
		entities.put("EVOKER_FANGS", V.v1_11);
		entities.put("EVOKER", V.v1_11);
		entities.put("VEX", V.v1_11);
		entities.put("VINDICATOR", V.v1_11);
		entities.put("ILLUSIONER", V.v1_12);
		entities.put("PARROT", V.v1_12);
		entities.put("TURTLE", V.v1_13);
		entities.put("PHANTOM", V.v1_13);
		entities.put("TRIDENT", V.v1_13);
		entities.put("COD", V.v1_13);
		entities.put("SALMON", V.v1_13);
		entities.put("PUFFERFISH", V.v1_13);
		entities.put("TROPICAL_FISH", V.v1_13);
		entities.put("DROWNED", V.v1_13);
		entities.put("DOLPHIN", V.v1_13);
		entities.put("CAT", V.v1_14);
		entities.put("PANDA", V.v1_14);
		entities.put("PILLAGER", V.v1_14);
		entities.put("RAVAGER", V.v1_14);
		entities.put("TRADER_LLAMA", V.v1_14);
		entities.put("WANDERING_TRADER", V.v1_14);
		entities.put("FOX", V.v1_14);
		entities.put("BEE", V.v1_15);
		entities.put("HOGLIN", V.v1_16);
		entities.put("PIGLIN", V.v1_16);
		entities.put("STRIDER", V.v1_16);
		entities.put("ZOGLIN", V.v1_16);
		entities.put("PIGLIN_BRUTE", V.v1_16);
		entities.put("AXOLOTL", V.v1_17);
		entities.put("GLOW_ITEM_FRAME", V.v1_17);
		entities.put("GLOW_SQUID", V.v1_17);
		entities.put("GOAT", V.v1_17);
		entities.put("MARKER", V.v1_17);
		legacyEnums.put(EntityType.class, entities);

		final Map<String, V> spawnReasons = new HashMap<>();
		spawnReasons.put("DROWNED", V.v1_13);
		legacyEnums.put(SpawnReason.class, spawnReasons);

		legacyEnumTypes = legacyEnums;
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	private static final class ReflectionData<T> {
		private final Class<T> clazz;

		ReflectionData(final Class<T> clazz) {
			this.clazz = clazz;
		}

		//private final Map<String, Collection<Method>> methodCache = new ConcurrentHashMap<>();
		private final Map<Integer, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
		private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
		private final Collection<String> fieldGuard = ConcurrentHashMap.newKeySet();
		private final Collection<Integer> constructorGuard = ConcurrentHashMap.newKeySet();

		public void cacheConstructor(final Constructor<T> constructor) {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Class<?> param : constructor.getParameterTypes()) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);

				classes.add(param);
			}

			constructorCache.put(Arrays.hashCode(classes.toArray(new Class<?>[0])), constructor);
		}

		public Constructor<T> getDeclaredConstructor(final Class<?>... paramTypes) throws NoSuchMethodException {
			final Integer hashCode = Arrays.hashCode(paramTypes);

			if (constructorCache.containsKey(hashCode))
				return (Constructor<T>) constructorCache.get(hashCode);

			if (constructorGuard.contains(hashCode)) {
				while (constructorGuard.contains(hashCode)) {

				} // Wait for other thread;
				return getDeclaredConstructor(paramTypes);
			}

			constructorGuard.add(hashCode);

			try {
				final Constructor<T> constructor = clazz.getDeclaredConstructor(paramTypes);

				cacheConstructor(constructor);

				return constructor;

			} finally {
				constructorGuard.remove(hashCode);
			}
		}

		public Constructor<T> getConstructor(final Class<?>... paramTypes) throws NoSuchMethodException {
			final Integer hashCode = Arrays.hashCode(paramTypes);

			if (constructorCache.containsKey(hashCode))
				return (Constructor<T>) constructorCache.get(hashCode);

			if (constructorGuard.contains(hashCode)) {
				while (constructorGuard.contains(hashCode)) {
					// Wait for other thread;
				}

				return getConstructor(paramTypes);
			}

			constructorGuard.add(hashCode);

			try {
				final Constructor<T> constructor = clazz.getConstructor(paramTypes);

				cacheConstructor(constructor);

				return constructor;

			} finally {
				constructorGuard.remove(hashCode);
			}
		}

		/*public void cacheMethod(final Method method) {
			methodCache.computeIfAbsent(method.getName(), unused -> ConcurrentHashMap.newKeySet()).add(method);
		}*/

		/*public Method getDeclaredMethod(final String name, final Class<?>... paramTypes) throws NoSuchMethodException {
			if (methodCache.containsKey(name)) {
				final Collection<Method> methods = methodCache.get(name);
		
				for (final Method method : methods)
					if (Arrays.equals(paramTypes, method.getParameterTypes()))
						return method;
			}
		
			final Method method = clazz.getDeclaredMethod(name, paramTypes);
		
			cacheMethod(method);
		
			return method;
		}*/

		public void cacheField(final Field field) {
			fieldCache.put(field.getName(), field);
		}

		public Field getDeclaredField(final String name) throws NoSuchFieldException {

			if (fieldCache.containsKey(name))
				return fieldCache.get(name);

			if (fieldGuard.contains(name)) {
				while (fieldGuard.contains(name)) {
				}

				return getDeclaredField(name);
			}

			fieldGuard.add(name);

			try {
				final Field field = clazz.getDeclaredField(name);

				cacheField(field);

				return field;

			} finally {
				fieldGuard.remove(name);
			}
		}
	}

	/**
	 * Represents an exception during reflection operation
	 */
	public static final class ReflectionException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ReflectionException(final String message) {
			super(message);
		}

		public ReflectionException(final Throwable ex, final String message) {
			super(message, ex);
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
