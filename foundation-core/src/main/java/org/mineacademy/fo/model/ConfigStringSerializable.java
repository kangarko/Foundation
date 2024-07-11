package org.mineacademy.fo.model;

/**
 * <p>Classes implementing this can be stored/loaded from a settings file</p>
 *
 * <p>** All classes must also implement the following: **</p>
 * <p>public static T fromString(String string)</p>
 */
public interface ConfigStringSerializable {

	/**
	 * Creates a String representation of this class that you can
	 * save in your settings yaml or json file.
	 *
	 * @return String containing the current state of this class
	 */
	String serialize();
}
