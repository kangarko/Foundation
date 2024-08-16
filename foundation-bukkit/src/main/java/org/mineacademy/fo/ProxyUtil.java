package org.mineacademy.fo;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.RemainCore;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for sending messages to the proxy.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProxyUtil {

	/**
	 * See {@link #sendPluginMessage(String, ProxyMessage, Object...)}
	 * <p>
	 * NB: This one uses the default channel name specified in {@link SimplePlugin}. By
	 * default, nothing is specified there and so an exception will be thrown.
	 *
	 * We find a random player through which we will send the message. If the server is
	 * empty, nothing will happen.
	 *
	 * @param <T>
	 * @param message
	 * @param datas
	 */
	@SafeVarargs
	public static <T> void sendPluginMessage(ProxyMessage message, T... datas) {
		final ProxyListener proxy = SimplePlugin.getInstance().getDefaultProxyListener();
		ValidCore.checkNotNull(proxy, "Cannot call sendPluginMessage() without channel name because " + SimplePlugin.getInstance().getName() + " does not have any class extending ProxyListener with @AutoMessage!");

		sendPluginMessage(proxy.getChannel(), message, datas);
	}

	/**
	 * See {@link #sendPluginMessage(String, ProxyMessage, Object...)}
	 * <p>
	 * NB: This one uses the default channel name specified in {@link SimplePlugin}. By
	 * default, nothing is specified there and so an exception will be thrown.
	 *
	 * @param <T>
	 * @param player
	 * @param message
	 * @param datas
	 */
	@SafeVarargs
	public static <T> void sendPluginMessageAs(@Nullable Player player, ProxyMessage message, T... datas) {
		final ProxyListener proxy = SimplePlugin.getInstance().getDefaultProxyListener();
		ValidCore.checkNotNull(proxy, "Cannot call sendPluginMessageAs() without channel name because " + SimplePlugin.getInstance().getName() + " does not have any class extending ProxyListener with @AutoMessage");

		sendPluginMessage(player, proxy.getChannel(), message, datas);
	}

	/**
	 * Sends message via a channel to proxy. You need an
	 * implementation in proxy to handle it, otherwise nothing will happen.
	 *
	 * OBS! The data written always start with:
	 *
	 * 1. The recipient UUID
	 * 2. {@link RemainCore#getServerName()}
	 * 3. The action parameter
	 *
	 * We find a random player through which we will send the message. If the server is
	 * empty, nothing will happen.
	 *
	 * @param <T>
	 * @param channel
	 * @param message
	 * @param data
	 */
	@SafeVarargs
	public static <T> void sendPluginMessage(String channel, ProxyMessage message, T... data) {
		sendPluginMessage(null, channel, message, data);
	}

	/**
	 * Sends message via a channel to proxy. You need an
	 * implementation in proxy to handle it, otherwise nothing will happen.
	 *
	 * OBS! The data written always start with the following header data:
	 *
	 * 1. The channel name (String)
	 * 2. The recipient UUID (String)
	 * 3. {@link RemainCore#getServerName()} (String)
	 * 4. The action parameter (enum to String)
	 *
	 * @param <T>
	 * @param sender through which sender to send, if empty, we find a random player, or if server is empty, no message is sent
	 * @param channel
	 * @param message
	 * @param dataArray
	 */
	@SafeVarargs
	public static <T> void sendPluginMessage(@Nullable Player sender, String channel, ProxyMessage message, T... dataArray) {
		synchronized (SimplePlugin.getInstance()) {
			if (sender == null)
				sender = findFirstPlayer();

			if (sender == null) {
				Debugger.put("proxy", "Cannot send message " + message + " on channel '" + channel + "' to proxy because this server has no players.");

				return;
			}

			final OutgoingMessage out = new OutgoingMessage(message);

			for (final T data : dataArray)
				out.write(data, data.getClass());

			out.send(sender.getUniqueId());
		}
	}

	/**
	 * Sends a plugin message that will re-connect the player to another server on proxy
	 *
	 * @param player     the living non-dead player
	 * @param serverName the server name as you have in your proxy
	 */
	public static void connect(@NonNull Player player, @NonNull String serverName) {
		sendBungeeMessage(player, "Connect", serverName);
	}

	/**
	 * Sends message via a channel to proxy on the "BungeeCord" channel. You need an
	 * implementation at proxy to handle it, otherwise nothing will happen.
	 *
	 * Please see the link below for what data to write:
	 * https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/
	 *
	 * @param sender the player to send the message as
	 * @param data  the data
	 */
	public static void sendBungeeMessage(@NonNull Player sender, Object... data) {
		synchronized (SimplePlugin.getInstance()) {
			ValidCore.checkBoolean(data != null && data.length >= 1, "");

			final ByteArrayDataOutput out = ByteStreams.newDataOutput();

			for (final Object datum : data) {

				if (data == null)
					throw new FoException("Found null object when sending proxy plugin message! Data: " + CommonCore.join(data, t -> CommonCore.getOrDefault(SerializeUtil.serialize(SerializeUtil.Mode.YAML, t), "").toString()));

				if (datum instanceof Integer)
					out.writeInt((Integer) datum);

				else if (datum instanceof Double)
					out.writeDouble((Double) datum);

				else if (datum instanceof Boolean)
					out.writeBoolean((Boolean) datum);

				else if (datum instanceof String)
					out.writeUTF((String) datum);

				else
					throw new FoException("Unknown type of data in proxy plugin message: " + datum + " (" + datum.getClass().getSimpleName() + ")");
			}

			// Can't use "Bukkit.getServer()" since it will send one message for each player, creating duplicates (i.e. 4X join message bug)
			sender.sendPluginMessage(SimplePlugin.getInstance(), "BungeeCord", out.toByteArray());
		}
	}

	/*
	 * Return either the first online player or the server itself
	 * through which we send the proxy message as
	 */
	private static Player findFirstPlayer() {
		return Remain.getOnlinePlayers().isEmpty() ? null : Remain.getOnlinePlayers().iterator().next();
	}
}
