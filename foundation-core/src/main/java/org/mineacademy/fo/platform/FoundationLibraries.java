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

		// These only download when the server lacks Adventure, i.e. when we send through
		// the adventure-platform 4.4.1 adapters. Those adapters are compiled against the
		// 4.x line and silently drop messages on 5.x, whose Audience defaults are no-ops.
		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.audience.Audience"))
			plugin.loadLibrary("net.kyori", "adventure-api", "4.26.1");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-plain", "4.26.1");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-legacy", "4.26.1");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-gson", "4.26.1");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-bungeecord", "4.4.1");
	}
}
