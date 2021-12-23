package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class SimpleScoreboard {

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * List of all active scoreboard (added upon creating a new instance)
	 */
	@Getter
	private final static List<SimpleScoreboard> registeredBoards = new ArrayList<>();

	/**
	 * Clears registered boards, usually called on reload
	 */
	public static final void clearBoards() {
		registeredBoards.clear();
	}

	/**
	 * Removes all scoreboard for a player
	 *
	 * @param player
	 */
	public static final void clearBoardsFor(final Player player) {
		for (final SimpleScoreboard scoreboard : registeredBoards)
			if (scoreboard.isViewing(player))
				scoreboard.hide(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Public entries
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Stored scoreboard lines
	 */
	@Getter
	private final List<String> rows = new ArrayList<>();

	/**
	 * A list of viewed scoreboards
	 */
	private final StrictList<ViewedScoreboard> scoreboards = new StrictList<>();

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
	@Getter
	private String title;

	/**
	 * The update tick delay
	 */
	@Getter
	private int updateDelayTicks;

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Stores a viewed scoreboard per player
	 */
	@Getter
	@Setter
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private class ViewedScoreboard {

		/**
		 * The scoreboard
		 */
		private final Scoreboard scoreboard;

		/**
		 * The objective
		 */
		private Objective objective;

		/**
		 * The viewer
		 */
		private final Player viewer;

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof ViewedScoreboard && ((ViewedScoreboard) obj).getViewer().equals(this.viewer);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Private entries
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The running update task
	 */
	private BukkitTask updateTask;

	/**
	 * Create a new scoreboard
	 */
	public SimpleScoreboard() {
		registeredBoards.add(this);
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
		addRows(Arrays.asList(entries));
	}

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(final List<String> entries) {
		rows.addAll(entries);
	}

	/**
	 * Remove all rows
	 */
	public final void clearRows() {
		rows.clear();
	}

	/**
	 * Remove row at the given index
	 *
	 * @param index
	 */
	public final void removeRow(final int index) {
		rows.remove(index);
	}

	/**
	 * Remove row that contains the given text
	 *
	 * @param thatContains
	 */
	public final void removeRow(final String thatContains) {
		for (final Iterator<String> it = rows.iterator(); it.hasNext();) {
			final String row = it.next();

			if (row.contains(thatContains))
				it.remove();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Start / stop
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Starts visualizing this scoreboard
	 */
	private final void start() {
		Valid.checkBoolean(updateTask == null, "Scoreboard " + this + " already running");

		updateTask = new BukkitRunnable() {
			@Override
			public void run() {

				try {
					update();

				} catch (final Throwable t) {
					final String lines = String.join(" ", rows);

					Common.error(t,
							"Error displaying " + SimpleScoreboard.this,
							"Entries: " + lines,
							"%error",
							"Stopping rendering for safety.");

					stop();
				}
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, updateDelayTicks);
	}

	/**
	 * Updates this scoreboard
	 */
	private final void update() {
		onUpdate();

		for (final ViewedScoreboard viewedScoreboard : scoreboards) {
			resetObjective(viewedScoreboard);
			reloadEntries(viewedScoreboard);
		}
	}

	/**
	 * Resets the objective
	 *
	 * @param viewedScoreboard
	 */
	private final void resetObjective(final ViewedScoreboard viewedScoreboard) {
		final Scoreboard scoreboard = viewedScoreboard.getScoreboard();
		Objective objective = viewedScoreboard.getObjective();

		if (objective != null)
			objective.unregister();

		objective = scoreboard.registerNewObjective(viewedScoreboard.getViewer().getName(), "dummy");

		objective.setDisplayName(Common.colorize(title));
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		viewedScoreboard.setObjective(objective);
	}

	/**
	 * Reload entries for the given player
	 *
	 * @param viewedScoreboard
	 */
	private final void reloadEntries(final ViewedScoreboard viewedScoreboard) {
		final Objective objective = viewedScoreboard.getObjective();
		final StrictList<String> duplicates = new StrictList<>();

		for (int i = rows.size(); i > 0; i--) {
			final String sidebarEntry = rows.get(rows.size() - i);
			final String entry = replaceVariables(viewedScoreboard.getViewer(), replaceTheme(sidebarEntry));

			String line = fixDuplicates(duplicates, entry);

			if (line.length() > 40)
				line = line.substring(0, 40);

			Remain.getScore(objective, line).setScore(i);
		}
	}

	/**
	 * Adds theme colors to the row if applicable
	 *
	 * @param row
	 * @return
	 */
	private final String replaceTheme(final String row) {
		if (theme != null && row.contains(":"))
			if (theme.length == 1)
				return theme[0] + row;

			else if (theme[0] != null) {
				final String[] split = row.split("\\:");

				if (split.length > 1)
					return theme[0] + split[0] + ":" + theme[1] + split[1];
			}

		return row;
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

	/**
	 * Fixes color and empty lines colliding into one by adding random colors
	 *
	 * @param duplicates
	 * @param message
	 * @return
	 */
	private final String fixDuplicates(final StrictList<String> duplicates, String message) {
		message = message.substring(0, 40);

		final boolean cut = MinecraftVersion.olderThan(V.v1_8);

		if (cut && message.length() > 16)
			message = message.substring(0, 16);

		if (duplicates.contains(message))
			for (int i = 0; i < duplicates.size() && message.length() < 40; i++)
				message += RandomUtil.nextColorOrDecoration();

		if (cut && message.length() > 16)
			message = message.substring(0, 16);

		duplicates.add(message);
		return message;
	}

	/**
	 * Replaces variables in the message for the given player
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	protected String replaceVariables(final Player player, final String message) {
		return message;
	}

	/**
	 * Called when this scoreboard is ticked
	 */
	protected void onUpdate() {
	}

	/**
	 * Stops this scoreboard and removes it from all viewers
	 */
	public final void stop() {
		for (final Iterator<ViewedScoreboard> iterator = scoreboards.iterator(); iterator.hasNext();) {
			final ViewedScoreboard score = iterator.next();

			score.getViewer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
			iterator.remove();
		}

		if (updateTask != null)
			cancelUpdateTask();
	}

	/**
	 * Cancels the update task
	 */
	private final void cancelUpdateTask() {
		Valid.checkNotNull(updateTask, "Scoreboard " + this + " not running");

		updateTask.cancel();
		updateTask = null;
	}

	/**
	 * Return true if the scoreboard is running and rendering?
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return updateTask != null;
	}

	/**
	 * @param updateDelayTicks the updateDelayTicks to set
	 */
	public final void setUpdateDelayTicks(int updateDelayTicks) {
		this.updateDelayTicks = updateDelayTicks;
	}

	/**
	 * @param title the title to set
	 */
	public final void setTitle(String title) {
		this.title = title;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Show this scoreboard to the player
	 *
	 * @param player
	 */
	public final void show(final Player player) {
		Valid.checkBoolean(!isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + getTitle());

		if (updateTask == null)
			start();

		final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

		scoreboards.add(new ViewedScoreboard(scoreboard, null, player));
		player.setScoreboard(scoreboard);
	}

	/**
	 * Hide this scoreboard from the player
	 *
	 * @param player
	 */
	public final void hide(final Player player) {
		Valid.checkBoolean(isViewing(player), "Player " + player.getName() + " is not viewing scoreboard: " + getTitle());

		player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

		for (final ViewedScoreboard viewed : scoreboards)
			if (viewed.getViewer().equals(player)) {
				scoreboards.remove(viewed);
				break;
			}

		if (scoreboards.isEmpty())
			cancelUpdateTask();
	}

	/**
	 * Returns true if the given player is viewing the scoreboard
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(final Player player) {
		for (final ViewedScoreboard viewed : scoreboards)
			if (viewed.getViewer().equals(player))
				return true;

		return false;
	}

	@Override
	public final String toString() {
		return "Scoreboard{title=" + getTitle() + "}";
	}
}