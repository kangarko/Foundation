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
 * See https://minecraft.wiki/w/Attribute for more information
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
	 * For default values see https://minecraft.wiki/w/Attribute
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
	 * Attack knockback of an Entity.
	 */
	GENERIC_ATTACK_KNOCKBACK("generic.attack_knockback"),

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
	 * The fall damage multiplier of an Entity.
	 */
	GENERIC_FALL_DAMAGE_MULTIPLIER("generic.fall_damage_multiplier"),

	/**
	 * Luck bonus of an Entity.
	 */
	GENERIC_LUCK("generic.luck"),

	/**
	 * Maximum absorption of an Entity.
	 */
	GENERIC_MAX_ABSORPTION("generic.max_absorption"),

	/**
	 * The distance which an Entity can fall without damage.
	 */
	GENERIC_SAFE_FALL_DISTANCE("generic.safe_fall_distance"),

	/**
	 * The relative scale of an Entity.
	 */
	GENERIC_SCALE("generic.scale"),

	/**
	 * The height which an Entity can walk over.
	 */
	GENERIC_STEP_HEIGHT("generic.step_height"),

	/**
	 * The gravity applied to an Entity.
	 */
	GENERIC_GRAVITY("generic.gravity"),

	/**
	 * Strength with which a horse will jump.
	 *
	 * @deprecated on modern MC versions, this is {@link #GENERIC_JUMP_STRENGTH}
	 */
	HORSE_JUMP_STRENGTH("horse.jumpStrength"),

	/**
	 * Strength with which an Entity will jump.
	 */
	GENERIC_JUMP_STRENGTH("generic.jump_strength"),

	/**
	 * How long an entity remains burning after ingition.
	 */
	GENERIC_BURNING_TIME("generic.burning_time"),

	/**
	 * Resistance to knockback from explosions.
	 */
	GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE("generic.explosion_knockback_resistance"),

	/**
	 * Movement speed through difficult terrain.
	 */
	GENERIC_MOVEMENT_EFFICIENCY("generic.movement_efficiency"),

	/**
	 * Oxygen use underwater.
	 */
	GENERIC_OXYGEN_BONUS("generic.oxygen_bonus"),

	/**
	 * Movement speed through water.
	 */
	GENERIC_WATER_MOVEMENT_EFFICIENCY("generic.water_movement_efficiency"),

	/**
	 * Range at which mobs will be tempted by items.
	 */
	GENERIC_TEMPT_RANGE("tempt_range"),

	/**
	 * The block reach distance of a Player.
	 */
	PLAYER_BLOCK_INTERACTION_RANGE("player.block_interaction_range"),

	/**
	 * The entity reach distance of a Player.
	 */
	PLAYER_ENTITY_INTERACTION_RANGE("player.entity_interaction_range"),

	/**
	 * Block break speed of a Player.
	 */
	PLAYER_BLOCK_BREAK_SPEED("player.block_break_speed"),

	/**
	 * Mining speed for correct tools.
	 */
	PLAYER_MINING_EFFICIENCY("player.mining_efficiency"),

	/**
	 * Sneaking speed.
	 */
	PLAYER_SNEAKING_SPEED("player.sneaking_speed"),

	/**
	 * Underwater mining speed.
	 */
	PLAYER_SUBMERGED_MINING_SPEED("player.submerged_mining_speed"),

	/**
	 * Sweeping damage.
	 */
	PLAYER_SWEEPING_DAMAGE_RATIO("player.sweeping_damage_ratio"),

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
	 * @param genericFieldName see {@link #genericFieldName}
	 */
	CompAttribute(final String name, final String genericFieldName) {
		this.minecraftName = name;
		this.genericFieldName = genericFieldName;
	}

	/**
	 * Get if this attribute existed in MC 1.8.9
	 *
	 * @return true if this attribute existed in MC 1.8.9
	 */
	public final boolean hasLegacy() {
		return this.genericFieldName != null;
	}

	/**
	 * Finds the attribute of an entity
	 *
	 * @param entity
	 * @return the attribute, or null if not supported by the server
	 */
	public final Double get(final LivingEntity entity) {
		try {
			final AttributeInstance instance = entity.getAttribute(Attribute.valueOf(this.toString()));

			return instance != null ? instance.getValue() : null;

		} catch (IllegalArgumentException | NoSuchMethodError | NoClassDefFoundError ex) {
			try {
				return this.hasLegacy() ? this.getLegacy(entity) : null;

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
			final AttributeInstance instance = entity.getAttribute(Attribute.valueOf(this.toString()));

			instance.setBaseValue(value);

		} catch (NullPointerException | NoSuchMethodError | NoClassDefFoundError ex) {

			if (this == GENERIC_MAX_HEALTH)
				entity.setMaxHealth(value);

			else
				try {
					if (this.hasLegacy())
						this.setLegacy(entity, value);

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
		return (double) ReflectionUtil.invoke("getValue", this.getLegacyAttributeInstance(entity));
	}

	// MC 1.8.9
	private void setLegacy(final Entity entity, final double value) {
		final Object instance = this.getLegacyAttributeInstance(entity);

		ReflectionUtil.invoke(ReflectionUtil.getMethod(instance.getClass(), "setValue", double.class), instance, value);
	}

	// MC 1.8.9
	private Object getLegacyAttributeInstance(final Entity entity) {
		final Object nmsEntity = ReflectionUtil.invoke("getHandle", entity);

		final Class<?> genericAttribute = Remain.getNMSClass("GenericAttributes", "net.minecraft.world.entity.ai.attributes.GenericAttributes");
		Object iAttribute;

		try {
			iAttribute = ReflectionUtil.getStaticFieldContent(genericAttribute, this.genericFieldName);
		} catch (final Throwable t) {
			iAttribute = ReflectionUtil.getStaticFieldContent(genericAttribute, this.minecraftName);
		}

		final Class<?> nmsLiving = Remain.getNMSClass("EntityLiving", "N/A");
		final Method method = ReflectionUtil.getMethod(nmsLiving, "getAttributeInstance", Remain.getNMSClass("IAttribute", "N/A"));

		final Object ret = ReflectionUtil.invoke(method, nmsEntity, iAttribute);

		return ret;
	}
}