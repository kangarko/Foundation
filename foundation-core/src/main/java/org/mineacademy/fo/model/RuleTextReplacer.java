package org.mineacademy.fo.model;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
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
	 * Matches $0-$9 group references and the {matched_message} token.
	 */
	private static final Pattern GROUP_TOKEN = Pattern.compile("\\$(\\d)|\\{matched_message\\}");

	/**
	 * Did we change the message?
	 */
	@Getter
	private boolean changed = false;

	/**
	 * Replace the message with the given pattern and replacement,
	 * expanding $0-$n group references and {matched_message} per match.
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
				final String prolongChar = replaceMatchGroups(replacement.substring("@prolong ".length()), matchResult);
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

			return PlainTextComponentSerializer.plainText().deserialize(replaceMatchGroups(replacement, matchResult));
		}));
	}

	/**
	 * Expand $0-$n group references and {matched_message} from the given match in
	 * a single pass over the template, so tokens inside player-typed group content
	 * are never rescanned and expanded again.
	 *
	 * @param message
	 * @param result
	 * @return
	 */
	public static String replaceMatchGroups(final String message, final MatchResult result) {
		final Matcher tokens = GROUP_TOKEN.matcher(message);
		final StringBuffer output = new StringBuffer();

		while (tokens.find()) {
			final String replaced;

			if (tokens.group(1) != null) {
				final int index = Integer.parseInt(tokens.group(1));

				if (index > result.groupCount()) {
					tokens.appendReplacement(output, Matcher.quoteReplacement(tokens.group()));

					continue;
				}

				// Can be null for regexes using optional groups
				final String group = result.group(index);

				replaced = group == null ? "" : group.trim();

			} else
				replaced = result.group().trim();

			tokens.appendReplacement(output, Matcher.quoteReplacement(replaced));
		}

		tokens.appendTail(output);

		return output.toString();
	}
}