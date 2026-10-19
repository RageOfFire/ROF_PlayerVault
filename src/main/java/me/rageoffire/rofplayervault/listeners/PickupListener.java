package me.rageoffire.rofplayervault.listeners;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.managers.ConfigManager;
import me.rageoffire.rofplayervault.managers.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles auto-pickup on 1.8–1.11 via the deprecated PlayerPickupItemEvent.
 *
 * On 1.12+ a separate listener class (EntityPickupListener) is loaded
 * dynamically at runtime so this file compiles cleanly against the 1.8.8 API.
 */
@SuppressWarnings("deprecation")
public class PickupListener implements Listener {

    private final ROFPlayerVault plugin;
    private final VaultManager   vm;
    private final ConfigManager  cfg;

    public PickupListener(ROFPlayerVault plugin) {
        this.plugin = plugin;
        this.vm     = plugin.getVaultManager();
        this.cfg    = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!shouldIntercept(player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (!shouldPickup(item)) return;
        if (!redirectToVault(player, item)) return;

        event.getItem().remove();
        event.setCancelled(true);
    }

    // ── Shared helpers (also used by EntityPickupListener) ────────────────────

    boolean shouldIntercept(Player player) {
        if (!cfg.isPickupEnabled()) return false;
        if (!player.hasPermission("rofvault.use")) return false;
        return player.getInventory().firstEmpty() == -1;
    }

    boolean shouldPickup(ItemStack item) {
        if (vm.isBlacklisted(item)) return false;
        boolean whitelist = "WHITELIST".equals(cfg.getPickupMode());
        boolean listed    = cfg.getPickupItems().contains(item.getType().name());
        return whitelist == listed;
    }

    boolean redirectToVault(Player player, ItemStack item) {
        int targetVault = cfg.getPickupVault();
        if (!player.hasPermission("rofvault.storage." + targetVault)) return false;
        if (!vm.isVaultOpen(player.getUniqueId(), targetVault)) return false;
        return vm.redirectItemToVault(player, targetVault, item);
    }
}
