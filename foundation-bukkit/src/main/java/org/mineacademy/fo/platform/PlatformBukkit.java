package org.mineacademy.fo.platform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.command.BukkitCommandImpl;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.RemainCore;
import org.mineacademy.fo.remain.internal.BossBarInternals;
import org.mineacademy.fo.settings.SimpleLocalization;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEventSource;

public class PlatformBukkit implements FoundationPlatform {

	/**
	 * The Adventure platform
	 */
	private final BukkitAudiences adventure;

	public PlatformBukkit(Plugin plugin) {
		this.adventure = BukkitAudiences.create(plugin);
	}

	@Override
	public boolean callEvent(final Object event) {
		Valid.checkBoolean(event instanceof Event, "Object must be an instance of Bukkit Event, not " + event.getClass());

		Bukkit.getPluginManager().callEvent((Event) event);
		return event instanceof Cancellable ? !((Cancellable) event).isCancelled() : true;
	}

	/**
	 * Closes the adventure platform, if we have one
	 */
	@Override
	public void closeAdventurePlatform() {
		if (this.adventure != null)
			this.adventure.close();
	}

	@Override
	public void dispatchCommand(Audience sender, String command) {
		if (sender instanceof Player)
			((Player) sender).performCommand(command);

		else
			dispatchConsoleCommand(command);
	}

