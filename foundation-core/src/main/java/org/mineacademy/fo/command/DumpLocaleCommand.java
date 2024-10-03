package org.mineacademy.fo.command;

import java.io.File;
import java.util.List;

import org.mineacademy.fo.platform.FoundationPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

public final class DumpLocaleCommand extends SimpleSubCommandCore {

	/**
	 * Create a new sub-command with the "dumplocale" and "dumploc" aliases registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 */
	public DumpLocaleCommand() {
		this("dumplocale|dumploc");
	}

	/**
	 * Create a new sub-command with the given label registered in your
	 * {@link FoundationPlugin#getDefaultCommandGroup()} command group.
	 *
	 * @param label
	 */
	public DumpLocaleCommand(String label) {
		super(label);

		this.setProperties();
	}

	/**
	 * Create a new sub-command with the "dumplocale" and "dumploc" aliases registered in the given command group.
	 *
	 * @param group
	 */
	public DumpLocaleCommand(SimpleCommandGroup group) {
		this(group, "dumplocale|dumploc");
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param group
	 * @param label
	 */
	public DumpLocaleCommand(SimpleCommandGroup group, String label) {
		super(group, label);

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setMaxArguments(0);
		this.setDescription("Copy language file to lang/ folder so you can edit it. This uses 'Locale' key from settings.yml. Existing file will be updated with new keys and unused ones will be deleted.");
	}

	@Override
	protected void onCommand() {
		tellInfo("Dumping or updating " + SimpleSettings.LOCALE + " locale file...");

		final File dumped = Lang.Storage.createAndDumpToFile();
		final File rootFile = Platform.getPlugin().getDataFolder();

		tellSuccess("Locale file dumped to " + dumped.getAbsolutePath().replace(rootFile.getParentFile().getAbsolutePath(), "") + ". Existing keys were updated, see console for details.");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}
