package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.platform.BukkitPlugin;
import org.mineacademy.fo.remain.Remain;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import lombok.Getter;
import lombok.NonNull;

/**
 * A simple way of rendering custom scoreboards for players with zero flickering.
 * Uses fixed invisible entries per line and updates only team prefix/suffix.
 * Using &c takes 2 characters. Since the text gets split once, text with colors = (total - 2)
 * Maximum line lengths:
 * - 1.8: 30 with color, 32 without color
 * - 1.13: 126 with color, 128 without color
 * - 1.18: 65532 with color, 65534 without color
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
	 * Pre-compiled pattern for stripping useless color codes before spaces in {@link #copyColors(String, int...)}
	 */
	private static final Pattern SPACE_COLOR_PATTERN = Pattern.compile("^( )+(" + COLOR_CHAR + ")");

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
	 * Whether the Folia unsupported warning was already printed.
	 */
	private static boolean foliaWarned = false;

	/**
	 * Whether the server supports hiding the right-side red score numbers,
	 * added in the Paper API for Minecraft 1.20.3.
	 */
	private static boolean hasNumberFormat = true;

	/**
	 * Cache flags for performance purposes.
	 */
	private final boolean atLeast1_13 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);
	private final boolean atLeast1_18 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_18);

	/**
	 * Stored scoreboard lines
	 */
	private final List<String> rows = new ArrayList<>();

	/**
	 * Stored players viewing THIS scoreboard
	 */

	private final List<UUID> viewers = new ArrayList<>();

	/**
	 * The color theme for key such as "Players" in "Players: 12"
	 */
	private CompChatColor primaryTheme;

	/**
	 * The color theme for value such as "12" in "Players: 12"
	 */
	private CompChatColor secondaryTheme;

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

	public SimpleScoreboard(final String title) {
		this(title, 20);
	}

	/**
	 * Create a new scoreboard with customizable updateDelayTicks
	 * @param title
	 * @param updateDelayTicks
	 */
	public SimpleScoreboard(final String title, final int updateDelayTicks) {
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
	public final void setTitle(final String title) {
		final int maxTitleLength = this.atLeast1_13 ? 128 : 32;

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
	public final void setUpdateDelayTicks(final int updateDelayTicks) {
		this.updateDelayTicks = updateDelayTicks;
	}

	/**
	 * Show this scoreboard to the player
	 *
	 * @param player
	 */
	public final void show(final Player player) {
		if (Remain.isFolia()) {
			if (!foliaWarned) {
				CommonCore.warning("Scoreboards are unsupported on Folia, scoreboards will not be shown.");

				foliaWarned = true;
			}

			return;
		}

		ValidCore.checkBoolean(!this.isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + this);

		if (this.title == null)
			this.title = "";

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
		ValidCore.checkBoolean(this.isViewing(player), "Player " + player.getName() + " is not viewing scoreboard: " + this.getTitle());

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
	 * @param primaryTheme
	 * @param secondaryTheme
	 */
	public final void setTheme(final CompChatColor primaryTheme, final CompChatColor secondaryTheme) {
		this.primaryTheme = primaryTheme;
		this.secondaryTheme = secondaryTheme;
	}

	/**
	 * See {@link #setTheme(CompChatColor, CompChatColor)}
	 *
	 * @param primaryTheme
	 */
	public final void setPrimaryTheme(final CompChatColor primaryTheme) {
		this.primaryTheme = primaryTheme;
	}

	/**
	 * See {@link #setTheme(CompChatColor, CompChatColor)}
	 *
	 * @param secondaryTheme
	 */
	public final void setSecondaryTheme(final CompChatColor secondaryTheme) {
		this.secondaryTheme = secondaryTheme;
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
		ValidCore.checkBoolean(this.rows.size() + entries.size() <= 15, "You are trying to add too many rows (the limit is 15)");

		for (final String line : entries)
			this.rows.add(line == null ? "" : CompChatColor.translateColorCodes(line));
	}

	/**
	 * Changes the row at the given index, if exists
	 *
	 * @param index
	 * @param value
	 */
	public final void setRow(final int index, final String value) {
		ValidCore.checkBoolean(index < this.rows.size(), "The row for index " + index + " is currently not existing. Please use addRows()!");

		this.rows.set(index, value == null ? "" : CompChatColor.translateColorCodes(value));
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
		ValidCore.checkBoolean(this.updateTask == null, "Scoreboard " + this + " already running");

		this.updateTask = Bukkit.getScheduler().runTaskTimer(BukkitPlugin.getInstance(), () -> {
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
				CommonCore.error(t,
						"Failed to render scoreboard: " + this,
						"Entries: " + this.rows,
						"Title: " + this.title,
						"Error: {error}",
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
		if (this.primaryTheme == null && this.secondaryTheme == null)
			return row;

		final CompChatColor primary = CommonCore.getOrDefault(this.primaryTheme, CompChatColor.RESET);
		final String[] split = row.split("\\:");

		if (split.length > 1)
			return primary + split[0] + ":" + CommonCore.getOrDefault(this.secondaryTheme, CompChatColor.RESET) + split[1];

		else
			return primary + row;
	}

	/**
	 * Cancels the update task
	 */
	private void cancelUpdateTask() {
		ValidCore.checkNotNull(this.updateTask, "Scoreboard " + this + " not running");

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
		final String colorizedTitle = CompChatColor.translateColorCodes(this.title);
		final Scoreboard scoreboard = player.getScoreboard();
		Objective mainboard = scoreboard.getObjective("mainboard");

		if (mainboard == null) {
			mainboard = scoreboard.registerNewObjective("mainboard", "dummy");

			mainboard.setDisplayName(colorizedTitle);
			mainboard.setDisplaySlot(DisplaySlot.SIDEBAR);

			if (hasNumberFormat)
				try {
					mainboard.numberFormat(NumberFormat.blank());

				} catch (final NoClassDefFoundError | NoSuchMethodError err) {
					hasNumberFormat = false;
				}
		}

		if (!mainboard.getDisplayName().equals(colorizedTitle))
			mainboard.setDisplayName(colorizedTitle);

		for (int lineNumber = 0; lineNumber < 15; lineNumber++) {
			final String entry = COLOR_CHAR + COLORS[lineNumber] + COLOR_CHAR + "r";
			Team line = scoreboard.getTeam("line" + lineNumber);

			if (lineNumber < this.rows.size()) {

				if (line == null) {
					line = scoreboard.registerNewTeam("line" + lineNumber);
					line.addEntry(entry);
				}

				final String scoreboardLineRaw = this.rows.get(lineNumber).replace("{player}", player.getName());
				final String finishedRow = CompChatColor.translateColorCodes(this.replaceTheme(this.replaceVariables(player, scoreboardLineRaw)));
				final int maxLength = this.atLeast1_18 ? 32767 : (this.atLeast1_13 ? 64 : 16);
				final List<String> parts = this.copyColors(finishedRow, maxLength, maxLength);

				final String prefix = parts.isEmpty() ? "" : parts.get(0);
				final String suffix = parts.size() < 2 ? "" : parts.get(1);

				if (!line.getPrefix().equals(prefix))
					line.setPrefix(prefix);

				if (!line.getSuffix().equals(suffix))
					line.setSuffix(suffix);

				mainboard.getScore(entry).setScore(this.rows.size() - lineNumber);

			} else if (line != null) {
				scoreboard.resetScores(entry);
				line.unregister();
			}
		}
	}

	/**
	 * @param text        The text containing the color codes
	 * @param splitPoints The points to split the text
	 * @return The method will split the text at the given splitPoints and will copy the colors over
	 */
	private List<String> copyColors(String text, final int... splitPoints) {
		// Removes useless colors in front of only spaces (e.g. [§a     §aText] becomes [     §aText])
		final List<String> splitText = new ArrayList<>();

		for (final int splitPoint : splitPoints) {
			final String lastEntry = splitText.isEmpty() ? "" : splitText.get(splitText.size() - 1);
			final String lastColor = CompChatColor.getLastColors(lastEntry);

			final boolean addColor = !text.startsWith(COLOR_CHAR) && !lastColor.isEmpty() && !SPACE_COLOR_PATTERN.matcher(text).find();
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