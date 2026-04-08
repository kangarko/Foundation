package org.mineacademy.fo.menu.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;


import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.enchant.SimpleEnchantment;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.SimpleBook;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompEnchantment;
import org.mineacademy.fo.remain.CompEntityType;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.CompMonsterEgg;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTItem;

import com.google.common.collect.MultimapBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * ItemCreator allows you to create highly customized {@link ItemStack}
 * easily, simply call the static "of" methods, customize your item and then
 * call {@link #make()} to turn it into a Bukkit ItemStack.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemCreator {

	/**
	 * The lore prefix automatically inserted before each lore.
	 * Defaults to "&7" to make lores gray instead of pink italics like the vanilla.
	 */
	@Setter
	@Getter
	private static String lorePrefix = "&7";

	/**
	 * The {@link ItemStack}, if any, to start building with. Either this, or {@link #material} must be set.
	 */
	private ItemStack item;

	/**
	 * The item meta, if any, to start building with. Parameters above
	 * will override this.
	 */
	private ItemMeta meta;

	/**
	 * The {@link CompMaterial}, if any, to start building with. Either this, or {@link #item} must be set.
	 */
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
	private CompColor color;

	/**
	 * Should we hide all tags from the item (enchants, attributes, etc.)?
	 */
	private boolean hideTags = false;

	/**
	 * The custom model data of the item
	 */
	private Integer modelData;

	/**
	 * Should we add glow to the item? (adds a fake enchant and uses {@link ItemFlag}
	 * to hide it). The enchant is visible on older MC versions.
	 */
	private boolean glow = false;

	/**
	 * The skull owner, in case the item is a skull.
	 */
	private String skullOwner;

	/**
	 * The skull encoded as base64, in case the item is a skull.
	 */
	private String skullBase64;

	/**
	 * The skull UUID, in case the item is a skull.
	 */
	private UUID skullUid;

	/**
	 * The skull URL, in case the item is a skull.
	 */
	private String skullUrl;

	/**
	 * The list of custom hidden data injected to the item.
	 */
	private final Map<String, String> tags = new LinkedHashMap<>();

	/**
	 * If this is a book, you can set its new pages here.
	 */
	private List<String> bookPages = null;

	/**
	 * Replace mini and legacy color codes in books?
	 */
	private boolean colorizeBook = true;

	/**
	 * If this a book, you can set its author here.
	 */
	private String bookAuthor;

	/**
	 * If this a book, you can set its title here.
	 */
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
	public ItemCreator item(final ItemStack item) {
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
	public ItemCreator meta(final ItemMeta meta) {
		this.meta = meta;

		return this;
	}

	/**
	 * Set the Material for the item. If an itemstack has already been set,
	 * this material will take priority.
	 *
	 * @param material
	 * @return
	 */
	public ItemCreator material(final CompMaterial material) {
		this.material = material;

		return this;
	}

	/**
	 * Set the amount of ItemStack to create.
	 *
	 * @param amount
	 * @return
	 */
	public ItemCreator amount(final int amount) {
		this.amount = amount;

		return this;
	}

	/**
	 * Set the damage to the ItemStack. Notice that this only
	 * works for certain items, such as tools.
	 *
	 * See Damageable#setDamage(int)
	 *
	 * @param damage
	 * @return
	 */
	public ItemCreator damage(final int damage) {
		this.damage = damage;

		return this;
	}

	/**
	 * Set a custom name for the item (& color codes are replaced automatically).
	 *
	 * @param name
	 * @return
	 */
	public ItemCreator name(final String name) {
		this.name = name;

		return this;
	}

	/**
	 * Remove any previous lore from the item. Useful if you initiated this
	 * class with an ItemStack or set the itemstack already, to clear old lore off of it.
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
	public ItemCreator lore(final String... lore) {
		for (final String line : lore)
			if (line != null)
				for (final String subpart : line.split("\n"))
					this.lores.add(subpart);

		return this;
	}

	/**
	 * Append the given lore to the end of existing item lore.
	 *
	 * @param lore
	 * @return
	 */
	public ItemCreator lore(final List<String> lore) {
		for (final String line : lore)
			if (line != null)
				for (final String subpart : line.split("\n"))
					this.lores.add(subpart);

		return this;
	}

	/**
	 * Add the given enchant to the item.
	 *
	 * @param enchantment
	 * @return
	 */
	public ItemCreator enchant(final SimpleEnchantment enchantment) {
		return this.enchant(enchantment.toBukkit(), 1);
	}

	/**
	 * Add the given enchant to the item.
	 *
	 * @param enchantment
	 * @return
	 */
	public ItemCreator enchant(final Enchantment enchantment) {
		return this.enchant(enchantment, 1);
	}

	/**
	 * Add the given enchant to the item.
	 *
	 * @param enchantment
	 * @param level
	 * @return
	 */
	public ItemCreator enchant(final SimpleEnchantment enchantment, final int level) {
		this.enchants.put(enchantment.toBukkit(), level);

		return this;
	}

	/**
	 * Add the given enchant to the item. Use {@link CompEnchantment} for
	 * familiar names.
	 *
	 * @param enchantment
	 * @param level
	 * @return
	 */
	public ItemCreator enchant(final Enchantment enchantment, final int level) {
		this.enchants.put(enchantment, level);

		return this;
	}

	/**
	 * Add the given flags to the item.
	 *
	 * @param flags
	 * @return
	 */
	public ItemCreator flags(final CompItemFlag... flags) {
		this.flags.addAll(Arrays.asList(flags));

		return this;
	}

	/**
	 * Set the item to be unbreakable.
	 *
	 * @param unbreakable
	 * @return
	 */
	public ItemCreator unbreakable(final boolean unbreakable) {
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
	public ItemCreator color(final CompColor color) {
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
	public ItemCreator hideTags(final boolean hideTags) {
		this.hideTags = hideTags;

		return this;
	}

	/**
	 * Set the Custom Model Data of this item, compatible with MC 1.14+
	 *
	 * @param modelData
	 * @return
	 */
	public ItemCreator modelData(final int modelData) {
		this.modelData = modelData;

		return this;
	}

	/**
	 * Makes this item glow. Ignored if enchantments exists. Call {@link #hideTags(boolean)}
	 * to hide enchantment lores instead.
	 *
	 * @param glow
	 * @return
	 */
	public ItemCreator glow(final boolean glow) {
		this.glow = glow;

		return this;
	}

	/**
	 * Set the skull owner for this item, only works if the item is a skull.
	 *
	 * @see #fromPlayerSkull()
	 *
	 * @param skullOwner
	 * @return
	 */
	public ItemCreator skullOwner(final String skullOwner) {
		this.skullOwner = skullOwner;

		return this;
	}

	/**
	 * Set the skull owner for this item, only works if the item is a skull.
	 *
	 * @see #fromPlayerSkull()
	 *
	 * @param skullUrl
	 * @return
	 */
	public ItemCreator skullUrl(final String skullUrl) {
		this.skullUrl = skullUrl;

		return this;
	}

	/**
	 * Set the skull owner for this item, only works if the item is a skull.
	 *
	 * @see #fromPlayerSkull()
	 *
	 * @param skullBase64
	 * @return
	 */
	public ItemCreator skullBase64(final String skullBase64) {
		this.skullBase64 = skullBase64;

		return this;
	}

	/**
	 * Set the skull owner for this item, only works if the item is a skull.
	 *
	 * @see #fromPlayerSkull()
	 *
	 * @param skullUid
	 * @return
	 */
	public ItemCreator skullUid(final UUID skullUid) {
		this.skullUid = skullUid;

		return this;
	}

	/**
	 * Places an invisible custom tag to the item, for most server instances it
	 * will persist across saves/restarts (you should check just to be safe).
	 *
	 * To get the tag, use CompMetadata#getMetadata
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public ItemCreator tag(final String key, final String value) {
		this.tags.put(key, value);

		return this;
	}

	/**
	 * If this is a book, set its pages.
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(final String... pages) {
		return this.bookPages(Arrays.asList(pages));
	}

	/**
	 * Replace mini and legacy color codes in books?
	 *
	 * @param colorizeBook
	 * @return
	 */
	public ItemCreator colorizeBook(final boolean colorizeBook) {
		this.colorizeBook = colorizeBook;

		return this;
	}

	/**
	 * If this is a book, set its pages.
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(final List<String> pages) {
		if (this.bookPages == null)
			this.bookPages = new ArrayList<>();

		this.bookPages.addAll(pages);

		return this;
	}

	/**
	 * If this is a book, set its author.
	 *
	 * @param bookAuthor
	 * @return
	 */
	public ItemCreator bookAuthor(final String bookAuthor) {
		this.bookAuthor = bookAuthor;

		return this;
	}

	/**
	 * If this is a book, set its title.
	 *
	 * @param bookTitle
	 * @return
	 */
	public ItemCreator bookTitle(final String bookTitle) {
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

	/**
	 * Convenience method for dropping this item at the given location
	 *
	 * @param location
	 */
	public void drop(final Location location) {
		location.getWorld().dropItem(location, this.make());
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

		return this.make();
	}

	/**
	 * Construct a valid {@link ItemStack} from all parameters of this class.
	 *
	 * @return the finished item
	 */
	public ItemStack make() {

		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		ValidCore.checkBoolean(this.material != null || this.item != null, "Material or item must be set!");

		ItemStack compiledItem = this.item != null ? this.item.clone() : this.material.toItem();

		Object compiledMeta = this.meta != null ? this.meta.clone() : compiledItem.getItemMeta();

		// Override with given material
		if (this.item != null && this.material != null) {
			compiledItem.setType(this.material.getMaterial());

			if (MinecraftVersion.olderThan(V.v1_13))
				compiledItem.setData(new MaterialData(this.material.getMaterial(), this.material.getData()));
		}

		// Skip if air
		if (CompMaterial.isAir(compiledItem.getType()))
			return compiledItem;

		// Apply specific material color if possible
		color:
		if (this.color != null)
			if (compiledItem.getType().toString().contains("LEATHER")) {
				ValidCore.checkBoolean(compiledMeta instanceof LeatherArmorMeta, "Expected a leather item, cannot apply color to " + compiledItem);
				((LeatherArmorMeta) compiledMeta).setColor(this.color.getColor());

				// Hack: If you put WHITE_WOOL and a color, we automatically will change the material to the colorized version
			} else if (MinecraftVersion.atLeast(V.v1_13)) {
				final String dye = this.color.getDye().toString();
				final List<String> colorableMaterials = Arrays.asList("BANNER", "BED", "CARPET", "CONCRETE", "GLAZED_TERRACOTTA", "SHULKER_BOX", "STAINED_GLASS",
						"STAINED_GLASS_PANE", "TERRACOTTA", "WALL_BANNER", "WOOL");

				for (final String material : colorableMaterials) {
					final String suffix = "_" + material;

					if (compiledItem.getType().toString().endsWith(suffix)) {
						compiledItem.setType(ReflectionUtil.lookupEnum(Material.class, dye + suffix));

						break color;
					}
				}
			} else
				try {
					final byte dataValue = this.color.getDye().getWoolData();

					compiledItem.setData(new MaterialData(compiledItem.getType(), dataValue));
					compiledItem.setDurability(dataValue);

				} catch (final NoSuchMethodError err) {
					// Ancient MC, ignore
				}

		// Fix monster eggs
		if (compiledItem.getType().toString().endsWith("SPAWN_EGG") || compiledItem.getType().toString().equals("MONSTER_EGG")) {
			EntityType entity = null;

			if (MinecraftVersion.olderThan(V.v1_13)) { // Try to find it if already exists
				final EntityType pre = CompMonsterEgg.lookupEntity(compiledItem);

				if (pre != null && pre != CompEntityType.UNKNOWN)
					entity = pre;
			}

			if (entity == null) {
				final String itemName = compiledItem.getType().toString();

				String entityRaw = itemName.replace("_SPAWN_EGG", "");

				if (entityRaw.equals("MONSTER_EGG") && this.material != null && this.material.toString().endsWith("SPAWN_EGG"))
					entityRaw = this.material.toString().replace("_SPAWN_EGG", "");

				entity = CompEntityType.fromName(entityRaw);

				// Probably version incompatible
				if (entity == null)
					CommonCore.log("The following item could not be transformed into " + entityRaw + " egg, item: " + compiledItem);
			}

			if (entity != null)
				compiledMeta = CompMonsterEgg.setEntity(compiledItem, entity).getItemMeta();
		}

		if (this.damage != -1) {

			try {
				compiledItem.setDurability((short) this.damage);
			} catch (final Throwable t) {
			}

			try {
				if (compiledMeta instanceof org.bukkit.inventory.meta.Damageable)
					((org.bukkit.inventory.meta.Damageable) compiledMeta).setDamage(this.damage);
			} catch (final Throwable t) {
			}
		}

		if (compiledMeta instanceof SkullMeta) {
			final SkullMeta skullMeta = (SkullMeta) compiledMeta;

			if (this.skullOwner != null)
				try {
					skullMeta.setPlayerProfile(Bukkit.createProfile(this.skullOwner));

				} catch (final Throwable ex) {
					try {
						skullMeta.setOwnerProfile(Bukkit.createPlayerProfile(this.skullOwner));

					} catch (final Throwable ex2) {
						skullMeta.setOwner(this.skullOwner);
					}
				}

			if (this.skullUid != null)
				try {
					skullMeta.setPlayerProfile(Bukkit.createProfile(this.skullUid));

				} catch (final Throwable ex) {
					try {
						skullMeta.setOwnerProfile(Bukkit.createPlayerProfile(this.skullUid));

					} catch (final Throwable ex2) {
						try {
							skullMeta.setOwningPlayer(Remain.getOfflinePlayerByUniqueId(this.skullUid));

						} catch (final Throwable t) {
							skullMeta.setOwner(Remain.getOfflinePlayerByUniqueId(this.skullUid).getName());
						}
					}
				}

			if (this.skullUrl != null)
				compiledMeta = Remain.setSkullMetaBase64(skullMeta, Remain.convertSkinTextureUrlToBase64(this.skullUrl));

			if (this.skullBase64 != null)
				compiledMeta = Remain.setSkullMetaBase64(skullMeta, this.skullBase64);
		}

		if (compiledMeta instanceof BookMeta) {
			final BookMeta bookMeta = (BookMeta) compiledMeta;

			if (this.bookPages != null) {
				final List<SimpleComponent> colorizedComponents = new ArrayList<>();

				for (final String page : this.bookPages)
					colorizedComponents.add(SimpleComponent.fromMiniAmpersand(this.colorizeBook ? page : SimpleComponent.stripMiniMessageTags(CompChatColor.stripColorCodes(page))));

				Remain.setPages(bookMeta, colorizedComponents);
			}

			if (this.bookAuthor != null)
				bookMeta.setAuthor(CommonCore.getOrEmpty(this.bookAuthor));

			if (this.bookTitle != null) {
				final String title = CommonCore.getOrEmpty(this.bookTitle);

				bookMeta.setTitle(this.colorizeBook ? CompChatColor.translateColorCodes(title) : title);
			}

			// Fix "Corrupted NBT tag" error when any of these fields are not set
			if (bookMeta.getPages() == null)
				bookMeta.setPages(Arrays.asList(""));

			if (bookMeta.getAuthor() == null)
				bookMeta.setAuthor("Anonymous");

			if (bookMeta.getTitle() == null)
				bookMeta.setTitle("Book");
		}

		if (compiledMeta instanceof ItemMeta) {
			if (this.glow && this.enchants.isEmpty())
				try {
					((ItemMeta) compiledMeta).setEnchantmentGlintOverride(true);

				} catch (final Throwable t) {
					((ItemMeta) compiledMeta).addEnchant(CompEnchantment.DURABILITY, 1, true);

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
				((ItemMeta) compiledMeta).setDisplayName(CompChatColor.translateColorCodes("<reset><white>" + this.name));

			if (!this.lores.isEmpty()) {
				final List<String> coloredLores = new ArrayList<>();

				for (String lore : this.lores) {
					lore = CompChatColor.translateColorCodes((lorePrefix != null ? lorePrefix : "") + lore);

					for (final String split : CommonCore.split(lore, 40))
						coloredLores.add(split);
				}

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

		if (this.hideTags || this.flags.contains(CompItemFlag.HIDE_ATTRIBUTES))
			try {
				((ItemMeta) compiledMeta).setAttributeModifiers(MultimapBuilder.hashKeys().hashSetValues().build());

			} catch (final Throwable t) {
				// ignore
			}

		for (final CompItemFlag flag : this.flags)
			try {
				((ItemMeta) compiledMeta).addItemFlags(ReflectionUtil.lookupEnum(ItemFlag.class, flag.toString()));
			} catch (final Throwable t) {
			}

		// Set custom model data
		if (this.modelData != null && MinecraftVersion.atLeast(V.v1_14))
			try {
				((ItemMeta) compiledMeta).setCustomModelData(this.modelData);
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

		// 1.7.10 hack to add glow, requires no enchants
		if (this.glow && MinecraftVersion.equals(V.v1_7) && (this.enchants == null || this.enchants.isEmpty())) {
			final NBTItem nbtItem = new NBTItem(compiledItem);

			nbtItem.removeKey("ench");
			nbtItem.addCompound("ench");

			compiledItem = nbtItem.getItem();
		}

		// Apply NBT tags
		for (final Entry<String, String> entry : this.tags.entrySet())
			compiledItem = CompMetadata.setMetadata(compiledItem, entry.getKey(), entry.getValue());

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
	public static ItemCreator from(final CompMaterial material, final String name, @NonNull final Collection<String> lore) {
		return from(material, name, CommonCore.toArray(lore));
	}

	/**
	 * Convenience method to get a new item creator with material, name and lore set
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return new item creator
	 */
	public static ItemCreator from(final CompMaterial material, final String name, @NonNull final String... lore) {
		return new ItemCreator().material(material).name(name).lore(lore).hideTags(true);
	}

	/**
	 * Convenience method to get a wool
	 *
	 * @param color the wool color
	 * @return the new item creator
	 */
	public static ItemCreator fromWool(final CompColor color) {
		return fromItemStack(CompMaterial.makeWoolItem(color, 1)).color(color);
	}

	/**
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @return
	 */
	public static ItemCreator fromMonsterEgg(final EntityType entityType) {
		return fromItemStack(CompMonsterEgg.toItemStack(entityType));
	}

	/**
	 * Convenience method to get monster eggs
	 *
	 * @param entityType
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator fromMonsterEgg(final EntityType entityType, final String name, final String... lore) {
		return fromItemStack(CompMonsterEgg.toItemStack(entityType)).name(name).lore(lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param type
	 * @return
	 */
	public static ItemCreator fromPotion(final PotionEffectType type) {
		return fromPotion(type, 1);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param type
	 * @param durationTicks
	 * @param level
	 * @return
	 */
	public static ItemCreator fromPotion(final PotionEffectType type, final int durationTicks, final int level) {
		return fromPotion(type, durationTicks, level, null);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param type
	 * @param level
	 * @return
	 */
	public static ItemCreator fromPotion(final PotionEffectType type, final int level) {
		return fromPotion(type, Integer.MAX_VALUE, level, null);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param type
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator fromPotion(final PotionEffectType type, final String name, final String... lore) {
		return fromPotion(type, Integer.MAX_VALUE, 1, name, lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param effect
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator fromPotion(final PotionEffect effect, final String name, final String... lore) {
		return fromPotion(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier() + 1, name, lore);
	}

	/**
	 * Convenience method for creation potions
	 *
	 * @param effect
	 * @param durationTicks
	 * @param level
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator fromPotion(final PotionEffectType effect, final int durationTicks, final int level, final String name, final String... lore) {
		final boolean noLevel = level == 0;
		final ItemStack item = new ItemStack(level == 0 ? CompMaterial.GLASS_BOTTLE.getMaterial() : CompMaterial.POTION.getMaterial());

		if (!noLevel)
			Remain.setPotion(item, effect, durationTicks, level);

		final ItemCreator builder = fromItemStack(item);

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
	public static ItemCreator fromItemStack(final ItemStack item) {
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
	public static ItemCreator fromMaterial(final CompMaterial mat) {
		ValidCore.checkNotNull(mat, "Material cannot be null!");

		return new ItemCreator().material(mat);
	}

	/**
	 * Creates a player skull, should work in both legacy and new Bukkit APIs.
	 *
	 * @return
	 */
	public static ItemCreator fromPlayerSkull() {
		try {
			return fromItemStack(new ItemStack(ReflectionUtil.lookupEnum(Material.class, "PLAYER_HEAD")));

		} catch (final IllegalArgumentException e) {
			return fromItemStack(new ItemStack(ReflectionUtil.lookupEnum(Material.class, "SKULL_ITEM"), 1, (byte) 3));
		}
	}

	/**
	 * Creates a book
	 *
	 * @param book
	 * @param editable
	 * @return
	 */
	public static ItemCreator fromBookAdventure(final net.kyori.adventure.inventory.Book book, final boolean editable) {
		final String title = CommonCore.getOrDefault(LegacyComponentSerializer.legacySection().serialize(book.title()), "Blank");

		return ItemCreator.fromMaterial(editable ? CompMaterial.WRITABLE_BOOK : CompMaterial.WRITTEN_BOOK)
				.bookTitle(title)
				.bookAuthor(CommonCore.getOrDefault(LegacyComponentSerializer.legacySection().serialize(book.author()), "Blank"))
				.bookPages(CommonCore.convertList(book.pages(), SimpleComponent::serializeAdventureToMini))
				.name(title)
				.tag(SimpleBook.TAG, "true")
				.hideTags(true);
	}

	/**
	 * Creates a book
	 *
	 * @param book
	 * @param editable
	 * @return
	 */
	public static ItemCreator fromBookPlain(final SimpleBook book, final boolean editable) {
		return fromBook(book, editable, false);
	}

	/**
	 * Creates a book
	 *
	 * @param book
	 * @param editable
	 * @return
	 */
	public static ItemCreator fromBookColorized(final SimpleBook book, final boolean editable) {
		return fromBook(book, editable, true);
	}

	/**
	 * Creates a book
	 */
	private static ItemCreator fromBook(final SimpleBook book, final boolean editable, final boolean translateColors) {
		return ItemCreator.fromMaterial(editable ? CompMaterial.WRITABLE_BOOK : CompMaterial.WRITTEN_BOOK)
				.colorizeBook(translateColors)
				.bookTitle(CommonCore.getOrDefault(book.getTitle(), "Blank"))
				.bookAuthor(CommonCore.getOrDefault(book.getAuthor(), "Blank"))
				.bookPages(book.getPages())
				.name(book.getTitle())
				.tag(SimpleBook.TAG, "true")
				.hideTags(true);
	}
}