package org.mineacademy.fo.settings;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CaseNumberFormat;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.Platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Represents a localization system for your plugin. All localization keys
 * are stored in a json file with no nested keys, and can be layered.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Lang {

	/**
	 * The instance of this class
	 */
	private static final Lang instance = new Lang();

	/*
	 * The entire dictionary with all keys, unchanged, as a JsonObject.
	 */
	private JsonObject dictionary;

	/*
	 * The different caches for maximum performance. Legacy caches contain
	 * keys with MiniMessage and & colors translated to §.
	 */
	private Map<String, String> plainCache;
	private Map<String, String> legacyCache;
	private Map<String, String[]> legacyArrayCache;
	private Map<String, SimpleComponent> componentCache;
	private Map<String, SimpleComponent[]> componentArrayCache;
	private final Map<String, CaseNumberFormat> numberFormatCache = new HashMap<>();

	/*
	 * Return a plain String from the language file, throwing an error if the key is missing.
	 */
	private String getPlain(String path) {
		ValidCore.checkNotNull(this.plainCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");
		ValidCore.checkBoolean(this.plainCache.containsKey(path), "Missing localization key '" + path + "'");

		return this.plainCache.get(path);
	}

	/*
	 * Return a legacy key from the given path in the language file.
	 */
	private String getLegacy(String path) {
		ValidCore.checkNotNull(this.legacyCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");
		ValidCore.checkBoolean(this.legacyCache.containsKey(path), "Missing localization key '" + path + "'");

		return this.legacyCache.get(path);
	}

	/*
	 * Return a clone of the array to prevent modification of the cache.
	 */
	private String[] getLegacyArray(String path) {
		ValidCore.checkNotNull(this.legacyArrayCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");

		if (!this.legacyArrayCache.containsKey(path)) {
			if (this.legacyCache.containsKey(path))
				throw new FoException("Localization key '" + path + "' is not an array!");
			else
				throw new FoException("Missing localization array key '" + path + "'");
		}

		final String[] stored = this.legacyArrayCache.get(path);
		return Arrays.copyOf(stored, stored.length);
	}

	/*
	 * Return a CaseNumberFormat from the given path in the language file,
	 * caching the result if it does not exist.
	 */
	private CaseNumberFormat getCaseNumberFormat(String path) {
		CaseNumberFormat format = this.numberFormatCache.get(path);

		if (format == null) {
			format = CaseNumberFormat.fromString(this.getPlain(path));

			this.numberFormatCache.put(path, format);
		}

		return format;
	}

	/*
	 * Return a component from the given path in the language file.
	 */
	private SimpleComponent getComponent(String path) {
		ValidCore.checkNotNull(this.componentCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");
		ValidCore.checkBoolean(this.componentCache.containsKey(path), "Missing localization key '" + path + "'");

		return this.componentCache.get(path);
	}

	/*
	 * Returns a clone of the array to prevent modification of the cache.
	 */
	private SimpleComponent[] getComponentArray(String path) {
		ValidCore.checkNotNull(this.componentArrayCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");

		if (!this.componentArrayCache.containsKey(path)) {
			if (this.componentCache.containsKey(path))
				throw new FoException("Localization key '" + path + "' is not an array!");
			else
				throw new FoException("Missing localization array key '" + path + "'");
		}

		final SimpleComponent[] stored = this.componentArrayCache.get(path);
		return Arrays.copyOf(stored, stored.length);
	}

	/*
	 * Return true if the plain cache has a key at the given path.
	 */
	private boolean has(String path) {
		return this.plainCache.containsKey(path);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return if the given key exists in the language file.
	 *
	 * @see Storage#load()
	 *
	 * @param path
	 * @return
	 */
	public static boolean exists(String path) {
		return instance.has(path);
	}

	/**
	 * Return a plain String from the language file, throwing an error if the key is missing.
	 *
	 * No modifications are done to the key.
	 *
	 * @param path
	 * @return
	 */
	public static String plain(String path) {
		return instance.getPlain(path);
	}

	/**
	 * Return a legacy key from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * MiniMessage tags and & legacy colors are translated to §.
	 *
	 * @param path
	 * @return
	 */
	public static String legacy(String path) {
		return instance.getLegacy(path);
	}

	/**
	 * Return a legacy key from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * SimpleComponent, or a list of either.
	 *
	 * Example: legacyVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * MiniMessage tags and & legacy colors are translated to §.
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static String legacyVars(String path, Object... replacements) {
		final String value = legacy(path);

		return Variables.replace(value, null, CommonCore.newHashMap(replacements));
	}

	/**
	 * Return a legacy array from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Example: legacyArrayVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * MiniMessage tags and & legacy colors are translated to §.
	 *
	 * @param path
	 * @return
	 */
	public static String[] legacyArray(String path) {
		return instance.getLegacyArray(path);
	}

	/**
	 * Return a legacy array from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * SimpleComponent, or a list of either.
	 *
	 * Example: legacyArrayVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * MiniMessage tags and & legacy colors are translated to §.
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static String[] legacyArrayVars(String path, Object... replacements) {
		final String[] lines = instance.getLegacyArray(path);
		final Map<String, Object> replacementsMap = CommonCore.newHashMap(replacements);

		for (int i = 0; i < lines.length; i++)
			lines[i] = Variables.replace(lines[i], null, replacementsMap);

		return lines;
	}

	/**
	 * Return a component from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * @param path
	 * @return
	 */
	public static SimpleComponent component(String path) {
		return instance.getComponent(path);
	}

	/**
	 * Return a component from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * SimpleComponent, or a list of either.
	 *
	 * Example: componentVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static SimpleComponent componentVars(String path, Object... replacements) {
		final SimpleComponent component = component(path);

		return Variables.replace(component, null, CommonCore.newHashMap(replacements));
	}

	/**
	 * Return component array from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * @param path
	 * @return
	 */
	public static SimpleComponent[] componentArray(String path) {
		return instance.getComponentArray(path);
	}

	/**
	 * Return component array from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * SimpleComponent, or a list of either.
	 *
	 * Example: componentArrayVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static SimpleComponent[] componentArrayVars(String path, Object... replacements) {
		final SimpleComponent[] components = instance.getComponentArray(path);
		final Map<String, Object> replacementsMap = CommonCore.newHashMap(replacements);

		for (int i = 0; i < components.length; i++)
			components[i] = Variables.replace(components[i], null, replacementsMap);

		return components;
	}

	/**
	 * Return a String key from the given path in the language file.
	 *
	 * Singular or plural form is automatically chosen based on the amount and the
	 * result includes the amount itself.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Example: numberFormat("case-apples", 5) returns "5 apples" if the
	 * key at "case-apples" is "apple, apples"
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String numberFormat(String path, long amount) {
		return instance.getCaseNumberFormat(path).formatWithCount(amount);
	}

	/**
	 * Return a String key from the given path in the language file.
	 *
	 * Singular or plural form is automatically chosen based on the amount and the
	 * result excludes the amount.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Example: numberFormat("case-apples", 5) returns "apples" if the
	 * key at "case-apples" is "apple, apples"
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String numberFormatNoAmount(String path, long amount) {
		return instance.getCaseNumberFormat(path).formatWithoutCount(amount);
	}

	/**
	 * Stores some default keys from the main overlay that need to be initialized into a
	 * class for maximum performance.
	 */
	public static final class Default {

		/**
		 * The {date}, {date_short} and {date_month} formats.
		 */
		private static DateFormat dateFormat;
		private static DateFormat dateFormatShort;
		private static DateFormat dateFormatMonth;

		/**
		 * The format used in the {date} placeholder.
		 *
		 * @see Variables
		 *
		 * @return
		 */
		public static DateFormat getDateFormat() {
			if (dateFormat == null)
				dateFormat = makeFormat("format-date", "dd.MM.yyyy HH:mm:ss");

			return dateFormat;
		}

		/**
		 * The format used in the {date_short} placeholder.
		 *
		 * @see Variables
		 *
		 * @return
		 */
		public static DateFormat getDateFormatShort() {
			if (dateFormatShort == null)
				dateFormatShort = makeFormat("format-date-short", "dd.MM.yyyy HH:mm");

			return dateFormatShort;
		}

		/**
		 * The format used in the {date_month} placeholder.
		 *
		 * @see Variables
		 *
		 * @return
		 */
		public static DateFormat getDateFormatMonth() {
			if (dateFormatMonth == null)
				dateFormatMonth = makeFormat("format-date-month", "dd.MM HH:mm");

			return dateFormatMonth;
		}

		/*
		 * A helper method to create a date format from the given plain lang key.
		 */
		private static DateFormat makeFormat(String key, String def) {
			final String raw = exists(key) ? plain(key) : def;

			try {
				return new SimpleDateFormat(raw);

			} catch (final IllegalArgumentException ex) {
				CommonCore.throwError(ex, "Date format at '" + key + "' is invalid: '" + raw + "'! See https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for syntax'");

				return null;
			}
		}
	}

	/**
	 * A class that handles downloading the localization keys.
	 */
	public static final class Storage {

		/**
		 * Will dump the locale keys onto the lang/xx_YY.json file.
		 *
		 * Existing keys will be preserved, new keys will be added, and unused keys will be removed.
		 *
		 * If the file does not exist, it will be created.
		 *
		 * @return
		 */
		public static File createAndDumpToFile() {
			return dumpToFile0(true);
		}

		/**
		 * Will update the locale keys in the lang/xx_YY.json file if the file exists.
		 *
		 * Existing keys will be preserved, new keys will be added, and unused keys will be removed.
		 *
		 * @return
		 */
		public static File updateFileIfExists() {
			return dumpToFile0(false);
		}

		/*
		 * Implementation of the file dump.
		 */
		private static File dumpToFile0(boolean createFileIfNotExists) {
			final File localFile = FileUtil.getFile("lang/" + SimpleSettings.LOCALE + ".json");

			if (!localFile.exists()) {
				if (createFileIfNotExists)
					FileUtil.createIfNotExists(localFile);
				else
					return localFile;
			}

			JsonObject localJson = CommonCore.GSON.fromJson(String.join("\n", FileUtil.readLinesFromFile(localFile)), JsonObject.class);

			if (localJson == null)
				localJson = new JsonObject();

			CommonCore.log("Updating localization file " + localFile);

			// First, remove local keys that no longer exist in our dictionary
			for (final String key : localJson.keySet())
				if (!instance.dictionary.has(key)) {
					CommonCore.log("Removing unused key '" + key + "'");

					localJson.remove(key);
				}

			// Then, add new keys to the local file
			for (final String key : instance.dictionary.keySet())
				if (!localJson.has(key)) {
					CommonCore.log("Adding new key '" + key + "'");

					localJson.add(key, instance.dictionary.get(key));
				}

			FileUtil.write(localFile, Arrays.asList(CommonCore.GSON_PRETTY.toJson(localJson)), StandardOpenOption.TRUNCATE_EXISTING);

			return localFile;
		}

		/**
		 * Load the localization keys from the plugin jar and disk. This is done in layers,
		 * first come the base overlay which should be shipped in lang/overlay/xx_YY.json in Foundation,
		 * then the plugin-specific overlay in lang/yy_XX.json, and lastly the file in lang/xx_YY.json file on disk.
		 *
		 * Each load is further split into first loading the English keys and then the language specific
		 * if they exists.
		 *
		 * The code is further split into {} because I love it.
		 */
		public static void load() {
			final String englishLangTag = Locale.US.getLanguage() + "_" + Locale.US.getCountry();
			final boolean isEnglish = SimpleSettings.LOCALE.equals("en_US");

			List<String> content;
			final JsonObject dictionary = new JsonObject();

			// Foundation locale
			{
				content = FileUtil.readLinesFromInternalPath("lang/overlay/" + englishLangTag + ".json");

				// Base overlay must be set
				ValidCore.checkNotNull(content, "Locale file lang/overlay/en_US.json is missing! Did you reload or used PlugMan(X)? Make sure Foundation is shaded properly!");
				putToDictionary(dictionary, content);

				// Language specific base overlay can be null
				if (!isEnglish) {
					content = FileUtil.readLinesFromInternalPath("lang/overlay/" + SimpleSettings.LOCALE + ".json");

					putToDictionary(dictionary, content);
				}

			}

			// Plugin-specific
			{
				// Optional
				content = FileUtil.readLinesFromInternalPath("lang/" + englishLangTag + ".json");
				putToDictionary(dictionary, content);

				if (!isEnglish) {

					// Base overlay must be set when using non-English locale
					ValidCore.checkNotNull(content, "When using non-English locale (" + SimpleSettings.LOCALE + "), the base overlay en_US.json must exists in " + Platform.getPlugin().getName());

					content = FileUtil.readLinesFromInternalPath("lang/" + SimpleSettings.LOCALE + ".json");
					putToDictionary(dictionary, content);
				}
			}

			// On disk
			{
				// Start with base locale as overlay
				content = FileUtil.readLinesFromFile("lang/" + englishLangTag + ".json");
				putToDictionary(dictionary, content);

				if (!isEnglish) {
					content = FileUtil.readLinesFromFile("lang/" + SimpleSettings.LOCALE + ".json");
					putToDictionary(dictionary, content);
				}
			}

			// At last, update the dictionary on disk if the file exists
			updateFileIfExists();

			// Cache all the keys for maximum performance
			final Map<String, String> plainCache = new HashMap<>();
			final Map<String, String> legacyCache = new HashMap<>();
			final Map<String, String[]> legacyArrayCache = new HashMap<>();
			final Map<String, SimpleComponent> componentCache = new HashMap<>();
			final Map<String, SimpleComponent[]> componentArrayCache = new HashMap<>();

			for (final String key : dictionary.keySet()) {
				final JsonElement value = dictionary.get(key);

				if (value.isJsonPrimitive()) {
					final String string = value.getAsString();
					final SimpleComponent component = SimpleComponent.fromMini(string);

					plainCache.put(key, string);
					componentCache.put(key, component);
					legacyCache.put(key, component.toLegacy());
				}

				// else if it it is array, join with \n
				else if (value.isJsonArray()) {
					final JsonArray array = value.getAsJsonArray();

					final List<SimpleComponent> componentList = new ArrayList<>();
					final List<String> legacyList = new ArrayList<>();

					for (final JsonElement element : array)
						if (element.isJsonPrimitive()) {
							final String string = element.getAsString();
							final SimpleComponent component = SimpleComponent.fromMini(string);

							componentList.add(component);
							legacyList.add(component.toLegacy());

						} else {
							ValidCore.checkBoolean(element != null && !element.isJsonNull(), "Missing element in array for lang key " + key + "! Make sure to remove ',' at the end of the list");

							CommonCore.warning("Invalid element in array for lang key " + key + ": " + element + ", only Strings and primitives are supported");
						}

					componentArrayCache.put(key, componentList.toArray(new SimpleComponent[componentList.size()]));
					legacyArrayCache.put(key, legacyList.toArray(new String[legacyList.size()]));

				} else {
					ValidCore.checkBoolean(value != null && !value.isJsonNull(), "Missing element for lang key " + key + ", check for trailing commas");

					CommonCore.warning("Invalid element for lang key " + key + ": " + value + ", only Strings, primitives and arrays are supported");
				}
			}

			instance.plainCache = plainCache;
			instance.legacyCache = legacyCache;
			instance.legacyArrayCache = legacyArrayCache;
			instance.componentCache = componentCache;
			instance.componentArrayCache = componentArrayCache;

			instance.dictionary = dictionary;
		}

		/*
		 * Helper method to turn the lines content into a single dump, parse to JSON and
		 * put the keys into the dictionary.
		 */
		private static void putToDictionary(JsonObject dictionary, List<String> content) {
			if (content != null && !content.isEmpty()) {
				final JsonObject json = CommonCore.GSON.fromJson(String.join("\n", content), JsonObject.class);

				for (final String key : json.keySet())
					dictionary.add(key, json.get(key));
			}
		}
	}
}
