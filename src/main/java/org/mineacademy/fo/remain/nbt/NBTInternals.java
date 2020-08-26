package org.mineacademy.fo.remain.nbt;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;

/**
 * Utility class to test NBT library's compatibility
 *
 * @deprecated internal use only, please use {@link NBTEntity},
 * {@link NBTTileEntity} and {@link NBTItem} to modify entity and
 * items
 */
@Deprecated
public class NBTInternals {

	// Holds all test keys
	private static final String STRING_KEY = "stringTest";
	private static final String INT_KEY = "intTest";
	private static final String DOUBLE_KEY = "doubleTest";
	private static final String BOOLEAN_KEY = "booleanTest";
	private static final String COMPONENT_KEY = "componentTest";
	private static final String SHORT_KEY = "shortTest";
	private static final String BYTE_KEY = "byteTest";
	private static final String FLOAT_KEY = "floatTest";
	private static final String LONG_KEY = "longTest";
	private static final String INTARRAY_KEY = "intarrayTest";
	private static final String BYTEARRAY_KEY = "bytearrayTest";

	// Holds all test values
	private static final String STRING_VALUE = "TestString";
	private static final int INT_VALUE = 42;
	private static final double DOUBLE_VALUE = 1.5d;
	private static final boolean BOOLEAN_VALUE = true;
	private static final short SHORT_VALUE = 64;
	private static final byte BYTE_VALUE = 7;
	private static final float FLOAT_VALUE = 13.37f;
	private static final long LONG_VALUE = Integer.MAX_VALUE + 42L;
	private static final int[] INTARRAY_VALUE = new int[] { 1337, 42, 69 };
	private static final byte[] BYTEARRAY_VALUE = new byte[] { 8, 7, 3, 2 };

	/**
	 * Checks if the NBT library is working properly
	 */
	@SuppressWarnings("rawtypes")
	public static boolean checkCompatible() {
		boolean compatible = true;
		final boolean jsonCompatible = true;

		ItemStack item = new ItemStack(Material.STONE, 1);
		NBTItem nbtItem = new NBTItem(item);

		nbtItem.setString(STRING_KEY, STRING_VALUE);
		nbtItem.setInteger(INT_KEY, INT_VALUE);
		nbtItem.setDouble(DOUBLE_KEY, DOUBLE_VALUE);
		nbtItem.setBoolean(BOOLEAN_KEY, BOOLEAN_VALUE);
		nbtItem.setByte(BYTE_KEY, BYTE_VALUE);
		nbtItem.setShort(SHORT_KEY, SHORT_VALUE);
		nbtItem.setLong(LONG_KEY, LONG_VALUE);
		nbtItem.setFloat(FLOAT_KEY, FLOAT_VALUE);
		nbtItem.setIntArray(INTARRAY_KEY, INTARRAY_VALUE);
		nbtItem.setByteArray(BYTEARRAY_KEY, BYTEARRAY_VALUE);
		nbtItem.addCompound(COMPONENT_KEY);

		NBTCompound comp = nbtItem.getCompound(COMPONENT_KEY);
		comp.setString(STRING_KEY, STRING_VALUE + "2");
		comp.setInteger(INT_KEY, INT_VALUE * 2);
		comp.setDouble(DOUBLE_KEY, DOUBLE_VALUE * 2);

		NBTList list = comp.getStringList("testlist");
		list.add("test1");
		list.add("test2");
		list.add("test3");
		list.add("test4");
		list.set(2, "test42");
		list.remove(1);

		NBTCompoundList taglist = comp.getCompoundList("complist");
		NBTListCompound lcomp = taglist.addCompound();

		lcomp.setDouble("double1", 0.3333);
		lcomp.setInteger("int1", 42);
		lcomp.setString("test1", "test1");
		lcomp.setString("test2", "test2");
		lcomp.removeKey("test1");

		item = nbtItem.getItem();
		nbtItem = null;
		comp = null;
		list = null;
		nbtItem = new NBTItem(item);

		if (!nbtItem.hasKey(STRING_KEY)) {
			Common.log("NBTAPI was not able to check a key!");

			compatible = false;
		}

		if (!STRING_VALUE.equals(nbtItem.getString(STRING_KEY))
				|| nbtItem.getInteger(INT_KEY) != INT_VALUE
				|| nbtItem.getDouble(DOUBLE_KEY) != DOUBLE_VALUE
				|| nbtItem.getByte(BYTE_KEY) != BYTE_VALUE
				|| nbtItem.getShort(SHORT_KEY) != SHORT_VALUE
				|| nbtItem.getFloat(FLOAT_KEY) != FLOAT_VALUE
				|| nbtItem.getLong(LONG_KEY) != LONG_VALUE
				|| nbtItem.getIntArray(INTARRAY_KEY).length != INTARRAY_VALUE.length
				|| nbtItem.getByteArray(BYTEARRAY_KEY).length != BYTEARRAY_VALUE.length
				|| !nbtItem.getBoolean(BOOLEAN_KEY).equals(BOOLEAN_VALUE)) {
			Common.log("One key does not equal the original value!");

			compatible = false;
		}
		nbtItem.setString(STRING_KEY, null);
		if (nbtItem.getKeys().size() != 10) {
			Common.log("Wasn't able to remove a key (Got " + nbtItem.getKeys().size() + " when expecting 10)!");

			compatible = false;
		}
		comp = nbtItem.getCompound(COMPONENT_KEY);
		if (comp == null) {
			Common.log("Wasn't able to get the NBTCompound!!");

			compatible = false;
		}
		if (!comp.hasKey(STRING_KEY)) {
			Common.log("Wasn't able to check a compound key!");

			compatible = false;
		}
		if (!(STRING_VALUE + "2").equals(comp.getString(STRING_KEY))
				|| comp.getInteger(INT_KEY) != INT_VALUE * 2
				|| comp.getDouble(DOUBLE_KEY) != DOUBLE_VALUE * 2
				|| comp.getBoolean(BOOLEAN_KEY) == BOOLEAN_VALUE) {
			Common.log("One key does not equal the original compound value!");

			compatible = false;
		}

		list = comp.getStringList("testlist");
		if (comp.getType("testlist") != NBTType.NBTTagList) {
			Common.log("Wasn't able to get the correct Tag type!");

			compatible = false;
		}
		if (!list.get(1).equals("test42") || list.size() != 3)
			Common.log("The List support got an error, and may not work!");
		taglist = comp.getCompoundList("complist");
		if (taglist.size() == 1) {
			lcomp = taglist.get(0);
			if (lcomp.getKeys().size() != 3) {
				Common.log("Wrong key amount in Taglist (" + lcomp.getKeys().size() + ")!");

				compatible = false;
			} else if (lcomp.getDouble("double1") == 0.3333 && lcomp.getInteger("int1") == 42 && lcomp.getString("test2").equals("test2")
					&& !lcomp.hasKey("test1")) {
				//ok
			} else {
				Common.log("One key in the Taglist changed!");

				compatible = false;
			}
		} else {
			Common.log("Taglist is empty!");

			compatible = false;
		}

		if ((!compatible || !jsonCompatible) && MinecraftVersion.newerThan(V.v1_7)) {
			Common.log("WARNING");
			Common.log("The NBT library seems to be broken with your");
			Common.log("Spigot version " + MinecraftVersion.getServerVersion());
			Common.log();
			Common.log("Please contact the developer of this library.");
		}

		return compatible && jsonCompatible;
	}

}
