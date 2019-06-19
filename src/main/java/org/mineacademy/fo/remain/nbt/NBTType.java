package org.mineacademy.fo.remain.nbt;

import lombok.Getter;

/**
 * Represents a NBT tag type
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

	/**
	 * Create a new enum by tag ID
	 *
	 * @param id
	 */
	private NBTType(int id) {
		this.id = id;
	}

	/**
	 * The internal tag id
	 */
	@Getter
	private final int id;

	/**
	 * Get NBTType from internal ID
	 *
	 * @param id
	 * @return the tag, or NBTTagEnd if not found
	 */
	public static NBTType valueOf(int id) {
		for (final NBTType t : values())
			if (t.getId() == id)
				return t;

		return NBTType.NBTTagEnd;
	}
}
