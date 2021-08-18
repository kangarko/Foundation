package org.mineacademy.fo.remain;

import java.lang.reflect.Constructor;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;

import lombok.SneakyThrows;

/**
 * Wrapper for {@link Particle}
 */
public enum CompParticle {

	EXPLOSION_NORMAL("explode"),
	EXPLOSION_LARGE("largeexplode"),
	EXPLOSION_HUGE("hugeexplosion"),
	FIREWORKS_SPARK("fireworksSpark"),
	WATER_BUBBLE("bubble"),
	WATER_SPLASH("splash"),
	WATER_WAKE("wake"),
	SUSPENDED("suspend"),
	SUSPENDED_DEPTH("depthsuspend"),
	CRIT("crit"),
	CRIT_MAGIC("magicCrit"),
	SMOKE_NORMAL("smoke"),
	SMOKE_LARGE("largesmoke"),
	SPELL("spell"),
	SPELL_INSTANT("instantSpell"),
	SPELL_MOB("mobSpell"),
	SPELL_MOB_AMBIENT("mobSpellAmbient"),
	SPELL_WITCH("witchMagic"),
	DRIP_WATER("dripWater"),
	DRIP_LAVA("dripLava"),
	VILLAGER_ANGRY("angryVillager"),
	VILLAGER_HAPPY("happyVillager"),
	TOWN_AURA("townaura"),
	NOTE("note"),
	PORTAL("portal"),
	ENCHANTMENT_TABLE("enchantmenttable"),
	FLAME("flame"),
	LAVA("lava"),
	FOOTSTEP("footstep"),
	CLOUD("cloud"),
	REDSTONE("reddust"),
	SNOWBALL("snowballpoof"),
	SNOW_SHOVEL("snowshovel"),
	SLIME("slime"),
	HEART("heart"),
	BARRIER("barrier"),
	ITEM_CRACK("iconcrack_"),
	BLOCK_CRACK("blockcrack_"),
	BLOCK_DUST("blockdust_"),
	WATER_DROP("droplet"),
	ITEM_TAKE("take"),
	MOB_APPEARANCE("mobappearance"),

	// MC 1.8.8+
	DRAGON_BREATH,
	END_ROD,
	DAMAGE_INDICATOR,
	SWEEP_ATTACK,
	FALLING_DUST,
	TOTEM,
	SPIT,
	SQUID_INK,
	BUBBLE_POP,
	CURRENT_DOWN,
	BUBBLE_COLUMN_UP,
	NAUTILUS,
	DOLPHIN,
	SNEEZE,
	CAMPFIRE_COSY_SMOKE,
	CAMPFIRE_SIGNAL_SMOKE,
	COMPOSTER,
	FLASH,
	FALLING_LAVA,
	LANDING_LAVA,
	FALLING_WATER,
	DRIPPING_HONEY,
	FALLING_HONEY,
	LANDING_HONEY,
	FALLING_NECTAR,
	SOUL_FIRE_FLAME,
	ASH,
	CRIMSON_SPORE,
	WARPED_SPORE,
	SOUL,
	DRIPPING_OBSIDIAN_TEAR,
	FALLING_OBSIDIAN_TEAR,
	LANDING_OBSIDIAN_TEAR,
	REVERSE_PORTAL,
	WHITE_ASH,
	LIGHT,
	DUST_COLOR_TRANSITION,
	VIBRATION,
	FALLING_SPORE_BLOSSOM,
	SPORE_BLOSSOM_AIR,
	SMALL_FLAME,
	SNOWFLAKE,
	DRIPPING_DRIPSTONE_LAVA,
	FALLING_DRIPSTONE_LAVA,
	DRIPPING_DRIPSTONE_WATER,
	FALLING_DRIPSTONE_WATER,
	GLOW_SQUID_INK,
	GLOW,
	WAX_ON,
	WAX_OFF,
	ELECTRIC_SPARK,
	SCRAPE;

	/**
	 * Hardcoded values for best performance
	 */
	private static final boolean atLeast1_13 = MinecraftVersion.atLeast(V.v1_13);
	private static final boolean atLeast1_12 = MinecraftVersion.atLeast(V.v1_12);
	private static final boolean atLeast1_8 = MinecraftVersion.atLeast(V.v1_8);

	/**
	 * The name for Minecraft 1.7.10
	 */
	private final String name1_7;

	/**
	 * The NMS EnumParticle class cache here for top performance
	 */
	@Nullable
	private final Object nmsEnumParticle;

	/**
	 * The bukkit particle object, if this is running on a new MC version, cached for top performance
	 */
	@Nullable
	private final Object bukkitEnumParticle;

	/**
	 * The NMS packet class constructor cached here for top performance
	 */
	private final Constructor<?> packetConstructor;

	/*
	 * Construct a new compatible particle class
	 */
	private CompParticle() {
		this(null);
	}

