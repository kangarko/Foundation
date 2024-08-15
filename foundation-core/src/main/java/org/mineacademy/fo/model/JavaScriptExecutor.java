package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtilCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.RemainCore;

import lombok.NonNull;
import net.kyori.adventure.audience.Audience;

/**
 * An engine that compiles and executes code on the fly.
 * <p>
 * The code is based off JavaScript with new Java methods, see:
 * https://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
 */
public final class JavaScriptExecutor {

	/**
	 * The engine singleton
	 */
	private static final ScriptEngine engine;

	// Load the engine
	static {
		Thread.currentThread().setContextClassLoader(Platform.getPlugin().getPluginClassLoader());

		ScriptEngineManager engineManager = new ScriptEngineManager();
		ScriptEngine scriptEngine = engineManager.getEngineByName("Nashorn");

		// Workaround for newer Minecraft releases, still unsure what the cause is
		if (scriptEngine == null) {
			engineManager = new ScriptEngineManager(null);

			scriptEngine = engineManager.getEngineByName("Nashorn");
		}

		// If still fails, try to load our own library for Java 15 and up
		if (scriptEngine == null) {
			final String nashorn = "org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory";

			if (ReflectionUtilCore.isClassAvailable(nashorn)) {
				final ScriptEngineFactory engineFactory = ReflectionUtilCore.instantiate(ReflectionUtilCore.lookupClass(nashorn));

				engineManager.registerEngineName("Nashorn", engineFactory);
				scriptEngine = engineManager.getEngineByName("Nashorn");
			}
		}

		engine = scriptEngine;

		if (engine == null) {
			final List<String> warningMessage = CommonCore.newList(
					"ERROR: JavaScript placeholders will not function!",
					"",
					"Your Java version/distribution lacks the",
					"Nashorn library for JavaScript placeholders.");

			if (RemainCore.getJavaVersion() >= 15)
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install the NashornPlus",
						"plugin from mineacademy.org/nashorn"));
			else
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install Java 11 from Oracle",
						"or other vendor that supports Nashorn."));

			CommonCore.logFramed(false, CommonCore.toArray(warningMessage));
		}

	}

	/**
	 * Compiles and executes the given JavaScript code
	 *
	 * @param javascript
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(final String javascript) throws FoScriptException {
		return run(javascript, null, null);
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param sender
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull String javascript, final Audience sender) throws FoScriptException {
		return run(javascript, sender, new HashMap<>());
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param sender
	 * @param replacements
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull String javascript, final Audience sender, Map<String, Object> replacements) throws FoScriptException {
		synchronized (engine) {

			if (replacements == null)
				replacements = new HashMap<>();

			// Workaround hasPermission for null senders (i.e. Discord)
			final Pattern pattern = Pattern.compile("player\\.hasPermission\\(\"([^\"]+)\"\\)");
			final Matcher matcher = pattern.matcher(javascript);

			while (matcher.find()) {
				final String permission = matcher.group(1);
				final boolean hasPermission = sender == null ? false : Platform.hasPermission(sender, permission);

				javascript = javascript.replace(matcher.group(), String.valueOf(hasPermission));
			}

			// Find and replace all %syntax% and {syntax} variables since they were not replaced for Discord
			if (Platform.isDiscord(sender)) {

				// Replace by line to avoid the {...} in "function() { return false; }" being replaced to "function() false"
				final String[] copy = javascript.split("\n");
				final String[] replaced = new String[copy.length];

				for (int i = 0; i < copy.length; i++) {
					String line = copy[i];

					line = replaceVariablesToFalse(line, Variables.BRACKET_VARIABLE_PATTERN.matcher(line));

					replaced[i] = line;
				}

				javascript = String.join("\n", replaced);
			}

			if (sender == null && javascript.contains("player.")) {
				CommonCore.warning("Not running JavaScript because it contains 'player' but player was not provided. Script: " + javascript);

				return false;
			}

			if (Platform.isDiscord(sender) && javascript.contains("player.")) {
				CommonCore.warning("Not running JavaScript because it contains 'player' but player was on Discord. Set Sender_Condition to '{sender_is_player}' to remove this warning next to your code. Script: " + javascript);

				return false;
			}

			if (sender != null)
				replacements.put("player", sender);

			return run(javascript, replacements);
		}
	}

	/*
	 * We do not support variables when the message sender is Discord,
	 * so just replace those that were not translated earlier with false value.
	 */
	private static String replaceVariablesToFalse(String javascript, Matcher matcher) {
		while (matcher.find())
			javascript = javascript.replace(matcher.group(), "false");

		return javascript;
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param replacements
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull String javascript, Map<String, Object> replacements) throws FoScriptException {
		synchronized (engine) {
			if (replacements == null)
				replacements = new HashMap<>();

			// Mohist is unsupported
			if (Platform.getServerName().equals("Mohist"))
				return null;

			// Speed up
			if (javascript.equals("true") || javascript.equals("!false") || javascript.equals("yes"))
				return true;

			if (javascript.equals("false") || javascript.equals("!true") || javascript.equals("no"))
				return false;

			if (engine == null) {
				CommonCore.warning("Not running script because JavaScript library is missing (try installing mineacademy.org/nashorn). Script: " + javascript);

				return null;
			}

			// CLear past variables
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			// Put new variables
			for (final Map.Entry<String, Object> replacement : replacements.entrySet()) {
				final String key = replacement.getKey();
				ValidCore.checkNotNull(key, "Key can't be null in javascript variables for code " + javascript + ": " + replacements);

				final Object value = replacement.getValue();
				ValidCore.checkNotNull(value, "Value can't be null in javascript variables for key " + key + ": " + replacements);

				engine.put(key, value);
			}

			try {
				final Object result = engine.eval(javascript);

				if (result instanceof String) {
					String resultString = ((String) result).trim().toLowerCase();

					if (resultString.startsWith("\"") || resultString.startsWith("'"))
						resultString = resultString.substring(1);

					if (resultString.endsWith("\"") || resultString.endsWith("'"))
						resultString = resultString.substring(0, resultString.length() - 1);

					if (resultString.equals("true"))
						return true;

					else if (resultString.equals("false"))
						return false;
				}

				return result;

			} catch (ClassCastException | ScriptException ex) {

				// Special support for throwing exceptions in the JS code so that users
				// can send messages to player directly if upstream supports that
				final String cause = ex.getCause() != null ? ex.getCause().toString() : "";

				if (ex.getCause() != null && cause.contains("event handled")) {
					final String[] errorMessageSplit = cause.contains("event handled: ") ? cause.split("event handled\\: ") : new String[0];
					final Object sender = replacements.get("player");

					if (errorMessageSplit.length == 2 && sender instanceof Audience)
						CommonCore.tellNoPrefix((Audience) sender, RemainCore.convertLegacyToAdventure(errorMessageSplit[1]));

					throw new EventHandledException(true);
				}

				final String message = ex.toString();
				final List<String> errorMessage = CommonCore.newList("Error parsing JavaScript!");

				if (message.contains("Cannot cast org.openjdk.nashorn.internal.runtime.Undefined to org.bukkit.Statistic"))
					errorMessage.add("Your code uses invalid Statistic enum for your MC version. Do NOT report this, check Bukkit javadocs.");

				if (message.contains("ReferenceError:") && message.contains("is not defined"))
					errorMessage.add("Invalid or unparsed variable!");

				if (ex instanceof ScriptException)
					errorMessage.add("Line: " + ((ScriptException) ex).getLineNumber() + ". Error: " + ex.getMessage());
				else
					errorMessage.add("Error: " + ex.getMessage());
				throw new FoScriptException(String.join(" ", errorMessage), javascript, ex instanceof ScriptException ? ((ScriptException) ex).getLineNumber() : -1, ex);
			}
		}
	}
}