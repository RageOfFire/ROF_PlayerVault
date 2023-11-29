package me.rof_playervault.commands;

import me.rof_playervault.ROF_PlayerVault;
import me.rof_playervault.utils.isNumeric;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

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
        // No permission
        if(!commandSender.hasPermission("rofvault.use")) {
            commandSender.sendMessage("You don't have permission to use this");
            return true;
        }
        // Empty arguments
        if (strings.length == 0) {
            Inventory vault = Bukkit.createInventory(player, 54, "ROF Vault - #1");
            try {
                if (plugin.getVaultDatabase().playerExists(player, 1)) {
                    ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(player, 1);
                    vault.setContents(vaultContent);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            player.openInventory(vault);
            return true;
        }
        // arguments exist and it's number
        else if((isNumeric.isInteger(strings[0]))) {
            if(!commandSender.hasPermission("rofvault.slot." + strings[0])) {
                commandSender.sendMessage("You don't have enough permission to use this vault");
                return true;
            }
            Inventory vault = Bukkit.createInventory(player, 54, "ROF Vault - #" + strings[0]);
            try {
                if (plugin.getVaultDatabase().playerExists(player, Integer.parseInt(strings[0]))) {
                    ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(player, Integer.parseInt(strings[0]));
                    vault.setContents(vaultContent);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            player.openInventory(vault);
            return true;
        }
        return true;
    }
}
