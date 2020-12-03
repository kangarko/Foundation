package org.mineacademy.fo.model;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompSound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * A class holding a sound, volume and a pitch
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimpleSound {

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
	 * Create a new sound from the raw line,
	 * we accept the plain SOUND name or the following syntax: SOUND VOLUME PITCH
	 * for example: ENTITY_PLAYER_HURT 1.0F 1.0F
	 * <p>
	 * Set to 'none' to disable
	 *
	 * @param line
	 */
	public SimpleSound(String line) {
		Valid.checkNotNull(line);

		if ("none".equals(line)) {
			this.sound = CompSound.CLICK.getSound();
			this.volume = 0.0F;
			this.enabled = false;

			return;
		}

		final String[] values = line.contains(", ") ? line.split(", ") : line.split(" ");

		try {
			sound = CompSound.convert(values[0]);

		} catch (final IllegalArgumentException ex) {
			Common.throwError(ex, "Sound '" + values[0] + "' does not exists (in your Minecraft version)!",
					"Notice: Sound names has changed as per 1.9. See:",
					"https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html");
		}

		if (values.length == 1) {
			volume = 1F;
			pitch = 1.5F;
			return;
		}

		Valid.checkBoolean(values.length == 3, "Malformed sound type, use format: 'sound' OR 'sound volume pitch'. Got: " + line);
		Valid.checkNotNull(sound, "Unable to parse sound from: " + line);

		final String volumeRaw = values[1];
		final String pitchRaw = values[2];

		volume = Float.parseFloat(volumeRaw);

		if ("random".equals(pitchRaw)) {
			pitch = 1.0F;
			randomPitch = true;
		}

		else
			pitch = Float.parseFloat(pitchRaw);
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param players
	 */
	public void play(Iterable<Player> players) {
		if (enabled)
			for (final Player player : players)
				play(player);
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param player
	 */
	public void play(Player player) {
		if (enabled) {
			Valid.checkNotNull(sound);

			player.playSound(player.getLocation(), sound, volume, getPitch());
		}
	}

	/**
	 * Play the sound at the given location
	 *
	 * @param location
	 */
	public void play(Location location) {
		if (enabled) {
			Valid.checkNotNull(sound);

			location.getWorld().playSound(location, sound, volume, getPitch());
		}
	}

	/**
	 * Return the pitch or random if {@link #isRandomPitch()} is true
	 *
	 * @return
	 */
	public float getPitch() {
		return randomPitch ? (float) Math.random() : pitch;
	}

	/**
	 * Returns a serialized sound, does not support random pitch
	 */
	@Override
	public String toString() {
		return enabled ? sound + " " + volume + " " + (randomPitch ? "random" : pitch) : "none";
	}
}