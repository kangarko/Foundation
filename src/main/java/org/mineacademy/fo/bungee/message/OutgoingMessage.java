package org.mineacademy.fo.bungee.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.bungee.BungeeMessageType;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.plugin.SimplePlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link BungeeMessageType} by its name *written automatically*.
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
	public OutgoingMessage(BungeeMessageType action) {
		this(SimplePlugin.getInstance().getBungeeCord(), action);
	}

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param listener
	 * @param action
	 */
	public OutgoingMessage(BungeeListener listener, BungeeMessageType action) {
		super(listener, action);
	}

	/**
	 * Write a compatible object into the message
	 *
	 * @param map
	 */
	public void write(ConfigSerializable map) {
		this.write(map.serialize().toJson(), String.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param map
	 */
	public void writeMap(SerializedMap map) {
		this.write(map.toJson(), String.class);
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
	 * <p>
	 * We move the head and ensure writing safety in accordance
	 * to the {@link BungeeMessageType#getContent()} length and
	 * data type at the given position
	 *
	 * @param object
	 * @param typeOf
	 */
	private void write(Object object, Class<?> typeOf) {
		Valid.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * Delegate write methods for the byte array data output
	 * based on the queue
	 *
	 * @param serverName
	 * @return
	 */
	public byte[] getData(String serverName) {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		// -----------------------------------------------------------------
		// We are automatically writing the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		out.writeUTF(this.getListener().getChannel());
		out.writeUTF(UUID.fromString("00000000-0000-0000-0000-000000000000").toString());
		out.writeUTF(serverName);
		out.writeUTF(this.getAction().name());

		for (final Object object : this.queue)
			if (object instanceof String)
				out.writeUTF((String) object);

			else if (object instanceof Boolean)
				out.writeBoolean((Boolean) object);

			else if (object instanceof Byte)
				out.writeByte((Byte) object);

			else if (object instanceof Double)
				out.writeDouble((Double) object);

			else if (object instanceof Float)
				out.writeFloat((Float) object);

			else if (object instanceof Integer)
				out.writeInt((Integer) object);

			else if (object instanceof Long)
				out.writeLong((Long) object);

			else if (object instanceof Short)
				out.writeShort((Short) object);

			else if (object instanceof byte[])
				out.write((byte[]) object);

			else if (object instanceof UUID)
				out.writeUTF(object.toString());

			else
				throw new FoException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + this.getChannel() + " with action " + this.getAction().toString());

		return out.toByteArray();
	}

	/**
	 *
	 * @return
	 */
	protected String getChannel() {
		return this.getListener().getChannel();
	}
}