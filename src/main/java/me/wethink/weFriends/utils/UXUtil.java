package me.wethink.weFriends.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class UXUtil {
    private UXUtil() {
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = Component.text(title, NamedTextColor.GREEN);
        Component subtitleComponent = Component.text(subtitle, NamedTextColor.GRAY);

        Title titleObj = Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        );

        player.showTitle(titleObj);
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static Component createHoverText(String text, String hoverText) {
        return Component.text(text)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.GRAY)));
    }

    public static Component createFriendStatusComponent(String name, boolean online) {
        Component base = Component.text(name);
        if (online) {
            base = base.color(NamedTextColor.GREEN);
        } else {
            base = base.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true);
        }
        return base;
    }

    public static void sendFriendJoinNotification(Player player, String friendName) {
        sendTitle(player, "Friend Joined!", friendName + " is now online", 10, 40, 10);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
    }

    public static void sendFriendQuitNotification(Player player, String friendName) {
        sendTitle(player, "Friend Left", friendName + " went offline", 10, 40, 10);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.8f);
    }

    public static void sendPartyInviteNotification(Player player, String inviterName) {
        sendTitle(player, "Party Invite!", inviterName + " invited you", 10, 60, 10);
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
    }

    public static void sendFriendRequestNotification(Player player, String senderName) {
        sendTitle(player, "Friend Request", senderName + " wants to be friends", 10, 60, 10);
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
    }

    public static void sendPartyCreatedNotification(Player player) {
        sendTitle(player, "Party Created!", "You are now the leader", 10, 40, 10);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f);
    }

    public static void sendPartyJoinedNotification(Player player) {
        sendTitle(player, "Joined Party!", "You are now a member", 10, 40, 10);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f);
    }

    public static Component createPartyMemberComponent(String name, String role) {
        Component base = Component.text(name);
        if ("LEADER".equals(role)) {
            base = base.color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
        } else {
            base = base.color(NamedTextColor.WHITE);
        }
        return base.hoverEvent(HoverEvent.showText(Component.text("Role: " + role, NamedTextColor.GRAY)));
    }
}
