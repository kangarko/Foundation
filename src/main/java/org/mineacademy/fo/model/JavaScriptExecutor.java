package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.debug.Debugger;
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

	/**
	 * Cache scripts for 1 second per player for highest performance
	 * <p>
	 * Player -> Map of scripts and their results
	 */
	private static final Map<UUID, Map<String, Object>> resultCache = ExpiringMap.builder().expiration(1, TimeUnit.SECONDS).build();

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
	 * Runs the given JavaScript code for the player,
	 * making the "player" variable in the code usable
	 *
	 * @param javascript
	 * @param sender
	 * @return
	 *
	 * @throws FoScriptException
	 */
	public static Object run(final String javascript, final CommandSender sender) throws FoScriptException {
		return run(javascript, sender, null);
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param sender
	 * @param event
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull String javascript, final CommandSender sender, final Event event) throws FoScriptException {

		// Mohist is unsupported
		if (Bukkit.getName().equals("Mohist"))
			return null;

		// Speed up
		if (javascript.equalsIgnoreCase("true") || javascript.equalsIgnoreCase("!false") || javascript.equalsIgnoreCase("yes"))
			return true;

		if (javascript.equalsIgnoreCase("false") || javascript.equalsIgnoreCase("!true") || javascript.equalsIgnoreCase("no"))
			return false;

		// Cache for highest performance
		Map<String, Object> cached = sender instanceof Player ? resultCache.get(((Player) sender).getUniqueId()) : null;

		if (cached != null) {
			final Object result = cached.get(javascript);

			if (result != null)
				return result;
		}

		if (engine == null) {
			Common.warning("Not running script" + (sender == null ? "" : " for " + sender.getName()) + " because JavaScript library is missing "
					+ "(install Oracle Java 8, 11 or 16 and download mineacademy.org/nashorn): " + javascript);

			return null;
		}

		Object result = null;

		try {

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

			Debugger.debug("javascript", "Sender: " + (sender == null ? "null" : sender.getName()) + " with code: " + javascript);

			if (sender != null)
				engine.put("player", sender);

			if (event != null)
				engine.put("event", event);

			result = engine.eval(javascript);

			if (result instanceof String) {
				String resultString = Common.stripColors((String) result).trim().toLowerCase();

				if (resultString.startsWith("\"") || resultString.startsWith("'"))
					resultString = resultString.substring(1);

				if (resultString.endsWith("\"") || resultString.endsWith("'"))
					resultString = resultString.substring(0, resultString.length() - 1);

				if (resultString.equalsIgnoreCase("true"))
					result = true;

				else if (resultString.equalsIgnoreCase("false"))
					result = false;
			}

			if (sender instanceof Player) {
				if (cached == null)
					cached = new HashMap<>();

				cached.put(javascript, result);
				resultCache.put(((Player) sender).getUniqueId(), cached);
			}

			return result;

		} catch (final ScriptException ex) {
			final String senderName = (sender == null ? "null sender" : sender.getName());
			final String message = ex.toString();
			String errorMessage = "Unable to parse JavaScript code on line '" + ex.getLineNumber() + "' for sender '" + senderName + "'. Error: " + message;

			if (message.contains("ReferenceError:") && message.contains("\"player\" is not defined"))
				return false;

			if (message.contains("ReferenceError:") && message.contains("is not defined"))
				errorMessage = "Found invalid or unparsed variable for sender '" + senderName + "' on line " + ex.getLineNumber() + ": " + ex.getMessage();

			// Special support for throwing exceptions in the JS code so that users
			// can send messages to player directly if upstream supports that
			final String cause = ex.getCause() != null ? ex.getCause().toString() : "";

			if (ex.getCause() != null && cause.contains("event handled")) {
				final String[] errorMessageSplit = cause.contains("event handled: ") ? cause.split("event handled\\: ") : new String[0];

				if (errorMessageSplit.length == 2)
					Common.tellNoPrefix(sender, errorMessageSplit[1]);

				throw new EventHandledException(true);
			}

			throw new FoScriptException(errorMessage, javascript, ex.getLineNumber(), ex);

		} finally {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
		}
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
	 * Executes the Javascript code with the given variables - you have to handle the error yourself
	 *
	 * @param javascript
	 * @param replacements
	 *
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull final String javascript, @NonNull final Map<String, Object> replacements) throws FoScriptException {

		// Mohist is unsupported
		if (Bukkit.getName().equals("Mohist"))
			return javascript;

		if (engine == null) {
			Common.warning("Not running script because JavaScript library is missing "
					+ "(install Oracle Java 8, 11 or 16 and download mineacademy.org/nashorn): " + javascript);

			return javascript;
		}

		for (final Map.Entry<String, Object> replacement : replacements.entrySet()) {
			final String key = replacement.getKey();
			Valid.checkNotNull(key, "Key can't be null in javascript variables for code " + javascript + ": " + replacements);

			final Object value = replacement.getValue();
			Valid.checkNotNull(value, "Value can't be null in javascript variables for key " + key + ": " + replacements);

			engine.put(key, value);
		}

		try {
			return engine.eval(javascript);

		} catch (final ScriptException ex) {
			throw new FoScriptException(ex.getMessage(), javascript, ex.getLineNumber(), ex);

		} finally {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
		}
	}
}