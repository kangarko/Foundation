package org.mineacademy.fo.settings;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.NonNull;

/**
 * Represents the internal data map a configuration section has.
 * Credits goes to the original Bukkit/Spigot team, enhanced by MineAcademy.
 */
public class ConfigSection {

	/**
	 * The data map holding keys and values of this config section. A
	 * value can be another config section.
	 */
	final Map<String, Object> map = new LinkedHashMap<>();

	/**
	 * The root of this configuration
	 */
	private final ConfigSection root;

	/**
	 * The parent of this configuration, if any.
	 */
	private final ConfigSection parent;

	/**
	 * The current path for this configuration.
	 */
	private final String path;

	/**
	 * The full path for this configuration.
	 */
	private final String fullPath;

	ConfigSection() {
		this.path = "";
		this.fullPath = "";
		this.parent = null;
		this.root = this;
	}

	ConfigSection(@NonNull ConfigSection parent, @NonNull String path) {
		this.path = path;
		this.parent = parent;
		this.root = parent.root;
		this.fullPath = createPath(parent, path);
	}

	// ------------------------------------------------------------------------------------
	// Getting values
	// ------------------------------------------------------------------------------------

	/**
	 * Gets a set containing all keys in this section.
	 *
	 * If deep is set to true, then this will contain all the keys within any child config sections (and their children, etc).
	 * These will be in a valid path notation for you to use.
	 *
	 * If deep is set to false, then this will contain only the keys of any direct children, and not their own children.
	 *
	 * @param deep
	 * @return
	 */
	@NonNull
	public final Set<String> getKeys(boolean deep) {
		final Set<String> result = new LinkedHashSet<>();
		this.mapChildrenKeys(result, this, deep);

		return result;
	}

	/**
	 * Gets a Map containing all keys and their values for this section.
	 *
	 * If deep is set to true, then this will contain all the keys and values within any child config sections (and their children, etc).
	 * These keys will be in a valid path notation for you to use.
	 *
	 * If deep is set to false, then this will contain only the keys and values of any direct children, and not their own children.
	 *
	 * @param deep
	 * @return
	 */
	@NonNull
	public final Map<String, Object> getValues(boolean deep) {
		final Map<String, Object> result = new LinkedHashMap<>();

		this.mapChildrenValues(result, this, deep);

		return result;
	}

	/**
	 * Clears all keys in this config section
	 */
	public final void clear() {
		this.map.clear();
	}

	/**
	 * Returns true if the given path contains a valid value
	 *
	 * @param path
	 * @return
	 */
	public final boolean isStored(@NonNull String path) {

		if (this.root == null)
			return false;

		return this.retrieve(path) != null;
	}

	/**
	 * Overrides the given path with the new value, set value to null to remove
	 *
	 * @param path
	 * @param value
	 */
	public final void store(@NonNull String path, Object value) {

		if (path.isEmpty())
			throw new IllegalArgumentException("Cannot set to an empty path");

		if (this.root == null)
			throw new IllegalStateException("Cannot use section without a root");

		int leadingIndex = -1, trailingIndex;
		ConfigSection section = this;
		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String node = path.substring(trailingIndex, leadingIndex);
			final ConfigSection subSection = section.retrieveConfigurationSection(node);
			if (subSection == null) {
				if (value == null)
					// no need to create missing sub-sections if we want to remove the value:
					return;
				section = section.createSection(node);
			} else
				section = subSection;
		}

