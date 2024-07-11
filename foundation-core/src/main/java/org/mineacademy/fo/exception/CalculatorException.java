package org.mineacademy.fo.exception;

import org.mineacademy.fo.MathUtil;

/**
 * An exception thrown when calculating wrong numbers (i.e. 0 division).
 *
 * @see MathUtil#calculate(String)
 */
public final class CalculatorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CalculatorException(final String message) {
		super(message);
	}
}