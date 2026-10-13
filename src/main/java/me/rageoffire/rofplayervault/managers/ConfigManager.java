package me.rageoffire.rofplayervault.managers;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final ROFPlayerVault plugin;
    private FileConfiguration cfg;

    public ConfigManager(ROFPlayerVault plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        this.cfg = plugin.getConfig();
    }

    // ── General ───────────────────────────────────────────────────────────────

    public String getVaultTitle() {
        return color(cfg.getString("vault-title", "&6ROF Vault &f#%number%"));
    }

    public int getMaxVaults() {
        return cfg.getInt("max-vaults", 10);
    }

    // ── Vault size ────────────────────────────────────────────────────────────

    public int getDefaultRows() {
        return clamp(cfg.getInt("vault-size.default-rows", 3), 1, 6);
    }

    /**
     * Returns the per-vault row override from config, or -1 if not set.
     * The caller should fall back to the permission-based size when -1 is returned.
     */
    public int getPerVaultRows(int vaultNumber) {
        String path = "vault-size.per-vault." + vaultNumber;
        if (cfg.contains(path)) {
            return clamp(cfg.getInt(path), 1, 6);
        }
        return -1;
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    public String getStorageType() {
        return cfg.getString("storage.type", "FILE").toUpperCase();
    }

    public String getMySQLHost()     { return cfg.getString("storage.mysql.host",     "localhost"); }
    public int    getMySQLPort()     { return cfg.getInt   ("storage.mysql.port",     3306); }
    public String getMySQLDatabase() { return cfg.getString("storage.mysql.database", "minecraft"); }
    public String getMySQLUser()     { return cfg.getString("storage.mysql.username", "root"); }
    public String getMySQLPass()     { return cfg.getString("storage.mysql.password", ""); }
    public int    getMySQLPoolSize() { return cfg.getInt   ("storage.mysql.pool-size", 5); }
    public String getMySQLPrefix()   { return cfg.getString("storage.mysql.table-prefix", "rofvault_"); }
    public String getMySQLProps()    { return cfg.getString("storage.mysql.properties", "useSSL=false&autoReconnect=true"); }

    // ── Auto-pickup ───────────────────────────────────────────────────────────

    public boolean isPickupEnabled()   { return cfg.getBoolean("vault-pickup.enable", false); }
    public int     getPickupVault()    { return cfg.getInt("vault-pickup.target-vault", 1); }
    public int     getPickupInterval() { return Math.max(1, cfg.getInt("vault-pickup.interval", 3)); }
    public int     getPickupRadius()   { return cfg.getInt("vault-pickup.radius", 3); }
    public String  getPickupMode()     { return cfg.getString("vault-pickup.mode", "BLACKLIST").toUpperCase(); }
    public List<String> getPickupItems() { return cfg.getStringList("vault-pickup.items"); }

    // ── Blacklist ─────────────────────────────────────────────────────────────

    public List<String> getBlacklistedItems() {
        return cfg.getStringList("blacklist-items");
    }

    // ── Sound & title ─────────────────────────────────────────────────────────

    public String  getOpenSound()      { return cfg.getString("vault-open.sound", "BLOCK_CHEST_OPEN"); }
    public float   getOpenVolume()     { return (float) cfg.getDouble("vault-open.volume", 1.0); }
    public float   getOpenPitch()      { return (float) cfg.getDouble("vault-open.pitch", 1.0); }
    public boolean isShowTitle()       { return cfg.getBoolean("vault-open.show-title", false); }
    public String  getOpenTitle()      { return color(cfg.getString("vault-open.title", "&6ROF Vault &f#%number%")); }
    public String  getOpenSubtitle()   { return color(cfg.getString("vault-open.subtitle", "&7Your personal storage")); }
    public int     getTitleFadeIn()    { return cfg.getInt("vault-open.title-fade-in", 10); }
    public int     getTitleStay()      { return cfg.getInt("vault-open.title-stay", 40); }
    public int     getTitleFadeOut()   { return cfg.getInt("vault-open.title-fade-out", 10); }

    // ── Economy ───────────────────────────────────────────────────────────────

    public boolean isEconomyEnabled()  { return cfg.getBoolean("economy.enable", false); }
    public boolean isAdminEconBypass() { return cfg.getBoolean("economy.admin-bypass", true); }

    public double getVaultCost(int vaultNumber) {
        String path = "economy.cost-per-open." + vaultNumber;
        return cfg.contains(path) ? cfg.getDouble(path) : 0.0;
    }

    // ── Delete action ─────────────────────────────────────────────────────────

    public String getDeleteAction() {
        return cfg.getString("delete-action", "DROP").toUpperCase();
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public String getMessage(String key) {
        String prefix = color(cfg.getString("messages.prefix", "&f[&6ROF_PlayerVault&f] "));
        String msg    = cfg.getString("messages." + key, "&cMessage not found: " + key);
        return prefix + color(msg);
    }

    public String getRawMessage(String key) {
        return color(cfg.getString("messages." + key, "&cMessage not found: " + key));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String color(String s) {
        return s == null ? "" : org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
