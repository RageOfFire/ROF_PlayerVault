package me.rageoffire.rofplayervault.listeners;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import org.bukkit.Bukkit;

/**
 * Registers the correct pickup listener depending on the server version.
 *
 * 1.12+  → EntityPickupListener  (EntityPickupItemEvent)
 * 1.8–1.11 → PickupListener      (deprecated PlayerPickupItemEvent)
 *
 * Keeping the version check here avoids touching ROFPlayerVault's main class.
 */
public final class PickupListenerFactory {

    private PickupListenerFactory() {}

    public static void register(ROFPlayerVault plugin) {
        if (hasEntityPickupEvent()) {
            Bukkit.getPluginManager().registerEvents(new EntityPickupListener(plugin), plugin);
            plugin.getLogger().info("Auto-pickup: using EntityPickupItemEvent (1.12+)");
        } else {
            Bukkit.getPluginManager().registerEvents(new PickupListener(plugin), plugin);
            plugin.getLogger().info("Auto-pickup: using PlayerPickupItemEvent (1.8–1.11)");
        }
    }

    private static boolean hasEntityPickupEvent() {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
