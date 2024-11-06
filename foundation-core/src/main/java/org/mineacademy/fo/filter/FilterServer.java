package org.mineacademy.fo.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;

import lombok.Setter;

public final class FilterServer extends Filter {

	@Setter
	private static Supplier<List<String>> networkServersSupplier;

	private final Set<String> servers = new HashSet<>();

	public FilterServer() {
		super("server");
	}

	@Override
	public boolean isApplicable(Table table) {
		return true;
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"server:<server|server2> - Show results from the given server. Add multiple servers separated by |.",
		};
	}

	@Override
	public Collection<String> tabComplete(FoundationPlayer audience) {
		final Set<String> serverNames = new TreeSet<>();

		serverNames.add(Platform.getCustomServerName());

		ValidCore.checkNotNull(networkServersSupplier != null, "Call FilterServer#setNetworkServersSupplier before using the server filter.");
		serverNames.addAll(networkServersSupplier.get());

		return serverNames;
	}

	@Override
	public boolean validate(FoundationPlayer audience, String value) {
		this.servers.clear();

		for (final String split : value.split("\\|"))
			this.servers.add(split);

		return true;
	}

	@Override
	public boolean canDisplay(Row row) {
		return this.servers.contains(row.getServer());
	}
}
