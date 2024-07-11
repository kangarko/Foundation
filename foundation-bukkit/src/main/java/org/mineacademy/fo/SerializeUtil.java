package org.mineacademy.fo;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.database.SimpleResultSet;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidRowException;
import org.mineacademy.fo.exception.InvalidWorldException;
import org.mineacademy.fo.platform.SimplePlugin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 *
 * This is the Bukkit-specific implementation, inheriting from {@link SerializeUtilCore}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializeUtil extends SerializeUtilCore {

	/**
	 * Converts a {@link Location} into "world x y z yaw pitch" String.
	 *
	 * @param loc
	 * @return
	 */
	public static String serializeLoc(final Location loc) {
		if (loc == null)
			return "";

		if (loc.equals(new Location(null, 0, 0, 0)))
			throw new FoException("Cannot serialize location with null world: " + loc);

		return loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + (loc.getPitch() != 0F || loc.getYaw() != 0F ? " " + Math.round(loc.getYaw()) + " " + Math.round(loc.getPitch()) : "");
	}

	/**
	 * Converts a String into a {@link Location}.
	 *
	 * @param line
	 * @return
	 */
	public static Location deserializeLocation(String line) {
		if (line == null)
			return null;

		line = line.toString().replace("\"", "");

		final String[] parts = line.toString().contains(", ") ? line.toString().split(", ") : line.toString().split(" ");
		ValidCore.checkBoolean(parts.length == 4 || parts.length == 6, "Expected location (String) but got " + line.getClass().getSimpleName() + ": " + line);

		final String world = parts[0];
		final World bukkitWorld = Bukkit.getWorld(world);
		if (bukkitWorld == null)
			throw new InvalidWorldException("Location with invalid world '" + world + "': " + line + " (Doesn't exist)", world);

		final double x = Double.parseDouble(parts[1]), y = Double.parseDouble(parts[2]), z = Double.parseDouble(parts[3]);
		final float yaw = Float.parseFloat(parts.length == 6 ? parts[4] : "0"), pitch = Float.parseFloat(parts.length == 6 ? parts[5] : "0");

		return new Location(bukkitWorld, x, y, z, yaw, pitch);
	}

	/**
	 * Deserialize the given column from the {@link SimpleResultSet} into a Bukkit's {@link ItemStack}.
	 *
	 * Returns null if the column value is null or empty.
	 *
	 * @param resultSet
	 * @param columnLabel
	 * @return
	 * @throws SQLException
	 */
	public static ItemStack deserializeItem(SimpleResultSet resultSet, String columnLabel) throws SQLException {
		final String value = resultSet.getString(columnLabel);

		if (value == null || "".equals(value))
			return null;

		try {
			return deserialize(Language.JSON, ItemStack.class, value);

		} catch (final Throwable ex) {
			Common.warning(SimplePlugin.getInstance().getName() + " found invalid row with invalid item value '" + value + "' in column '" + columnLabel + "' in table " + resultSet.getTableName() + " ignoring.");

			throw new InvalidRowException();
		}
	}

	/**
	 * Deserialize the given column from the {@link SimpleResultSet} into a Bukkit's {@link ItemStack}.
	 *
	 * Throws {@link InvalidRowException} if the column value is null or empty.
	 *
	 * @param resultSet
	 * @param columnLabel
	 * @return
	 * @throws SQLException
	 */
	public static ItemStack deserializeItemStrict(SimpleResultSet resultSet, String columnLabel) throws SQLException {
		final String value = resultSet.getStringStrict(columnLabel);

		try {
			return deserialize(Language.JSON, ItemStack.class, value);

		} catch (final Throwable ex) {
			Common.warning(SimplePlugin.getInstance().getName() + " found invalid row with invalid item value '" + value + "' in column '" + columnLabel + "' in table " + resultSet.getTableName() + " ignoring.");
			throw new InvalidRowException();
		}
	}

	/**
	 * Deserialize the given column from the {@link SimpleResultSet} into a Bukkit's {@link ItemStack} array.
	 *
	 * Returns empty array if the column value is null or empty.
	 *
	 * @param resultSet
	 * @param columnLabel
	 * @return
	 * @throws SQLException
	 */
	public static ItemStack[] deserializeItemArray(SimpleResultSet resultSet, String columnLabel) throws SQLException {
		final String value = resultSet.getString(columnLabel);

		if (value == null || "".equals(value))
			return new ItemStack[0];

		return deserializeItemArrayStrict(resultSet, columnLabel);
	}

	/**
	 * Deserialize the given column from the {@link SimpleResultSet} into a Bukkit's {@link ItemStack} array.
	 *
	 * Throws {@link InvalidRowException} if the column value is null
	 *
	 * @param resultSet
	 * @param columnLabel
	 * @return
	 * @throws SQLException
	 */
	public static ItemStack[] deserializeItemArrayStrict(SimpleResultSet resultSet, String columnLabel) throws SQLException {
		final String value = resultSet.getString(columnLabel);

		if (value == null || "".equals(value)) {
			Common.warning(SimplePlugin.getInstance().getName() + " found invalid row with null/empty column '" + columnLabel + "' in table " + resultSet.getTableName() + " ignoring.");

			throw new InvalidRowException();
		}

		return SerializeUtil.deserialize(Language.JSON, ItemStack[].class, value);
	}
}
