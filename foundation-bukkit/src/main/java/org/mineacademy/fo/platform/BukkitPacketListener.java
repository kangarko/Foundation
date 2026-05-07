package org.mineacademy.fo.platform;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Listens to and intercepts packets using Foundation inbuilt features.
 */
@AutoRegister(hideIncompatibilityWarnings = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BukkitPacketListener extends PacketListener {

	/**
	 * The singleton of this class to auto register it.
	 */
	@Getter(value = AccessLevel.MODULE)
	private static final PacketListener instance = new BukkitPacketListener();

	@Override
	public void onRegister() {

		// "Fix" a Folia bug preventing Conversation API from working properly
		if (Remain.isFolia())
			this.addReceivingListener(PacketType.Play.Client.CHAT_MESSAGE, event -> {
				final Player player = event.getPlayer();

				if (player.isConversing()) {
					final WrapperPlayClientChatMessage wrapper = new WrapperPlayClientChatMessage(event);
					final String message = wrapper.getMessage();

					// Conversation input must run sync; packets fire on Netty threads
					Platform.runTask(() -> player.acceptConversationInput(message));

					event.setCancelled(true);
				}
			});

		// Support editing signs on legacy Minecraft versions
		if (!Remain.hasPlayerOpenSignMethod())
			this.addReceivingListener(PacketType.Play.Client.UPDATE_SIGN, event -> {
				final Player player = event.getPlayer();
				final MetadataValue rawMetadata = CompMetadata.getTempMetadata(player, CompMetadata.TAG_OPENED_SIGN);

				if (rawMetadata == null)
					return;

				final WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(event);
				final Vector3i position = wrapper.getBlockPosition();
				final Location metadataLocation = (Location) rawMetadata.value();
				final Location location = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ());

				if (location.equals(metadataLocation)) {
					CompMetadata.removeTempMetadata(player, CompMetadata.TAG_OPENED_SIGN);

					final Block block = player.getWorld().getBlockAt(location);
					final BlockState state = block.getState();

					if (state instanceof Sign) {
						final Sign sign = (Sign) state;
						final String[] lines = wrapper.getTextLines();

						for (int line = 0; line < lines.length; line++)
							sign.setLine(line, lines[line]);

						sign.update(true);
					}
				}
			});
	}
}
