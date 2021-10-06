package org.mineacademy.fo.menu.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.Button.DummyButton;
import org.mineacademy.fo.model.SimpleEnchant;
import org.mineacademy.fo.model.SimpleEnchantment;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * Our core class for easy and comfortable item creation.
 * <p>
 * You can use this to make named items with incredible speed and quality.
 */
final @Builder public class ItemCreator {

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
	 * <p>
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
	private final Map<String, String> tags;

	/**
	 * If this is a book, you can set its new pages here
	 */
	@Singular
	private final List<String> bookPages;

	/**
	 * If this a book, you can set its author here
	 */
	private final String bookAuthor;

	/**
	 * If this a book, you can set its title here
	 */
	private final String bookTitle;

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
		player.getInventory().addItem(this.make());
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
	 * Construct a valid {@link ItemStack} from all parameters above.
	 *
	 * @return the finished item
	 */
	public ItemStack make() {
		//
		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		//
		Valid.checkBoolean(material != null || item != null, "Material or item must be set!");

		if (material != null)
			Valid.checkNotNull(material.getMaterial(), "Material#getMaterial cannot be null for " + material);

		final ItemStack compiledItem = item != null ? item.clone() : material.toItem(amount);
		ItemMeta compiledMeta = meta != null ? meta.clone() : compiledItem.getItemMeta();

		// Skip if air
		if (CompMaterial.isAir(compiledItem.getType()))
			return compiledItem;

		// Override with given material
		if (material != null) {
			compiledItem.setType(material.getMaterial());

			if (MinecraftVersion.olderThan(V.v1_13))
				compiledItem.setData(new MaterialData(material.getMaterial(), material.getData()));
		}

		// Apply specific material color if possible
		color:
		if (color != null) {

			if (compiledItem.getType().toString().contains("LEATHER")) {
				if (MinecraftVersion.atLeast(V.v1_4)) {
					Valid.checkBoolean(compiledMeta instanceof LeatherArmorMeta, "Expected a leather item, cannot apply color to " + compiledItem);

					((LeatherArmorMeta) compiledMeta).setColor(color.getColor());
				}
			}

			else {

				// Hack: If you put WHITE_WOOL and a color, we automatically will change the material to the colorized version
				if (MinecraftVersion.atLeast(V.v1_13)) {
					final String dye = color.getDye().toString();
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
					final byte dataValue = color.getDye().getWoolData();

					compiledItem.setData(new MaterialData(compiledItem.getType(), dataValue));
					compiledItem.setDurability(dataValue);
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

				if (entityRaw.equals("MONSTER_EGG") && material != null && material.toString().endsWith("SPAWN_EGG"))
					entityRaw = material.toString().replace("_SPAWN_EGG", "");

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

		flags = new ArrayList<>(Common.getOrDefault(flags, new ArrayList<>()));

		if (damage != -1) {

			try {
				ReflectionUtil.invoke("setDurability", compiledItem, (short) damage);
			} catch (final Throwable t) {
			}

			try {
				if (compiledMeta instanceof org.bukkit.inventory.meta.Damageable)
					((org.bukkit.inventory.meta.Damageable) compiledMeta).setDamage(damage);
			} catch (final Throwable t) {
			}
		}

		if (color != null && compiledItem.getType().toString().contains("LEATHER"))
			((LeatherArmorMeta) compiledMeta).setColor(color.getColor());

		if (skullOwner != null && compiledMeta instanceof SkullMeta)
			((SkullMeta) compiledMeta).setOwner(skullOwner);

		if (compiledMeta instanceof BookMeta) {
			final BookMeta bookMeta = (BookMeta) compiledMeta;

			if (bookPages != null)
				bookMeta.setPages(Common.colorize(bookPages));

			if (bookMeta.getAuthor() == null)
				bookMeta.setAuthor(Common.getOrEmpty(bookAuthor));

			if (bookMeta.getTitle() == null)
				bookMeta.setTitle(Common.getOrEmpty(bookTitle));
		}

		if (glow) {
			compiledMeta.addEnchant(Enchantment.DURABILITY, 1, true);

			flags.add(CompItemFlag.HIDE_ENCHANTS);
		}

		if (enchants != null)
			for (final SimpleEnchant ench : enchants)
				if (compiledMeta instanceof EnchantmentStorageMeta)
					((EnchantmentStorageMeta) compiledMeta).addStoredEnchant(ench.getEnchant(), ench.getLevel(), true);
				else
					compiledMeta.addEnchant(ench.getEnchant(), ench.getLevel(), true);

		if (name != null && !"".equals(name))
			compiledMeta.setDisplayName(Common.colorize("&r&f" + name));

		if (lores != null && !lores.isEmpty()) {
			final List<String> coloredLores = new ArrayList<>();

			for (final String lore : lores)
				coloredLores.add(Common.colorize("&7" + lore));

			compiledMeta.setLore(coloredLores);
		}

		if (unbreakable != null) {
			flags.add(CompItemFlag.HIDE_ATTRIBUTES);
			flags.add(CompItemFlag.HIDE_UNBREAKABLE);

			CompProperty.UNBREAKABLE.apply(compiledMeta, true);
		}

		if (hideTags)
			for (final CompItemFlag f : CompItemFlag.values())
				if (!flags.contains(f))
					flags.add(f);

		for (final CompItemFlag flag : flags)
			try {
				compiledMeta.addItemFlags(ItemFlag.valueOf(flag.toString()));
			} catch (final Throwable t) {
			}

		// Apply Bukkit metadata
		compiledItem.setItemMeta(compiledMeta);

		//
		// From now on we have to re-set the item
		//

		// Apply custom enchantment lores
		ItemStack finalItem = compiledItem;
		final ItemStack enchantedIs = SimpleEnchantment.addEnchantmentLores(compiledItem);

		if (enchantedIs != null)
			finalItem = enchantedIs;

		// 1.7.10 hack to add glow, requires no enchants
		if (glow && MinecraftVersion.equals(V.v1_7) && (enchants == null || enchants.isEmpty())) {
			final NBTItem nbtItem = new NBTItem(compiledItem);

			nbtItem.removeKey("ench");
			nbtItem.addCompound("ench");

			finalItem = nbtItem.getItem();
		}

		// Apply NBT tags
		if (tags != null)
			if (MinecraftVersion.atLeast(V.v1_7))
				for (final Entry<String, String> entry : tags.entrySet())
					finalItem = CompMetadata.setMetadata(finalItem, entry.getKey(), entry.getValue());

			else if (!tags.isEmpty() && item != null)
				Common.log("Item had unsupported tags " + tags + " that are not supported on MC " + MinecraftVersion.getServerVersion() + " Item: " + finalItem);

		return finalItem;
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
	public static ItemCreatorBuilder of(final CompMaterial material, final String name, @NonNull final Collection<String> lore) {
		return of(material, name, lore.toArray(new String[0]));
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreatorBuilder of(final String material, final String name, @NonNull final Collection<String> lore) {
		return of(CompMaterial.valueOf(material), name, lore.toArray(new String[0]));
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
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return new item creator
	 */
	public static ItemCreatorBuilder of(final String material, final String name, @NonNull final String... lore) {
		return ItemCreator.builder().material(CompMaterial.valueOf(material)).name("&r" + name).lores(Arrays.asList(lore)).hideTags(true);
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
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreatorBuilder ofEgg(final EntityType entityType, String name, String... lore) {
		return of(CompMonsterEgg.makeEgg(entityType)).name(name).lores(Arrays.asList(lore));
	}

	/**
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @return
	 */
	public static ItemCreatorBuilder ofEgg(final EntityType entityType) {
		return of(CompMonsterEgg.makeEgg(entityType));
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
	 * @param mat existing material
	 * @return the new item creator
	 */
	public static ItemCreatorBuilder of(final CompMaterial mat) {
		Valid.checkNotNull(mat, "Material cannot be null!");

		return ItemCreator.builder().material(mat);
	}
}