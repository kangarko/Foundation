package org.mineacademy.fo.platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.LegacyEnumNameTranslator;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.VelocityCommandImpl;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.Remain;
import org.slf4j.Logger;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

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

		// Initialize platform-specific variables
		Variables.setCollector(new VelocityVariableCollector());

		Common.addSimplifier(object -> {
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
		SimplePlugin.getServer().getEventManager().fireAndForget(event);

		return true;
	}

	@Override
	public HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack) {
		throw new UnsupportedOperationException("Not supported in Velocity");
	}

	@Override
	protected void dispatchConsoleCommand0(String command) {
		final ProxyServer server = SimplePlugin.getServer();

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
		return SimplePlugin.getServer().getVersion().getName();
	}

	@Override
	public String getPlatformVersion() {
		return SimplePlugin.getServer().getVersion().getVersion();
	}

	@Override
	public FoundationPlugin getPlugin() {
		return SimplePlugin.getInstance();
	}

	@Override
	public File getPluginFile(String pluginName) {
		return SimplePlugin.getInstance().getFile();
	}

	@Override
	public List<Tuple<String, String>> getServerPlugins() {
		return Common.convertList(SimplePlugin.getServer().getPluginManager().getPlugins(), plugin -> new Tuple<>(plugin.getDescription().getName().orElse("Unnamed"), plugin.getDescription().getVersion().orElse("")));
	}

	@Override
	public boolean hasHexColorSupport() {
		return true;
	}

	@Override
	public boolean isAsync() {
		return true;
	}

	@Override
	public boolean isPluginInstalled(String name) {
		final PluginManager manager = SimplePlugin.getServer().getPluginManager();
		final boolean present = manager.getPlugin(name).isPresent() || manager.getPlugin(name.toLowerCase()).isPresent();

		if (present)
			this.runTaskAsync(0, () -> {
				if (!manager.isLoaded(name) && !manager.isLoaded(name.toLowerCase()))
					Common.warning(SimplePlugin.getInstance().getName() + " could not hook into " + name + " as the plugin is disabled! (DO NOT REPORT THIS TO " + SimplePlugin.getInstance().getName() + ", look for errors above and contact support of '" + name + "')");
			});

		return present;

	}

	@Override
	public void log(String message) {
		final String plain = CompChatColor.stripColorCodes(message);
		final Logger logger = SimplePlugin.getInstance().getLogger();

		if (plain.startsWith("Warning:") || plain.startsWith("[Warning]") || plain.startsWith("Warn:") || plain.startsWith("[Warn]"))
			logger.warn(plain);

		else
			logger.info(plain);
	}

	@Override
	public void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases) {
		final CommandManager manager = SimplePlugin.getServer().getCommandManager();
		final CommandMeta oldCommand = manager.getCommandMeta(command.getLabel());

		if (oldCommand != null && unregisterOldCommand)
			manager.unregister(oldCommand);

		if (unregisterOldAliases)
			for (final String alias : command.getAliases()) {
				final CommandMeta oldAlias = manager.getCommandMeta(alias);

				if (oldAlias != null)
					manager.unregister(oldAlias);
			}

		manager.register(command.getLabel(), new VelocityCommandImpl(command), Common.toArray(command.getAliases()));
	}

	@Override
	public void registerDefaultPlatformSubcommands(SimpleCommandGroup group) {
		// Not supported in Velocity
	}

	@Override
	public void registerEvents(final Object listener) {
		SimplePlugin.getInstance().registerEvents(listener);
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
		Valid.checkNotNull(player, "Unable to find player by UUID: " + senderUid);

		player.sendPluginMessage(SimplePlugin.LEGACY_BUNGEE_CHANNEL, array);
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
	public void unregisterCommand(SimpleCommandCore command) {
		SimplePlugin.getServer().getCommandManager().unregister(command.getLabel());
	}
}