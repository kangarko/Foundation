package org.mineacademy.fo.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.platform.Platform;

import net.kyori.adventure.bossbar.BossBar;

/**
 * An internal command to test Audience-related features, needs
 * to be manually registered.
 */
public final class TestAudienceCommand extends SimpleCommandCore {

	/**
	 * Holds temporary boss bars for players, clears on reload
	 */
	private final Map<UUID, BossBar> bossBars = new HashMap<>();

	/**
	 * Create a new instance of this command
	 */
	public TestAudienceCommand() {
		super(getPlatformCommandLabel());

		this.setPermission(null);
		this.setUsage("<" + CommonCore.join(Param.values()) + ">");
		this.setMinArguments(1);
	}

	/*
	 * Find the appropriate platform-specific command label
	 */
	private static String getPlatformCommandLabel() {
		final Platform.Type type = Platform.getType();

		switch (type) {
			case BUKKIT:
				return "bukkit-testaudience";
			case BUNGEECORD:
				return "bungee-testaudience";
			case VELOCITY:
				return "velocity-testaudience";
			default:
				throw new FoException("Unknown platform " + type);
		}
	}

	@Override
	protected void onCommand() {
		this.checkConsole();

		if ("all".equals(this.args[0])) {
			for (final Param param : Param.values())
				if (param != Param.KICK)
					this.audience.dispatchCommand(this.getLabel() + " " + param);

			return;
		}

		final Param param = this.findEnum(Param.class, this.args[0]);
		this.checkBoolean(param.isSupported(), "This platform (" + Platform.getType() + ") does not support parameter '" + param + "'. Required platforms: " + CommonCore.join(param.supportedPlatforms));

		if (param == Param.WARN)
			this.audience.dispatchCommand("@warn Hello!");

		else if (param == Param.SPOOF_CHAT)
			this.audience.chat("Hello this is a &cspoofed <red>message!");

		else if (param == Param.SPOOF_COMMAND)
			this.audience.dispatchCommand("say Hello &eCommand");

		else if (param == Param.IP)
			this.audience.sendPlainMessage("Your IP is " + this.audience.getAddress());

		else if (param == Param.NAME)
			this.audience.sendPlainMessage("Your name is " + this.audience.getName());

		else if (param == Param.SERVER)
			this.audience.sendPlainMessage("You are on server " + this.audience.getServer().getName());

		else if (param == Param.HAS_HEX)
			this.audience.sendPlainMessage("Do you have HEX color support? " + this.audience.hasHexColorSupport());

		else if (param == Param.BOSSBAR) {
			this.checkBoolean(!this.bossBars.containsKey(this.audience.getUniqueId()), "You already have a boss bar!");

			final BossBar bar = this.audience.showBossBar("<#468eed>Hello From &7Foundation", 0.5F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
			this.bossBars.put(this.audience.getUniqueId(), bar);

		} else if (param == Param.BOSSBAR_TIMED) {
			final int time = RandomUtil.nextIntBetween(2, 5);

			this.audience.showBossbarTimed("<green>This BossBar disappears in &b" + time + " seconds", time, 1F, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);

		} else if (param == Param.HIDEBOSSBAR) {
			this.checkBoolean(this.bossBars.containsKey(this.audience.getUniqueId()), "You don't have a boss bar to hide!");

			this.audience.hideBossBar(this.bossBars.remove(this.audience.getUniqueId()));

		} else if (param == Param.HIDEBOSSBARS) {
			this.audience.hideBossBars();
			this.bossBars.remove(this.audience.getUniqueId());

		} else if (param == Param.KICK)
			this.audience.kick("<#b8b8c6>You have &6been kicked!");

		else if (param == Param.BOOK)
			this.audience.openBook("<#b8b8c6>Hello &9Book", "Homeboy Jeffrey", "<red>First &9page is <hover:show_text:'Hello!'>hover me\nto uncover what", "Second is even cooler");

		else if (param == Param.TITLE)
			this.audience.showTitle("<#b8b8c6>Hello &eTitle", "<red>Subtitle &r&land bold");

		else if (param == Param.RESETTITLE)
			this.audience.resetTitle();

		else if (param == Param.ACTIONBAR)
			this.audience.sendActionBar("<red>Hello &eActionbar");

		else if (param == Param.JSON)
			this.audience.sendJson("{\"text\":\"Hey there hover me\",\"color\":\"#b8b8c6\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Hey drr\"]}}");

		else if (param == Param.TABLIST)
			this.audience.sendPlayerListHeaderAndFooter("<red>Header\nmulti&eline", "<green>Footer\nmulti&eline");

		else if (param == Param.TOAST)
			this.audience.sendToast("<red>This is an alert\n&7Toasts are cool");

		CommonCore.log("Tested " + param + " for " + this.audience.getName() + " - something should have happened in-game, if not, this is a bug!");
	}

	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(Param.values()) : null;
	}

	/**
	 * The different commands to test things
	 */
	enum Param {
		ACTIONBAR,
		BOOK(Platform.Type.BUKKIT),
		HAS_HEX,
		HIDEBOSSBARS,
		HIDEBOSSBAR,
		IP,
		JSON,
		KICK,
		NAME,
		RESETTITLE,
		SERVER,
		BOSSBAR,
		BOSSBAR_TIMED,
		TABLIST,
		TITLE,
		TOAST(Platform.Type.BUKKIT),
		SPOOF_CHAT,
		SPOOF_COMMAND,
		WARN;

		private final List<Platform.Type> supportedPlatforms;

		Param(Platform.Type... supportedPlatforms) {
			this.supportedPlatforms = Arrays.asList(supportedPlatforms);
		}

		public final boolean isSupported() {
			return this.supportedPlatforms.isEmpty() || this.supportedPlatforms.contains(Platform.getType());
		}

		@Override
		public String toString() {
			return this.name().toLowerCase();
		}
	}
}
