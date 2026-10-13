package me.rageoffire.rofplayervault.listeners;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.managers.ConfigManager;
import me.rageoffire.rofplayervault.managers.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Intercepts item pickup events when the player's inventory is full
 * and redirects qualifying items into the player's target vault instead.
 *
 * Uses EntityPickupItemEvent on 1.12+ and falls back to the deprecated
 * PlayerPickupItemEvent on 1.8–1.11 via a try/catch class-load check.
 */
public class PickupListener implements Listener {

    private final ROFPlayerVault plugin;
    private final VaultManager   vm;
    private final ConfigManager  cfg;

    /** True when the server is 1.12+ and EntityPickupItemEvent is available. */
    private final boolean useEntityEvent;

    public PickupListener(ROFPlayerVault plugin) {
        this.plugin = plugin;
        this.vm     = plugin.getVaultManager();
        this.cfg    = plugin.getConfigManager();

        boolean hasEntity;
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            hasEntity = true;
        } catch (ClassNotFoundException e) {
            hasEntity = false;
        }
        this.useEntityEvent = hasEntity;
    }

    // ── 1.12+ ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!useEntityEvent) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!shouldIntercept(player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (!shouldPickup(item)) return;
        if (!redirectToVault(player, item)) return;

        event.getItem().remove();
        event.setCancelled(true);
    }

    // ── 1.8–1.11 ─────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        if (useEntityEvent) return;   // handled above
        Player player = event.getPlayer();
        if (!shouldIntercept(player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (!shouldPickup(item)) return;
        if (!redirectToVault(player, item)) return;

        event.getItem().remove();
        event.setCancelled(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true when:
     *  - auto-pickup is enabled
     *  - player has the vault.use permission
     *  - player's own inventory is completely full
     */
    private boolean shouldIntercept(Player player) {
        if (!cfg.isPickupEnabled()) return false;
        if (!player.hasPermission("rofvault.use")) return false;
        return player.getInventory().firstEmpty() == -1;
    }

    /**
     * Applies the whitelist / blacklist filter from config.
     */
    private boolean shouldPickup(ItemStack item) {
        if (vm.isBlacklisted(item)) return false;

        boolean whitelist = "WHITELIST".equals(cfg.getPickupMode());
        boolean listed    = cfg.getPickupItems().contains(item.getType().name());
        return whitelist == listed;   // whitelist: must be listed; blacklist: must NOT be listed
    }

    /**
     * Attempts to add the item to the player's target vault.
     * Returns true only if the item was fully accepted.
     */
    private boolean redirectToVault(Player player, ItemStack item) {
        int targetVault = cfg.getPickupVault();
        if (!player.hasPermission("rofvault.storage." + targetVault)) return false;
        if (!vm.isVaultOpen(player.getUniqueId(), targetVault)) return false;

        // We need to access the live inventory; VaultManager exposes it indirectly.
        // Re-use the runPickupCheck path is not ideal here since we already have the
        // specific item.  Instead, delegate to VaultManager's direct redirect helper.
        return vm.redirectItemToVault(player, targetVault, item);
    }
}
