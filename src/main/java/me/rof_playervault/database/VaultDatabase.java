package me.rof_playervault.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;

public class VaultDatabase {
    private final Connection connection;
    public final Gson gson = new Gson();

    public VaultDatabase (String path) throws SQLException {
//        try {
//            Class.forName("org.sqlite.JDBC");
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        File sqlite = new File(path);
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try(Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS playervaults (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "username TEXT NOT NULL, " +
                    "pages INTEGER NOT NULL DEFAULT 1, " +
                    "vaults TEXT NOT NULL)");
        }
    }
    public String serializeVault(ArrayList<ItemStack> items) {
        return gson.toJson(items);
    }

    public ArrayList<ItemStack> deserializeVault(String json) {
        return gson.fromJson(json, new TypeToken<ArrayList<ItemStack>>(){}.getType());
    }

    public void closeConnection() throws SQLException {
        if(connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void addVault(Player p, ArrayList<ItemStack> items) throws SQLException{
        try(PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO playervaults (uuid, username, pages, vaults) VALUES (?, ?, ?, ?)")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            preparedStatement.setString(2, p.getDisplayName());
            preparedStatement.setInt(3, 1);
            preparedStatement.setString(4, serializeVault(items));
            preparedStatement.executeUpdate();
        }
    }
    public boolean playerExists(Player player) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM playervaults WHERE uuid = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    public void updateVaults(Player player, ArrayList<ItemStack> items, int page) throws SQLException{

        //if the player doesn't exist, add them
        if (!playerExists(player)){
            addVault(player, items);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE playervaults SET vaults = ? WHERE uuid = ? AND pages = ?")) {
            preparedStatement.setString(1, serializeVault(items));
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setInt(3, page);
            preparedStatement.executeUpdate();
        }
    }

    public ArrayList<ItemStack> getVaults(Player player, int page) throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT vaults FROM playervaults WHERE uuid = ? AND pages = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.setInt(2, page);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return deserializeVault(resultSet.getString("vaults"));
            } else {
                return null;
            }
        }
    }
}
