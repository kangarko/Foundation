package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.NonNull;

/**
 * The menu controlling a region.
 */
public class RegionMenu extends Menu {

	private final Button teleportButton;
	private final Button viewButton;
	private final Button removeButton;

	private RegionMenu(@NonNull DiskRegion region) {
		super(SelectRegionMenu.create(), true);

		final String regionName = region.getName();

		this.setTitle("Region " + regionName);
		this.setSize(9 * 4);

		this.teleportButton = Button.makeSimple(ItemCreator.of(CompMaterial.ENDER_PEARL,
				"Teleport",
				"",
				"Click to teleport",
				"to region center."), player -> {
					player.chat("/" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " region tp " + regionName);
				});

		this.viewButton = Button.makeSimple(ItemCreator.of(CompMaterial.GLASS,
				"Visualize",
				"",
				"Click to visualize",
				"region borders."), player -> {
					player.closeInventory();
					player.chat("/" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " region view " + regionName);
				});

		this.removeButton = new ButtonRemove(this, "region", regionName, () -> {

			// Remove the boss from disk
			DiskRegion.removeRegion(region);

			// Show parent menu for convenience
			final Menu nextMenu = SelectRegionMenu.create();

			nextMenu.displayTo(this.getViewer());
			Common.runLater(2, () -> nextMenu.animateTitle("&4Removed Region " + regionName));
		});
	}

	@Override
	public ItemStack getItemAt(int slot) {

		if (slot == 9 * 1 + 2)
			return this.teleportButton.getItem();

		if (slot == 9 * 1 + 4)
			return this.viewButton.getItem();

		if (slot == 9 * 1 + 6)
			return this.removeButton.getItem();

		return NO_ITEM;
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"This the main region menu. Edit region",
				"options here or via /" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " region command."
		};
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	public static void showTo(Player player, DiskRegion region) {
		new RegionMenu(region).displayTo(player);
	}
}
