package org.mineacademy.fo;

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictCollection;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.RangedSimpleTime;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.ConfigSection;

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
	 * @param object
	 * @return
	 */
	public static Object serialize(Object object) {

		synchronized (serializers) {

			if (object == null)
				return null;

			object = Remain.getRootOfSectionPathData(object);

			if (serializers.containsKey(object.getClass()))
				return serializers.get(object.getClass()).apply(object);

			if (object instanceof ConfigSerializable)
				return serialize(((ConfigSerializable) object).serialize().serialize());

			else if (object instanceof StrictCollection)
				return serialize(((StrictCollection) object).serialize());

			else if (object instanceof ChatColor)
				return ((ChatColor) object).name();

			else if (object instanceof CompChatColor)
				return ((CompChatColor) object).toSaveableString();

			else if (object instanceof net.md_5.bungee.api.ChatColor) {
				final net.md_5.bungee.api.ChatColor color = ((net.md_5.bungee.api.ChatColor) object);

				return MinecraftVersion.atLeast(V.v1_16) ? color.toString() : color.name();
			}

			else if (object instanceof CompMaterial)
				return object.toString();

			else if (object instanceof Location)
				return serializeLoc((Location) object);

			else if (object instanceof BoxedMessage) {
				final String message = ((BoxedMessage) object).getMessage();

				return message == null || "".equals(message) || "null".equals(message) ? null : message;

			} else if (object instanceof UUID)
				return object.toString();

			else if (object instanceof Enum<?>)
				return object.toString();

			else if (object instanceof CommandSender)
				return ((CommandSender) object).getName();

			else if (object instanceof World)
				return ((World) object).getName();

			else if (object instanceof PotionEffectType)
				return ((PotionEffectType) object).getName();

			else if (object instanceof PotionEffect)
				return serializePotionEffect((PotionEffect) object);

			else if (object instanceof ItemCreator)
				return serialize(((ItemCreator) object).make());

			else if (object instanceof SimpleTime)
				return ((SimpleTime) object).getRaw();

			else if (object instanceof SimpleSound)
				return ((SimpleSound) object).toString();

			else if (object instanceof Color)
				return "#" + ((Color) object).getRGB();

			else if (object instanceof RangedValue)
				return ((RangedValue) object).toLine();

			else if (object instanceof RangedSimpleTime)
				return ((RangedSimpleTime) object).toLine();

			else if (object instanceof BaseComponent)
				return Remain.toJson((BaseComponent) object);

			else if (object instanceof BaseComponent[])
				return Remain.toJson((BaseComponent[]) object);

			else if (object instanceof HoverEvent) {
				final HoverEvent event = (HoverEvent) object;

				return SerializedMap.ofArray("Action", event.getAction(), "Value", event.getValue()).serialize();
			}

			else if (object instanceof ClickEvent) {
				final ClickEvent event = (ClickEvent) object;

				return SerializedMap.ofArray("Action", event.getAction(), "Value", event.getValue()).serialize();
			}

			else if (object instanceof Path)
				throw new FoException("Cannot serialize Path " + object + ", did you mean to convert it into a name?");

			else if (object instanceof Iterable || object.getClass().isArray() || object instanceof IsInList) {
				final List<Object> serialized = new ArrayList<>();

				if (object instanceof Iterable || object instanceof IsInList)
					for (final Object element : object instanceof IsInList ? ((IsInList<?>) object).getList() : (Iterable<?>) object)
						serialized.add(serialize(element));

				else
					for (final Object element : (Object[]) object)
						serialized.add(serialize(element));

				return serialized;

			} else if (object instanceof StrictMap) {
				final StrictMap<Object, Object> oldMap = (StrictMap<Object, Object>) object;
				final StrictMap<Object, Object> newMap = new StrictMap<>();

				for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
					newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

				return newMap;

			} else if (object instanceof Map) {
				final Map<Object, Object> oldMap = (Map<Object, Object>) object;
				final Map<Object, Object> newMap = new LinkedHashMap<>();

				for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
					newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

				return newMap;
			}

			else if (object instanceof MemorySection)
				return serialize(Common.getMapFromSection(object));

			else if (object instanceof ConfigSection)
				return serialize(((ConfigSection) object).getValues(true));

			else if (object instanceof Pattern)
				return ((Pattern) object).pattern();

			else if (object instanceof Integer || object instanceof Double || object instanceof Float || object instanceof Long || object instanceof Short
					|| object instanceof String || object instanceof Boolean || object instanceof Character)
				return object;

			else if (object instanceof ConfigurationSerializable)
				return ((ConfigurationSerializable) object).serialize();

			throw new SerializeFailedException("Does not know how to serialize " + object.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + object);
		}
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
	 * @param parameters
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> T deserialize(@NonNull final Class<T> classOf, @NonNull Object object, final Object... parameters) {

		synchronized (serializers) {
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

			else if (classOf == BoxedMessage.class)
				object = new BoxedMessage(object.toString());

			else if (classOf == Location.class)
				object = deserializeLocation(object);

			else if (classOf == PotionEffectType.class)
				object = PotionEffectType.getByName(object.toString());

			else if (classOf == PotionEffect.class)
				object = deserializePotionEffect(object);

			else if (classOf == SimpleTime.class)
				object = SimpleTime.from(object.toString());

			else if (classOf == CompMaterial.class)
				object = CompMaterial.fromStringStrict(object.toString());

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

			else if (classOf == ItemStack.class)
				object = deserializeItemStack(object);

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
					name = name.toUpperCase();

					enchant = Enchantment.getByName(name);
				}

				if (enchant == null) {
					name = EnchantmentWrapper.toBukkit(name);
					enchant = Enchantment.getByName(name);

					if (enchant == null)
						enchant = Enchantment.getByName(name.toLowerCase());

					if (enchant == null)
						enchant = Enchantment.getByName(name.toUpperCase());
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

			} else if (Map.class.isAssignableFrom(classOf)) {
				if (object instanceof Map)
					return (T) object;

				if (object instanceof MemorySection)
					return (T) Common.getMapFromSection(object);

				if (object instanceof ConfigSection)
					return (T) ((ConfigSection) object).getValues(false);

				throw new SerializeFailedException("Does not know how to turn " + object.getClass().getSimpleName() + " into a Map! (Keep in mind we can only serialize into Map<Object/String, Object> Data: " + object);

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

			}

			// Try to call our own serializers
			else if (ConfigSerializable.class.isAssignableFrom(classOf)) {
				if (parameters != null && parameters.length > 0) {
					final List<Class<?>> argumentClasses = new ArrayList<>();
					final List<Object> arguments = new ArrayList<>();

					// Build parameters
					argumentClasses.add(SerializedMap.class);
					for (final Object param : parameters)
						argumentClasses.add(param.getClass());

					// Build parameter instances
					arguments.add(SerializedMap.of(object));
					Collections.addAll(arguments, parameters);

					// Find deserialize(SerializedMap, args[]) method
					final Method deserialize = ReflectionUtil.getMethod(classOf, "deserialize", argumentClasses.toArray(new Class[argumentClasses.size()]));

					Valid.checkNotNull(deserialize,
							"Expected " + classOf.getSimpleName() + " to have a public static deserialize(SerializedMap, " + Common.join(argumentClasses) + ") method to deserialize: " + object + " when params were given: " + Common.join(parameters));

					Valid.checkBoolean(argumentClasses.size() == arguments.size(),
							classOf.getSimpleName() + "#deserialize(SerializedMap, " + argumentClasses.size() + " args) expected, " + arguments.size() + " given to deserialize: " + object);

					return ReflectionUtil.invokeStatic(deserialize, arguments.toArray());
				}

				final Method deserialize = ReflectionUtil.getMethod(classOf, "deserialize", SerializedMap.class);

				if (deserialize != null)
					return ReflectionUtil.invokeStatic(deserialize, SerializedMap.of(object));

				throw new SerializeFailedException("Unable to deserialize " + classOf.getSimpleName()
						+ ", please write 'public static deserialize(SerializedMap map) or deserialize(SerializedMap map, X arg1, Y arg2, etc.) method to deserialize: " + object);
			}

			// Step 3 - Search for "getByName" method used by us or some Bukkit classes such as Enchantment
			else if (object instanceof String) {
				final Method method = ReflectionUtil.getMethod(classOf, "getByName", String.class);

				if (method != null)
					return ReflectionUtil.invokeStatic(method, object);
			}

			else if (classOf == Object.class) {
				// Good
			}

			else
				throw new SerializeFailedException("Does not know how to turn " + classOf + " into a serialized object from data: " + object);

			return (T) object;
		}
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
	 * Attempts to turn the given item or map into an item
	 *
	 * @param obj
	 * @return
	 */
	private static ItemStack deserializeItemStack(Object obj) {
		try {
			Valid.checkNotNull(obj);

			if (obj instanceof ItemStack)
				return (ItemStack) obj;

			final SerializedMap map = SerializedMap.of(obj);

			final ItemStack item = ItemStack.deserialize(map.asMap());
			final SerializedMap meta = map.getMap("meta");

			if (meta != null) {

				try {
					final Class<?> cl = ReflectionUtil.getOBCClass("inventory." + (meta.containsKey("spawnedType") ? "CraftMetaSpawnEgg" : "CraftMetaItem"));
					final Constructor<?> c = cl.getDeclaredConstructor(Map.class);
					c.setAccessible(true);

					final Object craftMeta = c.newInstance((Map<String, ?>) meta.serialize());

					if (craftMeta instanceof ItemMeta)
						item.setItemMeta((ItemMeta) craftMeta);

				} catch (final Throwable t) {

					// We have to manually deserialize metadata :(
					final ItemMeta itemMeta = item.getItemMeta();

					final String display = meta.containsKey("display-name") ? meta.getString("display-name") : null;

					if (display != null)
						itemMeta.setDisplayName(display);

					final List<String> lore = meta.containsKey("lore") ? meta.getStringList("lore") : null;

					if (lore != null)
						itemMeta.setLore(lore);

					final SerializedMap enchants = meta.containsKey("enchants") ? meta.getMap("enchants") : null;

					if (enchants != null)
						for (final Map.Entry<String, Object> entry : enchants.entrySet()) {
							final Enchantment enchantment = Enchantment.getByName(entry.getKey());
							final int level = (int) entry.getValue();

							itemMeta.addEnchant(enchantment, level, true);
						}

					final List<String> itemFlags = meta.containsKey("ItemFlags") ? meta.getStringList("ItemFlags") : null;

					if (itemFlags != null)
						for (final String flag : itemFlags)
							try {
								itemMeta.addItemFlags(ItemFlag.valueOf(flag));
							} catch (final Exception ex) {
								// Likely not MC compatible, ignore
							}

					item.setItemMeta(itemMeta);
				}
			}

			return item;

		} catch (final Throwable t) {
			t.printStackTrace();

			return null;
		}
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
	protected enum PotionWrapper {

		SLOW("SLOW", "Slowness"),
		STRENGTH("INCREASE_DAMAGE"),
		JUMP_BOOST("JUMP"),
		INSTANT_HEAL("INSTANT_HEALTH"),
		REGEN("REGENERATION");

		private final String bukkitName;
		private final String minecraftName;

		PotionWrapper(String bukkitName) {
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
	protected enum EnchantmentWrapper {
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
