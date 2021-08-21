package org.mineacademy.fo.remain.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Location;
import org.mineacademy.fo.ReflectionUtil;

/**
 * Represents a fake dragon entity for Minecraft 1.7.x
 */
class NMSDragon_v1_7 extends NMSDragon {

	private Object dragon;
	private int id;

	public NMSDragon_v1_7(String name, Location loc) {
		super(name, loc);
	}

	@Override
	public Object getSpawnPacket() {
		final Class<?> Entity = ReflectionUtil.getNMSClass("Entity", "N/A");
		final Class<?> EntityLiving = ReflectionUtil.getNMSClass("EntityLiving", "N/A");
		final Class<?> EntityEnderDragon = ReflectionUtil.getNMSClass("EntityEnderDragon", "N/A");
		Object packet = null;
		try {
			dragon = EntityEnderDragon.getConstructor(ReflectionUtil.getNMSClass("World", "N/A")).newInstance(getWorld());

			final Method setLocation = ReflectionUtil.getMethod(EntityEnderDragon, "setLocation", double.class, double.class, double.class, float.class, float.class);
			setLocation.invoke(dragon, getX(), getY(), getZ(), getPitch(), getYaw());

			final Method setInvisible = ReflectionUtil.getMethod(EntityEnderDragon, "setInvisible", boolean.class);
			setInvisible.invoke(dragon, isVisible());

			final Method setCustomName = ReflectionUtil.getMethod(EntityEnderDragon, "setCustomName", String.class);
			setCustomName.invoke(dragon, getName());

			final Method setHealth = ReflectionUtil.getMethod(EntityEnderDragon, "setHealth", float.class);
			setHealth.invoke(dragon, getHealth());

			final Field motX = ReflectionUtil.getDeclaredField(Entity, "motX");
			motX.set(dragon, getXvel());

			final Field motY = ReflectionUtil.getDeclaredField(Entity, "motY");
			motY.set(dragon, getYvel());

			final Field motZ = ReflectionUtil.getDeclaredField(Entity, "motZ");
			motZ.set(dragon, getZvel());

			final Method getId = ReflectionUtil.getMethod(EntityEnderDragon, "getId");
			this.id = (Integer) getId.invoke(dragon);

			final Class<?> PacketPlayOutSpawnEntityLiving = ReflectionUtil.getNMSClass("PacketPlayOutSpawnEntityLiving", "N/A");

			packet = PacketPlayOutSpawnEntityLiving.getConstructor(EntityLiving).newInstance(dragon);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return packet;
	}

	@Override
	public Object getDestroyPacket() {
		final Class<?> PacketPlayOutEntityDestroy = ReflectionUtil.getNMSClass("PacketPlayOutEntityDestroy", "N/A");

		Object packet = null;
		try {
			packet = PacketPlayOutEntityDestroy.newInstance();
			final Field a = PacketPlayOutEntityDestroy.getDeclaredField("a");
			a.setAccessible(true);
			a.set(packet, new int[] { id });
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return packet;
	}

	@Override
	public Object getMetaPacket(Object watcher) {
		final Class<?> DataWatcher = ReflectionUtil.getNMSClass("DataWatcher", "N/A");

		final Class<?> PacketPlayOutEntityMetadata = ReflectionUtil.getNMSClass("PacketPlayOutEntityMetadata", "N/A");

		Object packet = null;
		try {
			packet = PacketPlayOutEntityMetadata.getConstructor(int.class, DataWatcher, boolean.class).newInstance(id, watcher, true);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return packet;
	}

	@Override
	public Object getTeleportPacket(Location loc) {
		final Class<?> PacketPlayOutEntityTeleport = ReflectionUtil.getNMSClass("PacketPlayOutEntityTeleport", "N/A");

		Object packet = null;

		try {
			packet = PacketPlayOutEntityTeleport.getConstructor(int.class, int.class, int.class, int.class, byte.class, byte.class).newInstance(this.id, loc.getBlockX() * 32, loc.getBlockY() * 32, loc.getBlockZ() * 32, (byte) ((int) loc.getYaw() * 256 / 360), (byte) ((int) loc.getPitch() * 256 / 360));
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return packet;
	}

	@Override
	public Object getWatcher() {
		final Class<?> Entity = ReflectionUtil.getNMSClass("Entity", "N/A");
		final Class<?> DataWatcher = ReflectionUtil.getNMSClass("DataWatcher", "N/A");

		Object watcher = null;
		try {
			watcher = DataWatcher.getConstructor(Entity).newInstance(dragon);
			final Method a = ReflectionUtil.getMethod(DataWatcher, "a", int.class, Object.class);

			a.invoke(watcher, 0, isVisible() ? (byte) 0 : (byte) 0x20);
			a.invoke(watcher, 6, getHealth());
			a.invoke(watcher, 7, 0);
			a.invoke(watcher, 8, (byte) 0);
			a.invoke(watcher, 10, getName());
			a.invoke(watcher, 11, (byte) 1);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
		return watcher;
	}

	@Override
	Object getNMSEntity() {
		return this.dragon;
	}
}
