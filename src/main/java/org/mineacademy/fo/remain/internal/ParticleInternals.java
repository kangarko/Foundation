package org.mineacademy.fo.remain.internal;

import java.lang.reflect.Constructor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.remain.Remain;

/**
 * Reflection class to support packet sending of particles
 *
 * @deprecated internal use only, please use {@link Remain}
 * to call methods from this class for best performance
 */
@Deprecated
public enum ParticleInternals {

	HUGE_EXPLOSION("hugeexplosion", "EXPLOSION_HUGE"),
	LARGE_EXPLODE("largeexplode", "EXPLOSION_LARGE"),
	BUBBLE("bubble", "WATER_BUBBLE"),
	SUSPEND("suspended", "SUSPENDED"),
	DEPTH_SUSPEND("depthsuspend", "SUSPENDED_DEPTH"),
	MAGIC_CRIT("magicCrit", "CRIT_MAGIC"),
	MOB_SPELL("mobSpell", "SPELL_MOB", true),
	MOB_SPELL_AMBIENT("mobSpellAmbient", "SPELL_MOB_AMBIENT"),
	INSTANT_SPELL("instantSpell", "SPELL_INSTANT"),
	WITCH_MAGIC("witchMagic", "SPELL_WITCH"),
	EXPLODE("explode", "EXPLOSION_NORMAL"),
	SPLASH("splash", "WATER_SPLASH"),
	LARGE_SMOKE("largesmoke", "SMOKE_LARGE"),
	RED_DUST("reddust", "REDSTONE", true),
	SNOWBALL_POOF("snowballpoof", "SNOWBALL"),
	ANGRY_VILLAGER("angryVillager", "VILLAGER_ANGRY"),
	HAPPY_VILLAGER("happyVillager", "VILLAGER_HAPPY"),
	EXPLOSION_NORMAL(ParticleInternals.EXPLODE.name),
	EXPLOSION_LARGE(ParticleInternals.LARGE_EXPLODE.name),
	EXPLOSION_HUGE(ParticleInternals.HUGE_EXPLOSION.name),
	FIREWORKS_SPARK("fireworksSpark"),
	WATER_BUBBLE(ParticleInternals.BUBBLE.name),
	WATER_SPLASH(ParticleInternals.SPLASH.name),
	WATER_WAKE("wake"),
	SUSPENDED(ParticleInternals.SUSPEND.name),
	SUSPENDED_DEPTH(ParticleInternals.DEPTH_SUSPEND.name),
	CRIT("crit"),
	CRIT_MAGIC(ParticleInternals.MAGIC_CRIT.name),
	SMOKE_NORMAL("smoke"),
	SMOKE_LARGE(ParticleInternals.LARGE_SMOKE.name),
	SPELL("spell"),
	SPELL_INSTANT(ParticleInternals.INSTANT_SPELL.name),
	SPELL_MOB(ParticleInternals.MOB_SPELL.name, true),
	SPELL_MOB_AMBIENT(ParticleInternals.MOB_SPELL_AMBIENT.name),
	SPELL_WITCH(ParticleInternals.WITCH_MAGIC.name),
	DRIP_WATER("dripWater"),
	DRIP_LAVA("dripLava"),
	VILLAGER_ANGRY(ParticleInternals.ANGRY_VILLAGER.name),
	VILLAGER_HAPPY(ParticleInternals.HAPPY_VILLAGER.name),
	TOWN_AURA("townaura"),
	NOTE("note", true),
	PORTAL("portal"),
	ENCHANTMENT_TABLE("enchantmenttable"),
	FLAME("flame"),
	LAVA("lava"),
	FOOTSTEP("footstep"),
	CLOUD("cloud"),
	REDSTONE("reddust", true),
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
	MOB_APPEARANCE("mobappearance");

	//

	private static Constructor<?> particlePacketConstructor;
	private static Class<?> nmsEnumParticle;

	static {

		// Minecraft 1.6.4 has no particle packet, MC 1.12+ has native compatibility
		if (MinecraftVersion.atLeast(V.v1_7) && MinecraftVersion.olderThan(V.v1_12)) {

			final Class<?> particlePacketClass = ReflectionUtil.getNMSClass("PacketPlayOutWorldParticles", "net.minecraft.network.protocol.game.PacketPlayOutWorldParticles");

			try {
				if (MinecraftVersion.atLeast(V.v1_8)) {
					nmsEnumParticle = ReflectionUtil.getNMSClass("EnumParticle", "N/A");

					particlePacketConstructor = particlePacketClass
							.getConstructor(nmsEnumParticle, Boolean.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, int[].class);
				}

				else {
					particlePacketConstructor = particlePacketClass
							.getConstructor(String.class, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE);
				}

			} catch (final ReflectiveOperationException ex) {
				// Fail silently

				// TODO remove
				ex.printStackTrace();
			}
		}
	}

