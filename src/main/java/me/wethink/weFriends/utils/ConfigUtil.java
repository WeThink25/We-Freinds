package me.wethink.weFriends.utils;

import me.wethink.weFriends.WeFriends;

public final class ConfigUtil {

    private static int maxFriends;
    private static int maxPartySize;

    private ConfigUtil() {
    }

    public static void load(WeFriends plugin) {
        plugin.saveDefaultConfig();
        maxFriends = plugin.getConfig().getInt("limits.max-friends", 200);
        maxPartySize = plugin.getConfig().getInt("limits.max-party-size", 8);
    }

    public static int getMaxFriends() {
        return maxFriends;
    }

    public static int getMaxPartySize() {
        return maxPartySize;
    }
}


