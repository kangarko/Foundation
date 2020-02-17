package org.mineacademy.fo.visualize_old;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

/**
 *  @deprecated use classes in the new "visual" package
 */
@Deprecated
public abstract class VisualizedBlock {

	private final static boolean CAN_VISUALIZE = MinecraftVersion.atLeast(V.v1_9);

	/**
	 * The block this container originally holds
	 */
	private final BlockState state;

	/**
	 * The representation when selected
	 */
	private final CompMaterial mask;

	// Internal

	/** The visualized entity for this container */
	private FallingBlock fallingBlock = null;

	/** Is glowing? */
	private boolean glow = false;

	/** Keeping this block visualized */
	private BukkitTask keepAliveTask = null;

	/** The block under the falling block to prevent it from falling */
	private BlockState underground = null;

	public VisualizedBlock(final Block block, final CompMaterial mask) {
		this.state = block.getState();
		this.mask = mask;
	}

	public final void visualize(final VisualizeMode mode) {
		removeGlowIf();

		switch (mode) {
			case MASK:
				setMask();
				break;

			case GLOW:
				glow();
				break;

			default:
				throw new FoException("Unhandled visual mode: " + mode);
		}
	}

	public final void hide() {
		removeGlowIf();

		state.update(true, false);
	}

	public abstract String getBlockName(Block block);

	// ---------------------------------------------------------------------------
	// Mask
	// ---------------------------------------------------------------------------

	private final void setMask() {
		Remain.setTypeAndData(state.getBlock(), mask.getMaterial(), (byte) mask.getData(), false);
	}

	// ---------------------------------------------------------------------------
	// Glow
	// ---------------------------------------------------------------------------

	private final void glow() {
		Valid.checkBoolean(!glow, "Block " + state.getBlock().getType() + " already glows!");

		changeRealBlock();

		if (CAN_VISUALIZE) {
			spawnFallingBlock();
			startKeepingAlive();
		}

		glow = true;
	}

	private final void changeRealBlock() {
		state.getBlock().setType(CAN_VISUALIZE ? Remain.getMaterial("BARRIER", CompMaterial.GLASS).getMaterial() : Material.BEACON);
	}

	private final void spawnFallingBlock() {
		Valid.checkBoolean(fallingBlock == null, "Report / Already visualized!");

		final Location spawnLoc = state.getLocation().clone().add(0.5, 0, 0.5);

		final Block under = spawnLoc.getBlock().getRelative(BlockFace.DOWN);
		if (under.getType() == Material.AIR) {
			underground = under.getState();
			under.setType(Material.BARRIER);
		}

		final FallingBlock falling = spawnLoc.getWorld().spawnFallingBlock(spawnLoc, mask.getMaterial(), (byte) mask.getData());

		paintFallingBlock(falling);
		this.fallingBlock = falling;
	}

	private final void paintFallingBlock(final FallingBlock falling) {
		try {
			Remain.setCustomName(falling, Common.colorize("&8[" + getBlockName(falling.getLocation().getBlock()) + "&r&8]"));
		} catch (final Exception ex) {
			ex.printStackTrace();
		}

		falling.setVelocity(new Vector(0, 0, 0));
		falling.setDropItem(false);

		CompProperty.GLOWING.apply(falling, true);
		CompProperty.GRAVITY.apply(falling, false);
	}

	private final void startKeepingAlive() {
		Valid.checkBoolean(keepAliveTask == null, "Report / Task already running for " + this);

		keepAliveTask = new BukkitRunnable() {

			@Override
			public void run() {
				if (!glow) {
					cancel();
					return;
				}

				Valid.checkNotNull(fallingBlock, "Report / Falling block is null!");
				fallingBlock.setTicksLived(1);
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, 580 /* Falling sand holds for 600 ticks, but let's be safe */);
	}

	// ---------------------------------------------------------------------------
	// Hide
	// ---------------------------------------------------------------------------

	private final void removeGlowIf() {
		if (glow) {

			if (CAN_VISUALIZE) {
				removeFallingBlock();
				stopKeepingAlive();
			}

			glow = false;
		}
	}

	private final void removeFallingBlock() {
		Valid.checkNotNull(fallingBlock, "Report / Visualized, but visualized block is null!");

		if (underground != null) {
			underground.update(true);
			underground = null;
		}

		this.fallingBlock.remove();
		this.fallingBlock = null;
	}

	private final void stopKeepingAlive() {
		Valid.checkNotNull(keepAliveTask, "Report / Task not running for " + this);

		keepAliveTask.cancel();
		keepAliveTask = null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + state.getBlock().getType() + "}";
	}
}