package org.mineacademy.fo.remain.nbt;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary solution to hold Mojang to unmapped Spigot mappings.
 * Note years later: nothing is more permanent than a temporary solution.
 *
 * @author tr7zw
 *
 */
public class MojangToMapping {

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
            this.put("net.minecraft.nbt.CompoundTag#merge(net.minecraft.nbt.CompoundTag)", "a");
            this.put("net.minecraft.nbt.CompoundTag#putBoolean(java.lang.String,boolean)", "a");
            this.put("net.minecraft.nbt.CompoundTag#getTagType(java.lang.String)", "d");
            this.put("net.minecraft.nbt.CompoundTag#putLong(java.lang.String,long)", "a");
            this.put("net.minecraft.nbt.CompoundTag#putLongArray(java.lang.String,long[])", "a");
            this.put("net.minecraft.nbt.CompoundTag#getLongArray(java.lang.String)", "o");
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

    @SuppressWarnings("serial")
    private static Map<String, String> MC1_20R2 = new HashMap<String, String>() {

        {
            this.putAll(MC1_20R1);

            this.put("net.minecraft.world.entity.Entity#getEncodeId()", "bu");
        }

    };

    @SuppressWarnings("serial")
    private static Map<String, String> MC1_20R3 = new HashMap<String, String>() {

        {
            this.putAll(MC1_20R2);

            this.put("net.minecraft.nbt.NbtIo#readCompressed(java.io.InputStream,net.minecraft.nbt.NbtAccounter)", "a");
            this.put("net.minecraft.nbt.NbtAccounter#unlimitedHeap()", "a");
            this.put("net.minecraft.world.entity.Entity#getEncodeId()", "bw");
            this.put("net.minecraft.world.level.block.entity.BlockEntity#saveWithId()", "p");
            this.put("net.minecraft.world.level.block.entity.BlockEntity#getBlockState()", "r");
            this.put("net.minecraft.world.item.ItemStack#CODEC", "a");
        }

    };

    @SuppressWarnings("serial")
    private static Map<String, String> MC1_20R4 = new HashMap<String, String>() {

        {
            this.putAll(MC1_20R3);

            this.put("net.minecraft.world.entity.Entity#getEncodeId()", "bC");
            this.put("net.minecraft.world.level.block.entity.BlockEntity#getBlockState()", "n");
            this.put("net.minecraft.core.component.DataComponents#CUSTOM_DATA", "b");
            this.put("net.minecraft.core.component.DataComponentHolder#get(net.minecraft.core.component.DataComponentType)", "a");
            this.put("net.minecraft.world.item.component.CustomData#copyTag()", "c");
            this.put("net.minecraft.world.item.ItemStack#set(net.minecraft.core.component.DataComponentType,java.lang.Object)", "b");
            this.put("net.minecraft.world.item.ItemStack#save(net.minecraft.core.HolderLookup$Provider)", "a");
            this.put("net.minecraft.server.MinecraftServer#registryAccess()", "bc");
            this.put("net.minecraft.world.item.ItemStack#parseOptional(net.minecraft.core.HolderLookup$Provider,net.minecraft.nbt.CompoundTag)", "a");
            this.put("net.minecraft.world.level.block.entity.BlockEntity#saveWithId(net.minecraft.core.HolderLookup$Provider)", "c");
            this.put("net.minecraft.world.level.block.entity.BlockEntity#loadWithComponents(net.minecraft.nbt.CompoundTag,net.minecraft.core.HolderLookup$Provider)", "c");
            this.put("net.minecraft.util.datafix.DataFixers#getDataFixer()", "a");
            this.put("net.minecraft.util.datafix.fixes.References#ITEM_STACK", "t");
            this.put("net.minecraft.nbt.NbtOps#INSTANCE", "a");
            this.put("net.minecraft.world.item.ItemStack#CODEC", "b");
        }

    };

    @SuppressWarnings("serial")
    private static Map<String, String> MC1_21R1 = new HashMap<String, String>() {

        {
            this.putAll(MC1_20R4);

            this.put("net.minecraft.world.entity.Entity#getEncodeId()", "bD");
        }

    };

