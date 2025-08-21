package me.wethink.weFriends.listeners;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.managers.FriendManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final WeFriends plugin;

    public PlayerConnectionListener(WeFriends plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FriendManager fm = plugin.getFriendManager();
        fm.handleJoin(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FriendManager fm = plugin.getFriendManager();
        fm.handleQuit(player);
    }
}


