package org.mineacademy.fo.settings;

import java.util.Arrays;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.JavaScriptExecutor;
import org.mineacademy.fo.model.SimpleComponent;

/**
 * Represents the new way of internalization, with the greatest
 * upside of saving development time.
 *
 * The downside is that keys are not checked during load so any
 * malformed or missing key will fail later and may be unnoticed.
 */
public final class Lang extends YamlConfig {

	/**
	 * The instance of this class
	 */
	private static volatile Lang instance;

	/*
	 * Create a new instance and load the given file
	 */
	private Lang(String filePath) {
		this.loadConfiguration(filePath);
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#saveComments()
	 */
	@Override
	protected boolean saveComments() {
		return true;
	}

	/*
	 * Return a key from our localization, failing if not exists
	 */
	private String getStringStrict(String path) {
		final String key = getString(path);
		Valid.checkNotNull(key, "Missing localization key '" + path + "' from " + getFileName());

		return key;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static access - loading
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Call this method in your onPluginPreStart to use the Lang features,
	 * the Lang class will use the given file in the path below:
	 * "localization/messages_" + SimpleSettings.LOCALE_PREFIX ".yml"
	 */
	public static void init() {
		init("localization/messages_" + SimpleSettings.LOCALE_PREFIX + ".yml");
	}

	/**
	 * Call this method in your onPluginPreStart to use the Lang features,
	 * the Lang class will use the given file in the given path.
	 *
	 * Example: "localization/messages_" + SimpleSettings.LOCALE_PREFIX ".yml"
	 * @param filePath
	 */
	public static void init(String filePath) {
		instance = new Lang(filePath);

		loadPrefixes();
	}

	/**
	 * Reload prefixes from the locale file
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void loadPrefixes() {
		if (instance != null) {
			synchronized (instance) {
				if (instance.isSet("Prefix.Announce"))
					Messenger.setAnnouncePrefix(Lang.of("Prefix.Announce"));

				if (instance.isSet("Prefix.Error"))
					Messenger.setErrorPrefix(Lang.of("Prefix.Error"));

				if (instance.isSet("Prefix.Info"))
					Messenger.setInfoPrefix(Lang.of("Prefix.Info"));

				if (instance.isSet("Prefix.Question"))
					Messenger.setQuestionPrefix(Lang.of("Prefix.Question"));

				if (instance.isSet("Prefix.Success"))
					Messenger.setSuccessPrefix(Lang.of("Prefix.Success"));

				if (instance.isSet("Prefix.Warn"))
					Messenger.setWarnPrefix(Lang.of("Prefix.Warn"));
			}
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a boolean at path
	 *
	 * @param path
	 * @return
	 */
	public static boolean getOption(String path) {
		checkInit();

		synchronized (instance) {
			return instance.getBoolean(path);
		}
	}

	/**
	 * Return a component list from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static List<SimpleComponent> ofComponentList(String path, Object... variables) {
		return Common.convert(ofList(path, variables), SimpleComponent::of);
	}

	/**
	 * Return a list from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static List<String> ofList(String path, Object... variables) {
		return Arrays.asList(ofArray(path, variables));
	}

	/**
	 * Return an array from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static String[] ofArray(String path, Object... variables) {
		return of(path, variables).split("\n");
	}

	/**
	 * Return a component from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static SimpleComponent ofComponent(String path, Object... variables) {
		return SimpleComponent.of(of(path, variables));
	}

	/**
	 * Return the given key for the given amount automatically
	 * singular or plural form including the amount
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String ofCase(long amount, String path) {
		return amount + " " + ofCaseNoAmount(amount, path);
	}

	/**
	 * Return the given key for the given amount automatically
	 * singular or plural form excluding the amount
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String ofCaseNoAmount(long amount, String path) {
		final String key = of(path);
		final String[] split = key.split(", ");

		Valid.checkBoolean(split.length == 1 || split.length == 2, "Invalid syntax of key at '" + path + "', this key is a special one and "
				+ "it needs singular and plural form separated with , such as: second, seconds");

		final String singular = split[0];
		final String plural = split[split.length == 2 ? 1 : 0];

		return amount == 0 || amount > 1 ? plural : singular;
	}

	/**
	 * Return an array from the localization file with {0} {1} etc. variables replaced.
	 * and script variables parsed. We treat the locale key as a valid JavaScript
	 *
	 * @param path
	 * @param scriptVariables
	 * @param variables
	 * @return
	 */
	public static String ofScript(String path, SerializedMap scriptVariables, Object... variables) {
		String script = of(path, variables);
		Object result;

		// Our best guess is that the user has removed the script completely but forgot to put the entire message in '',
		// so we attempt to do so
		if (!script.contains("?") && !script.contains(":") && !script.contains("+") && !script.startsWith("'") && !script.endsWith("'"))
			script = "'" + script + "'";

		try {
			result = JavaScriptExecutor.run(script, scriptVariables.asMap());

		} catch (final Throwable t) {
			throw new FoException(t, "Failed to compile localization key '" + path + "' with script: " + script + " (this must be a valid JavaScript code)");
		}

		return result.toString();
	}

	/**
	 * Return a key from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static String of(String path, Object... variables) {
		checkInit();

		synchronized (instance) {
			String key = instance.getStringStrict(path);

			key = Messenger.replacePrefixes(key);
			key = translate(key, variables);

			return key;
		}
	}

	/*
	 * Replace placeholders in the message
	 */
	private static String translate(String key, Object... variables) {
		Valid.checkNotNull(key, "Cannot translate a null key with variables " + Common.join(variables));

		if (variables != null)
			for (int i = 0; i < variables.length; i++) {
				Object variable = variables[i];

				variable = Common.getOrDefaultStrict(SerializeUtil.serialize(variable), SimpleLocalization.NONE);
				Valid.checkNotNull(variable, "Failed to replace {" + i + "} as " + variable + " (raw = " + variables[i] + ")");

				key = key.replace("{" + i + "}", variable.toString());
			}

		return key;
	}

	/*
	 * Check if this class has properly been initialized
	 */
	private static void checkInit() {

		// Automatically load when not loaded in onPluginPreStart
		if (instance == null)
			init();
	}
}
