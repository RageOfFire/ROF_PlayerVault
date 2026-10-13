package me.rof_playervault.runnables;

import me.rof_playervault.ROF_PlayerVault;
import me.rof_playervault.utils.FuctionHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.List;

public class VaultPickUp extends BukkitRunnable {
    private final ROF_PlayerVault plugin;
    private final FuctionHandler fuctionHandler;
    public VaultPickUp(ROF_PlayerVault plugin) {
        this.plugin = plugin;
        this.fuctionHandler = new FuctionHandler(plugin);
    }
    @Override
    public void run() {
        long radius = plugin.getConfig().getLong("vault-pickup.radius");
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            int permissionStorage = fuctionHandler.getAmount(player);
            List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                if(entity instanceof Item) {
                    ItemStack item = ((Item) entity).getItemStack();
                    // Check if the player's inventory is full
                    if (isInventoryFull(player)) {
                        for (int page = 1;page <= permissionStorage;page++) {
                            ItemStack[] vaultContent;
                            try {
                                vaultContent = plugin.getVaultDatabase().getVaults(player, page);
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                            // Find the first empty slot in vault
                            int emptySlot = findEmptySlot(vaultContent, item);
                            // If there's an empty slot, add the item
                            if (emptySlot != -1) {
                                ItemStack existItem = vaultContent[emptySlot];
                                // If item not exist in that slot add new item
                                if(existItem == null) {
                                    vaultContent[emptySlot] = item;
                                    try {
                                        plugin.getVaultDatabase().updateVaults(player, vaultContent, page);
                                    } catch (SQLException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                    // Remove the item
                                    entity.remove();
                                }
                                else {
                                    int maxStackSize = existItem.getMaxStackSize();
                                    int amountAdd = Math.min(maxStackSize - existItem.getAmount(), item.getAmount());
                                    existItem.setAmount(existItem.getAmount() + amountAdd);
                                    vaultContent[emptySlot] = existItem;
                                    try {
                                        plugin.getVaultDatabase().updateVaults(player, vaultContent, page);
                                    } catch (SQLException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                    if(amountAdd < item.getAmount()) {
                                        ItemStack remainItems = item.clone();
                                        remainItems.setAmount(item.getAmount() - amountAdd);
                                        // Remove the item
                                        entity.remove();
                                        player.getWorld().dropItem(player.getLocation(), remainItems);
                                    }
                                }
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
