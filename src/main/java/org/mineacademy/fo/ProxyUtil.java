package org.mineacademy.fo;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.bukkit.plugin.messaging.MessageTooLargeException;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.mineacademy.fo.remain.Remain;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

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
		final ProxyListener proxy = SimplePlugin.getInstance().getProxyListener();
		Valid.checkNotNull(proxy, "Cannot call sendPluginMessage() without channel name because " + SimplePlugin.getInstance().getClass() + " does not have any class extending ProxyListener with @AutoMessage!");

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
		final ProxyListener proxy = SimplePlugin.getInstance().getProxyListener();
		Valid.checkNotNull(proxy, "Cannot call sendPluginMessageAs() without channel name because " + SimplePlugin.getInstance().getClass() + " does not have any class extending ProxyListener with @AutoMessage");

		sendPluginMessage(player, proxy.getChannel(), message, datas);
	}

	/**
	 * Sends message via a channel to proxy. You need an
	 * implementation in proxy to handle it, otherwise nothing will happen.
	 *
	 * OBS! The data written always start with:
	 *
	 * 1. The recipient UUID
	 * 2. {@link Remain#getServerName()}
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
	 * 3. {@link Remain#getServerName()} (String)
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
			Valid.checkBoolean(dataArray.length == message.getContent().length,
					"Proxy message " + message + " on channel " + channel + " has invalid data lenght! Expected: " + message.getContent().length + ". Got: " + dataArray.length);

			if (!message.name().equals("PLAYERS_CLUSTER_DATA") && Debugger.isDebugged("proxy"))
				Debugger.put("proxy", "Sending proxy message " + message + " on channel " + channel + " with data: " + Common.join(dataArray, t -> Common.getOrDefault(SerializeUtil.serialize(t), "").toString()));

			if (sender == null)
				sender = findFirstPlayer();

			if (sender == null) {
				Common.warning("Cannot send message " + message + " on channel '" + channel + "' to proxy because this server has no players.");

				return;
			}

			final ByteArrayDataOutput out = ByteStreams.newDataOutput();

			out.writeUTF(channel);
			out.writeUTF(sender.getUniqueId().toString());
			out.writeUTF(Remain.getServerName());
			out.writeUTF(message.toString());

			int head = 0;

			for (Object data : dataArray)
				try {
					if (data == null)
						throw new NullPointerException("Null data");

					if (data instanceof CommandSender)
						data = ((CommandSender) data).getName();

					if (data instanceof Integer) {
						checkData(head, message, Integer.class);

						out.writeInt((Integer) data);

					} else if (data instanceof Double) {
						checkData(head, message, Double.class);

						out.writeDouble((Double) data);

					} else if (data instanceof Long) {
						checkData(head, message, Long.class);

						out.writeLong((Long) data);

					} else if (data instanceof Boolean) {
						checkData(head, message, Boolean.class);

						out.writeBoolean((Boolean) data);

					} else if (data instanceof String) {
						checkData(head, message, String.class);

						OutgoingMessage.writeCompressedString(out, (String) data);

					} else if (data instanceof Component) {
						checkData(head, message, Component.class);

						OutgoingMessage.writeCompressedString(out, Remain.convertAdventureToJson((Component) data));

					} else if (data instanceof SimpleComponent) {
						checkData(head, message, SimpleComponent.class);

						OutgoingMessage.writeCompressedString(out, ((SimpleComponent) data).serialize().toJson());

					} else if (data instanceof SerializedMap) {
						checkData(head, message, SerializedMap.class);

						OutgoingMessage.writeCompressedString(out, ((SerializedMap) data).toJson());

					} else if (data instanceof UUID) {
						checkData(head, message, UUID.class);

						out.writeUTF(((UUID) data).toString());

					} else if (data instanceof Enum) {
						checkData(head, message, Enum.class);

						out.writeUTF(((Enum<?>) data).toString());

					} else if (data instanceof byte[]) {
						checkData(head, message, String.class);

						out.write((byte[]) data);

					} else
						throw new IllegalArgumentException("Unknown type of data");

					head++;

				} catch (final Throwable t) {
					Common.error(t,
							"Error writing data in proxy plugin message!",
							"Message: " + message,
							"Channel: " + channel,
							"Wrong data: " + data,
							"Error: %error%",
							"All data: " + Common.join(dataArray, data2 -> Common.getOrDefault(SerializeUtil.serialize(data2), "").toString()));

					return;
				}

			final byte[] byteArray = out.toByteArray();

			try {
				sender.sendPluginMessage(SimplePlugin.getInstance(), ProxyListener.DEFAULT_CHANNEL, byteArray);

			} catch (final ChannelNotRegisteredException ex) {
				Common.log("Cannot send proxy message " + message + " because channel '" + ProxyListener.DEFAULT_CHANNEL + "/" + channel + "' is not registered. "
						+ "Use @AutoRegister above your class extending ProxyListener and return its instance in getProxy Cord in your main plugin class.");

			} catch (final MessageTooLargeException ex) {
				Common.log("Outgoing proxy message '" + message + "' was oversized, not sending. Max length: 32,766 bytes, got " + byteArray.length + " bytes.");
			}
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
			Valid.checkBoolean(data != null && data.length >= 1, "");

			final ByteArrayDataOutput out = ByteStreams.newDataOutput();

			for (final Object datum : data) {

				if (data == null)
					throw new FoException("Found null object when sending proxy plugin message! Data: " + Common.join(data, t -> Common.getOrDefault(SerializeUtil.serialize(t), "").toString()));

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

	/*
	 * Ensures we are reading in the correct order and the correct data type.
	 */
	private static void checkData(int head, ProxyMessage message, Class<?> requiredType) throws Throwable {
		final Class<?>[] content = message.getContent();
		final Class<?> clazz = content[head];

		Valid.checkBoolean(requiredType.isAssignableFrom(clazz), "Expected " + requiredType.getSimpleName() + " at position " + head + " but got " + clazz.getSimpleName() + " for " + message.name());
		Valid.checkBoolean(head < content.length, "Head out of bounds! Max data size for " + message.name() + " is " + content.length);
	}
}
