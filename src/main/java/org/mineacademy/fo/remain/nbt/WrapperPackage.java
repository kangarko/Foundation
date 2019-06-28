package org.mineacademy.fo.remain.nbt;

/**
 * Package enum
 *
 * @author tr7zw
 *
 */
enum WrapperPackage {
	NMS("net.minecraft.server"),
	CRAFTBUKKIT("org.bukkit.craftbukkit"),
	;

	private final String uri;

	private WrapperPackage(String uri) {
		this.uri = uri;
	}

	/**
	 * @return The Uri for that package
	 */
	public String getUri() {
		return uri;
	}

}
