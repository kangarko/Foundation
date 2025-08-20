package org.mineacademy.fo.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.FoException;
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
	private CompSound sound;

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
	 * Is this sound custom?
	 */
	private String customSoundName;

	private SimpleSound(final CompSound sound, final float volume, final float pitch) {
		this(sound, volume, pitch, false, true, null);
	}

	private SimpleSound(final CompSound sound, final float volume) {
		this(sound, volume, 1.0F, true, true, null);
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param players
	 */
	public void play(final Iterable<Player> players) {
		if (this.enabled)
			for (final Player player : players)
				this.play(player);
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param audience
	 */
	public void play(final FoundationPlayer audience) {
		if (audience.isPlayer())
			this.play((Player) audience.getPlayer());
	}

	/**
	 * Play the sound to the given player
	 *
	 * @param player
	 */
	public void play(final Player player) {
		if (this.enabled) {

			try {
				if(this.customSoundName != null)
					player.playSound(player.getLocation(), this.customSoundName, this.volume, this.getPitch());
				else {
					ValidCore.checkNotNull(this.sound);
					player.playSound(player.getLocation(), this.sound.getSound(), this.volume, this.getPitch());
				}
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
	public void play(final Location location) {
		if (this.enabled) {

			try {
				if(this.customSoundName != null)
					location.getWorld().playSound(location, this.customSoundName, this.volume, this.getPitch());
				else {
					ValidCore.checkNotNull(this.sound);
					location.getWorld().playSound(location, this.sound.getSound(), this.volume, this.getPitch());
				}
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
		return this.enabled ? (this.customSoundName != null ? "custom:" + this.customSoundName : this.sound) + " " + this.volume + " " + (this.randomPitch ? "random" : this.pitch) : "none";
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
	public static SimpleSound fromString(final String line) {
		if ("none".equals(line) || "".equals(line))
			return new SimpleSound(CompSound.UI_BUTTON_CLICK, 0.0F, 1.0F, false, false, null);

		final String[] values = line.contains(", ") ? line.split(", ") : line.split(" ");

		final String soundNameRaw = values[0].toLowerCase();

		// Check if the sound is custom
		final boolean customSound = soundNameRaw.startsWith("custom:");

		// If the sound is custom, we strip the "custom:" prefix
		String soundName = customSound ? soundNameRaw.substring("custom:".length()) : soundNameRaw.toUpperCase();

		// If the sound is custom and does not start with "minecraft:", we prefix it with "minecraft:"
		if(customSound && !soundName.startsWith("minecraft:"))
			soundName = "minecraft:" + soundName; // Ensure custom sounds are prefixed with minecraft:

		// If the sound is not custom, we try to get the CompSound from the name
		CompSound compSound = customSound ? null : CompSound.fromName(soundName);

		if (compSound == null && !customSound)
			throw new FoException("Sound '" + values[0] + "' does not exists (in your Minecraft version " + MinecraftVersion.getFullVersion() + ")! Pick one from mineacademy.org/sounds", false);

		// If the sound is custom and does not exist, we set it to a dummy sound
		else if(compSound == null)
			compSound = CompSound.UI_BUTTON_CLICK; // Dummy sound for custom sounds

		final SimpleSound sound = new SimpleSound();

		sound.sound = compSound;
		if(customSound)
			sound.customSoundName = soundName;

		if (values.length == 1) {
			sound.volume = 1F;
			sound.pitch = 1.5F;

			return sound;
		}

		if (values.length != 3)
			throw new FoException("Malformed sound type, use format: 'sound' OR 'sound volume pitch'. Got: " + line, false);

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

	/**
	 * Create a new sound
	 *
	 * @param sound
	 * @param volume
	 * @param pitch
	 *
	 * @return
	 */
	public static SimpleSound fromSound(@NonNull final CompSound sound, final float volume, final float pitch) {
		return new SimpleSound(sound, volume, pitch);
	}

	/**
	 * Create a new sound with a random pitch
	 *
	 * @param sound
	 * @param volume
	 *
	 * @return
	 */
	public static SimpleSound fromSound(@NonNull final CompSound sound, final float volume) {
		return new SimpleSound(sound, volume);
	}
}