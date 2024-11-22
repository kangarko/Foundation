package org.mineacademy.fo;

import org.mineacademy.fo.exception.FoException;

/**
 * Represents the current Minecraft version the plugin is loaded on.
 */
public final class MinecraftVersion {

	/**
	 * The wrapper representation of the version.
	 */
	private static V current = null;

	/**
	 * The subversion such as 8 in 1.8.8 or 6 in 1.20.6.
	 */
	private static int subversion = -1;

	/**
	 * The version wrapper.
	 */
	public enum V {
		v1_21(21),
		v1_20(20),
		v1_19(19),
		v1_18(18),
		v1_17(17),
		v1_16(16),
		v1_15(15),
		v1_14(14),
		v1_13(13),
		v1_12(12),
		v1_11(11),
		v1_10(10),
		v1_9(9),
		v1_8(8),
		v1_7(7),
		v1_6(6),
		v1_5(5),
		v1_4(4),
		v1_3_AND_BELOW(3);

		/**
		 * The numeric version (the second part of the 1.x number).
		 */
		private final int minorVersionNumber;

		/**
		 * Creates new enum for a Minecraft version.
		 *
		 * @param version
		 */
		V(int version) {
			this.minorVersionNumber = version;
		}

		/**
		 * Attempts to get the version from number.
		 *
		 * @deprecated internal use only
		 * @param number
		 * @return
		 * @throws RuntimeException if number not found
		 */
		@Deprecated
		public static V parse(int number) {
			for (final V v : values())
				if (v.minorVersionNumber == number)
					return v;

			throw new FoException("Invalid version number: " + number);
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return "1." + this.minorVersionNumber;
		}
	}

	/**
	 * Does the current Minecraft version equal the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean equals(V version) {
		return compareWith(version) == 0;
	}

	/**
	 * Is the current Minecraft version older than the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean olderThan(V version) {
		return compareWith(version) < 0;
	}

	/**
	 * Is the current Minecraft version newer than the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean newerThan(V version) {
		return compareWith(version) > 0;
	}

	/**
	 * Is the current Minecraft version at equals or newer than the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean atLeast(V version) {
		return equals(version) || newerThan(version);
	}

	/*
	 * Compares two versions by the number
	 */
	private static int compareWith(V version) {
		try {
			return getCurrent().minorVersionNumber - version.minorVersionNumber;

		} catch (final Throwable t) {
			t.printStackTrace();

			return 0;
		}
	}

	/**
	 * Return the full version such as 1.20.6.
	 *
	 * @return
	 */
	public static String getFullVersion() {
		return getCurrent().toString() + (getSubversion() > 0 ? "." + getSubversion() : "");
	}

	/**
	 * Return the current Minecraft version.
	 *
	 * @return
	 */
	public static V getCurrent() {
		ValidCore.checkBoolean(current != null, "Call MinecraftVersion.setVersion() first before calling getCurrent() - or unsupported on this platform (Velocity doesnt support this)");

		return current;
	}

	/**
	 * Return the current Minecraft subversion.
	 *
	 * @return
	 */
	public static int getSubversion() {
		ValidCore.checkBoolean(subversion != -1, "Call MinecraftVersion.setVersion() first before calling getSubversion() - or unsupported on this platform (Velocity doesnt support this)");

		return subversion;
	}

	/**
	 * Return true if this server supports reporting Minecraft version.
	 *
	 * Bukkit = true, includes subversions
	 * Bungee = true, excludes subversions
	 * Velocity = false
	 *
	 * @return
	 */
	public static boolean hasVersion() {
		return current != null;
	}

	/**
	 * Set the current Minecraft version.
	 *
	 * @deprecated internal use only
	 * @param current
	 * @param subversion
	 */
	@Deprecated
	public static void setVersion(V current, int subversion) {
		ValidCore.checkBoolean(MinecraftVersion.current == null, "Version already set to " + MinecraftVersion.current);
		ValidCore.checkBoolean(MinecraftVersion.subversion == -1, "Subversion already set to " + MinecraftVersion.subversion);

		MinecraftVersion.current = current;
		MinecraftVersion.subversion = subversion;
	}
}