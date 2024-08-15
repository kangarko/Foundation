package org.mineacademy.fo.command;

import java.util.Arrays;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.platform.Platform;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

/**
 * A simple subcommand belonging to a {@link SimpleCommandGroup}
 */
public abstract class SimpleSubCommandCore extends SimpleCommandCore {

	/**
	 * All registered sublabels this subcommand can have
	 */
	@Getter
	private final String[] sublabels;

	/**
	 * The latest sublabel used when the subcommand was run,
	 * always updated on executing
	 */
	@Setter(value = AccessLevel.PROTECTED)
	@Getter(value = AccessLevel.PROTECTED)
	private String sublabel;

	/**
	 * Create a new subcommand given the main plugin instance defines a main command group
	 *
	 * @param sublabel
	 */
	protected SimpleSubCommandCore(String sublabel) {
		this(getMainCommandGroup0(), sublabel);
	}

	/*
	 * Attempts to get the main command group, failing with an error if not defined
	 */
	private static SimpleCommandGroup getMainCommandGroup0() {
		final SimpleCommandGroup main = Platform.getPlugin().getDefaultCommandGroup();

		ValidCore.checkNotNull(main, Platform.getPlugin().getName() + " does not define a main command group!"
				+ " You need to put @AutoRegister over your class extending a SimpleCommandGroup that has a no args constructor to register it automatically");

		return main;
	}

	/**
	 * Creates a new subcommand belonging to a command group
	 *
	 * @param parent
	 * @param sublabel
	 */
	protected SimpleSubCommandCore(SimpleCommandGroup parent, String sublabel) {
		super(parent.getLabel());

		this.sublabels = sublabel.split("(\\||\\/)");
		ValidCore.checkBoolean(this.sublabels.length > 0, "Please set at least 1 sublabel");

		this.sublabel = this.sublabels[0];

		// If the default perm was not changed, improve it
		if (this.getRawPermission().equals(getDefaultPermission()))
			if (Platform.getPlugin().getDefaultCommandGroup() != null && Platform.getPlugin().getDefaultCommandGroup().getLabel().equals(this.getLabel()))
				this.setPermission(this.getRawPermission().replace("{label}", "{sublabel}")); // simply replace label with sublabel

			else
				this.setPermission(this.getRawPermission() + ".{sublabel}"); // append the sublabel at the end since this is not our main command
	}

	/**
	 * The command group automatically displays all subcommands in the /{label} help|? menu.
	 * Shall we display the subcommand in this menu?
	 *
	 * @return
	 */
	protected boolean showInHelp() {
		return true;
	}

	/**
	 * Replace additional {sublabel} placeholder for this subcommand.
	 * See {@link SimpleCommandCore#replacePlaceholders(String)}
	 */
	@Override
	protected Component replacePlaceholders(Component message) {
		return super.replacePlaceholders(message).replaceText(b -> b.matchLiteral("{sublabel}").replacement(this.getSublabel()));
	}

	@Override
	public String toString() {
		return "SubCommand{parent=/" + this.getLabel() + ", label=" + this.getSublabel() + "}";
	}

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof SimpleSubCommandCore ? Arrays.equals(((SimpleSubCommandCore) obj).sublabels, this.sublabels) : false;
	}
}
