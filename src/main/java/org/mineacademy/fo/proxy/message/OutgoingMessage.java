package org.mineacademy.fo.proxy.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ProxyUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.proxy.ProxyListener;
import org.mineacademy.fo.proxy.ProxyMessage;

import com.google.common.io.ByteArrayDataOutput;

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
		this(SimplePlugin.getInstance().getProxyListener(), action);
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

	/*
	 * Write an object of the given type into the message
	 */
	private void write(Object object, Class<?> typeOf) {
		Valid.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * Shortcut to send this message as the given sender to proxy
	 *
	 * @param sender
	 */
	public void send(@Nullable Player sender) {
		ProxyUtil.sendPluginMessage(sender, this.getChannel(), this.getMessage(), this.queue.toArray());
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
	public static void writeCompressedString(ByteArrayDataOutput out, String data) {
		final byte[] compressed = Common.compress(data);

		out.writeInt(compressed.length);
		out.write(compressed);
	}
}