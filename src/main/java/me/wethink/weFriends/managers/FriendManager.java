package me.wethink.weFriends.managers;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FriendManager {

    private final WeFriends plugin;
    private final DatabaseManager db;

    private final Map<UUID, Boolean> requestsEnabled = new ConcurrentHashMap<>();

    public FriendManager(WeFriends plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void handleJoin(Player player) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO players(uuid,name,requests_enabled,last_seen) VALUES(?,?,1,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name")) {
                    ps.setString(1, player.getUniqueId().toString());
                    ps.setString(2, player.getName());
                    ps.setLong(3, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO players(uuid,name,requests_enabled,last_seen) VALUES(?,?,1,?) ON DUPLICATE KEY UPDATE name=VALUES(name)")) {
                        ps.setString(1, player.getUniqueId().toString());
                        ps.setString(2, player.getName());
                        ps.setLong(3, System.currentTimeMillis());
                        ps.executeUpdate();
                    } catch (SQLException ignored) {
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT requests_enabled FROM players WHERE uuid=?")) {
                    ps.setString(1, player.getUniqueId().toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) requestsEnabled.put(player.getUniqueId(), rs.getBoolean(1));
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("DB error on handleJoin: " + ex.getMessage());
            }
        });
        broadcastToFriends(player.getUniqueId(), "actionbar.friend_join", player.getName());
        if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
            plugin.getRedisManager().publishFriendJoin(player.getUniqueId(), player.getName());
        }
    }

    public void handleQuit(Player player) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE players SET last_seen=? WHERE uuid=?")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, player.getUniqueId().toString());
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("DB error on handleQuit: " + ex.getMessage());
            }
        });
        broadcastToFriends(player.getUniqueId(), "actionbar.friend_quit", player.getName());
        if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
            plugin.getRedisManager().publishFriendQuit(player.getUniqueId(), player.getName());
        }
    }

    private void broadcastToFriends(UUID playerUuid, String messageKey, String playerName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            Set<UUID> friendUuids = getFriendUuids(playerUuid);
            for (UUID friendUuid : friendUuids) {
                Player friend = Bukkit.getPlayer(friendUuid);
                if (friend != null && friend.isOnline()) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(friend, (entityTask) -> {
                        MessageUtil.actionbar(friend, messageKey, Map.of("player", playerName));
                    });
                }
            }
        });
    }

    public void sendRequest(UUID senderUuid, String senderName, OfflinePlayer target) {
        if (target == null || target.getUniqueId() == null) return;
        UUID targetUuid = target.getUniqueId();
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection()) {
                if (!isRequestsEnabled(targetUuid, c)) {
                    Player p = Bukkit.getPlayer(senderUuid);
                    if (p != null) {
                        WeFriends.getFoliaLib().getImpl().runAtEntity(p, (entityTask) -> {
                            MessageUtil.send(p, "friend.requests_off", Map.of("friend", target.getName()));
                        });
                    }
                    return;
                }
                if (areFriends(senderUuid, targetUuid, c)) {
                    Player p = Bukkit.getPlayer(senderUuid);
                    if (p != null) {
                        WeFriends.getFoliaLib().getImpl().runAtEntity(p, (entityTask) -> {
                            MessageUtil.send(p, "friend.already", Map.of("friend", target.getName()));
                        });
                    }
                    return;
                }
                try (PreparedStatement ins = c.prepareStatement("INSERT INTO friend_requests(sender_uuid,target_uuid,created_at) VALUES(?,?,?)")) {
                    ins.setString(1, senderUuid.toString());
                    ins.setString(2, targetUuid.toString());
                    ins.setLong(3, System.currentTimeMillis());
                    ins.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to send request: " + e.getMessage());
            }

            Player targetOnline = target.isOnline() ? target.getPlayer() : null;
            if (targetOnline != null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(targetOnline, (entityTask) -> {
                    MessageUtil.actionbar(targetOnline, "actionbar.friend_request", Map.of("player", senderName));
                    MessageUtil.send(targetOnline, "friend.request_received", Map.of("player", senderName));
                    targetOnline.playSound(targetOnline.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                });
            }
            Player sender = Bukkit.getPlayer(senderUuid);
            if (sender != null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(sender, (entityTask) -> {
                    MessageUtil.send(sender, "friend.request_sent", Map.of("friend", target.getName()));
                });
            }
        });
    }

    public void acceptRequest(UUID acceptorUuid, String fromName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection()) {
                UUID senderUuid = resolveUuidByName(fromName, c);
                if (senderUuid == null) return;
                try (PreparedStatement del = c.prepareStatement("DELETE FROM friend_requests WHERE sender_uuid=? AND target_uuid=?")) {
                    del.setString(1, senderUuid.toString());
                    del.setString(2, acceptorUuid.toString());
                    int removed = del.executeUpdate();
                    if (removed == 0) return;
                }
                linkFriends(senderUuid, acceptorUuid, c);

                Player a = Bukkit.getPlayer(acceptorUuid);
                if (a != null) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(a, (entityTask) -> {
                        MessageUtil.send(a, "friend.accepted", Map.of("friend", fromName));
                        a.playSound(a.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    });
                }
                Player s = Bukkit.getPlayer(senderUuid);
                if (s != null) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(s, (entityTask) -> {
                        MessageUtil.send(s, "friend.accepted_other", Map.of("friend", a != null ? a.getName() : ""));
                        s.playSound(s.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("acceptRequest error: " + e.getMessage());
            }
        });
    }

    public void denyRequest(UUID denierUuid, String fromName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection()) {
                UUID senderUuid = resolveUuidByName(fromName, c);
                if (senderUuid == null) return;
                try (PreparedStatement del = c.prepareStatement("DELETE FROM friend_requests WHERE sender_uuid=? AND target_uuid=?")) {
                    del.setString(1, senderUuid.toString());
                    del.setString(2, denierUuid.toString());
                    del.executeUpdate();
                }
                Player d = Bukkit.getPlayer(denierUuid);
                if (d != null) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(d, (entityTask) -> {
                        MessageUtil.send(d, "friend.denied", Map.of("friend", fromName));
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("denyRequest error: " + e.getMessage());
            }
        });
    }

    public void removeFriend(UUID playerUuid, String friendName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection()) {
                UUID friendUuid = resolveUuidByName(friendName, c);
                if (friendUuid == null) return;
                try (PreparedStatement del = c.prepareStatement("DELETE FROM friends WHERE (user_uuid=? AND friend_uuid=?) OR (user_uuid=? AND friend_uuid=?)")) {
                    del.setString(1, playerUuid.toString());
                    del.setString(2, friendUuid.toString());
                    del.setString(3, friendUuid.toString());
                    del.setString(4, playerUuid.toString());
                    del.executeUpdate();
                }
                Player p = Bukkit.getPlayer(playerUuid);
                if (p != null) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(p, (entityTask) -> {
                        MessageUtil.send(p, "friend.removed", Map.of("friend", friendName));
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("removeFriend error: " + e.getMessage());
            }
        });
    }

    public void sendFriendList(Player player) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            Set<UUID> friends = getFriendUuids(player.getUniqueId());
            int online = 0;
            List<String> onlineFriends = new ArrayList<>();
            List<String> offlineFriends = new ArrayList<>();

            for (UUID f : friends) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(f);
                boolean isOnline = op.isOnline();
                if (isOnline) {
                    online++;
                    onlineFriends.add(op.getName());
                } else {
                    offlineFriends.add(op.getName());
                }
            }

            final int finalOnline = online;
            WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
                MessageUtil.send(player, "friend.list_header", Map.of("online", String.valueOf(finalOnline), "total", String.valueOf(friends.size())));

                if (friends.isEmpty()) {
                    MessageUtil.send(player, "friend.list_empty");
                } else {
                    if (!onlineFriends.isEmpty()) {
                        MessageUtil.send(player, "friend.list_online", Map.of("friends", String.join(", ", onlineFriends)));
                    }
                    if (!offlineFriends.isEmpty()) {
                        MessageUtil.send(player, "friend.list_offline", Map.of("friends", String.join(", ", offlineFriends)));
                    }
                }
            });
        });
    }

    public void sendPendingRequests(Player player) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            List<String> requests = getPendingRequestNames(player.getUniqueId());
            WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
                if (requests.isEmpty()) {
                    MessageUtil.send(player, "friend.no_requests");
                } else {
                    MessageUtil.send(player, "friend.requests_header", Map.of("count", String.valueOf(requests.size())));
                    for (String name : requests) {
                        MessageUtil.send(player, "friend.request_item", Map.of("player", name));
                    }
                }
            });
        });
    }

    public void toggleRequests(UUID playerUuid, Player player) {
        boolean current = requestsEnabled.getOrDefault(playerUuid, true);
        boolean next = !current;
        requestsEnabled.put(playerUuid, next);
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE players SET requests_enabled=? WHERE uuid=?")) {
                ps.setBoolean(1, next);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("toggleRequests error: " + e.getMessage());
            }
        });
        WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
            MessageUtil.send(player, next ? "friend.toggle_on" : "friend.toggle_off");
        });
    }

    public void sendFriendChat(Player sender, String message) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            Set<UUID> friends = getFriendUuids(sender.getUniqueId());
            for (UUID f : friends) {
                Player fp = Bukkit.getPlayer(f);
                if (fp != null) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(fp, (entityTask) -> {
                        MessageUtil.send(fp, "chat.friend_format", Map.of("player", sender.getName(), "message", message));
                    });
                }
            }
            WeFriends.getFoliaLib().getImpl().runAtEntity(sender, (entityTask) -> {
                MessageUtil.send(sender, "chat.friend_format", Map.of("player", sender.getName(), "message", message));
                plugin.getSpyManager().broadcastFriendSpy(sender, message);
            });
        });
        if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
            plugin.getRedisManager().publishFriendChat(sender.getUniqueId(), sender.getName(), message);
        }
    }

    public void sendPrivateMessage(Player sender, String targetName, String message) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            try (Connection c = db.getConnection()) {
                UUID targetUuid = resolveUuidByName(targetName, c);
                if (targetUuid == null) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(sender, (entityTask) -> {
                        MessageUtil.send(sender, "error.player_not_found", Map.of("player", targetName));
                    });
                    return;
                }
                if (!areFriends(sender.getUniqueId(), targetUuid, c)) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(sender, (entityTask) -> {
                        MessageUtil.send(sender, "friend.not_friends", Map.of("friend", targetName));
                    });
                    return;
                }
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(target, (entityTask) -> {
                        MessageUtil.send(target, "chat.friend_msg_receive", Map.of("player", sender.getName(), "message", message));
                    });
                }
                WeFriends.getFoliaLib().getImpl().runAtEntity(sender, (entityTask) -> {
                    MessageUtil.send(sender, "chat.friend_msg_send", Map.of("friend", targetName, "message", message));
                    plugin.getSpyManager().broadcastFriendSpy(sender, "(to " + targetName + ") " + message);
                });
                if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
                    plugin.getRedisManager().publishFriendMessage(sender.getUniqueId(), sender.getName(), targetName, message);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("sendPrivateMessage error: " + e.getMessage());
            }
        });
    }

    private boolean isRequestsEnabled(UUID uuid, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT requests_enabled FROM players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return !rs.next() || rs.getBoolean(1);
        }
    }

    private boolean areFriends(UUID a, UUID b, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM friends WHERE (user_uuid=? AND friend_uuid=?) OR (user_uuid=? AND friend_uuid=?)")) {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ps.setString(3, b.toString());
            ps.setString(4, a.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public boolean areFriends(UUID a, UUID b) {
        try (Connection c = db.getConnection()) {
            return areFriends(a, b, c);
        } catch (SQLException e) {
            plugin.getLogger().warning("areFriends error: " + e.getMessage());
            return false;
        }
    }

    private void linkFriends(UUID a, UUID b, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO friends(user_uuid,friend_uuid,created_at) VALUES(?,?,?)")) {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO friends(user_uuid,friend_uuid,created_at) VALUES(?,?,?)")) {
            ps.setString(1, b.toString());
            ps.setString(2, a.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private UUID resolveUuidByName(String name, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid FROM players WHERE LOWER(name)=LOWER(?)")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UUID.fromString(rs.getString(1));
            return null;
        }
    }

    private Set<UUID> getFriendUuids(UUID uuid) {
        Set<UUID> out = new HashSet<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT friend_uuid FROM friends WHERE user_uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(UUID.fromString(rs.getString(1)));
        } catch (SQLException e) {
            plugin.getLogger().warning("getFriendUuids error: " + e.getMessage());
        }
        return out;
    }

    public Set<UUID> getFriendUuidsPublic(UUID uuid) {
        Set<UUID> out = new HashSet<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT friend_uuid FROM friends WHERE user_uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(UUID.fromString(rs.getString(1)));
        } catch (SQLException e) {
            plugin.getLogger().warning("getFriendUuids error: " + e.getMessage());
        }
        return out;
    }

    public long getLastSeen(String playerName) {
        try (Connection c = db.getConnection()) {
            UUID targetUuid = resolveUuidByName(playerName, c);
            if (targetUuid == null) return 0;

            try (PreparedStatement ps = c.prepareStatement("SELECT last_seen FROM players WHERE uuid=?")) {
                ps.setString(1, targetUuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getLastSeen error: " + e.getMessage());
        }
        return 0;
    }

    public List<String> getPendingRequestNames(UUID playerUuid) {
        List<String> names = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT sender_uuid FROM friend_requests WHERE target_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID senderUuid = UUID.fromString(rs.getString(1));
                String name = Bukkit.getOfflinePlayer(senderUuid).getName();
                if (name != null) names.add(name);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getPendingRequestNames error: " + e.getMessage());
        }
        return names;
    }

    public void shutdown() {
    }
}
