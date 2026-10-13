package me.rageoffire.rofplayervault.commands;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.managers.ConfigManager;
import me.rageoffire.rofplayervault.managers.VaultManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all /rofvault (alias: /vault /pv) subcommands.
 *
 * Sub-commands
 * ─────────────
 *  /rofvault open [vault#]                   – open own vault
 *  /rofvault open <player> [vault#]          – admin: open another player's vault
 *  /rofvault list                            – list own accessible vaults
 *  /rofvault delete <player> [vault#]        – admin: delete a vault
 *  /rofvault reload                          – admin: reload config
 *  /rofvault help                            – show help
 */
public class VaultCommand implements CommandExecutor, TabCompleter {

    private final ROFPlayerVault plugin;
    private final ConfigManager  cfg;
    private final VaultManager   vm;

    public VaultCommand(ROFPlayerVault plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getConfigManager();
        this.vm     = plugin.getVaultManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            // Default: open vault #1 for the player, or show help for console
            if (!(sender instanceof Player)) {
                sendHelp(sender, label);
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("rofvault.use")) {
                player.sendMessage(cfg.getMessage("no-permission"));
                return true;
            }
            openOwnVault(player, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            // ── open ──────────────────────────────────────────────────────────
            case "open":
                return handleOpen(sender, args, label);

            // ── list ──────────────────────────────────────────────────────────
            case "list":
                return handleList(sender);

            // ── delete ────────────────────────────────────────────────────────
            case "delete":
                return handleDelete(sender, args);

            // ── reload ────────────────────────────────────────────────────────
            case "reload":
                if (!sender.hasPermission("rofvault.admin.reload")) {
                    sender.sendMessage(cfg.getMessage("no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(cfg.getMessage("command-reload"));
                return true;

            // ── help / default ────────────────────────────────────────────────
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    // ── /rofvault open ────────────────────────────────────────────────────────

    private boolean handleOpen(CommandSender sender, String[] args, String label) {
        // /rofvault open [vault#]  → own vault
        // /rofvault open <player> [vault#]  → admin open

        if (!(sender instanceof Player)) {
            sender.sendMessage(cfg.getMessage("only-player"));
            return true;
        }
        Player viewer = (Player) sender;

        if (!viewer.hasPermission("rofvault.use")) {
            viewer.sendMessage(cfg.getMessage("no-permission"));
            return true;
        }

        // Determine if second arg is a number (own vault) or player name (admin)
        if (args.length == 1) {
            // /rofvault open  → vault 1
            openOwnVault(viewer, 1);
            return true;
        }

        // Try to parse args[1] as a vault number first
        Integer vaultNumDirect = tryParseInt(args[1]);
        if (vaultNumDirect != null) {
            // /rofvault open <number>
            if (!isValidVaultNumber(viewer, vaultNumDirect)) return true;
            openOwnVault(viewer, vaultNumDirect);
            return true;
        }

        // args[1] is a player name → admin open
        if (!viewer.hasPermission("rofvault.admin.open")) {
            viewer.sendMessage(cfg.getMessage("no-permission"));
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            viewer.sendMessage(cfg.getMessage("player-not-found")
                    .replace("%player%", targetName));
            return true;
        }

        int vaultNum = 1;
        if (args.length >= 3) {
            Integer parsed = tryParseInt(args[2]);
            if (parsed == null || !isValidVaultNumber(viewer, parsed)) return true;
            vaultNum = parsed;
        }

        // Admin open (read-write)
        boolean readOnly = !viewer.hasPermission("rofvault.admin.edit");
        vm.openVault(viewer, target, vaultNum, readOnly);
        return true;
    }

    // ── /rofvault list ────────────────────────────────────────────────────────

    private boolean handleList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(cfg.getMessage("only-player"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("rofvault.list")) {
            player.sendMessage(cfg.getMessage("no-permission"));
            return true;
        }

        List<Integer> vaults = vm.getAccessibleVaults(player);
        if (vaults.isEmpty()) {
            player.sendMessage(cfg.getMessage("vault-list-empty"));
        } else {
            String list = vaults.stream()
                    .map(n -> "#" + n)
                    .collect(Collectors.joining(", "));
            player.sendMessage(cfg.getMessage("vault-list").replace("%vaults%", list));
        }
        return true;
    }

    // ── /rofvault delete ──────────────────────────────────────────────────────

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rofvault.admin.delete")) {
            sender.sendMessage(cfg.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(cfg.getMessage("usage-delete"));
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID;

        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // Try offline player
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (!offline.hasPlayedBefore()) {
                sender.sendMessage(cfg.getMessage("player-not-found").replace("%player%", targetName));
                return true;
            }
            targetUUID = offline.getUniqueId();
        }

        int vaultNum = 1;
        if (args.length >= 3) {
            Integer parsed = tryParseInt(args[2]);
            if (parsed == null) {
                sender.sendMessage(cfg.getMessage("invalid-vault-number")
                        .replace("%max%", String.valueOf(cfg.getMaxVaults())));
                return true;
            }
            vaultNum = parsed;
        }

        // Works for both console and player — VaultManager accepts CommandSender
        vm.deleteVault(sender, targetUUID, targetName, vaultNum);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openOwnVault(Player player, int vaultNumber) {
        vm.openVault(player, vaultNumber);
    }

    private boolean isValidVaultNumber(Player player, int n) {
        int max = cfg.getMaxVaults();
        if (n < 1 || n > max) {
            player.sendMessage(cfg.getMessage("invalid-vault-number").replace("%max%", String.valueOf(max)));
            return false;
        }
        return true;
    }

    private Integer tryParseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private void sendHelp(CommandSender sender, String label) {
        String c = "§6";
        String w = "§f";
        String g = "§7";
        sender.sendMessage(c + "=== ROF_PlayerVault Help ===");
        sender.sendMessage(c + "/" + label + " open [#]"            + g + " - Open your vault");
        sender.sendMessage(c + "/" + label + " open <player> [#]"   + g + " - [Admin] Open a player's vault");
        sender.sendMessage(c + "/" + label + " list"                + g + " - List your accessible vaults");
        sender.sendMessage(c + "/" + label + " delete <player> [#]" + g + " - [Admin] Delete a player's vault");
        sender.sendMessage(c + "/" + label + " reload"              + g + " - [Admin] Reload config");
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("open", "list", "help"));
            if (sender.hasPermission("rofvault.admin.delete")) subs.add("delete");
            if (sender.hasPermission("rofvault.admin.reload")) subs.add("reload");
            filterPrefix(completions, subs, args[0]);
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open") && sender.hasPermission("rofvault.admin.open")) {
                // Suggest online player names
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .forEach(completions::add);
            }
            if (sub.equals("delete") && sender.hasPermission("rofvault.admin.delete")) {
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .forEach(completions::add);
            }
            return completions;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open") || sub.equals("delete")) {
                // Suggest vault numbers
                int max = cfg.getMaxVaults();
                for (int i = 1; i <= max; i++) {
                    String s = String.valueOf(i);
                    if (s.startsWith(args[2])) completions.add(s);
                }
            }
            return completions;
        }

        return completions;
    }

    private void filterPrefix(List<String> out, List<String> options, String prefix) {
        for (String s : options) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) out.add(s);
        }
    }
}
