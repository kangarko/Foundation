package org.mineacademy.fo.platform;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.proxy.ProxyListener;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEventSource;

public interface FoundationPlatform {

	boolean callEvent(Object event);

	void closeAdventurePlatform();

	void disablePlugin();

	void dispatchCommand(Audience sender, String command);

	void dispatchConsoleCommand(String command);

	ProxyListener getDefaultProxyListener();

	SimpleCommandGroup getDefaultCommandGroup();

	List<Audience> getOnlinePlayers();

	File getPluginFile();

	File getPluginFile(String pluginName);

	File getPluginFolder();

	String getPluginName();

	String getPluginVersion();

	String getServerName();

	String getServerVersion();

	List<String> getServerPlugins();

	ClassLoader getPluginClassLoader();

	boolean hasHexColorSupport();

	boolean hasPermission(Audience audience, String permission);

	boolean isConversing(Audience audience);

	boolean isDiscord(Object audience);

	boolean isConsole(Object audience);

	boolean isOnline(Audience audience);

	boolean isPluginReloading();

	boolean isPluginEnabled();

	boolean isPluginEnabled(String name);

	boolean isRegexCaseInsensitive();

	boolean isRegexStrippingAccents();

	boolean isRegexStrippingColors();

	boolean isRegexUnicode();

	boolean isSimilarityStrippingAccents();

	boolean isPlaceholderAPIHooked();

	String getDefaultCommandLabel();

	void logToConsole(String message);

	void registerEvents(Object listener);

	String resolveSenderName(Audience sender);

	Task runTask(int delayTicks, Runnable runnable);

	Task runTaskAsync(int delayTicks, Runnable runnable);

	void sendActionBar(Audience audience, Component message);

	void sendBossbarPercent(final Audience audience, final Component message, final float progress, final BossBar.Color color, final BossBar.Overlay overlay);

	void sendBossbarTimed(final Audience audience, final Component message, final int seconds, final float progress, final BossBar.Color color, final BossBar.Overlay overlay);

	void sendConversingMessage(Object conversable, Component message);

	void sendPluginMessage(UUID senderUid, String channel, byte[] array);

	void sendToast(Audience audience, Component message);

	Set<Audience> toAudience(Collection<Object> players, boolean addConsole);

	Audience toAudience(Object sender);

	void tell(Object sender, Component component, boolean skipEmpty);

	HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack);

	void checkCommandUse(SimpleCommand command);

	void registerCommand(SimpleCommand command, boolean unregisterOldCommand, boolean unregisterOldAliases);

	void unregisterCommand(SimpleCommand command);

	void loadLibrary(String groupId, String artifactId, String version);

	boolean isAsync();
}
