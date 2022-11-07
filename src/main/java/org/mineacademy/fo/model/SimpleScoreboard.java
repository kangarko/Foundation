package org.mineacademy.fo.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * A simple way of rendering custom scoreboards for players with close to no flickering.
 * Using &c takes 2 characters. Since the text gets split two times, text with colors = (total - (2 * 2)) = (total - 4)
 * Maximum line lengths:
 * - 1.8: 66 with color, 70 without color
 * - 1.13: 98 with color, 104 without color
 * - 1.18: 32889 with color, 32895 without color
 * Maximum title lengths:
 * - 1.8: 30 with color, 32 without color
 * - 1.13: 126 with color, 128 without color
 *
 * @author kangarko and Tijn (<a href="https://github.com/Tvhee-Dev">Tvhee-Dev</a>)
 */
public class SimpleScoreboard {

	// ------------------------------------------------------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------------------------------------------------------

	private static final String COLOR_CHAR = "\u00A7";

	/**
	 * List of all active scoreboard (added upon creating a new instance)
	 */
	@Getter
	private static final List<SimpleScoreboard> registeredBoards = new ArrayList<>();

	/**
	 * Unique chat color identifiers for specific team entries
	 */
	private static final String[] colors =
			{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

	/**
	 * Stored scoreboard lines
	 */
	private final List<String> rows = new ArrayList<>();

	/**
	 * Stored players viewing THIS scoreboard
	 */
	private final List<UUID> viewers = new ArrayList<>();

	/**
	 * The color theme for key: value pairs such as
	 * Players: 12
	 * Mode: Playing
	 */
	private final String[] theme = new String[2];
	/**
	 * The update tick delay
	 */
	@Getter
	@Setter
	private final int updateDelayTicks = 20;
	/**
	 * The title of this scoreboard
	 */
	@Getter
	@Setter
	private String title;
	/**
	 * The running update task
	 */
	private BukkitTask updateTask;

	/**
	 * Create a new scoreboard updating every second
	 */

	public SimpleScoreboard() {
		registeredBoards.add(this);
	}

	public SimpleScoreboard(final String title) {
		this(title, 20);
	}

	/**
	 * Create a new scoreboard with customizable updateDelayTicks
	 */
	public SimpleScoreboard(final String title, final int updateDelayTicks) {
		//Scoreboards were introduced in 1.5, objectives were added in 1.8
		Valid.checkBoolean(
				MinecraftVersion.atLeast(MinecraftVersion.V.v1_8),
				"(Fancy) Scoreboards are not supported below Minecraft version 1.8!");

		this.setTitle(title);
		this.setUpdateDelayTicks(updateDelayTicks);

		registeredBoards.add(this);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Clears registered boards, usually called on reload
	 */
	public static void clearBoards() {
		registeredBoards.clear();
	}

	/**
	 * Removes all scoreboard for a player
	 *
	 * @param player
	 */
	public static void clearBoardsFor(final Player player) {
		for (final SimpleScoreboard scoreboard : registeredBoards)
			if (scoreboard.isViewing(player))
				scoreboard.hide(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Public entries
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Called when this scoreboard is ticked (1)
	 */
	protected void onUpdate() {
	}

	/**
	 * Replaces variables in the message for the given player (2)
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	protected String replaceVariables(final Player player, final String message) {
		return message;
	}

	/**
	 * Return true if the scoreboard is running and rendering
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return this.updateTask != null;
	}

	/**
	 * Show this scoreboard to the player
	 *
	 * @param player
	 */
	public final void show(final Player player) {
		Valid.checkBoolean(
				this.title != null && !this.title.isEmpty(),
				"Before calling show(Player) you need to use setTitle() for " + this);
		Valid.checkBoolean(!this.isViewing(player),
				"Player " + player.getName() + " is already viewing scoreboard: " + this);

		if (this.updateTask == null)
			this.start();

		final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		this.viewers.add(player.getUniqueId());
		player.setScoreboard(scoreboard);
	}

	/**
	 * Hide this scoreboard from the player
	 *
	 * @param player
	 */
	public final void hide(final Player player) {
		Valid.checkBoolean(this.isViewing(player),
				"Player " + player.getName() + " is not viewing scoreboard: " + this.getTitle());

		player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		this.viewers.remove(player.getUniqueId());

		if (this.viewers.isEmpty())
			this.cancelUpdateTask();
	}

	/**
	 * Returns true if the given player is viewing THIS scoreboard
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(final Player player) {
		return this.viewers.contains(player.getUniqueId());
	}

	public final void setTheme(final ChatColor primary) {
		this.setTheme(primary, null);
	}

	/**
	 * Set the coloring theme for rows having : such as
	 * <p>
	 * Players: 12
	 * Mode: playing
	 * <p>
	 * To use simply put color codes
	 *
	 * @param primary
	 * @param secondary
	 */
	public final void setTheme(final ChatColor primary, final ChatColor secondary) {
		if (secondary != null) {
			this.theme[0] = "&" + primary.getChar();
			this.theme[1] = "&" + secondary.getChar();
		} else
			this.theme[0] = "&" + primary.getChar();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Add new rows
	// ------------------------------------------------------------------------------------------------------------

	public List<String> getRows() {
		return new ArrayList<>(this.rows);
	}

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final String... entries) {
		Valid.checkBoolean(entries != null, "You cannot add a 'null' array!");
		this.addRows(Arrays.asList(entries));
	}

	/**
	 * Add rows to the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final Collection<String> entries) {
		Valid.checkBoolean((this.rows.size() + entries.size()) < 16,
				"You are trying to add too many rows (the limit is 15)");

		for (final String entry : entries)
			this.rows.add(Common.colorize(String.valueOf(entry)));
	}

	public final void changeRow(final int index, final String value) {
		Valid.checkBoolean(index < this.rows.size(),
				"The row for index " + index + " is currently not existing. Please use addRows()!");

		this.rows.set(index, Common.colorize(value));
	}

	/**
	 * Remove all rows
	 */
	public final void clearRows() {
		this.rows.clear();
	}

	/**
	 * Remove row at the given index
	 *
	 * @param index
	 */
	public final void removeRow(final int index) {
		this.rows.remove(index);
	}

	/**
	 * Remove row that contains the given text
	 *
	 * @param thatContains
	 */
	public final void removeRow(final String thatContains) {
		this.rows.removeIf(row -> row.contains(thatContains));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Start / stop
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Starts visualizing this scoreboard
	 */
	private void start() {
		Valid.checkBoolean(this.updateTask == null, "Scoreboard " + this + " already running");

		this.updateTask = Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), () ->
		{
			try {
				this.onUpdate();

				for (final UUID viewerId : new ArrayList<>(this.viewers)) {
					final Player viewer = Bukkit.getPlayer(viewerId);

					if (viewer == null || !viewer.isOnline()) {
						this.viewers.remove(viewerId);
						continue;
					}

					this.reloadEntries(viewer);
				}

			} catch (final Throwable t) {
				final String lines = String.join(" ", this.getRows());

				Common.error(t, "Error displaying " + this, "Entries: " + lines, "Title: " + this.title, "%error",
						"Stopping rendering for safety.");

				this.stop();
			}
		}, 0, this.updateDelayTicks);
	}

	/**
	 * Stops this scoreboard and removes it from all viewers
	 */
	public final void stop() {
		this.viewers.forEach(viewerId ->
		{
			final Player viewer = Bukkit.getPlayer(viewerId);

			if (viewer != null && viewer.isOnline())
				viewer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		});

		this.viewers.clear();

		if (this.updateTask != null)
			this.cancelUpdateTask();
	}

	@Override
	public final String toString() {
		return "Scoreboard{title=" + this.getTitle() + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Private
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Adds theme colors to the row if applicable
	 *
	 * @param row
	 * @return
	 */
	private String replaceTheme(final String row) {
		if (row.contains(":")) {
			if (this.theme.length == 1)
				return this.theme[0] + row;

			if (this.theme[0] != null) {
				final String[] split = row.split("\\:");

				if (split.length > 1)
					return this.theme[0] + split[0] + ":" + this.theme[1] + split[1];
			}
		}

		return row;
	}

	/**
	 * Cancels the update task
	 */
	private void cancelUpdateTask() {
		Valid.checkNotNull(this.updateTask, "Scoreboard " + this + " not running");

		this.updateTask.cancel();
		this.updateTask = null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Reload entries for the given player
	 *
	 * @param player
	 */
	private void reloadEntries(final Player player) throws IllegalArgumentException {
		final int rowSize = this.rows.size();
		final Scoreboard scoreboard = player.getScoreboard();
		Objective mainboard = scoreboard.getObjective("scoreboard");

		if (mainboard == null) {
			mainboard = scoreboard.registerNewObjective("scoreboard", "dummy");
			mainboard.setDisplayName(this.title);
			mainboard.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		final int maxTitleLength = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? 128 : 32;
		final String colorizedTitle = Common.colorize(this.title);
		String title =
				colorizedTitle.length() > maxTitleLength ? colorizedTitle.substring(0, maxTitleLength) : colorizedTitle;
		title = title.endsWith(COLOR_CHAR) ? title.substring(0, title.length() - 1) : title;

		if (!mainboard.getDisplayName().equals(title))
			mainboard.setDisplayName(title);

		for (int lineNumber = 0; lineNumber < 15; lineNumber++) {
			final int scoreboardLineNumber = rowSize - lineNumber;
			Team line = scoreboard.getTeam("line" + scoreboardLineNumber);

			if (lineNumber < this.rows.size()) {
				if (line == null)
					line = scoreboard.registerNewTeam("line" + scoreboardLineNumber);

				final String lineText = this.rows.get(lineNumber);
				final Function<String, String> replaceFunction = text -> this.replaceTheme(
						this.replaceVariables(player, text.replace("{player}", player.getName())));
				final String[] createdLine =
						TextSplitter.createScoreBoardLine(lineText, COLOR_CHAR + colors[lineNumber] + COLOR_CHAR + "r",
								replaceFunction);

				final String prefix = createdLine[0];
				final String entry = createdLine[1];
				final String suffix = createdLine[2];
				String oldEntry = null;

				if (!line.getPrefix().equals(prefix))
					line.setPrefix(prefix);

				if (line.getEntries().size() > 1)
					throw new IllegalArgumentException("There are more than allowed entries in the teams entry list!");

				if (!line.getEntries().contains(entry)) {
					if (!line.getEntries().isEmpty()) {
						oldEntry = new ArrayList<>(line.getEntries()).get(0);
						line.removeEntry(oldEntry);
					}

					line.addEntry(entry);
				}

				if (!line.getSuffix().equals(suffix))
					line.setSuffix(suffix);

				if (oldEntry != null)
					scoreboard.resetScores(oldEntry);

				mainboard.getScore(entry).setScore(scoreboardLineNumber);
			} else if (line != null) {
				for (final String oldEntry : line.getEntries())
					scoreboard.resetScores(oldEntry);

				line.unregister();
			}
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private static class TextSplitter {

		private static String[] createScoreBoardLine(
				String text, final String lineIdentifier, final Function<String, String> textReplaceFunction) {
			text = textReplaceFunction.apply(text);

			final String[] line = new String[3];
			final boolean mc1_13 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);
			final boolean mc1_18 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_18);
			//< 1.13: 16, 40 | 1.13 - 1.18: 64, 40 | > 1.18: 64, 23767
			final int[] splitPoints = new int[]{mc1_13 || mc1_18 ? 64 : 16, mc1_18 ? 32767 : 40};
			//< 1.13: 70 | 1.13: 168 | > 1.18: 32895
			final int maxLength = mc1_18 ? 32895 : (mc1_13 ? 168 : 70);
			final List<String> copy = TextSplitter.copyColors(text, maxLength, splitPoints);

			line[0] = Common.colorize(copy.isEmpty() ? "" : copy.get(0)); //Team Prefix
			line[1] = Common.colorize(copy.size() < 2 ? lineIdentifier : copy.get(1)); //Score entry
			line[2] = Common.colorize(copy.size() < 3 ? "" : copy.get(2)); //Team Suffix

			return line;
		}

		/**
		 * @param text        The text containing the color codes
		 * @param maxLength   The maximum length the text can have all added
		 * @param splitPoints The points to split the text
		 * @return The method will split the text at the given splitPoints and will copy the colors over
		 */
		private static List<String> copyColors(final String text, final int maxLength, final int... splitPoints) {
			final List<String> result = new ArrayList<>();
			String leftOverText = text;
			int splitIndex = 0;

			while (!leftOverText.equals("")) {
				String colorPrefix = "";

				if (!result.isEmpty()) {
					final int lastEntryIndex = result.size() - 1;
					final String lastEntry = result.get(lastEntryIndex);

					if (lastEntry.endsWith(COLOR_CHAR)) {
						result.set(lastEntryIndex, lastEntry.substring(0, lastEntry.length() - 1));
						colorPrefix = COLOR_CHAR;
					} else if (lastEntry.contains(COLOR_CHAR)) {
						final int lastIndexOfColor = lastEntry.lastIndexOf(COLOR_CHAR);
						colorPrefix = COLOR_CHAR + lastEntry.charAt(lastIndexOfColor);
					}
				}

				final int splitPoint = splitIndex >= splitPoints.length ?
						leftOverText.length() + colorPrefix.length() : splitPoints[splitIndex];
				final int splitLength = splitPoint - colorPrefix.length();
				final String textPart =
						colorPrefix + leftOverText.substring(0, Math.min(splitLength, leftOverText.length()));

				result.add(textPart);
				leftOverText = leftOverText.substring(Math.min(splitLength, leftOverText.length()));
				splitIndex++;
			}

			int lengthToRemove = -maxLength;

			for (final String resultString : result)
				lengthToRemove += resultString.length();

			while (lengthToRemove > 0) {
				final int lastEntryIndex = result.size() - 1;
				String lastEntry = result.get(lastEntryIndex);
				final int lengthToRemoveFromEntry = Math.min(lengthToRemove, lastEntry.length());

				lastEntry = lastEntry.substring(0, lastEntry.length() - lengthToRemoveFromEntry);
				lengthToRemove -= lengthToRemoveFromEntry;

				if (lastEntry.endsWith(COLOR_CHAR))
					lastEntry = lastEntry.substring(0, lastEntry.length() - 1);

				if (lastEntry.isEmpty())
					result.remove(lastEntryIndex);
				else
					result.set(lastEntryIndex, lastEntry);
			}

			return result;
		}
	}
}