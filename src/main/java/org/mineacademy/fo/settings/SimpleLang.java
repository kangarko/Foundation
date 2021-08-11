package org.mineacademy.fo.settings;

import java.util.Arrays;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.JavaScriptExecutor;
import org.mineacademy.fo.model.SimpleComponent;

/**
 * Represents the new way of plugin localization, with the greatest
 * upside of saving development time.
 *
 * The downside is that keys are not checked during load so any
 * malformed or missing key will fail later and may be unnoticed.
 *
 * Using the classic SimpleLocalization is still recommended to ensure
 * your users get notified when they malform their localization file early
 * on startup.
 */
public final class SimpleLang extends YamlConfig {

	/**
	 * The instance of this class
	 */
	private static volatile SimpleLang instance;

	/**
	 * Set the instance in your plugin's onStart method.
	 *
	 * @param filePath
	 */
	public static void setInstance(String filePath) {
		instance = new SimpleLang(filePath);
	}

	/**
	 * Set the instance in your plugin's onStart method.
	 *
	 * In this method we pull the locale file from localization/messages_{SimplePrefix.LOCALE_PREFIX}.yml file
	 *
	 * @param filePath
	 */
	public static void setInstance() {
		instance = new SimpleLang("localization/messages_" + SimpleSettings.LOCALE_PREFIX + ".yml");
	}

	/**
	 * Creates a new instance
	 *
	 * @param path
	 */
	private SimpleLang(String path) {
		this.loadConfiguration(path);
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

	/**
	 * Reload this file
	 */
	public static void reloadFile() {
		synchronized (instance) {
			instance.reload();
		}
	}

	/**
	 * Return a boolean at path
	 *
	 * @param path
	 * @return
	 */
	public static boolean getOption(String path) {
		return instance.getBoolean(path);
	}

	/**
	 * Return a component list from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static List<SimpleComponent> ofComponentList(String path, Object... variables) {
		return Common.convert(ofList(path, variables), item -> SimpleComponent.of(item));
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
		synchronized (instance) {
			final String key = instance.getStringStrict(path);

			return translate(key, variables);
		}
	}

	/*
	 * Replace placeholders in the message
	 */
	private static String translate(String key, Object... variables) {
		if (variables != null)
			for (int i = 0; i < variables.length; i++) {
				Object variable = variables[i];

				variable = Common.getOrDefaultStrict(SerializeUtil.serialize(variable), "");
				Valid.checkNotNull("Failed to replace {" + i + "} as " + variable + "(raw = " + variables[i] + ")");

				key = key.replace("{" + i + "}", variable.toString());
			}

		return key;
	}
}
