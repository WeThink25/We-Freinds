package me.wethink.weFriends.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpyManager {
    private final Set<UUID> friendSpy = ConcurrentHashMap.newKeySet();
    private final Set<UUID> partySpy = ConcurrentHashMap.newKeySet();

    public boolean toggleFriendSpy(UUID uuid) {
        if (friendSpy.contains(uuid)) {
            friendSpy.remove(uuid);
            return false;
        }
        friendSpy.add(uuid);
        return true;
    }

    public boolean togglePartySpy(UUID uuid) {
        if (partySpy.contains(uuid)) {
            partySpy.remove(uuid);
            return false;
        }
        partySpy.add(uuid);
        return true;
    }

    public boolean toggleAll(UUID uuid) {
        boolean enable = !(friendSpy.contains(uuid) && partySpy.contains(uuid));
        if (enable) {
            friendSpy.add(uuid);
            partySpy.add(uuid);
        } else {
            friendSpy.remove(uuid);
            partySpy.remove(uuid);
        }
        return enable;
    }

    public void broadcastFriendSpy(Player sender, String message) {
        Component comp = Component.text("[Spy] ", NamedTextColor.GRAY, TextDecoration.ITALIC)
                .append(Component.text("Friend ", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(Component.text(sender.getName() + ": ", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(Component.text(message, NamedTextColor.GRAY, TextDecoration.ITALIC));
        for (UUID uuid : friendSpy) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(comp);
        }
    }

    public void broadcastPartySpy(Player sender, String message) {
        Component comp = Component.text("[Spy] ", NamedTextColor.GRAY, TextDecoration.ITALIC)
                .append(Component.text("Party ", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(Component.text(sender.getName() + ": ", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(Component.text(message, NamedTextColor.GRAY, TextDecoration.ITALIC));
        for (UUID uuid : partySpy) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(comp);
        }
    }
}


