package org.mineacademy.fo;

import lombok.experimental.UtilityClass;
import org.bukkit.event.block.Action;

/**
 * 2020-03-09 上午 09:25
 */
@UtilityClass
public class ActionChecker {

    public boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_BLOCK
                || action == Action.RIGHT_CLICK_AIR;
    }

    public boolean isLeftClick(Action action) {
        return action == Action.LEFT_CLICK_BLOCK
                || action == Action.LEFT_CLICK_AIR;
    }
}