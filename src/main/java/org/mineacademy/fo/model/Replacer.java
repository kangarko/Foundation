package org.mineacademy.fo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.Remain;

import java.util.*;
import java.util.Map.Entry;

/**
 * An elegant way to find {variables} and replace them.
 * <p>
 * Use {@link #find(String...)} method to find {} variables such as find("health") and then
 * replace them with {@link #replace(Object...)} with the objects such as replace(20)
 * <p>
 * Something like {@link String#format(String, Object...)} but more useful for Minecraft
 */
@AllArgsConstructor
public final class Replacer {

	/**
	 * The internal deliminer used to separate variables
	 */
	private final static String DELIMITER = "%D3L1M1T3R%";

	/**
	 * The messages we are replacing variables in
	 */
	private final String[] messages;

	// ------------------------------------------------------------------------------------------------------------
	// Temporary variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The variables we are looking for to replace
	 */
	private String[] variables;

	/**
	 * The finished replaced message
	 */
	@Getter
	private String[] replacedMessage;

	/**
	 * Create a new instance for the given messages
	 *
	 * @param messages
	 */
	private Replacer(String... messages) {
		this.messages = messages;
	}

	/**
	 * Find the {} variables
	 *
	 * @param variables
	 * @return
	 */
	public Replacer find(String... variables) {
		this.variables = variables;

		return this;
	}

	/**
	 * Attempts to replace key:value pairs automatically
	 * <p>
	 * See {@link SerializedMap#ofArray(Object...)}
	 *
	 * @param associativeArray
	 * @return
	 */
	public String replaceAll(Object... associativeArray) {
		final SerializedMap map = SerializedMap.ofArray(associativeArray);

		final List<String> find = new ArrayList<>();
		final List<Object> replaced = new ArrayList<>();

		for (final Map.Entry<String, Object> entry : map.entrySet()) {
			find.add(entry.getKey());
			replaced.add(entry.getValue());
		}

		find(find.toArray(new String[find.size()]));
		replace(replaced.toArray(new Object[replaced.size()]));

		return getReplacedMessageJoined();
	}

	/**
	 * Replaces stored variables with their counterparts
	 * <p>
	 * Call this after {@link #find(String...)}!
	 *
	 * @param replacements
	 * @return
	 */
	public Replacer replace(Object... replacements) {
		Valid.checkNotNull(variables, "call find() first");
		Valid.checkBoolean(replacements.length == variables.length, "Variables " + variables.length + " != replacements " + replacements.length);

		// Join and replace as 1 message for maximum performance
		String message = StringUtils.join(messages, DELIMITER);

		for (int i = 0; i < variables.length; i++) {
			String find = variables[i];

			{ // Auto insert brackets
				if (!find.startsWith("{"))
					find = "{" + find;

				if (!find.endsWith("}"))
					find = find + "}";
			}

			final Object rep = i < replacements.length ? replacements[i] : null;

			// Convert it into a human readable string
			String serialized;

			SerializeUtil.STRICT_MODE = false;
			serialized = Objects.toString(SerializeUtil.serialize(rep));
			SerializeUtil.STRICT_MODE = true;

			message = message.replace(find, rep != null ? serialized : "");
		}

		this.replacedMessage = message.split(DELIMITER);

		return this;
	}

	/**
	 * Sends the finished message with variables replaced to the player
	 *
	 * @param recipient
	 */
	public void tell(CommandSender recipient) {
		Valid.checkNotNull(replacedMessage, "Replaced message not yet set, use find() and replace()");

		Common.tell(recipient, replacedMessage);
	}

	/**
	 * Return the replaced message joined with spaces
	 *
	 * @return
	 */
	public String getReplacedMessageJoined() {
		Valid.checkNotNull(replacedMessage, "Replaced message not yet set, use find() and replace()");

		return StringUtils.join(replacedMessage, " ");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static access
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new replacer instance in which you can easily find and replace variables and send the message to player
	 *
	 * @param messages
	 * @return
	 */
	public static Replacer of(String... messages) {
		return new Replacer(messages);
	}

	/**
	 * Sends a message to the player replacing the given associative array of placeholders in the given message
	 *
	 * @param recipient
	 * @param message
	 * @param replacements
	 */
	public static void tell(CommandSender recipient, String message, Object... replacements) {
		Common.tell(recipient, replaceArray(message, replacements));
	}

	/**
	 * Replace all variables in the {@link SerializedMap#ofArray(Object...)} format
	 * adding {} to them if they do not contain it already
	 *
	 * @param message
	 * @param associativeArray
	 * @return
	 */
	public static String replaceArray(String message, Object... replacements) {
		final SerializedMap map = SerializedMap.ofArray(replacements);

		for (final Entry<String, Object> replacement : map.entrySet()) {
			String key = replacement.getKey();

			key = key.indexOf(0) != '{' ? "{" + key : key;
			key = key.indexOf(key.length() - 1) != '}' ? key + "}" : key;

			message = message.replace(key, simplify(replacement.getValue()));
		}

		return message;
	}

	/**
	 * Replace some common classes such as entity to name automatically
	 *
	 * @param arg
	 * @return
	 */
	public static String simplify(Object arg) {
		if (arg instanceof Entity)
			return Remain.getName((Entity) arg);

		else if (arg instanceof CommandSender)
			return ((CommandSender) arg).getName();

		else if (arg instanceof World)
			return ((World) arg).getName();

		else if (arg instanceof Location)
			return Common.shortLocation((Location) arg);

		else if (arg.getClass() == double.class || arg.getClass() == float.class)
			return MathUtil.formatTwoDigits((double) arg);

		else if (arg instanceof Collection)
			return Common.join((Collection<?>) arg, ", ", Replacer::simplify);

		else if (arg instanceof Enum)
			return ((Enum<?>) arg).name();

		return arg.toString();
	}
}
