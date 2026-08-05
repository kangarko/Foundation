package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An owner-editable item template loaded from a config section holding the
 * Material, Name, Lore, Glow and Custom_Model_Data keys, built into items
 * through {@link ItemCreator}. Load it in your settings class via
 * {@code get(path, ItemTemplate.class)}:
 *
 * <pre>
 * Life_Heart:
 *   Material: RED_DYE
 *   Name: "&amp;cLife Heart"
 *   Lore:
 *     - "&amp;7Right-click to gain a life."
 *   Glow: true
 *   Custom_Model_Data: 0
 * </pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemTemplate implements ConfigSerializable {

	/**
	 * The item material.
	 */
	@Getter
	private final CompMaterial material;

	/**
	 * The item name, may contain color codes.
	 */
	@Getter
	private final String name;

	private final List<String> lore;
	private final boolean glow;
	private final int modelData;

	/**
	 * Builds a single item from this template.
	 *
	 * @return
	 */
	public ItemStack make() {
		return this.build(null, 1).make();
	}

	/**
	 * Builds a stack of the given size from this template.
	 *
	 * @param amount
	 * @return
	 */
	public ItemStack make(final int amount) {
		return this.build(null, amount).make();
	}

	/**
	 * Builds a single item carrying the given {@link CompMetadata} tag,
	 * typically used for item identity checks.
	 *
	 * @param tagKey
	 * @param tagValue
	 * @return
	 */
	public ItemStack makeTagged(final String tagKey, final String tagValue) {
		return CompMetadata.setMetadata(this.make(), tagKey, tagValue);
	}

	/**
	 * Builds a stack of the given size carrying the given {@link CompMetadata} tag.
	 *
	 * @param tagKey
	 * @param tagValue
	 * @param amount
	 * @return
	 */
	public ItemStack makeTagged(final String tagKey, final String tagValue, final int amount) {
		return CompMetadata.setMetadata(this.make(amount), tagKey, tagValue);
	}

	/**
	 * Builds a player skull, replacing the {player} placeholder in the
	 * name and lore with the given player name.
	 *
	 * @param playerName
	 * @return
	 */
	public ItemStack makeSkull(final String playerName) {
		return this.build(playerName, 1).skullOwner(playerName).make();
	}

	private ItemCreator build(final String playerName, final int amount) {
		final ItemCreator creator = ItemCreator
				.from(this.material, this.replace(this.name, playerName), this.replaceAll(playerName))
				.glow(this.glow)
				.amount(amount);

		if (this.modelData > 0)
			creator.modelData(this.modelData);

		return creator;
	}

	private List<String> replaceAll(final String playerName) {
		if (playerName == null)
			return this.lore;

		return this.lore.stream().map(line -> this.replace(line, playerName)).collect(Collectors.toList());
	}

	private String replace(final String message, final String playerName) {
		return playerName == null ? message : message.replace("{player}", playerName);
	}

	/**
	 * @see ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.put("Material", this.material.name());
		map.put("Name", this.name);

		if (!this.lore.isEmpty())
			map.put("Lore", this.lore);

		if (this.glow)
			map.put("Glow", true);

		if (this.modelData > 0)
			map.put("Custom_Model_Data", this.modelData);

		return map;
	}

	/**
	 * Loads a template from a config section, throwing a clear error when
	 * the Material or Name key is missing or the material name is unknown.
	 *
	 * @param map
	 * @return
	 */
	public static ItemTemplate deserialize(final SerializedMap map) {
		final String materialName = map.getString("Material");
		ValidCore.checkNotEmpty(materialName, "Missing the 'Material' key for the item template: " + map);

		final CompMaterial material = CompMaterial.fromString(materialName);

		if (material == null)
			throw new FoException("Unknown material '" + materialName + "' for the item template: " + map + ". See https://mineacademy.org/materials for valid names.");

		final String name = map.getString("Name");
		ValidCore.checkNotNull(name, "Missing the 'Name' key for the item template: " + map);

		return new ItemTemplate(material, name, map.getStringList("Lore", new ArrayList<>()), map.getBoolean("Glow", false), map.getInteger("Custom_Model_Data", 0));
	}
}
