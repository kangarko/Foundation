package org.mineacademy.fo.remain.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.ReflectionException;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.remain.Remain;

/**
 * Reflection class for handling chat-related methods
 *
 * @deprecated internal use only, please use {@link Remain}
 * to call methods from this class for best performance
 */
@Deprecated
public class ChatInternals {

	private static Object enumTitle;
	private static Object enumSubtitle;
	private static Object enumReset;

	private static Constructor<?> tabConstructor;

	private static Constructor<?> titleTimesConstructor;
	private static Constructor<?> titleConstructor;
	private static Constructor<?> subtitleConstructor;
	private static Constructor<?> resetTitleConstructor;

	private static Method componentSerializer;
	private static Constructor<?> chatMessageConstructor;

	// Prevent new instance, always call static methods
	public ChatInternals() {
	}

	static {

		// New MC versions have native API's
		if (MinecraftVersion.newerThan(V.v1_6) && MinecraftVersion.olderThan(V.v1_12))
			try {

				final Class<?> chatBaseComponent = ReflectionUtil.getNMSClass("IChatBaseComponent", "N/A");

				Class<?> serializer = null;
				if (MinecraftVersion.newerThan(V.v1_7))
					serializer = chatBaseComponent.getDeclaredClasses()[0];
				else
					serializer = ReflectionUtil.getNMSClass("ChatSerializer", "N/A");

				componentSerializer = serializer.getMethod("a", String.class);

				final Class<?> chatPacket = ReflectionUtil.getNMSClass("PacketPlayOutChat", "N/A");

				if (MinecraftVersion.newerThan(V.v1_11))
					chatMessageConstructor = chatPacket.getConstructor(chatBaseComponent, ReflectionUtil.getNMSClass("ChatMessageType", "N/A"));
				else
					chatMessageConstructor = MinecraftVersion.newerThan(V.v1_7) ? chatPacket.getConstructor(chatBaseComponent, byte.class) : chatPacket.getConstructor(chatBaseComponent);

				if (MinecraftVersion.newerThan(V.v1_7)) {
					final Class<?> titlePacket = ReflectionUtil.getNMSClass("PacketPlayOutTitle", "N/A");
					final Class<?> enumAction = titlePacket.getDeclaredClasses()[0];

					enumTitle = enumAction.getField("TITLE").get(null);
					enumSubtitle = enumAction.getField("SUBTITLE").get(null);
					enumReset = enumAction.getField("RESET").get(null);

					tabConstructor = ReflectionUtil.getNMSClass("PacketPlayOutPlayerListHeaderFooter", "N/A").getConstructor(chatBaseComponent);

					titleTimesConstructor = titlePacket.getConstructor(int.class, int.class, int.class);
					titleConstructor = titlePacket.getConstructor(enumAction, chatBaseComponent);
					subtitleConstructor = titlePacket.getConstructor(enumAction, chatBaseComponent);
					resetTitleConstructor = titlePacket.getConstructor(enumAction, chatBaseComponent);
				}

			} catch (final Exception ex) {
				if (MinecraftVersion.olderThan(V.v1_8))
					Common.log("Error initiating Chat/Title/ActionBAR API. Assuming Thermos or modded. Some features will not work.");

				else {
					ex.printStackTrace();

					throw new ReflectionException(ex, "Error initiating Chat/Title/ActionBAR API (incompatible Craftbukkit? - " + Bukkit.getVersion() + " / " + Bukkit.getBukkitVersion() + " / " + MinecraftVersion.getServerVersion() + ")");
				}
			}
	}

