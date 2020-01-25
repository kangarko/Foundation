package org.mineacademy.fo.model;

import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.NonNull;

/**
 * An engine that compiles and executes code on the fly.
 *
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

		final ScriptEngineManager engineManager = new ScriptEngineManager();
		engine = engineManager.getEngineByName("Nashorn");

		if (engine == null)
			Common.logFramed(false,
					"JavaScript placeholders will not function!",
					"",
					"Your Java version/distribution lacks",
					"the Nashorn library for JavaScript",
					"placeholders. Ensure you have Oracle",
					"Java 8.");
	}

	/**
	 * Return true if the JavaScript library is loaded successfuly and may be used
	 *
	 * @return
	 */
	public static boolean isEngineLoaded() {
		return engine != null;
	}

	/**
	 * Compiles and executes the given JavaScript code
	 *
	 * @param javascript
	 * @return
	 */
	public static Object run(String javascript) {
		return run(javascript, null, null);
	}

	/**
	 * Runs the given JavaScript code for the player,
	 * making the "player" variable in the code usable
	 *
	 * @param javascript
	 * @param player
	 * @return
	 */
	public static Object run(String javascript, Player player) {
		return run(javascript, player, null);
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param player
	 * @param event
	 * @return
	 */
	public static Object run(@NonNull String javascript, Player player, Event event) {

		if (!isEngineLoaded())
			return null;

		try {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			if (player != null)
				engine.put("player", player);

			if (event != null)
				engine.put("event", event);

			return engine.eval(javascript);

		} catch (final ScriptException ex) {
			Common.error(ex,
					"Script executing failed!",
					"Script: " + javascript,
					"%error");

			return null;
		}
	}

	/**
	 * Executes the Javascript code with the given variables - you have to handle the error yourself
	 *
	 * @param javascript
	 * @param cast
	 * @param replacements
	 *
	 * @return
	 *
	 * @throws ScriptException
	 */
	public static Object run(String javascript, Map<String, Object> replacements) throws ScriptException {
		if (!isEngineLoaded())
			return null;

		engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

		if (replacements != null)
			for (final Map.Entry<String, Object> replacement : replacements.entrySet())
				engine.put(replacement.getKey(), replacement.getValue());

		return engine.eval(javascript);
	}
}
