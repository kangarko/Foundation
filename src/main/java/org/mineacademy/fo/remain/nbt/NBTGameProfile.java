package org.mineacademy.fo.remain.nbt;

public class NBTGameProfile {

	/**
	 * Convert a GameProfile to NBT. The NBT then can be modified or be stored
	 *
	 * @param profile
	 * @return A NBTContainer with all the GameProfile data
	 */
	public static NBTCompound toNBT(Object profile) {
		return new NBTContainer(ReflectionMethod.GAMEPROFILE_SERIALIZE.run(null, ObjectCreator.NMS_NBTTAGCOMPOUND.getInstance(), profile));
	}

	/**
	 * Reconstructs a GameProfile from a NBTCompound
	 *
	 * @param compound Has to contain GameProfile data
	 * @return The reconstructed GameProfile
	 */
	public static Object fromNBT(NBTCompound compound) {
		return ReflectionMethod.GAMEPROFILE_DESERIALIZE.run(null, NBTReflectionUtil.getToCompound(compound.getCompound(), compound));
	}

}
