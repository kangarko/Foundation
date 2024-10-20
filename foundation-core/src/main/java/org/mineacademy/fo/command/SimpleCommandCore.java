package org.mineacademy.fo.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.ExpiringMap;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * A simple command used to replace all Bukkit/Spigot command functionality
 * across any plugin that utilizes this.
 */
public abstract class SimpleCommandCore {

	/**
	 * The pattern to match a command argument, see {@link #colorizeUsage(SimpleComponent)}.
	 */
	private static final Pattern PATTERN_TABLE = Pattern.compile("<(.*?)>");

	/**
	 * The pattern to match a command argument, see {@link #colorizeUsage(SimpleComponent)}.
	 */
	private static final Pattern PATTERN_FILTER = Pattern.compile("\\[(.*?)]");

	/**
	 * The pattern to match a command argument, see {@link #colorizeUsage(SimpleComponent)}.
	 */
	private static final Pattern PATTERN_DASH = Pattern.compile("-\\s.*$");

	/**
	 * An empty list used to disable tab completion.
	 */
	protected static final List<String> NO_COMPLETE = Collections.EMPTY_LIST;

	/**
	 * The cooldown times before executing the command again. This map
	 * stores the executors name and his last execution of the command.
	 */
	private final ExpiringMap<String /* Command Sender Name */, Long /* Last Execution Timestamp */> lastExecutedTimes = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	/**
	 * The command label, eg. boss for /boss.
	 */
	private final String label;

	/**
	 * Command aliases.
	 */
	private List<String> aliases = new ArrayList<>();

	/**
	 * The command usage.
	 */
	private SimpleComponent usage = null;

	/**
	 * The command description.
	 */
	private SimpleComponent description = null;

	/**
	 * Has this command been already registered?
	 */
	private boolean registered = false;

	/**
	 * The {@link CommonCore#getTellPrefix()} custom prefix only used for sending messages in {@link #onCommand()} method
	 * for this command, null to use the one in Common#getTellPrefix or empty to force no prefix.
	 */
	private SimpleComponent tellPrefix = null;

	/**
	 * Minimum arguments required to run this command.
	 */
	@Getter
	private int minArguments = 0;

	/**
	 * Maximum arguments this command can have, or -1 for unlimited.
	 */
	@Getter
	private int maxArguments = -1;

	/**
	 * The command cooldown before we can run this command again.
	 */
	@Getter
	private int cooldownSeconds = 0;

	/**
	 * A custom message when the player attempts to run this command
	 * within {@link #cooldownSeconds}.
	 * <p>
	 * TIP: Use {duration} to replace the remaining time till next run.
	 */
	private SimpleComponent cooldownMessage = null;

	/**
	 * The permission to run this command. Set to null to always allow.
	 *
	 * Defaults to {@literal <plugin_name>.command.<label>}
	 */
	private String permission = null;

	/**
	 * The permission message to send when the player does not have the permission.
	 */
	private SimpleComponent permissionMessage = null;

	/**
	 * If we should automatically send the usage message when the first subargument
	 * of the command equals to "help" or "?".
	 */
	private boolean autoHandleHelp = true;

	// ----------------------------------------------------------------------
	// Temporary variables
	// ----------------------------------------------------------------------

	/**
	 * The source of this command, or null if does not exist
	 * <p>
	 * This variable is updated dynamically when the command is run with the
	 * last known sender
	 */
	protected FoundationPlayer audience;

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
		this.label = label;

		if (aliases != null)
			this.aliases = aliases;

		this.permission = Platform.getPlugin().getName().toLowerCase() + ".command." + label;
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

		Platform.registerCommand(this, unregisterOldCommand, unregisterOldAliases);

