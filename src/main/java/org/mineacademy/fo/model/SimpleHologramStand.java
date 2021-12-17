package org.mineacademy.fo.model;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 *
 */
@Getter
public class SimpleHologramStand extends SimpleHologram {

	/**
	 * The material this hologram will have
	 */
	private final CompMaterial material;

	/**
	 * Is this item stand small?
	 */
	private boolean small;

	/**
	 * Is this item stand glowing?
	 */
	private boolean glowing;

	/**
	 * Create a new simple hologram using armor stand showing the given material
	 *
	 * @param spawnLocation
	 * @param material
	 */
	public SimpleHologramStand(Location spawnLocation, CompMaterial material) {
		super(spawnLocation);

		this.material = material;
	}

	/**
	 * @see org.mineacademy.fo.model.SimpleHologram#createEntity()
	 */
	@Override
	protected final Entity createEntity() {
		final ArmorStand armorStand = this.getLastTeleportLocation().getWorld().spawn(this.getLastTeleportLocation(), ArmorStand.class);

		armorStand.setGravity(false);
		armorStand.setHelmet(ItemCreator.of(material).glow(this.glowing).make());
		armorStand.setVisible(false);
		armorStand.setSmall(this.small);

		return armorStand;
	}

	/**
	 *
	 * @param glowing
	 * @return
	 */
	public final SimpleHologram setGlowing(boolean glowing) {
		this.glowing = glowing;

		return this;
	}

	/**
	 * @param small the small to set
	 * @return
	 */
	public final SimpleHologram setSmall(boolean small) {
		this.small = small;

		return this;
	}
}
