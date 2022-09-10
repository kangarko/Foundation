package org.mineacademy.fo.bungee.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.plugin.SimplePlugin;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import lombok.Getter;

/**
 * Represents an incoming plugin message.
 * <p>
 * NB: This uses the standardized Foundation model where the first
 * string is the server name and the second string is the
 * {@link BungeeMessageType} by its name *read automatically*.
 */
public final class IncomingMessage extends Message {

	/**
	 * The raw byte array to read from
	 */
	@Getter
	private final byte[] data;

	/**
	 * The input we use to read our data array
	 */
	private ByteArrayDataInput input;

	/**
	 * The internal stream
	 */
	private final ByteArrayInputStream stream;

	/**
	 * Create a new incoming message from the given array
	 * <p>
	 * NB: This uses the standardized Foundation model where the first
	 * string is the server name and the second string is the
	 * {@link BungeeMessageType} by its name *read automatically*.
	 *
	 * @param data
	 */
	public IncomingMessage(byte[] data) {
		this(SimplePlugin.getInstance().getBungeeCord(), data);
	}

	/**
	 * Create a new incoming message from the given array
	 * <p>
	 * NB: This uses the standardized Foundation model where the first
	 * string is the server name and the second string is the
	 * {@link BungeeMessageType} by its name *read automatically*.
	 *
	 * @param listener
	 * @param data
	 */
	public IncomingMessage(BungeeListener listener, byte[] data) {
		super(listener);

		this.data = data;
		this.stream = new ByteArrayInputStream(data);

		try {
			this.input = ByteStreams.newDataInput(this.stream);

		} catch (final Throwable t) {
			this.input = ByteStreams.newDataInput(data);
		}
		// -----------------------------------------------------------------
		// We are automatically reading the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		// Read senders UUID
		this.setSenderUid(this.input.readUTF());

		// Read server name
		this.setServerName(this.input.readUTF());

		// Read action
		this.setAction(this.input.readUTF());
	}

	/**
	 * Read a string from the data
	 *
	 * @return
	 */
	public String readString() {
		this.moveHead(String.class);

		return this.input.readUTF();
	}

	/**
	 * Read a UUID from the string data
	 *
	 * @return
	 */
	public UUID readUUID() {
		this.moveHead(UUID.class);

		return UUID.fromString(this.input.readUTF());
	}

	/**
	 * Read a map from the string data if json
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		this.moveHead(String.class);

		return SerializedMap.fromJson(this.input.readUTF());
	}

	/**
	 * Read an enumerator from the given string data
	 *
	 * @param <T>
	 * @param typeOf
	 * @return
	 */
	public <T extends Enum<T>> T readEnum(Class<T> typeOf) {
		this.moveHead(typeOf);

		return ReflectionUtil.lookupEnum(typeOf, this.input.readUTF());
	}

	/**
	 * Read a boolean from the data
	 *
	 * @return
	 */
	public boolean readBoolean() {
		this.moveHead(Boolean.class);

		return this.input.readBoolean();
	}

	/**
	 * Read a byte from the data
	 *
	 * @return
	 */
	public byte readByte() {
		this.moveHead(Byte.class);

		return this.input.readByte();
	}

	/**
	 * Reads the rest of the bytes
	 *
	 * @return
	 */
	public byte[] readBytes() {
		this.moveHead(byte[].class);

		final byte[] array = new byte[this.stream.available()];

		try {
			this.stream.read(array);

		} catch (final IOException e) {
			e.printStackTrace();
		}

		return array;
	}

	/**
	 * Read a double from the data
	 *
	 * @return
	 */
	public double readDouble() {
		this.moveHead(Double.class);

		return this.input.readDouble();
	}

	/**
	 * Read a float from the data
	 *
	 * @return
	 */
	public float readFloat() {
		this.moveHead(Float.class);

		return this.input.readFloat();
	}

	/**
	 * Read an integer from the data
	 *
	 * @return
	 */
	public int writeInt() {
		this.moveHead(Integer.class);

		return this.input.readInt();
	}

	/**
	 * Read a long from the data
	 *
	 * @return
	 */
	public long readLong() {
		this.moveHead(Long.class);

		return this.input.readLong();
	}

	/**
	 * Read a short from the data
	 *
	 * @return
	 */
	public short readShort() {
		this.moveHead(Short.class);

		return this.input.readShort();
	}

	/**
	 * Forwards this message to a player
	 *
	 * @param player
	 */
	public void forward(Player player) {
		player.sendPluginMessage(SimplePlugin.getInstance(), this.getChannel(), this.data);
	}
}