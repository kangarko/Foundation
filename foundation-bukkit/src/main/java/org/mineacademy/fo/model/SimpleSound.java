package org.mineacademy.fo.model;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.platform.BukkitPlayer;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.CompSound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * A class holding a sound, volume and a pitch.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimpleSound implements ConfigStringSerializable {

	/**
	 * The Bukkit sound value
	 */
	@NonNull
	private Sound sound;

	/**
	 * The volume value
	 */
	private float volume = 1.0F;

	/**
	 * The pitch value
	 */
	private float pitch = 1.0F;

	/**
	 * Is the pitch random?
	 */
	private boolean randomPitch = false;

	/**
	 * Is this sound enabled?
	 */
	private boolean enabled = true;

	/**
	 * Create a new sound
	 *
	 * @param sound
	 * @param volume
	 * @param pitch
	 */
	public SimpleSound(Sound sound, float volume, float pitch) {
		this(sound, volume, pitch, false, true);
	}

	/**
	 * Create a new sound with a random pitch
	 *
	 * @param sound
	 * @param volume
	 */
	public SimpleSound(Sound sound, float volume) {
		this(sound, volume, 1.0F, true, true);
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param players
	 */
	public void play(Iterable<Player> players) {
		if (this.enabled)
			for (final Player player : players)
				this.play(player);
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param audience
	 */
	public void play(FoundationPlayer audience) {
		this.play(((BukkitPlayer) audience).getPlayer());
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param player
	 */
	public void play(Player player) {
		if (this.enabled) {
			Valid.checkNotNull(this.sound);

			try {
				player.playSound(player.getLocation(), this.sound, this.volume, this.getPitch());
			} catch (final NoSuchMethodError err) {
				// Legacy MC
			}
		}
	}

	/**
	 * Play the sound at the given location
	 *
	 * @param location
	 */
	public void play(Location location) {
		if (this.enabled) {
			Valid.checkNotNull(this.sound);

			try {
				location.getWorld().playSound(location, this.sound, this.volume, this.getPitch());
			} catch (final NoSuchMethodError err) {
				// Legacy MC
			}
		}
	}

	/**
	 * Return the pitch or random if {@link #isRandomPitch()} is true
	 *
	 * @return
	 */
	public float getPitch() {
		return this.randomPitch ? (float) Math.random() : this.pitch;
	}

	/**
	 * @return
	 */
	@Override
	public String serialize() {
		return this.enabled ? this.sound + " " + this.volume + " " + (this.randomPitch ? "random" : this.pitch) : "none";
	}

	/**
	 * Returns a serialized sound, does not support random pitch
	 */
	@Override
	public String toString() {
		return this.serialize();
	}

	/**
	 * Create a new sound from the raw line,
	 * we accept the plain SOUND name or the following syntax: SOUND VOLUME PITCH
	 * for example: ENTITY_PLAYER_HURT 1.0F 1.0F
	 * <p>
	 * Set to 'none' to disable
	 *
	 * @param line
	 * @return
	 */
	public static SimpleSound fromString(String line) {
		if ("none".equals(line))
			return new SimpleSound(CompSound.CLICK.getSound(), 0.0F, 1.0F, false, false);

		final String[] values = line.contains(", ") ? line.split(", ") : line.split(" ");
		final CompSound compSound = CompSound.fromName(values[0]);

		final SimpleSound sound = new SimpleSound();

		Valid.checkNotNull(compSound, "Sound '" + values[0] + "' does not exists (in your Minecraft version " + MinecraftVersion.getFullVersion() + ")! Pick one from mineacademy.org/sounds");
		sound.sound = compSound.getSound();

		if (values.length == 1) {
			sound.volume = 1F;
			sound.pitch = 1.5F;

			return sound;
		}

		Valid.checkBoolean(values.length == 3, "Malformed sound type, use format: 'sound' OR 'sound volume pitch'. Got: " + line);

		final String volumeRaw = values[1];
		final String pitchRaw = values[2];

		sound.volume = Float.parseFloat(volumeRaw);

		if ("random".equals(pitchRaw)) {
			sound.pitch = 1.0F;
			sound.randomPitch = true;
		}

		else
			sound.pitch = Float.parseFloat(pitchRaw);

		return sound;
	}
}