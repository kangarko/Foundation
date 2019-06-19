package org.mineacademy.fo.model;

import org.mineacademy.fo.collection.SerializedMap;

/**
 * Classes implementing this can be stored/loaded from a settings file
 *
 * All classes must also implement the following:
 * public static T deserialize(SerializedMap map)
 */
public interface ConfigSerializable {

	/**
	 * Creates a Map representation of this class that you can
	 * save in your settings yaml or json file.
	 *
	 * @return Map containing the current state of this class
	 */
	SerializedMap serialize();
}
