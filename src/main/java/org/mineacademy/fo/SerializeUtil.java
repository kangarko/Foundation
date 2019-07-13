package org.mineacademy.fo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictCollection;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializeUtil {

	/**
	 * Special case: Support for GameAPI's ConfigSerializable interface
	 *
	 */
	private static final Class<?> gameAPIserializeClass;

	// Load the GameAPI ConfigSerializable interface
	static {
		Class<?> cl;

		final char[] path = new char[] {
				'o', 'r', 'g',
				'.',
				'm', 'i', 'n', 'e', 'a', 'c', 'a', 'd', 'e', 'm', 'y',
				'.',
				'g', 'a', 'm', 'e', 'a', 'p', 'i',
				'.',
				'm', 'i', 's', 'c',
				'.',
				'C', 'o', 'n', 'f', 'i', 'g', 'S', 'e', 'r', 'i', 'a', 'l', 'i', 'z', 'a', 'b', 'l', 'e'
		};

		try {
			cl = Class.forName(new String(path));
		} catch (final ClassNotFoundException ex) {
			cl = null;
		}

		gameAPIserializeClass = cl;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting objects into strings so you can save them in your files
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the given object into something you can safely save in file as a string
	 *
	 * @param obj
	 * @return
	 */
	public static Object serialize(Object obj) {
		if (obj == null)
			return null;

		if (obj instanceof SerializedMap)
			return serialize(((SerializedMap) obj).serialize());

		else if (obj instanceof ConfigSerializable)
			return serialize(((ConfigSerializable) obj).serialize().serialize());

		else if (gameAPIserializeClass != null && gameAPIserializeClass.isAssignableFrom(obj.getClass()))
			try {
				return serialize(obj.getClass().getMethod("serialize").invoke(obj));
			} catch (final ReflectiveOperationException ex) {
				throw new FoException(ex);
			}

		else if (obj instanceof StrictCollection)
			return serialize(((StrictCollection) obj).serialize());

		else if (obj instanceof ChatColor)
			return ((ChatColor) obj).name();

		else if (obj instanceof CompMaterial)
			return obj.toString();

		else if (obj instanceof Location)
			return serializeLoc((Location) obj);

		else if (obj instanceof UUID)
			return obj.toString();

		else if (obj instanceof Enum<?>)
			return obj.toString();

		else if (obj instanceof CommandSender)
			return ((CommandSender) obj).getName();

		else if (obj instanceof World)
			return ((World) obj).getName();

		else if (obj instanceof ItemCreator.ItemCreatorBuilder)
			return ((ItemCreator.ItemCreatorBuilder) obj).build().make();

		else if (obj instanceof ItemCreator)
			return ((ItemCreator) obj).make();

		else if (obj instanceof Iterable || obj.getClass().isArray()) {
			final List<Object> serialized = new ArrayList<>();

			if (obj instanceof Iterable)
				for (final Object element : (Iterable<?>) obj)
					serialized.add(serialize(element));
			else
				for (final Object element : (Object[]) obj)
					serialized.add(serialize(element));

			return serialized;
		}

		else if (obj instanceof YamlConfig)
			throw new FoException("To save your YamlConfig " + obj.getClass().getSimpleName() + " make it implement ConfigSerializable!");

		else if (obj instanceof Integer || obj instanceof Double || obj instanceof Float || obj instanceof Long
				|| obj instanceof String || obj instanceof Boolean || obj instanceof Map
				|| obj instanceof ItemStack
		/*|| obj instanceof MemorySection*/)
			return obj;

		throw new FoException("Does not know how to serialize " + obj.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + obj);
	}

	/**
	 * Converts a {@link Location} into "world x y z yaw pitch" string
	 *
	 * @param loc
	 * @return
	 */
	public static String serializeLoc(Location loc) {
		return loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + (loc.getPitch() != 0F || loc.getYaw() != 0F ? " " + loc.getYaw() + " " + loc.getPitch() : "");
	}

	/**
	 * Runsthrough each item in the list and serializes it
	 *
	 * Returns a new list of serialized items
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static List<Object> serializeList(Iterable<?> array) {
		final List<Object> list = new ArrayList<>();

		for (final Object t : array)
			list.add(serialize(t));

		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting stored strings from your files back into classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Attempts to convert the given object into a class
	 *
	 * Example: Call deserialize(Location.class, "worldName 5 -1 47") to convert that into a Bukkit location object
	 *
	 * @param <T>
	 * @param classOf
	 * @param object
	 * @return
	 */
	public static <T> T deserialize(@NonNull Class<T> classOf, @NonNull Object object) {
		return deserialize(classOf, object, (Object[]) null);
	}

	/**
	 * Please see {@link #deserialize(Class, Object)}
	 *
	 * @param <T>
	 * @param classOf
	 * @param object
	 * @param deserializeParameters, special case for some of our plugins
	 * @return
	 */
	public static <T> T deserialize(@NonNull Class<T> classOf, @NonNull Object object, Object... deserializeParameters) {
		final SerializedMap map = SerializedMap.of(object);

		// Step 1 - Search for basic deserialize(SerializedMap) method
		Method deserializeMethod = ReflectionUtil.getMethod(classOf, "deserialize", SerializedMap.class);

		if (deserializeMethod != null)
			return ReflectionUtil.invokeStatic(deserializeMethod, map);

		// Step 2 - Search for our deserialize(Params[], SerializedMap) method
		if (deserializeParameters != null) {
			final List<Class<?>> joinedClasses = new ArrayList<>();

			for (final Object param : deserializeParameters)
				joinedClasses.add(param.getClass());

			joinedClasses.add(SerializedMap.class);

			deserializeMethod = ReflectionUtil.getMethod(classOf, "deserialize", joinedClasses.toArray(new Class[joinedClasses.size()]));

			final List<Object> joinedParams = new ArrayList<>();

			for (final Object param : deserializeParameters)
				joinedParams.add(param);

			joinedParams.add(map);

			if (deserializeMethod != null)
				return ReflectionUtil.invokeStatic(deserializeMethod, joinedParams);
		}

		// Step 3 - Search for "getByName" method used by us or some Bukkit classes such as Enchantment
		if (deserializeMethod == null && object instanceof String) {
			deserializeMethod = ReflectionUtil.getMethod(classOf, "getByName", String.class);

			if (deserializeMethod != null)
				return ReflectionUtil.invokeStatic(deserializeMethod, object);
		}

		// Step 4 - If there is no deserialize method, just deserialize the given object
		if (object != null) {

			if (classOf == String.class)
				object = object.toString();

			else if (classOf == Integer.class)
				object = Double.valueOf(object.toString()).intValue();

			else if (classOf == Long.class)
				object = Double.valueOf(object.toString()).longValue();

			else if (classOf == Double.class)
				object = Double.valueOf(object.toString());

			else if (classOf == Float.class)
				object = Float.valueOf(object.toString());

			else if (classOf == Boolean.class)
				object = Boolean.valueOf(object.toString());

			else if (classOf == SerializedMap.class)
				object = SerializedMap.of(object);

			else if (classOf == Location.class)
				object = deserializeLocation(object);

			else if (classOf == CompMaterial.class)
				object = CompMaterial.fromString(object.toString());

			else if (Enum.class.isAssignableFrom(classOf))
				object = ReflectionUtil.lookupEnum((Class<Enum>) classOf, object.toString());

			else if (classOf == Object.class) {
				// pass through

			} else
				throw new FoException("Unable to deserialize " + classOf.getSimpleName() + ", lacking static deserialize method! Data: " + object);
		}

		return (T) object;
	}

	/**
	 * Converts a string into location, see {@link #deserializeLocation(Object)} for how strings are saved
	 *
	 * @param raw
	 * @return
	 */
	public static Location deserializeLocation(Object raw) {
		if (raw == null)
			return null;

		if (raw instanceof Location)
			return (Location) raw;

		final String[] parts = raw.toString().contains(", ") ? raw.toString().split(", ") : raw.toString().split(" ");
		Valid.checkBoolean(parts.length == 4 || parts.length == 6, "Expected location (String) but got " + raw.getClass().getSimpleName() + ": " + raw);

		final String world = parts[0];
		final World bukkitWorld = Bukkit.getWorld(world);
		if (Bukkit.getWorld(world) == null)
			throw new InvalidWorldException("Location with invalid world '" + world + "': " + raw);

		final int x = Integer.parseInt(parts[1]), y = Integer.parseInt(parts[2]), z = Integer.parseInt(parts[3]);
		final float yaw = Float.parseFloat(parts.length == 6 ? parts[4] : "0"), pitch = Float.parseFloat(parts.length == 6 ? parts[5] : "0");

		return new Location(bukkitWorld, x, y, z, yaw, pitch);
	}

	/**
	 * Deserializes a list containing maps
	 *
	 * @param <T>
	 * @param listOfObjects
	 * @param asWhat
	 * @return
	 */
	public static <T extends ConfigSerializable> List<T> deserializeMapList(Object listOfObjects, Class<T> asWhat) {
		if (listOfObjects == null)
			return null;

		Valid.checkBoolean(listOfObjects instanceof ArrayList, "Only deserialize a list of maps, nie " + listOfObjects.getClass());
		final List<T> loaded = new ArrayList<>();

		for (final Object part : (ArrayList<?>) listOfObjects) {
			final T deserialized = deserializeMap(part, asWhat);

			if (deserialized != null)
				loaded.add(deserialized);
		}

		return loaded;
	}

	/**
	 * Deserializes a map
	 *
	 * @param <T>
	 * @param rawMap
	 * @param asWhat
	 * @return
	 */
	public static <T extends ConfigSerializable> T deserializeMap(Object rawMap, Class<T> asWhat) {
		if (rawMap == null)
			return null;

		Valid.checkBoolean(rawMap instanceof Map, "The object to deserialize must be map, but got: " + rawMap.getClass());

		final Map<String, Object> map = (Map<String, Object>) rawMap;
		final Method deserialize;

		try {
			deserialize = asWhat.getMethod("deserialize", SerializedMap.class);
			Valid.checkBoolean(Modifier.isPublic(deserialize.getModifiers()) && Modifier.isStatic(deserialize.getModifiers()), asWhat + " is missing public 'public static T deserialize()' method");

		} catch (final NoSuchMethodException ex) {
			Common.throwError(ex, "Triede chyba konecna deserialize(mapa) metoda. Tried: " + asWhat.getSimpleName());
			return null;
		}

		final Object invoked;

		try {
			invoked = deserialize.invoke(null, SerializedMap.of(map));
		} catch (final ReflectiveOperationException e) {
			Common.throwError(e, "Chyba pri volani " + deserialize.getName() + " z " + asWhat.getSimpleName() + " z udajmi " + map);
			return null;
		}

		Valid.checkBoolean(invoked.getClass().isAssignableFrom(asWhat), invoked.getClass().getSimpleName() + " != " + asWhat.getSimpleName());
		return (T) invoked;
	}
}
