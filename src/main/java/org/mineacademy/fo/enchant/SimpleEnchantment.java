package org.mineacademy.fo.enchant;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents a simple way of getting your own enchantments into Minecraft
 *
 * DISCLAIMER: Minecraft is not built for your custom enchants. The enchant name is stored
 * on the client so it wont show anything for custom enchantments - Foundation will do its
 * best to intercept the set slot packet and inject the lore manually (requires ProtocolLib),
 *
 * USAGE: To use this, You need to first register your custom NMS class in
 * {@link #registerEnchantmentHandle(Class)} which needs to extend either Bukkit or NMS Enchantment class
 * and implement {@link NmsEnchant}
 *
 * TIP: If you want to register for custom events, make sure that the event actually happened when the
 * enchantment using {@link SimpleEnchantment#hasEnchant(ItemStack)} - DO NOT USE BUKKIT'S CONTAINENCHANTMENT
 * METHOD because some Minecraft versions report it to false (or poor implementations do - check that you have properly
 * registered it in the NMS inbuilt registry, especially for 1.19+).
 */
public abstract class SimpleEnchantment implements Listener {

	/*
	 * Cached for performance reasons
	 */
	private static boolean hasNamespacedKeys = MinecraftVersion.atLeast(V.v1_13);

	/**
	 * Pattern for matching namespaces
	 */
	private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9._-]+");

	/**
	 * Registration of custom enchants by their namespaced key.
	 */
	private static final StrictSet<SimpleEnchantment> registeredEnchantments = new StrictSet<>();

	/**
	 * The class that will be instantiated to wrap custom enchants.
	 */
	private static Class<? extends NmsEnchant> handleClass = null;

	/**
	 * The name of this enchant (i.e. Black Nova)
	 */
	private final String name;

	/**
	 * The name in the namespace format (i.e. black_nova)
	 */
	private final String namespacedName;
	private final String namespacedNameWithPrefix;

	/**
	 * The maximum level of this enchant
	 */
	private final int maxLevel;

	/**
	 * The actual handle injecting this enchant into Minecraft
	 */
	private final NmsEnchant handle;

	/**
	 * Used internally to pair enchants for MC older than 1.13
	 */
	@Deprecated
	private int id = -1;

	/**
	 * Create a new enchantment with the given name
	 *
	 * @param name
	 */
	protected SimpleEnchantment(@NonNull String name, int maxLevel) {
		String namespacedName = new String(name);
		namespacedName = namespacedName.toLowerCase().replace(" ", "_");
		namespacedName = ChatUtil.replaceDiacritic(namespacedName);

		Valid.checkBoolean(VALID_NAMESPACE.matcher(namespacedName).matches(), "Enchant name must only contain English alphabet names: " + name);

		this.name = name;
		this.namespacedName = namespacedName;
		this.namespacedNameWithPrefix = "minecraft:" + this.namespacedName;
		this.maxLevel = maxLevel;
		this.handle = this.assignHandle();

		this.handle.register();

		registeredEnchantments.add(this);
	}

	/*
	 * Private method to register this enchant
	 */
	private NmsEnchant assignHandle() {
		final V currentVersion = MinecraftVersion.getCurrent();

		Valid.checkNotNull(handleClass, "Custom enchantments are not implemented for " + currentVersion
				+ ". If you are a developer, implement it and call in SimpleEnchantment#registerEnchantmentHandle in onPluginLoad().");

		Constructor<?> constructor;

		try {
			constructor = ReflectionUtil.getConstructor(handleClass, SimpleEnchantment.class);

		} catch (final Throwable t) {
			throw new FoException("Please add one public constructor taking SimpleEnchantment as one parameter to your " + handleClass);
		}

		return (NmsEnchant) ReflectionUtil.instantiate(constructor, this);
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
		return this.name + " " + MathUtil.toRoman(level);
	}

	// ------------------------------------------------------------------------------------------
	// Bukkit methods
	// ------------------------------------------------------------------------------------------

	/**
	 * Converts into Bukkit's class {@link Enchantment}
	 *
	 * @return
	 */
	public final Enchantment toBukkit() {
		final Enchantment enchantment = this.handle.toBukkit();
		Valid.checkNotNull(enchantment, "Failed to convert " + this + " into a Bukkit class");

		return enchantment;
	}

	/**
	 *
	 * @param item
	 * @return
	 */
	public final boolean hasEnchant(ItemStack item) {
		return SimpleEnchantment.hasEnchantment(item, this);
	}

	/**
	 *
	 * @param item
	 * @param level
	 * @return
	 */
	public final ItemStack applyTo(ItemStack item, int level) {
		final ItemMeta meta = item.getItemMeta();

		meta.addEnchant(this.toBukkit(), level, true);
		item.setItemMeta(meta);

		return item;
	}

	// ------------------------------------------------------------------------------------------
	// Overridable methods
	// ------------------------------------------------------------------------------------------

	/**
	 * What items may this be applied to? Defaults to ALL
	 *
	 * @return
	 */
	public SimpleEnchantmentTarget getTarget() {
		return SimpleEnchantmentTarget.BREAKABLE;
	}

	/**
	 * Return the rarity
	 *
	 * @return
	 */
	public SimpleEnchantmentRarity getRarity() {
		return SimpleEnchantmentRarity.COMMON;
	}

	/**
	 * Get all of this enchants active slots
	 *
	 * @return
	 */
	public Set<EquipmentSlot> getActiveSlots() {
		return Common.newSet(EquipmentSlot.values());
	}

	/**
	 * What other enchants this one conflicts with? Defaults to false for all
	 *
	 * @param other
	 * @return
	 */
	public boolean conflictsWith(Enchantment other) {
		return false;
	}

	/**
	 * What items can be enchanted? Defaults to true for all
	 *
	 * @param item
	 * @return
	 */
	public boolean canEnchantItem(ItemStack item) {
		return true;
	}

	/**
	 * Get the startup level, 1 by default
	 *
	 * @return
	 */
	public int getStartLevel() {
		return 1;
	}

	/**
	 * Return the min cost for the given level
	 *
	 * @param level
	 * @return
	 */
	public int getMinCost(int level) {
		return 1;
	}

	/**
	 * Return the max cost for the given level
	 *
	 * @param level
	 * @return
	 */
	public int getMaxCost(int level) {
		return level;
	}

	/**
	 * Return if this enchant is tradeable by villagers, default is true
	 *
	 * @return
	 */
	public boolean isTradeable() {
		return true;
	}

	/**
	 * Return if this enchant is discoverable on the enchant table, default is true
	 *
	 * @return
	 */
	public boolean isDiscoverable() {
		return true;
	}

	/**
	 * Get if this enchant is a treasure, default false
	 *
	 * @return
	 */
	public boolean isTreasure() {
		return false;
	}

	/**
	 * Get if this enchant is cursed, default false
	 *
	 * @return
	 */
	public boolean isCursed() {
		return false;
	}

	/**
	 * Return the max level of this enchant
	 *
	 * @return
	 */
	public final int getMaxLevel() {
		return this.maxLevel;
	}

	/**
	 * Return the name of this enchant
	 *
	 * @return
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * Return the namedspaced name
	 *
	 * Use new NamespacedKey(SimplePlugin.getInstance(), this.name)) to convert on MC 1.13+ to a {@link NamespacedKey}.
	 *
	 * @return
	 */
	public final String getNamespacedName() {
		return this.namespacedName;
	}

	/**
	 * @deprecated internal use only
	 * @param id
	 */
	@Deprecated
	public final void setLegacyId(int id) {
		this.id = id;
	}

	// ------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------

	/**
	 * Registers a compatible NMS class to handle enchants
	 *
	 * @param handleClass
	 */
	public static void registerEnchantmentHandle(Class<? extends NmsEnchant> handleClass) {
		SimpleEnchantment.handleClass = handleClass;
	}

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

		for (final Entry<Enchantment, Integer> entry : vanilla.entrySet()) {
			final Enchantment enchantment = entry.getKey();
			final int level = entry.getValue();
			final SimpleEnchantment simpleEnchantment = fromBukkit(enchantment);

			if (simpleEnchantment != null)
				map.put(simpleEnchantment, level);
		}

		return map;
	}

	/**
	 * Return true if the item has our custom enchantment
	 *
	 * @param item
	 * @param simpleEnchantment
	 * @return
	 */
	public static boolean hasEnchantment(ItemStack item, @NonNull SimpleEnchantment simpleEnchantment) {
		if (item == null)
			return false;

		final Map<Enchantment, Integer> vanilla;

		try {
			vanilla = item.hasItemMeta() ? item.getItemMeta().getEnchants() : new HashMap<>();

		} catch (final NoSuchMethodError err) {
			if (Remain.hasItemMeta())
				err.printStackTrace();

			return false;

		} catch (final NullPointerException ex) {
			return false;
		}

		for (final Entry<Enchantment, Integer> entry : vanilla.entrySet()) {
			final Enchantment enchantment = entry.getKey();
			final SimpleEnchantment otherSimpleEnchantment = fromBukkit(enchantment);

			if (otherSimpleEnchantment != null && otherSimpleEnchantment.getNamespacedName().equals(simpleEnchantment.getNamespacedName()))
				return true;
		}

		return false;
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
			for (final Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
				final Enchantment enchantment = entry.getKey();
				final SimpleEnchantment simpleEnchantment = fromBukkit(enchantment);

				if (simpleEnchantment != null) {
					final String lore = simpleEnchantment.getLore(entry.getValue());

					if (lore != null && !lore.isEmpty())
						customEnchants.add(Common.colorize("&r&7" + lore));
				}
			}

		} catch (final NullPointerException ex) {
			// Some weird problem in third party plugin
		}

		if (!customEnchants.isEmpty()) {
			final ItemMeta meta = Remain.hasItemMeta() && item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
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

	private static SimpleEnchantment fromBukkit(Enchantment bukkitEnchantment) {
		if (hasNamespacedKeys) {
			final String key = bukkitEnchantment.getKey().asString();

			for (final SimpleEnchantment simpleEnchantment : registeredEnchantments)
				if (simpleEnchantment.namespacedNameWithPrefix.equals(key))
					return simpleEnchantment;

		} else {
			final String name = bukkitEnchantment.getName();

			for (final SimpleEnchantment simpleEnchantment : registeredEnchantments)
				if (simpleEnchantment.name.equals(name))
					return simpleEnchantment;

			try {
				final int id = ReflectionUtil.invoke("getId", bukkitEnchantment);

				for (final SimpleEnchantment simpleEnchantment : registeredEnchantments)
					if (simpleEnchantment.id == id)
						return simpleEnchantment;

			} catch (final Throwable t) {
				// Unsupported, very old MC
			}
		}

		return null;
	}

	/**
	 * Listens and executes events for {@link SimpleEnchantment}
	 * <p>
	 * @deprecated Internal use only!
	 */
	@Deprecated
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Listener implements org.bukkit.event.Listener {

		@Getter
		private static volatile Listener instance = new Listener();

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageByEntityEvent event) {
			final Entity damager = event.getDamager();

			if (damager instanceof LivingEntity)
				this.execute((LivingEntity) damager, (enchant, level) -> enchant.onDamage(level, (LivingEntity) damager, event));
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
		public void onInteract(PlayerInteractEvent event) {
			if (!Remain.isInteractEventPrimaryHand(event))
				return;

			this.execute(event.getPlayer(), (enchant, level) -> enchant.onInteract(level, event));
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onBreakBlock(BlockBreakEvent event) {
			this.execute(event.getPlayer(), (enchant, level) -> enchant.onBreakBlock(level, event));
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onShoot(ProjectileLaunchEvent event) {
			try {
				final ProjectileSource projectileSource = event.getEntity().getShooter();

				if (projectileSource instanceof LivingEntity) {
					final LivingEntity shooter = (LivingEntity) projectileSource;

					this.execute(shooter, (enchant, level) -> enchant.onShoot(level, shooter, event));
					EntityUtil.trackHit(event.getEntity(), hitEvent -> this.execute(shooter, (enchant, level) -> enchant.onHit(level, shooter, hitEvent)));
				}
			} catch (final NoSuchMethodError ex) {
				if (MinecraftVersion.atLeast(V.v1_4))
					ex.printStackTrace();
			}
		}

		private void execute(LivingEntity source, BiConsumer<SimpleEnchantment, Integer> executer) {
			try {
				final ItemStack hand = source instanceof Player ? ((Player) source).getItemInHand() : source.getEquipment().getItemInHand();

				if (hand != null)
					for (final Entry<SimpleEnchantment, Integer> e : SimpleEnchantment.findEnchantments(hand).entrySet())
						executer.accept(e.getKey(), e.getValue());

			} catch (final NoSuchMethodError ex) {
				if (Remain.hasItemMeta())
					ex.printStackTrace();
			}
		}
	}

}