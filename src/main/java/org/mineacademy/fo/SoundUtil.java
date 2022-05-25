package org.mineacademy.fo;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.mineacademy.fo.remain.CompSound;

/**
 * Utility class containing sounds ready to play.
 */
public class SoundUtil {

    public static class Play {
        public static void LEVEL_UP(Player player) {
            CompSound.LEVEL_UP.play(player);
        }

        public static void NO(Player player) {
            CompSound.VILLAGER_NO.play(player, 1F, 0.85F);
        }

        public static void ORB(Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1.4F);
        }

        public static void CLICK_LOW(Player player) {
            CompSound.CLICK.play(player, 0.5F, 0.8F);
        }

        public static void CLICK_HIGH(Player player) {
            CompSound.CLICK.play(player, 0.5F, 1.2F);
        }

        public static void POP(Player player) {
            CompSound.CHICKEN_EGG_POP.play(player, 0.5F, 1F);
        }

        public static void POP_HIGH(Player player) {
            CompSound.CHICKEN_EGG_POP.play(player, 0.5F, 1.2F);
        }

        public static void POP_LOW(Player player) {
            CompSound.CHICKEN_EGG_POP.play(player, 0.5F, 0.8F);
        }
    }

}
