package org.mineacademy.fo.platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.LegacyEnumNameTranslator;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.BungeeCommandImpl;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.event.HoverEventSource;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.PluginManager;

/**
 * An implementation of {@link FoundationPlatform} for Bukkit.
 */
final class BungeePlatform extends FoundationPlatform {

	public static void inject() {
		Platform.setInstance(new BungeePlatform());
	}

	private BungeePlatform() {
		Platform.setType(Platform.Type.BUNGEECORD);

		Common.addSimplifier(object -> {
			if (object instanceof ProxiedPlayer)
				return ((ProxiedPlayer) object).getName();

			if (object instanceof Keyed)
				return ((Keyed) object).key().toString();

			return null;
		});

		ReflectionUtil.setLegacyEnumNameTranslator(new LegacyEnumNameTranslator() {

			@Override
			public <E> String translateName(Class<E> enumType, String name) {
				if (enumType == BossBar.Overlay.class)
					name = name.toUpperCase().replace("SEGMENTED", "NOTCHED").replace("SOLID", "PROGRESS");

				return name;
			}
		});
	}

	@Override
	public boolean callEvent(final Object event) {
		Valid.checkBoolean(event instanceof Event, "Object is not a bungee Event: " + event);
		final Event result = BungeePlugin.getServer().getPluginManager().callEvent((Event) event);

		return result instanceof Cancellable ? !((Cancellable) result).isCancelled() : true;
	}

	@Override
	public HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack) {
		throw new UnsupportedOperationException("Not supported in Velocity");
	}

	@Override
	protected void dispatchConsoleCommand0(String command) {
		final ProxyServer server = BungeePlugin.getServer();

		server.getPluginManager().dispatchCommand(server.getConsole(), command);
	}

	@Override
	public List<FoundationPlayer> getOnlinePlayers() {
		final List<FoundationPlayer> players = new ArrayList<>();

		for (final ProxiedPlayer player : Remain.getOnlinePlayers(false))
			players.add(this.toPlayer(player));

		return players;
	}

	@Override
	public String getPlatformName() {
		return BungeePlugin.getServer().getName();
	}

	@Override
	public String getPlatformVersion() {
		return BungeePlugin.getServer().getVersion();
	}

	@Override
	protected FoundationPlayer getPlayer(String name) {
		final ProxiedPlayer player = BungeePlugin.getServer().getPlayer(name);

		return player != null ? toPlayer(player) : null;
	}

	@Override
	protected FoundationPlayer getPlayer(UUID uniqueId) {
		final ProxiedPlayer player = BungeePlugin.getServer().getPlayer(uniqueId);

		return player != null && player.isConnected() ? toPlayer(player) : null;
	}

	@Override
	public FoundationPlugin getPlugin() {
		return BungeePlugin.getInstance();
	}

	@Override
	public File getPluginFile(String pluginName) {
		return BungeePlugin.getInstance().getFile();
	}

	@Override
	public List<Tuple<String, String>> getPlugins() {
		return Common.convertList(BungeePlugin.getServer().getPluginManager().getPlugins(), plugin -> new Tuple<>(plugin.getDescription().getName(), plugin.getDescription().getVersion()));
	}

	@Override
	public FoundationServer getServer(String name) {
		final ServerInfo server = Remain.getServer(name);

		return server != null ? new BungeeServer(server) : null;
	}

	@Override
	public List<FoundationServer> getServers() {
		return Common.convertList(Remain.getServers(), server -> new BungeeServer(server));
	}

	@Override
	public boolean isAsync() {
		return true;
	}

	@Override
	public boolean isPluginInstalled(String name) {
		return BungeePlugin.getServer().getPluginManager().getPlugin(name) != null;
	}

	@Override
	public void log(String message) {
		BungeePlugin.getServer().getConsole().sendMessage(message);
	}

	@Override
	public void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases) {
		final PluginManager manager = BungeePlugin.getServer().getPluginManager();
		Command oldCommand = null;

		for (final Entry<String, Command> entry : manager.getCommands())
			if (entry.getKey().equals(command.getLabel())) {
				oldCommand = entry.getValue();

				break;
			}

		if (oldCommand != null && unregisterOldCommand)
			manager.unregisterCommand(oldCommand);

		if (unregisterOldAliases)
			for (final String alias : command.getAliases()) {
				Command oldAlias = null;

				for (final Entry<String, Command> entry : manager.getCommands())
					if (entry.getKey().equals(alias)) {
						oldAlias = entry.getValue();

						break;
					}

				if (oldAlias != null)
					manager.unregisterCommand(oldAlias);
			}

		manager.registerCommand(BungeePlugin.getInstance(), new BungeeCommandImpl(command));
	}

	@Override
	public void registerDefaultPlatformSubcommands(SimpleCommandGroup group) {
		// Not supported in Velocity
	}

	@Override
	public void registerEvents(@NonNull Object listener) {
		Valid.checkBoolean(listener instanceof Listener, "To register events you must make " + listener.getClass() + " implements Listener");

		BungeePlugin.getServer().getPluginManager().registerListener(BungeePlugin.getInstance(), (Listener) listener);
	}

	@Override
	public Task runTask(int delayTicks, Runnable runnable) {
		return Remain.runTaskAsync(delayTicks, runnable); // On velocity all tasks are async
	}

	@Override
	public Task runTaskAsync(int delayTicks, Runnable runnable) {
		return Remain.runTaskAsync(delayTicks, runnable);
	}

	@Override
	public Task runTaskTimer(int delayTicks, int repeatTicks, Runnable runnable) {
		return Remain.runTaskTimerAsync(delayTicks, repeatTicks, runnable); // On velocity all tasks are async
	}

	@Override
	public Task runTaskTimerAsync(int delayTicks, int repeatTicks, Runnable runnable) {
		return Remain.runTaskTimerAsync(delayTicks, repeatTicks, runnable);
	}

	@Override
	public void sendPluginMessage(UUID senderUid, String channel, byte[] array) {
		final ProxiedPlayer player = Remain.getPlayer(senderUid, false);
		Valid.checkNotNull(player, "Unable to find player by UUID: " + senderUid);

		player.sendData(BungeePlugin.BUNGEE_CHANNEL, array);
	}

	@Override
	public FoundationPlayer toPlayer(Object sender) {
		if (sender instanceof FoundationPlayer)
			return (FoundationPlayer) sender;

		if (sender == null)
			throw new FoException("Cannot convert null sender to FoundationPlayer!");

		if (!(sender instanceof CommandSender))
			throw new FoException("Can only convert CommandSender to FoundationPlayer, got " + sender.getClass().getSimpleName() + ": " + sender);

		return new BungeePlayer((CommandSender) sender);
	}

	@Override
	public FoundationServer toServer(Object server) {
		if (server instanceof FoundationServer)
			return (FoundationServer) server;

		else if (server instanceof ServerInfo)
			return new BungeeServer((ServerInfo) server);

		else if (server instanceof Server)
			return new BungeeServer(((Server) server).getInfo());

		else
			throw new FoException("Cannot convert " + server + " to FoundationServer, only ServerInfo or Server are supported.");
	}

	@Override
	public void unregisterCommand(SimpleCommandCore command) {
		final PluginManager manager = BungeePlugin.getServer().getPluginManager();
		Command bungeeCommand = null;

		for (final Entry<String, Command> entry : manager.getCommands())
			if (entry.getKey().equals(command.getLabel())) {
				bungeeCommand = entry.getValue();

				break;
			}

		manager.unregisterCommand(bungeeCommand);
	}
}