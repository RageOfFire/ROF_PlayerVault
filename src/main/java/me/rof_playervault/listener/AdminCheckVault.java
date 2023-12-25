package me.rof_playervault.listener;

import me.rof_playervault.ROF_PlayerVault;
import me.rof_playervault.database.VaultHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class AdminCheckVault implements Listener {
    private final ROF_PlayerVault plugin;
    public AdminCheckVault(ROF_PlayerVault plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        ItemStack[] inventory = e.getInventory().getContents();
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder instanceof VaultHolder) {
            if (player.hasPermission("rofvault.admin.edit")) {
                try {
                    String[] number = e.getView().getTitle().split("\\#");
                    if(number.length > 1) {
                        String page = number[1].trim();
                        plugin.getVaultDatabase().updateVaults(player, inventory, Integer.parseInt(page));
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else {
                e.setCancelled(true);
            }
        }
    }
}
