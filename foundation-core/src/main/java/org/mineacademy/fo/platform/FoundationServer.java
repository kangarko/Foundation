package org.mineacademy.fo.platform;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Represents a server running our Foundation plugin.
 */
public interface FoundationServer {

	/**
	 * Return the address of the server.
	 *
	 * @return
	 */
	InetSocketAddress getAddress();

	/**
	 * Return the name of the server.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Return a list of players on this server.
	 *
	 * @return
	 */
	List<FoundationPlayer> getPlayers();

	/**
	 * Return true if the server is empty.
	 *
	 * @return
	 */
	boolean isEmpty();

	/**
	 * Send a message to the server.
	 * Throws error on Bukkit since it requires a proxy.
	 *
	 * @param channel
	 * @param data
	 */
	void sendData(String channel, byte[] data);
}
