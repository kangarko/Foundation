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
	private static String PREFIX = "&8[", SUFFIX = "&8]";

	/**
	 * The default health bar colors
	 */
	private static ChatColor REMAINING_COLOR = ChatColor.DARK_RED, TOTAL_COLOR = ChatColor.GRAY, DEAD_COLOR = ChatColor.BLACK;

	/**
	 * Formats and displays the health bar as action bar.
	 *
	 * @param displayTo    the player to whom to display
	 * @param displayAbout the entity from which to measure
	 * @param damage       the damage from {@link EntityDamageByEntityEvent} event,
	 *                     or just set 0 to not subtract anything from health.
	 */
	public static void display(Player displayTo, LivingEntity displayAbout, double damage) {
		final int maxHealth = (int) displayAbout.getMaxHealth();
		final int health = (int) displayAbout.getHealth();

		final String name = Common.getOrEmpty(displayAbout.getCustomName());
		final String formatted = (name.isEmpty() ? ItemUtil.bountifyCapitalized(displayAbout.getType()) : name) + " - " + getHealthMessage(health, maxHealth, (int) damage);

		Remain.sendActionBar(displayTo, formatted);
	}

	/* Creates a new health component */
	private static String getHealthMessage(int health, int maxHealth, int damage) {
		final int remainingHealth = health - damage;

		return remainingHealth > 0 ? formatHealth(remainingHealth, maxHealth) : formatDeath(maxHealth);
	}

	/* Fills up the graphics of health indicator */
	private static String formatHealth(int remainingHealth, int maxHealth) {

		if (maxHealth > 30)
			return formatMuchHealth(remainingHealth, maxHealth);

		// Fill the remaining health
		String left = "";
		{
			for (int i = 0; i < remainingHealth; i++)
				left += "|";
		}

		// Fill max health - remaining
		String max = "";
		{
			for (int i = 0; i < maxHealth - left.length(); i++)
				max += "|";
		}

		return PREFIX + REMAINING_COLOR + left + TOTAL_COLOR + max + SUFFIX;
	}

	/* Fills up the graphics if the health is too high */
	private static String formatMuchHealth(int remaining, int max) {
		return PREFIX + REMAINING_COLOR + remaining + " &8/ " + TOTAL_COLOR + max + SUFFIX;
	}

	/* Fills up the graphic if the health is 0 */
	private static String formatDeath(int maxHealth) {
		String max = "";

		if (maxHealth > 30)
			max = "-0-";

		else
			for (int i = 0; i < maxHealth; i++)
				max += "|";

		return PREFIX + DEAD_COLOR + max + SUFFIX;
	}
}