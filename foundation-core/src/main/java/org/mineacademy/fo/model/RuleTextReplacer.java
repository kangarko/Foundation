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
	public Component replaceWithProlong(String message, Pattern pattern, String replacement) {
		return LegacyComponentSerializer.legacySection().deserialize(message).replaceText(b -> b.match(pattern).replacement((matchResult, builder) -> {
			this.changed = true;

			if (replacement.startsWith("@prolong "))
				return PlainTextComponentSerializer.plainText().deserialize(CommonCore.duplicate(replacement.replace("@prolong ", ""), matchResult.group().length()) + (matchResult.group().endsWith(" ") ? " " : ""));

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
	public Component replace(String message, Pattern pattern, String replacement) {
		return LegacyComponentSerializer.legacySection().deserialize(message).replaceText(b -> b.match(pattern).replacement((matchResult, builder) -> {
			this.changed = true;

			return PlainTextComponentSerializer.plainText().deserialize(replacement);
		}));
	}
}