package me.rof_playervault.listener;

import me.rof_playervault.ROF_PlayerVault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class AdminCheckVault implements Listener {
    private final ROF_PlayerVault plugin;
    public AdminCheckVault(ROF_PlayerVault plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Messages
        String vault_title = plugin.getConfig().getString("vault-title");
        Player player = (Player) e.getWhoClicked();
        ItemStack[] inventory = e.getInventory().getContents();
        if (e.getView().getTitle().contains("- " + vault_title)) {
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
//                player.sendMessage("You don't have enough permission to edit another vault");
            }
        }
    }
}
