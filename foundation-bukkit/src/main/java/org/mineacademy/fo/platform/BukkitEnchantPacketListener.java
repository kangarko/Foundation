package org.mineacademy.fo.platform;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.enchant.SimpleEnchantment;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Listens to and intercepts packets using Foundation inbuilt features
 */
@AutoRegister(hideIncompatibilityWarnings = true, doNotAutoRegister = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BukkitEnchantPacketListener extends PacketListener {

	/**
	 * The singleton of this class to auto register it.
	 */
	@Getter(value = AccessLevel.MODULE)
	private static final PacketListener instance = new BukkitEnchantPacketListener();

	/**
	 * Registers our packet listener for some of the more advanced features of Foundation
	 */
	@Override
	public void onRegister() {

		// MC 1.20.5+ uses data components with ItemEnchantments.addToTooltip() to natively
		// render custom enchant names in the tooltip. Injecting fake lore would cause duplicates.
		if (MinecraftVersion.atLeast(V.v1_21) || (MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() >= 5))
			return;

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

			// for older versions, this is not needed because I believe they use an array
			final StructureModifier<List<ItemStack>> itemListModifier = packet.getItemListModifier();
			for (int i = 0; i < itemListModifier.size(); i++) {
				final List<ItemStack> itemStacks = itemListModifier.read(i);
				if (itemStacks != null) {
					boolean changed = false;
					final int size = itemStacks.size();
					for (int j = 0; j < size; j++) {

						ItemStack item = itemStacks.get(j);
						if (item != null && !CompMaterial.isAir(item.getType()) && !CompItemFlag.HIDE_ENCHANTS.has(item)) {
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

			// Not needed for 1.13+ since they changed it to a list according to someone on the spigot forum
			// Though, why not
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

		if (MinecraftVersion.atLeast(V.v1_9))
			this.addSendingListener(PacketType.Play.Server.OPEN_WINDOW_MERCHANT, event -> {
				final PacketContainer packet = event.getPacket();
				final List<MerchantRecipe> ls = packet.getMerchantRecipeLists().read(0);

				boolean changed = false;

				for (int i = 0; i < ls.size(); i++) {
					final MerchantRecipe recipe = ls.get(i);
					ItemStack item = recipe.getResult();

					if (!CompMaterial.isAir(item.getType()) && !CompItemFlag.HIDE_ENCHANTS.has(item)) {
						item = SimpleEnchantment.addEnchantmentLores(item);

						if (item == null)
							continue;

						final MerchantRecipe newRecipe = new MerchantRecipe(item, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier());
						newRecipe.setIngredients(recipe.getIngredients());

						ls.set(i, newRecipe);

						changed = true;
					}
				}

				if (changed)
					packet.getMerchantRecipeLists().write(0, ls);
			});
	}
}