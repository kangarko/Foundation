package org.mineacademy.fo.plugin;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.metadata.MetadataValue;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.enchant.SimpleEnchantment;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
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

		// To ensure, the client isn't trying to create an item with our fake enchantment lore
		// Because that would cause duplicate entries if the enchantment is upgraded/remove
		// Ex:
		//   BlackNova II (fake lore)
		//   BlackNova I  (actual lore of the item)
		this.addReceivingListener(PacketType.Play.Client.SET_CREATIVE_SLOT, event -> {
			final PacketContainer packet = event.getPacket();
			final ItemStack item = packet.getItemModifier().readSafely(0);

			if (item != null && !CompMaterial.isAir(item.getType()) && !CompItemFlag.HIDE_ENCHANTS.has(item)) {
				final ItemStack newItem = SimpleEnchantment.removeEnchantmentLores(item);

				if (newItem != null)
					packet.getItemModifier().write(0, newItem);
			}
		});

		// Auto placement of our lore when items are custom enchanted
		this.addSendingListener(PacketType.Play.Server.SET_SLOT, event -> {
			final StructureModifier<ItemStack> itemModifier = event.getPacket().getItemModifier();
			ItemStack item = itemModifier.read(0);

			if (item != null && !CompMaterial.isAir(item.getType()) && !CompItemFlag.HIDE_ENCHANTS.has(item)) {
				item = SimpleEnchantment.addEnchantmentLores(item);

				// Write the item
				if (item != null)
					itemModifier.write(0, item);
			}
		});

		this.addSendingListener(PacketType.Play.Server.WINDOW_ITEMS, event -> {
			final PacketContainer packet = event.getPacket();

			// for older versions, this is not needed because they use an array
			final StructureModifier<List<ItemStack>> itemListModifier = packet.getItemListModifier();
			for (int i = 0; i < itemListModifier.size(); i++) {
				final List<ItemStack> itemStacks = itemListModifier.read(i);
				if (itemStacks != null) {
					boolean changed = false;
					final int size = itemStacks.size();
					for (int j = 0; j < size; j++) {

						ItemStack item = itemStacks.get(j);
						if (item != null && !CompMaterial.isAir(item) && !CompItemFlag.HIDE_ENCHANTS.has(item)) {
							item = SimpleEnchantment.addEnchantmentLores(item);
							if (item == null)
								continue;

							itemStacks.set(j, item);
							changed = true;
						}
					}
					if (changed)
						itemListModifier.write(i, itemStacks);
				}
			}

			// Not needed for 1.13+ since they changed it to a list
			final StructureModifier<ItemStack[]> itemArrayModifier = packet.getItemArrayModifier();
			for (int i = 0; i < itemArrayModifier.size(); i++) {
				final ItemStack[] itemStacks = itemArrayModifier.read(i);
				if (itemStacks != null) {
					boolean changed = false;

					for (int j = 0; j < itemStacks.length; j++) {
						ItemStack item = itemStacks[j];
						if (item != null && !CompMaterial.isAir(item.getType()) && !CompItemFlag.HIDE_ENCHANTS.has(item)) {
							item = SimpleEnchantment.addEnchantmentLores(item);
							if (item == null)
								continue;

							itemStacks[j] = item;
							changed = true;
						}
					}
					if (changed)
						itemArrayModifier.write(i, itemStacks);
				}
			}
		});

		if (MinecraftVersion.newerThan(V.v1_8))
			this.addSendingListener(PacketType.Play.Server.OPEN_WINDOW_MERCHANT, event -> {
				final PacketContainer packet = event.getPacket();
				final List<MerchantRecipe> merchantRecipes = packet.getMerchantRecipeLists().read(0);
				boolean foEnchanted = false;

				for (int recipeIndex = 0; recipeIndex < merchantRecipes.size(); recipeIndex++) {
					final MerchantRecipe recipe = merchantRecipes.get(recipeIndex);
					ItemStack item = recipe.getResult();

					if (!CompMaterial.isAir(item.getType()) && !CompItemFlag.HIDE_ENCHANTS.has(item)) {
						item = SimpleEnchantment.addEnchantmentLores(item);

						if (item == null) {
							continue;
						}

						final MerchantRecipe newRecipe = new MerchantRecipe(item, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier());

						newRecipe.setIngredients(recipe.getIngredients());
						merchantRecipes.set(recipeIndex, newRecipe);

						foEnchanted = true;
					}
				}
				if (foEnchanted)
					packet.getMerchantRecipeLists().write(0, merchantRecipes);
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