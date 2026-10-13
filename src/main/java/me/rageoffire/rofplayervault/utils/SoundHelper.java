package me.rageoffire.rofplayervault.utils;

import me.rageoffire.rofplayervault.managers.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Plays the configured vault-open sound.
 *
 * Handles the naming difference between 1.8 (CHEST_OPEN) and 1.9+
 * (BLOCK_CHEST_OPEN) by trying the configured name first, then common
 * fallbacks, and silently skipping if nothing matches.
 */
public final class SoundHelper {

    private SoundHelper() {}

    public static void playOpenSound(Player player, ConfigManager cfg) {
        String soundName = cfg.getOpenSound();
        if (soundName == null || soundName.isEmpty()) return;

        Sound sound = resolveSound(soundName);
        if (sound == null) return;

        player.playSound(player.getLocation(), sound, cfg.getOpenVolume(), cfg.getOpenPitch());
    }

    private static Sound resolveSound(String name) {
        // Direct match
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        // 1.8 → 1.9+ mapping for the most common vault sounds
        String legacy = name.toUpperCase();
        try {
            switch (legacy) {
                case "CHEST_OPEN":         return Sound.valueOf("BLOCK_CHEST_OPEN");
                case "CHEST_CLOSE":        return Sound.valueOf("BLOCK_CHEST_CLOSE");
                case "CHEST_LOCKED":       return Sound.valueOf("BLOCK_CHEST_LOCKED");
                case "ENDERCHEST_OPEN":    return Sound.valueOf("BLOCK_ENDERCHEST_OPEN");
                case "ENDERCHEST_CLOSE":   return Sound.valueOf("BLOCK_ENDERCHEST_CLOSE");
                default: break;
            }
        } catch (IllegalArgumentException ignored) {}

        // 1.9+ → 1.8 fallback
        try {
            switch (legacy) {
                case "BLOCK_CHEST_OPEN":       return Sound.valueOf("CHEST_OPEN");
                case "BLOCK_CHEST_CLOSE":      return Sound.valueOf("CHEST_CLOSE");
                case "BLOCK_ENDERCHEST_OPEN":  return Sound.valueOf("ENDERCHEST_OPEN");
                case "BLOCK_ENDERCHEST_CLOSE": return Sound.valueOf("ENDERCHEST_CLOSE");
                default: break;
            }
        } catch (IllegalArgumentException ignored) {}

        return null; // no match — silently skip
    }
}
