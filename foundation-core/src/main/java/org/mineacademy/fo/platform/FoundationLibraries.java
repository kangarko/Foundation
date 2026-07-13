package org.mineacademy.fo.platform;

import org.mineacademy.fo.ReflectionUtil;

/**
 * Literally the only reason for this class is not to include anything else in the class
 * path that might cause no class def error.
 */
final class FoundationLibraries {

	/**
	 * Load libraries for the platform's plugin.
	 *
	 * @param plugin
	 */
	public static void load(final FoundationPlugin plugin) {
		plugin.loadLibrary("org.snakeyaml", "snakeyaml-engine", "3.0.1");

		// Java 15 removed the bundled Nashorn engine. Foundation requires Java 21+
		// so the standalone Nashorn JAR is always needed for JS chat-format conditions.
		if (!ReflectionUtil.isClassAvailable("org.openjdk.nashorn.api.scripting.NashornScriptEngine"))
			plugin.loadLibrary("org.openjdk.nashorn", "nashorn-core", "15.7");

		if (!ReflectionUtil.isClassAvailable("com.google.gson.Gson"))
			plugin.loadLibrary("com.google.code.gson", "gson", "2.14.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.audience.Audience"))
			plugin.loadLibrary("net.kyori", "adventure-api", "5.2.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-plain", "5.2.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-legacy", "5.2.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-gson", "5.2.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-bungeecord", "4.4.1");
	}
}
