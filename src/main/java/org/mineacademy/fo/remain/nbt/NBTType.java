package org.mineacademy.fo.remain.nbt;

/**
 * Enum of all NBT Types Minecraft contains
 *
 * @author tr7zw
 *
 */
public enum NBTType {
	NBTTagEnd(0),
	NBTTagByte(1),
	NBTTagShort(2),
	NBTTagInt(3),
	NBTTagLong(4),
	NBTTagFloat(5),
	NBTTagDouble(6),
	NBTTagByteArray(7),
	NBTTagIntArray(11),
	NBTTagString(8),
	NBTTagList(9),
	NBTTagCompound(10);

	NBTType(final int i) {
		id = i;
	}

	private final int id;

	/**
	 * @return Id used by Minecraft internally
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id Internal Minecraft id
	 * @return Enum representing the id, NBTTagEnd for invalide ids
	 */
	public static NBTType valueOf(final int id) {
		for (final NBTType t : values())
			if (t.getId() == id)
				return t;
		return NBTType.NBTTagEnd;
	}

}