    @SuppressWarnings("serial")
    private static Map<String, String> MC1_21R2 = new HashMap<String, String>() {

        {
            this.putAll(MC1_21R1);

            this.put("net.minecraft.server.MinecraftServer#registryAccess()", "ba");
            this.put("net.minecraft.world.entity.Entity#getEncodeId()", "bK");
            this.put("net.minecraft.world.level.block.entity.BlockEntity#getBlockState()", "m");
            this.put("net.minecraft.world.item.ItemStack#CODEC", "a");
        }

    };

    @SuppressWarnings("serial")
    private static Map<String, String> MC1_21R3 = new HashMap<String, String>() {

        {
            this.putAll(MC1_21R2);

            this.put("net.minecraft.world.item.component.CustomData#copyTag()", "d");
        }

    };

    @SuppressWarnings("serial")
    private static Map<String, String> MC1_21R4 = new HashMap<String, String>() {

        {
            this.putAll(MC1_21R3);

            this.put("net.minecraft.nbt.CompoundTag#getShort(java.lang.String)", "d");
            this.put("net.minecraft.nbt.CompoundTag#getString(java.lang.String)", "i");
            this.put("net.minecraft.nbt.CompoundTag#contains(java.lang.String)", "b");
            this.put("net.minecraft.nbt.CompoundTag#getDouble(java.lang.String)", "h");
            this.put("net.minecraft.nbt.CompoundTag#getByteArray(java.lang.String)", "j");
            this.put("net.minecraft.nbt.CompoundTag#getFloat(java.lang.String)", "g");
            this.put("net.minecraft.nbt.CompoundTag#get(java.lang.String)", "a");
            this.put("net.minecraft.nbt.CompoundTag#getLong(java.lang.String)", "f");
            this.put("net.minecraft.nbt.CompoundTag#getLongArray(java.lang.String)", "l");
            this.put("net.minecraft.nbt.CompoundTag#getInt(java.lang.String)", "e");
            this.put("net.minecraft.nbt.CompoundTag#getIntArray(java.lang.String)", "k");
            this.put("net.minecraft.nbt.CompoundTag#getCompound(java.lang.String)", "m");
            this.put("net.minecraft.nbt.CompoundTag#getByte(java.lang.String)", "c");
            this.put("net.minecraft.nbt.ListTag#getString(int)", "m");
            this.put("net.minecraft.world.entity.Entity#saveWithoutId(net.minecraft.nbt.CompoundTag)", "h");
            this.put("net.minecraft.world.entity.Entity#getEncodeId()", "bI");
            this.put("net.minecraft.world.entity.Entity#load(net.minecraft.nbt.CompoundTag)", "i");
            this.put("net.minecraft.nbt.Tag#getType()", "c");
            this.put("net.minecraft.nbt.TagType#getName()", "a");
            this.put("net.minecraft.nbt.CompoundTag#getList(java.lang.String)", "o");
            this.put("net.minecraft.world.item.ItemStack#parse(net.minecraft.core.HolderLookup$Provider,net.minecraft.nbt.Tag)", "a");
        }

    };

    public static Map<String, String> getMapping() {
        switch (MinecraftVersion.getVersion()) {
        case MC1_21_R4:
            return MC1_21R4;
        case MC1_21_R3:
            return MC1_21R3;
        case MC1_21_R2:
            return MC1_21R2;
        case MC1_21_R1:
            return MC1_21R1;
        case MC1_20_R4:
            return MC1_20R4;
        case MC1_20_R3:
            return MC1_20R3;
        case MC1_20_R2:
            return MC1_20R2;
        case MC1_20_R1:
            return MC1_20R1;
        case MC1_19_R3:
            return MC1_19R2;
        case MC1_19_R2:
            return MC1_19R2;
        case MC1_19_R1:
            return MC1_19R1;
        case MC1_18_R2:
            return MC1_18R2;
        case MC1_18_R1:
            return MC1_18R1;
        case UNKNOWN:
            return MC1_20R2; // assume it's a future version, so try the latest known mappings
        default:
            // this should never happen, unless a version is forgotten here(like 1.19R3 which uses the 1.19R2 mappings)
            throw new NbtApiException("No fitting mapping found for version " + MinecraftVersion.getVersion() + ". This is a bug!");
        }
    }

}
