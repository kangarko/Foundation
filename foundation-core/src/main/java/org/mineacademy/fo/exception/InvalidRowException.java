package org.mineacademy.fo.exception;

import org.mineacademy.fo.database.SimpleDatabase;

/**
 * A checked exception thrown when a row in the {@link SimpleDatabase} is invalid.
 */
public final class InvalidRowException extends RuntimeException {

	private static final long serialVersionUID = 1L;
}