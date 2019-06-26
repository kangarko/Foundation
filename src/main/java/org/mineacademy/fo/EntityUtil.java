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
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.exception.FoException;

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

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your hit listener
	 * when the given entity isOnGround. If the entity gets removed before it hits
	 * the ground, nothing is called
	 *
	 * If the entity still flies after 20 seconds, nothing is called
	 *
	 * @param entity
	 * @param hitGroundListener
	 */
	public static void trackFalling(Entity entity, Runnable hitGroundListener) {
		track(entity, 20 * 20, null, hitGroundListener);
	}

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your fly listener
	 * each tick until entity is either removed or isOnGround
	 *
	 * If the entity still flies after 20 seconds, nothing is called
	 *
	 * @param entity
	 * @param flyListener
	 */
	public static void trackFlying(Entity entity, Runnable flyListener) {
		track(entity, 20 * 20, flyListener, null);
	}

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your hit listener
	 * when the given entity isOnGround. If the entity gets removed before it hits
	 * the ground, nothing is called
	 *
	 * The fly listener is called every tick
	 *
	 * @param entity
	 * @param timeoutTicks
	 * @param flyListener
	 * @param hitGroundListener
	 */
	public static void track(Entity entity, int timeoutTicks, Runnable flyListener, Runnable hitGroundListener) {
		if (flyListener == null && hitGroundListener == null)
			throw new FoException("Cannot track entity with fly and hit listeners on null!");

		Common.runTimer(1, new BukkitRunnable() {

			private int elapsedTicks = 0;

			@Override
			public void run() {

				// Cancel after the given timeout to save performance
				if (elapsedTicks++ > timeoutTicks) {
					cancel();

					return;
				}

				// Cancel when invalid
				if (entity == null || entity.isDead() || !entity.isValid()) {
					cancel();

					return;
				}

				// Run the hit listener
				if (entity.isOnGround()) {
					if (hitGroundListener != null)
						hitGroundListener.run();

					cancel();

				} else {
					if (flyListener != null)
						flyListener.run();
				}
			}
		});
	}
}
