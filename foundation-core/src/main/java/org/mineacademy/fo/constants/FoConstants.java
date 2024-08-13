package org.mineacademy.fo.constants;

import java.util.UUID;

import org.mineacademy.fo.platform.Platform;

/**
 * Stores constants for this plugin
 */
public final class FoConstants {

	/**
	 * Represents a UUID consisting of 0's only
	 */
	public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	public static final class File {

		/**
		 * The data file (uses YAML) for saving various data
		 */
		public static final String DATA = "data.db";
	}

	public static final class NBT {

		/**
		 * An internal metadata tag the player gets when he opens a sign on legacy Minecraft version.
		 *
		 * We use this in the sign update packet listener to handle sign updating.
		 */
		public static final String METADATA_OPENED_SIGN = Platform.getPluginName() + "_OpenedSign";
	}
}
