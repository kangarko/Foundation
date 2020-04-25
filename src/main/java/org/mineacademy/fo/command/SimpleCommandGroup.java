package org.mineacademy.fo.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.Getter;

/**
 * A command group contains a set of different subcommands
 * associated with the main command, for example: /arena join, /arena leave etc.
 */
public abstract class SimpleCommandGroup {

	/**
	 * The list of sub-commands belonging to this command tree, for example
	 * the /boss command has subcommands /boss region, /boss menu etc.
	 */
	protected final StrictList<SimpleSubCommand> subcommands = new StrictList<>();

	/**
	 * The registered main command, if any
	 */
	protected SimpleCommand mainCommand;

	/**
	 * Create a new command group, only allow extending classes
	 */
	protected SimpleCommandGroup() {
	}

	// ----------------------------------------------------------------------
	// Main functions
	// ----------------------------------------------------------------------

	/**
	 * Register this command group into Bukkit and start using it
	 *
	 * @param labelAndAliases
	 */
	public final void register(StrictList<String> labelAndAliases) {
		register(labelAndAliases.get(0), (labelAndAliases.size() > 1 ? labelAndAliases.range(1) : new StrictList<String>()).getSource());
	}

	/**
	 * Register this command group into Bukkit and start using it
	 *
	 * @param label
	 * @param aliases
	 */
	public final void register(String label, List<String> aliases) {
		Valid.checkBoolean(!isRegistered(), "Main command already registered as: " + mainCommand);

		mainCommand = new MainCommand(label);

		if (aliases != null)
			mainCommand.setAliases(aliases);

		mainCommand.register();

		registerSubcommands();
	}

	/**
	 * Remove this command group from Bukkit. Takes immediate changes in the game.
	 */
	public final void unregister() {
		Valid.checkBoolean(isRegistered(), "Main command not registered!");

		mainCommand.unregister();
		mainCommand = null;

		subcommands.clear();
	}

	/**
	 * Has the command group been registered yet?
	 *
	 * @return
	 */
	public final boolean isRegistered() {
		return mainCommand != null;
	}

	/**
	 * Extending method to register subcommands, call
	 * {@link #registerSubcommand(SimpleSubCommand)} and {@link #registerHelpLine(String...)}
	 * there for your command group.
	 */
	protected abstract void registerSubcommands();

	/**
	 * Registers a new subcommand for this group
	 *
	 * @param command
	 */
	protected final void registerSubcommand(SimpleSubCommand command) {
		Valid.checkNotNull(mainCommand, "Cannot add subcommands when main command is missing! Call register()");
		Valid.checkBoolean(!subcommands.contains(command), "Subcommand /" + mainCommand.getLabel() + " " + command.getSublabel() + " already registered!");

		subcommands.add(command);
	}

	/**
	 * Registers a simple help message for this group, used in /{label} help|?
	 * since we add help for all subcommands automatically
	 *
	 * @param menuHelp
	 */
	protected final void registerHelpLine(String... menuHelp) {
		Valid.checkNotNull(mainCommand, "Cannot add subcommands when main command is missing! Call register()");

		subcommands.add(new FillerSubCommand(this, menuHelp));
	}

	// ----------------------------------------------------------------------
	// Shortcuts
	// ----------------------------------------------------------------------

	/**
	 * Get the label for this command group, failing if not yet registered
	 *
	 * @return
	 */
	public final String getLabel() {
		Valid.checkBoolean(isRegistered(), "Main command has not yet been set!");

		return mainCommand.getMainLabel();
	}

	// ----------------------------------------------------------------------
	// Functions
	// ----------------------------------------------------------------------

	/**
	 * Return the message displayed when no parameter is given, by
	 * default we give credits
	 *
	 * If you specify "author" in your plugin.yml we display author information
	 * If you override {@link SimplePlugin#getFoundedYear()} we display copyright
	 *
	 * @param sender the command sender that requested this to be shown to him
	 * 				 may be null
	 *
	 * @return
	 */
	protected String[] getNoParamsHeader(CommandSender sender) {
		final int foundedYear = SimplePlugin.getInstance().getFoundedYear();
		final int yearNow = Calendar.getInstance().get(Calendar.YEAR);

		final List<String> messages = new ArrayList<>();

		messages.add("&8" + Common.chatLine());
		messages.add(getHeaderPrefix() + "  " + SimplePlugin.getNamed() + getTrademark() + " &7" + SimplePlugin.getVersion());
		messages.add(" ");

		{
			final String authors = String.join(", ", SimplePlugin.getInstance().getDescription().getAuthors());

			if (!authors.isEmpty())
				messages.add("   &7Made by &f" + authors + (foundedYear != -1 ? " &7\u00A9 " + foundedYear + (yearNow != foundedYear ? " - " + yearNow : "") : ""));
		}

		{
			final String credits = getCredits();

			if (credits != null && !credits.isEmpty())
				messages.add("   " + credits);
		}

		messages.add("&8" + Common.chatLine());

		return messages.toArray(new String[messages.size()]);
	}

	/**
	 * Should we send command helps instead of no-param header?
	 * @return
	 */

	protected boolean sendHelpIfNoArgs() {
		return false;
	}

	// Return the TM symbol in case we have it for kangarko's plugins
	private final String getTrademark() {
		return SimplePlugin.getInstance().getDescription().getAuthors().contains("kangarko") ? getHeaderPrefix() + "&8\u2122" : "";
	}

	/**
	 * Get a part of the {@link #getNoParamsHeader()} typically showing
	 * your website where the user can find more information about this command
	 * or your plugin in general
	 *
	 * @return
	 */
	protected String getCredits() {
		return "&7Visit &fmineacademy.org &7for more information.";
	}

