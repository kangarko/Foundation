package org.mineacademy.fo;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for displaying health bar above mobs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HealthBarUtil {

	/**
	 * The default prefix and suffix
	 */
	private static final String PREFIX = "&8[";
	private static final String SUFFIX = "&8]";

	/**
	 * The default health bar colors
	 */
	private static final ChatColor REMAINING_COLOR = ChatColor.DARK_RED;
	private static final ChatColor TOTAL_COLOR = ChatColor.GRAY;
	private static final ChatColor DEAD_COLOR = ChatColor.BLACK;

	/**
	 * Formats and displays the health bar as action bar.
	 *
	 * @param displayTo    the player to whom to display
	 * @param displayAbout the entity from which to measure
	 * @param damage       the damage from {@link EntityDamageByEntityEvent} event,
	 *                     or just set 0 to not subtract anything from health.
	 */
	public static void display(final Player displayTo, final LivingEntity displayAbout, final double damage) {
		final int maxHealth = Remain.getMaxHealth(displayAbout);
		final int health = Remain.getHealth(displayAbout);

		final String name = Common.getOrEmpty(displayAbout.getCustomName());
		final String formatted = (name.isEmpty() ? ItemUtil.bountifyCapitalized(displayAbout.getType()) : name) + " - " + getHealthMessage(health, maxHealth, (int) damage);

		Remain.sendActionBar(displayTo, formatted);
	}

	/* Creates a new health component */
	private static String getHealthMessage(final int health, final int maxHealth, final int damage) {
		final int remainingHealth = health - damage;

		return remainingHealth > 0 ? formatHealth(remainingHealth, maxHealth) : formatDeath(maxHealth);
	}

	/* Fills up the graphics of health indicator */
	private static String formatHealth(final int remainingHealth, final int maxHealth) {

		if (maxHealth > 30)
			return formatMuchHealth(remainingHealth, maxHealth);

		// Fill the remaining health
		String left = "";
		{
			for (int i = 0; i < remainingHealth; i++)
				left += "|";
		}

		// Fill max health - remaining
		StringBuilder max = new StringBuilder();
		{
			for (int i = 0; i < maxHealth - left.length(); i++)
				max.append("|");
		}

		return PREFIX + REMAINING_COLOR + left + TOTAL_COLOR + max + SUFFIX;
	}

	/* Fills up the graphics if the health is too high */
	private static String formatMuchHealth(final int remaining, final int max) {
		return PREFIX + REMAINING_COLOR + remaining + " &8/ " + TOTAL_COLOR + max + SUFFIX;
	}

	/* Fills up the graphic if the health is 0 */
	private static String formatDeath(final int maxHealth) {
		StringBuilder max = new StringBuilder();

		if (maxHealth > 30)
			max = new StringBuilder("-0-");

		else
			for (int i = 0; i < maxHealth; i++)
				max.append("|");

		return PREFIX + DEAD_COLOR + max + SUFFIX;
	}
}
