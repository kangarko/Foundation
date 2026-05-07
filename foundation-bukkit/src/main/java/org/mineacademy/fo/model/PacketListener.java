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
import org.mineacademy.fo.platform.BukkitPlugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * Extend this class to listen to packets. Requires the PacketEvents plugin.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PacketListener {

	/**
	 * The PE listeners we registered, kept so they can be unregistered if needed.
	 */
	private final List<PacketListenerCommon> registered = new ArrayList<>();

	/**
	 * Called automatically when you use \@AutoRegister, inject your packet listeners here.
	 */
	public abstract void onRegister();

	// ------------------------------------------------------------------------------------------------------------
	// Receiving
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Listen to a Client>Server packet of the given type at NORMAL priority.
	 *
	 * @param type
	 * @param consumer
	 */
	protected final void addReceivingListener(final PacketTypeCommon type, final Consumer<PacketReceiveEvent> consumer) {
		this.addReceivingListener(PacketListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * Listen to a Client>Server packet of the given type and priority.
	 *
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	protected final void addReceivingListener(final PacketListenerPriority priority, final PacketTypeCommon type, final Consumer<PacketReceiveEvent> consumer) {
		this.register(new PacketListenerAbstract(priority) {

			@Override
			public void onPacketReceive(final PacketReceiveEvent event) {
				if (event.getPacketType() != type)
					return;

				// Player is null for non-PLAY state packets (HANDSHAKE / STATUS / LOGIN).
				// Consumers that dereference event.getPlayer() must guard against null.
				consumer.accept(event);
			}
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Sending
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Listen to a Server>Client packet of the given type at NORMAL priority.
	 *
	 * @param type
	 * @param consumer
	 */
	protected final void addSendingListener(final PacketTypeCommon type, final Consumer<PacketSendEvent> consumer) {
		this.addSendingListener(PacketListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * Listen to a Server>Client packet of the given type and priority.
	 *
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	protected final void addSendingListener(final PacketListenerPriority priority, final PacketTypeCommon type, final Consumer<PacketSendEvent> consumer) {
		this.register(new PacketListenerAbstract(priority) {

			@Override
			public void onPacketSend(final PacketSendEvent event) {
				if (event.getPacketType() != type)
					return;

				// Player is null for non-PLAY state packets (HANDSHAKE / STATUS / LOGIN).
				// Consumers that dereference event.getPlayer() must guard against null.
				consumer.accept(event);
			}
		});
	}

	/**
	 * Register a fully custom PE listener (e.g. a {@link SimpleChatAdapter} subclass).
	 *
	 * @param listener
	 */
	protected final void register(final PacketListenerCommon listener) {
		this.registered.add(PacketEvents.getAPI().getEventManager().registerListener(listener));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Server list ping helpers
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Compile a list of pseudo-player profiles for use in the server list ping
	 * "players sample" array (the hover text under the player count).
	 *
	 * @param hoverTexts
	 * @return
	 */
	protected final List<UserProfile> compileHoverText(final String... hoverTexts) {
		final List<UserProfile> profiles = new ArrayList<>(hoverTexts.length);

		for (final String hoverText : hoverTexts)
			profiles.add(new UserProfile(UUID.randomUUID(), CompChatColor.translateColorCodes(hoverText)));

		return profiles;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Inner adapters
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A convenience adapter for handling chat packets, supporting both modern
	 * SYSTEM_CHAT_MESSAGE (1.19+) and legacy CHAT_MESSAGE (pre-1.19).
	 *
	 * Override {@link #onJsonMessage(Player, String)} to inspect/mutate the
	 * chat JSON. To rewrite the message back, also override {@link #editJson()}
	 * and return true.
	 */
	protected abstract class SimpleChatAdapter extends PacketListenerAbstract {

		/**
		 * Players being processed RIGHT NOW inside the method. Prevents dead loop.
		 */
		private final Set<String> processedPlayers = new HashSet<>();

		public SimpleChatAdapter() {
			super(PacketListenerPriority.HIGHEST);
		}

		@Override
		public final void onPacketSend(final PacketSendEvent event) {
			final PacketTypeCommon type = event.getPacketType();
			final boolean modern = type == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE;
			final boolean legacy = type == PacketType.Play.Server.CHAT_MESSAGE;

			if (!modern && !legacy)
				return;

			final Player player = event.getPlayer();

			if (player == null || !player.isOnline() || !BukkitPlugin.getInstance().isEnabled())
				return;

			final String name = player.getName();

			if (this.processedPlayers.contains(name))
				return;

			try {
				this.processedPlayers.add(name);

				final WrapperPlayServerSystemChatMessage modernWrapper;
				final WrapperPlayServerChatMessage legacyWrapper;
				final Component before;

				if (modern) {
					modernWrapper = new WrapperPlayServerSystemChatMessage(event);

					// Skip action-bar / overlay messages
					if (modernWrapper.isOverlay())
						return;

					before = modernWrapper.getMessage();
					legacyWrapper = null;

				} else {
					legacyWrapper = new WrapperPlayServerChatMessage(event);
					before = legacyWrapper.getMessage().getChatContent();
					modernWrapper = null;
				}

				if (before == null)
					return;

				final boolean useLegacyHexFormat = MinecraftVersion.olderThan(V.v1_16);
				final String originalJson = SimpleComponent.fromAdventure(before).toAdventureJson(null, useLegacyHexFormat);

				if (originalJson == null || originalJson.isEmpty() || originalJson.length() >= 50_000)
					return;

				String json = originalJson;

				try {
					json = this.onJsonMessage(player, json);

				} catch (final EventHandledException ex) {
					event.setCancelled(true);

					return;
				}

				if (this.editJson() && json != null && !json.equals(originalJson)) {
					final Component newComponent = GsonComponentSerializer.gson().deserialize(json);

					if (modern)
						modernWrapper.setMessage(newComponent);
					else
						legacyWrapper.getMessage().setChatContent(newComponent);

					event.markForReEncode(true);
				}

			} finally {
				this.processedPlayers.remove(name);
			}
		}

		/**
		 * Called when chat message packet is sent to the player.
		 *
		 * If you edit the json, we do NOT set it back unless you also override
		 * {@link #editJson()} to return true.
		 *
		 * To cancel the packet, throw {@link EventHandledException}.
		 *
		 * @param player
		 * @param json
		 * @return
		 */
		protected String onJsonMessage(final Player player, final String json) {
			return json;
		}

		/**
		 * For performance purposes, the json message is not written back by default.
		 * Return true to enable that behavior.
		 *
		 * @return
		 */
		protected boolean editJson() {
			return false;
		}
	}
}
