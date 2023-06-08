package org.mineacademy.fo.remain.nbt;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary solution to hold Mojang to unmapped Spigot mappings.
 *
 * @author tr7zw
 *
 */
class MojangToMapping {

	@SuppressWarnings("serial")
	private static Map<String, String> MC1_18R1 = new HashMap<String, String>() {

		{
			this.put("net.minecraft.nbt.CompoundTag#contains(java.lang.String)", "e");
			this.put("net.minecraft.nbt.CompoundTag#getCompound(java.lang.String)", "p");
			this.put("net.minecraft.nbt.CompoundTag#getList(java.lang.String,int)", "c");
			this.put("net.minecraft.nbt.CompoundTag#putByteArray(java.lang.String,byte[])", "a");
			this.put("net.minecraft.nbt.CompoundTag#getDouble(java.lang.String)", "k");
			this.put("net.minecraft.nbt.CompoundTag#putDouble(java.lang.String,double)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getByteArray(java.lang.String)", "m");
			this.put("net.minecraft.nbt.CompoundTag#putInt(java.lang.String,int)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getIntArray(java.lang.String)", "n");
			this.put("net.minecraft.nbt.CompoundTag#remove(java.lang.String)", "r");
			this.put("net.minecraft.nbt.CompoundTag#get(java.lang.String)", "c");
			this.put("net.minecraft.nbt.CompoundTag#put(java.lang.String,net.minecraft.nbt.Tag)", "a");
			this.put("net.minecraft.nbt.CompoundTag#putBoolean(java.lang.String,boolean)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getTagType(java.lang.String)", "d");
			this.put("net.minecraft.nbt.CompoundTag#putLong(java.lang.String,long)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getString(java.lang.String)", "l");
			this.put("net.minecraft.nbt.CompoundTag#getInt(java.lang.String)", "h");
			this.put("net.minecraft.nbt.CompoundTag#putString(java.lang.String,java.lang.String)", "a");
			this.put("net.minecraft.nbt.CompoundTag#put(java.lang.String,net.minecraft.nbt.Tag)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getByte(java.lang.String)", "f");
			this.put("net.minecraft.nbt.CompoundTag#putIntArray(java.lang.String,int[])", "a");
			this.put("net.minecraft.nbt.CompoundTag#getShort(java.lang.String)", "g");
			this.put("net.minecraft.nbt.CompoundTag#putByte(java.lang.String,byte)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getAllKeys()", "d");
			this.put("net.minecraft.nbt.CompoundTag#putUUID(java.lang.String,java.util.UUID)", "a");
			this.put("net.minecraft.nbt.CompoundTag#putShort(java.lang.String,short)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getLong(java.lang.String)", "i");
			this.put("net.minecraft.nbt.CompoundTag#putFloat(java.lang.String,float)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getBoolean(java.lang.String)", "q");
			this.put("net.minecraft.nbt.CompoundTag#getUUID(java.lang.String)", "a");
			this.put("net.minecraft.nbt.CompoundTag#getFloat(java.lang.String)", "j");
			this.put("net.minecraft.nbt.ListTag#addTag(int,net.minecraft.nbt.Tag)", "b");
			this.put("net.minecraft.nbt.ListTag#setTag(int,net.minecraft.nbt.Tag)", "a");
			this.put("net.minecraft.nbt.ListTag#getString(int)", "j");
			this.put("net.minecraft.nbt.ListTag#remove(int)", "remove");
			this.put("net.minecraft.nbt.ListTag#getCompound(int)", "a");
			this.put("net.minecraft.nbt.ListTag#size()", "size");
			this.put("net.minecraft.nbt.ListTag#get(int)", "get");
			this.put("net.minecraft.nbt.NbtIo#readCompressed(java.io.InputStream)", "a");
			this.put("net.minecraft.nbt.NbtIo#writeCompressed(net.minecraft.nbt.CompoundTag,java.io.OutputStream)", "a");
			this.put("net.minecraft.nbt.NbtUtils#readGameProfile(net.minecraft.nbt.CompoundTag)", "a");
			this.put("net.minecraft.nbt.NbtUtils#writeGameProfile(net.minecraft.nbt.CompoundTag,com.mojang.authlib.GameProfile)",
					"a");
			this.put("net.minecraft.nbt.TagParser#parseTag(java.lang.String)", "a");
			this.put("net.minecraft.world.entity.Entity#getEncodeId()", "bk");
			this.put("net.minecraft.world.entity.Entity#load(net.minecraft.nbt.CompoundTag)", "g");
			this.put("net.minecraft.world.entity.Entity#saveWithoutId(net.minecraft.nbt.CompoundTag)", "f");
			this.put("net.minecraft.world.item.ItemStack#setTag(net.minecraft.nbt.CompoundTag)", "c");
			this.put("net.minecraft.world.item.ItemStack#getTag()", "s");
			this.put("net.minecraft.world.item.ItemStack#save(net.minecraft.nbt.CompoundTag)", "b");
			this.put("net.minecraft.world.level.block.entity.BlockEntity#saveWithId()", "n");
			this.put("net.minecraft.world.level.block.entity.BlockEntity#getBlockState()", "q");
			this.put("net.minecraft.world.level.block.entity.BlockEntity#load(net.minecraft.nbt.CompoundTag)", "a");
			this.put("net.minecraft.server.level.ServerLevel#getBlockEntity(net.minecraft.core.BlockPos)", "c_");
		}

	};

	@SuppressWarnings("serial")
	private static Map<String, String> MC1_18R2 = new HashMap<String, String>() {

		{
			this.putAll(MC1_18R1);

			this.put("net.minecraft.world.item.ItemStack#getTag()", "t");
		}
	};

	@SuppressWarnings("serial")
	private static Map<String, String> MC1_19R1 = new HashMap<String, String>() {

		{
			this.putAll(MC1_18R2);

			this.put("net.minecraft.world.item.ItemStack#getTag()", "u");
		}

	};

	@SuppressWarnings("serial")
	private static Map<String, String> MC1_19R2 = new HashMap<String, String>() {

		{
			this.putAll(MC1_19R1);

			this.put("net.minecraft.nbt.CompoundTag#getAllKeys()", "e");
		}

	};

	@SuppressWarnings("serial")
	private static Map<String, String> MC1_20R1 = new HashMap<String, String>() {

		{
			this.putAll(MC1_19R2);

			this.put("net.minecraft.world.entity.Entity#getEncodeId()", "br");
			this.put("net.minecraft.world.item.ItemStack#getTag()", "v");
		}

	};

	public static Map<String, String> getMapping() {
		switch (MinecraftVersion.getVersion()) {
			case MC1_20_R1:
				return MC1_20R1;
			case MC1_19_R2:
				return MC1_19R2;
			case MC1_19_R1:
				return MC1_19R1;
			case MC1_18_R2:
				return MC1_18R2;
			case MC1_18_R1:
				return MC1_18R1;
			default:
				return MC1_19R2;// throw new NbtApiException("This version of the NBTAPI is not compatible with
								// this server version!");
		}
	}

}
