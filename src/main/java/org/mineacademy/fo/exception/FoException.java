package org.mineacademy.fo.exception;

import org.mineacademy.fo.debug.Debugger;

/**
 * Represents our core exception. All exceptions of this
 * kind are logged automatically to the error.log file
 */
public class FoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new exception and logs it
	 *
	 * @param t
	 */
	public FoException(Throwable t) {
		super(t);

		Debugger.saveError(t);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 */
	public FoException(String message) {
		super(message);

		Debugger.saveError(this, message);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 * @param t
	 */
	public FoException(String message, Throwable t) {
		this(t, message);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 * @param t
	 */
	public FoException(Throwable t, String message) {
		super(message, t);

		Debugger.saveError(t, message);
	}

	/**
	 * Create a new exception and logs it
	 */
	public FoException() {
		Debugger.saveError(this);
	}

	@Override
	public String getMessage() {
		return "Report: " + super.getMessage();
	}
}