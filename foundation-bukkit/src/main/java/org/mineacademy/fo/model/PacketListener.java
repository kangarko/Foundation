package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.SimplePlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * Extend this class to listen to packets. Requires ProtocolLib.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PacketListener {

	/**
	 * Called automatically when you use \@AutoRegister, inject
	 * your packet listeners here.
	 */
	public abstract void onRegister();

	/**
	 * A convenience shortcut to add packet listener
	 *
	 * @param adapter
	 */
	protected void addPacketListener(final SimpleAdapter adapter) {
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
	protected void addReceivingListener(final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addReceivingListener(ListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * A convenience method for listening to Client>Server packets of the given type and priority.
	 *
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	protected void addReceivingListener(final ListenerPriority priority, final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addPacketListener(new SimpleAdapter(priority, type) {

			/**
			 * @see com.comphenix.protocol.events.PacketAdapter#onPacketReceiving(com.comphenix.protocol.events.PacketEvent)
			 */
			@Override
			public void onPacketReceiving(final PacketEvent event) {

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
	protected void addSendingListener(final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addSendingListener(ListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * A convenience method for listening to Server>Client packets of the given type and priority.
	 *
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	protected void addSendingListener(final ListenerPriority priority, final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addPacketListener(new SimpleAdapter(priority, type) {

			/**
			 * @see com.comphenix.protocol.events.PacketAdapter#onPacketReceiving(com.comphenix.protocol.events.PacketEvent)
			 */
			@Override
			public void onPacketSending(final PacketEvent event) {

				if (event.getPlayer() != null)
					consumer.accept(event);
			}

			@Override
			public void onPacketReceiving(PacketEvent event) {
				if (type == PacketType.Play.Server.CHAT || type == PacketType.Play.Client.CHAT) {
					// Packet can be both sided
				} else
					super.onPacketReceiving(event);
			}
		});
	}

	/**
	 * Sets the hoverable text in the server's menu
	 * To use this, create a new addSendingListener for PacketType.Status.Server.SERVER_INFO
	 * and get the {@link WrappedServerPing} from event.getPacket().getServerPings().read(0)
	 * then finallly call WrappedServerPing#setPlayers method
	 *
	 * @param hoverTexts
	 */
	protected List<WrappedGameProfile> compileHoverText(final String... hoverTexts) {
		final List<WrappedGameProfile> profiles = new ArrayList<>();

		int count = 0;

		for (final String hoverText : hoverTexts) {
			final String colorized = CompChatColor.translateColorCodes(hoverText);
			WrappedGameProfile profile;

			try {
				profile = new WrappedGameProfile(UUID.randomUUID(), colorized);

			} catch (final Throwable t) {
				profile = new WrappedGameProfile(String.valueOf(count++), colorized);
			}

			profiles.add(profile);
		}

		return profiles;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A convenience adapter for handling chat packets doing most of the heavy work for you.
	 */
	protected abstract class SimpleChatAdapter extends SimpleAdapter {

		/**
		 * Players being processed RIGHT NOW inside the method. Prevents dead loop.
		 */
		private final Set<String> processedPlayers = new HashSet<>();

		/**
		 * Cached flags for performance purposes.
		 */
		private int actionBarMode = -1;
		private Boolean hasAdventure = null;
		private Boolean hasBungee = null;
		private Boolean hasIChatBase = null;

		/**
		 * Create new chat listener
		 */
		public SimpleChatAdapter() {
			super(ListenerPriority.HIGHEST, MinecraftVersion.atLeast(V.v1_19) ? PacketType.Play.Server.SYSTEM_CHAT : PacketType.Play.Server.CHAT);
		}

		@Override
		public void onPacketSending(final PacketEvent event) {
			final Player player = event.getPlayer();

			if (player == null)
				return;

			final String playerName = event.getPlayer().getName();
			final PacketContainer packet = event.getPacket();

			// Ignore dummy instances and disabled plugin or processed players
			if (!player.isOnline() || !SimplePlugin.getInstance().isEnabled() || this.processedPlayers.contains(playerName))
				return;

			// Ignore action bar messages
			if (this.actionBarMode == -1) {
				if (!packet.getBooleans().getFields().isEmpty())
					this.actionBarMode = 1;

				else if (!packet.getBytes().getFields().isEmpty())
					this.actionBarMode = 2;

				else if (!packet.getChatTypes().getFields().isEmpty())
					this.actionBarMode = 3;

				else
					throw new FoException("Unknown way to find if chat packet is action bar, packet: " + packet.getHandle().getClass());
			}

			if (this.actionBarMode == 1) {
				if (packet.getBooleans().read(0) == true)
					return;

			} else if (this.actionBarMode == 2) {
				if (packet.getBytes().read(0) == (byte) 0)
					return;

			} else if (this.actionBarMode == 3) {
				if (packet.getChatTypes().read(0) == ChatType.GAME_INFO)
					return;
			}

			// Cache booleans for faster performance: 0.3ms vs ~1ms
			if (this.hasAdventure == null) {
				this.hasAdventure = !event.getPacket().getModifier().withType(Component.class).getFields().isEmpty();
				this.hasBungee = !event.getPacket().getModifier().withType(BaseComponent[].class).getFields().isEmpty();
				this.hasIChatBase = !event.getPacket().getChatComponents().getFields().isEmpty();
			}

			// Lock processing to one instance only to prevent another packet filtering
			try {
				this.processedPlayers.add(playerName);

				final StructureModifier<Component> modifierAdventure = this.hasAdventure ? event.getPacket().getModifier().withType(Component.class) : null;
				final StructureModifier<BaseComponent[]> modifierBaseComponent = this.hasBungee ? event.getPacket().getModifier().withType(BaseComponent[].class) : null;
				final StructureModifier<WrappedChatComponent> modifierIChatBaseComponent = this.hasIChatBase ? event.getPacket().getChatComponents() : null;

				String json = null;

				if (this.hasAdventure) {
					final Component component = modifierAdventure.read(0);

					if (component != null)
						json = GsonComponentSerializer.gson().serialize(component);
				}

				if (json == null && !"".equals(json) && !"{}".equals(json) && this.hasBungee) {
					final BaseComponent[] components = modifierBaseComponent.read(0);

					if (components != null)
						json = GsonComponentSerializer.gson().serialize(BungeeComponentSerializer.get().deserialize(components));
				}

				if (json == null && !"".equals(json) && !"{}".equals(json) && this.hasIChatBase) {
					final WrappedChatComponent chatComponent = modifierIChatBaseComponent.read(0);

					if (chatComponent != null)
						json = chatComponent.getJson();
				}

				if (json != null && json.length() < 50_000) {

					// This flag effectivelly doubles processing time from ~0.3ms to ~0.6ms that is why it needs to be explicitly enabled
					final boolean editJson = this.editJson();
					final Component oldJson = editJson ? GsonComponentSerializer.gson().deserialize(json) : null;

					try {
						json = this.onJsonMessage(player, json);

					} catch (final EventHandledException ex) {
						event.setCancelled(true);

						return;
					}

					if (editJson) {
						final Component newJson = GsonComponentSerializer.gson().deserialize(json);

						if (!newJson.equals(oldJson)) {
							if (this.hasAdventure)
								modifierAdventure.write(0, newJson);

							else if (this.hasBungee)
								modifierBaseComponent.write(0, BungeeComponentSerializer.get().serialize(newJson));

							else if (this.hasIChatBase)
								modifierIChatBaseComponent.write(0, WrappedChatComponent.fromJson(json));
						}
					}
				}

			} finally {
				this.processedPlayers.remove(player.getName());
			}
		}

		/**
		 * Called when chat message packet is received.
		 *
		 * If you edit the jsonMessage we do NOT set it back unless you call
		 * {@link #editJson()} and set it to true.
		 *
		 * To cancel cancel the packet, throw {@link EventHandledException}
		 *
		 * @param player
		 * @param json
		 *
		 * @return
		 */
		protected String onJsonMessage(Player player, String json) {
			return json;
		}

		/**
		 * For performance purposes, json message in {@link #jsonMessage} is not edited by default
		 * Return true to change this behavior.
		 *
		 * @return
		 */
		protected boolean editJson() {
			return false;
		}
	}

	/**
	 * A convenience class so that you don't have to specify which plugin is the owner of the packet adapter
	 */
	protected class SimpleAdapter extends PacketAdapter {

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
		public SimpleAdapter(final PacketType type) {
			this(ListenerPriority.NORMAL, type);
		}

		/**
		 * Create a new packet adapter for the given packet type with the given priority
		 *
		 * @param priority
		 * @param type
		 */
		public SimpleAdapter(final ListenerPriority priority, final PacketType type) {
			super(SimplePlugin.getInstance(), priority, type);

			this.type = type;
		}

		/**
		 * This method is automatically fired when the client sends the {@link #type} to the server.
		 *
		 * @param event
		 */
		@Override
		public void onPacketReceiving(final PacketEvent event) {
			throw new FoException("Override onPacketReceiving to handle receiving client>server packet type " + this.type);
		}

		/**
		 * This method is automatically fired when the server wants to send the {@link #type} to the client.
		 *
		 * @param event
		 */
		@Override
		public void onPacketSending(final PacketEvent event) {
			throw new FoException("Override onPacketReceiving to handle sending server>client packet type " + this.type);
		}
	}
}
