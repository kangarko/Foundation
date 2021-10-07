package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.model.SimpleScoreboard;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.RequiredArgsConstructor;

/**
 * Utility class for creating text animations for BossBars, Scoreboards, HUD Titles and Inventories.
 */
public class AnimationUtil {

	//////////////////////
	// Frame generators //
	//////////////////////

	/**
	 * Animate a text with colors that move from left to right.
	 * The animation ends once the firstColor reached the length of the message, which results in a half cycle. For a full cycle, use {@link #rightToLeftFull}
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> leftToRight(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		List<String> result = new ArrayList<>();
		String msg = Common.colorize(message);

		for (int frame = 0; frame < message.length(); frame++) {
			/*String middle = middleColor != null ? middleColor + String.valueOf(message.charAt(frame)) : String.valueOf(message.charAt(frame));
			String messageFinal = "";
			
			if (frame > 0)
			    messageFinal = firstColor + message.substring(0, frame - 1) + middle + lastColor + message.substring(frame - 1);
			
			result.add(messageFinal);*/

			String first = msg.substring(0, frame),
					middle = frame == msg.length() ? "" : String.valueOf(msg.charAt(frame)),
					last = frame == msg.length() ? "" : msg.substring(frame + 1);

			ChatColor middleColorFinal = middleColor != null ? middleColor : firstColor;

