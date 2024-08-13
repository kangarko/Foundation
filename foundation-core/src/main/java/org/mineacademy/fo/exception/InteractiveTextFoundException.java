package org.mineacademy.fo.exception;

/**
 * Thrown when message contains hover or click events which would otherwise got
 * removed.
 * <p>
 * Such message is not checked.
 */
public final class InteractiveTextFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;
}