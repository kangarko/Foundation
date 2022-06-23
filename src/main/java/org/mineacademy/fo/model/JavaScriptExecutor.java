package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.exception.EventHandledException;
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
	 */
	public static Object run(final String javascript) {
		return run(javascript, null, null);
	}

	/**
	 * Runs the given JavaScript code for the player,
	 * making the "player" variable in the code usable
	 *
	 * @param javascript
	 * @param sender
	 * @return
	 */
	public static Object run(final String javascript, final CommandSender sender) {
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
	 */
	public static Object run(@NonNull String javascript, final CommandSender sender, final Event event) {

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

		try {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			if (sender != null)
				engine.put("player", sender);

			if (event != null)
				engine.put("event", event);

			if (sender instanceof DiscordSender) {
				final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(javascript);

				while (matcher.find())
					// We do not support variables when the message sender is Discord,
					// so just replace those that were not translated earlier with false value.
					javascript = javascript.replace(matcher.group(), "false");
			}

			Object result = engine.eval(javascript);

			if (result instanceof String) {
				String resultString = Common.stripColors((String) result).toLowerCase();

				if (resultString.equals("true") || resultString.equals("yes"))
					result = true;

				else if (resultString.equals("false") || resultString.equals("no"))
					result = false;
			}

			if (sender instanceof Player) {
				if (cached == null)
					cached = new HashMap<>();

				cached.put(javascript, result);
				resultCache.put(((Player) sender).getUniqueId(), cached);
			}

			return result;

		} catch (final Throwable ex) {
			final String message = ex.toString();
			String error = "Script execution failed for";

			if (message.contains("ReferenceError:") && message.contains("is not defined"))
				error = "Found invalid or unparsed variable in";

			// Special support for throwing exceptions in the JS code so that users
			// can send messages to player directly if upstream supports that
			final String cause = ex.getCause().toString();

			if (ex.getCause() != null && cause.contains("event handled")) {
				final String[] errorMessageSplit = cause.contains("event handled: ") ? cause.split("event handled\\: ") : new String[0];

				if (errorMessageSplit.length == 2)
					Common.tellNoPrefix(sender, errorMessageSplit[1]);

				throw new EventHandledException(true);
			}

			throw new RuntimeException(error + " '" + javascript + "'", ex);
		}
	}

	/**
	 * Executes the Javascript code with the given variables - you have to handle the error yourself
	 *
	 * @param javascript
	 * @param replacements
	 *
	 * @return
	 */
	public static Object run(final String javascript, final Map<String, Object> replacements) {

		if (engine == null) {
			Common.warning("Not running script because JavaScript library is missing "
					+ "(install Oracle Java 8, 11 or 16 and download mineacademy.org/nashorn): " + javascript);

			return javascript;
		}

		try {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			if (replacements != null)
				for (final Map.Entry<String, Object> replacement : replacements.entrySet())
					engine.put(replacement.getKey(), replacement.getValue());

			return engine.eval(javascript);

		} catch (final ScriptException ex) {
			throw new RuntimeException("Script execution failed for '" + javascript + "'", ex);
		}
	}
}