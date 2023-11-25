package me.rof_playervault;

import me.rof_playervault.commands.OpenVault;
import me.rof_playervault.database.VaultDatabase;
import me.rof_playervault.listener.CloseVault;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class ROF_PlayerVault extends JavaPlugin {
    private VaultDatabase vaultDatabase;
    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        try {
            vaultDatabase = new VaultDatabase(getDataFolder().getAbsolutePath() + "/playervault.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        getServer().getPluginManager().registerEvents(new CloseVault(this), this);
        getCommand("rofvault").setExecutor(new OpenVault(this));
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
