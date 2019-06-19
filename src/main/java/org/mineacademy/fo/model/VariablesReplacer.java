package org.mineacademy.fo.model;

/**
 * A simple interface for replacing variables
 */
public interface VariablesReplacer {

	/**
	 * Replace variables within that message
	 *
	 * @param message
	 * @return
	 */
	String replace(String message);
}