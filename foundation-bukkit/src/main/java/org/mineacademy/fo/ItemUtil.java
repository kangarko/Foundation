package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBT;
import org.mineacademy.fo.remain.nbt.ReadableNBT;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemUtil extends ItemUtilCore {

	// Is Minecraft older than 1.13? Storing here for best performance.
	private static final boolean LEGACY_MATERIALS = MinecraftVersion.olderThan(V.v1_13);

	// ----------------------------------------------------------------------------------------------------
	// Enumeration - fancy names
	// ----------------------------------------------------------------------------------------------------

	/**
	 * See {@link #bountify(String)}
	 *
	 * @param type
	 * @return
	 */
	public static String bountify(@NonNull PotionEffectType type) {
		return bountify(type.getName());
	}

	/**
	 * Returns a fancy enchantment name
	 *
	 * @param enchant
	 * @return
	 */
	public static String bountify(@NonNull Enchantment enchant) {
		return bountify(enchant.getName());
	}

	// ----------------------------------------------------------------------------------------------------
	// Comparing items
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Compares two items. Returns true if they are similar.
	 * <p>
	 * Two items are similar if both are not null and if their type, data, name and lore equals.
	 * The damage, quantity, item flags enchants and other properties are ignored.
	 *
	 * @param first
	 * @param second
	 * @return true if items are similar (see above)
	 */
	public static boolean isSimilar(ItemStack first, ItemStack second) {
		if (first == null || second == null)
			return false;

		final boolean firstAir = CompMaterial.isAir(first.getType());
		final boolean secondAir = CompMaterial.isAir(second.getType());

		if ((firstAir && !secondAir) || (!firstAir && secondAir))
			return false;

		if (firstAir && secondAir)
			return true;

		final boolean idMatch = first.getType() == second.getType();

		final boolean isSkull = CompMaterial.isSkull(first.getType()) && CompMaterial.isSkull(second.getType());
		boolean dataMatch = !LEGACY_MATERIALS || isSkull || first.getData().getData() == second.getData().getData();
		final boolean metaMatch = Remain.hasItemMeta() && first.hasItemMeta() == second.hasItemMeta();

		if (!idMatch || !metaMatch || !(dataMatch || (dataMatch = first.getType() == Material.BOW)))
			return false;

		// ItemMeta
		{
			final ItemMeta firstMeta = first.getItemMeta();
			final ItemMeta secondMeta = second.getItemMeta();

			if ((firstMeta == null && secondMeta != null) || (secondMeta == null && firstMeta != null))
				return false;

			if (firstMeta != null && secondMeta != null) {
				final String fName = firstMeta.getDisplayName();
				final String sName = secondMeta.getDisplayName();

				if ((fName != null && !fName.equals(sName)) || !listMatchPlain(firstMeta.getLore(), secondMeta.getLore()))
					return false;
			}
		}

		if (MinecraftVersion.atLeast(V.v1_7)) {
			final ReadableNBT firstNbt = NBT.readNbt(first);
			final ReadableNBT secondNbt = NBT.readNbt(second);

			return matchNbt(SimplePlugin.getNamed(), firstNbt, secondNbt) && matchNbt(SimplePlugin.getNamed() + "_Item", firstNbt, secondNbt);
		}

		return true;
	}

	private static boolean listMatchPlain(List<String> first, List<String> second) {

		if (first == null)
			first = new ArrayList<>();

		if (second == null)
			second = new ArrayList<>();

		if (first.isEmpty() && second.isEmpty())
			return true;

		if (first.size() != second.size())
			return false;

		for (int i = 0; i < first.size(); i++) {
			final String firstString = first.get(i);
			final String secondString = second.get(i);

			if (!firstString.equals(secondString))
				return false;
		}

		return true;
	}

	// Compares the NBT string tag of two items
	private static boolean matchNbt(String key, ReadableNBT firstNbt, ReadableNBT secondNbt) {
		final boolean firstHas = firstNbt.hasTag(key);
		final boolean secondHas = secondNbt.hasTag(key);

		if (!firstHas && !secondHas)
			return true; // nothing has, essentially same

		else if (firstHas && !secondHas || !firstHas && secondHas)
			return false; // one has but another hasn't, cannot be same

		return firstNbt.getString(key).equals(secondNbt.getString(key));
	}
}