		this.registered = true;
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

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * Execute this command, updates the sender, label and args variables,
	 * checks permission and returns if the sender lacks it,
	 * checks minimum arguments and finally passes the command to the child class.
	 *
	 * @deprecated internal use only
	 *
	 * @param audience
	 * @param label
	 * @param args
	 *
	 * @return
	 */
	@Deprecated
	public final boolean delegateExecute(final FoundationPlayer audience, final String label, final String[] args) {
		if (!Platform.getPlugin().isEnabled()) {
			audience.sendMessage(Lang.component("command-cannot-use-while-plugin-disabled"));

			return true;
		}

		this.audience = audience;
		this.args = args;

		try {
			if (this.getPermission() != null)
				this.checkPerm(this.getPermission());

			if (this.cooldownSeconds > 0)
				this.handleCooldown();

			if (args.length < this.getMinArguments() || this.autoHandleHelp && args.length == 1 && ("help".equals(args[0]) || "?".equals(args[0]))) {
				final String[] legacyUsage = this.getMultilineUsageMessage();
				final SimpleComponent[] newUsage = this.getMultilineUsage();

				if (legacyUsage != null || newUsage != null)
					this.tellNoPrefix("<dark_gray>" + CommonCore.chatLineSmooth());

				if (this.getDescription() != null)
					this.tellNoPrefix(Lang.componentVars("command-label-description", "description", this.getDescription()));

				if (legacyUsage != null || newUsage != null || this.getUsage() != null) {
					this.tellNoPrefix(Lang.componentVars("command-label-usage", "usage", SimpleComponent.fromPlain(this.getEffectiveCommand() + " ").append(CommonCore.getOrDefault(this.getUsage(), SimpleComponent.empty()))));

					if (legacyUsage != null || newUsage != null) {
						this.tellNoPrefix("<dark_gray>" + CommonCore.chatLineSmooth());

						if (legacyUsage != null)
							for (final String legacyLine : legacyUsage)
								audience.sendMessage(this.replacePlaceholders(this.colorizeUsage(SimpleComponent.fromMini(legacyLine))));

						else if (newUsage != null)
							for (final SimpleComponent newLine : newUsage)
								audience.sendMessage(this.replacePlaceholders(this.colorizeUsage(newLine)));

						this.tellNoPrefix("<dark_gray>" + CommonCore.chatLineSmooth());
					}
				}

				return true;
			}

			if (this.getMaxArguments() != -1 && args.length > this.getMaxArguments())
				this.returnInvalidArgs(this.joinArgs(this.getMaxArguments()));

			this.onCommand();

		} catch (final Throwable throwable) {
			this.handleCommandError(throwable);
		}

		return true;
	}

	/*
	 * Handle the command error, send the error message to the player and log.
	 */
	private void handleCommandError(Throwable throwable) {
		if (throwable instanceof InvalidCommandArgException) {
			this.tellError(Lang.componentVars("command-invalid-argument",
					"arguments", ((InvalidCommandArgException) throwable).getInvalidArgument(),
					"help_command", SimpleComponent.fromPlain(this.getEffectiveCommand() + " ?").onHover("Click to execute.").onClickRunCmd(this.getEffectiveCommand() + " ?")));

		} else if (throwable instanceof CommandException) {
			((CommandException) throwable).sendErrorMessage(audience);

		} else {
			this.tellError(Lang.component("command-error"));

			CommonCore.error(throwable, "Error executing " + this.getEffectiveCommand() + " " + String.join(" ", args));
		}
	}

	/*
	 * Get the effective command with sublabel if applicable
	 */
	private String getEffectiveCommand() {
		return "/" + this.getLabel() + (this instanceof SimpleSubCommandCore ? " " + ((SimpleSubCommandCore) this).getSublabel() : "");
	}

	/**
	 * Check if the command cooldown is active and if the command
	 * is run within the given limit, we stop it and inform the player
	 */
	private void handleCooldown() {
		if (!this.isCooldownApplied(this.audience))
			return;

		final long lastRun = this.lastExecutedTimes.getOrDefault(this.audience.getName(), 0L);
		final long difference = (System.currentTimeMillis() - lastRun) / 1000;

		// Check if the command was run earlier within the wait threshold
		if (lastRun != 0)
			this.checkBoolean(difference > this.cooldownSeconds, CommonCore.getOrDefault(this.cooldownMessage, Lang.component("command-cooldown-wait")).replaceBracket("duration", String.valueOf(this.cooldownSeconds - difference + 1)));

		// Update the last try with the current time
		this.lastExecutedTimes.put(this.audience.getName(), System.currentTimeMillis());
	}

