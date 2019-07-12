package org.mineacademy.fo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
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
	public static Class<?> getNMSClass(String name) {
		return ReflectionUtil.lookupClass(NMS + "." + MinecraftVersion.getServerVersion() + "." + name);
	}

	/**
	 * Find a class in org.bukkit.craftbukkit package, adding the version
	 * automatically
	 *
	 * @param name
	 * @return
	 */
	public static Class<?> getOBCClass(String name) {
		return ReflectionUtil.lookupClass(CRAFTBUKKIT + "." + MinecraftVersion.getServerVersion() + "." + name);
	}

	/**
	 * Set the static field to the given value
	 *
	 * @param clazz
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setStaticField(@NonNull Class<?> clazz, String fieldName, Object fieldValue) {
		try {
			final Field field = getDeclaredField(clazz, fieldName);

			field.set(null, fieldValue);

		} catch (final Throwable t) {
			throw new FoException(t, "Could not set " + fieldName + " in " + clazz + " to " + fieldValue);
		}
	}

	/**
	 * Convenience method for getting a static field content.
	 *
	 * @param <T>
	 * @param clazz
	 * @param field
	 * @param type
	 * @return
	 */
	public static <T> T getStaticFieldContent(@NonNull Class<?> clazz, String field) {
		return getFieldContent(clazz, field, null);
	}

	/**
	 * Return a constructor for the given NMS class. We prepend the class name
	 * with the {@link #NMS} so you only have to give in the name of the class.
	 *
	 * @param nmsClass
	 * @param params
	 * @return
	 */
	public static Constructor<?> getNMSConstructor(@NonNull String nmsClass, Class<?>... params) {
		return getConstructor(getNMSClass(nmsClass), params);
	}

	/**
	 * Return a constructor for the given OBC class. We prepend the class name
	 * with the {@link #OBC} so you only have to give in the name of the class.
	 *
	 * @param obcClass
	 * @param params
	 * @return
	 */
	public static Constructor<?> getOBCConstructor(@NonNull String obcClass, Class<?>... params) {
		return getConstructor(getOBCClass(obcClass), params);
	}

	/**
	 * Return a constructor for the given class
	 *
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructor(@NonNull Class<?> clazz, Class<?>... params) {
		try {
			return clazz.getConstructor(params);

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
	 *
	 */
	public static <T> T getFieldContent(Object instance, String field) {
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
	public static <T> T getFieldContent(Class<?> clazz, String field, Object instance) {
		final String originalClassName = clazz.getSimpleName();

		do {
			// note: getDeclaredFields() fails if any of the fields are classes that cannot be loaded
			for (final Field f : clazz.getDeclaredFields())
				if (f.getName().equals(field))
					return (T) getFieldContent(f, instance);

		} while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		throw new ReflectionException("No such field " + field + " in " + originalClassName + " or its superclasses");
	}

	/**
	 * Get the field content
	 *
	 * @param field
	 * @param instance
	 * @return
	 */
	public static Object getFieldContent(Field field, Object instance) {
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

		do {
			list.addAll(Arrays.asList(clazz.getDeclaredFields()));
		} while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		return list.toArray(new Field[list.size()]);
	}

	/**
	 * Gets the declared field in class by its name
	 *
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getDeclaredField(Class<?> clazz, String fieldName) {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);

			return field;

		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Gets a class method
	 *
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 */
	public static Method getMethod(Class<?> clazz, String methodName, Class<?>... args) {
		for (final Method method : clazz.getMethods())
			if (method.getName().equals(methodName) && isClassListEqual(args, method.getParameterTypes())) {
				method.setAccessible(true);

				return method;
			}

		return null;
	}

	// Compares class lists
	private static boolean isClassListEqual(Class<?>[] first, Class<?>[] second) {
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
	public static Method getMethod(Class<?> clazz, String methodName, Integer args) {
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
	public static Method getMethod(Class<?> clazz, String methodName) {
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
	public static <T> T invokeStatic(Class<?> cl, String methodName, Object... params) {
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
	public static <T> T invokeStatic(Method method, Object... params) {
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
	public static <T> T invoke(String methodName, Object instance, Object... params) {
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
	public static <T> T invoke(Method method, Object instance, Object... params) {
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
	public static <T> T instantiate(Class<T> clazz) {
		try {
			final Constructor<T> c = clazz.getDeclaredConstructor();
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
	public static <T> T instantiate(Class<T> clazz, Object... params) {
		try {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Object param : params) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);
				final Class<?> paramClass = param.getClass();

				classes.add(paramClass.isPrimitive() ? ClassUtils.wrapperToPrimitive(paramClass) : paramClass);
			}

			final Constructor<T> c = clazz.getDeclaredConstructor(classes.toArray(new Class[classes.size()]));
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
	public static <T> T instantiate(Constructor<T> constructor, Object... params) {
		try {
			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not make new instance of " + constructor + " with params: " + Common.joinToString(params));
		}
	}

	/**
	 * Wrapper for Class.forName
	 *
	 * @param path
	 * @param type
	 * @return
	 */
	public static <T> Class<T> lookupClass(String path, Class<T> type) {
		return (Class<T>) lookupClass(path);
	}

	/**
	 * Wrapper for Class.forName
	 *
	 * @param path
	 * @return
	 */
	public static Class<?> lookupClass(String path) {
		try {
			return Class.forName(path);

		} catch (final ClassNotFoundException ex) {
			throw new ReflectionException("Could not find class: " + path);
		}
	}

	/**
	 * Attempts to find an enum, throwing formatted error showing all available
	 * values if not found
	 *
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum
	 *
	 * @param enumType
	 * @param name
	 * @return the enum or error
	 */
	public static <E extends Enum<E>> E lookupEnum(Class<E> enumType, String name) {
		return lookupEnum(enumType, name, "The enum '" + enumType.getSimpleName() + "' does not contain '" + name + "' on MC " + MinecraftVersion.getServerVersion() + "! Available values: {available}");
	}

	/**
	 * Attempts to find an enum, throwing formatted error showing all available
	 * values if not found Use {available} in errMessage to get all enum values.
	 *
	 * The field name is uppercased, spaces are replaced with underscores and even
	 * plural S is added in attempts to detect the correct enum
	 *
	 * @param enumType
	 * @param name
	 * @param errMessage
	 * @return
	 */
	public static <E extends Enum<E>> E lookupEnum(Class<E> enumType, String name, String errMessage) {
		Valid.checkNotNull(enumType, "Type missing for " + name);
		Valid.checkNotNull(name, "Name missing for " + enumType);

		final String rawName = new String(name).toUpperCase().replace(" ", "_");

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

		if (MinecraftVersion.atLeast(V.v1_14)) {
			if (enumType == EntityType.class)
				if (rawName.equals("TIPPED_ARROW"))
					name = "ARROW";
		}

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
	public static <E extends Enum<E>> E lookupEnumSilent(Class<E> enumType, String name) {
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
	public static <T extends Enum<T>> T getEnum(String newName, String oldName, Class<T> clazz) {
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
	 * Gets the caller stack trace methods if you call this method Useful for
	 * debugging
	 *
	 * @param skipMethods
	 * @param count
	 * @return
	 */
	public static String getCallerMethods(int skipMethods, int count) {
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
	public static String getCallerMethod(int skipMethods) {
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
	 * Get all classes in the java plugin
	 *
	 * @param plugin
	 * @return
	 */
	public static TreeSet<Class<?>> getClasses(Plugin plugin) {
		try {
			return getClasses0(plugin);

		} catch (ReflectiveOperationException | IOException ex) {
			throw new FoException(ex, "Failed getting classes for " + plugin.getName());
		}
	}

	// Attempts to search for classes inside of the plugin's jar
	private static TreeSet<Class<?>> getClasses0(Plugin plugin) throws ReflectiveOperationException, IOException {
		Valid.checkNotNull(plugin, "Plugin is null!");
		Valid.checkBoolean(JavaPlugin.class.isAssignableFrom(plugin.getClass()), "Plugin must be a JavaPlugin");

		// Get the plugin .jar
		final Method m = JavaPlugin.class.getDeclaredMethod("getFile");
		m.setAccessible(true);
		final File pluginFile = (File) m.invoke(plugin);

		final TreeSet<Class<?>> classes = new TreeSet<>((first, second) -> first.toString().compareTo(second.toString()));

		try (JarFile jarFile = new JarFile(pluginFile)) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();

				if (name.endsWith(".class")) {
					name = name.replace("/", ".").replaceFirst(".class", "");

					Class<?> clazz;

					try {
						clazz = Class.forName(name);
					} catch (final NoClassDefFoundError ex) {
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

		public ReflectionException(String msg) {
			super(msg);
		}

		public ReflectionException(String msg, Exception ex) {
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

		public MissingEnumException(String enumName, String msg) {
			super(msg);

			this.enumName = enumName;
		}

		public MissingEnumException(String enumName, String msg, Exception ex) {
			super(msg, ex);

			this.enumName = enumName;
		}

		public String getEnumName() {
			return enumName;
		}
	}
}
