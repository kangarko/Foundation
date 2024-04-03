package org.mineacademy.fo.plugin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.enchant.SimpleEnchantment;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.StructureModifier;

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
	private static volatile PacketListener instance = new FoundationPacketListener();

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
	}
}