	/**
	 * Override this if you need to customize if the specific player should have the cooldown
	 * for this command.
	 *
	 * @param audience
	 * @return
	 */
	protected boolean isCooldownApplied(final FoundationPlayer audience) {
		return true;
	}

	/**
	 * Executed when the command is run. You can get the variables sender and args directly,
	 * and use convenience checks in the simple command class.
	 */
	protected abstract void onCommand();

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
	protected final void checkPerm(@NonNull final String permission) throws CommandException {
		if (!this.hasPerm(permission))
			throw new CommandException(this.getPermissionMessage().replaceBracket("permission", permission));
	}

	/**
	 * Checks if the given sender has the given permission
	 *
	 * @param audience
	 * @param permission
	 * @throws CommandException
	 */
	protected final void checkPerm(@NonNull final FoundationPlayer audience, @NonNull final String permission) throws CommandException {
		if (!audience.hasPermission(permission))
			throw new CommandException(this.getPermissionMessage().replaceBracket("permission", permission));
	}

	/**
	 * Checks if the given sender has the given permission
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(final int minimumLength, final String falseMessage) throws CommandException {
		this.checkArgs(minimumLength, SimpleComponent.fromMini(falseMessage));
	}

	/**
	 * Check if the command arguments are of the minimum length
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(final int minimumLength, final SimpleComponent falseMessage) throws CommandException {
		if (this.args.length < minimumLength)
			this.returnTell(falseMessage);
	}

	/**
	 * Checks if the given boolean is true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	public final void checkBoolean(final boolean value, final String falseMessage) throws CommandException {
		this.checkBoolean(value, SimpleComponent.fromMini(falseMessage));
	}

	/**
	 * Checks if the given boolean is true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	public final void checkBoolean(final boolean value, final SimpleComponent falseMessage) throws CommandException {
		if (!value)
			this.returnTell(falseMessage);
	}

	/**
	 * Checks if the server is running at least the given version
	 *
	 * @param minimumVersion
	 */
	protected final void checkServerVersion(V minimumVersion) {
		this.checkBoolean(MinecraftVersion.atLeast(minimumVersion), Lang.componentVars("command-incompatible", "version", minimumVersion.toString()));
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
			this.returnTell(Lang.componentVars("command-invalid-usage", "usage", this.usage));
	}

