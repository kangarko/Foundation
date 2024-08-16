package org.mineacademy.fo.command;

import java.util.List;

public abstract class SimpleCommand extends SimpleCommandCore implements SharedCommandCore {

	protected SimpleCommand(List<String> labelAndAliases) {
		super(labelAndAliases);
	}

	protected SimpleCommand(String label) {
		super(label);
	}

	protected SimpleCommand(String label, List<String> aliases) {
		super(label, aliases);
	}

}
