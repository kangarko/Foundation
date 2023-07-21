package org.mineacademy.fo.remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.boss.BarStyle;
import org.mineacademy.fo.Common;

/**
 * A wrapper class for {@link BarStyle} from Bukkit.
 */
@Getter
@RequiredArgsConstructor
public enum CompBarStyle {

    /**
     * Makes the boss bar solid (no segments).
     */
    SOLID("SOLID", "SOLID"),

    /**
     * Splits the boss bar into 6 segments.
     */
    SEGMENTED_6("SEGMENTED_6", "SEG6"),

    /**
     * Splits the boss bar into 10 segments.
     */
    SEGMENTED_10("SEGMENTED_10", "SEG10"),

    /**
     * Splits the boss bar into 12 segments.
     */
    SEGMENTED_12("SEGMENTED_12", "SEG12"),

    /**
     * Splits the boss bar into 20 segments.
     */
    SEGMENTED_20("SEGMENTED_20", "SEG20");

    /**
     * The internal key for this bar style.
     */
    private final String key;

    /**
     * The short key for this bar style.
     */
    private final String shortKey;

    /**
     * Attempts to load a bar style from the given key.
     *
     * @param key the key to load.
     * @return the found bar style.
     */
    public static CompBarStyle fromKey(final String key) {
        for (final CompBarStyle style : values())
            if (style.key.equalsIgnoreCase(key) || style.shortKey.equalsIgnoreCase(key))
                return style;

        throw new IllegalArgumentException("No such CompBarStyle: " + key + ". Available: " + Common.join(values()));
    }

    /**
     * Returns the internal key for this bar style.
     *
     * @return the internal key for this bar style.
     */
    @Override
    public String toString() {
        return this.key;
    }
}
