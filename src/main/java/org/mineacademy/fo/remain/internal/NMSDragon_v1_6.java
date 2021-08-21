package org.mineacademy.fo.remain.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.mineacademy.fo.ReflectionUtil;

/**
 * Represents a fake dragon entity for Minecraft 1.6.x
 */
class NMSDragon_v1_6 extends NMSDragon {

	private static final Integer EntityID = 6000;

	public NMSDragon_v1_6(String name, Location loc) {
		super(name, loc);
	}

	@Override
	public Object getSpawnPacket() {
		final Class<?> mob_class = ReflectionUtil.getNMSClass("Packet24MobSpawn", "N/A");
		Object mobPacket = null;
		try {
			mobPacket = mob_class.newInstance();

			final Field a = ReflectionUtil.getDeclaredField(mob_class, "a");
			a.setAccessible(true);
			a.set(mobPacket, EntityID);// Entity ID

			final Field b = ReflectionUtil.getDeclaredField(mob_class, "b");
			b.setAccessible(true);
			b.set(mobPacket, EntityType.ENDER_DRAGON.getTypeId());// Mob type

			// (ID: 64)
			final Field c = ReflectionUtil.getDeclaredField(mob_class, "c");
			c.setAccessible(true);
			c.set(mobPacket, getX());// X position

			final Field d = ReflectionUtil.getDeclaredField(mob_class, "d");
			d.setAccessible(true);
			d.set(mobPacket, getY());// Y position

			final Field e = ReflectionUtil.getDeclaredField(mob_class, "e");
			e.setAccessible(true);
			e.set(mobPacket, getZ());// Z position

			final Field f = ReflectionUtil.getDeclaredField(mob_class, "f");
			f.setAccessible(true);
			f.set(mobPacket, (byte) (int) (getPitch() * 256.0F / 360.0F));// Pitch

			final Field g = ReflectionUtil.getDeclaredField(mob_class, "g");
			g.setAccessible(true);
			g.set(mobPacket, (byte) 0);// Head

			// Pitch
			final Field h = ReflectionUtil.getDeclaredField(mob_class, "h");
			h.setAccessible(true);
			h.set(mobPacket, (byte) (int) (getYaw() * 256.0F / 360.0F));// Yaw

			final Field i = ReflectionUtil.getDeclaredField(mob_class, "i");
			i.setAccessible(true);
			i.set(mobPacket, getXvel());// X velocity

			final Field j = ReflectionUtil.getDeclaredField(mob_class, "j");
			j.setAccessible(true);
			j.set(mobPacket, getYvel());// Y velocity

			final Field k = ReflectionUtil.getDeclaredField(mob_class, "k");
			k.setAccessible(true);
			k.set(mobPacket, getZvel());// Z velocity

			final Object watcher = getWatcher();
			final Field t = ReflectionUtil.getDeclaredField(mob_class, "t");
			t.setAccessible(true);
			t.set(mobPacket, watcher);

		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return mobPacket;
	}

	@Override
	public Object getDestroyPacket() {
		final Class<?> packet_class = ReflectionUtil.getNMSClass("Packet29DestroyEntity", "N/A");
		Object packet = null;

		try {
			packet = packet_class.newInstance();

			final Field a = ReflectionUtil.getDeclaredField(packet_class, "a");
			a.setAccessible(true);
			a.set(packet, new int[] { EntityID });
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return packet;
	}

	@Override
	public Object getMetaPacket(Object watcher) {
		final Class<?> packet_class = ReflectionUtil.getNMSClass("Packet40EntityMetadata", "N/A");
		Object packet = null;

		try {
			packet = packet_class.newInstance();

			final Field a = ReflectionUtil.getDeclaredField(packet_class, "a");
			a.setAccessible(true);
			a.set(packet, EntityID);

			final Method watcher_c = ReflectionUtil.getMethod(watcher.getClass(), "c");
			final Field b = ReflectionUtil.getDeclaredField(packet_class, "b");
			b.setAccessible(true);
			b.set(packet, watcher_c.invoke(watcher));
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return packet;
	}

	@Override
	public Object getTeleportPacket(Location loc) {
		final Class<?> packet_class = ReflectionUtil.getNMSClass("Packet34EntityTeleport", "N/A");
		Object packet = null;

		try {
			packet = packet_class.newInstance();

			final Field a = ReflectionUtil.getDeclaredField(packet_class, "a");
			a.setAccessible(true);
			a.set(packet, EntityID);

			final Field b = ReflectionUtil.getDeclaredField(packet_class, "b");
			b.setAccessible(true);
			b.set(packet, (int) Math.floor(loc.getX() * 32.0D));

			final Field c = ReflectionUtil.getDeclaredField(packet_class, "c");
			c.setAccessible(true);
			c.set(packet, (int) Math.floor(loc.getY() * 32.0D));

			final Field d = ReflectionUtil.getDeclaredField(packet_class, "d");
			d.setAccessible(true);
			d.set(packet, (int) Math.floor(loc.getZ() * 32.0D));

			final Field e = ReflectionUtil.getDeclaredField(packet_class, "e");
			e.setAccessible(true);
			e.set(packet, (byte) (int) (loc.getYaw() * 256.0F / 360.0F));

			final Field f = ReflectionUtil.getDeclaredField(packet_class, "f");
			f.setAccessible(true);
			f.set(packet, (byte) (int) (loc.getPitch() * 256.0F / 360.0F));
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
		return packet;
	}

	@Override
	public Object getWatcher() {
		final Class<?> watcher_class = ReflectionUtil.getNMSClass("DataWatcher", "N/A");
		Object watcher = null;

		try {
			watcher = watcher_class.newInstance();

			final Method a = ReflectionUtil.getMethod(watcher_class, "a", int.class, Object.class);
			a.setAccessible(true);

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
		return null;
	}
}
