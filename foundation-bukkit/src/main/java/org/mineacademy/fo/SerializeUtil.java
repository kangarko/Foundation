package org.mineacademy.fo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.jsonsimple.JSONArray;
import org.mineacademy.fo.jsonsimple.JSONParser;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.JsonItemStack;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializeUtil extends SerializeUtilCore {

	static {
		SerializeUtil.addCustomSerializer(new Serializer() {

			@Override
			public Object serialize(Mode mode, Object object) {
				if (object instanceof ChatColor)
					return ((ChatColor) object).name();

				else if (object instanceof net.md_5.bungee.api.ChatColor)
					return ((net.md_5.bungee.api.ChatColor) object).name();

				else if (object instanceof Location)
					return SerializeUtil.serializeLoc((Location) object);

				else if (object instanceof CommandSender)
					return ((CommandSender) object).getName();

				else if (object instanceof World)
					return ((World) object).getName();

				else if (object instanceof Entity)
					return Remain.getName((Entity) object);

				else if (object instanceof PotionEffectType)
					return ((PotionEffectType) object).getName();

				else if (object instanceof PotionEffect)
					return SerializeUtil.serializePotionEffect((PotionEffect) object);

				else if (object instanceof Enchantment)
					return ((Enchantment) object).getName();

				else if (object instanceof ItemCreator)
					return SerializeUtilCore.serialize(mode, ((ItemCreator) object).make());

				else if (object instanceof SimpleSound)
					return ((SimpleSound) object).toString();

				//else if (object instanceof MemorySection)
				//	return SerializeUtil.serialize(mode, Common.getMapFromSection(object));

				else if (object instanceof ConfigurationSerializable) {
					if (object instanceof ItemStack) {
						return mode == SerializeUtil.Mode.JSON ? JsonItemStack.toJson((ItemStack) object) : object;

					} else if (mode == SerializeUtil.Mode.JSON)
						throw new FoException("Serializing " + object.getClass().getSimpleName() + " to JSON is not implemented! Please serialize it to string manually first!");

					return object;
				}

				return null;
			}

			@Override
			public <T> T deserialize(Mode mode, Class<T> classOf, Object object, Object... parameters) {
				if (classOf == Location.class)
					object = SerializeUtil.deserializeLocation(object);

				else if (classOf == PotionEffectType.class)
					object = PotionEffectType.getByName(object.toString());

				else if (classOf == PotionEffect.class)
					object = SerializeUtil.deserializePotionEffect(object);

				else if (classOf == CompMaterial.class)
					object = CompMaterial.fromStringStrict(object.toString());

				else if (classOf == SimpleSound.class)
					object = new SimpleSound(object.toString());

				else if (classOf == ItemStack.class)
					object = SerializeUtil.deserializeItemStack(mode, object);

				else if (Enchantment.class.isAssignableFrom(classOf)) {
					String name = object.toString().toLowerCase();
					Enchantment enchant = Enchantment.getByName(name);

					if (enchant == null) {
						name = name.toUpperCase();

						enchant = Enchantment.getByName(name);
					}

					if (enchant == null) {
						name = object.toString(); // TODO EnchantmentWrapper.toBukkit(name);
						enchant = Enchantment.getByName(name);

						if (enchant == null)
							enchant = Enchantment.getByName(name.toLowerCase());

						if (enchant == null)
							enchant = Enchantment.getByName(name.toUpperCase());
					}

					ValidCore.checkNotNull(enchant, "Invalid enchantment '" + name + "'! For valid names, see: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/enchantments/Enchantment.html");
					object = enchant;
				}

				else if (PotionEffectType.class.isAssignableFrom(classOf)) {
					final String name = object.toString(); // TODO PotionWrapper.getBukkitName(object.toString());
					final PotionEffectType potion = PotionEffectType.getByName(name);

					ValidCore.checkNotNull(potion, "Invalid potion '" + name + "'! For valid names, see: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html");
					object = potion;
				}

				//else if (Map.class.isAssignableFrom(classOf)) {
				//	if (object instanceof MemorySection)
				//		return (T) Common.getMapFromSection(object);
				//}

				else if (ConfigurationSerializable.class.isAssignableFrom(classOf) && object instanceof ConfigurationSerializable) {
					if (mode == SerializeUtil.Mode.JSON)
						throw new FoException("Deserializing JSON into " + classOf + " is not implemented, please do it manually");
					else
						return (T) object;

				} else if (classOf.isArray() && !(object instanceof List) && ItemStack[].class.isAssignableFrom(classOf)) {
					// The object is a raw json string. It's a list of json itemstacks. We need to call JsonItemStack#fromJson for each element in the list.
					final List<ItemStack> list = new ArrayList<>();

					if (mode == SerializeUtil.Mode.JSON) {
						final JSONArray jsonList = JSONParser.deserialize(object.toString(), new JSONArray());

						for (final Object element : jsonList)
							list.add(element == null ? null : JsonItemStack.fromJson(element.toString()));
					} else
						throw new FoException("Cannot deserialize non-JSON ItemStack[]");

					return (T) list.toArray(new ItemStack[list.size()]);
				}

				return null;
			}
		});
	}

	/**
	 * Converts a {@link Location} into "world x y z yaw pitch" string
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
	/*private static String serializeLocD(final Location loc) {
		return loc.getWorld().getName() + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + (loc.getPitch() != 0F || loc.getYaw() != 0F ? " " + loc.getYaw() + " " + loc.getPitch() : "");
	}*/

	/**
	 * Converts a string into location, see {@link #deserializeLocation(Object)} for how strings are saved
	 *
	 * @param locationOrLine
	 * @return
	 */
	public static Location deserializeLocation(Object locationOrLine) {
		if (locationOrLine == null)
			return null;

		if (locationOrLine instanceof Location)
			return (Location) locationOrLine;

		locationOrLine = locationOrLine.toString().replace("\"", "");

		final String[] parts = locationOrLine.toString().contains(", ") ? locationOrLine.toString().split(", ") : locationOrLine.toString().split(" ");
		ValidCore.checkBoolean(parts.length == 4 || parts.length == 6, "Expected location (String) but got " + locationOrLine.getClass().getSimpleName() + ": " + locationOrLine);

		final String world = parts[0];
		final World bukkitWorld = Bukkit.getWorld(world);
		if (bukkitWorld == null)
			throw new InvalidWorldException("Location with invalid world '" + world + "': " + locationOrLine + " (Doesn't exist)", world);

		final double x = Double.parseDouble(parts[1]), y = Double.parseDouble(parts[2]), z = Double.parseDouble(parts[3]);
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
	/*private static Location deserializeLocationD(Object raw) {
		if (raw == null)
			return null;

		if (raw instanceof Location)
			return (Location) raw;

		raw = raw.toString().replace("\"", "");

		final String[] parts = raw.toString().contains(", ") ? raw.toString().split(", ") : raw.toString().split(" ");
		ValidCore.checkBoolean(parts.length == 4 || parts.length == 6, "Expected location (String) but got " + raw.getClass().getSimpleName() + ": " + raw);

		final String world = parts[0];
		final World bukkitWorld = Bukkit.getWorld(world);

		if (bukkitWorld == null)
			throw new InvalidWorldException("Location with invalid world '" + world + "': " + raw + " (Doesn't exist)", world);

		final double x = Double.parseDouble(parts[1]), y = Double.parseDouble(parts[2]), z = Double.parseDouble(parts[3]);
		final float yaw = Float.parseFloat(parts.length == 6 ? parts[4] : "0"), pitch = Float.parseFloat(parts.length == 6 ? parts[5] : "0");

		return new Location(bukkitWorld, x, y, z, yaw, pitch);
	}*/

	/**
	 * Turn the potion effect into a saveable String
	 *
	 * @param effect
	 * @return
	 */
	private static String serializePotionEffect(final PotionEffect effect) {
		return effect.getType().getName() + " " + effect.getDuration() + " " + effect.getAmplifier();
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
		ValidCore.checkBoolean(parts.length == 3, "Expected PotionEffect (String) but got " + raw.getClass().getSimpleName() + ": " + raw);

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
	private static ItemStack deserializeItemStack(@NonNull Mode mode, @NonNull Object obj) {
		try {

			if (obj instanceof ItemStack)
				return (ItemStack) obj;

			if (mode == Mode.JSON)
				return JsonItemStack.fromJson(obj.toString());

			final SerializedMap map = SerializedMap.of(obj);

			final ItemStack item = ItemStack.deserialize(map.asMap());
			final SerializedMap meta = map.getMap("meta");

			if (meta != null)
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

			return item;

		} catch (final Throwable t) {
			t.printStackTrace();

			return null;
		}
	}
}
