package org.mineacademy.fo.proxy.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.SimpleComponentCore;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.remain.RemainCore;

import net.kyori.adventure.text.Component;

/**
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link ProxyMessage} by its name *written automatically*.
 */
public final class OutgoingMessage extends Message {

	/**
	 * The pending queue to write the message
	 */
	private final List<Object> queue = new ArrayList<>();

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param action
	 */
	public OutgoingMessage(ProxyMessage action) {
		this(Platform.getPlugin().getDefaultProxyListener(), action);
	}

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param listener
	 * @param action
	 */
	public OutgoingMessage(ProxyListener listener, ProxyMessage action) {
		super(listener, action);
	}

	/**
	 * Write the given strings into the message
	 *
	 * @param messages
	 */
	public void writeString(String... messages) {
		for (final String message : messages)
			this.write(message, String.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param component
	 */
	public void writeComponent(Component component) {
		this.write(component, Component.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param component
	 */
	public void writeSimpleComponent(SimpleComponentCore component) {
		this.write(component, SimpleComponentCore.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param map
	 */
	public void writeMap(SerializedMap map) {
		this.write(map, SerializedMap.class);
	}

	/**
	 * Write a boolean into the message
	 *
	 * @param bool
	 */
	public void writeBoolean(boolean bool) {
		this.write(bool, Boolean.class);
	}

	/**
	 * Write a byte into the message
	 *
	 * @param number
	 */
	public void writeByte(byte number) {
		this.write(number, Byte.class);
	}

	/**
	 * Write a double into the message
	 *
	 * @param number
	 */
	public void writeDouble(double number) {
		this.write(number, Double.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeFloat(float number) {
		this.write(number, Float.class);
	}

	/**
	 * Write an integer into the message
	 *
	 * @param number
	 */
	public void writeInt(int number) {
		this.write(number, Integer.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeLong(long number) {
		this.write(number, Long.class);
	}

	/**
	 * Write a short into the message
	 *
	 * @param number
	 */
	public void writeShort(short number) {
		this.write(number, Short.class);
	}

	/**
	 * Write an uuid into the message
	 *
	 * @param uuid
	 */
	public void writeUUID(UUID uuid) {
		this.write(uuid, UUID.class);
	}

	/**
	 * Write an object of the given type into the message
	 *
	 * @param object
	 * @param typeOf
	 */
	public void write(Object object, Class<?> typeOf) {
		ValidCore.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * Shortcut to send this message as the given sender to proxy
	 *
	 * @param senderUid
	 */
	public void send(UUID senderUid) {
		final String channel = this.getChannel();
		final ProxyMessage message = this.getMessage();
		final Object[] dataArray = this.queue.toArray();

		ValidCore.checkBoolean(dataArray.length == message.getContent().length,
				"Proxy message " + message + " on channel " + channel + " has invalid data lenght! Expected: " + message.getContent().length + ". Got: " + dataArray.length);

		if (!message.name().equals("PLAYERS_CLUSTER_DATA") && Debugger.isDebugged("proxy"))
			Debugger.put("proxy", "Sending proxy message " + message + " on channel " + channel + " with data: " + CommonCore.join(dataArray, t -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Mode.YAML, t), "").toString()));

		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

		try {
			out.writeUTF(channel);
			out.writeUTF(senderUid.toString());
			out.writeUTF(RemainCore.getServerName());
			out.writeUTF(message.toString());

		} catch (final IOException ex) {
			CommonCore.error(ex,
					"Error writing header in proxy plugin message!",
					"Message: " + message,
					"Channel: " + channel,
					"Error: %error%",
					"All data: " + CommonCore.join(dataArray, data2 -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Mode.YAML, data2), "").toString()));
		}

		for (final Object data : dataArray)
			try {
				if (data instanceof Integer) {
					out.writeInt((Integer) data);

				} else if (data instanceof Double) {
					out.writeDouble((Double) data);

				} else if (data instanceof Long) {
					out.writeLong((Long) data);

				} else if (data instanceof Boolean) {
					out.writeBoolean((Boolean) data);

				} else if (data instanceof String) {
					OutgoingMessage.writeCompressedString(out, (String) data);

				} else if (data instanceof Component) {
					OutgoingMessage.writeCompressedString(out, RemainCore.convertAdventureToJson((Component) data));

				} else if (data instanceof SimpleComponentCore) {
					OutgoingMessage.writeCompressedString(out, ((SimpleComponentCore) data).serialize().toJson());

				} else if (data instanceof SerializedMap) {
					OutgoingMessage.writeCompressedString(out, ((SerializedMap) data).toJson());

				} else if (data instanceof UUID) {
					out.writeUTF(((UUID) data).toString());

				} else if (data instanceof Enum) {
					out.writeUTF(((Enum<?>) data).toString());

				} else if (data instanceof byte[]) {
					out.write((byte[]) data);

				} else
					throw new IllegalArgumentException("Unknown data type to write as plugin message: " + data.getClass());

			} catch (final Throwable t) {
				CommonCore.error(t,
						"Error writing data in proxy plugin message!",
						"Message: " + message,
						"Channel: " + channel,
						"Wrong data: " + data,
						"Error: %error%",
						"All data: " + CommonCore.join(dataArray, data2 -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Mode.YAML, data2), "").toString()));

				return;
			}

		final byte[] byteArray = byteArrayOutputStream.toByteArray();

		if (byteArray.length >= MAX_MESSAGE_SIZE) {
			CommonCore.log("Outgoing proxy message '" + message + "' was oversized, not sending. Max length: " + MAX_MESSAGE_SIZE + " bytes, got " + byteArray.length + " bytes.");

			return;
		}

		try {
			Platform.sendPluginMessage(senderUid, ProxyListener.DEFAULT_CHANNEL, byteArray);

		} catch (final Throwable throwable) {
			final String throwableClass = throwable.getClass().getSimpleName();

			if (throwableClass.equals("NotRegistered"))
				CommonCore.log("Cannot send proxy message " + message + " because channel '" + ProxyListener.DEFAULT_CHANNEL + "/" + channel + "' is not registered. "
						+ "Use @AutoRegister above your class extending ProxyListener and return its instance in getProxy in your main plugin class.");

			else
				CommonCore.error(throwable, "Failed to send proxy message " + message + " on channel " + channel);
		}
	}

	/**
	 *
	 * @return
	 */
	protected String getChannel() {
		return this.getListener().getChannel();
	}

	/**
	 * Writes a compressed string to the output
	 *
	 * @param out
	 * @param data
	 */
	public static void writeCompressedString(DataOutput out, String data) {
		final byte[] compressed = CommonCore.compress(data);

		try {
			out.writeInt(compressed.length);
			out.write(compressed);

		} catch (final Exception ex) {
			throw new FoException("Failed to write compressed String: " + data, ex);
		}
	}
}