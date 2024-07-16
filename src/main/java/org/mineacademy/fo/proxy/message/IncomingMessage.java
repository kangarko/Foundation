package org.mineacademy.fo.proxy.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;
import org.mineacademy.fo.remain.Remain;

import com.google.common.io.ByteArrayDataInput;

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
	private final ByteArrayDataInput input;

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
	public IncomingMessage(ProxyListener listener, UUID senderUid, String serverName, ProxyMessage type, byte[] data, ByteArrayDataInput input, ByteArrayInputStream stream) {
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

		return Remain.convertJsonToAdventure(this.readCompressedString());
	}

	/**
	 * Read a simple component from the string data if json
	 *
	 * @return
	 */
	public SimpleComponent readSimpleComponent() {
		this.moveHead(SimpleComponent.class);

		return SimpleComponent.fromJson(this.readCompressedString());
	}

	/**
	 * Read a map from the string data if json
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		this.moveHead(String.class);

		return SerializedMap.fromJson(this.readCompressedString());
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
	public int readInt() {
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

	/*
	 * Helper util to read the next compressed string
	 */
	private String readCompressedString() {
		final int length = this.input.readInt();
		final byte[] compressed = new byte[length];

		this.input.readFully(compressed);

		return Common.decompress(compressed);
	}

	/**
	 *
	 * @return
	 */
	public String getChannel() {
		return this.getListener().getChannel();
	}
}