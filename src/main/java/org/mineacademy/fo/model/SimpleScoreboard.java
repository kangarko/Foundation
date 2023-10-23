package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

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
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.Getter;
import lombok.NonNull;

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
	 * Unique chat color identifiers for specific team entries
	 */
	private static final String[] COLORS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	/**
	 * List of all active scoreboard (added upon creating a new instance)
	 */
	@Getter
	private static final List<SimpleScoreboard> registeredBoards = new ArrayList<>();

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
	 * <p>
	 * Players: 12
	 * Mode: playing
	 */
	private final String[] theme = new String[2];

	/**
	 * The title of this scoreboard
	 */
	private String title;

	/**
	 * The update tick delay
	 */
	@Getter
	private int updateDelayTicks = 20;

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

	public SimpleScoreboard(String title) {
		this(title, 20);
	}

	/**
	 * Create a new scoreboard with customizable updateDelayTicks
	 * @param title
	 * @param updateDelayTicks
	 */
	public SimpleScoreboard(String title, int updateDelayTicks) {
		// Scoreboards were introduced in 1.5, objectives were added in 1.7.2
		Valid.checkBoolean(MinecraftVersion.atLeast(MinecraftVersion.V.v1_7), "Scoreboards (with objectives) are not supported below Minecraft version 1.7!");

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
	 * Replaces variables in the message for the given player
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	protected String replaceVariables(final @NonNull Player player, final @NonNull String message) {
		return message;
	}

	/**
	 * Called when this scoreboard is ticked
	 */
	protected void onUpdate() {
	}

	public final String getTitle() {
		return this.title;
	}

	/**
	 * @param title the title to set
	 */
	public final void setTitle(String title) {
		final int maxTitleLength = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? 128 : 32;

		this.title = title.length() > maxTitleLength ? title.substring(0, maxTitleLength) : title;
		this.title = this.title.endsWith(COLOR_CHAR) ? this.title.substring(0, this.title.length() - 1) : this.title;
	}

	/**
	 * Return the list of rows you can modify
	 *
	 * @return
	 */
	public List<String> getRows() {
		return this.rows;
	}

	/**
	 * Return true if the scoreboard is running and rendering?
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return this.updateTask != null;
	}

	/**
	 * @param updateDelayTicks the updateDelayTicks to set
	 */
	public final void setUpdateDelayTicks(int updateDelayTicks) {
		this.updateDelayTicks = updateDelayTicks;
	}

	/**
	 * Show this scoreboard to the player
	 *
	 * @param player
	 */
	public final void show(final Player player) {
		Valid.checkBoolean(this.title != null && !this.title.isEmpty(), "Before calling show(Player) you need to use setTitle() for " + this);
		Valid.checkBoolean(!this.isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + this);

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
		Valid.checkBoolean(this.isViewing(player), "Player " + player.getName() + " is not viewing scoreboard: " + this.getTitle());

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
	public final void setTheme(@NonNull final ChatColor primary, final ChatColor secondary) {
		if (secondary != null) {
			this.theme[0] = "&" + primary.getChar();
			this.theme[1] = "&" + secondary.getChar();
		} else
			this.theme[0] = "&" + primary.getChar();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Add new rows
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final Object... entries) {
		this.addRows(Arrays.asList(entries));
	}

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final List<Object> entries) {
		Valid.checkBoolean((this.rows.size() + entries.size()) <= 15, "You are trying to add too many rows (the limit is 15)");
		final List<String> lines = new ArrayList<>();

		for (final Object object : entries)
			lines.add(object == null ? "" : Common.colorize(SerializeUtil.serialize(SerializeUtil.Mode.YAML, object).toString()));

		this.rows.addAll(lines);
	}

	/**
	 * Changes the row at the given index, if exists
	 *
	 * @param index
	 * @param value
	 */
	public final void setRow(final int index, final String value) {
		Valid.checkBoolean(index < this.rows.size(), "The row for index " + index + " is currently not existing. Please use addRows()!");

		this.rows.set(index, value == null ? "" : value);
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

		this.updateTask = Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), () -> {
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
				Common.error(t,
						"Error displaying " + this,
						"Entries: " + this.rows,
						"Title: " + this.title,
						"%error",
						"Stopping rendering for safety.");

				this.stop();
			}
		}, 0, this.updateDelayTicks);
	}

	/**
	 * Stops this scoreboard and removes it from all viewers
	 */
	public final void stop() {
		this.viewers.forEach(viewerId -> {
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
		if (row.contains(":"))
			if (this.theme.length == 1)
				return this.theme[0] + row;

			else if (this.theme[0] != null) {
				final String[] split = row.split("\\:");

				if (split.length > 1)
					return this.theme[0] + split[0] + ":" + this.theme[1] + split[1];
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
	private void reloadEntries(Player player) throws IllegalArgumentException {
		final String colorizedTitle = Common.colorize(this.title);
		final Scoreboard scoreboard = player.getScoreboard();
		final List<String> rowsDone = new ArrayList<>();
		Objective mainboard = scoreboard.getObjective("mainboard");

		if (mainboard == null) {
			mainboard = scoreboard.registerNewObjective("mainboard", "dummy");
			mainboard.setDisplayName(colorizedTitle);
			mainboard.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		if (!mainboard.getDisplayName().equals(colorizedTitle))
			mainboard.setDisplayName(colorizedTitle);

		for (int lineNumber = 0; lineNumber < 15; lineNumber++) {
			final int scoreboardLineNumber = this.rows.size() - lineNumber;
			Team line = scoreboard.getTeam("line" + scoreboardLineNumber);

			if (lineNumber < this.rows.size()) {
				if (line == null)
					line = scoreboard.registerNewTeam("line" + scoreboardLineNumber);

				final String scoreboardLineRaw = this.rows.get(lineNumber).replace("{player}", player.getName());
				final boolean mc1_13 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);
				final boolean mc1_18 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_18);
				final String finishedRow = Common.colorize(replaceTheme(this.replaceVariables(player, scoreboardLineRaw)));
				final boolean rowUsed = rowsDone.contains(finishedRow);
				final int[] splitPoints = { mc1_13 ? 64 : 16, mc1_18 ? 32767 : 40, mc1_13 ? 64 : 16 };

				if (rowUsed)
					splitPoints[1] = splitPoints[1] - 2;

				final List<String> copy = copyColors(finishedRow, splitPoints);
				final String prefix = copy.isEmpty() ? "" : copy.get(0);
				String entry = copy.size() < 2 ? COLOR_CHAR + COLORS[lineNumber] + COLOR_CHAR + "r" : copy.get(1) + (rowUsed ? COLOR_CHAR + COLORS[lineNumber] : "");

				if (MinecraftVersion.olderThan(V.v1_13) && entry.length() > 16)
					entry = entry.substring(0, 16);

				final String suffix = copy.size() < 3 ? "" : copy.get(2);
				String oldEntry = null;

				if (!line.getPrefix().equals(prefix))
					line.setPrefix(prefix);

				if (line.getEntries().size() > 1) {
					for (final String teamEntry : line.getEntries()) {
						line.removeEntry(teamEntry);
						scoreboard.resetScores(teamEntry);
					}
				}

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
				rowsDone.add(finishedRow);
			} else if (line != null) {
				for (final String oldEntry : line.getEntries())
					scoreboard.resetScores(oldEntry);

				line.unregister();
			}
		}
	}

	/**
	 * @param text        The text containing the color codes
	 * @param splitPoints The points to split the text
	 * @return The method will split the text at the given splitPoints and will copy the colors over
	 */
	private List<String> copyColors(String text, int... splitPoints) {
		//Removes useless colors in front of only spaces (e.g. [§a     §aText] becomes [     §aText])
		final Pattern spaceMatcher = Pattern.compile("^( )+(" + COLOR_CHAR + ")");
		final List<String> splitText = new ArrayList<>();

		for (final int splitPoint : splitPoints) {
			final String lastEntry = splitText.isEmpty() ? "" : splitText.get(splitText.size() - 1);
			final String lastColor = ChatColor.getLastColors(lastEntry);

			final boolean addColor = !text.startsWith(COLOR_CHAR) && !lastColor.isEmpty() && !spaceMatcher.matcher(text).find();
			final int realSplitPoint = Math.min(splitPoint - (addColor ? 2 : 0), text.length());
			String line = (addColor ? lastColor : "") + text.substring(0, realSplitPoint);

			text = text.substring(realSplitPoint);

			if (line.endsWith(COLOR_CHAR)) {
				line = line.substring(0, line.length() - 1);
				text = COLOR_CHAR + text;
			}

			splitText.add(line);

			if (text.isEmpty())
				break;
		}

		return splitText;
	}
}