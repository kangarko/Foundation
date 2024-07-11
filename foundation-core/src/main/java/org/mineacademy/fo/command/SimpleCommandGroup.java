package org.mineacademy.fo.command;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.Getter;

/**
 * A command group contains a set of different subcommands associated with the main command.
 * Example: /arena join, /arena leave etc.
 */
public abstract class SimpleCommandGroup {

	/**
	 * The list of sub-commands belonging to this command group, for example
	 * the /boss command has subcommands /boss region, /boss menu etc.
	 */
	private final List<SimpleSubCommandCore> subcommands = new ArrayList<>();

	/**
	 * The main command handling this command group, or null if not registered yet.
	 */
	private SimpleCommandCore mainCommand;

	/**
	 * The label to execute subcommands in this group, example: for Boss it's "boss".
	 */
	@Getter
	private final String label;

	/**
	 * What other commands trigger this command group? Example: for Boss it's "b".
	 */
	@Getter
	private final List<String> aliases;

	/**
	 * The last executor of this command group, mostly used in
	 * compiling info messages such as in {@link #getNoParamsHeader()}
	 */
	@Getter
	private FoundationPlayer audience;

	/**
	 * Create a new simple command group using {@link SimpleSettings#MAIN_COMMAND_ALIASES}
	 */
	protected SimpleCommandGroup() {
		this(SimpleSettings.MAIN_COMMAND_ALIASES);
	}

	/**
	 * Create a new simple command group with the given label and aliases.
	 */
	protected SimpleCommandGroup(final List<String> labelAndAliases) {
		ValidCore.checkBoolean(!labelAndAliases.isEmpty(), "No label and aliases given for " + this);

		this.label = labelAndAliases.get(0);
		this.aliases = labelAndAliases.size() > 1 ? labelAndAliases.subList(1, labelAndAliases.size()) : new ArrayList<>();
	}

	/**
	 * Create a new simple command group with the given label and aliases automatically
	 * separated by "|" or "/".
	 *
	 * Example: channel|ch will create a /channel command group that can also be called by using "/ch".
	 */
	protected SimpleCommandGroup(final String labelAndAliases) {
		final String[] split = labelAndAliases.split("(\\||\\/)");

		this.label = split[0];
		this.aliases = split.length > 0 ? Arrays.asList(Arrays.copyOfRange(split, 1, split.length)) : new ArrayList<>();
	}

	/**
	 * Create a new simple command group with the given label and aliases.
	 */
	protected SimpleCommandGroup(final String label, final List<String> aliases) {
		this.label = label;
		this.aliases = aliases;
	}

	// ----------------------------------------------------------------------
	// Main functions
	// ----------------------------------------------------------------------

	/**
	 * Register this command group into Bukkit and start using it.
	 */
	public final void register() {
		ValidCore.checkBoolean(!this.isRegistered(), "Command group already registered as: " + this.mainCommand);

		this.mainCommand = new MainCommand(this.label);

		if (this.aliases != null)
			this.mainCommand.setAliases(this.aliases);

		this.mainCommand.register();
		this.registerSubcommands();

		Collections.sort(this.subcommands, Comparator.comparing(SimpleSubCommandCore::getSublabel));

		this.checkSubCommandAliasesCollision();
	}

	/*
	 * Enforce non-overlapping aliases for subcommands.
	 */
	private void checkSubCommandAliasesCollision() {
		final List<String> aliases = new ArrayList<>();

		for (final SimpleSubCommandCore subCommand : this.subcommands)
			for (final String alias : subCommand.getSublabels()) {
				ValidCore.checkBoolean(!aliases.contains(alias), "Subcommand '/" + this.getLabel() + " " + subCommand.getSublabel() + "' has alias '" + alias + "' that is already in use by another subcommand!");

				aliases.add(alias);
			}
	}

