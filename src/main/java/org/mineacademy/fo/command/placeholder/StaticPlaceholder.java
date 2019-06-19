package org.mineacademy.fo.command.placeholder;

/**
 * An argument that, when parsed, only returns the raw argument without any change.
 */
public class StaticPlaceholder extends Placeholder {

	/**
	 * The fixed replacement for this placeholder, for example
	 * {color} can always return RED as set here
	 */
	private final String replacement;

	/**
	 * Create a new static placeholder
	 *
	 * @param identifier
	 * @param replacement
	 */
	public StaticPlaceholder(String identifier, String replacement) {
		super(identifier);

		this.replacement = replacement;
	}

	@Override
	public final String replace(String raw) {
		return replacement;
	}
}
