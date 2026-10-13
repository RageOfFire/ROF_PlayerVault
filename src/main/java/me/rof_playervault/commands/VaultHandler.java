package me.rof_playervault.commands;

import me.rof_playervault.ROF_PlayerVault;
import me.rof_playervault.database.AdminVaultHolder;
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
    private Player target;
    private int page;
    public VaultHandler(ROF_PlayerVault plugin) {
        this.plugin = plugin;
        this.fuctionHandler = new FuctionHandler(plugin);
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        // Messages
        String vault_title = plugin.getConfig().getString("vault-title");
        String prefix = plugin.getConfig().getString("messages.prefix");
        String only_player = plugin.getConfig().getString("messages.only-player");
        String reload_message = plugin.getConfig().getString("messages.command-reload");
        String no_permission_message = plugin.getConfig().getString("messages.no-permission");
        String usage_main = plugin.getConfig().getString("messages.usage-main");

        // No permission
        if(!commandSender.hasPermission("rofvault.use")) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + no_permission_message));
            return true;
        }

        // Empty arguments
        if (strings.length == 0) {
            // Only player can use
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + only_player));
                return true;
            }

            Player player = (Player) commandSender;
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
            // Only player can use
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + only_player));
                return true;
            }

            Player player = (Player) commandSender;
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
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + no_permission_message));
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
            // Start command usage admin
            switch (strings[0]) {
                case "reload":
                    if(commandSender.hasPermission("rofvault.admin.reload")) {
                        plugin.reloadConfig();
                        plugin.loadConfiguration();
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + reload_message));
                    }
                    else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + no_permission_message));
                    }
                    break;
                case "delete":
                    target = fuctionHandler.checkPlayer(strings[1]);
                    page = Integer.parseInt(strings[2]);
                    String delete_message = plugin.getConfig().getString("messages.command-delete")
                            .replace("%player%", target.getName())
                            .replace("%number%", String.valueOf(page));
                    if(commandSender.hasPermission("rofvault.admin.delete")) {
                        try {
                            plugin.getVaultDatabase().deleteVaults(target, page);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + delete_message));
                    }
                    else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + no_permission_message));
                    }
                    break;
                case "open":
                    // Only player can use
                    if(!(commandSender instanceof Player)) {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + only_player));
                        return true;
                    }

                    Player player = (Player) commandSender;
                    target = fuctionHandler.checkPlayer(strings[1]);
                    page = Integer.parseInt(strings[2]);
                    if(commandSender.hasPermission("rofvault.admin.view") || commandSender.hasPermission("rofvault.admin.edit")) {
                        Inventory vaultadmin = Bukkit.createInventory(new AdminVaultHolder(), 54, target.getDisplayName() + " - "+ vault_title + " - #" + strings[2]);
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
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + no_permission_message));
                    }
                    break;
                default:
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix + "&r " + usage_main));
            }
        }
        return true;
    }
}