	/**
	 * Checks if the given object is not null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	public final void checkNotNull(final Object value, final String messageIfNull) throws CommandException {
		this.checkNotNull(value, SimpleComponent.fromMini(messageIfNull));
	}

	/**
	 * Checks if the given object is not null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	public final void checkNotNull(final Object value, final SimpleComponent messageIfNull) throws CommandException {
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
			return SimpleTime.fromString(raw);

		} catch (final IllegalArgumentException ex) {
			this.returnTell(Lang.componentVars("command-invalid-time", "input", raw));

			return null;
		}
	}

	/**
	 * Attempts to convert the given input (such as "1h50m30s") into seconds
	 *
	 * @param text
	 * @return
	 */
	protected final long findTimeMillis(final String text) {
		try {
			return TimeUtil.toMilliseconds(text);

		} catch (final IllegalArgumentException ex) {
			throw new CommandException(Lang.componentVars("command-invalid-time-token", "input", text));
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
		return this.findEnum(enumType, enumValue, null, Lang.component("command-invalid-type"));
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
	 * @param falseMessage
	 *
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue, final SimpleComponent falseMessage) throws CommandException {
		return this.findEnum(enumType, enumValue, null, falseMessage);
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
	 * @param falseMessage
	 *
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue, final String falseMessage) throws CommandException {
		return this.findEnum(enumType, enumValue, null, SimpleComponent.fromMini(falseMessage));
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
	 *
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue, final Function<T, Boolean> condition) throws CommandException {
		return this.findEnum(enumType, enumValue, condition, Lang.component("command-invalid-type"));
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
	 * @param falseMessage
	 *
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue, final Function<T, Boolean> condition, final SimpleComponent falseMessage) throws CommandException {
		T found = null;

		try {
			found = ReflectionUtil.lookupEnum(enumType, enumValue);

			if (condition != null)
				if (!condition.apply(found))
					found = null;

		} catch (final Throwable t) {
			// Not found, pass through below to error out
		}

		this.checkNotNull(found, falseMessage
				.replaceBracket("type", enumType.getSimpleName().replaceAll("([a-z])([A-Z]+)", "$1 $2").toLowerCase())
				.replaceBracket("value", enumValue)
				.replaceBracket("available", CommonCore.join(enumType.getEnumConstants(), constant -> constant.name().toLowerCase())));

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
		return this.findNumber(index, SimpleComponent.fromMini(falseMessage));
	}

	/**
	 * A convenience method for parsing a number at the given args index
	 *
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final SimpleComponent falseMessage) {
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
		return this.findNumber(index, min, max, SimpleComponent.fromMini(falseMessage));
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
	protected final int findNumber(final int index, final int min, final int max, final SimpleComponent falseMessage) {
		return this.findNumber(Integer.class, index, min, max, falseMessage);
	}

	/*
	 * A convenience method for parsing any number type that is between two bounds
	 * Number can be of any type, that supports method valueOf(String)
	 * You can use {min} and {max} in the message to be automatically replaced
	 */
	private final <T extends Number & Comparable<T>> T findNumber(final Class<T> numberType, final int index, final T min, final T max, SimpleComponent falseMessage) {
		falseMessage = falseMessage.replaceBracket("min", String.valueOf(min)).replaceBracket("max", String.valueOf(max));

		final T number = this.findNumber(numberType, index, falseMessage);
		this.checkBoolean(number.compareTo(min) >= 0 && number.compareTo(max) <= 0, falseMessage);

		return number;
	}

	/*
	 * A convenience method for parsing any number type at the given args index
	 * Number can be of any type, that supports method valueOf(String)
	 */
	private final <T extends Number> T findNumber(final Class<T> numberType, final int index, final SimpleComponent falseMessage) {
		this.checkBoolean(index < this.args.length, falseMessage);

		try {
			return (T) numberType.getMethod("valueOf", String.class).invoke(null, this.args[index]); // Method valueOf is part of all main Number sub classes, eg. Short, Integer, Double, etc.
		}

		catch (final IllegalAccessException | NoSuchMethodException ex) {
			ex.printStackTrace();

		} catch (final InvocationTargetException ex) {

			// Print stack trace for all exceptions, except NumberFormatException
			// NumberFormatException is expected to happen, in this case we just want to display falseMessage without stack trace
			if (!(ex.getCause() instanceof NumberFormatException))
				ex.printStackTrace();
		}

		throw new CommandException(this.replacePlaceholders(falseMessage.replaceBracket("value", this.args[index])));
	}

	/**
	 * A convenience method for parsing a boolean at the given args index
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final boolean findBoolean(final int index, final String invalidMessage) {
		return this.findBoolean(index, SimpleComponent.fromMini(invalidMessage));
	}

	/**
	 * A convenience method for parsing a boolean at the given args index
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final boolean findBoolean(final int index, final SimpleComponent invalidMessage) {
		this.checkUsage(index < this.args.length);

		if (this.args[index].equalsIgnoreCase("true"))
			return true;

		else if (this.args[index].equalsIgnoreCase("false"))
			return false;

		this.returnTell(invalidMessage);
		return false;
	}

	/**
	 * A convenience method for parsing a UUID at the given args index.
	 *
	 * Throws invalid with the default command-invalid-uuid lang message.
	 *
	 * @param index
	 * @return
	 */
	protected final UUID findUUID(final int index) {
		return this.findUUID(index, Lang.component("command-invalid-uuid"));
	}

	/**
	 * A convenience method for parsing a UUID at the given args index.
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final UUID findUUID(final int index, final String invalidMessage) {
		return this.findUUID(index, SimpleComponent.fromMini(invalidMessage));
	}

	/**
	 * A convenience method for parsing a UUID at the given args index.
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final UUID findUUID(final int index, final SimpleComponent invalidMessage) {
		this.checkUsage(index < this.args.length);

		UUID uuid = null;

		try {
			uuid = UUID.fromString(this.args[index]);

		} catch (final IllegalArgumentException ex) {
			this.returnTell(invalidMessage.replaceBracket("uuid", this.args[index]));
		}

		return uuid;
	}

	// ----------------------------------------------------------------------
	// Other checks
	// ----------------------------------------------------------------------

	/**
	 * A convenience check for quickly determining if the sender has a given
	 * permission. Returns true if the permission is null.
	 *
	 * TIP: For a more complete check use {@link #checkPerm(String)} that
	 * will automatically return your command if they lack the permission.
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(final String permission) {
		return permission == null || this.audience.hasPermission(permission);
	}

	// ----------------------------------------------------------------------
	// Messaging
	// ----------------------------------------------------------------------

	/**
	 * Sends a message to the player without the specific tell prefix set
	 * for this command.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @see #setTellPrefix(String)
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(String... messages) {
		final SimpleComponent oldLocalPrefix = this.tellPrefix;

		this.tellPrefix = null;

		for (final String message : messages)
			this.tell(message);

		this.tellPrefix = oldLocalPrefix;
	}

	/**
	 * Sends a message to the player without the specific tell prefix set
	 * for this command.
	 *
	 * @see #setTellPrefix(String)
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 */
	protected final void tellNoPrefix(final SimpleComponent component) {
		final SimpleComponent oldLocalPrefix = this.tellPrefix;

		this.tellPrefix = null;

		this.tell(component);

		this.tellPrefix = oldLocalPrefix;
	}

	/**
	 * Sends a message to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param message
	 */
	protected final void tell(String message) {
		this.tell(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends a message to the player.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 * @param component
	 */
	public final void tell(SimpleComponent component) {
		component = this.replacePlaceholders(component);

		this.audience.sendMessage(this.tellPrefix != null ? this.tellPrefix.append(component) : component);
	}

	/**
	 * Sends a success message to the player.
	 *
	 * @see Messenger#success(FoundationPlayer, String)
	 *
	 * @param component
	 */
	protected final void tellSuccess(String component) {
		this.tellSuccess(SimpleComponent.fromMini(component));
	}

	/**
	 * Sends a success message to the player.
	 *
	 * @see Messenger#success(FoundationPlayer, SimpleComponent)
	 *
	 * @param component
	 */
	protected final void tellSuccess(SimpleComponent component) {
		Messenger.success(this.audience, this.replacePlaceholders(component));
	}

	/**
	 * Sends an info message to the player.
	 *
	 * @see Messenger#info(FoundationPlayer, String)
	 *
	 * @param message
	 */
	public final void tellInfo(String message) {
		this.tellInfo(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends an info message to the player.
	 *
	 * @see Messenger#info(FoundationPlayer, SimpleComponent)
	 *
	 * @param component
	 */
	public final void tellInfo(SimpleComponent component) {
		Messenger.info(this.audience, this.replacePlaceholders(component));
	}

	/**
	 * Sends a warning message to the player.
	 *
	 * @see Messenger#warn(FoundationPlayer, String)
	 *
	 * @param message
	 */
	protected final void tellWarn(String message) {
		this.tellWarn(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends a warning message to the player.
	 *
	 * @see Messenger#warn(FoundationPlayer, SimpleComponent)
	 *
	 * @param component
	 */
	protected final void tellWarn(SimpleComponent component) {
		if (component != null)
			Messenger.warn(this.audience, this.replacePlaceholders(component));
	}

	/**
	 * Sends an error message to the player.
	 *
	 * @see Messenger#error(FoundationPlayer, String)
	 *
	 * @param message
	 */
	protected final void tellError(String message) {
		this.tellError(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends an error message to the player.
	 *
	 * @see Messenger#error(FoundationPlayer, SimpleComponent)
	 *
	 * @param component
	 */
	protected final void tellError(SimpleComponent component) {
		Messenger.error(this.audience, this.replacePlaceholders(component));
	}

	/**
	 * Sends a question-prefixed message to the player.
	 *
	 * @see Messenger#question(FoundationPlayer, String)
	 *
	 * @param message
	 */
	protected final void tellQuestion(String message) {
		this.tellQuestion(SimpleComponent.fromMini(message));
	}

	/**
	 * Sends a question-prefixed message to the player.
	 *
	 * @see Messenger#question(FoundationPlayer, SimpleComponent)
	 *
	 * @param component
	 */
	protected final void tellQuestion(SimpleComponent component) {
		Messenger.question(this.audience, this.replacePlaceholders(component));
	}

	/**
	 * Convenience method for returning the invalid arguments message for the player.
	 */
	protected final void returnInvalidArgs(String invalidArgs) {
		throw new InvalidCommandArgException(invalidArgs);
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param messages
	 * @throws CommandException
	 */
	public final void returnTell(final String... messages) throws CommandException {
		final List<SimpleComponent> components = new ArrayList<>();

		for (final String message : messages)
			components.add(this.replacePlaceholders(SimpleComponent.fromMini(message)));

		throw new CommandException(components.toArray(new SimpleComponent[components.size()]));
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param component
	 * @throws CommandException
	 */
	public final void returnTell(final SimpleComponent component) throws CommandException {
		throw new CommandException(this.replacePlaceholders(component));
	}

	// ----------------------------------------------------------------------
	// Placeholder
	// ----------------------------------------------------------------------

	/**
	 * Replaces placeholders in the message. By default, we replace
	 * {label}, {sublabel} (if applicable), {player} and {X} with the
	 * arguments where X is the index of the argument.
	 *
	 * @param component
	 * @return
	 */
	protected SimpleComponent replacePlaceholders(SimpleComponent component) {
		component = component
				.replaceBracket("plugin_name", Platform.getPlugin().getName())
				.replaceBracket("plugin_version", Platform.getPlugin().getVersion())
				.replaceBracket("label", this.label)
				.replaceBracket("player", this.audience.getName());

		for (int i = 0; i < this.args.length; i++)
			component = component.replaceBracket(String.valueOf(i), this.args[i]);

		return component;
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end.
	 *
	 * @param from
	 * @return
	 */
	protected final String[] rangeArgs(final int from) {
		return this.rangeArgs(from, this.args.length);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to the given end.
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
	 * to their end joined by spaces.
	 *
	 * @param from
	 * @return
	 */
	protected final String joinArgs(final int from) {
		return this.joinArgs(from, this.args.length);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to the given end joined by spaces.
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
	 * Show tab completion suggestions.
	 *
	 * Tab completion is only shown if the sender has the appropriate command permission.
	 *
	 * @param audience
	 * @param label
	 * @param args
	 *
	 * @deprecated internal use only
	 * @return
	 */
	@Deprecated
	public final List<String> delegateTabComplete(final FoundationPlayer audience, final String label, final String[] args) {
		this.audience = audience;
		this.args = args;

		if (this.hasPerm(this.getPermission())) {
			final List<String> suggestions = this.tabComplete();

			return suggestions == null ? NO_COMPLETE : suggestions;
		}

		return NO_COMPLETE;
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
	 * and that are not vanished.
	 *
	 * This is overriden in platform-specific implementations to exclude vanished players,
	 * i.e. on Bukkit we support CMI, Essentials and other plugins.
	 *
	 * TIP: You can simply return null for the same behaviour.
	 *
	 * @return
	 */
	public List<String> completeLastWordPlayerNames() {
		return CommonCore.tabComplete(this.getLastArg(), CommonCore.convertList(Platform.getOnlinePlayers(), FoundationPlayer::getName));
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
		return CommonCore.tabComplete(this.getLastArg(), suggestions);
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

		return CommonCore.tabComplete(this.getLastArg(), list.toArray());
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

		return CommonCore.tabComplete(this.getLastArg(), list.toArray());
	}

	/**
	 * Convenience method for returning the last word in arguments.
	 *
	 * @return
	 */
	public final String getLastArg() {
		return this.args.length > 0 ? this.args[this.args.length - 1] : "";
	}

	// ----------------------------------------------------------------------
	// Temporary variables and safety
	// ----------------------------------------------------------------------

	/**
	 * Sets a custom prefix used in tell messages for this command.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param tellPrefix
	 */
	protected final void setTellPrefix(final String tellPrefix) {
		this.setTellPrefix(SimpleComponent.fromMini(tellPrefix));
	}

	/**
	 * Sets a custom prefix used in tell messages for this command.
	 *
	 * @param tellPrefix
	 */
	protected final void setTellPrefix(final SimpleComponent tellPrefix) {
		this.tellPrefix = tellPrefix;
	}

	/**
	 * Shortcut method for setting the min-max arguments range
	 * to automatically perform command argument validation.
	 *
	 * @param min
	 * @param max
	 */
	protected final void setValidArguments(int min, int max) {
		this.setMinArguments(min);
		this.setMaxArguments(max);
	}

	/**
	 * Sets the minimum number of arguments to run this command.
	 *
	 * @param minArguments
	 */
	protected final void setMinArguments(final int minArguments) {
		ValidCore.checkBoolean(minArguments >= 0, "Minimum arguments must be 0 or greater");

		this.minArguments = minArguments;
	}

	/**
	 * Sets the maximum number of arguments to run this command.
	 *
	 * @param maxArguments
	 */
	protected final void setMaxArguments(final int maxArguments) {
		ValidCore.checkBoolean(maxArguments >= 0, "Maximum arguments must be 0 or greater");
		ValidCore.checkBoolean(maxArguments >= this.minArguments, "Maximum arguments must be >= minimum arguments, got " + maxArguments + " < " + this.minArguments + " for " + this);

		this.maxArguments = maxArguments;
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
	 * Set the cooldown message for this command.
	 *
	 * @return
	 */
	protected final SimpleComponent getCooldownMessage() {
		return cooldownMessage;
	}

	/**
	 * Set a custom cooldown message.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * Use {duration} to dynamically replace the remaining time.
	 *
	 * @param cooldownMessage
	 */
	protected final void setCooldownMessage(final String cooldownMessage) {
		this.cooldownMessage = SimpleComponent.fromMini(cooldownMessage);
	}

	/**
	 * Set a custom cooldown message.
	 *
	 * Use {duration} to dynamically replace the remaining time.
	 *
	 * @param cooldownMessage
	 */
	protected final void setCooldownMessage(final SimpleComponent cooldownMessage) {
		this.cooldownMessage = cooldownMessage;
	}

	/**
	 * Get the permission for this command. If null, we return the "no-permission"
	 * key from the localization.
	 *
	 * @see Lang
	 */
	protected final SimpleComponent getPermissionMessage() {
		return CommonCore.getOrDefault(this.permissionMessage, Lang.component("no-permission"));
	}

	/**
	 * Set the permission message.
	 *
	 * @param permissionMessage
	 */
	protected final void setPermissionMessage(SimpleComponent permissionMessage) {
		this.permissionMessage = permissionMessage;
	}

	/**
	 * By default we check if the player has the permission you set in {@link #setPermission(String)}
	 *
	 * Defaults to {@literal <plugin_name>.command.<label>}.
	 *
	 * @return
	 */
	protected final String getPermission() {
		return this.permission;
	}

	/**
	 * Sets the permission required for this command to run. If you set the
	 * permission to null we will not require any permission.
	 *
	 * Defaults to {@literal <plugin_name>.command.<label>}. Variables in permission are not supported.
	 *
	 * @param permission
	 */
	protected final void setPermission(final String permission) {
		this.permission = permission;
	}

	/**
	 * Get the last sender of this command, might be null if the command was never executed.
	 *
	 * @return
	 */
	public final FoundationPlayer getAudience() {
		ValidCore.checkNotNull(this.audience, "Sender cannot be null");

		return this.audience;
	}

	/**
	 * Get aliases for this command.
	 *
	 * @return
	 */
	public final List<String> getAliases() {
		return this.aliases;
	}

	/**
	 * Set the command aliases.
	 *
	 * @param aliases
	 */
	protected final void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	/**
	 * Get description for this command.
	 *
	 * @return
	 */
	public final SimpleComponent getDescription() {
		return this.description;
	}

	/**
	 * Get the usage message of this command.
	 *
	 * @return
	 */
	public final SimpleComponent getUsage() {
		return this.usage;
	}

	/**
	 * Get a custom multilined usage message to be shown instead of the one line one.
	 * Defaults to null.
	 *
	 * @return the multiline custom usage message, or null
	 */
	protected SimpleComponent[] getMultilineUsage() {
		return null;
	}

	/**
	 * Get a custom usage message to be shown instead of the one line one. This is
	 * prioritized over {@link #getMultilineUsage()}. Defaults to null.
	 *
	 * @return
	 */
	protected String[] getMultilineUsageMessage() {
		return null;
	}

	/**
	 * Get the most recent label for this command.
	 *
	 * @return
	 */
	public final String getLabel() {
		return this.label;
	}

	/**
	 * Set whether we automatically show usage params in {@link #getMinArguments()}
	 * and when the first arg == "help" or "?".
	 * <p>
	 * True by default
	 *
	 * @param autoHandleHelp
	 */
	protected final void setAutoHandleHelp(final boolean autoHandleHelp) {
		this.autoHandleHelp = autoHandleHelp;
	}

	/**
	 * Set the command usage.
	 *
	 * @param usage
	 */
	protected final void setUsage(String usage) {
		this.usage = usage == null || usage.isEmpty() ? null : SimpleComponent.fromMini(usage);
	}

	/**
	 * Replace <> and [] with appropriate color codes, you can return the given string
	 * without modification to disable this functionality.
	 *
	 * @param usage
	 * @return
	 */
	final SimpleComponent colorizeUsage(SimpleComponent usage) {
		return usage.replaceMatch(PATTERN_TABLE, (match, result) -> {
			return result.color(NamedTextColor.GOLD);

		}).replaceMatch(PATTERN_FILTER, (match, result) -> {
			return result.color(NamedTextColor.DARK_GREEN);

		}).replaceMatch(PATTERN_DASH, (match, result) -> {
			return result.color(NamedTextColor.GRAY);
		});
	}

	/**
	 * Set the command usage.
	 *
	 * @param usage
	 */
	protected final void setUsage(SimpleComponent usage) {
		this.usage = usage == null || usage.isEmpty() ? null : usage;
	}

	/**
	 * Set the command label.
	 *
	 * @param description
	 */
	protected final void setDescription(String description) {
		this.description = description == null || description.isEmpty() ? null : SimpleComponent.fromMini(description);
	}

	/**
	 * Set the command description.
	 *
	 * @param description
	 */
	protected final void setDescription(SimpleComponent description) {
		this.description = description == null || description.isEmpty() ? null : description;
	}

	/**
	 * Get the command arguments.
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
	 * where we handle sending messages to player automatically.
	 *
	 * @param runnable
	 * @return
	 */
	public final Task runTask(final Runnable runnable) {
		return runTask(0, runnable);
	}

	/**
	 * Runs the given task later, this supports checkX methods
	 * where we handle sending messages to player automatically.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final Task runTask(final int delayTicks, final Runnable runnable) {
		return Platform.runTask(delayTicks, () -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically.
	 *
	 * @param runnable
	 * @return
	 */
	public final Task runTaskAsync(final Runnable runnable) {
		return this.runTaskAsync(0, runnable);
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically.
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final Task runTaskAsync(final int delayTicks, final Runnable runnable) {
		return Platform.runTaskAsync(delayTicks, () -> this.delegateTask(runnable));
	}

	/*
	 * A helper method to catch command-related exceptions from runnables
	 */
	private void delegateTask(final Runnable runnable) {
		try {
			runnable.run();

		} catch (final Throwable throwable) {
			this.handleCommandError(throwable);
		}
	}

	/**
	 * Return true if the object is a command whose label and aliases equal this one.
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SimpleCommandCore ? ((SimpleCommandCore) obj).getLabel().equals(this.getLabel()) && ((SimpleCommandCore) obj).getAliases().equals(this.getAliases()) : false;
	}

	@Override
	public String toString() {
		return "Command{/" + this.label + "}";
	}

	/**
	 * Thrown when a command has invalid argument
	 */
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private final class InvalidCommandArgException extends CommandException {
		private static final long serialVersionUID = 1L;
		private final String invalidArgument;
	}
}