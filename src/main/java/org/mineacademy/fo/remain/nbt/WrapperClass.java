package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;

/**
 * Wraps NMS and CRAFT classes
 *
 * @author tr7zw
 *
 */
enum WrapperClass {
	CRAFT_ITEMSTACK(WrapperPackage.CRAFTBUKKIT, "inventory.CraftItemStack"),
	CRAFT_METAITEM(WrapperPackage.CRAFTBUKKIT, "inventory.CraftMetaItem"),
	CRAFT_ENTITY(WrapperPackage.CRAFTBUKKIT, "entity.CraftEntity"),
	CRAFT_WORLD(WrapperPackage.CRAFTBUKKIT, "CraftWorld"),
	NMS_NBTBASE(WrapperPackage.NMS, "NBTBase"),
	NMS_NBTTAGSTRING(WrapperPackage.NMS, "NBTTagString"),
	NMS_NBTTAGINT(WrapperPackage.NMS, "NBTTagInt"),
	NMS_NBTTAGFLOAT(WrapperPackage.NMS, "NBTTagFloat"),
	NMS_NBTTAGDOUBLE(WrapperPackage.NMS, "NBTTagDouble"),
	NMS_NBTTAGLONG(WrapperPackage.NMS, "NBTTagLong"),
	NMS_ITEMSTACK(WrapperPackage.NMS, "ItemStack"),
	NMS_NBTTAGCOMPOUND(WrapperPackage.NMS, "NBTTagCompound"),
	NMS_NBTTAGLIST(WrapperPackage.NMS, "NBTTagList"),
	NMS_NBTCOMPRESSEDSTREAMTOOLS(WrapperPackage.NMS, "NBTCompressedStreamTools"),
	NMS_MOJANGSONPARSER(WrapperPackage.NMS, "MojangsonParser"),
	NMS_TILEENTITY(WrapperPackage.NMS, "TileEntity"),
	NMS_BLOCKPOSITION(WrapperPackage.NMS, "BlockPosition", WrapperVersion.MC1_8_R3, null),
	NMS_WORLDSERVER(WrapperPackage.NMS, "WorldServer"),
	NMS_MINECRAFTSERVER(WrapperPackage.NMS, "MinecraftServer"),
	NMS_WORLD(WrapperPackage.NMS, "World"),
	NMS_ENTITY(WrapperPackage.NMS, "Entity"),
	NMS_ENTITYTYPES(WrapperPackage.NMS, "EntityTypes"),
	NMS_REGISTRYSIMPLE(WrapperPackage.NMS, "RegistrySimple", WrapperVersion.MC1_11_R1, WrapperVersion.MC1_12_R1),
	NMS_REGISTRYMATERIALS(WrapperPackage.NMS, "RegistryMaterials"),
	NMS_IREGISTRY(WrapperPackage.NMS, "IRegistry"),
	NMS_MINECRAFTKEY(WrapperPackage.NMS, "MinecraftKey", WrapperVersion.MC1_8_R3, null),
	NMS_GAMEPROFILESERIALIZER(WrapperPackage.NMS, "GameProfileSerializer"),
	NMS_IBLOCKDATA(WrapperPackage.NMS, "IBlockData", WrapperVersion.MC1_8_R3, null),
	GAMEPROFILE("com.mojang.authlib.GameProfile", WrapperVersion.MC1_8_R3);

	private Class<?> clazz;
	private boolean enabled = false;

	WrapperClass(WrapperPackage packageId, String suffix) {
		this(packageId, suffix, null, null);
	}

	WrapperClass(WrapperPackage packageId, String suffix, WrapperVersion from, WrapperVersion to) {
		if (from != null && WrapperVersion.getVersion().getVersionId() < from.getVersionId())
			return;

		if (to != null && WrapperVersion.getVersion().getVersionId() > to.getVersionId())
			return;

		enabled = true;

		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			clazz = Class.forName(packageId.getUri() + "." + version + "." + suffix);

		} catch (final Exception ex) {
			Common.error(ex, "[NBTAPI] Error while trying to resolve the class '" + suffix + "'!");
		}
	}

	WrapperClass(String path, WrapperVersion from) {
		if (from != null && WrapperVersion.getVersion().getVersionId() < from.getVersionId())
			return;

		enabled = true;

		try {
			clazz = Class.forName(path);
		} catch (final Exception ex) {
			Common.error(ex, "[NBTAPI] Error while trying to resolve the class '" + path + "'!");
		}
	}

	/**
	 * @return The wrapped class
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * @return Is this class available in this Version
	 */
	public boolean isEnabled() {
		return enabled;
	}

}
