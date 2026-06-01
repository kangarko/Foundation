package org.mineacademy.fo.remain;

import java.lang.reflect.Constructor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Vibration;
import org.bukkit.entity.Player;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.ReflectionException;

import lombok.SneakyThrows;

/**
 * Wrapper for {@link Particle}
 */
public enum CompParticle {

	ASH("ASH", "ASH"),
	/**
	 * @Deprecated removed in 1.13, use {@value CompParticle#BLOCK_DUST} instead with metadata
	 */
	BARRIER("BARRIER", null) {
		@Override
		public boolean isRemoved() {
			return true;
		}
	},
	BLOCK_CRACK("BLOCK_CRACK", "BLOCK"),
	BLOCK_CRUMBLE("BLOCK_CRUMBLE", "BLOCK_CRUMBLE"),
	BLOCK_DUST("BLOCK_DUST", "BLOCK"),
	/**
	 * Uses BlockData as DataType
	 */
	BLOCK_MARKER("BLOCK_MARKER", "BLOCK_MARKER"),
	BUBBLE_COLUMN_UP("BUBBLE_COLUMN_UP", "BUBBLE_COLUMN_UP"),
	BUBBLE_POP("BUBBLE_POP", "BUBBLE_POP"),
	CAMPFIRE_COSY_SMOKE("CAMPFIRE_COSY_SMOKE", "CAMPFIRE_COSY_SMOKE"),
	CAMPFIRE_SIGNAL_SMOKE("CAMPFIRE_SIGNAL_SMOKE", "CAMPFIRE_SIGNAL_SMOKE"),
	CHERRY_LEAVES("CHERRY_LEAVES", "CHERRY_LEAVES"),
	PALE_OAK_LEAVES("CHERRY_LEAVES", "CHERRY_LEAVES"),
	CLOUD("CLOUD", "CLOUD"),
	COMPOSTER("COMPOSTER", "COMPOSTER"),
	CRIMSON_SPORE("CRIMSON_SPORE", "CRIMSON_SPORE"),
	CRIT("CRIT", "CRIT"),
	CRIT_MAGIC("CRIT_MAGIC", "ENCHANTED_HIT"),
	CURRENT_DOWN("CURRENT_DOWN", "CURRENT_DOWN"),
	DAMAGE_INDICATOR("DAMAGE_INDICATOR", "DAMAGE_INDICATOR"),
	DOLPHIN("DOLPHIN", "DOLPHIN"),
	// MC 1.8.8+
	DRAGON_BREATH("DRAGON_BREATH", "DRAGON_BREATH"),
	DRIP_LAVA("DRIP_LAVA", "DRIPPING_LAVA"),
	DRIP_WATER("DRIP_WATER", "DRIPPING_WATER"),
	DRIPPING_DRIPSTONE_LAVA("DRIPPING_DRIPSTONE_LAVA", "DRIPPING_DRIPSTONE_LAVA"),
	DRIPPING_DRIPSTONE_WATER("DRIPPING_DRIPSTONE_WATER", "DRIPPING_DRIPSTONE_WATER"),
	DRIPPING_HONEY("DRIPPING_HONEY", "DRIPPING_HONEY"),
	DRIPPING_OBSIDIAN_TEAR("DRIPPING_OBSIDIAN_TEAR", "DRIPPING_OBSIDIAN_TEAR"),
	/**
	 * Uses {@link DustTransition} as DataType
	 */
	DUST_COLOR_TRANSITION("DUST_COLOR_TRANSITION", "DUST_COLOR_TRANSITION"),
	DUST_PILLAR("DUST_PILLAR", "DUST_PILLAR"),
	DUST_PLUME("DUST_PLUME", "DUST_PLUME"),
	EGG_CRACK("EGG_CRACK", "EGG_CRACK"),
	ELECTRIC_SPARK("ELECTRIC_SPARK", "ELECTRIC_SPARK"),
	ENCHANTMENT_TABLE("ENCHANTMENT_TABLE", "ENCHANT"),
	END_ROD("END_ROD", "END_ROD"),
	EXPLOSION_HUGE("EXPLOSION_HUGE", "EXPLOSION_EMITTER"),
	EXPLOSION_LARGE("EXPLOSION_LARGE", "EXPLOSION"),
	EXPLOSION_NORMAL("EXPLOSION_NORMAL", "POOF"),
	FALLING_DRIPSTONE_LAVA("FALLING_DRIPSTONE_LAVA", "FALLING_DRIPSTONE_LAVA"),
	FALLING_DRIPSTONE_WATER("FALLING_DRIPSTONE_WATER", "FALLING_DRIPSTONE_WATER"),
	/**
	 * Uses BlockData as DataType
	 */
	FALLING_DUST("FALLING_DUST", "FALLING_DUST"),

