package org.mineacademy.fo.platform;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Listens to and intercepts packets using Foundation inbuilt features
 */
@AutoRegister(hideIncompatibilityWarnings = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BukkitPacketListener extends PacketListener {

	/**
	 * The singleton of this class to auto register it.
	 */
	@Getter(value = AccessLevel.MODULE)
	private static final PacketListener instance = new BukkitPacketListener();

	/**
	 * Registers our packet listener for some of the more advanced features of Foundation
	 */
	@Override
	public void onRegister() {

		// "Fix" a Folia bug preventing Conversation API from working properly
		if (Remain.isFolia())
			this.addReceivingListener(PacketType.Play.Client.CHAT, event -> {
				final String message = event.getPacket().getStrings().read(0);
				final Player player = event.getPlayer();

				if (player.isConversing()) {

					// Ensure to run sync since packets are async
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

				final Location metadataLocation = (Location) rawMetadata.value();

				final BlockPosition position = event.getPacket().getBlockPositionModifier().read(0);
				final WrappedChatComponent[] lines = event.getPacket().getChatComponentArrays().read(0);

				final Location location = position.toLocation(player.getWorld());

				if (location.equals(metadataLocation)) {
					CompMetadata.removeTempMetadata(player, CompMetadata.TAG_OPENED_SIGN);

					final Block block = player.getWorld().getBlockAt(location);
					final BlockState state = block.getState();

					if (state instanceof Sign) {
						final Sign sign = (Sign) state;

						for (int line = 0; line < lines.length; line++) {
							final WrappedChatComponent component = lines[line];
							final String signText = SimpleComponent.fromAdventureJson(component.getJson().replace("§f", ""), MinecraftVersion.olderThan(V.v1_16)).toLegacySection(null);

							sign.setLine(line, signText);
						}

						sign.update(true);
					}
				}
			});
	}
}