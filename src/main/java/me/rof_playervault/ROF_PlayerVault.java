package me.rof_playervault;

import me.rof_playervault.commands.VaultHandler;
import me.rof_playervault.database.VaultDatabase;
import me.rof_playervault.listener.AdminCheckVault;
import me.rof_playervault.listener.CloseVault;
import me.rof_playervault.utils.FuctionHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class ROF_PlayerVault extends JavaPlugin {
    private VaultDatabase vaultDatabase;
    private FuctionHandler fuctionHandler;
    @Override
    public void onEnable() {
        //
        fuctionHandler = new FuctionHandler(this);
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        //Setup Config
        getConfig().options().copyDefaults();
        saveDefaultConfig();
//        File configFile = new File(getDataFolder(), "config.yml");
//
//        try {
//            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        loadConfiguration();
        reloadConfig();
        try {
            vaultDatabase = new VaultDatabase(getDataFolder().getAbsolutePath() + "/playervault.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // Listener
        getServer().getPluginManager().registerEvents(new CloseVault(this), this);
        getServer().getPluginManager().registerEvents(new AdminCheckVault(this), this);
        // Commands
        getCommand("rofvault").setExecutor(new VaultHandler(this));
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
}
