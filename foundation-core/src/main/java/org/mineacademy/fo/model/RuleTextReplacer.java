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

			if (replacement.startsWith("@prolong ")) {
				final String prolongChar = replacement.substring("@prolong ".length());
				final int length = matchResult.group().length();

				final String prefix;
				final String actualProlongChar;

				if (prolongChar.length() > 1) {
					final int lastCodePoint = prolongChar.codePointBefore(prolongChar.length());

					actualProlongChar = new String(Character.toChars(lastCodePoint));
					prefix = prolongChar.substring(0, prolongChar.length() - Character.charCount(lastCodePoint));
				} else {
					prefix = "";
					actualProlongChar = prolongChar;
				}

				return PlainTextComponentSerializer.plainText().deserialize(prefix + CommonCore.duplicate(actualProlongChar, length) + (matchResult.group().endsWith(" ") ? " " : ""));
			}

			return PlainTextComponentSerializer.plainText().deserialize(replacement);
		}));
	}
}