	FALLING_HONEY("FALLING_HONEY", "FALLING_HONEY"),
	FALLING_LAVA("FALLING_LAVA", "FALLING_LAVA"),

	FALLING_NECTAR("FALLING_NECTAR", "FALLING_NECTAR"),
	FALLING_OBSIDIAN_TEAR("FALLING_OBSIDIAN_TEAR", "FALLING_OBSIDIAN_TEAR"),
	FALLING_SPORE_BLOSSOM("FALLING_SPORE_BLOSSOM", "FALLING_SPORE_BLOSSOM"),
	FALLING_WATER("FALLING_WATER", "FALLING_WATER"),
	FIREWORKS_SPARK("FIREWORKS_SPARK", "FIREWORK"),
	FLAME("FLAME", "FLAME"),
	FLASH("FLASH", "FLASH"),
	GLOW("GLOW", "GLOW"),
	GLOW_SQUID_INK("GLOW_SQUID_INK", "GLOW_SQUID_INK"),
	GUST("GUST", "GUST"),
	GUST_EMITTER_LARGE("GUST_EMITTER_LARGE", "GUST_EMITTER_LARGE"),
	GUST_EMITTER_SMALL("GUST_EMITTER_SMALL", "GUST_EMITTER_SMALL"),
	HEART("HEART", "HEART"),
	INFESTED("INFESTED", "INFESTED"),
	ITEM_COBWEB("ITEM_COBWEB", "ITEM_COBWEB"),
	ITEM_CRACK("ITEM_CRACK", "ITEM"),
	/**
	 * @deprecated removed in 1.20.5
	 */
	@Deprecated
	ITEM_TAKE("ITEM_TAKE", null) {
		@Override
		public boolean isRemoved() {
			return true;
		}
	},
	LANDING_HONEY("LANDING_HONEY", "LANDING_HONEY"),
	LANDING_LAVA("LANDING_LAVA", "LANDING_LAVA"),
	LANDING_OBSIDIAN_TEAR("LANDING_OBSIDIAN_TEAR", "LANDING_OBSIDIAN_TEAR"),
	LAVA("LAVA", "LAVA"),
	MOB_APPEARANCE("MOB_APPEARANCE", "ELDER_GUARDIAN"),
	NAUTILUS("NAUTILUS", "NAUTILUS"),
	NOTE("NOTE", "NOTE"),
	OMINOUS_SPAWNING("OMINOUS_SPAWNING", "OMINOUS_SPAWNING"),
	PORTAL("PORTAL", "PORTAL"),
	RAID_OMEN("RAID_OMEN", "RAID_OMEN"),
	REDSTONE("REDSTONE", "DUST"),
	REVERSE_PORTAL("REVERSE_PORTAL", "REVERSE_PORTAL"),
	SCRAPE("SCRAPE", "SCRAPE"),
	/**
	 * Use {@link Float} as DataType
	 */
	SCULK_CHARGE("SCULK_CHARGE", "SCULK_CHARGE"),
	SCULK_CHARGE_POP("SCULK_CHARGE_POP", "SCULK_CHARGE_POP"),
	SCULK_SOUL("SCULK_SOUL", "SCULK_SOUL"),
	/**
	 * Use {@link Integer} as DataType
	 */
	SHRIEK("SHRIEK", "SHRIEK"),
	SLIME("SLIME", "ITEM_SLIME"),
	SMALL_FLAME("SMALL_FLAME", "SMALL_FLAME"),
	SMALL_GUST("SMALL_GUST", "SMALL_GUST"),
	SMOKE_LARGE("SMOKE_LARGE", "LARGE_SMOKE"),
	SMOKE_NORMAL("SMOKE_NORMAL", "SMOKE"),
	SNEEZE("SNEEZE", "SNEEZE"),
	SNOW_SHOVEL("SNOW_SHOVEL", "SNOW_SHOVEL"),
	SNOWBALL("SNOWBALL", "ITEM_SNOWBALL"),
	SNOWFLAKE("SNOWFLAKE", "SNOWFLAKE"),
	SONIC_BOOM("SONIC_BOOM", "SONIC_BOOM"),
	SOUL("SOUL", "SOUL"),
	SOUL_FIRE_FLAME("SOUL_FIRE_FLAME", "SOUL_FIRE_FLAME"),
	SPELL("SPELL", "EFFECT"),
	SPELL_INSTANT("SPELL_INSTANT", "INSTANT_EFFECT"),
	SPELL_MOB("SPELL_MOB", "ENTITY_EFFECT"),
	/**
	 * @deprecated removed somewhere in 1.19
	 */
	@Deprecated
	SPELL_MOB_AMBIENT("SPELL_MOB_AMBIENT", "AMBIENT_ENTITY_EFFECT") {
		@Override
		public boolean isRemoved() {
			return true;
		}
	},
	SPELL_WITCH("SPELL_WITCH", "WITCH"),
	SPIT("SPIT", "SPIT"),
	SPORE_BLOSSOM_AIR("SPORE_BLOSSOM_AIR", "SPORE_BLOSSOM_AIR"),
	SQUID_INK("SQUID_INK", "SQUID_INK"),
	SUSPENDED("SUSPENDED", "UNDERWATER"),
	SUSPENDED_DEPTH("SUSPENDED_DEPTH", "UNDERWATER"),
	SWEEP_ATTACK("SWEEP_ATTACK", "SWEEP_ATTACK"),
	TOTEM("TOTEM", "TOTEM_OF_UNDYING"),
	TOWN_AURA("TOWN_AURA", "MYCELIUM"),
	TRAIL("TRAIL", "TRAIL"),
	TRIAL_OMEN("TRIAL_OMEN", "TRIAL_OMEN"),
	TRIAL_SPAWNER_DETECTION("TRIAL_SPAWNER_DETECTION", "TRIAL_SPAWNER_DETECTION"),
	TRIAL_SPAWNER_DETECTION_OMINOUS("TRIAL_SPAWNER_DETECTION_OMINOUS", "TRIAL_SPAWNER_DETECTION_OMINOUS"),
	VAULT_CONNECTION("VAULT_CONNECTION", "VAULT_CONNECTION"),
	/**
	 * Uses {@link Vibration} as DataType
	 */
	VIBRATION("VIBRATION", "VIBRATION"),
	VILLAGER_ANGRY("VILLAGER_ANGRY", "ANGRY_VILLAGER"),
	VILLAGER_HAPPY("VILLAGER_HAPPY", "HAPPY_VILLAGER"),
	WARPED_SPORE("WARPED_SPORE", "WARPED_SPORE"),
	WATER_BUBBLE("WATER_BUBBLE", "BUBBLE"),
	WATER_DROP("WATER_DROP", "RAIN"),
	WATER_SPLASH("WATER_SPLASH", "SPLASH"),
	WATER_WAKE("WATER_WAKE", "FISHING"),
	WAX_OFF("WAX_OFF", "WAX_OFF"),
	WAX_ON("WAX_ON", "WAX_ON"),
	WHITE_ASH("WHITE_ASH", "WHITE_ASH"),
	WHITE_SMOKE("WHITE_SMOKE", "WHITE_SMOKE"),
	TINTED_LEAVES("TINTED_LEAVES", "TINTED_LEAVES"),
	FIREFLY("FIREFLY", "FIREFLY"),
	COPPER_FIRE_FLAME("COPPER_FIRE_FLAME", "COPPER_FIRE_FLAME"),
	PAUSE_MOB_GROWTH("PAUSE_MOB_GROWTH", "PAUSE_MOB_GROWTH"),
	RESET_MOB_GROWTH("RESET_MOB_GROWTH", "RESET_MOB_GROWTH");

