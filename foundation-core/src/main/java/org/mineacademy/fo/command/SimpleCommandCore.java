package org.mineacademy.fo.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MessengerCore;
import org.mineacademy.fo.ReflectionUtilCore;
import org.mineacademy.fo.TabUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.command.SimpleCommandGroup.MainCommand;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidCommandArgException;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.RemainCore;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * A simple command used to replace all Bukkit/Spigot command functionality
 * across any plugin that utilizes this.
 */
public abstract class SimpleCommandCore {

	/**
	 * Denotes an empty list used to disable tab-completion
	 */
	protected static final List<String> NO_COMPLETE = Collections.unmodifiableList(new ArrayList<>());

	/**
	 * The default permission syntax, {pluginName}.command.{label}
	 */
	private static String defaultPermission = Platform.getPlugin().getName().toLowerCase() + ".command.{label}";

	/**
	 * Return the default permission syntax
	 *
	 * @return
	 */
	public static String getDefaultPermission() {
		return defaultPermission;
	}

	/**
	 * Set the default permission syntax
	 *
	 * @param permission
	 */
	public static void setDefaultPermission(final String permission) {
		defaultPermission = permission;
	}

	/**
	 * You can set the cooldown time before executing the command again. This map
	 * stores the player uuid and his last execution of the command.
	 */
	private final ExpiringMap<Audience, Long> cooldownMap = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	/**
	 * The command label, eg. boss for /boss
	 */
	private final String label;

	/**
	 * The command label used at last command execution
	 *
	 * This variable is updated dynamically when the command is run with the
	 * last known version of the label, e.g. /boss or /b will set it to boss or b
	 * respectively
	 */
	private String currentLabel;

	/**
	 * Command aliases
	 */
	private List<String> aliases = new ArrayList<>();

	/**
	 * The command usage
	 */
	private Component usage = null;

	/**
	 * The command description
	 */
	private Component description = null;

	/**
	 * Has this command been already registered?
	 */
	private boolean registered = false;

	/**
	 * The {@link CommonCore#getTellPrefix()} custom prefix only used for sending messages in {@link #onCommand()} method
	 * for this command, null to use the one in Common#getTellPrefix or empty to force no prefix.
	 */
	private Component tellPrefix = null;

	/**
	 * Minimum arguments required to run this command
	 */
	@Getter
	private int minArguments = 0;

	/**
	 * The command cooldown before we can run this command again
	 */
	@Getter
	private int cooldownSeconds = 0;

	/**
	 * The permission for players to bypass this command's cooldown, if it is set
	 */
	@Getter
	private String cooldownBypassPermission = null;

	/**
	 * A custom message when the player attempts to run this command
	 * within {@link #cooldownSeconds}. By default we use the one found in
	 * {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 * <p>
	 * TIP: Use {duration} to replace the remaining time till next run
	 */
	private Component cooldownMessage = null;

	/**
	 * The permission to run this command. Set to null to always allow.
	 *
	 * Defaults to {@link #getDefaultPermission()}
	 */
	private String permission = null;

	/**
	 * The permission message to send when the player does not have the permission,
	 * defaults to SimpleLocalization.Commands.NO_PERMISSION
	 */
	private Component permissionMessage = null;

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
	protected Audience sender;

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
	protected SimpleCommandCore(final String label) {
		this(parseLabel0(label), parseAliases0(label));
	}

	/**
	 * Create a new simple command from the list. The first
	 * item in the list is the main label and the other ones are the aliases.
	 */
	protected SimpleCommandCore(final List<String> labels) {
		this(parseLabelList0(labels), labels.size() > 1 ? labels.subList(1, labels.size()) : null);
	}

	/**
	 * Create a new simple command
	 *
	 * @param label
	 * @param aliases
	 */
	protected SimpleCommandCore(final String label, final List<String> aliases) {
		Platform.checkCommandUse(this);

		this.label = label;
		this.currentLabel = label;

		if (aliases != null)
			this.aliases = aliases;

		// Set a default permission for this command
		this.permission = defaultPermission;
	}