	/**
	 * Send a title to player
	 *
	 * @param player
	 * @param fadeIn
	 * @param stay
	 * @param fadeOut
	 * @param title
	 * @param subtitle
	 */
	public static void sendTitleLegacy(final Player player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		Valid.checkBoolean(MinecraftVersion.olderThan(V.v1_12), "This method is unsupported on MC 1.13 and later");

		try {
			if (titleConstructor == null)
				return;

			resetTitleLegacy(player);

			if (titleTimesConstructor != null) {
				final Object packet = titleTimesConstructor.newInstance(fadeIn, stay, fadeOut);

				Remain.sendPacket(player, packet);
			}

			if (title != null) {
				final Object chatTitle = serializeText(title);
				final Object packet = titleConstructor.newInstance(enumTitle, chatTitle);

				Remain.sendPacket(player, packet);
			}

			if (subtitle != null) {
				final Object chatSubtitle = serializeText(subtitle);
				final Object packet = subtitleConstructor.newInstance(enumSubtitle, chatSubtitle);

				Remain.sendPacket(player, packet);
			}
		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Error sending title to: " + player.getName() + ", title: " + title + ", subtitle: " + subtitle);
		}
	}

	/**
	 * Reset title for player
	 *
	 * @param player
	 */
	public static void resetTitleLegacy(final Player player) {
		Valid.checkBoolean(MinecraftVersion.olderThan(V.v1_12), "This method is unsupported on MC 1.13 and later");

		try {
			if (resetTitleConstructor == null)
				return;

			final Object packet = resetTitleConstructor.newInstance(enumReset, null);

			Remain.sendPacket(player, packet);
		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Error resetting title to: " + player.getName());
		}
	}

	/**
	 * Send tablist to player
	 *
	 * @param player
	 * @param headerRaw
	 * @param footerRaw
	 */
	public static void sendTablistLegacy(final Player player, final String headerRaw, final String footerRaw) {
		Valid.checkBoolean(MinecraftVersion.olderThan(V.v1_12), "This method is unsupported on MC 1.13 and later");

		try {
			if (tabConstructor == null)
				return;

			final Object header = serializeText(headerRaw);
			final Object packet = tabConstructor.newInstance(header);

			if (footerRaw != null) {
				final Object footer = serializeText(footerRaw);

				final Field f = packet.getClass().getDeclaredField("b"); // setFooter
				f.setAccessible(true);
				f.set(packet, footer);
			}

			Remain.sendPacket(player, packet);

		} catch (final ReflectiveOperationException ex) {
			Common.error(ex, "Failed to send tablist to " + player.getName() + ", title: " + headerRaw + " " + footerRaw);
		}
	}

	/**
	 * Send action bar to player
	 *
	 * @param player
	 * @param message
	 */
	public static void sendActionBarLegacy(final Player player, final String message) {
		Valid.checkBoolean(MinecraftVersion.olderThan(V.v1_12), "This method is unsupported on MC 1.13 and later");

		sendChat(player, message, (byte) 2);
	}

	// http://wiki.vg/Protocol#Chat_Message
	private static void sendChat(final Player pl, final String text, final byte type) {
		try {
			final Object message = serializeText(text);
			Valid.checkNotNull(message, "Message cannot be null!");

			final Object packet;

			if (MinecraftVersion.atLeast(V.v1_12)) {
				final Class<?> chatMessageTypeEnum = ReflectionUtil.getNMSClass("ChatMessageType", "net.minecraft.network.chat.ChatMessageType");

				packet = chatMessageConstructor.newInstance(message, chatMessageTypeEnum.getMethod("a", byte.class).invoke(null, type));

			} else
				packet = chatMessageConstructor.newInstance(message, type);

			Remain.sendPacket(pl, packet);

		} catch (final ReflectiveOperationException ex) {
			Common.error(ex, "Failed to send chat packet type " + type + " to " + pl.getName() + ", message: " + text);
		}
	}

	private static Object serializeText(String text) throws ReflectiveOperationException {
		text = removeBracketsAndColorize(text);

		try {
			return componentSerializer.invoke(null, SerializedMap.of("text", text).toJson());

		} catch (final Throwable t) {
			throw new FoException(t, "Failed to serialize text: " + text);
		}
	}

	private static String removeBracketsAndColorize(String text) {
		if (text == null)
			return "";

		if (text.startsWith("\"") && text.endsWith("\"") || text.startsWith("'") && text.endsWith("'"))
			text = text.substring(1, text.length() - 1);

		return Common.colorize(text);
	}

	public static void callStatic() {
		// Test compatibility
	}
}
