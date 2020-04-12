package org.mineacademy.fo.menu.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.Button.DummyButton;
import org.mineacademy.fo.model.SimpleEnchant;
import org.mineacademy.fo.model.SimpleEnchantment;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.CompProperty;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * Our core class for easy and comfortable item creation.
 *
 * You can use this to make named items with incredible speed and quality.
 */
@Builder
public final class ItemCreator {

	/**
	 * The initial item stack
	 */
	private final ItemStack item;

	/**
	 * The initial material
	 */
	private final CompMaterial material;

	/**
	 * The amount of the item
	 */
	@Builder.Default
	private final int amount = 1;

	/**
	 * The item damage
	 */
	@Builder.Default
	private final int damage = -1;

	/**
	 * The item name, colors are replaced
	 */
	private final String name;

	/**
	 * The lore for this item, colors are replaced
	 */
	@Singular
	private final List<String> lores;

	/**
	 * The enchants applied for the item
	 */
	@Singular
	private final List<SimpleEnchant> enchants;

	/**
	 * The item flags
	 */
	@Singular
	private List<CompItemFlag> flags;

	/**
	 * Is the item unbreakable?
	 */
	private Boolean unbreakable;

	/**
	 * The dye color in case your item is compatible
	 */
	private final CompColor color;

	/**
	 * Should we hide all tags from the item (enchants, etc.)?
	 */
	@Builder.Default
	private boolean hideTags = false;

	/**
	 * Should we add glow to the item? (adds a fake enchant and uses
	 * {@link ItemFlag} to hide it)
	 *
	 * The enchant is visible on older MC versions.
	 */
	private final boolean glow;

	/**
	 * The skull owner, in case it applies
	 */
	private final String skullOwner;

	/**
	 * The list of NBT tags with their key-value pairs
	 */
	@Singular
	private final List<Tuple<String, String>> tags;

	/**
	 * The item meta, overriden by other fields
	 */
	private final ItemMeta meta;

	// ----------------------------------------------------------------------------------------
	// Convenience give methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly adding this item into a players inventory
	 *
	 * @param player
	 */
	public void give(final Player player) {
		player.getInventory().addItem(makeSurvival());
	}

	// ----------------------------------------------------------------------------------------
	// Constructing items
	// ----------------------------------------------------------------------------------------

	/**
	 * Constructs a new {@link DummyButton} from this item
	 *
	 * @return a new dummy button
	 */
	public DummyButton makeButton() {
		return Button.makeDummy(this);
	}

	/**
	 * Make an unbreakable item with all attributes hidden, suitable for menu use.
	 *
	 * @return the new menu tool, unbreakable with all attributes hidden
	 */
	public ItemStack makeMenuTool() {
		unbreakable = true;
		hideTags = true;

		return make();
	}

	/**
	 * Make an item suitable for survival where we remove the "hideFlag"
	 * that is automatically put in {@link ItemCreator#of(CompMaterial, String, String...)}
	 * to hide enchants, attributes etc.
	 *
	 * @return
	 */
	public ItemStack makeSurvival() {
		hideTags = false;

		return make();
	}

	/**
	 * Attempts to remove all enchants, used to remove glow
	 */
	public ItemCreator removeEnchants() {
		if (item != null)
			for (final Enchantment enchant : item.getEnchantments().keySet())
				item.removeEnchantment(enchant);

		return this;
	}