	/**
	 * Hardcoded values for best performance
	 */
	private static final boolean atLeast1_13 = MinecraftVersion.atLeast(V.v1_13);
	private static final boolean atLeast1_12 = MinecraftVersion.atLeast(V.v1_12);

	/**
	 * The name used until Minecraft 1.20.4
	 */
	private final String nameLegacy;

	/**
	 * The name used since Minecraft 1.20.5
	 */
	private final String nameModern;

	/**
	 * The NMS EnumParticle class cache here for top performance
	 *
	 * Can be null.
	 */
	private final Object nmsEnumParticle;

	/**
	 * The bukkit particle object, if this is running on a new MC version, cached for top performance
	 *
	 * Can be null.
	 */
	private final Object bukkitEnumParticle;

	/**
	 * The NMS packet class constructor cached here for top performance
	 */
	private final Constructor<?> packetConstructor;

	@SuppressWarnings("rawtypes")
	CompParticle(final String nameLegacy, final String nameModern) {
		this.nameLegacy = nameLegacy;
		this.nameModern = nameModern;

		try {

			// Minecraft >1.13 already has native API
			if (MinecraftVersion.olderThan(V.v1_13)) {
				final Class<?> packetClass;

				try {
					packetClass = Remain.getNMSClass("PacketPlayOutWorldParticles", "N/A");

				} catch (final ReflectionException ex) {

					// Likely 1.7.10 Thermos, unsupported
					this.nmsEnumParticle = null;
					this.packetConstructor = null;
					this.bukkitEnumParticle = null;

					return;
				}

				final Class<? extends Enum> particleClass = (Class<? extends Enum>) Remain.getNMSClass("EnumParticle");

				this.nmsEnumParticle = ReflectionUtil.lookupEnumSilent(particleClass, this.name());

				// MC 1.8.8 or newer but lacks this given particle type
				if (this.nmsEnumParticle == null)
					this.packetConstructor = null;
				else
					this.packetConstructor = ReflectionUtil.getConstructor(packetClass, particleClass, Boolean.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, int[].class);

			} else {
				this.nmsEnumParticle = null;
				this.packetConstructor = null;
			}

			if (MinecraftVersion.atLeast(V.v1_12)) {
				final Class<Enum> particleClass = ReflectionUtil.lookupClass("org.bukkit.Particle");

				Object particleInstance = null;

				if (this.nameModern != null)
					particleInstance = ReflectionUtil.lookupEnumSilent(particleClass, this.nameModern);

				if (particleInstance == null)
					particleInstance = ReflectionUtil.lookupEnumSilent(particleClass, this.nameLegacy);

				if (particleInstance == null)
					particleInstance = ReflectionUtil.lookupEnumSilent(particleClass, this.name());

				this.bukkitEnumParticle = particleInstance;

			} else
				this.bukkitEnumParticle = null;

		} catch (final Throwable t) {
			t.printStackTrace();

			throw new RuntimeException("Fatal error setting up CompParticle, see above");
		}
	}

