package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for tab completion.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TabUtil {

	/**
	 * A shortcut for completing player names from all players connected online
	 *
	 * @param partialName
	 * @return
	 */
	public static List<String> completePlayerName(String partialName) {
		return complete(partialName, Common.getPlayerNames());
	}

	/**
	 * Return a list of tab completions for the given array,
	 * we attempt to resolve what type of the array it is,
	 * supports for chat colors, command senders, enumerations etc.
	 *
	 * @param <T>
	 * @param partialName
	 * @param all
	 * @return
	 */
	@SafeVarargs
	public static <T> List<String> complete(String partialName, T... all) {
		final List<String> clone = new ArrayList<>();

		for (final T s : all)
			if (s != null)

				if (s instanceof ChatColor)
					clone.add(((ChatColor) s).name().toLowerCase());

				else if (s instanceof CommandSender)
					clone.add(((CommandSender) s).getName());

				else if (s instanceof EntityType) {
					if (((EntityType) s).isAlive() && ((EntityType) s).isSpawnable())
						clone.add(s.toString().toLowerCase());

				} else if (s instanceof Enum)
					clone.add(s.toString().toLowerCase());

				else if (s instanceof Double)
					clone.add(MathUtil.formatTwoDigits(Double.parseDouble(s.toString())));

				else if (s instanceof Float)
					clone.add(MathUtil.formatTwoDigits(Float.parseFloat(s.toString())));

				else
					clone.add(s.toString());

		return complete(partialName, clone);
	}

	/**
	 * Returns valid tab completions for the given collection
	 *
	 * @param partialName
	 * @param all
	 * @return
	 */
	public static List<String> complete(String partialName, Iterable<String> all) {
		final ArrayList<String> tab = new ArrayList<>();

		for (final String s : all)
			tab.add(s);

		partialName = partialName.toLowerCase();

		for (final Iterator<String> iterator = tab.iterator(); iterator.hasNext();) {
			final String val = iterator.next();

			if (!val.toLowerCase().startsWith(partialName))
				iterator.remove();
		}

		return tab;
	}
}
