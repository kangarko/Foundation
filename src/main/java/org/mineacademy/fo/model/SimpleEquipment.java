package org.mineacademy.fo.model;

import javax.annotation.Nullable;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * A more robust alternative to the {@link EntityEquipment} class found in Bukkit
 */
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
	public SimpleEquipment(LivingEntity entity) {
		this(entity.getEquipment());
	}

	/**
	 * Create a new simple equipment
	 *
	 * @param equipment
	 */
	public SimpleEquipment(EntityEquipment equipment) {
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
	public void set(EquipmentSlot slot, CompMaterial material) {
		set(slot, material.toItem());
	}

	/**
	 * Sets the given slot to the given item
	 *
	 * @param slot
	 * @param builder
	 */
	public void set(EquipmentSlot slot, ItemCreator.ItemCreatorBuilder builder) {
		set(slot, builder.build().make());
	}

	/**
	 * Sets the given slot to the given item
	 *
	 * @param slot
	 * @param item
	 */
	public void set(EquipmentSlot slot, ItemStack item) {
		Valid.checkNotNull(item, "Equipment item cannot be null");

		set(slot, item, null);
	}

	/**
	 * Updates the drop chance (0.0-1.0 for the given slot)
	 *
	 * @param slot
	 * @param dropChance
	 */
	public void set(EquipmentSlot slot, float dropChance) {
		set(slot, (ItemStack) null, dropChance);
	}

	/**
	 * Sets the given slot with the given item and drop chance (0.0-1.0)
	 *
	 * @param slot
	 * @param material
	 * @param dropChance
	 */
	public void set(EquipmentSlot slot, @Nullable CompMaterial material, @Nullable Float dropChance) {
		set(slot, material.toItem(), dropChance);
	}

	/**
	 * Sets the given slot with the given item and drop chance (0.0-1.0)
	 *
	 * @param slot
	 * @param builder
	 * @param dropChance
	 */
	public void set(EquipmentSlot slot, @Nullable ItemCreator.ItemCreatorBuilder builder, @Nullable Float dropChance) {
		set(slot, builder.build().make(), dropChance);
	}

	/**
	 * Sets the given slot with the given item and drop chance (0.0-1.0)
	 *
	 * @param slot
	 * @param item
	 * @param dropChance
	 */
	public void set(EquipmentSlot slot, @Nullable ItemStack item, @Nullable Float dropChance) {
		Valid.checkBoolean(item != null || dropChance != null, "Either item or drop chance must be given!");

		if (slot == EquipmentSlot.HEAD) {
			if (item != null)
				equipment.setHelmet(item);
			if (dropChance != null)
				equipment.setHelmetDropChance(dropChance);
		}

		else if (slot == EquipmentSlot.CHEST) {
			if (item != null)
				equipment.setChestplate(item);
			if (dropChance != null)
				equipment.setChestplateDropChance(dropChance);
		}

		else if (slot == EquipmentSlot.LEGS) {
			if (item != null)
				equipment.setLeggings(item);
			if (dropChance != null)
				equipment.setLeggingsDropChance(dropChance);
		}

		else if (slot == EquipmentSlot.FEET) {
			if (item != null)
				equipment.setBoots(item);
			if (dropChance != null)
				equipment.setBootsDropChance(dropChance);
		}

		else if (slot == EquipmentSlot.HAND) {
			if (item != null)
				equipment.setItemInHand(item);
			if (dropChance != null)
				equipment.setItemInHandDropChance(dropChance);
		}

		else if (slot.toString().equals("OFF_HAND"))
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
	public void setContent(ItemCreator.ItemCreatorBuilder helmet, ItemCreator.ItemCreatorBuilder chest, ItemCreator.ItemCreatorBuilder leggings, ItemCreator.ItemCreatorBuilder boots) {
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
	public void setContent(CompMaterial helmet, CompMaterial chest, CompMaterial leggings, CompMaterial boots) {
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
	public void setContent(ItemStack helmet, ItemStack chest, ItemStack leggings, ItemStack boots) {
		set(EquipmentSlot.HEAD, helmet);
		set(EquipmentSlot.CHEST, chest);
		set(EquipmentSlot.FEET, leggings);
		set(EquipmentSlot.LEGS, boots);
	}

	/**
	 * Set the armor content for this entity
	 *
	 * @param content
	 */
	public void setContent(ArmorContent content) {
		set(EquipmentSlot.HEAD, content.getHelmet());
		set(EquipmentSlot.CHEST, content.getChestplate());
		set(EquipmentSlot.FEET, content.getLeggings());
		set(EquipmentSlot.LEGS, content.getLeggings());
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
