package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompParticle;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
public abstract class SimpleHologram {

	/**
	 * The distance between each line of lore for this item
	 */
	@Getter
	@Setter
	private static double loreLineHeight = 0.26D;

	/**
	 * A registry of created animated items
	 */
	@Getter
	private static Set<SimpleHologram> registeredItems = new HashSet<>();

	/**
	 * The armor stand names, each line spawns another invisible stand
	 */
	private final List<ArmorStand> loreEntities = new ArrayList<>();

	/**
	 * The spawning location
	 */
	private final Location lastTeleportLocation;

	/**
	 * The lore over the item
	 */
	@Getter
	private final List<String> loreLines = new ArrayList<>();

	/**
	 * Optional particles spawning below the hologram
	 */
	@Getter
	private final List<Tuple<CompParticle, Object>> particles = new ArrayList<>();

	/**
	 * The displayed entity
	 */
	@Getter
	private Entity entity;

	/*
	 * A private flag to help with teleporting of this entity
	 */
	private Location pendingTeleport = null;

	/*
	 * Constructs a new item and registers it
	 */
	protected SimpleHologram(Location spawnLocation) {
		this.lastTeleportLocation = spawnLocation.clone();

		registeredItems.add(this);
	}

	/**
	 * Spawns this hologram entity
	 *
	 * @return
	 */
	public SimpleHologram spawn() {
		Valid.checkBoolean(!this.isSpawned(), this + " is already spawned!");
		Valid.checkNotEmpty(this.loreLines, "Call lore() first before calling spawn method for " + this);

		this.entity = this.createEntity();
		Valid.checkNotNull(this.entity, "Failed to spawn entity from " + this);

		this.drawLore(this.lastTeleportLocation);

		return this;
	}

	/**
	 * Core implementation method to spawn your entity
	 *
	 * @return
	 */
	protected abstract Entity createEntity();

	/*
	 * Set a lore for this armor stand
	 */
	private void drawLore(Location location) {

		if (this.entity instanceof ArmorStand && ((ArmorStand) this.entity).isSmall())
			location = location.add(0, -0.5, 0);

		for (final String loreLine : this.loreLines) {
			final ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

			armorStand.setGravity(false);
			armorStand.setVisible(false);

			Remain.setCustomName(armorStand, loreLine);

			location = location.subtract(0, loreLineHeight, 0);

			this.loreEntities.add(armorStand);
		}
	}

	/*
	 * Iterate the ticking mechanism of this entity
	 */
	private void tick() {

		if (this.pendingTeleport != null) {
			this.entity.teleport(this.pendingTeleport);

			for (final ArmorStand loreEntity : this.loreEntities)
				loreEntity.teleport(this.pendingTeleport);

			this.pendingTeleport = null;
			return;
		}

		this.onTick();

		for (final Tuple<CompParticle, Object> tuple : this.particles) {
			final CompParticle particle = tuple.getKey();
			final Object extra = tuple.getValue();

			if (extra instanceof CompMaterial)
				particle.spawn(this.getLocation(), (CompMaterial) extra);

			else if (extra instanceof Double)
				particle.spawn(this.getLocation(), (double) extra);
		}
	}

	/**
	 * Called automatically where you can animate this armor stand
	 */
	protected void onTick() {
	}

	/**
	 * Return true if this armor stand is spawned
	 *
	 * @return
	 */
	public final boolean isSpawned() {
		return this.entity != null;
	}

	/**
	 * Deletes all text that the armor stand has
	 */
	public final void removeLore() {
		this.loreEntities.forEach(stand -> stand.remove());
	}

	/**
	 *
	 * @param lore
	 * @return
	 */
	public final SimpleHologram setLore(String... lore) {
		this.loreLines.clear();
		this.loreLines.addAll(Arrays.asList(lore));

		return this;
	}

	/**
	 * Add particle effect for this hologram
	 *
	 * @param particle
	 */
	public final void addParticleEffect(CompParticle particle) {
		this.addParticleEffect(particle, null);
	}

	/**
	 * Add particle effect for this hologram
	 *
	 * @param particle
	 * @param data
	 */
	public final void addParticleEffect(CompParticle particle, CompMaterial data) {
		this.particles.add(new Tuple<>(particle, data));
	}

	/**
	 * Return the current armor stand location
	 *
	 * @return
	 */
	public final Location getLocation() {
		this.checkSpawned("getLocation");

		return this.entity.getLocation();
	}

	/**
	 * Return the last known teleport location
	 *
	 * @return
	 */
	public final Location getLastTeleportLocation() {
		return lastTeleportLocation.clone();
	}

	/**
	 * Teleport this hologram with its lores to the given location
	 *
	 * @param location
	 */
	public final void teleport(Location location) {
		Valid.checkBoolean(this.pendingTeleport == null, this + " is already pending teleport to " + this.pendingTeleport);
		this.checkSpawned("teleport");

		this.lastTeleportLocation.setX(location.getY());
		this.lastTeleportLocation.setY(location.getY());
		this.lastTeleportLocation.setZ(location.getZ());

		this.pendingTeleport = location;
	}

	/**
	 * Deletes this armor stand
	 */
	public final void remove() {
		this.removeLore();

		if (this.entity != null)
			this.entity.remove();

		registeredItems.remove(this);
	}

	/*
	 * A helper method to check if this entity is spawned
	 */
	private void checkSpawned(String method) {
		Valid.checkBoolean(this.isSpawned(), this + " is not spawned, cannot call " + method + "!");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ArmorStandItem{spawnLocation=" + Common.shortLocation(this.lastTeleportLocation) + ", spawned=" + this.isSpawned() + "}";
	}

	/**
	 * Called internally from Foundation launch ticking mechanism task
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static final void init() {

		Common.runTimer(1, () -> {

			for (final Iterator<SimpleHologram> it = registeredItems.iterator(); it.hasNext();) {
				final SimpleHologram model = it.next();

				if (model.isSpawned()) {
					if (!model.getEntity().isValid() || model.getEntity().isDead()) {
						model.removeLore();
						model.getEntity().remove();

						it.remove();
					} else
						model.tick();
				}
			}
		});
	}

	/**
	 * Deletes all floating items on the server
	 */
	public static final void deleteAll() {

		for (final Iterator<SimpleHologram> it = registeredItems.iterator(); it.hasNext();) {
			final SimpleHologram item = it.next();

			if (item.isSpawned())
				item.getEntity().remove();

			item.removeLore();
			it.remove();
		}
	}
}
