package org.mineacademy.fo.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.MissingEnumException;
import org.mineacademy.fo.TabUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.exception.InvalidCommandArgException;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.NonNull;

/**
 * A simple command used to replace all Bukkit/Spigot command functionality
 * across any plugin that utilizes this.
 */
public abstract class SimpleCommand extends Command {

	/**
	 * The default permission syntax for this command.
	 */
	protected static final String DEFAULT_PERMISSION_SYNTAX = "{plugin_name_lower}.command.{label}";

	/**
	 * Denotes an empty list used to disable tab-completion
	 */
	protected static final List<String> NO_COMPLETE = new ArrayList<>();

	/**
	 * If this flag is true, we will use {@link Messenger} to send
	 * messages (no prefix supported)
	 */
	public static boolean USE_MESSENGER = false;

	/**
	 * You can set the cooldown time before executing the command again. This map
	 * stores the player uuid and his last execution of the command.
	 */
	private final ExpiringMap<UUID, Long> cooldownMap = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	/**
	 * The command label, eg. boss for /boss
	 * <p>
	 * This variable is updated dynamically when the command is run with the
	 * last known version of the label, e.g. /boss or /b will set it to boss or b
	 * respectively
	 */
	private String label;

	/**
	 * Has this command been already registered?
	 */
	private boolean registered = false;

	/**
	 * Should we add {@link Common#getTellPrefix()} automatically when calling tell and returnTell methods
	 * from this command?
	 */
	private boolean addTellPrefix = true;

	/**
	 * The {@link Common#getTellPrefix()} custom prefix only used for sending messages in {@link #onCommand()} method
	 * for this command, empty by default, then we use the one in Common
	 */
	private String tellPrefix = "";

	/**
	 * Minimum arguments required to run this command
	 */
	@Getter
	private int minArguments = 0;

	/**
	 * The command cooldown before we can run this command again
	 */
	private int cooldownSeconds = 0;

	/**
	 * A custom message when the player attempts to run this command
	 * within {@link #cooldownSeconds}. By default we use the one found in
	 * {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 * <p>
	 * TIP: Use {duration} to replace the remaining time till next run
	 */
	private String cooldownMessage = null;

	/**
	 * Should we automatically send usage message when the first argument
	 * equals to "help" or "?" ?
	 */
	private boolean autoHandleHelp = true;

	// ----------------------------------------------------------------------
	// Temporary variables
	// ----------------------------------------------------------------------

	/**
	 * The command sender, or null if does not exist
	 * <p>
	 * This variable is updated dynamically when the command is run with the
	 * last known sender
	 */
	protected CommandSender sender;

	/**
	 * The arguments used when the command was last executed
	 * <p>
	 * This variable is updated dynamically when the command is run with the
	 * last known arguments
	 */
	protected String[] args;

	// ----------------------------------------------------------------------

	/**
	 * Create a new simple command with the given label.
	 * <p>
	 * Separate the label with | to split between label and aliases.
	 * Example: remove|r|rm will create a /remove command that can
	 * also be run by typing /r and /rm as its aliases.
	 *
	 * @param label
	 */
	protected SimpleCommand(final String label) {
		this(parseLabel0(label), parseAliases0(label));
	}

	/**
	 * Create a new simple command from the list. The first
	 * item in the list is the main label and the other ones are the aliases.
	 */
	protected SimpleCommand(final StrictList<String> labels) {
		this(parseLabelList0(labels), labels.size() > 1 ? labels.subList(1, labels.size()) : null);
	}

	/**
	 * Create a new simple command
	 *
	 * @param label
	 * @param aliases
	 */
	protected SimpleCommand(final String label, final List<String> aliases) {
		super(label);

		Valid.checkBoolean(!(this instanceof CommandExecutor), "Please do not write 'implements CommandExecutor' for /" + super.getLabel() + " cmd, we already have a listener there");
		Valid.checkBoolean(!(this instanceof TabCompleter), "Please do not write 'implements TabCompleter' for /" + super.getLabel() + " cmd, simply override tabComplete method");

		setLabel(label);

		if (aliases != null)
			setAliases(aliases);

		// Set a default permission for this command
		setPermission(DEFAULT_PERMISSION_SYNTAX);
	}

