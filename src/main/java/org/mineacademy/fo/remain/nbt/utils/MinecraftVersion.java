package org.mineacademy.fo.remain.nbt.utils;

import java.util.logging.Logger;

import org.bukkit.Bukkit;

/**
 * This class acts as the "Brain" of the NBTApi. It contains the main logger for
 * other classes,registers bStats and checks rather Maven shading was done
 * correctly.
 *
 * @author tr7zw
 */
public enum MinecraftVersion {
	UNKNOWN(Integer.MAX_VALUE), // Use the newest known mappings
	MC1_7_R4(174),
	MC1_8_R3(183),
	MC1_9_R1(191),
	MC1_9_R2(192),
	MC1_10_R1(1101),
	MC1_11_R1(1111),
	MC1_12_R1(1121),
	MC1_13_R1(1131),
	MC1_13_R2(1132),
	MC1_14_R1(1141),
	MC1_15_R1(1151),
	MC1_16_R1(1161);

	private static MinecraftVersion version;
	private static Boolean hasGsonSupport;

	/**
	 * Logger used by the api
	 */
	public static final Logger logger = Logger.getLogger("NBTAPI");

	// NBT-API Version
	protected static final String VERSION = "2.4.2-SNAPSHOT";

	private final int versionId;

	MinecraftVersion(final int versionId) {
		this.versionId = versionId;
	}

	/**
	 * @return A simple comparable Integer, representing the version.
	 */
	public int getVersionId() {
		return versionId;
	}

	/**
	 * Getter for this servers MinecraftVersion. Also init's bStats and checks the
	 * shading.
	 *
	 * @return The enum for the MinecraftVersion this server is running
	 */
	public static MinecraftVersion getVersion() {
		if (version != null)
			return version;
		final String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		//		logger.info("[NBTAPI] Found Spigot: " + ver + "! Trying to find NMS support");
		try {
			version = MinecraftVersion.valueOf(ver.replace("v", "MC"));
		} catch (final IllegalArgumentException ex) {
			version = MinecraftVersion.UNKNOWN;
		}
		if (version != UNKNOWN) {

			//			logger.info("[NBTAPI] NMS support '" + version.name() + "' loaded!")
		} else
			logger.warning("[NBTAPI] Wasn't able to find NMS Support! Some functions may not work!");
		return version;
	}

	/**
	 * @return True, if Gson is usable
	 */
	public static boolean hasGsonSupport() {
		if (hasGsonSupport != null)
			return hasGsonSupport;
		try {
			logger.info("[NBTAPI] Found Gson: " + Class.forName("com.google.gson.Gson"));
			hasGsonSupport = true;
		} catch (final Exception ex) {
			logger.info("[NBTAPI] Gson not found! This will not allow the usage of some methods!");
			hasGsonSupport = false;
		}
		return hasGsonSupport;
	}
}
