package org.mineacademy.fo.settings;

import java.util.Arrays;
import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MessengerCore;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Mode;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.model.JavaScriptExecutor;
import org.mineacademy.fo.model.SimpleComponentCore;
import org.mineacademy.fo.model.Variables;

import net.kyori.adventure.text.Component;

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
	private static Lang instance;

	/*
	 * Create a new instance and load the given file
	 */
	private Lang(String filePath) {
		this.loadConfiguration(filePath);

		this.setFastMode(true);
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
	 * Reload the language file
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void reloadLang() {
		if (instance != null) {
			instance.reload();
			instance.save();
		}
	}

	/**
	 * Reload prefixes from the locale file
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void loadPrefixes() {
		if (instance != null) {
			if (instance.isSet("Prefix.Announce"))
				MessengerCore.setAnnouncePrefix(Lang.ofComponent("Prefix.Announce"));

			if (instance.isSet("Prefix.Error"))
				MessengerCore.setErrorPrefix(Lang.ofComponent("Prefix.Error"));

			if (instance.isSet("Prefix.Info"))
				MessengerCore.setInfoPrefix(Lang.ofComponent("Prefix.Info"));

			if (instance.isSet("Prefix.Question"))
				MessengerCore.setQuestionPrefix(Lang.ofComponent("Prefix.Question"));

			if (instance.isSet("Prefix.Success"))
				MessengerCore.setSuccessPrefix(Lang.ofComponent("Prefix.Success"));

			if (instance.isSet("Prefix.Warn"))
				MessengerCore.setWarnPrefix(Lang.ofComponent("Prefix.Warn"));

			instance.save();
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

		return instance.getBoolean(path);
	}

	/**
	 * Return a component list from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static List<SimpleComponentCore> ofComponentList(String path, Object... variables) {
		return CommonCore.convert(ofList(path, variables), SimpleComponentCore::of);
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
	public static Component ofComponent(String path, Object... variables) {
		return CommonCore.colorize(of(path, variables));
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

		ValidCore.checkBoolean(split.length == 1 || split.length == 2, "Invalid syntax of key at '" + path + "', this key is a special one and "
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
	 * @param stringVariables
	 * @deprecated unstable, JavaScript executor might desynchronize and break scriptVariables
	 *
	 * @return
	 */
	@Deprecated
	public static String ofScript(String path, SerializedMap scriptVariables, Object... stringVariables) {
		String script = of(path, stringVariables);
		Object result;

		// Our best guess is that the user has removed the script completely but forgot to put the entire message in '',
		// so we attempt to do so
		if (!script.contains("?") && !script.contains(":") && !script.contains("+") && !script.startsWith("'") && !script.endsWith("'"))
			script = "'" + script + "'";

		try {
			result = JavaScriptExecutor.run(script, scriptVariables.asMap());

		} catch (final FoScriptException ex) {
			CommonCore.logFramed("Failed to compile localization key!",
					"It must be a valid JavaScript code, if you modified it, check the syntax!",
					"",
					"Locale path: '" + path + "'",
					"Variables: " + scriptVariables,
					"String variables: " + CommonCore.join(stringVariables),
					"Script: " + script,
					"Error: %error%");

			throw ex;
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

		String key = instance.getString(path);

		if (key == null)
			throw new FoException("Missing localization key '" + path + "' from " + instance.getFileName());

		key = Variables.replace(key, null);
		key = translate(key, variables);

		return key;
	}

	/*
	 * Replace placeholders in the message
	 */
	// TODO knock to Variables for improved performance
	private static String translate(String key, Object... variables) {
		ValidCore.checkNotNull(key, "Cannot translate a null key with variables " + CommonCore.join(variables));

		if (variables != null)
			for (int i = 0; i < variables.length; i++) {
				Object variable = variables[i];

				if (variable == null)
					variable = SimpleLocalization.NONE;

				else if (variable instanceof String)
					variable = variable.toString();

				else
					variable = CommonCore.getOrDefaultStrict(SerializeUtilCore.serialize(Mode.YAML /* ĺocale is always .yml */, variable), SimpleLocalization.NONE);

				ValidCore.checkNotNull(variable, "Failed to replace {" + i + "} as " + variable + " (raw = " + variables[i] + ")");
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
