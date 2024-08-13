package org.mineacademy.fo.settings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.model.ConfigSerializable;

/**
 * Utility class for storing and retrieving classes
 */
public class ConfigurationSerialization {

	public static final String SERIALIZED_TYPE_KEY = "==";
	private final Class<? extends ConfigSerializable> clazz;
	private static Map<String, Class<? extends ConfigSerializable>> aliases = new HashMap<>();

	static {
		/*registerClass(Vector.class);
		registerClass(BlockVector.class);
		registerClass(ItemStack.class);
		registerClass(Color.class);
		registerClass(PotionEffect.class);
		registerClass(FireworkEffect.class);
		registerClass(Pattern.class);
		registerClass(Location.class);
		registerClass(AttributeModifier.class);
		registerClass(BoundingBox.class);
		registerClass(SpawnRule.class);*/
	}

	protected ConfigurationSerialization(Class<? extends ConfigSerializable> clazz) {
		this.clazz = clazz;
	}

	@Nullable
	protected Method getMethod(String name, boolean isStatic) {
		try {
			final Method method = clazz.getDeclaredMethod(name, Map.class);

			if (!ConfigSerializable.class.isAssignableFrom(method.getReturnType())) {
				return null;
			}
			if (Modifier.isStatic(method.getModifiers()) != isStatic) {
				return null;
			}

			return method;
		} catch (final NoSuchMethodException ex) {
			return null;
		} catch (final SecurityException ex) {
			return null;
		}
	}

	@Nullable
	protected Constructor<? extends ConfigSerializable> getConstructor() {
		try {
			return clazz.getConstructor(Map.class);
		} catch (final NoSuchMethodException ex) {
			return null;
		} catch (final SecurityException ex) {
			return null;
		}
	}

	@Nullable
	protected ConfigSerializable deserializeViaMethod(Method method, Map<String, ?> args) {
		try {
			final ConfigSerializable result = (ConfigSerializable) method.invoke(null, args);

			if (result == null) {
				Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE, "Could not call method '" + method.toString() + "' of " + clazz + " for deserialization: method returned null");
			} else {
				return result;
			}
		} catch (final Throwable ex) {
			Logger.getLogger(ConfigurationSerialization.class.getName()).log(
					Level.SEVERE,
					"Could not call method '" + method.toString() + "' of " + clazz + " for deserialization",
					ex instanceof InvocationTargetException ? ex.getCause() : ex);
		}

		return null;
	}

	@Nullable
	protected ConfigSerializable deserializeViaCtor(Constructor<? extends ConfigSerializable> ctor, Map<String, ?> args) {
		try {
			return ctor.newInstance(args);
		} catch (final Throwable ex) {
			Logger.getLogger(ConfigurationSerialization.class.getName()).log(
					Level.SEVERE,
					"Could not call constructor '" + ctor.toString() + "' of " + clazz + " for deserialization",
					ex instanceof InvocationTargetException ? ex.getCause() : ex);
		}

		return null;
	}

	@Nullable
	public ConfigSerializable deserialize(Map<String, ?> args) {
		ConfigSerializable result = null;
		Method method = null;

		if (result == null) {
			method = getMethod("deserialize", true);

			if (method != null) {
				result = deserializeViaMethod(method, args);
			}
		}

		if (result == null) {
			method = getMethod("valueOf", true);

			if (method != null) {
				result = deserializeViaMethod(method, args);
			}
		}

		if (result == null) {
			final Constructor<? extends ConfigSerializable> constructor = getConstructor();

			if (constructor != null) {
				result = deserializeViaCtor(constructor, args);
			}
		}

		return result;
	}

	/**
	 * Attempts to deserialize the given arguments into a new instance of the
	 * given class.
	 * <p>
	 * The class must implement {@link ConfigSerializable}, including
	 * the extra methods as specified in the javadoc of
	 * ConfigSerializable.
	 * <p>
	 * If a new instance could not be made, an example being the class not
	 * fully implementing the interface, null will be returned.
	 *
	 * @param args Arguments for deserialization
	 * @param clazz Class to deserialize into
	 * @return New instance of the specified class
	 */
	@Nullable
	public static ConfigSerializable deserializeObject(Map<String, ?> args, Class<? extends ConfigSerializable> clazz) {
		return new ConfigurationSerialization(clazz).deserialize(args);
	}

	/**
	 * Attempts to deserialize the given arguments into a new instance of the
	 * given class.
	 * <p>
	 * The class must implement {@link ConfigSerializable}, including
	 * the extra methods as specified in the javadoc of
	 * ConfigSerializable.
	 * <p>
	 * If a new instance could not be made, an example being the class not
	 * fully implementing the interface, null will be returned.
	 *
	 * @param args Arguments for deserialization
	 * @return New instance of the specified class
	 */
	@Nullable
	public static ConfigSerializable deserializeObject(Map<String, ?> args) {
		Class<? extends ConfigSerializable> clazz = null;

		if (args.containsKey(SERIALIZED_TYPE_KEY)) {
			try {
				final String alias = (String) args.get(SERIALIZED_TYPE_KEY);

				if (alias == null) {
					throw new IllegalArgumentException("Cannot have null alias");
				}
				clazz = getClassByAlias(alias);
				if (clazz == null) {
					throw new IllegalArgumentException("Specified class does not exist ('" + alias + "')");
				}
			} catch (final ClassCastException ex) {
				ex.fillInStackTrace();
				throw ex;
			}
		} else {
			throw new IllegalArgumentException("Args doesn't contain type key ('" + SERIALIZED_TYPE_KEY + "')");
		}

		return new ConfigurationSerialization(clazz).deserialize(args);
	}

	/**
	 * Registers the given {@link ConfigSerializable} class by its
	 * alias
	 *
	 * @param clazz Class to register
	 */
	public static void registerClass(Class<? extends ConfigSerializable> clazz) {
		registerClass(clazz, clazz.getName());
	}

	/**
	 * Registers the given alias to the specified {@link
	 * ConfigSerializable} class
	 *
	 * @param clazz Class to register
	 * @param alias Alias to register as
	 */
	public static void registerClass(Class<? extends ConfigSerializable> clazz, String alias) {
		aliases.put(alias, clazz);
	}

	/**
	 * Unregisters the specified alias to a {@link ConfigSerializable}
	 *
	 * @param alias Alias to unregister
	 */
	public static void unregisterClass(String alias) {
		aliases.remove(alias);
	}

	/**
	 * Unregisters any aliases for the specified {@link
	 * ConfigSerializable} class
	 *
	 * @param clazz Class to unregister
	 */
	public static void unregisterClass(Class<? extends ConfigSerializable> clazz) {
		while (aliases.values().remove(clazz)) {

		}
	}

	/**
	 * Attempts to get a registered {@link ConfigSerializable} class by
	 * its alias
	 *
	 * @param alias Alias of the serializable
	 * @return Registered class, or null if not found
	 */
	@Nullable
	public static Class<? extends ConfigSerializable> getClassByAlias(String alias) {
		return aliases.get(alias);
	}
}
