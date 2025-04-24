package org.mineacademy.fo.platform;

import org.mineacademy.fo.CommonCore;
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
		plugin.loadLibrary("org.snakeyaml", "snakeyaml-engine", "2.9");

		if (CommonCore.getJavaVersion() >= 15 && !ReflectionUtil.isClassAvailable("org.openjdk.nashorn.api.scripting.NashornScriptEngine"))
			plugin.loadLibrary("org.openjdk.nashorn", "nashorn-core", "15.6");

		if (!ReflectionUtil.isClassAvailable("com.google.gson.Gson"))
			plugin.loadLibrary("com.google.code.gson", "gson", "2.13.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.audience.Audience"))
			plugin.loadLibrary("net.kyori", "adventure-api", "4.20.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-plain", "4.20.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-legacy", "4.20.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-gson", "4.20.0");

		if (!ReflectionUtil.isClassAvailable("net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer"))
			plugin.loadLibrary("net.kyori", "adventure-text-serializer-bungeecord", "4.3.4");
	}
}
