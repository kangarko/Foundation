package org.mineacademy.fo;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
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
import lombok.Setter;

/**
 * Our main utility class hosting a large variety of different convenience functions.
 *
 * This is a platform-neutral class, which is extended by "Common" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CommonCore {

	/**
	 * The Google Json instance
	 */
	public final static Gson GSON = new Gson();

	/**
	 * The Google Json instance with pretty printing
	 */
	public final static Gson GSON_PRETTY = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	/**
	 * Caches a chat-wide dark gray and strikethrough line for performance.
	 *
	 * @see #tellBoxed(FoundationPlayer, SimpleComponent...)
	 */
	private static final SimpleComponent CHAT_LINE_DARK_GRAY = SimpleComponent.fromMini("<dark_gray>" + CommonCore.chatLineSmooth());

	/**
	 * Used to send messages to player without repetition, e.g. if they attempt to break a block
	 * in a restricted region, we will not spam their chat with "You cannot break this block here" 120x times,
	 * instead, we only send this message once per X seconds.
	 *
	 * This cache holds the last times when we sent that message.
	 */
	private static final Map<SimpleComponent, Long> TIMED_TELL_CACHE = new LinkedHashMap<>();

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
	@Setter
	private static Function<Object, String> simplifier = t -> t.toString();

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
			broadcast(SimpleComponent.fromMini(message));
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
		broadcastWithPerm(showPermission, SimpleComponent.fromMini(message));
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
	public static void tellBoxed(FoundationPlayer audience, String... messages) {
		tellBoxed(audience, CommonCore.convertArray(messages, SimpleComponent::fromMini));
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
	public static void tellBoxed(FoundationPlayer audience, SimpleComponent... messages) {
		final int length = messages.length;

		Platform.runTask(2, () -> {
			audience.sendMessage(CHAT_LINE_DARK_GRAY);

			for (int i = 0; i < (length == 1 ? 2 : length == 2 || length == 3 || length == 4 ? 1 : 0); i++)
				audience.sendMessage(SimpleComponent.empty());

			for (final SimpleComponent message : messages)
				audience.sendMessage(message.replaceBracket("player", audience.getName()));

			for (int i = 0; i < (length == 1 || length == 2 ? 2 : length == 3 ? 1 : 0); i++)
				audience.sendMessage(SimpleComponent.empty());

			audience.sendMessage(CHAT_LINE_DARK_GRAY);
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
	public static final void tell(@NonNull FoundationPlayer audience, String... messages) {
		for (final String message : messages)
			audience.sendMessage(SimpleComponent.fromMini(message));
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
		tellTimed(delaySeconds, audience, SimpleComponent.fromMini(message));
	}

	/**
	 * Send a message to the audience if it was not sent previously in the given delay.
	 *
	 * @see FoundationPlayer#sendMessage(SimpleComponent)
	 *
	 * @param delaySeconds
	 * @param audience
	 * @param message
	 */
	public static final void tellTimed(final int delaySeconds, final FoundationPlayer audience, final SimpleComponent message) {

		// No previous message stored, just tell the player now
		if (!TIMED_TELL_CACHE.containsKey(message)) {
			audience.sendMessage(message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_TELL_CACHE.get(message) > delaySeconds) {
			audience.sendMessage(message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
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
		Platform.runTask(delayTicks, () -> audience.sendMessage(SimpleComponent.fromMini(message)));
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
			TIMED_LOG_CACHE.put(message, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_LOG_CACHE.get(message) > delaySeconds) {
			log(message);
			TIMED_LOG_CACHE.put(message, TimeUtil.currentTimeSeconds());
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
			log("&7" + chatLine());

			for (final String msg : messages)
				log(" &c" + msg);

			if (disablePlugin)
				log(" &cPlugin is now disabled.");

			log("&7" + chatLine());
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
	public static final void warning(String message) {
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
					log(SimpleComponent.fromAdventureJson(stripped).toLegacy());

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
	public static final void error(@NonNull Throwable throwable, String... messages) {

		if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
			throwable = throwable.getCause();

		if (!(throwable instanceof FoException))
			Debugger.saveError(throwable, messages);

		Debugger.printStackTrace(throwable);
		logFramed(replaceErrorVariable(throwable, messages));
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
	public static final void throwError(Throwable throwable, final String... messages) {
		if (throwable instanceof FoException)
			throw (FoException) throwable;

		Throwable cause = throwable;

		while (cause.getCause() != null)
			cause = cause.getCause();

		// Delegate to only print out the relevant stuff
		if (cause instanceof FoException)
			throw (FoException) throwable;

		if (messages != null)
			logFramed(false, replaceErrorVariable(throwable, messages));

		Debugger.saveError(throwable, messages);
		sneaky(throwable);
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
	public static final String duplicate(String text, int nTimes) {
		if (nTimes == 0)
			return "";

		final String toDuplicate = new String(text);

		for (int i = 1; i < nTimes; i++)
			text += toDuplicate;

		return text;
	}

	/**
	 * Limit the string to the given length maximum appending "..." at the end if the string is longer.
	 *
	 * @param text
	 * @param maxLength
	 * @return
	 */
	public static final String limit(String text, int maxLength) {
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
	public static final String[] split(String input, int maxLineLength) {
		final StringTokenizer tok = new StringTokenizer(input, " ");
		final StringBuilder output = new StringBuilder(input.length());
		int lineLen = 0;
		String lastColorCode = "";

		while (tok.hasMoreTokens()) {
			final String word = tok.nextToken();

			if (lineLen + word.length() > maxLineLength) {
				output.append("\n").append(lastColorCode);

				lineLen = 0;
			}

			final String colorCode = CompChatColor.getLastColors(word);

			if (!colorCode.isEmpty())
				lastColorCode = colorCode;

			output.append(word).append(" ");
			lineLen += word.length() + 1;
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
	 * You can set a custom simplifier by calling {@link #setSimplifier(Function)} which
	 * will be used for all unknown object types.
	 *
	 * @see #setSimplifier(Function)
	 *
	 * @param arg the object to simplify
	 * @return the simplified string representation of the object
	 */
	public static final String simplify(Object arg) {
		if (arg == null)
			return "";

		else if (arg instanceof String)
			return (String) arg;

		else if (arg.getClass() == double.class || arg.getClass() == float.class)
			return MathUtil.formatTwoDigits((double) arg);

		else if (arg instanceof Collection)
			return CommonCore.join((Collection<?>) arg, ", ", CommonCore::simplify);

		else if (arg instanceof CompChatColor)
			return ((CompChatColor) arg).getName();

		else if (arg instanceof Enum)
			return ((Enum<?>) arg).toString().toLowerCase();

		else if (arg instanceof FoundationPlayer)
			return ((FoundationPlayer) arg).getName();

		else if (arg instanceof ConfigStringSerializable)
			return ((ConfigStringSerializable) arg).serialize();

		return simplifier.apply(arg);
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
	public static final <T> Map<Integer, List<T>> fillPages(int cellSize, Iterable<T> items) {
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
	public static final Pattern compilePattern(String regex) {
		regex = Platform.getPlugin().isRegexStrippingColors() ? CompChatColor.stripColorCodes(regex) : regex;
		regex = Platform.getPlugin().isRegexStrippingAccents() ? ChatUtil.replaceDiacritic(regex) : regex;

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
	public static <T> List<String> tabComplete(String partialName, T... elements) {
		final List<String> toComplete = new ArrayList<>();

		if (elements != null)
			for (final T element : elements)
				if (element != null)
					if (element instanceof Iterable)
						for (final Object iterable : (Iterable<?>) element) {
							final String parsedValue = SerializeUtilCore.serialize(Language.YAML, iterable).toString();

							toComplete.add(ReflectionUtil.isEnumLike(iterable) ? parsedValue.toLowerCase() : parsedValue);
						}

					else if (element.getClass().isArray())
						for (int i = 0; i < Array.getLength(element); i++) {
							final Object iterable = Array.get(element, i);
							final String parsedValue = SerializeUtilCore.serialize(Language.YAML, iterable).toString();

							toComplete.add(ReflectionUtil.isEnumLike(iterable) ? parsedValue.toLowerCase() : parsedValue);
						}

					// Trick: Automatically parse enum constants
					else if (element instanceof Enum[])
						for (final Object iterable : ((Enum[]) element)[0].getClass().getEnumConstants())
							toComplete.add(iterable.toString().toLowerCase());

					else {
						final boolean lowercase = ReflectionUtil.isEnumLike(element);
						final String parsedValue = SerializeUtilCore.serialize(Language.YAML, element).toString();

						if (!"".equals(parsedValue))
							toComplete.add(lowercase ? parsedValue.toLowerCase() : parsedValue);
					}

		partialName = partialName.toLowerCase();

		for (final Iterator<String> iterator = toComplete.iterator(); iterator.hasNext();) {
			final String val = iterator.next();

			if (!val.toLowerCase().startsWith(partialName))
				iterator.remove();
		}

		Collections.sort(toComplete);

		return toComplete;
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
	public static final String joinAnd(Collection<?> list) {
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
	public static final String joinAnd(String... array) {
		if (array.length == 0)
			return "";

		if (array.length == 1)
			return array[0];

		if (array.length == 2)
			return array[0] + " " + Lang.plain("and") + " " + array[1];

		final StringBuilder out = new StringBuilder();

		for (int i = 0; i < array.length; i++) {
			if (i == array.length - 1)
				out.append(" " + Lang.plain("and") + " ").append(array[i]);
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
		final Iterator<T> it = list.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			if (next != null)
				message += stringer.toString(next) + (it.hasNext() ? delimiter : "");
		}

		return message;
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
		final New[] newArray = (New[]) Array.newInstance(oldArray.getClass().getComponentType(), oldArray.length);

		for (int i = 0; i < oldArray.length; i++)
			newArray[i] = converter.convert(oldArray[i]);

		return newArray;
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
	public static final <T> T last(List<T> list) {
		return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * Return the last key in the array or null if array is null or empty.
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> T last(T[] array) {
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
	public static final <K, V> Map<K, V> newHashMap(Object... entries) {
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
	public static final Map<String, Integer> sortByValue(Map<String, Integer> map) {
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
	public static final byte[] compress(String data) {
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
	public static final String decompress(byte[] data) {
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