package me.rof_playervault;

import me.rof_playervault.commands.OpenVault;
import me.rof_playervault.database.VaultDatabase;
import me.rof_playervault.listener.CloseVault;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class ROF_PlayerVault extends JavaPlugin {
    public File dataFolder;
    @Override
    public void onEnable() {
        // Plugin startup logic
        dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        getServer().getPluginManager().registerEvents(new CloseVault(this), this);
        getCommand("rofvault").setExecutor(new OpenVault(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            File playerFile = new File(dataFolder, playerUUID.toString() + ".yml");
            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            // Save the player's vault contents to their file
            playerConfig.set("vault", player.getOpenInventory().getTopInventory().getContents());

            try {
                playerConfig.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
