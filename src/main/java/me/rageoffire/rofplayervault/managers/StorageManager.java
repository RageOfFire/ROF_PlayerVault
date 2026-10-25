package me.rageoffire.rofplayervault.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Handles all async reading and writing of vault data.
 *
 * Backend is chosen via config:
 *   type: FILE  – one binary file per player UUID under /plugins/ROF_PlayerVault/vaults/
 *   type: MYSQL – HikariCP connection pool; MySQL driver must be on the server classpath
 *                 (Spigot/Paper 1.16+ bundles it; older servers need the jar in /libs/)
 *
 * All public methods are async — they submit work to a single-threaded executor
 * and invoke the callback on the main thread when done.
 */
public class StorageManager {

    private final ROFPlayerVault plugin;
    private final String storageType;

    // FILE backend
    private File vaultsFolder;

    // MYSQL backend — HikariCP manages the connection pool
    private HikariDataSource dataSource;
    private String tablePrefix;

    // Single background thread for all I/O — keeps operations serial and off the main thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ROFVault-IO");
        t.setDaemon(true);
        return t;
    });

    public StorageManager(ROFPlayerVault plugin) {
        this.plugin = plugin;
        this.storageType = plugin.getConfigManager().getStorageType();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    public boolean init() {
        return storageType.equals("MYSQL") ? initMySQL() : initFile();
    }

    private boolean initFile() {
        vaultsFolder = new File(plugin.getDataFolder(), "vaults");
        if (!vaultsFolder.exists()) vaultsFolder.mkdirs();
        plugin.getLogger().info("Storage backend: FILE (" + vaultsFolder.getPath() + ")");
        return true;
    }

    private boolean initMySQL() {
        ConfigManager cfg = plugin.getConfigManager();
        tablePrefix = cfg.getMySQLPrefix();

        // Ensure MySQL driver is on the classpath — download if needed
        File libsFolder = new File(plugin.getDataFolder(), "libs");
        if (!me.rageoffire.rofplayervault.utils.MySQLDriverLoader.ensureDriver(libsFolder, plugin.getLogger())) {
            plugin.getLogger().severe("Cannot initialise MySQL — driver unavailable.");
            return false;
        }

        try {
            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl("jdbc:mysql://" + cfg.getMySQLHost() + ":" + cfg.getMySQLPort()
                    + "/" + cfg.getMySQLDatabase() + "?" + cfg.getMySQLProps());
            hikari.setUsername(cfg.getMySQLUser());
            hikari.setPassword(cfg.getMySQLPass());
            hikari.setMaximumPoolSize(cfg.getMySQLPoolSize());
            hikari.setMinimumIdle(1);
            hikari.setConnectionTimeout(10_000);
            hikari.setIdleTimeout(600_000);
            hikari.setMaxLifetime(1_800_000);
            hikari.setPoolName("ROFVault-Pool");

            // Recommended settings for MySQL
            hikari.addDataSourceProperty("cachePrepStmts",          "true");
            hikari.addDataSourceProperty("prepStmtCacheSize",        "250");
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");
            hikari.addDataSourceProperty("useServerPrepStmts",       "true");

            dataSource = new HikariDataSource(hikari);
            createMySQLTable();
            plugin.getLogger().info("Storage backend: MYSQL (pool size " + cfg.getMySQLPoolSize() + ")");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL: " + e.getMessage(), e);
            return false;
        }
    }

    private void createMySQLTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "vaults` (" +
                "  `uuid`         VARCHAR(36)  NOT NULL," +
                "  `vault_number` INT          NOT NULL," +
                "  `slot`         INT          NOT NULL," +
                "  `item_data`    MEDIUMTEXT," +
                "  PRIMARY KEY (`uuid`, `vault_number`, `slot`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
        }
    }

    // ── Public async API ──────────────────────────────────────────────────────

    public void loadVaultAsync(UUID uuid, int vaultNumber, int size, Consumer<ItemStack[]> callback) {
        executor.submit(() -> {
            ItemStack[] items;
            try {
                items = storageType.equals("MYSQL")
                        ? loadFromMySQL(uuid, vaultNumber, size)
                        : loadFromFile(uuid, vaultNumber, size);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading vault " + vaultNumber + " for " + uuid, e);
                items = new ItemStack[size];
            }
            final ItemStack[] result = items;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void saveVaultAsync(UUID uuid, int vaultNumber, ItemStack[] contents) {
        final ItemStack[] snapshot = Arrays.copyOf(contents, contents.length);
        executor.submit(() -> {
            try {
                if (storageType.equals("MYSQL")) saveToMySQL(uuid, vaultNumber, snapshot);
                else                              saveToFile(uuid, vaultNumber, snapshot);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving vault " + vaultNumber + " for " + uuid, e);
            }
        });
    }

    public void deleteVaultAsync(UUID uuid, int vaultNumber, Consumer<ItemStack[]> callback) {
        executor.submit(() -> {
            ItemStack[] items;
            try {
                items = storageType.equals("MYSQL")
                        ? deleteFromMySQL(uuid, vaultNumber)
                        : deleteFromFile(uuid, vaultNumber);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting vault " + vaultNumber + " for " + uuid, e);
                items = new ItemStack[0];
            }
            final ItemStack[] result = items;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    /** Synchronous save used only during onDisable. */
    public void saveVaultSync(UUID uuid, int vaultNumber, ItemStack[] contents) {
        try {
            if (storageType.equals("MYSQL")) saveToMySQL(uuid, vaultNumber, contents);
            else                              saveToFile(uuid, vaultNumber, contents);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving vault " + vaultNumber + " for " + uuid + " on shutdown", e);
        }
    }

    // ── FILE implementation ───────────────────────────────────────────────────

    private File playerFile(UUID uuid) {
        return new File(vaultsFolder, uuid.toString() + ".dat");
    }

    @SuppressWarnings("unchecked")
    private ItemStack[] loadFromFile(UUID uuid, int vaultNumber, int size) throws Exception {
        File file = playerFile(uuid);
        if (!file.exists()) return new ItemStack[size];

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<Integer, Map<Integer, byte[]>> data = (Map<Integer, Map<Integer, byte[]>>) ois.readObject();
            Map<Integer, byte[]> vaultData = data.getOrDefault(vaultNumber, Collections.emptyMap());
            ItemStack[] result = new ItemStack[size];
            for (Map.Entry<Integer, byte[]> entry : vaultData.entrySet()) {
                int slot = entry.getKey();
                if (slot >= 0 && slot < size) {
                    result[slot] = ItemSerializer.deserialize(entry.getValue());
                }
            }
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private void saveToFile(UUID uuid, int vaultNumber, ItemStack[] contents) throws Exception {
        File file = playerFile(uuid);
        Map<Integer, Map<Integer, byte[]>> data = new HashMap<>();
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                data = (Map<Integer, Map<Integer, byte[]>>) ois.readObject();
            }
        }
        Map<Integer, byte[]> vaultData = new HashMap<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                vaultData.put(i, ItemSerializer.serialize(contents[i]));
            }
        }
        data.put(vaultNumber, vaultData);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
        }
    }

    @SuppressWarnings("unchecked")
    private ItemStack[] deleteFromFile(UUID uuid, int vaultNumber) throws Exception {
        File file = playerFile(uuid);
        if (!file.exists()) return new ItemStack[0];

        Map<Integer, Map<Integer, byte[]>> data;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            data = (Map<Integer, Map<Integer, byte[]>>) ois.readObject();
        }
        Map<Integer, byte[]> vaultData = data.remove(vaultNumber);
        if (vaultData == null) return new ItemStack[0];

        List<ItemStack> items = new ArrayList<>();
        for (byte[] bytes : vaultData.values()) {
            ItemStack item = ItemSerializer.deserialize(bytes);
            if (item != null) items.add(item);
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
        }
        return items.toArray(new ItemStack[0]);
    }

    // ── MYSQL implementation ──────────────────────────────────────────────────

    private ItemStack[] loadFromMySQL(UUID uuid, int vaultNumber, int size) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT `slot`, `item_data` FROM `" + tablePrefix + "vaults` " +
                "WHERE `uuid` = ? AND `vault_number` = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, vaultNumber);
            ResultSet rs = ps.executeQuery();
            ItemStack[] result = new ItemStack[size];
            while (rs.next()) {
                int slot    = rs.getInt("slot");
                String data = rs.getString("item_data");
                if (slot >= 0 && slot < size && data != null && !data.isEmpty()) {
                    result[slot] = ItemSerializer.deserializeBase64(data);
                }
            }
            return result;
        }
    }

    private void saveToMySQL(UUID uuid, int vaultNumber, ItemStack[] contents) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM `" + tablePrefix + "vaults` WHERE `uuid` = ? AND `vault_number` = ?")) {
                del.setString(1, uuid.toString());
                del.setInt(2, vaultNumber);
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO `" + tablePrefix + "vaults` (`uuid`, `vault_number`, `slot`, `item_data`) VALUES (?,?,?,?)")) {
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null) {
                        ins.setString(1, uuid.toString());
                        ins.setInt(2, vaultNumber);
                        ins.setInt(3, i);
                        ins.setString(4, ItemSerializer.serializeBase64(contents[i]));
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }
        }
    }

    private ItemStack[] deleteFromMySQL(UUID uuid, int vaultNumber) throws Exception {
        ItemStack[] items = loadFromMySQL(uuid, vaultNumber, 54);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement del = conn.prepareStatement(
                "DELETE FROM `" + tablePrefix + "vaults` WHERE `uuid` = ? AND `vault_number` = ?")) {
            del.setString(1, uuid.toString());
            del.setInt(2, vaultNumber);
            del.executeUpdate();
        }
        return items;
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
