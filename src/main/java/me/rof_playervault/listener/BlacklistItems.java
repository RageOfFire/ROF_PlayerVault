package me.rof_playervault.listener;

import me.rof_playervault.ROF_PlayerVault;
import me.rof_playervault.database.VaultHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static org.bukkit.Bukkit.getLogger;

public class BlacklistItems implements Listener {
    private final ROF_PlayerVault plugin;
    public BlacklistItems( ROF_PlayerVault plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPutItems(InventoryClickEvent e) {
        ItemStack currentItems = e.getCurrentItem();
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder instanceof VaultHolder) {
            if(currentItems != null && plugin.getBlacklistItems().contains(currentItems.getType())) {
                e.setCancelled(true);
            }
        }
    }
}
