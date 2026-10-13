package me.rof_playervault.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class VaultTabComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        List<String> commandtab = new ArrayList<>();
        if (commandSender.hasPermission("rofvault.use")) {
            if (strings.length == 1) {
                commandtab.add("<number>");
            }
        }
        if (commandSender.hasPermission("rofvault.admin.reload")) {
            if (strings.length == 1) {
                commandtab.add("reload");
            }
        }
        if (commandSender.hasPermission("rofvault.admin.delete")) {
            if (strings.length == 1) {
                commandtab.add("delete");
            }
            if (strings.length == 2 && strings[0].equalsIgnoreCase("delete")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    commandtab.add(p.getName());
                }
            }
            if(strings.length == 3) {
                commandtab.add("<number>");
            }
        }
        if (commandSender.hasPermission("rofvault.admin.view") || commandSender.hasPermission("rofvault.admin.edit")) {
            if (strings.length == 1) {
                commandtab.add("open");
            }
            if (strings.length == 2 && strings[0].equalsIgnoreCase("open")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    commandtab.add(p.getName());
                }
            }
            if (strings.length == 3) {
                commandtab.add("<number>");
            }
        }
        return commandtab;
    }
}
