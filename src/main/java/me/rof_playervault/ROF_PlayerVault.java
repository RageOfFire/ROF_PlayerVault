package me.rof_playervault;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.rof_playervault.commands.VaultHandler;
import me.rof_playervault.commands.VaultTabComplete;
import me.rof_playervault.database.VaultDatabase;
import me.rof_playervault.listener.AdminCheckVault;
import me.rof_playervault.listener.BlacklistItems;
import me.rof_playervault.listener.CloseVault;
import me.rof_playervault.listener.VaultPickUp;
import me.rof_playervault.utils.FuctionHandler;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ROF_PlayerVault extends JavaPlugin {
    private VaultDatabase vaultDatabase;
    private FuctionHandler fuctionHandler;
    private final Set<Material> blacklistItems = new HashSet<>();
    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onEnable() {
        int pluginId = 20551;
        new Metrics(this, pluginId);
        fuctionHandler = new FuctionHandler(this);
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        //Setup Config
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        loadConfiguration();
        File configFile = new File(getDataFolder(), "config.yml");

        try {
            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        reloadConfig();
        getBlacklistItems();
        try {
            vaultDatabase = new VaultDatabase(getDataFolder().getAbsolutePath() + "/playervault.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // Listener
        getServer().getPluginManager().registerEvents(new CloseVault(this), this);
        getServer().getPluginManager().registerEvents(new AdminCheckVault(this), this);
        getServer().getPluginManager().registerEvents(new VaultPickUp(this), this);
        getServer().getPluginManager().registerEvents(new BlacklistItems(this), this);
        // Commands
        getCommand("rofvault").setExecutor(new VaultHandler(this));
        getCommand("rofvault").setTabCompleter(new VaultTabComplete());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            vaultDatabase.closeConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public VaultDatabase getVaultDatabase() {
        return vaultDatabase;
    }
    public void loadConfiguration() {
        List<String> blacklist = getConfig().getStringList("blacklist-items");
        for (String itemName : blacklist) {
            Material material = Material.matchMaterial(itemName);
            if (material != null) {
                blacklistItems.add(material);
            }
            else {
                getLogger().warning("Invalid material name: " + itemName);
                // You might want to log a warning or handle it in another way
            }
        }
    }
    public Set<Material> getBlacklistItems() {
        return blacklistItems;
    }
}
