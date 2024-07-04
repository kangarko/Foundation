package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;

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
		Thread.currentThread().setContextClassLoader(SimplePlugin.class.getClassLoader());

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

			if (ReflectionUtil.isClassAvailable(nashorn)) {
				final ScriptEngineFactory engineFactory = ReflectionUtil.instantiate(ReflectionUtil.lookupClass(nashorn));

				engineManager.registerEngineName("Nashorn", engineFactory);
				scriptEngine = engineManager.getEngineByName("Nashorn");
			}
		}

		engine = scriptEngine;

		if (engine == null) {
			final List<String> warningMessage = Common.newList(
					"ERROR: JavaScript placeholders will not function!",
					"",
					"Your Java version/distribution lacks the",
					"Nashorn library for JavaScript placeholders.");

			if (Remain.getJavaVersion() >= 15)
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install the NashornPlus",
						"plugin from mineacademy.org/nashorn"));
			else
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install Java 11 from Oracle",
						"or other vendor that supports Nashorn."));

			Common.logFramed(false, Common.toArray(warningMessage));
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
	public static Object run(@NonNull String javascript, @Nullable final CommandSender sender) throws FoScriptException {
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
	public static Object run(@NonNull String javascript, @Nullable final CommandSender sender, @NonNull Map<String, Object> replacements) throws FoScriptException {

		// Workaround hasPermission for null senders (i.e. Discord)
		final Pattern pattern = Pattern.compile("player\\.hasPermission\\(\"([^\"]+)\"\\)");
		final Matcher matcher = pattern.matcher(javascript);

		while (matcher.find()) {
			final String permission = matcher.group(1);
			final boolean hasPermission = sender == null ? false : sender.hasPermission(permission);

			javascript = javascript.replace(matcher.group(), String.valueOf(hasPermission));
		}

		// Find and replace all %syntax% and {syntax} variables since they were not replaced for Discord
		if (sender instanceof DiscordSender) {

			// Replace by line to avoid the {...} in "function() { return false; }" being replaced to "function() false"
			final String[] copy = javascript.split("\n");
			final String[] replaced = new String[copy.length];

			for (int i = 0; i < copy.length; i++) {
				String line = copy[i];

				line = replaceVariables(line, Variables.VARIABLE_PATTERN.matcher(line));
				line = replaceVariables(line, Variables.BRACKET_VARIABLE_PATTERN.matcher(line));

				replaced[i] = line;
			}

			javascript = String.join("\n", replaced);
		}

		if (sender == null && javascript.contains("player.")) {
			Common.warning("Not running JavaScript because it contains 'player' but player was not provided. Script: " + javascript);

			return false;
		}

		if (sender instanceof DiscordSender && javascript.contains("player.")) {
			Common.warning("Not running JavaScript because it contains 'player' but player was on Discord. Set Sender_Condition to '{sender_is_player}' to remove this warning next to your code. Script: " + javascript);

			return false;
		}

		if (sender != null)
			replacements.put("player", sender);

		return run(javascript, replacements);
	}

	/*
	 * We do not support variables when the message sender is Discord,
	 * so just replace those that were not translated earlier with false value.
	 */
	private static String replaceVariables(String javascript, Matcher matcher) {
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
	public static Object run(@NonNull String javascript, @NonNull final Map<String, Object> replacements) throws FoScriptException {
		synchronized (SimplePlugin.getInstance()) {
			// Mohist is unsupported
			if (Bukkit.getName().equals("Mohist"))
				return null;

			// Speed up
			if (javascript.equals("true") || javascript.equals("!false") || javascript.equals("yes"))
				return true;

			if (javascript.equals("false") || javascript.equals("!true") || javascript.equals("no"))
				return false;

			if (engine == null) {
				Common.warning("Not running script because JavaScript library is missing (try installing mineacademy.org/nashorn). Script: " + javascript);

				return null;
			}

			// CLear past variables
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			// Put new variables
			for (final Map.Entry<String, Object> replacement : replacements.entrySet()) {
				final String key = replacement.getKey();
				Valid.checkNotNull(key, "Key can't be null in javascript variables for code " + javascript + ": " + replacements);

				final Object value = replacement.getValue();
				Valid.checkNotNull(value, "Value can't be null in javascript variables for key " + key + ": " + replacements);

				engine.put(key, value);
			}

			try {
				final Object result = engine.eval(javascript);

				if (result instanceof String) {
					String resultString = Common.stripColors((String) result).trim().toLowerCase();

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

			} catch (final ScriptException ex) {

				// Special support for throwing exceptions in the JS code so that users
				// can send messages to player directly if upstream supports that
				final String cause = ex.getCause() != null ? ex.getCause().toString() : "";

				if (ex.getCause() != null && cause.contains("event handled")) {
					final String[] errorMessageSplit = cause.contains("event handled: ") ? cause.split("event handled\\: ") : new String[0];
					final Object sender = replacements.get("player");

					if (errorMessageSplit.length == 2 && sender instanceof CommandSender)
						Common.tellNoPrefix((CommandSender) sender, errorMessageSplit[1]);

					throw new EventHandledException(true);
				}

				final String message = ex.toString();
				final List<String> errorMessage = Common.newList("Error parsing JavaScript!");

				if (message.contains("ReferenceError:") && message.contains("is not defined"))
					errorMessage.add("Invalid or unparsed variable!");

				errorMessage.add("Line: " + ex.getLineNumber() + ". Error: " + ex.getMessage());

				throw new FoScriptException(String.join(" ", errorMessage), javascript, ex.getLineNumber(), ex);
			}
		}
	}
}