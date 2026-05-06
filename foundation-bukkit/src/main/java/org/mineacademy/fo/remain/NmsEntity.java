package org.mineacademy.fo.remain;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.ReflectionException;

import lombok.Getter;

/**
 * Advanced spawning of entities, enables manipulation of
 * an entity before it's added to the world.
 */
public final class NmsEntity {

	/**
	 * The world to add the entity to
	 */
	private final World bukkitWorld;

	/**
	 * The NMS entity class
	 */
	@Getter
	private final Object nmsEntity;

	/**
	 * Create an entity at X:0 Y:0 Z:0 in the first existing world
	 * You can use {@link EntityType#getEntityClass()} to get the class
	 *
	 * @param entityClass
	 */
	public NmsEntity(final Class<?> entityClass) {
		this(new Location(Bukkit.getWorlds().get(0), 0, 0, 0), entityClass);
	}

	/**
	 * Create an entity at the given location
	 * You can use {@link EntityType#getEntityClass()} to get the class
	 *
	 * @param location
	 * @param entityClass
	 */
	public NmsEntity(final Location location, final Class<?> entityClass) {
		try {
			NmsAccessor.call();
		} catch (final Throwable t) {
			throw new FoException(t, "Failed to setup entity reflection! MC version: " + MinecraftVersion.getCurrent());
		}

		this.bukkitWorld = location.getWorld();
		this.nmsEntity = this.createEntity(location, entityClass);
	}

	//
	// Creates the entity and registers in the NMS server
	//
	private Object createEntity(final Location location, final Class<?> entityClass) {
		try {
			return NmsAccessor.createEntity.invoke(this.bukkitWorld, location, entityClass);

		} catch (final ReflectiveOperationException e) {
			throw new FoException(e, "Error creating entity " + entityClass + " at " + location);
		}
	}

	/**
	 * Adds the entity to the world for the given spawn reason, calls Bukkit {@link CreatureSpawnEvent}
	 *
	 * @param <T>
	 * @param reason
	 * @return
	 */
	public <T extends Entity> T addEntity(final SpawnReason reason) {
		try {

			return (T) NmsAccessor.addEntity(this.bukkitWorld, this.nmsEntity, reason);

		} catch (final ReflectiveOperationException e) {
			throw new FoException(e, "Error creating entity " + this.nmsEntity + " for " + reason);
		}
	}

	/**
	 * Get the Bukkit entity
	 *
	 * @return
	 */
	public Entity getBukkitEntity() {
		try {
			return (Entity) NmsAccessor.getBukkitEntity.invoke(this.nmsEntity);

		} catch (final ReflectiveOperationException e) {
			throw new FoException(e, "Error getting bukkit entity from " + this.nmsEntity);
		}
	}
}

/**
 * A helper class accessing NMS internals
 */
final class NmsAccessor {

	/**
	 * The create entity method
	 */
	static final Method createEntity;

	/**
	 * The get bukkit entity method
	 */
	static final Method getBukkitEntity;

	/**
	 * The add entity method
	 */
	static Method addEntity;

	/**
	 * Does the {@link #addEntity} field have consumer function input?
	 */
	private static boolean hasEntityConsumer = false;

	/**
	 * Does the {@link #addEntity} field have randomize data boolean? 1.17+
	 */
	private static boolean hasRandomizeData = false;

	/**
	 * Static block initializer
	 */
	static void call() {
	}

	/**
	 * Load this class
	 */
	static {
		try {
			final Class<?> nmsEntity = Remain.getNMSClass("Entity", "net.minecraft.world.entity.Entity");
			final Class<?> ofcWorld = Remain.getOBCClass("CraftWorld");

			createEntity = ReflectionUtil.getMethod(ofcWorld, "createEntity", Location.class, Class.class);
			getBukkitEntity = nmsEntity.getMethod("getBukkitEntity");

			if (MinecraftVersion.newerThan(V.v1_10)) {
				hasEntityConsumer = true;

				try {
					addEntity = ReflectionUtil.getMethod(ofcWorld, "addEntity", nmsEntity, SpawnReason.class, Class.forName("org.bukkit.util.Consumer"));

				} catch (final ReflectionException ex) {
					addEntity = ReflectionUtil.getMethod(ofcWorld, "addEntity", nmsEntity, SpawnReason.class, Class.forName("org.bukkit.util.Consumer"), boolean.class);
					hasRandomizeData = true;
				}

			} else
				addEntity = ReflectionUtil.getMethod(ofcWorld, "addEntity", nmsEntity, SpawnReason.class);

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			throw new FoException(ex, "Error setting up nms entity accessor!");
		}
	}

	/**
	 * Adds an entity to the given world
	 *
	 * @param bukkitWorld
	 * @param nmsEntity
	 * @param reason
	 * @return
	 * @throws ReflectiveOperationException
	 */
	static Object addEntity(final World bukkitWorld, final Object nmsEntity, final SpawnReason reason) throws ReflectiveOperationException {
		if (hasEntityConsumer)
			if (hasRandomizeData)
				return addEntity.invoke(bukkitWorld, nmsEntity, reason, null, false);
			else
				return addEntity.invoke(bukkitWorld, nmsEntity, reason, null);

		return addEntity.invoke(bukkitWorld, nmsEntity, reason);
	}
}
