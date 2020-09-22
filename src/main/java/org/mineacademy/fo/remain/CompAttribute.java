package org.mineacademy.fo.remain;

import java.lang.reflect.Method;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper for {@link Attribute}
 * <p>
 * See https://minecraft.gamepedia.com/Attribute for more information
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
	 * <p>
	 * For default values see https://minecraft.gamepedia.com/Attribute
	 */
	GENERIC_MOVEMENT_SPEED("generic.movementSpeed", "MOVEMENT_SPEED"),

	/**
	 * Flying speed of an Entity.
	 */
	GENERIC_FLYING_SPEED("generic.flyingSpeed"),

	/**
	 * Attack damage of an Entity.
	 * <p>
	 * This attribute is not found on passive mobs and golems.
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
	private CompAttribute(final String name, final String genericFieldName) {
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
	public final Double get(final LivingEntity entity) {
		try {
			final AttributeInstance instance = entity.getAttribute(Attribute.valueOf(toString()));

			return instance != null ? instance.getBaseValue() : null;

		} catch (IllegalArgumentException | NoSuchMethodError | NoClassDefFoundError ex) {
			try {
				return hasLegacy() ? getLegacy(entity) : null;

			} catch (final NullPointerException exx) {
				return null;

			} catch (final Throwable t) {
				if (MinecraftVersion.equals(V.v1_8))
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
	public final void set(final LivingEntity entity, final double value) {
		Valid.checkNotNull(entity, "Entity cannot be null");
		Valid.checkNotNull(entity, "Attribute cannot be null");

		try {
			final AttributeInstance instance = entity.getAttribute(Attribute.valueOf(toString()));

			instance.setBaseValue(value);
		} catch (NullPointerException | NoSuchMethodError | NoClassDefFoundError ex) {
			try {
				if (hasLegacy())
					setLegacy(entity, value);

			} catch (final Throwable t) {
				if (MinecraftVersion.equals(V.v1_8))
					t.printStackTrace();

				if (t instanceof NullPointerException)
					throw new FoException("Attribute " + this + " cannot be set for " + entity);
			}
		}
	}

	// MC 1.8.9
	private double getLegacy(final Entity entity) {
		return (double) ReflectionUtil.invoke("getValue", getLegacyAttributeInstance(entity));
	}

	// MC 1.8.9
	private void setLegacy(final Entity entity, final double value) {
		final Object instance = getLegacyAttributeInstance(entity);

		ReflectionUtil.invoke(ReflectionUtil.getMethod(instance.getClass(), "setValue", double.class), instance, value);
	}

	// MC 1.8.9
	private Object getLegacyAttributeInstance(final Entity entity) {
		final Object nmsEntity = ReflectionUtil.invoke("getHandle", entity);

		final Class<?> genericAttribute = ReflectionUtil.getNMSClass("GenericAttributes");
		Object iAttribute;

		try {
			iAttribute = ReflectionUtil.getStaticFieldContent(genericAttribute, this.genericFieldName);
		} catch (final Throwable t) {
			iAttribute = ReflectionUtil.getStaticFieldContent(genericAttribute, this.minecraftName);
		}

		final Class<?> nmsLiving = ReflectionUtil.getNMSClass("EntityLiving");
		final Method method = ReflectionUtil.getMethod(nmsLiving, "getAttributeInstance", ReflectionUtil.getNMSClass("IAttribute"));

		final Object ret = ReflectionUtil.invoke(method, nmsEntity, iAttribute);

		return ret;
	}
}