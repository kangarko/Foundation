
package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtil.EnchantmentWrapper;
import org.mineacademy.fo.SerializeUtil.PotionWrapper;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for managing items.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemUtil {

	// Is Minecraft older than 1.13? Storing here for best performance.
	private static final boolean LEGACY_MATERIALS = MinecraftVersion.olderThan(V.v1_13);

	// ----------------------------------------------------------------------------------------------------
	// Enumeration - fancy names
	// ----------------------------------------------------------------------------------------------------

	/**
	 * See {@link #bountifyCapitalized(String)}
	 *
	 * @param type
	 * @return
	 */
	public static String bountifyCapitalized(@NonNull PotionEffectType type) {
		return PotionWrapper.getLocalizedName(type.getName());
	}

	/**
	 * See {@link #bountifyCapitalized(String)}
	 *
	 * @param color
	 * @return
	 */
	public static String bountifyCapitalized(@NonNull CompChatColor color) {
		return bountifyCapitalized(color.getName());
	}

	/**
	 * Removes _ from the enum, lowercases everything and finally capitalizes it
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountifyCapitalized(@NonNull Enum<?> enumeration) {
		return ChatUtil.capitalizeFully(bountify(enumeration.toString().toLowerCase()));
	}

	/**
	 * Removes _ from the name, lowercases everything and finally capitalizes it
	 *
	 * @param name
	 * @return
	 */
	public static String bountifyCapitalized(String name) {
		return ChatUtil.capitalizeFully(bountify(name));
	}

	/**
	 * Lowercases the given enum and replaces _ with spaces
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountify(@NonNull Enum<?> enumeration) {
		return bountify(enumeration.toString());
	}

	/**
	 * Lowercases the given name and replaces _ with spaces
	 *
	 * @param name
	 * @return
	 */
	public static String bountify(@NonNull String name) {
		return name.toLowerCase().replace("_", " ");
	}

	/**
	 * Returns a human readable fancy potion effect type
	 *
	 * @param type
	 * @return
	 */
	public static String bountify(@NonNull PotionEffectType type) {
		return PotionWrapper.getLocalizedName(type.getName()).toLowerCase();
	}

	/**
	 * Returns a fancy enchantment name
	 *
	 * @param enchant
	 * @return
	 */
	public static String bountify(@NonNull Enchantment enchant) {
		return EnchantmentWrapper.toMinecraft(enchant.getName());
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
		final boolean metaMatch = first.hasItemMeta() == second.hasItemMeta();

		if (!idMatch || !metaMatch || !(dataMatch || (dataMatch = first.getType() == Material.BOW)))
			return false;

		// ItemMeta
		{
			final ItemMeta f = first.getItemMeta();
			final ItemMeta s = second.getItemMeta();

			if ((f == null && s != null) || (s == null && f != null))
				return false;

			if (f != null && s != null) {
				final String fName = Common.stripColors(f.getDisplayName());
				final String sName = Common.stripColors(s.getDisplayName());

				if ((fName != null && !fName.equals(sName)) || !listMatch(f.getLore(), s.getLore()))
					return false;
			}
		}

		if (MinecraftVersion.atLeast(V.v1_7)) {
			final NBTItem firstNbt = new NBTItem(first);
			final NBTItem secondNbt = new NBTItem(second);

			return matchNbt(SimplePlugin.getNamed(), firstNbt, secondNbt) && matchNbt(SimplePlugin.getNamed() + "_Item", firstNbt, secondNbt);
		}

		return true;
	}

	private static boolean listMatch(List<String> first, List<String> second) {

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

			if (!Common.stripColors(firstString).equals(Common.stripColors(secondString)))
				return false;
		}

		return true;
	}

	// Compares the NBT string tag of two items
	private static boolean matchNbt(String key, NBTItem firstNbt, NBTItem secondNbt) {
		final boolean firstHas = firstNbt.hasKey(key);
		final boolean secondHas = secondNbt.hasKey(key);

		if (!firstHas && !secondHas)
			return true; // nothing has, essentially same

		else if (firstHas && !secondHas || !firstHas && secondHas)
			return false; // one has but another hasn't, cannot be same

		return firstNbt.getString(key).equals(secondNbt.getString(key));
	}
}