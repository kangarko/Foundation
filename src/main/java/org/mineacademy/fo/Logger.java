package org.mineacademy.fo;

import org.mineacademy.fo.exception.CommandException;

/**
 * This class is a wrapper on Common logger methods.
 * It has some improvements:<br>
 * <ul>
 *     <li>It does not require all messages to be String.</li>
 *     <li>It appends the message with a prefix.<br>
 *     This prefix is empty by default. You can change it by calling Logger.LOG_PREFIX.</li>
 * </ul>
 */
public class Logger {

    public static String LOG_PREFIX = "";

    /**
     * Log an info message to the console.
     * @param s the message
     */
    public static void info(Object s){ Common.log(LOG_PREFIX + s); }

    /**
     * Log a warning to the console.
     * @param s the warning message
     */
    public static void warning(Object s){
        Common.warning("&e" + LOG_PREFIX + s);
    }

    /**
     * Log an error message to the console.
     * @param s the error message
     */
    public static void error(Object s){
        Common.error(new CommandException(), "&6" + LOG_PREFIX + s);
    }

    /**
     * Throw an exception and log the message to the console.
     * @param t an exception or an error
     * @param s the message
     */
    public static void error(Throwable t, Object s){
        Common.error(t, "&6" + LOG_PREFIX + s);
    }

    /**
     * Append prefix and colorize the message.
     * @param s the message
     * @return colorized prefixed message
     */
    public static String get(String s){
        return Common.colorize(LOG_PREFIX + s);
    }

}
