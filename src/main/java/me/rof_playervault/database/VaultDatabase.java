package me.rof_playervault.database;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;

import static me.rof_playervault.utils.BukkitSerialization.itemStackArrayToBase64;
import static me.rof_playervault.utils.BukkitSerialization.itemStackArrayFromBase64;

public class VaultDatabase {
    private final Connection connection;

    public VaultDatabase (String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try(Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS playervaults (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid TEXT NOT NULL, " +
                    "username TEXT NOT NULL, " +
                    "pages INTEGER NOT NULL DEFAULT 1, " +
                    "vaults TEXT NOT NULL)");
        }
    }

    public void closeConnection() throws SQLException {
        if(connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void addVault(Player p, ItemStack[] items, int page) throws SQLException{
        try(PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO playervaults (uuid, username, pages, vaults) VALUES (?, ?, ?, ?)")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            preparedStatement.setString(2, p.getDisplayName());
            preparedStatement.setInt(3, page);
            preparedStatement.setString(4, itemStackArrayToBase64(items));
            preparedStatement.executeUpdate();
        }
    }
    public boolean playerExists(Player player, int page) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM playervaults WHERE uuid = ? AND pages = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.setInt(2, page);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    public void updateVaults(Player player, ItemStack[] items, int page) throws SQLException{

        //if the player doesn't exist, add them
        if (!playerExists(player, page)){
            addVault(player, items, page);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE playervaults SET vaults = ? WHERE uuid = ? AND pages = ?")) {
            preparedStatement.setString(1, itemStackArrayToBase64(items));
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setInt(3, page);
            preparedStatement.executeUpdate();
        }
    }

    public ItemStack[] getVaults(Player player, int page) throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT vaults FROM playervaults WHERE uuid = ? AND pages = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.setInt(2, page);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return itemStackArrayFromBase64(resultSet.getString("vaults"));
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
