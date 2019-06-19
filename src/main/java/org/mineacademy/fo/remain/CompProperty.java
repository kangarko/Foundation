package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A convenience class for applying "properies" to certain Bukkit classes
 * such as items or entities
 *
 * This basically calls methods that are not available in all MC versions
 * and prevents errors in your plugin.
 *
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

	/**
	 * Apply the property to the entity. Class must be compatible with {@link #requiredClass}
	 *
	 * Example: SILENT.apply(myZombieEntity, true)
	 *
	 * @param instance
	 * @param key
	 */
	public final void apply(Object instance, Object key) {
		Objects.requireNonNull(instance, "instance is null!");
		Validate.isTrue(requiredClass.isAssignableFrom(instance.getClass()), this + " accepts " + requiredClass.getSimpleName() + ", not " + instance.getClass().getSimpleName());

		try {
			final Method m = getMethod(instance.getClass());
			m.setAccessible(true);

			m.invoke(instance, key);

		} catch (final ReflectiveOperationException e) {
			if (e instanceof NoSuchMethodException && MinecraftVersion.olderThan(V.values()[0])) {
				// Pass through

			} else
				e.printStackTrace();
		}
	}

	/**
	 * Can this property be used on this server for the given class? Class must be compatible with {@link #requiredClass}
	 *
	 * Class is for example {@link Entity}
	 *
	 * @param clazz
	 * @return
	 */
	public final boolean isAvailable(Class<?> clazz) {
		try {
			getMethod(clazz);
		} catch (final ReflectiveOperationException e) {
			if (e instanceof NoSuchMethodException && MinecraftVersion.olderThan(V.values()[0]))
				return false;
		}

		return true;
	}

	// Automatically returns the correct getter or setter method for class
	private final Method getMethod(Class<?> clazz) throws ReflectiveOperationException {
		return clazz.getMethod("set" + (toString().equals("AI") ? "AI" : WordUtils.capitalize(toString().toLowerCase())), setterMethodType);
	}
}