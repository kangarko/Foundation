package org.mineacademy.fo.model;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.EventHandledException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.event.NPCClickEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

/**
 * This is a trait that will be applied to a NPC using the /trait killboss command. Each NPC gets its own instance of this class.
 * The Trait class has a reference to the attached NPC class through the protected field 'npc' or getNPC().
 * The Trait class also implements Listener so you can add EventHandlers directly to your trait.
 *
 * @author https://wiki.citizensnpcs.co/API, improved by kangarko
 */
public abstract class SimpleTrait extends Trait {

	private int tickAmount = 0;

	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PROTECTED)
	private int tickThreshold;

	protected SimpleTrait(String traitName) {
		super(traitName);
	}

	/**
	 * @see net.citizensnpcs.api.trait.Trait#load(net.citizensnpcs.api.util.DataKey)
	 */
	@Override
	public final void load(DataKey key) {
		final SerializedMap map = SerializedMap.fromJson(key.getString("Data"));

		this.load(map);
	}

	/**
	 * Here you should load up any values you have previously saved (optional).
	 * This does NOT get called when applying the trait for the first time, only loading onto an existing npc at server start.
	 *
	 * This is called BEFORE onSpawn, npc.getEntity() will return null.
	 *
	 * @param map
	 */
	protected abstract void load(SerializedMap map);

	/**
	 * @see net.citizensnpcs.api.trait.Trait#save(net.citizensnpcs.api.util.DataKey)
	 */
	@Override
	public final void save(DataKey key) {
		final SerializedMap map = new SerializedMap();

		this.save(map);

		key.setString("Data", map.toJson());
	}

	/**
	 * Save settings for this NPC (optional). These values will be persisted to the Citizens saves file
	 *
	 * @param map
	 */
	protected abstract void save(SerializedMap map);

	/**
	 * Handle clicking.
	 *
	 * @param event
	 */
	@EventHandler
	public final void onRightClick(NPCRightClickEvent event) {
		this.handleClickEvent(event, ClickType.RIGHT);
	}

	/**
	 * Handle clicking.
	 *
	 * @param event
	 */
	@EventHandler
	public final void onLeftClick(NPCLeftClickEvent event) {
		this.handleClickEvent(event, ClickType.LEFT);
	}

	/*
	 * A helper method for click events
	 */
	private void handleClickEvent(NPCClickEvent event, ClickType clickType) {

		// Only apply this event for this particular NPC
		if (!event.getNPC().equals(this.getNPC()))
			return;

		final Player player = event.getClicker();

		try {
			this.onClick(player, clickType);

		} catch (final EventHandledException ex) {
			if (ex.getMessages() != null) {
				if (Messenger.ENABLED)
					Messenger.error(player, ex.getMessages());
				else
					Common.tell(player, ex.getMessages());
			}

			if (ex.isCancelled())
				event.setCancelled(true);
		}
	}

	/**
	 * Called automatically when this NPC is clicked by the player.
	 * Only RIGHT or LEFT click types are supported.
	 *
	 * @param player
	 * @param clickType
	 *
	 * @throws EventHandledException
	 */
	public void onClick(Player player, ClickType clickType) throws EventHandledException {
	}

	/**
	 * A helper method you can use in onX methods such as in the {@link #onClick(Player)}
	 * method to cancel the event with an optional error message for the player.
	 *
	 * @param playerMessage
	 */
	protected final void cancel(String... playerMessage) {
		throw new EventHandledException(true, playerMessage);
	}

	/**
	 * @see net.citizensnpcs.api.trait.Trait#run()
	 */
	@Override
	public final void run() {

		if (this.tickAmount++ % this.tickThreshold == 0) {
			this.tickAmount = 1;

			this.onTick();
		}
	}

	/**
	 * Called every {@link #tickThreshold}
	 */
	protected abstract void onTick();

	/**
	 * Run code when the NPC is spawned. Note that npc.getEntity() will be null until this method is called.
	 * This is called AFTER Load when the server is started.
	 *
	 * @see net.citizensnpcs.api.trait.Trait#onSpawn()
	 */
	@Override
	public void onSpawn() {
	}

	/**
	 * Run code when the NPC is despawned. This is called before the entity actually despawns so npc.getEntity() is still valid.
	 *
	 * @see net.citizensnpcs.api.trait.Trait#onDespawn()
	 */
	@Override
	public void onDespawn() {
	}

	/**
	 * Run code when the NPC is removed.
	 */
	@Override
	public void onRemove() {
	}
}
