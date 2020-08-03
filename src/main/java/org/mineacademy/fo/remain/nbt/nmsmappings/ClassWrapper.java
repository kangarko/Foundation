package org.mineacademy.fo.remain.nbt.nmsmappings;

import org.bukkit.Bukkit;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.remain.nbt.utils.MinecraftVersion;

/**
 * Wraps NMS and CRAFT classes
 *
 * @author tr7zw
 */
public enum ClassWrapper {
	CRAFT_ITEMSTACK(PackageWrapper.CRAFTBUKKIT, "inventory.CraftItemStack"),
	CRAFT_METAITEM(PackageWrapper.CRAFTBUKKIT, "inventory.CraftMetaItem"),
	CRAFT_ENTITY(PackageWrapper.CRAFTBUKKIT, "entity.CraftEntity"),
	CRAFT_WORLD(PackageWrapper.CRAFTBUKKIT, "CraftWorld"),
	NMS_NBTBASE(PackageWrapper.NMS, "NBTBase"),
	NMS_NBTTAGSTRING(PackageWrapper.NMS, "NBTTagString"),
	NMS_NBTTAGINT(PackageWrapper.NMS, "NBTTagInt"),
	NMS_NBTTAGFLOAT(PackageWrapper.NMS, "NBTTagFloat"),
	NMS_NBTTAGDOUBLE(PackageWrapper.NMS, "NBTTagDouble"),
	NMS_NBTTAGLONG(PackageWrapper.NMS, "NBTTagLong"),
	NMS_ITEMSTACK(PackageWrapper.NMS, "ItemStack"),
	NMS_NBTTAGCOMPOUND(PackageWrapper.NMS, "NBTTagCompound"),
	NMS_NBTTAGLIST(PackageWrapper.NMS, "NBTTagList"),
	NMS_NBTCOMPRESSEDSTREAMTOOLS(PackageWrapper.NMS, "NBTCompressedStreamTools"),
	NMS_MOJANGSONPARSER(PackageWrapper.NMS, "MojangsonParser"),
	NMS_TILEENTITY(PackageWrapper.NMS, "TileEntity"),
	NMS_BLOCKPOSITION(PackageWrapper.NMS, "BlockPosition"),
	NMS_WORLDSERVER(PackageWrapper.NMS, "WorldServer"),
	NMS_MINECRAFTSERVER(PackageWrapper.NMS, "MinecraftServer"),
	NMS_WORLD(PackageWrapper.NMS, "World"),
	NMS_ENTITY(PackageWrapper.NMS, "Entity"),
	NMS_ENTITYTYPES(PackageWrapper.NMS, "EntityTypes"),
	NMS_REGISTRYSIMPLE(PackageWrapper.NMS, "RegistrySimple", MinecraftVersion.MC1_11_R1, MinecraftVersion.MC1_12_R1),
	NMS_REGISTRYMATERIALS(PackageWrapper.NMS, "RegistryMaterials"),
	NMS_IREGISTRY(PackageWrapper.NMS, "IRegistry"),
	NMS_MINECRAFTKEY(PackageWrapper.NMS, "MinecraftKey"),
	NMS_GAMEPROFILESERIALIZER(PackageWrapper.NMS, "GameProfileSerializer"),
	NMS_IBLOCKDATA(PackageWrapper.NMS, "IBlockData");

	private Class<?> clazz;
	private boolean enabled = false;

	ClassWrapper(final PackageWrapper packageId, final String suffix) {
		this(packageId, suffix, null, null);
	}

	ClassWrapper(final PackageWrapper packageId, final String suffix, final MinecraftVersion from, final MinecraftVersion to) {
		if (from != null && MinecraftVersion.getVersion().getVersionId() < from.getVersionId())
			return;
		if (to != null && MinecraftVersion.getVersion().getVersionId() > to.getVersionId())
			return;
		enabled = true;
		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			clazz = Class.forName(packageId.getUri() + "." + version + "." + suffix);
		} catch (final Exception ex) {
			Debugger.debug("NBT", "[NBTAPI] Error while trying to resolve the class '" + suffix + "'!");
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
