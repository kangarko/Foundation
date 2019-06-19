package org.mineacademy.fo.remain.nbt;

import org.bukkit.entity.Entity;

/**
 * Represents an entity NBT tag
 */
public class NBTEntity extends NBTCompound {

	// Safety compatibility check
	public static boolean COMPATIBLE = true;

	/**
	 * The entity associated with this tag
	 */
	private final Entity entity;

	/**
	 * Access an entity's NBT tag
	 */
	public NBTEntity(Entity entity) {
		super(null, null);

		this.entity = entity;
	}

	@Override
	protected Object getCompound() {
		return !COMPATIBLE ? null : NBTReflectionUtil.getEntityNBTTagCompound(NBTReflectionUtil.getNMSEntity(entity));
	}

	@Override
	protected void setCompound(Object tag) {
		if (COMPATIBLE)
			NBTReflectionUtil.setEntityNBTTag(tag, NBTReflectionUtil.getNMSEntity(entity));
	}

}
