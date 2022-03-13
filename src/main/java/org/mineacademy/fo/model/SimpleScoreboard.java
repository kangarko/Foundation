package org.mineacademy.fo.model;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import java.util.ArrayList;
import java.util.UUID;

/**
 * A simple way of rendering custom scoreboards for players with close to no flickering.
 *
 * @author kangarko and Tijn (https://github.com/Tvhee-Dev)
 */

// 31 or 30 is the maximum, depends on the chatcolor from the previous line
// 16 for prefix, COLOR_CHAR + chatcolor and 14 more characters for the suffix
// Maybe there is a way to get more characters, but then you have to play with the entries
// (Team.addEntry() and add the same entry on the scoreboard)
// Refer to https://www.spigotmc.org/wiki/making-scoreboard-with-teams-no-flicker/ for more information
public class SimpleScoreboard {

    // ------------------------------------------------------------------------------------------------------------
    // Static
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Unique chat color identifiers for specific team entries
     */
    private static final StrictMap<Integer, String> lineEntries;

    static {
        lineEntries = new StrictMap<>();

        for (int i = 0; i < 10; i++)
            lineEntries.put(i, Common.COLOR_CHAR + i + Common.COLOR_CHAR + "r");

        lineEntries.put(10, Common.COLOR_CHAR + "a" + Common.COLOR_CHAR + "r");
        lineEntries.put(11, Common.COLOR_CHAR + "b" + Common.COLOR_CHAR + "r");
        lineEntries.put(12, Common.COLOR_CHAR + "c" + Common.COLOR_CHAR + "r");
        lineEntries.put(13, Common.COLOR_CHAR + "d" + Common.COLOR_CHAR + "r");
        lineEntries.put(14, Common.COLOR_CHAR + "e" + Common.COLOR_CHAR + "r");
        lineEntries.put(15, Common.COLOR_CHAR + "f" + Common.COLOR_CHAR + "r");
    }

    /**
     * List of all active scoreboard (added upon creating a new instance)
     */
    @Getter
    private final static StrictList<SimpleScoreboard> registeredBoards = new StrictList<>();

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

    /**
     * The running update task
     */
    private BukkitTask updateTask;

    /**
     * Stored scoreboard lines
     */
    @Getter
    private final StrictList<String> rows = new StrictList<>();

    /**
     * A list of viewed scoreboards
     */
    private final StrictList<UUID> viewedToPlayers = new StrictList<>();

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
    // Public entries
    // ------------------------------------------------------------------------------------------------------------

    public SimpleScoreboard(String title) {
        this(title, 1);
    }

