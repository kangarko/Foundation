package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.conversation.CreateRegionPrompt;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.StartPosition;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * The menu where players can select a region or create one.
 */
public class SelectRegionMenu extends MenuPaged<String> {

	/**
	 * The button to create a new region.
	 */
	@Position(start = StartPosition.BOTTOM_RIGHT)
	private final Button createButton;

	/**
	 * The color mask for the region items.
	 */
	private int colorMask = CompColor.values().length;

	/**
	 * Create a new region menu to select a region.
	 *
	 * @param parent
	 */
	private SelectRegionMenu(final Menu parent) {
		super(parent, DiskRegion.getRegionNames());

		this.setTitle("Create Or Edit Regions");

		this.createButton = Button.makeSimple(ItemCreator.from(CompMaterial.EMERALD,
				"&aCreate New",
				"",
				"Click to create",
				"a new region."), player -> {
					if (SimpleSettings.REGISTER_TOOLS)
						CreateRegionPrompt.showToOrHint(player);
					else {
						player.closeInventory();

						Messenger.error(player, "Enable Register_Tools in settings.yml before creating regions!");
					}
				});
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuPaged#onPostDisplay(org.mineacademy.fo.menu.model.InventoryDrawer)
	 */
	@Override
	protected void onPostDisplay(final InventoryDrawer drawer) {
		this.colorMask = 0;
	}

	@Override
	protected ItemStack convertToItemStack(final String regionName) {
		return ItemCreator.from(CompMaterial.WHITE_STAINED_GLASS,
				"Region " + regionName,
				"",
				"Click to open the region",
				"menu and customize it.")
				.color(CompColor.values()[this.colorMask++ % CompColor.values().length])
				.make();
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Select a region to open its",
				"menu and customize it.",
		};
	}

	@Override
	public Menu newInstance() {
		return new SelectRegionMenu(this.getParent());
	}

	@Override
	protected void onPageClick(final Player player, final String regionName, final ClickType click) {
		RegionMenu.showTo(player, DiskRegion.findRegion(regionName));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new select region menu.
	 *
	 * @param parent
	 * @return
	 */
	public static Menu create(final Menu parent) {
		return new SelectRegionMenu(parent);
	}

	/**
	 * Create a new select region menu.
	 *
	 * @return
	 */
	public static Menu create() {
		return new SelectRegionMenu(null);
	}
}