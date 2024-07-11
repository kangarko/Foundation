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
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;

/**
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link ProxyMessage} by its name *written automatically*.
 */
public final class OutgoingMessage extends Message {

	/**
	 * The pending queue to write the message.
	 */
	private final List<Object> queue = new ArrayList<>();

	/**
	 * Create a new outgoing message, see header of this class.
	 *
	 * @param action
	 */
	public OutgoingMessage(ProxyMessage action) {
		this(getDefaultListener(), action);
	}

	/*
	 * Get the default listener and ensure it is set.
	 */
	private static ProxyListener getDefaultListener() {
		final ProxyListener defaultListener = Platform.getPlugin().getDefaultProxyListener();
		ValidCore.checkNotNull(defaultListener, "Cannot call OutgoingMessage with no params because SimplePlugin#getDefaultProxyListener() is not set!");

		return defaultListener;
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
	public void writeSimpleComponent(SimpleComponent component) {
		this.write(component, SimpleComponent.class);
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
	 * Sends this outgoing message into proxy in the following format:
	 *
	 * You need an implementation in proxy to handle it, otherwise nothing will happen.
	 *
	 * <ul>
	 *   <li><b>Channel</b>: The identifier for the communication channel through
	 *       which the message is sent.</li>
	 *   <li><b>Sender UUID</b>: The unique identifier of the player or entity
	 *       that originates the message.</li>
	 *   <li><b>Server Name</b>: The name of the server sending the message, as
	 *       defined by the platform. Please call {@link Platform#setCustomServerName(String)} first.</li>
	 *   <li><b>Message Content</b>: A string representation of the proxy message
	 *       that describes its type or action.</li>
	 *	 <li><b>Data Array</b>: Byte array of data set in this class.</li>
	 * </ul>
	 * </code></pre>
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
			Debugger.debug("proxy", "Sending proxy message " + message + " on channel " + channel + " with data: " + CommonCore.join(dataArray, t -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Language.YAML, t), "").toString()));

		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

		try {
			out.writeUTF(channel);
			out.writeUTF(senderUid.toString());
			out.writeUTF(Platform.getCustomServerName());
			out.writeUTF(message.toString());

		} catch (final IOException ex) {
			CommonCore.error(ex,
					"Error writing header in proxy plugin message!",
					"Message: " + message,
					"Channel: " + channel,
					"Error: {error}",
					"All data: " + CommonCore.join(dataArray, data2 -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Language.YAML, data2), "").toString()));
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
					this.writeCompressedString(out, (String) data);

				} else if (data instanceof SimpleComponent) {
					this.writeCompressedString(out, ((SimpleComponent) data).serialize().toJson());

				} else if (data instanceof SerializedMap) {
					this.writeCompressedString(out, ((SerializedMap) data).toJson());

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
						"Error: {error}",
						"All data: " + CommonCore.join(dataArray, data2 -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Language.YAML, data2), "").toString()));

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
	 * Writes a compressed string to the output.
	 *
	 * @param out
	 * @param data
	 */
	private void writeCompressedString(DataOutput out, String data) {
		final byte[] compressed = CommonCore.compress(data);

		try {
			out.writeInt(compressed.length);
			out.write(compressed);

		} catch (final Exception ex) {
			throw new FoException("Failed to write compressed String: " + data, ex);
		}
	}

	/**
	 * Get the channel for this message.
	 *
	 * @return
	 */
	protected String getChannel() {
		ValidCore.checkNotNull(this.getListener(), "Listener cannot be null for " + this);

		return this.getListener().getChannel();
	}
}