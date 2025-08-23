package me.wethink.weFriends.managers;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RedisManager {
    private final WeFriends plugin;
    private JedisPool pool;
    private String channel;
    private String serverName;
    private boolean enabled;
    private boolean crossServerEnabled;
    private Thread pubSubThread;

    public RedisManager(WeFriends plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        enabled = plugin.getConfig().getBoolean("redis.enabled", false);
        crossServerEnabled = plugin.getConfig().getBoolean("cross-server.enabled", true);
        serverName = plugin.getConfig().getString("server.name", "unknown");
        
        if (!enabled) return;

        String host = plugin.getConfig().getString("redis.host", "localhost");
        int port = plugin.getConfig().getInt("redis.port", 6379);
        String password = plugin.getConfig().getString("redis.password", "");
        channel = plugin.getConfig().getString("redis.channel", "wefriends:events");

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setMinIdle(1);

        if (password.isEmpty()) {
            pool = new JedisPool(config, host, port);
        } else {
            pool = new JedisPool(config, host, port, 2000, password);
        }

        startPubSub();
        plugin.getLogger().info("Redis connection established for server: " + serverName);
    }

    private void startPubSub() {
        pubSubThread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handleMessage(message);
                    }
                }, channel);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis pub/sub error: " + e.getMessage());
            }
        });
        pubSubThread.setDaemon(true);
        pubSubThread.start();
    }

    private void handleMessage(String message) {
        String[] parts = message.split("\\|", 5);
        if (parts.length < 4) return;

        String sourceServer = parts[0];
        String type = parts[1];
        String playerUuid = parts[2];
        String playerName = parts[3];
        String extraData = parts.length > 4 ? parts[4] : "";

        if (sourceServer.equals(serverName)) return;

        switch (type) {
            case "FRIEND_JOIN":
                handleCrossServerFriendJoin(UUID.fromString(playerUuid), playerName, sourceServer);
                break;
            case "FRIEND_QUIT":
                handleCrossServerFriendQuit(UUID.fromString(playerUuid), playerName, sourceServer);
                break;
            case "FRIEND_CHAT":
                handleCrossServerFriendChat(UUID.fromString(playerUuid), playerName, extraData, sourceServer);
                break;
            case "FRIEND_MSG":
                handleCrossServerFriendMessage(UUID.fromString(playerUuid), playerName, extraData, sourceServer);
                break;
            case "PARTY_JOIN":
                handleCrossServerPartyJoin(UUID.fromString(playerUuid), playerName, extraData, sourceServer);
                break;
            case "PARTY_LEAVE":
                handleCrossServerPartyLeave(UUID.fromString(playerUuid), playerName, extraData, sourceServer);
                break;
            case "PARTY_CHAT":
                handleCrossServerPartyChat(UUID.fromString(playerUuid), playerName, extraData, sourceServer);
                break;
            case "PARTY_INVITE":
                handleCrossServerPartyInvite(UUID.fromString(playerUuid), playerName, extraData, sourceServer);
                break;
        }
    }

    private void handleCrossServerFriendJoin(UUID playerUuid, String playerName, String sourceServer) {
        if (!crossServerEnabled) return;
        
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            for (UUID friendUuid : plugin.getFriendManager().getFriendUuidsPublic(playerUuid)) {
                Player friend = Bukkit.getPlayer(friendUuid);
                if (friend != null && friend.isOnline()) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(friend, (entityTask) -> {
                        MessageUtil.actionbar(friend, "actionbar.friend_join", 
                            Map.of("player", playerName + " (" + sourceServer + ")"));
                    });
                }
            }
        });
    }

    private void handleCrossServerFriendQuit(UUID playerUuid, String playerName, String sourceServer) {
        if (!crossServerEnabled) return;
        
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            for (UUID friendUuid : plugin.getFriendManager().getFriendUuidsPublic(playerUuid)) {
                Player friend = Bukkit.getPlayer(friendUuid);
                if (friend != null && friend.isOnline()) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(friend, (entityTask) -> {
                        MessageUtil.actionbar(friend, "actionbar.friend_quit", 
                            Map.of("player", playerName + " (" + sourceServer + ")"));
                    });
                }
            }
        });
    }

    private void handleCrossServerFriendChat(UUID senderUuid, String senderName, String message, String sourceServer) {
        if (!crossServerEnabled) return;
        
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            for (UUID friendUuid : plugin.getFriendManager().getFriendUuidsPublic(senderUuid)) {
                Player friend = Bukkit.getPlayer(friendUuid);
                if (friend != null && friend.isOnline()) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(friend, (entityTask) -> {
                        MessageUtil.send(friend, "chat.friend_format", 
                            Map.of("player", senderName + " (" + sourceServer + ")", "message", message));
                    });
                }
            }
        });
    }

    private void handleCrossServerFriendMessage(UUID senderUuid, String senderName, String data, String sourceServer) {
        if (!crossServerEnabled) return;
        
        String[] msgParts = data.split(":", 2);
        if (msgParts.length != 2) return;
        
        String targetName = msgParts[0];
        String message = msgParts[1];
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null && target.isOnline()) {
            if (plugin.getFriendManager().areFriends(senderUuid, target.getUniqueId())) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(target, (entityTask) -> {
                    MessageUtil.send(target, "chat.friend_msg_receive", 
                        Map.of("player", senderName + " (" + sourceServer + ")", "message", message));
                });
            }
        }
    }

    private void handleCrossServerPartyJoin(UUID playerUuid, String playerName, String partyId, String sourceServer) {
        if (!crossServerEnabled) return;
        
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            for (UUID memberUuid : plugin.getPartyManager().getPartyMembers(partyId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline() && !memberUuid.equals(playerUuid)) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(member, (entityTask) -> {
                        MessageUtil.send(member, "party.member_joined", 
                            Map.of("player", playerName + " (" + sourceServer + ")"));
                    });
                }
            }
        });
    }

    private void handleCrossServerPartyLeave(UUID playerUuid, String playerName, String partyId, String sourceServer) {
        if (!crossServerEnabled) return;
        
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            for (UUID memberUuid : plugin.getPartyManager().getPartyMembers(partyId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(member, (entityTask) -> {
                        MessageUtil.send(member, "party.member_left", 
                            Map.of("player", playerName + " (" + sourceServer + ")"));
                    });
                }
            }
        });
    }

    private void handleCrossServerPartyChat(UUID senderUuid, String senderName, String data, String sourceServer) {
        if (!crossServerEnabled) return;
        
        String[] chatParts = data.split(":", 2);
        if (chatParts.length != 2) return;
        
        String partyId = chatParts[0];
        String message = chatParts[1];
        
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            for (UUID memberUuid : plugin.getPartyManager().getPartyMembers(partyId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    WeFriends.getFoliaLib().getImpl().runAtEntity(member, (entityTask) -> {
                        MessageUtil.send(member, "chat.party_format", 
                            Map.of("player", senderName + " (" + sourceServer + ")", "message", message));
                    });
                }
            }
        });
    }

    private void handleCrossServerPartyInvite(UUID inviterUuid, String inviterName, String targetName, String sourceServer) {
        if (!crossServerEnabled) return;
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null && target.isOnline()) {
            WeFriends.getFoliaLib().getImpl().runAtEntity(target, (entityTask) -> {
                MessageUtil.send(target, "party.invite_received", 
                    Map.of("player", inviterName + " (" + sourceServer + ")"));
                MessageUtil.actionbar(target, "actionbar.party_invite", 
                    Map.of("player", inviterName + " (" + sourceServer + ")"));
                target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            });
        }
    }

    public void publishFriendJoin(UUID playerUuid, String playerName) {
        if (!enabled || !crossServerEnabled) return;
        publish("FRIEND_JOIN", playerUuid.toString(), playerName, "");
    }

    public void publishFriendQuit(UUID playerUuid, String playerName) {
        if (!enabled || !crossServerEnabled) return;
        publish("FRIEND_QUIT", playerUuid.toString(), playerName, "");
    }

    public void publishFriendChat(UUID senderUuid, String senderName, String message) {
        if (!enabled || !crossServerEnabled) return;
        publish("FRIEND_CHAT", senderUuid.toString(), senderName, message);
    }

    public void publishFriendMessage(UUID senderUuid, String senderName, String targetName, String message) {
        if (!enabled || !crossServerEnabled) return;
        publish("FRIEND_MSG", senderUuid.toString(), senderName, targetName + ":" + message);
    }

    public void publishPartyJoin(UUID playerUuid, String playerName, String partyId) {
        if (!enabled || !crossServerEnabled) return;
        publish("PARTY_JOIN", playerUuid.toString(), playerName, partyId);
    }

    public void publishPartyLeave(UUID playerUuid, String playerName, String partyId) {
        if (!enabled || !crossServerEnabled) return;
        publish("PARTY_LEAVE", playerUuid.toString(), playerName, partyId);
    }

    public void publishPartyChat(UUID senderUuid, String senderName, String partyId, String message) {
        if (!enabled || !crossServerEnabled) return;
        publish("PARTY_CHAT", senderUuid.toString(), senderName, partyId + ":" + message);
    }

    public void publishPartyInvite(UUID inviterUuid, String inviterName, String targetName) {
        if (!enabled || !crossServerEnabled) return;
        publish("PARTY_INVITE", inviterUuid.toString(), inviterName, targetName);
    }

    private void publish(String type, String playerUuid, String playerName, String extraData) {
        if (!enabled || pool == null) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String message = serverName + "|" + type + "|" + playerUuid + "|" + playerName + "|" + extraData;
                jedis.publish(channel, message);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis publish error: " + e.getMessage());
            }
        });
    }

    public void saveFriendData(UUID playerUuid, String data) {
        if (!enabled || pool == null) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String key = "wefriends:friends:" + playerUuid.toString();
                jedis.setex(key, 3600, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis saveFriendData error: " + e.getMessage());
            }
        });
    }

    public void savePartyData(String partyId, String data) {
        if (!enabled || pool == null) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String key = "wefriends:party:" + partyId;
                jedis.setex(key, 3600, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis savePartyData error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<String> getFriendData(UUID playerUuid) {
        if (!enabled || pool == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String key = "wefriends:friends:" + playerUuid.toString();
                return jedis.get(key);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis getFriendData error: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<String> getPartyData(String partyId) {
        if (!enabled || pool == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String key = "wefriends:party:" + partyId;
                return jedis.get(key);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis getPartyData error: " + e.getMessage());
                return null;
            }
        });
    }

    public void removePlayerData(UUID playerUuid) {
        if (!enabled || pool == null) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String friendKey = "wefriends:friends:" + playerUuid.toString();
                jedis.del(friendKey);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis removePlayerData error: " + e.getMessage());
            }
        });
    }

    public void removePartyData(String partyId) {
        if (!enabled || pool == null) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String partyKey = "wefriends:party:" + partyId;
                jedis.del(partyKey);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis removePartyData error: " + e.getMessage());
            }
        });
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCrossServerEnabled() {
        return crossServerEnabled;
    }

    public String getServerName() {
        return serverName;
    }

    public void shutdown() {
        if (pool != null) {
            pool.close();
        }
        if (pubSubThread != null && pubSubThread.isAlive()) {
            pubSubThread.interrupt();
        }
    }
}
