package me.rageoffire.rofplayervault.managers;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optional Vault economy integration.
 * All methods are safe to call even when Vault is not installed;
 * they simply return sane defaults (has → true, withdraw → no-op).
 */
public class EconomyManager {

    private final ROFPlayerVault plugin;
    private Economy economy;

    public EconomyManager(ROFPlayerVault plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        economy = null;
        if (!plugin.getConfigManager().isEconomyEnabled()) return;

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Economy is enabled in config but Vault is not installed.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Vault is installed but no economy provider was found.");
            return;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Economy hooked: " + economy.getName());
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        if (!isAvailable()) return true;
        return economy.has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        if (!isAvailable()) return;
        economy.withdrawPlayer(player, amount);
    }

    public String format(double amount) {
        if (!isAvailable()) return String.valueOf(amount);
        return economy.format(amount);
    }
}
