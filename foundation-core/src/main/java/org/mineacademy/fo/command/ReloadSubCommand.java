package org.mineacademy.fo.command;

import java.util.List;

import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.settings.Lang;

/**
 * A simple predefined sub-command for quickly reloading the plugin
 * using /{label} reload|rl
 */
public final class ReloadSubCommand extends SimpleSubCommandCore {

	/**
	 * Create a new sub-command with the "reload" and "rl" aliases registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 */
	public ReloadSubCommand() {
		this("reload|rl");
	}

	/**
	 * Create a new sub-command with the given label registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 *
	 * @param label
	 */
	public ReloadSubCommand(String label) {
		super(label);

		this.setProperties();
	}

	/**
	 * Create a new sub-command with the "reload" and "rl" aliases registered in the given command group.
	 *
	 * @param group
	 */
	public ReloadSubCommand(SimpleCommandGroup group) {
		this(group, "reload|rl");
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param group
	 * @param label
	 */
	public ReloadSubCommand(SimpleCommandGroup group, String label) {
		super(group, label);

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setMaxArguments(0);
		this.setDescription(Lang.component("command-reload-description"));
	}

	@Override
	protected void onCommand() {
		ReloadCommand.handleCommand(this);
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}