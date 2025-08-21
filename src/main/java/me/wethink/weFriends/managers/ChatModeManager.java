package me.wethink.weFriends.managers;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatModeManager {
    private final WeFriends plugin;
    private final Map<UUID, ChatMode> playerModes = new HashMap<>();

    public ChatModeManager(WeFriends plugin) {
        this.plugin = plugin;
    }

    public void setMode(Player player, ChatMode mode) {
        playerModes.put(player.getUniqueId(), mode);
        String modeName = mode.name().toLowerCase();
        MessageUtil.send(player, "chat.mode_switched", Map.of("mode", modeName));
    }

    public ChatMode getMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), ChatMode.GLOBAL);
    }

    public void handleChat(Player player, String message) {
        ChatMode mode = getMode(player);
        switch (mode) {
            case FRIEND:
                plugin.getFriendManager().sendFriendChat(player, message);
                break;
            case PARTY:
                plugin.getPartyManager().sendPartyChat(player, message);
                break;
            default:
                break;
        }
    }

    public void resetMode(Player player) {
        playerModes.remove(player.getUniqueId());
        MessageUtil.send(player, "chat.mode_reset");
    }

    public void clearPlayer(Player player) {
        playerModes.remove(player.getUniqueId());
    }

    public enum ChatMode {
        GLOBAL, FRIEND, PARTY
    }
}
