package org.mineacademy.fo.settings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mineacademy.fo.ValidCore;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A type of configuration that is stored in memory.
 */
public class ConfigSection {

	private static final char PATH_SEPARATOR = '.';

	protected final Map<String, SectionPathData> map = new LinkedHashMap<>();
	private final FileConfig root;
	private final ConfigSection parent;
	private final String path;
	private final String fullPath;

	/**
	 * Creates an empty MemorySection for use as a root configuration
	 * section.
	 * <p>
	 * Note that calling this without being yourself a configuration
	 * will throw an exception!
	 *
	 * @throws IllegalStateException Thrown if this is not a {@link
	 *     Configuration} root.
	 */
	protected ConfigSection() {
		if (!(this instanceof FileConfig))
			throw new IllegalStateException("Cannot use MemorySection without being a FileConfiguration");

		this.path = "";
		this.fullPath = "";
		this.parent = null;
		this.root = (FileConfig) this;
	}

	/**
	 * Creates an empty MemorySection with the specified parent and path.
	 *
	 * @param parent Parent section that contains this own section.
	 * @param path Path that you may access this section from via the root
	 *     configuration.
	 * @throws IllegalArgumentException Thrown is parent or path is null, or
	 *     if parent contains no root Configuration.
	 */
	protected ConfigSection(@NonNull ConfigSection parent, @NonNull String path) {
		this.path = path;
		this.parent = parent;
		this.root = parent.root;

		ValidCore.checkNotNull(this.root, "Path cannot be orphaned");

		this.fullPath = createPath(parent, path);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting keys
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a set of top-level keys in this section.
	 *
	 * @param deep
	 * @return
	 */
	public final Set<String> getKeys(boolean deep) {
		final Set<String> result = new LinkedHashSet<>();

		this.mapChildrenKeys(result, this, deep);

		return result;
	}

	private void mapChildrenKeys(Set<String> output, ConfigSection section, boolean deep) {
		if (section instanceof ConfigSection) {
			final ConfigSection sec = section;

			for (final Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {
				output.add(createPath(section, entry.getKey(), this));

				if ((deep) && (entry.getValue().getData() instanceof ConfigSection)) {
					final ConfigSection subsection = (ConfigSection) entry.getValue().getData();
					this.mapChildrenKeys(output, subsection, deep);
				}
			}

		} else {
			final Set<String> keys = section.getKeys(deep);

			for (final String key : keys)
				output.add(createPath(section, key, this));
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting values
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a map of all keys and their values in this section.
	 *
	 * @param deep
	 * @return
	 */
	public final Map<String, Object> getValues(boolean deep) {
		final Map<String, Object> result = new LinkedHashMap<>();

		this.mapChildrenValues(result, this, deep);

		return result;
	}

	private void mapChildrenValues(Map<String, Object> output, ConfigSection section, boolean deep) {
		if (section instanceof ConfigSection) {
			final ConfigSection sec = section;

			for (final Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {

				// Because of the copyDefaults call potentially copying out of order, we must remove and then add in our saved order
				// This means that default values we haven't set end up getting placed first
				final String childPath = createPath(section, entry.getKey(), this);
				output.remove(childPath);
				output.put(childPath, entry.getValue().getData());

				if (entry.getValue().getData() instanceof ConfigSection)
					if (deep)
						this.mapChildrenValues(output, (ConfigSection) entry.getValue().getData(), deep);
			}
		} else {
			final Map<String, Object> values = section.getValues(deep);

			for (final Map.Entry<String, Object> entry : values.entrySet())
				output.put(createPath(section, entry.getKey(), this), entry.getValue());
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting values
	// ------------------------------------------------------------------------------------------------------------

	final boolean isMemorySection(String path) {
		final Object val = this.retrieve(path);

		return val instanceof ConfigSection;
	}

	final ConfigSection retrieveMemorySection(String path) {
		Object val = this.retrieve(path);

		if (val != null)
			return val instanceof ConfigSection ? (ConfigSection) val : null;

		val = this.retrieve(path);

		return val instanceof ConfigSection ? this.createSection(path) : null;
	}

	/**
	 * Retrieves the object at the given path.
	 *
	 * @param fullPath
	 * @return
	 */
	public final Object retrieve(@NonNull String fullPath) {
		if (fullPath.length() == 0)
			return this;

		final ConfigSection root = this.root;
		if (root == null)
			throw new IllegalStateException("Cannot access section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		ConfigSection section = this;

		while ((i1 = fullPath.indexOf(PATH_SEPARATOR, i2 = i1 + 1)) != -1) {
			final String currentPath = fullPath.substring(i2, i1);

			if (section.retrieve(currentPath) == null)
				return null;

			section = section.retrieveMemorySection(currentPath);

			if (section == null)
				return null;
		}

		final String key = fullPath.substring(i2);

		if (section == this) {
			final SectionPathData result = this.map.get(key);

			return result == null ? null : result.getData();
		}

		return section.retrieve(key);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Setting values
	// ------------------------------------------------------------------------------------------------------------

	final boolean isStored(String fullPath) {
		return this.root != null && this.retrieve(fullPath) != null;
	}

	final void store(String fullPath, Object value) {
		ValidCore.checkNotEmpty(fullPath, "Cannot set to an empty path");

		final ConfigSection root = this.root;
		if (root == null)
			throw new IllegalStateException("Cannot use section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		ConfigSection section = this;

		while ((i1 = fullPath.indexOf(PATH_SEPARATOR, i2 = i1 + 1)) != -1) {
			final String node = fullPath.substring(i2, i1);
			final ConfigSection subSection = section.retrieveMemorySection(node);

			if (subSection == null) {
				if (value == null)
					// no need to create missing sub-sections if we want to remove the value:
					return;

				section = section.createSection(node);

			} else
				section = subSection;
		}

		final String key = fullPath.substring(i2);
		if (section == this) {
			if (value == null)
				this.map.remove(key);

			else {
				final SectionPathData entry = this.map.get(key);
				if (entry == null)
					this.map.put(key, new SectionPathData(value));
				else
					entry.setData(value);
			}

		} else
			section.store(key, value);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Comments
	// ------------------------------------------------------------------------------------------------------------

	final List<String> getComments(String fullPath) {
		final SectionPathData pathData = this.getSectionPathData(fullPath);

		return pathData == null ? Collections.emptyList() : pathData.getComments();
	}

	final List<String> getInlineComments(String fullPath) {
		final SectionPathData pathData = this.getSectionPathData(fullPath);

		return pathData == null ? Collections.emptyList() : pathData.getInlineComments();
	}

	final void setComments(String fullPath, final List<String> comments) {
		final SectionPathData pathData = this.getSectionPathData(fullPath);

		if (pathData != null)
			pathData.setComments(comments);
	}

	final void setInlineComments(String fullPath, final List<String> comments) {
		final SectionPathData pathData = this.getSectionPathData(fullPath);

		if (pathData != null)
			pathData.setInlineComments(comments);
	}

	private final SectionPathData getSectionPathData(@NonNull String fullPath) {
		final ConfigSection root = this.root;

		if (root == null)
			throw new IllegalStateException("Cannot access section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		ConfigSection section = this;

		while ((i1 = fullPath.indexOf(PATH_SEPARATOR, i2 = i1 + 1)) != -1) {
			section = section.retrieveMemorySection(fullPath.substring(i2, i1));

			if (section == null)
				return null;
		}

		final String key = fullPath.substring(i2);

		if (section == this) {
			final SectionPathData entry = this.map.get(key);

			if (entry != null)
				return entry;

		} else if (section instanceof ConfigSection)
			return section.getSectionPathData(key);

		return null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------------------------------------------

	final ConfigSection createSection(String fullPath) {
		ValidCore.checkNotEmpty(fullPath, "Cannot create section at empty path");

		final ConfigSection root = this.root;
		if (root == null)
			throw new IllegalStateException("Cannot create section without a root");

		// i1 is the leading (higher) index
		// i2 is the trailing (lower) index
		int i1 = -1, i2;
		ConfigSection section = this;

		while ((i1 = fullPath.indexOf(PATH_SEPARATOR, i2 = i1 + 1)) != -1) {
			final String node = fullPath.substring(i2, i1);
			final ConfigSection subSection = section.retrieveMemorySection(node);

			if (subSection == null)
				section = section.createSection(node);
			else
				section = subSection;
		}

		final String key = fullPath.substring(i2);

		if (section == this) {
			final ConfigSection result = new ConfigSection(this, key);
			this.map.put(key, new SectionPathData(result));

			return result;
		}

		return section.createSection(key);
	}

	final ConfigSection createSection(String fullPath, Map<?, ?> map) {
		final ConfigSection section = this.createSection(fullPath);

		for (final Map.Entry<?, ?> entry : map.entrySet())
			if (entry.getValue() instanceof Map)
				section.createSection(entry.getKey().toString(), (Map<?, ?>) entry.getValue());
			else
				section.store(entry.getKey().toString(), entry.getValue());

		return section;
	}

	final String getFullPath() {
		return fullPath;
	}

	ConfigSection getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append(this.getClass().getSimpleName())
				.append("[path='")
				.append(this.fullPath)
				.append("', root='")
				.append(this.root == null ? null : this.root.getClass().getSimpleName())
				.append("']")
				.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static helpers
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * Creates a full path to the given memory section from its
	 * root configuration.
	 *
	 * You may use this method for any given memory section.
	 */
	private static String createPath(ConfigSection section, String key) {
		return createPath(section, key, (section == null) ? null : section.root);
	}

	/*
	 * Creates a relative path to the given memory section from
	 * the given relative section.
	 *
	 * You may use this method for any given memory section.
	 */
	private static String createPath(@NonNull ConfigSection section, String key, ConfigSection relativeTo) {
		final ConfigSection root = section.root;

		if (root == null)
			throw new IllegalStateException("Cannot create path without a root");

		final StringBuilder builder = new StringBuilder();

		for (ConfigSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.getParent()) {
			if (builder.length() > 0)
				builder.insert(0, PATH_SEPARATOR);

			builder.insert(0, parent.path);
		}

		if ((key != null) && (key.length() > 0)) {
			if (builder.length() > 0)
				builder.append(PATH_SEPARATOR);

			builder.append(key);
		}

		return builder.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a single node in a configuration.
	 */
	@Getter
	@Setter
	private final class SectionPathData {

		/**
		 * The data that is stored in this section.
		 */
		private Object data;

		/**
		 * If no comments exist, an empty list will be returned. A null entry in the
		 * list represents an empty line and an empty String represents an empty
		 * comment line.
		 */
		private List<String> comments = Collections.emptyList();

		/**
		 * If no comments exist, an empty list will be returned. A null entry in the
		 * list represents an empty line and an empty String represents an empty
		 * comment line.
		 */
		private List<String> inlineComments = Collections.emptyList();

		/**
		 * Creates a new instance of SectionPathData.
		 *
		 * @param data
		 */
		public SectionPathData(Object data) {
			this.data = data;
		}
	}
}
