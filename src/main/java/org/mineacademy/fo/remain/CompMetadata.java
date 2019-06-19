package org.mineacademy.fo.remain;

import java.util.Objects;

import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * Utility class for persistent metadata manipulation
 *
 * We apply scoreboard tags to ensure permanent metadata storage
 * if supported, otherwise it is lost on reload
 */
public class CompMetadata {

	/**
	 * The tag delimiter
	 */
	private final static String DELIMITER = "%-%";

	// Static access
	private CompMetadata() {
	}

	/**
	 * Attempts to set a persistent metadata for entity
	 *
	 * @param entity
	 * @param tag
	 */
	public static final void setMetadata(Entity entity, String tag) {
		setMetadata(entity, tag, "null");
	}

	/**
	 * Attempts to set a persistent metadata tag with value for entity
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	public static final void setMetadata(Entity entity, String key, String value) {
		Objects.requireNonNull(entity);

		final String tag = format(key, value);

		try {
			if (!entity.getScoreboardTags().contains(tag))
				entity.addScoreboardTag(tag);

		} catch (NoSuchMethodError | NoSuchFieldError ex) {

			// Lost upon reload
			entity.setMetadata(key, new FixedMetadataValue(SimplePlugin.getInstance(), tag));
		}
	}

	// Format the syntax of stored tags
	private static final String format(String key, String value) {
		return SimplePlugin.getNamed() + DELIMITER + key + DELIMITER + value;
	}

	/**
	 * Attempts to get the entity's metadata, first from scoreboard tag,
	 * second from Bukkit metadata
	 *
	 * @param entity
	 * @param key
	 * @return the tag, or null
	 */
	public static final String getMetadata(Entity entity, String key) {
		Objects.requireNonNull(entity);

		try {
			for (final String line : entity.getScoreboardTags()) {
				final String tag = getTag(line, key);

				if (tag != null)
					return tag;
			}

		} catch (NoSuchMethodError | NoSuchFieldError ex) {
			if (entity.hasMetadata(key))
				return getTag(entity.getMetadata(key).get(0).asString(), key);
		}

		return null;
	}

	// Parses the tag and gets its value
	private static final String getTag(String raw, String key) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(key) ? parts[2] : null;
	}

	/**
	 * Returns if the entity has the given tag by key, first checks scoreboard tags,
	 * and then bukkit metadata
	 *
	 * @param entity
	 * @param key
	 * @return
	 */
	public static final boolean hasMetadata(Entity entity, String key) {
		Objects.requireNonNull(entity);

		try {
			for (final String line : entity.getScoreboardTags())
				if (hasTag(line, key))
					return true;

		} catch (NoSuchMethodError | NoSuchFieldError ex) {
			return entity.hasMetadata(key);
		}

		return false;
	}

	// Parses the tag and gets its value
	private static final boolean hasTag(String raw, String tag) {
		final String[] parts = raw.split(DELIMITER);

		return parts.length == 3 && parts[0].equals(SimplePlugin.getNamed()) && parts[1].equals(tag);
	}
}
