package me.rageoffire.rofplayervault.managers;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.utils.SoundHelper;
import me.rageoffire.rofplayervault.utils.TitleHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages open vaults, permission-based sizing, economy charges,
 * auto-pickup scanning, and vault lifecycle (open → save → close / delete).
 *
 * Key design decisions
 * ────────────────────
 *  • Vaults are identified by a composite key: "uuid:vaultNumber"
 *  • Loading is async (via StorageManager); the GUI opens only after the
 *    data arrives on the main thread to avoid inventory glitches.
 *  • Admin "view" vaults are read-only: the InventoryListener rejects changes.
 *  • On delete the items are either dropped at the player's feet or silently
 *    cleared, depending on the delete-action config key.
 */
public class VaultManager {

    private final ROFPlayerVault plugin;

    /** Tracks every currently-open vault inventory. key = "uuid:vaultNumber" */
    private final Map<String, Inventory> openVaults = new HashMap<>();

    /** Tracks which vault number each viewer currently has open. key = viewer UUID */
    private final Map<UUID, Integer> viewerVaultNumber = new HashMap<>();

    /** Tracks the owner UUID for admin-opened vaults. key = viewer UUID */
    private final Map<UUID, UUID> viewerVaultOwner = new HashMap<>();

    /** Vaults that admins opened in read-only (view) mode. key = "viewerUUID:vaultNumber" */
    private final Set<String> readOnlyVaults = new HashSet<>();

