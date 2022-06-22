package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

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

	/**
	 * @param entity Any valid Bukkit Entity
	 */
	public NBTEntity(Entity entity) {
		super(null, null);
		if (entity == null)
			throw new NullPointerException("Entity can't be null!");
		this.ent = entity;
	}

	@Override
	public Object getCompound() {
		if (!Bukkit.isPrimaryThread())
			throw new FoException("Entity NBT needs to be accessed sync!");
		return NBTReflectionUtil.getEntityNBTTagCompound(NBTReflectionUtil.getNMSEntity(this.ent));
	}

	@Override
	protected void setCompound(Object compound) {
		if (!Bukkit.isPrimaryThread())
			throw new FoException("Entity NBT needs to be accessed sync!");
		NBTReflectionUtil.setEntityNBTTag(compound, NBTReflectionUtil.getNMSEntity(this.ent));
	}

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.14+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_14), "NBTEntity#getPersistentDataContainer requires Minecraft 1.14+");

		return new NBTPersistentDataContainer(this.ent.getPersistentDataContainer());
	}

}
