package org.mineacademy.fo.platform;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.filter.Filter;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.Platform.Type;

import lombok.NonNull;
import net.kyori.adventure.text.event.HoverEventSource;

/**
 * An implementation of a {@link Platform}
 */
public abstract class FoundationPlatform {

	private String customServerName;

	final Map<UUID, FoundationPlayer> players = new HashMap<>();

	public final void unregisterPlayer(UUID uniqueId) {
		this.players.remove(uniqueId);
	}

	protected FoundationPlatform() {

		// Dynamically load filters
		for (final Class<? extends Filter> filterClass : ReflectionUtil.getClasses(this.getPlugin().getFile(), Filter.class, this.getPlugin().getPluginClassLoader()))
			try {
				final Constructor<? extends Filter> constructor = ReflectionUtil.getConstructor(filterClass);
				ValidCore.checkBoolean(constructor.getParameterCount() == 0, "Filter class " + filterClass + " must have a public no args constructor!");
				ValidCore.checkBoolean(Modifier.isPublic(constructor.getModifiers()), "Filter class " + filterClass + " must have a public constructor!");

				final Filter filter = ReflectionUtil.instantiate(constructor);

				Filter.register(filter.getIdentifier(), filter);

			} catch (final Exception ex) {
				CommonCore.error(ex,
						"Failed to load filter: " + filterClass,
						"Check that it has a public no args constructor!");

				continue;
			}
	}

	public abstract boolean callEvent(Object event);

	public abstract HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack);

	public final void dispatchConsoleCommand(final FoundationPlayer playerReplacement, String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		if (command.startsWith("@announce ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @announce without a player in: " + command);

			Messenger.announce(playerReplacement, command.replace("@announce ", ""));
		}

		else if (command.startsWith("@warn ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @warn without a player in: " + command);

			Messenger.warn(playerReplacement, command.replace("@warn ", ""));
		}

		else if (command.startsWith("@error ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @error without a player in: " + command);

			Messenger.error(playerReplacement, command.replace("@error ", ""));
		}

		else if (command.startsWith("@info ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @info without a player in: " + command);

			Messenger.info(playerReplacement, command.replace("@info ", ""));
		}

		else if (command.startsWith("@question ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @question without a player in: " + command);

			Messenger.question(playerReplacement, command.replace("@question ", ""));
		}

		else if (command.startsWith("@success ")) {
			ValidCore.checkNotNull(playerReplacement, "Cannot use @success without a player in: " + command);

			Messenger.success(playerReplacement, command.replace("@success ", ""));
		}

		else {
			command = command.charAt(0) == '/' && command.charAt(1) != '/' ? command.substring(1) : command;

			if (playerReplacement != null)
				command = Variables.builder(playerReplacement).replaceLegacy(command);
			else
				command = command.replace("{player}", "");

			this.dispatchConsoleCommand0(command);
		}
	}

	protected abstract void dispatchConsoleCommand0(String command);

	public final String getCustomServerName() {
		if (Platform.getType() != Type.BUKKIT)
			throw new IllegalArgumentException("Custom server name is only supported in Bukkit, for other platforms use Platform#toPlayer() and get the server per player");

		if (!this.hasCustomServerName())
			throw new IllegalArgumentException("Please instruct developer of " + Platform.getPlugin().getName() + " to call Platform#setCustomServerName");

		return this.customServerName;
	}

	public final Collection<FoundationPlayer> getPlayers() {
		return players.values();
	}

	public abstract String getPlatformName();

	public abstract String getPlatformVersion();

	protected final FoundationPlayer getPlayer(final String name) {
		for (final FoundationPlayer player : this.players.values())
			if (player.getName().equals(name))
				return player;

		return null;
	}

	protected final FoundationPlayer getPlayer(final UUID uniqueId) {
		return this.players.get(uniqueId);
	}

	public abstract FoundationPlugin getPlugin();

	public abstract File getPluginFile(String pluginName);

	public abstract List<Tuple<String, String>> getPlugins();

	public abstract FoundationServer getServer(String name);

	public abstract List<FoundationServer> getServers();

	public final boolean hasCustomServerName() {
		return this.customServerName != null && !this.customServerName.isEmpty() && !this.customServerName.contains("mineacademy.org/server-properties") && !"undefined".equals(this.customServerName) && !"Unknown Server".equals(this.customServerName);
	}

	public abstract boolean isAsync();

	public abstract boolean isPluginInstalled(String name);

	public abstract void log(String message);

	public abstract void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases);

	@Deprecated
	public abstract void registerDefaultPlatformSubcommands(SimpleCommandGroup group);

	public abstract void registerEvents(Object listener);

	public abstract Task runTask(int delayTicks, Runnable runnable);

	public final Task runTask(final Runnable runnable) {
		return this.runTask(0, runnable);
	}

	public abstract Task runTaskAsync(int delayTicks, Runnable runnable);

	public final Task runTaskAsync(final Runnable runnable) {
		return this.runTaskAsync(0, runnable);
	}

	public abstract Task runTaskTimer(int delayTicks, int repeatTicks, Runnable runnable);

	public final Task runTaskTimer(final int repeatTicks, final Runnable runnable) {
		return this.runTaskTimer(0, repeatTicks, runnable);
	}

	public abstract Task runTaskTimerAsync(int delayTicks, int repeatTicks, Runnable runnable);

	public final Task runTaskTimerAsync(final int repeatTicks, final Runnable runnable) {
		return this.runTaskTimerAsync(0, repeatTicks, runnable);
	}

	public abstract void sendPluginMessage(UUID senderUid, String channel, byte[] array);

	public final void setCustomServerName(@NonNull final String serverName) {
		if (!serverName.isEmpty()) {
			if (serverName.length() > 64)
				throw new IllegalArgumentException("Server name cannot be longer than 64 characters! Got: " + serverName);

			if (!serverName.matches("[a-zA-Z0-9_-]+"))
				throw new IllegalArgumentException("Server name can only contains alphanumeric characters, '_' and '-'. Got: " + serverName);

			this.customServerName = serverName;

		} else
			this.customServerName = "server";
	}

	public abstract FoundationPlayer toPlayer(Object sender);

	public abstract FoundationServer toServer(Object server);

	public abstract void unregisterCommand(SimpleCommandCore command);
}
