package org.mineacademy.fo.model;

/**
 * This boolean may only be set to true
 */
public final class OneWayBoolean {

	/**
	 * The value
	 */
	private boolean value = false;

	/**
	 * Set the boolean to true if the given value is true
	 * otherwise do nothin'
	 *
	 * @param value
	 */
	public void setIfTrue(boolean value) {
		if (value)
			this.value = true;
	}

	/**
	 * Set the value of the boolean to true
	 */
	public void setTrue() {
		this.value = true;
	}

	/**
	 * Return the value
	 *
	 * @return the value
	 */
	public boolean getValue() {
		return value;
	}
}
