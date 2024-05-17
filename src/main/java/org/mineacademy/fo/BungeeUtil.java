package org.mineacademy.fo;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.bukkit.plugin.messaging.MessageTooLargeException;
import org.mineacademy.fo.Common.Stringer;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.bungee.BungeeMessageType;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for sending messages to BungeeCord.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BungeeUtil {

	/**
	 * See {@link #sendPluginMessage(String, BungeeMessageType, Object...)}
	 * <p>
	 * NB: This one uses the default channel name specified in {@link SimplePlugin}. By
	 * default, nothing is specified there and so an exception will be thrown.
	 *
	 * We find a random player through which we will send the message. If the server is
	 * empty, nothing will happen.
	 *
	 * @param <T>
	 * @param action
	 * @param datas
	 */
	@SafeVarargs
	public static <T> void sendPluginMessage(BungeeMessageType action, T... datas) {
		final BungeeListener bungee = SimplePlugin.getInstance().getBungeeCord();
		Valid.checkNotNull(bungee, "Cannot call tellBungee() without channel name because " + SimplePlugin.getInstance().getClass() + " does not implement getBungeeCord()!");

		sendPluginMessage(bungee.getChannel(), action, datas);
	}

	/**
	 * See {@link #sendPluginMessage(String, BungeeMessageType, Object...)}
	 * <p>
	 * NB: This one uses the default channel name specified in {@link SimplePlugin}. By
	 * default, nothing is specified there and so an exception will be thrown.
	 *
	 * @param <T>
	 * @param player
	 * @param action
	 * @param datas
	 */
	@SafeVarargs
	public static <T> void sendPluginMessageAs(@Nullable Player player, BungeeMessageType action, T... datas) {
		final BungeeListener bungee = SimplePlugin.getInstance().getBungeeCord();
		Valid.checkNotNull(bungee, "Cannot call tellBungee() without channel name because " + SimplePlugin.getInstance().getClass() + " does not implement getBungeeCord()!");

		sendPluginMessage(player, bungee.getChannel(), action, datas);
	}

	/**
	 * Sends message via a channel to the bungee network (upstreams). You need an
	 * implementation in bungee to handle it, otherwise nothing will happen.
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
	 * @param action
	 * @param data
	 */
	@SafeVarargs
	public static <T> void sendPluginMessage(String channel, BungeeMessageType action, T... data) {
		sendPluginMessage(null, channel, action, data);
	}

	/**
	 * Sends message via a channel to the bungee network (upstreams). You need an
	 * implementation in bungee to handle it, otherwise nothing will happen.
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
	 * @param action
	 * @param data
	 */
	@SafeVarargs
	public static <T> void sendPluginMessage(@Nullable Player sender, String channel, BungeeMessageType action, T... data) {
		synchronized (SimplePlugin.getInstance()) {
			Valid.checkBoolean(data.length == action.getContent().length, "Data count != valid values count in " + action + "! Given data: " + data.length + " vs needed: " + action.getContent().length);
			Remain.getServerName(); // check

			if (!action.name().equals("PLAYERS_CLUSTER_DATA"))
				Debugger.put("bungee", "Server '" + Remain.getServerName() + "' sent bungee message [" + channel + ", " + action + "]: ");

			if (sender == null)
				sender = findFirstPlayer();

			// This server is empty, do not send
			if (sender == null) {
				Debugger.debug("bungee", "&eWarning: Cannot send " + action + " bungee message to channel '" + channel + "' because this server has no players");

				return;
			}

			final ByteArrayDataOutput out = ByteStreams.newDataOutput();

			// Write Foundation header
			out.writeUTF(channel);
			out.writeUTF(sender.getUniqueId().toString());
			out.writeUTF(Remain.getServerName());
			out.writeUTF(action.toString());

			int actionHead = 0;

			for (Object datum : data)
				try {
					Valid.checkNotNull(datum, "Bungee object in array is null! Array: " + Common.join(data, ", ", (Stringer<T>) t -> t == null ? "null" : t.toString() + " (" + t.getClass().getSimpleName() + ")"));

					if (datum instanceof CommandSender)
						datum = ((CommandSender) datum).getName();

					if (datum instanceof Integer) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, Integer.class, data);
						out.writeInt((Integer) datum);

					} else if (datum instanceof Double) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, Double.class, data);
						out.writeDouble((Double) datum);

					} else if (datum instanceof Long) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, Long.class, data);
						out.writeLong((Long) datum);

					} else if (datum instanceof Boolean) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, Boolean.class, data);
						out.writeBoolean((Boolean) datum);

					} else if (datum instanceof String) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, String.class, data);

						try {
							out.writeUTF((String) datum);

						} catch (final Throwable t) {
							if (t.getMessage().contains("too long"))
								Common.throwError(t, "Too long BungeeCord message to send (" + ((String) datum).length() + ")! Message: ", (String) datum);

							else
								throw t;
						}

					} else if (datum instanceof SimpleComponent) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, String.class, data);
						out.writeUTF(((SimpleComponent) datum).serialize().toJson());

					} else if (datum instanceof SerializedMap) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, String.class, data);
						out.writeUTF(((SerializedMap) datum).toJson());

					} else if (datum instanceof UUID) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, UUID.class, data);
						out.writeUTF(((UUID) datum).toString());

					} else if (datum instanceof Enum) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, Enum.class, data);
						out.writeUTF(((Enum<?>) datum).toString());

					} else if (datum instanceof byte[]) {
						Debugger.put("bungee", datum.toString() + ", ");

						moveHead(actionHead, action, String.class, data);
						out.write((byte[]) datum);

					} else
						throw new FoException("Unknown type of data: " + datum + " (" + datum.getClass().getSimpleName() + ")");

					actionHead++;

				} catch (final Throwable t) {
					t.printStackTrace();

					return;
				}

			Debugger.push("bungee");

			final byte[] byteArray = out.toByteArray();

			if (byteArray.length > 30_000) { // Safety margin
				Common.log("Outgoing bungee message '" + action + "' was oversized, not sending. Max length: 32766 bytes, got " + byteArray.length + " bytes.");

				actionHead = 0;
				return;
			}

			try {
				sender.sendPluginMessage(SimplePlugin.getInstance(), BungeeListener.DEFAULT_CHANNEL, byteArray);

			} catch (final ChannelNotRegisteredException ex) {
				Common.log("Cannot send Bungee '" + action + "' message because channel '" + BungeeListener.DEFAULT_CHANNEL + "/" + channel + "' is not registered. "
						+ "Use @AutoRegister above your class extending BungeeListener and return its instance in getBungeeCord in your main plugin class.");

			} catch (final MessageTooLargeException ex) {
				Common.log("Outgoing bungee message '" + action + "' was oversized, not sending. Max length: 32,766 bytes, got " + byteArray.length + " bytes.");
			}

			actionHead = 0;
		}
	}

	/**
	 * Sends a plugin message that will re-connect the player to another server on Bungee
	 *
	 * @param player     the living non-dead player
	 * @param serverName the server name as you have in config.yml of your BungeeCord
	 */
	public static void connect(@NonNull Player player, @NonNull String serverName) {
		sendBungeeMessage(player, "Connect", serverName);
	}

	/**
	 * Sends message via a channel to the bungee network (upstreams). You need an
	 * implementation in bungee to handle it, otherwise nothing will happen.
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
				Valid.checkNotNull(datum, "Bungee object in array is null! Array: " + Common.join(data, ", ", t -> t == null ? "null" : t.toString() + "(" + t.getClass().getSimpleName() + ")"));

				if (datum instanceof Integer)
					out.writeInt((Integer) datum);

				else if (datum instanceof Double)
					out.writeDouble((Double) datum);

				else if (datum instanceof Boolean)
					out.writeBoolean((Boolean) datum);

				else if (datum instanceof String)
					out.writeUTF((String) datum);

				else
					throw new FoException("Unknown type of data: " + datum + " (" + datum.getClass().getSimpleName() + ")");
			}

			// Can't use "Bukkit.getServer()" since it will send one message for each player, creating duplicates (i.e. 4X join message bug)
			sender.sendPluginMessage(SimplePlugin.getInstance(), "BungeeCord", out.toByteArray());
		}
	}

	/*
	 * Return either the first online player or the server itself
	 * through which we send the bungee message as
	 */
	private static Player findFirstPlayer() {
		return Remain.getOnlinePlayers().isEmpty() ? null : Remain.getOnlinePlayers().iterator().next();
	}

	/*
	 * Ensures we are reading in the correct order as the given {@link BungeeMessageType}
	 * specifies in its {@link BungeeMessageType#getContent()} getter.
	 * <p>
	 * This also ensures we are reading the correct data type (both primitives and wrappers
	 * are supported).
	 */
	private static void moveHead(int actionHead, BungeeMessageType action, Class<?> typeOf, Object[] data) throws Throwable {
		Valid.checkNotNull(action, "Action not set!");

		final Class<?>[] content = action.getContent();
		Valid.checkBoolean(actionHead < content.length, "Head out of bounds! Max data size for " + action.name() + " is " + content.length + "! Set Debug to [bungee] in settings.yml and report. Data length: " + data.length + " data: " + Common.join(data));
	}
}
