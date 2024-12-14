package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;

import lombok.NonNull;

/**
 * An engine that compiles and executes JavaScript code on the fly.
 * We automatically download the Nashorn library for Java 15 and up.
 *
 * See https://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
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
		ScriptEngine scriptEngine = null;

		boolean knownBug = false;

		try {
			scriptEngine = engineManager.getEngineByName("Nashorn");

		} catch (final ExceptionInInitializerError ex) {
			final Throwable cause = ex.getCause();

			if (cause instanceof NullPointerException && cause.toString().contains("java.lang.invoke.MethodHandle.type()")) {
				CommonCore.logFramed(
						"",
						"FATAL ERROR LOADING JAVASCRIPT ENGINE",
						"",
						"If you see 'Cannot set JUL log level through log4j-api: ignoring call...' above,",
						"that means your server version is not compatible with nashorn-core library we use",
						"to execute JavaScript code, such as in your variables or operators.",
						"",
						"THIS IS NOT OUR BUG - DO NOT REPORT TO US",
						"",
						"We are waiting for nashorn-core to update - please do not report it to us.",
						"See: https://github.com/PaperMC/Velocity/issues/1462 for more information",
						"",
						"Temporary solutions:",
						"1. If you're on Velocit, run your server with the following system property:",
						"   -Dlog4j2.julLoggerAdapter=org.apache.logging.log4j.jul.CoreLoggerAdapter",
						"",
						"   See https://docs.papermc.io/paper/reference/system-properties for how to do so.",
						"",
						"2. Or, if you can't do the above, downgrade to Velocity build 446 temporarily:",
						"   https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/446/downloads/velocity-3.4.0-SNAPSHOT-446.jar");

				knownBug = true;

			} else
				ex.printStackTrace();
		}

		if (!knownBug) {

			// Workaround for newer Minecraft releases
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
				final List<String> warningMessage = CommonCore.newList(
						"ERROR: JavaScript placeholders will not function!",
						"",
						"Your Java version/distribution lacks the",
						"Nashorn library for JavaScript placeholders.");

				if (CommonCore.getJavaVersion() >= 15)
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

		} else
			engine = null;
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
	 * @param audience
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull String javascript, final FoundationPlayer audience) throws FoScriptException {
		return run(javascript, audience, new HashMap<>());
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param audience
	 * @param placeholders
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull String javascript, final FoundationPlayer audience, Map<String, Object> placeholders) throws FoScriptException {
		if (placeholders == null)
			placeholders = new HashMap<>();

		if (audience == null && javascript.contains("player.")) {
			CommonCore.warning("Not running JavaScript because it contains 'player' but player was not provided. Script: " + javascript);

			return false;
		}

		if (audience != null && audience.isDiscord() && javascript.contains("player.")) {
			CommonCore.warning("Not running JavaScript because it contains 'player' but player was on Discord. Set Sender_Condition to '{sender_is_player}' to remove this warning next to your code. Script: " + javascript);

			return false;
		}

		// Find and replace all {syntax} variables since they were not replaced for Discord
		if (audience == null || audience.isDiscord()) {

			// Replace by line to avoid the {...} in "function() { return false; }" being replaced to "function() false"
			final String[] copy = javascript.split("\n");
			final String[] replaced = new String[copy.length];

			for (int i = 0; i < copy.length; i++) {
				String line = copy[i];
				final Matcher matcher = Variables.BRACKET_VARIABLE_PATTERN.matcher(line);

				while (matcher.find())
					line = line.replace(matcher.group(), "false");

				replaced[i] = line;
			}

			javascript = String.join("\n", replaced);
		}

		if (audience != null) {
			placeholders.put("audience", audience);
			placeholders.put("sender", audience.getSender());

			if (audience.isDiscord())
				placeholders.put("discord", audience.getSender());

			if (audience.isPlayer())
				placeholders.put("player", audience.getPlayer());
		}

		return run(javascript, placeholders);
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param placeholders
	 * @return
	 * @throws FoScriptException
	 */
	public static Object run(@NonNull String javascript, Map<String, Object> placeholders) throws FoScriptException {
		if (engine == null) {
			CommonCore.warning("Not running JavaScript code because nashorn-core library is missing (see earlier logs for details). Ignoring code: " + javascript);

			return null;
		}

		synchronized (engine) {
			if (placeholders == null)
				placeholders = new HashMap<>();

			// Speed up
			if (javascript.equals("true") || javascript.equals("!false") || javascript.equals("yes"))
				return true;

			if (javascript.equals("false") || javascript.equals("!true") || javascript.equals("no"))
				return false;

			// CLear past variables
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			// Put new variables
			for (final Map.Entry<String, Object> placeholder : placeholders.entrySet()) {
				final String key = placeholder.getKey();
				ValidCore.checkNotNull(key, "Key can't be null in javascript placeholders for code " + javascript + ": " + placeholders);

				final Object value = placeholder.getValue();
				ValidCore.checkNotNull(value, "Value can't be null in javascript placeholders for key " + key + ": " + placeholders);

				engine.put(key, value);
			}

			try {
				final Object result = engine.eval(javascript);

				if (result instanceof String) {
					String string = ((String) result).trim().toLowerCase();
					final char startChar = string.charAt(0);
					final char endChar = string.charAt(string.length() - 1);

					if (startChar == '"' || startChar == '\'')
						string = string.substring(1);

					if (endChar == '"' || endChar == '\'')
						string = string.substring(0, string.length() - 1);

					if (string.equals("true"))
						return true;

					else if (string.equals("false"))
						return false;
				}

				return result;

			} catch (ClassCastException | ScriptException ex) {

				// Special support for throwing exceptions in the JS code so that users
				// can send messages to player directly if upstream supports that
				final String cause = ex.getCause() != null ? ex.getCause().toString() : "";

				if (ex.getCause() != null && cause.contains("event handled")) {
					final String[] errorMessageSplit = cause.contains("event handled: ") ? cause.split("event handled\\: ") : new String[0];
					final Object sender = placeholders.get("player");

					if (errorMessageSplit.length == 2 && sender != null)
						Platform.toPlayer(sender).sendMessage(SimpleComponent.fromMiniAmpersand(errorMessageSplit[1]));

					throw new EventHandledException(true);
				}

				final String message = ex.toString();
				final List<String> errorMessage = CommonCore.newList("Error parsing JavaScript!");

				if (message.contains("Cannot cast org.openjdk.nashorn.internal.runtime.Undefined to org.bukkit.Statistic"))
					errorMessage.add("Your code uses invalid Statistic enum for your MC version. Do NOT report this, check Bukkit Javadocs.");

				if (message.contains("ReferenceError:") && message.contains("is not defined"))
					errorMessage.add("Invalid or unparsed variable!");

				if (message.contains("TypeError:") && message.contains("player.getName is not a function") && Platform.getPlatformName().contains("Velocity"))
					errorMessage.add("On Velocity, use player.getUsername() instead of player.getName()");

				if (ex instanceof ScriptException)
					errorMessage.add("Line: " + ((ScriptException) ex).getLineNumber() + ". Error: " + ex.getMessage());
				else
					errorMessage.add("Error: " + ex.getMessage());

				throw new FoScriptException(String.join(" ", errorMessage), javascript, ex instanceof ScriptException ? ((ScriptException) ex).getLineNumber() : -1, ex);
			}
		}
	}
}