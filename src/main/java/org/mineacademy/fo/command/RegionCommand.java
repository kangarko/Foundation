package org.mineacademy.fo.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.RegionMenu;
import org.mineacademy.fo.menu.SelectRegionMenu;
import org.mineacademy.fo.menu.tool.RegionTool;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.RequiredArgsConstructor;

/**
 * The command to manage plugin's region system.
 *
 * To use this, enable regions in {@link SimplePlugin#areRegionsEnabled()}
 * and register this subcommand manually in your class extending {@link SimpleCommandGroup}.
 */
public class RegionCommand extends SimpleSubCommand {

	public RegionCommand() {
		super("region|rg");

		this.setDescription("Create regions.");
		this.setUsage("<params ...>");
		this.setMinArguments(1);
	}

	@Override
	protected boolean showInHelp() {
		return SimplePlugin.getInstance().areRegionsEnabled();
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#getMultilineUsageMessage()
	 */
	@Override
	protected String[] getMultilineUsageMessage() {
		return Param.generateUsages();
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommand#onCommand()
	 */
	@Override
	protected void onCommand() {

		final String regionName = this.args.length > 1 ? this.args[1] : null;
		final DiskRegion region = regionName != null ? DiskRegion.findRegion(regionName) : null;
		final Param param = Param.find(this.args[0]);
		this.checkNotNull(param, "No such param '{0}'. Available: " + Common.join(Param.values()));

		//
		// Commands without a region.
		//
		if (param == Param.LIST) {
			final Collection<DiskRegion> regions = DiskRegion.getRegions();

			if (regions.isEmpty())
				this.returnTell("There are no regions created yet.");

			final List<SimpleComponent> components = new ArrayList<>();

			for (final DiskRegion otherRegion : DiskRegion.getRegions()) {

				final String longestText = "&7Secondary: &2" + Common.shortLocation(otherRegion.getSecondary());

				components.add(SimpleComponent
						.of(" ")

						.append("&8[&4X&8]")
						.onHover("&7Click to remove permanently.")
						.onClickRunCmd("/" + this.getLabel() + " " + this.getSublabel() + " " + Param.REMOVE + " " + otherRegion.getName() + " -list")

						.append(" ")

						.append("&8[&2?&8]")
						.onHover("&7Click to visualize.")
						.onClickRunCmd("/" + this.getLabel() + " " + this.getSublabel() + " " + Param.VIEW + " " + otherRegion.getName() + " -list")

						.append(" ")

						.append("&8[&3>&8]")
						.onHover("&7Click to teleport to the center.")
						.onClickRunCmd("/" + this.getLabel() + " " + this.getSublabel() + " " + Param.TELEPORT + " " + otherRegion.getName() + " -list")

						.append(" ")

						.append("&7" + otherRegion.getName())
						.onHover(ChatUtil.center("&fRegion Information", longestText.length() * 2 + longestText.length() / 3),
								"&7Primary: &2" + Common.shortLocation(otherRegion.getPrimary()),
								longestText,
								"&7Size: &2" + otherRegion.getBlocks().size() + " blocks"));
			}

			new ChatPaginator(ChatColor.DARK_RED)
					.setFoundationHeader("Listing " + Common.plural(regions.size(), "Region"))
					.setPages(components)
					.send(this.sender);

			return;
		}

		else if (param == Param.NEW) {
			this.checkConsole();

			this.checkArgs(2, "Please specify the region name.");
			this.checkBoolean(region == null, "Region '" + regionName + "' already exists.");

			final VisualizedRegion createdRegion = DiskRegion.getCreatedRegion(this.getPlayer());

			if (!createdRegion.isWhole()) {
				RegionTool.getInstance().giveIfHasnt(this.getPlayer());

				this.tellError("You must select a region first using the region tool which was given to you.");
				return;
			}

			DiskRegion.createRegion(regionName, createdRegion);

			this.tellSuccess("Region '" + regionName + "' has been created.");
			return;

		} else if (param == Param.TOOL) {
			this.checkConsole();

			RegionTool.getInstance().giveIfHasnt(this.getPlayer());
			this.tellSuccess("You were given a region tool. Left click to set primary, right click to set secondary.");

			return;

		} else if (param == Param.PRIMARY || param == Param.SECONDARY) {
			this.checkConsole();

			final VisualizedRegion createdRegion = DiskRegion.getCreatedRegion(getPlayer());
			final Location location = this.getPlayer().getLocation();

			if (param == Param.PRIMARY)
				createdRegion.setPrimary(location);
			else
				createdRegion.setSecondary(location);

			this.tellSuccess("Set the " + (param == Param.PRIMARY ? "primary" : "secondary") + " region point."
					+ (createdRegion.isWhole() ? " Use /{label} {sublabel} new <name> to create the region." : ""));

			return;
		}

		else if (param == Param.MENU) {
			this.checkConsole();

			if (this.args.length > 1) {
				this.checkNotNull(region, "&cRegion '" + regionName + "' doesn't exists.");

				RegionMenu.showTo(this.getPlayer(), region);
			} else
				SelectRegionMenu.create().displayTo(this.getPlayer());

			return;
		}

		// Require region name for all params below, except view, but when it is provided, check it
		if (param != Param.VIEW || (param == Param.VIEW && regionName != null)) {
			this.checkNotNull(regionName, "&cPlease specify the region name.");
			this.checkNotNull(region, "&cRegion '" + regionName + "' doesn't exists.");
		}

		if (param == Param.REMOVE) {
			DiskRegion.removeRegion(region);

			this.tellAndList(region, "&7Removed region named '&2" + regionName + "&7'");
		}

		else if (param == Param.VIEW) {
			this.checkConsole();

			if (region != null) {
				region.visualize(this.getPlayer());

				this.tellAndList(region, "&7Region '&2" + regionName + "&7' is being visualized for 10 seconds.");
			}

			else {
				final Location playerLocation = this.getPlayer().getLocation();
				int count = 0;

				for (final DiskRegion otherRegion : DiskRegion.getRegions())
					if (otherRegion.getCenter().distance(playerLocation) < 100) {
						otherRegion.visualize(this.getPlayer(), RandomUtil.nextColor());

						count++;
					}

				this.tellSuccess("&7Visualized " + Common.plural(count, "region") + " nearby for 10 seconds.");
			}

		}

		else if (param == Param.TELEPORT) {
			region.teleportToCenter(this.getPlayer());

			this.tellAndList(region, "&7Teleported to the center of region '&2" + regionName + "&7'");
		}

		else
			throw new FoException("Unhandled param " + param);
	}

	/*
	 * Util method to show the given region using the given message.
	 *
	 * We automatically will invoke /{label} region list before showing this message.
	 */
	private void tellAndList(DiskRegion region, final String message) {

		if (this.isPlayer() && this.args.length > 2 && "-list".equals(this.args[2]))
			this.getPlayer().performCommand(this.getLabel() + " " + this.getSublabel() + " " + Param.LIST);

		SimpleComponent
				.of(Messenger.getInfoPrefix() + message)

				.append(" Click here to open its menu.")
				.onHover("&7Click to open region menu.")
				.onClickRunCmd("/" + SimpleSettings.MAIN_COMMAND_ALIASES.get(0) + " region menu " + region.getName())

				.send(this.sender);
	}

	@Override
	public List<String> tabComplete() {

		final Param param = this.args.length > 0 ? Param.find(this.args[0]) : null;

		switch (this.args.length) {
			case 1:
				return this.completeLastWord(Param.values());

			case 2:
				return param == Param.LIST || param == Param.NEW ? NO_COMPLETE : this.completeLastWord(DiskRegion.getRegionNames());
		}

		return NO_COMPLETE;
	}

	/**
	 * The parameter for this command
	 */
	@RequiredArgsConstructor
	private enum Param {

		/**
		 * Create a new region.
		 */
		NEW("new", "n", "<name>", "Create a new region."),

		/**
		 * Get the region creation tool.
		 */
		TOOL("tool", "t", "", "Get the region creation tool."),

		/**
		 * Remove a region.
		 */
		REMOVE("remove", "rm", "<name>", "Delete a region."),

		/**
		 * Show particles around region.
		 */
		VIEW("view", "v", "[name]", "Visualize region border if center is < 100 blocks from you."),

		/**
		 * Teleport to a region.
		 */
		TELEPORT("teleport", "tp", "<name>", "Teleport to a region's center."),

		/**
		 * Show region menu.
		 */
		MENU("menu", "m", "[name]", "Show region menu."),

		/**
		 * List installed regions.
		 */
		LIST("list", "l", "", "Browse available regions."),

		/**
		 * Set created region's primary location.
		 */
		PRIMARY("primary", "p", "", "Set your feet to created region's primary location."),

		/**
		 * Set created region's secondary location.
		 */
		SECONDARY("secondary", "s", "", "Set your feet to to created region's secondary location.");

		/**
		 * The label for this command arg.
		 */
		private final String label;

		/**
		 * The sublabels for this command arg.
		 */
		private final String alias;

		/**
		 * The param's usage.
		 */
		private final String usage;

		/**
		 * The param's description.
		 */
		private final String description;

		/**
		 * Return a parameter from the string, or null.
		 *
		 * @param argument
		 * @return
		 */
		@Nullable
		private static Param find(String argument) {
			argument = argument.toLowerCase();

			for (final Param param : values()) {
				if (param.label.toLowerCase().equals(argument))
					return param;

				if (param.alias != null && param.alias.toLowerCase().equals(argument))
					return param;
			}

			return null;
		}

		/**
		 * Generate the usages for all params
		 *
		 * @return
		 */
		public static String[] generateUsages() {
			final List<String> messages = new ArrayList<>();

			for (final Param param : Param.values()) {
				// Format usage. Replace [] with &2 and <> with &6
				String usage = param.usage;

				if (!usage.isEmpty()) {
					usage = usage.replace("[", "&2[");
					usage = usage.replace("]", "]");
					usage = usage.replace("<", "&6<");
					usage = usage.replace(">", ">");
					usage = usage + " ";
				}

				messages.add("&c /{label} {sublabel} " + param.label + " " + usage + "&c- " + param.description);
			}

			return Common.toArray(messages);
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return this.label;
		}
	}
}