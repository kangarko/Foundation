package org.mineacademy.fo.filter;

import java.util.Arrays;
import java.util.Collection;

import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.database.Row;
import org.mineacademy.fo.database.RowLocationDate;
import org.mineacademy.fo.database.Table;
import org.mineacademy.fo.model.SimpleLocation;
import org.mineacademy.fo.platform.FoundationPlayer;

public final class FilterLocation extends Filter {

	private SimpleLocation location;
	private int radius;

	public FilterLocation() {
		super("location");
	}

	@Override
	public boolean isApplicable(final Table table) {
		return RowLocationDate.class.isAssignableFrom(table.getRowClass());
	}

	@Override
	public String[] getUsages() {
		return new String[] {
				"location:<[world],x,y,z,radius> - Show results for the given radius at the given x-y-z coordinates. If no world is specified, we get the world you're in. Example: location:100,-64,200,10",
				"location:here,radius - Show results for the given radius at your current location. Example: location:here,10"
		};
	}

	@Override
	public Collection<String> tabComplete(final FoundationPlayer audience) {
		if (audience.isPlayer()) {
			final SimpleLocation location = audience.getLocation();

			if (location != null)
				return Arrays.asList(location.getWorldName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + ",10", "here,10");
		}

		return null;
	}

	@Override
	public boolean validate(final FoundationPlayer audience, final String value) {
		final String[] split = value.split(",");

		if (split.length != 2 && split.length != 4 && split.length != 5) {
			Messenger.error(audience, "Invalid location format. Valid syntax: 'location:here,radius' or 'location:world,x,y,z,radius'.");

			return false;
		}

		if ("here".equals(split[0])) {
			if (!audience.isPlayer()) {
				Messenger.error(audience, "You must be a player to use 'here' location format.");

				return false;
			}

			if (split.length != 2) {
				Messenger.error(audience, "Invalid location format. Valid syntax: 'location:here,radius'");

				return false;
			}

			this.location = audience.getLocation();

			try {
				this.radius = Integer.parseInt(split[1]);

			} catch (final NumberFormatException ex) {
				Messenger.error(audience, "Invalid radius. It must be a whole number in the location. Example: here,10");

				return false;
			}

			return true;
		}

		if (split.length != 4 && split.length != 5) {
			Messenger.error(audience, "Invalid location format. Valid syntax: 'location:world,x,y,z,radius' or 'location:x,y,z,radius'");

			return false;
		}

		int offset = 0;

		String worldName = null;
		int x;
		int y;
		int z;

		// if split has 5 elements, get the world manually, otherwise, use the player's world or complain if sender is not a player
		if (split.length == 5)
			worldName = split[0];
		else {
			if (!audience.isPlayer()) {
				Messenger.error(audience, "When running from console, specify the location world.");

				return false;
			}

			worldName = audience.getLocation().getWorldName();
			offset = 1;
		}

		try {
			x = Integer.parseInt(split[1 - offset]);

		} catch (final NumberFormatException e) {
			Messenger.error(audience, "Invalid x location, it must be a whole number. Got: '" + split[1 - offset] + "'");

			return false;
		}

		try {
			y = Integer.parseInt(split[2 - offset]);

		} catch (final NumberFormatException e) {
			Messenger.error(audience, "Invalid y location, it must be a whole number. Got: '" + split[2 - offset] + "'");

			return false;
		}

		try {
			z = Integer.parseInt(split[3 - offset]);

		} catch (final NumberFormatException e) {
			Messenger.error(audience, "Invalid z location, it must be a whole number. Got: '" + split[3 - offset] + "'");

			return false;
		}

		this.location = new SimpleLocation(worldName, x, y, z);

		try {
			this.radius = Integer.parseInt(split[4 - offset]);

		} catch (final NumberFormatException e) {
			Messenger.error(audience, "Invalid radius, it must be a whole number. Got: '" + split[4 - offset] + "'");

			return false;
		}

		return true;
	}

	@Override
	public boolean canDisplay(final Row row) {
		final RowLocationDate rowLocation = (RowLocationDate) row;

		if (this.location.getWorldName().equalsIgnoreCase(rowLocation.getLocation().getWorldName()))
			return this.location.distance(rowLocation.getLocation()) <= this.radius;

		return false;
	}
}