	/*
	 * Split the given label by | and get the first part, used as the main label
	 */
	private static String parseLabel0(final String label) {
		ValidCore.checkNotNull(label, "Label must not be null!");

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
	private static String parseLabelList0(final List<String> labels) {
		ValidCore.checkBoolean(!labels.isEmpty(), "Command label must not be empty!");

		return labels.get(0);
	}

	// ----------------------------------------------------------------------
	// Registration
	// ----------------------------------------------------------------------

	/**
	 * Registers this command into Bukkit.
	 *
	 * Throws an error if the command is registered already.
	 */
	public final void register() {
		this.register(true);
	}

	/**
	 * Registers this command into Bukkit.
	 *
	 * Throws an error if the command is registered already.
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
	 *
	 * Throws an error if the command is registered already.
	 *
	 * @param unregisterOldCommand Unregister old command if exists with the same label?
	 * @param unregisterOldAliases If a command with the same label is already present, should
	 *                             we remove associated aliases with the old command? This solves a problem
	 *                             in ChatControl where unregistering /tell from the Essentials plugin would also
	 *                             unregister /t from Towny, which is undesired.
	 */
	public final void register(final boolean unregisterOldCommand, final boolean unregisterOldAliases) {
		ValidCore.checkBoolean(!(this instanceof SimpleSubCommandCore), "Sub commands cannot be registered!");
		ValidCore.checkBoolean(!this.registered, "The command /" + this.getLabel() + " has already been registered!");

		if (this.canRegister()) {
			Platform.registerCommand(this, unregisterOldCommand, unregisterOldAliases);

			this.registered = true;
		}
	}

	/**
	 * Removes the command from Bukkit.
	 * <p>
	 * Throws an error if the command is not registered.
	 */
	public final void unregister() {
		ValidCore.checkBoolean(!(this instanceof SimpleSubCommandCore), "Sub commands cannot be unregistered!");
		ValidCore.checkBoolean(this.registered, "The command /" + this.getLabel() + " is not registered!");

		Platform.unregisterCommand(this);
		this.registered = false;
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
	 *
	 * Also contains various error handling scenarios
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public final boolean delegateExecute(final Audience sender, final String label, final String[] args) {

		if (Platform.getPlugin().isReloading() || !Platform.getPlugin().isEnabled()) {
			CommonCore.tell(sender, SimpleLocalization.Commands.CANNOT_USE_WHILE_NULL.replaceText(b -> b.matchLiteral("{state}").replacement(Platform.getPlugin().isReloading() ? SimpleLocalization.Commands.RELOADING : SimpleLocalization.Commands.DISABLED)));

			return false;
		}

		// Set variables to re-use later
		this.sender = sender;
		this.currentLabel = label;
		this.args = args;

		// Optional sublabel if this is a sub command
		final String sublabel = this instanceof SimpleSubCommandCore ? " " + ((SimpleSubCommandCore) this).getSublabel() : "";

		// Catch "errors" that contain a message to send to the player
		// Measure performance of all commands
		final String lagSection = "Command /" + this.getLabel() + sublabel + (args.length > 0 ? " " + String.join(" ", args) : "");

		try {
			// Prevent duplication since MainCommand delegates this
			if (!(this instanceof MainCommand))
				LagCatcher.start(lagSection);

			// Check if sender has the proper permission
			if (this.getPermission() != null)
				this.checkPerm(this.getPermission());

			// Check for minimum required arguments and print help
			if (args.length < this.getMinArguments() || this.autoHandleHelp && args.length == 1 && ("help".equals(args[0]) || "?".equals(args[0]))) {
				final List<Component> messages = new ArrayList<>();

				if (this.getDescription() != null) {
					Component descriptionLabel = SimpleLocalization.Commands.LABEL_DESCRIPTION;
					final String descriptionPlain = RemainCore.convertAdventureToPlain(descriptionLabel);

					Component description = this.getDescription();

					if (description.color() == null)
						description = description.color(NamedTextColor.RED);

					if (!descriptionPlain.contains("{description}"))
						descriptionLabel = descriptionLabel.append(description);
					else
						descriptionLabel = descriptionLabel.replaceText(TextReplacementConfig.builder().matchLiteral("{description}").replacement(description).build());

					messages.add(descriptionLabel);
				}

				if (this.getMultilineUsage() != null) {
					messages.add(SimpleLocalization.Commands.LABEL_USAGE);

					Component usage = this.getMultilineUsage();

					if (usage.color() == null)
						usage = usage.color(NamedTextColor.RED);

					messages.add(usage);

				} else if (this.getUsage() != null) {
					Component usage = this.getUsage();
					final String usagePlain = RemainCore.convertAdventureToPlain(usage);

					if (usage.color() == null)
						usage = usage.color(NamedTextColor.RED);

					if (!usagePlain.startsWith("/"))
						usage = Component.text("/{label} " + (this instanceof SimpleSubCommandCore ? "{sublabel} " : "")).append(usage);

					messages.add(usage);

				} else
					throw new FoException("Either getUsage() or getMultilineUsageMessage() must be implemented for '/" + this.getLabel() + sublabel + "' command!");

				for (final Component message : messages)
					CommonCore.tellNoPrefix(sender, this.replacePlaceholders(message));

				return true;
			}

			// Check if we can run this command in time
			if (this.cooldownSeconds > 0)
				this.handleCooldown();

			this.onCommand();

		} catch (final InvalidCommandArgException ex) {
			if (this.getMultilineUsage() == null) {
				this.dynamicTellError(ex.getComponent() != null ? ex.getComponent() : SimpleLocalization.Commands.INVALID_SUB_ARGUMENT);

			} else {
				this.dynamicTellError(SimpleLocalization.Commands.INVALID_ARGUMENT_MULTILINE);

				this.tellNoPrefix(this.getMultilineUsage());
			}

		} catch (final CommandException ex) {
			if (ex.getComponent() != null)
				this.dynamicTellError(ex.getComponent());

		} catch (final Throwable t) {
			this.dynamicTellError(SimpleLocalization.Commands.ERROR.replaceText(b -> b.matchLiteral("{error}").replacement(t.toString())));

			CommonCore.error(t, "Failed to execute command /" + this.getLabel() + sublabel + " " + String.join(" ", args));

		} finally {

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
	/*private void dynamicTellError(final String... messages) {
		if (MessengerCore.ENABLED)
			for (final String message : messages)
				this.tellError(message);
		else
			this.tell(messages);
	}*/

	/*
	 * If messenger is on, we send the message prefixed with Messenger.getErrorPrefix()
	 * otherwise we just send a normal message
	 */
	private void dynamicTellError(final Component component) {
		if (MessengerCore.ENABLED)
			this.tellError(component);
		else
			this.tell(component);
	}

	/**
	 * Check if the command cooldown is active and if the command
	 * is run within the given limit, we stop it and inform the player
	 */
	private void handleCooldown() {
		if (this.cooldownBypassPermission != null && this.hasPerm(this.cooldownBypassPermission))
			return;

		if (!this.isCooldownApplied(this.sender))
			return;

		final long lastRun = this.cooldownMap.getOrDefault(this.sender, 0L);
		final long difference = (System.currentTimeMillis() - lastRun) / 1000;

		// Check if the command was run earlier within the wait threshold
		if (lastRun != 0)
			this.checkBoolean(difference > this.cooldownSeconds, CommonCore.getOrDefault(this.cooldownMessage, SimpleLocalization.Commands.COOLDOWN_WAIT)
					.replaceText(b -> b.matchLiteral("{duration}").replacement(String.valueOf(this.cooldownSeconds - difference + 1))));

		// Update the last try with the current time
		this.cooldownMap.put(this.sender, System.currentTimeMillis());
	}

	/**
	 * Override this if you need to customize if the specific player should have the cooldown
	 * for this command.
	 *
	 * @param audience
	 * @return
	 */
	protected boolean isCooldownApplied(final Audience audience) {
		return true;
	}

	/**
	 * Executed when the command is run. You can get the variables sender and args directly,
	 * and use convenience checks in the simple command class.
	 */
	protected abstract void onCommand();

	/**
	 * Get a custom multilined usage message to be shown instead of the one line one
	 * This calls {@link #getMultilineUsageMessage()} for backwards compatibility
	 *
	 * @return the multiline custom usage message, or null
	 */
	protected Component getMultilineUsage() {
		final String[] legacy = this.getMultilineUsageMessage();

		if (legacy != null) {
			Component component = Component.empty();

			for (int i = 0; i < legacy.length; i++) {
				component = component.append(Component.text(legacy[i]));

				if (i < legacy.length - 1)
					component = component.append(Component.newline());
			}

			return component;
		}

		return null;
	}

	/**
	 * Get a custom usage message to be shown instead of the one line one
	 *
	 * @deprecated use {@link #getMultilineUsage()}
	 * @return
	 */
	@Deprecated
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
	 * Checks if the current sender has the given permission
	 *
	 * @param permission
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull final String permission) throws CommandException {
		if (!this.hasPerm(permission))
			throw new CommandException(this.getPermissionMessage().replaceText(b -> b.matchLiteral("{permission}").replacement(permission)));
	}

	/**
	 * Checks if the given sender has the given permission
	 *
	 * @param sender
	 * @param permission
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull final Audience sender, @NonNull final String permission) throws CommandException {
		if (!this.hasPerm(sender, permission))
			throw new CommandException(this.getPermissionMessage().replaceText(b -> b.matchLiteral("{permission}").replacement(permission)));
	}

	/**
	 * Checks if the given sender has the given permission
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(final int minimumLength, final String falseMessage) throws CommandException {
		this.checkArgs(minimumLength, CommonCore.colorize(falseMessage));
	}

	/**
	 * Check if the command arguments are of the minimum length
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(final int minimumLength, final Component falseMessage) throws CommandException {
		if (this.args.length < minimumLength)
			this.returnTell(falseMessage);
	}

	/**
	 * Convenience method for returning the command with the
	 * message for player if the condition does not meet
	 */
	/*public final void checkArgs(final boolean condition) {
		this.checkBoolean(condition, SimpleLocalization.Commands.INVALID_ARGUMENT.replaceText(b -> b.matchLiteral("{label}").replacement(this.getLabel())));
	}*/

	/**
	 * Checks if the given boolean is true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	public final void checkBoolean(final boolean value, final String falseMessage) throws CommandException {
		this.checkBoolean(value, CommonCore.colorize(falseMessage));
	}

	/**
	 * Checks if the given boolean is true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	public final void checkBoolean(final boolean value, final Component falseMessage) throws CommandException {
		if (!value)
			this.returnTell(falseMessage);
	}

	/**
	 * Check if the given boolean is true or returns {@link #returnInvalidArgs()}
	 *
	 * @param value
	 *
	 * @throws CommandException
	 */
	/*protected final void checkUsage(final boolean value) throws CommandException {
		if (!value)
			this.returnInvalidArgs();
	}*/

	/**
	 * Checks if the given object is not null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	public final void checkNotNull(final Object value, final String messageIfNull) throws CommandException {
		this.checkNotNull(value, CommonCore.colorize(messageIfNull));
	}

	/**
	 * Checks if the given object is not null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	public final void checkNotNull(final Object value, final Component messageIfNull) throws CommandException {
		if (value == null)
			this.returnTell(messageIfNull);
	}

	/**
	 * Attempts to convert the given input (such as 1 hour) into
	 * a {@link SimpleTime} object
	 *
	 * @param raw
	 * @return
	 */
	protected final SimpleTime findTime(final String raw) {
		try {
			return SimpleTime.from(raw);

		} catch (final IllegalArgumentException ex) {
			this.returnTell(SimpleLocalization.Commands.INVALID_TIME.replaceText(b -> b.matchLiteral("{input}").replacement(raw)));

			return null;
		}
	}

	/**
	 * Finds an enumeration of a certain type, if it fails it prints a false message to the player
	 * You can use the {enum} variable in the false message for the name parameter
	 *
	 * @param <T>
	 * @param enumType
	 * @param enumValue
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue) throws CommandException {
		return this.findEnum(enumType, enumValue, null);
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
	 * @param enumValue
	 * @param condition

	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue, final Function<T, Boolean> condition) throws CommandException {
		T found = null;

		try {
			found = ReflectionUtilCore.lookupEnum(enumType, enumValue);

			if (!condition.apply(found))
				found = null;

		} catch (final Throwable t) {
			// Not found, pass through below to error out
		}

		this.checkNotNull(found, SimpleLocalization.Commands.INVALID_ENUM
				.replaceText(b -> b.matchLiteral("{type}").replacement(enumType.getSimpleName().replaceAll("([a-z])([A-Z]+)", "$1 $2").toLowerCase()))
				.replaceText(b -> b.matchLiteral("{value}").replacement(enumValue))
				.replaceText(b -> b.matchLiteral("{available}").replacement(CommonCore.join(enumType.getEnumConstants(), constant -> constant.name().toLowerCase()))));

		return found;
	}

	/**
	 * A convenience method for parsing a number at the given args index
	 *
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final String falseMessage) {
		return this.findNumber(index, CommonCore.colorize(falseMessage));
	}

	/**
	 * A convenience method for parsing a number at the given args index
	 *
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final Component falseMessage) {
		return this.findNumber(Integer.class, index, falseMessage);
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
		return this.findNumber(index, min, max, CommonCore.colorize(falseMessage));
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
	protected final int findNumber(final int index, final int min, final int max, final Component falseMessage) {
		return this.findNumber(Integer.class, index, min, max, falseMessage);
	}

	/*
	 * A convenience method for parsing any number type that is between two bounds
	 * Number can be of any type, that supports method valueOf(String)
	 * You can use {min} and {max} in the message to be automatically replaced
	 */
	private final <T extends Number & Comparable<T>> T findNumber(final Class<T> numberType, final int index, final T min, final T max, Component falseMessage) {
		falseMessage = falseMessage
				.replaceText(b -> b.matchLiteral("{min}").replacement(String.valueOf(min)))
				.replaceText(b -> b.matchLiteral("{max}").replacement(String.valueOf(max)));

		final T number = this.findNumber(numberType, index, falseMessage);
		this.checkBoolean(number.compareTo(min) >= 0 && number.compareTo(max) <= 0, falseMessage);

		return number;
	}

	/*
	 * A convenience method for parsing any number type at the given args index
	 * Number can be of any type, that supports method valueOf(String)
	 */
	private final <T extends Number> T findNumber(final Class<T> numberType, final int index, final Component falseMessage) {
		this.checkBoolean(index < this.args.length, falseMessage);

		try {
			return (T) numberType.getMethod("valueOf", String.class).invoke(null, this.args[index]); // Method valueOf is part of all main Number sub classes, eg. Short, Integer, Double, etc.
		}

		catch (final IllegalAccessException | NoSuchMethodException e) {
			e.printStackTrace();

		} catch (final InvocationTargetException e) {

			// Print stack trace for all exceptions, except NumberFormatException
			// NumberFormatException is expected to happen, in this case we just want to display falseMessage without stack trace
			if (!(e.getCause() instanceof NumberFormatException))
				e.printStackTrace();
		}

		throw new CommandException(this.replacePlaceholders(falseMessage.replaceText(b -> b.matchLiteral("{number}").replacement(this.args[index]))));
	}

	/**
	 * A convenience method for parsing a boolean at the given args index
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final boolean findBoolean(final int index, final String invalidMessage) {
		return this.findBoolean(index, CommonCore.colorize(invalidMessage));
	}

	/**
	 * A convenience method for parsing a boolean at the given args index
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final boolean findBoolean(final int index, final Component invalidMessage) {
		this.checkBoolean(index < this.args.length, invalidMessage);

		if (this.args[index].equalsIgnoreCase("true"))
			return true;

		else if (this.args[index].equalsIgnoreCase("false"))
			return false;

		this.returnTell(invalidMessage);
		return false;
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
	protected final boolean hasPerm(final String permission) {
		return this.hasPerm(this.sender, permission);
	}

	/**
	 * A convenience check for quickly determining if the sender has a given
	 * permission.
	 *
	 * TIP: For a more complete check use {@link #checkPerm(String)} that
	 * will automatically return your command if they lack the permission.
	 *
	 * @param audience
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(final Audience audience, final String permission) {
		return permission == null ? true : Platform.hasPermission(audience, permission.replace("{label}", this.getLabel()));
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
	/*protected final void tellReplaced(final String message, final Object... replacements) {
		this.tell(Replacer.replaceArray(message, replacements));
	}*/

	/**
	 * Sends a interactive chat component to the sender, not replacing any special
	 * variables just executing the {@link SimpleComponentCore#send(CommandSender...)} method
	 * as a shortcut
	 *
	 * @param components
	 */
	/*protected final void tell(final List<SimpleComponentCore> components) {
		if (components != null)
			this.tell(components.toArray(new SimpleComponentCore[components.size()]));
	}*/

	/**
	 * Sends a interactive chat component to the sender, not replacing any special
	 * variables just executing the {@link SimpleComponentCore#send(CommandSender...)} method
	 * as a shortcut
	 *
	 * @param components
	 */
	/*protected final void tell(final SimpleComponentCore... components) {
		if (components != null)
			for (final SimpleComponentCore component : components)
				component.send(this.sender);
	}*/

	/**
	 * Send a list of messages to the player
	 *
	 * @param messages
	 */
	/*protected final void tell(final Collection<String> messages) {
		if (messages != null)
			this.tell(messages.toArray(new String[messages.size()]));
	}*/

	/**
	 * Sends a multiline message to the player without plugin's prefix.
	 *
	 * @param messages
	 */
	/*protected final void tellNoPrefix(final Collection<String> messages) {
		this.tellNoPrefix(messages.toArray(new String[messages.size()]));
	}*/

	/**
	 * Sends a message to the player without plugin's prefix.
	 *
	 * @param message
	 */
	protected final void tellNoPrefix(final String message) {
		this.tellNoPrefix(CommonCore.colorize(message));
	}

	/**
	 * Sends a message to the player without plugin's prefix.
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(final Component message) {
		final Component oldLocalPrefix = this.tellPrefix;

		this.tellPrefix = Component.empty();

		this.tell(message);

		this.tellPrefix = oldLocalPrefix;
	}

	/**
	 * Sends a multiline message to the player, avoiding prefix if 3 lines or more
	 *
	 * @param messages
	 */
	/*protected final void tell(String... messages) {

		if (messages == null)
			return;

		final String oldTellPrefix = CommonCore.getTellPrefix();

		if (this.tellPrefix != null)
			CommonCore.setTellPrefix(this.tellPrefix);

		try {
			messages = this.replacePlaceholders(messages);

			if (messages.length > 2)
				CommonCore.tellNoPrefix(this.sender, messages);
			else
				CommonCore.tell(this.sender, messages);

		} finally {
			CommonCore.setTellPrefix(oldTellPrefix);
		}
	}*/

	/**
	 * Sends a message to the player
	 *
	 * @param message
	 */
	protected final void tell(String message) {
		this.tell(CommonCore.colorize(message));
	}

	/**
	 * Sends a message to the player
	 *
	 * @param component
	 */
	protected final void tell(Component component) {
		if (component == null)
			return;

		final Component oldTellPrefix = CommonCore.getTellPrefix();

		if (this.tellPrefix != null)
			CommonCore.setTellPrefix(this.tellPrefix);

		try {
			component = this.replacePlaceholders(component);

			CommonCore.tell(this.sender, component);

		} finally {
			CommonCore.setTellPrefix(oldTellPrefix);
		}
	}

	/**
	 * Sends a success message to the player
	 *
	 * @param component
	 */
	protected final void tellSuccess(String component) {
		this.tellSuccess(CommonCore.colorize(component));
	}

	/**
	 * Sends a success message to the player
	 *
	 * @param component
	 */
	protected final void tellSuccess(Component component) {
		if (component != null) {
			component = this.replacePlaceholders(component);

			MessengerCore.success(this.sender, component);
		}
	}

	/**
	 * Sends an info message to the player
	 *
	 * @param message
	 */
	protected final void tellInfo(String message) {
		this.tellInfo(CommonCore.colorize(message));
	}

	/**
	 * Sends an info message to the player
	 *
	 * @param message
	 */
	protected final void tellInfo(Component message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			MessengerCore.info(this.sender, message);
		}
	}

	/**
	 * Sends a warning message to the player
	 *
	 * @param message
	 */
	protected final void tellWarn(String message) {
		this.tellWarn(CommonCore.colorize(message));
	}

	/**
	 * Sends a warning message to the player
	 *
	 * @param message
	 */
	protected final void tellWarn(Component message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			MessengerCore.warn(this.sender, message);
		}
	}

	/**
	 * Sends an error message to the player
	 *
	 * @param message
	 */
	protected final void tellError(String message) {
		this.tellError(CommonCore.colorize(message));
	}

	/**
	 * Sends an error message to the player
	 *
	 * @param component
	 */
	protected final void tellError(Component component) {
		if (component != null) {
			component = this.replacePlaceholders(component);

			MessengerCore.error(this.sender, component);
		}
	}

	/**
	 * Sends a question-prefixed message to the player
	 *
	 * @param message
	 */
	protected final void tellQuestion(String message) {
		this.tellQuestion(CommonCore.colorize(message));
	}

	/**
	 * Sends a question-prefixed message to the player
	 *
	 * @param message
	 */
	protected final void tellQuestion(Component message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			MessengerCore.question(this.sender, message);
		}
	}