    /**
     * Create a new scoreboard
     */
    //You MUST put in a title and updateDelayTicks, because otherwise no scoreboard visible! That's why I made this constructor
    public SimpleScoreboard(String title, int updateDelayTicks) {
        setTitle(title);
        setUpdateDelayTicks(updateDelayTicks);
        registeredBoards.add(this);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Overridable
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Replaces variables in the message for the given player
     *
     * @param player
     * @param message
     * @return
     */
    protected @NotNull String replaceVariables(final @NonNull Player player, final @NonNull String message) {
        return message;
    }

    /**
     * Called when this scoreboard is ticked
     */
    protected void onUpdate() {
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
        this.addRows(new StrictList<>(entries));
    }

    /**
     * Add rows onto the scoreboard
     *
     * @param entries
     */
    public final void addRows(final StrictList<String> entries) {
        this.rows.addAll(entries);
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

        for(UUID viewer : new StrictList<>(this.viewedToPlayers)) {
            Player player = Bukkit.getPlayer(viewer);

            if(player != null)
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

            this.viewedToPlayers.remove(viewer);
        }

        if (this.updateTask != null)
            this.cancelUpdateTask();
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
        Valid.checkBoolean(!this.isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + this.getTitle());

        if (this.updateTask == null)
            this.start();

        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.viewedToPlayers.add(player.getUniqueId());
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
        this.viewedToPlayers.removeIf(uuid -> player.getUniqueId().equals(uuid));

        if (this.viewedToPlayers.isEmpty())
            this.cancelUpdateTask();
    }

    /**
     * Returns true if the given player is viewing the scoreboard
     *
     * @param player
     * @return
     */
    public final boolean isViewing(final Player player) {
        return this.viewedToPlayers.contains(player.getUniqueId());
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

        for (UUID viewer : new StrictList<>(this.viewedToPlayers)) {
            Player player = Remain.getPlayerByUUID(viewer);

            if(player == null || !player.isOnline() || player.isDead())
                this.viewedToPlayers.remove(viewer);
            else
                this.reloadEntries(player);
        }
    }

    /**
     * Cancels the update task
     */
    private void cancelUpdateTask() {
        Valid.checkNotNull(this.updateTask, "Scoreboard " + this + " not running");

        this.updateTask.cancel();
        this.updateTask = null;
    }

    /**
     * Reload entries for the given player
     *
     * @param player The player to reload the scoreboard for
     */
    private void reloadEntries(Player player) {
        int size = Math.min(this.rows.size(), 16);
        Scoreboard scoreboard = player.getScoreboard();
        Objective mainboard = scoreboard.getObjective("mainboard");
        String colorizedTitle = Common.colorize(this.title);
        colorizedTitle = colorizedTitle.length() > 32 ? colorizedTitle.substring(0, 32) : colorizedTitle;

        if (mainboard == null) {
            mainboard = scoreboard.registerNewObjective("mainboard", "dummy");
            mainboard.setDisplayName(colorizedTitle);
            mainboard.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        if (!mainboard.getDisplayName().equals(colorizedTitle))
            mainboard.setDisplayName(colorizedTitle);

        for (int lineNumber = 0; lineNumber < 16; lineNumber++) {
            int scoreboardLineNumber = size - lineNumber;
            Team line = scoreboard.getTeam("line" + scoreboardLineNumber);

            if (lineNumber < this.rows.size()) {
                String text = this.replaceTheme(this.replaceVariables(player, this.rows.get(lineNumber)));
                ScoreboardLine scoreboardLine = new ScoreboardLine(text, lineEntries.get(lineNumber));

                if (line == null)
                    line = scoreboard.registerNewTeam("line" + scoreboardLineNumber);

                String oldEntry = scoreboardLine.compare(line);

                if(oldEntry != null)
                    scoreboard.resetScores(oldEntry);

                Remain.getScore(mainboard, scoreboardLine.teamEntry).setScore(scoreboardLineNumber);
            } else if (line != null) {
                for (String oldEntry : line.getEntries())
                    scoreboard.resetScores(oldEntry);

                line.unregister();
            }
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

    // ------------------------------------------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------------------------------------------

    @Getter
    private static class ScoreboardLine {
        private final String prefix;
        private final String teamEntry;
        private final String suffix;

        private ScoreboardLine(String text, String lineIdentifier) {

            StrictList<String> copy = new StrictList<>();

            if(MinecraftVersion.olderThan(MinecraftVersion.V.v1_13))
                Common.copyColors(text, 48, 16, 32);
            else
                Common.copyColors(text, 160, 16, 144);

            this.prefix = copy.isEmpty() ? "" : copy.get(0);
            this.teamEntry = copy.size() < 2 ? lineIdentifier : copy.get(1);
            this.suffix = copy.size() < 3 ? "" : copy.get(2);
        }

        /**
         * Returns the OLD ENTRY if it has been changed
         * @author Tvhee-Dev (https://github.com/tvhee-dev/)
         */
        public String compare(Team team) {
            String oldEntry = null;
            String teamPrefix = Common.colorize(team.getPrefix());
            String linePrefix = Common.colorize(prefix);

            if (!teamPrefix.equals(linePrefix))
                team.setPrefix(linePrefix);

            String lineEntry = Common.colorize(teamEntry);

            if(team.getEntries().size() > 1)
                throw new IllegalArgumentException("Something went wrong!");

            if(!team.getEntries().contains(lineEntry)) {

                if(!team.getEntries().isEmpty()) {
                    oldEntry = new ArrayList<>(team.getEntries()).get(0);
                    team.removeEntry(oldEntry);
                }

                team.addEntry(lineEntry);
            }

            String teamSuffix = Common.colorize(team.getSuffix());
            String lineSuffix = Common.colorize(suffix);

            if (!teamSuffix.equals(lineSuffix))
                team.setSuffix(lineSuffix);

            return oldEntry;
        }
    }
}