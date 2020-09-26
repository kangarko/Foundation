package org.mineacademy.fo.remain.nbt;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;

/**
 * Wraps NMS and CRAFT classes
 *
 * @author tr7zw
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
	NMS_MOJANGSONPARSER(WrapperPackage.NMS, "MojangsonParser", WrapperVersion.MC1_7_R4),
	NMS_TILEENTITY(WrapperPackage.NMS, "TileEntity"),
	NMS_BLOCKPOSITION(WrapperPackage.NMS, "BlockPosition", WrapperVersion.MC1_8_R3),
	NMS_WORLDSERVER(WrapperPackage.NMS, "WorldServer"),
	NMS_MINECRAFTSERVER(WrapperPackage.NMS, "MinecraftServer"),
	NMS_WORLD(WrapperPackage.NMS, "World"),
	NMS_ENTITY(WrapperPackage.NMS, "Entity"),
	NMS_ENTITYTYPES(WrapperPackage.NMS, "EntityTypes"),
	NMS_REGISTRYSIMPLE(WrapperPackage.NMS, "RegistrySimple", WrapperVersion.MC1_11_R1, WrapperVersion.MC1_12_R1),
	NMS_REGISTRYMATERIALS(WrapperPackage.NMS, "RegistryMaterials", WrapperVersion.MC1_7_R4),
	NMS_IREGISTRY(WrapperPackage.NMS, "IRegistry"),
	NMS_MINECRAFTKEY(WrapperPackage.NMS, "MinecraftKey", WrapperVersion.MC1_8_R3),
	NMS_GAMEPROFILESERIALIZER(WrapperPackage.NMS, "GameProfileSerializer", WrapperVersion.MC1_7_R4),
	NMS_IBLOCKDATA(WrapperPackage.NMS, "IBlockData", WrapperVersion.MC1_8_R3);

	private Class<?> clazz;
	private boolean enabled = false;

	WrapperClass(final WrapperPackage packageId, final String suffix) {
		this(packageId, suffix, null, null);
	}

	WrapperClass(final WrapperPackage packageId, final String suffix, final WrapperVersion from) {
		this(packageId, suffix, from, null);
	}

	WrapperClass(final WrapperPackage packageId, final String suffix, final WrapperVersion from, final WrapperVersion to) {
		if (from != null && WrapperVersion.getVersion().getVersionId() < from.getVersionId())
			return;

		if (to != null && WrapperVersion.getVersion().getVersionId() > to.getVersionId())
			return;

		enabled = true;

		final String nms = MinecraftVersion.getServerVersion();

		try {
			clazz = Class.forName(packageId.getUri() + "." + (nms.isEmpty() ? "" : nms + ".") + suffix);

		} catch (final Exception ex) {

			if (nms.isEmpty()) {
				if (suffix.equals("inventory.CraftMetaItem"))
					return;
			}

			Common.throwError(ex,
					"Error while trying to resolve NBT class '" + suffix + "'!");
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
