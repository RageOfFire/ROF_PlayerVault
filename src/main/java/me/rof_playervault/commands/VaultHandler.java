package me.rof_playervault.commands;

import me.rof_playervault.ROF_PlayerVault;
import me.rof_playervault.database.VaultHolder;
import me.rof_playervault.utils.FuctionHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class VaultHandler implements CommandExecutor {
    private final ROF_PlayerVault plugin;
    private final FuctionHandler fuctionHandler;
    public VaultHandler(ROF_PlayerVault plugin) {
        this.plugin = plugin;
        this.fuctionHandler = new FuctionHandler(plugin);
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        // Only player can use
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only player can execute this bro");
            return true;
        }
        Player player = (Player) commandSender;

        // Messages
        String vault_title = plugin.getConfig().getString("vault-title");
        String reload_message = plugin.getConfig().getString("messages.command-reload");
        String no_permission_message = plugin.getConfig().getString("messages.no-permission");
        String usage_main = plugin.getConfig().getString("messages.usage-main");

        // No permission
        if(!commandSender.hasPermission("rofvault.use")) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', no_permission_message));
            return true;
        }

        // Empty arguments
        if (strings.length == 0) {
            Inventory vault = Bukkit.createInventory(new VaultHolder(), 54, vault_title + " - #1");
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
        if(fuctionHandler.isInteger(strings[0])) {
            // Define number
            int vaultNumber = Integer.parseInt(strings[0]);
            int permissionStorage = fuctionHandler.getAmount(player);
            Inventory vault = Bukkit.createInventory(new VaultHolder(), 54, vault_title + " - #" + strings[0]);
            // vault number 1 is valid
            if(vaultNumber == 1) {
                try {
                    if (plugin.getVaultDatabase().playerExists(player, vaultNumber)) {
                        ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(player, vaultNumber);
                        vault.setContents(vaultContent);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                player.openInventory(vault);
                return true;
            }
            // Check if player have permission to use that vault or not
            if(vaultNumber > permissionStorage) {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', no_permission_message));
                return true;
            }
            try {
                if (plugin.getVaultDatabase().playerExists(player, vaultNumber)) {
                    ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(player, vaultNumber);
                    vault.setContents(vaultContent);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            player.openInventory(vault);
            return true;
        }
        else {
            // Message with placeholder
            Player target = fuctionHandler.checkPlayer(strings[1]);
            int page = Integer.parseInt(strings[2]);
            String delete_message = plugin.getConfig().getString("messages.command-delete")
                    .replace("%player%", target.getName())
                    .replace("%number%", String.valueOf(page));
            // Start command usage admin
            switch (strings[0]) {
                case "reload":
                    if(commandSender.hasPermission("rofvault.admin.reload")) {
                        plugin.reloadConfig();
                        plugin.loadConfiguration();
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', reload_message));
                    }
                    else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', no_permission_message));
                    }
                    break;
                case "delete":
                    if(commandSender.hasPermission("rofvault.admin.delete")) {
                        try {
                            plugin.getVaultDatabase().deleteVaults(target, page);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', delete_message));
                    }
                    else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', no_permission_message));
                    }
                    break;
                case "open":
                    if(commandSender.hasPermission("rofvault.admin.view") || commandSender.hasPermission("rofvault.admin.edit")) {
                        Inventory vaultadmin = Bukkit.createInventory(new VaultHolder(), 54, target.getDisplayName() + " - "+ vault_title + " - #" + strings[2]);
                        try {
                            if (plugin.getVaultDatabase().playerExists(target, page)) {
                                ItemStack[] vaultContent = plugin.getVaultDatabase().getVaults(target, page);
                                vaultadmin.setContents(vaultContent);
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        player.openInventory(vaultadmin);
                    }
                    else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', no_permission_message));
                    }
                    break;
                default:
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', usage_main));
            }
        }
        return true;
    }
}
