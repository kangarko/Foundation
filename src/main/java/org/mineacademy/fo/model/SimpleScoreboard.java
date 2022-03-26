package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.Getter;
import lombok.NonNull;

/**
 * A simple way of rendering custom scoreboards for players with close to no flickering.
 *
 * @author kangarko and Tijn (https://github.com/Tvhee-Dev)
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
	private static final Map<Integer, String> lineEntries;

	/**
	 * Stored scoreboard lines
	 */
	@Getter
	private final List<String> rows = new ArrayList<>();

	/**
	 * Stored players viewing THIS scoreboard
	 */

	private final StrictList<Player> scoreboards = new StrictList<>();

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
	private int updateDelayTicks;

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

	/**
	 * Create a new scoreboard with customizable updateDelayTicks
	 */
	public SimpleScoreboard(String title, int updateDelayTicks) {
		setTitle(title);
		setUpdateDelayTicks(updateDelayTicks);

		registeredBoards.add(this);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	static {
		lineEntries = new HashMap<>();

		for (int i = 0; i < 10; i++)
			lineEntries.put(i, COLOR_CHAR + i + COLOR_CHAR + "r");

		lineEntries.put(10, COLOR_CHAR + "a" + COLOR_CHAR + "r");
		lineEntries.put(11, COLOR_CHAR + "b" + COLOR_CHAR + "r");
		lineEntries.put(12, COLOR_CHAR + "c" + COLOR_CHAR + "r");
		lineEntries.put(13, COLOR_CHAR + "d" + COLOR_CHAR + "r");
		lineEntries.put(14, COLOR_CHAR + "e" + COLOR_CHAR + "r");
		lineEntries.put(15, COLOR_CHAR + "f" + COLOR_CHAR + "r");
	}

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
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public final void setTitle(String title) {
		this.title = Common.colorize(title);
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
		Valid.checkBoolean(!this.isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + this.getTitle());

		if (this.updateTask == null)
			this.start();

		final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		this.scoreboards.add(player);
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
		this.scoreboards.remove(player);

		if (this.scoreboards.isEmpty())
			this.cancelUpdateTask();
	}

	/**
	 * Returns true if the given player is viewing the scoreboard
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(final Player player) {
		return this.scoreboards.contains(player);
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
	public final void addRows(final String... entries) {
		this.addRows(Arrays.asList(entries));
	}

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final List<String> entries) {
		this.rows.addAll(Common.colorize(entries));
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

		this.updateTask = new BukkitRunnable() {
			@Override
			public void run() {

				try {
					SimpleScoreboard.this.update();

				} catch (final Throwable t) {
					final String lines = String.join(" ", SimpleScoreboard.this.rows);

					Common.error(t,
							"Error displaying " + SimpleScoreboard.this,
							"Entries: " + lines,
							"%error",
							"Stopping rendering for safety.");

					SimpleScoreboard.this.stop();
				}
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, this.updateDelayTicks);
	}

	/**
	 * Stops this scoreboard and removes it from all viewers
	 */
	public final void stop() {

		this.scoreboards.forEach(viewer -> {
			if (viewer.isOnline())
				viewer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		});

		this.scoreboards.clear();

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
	 * Updates this scoreboard
	 */
	private void update() {
		this.onUpdate();

		for (final Player viewer : new StrictList<>(scoreboards)) {
			if (!viewer.isOnline()) {
				scoreboards.remove(viewer);
				continue;
			}

			this.reloadEntries(viewer);
		}
	}

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
	private void reloadEntries(Player player) {
		final int size = Math.min(this.rows.size(), 16);
		final Scoreboard scoreboard = player.getScoreboard();
		Objective mainboard = scoreboard.getObjective("mainboard");
		String colorizedTitle = ChatColor.translateAlternateColorCodes('&', this.title);
		colorizedTitle = colorizedTitle.length() > 32 ? colorizedTitle.substring(0, 32) : colorizedTitle;

		if (mainboard == null) {
			mainboard = scoreboard.registerNewObjective("mainboard", "dummy");
			mainboard.setDisplayName(colorizedTitle);
			mainboard.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		if (!mainboard.getDisplayName().equals(colorizedTitle))
			mainboard.setDisplayName(colorizedTitle);

		for (int lineNumber = 0; lineNumber < 16; lineNumber++) {
			final int scoreboardLineNumber = size - lineNumber;
			Team line = scoreboard.getTeam("line" + scoreboardLineNumber);

			if (lineNumber < this.rows.size()) {
				final String text = this.replaceTheme(this.replaceVariables(player, this.rows.get(lineNumber)));
				final ScoreboardLine scoreboardLine = new ScoreboardLine(text, lineEntries.get(lineNumber));

				if (line == null)
					line = scoreboard.registerNewTeam("line" + scoreboardLineNumber);

				final String oldEntry = scoreboardLine.compare(line);

				if (oldEntry != null)
					scoreboard.resetScores(oldEntry);

				mainboard.getScore(scoreboardLine.teamEntry).setScore(scoreboardLineNumber);
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

	private static class ScoreboardLine {
		private final String prefix;
		private final String teamEntry;
		private final String suffix;

		private ScoreboardLine(String text, String lineIdentifier) {
			final List<String> copy = copyColors(text, 48, 16, 32);

			this.prefix = copy.isEmpty() ? "" : copy.get(0);
			this.teamEntry = copy.size() < 2 ? lineIdentifier : copy.get(1);
			this.suffix = copy.size() < 3 ? "" : copy.get(2);
		}

		public String compare(Team team) {
			String oldEntry = null;
			final String teamPrefix = ChatColor.translateAlternateColorCodes('&', team.getPrefix());
			final String linePrefix = ChatColor.translateAlternateColorCodes('&', prefix);

			if (!teamPrefix.equals(linePrefix))
				team.setPrefix(linePrefix);

			final String lineEntry = ChatColor.translateAlternateColorCodes('&', teamEntry);

			if (team.getEntries().size() > 1)
				throw new IllegalArgumentException("Something went wrong!");

			if (!team.getEntries().contains(lineEntry)) {
				if (!team.getEntries().isEmpty()) {
					oldEntry = new ArrayList<>(team.getEntries()).get(0);
					team.removeEntry(oldEntry);
				}

				team.addEntry(lineEntry);
			}

			final String teamSuffix = ChatColor.translateAlternateColorCodes('&', team.getSuffix());
			final String lineSuffix = ChatColor.translateAlternateColorCodes('&', suffix);

			if (!teamSuffix.equals(lineSuffix))
				team.setSuffix(lineSuffix);

			return oldEntry;
		}
	}

	/**
	 * @param text        The text containing the color codes
	 * @param maxLength   The maximum length the text can have all added
	 * @param splitPoints The points to split the text
	 * @return The method will split the text at the given splitPoints and will copy the colors over
	 */
	public static List<String> copyColors(String text, int maxLength, int... splitPoints) {
		if (maxLength < 1)
			return new ArrayList<>();

		List<String> splitText = new ArrayList<>();
		int lastSplitPoint = 0;

		for (final int splitPoint : splitPoints) {

			if (splitPoint < 0)
				throw new IllegalArgumentException("SplitPoints cannot be negative!");
			else if (splitPoint <= lastSplitPoint)
				throw new IllegalArgumentException("Please put your splitPoints in order from small to large!");
			else if (splitPoint >= text.length() || splitPoint >= maxLength)
				break;

			splitText.add(text.substring(lastSplitPoint, splitPoint));
			lastSplitPoint = splitPoint;
		}

		if (text.length() > lastSplitPoint)
			splitText.add(text.substring(lastSplitPoint, Math.min(text.length(), maxLength)));

		for (int index = 1; index < splitText.size(); index++) {
			final int currentSplitPoint = splitPoints.length > index ? splitPoints[index] : maxLength;
			final int previousSplitPoint = splitPoints.length > index - 1 ? splitPoints[index - 1] : 0;
			final int maxSubStringLength = currentSplitPoint - previousSplitPoint;
			final boolean lastLoop = index == splitText.size() - 1;
			String previousText = splitText.get(index - 1);
			final String currentText = splitText.get(index);
			String fixedText = currentText;
			String marge = null;

			if (previousText.endsWith(SimpleScoreboard.COLOR_CHAR)) {
				previousText = previousText.substring(0, previousText.length() - 1);
				fixedText = SimpleScoreboard.COLOR_CHAR + currentText;

				if (!lastLoop) {
					marge = fixedText.substring(fixedText.length() - 1);
					fixedText = fixedText.substring(0, fixedText.length() - 1);
				}
			} else if (!currentText.startsWith(SimpleScoreboard.COLOR_CHAR)) {
				final String lastColor = ChatColor.getLastColors(previousText);
				fixedText = lastColor + currentText;

				if (!lastLoop) {
					marge = fixedText.substring(fixedText.length() - 2);
					fixedText = fixedText.substring(0, fixedText.length() - 2);
				}
			}

			if (fixedText.length() > maxSubStringLength) {
				marge = fixedText.substring(maxSubStringLength);
				fixedText = fixedText.substring(0, maxSubStringLength);
			}

			splitText.set(index - 1, previousText);
			splitText.set(index, fixedText);

			List<String> linesWithMarge = new ArrayList<>();

			for (int j = index + 1; j < splitText.size(); j++)
				linesWithMarge.add(splitText.get(j));

			linesWithMarge = passMergeThrough(marge, linesWithMarge, maxLength);

			for (int j = 0; j < linesWithMarge.size(); j++)
				splitText.set(index + 1 + j, linesWithMarge.get(j));
		}

		if (getStringLength(splitText) > maxLength)
			splitText = passMergeThrough(null, splitText, maxLength);

		return splitText;
	}

	public static int getStringLength(List<String> text) {
		int length = 0;

		for (final String line : text)
			length += line.length();

		return length;
	}

	public static List<String> passMergeThrough(String marge, List<String> text, int maxLength) {
		final StringBuilder textBuilder = new StringBuilder(marge == null ? "" : marge);
		final List<Integer> stringLengths = new ArrayList<>();
		final List<String> newSplitText = new ArrayList<>();

		for (final String line : text) {
			stringLengths.add(line.length());
			textBuilder.append(line);
		}

		String textWithMarge = textBuilder.toString();
		textWithMarge = textWithMarge.substring(0, Math.min(maxLength, textWithMarge.length()));
		int previousLength = 0;

		for (int index = 0; index < stringLengths.size(); index++) {
			int length = stringLengths.get(index);

			if (index == stringLengths.size() - 1)
				length = Math.max(length, maxLength);

			final String subString = textWithMarge.substring(previousLength, Math.min(textWithMarge.length(), previousLength + length));

			if (!subString.isEmpty())
				newSplitText.add(subString);

			previousLength += length;
		}

		return newSplitText;
	}
}