    public VaultManager(ROFPlayerVault plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPEN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the player's own vault.
     *
     * @param player      viewer / owner
     * @param vaultNumber which vault to open
     */
    public void openVault(Player player, int vaultNumber) {
        openVault(player, player, vaultNumber, false);
    }

    /**
     * Opens a vault, optionally in admin mode.
     *
     * @param viewer      the player who sees the inventory
     * @param owner       the player whose data is loaded
     * @param vaultNumber vault to open
     * @param readOnly    true = admin view (no edits allowed)
     */
    public void openVault(Player viewer, Player owner, int vaultNumber, boolean readOnly) {
        ConfigManager cfg = plugin.getConfigManager();
        EconomyManager eco = plugin.getEconomyManager();

        // ── Permission check ───────────────────────────────────────────────
        boolean isAdminOpen = !viewer.getUniqueId().equals(owner.getUniqueId());

        if (!isAdminOpen && !viewer.hasPermission("rofvault.storage." + vaultNumber)) {
            viewer.sendMessage(cfg.getMessage("vault-no-access")
                    .replace("%number%", String.valueOf(vaultNumber)));
            return;
        }

        // ── Economy check ──────────────────────────────────────────────────
        if (cfg.isEconomyEnabled() && !isAdminOpen &&
                !(cfg.isAdminEconBypass() && viewer.hasPermission("rofvault.admin"))) {
            double cost = cfg.getVaultCost(vaultNumber);
            if (cost > 0) {
                if (!eco.has(viewer, cost)) {
                    viewer.sendMessage(cfg.getMessage("economy-insufficient")
                            .replace("%cost%", eco.format(cost))
                            .replace("%number%", String.valueOf(vaultNumber)));
                    return;
                }
                eco.withdraw(viewer, cost);
                viewer.sendMessage(cfg.getMessage("economy-charged")
                        .replace("%cost%", eco.format(cost))
                        .replace("%number%", String.valueOf(vaultNumber)));
            }
        }

        // ── Determine size ─────────────────────────────────────────────────
        int rows = resolveRows(viewer, vaultNumber);
        int size = rows * 9;

        // ── Title ──────────────────────────────────────────────────────────
        String title = cfg.getVaultTitle().replace("%number%", String.valueOf(vaultNumber));

        // ── Load & open async ──────────────────────────────────────────────
        plugin.getStorageManager().loadVaultAsync(owner.getUniqueId(), vaultNumber, size, items -> {
            // Back on main thread
            if (!viewer.isOnline()) return;

            Inventory inv = Bukkit.createInventory(null, size, title);
            inv.setContents(items);

            String key = key(owner.getUniqueId(), vaultNumber);
            openVaults.put(key, inv);
            viewerVaultNumber.put(viewer.getUniqueId(), vaultNumber);
            viewerVaultOwner.put(viewer.getUniqueId(), owner.getUniqueId());

            if (readOnly) {
                readOnlyVaults.add(viewer.getUniqueId().toString() + ":" + vaultNumber);
            }

            viewer.openInventory(inv);

            // Sound
            SoundHelper.playOpenSound(viewer, cfg);

            // Title overlay
            if (cfg.isShowTitle()) {
                String t = cfg.getOpenTitle().replace("%number%", String.valueOf(vaultNumber));
                String s = cfg.getOpenSubtitle().replace("%number%", String.valueOf(vaultNumber));
                TitleHelper.sendTitle(viewer, t, s, cfg.getTitleFadeIn(), cfg.getTitleStay(), cfg.getTitleFadeOut());
            }

            // Admin feedback
            if (isAdminOpen) {
                String msgKey = readOnly ? "admin-viewing" : "admin-editing";
                viewer.sendMessage(cfg.getMessage(msgKey)
                        .replace("%number%", String.valueOf(vaultNumber))
                        .replace("%player%", owner.getName()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAVE / CLOSE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called by InventoryListener when a viewer closes their vault inventory.
     * Saves asynchronously unless it is a read-only admin view.
     */
    public void onVaultClose(Player viewer) {
        UUID viewerUUID = viewer.getUniqueId();
        Integer vaultNumber = viewerVaultNumber.remove(viewerUUID);
        UUID ownerUUID     = viewerVaultOwner.remove(viewerUUID);

        if (vaultNumber == null || ownerUUID == null) return;

        String roKey = viewerUUID.toString() + ":" + vaultNumber;
        boolean ro = readOnlyVaults.remove(roKey);

        String key = key(ownerUUID, vaultNumber);
        Inventory inv = openVaults.remove(key);

        if (inv == null || ro) return;  // nothing to save

        plugin.getStorageManager().saveVaultAsync(ownerUUID, vaultNumber, inv.getContents());
    }

    /** Flush all open vaults synchronously (called from onDisable). */
    public void saveAllOpenVaults() {
        for (Map.Entry<UUID, Integer> entry : viewerVaultNumber.entrySet()) {
            UUID viewerUUID  = entry.getKey();
            int  vaultNumber = entry.getValue();
            UUID ownerUUID   = viewerVaultOwner.getOrDefault(viewerUUID, viewerUUID);

            String roKey = viewerUUID.toString() + ":" + vaultNumber;
            if (readOnlyVaults.contains(roKey)) continue;

            String key = key(ownerUUID, vaultNumber);
            Inventory inv = openVaults.get(key);
            if (inv != null) {
                plugin.getStorageManager().saveVaultSync(ownerUUID, vaultNumber, inv.getContents());
            }
        }
        openVaults.clear();
        viewerVaultNumber.clear();
        viewerVaultOwner.clear();
        readOnlyVaults.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a vault and handles the items per the delete-action config.
     *
     * @param admin       the admin issuing the delete (for feedback messages)
     * @param targetUUID  owner of the vault
     * @param targetName  display name of the owner
     * @param vaultNumber which vault to delete
     */
    public void deleteVault(org.bukkit.command.CommandSender admin, UUID targetUUID, String targetName, int vaultNumber) {
        ConfigManager cfg = plugin.getConfigManager();

        // Force-close the vault if anyone has it open right now
        forceCloseVault(targetUUID, vaultNumber);

        plugin.getStorageManager().deleteVaultAsync(targetUUID, vaultNumber, items -> {
            admin.sendMessage(cfg.getMessage("vault-deleted")
                    .replace("%number%", String.valueOf(vaultNumber))
                    .replace("%player%", targetName));

            String action = cfg.getDeleteAction();
            List<ItemStack> toHandle = new ArrayList<>();
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    toHandle.add(item);
                }
            }

            if (toHandle.isEmpty()) return;

            if ("DROP".equals(action)) {
                // Try online player first, else world spawn
                Player target = Bukkit.getPlayer(targetUUID);
                Location dropLoc = (target != null && target.isOnline())
                        ? target.getLocation()
                        : Bukkit.getWorlds().get(0).getSpawnLocation();

                for (ItemStack item : toHandle) {
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                }
                admin.sendMessage(cfg.getMessage("vault-deleted-items-dropped"));
            }
            // CLEAR — do nothing, items are gone
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIST
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a list of vault numbers the given player has permission to use.
     */
    public List<Integer> getAccessibleVaults(Player player) {
        int max = plugin.getConfigManager().getMaxVaults();
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= max; i++) {
            if (player.hasPermission("rofvault.storage." + i)) {
                result.add(i);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AUTO-PICKUP
    // ─────────────────────────────────────────────────────────────────────────

    /** Scheduled task: scan all online players and move items to their vault. */
    public void runPickupCheck() {
        ConfigManager cfg = plugin.getConfigManager();
        int targetVault = cfg.getPickupVault();
        int radius      = cfg.getPickupRadius();
        boolean whitelist = "WHITELIST".equals(cfg.getPickupMode());
        List<String> filterItems = cfg.getPickupItems();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("rofvault.use")) continue;
            if (!player.hasPermission("rofvault.storage." + targetVault)) continue;
            if (!isInventoryFull(player)) continue;

            String key = key(player.getUniqueId(), targetVault);
            Inventory vaultInv = openVaults.get(key);
            if (vaultInv == null) continue; // vault must be open to receive items

            // Find nearby item entities
            player.getNearbyEntities(radius, radius, radius).stream()
                    .filter(e -> e instanceof org.bukkit.entity.Item)
                    .map(e -> (org.bukkit.entity.Item) e)
                    .forEach(dropped -> {
                        ItemStack item = dropped.getItemStack();
                        String matName = item.getType().name();
                        boolean listed = filterItems.contains(matName);
                        boolean shouldPickup = whitelist ? listed : !listed;

                        if (!shouldPickup) return;
                        if (isBlacklisted(item)) return;
                        if (vaultInv.firstEmpty() == -1) return;

                        vaultInv.addItem(item);
                        dropped.remove();
                    });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves how many rows the vault should have for this player + vault number.
     * Priority: per-vault config override → permission node → default-rows config.
     */
    public int resolveRows(Player player, int vaultNumber) {
        ConfigManager cfg = plugin.getConfigManager();

        // 1. Hard override in config
        int configRows = cfg.getPerVaultRows(vaultNumber);
        if (configRows != -1) return configRows;

        // 2. Highest matching rofvault.size.X permission
        for (int rows = 6; rows >= 1; rows--) {
            if (player.hasPermission("rofvault.size." + rows)) {
                return rows;
            }
        }

        // 3. Default
        return cfg.getDefaultRows();
    }

    public boolean isVaultOpen(UUID ownerUUID, int vaultNumber) {
        return openVaults.containsKey(key(ownerUUID, vaultNumber));
    }

    public boolean isReadOnly(UUID viewerUUID, int vaultNumber) {
        return readOnlyVaults.contains(viewerUUID.toString() + ":" + vaultNumber);
    }

    public boolean isBlacklisted(ItemStack item) {
        List<String> list = plugin.getConfigManager().getBlacklistedItems();
        return list.contains(item.getType().name());
    }

    /** Returns the owner UUID for the vault a viewer currently has open, or null. */
    public UUID getVaultOwner(Player viewer) {
        return viewerVaultOwner.get(viewer.getUniqueId());
    }

    /** Returns the vault number a viewer currently has open, or null. */
    public Integer getOpenVaultNumber(Player viewer) {
        return viewerVaultNumber.get(viewer.getUniqueId());
    }

    private void forceCloseVault(UUID ownerUUID, int vaultNumber) {
        // Close the inventory for anyone who has it open
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(viewerVaultOwner).entrySet()) {
            if (entry.getValue().equals(ownerUUID)) {
                Integer vn = viewerVaultNumber.get(entry.getKey());
                if (vn != null && vn == vaultNumber) {
                    Player viewer = Bukkit.getPlayer(entry.getKey());
                    if (viewer != null) viewer.closeInventory();
                }
            }
        }
    }

    /**
     * Directly adds a single item to an already-open vault inventory.
     * Called by PickupListener when a player's main inventory is full.
     *
     * @return true if the item was fully accepted, false if the vault is full
     */
    public boolean redirectItemToVault(Player player, int vaultNumber, ItemStack item) {
        String key = key(player.getUniqueId(), vaultNumber);
        Inventory inv = openVaults.get(key);
        if (inv == null) return false;
        if (inv.firstEmpty() == -1) return false;

        Map<Integer, ItemStack> leftover = inv.addItem(item.clone());
        return leftover.isEmpty();  // true = all items added
    }

    private boolean isInventoryFull(Player player) {
        return player.getInventory().firstEmpty() == -1;
    }

    private String key(UUID uuid, int vaultNumber) {
        return uuid.toString() + ":" + vaultNumber;
    }
}
