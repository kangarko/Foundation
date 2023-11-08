package org.mineacademy.fo.exception;

import lombok.Getter;

@Getter
public class FoScriptException extends FoException {

	private static final long serialVersionUID = 1L;

	private final String javascript;
	private final int lineNumber;

	public FoScriptException(String message, String javascript, int lineNumber, Throwable cause) {
		super(message, cause);

		this.javascript = javascript;
		this.lineNumber = lineNumber;
	}

	public FoScriptException(String message, String javascript, int lineNumber) {
		super(message);

		this.javascript = javascript;
		this.lineNumber = lineNumber;
	}

	public String getErrorLine() {
		final String[] lines = javascript.split("\n");
		final int actualLine = this.lineNumber - 1;

		if (actualLine < 0 || actualLine >= lines.length)
			return "invalid line number";

		return lines[actualLine].trim();
	}
}
