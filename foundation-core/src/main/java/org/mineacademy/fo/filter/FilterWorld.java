package org.mineacademy.fo.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.RowLocationDate;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.FoundationPlayer;

import lombok.Setter;

public final class FilterWorld extends Filter {

	@Setter
	private static Supplier<Set<String>> networkWorldsSupplier;

	private final Set<String> worlds = new HashSet<>();

	public FilterWorld() {
		super("world");
	}

	@Override
	public boolean isApplicable(Table table) {
		return RowLocationDate.class.isAssignableFrom(table.getRowClass());
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"world:<world|world2> - Show results for the given world.",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		final Set<String> worldNames = new TreeSet<>();

		ValidCore.checkNotNull(networkWorldsSupplier, "Call FilterWorld#setNetworkWorldsSupplier using the world filter.");
		worldNames.addAll(networkWorldsSupplier.get());

		return worldNames;
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.worlds.clear();

		for (final String split : value.split("\\|"))
			this.worlds.add(split);

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return this.worlds.contains(((RowLocationDate) row).getLocation().getWorldName());
	}
}
