package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

/**
 * NBT class to access vanilla tags from Entities. Entities don't support custom
 * tags. Use the NBTInjector for custom tags. Changes will be instantly applied
 * to the Entity, use the merge method to do many things at once.
 *
 * @author tr7zw
 *
 */
public class NBTEntity extends NBTCompound {

	private final Entity ent;
	private final boolean readonly;
	private final Object compound;

	/**
	 * @param entity   Any valid Bukkit Entity
	 * @param readonly Readonly makes a copy at init, only reading from that copy
	 */
	protected NBTEntity(Entity entity, boolean readonly) {
		super(null, null);
		if (entity == null)
			throw new NullPointerException("Entity can't be null!");
		this.readonly = readonly;
		this.ent = entity;
		if (readonly)
			this.compound = this.getCompound();
		else
			this.compound = null;
	}

	/**
	 * @param entity Any valid Bukkit Entity
	 */
	public NBTEntity(Entity entity) {
		super(null, null);
		if (entity == null)
			throw new NullPointerException("Entity can't be null!");
		this.readonly = false;
		this.compound = null;
		this.ent = entity;
	}

	@Override
	public Object getCompound() {
		// this runs before async check, since it's just a copy
		if (this.readonly && this.compound != null)
			return this.compound;
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("Entity NBT needs to be accessed sync!");
		return NBTReflectionUtil.getEntityNBTTagCompound(NBTReflectionUtil.getNMSEntity(this.ent));
	}

	@Override
	protected void setCompound(Object compound) {
		if (this.readonly)
			throw new NbtApiException("Tried setting data in read only mode!");
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("Entity NBT needs to be accessed sync!");
		NBTReflectionUtil.setEntityNBTTag(compound, NBTReflectionUtil.getNMSEntity(this.ent));
	}

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.14+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_14), "MC 1.14 required!");

		return new NBTPersistentDataContainer(this.ent.getPersistentDataContainer());
	}

}