	//

	private String name;
	private boolean hasColor;

	private ParticleInternals(final String particleName, final String enumValue, final boolean hasColor) {
		this.name = particleName;
		//this.enumValue = enumValue;
		this.hasColor = hasColor;
	}

	private ParticleInternals(final String particleName, final String enumValue) {
		this(particleName, enumValue, false);
	}

	private ParticleInternals(final String particleName) {
		this(particleName, null);
	}

	private ParticleInternals(final String particleName, final boolean hasColor) {
		this(particleName, null, hasColor);
	}

	/**
	 * Send a particle to player
	 *
	 * @param loc
	 * @param speed
	 */
	public void send(final Location loc, final float speed) {
		for (final Player player : loc.getWorld().getPlayers())
			this.send(player, loc, 0, 0, 0, speed, 1);
	}

	/**
	 * Send a particle to player
	 *
	 * @param player
	 * @param location
	 * @param speed
	 */
	public void send(final Player player, final Location location, final float speed) {
		this.send(player, location, 0, 0, 0, speed, 1);
	}

	/**
	 * Send a particle to player
	 *
	 * @param player
	 * @param location
	 * @param offsetX
	 * @param offsetY
	 * @param offsetZ
	 * @param speed
	 * @param count
	 * @param extra
	 */
	@SuppressWarnings("rawtypes")
	public void send(final Player player, final Location location, final float offsetX, final float offsetY, final float offsetZ, final float speed, final int count, int... extra) {
		if (MinecraftVersion.olderThan(V.v1_7))
			return;

		if (MinecraftVersion.atLeast(V.v1_12)) {
			player.spawnParticle(Particle.valueOf(this.name()), location, count, offsetX, offsetY, offsetZ, 1, null);

			return;
		}

		final Object packet;

		if (MinecraftVersion.atLeast(V.v1_8)) {
			if (this == ParticleInternals.BLOCK_CRACK) {
				int id = 0;
				int data = 0;

				if (extra.length > 0)
					id = extra[0];

				if (extra.length > 1)
					data = extra[1];

				extra = new int[] { id, id | data << 12 };
			}

			try {
				packet = particlePacketConstructor.newInstance(
						ReflectionUtil.lookupEnumSilent((Class<? extends Enum>) nmsEnumParticle, this.name()),
						true,
						(float) location.getX(),
						(float) location.getY(),
						(float) location.getZ(),
						offsetX,
						offsetY,
						offsetZ,
						speed,
						count,
						extra);

			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();

				return;
			}

		} else {
			if (this.name == null)
				this.name = this.name().toLowerCase();

			String name = this.name;

			if (this == ParticleInternals.BLOCK_CRACK || this == ParticleInternals.ITEM_CRACK || this == ParticleInternals.BLOCK_DUST) {
				int id2 = 0;
				int data2 = 0;

				if (extra.length > 0)
					id2 = extra[0];

				if (extra.length > 1)
					data2 = extra[1];

				name = name + id2 + "_" + data2;
			}

			try {
				packet = particlePacketConstructor.newInstance(
						name,
						(float) location.getX(),
						(float) location.getY(),
						(float) location.getZ(),
						offsetX,
						offsetY,
						offsetZ,
						speed,
						count);

			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();

				return;
			}
		}

		Remain.sendPacket(player, packet);
	}

	/**
	 * Send a colored particle to player
	 *
	 * @param loc
	 * @param color
	 */
	public void sendColor(final Location loc, final Color color) {
		for (final Player player : loc.getWorld().getPlayers())
			this.sendColor(player, loc, color);
	}

	/**
	 * Send a colored particle to player
	 *
	 * @param player
	 * @param location
	 * @param color
	 */
	public void sendColor(final Player player, final Location location, final Color color) {
		if (this.hasColor) {
			final float red = color.getRed() == 0 ? Float.MIN_VALUE : color.getRed() / 255F;
			final float green = color.getGreen() / 255F;
			final float blue = color.getBlue() / 255F;

			this.send(player, location, red, green, blue, 1.0f, 0);
		}
	}
}
