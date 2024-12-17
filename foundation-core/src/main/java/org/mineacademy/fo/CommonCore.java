package org.mineacademy.fo;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.HandledException;
import org.mineacademy.fo.model.CompChatColor;
import org.mineacademy.fo.model.ConfigStringSerializable;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Our main utility class hosting a large variety of different convenience functions.
 *
 * This is a platform-neutral class, which is extended by "Common" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CommonCore {

	/**
	 * The UUID of the console
	 */
	public static final UUID CONSOLE_UID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	/**
	 * The Google Json instance
	 */
	public final static Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	/**
	 * The Google Json instance with pretty printing
	 */
	public final static Gson GSON_PRETTY = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	/**
	 * Used to send messages to player without repetition, e.g. if they attempt to break a block
	 * in a restricted region, we will not spam their chat with "You cannot break this block here" 120x times,
	 * instead, we only send this message once per X seconds.
	 *
	 * This cache holds the last times when we sent that message.
	 */
	private static final Map<String, Long> TIMED_TELL_CACHE = new LinkedHashMap<>();

	/**
	 * See {@link #TIMED_TELL_CACHE}, but this is for sending messages to the console.
	 */
	private static final Map<String, Long> TIMED_LOG_CACHE = new LinkedHashMap<>();

	/**
	 * Holds a way to convert objects into their string representation, for example
	 * World into world's name. However, World is only available on Bukkit so we need
	 * to set this field on the specific platform and implement there.
	 *
	 * @see #simplify(Object)
	 */
	private static List<Function<Object, String>> simplifiers = new ArrayList<>();

	// ------------------------------------------------------------------------------------------------------------
	// Broadcasting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Broadcast the message to all online players.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param messages
	 */
	public static final void broadcast(final String... messages) {
		for (final String message : messages)
			broadcast(SimpleComponent.fromMiniAmpersand(message));
	}

	/**
	 * Broadcast the message to all online players.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 * @param message
	 */
	public static final void broadcast(final SimpleComponent message) {
		for (final FoundationPlayer audience : Platform.getOnlinePlayers())
			audience.sendMessage(message);
	}

	/**
	 * Broadcast the message to all online players who have the given permission.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param showPermission
	 * @param message
	 */
	public static final void broadcastWithPerm(final String showPermission, @NonNull final String message) {
		broadcastWithPerm(showPermission, SimpleComponent.fromMiniAmpersand(message));
	}

	/**
	 * Broadcast the message to all online players who have the given permission.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param showPermission
	 * @param message
	 */
	public static final void broadcastWithPerm(final String showPermission, @NonNull final SimpleComponent message) {
		for (final FoundationPlayer audience : Platform.getOnlinePlayers())
			if (audience.hasPermission(showPermission))
				audience.sendMessage(message);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Send messages to the given audience, vertically centered and
	 * surrounded by chat-wide line on the top and bottom:
	 *
	 * {@literal ----------------------------------- }
	 *
	 * Hello this is a test!
	 *
	 * {@literal ----------------------------------- }
	 *
	 * You can specify {@literal <center>} among others before the message,
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param messages
	 */
	public static void tellBoxed(final FoundationPlayer audience, final String... messages) {
		tellBoxed(audience, CommonCore.convertArray(messages, SimpleComponent::fromMiniAmpersand));
	}

	/**
	 * Send messages to the given audience, vertically centered and
	 * surrounded by chat-wide line on the top and bottom:
	 *
	 * {@literal ----------------------------------- }
	 *
	 * Hello this is a test!
	 *
	 * {@literal ----------------------------------- }
	 *
	 * You can specify {@literal <center>} among others before the message,
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param messages
	 */
	public static void tellBoxed(final FoundationPlayer audience, final SimpleComponent... messages) {
		final int length = messages.length;

		Platform.runTask(2, () -> {
			audience.sendMessage(SimpleComponent.fromMiniNative("<dark_gray>" + CommonCore.chatLineSmooth()));

			for (int i = 0; i < (length == 1 ? 2 : length == 2 || length == 3 || length == 4 ? 1 : 0); i++)
				audience.sendMessage(SimpleComponent.empty());

			for (final SimpleComponent message : messages)
				audience.sendMessage(message.replaceBracket(audience, "player", audience.getName()));

			for (int i = 0; i < (length == 1 || length == 2 ? 2 : length == 3 ? 1 : 0); i++)
				audience.sendMessage(SimpleComponent.empty());

			audience.sendMessage(SimpleComponent.fromMiniNative("<dark_gray>" + CommonCore.chatLineSmooth()));
		});
	}

	/**
	 * Send messages to the given audience.
	 *
	 * The messages are converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param audience
	 * @param messages
	 */
	public static final void tell(@NonNull final FoundationPlayer audience, final String... messages) {
		for (final String message : messages)
			audience.sendMessage(SimpleComponent.fromMiniAmpersand(message));
	}

	/**
	 * Send a message to the player if it was not sent previously in the given delay.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delaySeconds
	 * @param audience
	 * @param message
	 */
	public static final void tellTimed(final int delaySeconds, final FoundationPlayer audience, final String message) {
		tellTimed(delaySeconds, audience, SimpleComponent.fromMiniAmpersand(message));
	}

	/**
	 * Send a message to the audience if it was not sent previously in the given delay.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delaySeconds
	 * @param audience
	 * @param component
	 */
	public static final void tellTimed(final int delaySeconds, final FoundationPlayer audience, final SimpleComponent component) {
		final String legacy = component.toLegacySection(null);

		if (!TIMED_TELL_CACHE.containsKey(legacy) || TimeUtil.getCurrentTimeSeconds() - TIMED_TELL_CACHE.get(legacy) > delaySeconds) {
			audience.sendMessage(component);

			TIMED_TELL_CACHE.put(legacy, TimeUtil.getCurrentTimeSeconds());
		}
	}

	/**
	 * Send a message to the audience after the given time in ticks.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delayTicks
	 * @param audience
	 * @param message
	 */
	public static final void tellLater(final int delayTicks, final FoundationPlayer audience, final String message) {
		Platform.runTask(delayTicks, () -> audience.sendMessage(SimpleComponent.fromMiniAmpersand(message)));
	}

	/**
	 * Sends a message to the player after the given time in ticks.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delayTicks
	 * @param audience
	 * @param message
	 */
	public static final void tellLater(final int delayTicks, final FoundationPlayer audience, final SimpleComponent message) {
		Platform.runTask(delayTicks, () -> audience.sendMessage(message));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Logging and error handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Log the message if it was not logged previously within the given delay.
	 *
	 * @see #log(String...)
	 *
	 * @param delaySeconds
	 * @param message
	 */
	public static final void logTimed(final int delaySeconds, final String message) {
		if (!TIMED_LOG_CACHE.containsKey(message)) {
			log(message);
			TIMED_LOG_CACHE.put(message, TimeUtil.getCurrentTimeSeconds());
			return;
		}

		if (TimeUtil.getCurrentTimeSeconds() - TIMED_LOG_CACHE.get(message) > delaySeconds) {
			log(message);
			TIMED_LOG_CACHE.put(message, TimeUtil.getCurrentTimeSeconds());
		}
	}

	/**
	 * Log the given messages to the console with {@link #chatLine()} before and after the message.
	 *
	 * @see #log(String...)
	 * @param messages
	 */
	public static final void logFramed(final String... messages) {
		logFramed(false, messages);
	}

	/**
	 * Log the given messages to the console with {@link #chatLine()} before and after the message.
	 * Optionally, disable the plugin after sending.
	 *
	 * @see #log(String...)
	 *
	 * @param disablePlugin
	 * @param messages
	 */
	public static final void logFramed(final boolean disablePlugin, final String... messages) {
		if (messages != null && !ValidCore.isNullOrEmpty(messages)) {
			log("&7" + configLine());

			for (final String message : messages)
				log(" &c" + message);

			if (disablePlugin)
				log(" &cPlugin is now disabled.");

			log("&7" + configLine());
		}

		if (disablePlugin)
			Platform.getPlugin().disable();
	}

	/**
	 * Literally log the message to the console with the "&cWarning: &7" prefix.
	 *
	 * @see #log(String...)
	 *
	 * @param message
	 */
	public static final void warning(final String message) {
		log("&cWarning: &7" + message);
	}

	/**
	 * Log the messages to the console, prepending "[Plugin Name] " as their prefix.
	 *
	 * If the message starts with [JSON], we will parse the message as JSON and log it as legacy text.
	 *
	 * The message array is further splity by \n and each part is logged separately.
	 *
	 * @param messages
	 */
	public static final void log(final String... messages) {
		final String prefix = "[" + Platform.getPlugin().getName() + "] ";

		for (final String message : messages) {
			if (message == null || "none".equals(message))
				continue;

			if (message.replace(" ", "").isEmpty()) {
				Platform.log("  ");

				continue;
			}

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					log(SimpleComponent.fromAdventureJson(stripped, false).toLegacySection(null));

			} else
				for (final String part : message.split("\n"))
					Platform.log(prefix + CompChatColor.translateColorCodes(part));
		}
	}

	/**
	 * Save the root error to error.log file and log the given message in a frame.
	 *
	 * Use the {error} variable to replace it with the actual error message.
	 * This does not throw the exception.
	 *
	 * @see #logFramed(String...)
	 * @see #throwError(Throwable, String...)
	 *
	 * @param throwable
	 * @param messages
	 */
	public static final void error(@NonNull Throwable throwable, final String... messages) {
		if (throwable instanceof HandledException)
			return;

		if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
			throwable = throwable.getCause();

		if (throwable instanceof HandledException)
			return;

		final String message = throwable.getMessage();
		boolean sentry = true;

		// Certain edge cases: Do not report to sentry since it's used-caused and must be solved on his end
		if (message.contains("The database file has been moved since it was opened"))
			sentry = false;

		if (!(throwable instanceof FoException))
			Debugger.saveError(sentry, throwable, messages);

		logFramed(replaceErrorVariable(throwable, messages));
		Debugger.printStackTrace(throwable);
	}

	/**
	 * Save the root error to error.log file and log the given message in a frame,
	 * then throws the exception as unchecked.
	 *
	 * Use the {error} variable to replace it with the actual error message.
	 *
	 * @see #logFramed(String...)
	 *
	 * @param throwable
	 * @param messages
	 */
	public static final void throwError(final Throwable throwable, final String... messages) {
		if (throwable instanceof FoException)
			throw (FoException) throwable;

		if (throwable instanceof HandledException)
			throw (HandledException) throwable;

		Throwable cause = throwable;

		while (cause.getCause() != null)
			cause = cause.getCause();

		// Delegate to only print out the relevant stuff
		if (cause instanceof FoException)
			throw (FoException) cause;

		if (messages != null)
			logFramed(false, replaceErrorVariable(throwable, messages));

		Debugger.saveError(throwable, messages);
		Debugger.printStackTrace(throwable);

		throw new HandledException(throwable);
	}

	/*
	 * Replace the error variable with a smart error info, see above
	 */
	private static String[] replaceErrorVariable(Throwable throwable, final String... messages) {
		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

		for (int i = 0; i < messages.length; i++) {
			final String error = throwableName + throwableMessage;

			messages[i] = messages[i].replace("{error}", error);
		}

		return messages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// GSON
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Convert the given json into list
	 *
	 * @param json
	 * @return
	 */
	public static List<String> convertJsonToList(@NonNull final String json) {
		if (json.isEmpty() || json.equals("[]"))
			return new ArrayList<>();

		final List<String> list = GSON.fromJson(json, List.class);
		ValidCore.checkNotNull(list, "Failed to convert JSON to list: " + json);

		return list;
	}

	/**
	 * Return the given list as JSON
	 *
	 * @param list
	 * @return
	 */
	public static String convertListToJson(@NonNull final Collection<String> list) {
		return GSON.toJson(list);
	}

	/**
	 * Return the given list as JSON
	 *
	 * @param list
	 * @return
	 */
	public static String convertUniqueIdListToJson(final Collection<UUID> list) {
		return GSON.toJson(list);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Aesthetics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the {@literal *---------------------------------------------------*} chat line suitable for
	 * covering the entire chat in default English encoding (no UTF-8).
	 *
	 * @return
	 */
	public static final String chatLine() {
		return "*---------------------------------------------------*";
	}

	/**
	 * Returns the {@literal &m-----------------------------------------------------} smooth chat line suitable for
	 * covering the entire chat in default English encoding (no UTF-8).
	 *
	 * @return
	 */
	public static final String chatLineSmooth() {
		return CompChatColor.STRIKETHROUGH + "-----------------------------------------------------";
	}

	/**
	 * Returns the {@literal -------------------------------------------------------------------------------------------}
	 * config line suitable to be used in YAML files.
	 *
	 * @return
	 */
	public static final String configLine() {
		return "-------------------------------------------------------------------------------------------";
	}

	/**
	 * Duplicate the given text the given amount of times.
	 *
	 * Example: duplicate("apple", 2) will produce "appleapple"
	 *
	 * @param text
	 * @param nTimes
	 * @return
	 */
	public static final String duplicate(final String text, final int nTimes) {
		if (nTimes <= 0)
			return "";

		final StringBuilder builder = new StringBuilder(text.length() * nTimes);

		for (int i = 0; i < nTimes; i++)
			builder.append(text);

		return builder.toString();
	}

	/**
	 * Limit the string to the given length maximum appending "..." at the end if the string is longer.
	 *
	 * @param text
	 * @param maxLength
	 * @return
	 */
	public static final String limit(final String text, final int maxLength) {
		final int length = text.length();

		return maxLength >= length ? text : text.substring(0, maxLength) + "...";
	}

	/**
	 * Split a string into multiple lines, ensuring each line does not exceed a specified maximum length.
	 *
	 * <p>The method intelligently breaks the input string at spaces to avoid splitting words. If one line exceeds
	 * {@code maxLineLength}, it starts a new line. Additionally, it preserves the last color code from Minecraft chat formatting between lines.
	 *
	 * <ul>
	 * <li> For example, calling {@code split("Hello world", 5)} may result in lines like "Hello" and "world".
	 * </ul>
	 *
	 * @param input the string to split
	 * @param maxLineLength the maximum allowed length for each line
	 * @return an array of strings, where each element is a line that fits within the specified length
	 */
	public static final String[] split(final String input, final int maxLineLength) {
		final StringTokenizer tok = new StringTokenizer(input, " ");
		final StringBuilder output = new StringBuilder(input.length());
		int lineLen = 0;
		String lastColorCode = "";

		while (tok.hasMoreTokens()) {
			final String word = tok.nextToken();

			// Remove color codes to calculate the visible length of the word
			final String strippedWord = CompChatColor.stripColorCodes(word);
			final int wordLen = strippedWord.length();

			if (lineLen + wordLen > maxLineLength) {
				output.append("\n").append(lastColorCode);
				lineLen = 0;
			}

			output.append(word).append(" ");
			lineLen += wordLen + 1;

			final String colorCode = CompChatColor.getLastColors(word);

			if (!colorCode.isEmpty())
				lastColorCode = colorCode;
		}

		return output.toString().split("\n");
	}

	/**
	 * Simplify an object and return a readable string representation.
	 *
	 * <p>Handles multiple types of objects such as String, Number, Collection, Enum, and more.
	 * If the object type is unknown, a custom simplifier is used.
	 * <p>Returns an empty string if the object is null.
	 *
	 * <ul>
	 * <li> For a String object, return the string itself.
	 * <li> For a double or float, return a formatted two-decimal string.
	 * <li> For a Collection, join its elements into a comma-separated string.
	 * <li> For a CompChatColor, return its name.
	 * <li> For an Enum, return its lowercase string representation.
	 * <li> For a FoundationPlayer, return the player's name.
	 * <li> For a ConfigStringSerializable, return its serialized string.
	 * </ul>
	 *
	 * You can set a custom simplifier by calling {@link #addSimplifier(Function)} which
	 * will be used for all unknown object types.
	 *
	 * @see #addSimplifier(Function)
	 *
	 * @param object the object to simplify
	 * @return the simplified string representation of the object
	 */
	public static final String simplify(final Object object) {
		if (object == null)
			return "";

		else if (object instanceof String)
			return (String) object;

		else if (object.getClass() == double.class || object.getClass() == float.class)
			return MathUtil.formatTwoDigits((double) object);

		else if (object instanceof Collection)
			return CommonCore.join((Collection<?>) object, ", ", CommonCore::simplify);

		else if (object instanceof CompChatColor)
			return ((CompChatColor) object).getName();

		else if (object instanceof Enum)
			return ((Enum<?>) object).toString().toLowerCase();

		else if (object instanceof FoundationPlayer)
			return ((FoundationPlayer) object).getName();

		else if (object instanceof ConfigStringSerializable)
			return ((ConfigStringSerializable) object).serialize();

		else if (object instanceof Row)
			throw new FoException("Cannot simplify a Row object, got: " + object);

		for (final Function<Object, String> simplifier : simplifiers) {
			final String result = simplifier.apply(object);

			if (result != null)
				return result;
		}

		Method nameMethod = ReflectionUtil.getMethod(object.getClass(), "name");

		if (nameMethod == null)
			nameMethod = ReflectionUtil.getMethod(object.getClass(), "getName");

		if (nameMethod == null)
			nameMethod = ReflectionUtil.getMethod(object.getClass(), "getKey");

		if (nameMethod != null)
			return ReflectionUtil.invoke(nameMethod, object);

		return object.toString();
	}

	/**
	 * Add a simplifier function that converts objects into their string representation.
	 *
	 * @param simplifier
	 */
	public static void addSimplifier(final Function<Object, String> simplifier) {
		simplifiers.add(simplifier);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Menu-related
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Split a collection of items into pages, each containing up to a specified number of items (cell size).
	 *
	 * <p>This method organizes the provided items into pages where each page holds the number of items
	 * specified by {@code cellSize}. The result is a map where the key represents the page number (starting from 0)
	 * and the value is the list of items on that page.
	 * <p>If the collection is empty, at least one empty page will be returned.
	 *
	 * <ul>
	 * <li> For example, calling {@code fillPages(5, items)} will create pages with 5 items each.
	 * </ul>
	 *
	 * @param <T> the type of items to paginate
	 * @param cellSize the maximum number of items per page
	 * @param items the items to split into pages
	 * @return a map with page numbers as keys and lists of items as values
	 */
	public static final <T> Map<Integer, List<T>> fillPages(final int cellSize, final Iterable<T> items) {
		final List<T> allItems = new ArrayList<>();

		for (final T iterable : items)
			allItems.add(iterable);

		final Map<Integer, List<T>> pages = new LinkedHashMap<>();
		final int pageCount = allItems.size() == cellSize ? 0 : allItems.size() / cellSize;

		for (int i = 0; i <= pageCount; i++) {
			final List<T> pageItems = new ArrayList<>();

			final int down = cellSize * i;
			final int up = down + cellSize;

			for (int valueIndex = down; valueIndex < up; valueIndex++)
				if (valueIndex < allItems.size()) {
					final T page = allItems.get(valueIndex);

					pageItems.add(page);
				} else
					break;

			// If the menu is completely empty, at least allow the first page
			if (i == 0 || !pageItems.isEmpty())
				pages.put(i, pageItems);
		}

		return pages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Regular expressions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Compile a regex pattern based on platform-specific settings.
	 * This method allows customization of how regex patterns are created
	 * by considering platform-specific options like case sensitivity, unicode handling,
	 * color code stripping, and accent mark normalization.
	 *
	 * <p>The method works in the following steps:</p>
	 * <ul>
	 *   <li>If the platform is set to strip color codes from the regex, those will be removed.</li>
	 *   <li>If the platform is configured to normalize accents (diacritics), they will be replaced in the regex.</li>
	 *   <li>The regex will be compiled either case-insensitively or case-sensitively,
	 *   depending on the platform configuration.</li>
	 *   <li>If Unicode processing is enabled, the pattern will be compiled accordingly to handle Unicode properly.</li>
	 * </ul>
	 *
	 * @param regex The regular expression string that you want to compile.
	 * @return A Pattern object that represents the compiled regular expression with applied platform settings.
	 */
	public static final Pattern compilePattern(final String regex) {
		if (Platform.getPlugin().isRegexCaseInsensitive())
			return Pattern.compile(regex, Platform.getPlugin().isRegexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);

		else
			return Platform.getPlugin().isRegexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Tab completing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Provide tab-completion suggestions based on a partial string and a list of elements.
	 * <p>
	 * This method converts the provided elements into string representations and matches
	 * elements that start with the given partial name. The elements can be regular objects,
	 * iterables, arrays, or enums.
	 * <p>
	 * Example usage:
	 * <pre>
	 * {@code
	 * List<String> completions = tabComplete("ap", "apple", "orange", "grape", "banana");
	 * // completions will contain ["apple"]
	 * }
	 * </pre>
	 *
	 * @see SerializeUtilCore#serialize(Language, Object)
	 *
	 * @param <T> The type of the elements used for completion.
	 * @param partialName The user's input that will be matched against to suggest completions.
	 * @param elements The elements which will be turned into strings and used to generate suggestions.
	 * @return A sorted list of strings that start with the specified partial name.
	 */
	@SafeVarargs
	public static <T> List<String> tabComplete(String partialName, final T... elements) {
		final Collection<String> toComplete = new HashSet<>();

		if (elements != null)
			for (final T element : elements)
				if (element != null)
					if (element instanceof Iterable)
						for (final Object iterable : (Iterable<?>) element) {
							final String parsedValue = CommonCore.simplify(iterable);

							toComplete.add(ReflectionUtil.isEnumLike(iterable) ? parsedValue.toLowerCase() : parsedValue);
						}

					else if (element.getClass().isArray())
						for (int i = 0; i < Array.getLength(element); i++) {
							final Object iterable = Array.get(element, i);
							final String parsedValue = CommonCore.simplify(iterable);

							toComplete.add(ReflectionUtil.isEnumLike(iterable) ? parsedValue.toLowerCase() : parsedValue);
						}

					// Trick: Automatically parse enum constants
					else if (element instanceof Enum[])
						for (final Object iterable : ((Enum[]) element)[0].getClass().getEnumConstants())
							toComplete.add(iterable.toString().toLowerCase());

					else {
						final boolean lowercase = ReflectionUtil.isEnumLike(element);
						final String parsedValue = CommonCore.simplify(element);

						if (!"".equals(parsedValue))
							toComplete.add(lowercase ? parsedValue.toLowerCase() : parsedValue);
					}

		partialName = partialName.toLowerCase();

		for (final Iterator<String> iterator = toComplete.iterator(); iterator.hasNext();) {
			final String val = iterator.next();

			if (!val.toLowerCase().startsWith(partialName))
				iterator.remove();
		}

		// Prevents duplicates
		final List<String> sorted = new ArrayList<>();
		sorted.addAll(toComplete);

		Collections.sort(sorted);

		return sorted;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Joining strings and lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Join the given array with with "," and "and" of the last element.
	 *
	 * This method calls {@link #simplify(Object)} for each element in the list.
	 *
	 * @see #simplify(Object)
	 *
	 * @param list
	 * @return
	 */
	public static final String joinAnd(final Collection<?> list) {
		final List<String> simplified = new ArrayList<>();

		for (final Object element : list)
			simplified.add(simplify(element));

		return joinAnd(simplified.toArray(new String[simplified.size()]));
	}

	/**
	 * Join the given array with with "," and "and" of the last element.
	 *
	 * @param array
	 * @return
	 */
	public static final String joinAnd(final String... array) {
		if (array.length == 0)
			return "";

		if (array.length == 1)
			return array[0];

		if (array.length == 2)
			return array[0] + " " + Lang.plain("part-and") + " " + array[1];

		final StringBuilder out = new StringBuilder();

		for (int i = 0; i < array.length; i++) {
			if (i == array.length - 1)
				out.append(" " + Lang.plain("part-and") + " ").append(array[i]);
			else
				out.append(i == 0 ? "" : ", ").append(array[i]);
		}

		return out.toString();
	}

	/**
	 * Join the list array into one list.
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	@SafeVarargs
	public static final <T> List<T> joinLists(final Iterable<T>... arrays) {
		final List<T> all = new ArrayList<>();

		for (final Iterable<T> array : arrays)
			for (final T element : array)
				all.add(element);

		return all;
	}

	/**
	 * Join the array using spaces from the given start index.
	 *
	 * @param startIndex
	 * @param array
	 * @return
	 */
	public static final String joinRange(final int startIndex, final String[] array) {
		return joinRange(startIndex, array.length, array);
	}

	/**
	 * Join the array using spaces from the given start and end index.
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @param array
	 * @return
	 */
	public static final String joinRange(final int startIndex, final int stopIndex, final String[] array) {
		return joinRange(startIndex, stopIndex, array, " ");
	}

	/**
	 * Join the array using the given delimiter from the given start and end index.
	 *
	 * @param start
	 * @param stop
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static final String joinRange(final int start, final int stop, final String[] array, final String delimiter) {
		String joined = "";

		for (int i = start; i < MathUtil.range(stop, 0, array.length); i++)
			joined += (joined.isEmpty() ? "" : delimiter) + array[i];

		return joined;
	}

	/**
	 * Join array of objects into comma-separated string.
	 *
	 * We invoke {@link #simplify(Object)} for each object given it is not null, or return "" if it is.
	 *
	 * @see #simplify(Object)
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> String join(final T[] array) {
		return array == null ? "" : join(Arrays.asList(array));
	}

	/**
	 * Join a list of objects into comma-separated string.
	 *
	 * We invoke {@link #simplify(Object)} for each object given it is not null, or return "" if it is.
	 *
	 * @see #simplify(Object)
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> String join(final Iterable<T> array) {
		return array == null ? "" : join(array, ", ");
	}

	/**
	 * Join array of objects into a string separated by the given delimiter.
	 *
	 * We invoke {@link #simplify(Object)} for each object given it is not null, or return "" if it is.
	 *
	 * @see #simplify(Object)
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static final <T> String join(final T[] array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
	}

	/**
	 * Join the list into a string separated by the given delimiter.
	 *
	 * We invoke {@link #simplify(Object)} for each object given it is not null, or return "" if it is.
	 *
	 * @see #simplify(Object)
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static final <T> String join(final Iterable<T> array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
	}

	/**
	 * Join the array into a comma-separated string by invoking the given stringer
	 * for each element in the array.
	 *
	 * @param <T>
	 * @param array
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(final T[] array, final Stringer<T> stringer) {
		return join(array, ", ", stringer);
	}

	/**
	 * Join the array into a string separated by the given delimiter by invoking the given stringer
	 * for each element in the array.
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(@NonNull final T[] array, final String delimiter, final Stringer<T> stringer) {
		return join(Arrays.asList(array), delimiter, stringer);
	}

	/**
	 * Join the list into a comma-separated string by invoking the given stringer
	 * for each element in the array.
	 *
	 * @param <T>
	 * @param list
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(final Iterable<T> list, final Stringer<T> stringer) {
		return join(list, ", ", stringer);
	}

	/**
	 * Join the list into a string separated by the given delimiter by invoking the given stringer
	 * for each element in the array.
	 *
	 * @param <T>
	 * @param list
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(final Iterable<T> list, final String delimiter, final Stringer<T> stringer) {
		final StringBuilder builder = new StringBuilder();
		boolean first = true;

		for (final T element : list) {
			if (element != null) {
				if (!first)
					builder.append(delimiter);
				else
					first = false;

				builder.append(stringer.toString(element));
			}
		}
		return builder.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Data type conversion
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Convert the list having one data type into another.
	 *
	 * @param <Old>
	 * @param <New>
	 * @param list
	 * @param converter
	 * @return the new list
	 */
	public static final <Old, New> List<New> convertList(final Iterable<Old> list, final TypeConverter<Old, New> converter) {
		final List<New> copy = new ArrayList<>();

		for (final Old old : list) {
			final New result = converter.convert(old);

			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Convert the array having one data type into a list having another data type.
	 *
	 * @param <Old>
	 * @param <New>
	 * @param oldArray
	 * @param converter
	 * @return
	 */
	public static final <Old, New> List<New> convertArrayToList(final Old[] oldArray, final TypeConverter<Old, New> converter) {
		final List<New> newList = new ArrayList<>();

		for (final Old old : oldArray)
			newList.add(converter.convert(old));

		return newList;
	}

	/**
	 * Convert the list having one data type into array having another data type.
	 *
	 * @param <Old>
	 * @param <New>
	 * @param oldArray
	 * @param converter
	 * @return
	 */
	public static final <Old, New> New[] convertArray(final Old[] oldArray, final TypeConverter<Old, New> converter) {
		final List<New> newList = new ArrayList<>(oldArray.length);

		for (final Old oldItem : oldArray)
			newList.add(converter.convert(oldItem));

		if (newList.isEmpty())
			return (New[]) new Object[0]; // This handles the empty array case

		final New[] newArray = (New[]) Array.newInstance(newList.get(0).getClass(), newList.size());
		return newList.toArray(newArray);
	}

	/**
	 * Convert the set having one data type into another.
	 *
	 * @param <Old>
	 * @param <New>
	 * @param list
	 * @param converter
	 * @return the new list
	 */
	public static final <Old, New> Set<New> convertSet(final Iterable<Old> list, final TypeConverter<Old, New> converter) {
		final Set<New> copy = new HashSet<>();

		for (final Old old : list) {
			final New result = converter.convert(old);

			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Convert the map having one key=value data types into a map having another key=value pairs.
	 *
	 * @param <OldK>
	 * @param <OldV>
	 * @param <NewK>
	 * @param <NewV>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static final <OldK, OldV, NewK, NewV> Map<NewK, NewV> convertMap(final Map<OldK, OldV> oldMap, final MapToMapConverter<OldK, OldV, NewK, NewV> converter) {
		final Map<NewK, NewV> newMap = new LinkedHashMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * Convert the map into a list.
	 *
	 * @param <ListKey>
	 * @param <OldK>
	 * @param <OldV>
	 * @param map
	 * @param converter
	 * @return
	 */
	public static final <ListKey, OldK, OldV> List<ListKey> convertMapToList(final Map<OldK, OldV> map, final MapToListConverter<ListKey, OldK, OldV> converter) {
		final List<ListKey> list = new ArrayList<>();

		for (final Map.Entry<OldK, OldV> e : map.entrySet())
			list.add(converter.convert(e.getKey(), e.getValue()));

		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc message handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Remove null or empty values from a list and return a new list.
	 *
	 * <p>This method filters the provided list and creates a new one without:
	 * <ul>
	 * <li> Null values
	 * <li> Empty strings (if the elements are strings)
	 * </ul>
	 *
	 * <p>Other types of objects are simply added to the new list if they are not null.
	 *
	 * <ul>
	 * <li> For example, calling {@code removeNullAndEmpty([null, "", "text"])} will return a list with only "text".
	 * </ul>
	 *
	 * @param <T> the type of items in the list
	 * @param list the list from which null or empty values will be removed
	 * @return a new list without null or empty values
	 */
	public static final <T> List<T> removeNullAndEmpty(final List<T> list) {
		final List<T> copy = new ArrayList<>();

		for (final T key : list)
			if (key != null)
				if (key instanceof String) {
					if (!((String) key).isEmpty())
						copy.add(key);
				} else
					copy.add(key);

		return copy;
	}

	/**
	 * Remove null or empty values from a list and return a new list.
	 *
	 * <p>This method filters the provided list and creates a new one without:
	 * <ul>
	 * <li> Null values
	 * <li> Empty strings (if the elements are strings)
	 * </ul>
	 *
	 * <p>Other types of objects are simply added to the new list if they are not null.
	 *
	 * <ul>
	 * <li> For example, calling {@code removeNullAndEmpty([null, "", "text"])} will return a list with only "text".
	 * </ul>
	 *
	 * @param <T> the type of items in the list
	 * @param list the list from which null or empty values will be removed
	 * @return a new list without null or empty values
	 */
	public static final <T> T[] removeNullAndEmpty(final T[] list) {
		final List<T> copy = new ArrayList<>();

		for (final T key : list)
			if (key != null)
				if (key instanceof String) {
					if (!((String) key).isEmpty())
						copy.add(key);
				} else
					copy.add(key);

		return copy.toArray(list);
	}

	/**
	 * Replace all null elements in a string array with empty strings.
	 *
	 * <p>This method iterates through the array and substitutes any null elements with empty strings, leaving other values unchanged.
	 *
	 * <ul>
	 * <li> For example, calling {@code replaceNullWithEmpty([null, "text"])} will return an array with ["", "text"].
	 * </ul>
	 *
	 * @param list the array of strings where null values will be replaced
	 * @return the same array with null values replaced by empty strings
	 */
	public static final String[] replaceNullWithEmpty(final String[] list) {
		for (int i = 0; i < list.length; i++)
			if (list[i] == null)
				list[i] = "";

		return list;
	}

	/**
	 * Return an empty String if the String is null or equals to none.
	 *
	 * @param input
	 * @return
	 */
	public static final String getOrEmpty(final String input) {
		return input == null || "none".equalsIgnoreCase(input) ? "" : input;
	}

	/**
	 * If the String equals to none or is empty, return null.
	 *
	 * @param input
	 * @return
	 */
	public static final String getOrNull(final String input) {
		return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? null : input;
	}

	/**
	 * Return the provided value if it is valid, otherwise return the default value.
	 *
	 * <p>Specifically for strings, the method treats values like "none" (ignoring case) or an empty string as invalid, returning the default instead.
	 * For other types, it provides the given value if non-null; otherwise, the default value is returned.
	 *
	 * <ul>
	 * <li> For example, calling {@code getOrDefault("", "default")} will return "default".
	 * </ul>
	 *
	 * @param <T> the type of the value and default
	 * @param value the value to check
	 * @param def the default value to return if the provided value is invalid
	 * @return the value if valid, or the default if the value is invalid
	 */
	public static final <T> T getOrDefault(final T value, final T def) {
		if (value instanceof String && ("none".equalsIgnoreCase((String) value) || "".equals(value)))
			return def;

		return getOrDefaultStrict(value, def);
	}

	/**
	 * Returns the value or its default counterpart in case it is null.
	 *
	 * @param <T>
	 * @param value
	 * @param def
	 * @return
	 */
	public static final <T> T getOrDefaultStrict(final T value, final T def) {
		return value != null ? value : def;
	}

	/**
	 * Get next element in the list increasing the index by 1 if forward is true,
	 * or decreasing it by 1 if it is false.
	 *
	 * @param <T>
	 * @param given
	 * @param list
	 * @param forward
	 * @return
	 */
	public static final <T> T getNext(final T given, final List<T> list, final boolean forward) {
		if (given == null && list.isEmpty())
			return null;

		final T[] array = (T[]) Array.newInstance((given != null ? given : list.get(0)).getClass(), list.size());

		for (int i = 0; i < list.size(); i++)
			Array.set(array, i, list.get(i));

		return getNext(given, array, forward);
	}

	/**
	 * Get next element in the list increasing the index by 1 if forward is true,
	 * or decreasing it by 1 if it is false.
	 *
	 * @param <T>
	 * @param given
	 * @param array
	 * @param forward
	 * @return
	 */
	public static final <T> T getNext(final T given, final T[] array, final boolean forward) {
		if (array.length == 0)
			return null;

		int index = 0;

		for (int i = 0; i < array.length; i++) {
			final T element = array[i];

			if (element.equals(given)) {
				index = i;

				break;
			}
		}

		if (index != -1) {
			final int nextIndex = index + (forward ? 1 : -1);

			// Return the first slot if reached the end, or the last if vice versa
			return nextIndex >= array.length ? array[0] : nextIndex < 0 ? array[array.length - 1] : array[nextIndex];
		}

		return null;
	}

	/**
	 * Return the last key in the list or null if list is null or empty.
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static final <T> T last(final List<T> list) {
		return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * Return the last key in the array or null if array is null or empty.
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> T last(final T[] array) {
		return array == null || array.length == 0 ? null : array[array.length - 1];
	}

	/**
	 * Convert a list of string into a string array.
	 *
	 * @param array
	 * @return
	 */
	public static final String[] toArray(final Collection<String> array) {
		return array == null ? new String[0] : array.toArray(new String[array.size()]);
	}

	/**
	 * Create a new modifiable array list from String array.
	 *
	 * @param array
	 * @return
	 */
	public static final List<String> toList(final String... array) {
		return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Create a new modifiable array list from array.
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> List<T> toList(final T[] array) {
		return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Create a new modifiable array list from the given iterable.
	 *
	 * @param <T>
	 * @param iterable
	 * @return
	 */
	public static final <T> List<T> toList(final Iterable<T> iterable) {
		final List<T> list = new ArrayList<>();

		for (final T element : iterable)
			list.add(element);

		return list;
	}

	/**
	 * Reverse elements in the array.
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> T[] reverse(final T[] array) {
		if (array == null)
			return null;

		int i = 0;
		int j = array.length - 1;

		while (j > i) {
			final T tmp = array[j];

			array[j] = array[i];
			array[i] = tmp;

			j--;
			i++;
		}

		return array;
	}

	/**
	 * Return a new hashmap having the given first key and value pair
	 *
	 * @param <A>
	 * @param <B>
	 * @param firstKey
	 * @param firstValue
	 * @return
	 */
	public static final <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue) {
		final Map<A, B> map = new LinkedHashMap<>();
		map.put(firstKey, firstValue);

		return map;
	}

	/**
	 * Create a map with multiple keys and values.
	 * The keys and values must be in pairs and of the same type.
	 *
	 * @param <K>
	 * @param <V>
	 * @param entries
	 * @return
	 */
	@SafeVarargs
	public static final <K, V> Map<K, V> newHashMap(final Object... entries) {
		if (entries == null || entries.length == 0)
			return new LinkedHashMap<>();

		if (entries.length % 2 != 0)
			throw new FoException("Entries must be in pairs: " + Arrays.toString(entries) + ", got " + entries.length + " entries.");

		final Map<K, V> map = new LinkedHashMap<>();

		final K firstKey = (K) entries[0];

		for (int i = 0; i < entries.length; i += 2) {
			final K key = (K) entries[i];
			final V value = (V) entries[i + 1];

			if (key == null)
				throw new FoException("Key cannot be null at index " + i);

			if (!firstKey.getClass().isInstance(key))
				throw new FoException("All keys must be a String. Got " + key.getClass().getSimpleName());

			map.put(key, value);
		}

		return map;
	}

	/**
	 * Create a new modifiable HashSet.
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static final <T> Set<T> newSet(final T... keys) {
		return new HashSet<>(Arrays.asList(keys));
	}

	/**
	 * Create a modifiable List.
	 *
	 * Use this instead of Arrays.asList which is unmodifiable.
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static final <T> List<T> newList(final T... keys) {
		final List<T> list = new ArrayList<>();

		Collections.addAll(list, keys);

		return list;
	}

	/**
	 * Return a map sorted by values (i.e. from smallest to highest for numbers).
	 *
	 * @param map
	 * @return
	 */
	public static final Map<String, Integer> sortByValue(final Map<String, Integer> map) {
		final List<Map.Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
		list.sort(Map.Entry.comparingByValue());

		final Map<String, Integer> sortedMap = new LinkedHashMap<>();

		for (final Map.Entry<String, Integer> entry : list)
			sortedMap.put(entry.getKey(), entry.getValue());

		return sortedMap;
	}

	// ------------------------------------------------------------------------------------------------------------
	// I/O
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Compress the given string into a byte array.
	 *
	 * @param data
	 * @return
	 */
	public static final byte[] compress(final String data) {
		try {
			final byte[] input = data.getBytes("UTF-8");
			final Deflater deflater = new Deflater();

			deflater.setInput(input);
			deflater.finish();

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length)) {
				final byte[] buffer = new byte[1024];

				while (!deflater.finished()) {
					final int count = deflater.deflate(buffer);

					outputStream.write(buffer, 0, count);
				}

				return outputStream.toByteArray();
			}

		} catch (final Exception ex) {
			CommonCore.throwError(ex, "Failed to compress data");

			return new byte[0];
		}
	}

	/**
	 * Decompress the given byte array into a string.
	 *
	 * @param data
	 * @return
	 */
	public static final String decompress(final byte[] data) {
		final Inflater inflater = new Inflater();
		inflater.setInput(data);

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
			final byte[] buffer = new byte[1024];

			while (!inflater.finished()) {
				final int count = inflater.inflate(buffer);

				outputStream.write(buffer, 0, count);
			}

			return new String(outputStream.toByteArray(), "UTF-8");

		} catch (final Exception ex) {
			CommonCore.throwError(ex, "Failed to decompress data");

			return "";
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		String version = System.getProperty("java.version");

		if (version.startsWith("1."))
			version = version.substring(2, 3);

		else {
			final int dot = version.indexOf(".");

			if (dot != -1)
				version = version.substring(0, dot);
		}

		if (version.contains("-"))
			version = version.split("\\-")[0];

		return Integer.parseInt(version);
	}

	/**
	 * The sleep method from {@link Thread#sleep(long)} but without the need for try-catch.
	 *
	 * @param milliseconds
	 */
	public static final void sleep(final int milliseconds) {
		try {
			Thread.sleep(milliseconds);

		} catch (final InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Converts an unchecked exception into checked
	 *
	 * @param throwable
	 */
	public static void sneaky(final Throwable throwable) {
		try {
			SneakyThrows.sneaky(throwable);

		} catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
	}

	/**
	 * Wraps the runnable to catch any exceptions and log them.
	 *
	 * @param original
	 * @return
	 */
	public static Runnable wrapRunnableInExceptionCatcher(@NonNull final Runnable original) {
		final StackTraceElement[] outerElements = new Throwable().getStackTrace();

		return () -> {
			try {
				original.run();

			} catch (final Throwable throwable) {
				logCombinedError(throwable, outerElements);
			}
		};
	}

	/**
	 * Run the given runnable if the plugin is disabled.
	 *
	 * @param run
	 * @return
	 */
	public static boolean runIfDisabled(@NonNull final Runnable run) {
		if (!Platform.getPlugin().isEnabled()) {
			run.run();

			return true;
		}

		return false;
	}

	/*
	 * Combines the stack traces of two throwables and logs them.
	 */
	private static void logCombinedError(final Throwable throwable, final StackTraceElement[] outerTrace) {
		final StackTraceElement[] innerTrace = throwable.getStackTrace();

		final StackTraceElement[] combinedTrace = new StackTraceElement[outerTrace.length + innerTrace.length];

		System.arraycopy(innerTrace, 0, combinedTrace, 0, innerTrace.length);
		System.arraycopy(outerTrace, 0, combinedTrace, innerTrace.length, outerTrace.length);

		throwable.setStackTrace(combinedTrace);

		Debugger.printStackTrace(throwable);
		Debugger.saveError(throwable);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A simple interface from converting objects into strings.
	 *
	 * @param <T>
	 */
	public interface Stringer<T> {

		/**
		 * Convert the given object into a string.
		 *
		 * @param object
		 * @return
		 */
		String toString(T object);
	}

	/**
	 * A simple interface to convert between types.
	 *
	 * @param <Old> the initial type to convert from
	 * @param <New> the final type to convert to
	 */
	public interface TypeConverter<Old, New> {

		/**
		 * Convert a type given from A to B.
		 *
		 * @param value the old value type
		 * @return the new value type
		 */
		New convert(Old value);
	}

	/**
	 * Convenience class for converting map to a list.
	 *
	 * @param <O>
	 * @param <K>
	 * @param <Val>
	 */
	public interface MapToListConverter<O, K, Val> {

		/**
		 * Convert the given map key-value pair into a new type stored in a list.
		 *
		 * @param key
		 * @param value
		 * @return
		 */
		O convert(K key, Val value);
	}

	/**
	 * Convenience class for converting between maps.
	 *
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public interface MapToMapConverter<A, B, C, D> {

		/**
		 * Convert the old key type to a new type.
		 *
		 * @param key
		 * @return
		 */
		C convertKey(A key);

		/**
		 * Convert the old value into a new value type.
		 *
		 * @param value
		 * @return
		 */
		D convertValue(B value);
	}
}

/**
 * A wrapper for Spigot
 */
final class SneakyThrows {

	public static void sneaky(final Throwable t) {
		throw SneakyThrows.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
		throw (T) t;
	}
}