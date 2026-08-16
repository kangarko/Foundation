package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents EquipmentSlot
 */
@RequiredArgsConstructor
public enum CompEquipmentSlot {

	HAND("HAND", "HAND", null, 0, false),
	/**
	 * Requires Minecraft 1.9+
	 */
	OFF_HAND("OFF_HAND", "OFF_HAND", V.v1_9, 0, false),
	HEAD("HEAD", "HELMET", null, 0, false),
	CHEST("CHEST", "CHESTPLATE", null, 0, false),
	LEGS("LEGS", "LEGGINGS", null, 0, false),
	FEET("FEET", "BOOTS", null, 0, false),
	/**
	 * Body armor of wolves, horses and llamas, requires Minecraft 1.20.5+
	 */
	BODY("BODY", "BODY", V.v1_20, 5, true),
	/**
	 * Saddle of rideable animals, requires Minecraft 1.21.5+
	 */
	SADDLE("SADDLE", "SADDLE", V.v1_21, 5, true);

	/**
	 * The localizable key
	 */
	@Getter
	private final String key;

	/**
	 * The alternative Bukkit name.
	 */
	private final String bukkitName;

	/**
	 * The oldest Minecraft version shipping this slot, null if it always existed.
	 */
	private final V minimumVersion;

	/**
	 * The subversion of {@link #minimumVersion} such as 5 in 1.20.5.
	 */
	private final int minimumSubversion;

