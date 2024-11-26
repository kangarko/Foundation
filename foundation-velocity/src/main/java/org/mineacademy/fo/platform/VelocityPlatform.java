package org.mineacademy.fo.platform;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.LegacyEnumNameTranslator;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.VelocityCommandImpl;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.ReflectionException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.Remain;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.event.HoverEventSource;

/**
 * An implementation of {@link FoundationPlatform} for Bukkit.
 */
final class VelocityPlatform extends FoundationPlatform {

	public static void inject() {
		Platform.setInstance(new VelocityPlatform());
	}

	private VelocityPlatform() {
		Platform.setType(Platform.Type.VELOCITY);

		CommonCore.addSimplifier(object -> {
			if (object instanceof Player)
				return ((Player) object).getUsername();

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
		final Object futureEvent = VelocityPlugin.getServer().getEventManager().fire(event).join();
		Method isCancelled = null;

		try {
			isCancelled = ReflectionUtil.getMethod(futureEvent.getClass(), "isCancelled");
		} catch (final ReflectionException ex) {
		}

		return isCancelled == null || !((boolean) ReflectionUtil.invoke(isCancelled, futureEvent));
	}

	@Override
	public HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack) {
		throw new UnsupportedOperationException("Not supported in Velocity");
	}

	@Override
	protected void dispatchConsoleCommand0(String command) {
		final ProxyServer server = VelocityPlugin.getServer();

		server.getCommandManager().executeAsync(server.getConsoleCommandSource(), command);
	}

	@Override
	public List<FoundationPlayer> getOnlinePlayers() {
		final List<FoundationPlayer> players = new ArrayList<>();

		for (final Player player : Remain.getOnlinePlayers(false))
			players.add(this.toPlayer(player));

		return players;
	}

	@Override
	public String getPlatformName() {
		return VelocityPlugin.getServer().getVersion().getName();
	}

	@Override
	public String getPlatformVersion() {
		return VelocityPlugin.getServer().getVersion().getVersion();
	}

	@Override
	protected FoundationPlayer getPlayer(String name) {
		final Player player = VelocityPlugin.getServer().getPlayer(name).orElse(null);

		return player != null ? this.toPlayer(player) : null;
	}

	@Override
	protected FoundationPlayer getPlayer(UUID uniqueId) {
		final Player player = VelocityPlugin.getServer().getPlayer(uniqueId).orElse(null);

		return player != null && player.isActive() ? this.toPlayer(player) : null;
	}

	@Override
	public FoundationPlugin getPlugin() {
		return VelocityPlugin.getInstance();
	}

	@Override
	public File getPluginFile(String pluginName) {
		return VelocityPlugin.getInstance().getFile();
	}

	@Override
	public List<Tuple<String, String>> getPlugins() {
		return CommonCore.convertList(VelocityPlugin.getServer().getPluginManager().getPlugins(), plugin -> new Tuple<>(plugin.getDescription().getName().orElse("Unnamed"), plugin.getDescription().getVersion().orElse("")));
	}

	@Override
	public FoundationServer getServer(String name) {
		final RegisteredServer server = VelocityPlugin.getServer().getServer(name).orElse(null);

		return server != null ? new VelocityServer(server) : null;
	}

	@Override
	public List<FoundationServer> getServers() {
		return CommonCore.convertList(VelocityPlugin.getServer().getAllServers(), VelocityServer::new);
	}

	@Override
	public boolean isAsync() {
		return true;
	}

	@Override
	public boolean isPluginInstalled(String name) {
		final PluginManager manager = VelocityPlugin.getServer().getPluginManager();
		final boolean present = manager.getPlugin(name).isPresent() || manager.getPlugin(name.toLowerCase()).isPresent();

		if (present)
			Platform.runTaskAsync(0, () -> {
				if (!manager.isLoaded(name) && !manager.isLoaded(name.toLowerCase()))
					CommonCore.warning(VelocityPlugin.getInstance().getName() + " could not hook into " + name + " as the plugin is disabled! (DO NOT REPORT THIS TO " + VelocityPlugin.getInstance().getName() + ", look for errors above and contact support of '" + name + "')");
			});

		return present;
	}

	@Override
	public void log(String message) {
		//final Logger logger = VelocityPlugin.getInstance().getLogger(); // Bugs out with duplicated prefix

		System.out.println(CompChatColor.stripColorCodes(message));
	}

	@Override
	public void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases) {
		final CommandManager manager = VelocityPlugin.getServer().getCommandManager();
		final CommandMeta oldCommand = manager.getCommandMeta(command.getLabel());

		if (oldCommand != null && unregisterOldCommand)
			manager.unregister(oldCommand);

		if (unregisterOldAliases)
			for (final String alias : command.getAliases()) {
				final CommandMeta oldAlias = manager.getCommandMeta(alias);

				if (oldAlias != null)
					manager.unregister(oldAlias);
			}

		manager.register(command.getLabel(), new VelocityCommandImpl(command), CommonCore.toArray(command.getAliases()));
	}

	@Override
	public void registerDefaultPlatformSubcommands(SimpleCommandGroup group) {
		// Not supported in Velocity
	}

	@Override
	public void registerEvents(final Object listener) {
		VelocityPlugin.getServer().getEventManager().register(VelocityPlugin.getInstance(), listener);
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
		final Player player = Remain.getPlayer(senderUid, false);
		ValidCore.checkNotNull(player, "Unable to find player by UUID: " + senderUid);

		player.sendPluginMessage(VelocityPlugin.LEGACY_BUNGEE_CHANNEL, array);
	}

	@Override
	public FoundationPlayer toPlayer(Object sender) {
		if (sender instanceof FoundationPlayer)
			return (FoundationPlayer) sender;

		if (sender == null)
			throw new FoException("Cannot convert null sender to FoundationPlayer!");

		if (!(sender instanceof CommandSource))
			throw new FoException("Can only convert CommandSender to FoundationPlayer, got " + sender.getClass().getSimpleName() + ": " + sender);

		return new VelocityPlayer((CommandSource) sender);
	}

	@Override
	public FoundationServer toServer(Object server) {
		if (server instanceof FoundationServer)
			return (FoundationServer) server;

		else if (server instanceof RegisteredServer)
			return new VelocityServer((RegisteredServer) server);

		else if (server instanceof ServerConnection)
			return new VelocityServer(((ServerConnection) server).getServer());

		else
			throw new FoException("Cannot convert " + server + " to FoundationServer! Only RegisteredServer and ServerConnection are supported.");
	}

	@Override
	public void unregisterCommand(SimpleCommandCore command) {
		VelocityPlugin.getServer().getCommandManager().unregister(command.getLabel());
	}
}