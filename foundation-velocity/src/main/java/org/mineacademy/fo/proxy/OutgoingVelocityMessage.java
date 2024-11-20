package org.mineacademy.fo.proxy;

import javax.annotation.Nullable;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.platform.SimplePlugin;
import org.mineacademy.fo.proxy.message.OutgoingMessage;
import org.mineacademy.fo.remain.Remain;

import com.velocitypowered.api.proxy.server.RegisteredServer;

public final class OutgoingVelocityMessage extends OutgoingMessage {

	/**
	 * Create a new outgoing message, see header of {@link OutgoingMessage}
	 *
	 * @param listener
	 * @param message
	 */
	public OutgoingVelocityMessage(ProxyListener listener, ProxyMessage message) {
		super(listener, message);
	}

	/**
	 * Create a new outgoing message, see header of {@link OutgoingMessage}
	 *
	 * @param message
	 */
	public OutgoingVelocityMessage(ProxyMessage message) {
		super(message);
	}

	/**
	 * Forwards this message to another server
	 *
	 * @param fromServer
	 * @param info
	 */
	public void sendToServer(String fromServer, RegisteredServer info) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			final String channel = this.getChannel();
			final byte[] byteArray = this.toByteArray(Common.CONSOLE_UID, fromServer);

			if (info.getPlayersConnected().isEmpty()) {
				Debugger.debug("bungee", "NOT sending data on " + channel + " channel from " + this + " to " + info.getServerInfo().getName() + " server because it is empty.");

				return;
			}

			if (byteArray.length >= OutgoingMessage.MAX_MESSAGE_SIZE) {
				CommonCore.log("Outgoing proxy message '" + this + "' was oversized, not sending. Max length: " + OutgoingMessage.MAX_MESSAGE_SIZE + " bytes, got " + byteArray.length + " bytes.");

				return;
			}

			info.sendPluginMessage(SimplePlugin.LEGACY_BUNGEE_CHANNEL, byteArray);
			Debugger.debug("bungee", "Forwarding data on " + channel + " channel from " + this + " to " + info.getServerInfo().getName() + " server.");
		}
	}

	/**
	 * Broadcasts the message to all servers
	 */
	public void broadcast() {
		broadcastExcept(null);
	}

	/**
	 * Broadcasts the message to all servers except the one ignored
	 *
	 * @param ignoredServerName
	 */
	public void broadcastExcept(@Nullable String ignoredServerName) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			final String channel = this.getChannel();

			for (final RegisteredServer otherServer : Remain.getServers()) {
				if (otherServer.getPlayersConnected().isEmpty()) {
					Debugger.debug("bungee", "NOT sending data on " + channel + " channel from " + this + " to " + otherServer.getServerInfo().getName() + " server because it is empty.");

					continue;
				}

				if (ignoredServerName != null && otherServer.getServerInfo().getName().equalsIgnoreCase(ignoredServerName)) {
					Debugger.debug("bungee", "NOT sending data on " + channel + " channel from " + this + " to " + otherServer.getServerInfo().getName() + " server because it is ignored.");

					continue;
				}

				final byte[] byteArray = this.toByteArray(Common.CONSOLE_UID, otherServer.getServerInfo().getName());

				if (byteArray.length >= OutgoingMessage.MAX_MESSAGE_SIZE) {
					CommonCore.log("Outgoing proxy message '" + this + "' was oversized, not sending. Max length: " + OutgoingMessage.MAX_MESSAGE_SIZE + " bytes, got " + byteArray.length + " bytes.");

					return;
				}

				otherServer.sendPluginMessage(SimplePlugin.LEGACY_BUNGEE_CHANNEL, byteArray);
				Debugger.debug("bungee", "Sending data on " + channel + " channel from " + this + " to " + otherServer.getServerInfo().getName() + " server.");
			}
		}
	}
}
