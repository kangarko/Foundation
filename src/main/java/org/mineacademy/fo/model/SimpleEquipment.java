package org.mineacademy.fo.model;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompEquipmentSlot;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * A more robust alternative to the {@link EntityEquipment} class found in Bukkit
 *
 * @deprecated subject for removal, simply use {@link LivingEntity#getEquipment()} and
 *  		   achieve the same outcome there on the Bukkit API instead
 */
@Deprecated
@Getter
public final class SimpleEquipment {

	/**
	 * The Bukkit equipment
	 */
	private final EntityEquipment equipment;

	/**
	 * Wrap the equipment for the given entity
	 *
	 * @param entity
	 */
	public SimpleEquipment(final LivingEntity entity) {
		this(entity.getEquipment());
	}

	/**
	 * Create a new simple equipment
	 *
	 * @param equipment
	 */
	public SimpleEquipment(final EntityEquipment equipment) {
		this.equipment = equipment;
	}

	// ------------------------------------------------------------------------------------------
	// Main settings methods
	// ------------------------------------------------------------------------------------------

	/**
	 * Sets the given slot to the given item
	 *
	 * @param slot
	 * @param material
	 */
	public void set(final CompEquipmentSlot slot, final CompMaterial material) {
		set(slot, material.toItem());
	}

	/**
	 * Sets the given slot to the given item
	 *
	 * @param slot
	 * @param builder
	 */
	public void set(final CompEquipmentSlot slot, final ItemCreator.ItemCreatorBuilder builder) {
		set(slot, builder.build().make());
	}

	/**
	 * Sets the given slot to the given item
	 *
	 * @param slot
	 * @param item
	 */
	public void set(final CompEquipmentSlot slot, final ItemStack item) {
		Valid.checkNotNull(item, "Equipment item cannot be null");

		set(slot, item, null);
	}

	/**
	 * Updates the drop chance (0.0-1.0 for the given slot)
	 *
	 * @param slot
	 * @param dropChance
	 */
	public void set(final CompEquipmentSlot slot, final float dropChance) {
		set(slot, (ItemStack) null, dropChance);
	}

	/**
	 * Sets the given slot with the given item and drop chance (0.0-1.0)
	 *
	 * @param slot
	 * @param material
	 * @param dropChance
	 */
	public void set(final CompEquipmentSlot slot, final CompMaterial material, final Float dropChance) {
		set(slot, material.toItem(), dropChance);
	}

	/**
	 * Sets the given slot with the given item and drop chance (0.0-1.0)
	 *
	 * @param slot
	 * @param builder
	 * @param dropChance
	 */
	public void set(final CompEquipmentSlot slot, final ItemCreator.ItemCreatorBuilder builder, final Float dropChance) {
		set(slot, builder.build().make(), dropChance);
	}

	/**
	 * Sets the given slot with the given item and drop chance (0.0-1.0)
	 *
	 * @param slot
	 * @param item
	 * @param dropChance
	 */
	public void set(CompEquipmentSlot slot, final ItemStack item, final Float dropChance) {
		Valid.checkBoolean(item != null || dropChance != null, "Either item or drop chance must be given!");

		if (slot.toString().equals("OFF_HAND") && MinecraftVersion.olderThan(V.v1_9))
			slot = CompEquipmentSlot.HAND;

		if (slot == CompEquipmentSlot.HEAD) {
			if (item != null)
				equipment.setHelmet(item);
			if (dropChance != null)
				equipment.setHelmetDropChance(dropChance);
		} else if (slot == CompEquipmentSlot.CHEST) {
			if (item != null)
				equipment.setChestplate(item);
			if (dropChance != null)
				equipment.setChestplateDropChance(dropChance);
		} else if (slot == CompEquipmentSlot.LEGS) {
			if (item != null)
				equipment.setLeggings(item);
			if (dropChance != null)
				equipment.setLeggingsDropChance(dropChance);
		} else if (slot == CompEquipmentSlot.FEET) {
			if (item != null)
				equipment.setBoots(item);
			if (dropChance != null)
				equipment.setBootsDropChance(dropChance);
		} else if (slot == CompEquipmentSlot.HAND) {
			if (item != null)
				equipment.setItemInHand(item);
			if (dropChance != null)
				equipment.setItemInHandDropChance(dropChance);
		} else if (slot.toString().equals("OFF_HAND"))
			try {
				if (item != null)
					equipment.setItemInOffHand(item);
				if (dropChance != null)
					equipment.setItemInOffHandDropChance(dropChance);
			} catch (final Throwable t) {
			}

		else
			throw new FoException("Does not know how to set " + slot + " to " + item);
	}

	// ------------------------------------------------------------------------------------------
	// Armor content
	// ------------------------------------------------------------------------------------------

	/**
	 * Return the item stack array of all content
	 *
	 * @return
	 */
	public ItemStack[] getArmorContents() {
		return equipment.getArmorContents();
	}

	/**
	 * Set the armor content for this entity
	 *
	 * @param helmet
	 * @param chest
	 * @param leggings
	 * @param boots
	 */
	public void setContent(final ItemCreator.ItemCreatorBuilder helmet, final ItemCreator.ItemCreatorBuilder chest, final ItemCreator.ItemCreatorBuilder leggings, final ItemCreator.ItemCreatorBuilder boots) {
		setContent(helmet.build().make(), chest.build().make(), leggings.build().make(), boots.build().make());
	}

	/**
	 * Set the armor content for this entity
	 *
	 * @param helmet
	 * @param chest
	 * @param leggings
	 * @param boots
	 */
	public void setContent(final CompMaterial helmet, final CompMaterial chest, final CompMaterial leggings, final CompMaterial boots) {
		setContent(helmet.toItem(), chest.toItem(), leggings.toItem(), boots.toItem());
	}

	/**
	 * Set the armor content for this entity
	 *
	 * @param helmet
	 * @param chest
	 * @param leggings
	 * @param boots
	 */
	public void setContent(final ItemStack helmet, final ItemStack chest, final ItemStack leggings, final ItemStack boots) {
		set(CompEquipmentSlot.HEAD, helmet);
		set(CompEquipmentSlot.CHEST, chest);
		set(CompEquipmentSlot.FEET, leggings);
		set(CompEquipmentSlot.LEGS, boots);
	}

	// ------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------

	/**
	 * Removes entity all equipment
	 */
	public void clear() {
		equipment.clear();
	}

}