package org.mineacademy.fo.remain.nbt;

/**
 * A Standalone {@link NBTCompound} implementation. All data is just kept inside
 * this Object.
 *
 * @author tr7zw
 *
 */
public class NBTContainer extends NBTCompound {

	private Object nbt;

	/**
	 * Creates an empty, standalone NBTCompound
	 */
	public NBTContainer() {
		super(null, null);
		nbt = WrapperObject.NMS_NBTTAGCOMPOUND.getInstance();
	}

	/**
	 * Takes in any NMS Compound to wrap it
	 *
	 * @param nbt
	 */
	public NBTContainer(Object nbt) {
		super(null, null);
		this.nbt = nbt;
	}

	/**
	 * Parses in a NBT String to a standalone {@link NBTCompound}. Can throw a
	 * {@link NbtApiException} in case something goes wrong.
	 *
	 * @param nbtString
	 */
	public NBTContainer(String nbtString) {
		super(null, null);
		try {
			nbt = WrapperMethod.PARSE_NBT.run(null, nbtString);
		} catch (final Exception ex) {
			throw new NbtApiException("Unable to parse Malformed Json!", ex);
		}
	}

	@Override
	public Object getCompound() {
		return nbt;
	}

	@Override
	public void setCompound(Object tag) {
		nbt = tag;
	}

}
