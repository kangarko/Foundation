package org.mineacademy.fo.plugin;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.enchant.SimpleEnchantment;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Listens to and intercepts packets using Foundation inbuilt features
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FoundationPacketListener extends PacketListener {

	/**
	 * The singleton of this class to auto register it.
	 */
	@Getter(value = AccessLevel.MODULE)
	private static final PacketListener instance = new FoundationPacketListener();

	/**
	 * Registers our packet listener for some of the more advanced features of Foundation
	 */
	@Override
	public void onRegister() {

		// Auto placement of our lore when items are custom enchanted
		this.addSendingListener(PacketType.Play.Server.SET_SLOT, event -> {
			final StructureModifier<ItemStack> itemModifier = event.getPacket().getItemModifier();
			ItemStack item = itemModifier.read(0);

			if (item != null && !CompMaterial.isAir(item.getType())) {
				item = SimpleEnchantment.addEnchantmentLores(item);

				// Write the item
				if (item != null)
					itemModifier.write(0, item);
			}
		});

		// "Fix" a Folia bug preventing Conversation API from working properly
		if (Remain.isFolia())
			this.addReceivingListener(PacketType.Play.Client.CHAT, event -> {
				final String message = event.getPacket().getStrings().read(0);
				final Player player = event.getPlayer();

				if (player.isConversing()) {
					player.acceptConversationInput(message);

					event.setCancelled(true);
				}
			});

		// Support editing signs on legacy Minecraft versions
		if (!Remain.hasPlayerOpenSignMethod())
			this.addReceivingListener(PacketType.Play.Client.UPDATE_SIGN, event -> {
				final Player player = event.getPlayer();
				final MetadataValue rawMetadata = CompMetadata.getTempMetadata(player, FoConstants.NBT.METADATA_OPENED_SIGN);

				if (rawMetadata == null)
					return;

				final Location metadataLocation = (Location) rawMetadata.value();

				final BlockPosition position = event.getPacket().getBlockPositionModifier().read(0);
				final WrappedChatComponent[] lines = event.getPacket().getChatComponentArrays().read(0);

				final Location location = position.toLocation(player.getWorld());

				if (location.equals(metadataLocation)) {
					CompMetadata.removeTempMetadata(player, FoConstants.NBT.METADATA_OPENED_SIGN);

					final Block block = player.getWorld().getBlockAt(location);
					final BlockState state = block.getState();

					if (state instanceof Sign) {
						final Sign sign = (Sign) state;

						for (int line = 0; line < lines.length; line++) {
							final WrappedChatComponent component = lines[line];
							final String signText = Remain.toLegacyText(component.getJson()).replace("§f", "");

							sign.setLine(line, signText);
						}

						sign.update(true);
					}
				}
			});
	}
}