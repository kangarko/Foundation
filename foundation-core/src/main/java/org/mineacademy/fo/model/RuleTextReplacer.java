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
					// @prolong:N <char> — match specific group length
					final int spaceIndex = afterProlong.indexOf(' ');

					if (spaceIndex == -1) {
						prolongChar = "";
					} else {
						try {
							groupIndex = Integer.parseInt(afterProlong.substring(1, spaceIndex));
						} catch (final NumberFormatException ex) {
							groupIndex = 0;
						}

						prolongChar = afterProlong.substring(spaceIndex + 1);
					}

				} else if (afterProlong.startsWith(" ")) {
					// @prolong <char> — match entire match length
					prolongChar = afterProlong.substring(1);

				} else
					return PlainTextComponentSerializer.plainText().deserialize(replacement);

				final String matchedGroup = groupIndex > 0 && groupIndex <= matchResult.groupCount() ? matchResult.group(groupIndex) : matchResult.group();
				final int length = matchedGroup != null ? matchedGroup.length() : 0;

				return PlainTextComponentSerializer.plainText().deserialize(CommonCore.duplicate(prolongChar, length) + (matchedGroup != null && matchedGroup.endsWith(" ") ? " " : ""));
			}

			return PlainTextComponentSerializer.plainText().deserialize(replacement);
		}));
	}

	/**
	 * Replace the message with the given pattern and replacement, prolonging the replacement to match the original message length
	 * if the replacement starts with "@prolong"
	 *
	 * @param message
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public Component replace(final String message, final Pattern pattern, final String replacement) {
		return LegacyComponentSerializer.legacySection().deserialize(message).replaceText(b -> b.match(pattern).replacement((matchResult, builder) -> {
			this.changed = true;

			return PlainTextComponentSerializer.plainText().deserialize(replacement);
		}));
	}
}