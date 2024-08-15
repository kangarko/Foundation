package org.mineacademy.fo;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.MissingEnumException;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.kyori.adventure.bossbar.BossBar;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtil extends ReflectionUtilCore {

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
	 * @deprecated Minecraft 1.17+ has a different path name,
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
	 * Return a tree set of classes from the plugin that extend the given class
	 *
	 * @param plugin
	 * @return
	 */
	public static List<Class<?>> getClasses(final Plugin plugin) {
		final List<Class<?>> found = new ArrayList<>();

		found.addAll(getClasses(plugin, null));

		return found;
	}

	/**
	 * Get all classes in the java plugin
	 *
	 * @param <T>
	 * @param plugin
	 * @param extendingClass
	 * @return
	 */
	@SneakyThrows
	public static <T> TreeSet<Class<T>> getClasses(@NonNull Plugin plugin, Class<T> extendingClass) {
		Valid.checkNotNull(plugin, "Plugin is null!");
		Valid.checkBoolean(JavaPlugin.class.isAssignableFrom(plugin.getClass()), "Plugin must be a JavaPlugin");

		// Get the plugin .jar
		final Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
		getFileMethod.setAccessible(true);

		final File pluginFile = (File) getFileMethod.invoke(plugin);

		return ReflectionUtilCore.getClasses(pluginFile, extendingClass);
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
		return lookupEnum(enumType, name, enumType.getSimpleName() + " value '" + name + "' is not found on Minecraft " + MinecraftVersion.getFullVersion() + "! Available: {available}");
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
		ValidCore.checkNotNull(enumType, "Type missing for " + name);
		ValidCore.checkNotNull(name, "Name missing for " + enumType);

		final String rawName = name.toUpperCase().replace(" ", "_");

		// Some compatibility workaround for ChatControl, Boss, CoreArena and other plugins
		// having these values in their default config. This prevents
		// malfunction on plugin's first load, in case it is loaded on an older MC version.
		{
			if (enumType == ChatColor.class && name.contains(ChatColor.COLOR_CHAR + "")) {
				return (E) ChatColor.getByChar(name.charAt(1));

			} else if (enumType == Biome.class) {
				if (MinecraftVersion.atLeast(V.v1_13))
					if (rawName.equalsIgnoreCase("ICE_MOUNTAINS"))
						name = "SNOWY_TAIGA";

			} else if (enumType == EntityType.class) {
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

			} else if (enumType == DamageCause.class) {
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

			} else if (enumType == BossBar.Overlay.class)
				name = name.toUpperCase().replace("SEGMENTED", "NOTCHED").replace("SOLID", "PROGRESS");
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

			throw new MissingEnumException(oldName, errMessage.replace("{available}", CommonCore.join(enumType.getEnumConstants(), ", ")));
		}

		return result;
	}

	/**
	 * Attempts to lookup an enum by its multiple names, typically the case for
	 * multiple MC versions where names have changed but enum class stayed the same.
	 *
	 * NOTE: For Material class, use our dedicated CompMaterial instead of this method.
	 *
	 * @param enumClass
	 * @param names
	 * @return
	 */
	public static <T extends Enum<T>> T lookupLegacyEnum(final Class<T> enumClass, String... names) {

		for (final String name : names) {
			final T foundEnum = lookupEnumSilent(enumClass, name);

			if (foundEnum != null)
				return foundEnum;
		}

		return null;
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
}
