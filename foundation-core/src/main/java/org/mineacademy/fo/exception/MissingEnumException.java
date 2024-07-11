package org.mineacademy.fo.exception;

import org.mineacademy.fo.ReflectionUtil;

/**
 * Represents a failure to get the enum from our reflection utility.
 *
 * @see ReflectionUtil#lookupEnum(Class, String)
 */
public final class MissingEnumException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String enumName;

	public MissingEnumException(final String enumName, final String msg) {
		super(msg);

		this.enumName = enumName;
	}

	public MissingEnumException(final String enumName, final String msg, final Exception ex) {
		super(msg, ex);

		this.enumName = enumName;
	}

	public String getEnumName() {
		return this.enumName;
	}
}