package org.mineacademy.fo.exception;

import org.mineacademy.fo.model.Rule;
import org.mineacademy.fo.model.RuleSetReader;

/**
 * An exception thrown when {@link RuleSetReader} encounters an unrecognized line in the rule file.
 */
public final class UnrecognizedRuleOperatorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The line args that were not recognized.
	 */
	private final String[] args;

	/**
	 * The rule that was being processed when the exception was thrown.
	 */
	private final Rule rule;

	/**
	 * The hint to be displayed in the exception message.
	 */
	private final String hint;

	public UnrecognizedRuleOperatorException(final String[] args, Rule rule) {
		this(args, rule, null);
	}

	public UnrecognizedRuleOperatorException(final String[] args, Rule rule, String hint) {
		this.args = args;
		this.rule = rule;
		this.hint = hint;
	}

	@Override
	public String getMessage() {
		return "'" + String.join(" ", args) + "' in " + this.rule.getFile().getName() + " rule " + this.rule.getUniqueName() + (hint != null ? " (did you mean '" + this.hint + "'?)" : "");
	}
}