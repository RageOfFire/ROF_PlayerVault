package me.rageoffire.rofplayervault;

import me.rageoffire.rofplayervault.commands.VaultCommand;
import me.rageoffire.rofplayervault.listeners.InventoryListener;
import me.rageoffire.rofplayervault.listeners.PickupListenerFactory;
import me.rageoffire.rofplayervault.managers.ConfigManager;
import me.rageoffire.rofplayervault.managers.EconomyManager;
import me.rageoffire.rofplayervault.managers.StorageManager;
import me.rageoffire.rofplayervault.managers.VaultManager;
import me.rageoffire.rofplayervault.placeholders.VaultPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ROFPlayerVault extends JavaPlugin {

    private static ROFPlayerVault instance;

    private ConfigManager configManager;
    private StorageManager storageManager;
    private VaultManager vaultManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        instance = this;

        // Config
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // Storage (FILE or MYSQL)
        this.storageManager = new StorageManager(this);
        if (!storageManager.init()) {
            getLogger().severe("Failed to initialise storage backend. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Vault manager (business logic)
        this.vaultManager = new VaultManager(this);

        // Economy (optional – requires Vault plugin)
        this.economyManager = new EconomyManager(this);
        economyManager.setup();

        // Commands
        VaultCommand vaultCommand = new VaultCommand(this);
        getCommand("rofvault").setExecutor(vaultCommand);
        getCommand("rofvault").setTabCompleter(vaultCommand);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);

        // Auto-pickup listener (only register if feature is enabled)
        if (configManager.isPickupEnabled()) {
            PickupListenerFactory.register(this);
            startPickupTask();
        }

        // PlaceholderAPI hook (optional)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VaultPlaceholders(this).register();
            getLogger().info("PlaceholderAPI hooked successfully.");
        }

        getLogger().info("ROF_PlayerVault v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // Flush all open vaults to storage before shutdown
        if (vaultManager != null) {
            vaultManager.saveAllOpenVaults();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        getLogger().info("ROF_PlayerVault disabled.");
    }

    // ── Reload ────────────────────────────────────────────────────────────────

    public void reload() {
        reloadConfig();
        configManager.reload();
        economyManager.setup();
        getLogger().info("ROF_PlayerVault reloaded.");
    }

    // ── Auto-pickup task ──────────────────────────────────────────────────────

    private void startPickupTask() {
        long intervalTicks = configManager.getPickupInterval() * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () ->
                vaultManager.runPickupCheck(), intervalTicks, intervalTicks);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static ROFPlayerVault getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
