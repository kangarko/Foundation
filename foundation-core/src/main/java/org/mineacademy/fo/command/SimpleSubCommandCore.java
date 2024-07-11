package org.mineacademy.fo.command;

import java.util.Arrays;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;

import lombok.Getter;

/**
 * A simple subcommand belonging to a {@link SimpleCommandGroup}.
 */
public abstract class SimpleSubCommandCore extends SimpleCommandCore {

	/**
	 * All registered sublabels this subcommand can have
	 */
	@Getter
	private final String[] sublabels;

	/**
	 * Create a new subcommand for the {@link FoundationPlugin#getDefaultCommandGroup()} group.
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

		final String[] split = sublabel.split("(\\||\\/)");
		ValidCore.checkBoolean(split.length > 0, "Please set at least 1 sublabel");

		this.sublabels = split;

		if (Platform.getPlugin().getDefaultCommandGroup() != null && Platform.getPlugin().getDefaultCommandGroup().getLabel().equals(this.getLabel()))
			this.setPermission(Platform.getPlugin().getName().toLowerCase() + ".command." + this.getSublabel()); // simply replace label with sublabel
		else
			this.setPermission(this.getPermission() + ".{sublabel}"); // append the sublabel at the end since this is not our main command
	}

	/**
	 * Shall we display the subcommand in the "/{label} help|?" menu?
	 *
	 * @see SimpleCommandGroup#autoHandleHelp()
	 *
	 * @return
	 */
	protected boolean showInHelp() {
		return true;
	}

	/**
	 * @see SimpleCommandCore#replacePlaceholders(SimpleComponent)
	 *
	 * @param component
	 * @return
	 */
	@Override
	protected SimpleComponent replacePlaceholders(SimpleComponent component) {
		component = component.replaceBracket("sublabel", this.getSublabel());

		return super.replacePlaceholders(component);
	}

	/**
	 * Get the first sublabel of this subcommand
	 *
	 * @return
	 */
	public final String getSublabel() {
		return this.sublabels[0];
	}

	@Override
	public String toString() {
		return "SubCommand{parent=/" + this.getLabel() + ", label=" + this.getSublabel() + "}";
	}

	/**
	 * Return true if the given object is a {@link SimpleSubCommandCore} and has the same sublabel and sublabel aliases.
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof SimpleSubCommandCore) {
			final SimpleSubCommandCore other = (SimpleSubCommandCore) obj;

			return Arrays.equals(other.getSublabels(), this.sublabels);
		}

		return false;
	}
}