	/**
	 * True if only animals carry this slot and players lack it entirely.
	 */
	private final boolean animalOnly;

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param itemCreator
	 */
	public void applyTo(final LivingEntity entity, final ItemCreator itemCreator) {
		this.applyTo(entity, itemCreator.make(), null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param itemCreator
	 * @param dropChance
	 */
	public void applyTo(final LivingEntity entity, final ItemCreator itemCreator, final Double dropChance) {
		this.applyTo(entity, itemCreator.make(), dropChance);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param material
	 */
	public void applyTo(final LivingEntity entity, final CompMaterial material) {
		this.applyTo(entity, material.toItem(), null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param material
	 * @param dropChance
	 */
	public void applyTo(final LivingEntity entity, final CompMaterial material, final Double dropChance) {
		this.applyTo(entity, material.toItem(), dropChance);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param material
	 */
	public void applyTo(final LivingEntity entity, final Material material) {
		this.applyTo(entity, new ItemStack(material), null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * @param entity
	 * @param material
	 * @param dropChance
	 */
	public void applyTo(final LivingEntity entity, final Material material, final Double dropChance) {
		this.applyTo(entity, new ItemStack(material), dropChance);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item
	 *
	 * @param entity
	 * @param item
	 */
	public void applyTo(final LivingEntity entity, final ItemStack item) {
		this.applyTo(entity, item, null);
	}

	/**
	 * Clear this equipment slot.
	 *
	 * @param entity
	 */
	public void clear(final LivingEntity entity) {
		this.applyTo(entity, (ItemStack) null, (Double) null);
	}

	/**
	 * Applies this equipment slot to the given entity with the given item,
	 * and optional drop chance from 0 to 1.0
	 *
	 * Does nothing when the entity has no such slot, see {@link #isApplicableTo(LivingEntity)}.
	 *
	 * @param entity
	 * @param item
	 * @param dropChance
	 */
	public void applyTo(@NonNull final LivingEntity entity, ItemStack item, final Double dropChance) {
		if (!this.isAvailable())
			throw new FoException("Equipment slot " + this.name() + " requires Minecraft " + this.getMinimumVersion() + "+, this server runs " + MinecraftVersion.getFullVersion()
					+ ". Iterate CompEquipmentSlot#getAvailable() instead of values() to skip slots this server lacks.", false);

		if (!this.isApplicableTo(entity))
			return;

		final EntityEquipment equipment = entity.getEquipment();
		ValidCore.checkNotNull(equipment, "Entity " + entity.getType() + " has no equipment to set " + this.name() + " on");

		final boolean lacksDropChance = entity instanceof HumanEntity || entity.getType().toString().equals("ARMOR_STAND") || entity.getType().toString().equals("MANNEQUIN");

		if (MinecraftVersion.olderThan(V.v1_9) && item == null)
			item = new ItemStack(Material.AIR);

		switch (this) {
			case HAND:
				if (entity instanceof Enderman) {
					final Enderman enderman = (Enderman) entity;

					if (item != null && item.getType().isBlock())
						try {
							enderman.setCarriedBlock(Bukkit.createBlockData(item.getType()));

						} catch (final Throwable t) {
							enderman.setCarriedMaterial(item.getData());
						}

				} else {
					equipment.setItemInHand(item);

					if (dropChance != null && !lacksDropChance)
						equipment.setItemInHandDropChance(dropChance.floatValue());
				}

				break;

			case OFF_HAND:
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

			case BODY:
			case SADDLE:
				this.applyToModernSlot(equipment, item, dropChance, lacksDropChance);

				break;
		}
	}

	/*
	 * Body armor and saddles never had a dedicated EntityEquipment setter, they are
	 * only reachable through the slot based API added in Minecraft 1.9.
	 */
	private void applyToModernSlot(final EntityEquipment equipment, final ItemStack item, final Double dropChance, final boolean lacksDropChance) {
		final EquipmentSlot bukkitSlot = this.toBukkit();

		equipment.setItem(bukkitSlot, item);

		if (dropChance != null && !lacksDropChance)
			equipment.setDropChance(bukkitSlot, dropChance.floatValue());
	}

	/**
	 * Return true if the slot is available in this MC version.
	 *
	 * @return
	 */
	public boolean isAvailable() {
		if (this.minimumVersion == null)
			return true;

		return MinecraftVersion.atLeast(this.minimumVersion, this.minimumSubversion);
	}

	/**
	 * Return true if the given entity carries this slot on this MC version. Body armor
	 * and saddles belong to animals only, players have no such slot: older Minecraft
	 * versions throw when you touch it on a player and newer ones silently write into
	 * a slot the player can never wear.
	 *
	 * @param entity
	 * @return
	 */
	public boolean isApplicableTo(@NonNull final LivingEntity entity) {
		if (!this.isAvailable())
			return false;

		if (this.animalOnly && entity instanceof HumanEntity)
			return false;

		return true;
	}

	/**
	 * Return the oldest Minecraft version shipping this slot such as "1.20.5",
	 * or null if this slot always existed.
	 *
	 * @return
	 */
	public String getMinimumVersion() {
		if (this.minimumVersion == null)
			return null;

		return this.minimumVersion.toString() + (this.minimumSubversion > 0 ? "." + this.minimumSubversion : "");
	}

	/**
	 * Return the Bukkit name of this equipment
	 * or throw an error if not found
	 *
	 * @return
	 */
	public String getBukkitName() {
		ValidCore.checkNotNull(this.bukkitName, "CompEquipmentSlot." + this.name() + " does not have a Bukkit counterpart!");

		return this.bukkitName;
	}

	/**
	 * Return the Bukkit equipment slot of this equipment
	 * or throw an error if not found
	 *
	 * @return
	 */
	public EquipmentSlot toBukkit() {
		return ReflectionUtil.lookupEnum(EquipmentSlot.class, this.getBukkitName());
	}

	/**
	 * Return all available equipment slots. Iterate this instead of {@link #values()},
	 * which also lists slots only newer Minecraft versions have.
	 *
	 * @return
	 */
	public static List<CompEquipmentSlot> getAvailable() {
		final List<CompEquipmentSlot> availableSlots = new ArrayList<>();

		for (final CompEquipmentSlot slot : values())
			if (slot.isAvailable())
				availableSlots.add(slot);

		return Collections.unmodifiableList(availableSlots);
	}

	/**
	 * Attempts to parse equip. slot from the given key, or throwing
	 * an error if not found.
	 *
	 * Stays an {@link IllegalArgumentException} because {@link ReflectionUtil#lookupEnumSilent(Class, String)}
	 * calls this method reflectively and only treats that type as "no such value".
	 *
	 * @param key
	 * @return
	 */
	public static CompEquipmentSlot fromKey(String key) {
		key = key.toUpperCase().replace(" ", "_");

		for (final CompEquipmentSlot slot : values())
			if (slot.key.equals(key) || slot.bukkitName.equals(key))
				return slot;

		throw new IllegalArgumentException("No such equipment slot '" + key + "'. Available: " + CommonCore.join(values()));
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 * that does not drop.
	 *
	 * @param entity
	 * @param color
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color) {
		applyArmor(entity, color, 0D, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 * that does not drop.
	 *
	 * @param entity
	 * @param color
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color, final Set<CompEquipmentSlot> ignoredSlots) {
		applyArmor(entity, color, 0D, ignoredSlots);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 *
	 * @param entity
	 * @param color
	 * @param dropChance
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color, final double dropChance) {
		applyArmor(entity, color, dropChance, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full leather armor in the given color
	 *
	 * @param entity
	 * @param color
	 * @param dropChance
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color, final Double dropChance, final Set<CompEquipmentSlot> ignoredSlots) {
		if (!ignoredSlots.contains(HEAD))
			HEAD.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_HELMET).color(color).make(), dropChance);

		if (!ignoredSlots.contains(CHEST))
			CHEST.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_CHESTPLATE).color(color).make(), dropChance);

		if (!ignoredSlots.contains(LEGS))
			LEGS.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_LEGGINGS).color(color).make(), dropChance);

		if (!ignoredSlots.contains(FEET))
			FEET.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_BOOTS).color(color).make(), dropChance);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 * with 0 drop chance
	 *
	 * @param entity
	 * @param type
	 */
	public static void applyArmor(final LivingEntity entity, final Type type) {
		applyArmor(entity, type, 0d, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 * with 0 drop chance
	 *
	 * @param entity
	 * @param type
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, final Type type, final Set<CompEquipmentSlot> ignoredSlots) {
		applyArmor(entity, type, 0d, ignoredSlots);
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 *
	 * @param entity
	 * @param type
	 * @param dropChance
	 */
	public static void applyArmor(final LivingEntity entity, final Type type, final double dropChance) {
		applyArmor(entity, type, dropChance, new HashSet<>());
	}

	/**
	 * A convenience shortcut to quickly give the entity a full armor of the given type
	 *
	 * @param entity
	 * @param type
	 * @param dropChance
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, Type type, final Double dropChance, final Set<CompEquipmentSlot> ignoredSlots) {

		// Compatibility
		if (type == Type.NETHERITE && MinecraftVersion.olderThan(V.v1_16))
			type = Type.DIAMOND;

		final String name = type == Type.GOLD ? "GOLDEN" : type.toString();

		if (!ignoredSlots.contains(HEAD))
			HEAD.applyTo(entity, CompMaterial.valueOf(name + "_HELMET").toItem(), dropChance);

		if (!ignoredSlots.contains(CHEST))
			CHEST.applyTo(entity, CompMaterial.valueOf(name + "_CHESTPLATE").toItem(), dropChance);

		if (!ignoredSlots.contains(LEGS))
			LEGS.applyTo(entity, CompMaterial.valueOf(name + "_LEGGINGS").toItem(), dropChance);

		if (!ignoredSlots.contains(FEET))
			FEET.applyTo(entity, CompMaterial.valueOf(name + "_BOOTS").toItem(), dropChance);
	}

	@Override
	public String toString() {
		return this.key.toUpperCase();
	}

	/**
	 * Denotes the main armor material type such as Leather or Diamond
	 *
	 */
	public static enum Type {
		LEATHER,
		CHAINMAIL,
		IRON,
		GOLD,
		DIAMOND,
		NETHERITE;

		/**
		 * Attempts to parse armor material (any helmet, chestplate, leggings or boots)
		 * to a type based on its type (i.e. iron_helmet -> iron)
		 *
		 * @param armorMaterial
		 * @return
		 */
		public static Type fromArmor(final CompMaterial armorMaterial) {
			final String n = armorMaterial.name();

			ValidCore.checkBoolean(n.contains("LEATHER") || n.contains("CHAINMAIL") || n.contains("IRON") || n.contains("GOLD") || n.contains("DIAMOND") || n.contains("NETHERITE"),
					"Only leather to netherite armors are supported, not: " + armorMaterial);

			return Type.valueOf(n.split("_")[0]);
		}
	}
}
