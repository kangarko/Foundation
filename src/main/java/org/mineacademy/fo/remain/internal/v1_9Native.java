package org.mineacademy.fo.remain.internal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * Represents the native boss bar api for Minecraft 1.9 and newer
 */
class v1_9Native extends EnderDragonEntity {

	private final BossBar bar;

	public v1_9Native(String name, Location loc) {
		super(name, loc);

		bar = Bukkit.createBossBar(name, BarColor.PINK, BarStyle.SOLID);
	}

	public final void removePlayer(Player player) {
		getBar().removePlayer(player);
	}

	public final void addPlayer(Player player) {
		getBar().addPlayer(player);
	}

	public final void setProgress(double progress) {
		getBar().setProgress(progress);
	}

	private BossBar getBar() {
		if (barColor != null)
			bar.setColor(BarColor.valueOf(barColor.toString()));

		if (barStyle != null)
			bar.setStyle(BarStyle.valueOf(barStyle.toString()));

		return bar;
	}

	@Override
	public Object getSpawnPacket() {
		return null;
	}

	@Override
	public Object getDestroyPacket() {
		return null;
	}

	@Override
	public Object getMetaPacket(Object watcher) {
		return null;
	}

	@Override
	public Object getTeleportPacket(Location loc) {
		return null;
	}

	@Override
	public Object getWatcher() {
		return null;
	}
}
