package org.mineacademy.fo.remain;

import javax.annotation.Nullable;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents {@link EquipmentSlot}
 */
@RequiredArgsConstructor
public enum CompEquipmentSlot {

	HAND("HAND", "HAND"),
	/**
	 * Requires Minecraft 1.9+
	 */
	OFF_HAND("OFF_HAND", "OFF_HAND"),
	HEAD("HEAD", "HELMET"),
	CHEST("CHEST", "CHESTPLATE"),
	LEGS("LEGS", "LEGGINGS"),
	FEET("FEET", "BOOTS");

	/**
	 * The localizable key
	 */
	@Getter
	private final String key;

	/**
	 * The alternative Bukkit name.
	 */
	@Getter
	private final String bukkitName;

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param item
	 */
	public void applyTo(LivingEntity entity, ItemStack item) {
		this.applyTo(entity, item, null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param item
	 * @param dropChance
	 */
	public void applyTo(LivingEntity entity, ItemStack item, @Nullable Double dropChance) {
		final EntityEquipment equipment = entity instanceof LivingEntity ? entity.getEquipment() : null;
		Valid.checkNotNull(equipment);

		final boolean lacksDropChance = entity instanceof HumanEntity || entity.getType().toString().equals("ARMOR_STAND");

		switch (this) {

			case HAND:
				equipment.setItemInHand(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setItemInHandDropChance(dropChance.floatValue());

				break;

			case OFF_HAND:
				Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_9), "Setting off hand item requires Minecraft 1.9+");

				equipment.setItemInOffHand(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setItemInOffHandDropChance(dropChance.floatValue());

				break;

			case HEAD:
				equipment.setHelmet(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setHelmetDropChance(dropChance.floatValue());

				break;

			case CHEST:
				equipment.setChestplate(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setChestplateDropChance(dropChance.floatValue());

				break;

			case LEGS:
				equipment.setLeggings(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setLeggingsDropChance(dropChance.floatValue());

				break;

			case FEET:
				equipment.setBoots(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setBootsDropChance(dropChance.floatValue());

				break;
		}
	}

	/**
	 * Attempts to parse equip. slot from the given key, or throwing
	 * an error if not found
	 *
	 * @param key
	 * @return
	 */
	public static CompEquipmentSlot fromKey(String key) {
		key = key.toUpperCase().replace(" ", "_");

		for (final CompEquipmentSlot slot : values())
			if (slot.key.equals(key) || slot.bukkitName.equals(key))
				return slot;

		throw new FoException("No such equipment slot from key: " + key);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 * that does not drop.
	 *
	 * @param entity
	 * @param color
	 */
	public static void applyDyeToAllSlots(LivingEntity entity, CompColor color) {
		applyDyeToAllSlots(entity, color, 0D);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 *
	 * @param entity
	 * @param color
	 * @param dropChance
	 */
	public static void applyDyeToAllSlots(LivingEntity entity, CompColor color, Double dropChance) {
		CompEquipmentSlot.HEAD.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_HELMET).color(color).make(), dropChance);
		CompEquipmentSlot.CHEST.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_CHESTPLATE).color(color).make(), dropChance);
		CompEquipmentSlot.LEGS.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_LEGGINGS).color(color).make(), dropChance);
		CompEquipmentSlot.FEET.applyTo(entity, ItemCreator.of(CompMaterial.LEATHER_BOOTS).color(color).make(), dropChance);
	}

	@Override
	public String toString() {
		return this.key.toUpperCase();
	}
}
