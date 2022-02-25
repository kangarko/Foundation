package org.mineacademy.fo.settings;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;

public class StorageSection {

	final Map<String, Object> map = new LinkedHashMap<>();

	private final FileStorage root;
	private final StorageSection parent;
	private final String path;
	private final String fullPath;

	StorageSection() {
		this.path = "";
		this.fullPath = "";
		this.parent = null;
		this.root = (FileStorage) this;
	}

	StorageSection(@NonNull StorageSection parent, @NonNull String path) {
		this.path = path;
		this.parent = parent;
		this.root = parent.getRoot();
		this.fullPath = createPath(parent, path);
	}

	// ------------------------------------------------------------------------------------
	// Getting values
	// ------------------------------------------------------------------------------------

	@NonNull
	public final Set<String> getKeys(boolean deep) {
		final Set<String> result = new LinkedHashSet<>();
		this.mapChildrenKeys(result, this, deep);

		return result;
	}

	@NonNull
	public final Map<String, Object> getValues(boolean deep) {
		final Map<String, Object> result = new LinkedHashMap<>();

		this.mapChildrenValues(result, this, deep);

		return result;
	}

	final boolean isStored(@NonNull String path) {
		final FileStorage root = this.getRoot();

		if (root == null)
			return false;

		return this.retrieve(path) != null;
	}

	final void store(@NonNull String path, Object value) {

		if (path.isEmpty())
			throw new IllegalArgumentException("Cannot set to an empty path");

		final FileStorage root = this.getRoot();
		if (root == null)
			throw new IllegalStateException("Cannot use section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		StorageSection section = this;
		while ((i1 = path.indexOf('.', i2 = i1 + 1)) != -1) {
			final String node = path.substring(i2, i1);
			final StorageSection subSection = section.retrieveConfigurationSection(node);
			if (subSection == null) {
				if (value == null)
					// no need to create missing sub-sections if we want to remove the value:
					return;
				section = section.createSection(node);
			} else
				section = subSection;
		}

		final String key = path.substring(i2);
		if (section == this) {
			if (value == null)
				this.map.remove(key);
			else
				this.map.put(key, value);
		} else
			section.store(key, value);
	}

	final Object retrieve(@NonNull String path) {

		if (path.length() == 0)
			return this;

		final FileStorage root = this.getRoot();
		if (root == null)
			throw new IllegalStateException("Cannot access section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		StorageSection section = this;
		while ((i1 = path.indexOf('.', i2 = i1 + 1)) != -1) {
			final String currentPath = path.substring(i2, i1);

			if (section.retrieve(currentPath) == null)
				return null;

			section = section.retrieveConfigurationSection(currentPath);

			if (section == null)
				return null;
		}

		final String key = path.substring(i2);

		if (section == this)
			return this.map.get(key);

		return section.retrieve(key);
	}

	@NonNull
	final StorageSection createSection(@NonNull String path) {
		if (path.isEmpty())
			throw new IllegalArgumentException("Cannot create section at empty path");

		final FileStorage root = this.getRoot();

		if (root == null)
			throw new IllegalStateException("Cannot create section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		StorageSection section = this;
		while ((i1 = path.indexOf('.', i2 = i1 + 1)) != -1) {
			final String node = path.substring(i2, i1);
			final StorageSection subSection = section.retrieveConfigurationSection(node);
			if (subSection == null)
				section = section.createSection(node);
			else
				section = subSection;
		}

		final String key = path.substring(i2);
		if (section == this) {
			final StorageSection result = new StorageSection(this, key);
			this.map.put(key, result);
			return result;
		}
		return section.createSection(key);
	}

	final StorageSection retrieveConfigurationSection(@NonNull String path) {
		final Object val = this.retrieve(path);

		if (val != null)
			return (val instanceof StorageSection) ? (StorageSection) val : null;

		return (val instanceof StorageSection) ? this.createSection(path) : null;
	}

	private void mapChildrenKeys(@NonNull Set<String> output, @NonNull StorageSection section, boolean deep) {
		if (section instanceof StorageSection) {
			final StorageSection sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				output.add(createPath(section, entry.getKey(), this));

				if ((deep) && (entry.getValue() instanceof StorageSection)) {
					final StorageSection subsection = (StorageSection) entry.getValue();
					this.mapChildrenKeys(output, subsection, deep);
				}
			}
		} else {
			final Set<String> keys = section.getKeys(deep);

			for (final String key : keys)
				output.add(createPath(section, key, this));
		}
	}

	private void mapChildrenValues(@NonNull Map<String, Object> output, @NonNull StorageSection section, boolean deep) {
		if (section instanceof StorageSection) {
			final StorageSection sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				final String childPath = createPath(section, entry.getKey(), this);
				output.remove(childPath);
				output.put(childPath, entry.getValue());

				if (entry.getValue() instanceof StorageSection)
					if (deep)
						this.mapChildrenValues(output, (StorageSection) entry.getValue(), deep);
			}
		} else {
			final Map<String, Object> values = section.getValues(deep);

			for (final Map.Entry<String, Object> entry : values.entrySet())
				output.put(createPath(section, entry.getKey(), this), entry.getValue());
		}
	}

	@NonNull
	private static String createPath(@NonNull StorageSection section, String key) {
		return createPath(section, key, (section == null) ? null : section.getRoot());
	}

	@NonNull
	private static String createPath(@NonNull StorageSection section, String key, StorageSection relativeTo) {
		final FileStorage root = section.getRoot();

		if (root == null)
			throw new IllegalStateException("Cannot create path without a root");

		final StringBuilder builder = new StringBuilder();
		if (section != null)
			for (StorageSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.getParent()) {
				if (builder.length() > 0)
					builder.insert(0, '.');

				builder.insert(0, parent.getName());
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

	@NonNull
	private String getName() {
		return this.path;
	}

	private FileStorage getRoot() {
		return this.root;
	}

	private StorageSection getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		final FileStorage root = this.getRoot();
		return new StringBuilder()
				.append(this.getClass().getSimpleName())
				.append("[path='")
				.append(this.fullPath)
				.append("', root='")
				.append(root == null ? null : root.getClass().getSimpleName())
				.append("']")
				.toString();
	}
}
