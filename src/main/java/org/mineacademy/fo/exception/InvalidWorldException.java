package org.mineacademy.fo.exception;

/**
 * Thrown when we load data from data.db but they have a location with a world
 * that no longer exists
 */
public final class InvalidWorldException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidWorldException(String message) {
		super(message);
	}
}