	/**
	 * Convenience method for returning the command with the {@link SimpleLocalization.Commands#INVALID_ARGUMENT}
	 * message for player
	 */
	protected final void returnInvalidArgs() {
		this.tellError(SimpleLocalization.Commands.INVALID_ARGUMENT.replaceText(b -> b.matchLiteral("{label}").replacement(this.getLabel())));

		throw new CommandException();
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	/*protected final void returnTell(final Collection<String> messages) throws CommandException {
		this.returnTell(messages.toArray(new String[messages.size()]));
	}*/

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param message
	 * @throws CommandException
	 */
	public final void returnTell(final String message) throws CommandException {
		this.returnTell(CommonCore.colorize(message));
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param component
	 * @throws CommandException
	 */
	public final void returnTell(final Component component) throws CommandException {
		throw new CommandException(this.replacePlaceholders(component));
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	/*public final void returnTell(final String... messages) throws CommandException {
		throw new CommandException(this.replacePlaceholders(messages));
	}*/

	/**
	 * Ho ho ho, returns the command usage to the sender
	 *
	 * @throws InvalidCommandArgException
	 */
	public final void returnUsage() throws InvalidCommandArgException {
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
	/*public final String[] replacePlaceholders(final String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = this.replacePlaceholders(messages[i]);

		return messages;
	}

	// TODO get rid of
	protected String replacePlaceholders(String legacy) {
		return RemainCore.convertAdventureToLegacy(replacePlaceholders(RemainCore.convertLegacyToAdventure(legacy)));
	}*/

	/**
	 * Replaces placeholders in the message
	 *
	 * @param component
	 * @return
	 */
	protected Component replacePlaceholders(Component component) {
		component = this.replaceBasicPlaceholders0(component);

		// Replace {X} with arguments
		for (int i = 0; i < this.args.length; i++)
			component = component.replaceText(TextReplacementConfig.builder().matchLiteral("{" + i + "}").replacement(CommonCore.getOrEmpty(this.args[i])).build());

		return component;
	}

	// TODO get rid of
	/*private String replaceBasicPlaceholders0(String component) {
		return RemainCore.convertAdventureToLegacy(this.replaceBasicPlaceholders0(RemainCore.convertLegacyToAdventure(component)));
	}*/

	/**
	 * Internal method for replacing label and sublabel variables
	 *
	 * @param component
	 * @return
	 */
	private Component replaceBasicPlaceholders0(Component component) {

		component = component.replaceText(b -> b.matchLiteral("{label}").replacement(this.label));
		component = component.replaceText(b -> b.matchLiteral("{current_label}").replacement(CommonCore.getOrDefault(this.currentLabel, this.label)));
		component = component.replaceText(b -> b.matchLiteral("{sublabel}").replacement(this instanceof SimpleSubCommandCore ? ((SimpleSubCommandCore) this).getSublabels()[0] : this.args != null && this.args.length > 0 ? this.args[0] : this.label));
		component = component.replaceText(b -> b.matchLiteral("{current_sublabel}").replacement(this instanceof SimpleSubCommandCore ? ((SimpleSubCommandCore) this).getSublabel() : this.args != null && this.args.length > 0 ? this.args[0] : this.label));

		component = Variables.replace(component, this.sender);

		return component;
	}

	/**
	 * Utility method to safely update the args, increasing them if the position is too high
	 * <p>
	 * Used in placeholders
	 *
	 * @param position
	 * @param value
	 */
	/*protected final void setArg(final int position, final String value) {
		if (this.args.length <= position)
			this.args = Arrays.copyOf(this.args, position + 1);

		this.args[position] = value;
	}*/

	/**
	 * Convenience method for returning the last word in arguments
	 *
	 * @return
	 */
	protected final String getLastArg() {
		return this.args.length > 0 ? this.args[this.args.length - 1] : "";
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end
	 *
	 * @param from
	 * @return
	 */
	protected final String[] rangeArgs(final int from) {
		return this.rangeArgs(from, this.args.length);
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
		return Arrays.copyOfRange(this.args, from, to);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end joined by spaces
	 *
	 * @param from
	 * @return
	 */
	protected final String joinArgs(final int from) {
		return this.joinArgs(from, this.args.length);
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

		for (int i = from; i < this.args.length && i < to; i++)
			message += this.args[i] + (i + 1 == this.args.length ? "" : " ");

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
	 * @param label
	 * @param args
	 *
	 * @deprecated internal use only
	 * @return
	 */
	@Deprecated
	public final List<String> delegateTabComplete(final Audience sender, final String label, final String[] args) {
		this.sender = sender;
		this.currentLabel = label;
		this.args = args;

		if (this.hasPerm(this.getPermission())) {
			List<String> suggestions = this.tabComplete();

			// Return online player names when suggestions are null - simulate Bukkit behaviour
			if (suggestions == null)
				suggestions = this.completeLastWordPlayerNames();

			return suggestions;
		}

		return new ArrayList<>();
	}

	/**
	 * Override this method to support tab completing in your command.
	 * <p>
	 * You can then use "sender", "label" or "args" fields from {@link SimpleCommandCore}
	 * class normally and return a list of tab completion suggestions.
	 * <p>
	 * We already check for {@link #getPermission()} and only call this method if the
	 * sender has it.
	 * <p>
	 * TIP: Use {@link #completeLastWord(Iterable)} and {@link #getLastArg()} methods
	 * in {@link SimpleCommandCore} for your convenience
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
	public List<String> completeLastWordPlayerNames() {
		return TabUtil.complete(this.getLastArg(), CommonCore.convert(Platform.getOnlinePlayers(), audience -> Platform.resolveSenderName(audience)));
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
	public final <T> List<String> completeLastWord(final T... suggestions) {
		return TabUtil.complete(this.getLastArg(), suggestions);
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
	public final <T> List<String> completeLastWord(final Iterable<T> suggestions) {
		final List<T> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(suggestion);

		return TabUtil.complete(this.getLastArg(), list.toArray());
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
	protected final <T> List<String> completeLastWord(final Iterable<T> suggestions, final Function<T, String> toString) {
		final List<String> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(toString.apply(suggestion));

		return TabUtil.complete(this.getLastArg(), list.toArray());
	}

	// ----------------------------------------------------------------------
	// Temporary variables and safety
	// ----------------------------------------------------------------------

	/**
	 * Sets a custom prefix used in tell messages for this command.
	 * This overrides {@link CommonCore#getTellPrefix()} however won't work if
	 * {@link #addTellPrefix} is disabled
	 *
	 * @param tellPrefix
	 */
	protected final void setTellPrefix(final Component tellPrefix) {
		this.tellPrefix = tellPrefix;
	}

	/**
	 * Sets the minimum number of arguments to run this command
	 *
	 * @param minArguments
	 */
	protected final void setMinArguments(final int minArguments) {
		ValidCore.checkBoolean(minArguments >= 0, "Minimum arguments must be 0 or greater");

		this.minArguments = minArguments;
	}

	/**
	 * Set the time before the same player can execute this command again
	 *
	 * @param cooldown
	 * @param unit
	 */
	protected final void setCooldown(final int cooldown, final TimeUnit unit) {
		ValidCore.checkBoolean(cooldown >= 0, "Cooldown must be >= 0 for /" + this.getLabel());

		this.cooldownSeconds = (int) unit.toSeconds(cooldown);
	}

	/**
	 * Set the permission to bypass the cooldown, only works if the {@link #setCooldown(int, TimeUnit)} is set
	 *
	 * @param cooldownBypassPermission
	 */
	protected final void setCooldownBypassPermission(final String cooldownBypassPermission) {
		this.cooldownBypassPermission = cooldownBypassPermission;
	}

	/**
	 * Set the cooldown message for this command
	 *
	 * @return
	 */
	protected final Component getCooldownMessage() {
		return cooldownMessage;
	}

	/**
	 * Set a custom cooldown message, by default we use the one found in {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 * <p>
	 * Use {duration} to dynamically replace the remaining time
	 *
	 * @param cooldownMessage
	 */
	protected final void setCooldownMessage(final Component cooldownMessage) {
		this.cooldownMessage = cooldownMessage;
	}

	/**
	 * Get the permission for this command, either the one you set or our from Localization
	 */
	public final Component getPermissionMessage() {
		return CommonCore.getOrDefault(this.permissionMessage, SimpleLocalization.NO_PERMISSION);
	}

	/**
	 * Set the permission message
	 *
	 * @param permissionMessage
	 */
	protected final void setPermissionMessage(Component permissionMessage) {
		this.permissionMessage = permissionMessage;
	}

	/**
	 * By default we check if the player has the permission you set in setPermission.
	 * <p>
	 * If that is null, we check for the following:
	 * {yourpluginname}.command.{label} for {@link SimpleCommandCore}
	 * {yourpluginname}.command.{label}.{sublabel} for {@link SimpleSubCommandCore}
	 * <p>
	 * We handle lacking permissions automatically and return with an no-permission message
	 * when the player lacks it.
	 *
	 * @return
	 */
	public final String getPermission() {
		return this.permission;
	}

	/**
	 * Sets the permission required for this command to run. If you set the
	 * permission to null we will not require any permission (unsafe).
	 *
	 * @param permission
	 */
	protected final void setPermission(final String permission) {
		this.permission = permission;
	}

	/**
	 * Get the sender of this command
	 *
	 * @return
	 */
	public final Audience getSender() {
		ValidCore.checkNotNull(this.sender, "Sender cannot be null");

		return this.sender;
	}

	/**
	 * Get aliases for this command
	 */
	public final List<String> getAliases() {
		return this.aliases;
	}

	/**
	 * Set the command aliases
	 *
	 * @param aliases
	 */
	protected final void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	/**
	 * Get description for this command
	 */
	public final Component getDescription() {
		return this.description == null ? Component.empty() : this.description;
	}

	/**
	 * Get the usage message of this command
	 */
	public final Component getUsage() {
		return this.usage == null ? Component.empty() : this.usage;
	}

	/**
	 * Get the most recent label for this command
	 */
	public final String getLabel() {
		return this.label;
	}

	/**
	 * Get the label given when the command was created or last updated
	 *
	 * @return
	 */
	public final String getCurrentLabel() {
		return CommonCore.getOrDefault(this.currentLabel, this.label);
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

	/**
	 * Set the command usage
	 *
	 * @param usage
	 */
	protected final void setUsage(String usage) {
		this.setUsage(CommonCore.colorize(usage));
	}

	/**
	 * Set the command usage
	 *
	 * @param usage
	 */
	protected final void setUsage(Component usage) {
		this.usage = usage;
	}

	/**
	 * Set the command label
	 *
	 * @param description
	 */
	protected final void setDescription(String description) {
		this.setDescription(CommonCore.colorize(description));
	}

	/**
	 * Set the command description
	 *
	 * @param description
	 */
	protected final void setDescription(Component description) {
		this.description = description;
	}

	/**
	 * Get the command arguments
	 *
	 * @return
	 */
	public final String[] getArgs() {
		return args;
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
	public final Task runLater(final Runnable runnable) {
		return runLater(0, runnable);
	}

	/**
	 * Runs the given task later, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final Task runLater(final int delayTicks, final Runnable runnable) {
		return Platform.runTask(delayTicks, () -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param runnable
	 * @return
	 */
	public final Task runAsync(final Runnable runnable) {
		return this.runAsync(0, runnable);
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final Task runAsync(final int delayTicks, final Runnable runnable) {
		return Platform.runTaskAsync(delayTicks, () -> this.delegateTask(runnable));
	}

	/*
	 * A helper method to catch command-related exceptions from runnables
	 */
	private void delegateTask(final Runnable runnable) {
		try {
			runnable.run();

		} catch (final CommandException ex) {
			if (ex.getComponent() != null)
				if (MessengerCore.ENABLED)
					MessengerCore.error(this.sender, ex.getComponent());
				else
					CommonCore.tell(this.sender, ex.getComponent());

		} catch (final Throwable t) {
			final Component errorMessage = SimpleLocalization.Commands.ERROR.replaceText(b -> b.matchLiteral("{error}").replacement(t.toString()));

			if (MessengerCore.ENABLED)
				MessengerCore.error(this.sender, errorMessage);
			else
				CommonCore.tell(this.sender, errorMessage);

			throw t;
		}
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SimpleCommandCore ? ((SimpleCommandCore) obj).getLabel().equals(this.getLabel()) && ((SimpleCommandCore) obj).getAliases().equals(this.getAliases()) : false;
	}

	@Override
	public String toString() {
		return "Command{/" + this.label + "}";
	}
}
