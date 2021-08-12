package org.mineacademy.fo.remain;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.remain.internal.ParticleInternals;

import lombok.Getter;

/**
 * Wrapper for {@link Particle}
 */
public enum CompParticle {

	EXPLOSION_NORMAL,
	EXPLOSION_LARGE,
	EXPLOSION_HUGE,
	FIREWORKS_SPARK,
	WATER_BUBBLE,
	WATER_SPLASH,
	WATER_WAKE,
	SUSPENDED,
	SUSPENDED_DEPTH,
	CRIT,
	CRIT_MAGIC,
	SMOKE_NORMAL,
	SMOKE_LARGE,
	SPELL,
	SPELL_INSTANT,
	SPELL_MOB,
	SPELL_MOB_AMBIENT,
	SPELL_WITCH,
	DRIP_WATER,
	DRIP_LAVA,
	VILLAGER_ANGRY,
	VILLAGER_HAPPY,
	TOWN_AURA,
	NOTE,
	PORTAL,
	ENCHANTMENT_TABLE,
	FLAME,
	LAVA,
	FOOTSTEP,
	CLOUD,
	REDSTONE,
	SNOWBALL,
	SNOW_SHOVEL,
	SLIME,
	HEART,
	BARRIER,
	ITEM_CRACK,
	BLOCK_CRACK,
	BLOCK_DUST,
	WATER_DROP,
	ITEM_TAKE,
	MOB_APPEARANCE,
	DRAGON_BREATH,
	END_ROD,
	DAMAGE_INDICATOR,
	SWEEP_ATTACK,
	FALLING_DUST,
	TOTEM,
	SPIT;

	// Hardcoded for best performance
	private static final boolean hasNewMaterials = MinecraftVersion.atLeast(V.v1_13);

	/**
	 * Internal use
	 *
	 * @deprecated use {@link #spawnWithData(Location, CompMaterial)} instead
	 */
	@Deprecated
	@Getter
	private MaterialData data;

	/**
	 * Internal use
	 *
	 * @param data
	 * @return
	 * @deprecated use {@link #spawnWithData(Location, CompMaterial)} instead
	 */
	@Deprecated
	public CompParticle setWoolData(final int data) {
		this.data = new MaterialData(CompMaterial.WHITE_WOOL.getMaterial(), (byte) data);

		return this;
	}

	/**
	 * Internal use
	 *
	 * @param material
	 * @param data
	 * @return
	 * @deprecated use {@link #spawnWithData(Location, CompMaterial)} instead
	 */
	@Deprecated
	public CompParticle setData(final Material material, final int data) {
		this.data = new MaterialData(material, (byte) data);

		return this;
	}

	/**
	 * Spawns the particle at the given location
	 *
	 * @param location the location where to spawn
	 */
	public final void spawn(final Location location) {
		spawn(location, (Double) null);
	}

	/**
	 * Spawns the particle at the given location with extra data
	 *
	 * @param location
	 * @param extra
	 */
	public final void spawn(final Location location, final Double extra) {
		if (Remain.hasParticleAPI()) {
			final org.bukkit.Particle particle = ReflectionUtil.lookupEnumSilent(org.bukkit.Particle.class, toString());

			if (particle != null) {
				if (MinecraftVersion.atLeast(V.v1_13))
					if (particle.getDataType() == org.bukkit.block.data.BlockData.class) {
						org.bukkit.block.data.BlockData opt = org.bukkit.Material.END_ROD.createBlockData(); // GRAVEL

						if (data != null)
							opt = Bukkit.getUnsafe().fromLegacy(data.getItemType(), data.getData());

						location.getWorld().spawnParticle(particle, location, 1, 0D, 0D, 0D, extra != null ? extra : 0D, opt);
						return;
					}

				location.getWorld().spawnParticle(particle, location, 1, 0D, 0D, 0D, extra != null ? extra : 0D);
			}
		} else {
			final ParticleInternals particle = ReflectionUtil.lookupEnumSilent(ParticleInternals.class, toString());

			if (particle != null)
				particle.send(location, extra != null ? extra.floatValue() : 0F);
		}
	}

	public final void spawnWithMaterial(final Location location, final CompMaterial extra) {
		if (Remain.hasParticleAPI()) {
			final org.bukkit.Particle particle = ReflectionUtil.lookupEnumSilent(org.bukkit.Particle.class, toString());

			if (particle != null) {
				if (MinecraftVersion.atLeast(V.v1_13))
					if (particle.getDataType() == org.bukkit.block.data.BlockData.class) {
						final org.bukkit.block.data.BlockData opt = extra.toMaterial().createBlockData(); // GRAVEL

						location.getWorld().spawnParticle(particle, location, 1, 0D, 0D, 0D, 0D, opt);
						return;
					}

				location.getWorld().spawnParticle(particle, location, 1, 0D, 0D, 0D, extra != null ? extra : 0D);
			}
		} else {
			final ParticleInternals particle = ReflectionUtil.lookupEnumSilent(ParticleInternals.class, toString());

			if (particle != null)
				particle.send(location, extra != null ? extra.getData() : 0F);
		}
	}

	/**
	 * Spawns the particle at the given location with extra material data
	 *
	 * @param location
	 * @param data
	 */
	public final void spawnWithData(final Location location, final CompMaterial data) {
		if (Remain.hasParticleAPI()) {
			final org.bukkit.Particle particle = ReflectionUtil.lookupEnumSilent(org.bukkit.Particle.class, toString());

			if (particle != null)
				if (hasNewMaterials)
					location.getWorld().spawnParticle(particle, location, 1, data.getMaterial().createBlockData());
				else
					location.getWorld().spawnParticle(particle, location, 1, data.getMaterial().getNewData(data.getData()));

		} else {
			final ParticleInternals particle = ReflectionUtil.lookupEnumSilent(ParticleInternals.class, toString());

			if (particle != null)
				particle.sendColor(location, DyeColor.getByWoolData(data.getData()).getColor());
		}
	}

	/**
	 * Spawns the particle at the given location only visible for the given player
	 *
	 * @param player
	 * @param location
	 */
	public final void spawnFor(final Player player, final Location location) {
		spawnFor(player, location, null);
	}

	/**
	 * Spawns the particle at the given location only visible for the given player
	 * adding additional extra data
	 *
	 * @param player
	 * @param location
	 * @param extra
	 */
	public final void spawnFor(final Player player, final Location location, final Double extra) {
		if (Remain.hasParticleAPI()) {
			final org.bukkit.Particle particle = ReflectionUtil.lookupEnumSilent(org.bukkit.Particle.class, toString());

			if (particle != null)
				player.spawnParticle(particle, location, 1, 0D, 0D, 0D, extra != null ? extra : 0D);

		} else {
			final ParticleInternals p = ReflectionUtil.lookupEnumSilent(ParticleInternals.class, toString());

			if (p != null)
				p.send(player, location, extra != null ? extra.floatValue() : 0F);
		}
	}
}