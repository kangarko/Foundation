package org.mineacademy.fo.command;

public abstract class SimpleSubCommand extends SimpleSubCommandCore implements SharedCommandCore {

	protected SimpleSubCommand(SimpleCommandGroup parent, String sublabel) {
		super(parent, sublabel);
	}

	protected SimpleSubCommand(String sublabel) {
		super(sublabel);
	}

}
