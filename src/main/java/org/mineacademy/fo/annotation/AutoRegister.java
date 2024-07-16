package org.mineacademy.fo.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.bukkit.event.Listener;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.event.SimpleListener;
import org.mineacademy.fo.model.DiscordListener;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.model.SimpleExpansion;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.settings.YamlConfig;

/**
 * Place this annotation over any of the following classes to make Foundation
 * automatically register it when the plugin starts, and properly reload it.
 *
 * Supported classes:
 * - {@link SimpleListener}
 * - {@link PacketListener}
 * - {@link ProxyListener}
 * - {@link DiscordListener}
 * - {@link SimpleCommand}
 * - {@link SimpleCommandGroup}
 * - {@link SimpleExpansion}
 * - {@link YamlConfig} (we will load your config when the plugin starts and reload it properly)
 * - any class that "implements {@link Listener}"
 *
 * In addition, the following classes will self-register automatically regardless
 * if you place this annotation on them or not:
 * - Tool (and its derivates such as Rocket)
 * - SimpleEnchantment
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AutoRegister {

	/**
	 * When false, we wont print console warnings such as that registration failed
	 * because the server runs outdated MC version (example: SimpleEnchantment) or lacks
	 * necessary plugins to be hooked into (example: DiscordListener, PacketListener)
	 *
	 * @return
	 */
	boolean hideIncompatibilityWarnings() default false;
}
