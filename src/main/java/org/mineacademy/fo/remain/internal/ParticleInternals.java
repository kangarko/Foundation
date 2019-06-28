package org.mineacademy.fo.remain.internal;

import org.bukkit.Color;
import org.bukkit.Location;
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

	private static final Class<?> nmsPacketPlayOutParticle;
	private static Class<?> nmsEnumParticle;

	static {
		nmsPacketPlayOutParticle = ReflectionUtil.getNMSClass("PacketPlayOutWorldParticles");
	}

	//

	private String name;
	private String enumValue;
	private boolean hasColor;

	private ParticleInternals(final String particleName, final String enumValue, final boolean hasColor) {
		this.name = particleName;
		this.enumValue = enumValue;
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
	public void send(final Player player, final Location location, final float offsetX, final float offsetY, final float offsetZ, final float speed, final int count, int... extra) {
		final Object packet;

		if (MinecraftVersion.equals(V.v1_8)) {
			if (nmsEnumParticle == null)
				nmsEnumParticle = ReflectionUtil.getNMSClass("EnumParticle");

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
				packet = ParticleInternals.nmsPacketPlayOutParticle.getConstructor(ParticleInternals.nmsEnumParticle, Boolean.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, int[].class).newInstance(ReflectionUtil.getEnum(ParticleInternals.nmsEnumParticle.getName() + "." + (this.enumValue != null ? this.enumValue : this.name().toUpperCase())), true, (float) location.getX(), (float) location.getY(),
						(float) location.getZ(), offsetX, offsetY, offsetZ, speed, count, extra);
			} catch (final ReflectiveOperationException ex) {
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
				packet = ParticleInternals.nmsPacketPlayOutParticle.getConstructor(String.class, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE).newInstance(name, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);
			} catch (final ReflectiveOperationException ex) {
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
		if (!this.hasColor)
			return;

		this.send(player, location, this.getColor(color.getRed()), this.getColor(color.getGreen()), this.getColor(color.getBlue()), 1.0f, 0);
	}

	private float getColor(float value) {
		if (value <= 0.0f)
			value = -1.0f;

		return value / 255.0f;
	}
}
