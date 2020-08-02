package org.mineacademy.fo.remain.nbt;

import com.mojang.authlib.GameProfile;
import org.mineacademy.fo.remain.nbt.nmsmappings.ObjectCreator;
import org.mineacademy.fo.remain.nbt.nmsmappings.ReflectionMethod;

public class NBTGameProfile {

	/**
	 * Convert a GameProfile to NBT. The NBT then can be modified or be stored
	 *
	 * @param profile
	 * @return A NBTContainer with all the GameProfile data
	 */
	public static NBTCompound toNBT(final GameProfile profile) {
		return new NBTContainer(ReflectionMethod.GAMEPROFILE_SERIALIZE.run(null, ObjectCreator.NMS_NBTTAGCOMPOUND.getInstance(), profile));
	}

	/**
	 * Reconstructs a GameProfile from a NBTCompound
	 *
	 * @param compound Has to contain GameProfile data
	 * @return The reconstructed GameProfile
	 */
	public static GameProfile fromNBT(final NBTCompound compound) {
		return (GameProfile) ReflectionMethod.GAMEPROFILE_DESERIALIZE.run(null, NBTReflectionUtil.gettoCompount(compound.getCompound(), compound));
	}

}
