package org.mineacademy.fo.model;

import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.CompSound;

import lombok.Getter;
import lombok.NonNull;

/**
 * A class holding a sound, volume and a pitch.
 */
@Getter
public final class SimpleSound implements ConfigStringSerializable {

	/**
	 * The Bukkit sound value
	 */
	@NonNull
	private final CompSound sound;

	/**
	 * The volume value
	 */
	private final float volume;

	/**
	 * The pitch value
	 */
	private final float pitch;

	/**
	 * Is the pitch random?
	 */
	private final boolean randomPitch;

	/**
	 * Is this sound enabled?
	 */
	private final boolean enabled;

	/**
	 * Is this sound custom?
	 */
	private final String customSoundName;

    private SimpleSound(@NonNull final CompSound sound, final float volume, final float pitch, final boolean randomPitch, final boolean enabled, final String customSoundName) {

        if (!Float.isFinite(volume) || !Float.isFinite(pitch))
            throw new FoException("Sound volume and pitch must be finite. Got: " + volume + " " + pitch, false);

        this.sound           = sound;
        this.volume          = volume;
        this.pitch           = pitch;
        this.randomPitch     = randomPitch;
        this.enabled         = enabled;
        this.customSoundName = customSoundName;
    }

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

        if (!this.enabled)
            return;

        if (this.customSoundName != null)
            player.playSound(player.getLocation(), this.customSoundName, this.volume, this.getPitch());
        else
            player.playSound(player.getLocation(), this.sound.getSound(), this.volume, this.getPitch());
    }

	/**
	 * Play the sound at the given location
	 *
	 * @param location
	 */
    public void play(final Location location) {

        if (!this.enabled)
            return;

        if (this.customSoundName != null)
            location.getWorld().playSound(location, this.customSoundName, this.volume, this.getPitch());
        else
            location.getWorld().playSound(location, this.sound.getSound(), this.volume, this.getPitch());
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

        final String[] values = line.contains(", ") ? line.split(", ", -1) : line.split(" ");

        if (values.length != 1 && values.length != 3)
            throw new FoException("Malformed sound type, use format: 'sound' OR 'sound volume pitch'. Got: " + line, false);

        final String soundNameRaw = values[0];

		// Check if the sound is custom
        final boolean customSound = soundNameRaw.toLowerCase(Locale.ROOT).startsWith("custom:");

		// If the sound is custom, we strip the "custom:" prefix
        String soundName = customSound ? soundNameRaw.substring("custom:".length()) : soundNameRaw.toUpperCase(Locale.ROOT);

        if (customSound) {

            if (!soundName.contains(":"))
                soundName = "minecraft:" + soundName;

            if (!soundName.matches("[a-zA-Z0-9._-]+:[a-zA-Z0-9/._-]+"))
                throw new FoException("Malformed custom sound '" + values[0] + "', use custom:namespace:sound with lowercase letters, digits, '.', '_' or '-', and '/' only in the sound path.", false);

            soundName = soundName.toLowerCase(Locale.ROOT);
        }

		// If the sound is not custom, we try to get the CompSound from the name
		CompSound compSound = customSound ? null : CompSound.fromName(soundName);

		if (compSound == null && !customSound)
			throw new FoException("Sound '" + values[0] + "' does not exists (in your Minecraft version " + MinecraftVersion.getFullVersion() + ")! Pick one from mineacademy.org/sounds", false);

		// If the sound is custom and does not exist, we set it to a dummy sound
		else if (compSound == null)
			compSound = CompSound.UI_BUTTON_CLICK; // Dummy sound for custom sounds

        if (values.length == 1)
            return new SimpleSound(compSound, 1F, 1.5F, false, true, customSound ? soundName : null);

        final boolean randomPitch = "random".equals(values[2]);
        final float volume;
        final float pitch;

        try {
            volume = Float.parseFloat(values[1]);
            pitch  = randomPitch ? 1.0F : Float.parseFloat(values[2]);

        } catch (final NumberFormatException ex) {
            throw new FoException("Malformed sound volume or pitch, use finite numbers or 'random' for pitch. Got: " + line, false);
        }

        return new SimpleSound(compSound, volume, pitch, randomPitch, true, customSound ? soundName : null);
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