	/**
	 * Return true if this particle no longer exists on modern MC.
	 *
	 * @return
	 */
	public boolean isRemoved() {
		return false;
	}

	/**
	 * Return true if this particle is supported on this server version, false if not
	 * 
	 * @return
	 */
	public boolean isAvailable() {
		if (atLeast1_13)
			return this.bukkitEnumParticle != null;

		return this.packetConstructor != null;
	}

	/**
	 * Spawns this particle with the given color, only works for {@link #REDSTONE}
	 * The particle size requires MC 1.13+
	 *
	 * @param location
	 * @param color
	 * @param particleSize
	 */
	public void spawn(final Location location, final Color color, final float particleSize) {
		ValidCore.checkBoolean(this == REDSTONE, "Can only send colors for REDSTONE particle, not: " + this);

		if (atLeast1_13)
			location.getWorld().spawnParticle((Particle) this.bukkitEnumParticle, location, 1, 0, 0, 0, 0, new DustOptions(color, particleSize));

		else {
			final float red = color.getRed() == 0 ? Float.MIN_VALUE : color.getRed() / 255F;
			final float green = color.getGreen() / 255F;
			final float blue = color.getBlue() / 255F;

			this.spawn(location, red, green, blue, 1, 0, 1, null);
		}
	}

	/**
	 * Spawns this particle with the given color, only works for {@link #REDSTONE}
	 * The particle size requires MC 1.13+
	 *
	 * @param player
	 * @param location
	 * @param color
	 * @param particleSize
	 */
	public void spawn(final Player player, final Location location, final Color color, final float particleSize) {
		ValidCore.checkBoolean(this == REDSTONE, "Can only send colors for REDSTONE particle, not: " + this);

		if (atLeast1_13)
			player.spawnParticle((Particle) this.bukkitEnumParticle, location, 1, 0, 0, 0, 0, new DustOptions(color, particleSize));

		else {
			final float red = color.getRed() == 0 ? Float.MIN_VALUE : color.getRed() / 255F;
			final float green = color.getGreen() / 255F;
			final float blue = color.getBlue() / 255F;

			this.spawn(player, location, red, green, blue, 1, 0, 1, null);
		}
	}

