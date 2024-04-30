package org.mineacademy.fo.remain.nbt;

import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * NBT class to access vanilla/custom tags on ItemStacks. This class doesn't
 * autosave to the Itemstack, use getItem to get the changed ItemStack
 *
 * @author tr7zw
 *
 */
public class NBTItem extends NBTCompound implements ReadWriteItemNBT {

	private ItemStack bukkitItem;
	private final boolean directApply;
	private final boolean finalizer;
	private ItemStack originalSrcStack = null;
	private Object cachedCompound = null;
	private boolean closed = false;

	/**
	 * Constructor for NBTItems. The ItemStack will be cloned!
	 * 
	 * @param item
	 */
	public NBTItem(ItemStack item) {
		this(item, false);
	}

	/**
	 * @param item
	 * @param directApply
	 * @param readOnly    When turned on, no copy of the source item is created.
	 *                    Modifying the stack in that case is not valid! Also
	 *                    overwrites directApply
	 */
	protected NBTItem(ItemStack item, boolean directApply, boolean readOnly, boolean finalizer) {
		super(null, null, readOnly);
		if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0)
			throw new NullPointerException("ItemStack can't be null/air/amount of 0! This is not a NBTAPI bug!");
		this.finalizer = finalizer;
		if (finalizer) {
			this.bukkitItem = item;
			this.originalSrcStack = item;
			this.directApply = false;
		} else if (readOnly) {
			bukkitItem = item;
			this.directApply = false;
		} else {
			this.directApply = directApply;
			bukkitItem = item.clone();
			if (directApply)
				this.originalSrcStack = item;
		}
	}

	/**
	 * Constructor for NBTItems. The ItemStack will be cloned! If directApply is
	 * true, all changed will be mapped to the original item. Changes to the NBTItem
	 * will overwrite changes done to the original item in that case.
	 * 
	 * @param item
	 * @param directApply
	 */
	@Deprecated
	public NBTItem(ItemStack item, boolean directApply) {
		super(null, null);
		if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0)
			throw new NullPointerException("ItemStack can't be null/air/amount of 0! This is not a NBTAPI bug!");
		this.finalizer = false;
		this.directApply = directApply;
		bukkitItem = item.clone();
		if (directApply)
			this.originalSrcStack = item;
	}

	@Override
	public Object getCompound() {
		if (closed)
			throw new NbtApiException("Tried using closed NBT data!");
		if (isReadOnly() && (cachedCompound != null
				|| ClassWrapper.CRAFT_ITEMSTACK.getClazz().isAssignableFrom(bukkitItem.getClass()))) {
			if (cachedCompound == null)
				cachedCompound = NBTReflectionUtil
						.getItemRootNBTTagCompound(NBTReflectionUtil.getCraftItemHandle(bukkitItem));
			return cachedCompound;
		}
		if (finalizer) {
			if (cachedCompound == null)
				updateCachedCompound();
			return cachedCompound;
		}
		return NBTReflectionUtil.getItemRootNBTTagCompound(ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, bukkitItem));
	}

	private void updateCachedCompound() {
		if (finalizer)
			cachedCompound = NBTReflectionUtil
					.getItemRootNBTTagCompound(ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, bukkitItem));
	}

	protected void finalizeChanges() {
		if (!finalizer || cachedCompound == null)
			return;
		// There was data, but not anymore, delete the tag from the itemstack
		if (NBTReflectionUtil.getKeys(this).isEmpty())
			cachedCompound = null;
		if (ClassWrapper.CRAFT_ITEMSTACK.getClazz().isAssignableFrom(originalSrcStack.getClass())) {
			final Object nmsStack = NBTReflectionUtil.getCraftItemHandle(originalSrcStack);
			NBTReflectionUtil.setItemStackCompound(nmsStack, cachedCompound);
			bukkitItem = originalSrcStack;
		} else {
			final Object stack = ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, bukkitItem);
			NBTReflectionUtil.setItemStackCompound(stack, cachedCompound);
			bukkitItem = (ItemStack) ReflectionMethod.ITEMSTACK_BUKKITMIRROR.run(null, stack);
			originalSrcStack.setItemMeta(bukkitItem.getItemMeta());
		}
	}

	@Override
	protected void setClosed() {
		this.closed = true;
	}

	@Override
	protected boolean isClosed() {
		return closed;
	}

	@Override
	protected void setCompound(Object compound) {
		if (isReadOnly())
			throw new NbtApiException("Tried setting data in read only mode!");
		if (closed)
			throw new NbtApiException("Tried using closed NBT data!");
		if (finalizer) {
			cachedCompound = compound;
			return;
		}
		if (compound != null && ((Set<String>) ReflectionMethod.COMPOUND_GET_KEYS.run(compound)).isEmpty())
			compound = null;
		if (ClassWrapper.CRAFT_ITEMSTACK.getClazz().isAssignableFrom(bukkitItem.getClass())) {
			final Object nmsStack = NBTReflectionUtil.getCraftItemHandle(bukkitItem);
			NBTReflectionUtil.setItemStackCompound(nmsStack, compound);
		} else {
			final Object stack = ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, bukkitItem);
			NBTReflectionUtil.setItemStackCompound(stack, compound);
			bukkitItem = (ItemStack) ReflectionMethod.ITEMSTACK_BUKKITMIRROR.run(null, stack);
		}
	}

	/**
	 * Apply stored NBT tags to the provided ItemStack.
	 * <p>
	 * Note: This will completely override current item's {@link ItemMeta}. If you
	 * still want to keep the original item's NBT tags, see
	 * {@link #mergeNBT(ItemStack)} and {@link #mergeCustomNBT(ItemStack)}.
	 *
	 * @param item ItemStack that should get the new NBT data
	 */
	public void applyNBT(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			throw new NullPointerException("ItemStack can't be null/Air! This is not a NBTAPI bug!");
		final NBTItem nbti = new NBTItem(new ItemStack(item.getType()));
		nbti.mergeCompound(this);
		item.setItemMeta(nbti.getItem().getItemMeta());
	}

	/**
	 * Merge all NBT tags to the provided ItemStack.
	 *
	 * @param item ItemStack that should get the new NBT data
	 */
	public void mergeNBT(ItemStack item) {
		final NBTItem nbti = new NBTItem(item);
		nbti.mergeCompound(this);
		item.setItemMeta(nbti.getItem().getItemMeta());
	}

	/**
	 * Merge only custom (non-vanilla) NBT tags to the provided ItemStack.
	 *
	 * @param item ItemStack that should get the new NBT data
	 */
	@Deprecated
	public void mergeCustomNBT(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			throw new NullPointerException("ItemStack can't be null/Air!");
		if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
			// 1.20.5+ doesn't have any vanilla tags
			NBT.modify(item, nbt -> {
				nbt.mergeCompound(this);
			});
			return;
		}
		final ItemMeta meta = item.getItemMeta();
		NBTReflectionUtil.getUnhandledNBTTags(meta)
				.putAll(NBTReflectionUtil.getUnhandledNBTTags(bukkitItem.getItemMeta()));
		item.setItemMeta(meta);
	}

	/**
	 * True, if the item has any tags now known for this item type.
	 * 
	 * @return true when custom tags are present
	 */
	@Override
	@Deprecated
	public boolean hasCustomNbtData() {
		if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
			// 1.20.5+ doesn't have any vanilla tags
			return hasNBTData();
		finalizeChanges();
		final ItemMeta meta = bukkitItem.getItemMeta();
		return !NBTReflectionUtil.getUnhandledNBTTags(meta).isEmpty();
	}

	/**
	 * Remove all custom (non-vanilla) NBT tags from the NBTItem.
	 */
	@Override
	@Deprecated
	public void clearCustomNBT() {
		finalizeChanges();
		if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
			// 1.20.5+ doesn't have any vanilla tags
			setCompound(null);
			return;
		}
		final ItemMeta meta = bukkitItem.getItemMeta();
		NBTReflectionUtil.getUnhandledNBTTags(meta).clear();
		bukkitItem.setItemMeta(meta);
		updateCachedCompound();
	}

	/**
	 * @return The modified ItemStack
	 */
	public ItemStack getItem() {
		return bukkitItem;
	}

	protected void setItem(ItemStack item) {
		bukkitItem = item;
	}

	/**
	 * Returns true if the item has NBT data. This needs to be checked before
	 * calling methods like remove, otherwise the value might be wrong!
	 * 
	 * @return Does the ItemStack have a NBTCompound.
	 */
	@Override
	public boolean hasNBTData() {
		return getCompound() != null;
	}

	/**
	 * Gives save access to the {@link ItemMeta} of the internal {@link ItemStack}.
	 * Supported operations while inside this scope: - any get/set method of
	 * {@link ItemMeta} - any getter on {@link NBTItem}
	 * 
	 * All changes made to the {@link NBTItem} during this scope will be reverted at
	 * the end.
	 * 
	 * @param handler
	 */
	@Override
	public void modifyMeta(BiConsumer<ReadableNBT, ItemMeta> handler) {
		finalizeChanges();
		final ItemMeta meta = bukkitItem.getItemMeta();
		handler.accept(new NBTContainer(getResolvedObject()).setReadOnly(true), meta);
		bukkitItem.setItemMeta(meta);
		updateCachedCompound();
		if (directApply) {
			if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
				throw new NbtApiException("Direct apply mode meta changes don't work anymore in 1.20.5+. Please switch to the modern NBT.modify sytnax!");
			applyNBT(originalSrcStack);
		}
	}

	/**
	 * Gives save access to the {@link ItemMeta} of the internal {@link ItemStack}.
	 * Supported operations while inside this scope: - any get/set method of
	 * {@link ItemMeta} - any getter on {@link NBTItem}
	 * 
	 * All changes made to the {@link NBTItem} during this scope will be reverted at
	 * the end.
	 * 
	 * @param handler
	 */
	@Override
	public <T extends ItemMeta> void modifyMeta(Class<T> type, BiConsumer<ReadableNBT, T> handler) {
		finalizeChanges();

		final T meta = (T) bukkitItem.getItemMeta();
		handler.accept(new NBTContainer(getResolvedObject()).setReadOnly(true), meta);
		bukkitItem.setItemMeta(meta);
		updateCachedCompound();
		if (directApply)
			applyNBT(originalSrcStack);
	}

	/**
	 * Helper method that converts {@link ItemStack} to {@link NBTContainer} with
	 * all it's data like Material, Damage, Amount and Tags.
	 * 
	 * @param item
	 * @return Standalone {@link NBTContainer} with the Item's data
	 */
	public static NBTContainer convertItemtoNBT(ItemStack item) {
		return NBTReflectionUtil.convertNMSItemtoNBTCompound(ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, item));
	}

	/**
	 * Helper method to do the inverse of "convertItemtoNBT". Creates an
	 * {@link ItemStack} using the {@link NBTCompound}
	 * 
	 * @param comp
	 * @return ItemStack using the {@link NBTCompound}'s data
	 */
	@Nullable
	public static ItemStack convertNBTtoItem(NBTCompound comp) {
		return (ItemStack) ReflectionMethod.ITEMSTACK_BUKKITMIRROR.run(null,
				NBTReflectionUtil.convertNBTCompoundtoNMSItem(comp));
	}

	/**
	 * Helper method that converts {@link ItemStack}[] to {@link NBTContainer} with
	 * all its data like Material, Damage, Amount and Tags. This is a custom
	 * implementation and won't work with vanilla code(Shulker content etc).
	 * 
	 * @param items
	 * @return Standalone {@link NBTContainer} with the Item's data
	 */
	public static NBTContainer convertItemArraytoNBT(ItemStack[] items) {
		final NBTContainer container = new NBTContainer();
		container.setInteger("size", items.length);
		final NBTCompoundList list = container.getCompoundList("items");
		for (int i = 0; i < items.length; i++) {
			final ItemStack item = items[i];
			if (item == null || item.getType() == Material.AIR)
				continue;
			final NBTListCompound entry = list.addCompound();
			entry.setInteger("Slot", i);
			entry.mergeCompound(convertItemtoNBT(item));
		}
		return container;
	}

	/**
	 * Helper method to do the inverse of "convertItemArraytoNBT". Creates an
	 * {@link ItemStack}[] using the {@link NBTCompound}. This is a custom
	 * implementation and won't work with vanilla code (Shulker content, etc.).
	 * 
	 * Will return null for invalid data. Empty slots in the array are filled with
	 * AIR Stacks!
	 * 
	 * @param comp
	 * @return ItemStack[] using the {@link NBTCompound}'s data
	 */
	@Nullable
	public static ItemStack[] convertNBTtoItemArray(NBTCompound comp) {
		if (!comp.hasTag("size"))
			return null;
		final ItemStack[] rebuild = new ItemStack[comp.getInteger("size")];
		for (int i = 0; i < rebuild.length; i++)
			rebuild[i] = new ItemStack(Material.AIR);
		if (!comp.hasTag("items"))
			return rebuild;
		final NBTCompoundList list = comp.getCompoundList("items");
		for (final ReadWriteNBT lcomp : list)
			if (lcomp instanceof NBTCompound) {
				final int slot = lcomp.getInteger("Slot");
				rebuild[slot] = convertNBTtoItem((NBTCompound) lcomp);
			}
		return rebuild;
	}

	@Override
	protected void saveCompound() {
		if (directApply)
			applyNBT(originalSrcStack);
	}

}
