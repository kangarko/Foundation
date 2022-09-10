package org.mineacademy.fo.model;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Consumer;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

/**
 *
 */
@Getter
public class SimpleHologramStand extends SimpleHologram {

	/**
	 * The item or material this hologram will have
	 */
	private final Object itemOrMaterial;

	/**
	 * Is this item stand small?
	 */
	private boolean small;

	/**
	 * Is this item stand glowing?
	 */
	private boolean glowing;

	/**
	 * Create a new simple hologram using armor stand showing the given itemstack
	 *
	 * @param spawnLocation
	 * @param item
	 */
	public SimpleHologramStand(final Location spawnLocation, final ItemStack item) {
		super(spawnLocation);

		this.itemOrMaterial = item;
	}

	/**
	 * Create a new simple hologram using armor stand showing the given material
	 *
	 * @param spawnLocation
	 * @param material
	 */
	public SimpleHologramStand(final Location spawnLocation, final CompMaterial material) {
		super(spawnLocation);

		this.itemOrMaterial = material;
	}

	/**
	 * @see org.mineacademy.fo.model.SimpleHologram#createEntity()
	 */
	@Override
	protected final Entity createEntity() {
		final Consumer<ArmorStand> consumer = armorStand -> {
			armorStand.setGravity(false);
			armorStand.setHelmet(ItemCreator.of(this.itemOrMaterial).glow(this.glowing).make());
			armorStand.setVisible(false);
			armorStand.setSmall(this.small);
		};

		return this.getLastTeleportLocation().getWorld().spawn(this.getLastTeleportLocation(), ArmorStand.class, consumer);

		
	}

	/**
	 * @param glowing
	 * @return
	 */
	public final SimpleHologram setGlowing(final boolean glowing) {
		this.glowing = glowing;

		return this;
	}

	/**
	 * @param small the small to set
	 * @return
	 */
	public final SimpleHologram setSmall(final boolean small) {
		this.small = small;

		return this;
	}
}