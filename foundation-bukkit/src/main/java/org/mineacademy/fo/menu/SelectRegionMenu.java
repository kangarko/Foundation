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
import org.mineacademy.fo.platform.SimplePlugin;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompMaterial;

/**
 * The menu where players can select a region or create one.
 */
public class SelectRegionMenu extends MenuPaged<String> {

	/**
	 * The button to create a new region.
	 */
	@Position(start = StartPosition.BOTTOM_LEFT)
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
	private SelectRegionMenu(Menu parent) {
		super(parent, DiskRegion.getRegionNames());

		this.setTitle("Create Or Edit Regions");

		this.createButton = Button.makeSimple(ItemCreator.of(CompMaterial.EMERALD,
				"&aCreate New",
				"",
				"Click to create",
				"a new region."), player -> {
					if (SimplePlugin.getInstance().areToolsEnabled())
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
	protected void onPostDisplay(InventoryDrawer drawer) {
		this.colorMask = 0;
	}

	@Override
	protected ItemStack convertToItemStack(String regionName) {
		return ItemCreator.of(CompMaterial.WHITE_STAINED_GLASS,
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
	protected void onPageClick(Player player, String regionName, ClickType click) {
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
	public static Menu create(Menu parent) {
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