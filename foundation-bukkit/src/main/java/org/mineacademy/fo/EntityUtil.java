package org.mineacademy.fo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;
import org.mineacademy.fo.collection.ExpiringMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompAttribute;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for managing entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityUtil {

	/**
	 * Used to prevent duplicate registering of {@link HitTracking} listener but only
	 * register events when needed.
	 */
	private static boolean registeredHitListener = false;

	/**
	 * Returns the closest entity to the center location within the given 3-dimensional range
	 * that matches the given entity class, or null if not found.
	 *
	 * @param <T>
	 * @param center
	 * @param range3D
	 * @param entityClass
	 * @return
	 */
	public static <T extends LivingEntity> T findNearestEntity(final Location center, final double range3D, final Class<T> entityClass) {
		final List<T> found = new ArrayList<>();

		for (final Entity nearby : Remain.getNearbyEntities(center, range3D))
			if (nearby instanceof LivingEntity && entityClass.isAssignableFrom(nearby.getClass()))
				found.add((T) nearby);

		Collections.sort(found, (first, second) -> Double.compare(first.getLocation().distance(center), second.getLocation().distance(center)));

		return found.isEmpty() ? null : found.get(0);
	}

	/**
	 * Check if a ray from origin along direction intersects an axis-aligned bounding box
	 * centered at targetFeet with the given half-width and height. Uses the slab method.
	 *
	 * @param origin the ray origin (e.g. eye location)
	 * @param direction the normalized ray direction
	 * @param targetFeet the base (feet) position of the AABB
	 * @param halfWidth half the width of the AABB (e.g. 0.3 for a player)
	 * @param height the height of the AABB (e.g. 1.8 for a player)
	 * @return the distance along the ray to the hit point, or -1 if no intersection
	 */
	public static double rayIntersectsAABB(final Vector origin, final Vector direction, final Vector targetFeet, final double halfWidth, final double height) {
		final double minX = targetFeet.getX() - halfWidth;
		final double minY = targetFeet.getY();
		final double minZ = targetFeet.getZ() - halfWidth;
		final double maxX = targetFeet.getX() + halfWidth;
		final double maxY = targetFeet.getY() + height;
		final double maxZ = targetFeet.getZ() + halfWidth;

		double tMin = Double.NEGATIVE_INFINITY;
		double tMax = Double.POSITIVE_INFINITY;

		final double dx = direction.getX();
		final double dy = direction.getY();
		final double dz = direction.getZ();
		final double ox = origin.getX();
		final double oy = origin.getY();
		final double oz = origin.getZ();

		if (dx != 0) {
			final double t1 = (minX - ox) / dx;
			final double t2 = (maxX - ox) / dx;
			tMin = Math.max(tMin, Math.min(t1, t2));
			tMax = Math.min(tMax, Math.max(t1, t2));
		} else if (ox < minX || ox > maxX)
			return -1;

		if (dy != 0) {
			final double t1 = (minY - oy) / dy;
			final double t2 = (maxY - oy) / dy;
			tMin = Math.max(tMin, Math.min(t1, t2));
			tMax = Math.min(tMax, Math.max(t1, t2));
		} else if (oy < minY || oy > maxY)
			return -1;

		if (dz != 0) {
			final double t1 = (minZ - oz) / dz;
			final double t2 = (maxZ - oz) / dz;
			tMin = Math.max(tMin, Math.min(t1, t2));
			tMax = Math.min(tMax, Math.max(t1, t2));
		} else if (oz < minZ || oz > maxZ)
			return -1;

		if (tMax < 0 || tMin > tMax)
			return -1;

		return tMin >= 0 ? tMin : tMax;
	}

	/**
	 * Return the target for the given entity, supporting getting targets if entity is an NPC, use
	 * {@link HookManager#isNPC(Entity)} to check if the target is an NPC
	 *
	 * @param entity
	 * @return the target, or null if does not have / unsupported
	 */
	public static Entity getTarget(final Entity entity) {
		Entity target = null;

		try {
			if (entity instanceof Mob)
				target = ((Mob) entity).getTarget();
		} catch (final Throwable t) {
			// Old MC
		}

		if (target == null && entity instanceof Creature)
			target = ((Creature) entity).getTarget();

		if (target == null)
			target = HookManager.getNPCTarget(entity);

		return target;
	}

	/**
	 * Cached default health per entity type so we only resolve once per server lifetime.
	 * Thread-safe so lookups can happen from any scheduler/region on Folia.
	 */
	private static final Map<EntityType, Double> defaultHealthCache = new ConcurrentHashMap<>();

	/**
	 * Returns the vanilla default max-health for the given entity type.
	 *
	 * Reads from the NMS default-attribute registry ({@code DefaultAttributes}) when available
	 * so the call is thread-safe on Folia (no region context needed) and does not fire spawn events.
	 * Falls back to a one-shot entity spawn probe on legacy / non-Paper servers where the registry
	 * class is absent. The result is cached per entity type.
	 *
	 * @param type
	 * @return
	 */
	public static double getDefaultHealth(final EntityType type) {
		if (type == CompEntityType.PLAYER)
			return 20;

		final Double cached = defaultHealthCache.get(type);

		if (cached != null)
			return cached;

		final Double registryValue = readDefaultHealthFromRegistry(type);

		if (registryValue != null) {
			defaultHealthCache.put(type, registryValue);

			return registryValue;
		}

		final double probed = probeDefaultHealthBySpawn(type);
		defaultHealthCache.put(type, probed);

		return probed;
	}

	/**
	 * Resolve the default max-health from the NMS attribute registry via reflection,
	 * or null if the server is too old (pre-1.17 NMS package layout) or the type has
	 * no registered attribute supplier. Handles both pre-1.20.5 (Attributes.MAX_HEALTH
	 * is raw Attribute) and post-1.20.5 (wrapped in Holder) signatures.
	 */
	private static Double readDefaultHealthFromRegistry(final EntityType type) {
		try {
			final Object supplier = resolveDefaultAttributeSupplier(type);

			if (supplier == null)
				return null;

			final Class<?> attributesClass = Class.forName("net.minecraft.world.entity.ai.attributes.Attributes");
			final Object maxHealthAttribute = attributesClass.getField("MAX_HEALTH").get(null);

			// AttributeSupplier has exactly one getBaseValue(Object) method; its parameter
			// type is Holder on 1.20.5+ and Attribute on earlier versions. The MAX_HEALTH
			// field's runtime type matches the signature, so we find the method by name
			// rather than hard-coding either parameter class.
			for (final Method method : supplier.getClass().getMethods())
				if ("getBaseValue".equals(method.getName()) && method.getParameterCount() == 1)
					return (double) method.invoke(supplier, maxHealthAttribute);

			return null;

		} catch (final ClassNotFoundException | NoSuchMethodException | NoSuchFieldException ignored) {
			// Pre-1.17 server or mapping mismatch: caller falls back to spawn probe.
			return null;

		} catch (final Throwable t) {
			CommonCore.log("Foundation.getDefaultHealth: NMS registry path failed for " + type + ", falling back to spawn probe. Raw error: " + t);

			return null;
		}
	}

	/**
	 * Returns the vanilla default base values for every attribute the given entity type defines,
	 * read from the NMS default-attribute registry ({@code DefaultAttributes}).
	 *
	 * Unlike spawning a probe entity, this is thread-safe with no region context (Folia-safe) and
	 * fires no events. Returns an empty map on servers without the registry (legacy / pre-1.20.5
	 * mappings), where the caller should fall back to reading the values off a spawned entity.
	 *
	 * @param type
	 * @return
	 */
	public static Map<CompAttribute, Double> getDefaultAttributeBaseValues(final EntityType type) {
		final Map<CompAttribute, Double> defaults = new LinkedHashMap<>();

		if (type == CompEntityType.PLAYER)
			return defaults;

		try {
			final Object supplier = resolveDefaultAttributeSupplier(type);

			if (supplier == null)
				return defaults;

			final Class<?> bukkitAttributeClass = Class.forName("org.bukkit.attribute.Attribute");
			final Class<?> craftAttributeClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".attribute.CraftAttribute");
			final Method bukkitToHolder = craftAttributeClass.getMethod("bukkitToMinecraftHolder", bukkitAttributeClass);

			// getBaseValue / hasAttribute each take a single argument whose type is Holder on
			// 1.20.5+ and Attribute earlier, so we resolve them by name to stay version-safe.
			Method getBaseValue = null;
			Method hasAttribute = null;

			for (final Method method : supplier.getClass().getMethods()) {
				if (method.getParameterCount() != 1)
					continue;

				if ("getBaseValue".equals(method.getName()))
					getBaseValue = method;

				else if ("hasAttribute".equals(method.getName()))
					hasAttribute = method;
			}

			if (getBaseValue == null || hasAttribute == null)
				return defaults;

			for (final CompAttribute attribute : CompAttribute.values()) {
				final Object bukkitAttribute = attribute.getBukkitAttribute();

				if (bukkitAttribute == null)
					continue;

				try {
					final Object holder = bukkitToHolder.invoke(null, bukkitAttribute);

					if ((boolean) hasAttribute.invoke(supplier, holder))
						defaults.put(attribute, (double) getBaseValue.invoke(supplier, holder));

				} catch (final Throwable ignored) {
					// Attribute not registered for this type or not mappable: skip it.
				}
			}

		} catch (final ClassNotFoundException | NoSuchMethodException ignored) {
			// Legacy server / pre-1.20.5 mappings: caller falls back to a spawn probe.
			return new LinkedHashMap<>();

		} catch (final Throwable t) {
			CommonCore.log("Foundation.getDefaultAttributeBaseValues: NMS registry path failed for " + type + ", falling back to spawn probe. Raw error: " + t);

			return new LinkedHashMap<>();
		}

		return defaults;
	}

	/**
	 * Resolves the NMS {@code AttributeSupplier} holding the vanilla default attributes for the
	 * given entity type, or null when the type has no registered supplier. Throws when the registry
	 * classes are absent (legacy servers), letting callers fall back to a spawn probe.
	 */
	private static Object resolveDefaultAttributeSupplier(final EntityType type) throws ReflectiveOperationException {
		final Class<?> defaultAttributesClass = Class.forName("net.minecraft.world.entity.ai.attributes.DefaultAttributes");
		final Class<?> craftEntityTypeClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntityType");
		final Class<?> nmsEntityTypeClass = Class.forName("net.minecraft.world.entity.EntityType");

		final Object nmsType = craftEntityTypeClass.getMethod("bukkitToMinecraft", EntityType.class).invoke(null, type);
		final boolean hasSupplier = (boolean) defaultAttributesClass.getMethod("hasSupplier", nmsEntityTypeClass).invoke(null, nmsType);

		if (!hasSupplier)
			return null;

		return defaultAttributesClass.getMethod("getSupplier", nmsEntityTypeClass).invoke(null, nmsType);
	}

	/**
	 * Legacy fallback: spawn the entity at y=0 in a supported world, read health, remove it.
	 * Requires the main / region tick thread and will NPE on Folia outside a region context.
	 */
	private static double probeDefaultHealthBySpawn(final EntityType type) {
		Valid.checkSync("Cannot use getDefaultHealth async!");

		World world = Bukkit.getWorlds().get(0);

		try {
			final Method isEnabled = Remain.getIsEnabledFeatureWorldMethod();
			boolean found = false;

			if (isEnabled == null)
				found = true;
			else
				for (final World other : Bukkit.getWorlds())
					if ((boolean) isEnabled.invoke(other, type) == true) {
						world = other;

						found = true;
						break;
					}

			if (!found)
				throw new FoException("Lacking experimental databack! Cannot find a world that supports entity type '" + type + "'. This is NOT A BUG IN OUR PLUGIN. You need to install the datapack for " + type.requiredFeatures() + " or delete the entity");

		} catch (final IllegalArgumentException | NoSuchMethodError | NoSuchElementException err) {
			// Ignore

		} catch (final ReflectiveOperationException ex) {
			Throwable cause = ex;

			while (cause.getCause() != null)
				cause = cause.getCause();

			if (cause instanceof IllegalArgumentException || cause instanceof NoSuchMethodError || cause instanceof NoSuchElementException) {
				// ignore
			} else
				Common.throwError(ex, "Failed to check if " + type + " has required data pack"); // print error in case of a future breakage we can spot
		}

		final Location location = world.getSpawnLocation();
		location.setY(0);

		final Entity entity = world.spawnEntity(location, type);
		ValidCore.checkBoolean(entity instanceof LivingEntity, "Cannot use getDefaultHealth for non-living entity: " + type);

		final double health = ((LivingEntity) entity).getHealth();

		entity.remove();
		return health;
	}

	/**
	 * Attempts to remove all vehicles and passengers stack from the given entity
	 *
	 * @param entity
	 */
	public static void removeVehiclesAndPassengers(final Entity entity) {
		Entity vehicle = entity.getVehicle();

		if (vehicle != null)
			entity.leaveVehicle();

		while (vehicle != null) {
			final Entity copyOf = vehicle;
			vehicle = vehicle.getVehicle();

			if (copyOf instanceof Player)
				copyOf.eject();
			else
				copyOf.remove();
		}

		try {
			for (final Entity passenger : entity.getPassengers()) {
				entity.removePassenger(passenger);

				if (!(passenger instanceof Player))
					passenger.remove();
			}

		} catch (final NoSuchMethodError err) {
			final Entity passenger = entity.getPassenger();

			if (passenger != null) {
				entity.eject();

				if (!(passenger instanceof Player))
					passenger.remove();
			}
		}
	}

	/**
	 * Return if this entity is creature and aggressive (not an animal)
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isAggressive(final Entity entity) {
		if (entity instanceof Ghast || entity instanceof Slime)
			return true;

		if (entity.getType().toString().equals("BEE"))
			return ((Bee) entity).getAnger() > 0;

		if (entity instanceof Wolf && ((Wolf) entity).isAngry())
			return true;

		if (entity instanceof Animals)
			return false;

		if (entity instanceof ArmorStand)
			return false;

		try {
			if (entity instanceof Tameable)
				return false;
		} catch (final NoClassDefFoundError err) {
			// Ignore
		}

		return entity instanceof Creature;
	}

	/**
	 * Return if this entity is a {@link Creature}, {@link Slime} or {@link Wolf}
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isCreature(final Entity entity) {
		return entity instanceof Slime || entity instanceof Wolf || entity instanceof Creature;
	}

	/**
	 * Return if this entity is suitable for removing (e.g. dropped items,
	 * falling blocks, arrows, projectiles)
	 *
	 * @param entity
	 * @return
	 */
	public static boolean canBeCleaned(final Entity entity) {
		return entity instanceof FallingBlock || entity instanceof Item || entity instanceof Projectile || entity instanceof ExperienceOrb;
	}

	// ----------------------------------------------------------------------------------------------------
	// Tracking
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your hit listener
	 * when the given entity isOnGround. If the entity gets removed before it hits
	 * the ground, nothing is called
	 * <p>
	 * If the entity still flies after 30 seconds, nothing is called
	 *
	 * @param entity
	 * @param hitGroundListener
	 */
	public static void trackFalling(final Entity entity, final Runnable hitGroundListener) {
		track(entity, 30 * 20, null, hitGroundListener);
	}

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your fly listener
	 * each tick until entity is either removed or isOnGround
	 * <p>
	 * If the entity still flies after 30 seconds, nothing is called
	 *
	 * @param entity
	 * @param flyListener
	 */
	public static void trackFlying(final Entity entity, final Runnable flyListener) {
		track(entity, 30 * 20, flyListener, null);
	}

	/**
	 * Runs a timer task with the tick period of 1 and will trigger your hit listener
	 * when the given entity isOnGround. If the entity gets removed before it hits
	 * the ground, nothing is called
	 * <p>
	 * The fly listener is called every tick
	 *
	 * @param entity
	 * @param timeoutTicks
	 * @param flyListener
	 * @param hitGroundListener
	 */
	public static void track(final Entity entity, final int timeoutTicks, final Runnable flyListener, final Runnable hitGroundListener) {
		if (flyListener == null && hitGroundListener == null)
			throw new FoException("Cannot track entity with fly and hit listeners on null!");

		Platform.runTaskTimer(1, new SimpleRunnable() {

			private int elapsedTicks = 0;

			@Override
			public void run() {

				// Cancel after the given timeout to save performance
				if (this.elapsedTicks++ > timeoutTicks) {
					this.cancel();

					return;
				}

				// Cancel when invalid
				if (entity == null || entity.isDead() || !entity.isValid()) {
					if (entity instanceof FallingBlock && hitGroundListener != null)
						hitGroundListener.run();

					this.cancel();
					return;
				}

				// Run the hit listener
				if (entity.isOnGround()) {
					if (hitGroundListener != null)
						hitGroundListener.run();

					this.cancel();

				} else if (flyListener != null)
					flyListener.run();
			}
		});
	}

	/**
	 * (No timer task) Starts tracking a projectile's impact and executes the hit
	 * task when it hits something. After 30 seconds of flight we stop tracking
	 * to save performance
	 *
	 * @param projectile
	 * @param hitTask
	 */
	public static void trackHit(final Projectile projectile, final Consumer<ProjectileHitEvent> hitTask) {
		HitTracking.addFlyingProjectile(projectile, hitTask);

		if (!registeredHitListener) {
			Platform.registerEvents(new HitTracking());

			registeredHitListener = true;
		}
	}
}

