package org.mineacademy.fo.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TabUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.command.SimpleCommandGroup.MainCommand;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.exception.InvalidCommandArgException;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleTime;
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
	 * Denotes an empty list used to disable tab-completion
	 */
	protected static final List<String> NO_COMPLETE = Collections.unmodifiableList(new ArrayList<>());

	/**
	 * Return the default permission syntax
	 *
	 * @return
	 */
	protected static final String getDefaultPermission() {
		return SimplePlugin.getNamed().toLowerCase() + ".command.{label}";
	}

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
	 * The {@link Common#getTellPrefix()} custom prefix only used for sending messages in {@link #onCommand()} method
	 * for this command, null to use the one in Common#getTellPrefix or empty to force no prefix.
	 */
	private String tellPrefix = null;

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

		// NOTICE:
		// Any usages of "this instanceof" is considered a poor quality code. The reason we use it
		// is that we know for certain these parent classes exist, and we need to navigate
		// developers on their proper usage. We recommend you avoid using "this instanceof" if possible.
		Valid.checkBoolean(!(this instanceof CommandExecutor), "Please do not write 'implements CommandExecutor' for /" + super.getLabel() + " cmd, we already have a listener there");
		Valid.checkBoolean(!(this instanceof TabCompleter), "Please do not write 'implements TabCompleter' for /" + super.getLabel() + " cmd, simply override tabComplete method");

		setLabel(label);

		if (aliases != null)
			setAliases(aliases);

		// Set a default permission for this command
		setPermission(getDefaultPermission());
	}

	/*
	 * Split the given label by | and get the first part, used as the main label
	 */
	private static String parseLabel0(final String label) {
		Valid.checkNotNull(label, "Label must not be null!");

		return label.split("(\\||\\/)")[0];
	}

	/*
	 * Split the given label by | and use the second and further parts as aliases
	 */
	private static List<String> parseAliases0(final String label) {
		final String[] aliases = label.split("(\\||\\/)");

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
		this.register(true, unregisterOldAliases);
	}

	/**
	 * Registers this command into Bukkit.
	 * <p>
	 * Throws an error if the command {@link #isRegistered()} already.
	 *
	 * @param unregisterOldCommand Unregister old command if exists with the same label?
	 * @param unregisterOldAliases If a command with the same label is already present, should
	 *                             we remove associated aliases with the old command? This solves a problem
	 *                             in ChatControl where unregistering /tell from the Essentials plugin would also
	 *                             unregister /t from Towny, which is undesired.
	 */
	public final void register(final boolean unregisterOldCommand, final boolean unregisterOldAliases) {
		Valid.checkBoolean(!(this instanceof SimpleSubCommand), "Sub commands cannot be registered!");
		Valid.checkBoolean(!registered, "The command /" + getLabel() + " has already been registered!");

		if (!canRegister())
			return;

		final PluginCommand oldCommand = Bukkit.getPluginCommand(getLabel());

		if (oldCommand != null && unregisterOldCommand) {
			final String owningPlugin = oldCommand.getPlugin().getName();

			if (!owningPlugin.equals(SimplePlugin.getNamed()))
				Debugger.debug("command", "Command /" + getLabel() + " already (" + owningPlugin + "), overriding and unregistering /" + oldCommand.getLabel() + ", /" + String.join(", /", oldCommand.getAliases()));

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

	/**
	 * Return true if this command can be registered through {@link #register()} methods.
	 * By default true.
	 *
	 * @return
	 */
	protected boolean canRegister() {
		return true;
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * Execute this command, updates the sender, label and args variables,
	 * checks permission and returns if the sender lacks it,
	 * checks minimum arguments and finally passes the command to the child class.
	 * <p>
	 * Also contains various error handling scenarios
	 */
	@Override
	public final boolean execute(final CommandSender sender, final String label, final String[] args) {

		if (SimplePlugin.isReloading() || !SimplePlugin.getInstance().isEnabled()) {
			Common.tell(sender, SimpleLocalization.Commands.USE_WHILE_NULL.replace("{state}", SimplePlugin.isReloading() ? SimpleLocalization.Commands.RELOADING : SimpleLocalization.Commands.DISABLED));

			return false;
		}

		// Set variables to re-use later
		this.sender = sender;
		this.label = label;
		this.args = args;

		// Set tell prefix only if the parent setting was on
		final String oldTellPrefix = Common.getTellPrefix();

		if (this.tellPrefix != null)
			Common.setTellPrefix(this.tellPrefix);

		// Optional sublabel if this is a sub command
		final String sublabel = this instanceof SimpleSubCommand ? " " + ((SimpleSubCommand) this).getSublabel() : "";

		// Catch "errors" that contain a message to send to the player
		// Measure performance of all commands
		final String lagSection = "Command /" + getLabel() + sublabel + (args.length > 0 ? " " + String.join(" ", args) : "");

		try {
			// Prevent duplication since MainCommand delegates this
			if (!(this instanceof MainCommand))
				LagCatcher.start(lagSection);

			// Check if sender has the proper permission
			if (getPermission() != null)
				checkPerm(getPermission());

			// Check for minimum required arguments and print help
			if (args.length < getMinArguments() || autoHandleHelp && args.length == 1 && ("help".equals(args[0]) || "?".equals(args[0]))) {

				Common.runAsync(() -> {
					final String usage = getMultilineUsageMessage() != null ? String.join("\n&c", getMultilineUsageMessage()) : getUsage() != null ? getUsage() : null;
					Valid.checkNotNull(usage, "getUsage() nor getMultilineUsageMessage() not implemented for '/" + getLabel() + sublabel + "' command!");

					final ChatPaginator paginator = new ChatPaginator(SimpleLocalization.Commands.HEADER_SECONDARY_COLOR);
					final List<String> pages = new ArrayList<>();

					if (!Common.getOrEmpty(getDescription()).isEmpty()) {
						pages.add(replacePlaceholders(SimpleLocalization.Commands.LABEL_DESCRIPTION));
						pages.add(replacePlaceholders("&c" + getDescription()));
					}

					if (getMultilineUsageMessage() != null) {
						pages.add("");
						pages.add(replacePlaceholders(SimpleLocalization.Commands.LABEL_USAGES));

						for (final String usagePart : usage.split("\n"))
							pages.add(replacePlaceholders("&c" + usagePart));

					} else {
						pages.add("");
						pages.add(SimpleLocalization.Commands.LABEL_USAGE);
						pages.add("&c" + replacePlaceholders("/" + label + sublabel + (!usage.startsWith("/") ? " " + Common.stripColors(usage) : "")));
					}

					paginator
							.setFoundationHeader(SimpleLocalization.Commands.LABEL_HELP_FOR.replace("{label}", getLabel() + sublabel))
							.setPages(Common.toArray(pages));

					// Force sending on the main thread
					Common.runLater(() -> paginator.send(sender));
				});

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

		} catch (final EventHandledException ex) {
			if (ex.getMessages() != null)
				dynamicTellError(ex.getMessages());

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				dynamicTellError(ex.getMessages());

		} catch (final Throwable t) {
			dynamicTellError(SimpleLocalization.Commands.ERROR.replace("{error}", t.toString()));

			Common.error(t, "Failed to execute command /" + getLabel() + sublabel + " " + String.join(" ", args));

		} finally {
			Common.setTellPrefix(oldTellPrefix);

			// Prevent duplication since MainCommand delegates this
			if (!(this instanceof MainCommand))
				LagCatcher.end(lagSection, 8, "{section} took {time} ms");
		}

		return true;
	}

	/*
	 * If messenger is on, we send the message prefixed with Messenger.getErrorPrefix()
	 * otherwise we just send a normal message
	 */
	private void dynamicTellError(final String... messages) {
		if (Messenger.ENABLED)
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
	 * Checks if the current sender has the given permission
	 *
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull final String perm) throws CommandException {
		if (isPlayer() && !hasPerm(perm))
			throw new CommandException(getPermissionMessage().replace("{permission}", perm));
	}

	/**
	 * Checks if the given sender has the given permission
	 *
	 * @param sender
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull CommandSender sender, @NonNull final String perm) throws CommandException {
		if (isPlayer() && !hasPerm(sender, perm))
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
			returnTell((Messenger.ENABLED ? "" : "&c") + falseMessage);
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
			returnTell((Messenger.ENABLED ? "" : "&c") + falseMessage);
	}

	/**
	 * Check if the given boolean is true or returns {@link #returnInvalidArgs()}
	 *
	 * @param value
	 *
	 * @throws CommandException
	 */
	protected final void checkUsage(final boolean value) throws CommandException {
		if (!value)
			returnInvalidArgs();
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
			returnTell((Messenger.ENABLED ? "" : "&c") + messageIfNull);
	}

	/**
	 * Attempts to find the offline player by name, sends an error message to sender if he did not play before
	 * or runs the specified callback.
	 *
	 * The offline player lookup is done async, the callback is synchronized.
	 *
	 * @param name
	 * @param callback
	 * @throws CommandException
	 */
	protected final void findOfflinePlayer(final String name, Consumer<OfflinePlayer> callback) throws CommandException {
		runAsync(() -> {
			final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(name);
			checkBoolean(targetPlayer != null && (targetPlayer.isOnline() || targetPlayer.hasPlayedBefore()), SimpleLocalization.Player.NOT_PLAYED_BEFORE.replace("{player}", name));

			runLater(() -> callback.accept(targetPlayer));
		});
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
		final Player player = findPlayerInternal(name);
		checkBoolean(player != null && player.isOnline() && !PlayerUtil.isVanished(player), falseMessage.replace("{player}", name));

		return player;
	}

	/**
	 * Return the player by the given name, and, when the name is null, return the sender if sender is player.
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayerOrSelf(final String name) throws CommandException {
		if (name == null) {
			checkBoolean(isPlayer(), SimpleLocalization.Commands.CONSOLE_MISSING_PLAYER_NAME);

			return getPlayer();
		}

		final Player player = findPlayerInternal(name);
		checkBoolean(player != null && player.isOnline(), SimpleLocalization.Player.NOT_ONLINE.replace("{player}", name));

		return player;
	}

	/**
	 * A simple call to Bukkit.getPlayer(name) meant to be overriden
	 * if you have a custom implementation of getting players by name.
	 *
	 * Example use: ChatControl can find players by their nicknames too
	 *
	 * @param name
	 * @return
	 */
	protected Player findPlayerInternal(String name) {
		return Bukkit.getPlayer(name);
	}

	/**
	 * Attempts to convert the given input (such as 1 hour) into
	 * a {@link SimpleTime} object
	 *
	 * @param raw
	 * @return
	 */
	protected final SimpleTime findTime(String raw) {
		try {
			return SimpleTime.from(raw);

		} catch (final IllegalArgumentException ex) {
			returnTell(SimpleLocalization.Commands.INVALID_TIME.replace("{input}", raw));

			return null;
		}
	}

	/**
	 * Attempts to convert the given name into a bukkit world,
	 * sending localized error message if such world does not exist.
	 *
	 * @param name
	 * @return
	 */
	protected final World findWorld(String name) {
		final World world = Bukkit.getWorld(name);

		checkNotNull(world, SimpleLocalization.Commands.INVALID_WORLD.replace("{world}", name).replace("{available}", Common.join(Bukkit.getWorlds())));
		return world;
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
		return this.findEnum(enumType, name, null, falseMessage);
	}

	/**
	 * Finds an enumeration of a certain type, if it fails it prints a false message to the player
	 * You can use the {enum} variable in the false message for the name parameter
	 *
	 * You can also use the condition to filter certain enums and act as if they did not existed
	 * if your function returns false for such
	 *
	 * @param <T>
	 * @param enumType
	 * @param name
	 * @param condition
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String name, final Function<T, Boolean> condition, final String falseMessage) throws CommandException {
		T found = null;

		try {
			found = ReflectionUtil.lookupEnum(enumType, name);

			if (!condition.apply(found))
				found = null;

		} catch (final Throwable t) {
			// Not found, pass through below to error out
		}

		checkNotNull(found, falseMessage.replace("{enum}", name).replace("{available}", Common.join(enumType.getEnumConstants())));
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

		catch (final IllegalAccessException | NoSuchMethodException e) {
			e.printStackTrace();

		} catch (final InvocationTargetException e) {

			// Print stack trace for all exceptions, except NumberFormatException
			// NumberFormatException is expected to happen, in this case we just want to display falseMessage without stack trace
			if (!(e.getCause() instanceof NumberFormatException))
				e.printStackTrace();
		}

		throw new CommandException(replacePlaceholders((Messenger.ENABLED ? "" : "&c") + falseMessage));
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

		throw new CommandException(replacePlaceholders((Messenger.ENABLED ? "" : "&c") + invalidMessage));
	}

	// ----------------------------------------------------------------------
	// Other checks
	// ----------------------------------------------------------------------

	/**
	 * A convenience check for quickly determining if the sender has a given
	 * permission.
	 *
	 * TIP: For a more complete check use {@link #checkPerm(String)} that
	 * will automatically return your command if they lack the permission.
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(String permission) {
		return this.hasPerm(sender, permission);
	}

	/**
	 * A convenience check for quickly determining if the sender has a given
	 * permission.
	 *
	 * TIP: For a more complete check use {@link #checkPerm(String)} that
	 * will automatically return your command if they lack the permission.
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(CommandSender sender, String permission) {
		return permission == null ? true : PlayerUtil.hasPerm(sender, permission.replace("{label}", getLabel()));
	}

	// ----------------------------------------------------------------------
	// Messaging
	// ----------------------------------------------------------------------

	/**
	 * Sends a message to the player
	 *
	 * @see Replacer#replaceArray
	 *
	 * @param message
	 * @param replacements
	 */
	protected final void tellReplaced(final String message, final Object... replacements) {
		tell(Replacer.replaceArray(message, replacements));
	}

	/**
	 * Sends a interactive chat component to the sender, not replacing any special
	 * variables just executing the {@link SimpleComponent#send(CommandSender...)} method
	 * as a shortcut
	 *
	 * @param components
	 */
	protected final void tell(List<SimpleComponent> components) {
		if (components != null)
			tell(components.toArray(new SimpleComponent[components.size()]));
	}

	/**
	 * Sends a interactive chat component to the sender, not replacing any special
	 * variables just executing the {@link SimpleComponent#send(CommandSender...)} method
	 * as a shortcut
	 *
	 * @param components
	 */
	protected final void tell(SimpleComponent... components) {
		if (components != null)
			for (final SimpleComponent component : components)
				component.send(sender);
	}

	/**
	 * Send a list of messages to the player
	 *
	 * @param messages
	 */
	protected final void tell(Collection<String> messages) {
		if (messages != null)
			tell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a multiline message to the player without plugin's prefix.
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(Collection<String> messages) {
		tellNoPrefix(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a multiline message to the player without plugin's prefix.
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(String... messages) {
		final String oldLocalPrefix = this.tellPrefix;

		this.tellPrefix = "";

		tell(messages);

		this.tellPrefix = oldLocalPrefix;
	}

	/**
	 * Sends a multiline message to the player, avoiding prefix if 3 lines or more
	 *
	 * @param messages
	 */
	protected final void tell(String... messages) {

		if (messages == null)
			return;

		final String oldTellPrefix = Common.getTellPrefix();

		if (this.tellPrefix != null)
			Common.setTellPrefix(this.tellPrefix);

		try {
			messages = replacePlaceholders(messages);

			if (messages.length > 2) {
				Common.tellNoPrefix(sender, messages);

			} else
				Common.tell(sender, messages);

		} finally {
			Common.setTellPrefix(oldTellPrefix);
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
	 * Internal method for replacing {label} and {sublabel}
	 *
	 * @param message
	 * @return
	 */
	private String replaceBasicPlaceholders0(final String message) {
		return message
				.replace("{label}", getLabel())
				.replace("{sublabel}", this instanceof SimpleSubCommand ? ((SimpleSubCommand) this).getSublabels()[0] : super.getLabel());
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
	 * @deprecated do not use
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
	 * Convenience method for completing all player names that the sender can see
	 * and that are not vanished
	 * <p>
	 * TIP: You can simply return null for the same behaviour
	 *
	 * @return
	 */
	protected List<String> completeLastWordPlayerNames() {
		return TabUtil.complete(getLastArg(), isPlayer() ? Common.getPlayerNames(false) : Common.getPlayerNames());
	}

	/**
	 * Convenience method for completing all world names
	 *
	 * @return
	 */
	protected List<String> completeLastWordWorldNames() {
		return this.completeLastWord(Common.getWorldNames());
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
	@SafeVarargs
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
	 * Convenience method for automatically completing the last word
	 * with the given suggestions converting them to a string. We sort them and only select ones
	 * that the last word starts with.
	 *
	 * @param <T>
	 * @param suggestions
	 * @param toString
	 * @return
	 */
	protected final <T> List<String> completeLastWord(final Iterable<T> suggestions, Function<T, String> toString) {
		final List<String> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(toString.apply(suggestion));

		return TabUtil.complete(getLastArg(), list.toArray());
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
		return Common.getOrDefault(super.getPermissionMessage(), SimpleLocalization.NO_PERMISSION);
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
	 * Get the permission without replacing variables
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
	 * @param permission
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

	// ----------------------------------------------------------------------
	// Scheduling
	// ----------------------------------------------------------------------

	/**
	 * Runs the given task later, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runLater(Runnable runnable) {
		return Common.runLater(() -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task later, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runLater(int delayTicks, Runnable runnable) {
		return Common.runLater(delayTicks, () -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runAsync(Runnable runnable) {
		return Common.runAsync(() -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runAsync(int delayTicks, Runnable runnable) {
		return Common.runLaterAsync(delayTicks, () -> this.delegateTask(runnable));
	}

	/*
	 * A helper method to catch command-related exceptions from runnables
	 */
	private void delegateTask(Runnable runnable) {
		try {
			runnable.run();

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				for (final String message : ex.getMessages()) {
					if (Messenger.ENABLED)
						Messenger.error(sender, message);
					else
						Common.tell(sender, message);
				}

		} catch (final Throwable t) {
			final String errorMessage = SimpleLocalization.Commands.ERROR.replace("{error}", t.toString());

			if (Messenger.ENABLED)
				Messenger.error(sender, errorMessage);
			else
				Common.tell(sender, errorMessage);

			throw t;
		}
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
