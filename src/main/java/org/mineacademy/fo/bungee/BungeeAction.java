package org.mineacademy.fo.bungee;

/**
 * Represents an action sent over Bungeecords containing
 * a set of data
 */
public interface BungeeAction {

	/**
	 * Stores all valid values in this action in the order of which they
	 * are being sent. The names of them are only used in the error message
	 * when the length of data does not match otherwise they don't matter.
	 */
	String[] getValidValues();
}
