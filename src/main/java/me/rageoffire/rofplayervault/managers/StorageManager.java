package me.rageoffire.rofplayervault.managers;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import me.rageoffire.rofplayervault.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

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
 *   type: FILE  – one YML-like file per player UUID under /plugins/ROF_PlayerVault/vaults/
 *   type: MYSQL – single table per vault slot; connection pool maintained here
 */
public class StorageManager {

    private final ROFPlayerVault plugin;
    private final String storageType;

    // FILE backend
    private File vaultsFolder;

    // MYSQL backend
    private final List<Connection> pool = new ArrayList<>();
    private final BlockingQueue<Connection> connectionQueue = new LinkedBlockingQueue<>();
    private String tablePrefix;

    // Shared async executor (single thread keeps I/O serial)
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
        if (storageType.equals("MYSQL")) {
            return initMySQL();
        } else {
            return initFile();
        }
    }

    private boolean initFile() {
        vaultsFolder = new File(plugin.getDataFolder(), "vaults");
        if (!vaultsFolder.exists()) {
            vaultsFolder.mkdirs();
        }
        plugin.getLogger().info("Storage backend: FILE (" + vaultsFolder.getPath() + ")");
        return true;
    }

    private boolean initMySQL() {
        ConfigManager cfg = plugin.getConfigManager();
        tablePrefix = cfg.getMySQLPrefix();
        String url = "jdbc:mysql://" + cfg.getMySQLHost() + ":" + cfg.getMySQLPort()
                + "/" + cfg.getMySQLDatabase() + "?" + cfg.getMySQLProps();

        try {
            // mysql-connector-j 9.x auto-registers via ServiceLoader — no Class.forName needed
            int poolSize = cfg.getMySQLPoolSize();
            for (int i = 0; i < poolSize; i++) {
                Connection conn = DriverManager.getConnection(url, cfg.getMySQLUser(), cfg.getMySQLPass());
                pool.add(conn);
                connectionQueue.add(conn);
            }
            createMySQLTable();
            plugin.getLogger().info("Storage backend: MYSQL (pool size " + poolSize + ")");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL: " + e.getMessage(), e);
            return false;
        }
    }

    private void createMySQLTable() throws SQLException {
        try (Connection conn = borrowConnection();
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
        } finally {
            // borrowConnection returns directly from pool – no release needed here
        }
    }

    // ── Public async API ──────────────────────────────────────────────────────

    /**
     * Asynchronously load a vault.  Calls {@code callback} on the main thread
     * with the loaded ItemStack array (null slots = empty).
     */
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

    /**
     * Asynchronously save a vault.  Fire-and-forget; errors are logged.
     */
    public void saveVaultAsync(UUID uuid, int vaultNumber, ItemStack[] contents) {
        // Copy the array so the live inventory can be modified freely
        final ItemStack[] snapshot = Arrays.copyOf(contents, contents.length);
        executor.submit(() -> {
            try {
                if (storageType.equals("MYSQL")) {
                    saveToMySQL(uuid, vaultNumber, snapshot);
                } else {
                    saveToFile(uuid, vaultNumber, snapshot);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving vault " + vaultNumber + " for " + uuid, e);
            }
        });
    }

    /**
     * Asynchronously delete all vaults for a player, returning all items
     * found so the caller can drop/mail them on the main thread.
     */
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

    /**
     * Synchronous save used only during onDisable (server is shutting down,
     * the async executor may be interrupted).
     */
    public void saveVaultSync(UUID uuid, int vaultNumber, ItemStack[] contents) {
        try {
            if (storageType.equals("MYSQL")) {
                saveToMySQL(uuid, vaultNumber, contents);
            } else {
                saveToFile(uuid, vaultNumber, contents);
            }
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

        // Read existing data first so we don't overwrite other vaults
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

        // Collect items before deleting
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
        Connection conn = borrowConnection();
        try {
            String sql = "SELECT `slot`, `item_data` FROM `" + tablePrefix + "vaults` " +
                         "WHERE `uuid` = ? AND `vault_number` = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, vaultNumber);
                ResultSet rs = ps.executeQuery();
                ItemStack[] result = new ItemStack[size];
                while (rs.next()) {
                    int slot = rs.getInt("slot");
                    String data = rs.getString("item_data");
                    if (slot >= 0 && slot < size && data != null && !data.isEmpty()) {
                        result[slot] = ItemSerializer.deserializeBase64(data);
                    }
                }
                return result;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    private void saveToMySQL(UUID uuid, int vaultNumber, ItemStack[] contents) throws Exception {
        Connection conn = borrowConnection();
        try {
            // Delete existing rows for this vault, then re-insert
            String del = "DELETE FROM `" + tablePrefix + "vaults` WHERE `uuid` = ? AND `vault_number` = ?";
            try (PreparedStatement ps = conn.prepareStatement(del)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, vaultNumber);
                ps.executeUpdate();
            }

            String ins = "INSERT INTO `" + tablePrefix + "vaults` (`uuid`, `vault_number`, `slot`, `item_data`) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null) {
                        ps.setString(1, uuid.toString());
                        ps.setInt(2, vaultNumber);
                        ps.setInt(3, i);
                        ps.setString(4, ItemSerializer.serializeBase64(contents[i]));
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        } finally {
            releaseConnection(conn);
        }
    }

    private ItemStack[] deleteFromMySQL(UUID uuid, int vaultNumber) throws Exception {
        // Load items first, then delete
        ItemStack[] items = loadFromMySQL(uuid, vaultNumber, 54);
        Connection conn = borrowConnection();
        try {
            String del = "DELETE FROM `" + tablePrefix + "vaults` WHERE `uuid` = ? AND `vault_number` = ?";
            try (PreparedStatement ps = conn.prepareStatement(del)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, vaultNumber);
                ps.executeUpdate();
            }
        } finally {
            releaseConnection(conn);
        }
        return items;
    }

    // ── Connection pool ───────────────────────────────────────────────────────

    private Connection borrowConnection() throws SQLException {
        try {
            Connection conn = connectionQueue.poll(5, TimeUnit.SECONDS);
            if (conn == null) throw new SQLException("Connection pool timeout");
            // Reconnect if stale
            if (!conn.isValid(2)) {
                ConfigManager cfg = plugin.getConfigManager();
                String url = "jdbc:mysql://" + cfg.getMySQLHost() + ":" + cfg.getMySQLPort()
                        + "/" + cfg.getMySQLDatabase() + "?" + cfg.getMySQLProps();
                conn = DriverManager.getConnection(url, cfg.getMySQLUser(), cfg.getMySQLPass());
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag
            throw new SQLException("Connection pool interrupted", e);
        }
    }

    private void releaseConnection(Connection conn) {
        if (conn != null) connectionQueue.offer(conn);
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        if (storageType.equals("MYSQL")) {
            for (Connection conn : pool) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }
}
