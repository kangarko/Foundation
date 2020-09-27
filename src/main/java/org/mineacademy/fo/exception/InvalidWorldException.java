package org.mineacademy.fo.exception;

import lombok.Getter;

/**
 * Thrown when we load data from data.db but they have a location with a world
 * that no longer exists
 */
public final class InvalidWorldException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The world that was invali
	 */
	@Getter
	private final String world;

	public InvalidWorldException(String message, String world) {
		super(message);

		this.world = world;
	}
}