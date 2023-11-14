package me.rof_playervault.commands;

import me.rof_playervault.ROF_PlayerVault;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class OpenVault implements CommandExecutor {
    private final ROF_PlayerVault plugin;
    public OpenVault(ROF_PlayerVault plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only player can execute this bro");
            return true;
        }
        Player player = (Player) commandSender;
        UUID playerUUID = player.getUniqueId();

        File playerFile = new File(plugin.dataFolder, playerUUID.toString() + ".yml");
        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        Inventory vault = Bukkit.createInventory(player, 54, "ROF Vault");
        if (playerConfig.contains("vault")) {
            // Load the player's vault contents from the player's file
            vault.setContents(playerConfig.getList("vault").toArray(new org.bukkit.inventory.ItemStack[0]));
        }
        player.openInventory(vault);
        return true;
    }
}
