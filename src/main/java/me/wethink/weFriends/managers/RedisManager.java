package me.wethink.weFriends.managers;

import me.wethink.weFriends.WeFriends;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RedisManager {
    private final WeFriends plugin;
    private JedisPool pool;
    private String channel;
    private boolean enabled;
    private Thread pubSubThread;

    public RedisManager(WeFriends plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        enabled = plugin.getConfig().getBoolean("redis.enabled", false);
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
        plugin.getLogger().info("Redis connection established");
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
        String[] parts = message.split("\\|", 3);
        if (parts.length < 2) return;

        String type = parts[0];
        String data = parts[1];

        switch (type) {
            case "FRIEND_JOIN":
                handleFriendJoin(data);
                break;
            case "FRIEND_QUIT":
                handleFriendQuit(data);
                break;
            case "PARTY_UPDATE":
                handlePartyUpdate(data);
                break;
            case "FRIEND_CHAT":
                handleFriendChat(data);
                break;
            case "PARTY_CHAT":
                handlePartyChat(data);
                break;
        }
    }

    private void handleFriendJoin(String data) {
        plugin.getLogger().info("Cross-server friend join: " + data);
    }

    private void handleFriendQuit(String data) {
        plugin.getLogger().info("Cross-server friend quit: " + data);
    }

    private void handlePartyUpdate(String data) {
        plugin.getLogger().info("Cross-server party update: " + data);
    }

    private void handleFriendChat(String data) {
        plugin.getLogger().info("Cross-server friend chat: " + data);
    }

    private void handlePartyChat(String data) {
        plugin.getLogger().info("Cross-server party chat: " + data);
    }

    public void publish(String type, String data) {
        if (!enabled || pool == null) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String message = type + "|" + data;
                jedis.publish(channel, message);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis publish error: " + e.getMessage());
            }
        });
    }

    // Save/sync methods for cross-server data persistence
    public void saveFriendData(UUID playerUuid, String data) {
        if (!enabled || pool == null) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String key = "wefriends:friends:" + playerUuid.toString();
                jedis.setex(key, 3600, data); // Expire after 1 hour
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
                jedis.setex(key, 3600, data); // Expire after 1 hour
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

    public void shutdown() {
        if (pool != null) {
            pool.close();
        }
        if (pubSubThread != null && pubSubThread.isAlive()) {
            pubSubThread.interrupt();
        }
    }
}
