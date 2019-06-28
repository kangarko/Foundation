package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;

/**
 * Wraps NMS and CRAFT classes
 *
 * @author tr7zw
 *
 */
enum WrapperClass {
	CRAFT_ITEMSTACK(WrapperPackage.CRAFTBUKKIT, "inventory.CraftItemStack"),
	CRAFT_ENTITY(WrapperPackage.CRAFTBUKKIT, "entity.CraftEntity"),
	CRAFT_WORLD(WrapperPackage.CRAFTBUKKIT, "CraftWorld"),
	NMS_NBTBASE(WrapperPackage.NMS, "NBTBase"),
	NMS_NBTTAGSTRING(WrapperPackage.NMS, "NBTTagString"),
	NMS_NBTTAGINT(WrapperPackage.NMS, "NBTTagInt"),
	NMS_ITEMSTACK(WrapperPackage.NMS, "ItemStack"),
	NMS_NBTTAGCOMPOUND(WrapperPackage.NMS, "NBTTagCompound"),
	NMS_NBTTAGLIST(WrapperPackage.NMS, "NBTTagList"),
	NMS_NBTCOMPRESSEDSTREAMTOOLS(WrapperPackage.NMS, "NBTCompressedStreamTools"),
	NMS_MOJANGSONPARSER(WrapperPackage.NMS, "MojangsonParser"),
	NMS_TILEENTITY(WrapperPackage.NMS, "TileEntity"),
	NMS_BLOCKPOSITION(WrapperPackage.NMS, "BlockPosition"),
	NMS_WORLDSERVER(WrapperPackage.NMS, "WorldServer"),
	NMS_MINECRAFTSERVER(WrapperPackage.NMS, "MinecraftServer"),
	NMS_WORLD(WrapperPackage.NMS, "World"),
	NMS_ENTITY(WrapperPackage.NMS, "Entity"),
	NMS_ENTITYTYPES(WrapperPackage.NMS, "EntityTypes"),
	NMS_REGISTRYSIMPLE(WrapperPackage.NMS, "RegistrySimple", MinecraftVersion.V.v1_11, MinecraftVersion.V.v1_12),
	NMS_REGISTRYMATERIALS(WrapperPackage.NMS, "RegistryMaterials"),
	NMS_IREGISTRY(WrapperPackage.NMS, "IRegistry"),
	NMS_MINECRAFTKEY(WrapperPackage.NMS, "MinecraftKey"),

	;

	private Class<?> clazz;
	private boolean enabled = false;

	WrapperClass(WrapperPackage packageId, String suffix) {
		this(packageId, suffix, null, null);
	}

	WrapperClass(WrapperPackage packageId, String suffix, MinecraftVersion.V from, MinecraftVersion.V to) {
		if (from != null && MinecraftVersion.olderThan(from))
			return;

		if (to != null && MinecraftVersion.newerThan(to))
			return;

		enabled = true;

		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			clazz = Class.forName(packageId.getUri() + "." + version + "." + suffix);

		} catch (final Exception ex) {
			Common.error(ex, "Error while trying to resolve the class '" + suffix + "'!");
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
