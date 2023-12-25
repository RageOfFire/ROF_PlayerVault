package me.rof_playervault.listener;

import me.rof_playervault.ROF_PlayerVault;
import me.rof_playervault.utils.FuctionHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class VaultPickUp implements Listener {
    private final ROF_PlayerVault plugin;
    private final FuctionHandler fuctionHandler;
    public VaultPickUp(ROF_PlayerVault plugin) {
        this.plugin = plugin;
        this.fuctionHandler = new FuctionHandler(plugin);
    }
    @EventHandler
    public void onItemPickUp(PlayerPickupItemEvent e) throws SQLException {
        Player player = e.getPlayer();
        ItemStack item = e.getItem().getItemStack();
        int permissionStorage = fuctionHandler.getAmount(player);

        // If this option is allow
        if(plugin.getConfig().getBoolean("vault-pickup")) {
            // Check if the player's inventory is full
            if (isInventoryFull(player)) {
                for (int page = 1;page <= permissionStorage;page++) {
                    ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(player, page);
                    // Find the first empty slot in vault
                    int emptySlot = findEmptySlot(vaultContent, item);
                    // If there's an empty slot, add the item
                    if (emptySlot != -1) {
                        ItemStack existItem = vaultContent[emptySlot];
                        // If item not exist in that slot add new item
                        if(existItem == null) {
                            vaultContent[emptySlot] = item;
                            plugin.getVaultDatabase().updateVaults(player, vaultContent, page);
                            // Cancel the event to prevent picking up the item
                            e.setCancelled(true);
                            e.getItem().remove();
                            break;
                        }
                        else {
                            int maxStackSize = existItem.getMaxStackSize();
                            int amountAdd = Math.min(maxStackSize - existItem.getAmount(), item.getAmount());
                            existItem.setAmount(existItem.getAmount() + amountAdd);
                            vaultContent[emptySlot] = existItem;
                            plugin.getVaultDatabase().updateVaults(player, vaultContent, page);
                            if(amountAdd < item.getAmount()) {
                                ItemStack remainItems = item.clone();
                                remainItems.setAmount(item.getAmount() - amountAdd);
                                // Cancel the event to prevent picking up the item
                                e.setCancelled(true);
                                e.getItem().remove();
                                player.getWorld().dropItem(player.getLocation(), remainItems);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    private boolean isInventoryFull(Player player) {
        return player.getInventory().firstEmpty() == -1;
    }

    private int findEmptySlot(ItemStack[] inventory, ItemStack item) {
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null || inventory[i].getType() == Material.AIR) {
                return i;
            }
            if (inventory[i].isSimilar(item) && inventory[i].getAmount() < inventory[i].getMaxStackSize()) {
                return i;
            }
        }
        return -1; // Return -1 if no empty slot is found
    }
}