			result.add(firstColor + first + middleColorFinal + middle + lastColor + last);
		}
		return result;
	}

	/**
	 * Animate a text with colors that move from right to left.
	 * The animation ends once the firstColor reached the start of the message, which results in a half cycle. For a full cycle, use {@link #rightToLeftFull}
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> rightToLeft(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		String msg = Common.colorize(message);
		List<String> result = new ArrayList<>();

		for (int frame = msg.length(); frame >= 0; frame--) {
			String first = msg.substring(0, frame),
					middle = frame == msg.length() ? "" : String.valueOf(msg.charAt(frame)),
					last = frame == msg.length() ? "" : msg.substring(frame + 1);

			ChatColor middleColorFinal = middleColor != null ? middleColor : firstColor;

			result.add(firstColor + first + middleColorFinal + middle + lastColor + last);
		}
		return result;
	}

	/**
	 * Animate a text with colors that move from left to right in a full cycle.
	 * A full cycle animation is a cycle in this pattern: lastColor -> firstColor -> lastColor.
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> leftToRightFull(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		List<String> result = new ArrayList<>();
		result.addAll(leftToRight(message, firstColor, middleColor, lastColor));
		result.addAll(leftToRight(message, lastColor, middleColor, firstColor));
		return result;
	}

	/**
	 * Animate a text with colors that move from right to left in a full cycle.
	 * A full cycle animation is a cycle in this pattern: lastColor -> firstColor -> lastColor.
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> rightToLeftFull(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		List<String> result = new ArrayList<>();
		result.addAll(rightToLeft(message, firstColor, middleColor, lastColor));
		result.addAll(rightToLeft(message, lastColor, middleColor, firstColor));
		return result;
	}

	/**
	 * Flicker a text with the arranged colors.
	 * NOTE: For randomness, call this method inside the {@link #shuffle(List)} method.
	 *
	 * @param message  The message to be animated
	 * @param amount   The amount of times the message will flicker.
	 * @param duration How many duplicates will each frame have. More duplicates make the frames stay longer.
	 * @param colors   The flickering colors, ordered by array index.
	 * @return List of ordered string frames.
	 */
	public static List<String> flicker(String message, int amount, int duration, ChatColor[] colors) {
		List<String> result = new ArrayList<>();

		for (int frame = 0; frame < amount; frame++)
			for (int i = 0; i < duration; i++)
				result.add(colors[amount % colors.length] + message);

		return result;
	}

	/**
	 * Duplicate all frames by a specific amount, useful for slowing the animation.
	 *
	 * @param frames The frames to be duplicated.
	 * @param amount The amount of duplications.
	 * @return List of duplicates string frames.
	 */
	public static List<String> duplicate(List<String> frames, int amount) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < frames.size(); i++) {
			//duplicate j times;
			for (int j = 0; j < amount; j++) {
				String duplicated = frames.get(i);
				result.add(i, duplicated);
			}
		}
		return result;
	}

	/**
	 * Duplicate a specific frame by a specific amount, useful for slowing the animation.
	 *
	 * @param frame The frame to be duplicated
	 * @param frames The frames in which the frame is contained.
	 * @param amount The amount of duplications.
	 * @return The new result of frames after duplication.
	 */
	public static List<String> duplicateFrame(int frame, List<String> frames, int amount) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			String duplicated = frames.get(frame);
			result.add(frame, duplicated);
		}
		return result;
	}

	/**
	 * Shuffles the order of the frames.
	 *
	 * @param animatedFrames The frames to be shuffled.
	 * @return The new animatedFrames list after shuffling.
	 */
	public static List<String> shuffle(List<String> animatedFrames) {
		Collections.shuffle(animatedFrames);
		return animatedFrames;
	}

	/**
     * Combines animations in order.
     *
     * @param animationsToCombine The animations to combine (in order of the list)
     * @return The combined list of frames.
     */
    public static @NotNull List<String> combine(List<String>[] @NotNull animationsToCombine) {
        List<String> combined = new ArrayList<>();
        for (List<String> animation : animationsToCombine) {
            combined.addAll(animation);
        }

        return combined;
    }

	///////////////
	// Animators //
	///////////////

	/**
	 * Animates the title of a BossBar.
	 *
	 * @param bossBar        The BossBar to animate.
	 * @param animatedFrames The frames (in order) to be displayed in the BossBar.
	 * @param delay          The delay between animation cycles.
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return The repeating BukkitTask (Useful to cancel on reload or shutdown).
	 */
	public static BukkitTask animateBossBar(BossBar bossBar, List<String> animatedFrames, long delay, long period) {
		return new BukkitRunnable() {
			int frame = 0;

			@Override
			public void run() {
				bossBar.setTitle(animatedFrames.get(frame));
				frame++;

				if (frame == animatedFrames.size())
					frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * This will animated the BossBar
	 *
	 * @param bossBar        The BossBar to be animated
	 * @param animatedFrames The BossBar title frames list
	 * @param animatedColors The BossBar colors to be cycled
	 * @param delay          The delay before animating
	 * @param period         The period before animating
	 * @param animateOnce    Should the animation stop after all the frames have been cycled?
	 * @param countdownBar   Leave this as null if you don't want it to act as a countdown.
	 * @return BukkitTask of the animation
	 */
	public static BukkitTask animateBossBar(BossBar bossBar, List<String> animatedFrames,
			@Nullable List<BarColor> animatedColors, long delay,
			long period, boolean animateOnce, @Nullable CountdownBar countdownBar) {
		int smoothnessLevel = 1;
		if (countdownBar != null && countdownBar.isSmooth)
			smoothnessLevel = 10;
		int finalSmoothnessLevel = smoothnessLevel;

		return SimplePlugin.getInstance().getServer().getScheduler().runTaskTimer(SimplePlugin.getInstance(), new Runnable() {
			boolean run = true;
			int frame = 0;
			float health = 1F;

			@Override
			public void run() {
				if (!run)
					return;
				bossBar.setTitle(animatedFrames.get(frame % (animatedFrames.size() * finalSmoothnessLevel)));
				if (animatedColors != null)
					bossBar.setColor(animatedColors.get(frame % (animatedColors.size() * finalSmoothnessLevel)));
				bossBar.setProgress(health);

				if (countdownBar != null) {
					if (countdownBar.isSmooth)
						health -= countdownBar.duration / (10D * finalSmoothnessLevel);
					else
						health -= countdownBar.duration / 10D;
				}
				frame++;

				if (frame >= animatedFrames.size()) {
					frame = 0;
					health = 1F;
					if (animateOnce) {
						run = false;
						bossBar.removeAll();
					}
				}
			}
		}, delay, period / smoothnessLevel);
	}

	/**
	 * Animates the title of a Scoreboard.
	 *
	 * @param scoreboard     The scoreboard to animate.
	 * @param animatedFrames The frames (in order) to be displayed in the BossBar.
	 * @param delay          The delay (in tick) to wait between animation cycles.
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return The repeating BukkitTask (Useful to cancel on reload or shutdown).
	 */

	public static @NotNull BukkitTask animateScoreboardTitle(SimpleScoreboard scoreboard, List<String> animatedFrames, long delay, long period) {
		return new BukkitRunnable() {
			int frame = 0;

			@Override
			public void run() {
				scoreboard.setTitle(animatedFrames.get(frame));
				frame++;

				if (frame == animatedFrames.size())
					frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * Animates a Title for the player (Does not repeat).
	 *
	 * @param who            The player to show the Title.
	 * @param titleFrames    The frames (in order) to be displayed in the Title. (set to null to hide)
	 * @param subtitleFrames The frames (in order) to be displayed in the SubTitle. (set to null to hide)
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 */
	public static void animateTitle(Player who, @Nullable List<String> titleFrames, @Nullable List<String> subtitleFrames, long period) {
		new BukkitRunnable() {
			int frame = 0;
			String title = "", subtitle = "";

			@Override
			public void run() {
				if (titleFrames != null)
					title = titleFrames.get(frame % titleFrames.size());
				if (subtitleFrames != null)
					subtitle = subtitleFrames.get(frame % subtitleFrames.size());

				who.sendTitle(title, subtitle, 10, 70, 20);

				frame++;

				if (frame == Math.max(titleFrames != null ? titleFrames.size() : 0,
						subtitleFrames != null ? subtitleFrames.size() : 0) || SimplePlugin.isReloading())
					cancel();
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, period);
	}

	/**
	 * Animates the title of an item.
	 *
	 * @param item           The player to show the Title.
	 * @param animatedFrames The frames (in order) to be displayed in the Title.
	 * @param delay          The delay (in tick) to wait between animation cycles.
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return The repeating BukkitTask (Useful to cancel on reload or shutdown).
	 */
	public static @NotNull BukkitTask animateItemTitle(ItemStack item, List<String> animatedFrames, long delay, long period) {
		return new BukkitRunnable() {
			int frame = 0;

			@Override
			public void run() {
				ItemMeta meta = checkMeta(item);

				meta.setDisplayName(animatedFrames.get(frame));
				item.setItemMeta(meta);

				frame++;
				if (frame > animatedFrames.size())
					frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * @param item           The item to be animated.
	 * @param line           The line in the lore, if the line number is
	 * @param animatedFrames The frames (in order) to be displayed in the Title.
	 * @param delay          The delay (in tick) to wait between animation cycles.
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return The repeating BukkitTask (Useful to cancel on reload or shutdown).
	 * @throws IndexOutOfBoundsException if the line number is out of range
	 *                                   ({@code line < 0 || line > lore.size()})
	 */
	public static @NotNull BukkitTask animateItemLore(ItemStack item, int line, List<String> animatedFrames, long delay, long period) {
		return new BukkitRunnable() {
			int frame = 0;

			@Override
			public void run() {
				String frameText = animatedFrames.get(frame % animatedFrames.size());
				ItemMeta meta = checkMeta(item);
				List<String> lore = meta.getLore();
				if (lore == null)
					lore = new ArrayList<>(); // prevents NPE

				if (lore.size() < line)
					throw new IndexOutOfBoundsException("line #" + line + " is out of range!");

				lore.set(line, frameText); // update line

				meta.setLore(lore);
				item.setItemMeta(meta);

				frame++;
				if (frame > animatedFrames.size())
					frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * Animates the title of an inventory (that is currently viewed by the player).
	 *
	 * @param viewer         The player that views the inventory
	 * @param animatedFrames The frames (in order) to be displayed in the Title.
	 * @param delay          The delay (in tick) to wait between animation cycles.
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return The repeating BukkitTask (Useful to cancel on reload or shutdown).
	 */
	public static @NotNull BukkitTask animateInventoryTitle(Player viewer, List<String> animatedFrames, long delay, long period) {
		return new BukkitRunnable() {
			int frame = 0;

			@Override
			public void run() {
				PlayerUtil.updateInventoryTitle(viewer, animatedFrames.get(frame));
				frame++;
				if (frame > animatedFrames.size())
					frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/////////////////////
	// Private methods //
	/////////////////////

	/**
	 * Checks if an item has an ItemMeta (to prevent {@link NullPointerException}).
	 *
	 * @param item The item to be checked.
	 * @return new ItemMeta using the getItemMeta method in {@link Bukkit#getItemFactory} or an existing ItemMeta if the item already has one.
	 */
	private static ItemMeta checkMeta(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null || !item.hasItemMeta())
			meta = Bukkit.getItemFactory().getItemMeta(item.getType());

		return meta;
	}

	@RequiredArgsConstructor
	public static class CountdownBar {
		final long duration;
		final boolean isSmooth;
		final boolean goUp;
	}
}
// parpar8090 was here (you can remove this if you would like) :)