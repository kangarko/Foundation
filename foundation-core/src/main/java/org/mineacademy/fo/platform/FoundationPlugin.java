package org.mineacademy.fo.platform;

import java.io.File;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommandCore;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.message.OutgoingMessage;

/**
 * Represents a plugin using Foundation
 */
public interface FoundationPlugin {

	/**
	 * Disable this plugin.
	 */
	void disable();

	/**
	 * Return authors of the plugin, comma separated.
	 *
	 * @return
	 */
	String getAuthors();

	/**
	 * Used for plugin update notifications, return -1 if unset otherwise
	 * return your BuiltByBit.com plugin ID (get it from the URL of your Overview page)
	 *
	 * @return
	 */
	int getBuiltByBitId();

	/**
	 * Override this from {@link #getBuiltByBitId()} to work.
	 * See <a href="https://builtbybit.com/account/api">https://builtbybit.com/account/api</a> for more information.
	 *
	 * @return
	 */
	String getBuiltByBitSharedToken();

	/**
	 * Return the data folder of this plugin.
	 *
	 * @return
	 */
	File getDataFolder();

	/**
	 * Return the default command group, or null if not set.
	 *
	 * @see ProxyListener
	 * @see AutoRegister
	 *
	 * @return
	 */
	SimpleCommandGroup getDefaultCommandGroup();

	/**
	 * Return the default proxy listener, or null if not set.
	 *
	 * @see ProxyListener
	 * @see AutoRegister
	 *
	 * @return
	 */
	ProxyListener getDefaultProxyListener();

	/**
	 * Return the jar file of this plugin.
	 *
	 * @return
	 */
	File getFile();

	/**
	 * Return the founded year of this plugin.
	 *
	 * This is shown in {@link SimpleCommandGroup} on help.
	 *
	 * @return
	 */
	int getFoundedYear();

	/**
	 * Get the name of this plugin.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Return the class loader of this plugin.
	 *
	 * @return
	 */
	ClassLoader getPluginClassLoader();

	/**
	 * Return the Sentry DSN for this plugin used for error reporting.
	 *
	 * @return
	 */
	String getSentryDsn();

	/**
	 * Return the plugin's version.
	 *
	 * @return
	 */
	String getVersion();

	/**
	 * Return true if the plugin is enabled.
	 *
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Should Pattern.CASE_INSENSITIVE be applied when compiling regular expressions in the Common class?
	 *
	 * May impose a slight performance penalty but increases catches.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	boolean isRegexCaseInsensitive();

	/**
	 * Should we remove diacritical marks before matching regex?
	 * Defaults to true.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	boolean isRegexStrippingAccents();

	/**
	 * Strip colors from checked message while checking it against a regex?
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	boolean isRegexStrippingColors();

	/**
	 * Should Pattern.UNICODE_CASE be applied when compiling regular expressions in the Common class?
	 *
	 * May impose a slight performance penalty but useful for non-English servers.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	boolean isRegexUnicode();

	/**
	 * Should we replace accents with their non accented friends when
	 * checking two strings for similarity in {@link ChatUtil}?
	 *
	 * @return defaults to true
	 */
	boolean isSimilarityStrippingAccents();

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 *
	 * If the provided library has any relocations, they will be applied to
	 * create a relocated jar and the relocated jar will be loaded instead.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	void loadLibrary(String groupId, String artifactId, String version);

	/**
	 * Convenience method for registering a command.
	 *
	 * @see SimpleCommandCore#register()
	 *
	 * @param command
	 */
	void registerCommand(SimpleCommandCore command);

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register()}
	 *
	 * @param group
	 */
	void registerCommands(SimpleCommandGroup group);

	/**
	 * Reload this plugin's settings files.
	 */
	void reload();

	/**
	 * Set the default command group used in registering a {@link SimpleSubCommandCore} using {@link AutoRegister}
	 * annotation when no group is provided in its constructor.
	 *
	 * @param group
	 */
	void setDefaultCommandGroup(SimpleCommandGroup group);

	/**
	 * Set the default proxy used in {@link OutgoingMessage} when no group is provided.
	 *
	 * @param listener
	 */
	void setDefaultProxyListener(ProxyListener listener);
}
