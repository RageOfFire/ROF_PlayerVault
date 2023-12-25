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
        if(commandSender.hasPermission("rofvault.use")) {
            if (strings.length == 0) {
                commandtab.add("<number>");
            }
        }
        if(commandSender.hasPermission("rofvault.admin.reload")) {
            if (strings.length == 0) {
                commandtab.add("reload");
            }
        }
        if(commandSender.hasPermission("rofvault.admin.delete")) {
            if (strings.length == 0) {
                commandtab.add("delete");
            }
            if(strings[1].equalsIgnoreCase("delete")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    commandtab.add(p.getName());
                }
                if (!strings[2].isEmpty()) {
                    commandtab.add("<number>");
                }
            }
        }
        if(commandSender.hasPermission("rofvault.admin.view") || commandSender.hasPermission("rofvault.admin.edit")) {
            if (strings.length == 0) {
                commandtab.add("open");
            }
            if(strings[1].equalsIgnoreCase("open")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    commandtab.add(p.getName());
                }
                if (!strings[2].isEmpty()) {
                    commandtab.add("<number>");
                }
            }
        }
        return commandtab;
    }
}
