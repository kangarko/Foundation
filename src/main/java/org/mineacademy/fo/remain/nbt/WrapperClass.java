package org.mineacademy.fo.remain.nbt;

import static org.mineacademy.fo.ReflectionUtil.CRAFTBUKKIT;
import static org.mineacademy.fo.ReflectionUtil.NMS;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;

/**
 * Wraps NMS and CRAFT classes
 *
 * @author tr7zw
 *
 */
@SuppressWarnings("javadoc")
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
	;

	private Class<?> clazz;
	private boolean enabled = false;

	WrapperClass(String packageId, String suffix) {
		this(packageId, suffix, null, null);
	}

	WrapperClass(String packageId, String suffix, MinecraftVersion.V from, MinecraftVersion.V to) {
		if (from != null && MinecraftVersion.olderThan(from))
			return;

		if (to != null && MinecraftVersion.newerThan(to))
			return;

		enabled = true;

		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			clazz = Class.forName(packageId + "." + version + "." + suffix);

		} catch (final Exception ex) {
			Common.error(ex, "Error while trying to resolve the class '" + suffix + "'!");
		}
	}

	/**
	 * @return The wrapped class
	 */
	public Class<?> getClazz(){
		return clazz;
	}

	/**
	 * @return Is this class available in this Version
	 */
	public boolean isEnabled() {
		return enabled;
	}

}
