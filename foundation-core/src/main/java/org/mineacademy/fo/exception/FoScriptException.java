package org.mineacademy.fo.exception;

import org.mineacademy.fo.model.JavaScriptExecutor;

import lombok.Getter;

/**
 * Thrown when a script fails to execute.
 *
 * @see JavaScriptExecutor
 */
@Getter
public class FoScriptException extends FoException {

	private static final long serialVersionUID = 1L;

	private final String javascript;
	private final int lineNumber;

	public FoScriptException(final String message, final String javascript, final int lineNumber, final Throwable cause) {
		super(cause, message, false);

		this.javascript = javascript;
		this.lineNumber = lineNumber;
	}

	public FoScriptException(final String message, final String javascript, final int lineNumber) {
		super(message, false);

		this.javascript = javascript;
		this.lineNumber = lineNumber;
	}

	public String getErrorLine() {
		final String[] lines = this.javascript.split("\n");
		final int actualLine = this.lineNumber - 1;

		if (actualLine < 0 || actualLine >= lines.length)
			return "invalid line number";

		return lines[actualLine].trim();
	}
}