	/*
	 * Split the given label by | and get the first part, used as the main label
	 */
	private static String parseLabel0(final String label) {
		Valid.checkNotNull(label, "Label must not be null!");

		return label.split("\\|")[0];
	}

	/*
	 * Split the given label by | and use the second and further parts as aliases
	 */
	private static List<String> parseAliases0(final String label) {
		final String[] aliases = label.split("\\|");

		return aliases.length > 0 ? Arrays.asList(Arrays.copyOfRange(aliases, 1, aliases.length)) : new ArrayList<>();
	}

	/*
	 * Return the first index from the list or thrown an error if list empty
	 */
	private static String parseLabelList0(final StrictList<String> labels) {
		Valid.checkBoolean(!labels.isEmpty(), "Command label must not be empty!");

		return labels.get(0);
	}

	// ----------------------------------------------------------------------
	// Registration
	// ----------------------------------------------------------------------

	/**
	 * Registers this command into Bukkit.
	 * <p>
	 * Throws an error if the command {@link #isRegistered()} already.
	 */
	public final void register() {
		register(true);
	}

	/**
	 * Registers this command into Bukkit.
	 * <p>
	 * Throws an error if the command {@link #isRegistered()} already.
	 *
	 * @param unregisterOldAliases If a command with the same label is already present, should
	 *                             we remove associated aliases with the old command? This solves a problem
	 *                             in ChatControl where unregistering /tell from the Essentials plugin would also
	 *                             unregister /t from Towny, which is undesired.
	 */
	public final void register(final boolean unregisterOldAliases) {
		Valid.checkBoolean(!(this instanceof SimpleSubCommand), "Sub commands cannot be registered!");
		Valid.checkBoolean(!registered, "The command /" + getLabel() + " has already been registered!");

		final PluginCommand oldCommand = Bukkit.getPluginCommand(getLabel());

		if (oldCommand != null) {
			final String owningPlugin = oldCommand.getPlugin().getName();

			if (!owningPlugin.equals(SimplePlugin.getNamed()))
				Debugger.debug("plugin", "&eCommand &f/" + getLabel() + " &ealready used by " + owningPlugin + ", we take it over...");

			Remain.unregisterCommand(oldCommand.getLabel(), unregisterOldAliases);
		}

		Remain.registerCommand(this);
		registered = true;
	}

