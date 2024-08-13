package org.mineacademy.fo.proxy.message;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtilCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.SimpleComponentCore;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.remain.RemainCore;

import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Represents an incoming plugin message.
 * <p>
 * NB: This uses the standardized Foundation model where the first
 * string is the server name and the second string is the
 * {@link ProxyMessage} by its name *read automatically*.
 */
public final class IncomingMessage extends Message {

	/**
	 * The raw byte array to read from
	 */
	@Getter
	private final byte[] data;

	/**
	 * The sender UUID
	 */
	@Getter
	private final UUID senderUid;

	/**
	 * The serverName
	 */
	@Getter
	private final String serverName;

	/**
	 * The input we use to read our data array
	 */
	private final DataInput input;

	/**
	 * The internal stream
	 */
	private final ByteArrayInputStream stream;

	/**
	 * Create a new incoming message from the given array
	 *
	 * NB: This uses the standardized Foundation header:
	 *
	 * 1. Channel name (string) (this is the actual individualized channel name because we broadcast on proxy channel)
	 * 2. Sender UUID (string)
	 * 3. Server name (string)
	 * 4  Action (String converted to enum of {@link ProxyMessage})
	 *
	 * @param listener
	 * @param senderUid
	 * @param serverName
	 * @param type
	 * @param data
	 * @param input
	 * @param stream
	 */
	public IncomingMessage(ProxyListener listener, UUID senderUid, String serverName, ProxyMessage type, byte[] data, DataInput input, ByteArrayInputStream stream) {
		super(listener, type);

		this.data = data;
		this.senderUid = senderUid;
		this.serverName = serverName;
		this.input = input;
		this.stream = stream;
	}

	/**
	 * Read a string from the data
	 *
	 * @return
	 */
	public String readString() {
		this.moveHead(String.class);

		return this.readCompressedString();
	}

	/**
	 * Read a component from the string data if json
	 *
	 * @return
	 */
	public Component readComponent() {
		this.moveHead(Component.class);

		return RemainCore.convertJsonToAdventure(this.readCompressedString());
	}

	/**
	 * Read a simple component from the string data if json
	 *
	 * @return
	 */
	public SimpleComponentCore readSimpleComponent() {
		this.moveHead(SimpleComponentCore.class);

		return SimpleComponentCore.fromJson(this.readCompressedString());
	}

	/**
	 * Read a map from the string data if json
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		this.moveHead(SerializedMap.class);

		return SerializedMap.fromJson(this.readCompressedString());
	}

	/**
	 * Read a UUID from the string data
	 *
	 * @return
	 */
	public UUID readUUID() {
		this.moveHead(UUID.class);

		try {
			return UUID.fromString(this.input.readUTF());

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return null;
		}
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

		try {
			return ReflectionUtilCore.lookupEnum(typeOf, this.input.readUTF());

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return null;
		}
	}

	/**
	 * Read a boolean from the data
	 *
	 * @return
	 */
	public boolean readBoolean() {
		this.moveHead(Boolean.class);

		try {
			return this.input.readBoolean();

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return false;
		}
	}

	/**
	 * Read a byte from the data
	 *
	 * @return
	 */
	public byte readByte() {
		this.moveHead(Byte.class);

		try {
			return this.input.readByte();

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return 0;
		}
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

		try {
			return this.input.readDouble();

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read a float from the data
	 *
	 * @return
	 */
	public float readFloat() {
		this.moveHead(Float.class);

		try {
			return this.input.readFloat();

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read an integer from the data
	 *
	 * @return
	 */
	public int readInt() {
		this.moveHead(Integer.class);

		try {
			return this.input.readInt();

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read a long from the data
	 *
	 * @return
	 */
	public long readLong() {
		this.moveHead(Long.class);

		try {
			return this.input.readLong();

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read a short from the data
	 *
	 * @return
	 */
	public short readShort() {
		this.moveHead(Short.class);

		try {
			return this.input.readShort();

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return 0;
		}
	}

	/*
	 * Helper util to read the next compressed string
	 */
	private String readCompressedString() {
		try {
			final int length = this.input.readInt();
			final byte[] compressed = new byte[length];

			this.input.readFully(compressed);

			return CommonCore.decompress(compressed);

		} catch (final IOException ex) {
			RemainCore.sneaky(ex);

			return null;
		}
	}

	/**
	 *
	 * @return
	 */
	public String getChannel() {
		return this.getListener().getChannel();
	}
}