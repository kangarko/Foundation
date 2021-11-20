package org.mineacademy.fo.menu.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.SimpleEnchantment;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * ItemCreator allows you to create highly customized {@link ItemStack}
 * easily, simply call the static "of" methods, customize your item and then
 * call {@link #make()} to turn it into a Bukkit ItemStack.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemCreator {

	/**
	 * The {@link ItemStack}, if any, to start building with. Either this, or {@link #material} must be set.
	 */
	@Nullable
	private ItemStack item;

	/**
	 * The item meta, if any, to start building with. Parameters above
	 * will override this.
	 */
	@Nullable
	private ItemMeta meta;

	/**
	 * The {@link CompMaterial}, if any, to start building with. Either this, or {@link #item} must be set.
	 */
	@Nullable
	private CompMaterial material;

	/**
	 * The amount of the item.
	 */
	private int amount = -1;

	/**
	 * The item damage.
	 */
	private int damage = -1;

	/**
	 * The item name (& color codes are replaced automatically).
	 */
	@Getter
	private String name;

	/**
	 * The lore for this item (& color codes are replaced automatically).
	 */
	private final List<String> lores = new ArrayList<>();

	/**
	 * The enchants applied to the item.
	 */
	private final Map<Enchantment, Integer> enchants = new HashMap<>();

	/**
	 * The {@link CompItemFlag}.
	 */
	private final List<CompItemFlag> flags = new ArrayList<>();

	/**
	 * Is the item unbreakable?
	 */
	private boolean unbreakable = false;

	/**
	 * The color in case your item is either of {@link LeatherArmorMeta},
	 * or from a selected list of compatible items such as stained glass, wool, etc.
	 */
	@Nullable
	private CompColor color;

	/**
	 * Should we hide all tags from the item (enchants, attributes, etc.)?
	 */
	private boolean hideTags = false;

	/**
	 * Should we add glow to the item? (adds a fake enchant and uses {@link ItemFlag}
	 * to hide it). The enchant is visible on older MC versions.
	 */
	private boolean glow = false;

	/**
	 * The skull owner, in case the item is a skull.
	 */
	@Nullable
	private String skullOwner;

	/**
	 * The list of custom hidden data injected to the item.
	 */
	private final Map<String, String> tags = new HashMap<>();

	/**
	 * If this is a book, you can set its new pages here.
	 */
	private final List<String> bookPages = new ArrayList<>();

	/**
	 * If this a book, you can set its author here.
	 */
	@Nullable
	private String bookAuthor;

	/**
	 * If this a book, you can set its title here.
	 */
	@Nullable
	private String bookTitle;

	// ----------------------------------------------------------------------------------------
	// Builder methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Set the ItemStack for this item. We will reapply all other properties
	 * on this ItemStack, make sure they are compatible (such as skullOwner requiring a skull ItemStack, etc.)
	 *
	 * @param item
	 * @return
	 */
	public ItemCreator item(ItemStack item) {
		this.item = item;

		return this;
	}

	/**
	 * Set the ItemMeta we use to start building. All other properties in this
	 * class will build on this meta and take priority.
	 *
	 * @param meta
	 * @return
	 */
	public ItemCreator meta(ItemMeta meta) {
		this.meta = meta;

		return this;
	}

	/**
	 * Set the Material for the item. If {@link #item} is set,
	 * this material will take priority.
	 *
	 * @param material
	 * @return
	 */
	public ItemCreator material(CompMaterial material) {
		this.material = material;

		return this;
	}

	/**
	 * Set the amount of ItemStack to create.
	 *
	 * @param amount
	 * @return
	 */
	public ItemCreator amount(int amount) {
		this.amount = amount;

		return this;
	}

	/**
	 * Set the damage to the ItemStack. Notice that this only
	 * works for certain items, such as tools.
	 *
	 * See {@link Damageable#setDamage(int)}
	 *
	 * @param damage
	 * @return
	 */
	public ItemCreator damage(int damage) {
		this.damage = damage;

		return this;
	}

	/**
	 * Set a custom name for the item (& color codes are replaced automatically).
	 *
	 * @param name
	 * @return
	 */
	public ItemCreator name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Remove any previous lore from the item. Useful if you initiated this
	 * class with an ItemStack or set {@link #item} already, to clear old lore off of it.
	 *
	 * @return
	 */
	public ItemCreator clearLore() {
		this.lores.clear();

		return this;
	}

	/**
	 * Append the given lore to the end of existing item lore.
	 *
	 * @param lore
	 * @return
	 */
	public ItemCreator lore(String... lore) {
		return this.lore(Arrays.asList(lore));
	}

	/**
	 * Append the given lore to the end of existing item lore.
	 *
	 * @param lore
	 * @return
	 */
	public ItemCreator lore(List<String> lore) {
		this.lores.addAll(lore);

		return this;
	}

	/**
	 * Add the given enchant to the item.
	 *
	 * @param enchantment
	 * @return
	 */
	public ItemCreator enchant(Enchantment enchantment) {
		return this.enchant(enchantment, 1);
	}

	/**
	 * Add the given enchant to the item.
	 *
	 * @param enchantment
	 * @param level
	 * @return
	 */
	public ItemCreator enchant(Enchantment enchantment, int level) {
		this.enchants.put(enchantment, level);

		return this;
	}

	/**
	 * Add the given flags to the item.
	 *
	 * @param flags
	 * @return
	 */
	public ItemCreator flags(CompItemFlag... flags) {
		this.flags.addAll(Arrays.asList(flags));

		return this;
	}

	/**
	 * Set the item to be unbreakable.
	 *
	 * @param unbreakable
	 * @return
	 */
	public ItemCreator unbreakable(boolean unbreakable) {
		this.unbreakable = unbreakable;

		return this;
	}

	/**
	 * Set the stained or dye color in case your item is either of {@link LeatherArmorMeta},
	 * or from a selected list of compatible items such as stained glass, wool, etc.
	 *
	 * @param color
	 * @return
	 */
	public ItemCreator color(CompColor color) {
		this.color = color;

		return this;
	}

	/**
	 * Removes all enchantment, attribute and other tags appended
	 * at the end of item lore, typically with blue color.
	 *
	 * @param hideTags
	 * @return
	 */
	public ItemCreator hideTags(boolean hideTags) {
		this.hideTags = hideTags;

		return this;
	}

	/**
	 * Makes this item glow. Ignored if enchantments exists. Call {@link #hideTags(boolean)}
	 * to hide enchantment lores instead.
	 *
	 * @param glow
	 * @return
	 */
	public ItemCreator glow(boolean glow) {
		this.glow = glow;

		return this;
	}

	/**
	 * Set the skull owner for this item, only works if the item is a skull.
	 *
	 * See {@link SkullCreator}
	 *
	 * @param skullOwner
	 * @return
	 */
	public ItemCreator skullOwner(String skullOwner) {
		this.skullOwner = skullOwner;

		return this;
	}

	/**
	 * Places an invisible custom tag to the item, for most server instances it
	 * will persist across saves/restarts (you should check just to be safe).
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public ItemCreator tag(String key, String value) {
		this.tags.put(key, value);

		return this;
	}

	/**
	 * If this is a book, set its pages.
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(String... pages) {
		return this.bookPages(Arrays.asList(pages));
	}

	/**
	 * If this is a book, set its pages.
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(List<String> pages) {
		this.bookPages.addAll(pages);

		return this;
	}

	/**
	 * If this is a book, set its author.
	 *
	 * @param bookAuthor
	 * @return
	 */
	public ItemCreator bookAuthor(String bookAuthor) {
		this.bookAuthor = bookAuthor;

		return this;
	}

	/**
	 * If this is a book, set its title.
	 *
	 * @param bookTitle
	 * @return
	 */
	public ItemCreator bookTitle(String bookTitle) {
		this.bookTitle = bookTitle;

		return this;
	}

	// ----------------------------------------------------------------------------------------
	// Convenience give methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly adding this item into a players inventory
	 *
	 * @param player
	 */
	public void give(final Player player) {
		player.getInventory().addItem(this.make());
	}

	// ----------------------------------------------------------------------------------------
	// Constructing items
	// ----------------------------------------------------------------------------------------

	/**
	 * Make an unbreakable item with all attributes hidden, suitable for menu use.
	 *
	 * @return the new menu tool with all attributes hidden
	 */
	public ItemStack makeMenuTool() {
		this.hideTags = true;

		return make();
	}

	/**
	 * Construct a valid {@link ItemStack} from all parameters of this class.
	 *
	 * @return the finished item
	 */
	public ItemStack make() {

		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		Valid.checkBoolean(this.material != null || this.item != null, "Material or item must be set!");

		ItemStack compiledItem = this.item != null ? this.item.clone() : this.material.toItem();

		Object compiledMeta = Remain.hasItemMeta() ? this.meta != null ? this.meta.clone() : compiledItem.getItemMeta() : null;

		// Override with given material
		if (this.material != null) {
			compiledItem.setType(this.material.getMaterial());

			if (MinecraftVersion.olderThan(V.v1_13))
				compiledItem.setData(new MaterialData(this.material.getMaterial(), this.material.getData()));
		}

		// Skip if air
		if (CompMaterial.isAir(compiledItem.getType()))
			return compiledItem;

		// Apply specific material color if possible
		color:
		if (this.color != null) {

			if (compiledItem.getType().toString().contains("LEATHER")) {
				if (MinecraftVersion.atLeast(V.v1_4)) {
					Valid.checkBoolean(compiledMeta instanceof LeatherArmorMeta, "Expected a leather item, cannot apply color to " + compiledItem);

					((LeatherArmorMeta) compiledMeta).setColor(this.color.getColor());
				}
			}

			else {

				// Hack: If you put WHITE_WOOL and a color, we automatically will change the material to the colorized version
				if (MinecraftVersion.atLeast(V.v1_13)) {
					final String dye = this.color.getDye().toString();
					final List<String> colorableMaterials = Arrays.asList("BANNER", "BED", "CARPET", "CONCRETE", "GLAZED_TERRACOTTA", "SHULKER_BOX", "STAINED_GLASS",
							"STAINED_GLASS_PANE", "TERRACOTTA", "WALL_BANNER", "WOOL");

					for (final String material : colorableMaterials) {
						final String suffix = "_" + material;

						if (compiledItem.getType().toString().endsWith(suffix)) {
							compiledItem.setType(Material.valueOf(dye + suffix));

							break color;
						}
					}
				}

				else {
					try {
						final byte dataValue = this.color.getDye().getWoolData();

						compiledItem.setData(new MaterialData(compiledItem.getType(), dataValue));
						compiledItem.setDurability(dataValue);

					} catch (final NoSuchMethodError err) {
						// Ancient MC, ignore
					}
				}
			}
		}

		// Fix monster eggs
		if (compiledItem.getType().toString().endsWith("SPAWN_EGG") || compiledItem.getType().toString().equals("MONSTER_EGG")) {

			EntityType entity = null;

			if (MinecraftVersion.olderThan(V.v1_13)) { // Try to find it if already exists
				CompMonsterEgg.acceptUnsafeEggs = true;
				final EntityType pre = CompMonsterEgg.getEntity(compiledItem);
				CompMonsterEgg.acceptUnsafeEggs = false;

				if (pre != null && pre != EntityType.UNKNOWN)
					entity = pre;
			}

			if (entity == null) {
				final String itemName = compiledItem.getType().toString();

				String entityRaw = itemName.replace("_SPAWN_EGG", "");

				if (entityRaw.equals("MONSTER_EGG") && this.material != null && this.material.toString().endsWith("SPAWN_EGG"))
					entityRaw = this.material.toString().replace("_SPAWN_EGG", "");

				if ("MOOSHROOM".equals(entityRaw))
					entityRaw = "MUSHROOM_COW";

				else if ("ZOMBIE_PIGMAN".equals(entityRaw))
					entityRaw = "PIG_ZOMBIE";

				try {
					entity = EntityType.valueOf(entityRaw);

				} catch (final Throwable t) {

					// Probably version incompatible
					Common.log("The following item could not be transformed into " + entityRaw + " egg, item: " + compiledItem);
				}
			}

			if (entity != null)
				compiledMeta = CompMonsterEgg.setEntity(compiledItem, entity).getItemMeta();
		}

		if (this.damage != -1) {

			try {
				ReflectionUtil.invoke("setDurability", compiledItem, (short) this.damage);
			} catch (final Throwable t) {
			}

			try {
				if (compiledMeta instanceof org.bukkit.inventory.meta.Damageable)
					((org.bukkit.inventory.meta.Damageable) compiledMeta).setDamage(this.damage);
			} catch (final Throwable t) {
			}
		}

		if (this.skullOwner != null && compiledMeta instanceof SkullMeta)
			((SkullMeta) compiledMeta).setOwner(this.skullOwner);

		if (compiledMeta instanceof BookMeta) {
			final BookMeta bookMeta = (BookMeta) compiledMeta;

			if (this.bookPages != null)
				bookMeta.setPages(Common.colorize(this.bookPages));

			if (this.bookAuthor == null)
				bookMeta.setAuthor(Common.getOrEmpty(this.bookAuthor));

			if (this.bookTitle == null)
				bookMeta.setTitle(Common.getOrEmpty(this.bookTitle));

			// Fix "Corrupted NBT tag" error when any of these fields are not set
			if (bookMeta.getPages() == null)
				bookMeta.setPages("");

			if (bookMeta.getAuthor() == null)
				bookMeta.setAuthor("Anonymous");

			if (bookMeta.getTitle() == null)
				bookMeta.setTitle("Book");
		}

		if (compiledMeta instanceof ItemMeta) {
			if (this.glow && this.enchants.isEmpty()) {
				((ItemMeta) compiledMeta).addEnchant(Enchantment.DURABILITY, 1, true);

				this.flags.add(CompItemFlag.HIDE_ENCHANTS);
			}

			for (final Map.Entry<Enchantment, Integer> entry : this.enchants.entrySet()) {
				final Enchantment enchant = entry.getKey();
				final int level = entry.getValue();

				if (compiledMeta instanceof EnchantmentStorageMeta)
					((EnchantmentStorageMeta) compiledMeta).addStoredEnchant(enchant, level, true);

				else
					((ItemMeta) compiledMeta).addEnchant(enchant, level, true);
			}

			if (this.name != null && !"".equals(this.name))
				((ItemMeta) compiledMeta).setDisplayName(Common.colorize("&r&f" + name));

			if (!this.lores.isEmpty()) {
				final List<String> coloredLores = new ArrayList<>();

				for (final String lore : this.lores)
					if (lore != null)
						for (final String subLore : lore.split("\n"))
							coloredLores.add(Common.colorize("&7" + subLore));

				((ItemMeta) compiledMeta).setLore(coloredLores);
			}
		}

		if (this.unbreakable) {
			this.flags.add(CompItemFlag.HIDE_ATTRIBUTES);
			this.flags.add(CompItemFlag.HIDE_UNBREAKABLE);

			CompProperty.UNBREAKABLE.apply(compiledMeta, true);
		}

		if (this.hideTags)
			for (final CompItemFlag f : CompItemFlag.values())
				if (!this.flags.contains(f))
					this.flags.add(f);

		for (final CompItemFlag flag : this.flags)
			try {
				((ItemMeta) compiledMeta).addItemFlags(ItemFlag.valueOf(flag.toString()));
			} catch (final Throwable t) {
			}

		// Override with custom amount if set
		if (this.amount != -1)
			compiledItem.setAmount(this.amount);

		// Apply Bukkit metadata
		if (compiledMeta instanceof ItemMeta)
			compiledItem.setItemMeta((ItemMeta) compiledMeta);

		//
		// From now on we have to re-set the item
		//

		// Apply custom enchantment lores
		compiledItem = Common.getOrDefault(SimpleEnchantment.addEnchantmentLores(compiledItem), compiledItem);

		// 1.7.10 hack to add glow, requires no enchants
		if (this.glow && MinecraftVersion.equals(V.v1_7) && (this.enchants == null || this.enchants.isEmpty())) {
			final NBTItem nbtItem = new NBTItem(compiledItem);

			nbtItem.removeKey("ench");
			nbtItem.addCompound("ench");

			compiledItem = nbtItem.getItem();
		}

		// Apply NBT tags
		if (MinecraftVersion.atLeast(V.v1_7))
			for (final Entry<String, String> entry : this.tags.entrySet())
				compiledItem = CompMetadata.setMetadata(compiledItem, entry.getKey(), entry.getValue());

		else if (!this.tags.isEmpty() && this.item != null)
			Common.log("Item had unsupported tags " + tags + " that are not supported on MC " + MinecraftVersion.getServerVersion() + " Item: " + compiledItem);

		return compiledItem;
	}

	// ----------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator of(final CompMaterial material, final String name, @NonNull final Collection<String> lore) {
		return of(material, name, Common.toArray(lore));
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return new item creator
	 */
	public static ItemCreator of(final CompMaterial material, final String name, @NonNull final String... lore) {
		return new ItemCreator().material(material).name(name).lore(lore).hideTags(true);
	}

	/**
	 * Convenience method to get a wool
	 *
	 * @param color the wool color
	 * @return the new item creator
	 */
	public static ItemCreator ofWool(final CompColor color) {
		return of(CompMaterial.makeWool(color, 1)).color(color);
	}

	/**
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @return
	 */
	public static ItemCreator ofEgg(final EntityType entityType) {
		return of(CompMonsterEgg.makeEgg(entityType));
	}

	/**
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofEgg(final EntityType entityType, String name, String... lore) {
		return of(CompMonsterEgg.makeEgg(entityType)).name(name).lore(lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect) {
		return ofPotion(potionEffect, 1);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param durationTicks
	 * @param level
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int durationTicks, int level) {
		return ofPotion(potionEffect, durationTicks, level, null);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param level
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int level) {
		return ofPotion(potionEffect, Integer.MAX_VALUE, level, null);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, String name, String... lore) {
		return ofPotion(potionEffect, Integer.MAX_VALUE, 1, name, lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param effect
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffect effect, String name, String... lore) {
		return ofPotion(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier() + 1, name, lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param potionEffect
	 * @param level
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int durationTicks, int level, String name, String... lore) {
		final ItemStack item = new ItemStack(CompMaterial.POTION.getMaterial());
		Remain.setPotion(item, potionEffect, durationTicks, level);

		final ItemCreator builder = of(item);

		if (name != null)
			builder.name(name);

		if (lore != null)
			builder.lore(lore);

		return builder;
	}

	/**
	 * Convenience method to get the creator of an existing itemstack
	 *
	 * @param item existing itemstack
	 * @return the new item creator
	 */
	public static ItemCreator of(final ItemStack item) {
		final ItemCreator builder = new ItemCreator();
		final ItemMeta meta = item.getItemMeta();

		if (meta != null && meta.getLore() != null)
			builder.lore(meta.getLore());

		return builder.item(item);
	}

	/**
	 * Get a new item creator from material
	 *
	 * @param mat existing material
	 * @return the new item creator
	 */
	public static ItemCreator of(final CompMaterial mat) {
		Valid.checkNotNull(mat, "Material cannot be null!");

		return new ItemCreator().material(mat);
	}
}