	/**
	 * Construct a valid {@link ItemStack} from all parameters above.
	 *
	 * @return the finished item
	 */
	public ItemStack make() {
		//
		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		//

		Valid.checkBoolean(material != null || item != null, "Material or item must be set!");
		ItemStack is = item != null ? item.clone() : new ItemStack(material.getMaterial(), amount);
		final ItemMeta itemMeta = meta != null ? meta.clone() : is.getItemMeta();

		// Skip if air
		if (CompMaterial.isAir(is.getType()))
			return is;

		// Override with given material
		if (this.material != null)
			is.setType(this.material.getMaterial());

		if (MinecraftVersion.atLeast(V.v1_13)) {

			// Apply specific material color if possible
			color: if (color != null && !is.getType().toString().contains("LEATHER")) {
				final String dye = color.getDye().toString();
				final List<String> colorableMaterials = Arrays.asList(
						"BANNER", "BED", "CARPET", "CONCRETE", "GLAZED_TERRACOTTA", "SHULKER_BOX",
						"STAINED_GLASS", "STAINED_GLASS_PANE", "TERRACOTTA", "WALL_BANNER", "WOOL");

				for (final String colorable : colorableMaterials) {
					final String suffix = "_" + colorable;

					if (is.getType().toString().endsWith(suffix)) {
						is.setType(Material.valueOf(dye + suffix));

						break color;
					}
				}

				// If not revert to wool
				is.setType(Material.valueOf(dye + "_WOOL"));
			}
		}

		// If using legacy MC version
		else {
			int dataValue = material != null ? material.getData() : is.getData().getData();

			if (!is.getType().toString().contains("LEATHER") && color != null)
				dataValue = color.getDye().getWoolData();

			if (MinecraftVersion.newerThan(V.v1_8) && CompMaterial.isMonsterEgg(is.getType()))
				dataValue = 0;

			is.setData(new MaterialData(is.getType(), (byte) dataValue));

			if (MinecraftVersion.olderThan(V.v1_13))
				is.setDurability((short) dataValue);

		}

		// Fix monster eggs
		if (is.getType().toString().endsWith("SPAWN_EGG")) {

			EntityType entity = null;

			if (MinecraftVersion.olderThan(V.v1_13)) { // Try to find it if already exists
				CompMonsterEgg.acceptUnsafeEggs = true;
				final EntityType pre = CompMonsterEgg.getEntity(is);
				CompMonsterEgg.acceptUnsafeEggs = false;

				if (pre != null && pre != EntityType.UNKNOWN)
					entity = pre;
			}

			if (entity == null) {
				final String itemName = is.getType().toString();

				String entityRaw = itemName.replace("_SPAWN_EGG", "");

				if ("MOOSHROOM".equals(entityRaw))
					entityRaw = "MUSHROOM_COW";

				else if ("ZOMBIE_PIGMAN".equals(entityRaw))
					entityRaw = "PIG_ZOMBIE";

				try {
					entity = EntityType.valueOf(entityRaw);

				} catch (final Throwable t) {

					// Probably version incompatible
					Common.log("The following item could not be transformed into " + entityRaw + " egg, item: " + is);
				}
			}

			if (entity != null)
				is = CompMonsterEgg.setEntity(is, entity);
		}

		flags = new ArrayList<>(Common.getOrDefault(flags, new ArrayList<>()));

		if (damage != -1) {
			try {
				is.setDurability((short) damage);
			} catch (final Throwable t) {
			}

			try {
				if (itemMeta instanceof Damageable)
					((Damageable) itemMeta).setDamage(damage);
			} catch (final Throwable t) {
			}
		}

		if (color != null && is.getType().toString().contains("LEATHER"))
			((LeatherArmorMeta) itemMeta).setColor(color.getDye().getColor());

		if (skullOwner != null && itemMeta instanceof SkullMeta)
			((SkullMeta) itemMeta).setOwner(skullOwner);

		if (glow) {
			itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);

			flags.add(CompItemFlag.HIDE_ENCHANTS);
		}

		if (enchants != null)
			for (final SimpleEnchant ench : enchants)
				if (itemMeta instanceof EnchantmentStorageMeta)
					((EnchantmentStorageMeta) itemMeta).addStoredEnchant(ench.getEnchant(), ench.getLevel(), true);
				else
					itemMeta.addEnchant(ench.getEnchant(), ench.getLevel(), true);

		if (name != null && !"".equals(name))
			itemMeta.setDisplayName(Common.colorize("&r" + name));