	/*
	 * Construct a new compatible particle class
	 */
	@SuppressWarnings("rawtypes")
	private CompParticle(String name1_7) {
		this.name1_7 = name1_7;

		try {

			// Minecraft <1.7 lacks that class, Minecraft >1.13 already has native API
			if (MinecraftVersion.atLeast(V.v1_7) && MinecraftVersion.olderThan(V.v1_13)) {

				final Class<?> packetClass = ReflectionUtil.getNMSClass("PacketPlayOutWorldParticles", "net.minecraft.network.protocol.game.PacketPlayOutWorldParticles");

				if (MinecraftVersion.equals(V.v1_7)) {
					this.nmsEnumParticle = null;
					this.packetConstructor = ReflectionUtil.getConstructor(packetClass, String.class, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE);
				}

				else {
					final Class<? extends Enum> particleClass = (Class<? extends Enum>) ReflectionUtil.getNMSClass("EnumParticle");

					this.nmsEnumParticle = ReflectionUtil.lookupEnumSilent(particleClass, this.name());

					// MC 1.8.8 or newer but lacks this given particle type
					if (this.nmsEnumParticle == null)
						this.packetConstructor = null;
					else
						this.packetConstructor = ReflectionUtil.getConstructor(packetClass, particleClass, Boolean.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, int[].class);
				}

			} else {
				this.nmsEnumParticle = null;
				this.packetConstructor = null;
			}

			if (MinecraftVersion.atLeast(V.v1_12)) {
				final Class<Enum> particleClass = ReflectionUtil.lookupClass("org.bukkit.Particle");

				this.bukkitEnumParticle = ReflectionUtil.lookupEnumSilent(particleClass, this.name());

			} else
				this.bukkitEnumParticle = null;

		} catch (final Throwable t) {
			t.printStackTrace();

			throw new RuntimeException("Fatal error setting up CompParticle, see above");
		}
	}

	/**
	 * Spawns this particle with the given color, only works for {@link #REDSTONE}
	 * The particle size requires MC 1.13+
	 *
	 * @param location
	 * @param color
	 * @param particleSize
	 */
	public void spawn(Location location, Color color, float particleSize) {
		Valid.checkBoolean(this == REDSTONE, "Can only send colors for REDSTONE particle, not: " + this);

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
	 * Spawns a particle at the given location
	 *
	 * @param location
	 */
	public void spawn(Location location) {
		this.spawn(location, 0, 0, 0, 0, 0, 0, null);
	}

	/**
	 * Spawns the particle at the given location with extra material data
	 *
	 * @param location
	 * @param data
	 */
	public final void spawn(Location location, CompMaterial data) {
		Valid.checkBoolean(this == ITEM_CRACK || this == BLOCK_CRACK || this == BLOCK_DUST || this == FALLING_DUST, "Can only call particle spawn with data on crack or dust particles, not: " + this);

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
	public final void spawn(Location location, double extra) {
		this.spawn(location, 0, extra);
	}

	/**
	 * Spawns a particle at the given location with given data
	 *
	 * @param location
	 * @param speed
	 * @param extra
	 */
	public final void spawn(Location location, double speed, double extra) {
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
	public void spawn(Location location, double offsetX, double offsetY, double offsetZ, double speed, int count, double extra, int... data) {

		// Minecraft 1.12 and up
		if (this.bukkitEnumParticle != null) {
			if (MinecraftVersion.atLeast(V.v1_13) && this == REDSTONE)
				location.getWorld().spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra, new DustOptions(Color.RED, 1F));
			else
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
	public void spawn(Player player, Location location) {
		this.spawn(player, location, 0d, 0d, 0d, 0d, 0, 0d, null);
	}

	/**
	 * Spawns a particle only shown to the given player at the given location
	 *
	 * @param player
	 * @param location
	 * @param extra
	 */
	public void spawn(Player player, Location location, double extra) {
		this.spawn(player, location, 0d, 0d, 0d, 0d, 0, extra, null);
	}

	/**
	 * Spawns the particle only shown to the given player at the given location with extra material data
	 *
	 * @param location
	 * @param data
	 */
	public final void spawn(Player player, Location location, CompMaterial data) {
		Valid.checkBoolean(this == ITEM_CRACK || this == BLOCK_CRACK || this == BLOCK_DUST || this == FALLING_DUST, "Can only call particle spawn with data on crack or dust particles, not: " + this);

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
	public void spawn(Player player, Location location, double offsetX, double offsetY, double offsetZ, double speed, int count, double extra, int... data) {

		// Minecraft 1.12 and up
		if (this.bukkitEnumParticle != null && this != REDSTONE)
			player.spawnParticle((Particle) this.bukkitEnumParticle, location, count, offsetX, offsetY, offsetZ, extra, data);

		else if (this.packetConstructor != null)
			Remain.sendPacket(player, this.preparePacket(location.getX(), location.getY(), location.getZ(), offsetX, offsetY, offsetZ, speed, count, extra, data));
	}

	/*
	 * Resolves a compatible particle packet using Foundation fast cached reflection methods
	 */
	@SneakyThrows
	private Object preparePacket(double posX, double posY, double posZ, double offsetX, double offsetY, double offsetZ, double speed, int count, double extra, int... data) {

		if (atLeast1_8) {

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

		else {
			String correctedName = this.name1_7;

			if (this == BLOCK_CRACK || this == ITEM_CRACK || this == BLOCK_DUST) {
				int materialId = 0;
				int materialData = 0;

				if (data.length > 0)
					materialId = data[0];

				if (data.length > 1)
					materialData = data[1];

				correctedName = correctedName + materialId + "_" + materialData;
			}

			return ReflectionUtil.instantiate(this.packetConstructor, correctedName, (float) posX, (float) posY, (float) posZ, (float) offsetX, (float) offsetY, (float) offsetZ, (float) speed, count);
		}
	}
}