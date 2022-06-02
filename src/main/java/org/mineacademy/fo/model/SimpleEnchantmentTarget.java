package org.mineacademy.fo.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.remain.CompMaterial;

public enum SimpleEnchantmentTarget {

	/**
	 * Allows the Enchantment to be placed on armor
	 */
	ARMOR {
		@Override
		public boolean includes(Material item) {
			return ARMOR_FEET.includes(item)
					|| ARMOR_LEGS.includes(item)
					|| ARMOR_HEAD.includes(item)
					|| ARMOR_TORSO.includes(item);
		}
	},

	/**
	 * Allows the Enchantment to be placed on feet slot armor
	 */
	ARMOR_FEET {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_BOOTS)
					|| item.equals(Material.CHAINMAIL_BOOTS)
					|| item.equals(Material.IRON_BOOTS)
					|| item.equals(Material.DIAMOND_BOOTS)
					|| item.equals(CompMaterial.GOLDEN_BOOTS.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_BOOTS.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on leg slot armor
	 */
	ARMOR_LEGS {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_LEGGINGS)
					|| item.equals(Material.CHAINMAIL_LEGGINGS)
					|| item.equals(Material.IRON_LEGGINGS)
					|| item.equals(Material.DIAMOND_LEGGINGS)
					|| item.equals(CompMaterial.GOLDEN_LEGGINGS.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_LEGGINGS.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on torso slot armor
	 */
	ARMOR_TORSO {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_CHESTPLATE)
					|| item.equals(Material.CHAINMAIL_CHESTPLATE)
					|| item.equals(Material.IRON_CHESTPLATE)
					|| item.equals(Material.DIAMOND_CHESTPLATE)
					|| item.equals(CompMaterial.GOLDEN_CHESTPLATE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_CHESTPLATE.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on head slot armor
	 */
	ARMOR_HEAD {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_HELMET)
					|| item.equals(Material.CHAINMAIL_HELMET)
					|| item.equals(Material.DIAMOND_HELMET)
					|| item.equals(Material.IRON_HELMET)
					|| item.equals(CompMaterial.GOLDEN_HELMET.getMaterial())
					|| item.equals(CompMaterial.TURTLE_HELMET.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_HELMET.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on weapons (swords)
	 */
	WEAPON {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_SWORD.getMaterial())
					|| item.equals(Material.STONE_SWORD)
					|| item.equals(Material.IRON_SWORD)
					|| item.equals(Material.DIAMOND_SWORD)
					|| item.equals(CompMaterial.GOLDEN_SWORD.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_SWORD.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on tools (spades, pickaxe, axes)
	 */
	TOOL {
		@Override
		public boolean includes(Material item) {
			return SHOVEL.includes(item)
					|| PICKAXE.includes(item)
					|| AXE.includes(item)
					|| HOE.includes(item);
		}
	},

	/**
	 * Allows the Enchantment to be placed on Shovel tools.
	 */
	SHOVEL {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.STONE_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.IRON_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.DIAMOND_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.GOLDEN_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_SHOVEL.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on Pickaxe tools.
	 */
	PICKAXE {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_PICKAXE.getMaterial())
					|| item.equals(Material.STONE_PICKAXE)
					|| item.equals(Material.IRON_PICKAXE)
					|| item.equals(Material.DIAMOND_PICKAXE)
					|| item.equals(CompMaterial.GOLDEN_PICKAXE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_PICKAXE.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on Hoe tools.
	 */
	AXE {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_AXE.getMaterial())
					|| item.equals(Material.STONE_AXE)
					|| item.equals(Material.IRON_AXE)
					|| item.equals(Material.DIAMOND_AXE)
					|| item.equals(CompMaterial.GOLDEN_AXE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_AXE.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on Hoe tools.
	 */
	HOE {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_HOE.getMaterial())
					|| item.equals(Material.STONE_HOE)
					|| item.equals(Material.IRON_HOE)
					|| item.equals(Material.DIAMOND_HOE)
					|| item.equals(CompMaterial.GOLDEN_HOE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_HOE.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on bows.
	 */
	BOW {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.BOW);
		}
	},

	/**
	 * Allows the Enchantment to be placed on fishing rods.
	 */
	FISHING_ROD {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.FISHING_ROD);
		}
	},

	/**
	 * Allows the enchantment to be placed on items with durability.
	 */
	BREAKABLE {
		@Override
		public boolean includes(Material item) {
			return item.getMaxDurability() > 0 && item.getMaxStackSize() == 1;
		}
	},

	/**
	 * Allows the enchantment to be placed on wearable items.
	 */
	WEARABLE {
		@Override
		public boolean includes(Material item) {
			return ARMOR.includes(item)
					|| ELYTRA.includes(item)
					|| item.equals(CompMaterial.CARVED_PUMPKIN.getMaterial())
					|| item.equals(CompMaterial.JACK_O_LANTERN.getMaterial())
					|| item.equals(CompMaterial.SKELETON_SKULL.getMaterial())
					|| item.equals(CompMaterial.WITHER_SKELETON_SKULL.getMaterial())
					|| item.equals(CompMaterial.ZOMBIE_HEAD.getMaterial())
					|| item.equals(CompMaterial.PLAYER_HEAD.getMaterial())
					|| item.equals(CompMaterial.CREEPER_HEAD.getMaterial())
					|| item.equals(CompMaterial.DRAGON_HEAD.getMaterial());
		}
	},

	/**
	 * Allows the Enchantment to be placed on Elytra's.
	 */
	ELYTRA {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.ELYTRA.getMaterial());
		}
	},

	/**
	 * Allow the Enchantment to be placed on tridents.
	 */
	TRIDENT {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.TRIDENT.getMaterial());
		}
	},

	/**
	 * Allow the Enchantment to be placed on crossbows.
	 */
	CROSSBOW {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.CROSSBOW.getMaterial());
		}
	},

	/**
	 * Allow the Enchantment to be placed on vanishing items.
	 */
	VANISHABLE {
		@Override
		public boolean includes(Material item) {
			return BREAKABLE.includes(item) || (WEARABLE.includes(item) && !item.equals(CompMaterial.ELYTRA.getMaterial())) || item.equals(Material.COMPASS);
		}
	};

	public abstract boolean includes(Material item);

	public boolean includes(ItemStack item) {
		return this.includes(item.getType());
	}
}
