package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;

/**
 * This class acts as the "Brain" of the NBTApi.
 *
 * @author tr7zw
 */
enum WrapperVersion {
	UNKNOWN(Integer.MAX_VALUE), // Use the newest known mappings
	MC1_4_R1(147),
	MC1_5_R3(152),
	MC1_6_R3(163),
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
	MC1_16_R1(1161),
	MC1_16_R2(1162),
	;

	private static WrapperVersion version;
	private final int versionId;

	WrapperVersion(final int versionId) {
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
	public static WrapperVersion getVersion() {
		if (version != null)
			return version;

		final String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

		try {
			version = WrapperVersion.valueOf(ver.replace("v", "MC"));

		} catch (final IllegalArgumentException ex) {
			version = WrapperVersion.UNKNOWN;
		}

		if (version == UNKNOWN)
			Common.log("[NBTAPI] Wasn't able to find NMS Support! Some functions may not work!");

		return version;
	}
}
