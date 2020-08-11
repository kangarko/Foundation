package org.mineacademy.fo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.mineacademy.fo.Common.Stringer;
import org.mineacademy.fo.bungee.BungeeAction;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Primitives;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for sending messages to BungeeCord.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BungeeUtil {

	/**
	 * The current position of writing the data based on the
	 * {@link BungeeAction#getContent()}
	 * <p>
	 * Reset in tell methods
	 */
	private static int actionHead = 0;

	/**
	 * See {@link #tellBungee(BungeeChannel, Player, Object...)}
	 * <p>
	 * NB: This one uses the default channel name specified in {@link SimplePlugin}. By
	 * default, nothing is specified there and so an exception will be thrown.
	 */
	@SafeVarargs
	public static <T> void tellBungee(BungeeAction action, T... datas) {
		tellBungee(SimplePlugin.getInstance().getBungeeCord().getChannel(), action, datas);
	}

	/**
	 * Sends message via a channel to the bungee network (upstreams). You need an
	 * implementation in bungee to handle it, otherwise nothing will happens.
	 * <p>
	 * OBS! The data written:
	 * <p>
	 * 1. This server name specified in {@link SimplePlugin#getServerName()} 2. The
	 * datas in the data parameter.
	 *
	 * @param channel the name of channel in an enum object
	 * @param datas   the data
	 */
	@SafeVarargs
	public static <T> void tellBungee(String channel, BungeeAction action, T... datas) {
		Valid.checkBoolean(datas.length == action.getContent().length, "Data count != valid values count in " + action + "! Given data: " + datas.length + " vs needed: " + action.getContent().length);

		Debugger.put("bungee", "Server '" + SimpleSettings.BUNGEE_SERVER_NAME + "' sent bungee message [" + channel + ", " + action + "]: ");
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF(SimpleSettings.BUNGEE_SERVER_NAME);
		out.writeUTF(action.toString());

		actionHead = 0;

		for (final Object data : datas) {
			Valid.checkNotNull(data, "Bungee object in array is null! Array: " + Common.join(datas, ", ", (Stringer<T>) t -> t == null ? "null" : t.toString() + "(" + t.getClass().getSimpleName() + ")"));

			if (data instanceof Integer) {
				Debugger.put("bungee", data.toString() + ", ");

				moveHead(action, Integer.class);
				out.writeInt((Integer) data);

			} else if (data instanceof Double) {
				Debugger.put("bungee", data.toString() + ", ");

				moveHead(action, Double.class);
				out.writeDouble((Double) data);
			} else if (data instanceof Long) {
				Debugger.put("bungee", data.toString() + ", ");

				moveHead(action, Long.class);
				out.writeLong((Long) data);
			} else if (data instanceof Boolean) {
				Debugger.put("bungee", data.toString() + ", ");

				moveHead(action, Boolean.class);
				out.writeBoolean((Boolean) data);
			} else if (data instanceof String) {
				Debugger.put("bungee", data.toString() + ", ");

				moveHead(action, String.class);
				out.writeUTF((String) data);
			} else
				throw new FoException("Unknown type of data: " + data + " (" + data.getClass().getSimpleName() + ")");
		}

		Debugger.push("bungee");
		getThroughWhomSendMessage().sendPluginMessage(SimplePlugin.getInstance(), channel, out.toByteArray());

		actionHead = 0;
	}

	/**
	 * Sends message via a channel to the bungee network (upstreams). You need an
	 * implementation in bungee to handle it, otherwise nothing will happens.
	 * <p>
	 * OBS! The data written:
	 * <p>
	 * 1. This server name specified in {@link SimplePlugin#getServerName()} 2. The
	 * datas in the data parameter.
	 *
	 * @param sender the player to send the message as
	 * @param datas  the data
	 */
	public static void tellNative(Player sender, Object... datas) {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		for (final Object data : datas) {
			Valid.checkNotNull(data, "Bungee object in array is null! Array: " + Common.join(datas, ", ", t -> t == null ? "null" : t.toString() + "(" + t.getClass().getSimpleName() + ")"));

			if (data instanceof Integer)
				out.writeInt((Integer) data);

			else if (data instanceof Double)
				out.writeDouble((Double) data);

			else if (data instanceof Boolean)
				out.writeBoolean((Boolean) data);

			else if (data instanceof String)
				out.writeUTF((String) data);

			else
				throw new FoException("Unknown type of data: " + data + " (" + data.getClass().getSimpleName() + ")");
		}

		sender.sendPluginMessage(SimplePlugin.getInstance(), "BungeeCord", out.toByteArray());
	}

	/**
	 * Sends a plugin message that will re-connect the player to another server on Bungee
	 *
	 * @param player     the living non-dead player
	 * @param serverName the server name as you have in config.yml of your BungeeCord
	 */
	public static void connect(@NonNull Player player, @NonNull String serverName) {
		final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(byteArray);

		try {
			out.writeUTF("Connect");
			out.writeUTF(serverName);

		} catch (final Throwable t) {
			Common.error(t,
					"Unable to connect " + player.getName() + " to server " + serverName,
					"Error: %error");
		}

		player.sendPluginMessage(SimplePlugin.getInstance(), "BungeeCord", byteArray.toByteArray());
	}

	/**
	 * Return either the first online player or the server itself
	 * through which we send the bungee message as
	 *
	 * @return
	 */
	private static PluginMessageRecipient getThroughWhomSendMessage() {
		return Remain.getOnlinePlayers().isEmpty() ? Bukkit.getServer() : Remain.getOnlinePlayers().iterator().next();
	}

	/**
	 * Ensures we are reading in the correct order as the given {@link BungeeAction}
	 * specifies in its {@link BungeeAction#getContent()} getter.
	 * <p>
	 * This also ensures we are reading the correct data type (both primitives and wrappers
	 * are supported).
	 *
	 * @param typeOf
	 */
	private static void moveHead(BungeeAction action, Class<?> typeOf) {
		Valid.checkNotNull(action, "Action not set!");

		final Class<?>[] content = action.getContent();
		Valid.checkBoolean(actionHead < content.length, "Head out of bounds! Max data size for " + action.name() + " is " + content.length + "! (Set Debug to [bungee] in settings.yml and report this issue along with the new console messages");
		Valid.checkBoolean(Primitives.wrap(content[actionHead]) == typeOf, "Unexpected data type " + typeOf + ", expected " + content[actionHead] + " for " + action.name());

		actionHead++;
	}
}
