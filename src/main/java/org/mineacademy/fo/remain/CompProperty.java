package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.nbt.NBTEntity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A convenience class for applying "properies" to certain Bukkit classes
 * such as items or entities
 * <p>
 * This basically calls methods that are not available in all MC versions
 * and prevents errors in your plugin.
 * <p>
 * If they are not available, nothing is applied.
 */
@RequiredArgsConstructor
public enum CompProperty {

	// ItemMeta
	/**
	 * The unbreakable property of ItemMeta
	 */
	UNBREAKABLE(ItemMeta.class, boolean.class),

	// Entity
	/**
	 * The glowing entity property, currently only support white color
	 */
	GLOWING(Entity.class, boolean.class),

	/**
	 * The AI navigator entity property
	 */
	AI(Entity.class, boolean.class),

	/**
	 * The gravity entity property
	 */
	GRAVITY(Entity.class, boolean.class),

	/**
	 * Silent entity property that controls if the entity emits sounds
	 */
	SILENT(Entity.class, boolean.class),

	/**
	 * The god mode entity property
	 */
	INVULNERABLE(Entity.class, boolean.class);

	/**
	 * The class that this enum applies for, for example {@link Entity}
	 */
	@Getter
	private final Class<?> requiredClass;

	/**
	 * The "setter" field type, for example setSilent method accepts boolean
	 */
	private final Class<?> setterMethodType;

	private final Map<Class<?>, Boolean> isAvailable = new HashMap<>();
	private final Map<Class<?>, Method> cachedMethods = new HashMap<>();

	/**
	 * Apply the property to the entity. Class must be compatible with the {@link #getRequiredClass()} of this property.
	 * <p>
	 * Example: SILENT.apply(myZombieEntity, true)
	 *
	 * @param instance
	 * @param key
	 */
	public void apply(Object instance, Object key) {
		Valid.checkNotNull(instance, "instance is null!");
		Valid.checkBoolean(requiredClass.isAssignableFrom(instance.getClass()), this + " accepts " + requiredClass.getSimpleName() + ", not " + instance.getClass().getSimpleName());

		final Method method = getMethod(instance.getClass());

		if (method == null)
			this.applyLegacy(instance, key);

		else
			try {
				ReflectionUtil.invoke(method, instance, key);

			} catch (final Throwable t) {
				if (MinecraftVersion.olderThan(V.values()[0])) {
					this.applyLegacy(instance, key);

				} else
					// Print error when on latest MC version
					t.printStackTrace();
			}
	}

	private void applyLegacy(Object instance, Object key) {
		if (instance instanceof Entity) {
			final NBTEntity nbtEntity = new NBTEntity((Entity) instance);
			final boolean has = Boolean.parseBoolean(key.toString());

			if (this == INVULNERABLE)
				nbtEntity.setInteger("Invulnerable", has ? 1 : 0);

			else if (this == AI)
				nbtEntity.setInteger("NoAI", has ? 0 : 1);

			else if (this == CompProperty.GRAVITY)
				nbtEntity.setInteger("NoGravity", has ? 0 : 1);
		}

		if (Remain.hasItemMeta() && instance instanceof ItemMeta) {
			if (this == UNBREAKABLE)
				try {
					final boolean has = Boolean.parseBoolean(key.toString());

					final Method spigotMethod = instance.getClass().getMethod("spigot");
					spigotMethod.setAccessible(true);

					final Object spigot = spigotMethod.invoke(instance);

					final Method setUnbreakable = spigot.getClass().getMethod("setUnbreakable", boolean.class);
					setUnbreakable.setAccessible(true);

					setUnbreakable.invoke(spigot, has);

				} catch (final Throwable t) {
					if (MinecraftVersion.atLeast(V.v1_8))
						t.printStackTrace();
				}
		}
	}

	/**
	 * Can this property be used on this server for the given class? Class must be compatible with {@link #getRequiredClass()}
	 * <p>
	 * Class is for example {@link Entity}
	 *
	 * @param clazz
	 * @return
	 */
	public boolean isAvailable(Class<?> clazz) {

		if (this.isAvailable.containsKey(clazz))
			return this.isAvailable.get(clazz);

		return this.getMethod(clazz) != null;
	}

	// Automatically returns the correct getter or setter method for class
	private Method getMethod(Class<?> clazz) {

		if (this.isAvailable.containsKey(clazz) && !this.isAvailable.get(clazz))
			return null;

		Method method = this.cachedMethods.get(clazz);

		if (method == null)
			try {
				method = clazz.getMethod("set" + (toString().equals("AI") ? "AI" : WordUtils.capitalize(toString().toLowerCase())), setterMethodType);
				method.setAccessible(true);

				this.isAvailable.put(clazz, true);
				this.cachedMethods.put(clazz, method);

			} catch (final Throwable t) {
				this.isAvailable.put(clazz, false);

				return null;
			}

		return method;
	}
}