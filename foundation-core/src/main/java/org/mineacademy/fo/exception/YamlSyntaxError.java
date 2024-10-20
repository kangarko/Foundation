package org.mineacademy.fo.exception;

import java.io.File;

import org.snakeyaml.engine.v2.exceptions.ParserException;

/**
 * Thrown when we load data from yaml and the syntax is invalid.
 */
public final class YamlSyntaxError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The file that has the syntax error, if any.
	 */
	private final File file;

	public YamlSyntaxError(ParserException parent, File file) {
		super(parent);

		this.file = file;
	}

	@Override
	public String getMessage() {
		return "Failed to read yml" + (this.file != null ? " in " + this.file : "") + "due to bad syntax! Copy and paste its content to yaml-online-parser.appspot.com and fix it. Got: " + super.getMessage();
	}
}