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
	 * The subversion such as 8 in 1.8.8 or 1 in 26.1.2.
	 */
	private static int subversion = -1;

	/**
	 * The version wrapper.
	 *
	 * All entries use uniform encoding: major * 100 + minor.
	 * Example: 1.21 = 121, 1.8 = 108, 26.1 = 2601.
	 */
	public enum V {
		v26_1(2601),
		v1_21(121),
		v1_20(120),
		v1_19(119),
		v1_18(118),
		v1_17(117),
		v1_16(116),
		v1_15(115),
		v1_14(114),
		v1_13(113),
		v1_12(112),
		v1_11(111),
		v1_10(110),
		v1_9(109),
		v1_8(108),
		v1_7(107),
		v1_6(106),
		v1_5(105),
		v1_4(104),
		v1_3_AND_BELOW(103);

		private final int versionNumber;

		V(final int version) {
			this.versionNumber = version;
		}

		/**
		 * Attempts to get the version from the encoded version number (major * 100 + minor).
		 *
		 * @deprecated internal use only
		 * @param number
		 * @return
		 * @throws RuntimeException if number not found
		 */
		@Deprecated
		public static V parse(final int number) {
			for (final V v : values())
				if (v.versionNumber == number)
					return v;

			if (number > values()[0].versionNumber)
				return values()[0];

			throw new FoException("Invalid version number: " + number);
		}

		@Override
		public String toString() {
			return this.versionNumber / 100 + "." + this.versionNumber % 100;
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
			return getCurrent().versionNumber - version.versionNumber;

		} catch (final Throwable t) {
			t.printStackTrace();

			return 0;
		}
	}

	/**
	 * Return the full version such as 1.20.6 or 26.1.1.
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
		Valid.checkBoolean(current != null, "Call MinecraftVersion.setVersion() first before calling getCurrent() - or unsupported on this platform (Velocity doesnt support this)");

		return current;
	}

	/**
	 * Return the current Minecraft subversion.
	 *
	 * @return
	 */
	public static int getSubversion() {
		Valid.checkBoolean(subversion != -1, "Call MinecraftVersion.setVersion() first before calling getSubversion() - or unsupported on this platform (Velocity doesnt support this)");

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
	 * Parse a raw version string (e.g. "1.21.1-R0.1-SNAPSHOT" or "26.1.1.build.29-alpha")
	 * and set the current version.
	 *
	 * @deprecated internal use only
	 * @param rawVersionString
	 */
	@Deprecated
	public static void parseAndSet(final String rawVersionString) {
		final String afterDash = rawVersionString.split("\\-")[0];
		final String[] allParts = afterDash.split("\\.");

		int numericCount = 0;

		for (final String part : allParts)
			try {
				Integer.parseInt(part);
				numericCount++;

			} catch (final NumberFormatException e) {
				break;
			}

		Valid.checkBoolean(numericCount == 2 || numericCount == 3, "Cannot read version '" + rawVersionString + "', expected 2-3 numeric version parts");

		final int major = Integer.parseInt(allParts[0]);
		final int minor = Integer.parseInt(allParts[1]);
		final int versionNumber = major * 100 + minor;

		final V resolved = versionNumber <= V.v1_3_AND_BELOW.versionNumber ? V.v1_3_AND_BELOW : V.parse(versionNumber);
		final int sub = numericCount == 3 ? Integer.parseInt(allParts[2]) : 0;

		setVersion(resolved, sub);
	}

	/**
	 * Set the current Minecraft version.
	 *
	 * @deprecated internal use only
	 * @param current
	 * @param subversion
	 */
	@Deprecated
	public static void setVersion(final V current, final int subversion) {
		if (MinecraftVersion.current != null)
			throw new FoException("Version already set to " + MinecraftVersion.current + " (avoid using plugin managers to reload this plugin as they are known to cause issues)");

		if (MinecraftVersion.subversion != -1)
			throw new FoException("Subversion already set to " + MinecraftVersion.subversion + " (avoid using plugin managers to reload this plugin as they are known to cause issues)");

		MinecraftVersion.current = current;
		MinecraftVersion.subversion = subversion;
	}
}