package org.mineacademy.fo;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Utility class for displaying health bar above mobs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HealthBarUtil {

	/**
	 * The default prefix and suffix
	 */
	@Setter
	private static String prefix = "&8[", suffix = "&8]";

	/**
	 * The default health bar colors
	 */
	@Setter
	private static ChatColor remainingColor = ChatColor.DARK_RED, totalColor = ChatColor.GRAY, deadColor = ChatColor.BLACK;

	/**
	 * Formats and displays the health bar as action bar.
	 *
	 * @param displayTo    the player to whom to display
	 * @param displayAbout the entity from which to measure
	 * @param damage       the damage from {@link EntityDamageByEntityEvent} event,
	 *                     or just set 0 to not subtract anything from health.
	 */
	public static void display(final Player displayTo, final LivingEntity displayAbout, final double damage) {
		display(displayTo, displayAbout, null, damage);
	}

	/**
	 * Formats and displays the health bar as action bar.
	 *
	 * @param displayTo
	 * @param displayAbout
	 * @param damagedEntityName
	 * @param damage
	 */
	public static void display(final Player displayTo, final LivingEntity displayAbout, @Nullable final String damagedEntityName, final double damage) {
		final int maxHealth = Remain.getMaxHealth(displayAbout);
		final int health = Remain.getHealth(displayAbout);

		final String name = Common.getOrEmpty(damagedEntityName);
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
		String max = "";
		{
			for (int i = 0; i < maxHealth - left.length(); i++)
				max += "|";
		}

		return prefix + remainingColor + left + totalColor + max + suffix;
	}

	/* Fills up the graphics if the health is too high */
	private static String formatMuchHealth(final int remaining, final int max) {
		return prefix + remainingColor + remaining + " &8/ " + totalColor + max + suffix;
	}

	/* Fills up the graphic if the health is 0 */
	private static String formatDeath(final int maxHealth) {
		String max = "";

		if (maxHealth > 30)
			max = "-0-";

		else
			for (int i = 0; i < maxHealth; i++)
				max += "|";

		return prefix + deadColor + max + suffix;
	}
}