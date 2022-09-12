package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEnterBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPortalExitEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerBucketFishEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.mineacademy.fo.Common;

import lombok.Getter;

public abstract class SimpleProtectionListener {

	static List<SimpleProtectionListener> listeners = new ArrayList<>();

	public SimpleProtectionListener() {
		listeners.add(this);

		System.out.println("@registering " + this + ", total: " + listeners.size());
	}

	public abstract void onBlockBreak(BlockBreakEvent event);

	public abstract void onBlockPlace(BlockPlaceEvent event);

	public abstract void onBlockBurn(BlockBurnEvent event);

	public abstract void onBlockDispense(BlockBurnEvent event);

	public abstract void onBlockExplode(BlockExplodeEvent event);

	/**
	 * @deprecated internal call
	 */
	@Deprecated
	public static final void clearListeners() {
		listeners.clear();

		System.out.println("@listeners cleared");
	}

	/**
	 * @deprecated internal call
	 *
	 * @return
	 */
	@Deprecated
	public static final int countListeners() {
		return listeners.size();
	}

	/**
	 * @deprecated internal use only
	 */
	@Deprecated
	public final static class ProtectionEvents implements Listener {

		@Getter
		private static final Listener instance = new ProtectionEvents();

		private ProtectionEvents() {
			registerEvent("org.bukkit.event.entity.EntityEnterBlockEvent", ModernEntityEnterBlock::new);
			registerEvent("org.bukkit.event.entity.EntityPickupItemEvent", ModernEntityPickup::new, LegacyPlayerPickup::new);
			registerEvent("org.bukkit.event.player.PlayerBucketEntityEvent", ModernBucket::new);
			registerEvent("org.bukkit.event.player.PlayerBucketFishEvent", LegacyFish::new);
			registerEvent("org.bukkit.event.player.PlayerTakeLecternBookEvent", ModernTakeLecternBook::new);
			registerEvent("org.bukkit.event.raid.RaidTriggerEvent", ModernRaidTrigger::new);
			registerEvent("org.bukkit.event.block.BlockReceiveGameEvent", ModernBlockReceiveGame::new);
			registerEvent("org.bukkit.event.block.BlockExplodeEvent", ModernBlockExplode::new);
			registerEvent("org.bukkit.event.player.PlayerInteractAtEntityEvent", ModernInteractAtEntity::new);
			registerEvent("org.bukkit.event.block.CauldronLevelChangeEvent", ModernCauldronLevelChange::new);
		}

		private void registerEvent(String classPath, Supplier<Listener> listener) {
			this.registerEvent(classPath, listener, null);
		}

		/*
		 * Register a class containing events not available in older Minecraft versions
		 */
		private void registerEvent(String classPath, Supplier<Listener> listener, @Nullable Supplier<Listener> fallbackListener) {
			try {
				Class.forName(classPath);

				Common.registerEvents(listener.get());

			} catch (final ClassNotFoundException ex) {
				if (fallbackListener != null)
					Common.registerEvents(fallbackListener.get());
			}
		}

		private void forEach(Consumer<SimpleProtectionListener> listener) {
			for (final SimpleProtectionListener otherListener : SimpleProtectionListener.listeners)
				listener.accept(otherListener);
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockBreakEvent event) {
			forEach(listener -> listener.onBlockBreak(event));
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockBurnEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockDispenseEvent event) {
		}

		class ModernBlockExplode implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(BlockExplodeEvent event) {
				for (final Block block : event.blockList())
					;
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockFormEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockFromToEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockIgniteEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockMultiPlaceEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockPistonExtendEvent event) {
			for (final Block block : event.getBlocks())
				;
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockPistonRetractEvent event) {
			try {
				for (final Block block : event.getBlocks())
					;

			} catch (final NoSuchMethodError ex) {
				// Old MC lack the event.getBlocks method
			}
		}

		@EventHandler
		public void onPistonExtend(final BlockPistonExtendEvent event) {
			preventPistonMovement(event, event.getBlocks());
		}

		@EventHandler
		public void onPistonRetract(final BlockPistonRetractEvent event) {
			try {
				preventPistonMovement(event, event.getBlocks());

			} catch (final NoSuchMethodError ex) {
				// Old MC lack the event.getBlocks method
			}
		}

		/*
		 * Calculate if the blocks being pushed/pulled by piston cross an arena border
		 * and cancel the event if they do
		 */
		private void preventPistonMovement(final BlockPistonEvent event, List<Block> blocks) {
			final BlockFace direction = event.getDirection();

			// Clone the list otherwise it wont work
			blocks = new ArrayList<>(blocks);

			// Calculate blocks ONE step ahed in the push/pull direction
			for (int i = 0; i < blocks.size(); i++) {
				final Block block = blocks.get(i);

				blocks.set(i, block.getRelative(direction));
			}

			for (final Block extendingBlock : blocks)
				this.checkLocation(event, event.getBlock(), extendingBlock);
		}

		private void checkLocation(BlockPistonEvent event, Block mainBlock, Block extendingBlock) {
			// TODO
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockPlaceEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(BlockSpreadEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(CreatureSpawnEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityBlockFormEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityBreakDoorEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityChangeBlockEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityCombustByEntityEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityDamageByEntityEvent event) {
		}

		class ModernEntityEnterBlock implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(EntityEnterBlockEvent event) {
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityExplodeEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityInteractEvent event) {
		}

		class ModernEntityPickup implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(EntityPickupItemEvent event) {
			}
		}

		class LegacyPlayerPickup implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(PlayerPickupItemEvent event) {
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityPortalEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityPortalExitEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityShootBowEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(EntityTargetEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(ExpBottleEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(HangingBreakByEntityEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(HangingBreakEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(HangingPlaceEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(InventoryPickupItemEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PlayerBedEnterEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PlayerBucketEmptyEvent event) {
		}

		class ModernBucket implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(PlayerBucketEntityEvent event) {
			}
		}

		class LegacyFish implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(PlayerBucketFishEvent event) {
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PlayerBucketFillEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PlayerDropItemEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PlayerFishEvent event) {
		}

		class ModernInteractAtEntity implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(PlayerInteractAtEntityEvent event) {
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PlayerInteractEntityEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PlayerInteractEvent event) {
		}

		class ModernTakeLecternBook implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(PlayerTakeLecternBookEvent event) {
			}
		}

		// TODO You may want to expand on this, this will simply prevent all teleportation
		// to a claim by a player who does not own the claim.
		//@EventHandler(priority = EventPriority.LOWEST)
		//public void event(PlayerTeleportEvent event) {
		//	this.disallowIfNotOwner(event.getTo(), event.getPlayer(), event);
		//}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(PotionSplashEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(ProjectileHitEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(ProjectileLaunchEvent event) {
		}

		class ModernRaidTrigger implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(RaidTriggerEvent event) {
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(SignChangeEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(StructureGrowEvent event) {
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void event(VehicleDamageEvent event) {
		}

		class ModernBlockReceiveGame implements Listener {

			@EventHandler(priority = EventPriority.LOWEST)
			public void event(BlockReceiveGameEvent event) {
			}
		}

		class ModernCauldronLevelChange implements Listener {

			@EventHandler
			public void event(CauldronLevelChangeEvent event) {
			}
		}
	}
}
