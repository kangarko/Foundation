package org.mineacademy.fo.remain;

import java.lang.reflect.Method;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.MissingEnumException;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

import lombok.NonNull;

/**
 * Wrapper for {@link Attribute}
 * <p>
 * See https://minecraft.wiki/w/Attribute for more information
 */
public enum CompAttribute {

	/**
	 * Armor bonus of an Entity.
	 */
	ARMOR("ARMOR", "GENERIC_ARMOR"),

	/**
	 * Armor durability bonus of an Entity.
	 */
	ARMOR_TOUGHNESS("ARMOR_TOUGHNESS", "GENERIC_ARMOR_TOUGHNESS"),

	/**
	 * Attack damage of an Entity.
	 * <p>
	 * This attribute is not found on passive mobs and golems.
	 */
	ATTACK_DAMAGE("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE") {
		@Override
		public String getNmsName() {
			return "ATTACK_DAMAGE";
		}
	},

	/**
	 * Attack knockback of an Entity.
	 */
	ATTACK_KNOCKBACK("ATTACK_KNOCKBACK", "GENERIC_ATTACK_KNOCKBACK"),

	/**
	 * Attack speed of an Entity.
	 */
	ATTACK_SPEED("ATTACK_SPEED", "GENERIC_ATTACK_SPEED"),

	/**
	 * Block break speed of a Player.
	 */
	BLOCK_BREAK_SPEED("BLOCK_BREAK_SPEED", "PLAYER_BLOCK_BREAK_SPEED"),

	/**
	 * The block reach distance of a Player.
	 */
	BLOCK_INTERACTION_RANGE("BLOCK_INTERACTION_RANGE", "PLAYER_BLOCK_INTERACTION_RANGE"),

	/**
	 * How long an entity remains burning after ingition.
	 */
	BURNING_TIME("BURNING_TIME", "GENERIC_BURNING_TIME"),

	/**
	 * The entity reach distance of a Player.
	 */
	ENTITY_INTERACTION_RANGE("ENTITY_INTERACTION_RANGE", "PLAYER_ENTITY_INTERACTION_RANGE"),

	/**
	 * Resistance to knockback from explosions.
	 */
	EXPLOSION_KNOCKBACK_RESISTANCE("EXPLOSION_KNOCKBACK_RESISTANCE", "GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE"),

	/**
	 * The fall damage multiplier of an Entity.
	 */
	FALL_DAMAGE_MULTIPLIER("FALL_DAMAGE_MULTIPLIER", "GENERIC_FALL_DAMAGE_MULTIPLIER"),

	/**
	 * Flying speed of an Entity.
	 */
	FLYING_SPEED("FLYING_SPEED", "GENERIC_FLYING_SPEED"),

	/**
	 * Range at which an Entity will follow others.
	 */
	FOLLOW_RANGE("FOLLOW_RANGE", "GENERIC_FOLLOW_RANGE") {
		@Override
		public String getNmsName() {
			return "FOLLOW_RANGE";
		}
	},

	/**
	 * The gravity applied to an Entity.
	 */
	GRAVITY("GRAVITY", "GENERIC_GRAVITY"),

	/**
	 * Strength with which an Entity will jump.
	 */
	JUMP_STRENGTH("JUMP_STRENGTH", "GENERIC_JUMP_STRENGTH", "HORSE_JUMP_STRENGTH"),

	/**
	 * Resistance of an Entity to knockback.
	 */
	KNOCKBACK_RESISTANCE("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE") {
		@Override
		public String getNmsName() {
			return "c";
		}
	},

	/**
	 * Luck bonus of an Entity.
	 */
	LUCK("LUCK", "GENERIC_LUCK"),

	/**
	 * Maximum absorption of an Entity.
	 */
	MAX_ABSORPTION("MAX_ABSORPTION", "GENERIC_MAX_ABSORPTION"),

	/**
	 * Maximum health of an Entity.
	 */
	MAX_HEALTH("MAX_HEALTH", "GENERIC_MAX_HEALTH") {
		@Override
		public String getNmsName() {
			return "maxHealth";
		}
	},

	/**
	 * Mining speed for correct tools.
	 */
	MINING_EFFICIENCY("MINING_EFFICIENCY", "PLAYER_MINING_EFFICIENCY"),

	/**
	 * Movement speed through difficult terrain.
	 */
	MOVEMENT_EFFICIENCY("MOVEMENT_EFFICIENCY", "GENERIC_MOVEMENT_EFFICIENCY"),

	/**
	 * Movement speed of an Entity.
	 * <p>
	 * For default values see https://minecraft.wiki/w/Attribute
	 */
	MOVEMENT_SPEED("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED") {
		@Override
		public String getNmsName() {
			return "MOVEMENT_SPEED";
		}
	},

	/**
	 * Oxygen use underwater.
	 */
	OXYGEN_BONUS("OXYGEN_BONUS", "GENERIC_OXYGEN_BONUS"),

	/**
	 * The distance which an Entity can fall without damage.
	 */
	SAFE_FALL_DISTANCE("SAFE_FALL_DISTANCE", "GENERIC_SAFE_FALL_DISTANCE"),

	/**
	 * The relative scale of an Entity.
	 */
	SCALE("SCALE", "GENERIC_SCALE"),

	/**
	 * Sneaking speed.
	 */
	SNEAKING_SPEED("SNEAKING_SPEED", "PLAYER_SNEAKING_SPEED"),

