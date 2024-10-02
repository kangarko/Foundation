package org.mineacademy.fo.settings;

import java.io.File;
import java.util.Map;

import org.mineacademy.fo.FileUtil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Holds the "main" data yaml file config suitable for storing
 * varius plugin data such as player caches.
 *
 * This class is synchronized where as the YamlConfig is not.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataFileConfig extends YamlConfig {

	/**
	 * The file name.
	 */
	private static final String FILE_NAME = "data.yml";

	/**
	 * The instance
	 */
	private static volatile DataFileConfig instance;

	/**
	 * Remove empty config sections on save.
	 */
	@Override
	protected void onSave() {
		for (final Map.Entry<String, Object> entry : this.getValues(true).entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof ConfigSection && ((ConfigSection) value).getKeys(false).isEmpty())
				this.setAbsolute(key, null);
		}
	}

	/**
	 * Return the data config file instance, creating a new file if
	 * it does not exist, and sets the given path prefix for
	 * any future get() or set() calls.
	 *
	 * @param pathPrefix
	 * @return
	 */
	public static DataFileConfig getSection(String pathPrefix) {
		final DataFileConfig instance = getInstance();

		instance.setPathPrefix(pathPrefix);

		return instance;
	}

	/**
	 * Return the data config file instance, creating a new file if
	 * it does not exist.
	 *
	 * @return
	 */
	public static DataFileConfig getInstance() {
		if (instance == null) {
			instance = new DataFileConfig();

			final File legacyFile = FileUtil.getFile("data.db");
			final File newFile = FileUtil.getFile(FILE_NAME);

			if (legacyFile.exists())
				legacyFile.renameTo(newFile);

			FileUtil.createIfNotExists(FILE_NAME);

			instance.load(newFile);
		}

		synchronized (instance) {

			// Reset from last calls
			instance.setPathPrefix(null);

			return instance;
		}
	}
}
