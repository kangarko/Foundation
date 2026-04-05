package org.mineacademy.fo;

import org.mineacademy.fo.exception.FoException;

import java.util.regex.Pattern;

/**
 * Represents the current Minecraft version the plugin is loaded on.
 */
public final class MinecraftVersion {

	public static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+\\.\\d+(?:\\.\\d+)?)");

	/**
	 * The wrapper representation of the version.
	 */
	private static V current = null;

	/**
	 * The subversion such as 8 in 1.8.8 or 8 in 1.21.8.
	 */
	private static int subversion = -1;

	/**
	 * The version wrapper.
	 */
	public enum V {
		v26_2(26, 2),
		v26_1(26, 1),
		v1_21(1, 21),
		v1_20(1, 20),
		v1_19(1, 19),
		v1_18(1, 18),
		v1_17(1, 17),
		v1_16(1, 16),
		v1_15(1, 15),
		v1_14(1, 14),
		v1_13(1, 13),
		v1_12(1, 12),
		v1_11(1, 11),
		v1_10(1, 10),
		v1_9(1, 9),
		v1_8(1, 8),
		v1_7(1, 7),
		v1_6(1, 6),
		v1_5(1, 5),
		v1_4(1, 4),
		v1_3_AND_BELOW(1, 3);

		/**
		 * The major version (the first part of the version number).
		 */
		private final int majorVersionNumber;

		/**
		 * The numeric version (the second part of the 1.x number).
		 */
		private final int minorVersionNumber;

		/**
		 * Creates new enum for a Minecraft version.
		 *
		 * @param majorVersionNumber
		 * @param minorVersionNumber
		 */
		V(int majorVersionNumber, int minorVersionNumber) {
			this.majorVersionNumber = majorVersionNumber;
			this.minorVersionNumber = minorVersionNumber;
		}

		/**
		 * Attempts to get the version from number.
		 *
		 * @param major
		 * @param minor
		 * @return
		 * @throws RuntimeException if number not found
		 * @deprecated internal use only
		 */
		@Deprecated
		public static V parse(final int major, final int minor) {
			for (final V v : values())
				if (v.majorVersionNumber == major && v.minorVersionNumber == minor) {
					return v;
				}

			throw new FoException("Invalid version number: " + major + "." + minor);
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return majorVersionNumber + "." + minorVersionNumber;
		}
	}

	/**
	 * Does the current Minecraft version equal the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean equals(final V version) {
		return compareWith(version) == 0;
	}

	/**
	 * Is the current Minecraft version older than the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean olderThan(final V version) {
		return compareWith(version) < 0;
	}

	/**
	 * Is the current Minecraft version newer than the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean newerThan(final V version) {
		return compareWith(version) > 0;
	}

	/**
	 * Is the current Minecraft version at equals or newer than the given version?
	 *
	 * @param version
	 * @return
	 */
	public static boolean atLeast(final V version) {
		return equals(version) || newerThan(version);
	}

	/*
	 * Compares two versions by the number
	 */
	private static int compareWith(final V version) {
		try {
			if (getCurrent().majorVersionNumber != version.majorVersionNumber) {
				return getCurrent().majorVersionNumber - version.majorVersionNumber;
			}
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
	 * <p>
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
	 * @param current
	 * @param subversion
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void setVersion(final V current, final int subversion) {
		if (MinecraftVersion.current != null)
			throw new FoException("Version already set to " + MinecraftVersion.current + " (avoid using plugin managers to reload this plugin as they are known to cause issues)", false);

		if (MinecraftVersion.subversion != -1)
			throw new FoException("Subversion already set to " + MinecraftVersion.subversion + " (avoid using plugin managers to reload this plugin as they are known to cause issues)", false);

		MinecraftVersion.current = current;
		MinecraftVersion.subversion = subversion;
	}
}