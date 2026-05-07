package org.mineacademy.fo.platform;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.enchant.SimpleEnchantment;
import org.mineacademy.fo.model.PacketListener;
import org.mineacademy.fo.remain.CompItemFlag;
import org.mineacademy.fo.remain.CompMaterial;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.recipe.data.MerchantOffer;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMerchantOffers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Listens to and intercepts inventory packets to inject custom-enchantment lore.
 * Only active on Minecraft versions older than 1.20.5 — newer versions natively
 * render custom enchant names through the data-component tooltip system.
 */
@AutoRegister(hideIncompatibilityWarnings = true, doNotAutoRegister = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BukkitEnchantPacketListener extends PacketListener {

	/**
	 * The singleton of this class to auto register it.
	 */
	@Getter(value = AccessLevel.MODULE)
	private static final PacketListener instance = new BukkitEnchantPacketListener();

	@Override
	public void onRegister() {

		// MC 1.20.5+ uses data components with ItemEnchantments.addToTooltip() to natively
		// render custom enchant names in the tooltip. Injecting fake lore would cause duplicates.
		if (MinecraftVersion.atLeast(V.v1_21) || (MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() >= 5))
			return;

		// Strip our fake lore from items the client sends back via creative-mode slot drag,
		// so the enchantment name doesn't get duplicated when the upgrade level changes.
		this.addReceivingListener(PacketType.Play.Client.CREATIVE_INVENTORY_ACTION, event -> {
			final WrapperPlayClientCreativeInventoryAction wrapper = new WrapperPlayClientCreativeInventoryAction(event);
			final ItemStack bukkit = toBukkit(wrapper.getItemStack());

			if (!shouldProcess(bukkit))
				return;

			final ItemStack mutated = SimpleEnchantment.removeEnchantmentLores(bukkit);

			if (mutated != null) {
				wrapper.setItemStack(SpigotConversionUtil.fromBukkitItemStack(mutated));

				event.markForReEncode(true);
			}
		});

		// Inject lore for single-slot updates
		this.addSendingListener(PacketType.Play.Server.SET_SLOT, event -> {
			final WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
			final ItemStack bukkit = toBukkit(wrapper.getItem());

			if (!shouldProcess(bukkit))
				return;

			final ItemStack mutated = SimpleEnchantment.addEnchantmentLores(bukkit);

			if (mutated != null) {
				wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(mutated));

				event.markForReEncode(true);
			}
		});

		// Inject lore for full-window updates (inventory open, refresh, etc.)
		this.addSendingListener(PacketType.Play.Server.WINDOW_ITEMS, event -> {
			final WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
			final List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper.getItems();
			boolean changed = false;

			for (int i = 0; i < items.size(); i++) {
				final ItemStack bukkit = toBukkit(items.get(i));

				if (!shouldProcess(bukkit))
					continue;

				final ItemStack mutated = SimpleEnchantment.addEnchantmentLores(bukkit);

				if (mutated != null) {
					items.set(i, SpigotConversionUtil.fromBukkitItemStack(mutated));

					changed = true;
				}
			}

			if (changed)
				event.markForReEncode(true);
		});

		// Inject lore on villager merchant trade results
		if (MinecraftVersion.atLeast(V.v1_9))
			this.addSendingListener(PacketType.Play.Server.MERCHANT_OFFERS, event -> {
				final WrapperPlayServerMerchantOffers wrapper = new WrapperPlayServerMerchantOffers(event);
				final List<MerchantOffer> offers = wrapper.getMerchantOffers();
				boolean changed = false;

				for (final MerchantOffer offer : offers) {
					final ItemStack bukkit = toBukkit(offer.getOutputItem());

					if (!shouldProcess(bukkit))
						continue;

					final ItemStack mutated = SimpleEnchantment.addEnchantmentLores(bukkit);

					if (mutated != null) {
						offer.setOutputItem(SpigotConversionUtil.fromBukkitItemStack(mutated));

						changed = true;
					}
				}

				if (changed)
					event.markForReEncode(true);
			});
	}

	/*
	 * Helper: converts a PE ItemStack to a Bukkit ItemStack. Returns null if the
	 * input itself is null or PE's empty-stack sentinel.
	 */
	private static ItemStack toBukkit(final com.github.retrooper.packetevents.protocol.item.ItemStack peItem) {
		if (peItem == null || peItem == com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY)
			return null;

		return SpigotConversionUtil.toBukkitItemStack(peItem);
	}

	/*
	 * Whether this item should pass through enchant-lore injection. Skips null,
	 * air, and items that have HIDE_ENCHANTS flag set.
	 */
	private static boolean shouldProcess(final ItemStack item) {
		if (item == null || CompMaterial.isAir(item.getType()))
			return false;

		return !CompItemFlag.HIDE_ENCHANTS.has(item);
	}
}