	/**
	 * Chance of a Zombie to spawn reinforcements.
	 */
	SPAWN_REINFORCEMENTS("SPAWN_REINFORCEMENTS", "ZOMBIE_SPAWN_REINFORCEMENTS"),

	/**
	 * The height which an Entity can walk over.
	 */
	STEP_HEIGHT("STEP_HEIGHT", "GENERIC_STEP_HEIGHT"),

	/**
	 * Underwater mining speed.
	 */
	SUBMERGED_MINING_SPEED("SUBMERGED_MINING_SPEED", "PLAYER_SUBMERGED_MINING_SPEED"),

	/**
	 * Sweeping damage.
	 */
	SWEEPING_DAMAGE_RATIO("SWEEPING_DAMAGE_RATIO", "PLAYER_SWEEPING_DAMAGE_RATIO"),

	/**
	 * Range at which mobs will be tempted by items.
	 */
	TEMPT_RANGE("TEMPT_RANGE", "GENERIC_TEMPT_RANGE"),

	/**
	 * Movement speed through water.
	 */
	WATER_MOVEMENT_EFFICIENCY("WATER_MOVEMENT_EFFICIENCY", "GENERIC_WATER_MOVEMENT_EFFICIENCY");

	/**
	 * Returns true if the attribute is supported by the server.
	 */
	private static final boolean hasAttributeClass = MinecraftVersion.atLeast(V.v1_9);

	/**
	 * The cached Bukkit attribute, if any
	 */
	private Object bukkitAttribute;

	/**
	 * Create a new attribute
	 *
	 * @param names
	 */
	CompAttribute(String... names) {
		if (MinecraftVersion.atLeast(V.v1_9))
			for (final String name : names)
				try {
					this.bukkitAttribute = ReflectionUtil.lookupEnum(Attribute.class, name);

					break;

				} catch (final MissingEnumException | IllegalArgumentException ex) {
					// Ignore
				}
	}

	/**
	 * Get the 1.8.9 NMS name or null if not existing on that version
	 *
	 * @return
	 */
	public String getNmsName() {
		return null;
	}

	/**
	 * Finds the attribute of an entity
	 *
	 * @param entity
	 * @return the attribute, or null if not supported by the server or not applicable for the entity
	 */
	public final Double get(@NonNull final LivingEntity entity) {

		// Minecraft 1.8.8+
		if (hasAttributeClass) {

			// Too modern attribute
			if (this.bukkitAttribute != null) {
				final AttributeInstance instance = entity.getAttribute((Attribute) this.bukkitAttribute);

				return instance != null ? instance.getValue() : null;
			}

		} else if (this.getNmsName() != null)
			try {
				return (double) ReflectionUtil.invoke("getValue", this.getLegacyAttributeInstance(entity));

			} catch (final NullPointerException exx) {
				return null;

			} catch (final Throwable t) {
				throw new FoException("Error retrieving attribute " + this + " for " + entity);
			}

		return null;

	}

	/**
	 * If supported by the server, sets a new attribute to the entity
	 *
	 * @param entity
	 * @param value
	 */
	public final void set(@NonNull final LivingEntity entity, final double value) {

		// Minecraft 1.8.8+
		if (hasAttributeClass) {
			if (this.bukkitAttribute != null) {
				final AttributeInstance instance = entity.getAttribute((Attribute) this.bukkitAttribute);
				Valid.checkNotNull(instance, "Attribute " + this + " cannot be set for " + entity);

				instance.setBaseValue(value);
			}

		} else {
			if (this == MAX_HEALTH)
				entity.setMaxHealth(value);

			else if (this.getNmsName() != null) {
				final Object instance = this.getLegacyAttributeInstance(entity);
				Valid.checkNotNull(instance, "Attribute " + this + " cannot be set for " + entity);

				ReflectionUtil.invoke(ReflectionUtil.getMethod(instance.getClass(), "setValue", double.class), instance, value);
			}
		}
	}

	/**
	 * Return true if this attribute can be applied to the given entity
	 *
	 * @param entity
	 * @return
	 */
	public final boolean canApply(@NonNull final LivingEntity entity) {
		if (hasAttributeClass) {
			if (this.bukkitAttribute != null) {
				final AttributeInstance instance = entity.getAttribute((Attribute) this.bukkitAttribute);

				return instance != null;
			}

		} else {
			if (this == MAX_HEALTH)
				return true;

			else if (this.getNmsName() != null) {
				final Object instance = this.getLegacyAttributeInstance(entity);

				return instance != null;
			}
		}

		return false;
	}

	private Object getLegacyAttributeInstance(final Entity entity) {
		final Object nmsEntity = ReflectionUtil.invoke("getHandle", entity);
		final Class<?> genericAttribute = ReflectionUtil.getNMSClass("GenericAttributes", "net.minecraft.world.entity.ai.attributes.GenericAttributes");

		final Object iAttribute = ReflectionUtil.getStaticFieldContent(genericAttribute, this.getNmsName());

		final Class<?> nmsLiving = ReflectionUtil.getNMSClass("EntityLiving", "N/A");
		final Method method = ReflectionUtil.getMethod(nmsLiving, "getAttributeInstance", ReflectionUtil.getNMSClass("IAttribute", "N/A"));

		return ReflectionUtil.invoke(method, nmsEntity, iAttribute);
	}
}