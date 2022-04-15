package org.mineacademy.fo.bungee.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.bungee.BungeeMessageType;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
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
	private final ByteArrayDataInput input;

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
		this.input = ByteStreams.newDataInput(stream);

		// -----------------------------------------------------------------
		// We are automatically reading the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		// Read senders UUID
		setSenderUid(input.readUTF());

		// Read server name
		setServerName(input.readUTF());

		// Read action
		setAction(input.readUTF());
	}

	/**
	 * Read a string from the data
	 *
	 * @return
	 */
	public String readString() {
		moveHead(String.class);

		return input.readUTF();
	}

	/**
	 * Read a UUID from the string data
	 *
	 * @return
	 */
	public UUID readUUID() {
		moveHead(UUID.class);

		return UUID.fromString(input.readUTF());
	}

	/**
	 * Read a map from the string data if json
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		moveHead(String.class);

		return SerializedMap.fromJson(input.readUTF());
	}

	/**
	 * Read an enumerator from the given string data
	 *
	 * @param <T>
	 * @param typeOf
	 * @return
	 */
	public <T extends Enum<T>> T readEnum(Class<T> typeOf) {
		moveHead(typeOf);

		return ReflectionUtil.lookupEnum(typeOf, input.readUTF());
	}

	/**
	 * Read a boolean from the data
	 *
	 * @return
	 */
	public boolean readBoolean() {
		moveHead(Boolean.class);

		return input.readBoolean();
	}

	/**
	 * Read a byte from the data
	 *
	 * @return
	 */
	public byte readByte() {
		moveHead(Byte.class);

		return input.readByte();
	}

	/**
	 * Reads the rest of the bytes
	 *
	 * @return
	 */
	public byte[] readBytes() {
		moveHead(byte[].class);

		final byte[] array = new byte[stream.available()];

		try {
			stream.read(array);

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
		moveHead(Double.class);

		return input.readDouble();
	}

	/**
	 * Read a float from the data
	 *
	 * @return
	 */
	public float readFloat() {
		moveHead(Float.class);

		return input.readFloat();
	}

	/**
	 * Read an integer from the data
	 *
	 * @return
	 */
	public int writeInt() {
		moveHead(Integer.class);

		return input.readInt();
	}

	/**
	 * Read a long from the data
	 *
	 * @return
	 */
	public long readLong() {
		moveHead(Long.class);

		return input.readLong();
	}

	/**
	 * Read a short from the data
	 *
	 * @return
	 */
	public short readShort() {
		moveHead(Short.class);

		return input.readShort();
	}

	/**
	 * Forwards this message to a player
	 *
	 * @param player
	 */
	public void forward(Player player) {
		player.sendPluginMessage(SimplePlugin.getInstance(), getChannel(), data);

		Debugger.debug("bungee", "Forwarding data on " + getChannel() + " channel from " + getAction() + " as " + player.getName() + " player to BungeeCord.");
	}
}