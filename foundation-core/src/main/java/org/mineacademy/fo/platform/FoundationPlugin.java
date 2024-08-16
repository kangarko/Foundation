package org.mineacademy.fo.platform;

import java.io.File;

import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.proxy.ProxyListener;

public interface FoundationPlugin {

	void disable();

	File getDataFolder();

	SimpleCommandGroup getDefaultCommandGroup();

	ProxyListener getDefaultProxyListener();

	File getFile();

	String getName();

	ClassLoader getPluginClassLoader();

	String getVersion();

	boolean isEnabled();

	boolean isReloading();

	boolean isRegexCaseInsensitive();

	boolean isRegexStrippingAccents();

	boolean isRegexStrippingColors();

	boolean isRegexUnicode();

	boolean isSimilarityStrippingAccents();

	void loadLibrary(String groupId, String artifactId, String version);

	void reload();

	int getFoundedYear();

	String getAuthors();

	void setDefaultCommandGroup(SimpleCommandGroup group);

	void registerCommands(SimpleCommandGroup group);

	void registerCommand(SimpleCommandCore instance);

	void setDefaultProxyListener(ProxyListener instance);

	Object getAdventure();
}
