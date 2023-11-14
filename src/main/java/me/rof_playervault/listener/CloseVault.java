package me.rof_playervault.listener;

import me.rof_playervault.ROF_PlayerVault;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class CloseVault implements Listener {
    private final ROF_PlayerVault plugin;
    public CloseVault(ROF_PlayerVault plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (e.getView().getTitle().equalsIgnoreCase("ROF Vault")) {
            File playerFile = new File(plugin.dataFolder, playerUUID.toString() + ".yml");
            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            // Save the player's vault contents to their file
            playerConfig.set("vault", player.getOpenInventory().getTopInventory().getContents());

            try {
                playerConfig.save(playerFile);
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }
}