	/**
	 * Spawns a particle at the given location
	 *
	 * @param location
	 */
	public void spawn(final Location location) {
		this.spawn(location, 0, 0, 0, 0, 1, 0, null);
	}

	/**
	 * Spawns the particle at the given location with extra material data
	 *
	 * @param location
	 * @param data
	 */
	public final void spawn(final Location location, final CompMaterial data) {
		ValidCore.checkBoolean(this == ITEM_CRACK || this == BLOCK_CRACK || this == BLOCK_DUST || this == FALLING_DUST, "Can only call particle spawn with data on crack or dust particles, not: " + this);

		if (atLeast1_12) {
			if (atLeast1_13)
				location.getWorld().spawnParticle((Particle) this.bukkitEnumParticle, location, 1, data.getMaterial().createBlockData());

			else
				location.getWorld().spawnParticle((Particle) this.bukkitEnumParticle, location, 1, data.getMaterial().getNewData(data.getData()));

		} else
			this.spawn(location, 0, 0, 0, 0, 1, 0, (int) data.getData());
	}

	/**
	 * Spawns a particle at the given location with given data
	 *
	 * @param location
	 * @param extra
	 */
	public final void spawn(final Location location, final double extra) {
		this.spawn(location, 0, extra);
	}

	/**
	 * Spawns a particle at the given location with given data
	 *
	 * @param location
	 * @param speed
	 * @param extra
	 */
	public final void spawn(final Location location, final double speed, final double extra) {
		this.spawn(location, 0d, 0d, 0d, speed, 0, extra, null);
	}

