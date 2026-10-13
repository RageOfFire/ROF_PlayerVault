package me.rageoffire.rofplayervault.listeners;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.managers.ConfigManager;
import me.rageoffire.rofplayervault.managers.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final ROFPlayerVault plugin;
    private final VaultManager   vm;
    private final ConfigManager  cfg;

    public InventoryListener(ROFPlayerVault plugin) {
        this.plugin = plugin;
        this.vm     = plugin.getVaultManager();
        this.cfg    = plugin.getConfigManager();
    }

    // ── Vault closed ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (vm.getOpenVaultNumber(player) == null) return;

        vm.onVaultClose(player);
    }

    // ── Block blacklisted items ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (vm.getOpenVaultNumber(player) == null) return;

        // Read-only vault: block all modifications
        Integer vaultNum = vm.getOpenVaultNumber(player);
        if (vaultNum != null && vm.isReadOnly(player.getUniqueId(), vaultNum)) {
            event.setCancelled(true);
            return;
        }

        // Determine what item is being moved into the vault
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Item being placed INTO the vault top inventory
        if (event.getClickedInventory() != null &&
                event.getClickedInventory().equals(event.getView().getTopInventory())) {

            ItemStack incoming = null;
            switch (event.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                    incoming = cursor;
                    break;
                case HOTBAR_SWAP:
                    int hotbarSlot = event.getHotbarButton();
                    if (hotbarSlot >= 0) {
                        incoming = player.getInventory().getItem(hotbarSlot);
                    }
                    break;
                default:
                    break;
            }

            if (incoming != null && vm.isBlacklisted(incoming)) {
                event.setCancelled(true);
                player.sendMessage(cfg.getMessage("blacklisted-item"));
            }
        }

        // Shift-click FROM player inventory INTO vault
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                ItemStack moved = current;
                if (moved != null && vm.isBlacklisted(moved)) {
                    event.setCancelled(true);
                    player.sendMessage(cfg.getMessage("blacklisted-item"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (vm.getOpenVaultNumber(player) == null) return;

        // Read-only vault
        Integer vaultNum = vm.getOpenVaultNumber(player);
        if (vaultNum != null && vm.isReadOnly(player.getUniqueId(), vaultNum)) {
            event.setCancelled(true);
            return;
        }

        // Check if any slots being dragged are in the top inventory
        int topSize = event.getView().getTopInventory().getSize();
        boolean affectsVault = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);

        if (affectsVault && vm.isBlacklisted(event.getOldCursor())) {
            event.setCancelled(true);
            player.sendMessage(cfg.getMessage("blacklisted-item"));
        }
    }
}