		if (lores != null && !lores.isEmpty()) {
			final List<String> coloredLores = new ArrayList<>();

			lores.forEach((line) -> coloredLores.add(Common.colorize("&7" + line)));
			itemMeta.setLore(coloredLores);
		}

		if (unbreakable != null) {
			flags.add(CompItemFlag.HIDE_ATTRIBUTES);
			flags.add(CompItemFlag.HIDE_UNBREAKABLE);

			if (MinecraftVersion.olderThan(V.v1_12))
				try {
					final Object spigot = itemMeta.getClass().getMethod("spigot").invoke(itemMeta);

					spigot.getClass().getMethod("setUnbreakable", boolean.class).invoke(spigot, true);

				} catch (final Throwable t) {
					// Probably 1.7.10, tough luck
				}
			else
				CompProperty.UNBREAKABLE.apply(itemMeta, true);
		}

		if (hideTags)
			for (final CompItemFlag f : CompItemFlag.values())
				if (!flags.contains(f))
					flags.add(f);

		for (final CompItemFlag flag : flags)
			try {
				itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flag.toString()));
			} catch (final Throwable t) {
			}

		// Apply Bukkit metadata
		is.setItemMeta(itemMeta);

		//
		// From now on we have to re-set the item
		//

		// Apply custom enchantment lores
		final ItemStack enchantedIs = SimpleEnchantment.addEnchantmentLores(is);

		if (enchantedIs != null)
			is = enchantedIs;

		// Apply NBT tags
		if (tags != null)
			for (final Tuple<String, String> tag : tags)
				is = CompMetadata.setMetadata(is, tag.getKey(), tag.getValue());

		return is;
	}

	// ----------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------

	/**
	 * Creates an ItemBuilder from a skull-texture
	 *
	 * @param hash Base64-String representation of a skull-texture
	 *
	 * @return
	 */
	public static ItemCreatorBuilder ofSkullHash(final String hash) {
		Valid.checkNotNull(hash, "Hash mustn't be null");

		final ItemStack head = new ItemStack(CompMaterial.PLAYER_HEAD.getMaterial(), 1, (short) 3);
		final SkullMeta meta = (SkullMeta) head.getItemMeta();
		final GameProfile profile = new GameProfile(UUID.randomUUID(), "");

		profile.getProperties().put("textures", new Property("textures", hash));
		Field profileField = null;

		try {
			profileField = meta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(meta, profile);

		} catch (final IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			Common.throwError(e);
		}

		head.setItemMeta(meta);

		return of(head);
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreatorBuilder of(final CompMaterial material, final String name, @NonNull final Collection<String> lore) {
		return of(material, name, lore.toArray(new String[lore.size()]));
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return new item creator
	 */
	public static ItemCreatorBuilder of(final CompMaterial material, final String name, @NonNull final String... lore) {
		return ItemCreator.builder().material(material).name("&r" + name).lores(Arrays.asList(lore)).hideTags(true);
	}

	/**
	 * Convenience method to get a wool
	 *
	 * @param color the wool color
	 * @return the new item creator
	 */
	public static ItemCreatorBuilder ofWool(final CompColor color) {
		return of(CompMaterial.makeWool(color, 1)).color(color);
	}

	/**
	 * Convenience method to get the creator of an existing itemstack
	 *
	 * @param item existing itemstack
	 * @return the new item creator
	 */
	public static ItemCreatorBuilder of(final ItemStack item) {
		final ItemCreatorBuilder builder = ItemCreator.builder();
		final ItemMeta meta = item.getItemMeta();

		if (meta != null && meta.getLore() != null)
			builder.lores(meta.getLore());

		return builder.item(item);
	}

	/**
	 * Get a new item creator from material
	 *
	 * @param material existing material
	 * @return the new item creator
	 */
	public static ItemCreatorBuilder of(final CompMaterial mat) {
		Valid.checkNotNull(mat, "Material cannot be null!");

		return ItemCreator.builder().material(mat);
	}
}