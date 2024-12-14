package org.mineacademy.fo.model;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.platform.FoundationPlayer;

/**
 * Represents a placeholder expansion used for variables that need
 * dynamic on-the-fly replacements depending on the variable content.
 *
 * If you have PlaceholderAPI installed, we automatically hook into it.
 * If you don't, you can still use this class to replace placeholders
 * in your own plugin by using the {@link Variables} class.
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
	 * Return the value of the placeholder such as corearena_arena_name
	 * The corearena_ is removed automatically.
	 *
	 * @param audience the player or null if not given
	 * @param identifier
	 *
	 * @return the value or null if not valid
	 */
	public final String replacePlaceholders(final FoundationPlayer audience, final String identifier) {
		this.args = identifier.split("\\_");

		return this.onReplace(audience, identifier);
	}

	/**
	 * Return what variable we should replace for the given audience and
	 * identifier.
	 *
	 * @param audience the player or null if not given
	 * @param identifier everything after your plugin name such as if user types {corearena_player_health},
	 * 		  we return only "player_health". You can also use {@link #args} here.
	 * @return
	 */
	protected abstract String onReplace(FoundationPlayer audience, String identifier);

	/**
	 * Automatically joins the {@link #args} from the given index
	 *
	 * @param startIndex
	 * @return
	 */
	protected final String join(final int startIndex) {
		return CommonCore.joinRange(startIndex, this.args);
	}

	/**
	 * Automatically joins the {@link #args} from and to the given index
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @return
	 */
	protected final String join(final int startIndex, final int stopIndex) {
		return CommonCore.joinRange(startIndex, stopIndex, this.args);
	}

	/**
	 * Return the priority of this expansion, the higher the sooner
	 * it will be called. You can override FoundationPlaceholders if
	 * you change this to 20 or more.
	 *
	 * @return
	 */
	public int getPriority() {
		return 1;
	}
}
