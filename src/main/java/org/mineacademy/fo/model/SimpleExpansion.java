package org.mineacademy.fo.model;

import org.bukkit.command.CommandSender;
import org.mineacademy.fo.Common;

import lombok.NonNull;

/**
 * Represents a placeholder expansion used for variables that need
 * dynamic on-the-fly replacements depending on the variable content.
 *
 * This also hooks into PlaceholderAPI as needed
 */
public abstract class SimpleExpansion {

	/**
	 * Indicates there is no replacement and the placeholder should be
	 * printed out explicitly as-is to the console/game chat.
	 */
	protected static final String NO_REPLACE = null;

	/**
	 * The current arguments changed each time the expansion is called,
	 * we simply split the placeholder identifier by _ after the plugin
	 * such as corearena_player_health will give you [player, health]
	 */
	protected String[] args;

	/**
	 * Return the value of the placeholder such as arena_name
	 *
	 * @param sender
	 * @param params
	 *
	 * @return the value or null if not valid
	 */
	public final String replacePlaceholders(CommandSender sender, String params) {
		this.args = params.split("\\_");

		return this.onReplace(sender, params);
	}

	/**
	 * Return what variable we should replace for the given player and
	 * identifier.
	 *
	 * @param sender
	 * @param identifier everything after your plugin name such as if user types {corearena_player_health},
	 * 		  we return only "player_health". You can also use {@link #args} here.
	 * @return
	 */
	protected abstract String onReplace(@NonNull CommandSender sender, String identifier);

	/**
	 * Automatically joins the {@link #args} from the given index
	 *
	 * @param startIndex
	 * @return
	 */
	protected final String join(int startIndex) {
		return Common.joinRange(startIndex, this.args);
	}

	/**
	 * Automatically joins the {@link #args} from and to the given index
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @return
	 */
	protected final String join(int startIndex, int stopIndex) {
		return Common.joinRange(startIndex, stopIndex, this.args);
	}
}
