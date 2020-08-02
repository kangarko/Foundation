package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

import static org.mineacademy.fo.ReflectionUtil.CRAFTBUKKIT;
import static org.mineacademy.fo.ReflectionUtil.NMS;

/**
 * Wraps NMS and CRAFT classes
 *
 * @author tr7zw
 */
public enum WrapperClass {
	CRAFT_ITEMSTACK(CRAFTBUKKIT, "inventory.CraftItemStack"),
	CRAFT_ENTITY(CRAFTBUKKIT, "entity.CraftEntity"),
	CRAFT_WORLD(CRAFTBUKKIT, "CraftWorld"),

	NMS_NBTBASE(NMS, "NBTBase"),
	NMS_NBTTAGSTRING(NMS, "NBTTagString"),
	NMS_NBTTAGINT(NMS, "NBTTagInt"),
	NMS_ITEMSTACK(NMS, "ItemStack"),
	NMS_NBTTAGCOMPOUND(NMS, "NBTTagCompound"),
	NMS_NBTTAGLIST(NMS, "NBTTagList"),
	NMS_NBTCOMPRESSEDSTREAMTOOLS(NMS, "NBTCompressedStreamTools"),
	NMS_MOJANGSONPARSER(NMS, "MojangsonParser"),
	NMS_TILEENTITY(NMS, "TileEntity"),
	NMS_BLOCKPOSITION(NMS, "BlockPosition"),
	NMS_WORLDSERVER(NMS, "WorldServer"),
	NMS_MINECRAFTSERVER(NMS, "MinecraftServer"),
	NMS_WORLD(NMS, "World"),
	NMS_ENTITY(NMS, "Entity"),
	NMS_ENTITYTYPES(NMS, "EntityTypes"),
	NMS_REGISTRYSIMPLE(NMS, "RegistrySimple", MinecraftVersion.V.v1_11, MinecraftVersion.V.v1_12),
	NMS_REGISTRYMATERIALS(NMS, "RegistryMaterials"),
	NMS_IREGISTRY(NMS, "IRegistry"),
	NMS_MINECRAFTKEY(NMS, "MinecraftKey"),
	NMS_IBLOCKDATA(NMS, "IBlockData");

	private Class<?> clazz;
	private boolean enabled = false;

	WrapperClass(final String packageId, final String suffix) {
		this(packageId, suffix, null, null);
	}

	WrapperClass(final String packageId, final String suffix, final MinecraftVersion.V from, final MinecraftVersion.V to) {
		if (from != null && MinecraftVersion.olderThan(from))
			return;

		if (to != null && MinecraftVersion.newerThan(to))
			return;

		enabled = true;

		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			clazz = Class.forName(packageId + "." + version + "." + suffix);

		} catch (final Exception ex) {
			if (MinecraftVersion.atLeast(V.v1_8))
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
