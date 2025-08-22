package me.wethink.weFriends.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.wethink.weFriends.WeFriends;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

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
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS friends (" +
                            "user_uuid VARCHAR(36) NOT NULL," +
                            "friend_uuid VARCHAR(36) NOT NULL," +
                            "created_at BIGINT NOT NULL," +
                            "PRIMARY KEY (user_uuid, friend_uuid)" +
                            ")");

            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS friend_requests (" +
                            "sender_uuid VARCHAR(36) NOT NULL," +
                            "target_uuid VARCHAR(36) NOT NULL," +
                            "created_at BIGINT NOT NULL," +
                            "PRIMARY KEY (sender_uuid, target_uuid)" +
                            ")");

            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "uuid VARCHAR(36) PRIMARY KEY," +
                            "name VARCHAR(16) NOT NULL," +
                            "requests_enabled BOOLEAN NOT NULL DEFAULT 1," +
                            "last_seen BIGINT NOT NULL DEFAULT 0" +
                            ")");

            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS parties (" +
                            "id VARCHAR(36) PRIMARY KEY," +
                            "leader_uuid VARCHAR(36) NOT NULL," +
                            "created_at BIGINT NOT NULL" +
                            ")");

            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS party_members (" +
                            "party_id VARCHAR(36) NOT NULL," +
                            "player_uuid VARCHAR(36) NOT NULL," +
                            "role VARCHAR(16) NOT NULL," +
                            "joined_at BIGINT NOT NULL," +
                            "PRIMARY KEY (party_id, player_uuid)" +
                            ")");

            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS party_invites (" +
                            "party_id VARCHAR(36) NOT NULL," +
                            "inviter_uuid VARCHAR(36) NOT NULL," +
                            "invitee_uuid VARCHAR(36) NOT NULL," +
                            "created_at BIGINT NOT NULL," +
                            "PRIMARY KEY (party_id, invitee_uuid)" +
                            ")");
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

    public void createParty(UUID partyId, UUID leaderId) {
        String sql = "INSERT INTO parties (id, leader_uuid, created_at) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partyId.toString());
            ps.setString(2, leaderId.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("createParty error: " + e.getMessage());
        }
    }

    public void deleteParty(UUID partyId) {
        String sql = "DELETE FROM parties WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partyId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("deleteParty error: " + e.getMessage());
        }
    }

    public void addPartyMember(UUID partyId, UUID memberId, String role) {
        String sql = "INSERT INTO party_members (party_id, player_uuid, role, joined_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partyId.toString());
            ps.setString(2, memberId.toString());
            ps.setString(3, role);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("addPartyMember error: " + e.getMessage());
        }
    }

    public void removePartyMember(UUID partyId, UUID memberId) {
        String sql = "DELETE FROM party_members WHERE party_id=? AND player_uuid=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partyId.toString());
            ps.setString(2, memberId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("removePartyMember error: " + e.getMessage());
        }
    }

    public void updatePartyMemberRole(UUID partyId, UUID memberId, String role) {
        String sql = "UPDATE party_members SET role=? WHERE party_id=? AND player_uuid=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, partyId.toString());
            ps.setString(3, memberId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("updatePartyMemberRole error: " + e.getMessage());
        }
    }

    public void addPartyInvite(UUID partyId, UUID inviterUuid, UUID inviteeUuid, long createdAt) {
        String sql = generateUpsertPartyInviteSql();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partyId.toString());
            ps.setString(2, inviterUuid.toString());
            ps.setString(3, inviteeUuid.toString());
            ps.setLong(4, createdAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("addPartyInvite error: " + e.getMessage());
        }
    }

    private String generateUpsertPartyInviteSql() {
        if (isUsingMySql()) {
            return "INSERT INTO party_invites (party_id, inviter_uuid, invitee_uuid, created_at) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE created_at=VALUES(created_at)";
        } else {
            return "INSERT INTO party_invites (party_id, inviter_uuid, invitee_uuid, created_at) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(party_id, invitee_uuid) DO UPDATE SET created_at=excluded.created_at";
        }
    }

    public void removePartyInvite(UUID inviteeUuid) {
        String sql = "DELETE FROM party_invites WHERE invitee_uuid=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inviteeUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("removePartyInvite error: " + e.getMessage());
        }
    }

    public void updatePartyLeader(UUID partyId, UUID newLeaderUuid) {
        String sql = "UPDATE parties SET leader_uuid=? WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newLeaderUuid.toString());
            ps.setString(2, partyId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("updatePartyLeader error: " + e.getMessage());
        }
    }
}