		final String key = path.substring(trailingIndex);
		if (section == this) {
			if (value == null)
				this.map.remove(key);
			else
				this.map.put(key, value);
		} else
			section.store(key, value);
	}

	/**
	 * Gets a key (or null if not set) at the given path
	 *
	 * @param path
	 * @return
	 */
	public final Object retrieve(@NonNull String path) {

		if (path.length() == 0)
			return this;

		if (this.root == null)
			throw new IllegalStateException("Cannot access section without a root");

		int leadingIndex = -1, trailingIndex;
		ConfigSection section = this;
		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String currentPath = path.substring(trailingIndex, leadingIndex);

			if (section.retrieve(currentPath) == null)
				return null;

			section = section.retrieveConfigurationSection(currentPath);

			if (section == null)
				return null;
		}

		final String key = path.substring(trailingIndex);

		if (section == this)
			return this.map.get(key);

		return section.retrieve(key);
	}

	/**
	 * Returns a config section on the given path, or null if not set
	 *
	 * @param path
	 * @return
	 */
	public final ConfigSection retrieveConfigurationSection(@NonNull String path) {
		final Object val = this.retrieve(path);

		if (val != null)
			return (val instanceof ConfigSection) ? (ConfigSection) val : null;

		return (val instanceof ConfigSection) ? this.createSection(path) : null;
	}

	/*
	 * Helper to create a new config section at the given path
	 */
	@NonNull
	final ConfigSection createSection(@NonNull String path) {
		if (path.isEmpty())
			throw new IllegalArgumentException("Cannot create section at empty path");

		if (this.root == null)
			throw new IllegalStateException("Cannot create section without a root");

		int leadingIndex = -1, trailingIndex;
		ConfigSection section = this;
		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String node = path.substring(trailingIndex, leadingIndex);
			final ConfigSection subSection = section.retrieveConfigurationSection(node);
			if (subSection == null)
				section = section.createSection(node);
			else
				section = subSection;
		}

		final String key = path.substring(trailingIndex);
		if (section == this) {
			final ConfigSection result = new ConfigSection(this, key);
			this.map.put(key, result);
			return result;
		}
		return section.createSection(key);
	}

	/*
	 * Helper to map children keys to the given output
	 */
	private void mapChildrenKeys(@NonNull Set<String> output, @NonNull ConfigSection section, boolean deep) {
		if (section instanceof ConfigSection) {
			final ConfigSection sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				output.add(createPath(section, entry.getKey(), this));

				if ((deep) && (entry.getValue() instanceof ConfigSection)) {
					final ConfigSection subsection = (ConfigSection) entry.getValue();
					this.mapChildrenKeys(output, subsection, deep);
				}
			}
		} else {
			final Set<String> keys = section.getKeys(deep);

			for (final String key : keys)
				output.add(createPath(section, key, this));
		}
	}

	/*
	 * Helper to map children keys to the given output
	 */
	private void mapChildrenValues(@NonNull Map<String, Object> output, @NonNull ConfigSection section, boolean deep) {
		if (section instanceof ConfigSection) {
			final ConfigSection sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				final String childPath = createPath(section, entry.getKey(), this);
				output.remove(childPath);
				output.put(childPath, entry.getValue());

				if (entry.getValue() instanceof ConfigSection)
					if (deep)
						this.mapChildrenValues(output, (ConfigSection) entry.getValue(), deep);
			}
		} else {
			final Map<String, Object> values = section.getValues(deep);

			for (final Map.Entry<String, Object> entry : values.entrySet())
				output.put(createPath(section, entry.getKey(), this), entry.getValue());
		}
	}

	/*
	 * Helper to create a new config section
	 */
	@NonNull
	private static String createPath(@NonNull ConfigSection section, String key) {
		return createPath(section, key, (section == null) ? null : section.root);
	}

	/*
	 * Helper to create a new config section
	 */
	@NonNull
	private static String createPath(@NonNull ConfigSection section, String key, ConfigSection relativeTo) {
		final ConfigSection root = section.root;

		if (root == null)
			throw new IllegalStateException("Cannot create path without a root");

		final StringBuilder builder = new StringBuilder();
		if (section != null)
			for (ConfigSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.parent) {
				if (builder.length() > 0)
					builder.insert(0, '.');

				builder.insert(0, parent.path);
			}

		if ((key != null) && (key.length() > 0)) {
			if (builder.length() > 0)
				builder.append('.');

			builder.append(key);
		}

		return builder.toString();
	}

	// ------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------

	/**
	 * Converts all values in this section into a saveable map
	 *
	 * @return
	 */
	public final SerializedMap serialize() {
		return SerializedMap.of(this.getValues(true));
	}

	/**
	 * Returns true if there are no keys in this section
	 *
	 * @return
	 */
	public final boolean isEmpty() {
		return Valid.isNullOrEmptyValues(this.map);
	}

	@Override
	public String toString() {
		final ConfigSection root = this.root;
		return new StringBuilder()
				.append(this.getClass().getSimpleName())
				.append("[path='")
				.append(this.fullPath)
				.append("', root='")
				.append(root == null ? null : root.getClass().getSimpleName())
				.append("', keys=" + this.map + "]")
				.toString();
	}
}
