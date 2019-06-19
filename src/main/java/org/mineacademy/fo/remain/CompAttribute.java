package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.util.Objects;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.MinecraftVersion.V;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper for {@link Attribute}
 */
@RequiredArgsConstructor
public enum CompAttribute {

	/**
	 * Maximum health of an Entity.
	 */
	GENERIC_MAX_HEALTH("generic.maxHealth", "maxHealth"),

	/**
	 * Range at which an Entity will follow others.
	 */
	GENERIC_FOLLOW_RANGE("generic.followRange", "FOLLOW_RANGE"),

	/**
	 * Resistance of an Entity to knockback.
	 */
	GENERIC_KNOCKBACK_RESISTANCE("generic.knockbackResistance", "c"),

	/**
	 * Movement speed of an Entity.
	 */
	GENERIC_MOVEMENT_SPEED("generic.movementSpeed", "MOVEMENT_SPEED"),

	/**
	 * Flying speed of an Entity.
	 */
	GENERIC_FLYING_SPEED("generic.flyingSpeed"),

	/**
	 * Attack damage of an Entity.
	 */
	GENERIC_ATTACK_DAMAGE("generic.attackDamage", "ATTACK_DAMAGE"),

	/**
	 * Attack speed of an Entity.
	 */
	GENERIC_ATTACK_SPEED("generic.attackSpeed"),

	/**
	 * Armor bonus of an Entity.
	 */
	GENERIC_ARMOR("generic.armor"),

	/**
	 * Armor durability bonus of an Entity.
	 */
	GENERIC_ARMOR_TOUGHNESS("generic.armorToughness"),

	/**
	 * Luck bonus of an Entity.
	 */
	GENERIC_LUCK("generic.luck"),

	/**
	 * Strength with which a horse will jump.
	 */
	HORSE_JUMP_STRENGTH("horse.jumpStrength"),

	/**
	 * Chance of a Zombie to spawn reinforcements.
	 */
	ZOMBIE_SPAWN_REINFORCEMENTS("zombie.spawnReinforcements");

	/**
	 * The internal name
	 */
	@Getter
	private final String minecraftName;

	/**
	 * Used for MC 1.8.9 compatibility. Returns the field name in GenericAttributes
	 * class for that MC version, or null if not existing.
	 */
	private String genericFieldName;

	/**
	 * Construct a new Attribute.
	 *
	 * @param name              the generic name
	 * @param genericFieldName, see {@link #genericFieldName}
	 */
	private CompAttribute(String name, String genericFieldName) {
		this.minecraftName = name;
		this.genericFieldName = genericFieldName;
	}

	/**
	 * Get if this attribute existed in MC 1.8.9
	 *
	 * @return true if this attribute existed in MC 1.8.9
	 */
	public final boolean hasLegacy() {
		return genericFieldName != null;
	}

	/**
	 * Finds the attribute of an entity
	 *
	 * @param entity
	 * @return the attribute, or null if not supported by the server
	 */
	public final Double get(LivingEntity entity) {
		try {
			final AttributeInstance instance = entity.getAttribute(Attribute.valueOf(toString()));

			return instance != null ? instance.getBaseValue() : null;

		} catch (IllegalArgumentException | NoSuchMethodError | NoClassDefFoundError ex) {
			try {
				return hasLegacy() ? getLegacy(entity) : null;

			} catch (final Throwable t) {
				if (MinecraftVersion.olderThan(V.v1_9))
					t.printStackTrace();

				return null;
			}
		}
	}

	/**
	 * If supported by the server, sets a new attribute to the entity
	 *
	 * @param entity
	 * @param value
	 */
	public final void set(LivingEntity entity, double value) {
		try {
			Objects.requireNonNull(entity, "Entity cannot be null");
			Objects.requireNonNull(entity, "Attribute cannot be null");

			final AttributeInstance instance = entity.getAttribute(Attribute.valueOf(toString()));

			instance.setBaseValue(value);
		} catch (NoSuchMethodError | NoClassDefFoundError ex) {
			try {
				if (hasLegacy())
					setLegacy(entity, value);
			} catch (final Throwable t) {
				if (MinecraftVersion.olderThan(V.v1_9))
					t.printStackTrace();
			}
		}
	}

	// MC 1.8.9
	private double getLegacy(Entity entity) {
		return (double) ReflectionUtil.invoke("getValue", getLegacyAttributeInstance(entity));
	}

	// MC 1.8.9
	private void setLegacy(Entity entity, double value) {
		final Object instance = getLegacyAttributeInstance(entity);

		ReflectionUtil.invoke(ReflectionUtil.getMethod(instance.getClass(), "setValue", double.class), instance, value);
	}

	// MC 1.8.9
	private Object getLegacyAttributeInstance(Entity entity) {
		final Object nmsEntity = ReflectionUtil.invoke("getHandle", entity);

		final Class<?> genericAttribute = ReflectionUtil.getNMSClass("GenericAttributes");
		final Object iAttribute = ReflectionUtil.getStaticFieldContent(genericAttribute, this.genericFieldName);

		final Class<?> nmsLiving = ReflectionUtil.getNMSClass("EntityLiving");
		final Method method = ReflectionUtil.getMethod(nmsLiving, "getAttributeInstance", ReflectionUtil.getNMSClass("IAttribute"));

		final Object ret = ReflectionUtil.invoke(method, nmsEntity, iAttribute);

		return ret;
	}
}