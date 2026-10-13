package me.rageoffire.rofplayervault.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.managers.VaultManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registers PlaceholderAPI placeholders for ROF_PlayerVault.
 *
 * Available placeholders
 * ──────────────────────
 *  %rofvault_count%          – number of vaults the player has permission to use
 *  %rofvault_list%           – comma-separated list of accessible vault numbers
 *  %rofvault_has_<n>%        – "true"/"false" – whether player can open vault #n
 *  %rofvault_rows_<n>%       – resolved row count for vault #n (for this player)
 *  %rofvault_size_<n>%       – resolved slot count for vault #n (rows × 9)
 *  %rofvault_open%           – "true"/"false" – whether player currently has a vault open
 *  %rofvault_open_number%    – vault number currently open, or "" if none
 */
public class VaultPlaceholders extends PlaceholderExpansion {

    private final ROFPlayerVault plugin;
    private final VaultManager   vm;

    public VaultPlaceholders(ROFPlayerVault plugin) {
        this.plugin = plugin;
        this.vm     = plugin.getVaultManager();
    }

    @Override
    public @NotNull String getIdentifier() { return "rofvault"; }

    @Override
    public @NotNull String getAuthor()     { return "RageOfFire"; }

    @Override
    public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }   // keep registered across PAPI reloads

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // %rofvault_count%
        if (params.equalsIgnoreCase("count")) {
            return String.valueOf(vm.getAccessibleVaults(player).size());
        }

        // %rofvault_list%
        if (params.equalsIgnoreCase("list")) {
            List<Integer> vaults = vm.getAccessibleVaults(player);
            if (vaults.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < vaults.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("#").append(vaults.get(i));
            }
            return sb.toString();
        }

        // %rofvault_open%
        if (params.equalsIgnoreCase("open")) {
            return String.valueOf(vm.getOpenVaultNumber(player) != null);
        }

        // %rofvault_open_number%
        if (params.equalsIgnoreCase("open_number")) {
            Integer n = vm.getOpenVaultNumber(player);
            return n == null ? "" : String.valueOf(n);
        }

        // %rofvault_has_<n>%
        if (params.toLowerCase().startsWith("has_")) {
            String rest = params.substring(4);
            try {
                int n = Integer.parseInt(rest);
                return String.valueOf(player.hasPermission("rofvault.storage." + n));
            } catch (NumberFormatException e) {
                return "false";
            }
        }

        // %rofvault_rows_<n>%
        if (params.toLowerCase().startsWith("rows_")) {
            String rest = params.substring(5);
            try {
                int n = Integer.parseInt(rest);
                return String.valueOf(vm.resolveRows(player, n));
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        // %rofvault_size_<n>%
        if (params.toLowerCase().startsWith("size_")) {
            String rest = params.substring(5);
            try {
                int n = Integer.parseInt(rest);
                return String.valueOf(vm.resolveRows(player, n) * 9);
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        return null; // unrecognised placeholder
    }
}
