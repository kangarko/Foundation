package org.mineacademy.fo.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.platform.Platform.Type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
final class BossBarTask implements Runnable {

	@Getter
	private static final BossBarTask instance = new BossBarTask();

	private final Map<UUID, List<TimedBar>> playerBars = new HashMap<>();

	@Override
	public void run() {

		// It takes around 1 second to remove the wither
		final boolean legacyPlatform = Platform.getType() == Type.BUKKIT && MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(V.v1_9);

		synchronized (this.playerBars) {
			for (final Iterator<Map.Entry<UUID, List<TimedBar>>> it = this.playerBars.entrySet().iterator(); it.hasNext();) {
				final Entry<UUID, List<TimedBar>> entry = it.next();

				final UUID audienceUid = entry.getKey();

				final FoundationPlayer audience = Platform.getPlayer(audienceUid);
				final List<TimedBar> timedBars = entry.getValue();

				if (audience == null || !audience.isPlayerOnline()) {
					it.remove();

					continue;
				}

				for (final Iterator<TimedBar> barIt = timedBars.iterator(); barIt.hasNext();) {
					final TimedBar timedBar = barIt.next();

					if (timedBar.isTimed()) {
						final float newProgress = timedBar.getBar().progress() - 1F / (timedBar.getSecondsToShow() - (legacyPlatform ? 1 : 0));

						if (newProgress <= 0) {

							if (legacyPlatform) {
								timedBar.getBar().progress(0.0F);

								audience.showBossBar0(timedBar);
							}

							audience.hideBossBar0(timedBar);

							barIt.remove();

						} else {
							timedBar.getBar().progress(newProgress);

							audience.showBossBar0(timedBar);
						}
					}
				}

				if (timedBars.isEmpty())
					it.remove();
			}
		}
	}

	void show(final FoundationPlayer audience, final TimedBar bar) {
		synchronized (this.playerBars) {
			this.playerBars.computeIfAbsent(audience.getUniqueId(), key -> new ArrayList<>()).add(bar);

			audience.showBossBar0(bar);
		}
	}

	void hide(final FoundationPlayer audience, final BossBar bar) {
		synchronized (this.playerBars) {
			final List<TimedBar> bars = this.playerBars.get(audience.getUniqueId());

			if (bars != null) {
				for (final Iterator<TimedBar> it = bars.iterator(); it.hasNext();) {
					final TimedBar timedBar = it.next();

					if (timedBar.equals(bar)) {
						audience.hideBossBar0(timedBar);

						it.remove();
					}
				}

				if (bars.isEmpty())
					this.playerBars.remove(audience.getUniqueId());
			}
		}
	}

	void hideAll(final FoundationPlayer player) {
		synchronized (this.playerBars) {
			final List<TimedBar> bars = this.playerBars.remove(player.getUniqueId());

			if (bars != null)
				for (final TimedBar bar : bars)
					player.hideBossBar0(bar);
		}
	}

	@Getter
	@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
	final static class TimedBar {

		private final UUID uniqueId;
		private final BossBar bar;
		private final int secondsToShow;

		public boolean equals(final BossBar bar) {
			return this.bar.equals(bar);
		}

		@Override
		public String toString() {
			return LegacyComponentSerializer.legacySection().serialize(this.bar.name());
		}

		boolean isTimed() {
			return this.secondsToShow != -1;
		}

		static TimedBar permanent(final BossBar bar) {
			return new TimedBar(UUID.randomUUID(), bar, -1);
		}

		static TimedBar timed(final BossBar bar, final int secondsToShow) {
			return new TimedBar(UUID.randomUUID(), bar, secondsToShow);
		}
	}
}