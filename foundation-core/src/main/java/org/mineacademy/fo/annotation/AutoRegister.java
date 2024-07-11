package org.mineacademy.fo.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation that instructs Foundation to automatically register the following
 * classes when the plugin starts or is reloaded:
 *
 * - SimpleListener on Bukkit
 * - PacketListener
 * - ProxyListener
 * - DiscordListener
 * - SimpleCommand
 * - SimpleCommandGroup
 * - SimpleExpansion
 * - YamlConfig (we will load your config when the plugin starts and reload it properly)
 * - any class that "implements Listener" on Bukkit
 *
 * These classes must be made final. Some of them must have a public no argumnets
 * constructor or be a singleton, you will be notified in the console about this.
 *
 * In addition, the following classes will self-register automatically regardless
 * if you place this annotation on them or not:
 *
 * - Tool (and its children such as Rocket)
 * - SimpleEnchantment
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AutoRegister {

	/**
	 * When false, we won't print console warnings such as that registration failed
	 * because the server is outdated or lacks the necessary plugins to be hooked into
	 * (example: PacketListener needs ProtocolLib)
	 *
	 * @return
	 */
	boolean hideIncompatibilityWarnings() default false;
}
