package org.mineacademy.fo;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wolf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for managing entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityUtil {

	/**
	 * Returns the entity target only if it is a player, or null
	 *
	 * @param entity
	 * @return
	 */
	public static Player getTargetPlayer(Entity entity) {
		final LivingEntity target = getTarget(entity);

		return target instanceof Player ? (Player) target : null;
	}

	/**
	 * Return the target for the given entity
	 *
	 * @param entity
	 * @return the target, or null if does not have / unsupported
	 */
	public static LivingEntity getTarget(Entity entity) {
		return entity instanceof Creature ? ((Creature) entity).getTarget() : null;
	}

	/**
	 * Return if this entity is creature and aggressive (not an animal)
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isAggressive(Entity entity) {
		return entity instanceof Ghast ||
				entity instanceof Slime ||
				entity instanceof Wolf && ((Wolf) entity).isAngry() ||
				entity instanceof Creature && !(entity instanceof Animals);
	}

	/**
	 * Return if this entity is a {@link Creature}, {@link Slime} or {@link Wolf}
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isCreature(Entity entity) {
		return entity instanceof Slime ||
				entity instanceof Wolf ||
				entity instanceof Creature;
	}

	/**
	 * Return if this entity is suitable for removing (e.g. dropped items,
	 * falling blocks, arrows, projectiles)
	 *
	 * @param entity
	 * @return
	 */
	public static boolean canBeCleaned(Entity entity) {
		return entity instanceof FallingBlock ||
				entity instanceof Item ||
				entity instanceof Projectile ||
				entity instanceof ExperienceOrb;
	}
}
