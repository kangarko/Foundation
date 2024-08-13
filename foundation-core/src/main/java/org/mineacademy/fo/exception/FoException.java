package org.mineacademy.fo.exception;

import org.mineacademy.fo.debug.Debugger;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents our core exception. All exceptions of this
 * kind are logged automatically to the error.log file
 */
public class FoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Should we save thrown exceptions to error.log file automatically when they are thrown?
	 */
	@Getter
	@Setter
	private static boolean errorSavedAutomatically = true;

	/**
	 * Create a new exception and logs it
	 *
	 * @param t
	 */
	public FoException(Throwable t) {
		super(t);

		if (errorSavedAutomatically)
			Debugger.saveError(t);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 */
	public FoException(String message) {
		super(message);

		if (errorSavedAutomatically)
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

		if (errorSavedAutomatically)
			Debugger.saveError(t, message);
	}

	/**
	 * Create a new exception and logs it
	 */
	public FoException() {

		if (errorSavedAutomatically)
			Debugger.saveError(this);
	}

	@Override
	public String getMessage() {
		return "Report: " + super.getMessage();
	}
}