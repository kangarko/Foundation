package org.mineacademy.fo.remain.internal;

import org.bukkit.Location;
import org.mineacademy.fo.remain.CompBarColor;
import org.mineacademy.fo.remain.CompBarStyle;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the fake dragon entity
 * <p>
 * Typically you dont have to use this at all.
 */
@Getter
@Setter
abstract class NMSDragon {

	private float maxHealth = 200;
	private int x;
	private int y;
	private int z;

	private int pitch = 0;
	private int yaw = 0;
	private byte xvel = 0;
	private byte yvel = 0;
	private byte zvel = 0;
	private float health = 0;
	private boolean visible = false;
	private String name;
	private Object world;

	protected CompBarColor barColor;
	protected CompBarStyle barStyle;

	NMSDragon(String name, Location loc, int percent) {
		this.name = name;
		this.x = loc.getBlockX();
		this.y = loc.getBlockY();
		this.z = loc.getBlockZ();
		this.health = percent / 100F * maxHealth;
		this.world = Remain.getHandleWorld(loc.getWorld());
	}

	NMSDragon(String name, Location loc) {
		this.name = name;
		this.x = loc.getBlockX();
		this.y = loc.getBlockY();
		this.z = loc.getBlockZ();
		this.world = Remain.getHandleWorld(loc.getWorld());
	}

	void setHealth(int percent) {
		this.health = percent / 100F * maxHealth;
	}

	void setHealthF(float health) {
		this.health = health;
	}

	abstract Object getSpawnPacket();

	abstract Object getDestroyPacket();

	abstract Object getMetaPacket(Object watcher);

	abstract Object getTeleportPacket(Location loc);

	abstract Object getWatcher();

	abstract Object getNMSEntity();
}