	/**
	 * Remove this command group from Bukkit. Takes immediate changes in the game.
	 */
	public final void unregister() {
		ValidCore.checkBoolean(this.isRegistered(), "Main command not registered!");

		this.mainCommand.unregister();
		this.mainCommand = null;
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
	 * Register your subcommands here.
	 *
	 * @see {@link #registerSubcommand(SimpleSubCommandCore)}
	 * @see {@link #registerSubcommand(Class)}
	 * @see {@link #registerDefaultSubcommands()}
	 */
	protected abstract void registerSubcommands();

	/**
	 * Registers a new subcommand for this group.
	 *
	 * @param command
	 */
	public final void registerSubcommand(final SimpleSubCommandCore command) {
		ValidCore.checkNotNull(this.mainCommand, "Cannot add subcommands when main command is missing! Call register()");

		// Fixes reloading issue where all subcommands are cleared
		if (this.subcommands.contains(command))
			this.subcommands.remove(command);

		this.subcommands.add(command);
	}

	/**
	 * Automatically registers all extending classes for the given parent class into this command group.
	 *
	 * We automatically ignore abstract classes.
	 *
	 * The registered children classes must be made final.
	 *
	 * @param parentClass
	 */
	public final void registerSubcommand(final Class<? extends SimpleSubCommandCore> parentClass) {
		for (final Class<? extends SimpleSubCommandCore> clazz : ReflectionUtil.getClasses(Platform.getPlugin().getFile(), parentClass))
			if (!Modifier.isAbstract(clazz.getModifiers())) {
				ValidCore.checkBoolean(Modifier.isFinal(clazz.getModifiers()), "Make child of " + parentClass.getSimpleName() + " class " + clazz.getSimpleName() + " final to auto register it!");

				this.registerSubcommand(ReflectionUtil.instantiate(clazz));
			}
	}

	/**
	 * Automatically registers default Foundation subcommands:
	 *
	 * NB: {@link PermsCommand} is not automatically registed as it requires a class in its argument.
	 *
	 * @see DebugCommand
	 * @see DumpLocaleCommand
	 * @see ReloadCommand
	 *
	 * For platform specific implementations this might register more commands such as RegionCommand on Bukkit, etc.
	 */
	public final void registerDefaultSubcommands() {
		Platform.registerDefaultSubcommands(this);
	}

	// ----------------------------------------------------------------------
	// Functions
	// ----------------------------------------------------------------------

	/**
	 * The message displayed when no arguments are given to the command:
	 * i.e. /boss
	 *
	 * @return
	 */
	protected List<String> getNoParamsHeader() {
		final List<String> messages = new ArrayList<>();

		messages.add("&8" + CommonCore.chatLineSmooth());
		messages.add("  " + this.getHeaderPrefix() + Platform.getPlugin().getName() + " &r&7" + Platform.getPlugin().getVersion());
		messages.add("  ");

		final String authors = Platform.getPlugin().getAuthors();

		if (!authors.isEmpty()) {
			final int foundedYear = Platform.getPlugin().getFoundedYear();
			final int currentYear = Calendar.getInstance().get(Calendar.YEAR);

			messages.add("  " + Lang.plain("command-label-authors") + " &f" + authors + (foundedYear != -1 ? " &7\u00A9 " + foundedYear + (currentYear != foundedYear ? " - " + currentYear : "") : ""));
		}

		messages.add("  " + this.getCredits());
		messages.add("&8" + CommonCore.chatLineSmooth());

		return messages;
	}

	/**
	 * The credits message pointing to mineacademy most users want to change.
	 *
	 * @return
	 */
	protected String getCredits() {
		return "<gray>Visit <white><click:open_url:'https://mineacademy.org/plugins'>mineacademy.org/plugins</click> <gray>for more information.";
	}

	/**
	 * The message displayed when the user types "help" or "?" to the command:
	 * i.e. "/boss help" or "/boss ?"
	 *
	 * @return
	 */
	protected String[] getHelpHeader() {
		return new String[] {
				"  ",
				"&8" + CommonCore.chatLineSmooth(),
				"  " + this.getHeaderPrefix() + Platform.getPlugin().getName() + " &r&7" + Platform.getPlugin().getVersion(),
				"  ",
				"  &2[] &7= " + Lang.plain("command-label-optional-args"),
				"  &6<> &7= " + Lang.plain("command-label-required-args"),
				"  "
		};
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
	protected List<String> getHelpArguments() {
		return Arrays.asList("help", "?");
	}

	/**
	 * Return the default color in the {@link #getHelpHeader()},
	 * GOLD + BOLD colors by default
	 *
	 * @return
	 */
	protected String getHeaderPrefix() {
		return "&6&l";
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * The main command handling this command group
	 */
	public final class MainCommand extends SimpleCommandCore {

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
			SimpleCommandGroup.this.audience = this.audience;

			// Print a special message on no arguments
			if (this.args.length == 0) {
				for (final String component : SimpleCommandGroup.this.getNoParamsHeader())
					this.audience.sendMessage(SimpleComponent.fromMini(component));

				return;
			}

			final String argument = this.args[0];
			final SimpleSubCommandCore subcommand = this.findSubcommand(argument);

			if (subcommand != null)
				subcommand.delegateExecute(this.audience, this.getLabel(), this.compileSubcommandArgs());

			else if (!SimpleCommandGroup.this.getHelpArguments().isEmpty() && ValidCore.isInList(argument, SimpleCommandGroup.this.getHelpArguments()))
				this.tellSubcommandsHelp();

			else
				this.returnInvalidArgs(argument);
		}

		/**
		 * Automatically tells all help for all subcommands
		 */
		protected void tellSubcommandsHelp() {
			if (SimpleCommandGroup.this.subcommands.isEmpty()) {
				this.tellError(Lang.component("command-header-no-subcommands"));

				return;
			}

			final List<SimpleComponent> lines = new ArrayList<>();

			for (final SimpleSubCommandCore subcommand : SimpleCommandGroup.this.subcommands) {
				if (!subcommand.showInHelp() || !this.hasPerm(subcommand.getPermission()))
					continue;

				// Simulate the sender to enable permission checks in getMultilineHelp for ex.
				subcommand.audience = this.audience;
				subcommand.args = this.compileSubcommandArgs();

				final List<SimpleComponent> hover = new ArrayList<>();

				if (subcommand.getDescription() != null)
					hover.add(Lang.componentVars("command-help-tooltip-description", "description", subcommand.getDescription()));

				if (subcommand.getPermission() != null)
					hover.add(Lang.componentVars("command-help-tooltip-permission", "permission", subcommand.getPermission()));

				final String[] legacyUsage = subcommand.getMultilineUsageMessage();
				final SimpleComponent[] newUsage = subcommand.getMultilineUsage();

				if (legacyUsage != null || newUsage != null || subcommand.getUsage() != null)
					hover.add(Lang.componentVars("command-help-tooltip-usage", "usage", legacyUsage != null || newUsage != null ? SimpleComponent.empty() : CommonCore.getOrDefault(this.colorizeUsage(subcommand.getUsage()), SimpleComponent.empty())));

				if (legacyUsage != null)
					for (final String line : legacyUsage)
						hover.add(subcommand.replacePlaceholders(this.colorizeUsage(SimpleComponent.fromMini(line))));

				else if (newUsage != null)
					for (final SimpleComponent component : newUsage)
						hover.add(subcommand.replacePlaceholders(this.colorizeUsage(component)));

				SimpleComponent line = SimpleComponent
						.fromPlain("  /" + this.getLabel())
						.appendMini(" &n" + subcommand.getSublabel() + "&r ")
						.onClickSuggestCmd("/" + this.getLabel() + " " + subcommand.getSublabel())
						.onHover(hover);

				if (subcommand.getUsage() != null)
					line = line.append(this.colorizeUsage(subcommand.getUsage()));

				lines.add(line);
			}

			if (!lines.isEmpty()) {
				final ChatPaginator pages = new ChatPaginator(12);

				if (SimpleCommandGroup.this.getHelpHeader() != null)
					pages.setHeader(SimpleCommandGroup.this.getHelpHeader());

				pages.setPages(lines);

				// Allow "? <page>" page parameter
				final int page = (this.args.length > 1 && ValidCore.isInteger(this.args[1]) ? Integer.parseInt(this.args[1]) : 1);

				pages.send(this.audience, page);

			} else
				this.tellError(Lang.component("command-header-no-subcommands-permission"));
		}

		/*
		 * Compiles the arguments for the subcommand
		 */
		private String[] compileSubcommandArgs() {
			return this.args.length == 1 ? new String[] {} : Arrays.copyOfRange(this.args, 1, this.args.length);
		}

		/**
		 * Finds a subcommand by label
		 *
		 * @param label
		 * @return
		 */
		private SimpleSubCommandCore findSubcommand(final String label) {
			for (final SimpleSubCommandCore command : SimpleCommandGroup.this.subcommands)
				for (final String alias : command.getSublabels())
					if (alias.equalsIgnoreCase(label))
						return command;

			return null;
		}

		/**
		 * Handle tabcomplete for subcommands and their tabcomplete
		 */
		@Override
		public List<String> tabComplete() {
			if (this.args.length == 1)
				return this.tabCompleteSubcommands(this.audience, this.args[0]);

			if (this.args.length > 1) {
				final SimpleSubCommandCore cmd = this.findSubcommand(this.args[0]);

				if (cmd != null)
					return cmd.delegateTabComplete(this.audience, this.getLabel(), Arrays.copyOfRange(this.args, 1, this.args.length));
			}

			return NO_COMPLETE;
		}

		/**
		 * Automatically tab-complete subcommands
		 *
		 * @param audience
		 * @param param
		 * @return
		 */
		private List<String> tabCompleteSubcommands(final FoundationPlayer audience, String param) {
			param = param.toLowerCase();

			final List<String> tab = new ArrayList<>();

			for (final SimpleSubCommandCore subcommand : SimpleCommandGroup.this.subcommands)
				if (subcommand.showInHelp() && this.hasPerm(subcommand.getPermission()))
					for (final String label : subcommand.getSublabels())
						if (!label.trim().isEmpty() && label.startsWith(param))
							tab.add(label);

			return tab;
		}
	}
}
