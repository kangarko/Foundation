package org.mineacademy.fo.exception;

import lombok.Getter;

/**
 * Thrown when we check a regex, see {@link org.mineacademy.fo.Common#regExMatch(java.util.regex.Matcher)}
 * and the evaluation takes over the given limit
 */
@Getter
public final class RegexTimeoutException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The message that was being checked
	 */
	private final String checkedMessage;

	/**
	 * The execution limit in miliseconds
	 */
	private final long executionLimit;

	public RegexTimeoutException(CharSequence checkedMessage, long timeoutLimit) {
		this.checkedMessage = checkedMessage.toString();
		this.executionLimit = timeoutLimit;
	}
}