package org.mineacademy.fo.exception;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents an exception that has already been handled, stack trace printed
 * and all that good stuff. Meaning we just ignore it.
 */
public class HandledException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	@Getter
	private final Throwable handle;

	public HandledException(@NonNull Throwable handle) {
		this.handle = handle;
	}

	@Override
	public synchronized Throwable getCause() {
		return this.handle.getCause();
	}

	@Override
	public String getMessage() {
		return this.handle.getMessage();
	}

	@Override
	public StackTraceElement[] getStackTrace() {
		return this.handle.getStackTrace();
	}

	@Override
	public void printStackTrace() {
		this.handle.printStackTrace();
	}

	@Override
	public String toString() {
		return this.handle.toString();
	}
}