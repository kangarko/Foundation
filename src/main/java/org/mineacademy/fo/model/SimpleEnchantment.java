package org.mineacademy.fo.model;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;

/**
 * Represents a simple way of getting your own enchantments into Minecraft
 * <p>
 * DISCLAIMER: Minecraft is not built for your custom enchants. Removing this enchant
 * from the item later will still preserve the lore saying the item has it.
 * <p>
 * TIP: If you want to register for custom events you just make this class implemements
 * Listener and we register it automatically! However, you must make sure that the
 * event actually happened when the item was used. Use {@link ItemStack#containsEnchantment(Enchantment)}
 * to check
 */
public abstract class SimpleEnchantment extends Enchantment {

	/**
	 * Pattern for matching namespaces
	 */
	private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9._-]+");

	/**
	 * The name of this enchant
	 */
	private final String name;

	/**
	 * The maximum level of this enchant
	 */
	private final int maxLevel;

	/**
	 * Create a new enchantment with the given name
	 *
	 * @param name
	 */
	protected SimpleEnchantment(String name, int maxLevel) {
		super(toKey(name));

		this.name = name;
		this.maxLevel = maxLevel;

		Remain.registerEnchantment(this);
	}

	// Convert a name into a namespace
	private static NamespacedKey toKey(@NonNull String name) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_13), "Unfortunately, SimpleEnchantment requires Minecraft 1.13.2 or greater. Cannot make " + name);

		name = new String(name);
		name = name.toLowerCase().replace(" ", "_");
		name = ChatUtil.replaceDiacritic(name);

		Valid.checkBoolean(name != null && VALID_NAMESPACE.matcher(name).matches(), "Enchant name must only contain English alphabet names: " + name);
		return new NamespacedKey(SimplePlugin.getInstance(), name);
	}

	// ------------------------------------------------------------------------------------------
	// Events
	// ------------------------------------------------------------------------------------------

	/**
	 * Triggered automatically when the attacker has this enchantment
	 *
	 * @param level   the level of this enchant
	 * @param damager
	 * @param event
	 */
	protected void onDamage(int level, LivingEntity damager, EntityDamageByEntityEvent event) {
	}

	/**
	 * Triggered automatically when the player clicks block/air with the given enchant
	 *
	 * @param level
	 * @param event
	 */
	protected void onInteract(int level, PlayerInteractEvent event) {
	}

	/**
	 * Triggered automatically when the player breaks block having hand item with this enchantment
	 *
	 * @param level
	 * @param event
	 */
	protected void onBreakBlock(int level, BlockBreakEvent event) {
	}

	/**
	 * Triggered automatically when the projectile was shot from a living entity
	 * having this item at their hand
	 *
	 * @param level
	 * @param shooter
	 * @param event
	 */
	protected void onShoot(int level, LivingEntity shooter, ProjectileLaunchEvent event) {
	}

	/**
	 * Triggered automatically when the projectile hits something and the shooter
	 * is a living entity having the hand item having this enchant
	 *
	 * @param level
	 * @param shooter
	 * @param event
	 */
	protected void onHit(int level, LivingEntity shooter, ProjectileHitEvent event) {
	}

	// ------------------------------------------------------------------------------------------
	// Convenience methods
	// ------------------------------------------------------------------------------------------

	/**
	 * Gives this enchant to the given item at a certain level
	 *
	 * @param item
	 * @param level
	 * @return
	 */
	public ItemStack applyTo(ItemStack item, int level) {
		final ItemMeta meta = item.getItemMeta();

		meta.addEnchant(this, level, true);
		item.setItemMeta(meta);

		return item;
	}

	// ------------------------------------------------------------------------------------------
	// Our own methods
	// ------------------------------------------------------------------------------------------

	/**
	 * Return the lore shown on items having this enchant
	 * Return null to hide the lore
	 * <p>
	 * We have to add item lore manually since Minecraft does not really support custom
	 * enchantments
	 *
	 * @param level
	 * @return
	 */
	public String getLore(int level) {
		return name + " " + MathUtil.toRoman(level);
	}

	/**
	 * Improved version of EnchantmentTarget with more
	 * variety in the options.
	 *
	 * Select what items this enchantment may be applied to?
	 * Defaults to BREAKABLE (all)
	 *
	 * @return
	 */
	public SimpleEnchantmentTarget getCustomItemTarget() {
		return SimpleEnchantmentTarget.BREAKABLE;
	}

	/**
	 * Select what material this enchantment can be
	 * applied to.
	 * @return
	 */
	public Material enchantMaterial() { return SimpleEnchantmentTargetMaterial.enchantMaterial(); }

	/**
	 * Select what materials this enchantment can be
	 * applied to.
	 * @return
	 */
	public Set<Material> enchantMaterials() { return SimpleEnchantmentTargetMaterial.enchantMaterials(); }

	// ------------------------------------------------------------------------------------------
	// Bukkit methods
	// ------------------------------------------------------------------------------------------

	/**
	 * What items may this be applied to? Defaults to ALL
	 *
	 * @return
	 */
	@Override
	public EnchantmentTarget getItemTarget() {
		return EnchantmentTarget.BREAKABLE;
	}

	/**
	 * What other enchants this one conflicts with? Defaults to false for all
	 *
	 * @param
	 * @return
	 */
	@Override
	public boolean conflictsWith(Enchantment other) {
		return false;
	}

	/**
	 * What items can be enchanted? Defaults to true for all
	 *
	 * @param
	 * @return
	 */
	@Override
	public boolean canEnchantItem(ItemStack item) {
		return true;
	}

	/**
	 * Get the startup level, 1 by default
	 *
	 * @return
	 */
	@Override
	public int getStartLevel() {
		return 1;
	}

	/**
	 * Get if this enchant is a treasure, default false
	 *
	 * @return
	 */
	@Override
	public boolean isTreasure() {
		return false;
	}

	/**
	 * Get if this enchant is cursed, default false
	 *
	 * @return
	 */
	@Override
	public boolean isCursed() {
		return false;
	}

	/**
	 * Return the max level of this enchant
	 *
	 * @return
	 */
	@Override
	public final int getMaxLevel() {
		return maxLevel;
	}

	/**
	 * Return the name of this enchant
	 *
	 * @return
	 */
	@Override
	public final String getName() {
		return name;
	}

	// ------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------

	/**
	 * Return a map of enchantments with their levels on the given item
	 *
	 * @param item
	 * @return
	 */
	public static Map<SimpleEnchantment, Integer> findEnchantments(ItemStack item) {
		final Map<SimpleEnchantment, Integer> map = new HashMap<>();

		if (item == null)
			return map;

		final Map<Enchantment, Integer> vanilla;

		try {
			vanilla = item.hasItemMeta() ? item.getItemMeta().getEnchants() : new HashMap<>();
		} catch (final NoSuchMethodError err) {
			if (Remain.hasItemMeta())
				err.printStackTrace();

			return map;

		} catch (final NullPointerException ex) {
			// Caused if any associated enchant is null, probably by a third party plugin
			return map;
		}

		for (final Entry<Enchantment, Integer> e : vanilla.entrySet()) {
			final Enchantment enchantment = e.getKey();
			final int level = e.getValue();

			if (enchantment instanceof SimpleEnchantment)
				map.put((SimpleEnchantment) enchantment, level);
		}

		return map;
	}

	/**
	 * Since Minecraft client cannot display custom enchantments we have to add lore manually.
	 * <p>
	 * This adds the fake enchant lore for the given item in case it does not exist.
	 *
	 * @param item
	 * @return the modified item or null if item was not edited (no enchants found)
	 * @deprecated internal use only
	 */
	@Deprecated
	public static ItemStack addEnchantmentLores(ItemStack item) {
		final List<String> customEnchants = new ArrayList<>();

		// Fill in our enchants
		try {
			for (final Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet())
				if (e.getKey() instanceof SimpleEnchantment) {
					final String lore = ((SimpleEnchantment) e.getKey()).getLore(e.getValue());

					if (lore != null && !lore.isEmpty())
						customEnchants.add(Common.colorize("&r&7" + lore));
				}

		} catch (final NullPointerException ex) {
			// Some weird problem in third party plugin
		}

		if (!customEnchants.isEmpty()) {
			final ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
			final List<String> originalLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
			final List<String> finalLore = new ArrayList<>();

			final List<String> colorlessOriginals = new ArrayList<>();

			for (final String original : originalLore)
				colorlessOriginals.add(ChatColor.stripColor(Common.colorize(original)));

			// Place our enchants
			for (final String customEnchant : customEnchants) {
				final String colorlessEnchant = ChatColor.stripColor(Common.colorize(customEnchant));

				if (!colorlessOriginals.contains(colorlessEnchant))
					finalLore.add(customEnchant);
			}

			// Place the original lore at the bottom
			finalLore.addAll(originalLore);

			// Set the lore
			meta.setLore(finalLore);

			// Update the item stack
			item.setItemMeta(meta);

			return item;
		}

		return null;
	}
}