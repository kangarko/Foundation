package org.mineacademy.fo.exception;

import org.mineacademy.fo.debug.Debugger;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents our core exception. All exceptions of this
 * kind are logged automatically to the error.log file.
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
	public FoException(final Throwable t) {
		super(t);

		if (errorSavedAutomatically)
			Debugger.saveError(t);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 */
	public FoException(final String message) {
		super(message);

		if (errorSavedAutomatically)
			Debugger.saveError(this, message);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 * @param report
	 */
	public FoException(final String message, final boolean report) {
		super(message);

		if (errorSavedAutomatically && report)
			Debugger.saveError(this, message);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 * @param t
	 */
	public FoException(final String message, final Throwable t) {
		this(t, message);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 * @param t
	 */
	public FoException(final Throwable t, final String message) {
		super(message, t);

		if (errorSavedAutomatically)
			Debugger.saveError(t, message);
	}

	/**
	 * Create a new exception and logs it
	 *
	 * @param message
	 * @param t
	 * @param report
	 */
	public FoException(final Throwable t, final String message, final boolean report) {
		super(message, t);

		if (errorSavedAutomatically && report)
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