package org.mineacademy.fo.remain.nbt;

final class CheckUtil {

	private CheckUtil() {
		// util
	}

	public static void assertAvailable(final MinecraftVersion version) {
		if (!MinecraftVersion.isAtLeastVersion(version))
			throw new NbtApiException(
					"This Method is only avaliable for the version " + version.name() + " and above!");
	}

}
