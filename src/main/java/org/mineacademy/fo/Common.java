package org.mineacademy.fo;

import lombok.*;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.RegexTimeoutException;
import org.mineacademy.fo.model.DiscordSender;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTItem;
import org.mineacademy.fo.settings.ConfigSection;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.SimpleSettings;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.bukkit.ChatColor.COLOR_CHAR;

/**
 * Our main utility class hosting a large variety of different convenience functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common {

    // ------------------------------------------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------------------------------------------

    /**
     * {@link Pattern} used to match colors with & or {@link ChatColor#COLOR_CHAR}.
     */
    private static final Pattern COLOR_AND_DECORATION_REGEX = Pattern.compile("(&|" + COLOR_CHAR + ")[0-9a-fk-orA-FK-OR]");

    /**
     * {@link Pattern} used to match colors with #HEX code for Minecraft 1.16+.
     * <p>
     * Matches {#CCCCCC}, &#CCCCCC or #CCCCCC.
     */
    public static final Pattern HEX_COLOR_REGEX = Pattern.compile("(?<!\\\\)(\\{|&|)#((?:[0-9a-fA-F]{3}){2})(}|)");

    /**
     * {@link Pattern} used to match colors with #HEX code for Minecraft 1.16+.
     */
    private static final Pattern RGB_X_COLOR_REGEX = Pattern.compile("(" + COLOR_CHAR + "x)(" + COLOR_CHAR + "[0-9a-fA-F]){6}");

    /**
     * High performance regular expression matcher for colors, used in {@link #stripColors(String)}.
     */
    private static final Pattern ALL_IN_ONE = Pattern.compile("((&|" + COLOR_CHAR + ")[0-9a-fk-or])|(" + COLOR_CHAR + "x(" + COLOR_CHAR + "[0-9a-fA-F]){6})|((?<!\\\\)(\\{|&|)#((?:[0-9a-fA-F]{3}){2})(}|))");

    /**
     * Used to send messages to players without repetition, e.g. if they attempt to break a block in a restricted
     * region, we will not spam their chat with "You cannot break this block here" 120x times. Instead, we only send
     * this message once per X seconds. This cache holds the last times when we sent that message so we know how long to
     * wait before the next one.
     */
    private static final Map<String, Long> TIMED_TELL_CACHE = new HashMap<>();

    /**
     * This is the same as {@link #TIMED_TELL_CACHE}, but this is for sending messages to your console.
     */
    private static final Map<String, Long> TIMED_LOG_CACHE = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Tell prefix
    // ------------------------------------------------------------------------------------------------------------

    /**
     * The tell prefix applied on tell() methods, defaults to an empty {@link String}.
     */
    @Getter
    private static String tellPrefix = "";

    /**
     * The log prefix applied on log() methods, defaults to [PluginName]
     */
    @Getter
    private static String logPrefix = "[" + SimplePlugin.getNamed() + "]";

    /**
     * Sets the tell prefix applied for messages to players from tell() methods
     * <p>
     * & color codes are translated automatically.
     *
     * @param prefix the new tell prefix to set.
     */
    public static void setTellPrefix(@Nullable final String prefix) {
        tellPrefix = prefix == null ? "" : colorize(prefix);
    }

    /**
     * Sets the log prefix applied for messages in the console from log() methods.
     * <p>
     * & color codes are translated automatically.
     *
     * @param prefix the new log prefix to set.
     */
    public static void setLogPrefix(@Nullable final String prefix) {
        logPrefix = prefix == null ? "" : colorize(prefix);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Broadcasting
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Broadcasts the message as per {@link Replacer#replaceArray(String, Object...)} mechanics such as
     * {@code broadcastReplaced("Hello {world} from {player}", "world", "survival_world", "player", "kangarko")}
     *
     * @param message      the message to broadcast.
     * @param replacements optional variables to replace in the message.
     */
    public static void broadcastReplaced(final String message, final Object... replacements) {
        broadcast(Replacer.replaceArray(message, replacements));
    }

    /**
     * Broadcasts the message, replacing {player} with the given {@link CommandSender}.
     *
     * @param message the message to broadcast.
     * @param sender  the {@link CommandSender} to replace {player} with.
     */
    public static void broadcast(final String message, final CommandSender sender) {
        broadcast(message, resolveSenderName(sender));
    }

    /**
     * Broadcasts the message replacing {player} variable with the given player replacement.
     *
     * @param message           the message to broadcast.
     * @param playerReplacement the name to replace {player} with.
     */
    public static void broadcast(final String message, final String playerReplacement) {
        broadcast(message.replace("{player}", playerReplacement));
    }

    /**
     * Broadcasts the message to everyone and logs it
     *
     * @param messages the messages to broadcast.
     */
    public static void broadcast(final String... messages) {
        if (!Valid.isNullOrEmpty(messages))
            for (final String message : messages) {
                for (final Player online : Remain.getOnlinePlayers())
                    tellJson(online, message);

                log(message);
            }
    }

    /**
     * Sends the given messages to all recipients.
     *
     * @param recipients the recipients to send the messages to.
     * @param messages   the messages to send.
     */
    public static void broadcastTo(final Iterable<? extends CommandSender> recipients, final String... messages) {
        for (final CommandSender sender : recipients)
            tell(sender, messages);
    }

    /**
     * Broadcasts the message to everyone with the given permission.
     *
     * @param showPermission the permission required to see the message.
     * @param message        the message to broadcast.
     * @param log            should the message be logged to the console?
     */
    public static void broadcastWithPerm(final String showPermission, final String message, final boolean log) {
        if (message != null && !"none".equals(message)) {
            for (final Player online : Remain.getOnlinePlayers())
                if (PlayerUtil.hasPerm(online, showPermission))
                    tellJson(online, message);

            if (log)
                log(message);
        }
    }

    /**
     * Broadcasts the message to everyone with the given permission.
     *
     * @param permission the permission required to see the message.
     * @param message    the message to broadcast.
     * @param log        should the message be logged to the console?
     */
    public static void broadcastWithPerm(final String permission, @NonNull final TextComponent message, final boolean log) {
        final String legacy = message.toLegacyText();

        if (!"none".equals(legacy)) {
            for (final Player online : Remain.getOnlinePlayers())
                if (PlayerUtil.hasPerm(online, permission))
                    Remain.sendComponent(online, message);

            if (log)
                log(legacy);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Messaging
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Sends a message to the given {@link CommandSender} and saves the time it was sent. The delay in seconds is the
     * delay between which we won't send the sender the same message, in case you call this method again.
     * <p>
     * This method does not prepend the message with {@link #getTellPrefix()}.
     *
     * @param delaySeconds the delay (in seconds) to wait before sending the message to the sender again.
     * @param sender       the {@link CommandSender} to send the message to.
     * @param message      the message to send.
     */
    public static void tellTimedNoPrefix(final int delaySeconds, final CommandSender sender, final String message) {
        final String oldPrefix = getTellPrefix();
        setTellPrefix("");

        tellTimed(delaySeconds, sender, message);
        setTellPrefix(oldPrefix);
    }

    /**
     * Sends a message to the given {@link CommandSender} and saves the time it was sent. The delay in seconds is the
     * delay between which we won't send the sender the same message, in case you call this method again.
     *
     * @param delaySeconds the delay (in seconds) to wait before sending the message to the sender again.
     * @param sender       the {@link CommandSender} to send the message to.
     * @param message      the message to send.
     */
    public static void tellTimed(final int delaySeconds, final CommandSender sender, final String message) {
        // No previous message stored, just tell the player now.
        if (!TIMED_TELL_CACHE.containsKey(message)) {
            tell(sender, message);

            TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
            return;
        }

        if (TimeUtil.currentTimeSeconds() - TIMED_TELL_CACHE.get(message) > delaySeconds) {
            tell(sender, message);

            TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
        }
    }

    /**
     * Sends the {@link Conversable} a message after the given delay.
     *
     * @param delayTicks  the delay (in ticks) to wait before sending the message.
     * @param conversable the {@link Conversable} to send the message to.
     * @param message     the message to send.
     */
    public static void tellLaterConversing(final int delayTicks, final Conversable conversable, final String message) {
        runLater(delayTicks, () -> tellConversing(conversable, message));
    }

    /**
     * Sends the {@link Conversable} a message.
     * <p>
     * & color codes are translated automatically.
     *
     * @param conversable the {@link Conversable} to send the message to.
     * @param message     the message to send.
     */
    public static void tellConversing(final Conversable conversable, final String message) {
        conversable.sendRawMessage(colorize((message.contains(tellPrefix) ? "" : addLastSpace(tellPrefix)) + removeFirstSpaces(message)).trim());
    }

    /**
     * Sends messages to the specified {@link CommandSender} after the given delay.
     * <p>
     * & color codes are translated automatically.
     *
     * @param sender     the {@link CommandSender} to send the messages to.
     * @param delayTicks the delay (in ticks) to wait before sending the message.
     * @param messages   the messages to send.
     */
    public static void tellLater(final int delayTicks, final CommandSender sender, final String... messages) {
        runLater(delayTicks, () -> {
            if (sender instanceof Player && !((Player) sender).isOnline())
                return;

            tell(sender, messages);
        });
    }

    /**
     * Sends messages to the specified {@link CommandSender}.
     * <p>
     * & color codes are translated automatically.
     * <p>
     * This method does not prepend the messages with {@link #getTellPrefix()}.
     *
     * @param sender   the {@link CommandSender} to send the messages to.
     * @param messages the messages to send.
     */
    public static void tellNoPrefix(final CommandSender sender, final Collection<String> messages) {
        tellNoPrefix(sender, toArray(messages));
    }

    /**
     * Sends messages to the specified {@link CommandSender}.
     * <p>
     * & color codes are translated automatically.
     * <p>
     * This method does not prepend the messages with {@link #getTellPrefix()}.
     *
     * @param sender   the {@link CommandSender} to send the messages to.
     * @param messages the messages to send.
     */
    public static void tellNoPrefix(final CommandSender sender, final String... messages) {
        final String oldPrefix = getTellPrefix();

        setTellPrefix("");
        tell(sender, messages);
        setTellPrefix(oldPrefix);
    }

    /**
     * Send messages to the specified {@link CommandSender}.
     * <p>
     * & color codes are translated automatically.
     *
     * @param sender   the {@link CommandSender} to send the messages to.
     * @param messages the messages to send.
     */
    public static void tell(final CommandSender sender, final Collection<String> messages) {
        tell(sender, toArray(messages));
    }

    /**
     * Sends messages to the {@link CommandSender}, ignoring ones that equal "none" or are {@code null} and replacing
     * {player} with the sender's name.
     * <p>
     * & color codes are translated automatically.
     *
     * @param sender   the {@link CommandSender} to send the messages to.
     * @param messages the messages to send.
     */
    public static void tell(final CommandSender sender, final String... messages) {
        for (final String message : messages)
            if (message != null && !"none".equals(message))
                tellJson(sender, message);
    }

    /**
     * Sends a message to the specified {@link CommandSender}, replacing variables in the
     * {@link Replacer#replaceArray(String, Object...)} format.
     *
     * @param recipient    the {@link CommandSender} to send the message to.
     * @param message      the message to send.
     * @param replacements optional variables to replace in the message.
     */
    public static void tellReplaced(final CommandSender recipient, final String message, final Object... replacements) {
        tell(recipient, Replacer.replaceArray(message, replacements));
    }

    /**
     * Sends a basic message to the specified {@link CommandSender}, replacing {player} with the sender's name.
     * <p>
     * & color codes are translated automatically.
     * <p>
     * If the message starts with "[JSON]" then we remove the "[JSON]" prefix and handle the message as a valid JSON
     * component.
     * <p>
     * Finally, a prefix to non-JSON messages is added (see {@link #getTellPrefix()}).
     */
    private static void tellJson(@NonNull final CommandSender sender, String message) {
        if (message.isEmpty() || "none".equals(message))
            return;

        // Does the message have a prefix already? This is replaced when colorizing.
        final boolean hasPrefix = message.contains("{prefix}");
        final boolean hasJSON = message.startsWith("[JSON]");

        // Replace {player} with the sender's name.
        message = message.replace("{player}", resolveSenderName(sender));

        // Replace colors.
        if (!hasJSON)
            message = colorize(message);

        // Used for matching.
        final String colorlessMessage = stripColors(message);

        // Send [JSON]-prefixed messages as JSON components.
        if (hasJSON) {
            final String stripped = message.substring(6).trim();

            if (!stripped.isEmpty())
                Remain.sendJson(sender, stripped);
        } else if (colorlessMessage.startsWith("<actionbar>")) {
            final String stripped = message.replace("<actionbar>", "");

            if (!stripped.isEmpty())
                if (sender instanceof Player)
                    Remain.sendActionBar((Player) sender, stripped);
                else
                    tellJson(sender, stripped);
        } else if (colorlessMessage.startsWith("<toast>")) {
            final String stripped = message.replace("<toast>", "");

            if (!stripped.isEmpty())
                if (sender instanceof Player)
                    Remain.sendToast((Player) sender, stripped);
                else
                    tellJson(sender, stripped);
        } else if (colorlessMessage.startsWith("<title>")) {
            final String stripped = message.replace("<title>", "");

            if (!stripped.isEmpty()) {
                final String[] split = stripped.split("\\|");
                final String title = split[0];
                final String subtitle = split.length > 1 ? joinRange(1, split) : null;

                if (sender instanceof Player)
                    Remain.sendTitle((Player) sender, title, subtitle);
                else {
                    tellJson(sender, title);

                    if (subtitle != null)
                        tellJson(sender, subtitle);
                }
            }
        } else if (colorlessMessage.startsWith("<bossbar>")) {
            final String stripped = message.replace("<bossbar>", "");

            if (!stripped.isEmpty())
                if (sender instanceof Player)
                    // The time cannot be provided here so we just show it for 10 seconds.
                    Remain.sendBossbarTimed((Player) sender, stripped, 10);
                else
                    tellJson(sender, stripped);
        } else
            for (final String part : message.split("\n")) {
                final String prefixStripped = removeSurroundingSpaces(tellPrefix);
                final String prefix = !hasPrefix && !prefixStripped.isEmpty() ? prefixStripped + " " : "";

                String toSend;

                if (stripColors(part).startsWith("<center>"))
                    toSend = ChatUtil.center(prefix + part.replace("<center>", ""));
                else
                    toSend = prefix + part;

                if (MinecraftVersion.olderThan(V.v1_9) && toSend.length() + 1 >= Short.MAX_VALUE) {
                    toSend = toSend.substring(0, Short.MAX_VALUE / 2);

                    warning("Message to " + sender.getName() + " was too large, sending the first 16,000 letters: " + toSend);
                }

                // Make players engaged in a server conversation still receive the message.
                if (sender instanceof Conversable && ((Conversable) sender).isConversing())
                    ((Conversable) sender).sendRawMessage(toSend);
                else
                    try {
                        sender.sendMessage(toSend);
                    } catch (final Throwable t) {
                        Bukkit.getLogger().severe("Failed to send message to " + sender.getName() + ", message: " + toSend);

                        t.printStackTrace();
                    }
            }
    }

    /**
     * Returns the given {@link CommandSender}'s name if it's a {@link Player} or {@link DiscordSender}, or simply
     * {@link SimpleLocalization#CONSOLE_NAME} if it's a console.
     *
     * @param sender the {@link CommandSender} to resolve the name of.
     * @return the name of the given {@link CommandSender}.
     */
    public static String resolveSenderName(final CommandSender sender) {
        return sender instanceof Player || sender instanceof DiscordSender ? sender.getName() : SimpleLocalization.CONSOLE_NAME;
    }

    /**
     * Removes the leading spaces from the given message.
     *
     * @param message the message to remove the leading spaces from.
     * @return the message without the leading spaces.
     */
    private static String removeFirstSpaces(String message) {
        message = getOrEmpty(message);

        while (message.startsWith(" "))
            message = message.substring(1);

        return message;
    }

    /**
     * Adds a space at the end of the given message if it doesn't already end with a space.
     *
     * @param message the message to add a space to.
     * @return the message with a space at the end.
     */
    private static String addLastSpace(String message) {
        message = message.trim();

        if (!message.endsWith(" "))
            message += " ";

        return message;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Colorizing messages
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Replaces & color codes in the given {@link String} {@link List}.
     *
     * @param list the {@link String} {@link List} to replace & color codes in.
     * @return a new list containing the colorized strings.
     */
    public static List<String> colorize(@NonNull final List<@NonNull String> list) {
        final List<String> copy = new ArrayList<>(list);

        for (int i = 0; i < copy.size(); i++) {
            final String message = copy.get(i);

            if (message != null)
                copy.set(i, colorize(message));
        }

        return copy;
    }

    /**
     * Replaces & color codes in the given messages.
     *
     * @param messages the messages to replace & color codes in.
     * @return the colored message, joined by {@code \n}.
     */
    public static String colorize(final String... messages) {
        return colorize(String.join("\n", messages));
    }

    /**
     * Replaces & color codes in the given messages.
     *
     * @param messages the messages to replace & color codes in.
     * @return the colored message as an array.
     */
    public static String[] colorizeArray(@NonNull final String @NonNull ... messages) {
        for (int i = 0; i < messages.length; i++)
            messages[i] = colorize(messages[i]);

        return messages;
    }

    /**
     * Replaces & color codes in the given message.
     * <p>
     * This method also replaces {prefix} with {@link #getTellPrefix()} and {server} with
     * {@link SimpleLocalization#SERVER_PREFIX}.
     *
     * @param message the message to replace & color codes in.
     * @return the colored message.
     */
    public static String colorize(@Nullable final String message) {
        if (message == null || message.isEmpty())
            return "";

        final char[] letters = message.toCharArray();

        for (int index = 0; index < letters.length - 1; index++)
            if (letters[index] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(letters[index + 1]) > -1) {
                letters[index] = ChatColor.COLOR_CHAR;

                letters[index + 1] = Character.toLowerCase(letters[index + 1]);
            }

        String result = new String(letters)
                .replace("{prefix}", message.startsWith(tellPrefix) ? "" : removeSurroundingSpaces(tellPrefix.trim()))
                .replace("{server}", SimpleLocalization.SERVER_PREFIX)
                .replace("{plugin_name}", SimplePlugin.getNamed())
                .replace("{plugin_version}", SimplePlugin.getVersion());

        // RGB colors - return the closest color for legacy Minecraft versions.
        final Matcher match = HEX_COLOR_REGEX.matcher(result);

        while (match.find()) {
            final String matched = match.group();
            final String colorCode = match.group(2);
            String replacement = "";

            try {
                replacement = CompChatColor.of("#" + colorCode).toString();
            } catch (final IllegalArgumentException ex) {
            }

            result = result.replaceAll(Pattern.quote(matched), replacement);
        }

        if (result.contains("\\\\#"))
            result = result.replace("\\\\#", "\\#");
        else if (result.contains("\\#"))
            result = result.replace("\\#", "#");

        return result;
    }

    /**
     * Removes the first and last spaces from the given message.
     *
     * @param message the message to remove surrounding spaces from.
     * @return the modified message without the surrounding spaces.
     */
    private static String removeSurroundingSpaces(String message) {
        message = getOrEmpty(message);

        while (message.endsWith(" "))
            message = message.substring(0, message.length() - 1);

        return removeFirstSpaces(message);
    }

    /**
     * Replaces {@link ChatColor#COLOR_CHAR} colors with & letters in the given messages.
     *
     * @param messages the messages to replace {@link ChatColor#COLOR_CHAR} letters in.
     * @return the reverted message.
     */
    public static String[] revertColorizing(final String[] messages) {
        for (int i = 0; i < messages.length; i++)
            messages[i] = revertColorizing(messages[i]);

        return messages;
    }

    /**
     * Replaces {@link ChatColor#COLOR_CHAR} colors with & letters in the given message.
     *
     * @param message the message to replace {@link ChatColor#COLOR_CHAR} letters in.
     * @return the reverted message.
     */
    public static String revertColorizing(final String message) {
        return message.replaceAll("(?i)" + ChatColor.COLOR_CHAR + "([0-9a-fk-or])", "&$1");
    }

    /**
     * Removes all {@link ChatColor#COLOR_CHAR} colors as well as & letter colors from the given message.
     *
     * @param message the message to remove colors from.
     * @return the modified message with color codes stripped.
     */
    public static String stripColors(String message) {
        if (message == null || message.isEmpty())
            return message;

        // Replace & color codes.
        final Matcher matcher = ALL_IN_ONE.matcher(message);

        while (matcher.find())
            message = matcher.replaceAll("");

        // Replace hex colors, both raw and parsed.
		/*if (Remain.hasHexColors()) {
			matcher = HEX_COLOR_REGEX.matcher(message);

			while (matcher.find())
				message = matcher.replaceAll("");

			matcher = RGB_X_COLOR_REGEX.matcher(message);

			while (matcher.find())
				message = matcher.replaceAll("");

			message = message.replace(ChatColor.COLOR_CHAR + "x", "");
		}*/

        return message;
    }

    /**
     * Removes & color codes from the given message.
     *
     * @param message the message to remove & color codes from.
     * @return the modified message, with & color codes removed.
     */
    public static String stripColorsLetter(@Nullable final String message) {
        return message == null ? "" : message.replaceAll("&([0-9a-fk-orA-F-K-OR])", "");
    }

    /**
     * Checks if the given message contains either {@link ChatColor#COLOR_CHAR} or & letter colors.
     *
     * @param message the message to check.
     * @return {@code true} if the given message contains either {@link ChatColor#COLOR_CHAR} or & letter colors.
     */
    public static boolean hasColors(@NonNull final String message) {
        return COLOR_AND_DECORATION_REGEX.matcher(message).find();
    }

    /**
     * Returns the last color (either & or {@link ChatColor#COLOR_CHAR} from the given message.
     *
     * @param message the message to check.
     * @return the last color in the given message, or an empty {@link String} if it doesn't exist.
     */
    public static String lastColor(final String message) {
        // RGB colors
        if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_16)) {
            final int c = message.lastIndexOf(ChatColor.COLOR_CHAR);
            final Matcher match = RGB_X_COLOR_REGEX.matcher(message);

            String lastColor = null;

            while (match.find())
                lastColor = match.group(0);

            if (lastColor != null)
                if (c == -1 || c < message.lastIndexOf(lastColor) + lastColor.length())
                    return lastColor;
        }

        final String andLetter = lastColorLetter(message);
        final String colorCharacter = lastColorChar(message);

        return !andLetter.isEmpty() ? andLetter : !colorCharacter.isEmpty() ? colorCharacter : "";
    }

    /**
     * Returns the last color from the given message in the following format: & + the color letter.
     *
     * @param message the message to check.
     * @return the last color from the given message, or an empty {@link String} if it doesn't exist.
     */
    public static String lastColorLetter(final String message) {
        return lastColor(message, '&');
    }

    /**
     * Returns the last color from the given message in the format: {@link ChatColor#COLOR_CHAR} + the color letter.
     *
     * @param message the message to check.
     * @return the last color from the given message, or an empty {@link String} if it doesn't exist.
     */
    public static String lastColorChar(final String message) {
        return lastColor(message, ChatColor.COLOR_CHAR);
    }

    private static String lastColor(final String msg, final char colorChar) {
        final int c = msg.lastIndexOf(colorChar);

        // Contains our character.
        if (c != -1) {
            // Contains a character after the color character.
            if (msg.length() > c + 1)
                if (msg.substring(c + 1, c + 2).matches("([0-9a-fk-or])"))
                    return msg.substring(c, c + 2).trim();

            // Search after colors before that invalid character.
            return lastColor(msg.substring(0, c), colorChar);
        }

        return "";
    }

    // ------------------------------------------------------------------------------------------------------------
    // Aesthetics
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Returns a long !------! console line.
     *
     * @return a long !------! console line.
     */
    public static String consoleLine() {
        return "!-----------------------------------------------------!";
    }

    /**
     * Returns a long ______ console line.
     *
     * @return a long ______ console line.
     */
    public static String consoleLineSmooth() {
        return "______________________________________________________________";
    }

    /**
     * Returns a long *--------* chat line.
     *
     * @return a long *--------* chat line.
     */
    public static String chatLine() {
        return "*---------------------------------------------------*";
    }

    /**
     * Returns a long ----------- chat line with a strike effect.
     *
     * @return a long ----------- chat line with a strike effect.
     */
    public static String chatLineSmooth() {
        return "&m-----------------------------------------------------";
    }

    /**
     * Returns a very long -------- configuration line.
     *
     * @return a very long -------- configuration line.
     */
    public static String configLine() {
        return "-------------------------------------------------------------------------------------------";
    }

    /**
     * Returns a |------------| scoreboard line with the given amount of dashes.
     *
     * @param length the amount of dashes to add.
     * @return a |------------| scoreboard line with the given amount of dashes.
     */
    public static String scoreboardLine(final int length) {
        final StringBuilder fill = new StringBuilder();

        for (int i = 0; i < length; i++)
            fill.append("-");

        return "&m|" + fill + "|";
    }

    /**
     * Prints the count with what the given {@link Collection} contains. For example: "X bosses: Creeper, Zombie".
     *
     * @param iterable the {@link Collection} to print.
     * @param ofWhat   the name of the value.
     * @param <T>      the type of elements in the collection.
     * @return the count with what the given {@link Collection} contains.
     */
    public static <T> String plural(final Collection<T> iterable, final String ofWhat) {
        return plural(iterable.size(), ofWhat) + ": " + join(iterable);
    }

    /**
     * Adds "s" to the given {@link String} if the count is 0 or over 1.
     *
     * @param count  the amount.
     * @param ofWhat the name of the value.
     * @return the count and plural representation of the value if the count is 0 or over 1.
     */
    public static String plural(final long count, final String ofWhat) {
        final String exception = getException(count, ofWhat);

        return exception != null ? exception : count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("s") ? "s" : "");
    }

    /**
     * Adds "es" to the given {@link String} if the count is 0 or over 1.
     *
     * @param count  the amount.
     * @param ofWhat the name of the value.
     * @return the count and plural representation of the value if the count is 0 or over 1.
     */
    public static String pluralEs(final long count, final String ofWhat) {
        final String exception = getException(count, ofWhat);

        return exception != null ? exception : count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("es") ? "es" : "");
    }

    /**
     * Adds "ies" to the given {@link String} if the count is 0 or over 1.
     *
     * @param count  the amount.
     * @param ofWhat the name of the value.
     * @return the count and plural representation of the value if the count is 0 or over 1.
     */
    public static String pluralIes(final long count, final String ofWhat) {
        final String exception = getException(count, ofWhat);

        return exception != null ? exception : count + " " + (count == 0 || count > 1 && !ofWhat.endsWith("ies") ? ofWhat.substring(0, ofWhat.length() - 1) + "ies" : ofWhat);
    }

    /**
     * Returns the plural word from the exception list, or {@code null} if there is none.
     *
     * @param count  the amount.
     * @param ofWhat the name of the value.
     * @return the plural word from the exception list, or {@code null} if there is none.
     * @deprecated contains a very limited list of the most commonly used English plural irregularities.
     */
    @Deprecated
    @Nullable
    private static String getException(final long count, final String ofWhat) {
        final SerializedMap exceptions = SerializedMap.ofArray(
                "life", "lives",
                "class", "classes",
                "wolf", "wolves",
                "knife", "knives",
                "wife", "wives",
                "calf", "calves",
                "leaf", "leaves",
                "potato", "potatoes",
                "tomato", "tomatoes",
                "hero", "heroes",
                "torpedo", "torpedoes",
                "veto", "vetoes",
                "foot", "feet",
                "tooth", "teeth",
                "goose", "geese",
                "man", "men",
                "woman", "women",
                "mouse", "mice",
                "die", "dice",
                "ox", "oxen",
                "child", "children",
                "person", "people",
                "penny", "pence",
                "sheep", "sheep",
                "fish", "fish",
                "deer", "deer",
                "moose", "moose",
                "swine", "swine",
                "buffalo", "buffalo",
                "shrimp", "shrimp",
                "trout", "trout",
                "spacecraft", "spacecraft",
                "cactus", "cacti",
                "axis", "axes",
                "analysis", "analyses",
                "crisis", "crises",
                "thesis", "theses",
                "datum", "data",
                "index", "indices",
                "entry", "entries",
                "boss", "bosses",
                "iron", "iron",
                "Iron", "Iron",
                "gold", "gold",
                "Gold", "Gold");

        return exceptions.containsKey(ofWhat) ? count + " " + (count == 0 || count > 1 ? exceptions.getString(ofWhat) : ofWhat) : null;
    }

    /**
     * Prepends the given {@link String} with either "a" or "an" (does a dummy syllable check).
     *
     * @param ofWhat the {@link String} to prepend the syllable to.
     * @return the modified {@link String} prepended with either "a" or "an".
     * @deprecated only a dummy syllable check, e.g. returns a hour.
     */
    @Deprecated
    @NonNull
    public static String article(final String ofWhat) {
        Valid.checkBoolean(!ofWhat.isEmpty(), "String cannot be empty");
        final List<String> syllables = Arrays.asList("a", "e", "i", "o", "u", "y");

        return (syllables.contains(ofWhat.toLowerCase().trim().substring(0, 1)) ? "an" : "a") + " " + ofWhat;
    }

    /**
     * Generates a bar indicating progress. Example:
     * <p>
     * ##-----
     * <p>
     * ###----
     * <p>
     * ####---
     *
     * @param min            the minimum progress.
     * @param minChar        the character to show for the minimum progress.
     * @param max            the maximum progress.
     * @param maxChar        the character to show for the maximum progress.
     * @param delimiterColor the delimiter {@link ChatColor color}.
     * @return the generated bar.
     */
    public static String fancyBar(final int min, final char minChar, final int max, final char maxChar, final ChatColor delimiterColor) {
        final StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < min; i++)
            formatted.append(minChar);

        formatted.append(delimiterColor);

        for (int i = 0; i < max - min; i++)
            formatted.append(maxChar);

        return formatted.toString();
    }

    /**
     * Formats the given {@link Vector} location to one digit decimal points.
     * <p>
     * <b>Do not use this for saving, it's only intended for debugging!</b>
     *
     * @param vector the {@link Vector} to format.
     * @return the formatted {@link Vector} as a {@link String}.
     */
    public static String shortLocation(@NonNull final Vector vector) {
        return " [" + MathUtil.formatOneDigit(vector.getX()) + ", " + MathUtil.formatOneDigit(vector.getY()) + ", " + MathUtil.formatOneDigit(vector.getZ()) + "]";
    }

    /**
     * Formats the given {@link ItemStack} into a readable and useful console log, printing only its name, lore and NBT
     * tags.
     * <p>
     * <b>Do not use this for saving, it's only intended for debugging!</b>
     *
     * @param item the {@link ItemStack} to format.
     * @return the formatted {@link ItemStack} as a {@link String}.
     */
    public static String shortItemStack(@Nullable final ItemStack item) {
        if (item == null)
            return "null";

        if (CompMaterial.isAir(item.getType()))
            return "Air";

        String name = ItemUtil.bountifyCapitalized(item.getType());

        if (Remain.hasItemMeta() && item.hasItemMeta()) {
            final ItemMeta meta = item.getItemMeta();

            name += "{";

            if (meta.hasDisplayName())
                name += "name='" + stripColors(meta.getDisplayName()) + "', ";

            if (meta.hasLore())
                name += "lore=[" + stripColors(String.join(", ", meta.getLore())) + "], ";

            final NBTItem nbt = new NBTItem(item);

            if (nbt.hasTag(FoConstants.NBT.TAG))
                name += "tags=" + nbt.getCompound(FoConstants.NBT.TAG) + ", ";

            if (name.endsWith(", "))
                name = name.substring(0, name.length() - 2);

            name += "}";
        }

        return name;
    }

    /**
     * Formats the given {@link Location} to block points without decimals.
     * <p>
     * This method uses the format found at {@link SimpleSettings#LOCATION_FORMAT}.
     *
     * @param loc the {@link Location} to format.
     * @return the formatted {@link Location} as a {@link String}.
     */
    public static String shortLocation(final Location loc) {
        if (loc == null)
            return "Location(null)";

        if (loc.equals(new Location(null, 0, 0, 0)))
            return "Location(null, 0, 0, 0)";

        Valid.checkNotNull(loc.getWorld(), "Cannot shorten a location with null world!");

        return Replacer.replaceArray(SimpleSettings.LOCATION_FORMAT,
                "world", loc.getWorld().getName(),
                "x", loc.getBlockX(),
                "y", loc.getBlockY(),
                "z", loc.getBlockZ());
    }

    /**
     * Duplicating the given text the given amount of times.
     * <p>
     * Example: {@code duplicate("apple", 2)} will produce "appleapple".
     *
     * @param text          the text to duplicate.
     * @param numberOfTimes the number of times to duplicate the text.
     * @return the duplicated text.
     */
    public static String duplicate(String text, final int numberOfTimes) {
        if (numberOfTimes == 0)
            return "";

        final String toDuplicate = text;
        final StringBuilder textBuilder = new StringBuilder(text);

        for (int i = 1; i < numberOfTimes; i++)
            textBuilder.append(toDuplicate);

        text = textBuilder.toString();
        return text;
    }

    /**
     * Limits the {@link String} to the given length, appending "..." at the end when it is cut.
     *
     * @param text      the text to limit.
     * @param maxLength the maximum length of the text.
     * @return the modified {@link String}, with "..." appended if it exceeds the given length.
     */
    public static String limit(final String text, final int maxLength) {
        final int length = text.length();

        return maxLength >= length ? text : text.substring(0, maxLength) + "...";
    }

    // ------------------------------------------------------------------------------------------------------------
    // Plugin management
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Checks if a plugin is enabled. We also schedule an async task to make sure the plugin is loaded correctly when
     * the server is done booting.
     *
     * @param pluginName the name of the plugin to check.
     * @return {@code true} if the given plugin is enabled (this doesn't mean it works correctly), {@code false}
     * otherwise.
     */
    public static boolean doesPluginExist(final String pluginName) {
        Plugin lookup = null;

        for (final Plugin otherPlugin : Bukkit.getPluginManager().getPlugins())
            if (otherPlugin.getDescription().getName().equals(pluginName)) {
                lookup = otherPlugin;

                break;
            }

        final Plugin found = lookup;

        if (found == null)
            return false;

        if (!found.isEnabled())
            runLaterAsync(0, () -> Valid.checkBoolean(found.isEnabled(), SimplePlugin.getNamed() + " could not hook into " + pluginName + " as the plugin is disabled! (DO NOT REPORT THIS TO " + SimplePlugin.getNamed() + ", look for errors above and contact support of '" + pluginName + "')"));

        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Running commands
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Runs the given command (without /) as the console, replacing {player} with the given {@link CommandSender}'s
     * name.
     * <p>
     * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted message to the
     * sender directly.
     *
     * @param playerReplacement the {@link CommandSender} to use when replacing {player}.
     * @param command           the command to perform.
     */
    public static void dispatchCommand(@Nullable final CommandSender playerReplacement, @NonNull String command) {
        if (command.isEmpty() || "none".equalsIgnoreCase(command))
            return;

        if (command.startsWith("@announce "))
            Messenger.announce(playerReplacement, command.replace("@announce ", ""));
        else if (command.startsWith("@warn "))
            Messenger.warn(playerReplacement, command.replace("@warn ", ""));
        else if (command.startsWith("@error "))
            Messenger.error(playerReplacement, command.replace("@error ", ""));
        else if (command.startsWith("@info "))
            Messenger.info(playerReplacement, command.replace("@info ", ""));
        else if (command.startsWith("@question "))
            Messenger.question(playerReplacement, command.replace("@question ", ""));
        else if (command.startsWith("@success "))
            Messenger.success(playerReplacement, command.replace("@success ", ""));
        else {
            command = command.startsWith("/") && !command.startsWith("//") ? command.substring(1) : command;
            command = command.replace("{player}", playerReplacement == null ? "" : resolveSenderName(playerReplacement));

            // Workaround for JSON in /tellraw getting HEX colors replaced.
            if (!command.startsWith("tellraw"))
                command = colorize(command);

            final String finalCommand = command;

            runLater(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }

    /**
     * Runs the given command (without /) as the given {@link Player}, replacing {player} with their name.
     *
     * @param playerSender the {@link Player} to perform the command as and replace {player} with.
     * @param command      the command to perform.
     */
    public static void dispatchCommandAsPlayer(@NonNull final Player playerSender, @NonNull String command) {
        if (command.isEmpty() || "none".equalsIgnoreCase(command))
            return;

        // Remove trailing /.
        if (command.startsWith("/") && !command.startsWith("//"))
            command = command.substring(1);

        final String finalCommand = command;

        runLater(() -> playerSender.performCommand(colorize(finalCommand.replace("{player}", resolveSenderName(playerSender)))));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Logging and error handling
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Logs a message to the console and saves the time it was sent. The delay in seconds is the delay between which we
     * won't log the same message, in case you call this method again. within the delay in seconds, it will not be
     * logged.
     *
     * @param delaySeconds the delay (in seconds) to wait before logging the message again.
     * @param message      the message to log.
     */
    public static void logTimed(final int delaySeconds, final String message) {
        // No message stored, just log it now.
        if (!TIMED_LOG_CACHE.containsKey(message)) {
            log(message);

            TIMED_LOG_CACHE.put(message, TimeUtil.currentTimeSeconds());
            return;
        }

        if (TimeUtil.currentTimeSeconds() - TIMED_LOG_CACHE.get(message) > delaySeconds) {
            log(message);

            TIMED_LOG_CACHE.put(message, TimeUtil.currentTimeSeconds());
        }
    }

    /**
     * Works similarly to {@link String#format(String, Object...)} however all arguments are explored, so player names
     * are properly given, locations are shortened, etc.
     *
     * @param format the {@link String} to format.
     * @param args   the arguments to replace.
     */
    public static void logF(final String format, @NonNull final Object... args) {
        final String formatted = format(format, args);

        log(false, formatted);
    }

    /**
     * Replaces something like CraftPlayer{name=noob} into a proper player name. Works fine with entities, worlds, and
     * locations.
     * <p>
     * Example use: {@code format("Hello %s from world %s", player, player.getWorld())}.
     *
     * @param format the {@link String} to format.
     * @param args   the arguments to replace.
     * @return the formatted {@link String}.
     */
    public static String format(final String format, @NonNull final Object... args) {
        for (int i = 0; i < args.length; i++) {
            final Object arg = args[i];
            args[i] = simplify(arg);
        }

        return String.format(format, args);
    }

    /**
     * A dummy helper method that adds "&cWarning: &f" to the given message and logs it.
     *
     * @param message the message to log.
     */
    public static void warning(final String message) {
        log("&cWarning: &7" + message);
    }

    /**
     * Logs the given messages to the console.
     * <p>
     * & color codes are translated automatically.
     *
     * @param messages the messages to log.
     */
    public static void log(final List<String> messages) {
        log(toArray(messages));
    }

    /**
     * Logs the given messages to the console.
     * <p>
     * & color codes are translated automatically.
     *
     * @param messages the messages to log.
     */
    public static void log(final String... messages) {
        log(true, messages);
    }

    /**
     * Logs the given messages to the console, & colors are supported
     * <p>
     * This method does not prepend {@link #getLogPrefix()} before the messages.
     *
     * @param messages the messages to log.
     */
    public static void logNoPrefix(final String... messages) {
        log(false, messages);
    }

    /**
     * Logs the given messages to the console.
     * <p>
     * & color codes are translated automatically.
     *
     * @param addLogPrefix should {@link #getLogPrefix()} be prepended before the message?
     * @param messages     the messages to log.
     */
    private static void log(final boolean addLogPrefix, final String... messages) {
        if (messages == null)
            return;

        final CommandSender console = Bukkit.getConsoleSender();

        if (console == null)
            throw new FoException("Failed to initialize Console Sender, are you running Foundation under a Bukkit/Spigot server?");

        for (String message : messages) {
            if (message == null || "none".equals(message))
                continue;

            if (stripColors(message).replace(" ", "").isEmpty()) {
                console.sendMessage("  ");

                continue;
            }

            message = colorize(message);

            if (message.startsWith("[JSON]")) {
                final String stripped = message.replaceFirst("\\[JSON]", "").trim();

                if (!stripped.isEmpty())
                    log(Remain.toLegacyText(stripped, false));
            } else
                for (final String part : message.split("\n")) {
                    final String log = ((addLogPrefix && !logPrefix.isEmpty() ? removeSurroundingSpaces(logPrefix) + " " : "") + getOrEmpty(part).replace("\n", colorize("\n&r"))).trim();

                    console.sendMessage(log);
                }
        }
    }

    /**
     * Logs the given messages to the console in a {@link #consoleLine()} frame.
     *
     * @param messages the messages to log.
     */
    public static void logFramed(final String... messages) {
        logFramed(false, messages);
    }

    /**
     * Logs the given messages to the console in a {@link #consoleLine()} frame.
     * <p>
     * Used when an error occurs, can also disable the plugin.
     *
     * @param disablePlugin should the plugin be disabled?
     * @param messages      the messages to log.
     */
    public static void logFramed(final boolean disablePlugin, final String... messages) {
        if (messages != null && !Valid.isNullOrEmpty(messages)) {
            log("&7" + consoleLine());
            for (final String msg : messages)
                log(" &c" + msg);

            if (disablePlugin)
                log(" &cThe plugin is now disabled.");

            log("&7" + consoleLine());
        }

        if (disablePlugin)
            Bukkit.getPluginManager().disablePlugin(SimplePlugin.getInstance());
    }

    /**
     * Saves the error, prints the stack trace and logs it in a frame.
     * <p>
     * You can use %error and %error% to get the error.
     *
     * @param throwable the {@link Throwable} to save, print and log.
     * @param messages  the messages to include with the error.
     */
    public static void error(@NonNull Throwable throwable, final String... messages) {
        if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
            throwable = throwable.getCause();

        if (!(throwable instanceof FoException))
            Debugger.saveError(throwable, messages);

        Debugger.printStackTrace(throwable);
        logFramed(replaceErrorVariable(throwable, messages));
    }

    /**
     * Logs the given messages in a frame (if they are not null), saves the error to error.log and then throws it.
     * <p>
     * You can use %error and %error% to get the error.
     *
     * @param throwable the {@link Throwable} to save and throw.
     * @param messages  the messages to include with the error.
     */
    public static void throwError(final Throwable throwable, final String... messages) {
        // Delegate to only print out the relevant stuff.
        if (throwable instanceof FoException)
            throw (FoException) throwable;

        if (messages != null)
            logFramed(false, replaceErrorVariable(throwable, messages));

        Debugger.saveError(throwable, messages);
        Remain.sneaky(throwable);
    }

    /**
     * Replaces %error and %error% with smart error information.
     *
     * @param throwable the {@link Throwable} containing the error information.
     * @param messages  the messages to replace %error and %error in.
     * @return the updated array of messages with %error and %error% replaced.
     */
    private static String[] replaceErrorVariable(Throwable throwable, final String... messages) {
        while (throwable.getCause() != null)
            throwable = throwable.getCause();

        final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
        final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

        for (int i = 0; i < messages.length; i++) {
            final String error = throwableName + throwableMessage;

            messages[i] = messages[i]
                    .replace("%error%", error)
                    .replace("%error", error);
        }

        return messages;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Regular expressions
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Matches a string against a regular expression.
     *
     * @param regex   the regular expression to match.
     * @param message the message to match against the regular expression.
     * @return {@code true} if the message matches the regular expression, {@code false} otherwise.
     */
    public static boolean regExMatch(final String regex, final String message) {
        return regExMatch(compilePattern(regex), message);
    }

    /**
     * Matches a string against a regular expression {@link Pattern}.
     *
     * @param regex   the regular expression {@link Pattern} to match.
     * @param message the message to match against the regular expression pattern.
     * @return {@code true} if the message matches the regular expression {@link Pattern}, {@code false} otherwise.
     */
    public static boolean regExMatch(final Pattern regex, final String message) {
        return regExMatch(compileMatcher(regex, message));
    }

    /**
     * Matches a string against a regular expression {@link Matcher}. We also evaluate how long the evaluation took and
     * stop it in case it takes too long (see {@link SimplePlugin#getRegexTimeout()}).
     *
     * @param matcher the regular expression {@link Matcher} to match.
     * @return {@code true} if the message matches the regular expression {@link Matcher}, {@code false} otherwise.
     */
    public static boolean regExMatch(final Matcher matcher) {
        Valid.checkNotNull(matcher, "Cannot call regExMatch on null matcher");

        try {
            return matcher.find();
        } catch (final RegexTimeoutException ex) {
            handleRegexTimeoutException(ex, matcher.pattern());

            return false;
        }
    }

    /**
     * Compiles a regular expression {@link Pattern} and returns a corresponding {@link Matcher} using the given pattern
     * and message. Colors and diacritic characters are stripped (see {@link SimplePlugin#regexStripColors()} and
     * {@link SimplePlugin#regexStripAccents()} to change this behavior).
     *
     * @param pattern the regular expression {@link Pattern} to compile.
     * @param message the message to match against the regular expression.
     * @return a {@link Matcher} that matches the given message against the compiled regular expression {@link Pattern},
     * or {@code null} if a regex timeout exception occurs.
     */
    public static Matcher compileMatcher(@NonNull final Pattern pattern, final String message) {
        try {
            final SimplePlugin instance = SimplePlugin.getInstance();

            String strippedMessage = instance.regexStripColors() ? stripColors(message) : message;
            strippedMessage = instance.regexStripAccents() ? ChatUtil.replaceDiacritic(strippedMessage) : strippedMessage;

            return pattern.matcher(TimedCharSequence.withSettingsLimit(strippedMessage));
        } catch (final RegexTimeoutException ex) {
            handleRegexTimeoutException(ex, pattern);

            return null;
        }
    }

    /**
     * Compiles a regular expression {@link Pattern} and returns a corresponding {@link Matcher} using the given regular
     * expression {@link String} and message.
     *
     * @param regex   the regular expression {@link String} to compile.
     * @param message the message to match against the compiled regular expression.
     * @return a {@link Matcher} that matches the given message against the compiled regular expression {@link Pattern}.
     */
    public static Matcher compileMatcher(final String regex, final String message) {
        final Pattern pattern = compilePattern(regex);

        if (pattern == null)
            throw new FoException("Invalid regular expression: " + regex);

        return compileMatcher(pattern, message);
    }

    /**
     * Compiles a regular expression {@link Pattern}.
     *
     * @param regex the regular expression {@link String} to compile.
     * @return a compiled {@link Pattern}.
     */
    public static Pattern compilePattern(String regex) {
        final SimplePlugin instance = SimplePlugin.getInstance();
        final Pattern pattern;

        regex = instance.regexStripColors() ? stripColors(regex) : regex;
        regex = instance.regexStripAccents() ? ChatUtil.replaceDiacritic(regex) : regex;

        try {
            if (instance.regexCaseInsensitive())
                pattern = Pattern.compile(regex, instance.regexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);
            else
                pattern = instance.regexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);
        } catch (final PatternSyntaxException ex) {
            throwError(ex,
                    "Your regular expression is malformed!",
                    "Expression: '" + regex + "'",
                    "",
                    "If you created it yourself, we unfortunately",
                    "can't provide support for custom expressions.",
                    "Use online services like regex101.com to put your",
                    "expression there (without '') and discover where",
                    "the syntax error lies and how to fix it.");

            return null;
        }

        return pattern;
    }

    /**
     * Handles a {@link RegexTimeoutException} and logs an error message.
     * <p>
     * Do not use this method.
     *
     * @param ex      the {@link RegexTimeoutException} that occurred.
     * @param pattern the {@link Pattern} that caused the timeout exception. Can be {@code null} if it's unknown.
     */
    public static void handleRegexTimeoutException(final RegexTimeoutException ex, final Pattern pattern) {
        final boolean caseInsensitive = SimplePlugin.getInstance().regexCaseInsensitive();

        error(ex,
                "A regular expression took too long to process, and was",
                "stopped to prevent it from freezing your server.",
                " ",
                "Limit " + SimpleSettings.REGEX_TIMEOUT + "ms",
                "Expression: '" + (pattern == null ? "Unknown" : pattern.pattern()) + "'",
                "Evaluated message: '" + ex.getCheckedMessage() + "'",
                " ",
                "If you created that rule yourself, we unfortunately",
                "can't provide support for custom expressions.",
                " ",
                "Sometimes, all you need to do is increase the timeout",
                "limit in your settings.yml.",
                " ",
                "Use services like regex101.com to test and fix it.",
                "Put the expression without '' and the message there.",
                "Ensure you turn flags 'insensitive' and 'unicode' " + (caseInsensitive ? "on" : "off"),
                "on there when testing: https://i.imgur.com/PRR5Rfn.png.");
    }

    // ------------------------------------------------------------------------------------------------------------
    // Joining strings and lists
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Joins multiple arrays into one array.
     *
     * @param arrays the arrays to be joined.
     * @param <T>    the type of the elements in the arrays.
     * @return an array containing all the elements from the input arrays.
     */
    @SafeVarargs
    public static <T> Object[] joinArrays(final T[]... arrays) {
        final List<T> all = new ArrayList<>();

        for (final T[] array : arrays)
            Collections.addAll(all, array);

        return all.toArray();
    }

    /**
     * Joins multiple {@link Iterable lists} into one {@link List}.
     *
     * @param lists the {@link Iterable lists} to be joined.
     * @param <T>   the type of the elements in the lists.
     * @return a {@link List} containing all the elements from the input {@link Iterable lists}.
     */
    @SafeVarargs
    public static <T> List<T> joinLists(final Iterable<T>... lists) {
        final List<T> all = new ArrayList<>();

        for (final Iterable<T> array : lists)
            for (final T element : array)
                all.add(element);

        return all;
    }

    /**
     * Joins player names from the input {@link Iterable array} excluding a specific player name.
     *
     * @param array        the {@link Iterable array} containing the players.
     * @param nameToIgnore the name of the player to be ignored.
     * @param <T>          the type of the elements in the array (must extend {@link CommandSender}).
     * @return a {@link String} containing the names of the players from the input {@link Iterable array}, excluding the
     * specified player name.
     */
    public static <T extends CommandSender> String joinPlayersExcept(final Iterable<T> array, final String nameToIgnore) {
        final Iterator<T> iterator = array.iterator();
        final StringBuilder message = new StringBuilder();

        while (iterator.hasNext()) {
            final T next = iterator.next();

            if (!next.getName().equals(nameToIgnore))
                message.append(next.getName()).append(iterator.hasNext() ? ", " : "");
        }

        return message.toString().endsWith(", ") ? message.substring(0, message.length() - 2) : message.toString();
    }

    /**
     * Joins elements from the input {@link String} array using spaces within a specific range.
     *
     * @param startIndex the index to start joining from.
     * @param array      the input {@link String} array.
     * @return a {@link String} containing the joined elements within the specified range.
     */
    public static String joinRange(final int startIndex, final String[] array) {
        return joinRange(startIndex, array.length, array);
    }

    /**
     * Joins elements from the input {@link String} array using spaces within a specific range.
     *
     * @param startIndex the index to start joining from.
     * @param stopIndex  the index to join until.
     * @param array      the input {@link String} array.
     * @return a {@link String} containing the joined elements within the specified range.
     */
    public static String joinRange(final int startIndex, final int stopIndex, final String[] array) {
        return joinRange(startIndex, stopIndex, array, " ");
    }

    /**
     * Joins elements from the input {@link String} array using a specified delimiter within a specific range.
     *
     * @param startIndex the index to start joining from.
     * @param stopIndex  the index to join until.
     * @param array      the input {@link String} array.
     * @param delimiter  the delimiter used to separate the joined elements.
     * @return a {@link String} containing the joined elements within the specified range, separated by the given
     * delimiter.
     */
    public static String joinRange(final int startIndex, final int stopIndex, final String[] array, final String delimiter) {
        final StringBuilder joined = new StringBuilder();

        for (int i = startIndex; i < MathUtil.range(stopIndex, 0, array.length); i++)
            joined.append((joined.length() == 0) ? "" : delimiter).append(array[i]);

        return joined.toString();
    }

    /**
     * Joins elements from the input array, separated by ", ". We invoke {@link T#toString()} for each element given it
     * is not {@code null}, or return an empty {@link String} if it is.
     *
     * @param array the input array.
     * @return a {@link String} containing the joined elements of the array.
     */
    public static <T> String join(final T[] array) {
        return array == null ? "null" : join(Arrays.asList(array));
    }

    /**
     * Joins elements from the input {@link Iterable array}, separated by ", ". We invoke {@link T#toString()} for each
     * element given it is not {@code null}, or return an empty {@link String} if it is.
     *
     * @param <T>   the type of the elements in the array.
     * @param array the input {@link Iterable array}.
     * @return a {@link String} containing the joined elements of the iterable.
     */
    public static <T> String join(final Iterable<T> array) {
        return array == null ? "null" : join(array, ", ");
    }

    /**
     * Joins elements from the input array, separated by the specified delimiter. We invoke {@link T#toString()} for
     * each element given it is not null, or return an empty {@link String} if it is.
     *
     * @param <T>       the type of the elements in the array.
     * @param array     the input array.
     * @param delimiter the delimiter used to separate the joined elements.
     * @return a {@link String} containing the joined elements of the array with the specified delimiter.
     */
    public static <T> String join(final T[] array, final String delimiter) {
        return join(array, delimiter, object -> object == null ? "" : simplify(object));
    }

    /**
     * Joins elements from the input {@link Iterable array}, separated by the specified delimiter. We invoke
     * {@link T#toString()} for each element given it is not null, or return an empty {@link String} if it is.
     *
     * @param <T>       the type of the elements in the array.
     * @param array     the input {@link Iterable array}.
     * @param delimiter the delimiter used to separate the joined elements.
     * @return a {@link String} containing the joined elements of the {@link Iterable array} with the specified
     * delimiter.
     */
    public static <T> String join(final Iterable<T> array, final String delimiter) {
        return join(array, delimiter, object -> object == null ? "" : simplify(object));
    }

    /**
     * Joins elements from the input array, separated by ", ". We use the provided {@link Stringer} to convert each
     * element to its {@link String} representation.
     *
     * @param <T>      the type of the elements in the array.
     * @param array    the input array.
     * @param stringer the {@link Stringer} used to convert each element to its String representation.
     * @return a {@link String} containing the joined elements of the array with the specified delimiter.
     */
    public static <T> String join(final T[] array, final Stringer<T> stringer) {
        return join(array, ", ", stringer);
    }

    /**
     * Joins elements from the input array, separated by the specified delimiter. We use the provided {@link Stringer}
     * to convert each element to its {@link String} representation.
     *
     * @param <T>       the type of the elements in the array.
     * @param array     the input array.
     * @param delimiter the delimiter used to separate the joined elements.
     * @param stringer  the {@link Stringer} used to convert each element to its String representation.
     * @return a {@link String} containing the joined elements of the array with the specified delimiter.
     */
    public static <T> String join(final T[] array, final String delimiter, final Stringer<T> stringer) {
        Valid.checkNotNull(array, "Cannot join null array!");

        return join(Arrays.asList(array), delimiter, stringer);
    }

    /**
     * Joins elements from the input {@link Iterable array}, separated by ", ". We use the provided {@link Stringer} to
     * convert each element to its {@link String} representation.
     *
     * @param <T>      the type of the elements in the array.
     * @param array    the input {@link Iterable array}.
     * @param stringer the {@link Stringer} used to convert each element to its String representation.
     * @return a {@link String} containing the joined elements of the {@link Iterable array} with the specified
     * delimiter.
     */
    public static <T> String join(final Iterable<T> array, final Stringer<T> stringer) {
        return join(array, ", ", stringer);
    }

    /**
     * Joins elements from the input {@link Iterable array}, separated by the specified delimiter. We use the provided
     * {@link Stringer} to convert each element to its {@link String} representation.
     *
     * @param <T>       the type of the elements in the array.
     * @param array     the input {@link Iterable array}.
     * @param delimiter the delimiter used to separate the joined elements.
     * @param stringer  the {@link Stringer} used to convert each element to its String representation.
     * @return a {@link String} containing the joined elements of the {@link Iterable array} with the specified
     * delimiter.
     */
    public static <T> String join(final Iterable<T> array, final String delimiter, final Stringer<T> stringer) {
        final Iterator<T> it = array.iterator();
        final StringBuilder message = new StringBuilder();

        while (it.hasNext()) {
            final T next = it.next();

            if (next != null)
                message.append(stringer.toString(next)).append(it.hasNext() ? delimiter : "");
        }

        return message.toString();
    }

    /**
     * Converts some common classes such as {@link Entity} to its name automatically.
     *
     * @param arg the {@link Object} to be simplified.
     * @return the {@link String} representation of the {@link Object}.
     */
    public static String simplify(final Object arg) {
        if (arg instanceof Entity)
            return Remain.getName((Entity) arg);
        else if (arg instanceof CommandSender)
            return ((CommandSender) arg).getName();
        else if (arg instanceof World)
            return ((World) arg).getName();
        else if (arg instanceof Location)
            return shortLocation((Location) arg);
        else if (arg.getClass() == double.class || arg.getClass() == float.class)
            return MathUtil.formatTwoDigits((double) arg);
        else if (arg instanceof Collection)
            return join((Collection<?>) arg, ", ", Common::simplify);
        else if (arg instanceof ChatColor)
            return ((Enum<?>) arg).name().toLowerCase();
        else if (arg instanceof CompChatColor)
            return ((CompChatColor) arg).getName();
        else if (arg instanceof Enum)
            return arg.toString().toLowerCase();

        try {
            if (arg instanceof net.md_5.bungee.api.ChatColor)
                return ((net.md_5.bungee.api.ChatColor) arg).getName();
        } catch (final Exception e) {
            // No Minecraft compatible.
        }

        return arg.toString();
    }

    /**
     * Dynamically populates pages.
     *
     * @param <T>      the type of items in the list.
     * @param cellSize the number of items per page.
     * @param items    the {@link Iterable list} of items to be filled into pages.
     * @return a {@link Map} containing the page numbers as keys and a {@link List} of items as values.
     */
    public static <T> Map<Integer, List<T>> fillPages(final int cellSize, final Iterable<T> items) {
        final List<T> allItems = toList(items);

        final Map<Integer, List<T>> pages = new HashMap<>();
        final int pageCount = allItems.size() == cellSize ? 0 : allItems.size() / cellSize;

        for (int i = 0; i <= pageCount; i++) {
            final List<T> pageItems = new ArrayList<>();

            final int down = cellSize * i;
            final int up = down + cellSize;

            for (int valueIndex = down; valueIndex < up; valueIndex++)
                if (valueIndex < allItems.size()) {
                    final T page = allItems.get(valueIndex);

                    pageItems.add(page);
                } else
                    break;

            pages.put(i, pageItems);
        }

        return pages;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Converting and retyping
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Returns the last element of the given {@link List}.
     *
     * @param <T>  the type of elements in the list.
     * @param list the {@link List} of elements.
     * @return the last element of the {@link List}, or {@code null} if the list is {@code null} or empty.
     */
    public static <T> T last(final List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * Returns the last element of the given array.
     *
     * @param <T>   the type of elements in the array.
     * @param array the array of elements.
     * @return the last element of the array, or {@code null} if the array is {@code null} or empty.
     */
    public static <T> T last(final T[] array) {
        return array == null || array.length == 0 ? null : array[array.length - 1];
    }

    /**
     * Returns a {@link List} of world names.
     *
     * @return a {@code List} of world names.
     */
    public static List<String> getWorldNames() {
        final List<String> worlds = new ArrayList<>();

        for (final World world : Bukkit.getWorlds())
            worlds.add(world.getName());

        return worlds;
    }

    /**
     * Returns a {@link List} of player names including any that are vanished.
     *
     * @return a {@code List} of player names.
     */
    public static List<String> getPlayerNames() {
        return getPlayerNames(true, null);
    }

    /**
     * Returns a {@link List} of player names, optionally including vanished players.
     *
     * @param includeVanished should vanished players be included?
     * @return a {@code List} of player names.
     */
    public static List<String> getPlayerNames(final boolean includeVanished) {
        return getPlayerNames(includeVanished, null);
    }

    /**
     * Returns a {@link List} of player names, optionally including vanished players and that the given {@link Player}
     * can see.
     *
     * @param includeVanished should vanished players be included?
     * @param otherPlayer     the {@link Player} whose visibility is checked for vanished players.
     * @return a {@link List} of player names.
     */
    public static List<String> getPlayerNames(final boolean includeVanished, final Player otherPlayer) {
        final List<String> found = new ArrayList<>();

        for (final Player online : Remain.getOnlinePlayers()) {
            if (PlayerUtil.isVanished(online, otherPlayer) && !includeVanished)
                continue;

            found.add(online.getName());
        }

        return found;
    }

    /**
     * Returns a {@link List} of player nicknames, optionally including vanished players.
     *
     * @param includeVanished should vanished players be included?
     * @return a {@link List} of player nicknames.
     */
    public static List<String> getPlayerNicknames(final boolean includeVanished) {
        return getPlayerNicknames(includeVanished, null);
    }

    /**
     * Returns a {@link List} of player nicknames, optionally including vanished players and that the given
     * {@link Player} can see.
     *
     * @param includeVanished should vanished players be included?
     * @param otherPlayer     the {@link Player} whose visibility is checked for vanished players.
     * @return a {@link List} of player nicknames.
     */
    public static List<String> getPlayerNicknames(final boolean includeVanished, final Player otherPlayer) {
        final List<String> found = new ArrayList<>();

        for (final Player online : Remain.getOnlinePlayers()) {
            if (PlayerUtil.isVanished(online, otherPlayer) && !includeVanished)
                continue;

            found.add(HookManager.getNickColorless(online));
        }

        return found;
    }

    /**
     * Converts the elements of a {@link Iterable list} into a new type using a {@link TypeConverter}.
     *
     * @param <OLD>     the type of elements in the input list.
     * @param <NEW>     the type of elements in the output list.
     * @param list      the input {@link Iterable list} containing the elements to be converted.
     * @param converter the {@link TypeConverter} used to convert the elements.
     * @return a {@link List} containing the converted elements.
     */
    public static <OLD, NEW> List<NEW> convert(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
        final List<NEW> copy = new ArrayList<>();

        for (final OLD old : list) {
            final NEW result = converter.convert(old);

            if (result != null)
                copy.add(converter.convert(old));
        }

        return copy;
    }

    /**
     * Converts the elements of a {@link Iterable set} into a new type using a {@link TypeConverter}.
     *
     * @param <OLD>     the type of elements in the input set.
     * @param <NEW>     the type of elements in the output set.
     * @param set       the input {@link Iterable set} containing the elements to be converted.
     * @param converter the {@link TypeConverter} used to convert the elements.
     * @return a {@link Set} containing the converted elements.
     */
    public static <OLD, NEW> Set<NEW> convertSet(final Iterable<OLD> set, final TypeConverter<OLD, NEW> converter) {
        final Set<NEW> copy = new HashSet<>();

        for (final OLD old : set) {
            final NEW result = converter.convert(old);

            if (result != null)
                copy.add(converter.convert(old));
        }

        return copy;
    }

    /**
     * Converts the elements of a {@link Iterable strict list} into a new type using a {@link TypeConverter}.
     *
     * @param <OLD>     the type of elements in the input {@link Iterable list}.
     * @param <NEW>     the type of elements in the output strict list.
     * @param list      the input {@link Iterable list} containing the elements to be converted.
     * @param converter the {@link TypeConverter} used to convert the elements.
     * @return a {@link StrictList} containing the converted elements.
     */
    public static <OLD, NEW> StrictList<NEW> convertStrict(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
        final StrictList<NEW> copy = new StrictList<>();

        for (final OLD old : list)
            copy.add(converter.convert(old));

        return copy;
    }

    /**
     * Converts the keys and values of a {@link Map} into a new type using a {@link MapToMapConverter}.
     *
     * @param <OLD_KEY>   the type of keys in the input map.
     * @param <OLD_VALUE> the type of values in the input map.
     * @param <NEW_KEY>   the type of keys in the output map.
     * @param <NEW_VALUE> the type of values in the output map.
     * @param oldMap      the input {@link Map} containing the keys and values to be converted.
     * @param converter   the {@link MapToMapConverter} used to convert the keys and values.
     * @return a new {@link Map} containing the converted keys and values.
     */
    public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> Map<NEW_KEY, NEW_VALUE> convert(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
        final Map<NEW_KEY, NEW_VALUE> newMap = new HashMap<>();
        oldMap.forEach((key, value) -> newMap.put(converter.convertKey(key), converter.convertValue(value)));

        return newMap;
    }

    /**
     * Converts the keys and values of a {@link Map} into a new type using a {@link MapToMapConverter}. The converted
     * keys and values are added to a {@link StrictMap}.
     *
     * @param <OLD_KEY>   the type of keys in the input map.
     * @param <OLD_VALUE> the type of values in the input map.
     * @param <NEW_KEY>   the type of keys in the output strict map.
     * @param <NEW_VALUE> the type of values in the output strict map.
     * @param oldMap      the input {@link Map} containing the keys and values to be converted.
     * @param converter   the {@link MapToMapConverter} used to convert the keys and values.
     * @return a new {@link StrictMap} containing the converted keys and values.
     */
    public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> StrictMap<NEW_KEY, NEW_VALUE> convertStrict(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
        final StrictMap<NEW_KEY, NEW_VALUE> newMap = new StrictMap<>();
        oldMap.forEach((key, value) -> newMap.put(converter.convertKey(key), converter.convertValue(value)));

        return newMap;
    }

    /**
     * Converts the keys and values of a {@link Map} into a {@link StrictList} of a new type using a
     * {@link MapToListConverter}.
     *
     * @param <LIST_KEY>  the type of elements in the output strict list.
     * @param <OLD_KEY>   the type of keys in the input map.
     * @param <OLD_VALUE> the type of values in the input map.
     * @param map         the input {@link Map} containing the keys and values to be converted.
     * @param converter   the {@link MapToListConverter} used to convert the keys and values.
     * @return a new {@link StrictList} containing the converted elements.
     */
    public static <LIST_KEY, OLD_KEY, OLD_VALUE> StrictList<LIST_KEY> convertToList(final Map<OLD_KEY, OLD_VALUE> map, final MapToListConverter<LIST_KEY, OLD_KEY, OLD_VALUE> converter) {
        final StrictList<LIST_KEY> list = new StrictList<>();

        for (final Entry<OLD_KEY, OLD_VALUE> e : map.entrySet())
            list.add(converter.convert(e.getKey(), e.getValue()));

        return list;
    }

    /**
     * Converts an array of elements into a {@link List} of a new type using a {@link TypeConverter}.
     *
     * @param <OLD_TYPE> the type of elements in the input array.
     * @param <NEW_TYPE> the type of elements in the output list.
     * @param oldArray   the input array containing the elements to be converted.
     * @param converter  the {@link TypeConverter} used to convert the elements.
     * @return a new {@link List} containing the converted elements.
     */
    public static <OLD_TYPE, NEW_TYPE> List<NEW_TYPE> convert(final OLD_TYPE[] oldArray, final TypeConverter<OLD_TYPE, NEW_TYPE> converter) {
        final List<NEW_TYPE> newList = new ArrayList<>();

        for (final OLD_TYPE old : oldArray)
            newList.add(converter.convert(old));

        return newList;
    }

    /**
     * Splits a {@link String} into a {@link String} array based on the given maximum line length.
     *
     * @param input         the input {@link String} to be split.
     * @param maxLineLength the maximum length of each line in the output array.
     * @return a {@link String} array where each element represents a line within the input {@link String}.
     */
    public static String[] split(final String input, final int maxLineLength) {
        final StringTokenizer tokenizer = new StringTokenizer(input, " ");
        final StringBuilder output = new StringBuilder(input.length());

        int lineLength = 0;

        while (tokenizer.hasMoreTokens()) {
            final String word = tokenizer.nextToken();

            if (lineLength + word.length() > maxLineLength) {
                output.append("\n");

                lineLength = 0;
            }

            output.append(word).append(" ");
            lineLength += word.length() + 1;
        }

        return output.toString().split("\n");
    }

    // ------------------------------------------------------------------------------------------------------------
    // Miscellaneous message handling
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Removes {@code null} and empty elements from an array and returns a {@link List} containing the non-{@code null}
     * and non-empty elements.
     *
     * @param array the input array.
     * @param <T>   the type of the elements in the input array.
     * @return a {@link List} containing the non-{@code null} and non-empty elements from the input array.
     */
    public static <T> List<T> removeNullAndEmpty(final T @Nullable [] array) {
        return array != null ? removeNullAndEmpty(Arrays.asList(array)) : new ArrayList<>();
    }

    /**
     * Removes {@code null} and empty elements from a {@link List} and returns a new {@link List} containing the
     * non-{@code null} and non-empty elements.
     *
     * @param list the input {@link List}.
     * @param <T>  the type of the elements in the input list.
     * @return a new {@link List} containing the non-{@code null} and non-empty elements from the input {@link List}.
     */
    public static <T> List<T> removeNullAndEmpty(final List<T> list) {
        final List<T> copy = new ArrayList<>();

        for (final T key : list)
            if (key != null)
                if (key instanceof String) {
                    if (!((String) key).isEmpty())
                        copy.add(key);
                } else
                    copy.add(key);

        return copy;
    }

    /**
     * Replaces {@code null} elements in a {@link String} array with empty {@link String Strings} and returns the
     * modified array.
     *
     * @param list the input {@link String} array.
     * @return the modified {@link String} array with {@code null} elements replaced by empty {@link String Strings}.
     */
    public static String[] replaceNullWithEmpty(final String[] list) {
        for (int i = 0; i < list.length; i++)
            if (list[i] == null)
                list[i] = "";

        return list;
    }

    /**
     * Returns the element at the specified index in the given array. If the index is within the bounds of the array,
     * the corresponding element is returned. Otherwise, the default value is returned.
     *
     * @param array        the input array.
     * @param index        the index of the element to retrieve.
     * @param defaultValue the default value to return if the index is out of bounds.
     * @param <T>          the type of elements in the array.
     * @return the element at the specified index in the array, or the default value if the index is out of bounds.
     */
    public static <T> T getOrDefault(final T[] array, final int index, final T defaultValue) {
        return index < array.length ? array[index] : defaultValue;
    }

    /**
     * Returns the input {@link String} if it is not {@code null} and not equal to "none". Otherwise, an empty
     * {@link String} is returned.
     *
     * @param input the input {@link String}.
     * @return the input {@link String} if it is not {@code null} and not equal to "none", otherwise an empty
     * {@link String}.
     */
    public static String getOrEmpty(final String input) {
        return input == null || "none".equalsIgnoreCase(input) ? "" : input;
    }

    /**
     * Returns the input {@link String} if it is not {@code null}, not equal to "none", and not empty. Otherwise,
     * {@code null} is returned.
     *
     * @param input the input {@link String}.
     * @return the input {@link String} if it is not {@code null}, not equal to "none", and not empty, otherwise
     * {@code null}.
     */
    public static String getOrNull(final String input) {
        return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? null : input;
    }

    /**
     * Returns the value if it is not {@code null}. Otherwise, the default value is returned.
     * <p>
     * If the value is a {@link String}, the default value is returned if the value is empty or equals to "none".
     *
     * @param value        the value to be checked.
     * @param defaultValue the default value to be returned if the actual value is {@code null}.
     * @param <T>          the type of the value and default value.
     * @return the value if it is not {@code null}, otherwise the default value.
     */
    public static <T> T getOrDefault(final T value, final T defaultValue) {
        if (value instanceof String && ("none".equalsIgnoreCase((String) value) || "".equals(value)))
            return defaultValue;

        return getOrDefaultStrict(value, defaultValue);
    }

    /**
     * Returns the value if it is not {@code null}. Otherwise, the default value is returned.
     *
     * @param value        the value to be checked.
     * @param defaultValue the default value to be returned if the actual value is {@code null}.
     * @param <T>          the type of the value and default value.
     * @return the value if it is not {@code null}, otherwise the default value.
     */
    public static <T> T getOrDefaultStrict(final T value, final T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the next element in the given {@link List}, increasing the index by 1 if forward is {@code true}, or
     * decreasing it by 1 if it is {@code false}.
     *
     * @param given   the current element.
     * @param list    the {@link List} of elements.
     * @param forward should the index be increased?
     * @param <T>     the type of the elements.
     * @return the next element in the {@link List}. Returns {@code null} if the given element is {@code null} and the
     * list is empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNext(final T given, final List<T> list, final boolean forward) {
        if (given == null && list.isEmpty())
            return null;

        final T[] array = (T[]) Array.newInstance((given != null ? given : list.get(0)).getClass(), list.size());

        for (int i = 0; i < list.size(); i++)
            Array.set(array, i, list.get(i));

        return getNext(given, array, forward);
    }

    /**
     * Returns the next element in the given array, increasing the index by 1 if forward is {@code true}, or decreasing
     * it by 1 if it is {@code false}.
     *
     * @param given   the current element.
     * @param array   the array of elements.
     * @param forward should the index be increased?
     * @param <T>     the type of the elements.
     * @return the next element in the array. Returns {@code null} if the array is empty.
     */
    public static <T> T getNext(final T given, final T[] array, final boolean forward) {
        if (array.length == 0)
            return null;

        int index = 0;

        for (int i = 0; i < array.length; i++) {
            final T element = array[i];

            if (element.equals(given)) {
                index = i;

                break;
            }
        }

        if (index != -1) {
            final int nextIndex = index + (forward ? 1 : -1);

            // Return the first slot if we have reached the end, or the last otherwise.
            return nextIndex >= array.length ? array[0] : nextIndex < 0 ? array[array.length - 1] : array[nextIndex];
        }

        return null;
    }

    /**
     * Converts a {@link Collection} of {@link String Strings} into an array of {@link String Strings}.
     *
     * @param array the {@link Collection} of {@link String Strings} to convert.
     * @return an array of {@link String Strings}. Returns an empty array if the {@link Collection} is {@code null}.
     */
    public static String[] toArray(final Collection<String> array) {
        return array == null ? new String[0] : array.toArray(new String[array.size()]);
    }

    /**
     * Converts an array of elements into an {@link ArrayList}.
     *
     * @param array the array of elements to convert.
     * @param <T>   the type of elements in the array.
     * @return an {@link ArrayList} containing the elements from the array. Returns an empty {@link ArrayList} if the
     * array is {@code null}.
     */
    @SafeVarargs
    public static <T> ArrayList<T> toList(final T... array) {
        return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
    }

    /**
     * Converts an {@link Iterable} of elements into a {@link List}.
     *
     * @param iterable the {@link Iterable} of elements to convert.
     * @param <T>      the type of elements in the iterable.
     * @return a {@link List} containing the elements from the {@link Iterable}. Returns an empty {@link List} if the
     * {@link Iterable} is {@code null}.
     */
    public static <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();

        if (iterable != null)
            iterable.forEach(el -> {
                if (el != null)
                    list.add(el);
            });

        return list;
    }

    /**
     * Reverses the order of elements in an array.
     *
     * @param array the array of elements to reverse.
     * @param <T>   the type of elements in the array.
     * @return the reversed array. Returns {@code null} if the input array is {@code null}.
     */
    public static <T> T[] reverse(final T[] array) {
        if (array == null)
            return null;

        int i = 0;
        int j = array.length - 1;

        while (j > i) {
            final T tmp = array[j];

            array[j] = array[i];
            array[i] = tmp;

            j--;
            i++;
        }

        return array;
    }

    /**
     * Creates a new {@link HashMap} with a single key-value pair.
     *
     * @param firstKey   the key of the first entry.
     * @param firstValue the value of the first entry.
     * @param <A>        the type of the key.
     * @param <B>        the type of the value.
     * @return a new {@link HashMap} with the specified key-value pair.
     */
    public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue) {
        final Map<A, B> map = new HashMap<>();
        map.put(firstKey, firstValue);

        return map;
    }

    /**
     * Creates a new {@link HashSet} with the given elements.
     *
     * @param <T>  the type of elements in the HashSet.
     * @param keys the elements to be added to the HashSet.
     * @return a new {@link HashSet} containing the given elements.
     * @deprecated badly named, use {@link #newHashSet(Object[])} instead.
     */
    @Deprecated
    @SafeVarargs
    public static <T> Set<T> newSet(final T... keys) {
        return newHashSet(keys);
    }

    /**
     * Creates a new {@link HashSet} with the given elements.
     *
     * @param <T>  the type of elements in the set.
     * @param keys the elements to be added to the set.
     * @return a new {@link HashSet} containing the given elements.
     */
    @SafeVarargs
    public static <T> Set<T> newHashSet(final T... keys) {
        return new HashSet<>(Arrays.asList(keys));
    }

    /**
     * Create a new mutable {@link ArrayList} with the given elements.
     *
     * @param <T>  the type of elements in the list.
     * @param keys the elements to be added to the list.
     * @return a new {@link ArrayList} containing the given elements.
     * @deprecated badly named, use {@link #newArrayList(Object[])} instead.
     */
    @Deprecated
    @SafeVarargs
    public static <T> List<T> newList(final T... keys) {
        return newArrayList(keys);
    }

    /**
     * Create a new mutable {@link ArrayList} with the given elements.
     *
     * @param <T>  the type of elements in the list.
     * @param keys the elements to be added to the list.
     * @return a new {@link ArrayList} containing the given elements.
     */
    @SafeVarargs
    public static <T> List<T> newArrayList(final T... keys) {
        final List<T> list = new ArrayList<>();

        Collections.addAll(list, keys);

        return list;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Scheduling
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Runs the given task after 1 tick if the plugin is enabled correctly.
     *
     * @param task the task to be run.
     * @param <T>  the type of the task (must implement {@link Runnable}).
     * @return a {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static <T extends Runnable> BukkitTask runLater(final T task) {
        return runLater(1, task);
    }

    /**
     * Runs the given task after the given delay even if the plugin is disabled for some reason.
     *
     * @param delayTicks the delay (in ticks) to wait before running the task.
     * @param task       the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runLater(final int delayTicks, final Runnable task) {
        final BukkitScheduler scheduler = Bukkit.getScheduler();
        final JavaPlugin instance = SimplePlugin.getInstance();

        try {
            return runIfDisabled(task) ? null : delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTask(instance) : scheduler.runTask(instance, task) : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLater(instance, delayTicks) : scheduler.runTaskLater(instance, task, delayTicks);
        } catch (final NoSuchMethodError err) {
            return runIfDisabled(task) ? null
                    : delayTicks == 0
                    ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTask(instance) : getTaskFromId(scheduler.scheduleSyncDelayedTask(instance, task))
                    : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLater(instance, delayTicks) : getTaskFromId(scheduler.scheduleSyncDelayedTask(instance, task, delayTicks));
        }
    }

    /**
     * Runs the given task asynchronously on the next tick even if the plugin is disabled for some reason.
     *
     * @param task the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runAsync(final Runnable task) {
        return runLaterAsync(0, task);
    }

    /**
     * Runs the given task asynchronously on the next tick even if the plugin is disabled for some reason.
     *
     * @param task the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runLaterAsync(final Runnable task) {
        return runLaterAsync(0, task);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Bukkit scheduling
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Runs the given task asynchronously after the given delay even if the plugin is disabled for some reason.
     *
     * @param delayTicks the delay (in ticks) to wait before running the task.
     * @param task       the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runLaterAsync(final int delayTicks, final Runnable task) {
        final BukkitScheduler scheduler = Bukkit.getScheduler();
        final JavaPlugin instance = SimplePlugin.getInstance();

        try {
            return runIfDisabled(task) ? null : delayTicks == 0 ? task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskAsynchronously(instance) : scheduler.runTaskAsynchronously(instance, task) : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskLaterAsynchronously(instance, delayTicks) : scheduler.runTaskLaterAsynchronously(instance, task, delayTicks);
        } catch (final NoSuchMethodError err) {
            return runIfDisabled(task) ? null
                    : delayTicks == 0
                    ? getTaskFromId(scheduler.scheduleAsyncDelayedTask(instance, task))
                    : getTaskFromId(scheduler.scheduleAsyncDelayedTask(instance, task, delayTicks));
        }
    }

    /**
     * Runs the given task on the next tick with a fixed delay between repetitions, even if the plugin is disabled for
     * some reason.
     *
     * @param repeatTicks the delay (in ticks) between repetitions of the task.
     * @param task        the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runTimer(final int repeatTicks, final Runnable task) {
        return runTimer(0, repeatTicks, task);
    }

    /**
     * Runs the given task after the given delay with a fixed delay between repetitions, even if the plugin is disabled
     * for some reason.
     *
     * @param delayTicks  the delay (in ticks) to wait before running the task.
     * @param repeatTicks the delay (in ticks) between repetitions of the task.
     * @param task        the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runTimer(final int delayTicks, final int repeatTicks, final Runnable task) {
        try {
            return runIfDisabled(task) ? null : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimer(SimplePlugin.getInstance(), delayTicks, repeatTicks) : Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);
        } catch (final NoSuchMethodError err) {
            return runIfDisabled(task) ? null
                    : getTaskFromId(Bukkit.getScheduler().scheduleSyncRepeatingTask(SimplePlugin.getInstance(), task, delayTicks, repeatTicks));
        }
    }

    /**
     * Runs the given task asynchronously on the next tick with a fixed delay between repetitions, even if the plugin is
     * disabled for some reason.
     *
     * @param repeatTicks the delay (in ticks) between repetitions of the task.
     * @param task        the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runTimerAsync(final int repeatTicks, final Runnable task) {
        return runTimerAsync(0, repeatTicks, task);
    }

    /**
     * Runs the given task after the given delay with a fixed delay between repetitions, even if the plugin is disabled
     * for some reason.
     *
     * @param delayTicks  the delay (in ticks) to wait before running the task.
     * @param repeatTicks the delay (in ticks) between repetitions of the task.
     * @param task        the task to be run.
     * @return the {@link BukkitTask} representing the scheduled task, or {@code null}.
     */
    public static BukkitTask runTimerAsync(final int delayTicks, final int repeatTicks, final Runnable task) {
        try {
            return runIfDisabled(task) ? null : task instanceof BukkitRunnable ? ((BukkitRunnable) task).runTaskTimerAsynchronously(SimplePlugin.getInstance(), delayTicks, repeatTicks) : Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), task, delayTicks, repeatTicks);
        } catch (final NoSuchMethodError err) {
            return runIfDisabled(task) ? null
                    : getTaskFromId(Bukkit.getScheduler().scheduleAsyncRepeatingTask(SimplePlugin.getInstance(), task, delayTicks, repeatTicks));
        }
    }

    /**
     * Converts the given task ID into a {@link BukkitTask}.
     *
     * @param taskId the ID of the task to retrieve.
     * @return the {@link BukkitTask} with the specified ID, or {@code null} if no task was found.
     */
    @Nullable
    private static BukkitTask getTaskFromId(final int taskId) {
        for (final BukkitTask task : Bukkit.getScheduler().getPendingTasks())
            if (task.getTaskId() == taskId)
                return task;

        return null;
    }

    /**
     * Runs the specified task if the plugin is disabled.
     * <p>
     * In case the plugin is disabled, this method will return {@code true} and the task will be run. Otherwise, we
     * return {@code false} and the task is run correctly in Bukkit's scheduler.
     * <p>
     * This is a fail-safe for critical save-on-exit operations in case the plugin is improperly reloaded using a plugin
     * manager such as PlugMan or malfunctions.
     *
     * @param task the task to be run.
     * @return {@code true} if the task was run, or {@code false} if the plugin is enabled.
     */
    private static boolean runIfDisabled(final Runnable task) {
        if (!SimplePlugin.getInstance().isEnabled()) {
            task.run();

            return true;
        }

        return false;
    }

    /**
     * Calls the specified {@link Event} and checks if it was cancelled.
     * <p>
     * If the event is {@link Cancellable}, this method will return {@code true} if the event is not cancelled,
     * otherwise it returns {@code false}.
     *
     * @param event the {@link Event} to be called.
     * @return {@code true} if the {@link Event} is not cancelled or not {@link Cancellable}, {@code false} otherwise.
     */
    public static boolean callEvent(final Event event) {
        Bukkit.getPluginManager().callEvent(event);

        return !(event instanceof Cancellable) || !((Cancellable) event).isCancelled();
    }

    /**
     * Registers the specified {@link Listener}.
     *
     * @param listener the {@link Listener} to be registered.
     */
    public static void registerEvents(final Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, SimplePlugin.getInstance());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Miscellaneous
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Returns a {@link Map} representation of the specified {@link Object}. The object can be a {@link Map},
     * {@link MemorySection}, or {@link ConfigSection}.
     *
     * @param mapOrSection the {@link Object} to retrieve a map from.
     * @return a {@link Map} representation of the specified {@link Object}.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMapFromSection(@NonNull Object mapOrSection) {
        mapOrSection = Remain.getRootOfSectionPathData(mapOrSection);

        final Map<String, Object> map = mapOrSection instanceof ConfigSection ? ((ConfigSection) mapOrSection).getValues(false)
                : mapOrSection instanceof Map ? (Map<String, Object>) mapOrSection
                : mapOrSection instanceof MemorySection ? ReflectionUtil.getFieldContent(mapOrSection, "map") : null;

        Valid.checkNotNull(map, "Unexpected " + mapOrSection.getClass().getSimpleName() + " '" + mapOrSection + "'. Must be Map or MemorySection! (Do not just send config name here, but the actual section with get('section'))");

        final Map<String, Object> copy = new LinkedHashMap<>();

        for (final Entry<String, Object> entry : map.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            copy.put(key, Remain.getRootOfSectionPathData(value));
        }

        return copy;
    }

    /**
     * Checks if the specified domain is reachable. This method is blocking.
     *
     * @param url     the URL of the domain to check.
     * @param timeout the timeout for the HTTP connection in milliseconds.
     * @return {@code true} if the domain is reachable, {@code false} otherwise.
     */
    public static boolean isDomainReachable(String url, final int timeout) {
        url = url.replaceFirst("^https", "http");

        try {
            final HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();

            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.setRequestMethod("HEAD");

            final int responseCode = c.getResponseCode();
            return 200 <= responseCode && responseCode <= 399;
        } catch (final IOException exception) {
            return false;
        }
    }

    /**
     * Checked sleep method from {@link Thread#sleep(long)} but without the try-catch need.
     *
     * @param millis the time (in milliseconds) to sleep the thread for.
     */
    public static void sleep(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------------------------------------------

    /**
     * A simple interface for converting objects into {@link String Strings}.
     *
     * @param <T> the type that is being converted.
     */
    public interface Stringer<T> {

        /**
         * Converts the given object into a {@link String}.
         *
         * @param object the object to convert.
         * @return the {@link String} representation of the given object.
         */
        String toString(T object);
    }

    /**
     * A simple interface for converting between types.
     *
     * @param <OLD> the initial type to convert from.
     * @param <NEW> the final type to convert to.
     */
    public interface TypeConverter<OLD, NEW> {

        /**
         * Converts the given value.
         *
         * @param value the value to convert.
         * @return the converted value.
         */
        NEW convert(OLD value);
    }

    /**
     * Convenience class for converting a {@link Map} to a {@link List}.
     *
     * @param <O> the type to convert to.
     * @param <K> the key type.
     * @param <V> the value type.
     */
    @SuppressWarnings("hiding")
    public interface MapToListConverter<O, K, V> {

        /**
         * Converts the given key and value into a new type stored in a {@link List}.
         *
         * @param key   the key to be converted
         * @param value the value to be converted
         * @return the converted result as output
         */
        O convert(K key, V value);
    }

    /**
     * Convenience class for converting between {@link Map Maps}.
     *
     * @param <A> the initial key type to convert from.
     * @param <B> the initial value type to convert from.
     * @param <C> the final key type to convert to.
     * @param <D> the final value type to convert to.
     */
    public interface MapToMapConverter<A, B, C, D> {

        /**
         * Converts the old key into a new key type
         *
         * @param key the old key to be converted
         * @return the new key type.
         */
        C convertKey(A key);

        /**
         * Converts the old value into a new value type.
         *
         * @param value the old value to be converted.
         * @return the new value type.
         */
        D convertValue(B value);
    }

    /**
     * Represents a timed {@link CharSequence}, used when checking for regular expressions so we time how long it takes
     * and stop the execution if it takes too long.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TimedCharSequence implements CharSequence {

        /**
         * The timed message.
         */
        private final CharSequence message;

        /**
         * The timeout limit in milliseconds.
         */
        private final long futureTimestampLimit;

        /**
         * Returns a character at the given index, or throws an error if this is called too late after the constructor.
         */
        @Override
        public char charAt(final int index) {
            // Temporarily disabled due to a rare condition upstream when we take this message
            // and run it in a runnable, then this is still being evaluated past the limit and
            // it fails.
            //if (System.currentTimeMillis() > futureTimestampLimit)
            //	throw new RegexTimeoutException(message, futureTimestampLimit);

            try {
                return this.message.charAt(index);
            } catch (final StringIndexOutOfBoundsException ex) {
                // Odd case: Java 8 seems to overflow for unicode characters that are too long.
                return ' ';
            }
        }

        /**
         * Returns the length of the message.
         *
         * @return the length of the message.
         */
        @Override
        public int length() {
            return this.message.length();
        }

        /**
         * Returns a new {@link CharSequence} that is a subsequence of this message.
         *
         * @param start the start index (inclusive).
         * @param end   the end index (exclusive).
         * @return a new {@link CharSequence} that is a subsequence of this message.
         */
        @NonNull
        @Override
        public CharSequence subSequence(final int start, final int end) {
            return new TimedCharSequence(this.message.subSequence(start, end), this.futureTimestampLimit);
        }

        /**
         * Returns a {@link String} representation of the message.
         *
         * @return a {@link String} representation of the message.
         */
        @NonNull
        @Override
        public String toString() {
            return this.message.toString();
        }

        /**
         * Creates a new {@link TimedCharSequence} with the given message, using the timeout from
         * {@link SimpleSettings#REGEX_TIMEOUT}.
         *
         * @param message the {@link CharSequence} message to be encapsulated.
         * @return a new {@link TimedCharSequence} with the given message.
         */
        public static TimedCharSequence withSettingsLimit(final CharSequence message) {
            return new TimedCharSequence(message, System.currentTimeMillis() + SimpleSettings.REGEX_TIMEOUT);
        }
    }
}