	/**
	 * Spawns a particle at the given location with the given params, see Bukkit API for documentation
	 *
	 * @param location
	 * @param offsetX
	 * @param offsetY
	 * @param offsetZ
	 * @param speed
	 * @param count
	 * @param extra
	 * @param data
	 */
	public void spawn(final Location location, final double offsetX, final double offsetY, final double offsetZ, final double speed, final int count, final double extra, final int... data) {
		if (this.isRemoved())
			return;

		// Minecraft 1.12 and up
		if (this.bukkitEnumParticle != null) {
			if (atLeast1_13 && this == REDSTONE)
				location.getWorld().spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra, new DustOptions(Color.RED, 1F));

			else if (data == null || data.length == 0) {

				// Particles such as block/item/dust require a data object and would crash the server if spawned without one
				if (atLeast1_13 && ((Particle) this.bukkitEnumParticle).getDataType() != Void.class)
					return;

				location.getWorld().spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra);

			} else
				location.getWorld().spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra, data);
		}

		else if (this.packetConstructor != null) {
			final Object packet = this.preparePacket(location.getX(), location.getY(), location.getZ(), offsetX, offsetY, offsetZ, speed, count, extra, data);

			for (final Player player : Remain.getOnlinePlayers())
				Remain.sendPacket(player, packet);
		}
	}

	/**
	 * Spawns a particle only shown to the given player at the given location
	 *
	 * @param player
	 * @param location
	 */
	public void spawn(final Player player, final Location location) {
		this.spawn(player, location, 0d, 0d, 0d, 0d, 1, 0d, null);
	}

	/**
	 * Spawns a particle only shown to the given player at the given location
	 *
	 * @param player
	 * @param location
	 * @param extra
	 */
	public void spawn(final Player player, final Location location, final double extra) {
		this.spawn(player, location, 0d, 0d, 0d, 0d, 0, extra, null);
	}

	/**
	 * Spawns the particle only shown to the given player at the given location with extra material data
	 *
	 * @param player
	 * @param location
	 * @param data
	 */
	public final void spawn(final Player player, final Location location, final CompMaterial data) {
		ValidCore.checkBoolean(this == ITEM_CRACK || this == BLOCK_CRACK || this == BLOCK_DUST || this == FALLING_DUST, "Can only call particle spawn with data on crack or dust particles, not: " + this);

		if (atLeast1_12) {
			if (atLeast1_13)
				player.spawnParticle((Particle) this.bukkitEnumParticle, location, 1, data.getMaterial().createBlockData());

			else
				player.spawnParticle((Particle) this.bukkitEnumParticle, location, 1, data.getMaterial().getNewData(data.getData()));

		} else
			this.spawn(player, location, 0, 0, 0, 0, 1, 0, (int) data.getData());
	}

	/**
	 * Spawns a particle only shown to the given player at the given location with the given params, see Bukkit API for documentation
	 *
	 * @param player
	 * @param location
	 * @param offsetX
	 * @param offsetY
	 * @param offsetZ
	 * @param speed
	 * @param count
	 * @param extra
	 * @param data
	 */
	public void spawn(final Player player, final Location location, final double offsetX, final double offsetY, final double offsetZ, final double speed, final int count, final double extra, final int... data) {
		if (this.isRemoved())
			return;

		// Minecraft 1.12 and up
		if (this.bukkitEnumParticle != null) {
			if (atLeast1_13 && this == REDSTONE)
				player.spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra, new DustOptions(Color.RED, 1F));

			else if (data == null || data.length == 0) {

				// Particles such as block/item/dust require a data object and would crash the server if spawned without one
				if (atLeast1_13 && ((Particle) this.bukkitEnumParticle).getDataType() != Void.class)
					return;

				player.spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra);

			} else
				player.spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra, data);

		} else if (this.packetConstructor != null)
			if (data == null || data.length == 0)
				Remain.sendPacket(player, this.preparePacket(location.getX(), location.getY(), location.getZ(), offsetX, offsetY, offsetZ, speed, count, extra));
			else
				Remain.sendPacket(player, this.preparePacket(location.getX(), location.getY(), location.getZ(), offsetX, offsetY, offsetZ, speed, count, extra, data));
	}

	/*
	 * Resolves a compatible particle packet using Foundation fast cached reflection methods
	 */
	@SneakyThrows
	private Object preparePacket(final double posX, final double posY, final double posZ, final double offsetX, final double offsetY, final double offsetZ, final double speed, final int count, final double extra, int... data) {

		if (data == null)
			data = new int[] { 1, 0 };

		if (this == BLOCK_CRACK) {
			int materialId = 0;
			int materialData = 0;

			if (data.length > 0)
				materialId = data[0];

			if (data.length > 1)
				materialData = data[1];

			data = new int[] { materialId, materialId | materialData << 12 };
		}

		return ReflectionUtil.instantiate(this.packetConstructor, this.nmsEnumParticle, true, (float) posX, (float) posY, (float) posZ, (float) offsetX, (float) offsetY, (float) offsetZ, (float) speed, count, data);
	}

	/**
	 * Return the particle from the given name
	 *
	 * @param name
	 * @return
	 */
	public static CompParticle fromName(String name) {
		name = name.toUpperCase();

		for (final CompParticle particle : values())
			if (particle.name().equals(name) || particle.nameLegacy != null && particle.nameLegacy.equals(name) || particle.nameModern != null && particle.nameModern.equals(name))
				return particle;

		throw new IllegalArgumentException("Unknown CompParticle from name: " + name);
	}
}