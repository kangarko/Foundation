package org.mineacademy.fo.model;

import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * A helper class to perform text replacements in the message
 */
public final class RuleTextReplacer {

	/**
	 * Did we change the message?
	 */
	@Getter
	private boolean changed = false;

	/**
	 * Replace the message with the given pattern and replacement
	 *
	 * @param message
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public Component replaceWithProlong(final String message, final Pattern pattern, final String replacement) {
		return LegacyComponentSerializer.legacySection().deserialize(message).replaceText(b -> b.match(pattern).replacement((matchResult, builder) -> {
			this.changed = true;

			if (replacement.startsWith("@prolong")) {
				final String afterProlong = replacement.substring("@prolong".length());
				int groupIndex = 0;
				String prolongChar;

				if (afterProlong.startsWith(":")) {
					final int spaceIndex = afterProlong.indexOf(' ');

					if (spaceIndex == -1)
						throw new IllegalArgumentException("Invalid @prolong syntax, expected '@prolong:N <char>', got: " + replacement);

					final String groupStr = afterProlong.substring(1, spaceIndex);

					try {
						groupIndex = Integer.parseInt(groupStr);
					} catch (final NumberFormatException ex) {
						throw new IllegalArgumentException("Invalid @prolong group index, expected a number, got: '" + groupStr + "' in: " + replacement);
					}

					if (groupIndex < 0 || groupIndex > matchResult.groupCount())
						throw new IllegalArgumentException("@prolong group index " + groupIndex + " is out of range, pattern has " + matchResult.groupCount() + " group(s)");

					prolongChar = afterProlong.substring(spaceIndex + 1);

				} else if (afterProlong.startsWith(" "))
					prolongChar = afterProlong.substring(1);
				else
					return PlainTextComponentSerializer.plainText().deserialize(replacement);

				final String matched = matchResult.group(groupIndex);
				final int length = matched != null ? matched.length() : 0;

				return PlainTextComponentSerializer.plainText().deserialize(CommonCore.duplicate(prolongChar, length) + (matchResult.group().endsWith(" ") ? " " : ""));
			}

			return PlainTextComponentSerializer.plainText().deserialize(replacement);
		}));
	}
}