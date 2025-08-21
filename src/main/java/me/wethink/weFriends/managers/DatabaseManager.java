package me.wethink.weFriends.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.wethink.weFriends.WeFriends;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final WeFriends plugin;
    private HikariDataSource dataSource;
    private boolean usingMySql;

    public DatabaseManager(WeFriends plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        String type = plugin.getConfig().getString("database.type", "sqlite");
        usingMySql = type.equalsIgnoreCase("mysql");
        HikariConfig config = new HikariConfig();
        if (usingMySql) {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "wefriends");
            String user = plugin.getConfig().getString("database.mysql.username", "root");
            String pass = plugin.getConfig().getString("database.mysql.password", "");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
            config.setUsername(user);
            config.setPassword(pass);
        } else {
            String file = plugin.getDataFolder().toPath().resolve("wefriends.db").toString();
            config.setJdbcUrl("jdbc:sqlite:" + file);
        }
        config.setMaximumPoolSize(10);
        config.setPoolName("WeFriendsPool");
        this.dataSource = new HikariDataSource(config);
        initializeSchema();
    }

    private void initializeSchema() {
        try (Connection connection = getConnection()) {
            String friendsTable = "CREATE TABLE IF NOT EXISTS friends (" +
                    "user_uuid VARCHAR(36) NOT NULL," +
                    "friend_uuid VARCHAR(36) NOT NULL," +
                    "created_at BIGINT NOT NULL," +
                    "PRIMARY KEY (user_uuid, friend_uuid)" +
                    ")";
            connection.createStatement().executeUpdate(friendsTable);

            String requestsTable = "CREATE TABLE IF NOT EXISTS friend_requests (" +
                    "sender_uuid VARCHAR(36) NOT NULL," +
                    "target_uuid VARCHAR(36) NOT NULL," +
                    "created_at BIGINT NOT NULL," +
                    "PRIMARY KEY (sender_uuid, target_uuid)" +
                    ")";
            connection.createStatement().executeUpdate(requestsTable);

            String playersTable = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(16) NOT NULL," +
                    "requests_enabled BOOLEAN NOT NULL DEFAULT 1," +
                    "last_seen BIGINT NOT NULL DEFAULT 0" +
                    ")";
            connection.createStatement().executeUpdate(playersTable);

            String partiesTable = "CREATE TABLE IF NOT EXISTS parties (" +
                    "party_id VARCHAR(36) PRIMARY KEY," +
                    "leader_uuid VARCHAR(36) NOT NULL" +
                    ")";
            connection.createStatement().executeUpdate(partiesTable);

            String partyMembersTable = "CREATE TABLE IF NOT EXISTS party_members (" +
                    "party_id VARCHAR(36) NOT NULL," +
                    "member_uuid VARCHAR(36) NOT NULL," +
                    "role VARCHAR(16) NOT NULL," +
                    "PRIMARY KEY (party_id, member_uuid)" +
                    ")";
            connection.createStatement().executeUpdate(partyMembersTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database schema: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean isUsingMySql() {
        return usingMySql;
    }

    public String debugString() {
        return "DB{" + (usingMySql ? "MySQL" : "SQLite") + ", pool=" + (dataSource != null ? "up" : "down") + "}";
    }

    public void shutdown() {
        if (dataSource != null) dataSource.close();
    }
}


