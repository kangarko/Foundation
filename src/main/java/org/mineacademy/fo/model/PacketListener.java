package org.mineacademy.fo.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.RegexTimeoutException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * Represents packet handling using ProtocolLib
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PacketListener {

	/**
	 * Stores 1.19 system chat packet constructor and Adventure stuff for maximum performance
	 */
	private static Class<?> textComponentClass;

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
			WrappedGameProfile profile;

			try {
				profile = new WrappedGameProfile(UUID.randomUUID(), Common.colorize(hoverText));

			} catch (final Throwable t) {
				profile = new WrappedGameProfile(String.valueOf(count++), Common.colorize(hoverText));
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
		 * The currently filtered json message
		 */
		private String jsonMessage;

		/**
		 * Support md_5 BaseComponent API
		 */
		private boolean isBaseComponent = false;

		/**
		 * Support Adventure PaperSpigot library
		 */
		private boolean adventure = false;

		/**
		 * Support 1.19+ system chat
		 */
		private final boolean systemChat = MinecraftVersion.atLeast(V.v1_19);

		/**
		 * Create new chat listener
		 */
		public SimpleChatAdapter() {
			super(ListenerPriority.HIGHEST, MinecraftVersion.atLeast(V.v1_19) ? PacketType.Play.Server.SYSTEM_CHAT : PacketType.Play.Server.CHAT);
		}

		@Override
		public void onPacketSending(final PacketEvent event) {
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

				final String legacyText = this.compileChatMessage(event);
				String parsedText = legacyText;

				try {
					parsedText = this.onMessage(parsedText);

				} catch (final RegexTimeoutException ex) {
					// Such errors mean the parsed message took too long to process.
					// Only show such errors every 30 minutes to prevent console spam
					Common.logTimed(1800, "&cWarning: &fPacket message '" + Common.limit(this.jsonMessage, 500)
							+ "' (possibly longer) took too long time to edit received message and was ignored."
							+ " This message only shows once per 30 minutes when that happens. For most cases, this can be ignored.");

					return;

				} catch (final EventHandledException ex) {
					event.setCancelled(true);

					return;
				}

				if (this.jsonMessage != null && !this.jsonMessage.isEmpty())
					this.onJsonMessage(this.jsonMessage);

				if (!legacyText.equals(parsedText))
					this.writeEditedMessage(parsedText, event);

			} finally {
				this.processedPlayers.remove(this.player.getName());
			}
		}

		/*
		 * Read the chat message in unpacked format from the event
		 */
		private String compileChatMessage(PacketEvent event) {

			// Reset
			this.jsonMessage = null;

			// Components
			if (MinecraftVersion.atLeast(V.v1_7)) {

				// System chat
				if (this.systemChat) {

					try {
						// Minecraft 1.20.4+ uses Component field instead of text
						this.jsonMessage = event.getPacket().getChatComponents().read(0).getJson();

					} catch (final Exception ex) {
						this.jsonMessage = event.getPacket().getStrings().read(0);
					}

					if (this.jsonMessage != null)
						return Remain.toLegacyText(this.jsonMessage, false);

					try {
						final StructureModifier<Object> adventureModifier = event.getPacket().getModifier().withType(AdventureComponentConverter.getComponentClass());

						if (!adventureModifier.getFields().isEmpty()) {
							final Object comp = adventureModifier.read(0);

							final Class<?> serializerClass = ReflectionUtil.lookupClass("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
							final Object gsonInstance = ReflectionUtil.invokeStatic(serializerClass, "gson");

							final Class<?> componentClass = ReflectionUtil.lookupClass("net.kyori.adventure.text.Component");
							final Method gsonMethod = ReflectionUtil.getMethod(gsonInstance.getClass(), "serialize", componentClass);

							final String json = ReflectionUtil.invoke(gsonMethod, gsonInstance, comp);
							this.jsonMessage = WrappedChatComponent.fromJson(json).getJson();
						}

					} catch (final Throwable ignored) {
						ignored.printStackTrace();
						// Ignore if Adventure is unavailable
					}

					final Object adventureContent = ReflectionUtil.getFieldContent(event.getPacket().getHandle(), "adventure$content");

					if (adventureContent != null) {
						final List<String> contents = new ArrayList<>();

						this.mergeChildren(adventureContent, contents);
						final String mergedContents = String.join("", contents);

						return mergedContents;
					}

				} else {
					final StructureModifier<Object> packet = event.getPacket().getModifier();
					final StructureModifier<WrappedChatComponent> chat = event.getPacket().getChatComponents();
					final WrappedChatComponent component = chat.read(0);

					try {
						final ChatType chatType = event.getPacket().getChatTypes().readSafely(0);

						if (chatType == ChatType.GAME_INFO)
							return "";

					} catch (final NoSuchMethodError t) {
						// Silence on legacy MC
					}

					if (component != null)
						this.jsonMessage = component.getJson();

					// Md_5 way of dealing with packets
					else if (packet.size() > 1) {
						Object secondField = packet.readSafely(1);

						// Support "Adventure" library in PaperSpigot
						if (secondField == null) {
							secondField = packet.readSafely(2);

							if (secondField != null)
								this.adventure = true;
						}

						if (secondField instanceof BaseComponent[]) {
							this.jsonMessage = Remain.toJson((BaseComponent[]) secondField);

							this.isBaseComponent = true;
						}
					}
				}
			}

			// No components for this MC version
			else
				this.jsonMessage = event.getPacket().getStrings().read(0);

			if (this.jsonMessage != null && !this.jsonMessage.isEmpty())
				// Only check valid messages, skipping those over 50k since it would cause rules
				// to take too long and overflow. 99% packets are below this size, it may even be
				// that such oversized packets are maliciously sent so we protect the server from freeze
				if (this.jsonMessage.length() < 50_000) {
					final String legacyText;

					// Catch errors from other plugins and silence them
					try {
						legacyText = Remain.toLegacyText(this.jsonMessage, false);

					} catch (final Throwable t) {
						return "";
					}

					return legacyText;
				}

			return "";
		}

		/*
		 * Helper method to get content of all children of the given component
		 */
		private void mergeChildren(Object component, List<String> contents) {
			final Method contentMethod = ReflectionUtil.getMethod(component.getClass(), "content");
			final Method childrenMethod = ReflectionUtil.getMethod(component.getClass(), "children");

			if (textComponentClass == null)
				textComponentClass = ReflectionUtil.lookupClass("net.kyori.adventure.text.TextComponent");

			if (textComponentClass.isAssignableFrom(component.getClass())) {
				contents.add(ReflectionUtil.invoke(contentMethod, component));

				for (final Object child : (List<?>) ReflectionUtil.invoke(childrenMethod, component))
					mergeChildren(child, contents);
			}
		}

		/*
		 * Writes the edited message as JSON format from the event
		 */
		private void writeEditedMessage(String message, PacketEvent event) {
			final PacketContainer packet = event.getPacket();

			this.jsonMessage = Remain.toJson(message);

			if (this.systemChat) {

				// We first need to get rid of Adventure library adding an extra field, so that the string JSON will be used below
				// Thanks to lukalt for help! https://github.com/dmulloy2/ProtocolLib/issues/2330#issuecomment-1517542145
				try {
					final StructureModifier<Object> adventureModifier = packet.getModifier().withType(AdventureComponentConverter.getComponentClass());

					if (!adventureModifier.getFields().isEmpty())
						adventureModifier.write(0, null);

				} catch (final Throwable ignored) {
					// Ignore if Adventure is unavailable
				}

				try {
					packet.getChatComponents().write(0, WrappedChatComponent.fromJson(this.jsonMessage));

				} catch (final FieldAccessException t) {
					packet.getStrings().write(0, this.jsonMessage);
				}

			} else if (this.isBaseComponent)
				packet.getModifier().writeSafely(this.adventure ? 2 : 1, Remain.toComponent(this.jsonMessage));

			else if (MinecraftVersion.atLeast(V.v1_7))
				packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(this.jsonMessage));

			else
				packet.getStrings().writeSafely(0, SerializedMap.of("text", this.jsonMessage.substring(1, this.jsonMessage.length() - 1)).toJson());
		}

		/**
		 * Called automatically when we receive and decipher a chat message packet.
		 * <p>
		 * You can use {@link #getEvent()} and {@link #getPlayer()} here.
		 * The preferred way of cancelling the packet is throwing an {@link EventHandledException}
		 * <p>
		 * If you edit the message we automatically set it.
		 *
		 * @param message
		 * @return
		 */
		protected abstract String onMessage(String message);

		/**
		 * Called automatically when we receive the chat message and decipher it into plain json.
		 * You can use {@link #getEvent()} and {@link #getPlayer()} here.
		 * <p>
		 * If you edit the jsonMessage we do NOT set it back.
		 *
		 * @param jsonMessage
		 */
		protected void onJsonMessage(final String jsonMessage) {
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
