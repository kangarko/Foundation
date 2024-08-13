package org.mineacademy.fo.exception;

/**
 * Thrown when cannot serialize an object because it failed to determine its type
 */
public final class SerializeFailedException extends FoException {

	private static final long serialVersionUID = 1L;

	public SerializeFailedException(String reason) {
		super(reason);
	}
}