package org.mineacademy.fo;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.RegexTimeoutException;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * Represents packet handling using ProtocolLib
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PacketUtil {

	/**
	 * A convenience shortcut to add packet listener
	 * 
	 * @param adapter
	 */
	public static void addPacketListener(SimpleAdapter adapter) {
		Valid.checkBoolean(HookManager.isVaultLoaded(), "ProtocolLib integration requires Vault to be installed. Please install that plugin before continuing.");

		HookManager.addPacketListener(adapter);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Receiving
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A convenience method for listening to Client>Server packets of the given type.
	 * 
	 * @param type
	 * @param consumer
	 */
	public static void addReceivingListener(PacketType type, Consumer<PacketEvent> consumer) {
		addReceivingListener(ListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * A convenience method for listening to Client>Server packets of the given type and priority.
	 * 
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	public static void addReceivingListener(ListenerPriority priority, PacketType type, Consumer<PacketEvent> consumer) {
		addPacketListener(new SimpleAdapter(priority, type) {

			/**
			 * @see com.comphenix.protocol.events.PacketAdapter#onPacketReceiving(com.comphenix.protocol.events.PacketEvent)
			 */
			@Override
			public void onPacketReceiving(PacketEvent event) {

				if (event.getPlayer() != null)
					consumer.accept(event);
			}
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Sending
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A convenience method for listening to Server>Client packets of the given type.
	 * 
	 * @param type
	 * @param consumer
	 */
	public static void addSendingListener(PacketType type, Consumer<PacketEvent> consumer) {
		addSendingListener(ListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * A convenience method for listening to Server>Client packets of the given type and priority.
	 * 
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	public static void addSendingListener(ListenerPriority priority, PacketType type, Consumer<PacketEvent> consumer) {
		addPacketListener(new SimpleAdapter(priority, type) {

			/**
			 * @see com.comphenix.protocol.events.PacketAdapter#onPacketReceiving(com.comphenix.protocol.events.PacketEvent)
			 */
			@Override
			public void onPacketSending(PacketEvent event) {

				if (event.getPlayer() != null)
					consumer.accept(event);
			}
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A convenience adapter for handling chat packets doing most of the heavy work for you.
	 */
	public static abstract class SimpleChatAdapter extends SimpleAdapter {

		/**
		 * Players being processed RIGHT NOW inside the method. Prevents dead loop.
		 */
		private final Set<String> processedPlayers = new HashSet<>();

		/**
		 * The event field for convenient calling in the overridable methods
		 */
		@Getter
		private PacketEvent event;

		/**
		 * The player field you can use below
		 */
		@Getter
		private Player player;

		/**
		 * @param params
		 */
		public SimpleChatAdapter() {
			super(ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT);
		}

		@Override
		public void onPacketSending(PacketEvent event) {
			if (event.getPlayer() == null)
				return;

			this.event = event;
			this.player = event.getPlayer();

			final String playerName = event.getPlayer().getName();

			// Ignore temporary players
			try {
				this.player.getUniqueId();

			} catch (final UnsupportedOperationException ex) {
				return;
			}

			// Ignore dummy instances and rare reload case
			if (!this.player.isOnline() || SimplePlugin.isReloading())
				return;

			// Prevent deadlock
			if (this.processedPlayers.contains(playerName))
				return;

			// Lock processing to one instance only to prevent another packet filtering
			// in a filtering
			try {
				this.processedPlayers.add(playerName);

				final StructureModifier<Object> packet = event.getPacket().getModifier();
				final StructureModifier<WrappedChatComponent> chat = event.getPacket().getChatComponents();
				final WrappedChatComponent component = chat.read(0);

				try {
					final ChatType chatType = event.getPacket().getChatTypes().readSafely(0);

					if (chatType == ChatType.GAME_INFO)
						return;

				} catch (final NoSuchMethodError t) {
					// Silence on legacy MC
				}

				boolean isBaseComponent = false;
				boolean adventure = false;
				String jsonMessage = null;

				if (component != null)
					jsonMessage = component.getJson();

				// Md_5 way of dealing with packets
				else if (packet.size() > 1) {
					Object secondField = packet.readSafely(1);

					// Support "Adventure" library in PaperSpigot
					if (secondField == null) {
						secondField = packet.readSafely(2);

						if (secondField != null)
							adventure = true;
					}

					if (secondField instanceof BaseComponent[]) {
						jsonMessage = Remain.toJson((BaseComponent[]) secondField);

						isBaseComponent = true;
					}
				}

				if (jsonMessage != null && !jsonMessage.isEmpty()) {

					// Only check valid messages, skipping those over 50k since it would cause rules
					// to take too long and overflow. 99% packets are below this size, it may even be
					// that such oversized packets are maliciously sent so we protect the server from freeze
					if (jsonMessage.length() < 50_000) {
						final String legacyText;

						// Catch errors from other plugins and silence them
						try {
							legacyText = Remain.toLegacyText(jsonMessage, false);

						} catch (final Throwable t) {
							return;
						}

						final String parsedText = legacyText;

						try {
							Debugger.debug("packet", "Chat packet parsed message: '" + Common.stripColors(parsedText) + "'");

							this.onMessage(parsedText);

						} catch (final RegexTimeoutException ex) {
							// Such errors mean the parsed message took too long to process.
							// Only show such errors every 30 minutes to prevent console spam
							Common.logTimed(1800, "&cWarning: &fPacket message '" + Common.limit(jsonMessage, 500)
									+ "' (possibly longer) took too long time to edit received message and was ignored."
									+ " This message only shows once per 30 minutes when that happens. For most cases, this can be ignored.");

							return;

						} catch (final EventHandledException ex) {
							event.setCancelled(true);

							return;
						}

						if (!Common.stripColors(legacyText).equals(Common.stripColors(parsedText))) {
							jsonMessage = Remain.toJson(parsedText);

							if (isBaseComponent)
								packet.writeSafely(adventure ? 2 : 1, Remain.toComponent(jsonMessage));

							else
								chat.writeSafely(0, WrappedChatComponent.fromJson(jsonMessage));
						}
					}

					this.onJsonMessage(jsonMessage);
				}

			} finally {
				this.processedPlayers.remove(this.player.getName());
			}
		}

		/**
		 * Called automatically when we receive and decipher a chat message packet.
		 * 
		 * You can use {@link #getEvent()} and {@link #getPlayer()} here.
		 * The preferred way of cancelling the packet is throwing an {@link EventHandledException}
		 * 
		 * If you edit the message we automatically set it.
		 * 
		 * @param message
		 * @return
		 */
		protected abstract String onMessage(String message);

		/**
		 * Called automatically when we receive the chat message and decipher it into plain json.
		 * You can use {@link #getEvent()} and {@link #getPlayer()} here.
		 * 
		 * If you edit the jsonMessage we do NOT set it back.
		 * 
		 * @param jsonMessage
		 */
		protected void onJsonMessage(String jsonMessage) {
		}
	}

	/**
	 * A convenience class so that you don't have to specify which plugin is the owner of the packet adapter
	 */
	public static class SimpleAdapter extends PacketAdapter {

		/**
		 * The packet we're listening for
		 */
		@Getter
		private final PacketType type;

		/**
		 * Create a new packet adapter for the given packet type
		 * 
		 * @param type
		 */
		public SimpleAdapter(PacketType type) {
			this(ListenerPriority.NORMAL, type);
		}

		/**
		 * Create a new packet adapter for the given packet type with the given priority
		 * 
		 * @param priority
		 * @param type
		 */
		public SimpleAdapter(ListenerPriority priority, PacketType type) {
			super(SimplePlugin.getInstance(), priority, type);

			this.type = type;
		}

		/**
		 * This method is automatically fired when the client sends the {@link #type} to the server.
		 * 
		 * @param event
		 */
		@Override
		public void onPacketReceiving(PacketEvent event) {
			throw new FoException("Override onPacketReceiving to handle receiving client>server packet type " + this.type);
		}

		/**
		 * This method is automatically fired when the server wants to send the {@link #type} to the client.
		 * 
		 * @param event
		 */
		@Override
		public void onPacketSending(PacketEvent event) {
			throw new FoException("Override onPacketReceiving to handle sending server>client packet type " + this.type);
		}
	}
}