	/**
	 * Removes the command from Bukkit.
	 * <p>
	 * Throws an error if the command is not {@link #isRegistered()}.
	 */
	public final void unregister() {
		Valid.checkBoolean(!(this instanceof SimpleSubCommand), "Sub commands cannot be unregistered!");
		Valid.checkBoolean(registered, "The command /" + getLabel() + " is not registered!");

		Remain.unregisterCommand(getLabel());
		registered = false;
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * Execute this command, updates the {@link #sender}, {@link #label} and {@link #args} variables,
	 * checks permission and returns if the sender lacks it,
	 * checks minimum arguments and finally passes the command to the child class.
	 * <p>
	 * Also contains various error handling scenarios
	 */
	@Override
	public final boolean execute(final CommandSender sender, final String label, final String[] args) {

		if (SimplePlugin.isReloading() || !SimplePlugin.getInstance().isEnabled()) {
			Common.tell(sender, SimpleLocalization.Commands.USE_WHILE_NULL.replace("{state}", (SimplePlugin.isReloading() ? SimpleLocalization.Commands.RELOADING : SimpleLocalization.Commands.DISABLED)));

			return false;
		}

		// Set variables to re-use later
		this.sender = sender;
		this.label = label;
		this.args = args;

		// Set tell prefix only if the parent setting was on
		final boolean hadTellPrefix = Common.ADD_TELL_PREFIX;
		if (hadTellPrefix)
			Common.ADD_TELL_PREFIX = addTellPrefix;

		// Catch "errors" that contain a message to send to the player
		try {

			// Check if sender has the proper permission
			if (getPermission() != null)
				checkPerm(getPermission());

			// Check for minimum required arguments and print help
			if (args.length < getMinArguments() || autoHandleHelp && args.length == 1 && ("help".equals(args[0]) || "?".equals(args[0]))) {

				final String usage = getMultilineUsageMessage() != null ? String.join("\n&c", getMultilineUsageMessage()) : getUsage() != null && !getUsage().isEmpty() ? getUsage() : null;
				Valid.checkNotNull(usage, "If you set getMinArguments you must also call setUsage for /" + getLabel() + " command!");

				if (!Common.getOrEmpty(getDescription()).isEmpty())
					tellNoPrefix(SimpleLocalization.Commands.LABEL_DESCRIPTION.replace("{description}", getDescription()));

				if (getMultilineUsageMessage() != null) {
					tellNoPrefix(SimpleLocalization.Commands.LABEL_USAGES, "&c" + usage);

				} else {
					final String sublabel = this instanceof SimpleSubCommand ? " " + ((SimpleSubCommand) this).getSublabel() : "";

					tellNoPrefix(SimpleLocalization.Commands.LABEL_USAGE + " /" + label + sublabel + (!usage.startsWith("/") ? " " + Common.stripColors(usage) : ""));
				}

				return true;
			}

			// Check if we can run this command in time
			if (cooldownSeconds > 0)
				handleCooldown();

			onCommand();

		} catch (final InvalidCommandArgException ex) {
			if (getMultilineUsageMessage() == null)
				dynamicTellError(ex.getMessage() != null ? ex.getMessage() : SimpleLocalization.Commands.INVALID_SUB_ARGUMENT);
			else {
				dynamicTellError(SimpleLocalization.Commands.INVALID_ARGUMENT_MULTILINE);

				for (final String line : getMultilineUsageMessage())
					tellNoPrefix("&c" + line);
			}

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				dynamicTellError(ex.getMessages());

		} catch (final Throwable t) {
			dynamicTellError(SimpleLocalization.Commands.ERROR.replace("{error}", t.toString()));
			final String sublabel = this instanceof SimpleSubCommand ? " " + ((SimpleSubCommand) this).getSublabel() : "";

			Common.error(t, "Failed to execute command /" + getLabel() + sublabel + " " + String.join(" ", args));

		} finally {
			Common.ADD_TELL_PREFIX = hadTellPrefix;
		}

		return true;
	}

	private void dynamicTellError(final String... messages) {
		if (USE_MESSENGER)
			for (final String message : messages)
				tellError(message);
		else
			tell(messages);
	}

	/**
	 * Check if the command cooldown is active and if the command
	 * is run within the given limit, we stop it and inform the player
	 */
	private void handleCooldown() {
		if (isPlayer()) {
			final Player player = getPlayer();

			final long lastExecution = cooldownMap.getOrDefault(player.getUniqueId(), 0L);
			final long lastExecutionDifference = (System.currentTimeMillis() - lastExecution) / 1000;

			// Check if the command was not run earlier within the wait threshold
			checkBoolean(lastExecution == 0 || lastExecutionDifference > cooldownSeconds, Common.getOrDefault(cooldownMessage, SimpleLocalization.Commands.COOLDOWN_WAIT).replace("{duration}", cooldownSeconds - lastExecutionDifference + 1 + ""));

			// Update the last try with the current time
			cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
		}
	}

	/**
	 * Executed when the command is run. You can get the variables sender and args directly,
	 * and use convenience checks in the simple command class.
	 */
	protected abstract void onCommand();

	/**
	 * Get a custom multilined usagem message to be shown instead of the one line one
	 *
	 * @return the multiline custom usage message, or null
	 */
	protected String[] getMultilineUsageMessage() {
		return null;
	}

	// ----------------------------------------------------------------------
	// Convenience checks
	//
	// Here is how they work: When you command is executed, simply call any
	// of these checks. If they fail, an error will be thrown inside of
	// which will be a message for the player.
	//
	// We catch that error and send the message to the player without any
	// harm or console errors to your plugin. That is intended and saves time.
	// ----------------------------------------------------------------------

	/**
	 * Checks if the player is a console and throws an error if he is
	 *
	 * @throws CommandException
	 */
	protected final void checkConsole() throws CommandException {
		if (!isPlayer())
			throw new CommandException("&c" + SimpleLocalization.Commands.NO_CONSOLE);
	}

	/**
	 * Checks if the player has the given permission
	 *
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull final String perm) throws CommandException {
		if (isPlayer() && !PlayerUtil.hasPerm(sender, perm))
			throw new CommandException(getPermissionMessage().replace("{permission}", perm));
	}

	/**
	 * Check if the command arguments are of the minimum length
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(final int minimumLength, final String falseMessage) throws CommandException {
		if (args.length < minimumLength)
			returnTell((USE_MESSENGER ? "" : "&c") + falseMessage);
	}

	/**
	 * Checks if the given boolean is true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkBoolean(final boolean value, final String falseMessage) throws CommandException {
		if (!value)
			returnTell((USE_MESSENGER ? "" : "&c") + falseMessage);
	}

	/**
	 * Checks if the given object is not null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	protected final void checkNotNull(final Object value, final String messageIfNull) throws CommandException {
		if (value == null)
			returnTell((USE_MESSENGER ? "" : "&c") + messageIfNull);
	}

	/**
	 * Attempts to find a non-vanished online player, failing with the message
	 * found at {@link SimpleLocalization.Player#NOT_ONLINE}
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayer(final String name) throws CommandException {
		return findPlayer(name, SimpleLocalization.Player.NOT_ONLINE);
	}

	/**
	 * Attempts to find a non-vanished online player, failing with a false message
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayer(final String name, final String falseMessage) throws CommandException {
		final Player player = Bukkit.getPlayer(name);
		checkBoolean(player != null && player.isOnline(), falseMessage.replace("{player}", name));

		return player;
	}

	/**
	 * Attempts to parse the given name into a CompMaterial, will work for both modern
	 * and legacy materials: MONSTER_EGG and SHEEP_SPAWN_EGG
	 * <p>
	 * You can use the {enum} or {item} variable to replace with the given name
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final CompMaterial findMaterial(final String name, final String falseMessage) throws CommandException {
		final CompMaterial found = CompMaterial.fromString(name);

		checkNotNull(found, falseMessage.replace("{enum}", name).replace("{item}", name));
		return found;
	}

	/**
	 * Finds an enumeration of a certain type, if it fails it prints a false message to the player
	 * You can use the {enum} variable in the false message for the name parameter
	 *
	 * @param <T>
	 * @param enumType
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String name, final String falseMessage) throws CommandException {
		T found = null;

		try {
			found = ReflectionUtil.lookupEnum(enumType, name);
		} catch (final MissingEnumException ex) {
		}

		checkNotNull(found, falseMessage.replace("{enum}", name));
		return found;
	}

	/**
	 * A convenience method for parsing a number that is between two bounds
	 * You can use {min} and {max} in the message to be automatically replaced
	 *
	 * @param index
	 * @param min
	 * @param max
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final int min, final int max, final String falseMessage) {
		return findNumber(Integer.class, index, min, max, falseMessage);
	}

	/**
	 * A convenience method for parsing a number at the given args index
	 *
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final String falseMessage) {
		return findNumber(Integer.class, index, falseMessage);
	}

	/**
	 * A convenience method for parsing any number type that is between two bounds
	 * Number can be of any type, that supports method valueOf(String)
	 * You can use {min} and {max} in the message to be automatically replaced
	 *
	 * @param <T>
	 * @param numberType
	 * @param index
	 * @param min
	 * @param max
	 * @param falseMessage
	 * @return
	 */
	protected final <T extends Number & Comparable<T>> T findNumber(final Class<T> numberType, final int index, final T min, final T max, final String falseMessage) {
		final T number = findNumber(numberType, index, falseMessage);
		checkBoolean(number.compareTo(min) >= 0 && number.compareTo(max) <= 0, falseMessage.replace("{min}", min + "").replace("{max}", max + ""));

		return number;
	}

	/**
	 * A convenience method for parsing any number type at the given args index
	 * Number can be of any type, that supports method valueOf(String)
	 *
	 * @param <T>
	 * @param numberType
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final <T extends Number> T findNumber(final Class<T> numberType, final int index, final String falseMessage) {
		checkBoolean(index < args.length, falseMessage);

		try {
			return (T) numberType.getMethod("valueOf", String.class).invoke(null, args[index]); // Method valueOf is part of all main Number sub classes, eg. Short, Integer, Double, etc.
		}

		// Print stack trace for all exceptions, except NumberFormatException
		// NumberFormatException is expected to happen, in this case we just want to display falseMessage without stack trace
		catch (final IllegalAccessException | NoSuchMethodException e) {
			e.printStackTrace();

		} catch (final InvocationTargetException e) {
			if (!(e.getCause() instanceof NumberFormatException))
				e.printStackTrace();
		}

		throw new CommandException(replacePlaceholders((USE_MESSENGER ? "" : "&c") + falseMessage));
	}

	/**
	 * A convenience method for parsing a boolean at the given args index
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final boolean findBoolean(final int index, final String invalidMessage) {
		checkBoolean(index < args.length, invalidMessage);

		if (args[index].equalsIgnoreCase("true"))
			return true;

		else if (args[index].equalsIgnoreCase("false"))
			return false;

		throw new CommandException(replacePlaceholders((USE_MESSENGER ? "" : "&c") + invalidMessage));
	}

	// ----------------------------------------------------------------------
	// Other checks
	// ----------------------------------------------------------------------

	/**
	 * A convenience check for quickly determining if the sender has a given
	 * permission.
	 * <p>
	 * TIP: For a more complete check use {@link #checkPerm(String)} that
	 * will automatically return your command if they lack the permission.
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(final String permission) {
		return PlayerUtil.hasPerm(sender, permission);
	}

	// ----------------------------------------------------------------------
	// Messaging
	// ----------------------------------------------------------------------

	/**
	 * Sends a interactive chat component to the sender, not replacing any special
	 * variables just executing the {@link SimpleComponent#send(CommandSender...)} method
	 * as a shortcut
	 *
	 * @param component
	 */
	protected final void tell(final SimpleComponent component) {
		component.send(sender);
	}

	/**
	 * Send the player a Replacer message
	 *
	 * @param replacer
	 */
	protected final void tell(final Replacer replacer) {
		tell(replacer.getReplacedMessage());
	}

	/**
	 * Send a list of messages to the player
	 *
	 * @param messages
	 */
	protected final void tell(final Collection<String> messages) {
		tell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Send the player a Replacer message
	 *
	 * @param replacer
	 */
	protected final void tellNoPrefix(final Replacer replacer) {
		tellNoPrefix(replacer.getReplacedMessage());
	}

	/**
	 * Sends a multiline message to the player without plugins prefix
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(final String... messages) {
		final boolean tellPrefix = Common.ADD_TELL_PREFIX;
		final boolean localPrefix = addTellPrefix;

		Common.ADD_TELL_PREFIX = false;
		addTellPrefix = false;

		tell(messages);

		Common.ADD_TELL_PREFIX = tellPrefix;
		addTellPrefix = localPrefix;
	}

	/**
	 * Sends a multiline message to the player, avoiding prefix if 3 lines or more
	 *
	 * @param messages
	 */
	protected final void tell(String... messages) {
		if (messages != null) {
			messages = replacePlaceholders(messages);

			if (!addTellPrefix || USE_MESSENGER || messages.length > 2) {

				if (USE_MESSENGER && addTellPrefix) {
					tellInfo(messages[0]);

					if (messages.length > 1)
						for (int i = 1; i < messages.length; i++)
							Common.tellNoPrefix(sender, messages[i]);

				} else
					Common.tellNoPrefix(sender, messages);

			} else if (tellPrefix.isEmpty())
				Common.tell(sender, messages);
			else
				for (final String message : messages)
					Common.tellNoPrefix(sender, (tellPrefix.isEmpty() ? "" : tellPrefix + " ") + message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellSuccess(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.success(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellInfo(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.info(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellWarn(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.warn(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellError(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.error(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellQuestion(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.question(sender, message);
		}
	}

	/**
	 * Convenience method for returning the command with the {@link SimpleLocalization.Commands#INVALID_ARGUMENT}
	 * message for player
	 */
	protected final void returnInvalidArgs() {
		tellError(SimpleLocalization.Commands.INVALID_ARGUMENT.replace("{label}", getLabel()));

		throw new CommandException();
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(final Collection<String> messages) throws CommandException {
		returnTell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param replacer
	 * @throws CommandException
	 */
	protected final void returnTell(final Replacer replacer) throws CommandException {
		returnTell(replacer.getReplacedMessage());
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(final String... messages) throws CommandException {
		throw new CommandException(replacePlaceholders(messages));
	}

	/**
	 * Ho ho ho, returns the command usage to the sender
	 *
	 * @throws InvalidCommandArgException
	 */
	protected final void returnUsage() throws InvalidCommandArgException {
		throw new InvalidCommandArgException();
	}

	// ----------------------------------------------------------------------
	// Placeholder
	// ----------------------------------------------------------------------

	/**
	 * Replaces placeholders in all messages
	 * To change them override {@link #replacePlaceholders(String)}
	 *
	 * @param messages
	 * @return
	 */
	protected final String[] replacePlaceholders(final String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = replacePlaceholders(messages[i]).replace("{prefix}", Common.getTellPrefix());

		return messages;
	}

	/**
	 * Replaces placeholders in the message
	 *
	 * @param message
	 * @return
	 */
	protected String replacePlaceholders(String message) {
		// Replace basic labels
		message = replaceBasicPlaceholders0(message);

		// Replace {X} with arguments
		for (int i = 0; i < args.length; i++)
			message = message.replace("{" + i + "}", Common.getOrEmpty(args[i]));

		return message;
	}

	/**
	 * Internal method for replacing {label} {sublabel} and {plugin_name} placeholders
	 *
	 * @param message
	 * @return
	 */
	private String replaceBasicPlaceholders0(final String message) {
		return message
				.replace("{label}", getLabel())
				.replace("{sublabel}", this instanceof SimpleSubCommand ? ((SimpleSubCommand) this).getSublabels()[0] : super.getLabel())
				.replace("{plugin_name_lower}", SimplePlugin.getNamed().toLowerCase());
	}

	/**
	 * Utility method to safely update the args, increasing them if the position is too high
	 * <p>
	 * Used in placeholders
	 *
	 * @param position
	 * @param value
	 */
	protected final void setArg(final int position, final String value) {
		if (args.length <= position)
			args = Arrays.copyOf(args, position + 1);

		args[position] = value;
	}

	/**
	 * Convenience method for returning the last word in arguments
	 *
	 * @return
	 */
	protected final String getLastArg() {
		return args.length > 0 ? args[args.length - 1] : "";
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end
	 *
	 * @param from
	 * @return
	 */
	protected final String[] rangeArgs(final int from) {
		return rangeArgs(from, args.length);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to the given end
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final String[] rangeArgs(final int from, final int to) {
		return Arrays.copyOfRange(args, from, to);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end joined by spaces
	 *
	 * @param from
	 * @return
	 */
	protected final String joinArgs(final int from) {
		return joinArgs(from, args.length);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to the given end joined by spaces
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final String joinArgs(final int from, final int to) {
		String message = "";

		for (int i = from; i < args.length && i < to; i++)
			message += args[i] + (i + 1 == args.length ? "" : " ");

		return message;
	}

	// ----------------------------------------------------------------------
	// Tab completion
	// ----------------------------------------------------------------------

	/**
	 * Show tab completion suggestions when the given sender
	 * writes the command with the given arguments
	 * <p>
	 * Tab completion is only shown if the sender has {@link #getPermission()}
	 *
	 * @param sender
	 * @param alias
	 * @param args
	 * @param location
	 * @return
	 * @deprecated location is not used
	 */
	@Deprecated
	@Override
	public final List<String> tabComplete(final CommandSender sender, final String alias, final String[] args, final Location location) throws IllegalArgumentException {
		return tabComplete(sender, alias, args);
	}

	/**
	 * Show tab completion suggestions when the given sender
	 * writes the command with the given arguments
	 * <p>
	 * Tab completion is only shown if the sender has {@link #getPermission()}
	 *
	 * @param sender
	 * @param alias
	 * @param args
	 * @return
	 */
	@Override
	public final List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) throws IllegalArgumentException {
		this.sender = sender;
		label = alias;
		this.args = args;

		if (hasPerm(getPermission())) {
			List<String> suggestions = tabComplete();

			// Return online player names when suggestions are null - simulate Bukkit behaviour
			if (suggestions == null)
				suggestions = completeLastWordPlayerNames();

			return suggestions;
		}

		return new ArrayList<>();
	}

	/**
	 * Override this method to support tab completing in your command.
	 * <p>
	 * You can then use "sender", "label" or "args" fields from {@link SimpleCommand}
	 * class normally and return a list of tab completion suggestions.
	 * <p>
	 * We already check for {@link #getPermission()} and only call this method if the
	 * sender has it.
	 * <p>
	 * TIP: Use {@link #completeLastWord(Iterable)} and {@link #getLastArg()} methods
	 * in {@link SimpleCommand} for your convenience
	 *
	 * @return the list of suggestions to complete, or null to complete player names automatically
	 */
	protected List<String> tabComplete() {
		return null;
	}

	/**
	 * Convenience method for automatically completing the last word
	 * with the given suggestions. We sort them and only select ones
	 * that the last word starts with.
	 *
	 * @param <T>
	 * @param suggestions
	 * @return
	 */
	protected final <T> List<String> completeLastWord(final T... suggestions) {
		return TabUtil.complete(getLastArg(), suggestions);
	}

	/**
	 * Convenience method for automatically completing the last word
	 * with the given suggestions. We sort them and only select ones
	 * that the last word starts with.
	 *
	 * @param <T>
	 * @param suggestions
	 * @return
	 */
	protected final <T> List<String> completeLastWord(final Iterable<T> suggestions) {
		final List<T> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(suggestion);

		return TabUtil.complete(getLastArg(), list.toArray());
	}

	/**
	 * Convenience method for completing all player names that the sender can see
	 * and that are not vanished
	 * <p>
	 * TIP: You can simply return null for the same behaviour
	 *
	 * @return
	 */
	protected final List<String> completeLastWordPlayerNames() {
		return TabUtil.complete(getLastArg(), isPlayer() ? Common.getPlayerNames(false) : Common.getPlayerNames());
	}

	// ----------------------------------------------------------------------
	// Temporary variables and safety
	// ----------------------------------------------------------------------

	/**
	 * Attempts to get the sender as player, only works if the sender is actually a player,
	 * otherwise we return null
	 *
	 * @return
	 */
	protected final Player getPlayer() {
		return isPlayer() ? (Player) getSender() : null;
	}

	/**
	 * Return whether the sender is a living player
	 *
	 * @return
	 */
	protected final boolean isPlayer() {
		return sender instanceof Player;
	}

	/**
	 * Should we add {@link Common#getTellPrefix()} automatically when calling tell and returnTell methods
	 * from this command?
	 *
	 * @param addTellPrefix
	 */
	protected final void addTellPrefix(final boolean addTellPrefix) {
		this.addTellPrefix = addTellPrefix;
	}

	/**
	 * Sets a custom prefix used in tell messages for this command.
	 * This overrides {@link Common#getTellPrefix()} however won't work if
	 * {@link #addTellPrefix} is disabled
	 *
	 * @param tellPrefix
	 */
	protected final void setTellPrefix(final String tellPrefix) {
		this.tellPrefix = tellPrefix;
	}

	/**
	 * Sets the minimum number of arguments to run this command
	 *
	 * @param minArguments
	 */
	protected final void setMinArguments(final int minArguments) {
		Valid.checkBoolean(minArguments >= 0, "Minimum arguments must be 0 or greater");

		this.minArguments = minArguments;
	}

	/**
	 * Set the time before the same player can execute this command again
	 *
	 * @param cooldown
	 * @param unit
	 */
	protected final void setCooldown(final int cooldown, final TimeUnit unit) {
		Valid.checkBoolean(cooldown >= 0, "Cooldown must be >= 0 for /" + getLabel());

		cooldownSeconds = (int) unit.toSeconds(cooldown);
	}

	/**
	 * Set a custom cooldown message, by default we use the one found in {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 * <p>
	 * Use {duration} to dynamically replace the remaining time
	 *
	 * @param cooldownMessage
	 */
	protected final void setCooldownMessage(final String cooldownMessage) {
		this.cooldownMessage = cooldownMessage;
	}

	/**
	 * Get the permission for this command, either the one you set or our from Localization
	 */
	@Override
	public final String getPermissionMessage() {
		return Common.getOrDefault(super.getPermissionMessage(), "&c" + SimpleLocalization.NO_PERMISSION);
	}

	/**
	 * By default we check if the player has the permission you set in setPermission.
	 * <p>
	 * If that is null, we check for the following:
	 * {yourpluginname}.command.{label} for {@link SimpleCommand}
	 * {yourpluginname}.command.{label}.{sublabel} for {@link SimpleSubCommand}
	 * <p>
	 * We handle lacking permissions automatically and return with an no-permission message
	 * when the player lacks it.
	 *
	 * @return
	 */
	@Override
	public final String getPermission() {
		return super.getPermission() == null ? null : replaceBasicPlaceholders0(super.getPermission());
	}

	/**
	 * Get the permission without replacing {plugin_name}, {label} or {sublabel}
	 *
	 * @return
	 * @deprecated internal use only
	 */
	@Deprecated
	protected final String getRawPermission() {
		return super.getPermission();
	}

	/**
	 * Sets the permission required for this command to run. If you set the
	 * permission to null we will not require any permission (unsafe).
	 *
	 * @param
	 */
	@Override
	public final void setPermission(final String permission) {
		super.setPermission(permission);
	}

	/**
	 * Get the sender of this command
	 *
	 * @return
	 */
	protected final CommandSender getSender() {
		Valid.checkNotNull(sender, "Sender cannot be null");

		return sender;
	}

	/**
	 * Get aliases for this command
	 */
	@Override
	public final List<String> getAliases() {
		return super.getAliases();
	}

	/**
	 * Get description for this command
	 */
	@Override
	public final String getDescription() {
		return super.getDescription();
	}

	/**
	 * Get the name of this command
	 */
	@Override
	public final String getName() {
		return super.getName();
	}

	/**
	 * Get the usage message of this command
	 */
	@Override
	public final String getUsage() {
		final String bukkitUsage = super.getUsage();

		return bukkitUsage.equals("/" + getMainLabel()) ? "" : bukkitUsage;
	}

	/**
	 * Get the most recent label for this command
	 */
	@Override
	public final String getLabel() {
		return label;
	}

	/**
	 * Get the label given when the command was created or last updated with {@link #setLabel(String)}
	 *
	 * @return
	 */
	public final String getMainLabel() {
		return super.getLabel();
	}

	/**
	 * Updates the label of this command
	 */
	@Override
	public final boolean setLabel(final String label) {
		this.label = label;

		return super.setLabel(label);
	}

	/**
	 * Set whether we automatically show usage params in {@link #getMinArguments()}
	 * and when the first arg == "help" or "?"
	 * <p>
	 * True by default
	 *
	 * @param autoHandleHelp
	 */
	protected final void setAutoHandleHelp(final boolean autoHandleHelp) {
		this.autoHandleHelp = autoHandleHelp;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SimpleCommand ? ((SimpleCommand) obj).getLabel().equals(getLabel()) && ((SimpleCommand) obj).getAliases().equals(getAliases()) : false;
	}

	@Override
	public final String toString() {
		return "Command{label=/" + label + "}";
	}
}
