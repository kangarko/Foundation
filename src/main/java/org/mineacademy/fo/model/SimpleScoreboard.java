package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.SerializeUtil.Mode;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.Getter;
import lombok.NonNull;

/**
 * A simple way of rendering custom scoreboards for players with close to no flickering.
 * Using &c takes 2 characters. Since the text gets split in 3 parts, with colors = (total - (3 * 2)) = (total - 6)
 * Maximum line lengths:
 * - 1.8: 66 with colors, 70 without colors
 * - 1.13: 98 with colors, 104 without colors
 * - 1.18: 32889 with colors, 32895 without colors
 * Maximum title lengths:
 * - 1.8: 30 with colors, 32 without colors
 * - 1.13: 126 with colors, 128 without colors
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
		final String colorizedTitle = Common.colorize(title);

		this.title = colorizedTitle.length() > maxTitleLength ? colorizedTitle.substring(0, maxTitleLength) : colorizedTitle;
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
		Valid.checkBoolean((this.rows.size() + entries.size()) < 17, "You are trying to add too many rows (the limit is 16)");
		final List<String> lines = new ArrayList<>();

		for (Object object : entries)
			lines.add(object == null ? "" : Common.colorize(SerializeUtil.serialize(Mode.YAML, object).toString()));

		this.rows.addAll(lines);
	}

	/**
	 * Changes the row at the given index, if exists
	 *
	 * @param index
	 * @param value
	 */
	public final void setRow(final int index, final Object value) {
		Valid.checkBoolean(index < this.rows.size(), "The row for index " + index + " is currently not existing. Please use addRows()!");

		this.rows.set(index, value == null ? "" : Common.colorize(value.toString()));
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

				for (final UUID viewerId : new StrictList<>(this.viewers)) {
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
		final int size = this.rows.size();
		final Scoreboard scoreboard = player.getScoreboard();
		Objective mainboard = scoreboard.getObjective("mainboard");

		if (mainboard == null) {
			mainboard = scoreboard.registerNewObjective("mainboard", "dummy");
			mainboard.setDisplayName(this.title);
			mainboard.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		if (!mainboard.getDisplayName().equals(this.title))
			mainboard.setDisplayName(this.title);

		for (int lineNumber = 0; lineNumber < 16; lineNumber++) {
			final int scoreboardLineNumber = size - lineNumber;
			Team line = scoreboard.getTeam("line" + scoreboardLineNumber);

			if (lineNumber < this.rows.size()) {
				if (line == null)
					line = scoreboard.registerNewTeam("line" + scoreboardLineNumber);

				final String scoreboardLine = this.rows.get(lineNumber);
				final Function<String, String> replaceFunction = lineText -> this.replaceTheme(this.replaceVariables(player, lineText.replace("{player}", player.getName())));
				final String[] createdLine = createLine(scoreboardLine, COLOR_CHAR + COLORS[lineNumber] + COLOR_CHAR + "r", replaceFunction);

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

	/**
	 *
	 * @param text
	 * @param lineIdentifier
	 * @param textReplaceFunction
	 * @return
	 */
	private static String[] createLine(String rawText, String lineIdentifier, Function<String, String> textReplaceFunction) {
		final String[] line = new String[3];

		final String text = textReplaceFunction.apply(rawText);
		final boolean mc1_13 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);
		final boolean mc1_18 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_18);
		final int[] splitPoints = mc1_13 ? new int[] { 64, mc1_18 ? 32831 : 104 } : new int[] { 16, 56 };
		final int length = mc1_18 ? 32895 : (mc1_13 ? 168 : 70);
		final List<String> copy = copyColors(text, length, splitPoints);

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
	private static List<String> copyColors(String text, int maxLength, int... splitPoints) {
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

		int stringLength = 0;

		for (final String line : splitText)
			stringLength += line.length();

		if (stringLength > maxLength)
			splitText = passMergeThrough(null, splitText, maxLength);

		return splitText;
	}

	private static List<String> passMergeThrough(String marge, List<String> text, int maxLength) {
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