package org.mineacademy.fo.exception;

/**
 * Represents an exception that has already been handled, stack trace printed
 * and all that good stuff. Meaning we just ignore it.
 */
public class HandledException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HandledException() {
	}

	@Override
	public String getMessage() {
		throw new UnsupportedOperationException("HandledException do not support getMessage()");
	}

	@Override
	public String toString() {
		throw new UnsupportedOperationException("HandledException do not support toString()");
	}
}