package me.wethink.weFriends.listeners;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.managers.ChatModeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final WeFriends plugin;

    public ChatListener(WeFriends plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        ChatModeManager.ChatMode mode = plugin.getChatModeManager().getMode(player);
        if (mode != ChatModeManager.ChatMode.GLOBAL) {
            event.setCancelled(true);
            plugin.getChatModeManager().handleChat(player, message);
        }
    }
}


