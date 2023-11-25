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
        // empty arguments
        if (strings.length == 0) {
            Inventory vault = Bukkit.createInventory(player, 54, "ROF Vault - #1");
            try {
                if (plugin.getVaultDatabase().playerExists(player)) {
                    ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(player, 1);
                    vault.setContents(vaultContent);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            player.openInventory(vault);
        }
        // arguments exist and it's number
        if((isNumeric.isInteger(strings[0]))) {
            Inventory vault = Bukkit.createInventory(player, 54, "ROF Vault - #" + strings[0]);
            try {
                if (plugin.getVaultDatabase().playerExists(player)) {
                    ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(player, Integer.parseInt(strings[0]));
                    vault.setContents(vaultContent);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            player.openInventory(vault);
        }
        return true;
    }
}
