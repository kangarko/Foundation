package org.mineacademy.fo;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.mineacademy.fo.Common.Stringer;
import org.mineacademy.fo.bungee.BungeeAction;
import org.mineacademy.fo.bungee.BungeeChannel;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for sending messages to BungeeCord.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BungeeUtil {

	/**
	 * See {@link #tellBungee(BungeeChannel, Player, Object...)}
	 *
	 * NB: This one uses the default channel name specified in {@link SimplePlugin}. By
	 * default, nothing is specified there and so an exception will be thrown.
	 */
	@SafeVarargs
	public static <T> void tellBungee(BungeeAction action, T... datas) {
		tellBungee(SimplePlugin.getInstance().getDefaultBungeeChannel(), action, datas);
	}

	/**
	 *
	 * Sends message via a channel to the bungee network (upstreams). You need an
	 * implementation in bungee to handle it, otherwise nothing will happens.
	 *
	 * OBS! The data written:
	 *
	 * 1. This server name specified in {@link SimplePlugin#getServerName()} 2. The
	 * datas in the data parameter.
	 *
	 * @param channel the name of channel in an enum object
	 * @param datas   the data
	 */
	@SafeVarargs
	public static <T> void tellBungee(BungeeChannel channel, BungeeAction action, T... datas) {
		Valid.checkBoolean(datas.length == action.getValidValues().length, "Data count != valid values count in " + action + "! Data: " + datas.length + " vs " + action.getValidValues().length);

		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		Debugger.put("bungee", "Sending bungee message [" + channel + ", " + action + "]: ");

		out.writeUTF(SimpleSettings.SERVER_NAME);
		out.writeUTF(action.toString());

		for (final Object data : datas) {
			Valid.checkNotNull(data, "Bungee object in array is null! Array: " + Common.join(datas, ", ", (Stringer<T>) t -> t == null ? "null" : t.toString() + "(" + t.getClass().getSimpleName() + ")"));

			if (data instanceof Integer) {
				Debugger.put("bungee", data.toString() + ", ");

				out.writeInt((Integer) data);

			} else if (data instanceof Double) {
				Debugger.put("bungee", data.toString() + ", ");

				out.writeDouble((Double) data);
			}

			else if (data instanceof Boolean) {
				Debugger.put("bungee", data.toString() + ", ");

				out.writeBoolean((Boolean) data);
			}

			else if (data instanceof String) {
				Debugger.put("bungee", data.toString() + ", ");

				out.writeUTF((String) data);
			}

			else
				throw new FoException("Unknown type of data: " + data + " (" + data.getClass().getSimpleName() + ")");
		}

		Debugger.push("bungee");
		getThroughWhomSendMessage().sendPluginMessage(SimplePlugin.getInstance(), channel.getName(), out.toByteArray());
	}

	/**
	 *
	 * Sends message via a channel to the bungee network (upstreams). You need an
	 * implementation in bungee to handle it, otherwise nothing will happens.
	 *
	 * OBS! The data written:
	 *
	 * 1. This server name specified in {@link SimplePlugin#getServerName()} 2. The
	 * datas in the data parameter.
	 *
	 * @param sender the player to send the message as
	 * @param datas  the data
	 */
	public static void tellNative(Player sender, Object... datas) {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		for (final Object data : datas) {
			Valid.checkNotNull(data, "Bungee object in array is null! Array: " + Common.join(datas, ", ", (Stringer<Object>) t -> t == null ? "null" : t.toString() + "(" + t.getClass().getSimpleName() + ")"));

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
	 * Return either the first online player or the server itself
	 * through which we send the bungee message as
	 *
	 * @return
	 */
	private static PluginMessageRecipient getThroughWhomSendMessage() {
		return Remain.getOnlinePlayers().isEmpty() ? Bukkit.getServer() : Remain.getOnlinePlayers().iterator().next();
	}
}
