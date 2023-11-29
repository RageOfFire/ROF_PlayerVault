package me.rof_playervault.listener;

import me.rof_playervault.ROF_PlayerVault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.SQLException;
import java.util.Arrays;

public class CloseVault implements Listener {
    private final ROF_PlayerVault plugin;
    public CloseVault(ROF_PlayerVault plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        ItemStack[] inventory = e.getInventory().getContents();
        if (e.getView().getTitle().contains("ROF Vault")) {
            try {
                String[] number = e.getView().getTitle().split("\\#");
                if(number.length > 1) {
                    String page = number[1].trim();
                    plugin.getVaultDatabase().updateVaults(player, inventory, Integer.parseInt(page));
                }
                else {
                    plugin.getVaultDatabase().updateVaults(player, inventory, 1);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
