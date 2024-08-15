package org.mineacademy.fo.platform;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.model.Task;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEventSource;

public interface FoundationPlatform {

	boolean callEvent(Object event);

	void checkCommandUse(SimpleCommand command);

	void closeAdventurePlatform();

	HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack);

	void dispatchCommand(Audience sender, String command);

	void dispatchConsoleCommand(String command);

	List<Audience> getOnlinePlayers();

	FoundationPlugin getPlugin();

	File getPluginFile(String pluginName);

	String getServerName();

	List<String> getServerPlugins();

	String getServerVersion();

	boolean hasHexColorSupport();

	boolean hasPermission(Audience audience, String permission);

	boolean isAsync();

	boolean isConsole(Object audience);

	boolean isConversing(Audience audience);

	boolean isDiscord(Object audience);

	boolean isOnline(Audience audience);

	boolean isPlaceholderAPIHooked();

	boolean isPluginEnabled(String name);

	boolean isPluginReloading();

	void logToConsole(String message);

	void registerCommand(SimpleCommand command, boolean unregisterOldCommand, boolean unregisterOldAliases);

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

	void tell(Object sender, Component component, boolean skipEmpty);

	Set<Audience> toAudience(Collection<Object> players, boolean addConsole);

	Audience toAudience(Object sender);

	void unregisterCommand(SimpleCommand command);
}