/**
 * Class responsible for tracking connection between projectile launch and projectile hit event
 */
class HitTracking implements Listener {

	/**
	 * List of flying projectiles with code to run on impact,
	 * stop tracking after 30 seconds to prevent overloading the map
	 */
	private static ExpiringMap<UUID, List<Consumer<ProjectileHitEvent>>> flyingProjectiles = ExpiringMap.builder().expiration(30, TimeUnit.SECONDS).build();

	/**
	 * Invoke the hit listener when the registered projectile hits something
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHit(final ProjectileHitEvent event) {
		final List<Consumer<ProjectileHitEvent>> hitListeners = flyingProjectiles.remove(event.getEntity().getUniqueId());

		if (hitListeners != null)
			for (final Consumer<ProjectileHitEvent> listener : hitListeners)
				listener.accept(event);
	}

	/**
	 * Add a new flying projectile that will be pending and execute code when collide
	 *
	 * @param projectile
	 * @param hitTask
	 */
	static void addFlyingProjectile(final Projectile projectile, final Consumer<ProjectileHitEvent> hitTask) {
		final UUID uniqueId = projectile.getUniqueId();
		final List<Consumer<ProjectileHitEvent>> listeners = flyingProjectiles.getOrDefault(uniqueId, new ArrayList<>());

		listeners.add(hitTask);
		flyingProjectiles.put(uniqueId, listeners);
	}
}
