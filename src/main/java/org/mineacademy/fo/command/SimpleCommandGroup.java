package org.mineacademy.fo.command;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;

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
	private final StrictList<SimpleSubCommand> subcommands = new StrictList<>();

	/**
	 * The registered main command, if any
	 */
	private SimpleCommand mainCommand;

	/**
	 * The label to execute subcommands in this group, example: for ChatControl it's /chatcontrol
	 */
	@Getter
	private String label;

	/**
	 * What other commands trigger this command group? Example: for ChatControl it's /chc and /chatc
	 */
	@Getter
	private List<String> aliases;

	/**
	 * The temporary sender that is currently about to see the command group, mostly used in
	 * compiling info messages such as in {@link #getNoParamsHeader()}
	 */
	@Nullable
	@Getter
	protected CommandSender sender;

	/**
	 * Create a new simple command group using {@link SimpleSettings#MAIN_COMMAND_ALIASES}
	 */
	protected SimpleCommandGroup() {
		this(findMainCommandAliases());
	}

	/*
	 * A helper method to aid developers implement this command properly.
	 */
	private static StrictList<String> findMainCommandAliases() {
		final StrictList<String> aliases = SimpleSettings.MAIN_COMMAND_ALIASES;

		Valid.checkBoolean(!aliases.isEmpty(), "Called SimpleCommandGroup with no args constructor which uses SimpleSettings' MAIN_COMMAND_ALIASES field WHICH WAS EMPTY."
				+ " To make this work, make a settings class extending SimpleSettings and write 'Command_Aliases: [/yourmaincommand]' key-value pair with a list of aliases to your settings.yml file.");

		return aliases;
	}

	/**
	 * Create a new simple command group with the given label and aliases bundled in a list
	 */
	protected SimpleCommandGroup(StrictList<String> labelAndAliases) {
		this(labelAndAliases.get(0), (labelAndAliases.size() > 1 ? labelAndAliases.range(1) : new StrictList<String>()).getSource());
	}

	/**
	 * Create a new simple command group with the given label and aliases
	 */
	protected SimpleCommandGroup(String label, List<String> aliases) {
		this.label = label;
		this.aliases = aliases;
	}

	/**
	 * Create a new simple command group with the given label and aliases automatically
	 * separated by | or /
	 *
	 * Example: channel|ch will create a /channel command group that can also be called by using /ch
	 */
	protected SimpleCommandGroup(String labelAndAliases) {
		final String[] split = labelAndAliases.split("(\\||\\/)");

		this.label = split[0];
		this.aliases = split.length > 0 ? Arrays.asList(Arrays.copyOfRange(split, 1, split.length)) : new ArrayList<>();
	}

	// ----------------------------------------------------------------------
	// Main functions
	// ----------------------------------------------------------------------

	/**
	 * Register this command group into Bukkit and start using it
	 */
	public final void register() {
		Valid.checkBoolean(!this.isRegistered(), "Main command already registered as: " + this.mainCommand);

		this.mainCommand = new MainCommand(this.label);

		if (this.aliases != null)
			this.mainCommand.setAliases(this.aliases);

		this.mainCommand.register();
		this.registerSubcommands();

		// Sort A-Z
		Collections.sort(this.subcommands.getSource(), Comparator.comparing(SimpleSubCommand::getSublabel));

		// Check for collision
		this.checkSubCommandAliasesCollision();
	}

	/*
	 * Enforce non-overlapping aliases for subcommands
	 */
	private void checkSubCommandAliasesCollision() {
		final List<String> aliases = new ArrayList<>();

		for (final SimpleSubCommand subCommand : this.subcommands)
			for (final String alias : subCommand.getSublabels()) {
				Valid.checkBoolean(!aliases.contains(alias), "Subcommand '/" + this.getLabel() + " " + subCommand.getSublabel() + "' has alias '" + alias + "' that is already in use by another subcommand!");

				aliases.add(alias);
			}
	}

	/**
	 * Remove this command group from Bukkit. Takes immediate changes in the game.
	 */
	public final void unregister() {
		Valid.checkBoolean(this.isRegistered(), "Main command not registered!");

		this.mainCommand.unregister();
		this.mainCommand = null;

		this.subcommands.clear();
	}

	/**
	 * Has the command group been registered yet?
	 *
	 * @return
	 */
	public final boolean isRegistered() {
		return this.mainCommand != null;
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
	protected final void registerSubcommand(final SimpleSubCommand command) {
		Valid.checkNotNull(this.mainCommand, "Cannot add subcommands when main command is missing! Call register()");
		Valid.checkBoolean(!this.subcommands.contains(command), "Subcommand /" + this.mainCommand.getLabel() + " " + command.getSublabel() + " already registered when trying to add " + command.getClass());

		this.subcommands.add(command);
	}

	/**
	 * Automatically registers all extending classes for the given parent class into this command group.
	 * We automatically ignore abstract classes for you. ENSURE TO MAKE YOUR CHILDREN CLASSES FINAL.
	 *
	 * @param parentClass
	 */
	protected final void registerSubcommand(Class<? extends SimpleSubCommand> parentClass) {
		for (final Class<? extends SimpleSubCommand> clazz : ReflectionUtil.getClasses(SimplePlugin.getInstance(), parentClass)) {
			if (Modifier.isAbstract(clazz.getModifiers()))
				continue;

			Valid.checkBoolean(Modifier.isFinal(clazz.getModifiers()), "Make child of " + parentClass.getSimpleName() + " class " + clazz.getSimpleName() + " final to auto register it!");
			this.registerSubcommand(ReflectionUtil.instantiate(clazz));
		}
	}

	/**
	 * Registers a simple help message for this group, used in /{label} help|?
	 * since we add help for all subcommands automatically
	 *
	 * @param menuHelp
	 */
	protected final void registerHelpLine(final String... menuHelp) {
		Valid.checkNotNull(this.mainCommand, "Cannot add subcommands when main command is missing! Call register()");

		this.subcommands.add(new FillerSubCommand(this, menuHelp));
	}

	// ----------------------------------------------------------------------
	// Setters
	// ----------------------------------------------------------------------

	/**
	 * Updates the command label, only works if the command is not registered
	 *
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		Valid.checkBoolean(!this.isRegistered(), "Cannot use setLabel(" + label + ") for already registered command /" + this.getLabel());

		this.label = label;
	}

	/**
	 * Updates the command aliases, only works if the command is not registered
	 *
	 * @param aliases the aliases to set
	 */
	public void setAliases(List<String> aliases) {
		Valid.checkBoolean(!this.isRegistered(), "Cannot use setAliases(" + aliases + ") for already registered command /" + this.getLabel());

		this.aliases = aliases;
	}

	// ----------------------------------------------------------------------
	// Functions
	// ----------------------------------------------------------------------

	/**
	 * Return the message displayed when no parameter is given, by
	 * default we give credits
	 * <p>
	 * If you specify "author" in your plugin.yml we display author information
	 * If you override {@link SimplePlugin#getFoundedYear()} we display copyright
	 *
	 * @param sender the command sender that requested this to be shown to him
	 *               may be null
	 * @return
	 */
	protected List<SimpleComponent> getNoParamsHeader() {
		final int foundedYear = SimplePlugin.getInstance().getFoundedYear();
		final int yearNow = Calendar.getInstance().get(Calendar.YEAR);

		final List<String> messages = new ArrayList<>();

		messages.add("&8" + Common.chatLineSmooth());
		messages.add(this.getHeaderPrefix() + "  " + SimplePlugin.getNamed() + this.getTrademark() + " &7" + SimplePlugin.getVersion());
		messages.add(" ");

		{
			final String authors = String.join(", ", SimplePlugin.getInstance().getDescription().getAuthors());

			if (!authors.isEmpty())
				messages.add("   &7" + SimpleLocalization.Commands.LABEL_AUTHORS + " &f" + authors + (foundedYear != -1 ? " &7\u00A9 " + foundedYear + (yearNow != foundedYear ? " - " + yearNow : "") : ""));
		}

		{
			final String credits = this.getCredits();

			if (credits != null && !credits.isEmpty())
				messages.add("   " + credits);
		}

		messages.add("&8" + Common.chatLineSmooth());

		return Common.convert(messages, SimpleComponent::of);
	}

	/**
	 * Should we send command helps instead of no-param header?
	 *
	 * @return
	 */

	protected boolean sendHelpIfNoArgs() {
		return false;
	}

	// Return the TM symbol in case we have it for kangarko's plugins
	private String getTrademark() {
		return SimplePlugin.getInstance().getDescription().getAuthors().contains("kangarko") ? this.getHeaderPrefix() + "&8\u2122" : "";
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
	 * <p>
	 * Also see {@link #getHelpHeader()}
	 * <p>
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
				"&8" + Common.chatLineSmooth(),
				this.getHeaderPrefix() + "  " + SimplePlugin.getNamed() + this.getTrademark() + " &7" + SimplePlugin.getVersion(),
				" ",
				"&2  [] &f= " + SimpleLocalization.Commands.LABEL_OPTIONAL_ARGS,
				this.getTheme() + "  <> &f= " + SimpleLocalization.Commands.LABEL_REQUIRED_ARGS,
				" "
		};
	}

	/**
	 * Return the subcommand description when listing all commands using the "help" or "?" subcommand
	 * @return
	 */
	protected String getSubcommandDescription() {
		return SimpleLocalization.Commands.LABEL_SUBCOMMAND_DESCRIPTION;
	}

	/**
	 * Return the default color in the {@link #getHelpHeader()},
	 * GOLD + BOLD colors by default
	 *
	 * @return
	 */
	protected String getHeaderPrefix() {
		return this.getTheme() + "" + ChatColor.BOLD;
	}

	/**
	 * Return the color used in some places of the automatically generated command help
	 *
	 * @return
	 */
	protected ChatColor getTheme() {
		return ChatColor.GOLD;
	}

	/**
	 * How many commands shall we display per page by default?
	 *
	 * Defaults to 12
	 */
	protected int getCommandsPerPage() {
		return 12;
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * The main command handling this command group
	 */
	public final class MainCommand extends SimpleCommand {

		/**
		 * Create new main command with the given label
		 *
		 * @param label
		 */
		private MainCommand(final String label) {
			super(label);

			// Let everyone view credits of this command when they run it without any sublabels
			this.setPermission(null);

			// We handle help ourselves
			this.setAutoHandleHelp(false);
		}

		/**
		 * Handle this command group, print a special message when no arguments are given,
		 * execute subcommands, handle help or ? argument and more.
		 */
		@Override
		protected void onCommand() {

			// Pass through sender to the command group itself
			SimpleCommandGroup.this.sender = this.sender;

			// Print a special message on no arguments
			if (this.args.length == 0) {
				if (SimpleCommandGroup.this.sendHelpIfNoArgs())
					this.tellSubcommandsHelp();
				else
					this.tell(SimpleCommandGroup.this.getNoParamsHeader());

				return;
			}

			final String argument = this.args[0];
			final SimpleSubCommand command = this.findSubcommand(argument);

			// Handle subcommands
			if (command != null) {
				final String oldSublabel = command.getSublabel();

				try {
					// Simulate our main label
					command.setSublabel(this.args[0]);

					// Run the command
					command.execute(this.sender, this.getLabel(), this.args.length == 1 ? new String[] {} : Arrays.copyOfRange(this.args, 1, this.args.length));

				} finally {
					// Restore old sublabel after the command has been run
					command.setSublabel(oldSublabel);
				}
			}

			// Handle help argument
			else if (!SimpleCommandGroup.this.getHelpLabel().isEmpty() && Valid.isInList(argument, SimpleCommandGroup.this.getHelpLabel()))
				this.tellSubcommandsHelp();
			else
				this.returnInvalidArgs();
		}

		/**
		 * Automatically tells all help for all subcommands
		 */
		protected void tellSubcommandsHelp() {

			// Building help can be heavy so do it off of the main thread
			Common.runAsync(() -> {
				if (SimpleCommandGroup.this.subcommands.isEmpty()) {
					if (Messenger.ENABLED)
						this.tellError(SimpleLocalization.Commands.HEADER_NO_SUBCOMMANDS);
					else
						Common.tell(this.sender, SimpleLocalization.Commands.HEADER_NO_SUBCOMMANDS);

					return;
				}

				final List<SimpleComponent> lines = new ArrayList<>();
				final boolean atLeast17 = MinecraftVersion.atLeast(V.v1_7);

				for (final SimpleSubCommand subcommand : SimpleCommandGroup.this.subcommands)
					if (subcommand.showInHelp() && this.hasPerm(subcommand.getPermission())) {

						// Simulate the sender to enable permission checks in getMultilineHelp for ex.
						subcommand.sender = this.sender;

						if (subcommand instanceof FillerSubCommand) {
							this.tellNoPrefix(((FillerSubCommand) subcommand).getHelpMessages());

							continue;
						}

						final String usage = this.colorizeUsage(subcommand.getUsage());
						final String desc = Common.getOrEmpty(subcommand.getDescription());
						final String plainMessage = Replacer.replaceArray(SimpleCommandGroup.this.getSubcommandDescription(),
								"label", this.getLabel(),
								"sublabel", (atLeast17 ? "&n" : "") + subcommand.getSublabel() + (atLeast17 ? "&r" : ""),
								"usage", usage,
								"description", !desc.isEmpty() && !atLeast17 ? desc : "",
								"dash", !desc.isEmpty() && !atLeast17 ? "&e-" : "");

						final SimpleComponent line = SimpleComponent.of(plainMessage);

						if (!desc.isEmpty() && atLeast17) {
							final String command = Common.stripColors(plainMessage).substring(1);
							final List<String> hover = new ArrayList<>();

							hover.add(SimpleLocalization.Commands.HELP_TOOLTIP_DESCRIPTION.replace("{description}", desc));

							if (subcommand.getPermission() != null)
								hover.add(SimpleLocalization.Commands.HELP_TOOLTIP_PERMISSION.replace("{permission}", subcommand.getPermission()));

							if (subcommand.getMultilineUsageMessage() != null && subcommand.getMultilineUsageMessage().length > 0) {
								hover.add(SimpleLocalization.Commands.HELP_TOOLTIP_USAGE);

								for (final String usageLine : subcommand.getMultilineUsageMessage())
									hover.add("&f" + this.replacePlaceholders(this.colorizeUsage(usageLine.replace("{sublabel}", subcommand.getSublabel()))));

							} else
								hover.add(SimpleLocalization.Commands.HELP_TOOLTIP_USAGE + (usage.isEmpty() ? command : usage));

							for (int i = 0; i < hover.size(); i++) {
								final String hoverLine = String.join("\n    ", Common.split(hover.get(i), 65));

								hover.set(i, hoverLine);
							}

							line.onHover(hover);
							line.onClickSuggestCmd("/" + this.getLabel() + " " + subcommand.getSublabel());
						}

						lines.add(line);
					}

				if (!lines.isEmpty()) {
					final ChatPaginator pages = new ChatPaginator(MathUtil.range(0, lines.size(), SimpleCommandGroup.this.getCommandsPerPage()), ChatColor.DARK_GRAY);

					if (SimpleCommandGroup.this.getHelpHeader() != null)
						pages.setHeader(SimpleCommandGroup.this.getHelpHeader());

					pages.setPages(lines);

					// Allow "? <page>" page parameter
					final int page = (this.args.length > 1 && Valid.isInteger(this.args[1]) ? Integer.parseInt(this.args[1]) : 1);

					// Send the component on the main thread
					Common.runLater(() -> pages.send(this.sender, page));

				} else if (Messenger.ENABLED)
					this.tellError(SimpleLocalization.Commands.HEADER_NO_SUBCOMMANDS_PERMISSION);
				else
					Common.tell(this.sender, SimpleLocalization.Commands.HEADER_NO_SUBCOMMANDS_PERMISSION);
			});
		}

		/**
		 * Replaces some usage parameters such as <> or [] with colorized brackets
		 *
		 * @param message
		 * @return
		 */
		private String colorizeUsage(final String message) {
			return message == null ? "" : message.replace("<", "&6<").replace(">", "&6>&f").replace("[", "&2[").replace("]", "&2]&f").replaceAll(" \\-([a-zA-Z])", " &3-$1");
		}

		/**
		 * Finds a subcommand by label
		 *
		 * @param label
		 * @return
		 */
		private SimpleSubCommand findSubcommand(final String label) {
			for (final SimpleSubCommand command : SimpleCommandGroup.this.subcommands) {
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
			if (this.args.length == 1)
				return this.tabCompleteSubcommands(this.sender, this.args[0]);

			if (this.args.length > 1) {
				final SimpleSubCommand cmd = this.findSubcommand(this.args[0]);

				if (cmd != null)
					return cmd.tabComplete(this.sender, this.getLabel(), Arrays.copyOfRange(this.args, 1, this.args.length));
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
		private List<String> tabCompleteSubcommands(final CommandSender sender, String param) {
			param = param.toLowerCase();

			final List<String> tab = new ArrayList<>();

			for (final SimpleSubCommand subcommand : SimpleCommandGroup.this.subcommands)
				if (subcommand.showInHelp() && !(subcommand instanceof FillerSubCommand) && this.hasPerm(subcommand.getPermission()))
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

		private FillerSubCommand(final SimpleCommandGroup parent, final String... menuHelp) {
			super(parent, "_" + RandomUtil.nextBetween(1, Short.MAX_VALUE));

			this.helpMessages = menuHelp;
		}

		@Override
		protected void onCommand() {
			throw new FoException("Filler space command cannot be run!");
		}
	}

}
