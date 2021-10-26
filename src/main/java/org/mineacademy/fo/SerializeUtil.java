package org.mineacademy.fo;

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil.ReflectionException;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictCollection;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializeUtil {

	/**
	 * A list of custom serializers
	 */
	private static Map<Class<Object>, Function<Object, String>> serializers = new HashMap<>();

	/**
	 * Add a custom serializer to the list
	 *
	 * @param <T>
	 * @param fromClass
	 * @param serializer
	 */
	public static <T> void addSerializer(Class<T> fromClass, Function<T, String> serializer) {
		serializers.put((Class<Object>) fromClass, (Function<Object, String>) serializer);
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
	public static Object serialize(final Object obj) {
		if (obj == null)
			return null;

		if (serializers.containsKey(obj.getClass()))
			return serializers.get(obj.getClass()).apply(obj);

		if (obj instanceof ConfigSerializable)
			return serialize(((ConfigSerializable) obj).serialize().serialize());

		else if (obj instanceof StrictCollection)
			return serialize(((StrictCollection) obj).serialize());

		else if (obj instanceof ChatColor)
			return ((ChatColor) obj).name();

		else if (obj instanceof CompChatColor)
			return ((CompChatColor) obj).getName();

		else if (obj instanceof net.md_5.bungee.api.ChatColor) {
			final net.md_5.bungee.api.ChatColor color = ((net.md_5.bungee.api.ChatColor) obj);

			return MinecraftVersion.atLeast(V.v1_16) ? color.toString() : color.name();
		}

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

		else if (obj instanceof PotionEffectType)
			return ((PotionEffectType) obj).getName();

		else if (obj instanceof PotionEffect)
			return serializePotionEffect((PotionEffect) obj);

		else if (obj instanceof ItemCreator)
			return ((ItemCreator) obj).make();

		else if (obj instanceof SimpleTime)
			return ((SimpleTime) obj).getRaw();

		else if (obj instanceof SimpleSound)
			return ((SimpleSound) obj).toString();

		else if (obj instanceof Color)
			return "#" + ((Color) obj).getRGB();

		else if (obj instanceof RangedValue)
			return ((RangedValue) obj).toLine();

		else if (obj instanceof RangedSimpleTime)
			return ((RangedSimpleTime) obj).toLine();

		else if (obj instanceof BaseComponent)
			return Remain.toJson((BaseComponent) obj);

		else if (obj instanceof BaseComponent[])
			return Remain.toJson((BaseComponent[]) obj);

		else if (obj instanceof HoverEvent) {
			final HoverEvent event = (HoverEvent) obj;

			return SerializedMap.ofArray("Action", event.getAction(), "Value", event.getValue()).serialize();
		}

		else if (obj instanceof ClickEvent) {
			final ClickEvent event = (ClickEvent) obj;

			return SerializedMap.ofArray("Action", event.getAction(), "Value", event.getValue()).serialize();
		}

		else if (obj instanceof Path)
			throw new FoException("Cannot serialize Path " + obj + ", did you mean to convert it into a name?");

		else if (obj instanceof Iterable || obj.getClass().isArray() || obj instanceof IsInList) {
			final List<Object> serialized = new ArrayList<>();

			if (obj instanceof Iterable || obj instanceof IsInList)
				for (final Object element : obj instanceof IsInList ? ((IsInList<?>) obj).getList() : (Iterable<?>) obj)
					serialized.add(serialize(element));

			else
				for (final Object element : (Object[]) obj)
					serialized.add(serialize(element));

			return serialized;

		} else if (obj instanceof StrictMap) {
			final StrictMap<Object, Object> oldMap = (StrictMap<Object, Object>) obj;
			final StrictMap<Object, Object> newMap = new StrictMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;

		} else if (obj instanceof Map) {
			final Map<Object, Object> oldMap = (Map<Object, Object>) obj;
			final Map<Object, Object> newMap = new LinkedHashMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;

		} else if (obj instanceof YamlConfig)
			throw new SerializeFailedException("Called serialize for YamlConfig's '" + obj.getClass().getSimpleName()
					+ "' but failed, if you're trying to save it make it implement ConfigSerializable!");

		else if (obj instanceof Integer || obj instanceof Double || obj instanceof Float || obj instanceof Long || obj instanceof Short
				|| obj instanceof String || obj instanceof Boolean || obj instanceof Map
				|| obj instanceof ItemStack
				|| obj instanceof MemorySection
				|| obj instanceof Pattern)
			return obj;

		else if (obj instanceof ConfigurationSerializable)
			return ((ConfigurationSerializable) obj).serialize();

		throw new SerializeFailedException("Does not know how to serialize " + obj.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + obj);
	}

	/**
	 * Converts a {@link Location} into "world x y z yaw pitch" string
	 * Decimals not supported, use {@link #deserializeLocationD(Object)} for them
	 *
	 * @param loc
	 * @return
	 */
	public static String serializeLoc(final Location loc) {
		return loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + (loc.getPitch() != 0F || loc.getYaw() != 0F ? " " + Math.round(loc.getYaw()) + " " + Math.round(loc.getPitch()) : "");
	}

	/**
	 * Converts a {@link Location} into "world x y z yaw pitch" string with decimal support
	 * Unused, you have to call this in your save() method otherwise we remove decimals and use the above method
	 *
	 * @param loc
	 * @return
	 */
	public static String serializeLocD(final Location loc) {
		return loc.getWorld().getName() + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + (loc.getPitch() != 0F || loc.getYaw() != 0F ? " " + loc.getYaw() + " " + loc.getPitch() : "");
	}

	/**
	 * Converts a {@link PotionEffect} into a "type duration amplifier" string
	 *
	 * @param effect
	 * @return
	 */
	private static String serializePotionEffect(final PotionEffect effect) {
		return effect.getType().getName() + " " + effect.getDuration() + " " + effect.getAmplifier();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting stored strings from your files back into classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Attempts to convert the given object into a class
	 * <p>
	 * Example: Call deserialize(Location.class, "worldName 5 -1 47") to convert that into a Bukkit location object
	 *
	 * @param <T>
	 * @param classOf
	 * @param object
	 * @return
	 */
	public static <T> T deserialize(@NonNull final Class<T> classOf, @NonNull final Object object) {
		return deserialize(classOf, object, (Object[]) null);
	}

	/**
	 * Please see {@link #deserialize(Class, Object)}, plus that this method
	 * allows you to parse through more arguments to the static deserialize method
	 *
	 * @param <T>
	 * @param classOf
	 * @param object
	 * @param deserializeParameters, use more variables in the deserialize method
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> T deserialize(@NonNull final Class<T> classOf, @NonNull Object object, final Object... deserializeParameters) {

		final SerializedMap map = SerializedMap.of(object);

		// Step 1 - Search for basic deserialize(SerializedMap) method
		Method deserializeMethod = ReflectionUtil.getMethod(classOf, "deserialize", SerializedMap.class);

		if (deserializeMethod != null && deserializeParameters == null) {
			try {
				return ReflectionUtil.invokeStatic(deserializeMethod, map);

			} catch (final ReflectionException ex) {
				Common.throwError(ex, "Could not deserialize " + classOf + " from data: " + map);
			}
		}

		// Step 2 - Search for our deserialize(Params[], SerializedMap) method
		if (deserializeParameters != null) {
			final List<Class<?>> joinedClasses = new ArrayList<>();

			{ // Build parameters
				joinedClasses.add(SerializedMap.class);

				for (final Object param : deserializeParameters)
					joinedClasses.add(param.getClass());
			}

			deserializeMethod = ReflectionUtil.getMethod(classOf, "deserialize", joinedClasses.toArray(new Class[joinedClasses.size()]));

			final List<Object> joinedParams = new ArrayList<>();

			{ // Build parameter instances
				joinedParams.add(map);

				for (final Object param : deserializeParameters)
					joinedParams.add(param);
			}

			if (deserializeMethod != null) {
				Valid.checkBoolean(joinedClasses.size() == joinedParams.size(), "static deserialize method arguments length " + joinedClasses.size() + " != given params " + joinedParams.size());

				return ReflectionUtil.invokeStatic(deserializeMethod, joinedParams.toArray());
			}
		}

		// Step 3 - Search for "getByName" method used by us or some Bukkit classes such as Enchantment
		if (deserializeMethod == null && object instanceof String && (classOf != Enchantment.class && classOf != PotionEffectType.class)) {
			deserializeMethod = ReflectionUtil.getMethod(classOf, "getByName", String.class);

			if (deserializeMethod != null)
				return ReflectionUtil.invokeStatic(deserializeMethod, object);
		}

		// Step 4 - If there is no deserialize method, just deserialize the given object
		if (object != null)

			if (classOf == String.class)
				object = object.toString();

			else if (classOf == Integer.class)
				object = Integer.parseInt(object.toString());

			else if (classOf == Long.class)
				object = Long.decode(object.toString());

			else if (classOf == Double.class)
				object = Double.parseDouble(object.toString());

			else if (classOf == Float.class)
				object = Float.parseFloat(object.toString());

			else if (classOf == Boolean.class)
				object = Boolean.parseBoolean(object.toString());

			else if (classOf == SerializedMap.class)
				object = SerializedMap.of(object);

			else if (classOf == Location.class)
				object = deserializeLocation(object);

			else if (classOf == PotionEffectType.class)
				object = PotionEffectType.getByName(object.toString());

			else if (classOf == PotionEffect.class)
				object = deserializePotionEffect(object);

			else if (classOf == SimpleTime.class)
				object = SimpleTime.from(object.toString());

			else if (classOf == SimpleSound.class)
				object = new SimpleSound(object.toString());

			else if (classOf == RangedValue.class)
				object = RangedValue.parse(object.toString());

			else if (classOf == RangedSimpleTime.class)
				object = RangedSimpleTime.parse(object.toString());

			else if (classOf == net.md_5.bungee.api.ChatColor.class)
				throw new FoException("Instead of net.md_5.bungee.api.ChatColor, use our CompChatColor");

			else if (classOf == CompChatColor.class)
				object = CompChatColor.of(object.toString());

			else if (classOf == UUID.class)
				object = UUID.fromString(object.toString());

			else if (classOf == BaseComponent.class) {
				final BaseComponent[] deserialized = Remain.toComponent(object.toString());
				Valid.checkBoolean(deserialized.length == 1, "Failed to deserialize into singular BaseComponent: " + object);

				object = deserialized[0];

			} else if (classOf == BaseComponent[].class)
				object = Remain.toComponent(object.toString());

			else if (classOf == HoverEvent.class) {
				final SerializedMap serialized = SerializedMap.of(object);
				final HoverEvent.Action action = serialized.get("Action", HoverEvent.Action.class);
				final BaseComponent[] value = serialized.get("Value", BaseComponent[].class);

				object = new HoverEvent(action, value);
			}

			else if (classOf == ClickEvent.class) {
				final SerializedMap serialized = SerializedMap.of(object);

				final ClickEvent.Action action = serialized.get("Action", ClickEvent.Action.class);
				final String value = serialized.getString("Value");

				object = new ClickEvent(action, value);
			}

			else if (Enchantment.class.isAssignableFrom(classOf)) {
				String name = object.toString().toLowerCase();
				Enchantment enchant = Enchantment.getByName(name);

				if (enchant == null) {
					name = EnchantmentWrapper.toBukkit(name);
					enchant = Enchantment.getByName(name);

					if (enchant == null)
						enchant = Enchantment.getByName(name.toLowerCase());
				}

				Valid.checkNotNull(enchant, "Invalid enchantment '" + name + "'! For valid names, see: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/enchantments/Enchantment.html");
				object = enchant;
			}

			else if (PotionEffectType.class.isAssignableFrom(classOf)) {
				final String name = PotionWrapper.getBukkitName(object.toString());
				final PotionEffectType potion = PotionEffectType.getByName(name);

				Valid.checkNotNull(potion, "Invalid potion '" + name + "'! For valid names, see: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html");
				object = potion;
			}

			else if (Enum.class.isAssignableFrom(classOf)) {
				object = ReflectionUtil.lookupEnum((Class<Enum>) classOf, object.toString());

				if (object == null)
					return null;
			}

			else if (Color.class.isAssignableFrom(classOf)) {
				object = CompChatColor.of(object.toString()).getColor();

			} else if (List.class.isAssignableFrom(classOf) && object instanceof List) {
				// Good

			} else if (Map.class.isAssignableFrom(classOf) && object instanceof Map) {
				// Good

			} else if (ConfigurationSerializable.class.isAssignableFrom(classOf) && object instanceof ConfigurationSerializable) {
				// Good

			} else if (classOf.isArray()) {
				final Class<?> arrayType = classOf.getComponentType();
				T[] array;

				if (object instanceof List) {
					final List<?> rawList = (List<?>) object;
					array = (T[]) Array.newInstance(classOf.getComponentType(), rawList.size());

					for (int i = 0; i < rawList.size(); i++) {
						final Object element = rawList.get(i);

						array[i] = element == null ? null : (T) deserialize(arrayType, element, (Object[]) null);
					}
				}

				else {
					final Object[] rawArray = (Object[]) object;
					array = (T[]) Array.newInstance(classOf.getComponentType(), rawArray.length);

					for (int i = 0; i < array.length; i++)
						array[i] = rawArray[i] == null ? null : (T) deserialize(classOf.getComponentType(), rawArray[i], (Object[]) null);
				}

				return (T) array;

			} else if (classOf == Object.class) {
				// pass through

			} else
				throw new SerializeFailedException("Unable to deserialize " + classOf.getSimpleName() + ", lacking static deserialize method! Data: " + object);

		return (T) object;

	}

	/**
	 * Converts a string into location, see {@link #deserializeLocation(Object)} for how strings are saved
	 * Decimals not supported, use {@link #deserializeLocationD(Object)} to use them
	 *
	 * @param raw
	 * @return
	 */
	public static Location deserializeLocation(Object raw) {
		if (raw == null)
			return null;

		if (raw instanceof Location)
			return (Location) raw;

		raw = raw.toString().replace("\"", "");

		final String[] parts = raw.toString().contains(", ") ? raw.toString().split(", ") : raw.toString().split(" ");
		Valid.checkBoolean(parts.length == 4 || parts.length == 6, "Expected location (String) but got " + raw.getClass().getSimpleName() + ": " + raw);

		final String world = parts[0];
		final World bukkitWorld = Bukkit.getWorld(world);
		if (bukkitWorld == null)
			throw new InvalidWorldException("Location with invalid world '" + world + "': " + raw + " (Doesn't exist)", world);

		final int x = Integer.parseInt(parts[1]), y = Integer.parseInt(parts[2]), z = Integer.parseInt(parts[3]);
		final float yaw = Float.parseFloat(parts.length == 6 ? parts[4] : "0"), pitch = Float.parseFloat(parts.length == 6 ? parts[5] : "0");

		return new Location(bukkitWorld, x, y, z, yaw, pitch);
	}

	/**
	 * Converts a string into a location with decimal support
	 * Unused but you can use this for your own parser storing exact decimals
	 *
	 * @param raw
	 * @return
	 */
	public static Location deserializeLocationD(Object raw) {
		if (raw == null)
			return null;

		if (raw instanceof Location)
			return (Location) raw;

		raw = raw.toString().replace("\"", "");

		final String[] parts = raw.toString().contains(", ") ? raw.toString().split(", ") : raw.toString().split(" ");
		Valid.checkBoolean(parts.length == 4 || parts.length == 6, "Expected location (String) but got " + raw.getClass().getSimpleName() + ": " + raw);

		final String world = parts[0];
		final World bukkitWorld = Bukkit.getWorld(world);

		if (bukkitWorld == null)
			throw new InvalidWorldException("Location with invalid world '" + world + "': " + raw + " (Doesn't exist)", world);

		final double x = Double.parseDouble(parts[1]), y = Double.parseDouble(parts[2]), z = Double.parseDouble(parts[3]);
		final float yaw = Float.parseFloat(parts.length == 6 ? parts[4] : "0"), pitch = Float.parseFloat(parts.length == 6 ? parts[5] : "0");

		return new Location(bukkitWorld, x, y, z, yaw, pitch);
	}

	/**
	 * Convert a raw object back to {@link PotionEffect}
	 *
	 * @param raw
	 * @return
	 */
	private static PotionEffect deserializePotionEffect(final Object raw) {
		if (raw == null)
			return null;

		if (raw instanceof PotionEffect)
			return (PotionEffect) raw;

		final String[] parts = raw.toString().split(" ");
		Valid.checkBoolean(parts.length == 3, "Expected PotionEffect (String) but got " + raw.getClass().getSimpleName() + ": " + raw);

		final String typeRaw = parts[0];
		final PotionEffectType type = PotionEffectType.getByName(typeRaw);

		final int duration = Integer.parseInt(parts[1]);
		final int amplifier = Integer.parseInt(parts[2]);

		return new PotionEffect(type, duration, amplifier);
	}

	/**
	 * Deserializes a list containing maps
	 *
	 * @param <T>
	 * @param listOfObjects
	 * @param asWhat
	 * @return
	 */
	public static <T extends ConfigSerializable> List<T> deserializeMapList(final Object listOfObjects, final Class<T> asWhat) {
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
	public static <T extends ConfigSerializable> T deserializeMap(final Object rawMap, final Class<T> asWhat) {
		if (rawMap == null)
			return null;

		Valid.checkBoolean(rawMap instanceof Map, "The object to deserialize must be map, but got: " + rawMap.getClass());

		final Map<String, Object> map = (Map<String, Object>) rawMap;
		final Method deserialize;

		try {
			deserialize = asWhat.getMethod("deserialize", SerializedMap.class);
			Valid.checkBoolean(Modifier.isPublic(deserialize.getModifiers()) && Modifier.isStatic(deserialize.getModifiers()), asWhat + " is missing public 'public static T deserialize()' method");

		} catch (final NoSuchMethodException ex) {
			Common.throwError(ex, "Class lacks a final method deserialize(SerializedMap) metoda. Tried: " + asWhat.getSimpleName());
			return null;
		}

		final Object invoked;

		try {
			invoked = deserialize.invoke(null, SerializedMap.of(map));
		} catch (final ReflectiveOperationException e) {
			Common.throwError(e, "Error calling " + deserialize.getName() + " as " + asWhat.getSimpleName() + " with data " + map);
			return null;
		}

		Valid.checkBoolean(invoked.getClass().isAssignableFrom(asWhat), invoked.getClass().getSimpleName() + " != " + asWhat.getSimpleName());
		return (T) invoked;
	}

	/**
	 * Thrown when cannot serialize an object because it failed to determine its type
	 */
	public static class SerializeFailedException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public SerializeFailedException(String reason) {
			super(reason);
		}
	}

	/**
	 * A simple class holding some of the potion names
	 */
	@RequiredArgsConstructor
	protected static enum PotionWrapper {

		SLOW("SLOW", "Slowness"),
		STRENGTH("INCREASE_DAMAGE"),
		JUMP_BOOST("JUMP"),
		INSTANT_HEAL("INSTANT_HEALTH"),
		REGEN("REGENERATION");

		private final String bukkitName;
		private final String minecraftName;

		private PotionWrapper(String bukkitName) {
			this(bukkitName, null);
		}

		protected static String getLocalizedName(String name) {
			String localizedName = name;

			for (final PotionWrapper e : values())
				if (name.toUpperCase().replace(" ", "_").equals(e.bukkitName)) {
					localizedName = e.getMinecraftName();

					break;
				}

			return WordUtils.capitalizeFully(localizedName.replace("_", " "));
		}

		protected static String getBukkitName(String name) {
			name = name.toUpperCase().replace(" ", "_");

			for (final PotionWrapper wrapper : values())
				if (wrapper.toString().equalsIgnoreCase(name) || wrapper.minecraftName != null && wrapper.minecraftName.equalsIgnoreCase(name))
					return wrapper.bukkitName;

			return name;
		}

		public String getMinecraftName() {
			return Common.getOrDefault(minecraftName, bukkitName);
		}
	}

	/**
	 * A simple class holding some of the enchantments names
	 */
	@RequiredArgsConstructor
	protected static enum EnchantmentWrapper {
		PROTECTION("PROTECTION_ENVIRONMENTAL"),
		FIRE_PROTECTION("PROTECTION_FIRE"),
		FEATHER_FALLING("PROTECTION_FALL"),
		BLAST_PROTECTION("PROTECTION_EXPLOSIONS"),
		PROJECTILE_PROTECTION("PROTECTION_PROJECTILE"),
		RESPIRATION("OXYGEN"),
		AQUA_AFFINITY("WATER_WORKER"),
		THORN("THORNS"),
		CURSE_OF_VANISHING("VANISHING_CURSE"),
		CURSE_OF_BINDING("BINDING_CURSE"),
		SHARPNESS("DAMAGE_ALL"),
		SMITE("DAMAGE_UNDEAD"),
		BANE_OF_ARTHROPODS("DAMAGE_ARTHROPODS"),
		LOOTING("LOOT_BONUS_MOBS"),
		SWEEPING_EDGE("SWEEPING"),
		EFFICIENCY("DIG_SPEED"),
		UNBREAKING("DURABILITY"),
		FORTUNE("LOOT_BONUS_BLOCKS"),
		POWER("ARROW_DAMAGE"),
		PUNCH("ARROW_KNOCKBACK"),
		FLAME("ARROW_FIRE"),
		INFINITY("ARROW_INFINITE"),
		LUCK_OF_THE_SEA("LUCK");

		private final String bukkitName;

		protected static String toBukkit(String name) {
			name = name.toUpperCase().replace(" ", "_");

			for (final EnchantmentWrapper e : values())
				if (e.toString().equals(name))
					return e.bukkitName;

			return name;
		}

		protected static String toMinecraft(String name) {
			name = name.toUpperCase().replace(" ", "_");

			for (final EnchantmentWrapper e : values())
				if (name.equals(e.bukkitName))
					return ItemUtil.bountifyCapitalized(e);

			return WordUtils.capitalizeFully(name);
		}

		public String getBukkitName() {
			return bukkitName != null ? bukkitName : name();
		}
	}
}