	@Override
	public void dispatchConsoleCommand(String command) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
	}

	@Override
	public List<Audience> getOnlinePlayers() {
		final List<Audience> players = new ArrayList<>();

		for (final Player player : Remain.getOnlinePlayers())
			players.add(this.adventure.player(player));

		return players;
	}

	@Override
	public File getPluginFile(String pluginName) {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
		Valid.checkNotNull(plugin, "Plugin " + pluginName + " not found!");
		Valid.checkBoolean(plugin instanceof JavaPlugin, "Plugin " + pluginName + " is not a JavaPlugin. Got: " + plugin.getClass());

		return (File) ReflectionUtil.invoke(ReflectionUtil.getMethod(JavaPlugin.class, "getFile"), plugin);
	}

	@Override
	public String getServerName() {
		return Bukkit.getName();
	}

	@Override
	public String getServerVersion() {
		return Bukkit.getVersion();
	}

	@Override
	public String getNMSVersion() {
		final String packageName = Bukkit.getServer() == null ? "" : Bukkit.getServer().getClass().getPackage().getName();
		final String curr = packageName.substring(packageName.lastIndexOf('.') + 1);

		return !"craftbukkit".equals(curr) && !"".equals(packageName) ? curr : "";
	}

	@Override
	public List<String> getServerPlugins() {
		return Common.convert(Bukkit.getPluginManager().getPlugins(), Plugin::getName);
	}

	@Override
	public boolean isDiscord(Object audience) {
		return audience instanceof DiscordSender;
	}

	@Override
	public boolean hasHexColorSupport() {
		return MinecraftVersion.atLeast(V.v1_16);
	}

	@Override
	public boolean hasPermission(Audience audience, String permission) {
		return audience instanceof Permissible && ((Permissible) audience).hasPermission(permission);
	}

	@Override
	public boolean isConversing(Audience audience) {
		return audience instanceof Conversable && ((Conversable) audience).isConversing();
	}

	@Override
	public boolean isOnline(Audience audience) {
		return (audience instanceof Player && ((Player) audience).isOnline()) || audience instanceof ConsoleCommandSender;
	}

	/**
	 * Checks if a plugin is enabled. We also schedule an async task to make
	 * sure the plugin is loaded correctly when the server is done booting
	 * <p>
	 * Return true if it is loaded (this does not mean it works correctly)
	 *
	 * @param name
	 * @return
	 */
	@Override
	public boolean isPluginEnabled(String name) {
		Plugin lookup = null;

		for (final Plugin otherPlugin : Bukkit.getPluginManager().getPlugins())
			if (otherPlugin.getDescription().getName().equals(name)) {
				lookup = otherPlugin;

				break;
			}

		final Plugin found = lookup;

		if (found == null)
			return false;

		if (!found.isEnabled())
			Common.runLaterAsync(0, () -> Valid.checkBoolean(found.isEnabled(), SimplePlugin.getNamed() + " could not hook into " + name + " as the plugin is disabled! (DO NOT REPORT THIS TO " + SimplePlugin.getNamed() + ", look for errors above and contact support of '" + name + "')"));

		return true;
	}

	@Override
	public void logToConsole(String message) {
		Bukkit.getConsoleSender().sendMessage(message);
	}

	@Override
	public void registerEvents(final Object listener) {
		Valid.checkBoolean(listener instanceof Listener, "Listener must extend Bukkit's Listener, not " + listener.getClass());

		Bukkit.getPluginManager().registerEvents((Listener) listener, SimplePlugin.getInstance());
	}

	@Override
	public String resolveSenderName(Audience sender) {
		return sender instanceof ConsoleCommandSender ? SimpleLocalization.CONSOLE_NAME : sender != null ? sender instanceof CommandSender ? ((CommandSender) sender).getName() : "" : "";
	}

	@Override
	public Task runTask(int delayTicks, Runnable runnable) {
		return Common.runLater(delayTicks, runnable);
	}

	@Override
	public Task runTaskAsync(int delayTicks, Runnable runnable) {
		return Common.runLaterAsync(delayTicks, runnable);
	}

	@Override
	public void sendActionBar(Audience audience, Component message) {
		Remain.sendActionBar(audience, message);
	}

	@Override
	public void sendBossbarPercent(Audience audience, Component message, float progress, Color color, Overlay overlay) {
		if (MinecraftVersion.atLeast(V.v1_9)) {
			audience.showBossBar(BossBar.bossBar(message, progress, CommonCore.getOrDefault(color, BossBar.Color.WHITE), CommonCore.getOrDefault(overlay, BossBar.Overlay.PROGRESS)));

		} else if (audience instanceof Player)
			BossBarInternals.getInstance().sendMessage((Player) audience, RemainCore.convertAdventureToLegacy(message), progress, color, overlay);

		else
			audience.sendMessage(message);
	}

	@Override
	public void sendBossbarTimed(Audience audience, Component message, int secondsToShow, float progress, Color color, Overlay overlay) {
		if (MinecraftVersion.atLeast(V.v1_9)) {
			final BossBar bar = BossBar.bossBar(message, progress, CommonCore.getOrDefault(color, BossBar.Color.WHITE), CommonCore.getOrDefault(overlay, BossBar.Overlay.PROGRESS));

			audience.showBossBar(bar);
			Common.runLater(secondsToShow * 20, () -> audience.hideBossBar(bar));

		} else if (audience instanceof Player)
			BossBarInternals.getInstance().sendTimedMessage((Player) audience, RemainCore.convertAdventureToLegacy(message), secondsToShow, progress, color, overlay);
		else
			audience.sendMessage(message);
	}

	@Override
	public void sendConversingMessage(Object conversable, Component message) {
		((Conversable) conversable).sendRawMessage(Remain.convertAdventureToLegacy(message));
	}

	@Override
	public void sendPluginMessage(UUID senderUid, String channel, byte[] array) {
		final Player player = Remain.getPlayerByUUID(senderUid);
		Valid.checkNotNull(player, "Unable to find player by UUID: " + senderUid);

		player.sendPluginMessage(SimplePlugin.getInstance(), channel, array);
	}

	@Override
	public void sendToast(Audience audience, Component message) {
		Remain.sendToast(audience, message);
	}

	@Override
	public void tell(Object sender, Component component, boolean skipEmpty) {
		Remain.tell(sender, component, skipEmpty);
	}

	@Override
	public HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack) {
		ValidCore.checkBoolean(itemStack instanceof ItemStack, "Expected item stack, got: " + itemStack);

		return Remain.convertItemStackToHoverEvent((ItemStack) itemStack);
	}

	@Override
	public Set<Audience> toAudience(Collection<Object> players, boolean addConsole) {
		final Set<Audience> audiences = new HashSet<>();

		for (final Object player : players)
			if (player instanceof Audience)
				audiences.add((Audience) player);
			else if (player instanceof CommandSender)
				audiences.add(this.adventure.sender((CommandSender) player));

		if (addConsole)
			audiences.add(this.adventure.console());

		return audiences;
	}

	@Override
	public Audience toAudience(Object sender) {
		if (sender instanceof Audience)
			return (Audience) sender;

		if (sender instanceof CommandSender)
			return this.adventure.sender((CommandSender) sender);

		return null;
	}

	@Override
	public void checkCommandUse(SimpleCommandCore command) {
		// Navigate developers on proper simple command class usage.
		ValidCore.checkBoolean(!(command instanceof CommandExecutor), "Please do not write 'implements CommandExecutor' for /" + command + " command since it's already registered.");
		ValidCore.checkBoolean(!(command instanceof TabCompleter), "Please do not write 'implements TabCompleter' for /" + command + " command, simply override the tabComplete() method");
	}

	@Override
	public void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases) {
		final PluginCommand oldCommand = Bukkit.getPluginCommand(command.getLabel());

		if (oldCommand != null && unregisterOldCommand)
			Remain.unregisterCommand(oldCommand.getLabel(), unregisterOldAliases);

		Remain.registerCommand(new BukkitCommandImpl(command));
	}

	@Override
	public void unregisterCommand(SimpleCommandCore command) {
		Remain.unregisterCommand(command.getLabel());
	}

	@Override
	public boolean isConsole(Object audience) {
		return audience instanceof ConsoleCommandSender;
	}

	@Override
	public boolean isPlaceholderAPIHooked() {
		return HookManager.isPlaceholderAPILoaded();
	}

	@Override
	public boolean isAsync() {
		return !Bukkit.isPrimaryThread();
	}

	@Override
	public boolean isPluginReloading() {
		return SimplePlugin.isReloading();
	}

	@Override
	public FoundationPlugin getPlugin() {
		return SimplePlugin.getInstance();
	}
}