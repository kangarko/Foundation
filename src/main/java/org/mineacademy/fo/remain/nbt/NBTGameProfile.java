package org.mineacademy.fo.remain.nbt;

public class NBTGameProfile {

	/**
	 * Convert a GameProfile to NBT. The NBT then can be modified or be stored
	 *
	 * @param profile
	 * @return A NBTContainer with all the GameProfile data
	 */
	public static NBTCompound toNBT(final Object profile) {
		return new NBTContainer(WrapperReflection.GAMEPROFILE_SERIALIZE.run(null, WrapperObject.NMS_NBTTAGCOMPOUND.getInstance(), profile));
	}

	/**
	 * Reconstructs a GameProfile from a NBTCompound
	 *
	 * @param compound Has to contain GameProfile data
	 * @return The reconstructed GameProfile
	 */
	public static Object fromNBT(final NBTCompound compound) {
		return WrapperReflection.GAMEPROFILE_DESERIALIZE.run(null, NBTReflectionUtil.gettoCompount(compound.getCompound(), compound));
	}

}
