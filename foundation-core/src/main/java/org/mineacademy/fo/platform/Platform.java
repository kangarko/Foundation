package org.mineacademy.fo.platform;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.model.Task;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEventSource;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Platform {

	private static FoundationPlatform instance;

	public static boolean callEvent(Object event) {
		return getInstance().callEvent(event);
	}

	public static void checkCommandUse(SimpleCommandCore command) {
		getInstance().checkCommandUse(command);
	}

	public static void closeAdventurePlatform() {
		getInstance().closeAdventurePlatform();
	}

	public static HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack) {
		return getInstance().convertItemStackToHoverEvent(itemStack);
	}

	public static void dispatchCommand(Audience sender, String command) {
		getInstance().dispatchCommand(sender, command);
	}

	public static void dispatchConsoleCommand(String command) {
		getInstance().dispatchConsoleCommand(command);
	}

	private static FoundationPlatform getInstance() {
		// Do not throw FoException to prevent race condition
		if (instance == null)
			throw new NullPointerException("Foundation instance not set yet.");

		return instance;
	}

	public static List<Audience> getOnlinePlayers() {
		return getInstance().getOnlinePlayers();
	}

	public static File getPluginFile(String pluginName) {
		return getInstance().getPluginFile(pluginName);
	}

	public static String getServerName() {
		return getInstance().getServerName();
	}

	public static List<String> getServerPlugins() {
		return getInstance().getServerPlugins();
	}

	public static String getServerVersion() {
		return getInstance().getServerVersion();
	}

	public static boolean hasHexColorSupport() {
		return getInstance().hasHexColorSupport();
	}

	public static boolean hasPermission(Audience audience, String permission) {
		return getInstance().hasPermission(audience, permission);
	}

	public static boolean isAsync() {
		return getInstance().isAsync();
	}

	public static boolean isConsole(Object audience) {
		return getInstance().isConsole(audience);
	}

	public static boolean isConversing(Audience audience) {
		return getInstance().isConversing(audience);
	}

	public static boolean isDiscord(Object audience) {
		return getInstance().isDiscord(audience);
	}

	public static boolean isOnline(Audience audience) {
		return getInstance().isOnline(audience);
	}

	public static boolean isPlaceholderAPIHooked() {
		return getInstance().isPlaceholderAPIHooked();
	}

	public static boolean isPluginInstalled(String name) {
		return getInstance().isPluginInstalled(name);
	}

	public static void logToConsole(String message) {
		getInstance().logToConsole(message);
	}

	public static void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases) {
		getInstance().registerCommand(command, unregisterOldCommand, unregisterOldAliases);
	}

	public static void registerEvents(Object listener) {
		getInstance().registerEvents(listener);
	}

	public static String resolveSenderName(Audience sender) {
		return getInstance().resolveSenderName(sender);
	}

	public static Task runTask(int delayTicks, Runnable runnable) {
		return getInstance().runTask(delayTicks, runnable);
	}

	public static Task runTaskAsync(int delayTicks, Runnable runnable) {
		return getInstance().runTaskAsync(delayTicks, runnable);
	}

	public static void sendActionBar(Audience audience, Component message) {
		getInstance().sendActionBar(audience, message);
	}

	public static void sendBossbarPercent(final Audience audience, final Component message, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		getInstance().sendBossbarPercent(audience, message, progress, color, overlay);
	}

	public static void sendBossbarTimed(final Audience audience, final Component message, final int seconds, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		getInstance().sendBossbarTimed(audience, message, seconds, progress, color, overlay);
	}

	public static void sendConversingMessage(Object conversable, Component message) {
		getInstance().sendConversingMessage(conversable, message);
	}

	public static void sendPluginMessage(UUID senderUid, String channel, byte[] message) {
		getInstance().sendPluginMessage(senderUid, channel, message);
	}

	public static void sendToast(Audience audience, Component message) {
		getInstance().sendToast(audience, message);
	}

	public static void setInstance(FoundationPlatform instance) {
		Platform.instance = instance;
	}

	public static void tell(Object sender, Component component, boolean skipEmpty) {
		getInstance().tell(sender, component, skipEmpty);
	}

	public static Set<Audience> toAudience(Collection<Object> players, boolean addConsole) {
		return getInstance().toAudience(players, addConsole);
	}

	public static Audience toAudience(Object sender) {
		return getInstance().toAudience(sender);
	}

	public static void unregisterCommand(SimpleCommandCore command) {
		getInstance().unregisterCommand(command);
	}

	public static FoundationPlugin getPlugin() {
		return getInstance().getPlugin();
	}

	public static String getNMSVersion() {
		return getInstance().getNMSVersion();
	}
}