	/**
	 * Return which subcommands should trigger the automatic help
	 * menu that shows all subcommands sender has permission for.
	 *
	 * Also see {@link #getHelpHeader()}
	 *
	 * Default: help and ?
	 *
	 * @return
	 */
	protected List<String> getHelpLabel() {
		return Arrays.asList("help", "?");
	}

	/**
	 * Return the header messages used in /{label} help|? typically
	 * used to tell all available subcommands from this command group
	 *
	 * @return
	 */
	protected String[] getHelpHeader() {
		return new String[] {
				"&8",
				"&8" + Common.chatLine(),
				getHeaderPrefix() + "  " + SimplePlugin.getNamed() + getTrademark() + " &7" + SimplePlugin.getVersion(),
				" ",
				"&2  [] &f= optional arguments",
				"&6  <> &f= required arguments",
				" "
		};
	}

	/**
	 * Return the default color in the {@link #getHelpHeader()},
	 * GOLD + BOLD colors by default
	 *
	 * @return
	 */
	protected String getHeaderPrefix() {
		return "" + ChatColor.GOLD + ChatColor.BOLD;
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * The main command handling this command group
	 */
	private final class MainCommand extends SimpleCommand {

		/**
		 * Create new main command with the given label
		 *
		 * @param label
		 */
		private MainCommand(String label) {
			super(label);

			// Let everyone view credits of this command when they run it without any sublabels
			setPermission(null);

			// We handle help ourselves
			setAutoHandleHelp(false);
		}

		/**
		 * Handle this command group, print a special message when no arguments are given,
		 * execute subcommands, handle help or ? argument and more.
		 */
		@Override
		protected void onCommand() {

			// Print a special message on no arguments
			if (args.length == 0) {
				if (sendHelpIfNoArgs())
					tellSubcommandsHelp();
				else
					tell(getNoParamsHeader(sender));
				return;
			}

			final String argument = args[0];
			final SimpleSubCommand command = findSubcommand(argument);

			// Handle subcommands
			if (command != null) {

				// Simulate our main label
				command.setSublabel(args[0]);

				// Run the command
				command.execute(sender, getLabel(), args.length == 1 ? new String[] {} : Arrays.copyOfRange(args, 1, args.length));
			}

			// Handle help argument
			else if (!getHelpLabel().isEmpty() && Valid.isInList(argument, getHelpLabel()))
				tellSubcommandsHelp();

			// Handle unknown argument
			else
				returnInvalidArgs();
		}

		/**
		 * Automatically tells all help for all subcommands
		 */
		private void tellSubcommandsHelp() {
			tell(getHelpHeader());

			for (final SimpleSubCommand subcommand : subcommands)
				if (subcommand.showInHelp() && hasPerm(subcommand.getPermission())) {
					if (subcommand instanceof FillerSubCommand) {
						tellNoPrefix(((FillerSubCommand) subcommand).getHelpMessages());

						continue;
					}

					final String usage = colorizeUsage(subcommand.getUsage());
					final String desc = Common.getOrEmpty(subcommand.getDescription());

					tellNoPrefix(" &f/" + getLabel() + " " + subcommand.getSublabels()[0] + (!usage.startsWith("/") ? " " + usage : "") + (!desc.isEmpty() ? " &e- " + desc : ""));
				}
		}

		/**
		 * Replaces some usage parameters such as <> or [] with colorized brackets
		 *
		 * @param message
		 * @return
		 */
		private String colorizeUsage(String message) {
			return message == null ? "" : message.replace("<", "&6<").replace(">", "&6>&f").replace("[", "&2[").replace("]", "&2]&f");
		}

		/**
		 * Finds a subcommand by label
		 *
		 * @param label
		 * @return
		 */
		private SimpleSubCommand findSubcommand(String label) {
			for (final SimpleSubCommand command : subcommands) {
				if (command instanceof FillerSubCommand)
					continue;

				for (final String alias : command.getSublabels())
					if (alias.equalsIgnoreCase(label))
						return command;
			}

			return null;
		}

		/**
		 * Handle tabcomplete for subcommands and their tabcomplete
		 */
		@Override
		public List<String> tabComplete() {
			if (args.length == 1)
				return tabCompleteSubcommands(sender, args[0]);

			if (args.length > 1) {
				final SimpleSubCommand cmd = findSubcommand(args[0]);

				if (cmd != null)
					return cmd.tabComplete(sender, getLabel(), Arrays.copyOfRange(args, 1, args.length));
			}

			return null;
		}

		/**
		 * Automatically tab-complete subcommands
		 *
		 * @param sender
		 * @param param
		 * @return
		 */
		private List<String> tabCompleteSubcommands(CommandSender sender, String param) {
			param = param.toLowerCase();

			final List<String> tab = new ArrayList<>();

			for (final SimpleSubCommand subcommand : subcommands)
				if (!(subcommand instanceof FillerSubCommand) && hasPerm(subcommand.getPermission()))
					for (final String label : subcommand.getSublabels())
						if (!label.trim().isEmpty() && label.startsWith(param))
							tab.add(label);

			return tab;
		}
	}

	// ----------------------------------------------------------------------
	// Helper
	// ----------------------------------------------------------------------

	/**
	 * A helper class for showing plain messages in /{label} help|?
	 */
	private final class FillerSubCommand extends SimpleSubCommand {

		@Getter
		private final String[] helpMessages;

		private FillerSubCommand(SimpleCommandGroup parent, String... menuHelp) {
			super(parent, "_" + RandomUtil.nextBetween(1, Short.MAX_VALUE));

			this.helpMessages = menuHelp;
		}

		@Override
		protected void onCommand() {
			throw new FoException("Filler space command cannot be run!");
		}
	}

}