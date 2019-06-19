package org.mineacademy.fo.command.placeholder;

/**
 * An argument that, when parsed, only returns the raw argument without any change.
 */
public final class ForwardingPlaceholder extends PositionPlaceholder {

	public ForwardingPlaceholder(String identifier, int position) {
		super(identifier, position);
	}

	@Override
	public final String replace(String raw) {
		return raw;
	}
}
